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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
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
import kotlinx.coroutines.CoroutineScope
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
 * Providers: Podnapisi, OpenSubtitles (REST), OpenSubtitles via Stremio + Cinemeta (IMDb lookup), SubDL (free API key),
 * Wyzie Subs (free API key from sub.wyzie.io/redeem; many sources, many languages).
 * Several languages can be searched at once: every provider is asked once per selected language.
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
    val languages: List<OnlineSubLanguage>,
    val subdlKey: String,
    val wyzieKey: String = "",
    /** File name of the video the subtitle is for (lets Wyzie score the matches). */
    val videoName: String = ""
) {
    /** The language a single provider call is made for (the engine calls providers once per selected language). */
    val language: OnlineSubLanguage get() = languages.first()
}

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
        val hit = CinemetaLookup.find(req.query, series) ?: return emptyList()
        val imdb = hit.imdb
        val title = hit.title.ifBlank { req.query }
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

/** Season / episode detection inside file names and release names (shared by the on-device and the online search). */
object SubtitleEpisodeMatcher {
    private val sxe = Regex("""(?i)(?<![a-z0-9])s(\d{1,2})[ ._-]*e(\d{1,4})(?:-?e(\d{1,4})|-(\d{1,4}))?(?![0-9])""")
    private val nxm = Regex("""(?i)(?<![a-z0-9])(\d{1,2})x(\d{2,4})(?![0-9a-z])""")
    private val seasonEpisodeWords = Regex("""(?i)season[ ._-]*(\d{1,2})[ ._-]*(?:episode|ep|e)[ ._-]*(\d{1,4})""")
    private val seasonOnly = Regex("""(?i)(?<![a-z0-9])s(\d{1,2})(?![0-9a-z])""")
    private val seasonWord = Regex("""(?i)season[ ._-]*(\d{1,2})(?![0-9])""")
    private val episodeWord = Regex("""(?i)(?<![a-z0-9])(?:episode|ep|e)[ ._-]*0*(\d{1,4})(?![0-9])""")
    private val bareNumber = Regex("""(?<![\p{L}\p{N}])(\d{1,4})(?![\p{L}\p{N}])""")
    private val bracket = Regex("""\[([^\]]*)]|\(([^)]*)\)""")
    private val keepInBracket = Regex("""(?i).*(s\d{1,2}[ ._-]*e\d|\d{1,2}x\d|episode|ep[ ._-]?\d).*""")
    private val noise = Regex(
        """(?i)(\d{3,4}[pi](?![a-z0-9])|(?<![a-z0-9])[xh]\.?26[45](?![0-9])|(?<![0-9])(?:19|20)\d{2}(?![0-9])|(?<![0-9])[57]\.1(?![0-9])|(?<![a-z0-9])\d{1,2}[ ._-]?bit(?![a-z]))"""
    )

    private fun clean(raw: String): String {
        val t = raw.replace(bracket) { m ->
            val inner = m.groupValues[1].ifEmpty { m.groupValues[2] }.trim()
            if (inner.matches(Regex("""\d{1,4}""")) || inner.matches(keepInBracket)) " $inner " else " "
        }
        return t.replace(noise, " ")
    }

    /** Lower-case words / numbers of [text]. */
    fun words(text: String): Set<String> =
        text.lowercase().replace(Regex("""[^\p{L}\p{N}]+"""), " ").split(' ').filter { it.isNotEmpty() }.toSet()

    fun numbersIn(text: String): Set<Int> = words(text).mapNotNull { it.toIntOrNull() }.toSet()

    /** Season number a folder name stands for ("Season 14", "S14"), or null. */
    fun folderSeason(folder: String): Int? {
        seasonWord.find(folder)?.let { return it.groupValues[1].toIntOrNull() }
        Regex("""(?i)^s(\d{1,2})$""").find(folder.trim())?.let { return it.groupValues[1].toIntOrNull() }
        return null
    }

