package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Ensures any Compose Dialog window expands strictly edge-to-edge across 100% of the display,
 * including behind the status bar, camera notch / punch-hole cutout, and navigation bar,
 * with fully transparent system window backgrounds.
 */
fun configureEdgeToEdgeDialogWindow(
    view: android.view.View,
    isLightStatusBars: Boolean = false,
    isLightNavBars: Boolean = false
) {
    fun applyConfig() {
        var window: android.view.Window? = (view as? DialogWindowProvider)?.window
        if (window == null) {
            var current: android.view.ViewParent? = view.parent
            while (current != null) {
                if (current is DialogWindowProvider) {
                    window = current.window
                    break
                }
                current = current.parent
            }
        }

        window?.let { win ->
            WindowCompat.setDecorFitsSystemWindows(win, false)
            win.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            // Some phones (black status / navigation bar, notch hidden, 3-button navigation, older Android / OEM skins) resolve
            // MATCH_PARENT to the app area only, while FLAG_LAYOUT_IN_SCREEN starts the window at the very top of the display.
            // The window is then shifted up and every sheet looks lifted, with the app showing below it. So the window is
            // pinned to the REAL display size, from (0,0). Skipped in split-screen / freeform / PiP where the window is smaller.
            pinDialogWindowToDisplay(win, view)
            win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            win.clearFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
            // The window must reach under the status / navigation bar, otherwise the strip behind the clock and
            // network icons is NOT dimmed / blurred and shows the plain app colour (a different shade).
            win.addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                win.attributes = win.attributes.apply {
                    layoutInDisplayCutoutMode =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        } else {
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        fitInsetsTypes = 0
                    }
                }
            }
            win.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            win.decorView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            win.decorView.setPadding(0, 0, 0, 0)
            win.decorView.fitsSystemWindows = false
            win.statusBarColor = android.graphics.Color.TRANSPARENT
            win.navigationBarColor = android.graphics.Color.TRANSPARENT
            win.setDimAmount(0.0f)
            // Liquid Glass: blur the app behind every dialog / sheet window (Android 12+, no-op when OFF)
            applyLiquidWindowBlur(win)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Prevent the system from drawing its own light/dark contrast
                // scrim behind the status/nav bars, which otherwise shows up as
                // a stray solid strip on top of our own transparent scrim,
                // breaking the seamless edge-to-edge look of the dialog.
                win.isStatusBarContrastEnforced = false
                win.isNavigationBarContrastEnforced = false
            }

            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.isAppearanceLightStatusBars = isLightStatusBars
            insetsController.isAppearanceLightNavigationBars = isLightNavBars
        }
    }

    applyConfig()
    view.post { applyConfig() }
    // Closed-loop check: whatever the phone's window manager does with the requested size, the window is measured and
    // corrected until it really covers the whole display (top edge at 0, bottom edge at the real screen bottom).
    view.post { installDialogFrameSync(view) }
}

/**
 * Call at the top of a ModalBottomSheet's content: gives that sheet window the same edge-to-edge / full display size setup as
 * every other sheet, so on phones with black bars or 3-button navigation the sheet reaches the real screen edge.
 */
@androidx.compose.runtime.Composable
fun EdgeToEdgeSheetWindowEffect() {
    val view = LocalView.current
    val dark = com.example.ui.theme.isAppInDarkTheme()
    DisposableEffect(view, dark) {
        configureEdgeToEdgeDialogWindow(view = view, isLightStatusBars = !dark, isLightNavBars = !dark)
        onDispose { }
    }
}

private fun findActivity(context: android.content.Context): android.app.Activity? {
    var c: android.content.Context? = context
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}

