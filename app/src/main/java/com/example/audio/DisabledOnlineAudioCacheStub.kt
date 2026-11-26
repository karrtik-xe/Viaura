package com.example.player

import android.content.Context
import android.util.Log
import com.example.download.HttpFetch
import com.example.download.PlanPart
import com.example.download.CancelToken
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Small rolling look-ahead cache of online audio: the next tracks of the queue are downloaded in the background
 * while the current song plays, so the switch to the next one needs no buffering. It is not an offline library:
 * only the tracks that are about to play (and the one that plays) are kept, everything else is removed by
 * [retainOnly], and the folder is emptied on every app start.
 */
object OnlineAudioCache {
    private const val TAG = "OnlineAudioCache"
    private const val DIR_NAME = "online_audio_cache"
    private const val MAX_TRACK_MS = 15L * 60_000L

    @Volatile private var startupCleared = false
    private val running = ConcurrentHashMap<String, CancelToken>()

    private fun dir(context: Context): File = File(context.applicationContext.cacheDir, DIR_NAME)

    private fun clearOnce(context: Context) {
        if (startupCleared) return
        synchronized(this) {
            if (startupCleared) return
            runCatching { dir(context).deleteRecursively() }
            startupCleared = true
        }
    }

    private fun key(videoUrl: String): String =
        MessageDigest.getInstance("MD5").digest(videoUrl.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun fileFor(context: Context, videoUrl: String) = File(dir(context), key(videoUrl) + ".audio")

    /** The fully downloaded copy of [videoUrl], or null while it is missing / still loading. */
    fun cachedFile(context: Context, videoUrl: String): File? {
        clearOnce(context)
        val f = fileFor(context, videoUrl)
        return if (HttpFetch.isDone(f) && f.length() > 0L) f else null
    }

    fun delete(context: Context, videoUrl: String) {
        clearOnce(context)
        running.remove(key(videoUrl))?.cancel()
        removeFiles(fileFor(context, videoUrl))
    }

    private fun removeFiles(f: File) {
        runCatching { f.delete() }
        runCatching { File(f.path + ".done").delete() }
        runCatching { File(f.path + ".chunks").delete() }
    }

    /** Everything except [keepUrls] is stopped and removed (the window moves on with the queue). */
    fun retainOnly(context: Context, keepUrls: Collection<String>) {
        clearOnce(context)
        val keep = keepUrls.map { key(it) }.toSet()
        running.keys.filter { it !in keep }.forEach { k -> running.remove(k)?.cancel() }
        dir(context).listFiles()?.forEach { f ->
            val k = f.name.substringBefore('.')
            if (k !in keep) runCatching { f.delete() }
        }
    }

    /**
     * Downloads the audio of [videoUrl] from [streamUrl] (a partly downloaded copy continues where it stopped).
     * Returns true when the complete file is there. A very long track is not cached.
     */
    suspend fun prefetch(context: Context, videoUrl: String, streamUrl: String, durationMs: Long): Boolean {
        clearOnce(context)
        if (streamUrl.isBlank() || !streamUrl.startsWith("http")) return false
        if (durationMs > MAX_TRACK_MS) return false
        cachedFile(context, videoUrl)?.let { return true }
        val k = key(videoUrl)
        val token = CancelToken()
        if (running.putIfAbsent(k, token) != null) return false
        val dest = fileFor(context, videoUrl)
        return try {
            val part = PlanPart(role = "audio", url = streamUrl, chunkSize = 4L * 1024 * 1024)
            HttpFetch.download(context.applicationContext, part, dest, token, AtomicLong(0L), AtomicLong(0L), connections = 3)
            HttpFetch.isDone(dest) && dest.length() > 0L
        } catch (e: kotlinx.coroutines.CancellationException) {
            token.cancel()
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "Prefetch failed: ${t.message}")
            removeFiles(dest)
            false
        } finally {
            running.remove(k, token)
        }
    }
}
