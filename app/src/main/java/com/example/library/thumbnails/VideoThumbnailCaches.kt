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
 * Bounded in-memory cache for list thumbnails.
 * The disk cache below is the source of truth across app restarts; this cache only avoids
 * decoding the same thumbnail repeatedly during the current process.
 */
object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Keep substantially more decoded thumbnails resident so fast LazyColumn/LazyGrid
    // scrolling does not immediately evict frames that are still likely to be revisited.
    // The hard cap keeps this bounded on devices with large heaps.
    private val cacheSize = (maxMemory / 8).coerceIn(32 * 1024, 96 * 1024)

    val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int =
            (bitmap.byteCount / 1024).coerceAtLeast(1)
    }

    fun getCachedThumbnail(
        videoId: Long,
        path: String,
        strategy: ThumbnailStrategy,
        quality: ThumbnailQuality,
        fallbackSecond: Int
    ): Bitmap? {
        val suffix = "${strategy.name}_${quality.name}_t${fallbackSecond.coerceIn(1, 10)}"
        if (videoId > 0 && path.isNotBlank()) {
            memoryCache.get("${videoId}_${path}_$suffix")?.let { if (!it.isRecycled) return it }
        }
        if (path.isNotBlank()) {
            memoryCache.get("${path}_$suffix")?.let { if (!it.isRecycled) return it }
        }
        return null
    }
}

/**
 * Local media thumbnails are persisted under filesDir/thumbnails/video and survive normal cache
 * clearing and app restarts. Remote/provider thumbnails remain in cacheDir so they can be evicted.
 * The cache key includes the source signature and thumbnail settings to avoid stale reuse.
 */
internal object VideoThumbnailDiskCache {
    private const val CACHE_VERSION = "v4"
    private const val MAX_CACHE_BYTES = 256L * 1024L * 1024L

    private fun directory(context: Context, persistent: Boolean): File =
        if (persistent) File(context.filesDir, "thumbnails/video")
            .apply { mkdirs() }
        else File(context.cacheDir, "video_thumbnail_cache").apply { mkdirs() }

    fun key(
        videoId: Long,
        uri: Uri,
        path: String,
        strategy: ThumbnailStrategy,
        quality: ThumbnailQuality,
        fallbackSecond: Int
    ): String {
        val source = if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists()) {
                "file:${file.absolutePath}|${file.length()}|${file.lastModified()}"
            } else {
                "path:$path"
            }
        } else {
            "uri:${uri}"
        }
        val raw = "$CACHE_VERSION|$source|${strategy.name}|${quality.name}|$fallbackSecond"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "${strategy.name}_${quality.name}_$fallbackSecond-$digest"
    }

    // Do not purge other strategy/position caches when a thumbnail is requested.
    // Lazy lists can request several settings during recomposition and deleting the other
    // entries here was the reason cached thumbnails disappeared while scrolling.

    fun read(context: Context, key: String, persistent: Boolean): Bitmap? {
        val file = File(directory(context, persistent), "$key.jpg")
        if (!file.isFile || file.length() <= 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)?.takeUnless { it.isRecycled }
        } catch (_: Throwable) {
            try { file.delete() } catch (_: Throwable) {}
            null
        }
    }

    fun write(context: Context, key: String, bitmap: Bitmap, persistent: Boolean) {
        val dir = directory(context, persistent)
        val target = File(dir, "$key.jpg")
        if (target.isFile && target.length() > 0L) return
        val temp = File(dir, "$key.tmp")
        try {
            temp.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) return
            }
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
        } catch (_: Throwable) {
            try { temp.delete() } catch (_: Throwable) {}
            return
        }
        // Persistent local-media thumbnails are intentionally not subject to cache eviction.
        if (!persistent) trim(dir)
    }

    private fun trim(dir: File) {
        try {
            val files = dir.listFiles { f -> f.isFile && f.extension.equals("jpg", true) }.orEmpty()
            var total = files.sumOf { it.length() }
            if (total <= MAX_CACHE_BYTES) return
            for (file in files.sortedBy { it.lastModified() }) {
                if (total <= MAX_CACHE_BYTES) break
                total -= file.length()
                file.delete()
            }
        } catch (_: Throwable) {}
    }
}
