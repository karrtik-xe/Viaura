package com.example.util

import android.util.Base64
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * Online lyrics lookup for ANY track (local files and online music) - a port of the way MpvRx finds
 * lyrics, kept in Viaura's own "lyrics = one LRC/plain string" model so the existing player UI and
 * AudioTrackItem.lyricsText keep working unchanged.
 *
 * How a lookup works (same idea as MpvRx's LyricsRepository):
 *  1. [buildRefs] cleans the raw title/artist (file extension, "[youtubeId]", track numbers,
 *     "(Official Video)", "| T-Series", channel/label names, "Artist - Title" read both ways round)
 *     and produces an ordered list of title/artist guesses.
 *  2. Every provider (SimpMusic by video id, LRCLIB, KuGou, Megalobiz) is asked at the same time;
 *     answers are taken in priority order and the first *time-synced* answer that fits the file
 *     length wins; plain text is only a fallback.
 *  3. Every hit is verified by [TrackMatcher] (same title core, same version words such as
 *     live/remix, a shared artist, length within 12 s) so a cover or another edit never gets
 *     shown scrolling out of time.
 *  4. A timed answer whose last line is far past the end of the file is another edit of the song
 *     and is used only if nothing better exists.
 *  5. Hits are cached on disk, so a replayed song gets its lyrics instantly and offline.
 */
object LyricsFetcher {
    private const val TAG = "LyricsFetcher"
    private const val AGENT = "Viaura Music Player/1.0"
    private const val PROVIDER_BUDGET_MS = 10_000L
    private const val MAX_REFS = 8
    private const val LENGTH_OVERRUN_MS = 12_000L
    private const val CACHE_VERSION = "v1"

    data class LyricsResult(val syncedLyrics: String?, val plainLyrics: String?)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(9, TimeUnit.SECONDS)
        .build()

    private val memory = LruCache<String, String>(64)

    private data class Query(
        val refs: List<TrackRef>,
        val durationMs: Long,
        val album: String?,
        val videoId: String?
    ) {
        val primary: TrackRef get() = refs.firstOrNull() ?: TrackRef("", "")
        fun candidates(limit: Int = 4): List<TrackRef> =
            refs.filter { it.title.isNotBlank() }.distinctBy { it.key() }.take(limit)
    }

    private data class TrackRef(val title: String, val artist: String) {
        fun key(): String = "${title.trim()}|${artist.trim()}".lowercase()
    }

    private interface Provider {
        val name: String
        suspend fun fetch(q: Query): String?
    }

    /** Old API (kept for DownloadManager): synced and plain text for a title/artist/duration. */
    suspend fun fetch(title: String, artist: String, durationSec: Long): LyricsResult? {
        val raw = fetchBest(title, artist, durationSec * 1000L) ?: return null
        return if (LrcParser.isSynced(raw)) LyricsResult(raw, null) else LyricsResult(null, raw)
    }

    /**
     * Best lyrics for a track as one string: time-synced LRC when any provider has it, otherwise
     * plain text. Null when nothing was found or the network is unavailable.
     */
    suspend fun fetchBest(
        title: String,
        artist: String?,
        durationMs: Long,
        album: String? = null,
        videoId: String? = null,
        cacheDir: File? = null,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        try {
            val refs = buildRefs(title, artist)
            if (refs.isEmpty()) return@withContext null
            val key = cacheKey(refs.first(), durationMs, videoId)

            if (!forceRefresh) {
                memory.get(key)?.let { return@withContext it }
                readDisk(cacheDir, key)?.let { memory.put(key, it); return@withContext it }
            }

            val query = Query(refs, durationMs.coerceAtLeast(0L), album, videoId)
            val found = race(query) ?: looseLrcLib(query)
            if (found != null) {
                memory.put(key, found)
                writeDisk(cacheDir, key, found)
            }
            found
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fetch failed for '$title' by '$artist': ${e.message}", e)
            null
        }
    }

    fun videoIdFrom(path: String?): String? =
        path?.takeIf { it.isNotBlank() }?.let { YOUTUBE_VIDEO.find(it)?.groupValues?.get(1) }

    // ---------------------------------------------------------------------------------------
    // Race over providers
    // ---------------------------------------------------------------------------------------

    private val providers: List<Provider> by lazy {
        listOf(SimpMusicProvider, LrcLibProvider, KuGouProvider, MegalobizProvider)
    }

    private fun fitsLength(raw: String, durationMs: Long): Boolean {
        if (durationMs <= 0L) return true
        val last = LrcParser.lastStampMs(raw) ?: return true
        return last <= durationMs + LENGTH_OVERRUN_MS
    }

    private suspend fun race(query: Query): String? = coroutineScope {
        val jobs = providers.map { p ->
            p to async(Dispatchers.IO) {
                withTimeoutOrNull(PROVIDER_BUDGET_MS) {
                    try {
                        p.fetch(query)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        Log.d(TAG, "${p.name} failed: ${e.message}")
                        null
                    }
                }
            }
        }
        try {
            var plain: String? = null
            var misfit: String? = null
            var winner: String? = null
            for ((p, job) in jobs) {
                val raw = job.await()?.takeIf { it.isNotBlank() } ?: continue
                val lines = LrcParser.parse(raw)
                if (lines.isEmpty()) continue
                val synced = lines.any { it.timeMs != null }
                if (synced) {
                    if (fitsLength(raw, query.durationMs)) {
                        Log.d(TAG, "synced lyrics from ${p.name}")
                        winner = raw
                        break
                    }
                    if (misfit == null) misfit = raw
                } else if (plain == null) {
                    plain = raw
                }
            }
            winner ?: plain ?: misfit
        } finally {
            jobs.forEach { it.second.cancel() }
        }
    }

    /** Last resort, same as MpvRx: LRCLIB with the title alone, only if the length fits. */
    private suspend fun looseLrcLib(query: Query): String? {
        val loose = query.copy(
            refs = query.candidates().map { TrackRef(it.title, "") }.distinctBy { it.key() }
        )
        if (loose.refs.isEmpty()) return null
        val raw = withTimeoutOrNull(PROVIDER_BUDGET_MS) {
            try {
                LrcLibProvider.fetch(loose)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
        } ?: return null
        return raw.takeIf { LrcParser.parse(it).isNotEmpty() && fitsLength(it, query.durationMs) }
    }

    // ---------------------------------------------------------------------------------------
    // Title / artist cleaning (MpvRx LyricsRepository.buildRefs)
    // ---------------------------------------------------------------------------------------

    private val BRACKETED = Regex(
        """[\(\[\{（［｛【『「〔〈《]([^)\]\}）］｝】』」〕〉》「]*)[\)\]\}）］｝】』」〕〉》「]"""
    )
    private val TRACK_NO = Regex("""^\s*\d{1,3}\s*[\._-]\s+""")
    private val YOUTUBE_ID_SUFFIX = Regex("""\s*\[[a-zA-Z0-9_-]{6,16}]\s*$""")
    private val MEDIA_EXT = Regex(
        """\.(mp3|flac|m4a|aac|wav|ogg|opus|wma|alac|ape|mp4|mkv|webm|avi|mov|flv|wmv|m4v|3gp|ts)$""",
        RegexOption.IGNORE_CASE
    )
    private val YOUTUBE_NOISE = Regex(
        """(?i)\b(official\s+(music\s+)?video|official\s+audio|official\s+lyric\s+video|lyric\s+video|lyrics\s+video|lyrical\s+video|full\s+song\s+lyrics|full\s+song|full\s+video|video\s+song|music\s+video|lyrical|lyrics|audio|visualizer|remastered|remaster|4k|hd|8k|mv)\b"""
    )
    private val SEGMENT_DELIM = Regex("""\s*[|｜¦/／]\s*""")
    private val HYPHEN_DELIM = Regex("""\s*[-–—－]\s*""")
    private val UNKNOWN_ARTISTS = setOf("", "unknown", "unknown artist", "<unknown>", "various artists", "various")
    private val YOUTUBE_VIDEO = Regex(
        """(?:youtube\.com/watch\?(?:[^#]*&)?v=|music\.youtube\.com/watch\?(?:[^#]*&)?v=|youtu\.be/|youtube\.com/shorts/)([A-Za-z0-9_-]{11})"""
    )
    private val FEAT_MARKERS = arrayOf(" feat.", " ft.", " featuring", " Feat.", " Ft.", " feat ", " ft ")

    private fun cleanTitle(title: String): String =
        title.replace(MEDIA_EXT, "").replace(YOUTUBE_ID_SUFFIX, "").replace(TRACK_NO, "").trim()

    private fun superCleanTitle(title: String): String {
        val step1 = cleanTitle(title)
        val firstSeg = step1.split(SEGMENT_DELIM).firstOrNull()?.trim().orEmpty().ifBlank { step1 }
        val step2 = BRACKETED.replace(firstSeg, "").trim()
        val step3 = step2.split(*FEAT_MARKERS).first().trim()
        val step4 = YOUTUBE_NOISE.replace(step3, "").trim()
        return step4.ifBlank { step3.ifBlank { step1 } }
    }

    private fun cleanArtist(artist: String?): String {
        val raw = artist?.trim().orEmpty()
        if (raw.lowercase() in UNKNOWN_ARTISTS) return ""
        return raw.split(*FEAT_MARKERS).first().trim()
    }

    private fun isLikelyChannelOrLabel(artist: String?): Boolean {
        val lower = artist?.trim().orEmpty().lowercase()
        if (lower.isBlank() || lower in UNKNOWN_ARTISTS) return true
        return lower.endsWith(" music") || lower.endsWith(" records") ||
            lower.endsWith(" official") || lower.endsWith(" entertainment") ||
            lower.endsWith(" channel") || lower.endsWith(" series") ||
            lower.endsWith(" films") || lower.endsWith(" company") ||
            lower.endsWith(" vevo") || lower.contains("t-series") ||
            lower.contains("tseries") || lower.contains("saregama") ||
            lower.contains("zee music") || lower.contains("sony music") ||
            lower.contains("tips official") || lower.contains("yrf") ||
            lower.contains("speed records") || lower.contains("geet mp3") ||
            lower.contains("desire music") || lower.contains("lofi girl") ||
            lower.contains("aditya music") || lower.contains("lahari music")
    }

    private fun buildRefs(rawTitle: String?, rawArtist: String?): List<TrackRef> {
        if (rawTitle.isNullOrBlank()) return emptyList()

        val baseTitle = cleanTitle(rawTitle)
        val segments = baseTitle.split(SEGMENT_DELIM).map { it.trim() }.filter { it.isNotBlank() }
        val firstSeg = segments.firstOrNull() ?: baseTitle
        val primaryTitle = superCleanTitle(firstSeg)

        val cleanRawArtist = cleanArtist(rawArtist)
        val metadataArtist = if (isLikelyChannelOrLabel(cleanRawArtist)) "" else cleanRawArtist

        val refs = LinkedHashMap<String, TrackRef>()
        fun add(title: String, artist: String) {
            val t = title.trim()
            if (t.isBlank()) return
            val ref = TrackRef(t, artist.trim())
            refs.putIfAbsent(ref.key(), ref)
        }

        if (metadataArtist.isNotBlank()) add(primaryTitle, metadataArtist)

        if (firstSeg.contains(HYPHEN_DELIM)) {
            val parts = firstSeg.split(HYPHEN_DELIM, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                val p0 = superCleanTitle(parts[0])
                val p1 = superCleanTitle(parts[1])
                // "A.R. Rahman - Raanjhanaa" and "Vaaroon - Mirzapur" are both common.
                add(p1, p0)
                add(p0, p1)
            }
        }

        for (otherSeg in segments.drop(1)) {
            val cleanSeg = YOUTUBE_NOISE.replace(BRACKETED.replace(otherSeg, "").trim(), "").trim()
            if (cleanSeg.isBlank() || isLikelyChannelOrLabel(cleanSeg)) continue
            val firstArtist = cleanArtist(cleanSeg)
            if (firstArtist.isNotBlank()) add(primaryTitle, firstArtist)
            add(primaryTitle, cleanSeg)
        }

        add(primaryTitle, "")
        val rawFirstClean = cleanTitle(firstSeg)
        if (rawFirstClean != primaryTitle) add(rawFirstClean, "")

        return refs.values.toList().take(MAX_REFS)
    }

    // ---------------------------------------------------------------------------------------
    // Disk cache
    // ---------------------------------------------------------------------------------------

    private fun cacheKey(ref: TrackRef, durationMs: Long, videoId: String?): String {
        val raw = "$CACHE_VERSION|${ref.key()}|${durationMs / 1000}|${videoId.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun readDisk(dir: File?, key: String): String? = try {
        val f = File(File(dir ?: return null, "lyrics"), "$key.lrc")
        if (f.isFile && f.length() > 0) f.readText(Charsets.UTF_8) else null
    } catch (_: Throwable) {
        null
    }

    private fun writeDisk(dir: File?, key: String, text: String) {
        try {
            val d = File(dir ?: return, "lyrics")
            d.mkdirs()
            File(d, "$key.lrc").writeText(text, Charsets.UTF_8)
        } catch (_: Throwable) {
        }
    }

    // ---------------------------------------------------------------------------------------
    // HTTP
    // ---------------------------------------------------------------------------------------

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
    }

    private fun buildUrl(base: String, vararg params: Pair<String, String?>): String? {
        val url = base.toHttpUrlOrNull() ?: return null
        val b = url.newBuilder()
        params.forEach { (k, v) -> if (!v.isNullOrBlank()) b.addQueryParameter(k, v) }
        return b.build().toString()
    }

    private suspend fun httpGet(url: String?, headers: Map<String, String> = emptyMap()): String? {
        if (url == null) return null
        return try {
            val req = Request.Builder().url(url)
                .header("User-Agent", AGENT)
                .header("Accept", "*/*")
                .apply { headers.forEach { (k, v) -> header(k, v) } }
                .get().build()
            client.newCall(req).await().use { r ->
                if (r.isSuccessful) r.body?.string()?.takeIf { it.isNotBlank() } else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        return text
            .replace(Regex("&#x([0-9a-fA-F]+);")) { m ->
                m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
            }
            .replace(Regex("""&#(\d+);""")) { m ->
                m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
            }
            .replace("&apos;", "'").replace("&quot;", "\"").replace("&nbsp;", " ")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
    }

    // ---------------------------------------------------------------------------------------
    // Verification: reject another song / edit / artist (MpvRx LyricsTrackMatcher)
    // ---------------------------------------------------------------------------------------

    private object TrackMatcher {
        private const val MAX_DURATION_DIFFERENCE_SECONDS = 12.0
        private val BRACKETS = Regex("""\([^)]*\)|\[[^]]*]|\{[^}]*}""")
        private val TRAILING_FEAT = Regex("""\s+(?:feat\.?|ft\.?|featuring)\s+.+$""", RegexOption.IGNORE_CASE)
        private val DASH = Regex("""\s+[-–—]\s+""")
        private val MARKS = Regex("""\p{Mn}+""")
        private val NON_ALNUM = Regex("""[^\p{L}\p{N}]+""")
        private val SPACES = Regex("""\s+""")
        private val ARTIST_SPLIT = Regex(
            """\s*(?:,|&|;|/|\+|\band\b|\bwith\b|\bfeat\.?|\bft\.?|\bx\b)\s*""",
            RegexOption.IGNORE_CASE
        )
        private val VERSION_WORDS = setOf(
            "live", "remix", "remixed", "mix", "acoustic", "unplugged",
            "instrumental", "karaoke", "demo", "edit", "cover", "sped",
            "slowed", "nightcore", "lofi", "extended", "remaster", "remastered"
        )

        fun matches(
            ref: TrackRef,
            title: String?,
            artist: String?,
            durationSeconds: Int = 0,
            resultDurationSeconds: Double = 0.0
        ): Boolean {
            if (ref.title.isBlank() || title.isNullOrBlank()) return false
            if (core(ref.title) != core(title)) return false
            if (versions(ref.title) != versions(title)) return false
            if (ref.artist.isNotBlank()) {
                if (artist.isNullOrBlank()) return false
                val wanted = artists(ref.artist)
                val found = artists(artist)
                if (wanted.isEmpty() || found.none { it in wanted }) return false
            }
            return durationSeconds <= 0 || resultDurationSeconds <= 0 ||
                abs(resultDurationSeconds - durationSeconds) <= MAX_DURATION_DIFFERENCE_SECONDS
        }

        private fun core(title: String): String {
            val plain = BRACKETS.replace(title, " ").replace(TRAILING_FEAT, "").trim()
            val parts = plain.split(DASH, limit = 2)
            val without = if (parts.size > 1 && versions(parts[1]).isNotEmpty()) parts[0] else plain
            return fold(without)
        }

        private fun versions(title: String): Set<String> =
            fold(title).split(' ').filter { it in VERSION_WORDS }.toSet()

        private fun artists(artist: String): Set<String> =
            artist.split(ARTIST_SPLIT).map(::fold).filter { it.isNotEmpty() }.toSet()

        private fun fold(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(MARKS, "")
                .lowercase(Locale.ROOT)
                .replace(NON_ALNUM, " ")
                .replace(SPACES, " ")
                .trim()
    }

    // ---------------------------------------------------------------------------------------
    // Providers
    // ---------------------------------------------------------------------------------------

    /** LRCLIB: exact get, then fuzzy searches; every hit verified; synced beats plain. */
    private object LrcLibProvider : Provider {
        override val name = "LRCLIB"
        private const val BASE = "https://lrclib.net/api"
        private const val MAX_ATTEMPTS = 8

        private class Hit(
            val title: String,
            val artist: String,
            val duration: Double,
            val synced: String?,
            val plain: String?
        )

        private fun hit(o: JSONObject) = Hit(
            title = o.optString("trackName").ifBlank { o.optString("name") },
            artist = o.optString("artistName"),
            duration = o.optDouble("duration", 0.0).let { if (it.isNaN()) 0.0 else it },
            synced = o.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" },
            plain = o.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
        )

        private suspend fun get(track: String, artist: String, duration: Int?): Hit? {
            val url = buildUrl(
                "$BASE/get", "track_name" to track, "artist_name" to artist,
                "duration" to duration?.takeIf { it > 0 }?.toString()
            )
            val body = httpGet(url) ?: return null
            return try {
                hit(JSONObject(body))
            } catch (_: Exception) {
                null
            }
        }

        private suspend fun search(q: String? = null, track: String? = null, artist: String? = null): List<Hit> {
            val url = buildUrl("$BASE/search", "q" to q, "track_name" to track, "artist_name" to artist)
            val body = httpGet(url) ?: return emptyList()
            return try {
                val arr = JSONArray(body)
                (0 until arr.length()).map { hit(arr.getJSONObject(it)) }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun bestHit(hits: List<Hit>, ref: TrackRef, seconds: Int): String? {
            val matched = hits.filter { TrackMatcher.matches(ref, it.title, it.artist, seconds, it.duration) }
            val syncedHits = matched.filter { it.synced != null }
            val ordered = syncedHits.ifEmpty { matched.filter { it.plain != null } }
            if (ordered.isEmpty()) return null
            val best = if (seconds > 0) {
                ordered.minByOrNull { if (it.duration > 0) abs(it.duration - seconds) else 20.0 } ?: ordered.first()
            } else {
                ordered.firstOrNull { it.synced != null } ?: ordered.first()
            }
            return best.synced ?: best.plain
        }

        private fun matchingRef(ref: TrackRef, q: Query) =
            if (ref.artist.isBlank() && q.primary.artist.isNotBlank()) ref.copy(artist = q.primary.artist) else ref

        override suspend fun fetch(q: Query): String? {
            val duration = (q.durationMs / 1000).toInt()
            var plainFallback: String? = null
            var attempts = 0

            // Returns the text only when it is time-synced; plain text is remembered as fallback.
            fun syncedOrRemember(raw: String?): String? {
                if (raw == null) return null
                if (LrcParser.isSynced(raw)) return raw
                if (plainFallback == null) plainFallback = raw
                return null
            }

            for (ref in q.candidates()) {
                if (attempts >= MAX_ATTEMPTS) break

                if (duration > 0 && ref.artist.isNotBlank()) {
                    attempts++
                    val exact = get(ref.title, ref.artist, duration) ?: get(ref.title, ref.artist, null)
                    val verified = exact?.takeIf {
                        TrackMatcher.matches(ref, it.title, it.artist, duration, it.duration)
                    }
                    syncedOrRemember(verified?.synced)?.let { return it }
                    if (plainFallback == null) plainFallback = verified?.plain
                }

                if (attempts < MAX_ATTEMPTS) {
                    attempts++
                    val hits = if (ref.artist.isNotBlank()) search(track = ref.title, artist = ref.artist)
                    else search(q = ref.title)
                    syncedOrRemember(bestHit(hits, matchingRef(ref, q), duration))?.let { return it }
                }

                if (ref.artist.isNotBlank() && attempts < MAX_ATTEMPTS) {
                    attempts++
                    val hits = search(q = "${ref.artist} ${ref.title}")
                    syncedOrRemember(bestHit(hits, matchingRef(ref, q), duration))?.let { return it }
                }
            }
            return plainFallback
        }
    }

    /** SimpMusic: community database keyed on the YouTube video id (never the wrong edit). */
    private object SimpMusicProvider : Provider {
        override val name = "SimpMusic"
        private val ID = Regex("""[A-Za-z0-9_-]{11}""")

        override suspend fun fetch(q: Query): String? {
            val videoId = q.videoId?.takeIf { ID.matches(it) } ?: return null
            val body = httpGet("https://api-lyrics.simpmusic.org/v1/$videoId") ?: return null
            val root = try {
                JSONObject(body)
            } catch (_: Exception) {
                return null
            }
            if (!root.optBoolean("success", false)) return null
            val data = root.optJSONArray("data") ?: return null
            val seconds = (q.durationMs / 1000).toInt()
            val tracks = (0 until data.length()).map { data.getJSONObject(it) }
                .filter { seconds <= 0 || abs(it.optInt("duration", 0) - seconds) <= 10 }
                .sortedBy { abs(it.optInt("duration", 0) - seconds) }
            for (t in tracks) {
                val synced = t.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
                if (synced != null) return decodeEntities(synced)
            }
            for (t in tracks) {
                val plain = t.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
                if (plain != null) return decodeEntities(plain)
            }
            return null
        }
    }

    /** KuGou: line-synced, strong for Hindi and non-English catalogue. */
    private object KuGouProvider : Provider {
        override val name = "KuGou"
        private const val DURATION_TOLERANCE_SECONDS = 8
        private val STAMPED = Regex("""\[\d{2}:\d{2}\.\d{2,3}].*""")
        private val CREDIT = Regex(""".+][^\[]+[:：].+""")

        private fun stripParenthetical(s: String) = s.replace(Regex("""[(（].*?[)）]"""), "").trim().ifBlank { s }

        private fun keyword(ref: TrackRef, album: String?) = buildString {
            append(stripParenthetical(ref.title))
            append(" - ")
            append(stripParenthetical(ref.artist))
            if (!album.isNullOrBlank()) append(' ').append(album)
        }

        private suspend fun searchSongs(keyword: String, seconds: Int): List<String>? {
            val url = buildUrl(
                "https://mobileservice.kugou.com/api/v3/search/song",
                "version" to "9108", "plat" to "0", "pagesize" to "8", "showtype" to "0", "keyword" to keyword
            )
            val body = httpGet(url) ?: return null
            return try {
                val info = JSONObject(body).optJSONObject("data")?.optJSONArray("info") ?: return null
                (0 until info.length()).map { info.getJSONObject(it) }
                    .filter { seconds <= 0 || abs(it.optInt("duration", -1) - seconds) <= DURATION_TOLERANCE_SECONDS }
                    .sortedBy { abs(it.optInt("duration", -1) - seconds) }
                    .mapNotNull { it.optString("hash").takeIf { h -> h.isNotBlank() } }
            } catch (_: Exception) {
                null
            }
        }

        private suspend fun searchLyrics(
            hash: String? = null,
            keyword: String? = null,
            seconds: Int = -1
        ): Pair<String, String>? {
            if (hash == null && keyword == null) return null
            val url = buildUrl(
                "https://lyrics.kugou.com/search",
                "ver" to "1", "man" to "yes", "client" to "pc", "hash" to hash, "keyword" to keyword,
                "duration" to seconds.takeIf { it > 0 }?.let { (it * 1000).toString() }
            )
            val body = httpGet(url) ?: return null
            return try {
                val c = JSONObject(body).optJSONArray("candidates") ?: return null
                if (c.length() == 0) return null
                val first = c.getJSONObject(0)
                first.optString("id") to first.optString("accesskey")
            } catch (_: Exception) {
                null
            }
        }

        private suspend fun download(id: String, accessKey: String): String? {
            val url = buildUrl(
                "https://lyrics.kugou.com/download",
                "fmt" to "lrc", "charset" to "utf8", "client" to "pc", "ver" to "1",
                "id" to id, "accesskey" to accessKey
            )
            val body = httpGet(url) ?: return null
            return try {
                val content = JSONObject(body).optString("content")
                if (content.isBlank()) return null
                stripCredits(String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8))
            } catch (_: Exception) {
                null
            }
        }

        /** Songwriter / composer credit lines carry real stamps; cut them from both ends. */
        private fun stripCredits(text: String): String {
            val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { STAMPED.matches(it) }.toList()
            if (lines.isEmpty()) return ""
            val headLimit = minOf(30, lines.lastIndex)
            val headCut = (headLimit downTo 0).firstOrNull { CREDIT.matches(lines[it]) }?.let { it + 1 } ?: 0
            val body = lines.drop(headCut)
            val tailLimit = minOf(30, body.lastIndex)
            val tailCut = (0..tailLimit).firstOrNull { CREDIT.matches(body[body.lastIndex - it]) }?.let { it + 1 } ?: 0
            return body.dropLast(tailCut).joinToString("\n")
        }

        override suspend fun fetch(q: Query): String? {
            val seconds = (q.durationMs / 1000).toInt()
            for (ref in q.candidates()) {
                val kw = keyword(ref, q.album)
                val candidate = searchSongs(kw, seconds)
                    ?.firstNotNullOfOrNull { hash -> searchLyrics(hash = hash) }
                    ?: searchLyrics(keyword = kw, seconds = seconds)
                    ?: continue
                val lrc = download(candidate.first, candidate.second) ?: continue
                val decoded = decodeEntities(lrc)
                if (LrcParser.parse(decoded).isNotEmpty()) return decoded
            }
            return null
        }
    }

    /** Megalobiz: community-made line-synced LRC (scraped). */
    private object MegalobizProvider : Provider {
        override val name = "Megalobiz"
        private const val BASE = "https://www.megalobiz.com"
        private val LRC_LINK = Regex("""href=["'](/lrc/maker/download/[^"']+)["']""", RegexOption.IGNORE_CASE)
        private val LRC_BODY = Regex(
            """id=["']lrc_[^"']*_details["'][^>]*>(.*?)</span>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        override suspend fun fetch(q: Query): String? {
            for (ref in q.candidates(limit = 3)) {
                val search = buildUrl("$BASE/searchall", "qry" to "${ref.artist} ${ref.title}".trim()) ?: continue
                val results = httpGet(search) ?: continue
                val path = LRC_LINK.find(results)?.groupValues?.get(1) ?: continue
                val page = httpGet(BASE + path.replace("&amp;", "&")) ?: continue
                val raw = LRC_BODY.find(page)?.groupValues?.get(1) ?: continue
                val lrc = decodeEntities(
                    raw.replace(Regex("""(?i)<br\s*/?>"""), "\n").replace(Regex("<[^>]+>"), "")
                )
                if (LrcParser.parse(lrc).isNotEmpty()) return lrc
            }
            return null
        }
    }
}
