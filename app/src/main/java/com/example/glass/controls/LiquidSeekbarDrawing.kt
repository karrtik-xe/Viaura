package com.example.ui.components

import com.example.ui.theme.Neutral

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.state.SeekbarStyle
import com.example.ui.theme.AccentGradientColors
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.ui.platform.LocalView
import com.example.util.AppHaptics
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// ------------------------------------------------------------------------------------------------
// Colours / geometry helpers
// ------------------------------------------------------------------------------------------------

/** The selected app theme gradient (always at least two colours). */
internal fun glassGradientColors(): List<Color> {
    val c = AccentGradientColors
    return if (c.size >= 2) c else listOf(AccentSkyBlue, AccentPink)
}

private fun sampleGradient(colors: List<Color>, t: Float): Color {
    if (colors.size == 1) return colors[0]
    val pos = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = pos.toInt().coerceAtMost(colors.size - 2)
    return lerp(colors[i], colors[i + 1], pos - i)
}

/** Wavy / Thick / Slim have no thumb: the gradient line itself grows with the playback position. */
internal fun seekbarStyleHasThumb(style: SeekbarStyle): Boolean =
    style == SeekbarStyle.NORMAL || style == SeekbarStyle.STANDARD

/** Opacity of the gradient line: slightly see-through on the thumb-less styles so the video shows through. */
internal fun seekbarFillAlpha(style: SeekbarStyle): Float = if (seekbarStyleHasThumb(style)) 1f else 0.72f

/** Track thickness (dp) of every glass seekbar style. */
internal fun liquidSeekTrackHeightDp(style: SeekbarStyle): Float = when (style) {
    SeekbarStyle.NORMAL -> 8f
    SeekbarStyle.STANDARD -> 8f
    SeekbarStyle.WAVY -> 7f
    SeekbarStyle.THICK -> 18f
    SeekbarStyle.SLIM -> 8f
}

/** Glass pill thumb size (width, height) in dp. Normal uses a round bead instead. */
private fun liquidThumbPillDp(style: SeekbarStyle): Pair<Float, Float> = when (style) {
    SeekbarStyle.NORMAL -> 18f to 18f
    SeekbarStyle.STANDARD -> 9f to 24f
    SeekbarStyle.WAVY -> 9f to 24f
    SeekbarStyle.THICK -> 12f to 26f
    SeekbarStyle.SLIM -> 6f to 18f
}

// ------------------------------------------------------------------------------------------------
// Glass thumbs
// ------------------------------------------------------------------------------------------------

/** Glass pill (vertical pill on horizontal sliders, horizontal pill on vertical bars). */
internal fun DrawScope.drawGlassPillThumb(
    cx: Float,
    cy: Float,
    tw: Float,
    th: Float,
    tint: Color,
    onDarkBackdrop: Boolean,
    pressed: Float
) {
    val w = tw * (1f + 0.12f * pressed)
    val h = th * (1f + 0.10f * pressed)
    val left = cx - w / 2f
    val top = cy - h / 2f
    val r = min(w, h) / 2f
    val step = 1.2.dp.toPx()

    // soft shadow under the glass
    for (i in 1..3) {
        drawRoundRect(
            color = Color.Black.copy(alpha = if (onDarkBackdrop) 0.10f else 0.06f),
            topLeft = Offset(left - i * step, top - i * step + 1.5.dp.toPx()),
            size = Size(w + 2 * i * step, h + 2 * i * step),
            cornerRadius = CornerRadius(r + i * step, r + i * step)
        )
    }
    // glass body
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (onDarkBackdrop) 0.46f else 0.92f),
                tint.copy(alpha = if (onDarkBackdrop) 0.24f else 0.40f),
                Color.White.copy(alpha = if (onDarkBackdrop) 0.14f else 0.45f)
            ),
            startY = top,
            endY = top + h
        ),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r)
    )
    if (!onDarkBackdrop) {
        drawRoundRect(
            color = Neutral.S475569.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
    // specular rim
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.70f)),
            start = Offset(left, top),
            end = Offset(left + w, top + h)
        ),
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = 1.2.dp.toPx())
    )
    // small shine
    drawRoundRect(
        color = Color.White.copy(alpha = 0.62f),
        topLeft = Offset(cx - w * 0.20f, top + h * 0.10f),
        size = Size(w * 0.40f, max(h * 0.18f, 1.dp.toPx())),
        cornerRadius = CornerRadius(w * 0.20f, w * 0.20f)
    )
}

