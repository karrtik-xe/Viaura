package com.example.util.media

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks server reliability, response latency, and recent failures per provider.
 * - Prioritizes historically fast & reliable servers during parallel server race.
 * - Demotes recently failed servers to the end of candidate lists.
 * - Failure penalty automatically expires after 10 minutes.
 * - In-memory cache + lightweight persisted SharedPreferences.
 */
object ServerReputationStore {
    private const val PREFS_NAME = "viaura_server_reputation"
    private const val PENALTY_EXPIRE_MS = 10 * 60_000L // 10 minutes
    private const val CHOICE_PREFIX = "choice|"
    /** A provider-wide choice (the server of the last manual switch / download) is used for the next episodes for this long. */
    private const val CHOICE_TTL_PROVIDER_MS = 6 * 60 * 60_000L
    /** A choice made for one series (download folder) stays much longer. */
    private const val CHOICE_TTL_SERIES_MS = 30L * 24 * 60 * 60_000L

    /** The server (and its quality) the user picked last: the next episode / download starts with the same one. */
    data class Choice(val server: String, val quality: String, val savedMs: Long)

    data class ServerRecord(
        var successCount: Int = 0,
        var failureCount: Int = 0,
        var lastSuccessMs: Long = 0L,
        var lastFailureMs: Long = 0L,
        var avgLatencyMs: Long = 0L
    )

    private val records = ConcurrentHashMap<String, ServerRecord>()
    @Volatile private var prefs: SharedPreferences? = null

    private fun key(pluginId: String, serverName: String): String =
        "$pluginId|${serverName.trim().lowercase()}"

