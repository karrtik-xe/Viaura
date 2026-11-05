package com.example.util

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.player.SubtitleFontManager
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Unified metadata extraction result for a video file.
 */
data class ExtractedMediaMetadata(
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val resolution: String = "",
    val framerate: Double = 0.0,
    val embeddedSubtitleFormats: List<String> = emptyList(),
    val externalSubtitleFormats: List<String> = emptyList(),
    val embeddedFontCount: Int = 0,
    val embeddedFontNames: List<String> = emptyList(),
    val isCompleteScan: Boolean = true,
    val metadataSource: String = "CONTAINER"
) {
    val hasSubtitles: Boolean
        get() = embeddedSubtitleFormats.isNotEmpty() || externalSubtitleFormats.isNotEmpty()

    val effectiveSubtitleFormats: List<String>
        get() = if (embeddedSubtitleFormats.isNotEmpty()) embeddedSubtitleFormats else externalSubtitleFormats
}