/** Round glass bead (the "Normal" slider thumb). */
internal fun DrawScope.drawGlassBeadThumb(
    cx: Float,
    cy: Float,
    radius: Float,
    tint: Color,
    onDarkBackdrop: Boolean,
    pressed: Float
) {
    val r = radius * (1f + 0.20f * pressed)
    val center = Offset(cx, cy)
    val step = 1.2.dp.toPx()
    for (i in 1..3) {
        drawCircle(
            color = Color.Black.copy(alpha = if (onDarkBackdrop) 0.10f else 0.06f),
            radius = r + i * step,
            center = Offset(cx, cy + 1.5.dp.toPx())
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = if (onDarkBackdrop) 0.34f else 0.90f),
                tint.copy(alpha = if (onDarkBackdrop) 0.30f else 0.40f),
                tint.copy(alpha = if (onDarkBackdrop) 0.52f else 0.55f)
            ),
            center = Offset(cx - r * 0.30f, cy - r * 0.35f),
            radius = r * 1.5f
        ),
        radius = r,
        center = center
    )
    if (!onDarkBackdrop) {
        drawCircle(
            color = Neutral.S475569.copy(alpha = 0.35f),
            radius = r,
            center = center,
            style = Stroke(width = 0.8.dp.toPx())
        )
    }
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.70f)),
            start = Offset(cx - r, cy - r),
            end = Offset(cx + r, cy + r)
        ),
        radius = r - 0.6.dp.toPx(),
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
    // inner colour glow at the bottom and shine at the top-left
    drawCircle(color = tint.copy(alpha = 0.35f), radius = r * 0.42f, center = Offset(cx + r * 0.08f, cy + r * 0.34f))
    drawCircle(color = Color.White.copy(alpha = 0.72f), radius = r * 0.20f, center = Offset(cx - r * 0.32f, cy - r * 0.40f))
}

// ------------------------------------------------------------------------------------------------
// Glass seekbar drawing (shared by the video scrubber, the audio scrubber and LiquidGlassSlider)
// ------------------------------------------------------------------------------------------------

/**
 * Glass groove + gradient "front" part of a horizontal seekbar, vertically centred in this DrawScope.
 * Call [drawLiquidGlassSeekbarThumb] afterwards (so chapter / loop markers can sit between the two).
 */
