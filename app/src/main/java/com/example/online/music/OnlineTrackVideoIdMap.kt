package com.example.util

object OnlineTrackVideoIds {
    private val VIDEO_URL = Regex("(?:youtube\\.com/watch\\?(?:[^#]*&)?v=|music\\.youtube\\.com/watch\\?(?:[^#]*&)?v=|youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{11})")
    fun from(path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { VIDEO_URL.find(it)?.groupValues?.get(1) }
}
