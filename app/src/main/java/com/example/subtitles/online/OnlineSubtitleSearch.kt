package com.example.ui.components

import android.content.Context
import android.util.Xml
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.NetworkBypass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

/*
 * ONLINE SUBTITLE SEARCH (Bazarr-style: many providers searched at once, results merged).
 *
 * Bazarr itself is a Python server, so it cannot run inside the app. Its idea is rebuilt here natively in Kotlin:
 * every provider below is searched in parallel by show / movie name + season + episode + language, a failing provider
 * never stops the others, and the chosen subtitle is downloaded, unpacked (zip / gz), converted to UTF-8 and handed
 * to the player like any local subtitle file.
 *
 * Providers: Podnapisi, OpenSubtitles (REST), OpenSubtitles via Stremio + Cinemeta (IMDb lookup), SubDL (needs a free API key).
 * This single file holds the engine AND the screen ([OnlineSubtitleSearchContent]).
 */

// ---------------------------------------------------------------------------------------------------------------
// MODELS
// ---------------------------------------------------------------------------------------------------------------

/** [code] = ISO 639-1 (what Podnapisi / SubDL use), [code3] = ISO 639-2/B (what OpenSubtitles / Stremio use). */
data class OnlineSubLanguage(val code: String, val code3: String, val name: String, val charset: String = "windows-1252")

data class OnlineSubRequest(
    val query: String,
    val season: Int?,
    val episode: Int?,
    val language: OnlineSubLanguage,
    val subdlKey: String
)

data class OnlineSubResult(
    val provider: String,
    val title: String,
    val release: String,
    val language: OnlineSubLanguage,
    val season: Int?,
    val episode: Int?,
    val downloads: Int,
    val hearingImpaired: Boolean,
    val downloadUrl: String,
    val packing: Packing,
    val headers: Map<String, String> = emptyMap(),
    val encoding: String? = null
) {
    enum class Packing { PLAIN, GZIP, ZIP }
}

data class OnlineSubProviderStatus(val name: String, val running: Boolean, val count: Int, val error: String?)

object OnlineSubtitleLanguages {
    val all: List<OnlineSubLanguage> = listOf(
        OnlineSubLanguage("en", "eng", "English"),
        OnlineSubLanguage("hi", "hin", "Hindi", "UTF-8"),
        OnlineSubLanguage("ur", "urd", "Urdu", "windows-1256"),
        OnlineSubLanguage("bn", "ben", "Bengali", "UTF-8"),
        OnlineSubLanguage("ta", "tam", "Tamil", "UTF-8"),
        OnlineSubLanguage("te", "tel", "Telugu", "UTF-8"),
        OnlineSubLanguage("ml", "mal", "Malayalam", "UTF-8"),
        OnlineSubLanguage("kn", "kan", "Kannada", "UTF-8"),
        OnlineSubLanguage("mr", "mar", "Marathi", "UTF-8"),
        OnlineSubLanguage("gu", "guj", "Gujarati", "UTF-8"),
        OnlineSubLanguage("pa", "pan", "Punjabi", "UTF-8"),
        OnlineSubLanguage("ne", "nep", "Nepali", "UTF-8"),
        OnlineSubLanguage("ar", "ara", "Arabic", "windows-1256"),
        OnlineSubLanguage("es", "spa", "Spanish"),
        OnlineSubLanguage("pt", "por", "Portuguese"),
        OnlineSubLanguage("pt-br", "pob", "Portuguese (BR)"),
        OnlineSubLanguage("fr", "fre", "French"),
        OnlineSubLanguage("de", "ger", "German"),
        OnlineSubLanguage("it", "ita", "Italian"),
        OnlineSubLanguage("ru", "rus", "Russian", "windows-1251"),
        OnlineSubLanguage("uk", "ukr", "Ukrainian", "windows-1251"),
        OnlineSubLanguage("ja", "jpn", "Japanese", "Shift_JIS"),
        OnlineSubLanguage("ko", "kor", "Korean", "EUC-KR"),
        OnlineSubLanguage("zh", "chi", "Chinese", "GBK"),
        OnlineSubLanguage("tr", "tur", "Turkish", "windows-1254"),
        OnlineSubLanguage("id", "ind", "Indonesian"),
        OnlineSubLanguage("ms", "may", "Malay"),
        OnlineSubLanguage("th", "tha", "Thai", "windows-874"),
        OnlineSubLanguage("vi", "vie", "Vietnamese", "windows-1258"),
        OnlineSubLanguage("nl", "dut", "Dutch"),
        OnlineSubLanguage("pl", "pol", "Polish", "windows-1250"),
        OnlineSubLanguage("sv", "swe", "Swedish"),
        OnlineSubLanguage("el", "ell", "Greek", "windows-1253"),
        OnlineSubLanguage("he", "heb", "Hebrew", "windows-1255"),
        OnlineSubLanguage("fa", "per", "Persian", "windows-1256"),
        OnlineSubLanguage("ro", "rum", "Romanian", "windows-1250"),
        OnlineSubLanguage("hu", "hun", "Hungarian", "windows-1250"),
        OnlineSubLanguage("cs", "cze", "Czech", "windows-1250"),
        OnlineSubLanguage("bg", "bul", "Bulgarian", "windows-1251"),
        OnlineSubLanguage("hr", "hrv", "Croatian", "windows-1250"),
        OnlineSubLanguage("fi", "fin", "Finnish"),
        OnlineSubLanguage("da", "dan", "Danish"),
        OnlineSubLanguage("no", "nor", "Norwegian")
    )

