package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.ui.theme.Neutral
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Muted & desaturated colour space of the round picker (HSL): saturation and lightness stay in a soft range. */
private const val MUTED_MAX_SAT = 0.60f
private const val MUTED_MIN_LIGHT = 0.12f
private const val MUTED_MAX_LIGHT = 0.88f
private const val MUTED_HUE_SAT = 0.40f
private const val MUTED_HUE_LIGHT = 0.50f

private fun mutedHsl(h: Float, s: Float, l: Float): Color =
    Color(ColorUtils.HSLToColor(floatArrayOf(h.mod(360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))

private enum class MutedArc { HUE, SATURATION, LIGHTNESS }

/**
 * Round colour picker with the same 3-arc look as the subtitle colour picker (rainbow arc at the bottom, left arc,
 * right arc, centre circle), but limited to muted & desaturated colours: the hue arc is a muted rainbow, the left arc
 * only reaches a soft saturation and the right arc runs from dark to light. Touches that start away from an arc are
 * ignored (not consumed), so the page can still scroll.
 * [onPick] is called live while dragging; [onPickFinished] once when the finger is lifted.
 */
@Composable
fun MutedCircularColorPicker(
    selectedArgb: Long?,
    centerColor: Color,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onPickFinished: () -> Unit = {}
) {
    val onPickState by rememberUpdatedState(onPick)
    val onFinishedState by rememberUpdatedState(onPickFinished)

    val init = remember {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL((selectedArgb?.toInt() ?: centerColor.toArgb()) or (0xFF shl 24), hsl)
        floatArrayOf(hsl[0], hsl[1].coerceIn(0f, MUTED_MAX_SAT), hsl[2].coerceIn(MUTED_MIN_LIGHT, MUTED_MAX_LIGHT))
    }
    var hue by remember { mutableFloatStateOf(init[0]) }
    var sat by remember { mutableFloatStateOf(init[1]) }
    var light by remember { mutableFloatStateOf(init[2]) }

    fun emit() {
        val rgb = ColorUtils.HSLToColor(floatArrayOf(hue.mod(360f), sat, light)) and 0xFFFFFF
        onPickState(0xFF000000L or rgb.toLong())
    }

    val currentColor = mutedHsl(hue, sat, light)

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val centerPx = Offset(size.width / 2f, size.height / 2f)
                        val arcRadiusPx = (size.width / 2f) * 0.78f
                        val slopPx = 14.dp.toPx()

                        fun angleOf(p: Offset): Float {
                            var a = Math.toDegrees(atan2((p.y - centerPx.y).toDouble(), (p.x - centerPx.x).toDouble())).toFloat()
                            if (a < 0) a += 360f
                            return a
                        }

                        fun applyAt(p: Offset, arc: MutedArc) {
                            val angle = angleOf(p)
                            when (arc) {
                                MutedArc.HUE -> {
                                    hue = (((angle - 40f) / 100f).coerceIn(0f, 1f)) * 360f
                                    if (sat < 0.12f) sat = MUTED_HUE_SAT
                                    if (light < 0.2f || light > 0.8f) light = MUTED_HUE_LIGHT
                                }
                                MutedArc.SATURATION -> {
                                    sat = (((angle - 160f) / 100f).coerceIn(0f, 1f)) * MUTED_MAX_SAT
                                }
                                MutedArc.LIGHTNESS -> {
                                    val a = if (angle < 100f) angle + 360f else angle
                                    val t = ((a - 280f) / 100f).coerceIn(0f, 1f)
                                    light = MUTED_MIN_LIGHT + t * (MUTED_MAX_LIGHT - MUTED_MIN_LIGHT)
                                }
                            }
                            emit()
                        }

                        val dist = (down.position - centerPx).getDistance()
                        if (kotlin.math.abs(dist - arcRadiusPx) > slopPx) return@awaitEachGesture
                        val startAngle = angleOf(down.position)
                        val arc = when {
                            startAngle in 30f..150f -> MutedArc.HUE
                            startAngle in 150f..270f -> MutedArc.SATURATION
                            else -> MutedArc.LIGHTNESS
                        }
                        applyAt(down.position, arc)
                        down.consume()
                        drag(down.id) { change ->
                            applyAt(change.position, arc)
                            change.consume()
                        }
                        onFinishedState()
                    }
                }
        ) {
            drawMutedArcPicker(hue = hue, sat = sat, light = light)
        }

        // Centre circle = the colour that is picked right now (same look as the 3-arc picker)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Neutral.D0F172A)
                .border(2.dp, Neutral.D1E293B, CircleShape)
                .shadow(elevation = 6.dp, shape = CircleShape)
        ) {
            Box(Modifier.fillMaxSize().background(currentColor))
        }
    }
}

