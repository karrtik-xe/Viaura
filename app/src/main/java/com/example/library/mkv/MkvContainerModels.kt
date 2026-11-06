package com.example.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

// MARK: - Data Models for MKV Track Manager

enum class MkvTrackType(val displayName: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    SUBTITLE("Subtitle"),
    ATTACHMENT("Attachment"),
    UNKNOWN("Unknown")
}

data class MkvTrackInfo(
    val id: Int,
    var trackNumber: Long,
    val trackUid: Long = 0L,
    val trackType: MkvTrackType,
    var name: String = "",
    var language: String = "und",
    var codecId: String = "",
    var codecName: String = "",
    var isEnabled: Boolean = true,
    var isDefault: Boolean = false,
    var isForced: Boolean = false,
    // Video specs
    var pixelWidth: Int = 0,
    var pixelHeight: Int = 0,
    var displayWidth: Int = 0,
    var displayHeight: Int = 0,
    var displayUnit: Int = 0,
    var frameRate: Float = 0f,
    var defaultDurationNs: Long = 0L,
    var flagLacing: Boolean = true,
    // Audio specs
    var channels: Int = 0,
    var sampleRate: Int = 0,
    var bitDepth: Int = 0,
    // Advanced video / stream statistics
    var bitrateBps: Long = 0L,
    var durationMs: Long = 0L,
    var colorSpace: String = "",
    var hdrFormat: String = "",
    var aspectRatio: String = "",
    // Detailed codec profile info (parsed from CodecPrivate, mirrors MediaInfo's
    // "Format profile" / "Format level" / "Format tier" / chroma rows)
    var formatProfile: String = "",
    var formatLevel: String = "",
    var formatTier: String = "",
    var chromaSubsampling: String = "",
    var frameRateMode: String = "",
    // Exact compressed bytes stored for this track, measured from the real Cluster
    // blocks (not estimated) - mirrors MediaInfo's "Stream size" row.
    var streamSizeBytes: Long = 0L,
    // Number of subtitle dialogue/cue blocks found for this track - mirrors
    // MediaInfo's "Count of elements" row.
    var countOfElements: Int = 0,
    // Audio-only: container-declared playback delay relative to video (ms).
    var delayRelativeToVideoMs: Long = 0L,
    // Subtitle / Codec specifics
    var codecPrivate: ByteArray = ByteArray(0),
    var isExternalAdded: Boolean = false,
    var externalSourceUri: Uri? = null,
    var externalSourceContent: String? = null,
    var originalTrackIndex: Int = id,
    var originalTrackNumber: Long = trackNumber,
    // Per-Track Settings
    var syncOffsetMs: Long = 0L,
    var encoder: String = "None",
    var isOriginal: Boolean = false,
    var isCommentary: Boolean = false,
    var isDescriptions: Boolean = false,
    var isHearingImpaired: Boolean = false,
    var isVisualImpaired: Boolean = false,
    var metadataTags: Map<String, String> = emptyMap()
) {
    val displayResolution: String
        get() = if (pixelWidth > 0 && pixelHeight > 0) "${pixelWidth}x${pixelHeight}" else ""

    val displayAudioSpec: String
        get() = buildString {
            if (channels > 0) {
                append(if (channels == 1) "Mono" else if (channels == 2) "Stereo" else "${channels}.0")
            }
            if (sampleRate > 0) {
                if (isNotEmpty()) append(" • ")
                append("${sampleRate / 1000} kHz")
            }
        }

    val displayCodec: String
        get() {
            if (codecName.isNotBlank()) return codecName
            return when {
                codecId.contains("AVC", true) || codecId.contains("H264", true) -> "H.264 / AVC"
                codecId.contains("HEVC", true) || codecId.contains("H265", true) -> "H.265 / HEVC"
                codecId.contains("VP9", true) -> "VP9"
                codecId.contains("AV1", true) -> "AV1"
                codecId.contains("AAC", true) -> "AAC"
                codecId.contains("OPUS", true) -> "Opus"
                codecId.contains("FLAC", true) -> "FLAC"
                codecId.contains("EAC3", true) -> "Dolby Digital Plus (E-AC-3)"
                codecId.contains("AC3", true) -> "Dolby Digital (AC-3)"
                codecId.contains("DTS", true) -> "DTS"
                codecId.contains("ASS", true) -> "SubStation Alpha (.ass)"
                codecId.contains("SSA", true) -> "SubStation Alpha (.ssa)"
                codecId.contains("UTF8", true) -> "SubRip (.srt)"
                codecId.contains("WEBVTT", true) -> "WebVTT (.vtt)"
                codecId.contains("PGS", true) || codecId.contains("HDMV", true) -> "HDMV PGS"
                codecId.contains("VOBSUB", true) -> "VobSub"
                codecId.isNotBlank() -> codecId.substringAfterLast("/")
                else -> trackType.displayName
            }
        }

    /** Long-form codec description, e.g. "High Efficiency Video Coding" for HEVC. */
    val formatInfo: String
        get() = when {
            codecId.contains("HEVC", true) || codecId.contains("H265", true) -> "High Efficiency Video Coding"
            codecId.contains("AVC", true) || codecId.contains("H264", true) -> "Advanced Video Coding"
            codecId.contains("VP9", true) -> "Google / On2 VP9"
            codecId.contains("AV1", true) -> "AOMedia Video 1"
            codecId.contains("AAC", true) -> "Advanced Audio Coding"
            codecId.contains("OPUS", true) -> "Interactive Audio Lossy Compression"
            codecId.contains("FLAC", true) -> "Free Lossless Audio Codec"
            codecId.contains("EAC3", true) -> "Enhanced AC-3"
            codecId.contains("AC3", true) -> "Audio Coding 3"
            codecId.contains("DTS", true) -> "Digital Theater System"
            codecId.contains("PCM", true) -> "Pulse Code Modulation"
            codecId.contains("VORBIS", true) -> "Xiph.Org Vorbis"
            codecId.contains("ASS", true) || codecId.contains("SSA", true) -> "Advanced SubStation Alpha"
            codecId.contains("UTF8", true) -> "SubRip Text"
            codecId.contains("WEBVTT", true) -> "Web Video Text Tracks"
            codecId.contains("PGS", true) || codecId.contains("HDMV", true) -> "Presentation Graphic Stream"
            codecId.contains("VOBSUB", true) -> "DVD Subtitle"
            else -> ""
        }

    /** Lossy vs Lossless, matching MediaInfo's "Compression mode" row. */
    val compressionMode: String
        get() = when {
            trackType == MkvTrackType.SUBTITLE -> "Lossless"
            codecId.contains("FLAC", true) || codecId.contains("PCM", true) ||
                codecId.contains("TRUEHD", true) || codecId.contains("ALAC", true) -> "Lossless"
            trackType == MkvTrackType.VIDEO || trackType == MkvTrackType.AUDIO -> "Lossy"
            else -> ""
        }

    /** Bits/(Pixel*Frame) row for video tracks, e.g. "0.034". */
    val bitsPerPixelFrame: String
        get() {
            if (trackType != MkvTrackType.VIDEO) return ""
            val pixels = pixelWidth.toDouble() * pixelHeight.toDouble()
            if (pixels <= 0.0 || frameRate <= 0f || bitrateBps <= 0L) return ""
            val value = bitrateBps / (pixels * frameRate)
            return String.format(Locale.US, "%.3f", value)
        }

    /** "223 MiB (86%)" style row combining this track's bytes against the file size. */
    fun formattedStreamSize(fileSizeBytes: Long): String {
        if (streamSizeBytes <= 0L) return "—"
        val base = formatBytes(streamSizeBytes)
        return if (fileSizeBytes > 0L) {
            val pct = (streamSizeBytes.toDouble() / fileSizeBytes.toDouble() * 100.0).let {
                if (it < 0.1 && it > 0.0) "<0.1" else String.format(Locale.US, "%.0f", it)
            }
            "$base ($pct%)"
        } else base
    }
}

