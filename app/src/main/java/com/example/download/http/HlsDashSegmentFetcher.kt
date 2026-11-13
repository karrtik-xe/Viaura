package com.example.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** HLS (.m3u8) and DASH-fragment streams: every segment is fetched in parallel, finished segments are kept, then joined into one file. */
object SegmentFetch {
    private class Segment(
        val url: String,
        val rangeStart: Long = -1L,
        val rangeLen: Long = -1L,
        val keyUri: String? = null,
        val iv: ByteArray? = null,
        val sequence: Long = 0L
    )

    private class Playlist(val segments: List<Segment>, val initSegment: Segment?, val fMp4: Boolean)

    private fun resolve(base: String, ref: String): String =
        try { URI(base.substringBefore('#')).resolve(ref.trim()).toString() } catch (_: Throwable) { ref }

    private fun attr(line: String, name: String): String? {
        val m = Regex("(?:^|[,:])" + Regex.escape(name) + "=(\"[^\"]*\"|[^,]*)", RegexOption.IGNORE_CASE).find(line) ?: return null
        return m.groupValues[1].trim('"')
    }

    private fun hexIv(text: String): ByteArray {
        val hex = text.removePrefix("0x").removePrefix("0X").padStart(32, '0')
        return ByteArray(16) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }

    private fun fetchText(context: Context, url: String, headers: Map<String, String>, token: CancelToken): String =
        HttpFetch.call(context, HttpFetch.request(url, headers), token) { r ->
            if (r.code !in 200..299) throw if (r.code == 403 || r.code == 404 || r.code == 410) LinkExpiredException("HTTP ${r.code}") else HttpStatusException(r.code, "HTTP ${r.code}")
            r.body?.string().orEmpty()
        }

