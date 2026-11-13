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

private enum class ProviderPicker { SERVER, SUBTITLES }

private fun serverName(c: ServerCandidate, index: Int): String =
    c.stream.server.takeIf { it.isNotBlank() } ?: "Server ${index + 1}"

private fun serverChips(c: ServerCandidate, quality: String = c.quality): List<SheetChip> = listOfNotNull(
    c.format.takeIf { it.isNotBlank() }?.let { SheetChip(it) },
    quality.takeIf { it.isNotBlank() }?.let { SheetChip(it, accent = true) },
    SheetChip(if (c.sizeBytes > 0L) DownloadFormat.bytes(c.sizeBytes) else "size not provided", accent = true)
)

/**
 * Download panel of a movie / episode inside a provider: it first looks up which servers / storage links the provider
 * offers, tries each of them, and lists the working ones with their real file size and format. Same look as the other
 * download panels (header, thumbnail card, Title / Author boxes, rows that open a second sheet).
 */
@Composable
fun MediaDownloadSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    plugin: MediaPluginInfo,
    title: String,
    poster: String,
    link: String,
    type: String,
    onDismiss: () -> Unit,
    /** Series name: the episodes are saved in (and listed under) a folder of this name. Empty for a movie. */
    group: String = ""
) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ServersState>(ServersState.Loading) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(link, attempt) {
        state = ServersState.Loading
        try {
            val streams = MediaProviderRepository.loadStreams(context, plugin.value, link, type, fresh = attempt > 0).flatMap { listOf(it) + it.alternatives }.distinctBy { it.link }
            if (streams.isEmpty()) {
                state = ServersState.Failed("This provider has no download or stream link for this title.")
                return@LaunchedEffect
            }
            val app = context.applicationContext
            selectedIndex = 0
            // Settings > Network > Media: the download quality, the language and the server of the previous episode / download
            // decide which working server is on top (the quality is read from the file / playlist itself when the provider names none)
            val wantQuality = com.example.util.NetworkSettings.effectiveDownloadQuality(app)
            val sticky = com.example.util.media.MediaPreferences.stickyFor(app, plugin.value, group)
            val prefActive = com.example.util.media.MediaPreferences.active(app) || sticky != null || wantQuality > 0
            fun fit(s: MediaStream, quality: String?): Int =
                com.example.util.media.MediaPreferences.score(app, s, sticky, wantQuality, quality)
            val startedAt = android.os.SystemClock.elapsedRealtime()
            // every server is still tried, but the panel opens as soon as the first working one answers; the others join the list
            // as they finish (working ones are only ever appended, so the one that is selected never moves)
            val results = kotlinx.coroutines.channels.Channel<ServerCandidate>(streams.size)
            val gate = kotlinx.coroutines.sync.Semaphore(8)
            coroutineScope {
                streams.forEach { s -> launch(Dispatchers.IO) { gate.withPermit { results.send(tryServer(app, s)) } } }
                val done = ArrayList<ServerCandidate>()
                repeat(streams.size) {
                    done.add(results.receive())
                    val okList = done.filter { it.working }.let { l ->
                        if (prefActive) l.sortedByDescending { fit(it.stream, it.quality.ifBlank { it.stream.quality }) } else l
                    }
                    // The panel opens as soon as no server that is still being checked could fit the settings better than the best
                    // working one (or after 9 s): the server of the chosen language / quality is on top, not just the fastest one.
                    val pendingBest = streams.filter { s -> done.none { it.stream.link == s.link } }
                        .maxOfOrNull { fit(it, it.quality) } ?: Int.MIN_VALUE
                    val bestOk = okList.firstOrNull()?.let { fit(it.stream, it.quality.ifBlank { it.stream.quality }) }
                    val ready = done.size == streams.size ||
                        (bestOk != null && (!prefActive || bestOk >= pendingBest)) ||
                        (bestOk != null && android.os.SystemClock.elapsedRealtime() - startedAt > 9_000L)
                    if (ready) {
                        if (done.size < streams.size) selectedIndex = 0
                        state = ServersState.Ready(okList + done.filter { !it.working })
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (t: Throwable) {
            state = ServersState.Failed(t.message?.take(200) ?: "Could not load the links of this title")
        }
    }

    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss) {
        when (val s = state) {
            ServersState.Loading -> {
                DownloadSheetHeader(subtitle = "Adjust download", primary = primary, secondary = secondary, enabled = false) { }
                Text(title, fontSize = 14.sp, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 16.dp))
                Row(modifier = Modifier.padding(top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Looking for working servers…", fontSize = 13.sp, color = secondary)
                }
                // extra room below, like the loaded sheet, so nothing sits under the gesture bar
                Spacer(Modifier.height(64.dp))
            }
            is ServersState.Failed -> {
                DownloadSheetHeader(subtitle = "Adjust download", primary = primary, secondary = secondary, enabled = false) { }
                Text(s.message, fontSize = 13.sp, color = secondary, modifier = Modifier.padding(top = 16.dp, bottom = 14.dp))
                SheetButton("Retry", true) { attempt++ }
                Spacer(Modifier.height(56.dp))
            }
            is ServersState.Ready -> ProviderReady(
                servers = s.servers, selectedIndex = selectedIndex, onSelect = { selectedIndex = it }, isDark = isDark,
                glassBlurTransparency = glassBlurTransparency, primary = primary, secondary = secondary, plugin = plugin, title = title,
                poster = poster, onRetry = { attempt++ }, onDismiss = onDismiss, group = group
            )
        }
    }
}

@Composable
private fun ColumnScope.ProviderReady(
    servers: List<ServerCandidate>, selectedIndex: Int, onSelect: (Int) -> Unit, isDark: Boolean, glassBlurTransparency: Float,
    primary: Color, secondary: Color, plugin: MediaPluginInfo, title: String, poster: String, onRetry: () -> Unit, onDismiss: () -> Unit, group: String = ""
) {
    val context = LocalContext.current
    val working = servers.filter { it.working }
    val broken = servers.filter { !it.working }
    val chosen = working.getOrNull(selectedIndex)
    // Picture size of the selected plain file when the provider does not name it: read once, with a small download cap, never for every server
    val probedQuality = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    // The download quality of Settings > Network > Media (or the quality of the server used last for this series) decides which
    // rendition of an HLS master playlist is downloaded: it was always the highest one before.
    val app = context.applicationContext
    val wantQuality = remember { com.example.util.NetworkSettings.effectiveDownloadQuality(app) }
    val sticky = remember { com.example.util.media.MediaPreferences.stickyFor(app, plugin.value, group) }
    val hlsVariants = remember { androidx.compose.runtime.mutableStateMapOf<String, List<com.example.util.media.HlsVariant>>() }
    fun variantFor(c: ServerCandidate): com.example.util.media.HlsVariant? {
        if (!c.isHls) return null
        val list = hlsVariants[c.stream.link].orEmpty().filter { it.height > 0 }
        if (list.isEmpty()) return null
        val stickyHeight = sticky?.takeIf { it.server.trim().equals(c.stream.server.trim(), ignoreCase = true) }
            ?.let { com.example.util.media.MediaPreferences.heightOf(it.quality) } ?: 0
        val target = if (stickyHeight > 0) stickyHeight else wantQuality
        if (target <= 0) return null // best available, like before
        // the exact height; else the best one below it; else the lowest one above it
        return list.filter { it.height == target }.maxByOrNull { it.bandwidth }
            ?: list.filter { it.height < target }.maxWithOrNull(compareBy<com.example.util.media.HlsVariant>({ it.height }, { it.bandwidth }))
            ?: list.filter { it.height > target }.minWithOrNull(compareBy<com.example.util.media.HlsVariant>({ it.height }, { it.bandwidth }))
    }
    fun qualityOf(c: ServerCandidate): String =
        variantFor(c)?.label ?: c.quality.ifBlank { probedQuality[c.stream.link].orEmpty() }
    LaunchedEffect(chosen?.stream?.link) {
        val c = chosen ?: return@LaunchedEffect
        if (!c.isHls || hlsVariants.containsKey(c.stream.link)) return@LaunchedEffect
        hlsVariants[c.stream.link] = kotlinx.coroutines.withContext(Dispatchers.IO) {
            com.example.util.media.HlsVariants.fetch(c.stream.link, c.stream.headers)
        }
    }
    LaunchedEffect(chosen?.stream?.link) {
        val c = chosen ?: return@LaunchedEffect
        if (c.quality.isNotBlank() || c.isHls || probedQuality.containsKey(c.stream.link)) return@LaunchedEffect
        probedQuality[c.stream.link] = kotlinx.coroutines.runInterruptible(Dispatchers.IO) { probeFileQuality(app, c.stream, c.sizeBytes) }
    }
    var name by remember { mutableStateOf(title) }
    var author by remember { mutableStateOf(plugin.name) }
    var withThumb by remember { mutableStateOf(false) }
    var showBroken by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<ProviderPicker?>(null) }
    // the subtitles belong to the chosen server: the choice starts again (all selected) when another server is chosen
    val subs = chosen?.stream?.subtitles.orEmpty()
    val allSubKeys = subs.map { it.language + "|" + it.uri }
    val selectedSubKeys = remember(chosen?.stream?.link) { mutableStateOf(allSubKeys.toSet()) }
    val chosenSubs = subs.filter { (it.language + "|" + it.uri) in selectedSubKeys.value }

    // A provider's own MKV is saved byte for byte (its audio names, subtitles, chapters and fonts stay), so nothing is added to it
    val keepOriginal = chosen != null && !chosen.isHls && chosen.ext == "mkv"

    fun startDownload() {
        val c = chosen ?: return
        // an HLS master playlist: the rendition of the chosen download quality is taken, not always the highest one
        val partUrl = variantFor(c)?.uri ?: c.stream.link
        // the next episode / download of this series starts with this server and quality
        com.example.util.media.ServerReputationStore.rememberChoice(app, plugin.value, group, c.stream.server, qualityOf(c))
        val part = PlanPart(
            role = "muxed", url = partUrl, headers = c.stream.headers, sizeBytes = c.sizeBytes,
            ext = c.ext.ifBlank { "mp4" }, protocol = if (c.isHls) "hls" else "http", formatId = "provider"
        )
        val planSubs = (if (keepOriginal) emptyList() else chosenSubs).map { sb ->
            val e = sb.uri.substringBefore('?').substringAfterLast('.', "vtt").lowercase().takeIf { it in setOf("vtt", "srt", "ass", "ssa") } ?: "vtt"
            PlanSubtitle(sb.language, sb.title.ifBlank { sb.language.ifBlank { "Subtitle" } }, sb.uri, e, false)
        }
        val plan = DownloadPlan(
            pageUrl = c.stream.link, title = name.trim().ifBlank { title }, uploader = author.trim(), thumbnailUrl = poster, isAudio = false,
            parts = listOf(part), subtitles = planSubs, attachThumbnail = !keepOriginal && withThumb && poster.isNotBlank(),
            qualityLabel = qualityOf(c), group = group.trim()
        )
        val mkv = c.isHls || planSubs.isNotEmpty() || (!keepOriginal && withThumb && poster.isNotBlank())
        val detail = listOfNotNull(qualityOf(c).takeIf { it.isNotBlank() }, (if (mkv) "MKV" else c.format).takeIf { it.isNotBlank() }).joinToString(" · ")
        val msg = DownloadManager.enqueuePlan(context, plan, DownloadSourceType.MEDIA_PROVIDER, detail, c.sizeBytes.coerceAtLeast(0L))
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    DownloadSheetHeader(subtitle = "Adjust download", primary = primary, secondary = secondary, enabled = chosen != null) { startDownload() }
    Spacer(Modifier.height(16.dp))

    // large cover (the provider's poster; a generated one when the provider gives none) with Title and Author beside it
    SheetCoverTitleAuthor(
        thumbnail = poster, title = name, onTitleChange = { name = it }, author = author, onAuthorChange = { author = it },
        primary = primary, secondary = secondary
    )

    SheetSectionLabel("Server", primary)
    if (chosen == null) {
        Text("None of the servers answers right now.", fontSize = 14.sp, color = secondary, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        SheetButton("Retry", true) { onRetry() }
    } else {
        SelectorRow(
            badge = chosen.ext.ifBlank { if (chosen.isHls) "HLS" else "VIDEO" }, title = serverName(chosen, selectedIndex), chips = serverChips(chosen, qualityOf(chosen)),
            selected = true, primary = primary, secondary = secondary,
            onClick = if (working.size > 1 || broken.isNotEmpty()) { { picker = ProviderPicker.SERVER } } else null,
            trailing = SheetTrailing.Tick
        )
        SheetCaption("${working.size} working ${if (working.size == 1) "server" else "servers"}" + (if (broken.isNotEmpty()) " • ${broken.size} not working" else "") + " • tap above to change", secondary)
    }

    if (subs.isNotEmpty() && keepOriginal) {
        SheetCaption("This MKV is saved exactly as the provider made it, with all its own audio names, subtitles, chapters and fonts.", secondary)
    } else if (subs.isNotEmpty()) {
        SheetSectionLabel("Subtitles", primary)
        SelectorRow(
            badge = "SUB",
            title = when {
                chosenSubs.isEmpty() -> "No subtitles"
                chosenSubs.size <= 2 -> chosenSubs.joinToString(", ") { it.title.ifBlank { it.language.ifBlank { "Subtitle" } } }
                else -> "${chosenSubs.size} subtitles"
            },
            chips = listOf(SheetChip("${chosenSubs.size} of ${subs.size} selected")),
            selected = chosenSubs.isNotEmpty(), primary = primary, secondary = secondary,
            onClick = { picker = ProviderPicker.SUBTITLES },
            trailing = if (chosenSubs.isNotEmpty()) SheetTrailing.Tick else SheetTrailing.None
        )
        if (chosenSubs.isNotEmpty()) SheetCaption("Selected subtitles are kept inside one MKV file.", secondary)
    }

    SheetSaveToCard(
        "Movies/Viaura" + (if (group.isNotBlank()) "/" + com.example.util.DownloadFileSaver.safeName(group) else ""),
        primary, secondary
    )
    if (chosen != null) SheetLinkRow(chosen.stream.link)
    Spacer(Modifier.height(4.dp))

    when (picker) {
        ProviderPicker.SERVER -> DownloadPickerSheet("Servers", isDark, glassBlurTransparency, primary, onDismiss = { picker = null }) {
            working.forEachIndexed { i, c ->
                val on = i == selectedIndex
                SelectorRow(
                    badge = c.ext.ifBlank { if (c.isHls) "HLS" else "VIDEO" }, title = serverName(c, i), chips = serverChips(c, qualityOf(c)), selected = on,
                    primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                    onClick = { onSelect(i); picker = null },
                    trailing = if (on) SheetTrailing.Tick else SheetTrailing.None
                )
            }
            if (broken.isNotEmpty()) {
                Text(
                    if (showBroken) "Hide ${broken.size} not working" else "${broken.size} not working",
                    fontSize = 13.sp, color = secondary,
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showBroken = !showBroken }.padding(top = 14.dp, bottom = 4.dp, start = 4.dp, end = 8.dp)
                )
                if (showBroken) {
                    for (b in broken) {
                        Text(
                            listOfNotNull(b.stream.server.takeIf { it.isNotBlank() }, b.stream.quality).joinToString(" · ") + " — " + b.reason,
                            fontSize = 12.sp, color = secondary.copy(alpha = 0.75f), modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
                        )
                    }
                }
            }
        }
        ProviderPicker.SUBTITLES -> {
            val allOn = selectedSubKeys.value.size == allSubKeys.size
            DownloadPickerSheet(
                "Subtitles", isDark, glassBlurTransparency, primary, onDismiss = { picker = null }, showDone = true,
                action = { SheetTextAction(if (allOn) "Clear" else "Select all") { selectedSubKeys.value = if (allOn) emptySet() else allSubKeys.toSet() } }
            ) {
                for (sb in subs) {
                    val key = sb.language + "|" + sb.uri
                    val on = key in selectedSubKeys.value
                    SelectorRow(
                        badge = sb.uri.substringBefore('?').substringAfterLast('.', "").lowercase().takeIf { it in setOf("vtt", "srt", "ass", "ssa") } ?: "sub",
                        title = sb.title.ifBlank { sb.language.ifBlank { "Subtitle" } },
                        chips = listOfNotNull(sb.language.takeIf { it.isNotBlank() }?.let { SheetChip(it) }),
                        selected = on, primary = primary, secondary = secondary, modifier = Modifier.padding(top = 10.dp),
                        onClick = { selectedSubKeys.value = if (on) selectedSubKeys.value - key else selectedSubKeys.value + key },
                        trailing = if (on) SheetTrailing.Checked else SheetTrailing.Unchecked
                    )
                }
            }
        }
        null -> {}
    }
}

@Composable
private fun SheetButton(text: String, enabled: Boolean, onClick: () -> Unit) {
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
