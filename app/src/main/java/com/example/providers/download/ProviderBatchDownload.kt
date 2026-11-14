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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.download.DownloadPlan
import com.example.download.PlanPart
import com.example.download.PlanSubtitle
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.media.HlsVariant
import com.example.util.media.HlsVariants
import com.example.util.media.MediaPluginInfo
import com.example.util.media.MediaPreferences
import com.example.util.media.MediaProviderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Several episodes (or movies) of one provider title that are downloaded in one go.
 * [name] is how the file is saved, [matchTitle] / [index] are only used to find the same episode inside another
 * quality variant of the title.
 */
internal data class BatchEpisode(
    val name: String,
    val matchTitle: String,
    val index: Int,
    val link: String,
    val type: String,
    val poster: String,
    /** Shown / edited in the panel; empty = the provider's name. */
    val author: String = ""
)

/** One quality variant of the title ("1080p x264", "1080p 10Bit HEVC" ...) and a way to read its episode list. */
internal class BatchVariant(
    val label: String,
    /** The variant the episodes were selected in (their links are used as they are). */
    val isCurrent: Boolean,
    /** The episode list of this variant (null = could not be read). */
    val episodes: suspend () -> List<BatchEpisode>?
)

// ---------------------------------------------------------------------------------------------------------------------
// Matching the same episode in another variant
// ---------------------------------------------------------------------------------------------------------------------

private val EP_NUMBER_RE = Regex("(?i)(?:\\be|\\bep|episode)\\s*\\.?\\s*0*(\\d{1,4})\\b")
private val SEASON_NUMBER_RE = Regex("(?i)(?:\\bseason\\s*:?\\s*0*(\\d{1,2})\\b|\\bs0*(\\d{1,2})(?=\\s*e\\d))")

private fun episodeNumberOf(title: String): Int? = EP_NUMBER_RE.find(title)?.groupValues?.get(1)?.toIntOrNull()

private fun seasonNumberOf(title: String): Int? =
    SEASON_NUMBER_RE.find(title)?.let { m -> (m.groupValues[1].ifBlank { m.groupValues[2] }).toIntOrNull() }

private fun plainTitle(t: String): String = t.lowercase().filter { it.isLetterOrDigit() }

/** The episode of [list] that is the same one as [sel]: season + number, the number, the title, else the same position. */
private fun matchEpisode(sel: BatchEpisode, list: List<BatchEpisode>): BatchEpisode? {
    val num = episodeNumberOf(sel.matchTitle)
    val season = seasonNumberOf(sel.matchTitle)
    if (num != null) {
        val sameNumber = list.filter { episodeNumberOf(it.matchTitle) == num }
        if (sameNumber.size == 1) return sameNumber[0]
        if (sameNumber.size > 1) {
            sameNumber.firstOrNull { season != null && seasonNumberOf(it.matchTitle) == season }?.let { return it }
            sameNumber.firstOrNull { it.index == sel.index }?.let { return it }
            return sameNumber[0]
        }
    }
    val key = plainTitle(sel.matchTitle)
    if (key.isNotEmpty()) list.firstOrNull { plainTitle(it.matchTitle) == key }?.let { return it }
    return list.firstOrNull { it.index == sel.index }
}

// ---------------------------------------------------------------------------------------------------------------------
// The download itself (runs on its own scope, so closing the sheet does not stop it)
// ---------------------------------------------------------------------------------------------------------------------

/** What the first episode chose: every other episode is matched to it (same picture size, same server, close file size). */
internal class BatchReference(val server: String, val height: Int, val sizeBytes: Long)

/**
 * One thing an episode can be downloaded as: a working server / storage link, and for an HLS playlist one of its renditions.
 * The panel lists these per episode; what is chosen here is exactly what gets downloaded.
 */
internal class QOption(val c: ServerCandidate, val variant: HlsVariant?) {
    val label: String get() = variant?.label?.takeIf { it.isNotBlank() } ?: c.quality.ifBlank { c.stream.quality.orEmpty() }
    val height: Int get() = if (variant != null && variant.height > 0) variant.height else MediaPreferences.heightOf(label)
    /** Size of the file; -1 when it is not known (a rendition of a playlist has no fixed size). */
    val sizeBytes: Long get() = if (variant != null) -1L else c.sizeBytes
    val key: String get() = c.stream.link + "|" + (variant?.uri.orEmpty())
}