data class MkvChapterInfo(
    val id: Int,
    var startTimeMs: Long,
    var endTimeMs: Long = 0L,
    var title: String = "Chapter $id",
    var language: String = "eng",
    var uid: Long = id.toLong()
) {
    val formattedStartTime: String
        get() = formatTimeMs(startTimeMs)

    val formattedEndTime: String
        get() = if (endTimeMs > 0) formatTimeMs(endTimeMs) else ""

    companion object {
        fun formatTimeMs(ms: Long): String {
            val totalSeconds = ms / 1000
            val millis = ms % 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        }

        fun parseTimeStringToMs(timeStr: String): Long {
            return try {
                val clean = timeStr.trim().replace("\uFEFF", "")
                val token = clean.split(Regex("""\s+""")).firstOrNull { it.contains(":") || it.toDoubleOrNull() != null } ?: clean
                val parts = token.split(":")
                if (parts.size == 3) {
                    val h = parts[0].trim().toLongOrNull() ?: 0L
                    val m = parts[1].trim().toLongOrNull() ?: 0L
                    val sParts = parts[2].trim().split(".", ",")
                    val s = sParts[0].trim().toLongOrNull() ?: 0L
                    val ms = if (sParts.size > 1) {
                        sParts[1].trim().padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    } else 0L
                    (h * 3600 + m * 60 + s) * 1000 + ms
                } else if (parts.size == 2) {
                    val m = parts[0].trim().toLongOrNull() ?: 0L
                    val sParts = parts[1].trim().split(".", ",")
                    val s = sParts[0].trim().toLongOrNull() ?: 0L
                    val ms = if (sParts.size > 1) {
                        sParts[1].trim().padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                    } else 0L
                    (m * 60 + s) * 1000 + ms
                } else {
                    val d = token.toDoubleOrNull() ?: 0.0
                    (d * 1000).toLong()
                }
            } catch (e: Exception) {
                0L
            }
        }
    }
}