    private fun parseHls(context: Context, startUrl: String, headers: Map<String, String>, token: CancelToken): Playlist {
        var url = startUrl
        var text = fetchText(context, url, headers, token)
        if (!text.trimStart('\uFEFF', ' ', '\n', '\r').startsWith("#EXTM3U")) throw IOException("this is not an HLS playlist")
        if (text.contains("#EXT-X-STREAM-INF")) {
            // a master playlist: its highest rendition
            val lines = text.lines().map { it.trim() }
            var bestBw = -1L
            var bestUrl: String? = null
            for (i in lines.indices) {
                if (!lines[i].startsWith("#EXT-X-STREAM-INF")) continue
                val bw = attr(lines[i], "BANDWIDTH")?.toLongOrNull() ?: 0L
                val next = lines.drop(i + 1).firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: continue
                if (bw > bestBw) { bestBw = bw; bestUrl = next }
            }
            url = resolve(url, bestUrl ?: throw IOException("the playlist lists no video"))
            text = fetchText(context, url, headers, token)
        }
        if (!text.contains("#EXT-X-ENDLIST")) throw IllegalStateException("Live streams can't be downloaded")
        var keyUri: String? = null
        var keyIv: ByteArray? = null
        var seq = 0L
        var init: Segment? = null
        var pendingRange: Pair<Long, Long>? = null
        var lastRangeEnd = 0L
        val out = ArrayList<Segment>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> seq = line.substringAfter(':').trim().toLongOrNull() ?: 0L
                line.startsWith("#EXT-X-KEY:") -> {
                    val method = attr(line, "METHOD") ?: "NONE"
                    when {
                        method.equals("NONE", true) -> { keyUri = null; keyIv = null }
                        method.equals("AES-128", true) -> {
                            keyUri = attr(line, "URI")?.let { resolve(url, it) }
                            keyIv = attr(line, "IV")?.let { hexIv(it) }
                        }
                        else -> throw IllegalStateException("This stream uses $method encryption, which can't be downloaded")
                    }
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    val uri = attr(line, "URI") ?: continue
                    val br = attr(line, "BYTERANGE")
                    var rs = -1L
                    var rl = -1L
                    if (br != null) {
                        rl = br.substringBefore('@').toLongOrNull() ?: -1L
                        rs = br.substringAfter('@', "0").toLongOrNull() ?: 0L
                    }
                    init = Segment(resolve(url, uri), rs, rl)
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> {
                    val v = line.substringAfter(':').trim()
                    val len = v.substringBefore('@').toLongOrNull() ?: 0L
                    val start = if (v.contains('@')) v.substringAfter('@').toLongOrNull() ?: 0L else lastRangeEnd
                    pendingRange = start to len
                    lastRangeEnd = start + len
                }
                !line.startsWith("#") -> {
                    val pr = pendingRange
                    out.add(Segment(resolve(url, line), pr?.first ?: -1L, pr?.second ?: -1L, keyUri, keyIv, seq))
                    pendingRange = null
                    seq++
                }
            }
        }
        if (out.isEmpty()) throw IOException("the playlist has no segments")
        return Playlist(out, init, init != null)
    }

    private fun fetchSegment(context: Context, s: Segment, headers: Map<String, String>, token: CancelToken, keys: ConcurrentHashMap<String, ByteArray>): ByteArray {
        val range = if (s.rangeStart >= 0 && s.rangeLen > 0) "bytes=${s.rangeStart}-${s.rangeStart + s.rangeLen - 1}" else null
        val data = HttpFetch.call(context, HttpFetch.request(s.url, headers, range), token) { r ->
            if (r.code !in 200..299) throw if (r.code == 403 || r.code == 404 || r.code == 410) LinkExpiredException("HTTP ${r.code}") else HttpStatusException(r.code, "HTTP ${r.code}")
            r.body?.bytes() ?: throw IOException("empty response")
        }
        val keyUri = s.keyUri ?: return data
        val key = keys[keyUri] ?: HttpFetch.call(context, HttpFetch.request(keyUri, headers), token) { r ->
            if (r.code !in 200..299) throw HttpStatusException(r.code, "the key server answered HTTP ${r.code}")
            r.body?.bytes() ?: throw IOException("empty key")
        }.also { keys[keyUri] = it }
        if (key.size != 16) throw IOException("the decryption key is not valid")
        val iv = s.iv ?: ByteArray(16).also { b ->
            var v = s.sequence
            for (i in 15 downTo 8) { b[i] = (v and 0xFF).toByte(); v = v shr 8 }
        }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return try { cipher.doFinal(data) } catch (_: Throwable) {
            // some servers send a last block without padding
            val c2 = Cipher.getInstance("AES/CBC/NoPadding")
            c2.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val raw = c2.doFinal(data.copyOf(data.size - data.size % 16))
            raw
        }
    }

    /** Output file extension of the joined stream. */
    fun outputExt(part: PlanPart): String = if (part.protocol == "hls") "ts" else part.ext.ifBlank { "mp4" }

    suspend fun download(
        context: Context, part: PlanPart, dest: File, token: CancelToken, counter: AtomicLong, connections: Int = 8,
        /** Receives the size the finished file will have: measured from the segments already downloaded, exact at the end. */
        totalOut: AtomicLong? = null
    ) {
        if (HttpFetch.isDone(dest)) {
            counter.set(dest.length())
            totalOut?.set(dest.length())
            return
        }
        withContext(Dispatchers.IO) {
            dest.parentFile?.mkdirs()
            val headers = part.headers
            val segDir = File(dest.path + ".segs").apply { mkdirs() }
            val segments: List<Segment>
            val init: Segment?
            if (part.protocol == "hls") {
                // the playlist is read again on every start: with no internet it is awaited, never counted as a failure
                var pl: Playlist? = null
                var tries = 0
                while (pl == null) {
                    token.check()
                    try {
                        pl = parseHls(context, part.url, headers, token)
                    } catch (e: FetchCancelled) {
                        throw e
                    } catch (e: LinkExpiredException) {
                        throw e
                    } catch (e: IOException) {
                        tries++
                        if (HttpFetch.isOnline(context) && tries >= 6 && !HttpFetch.isNetworkError(e)) throw e
                        HttpFetch.awaitInternet(context, token)
                        delay(minOf(10_000L, 600L * tries))
                    }
                }
                val ready = pl!!
                segments = ready.segments
                init = ready.initSegment
            } else {
                // DASH: the first fragment is normally the initialisation segment
                segments = part.fragments.map { Segment(it) }
                init = null
            }
            val all = (listOfNotNull(init) + segments)
            val keys = ConcurrentHashMap<String, ByteArray>()
            var already = 0L
            var alreadyCount = 0
            for (i in all.indices) {
                val f = File(segDir, "s%06d".format(i))
                if (f.exists()) { already += f.length(); alreadyCount++ }
            }
            counter.set(already)
            val finishedSegments = AtomicInteger(alreadyCount)
            fun publishEstimate() {
                val n = finishedSegments.get()
                if (n > 0 && totalOut != null) totalOut.set(maxOf(counter.get(), counter.get() * all.size / n))
            }
            publishEstimate()
            val next = AtomicInteger(0)
            val lastProgress = AtomicLong(System.currentTimeMillis())
            coroutineScope {
                (0 until minOf(connections, all.size)).map {
                    async(Dispatchers.IO) {
                        while (true) {
                            token.check()
                            val i = next.getAndIncrement()
                            if (i >= all.size) break
                            val f = File(segDir, "s%06d".format(i))
                            if (f.exists()) continue
                            var attempt = 0
                            while (true) {
                                token.check()
                                try {
                                    val bytes = fetchSegment(context, all[i], headers, token, keys)
                                    val tmp = File(segDir, "s%06d.tmp".format(i))
                                    tmp.writeBytes(bytes)
                                    tmp.renameTo(f)
                                    counter.addAndGet(bytes.size.toLong())
                                    finishedSegments.incrementAndGet()
                                    publishEstimate()
                                    lastProgress.set(System.currentTimeMillis())
                                    break
                                } catch (e: FetchCancelled) {
                                    throw e
                                } catch (e: LinkExpiredException) {
                                    throw e
                                } catch (e: IOException) {
                                    token.check()
                                    attempt++
                                    if (HttpFetch.shouldGiveUp(e, lastProgress.get())) throw e
                                    // connection lost: wait, then fetch this same segment again (finished segments are kept)
                                    HttpFetch.awaitInternet(context, token)
                                    if (HttpFetch.isNetworkError(e)) lastProgress.set(System.currentTimeMillis())
                                    delay(minOf(10_000L, 400L * (1L shl minOf(attempt, 5))))
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
            // join: the segments are copied into one file in their order
            val joined = File(dest.path + ".join")
            FileOutputStream(joined).use { out ->
                for (i in all.indices) {
                    FastFileCopy.copy(File(segDir, "s%06d".format(i)), out)
                }
            }
            dest.delete()
            if (!joined.renameTo(dest)) throw IOException("could not finish the stream file")
            segDir.deleteRecursively()
            File(dest.path + ".done").writeText("1")
            counter.set(dest.length())
            totalOut?.set(dest.length())
        }
    }

    /**
     * Size a segmented stream will really have, without downloading it: a few segments spread over the whole stream are measured
     * (one tiny request each) and the average is multiplied by the segment count. The segment bitrate that a website lists is only
     * a peak value, so a size computed from it is much too big - this is measured from the real files. -1 = could not be measured.
     */
    fun estimateSize(context: Context, part: PlanPart): Long {
        return try {
            val token = CancelToken()
            val segments: List<String>
            var fixed = 0L
            var ranged: List<Segment>? = null
            if (part.protocol == "hls") {
                val pl = parseHls(context, part.url, part.headers, token)
                ranged = pl.segments
                segments = pl.segments.map { it.url }
                pl.initSegment?.let { fixed = if (it.rangeLen > 0) it.rangeLen else HttpFetch.probeSize(context, it.url, part.headers).coerceAtLeast(0L) }
                if (pl.segments.all { it.rangeLen > 0 }) return fixed + pl.segments.sumOf { it.rangeLen }
            } else {
                if (part.fragments.isEmpty()) return -1L
                segments = part.fragments
            }
            if (segments.isEmpty()) return -1L
            val picks = if (segments.size <= 5) segments.indices.toList()
            else listOf(0, segments.size / 4, segments.size / 2, segments.size * 3 / 4, segments.size - 1)
            var sum = 0L
            var n = 0
            for (i in picks.distinct()) {
                val r = ranged?.getOrNull(i)
                val size = if (r != null && r.rangeLen > 0) r.rangeLen else HttpFetch.probeSize(context, segments[i], part.headers)
                if (size > 0L) { sum += size; n++ }
            }
            if (n == 0) -1L else fixed + (sum.toDouble() / n * segments.size).toLong()
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (_: Throwable) {
            -1L
        }
    }
}
