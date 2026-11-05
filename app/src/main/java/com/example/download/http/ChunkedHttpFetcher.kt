package com.example.download

import android.content.Context
import com.example.util.NetworkBypass
import com.example.ytdlp.YtdlpFetch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object HttpFetch {
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val STALL_LIMIT_MS = 10 * 60_000L
    private val skipHeaders = setOf("host", "range", "if-range", "accept-encoding", "content-length", "connection", "transfer-encoding", "te", "upgrade")

    private val direct: OkHttpClient by lazy {
        com.example.util.network.ViauraHttpClient.get().newBuilder()
            .dns(com.example.util.network.ViauraDns)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(64, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Volatile private var bypass: OkHttpClient? = null
    @Volatile private var bypassPort = -1

    private fun clients(context: Context): List<OkHttpClient> {
        val proxy = try { NetworkBypass.okHttpProxy(context) } catch (_: Throwable) { null } ?: return listOf(direct)
        val port = (proxy.address() as? java.net.InetSocketAddress)?.port ?: 0
        if (bypassPort != port || bypass == null) {
            bypass = direct.newBuilder().proxy(proxy).build()
            bypassPort = port
        }
        return listOfNotNull(direct, bypass)
    }

    fun request(url: String, headers: Map<String, String>, range: String? = null): Request {
        val b = Request.Builder().url(url)
        var hasUa = false
        for ((k, v) in headers) {
            if (k.isBlank() || v.isBlank() || k.lowercase() in skipHeaders) continue
            if (k.equals("User-Agent", true)) hasUa = true
            try { b.header(k, v) } catch (_: Throwable) {}
        }
        if (!hasUa) b.header("User-Agent", UA)
        b.header("Accept-Encoding", "identity")
        if (range != null) b.header("Range", range)
        return b.build()
    }

    /** Runs [req] (the plain connection first, then the app's bypass connection) and gives the response to [block]. */
    fun <T> call(context: Context, req: Request, token: CancelToken?, block: (Response) -> T): T {
        var last: IOException? = null
        for (c in clients(context)) {
            token?.check()
            val call = c.newCall(req)
            token?.register(call)
            try {
                call.execute().use { return block(it) }
            } catch (e: IOException) {
                if (token?.cancelled == true) throw FetchCancelled()
                if (e is HttpStatusException || e is LinkExpiredException || e is RangeIgnored) throw e
                last = e
            } finally {
                token?.unregister(call)
            }
        }
        throw last ?: IOException("no connection")
    }

    private fun fileNameOf(r: Response): String? = r.header("Content-Disposition")?.let { d ->
        Regex("filename\\*?=(?:UTF-8\'\')?\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(d)?.groupValues?.get(1)?.let { android.net.Uri.decode(it) }
    }

    /**
     * Size / type of a file, the way CloudStream finds it: a plain HEAD request first (Content-Length), and a one-byte ranged
     * GET (Content-Range total) when HEAD gives nothing. The size is only trusted when the server states it.
     */
    fun head(context: Context, url: String, headers: Map<String, String>, token: CancelToken? = null): HeadInfo? {
        var byHead: HeadInfo? = null
        try {
            val req = request(url, headers).newBuilder().head().build()
            byHead = call(context, req, token) { r ->
                if (r.code !in 200..299) return@call null
                val len = r.header("Content-Length")?.toLongOrNull() ?: -1L
                val acc = r.header("Accept-Ranges")?.trim()?.lowercase()
                HeadInfo(
                    size = if (len > 0L) len else -1L,
                    contentType = r.header("Content-Type")?.substringBefore(';')?.trim(),
                    fileName = fileNameOf(r), acceptsRanges = acc == "bytes", finalUrl = r.request.url.toString()
                )
            }
        } catch (e: FetchCancelled) {
            throw e
        } catch (_: Throwable) {
        }
        val ranged: HeadInfo? = try {
            call(context, request(url, headers, "bytes=0-0"), token) { r ->
                if (r.code !in 200..299) return@call null
                val cr = r.header("Content-Range")
                val total = cr?.substringAfter('/', "")?.trim()?.toLongOrNull()
                val len = r.header("Content-Length")?.toLongOrNull() ?: -1L
                val ranges = r.code == 206
                HeadInfo(
                    size = if (ranges) (total ?: -1L) else len,
                    contentType = r.header("Content-Type")?.substringBefore(';')?.trim(),
                    fileName = fileNameOf(r), acceptsRanges = ranges, finalUrl = r.request.url.toString()
                )
            }
        } catch (e: FetchCancelled) {
            throw e
        } catch (_: Throwable) {
            null
        }
        if (ranged == null) return byHead?.copy(acceptsRanges = false)
        // the ranged answer says for sure whether ranges work; HEAD is used for the size when the ranged one has none
        val size = if (ranged.size > 0L) ranged.size else (byHead?.size ?: -1L)
        return ranged.copy(size = size, contentType = ranged.contentType ?: byHead?.contentType, fileName = ranged.fileName ?: byHead?.fileName)
    }

    /** Does this stream really deliver video / audio data right now? (a ranged read of its first bytes; HLS: a real playlist) */
    fun canPlay(context: Context, url: String, headers: Map<String, String>): Boolean = checkPlay(context, url, headers) == null

    /** Same check as [canPlay], but says WHY a stream does not deliver (null = it delivers). */
    fun checkPlay(context: Context, url: String, headers: Map<String, String>): String? = try {
        val isHls = url.lowercase().contains(".m3u8")
        call(context, request(url, headers, "bytes=0-65535"), null) { r ->
            if (r.code !in 200..299) {
                return@call when (r.code) {
                    401, 403 -> "The site refused the video (HTTP ${r.code}): it needs a login / cookies or blocks apps."
                    404, 410 -> "The video link has expired or was removed (HTTP ${r.code})."
                    429 -> "The site is limiting requests (HTTP 429). Try again later."
                    else -> "The site answered HTTP ${r.code} instead of the video."
                }
            }
            val ct = r.header("Content-Type")?.lowercase().orEmpty()
            if (ct.contains("text/html") || ct.contains("json")) return@call "The link opens a web page, not a video."
            val body = r.body ?: return@call "The site sent no video data."
            val buf = ByteArray(8192)
            val n = body.byteStream().read(buf)
            if (n <= 0) return@call "The site sent no video data."
            if (isHls && !String(buf, 0, n, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r').startsWith("#EXTM3U")) {
                "The site did not return a valid HLS playlist."
            } else null
        }
    } catch (_: Throwable) {
        "The video server could not be reached (connection failed or timed out)."
    }

    fun probeSize(context: Context, url: String, headers: Map<String, String>): Long = head(context, url, headers)?.size ?: -1L

    private fun failFor(code: Int, url: String): IOException =
        if (code == 403 || code == 404 || code == 410) LinkExpiredException("HTTP $code") else HttpStatusException(code, "HTTP $code")

    /** True if the file was already finished earlier. */
    fun isDone(dest: File): Boolean = File(dest.path + ".done").exists() && dest.exists()

    private fun markDone(dest: File) {
        File(dest.path + ".done").writeText("1")
    }

    /**
     * Downloads [part] into [dest]. A restart or a pause continues where it stopped (finished chunks are remembered in a
     * side file), so nothing is ever downloaded twice. [counter] always holds the bytes that are on disk.
     */
    suspend fun download(
        context: Context, part: PlanPart, dest: File, token: CancelToken, counter: AtomicLong,
        totalOut: AtomicLong? = null, connections: Int = 8
    ) {
        if (isDone(dest)) {
            counter.set(dest.length())
            totalOut?.set(dest.length())
            return
        }
        withContext(Dispatchers.IO) {
            dest.parentFile?.mkdirs()
            val h = head(context, part.url, part.headers, token)
            // the server's own answer for THIS request decides; the size seen earlier in the list is only a fallback
            val total = if ((h?.size ?: -1L) > 0L) h!!.size else part.sizeBytes
            val ranges = total > 0L && h?.acceptsRanges == true
            if (total > 0L) totalOut?.set(total)
            if (!ranges) {
                downloadSingle(context, part, dest, token, counter, total)
            } else {
                try {
                    downloadChunks(context, part, dest, token, counter, total, connections)
                } catch (_: RangeIgnored) {
                    File(dest.path + ".chunks").delete()
                    dest.delete()
                    counter.set(0L)
                    downloadSingle(context, part, dest, token, counter, total)
                }
            }
            markDone(dest)
            File(dest.path + ".chunks").delete()
            counter.set(dest.length())
        }
    }

    private suspend fun downloadChunks(
        context: Context, part: PlanPart, dest: File, token: CancelToken, counter: AtomicLong, total: Long, connections: Int
    ) = coroutineScope {
        val hostLimit = if (part.chunkSize > 0L) minOf(part.chunkSize, 10_485_760L) else 0L
        var chunk = (total / (connections * 3L)).coerceIn(512 * 1024L, 8L * 1024 * 1024)
        if (hostLimit > 0L) chunk = minOf(chunk, hostLimit)
        val count = ((total + chunk - 1) / chunk).toInt()
        val stateFile = File(dest.path + ".chunks")
        val header = "$total:$chunk"
        val doneSet = ConcurrentHashMap.newKeySet<Int>()
        if (stateFile.exists() && dest.exists() && dest.length() == total) {
            val lines = stateFile.readLines()
            if (lines.firstOrNull() == header) lines.drop(1).forEach { l -> l.trim().toIntOrNull()?.let { doneSet.add(it) } }
        }
        if (doneSet.isEmpty()) {
            stateFile.writeText(header + "\n")
            dest.delete()
        }
        val raf = RandomAccessFile(dest, "rw")
        try {
            if (raf.length() != total) raf.setLength(total)
            val channel = raf.channel
            var already = 0L
            for (i in doneSet) already += chunkLen(i, chunk, total)
            counter.set(already)
            val pending = (0 until count).filter { it !in doneSet }
            val next = AtomicInteger(0)
            val lastProgress = AtomicLong(System.currentTimeMillis())
            val stateLock = Any()
            val actualEnd = AtomicLong(total)
            val workers = (0 until minOf(connections, maxOf(1, pending.size))).map {
                async(Dispatchers.IO) {
                    while (true) {
                        token.check()
                        val slot = next.getAndIncrement()
                        if (slot >= pending.size) break
                        val index = pending[slot]
                        val start = index * chunk
                        val end = minOf(total, start + chunk) - 1
                        var attempt = 0
                        while (true) {
                            token.check()
                            var written = 0L
                            try {
                                call(context, request(part.url, part.headers, "bytes=$start-$end"), token) { r ->
                                    if (r.code == 200) throw RangeIgnored()
                                    if (r.code != 206) throw failFor(r.code, part.url)
                                    val body = r.body ?: throw IOException("empty response")
                                    val buf = ByteArray(128 * 1024)
                                    var pos = start
                                    body.byteStream().use { input ->
                                        while (true) {
                                            token.check()
                                            val n = input.read(buf)
                                            if (n < 0) break
                                            // like CloudStream: never take more than this piece, ignore what the server adds
                                            val take = minOf(n.toLong(), end + 1 - pos).toInt()
                                            if (take > 0) {
                                                channel.write(ByteBuffer.wrap(buf, 0, take), pos)
                                                pos += take
                                                written += take
                                                counter.addAndGet(take.toLong())
                                                lastProgress.set(System.currentTimeMillis())
                                            }
                                            if (pos > end) break
                                        }
                                    }
                                    if (pos != end + 1) {
                                        // the last piece may end early when the stated size was a little too big: the file is simply shorter
                                        if (index == count - 1 && pos > start) actualEnd.set(pos) else throw IOException("the chunk ended early")
                                    }
                                }
                                break
                            } catch (e: FetchCancelled) {
                                throw e
                            } catch (e: IOException) {
                                counter.addAndGet(-written)
                                if (e is LinkExpiredException || e is RangeIgnored) throw e
                                token.check()
                                attempt++
                                if (System.currentTimeMillis() - lastProgress.get() > STALL_LIMIT_MS) throw e
                                delay(minOf(10_000L, 400L * (1L shl minOf(attempt, 5))))
                            }
                        }
                        doneSet.add(index)
                        synchronized(stateLock) { stateFile.appendText("$index\n") }
                    }
                }
            }
            workers.awaitAll()
            if (actualEnd.get() < total) raf.setLength(actualEnd.get())
        } finally {
            try { raf.close() } catch (_: Throwable) {}
        }
    }

    private fun chunkLen(index: Int, chunk: Long, total: Long): Long = minOf(total, (index + 1) * chunk) - index * chunk

    private suspend fun downloadSingle(context: Context, part: PlanPart, dest: File, token: CancelToken, counter: AtomicLong, total: Long) {
        var attempt = 0
        var lastProgress = System.currentTimeMillis()
        while (true) {
            token.check()
            val existing = if (dest.exists()) dest.length() else 0L
            try {
                call(context, request(part.url, part.headers, if (existing > 0L) "bytes=$existing-" else null), token) { r ->
                    val append: Boolean
                    if (existing > 0L && r.code == 206) {
                        append = true
                    } else if (r.code == 200 || (existing == 0L && r.code == 206)) {
                        append = false
                    } else if (r.code == 416 && total > 0L && existing == total) {
                        counter.set(existing)
                        return@call
                    } else {
                        throw failFor(r.code, part.url)
                    }
                    val body = r.body ?: throw IOException("empty response")
                    val out = java.io.FileOutputStream(dest, append)
                    if (!append) counter.set(0L) else counter.set(existing)
                    out.use { o ->
                        val buf = ByteArray(256 * 1024)
                        body.byteStream().use { input ->
                            while (true) {
                                token.check()
                                val n = input.read(buf)
                                if (n < 0) break
                                o.write(buf, 0, n)
                                counter.addAndGet(n.toLong())
                                lastProgress = System.currentTimeMillis()
                            }
                        }
                    }
                }
                if (total > 0L && dest.length() < total) throw IOException("the download ended early")
                return
            } catch (e: FetchCancelled) {
                throw e
            } catch (e: IOException) {
                if (e is LinkExpiredException) throw e
                token.check()
                attempt++
                if (System.currentTimeMillis() - lastProgress > STALL_LIMIT_MS) throw e
                delay(minOf(10_000L, 400L * (1L shl minOf(attempt, 5))))
            }
        }
    }
}