data class MkvAttachmentInfo(
    val id: Int,
    var fileName: String,
    var mimeType: String,
    var sizeBytes: Long,
    var description: String = "",
    var data: ByteArray = ByteArray(0),
    var isNew: Boolean = false,
    var uid: Long = id.toLong()
) {
    val isFont: Boolean
        get() = mimeType.contains("font", true) || fileName.endsWith(".ttf", true) || fileName.endsWith(".otf", true) || fileName.endsWith(".ttc", true) || mimeType.contains("truetype", true) || mimeType.contains("opentype", true)

    val isImage: Boolean
        get() = mimeType.contains("image", true) || fileName.endsWith(".png", true) || fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true)

    val formattedSize: String
        get() = formatBytes(sizeBytes)
}

data class MkvContainerMetadata(
    var fileName: String = "",
    var filePath: String = "",
    var fileSizeBytes: Long = 0L,
    var durationMs: Long = 0L,
    var title: String = "",
    var muxingApp: String = "",
    var writingApp: String = "",
    var dateUtc: String = "",
    var timecodeScale: Long = 1000000L, // 1ms default
    var segmentUid: String = "",
    var isLargeFile: Boolean = false,
    // EBML DocTypeVersion, e.g. 4 for "Matroska Version 4". Matches MediaInfo's
    // "Format version" row.
    var docTypeVersion: Int = 0,
    // Human readable container name for non-Matroska files (MPEG-4, QuickTime, ...). Empty = derive from extension.
    var containerFormat: String = ""
) {
    val formattedDuration: String
        get() = MkvChapterInfo.formatTimeMs(durationMs)

    val formattedSize: String
        get() = formatBytes(fileSizeBytes)

    val formattedFormatVersion: String
        get() = if (docTypeVersion > 0) "Version $docTypeVersion" else ""

    val overallBitrateBps: Long
        get() = if (durationMs > 0L) (fileSizeBytes * 8_000L / durationMs) else 0L
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}
