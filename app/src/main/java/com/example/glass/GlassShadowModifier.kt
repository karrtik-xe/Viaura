package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow as composeShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Drop-in replacement for `Modifier.shadow(...)` used across the app (files import this one instead of the Compose one).
 *
 * Liquid Glass OFF: exactly the normal Compose elevation shadow.
 * Liquid Glass ON : no shadow at all. A translucent glass surface lets a drop shadow show THROUGH it, and the shadow of an
 * animated / sliding / fading item is drawn in its bounding box - that is the faint "square" behind round buttons,
 * pills and swipe actions. Flat glass (blur + border + highlight) has no such artefact. The `clip` behaviour is kept.
 */
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp,
    ambientColor: Color = DefaultShadowColor,
    spotColor: Color = DefaultShadowColor
): Modifier =
    if (ViauraSurfaceStyle.liquidGlass) {
        if (clip) this.clip(shape) else this
    } else {
        this.composeShadow(elevation, shape, clip, ambientColor, spotColor)
    }
