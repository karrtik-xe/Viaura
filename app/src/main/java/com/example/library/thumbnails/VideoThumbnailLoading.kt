package com.example.ui.components

import com.example.ui.theme.AccentMid
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import com.example.util.MkvManagerEngine

/**
 * Dedicated bounded dispatcher for thumbnail generation so fast scrolling
 * never starves the CPU or UI thread, and queues do not accumulate stale tasks.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(
    Runtime.getRuntime().availableProcessors().coerceIn(4, 6)
)

// Library-wide background warming gets its OWN small pool, so thumbnails of the cards the user is
// actually looking at are never queued behind thousands of prefetch jobs.
private val prefetchThumbnailDispatcher = Dispatchers.IO.limitedParallelism(
    (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
)

/**
 * Cache-only lookup (memory, then the saved file). Never opens the video and never touches the network, so a card can keep asking
 * cheaply until the background extraction has produced the picture.
 */
fun peekCachedVideoThumbnail(
    context: Context,
    videoId: Long,
    uri: Uri,
    path: String,
    strategy: ThumbnailStrategy,
    quality: ThumbnailQuality
): Bitmap? {
    val diskKey = VideoThumbnailDiskCache.key(videoId, uri, path, strategy, quality, 0)
    val cacheKey = "video_thumbnail_$diskKey"
    ThumbnailCache.memoryCache.get(cacheKey)?.let { if (!it.isRecycled) return it }
    val saved = VideoThumbnailDiskCache.read(context, diskKey, true) ?: return null
    ThumbnailCache.memoryCache.put(cacheKey, saved)
    return saved
}