private fun DrawScope.drawMutedArcPicker(hue: Float, sat: Float, light: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val arcRadius = (size.width / 2f) * 0.78f
    val strokeWidth = 10.dp.toPx()
    val knobRadius = 11.5.dp.toPx()
    val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)
    val arcSize = Size(arcRadius * 2, arcRadius * 2)

    fun arc(
        outlineStart: Float,
        start: Float,
        segments: Int,
        colorAt: (Float) -> Color,
        progress: Float,
        knobColor: Color
    ) {
        drawArc(
            color = Neutral.D1E293B,
            startAngle = outlineStart,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round)
        )
        val step = 100f / segments
        for (i in 0 until segments) {
            drawArc(
                color = colorAt(i.toFloat() / segments),
                startAngle = start + i * step,
                sweepAngle = step + 0.6f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = if (i == 0 || i == segments - 1) StrokeCap.Round else StrokeCap.Butt
                )
            )
        }
        val rad = Math.toRadians((start + progress.coerceIn(0f, 1f) * 100f).toDouble())
        val knob = Offset((center.x + arcRadius * cos(rad)).toFloat(), (center.y + arcRadius * sin(rad)).toFloat())
        drawCircle(color = Neutral.D1E293B, radius = knobRadius + 2.dp.toPx(), center = knob, style = Fill)
        drawCircle(color = knobColor, radius = knobRadius, center = knob, style = Fill)
    }

    // Bottom arc: muted rainbow (hue)
    arc(38f, 40f, 120, { t -> mutedHsl(t * 360f, MUTED_HUE_SAT, MUTED_HUE_LIGHT) }, hue / 360f, mutedHsl(hue, MUTED_HUE_SAT, MUTED_HUE_LIGHT))
    // Left arc: saturation (grey -> soft colour)
    arc(158f, 160f, 80, { t -> mutedHsl(hue, t * MUTED_MAX_SAT, light) }, sat / MUTED_MAX_SAT, mutedHsl(hue, sat, light))
    // Right / top arc: lightness (dark -> light)
    arc(
        278f, 280f, 80,
        { t -> mutedHsl(hue, sat, MUTED_MIN_LIGHT + t * (MUTED_MAX_LIGHT - MUTED_MIN_LIGHT)) },
        (light - MUTED_MIN_LIGHT) / (MUTED_MAX_LIGHT - MUTED_MIN_LIGHT),
        mutedHsl(hue, sat, light)
    )
}

/**
 * [MutedCircularColorPicker] inside the same card look as the 3-arc colour picker (title on top, round picker below).
 * Used for the second theme colour and for the System colour.
 */
@Composable
fun MutedCircularColorPickerCard(
    title: String,
    selectedArgb: Long?,
    centerColor: Color,
    onPick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onPickFinished: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SheetButtonBgColor)
            .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        MutedCircularColorPicker(
            selectedArgb = selectedArgb,
            centerColor = centerColor,
            onPick = onPick,
            onPickFinished = onPickFinished
        )
    }
}