    /**
     * 1 = [rawName] is for exactly this episode, -1 = it is clearly for another season / episode, 0 = no episode info.
     * [ignoreNumbers] are numbers that belong to the show's name ("Mob Psycho 100") and must not be read as an episode.
     */
    fun verdict(rawName: String, season: Int?, episode: Int?, ignoreNumbers: Set<Int> = emptySet()): Int {
        if (episode == null) return 0
        val t = clean(rawName)
        var found = false
        var ok = false
        fun consider(s: Int?, e1: Int?, e2: Int?) {
            if (e1 == null) return
            found = true
            val hi = e2 ?: e1
            val epOk = episode in minOf(e1, hi)..maxOf(e1, hi)
            val seOk = season == null || s == null || s == season
            if (epOk && seOk) ok = true
        }
        for (m in sxe.findAll(t)) {
            consider(m.groupValues[1].toIntOrNull(), m.groupValues[2].toIntOrNull(), m.groupValues[3].ifEmpty { m.groupValues[4] }.toIntOrNull())
        }
        for (m in nxm.findAll(t)) consider(m.groupValues[1].toIntOrNull(), m.groupValues[2].toIntOrNull(), null)
        for (m in seasonEpisodeWords.findAll(t)) consider(m.groupValues[1].toIntOrNull(), m.groupValues[2].toIntOrNull(), null)
        if (found) return if (ok) 1 else -1
        if (season != null) {
            val seasons = (seasonOnly.findAll(t).mapNotNull { it.groupValues[1].toIntOrNull() } +
                seasonWord.findAll(t).mapNotNull { it.groupValues[1].toIntOrNull() }).toList()
            if (seasons.isNotEmpty() && season !in seasons) return -1
        }
        val eps = episodeWord.findAll(t).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (eps.isNotEmpty()) return if (episode in eps) 1 else -1
        val nums = bareNumber.findAll(t).mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it !in ignoreNumbers || it == episode }.toList()
        if (nums.isNotEmpty()) return if (episode in nums) 1 else -1
        return 0
    }
}

/** Cinemeta (Stremio) name -> IMDb id lookup, cached so every provider / language shares one request. */
private object CinemetaLookup {
    data class Hit(val imdb: String, val title: String)

    private val cache = HashMap<String, Hit?>()

    fun find(query: String, series: Boolean): Hit? {
        val type = if (series) "series" else "movie"
        val key = "$type|${query.trim().lowercase()}"
        synchronized(cache) { if (cache.containsKey(key)) return cache[key] }
        val metas = JSONObject(
            OnlineSubHttp.getText("https://v3-cinemeta.strem.io/catalog/$type/top/search=${OnlineSubHttp.enc(query)}.json")
        ).optJSONArray("metas")
        var best: Hit? = null
        if (metas != null) {
            val wanted = SubtitleEpisodeMatcher.words(query)
            var bestScore = Int.MIN_VALUE
            for (i in 0 until minOf(metas.length(), 8)) {
                val o = metas.optJSONObject(i) ?: continue
                val imdb = o.optString("imdb_id").ifBlank { o.optString("id") }
                if (!imdb.startsWith("tt")) continue
                val name = o.optString("name")
                val have = SubtitleEpisodeMatcher.words(name)
                val hits = wanted.count { it in have }
                val score = hits * 10 - (have.size - hits) - i
                if (score > bestScore) { bestScore = score; best = Hit(imdb, name) }
            }
        }
        synchronized(cache) { cache[key] = best }
        return best
    }
}

/**
 * Wyzie Subs (github.com/wyziedevs/wyzie-subs, API at sub.wyzie.io). Needs a free API key (sub.wyzie.io/redeem).
 * Searches by IMDb id (found with Cinemeta), queries every source Wyzie offers and passes the video's file name so the
 * results come back scored; entries Wyzie marks as another season / episode and AI-translated rows are dropped.
 */
