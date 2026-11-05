package com.example.util.media

import android.content.Context
import android.net.Uri
import com.example.ui.screens.VideoItem
import com.example.util.PlaylistManager
import com.example.util.StreamEntryMeta
import com.example.util.StreamEntryStore
import com.example.util.network.ViauraHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class PluginQueuedEpisode(
    val plugin: MediaPluginInfo,
    val playerTitle: String,
    val poster: String,
    val link: String,
    val type: String,
    val episodeIndex: Int,
    val episodeTitle: String,
    val isSeries: Boolean,
    val choices: List<PluginQualityChoice>,
    val selectedQuality: String
)

/**
 * Preloads and caches the next episode links and validated VideoItem for 15 minutes.
 */
object PluginQueuedEpisodes {
    const val SCHEME = "pluginqueue://"
    private const val PRELOAD_TTL_MS = 15 * 60_000L // 15 minutes

    private val map = ConcurrentHashMap<String, PluginQueuedEpisode>()
    // Path -> (Resolved VideoItem, Cached timestamp)
    private val preloadedCache = ConcurrentHashMap<String, Pair<VideoItem, Long>>()

    fun isQueued(path: String?): Boolean = path != null && path.startsWith(SCHEME)

    fun placeholder(ep: PluginQueuedEpisode): VideoItem {
        val key = SCHEME + ep.plugin.value + "/" + kotlin.math.abs((ep.plugin.value + "|" + ep.link).hashCode()) + "/" + ep.episodeIndex
        map[key] = ep
        return VideoItem(
            id = -(kotlin.math.abs(key.hashCode().toLong())) - 1L,
            uri = Uri.parse(key),
            displayName = ep.playerTitle,
            path = key,
            sizeBytes = 0L,
            durationMs = 0L,
            dateModified = System.currentTimeMillis() / 1000L,
            isOnline = true,
            thumbnailUrl = ep.poster.ifBlank { null },
            channelName = ep.plugin.name,
            isDirectStream = true
        )
    }

    // Episodes that are being resolved right now (path -> result), so a tap on Next while the background preload is
    // still running waits for that one instead of starting a second race.
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Result<VideoItem>>>()

    private fun fresh(path: String): VideoItem? {
        val hit = preloadedCache[path] ?: return null
        return if (System.currentTimeMillis() - hit.second < PRELOAD_TTL_MS) hit.first else null
    }

    /** Resolves [path] (all servers raced in parallel), caches the result and returns it. */
    private suspend fun load(context: Context, path: String, ep: PluginQueuedEpisode): Result<VideoItem> {
        val result = MediaPlayback.resolve(
            context, ep.plugin, ep.link, ep.type, ep.episodeTitle,
            if (ep.isSeries) ep.episodeIndex else -1, verify = true
        )
        val stream = result.getOrNull()
            ?: return Result.failure(result.exceptionOrNull() ?: PluginException("No playable link was found"))
        val session = PluginPlaybackSession(
            plugin = ep.plugin,
            episodeIndex = ep.episodeIndex,
            episodeTitle = ep.episodeTitle,
            isSeries = ep.isSeries,
            choices = ep.choices,
            selectedQuality = ep.selectedQuality,
            subtitles = stream.subtitles
        )
        session.episodeLink = ep.link
        session.episodeType = ep.type
        val item = MediaPlayback.toVideoItem(context, ep.plugin, ep.playerTitle, ep.poster, stream, session)
        preloadedCache[path] = Pair(item, System.currentTimeMillis())
        return Result.success(item)
    }

    /** Runs [load] once per path; concurrent callers share the same attempt. */
    private suspend fun loadShared(context: Context, path: String, ep: PluginQueuedEpisode): Result<VideoItem> {
        val mine = CompletableDeferred<Result<VideoItem>>()
        val running = inFlight.putIfAbsent(path, mine)
        if (running != null) {
            return try { running.await() } catch (e: CancellationException) { throw e } catch (t: Throwable) { Result.failure<VideoItem>(t) }
        }
        return try {
            val r = try { load(context, path, ep) } catch (e: CancellationException) { throw e } catch (t: Throwable) { Result.failure<VideoItem>(t) }
            mine.complete(r)
            r
        } finally {
            if (!mine.isCompleted) mine.complete(Result.failure<VideoItem>(PluginException("Cancelled")))
            inFlight.remove(path, mine)
        }
    }

    /**
     * Resolves and pre-validates the next episode links in the background, caching them for 15 minutes.
     * Runs in the caller's coroutine, so leaving the player (which cancels it) also stops the background work.
     */
    suspend fun preloadNext(context: Context, path: String) = withContext(Dispatchers.IO) {
        val ep = map[path] ?: return@withContext
        if (fresh(path) != null) return@withContext
        try {
            loadShared(context, path, ep)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {}
        Unit
    }

    /** Quick backward-compatible prefetch */
    suspend fun prefetch(context: Context, path: String) {
        preloadNext(context, path)
    }

    /**
     * Returns the preloaded item instantly if it is less than 15 min old (links expire), or resolves it now:
     * every server of the episode is tried at the same time and the first one that works is returned.
     */
    suspend fun resolve(context: Context, path: String): Result<VideoItem> {
        fresh(path)?.let { return Result.success(it) }
        val ep = map[path] ?: return Result.failure(PluginException("This episode is no longer queued"))
        return withContext(Dispatchers.IO) {
            val r = loadShared(context, path, ep)
            // a cancelled background preload must not fail the tap: try once more on our own
            if (r.isFailure && fresh(path) == null && inFlight[path] == null && r.exceptionOrNull()?.message == "Cancelled") {
                loadShared(context, path, ep)
            } else r
        }
    }

    fun clearPreloadCache() {
        preloadedCache.clear()
    }

    /** Called when the player is left: nothing from this queue keeps running in the background. */
    fun cancelBackgroundWork() {
        for ((_, d) in inFlight) {
            try { d.complete(Result.failure<VideoItem>(PluginException("Cancelled"))) } catch (_: Throwable) {}
        }
    }
}