    fun init(context: Context) {
        if (prefs == null) {
            synchronized(this) {
                if (prefs == null) {
                    val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs = p
                    // Load existing records
                    try {
                        for ((k, v) in p.all) {
                            if (k.startsWith(CHOICE_PREFIX)) continue
                            if (v is String) {
                                val json = JSONObject(v)
                                val r = ServerRecord(
                                    successCount = json.optInt("sc", 0),
                                    failureCount = json.optInt("fc", 0),
                                    lastSuccessMs = json.optLong("ls", 0L),
                                    lastFailureMs = json.optLong("lf", 0L),
                                    avgLatencyMs = json.optLong("lat", 0L)
                                )
                                records[k] = r
                            }
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    private fun choiceKey(pluginId: String, scope: String): String =
        CHOICE_PREFIX + pluginId + "|" + scope.trim().lowercase()

    /**
     * Remembers the server (and quality) the user chose by hand or downloaded from, so the next episode starts with the
     * same one. [scope] is the series name (download folder) - blank = only the provider-wide choice.
     */
    fun rememberChoice(context: Context, pluginId: String, scope: String, server: String, quality: String?) {
        if (pluginId.isBlank() || server.isBlank()) return
        init(context)
        val p = prefs ?: return
        try {
            val value = JSONObject()
                .put("srv", server.trim())
                .put("q", quality.orEmpty().trim())
                .put("t", System.currentTimeMillis())
                .toString()
            val e = p.edit().putString(choiceKey(pluginId, ""), value)
            if (scope.isNotBlank()) e.putString(choiceKey(pluginId, scope), value)
            e.apply()
        } catch (_: Throwable) {}
    }

    /** The remembered choice of the series [scope] (when set), else the provider-wide one; null when none / expired. */
    fun lastChoice(context: Context, pluginId: String, scope: String = ""): Choice? {
        if (pluginId.isBlank()) return null
        init(context)
        val p = prefs ?: return null
        val now = System.currentTimeMillis()
        fun read(key: String, ttl: Long): Choice? = try {
            val raw = p.getString(key, null)
            if (raw == null) null else {
                val j = JSONObject(raw)
                val t = j.optLong("t", 0L)
                val srv = j.optString("srv", "")
                if (srv.isBlank() || now - t > ttl) null else Choice(srv, j.optString("q", ""), t)
            }
        } catch (_: Throwable) { null }
        return (if (scope.isNotBlank()) read(choiceKey(pluginId, scope), CHOICE_TTL_SERIES_MS) else null)
            ?: read(choiceKey(pluginId, ""), CHOICE_TTL_PROVIDER_MS)
    }

    /** Forgets every remembered choice (the language / quality setting changed: the new setting must apply again). */
    fun clearChoices(context: Context) {
        init(context)
        val p = prefs ?: return
        try {
            val e = p.edit()
            for (k in p.all.keys) if (k.startsWith(CHOICE_PREFIX)) e.remove(k)
            e.apply()
        } catch (_: Throwable) {}
    }

    /**
     * Records a successful probe or playback start for a server.
     */
    fun recordSuccess(context: Context, pluginId: String, serverName: String, latencyMs: Long) {
        init(context)
        val k = key(pluginId, serverName)
        val r = records.getOrPut(k) { ServerRecord() }
        synchronized(r) {
            r.successCount++
            r.lastSuccessMs = System.currentTimeMillis()
            r.avgLatencyMs = if (r.avgLatencyMs <= 0L) latencyMs else ((r.avgLatencyMs * 3 + latencyMs) / 4)
        }
        persist(k, r)
    }

    /**
     * Records a probe failure, playback timeout, or buffering stall for a server.
     */
    fun recordFailure(context: Context, pluginId: String, serverName: String) {
        init(context)
        val k = key(pluginId, serverName)
        val r = records.getOrPut(k) { ServerRecord() }
        synchronized(r) {
            r.failureCount++
            r.lastFailureMs = System.currentTimeMillis()
        }
        persist(k, r)
    }

    /**
     * Sorts candidate streams for a provider:
     * 1. Proven/fast working servers first (lowest latency, highest success rate).
     * 2. Unseen servers in provider's default order.
     * 3. Recently failed servers pushed to the end (unless penalty expired).
     */
    fun rankCandidateStreams(context: Context, pluginId: String, streams: List<MediaStream>): List<MediaStream> {
        if (streams.size <= 1) return streams
        init(context)
        val now = System.currentTimeMillis()

        return streams.sortedWith { a, b ->
            val recA = records[key(pluginId, a.server)]
            val recB = records[key(pluginId, b.server)]

            val penaltyA = recA != null && (now - recA.lastFailureMs < PENALTY_EXPIRE_MS)
            val penaltyB = recB != null && (now - recB.lastFailureMs < PENALTY_EXPIRE_MS)

            when {
                // If one has a recent penalty and the other does not, push penalized to back
                penaltyA && !penaltyB -> 1
                !penaltyA && penaltyB -> -1
                // Both penalized: pick the one with older failure or fewer failures
                penaltyA && penaltyB -> {
                    val countComp = (recA?.failureCount ?: 0).compareTo(recB?.failureCount ?: 0)
                    if (countComp != 0) countComp else (recA?.lastFailureMs ?: 0L).compareTo(recB?.lastFailureMs ?: 0L)
                }
                // Neither penalized: prefer server with higher success rate and lower latency
                else -> {
                    val scA = recA?.successCount ?: 0
                    val scB = recB?.successCount ?: 0
                    if (scA > 0 && scB > 0) {
                        // Compare latency (lower is better)
                        val latA = recA?.avgLatencyMs ?: Long.MAX_VALUE
                        val latB = recB?.avgLatencyMs ?: Long.MAX_VALUE
                        latA.compareTo(latB)
                    } else if (scA > 0) {
                        -1 // a has proven success, prefer it
                    } else if (scB > 0) {
                        1  // b has proven success, prefer it
                    } else {
                        0  // preserve original provider order
                    }
                }
            }
        }
    }

    private fun persist(key: String, r: ServerRecord) {
        try {
            val p = prefs ?: return
            val json = JSONObject().apply {
                put("sc", r.successCount)
                put("fc", r.failureCount)
                put("ls", r.lastSuccessMs)
                put("lf", r.lastFailureMs)
                put("lat", r.avgLatencyMs)
            }.toString()
            p.edit().putString(key, json).apply()
        } catch (_: Throwable) {}
    }
}
