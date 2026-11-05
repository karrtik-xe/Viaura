package com.example.ui.components

import com.example.ui.theme.Neutral

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** The app-size setting (1..100), provided once by the app theme so every panel / sheet can follow it. */
val LocalAppScale = compositionLocalOf { 75f }

/** The "UI Glass Blur & Opacity" setting (10..100), provided once by the app theme. Not used by the video player's own controls. */
val LocalPanelTransparency = compositionLocalOf { 85f }

/** Settings > Interface Appearance > "Liquid Glass" (frosted translucent surfaces, specular white border, round shapes). Provided once by the app theme. */
val LocalLiquidGlass = compositionLocalOf { false }

/**
 * Settings > Interface Appearance > Liquid Glass options (same option set as mpvRx, shown only while Liquid Glass is ON).
 * Percent values: 100 = default. [highlightStyle]: 0 Component, 1 Default, 2 Ambient, 3 Plain.
 */
@androidx.compose.runtime.Immutable
data class LiquidGlassParams(
    val opacity: Float = 100f,            // 0..100  strength of the blurred video behind the glass
    val blur: Float = 100f,               // 0..200
    val tint: Float = 50f,                // 0..100  colour wash of the glass fill (Viaura)
    val refractionHeight: Float = 100f,   // 0..200  how far in from the edge the lens bends
    val refractionAmount: Float = 100f,   // 0..200  how strongly it bends
    val depthEffect: Boolean = false,
    val chromaticAberration: Boolean = true,
    val vibrancy: Boolean = true,
    val saturation: Float = 100f,         // 0..200
    val brightness: Float = 0f,           // -100..100
    val contrast: Float = 100f,           // 0..200
    val highlightStyle: Int = 0,
    val highlightStrength: Float = 100f,  // 0..200
    val highlightWidth: Float = 100f,     // 0..200
    val highlightBlur: Float = 100f       // 0..200
) {
    fun encode(): String = listOf(
        opacity, blur, tint, refractionHeight, refractionAmount,
        if (depthEffect) 1f else 0f, if (chromaticAberration) 1f else 0f, if (vibrancy) 1f else 0f,
        saturation, brightness, contrast, highlightStyle.toFloat(), highlightStrength, highlightWidth, highlightBlur
    ).joinToString(",")

    companion object {
        fun decode(raw: String?): LiquidGlassParams {
            val d = LiquidGlassParams()
            val v = raw?.split(",")?.mapNotNull { it.trim().toFloatOrNull() } ?: return d
            if (v.size != 15 || v.any { !it.isFinite() }) return d
            return LiquidGlassParams(
                opacity = v[0].coerceIn(0f, 100f), blur = v[1].coerceIn(0f, 200f), tint = v[2].coerceIn(0f, 100f),
                refractionHeight = v[3].coerceIn(0f, 200f), refractionAmount = v[4].coerceIn(0f, 200f),
                depthEffect = v[5] >= 0.5f, chromaticAberration = v[6] >= 0.5f, vibrancy = v[7] >= 0.5f,
                saturation = v[8].coerceIn(0f, 200f), brightness = v[9].coerceIn(-100f, 100f), contrast = v[10].coerceIn(0f, 200f),
                highlightStyle = v[11].toInt().coerceIn(0, 3), highlightStrength = v[12].coerceIn(0f, 200f),
                highlightWidth = v[13].coerceIn(0f, 200f), highlightBlur = v[14].coerceIn(0f, 200f)
            )
        }
    }
}

val LocalLiquidGlassParams = compositionLocalOf { LiquidGlassParams() }

/** True while AMOLED Black Mode is active (dark theme + AMOLED switch). Liquid Glass then keeps its glass base around #000000. */
val LocalAmoledBlack = compositionLocalOf { false }

/**
 * Background of the app's panels and bottom sheets (Delete, Download, Sort & View, Settings, playlist sheets ...): the colour
 * of the Delete panel. The opacity follows the glass slider gently (about 73 % at 10 %, 94 % at the default 85 %, 98 % at 100 %).
 * The video player's own control panels keep their separate look.
 */
fun glassPanelColor(isDark: Boolean, glassBlurTransparency: Float): Color {
    // Liquid Glass: one central translucent "frosted" tint for every sheet / panel / dialog (see LiquidGlassModifiers.kt)
    if (ViauraSurfaceStyle.liquidGlass) return liquidGlassPanelFill(isDark, glassBlurTransparency)
    val factor = (glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)
    val alpha = (0.70f + 0.28f * factor).coerceIn(0.70f, 0.98f)
    // Theme = System and the phone is in light mode: every panel / popup uses the same white as the folder & video cards
    if (ViauraSurfaceStyle.systemMode && !isDark) return Neutral.F8FAFC.copy(alpha = alpha)
    return (if (isDark) Neutral.D1B2630 else Neutral.DAE3E9).copy(alpha = alpha)
}

/** Same density factor the Settings and Sort & View panels use, so a sheet gets bigger / smaller with the app-size setting. */
@Composable
fun PanelScaled(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val appScale = LocalAppScale.current
    val factor = (0.85f + 0.15f * ((appScale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
    val scaled = remember(base.density, base.fontScale, factor) {
        Density(density = base.density * factor, fontScale = base.fontScale * factor)
    }
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}

/**
 * Set by [com.example.ui.theme.VideoPlayerTheme]: true while Settings > Appearance is on "System".
 *
 * These are Compose snapshot states (NOT plain fields): every composable that reads them (glassPanelColor() and friends)
 * is recomposed the moment the theme changes. A plain @Volatile flag was the cause of the "Light mode shows a white
 * Track Information sheet" bug: switching System -> Light left `isDark` unchanged (false), so the sheet never
 * recomposed and kept the stale System-light white.
 */
object ViauraSurfaceStyle {
    var systemMode: Boolean by mutableStateOf(false)
    /** Liquid Glass setting (Settings > Interface Appearance). */
    var liquidGlass: Boolean by mutableStateOf(false)
    /** AMOLED active (dark theme + AMOLED switch). */
    var amoled: Boolean by mutableStateOf(false)
    /** "UI Glass Blur & Opacity" slider (10..100), for code that has no composition (dialog window blur). */
    var glassTransparency: Float by mutableStateOf(85f)
    /** Liquid Glass blur (%), for code that has no composition (dialog window blur). */
    var glassBlur: Float by mutableStateOf(100f)
    /** Liquid Glass highlight border options, read by the border helpers. */
    var highlightStyle: Int by mutableStateOf(0)
    var highlightStrength: Float by mutableStateOf(100f)
    var highlightWidth: Float by mutableStateOf(100f)
    var highlightBlur: Float by mutableStateOf(100f)
}
