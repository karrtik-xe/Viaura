package com.example.player

import androidx.compose.ui.graphics.Color
import com.example.ui.state.PlayerSettings

enum class SkipMarkerType(
    val defaultPrimaryLabel: String,
    val color: Color,
    val isOpening: Boolean = false,
    val isEnding: Boolean = false
) {
    OPENING("Skip Opening", Color(0xFFF59E0B), isOpening = true),
    ENDING("Skip Ending", Color(0xFF38BDF8), isEnding = true),
    PREVIEW("Skip Preview", Color(0xFF60A5FA), isEnding = true),
    RECAP("Skip Recap", Color(0xFFFBBF24)),
    PROLOGUE("Skip Intro", Color(0xFFF59E0B)),
    POST_CREDITS("Skip Post-Credits", Color(0xFF38BDF8))
}

data class ChapterSkipMarker(
    val chapterIndex: Int,
    val originalTitle: String,
    val displayButtonTitle: String,
    val secondaryTitle: String,
    val type: SkipMarkerType,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val color: Color = type.color
)
