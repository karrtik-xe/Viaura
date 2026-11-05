package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.R

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerMediaTrack
import com.example.player.SubtitleFontManager
import com.example.ui.state.PlayerSettings
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class ColorTarget {
    TEXT,
    BORDER,
    BACKGROUND,
    SHADOW
}

private enum class ArcPickerType {
    HUE,
    SATURATION,
    VALUE
}

// =============================================================================
// 2. ENHANCED COLORS SECTION WITH 3-ARC CIRCULAR COLOR PICKER (Image 1 style)
// =============================================================================
@Composable
internal fun EnhancedColorsContent(
    settings: PlayerSettings,
    onUpdateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit
) {
    var activeColorTarget by remember { mutableStateOf<ColorTarget>(ColorTarget.TEXT) }

    // 4 Color Swatches: [Text] [Border] [Background] [Shadow] + [Reset Colors]
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ColorTarget.entries.forEach { target ->
            val isSelected = activeColorTarget == target
            val targetLabel = when (target) {
                ColorTarget.TEXT -> "Text"
                ColorTarget.BORDER -> "Border"
                ColorTarget.BACKGROUND -> "Bg"
                ColorTarget.SHADOW -> "Shadow"
            }
            val targetColorLong = when (target) {
                ColorTarget.TEXT -> settings.subtitleTextColor
                ColorTarget.BORDER -> settings.subtitleBorderColor
                ColorTarget.BACKGROUND -> settings.subtitleBackgroundColor
                ColorTarget.SHADOW -> settings.subtitleShadowColor
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else SheetButtonBgColor
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else SheetButtonBorderColor,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { activeColorTarget = target }
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(Neutral.D334155)
                            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(targetColorLong.toInt()))
                        )
                    }
                    Text(
                        text = targetLabel,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else SheetButtonTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Reset Colors Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SheetButtonBgColor)
                .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                .clickable {
                    onUpdateSettings {
                        it.copy(
                            subtitleTextColor = 0xFFFFFFFFL,
                            subtitleBorderColor = 0xFF000000L,
                            subtitleBackgroundColor = 0x99000000L,
                            subtitleShadowColor = 0xBF000000L
                        )
                    }
                }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.RestartAlt,
                contentDescription = "Reset Colors",
                tint = SheetButtonTextColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // -------------------------------------------------------------------------
    // 3-Arc Circular Interactive Color Picker Card (Exact Image 1 Design)
    // -------------------------------------------------------------------------
    val currentColorLong = when (activeColorTarget) {
        ColorTarget.TEXT -> settings.subtitleTextColor
        ColorTarget.BORDER -> settings.subtitleBorderColor
        ColorTarget.BACKGROUND -> settings.subtitleBackgroundColor
        ColorTarget.SHADOW -> settings.subtitleShadowColor
    }

    val activeTargetTitle = when (activeColorTarget) {
        ColorTarget.TEXT -> "Text Color"
        ColorTarget.BORDER -> "Border Color"
        ColorTarget.BACKGROUND -> "Background Color"
        ColorTarget.SHADOW -> "Shadow Color"
    }

    key(activeColorTarget) {
        ThreeArcCircularColorPickerCard(
            title = activeTargetTitle,
            colorLong = currentColorLong,
            onColorChange = { updatedColorLong ->
                onUpdateSettings { latest ->
                    when (activeColorTarget) {
                        ColorTarget.TEXT -> latest.copy(subtitleTextColor = updatedColorLong)
                        ColorTarget.BORDER -> latest.copy(subtitleBorderColor = updatedColorLong)
                        ColorTarget.BACKGROUND -> latest.copy(subtitleBackgroundColor = updatedColorLong)
                        ColorTarget.SHADOW -> latest.copy(subtitleShadowColor = updatedColorLong)
                    }
                }
            }
        )
    }
}

