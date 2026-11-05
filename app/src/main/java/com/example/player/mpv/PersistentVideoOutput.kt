package com.example.player

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Build
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView

/**
 * The picture surface mpv renders into, owned by the PLAYER instead of by whichever view shows it (API 29+).
 *
 * A SurfaceView's own surface is destroyed every time the view is moved to another parent (full player <-> mini
 * player <-> PiP), and mpv has to rebuild its video output for it: the stream is re-seeked, the audio is flushed (the
 * short cut) and the picture jumps back a little. Here mpv gets ONE surface for its whole life; the host SurfaceView
 * of the current screen only shows it by re-parenting this layer under its own SurfaceControl. Moving between the
 * screens is then just a re-parent: mpv never notices, so nothing stops, cuts or jumps.
 */
@SuppressLint("NewApi")
class PersistentVideoOutput private constructor(
    private val control: SurfaceControl,
    val surface: Surface
) {
    @Volatile var width: Int = INITIAL_W
        private set
    @Volatile var height: Int = INITIAL_H
        private set
    @Volatile private var host: SurfaceView? = null
    @Volatile private var released = false

    /** True while a visible host shows this output. */
    val isBound: Boolean get() = host != null

    fun bindTo(newHost: SurfaceView, w: Int, h: Int): Boolean {
        if (released || w <= 0 || h <= 0) return false
        return try {
            val parent = newHost.surfaceControl
            if (!parent.isValid) return false
            SurfaceControl.Transaction().use { t ->
                t.reparent(control, parent)
                t.setBufferSize(control, w, h)
                t.setLayer(control, 1)
                t.setVisibility(control, true)
                t.apply()
            }
            host = newHost
            width = w
            height = h
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun unbind(from: SurfaceView) {
        if (released || host !== from) return
        host = null
        try {
            SurfaceControl.Transaction().use { t ->
                t.reparent(control, null)
                t.apply()
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        if (released) return
        released = true
        host = null
        try {
            SurfaceControl.Transaction().use { t ->
                t.reparent(control, null)
                t.apply()
            }
        } catch (_: Throwable) {}
        try { surface.release() } catch (_: Throwable) {}
        try { control.release() } catch (_: Throwable) {}
    }

    companion object {
        private const val INITIAL_W = 1280
        private const val INITIAL_H = 720

        fun create(): PersistentVideoOutput? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            return try {
                val sc = SurfaceControl.Builder()
                    .setName("viaura-video-output")
                    .setBufferSize(INITIAL_W, INITIAL_H)
                    .setFormat(PixelFormat.RGBA_8888)
                    .build()
                val s = Surface(sc)
                if (!s.isValid) {
                    try { s.release() } catch (_: Throwable) {}
                    try { sc.release() } catch (_: Throwable) {}
                    null
                } else PersistentVideoOutput(sc, s)
            } catch (_: Throwable) {
                null
            }
        }
    }
}
