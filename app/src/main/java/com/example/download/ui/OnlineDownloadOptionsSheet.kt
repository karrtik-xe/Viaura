package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.download.AudioChoice
import com.example.download.DownloadPlan
import com.example.download.LinkInfo
import com.example.download.MediaProbe
import com.example.download.PlanSubtitle
import com.example.download.VideoChoice
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadFormat
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.NetworkSettings
import kotlinx.coroutines.launch

private sealed interface ProbeState {
    object Loading : ProbeState
    data class Failed(val message: String) : ProbeState
    data class Ready(val info: LinkInfo) : ProbeState
}

private fun sizeLabel(bytes: Long, approx: Boolean): String =
    if (bytes <= 0L) "size unknown" else (if (approx) "~" else "") + DownloadFormat.bytes(bytes)

private enum class Picker { VIDEO, AUDIO_TRACKS, SUBTITLES, AUDIO_QUALITY }

/**
 * Download panel of every source (Online Video / Online Audio / Local Network link / Media provider link). The qualities,
 * sizes, audio tracks and subtitles are exactly what the website offers for this link - nothing is invented.
 * Layout: header with the Download button, Audio | Video tabs, thumbnail card, editable Title / Author, then rows that open a
 * second sheet (video quality, audio tracks, subtitles / audio quality).
 */
@Composable
fun DownloadOptionsSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    url: String,
    initialTitle: String,
    initialAuthor: String? = null,
    initialThumbnail: String? = null,
    startWithAudio: Boolean = false,
    audioOnly: Boolean = false,
    sourceType: DownloadSourceType = DownloadSourceType.ONLINE_VIDEO,
    headers: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ProbeState>(ProbeState.Loading) }

    LaunchedEffect(url, attempt) {
        state = ProbeState.Loading
        // the panel opens as soon as the website has answered; exact sizes are measured right after and fill in by themselves
        val r = MediaProbe.probe(context, url, headers, measureSizes = false)
        state = r.fold(
            onSuccess = { ProbeState.Ready(it) },
            onFailure = { ProbeState.Failed(it.message?.lineSequence()?.firstOrNull { l -> l.isNotBlank() }?.take(200) ?: "This link can't be downloaded") }
        )
        r.getOrNull()?.let { raw ->
            val measured = MediaProbe.refineSizes(context, raw)
            if (measured != raw) state = ProbeState.Ready(measured)
        }
    }

    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss) {
        when (val s = state) {
            ProbeState.Loading -> {
                DownloadSheetHeader(title = if (audioOnly) "Download audio" else "Download", subtitle = "Adjust download", primary = primary, secondary = secondary, enabled = false) { }
                Text(initialTitle.ifBlank { url }, fontSize = 14.sp, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp))
                Row(modifier = Modifier.padding(top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Checking the qualities this video really has…", fontSize = 13.sp, color = secondary)
                }
                Spacer(Modifier.height(64.dp))
            }
            is ProbeState.Failed -> {
                DownloadSheetHeader(title = if (audioOnly) "Download audio" else "Download", subtitle = "Adjust download", primary = primary, secondary = secondary, enabled = false) { }
                Text(s.message, fontSize = 13.sp, color = secondary, modifier = Modifier.padding(top = 16.dp, bottom = 14.dp))
                PrimaryButton("Retry") { attempt++ }
                Spacer(Modifier.height(56.dp))
            }
            is ProbeState.Ready -> ReadyContent(
                info = s.info, isDark = isDark, glassBlurTransparency = glassBlurTransparency, primary = primary, secondary = secondary, url = url,
                initialThumbnail = initialThumbnail, initialAuthor = initialAuthor, startWithAudio = startWithAudio, audioOnly = audioOnly,
                sourceType = sourceType, onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (enabled) AccentGradient else androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.4f)))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

private fun audioBytesOf(list: List<AudioChoice>): Long = list.sumOf { it.sizeBytes.coerceAtLeast(0L) }

private fun audioApproxOf(list: List<AudioChoice>): Boolean = list.isNotEmpty() && list.any { it.sizeApprox || it.sizeBytes <= 0L }

