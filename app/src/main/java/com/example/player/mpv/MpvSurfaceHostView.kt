package com.example.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.PixelCopy
import android.view.SurfaceHolder
import `is`.xyz.mpv.BaseMPVView
import java.io.File

class VideoPlayerMpvView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : BaseMPVView(context, attrs) {

    var onSurfaceCreatedCallback: (() -> Unit)? = null
    var onSurfaceDestroyedCallback: (() -> Unit)? = null
    var onSurfaceChangedCallback: (() -> Unit)? = null

    /**
     * mpv's own, player-owned surface (API 29+). While it exists, this view is only a window onto it: the lib's own
     * surface callbacks are removed, so a re-created SurfaceView surface never touches mpv.
     */
    @Volatile var output: PersistentVideoOutput? = null
        private set

    /** Called after a host surface of this view started showing [output]. */
    var onOutputBound: (() -> Unit)? = null

    /** A picture can be copied / the player is live: the persistent output always, otherwise the view's own surface. */
    fun hasLiveSurface(): Boolean = output != null || holder.surface?.isValid == true

    /** Must be called right after [initialize]. Returns false when the old per-view surface has to be used. */
    fun enablePersistentOutput(): Boolean {
        if (output != null) return true
        val o = PersistentVideoOutput.create() ?: return false
        val libCallback = this as? SurfaceHolder.Callback
        return try {
            libCallback?.let { holder.removeCallback(it) }
            mpv.attachSurface(o.surface)
            mpv.setOptionString("force-window", "yes")
            mpv.setPropertyString("vo", "gpu")
            output = o
            true
        } catch (_: Throwable) {
            libCallback?.let { try { holder.addCallback(it) } catch (_: Throwable) {} }
            o.release()
            false
        }
    }

    fun releaseOutput() {
        val o = output ?: return
        output = null
        o.release()
    }