internal fun DrawScope.drawLiquidGlassSeekbarTrack(
    style: SeekbarStyle,
    fraction: Float,
    onDarkBackdrop: Boolean,
    enabled: Boolean = true
) {
    val w = size.width
    val cy = size.height / 2f
    val f = fraction.coerceIn(0f, 1f)
    val trackH = liquidSeekTrackHeightDp(style).dp.toPx()
    val corner = CornerRadius(trackH / 2f, trackH / 2f)
    val activeW = w * f
    val mul = if (enabled) 1f else 0.45f
    val fillMul = mul * seekbarFillAlpha(style)
    val ink = if (onDarkBackdrop) Color.White else Neutral.D0F172A
    val colors = glassGradientColors()
    val brush = Brush.horizontalGradient(colors = colors, startX = 0f, endX = w.coerceAtLeast(1f))
    val grooveTop = cy - trackH / 2f
    // The wavy bar only has a groove in front of the wave
    val grooveLeft = if (style == SeekbarStyle.WAVY) activeW else 0f
    val grooveW = (w - grooveLeft).coerceAtLeast(0f)

    // 1. glass groove
    if (grooveW > 0f) {
        drawRoundRect(
            color = ink.copy(alpha = (if (onDarkBackdrop) 0.16f else 0.10f) * mul),
            topLeft = Offset(grooveLeft, grooveTop),
            size = Size(grooveW, trackH),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = (if (onDarkBackdrop) 0.22f else 0.55f) * mul), Color.Transparent),
                startY = grooveTop,
                endY = grooveTop + trackH
            ),
            topLeft = Offset(grooveLeft, grooveTop),
            size = Size(grooveW, trackH),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f * mul), Color.White.copy(alpha = 0.08f * mul), Color.White.copy(alpha = 0.35f * mul)),
                start = Offset(grooveLeft, grooveTop),
                end = Offset(grooveLeft + grooveW, grooveTop + trackH)
            ),
            topLeft = Offset(grooveLeft, grooveTop),
            size = Size(grooveW, trackH),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // 2. gradient front part
    if (style == SeekbarStyle.WAVY) {
        if (activeW > 0f) {
            val amp = 4.dp.toPx()
            val waveLen = 26.dp.toPx()
            val path = Path()
            path.moveTo(0f, cy)
            var x = 0f
            val stepPx = 2.dp.toPx()
            while (x <= activeW) {
                val y = cy + (sin((x / waveLen) * 2.0 * PI) * amp).toFloat()
                path.lineTo(x, y)
                x += stepPx
            }
            val strokeW = 4.dp.toPx()
            drawPath(
                path = path,
                brush = brush,
                alpha = 0.28f * fillMul,
                style = Stroke(width = strokeW + 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = path,
                brush = brush,
                alpha = fillMul,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.35f * mul),
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    } else if (activeW > 0f) {
        val aw = max(activeW, trackH)
        val glowPad = 3.dp.toPx()
        drawRoundRect(
            brush = brush,
            topLeft = Offset(-2.dp.toPx(), grooveTop - glowPad),
            size = Size(aw + 4.dp.toPx(), trackH + glowPad * 2f),
            cornerRadius = CornerRadius((trackH + glowPad * 2f) / 2f, (trackH + glowPad * 2f) / 2f),
            alpha = 0.26f * fillMul
        )
        drawRoundRect(
            brush = brush,
            topLeft = Offset(0f, grooveTop),
            size = Size(aw, trackH),
            cornerRadius = corner,
            alpha = fillMul
        )
        val glossW = (aw - trackH * 0.7f).coerceAtLeast(0f)
        if (glossW > 0f) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f * mul), Color.White.copy(alpha = 0f)),
                    startY = grooveTop,
                    endY = cy
                ),
                topLeft = Offset(trackH * 0.35f, grooveTop + trackH * 0.12f),
                size = Size(glossW, trackH * 0.32f),
                cornerRadius = CornerRadius(trackH * 0.16f, trackH * 0.16f)
            )
        }
    }
}

/** Glass thumb of a horizontal seekbar (bead for Normal, glass pill for the other styles). */
internal fun DrawScope.drawLiquidGlassSeekbarThumb(
    style: SeekbarStyle,
    fraction: Float,
    onDarkBackdrop: Boolean,
    pressed: Float = 0f
) {
    if (!seekbarStyleHasThumb(style)) return
    val w = size.width
    val cy = size.height / 2f
    val f = fraction.coerceIn(0f, 1f)
    val tint = sampleGradient(glassGradientColors(), f)
    val (twDp, thDp) = liquidThumbPillDp(style)
    val tw = twDp.dp.toPx()
    val th = thDp.dp.toPx()
    val half = max(tw, if (style == SeekbarStyle.NORMAL) th else tw) / 2f
    val cx = (w * f).coerceIn(half, (w - half).coerceAtLeast(half))
    if (style == SeekbarStyle.NORMAL) {
        drawGlassBeadThumb(cx, cy, th / 2f, tint, onDarkBackdrop, pressed)
    } else {
        drawGlassPillThumb(cx, cy, tw, th, tint, onDarkBackdrop, pressed)
    }
}

/** Whole glass seekbar (track + thumb) in one call. */
internal fun DrawScope.drawLiquidGlassSeekbar(
    style: SeekbarStyle,
    fraction: Float,
    onDarkBackdrop: Boolean,
    pressed: Float = 0f,
    enabled: Boolean = true
) {
    drawLiquidGlassSeekbarTrack(style, fraction, onDarkBackdrop, enabled)
    drawLiquidGlassSeekbarThumb(style, fraction, onDarkBackdrop, pressed)
}