// =============================================================================
// THREE-ARC CIRCULAR COLOR PICKER (Matching Image 1)
// =============================================================================
@Composable
internal fun ThreeArcCircularColorPickerCard(
    title: String,
    colorLong: Long,
    showAlpha: Boolean = true,
    onColorChange: (Long) -> Unit
) {
    val onColorChangeUpdated by rememberUpdatedState(onColorChange)

    val initialHsv = remember(colorLong) {
        val array = FloatArray(3)
        AndroidColor.colorToHSV(colorLong.toInt(), array)
        array
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember { mutableFloatStateOf(Color(colorLong.toInt()).alpha) }
    var activeArc by remember { mutableStateOf<ArcPickerType?>(null) }

    fun emitColor(h: Float, s: Float, v: Float, a: Float) {
        val alphaInt = (a * 255f).roundToInt().coerceIn(0, 255)
        val argb = AndroidColor.HSVToColor(alphaInt, floatArrayOf(h, s, v))
        onColorChangeUpdated(argb.toLong() and 0xFFFFFFFFL)
    }

    val centerPreviewColor = remember(hue, saturation, value, alpha) {
        val alphaInt = (alpha * 255f).roundToInt().coerceIn(0, 255)
        val argb = AndroidColor.HSVToColor(alphaInt, floatArrayOf(hue, saturation, value))
        Color(argb)
    }

    Column(
        modifier = Modifier
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

        // The 3-Arc Canvas with Center Preview Circle
        Box(
            modifier = Modifier
                .size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Only a touch that STARTS on the thin arc "line" (± a small slop) is handled.
                        // Touches in the empty area around the arcs / in the centre are ignored and are
                        // not consumed, so the panel can still be scrolled when the finger starts there.
                        // Tap and drag are one gesture: the colour is applied on touch-down and then
                        // follows the finger.
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

                            fun applyAt(p: Offset, arc: ArcPickerType) {
                                val angle = angleOf(p)
                                when (arc) {
                                    ArcPickerType.HUE -> {
                                        hue = (((angle - 40f) / 100f).coerceIn(0f, 1f)) * 360f
                                        if (value < 0.15f) value = 1.0f
                                        if (saturation < 0.15f) saturation = 1.0f
                                    }
                                    ArcPickerType.SATURATION -> {
                                        saturation = ((angle - 160f) / 100f).coerceIn(0f, 1f)
                                        if (value < 0.15f) value = 1.0f
                                    }
                                    ArcPickerType.VALUE -> {
                                        val normalizedAngle = if (angle < 100f) angle + 360f else angle
                                        value = ((normalizedAngle - 280f) / 100f).coerceIn(0f, 1f)
                                    }
                                }
                                emitColor(hue, saturation, value, alpha)
                            }

                            val dist = (down.position - centerPx).getDistance()
                            if (kotlin.math.abs(dist - arcRadiusPx) > slopPx) return@awaitEachGesture
                            val startAngle = angleOf(down.position)
                            val arc = when {
                                startAngle in 30f..150f -> ArcPickerType.HUE
                                startAngle in 150f..270f -> ArcPickerType.SATURATION
                                else -> ArcPickerType.VALUE
                            }
                            activeArc = arc
                            try {
                                applyAt(down.position, arc)
                                down.consume()
                                drag(down.id) { change ->
                                    applyAt(change.position, arc)
                                    change.consume()
                                }
                            } finally {
                                activeArc = null
                            }
                        }
                    }
            ) {
                drawThreeArcColorPicker(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    currentColor = centerPreviewColor
                )
            }

            // Center Circular Live Color Preview (Matching Image 1 center circle with dark border)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Neutral.D0F172A)
                    .border(2.dp, Neutral.D1E293B, CircleShape)
                    .shadow(elevation = 6.dp, shape = CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(centerPreviewColor)
                )
            }
        }

        // Alpha / Opacity Slider (Clean & smooth)
        if (showAlpha) Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opacity / Alpha",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SheetButtonTextColor
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGradient)
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(alpha * 100f).roundToInt()}%",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            ViauraSlider(
                value = alpha,
                onValueChange = { newAlpha ->
                    alpha = newAlpha
                    emitColor(hue, saturation, value, newAlpha)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = AccentPink,
                    activeTrackColor = AccentSkyBlue,
                    inactiveTrackColor = SheetButtonBorderColor
                )
            )
        }
    }
}

/**
 * Draws the 3 disconnected curved arcs matching Image 1:
 * - Bottom Arc: Full Rainbow Hue spectrum 0°–360° (40° to 140°, span 100°)
 * - Left Arc: Saturation gradient (160° to 260°, span 100°)
 * - Right/Top Arc: Brightness/Value gradient (280° to 380° / 20°, span 100°)
 * - Each arc has a distinct draggable thumb knob with border filled with the current tone!
 */