private fun videoChips(v: VideoChoice, bytes: Long, approx: Boolean): List<SheetChip> = listOfNotNull(
    v.codecName.takeIf { it.isNotBlank() }?.let { SheetChip(it) },
    SheetChip(sizeLabel(bytes, approx), accent = true),
    if (v.muxed) SheetChip("with audio") else null
)

private fun audioChips(a: AudioChoice): List<SheetChip> = listOfNotNull(
    a.codecName.takeIf { it.isNotBlank() }?.let { SheetChip(it) },
    a.bitrateKbps.takeIf { it > 0 }?.let { SheetChip("$it kbps") },
    SheetChip(sizeLabel(a.sizeBytes, a.sizeApprox), accent = true)
)

private fun audioQualityTitle(a: AudioChoice): String =
    if (a.bitrateKbps > 0) "${a.bitrateKbps} kbps" else a.ext.uppercase().ifBlank { "Original" }

/** The cover is the website's thumbnail; the one the app already shows for this item is kept as a backup (used when the first is empty / not loadable). */
private fun DownloadPlan.withCoverFallback(siteThumb: String, appThumb: String?): DownloadPlan {
    val app = appThumb.orEmpty().trim()
    val primary = siteThumb.trim().ifBlank { app }
    return copy(thumbnailUrl = primary, fallbackThumbnailUrl = app.takeIf { it.isNotBlank() && it != primary }.orEmpty())
}

private fun subtitleKey(sb: PlanSubtitle): String = sb.language + "|" + sb.url

