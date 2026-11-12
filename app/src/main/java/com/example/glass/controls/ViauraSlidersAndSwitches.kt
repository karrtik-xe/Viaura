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

/*
 * ------------------------------------------------------------------------------------------------
 * LIQUID GLASS CONTROLS (Settings > Interface Appearance > Liquid Glass > "Liquid Glass Sliders & Switches")
 *
 * Glass look for every slider and ON / OFF switch of the app:
 *   - the groove / switch body is translucent, so it takes the colour of whatever is behind it
 *     (video frame, album art, settings sheet)
 *   - the filled ("front") part of a slider and the ON knob of a switch use the selected app theme gradient
 *   - five slider shapes (Normal, Standard, Wavy, Thick, Slim) follow the Seekbar Style option of the video player
 * When the option is OFF every wrapper here falls back to the plain Material control, so the app looks as before.
 * ------------------------------------------------------------------------------------------------
 */

/** True while Liquid Glass AND "Liquid Glass Sliders & Switches" are ON. Provided by VideoPlayerTheme. */
val LocalLiquidGlassControls = compositionLocalOf { false }

/** The Seekbar Style chosen for the video player (audio player and glass previews follow it). Provided by VideoPlayerTheme. */
val LocalSeekbarStyle = compositionLocalOf { SeekbarStyle.STANDARD }

// ------------------------------------------------------------------------------------------------
// Slider
// ------------------------------------------------------------------------------------------------

/** Liquid glass slider: glass groove, gradient front part, glass thumb. Same behaviour as the Material slider. */
@Composable
fun LiquidGlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    style: SeekbarStyle = SeekbarStyle.NORMAL,
    onDarkBackdrop: Boolean = isAppInDarkTheme()
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    var pressedState by remember { mutableStateOf(false) }
    val press by animateFloatAsState(
        targetValue = if (pressedState) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "glassSliderPress"
    )
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)

    Canvas(
        modifier = modifier
            .height(36.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange.start, valueRange.endInclusive), valueRange, steps)
                setProgress { target ->
                    currentOnChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            }
            .pointerInput(enabled, valueRange.start, valueRange.endInclusive, steps) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressedState = true
                    fun emit(x: Float) {
                        var f = (x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        if (steps > 0) f = (f * (steps + 1)).roundToInt() / (steps + 1).toFloat()
                        currentOnChange(valueRange.start + f * span)
                    }
                    // A vertical swipe that starts on the slider must still scroll the page, so the value only
                    // changes once the finger moves sideways (or on a clean tap).
                    val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (slop != null) {
                        emit(slop.position.x)
                        horizontalDrag(slop.id) { change ->
                            change.consume()
                            emit(change.position.x)
                        }
                    } else {
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (up != null && !up.pressed && !up.isConsumed) emit(up.position.x)
                    }
                    pressedState = false
                    currentOnFinished?.invoke()
                }
            }
    ) {
        drawLiquidGlassSeekbar(style, fraction, onDarkBackdrop, press, enabled)
    }
}

/**
 * Drop-in replacement for Material's Slider: liquid glass while "Liquid Glass Sliders & Switches" is ON,
 * the unchanged Material slider otherwise.
 */
@Composable
fun ViauraSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors()
) {
    // Subtle tick every time the value moves to a new notch (every 5 % of the range, or every step).
    val hapticView = LocalView.current
    val rangeSpan = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val notches = if (steps > 0) steps + 1 else 20
    fun notchOf(v: Float): Int = (((v - valueRange.start) / rangeSpan) * notches).roundToInt()
    var lastNotch by remember { mutableIntStateOf(notchOf(value)) }
    val tickingOnChange: (Float) -> Unit = { v ->
        val n = notchOf(v)
        if (n != lastNotch) {
            lastNotch = n
            AppHaptics.performTick(hapticView)
        }
        onValueChange(v)
    }
    if (LocalLiquidGlassControls.current) {
        LiquidGlassSlider(
            value = value,
            onValueChange = tickingOnChange,
            // the Canvas has no intrinsic width: without this a slider placed without an explicit width (Volume boost cap) was 0 px wide
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            style = SeekbarStyle.NORMAL
        )
    } else {
        Slider(
            value = value,
            onValueChange = tickingOnChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors
        )
    }
}

// ------------------------------------------------------------------------------------------------
// ON / OFF switch
// ------------------------------------------------------------------------------------------------

