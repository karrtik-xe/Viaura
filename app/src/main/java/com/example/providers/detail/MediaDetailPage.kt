package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DownloadGlassSheet
import com.example.ui.components.HighResImage
import com.example.ui.components.MangaReaderScreen
import com.example.ui.components.ViauraExpressiveLoadingIndicator
import com.example.ui.components.OnlineQuickActionBar
import com.example.ui.components.StyledIcon
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VisibleFields
import com.example.ui.components.shareOnlineLink
import com.example.ui.components.swipeLeftForActions
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.media.MediaDirectLink
import com.example.util.media.MediaInfo
import com.example.util.media.MediaLibraryStore
import com.example.util.media.MediaLink
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaPluginInfo
import com.example.util.media.PluginException
import com.example.util.media.PluginPlaybackSession
import com.example.util.media.PluginQueuedEpisode
import com.example.util.media.PluginQueuedEpisodes
import com.example.util.media.PluginQualityChoice
import com.example.util.media.MediaPost
import com.example.util.media.MediaProviderRepository
import com.example.util.media.MediaSavedVideo
import com.example.util.media.MediaHistoryStore
import com.example.util.media.MediaWatchedTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun MediaDetailScreen(
    plugin: MediaPluginInfo,
    post: MediaPost,
    isDark: Boolean,
    glassBlurTransparency: Float,
    onBack: () -> Unit,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val pageBg = Color.Transparent

    // ---- title details (getMeta) ----
    // A title that was opened before is known at once (memory / disk): the page is complete on its very first frame.
    var info by remember(plugin.value, post.link) {
        mutableStateOf<MediaInfo?>(MediaProviderRepository.peekInfo(context, plugin.value, post.link))
    }
    var infoError by remember(plugin.value, post.link) { mutableStateOf<String?>(null) }
    var reload by remember(plugin.value, post.link) { mutableIntStateOf(0) }
    LaunchedEffect(plugin.value, post.link, reload) {
        if (reload == 0 && info != null) {
            // already on screen from the saved copy; MediaProviderRepository refreshes an old copy silently
            infoError = null
            MediaProviderRepository.loadInfo(context, plugin.value, post.link, force = false)
            return@LaunchedEffect
        }
        // a pull-down refresh keeps the old page visible until the new details arrive
        if (reload == 0) info = null
        infoError = null
        try {
            info = MediaProviderRepository.loadInfo(context, plugin.value, post.link, force = reload > 0)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (info == null) infoError = t.message ?: "Could not load this title"
        } finally {
            MediaDetailState.providerRefreshing = false
        }
    }

    // pull-down on the title page (the Home screen bumps the request): details and episodes are loaded again
    val refreshTick = MediaDetailState.refreshRequest
    var handledRefresh by remember(plugin.value, post.link) { mutableIntStateOf(refreshTick) }
    var titleRefreshes by remember(plugin.value, post.link) { mutableIntStateOf(0) }
    LaunchedEffect(refreshTick) {
        if (refreshTick != handledRefresh) {
            handledRefresh = refreshTick
            MediaProviderRepository.clearTitle(context, plugin.value, post.link)
            reload++
            titleRefreshes++
        }
    }
    // the refresh animation can never stay on screen for long, whatever happened
    LaunchedEffect(MediaDetailState.providerRefreshing) {
        if (MediaDetailState.providerRefreshing) {
            kotlinx.coroutines.delay(8_000L)
            MediaDetailState.providerRefreshing = false
        }
    }

    val loaded = info
    val groups = remember(loaded) { if (loaded == null) emptyList() else buildGroups(loaded) }
    val isSeries = loaded != null &&
        (loaded.type.equals("series", true) || loaded.linkList.any { it.episodesLink != null })
    val title = loaded?.title?.takeIf { it.isNotBlank() } ?: post.title
    val backdrop = loaded?.image?.takeIf { it.isNotBlank() } ?: loaded?.poster?.takeIf { it.isNotBlank() } ?: post.image
    val poster = loaded?.poster?.takeIf { it.isNotBlank() } ?: post.image.ifBlank { loaded?.image.orEmpty() }

    // ---- season / audio / quality selection (remembered for this title) ----
    val selKey = plugin.value + "|" + post.link
    // A choice remembered for this title only counts while Settings > Network > Media (language / quality) is unchanged:
    // a new setting must apply to the title again.
    val settingsSig = com.example.util.NetworkSettings.mediaLanguage(context).hashCode() * 31 + com.example.util.NetworkSettings.mediaPlayQuality(context)
    fun remembered(): IntArray? = MediaDetailState.selections[selKey]?.takeIf { it.getOrNull(4) == settingsSig }
    var seasonIdx by remember(selKey) { mutableIntStateOf(remembered()?.getOrNull(0) ?: 0) }
    // No choice remembered for this title yet: start with the audio (language) and quality of Settings > Network > Media settings
    val firstAudioDefault = remember(selKey, groups) { preferredAudioIndex(context, groups.getOrNull(0)) }
    var audioIdx by remember(selKey) { mutableIntStateOf(remembered()?.getOrNull(1) ?: firstAudioDefault) }
    var qualityIdx by remember(selKey) {
        mutableIntStateOf(
            remembered()?.getOrNull(2)
                ?: preferredQualityIndex(context, groups.getOrNull(0)?.audios?.getOrNull(firstAudioDefault))
        )
    }
    // the title's details arrive after the page opened: apply the same defaults once they are there
    LaunchedEffect(selKey, groups) {
        if (remembered() == null && groups.isNotEmpty()) {
            val g = groups[0]
            val a = preferredAudioIndex(context, g)
            audioIdx = a
            qualityIdx = preferredQualityIndex(context, g.audios.getOrNull(a))
        }
    }
    // Season taken from the episode titles (S01 E01 ...) when one list holds all the seasons of a series
    var epSeason by remember(selKey) { mutableIntStateOf(remembered()?.getOrNull(3) ?: 0) }
    fun saveSelection() { MediaDetailState.selections[selKey] = intArrayOf(seasonIdx, audioIdx, qualityIdx, epSeason, settingsSig) }
    val season = groups.getOrNull(seasonIdx.coerceIn(0, (groups.size - 1).coerceAtLeast(0)))
    val audioList = season?.audios.orEmpty()
    val audio = audioList.getOrNull(audioIdx.coerceIn(0, (audioList.size - 1).coerceAtLeast(0)))
    val optionList = audio?.options.orEmpty()
    val option = optionList.getOrNull(qualityIdx.coerceIn(0, (optionList.size - 1).coerceAtLeast(0)))
    // which picker sheet is open: 0 none, 1 season, 2 audio, 3 quality
    var pickerSheet by remember { mutableIntStateOf(0) }

    // ---- episodes of the selected quality (a series may need getEpisodes first) ----
    val optionKey = (option?.link?.episodesLink ?: option?.link?.directLinks?.firstOrNull()?.link).orEmpty() + "|" + audio?.label + "|" + option?.label
    var episodes by remember(optionKey) {
        mutableStateOf<List<MediaDirectLink>?>(
            option?.link?.let { l -> l.episodesLink?.let { MediaProviderRepository.peekEpisodes(context, plugin.value, it) } }
        )
    }
    var episodesError by remember(optionKey) { mutableStateOf<String?>(null) }
    var episodesReload by remember(optionKey) { mutableIntStateOf(0) }
    // the pull-down refresh that this quality option's episode list has already been loaded for
    var episodesRefreshedAt by remember(optionKey) { mutableIntStateOf(0) }
    LaunchedEffect(optionKey, episodesReload, titleRefreshes) {
        val l = option?.link ?: return@LaunchedEffect
        val epLink = l.episodesLink
        if (epLink == null) {
            episodes = l.directLinks
            return@LaunchedEffect
        }
        val force = episodesReload > 0 || titleRefreshes > episodesRefreshedAt
        if (!force && episodes != null) {
            // shown from the saved copy; an old copy is refreshed silently in the background
            episodesError = null
            try { MediaProviderRepository.loadEpisodes(context, plugin.value, epLink) } catch (e: CancellationException) { throw e } catch (_: Throwable) {}
            return@LaunchedEffect
        }
        // a refresh / retry keeps the old list visible until the new one arrives
        if (!force) episodes = null
        episodesError = null
        try {
            episodes = MediaProviderRepository.loadEpisodes(context, plugin.value, epLink, force = force)
            episodesRefreshedAt = titleRefreshes
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (episodes == null) episodesError = t.message ?: "Could not load the episodes"
        }
    }

    val allVideos: List<DetailVideo> = remember(episodes, option, isSeries, title, poster) {
        val q = option?.label.orEmpty()
        (episodes ?: emptyList()).map { d ->
            val shownTitle = if (!isSeries && d.title.equals("Movie", ignoreCase = true)) title else d.title
            DetailVideo(
                title = shownTitle,
                link = d.link,
                type = d.type ?: if (isSeries) "series" else "movie",
                poster = d.image ?: poster,
                qualityLabel = q,
                meta = d.meta.orEmpty()
            )
        }
    }

    val epSeasonNumbers = remember(allVideos) { allVideos.mapNotNull { seasonNumber(it.title) }.distinct().sorted() }
    val showEpSeason = groups.size <= 1 && epSeasonNumbers.size > 1
    val epSeasonShown = if (epSeasonNumbers.contains(epSeason)) epSeason else epSeasonNumbers.firstOrNull() ?: 0
    val query = searchQuery.trim()
    val videos: List<DetailVideo> = if (query.isNotEmpty()) {
        // top search button: finds the episode / movie by name or number, across every season
        val numberRe = if (query.all { it.isDigit() }) Regex("(?i)(?:\\be|\\bep|episode)\\s*0*" + query.trimStart('0').ifEmpty { "0" } + "\\b") else null
        allVideos.filter { it.title.contains(query, ignoreCase = true) || (numberRe != null && numberRe.containsMatchIn(it.title)) }
    } else if (showEpSeason) {
        allVideos.filter {
            val n = seasonNumber(it.title)
            n == epSeasonShown || (n == null && epSeasonShown == epSeasonNumbers.first())
        }
    } else allVideos

    // ---- swipe toolbar + background work ----
    // Swipe toolbar selection: every card swiped while the toolbar is open is added to the selection (swiping a selected card
    // again removes it); the toolbar's Download then downloads all of them in one go with one quality.
    val actionState = com.example.ui.components.rememberSwipeActionState<DetailVideo> { it.link + "|" + it.title }
    var actionTarget by actionState
    var batchTargets by remember { mutableStateOf<List<DetailVideo>?>(null) }
    var lastActionTarget by remember { mutableStateOf<DetailVideo?>(null) }
    var downloadTarget by remember { mutableStateOf<DetailVideo?>(null) }
    if (actionTarget != null) lastActionTarget = actionTarget
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var busyJob by remember { mutableStateOf<Job?>(null) }
    val savedTick by MediaLibraryStore.changes.collectAsState()

    BackHandler {
        when {
            busyLabel != null -> { busyJob?.cancel(); busyJob = null; busyLabel = null }
            pickerSheet != 0 -> pickerSheet = 0
            actionTarget != null -> actionTarget = null
            else -> onBack()
        }
    }

    var mangaPages by remember { mutableStateOf<List<com.example.util.media.MediaStream>?>(null) }
    var mangaTitle by remember { mutableStateOf("") }
    mangaPages?.let { pages -> MangaReaderScreen(title = mangaTitle, pages = pages, onClose = { mangaPages = null }) }

    fun playerTitle(v: DetailVideo): String = if (isSeries) "$title · ${v.title}" else v.title

    // The name of ONE episode, exactly as the episode list shows it: used by the player and by downloads
    // (not "Series · Episode"). With several seasons the season is kept in front so two "Episode 1" can't be mixed up.
    fun episodeName(v: DetailVideo): String {
        if (!isSeries) return v.title
        val name = v.title.trim()
        if (name.isBlank()) return playerTitle(v)
        val seasonNo = if (groups.size > 1 && seasonNumber(name) == null) (seasonNumber(season?.label.orEmpty()) ?: (seasonIdx + 1)) else null
        return if (seasonNo != null) "S%02d · %s".format(seasonNo, name) else name
    }

    fun runBusy(label: String, block: suspend () -> Unit) {
        if (busyLabel != null) return
        busyLabel = label
        busyJob = scope.launch {
            try {
                block()
            } finally {
                busyLabel = null
                busyJob = null
            }
        }
    }

    fun showResolveError(error: Throwable?) {
        Toast.makeText(context, "Could not get this video: " + (error?.message ?: "unknown error"), Toast.LENGTH_LONG).show()
    }

    fun runResolve(label: String, v: DetailVideo, onOk: (com.example.util.media.MediaStream) -> Unit) {
        runBusy(label) {
            val result = MediaPlayback.resolve(
                context, plugin, v.link, v.type,
                v.title, if (isSeries) allVideos.indexOfFirst { it.link == v.link }.coerceAtLeast(0) else -1,
                requireWorking = true
            )
            val stream = result.getOrNull()
            if (stream == null) showResolveError(result.exceptionOrNull()) else onOk(stream)
        }
    }

    fun play(v: DetailVideo) {
        actionTarget = null
        // Tapping a video does NOT open the player blindly: the servers of this episode are fetched and raced all at the same
        // time right here (like the link search of a download), and the player opens with the server that won, so it starts
        // playing at once. A series is queued with the whole episode list, so Next / auto-play continue with the following
        // episodes (those are resolved the same way by the player).
        val qualityChoices = optionList.map { PluginQualityChoice(it.label, it.link.episodesLink, it.link.directLinks) }
        val selectedQuality = option?.label.orEmpty()
        fun queued(d: DetailVideo, idx: Int): VideoItem = PluginQueuedEpisodes.placeholder(
            PluginQueuedEpisode(
                plugin = plugin,
                playerTitle = episodeName(d),
                poster = d.poster,
                link = d.link,
                type = d.type,
                episodeIndex = idx,
                episodeTitle = d.title,
                isSeries = isSeries,
                choices = qualityChoices,
                selectedQuality = selectedQuality
            )
        )
        val tappedIndex = allVideos.indexOfFirst { it.link == v.link }.coerceAtLeast(0)
        val placeholderItem = queued(v, tappedIndex)
        runBusy("Connecting to servers…") {
            // manga / comic providers return page images: those open in the reader (portrait), not in the video player
            val mangaChapter = try {
                com.example.util.media.MangaDetect.pagesOf(
                    com.example.util.media.MediaProviderRepository.loadStreams(context, plugin.value, v.link, v.type, forPlayback = true)
                )
            } catch (_: Throwable) { null }
            if (mangaChapter != null) {
                mangaTitle = playerTitle(v)
                mangaPages = mangaChapter
                return@runBusy
            }
            val result = PluginQueuedEpisodes.resolve(context, placeholderItem.path)
            val item = result.getOrNull()
            if (item == null) {
                showResolveError(result.exceptionOrNull())
                return@runBusy
            }
            // "Continue watching" row of the provider page
            MediaHistoryStore.record(
                context,
                MediaWatchedTitle(plugin.value, title, post.link, poster.ifBlank { post.image })
            )
            val queue = if (isSeries && allVideos.size > 1) {
                allVideos.mapIndexed { idx, d -> if (d.link == v.link) item else queued(d, idx) }
            } else listOf(item)
            onPlayVideo(item, queue)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(pageBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ---- gradient card: backdrop, title, rating / genres, synopsis, audio + quality (like a channel card) ----
            item(key = "card") {
                val cardShape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(cardShape)
                        .background(surfaceColor)
                        .then(if (isDark) Modifier.border(1.5.dp, AccentGradient, cardShape) else Modifier.border(1.5.dp, borderColor, cardShape))
                        .padding(1.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(topStart = 14.5.dp, topEnd = 14.5.dp))
                    ) {
                        HighResImage(
                            url = backdrop.ifBlank { null },
                            modifier = Modifier.fillMaxSize(),
                            placeholder = {
                                Box(modifier = Modifier.fillMaxSize().background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0))
                            }
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp, top = 10.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_arrow_left_solid,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = title,
                            fontSize = 26.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        val chips = buildList<String> {
                            loaded?.rating?.takeIf { it.isNotBlank() }?.let { add("★ $it") }
                            loaded?.durationMin?.takeIf { it > 0 }?.let { add(if (it >= 60) "${it / 60}h ${it % 60}m" else "${it}m") }
                            loaded?.status?.takeIf { it.isNotBlank() }?.let { add(it) }
                            loaded?.contentRating?.takeIf { it.isNotBlank() }?.let { add(it) }
                            loaded?.tags?.let { addAll(it.take(8)) }
                        }
                        if (chips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chips.forEach { chip ->
                                    Text(
                                        text = chip,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = primaryText,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(surfaceColor)
                                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        when {
                            infoError != null -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = infoError.orEmpty(), fontSize = 13.sp, color = secondaryText)
                                Spacer(modifier = Modifier.height(12.dp))
                                DetailPill("Retry", primaryText, surfaceColor, borderColor) { reload++ }
                            }
                            loaded == null -> Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ViauraExpressiveLoadingIndicator(size = 44.dp, isDark = isDark)
                            }
                            else -> {
                                if (loaded.synopsis.isNotBlank()) {
                                    var expanded by remember(selKey) { mutableStateOf(false) }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(text = "Synopsis", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = plugin.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentSkyBlue,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = loaded.synopsis,
                                        fontSize = 14.5.sp,
                                        lineHeight = 21.sp,
                                        color = secondaryText,
                                        maxLines = if (expanded) Int.MAX_VALUE else 5,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { expanded = !expanded }
                                    )
                                }
                                if (loaded.actors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(text = "Cast", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        loaded.actors.forEach { actor ->
                                            Column(modifier = Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(64.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                                                ) {
                                                    if (actor.image.isNotBlank()) {
                                                        HighResImage(url = actor.image, modifier = Modifier.fillMaxSize(), maxSidePx = 300)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = actor.name,
                                                    fontSize = 12.sp,
                                                    color = primaryText,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (actor.role.isNotBlank()) {
                                                    Text(
                                                        text = actor.role,
                                                        fontSize = 11.sp,
                                                        color = secondaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (loaded.recommendations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(text = "More like this", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        loaded.recommendations.forEach { rec ->
                                            Column(
                                                modifier = Modifier
                                                    .width(112.dp)
                                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                        MediaDetailState.open(plugin, rec)
                                                    }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(2f / 3f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                                                ) {
                                                    if (rec.image.isNotBlank()) {
                                                        HighResImage(url = rec.image, modifier = Modifier.fillMaxSize(), maxSidePx = 500)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = rec.title,
                                                    fontSize = 12.5.sp,
                                                    color = primaryText,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                if (groups.isNotEmpty()) {
                                    // only what this provider really offers a choice of: season / audio / quality
                                    val showSeasonPicker = groups.size > 1 || showEpSeason
                                    val showAudioPicker = audioList.size > 1
                                    val showQualityPicker = optionList.size > 1
                                    if (showSeasonPicker || showAudioPicker || showQualityPicker) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                    if (showSeasonPicker) {
                                        PickerPill(
                                            caption = "Season",
                                            value = if (groups.size > 1) season?.label.orEmpty() else "Season $epSeasonShown",
                                            primaryText = primaryText,
                                            secondaryText = secondaryText,
                                            borderColor = borderColor,
                                            modifier = Modifier.fillMaxWidth()
                                        ) { pickerSheet = if (groups.size > 1) 1 else 4 }
                                    }
                                    if (showAudioPicker || showQualityPicker) {
                                        if (showSeasonPicker) Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (showAudioPicker) {
                                                PickerPill(
                                                    caption = "Audio",
                                                    value = audio?.label.orEmpty(),
                                                    primaryText = primaryText,
                                                    secondaryText = secondaryText,
                                                    borderColor = borderColor,
                                                    modifier = Modifier.weight(1f)
                                                ) { pickerSheet = 2 }
                                            }
                                            if (showQualityPicker) {
                                                PickerPill(
                                                    caption = "Quality",
                                                    value = option?.label.orEmpty(),
                                                    primaryText = primaryText,
                                                    secondaryText = secondaryText,
                                                    borderColor = borderColor,
                                                    modifier = Modifier.weight(1f)
                                                ) { pickerSheet = 3 }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No download or stream links were found for this title.",
                                        fontSize = 13.sp,
                                        color = secondaryText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---- the movie / episode cards ----
            if (loaded != null && groups.isNotEmpty()) {
                when {
                    episodesError != null -> item(key = "episodes-error") {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                            Text(text = episodesError.orEmpty(), fontSize = 13.sp, color = secondaryText)
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailPill("Retry", primaryText, surfaceColor, borderColor) { episodesReload++ }
                        }
                    }
                    episodes == null && option != null -> item(key = "episodes-loading") {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            ViauraExpressiveLoadingIndicator(size = 36.dp, isDark = isDark)
                        }
                    }
                    else -> items(videos, key = { "v|" + it.link + "|" + it.title }) { v ->
                        // the picture is the one the provider gives (default thumbnail): nothing is read out of the video itself,
                        // so no internet data is spent on the cards
                        val card = remember(v) {
                            VideoItem(
                                id = -(kotlin.math.abs(v.link.hashCode().toLong())) - 1L,
                                uri = Uri.parse(v.link),
                                displayName = v.title,
                                path = v.link,
                                sizeBytes = 0L,
                                durationMs = 0L,
                                dateModified = 0L,
                                isOnline = true,
                                thumbnailUrl = v.poster.takeIf { it.isNotBlank() },
                                channelName = listOf(v.meta, v.qualityLabel, plugin.name).filter { it.isNotBlank() }.joinToString(" · ")
                            )
                        }
                        Box(modifier = Modifier.swipeLeftForActions(selected = actionState.contains(v)) { actionTarget = v }) {
                            VideoItemCard(
                                video = card,
                                isDark = isDark,
                                visibleFields = VisibleFields(
                                    showExtension = false,
                                    showSize = false,
                                    showSubtitleIndicator = false,
                                    showResolution = false,
                                    showProgressBar = false
                                ),
                                largeThumbnail = true,
                                onClick = { play(v) }
                            )
                        }
                    }
                }
            }
        }

        // ---- swipe (right -> left) toolbar: playlist, watch later, download, share ----
        AnimatedVisibility(
            visible = actionTarget != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(vertical = 18.dp, horizontal = 8.dp)
        ) {
            val t = actionTarget ?: lastActionTarget
            if (t != null) {
                val saved = remember(savedTick, t.link) { MediaLibraryStore.isSaved(context, plugin.value, t.link) }
                OnlineQuickActionBar(
                    isDark = isDark,
                    alphaRatio = (glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    isSavedForLater = saved,
                    selectedCount = actionState.size,
                    onClearSelection = { actionTarget = null },
                    onDownload = {
                        if (actionState.size > 1) batchTargets = actionState.items.toList() else downloadTarget = t
                        actionTarget = null
                    },
                    onSaveToPlaylist = {
                        actionTarget = null
                        runResolve("Adding to playlist…", t) { stream ->
                            val item = MediaPlayback.toVideoItem(context, plugin, episodeName(t), t.poster, stream)
                            MediaPlayback.addToPluginPlaylist(context, plugin, item)
                            Toast.makeText(context, "Added to the \"${plugin.name}\" playlist", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onWatchLater = {
                        val nowSaved = MediaLibraryStore.toggleWatchLater(
                            context,
                            MediaSavedVideo(
                                pluginId = plugin.value,
                                title = episodeName(t),
                                pageTitle = title,
                                poster = t.poster,
                                link = t.link,
                                type = t.type
                            )
                        )
                        Toast.makeText(context, if (nowSaved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onShare = {
                        actionTarget = null
                        val page = loaded?.webUrl?.takeIf { it.isNotBlank() }
                            ?: post.link.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }
                        if (page != null) {
                            shareOnlineLink(context, playerTitle(t), page)
                        } else {
                            runResolve("Getting link…", t) { stream ->
                                shareOnlineLink(context, playerTitle(t), stream.link)
                            }
                        }
                    }
                )
            }
        }

        downloadTarget?.let { dt ->
            com.example.ui.components.MediaDownloadSheet(
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                plugin = plugin,
                title = episodeName(dt),
                poster = dt.poster,
                link = dt.link,
                type = dt.type,
                onDismiss = { downloadTarget = null },
                // a series: its own folder (Downloads + Movies/Viaura/<series>), never mixed with movies
                group = if (isSeries) title else ""
            )
        }

        // ---- several episodes selected with the swipe toolbar: one panel, one quality, same-size files ----
        batchTargets?.let { picked ->
            val currentOption = qualityIdx.coerceIn(0, (optionList.size - 1).coerceAtLeast(0))
            val batchEpisodes = picked.map { v ->
                com.example.ui.components.BatchEpisode(
                    name = episodeName(v), matchTitle = v.title, index = allVideos.indexOfFirst { it.link == v.link && it.title == v.title },
                    link = v.link, type = v.type, poster = v.poster
                )
            }
            val batchVariants = optionList.mapIndexed { i, o ->
                com.example.ui.components.BatchVariant(label = o.label, isCurrent = i == currentOption) {
                    val l = o.link
                    val eps = if (l.episodesLink != null) MediaProviderRepository.loadEpisodes(context, plugin.value, l.episodesLink) else l.directLinks
                    eps.mapIndexed { n, d ->
                        com.example.ui.components.BatchEpisode(
                            name = d.title, matchTitle = d.title, index = n, link = d.link,
                            type = d.type ?: if (isSeries) "series" else "movie", poster = d.image ?: poster
                        )
                    }
                }
            }
            com.example.ui.components.MediaBatchDownloadSheet(
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                plugin = plugin,
                group = if (isSeries) title else "",
                episodes = batchEpisodes,
                variants = batchVariants,
                onDismiss = { batchTargets = null }
            )
        }

        // ---- season / audio / quality pickers (bottom sheets, same glass as the download sheet) ----
        when (pickerSheet) {
            1 -> OptionSheet(
                title = "Season",
                options = groups.map { it.label },
                selected = seasonIdx.coerceIn(0, (groups.size - 1).coerceAtLeast(0)),
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { pickerSheet = 0 },
                onSelect = {
                    seasonIdx = it
                    audioIdx = preferredAudioIndex(context, groups.getOrNull(it))
                    qualityIdx = preferredQualityIndex(context, groups.getOrNull(it)?.audios?.getOrNull(audioIdx))
                    saveSelection()
                    pickerSheet = 0
                }
            )
            4 -> OptionSheet(
                title = "Season",
                options = epSeasonNumbers.map { "Season $it" },
                selected = epSeasonNumbers.indexOf(epSeasonShown).coerceAtLeast(0),
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { pickerSheet = 0 },
                onSelect = {
                    epSeason = epSeasonNumbers[it]
                    saveSelection()
                    pickerSheet = 0
                }
            )
            2 -> OptionSheet(
                title = "Audio",
                options = audioList.map { it.label },
                selected = audioIdx.coerceIn(0, (audioList.size - 1).coerceAtLeast(0)),
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { pickerSheet = 0 },
                onSelect = {
                    audioIdx = it
                    qualityIdx = preferredQualityIndex(context, audioList.getOrNull(it))
                    saveSelection()
                    pickerSheet = 0
                }
            )
            3 -> OptionSheet(
                title = "Quality",
                options = optionList.map { it.label },
                selected = qualityIdx.coerceIn(0, (optionList.size - 1).coerceAtLeast(0)),
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { pickerSheet = 0 },
                onSelect = {
                    qualityIdx = it
                    saveSelection()
                    pickerSheet = 0
                }
            )
        }

        // ---- "getting the link" indicator: transparent glass, no dark scrim (back cancels) ----
        if (busyLabel != null) {
            val factor = (glassBlurTransparency / 100f).coerceIn(0.20f, 1.0f)
            val cardBg = (if (isDark) Neutral.D0F172A else Color.White).copy(alpha = 0.22f + 0.28f * factor)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                        .padding(horizontal = 28.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ViauraExpressiveLoadingIndicator(size = 44.dp, isDark = isDark)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = busyLabel.orEmpty(), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Press back to cancel", fontSize = 11.5.sp, color = secondaryText)
                }
            }
        }
    }
}