private object WyzieProvider : OnlineSubProvider {
    override val name = "Wyzie"
    override suspend fun search(req: OnlineSubRequest): List<OnlineSubResult> {
        if (req.wyzieKey.isBlank()) throw IllegalStateException("Add a free Wyzie API key to use this provider")
        val series = req.season != null && req.episode != null
        val hit = CinemetaLookup.find(req.query, series) ?: return emptyList()
        val lang = if (req.language.code == "pt-br") "pt" else req.language.code
        val url = StringBuilder("https://sub.wyzie.io/search?id=${hit.imdb}&language=${OnlineSubHttp.enc(lang)}&source=all&key=${OnlineSubHttp.enc(req.wyzieKey)}")
        if (series) url.append("&season=${req.season}&episode=${req.episode}")
        if (req.videoName.isNotBlank()) url.append("&video=${OnlineSubHttp.enc(req.videoName)}")
        val body = try {
            OnlineSubHttp.getText(url.toString(), mapOf("Accept" to "application/json"))
        } catch (e: java.io.IOException) {
            when (e.message) {
                "HTTP 400", "HTTP 404" -> return emptyList()
                "HTTP 401", "HTTP 403" -> throw IllegalStateException("Wyzie API key is missing, invalid or on hold")
                "HTTP 402", "HTTP 429" -> throw IllegalStateException("Wyzie request limit reached")
                else -> throw e
            }
        }
        val arr = JSONArray(body)
        val out = ArrayList<OnlineSubResult>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optBoolean("ai", false)) continue
            val link = o.optString("url")
            if (link.isBlank()) continue
            val reasons = o.optJSONObject("match")?.optJSONArray("reasons")
            var wrong = false
            if (reasons != null) for (k in 0 until reasons.length()) {
                val r = reasons.optString(k)
                if (r == "wrong_episode" || r == "wrong_season") wrong = true
            }
            if (wrong) continue
            out += OnlineSubResult(
                provider = name,
                title = o.optString("media").ifBlank { hit.title.ifBlank { req.query } },
                release = o.optString("release").ifBlank { o.optString("fileName") },
                language = OnlineSubtitleLanguages.match(o.optString("language")) ?: req.language,
                season = req.season, episode = req.episode,
                downloads = o.optInt("downloadCount", 0),
                hearingImpaired = o.optBoolean("isHearingImpaired", false),
                downloadUrl = link,
                packing = OnlineSubResult.Packing.PLAIN,
                encoding = o.optString("encoding").takeIf { it.isNotBlank() && !it.equals("UTF-8", true) }
            )
        }
        return out
    }
}

// ---------------------------------------------------------------------------------------------------------------
// ENGINE: parallel search + download
// ---------------------------------------------------------------------------------------------------------------

object OnlineSubtitleEngine {
    private val providers: List<OnlineSubProvider> = listOf(
        PodnapisiProvider, OpenSubtitlesRestProvider, StremioOpenSubtitlesProvider, SubdlProvider, WyzieProvider
    )

    val providerNames: List<String> get() = providers.map { it.name }

    private val subtitleExt = listOf("srt", "ass", "ssa", "vtt", "sub", "smi")

    /** Providers that search by text (not by IMDb id) can return other shows, so their titles are checked too. */
    private val textSearchProviders = setOf("Podnapisi", "OpenSubtitles")

