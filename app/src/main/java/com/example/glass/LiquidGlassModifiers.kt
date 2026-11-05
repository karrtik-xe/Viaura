package com.example.ui.components

import com.example.ui.theme.Neutral

import android.os.Build
import android.view.ViewParent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.ui.theme.isAppInDarkTheme

/*
 * ------------------------------------------------------------------------------------------------
 * LIQUID GLASS (Settings > Interface Appearance > "Liquid Glass")
 *
 * One central place for the whole look, so every surface of the app changes from here:
 *   - frosted translucent fill (tint + opacity follow the "UI Glass Blur & Opacity" slider, Dark / Light / System / AMOLED aware)
 *   - thin specular border: bright at the top-left, soft at the bottom-right (1 dp linear gradient)
 *   - round shapes are chosen by the caller (circle buttons, pill bars)
 *   - soft floating shadow
 *   - real backdrop blur (Android 12+, RenderEffect on a GraphicsLayer copy of the content behind the element);
 *     older Android versions and the places without a blur source simply keep the translucent fill.
 * When Liquid Glass is OFF every function here returns the modifier unchanged, so the app looks exactly as before.
 * ------------------------------------------------------------------------------------------------
 */

/** 0f (slider 10) .. 1f (slider 100). Low = more transparent, high = more opaque. */
private fun glassSliderFraction(transparency: Float): Float = ((transparency - 10f) / 90f).coerceIn(0f, 1f)

/**
 * Base tint of the glass. Dark = dark surface, AMOLED = pure black, Light = ice (0xFFDAE3E9),
 * System + phone in light mode = white (same rule as every other sheet / card of the app).
 */
fun liquidGlassBase(isDark: Boolean): Color = when {
    isDark -> if (ViauraSurfaceStyle.amoled) Color(0xFF000000) else Neutral.D1B2630
    ViauraSurfaceStyle.systemMode -> Neutral.F8FAFC
    else -> Neutral.DAE3E9
}

/** Fill of sheets / panels / dialogs (kept readable: never fully see-through). */
fun liquidGlassPanelFill(isDark: Boolean, transparency: Float): Color {
    val f = glassSliderFraction(transparency)
    val alpha = if (isDark) 0.34f + 0.60f * f else 0.58f + 0.38f * f
    return liquidGlassBase(isDark).copy(alpha = alpha.coerceIn(0f, 1f))
}

/** Fill of small floating elements (buttons, bars, chips, cards) that sit on top of blurred content. */
fun liquidGlassFill(isDark: Boolean, transparency: Float, tint: Float = 50f): Color {
    val f = glassSliderFraction(transparency)
    val alpha = (if (isDark) 0.22f + 0.66f * f else 0.40f + 0.52f * f) * (tint / 50f).coerceIn(0f, 2f)
    return liquidGlassBase(isDark).copy(alpha = alpha.coerceIn(0f, 1f))
}

/** Fill of the video player's own controls (they float over the video, so they are lighter than app surfaces). */
fun liquidGlassPlayerFill(isDark: Boolean, transparency: Float, tint: Float = 50f): Color {
    // The player controls float over a live (blurred) copy of the video, so the fill stays light: the "Glass Tint"
    // slider alone decides how strong the colour wash is (the video colours show through the rest).
    val t = (tint / 100f).coerceIn(0f, 1f)
    val base = if (isDark) (if (ViauraSurfaceStyle.amoled) Color(0xFF000000) else Color(0xFF0B1220)) else Color.White
    val alpha = if (isDark) 0.04f + 0.46f * t else 0.10f + 0.55f * t
    return base.copy(alpha = alpha.coerceIn(0f, 1f))
}