internal object ProviderBatchDownloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())

    private fun toast(app: Context, text: String) {
        main.post { Toast.makeText(app, text, Toast.LENGTH_LONG).show() }
    }

    private fun heightOf(c: ServerCandidate): Int =
        MediaPreferences.heightOf(c.quality.ifBlank { c.stream.quality.orEmpty() })

    /**
     * The working server for one episode. The first episode takes the best fit for the Media settings (like the single
     * download panel). Every next episode takes, among its working servers, the ones with the same picture size as the
     * first one and then the file whose size is the closest to the first one's, so no episode is much bigger or smaller.
     */
    private fun choose(
        app: Context, working: List<ServerCandidate>, ref: BatchReference?,
        sticky: com.example.util.media.ServerReputationStore.Choice?, wantQuality: Int
    ): ServerCandidate {
        if (ref == null) {
            val active = MediaPreferences.active(app) || sticky != null || wantQuality > 0
            if (!active) return working.first()
            return working.maxByOrNull { MediaPreferences.score(app, it.stream, sticky, wantQuality, it.quality.ifBlank { it.stream.quality }) } ?: working.first()
        }
        var list = working
        if (ref.height > 0) {
            val same = list.filter { heightOf(it) == ref.height }
            if (same.isNotEmpty()) {
                list = same
            } else {
                val known = list.filter { heightOf(it) > 0 }
                if (known.isNotEmpty()) {
                    val d = known.minOf { abs(heightOf(it) - ref.height) }
                    list = known.filter { abs(heightOf(it) - ref.height) == d }
                }
            }
        }
        val sameServer = list.filter { it.stream.server.trim().equals(ref.server.trim(), ignoreCase = true) }
        if (ref.sizeBytes > 0L) {
            val sized = list.filter { it.sizeBytes > 0L }
            if (sized.isNotEmpty()) {
                val best = sized.minOf { abs(it.sizeBytes - ref.sizeBytes) }
                // equally close: the server of the first episode wins
                val closest = sized.filter { abs(it.sizeBytes - ref.sizeBytes) == best }
                return closest.firstOrNull { c -> sameServer.any { it.stream.link == c.stream.link } } ?: closest.first()
            }
        }
        return sameServer.firstOrNull() ?: list.first()
    }

    /**
     * The same rule as [choose], over the options of the panel: the first episode takes the best fit for the settings
     * (download quality, language, server), every next one the same picture size as the first, the nearest size to it.
     */
    fun chooseOption(
        app: Context, options: List<QOption>, ref: BatchReference?,
        sticky: com.example.util.media.ServerReputationStore.Choice?, wantQuality: Int
    ): QOption? {
        if (options.isEmpty()) return null
        if (ref == null) {
            // the download quality of the settings: exact, else the nearest lower, else the nearest higher one
            if (wantQuality > 0) {
                val known = options.filter { it.height > 0 }
                val pool = known.filter { it.height == wantQuality }.ifEmpty {
                    known.filter { it.height < wantQuality }.let { lower ->
                        if (lower.isEmpty()) emptyList() else { val h = lower.maxOf { it.height }; lower.filter { it.height == h } }
                    }
                }.ifEmpty {
                    if (known.isEmpty()) emptyList() else { val h = known.minOf { it.height }; known.filter { it.height == h } }
                }
                if (pool.isNotEmpty()) {
                    return pool.maxByOrNull { MediaPreferences.score(app, it.c.stream, sticky, wantQuality, it.label) } ?: pool.first()
                }
            }
            return options.maxByOrNull { MediaPreferences.score(app, it.c.stream, sticky, wantQuality, it.label) } ?: options.first()
        }
        var list = options
        if (ref.height > 0) {
            val same = list.filter { it.height == ref.height }
            if (same.isNotEmpty()) list = same else {
                val known = list.filter { it.height > 0 }
                if (known.isNotEmpty()) {
                    val d = known.minOf { abs(it.height - ref.height) }
                    list = known.filter { abs(it.height - ref.height) == d }
                }
            }
        }
        val sameServer = list.filter { it.c.stream.server.trim().equals(ref.server.trim(), ignoreCase = true) }
        if (ref.sizeBytes > 0L) {
            val sized = list.filter { it.sizeBytes > 0L }
            if (sized.isNotEmpty()) {
                val best = sized.minOf { abs(it.sizeBytes - ref.sizeBytes) }
                val closest = sized.filter { abs(it.sizeBytes - ref.sizeBytes) == best }
                return closest.firstOrNull { o -> sameServer.any { it.key == o.key } } ?: closest.first()
            }
        }
        return sameServer.firstOrNull() ?: list.first()
    }

    /** The rendition of an HLS master playlist: the exact height, else the best one below it, else the lowest above. */
    private fun variantFor(list: List<HlsVariant>, target: Int): HlsVariant? {
        val l = list.filter { it.height > 0 }
        if (l.isEmpty() || target <= 0) return null
        return l.filter { it.height == target }.maxByOrNull { it.bandwidth }
            ?: l.filter { it.height < target }.maxWithOrNull(compareBy<HlsVariant>({ it.height }, { it.bandwidth }))
            ?: l.filter { it.height > target }.minWithOrNull(compareBy<HlsVariant>({ it.height }, { it.bandwidth }))
    }

    fun start(context: Context, plugin: MediaPluginInfo, group: String, episodes: List<BatchEpisode>, picks: Map<String, QOption> = emptyMap()) {
        val app = context.applicationContext
        if (episodes.isEmpty()) return
        toast(app, "Preparing ${episodes.size} downloads…")
        scope.launch {
            var started = 0
            var failed = 0
            var ref: BatchReference? = null
            val wantQuality = com.example.util.NetworkSettings.effectiveDownloadQuality(app)
            val sticky = MediaPreferences.stickyFor(app, plugin.value, group)
            val gate = Semaphore(8)
            for (ep in episodes) {
                try {
                    // what the panel showed for this episode is exactly what is downloaded
                    val pick = picks[ep.link]
                    val c: ServerCandidate
                    val variant: HlsVariant?
                    if (pick != null) {
                        c = pick.c
                        variant = pick.variant
                    } else {
                        val streams = MediaProviderRepository.loadStreams(app, plugin.value, ep.link, ep.type)
                            .flatMap { listOf(it) + it.alternatives }.distinctBy { it.link }
                        val tried: List<ServerCandidate> = coroutineScope {
                            streams.map { s -> async(Dispatchers.IO) { gate.withPermit { tryServer(app, s) } } }.awaitAll()
                        }
                        val working = tried.filter { it.working }
                        if (working.isEmpty()) { failed++; continue }
                        val chosenCandidate = choose(app, working, ref, if (ref == null) sticky else null, wantQuality)

                        // HLS: the rendition of the same height as the first episode (or of the chosen download quality)
                        val chosenVariant: HlsVariant? = if (chosenCandidate.isHls) {
                            val list = try { HlsVariants.fetch(chosenCandidate.stream.link, chosenCandidate.stream.headers) } catch (_: Throwable) { emptyList() }
                            val stickyHeight = if (ref == null) sticky?.takeIf { it.server.trim().equals(chosenCandidate.stream.server.trim(), true) }
                                ?.let { MediaPreferences.heightOf(it.quality) } ?: 0 else 0
                            val target = when {
                                ref != null && ref.height > 0 -> ref.height
                                stickyHeight > 0 -> stickyHeight
                                else -> wantQuality
                            }
                            variantFor(list, target)
                        } else null
                        c = chosenCandidate
                        variant = chosenVariant
                    }
                    val qualityText = variant?.label ?: c.quality.ifBlank { c.stream.quality.orEmpty() }

                    if (ref == null) {
                        ref = BatchReference(c.stream.server, MediaPreferences.heightOf(qualityText), if (c.isHls) -1L else c.sizeBytes)
                        com.example.util.media.ServerReputationStore.rememberChoice(app, plugin.value, group, c.stream.server, qualityText)
                    }

                    val keepOriginal = !c.isHls && c.ext == "mkv"
                    val part = PlanPart(
                        role = "muxed", url = variant?.uri ?: c.stream.link, headers = c.stream.headers, sizeBytes = c.sizeBytes,
                        ext = c.ext.ifBlank { "mp4" }, protocol = if (c.isHls) "hls" else "http", formatId = "provider"
                    )
                    val planSubs = (if (keepOriginal) emptyList() else c.stream.subtitles).map { sb ->
                        val e = sb.uri.substringBefore('?').substringAfterLast('.', "vtt").lowercase().takeIf { it in setOf("vtt", "srt", "ass", "ssa") } ?: "vtt"
                        PlanSubtitle(sb.language, sb.title.ifBlank { sb.language.ifBlank { "Subtitle" } }, sb.uri, e, false)
                    }
                    val plan = DownloadPlan(
                        pageUrl = c.stream.link, title = ep.name.trim().ifBlank { "Download" }, uploader = ep.author.trim().ifBlank { plugin.name }, thumbnailUrl = ep.poster, isAudio = false,
                        parts = listOf(part), subtitles = planSubs, attachThumbnail = false,
                        qualityLabel = qualityText, group = group.trim()
                    )
                    val mkv = c.isHls || planSubs.isNotEmpty()
                    val detail = listOfNotNull(qualityText.takeIf { it.isNotBlank() }, (if (mkv) "MKV" else c.format).takeIf { it.isNotBlank() }).joinToString(" · ")
                    DownloadManager.enqueuePlan(app, plan, DownloadSourceType.MEDIA_PROVIDER, detail, c.sizeBytes.coerceAtLeast(0L))
                    started++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    failed++
                }
            }
            toast(
                app,
                when {
                    started == 0 -> "No download could be started"
                    failed == 0 -> "$started downloads started"
                    else -> "$started downloads started, $failed had no working server"
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// The panel
// ---------------------------------------------------------------------------------------------------------------------

/**
 * Download panel for several selected episodes: one quality variant for all of them, the same server and a file size close
 * to the first episode for every one. Same look as [MediaDownloadSheet].
 */
@Composable
internal fun MediaBatchDownloadSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    plugin: MediaPluginInfo,
    group: String,
    episodes: List<BatchEpisode>,
    variants: List<BatchVariant>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    var variantIndex by remember { mutableIntStateOf(variants.indexOfFirst { it.isCurrent }.coerceAtLeast(0)) }
    var showPicker by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    val chosen = variants.getOrNull(variantIndex)
    // title / author of every selected episode, editable one by one
    val names = remember(episodes) { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(episodes.map { it.name }) } }
    val authors = remember(episodes) { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(episodes.map { it.author.ifBlank { plugin.name } }) } }
    // the servers / qualities of every episode (searched in the background) and the one the person picked for it
    val options = remember(episodes) { androidx.compose.runtime.mutableStateListOf<List<QOption>?>().apply { addAll(episodes.map { null }) } }
    val failedRows = remember(episodes) { androidx.compose.runtime.mutableStateListOf<Boolean>().apply { addAll(episodes.map { false }) } }
    val pickedKeys = remember(episodes) { androidx.compose.runtime.mutableStateListOf<String?>().apply { addAll(episodes.map { null }) } }
    var pickerIndex by remember { mutableStateOf<Int?>(null) }
    val wantQuality = remember { com.example.util.NetworkSettings.effectiveDownloadQuality(context.applicationContext) }
    val sticky = remember { MediaPreferences.stickyFor(context.applicationContext, plugin.value, group) }

    androidx.compose.runtime.LaunchedEffect(episodes) {
        val app = context.applicationContext
        val gate = Semaphore(8)
        val episodeGate = Semaphore(2)
        coroutineScope {
            episodes.mapIndexed { i, ep ->
                async(Dispatchers.IO) {
                    episodeGate.withPermit {
                        try {
                            val streams = MediaProviderRepository.loadStreams(app, plugin.value, ep.link, ep.type)
                                .flatMap { listOf(it) + it.alternatives }.distinctBy { it.link }
                            val working = coroutineScope {
                                streams.map { st -> async { gate.withPermit { tryServer(app, st) } } }.awaitAll()
                            }.filter { it.working }
                            val list = ArrayList<QOption>()
                            for (c in working) {
                                if (c.isHls) {
                                    val renditions = try { HlsVariants.fetch(c.stream.link, c.stream.headers) } catch (_: Throwable) { emptyList() }
                                        .filter { it.height > 0 }
                                        .groupBy { it.height }.map { (_, g) -> g.maxByOrNull { it.bandwidth }!! }
                                    if (renditions.isEmpty()) list.add(QOption(c, null)) else renditions.forEach { list.add(QOption(c, it)) }
                                } else list.add(QOption(c, null))
                            }
                            list.sortWith(compareByDescending<QOption> { it.height }.thenByDescending { it.sizeBytes })
                            if (list.isEmpty()) { failedRows[i] = true } else { options[i] = list }
                            Unit
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (_: Throwable) {
                            failedRows[i] = true
                        }
                        Unit
                    }
                }
            }.awaitAll()
        }
    }

    // what every episode gets right now: its own pick, else the automatic one (first episode by the settings, the others
    // matched to it by picture size, server and file size)
    val chosenOptions: List<QOption?> = run {
        var ref: BatchReference? = null
        episodes.indices.map { i ->
            val list = options[i]
            val o = if (list == null) null
                else list.firstOrNull { it.key == pickedKeys[i] } ?: ProviderBatchDownloader.chooseOption(context.applicationContext, list, ref, if (ref == null) sticky else null, wantQuality)
            if (ref == null && o != null) ref = BatchReference(o.c.stream.server, o.height, o.sizeBytes)
            o
        }
    }

    fun startAll() {
        val v = chosen ?: return
        if (preparing) return
        preparing = true
        val episodes = episodes.mapIndexed { i, e -> e.copy(name = names.getOrNull(i)?.ifBlank { e.name } ?: e.name, author = authors.getOrNull(i).orEmpty()) }
        scope.launch {
            val list: List<BatchEpisode>? = if (v.isCurrent) episodes else withContext(Dispatchers.IO) {
                val other = try { v.episodes() } catch (_: Throwable) { null }
                other?.let { o -> episodes.mapNotNull { sel -> matchEpisode(sel, o)?.let { m -> sel.copy(link = m.link, type = m.type) } } }
            }
            if (list == null) {
                preparing = false
                Toast.makeText(context, "Could not read the episodes of this quality", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (list.size < episodes.size) {
                Toast.makeText(context, "${episodes.size - list.size} episodes are not in this quality and are skipped", Toast.LENGTH_SHORT).show()
            }
            val picks = if (v.isCurrent) {
                val map = HashMap<String, QOption>()
                episodes.forEachIndexed { i, e -> chosenOptions.getOrNull(i)?.let { map[e.link] = it } }
                map
            } else emptyMap()
            ProviderBatchDownloader.start(context, plugin, group, list, picks)
            onDismiss()
        }
    }

    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss) {
        DownloadSheetHeader(
            title = "Download", subtitle = "${episodes.size} selected · same quality for all",
            primary = primary, secondary = secondary, enabled = chosen != null && !preparing
        ) { startAll() }
        Spacer(Modifier.height(10.dp))

        SheetSectionLabel("Selected (${episodes.size})", primary)
        // one below the other: cover (the provider's poster, a generated one when there is none) + title + author, editable
        episodes.forEachIndexed { i, e ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            SheetCoverTitleAuthor(
                thumbnail = e.poster, title = names.getOrElse(i) { e.name }, onTitleChange = { if (i < names.size) names[i] = it },
                author = authors.getOrElse(i) { plugin.name }, onAuthorChange = { if (i < authors.size) authors[i] = it },
                primary = primary, secondary = secondary, coverWidth = 112.dp
            )
            // this episode's own server / quality (searched for each episode); tap to pick another one
            val o = chosenOptions.getOrNull(i)
            val opts = options.getOrNull(i)
            if (failedRows.getOrElse(i) { false }) {
                Text("No working server found for this episode", fontSize = 13.sp, color = androidx.compose.ui.graphics.Color(0xFFEF4444), modifier = Modifier.padding(start = 6.dp, top = 8.dp))
            } else if (o == null || opts == null) {
                Row(modifier = Modifier.padding(start = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Searching qualities…", fontSize = 13.sp, color = secondary)
                }
            } else {
                SelectorRow(
                    badge = o.c.ext.ifBlank { if (o.c.isHls) "HLS" else "VIDEO" }.uppercase(),
                    title = o.label.ifBlank { "Quality not named" },
                    chips = listOfNotNull(
                        o.c.stream.server.takeIf { it.isNotBlank() }?.let { SheetChip(it) },
                        SheetChip(if (o.sizeBytes > 0L) com.example.util.DownloadFormat.bytes(o.sizeBytes) else "size not provided", accent = true),
                        if (pickedKeys[i] != null) SheetChip("your choice") else SheetChip("automatic")
                    ),
                    selected = true, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                    onClick = if (opts.size > 1) { { pickerIndex = i } } else null,
                    trailing = SheetTrailing.Tick
                )
            }
        }

        SheetSectionLabel(if (variants.size > 1) "Quality variant" else "Quality", primary)
        if (chosen != null) {
            SelectorRow(
                badge = "ALL", title = chosen.label.ifBlank { "Default" },
                chips = listOf(SheetChip("${episodes.size} episodes", accent = true)),
                selected = true, primary = primary, secondary = secondary,
                onClick = if (variants.size > 1) { { showPicker = true } } else null,
                trailing = SheetTrailing.Tick
            )
            SheetCaption(
                "Under every episode you see its own quality and size. Tap it to pick another one for that episode. Without a choice the download quality of Settings › Network is used for the first episode, and each next one gets the same picture size, the same server and a file of about the same size." +
                    if (variants.size > 1) " Choosing another variant here replaces these picks (each episode is matched automatically)." else "",
                secondary
            )
        }
        if (preparing) {
            Row(modifier = Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                Spacer(Modifier.width(12.dp))
                Text("Preparing…", fontSize = 13.sp, color = secondary)
            }
        }
        SheetSaveToCard(
            "Movies/Viaura" + (if (group.isNotBlank()) "/" + com.example.util.DownloadFileSaver.safeName(group) else ""),
            primary, secondary
        )
        Spacer(Modifier.height(4.dp))
    }

    pickerIndex?.let { pi ->
        val opts = options.getOrNull(pi)
        val currentKey = chosenOptions.getOrNull(pi)?.key
        if (opts != null) {
            DownloadPickerSheet("Quality · " + (names.getOrNull(pi) ?: "").take(28), isDark, glassBlurTransparency, primary, onDismiss = { pickerIndex = null }) {
                opts.forEach { o ->
                    val on = o.key == currentKey
                    SelectorRow(
                        badge = o.c.ext.ifBlank { if (o.c.isHls) "HLS" else "VIDEO" }.uppercase(),
                        title = o.label.ifBlank { "Quality not named" },
                        chips = listOfNotNull(
                            o.c.stream.server.takeIf { it.isNotBlank() }?.let { SheetChip(it) },
                            o.c.format.takeIf { it.isNotBlank() && o.variant == null }?.let { SheetChip(it) },
                            SheetChip(if (o.sizeBytes > 0L) com.example.util.DownloadFormat.bytes(o.sizeBytes) else "size not provided", accent = true)
                        ),
                        selected = on, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                        onClick = { pickedKeys[pi] = o.key; pickerIndex = null },
                        trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                    )
                }
            }
        }
    }

    if (showPicker) {
        DownloadPickerSheet("Quality", isDark, glassBlurTransparency, primary, onDismiss = { showPicker = false }) {
            variants.forEachIndexed { i, v ->
                val on = i == variantIndex
                SelectorRow(
                    badge = "ALL", title = v.label.ifBlank { "Default" }, chips = if (v.isCurrent) listOf(SheetChip("selected on the page")) else emptyList(),
                    selected = on, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                    onClick = { variantIndex = i; showPicker = false },
                    trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                )
            }
        }
    }
}