    fun byCode(code: String?): OnlineSubLanguage = all.firstOrNull { it.code == code } ?: all.first()

    /** Provider language ids (639-1, 639-2/B, 639-2/T, a few alternates) -> our language. */
    fun match(raw: String?): OnlineSubLanguage? {
        val k = raw?.trim()?.lowercase().orEmpty()
        if (k.isEmpty()) return null
        return all.firstOrNull { it.code == k || it.code3 == k } ?: when (k) {
            "fra" -> byCode("fr"); "deu" -> byCode("de"); "zho", "zht", "zhe" -> byCode("zh")
            "fas" -> byCode("fa"); "ron" -> byCode("ro"); "nld" -> byCode("nl"); "ces" -> byCode("cs")
            "msa" -> byCode("ms"); "pt-pt", "pt_pt" -> byCode("pt"); "scc" -> null
            else -> null
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// TITLE PARSING: "Show.Name.S01E02.1080p.WEB-DL.mkv" -> ("Show Name", 1, 2)
// ---------------------------------------------------------------------------------------------------------------

data class ParsedVideoTitle(val name: String, val season: Int?, val episode: Int?)

object OnlineSubtitleTitleParser {
    private val seasonEpisode = listOf(
        Regex("""(?i)\bS(\d{1,2})[ ._-]*E(\d{1,3})\b"""),
        Regex("""(?i)\b(\d{1,2})x(\d{2,3})\b"""),
        Regex("""(?i)\bseason[ ._-]*(\d{1,2})[ ._-]*(?:episode|ep|e)[ ._-]*(\d{1,3})\b""")
    )
    private val episodeOnly = listOf(
        Regex("""(?i)\b(?:episode|ep|e)[ ._-]*(\d{1,3})\b"""),
        Regex("""\s-\s(\d{1,3})(?:v\d)?(?=\s|\[|\(|$)""")
    )
    private val noise = Regex(
        """(?i)\b(2160p|1080p|720p|480p|4k|uhd|bluray|blu-ray|bdrip|brrip|web-?dl|web-?rip|webrip|hdtv|hdrip|dvdrip|x264|x265|h\.?264|h\.?265|hevc|10bit|aac|ac3|dts|ddp?5\.1|hindi|dual[ ._-]?audio|multi|amzn|nf|repack|proper|extended|remux)\b.*$"""
    )

    fun parse(rawTitle: String): ParsedVideoTitle {
        var t = rawTitle.substringBeforeLast('.', rawTitle).let { if (rawTitle.substringAfterLast('.', "").length in 2..4) it else rawTitle }
        t = t.replace(Regex("""\[[^\]]*]|\([^)]*\)"""), " ")
        var season: Int? = null
        var episode: Int? = null
        var cut = -1
        for (re in seasonEpisode) {
            val m = re.find(t) ?: continue
            season = m.groupValues[1].toIntOrNull(); episode = m.groupValues[2].toIntOrNull(); cut = m.range.first
            break
        }
        if (episode == null) {
            for (re in episodeOnly) {
                val m = re.find(t) ?: continue
                episode = m.groupValues[1].toIntOrNull(); season = 1; cut = m.range.first
                break
            }
        }
        var name = if (cut > 0) t.substring(0, cut) else t
        name = name.replace(noise, " ").replace(Regex("""[._]+"""), " ").replace(Regex("""\s+"""), " ").trim(' ', '-', '_')
        return ParsedVideoTitle(name, season, episode)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// PROVIDERS
// ---------------------------------------------------------------------------------------------------------------

private interface OnlineSubProvider {
    val name: String
    suspend fun search(req: OnlineSubRequest): List<OnlineSubResult>
}

private object OnlineSubHttp {
    const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    const val MAX_BYTES = 12L * 1024 * 1024

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(14, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    fun get(c: OkHttpClient, url: String, headers: Map<String, String> = emptyMap()): ByteArray {
        val b = Request.Builder().url(url).header("User-Agent", headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value ?: UA)
        for ((k, v) in headers) if (!k.equals("User-Agent", true)) b.header(k, v)
        c.newCall(b.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            val src = resp.body?.byteStream() ?: throw java.io.IOException("empty")
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val n = src.read(buf)
                if (n <= 0) break
                total += n
                if (total > MAX_BYTES) throw java.io.IOException("too large")
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    fun getText(url: String, headers: Map<String, String> = emptyMap()): String = String(get(client, url, headers), Charsets.UTF_8)
}

/** Podnapisi (XML search API, the same one Bazarr / subliminal use). */
private object PodnapisiProvider : OnlineSubProvider {
    override val name = "Podnapisi"
    override suspend fun search(req: OnlineSubRequest): List<OnlineSubResult> {
        val sb = StringBuilder("https://www.podnapisi.net/subtitles/search/old?sXML=1&sL=${OnlineSubHttp.enc(req.language.code)}&sK=${OnlineSubHttp.enc(req.query)}")
        if (req.season != null && req.episode != null) sb.append("&sTS=${req.season}&sTE=${req.episode}")
        val out = ArrayList<OnlineSubResult>()
        var page = 1
        while (page <= 3) {
            val xml = OnlineSubHttp.getText(if (page == 1) sb.toString() else "$sb&page=$page")
            val parser = Xml.newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var tag = ""
            var cur: MutableMap<String, String>? = null
            var pageNow = 1
            var pageCount = 1
            var inPagination = false
            var ev = parser.eventType
            while (ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (ev) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        tag = parser.name
                        if (tag == "subtitle") cur = HashMap()
                        if (tag == "pagination") inPagination = true
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            if (inPagination) {
                                if (tag == "current") pageNow = text.toIntOrNull() ?: pageNow
                                if (tag == "count") pageCount = text.toIntOrNull() ?: pageCount
                            } else cur?.put(tag, text)
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name == "pagination") inPagination = false
                        if (parser.name == "subtitle") {
                            val m = cur
                            cur = null
                            val pid = m?.get("pid")
                            if (m != null && !pid.isNullOrBlank()) {
                                val release = m["release"].orEmpty()
                                out += OnlineSubResult(
                                    provider = name,
                                    title = m["title"].orEmpty().ifBlank { req.query },
                                    release = release,
                                    language = OnlineSubtitleLanguages.match(m["language"]) ?: req.language,
                                    season = m["tvSeason"]?.toIntOrNull()?.takeIf { it > 0 },
                                    episode = m["tvEpisode"]?.toIntOrNull()?.takeIf { it > 0 },
                                    downloads = m["downloads"]?.toIntOrNull() ?: 0,
                                    hearingImpaired = m["flags"].orEmpty().contains('n'),
                                    downloadUrl = "https://www.podnapisi.net/subtitles/$pid/download?container=zip",
                                    packing = OnlineSubResult.Packing.ZIP
                                )
                            }
                        }
                        tag = ""
                    }
                }
                ev = parser.next()
            }
            if (pageNow >= pageCount) break
            page++
        }
        return out
    }
}

/** OpenSubtitles legacy REST search (no key; the public "TemporaryUserAgent"). Files come gzipped. */
private object OpenSubtitlesRestProvider : OnlineSubProvider {
    override val name = "OpenSubtitles"
    override suspend fun search(req: OnlineSubRequest): List<OnlineSubResult> {
        val parts = ArrayList<String>()
        if (req.season != null && req.episode != null) parts += "episode-${req.episode}"
        parts += "query-${OnlineSubHttp.enc(req.query.lowercase())}"
        if (req.season != null && req.episode != null) parts += "season-${req.season}"
        parts += "sublanguageid-${req.language.code3}"
        val json = OnlineSubHttp.getText(
            "https://rest.opensubtitles.org/search/" + parts.joinToString("/"),
            mapOf("X-User-Agent" to "TemporaryUserAgent", "Accept" to "application/json")
        )
        val arr = JSONArray(json)
        val out = ArrayList<OnlineSubResult>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val link = o.optString("SubDownloadLink")
            if (link.isBlank()) continue
            val file = o.optString("SubFileName")
            out += OnlineSubResult(
                provider = name,
                title = o.optString("MovieName").ifBlank { req.query },
                release = o.optString("MovieReleaseName").ifBlank { file },
                language = OnlineSubtitleLanguages.match(o.optString("SubLanguageID")) ?: req.language,
                season = o.optString("SeriesSeason").toIntOrNull()?.takeIf { it > 0 },
                episode = o.optString("SeriesEpisode").toIntOrNull()?.takeIf { it > 0 },
                downloads = o.optString("SubDownloadsCnt").toIntOrNull() ?: 0,
                hearingImpaired = o.optString("SubHearingImpaired") == "1",
                downloadUrl = link,
                packing = if (link.endsWith(".gz", true)) OnlineSubResult.Packing.GZIP else OnlineSubResult.Packing.PLAIN,
                headers = mapOf("X-User-Agent" to "TemporaryUserAgent"),
                encoding = o.optString("SubEncoding").takeIf { it.isNotBlank() }
            )
        }
        return out
    }
}

/** OpenSubtitles through the Stremio add-on (IMDb id found with Cinemeta by name). Direct, UTF-8 converted files. */
private object StremioOpenSubtitlesProvider : OnlineSubProvider {
    override val name = "OpenSubtitles (IMDb)"
    override suspend fun search(req: OnlineSubRequest): List<OnlineSubResult> {
        val series = req.season != null && req.episode != null
        val type = if (series) "series" else "movie"
        val meta = JSONObject(
            OnlineSubHttp.getText("https://v3-cinemeta.strem.io/catalog/$type/top/search=${OnlineSubHttp.enc(req.query)}.json")
        ).optJSONArray("metas") ?: return emptyList()
        val first = meta.optJSONObject(0) ?: return emptyList()
        val imdb = first.optString("imdb_id").ifBlank { first.optString("id") }
        if (!imdb.startsWith("tt")) return emptyList()
        val title = first.optString("name").ifBlank { req.query }
        val id = if (series) "$imdb:${req.season}:${req.episode}" else imdb
        val subs = JSONObject(OnlineSubHttp.getText("https://opensubtitles-v3.strem.io/subtitles/$type/$id.json")).optJSONArray("subtitles")
            ?: return emptyList()
        val out = ArrayList<OnlineSubResult>()
        for (i in 0 until subs.length()) {
            val o = subs.optJSONObject(i) ?: continue
            val lang = OnlineSubtitleLanguages.match(o.optString("lang")) ?: continue
            if (lang.code != req.language.code) continue
            val url = o.optString("url")
            if (url.isBlank()) continue
            out += OnlineSubResult(
                provider = name, title = title, release = "", language = lang,
                season = req.season, episode = req.episode, downloads = 0, hearingImpaired = false,
                downloadUrl = url, packing = OnlineSubResult.Packing.PLAIN
            )
        }
        return out
    }
}

/** SubDL (free API key from subdl.com, entered once in the screen). */
private object SubdlProvider : OnlineSubProvider {
    override val name = "SubDL"
    override suspend fun search(req: OnlineSubRequest): List<OnlineSubResult> {
        if (req.subdlKey.isBlank()) throw IllegalStateException("Add a free SubDL API key to use this provider")
        val series = req.season != null && req.episode != null
        val lang = if (req.language.code == "pt-br") "BR_PT" else req.language.code.uppercase()
        val url = StringBuilder("https://api.subdl.com/api/v1/subtitles?api_key=${OnlineSubHttp.enc(req.subdlKey)}")
            .append("&film_name=${OnlineSubHttp.enc(req.query)}&languages=$lang&subs_per_page=30")
        if (series) url.append("&type=tv&season_number=${req.season}&episode_number=${req.episode}")
        val root = JSONObject(OnlineSubHttp.getText(url.toString()))
        if (!root.optBoolean("status", true)) throw IllegalStateException(root.optString("error").ifBlank { "SubDL error" })
        val subs = root.optJSONArray("subtitles") ?: return emptyList()
        val title = root.optJSONArray("results")?.optJSONObject(0)?.optString("name").orEmpty().ifBlank { req.query }
        val out = ArrayList<OnlineSubResult>()
        for (i in 0 until subs.length()) {
            val o = subs.optJSONObject(i) ?: continue
            val path = o.optString("url")
            if (path.isBlank()) continue
            out += OnlineSubResult(
                provider = name,
                title = title,
                release = o.optString("release_name").ifBlank { o.optString("name") },
                language = req.language,
                season = o.optInt("season", 0).takeIf { it > 0 },
                episode = o.optInt("episode", 0).takeIf { it > 0 },
                downloads = 0,
                hearingImpaired = o.optBoolean("hi", false),
                downloadUrl = if (path.startsWith("http")) path else "https://dl.subdl.com$path",
                packing = OnlineSubResult.Packing.ZIP
            )
        }
        return out
    }
}

// ---------------------------------------------------------------------------------------------------------------
// ENGINE: parallel search + download
// ---------------------------------------------------------------------------------------------------------------

object OnlineSubtitleEngine {
    private val providers: List<OnlineSubProvider> = listOf(PodnapisiProvider, OpenSubtitlesRestProvider, StremioOpenSubtitlesProvider, SubdlProvider)

    val providerNames: List<String> get() = providers.map { it.name }

    private val subtitleExt = listOf("srt", "ass", "ssa", "vtt", "sub", "smi")

    /** Searches every provider at the same time; [onProgress] is called whenever one of them finishes. */
    suspend fun search(
        req: OnlineSubRequest,
        onProgress: (List<OnlineSubResult>, List<OnlineSubProviderStatus>) -> Unit
    ) {
        val lock = Any()
        val results = ArrayList<OnlineSubResult>()
        val status = LinkedHashMap<String, OnlineSubProviderStatus>()
        providers.forEach { status[it.name] = OnlineSubProviderStatus(it.name, true, 0, null) }
        onProgress(emptyList(), status.values.toList())
        coroutineScope {
            providers.map { p ->
                async(Dispatchers.IO) {
                    var found: List<OnlineSubResult> = emptyList()
                    var error: String? = null
                    try {
                        found = withTimeout(25_000) { p.search(req) }
                    } catch (e: CancellationException) {
                        if (e is kotlinx.coroutines.TimeoutCancellationException) error = "Timed out" else throw e
                    } catch (t: Throwable) {
                        error = t.message?.take(90) ?: t.javaClass.simpleName
                    }
                    synchronized(lock) {
                        results += found
                        status[p.name] = OnlineSubProviderStatus(p.name, false, found.size, error)
                        onProgress(rank(results, req), status.values.toList())
                    }
                }
            }.awaitAll()
        }
    }

    private fun rank(list: List<OnlineSubResult>, req: OnlineSubRequest): List<OnlineSubResult> {
        val seen = HashSet<String>()
        return list.filter { seen.add(it.downloadUrl) }.sortedWith(
            compareByDescending<OnlineSubResult> {
                var s = 0
                if (req.season != null && it.season == req.season) s += 2
                if (req.episode != null && it.episode == req.episode) s += 4
                if (it.release.contains(req.query, true) || it.title.contains(req.query, true)) s += 1
                s
            }.thenByDescending { it.downloads }
        )
    }

    /** Downloads [r], unpacks it, converts it to UTF-8 and returns the local subtitle file. */
    suspend fun download(context: Context, r: OnlineSubResult, req: OnlineSubRequest): File = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val raw = try {
            OnlineSubHttp.get(OnlineSubHttp.client, r.downloadUrl, r.headers)
        } catch (t: Throwable) {
            val proxy = try { NetworkBypass.okHttpProxy(app) } catch (_: Throwable) { null } ?: throw t
            OnlineSubHttp.get(OnlineSubHttp.client.newBuilder().proxy(proxy).build(), r.downloadUrl, r.headers)
        }
        var ext = "srt"
        val bytes: ByteArray = when (r.packing) {
            OnlineSubResult.Packing.GZIP -> GZIPInputStream(ByteArrayInputStream(raw)).use { it.readBytes() }
            OnlineSubResult.Packing.ZIP -> {
                val picked = pickFromZip(raw, req)
                ext = picked.first
                picked.second
            }
            OnlineSubResult.Packing.PLAIN -> raw
        }
        // a few servers hand a zip / gzip back although the link looked plain
        val body = when {
            bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() -> pickFromZip(bytes, req).also { ext = it.first }.second
            bytes.size > 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte() -> GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            else -> bytes
        }
        val text = decode(body, r.encoding, r.language)
        if (r.packing != OnlineSubResult.Packing.ZIP) ext = sniffExtension(text)
        val dir = File(app.cacheDir, "online_subtitles").apply { mkdirs() }
        val safe = (r.release.ifBlank { r.title }).replace(Regex("""[^\p{L}\p{N} ._\-\[\]()]"""), "_").trim().take(60).ifBlank { "subtitle" }
        val file = File(dir, "$safe.${r.language.code}.$ext")
        file.writeText(text.removePrefix("﻿"), Charsets.UTF_8)
        file
    }

    private fun sniffExtension(text: String): String {
        val head = text.take(2048)
        return when {
            head.contains("WEBVTT") -> "vtt"
            head.contains("[Script Info]", true) -> "ass"
            head.contains("<SAMI", true) -> "smi"
            else -> "srt"
        }
    }

    private fun pickFromZip(zip: ByteArray, req: OnlineSubRequest): Pair<String, ByteArray> {
        data class Entry(val name: String, val data: ByteArray)
        val entries = ArrayList<Entry>()
        ZipInputStream(ByteArrayInputStream(zip)).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                val ext = e.name.substringAfterLast('.', "").lowercase()
                if (!e.isDirectory && ext in subtitleExt) entries += Entry(e.name, zin.readBytes())
            }
        }
        if (entries.isEmpty()) throw java.io.IOException("No subtitle inside the archive")
        val ep = req.episode
        val se = req.season
        val matching = if (ep != null) entries.filter { en ->
            val n = en.name
            val sRe = if (se != null) Regex("""(?i)(s0*$se[ ._-]*e0*$ep(?!\d)|\b0*${se}x0*$ep(?!\d))""") else null
            (sRe != null && sRe.containsMatchIn(n)) || Regex("""(?i)(?<![a-z0-9])e(?:p|pisode)?[ ._-]*0*$ep(?!\d)""").containsMatchIn(n) ||
                Regex("""(?<!\d)0*$ep(?!\d)""").containsMatchIn(n.substringAfterLast('/'))
        } else emptyList()
        val chosen = (matching.ifEmpty { entries }).sortedBy { subtitleExt.indexOf(it.name.substringAfterLast('.').lowercase()) }.first()
        return chosen.name.substringAfterLast('.').lowercase() to chosen.data
    }

    /** Bytes -> text: UTF-8 when it is valid UTF-8, otherwise the provider's / language's legacy code page. */
    private fun decode(bytes: ByteArray, declared: String?, lang: OnlineSubLanguage): String {
        try {
            return Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Throwable) { /* not UTF-8 */ }
        for (name in listOfNotNull(declared, lang.charset)) {
            try { return String(bytes, Charset.forName(name)) } catch (_: Throwable) {}
        }
        return String(bytes, Charsets.ISO_8859_1)
    }
}

// ---------------------------------------------------------------------------------------------------------------
// SCREEN
// ---------------------------------------------------------------------------------------------------------------

private object OnlineSubPrefs {
    private const val FILE = "viaura_online_subtitles"
    fun lang(c: Context): String = try { c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("lang", "en") ?: "en" } catch (_: Throwable) { "en" }
    fun setLang(c: Context, v: String) { try { c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("lang", v).apply() } catch (_: Throwable) {} }
    fun key(c: Context): String = try { c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("subdl_key", "") ?: "" } catch (_: Throwable) { "" }
    fun setKey(c: Context, v: String) { try { c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("subdl_key", v.trim()).apply() } catch (_: Throwable) {} }
}

/** How many seasons of a show have already aired (TVMaze, no key needed). Cached per name; null when unknown. */
private object OnlineSeasonLookup {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    suspend fun aired(name: String): Int? {
        val key = name.trim().lowercase()
        if (key.length < 2) return null
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val show = JSONObject(OnlineSubHttp.getText("https://api.tvmaze.com/singlesearch/shows?q=${OnlineSubHttp.enc(name.trim())}"))
                val id = show.optInt("id", 0)
                if (id <= 0) return@withContext null
                val arr = JSONArray(OnlineSubHttp.getText("https://api.tvmaze.com/shows/$id/seasons"))
                val today = java.time.LocalDate.now().toString()
                var max = 0
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val n = o.optInt("number", 0)
                    if (n <= 0 || o.isNull("premiereDate")) continue
                    if (o.optString("premiereDate") <= today) max = maxOf(max, n)
                }
                if (max > 0) cache[key] = max
                max.takeIf { it > 0 }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
        }
    }
}