/** Specular highlight border: bright top-left, faint in the middle, medium bottom-right. */
fun liquidGlassBorderBrush(isDark: Boolean, scale: Float = 1f): Brush {
    val s = scale.coerceIn(0f, 2f)
    fun a(v: Float) = (v * s).coerceIn(0f, 1f)
    return when (ViauraSurfaceStyle.highlightStyle) {
        // Ambient: soft, even rim light all around
        2 -> Brush.linearGradient(
            0.0f to Color.White.copy(alpha = a(if (isDark) 0.38f else 0.60f)),
            0.5f to Color.White.copy(alpha = a(if (isDark) 0.20f else 0.38f)),
            1.0f to Color.White.copy(alpha = a(if (isDark) 0.38f else 0.60f))
        )
        // Plain: one flat thin white line
        3 -> Brush.linearGradient(
            0.0f to Color.White.copy(alpha = a(if (isDark) 0.45f else 0.70f)),
            1.0f to Color.White.copy(alpha = a(if (isDark) 0.45f else 0.70f))
        )
        // Component / Default: bright light from the corners, dim in between (specular)
        else -> Brush.linearGradient(
            0.00f to Color.White.copy(alpha = a(if (isDark) 0.80f else 0.95f)),
            0.30f to Color.White.copy(alpha = a(if (isDark) 0.24f else 0.50f)),
            0.70f to Color.White.copy(alpha = a(if (isDark) 0.07f else 0.20f)),
            1.00f to Color.White.copy(alpha = a(if (isDark) 0.42f else 0.72f))
        )
    }
}

/** Border width after the "Highlight Width" option. */
fun glassBorderWidth(base: Dp): Dp = base * (ViauraSurfaceStyle.highlightWidth / 100f).coerceIn(0f, 2f)

/**
 * The glass highlight border: specular line ("Highlight Strength" / "Width" / "Style") plus, when "Highlight Blur" is
 * above 0, a wider soft glow of the same line.
 */
fun Modifier.liquidGlassBorder(shape: Shape, isDark: Boolean, base: Dp = 1.dp): Modifier {
    val strength = (ViauraSurfaceStyle.highlightStrength / 100f).coerceIn(0f, 2f)
    val width = glassBorderWidth(base)
    val blur = (ViauraSurfaceStyle.highlightBlur / 100f).coerceIn(0f, 2f)
    var m = this
    if (blur > 0.05f && strength > 0f) {
        m = m.border(width * (1f + 1.6f * blur), liquidGlassBorderBrush(isDark, strength * 0.22f * blur), shape)
    }
    if (width.value > 0.01f && strength > 0f) {
        m = m.border(width, liquidGlassBorderBrush(isDark, strength), shape)
    }
    // Light theme: a white glass fill + white specular rim on a near-white app background vanishes completely
    // (title bar, bottom bar, video / folder cards). A faint slate hairline keeps the glass edge visible.
    if (!isDark) {
        m = m.border(0.8.dp, Neutral.S64748B.copy(alpha = 0.26f), shape)
    }
    return m
}

/** "Glass Highlight" slider as a multiplier for the specular border / sheen (70 = the default look). */
@Composable
fun liquidHighlightScale(): Float = (LocalLiquidGlassParams.current.highlightStrength / 100f).coerceIn(0f, 2f)

/** Soft glossy sheen drawn over the fill (light comes from the top-left). */
fun liquidGlassSheenBrush(isDark: Boolean): Brush = Brush.linearGradient(
    0.00f to Color.White.copy(alpha = if (isDark) 0.16f else 0.38f),
    0.45f to Color.White.copy(alpha = 0f),
    // Light theme: the lower-right fades to a soft slate shade (not white) so the glass has depth against a white page.
    1.00f to if (isDark) Color.White.copy(alpha = 0.06f) else Neutral.S64748B.copy(alpha = 0.09f)
)

/**
 * Soft shadow that is drawn ONLY outside the shape. Compose's Modifier.shadow() also darkens the inside of a translucent
 * surface (it is visible through the glass and shows up as a faint box shape), so every translucent Liquid Glass
 * surface uses this instead.
 */
fun Modifier.glassOuterShadow(shape: Shape, radius: Dp, color: Color, dy: Dp = 2.dp): Modifier = this

// ------------------------------------------------------------------------------------------------
// Backdrop blur (haze-style, no extra dependency)
// ------------------------------------------------------------------------------------------------

/** A recorded copy of the content that scrolls behind the glass elements. Glass elements draw it blurred. */
@Stable
class LiquidBackdropState(val layer: GraphicsLayer) {
    /** Window position of the source content (so a glass element can line the copy up exactly). */
    var origin: Offset by mutableStateOf(Offset.Zero)
}

/** Provided by the screen that owns the blur source (see [liquidBackdropSource]); glass elements outside the source use it for the real blur. */
val LocalLiquidBackdrop = compositionLocalOf<LiquidBackdropState?> { null }

/**
 * Marks the content that should appear blurred behind glass elements. IMPORTANT: glass elements must be siblings
 * of the content carrying this modifier (never children of it).
 */