private fun DrawScope.drawThreeArcColorPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    currentColor: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = size.width / 2f
    val arcRadius = maxRadius * 0.78f
    val strokeWidth = 10.dp.toPx()
    val knobRadius = 11.5.dp.toPx()

    val pureHueColor = Color(AndroidColor.HSVToColor(255, floatArrayOf(hue, 1f, 1f)))

    // -------------------------------------------------------------------------
    // 1. BOTTOM ARC: Full Rainbow Hue Spectrum 0° to 360° along (40° to 140°)
    // -------------------------------------------------------------------------
    drawArc(
        color = Neutral.D1E293B,
        startAngle = 38f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round)
    )

    val hueSegments = 120
    val hueStepSweep = 100f / hueSegments
    for (i in 0 until hueSegments) {
        val segHue = (i.toFloat() / hueSegments) * 360f
        val segColor = Color(AndroidColor.HSVToColor(255, floatArrayOf(segHue, 1f, 1f)))
        drawArc(
            color = segColor,
            startAngle = 40f + (i * hueStepSweep),
            sweepAngle = hueStepSweep + 0.6f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(
                width = strokeWidth,
                cap = if (i == 0 || i == hueSegments - 1) StrokeCap.Round else StrokeCap.Butt
            )
        )
    }

    // Knob for Hue
    val hueProgress = (hue / 360f).coerceIn(0f, 1f)
    val hueAngleDeg = 40f + (hueProgress * 100f)
    val hueRad = Math.toRadians(hueAngleDeg.toDouble())
    val hueKnobPos = Offset(
        (center.x + arcRadius * cos(hueRad)).toFloat(),
        (center.y + arcRadius * sin(hueRad)).toFloat()
    )
    drawCircle(
        color = Neutral.D1E293B,
        radius = knobRadius + 2.dp.toPx(),
        center = hueKnobPos,
        style = Fill
    )
    drawCircle(
        color = pureHueColor,
        radius = knobRadius,
        center = hueKnobPos,
        style = Fill
    )

    // -------------------------------------------------------------------------
    // 2. LEFT ARC: Saturation Gradient (160° to 260°, span 100°)
    // -------------------------------------------------------------------------
    drawArc(
        color = Neutral.D1E293B,
        startAngle = 158f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round)
    )

    val satSegments = 80
    val satStepSweep = 100f / satSegments
    for (i in 0 until satSegments) {
        val segSat = i.toFloat() / satSegments
        val segColor = Color(AndroidColor.HSVToColor(255, floatArrayOf(hue, segSat, 1f)))
        drawArc(
            color = segColor,
            startAngle = 160f + (i * satStepSweep),
            sweepAngle = satStepSweep + 0.6f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(
                width = strokeWidth,
                cap = if (i == 0 || i == satSegments - 1) StrokeCap.Round else StrokeCap.Butt
            )
        )
    }

    // Knob for Saturation
    val satProgress = saturation.coerceIn(0f, 1f)
    val satAngleDeg = 160f + (satProgress * 100f)
    val satRad = Math.toRadians(satAngleDeg.toDouble())
    val satKnobPos = Offset(
        (center.x + arcRadius * cos(satRad)).toFloat(),
        (center.y + arcRadius * sin(satRad)).toFloat()
    )
    val currentSatTone = Color(AndroidColor.HSVToColor(255, floatArrayOf(hue, saturation, 1f)))
    drawCircle(
        color = Neutral.D1E293B,
        radius = knobRadius + 2.dp.toPx(),
        center = satKnobPos,
        style = Fill
    )
    drawCircle(
        color = currentSatTone,
        radius = knobRadius,
        center = satKnobPos,
        style = Fill
    )

    // -------------------------------------------------------------------------
    // 3. RIGHT/TOP ARC: Value / Brightness Gradient (280° to 380° / 20°, span 100°)
    // -------------------------------------------------------------------------
    drawArc(
        color = Neutral.D1E293B,
        startAngle = 278f,
        sweepAngle = 104f,
        useCenter = false,
        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round)
    )

    val valSegments = 80
    val valStepSweep = 100f / valSegments
    for (i in 0 until valSegments) {
        val segVal = i.toFloat() / valSegments
        val segColor = Color(AndroidColor.HSVToColor(255, floatArrayOf(hue, saturation.coerceAtLeast(0.05f), segVal)))
        drawArc(
            color = segColor,
            startAngle = 280f + (i * valStepSweep),
            sweepAngle = valStepSweep + 0.6f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(
                width = strokeWidth,
                cap = if (i == 0 || i == valSegments - 1) StrokeCap.Round else StrokeCap.Butt
            )
        )
    }

    // Knob for Value/Brightness
    val valProgress = value.coerceIn(0f, 1f)
    val valAngleDeg = 280f + (valProgress * 100f)
    val valRad = Math.toRadians(valAngleDeg.toDouble())
    val valKnobPos = Offset(
        (center.x + arcRadius * cos(valRad)).toFloat(),
        (center.y + arcRadius * sin(valRad)).toFloat()
    )
    val currentValTone = Color(AndroidColor.HSVToColor(255, floatArrayOf(hue, saturation, value)))
    drawCircle(
        color = Neutral.D1E293B,
        radius = knobRadius + 2.dp.toPx(),
        center = valKnobPos,
        style = Fill
    )
    drawCircle(
        color = currentValTone,
        radius = knobRadius,
        center = valKnobPos,
        style = Fill
    )
}
