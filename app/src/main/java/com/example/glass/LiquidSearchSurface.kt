package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * One look for every search box of the app.
 * Liquid Glass ON  -> the full liquid-glass surface (tint + sheen + specular border), same as the pinned-player search bar.
 * Liquid Glass OFF -> the previous plain fill + 1dp border ([offBackground] / [offBorder]), exactly as before.
 */
@Composable
fun Modifier.liquidSearchSurface(
    shape: Shape,
    isDark: Boolean,
    offBackground: Color,
    offBorder: Color,
    transparency: Float = LocalPanelTransparency.current
): Modifier {
    return if (LocalLiquidGlass.current) {
        this.glassPanelSurface(shape, isDark, transparency)
    } else {
        this.clip(shape).background(offBackground).border(1.dp, offBorder, shape)
    }
}