/**
 * The "Online" tab of the subtitle search sheet: name + season + episode + language, results from all providers,
 * tap a result to download and load it ([onSubtitleFile]).
 */
@Composable
fun ColumnScope.OnlineSubtitleSearchContent(
    videoTitle: String,
    onSubtitleFile: (File) -> Unit,
    onNotify: (String) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val parsed = remember(videoTitle) { OnlineSubtitleTitleParser.parse(videoTitle) }

    var query by remember(videoTitle) { mutableStateOf(parsed.name) }
    var season by remember(videoTitle) { mutableStateOf(parsed.season?.toString().orEmpty()) }
    var episode by remember(videoTitle) { mutableStateOf(parsed.episode?.toString().orEmpty()) }
    var langCode by remember { mutableStateOf(OnlineSubPrefs.lang(context)) }
    var subdlKey by remember { mutableStateOf(OnlineSubPrefs.key(context)) }
    var showKey by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineSubResult>>(emptyList()) }
    var statuses by remember { mutableStateOf<List<OnlineSubProviderStatus>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    var downloadingUrl by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    val language = OnlineSubtitleLanguages.byCode(langCode)
    val isDark = isAppInDarkTheme()
    val running = statuses.any { it.running }

    fun currentRequest() = OnlineSubRequest(
        query = query.trim(),
        season = season.trim().toIntOrNull()?.takeIf { episode.trim().toIntOrNull() != null },
        episode = episode.trim().toIntOrNull()?.takeIf { season.trim().toIntOrNull() != null },
        language = language,
        subdlKey = subdlKey
    )

    fun runSearch() {
        val req = currentRequest()
        if (req.query.isBlank()) { onNotify("Type a show or movie name first"); return }
        focus.clearFocus()
        job?.cancel()
        searched = true
        results = emptyList()
        job = scope.launch {
            try {
                OnlineSubtitleEngine.search(req) { list, st -> results = list; statuses = st }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                onNotify("Search failed: ${t.message}")
            }
        }
    }

    LaunchedEffect(videoTitle) { if (query.isNotBlank()) runSearch() }

    @Composable
    fun Field(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier, number: Boolean = false, onSearch: (() -> Unit)? = null) {
        Box(
            modifier = modifier
                .height(40.dp)
                .liquidSearchSurface(RoundedCornerShape(14.dp), isDark, SheetButtonBgColor, SheetButtonBorderColor)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.5.sp, color = SheetButtonTextColor, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(ActiveHighlightColor),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(hint, color = SecondaryTextColor, fontSize = 13.sp)
                    inner()
                }
            )
        }
    }

    // 1. One line: language | name | S | E | search
    var langMenu by remember { mutableStateOf(false) }
    var seasonMenu by remember { mutableStateOf(false) }
    var seasonCount by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { seasonCount = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(600)
        seasonCount = OnlineSeasonLookup.aired(q)
    }

    @Composable
    fun Pill(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
        Row(
            modifier = modifier
                .height(40.dp)
                .liquidSearchSurface(RoundedCornerShape(14.dp), isDark, SheetButtonBgColor, SheetButtonBorderColor)
                .clip(RoundedCornerShape(14.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) { content() }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Language
        Box {
            Pill(onClick = { langMenu = true }) {
                Text(language.code.uppercase(), color = SheetButtonTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                StyledIcon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Language", tint = SecondaryTextColor, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = langMenu, onDismissRequest = { langMenu = false }) {
                OnlineSubtitleLanguages.all.forEach { l ->
                    DropdownMenuItem(
                        text = { Text(l.name, fontWeight = if (l.code == langCode) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            langMenu = false
                            langCode = l.code
                            OnlineSubPrefs.setLang(context, l.code)
                            if (searched) runSearch()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        // Name
        Field(query, { query = it }, "Show or movie name", Modifier.weight(1f), onSearch = { runSearch() })
        Spacer(Modifier.width(6.dp))
        // Season: list comes from the internet (aired seasons), falls back to a plain 1-10 list
        Box {
            Pill(onClick = { seasonMenu = true }) {
                Text(
                    if (season.isBlank()) "S" else "S$season",
                    color = if (season.isBlank()) SecondaryTextColor else SheetButtonTextColor,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
                StyledIcon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Season", tint = SecondaryTextColor, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = seasonMenu, onDismissRequest = { seasonMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Movie (no season)") },
                    onClick = { seasonMenu = false; season = ""; episode = "" }
                )
                val count = maxOf(seasonCount ?: 10, season.toIntOrNull() ?: 0)
                for (n in 1..count) {
                    DropdownMenuItem(
                        text = { Text("S$n", fontWeight = if (season == n.toString()) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { seasonMenu = false; season = n.toString() }
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        // Episode: type the number
        Pill(modifier = Modifier.width(60.dp)) {
            Text("E", color = SecondaryTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(3.dp))
            BasicTextField(
                value = episode,
                onValueChange = { episode = it.filter(Char::isDigit).take(3) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.5.sp, color = SheetButtonTextColor, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(ActiveHighlightColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.width(6.dp))
        // Search
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ActiveHighlightColor)
                .clickable { runSearch() },
            contentAlignment = Alignment.Center
        ) {
            if (running) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            else StyledIcon(imageVector = Icons.Outlined.Search, contentDescription = "Search online", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }

    // 4. Provider status line + SubDL key
    if (statuses.isNotEmpty()) {
        val line = statuses.joinToString("  ·  ") { s ->
            when {
                s.running -> "${s.name} …"
                s.error != null -> "${s.name} ✕"
                else -> "${s.name} ${s.count}"
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text(line, color = SecondaryTextColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(
                text = if (subdlKey.isBlank()) "Add SubDL key" else "SubDL key ✓",
                color = ActiveHighlightColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showKey = !showKey }.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
    if (showKey) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(subdlKey, { subdlKey = it; OnlineSubPrefs.setKey(context, it) }, "SubDL API key (free at subdl.com)", Modifier.weight(1f))
        }
    }

    // 5. Results
    when {
        !searched -> Unit
        results.isEmpty() && running -> Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = ActiveHighlightColor)
        }
        results.isEmpty() -> Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StyledIcon(imageVector = Icons.Outlined.Subtitles, contentDescription = null, tint = SecondaryTextColor, modifier = Modifier.size(38.dp))
            Spacer(Modifier.height(8.dp))
            Text("No ${language.name} subtitles found", color = SecondaryTextColor, fontSize = 13.5.sp)
            Text("Try only the show name, another language or season / episode", color = SecondaryTextColor, fontSize = 11.5.sp)
            statuses.firstOrNull { it.error != null }?.let {
                Spacer(Modifier.height(6.dp))
                Text("${it.name}: ${it.error}", color = SecondaryTextColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        else -> LazyColumn(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(results, key = { it.downloadUrl }) { r ->
                val busy = downloadingUrl == r.downloadUrl
                val bg = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = downloadingUrl == null) {
                            downloadingUrl = r.downloadUrl
                            scope.launch {
                                try {
                                    val f = OnlineSubtitleEngine.download(context, r, currentRequest())
                                    onSubtitleFile(f)
                                    onNotify("Loaded: ${f.name}")
                                    onDone()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (t: Throwable) {
                                    onNotify("Download failed: ${t.message ?: "network error"}")
                                } finally {
                                    downloadingUrl = null
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val head = buildString {
                            append(r.title)
                            if (r.season != null && r.episode != null) append("  S%02dE%02d".format(r.season, r.episode))
                            else if (r.episode != null) append("  E%02d".format(r.episode))
                        }
                        Text(head, color = NormalItemColor, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (r.release.isNotBlank()) {
                            Text(r.release, color = SecondaryTextColor, fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(r.provider, color = ActiveHighlightColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(r.language.name, color = SecondaryTextColor, fontSize = 11.sp)
                            if (r.downloads > 0) Text("↓ ${r.downloads}", color = SecondaryTextColor, fontSize = 11.sp)
                            if (r.hearingImpaired) Text("HI", color = SecondaryTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ActiveHighlightColor)
                    else StyledIcon(imageVector = Icons.Outlined.Download, contentDescription = "Download", tint = ActiveHighlightColor, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
