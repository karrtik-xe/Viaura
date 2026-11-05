package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// Accent Gradient Colors (Sky Blue to Magenta/Pink default)
// Backed by Compose snapshot state: every composable that reads them recomposes when the App Theme
// (or a user edited gradient) changes, so the new colours apply everywhere without restarting.
private val accentSkyBlueState = mutableStateOf(Color(0xFF38BDF8))

private val accentPinkState = mutableStateOf(Color(0xFFEC4899))

private val accentStrongState = mutableStateOf(Color(0xFF0284C7))

private val accentMidState = mutableStateOf(Color(0xFF6366F1))

private val accentGradientColorsState = mutableStateOf(listOf(Color(0xFF38BDF8), Color(0xFFEC4899)))

private val accentGradientState = mutableStateOf<Brush>(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFFEC4899))))

var AccentSkyBlue: Color
    get() = accentSkyBlueState.value
    set(v) { if (accentSkyBlueState.value != v) accentSkyBlueState.value = v }

var AccentPink: Color
    get() = accentPinkState.value
    set(v) { if (accentPinkState.value != v) accentPinkState.value = v }

/** Middle colour of the selected / edited App Theme gradient (the original indigo for Dynamic). */
var AccentMid: Color
    get() = accentMidState.value
    set(v) { if (accentMidState.value != v) accentMidState.value = v }

/** Soft light-mode container tint that follows the accent (replaces the old fixed ice blue 0xFFE0F2FE). */
val AccentContainerLight: Color
    get() = lerp(Color.White, accentSkyBlueState.value, 0.14f)

/** Soft dark-mode container tint that follows the accent (replaces the old fixed deep blue 0xFF0C4A6E). */
val AccentContainerDark: Color
    get() = lerp(Color(0xFF0B1120), accentSkyBlueState.value, 0.30f)

/** Deeper variant of the primary accent (used in light mode for text / icons). */
var AccentSkyBlueStrong: Color
    get() = accentStrongState.value
    set(v) { if (accentStrongState.value != v) accentStrongState.value = v }

/** The whole app accent gradient. Use [updateAccentGradient] to change it. */
val AccentGradient: Brush
    get() = accentGradientState.value

/** The colours of [AccentGradient] (selected App Theme gradient), for code that draws its own gradients (Liquid Glass sliders / switches). */
val AccentGradientColors: List<Color>
    get() = accentGradientColorsState.value

fun updateAccentGradient(colors: List<Color>) {
    if (accentGradientColorsState.value == colors) return
    accentGradientColorsState.value = colors
    accentGradientState.value = Brush.linearGradient(colors)
}