/** Liquid glass ON / OFF switch: glass body, glowing gradient rim and a gradient knob when ON, silver glass knob when OFF. */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDarkBackdrop: Boolean = isAppInDarkTheme()
) {
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "glassSwitchProgress"
    )
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(120),
        label = "glassSwitchPress"
    )
    val ink = if (onDarkBackdrop) Color.White else Neutral.D0F172A

    Box(
        modifier = modifier
            .size(width = 66.dp, height = 34.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = h / 2f
            val colors = glassGradientColors()
            val bodyCorner = CornerRadius(r, r)

            // glass body
            drawRoundRect(color = ink.copy(alpha = if (onDarkBackdrop) 0.14f else 0.08f), cornerRadius = bodyCorner)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = if (onDarkBackdrop) 0.20f else 0.50f), Color.Transparent),
                    startY = 0f,
                    endY = h
                ),
                cornerRadius = bodyCorner
            )
            // rim: white specular line (OFF) -> glowing gradient line (ON)
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.60f), Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.38f)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                cornerRadius = bodyCorner,
                alpha = 1f - progress,
                style = Stroke(width = 1.2.dp.toPx())
            )
            if (progress > 0.01f) {
                val rim = Brush.horizontalGradient(colors = colors, startX = 0f, endX = w)
                drawRoundRect(
                    brush = rim,
                    cornerRadius = bodyCorner,
                    alpha = 0.30f * progress,
                    style = Stroke(width = 5.dp.toPx())
                )
                drawRoundRect(
                    brush = rim,
                    cornerRadius = bodyCorner,
                    alpha = progress,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // knob
            val inset = 3.dp.toPx()
            val d = h - 2f * inset
            val knobR = d / 2f * (1f + 0.08f * press)
            val travel = (w - 2f * inset - d).coerceAtLeast(0f)
            val cx = inset + d / 2f + travel * progress
            val cy = h / 2f
            val knobCenter = Offset(cx, cy)
            for (i in 1..3) {
                drawCircle(
                    color = Color.Black.copy(alpha = if (onDarkBackdrop) 0.12f else 0.07f),
                    radius = knobR + i * 1.2.dp.toPx(),
                    center = Offset(cx, cy + 1.2.dp.toPx())
                )
            }
            // OFF knob: silver glass
            if (progress < 0.99f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.96f), Color(0xFFB8BDC7).copy(alpha = 0.88f), Color(0xFF6B7280).copy(alpha = 0.92f)),
                        center = Offset(cx - knobR * 0.25f, cy - knobR * 0.30f),
                        radius = knobR * 1.5f
                    ),
                    radius = knobR,
                    center = knobCenter,
                    alpha = 1f - progress
                )
            }
            // ON knob: theme gradient
            if (progress > 0.01f) {
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = colors,
                        start = Offset(cx - knobR, cy - knobR),
                        end = Offset(cx + knobR, cy + knobR)
                    ),
                    radius = knobR,
                    center = knobCenter,
                    alpha = progress
                )
            }
            // glass shine on the knob
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0f)),
                    startY = cy - knobR,
                    endY = cy
                ),
                topLeft = Offset(cx - knobR * 0.62f, cy - knobR * 0.90f),
                size = Size(knobR * 1.24f, knobR * 0.85f)
            )
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.55f)),
                    start = Offset(cx - knobR, cy - knobR),
                    end = Offset(cx + knobR, cy + knobR)
                ),
                radius = knobR - 0.6.dp.toPx(),
                center = knobCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }
        // ON / OFF labels on the free side of the knob
        val onColor = lerp(glassGradientColors().first(), if (onDarkBackdrop) Color.White else Color.Black, if (onDarkBackdrop) 0.40f else 0.25f)
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "ON",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = onColor,
                modifier = Modifier.alpha(progress)
            )
        }
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 11.dp), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "OFF",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ink.copy(alpha = 0.75f),
                modifier = Modifier.alpha(1f - progress)
            )
        }
    }
}

/**
 * Drop-in replacement for Material's Switch: liquid glass while "Liquid Glass Sliders & Switches" is ON,
 * the unchanged Material switch otherwise.
 */
@Composable
fun ViauraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors()
) {
    // Feedback when the switch is turned ON / OFF.
    val hapticView = LocalView.current
    val tickingOnCheckedChange: ((Boolean) -> Unit)? = onCheckedChange?.let { callback ->
        { on: Boolean ->
            AppHaptics.performToggle(hapticView, on)
            callback(on)
        }
    }
    if (LocalLiquidGlassControls.current) {
        LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = tickingOnCheckedChange,
            modifier = modifier,
            enabled = enabled
        )
    } else {
        Switch(
            checked = checked,
            onCheckedChange = tickingOnCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = colors
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Vertical bar (volume / brightness gesture HUD of the video player)
// ------------------------------------------------------------------------------------------------

/** Vertical glass bar: gradient fill rising from the bottom (no thumb, so it simply grows with the value). */
@Composable
fun LiquidGlassVerticalBar(
    fraction: Float,
    trackWidth: Dp,
    modifier: Modifier = Modifier,
    pressed: Float = 0f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val tw = trackWidth.toPx().coerceIn(2.dp.toPx(), w)
        val left = w / 2f - tw / 2f
        val f = fraction.coerceIn(0f, 1f)
        val corner = CornerRadius(tw / 2f, tw / 2f)
        val colors = glassGradientColors().reversed()

        drawRoundRect(color = Color.White.copy(alpha = 0.16f), topLeft = Offset(left, 0f), size = Size(tw, h), cornerRadius = corner)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.35f)),
                start = Offset(left, 0f),
                end = Offset(left + tw, h)
            ),
            topLeft = Offset(left, 0f),
            size = Size(tw, h),
            cornerRadius = corner,
            style = Stroke(width = 1.dp.toPx())
        )
        // Empty (0%): nothing is drawn, so no leftover dot remains at the bottom of the bar.
        if (f <= 0.001f) return@Canvas
        val fillH = h * f
        val top = h - fillH
        val fillCorner = CornerRadius(minOf(tw / 2f, fillH / 2f), minOf(tw / 2f, fillH / 2f))
        val brush = Brush.verticalGradient(colors = colors, startY = 0f, endY = h)
        drawRoundRect(
            brush = brush,
            topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
            size = Size(tw + 4.dp.toPx(), fillH + 4.dp.toPx()),
            cornerRadius = CornerRadius(fillCorner.x + 2.dp.toPx(), fillCorner.x + 2.dp.toPx()),
            alpha = 0.26f
        )
        drawRoundRect(brush = brush, topLeft = Offset(left, top), size = Size(tw, fillH), cornerRadius = fillCorner, alpha = 0.85f)
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0f)),
                startX = left,
                endX = left + tw * 0.6f
            ),
            topLeft = Offset(left + tw * 0.14f, top + tw * 0.3f),
            size = Size(tw * 0.30f, (fillH - tw * 0.6f).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(tw * 0.15f, tw * 0.15f)
        )
    }
}
