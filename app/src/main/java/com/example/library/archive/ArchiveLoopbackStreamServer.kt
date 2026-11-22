package com.example.util.archive

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Plays a video that sits INSIDE an archive without extracting or copying it: mpv opens http://127.0.0.1:<port>/arc/<id>/<index>/<name>
 * and this server reads exactly the requested bytes out of the archive (a file on the phone, or a link on a website). Byte ranges are
 * supported, so seeking works. 127.0.0.1 only.
 */
object ArchiveStreamServer {
    private val seq = AtomicInteger(0)
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "viaura-archive-" + seq.incrementAndGet()).apply { isDaemon = true } }

    private var server: ServerSocket? = null
    @Volatile private var appContext: Context? = null

    /** ZIP entries: where the data starts, read once from the local header. Key = "<archiveId>/<index>". */
    private val dataOffsets = ConcurrentHashMap<String, Long>()

    /** The address mpv should open for an "archive://" playlist path, or null when it cannot be started. */
    fun urlFor(context: Context, archivePath: String): String? {
        val (id, index) = ArchiveStore.parsePath(archivePath) ?: return null
        return try {
            appContext = context.applicationContext
            val port = ensureStarted()
            val name = archivePath.substringAfterLast('/')
            "http://127.0.0.1:$port/arc/$id/$index/${Uri.encode(name)}"
        } catch (_: Throwable) {
            null
        }
    }

    private const val MAX_ORIGIN_FAILURES = 6

    /** Where the data of video [index] starts inside the archive (ZIP: read once from its local header, then remembered). */
    fun dataOffsetFor(source: ArchiveSource, record: ArchiveRecord, index: Int, entry: ArchiveEntry): Long {
        if (record.kind != ArchiveKind.ZIP) return entry.dataOffset
        val key = "${record.id}/$index"
        return dataOffsets[key] ?: ArchiveEngine.resolveZipDataOffset(source, entry).also { dataOffsets[key] = it }
    }

    /** True when the archive behind an "archive://" path or a loopback address is a link on a website (reading it uses the internet). */
    fun isRemoteArchive(context: Context, path: String): Boolean {
        val id = if (ArchiveStore.isArchivePath(path)) ArchiveStore.parsePath(path)?.first
        else path.substringAfter("/arc/", "").substringBefore('/').takeIf { it.isNotEmpty() }
        val record = id?.let { ArchiveStore.get(context, it) } ?: return true
        return record.sourceType == "url"
    }

    /** True for the loopback address that [urlFor] returns. */
    fun isLoopbackUrl(path: String): Boolean =
        path.startsWith("http://127.0.0.1:") && path.contains("/arc/")

    // ---------------------------------------------------------------------------------------------------------------
    // Cast (DLNA / UPnP): the same server also hands one local video to a TV on the Wi-Fi network. It listens on the Wi-Fi
    // address only while a cast is running and answers ONLY "/cast/<random token>/<name>" (nothing else of the app is reachable
    // from the network). Byte ranges are supported, so the TV can seek.
    // ---------------------------------------------------------------------------------------------------------------
    private class CastSource(val path: String, val name: String, val mime: String)
    private val castSources = ConcurrentHashMap<String, CastSource>()
    private var lanServer: ServerSocket? = null

    /** Address a TV on [lanIp]'s network can open for [source] (a file path or content:// uri), or null when it cannot be started. */
    fun castUrlFor(context: Context, source: String, displayName: String, lanIp: String): String? {
        return try {
            appContext = context.applicationContext
            val name = displayName.ifBlank { source.substringAfterLast('/') }.ifBlank { "video" }
            val token = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            castSources[token] = CastSource(source, name, ArchiveEngine.mimeTypeFor(name))
            val port = ensureLanStarted()
            "http://$lanIp:$port/cast/$token/${Uri.encode(name)}"
        } catch (_: Throwable) {
            null
        }
    }

    /** MIME type of the file behind a cast address (for the TV's metadata). */
    fun castMimeFor(url: String): String {
        val token = url.substringAfter("/cast/", "").substringBefore('/')
        return castSources[token]?.mime ?: "video/mp4"
    }

    /** Ends the cast: nothing is shared any more and the network listener is closed. */
    fun stopCast() {
        castSources.clear()
        synchronized(this) {
            try { lanServer?.close() } catch (_: Throwable) {}
            lanServer = null
        }
    }

    @Synchronized
    private fun ensureLanStarted(): Int {
        lanServer?.let { if (!it.isClosed) return it.localPort }
        val s = ServerSocket(0, 64)
        lanServer = s
        Thread({
            while (!s.isClosed) {
                val c = try { s.accept() } catch (_: Throwable) { break }
                pool.execute { handle(c, lan = true) }
            }
        }, "viaura-cast-accept").apply { isDaemon = true }.start()
        return s.localPort
    }

    private fun castSizeOf(ctx: Context, src: CastSource): Long = try {
        if (src.path.startsWith("content://")) {
            ctx.contentResolver.openFileDescriptor(Uri.parse(src.path), "r")?.use { it.statSize } ?: -1L
        } else {
            java.io.File(src.path.removePrefix("file://")).length()
        }
    } catch (_: Throwable) {
        -1L
    }

    private fun openCastInput(ctx: Context, src: CastSource, start: Long): InputStream {
        if (src.path.startsWith("content://")) {
            val pfd = ctx.contentResolver.openFileDescriptor(Uri.parse(src.path), "r") ?: throw IOException("cannot open file")
            val fis = java.io.FileInputStream(pfd.fileDescriptor)
            if (start > 0L) fis.channel.position(start)
            return object : java.io.FilterInputStream(fis) {
                override fun close() {
                    try { super.close() } finally { try { pfd.close() } catch (_: Throwable) {} }
                }
            }
        }
        val fis = java.io.FileInputStream(java.io.File(src.path.removePrefix("file://")))
        if (start > 0L) fis.channel.position(start)
        return fis
    }

    private fun handleCast(client: Socket, out: OutputStream, method: String, rawPath: String, lines: List<String>) {
        val ctx = appContext
        val token = rawPath.removePrefix("/cast/").substringBefore('/')
        val src = castSources[token]
        if (ctx == null || src == null || (method != "GET" && method != "HEAD")) {
            sendText(out, 404, "Not Found", "unknown file")
            return
        }
        val size = castSizeOf(ctx, src)
        if (size <= 0L) {
            sendText(out, 404, "Not Found", "file not readable")
            return
        }
        var start = 0L
        var end = size - 1L
        var partial = false
        val rangeHeader = lines.firstOrNull { it.startsWith("Range:", true) }?.substringAfter(':')?.trim()
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            val spec = rangeHeader.removePrefix("bytes=").substringBefore(',').trim()
            val dash = spec.indexOf('-')
            if (dash >= 0) {
                val a = spec.substring(0, dash).trim()
                val b = spec.substring(dash + 1).trim()
                if (a.isEmpty()) {
                    start = (size - (b.toLongOrNull() ?: 0L)).coerceAtLeast(0L)
                } else {
                    start = a.toLongOrNull() ?: 0L
                    if (b.isNotEmpty()) end = (b.toLongOrNull() ?: end).coerceAtMost(size - 1L)
                }
                partial = true
            }
        }
        if (start >= size || start > end) {
            try {
                out.write("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$size\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                out.flush()
            } catch (_: IOException) {}
            return
        }
        val length = end - start + 1L
        val header = StringBuilder()
        header.append(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
        header.append("Content-Type: ").append(src.mime).append("\r\n")
        header.append("Accept-Ranges: bytes\r\n")
        header.append("Content-Length: ").append(length).append("\r\n")
        if (partial) header.append("Content-Range: bytes ").append(start).append('-').append(end).append('/').append(size).append("\r\n")
        // what DLNA renderers ask for with "getcontentFeatures.dlna.org: 1"
        header.append("transferMode.dlna.org: Streaming\r\n")
        header.append("contentFeatures.dlna.org: DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000\r\n")
        header.append("Cache-Control: no-store\r\nConnection: close\r\n\r\n")
        if (method == "HEAD") {
            try { out.write(header.toString().toByteArray(Charsets.ISO_8859_1)); out.flush() } catch (_: IOException) {}
            return
        }
        val input = try {
            openCastInput(ctx, src, start)
        } catch (e: Throwable) {
            sendText(out, 502, "Bad Gateway", e.message ?: "could not read the file")
            return
        }
        try {
            out.write(header.toString().toByteArray(Charsets.ISO_8859_1))
            val buf = ByteArray(64 * 1024)
            var sent = 0L
            while (sent < length) {
                val n = input.read(buf, 0, minOf(buf.size.toLong(), length - sent).toInt())
                if (n < 0) break
                out.write(buf, 0, n)
                sent += n
            }
            out.flush()
        } catch (_: IOException) {
            // the TV closed the connection (seek / stop)
        } finally {
            try { input.close() } catch (_: Throwable) {}
        }
    }

    @Synchronized
    private fun ensureStarted(): Int {
        server?.let { if (!it.isClosed) return it.localPort }
        val s = ServerSocket(0, 64, InetAddress.getByName("127.0.0.1"))
        server = s
        Thread({
            while (!s.isClosed) {
                val c = try { s.accept() } catch (_: Throwable) { break }
                pool.execute { handle(c) }
            }
        }, "viaura-archive-accept").apply { isDaemon = true }.start()
        return s.localPort
    }

    private fun readHead(input: InputStream): String? {
        val buf = ByteArrayOutputStream()
        var last4 = 0
        while (buf.size() < 32 * 1024) {
            val b = input.read()
            if (b < 0) return null
            buf.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) return buf.toString(Charsets.ISO_8859_1.name())
        }
        return null
    }

    private fun sendText(out: OutputStream, code: Int, reason: String, message: String) {
        try {
            val body = message.toByteArray(Charsets.UTF_8)
            out.write("HTTP/1.1 $code $reason\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
            out.write(body)
            out.flush()
        } catch (_: IOException) {}
    }

    private fun sourceFor(context: Context, record: ArchiveRecord): ArchiveSource =
        if (record.sourceType == "uri") UriArchiveSource(context, Uri.parse(record.source))
        else HttpArchiveSource(record.source, record.userAgent)

    private fun handle(client: Socket, lan: Boolean = false) {
        try {
            client.tcpNoDelay = true
            client.soTimeout = 20_000
            val head = readHead(client.getInputStream()) ?: return
            client.soTimeout = 0
            val out = client.getOutputStream()
            val lines = head.split("\r\n")
            val first = lines.firstOrNull().orEmpty().split(' ')
            if (first.size < 2) { sendText(out, 400, "Bad Request", "bad request"); return }
            val method = first[0].uppercase()
            val rawPath = first[1].substringBefore('?')
            if (rawPath.startsWith("/cast/")) { handleCast(client, out, method, rawPath, lines); return }
            // the network listener (cast) never serves archive files
            if (lan) { sendText(out, 404, "Not Found", "unknown file"); return }
            val parts = first[1].substringBefore('?').removePrefix("/arc/").split('/')
            val id = parts.getOrNull(0).orEmpty()
            val index = parts.getOrNull(1)?.toIntOrNull()
            val ctx = appContext
            val record = if (ctx != null && index != null) ArchiveStore.get(ctx, id) else null
            val entry = if (record != null && index != null) record.entries.getOrNull(index) else null
            if (ctx == null || record == null || entry == null || (method != "GET" && method != "HEAD")) {
                sendText(out, 404, "Not Found", "unknown file")
                return
            }

            val size = entry.size
            var start = 0L
            var end = size - 1L
            var partial = false
            val rangeHeader = lines.firstOrNull { it.startsWith("Range:", true) }?.substringAfter(':')?.trim()
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                val spec = rangeHeader.removePrefix("bytes=").substringBefore(',').trim()
                val dash = spec.indexOf('-')
                if (dash >= 0) {
                    val a = spec.substring(0, dash).trim()
                    val b = spec.substring(dash + 1).trim()
                    if (a.isEmpty()) {
                        val suffix = b.toLongOrNull() ?: 0L
                        start = (size - suffix).coerceAtLeast(0L)
                    } else {
                        start = a.toLongOrNull() ?: 0L
                        if (b.isNotEmpty()) end = (b.toLongOrNull() ?: end).coerceAtMost(size - 1L)
                    }
                    partial = true
                }
            }
            if (size <= 0L || start >= size || start > end) {
                try {
                    out.write("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$size\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                    out.flush()
                } catch (_: IOException) {}
                return
            }
            val length = end - start + 1L

            val header = StringBuilder()
            header.append(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
            header.append("Content-Type: ").append(ArchiveEngine.mimeTypeFor(entry.name)).append("\r\n")
            header.append("Accept-Ranges: bytes\r\n")
            header.append("Content-Length: ").append(length).append("\r\n")
            if (partial) header.append("Content-Range: bytes ").append(start).append('-').append(end).append('/').append(size).append("\r\n")
            header.append("Cache-Control: no-store\r\nConnection: close\r\n\r\n")

            if (method == "HEAD") {
                try { out.write(header.toString().toByteArray(Charsets.ISO_8859_1)); out.flush() } catch (_: IOException) {}
                return
            }

            val source = sourceFor(ctx, record)
            val entryIndex = index ?: 0
            var dataOffset = entry.dataOffset
            val firstStream: InputStream = try {
                dataOffset = dataOffsetFor(source, record, entryIndex, entry)
                ArchiveEngine.openEntry(source, record.kind, entry, dataOffset, start)
            } catch (e: Throwable) {
                sendText(out, 502, "Bad Gateway", e.message ?: "could not read the archive")
                return
            }
            var input: InputStream = firstStream
            var sent = 0L
            var failures = 0
            val buf = ByteArray(64 * 1024)
            try {
                out.write(header.toString().toByteArray(Charsets.ISO_8859_1))
                while (sent < length) {
                    val n = try {
                        input.read(buf, 0, minOf(buf.size.toLong(), length - sent).toInt())
                    } catch (_: IOException) {
                        -1
                    }
                    if (n < 0) {
                        // The website closed or dropped the connection (it often does while the player's buffer is full).
                        // Continue at the exact byte instead of ending the answer short, so playback does not stutter.
                        try { input.close() } catch (_: Throwable) {}
                        if (failures >= MAX_ORIGIN_FAILURES) break
                        failures++
                        try { Thread.sleep(250L * failures) } catch (_: InterruptedException) { break }
                        input = try {
                            ArchiveEngine.openEntry(source, record.kind, entry, dataOffset, start + sent)
                        } catch (_: Throwable) {
                            break
                        }
                        continue
                    }
                    out.write(buf, 0, n)
                    sent += n
                    failures = 0
                }
                out.flush()
            } catch (_: IOException) {
                // the player closed the connection (seek / stop): nothing more to do
            } finally {
                try { input.close() } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
        } finally {
            try { client.close() } catch (_: Throwable) {}
        }
    }
}
