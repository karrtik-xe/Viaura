package com.example.util.media

import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import java.net.URL

/** Reads the renditions (the quality list) out of an HLS master playlist. */
object HlsVariants {
    fun looksLikeHls(link: String, type: String?): Boolean {
        val path = link.substringBefore('?').substringBefore('#').lowercase()
        val t = type?.lowercase().orEmpty()
        return t == "m3u8" || t == "hls" || path.endsWith(".m3u8") || path.contains(".m3u8/")
    }

    /**
     * Downloads [url] and returns its variants, best first. Empty when it is not a master playlist (a single media
     * playlist, a plain file, or a request that failed): the player then simply has no quality choice.
     */
    fun fetch(url: String, headers: Map<String, String>, proxy: Proxy? = null, timeoutMs: Int = 7000): List<HlsVariant> =
        fetchResult(url, headers, proxy, timeoutMs) ?: emptyList()

    /** Like [fetch], but null when the request itself failed (so the caller can try another route); empty = answered, no master playlist. */
    fun fetchResult(url: String, headers: Map<String, String>, proxy: Proxy? = null, timeoutMs: Int = 7000): List<HlsVariant>? {
        return try {
            val conn = (URL(url).openConnection(proxy ?: Proxy.NO_PROXY) as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                for ((k, v) in headers) {
                    if (k.isNotBlank() && v.isNotBlank() && !k.equals("Accept-Encoding", true) && !k.equals("Host", true)) {
                        try { setRequestProperty(k, v) } catch (_: Throwable) {}
                    }
                }
                if (headers.keys.none { it.equals("User-Agent", true) }) {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
            }
            try {
                if (conn.responseCode !in 200..299) return null
                val text = conn.inputStream.use { ins ->
                    val bos = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(16 * 1024)
                    while (bos.size() < 2 * 1024 * 1024) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        bos.write(buf, 0, n)
                    }
                    bos.toString("UTF-8")
                }
                parseMaster(text, conn.url.toString())
            } finally {
                conn.disconnect()
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun parseMaster(text: String, baseUrl: String): List<HlsVariant> {
        if (!text.trimStart().startsWith("#EXTM3U") || !text.contains("#EXT-X-STREAM-INF")) return emptyList()
        val out = ArrayList<HlsVariant>()
        val lines = text.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                val attrs = parseAttributes(line.substringAfter(':'))
                var j = i + 1
                while (j < lines.size && (lines[j].isBlank() || lines[j].startsWith("#"))) j++
                val uriLine = lines.getOrNull(j)?.trim().orEmpty()
                if (uriLine.isNotEmpty()) {
                    val bw = attrs["BANDWIDTH"]?.toLongOrNull() ?: attrs["AVERAGE-BANDWIDTH"]?.toLongOrNull() ?: 0L
                    val res = attrs["RESOLUTION"].orEmpty()
                    val w = res.substringBefore('x', "").toIntOrNull() ?: 0
                    val h = res.substringAfter('x', "").toIntOrNull() ?: 0
                    val fps = attrs["FRAME-RATE"]?.toDoubleOrNull() ?: 0.0
                    val codecs = attrs["CODECS"].orEmpty().split(',').map { it.trim() }
                    val video = codecs.firstOrNull { c ->
                        val l = c.lowercase()
                        l.startsWith("avc") || l.startsWith("hvc") || l.startsWith("hev") || l.startsWith("vp0") ||
                            l.startsWith("vp9") || l.startsWith("av01") || l.startsWith("dvh")
                    }.orEmpty()
                    val abs = try { URI(baseUrl).resolve(uriLine).toString() } catch (_: Throwable) { uriLine }
                    out.add(HlsVariant(bw, w, h, fps, video, abs))
                }
                i = j
            }
            i++
        }
        // best first; the same resolution at two bitrates stays as two entries
        return out.distinctBy { Triple(it.height, it.bandwidth, it.width) }
            .sortedWith(compareByDescending<HlsVariant> { it.height }.thenByDescending { it.bandwidth })
    }

    /** The audio languages / subtitle tracks a master playlist lists next to its video renditions (EXT-X-MEDIA with a URI). */
    fun parseRenditions(text: String, baseUrl: String): List<HlsRendition> {
        if (!text.trimStart().startsWith("#EXTM3U")) return emptyList()
        val out = ArrayList<HlsRendition>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (!line.startsWith("#EXT-X-MEDIA:")) continue
            val attrs = parseAttributes(line.substringAfter(':'))
            val type = attrs["TYPE"].orEmpty().uppercase()
            if (type != "AUDIO" && type != "SUBTITLES") continue
            val uriRaw = attrs["URI"].orEmpty()
            val abs = if (uriRaw.isBlank()) "" else try { URI(baseUrl).resolve(uriRaw).toString() } catch (_: Throwable) { uriRaw }
            out.add(
                HlsRendition(
                    type = type,
                    groupId = attrs["GROUP-ID"].orEmpty(),
                    name = attrs["NAME"].orEmpty(),
                    language = attrs["LANGUAGE"].orEmpty(),
                    isDefault = attrs["DEFAULT"].equals("YES", true),
                    uri = abs
                )
            )
        }
        return out
    }

