package com.example.util.media

import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URI
import java.net.URL

/** One video rendition (EXT-X-STREAM-INF) of an HLS master playlist. */
data class HlsVariant(
    val bandwidth: Long,
    val width: Int,
    val height: Int,
    val frameRate: Double,
    /** Video codec only (e.g. avc1.640028), blank when the playlist does not say. */
    val videoCodec: String,
    val uri: String
) {
    /** "1080p", or the bitrate when the playlist has no resolution. */
    val label: String get() = if (height > 0) "${height}p" else bitrateText()

    /** "HD" / "SD" (and 2K / 4K) tag shown next to the quality, like the reference player. */
    val badge: String get() = when {
        height >= 2160 -> "4K"
        height >= 1440 -> "2K"
        height >= 720 -> "HD"
        else -> "SD"
    }

    fun bitrateText(): String = when {
        bandwidth >= 1_000_000L -> String.format(java.util.Locale.US, "%.1f Mbps", bandwidth / 1_000_000.0)
        bandwidth > 0L -> "${bandwidth / 1000} kbps"
        else -> ""
    }

    /** "4.1 Mbps · 1920x1080 · avc1.640028" */
    val info: String get() = listOf(
        bitrateText(),
        if (width > 0 && height > 0) "${width}x$height" else "",
        videoCodec
    ).filter { it.isNotBlank() }.joinToString(" · ")
}

/** One alternative rendition (EXT-X-MEDIA) of an HLS master playlist: another audio language, or a subtitle track. */
data class HlsRendition(
    /** "AUDIO" or "SUBTITLES". */
    val type: String,
    val groupId: String,
    val name: String,
    val language: String,
    val isDefault: Boolean,
    /** Absolute playlist URL; blank when the audio is part of the video playlist itself. */
    val uri: String
)

/** What a media playlist (the list of segments of ONE rendition) says: how long it is and where its segments are. */
class HlsMediaPlaylist(val durations: List<Double>, val segments: List<String>, val isVod: Boolean) {
    val totalSeconds: Double get() = durations.sum()
}
