package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/*
 * ------------------------------------------------------------------------------------------------
 * LIQUID GLASS FOR THE VIDEO PLAYER CONTROLS
 *
 * mpv draws the video on a native SurfaceView, and Compose cannot blur / refract what a SurfaceView shows. So while
 * Liquid Glass is ON and the controls are visible, a small copy of the video (about 1/6 size, PixelCopy, ~15 fps) is
 * kept in [VideoGlassSource]. Every glass control draws that copy behind itself - lined up exactly with the real video -
 * through a RenderEffect chain: saturation boost -> blur -> lens refraction at the edges (Android 13+).
 * The values come from Settings > Interface Appearance > Liquid Glass sliders. Without a sample (older Android,
 * Liquid Glass OFF, controls hidden) nothing is drawn here and the plain translucent fill is used.
 * ------------------------------------------------------------------------------------------------
 */

/** Latest low-resolution copy of the video + where the video sits in the window (px). */
@Stable
class VideoGlassSource {
    var frame: ImageBitmap? by mutableStateOf(null)
    var bounds: Rect by mutableStateOf(Rect.Zero)

    /**
     * Ambience Mode: width / height of the real picture while the glow bars are drawn on top of the black bars (0 = Ambience off).
     * The captured video copy still contains those black bars, so the glass would show them (black corner buttons and black
     * volume / brightness panels). With this set, the glass draws the picture itself plus the same mirrored edge colours the glow uses.
     */
    var ambienceDar: Float by mutableStateOf(0f)

    /** Layout of the native video view; lets [refreshBounds] follow zoom / pan animations (they do not re-layout). */
    var coordinates: androidx.compose.ui.layout.LayoutCoordinates? = null

    /**
     * Where the video view really sits in the window, INCLUDING the zoom / fill scale of its parents and WITHOUT being
     * clipped to the screen. boundsInWindow() clips to the screen, so with "Fill" zoom the sample (which still contains
     * the black side bars) was stretched over the visible area and the corner buttons showed those black bars.
     */
    fun refreshBounds() {
        val c = coordinates ?: return
        if (!c.isAttached) return
        val tl = c.localToWindow(Offset.Zero)
        val br = c.localToWindow(Offset(c.size.width.toFloat(), c.size.height.toFloat()))
        val r = Rect(minOf(tl.x, br.x), minOf(tl.y, br.y), maxOf(tl.x, br.x), maxOf(tl.y, br.y))
        if (r != bounds) bounds = r
    }
}

val LocalVideoGlassSource = staticCompositionLocalOf<VideoGlassSource?> { null }

/**
 * Keeps [source] fresh while [active]. [capture] copies the current video picture into the given bitmap (PixelCopy
 * scales it down to the bitmap size) and reports success.
 */