    /** Segment list of a media playlist, null when [text] is not one (a master playlist, a web page, an error). */
    fun parseMediaPlaylist(text: String, baseUrl: String): HlsMediaPlaylist? {
        if (!text.trimStart().startsWith("#EXTM3U") || text.contains("#EXT-X-STREAM-INF")) return null
        val durations = ArrayList<Double>()
        val segments = ArrayList<String>()
        var pending = -1.0
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF:")) {
                pending = line.substringAfter(':').substringBefore(',').trim().toDoubleOrNull() ?: 0.0
            } else if (!line.startsWith("#") && pending >= 0.0) {
                val abs = try { URI(baseUrl).resolve(line).toString() } catch (_: Throwable) { line }
                durations.add(pending)
                segments.add(abs)
                pending = -1.0
            }
        }
        if (segments.isEmpty()) return null
        return HlsMediaPlaylist(durations, segments, text.contains("#EXT-X-ENDLIST"))
    }

    /** Downloads a small text file (a playlist). Null when the request failed, so the caller can try another route. */
    fun fetchText(url: String, headers: Map<String, String>, proxy: Proxy? = null, timeoutMs: Int = 4000): String? {
        return try {
            val conn = (URL(url).openConnection(proxy ?: Proxy.NO_PROXY) as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                for ((k, v) in headers) {
                    if (k.isNotBlank() && v.isNotBlank() && !k.equals("Accept-Encoding", true) && !k.equals("Host", true) &&
                        !k.equals("Range", true) && !k.equals("Content-Length", true)
                    ) {
                        try { setRequestProperty(k, v) } catch (_: Throwable) {}
                    }
                }
                if (headers.keys.none { it.equals("User-Agent", true) }) {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
            }
            try {
                if (conn.responseCode !in 200..299) return null
                conn.inputStream.use { ins ->
                    val bos = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(16 * 1024)
                    while (bos.size() < 2 * 1024 * 1024) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        bos.write(buf, 0, n)
                    }
                    bos.toString("UTF-8")
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * The REAL download speed of the stream's server in bytes per second, measured by downloading (a part of) one segment of a
     * middle rendition. ExoPlayer in CloudStream does this all the time (its bandwidth meter) and picks the quality from it;
     * Android's linkDownstreamBandwidthKbps is only the nominal speed of the Wi-Fi / mobile link (hundreds of Mbit/s on Wi-Fi
     * even when the internet behind it delivers 3), so choosing the quality from that value started far too high and buffered.
     * Returns null when nothing could be measured (request failed, segment too small to tell): the caller then plays it safe.
     */
    fun measureThroughput(variants: List<HlsVariant>, headers: Map<String, String>, proxy: Proxy? = null, budgetMs: Long = 2500L): Long? {
        if (variants.isEmpty()) return null
        return try {
            val probe = variants[(variants.size - 1) / 2]
            val text = fetchText(probe.uri, headers, proxy, 3000) ?: return null
            val media = parseMediaPlaylist(text, probe.uri) ?: return null
            val segment = media.segments.firstOrNull() ?: return null
            val conn = (URL(segment).openConnection(proxy ?: Proxy.NO_PROXY) as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                instanceFollowRedirects = true
                for ((k, v) in headers) {
                    if (k.isNotBlank() && v.isNotBlank() && !k.equals("Accept-Encoding", true) && !k.equals("Host", true) &&
                        !k.equals("Range", true) && !k.equals("Content-Length", true)
                    ) {
                        try { setRequestProperty(k, v) } catch (_: Throwable) {}
                    }
                }
                if (headers.keys.none { it.equals("User-Agent", true) }) {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
            }
            try {
                if (conn.responseCode !in 200..299) return null
                conn.inputStream.use { ins ->
                    val buf = ByteArray(32 * 1024)
                    var total = 0L
                    val startedAt = System.nanoTime()
                    val deadline = startedAt + budgetMs * 1_000_000L
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        total += n
                        if (total >= 1_500_000L || System.nanoTime() > deadline) break
                    }
                    val seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0
                    if (total < 150_000L || seconds <= 0.0) null else (total / seconds).toLong()
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseAttributes(s: String): Map<String, String> {
        val map = HashMap<String, String>()
        val re = Regex("([A-Z0-9-]+)=(\"[^\"]*\"|[^,]*)")
        for (m in re.findAll(s)) map[m.groupValues[1]] = m.groupValues[2].trim().trim('"')
        return map
    }
}
