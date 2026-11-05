package com.example.util

import android.content.Context
import java.net.URI
import java.net.URLDecoder

/** A media URL accepted by the Local Network player. */
data class NetworkStream(
    val pageUrl: String,
    val title: String,
    val uploader: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Long = 0L,
    val videoUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val isDirect: Boolean = true,
    val isAudio: Boolean = false,
    /** A link of a website (YouTube, Instagram, TikTok ... 18000+ sites through yt-dlp): its streams, already checked. */
    val web: WebPlayback? = null
)

/** What the player needs to play a website link: qualities, audio tracks, chapters, subtitles and the request headers. */
data class WebPlayback(
    val playback: OnlineVideoPlayback,
    val headers: Map<String, String>,
    val subtitles: List<com.example.util.media.MediaSubtitle>
)
