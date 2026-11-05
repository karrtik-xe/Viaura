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
 * Video Thumbnail composable matching the exact requested UI:
 * - 16:9 rounded thumbnail container
 * - Red "NEW" badge on top-left
 * - Semi-transparent duration badge on bottom-right (e.g. "23:59")
 */
@Composable
fun VideoThumbnailCard(
    videoId: Long,
    videoUri: Uri,
    videoPath: String,
    durationText: String,
    isNew: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    customGradientSeed: Int = 0,
    showDuration: Boolean = true,
    showProgressBar: Boolean = true,
    watchedProgress: Float = 0f,
    isSelected: Boolean = false,
    isLastPlayed: Boolean = false,
    thumbnailStrategy: ThumbnailStrategy = ThumbnailStrategy.SMART,
    thumbnailQuality: ThumbnailQuality = ThumbnailQuality.HIGH,
    thumbnailFallbackSecond: Int = 1,
    showNetworkThumbnails: Boolean = false,
    remoteThumbnailUrl: String? = null,
    onThumbnailClick: (() -> Unit)? = null,
    /** Use a higher-resolution provider thumbnail and crop it to fill the landscape card without blurred side fill. */
    showWholeImage: Boolean = false
) {
    val context = LocalContext.current.applicationContext
    val remoteTargetPx = if (showWholeImage) 640 else 400
    val cacheKey = remember(videoId, videoPath, remoteThumbnailUrl, thumbnailStrategy, thumbnailQuality, thumbnailFallbackSecond, remoteTargetPx) {
        if (!remoteThumbnailUrl.isNullOrBlank()) "remote_audio_${remoteThumbnailUrl}_$remoteTargetPx"
        else "${videoId}_${videoPath}_${thumbnailStrategy.name}_${thumbnailQuality.name}_t${thumbnailFallbackSecond.coerceIn(1, 10)}"
    }
    val initialCached = remember(cacheKey, videoId, videoPath) {
        ThumbnailCache.memoryCache.get(cacheKey)
            ?: ThumbnailCache.getCachedThumbnail(
                videoId, videoPath, thumbnailStrategy, thumbnailQuality, thumbnailFallbackSecond
            )
    }
    var thumbnailBitmap by remember(cacheKey) { mutableStateOf(initialCached) }

    LaunchedEffect(cacheKey) {
        if (thumbnailBitmap == null) {
            if (!remoteThumbnailUrl.isNullOrBlank() && remoteThumbnailUrl.startsWith("/")) {
                // saved frame of a stream (local file)
                val bmp = withContext(Dispatchers.IO) {
                    try { android.graphics.BitmapFactory.decodeFile(remoteThumbnailUrl) } catch (_: Throwable) { null }
                }
                if (bmp != null) {
                    ThumbnailCache.memoryCache.put(cacheKey, bmp)
                    thumbnailBitmap = bmp
                }
            } else if (!remoteThumbnailUrl.isNullOrBlank()) {
                val bmp = loadRemoteAudioThumbnail(remoteThumbnailUrl, targetSizePx = remoteTargetPx, context = context)
                if (bmp != null) {
                    thumbnailBitmap = bmp
                }
            } else if (com.example.util.archive.ArchiveStore.isArchivePath(videoPath)) {
                // A video inside a ZIP / TAR: no picture is extracted (no playing / decoding, no internet). Only a picture that
                // already exists in the cache is shown; otherwise the normal placeholder stays.
                val cached = withContext(Dispatchers.IO) {
                    peekCachedVideoThumbnail(context, videoId, videoUri, videoPath, thumbnailStrategy, thumbnailQuality)
                }
                if (cached != null) thumbnailBitmap = cached
            } else {
                val bmp = loadVideoThumbnail(
                    context, videoId, videoUri, videoPath,
                    strategy = thumbnailStrategy,
                    quality = thumbnailQuality,
                    allowNetwork = showNetworkThumbnails,
                    fallbackSecond = thumbnailFallbackSecond
                )
                if (bmp != null) {
                    thumbnailBitmap = bmp
                }
            }
        }
    }

    val shape = RoundedCornerShape(14.dp)

    val accentColors = com.example.ui.theme.AccentGradientColors
    val lastPlayedGradient = remember(accentColors) {
        Brush.linearGradient(accentColors + accentColors.first())
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (onThumbnailClick != null) Modifier.clickable(onClick = onThumbnailClick) else Modifier)
            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, com.example.ui.theme.AccentGradient, shape)
                } else if (isLastPlayed) {
                    Modifier.border(2.dp, lastPlayedGradient, shape)
                } else {
                    Modifier.border(
                        1.dp,
                        if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFCBD5E1).copy(alpha = 0.7f),
                        shape
                    )
                }
            )
    ) {
        // Thumbnail content (Image or artistic placeholder)
        if (thumbnailBitmap != null) {
            val bmp = thumbnailBitmap!!
            // TV channel logos (square / wide / tall) are shown zoomed-out and fully visible instead of being
            // cropped; real 16:9 frames and YouTube thumbnails still fill the card.
            val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1).toFloat()
            val isLogoLike = !remoteThumbnailUrl.isNullOrBlank() && (aspect < 1.5f || aspect > 2.05f)
            if (showWholeImage && !remoteThumbnailUrl.isNullOrBlank()) {
                // Provider episode art fills the standard landscape card. Do not blur/stretch a second copy at the sides.
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = if (isLogoLike) ContentScale.Fit else ContentScale.Crop,
                modifier = if (isLogoLike) Modifier.fillMaxSize().padding(6.dp) else Modifier.fillMaxSize()
            )
        } else {
            // Never show an artistic gradient in place of a real video thumbnail.
            // The neutral surface remains stable until the cached/generated frame arrives.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 1. Top-Left: Red "NEW" badge with white bold text (matching user screenshot)
        if (isNew) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 6.dp, y = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE11D48)) // Vivid red matching screenshot
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "NEW",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                )
            }
        }

        // Watched progress on the thumbnail (only with Remove Card UI ON; the card fill shows it otherwise)
        ThumbnailPlayedProgress(watchedProgress, showProgressBar)

        // 2. Bottom-Right: Duration badge (e.g. "23:59") with dark translucent background
        if (showDuration && durationText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-6).dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

/**
 * Lightweight Thumbnail Loader helper for simple path-based video thumbnails.
 */
@Composable
fun VideoThumbnailLoader(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext
    val cacheKey = remember(videoPath) { "path_$videoPath" }
    var bitmap by remember(cacheKey) { mutableStateOf(ThumbnailCache.memoryCache.get(cacheKey)) }

    LaunchedEffect(cacheKey) {
        if (bitmap == null && videoPath.isNotBlank()) {
            val bmp = loadVideoThumbnail(
                context = context,
                videoId = 0L,
                uri = Uri.EMPTY,
                path = videoPath
            )
            if (bmp != null) {
                bitmap = bmp
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
