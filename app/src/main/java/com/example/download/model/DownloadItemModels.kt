package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, STOPPED, PROCESSING, COMPLETED, FAILED }

enum class DownloadSourceType { ONLINE_VIDEO, ONLINE_AUDIO, NETWORK_LINK, MEDIA_PROVIDER, TORRENT }

/** One download. It is saved to disk, so after an app restart it continues from the point where it stopped. */
data class DownloadItem(
    val id: String,
    val source: String,
    val title: String,
    val subtitle: String? = null,
    val isAudio: Boolean = false,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBps: Long = 0L,
    val etaSec: Long = -1L,
    val note: String? = null,
    val filePath: String? = null,
    val fileUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val thumbnailUrl: String? = null,
    val sourceType: DownloadSourceType = DownloadSourceType.TORRENT,
    val torrentFileIndex: Int = -1,
    /** JSON of the download plan (streams, subtitles, chapters ...) for every non-torrent download. */
    val planJson: String? = null,
    /** Short description such as "1080p · 2 audio · MKV". */
    val detail: String? = null,
    /** Series name (provider downloads): items with the same group are shown together in one folder. */
    val group: String? = null
) {
    val isActive: Boolean
        get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PROCESSING

    /** true while the transfer can be paused / stopped (never while the finished file is being put together). */
    val canInterrupt: Boolean
        get() = status == DownloadStatus.QUEUED || status == DownloadStatus.DOWNLOADING

    val fraction: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

object DownloadFormat {
    fun bytes(b: Long): String {
        if (b <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = b.toDouble()
        var index = 0
        while (value >= 1024.0 && index < units.size - 1) {
            value /= 1024.0
            index++
        }
        if (index == 0) return "$b B"
        val format = if (value >= 100.0) "%.0f %s" else if (value >= 10.0) "%.1f %s" else "%.2f %s"
        return String.format(Locale.US, format, value, units[index])
    }

    fun speed(bps: Long): String = bytes(bps) + "/s"

    fun eta(sec: Long): String {
        if (sec < 0L) return "--"
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun sizeLine(item: DownloadItem): String =
        bytes(item.downloadedBytes) + " / " + (if (item.totalBytes > 0L) bytes(item.totalBytes) else "?")

}
