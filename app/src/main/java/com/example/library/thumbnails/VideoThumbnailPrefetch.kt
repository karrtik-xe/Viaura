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
 * Process-wide thumbnail warming. Unlike a LaunchedEffect inside one screen, these jobs keep running
 * while the user moves between folders / opens a video, so every folder's thumbnails are ready by the
 * time it is opened. Starting the same library again (same videos + settings) is a no-op.
 */
object ThumbnailPrefetcher {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)
    private var libraryJob: kotlinx.coroutines.Job? = null
    private var librarySignature: String? = null
    private var priorityJob: kotlinx.coroutines.Job? = null
    private var prioritySignature: String? = null

    @Volatile
    var isPaused: Boolean = false
        private set

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    private fun signature(
        videos: List<com.example.ui.screens.VideoItem>,
        strategy: ThumbnailStrategy,
        quality: ThumbnailQuality,
        allowNetwork: Boolean,
        fallbackSecond: Int
    ): String {
        var acc = videos.size.toLong()
        for (v in videos) acc += v.path.hashCode().toLong() * 31L + v.id
        return "$acc|${strategy.name}|${quality.name}|$allowNetwork|$fallbackSecond"
    }

    @Synchronized
    fun startLibrary(
        context: Context,
        videos: List<com.example.ui.screens.VideoItem>,
        strategy: ThumbnailStrategy,
        quality: ThumbnailQuality,
        allowNetwork: Boolean,
        fallbackSecond: Int
    ) {
        if (videos.isEmpty()) return
        val sig = signature(videos, strategy, quality, allowNetwork, fallbackSecond)
        if (sig == librarySignature && libraryJob?.isActive == true) return
        if (sig == librarySignature && libraryJob?.isCompleted == true) return
        libraryJob?.cancel()
        librarySignature = sig
        val appContext = context.applicationContext
        libraryJob = scope.launch {
            prefetchVideoThumbnails(appContext, videos, strategy, quality, allowNetwork, fallbackSecond)
        }
    }

    /** The folder the user has open is warmed first (and re-warmed if it changes). */
    @Synchronized
    fun startPriority(
        context: Context,
        videos: List<com.example.ui.screens.VideoItem>,
        strategy: ThumbnailStrategy,
        quality: ThumbnailQuality,
        allowNetwork: Boolean,
        fallbackSecond: Int
    ) {
        if (videos.isEmpty()) return
        val sig = signature(videos, strategy, quality, allowNetwork, fallbackSecond)
        if (sig == prioritySignature && priorityJob?.isActive == true) return
        priorityJob?.cancel()
        prioritySignature = sig
        val appContext = context.applicationContext
        priorityJob = scope.launch {
            // The open folder is what the person is looking at: it uses the main (larger) pool, not the background one.
            prefetchVideoThumbnails(appContext, videos, strategy, quality, allowNetwork, fallbackSecond, background = false)
        }
    }
}

/**
 * Warms the thumbnail cache for the complete visible library in the background. Three workers
 * keep extraction moving without creating one coroutine per video, while loadVideoThumbnail's
 * own bounded dispatcher remains the final CPU/IO guard.
 */
suspend fun prefetchVideoThumbnails(
    context: Context,
    videos: List<com.example.ui.screens.VideoItem>,
    strategy: ThumbnailStrategy,
    quality: ThumbnailQuality,
    allowNetwork: Boolean = false,
    fallbackSecond: Int = 1,
    background: Boolean = true
) {
    if (videos.isEmpty()) return
    val uniqueVideos = videos.distinctBy { "${it.id}|${it.path}|${it.uri}" }
    val nextIndex = AtomicInteger(0)
    coroutineScope {
        repeat(if (background) 3 else 4) {
            launch {
                while (isActive) {
                    val index = nextIndex.getAndIncrement()
                    if (index >= uniqueVideos.size) break
                    val video = uniqueVideos[index]
                    // Stay out of the way while a video is opening / playing.
                    while (isActive && ThumbnailPrefetcher.isPaused) kotlinx.coroutines.delay(300L)
                    try {
                        loadVideoThumbnail(
                            context = context,
                            videoId = video.id,
                            uri = video.uri,
                            path = video.path,
                            strategy = strategy,
                            quality = quality,
                            allowNetwork = allowNetwork,
                            fallbackSecond = fallbackSecond,
                            background = background
                        )
                    } catch (_: Throwable) {
                        // One unreadable/corrupt file must not stop the rest of the library.
                    }
                }
            }
        }
    }
}
