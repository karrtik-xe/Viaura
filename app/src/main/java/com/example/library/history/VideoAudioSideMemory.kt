package com.example.util

import android.content.Context

/**
 * Remembers which side ("VIDEO" or "AUDIO") the user was on inside the Recents and Playlists sections, so
 * coming back to a section (even after switching tabs or restarting the app) opens the same side again.
 */
object MediaSideMemory {

    private const val PREFS_NAME = "media_side_memory_v1"
    const val RECENTS = "recents"
    const val PLAYLISTS = "playlists"

    fun get(context: Context, section: String): String {
        return try {
            val v = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(section, "VIDEO")
            if (v == "AUDIO") "AUDIO" else "VIDEO"
        } catch (_: Throwable) {
            "VIDEO"
        }
    }

    fun set(context: Context, section: String, side: String) {
        try {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(section, if (side == "AUDIO") "AUDIO" else "VIDEO").apply()
        } catch (_: Throwable) {
        }
    }
}
