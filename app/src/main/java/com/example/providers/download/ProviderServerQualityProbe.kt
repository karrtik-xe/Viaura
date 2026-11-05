package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.download.ContainerSniffer
import com.example.download.DownloadPlan
import com.example.download.HttpFetch
import com.example.download.PlanPart
import com.example.download.PlanSubtitle
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadFormat
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.media.MediaPluginInfo
import com.example.util.media.MediaProviderRepository
import com.example.util.media.MediaStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** One server / storage link of a provider title after it was really tried. */
internal data class ServerCandidate(
    val stream: MediaStream,
    val working: Boolean,
    val sizeBytes: Long,
    val format: String,
    val ext: String,
    val isHls: Boolean,
    val reason: String,
    /** "1080p" etc.: what the provider says, otherwise read from the file / playlist itself; "" when it can't be known. */
    val quality: String = ""
)

internal sealed interface ServersState {
    object Loading : ServersState
    data class Failed(val message: String) : ServersState
    data class Ready(val servers: List<ServerCandidate>) : ServersState
}

private fun isHlsStream(s: MediaStream): Boolean {
    val path = s.link.substringBefore('?').substringBefore('#').lowercase()
    val type = s.type.lowercase()
    return type == "m3u8" || type == "hls" || path.endsWith(".m3u8")
}

private fun isDashStream(s: MediaStream): Boolean {
    val path = s.link.substringBefore('?').substringBefore('#').lowercase()
    val type = s.type.lowercase()
    return type == "mpd" || type == "dash" || path.endsWith(".mpd")
}

private fun extOf(link: String, contentType: String?): String {
    val fromPath = link.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
    if (fromPath in setOf("mp4", "mkv", "webm", "mov", "avi", "m4v", "ts", "flv", "3gp")) return fromPath
    val ct = contentType.orEmpty().lowercase()
    return when {
        ct.contains("mp4") -> "mp4"
        ct.contains("matroska") -> "mkv"
        ct.contains("webm") -> "webm"
        ct.contains("quicktime") -> "mov"
        ct.contains("mp2t") -> "ts"
        else -> ""
    }
}

private fun heightLabel(width: Int, height: Int): String {
    if (height <= 0) return ""
    val byWidth = when {
        width >= 3600 -> "2160p"
        width >= 2400 -> "1440p"
        width >= 1800 -> "1080p"
        width >= 1200 -> "720p"
        width >= 800 -> "480p"
        width >= 600 -> "360p"
        else -> ""
    }
    return if (width > 0 && byWidth.isNotEmpty() && height >= 0.4f * width) byWidth else "${height}p"
}

/** Resolution of the rendition the downloader will really take from an HLS master playlist (its highest bandwidth one). */
private fun hlsQuality(text: String): String {
    var bestBw = -1L
    var best = ""
    for (line in text.lines()) {
        val l = line.trim()
        if (!l.startsWith("#EXT-X-STREAM-INF")) continue
        val bw = Regex("(?<![A-Z-])BANDWIDTH=(\\d+)").find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val res = Regex("RESOLUTION=(\\d+)x(\\d+)").find(l) ?: continue
        if (bw > bestBw) {
            bestBw = bw
            best = heightLabel(res.groupValues[1].toInt(), res.groupValues[2].toInt())
        }
    }
    return best
}

/** The first bytes of the file: the container is read from them, never guessed from the link. */
private fun sniffRemote(app: android.content.Context, s: MediaStream): String? = try {
    val head = HttpFetch.call(app, HttpFetch.request(s.link, s.headers, "bytes=0-1023"), null) { r ->
        if (r.code !in 200..299) null else {
            val src = r.body?.source()
            if (src == null) null else {
                src.request(1024)
                src.buffer.readByteArray(minOf(1024L, src.buffer.size))
            }
        }
    }
    if (head == null) null else ContainerSniffer.fromHead(head)
} catch (_: Throwable) {
    null
}

/** At most this much of a file is ever read to learn its picture size (it used to be unlimited, which could pull a large part of the movie). */
private const val QUALITY_PROBE_BUDGET = 3L * 1024 * 1024

private const val QUALITY_PROBE_BLOCK = 256 * 1024

/**
 * The remote file as a random-access source for the system's metadata reader, read with small ranged requests.
 * The total number of bytes it may ever download is capped by [QUALITY_PROBE_BUDGET]: when the picture size is not found
 * within it (or the server ignores Range, which would send the whole file) the read fails and the quality stays unknown.
 */
