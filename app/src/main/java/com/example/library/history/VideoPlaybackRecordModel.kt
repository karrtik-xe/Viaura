package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Data model for persisted video playback records.
 */
data class VideoPlaybackRecord(
    val videoId: Long = 0L,
    val path: String = "",
    val uriString: String = "",
    val title: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedPosition: String
        get() = formatTimestamp(positionMs)

    val formattedDuration: String
        get() = formatTimestamp(durationMs)

    val isFinished: Boolean
        get() = durationMs > 0L && (positionMs >= durationMs - 4000L || progressFraction >= 0.97f)

    companion object {
        fun formatTimestamp(timeMs: Long): String {
            if (timeMs <= 0L) return "00:00"
            val totalSec = timeMs / 1000
            val hours = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            return if (hours > 0) {
                String.format(java.util.Locale.US, "%d:%02d:%02d", hours, mins, secs)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
            }
        }
    }
}
