package com.example.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.example.ui.theme.AccentSkyBlue
import com.example.util.DownloadFormat
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.download.AudioChoice
import com.example.download.DownloadPlan
import com.example.download.LinkInfo
import com.example.download.MediaProbe
import com.example.download.VideoChoice
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.NetworkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Downloads several online videos / songs in one go with ONE quality: every item takes the chosen quality (the exact one,
 * else the nearest lower one, else the nearest higher one), so the files are about the same size. Runs on its own scope:
 * closing the panel does not stop it.
 */
internal object OnlineBatchDownloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())

    private fun toast(app: Context, text: String) {
        main.post { Toast.makeText(app, text, Toast.LENGTH_LONG).show() }
    }

    private fun withCoverFallback(plan: DownloadPlan, siteThumb: String, appThumb: String?): DownloadPlan {
        val app = appThumb.orEmpty().trim()
        val primary = siteThumb.trim().ifBlank { app }
        return plan.copy(thumbnailUrl = primary, fallbackThumbnailUrl = app.takeIf { it.isNotBlank() && it != primary }.orEmpty())
    }

    /** Same rule as the single panel: the exact height; else the best one below it; else the lowest one above it. */
    internal fun pickVideo(videos: List<VideoChoice>, want: Int): VideoChoice? {
        if (want <= 0) return videos.firstOrNull()
        val withHeight = videos.filter { it.height > 0 }
        return withHeight.firstOrNull { it.height == want }
            ?: withHeight.filter { it.height < want }.maxByOrNull { it.height }
            ?: withHeight.filter { it.height > want }.minByOrNull { it.height }
            ?: videos.firstOrNull()
    }

    /** The audio qualities of a link, best first (same order as the single download panel). */
    internal fun audioQualities(info: LinkInfo): List<AudioChoice> =
        info.audioQualities.ifEmpty { info.audioTracks }.ifEmpty {
            // only muxed files exist: the audio is taken out of the smallest one
            info.videos.filter { it.muxed }.sortedBy { it.height }.take(1).map { v ->
                AudioChoice(v.key, "Original", "", true, 0, "", v.ext, v.sizeBytes, v.sizeApprox, v.format)
            }
        }.sortedWith(
            compareByDescending<AudioChoice> { it.ext.equals("flac", ignoreCase = true) }
                .thenByDescending { it.ext.equals("m4a", ignoreCase = true) }
                .thenByDescending { it.ext.equals("mp3", ignoreCase = true) }
                .thenByDescending { it.bitrateKbps }
                .thenByDescending { it.sizeBytes }
        )

    internal fun pickAudio(list: List<AudioChoice>, wantKbps: Int): AudioChoice? {
        if (wantKbps <= 0) return list.firstOrNull()
        val withRate = list.filter { it.bitrateKbps > 0 }
        return withRate.firstOrNull { it.bitrateKbps == wantKbps }
            ?: withRate.filter { it.bitrateKbps < wantKbps }.maxByOrNull { it.bitrateKbps }
            ?: withRate.filter { it.bitrateKbps > wantKbps }.minByOrNull { it.bitrateKbps }
            ?: list.firstOrNull()
    }

    /** [target]: video height (0 = best) or audio kbps (0 = best). */
    fun start(
        context: Context, requests: List<DownloadRequest>, audio: Boolean, target: Int,
        /** url -> (title, author) as the person edited them in the panel */
        edited: Map<String, Pair<String, String>> = emptyMap()
    ) {
        val app = context.applicationContext
        if (requests.isEmpty()) return
        toast(app, "Preparing ${requests.size} downloads…")
        scope.launch {
            val started = AtomicInteger(0)
            val failed = AtomicInteger(0)
            val gate = Semaphore(3)
            coroutineScope {
                requests.map { r ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            try {
                                val info = MediaProbe.probe(app, r.url, emptyMap(), measureSizes = false, useCache = true).getOrNull()
                                if (info == null) { failed.incrementAndGet(); return@withPermit }
                                val asAudio = audio || info.videos.isEmpty() || info.isAudioOnlySource
                                if (asAudio) {
                                    val a = pickAudio(audioQualities(info), target)
                                    if (a == null) { failed.incrementAndGet(); return@withPermit }
                                    val plan = withCoverFallback(
                                        MediaProbe.planForAudio(info, a, true, true).copy(
                                            title = edited[r.url]?.first?.takeIf { it.isNotBlank() } ?: info.title,
                                            uploader = edited[r.url]?.second?.takeIf { it.isNotBlank() } ?: info.uploader.ifBlank { r.author.orEmpty() }
                                        ),
                                        info.thumbnailUrl, r.thumbnail
                                    )
                                    val outputExt = if (plan.parts.firstOrNull()?.role == "muxed" || a.ext.equals("webm", true) || a.ext.equals("weba", true)) "MKA" else a.ext.uppercase()
                                    val detail = listOf(plan.qualityLabel, outputExt).filter { it.isNotBlank() }.joinToString(" · ")
                                    DownloadManager.enqueuePlan(app, plan, DownloadSourceType.ONLINE_AUDIO, detail, a.sizeBytes.coerceAtLeast(0L))
                                } else {
                                    val video = pickVideo(info.videos, target)
                                    if (video == null) { failed.incrementAndGet(); return@withPermit }
                                    // the audio track of the preferred language when the video has one, else the original track
                                    val lang = NetworkSettings.mediaLanguage(app)
                                    val preferred = if (lang.isBlank()) null else info.audioTracks.firstOrNull {
                                        com.example.util.media.MediaPreferences.mentionsLanguage(it.label, lang) ||
                                            com.example.player.AudioLanguageMatcher.aliasesOf(lang).any { a -> a.equals(it.language.trim(), ignoreCase = true) || it.language.trim().lowercase().startsWith("$a-") }
                                    }
                                    val audioNeeded = !video.muxed && info.audioTracks.isNotEmpty()
                                    val attached = if (audioNeeded) (preferred?.let { listOf(it) } ?: info.audioTracks.take(1)) else emptyList()
                                    val subs = if (NetworkSettings.downloadSubtitles(app)) info.subtitles else emptyList()
                                    val plan = withCoverFallback(
                                        MediaProbe.planForVideo(info, video, attached, subs, info.chapters.isNotEmpty(), true)
                                            .copy(
                                                title = edited[r.url]?.first?.takeIf { it.isNotBlank() } ?: info.title,
                                                uploader = edited[r.url]?.second?.takeIf { it.isNotBlank() } ?: info.uploader.ifBlank { r.author.orEmpty() }
                                            ),
                                        info.thumbnailUrl, r.thumbnail
                                    )
                                    val total = if (video.sizeBytes > 0L) video.sizeBytes + attached.sumOf { it.sizeBytes.coerceAtLeast(0L) } else 0L
                                    DownloadManager.enqueuePlan(app, plan, DownloadSourceType.ONLINE_VIDEO, listOf(video.label, "MKV").filter { it.isNotBlank() }.joinToString(" · "), total)
                                }
                                started.incrementAndGet()
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (_: Throwable) {
                                failed.incrementAndGet()
                            }
                        }
                    }
                }.awaitAll()
            }
            val ok = started.get()
            val bad = failed.get()
            toast(app, when {
                ok == 0 -> "No download could be started"
                bad == 0 -> "$ok downloads started"
                else -> "$ok downloads started, $bad could not be read"
            })
        }
    }
}