private class BudgetedRemoteSource(
    private val app: android.content.Context,
    private val link: String,
    private val headers: Map<String, String>,
    private val total: Long
) : android.media.MediaDataSource() {
    private val blocks = HashMap<Long, ByteArray>()
    private var used = 0L

    override fun getSize(): Long = total

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (size <= 0) return 0
        if (total > 0L && position >= total) return -1
        var done = 0
        var pos = position
        while (done < size) {
            if (total > 0L && pos >= total) break
            val start = pos - pos % QUALITY_PROBE_BLOCK
            val block = blocks[start] ?: fetch(start).also { blocks[start] = it }
            val inBlock = (pos - start).toInt()
            if (inBlock >= block.size) break
            val n = minOf(size - done, block.size - inBlock)
            System.arraycopy(block, inBlock, buffer, offset + done, n)
            done += n
            pos += n
            if (block.size < QUALITY_PROBE_BLOCK) break
        }
        return if (done == 0) -1 else done
    }

    private fun fetch(start: Long): ByteArray {
        if (used + QUALITY_PROBE_BLOCK > QUALITY_PROBE_BUDGET) throw java.io.IOException("probe budget used")
        used += QUALITY_PROBE_BLOCK
        val end = start + QUALITY_PROBE_BLOCK - 1
        val bytes = HttpFetch.call(app, HttpFetch.request(link, headers, "bytes=$start-$end"), null) { r ->
            // only a ranged answer is read: a plain 200 means the server would send the whole file
            if (r.code != 206) null else {
                val src = r.body?.source()
                if (src == null) null else {
                    src.request(QUALITY_PROBE_BLOCK.toLong())
                    src.buffer.readByteArray(minOf(QUALITY_PROBE_BLOCK.toLong(), src.buffer.size))
                }
            }
        }
        return bytes ?: throw java.io.IOException("range not supported")
    }

    override fun close() {}
}

/** Real picture size of the file (only used when the provider gives no quality): the system's own metadata reader, with a hard download cap. */
internal fun probeFileQuality(app: android.content.Context, s: MediaStream, size: Long): String {
    if (size in 1L..2048L) return ""
    val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    val job = worker.submit<String> {
        val mmr = android.media.MediaMetadataRetriever()
        try {
            mmr.setDataSource(BudgetedRemoteSource(app, s.link, s.headers, size))
            val w = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            heightLabel(w, h)
        } catch (_: Throwable) {
            ""
        } finally {
            try { mmr.release() } catch (_: Throwable) {}
        }
    }
    return try {
        job.get(12, java.util.concurrent.TimeUnit.SECONDS)
    } catch (_: Throwable) {
        job.cancel(true)
        ""
    } finally {
        worker.shutdownNow()
    }
}

/** Only what the server answers is used: the size comes from its Content-Length, the format from the file / its type. */
internal fun tryServer(app: android.content.Context, s: MediaStream): ServerCandidate {
    return try {
        if (isDashStream(s)) return ServerCandidate(s, false, -1L, "DASH", "", false, "DASH streams can't be downloaded")
        if (isHlsStream(s)) {
            val text = HttpFetch.call(app, HttpFetch.request(s.link, s.headers), null) { r ->
                if (r.code !in 200..299) null else r.body?.string()
            }
            val ok = text != null && text.trimStart('\uFEFF', ' ', '\n', '\r').startsWith("#EXTM3U")
            if (!ok) return ServerCandidate(s, false, -1L, "HLS", "ts", true, "The playlist does not answer")
            if (text!!.contains("SAMPLE-AES", true)) return ServerCandidate(s, false, -1L, "HLS", "ts", true, "Protected stream")
            if (!text.contains("#EXT-X-ENDLIST") && !text.contains("#EXT-X-STREAM-INF")) return ServerCandidate(s, false, -1L, "HLS", "ts", true, "Live stream")
            val q = s.quality?.takeIf { it.isNotBlank() } ?: hlsQuality(text)
            return ServerCandidate(s, true, -1L, "HLS", "ts", true, "", q)
        }
        val head = HttpFetch.head(app, s.link, s.headers) ?: return ServerCandidate(s, false, -1L, "", "", false, "The server does not answer")
        val ct = head.contentType.orEmpty().lowercase()
        if (ct.startsWith("text/") || ct.contains("json") || ct.contains("html")) return ServerCandidate(s, false, -1L, "", "", false, "This is a web page, not a video")
        // the format is read from the file's first bytes; the link / content type only decide when those are not recognised
        val ext = sniffRemote(app, s)
            ?: extOf(head.finalUrl, head.contentType).ifBlank { extOf(s.link, head.contentType) }
        // the picture size of a plain file is read later, only for the server that is selected (see ProviderReady)
        val q = s.quality?.takeIf { it.isNotBlank() }.orEmpty()
        ServerCandidate(s, true, head.size, ext.uppercase(), ext, false, "", q)
    } catch (_: Throwable) {
        ServerCandidate(s, false, -1L, "", "", false, "The server does not answer")
    }
}
