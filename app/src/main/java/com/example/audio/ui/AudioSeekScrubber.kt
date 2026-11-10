package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentSkyBlue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.util.AppHaptics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Modern Interactive Audio Scrubber inspired by Android 13+ / YouTube Music Media Slider:
 * - Idle state: Sleek, minimalistic pill-shaped progress bar (6dp height) with left & right timestamps.
 * - Touch & Hold / Drag state: Smoothly expands in height (up to 16dp) with an animated spring effect,
 *   allowing real-time backward and forward scrubbing.
 * - Smoothly shrinks back down upon release and commits the seek position.
 */
@Composable
fun InteractiveAudioScrubber(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = if (isDark) Neutral.D334155 else Neutral.E2E8F0,
    textColor: Color = if (isDark) Neutral.S94A3B8 else Neutral.S64748B,
    loopPointA: Long? = null,
    loopPointB: Long? = null,
    isABLoopActive: Boolean = false,
    isPlaying: Boolean = true
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    var lastHapticSec by remember { mutableStateOf(-1L) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val liquidControls = LocalLiquidGlassControls.current
    val glassStyle = LocalSeekbarStyle.current
    val glassPress by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "audio_glass_press"
    )

    val safeDuration = durationMs.coerceAtLeast(1L)
    val actualFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    // After the finger is lifted the thumb stays on the spot that was chosen until the player has really arrived there
    // (otherwise it shows the old position for a moment, jumps back and then forward again).
    var holdMs by remember { mutableStateOf(-1L) }
    if (holdMs >= 0L && !isDragging) {
        LaunchedEffect(holdMs, currentPositionMs) {
            if (kotlin.math.abs(currentPositionMs - holdMs) <= 1500L) holdMs = -1L
            else {
                kotlinx.coroutines.delay(2500L)
                holdMs = -1L
            }
        }
    }
    val displayFraction = when {
        isDragging -> dragFraction
        holdMs >= 0L -> (holdMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        else -> actualFraction
    }

    val displayedPositionMs = (displayFraction * safeDuration).toLong().coerceIn(0L, safeDuration)
    // Animated track height on touch/hold/drag - Thick pill slider matching screenshot
    val animatedTrackHeight by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scrubber_track_height"
    )

    // Animated glow / alpha when expanded
    val animatedActiveAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.92f,
        animationSpec = tween(durationMillis = 200),
        label = "scrubber_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Scrubber Interactive Bar with Touch-and-Hold Expansion
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp) // Comfortable touch area
                .testTag("audio_player_progress_slider")
                .pointerInput(safeDuration) {
                    // Press-to-seek and drag-to-scrub are combined into a single
                    // pointerInput block (as sibling coroutines sharing the same
                    // pointer stream) instead of two separate stacked
                    // pointerInput modifiers. Two independent modifiers here
                    // used to race for the initial touch down, which is why a
                    // tap or the start of a drag on the seek bar sometimes
                    // needed to be repeated before it registered.
                    kotlinx.coroutines.coroutineScope {
                        launch {
                            detectTapGestures(
                                onPress = { offset ->
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    val targetFrac = (offset.x / width).coerceIn(0f, 1f)
                                    isDragging = true
                                    dragFraction = targetFrac
                                    AppHaptics.performGestureThreshold(view, haptic)

                                    val release = tryAwaitRelease()
                                    if (release) {
                                        val finalMs = (dragFraction * safeDuration).toLong()
                                        holdMs = finalMs
                                        onSeekTo(finalMs)
                                    }
                                    isDragging = false
                                }
                            )
                        }
                        launch {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    isDragging = true
                                    dragFraction = (offset.x / width).coerceIn(0f, 1f)
                                    lastHapticSec = (dragFraction * safeDuration).toLong() / 1000L
                                    AppHaptics.performGestureStart(view, haptic)
                                },
                                onDragEnd = {
                                    val finalMs = (dragFraction * safeDuration).toLong()
                                    holdMs = finalMs
                                    onSeekTo(finalMs)
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    dragFraction = (change.position.x / width).coerceIn(0f, 1f)
                                    val currentSec = (dragFraction * safeDuration).toLong() / 1000L
                                    if (kotlin.math.abs(currentSec - lastHapticSec) >= 1L) {
                                        lastHapticSec = currentSec
                                        AppHaptics.performTick(view, haptic)
                                    }
                                }
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                if (liquidControls) {
                    // Liquid Glass sliders: glass groove + gradient front part + glass thumb (follows the Seekbar Style)
                    if (isABLoopActive && loopPointA != null && loopPointB != null) {
                        val loopS = (loopPointA.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                        val loopE = (loopPointB.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                        val loopH = liquidSeekTrackHeightDp(glassStyle).dp.toPx() + 6.dp.toPx()
                        drawRoundRect(
                            color = AccentSkyBlue.copy(alpha = 0.35f),
                            topLeft = Offset(loopS * size.width, size.height / 2f - loopH / 2f),
                            size = Size(((loopE - loopS) * size.width).coerceAtLeast(2f), loopH),
                            cornerRadius = CornerRadius(loopH / 2f, loopH / 2f)
                        )
                    }
                    drawLiquidGlassSeekbar(glassStyle, displayFraction, isDark, glassPress)
                    return@Canvas
                }
                val canvasWidth = size.width
                val barHeight = animatedTrackHeight.toPx()
                val barTop = size.height - barHeight - 6.dp.toPx()
                val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

                // 1. Inactive / background track
                drawRoundRect(
                    color = inactiveTrackColor,
                    topLeft = Offset(0f, barTop),
                    size = Size(canvasWidth, barHeight),
                    cornerRadius = cornerRadius
                )

                // 2. A-B loop segment
                if (isABLoopActive && loopPointA != null && loopPointB != null) {
                    val loopStartFrac = (loopPointA.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    val loopEndFrac = (loopPointB.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    drawRoundRect(
                        color = AccentSkyBlue.copy(alpha = 0.20f),
                        topLeft = Offset(loopStartFrac * canvasWidth, barTop),
                        size = Size(((loopEndFrac - loopStartFrac) * canvasWidth).coerceAtLeast(2f), barHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // 3. Progress fill (rounded at both ends so it sits flush inside the track)
                val progressWidth = (canvasWidth * displayFraction).coerceIn(0f, canvasWidth)
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = activeTrackColor.copy(alpha = animatedActiveAlpha),
                        topLeft = Offset(0f, barTop),
                        size = Size(progressWidth.coerceAtLeast(barHeight), barHeight),
                        cornerRadius = cornerRadius
                    )
                }

                // 4. Drag handle while scrubbing
                if (isDragging) {
                    val knobX = progressWidth.coerceIn(barHeight / 2f, canvasWidth - barHeight / 2f)
                    drawCircle(
                        color = Color.White,
                        radius = barHeight * 0.42f,
                        center = Offset(knobX, barTop + barHeight / 2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Time Labels on Left and Right (e.g. 00:50 and 01:53)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatAudioScrubberTime(displayedPositionMs),
                fontSize = 12.5.sp,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium,
                color = if (isDragging) activeTrackColor else textColor
            )

            if (isABLoopActive && loopPointA != null && loopPointB != null) {
                Text(
                    text = "A-B Loop (${formatAudioScrubberTime(loopPointA)} - ${formatAudioScrubberTime(loopPointB)})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentSkyBlue
                )
            }

            Text(
                text = formatAudioScrubberTime(durationMs),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

private fun formatAudioScrubberTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
    } else {
        String.format(Locale.US, "%02d:%02d", remainingMinutes, remainingSeconds)
    }
}
