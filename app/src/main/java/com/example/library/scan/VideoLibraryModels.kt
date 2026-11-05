package com.example.ui.screens

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ui.components.hasVideoPermission
import com.example.ui.components.hasAllFilesAccess
import com.example.util.CachedVideoMeta
import com.example.util.ExtractedMediaMetadata
import com.example.util.MediaMetadataExtractor
import com.example.util.VideoLibraryCache
import com.example.util.accessibleStorageRoots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val path: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateModified: Long,
    val isNew: Boolean = false,
    val resolution: String = "",
    val framerate: Double = 0.0,
    val hasSubtitles: Boolean = false,
    val subtitleFormats: List<String> = emptyList(),
    val watchedProgress: Float = 0.0f,
    val embeddedSubtitleFormats: List<String> = emptyList(),
    val externalSubtitleFormats: List<String> = emptyList(),
    val embeddedFontCount: Int = 0,
    val isOnline: Boolean = false,
    val thumbnailUrl: String? = null,
    val channelName: String? = null,
    val viewCountText: String? = null,
    /** An imported M3U/IPTV stream: [path] is the real stream URL that mpv opens directly (no YouTube resolving). */
    val isDirectStream: Boolean = false,
    /** A link pasted in the Network tab: [path] is the page/media URL, resolved right before playback. */
    val isNetworkStream: Boolean = false
) {
    val formattedSize: String
        get() = formatFileSize(sizeBytes)

    val durationFormatted: String
        get() = formatDuration(durationMs)

    val formattedFramerate: String
        get() {
            if (framerate <= 0.0) return ""
            val roundInt = Math.round(framerate).toDouble()
            return if (Math.abs(framerate - roundInt) < 0.0001) {
                "${roundInt.toLong()} FPS"
            } else {
                val df = java.text.DecimalFormat("#.###", java.text.DecimalFormatSymbols(java.util.Locale.US))
                "${df.format(framerate)} FPS"
            }
        }

    val extension: String
        get() {
            val ext = displayName.substringAfterLast('.', "")
            return if (ext.isNotEmpty()) ".${ext.lowercase()}" else ""
        }

    val extensionTag: String
        get() = displayName.substringAfterLast('.', "MP4").uppercase()

    /** A video inside a ZIP / TAR archive (a file on the phone or a link). It is online only because of where the archive is. */
    val isArchiveEntry: Boolean
        get() = path.startsWith("archive://")

    val nameWithoutExtension: String
        get() = if ((!isOnline || isArchiveEntry) && displayName.contains('.')) displayName.substringBeforeLast('.') else displayName

    val formattedDate: String
        get() {
            if (dateModified <= 0) return ""
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(dateModified))
        }

    val effectiveSubtitleFormats: List<String>
        get() = if (embeddedSubtitleFormats.isNotEmpty()) embeddedSubtitleFormats else if (subtitleFormats.isNotEmpty()) subtitleFormats else externalSubtitleFormats
}

data class VideoFolder(
    val id: String,
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val newVideosCount: Int,
    val isCustom: Boolean = false,
    val previewVideoUri: Uri? = null,
    val videos: List<VideoItem> = emptyList(),
    val subFolders: List<VideoFolder> = emptyList()
) {
    val formattedSize: String
        get() = formatFileSize(totalSizeBytes)

    val totalDurationFormatted: String
        get() {
            val totalMs = getAllVideos().sumOf { it.durationMs }
            return formatDuration(totalMs)
        }

    val maxResolution: String
        get() {
            val allVids = getAllVideos()
            return when {
                allVids.any { it.resolution.contains("4k", true) || it.displayName.contains("4k", true) || it.displayName.contains("2160", true) } -> "4K"
                allVids.any { it.resolution.contains("1080", true) || it.displayName.contains("1080", true) } -> "1080p"
                allVids.any { it.resolution.contains("720", true) || it.displayName.contains("720", true) } -> "720p"
                allVids.any { it.resolution.contains("480", true) || it.displayName.contains("480", true) } -> "480p"
                else -> "1080p"
            }
        }

    val formattedDate: String
        get() {
            val date = lastModifiedDate
            if (date <= 0) return ""
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(date))
        }

    val lastModifiedDate: Long
        get() = (videos.maxOfOrNull { it.dateModified } ?: 0L).coerceAtLeast(
            subFolders.maxOfOrNull { it.lastModifiedDate } ?: 0L
        )

    val allVideosCount: Int
        get() = videos.size + subFolders.sumOf { it.allVideosCount }

    /**
     * Returns all video items inside this folder
     */
    fun getAllVideos(): List<VideoItem> {
        val list = mutableListOf<VideoItem>()
        list.addAll(videos)
        for (sub in subFolders) {
            list.addAll(sub.getAllVideos())
        }
        return list
    }
}