    /**
     * Searches every provider at the same time (each one once per selected language);
     * [onProgress] is called whenever one of them finishes.
     */
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
                    val found = ArrayList<OnlineSubResult>()
                    var error: String? = null
                    for (lang in req.languages) {
                        val sub = if (req.languages.size == 1) req else req.copy(languages = listOf(lang))
                        try {
                            found += withTimeout(20_000) { p.search(sub) }
                        } catch (e: CancellationException) {
                            if (e is kotlinx.coroutines.TimeoutCancellationException) error = "Timed out" else throw e
                        } catch (t: Throwable) {
                            error = t.message?.take(90) ?: t.javaClass.simpleName
                            if (t is IllegalStateException) break // missing key / limit: the next language would fail the same way
                        }
                    }
                    val accepted = found.filter { accept(it, req) }
                    synchronized(lock) {
                        results += accepted
                        status[p.name] = OnlineSubProviderStatus(p.name, false, accepted.size, if (accepted.isEmpty()) error else null)
                        onProgress(rank(results, req), status.values.toList())
                    }
                }
            }.awaitAll()
        }
    }

    /** Drops results that are clearly another season / episode (or another show) - the "faltu" files. */
    private fun accept(r: OnlineSubResult, req: OnlineSubRequest): Boolean {
        val ep = req.episode
        if (ep != null) {
            if (r.episode != null && r.episode != ep) return false
            if (req.season != null && r.season != null && r.season != req.season) return false
            if (r.episode == null && r.release.isNotBlank() &&
                SubtitleEpisodeMatcher.verdict(r.release, req.season, ep, SubtitleEpisodeMatcher.numbersIn(req.query)) < 0
            ) return false
        }
        if (r.provider in textSearchProviders) {
            val wanted = SubtitleEpisodeMatcher.words(req.query).filter { it.length >= 2 }
            if (wanted.isNotEmpty()) {
                val have = SubtitleEpisodeMatcher.words(r.title + " " + r.release)
                if (wanted.count { it in have } < maxOf(1, (wanted.size + 1) / 2)) return false
            }
        }
        return true
    }

    private fun rank(list: List<OnlineSubResult>, req: OnlineSubRequest): List<OnlineSubResult> {
        val seen = HashSet<String>()
        val order = req.languages.map { it.code }
        val queryNumbers = SubtitleEpisodeMatcher.numbersIn(req.query)
        return list.filter { seen.add(it.downloadUrl) }.sortedWith(
            compareByDescending<OnlineSubResult> {
                var s = 0
                if (req.season != null && it.season == req.season) s += 2
                if (req.episode != null && it.episode == req.episode) s += 4
                if (req.episode != null && it.episode == null && it.release.isNotBlank() &&
                    SubtitleEpisodeMatcher.verdict(it.release, req.season, req.episode, queryNumbers) == 1
                ) s += 3
                if (it.release.contains(req.query, true) || it.title.contains(req.query, true)) s += 1
                s
            }.thenBy { r -> order.indexOf(r.language.code).let { i -> if (i < 0) 99 else i } }
                .thenByDescending { it.downloads }
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
// SEARCH STATE + UI PIECES (used by the subtitle search screen in SubtitleSelectionPanel)
// ---------------------------------------------------------------------------------------------------------------

private object OnlineSubPrefs {
    private const val FILE = "viaura_online_subtitles"
    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Selected search languages (ISO 639-1 codes). English only until the user picks others. */
    fun langs(c: Context): List<String> = try {
        (prefs(c).getString("langs", "en") ?: "en").split(',').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("en") }
    } catch (_: Throwable) { listOf("en") }
    fun setLangs(c: Context, v: List<String>) { try { prefs(c).edit().putString("langs", v.joinToString(",")).apply() } catch (_: Throwable) {} }
    fun key(c: Context): String = try { prefs(c).getString("subdl_key", "") ?: "" } catch (_: Throwable) { "" }
    fun setKey(c: Context, v: String) { try { prefs(c).edit().putString("subdl_key", v.trim()).apply() } catch (_: Throwable) {} }
    fun wyzieKey(c: Context): String = try { prefs(c).getString("wyzie_key", "") ?: "" } catch (_: Throwable) { "" }
    fun setWyzieKey(c: Context, v: String) { try { prefs(c).edit().putString("wyzie_key", v.trim()).apply() } catch (_: Throwable) {} }
}

/**
 * Everything the subtitle search screen shows and edits: show name, season, episode, languages and the online results.
 * Season / episode are filled from the video's file name; the on-device search reads the same fields.
 */
@Stable
class SubtitleSearchState(private val appContext: Context, val videoTitle: String) {
    private val parsed = OnlineSubtitleTitleParser.parse(videoTitle)

    var query by mutableStateOf(parsed.name.ifBlank { cleanVideoTitleForSearch(videoTitle) })
    var season by mutableStateOf(parsed.season?.toString().orEmpty())
    var episode by mutableStateOf(parsed.episode?.toString().orEmpty())
    var langCodes by mutableStateOf(OnlineSubPrefs.langs(appContext))
    var subdlKey by mutableStateOf(OnlineSubPrefs.key(appContext))
    var wyzieKey by mutableStateOf(OnlineSubPrefs.wyzieKey(appContext))

    var results by mutableStateOf<List<OnlineSubResult>>(emptyList())
    var statuses by mutableStateOf<List<OnlineSubProviderStatus>>(emptyList())
    var searched by mutableStateOf(false)
    var downloadingUrl by mutableStateOf<String?>(null)

    val languages: List<OnlineSubLanguage>
        get() = langCodes.map { OnlineSubtitleLanguages.byCode(it) }.distinctBy { it.code }
    val seasonNumber: Int? get() = season.trim().toIntOrNull()
    val episodeNumber: Int? get() = episode.trim().toIntOrNull()
    val isSeries: Boolean get() = episodeNumber != null
    val running: Boolean get() = statuses.any { it.running }

    fun toggleLanguage(code: String) {
        val cur = langCodes
        val next = if (code in cur) (if (cur.size > 1) cur - code else cur) else cur + code
        langCodes = next
        OnlineSubPrefs.setLangs(appContext, next)
    }

    fun updateSubdlKey(v: String) { subdlKey = v; OnlineSubPrefs.setKey(appContext, v) }
    fun updateWyzieKey(v: String) { wyzieKey = v; OnlineSubPrefs.setWyzieKey(appContext, v) }

    fun request() = OnlineSubRequest(
        query = query.trim(),
        season = if (episodeNumber != null) (seasonNumber ?: 1) else null,
        episode = episodeNumber,
        languages = languages,
        subdlKey = subdlKey,
        wyzieKey = wyzieKey,
        videoName = videoTitle
    )

    /** Runs the online search with the current fields; results arrive in [results] / [statuses] as providers finish. */
    suspend fun searchOnline() {
        val req = request()
        if (req.query.isBlank()) {
            results = emptyList(); statuses = emptyList(); searched = false
            return
        }
        searched = true
        results = emptyList()
        OnlineSubtitleEngine.search(req) { list, st -> results = list; statuses = st }
    }
}

@Composable
private fun SearchTextField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier,
    number: Boolean = false,
    onSearch: () -> Unit = {}
) {
    val isDark = isAppInDarkTheme()
    Box(
        modifier = modifier
            .height(38.dp)
            .liquidSearchSurface(RoundedCornerShape(16.dp), isDark, SheetButtonBgColor, SheetButtonBorderColor)
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
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = SecondaryTextColor, fontSize = 13.sp)
                inner()
            }
        )
    }
}

