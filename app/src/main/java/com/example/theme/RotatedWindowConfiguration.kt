package com.example.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf

/**
 * The window's real (rotated) configuration.
 *
 * MainActivity wraps the Home screen in a portrait-forced LocalConfiguration so the home UI keeps its portrait layout
 * even while the phone is held sideways. Screens drawn on top of Home that must follow the real rotation (the full-screen
 * audio player) read this instead of LocalConfiguration. Null outside that wrapper.
 */
val LocalRealConfiguration = compositionLocalOf<Configuration?> { null }
