package com.example.util.media

import android.content.Context
import android.os.SystemClock
import com.example.util.NetworkBypass
import com.example.util.network.ViauraDns
import com.example.util.network.ViauraHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.net.URI
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

/**
 * Parallel Server Race:
 * When a provider returns multiple servers/links for an episode, probes all of them concurrently (up to 6 at once).
 * Runs a fast check (tiny ranged GET; for HLS fetches the .m3u8 and the first segment).
 * The FIRST link that proves it works wins immediately and every other probe is cancelled: their network calls are
 * cancelled too (OkHttp [Call.cancel]), so a slow server can never delay the winner.
 * Per-server timeout is 7 seconds. All requests use the shared [ViauraHttpClient] (same DoH / DNS fallback as everything else).
 * Records server performance and latencies in [ServerReputationStore].
 */
object StreamProbe {
    private const val PER_SERVER_TIMEOUT_MS = 7_000L
    private const val MAX_RACE_CANDIDATES = 6
    /** A server that is ranked higher (CloudStream order: better quality first) may still win when it proves itself within this time of the first one. */
    private const val RANK_GRACE_MS = 700L
    /** A server of the user's quality choice may still be checked this long when a worse one answered first. */
    private const val QUALITY_GRACE_MS = 3_500L

    private val PLAYER_OWN_HEADERS = setOf(
        "host", "connection", "content-length", "content-type", "accept-encoding", "range", "if-range",
        "transfer-encoding", "te", "upgrade-insecure-requests"
    )

    class Outcome(val ordered: List<MediaStream>, val anyWorks: Boolean)

    /** The network calls of one probe, so they can all be cancelled the moment the probe is cancelled. */
    private class Probe {
        private val calls = ConcurrentLinkedQueue<Call>()
        @Volatile var cancelled = false

        fun track(call: Call): Call {
            calls.add(call)
            if (cancelled) call.cancel()
            return call
        }