fun Modifier.liquidBackdropSource(state: LiquidBackdropState?, enabled: Boolean): Modifier {
    if (!enabled || state == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    return this
        .onGloballyPositioned { state.origin = it.positionInWindow() }
        .drawWithContent {
            state.layer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(state.layer)
        }
}

// ------------------------------------------------------------------------------------------------
// Surfaces
// ------------------------------------------------------------------------------------------------

/**
 * Liquid Glass surface for floating elements (bottom bar, chips, round buttons, cards ...).
 * Returns [this] untouched while Liquid Glass is OFF, so call sites can append it to their existing chain
 * and keep their old background / border for the OFF case (see [liquidGlassOr]).
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    isDark: Boolean = isAppInDarkTheme(),
    transparency: Float = LocalPanelTransparency.current,
    elevation: Dp = 10.dp,
    backdropBlur: Boolean = true,
    fill: Color? = null,
    borderWidth: Dp = 1.dp
): Modifier {
    if (!LocalLiquidGlass.current) return this
    val backdrop = LocalLiquidBackdrop.current
    val useBlur = backdropBlur && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val params = LocalLiquidGlassParams.current
    val tint = fill ?: liquidGlassFill(isDark, transparency, params.tint)
    val hl = liquidHighlightScale()
    var origin by remember { mutableStateOf(Offset.Zero) }
    val blurLayer = rememberGraphicsLayer()

    var m: Modifier = this
    if (elevation > 0.dp) {
        m = m.glassOuterShadow(
            shape = shape,
            radius = elevation * 1.6f,
            color = Color.Black.copy(alpha = if (isDark) 0.40f else 0.20f)
        )
    }
    m = m.clip(shape)
    if (useBlur && backdrop != null) {
        m = m
            .onGloballyPositioned { origin = it.positionInWindow() }
            .drawWithCache {
                // the blur effect is rebuilt only when the size / blur setting changes, not on every frame
                val radius = (2f + 34f * params.blur / 200f).dp.toPx()
                blurLayer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                onDrawBehind {
                    if (size.width > 0f && size.height > 0f) {
                        blurLayer.record {
                            translate(left = backdrop.origin.x - origin.x, top = backdrop.origin.y - origin.y) {
                                drawLayer(backdrop.layer)
                            }
                        }
                        drawLayer(blurLayer)
                    }
                }
            }
    }
    return m
        .background(tint)
        .background(liquidGlassSheenBrush(isDark), shape)
        .liquidGlassBorder(shape, isDark, borderWidth)
}

/**
 * Convenience for call sites that already have an old look: [off] is applied exactly as before while Liquid Glass is
 * OFF, the Liquid Glass surface replaces it when ON.
 */
@Composable
fun Modifier.liquidGlassOr(
    shape: Shape,
    isDark: Boolean = isAppInDarkTheme(),
    transparency: Float = LocalPanelTransparency.current,
    elevation: Dp = 10.dp,
    backdropBlur: Boolean = true,
    off: Modifier.() -> Modifier
): Modifier {
    return if (LocalLiquidGlass.current) {
        this.liquidGlass(shape, isDark, transparency, elevation, backdropBlur)
    } else {
        this.off()
    }
}

/**
 * Thin floating toolbars (selection top / bottom bar, online quick-action bar).
 * Liquid Glass ON  -> a slightly lighter, more see-through glass than the cards (real blur behind where available,
 *                     soft sheen + specular edge; no gradient border / shadow box).
 * Liquid Glass OFF -> [off] is applied exactly as before.
 */
@Composable
fun Modifier.liquidGlassToolbarOr(
    shape: Shape,
    isDark: Boolean = isAppInDarkTheme(),
    off: Modifier.() -> Modifier
): Modifier {
    if (!LocalLiquidGlass.current) return this.off()
    val base = liquidGlassFill(isDark, LocalPanelTransparency.current, LocalLiquidGlassParams.current.tint)
    val fill = base.copy(alpha = (base.alpha * 0.78f).coerceIn(0.18f, 0.80f))
    return this.liquidGlass(shape, isDark, elevation = 12.dp, fill = fill, borderWidth = 1.2.dp)
}

/**
 * Background of a sheet / panel / dialog column: the central panel tint, plus (Liquid Glass) sheen and the specular
 * border. Replaces `.clip(shape).background(glassPanelColor(...))`.
 */
@Composable
fun Modifier.glassPanelSurface(shape: Shape, isDark: Boolean, transparency: Float): Modifier {
    val bg = glassPanelColor(isDark, transparency)
    val hl = liquidHighlightScale()
    return if (LocalLiquidGlass.current) {
        this
            .clip(shape)
            .background(bg)
            .background(liquidGlassSheenBrush(isDark))
            .liquidGlassBorder(shape, isDark, 1.dp)
    } else {
        this.clip(shape).background(bg)
    }
}

/** Border of a dialog card: the old border while Liquid Glass is OFF; sheen + specular gradient border when ON. */
@Composable
fun Modifier.glassCardBorder(width: Dp, offColor: Color, shape: Shape, isDark: Boolean): Modifier {
    return if (LocalLiquidGlass.current) {
        this.background(liquidGlassSheenBrush(isDark), shape).liquidGlassBorder(shape, isDark, 1.dp)
    } else {
        this.border(width, offColor, shape)
    }
}

/** Same as above for a brush border (gradient borders of the existing dialogs). */
@Composable
fun Modifier.glassCardBorder(width: Dp, offBrush: Brush, shape: Shape, isDark: Boolean): Modifier {
    return if (LocalLiquidGlass.current) {
        this.background(liquidGlassSheenBrush(isDark), shape).liquidGlassBorder(shape, isDark, 1.dp)
    } else {
        this.border(width, offBrush, shape)
    }
}

/**
 * Only the specular edge (for ModalBottomSheet content roots: the sheet's own container colour already comes from
 * glassPanelColor()). No-op while Liquid Glass is OFF.
 */
@Composable
fun Modifier.liquidGlassEdge(shape: Shape, isDark: Boolean): Modifier {
    return if (LocalLiquidGlass.current) {
        this.liquidGlassBorder(shape, isDark, 1.dp)
    } else {
        this
    }
}

/**
 * Blur radius (px) of the window behind a dialog / sheet. Follows the Liquid Glass "Blur" option AND the
 * "UI Glass Blur & Opacity" slider (10..100): the lower the slider, the weaker the glassy backdrop (10 = no blur).
 */
fun liquidWindowBlurRadius(blurParam: Float, slider: Float): Int {
    val sliderFactor = ((slider - 10f) / 90f).coerceIn(0f, 1f)
    return ((4f + 36f * blurParam / 200f) * sliderFactor).toInt().coerceAtLeast(0)
}

/** Applies the Liquid Glass blur-behind to a dialog window (Android 12+). Called by configureEdgeToEdgeDialogWindow for every dialog. */
fun applyLiquidWindowBlur(window: android.view.Window) {
    if (!ViauraSurfaceStyle.liquidGlass || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    try {
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        val attrs = window.attributes
        attrs.blurBehindRadius = liquidWindowBlurRadius(ViauraSurfaceStyle.glassBlur, ViauraSurfaceStyle.glassTransparency)
        window.attributes = attrs
    } catch (_: Throwable) {
    }
}

/**
 * Call inside a Dialog / ModalBottomSheet content: while Liquid Glass is ON (Android 12+) the whole screen behind the
 * sheet is blurred by the system, so the translucent sheet looks like frosted glass. The blur strength follows the
 * glass slider. Does nothing when OFF, on older Android versions, or when the device does not support window blur.
 */
@Composable
fun LiquidDialogBlur() {
    val liquid = LocalLiquidGlass.current
    val blurStrength = LocalLiquidGlassParams.current.blur
    val sliderValue = LocalPanelTransparency.current
    val view = LocalView.current
    // Keyed (NOT a SideEffect): the window attributes are an IPC to the window manager, so they must be written only
    // when Liquid Glass or the blur strength really changes - not on every recomposition / animation frame.
    DisposableEffect(liquid, blurStrength, sliderValue, view) {
        if (!liquid || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@DisposableEffect onDispose { }
        try {
            var provider: DialogWindowProvider? = view as? DialogWindowProvider
            var parent: ViewParent? = view.parent
            while (provider == null && parent != null) {
                provider = parent as? DialogWindowProvider
                parent = parent.parent
            }
            val window = provider?.window
            if (window != null) {
                val radius = liquidWindowBlurRadius(blurStrength, sliderValue)
                val attrs = window.attributes
                if (attrs.blurBehindRadius != radius || (attrs.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) == 0) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    attrs.blurBehindRadius = radius
                    window.attributes = attrs
                }
            }
        } catch (_: Throwable) {
        }
        onDispose { }
    }
}
