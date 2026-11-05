package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Centralized, global haptic feedback controller for Viaura.
 * Controlled by the user's "Haptic Feedback" appearance setting (Default: ON).
 */
object AppHaptics {
    @Volatile
    var isEnabled: Boolean = true

    /**
     * True while the video player screen is open. The app UI (bottom tabs, folder back, settings, sliders, switches)
     * vibrates, but the video player controls and gestures must stay silent, so every call is skipped meanwhile.
     */
    @Volatile
    var inVideoPlayer: Boolean = false

    private val blocked: Boolean get() = !isEnabled || inVideoPlayer

    /**
     * Performs subtle tactile tick haptic feedback (ideal for volume/brightness steps, seek scrubbing, slider dragging).
     */
    fun performTick(view: View? = null, haptic: HapticFeedback? = null) {
        if (blocked) return
        try {
            if (view != null) {
                val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.SEGMENT_TICK
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                }
                val performed = view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                if (!performed) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                }
            } else if (haptic != null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Performs gesture threshold / activation haptic feedback (e.g. gesture locked into seek, volume, brightness, or double tap seek).
     */
    fun performGestureThreshold(view: View? = null, haptic: HapticFeedback? = null) {
        if (blocked) return
        try {
            if (view != null) {
                val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                }
                val performed = view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                if (!performed) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
                }
            } else if (haptic != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Performs gesture start haptic feedback (e.g. beginning to drag seekbar, volume, brightness).
     */
    fun performGestureStart(view: View? = null, haptic: HapticFeedback? = null) {
        if (blocked) return
        try {
            if (view != null) {
                val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.GESTURE_START
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                }
                view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            } else if (haptic != null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Performs haptic feedback in Compose UI if globally enabled.
     */
    fun perform(haptic: HapticFeedback?, type: HapticFeedbackType = HapticFeedbackType.LongPress) {
        if (blocked || haptic == null) return
        try {
            haptic.performHapticFeedback(type)
        } catch (_: Throwable) {}
    }

    /**
     * Performs haptic feedback on an Android View if globally enabled.
     */
    fun perform(view: View?, feedbackConstant: Int = HapticFeedbackConstants.LONG_PRESS) {
        if (blocked || view == null) return
        try {
            view.performHapticFeedback(feedbackConstant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        } catch (_: Throwable) {}
    }

    /**
     * Light click for app UI navigation: bottom tab change, going back from a folder, opening settings, switching a settings section.
     */
    fun performClick(view: View?) {
        if (blocked || view == null) return
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONTEXT_CLICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            val performed = view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            if (!performed) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
        } catch (_: Throwable) {}
    }

    /**
     * Feedback for an ON / OFF switch: a confirm pulse when turned on, a softer reject pulse when turned off.
     */
    fun performToggle(view: View?, on: Boolean) {
        if (blocked || view == null) return
        try {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (on) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            val performed = view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            if (!performed) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            }
        } catch (_: Throwable) {}
    }
}