        fun cancel() {
            cancelled = true
            for (c in calls) {
                try { c.cancel() } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Executes the parallel server race on [ranked] streams.
     * The first working link wins immediately and cancels the rest.
     * [Outcome.anyWorks] is false when none of the probed servers proved to work (the list is still returned in order,
     * so the player's own failover can try them one after another).
     */
    suspend fun verify(context: Context, pluginId: String, ranked: List<MediaStream>): Outcome {
        if (ranked.isEmpty()) return Outcome(emptyList(), false)

        val app = context.applicationContext
        ViauraDns.init(app)

        // Fast / reliable servers first, recently failed ones last
        // (the user's language / quality choice of Settings > Network > Media settings outranks the reputation order)
        val sticky = MediaPreferences.stickyFor(app, pluginId)
        val prefActive = MediaPreferences.active(app) || sticky != null
        val prioritized = ServerReputationStore.rankCandidateStreams(app, pluginId, ranked).let { byReputation ->
            if (prefActive) byReputation.sortedByDescending { MediaPreferences.score(app, it, sticky) } else byReputation
        }
        val candidates = prioritized.take(MAX_RACE_CANDIDATES)
        val scores = candidates.map { if (prefActive) MediaPreferences.score(app, it, sticky) else 0 }

        val winner: Pair<MediaStream, Long>? = coroutineScope {
            val first = CompletableDeferred<Unit>()
            // index in [candidates] -> latency, for every probe that worked
            val worked = java.util.concurrent.ConcurrentHashMap<Int, Long>()
            // index in [candidates] -> true once that probe is over (worked or failed)
            val over = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
            val jobs = candidates.mapIndexed { index, stream ->
                launch(Dispatchers.IO) {
                    val started = SystemClock.elapsedRealtime()
                    val ok = probeOne(app, stream)
                    val latency = SystemClock.elapsedRealtime() - started
                    if (ok) {
                        worked[index] = latency
                    } else {
                        ServerReputationStore.recordFailure(app, pluginId, stream.server)
                    }
                    over[index] = true
                    if (ok) first.complete(Unit)
                }
            }
            // every probe failed: nobody won
            launch {
                jobs.joinAll()
                first.complete(Unit)
            }
            first.await()
            // CloudStream plays the best-ranked link. The first server that answered wins immediately, unless a better-ranked
            // one is still being checked: that one gets a short moment to prove itself too.
            fun waitingBetter(): Boolean {
                val best = worked.keys.minOrNull() ?: return false
                return (0 until best).any { over[it] != true }
            }
            // A server that fits the user's language / quality / last choice better is waited for much longer than a plain
            // better-ranked one: a fast server of the wrong language must not win just because it answered first.
            fun graceMs(): Long {
                val best = worked.keys.minOrNull() ?: return 0L
                var grace = RANK_GRACE_MS
                for (i in 0 until best) {
                    if (over[i] == true) continue
                    val diff = scores[i] - scores[best]
                    grace = maxOf(grace, when {
                        diff >= 1000 -> PER_SERVER_TIMEOUT_MS
                        diff >= 15 -> QUALITY_GRACE_MS
                        else -> RANK_GRACE_MS
                    })
                }
                return grace
            }
            val waitStart = SystemClock.elapsedRealtime()
            while (waitingBetter() && SystemClock.elapsedRealtime() - waitStart < graceMs()) delay(25L)
            val bestIndex = worked.keys.minOrNull()
            val result = bestIndex?.let { Pair(candidates[it], worked[it] ?: 0L) }
            // winner found (or all done): stop every other probe right now
            coroutineContext.cancelChildren()
            result
        }

        if (winner != null) {
            ServerReputationStore.recordSuccess(app, pluginId, winner.first.server, winner.second)
            val reordered = ArrayList<MediaStream>(prioritized.size)
            reordered.add(winner.first)
            for (s in prioritized) if (s.link != winner.first.link) reordered.add(s)
            return Outcome(reordered, true)
        }
        return Outcome(prioritized, false)
    }

    /** Compatibility helper */
    suspend fun verify(context: Context, ranked: List<MediaStream>): Outcome =
        verify(context, "", ranked)

    /** One server: timeout 7 s, cancellable, never throws (except cancellation). */
    private suspend fun probeOne(context: Context, stream: MediaStream): Boolean {
        val probe = Probe()
        return try {
            withTimeoutOrNull(PER_SERVER_TIMEOUT_MS) {
                coroutineScope {
                    val work = async(Dispatchers.IO) {
                        try { checkStream(context, stream, probe) } catch (_: Throwable) { false }
                    }
                    try {
                        work.await()
                    } catch (e: CancellationException) {
                        // timeout or a faster server won: abort the blocking network calls right away
                        probe.cancel()
                        throw e
                    }
                }
            } ?: false
        } catch (e: CancellationException) {
            probe.cancel()
            throw e
        } catch (_: Throwable) {
            probe.cancel()
            false
        }
    }

    /** Fast check of a single media stream (blocking; call from a background thread). */
    fun checkStream(context: Context, s: MediaStream): Boolean = checkStream(context, s, Probe())

    private fun checkStream(context: Context, s: MediaStream, probe: Probe): Boolean {
        val app = context.applicationContext
        ViauraDns.init(app)
        if (check(s, clientFor(null), probe)) return true
        if (probe.cancelled) return false
        // direct failed: the same check through the local anti-DPI proxy, when it is on
        val proxy = try { NetworkBypass.okHttpProxy(app) } catch (_: Throwable) { null }
        return proxy != null && proxy.type() != Proxy.Type.DIRECT && check(s, clientFor(proxy), probe)
    }

    @Volatile private var directBase: OkHttpClient? = null
    @Volatile private var directDerived: OkHttpClient? = null

    private fun buildClient(proxy: Proxy?): OkHttpClient =
        ViauraHttpClient.get().newBuilder()
            .proxy(proxy ?: Proxy.NO_PROXY)
            .connectTimeout(3_500, TimeUnit.MILLISECONDS)
            .readTimeout(4_000, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    /** Short-timeout client sharing the app-wide pool / DNS (re-derived when the shared client is re-created after a network change). */
    private fun clientFor(proxy: Proxy?): OkHttpClient {
        if (proxy != null) return buildClient(proxy)
        val base = ViauraHttpClient.get()
        val cached = directDerived
        if (cached != null && directBase === base) return cached
        val derived = buildClient(null)
        directBase = base
        directDerived = derived
        return derived
    }

    private fun request(url: String, headers: Map<String, String>, range: String?): Request? {
        return try {
            val b = Request.Builder().url(url)
            for ((k, v) in headers) {
                if (k.isNotBlank() && v.isNotBlank() && k.lowercase() !in PLAYER_OWN_HEADERS) {
                    try { b.header(k, v) } catch (_: Throwable) {}
                }
            }
            if (headers.keys.none { it.equals("User-Agent", true) }) {
                b.header("User-Agent", ViauraHttpClient.DEFAULT_USER_AGENT)
            }
            if (range != null) b.header("Range", range)
            b.build()
        } catch (_: Throwable) {
            null
        }
    }

    private fun readHead(ins: java.io.InputStream, size: Int): ByteArray {
        val buf = ByteArray(size)
        var n = 0
        while (n < buf.size) {
            val c = ins.read(buf, n, buf.size - n)
            if (c <= 0) break
            n += c
        }
        return if (n == buf.size) buf else buf.copyOf(n)
    }

    private fun fetchText(url: String, headers: Map<String, String>, client: OkHttpClient, probe: Probe): String? {
        val req = request(url, headers, null) ?: return null
        return try {
            probe.track(client.newCall(req)).execute().use { r ->
                if (!r.isSuccessful) return null
                val ins = r.body?.byteStream() ?: return null
                String(readHead(ins, 64 * 1024), Charsets.UTF_8)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun checkFirstByte(url: String, headers: Map<String, String>, client: OkHttpClient, probe: Probe, range: String?): Boolean {
        val req = request(url, headers, range) ?: return false
        return try {
            probe.track(client.newCall(req)).execute().use { r ->
                if (r.code !in 200..299 && r.code != 206) return false
                val type = (r.header("Content-Type") ?: "").lowercase()
                if (type.startsWith("text/html") || type.contains("json") || (type.startsWith("text/plain") && !url.contains(".txt"))) {
                    return false
                }
                val ins = r.body?.byteStream() ?: return false
                val head = readHead(ins, 32)
                head.isNotEmpty() && !looksLikeWebPage(head, head.size)
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun looksLikeWebPage(b: ByteArray, n: Int): Boolean {
        var i = 0
        if (n >= 3 && b[0] == 0xEF.toByte() && b[1] == 0xBB.toByte() && b[2] == 0xBF.toByte()) i = 3
        while (i < n && (b[i] == ' '.code.toByte() || b[i] == '\n'.code.toByte() || b[i] == '\r'.code.toByte() || b[i] == '\t'.code.toByte())) i++
        if (i >= n) return false
        return b[i] == '<'.code.toByte() || b[i] == '{'.code.toByte()
    }

    private fun isKeyReachable(url: String, headers: Map<String, String>, client: OkHttpClient, probe: Probe): Boolean {
        val req = request(url, headers, null) ?: return false
        return try {
            probe.track(client.newCall(req)).execute().use { r ->
                if (!r.isSuccessful) return false
                val ins = r.body?.byteStream() ?: return false
                val head = readHead(ins, 32)
                head.size >= 16 && !looksLikeWebPage(head, head.size)
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun resolveUri(base: String, ref: String): String =
        try { URI(base.substringBefore('#')).resolve(ref.trim()).toString() } catch (_: Throwable) { ref }

    private fun check(s: MediaStream, client: OkHttpClient, probe: Probe): Boolean {
        val link = s.link
        val h = s.headers
        return try {
            when {
                HlsVariants.looksLikeHls(link, s.type) -> {
                    var url = link
                    var body = fetchText(url, h, client, probe) ?: return false
                    if (!body.trimStart('﻿', ' ', '\n', '\r').startsWith("#EXTM3U")) return false
                    if (body.contains("#EXT-X-STREAM-INF")) {
                        val variant = body.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return true
                        url = resolveUri(url, variant)
                        body = fetchText(url, h, client, probe) ?: return false
                    }
                    val keyUri = Regex("#EXT-X-KEY:[^\\n]*METHOD=(?!NONE)[^\\n]*URI=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)
                    if (keyUri != null && !keyUri.startsWith("data:", true) && !isKeyReachable(resolveUri(url, keyUri), h, client, probe)) {
                        return false
                    }
                    val seg = body.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return true
                    checkFirstByte(resolveUri(url, seg), h, client, probe, null)
                }
                s.type.equals("mpd", true) || s.type.equals("dash", true) || link.substringBefore('?').lowercase().endsWith(".mpd") ->
                    (fetchText(link, h, client, probe) ?: return false).contains("<MPD")
                else -> checkFirstByte(link, h, client, probe, "bytes=0-1024")
            }
        } catch (_: Throwable) {
            false
        }
    }
}