    init {
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                onSurfaceCreatedCallback?.invoke()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                val o = output
                if (o != null && o.bindTo(this@VideoPlayerMpvView, width, height)) {
                    try { mpv.setPropertyString("android-surface-size", "${width}x${height}") } catch (_: Throwable) {}
                    onOutputBound?.invoke()
                }
                onSurfaceChangedCallback?.invoke()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                output?.unbind(this@VideoPlayerMpvView)
                onSurfaceDestroyedCallback?.invoke()
            }
        })
    }

    override fun initOptions() {
        // libass (fontconfig provider) rebuilds its font database every time a subtitle track is selected or added,
        // on mpv's core thread. Without a persistent fontconfig cache that is a full font scan each time (a short
        // freeze of audio + video). A tiny fontconfig config gives it a cache folder so only the first scan is slow.
        setupFontconfigCache(context)
        // Needs to run before "sub-font" is set below: it finds the phone's default font family that SRT/VTT subtitles use.
        val fontsDir = SubtitleFontManager.initializeFontEnvironment(context)
        // yt-dlp bridge (ported from mpvRx): ytdl_hook paths must exist before init, but the hook stays OFF until
        // Viaura routes a link to yt-dlp (so NewPipe / HLS / direct streams are never hijacked by it).
        val self = java.lang.ref.WeakReference(this)
        com.example.ytdlp.YtdlpManager.mpvOptionSink = { name, value -> self.get()?.mpv?.setOptionString(name, value) }
        com.example.ytdlp.YtdlpManager.configureMpvHookBeforeInit(context)
        mpv.setOptionString("profile", "fast")
        // "fast" renders with bilinear scaling, which makes every 720p / 1080p video look soft on a phone screen.
        // Sharper upscaling (and a normal chroma scaler) costs very little and makes the real quality visible.
        mpv.setOptionString("scale", "spline36")
        mpv.setOptionString("cscale", "bilinear")
        mpv.setOptionString("vo", "gpu")
        mpv.setOptionString("gpu-context", "android")
        mpv.setOptionString("hwdec", "mediacodec")
        mpv.setOptionString("hwdec-codecs", "all")
        mpv.setOptionString("ao", "audiotrack,opensles")
        mpv.setOptionString("audio-channels", "auto-safe")
        mpv.setOptionString("video-sync", "audio")
        // Responsive 0.20s audio buffer ensures instantaneous audio setting updates (<50ms) while maintaining rock-solid playback
        mpv.setOptionString("audio-buffer", "0.20")
        mpv.setOptionString("audio-stream-silence", "yes")
        mpv.setOptionString("audio-pitch-correction", "yes")
        mpv.setOptionString("framedrop", "vo")
        mpv.setOptionString("hr-seek-framedrop", "yes")
        // mpv re-seeks to the CURRENT position whenever the video output / a track is re-selected. With the default
        // ("default") that internal seek lands on the previous keyframe first (audio jumps back, then forward). "yes"
        // makes every such seek frame-exact, so the position stays where it was.
        mpv.setOptionString("hr-seek", "yes")
        mpv.setOptionString("vd-lavc-threads", "4")
        mpv.setOptionString("vd-lavc-dr", "yes")
        mpv.setOptionString("keepaspect", "yes")
        mpv.setOptionString("force-window", "yes")
        // Keep the file loaded at its end. Without this mpv UNLOADS the file at EOF: the player then
        // can't report "finished" reliably (autoplay-next missed) and a seek back from the end
        // does nothing because there is no file left to play.
        mpv.setOptionString("keep-open", "yes")
        mpv.setOptionString("input-default-bindings", "yes")
        mpv.setOptionString("volume-max", "200")
        mpv.setOptionString("audio-pitch-correction", "yes")

        // Subtitle & ASS Aegisub Rendering Options (libass full support for all subtitle formats: SRT, VTT, SUB, SSA, ASS, PGS/SUP)
        mpv.setOptionString("sid", "no")
        mpv.setOptionString("secondary-sid", "no")
        mpv.setOptionString("sub-visibility", "yes")
        mpv.setOptionString("sub-ass", "yes")
        // Use libass/fontconfig so missing ASS fonts and missing individual glyphs can
        // fall back to the bundled universal subtitle font instead of disappearing.
        mpv.setOptionString("sub-font-provider", "fontconfig")
        mpv.setOptionString("sub-ass-override", "no")
        mpv.setOptionString("sub-ass-force-margins", "no")
        mpv.setOptionString("sub-use-margins", "yes")
        mpv.setOptionString("sub-margin-y", "0")
        mpv.setOptionString("sub-margin-x", "0")
        mpv.setOptionString("sub-pos", "100")
        mpv.setOptionString("blend-subtitles", "no")
        // Bundled multi-script fallback font (Devanagari + most other scripts),
        // not a generic name that may not resolve to anything with real glyph
        // coverage on a given device.
        mpv.setOptionString("sub-font", SubtitleFontManager.BUNDLED_FALLBACK_FONT_FAMILY)
        mpv.setOptionString("sub-font-size", "52")
        mpv.setOptionString("sub-color", "1.0/1.0/1.0/1.0")
        mpv.setOptionString("sub-border-color", "0.0/0.0/0.0/1.0")
        mpv.setOptionString("sub-border-size", "3.0")
        mpv.setOptionString("sub-shadow-offset", "1.5")
        mpv.setOptionString("sub-shadow-color", "0.0/0.0/0.0/0.75")
        mpv.setOptionString("sub-scale-with-window", "yes")
        mpv.setOptionString("sub-ass-scale-with-window", "no")
        mpv.setOptionString("sub-ass-hinting", "none")
        mpv.setOptionString("sub-ass-shaper", "complex")
        mpv.setOptionString("sub-ass-vsfilter-aspect-compat", "yes")
        mpv.setOptionString("sub-ass-vsfilter-blur-compat", "yes")
        mpv.setOptionString("sub-ass-vsfilter-color-compat", "full")
        mpv.setOptionString("sub-fix-timing", "no")
        mpv.setOptionString("sub-codepage", "auto:utf-8")
        mpv.setOptionString("sub-auto", "all")
        mpv.setOptionString("embeddedfonts", "yes")

        // https streams: libmpv cannot read Android's system CA folder, so every certificate check failed and the
        // stream stayed on "Buffering" without downloading anything. The phone's own trust store is handed to mpv.
        try {
            com.example.util.TlsCaBundle.path(context)?.let { mpv.setOptionString("tls-ca-file", it) }
        } catch (_: Throwable) {}

        // Custom Fonts Directory & Pre-initialized Fallback Fonts (libass font loader)
        mpv.setOptionString("sub-fonts-dir", fontsDir.absolutePath)
    }

    override fun postInitOptions() {
        mpv.setOptionString("save-position-on-quit", "no")
    }

    override fun observeProperties() {
        // No-op, polling is used for state updates
    }

    /**
     * Liquid Glass: copies the current video picture, scaled down to the size of [dest], for the blurred glass behind the
     * player controls. PixelCopy scales the surface buffer to the destination bitmap, so this stays cheap.
     */
    fun captureGlassSample(dest: Bitmap, onResult: (Boolean) -> Unit) {
        val o = output
        val surface = o?.surface ?: holder.surface
        if (surface == null || !surface.isValid || width <= 0 || height <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onResult(false)
            return
        }
        try {
            PixelCopy.request(
                surface,
                dest,
                { copyResult -> onResult(copyResult == PixelCopy.SUCCESS) },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Throwable) {
            onResult(false)
        }
    }

    /**
     * Ambience Mode: copies only [srcRect] (the real picture area of the surface, without the black bars) into [dest],
     * scaled to the size of [dest]. Colours at the picture edges are therefore exact and never mixed with black.
     */
    fun captureVideoRegion(dest: Bitmap, srcRect: android.graphics.Rect, onResult: (Boolean) -> Unit) {
        val o = output
        val surface = o?.surface ?: holder.surface
        val sw = o?.width ?: width
        val sh = o?.height ?: height
        if (surface == null || !surface.isValid || sw <= 0 || sh <= 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onResult(false)
            return
        }
        val r = android.graphics.Rect(
            srcRect.left.coerceIn(0, sw - 1),
            srcRect.top.coerceIn(0, sh - 1),
            srcRect.right.coerceIn(1, sw),
            srcRect.bottom.coerceIn(1, sh)
        )
        if (r.width() < 2 || r.height() < 2) {
            onResult(false)
            return
        }
        try {
            PixelCopy.request(
                surface,
                r,
                dest,
                { copyResult -> onResult(copyResult == PixelCopy.SUCCESS) },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Throwable) {
            onResult(false)
        }
    }

    /**
     * Captures the live hardware surface buffer via PixelCopy.
     * Used for seamless frame freeze during screen orientation rotation.
     */
    fun captureSurfaceFrame(onResult: (Bitmap?) -> Unit) {
        val o = output
        val surface = o?.surface ?: holder.surface
        val sw = o?.width ?: width
        val sh = o?.height ?: height
        if (surface == null || !surface.isValid || sw <= 0 || sh <= 0) {
            onResult(null)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bitmap = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    surface,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            onResult(bitmap)
                        } else {
                            onResult(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Throwable) {
                onResult(null)
            }
        } else {
            onResult(null)
        }
    }
}

/**
 * Points fontconfig (used by libass for subtitle fonts) to a minimal config that has a writable cache folder.
 * It keeps the same font folders the default setup already used, so no font becomes unavailable; if anything
 * fails, fontconfig simply keeps its built-in defaults.
 */
private fun setupFontconfigCache(context: Context) {
    try {
        val dir = File(context.filesDir, "fontconfig").apply { mkdirs() }
        val cache = File(context.cacheDir, "fontconfig-cache").apply { mkdirs() }
        val conf = File(dir, "fonts.conf")
        val xml = buildString {
            append("<?xml version=\"1.0\"?>\n<fontconfig>\n")
            for (d in listOf("/system/fonts", "/system/font", "/product/fonts", "/system_ext/fonts", "/vendor/fonts", "/data/fonts")) {
                append("  <dir>").append(d).append("</dir>\n")
            }
            append("  <cachedir>").append(cache.absolutePath).append("</cachedir>\n</fontconfig>\n")
        }
        if (!conf.exists() || conf.readText() != xml) conf.writeText(xml)
        android.system.Os.setenv("FONTCONFIG_PATH", dir.absolutePath, true)
        android.system.Os.setenv("FONTCONFIG_FILE", conf.absolutePath, true)
    } catch (_: Throwable) {
    }
}