private val VIDEO_TARGETS = listOf(0 to "Best available", 2160 to "2160p (4K)", 1440 to "1440p", 1080 to "1080p", 720 to "720p", 480 to "480p", 360 to "360p", 240 to "240p")
private val AUDIO_TARGETS = listOf(0 to "Best available", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 160 to "160 kbps", 128 to "128 kbps", 96 to "96 kbps", 64 to "64 kbps")

/** Shows the batch panel for [requests] (several items picked with the swipe toolbar). */
@Composable
fun OnlineBatchDownloadHost(requests: List<DownloadRequest>?, isDark: Boolean, glassBlurTransparency: Float, onDismiss: () -> Unit) {
    val r = requests ?: return
    OnlineBatchDownloadSheet(r, isDark, glassBlurTransparency, onDismiss)
}

/** One selected item of the batch panel: what the person edits (title, author) and what was found out about the link. */
private class BatchRow(val req: DownloadRequest) {
    var title by mutableStateOf(req.title)
    var author by mutableStateOf(req.author.orEmpty())
    var info by mutableStateOf<LinkInfo?>(null)
    var failed by mutableStateOf(false)
}

@Composable
private fun OnlineBatchDownloadSheet(requests: List<DownloadRequest>, isDark: Boolean, glassBlurTransparency: Float, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val audio = requests.all { it.audio }
    val rows = remember(requests) { requests.map { BatchRow(it) } }

    // The quality of Settings > Network > Media is chosen from the very first frame and stays until the person picks
    // another one here: nothing is switched while the qualities are still being read.
    val want = remember { if (audio) 0 else NetworkSettings.effectiveDownloadQuality(context) }
    var picked by remember { mutableStateOf<Int?>(null) }
    val target = picked ?: want
    var audioIndex by remember { mutableIntStateOf(0) }
    var showPicker by remember { mutableStateOf(false) }

    // every item is searched for the qualities it really has (all in the background, three at a time)
    LaunchedEffect(rows) {
        val gate = Semaphore(3)
        coroutineScope {
            rows.map { row ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        val r = try { MediaProbe.probe(context, row.req.url, emptyMap(), measureSizes = false, useCache = true).getOrNull() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Throwable) { null }
                        if (r == null) {
                            row.failed = true
                        } else {
                            row.info = r
                            if (row.title.isBlank()) row.title = r.title
                            if (row.author.isBlank()) row.author = r.uploader
                        }
                        Unit
                    }
                }
            }.awaitAll()
        }
        // exact sizes are measured right after and fill in by themselves
        val gate2 = Semaphore(2)
        coroutineScope {
            rows.map { row ->
                async(Dispatchers.IO) {
                    gate2.withPermit {
                        val raw = row.info ?: return@withPermit
                        val measured = try { MediaProbe.refineSizes(context, raw) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Throwable) { raw }
                        if (measured != raw) {
                            row.info = measured
                            MediaProbe.remember(row.req.url, emptyMap(), false, measured)
                        }
                    }
                }
            }.awaitAll()
        }
    }

    val probed = rows.count { it.info != null || it.failed }
    val allProbed = probed == rows.size
    val heights = rows.mapNotNull { it.info }.flatMap { i -> i.videos.map { v -> v.height } }.filter { it > 0 }.distinct().sortedDescending()
    val audioTarget = AUDIO_TARGETS[audioIndex].first

    // what every row would download right now: (label, bytes, approx, fellBack)
    fun summaryOf(row: BatchRow): Triple<String, Long, Boolean>? {
        val info = row.info ?: return null
        val asAudio = audio || info.videos.isEmpty() || info.isAudioOnlySource
        return if (asAudio) {
            val a = OnlineBatchDownloader.pickAudio(OnlineBatchDownloader.audioQualities(info), audioTarget) ?: return null
            Triple(listOf(if (a.bitrateKbps > 0) "${a.bitrateKbps} kbps" else "", a.ext.uppercase()).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Audio" }, a.sizeBytes, a.sizeApprox || a.sizeBytes <= 0L)
        } else {
            val v = OnlineBatchDownloader.pickVideo(info.videos, target) ?: return null
            val extra = if (!v.muxed && info.audioTracks.isNotEmpty()) info.audioTracks.first().sizeBytes.coerceAtLeast(0L) else 0L
            val bytes = if (v.sizeBytes > 0L) v.sizeBytes + extra else -1L
            Triple(listOf(v.label, v.codecName).filter { it.isNotBlank() }.joinToString(" · "), bytes, v.sizeApprox)
        }
    }
    val summaries = rows.map { summaryOf(it) }
    val totalBytes = summaries.sumOf { (it?.second ?: 0L).coerceAtLeast(0L) }
    val totalApprox = summaries.any { it != null && (it.third || it.second <= 0L) }

    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss) {
        DownloadSheetHeader(
            title = if (audio) "Download audio" else "Download", subtitle = "${requests.size} selected · same quality for all",
            primary = primary, secondary = secondary, enabled = true
        ) {
            val edited = rows.associate { it.req.url to (it.title.trim() to it.author.trim()) }
            OnlineBatchDownloader.start(context, requests, audio, if (audio) audioTarget else target, edited)
            onDismiss()
        }
        Spacer(Modifier.height(10.dp))

        // ---- quality for all ----
        SheetSectionLabel(if (audio) "Audio quality" else "Video quality", primary)
        val qualityTitle = if (audio) AUDIO_TARGETS[audioIndex].second else if (target <= 0) "Best available" else "${target}p"
        SelectorRow(
            badge = if (audio) "AUDIO" else "VIDEO", title = qualityTitle,
            chips = listOfNotNull(
                SheetChip("${requests.size} ${if (audio) "songs" else "videos"}", accent = true),
                if (totalBytes > 0L) SheetChip("Total " + (if (totalApprox) "~" else "") + DownloadFormat.bytes(totalBytes)) else null
            ),
            selected = true, primary = primary, secondary = secondary,
            onClick = { showPicker = true }, trailing = SheetTrailing.Tick
        )
        SheetCaption(
            (if (!audio && picked == null && want > 0) "Taken from Settings › Network › download quality. " else "") +
                "Every video is downloaded in this quality. When one does not have it exactly, the nearest lower quality is used (else the nearest higher one), so the files stay about the same size. Tap above to change it.",
            secondary
        )
        if (!allProbed) {
            Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                Spacer(Modifier.width(10.dp))
                Text("Checking the qualities of each video… $probed of ${rows.size}", fontSize = 13.sp, color = secondary)
            }
        }

        // ---- the selected items, one below the other: cover + title + author (editable) and what will be downloaded ----
        SheetSectionLabel("Selected (${requests.size})", primary)
        rows.forEachIndexed { i, row ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            SheetCoverTitleAuthor(
                thumbnail = row.info?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: row.req.thumbnail,
                title = row.title, onTitleChange = { row.title = it },
                author = row.author, onAuthorChange = { row.author = it },
                primary = primary, secondary = secondary, coverWidth = 112.dp
            )
            val sm = summaries[i]
            val line = when {
                row.failed -> "Could not read this link"
                sm == null -> "Checking…"
                else -> sm.first + (if (sm.second > 0L) " · " + (if (sm.third) "~" else "") + DownloadFormat.bytes(sm.second) else " · size unknown")
            }
            Text(line, fontSize = 13.sp, color = if (row.failed) androidx.compose.ui.graphics.Color(0xFFEF4444) else secondary, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
        }
        SheetSaveToCard(if (audio) "Music/Viaura" else "Movies/Viaura", primary, secondary)
        Spacer(Modifier.height(4.dp))
    }

    if (showPicker) {
        DownloadPickerSheet(if (audio) "Audio quality" else "Video quality", isDark, glassBlurTransparency, primary, onDismiss = { showPicker = false }) {
            if (audio) {
                AUDIO_TARGETS.forEachIndexed { i, o ->
                    val on = i == audioIndex
                    SelectorRow(
                        badge = "AUDIO", title = o.second, chips = emptyList(), selected = on, primary = primary, secondary = secondary,
                        modifier = Modifier.padding(top = 10.dp),
                        onClick = { audioIndex = i; showPicker = false }, trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                    )
                }
            } else {
                val options = listOf(0) + heights
                options.forEach { h ->
                    val on = target == h
                    val count = if (h == 0) rows.count { it.info != null } else rows.count { r -> r.info?.videos?.any { it.height == h } == true }
                    SelectorRow(
                        badge = "VIDEO", title = if (h == 0) "Best available" else "${h}p",
                        chips = if (allProbed || count > 0) listOf(SheetChip(if (h == 0) "every video's best" else "in $count of ${rows.size} videos", accent = count == rows.size)) else emptyList(),
                        selected = on, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                        onClick = { picked = h; showPicker = false }, trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                    )
                }
                if (heights.isEmpty()) {
                    Text("The qualities are still being read…", fontSize = 13.sp, color = secondary, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
