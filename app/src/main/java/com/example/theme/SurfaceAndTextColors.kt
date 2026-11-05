package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Wallpaper based Dynamic Color (Android 12+), active only while Settings > Interface Appearance = System.
 * [light] / [dark] are Android's dynamicLightColorScheme / dynamicDarkColorScheme; null = not active, and every
 * colour below returns exactly the old fixed value. [dark] is also null while AMOLED Black is on (pure black wins).
 * Snapshot state: everything that reads a colour from here recomposes when the palette changes.
 */
object SystemDynamic {
    private val lightState = mutableStateOf<ColorScheme?>(null)
    private val darkState = mutableStateOf<ColorScheme?>(null)

    var light: ColorScheme?
        get() = lightState.value
        set(v) { if (lightState.value != v) lightState.value = v }

    var dark: ColorScheme?
        get() = darkState.value
        set(v) { if (darkState.value != v) darkState.value = v }
}

/**
 * Neutral slate tones used all over the app, named after their original fixed value. Normally they return that value;
 * while System Dynamic Color is active each one returns the wallpaper tinted tone of the same lightness
 * (light tones from the light scheme, dark tones from the dark scheme), so no screen stays plain white / slate.
 */
object Neutral {
    val FFFFFF: Color get() = SystemDynamic.light?.surface ?: Color(0xFFFFFFFF)
    val F8FAFC: Color get() = SystemDynamic.light?.surfaceContainerLow ?: Color(0xFFF8FAFC)
    val F1F5F9: Color get() = SystemDynamic.light?.surfaceContainer ?: Color(0xFFF1F5F9)
    val E2E8F0: Color get() = SystemDynamic.light?.surfaceContainerHigh ?: Color(0xFFE2E8F0)
    val DAE3E9: Color get() = SystemDynamic.light?.surfaceContainerHighest ?: Color(0xFFDAE3E9)
    val CBD5E1: Color get() = SystemDynamic.light?.outlineVariant ?: Color(0xFFCBD5E1)
    val S64748B: Color get() = SystemDynamic.light?.onSurfaceVariant ?: Color(0xFF64748B)
    val S475569: Color get() = SystemDynamic.light?.onSurfaceVariant ?: Color(0xFF475569)
    val S94A3B8: Color get() = SystemDynamic.dark?.onSurfaceVariant ?: Color(0xFF94A3B8)

    val D0F172A: Color get() = SystemDynamic.dark?.surfaceContainerLowest ?: Color(0xFF0F172A)
    val D090D16: Color get() = SystemDynamic.dark?.surface ?: Color(0xFF090D16)
    val D111827: Color get() = SystemDynamic.dark?.surfaceContainerLow ?: Color(0xFF111827)
    val D1B2630: Color get() = SystemDynamic.dark?.surfaceContainer ?: Color(0xFF1B2630)
    val D1E293B: Color get() = SystemDynamic.dark?.surfaceContainerHigh ?: Color(0xFF1E293B)
    val D334155: Color get() = SystemDynamic.dark?.outlineVariant ?: Color(0xFF334155)
    val D243350: Color get() = SystemDynamic.dark?.outlineVariant ?: Color(0xFF243350)
}

// Light Mode Colors (state backed: written by VideoPlayerTheme, read by every screen)
private val lightBgStartState = mutableStateOf(Color(0xFFF8FAFC))

private val lightBgEndState = mutableStateOf(Color(0xFFF8FAFC))

private val lightGlassSurfaceState = mutableStateOf(Color(0xFFF8FAFC))

private val lightGlassBorderState = mutableStateOf(Color(0xFFE2E8F0))

var LightBackgroundStart: Color
    get() = lightBgStartState.value
    set(v) { if (lightBgStartState.value != v) lightBgStartState.value = v }

var LightBackgroundEnd: Color
    get() = lightBgEndState.value
    set(v) { if (lightBgEndState.value != v) lightBgEndState.value = v }

var LightGlassSurface: Color
    get() = lightGlassSurfaceState.value
    set(v) { if (lightGlassSurfaceState.value != v) lightGlassSurfaceState.value = v }

var LightGlassBorder: Color
    get() = lightGlassBorderState.value
    set(v) { if (lightGlassBorderState.value != v) lightGlassBorderState.value = v }

val LightTextPrimary: Color get() = SystemDynamic.light?.onSurface ?: Color(0xFF0F172A)

val LightTextSecondary: Color get() = SystemDynamic.light?.onSurfaceVariant ?: Color(0xFF64748B)

// Dark Mode Colors
private val darkBgStartState = mutableStateOf(Color(0xFF090D16))

private val darkBgEndState = mutableStateOf(Color(0xFF090D16))

private val darkGlassSurfaceState = mutableStateOf(Color(0xFF090D16))

private val darkGlassBorderState = mutableStateOf(Color(0xFF243350))

var DarkBackgroundStart: Color
    get() = darkBgStartState.value
    set(v) { if (darkBgStartState.value != v) darkBgStartState.value = v }

var DarkBackgroundEnd: Color
    get() = darkBgEndState.value
    set(v) { if (darkBgEndState.value != v) darkBgEndState.value = v }

var DarkGlassSurface: Color
    get() = darkGlassSurfaceState.value
    set(v) { if (darkGlassSurfaceState.value != v) darkGlassSurfaceState.value = v }

var DarkGlassBorder: Color
    get() = darkGlassBorderState.value
    set(v) { if (darkGlassBorderState.value != v) darkGlassBorderState.value = v }

val DarkTextPrimary: Color get() = SystemDynamic.dark?.onSurface ?: Color(0xFFF8FAFC)

val DarkTextSecondary: Color get() = SystemDynamic.dark?.onSurfaceVariant ?: Color(0xFF94A3B8)

// AMOLED Black Colors
val AmoledBlackBackground = Color(0xFF000000)

val AmoledBlackSurface = Color(0xFF07090E)

val AmoledBlackBorder = Color(0xFF1E293B)
