package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.util.OnlineMediaLibrary
import com.example.util.OnlineVideoQueue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.screens.VideoItem
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextSecondary
import com.example.util.OnlineChannelResult
import com.example.util.OnlineMusicService
import com.example.util.OnlineTrackResult
import com.example.util.OnlineVideoRecommender
import com.example.util.OnlineVideoService
import com.example.util.cachedBestLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.screens.ChannelTabsRow
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.Neutral
import com.example.util.SubscribedChannelsManager

/**
 * Online Video browsing panel (inside the "Online Video" folder): recommendation feed when the search box is empty,
 * video + channel search otherwise. Same architecture as [OnlineMusicPanel], but renders the regular video cards.
 */
@Composable
fun OnlineVideoPanel(
    isDark: Boolean,
    searchQuery: String,
    layoutMode: LayoutMode,
    visibleFields: VisibleFields,
    sortField: SortField,
    sortDirection: SortDirection,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    onOpenChannel: (OnlineChannelResult) -> Unit,
    modifier: Modifier = Modifier,
    glassBlurTransparency: Float = 85f
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = OnlineVideoFeedState
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    val listState = rememberLazyListState(st.listIndex, st.listOffset)
    val gridState = rememberLazyGridState(st.gridIndex, st.gridOffset)
    var job by remember { mutableStateOf<Job?>(null) }
    val isFeedMode = searchQuery.isBlank()

    // ---- swipe (right -> left) quick actions: nothing is selected, the bar just appears at the bottom ----
    // Swipe toolbar selection: every card swiped while the toolbar is open is added (swiping a selected card again removes it);
    // the toolbar's Download then downloads all of them in one go with one quality.
    val actionState = com.example.ui.components.rememberSwipeActionState<OnlineTrackResult> { it.videoUrl }
    var actionTarget by actionState
    var batchRequests by remember { mutableStateOf<List<com.example.ui.components.DownloadRequest>?>(null) }
    var lastActionTarget by remember { mutableStateOf<OnlineTrackResult?>(null) }
    var downloadRequest by remember { mutableStateOf<com.example.ui.components.DownloadRequest?>(null) }
    com.example.ui.components.DownloadRequestHost(downloadRequest, isDark, glassBlurTransparency) { downloadRequest = null }
    com.example.ui.components.OnlineBatchDownloadHost(batchRequests, isDark, glassBlurTransparency) { batchRequests = null }
    if (actionTarget != null) lastActionTarget = actionTarget
    var playlistTargets by remember { mutableStateOf<List<OnlineTrackResult>>(emptyList()) }
    LaunchedEffect(Unit) {
        OnlineMediaLibrary.attach(context)
        st.watchLater = withContext(Dispatchers.IO) { OnlineMediaLibrary.watchLater(context, OnlineMediaLibrary.KIND_VIDEO) }
    }
    val hidden = remember(st.hiddenVersion) {
        OnlineMediaLibrary.notInterestedSet(context, OnlineMediaLibrary.KIND_VIDEO)
    }
    BackHandler(enabled = actionTarget != null) { actionTarget = null }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (i, o) -> st.listIndex = i; st.listOffset = o }
    }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (i, o) -> st.gridIndex = i; st.gridOffset = o }
    }
    DisposableEffect(Unit) {
        onDispose {
            // a load that was cut short by leaving the screen must not leave the spinner stuck
            st.isSearching = false
            st.isLoadingMore = false
            st.isRefreshing = false
        }
    }

    fun loadFeed(fromRefresh: Boolean) {
        job?.cancel()
        st.token++
        val token = st.token
        st.isSearching = st.results.isEmpty()
        st.isRefreshing = fromRefresh
        st.isLoadingMore = false
        st.channelResults = emptyList()
        st.extractor = null
        st.nextPage = null
        st.hasNextPage = false
        st.lastQuery = ""
        st.usedQueries.clear()
        // runs in the process-wide scope: leaving the screen (opening a video) must not cut the refresh short and leave
        // the pull-to-refresh animation hanging; the result is simply there when the user comes back
        val appContext = context.applicationContext
        job = feedPrefetchScope.launch {
            try {
                val batch = try {
                    // topics / creators that were not played for a few app starts fade out (once per app start)
                    OnlineVideoRecommender.onFeedOpened(appContext)
                    kotlinx.coroutines.withTimeoutOrNull(12000L) {
                        OnlineVideoRecommender.fetchFeedBatch(appContext, emptySet(), st.usedQueries)
                    } ?: emptyList()
                } catch (t: kotlinx.coroutines.CancellationException) {
                    throw t
                } catch (_: Throwable) {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    if (token == st.token) {
                        if (batch.isNotEmpty() || st.results.isEmpty()) {
                            st.results = batch
                            st.generation++
                            st.savedFeed = null
                            st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                        }
                        st.initialized = batch.isNotEmpty()
                        st.isSearching = false
                        OnlineVideoRecommender.recordShownResults(appContext, batch)
                        if (fromRefresh && batch.isNotEmpty()) st.scrollTopTick++
                    }
                }
            } finally {
                // the pull-to-refresh animation must always end, whatever happened above (also when cancelled)
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    st.isRefreshing = false
                    if (token == st.token) st.isSearching = false
                }
            }
        }
    }

    fun loadMoreFeed() {
        if (st.isLoadingMore || st.isSearching) return
        val token = st.token
        val shown = st.results.map { it.videoUrl }.toSet()
        st.isLoadingMore = true
        scope.launch(Dispatchers.IO) {
            val batch = try {
                OnlineVideoRecommender.fetchFeedBatch(
                    context, shown, st.usedQueries,
                    onScreenChannels = st.results.groupingBy { it.artist.lowercase(Locale.ROOT) }.eachCount()
                )
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList()
            }
            withContext(Dispatchers.Main) {
                if (token == st.token && batch.isNotEmpty()) {
                    val already = st.results.map { it.videoUrl }.toSet()
                    val fresh = batch.filter { it.videoUrl !in already }
                    st.results = st.results + fresh
                    OnlineVideoRecommender.recordShownResults(context, fresh)
                }
                st.isLoadingMore = false
            }
        }
    }

    fun executeSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) {
            // search cleared: the feed that was there before the search comes back as it was (no new load)
            val saved = st.savedFeed
            if (saved != null && saved.isNotEmpty()) {
                job?.cancel()
                st.token++
                st.results = saved
                st.savedFeed = null
                st.channelResults = emptyList()
                st.extractor = null
                st.nextPage = null
                st.hasNextPage = false
                st.lastQuery = ""
                st.initialized = true
                st.isSearching = false
                st.generation++
                st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                st.scrollTopTick++
            } else if (st.initialized && st.lastQuery.isBlank() && st.results.isNotEmpty()) {
                // the feed is already on screen (a search was typed and cleared again before it finished): nothing to reload
                job?.cancel()
                st.token++
                st.isSearching = false
            } else loadFeed(false)
            return
        }
        if (st.lastQuery.isBlank() && st.results.isNotEmpty()) st.savedFeed = st.results
        job?.cancel()
        st.token++
        val token = st.token
        st.isSearching = true
        job = scope.launch(Dispatchers.IO) {
            try {
                val (extractor, page) = OnlineMusicService.searchMusicPage(trimmed)
                withContext(Dispatchers.Main) {
                    if (token == st.token) {
                        st.extractor = extractor
                        st.nextPage = page.nextPage
                        st.hasNextPage = page.hasNextPage
                        st.results = page.items
                        st.channelResults = page.channelResults
                        st.lastQuery = trimmed
                        st.initialized = true
                        st.isSearching = false
                        // a search always starts at the top result (the list must not stay scrolled down from before);
                        // what is only searched is NOT used for recommendations - only played videos are
                        st.generation++
                        st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                        st.scrollTopTick++
                    }
                    st.isRefreshing = false
                }
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    if (token == st.token) { st.results = emptyList(); st.channelResults = emptyList(); st.isSearching = false }
                    st.isRefreshing = false
                }
            }
        }
    }

    fun loadMoreSearch() {
        val ex = st.extractor ?: return
        val next = st.nextPage ?: return
        if (!st.hasNextPage || st.isLoadingMore || st.isSearching) return
        val token = st.token
        st.isLoadingMore = true
        scope.launch(Dispatchers.IO) {
            val p = OnlineMusicService.fetchNextMusicPage(ex, next)
            withContext(Dispatchers.Main) {
                if (token == st.token) {
                    val existing = st.results.map { it.videoUrl }.toSet()
                    st.results = st.results + p.items.filter { it.videoUrl !in existing }
                    st.nextPage = p.nextPage
                    st.hasNextPage = p.hasNextPage
                }
                st.isLoadingMore = false
            }
        }
    }

    // First composition after (re)opening: keep what is already loaded (e.g. coming back from the player).
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(searchQuery) {
        if (firstRun) {
            firstRun = false
            if (!st.initialized) executeSearch(searchQuery)
            else if (searchQuery.isBlank() && st.lastQuery.isNotBlank()) executeSearch("") // left during a search: show the feed again
            return@LaunchedEffect
        }
        if (searchQuery.isNotBlank() && !searchQuery.trim().equals(OnlineSearchBridge.lastPicked.trim(), ignoreCase = true)) delay(350)
        executeSearch(searchQuery)
    }

    // A new search / feed starts at its first item: scrolled only AFTER the new items are laid out
    var handledScrollTick by remember { mutableIntStateOf(st.scrollTopTick) }
    LaunchedEffect(st.scrollTopTick) {
        if (st.scrollTopTick != handledScrollTick) {
            handledScrollTick = st.scrollTopTick
            androidx.compose.runtime.withFrameNanos { }
            androidx.compose.runtime.withFrameNanos { }
            listState.scrollToItem(0)
            gridState.scrollToItem(0)
        }
    }

    // Manual pull-to-refresh (from the Home screen's pull gesture) -> brand-new recommendations.
    var handledRefresh by remember { mutableIntStateOf(st.refreshRequest) }
    LaunchedEffect(st.refreshRequest) {
        if (st.refreshRequest != handledRefresh) {
            handledRefresh = st.refreshRequest
            if (isFeedMode) loadFeed(true) else executeSearch(searchQuery)
        }
    }

    // Safety net: the pull-to-refresh indicator can never stay on screen for more than a few seconds.
    LaunchedEffect(st.isRefreshing) {
        if (st.isRefreshing) {
            delay(10000L)
            st.isRefreshing = false
        }
    }

    // ---- Home | Subs | Live strip (only while the feed is shown; a search always shows its results) ----
    val activeTab = if (isFeedMode) st.tab else "home"
    LaunchedEffect(activeTab, st.refreshRequest) {
        if (activeTab == "subs" && (!st.subsLoaded || st.subsLoadedRefresh != st.refreshRequest)) {
            st.subsLoading = true
            try {
                val groups = withContext(Dispatchers.IO) { loadSubsGroups(context) }
                st.subsGroups = groups
                st.subsLoaded = true
                st.subsLoadedRefresh = st.refreshRequest
            } finally {
                st.subsLoading = false
            }
        }
        if (activeTab == "live" && (!st.liveLoaded || st.liveLoadedRefresh != st.refreshRequest)) {
            st.liveLoading = true
            try {
                val live = withContext(Dispatchers.IO) { OnlineMusicService.fetchLiveStreams() }
                st.liveResults = live
                st.liveLoaded = true
                st.liveLoadedRefresh = st.refreshRequest
            } finally {
                st.liveLoading = false
            }
        }
    }

    // Unlimited scrolling
    val nearEnd by remember(layoutMode) {
        derivedStateOf {
            val total: Int
            val last: Int
            if (layoutMode == LayoutMode.GRID) {
                total = gridState.layoutInfo.totalItemsCount
                last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            } else {
                total = listState.layoutInfo.totalItemsCount
                last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            }
            total > 0 && last >= total - 4
        }
    }
    LaunchedEffect(nearEnd, st.results.size) {
        if (nearEnd && !st.isLoadingMore && !st.isSearching) {
            if (st.lastQuery.isBlank()) loadMoreFeed() else loadMoreSearch()
        }
    }

    val sorted = remember(st.results, sortField, sortDirection, isFeedMode, st.sortTouched) {
        // search results keep their relevance order (original + most popular first) until the user picks a sort order himself
        if (!st.sortTouched) st.results else when (sortField) {
            SortField.TITLE ->
                if (sortDirection == SortDirection.ASCENDING) st.results.sortedBy { it.title.lowercase(Locale.ROOT) }
                else st.results.sortedByDescending { it.title.lowercase(Locale.ROOT) }
            SortField.DATE ->
                if (sortDirection == SortDirection.ASCENDING) st.results.sortedBy { it.uploadDateEpochMs ?: 0L }
                else st.results.sortedByDescending { it.uploadDateEpochMs ?: 0L }
            else -> st.results
        }
    }
    // Videos that were left half-way (back pressed before the end) wait at the very top of the feed until they are
    // finished: a finished video is saved with position 0 and therefore drops out of this list by itself.
    val historyVersion by com.example.util.PlaybackHistoryManager.version.collectAsState()
    var resumeItems by remember { mutableStateOf<List<OnlineTrackResult>>(emptyList()) }
    LaunchedEffect(historyVersion, st.results.size, st.refreshRequest) {
        resumeItems = withContext(Dispatchers.IO) {
            try {
                val prefix = OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX
                com.example.util.PlaybackHistoryManager.getAllPlaybackRecords(context)
                    .filter { it.path.startsWith(prefix) && it.positionMs > 3_000L &&
                        !(it.durationMs > 0L && it.positionMs >= it.durationMs - it.durationMs / 25) }
                    .take(5)
                    .map { rec ->
                        val url = OnlineVideoService.toWatchUrl(rec.path)
                        OnlineMediaLibrary.getMeta(OnlineMediaLibrary.KIND_VIDEO, url, context)
                            ?: st.results.firstOrNull { it.videoUrl == url }
                            ?: OnlineTrackResult(
                                videoUrl = url,
                                title = rec.title.ifBlank { url },
                                artist = "",
                                durationSec = rec.durationMs / 1000L,
                                thumbnailUrl = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
                                    ?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }
                            )
                    }
            } catch (_: Throwable) { emptyList() }
        }
    }
    // Order: unfinished video(s) first, then Watch-later, then the recommendations; hidden ones never show.
    val shown = remember(sorted, isFeedMode, st.watchLater, hidden, resumeItems) {
        val resume = if (isFeedMode) resumeItems.filter { it.videoUrl !in hidden } else emptyList()
        val resumeUrls = resume.map { it.videoUrl }.toSet()
        val pinned = if (isFeedMode) st.watchLater.filter { it.videoUrl !in hidden && it.videoUrl !in resumeUrls } else emptyList()
        val pinnedUrls = resumeUrls + pinned.map { it.videoUrl }.toSet()
        resume + pinned + sorted.filter { it.videoUrl !in pinnedUrls && it.videoUrl !in hidden }
    }
    val cardFields = visibleFields.copy(showExtension = false, showSize = false, showSubtitleIndicator = false, showNewBadge = true)

    // The user opened a video: a fresh block built around it (3 more of the same, 2 they may like next, 3 from other
    // categories, 2 from popular channels) is prepared while it plays and waits at the top of the feed on return.
    fun prepareAfterWatch(watched: OnlineTrackResult) {
        if (!isFeedMode) return
        val appContext = context.applicationContext
        val token = st.token
        val exclude = st.results.map { it.videoUrl }.toSet() + watched.videoUrl
        afterWatchJob?.cancel()
        afterWatchJob = feedPrefetchScope.launch {
            val batch = try {
                kotlinx.coroutines.withTimeoutOrNull(25000L) {
                    OnlineVideoRecommender.fetchFeedBatch(
                        appContext, exclude, st.usedQueries, total = 10,
                        onScreenChannels = st.results.groupingBy { it.artist.lowercase(Locale.ROOT) }.eachCount()
                    )
                } ?: emptyList()
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList()
            }
            if (batch.isEmpty()) return@launch
            withContext(Dispatchers.Main) {
                if (token == st.token && st.lastQuery.isBlank()) {
                    val fresh = batch.filter { b -> st.results.none { it.videoUrl == b.videoUrl } }
                    if (fresh.isNotEmpty()) {
                        st.results = fresh + st.results
                        st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                        OnlineVideoRecommender.recordShownResults(appContext, fresh)
                    }
                }
            }
        }
    }

    fun play(r: OnlineTrackResult) {
        if (r.isNew) st.results = st.results.map { if (it.videoUrl == r.videoUrl) it.copy(isNew = false) else it }
        OnlineVideoRecommender.recordOpened(context, r.videoUrl, r.artist, r.title)
        st.watchedTick++
        feedPrefetchScope.launch { try { OnlineMediaLibrary.rememberMeta(context.applicationContext, OnlineMediaLibrary.KIND_VIDEO, r) } catch (_: Throwable) {} }
        prepareAfterWatch(r)
        val tappedPath = "${OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX}${r.videoUrl}"
        // What plays after this video: other creators of the same kind of video first (another cricketer after a cricket
        // video), never the same creator twice in a row; every few videos the creator that is playing comes back.
        val tappedIdx = shown.indexOfFirst { it.videoUrl == r.videoUrl }
        val before = if (tappedIdx > 0) shown.take(tappedIdx) else emptyList()
        val after = if (tappedIdx >= 0) shown.drop(tappedIdx + 1) else shown
        val topics = com.example.util.OnlineInterestProfile.extractTopics(r.title, 2).map { it.name }
        val arrangedAfter = com.example.util.OnlineTrackSequencer.arrange(
            items = after,
            seedKey = com.example.util.OnlineTrackSequencer.creatorKey(r.artist),
            currentUrl = r.videoUrl,
            preferred = { t -> topics.isNotEmpty() && com.example.util.OnlineInterestProfile.extractTopics(t.title, 2).any { it.name in topics } }
        )
        var all = (before + r + arrangedAfter).map { it.toOnlineVideoItem() }
        val item = all.firstOrNull { it.path == tappedPath } ?: r.toOnlineVideoItem()
        // Videos queued with "Play next / Play last" are spliced in right after the one that was tapped.
        val queued = OnlineVideoQueue.items.filter { it.videoUrl != r.videoUrl }.map { it.toOnlineVideoItem() }
        if (queued.isNotEmpty()) {
            val queuedPaths = queued.map { it.path }.toSet()
            val rest = all.filter { it.path !in queuedPaths }
            val idx = rest.indexOfFirst { it.path == tappedPath }
            all = if (idx >= 0) rest.take(idx + 1) + queued + rest.drop(idx + 1) else listOf(item) + queued
        }
        OnlineVideoQueue.clear()
        onPlayVideo(item, all)
    }

    fun playQueue() {
        val q = OnlineVideoQueue.items.toList()
        if (q.isEmpty()) return
        OnlineVideoQueue.clear()
        OnlineVideoRecommender.recordOpened(context, q.first().videoUrl, q.first().artist, q.first().title)
        val list = q.map { it.toOnlineVideoItem() }
        onPlayVideo(list.first(), list)
    }

    // Opens the channel of a video (tap on the channel logo / name of a YouTube-style card)
    fun openChannelOf(r: OnlineTrackResult) {
        val url = r.channelUrl
        if (!url.isNullOrBlank()) onOpenChannel(OnlineChannelResult(url, r.artist, r.channelAvatarUrl, null, null))
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Suggestions (5 popular searches for the typed text); the results start below them
        OnlineSearchSuggestions(
            query = searchQuery,
            isDark = isDark,
            glassBlurTransparency = glassBlurTransparency,
            fetch = { OnlineMusicService.getSearchSuggestions(it) }
        )
        if (isFeedMode) {
            ChannelTabsRow(
                tabs = listOf("home" to "Home", "subs" to "Subs", "live" to "Live"),
                selectedId = st.tab,
                onSelect = { st.tab = it },
                isDark = isDark
            )
        }
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when {
            activeTab == "subs" -> {
                // reading watchedTick makes a watched video leave the list as soon as the user comes back
                val watchedVersion = st.watchedTick
                val visibleGroups = remember(st.subsGroups, watchedVersion, st.hiddenVersion) {
                    st.subsGroups.mapNotNull { g ->
                        val left = g.videos.filter { !OnlineVideoRecommender.isWatched(context, it.videoUrl) && it.videoUrl !in hidden }
                        if (left.isEmpty()) null else g.copy(videos = left)
                    }
                }
                when {
                    st.subsLoading && st.subsGroups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ViauraExpressiveLoadingIndicator(size = 54.dp, isDark = isDark)
                    }
                    visibleGroups.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (SubscribedChannelsManager.getAll(context).isEmpty()) "You have not subscribed to any channel yet. Subscribe to a channel and its newest videos show up here."
                            else "No new videos from your subscriptions. Pull down to refresh.",
                            color = secondaryText, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        visibleGroups.forEach { g ->
                            item(key = "subs_h_${g.channel.channelUrl}") {
                                SubsChannelHeader(
                                    name = g.channel.name, avatarUrl = g.channel.avatarUrl, isDark = isDark,
                                    onClick = { onOpenChannel(OnlineChannelResult(g.channel.channelUrl, g.channel.name, g.channel.avatarUrl, null, null)) }
                                )
                            }
                            items(g.videos, key = { "subs_v_${it.videoUrl}" }) { r ->
                                Box(Modifier.swipeLeftForActions(selected = actionState.contains(r)) { actionTarget = r }) {
                                    if (layoutMode == LayoutMode.YOUTUBE) {
                                        OnlineYouTubeVideoCard(video = r, isDark = isDark, onClick = { actionTarget = null; play(r) }, onChannelClick = { openChannelOf(r) })
                                    } else {
                                        val v = remember(r) { r.toOnlineVideoItem() }
                                        VideoItemCard(video = v, isDark = isDark, visibleFields = cardFields, onClick = { actionTarget = null; play(r) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            activeTab == "live" -> {
                when {
                    st.liveLoading && st.liveResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ViauraExpressiveLoadingIndicator(size = 54.dp, isDark = isDark)
                    }
                    st.liveResults.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No live streams found right now. Pull down to refresh.", color = secondaryText, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(st.liveResults.filter { it.videoUrl !in hidden }, key = { "live_${it.videoUrl}" }) { r ->
                            Box(Modifier.swipeLeftForActions(selected = actionState.contains(r)) { actionTarget = r }) {
                                if (layoutMode == LayoutMode.YOUTUBE) {
                                    OnlineYouTubeVideoCard(video = r, isDark = isDark, onClick = { actionTarget = null; play(r) }, onChannelClick = { openChannelOf(r) })
                                } else {
                                    val v = remember(r) { r.toOnlineVideoItem() }
                                    VideoItemCard(video = v, isDark = isDark, visibleFields = cardFields, onClick = { actionTarget = null; play(r) })
                                }
                            }
                        }
                    }
                }
            }
            st.isSearching && st.results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ViauraExpressiveLoadingIndicator(size = 54.dp, isDark = isDark)
                }
            }
            shown.isEmpty() && st.channelResults.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotBlank()) "No online videos found for \"$searchQuery\". Check your internet connection or try a different search."
                        else "No online videos found. Check your internet connection and pull down to refresh.",
                        color = secondaryText,
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            layoutMode == LayoutMode.YOUTUBE -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(st.channelResults, key = { "ov_channel_${it.channelUrl}" }) { ch ->
                        OnlineChannelResultCard(channel = ch, isDark = isDark, onClick = {
                            onOpenChannel(ch)
                        })
                    }
                    items(shown, key = { "ov_y_${st.generation}_${it.videoUrl}" }) { r ->
                        Box(Modifier.swipeLeftForActions(selected = actionState.contains(r)) { actionTarget = r }) {
                            OnlineYouTubeVideoCard(video = r, isDark = isDark, onClick = { actionTarget = null; play(r) }, onChannelClick = { openChannelOf(r) })
                        }
                    }
                    if (st.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                ViauraExpressiveLoadingIndicator(size = 36.dp, isDark = isDark)
                            }
                        }
                    }
                }
            }
            layoutMode == LayoutMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(st.channelResults, key = { "ov_channel_${it.channelUrl}" }, span = { GridItemSpan(2) }) { ch ->
                        OnlineChannelResultCard(channel = ch, isDark = isDark, onClick = {
                            onOpenChannel(ch)
                        })
                    }
                    items(shown, key = { "ov_g_${st.generation}_${it.videoUrl}" }) { r ->
                        val v = remember(r) { r.toOnlineVideoItem() }
                        Box(Modifier.swipeLeftForActions(selected = actionState.contains(r)) { actionTarget = r }) {
                            VideoItemGridCard(video = v, isDark = isDark, visibleFields = cardFields, onClick = { actionTarget = null; play(r) })
                        }
                    }
                    if (st.isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                ViauraExpressiveLoadingIndicator(size = 36.dp, isDark = isDark)
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(st.channelResults, key = { "ov_channel_${it.channelUrl}" }) { ch ->
                        OnlineChannelResultCard(channel = ch, isDark = isDark, onClick = {
                            onOpenChannel(ch)
                        })
                    }
                    items(shown, key = { "ov_l_${st.generation}_${it.videoUrl}" }) { r ->
                        val v = remember(r) { r.toOnlineVideoItem() }
                        Box(Modifier.swipeLeftForActions(selected = actionState.contains(r)) { actionTarget = r }) {
                            VideoItemCard(video = v, isDark = isDark, visibleFields = cardFields, onClick = { actionTarget = null; play(r) })
                        }
                    }
                    if (st.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                ViauraExpressiveLoadingIndicator(size = 36.dp, isDark = isDark)
                            }
                        }
                    }
                }
            }
        }

        // "Play queue" button while videos are waiting in the queue
        if (OnlineVideoQueue.items.isNotEmpty() && actionTarget == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp)
                    .shadow(14.dp, RoundedCornerShape(22.dp), ambientColor = AccentSkyBlue.copy(alpha = 0.4f), spotColor = AccentPink.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(AccentGradient)
                    .clickable { playQueue() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledIcon(drawableRes = R.drawable.viaura_play_solid, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play queue (${OnlineVideoQueue.items.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Swipe toolbar
        androidx.compose.animation.AnimatedVisibility(
            visible = actionTarget != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(vertical = 18.dp)
        ) {
            val t = actionTarget ?: lastActionTarget
            if (t != null) {
                val kind = OnlineMediaLibrary.KIND_VIDEO
                // every action works on ALL selected cards (one card = the same as before)
                val picks = actionState.items.toList().ifEmpty { listOf(t) }
                OnlineQuickActionBar(
                    isDark = isDark,
                    alphaRatio = (glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    isSavedForLater = picks.all { p -> st.watchLater.any { it.videoUrl == p.videoUrl } },
                    selectedCount = actionState.size,
                    onClearSelection = { actionTarget = null },
                    onDownload = {
                        if (actionState.size > 1) {
                            batchRequests = actionState.items.map { x -> com.example.ui.components.DownloadRequest(x.videoUrl, x.title, x.artist, x.thumbnailUrl, false) }
                        } else {
                            downloadRequest = com.example.ui.components.DownloadRequest(t.videoUrl, t.title, t.artist, t.thumbnailUrl, false)
                        }
                        actionTarget = null
                    },
                    onPlayNext = {
                        // reversed: the selected videos keep their order at the front of the queue
                        picks.asReversed().forEach { v ->
                            OnlineMediaLibrary.rememberMeta(context, kind, v)
                            OnlineVideoQueue.addNext(v)
                        }
                        Toast.makeText(context, if (picks.size > 1) "${picks.size} videos will play next" else "Will play next: ${t.title}", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onPlayLast = {
                        picks.forEach { v ->
                            OnlineMediaLibrary.rememberMeta(context, kind, v)
                            OnlineVideoQueue.addLast(v)
                        }
                        Toast.makeText(context, if (picks.size > 1) "${picks.size} videos added to the end of the queue" else "Added to the end of the queue", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onSaveToPlaylist = {
                        picks.forEach { v -> OnlineMediaLibrary.rememberMeta(context, kind, v) }
                        playlistTargets = picks
                        actionTarget = null
                    },
                    onWatchLater = {
                        // all already saved -> remove them all, otherwise save the ones that are not saved yet
                        val allSaved = picks.all { p -> st.watchLater.any { it.videoUrl == p.videoUrl } }
                        var saved = false
                        picks.forEach { v ->
                            val isSaved = st.watchLater.any { it.videoUrl == v.videoUrl }
                            if (allSaved || !isSaved) saved = OnlineMediaLibrary.toggleWatchLater(context, kind, v)
                        }
                        st.watchLater = OnlineMediaLibrary.watchLater(context, kind)
                        if (saved) { st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0 }
                        Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onNotInterested = {
                        picks.forEach { v ->
                            OnlineMediaLibrary.markNotInterested(context, kind, v.videoUrl)
                            OnlineVideoQueue.remove(v.videoUrl)
                        }
                        st.watchLater = OnlineMediaLibrary.watchLater(context, kind)
                        st.hiddenVersion++
                        Toast.makeText(context, "Okay, you will see fewer videos like this", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onShare = {
                        shareOnlineLinks(context, picks.map { it.title to it.videoUrl })
                        actionTarget = null
                    },
                )
            }
        }

        if (playlistTargets.isNotEmpty()) {
            AddToPlaylistDialog(
                selectedVideos = playlistTargets.map { it.toOnlineVideoItem() },
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { playlistTargets = emptyList() },
                onPlaylistUpdated = { playlistTargets = emptyList() }
            )
        }
    }
    }
}


// ---------------------------------------------------------------------------------------------------------------------
// Feed layout (Sort & View > Layout > Feed) and the Subs tab
// ---------------------------------------------------------------------------------------------------------------------

/** Subs tab data: every subscribed channel with its newest uploads (max 3), the channel that uploaded last first. */
private suspend fun loadSubsGroups(context: android.content.Context): List<SubsChannelGroup> = kotlinx.coroutines.coroutineScope {
    val gate = kotlinx.coroutines.sync.Semaphore(6)
    SubscribedChannelsManager.getAll(context).take(40).map { ch ->
        async(Dispatchers.IO) {
            gate.acquire()
            try {
                val videos = try {
                    OnlineMusicService.fetchChannelLatestVideos(ch.channelUrl, 3)
                } catch (t: kotlinx.coroutines.CancellationException) {
                    throw t
                } catch (_: Throwable) {
                    emptyList()
                }
                SubsChannelGroup(
                    ch,
                    videos.filter { it.videoUrl.isNotBlank() && it.title.isNotBlank() }.map {
                        it.copy(
                            artist = it.artist.ifBlank { ch.name },
                            channelUrl = ch.channelUrl,
                            channelAvatarUrl = it.channelAvatarUrl ?: ch.avatarUrl
                        )
                    }
                )
            } finally {
                gate.release()
            }
        }
    }.awaitAll()
        .filter { it.videos.isNotEmpty() }
        .sortedByDescending { g -> g.videos.maxOf { it.uploadDateEpochMs ?: 0L } }
}

/** "3 days ago": the website's own text when there is one, otherwise worked out from the upload time. */
private fun youtubeAgeText(r: OnlineTrackResult): String {
    r.uploadDateText?.takeIf { it.isNotBlank() }?.let { return it }
    val at = r.uploadDateEpochMs ?: return ""
    if (at <= 0L) return ""
    val mins = ((System.currentTimeMillis() - at) / 60_000L).coerceAtLeast(0L)
    return when {
        mins < 1L -> "just now"
        mins < 60L -> "$mins minute${if (mins == 1L) "" else "s"} ago"
        mins < 60L * 24L -> "${mins / 60L} hour${if (mins / 60L == 1L) "" else "s"} ago"
        mins < 60L * 24L * 30L -> "${mins / (60L * 24L)} day${if (mins / (60L * 24L) == 1L) "" else "s"} ago"
        mins < 60L * 24L * 365L -> "${mins / (60L * 24L * 30L)} month${if (mins / (60L * 24L * 30L) == 1L) "" else "s"} ago"
        else -> "${mins / (60L * 24L * 365L)} year${if (mins / (60L * 24L * 365L) == 1L) "" else "s"} ago"
    }
}

private fun youtubeDurationText(sec: Long): String {
    val h = sec / 3600L
    val m = (sec % 3600L) / 60L
    val s = sec % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/** Round channel logo; the first letter on the app gradient while the logo is missing / loading. */
@Composable
private fun YouTubeChannelLogo(name: String, avatarUrl: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    var bmp by remember(avatarUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank()) bmp = loadRemoteAudioThumbnail(avatarUrl, targetSizePx = 160)
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(AccentGradient),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(bitmap = b.asImageBitmap(), contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
        } else {
            Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        }
    }
}

/**
 * One video in the YouTube-style layout: the same glass card look as the offline video card (thin plain border, rounded top corners),
 * the whole thumbnail on top, and below it the channel logo, the video title (up to 2 lines) and "channel - views - age" on one line.
 * No Subscribe button here: tap the video = play; tap the channel logo = open the channel (subscribe from there).
 */
@Composable
internal fun OnlineYouTubeVideoCard(
    video: OnlineTrackResult,
    isDark: Boolean,
    onClick: () -> Unit,
    onChannelClick: () -> Unit
) {
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) com.example.ui.theme.DarkGlassBorder else com.example.ui.theme.LightGlassBorder
    val shape = RoundedCornerShape(16.dp)

    var thumb by remember(video.thumbnailUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(video.thumbnailUrl) {
        if (!video.thumbnailUrl.isNullOrBlank()) thumb = loadRemoteAudioThumbnail(video.thumbnailUrl, targetSizePx = 900)
    }
    val channelUrl = video.channelUrl
    // one line beside each other: "channel \u2022 views \u2022 age"
    val meta = listOf(video.artist, formatViews(video.viewCount), youtubeAgeText(video)).filter { it.isNotBlank() }.joinToString(" \u2022 ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .cardSurface {
                this.liquidGlassOr(shape, isDark, LocalPanelTransparency.current, elevation = 4.dp, backdropBlur = false) {
                    this
                        .clip(shape)
                        .background(cardTone(surfaceColor))
                        // Liquid Glass OFF: the thin plain border of the offline video card (no gradient)
                        .border(1.dp, borderColor, shape)
                }
            }
            .padding(1.dp)
    ) {
        // Whole thumbnail, top corners rounded like the channel banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                .clickable { onClick() }
        ) {
            val t = thumb
            if (t != null) {
                Image(bitmap = t.asImageBitmap(), contentDescription = video.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            if (video.isNew) {
                Text(
                    "NEW", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(AccentGradient).padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
            if (video.isLive) {
                Text(
                    "LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE02424)).padding(horizontal = 7.dp, vertical = 2.dp)
                )
            } else if (video.durationSec > 0L) {
                Text(
                    youtubeDurationText(video.durationSec), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            YouTubeChannelLogo(
                name = video.artist.ifBlank { "?" }, avatarUrl = video.channelAvatarUrl, size = 40.dp,
                modifier = Modifier.clip(CircleShape).clickable(enabled = !channelUrl.isNullOrBlank()) { onChannelClick() }
            )
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).clickable { onClick() }) {
                Text(
                    video.title, color = primaryText, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotBlank()) {
                    Text(meta, color = secondaryText, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

/** Channel line of the Subs tab (logo + name); the newest videos of that channel follow below it. Tap = open the channel. */
@Composable
private fun SubsChannelHeader(name: String, avatarUrl: String?, isDark: Boolean, onClick: () -> Unit) {
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .cardSurface {
                this.liquidGlassOr(shape, isDark, LocalPanelTransparency.current, elevation = 4.dp, backdropBlur = false) {
                    this.clip(shape).background(cardTone(surfaceColor)).border(1.5.dp, AccentGradient, shape)
                }
            }
            .clip(shape)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        YouTubeChannelLogo(name = name.ifBlank { "?" }, avatarUrl = avatarUrl, size = 48.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = primaryText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Subscribed \u2022 latest videos", color = secondaryText, fontSize = 12.sp, maxLines = 1)
        }
    }
}
