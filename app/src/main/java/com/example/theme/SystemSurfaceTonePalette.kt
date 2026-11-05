package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.ui.state.SystemTone

/**
 * Builds the "System" surface palette (backmost background, folder / video cards, every sheet, borders, text) from ONE hue.
 * Used for both the wallpaper (Material You) colour and a hand picked muted colour (double tap on "System").
 * Saturation is always kept in the muted range, so the result is a soft tone (mustard / sand / sage ...) and never a loud red or blue.
 * The accent gradient is NOT touched by this file.
 */
object SystemTonePalette {
    /** Greys (saturation 0) are allowed for a hand picked colour; the wallpaper colour keeps [WALLPAPER_MIN_SAT]. */
    const val MIN_SAT = 0.0f
    const val MAX_SAT = 0.60f
    private const val WALLPAPER_MIN_SAT = 0.18f

    private fun hsl(h: Float, s: Float, l: Float): Color =
        Color(ColorUtils.HSLToColor(floatArrayOf(h.mod(360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))

    /** Hue (0..360) and saturation of a colour, saturation already clamped to the muted range. */
    fun toneOf(color: Color): SystemTone {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(color.toArgb(), out)
        return SystemTone(out[0], out[1].coerceIn(WALLPAPER_MIN_SAT, MAX_SAT), 0.5f)
    }

    fun preview(tone: SystemTone, dark: Boolean = false): Color = if (dark) {
        hsl(tone.hue, tone.sat * 0.55f, 0.16f)
    } else {
        hsl(tone.hue, tone.sat, 0.90f + (0.5f - tone.depth) * 0.08f)
    }

    /** [base] supplies every token this palette does not own (primary, error, ...). */
    fun scheme(base: ColorScheme?, tone: SystemTone, dark: Boolean): ColorScheme {
        val h = tone.hue
        val s = tone.sat.coerceIn(MIN_SAT, MAX_SAT)
        // depth 0..1: 0.5 = default; higher = stronger / darker tint, lower = paler
        val shift = (0.5f - tone.depth.coerceIn(0f, 1f)) * 0.08f
        val b = base ?: if (dark) darkColorScheme() else lightColorScheme()
        return if (!dark) {
            val sat = s
            b.copy(
                background = hsl(h, sat, 0.925f + shift),
                surface = hsl(h, sat, 0.925f + shift),
                surfaceVariant = hsl(h, sat * 0.9f, 0.86f + shift),
                surfaceContainerLowest = hsl(h, sat, 0.965f + shift),
                surfaceContainerLow = hsl(h, sat, 0.905f + shift),
                surfaceContainer = hsl(h, sat, 0.885f + shift),
                surfaceContainerHigh = hsl(h, sat, 0.855f + shift),
                surfaceContainerHighest = hsl(h, sat, 0.825f + shift),
                primaryContainer = hsl(h, sat, 0.80f + shift),
                secondaryContainer = hsl(h, sat * 0.8f, 0.84f + shift),
                outline = hsl(h, sat * 0.5f, 0.40f),
                outlineVariant = hsl(h, sat * 0.8f, 0.72f + shift),
                onSurface = hsl(h, 0.30f, 0.14f),
                onBackground = hsl(h, 0.30f, 0.14f),
                onSurfaceVariant = hsl(h, 0.22f, 0.34f)
            )
        } else {
            val sat = s * 0.55f
            val d = -shift
            b.copy(
                background = hsl(h, sat, 0.10f + d),
                surface = hsl(h, sat, 0.10f + d),
                surfaceVariant = hsl(h, sat, 0.24f + d),
                surfaceContainerLowest = hsl(h, sat, 0.07f + d),
                surfaceContainerLow = hsl(h, sat, 0.13f + d),
                surfaceContainer = hsl(h, sat, 0.16f + d),
                surfaceContainerHigh = hsl(h, sat, 0.20f + d),
                surfaceContainerHighest = hsl(h, sat, 0.24f + d),
                primaryContainer = hsl(h, sat, 0.28f + d),
                secondaryContainer = hsl(h, sat, 0.26f + d),
                outline = hsl(h, sat * 0.6f, 0.58f),
                outlineVariant = hsl(h, sat, 0.30f + d),
                onSurface = hsl(h, 0.20f, 0.90f),
                onBackground = hsl(h, 0.20f, 0.90f),
                onSurfaceVariant = hsl(h, 0.16f, 0.72f)
            )
        }
    }
}
