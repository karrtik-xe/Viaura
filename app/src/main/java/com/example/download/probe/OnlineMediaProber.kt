package com.example.download

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.util.NetworkSettings
import com.example.util.OnlineVideoService
import com.example.ytdlp.YtdlPreferences
import com.example.ytdlp.YtdlpManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object MediaProbe {
    private val audioExts = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "oga", "weba", "mka", "wma", "ac3")

    private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")
    private fun JSONObject.lng(key: String): Long = if (isNull(key)) 0L else try { optDouble(key, 0.0).toLong() } catch (_: Throwable) { 0L }
    private fun JSONObject.dbl(key: String): Double = if (isNull(key)) 0.0 else optDouble(key, 0.0)

    fun codecName(codec: String): String {
        val c = codec.lowercase()
        return when {
            c.startsWith("avc") || c.startsWith("h264") -> "H.264"
            c.startsWith("vp09") || c.startsWith("vp9") -> "VP9"
            c.startsWith("vp8") -> "VP8"
            c.startsWith("av01") || c.startsWith("av1") -> "AV1"
            c.startsWith("hev") || c.startsWith("hvc") || c.startsWith("h265") -> "HEVC"
            c.startsWith("mp4a") || c == "aac" -> "AAC"
            c.startsWith("opus") -> "Opus"
            c.startsWith("vorbis") -> "Vorbis"
            c.startsWith("mp3") || c == "mp3" -> "MP3"
            c.startsWith("ec-3") || c.startsWith("eac3") -> "E-AC3"
            c.startsWith("ac-3") || c == "ac3" -> "AC3"
            c.startsWith("flac") -> "FLAC"
            c.isBlank() || c == "none" -> ""
            else -> codec.substringBefore('.').uppercase(Locale.ROOT)
        }
    }

    /** "hi" -> "Hindi". Falls back to the website's own note. */
    fun languageName(lang: String, note: String = ""): String {
        val code = lang.trim()
        if (code.isNotEmpty() && code != "und") {
            val name = try { Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH) } catch (_: Throwable) { "" }
            if (name.isNotBlank() && !name.equals(code, true)) return name
        }
        val cleaned = note.substringBefore(',').replace(Regex("(?i)\\b(original|default|dubbed|auto|descriptive|\\(.*?\\))"), "").trim()
        return cleaned.ifBlank { code.ifBlank { "Audio" } }
    }

    private fun parseFormat(o: JSONObject, topHeaders: Map<String, String>, duration: Long): RawFormat? {
        val url = o.str("url")
        val protocol = o.str("protocol").ifBlank { "https" }
        val ext = o.str("ext")
        if (ext == "mhtml" || protocol == "mhtml" || o.str("format_note").contains("storyboard", true)) return null
        if (o.optBoolean("has_drm", false)) return null
        val fragments = ArrayList<String>()
        val fragArr = o.optJSONArray("fragments")
        val base = o.str("fragment_base_url")
        if (fragArr != null && protocol.contains("dash", true)) {
            for (i in 0 until fragArr.length()) {
                val f = fragArr.optJSONObject(i) ?: continue
                val u = f.str("url").ifBlank { f.str("path") }
                if (u.isBlank()) continue
                fragments.add(if (u.startsWith("http", true) || base.isBlank()) u else base + u)
            }
        }
        if (url.isBlank() && fragments.isEmpty()) return null
        // only streams the engine can really fetch are offered (plain files, HLS, DASH fragments)
        val supported = (protocol.startsWith("http") && !protocol.contains("generator")) || protocol.contains("m3u8")
        if (!supported) return null
        if (protocol.contains("dash") && fragments.isEmpty()) return null
        var vcodec = o.str("vcodec")
        val acodec = o.str("acodec")
        // a file that is clearly audio (m4a / mp3 / opus ...) but whose video codec is not listed is audio only
        if (vcodec.isBlank() && ext.lowercase() in audioExts && !acodec.equals("none", true)) vcodec = "none"
        var size = o.lng("filesize")
        var approx = false
        if (size <= 0L) {
            size = o.lng("filesize_approx")
            approx = size > 0L
        }
        val tbr = o.dbl("tbr")
        if (size <= 0L && tbr > 0.0 && duration > 0L) {
            size = (tbr * 1000.0 / 8.0 * duration).toLong()
            approx = true
        }
        val headers = LinkedHashMap<String, String>()
        headers.putAll(topHeaders)
        o.optJSONObject("http_headers")?.let { h -> h.keys().forEach { k -> headers[k] = h.optString(k) } }
        o.str("cookies").takeIf { it.isNotBlank() }?.let { headers["Cookie"] = it }
        val chunk = o.optJSONObject("downloader_options")?.lng("http_chunk_size") ?: 0L
        val dynamic = o.str("dynamic_range")
        return RawFormat(
            id = o.str("format_id"), url = url, ext = ext, protocol = protocol, vcodec = vcodec, acodec = acodec,
            width = o.lng("width").toInt(), height = o.lng("height").toInt(), fps = Math.round(o.dbl("fps")).toInt(),
            sizeBytes = size, sizeApprox = approx, tbr = tbr, abr = o.dbl("abr"), language = o.str("language"),
            note = o.str("format_note"), headers = headers, chunkSize = chunk,
            hdr = dynamic.isNotBlank() && !dynamic.equals("SDR", true), fragments = fragments,
            languagePreference = if (o.isNull("language_preference")) 0 else o.optInt("language_preference", 0)
        )
    }

    private fun videoRank(f: RawFormat): Int {
        val c = codecName(f.vcodec)
        return when {
            f.height > 1080 -> when (c) { "VP9" -> 0; "AV1" -> 1; "HEVC" -> 2; "H.264" -> 3; else -> 4 }
            else -> when (c) { "H.264" -> 0; "VP9" -> 1; "HEVC" -> 2; "AV1" -> 3; else -> 4 }
        }
    }

    fun videoLabel(height: Int, fps: Int, hdr: Boolean, fallback: String): String {
        if (height <= 0) return fallback.ifBlank { "Original" }
        val base = if (fps > 30) "${height}p$fps" else "${height}p"
        return if (hdr) "$base HDR" else base
    }

    private fun isDrcDuplicate(f: RawFormat): Boolean = f.id.contains("drc", true) || f.note.contains("DRC", false)

    /** Builds the lists the sheet shows from the answer of `yt-dlp -J`. */
    fun parseInfo(json: JSONObject, pageUrl: String, includeAutoSubs: Boolean = true, allowLive: Boolean = false): LinkInfo {
        if (!allowLive && json.optBoolean("is_live", false)) throw IllegalStateException("Live streams can't be downloaded")
        val duration = json.lng("duration")
        val topHeaders = LinkedHashMap<String, String>()
        json.optJSONObject("http_headers")?.let { h -> h.keys().forEach { k -> topHeaders[k] = h.optString(k) } }
        val formats = ArrayList<RawFormat>()
        val arr = json.optJSONArray("formats") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val f = parseFormat(arr.optJSONObject(i) ?: continue, topHeaders, duration) ?: continue
            formats.add(f)
        }
        // a link that is itself one file (generic extractor) has no "formats" list, only the top-level fields
        if (formats.isEmpty() && json.str("url").isNotBlank()) {
            parseFormat(json, topHeaders, duration)?.let { formats.add(it) }
        }
        if (formats.isEmpty()) throw IllegalStateException("No downloadable stream was found for this link")

        // ---- audio tracks ----
        val audioOnly = formats.filter { it.hasAudio && !it.hasVideo && !isDrcDuplicate(it) }
        val groups = LinkedHashMap<String, MutableList<RawFormat>>()
        for (f in audioOnly) {
            val dubbed = f.note.contains("dubbed", true)
            groups.getOrPut(f.language.ifBlank { "und" } + if (dubbed) "|d" else "|o") { ArrayList() }.add(f)
        }
        fun bestOf(list: List<RawFormat>): RawFormat =
            list.sortedWith(compareByDescending<RawFormat> { it.abr.takeIf { a -> a > 0 } ?: it.tbr }.thenBy { if (it.acodec.startsWith("mp4a")) 0 else 1 }).first()
        var originalKey = groups.keys.firstOrNull { k -> groups[k]!!.any { it.note.contains("original", true) } }
        if (originalKey == null && groups.size > 1) {
            val maxPref = groups.values.maxOf { g -> g.maxOf { it.languagePreference } }
            val minPref = groups.values.minOf { g -> g.maxOf { it.languagePreference } }
            if (maxPref != minPref) originalKey = groups.keys.firstOrNull { k -> groups[k]!!.maxOf { it.languagePreference } == maxPref }
        }
        if (originalKey == null) originalKey = groups.keys.firstOrNull { !it.endsWith("|d") } ?: groups.keys.firstOrNull()
        val tracks = ArrayList<AudioChoice>()
        val usedNames = HashSet<String>()
        val orderedKeys = listOfNotNull(originalKey) + groups.keys.filter { it != originalKey }
        for (k in orderedKeys) {
            val best = bestOf(groups[k]!!)
            val isOriginal = k == originalKey
            var name = if (isOriginal) "Original" else languageName(best.language, best.note)
            var n = 2
            val base = name
            while (!usedNames.add(name.lowercase())) name = "$base ${n++}"
            tracks.add(
                AudioChoice(
                    key = k, label = name, language = best.language, isOriginal = isOriginal,
                    bitrateKbps = Math.round(best.abr.takeIf { it > 0 } ?: best.tbr).toInt(), codecName = codecName(best.acodec),
                    ext = best.ext, sizeBytes = best.sizeBytes, sizeApprox = best.sizeApprox, format = best
                )
            )
        }
        // every quality of the original track, no duplicates (same container + same bitrate)
        val originalList = originalKey?.let { groups[it] }.orEmpty()
        val qualities = originalList
            .sortedWith(compareByDescending<RawFormat> { it.abr.takeIf { a -> a > 0 } ?: it.tbr }.thenBy { if (it.acodec.startsWith("mp4a")) 0 else 1 })
            .distinctBy { it.ext + "|" + Math.round(it.abr.takeIf { a -> a > 0 } ?: it.tbr) + "|" + codecName(it.acodec) }
            .map { f ->
                AudioChoice(
                    key = f.id, label = "Original", language = f.language, isOriginal = true,
                    bitrateKbps = Math.round(f.abr.takeIf { it > 0 } ?: f.tbr).toInt(), codecName = codecName(f.acodec),
                    ext = f.ext, sizeBytes = f.sizeBytes, sizeApprox = f.sizeApprox, format = f
                )
            }

        // ---- video qualities ----
        val hasSeparateAudio = tracks.isNotEmpty()
        val vids = formats.filter { it.hasVideo }.ifEmpty {
            // nothing says it holds video (plain direct link): a file with unknown codecs is offered as the video
            formats.filter { !it.hasVideo && !it.hasAudio && it.ext !in audioExts }
        }
        val byLabel = LinkedHashMap<String, MutableList<RawFormat>>()
        for (f in vids) {
            byLabel.getOrPut("${f.height}|${if (f.fps > 30) f.fps else 30}|${f.hdr}") { ArrayList() }.add(f)
        }
        val videos = byLabel.values.map { list ->
            val adaptive = list.filter { !it.hasAudio }
            val pool = if (hasSeparateAudio && adaptive.isNotEmpty()) adaptive else list
            val best = pool.sortedWith(compareBy<RawFormat> { videoRank(it) }.thenByDescending { it.tbr }).first()
            val muxed = best.hasAudio
            VideoChoice(
                key = best.id, label = videoLabel(best.height, best.fps, best.hdr, best.note), height = best.height, fps = best.fps, hdr = best.hdr,
                codecName = codecName(best.vcodec), ext = best.ext, sizeBytes = best.sizeBytes, sizeApprox = best.sizeApprox,
                muxed = muxed, format = best
            )
        }.sortedWith(compareByDescending<VideoChoice> { it.height }.thenByDescending { it.fps }.thenBy { if (it.hdr) 0 else 1 })

        // ---- subtitles ----
        val subs = ArrayList<PlanSubtitle>()
        fun pickSub(arr: JSONArray?): JSONObject? {
            if (arr == null) return null
            for (want in listOf("vtt", "srt", "ass", "ssa")) {
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    if (e.str("ext").equals(want, true) && e.str("url").isNotBlank()) return e
                }
            }
            return null
        }
        json.optJSONObject("subtitles")?.let { s ->
            s.keys().forEach { lang ->
                if (lang == "live_chat") return@forEach
                val e = pickSub(s.optJSONArray(lang)) ?: return@forEach
                subs.add(PlanSubtitle(lang, e.str("name").ifBlank { languageName(lang) }, e.str("url"), e.str("ext").lowercase(), false))
            }
        }
        if (includeAutoSubs) {
            val auto = json.optJSONObject("automatic_captions")
            if (auto != null) {
                val videoLang = json.str("language")
                val keys = auto.keys().asSequence().toList()
                val chosen = keys.filter { it.endsWith("-orig") }.ifEmpty { keys.filter { videoLang.isNotBlank() && it.equals(videoLang, true) } }
                for (lang in chosen) {
                    if (subs.any { it.language.equals(lang.removeSuffix("-orig"), true) }) continue
                    val e = pickSub(auto.optJSONArray(lang)) ?: continue
                    subs.add(PlanSubtitle(lang, e.str("name").ifBlank { languageName(lang.removeSuffix("-orig")) + " (auto)" }, e.str("url"), e.str("ext").lowercase(), true))
                }
            }
        }
        return buildInfo(json, pageUrl, duration, videos, tracks, qualities, subs, hasSeparateAudio)
    }

    private fun buildInfo(
        json: JSONObject, pageUrl: String, duration: Long, videos: List<VideoChoice>, tracks: List<AudioChoice>,
        qualities: List<AudioChoice>, subs: List<PlanSubtitle>, @Suppress("UNUSED_PARAMETER") hasSeparateAudio: Boolean
    ): LinkInfo {
        val chapters = ArrayList<PlanChapter>()
        json.optJSONArray("chapters")?.let { a ->
            for (i in 0 until a.length()) {
                val c = a.optJSONObject(i) ?: continue
                val start = (c.dbl("start_time") * 1000).toLong()
                val end = (c.dbl("end_time") * 1000).toLong()
                chapters.add(PlanChapter(c.str("title").ifBlank { "Chapter ${i + 1}" }, start, end))
            }
        }
        val thumb = json.str("thumbnail").ifBlank {
            val ts = json.optJSONArray("thumbnails")
            var best = ""
            var bestScore = Long.MIN_VALUE
            if (ts != null) for (i in 0 until ts.length()) {
                val t = ts.optJSONObject(i) ?: continue
                val score = t.lng("preference") * 1_000_000L + t.lng("width") * t.lng("height")
                if (t.str("url").isNotBlank() && score > bestScore) { best = t.str("url"); bestScore = score }
            }
            best
        }
        val audioOnlySource = videos.isEmpty() || (json.str("vcodec") == "none")
        return LinkInfo(
            pageUrl = pageUrl,
            title = json.str("title").ifBlank { json.str("fulltitle") }.ifBlank { "Download" },
            uploader = json.str("uploader").ifBlank { json.str("channel") },
            thumbnailUrl = thumb, durationSec = duration, videos = videos, audioTracks = tracks, audioQualities = qualities,
            subtitles = subs, chapters = chapters, isAudioOnlySource = audioOnlySource
        )
    }

    /** Full answer for [url]. yt-dlp first (18000+ sites); a plain file link is read with a HEAD request. */
    suspend fun probe(
        context: Context, url: String, extraHeaders: Map<String, String> = emptyMap(),
        /** Playback only needs the streams, not their sizes: skipping the size requests makes a link open sooner. */
        forPlayback: Boolean = false,
        /** false = return as soon as the website answered; the real sizes are then measured separately with [refineSizes]. */
        measureSizes: Boolean = true,
        /** false = always ask the website again (a retry, or fresh addresses for a download whose links expired). */
        useCache: Boolean = true
    ): Result<LinkInfo> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            if (useCache) cached(url, extraHeaders, forPlayback)?.let { return@withContext Result.success(it) }
            val result = probeUncached(app, url, extraHeaders, forPlayback, measureSizes)
            result.getOrNull()?.let { remember(url, extraHeaders, forPlayback, it) }
            result
        }

    // The answer of a website (qualities + addresses) stays valid for a good while: opening the same link again shows the sheet at once.
    private class CacheEntry(val info: LinkInfo, val at: Long)
    private val infoCache = object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean = size > 24
    }
    private const val CACHE_TTL_MS = 8 * 60_000L

    private fun cacheKey(url: String, headers: Map<String, String>, forPlayback: Boolean) =
        url + "\u0000" + headers.entries.sortedBy { it.key }.joinToString("&") { it.key + "=" + it.value } + "\u0000" + forPlayback

    private fun cached(url: String, headers: Map<String, String>, forPlayback: Boolean): LinkInfo? = synchronized(infoCache) {
        val e = infoCache[cacheKey(url, headers, forPlayback)] ?: return null
        if (System.currentTimeMillis() - e.at > CACHE_TTL_MS) { infoCache.remove(cacheKey(url, headers, forPlayback)); null } else e.info
    }

    /** Also used by the sheet to store the version whose sizes were measured. */
    fun remember(url: String, headers: Map<String, String>, forPlayback: Boolean, info: LinkInfo) {
        synchronized(infoCache) { infoCache[cacheKey(url, headers, forPlayback)] = CacheEntry(info, System.currentTimeMillis()) }
    }

    private suspend fun probeUncached(
        app: Context, url: String, extraHeaders: Map<String, String>, forPlayback: Boolean, measureSizes: Boolean
    ): Result<LinkInfo> =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(url)
                val isDirect = com.example.ytdlp.YtdlpUrlRules.isDirectMediaUrl(uri) && !url.contains(".m3u8", true) && !url.contains(".mpd", true)
                if (isDirect) {
                    val direct = directInfo(app, url, extraHeaders)
                    return@withContext Result.success(if (forPlayback || !measureSizes) direct else refine(app, direct))
                }
                var failure: Throwable? = null
                if (BuildConfig.ENABLE_YTDLP) {
                    if (!YtdlpManager.isInstalled(app) || !YtdlpManager.ensureRuntimeInstalled(app)) {
                        failure = IllegalStateException("yt-dlp is not installed. Install it from Settings > Network.")
                    } else {
                        val r = YtdlpManager.dumpSingleJson(app, url, YtdlPreferences.get(app), extraHeaders)
                        val text = r.getOrNull()
                        if (text != null) {
                            val parsed = parseInfo(JSONObject(text), url, NetworkSettings.autoSubtitles(app), allowLive = forPlayback)
                            return@withContext Result.success(if (forPlayback || !measureSizes) parsed else refine(app, parsed))
                        }
                        failure = r.exceptionOrNull()
                    }
                }
                // YouTube still works without yt-dlp (NewPipeExtractor, the engine of Online Video)
                val yt = YtdlpUrlRulesBridge.isYouTube(url)
                if (yt) {
                    newPipeInfo(app, url)?.let { return@withContext Result.success(if (measureSizes) refine(app, it) else it) }
                }
                Result.failure(failure ?: IllegalStateException("This link can't be downloaded"))
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    /**
     * What went wrong with a link, in words the person can act on (instead of a bare "can't play"). The wording of yt-dlp's
     * own error is mapped to the usual causes; anything else is shown cleaned, so the real reason is never hidden.
     */
    fun friendlyReason(t: Throwable?): String {
        val raw = (t?.message ?: "").replace(Regex("\\u001B\\[[0-9;]*m"), "").trim()
        if (raw.isBlank()) return "This video can't play"
        val m = raw.lowercase()
        return when {
            "unsupported url" in m -> "No video was found on this page. Open the link of the video itself."
            "not installed" in m -> "Install yt-dlp from Settings > Network to play links of websites"
            "login required" in m || "log in" in m || "sign in" in m || "cookies" in m || "private" in m || "rate-limit" in m ||
                "authentication" in m || "restricted" in m || "members-only" in m || "age" in m && "verif" in m ->
                "This site asks for a login (private, age-restricted or rate-limited video). Add a cookies file in Settings > Network, then try again."
            "geo" in m || "not available in your country" in m || "your location" in m -> "This video is blocked in your country (a proxy / VPN may be needed)."
            "drm" in m -> "This video is DRM protected and can't be played."
            "unavailable" in m || "removed" in m || "deleted" in m || "does not exist" in m || "no longer available" in m || "not found" in m ->
                "This video is unavailable (removed, deleted or private)."
            "http error 403" in m || "403" in m && "forbidden" in m -> "The site refused the request (HTTP 403)."
            "http error 429" in m || "too many requests" in m -> "The site is limiting requests (HTTP 429). Wait a while and try again."
            "unable to resolve host" in m || "name or service not known" in m || "failed to resolve" in m || "nodename nor servname" in m ->
                "The site could not be reached (DNS problem). Check the internet connection."
            "timed out" in m || "timeout" in m || "took too long" in m -> "The site took too long to answer."
            "ssl" in m || "certificate" in m -> "A secure-connection (SSL) error happened while reading the link."
            "no downloadable stream" in m || "no formats" in m || "no video formats" in m -> "No playable video stream was found on this page."
            "live streams can't" in m -> raw
            else -> {
                val cleaned = raw.removePrefix("ERROR:").trim()
                    .replace(Regex("^\\[[^\\]]+\\]\\s*[^:\\s]*:\\s*"), "")
                    .lineSequence().firstOrNull().orEmpty().trim()
                if (cleaned.isBlank()) "This video can't play" else cleaned.take(180)
            }
        }
    }

    private object YtdlpUrlRulesBridge {
        fun isYouTube(url: String): Boolean {
            val host = try { Uri.parse(url).host?.lowercase().orEmpty() } catch (_: Throwable) { "" }
            return host.contains("youtube.com") || host == "youtu.be"
        }
    }

    private suspend fun directInfo(app: Context, url: String, headers: Map<String, String>): LinkInfo {
        val head = HttpFetch.head(app, url, headers)
        val name = (head?.fileName?.takeIf { it.isNotBlank() } ?: Uri.decode(Uri.parse(url).lastPathSegment.orEmpty())).ifBlank { "Download" }
        val ext = name.substringAfterLast('.', "").lowercase().ifBlank { extFromMime(head?.contentType) }
        val isAudio = ext in audioExts || (head?.contentType?.startsWith("audio/") == true)
        val title = name.substringBeforeLast('.', name)
        val f = RawFormat(
            id = "direct", url = head?.finalUrl ?: url, ext = ext, protocol = "http", vcodec = if (isAudio) "none" else "unknown",
            acodec = if (isAudio) "unknown" else "unknown", width = 0, height = 0, fps = 0, sizeBytes = head?.size ?: -1L, sizeApprox = false,
            tbr = 0.0, abr = 0.0, language = "", note = "", headers = headers, chunkSize = 0L, hdr = false, fragments = emptyList(), languagePreference = 0
        )
        return if (isAudio) {
            val a = AudioChoice("direct", "Original", "", true, 0, ext.uppercase(), ext, f.sizeBytes, false, f)
            LinkInfo(url, title, "", "", 0L, emptyList(), listOf(a), listOf(a), emptyList(), emptyList(), isDirect = true, isAudioOnlySource = true)
        } else {
            val v = VideoChoice("direct", "Original", 0, 0, false, "", ext, f.sizeBytes, false, true, f)
            LinkInfo(url, title, "", "", 0L, listOf(v), emptyList(), emptyList(), emptyList(), emptyList(), isDirect = true)
        }
    }

    private fun extFromMime(mime: String?): String = when {
        mime == null -> ""
        mime.contains("mp4") -> "mp4"
        mime.contains("matroska") -> "mkv"
        mime.contains("webm") -> "webm"
        mime.contains("mpeg") && mime.startsWith("audio") -> "mp3"
        mime.contains("quicktime") -> "mov"
        mime.contains("ogg") -> "ogg"
        else -> ""
    }

    private suspend fun newPipeInfo(app: Context, url: String): LinkInfo? {
        val pb = OnlineVideoService.resolvePlayback(url, forceRefresh = true) ?: return null
        val audios = ArrayList<AudioChoice>()
        pb.audios.forEachIndexed { i, a ->
            val f = RawFormat("audio$i", a.url, "m4a", "https", "none", "unknown", 0, 0, 0, -1L, false, 0.0, a.bitrate.toDouble(), a.language, a.title, emptyMap(), 10_485_760L, false, emptyList(), 0)
            audios.add(AudioChoice("audio$i", if (a.isOriginal) "Original" else a.title, a.language, a.isOriginal, a.bitrate, "", "m4a", -1L, false, f))
        }
        val videos = pb.qualities.map { q ->
            val f = RawFormat(q.label, q.url, "mp4", "https", "unknown", if (q.isVideoOnly) "none" else "unknown", 0, q.height, q.fps, -1L, false, 0.0, 0.0, "", "", emptyMap(), 10_485_760L, false, emptyList(), 0)
            VideoChoice(q.label, q.label, q.height, q.fps, false, "", "mp4", -1L, false, !q.isVideoOnly, f)
        }
        val chapters = pb.chapters.mapIndexed { i, c -> PlanChapter(c.title, c.startMs, pb.chapters.getOrNull(i + 1)?.startMs ?: (pb.durationSec * 1000L)) }
        return LinkInfo(url, pb.title, pb.uploader, "", pb.durationSec, videos, audios.take(8), audios.take(1), emptyList(), chapters)
    }

    /** Streams whose size the website did not tell are measured (one tiny ranged request each), so the sheet never shows a made-up number. */
    private suspend fun refine(app: Context, info: LinkInfo): LinkInfo = refineSizes(app, info, 60_000L)

    private val sizeScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    /**
     * Same measuring as before (one tiny ranged request per stream whose size is not exact), but all at once and with a time limit:
     * a stream whose host answers late keeps the size the website itself gave (shown as approximate) instead of holding the whole panel back.
     */
    suspend fun refineSizes(app: Context, info: LinkInfo, budgetMs: Long = 8_000L): LinkInfo {
        fun needs(f: RawFormat) = f.sizeBytes <= 0L || f.sizeApprox
        val protoHttp = { f: RawFormat -> f.protocol.startsWith("http") && !f.protocol.contains("dash") && f.url.isNotBlank() }
        // HLS / DASH streams: the website only lists a peak bitrate, so a size computed from it is far too big. A few real segments are measured instead.
        val segmented = { f: RawFormat -> (f.protocol.contains("m3u8") || (f.protocol.contains("dash") && f.fragments.isNotEmpty())) && f.url.isNotBlank() }
        val all = (info.videos.map { it.format } + info.audioTracks.map { it.format } + info.audioQualities.map { it.format })
        val targets = all.filter { needs(it) && protoHttp(it) }.distinctBy { it.url }.take(40)
        val segTargets = all.filter { needs(it) && segmented(it) }.distinctBy { it.id + it.url }.take(16)
        if (targets.isEmpty() && segTargets.isEmpty()) return info
        val sizes = java.util.concurrent.ConcurrentHashMap<String, Long>()
        val estimates = java.util.concurrent.ConcurrentHashMap<String, Long>()
        val jobs = targets.map { f -> sizeScope.launch { sizes[f.url] = HttpFetch.probeSize(app, f.url, f.headers) } } +
            segTargets.map { f ->
                sizeScope.launch {
                    val part = partOf(f, "video", "", "")
                    val e = SegmentFetch.estimateSize(app, part)
                    if (e > 0L) estimates[f.id + f.url] = e
                }
            }
        kotlinx.coroutines.withTimeoutOrNull(budgetMs) { jobs.forEach { it.join() } }
        jobs.forEach { if (it.isActive) it.cancel() }
        fun upd(f: RawFormat): RawFormat {
            val s = sizes[f.url]
            if (s != null && s > 0L) return f.copy(sizeBytes = s, sizeApprox = false)
            val e = estimates[f.id + f.url]
            // measured from real segments: shown with "~", and the download itself then measures the exact total while it runs
            if (e != null && e > 0L) return f.copy(sizeBytes = e, sizeApprox = true)
            return f
        }
        return info.copy(
            videos = info.videos.map { v -> val nf = upd(v.format); v.copy(format = nf, sizeBytes = nf.sizeBytes, sizeApprox = nf.sizeApprox) },
            audioTracks = info.audioTracks.map { a -> val nf = upd(a.format); a.copy(format = nf, sizeBytes = nf.sizeBytes, sizeApprox = nf.sizeApprox) },
            audioQualities = info.audioQualities.map { a -> val nf = upd(a.format); a.copy(format = nf, sizeBytes = nf.sizeBytes, sizeApprox = nf.sizeApprox) }
        )
    }

    /** Download plan of a video: the chosen quality, the chosen audio tracks (several = all of them go into one MKV), extras. */
    fun planForVideo(
        info: LinkInfo, video: VideoChoice, audios: List<AudioChoice>, subs: List<PlanSubtitle>,
        attachChapters: Boolean, attachThumbnail: Boolean
    ): DownloadPlan {
        val parts = ArrayList<PlanPart>()
        // a video that comes with no separate audio keeps whatever audio its own file holds ("muxed" takes every track;
        // the "video" role keeps only the picture, which made websites without a separate audio list download silent)
        val withSeparateAudio = !video.muxed && audios.isNotEmpty()
        parts.add(partOf(video.format, if (withSeparateAudio) "video" else "muxed", "", ""))
        // video-only: the chosen audio tracks are attached; a video that already has its sound can take extra languages
        audios.forEachIndexed { i, a ->
            parts.add(partOf(a.format, "audio", a.label, a.language, isDefault = withSeparateAudio && i == 0))
        }
        return DownloadPlan(
            pageUrl = info.pageUrl, title = info.title, uploader = info.uploader, thumbnailUrl = info.thumbnailUrl,
            durationSec = info.durationSec, isAudio = false, parts = parts, subtitles = subs,
            chapters = if (attachChapters) info.chapters else emptyList(), attachThumbnail = attachThumbnail, qualityLabel = video.label
        )
    }

    fun planForAudio(info: LinkInfo, audio: AudioChoice, attachThumbnail: Boolean, lyrics: Boolean): DownloadPlan {
        val role = if (audio.format.hasVideo) "muxed" else "audio"
        return DownloadPlan(
            pageUrl = info.pageUrl, title = info.title, uploader = info.uploader, thumbnailUrl = info.thumbnailUrl,
            durationSec = info.durationSec, isAudio = true, parts = listOf(partOf(audio.format, role, audio.label, audio.language, true)),
            attachThumbnail = attachThumbnail, fetchLyrics = lyrics,
            qualityLabel = if (audio.bitrateKbps > 0) "${audio.bitrateKbps} kbps ${audio.codecName}".trim() else audio.codecName.ifBlank { audio.ext.uppercase() }
        )
    }

    private fun partOf(f: RawFormat, role: String, label: String, lang: String, isDefault: Boolean = false): PlanPart {
        val protocol = when {
            f.protocol.contains("m3u8") -> "hls"
            f.protocol.contains("dash") && f.fragments.isNotEmpty() -> "dash"
            f.protocol.startsWith("http") -> "http"
            else -> "ytdlp"
        }
        return PlanPart(
            role = role, url = f.url, headers = f.headers, sizeBytes = if (f.sizeApprox) -1L else f.sizeBytes, ext = f.ext, protocol = protocol, formatId = f.id,
            label = label, language = lang, isDefault = isDefault, chunkSize = f.chunkSize, fragments = f.fragments,
            codec = f.vcodec.takeIf { it.isNotBlank() && it != "none" } ?: f.acodec
        )
    }
}