/**
 * Header of the subtitle search screen: [back] [language] [name field] [search], and below it Season / Episode.
 * The language chip opens the language sheet; the search button (or the keyboard's search key) runs both the
 * on-device and the online search.
 */
@Composable
fun SubtitleSearchHeader(
    state: SubtitleSearchState,
    onBack: () -> Unit,
    onOpenLanguages: () -> Unit,
    onSearch: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val focus = LocalFocusManager.current
    val submit = { focus.clearFocus(); onSearch() }
    val langs = state.languages
    val langLabel = langs.first().code.uppercase() + if (langs.size > 1) " +${langs.size - 1}" else ""

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                StyledIcon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to Subtitle List",
                    tint = NormalItemColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(2.dp))

            // Language (opens the language sheet)
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .liquidSearchSurface(RoundedCornerShape(16.dp), isDark, SheetButtonBgColor, SheetButtonBorderColor)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenLanguages() }
                    .padding(start = 11.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(langLabel, color = SheetButtonTextColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                StyledIcon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Choose subtitle language",
                    tint = SecondaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(6.dp))

            // Show / movie name
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .liquidSearchSurface(RoundedCornerShape(16.dp), isDark, SheetButtonBgColor, SheetButtonBorderColor)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = SecondaryTextColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = state.query,
                        onValueChange = { state.query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.5.sp, color = SheetButtonTextColor, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(ActiveHighlightColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() }),
                        decorationBox = { inner ->
                            if (state.query.isEmpty()) Text("Show or movie name", color = SecondaryTextColor, fontSize = 13.sp)
                            inner()
                        }
                    )
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { state.query = "" }, modifier = Modifier.size(22.dp)) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = SecondaryTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))

            // Small search button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ActiveHighlightColor)
                    .clickable { submit() },
                contentAlignment = Alignment.Center
            ) {
                if (state.running) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else StyledIcon(imageVector = Icons.Outlined.Search, contentDescription = "Search subtitles", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(38.dp))
            Text("Season", color = SecondaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            SearchTextField(state.season, { state.season = it.filter(Char::isDigit).take(2) }, "–", Modifier.width(64.dp), number = true, onSearch = submit)
            Spacer(Modifier.width(14.dp))
            Text("Episode", color = SecondaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(6.dp))
            SearchTextField(state.episode, { state.episode = it.filter(Char::isDigit).take(3) }, "–", Modifier.width(72.dp), number = true, onSearch = submit)
            Spacer(Modifier.weight(1f))
            Text(
                text = if (state.isSeries) "Series" else "Movie",
                color = ActiveHighlightColor, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Bottom sheet to choose the subtitle language(s) to search for (several can be ticked). */
@Composable
fun SubtitleLanguageSheet(
    state: SubtitleSearchState,
    isLandscape: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val rowBg = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f)
    PlayerGlassModalSheet(
        panelKey = "subtitle_language",
        isLandscape = isLandscape,
        heightFraction = 0.6f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismiss,
        headerStartContent = {
            Text("Subtitle language", color = NormalItemColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        },
        headerEndContent = {
            Text(
                text = "Done",
                color = ActiveHighlightColor, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDismiss() }.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    ) {
        Text(
            "Tick one or more languages to search in",
            color = SecondaryTextColor, fontSize = 11.5.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(OnlineSubtitleLanguages.all, key = { it.code }) { l ->
                val selected = l.code in state.langCodes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(rowBg)
                        .border(1.dp, if (selected) ActiveHighlightColor else SheetButtonBorderColor, RoundedCornerShape(12.dp))
                        .clickable { state.toggleLanguage(l.code) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(l.name, color = NormalItemColor, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(l.code.uppercase(), color = SecondaryTextColor, fontSize = 11.sp)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        if (selected) StyledIcon(imageVector = Icons.Outlined.Check, contentDescription = "Selected", tint = ActiveHighlightColor, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Body of the "Online Subtitles" section (shown when the section is expanded): provider status line, API keys,
 * and every result found. Tapping a result downloads it and loads it into the player ([onSubtitleFile]).
 */
@Composable
fun OnlineSubtitleSectionContent(
    state: SubtitleSearchState,
    scope: CoroutineScope,
    onSubtitleFile: (File) -> Unit,
    onNotify: (String) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    var showKeys by remember { mutableStateOf(false) }
    val results = state.results
    val statuses = state.statuses
    val running = state.running
    val languageNames = state.languages.joinToString(", ") { it.name }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (statuses.isNotEmpty()) {
            val line = statuses.joinToString("  ·  ") { s ->
                when {
                    s.running -> "${s.name} …"
                    s.error != null -> "${s.name} ✕"
                    else -> "${s.name} ${s.count}"
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(line, color = SecondaryTextColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    text = if (state.subdlKey.isBlank() || state.wyzieKey.isBlank()) "Add API key" else "API keys ✓",
                    color = ActiveHighlightColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showKeys = !showKeys }.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
        if (showKeys) {
            SearchTextField(state.wyzieKey, { state.updateWyzieKey(it) }, "Wyzie API key (free at sub.wyzie.io/redeem)", Modifier.fillMaxWidth())
            SearchTextField(state.subdlKey, { state.updateSubdlKey(it) }, "SubDL API key (free at subdl.com)", Modifier.fillMaxWidth())
        }

        when {
            !state.searched -> Text(
                "Type the show name and press search",
                color = SecondaryTextColor, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
            )
            results.isEmpty() && running -> Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp, color = ActiveHighlightColor)
            }
            results.isEmpty() -> Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                StyledIcon(imageVector = Icons.Outlined.Subtitles, contentDescription = null, tint = SecondaryTextColor, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(8.dp))
                Text("No $languageNames subtitles found online", color = SecondaryTextColor, fontSize = 13.5.sp)
                Text("Try only the show name, another language or season / episode", color = SecondaryTextColor, fontSize = 11.5.sp)
                statuses.firstOrNull { it.error != null }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("${it.name}: ${it.error}", color = SecondaryTextColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            else -> results.forEach { r ->
                key(r.downloadUrl) {
                    val busy = state.downloadingUrl == r.downloadUrl
                    val bg = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = state.downloadingUrl == null) {
                                state.downloadingUrl = r.downloadUrl
                                scope.launch {
                                    try {
                                        val f = OnlineSubtitleEngine.download(context, r, state.request())
                                        onSubtitleFile(f)
                                        onNotify("Loaded: ${f.name}")
                                        onDone()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (t: Throwable) {
                                        onNotify("Download failed: ${t.message ?: "network error"}")
                                    } finally {
                                        state.downloadingUrl = null
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
}
