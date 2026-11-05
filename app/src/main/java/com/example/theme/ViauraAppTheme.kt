package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.ui.state.AppThemeVariant
import com.example.ui.state.ThemeMode
import androidx.compose.ui.text.font.FontFamily

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalIsSystemFontEnabled = compositionLocalOf { false }
val LocalAppFontFamily = compositionLocalOf<FontFamily> { FontFamily.Default }

@Composable
fun isAppInDarkTheme(): Boolean {
    val localDark = LocalIsDarkTheme.current
    if (localDark) return true
    return MaterialTheme.colorScheme.surface.luminance() < 0.5f
}

/** Blends [base] towards [tone] by [amount] (0..1); same maths the built-in themes use for their derived tones. */
private fun mixToneHex(base: Long, tone: Long, amount: Float): Long {
    fun ch(shift: Int): Long {
        val b = ((base shr shift) and 0xFF).toFloat()
        val t = ((tone shr shift) and 0xFF).toFloat()
        return (b + (t - b) * amount).toInt().coerceIn(0, 255).toLong()
    }
    return 0xFF000000L or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

@Composable
fun VideoPlayerTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    appScale: Float = 75f,
    glassBlurTransparency: Float = 85f,
    appTheme: AppThemeVariant = AppThemeVariant.DYNAMIC,
    customGradientHex: List<Long>? = null,
    systemTone: com.example.ui.state.SystemTone? = null,
    amoledBlackMode: Boolean = false,
    liquidGlassMode: Boolean = false,
    useSystemFont: Boolean = false,
    liquidGlassParams: com.example.ui.components.LiquidGlassParams = com.example.ui.components.LiquidGlassParams(),
    liquidGlassControls: Boolean = false,
    seekbarStyle: com.example.ui.state.SeekbarStyle = com.example.ui.state.SeekbarStyle.STANDARD,
    language: com.example.ui.state.AppLanguage = com.example.ui.state.AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Interface Appearance = System on Android 12+: wallpaper based Dynamic Color (like Gboard / Google apps).
    // Light and dark schemes are both read; the tokens in SurfaceAndTextColors.kt (SystemDynamic / Neutral) feed every screen from them.
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // System surfaces: the wallpaper hue (Android 12+) or the muted colour picked with the circle picker (any Android version).
    // Both go through the same muted tonal palette; the accent gradient is never taken from here.
    val dynamicSchemes = remember(themeMode, configuration, systemTone) {
        if (themeMode != ThemeMode.SYSTEM) {
            null
        } else if (systemTone != null) {
            SystemTonePalette.scheme(null, systemTone, false) to SystemTonePalette.scheme(null, systemTone, true)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                val l = dynamicLightColorScheme(context)
                val d = dynamicDarkColorScheme(context)
                val tone = SystemTonePalette.toneOf(l.primary)
                SystemTonePalette.scheme(l, tone, false) to SystemTonePalette.scheme(d, tone, true)
            } catch (_: Throwable) {
                null
            }
        } else {
            // Android 11 and older have no wallpaper colours: start from the muted mustard instead of plain white
            val t = com.example.ui.state.SystemTone.Mustard
            SystemTonePalette.scheme(null, t, false) to SystemTonePalette.scheme(null, t, true)
        }
    }
    val effectiveAmoledForDynamic = amoledBlackMode && isDark
    SystemDynamic.light = dynamicSchemes?.first
    SystemDynamic.dark = if (effectiveAmoledForDynamic) null else dynamicSchemes?.second
    // The accent gradient always stays the normal (theme) gradient, also while Appearance = System.

    // A user edited gradient (3 colours) overrides the theme's built-in one; DYNAMIC is never overridden.
    val gradientHex = customGradientHex?.takeIf { it.size in 2..3 } ?: appTheme.gradientHex
    val gradientColorList = gradientHex.map { Color(it) }
    val primaryColor = gradientColorList.first()
    val secondaryColor = gradientColorList.last()

    // Keep the existing application's global gradient language, but make it follow
    // the selected App Theme everywhere the shared AccentGradient is used.
    AccentSkyBlue = primaryColor
    AccentPink = secondaryColor
    AccentSkyBlueStrong = if (gradientHex == AppThemeVariant.DYNAMIC.gradientHex) Color(0xFF0284C7)
        else androidx.compose.ui.graphics.lerp(primaryColor, Color.Black, 0.30f)
    AccentMid = if (gradientColorList.size >= 3) gradientColorList[gradientColorList.size / 2]
        else if (gradientHex == AppThemeVariant.DYNAMIC.gradientHex) Color(0xFF6366F1)
        else androidx.compose.ui.graphics.lerp(primaryColor, secondaryColor, 0.5f)
    updateAccentGradient(gradientColorList)

    // A user edited gradient also drives the derived background / card / border tones (from its middle colour),
    // exactly like the built-in theme does, so every surface follows the edit.
    val isCustomGradient = gradientHex != appTheme.gradientHex
    val toneHex = if (gradientHex.size >= 3) gradientHex[1] else mixToneHex(gradientHex.first(), gradientHex.last(), 0.5f)
    val lightBg = if (isCustomGradient) Color(mixToneHex(0xFFFFFFFF, toneHex, 0.07f)) else appTheme.lightBgColor
    val lightCard = if (isCustomGradient) Color(mixToneHex(0xFFFFFFFF, toneHex, 0.12f)) else appTheme.lightCardColor
    val lightBorder = if (isCustomGradient) Color(mixToneHex(0xFFE2E8F0, toneHex, 0.25f)) else appTheme.lightBorderColor
    val darkBg = if (isCustomGradient) Color(mixToneHex(0xFF090D16, toneHex, 0.12f)) else appTheme.darkBgColor
    val darkCard = if (isCustomGradient) Color(mixToneHex(0xFF111827, toneHex, 0.16f)) else appTheme.darkCardColor
    val darkBorder = if (isCustomGradient) Color(mixToneHex(0xFF1F2937, toneHex, 0.30f)) else appTheme.darkBorderColor

    // Update global background/surface colors to match the theme variant
    LightBackgroundStart = lightBg
    LightBackgroundEnd = lightBg
    LightGlassSurface = lightCard
    LightGlassBorder = lightBorder

    val effectiveAmoled = amoledBlackMode && isDark

    // System: cards and panels stay white (system light) / the dark surface (system dark).
    // Light or Dark chosen by hand: folder / video cards (and every card surface) use the "ice" tone of the Sort & View panel.
    val systemMode = themeMode == ThemeMode.SYSTEM
    // Snapshot-state flags: every glassPanelColor() reader recomposes when they change (Light <-> System <-> Dark).
    // Written only when the value really changes, and before any content is composed.
    if (com.example.ui.components.ViauraSurfaceStyle.systemMode != systemMode) {
        com.example.ui.components.ViauraSurfaceStyle.systemMode = systemMode
    }
    if (com.example.ui.components.ViauraSurfaceStyle.liquidGlass != liquidGlassMode) {
        com.example.ui.components.ViauraSurfaceStyle.liquidGlass = liquidGlassMode
    }
    com.example.ui.components.ViauraSurfaceStyle.let { st ->
        if (st.glassBlur != liquidGlassParams.blur) st.glassBlur = liquidGlassParams.blur
        if (st.highlightStyle != liquidGlassParams.highlightStyle) st.highlightStyle = liquidGlassParams.highlightStyle
        if (st.highlightStrength != liquidGlassParams.highlightStrength) st.highlightStrength = liquidGlassParams.highlightStrength
        if (st.highlightWidth != liquidGlassParams.highlightWidth) st.highlightWidth = liquidGlassParams.highlightWidth
        if (st.highlightBlur != liquidGlassParams.highlightBlur) st.highlightBlur = liquidGlassParams.highlightBlur
    }
    if (com.example.ui.components.ViauraSurfaceStyle.glassTransparency != glassBlurTransparency) {
        com.example.ui.components.ViauraSurfaceStyle.glassTransparency = glassBlurTransparency
    }
    if (com.example.ui.components.ViauraSurfaceStyle.amoled != effectiveAmoled) {
        com.example.ui.components.ViauraSurfaceStyle.amoled = effectiveAmoled
    }
    // System: white surfaces with an ice outline. Light chosen by hand: ice surfaces with a pure white outline.
    if (systemMode) {
        LightGlassSurface = Color.White
        LightGlassBorder = Color(0xFFDAE3E9)
    } else {
        LightGlassSurface = Color(0xFFDAE3E9)
        LightGlassBorder = Color.White
    }

    if (effectiveAmoled) {
        DarkBackgroundStart = AmoledBlackBackground
        DarkBackgroundEnd = AmoledBlackBackground
        DarkGlassSurface = AmoledBlackSurface
        DarkGlassBorder = AmoledBlackBorder
    } else {
        DarkBackgroundStart = darkBg
        DarkBackgroundEnd = darkBg
        DarkGlassSurface = darkCard
        DarkGlassBorder = darkBorder
    }

    if (!systemMode && !effectiveAmoled) DarkGlassSurface = Color(0xFF22303B)

    // System + Dynamic Color: no plain white / slate anywhere. Root background, cards and borders come from the wallpaper palette.
    dynamicSchemes?.let { (dl, dd) ->
        LightBackgroundStart = dl.surface
        LightBackgroundEnd = dl.surface
        LightGlassSurface = dl.surfaceContainerLow
        LightGlassBorder = dl.outlineVariant
        if (!effectiveAmoled) {
            DarkBackgroundStart = dd.surface
            DarkBackgroundEnd = dd.surface
            DarkGlassSurface = dd.surfaceContainerHigh
            DarkGlassBorder = dd.outlineVariant
        }
    }
    val activeDynamicScheme = when {
        dynamicSchemes == null -> null
        isDark -> if (effectiveAmoled) null else dynamicSchemes.second
        else -> dynamicSchemes.first
    }

    val colorScheme = if (activeDynamicScheme != null) {
        activeDynamicScheme.copy(
            primary = primaryColor,
            secondary = secondaryColor,
            onPrimary = Color.White,
            onSecondary = Color.White
        )
    } else if (isDark) {
        if (effectiveAmoled) {
            darkColorScheme(
                primary = primaryColor,
                secondary = secondaryColor,
                background = AmoledBlackBackground,
                surface = AmoledBlackSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            darkColorScheme(
                primary = primaryColor,
                secondary = secondaryColor,
                background = darkBg,
                surface = darkCard,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = DarkTextPrimary,
                onSurface = DarkTextPrimary
            )
        }
    } else {
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = lightBg,
            surface = lightCard,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = LightTextPrimary,
            onSurface = LightTextPrimary
        )
    }

    val currentDensity = LocalDensity.current
    // 1% maps to 0.85f (85% min application size), 100% maps to 1.00f (100% full scale)
    val scaleFactor = (0.85f + 0.15f * ((appScale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
    val scaledDensity = Density(
        density = currentDensity.density * scaleFactor,
        fontScale = currentDensity.fontScale * scaleFactor
    )

    val typography = createViauraTypography(useSystemFont)
    val appFontFamily = getAppFontFamily(useSystemFont)
    val defaultTextStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = appFontFamily)

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        com.example.ui.components.LocalAppScale provides appScale,
        com.example.ui.components.LocalPanelTransparency provides glassBlurTransparency,
        com.example.ui.components.LocalLiquidGlass provides liquidGlassMode,
        com.example.ui.components.LocalLiquidGlassParams provides liquidGlassParams,
        com.example.ui.components.LocalLiquidGlassControls provides (liquidGlassMode && liquidGlassControls),
        com.example.ui.components.LocalSeekbarStyle provides seekbarStyle,
        com.example.ui.components.LocalAmoledBlack provides effectiveAmoled,
        LocalIsDarkTheme provides isDark,
        com.example.ui.i18n.LocalAppLanguage provides language,
        LocalIsSystemFontEnabled provides useSystemFont,
        LocalAppFontFamily provides appFontFamily,
        androidx.compose.material3.LocalTextStyle provides defaultTextStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

