package com.example.ui.components

import android.graphics.Outline
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.player.VideoMiniPlayerManager
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.Neutral
import kotlin.math.roundToInt

/** Height of the picture area: identical to the audio mini player's artwork, so the pill is exactly as tall as before. */
private val MINI_VIDEO_HEIGHT = 44.dp

/**
 * Floating mini player of a VIDEO (Settings > Player > General > "Video mini player").
 * Same pill, same enter/exit animation, same buttons and hold-to-drag as [MinimizedAudioPlayer]; the artwork slot shows
 * the LIVE video instead of a thumbnail. The slot is as tall as the audio artwork and exactly as wide as the video's
 * aspect ratio needs (portrait = narrower, wide = wider), so the picture is never stretched and the pill never gets thicker.
 *
 * This is the ONLY video mini player style (also when "Pinned video layout" is ON). Besides a tap, the full player is
 * opened by touching and holding the pill and then sliding to the right.
 */
@Composable
fun MinimizedVideoPlayer(
    isDark: Boolean,
    glassBlurTransparency: Float = 85f,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by VideoMiniPlayerManager.session.collectAsState()
    val s = session

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    // Hold + slide right opens the full player; the ring around the picture shows how far it is.
    var expandProgress by remember { mutableFloatStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    val animatedExpandProgress by animateFloatAsState(
        targetValue = expandProgress,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "mini_video_expand_progress"
    )
    val animatedDragOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "mini_video_drag"
    )

    // Keep the last session while the exit animation plays (the manager already cleared it by then).
    var shown by remember { mutableStateOf(s) }
    if (s != null) shown = s
    val live = shown

    AnimatedVisibility(
        visible = s != null,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(durationMillis = 340, delayMillis = 80, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.94f,
            animationSpec = tween(durationMillis = 320, delayMillis = 60, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(durationMillis = 280, delayMillis = 80, easing = LinearEasing)),
        exit = slideOutVertically(
            targetOffsetY = { it / 4 },
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        if (live == null) return@AnimatedVisibility
        val controller = live.controller

        val isPlaying by controller.isPlaying.collectAsState()
        val positionMs by controller.currentPositionMs.collectAsState()
        val durationMs by controller.durationMs.collectAsState()
        val videoW by controller.videoWidth.collectAsState()
        val videoH by controller.videoHeight.collectAsState()

        // The same native view that was playing in the full player is shown here; polling restarts on this host.
        val hostScope = rememberCoroutineScope()
        DisposableEffect(live.view) {
            controller.resumePolling(hostScope)
            onDispose { }
        }

        // Aspect ratio of the real picture (falls back to the resolution text, then 16:9).
        val aspect = remember(videoW, videoH, live.video.resolution) {
            val fromResolution = Regex("(\\d+)\\s*[x\u00D7]\\s*(\\d+)").find(live.video.resolution)?.let {
                val w = it.groupValues[1].toFloatOrNull()
                val h = it.groupValues[2].toFloatOrNull()
                if (w != null && h != null && h > 0f) w / h else null
            }
            when {
                videoW > 0 && videoH > 0 -> videoW.toFloat() / videoH.toFloat()
                fromResolution != null -> fromResolution
                else -> 16f / 9f
            }.coerceIn(0.4f, 2.6f)
        }
        // Wide videos get a wider slot, portrait ones a narrower slot; height is always the audio artwork height.
        val slotWidth = (MINI_VIDEO_HEIGHT * aspect).coerceIn(30.dp, 104.dp)

        val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
        val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
        val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

        val effectiveDuration = durationMs.coerceAtLeast(live.video.durationMs).coerceAtLeast(1L)
        val progressRatio = (positionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)

        val alphaFactor = (glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f)
        val containerBg = if (isDark) {
            if (alphaFactor >= 0.99f) Neutral.D0F172A
            else Neutral.D0F172A.copy(alpha = (0.75f + 0.25f * alphaFactor).coerceIn(0.60f, 0.98f))
        } else {
            val lightBase = if (ViauraSurfaceStyle.systemMode) Neutral.FFFFFF else Neutral.DAE3E9
            if (alphaFactor >= 0.99f) lightBase
            else lightBase.copy(alpha = (0.80f + 0.20f * alphaFactor).coerceIn(0.65f, 0.98f))
        }
        val progressFillColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else Color(0xFFBAE6FD).copy(alpha = 0.50f)

        Box(
            modifier = Modifier
                .offset { IntOffset(0, animatedDragOffsetY.roundToInt()) }
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .liquidGlassOr(RoundedCornerShape(20.dp), isDark, glassBlurTransparency, elevation = if (isDragging) 22.dp else 14.dp) {
                    this
                        .shadow(
                            elevation = if (isDragging) 22.dp else 14.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = if (isDark) AccentSkyBlue.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.12f),
                            spotColor = if (isDark) AccentSkyBlue.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.18f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(containerBg)
                        .border(
                            width = if (isDragging) 1.5.dp else 1.dp,
                            color = if (isDragging) AccentSkyBlue else borderColor,
                            shape = RoundedCornerShape(20.dp)
                        )
                }
                .then(
                    if (LocalLiquidGlass.current && isDragging) Modifier.border(1.5.dp, AccentSkyBlue, RoundedCornerShape(20.dp)) else Modifier
                )
                .pointerInput(Unit) {
                    // Touch & hold, then slide to the RIGHT: the full (YouTube-style) player opens.
                    // Sliding up / down while holding still just moves the pill, as before.
                    val sidePx = 64.dp.toPx()
                    var holdDx = 0f
                    var holdDy = 0f
                    var fired = false
                    detectDragGesturesAfterLongPress(
                        onDragStart = { isDragging = true; holdDx = 0f; holdDy = 0f; fired = false },
                        onDragEnd = { isDragging = false; expandProgress = 0f },
                        onDragCancel = { isDragging = false; expandProgress = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            holdDx += dragAmount.x
                            holdDy += dragAmount.y
                            val rightward = holdDx > 0f && holdDx > kotlin.math.abs(holdDy)
                            expandProgress = if (rightward) (holdDx / sidePx).coerceIn(0f, 1f) else 0f
                            if (!fired && rightward && holdDx >= sidePx) {
                                fired = true
                                dragOffsetY = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExpand()
                            } else if (!fired) {
                                dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-700f, 200f)
                            }
                        }
                    )
                }
                .clickable(onClick = onExpand)
        ) {
            // PROGRESS EFFECT BACKGROUND FILL (same as audio)
            Box(modifier = Modifier.matchParentSize().align(Alignment.CenterStart)) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progressRatio)
                        .background(
                            Brush.horizontalGradient(
                                listOf(progressFillColor, progressFillColor.copy(alpha = progressFillColor.alpha * 1.25f))
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIVE VIDEO in the artwork slot
                Box(
                    modifier = Modifier
                        .testTag("mini_video_surface")
                        .height(MINI_VIDEO_HEIGHT)
                        .width(slotWidth)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(2.dp, AccentSkyBlue.copy(alpha = animatedExpandProgress), RoundedCornerShape(12.dp))
                ) {
                    if (!com.example.util.PipModeState.isInPictureInPicture.value) {
                    AndroidView(
                        factory = {
                            val v = live.view
                            // The surface is re-created in the mini player (e.g. back from PiP): hold playback for it so the sound never cuts.
                            live.controller.markSeamlessHandOver(noDuck = true)
                            (v.parent as? ViewGroup)?.removeView(v)
                            // SurfaceView ignores Compose clipping: round it through the view's own outline.
                            v.outlineProvider = object : ViewOutlineProvider() {
                                override fun getOutline(view: View, outline: Outline) {
                                    outline.setRoundRect(0, 0, view.width, view.height, 12f * view.resources.displayMetrics.density)
                                }
                            }
                            v.clipToOutline = true
                            v
                        },
                        modifier = Modifier.fillMaxHeight().fillMaxWidth()
                    )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = live.video.displayName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${if (isPlaying) "Playing" else "Paused"} • Video",
                        fontSize = 11.sp,
                        color = if (isPlaying) AccentSkyBlue else secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .testTag("mini_video_play_pause_button")
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentGradient)
                            .then(
                                if (LocalLiquidGlass.current) Modifier.border(1.dp, liquidGlassBorderBrush(true), CircleShape) else Modifier
                            )
                            .clickable { VideoMiniPlayerManager.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (isPlaying) R.drawable.viaura_pause_solid else R.drawable.viaura_play_solid),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val hasNext = live.playlist.indexOfFirst { it.path == live.video.path }.let { it >= 0 && it + 1 < live.playlist.size }
                    if (hasNext) {
                        Box(
                            modifier = Modifier
                                .testTag("mini_video_next_button")
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { VideoMiniPlayerManager.skipNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.viaura_arrow_right_solid),
                                contentDescription = "Next video",
                                tint = primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("mini_video_close_button")
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { VideoMiniPlayerManager.close() },
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Player",
                            tint = secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
