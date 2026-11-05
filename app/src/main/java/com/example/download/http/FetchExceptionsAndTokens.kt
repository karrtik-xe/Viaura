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

class FetchCancelled : CancellationException("cancelled")

class HttpStatusException(val code: Int, message: String) : IOException(message)

/** The link has expired (403 / 404 / 410): the page has to be asked for fresh links. */
class LinkExpiredException(message: String) : IOException(message)

internal class RangeIgnored : IOException("the server ignored the Range request")

/** Lets Pause / Stop end every running request at once (a blocking read then fails immediately). */
class CancelToken {
    @Volatile var cancelled = false
        private set
    private val calls = ConcurrentHashMap.newKeySet<Call>()

    fun cancel() {
        cancelled = true
        for (c in calls) try { c.cancel() } catch (_: Throwable) {}
    }

    fun register(c: Call) {
        calls.add(c)
        if (cancelled) c.cancel()
    }

    fun unregister(c: Call) {
        calls.remove(c)
    }

    fun check() {
        if (cancelled) throw FetchCancelled()
    }
}

data class HeadInfo(val size: Long, val contentType: String?, val fileName: String?, val acceptsRanges: Boolean, val finalUrl: String)