private fun pinDialogWindowToDisplay(win: android.view.Window, view: android.view.View) {
    try {
        val activity = findActivity(view.context)
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (activity.isInMultiWindowMode || activity.isInPictureInPictureMode)) return
        }
        val wm = view.context.getSystemService(android.content.Context.WINDOW_SERVICE) as? WindowManager ?: return
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            width = b.width()
            height = b.height()
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels
        }
        if (width <= 0 || height <= 0) return
        win.setGravity(android.view.Gravity.TOP or android.view.Gravity.START)
        val lp = win.attributes
        if (lp.x != 0 || lp.y != 0 || lp.width != width || lp.height != height) {
            lp.x = 0
            lp.y = 0
            lp.width = width
            lp.height = height
            win.attributes = lp
        }
    } catch (_: Throwable) {
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// Real-frame correction. Some phones (OEM skins, 3-button / gesture navigation, "full screen display" compat modes) place a
// dialog / sheet window a navigation-bar height too short or too high, even with MATCH_PARENT + LAYOUT_NO_LIMITS + exact pixel
// size. The sheet is then cut at the bottom and the plain app shows under it. Instead of trusting the request, the window is
// MEASURED (getLocationOnScreen + size) and the layout params are corrected until the measured frame equals the display.
// ---------------------------------------------------------------------------------------------------------------------

private val frameSyncedWindows = java.util.WeakHashMap<android.view.Window, Boolean>()

private fun findDialogWindow(view: android.view.View): android.view.Window? {
    (view as? DialogWindowProvider)?.window?.let { return it }
    var current: android.view.ViewParent? = view.parent
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent
    }
    return null
}

/** Real display size in px, or null in split-screen / freeform / PiP (where the window is legitimately smaller). */
private fun realDisplaySizeOrNull(view: android.view.View): IntArray? {
    return try {
        val activity = findActivity(view.context)
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            (activity.isInMultiWindowMode || activity.isInPictureInPictureMode)
        ) return null
        val wm = view.context.getSystemService(android.content.Context.WINDOW_SERVICE) as? WindowManager ?: return null
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            width = b.width()
            height = b.height()
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            width = dm.widthPixels
            height = dm.heightPixels
        }
        if (width <= 0 || height <= 0) null else intArrayOf(width, height)
    } catch (_: Throwable) {
        null
    }
}

private fun installDialogFrameSync(view: android.view.View) {
    try {
        val win = findDialogWindow(view) ?: return
        if (frameSyncedWindows.put(win, true) != null) return
        val decor = win.decorView
        var fixes = 0
        var lastKey = 0L
        val listener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            try {
                val display = realDisplaySizeOrNull(decor) ?: return@OnGlobalLayoutListener
                if (decor.width <= 0 || decor.height <= 0) return@OnGlobalLayoutListener
                // new target (rotation / fold / window resize): the correction budget starts again
                val key = display[0].toLong() * 100000L + display[1].toLong()
                if (key != lastKey) {
                    lastKey = key
                    fixes = 0
                }
                val loc = IntArray(2)
                decor.getLocationOnScreen(loc)
                val dx = 0 - loc[0]
                val dy = 0 - loc[1]
                val dw = display[0] - decor.width
                val dh = display[1] - decor.height
                if (kotlin.math.abs(dx) <= 1 && kotlin.math.abs(dy) <= 1 && kotlin.math.abs(dw) <= 1 && kotlin.math.abs(dh) <= 1) {
                    return@OnGlobalLayoutListener
                }
                if (fixes >= 6) return@OnGlobalLayoutListener
                fixes++
                val lp = win.attributes
                lp.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                lp.x = lp.x + dx
                lp.y = lp.y + dy
                val baseW = if (lp.width > 0) lp.width else decor.width
                val baseH = if (lp.height > 0) lp.height else decor.height
                // never grow past a sane bound if a phone refuses to honour the size
                lp.width = (baseW + dw).coerceIn(display[0] / 2, display[0] * 5 / 4)
                lp.height = (baseH + dh).coerceIn(display[1] / 2, display[1] * 5 / 4)
                win.attributes = lp
            } catch (_: Throwable) {
            }
        }
        decor.viewTreeObserver.addOnGlobalLayoutListener(listener)
        decor.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) {}
            override fun onViewDetachedFromWindow(v: android.view.View) {
                try {
                    if (v.viewTreeObserver.isAlive) v.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                } catch (_: Throwable) {
                }
                frameSyncedWindows.remove(win)
                v.removeOnAttachStateChangeListener(this)
            }
        })
        decor.requestLayout()
    } catch (_: Throwable) {
    }
}
