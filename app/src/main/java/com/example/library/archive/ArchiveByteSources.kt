package com.example.util.archive

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/** An archive the user picked with the system file picker (content:// or file://). Read in place, never copied. */
class UriArchiveSource(private val context: Context, private val uri: Uri) : ArchiveSource {
    private var cachedLength = -1L

    override fun length(): Long {
        if (cachedLength >= 0) return cachedLength
        var size = -1L
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { size = it.statSize }
        } catch (_: Throwable) {}
        if (size <= 0L) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) size = c.getLong(0)
                }
            } catch (_: Throwable) {}
        }
        if (size <= 0L) throw ArchiveException("Could not read the size of this file")
        cachedLength = size
        return size
    }

    override fun open(offset: Long, length: Long): InputStream {
        val pfd = try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: SecurityException) {
            throw ArchiveException("No permission to read this file any more. Add the archive again.")
        } ?: throw ArchiveException("Could not open the archive file")
        val stream = ParcelFileDescriptor.AutoCloseInputStream(pfd)
        try {
            stream.channel.position(offset)
        } catch (t: Throwable) {
            try { stream.close() } catch (_: Throwable) {}
            throw ArchiveException("This storage location does not allow jumping inside the file")
        }
        return LimitedInputStream(stream, length)
    }
}

/** An archive on a website. Only the needed parts are requested (HTTP Range), so the whole archive is never downloaded. */
class HttpArchiveSource(
    private val url: String,
    private val userAgent: String?
) : ArchiveSource {
    private var cachedLength = -1L

    override fun length(): Long {
        if (cachedLength >= 0) return cachedLength
        // "bytes=0-0" is answered by every range-capable server with "Content-Range: bytes 0-0/<total>"
        val response = call(0L, 0L)
        response.use { r ->
            if (r.code == 206) {
                val total = r.header("Content-Range")?.substringAfterLast('/')?.trim()?.toLongOrNull()
                if (total != null && total > 0L) {
                    cachedLength = total
                    return total
                }
            }
            if (r.code == 200) {
                throw ArchiveException(
                    "This website does not allow partial reading (no Range support), so the archive cannot be played without downloading all of it."
                )
            }
            if (r.code == 416) throw ArchiveException("The archive link is empty")
            if (!r.isSuccessful) throw ArchiveException(httpMessage(r.code))
            throw ArchiveException("The website did not tell the size of the archive")
        }
    }

    override fun open(offset: Long, length: Long): InputStream {
        val response = call(offset, offset + length - 1L)
        if (response.code != 206) {
            val code = response.code
            val acceptedFull = code == 200 && offset == 0L
            if (!acceptedFull) {
                response.close()
                throw ArchiveException(if (code == 200) "The website ignored the partial request" else httpMessage(code))
            }
        }
        val body = response.body ?: run {
            response.close()
            throw ArchiveException("Empty answer from the website")
        }
        return LimitedInputStream(CloseResponseStream(body.byteStream(), response), length)
    }

    private fun call(first: Long, last: Long): Response {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_UA)
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("Range", "bytes=$first-$last")
        try {
            return client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw ArchiveException(e.message?.takeIf { it.isNotBlank() } ?: "Network error")
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        401, 403 -> "The server refused access (HTTP $code). Try again with a different User-Agent."
        404 -> "The archive link was not found (HTTP 404)"
        else -> "The server answered with HTTP $code"
    }

    companion object {
        private const val DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

        private val client: OkHttpClient
            get() = try {
                com.example.util.network.ViauraHttpClient.get()
            } catch (_: Throwable) {
                fallbackClient
            }

        private val fallbackClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }
}

/** Stops after [remaining] bytes, whatever the wrapped stream still has. */
internal class LimitedInputStream(input: InputStream, private var remaining: Long) : FilterInputStream(input) {
    override fun read(): Int {
        if (remaining <= 0L) return -1
        val b = super.read()
        if (b >= 0) remaining--
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (remaining <= 0L) return -1
        val n = super.read(b, off, minOf(len.toLong(), remaining).toInt())
        if (n > 0) remaining -= n
        return n
    }

    override fun skip(n: Long): Long {
        val skipped = super.skip(minOf(n, remaining))
        if (skipped > 0) remaining -= skipped
        return skipped
    }

    override fun available(): Int = minOf(super.available().toLong(), remaining).toInt()
}

private class CloseResponseStream(input: InputStream, private val response: Response) : FilterInputStream(input) {
    override fun close() {
        try { super.close() } catch (_: Throwable) {}
        try { response.close() } catch (_: Throwable) {}
    }
}
