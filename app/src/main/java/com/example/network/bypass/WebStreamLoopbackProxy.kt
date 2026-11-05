package com.example.util

import android.content.Context
import com.example.download.HttpFetch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Plays a website's stream THROUGH THE APP: mpv opens http://127.0.0.1:<port>/s/<token> and this server fetches the real stream
 * with the same OkHttp engine that already downloads these links successfully (the app's DNS-over-HTTPS fallback, bypass route,
 * browser User-Agent and the exact headers yt-dlp gave). mpv / ffmpeg therefore never needs its own DNS, TLS, header handling or
 * cookies for the website. Byte ranges are passed on (seeking works) and HLS playlists are rewritten so every segment and key
 * goes through the app as well. 127.0.0.1 only.
 */
object WebStreamProxy {
    private class Target(val url: String, val headers: Map<String, String>)

    /** The player's side of the socket went away (stopped / seeked): nothing more to do, and never a reason to retry the website. */
    private class ClientGone : RuntimeException()

    private val targets = ConcurrentHashMap<String, Target>()
    private val tokenByKey = ConcurrentHashMap<String, String>()
    private val seq = AtomicInteger(0)
    private val pool = Executors.newCachedThreadPool(java.util.concurrent.ThreadFactory { r -> Thread(r, "viaura-webproxy-" + seq.incrementAndGet()).apply { isDaemon = true } })

    private var server: ServerSocket? = null
    @Volatile private var appContext: Context? = null

    /** The address mpv should open for [url]; [url] itself when the proxy can't run (the player then connects directly as before). */
    fun wrap(context: Context, url: String, headers: Map<String, String>): String {
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return url
        if (lower.startsWith("http://127.0.0.1") || lower.startsWith("http://localhost")) return url
        return try {
            appContext = context.applicationContext
            register(url, headers, ensureStarted())
        } catch (_: Throwable) {
            url
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
        }, "viaura-webproxy-accept").apply { isDaemon = true }.start()
        return s.localPort
    }

    private fun register(url: String, headers: Map<String, String>, port: Int): String {
        if (targets.size > 60_000) { targets.clear(); tokenByKey.clear() }
        val key = url + "\n" + headers.toSortedMap().toString()
        val token = tokenByKey.getOrPut(key) { java.lang.Long.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextLong() and Long.MAX_VALUE) }
        targets[token] = Target(url, headers)
        val ext = try {
            URI(url).path.orEmpty().substringAfterLast('/').substringAfterLast('.', "").takeIf { it.length in 1..5 && it.all { c -> c.isLetterOrDigit() } }
        } catch (_: Throwable) { null }
        return "http://127.0.0.1:$port/s/$token" + (if (ext != null) ".$ext" else "")
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

    private fun send(out: OutputStream, bytes: ByteArray) {
        try { out.write(bytes) } catch (_: IOException) { throw ClientGone() }
    }

    private fun sendText(out: OutputStream, code: Int, reason: String, message: String) {
        val body = message.toByteArray(Charsets.UTF_8)
        send(out, ("HTTP/1.1 $code $reason\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray(Charsets.ISO_8859_1))
        send(out, body)
        try { out.flush() } catch (_: IOException) {}
    }

    private fun handle(client: Socket) {
        var headersSent = false
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
            val token = first[1].substringAfter("/s/", "").substringBefore('?').substringBefore('.')
            val target = targets[token]
            if (target == null || (method != "GET" && method != "HEAD")) { sendText(out, 404, "Not Found", "unknown stream"); return }
            val range = lines.firstOrNull { it.startsWith("Range:", true) }?.substringAfter(':')?.trim()?.takeIf { it.isNotBlank() }
            val ctx = appContext ?: return
            try {
                HttpFetch.call(ctx, HttpFetch.request(target.url, target.headers, range), null) { r ->
                    val code = r.code
                    val reason = (r.message.ifBlank { "OK" }).replace('\r', ' ').replace('\n', ' ')
                    val type = r.header("Content-Type").orEmpty()
                    val finalUrl = r.request.url.toString()
                    val body = r.body
                    val looksHls = type.contains("mpegurl", true) || finalUrl.substringBefore('?').lowercase().endsWith(".m3u8")
                    if (looksHls && code in 200..299 && body != null) {
                        val raw = body.bytes()
                        val text = String(raw, Charsets.UTF_8)
                        val isPlaylist = text.trimStart('\uFEFF', ' ', '\n', '\r').startsWith("#EXTM3U")
                        val payload = if (isPlaylist) rewritePlaylist(text, finalUrl, target.headers, client.localPort).toByteArray(Charsets.UTF_8) else raw
                        val ct = if (isPlaylist) "application/vnd.apple.mpegurl" else type.ifBlank { "application/octet-stream" }
                        headersSent = true
                        send(out, "HTTP/1.1 200 OK\r\nContent-Type: $ct\r\nContent-Length: ${payload.size}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
                        if (method != "HEAD") send(out, payload)
                        return@call
                    }
                    val sb = StringBuilder("HTTP/1.1 $code $reason\r\n")
                    if (type.isNotBlank()) sb.append("Content-Type: ").append(type).append("\r\n")
                    r.header("Content-Length")?.let { sb.append("Content-Length: ").append(it).append("\r\n") }
                    r.header("Content-Range")?.let { sb.append("Content-Range: ").append(it).append("\r\n") }
                    val ranges = r.header("Accept-Ranges") ?: if (code == 206) "bytes" else null
                    ranges?.let { sb.append("Accept-Ranges: ").append(it).append("\r\n") }
                    sb.append("Connection: close\r\nCache-Control: no-store\r\n\r\n")
                    headersSent = true
                    send(out, sb.toString().toByteArray(Charsets.ISO_8859_1))
                    if (method != "HEAD" && body != null) {
                        val buf = ByteArray(64 * 1024)
                        val input = body.byteStream()
                        while (true) {
                            val n = try { input.read(buf) } catch (_: IOException) { throw ClientGone() } // the website broke the stream: the player sees the end of data
                            if (n < 0) break
                            try { out.write(buf, 0, n) } catch (_: IOException) { throw ClientGone() }
                        }
                    }
                    try { out.flush() } catch (_: IOException) {}
                }
            } catch (_: ClientGone) {
            } catch (t: Throwable) {
                if (!headersSent) sendText(out, 502, "Bad Gateway", "The website could not be reached: ${t.javaClass.simpleName} ${t.message.orEmpty()}".take(300))
            }
        } catch (_: Throwable) {
        } finally {
            try { client.close() } catch (_: Throwable) {}
        }
    }

    private fun rewritePlaylist(text: String, baseUrl: String, headers: Map<String, String>, port: Int): String {
        fun proxied(ref: String): String {
            val abs = try { URI(baseUrl).resolve(ref.trim()).toString() } catch (_: Throwable) { return ref }
            return register(abs, headers, port)
        }
        val uriAttr = Regex("URI=\"([^\"]*)\"")
        val sb = StringBuilder(text.length + 256)
        for (line in text.lines()) {
            when {
                line.isBlank() -> sb.append(line)
                line.startsWith("#") -> sb.append(uriAttr.replace(line) { m -> "URI=\"" + proxied(m.groupValues[1]) + "\"" })
                else -> sb.append(proxied(line))
            }
            sb.append('\n')
        }
        return sb.toString()
    }
}
