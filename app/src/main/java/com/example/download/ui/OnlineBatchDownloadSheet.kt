package com.example.ui.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
    private fun pickVideo(videos: List<VideoChoice>, want: Int): VideoChoice? {
        if (want <= 0) return videos.firstOrNull()
        val withHeight = videos.filter { it.height > 0 }
        return withHeight.firstOrNull { it.height == want }
            ?: withHeight.filter { it.height < want }.maxByOrNull { it.height }
            ?: withHeight.filter { it.height > want }.minByOrNull { it.height }
            ?: videos.firstOrNull()
    }

    /** The audio qualities of a link, best first (same order as the single download panel). */
    private fun audioQualities(info: LinkInfo): List<AudioChoice> =
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

    private fun pickAudio(list: List<AudioChoice>, wantKbps: Int): AudioChoice? {
        if (wantKbps <= 0) return list.firstOrNull()
        val withRate = list.filter { it.bitrateKbps > 0 }
        return withRate.firstOrNull { it.bitrateKbps == wantKbps }
            ?: withRate.filter { it.bitrateKbps < wantKbps }.maxByOrNull { it.bitrateKbps }
            ?: withRate.filter { it.bitrateKbps > wantKbps }.minByOrNull { it.bitrateKbps }
            ?: list.firstOrNull()
    }

    /** [target]: video height (0 = best) or audio kbps (0 = best). */
    fun start(context: Context, requests: List<DownloadRequest>, audio: Boolean, target: Int) {
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
                                        MediaProbe.planForAudio(info, a, true, true).copy(title = info.title, uploader = info.uploader.ifBlank { r.author.orEmpty() }),
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
                                            .copy(title = info.title, uploader = info.uploader.ifBlank { r.author.orEmpty() }),
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

@Composable
private fun OnlineBatchDownloadSheet(requests: List<DownloadRequest>, isDark: Boolean, glassBlurTransparency: Float, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val audio = requests.all { it.audio }
    val options = if (audio) AUDIO_TARGETS else VIDEO_TARGETS
    var optionIndex by remember {
        val want = if (audio) 0 else NetworkSettings.effectiveDownloadQuality(context)
        val i = if (want <= 0) 0 else options.indices.filter { options[it].first > 0 }.minByOrNull { kotlin.math.abs(options[it].first - want) } ?: 0
        mutableIntStateOf(i)
    }
    var showPicker by remember { mutableStateOf(false) }
    val chosen = options[optionIndex]

    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss) {
        DownloadSheetHeader(
            title = if (audio) "Download audio" else "Download", subtitle = "${requests.size} selected · same quality for all",
            primary = primary, secondary = secondary, enabled = true
        ) {
            OnlineBatchDownloader.start(context, requests, audio, chosen.first)
            onDismiss()
        }
        Spacer(Modifier.height(10.dp))

        SheetSectionLabel("Selected (${requests.size})", primary)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(primary.copy(alpha = 0.07f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val shown = requests.take(6)
            shown.forEach { e ->
                Text(e.title.ifBlank { e.url }, fontSize = 14.sp, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 2.dp))
            }
            if (requests.size > shown.size) {
                Text("+ ${requests.size - shown.size} more", fontSize = 13.sp, color = secondary, modifier = Modifier.padding(top = 4.dp))
            }
        }

        SheetSectionLabel(if (audio) "Audio quality" else "Video quality", primary)
        SelectorRow(
            badge = if (audio) "AUDIO" else "VIDEO", title = chosen.second,
            chips = listOf(SheetChip("${requests.size} ${if (audio) "songs" else "videos"}", accent = true)),
            selected = true, primary = primary, secondary = secondary,
            onClick = { showPicker = true }, trailing = SheetTrailing.Tick
        )
        SheetCaption(
            "Every item is downloaded in this quality. When one does not have it exactly, the nearest lower quality is used (else the nearest higher one), so the files stay about the same size. Tap above to change it.",
            secondary
        )
        SheetSaveToCard(if (audio) "Music/Viaura" else "Movies/Viaura", primary, secondary)
        Spacer(Modifier.height(4.dp))
    }

    if (showPicker) {
        DownloadPickerSheet(if (audio) "Audio quality" else "Video quality", isDark, glassBlurTransparency, primary, onDismiss = { showPicker = false }) {
            options.forEachIndexed { i, o ->
                val on = i == optionIndex
                SelectorRow(
                    badge = if (audio) "AUDIO" else "VIDEO", title = o.second, chips = emptyList(),
                    selected = on, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                    onClick = { optionIndex = i; showPicker = false },
                    trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                )
            }
        }
    }
}