@Composable
fun VideoGlassSampler(
    source: VideoGlassSource?,
    active: Boolean,
    playing: Boolean,
    capture: ((Bitmap, (Boolean) -> Unit) -> Unit)?,
    /** true while the controls fade in / out: one picture is taken, then no more until the animation is over (no GPU work during it). */
    settling: Boolean = false
) {
    if (source == null || capture == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val settlingNow by androidx.compose.runtime.rememberUpdatedState(settling)
    LaunchedEffect(source, active, playing) {
        if (!active) return@LaunchedEffect
        var bitmaps = arrayOfNulls<Bitmap>(2)
        var index = 0
        var tookFirst = false
        while (isActive) {
            if (tookFirst && settlingNow) {
                delay(50L)
                continue
            }
            source.refreshBounds()
            val b = source.bounds
            if (b.width > 8f && b.height > 8f) {
                // the glass only shows a blurred copy, so a smaller sample (1/8) looks the same and costs far less
                val w = (b.width / 8f).toInt().coerceIn(48, 360)
                val h = (w * (b.height / b.width)).toInt().coerceAtLeast(27)
                var bmp = bitmaps[index]
                if (bmp == null || bmp.width != w || bmp.height != h) {
                    bmp = try {
                        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    } catch (_: Throwable) {
                        null
                    }
                    bitmaps[index] = bmp
                }
                if (bmp != null) {
                    val target: Bitmap = bmp
                    val ok = suspendCancellableCoroutine<Boolean> { cont ->
                        try {
                            capture(target) { result -> if (cont.isActive) cont.resume(result) }
                        } catch (_: Throwable) {
                            if (cont.isActive) cont.resume(false)
                        }
                    }
                    if (ok) {
                        source.frame = target.asImageBitmap()
                        index = 1 - index
                        tookFirst = true
                    }
                }
            }
            // ~12 fps while playing; a paused picture does not change, so it is refreshed only rarely
            delay(if (playing) 180L else 600L)
        }
    }
}

private const val LENS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float height;
uniform float amount;
uniform float ca;
uniform float depth;
half4 main(float2 coord) {
    float2 c = size * 0.5;
    float2 pc = coord - c;
    float2 q = abs(pc) - (c - float2(radius));
    float sd = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float d = -sd;
    if (d >= height) {
        return content.eval(coord);
    }
    float t = clamp(1.0 - max(d, 0.0) / height, 0.0, 1.0);
    float2 s = float2(pc.x < 0.0 ? -1.0 : 1.0, pc.y < 0.0 ? -1.0 : 1.0);
    float2 g;
    if (q.x > 0.0 && q.y > 0.0) {
        g = normalize(q) * s;
    } else if (q.x > q.y) {
        g = float2(s.x, 0.0);
    } else {
        g = float2(0.0, s.y);
    }
    float shift = amount * (depth > 0.5 ? t * t * (0.6 + 0.8 * t) : t * t);
    float2 src = coord - g * shift;
    half4 cr = content.eval(src - g * shift * ca);
    half4 cg = content.eval(src);
    half4 cb = content.eval(src + g * shift * ca);
    half4 outc = half4(cr.r, cg.g, cb.b, cg.a);
    if (depth > 0.5) {
        outc.rgb *= half(1.0 - 0.16 * t);
    }
    return outc;
}
"""

/**
 * Draws the blurred / saturated / refracted live video behind a player glass control. Must be applied AFTER
 * `.clip(shape)` and BEFORE the translucent fill. Does nothing when there is no video sample.
 */
@Composable
fun Modifier.videoGlassBackdrop(shape: Shape): Modifier {
    val source = LocalVideoGlassSource.current ?: return this
    if (!LocalLiquidGlass.current || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val params = LocalLiquidGlassParams.current
    val layer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    // one shared shader: compiling the AGSL for every control each time the controls fade in caused a stutter on touch
    val lens = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) sharedLensShader else null

    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawWithCache {
            val w = size.width
            val h = size.height
            var effect: RenderEffect? = null
            try {
                if (w > 0f && h > 0f) {
                    var e: RenderEffect? = null
                    // 1) colour: vibrancy + saturation + contrast + brightness
                    val sat = (params.saturation / 100f).coerceIn(0f, 2f) * (if (params.vibrancy) 1.35f else 1f)
                    val contrast = (params.contrast / 100f).coerceIn(0f, 2f)
                    val bright = (params.brightness / 100f).coerceIn(-1f, 1f) * 255f
                    if (kotlin.math.abs(sat - 1f) > 0.02f || kotlin.math.abs(contrast - 1f) > 0.02f || kotlin.math.abs(bright) > 1f) {
                        val cm = ColorMatrix().apply { setSaturation(sat) }
                        val off = (1f - contrast) * 128f + bright
                        cm.postConcat(
                            ColorMatrix(
                                floatArrayOf(
                                    contrast, 0f, 0f, 0f, off,
                                    0f, contrast, 0f, 0f, off,
                                    0f, 0f, contrast, 0f, off,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                        e = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
                    }
                    // 2) blur
                    val blurPx = 19f * (params.blur / 100f) * density
                    if (blurPx >= 0.5f) {
                        val blurEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                        e = if (e == null) blurEffect else RenderEffect.createChainEffect(blurEffect, e)
                    }
                    // 3) lens refraction (Android 13+)
                    if (lens != null && params.refractionHeight > 0f && params.refractionAmount > 0f &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val corner = if (outline is Outline.Rounded) outline.roundRect.topLeftCornerRadius.x else 0f
                        val lensHeight = (12f * density * params.refractionHeight / 100f).coerceAtMost(minOf(w, h) * 0.5f)
                        val lensAmount = (20f * density * params.refractionAmount / 100f).coerceAtMost(minOf(w, h) * 0.45f)
                        lens.setFloatUniform("size", w, h)
                        lens.setFloatUniform("radius", corner.coerceAtMost(minOf(w, h) * 0.5f))
                        lens.setFloatUniform("height", lensHeight)
                        lens.setFloatUniform("amount", lensAmount)
                        lens.setFloatUniform("ca", if (params.chromaticAberration) 0.08f else 0f)
                        lens.setFloatUniform("depth", if (params.depthEffect) 1f else 0f)
                        val lensEffect = RenderEffect.createRuntimeShaderEffect(lens, "content")
                        e = if (e == null) lensEffect else RenderEffect.createChainEffect(lensEffect, e)
                    }
                    effect = e
                }
            } catch (_: Throwable) {
                effect = null
            }
            // no effect at all (everything 0) = just draw the sharp video copy
            layer.renderEffect = effect?.asComposeRenderEffect()

            onDrawBehind {
                val frame = source.frame
                val b = source.bounds
                if (frame == null || b.width <= 1f || b.height <= 1f) return@onDrawBehind
                layer.alpha = (params.opacity / 100f).coerceIn(0f, 1f)
                val dar = source.ambienceDar
                layer.record {
                    translate(left = b.left - origin.x, top = b.top - origin.y) {
                        val bwPx = b.width
                        val bhPx = b.height
                        if (dar > 0.05f) {
                            // ---- Ambience: picture in the middle, mirrored edge colours in the bars (same idea as the glow behind the controls)
                            val fitW: Float
                            val fitH: Float
                            if (bwPx / bhPx > dar) { fitH = bhPx; fitW = bhPx * dar } else { fitW = bwPx; fitH = bwPx / dar }
                            val pl = (bwPx - fitW) / 2f
                            val pt = (bhPx - fitH) / 2f
                            val fw = frame.width
                            val fh = frame.height
                            // picture rectangle inside the sample (1 px inside, so no black seam pixel is picked up)
                            val sx = (pl / bwPx * fw).toInt() + 1
                            val sy = (pt / bhPx * fh).toInt() + 1
                            val sw = ((fitW / bwPx * fw).toInt() - 2).coerceAtLeast(2)
                            val sh = ((fitH / bhPx * fh).toInt() - 2).coerceAtLeast(2)
                            drawImage(
                                image = frame,
                                srcOffset = IntOffset(sx, sy),
                                srcSize = IntSize(sw, sh),
                                dstOffset = IntOffset(pl.toInt(), pt.toInt()),
                                dstSize = IntSize(fitW.toInt().coerceAtLeast(1), fitH.toInt().coerceAtLeast(1)),
                                filterQuality = FilterQuality.Medium
                            )
                            val sideBars = fitW < bwPx - 1.5f
                            for (bar in 0..1) {
                                val first = bar == 0
                                val x: Float
                                val y: Float
                                val w: Float
                                val h: Float
                                if (sideBars) {
                                    w = if (first) pl else bwPx - pl - fitW
                                    x = if (first) 0f else pl + fitW
                                    y = 0f; h = bhPx
                                } else {
                                    h = if (first) pt else bhPx - pt - fitH
                                    y = if (first) 0f else pt + fitH
                                    x = 0f; w = bwPx
                                }
                                if (w < 1.5f || h < 1.5f) continue
                                val srcOff: IntOffset
                                val srcSz: IntSize
                                if (sideBars) {
                                    val ssw = (w / fitW * sw).toInt().coerceIn(2, (sw / 2).coerceAtLeast(2))
                                    srcOff = IntOffset(if (first) sx else sx + sw - ssw, sy)
                                    srcSz = IntSize(ssw, sh)
                                } else {
                                    val ssh = (h / fitH * sh).toInt().coerceIn(2, (sh / 2).coerceAtLeast(2))
                                    srcOff = IntOffset(sx, if (first) sy else sy + sh - ssh)
                                    srcSz = IntSize(sw, ssh)
                                }
                                withTransform({
                                    if (sideBars) scale(-1f, 1f, pivot = Offset(x + w / 2f, y + h / 2f))
                                    else scale(1f, -1f, pivot = Offset(x + w / 2f, y + h / 2f))
                                }) {
                                    drawImage(
                                        image = frame,
                                        srcOffset = srcOff,
                                        srcSize = srcSz,
                                        dstOffset = IntOffset(x.toInt(), y.toInt()),
                                        dstSize = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1)),
                                        filterQuality = FilterQuality.Medium
                                    )
                                }
                            }
                        } else {
                            drawImage(
                                image = frame,
                                srcOffset = IntOffset.Zero,
                                srcSize = IntSize(frame.width, frame.height),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(bwPx.toInt().coerceAtLeast(1), bhPx.toInt().coerceAtLeast(1)),
                                filterQuality = FilterQuality.Medium
                            )
                        }
                    }
                }
                drawLayer(layer)
            }
        }
}

private val sharedLensShader: RuntimeShader? by lazy(LazyThreadSafetyMode.NONE) { createLensShader() }

private fun createLensShader(): RuntimeShader? {
    return try {
        RuntimeShader(LENS_AGSL)
    } catch (_: Throwable) {
        null
    }
}