@Composable
private fun ColumnScope.ReadyContent(
    info: LinkInfo, isDark: Boolean, glassBlurTransparency: Float, primary: Color, secondary: Color, url: String,
    initialThumbnail: String?, initialAuthor: String?, startWithAudio: Boolean, audioOnly: Boolean,
    sourceType: DownloadSourceType, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val canAudioMode = info.audioQualities.isNotEmpty() || info.audioTracks.isNotEmpty() || info.videos.any { it.muxed }
    val canVideoMode = info.videos.isNotEmpty() && !audioOnly
    var audioMode by remember { mutableStateOf(audioOnly || startWithAudio && canAudioMode || (!canVideoMode && canAudioMode) || info.isAudioOnlySource) }
    val showTabs = !audioOnly && canVideoMode && canAudioMode && !info.isAudioOnlySource

    var title by remember { mutableStateOf(info.title) }
    var author by remember { mutableStateOf(info.uploader.ifBlank { initialAuthor.orEmpty() }) }
    var picker by remember { mutableStateOf<Picker?>(null) }

    // Settings > Network > Media settings: the quality set there is pre-selected (changeable here for this download).
    // No quality set (Auto / best) = the best quality, like before.
    var videoKey by remember {
        val want = NetworkSettings.effectiveDownloadQuality(context)
        val pick = if (want > 0) {
            val withHeight = info.videos.filter { it.height > 0 }
            // the exact height; else the best one below it; else the lowest one above it
            withHeight.firstOrNull { it.height == want }
                ?: withHeight.filter { it.height < want }.maxByOrNull { it.height }
                ?: withHeight.filter { it.height > want }.minByOrNull { it.height }
        } else null
        mutableStateOf((pick ?: info.videos.firstOrNull())?.key)
    }
    // the audio track of the preferred language when the video has one, else the original track
    val selectedAudioKeys = remember {
        val lang = NetworkSettings.mediaLanguage(context)
        val preferred = if (lang.isBlank()) null else info.audioTracks.firstOrNull {
            com.example.util.media.MediaPreferences.mentionsLanguage(it.label, lang) ||
                com.example.player.AudioLanguageMatcher.aliasesOf(lang).any { a -> a.equals(it.language.trim(), ignoreCase = true) || it.language.trim().lowercase().startsWith("$a-") }
        }
        mutableStateOf((preferred?.let { listOf(it) } ?: info.audioTracks.take(1)).map { it.key }.toSet())
    }
    val selectedExtraAudioKeys = remember { mutableStateOf(emptySet<String>()) } // other-language tracks added to a video that already has sound
    val allSubKeys = info.subtitles.map { subtitleKey(it) }
    val selectedSubKeys = remember { mutableStateOf(if (NetworkSettings.downloadSubtitles(context)) allSubKeys.toSet() else emptySet()) }
    var withChapters by remember { mutableStateOf(info.chapters.isNotEmpty()) }
    var withThumb by remember { mutableStateOf(true) }
    var withLyrics by remember { mutableStateOf(true) }
    val audioQualityList: List<AudioChoice> = remember(info) {
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
    }
    var audioQualityKey by remember { mutableStateOf(audioQualityList.firstOrNull()?.key) }

    // what is chosen right now (video mode)
    val chosenVideo: VideoChoice? = info.videos.firstOrNull { it.key == videoKey } ?: info.videos.firstOrNull()
    val audioNeeded = chosenVideo != null && !chosenVideo.muxed && info.audioTracks.isNotEmpty()
    val chosenAudios = info.audioTracks.filter { it.key in selectedAudioKeys.value }
    // a video that already carries its sound can still take the website's other-language audio tracks
    val extraChoices = if (chosenVideo != null && chosenVideo.muxed) info.audioTracks.filter { !it.isOriginal } else emptyList()
    val chosenExtras = extraChoices.filter { it.key in selectedExtraAudioKeys.value }
    val attachedAudios = if (audioNeeded) chosenAudios else chosenExtras
    val audioBytes = audioBytesOf(attachedAudios)
    val audioApprox = audioApproxOf(attachedAudios)
    val chosenSubs = info.subtitles.filter { subtitleKey(it) in selectedSubKeys.value }

    // size of one video row = the video + the audio tracks that go with it
    val totalOf: (VideoChoice) -> Pair<Long, Boolean> = { v ->
        val extra = when {
            !v.muxed && info.audioTracks.isNotEmpty() -> chosenAudios
            v.muxed && v.key == chosenVideo?.key -> chosenExtras
            else -> emptyList()
        }
        val bytes = if (v.sizeBytes > 0L) v.sizeBytes + audioBytesOf(extra) else -1L
        bytes to (v.sizeApprox || audioApproxOf(extra))
    }

    // what is chosen right now (audio mode)
    val chosenAudioQ = audioQualityList.firstOrNull { it.key == audioQualityKey } ?: audioQualityList.firstOrNull()

    fun startDownload() {
        val cleanTitle = title.trim().ifBlank { info.title }
        val cleanAuthor = author.trim()
        if (audioMode) {
            val a = chosenAudioQ ?: return
            val plan = MediaProbe.planForAudio(info, a, withThumb, withLyrics).copy(title = cleanTitle, uploader = cleanAuthor).withCoverFallback(info.thumbnailUrl, initialThumbnail)
            val outputExt = if (plan.parts.firstOrNull()?.role == "muxed" || a.ext.equals("webm", true) || a.ext.equals("weba", true)) "MKA" else a.ext.uppercase()
            val detail = listOf(plan.qualityLabel, outputExt).filter { it.isNotBlank() }.joinToString(" · ")
            val msg = DownloadManager.enqueuePlan(context, plan, if (sourceType == DownloadSourceType.ONLINE_VIDEO) DownloadSourceType.ONLINE_AUDIO else sourceType, detail, a.sizeBytes.coerceAtLeast(0L))
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onDismiss()
        } else {
            val video = chosenVideo ?: return
            val plan = MediaProbe.planForVideo(
                info, video, attachedAudios, chosenSubs, withChapters && info.chapters.isNotEmpty(), withThumb
            ).copy(title = cleanTitle, uploader = cleanAuthor).withCoverFallback(info.thumbnailUrl, initialThumbnail)
            val totalBytes = totalOf(video).first
            val extras = ArrayList<String>()
            extras.add(video.label)
            if (attachedAudios.size > 1 || (!audioNeeded && attachedAudios.isNotEmpty())) extras.add("${attachedAudios.size + if (audioNeeded) 0 else 1} audio")
            if (chosenSubs.isNotEmpty()) extras.add("${chosenSubs.size} subtitles")
            extras.add("MKV")
            val msg = DownloadManager.enqueuePlan(context, plan, sourceType, extras.joinToString(" · "), if (totalBytes > 0L) totalBytes else 0L)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    // ---- header + tabs ----
    DownloadSheetHeader(
        title = if (audioOnly) "Download audio" else "Download", subtitle = "Adjust download", primary = primary, secondary = secondary,
        enabled = if (audioMode) chosenAudioQ != null else chosenVideo != null
    ) { startDownload() }
    if (showTabs) {
        Spacer(Modifier.height(14.dp))
        DownloadModeTabs(tabs = listOf("Audio", "Video"), selectedIndex = if (audioMode) 0 else 1, primary = primary, secondary = secondary, onSelect = { audioMode = it == 0 })
    }
    Spacer(Modifier.height(16.dp))

    // ---- thumbnail + name / author ----
    val thumb = info.thumbnailUrl.ifBlank { initialThumbnail.orEmpty() }.takeIf { it.isNotBlank() }
    SheetOptionCard(
        title = if (audioMode) "Audio Thumbnail" else "Video Thumbnail",
        subtitle = if (!withThumb) "Not attached" else if (audioMode) "Embedded as cover art" else "Preserved with video file",
        primary = primary, secondary = secondary, checked = withThumb, onCheckedChange = { withThumb = it },
        leading = {
            LinkThumbnail(
                thumbnail = thumb,
                modifier = Modifier.size(width = 82.dp, height = 58.dp).clip(RoundedCornerShape(14.dp)),
                fallback = { Box(Modifier.size(width = 82.dp, height = 58.dp).background(secondary.copy(alpha = 0.15f))) }
            )
        }
    )
    SheetTextField("Title", title, { title = it }, primary, secondary, Modifier.padding(top = 20.dp))
    SheetTextField("Author", author, { author = it }, primary, secondary, Modifier.padding(top = 14.dp))

    if (!audioMode) {
        // ---- video quality ----
        SheetSectionLabel("Video quality", primary)
        if (chosenVideo != null) {
            val (bytes, approx) = totalOf(chosenVideo)
            SelectorRow(
                badge = chosenVideo.ext, title = chosenVideo.label, chips = videoChips(chosenVideo, bytes, approx), selected = true,
                primary = primary, secondary = secondary,
                onClick = if (info.videos.size > 1) { { picker = Picker.VIDEO } } else null,
                trailing = SheetTrailing.Tick
            )
            val heights = info.videos.map { it.height }.filter { it > 0 }
            val lo = heights.minOrNull()
            val hi = heights.maxOrNull()
            SheetCaption(
                (if (lo != null && hi != null && lo != hi) "Qualities available: ${lo}p to ${hi}p (${info.videos.size} available)" else "${info.videos.size} quality available") +
                    " • ~ marks an estimated size" + (if (bytes > 0L) "\nDownload size: ${if (approx) "~" else ""}${DownloadFormat.bytes(bytes)}" else ""),
                secondary
            )

            // ---- audio tracks ----
            SheetSectionLabel("Audio tracks", primary)
            if (audioNeeded) {
                val names = chosenAudios.joinToString(", ") { it.label }.ifBlank { "Original" }
                SelectorRow(
                    badge = if (chosenAudios.size > 1) "×${chosenAudios.size}" else chosenAudios.firstOrNull()?.ext?.ifBlank { "audio" } ?: "audio",
                    title = if (chosenAudios.isEmpty()) "No audio tracks selected" else names,
                    chips = listOf(SheetChip("${chosenAudios.size} of ${info.audioTracks.size} selected"), SheetChip(sizeLabel(audioBytes, audioApprox), accent = true)),
                    selected = chosenAudios.isNotEmpty(), primary = primary, secondary = secondary,
                    leadingIconRes = R.drawable.viaura_audio_track,
                    onClick = if (info.audioTracks.isNotEmpty()) { { picker = Picker.AUDIO_TRACKS } } else null,
                    trailing = if (chosenAudios.isNotEmpty()) SheetTrailing.Tick else SheetTrailing.None
                )
                SheetCaption(
                    if (chosenAudios.isEmpty()) "No audio track selected; the downloaded video will be silent."
                    else if (chosenAudios.size > 1) "All selected audio tracks are kept inside one MKV file."
                    else if (info.audioTracks.size > 1) "Tap above to choose one or more audio tracks."
                    else "The only audio track this video offers.",
                    secondary
                )
            } else {
                val names = (listOf("Original") + chosenExtras.map { it.label }).joinToString(", ")
                SelectorRow(
                    badge = if (chosenExtras.isNotEmpty()) "×${chosenExtras.size + 1}" else "AUDIO",
                    title = names,
                    chips = listOfNotNull(
                        SheetChip(if (chosenVideo.muxed) "included in video" else "kept as in the file"),
                        if (chosenExtras.isNotEmpty()) SheetChip("+${chosenExtras.size} extra") else null,
                        if (chosenExtras.isNotEmpty()) SheetChip(sizeLabel(audioBytes, audioApprox), accent = true) else null
                    ),
                    selected = true, primary = primary, secondary = secondary,
                    onClick = if (extraChoices.isNotEmpty()) { { picker = Picker.AUDIO_TRACKS } } else null,
                    trailing = SheetTrailing.Tick
                )
                SheetCaption(
                    if (chosenExtras.isNotEmpty()) "The extra audio tracks are kept inside one MKV file."
                    else if (extraChoices.isNotEmpty()) "Tap above to add the website's other audio tracks."
                    else "This video has one audio track.",
                    secondary
                )
            }
        }

        // ---- subtitles ----
        if (info.subtitles.isNotEmpty()) {
            SheetSectionLabel("Subtitles", primary)
            SelectorRow(
                badge = "SUB",
                title = when {
                    chosenSubs.isEmpty() -> "No subtitles"
                    chosenSubs.size <= 2 -> chosenSubs.joinToString(", ") { it.name }
                    else -> "${chosenSubs.size} subtitles"
                },
                chips = listOf(SheetChip("${chosenSubs.size} of ${info.subtitles.size} selected")),
                selected = chosenSubs.isNotEmpty(), primary = primary, secondary = secondary,
                onClick = { picker = Picker.SUBTITLES },
                trailing = if (chosenSubs.isNotEmpty()) SheetTrailing.Tick else SheetTrailing.None
            )
            if (chosenSubs.isNotEmpty()) SheetCaption("Selected subtitles are kept inside one MKV file.", secondary)
        }

        if (info.chapters.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SheetOptionCard(
                title = "Chapters (${info.chapters.size})", subtitle = if (withChapters) "Attached to the video file" else "Not attached",
                primary = primary, secondary = secondary, checked = withChapters, onCheckedChange = { withChapters = it }
            )
        }
        SheetSaveToCard("Movies/Viaura", primary, secondary)
    } else {
        // ---- audio quality ----
        SheetSectionLabel("Audio quality", primary)
        if (chosenAudioQ != null) {
            SelectorRow(
                badge = chosenAudioQ.ext.ifBlank { "audio" }, title = audioQualityTitle(chosenAudioQ), chips = audioChips(chosenAudioQ), selected = true,
                primary = primary, secondary = secondary, leadingIconRes = R.drawable.viaura_audio_track,
                onClick = if (audioQualityList.size > 1) { { picker = Picker.AUDIO_QUALITY } } else null,
                trailing = SheetTrailing.Tick
            )
            SheetCaption("${audioQualityList.size} ${if (audioQualityList.size == 1) "quality" else "qualities"} available • ~ marks an estimated size", secondary)
        }
        Spacer(Modifier.height(18.dp))
        SheetOptionCard(
            title = "Lyrics", subtitle = if (withLyrics) "Embedded if available" else "Not attached",
            primary = primary, secondary = secondary, checked = withLyrics, onCheckedChange = { withLyrics = it }
        )
        SheetSaveToCard("Music/Viaura", primary, secondary)
    }
    SheetLinkRow(info.pageUrl.ifBlank { url })
    Spacer(Modifier.height(4.dp))

    // ---- second sheets ----
    when (picker) {
        Picker.VIDEO -> DownloadPickerSheet("Video quality", isDark, glassBlurTransparency, primary, onDismiss = { picker = null }) {
            for (v in info.videos) {
                val (bytes, approx) = totalOf(v)
                val on = v.key == chosenVideo?.key
                SelectorRow(
                    badge = v.ext, title = v.label, chips = videoChips(v, bytes, approx), selected = on, primary = primary, secondary = secondary,
                    modifier = Modifier.padding(top = 10.dp),
                    onClick = { videoKey = v.key; picker = null },
                    trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                )
            }
        }
        Picker.AUDIO_TRACKS -> {
            val pool = if (audioNeeded) info.audioTracks else extraChoices
            val keys = if (audioNeeded) selectedAudioKeys else selectedExtraAudioKeys
            val allOn = pool.isNotEmpty() && pool.all { it.key in keys.value }
            DownloadPickerSheet(
                "Audio tracks", isDark, glassBlurTransparency, primary, onDismiss = { picker = null }, showDone = true,
                action = {
                    if (pool.size > 1) {
                        SheetTextAction(if (allOn) "Clear" else "Select all") {
                            keys.value = if (allOn) emptySet() else pool.map { it.key }.toSet()
                        }
                    }
                }
            ) {
                if (!audioNeeded) {
                    SelectorRow(
                        badge = "AUDIO", title = "Original audio", chips = listOf(SheetChip(if (chosenVideo?.muxed == true) "included in video" else "kept as in the file")),
                        selected = true, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                        leadingIconRes = R.drawable.viaura_audio_track,
                        trailing = SheetTrailing.Locked
                    )
                }
                for (a in pool) {
                    val on = a.key in keys.value
                    SelectorRow(
                        badge = a.ext.ifBlank { "audio" }, title = a.label, chips = audioChips(a), selected = on, primary = primary, secondary = secondary,
                        modifier = Modifier.padding(top = 10.dp),
                        leadingIconRes = R.drawable.viaura_audio_track,
                        onClick = {
                            val cur = keys.value
                            keys.value = if (a.key in cur) cur - a.key else cur + a.key
                        },
                        trailing = if (on) SheetTrailing.Checked else SheetTrailing.Unchecked
                    )
                }
                val picked = pool.filter { it.key in keys.value }
                SheetCaption(
                    if (picked.isEmpty()) if (audioNeeded) "No audio track selected; the video will download without audio." else "No extra track selected"
                    else "${picked.size} selected · ${if (audioApproxOf(picked)) "~" else ""}${DownloadFormat.bytes(audioBytesOf(picked))}" +
                        if (picked.size > 1 || !audioNeeded) " · all kept inside one MKV file" else "",
                    secondary
                )
            }
        }
        Picker.SUBTITLES -> {
            val allOn = selectedSubKeys.value.size == allSubKeys.size
            DownloadPickerSheet(
                "Subtitles", isDark, glassBlurTransparency, primary, onDismiss = { picker = null }, showDone = true,
                action = { SheetTextAction(if (allOn) "Clear" else "Select all") { selectedSubKeys.value = if (allOn) emptySet() else allSubKeys.toSet() } }
            ) {
                for (sb in info.subtitles) {
                    val key = subtitleKey(sb)
                    val on = key in selectedSubKeys.value
                    SelectorRow(
                        badge = sb.ext.ifBlank { "sub" }, title = sb.name,
                        chips = listOfNotNull(sb.language.takeIf { it.isNotBlank() }?.let { SheetChip(it) }, if (sb.isAuto) SheetChip("auto-generated", warn = true) else null),
                        selected = on, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                        onClick = { selectedSubKeys.value = if (on) selectedSubKeys.value - key else selectedSubKeys.value + key },
                        trailing = if (on) SheetTrailing.Checked else SheetTrailing.Unchecked
                    )
                }
                SheetCaption("Selected subtitles are kept inside one MKV file.", secondary)
            }
        }
        Picker.AUDIO_QUALITY -> DownloadPickerSheet("Audio quality", isDark, glassBlurTransparency, primary, onDismiss = { picker = null }) {
            for (a in audioQualityList) {
                val on = a.key == chosenAudioQ?.key
                SelectorRow(
                    badge = a.ext.ifBlank { "audio" }, title = audioQualityTitle(a), chips = audioChips(a), selected = on, primary = primary, secondary = secondary,
                    modifier = Modifier.padding(top = 10.dp), leadingIconRes = R.drawable.viaura_audio_track,
                    onClick = { audioQualityKey = a.key; picker = null },
                    trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                )
            }
        }
        null -> {}
    }
}
