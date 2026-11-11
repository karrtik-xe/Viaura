package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.Executors

/**
 * Pictures for the preview above the seek bar while the finger moves.
 *  - one MediaMetadataRetriever is opened once and reused (opening a new one for every move cost 50-200 ms each),
 *  - only the LATEST position is worked on (positions that the finger has already left are skipped),
 *  - the nearest key frame is shown at once, then the exact frame of that position replaces it as soon as it is decoded,
 *  - frames are decoded small (preview size) and cached, so going back over a spot is instant.
 */
class SeekPreviewFrameSource(
    private val context: Context,
    private val uri: Uri?,
    private val path: String?
) {
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "seek-preview").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private val cache = LruCache<Long, Bitmap>(40)
    private val lock = Any()
    private var latestTargetMs = -1L
    private var onFrame: ((Bitmap) -> Unit)? = null
    private var running = false
    @Volatile private var released = false
    private var retriever: MediaMetadataRetriever? = null

    private fun key(ms: Long) = ms / 120L

    fun request(targetMs: Long, deliver: (Bitmap) -> Unit) {
        if (released) return
        cache.get(key(targetMs))?.let { deliver(it); }
        synchronized(lock) {
            latestTargetMs = targetMs
            onFrame = deliver
            if (running) return
            running = true
        }
        try {
            worker.execute { loop() }
        } catch (_: Throwable) {
            synchronized(lock) { running = false }
        }
    }

    private fun loop() {
        while (!released) {
            val target: Long
            synchronized(lock) {
                target = latestTargetMs
                if (target < 0L) { running = false; return }
                latestTargetMs = -1L
            }
            val r = open() ?: run { synchronized(lock) { running = false }; return }
            val us = target * 1000L
            // 1) nearest key frame: fast
            val quick = frame(r, us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (quick != null) publish(quick, false, target)
            // 2) the finger moved on meanwhile: this position is no longer needed
            if (synchronized(lock) { latestTargetMs >= 0L }) continue
            // 3) exact frame of the position
            val exact = frame(r, us, MediaMetadataRetriever.OPTION_CLOSEST)
            if (exact != null) publish(exact, true, target)
        }
        synchronized(lock) { running = false }
    }

    private fun publish(bmp: Bitmap, exact: Boolean, target: Long) {
        // only exact frames are remembered: a key frame can be seconds away from the position and must never be shown again as "the frame of this spot"
        if (exact) cache.put(key(target), bmp)
        val cb = synchronized(lock) { onFrame }
        main.post { if (!released) cb?.invoke(bmp) }
    }

    private fun frame(r: MediaMetadataRetriever, us: Long, option: Int): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= 27) r.getScaledFrameAtTime(us, option, 320, 180) else r.getFrameAtTime(us, option)
    } catch (_: Throwable) {
        null
    }

    private fun open(): MediaMetadataRetriever? {
        retriever?.let { return it }
        return try {
            val r = MediaMetadataRetriever()
            when {
                uri != null -> r.setDataSource(context, uri)
                !path.isNullOrBlank() -> r.setDataSource(path)
                else -> { r.release(); return null }
            }
            retriever = r
            r
        } catch (_: Throwable) {
            null
        }
    }

    fun release() {
        released = true
        try { worker.execute { try { retriever?.release() } catch (_: Throwable) {}; retriever = null; cache.evictAll() } } catch (_: Throwable) {}
        try { worker.shutdown() } catch (_: Throwable) {}
    }
}