suspend fun loadVideoThumbnail(
    context: Context,
    videoId: Long,
    uri: Uri,
    path: String,
    strategy: ThumbnailStrategy = ThumbnailStrategy.SMART,
    quality: ThumbnailQuality = ThumbnailQuality.HIGH,
    allowNetwork: Boolean = false,
    fallbackSecond: Int = 1,
    background: Boolean = false
): Bitmap? = withContext(Dispatchers.IO) {
    // Memory/disk cache hits are answered here on the unbounded IO pool, so an already-saved thumbnail never
    // waits in the queue behind slow frame extractions. Only real extraction uses the bounded pools below.
    if (!isActive) return@withContext null

    val targetWidth = quality.sizeDp.coerceIn(120, 720)
    val targetHeight = (targetWidth * 9f / 16f).roundToInt().coerceAtLeast(68)
    val isNetworkUri = uri.scheme == "http" || uri.scheme == "https"
    if (isNetworkUri && !allowNetwork) return@withContext null
    // A video inside a ZIP / TAR (file or link) never gets a thumbnail picture made from its frames: that would decode the
    // video and, for a link, use a lot of internet. Its details come from the media information (see ArchiveProbe) instead.
    if (path.isNotBlank() && com.example.util.archive.ArchiveStore.isArchivePath(path)) return@withContext null

    // The selected strategies no longer use a user-picked second. Keep the old parameter in
    // the public signature for source compatibility with existing callers, but never let it
    // fragment the cache or control thumbnail selection.
    val diskFallbackSecond = 0
    val diskKey = VideoThumbnailDiskCache.key(
        videoId = videoId,
        uri = uri,
        path = path,
        strategy = strategy,
        quality = quality,
        fallbackSecond = diskFallbackSecond
    )
    val persistentLocalThumbnail = !isNetworkUri
    val cacheKey = "video_thumbnail_$diskKey"
    ThumbnailCache.memoryCache.get(cacheKey)?.let { return@withContext it }
    VideoThumbnailDiskCache.read(context, diskKey, persistentLocalThumbnail)?.let { cached ->
        ThumbnailCache.memoryCache.put(cacheKey, cached)
        return@withContext cached
    }
    // Migrate thumbnails produced by older versions from cacheDir to filesDir. Once migrated,
    // ordinary Android cache clearing no longer removes thumbnails for local/downloaded media.
    if (persistentLocalThumbnail) {
        VideoThumbnailDiskCache.read(context, diskKey, persistent = false)?.let { legacy ->
            VideoThumbnailDiskCache.write(context, diskKey, legacy, persistent = true)
            ThumbnailCache.memoryCache.put(cacheKey, legacy)
            return@withContext legacy
        }
    }

    return@withContext withContext(if (background) prefetchThumbnailDispatcher else thumbnailDispatcher) {
    fun cache(bitmap: Bitmap): Bitmap {
        ThumbnailCache.memoryCache.put(cacheKey, bitmap)
        // Keep aliases used by list/grid composables so a recomposed item can reuse the
        // already-decoded bitmap without starting another extraction.
        val aliasSecond = fallbackSecond.coerceIn(1, 10)
        val aliasSuffix = "${strategy.name}_${quality.name}_t${aliasSecond}"
        if (videoId > 0 && path.isNotBlank()) {
            ThumbnailCache.memoryCache.put("${videoId}_${path}_$aliasSuffix", bitmap)
        }
        if (path.isNotBlank()) {
            ThumbnailCache.memoryCache.put("${path}_$aliasSuffix", bitmap)
        }
        // Persist only successfully extracted/generated images. A placeholder/gradient is never cached.
        VideoThumbnailDiskCache.write(context, diskKey, bitmap, persistentLocalThumbnail)
        return bitmap
    }

    fun openRetriever(): MediaMetadataRetriever? {
        return try {
            MediaMetadataRetriever().also { retriever ->
                if (path.isNotBlank() && com.example.util.archive.ArchiveStore.isArchivePath(path)) {
                    // Read straight out of the archive (file or link) through a seekable data source: no extraction and no
                    // loopback server, only the few blocks Android asks for.
                    val archiveData = com.example.util.archive.ArchiveProbe.dataSourceFor(context, path)
                    if (archiveData != null) {
                        retriever.setDataSource(archiveData)
                    } else {
                        retriever.release()
                        return null
                    }
                } else if (path.isNotBlank() && File(path).exists()) {
                    retriever.setDataSource(path)
                } else if (uri.toString().isNotBlank()) {
                    retriever.setDataSource(context, uri)
                } else {
                    retriever.release()
                    return null
                }
            }
        } catch (_: Throwable) { null }
    }

    fun scaleToFit(source: Bitmap): Bitmap {
        val maxW = targetWidth
        val maxH = targetHeight
        if (source.width <= maxW && source.height <= maxH) return source
        val scale = minOf(maxW.toFloat() / source.width, maxH.toFloat() / source.height)
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true).also {
            if (it !== source && !source.isRecycled) source.recycle()
        }
    }

    fun tryEmbedded(retriever: MediaMetadataRetriever): Bitmap? {
        return try {
            val bytes = retriever.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let(::scaleToFit)
        } catch (_: Throwable) { null }
    }

    // Embedded mode means authored artwork only. Android's retriever does not expose every MKV
    // attachment, so the native MKV parser is the second, container-accurate attachment source.
    if (strategy == ThumbnailStrategy.EMBEDDED_AND_GENERATED) {
        openRetriever()?.let { retriever ->
            try {
                tryEmbedded(retriever)?.let { return@withContext cache(it) }
            } finally {
                try { retriever.release() } catch (_: Throwable) {}
            }
        }

        if (!isNetworkUri && (path.endsWith(".mkv", true) || uri.toString().endsWith(".mkv", true))) {
            try {
                val sourceUri = if (path.isNotBlank() && File(path).exists()) Uri.fromFile(File(path)) else uri
                val attachments = MkvManagerEngine.parseMkvFile(context, sourceUri).third.second
                val imageAttachment = attachments
                    .filter { it.isImage && it.data.isNotEmpty() }
                    .sortedByDescending {
                        val name = (it.fileName + " " + it.description).lowercase()
                        when {
                            "cover" in name || "poster" in name || "front" in name || "folder" in name -> 3
                            else -> 1
                        }
                    }
                    .firstOrNull()
                if (imageAttachment != null) {
                    BitmapFactory.decodeByteArray(imageAttachment.data, 0, imageAttachment.data.size)?.let {
                        return@withContext cache(scaleToFit(it))
                    }
                }
            } catch (_: Throwable) {}
        }

        // Embedded + Generated is attachment-first: if no image attachment exists, the
        // generated-frame path below is the only fallback. No unrelated poster source is used.
    }

    if (!isActive) return@withContext null

    // Smart mode deliberately samples several points through the media instead of always using
    // the first second. This avoids black/logo/title-card intros and works for very long files.
    // Generated mode remains deterministic and uses exactly the user-selected second (clamped to
    // the media duration).
    openRetriever()?.let { retriever ->
        try {
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val durationUs = (durationMs * 1000L).coerceAtLeast(1L)
            if (durationMs > 0L && com.example.util.archive.ArchiveStore.isArchivePath(path)) {
                com.example.util.archive.ArchiveStore.parsePath(path)?.let { (id, idx) ->
                    com.example.util.archive.ArchiveStore.updateDuration(context, id, idx, durationMs)
                }
            }

            fun getFrame(timeUs: Long): Bitmap? {
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(
                            timeUs.coerceIn(0L, durationUs),
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetWidth,
                            targetHeight
                        )
                    } else {
                        retriever.getFrameAtTime(
                            timeUs.coerceIn(0L, durationUs),
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )?.let(::scaleToFit)
                    }
                } catch (_: Throwable) { null }
            }

            fun frameScore(bitmap: Bitmap): Double {
                if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return -1.0
                val stepX = maxOf(1, bitmap.width / 24)
                val stepY = maxOf(1, bitmap.height / 14)
                var samples = 0
                var sum = 0.0
                var sumSq = 0.0
                var dark = 0
                val pixels = IntArray(1)
                var y = 0
                while (y < bitmap.height) {
                    var x = 0
                    while (x < bitmap.width) {
                        bitmap.getPixels(pixels, 0, 1, x, y, 1, 1)
                        val c = pixels[0]
                        val r = (c shr 16) and 0xFF
                        val g = (c shr 8) and 0xFF
                        val b = c and 0xFF
                        val lum = (0.2126 * r + 0.7152 * g + 0.0722 * b)
                        sum += lum
                        sumSq += lum * lum
                        if (lum < 10.0) dark++
                        samples++
                        x += stepX
                    }
                    y += stepY
                }
                if (samples == 0) return -1.0
                val mean = sum / samples
                val variance = (sumSq / samples) - (mean * mean)
                val darkRatio = dark.toDouble() / samples
                // Prefer real visual content: moderate brightness + local variation; heavily
                // black/uniform frames are penalized so a title-card/blank frame is less likely.
                return variance.coerceAtLeast(0.0) + mean * 0.45 - darkRatio * 220.0
            }

            val isArchive = com.example.util.archive.ArchiveStore.isArchivePath(path)
            if (isArchive) {
                // Archive video: a stored video can be read at any position, so take a frame at ~10% (not the black intro), but not
                // further than 90 s so little is read from a link. A compressed one can only be unpacked from its start: near the start.
                val seekUs = if (com.example.util.archive.ArchiveProbe.isRandomAccess(context, path)) {
                    minOf((durationUs * 0.10).toLong(), 90_000_000L)
                } else if (durationUs > 15_000_000L) 3_000_000L else (durationUs * 0.10).toLong()
                val frame = getFrame(seekUs)
                    ?: getFrame(0L)
                    ?: retriever.frameAtTime
                if (frame != null) return@withContext cache(scaleToFit(frame))
            } else if (strategy == ThumbnailStrategy.EMBEDDED_AND_GENERATED) {
                // No attached artwork was found. Pick a representative frame from the actual
                // video timeline instead of using a fixed 1–10 second fallback.
                val sourceSeed = (path.ifBlank { uri.toString() }).hashCode().toLong()
                val fraction = (((sourceSeed and Long.MAX_VALUE) % 8000L) / 10000.0 + 0.10).coerceIn(0.10, 0.90)
                val frame = getFrame((durationUs * fraction).toLong())
                    ?: getFrame((durationUs * 0.5).toLong())
                    ?: getFrame(0L)
                    ?: retriever.frameAtTime
                if (frame != null) return@withContext cache(scaleToFit(frame))
            } else if (strategy == ThumbnailStrategy.SMART) {
                val fractions = doubleArrayOf(0.30, 0.12, 0.55, 0.75)
                var best: Bitmap? = null
                var bestScore = Double.NEGATIVE_INFINITY
                for (fraction in fractions) {
                    if (!isActive) return@withContext null
                    val frame = getFrame((durationUs * fraction).toLong()) ?: continue
                    val score = frameScore(frame)
                    if (score > bestScore) {
                        best?.let { if (!it.isRecycled) it.recycle() }
                        best = frame
                        bestScore = score
                    } else if (!frame.isRecycled) {
                        frame.recycle()
                    }
                    // A clearly good frame (real content, not black/flat) is accepted straight away: no more seeks.
                    if (bestScore >= 450.0) break
                }
                if (best != null) return@withContext cache(scaleToFit(best))
            }
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    null
    }
}
