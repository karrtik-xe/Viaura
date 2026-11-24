package com.example.ui.components

import com.example.ui.theme.Neutral

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.example.util.OnlineMediaLibrary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AudioPlaybackManager
import com.example.ui.screens.ChannelTabsRow
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.screens.AudioTrackGridCard
import com.example.ui.screens.AudioTrackItem
import com.example.ui.screens.AudioTrackListCard
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.AudioPlaybackHistoryManager
import com.example.util.OnlineChannelResult
import com.example.util.OnlineMusicService
import com.example.util.OnlineInterestProfile
import com.example.util.OnlineRecommendationMixer
import com.example.util.MusicRecoEngine
import com.example.util.MusicTaste
import com.example.util.MusicListeningTracker
import com.example.util.MusicFeedSection
import com.example.util.MusicArtist
import com.example.util.OnlineTrackResult
import com.example.util.OnlineTrackSequencer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import java.util.Locale

/**
 * Online Music browsing and search panel.
 * Tapping a result resolves the stream URL and auto-queues recommendations via NewPipeExtractor.
 * Visual cards use AudioTrackListCard / AudioTrackGridCard to match offline media 100%.
 */
@Composable
fun OnlineMusicPanel(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    layoutMode: LayoutMode = LayoutMode.LIST,
    visibleFields: VisibleFields = VisibleFields(),
    sortField: SortField = SortField.TITLE,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    onOpenChannel: ((OnlineChannelResult) -> Unit)? = null,
    onOpenSharedPlaylist: ((com.example.util.OnlinePlaylistDetail) -> Unit)? = null,
    glassBlurTransparency: Float = 85f
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Online audio has no List / Grid choice any more: search results are always a list
    @Suppress("NAME_SHADOWING")
    val layoutMode = LayoutMode.LIST

    val currentPlayingTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isPlaying by AudioPlaybackManager.isPlaying.collectAsState()
    val currentPositionMs by AudioPlaybackManager.currentPositionMs.collectAsState()

    var metaUpdateTrigger by remember { mutableStateOf(0) }

    val st = OnlineAudioFeedState
    var results by st::results
    var channelResults by st::channelResults
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var loadingVideoUrl by remember { mutableStateOf<String?>(null) }
    var hasSearchedOnce by st::hasSearchedOnce

    var activeExtractor by st::extractor
    var nextPage by st::nextPage
    var hasNextPage by st::hasNextPage
    var searchSuggestion by st::searchSuggestion
    var isCorrectedSearch by st::isCorrectedSearch

    val listState = rememberLazyListState(st.listIndex, st.listOffset)
    val gridState = rememberLazyGridState(st.gridIndex, st.gridOffset)
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
            // a load cut short by leaving the screen must not leave a spinner stuck
            st.isRefreshing = false
        }
    }

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
    var watchLaterTracks by remember { mutableStateOf<List<OnlineTrackResult>>(emptyList()) }
    var hiddenVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        OnlineMediaLibrary.attach(context)
        watchLaterTracks = withContext(Dispatchers.IO) { OnlineMediaLibrary.watchLater(context, OnlineMediaLibrary.KIND_AUDIO) }
    }
    val hiddenUrls = remember(hiddenVersion) { OnlineMediaLibrary.notInterestedSet(context, OnlineMediaLibrary.KIND_AUDIO) }
    BackHandler(enabled = actionTarget != null) { actionTarget = null }

    // Search query tracking with cancellation
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Blank search box = the "Recommended for you" feed (NOT a search). It is an endless feed: every time the
    // user nears the bottom another mixed batch (65% like their taste / 35% other genres) is appended.
    val isFeedMode = searchQuery.isBlank()
    val feedUsedQueries = st.usedQueries
    // Bumped whenever the feed is reset or a real search starts, so a slow feed batch that finishes late is
    // discarded instead of being appended to unrelated results.
    val feedToken = remember { intArrayOf(0) }

    fun loadFeed() {
        searchJob?.cancel()
        feedToken[0]++
        val token = feedToken[0]
        isSearching = true
        isLoadingMore = false
        hasSearchedOnce = true
        // No search happened, so no "Did you mean" / "Showing results for" pill and no channel cards.
        searchSuggestion = null
        isCorrectedSearch = false
        channelResults = emptyList()
        activeExtractor = null
        nextPage = null
        hasNextPage = false
        st.lastQuery = ""
        feedUsedQueries.clear()
        searchJob = scope.launch(Dispatchers.IO) {
            try {
                // the feed itself is built by MusicRecoEngine (see loadHome); this keeps the saved songs for offline / fast start
                val batch = try {
                    MusicRecoEngine.cachedFeed(context).flatMap { it.tracks }.distinctBy { it.videoUrl }.take(60)
                } catch (t: kotlinx.coroutines.CancellationException) {
                    throw t
                } catch (_: Throwable) {
                    emptyList<OnlineTrackResult>()
                }
                withContext(Dispatchers.Main) {
                    if (token == feedToken[0]) {
                        if (batch.isNotEmpty() || results.isEmpty()) {
                            results = batch
                            st.generation++
                            st.savedFeed = null
                        }
                        st.initialized = true
                        isSearching = false
                    }
                }
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    if (token == feedToken[0]) isSearching = false
                }
            }
        }
    }

    // ---- Online music home (blank search box): built by MusicRecoEngine, endless ----
    val home = OnlineMusicHomeState
    // The home on screen is NEVER cleared: the new one is built in the background and swapped in when it is ready
    // (no full-screen loading animation, no jump to the top). The pull-to-refresh animation ends when the swap is done.
    fun loadHome(reset: Boolean) {
        if (home.loading && !reset) return
        home.loading = true
        scope.launch {
            try {
                val used = if (reset) mutableSetOf<String>() else home.usedKeys
                if (home.sections.isEmpty()) {
                    // instantly from what is saved on the phone (works offline)
                    val cached = withContext(Dispatchers.IO) { OmHomeBuilder.cachedHome(context, used) }
                    if (cached.isNotEmpty()) { home.sections.addAll(cached); home.usedKeys.addAll(used) }
                }
                val usedFresh = mutableSetOf<String>()
                val first = withContext(Dispatchers.IO) { OmHomeBuilder.firstBatch(context, usedFresh) }
                if (first.size >= 2 || home.sections.isEmpty()) {
                    home.sections.clear()
                    home.sections.addAll(first)
                    home.usedKeys.clear()
                    home.usedKeys.addAll(usedFresh)
                    home.batch = 1
                    // the other tabs are built again from the new taste the next time they are opened
                    home.albums.clear(); home.artists.clear(); home.playlists.clear(); home.tabLoaded.clear()
                }
                home.lastBuild = System.currentTimeMillis()
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
            } finally {
                home.loading = false
                st.isRefreshing = false
            }
        }
    }
    fun loadMoreHome() {
        if (home.tab != "home" || home.loading || home.sections.isEmpty()) return
        home.loading = true
        home.loadingMore = true
        scope.launch {
            try {
                val more = withContext(Dispatchers.IO) { OmHomeBuilder.moreBatch(context, home.batch, home.usedKeys) }
                home.sections.addAll(more)
                home.batch++
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
            } finally {
                home.loading = false
                home.loadingMore = false
            }
        }
    }
    fun loadTab(tab: String) {
        if (tab == "home" || tab in home.tabLoaded) return
        home.tabLoaded.add(tab)
        home.tabLoading = true
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) {
                    when (tab) {
                        "albums" -> OmHomeBuilder.albumsTab(context)
                        "artists" -> OmHomeBuilder.artistsTab(context)
                        else -> OmHomeBuilder.playlistsTab(context)
                    }
                }
                val target = when (tab) { "albums" -> home.albums; "artists" -> home.artists; else -> home.playlists }
                target.clear(); target.addAll(list)
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                home.tabLoaded.remove(tab)
            } finally {
                home.tabLoading = false
            }
        }
    }
    LaunchedEffect(isFeedMode) {
        if (isFeedMode && home.sections.isEmpty()) loadHome(false)
    }
    LaunchedEffect(home.tab, isFeedMode) { if (isFeedMode) loadTab(home.tab) }
    // listening is tracked from the real playback (search never counts); a changed taste rebuilds the home quietly, at most every 90 s
    LaunchedEffect(Unit) {
        MusicListeningTracker.start(context)
        val initial = MusicTaste.changes.value
        MusicTaste.changes.collect { if (it != initial) home.stale = true }
    }
    LaunchedEffect(isFeedMode, home.stale) {
        if (isFeedMode && home.stale && home.sections.isNotEmpty()) {
            delay(((home.lastBuild + 90_000L) - System.currentTimeMillis()).coerceAtLeast(4000L))
            home.stale = false
            loadHome(true)
        }
    }

    fun executeSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) {
            // search cleared: the feed that was there before the search comes back as it was (no new load)
            val saved = st.savedFeed
            if (saved != null && saved.isNotEmpty()) {
                searchJob?.cancel()
                feedToken[0]++
                results = saved
                st.savedFeed = null
                channelResults = emptyList()
                activeExtractor = null
                nextPage = null
                hasNextPage = false
                searchSuggestion = null
                isCorrectedSearch = false
                st.lastQuery = ""
                st.initialized = true
                isSearching = false
                st.generation++
                st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                st.scrollTopTick++
            } else if (st.initialized && st.lastQuery.isBlank() && results.isNotEmpty()) {
                // the feed is already on screen (a search was typed and cleared again before it finished)
                searchJob?.cancel()
                feedToken[0]++
                isSearching = false
            } else loadFeed()
            return
        }
        if (st.lastQuery.isBlank() && results.isNotEmpty()) st.savedFeed = results
        feedToken[0]++
        searchJob?.cancel()
        isSearching = true
        hasSearchedOnce = true

        // If user entered or pasted a YouTube Music / Spotify playlist or song link
        if (onOpenSharedPlaylist != null && com.example.util.OnlineMusicService.isMusicPlaylistOrTrackUrl(trimmed)) {
            searchJob = scope.launch(Dispatchers.IO) {
                val pl = com.example.util.OnlineMusicService.fetchPlaylistDetails(trimmed)
                withContext(Dispatchers.Main) {
                    isSearching = false
                    if (pl != null && pl.tracks.isNotEmpty()) {
                        OnlineMusicHomeStats.recordPlaylist(context, pl)
                        onOpenSharedPlaylist(pl)
                    } else {
                        android.widget.Toast.makeText(context, "Could not load playlist from link", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            return
        }

        val searchToken = feedToken[0]
        searchJob = scope.launch(Dispatchers.IO) {
            val (extractor, page) = try {
                OnlineMusicService.searchMusicPage(trimmed)
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    if (searchToken == feedToken[0]) { results = emptyList(); channelResults = emptyList(); isSearching = false }
                    st.isRefreshing = false
                }
                return@launch
            }
            // the original / most played upload of what was typed is already first (see OnlineSearchRanker);
            // what is only searched is NOT used for recommendations - only played songs are
            val ranked = page.items

            withContext(Dispatchers.Main) {
                if (searchToken != feedToken[0]) return@withContext
                activeExtractor = extractor
                nextPage = page.nextPage
                hasNextPage = page.hasNextPage
                searchSuggestion = page.searchSuggestion
                isCorrectedSearch = page.isCorrectedSearch
                results = ranked
                channelResults = page.channelResults
                st.lastQuery = trimmed
                st.initialized = true
                isSearching = false
                st.isRefreshing = false
                // a search always shows its top result first: new keys + a scroll to the top once the new items are laid out
                st.generation++
                st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                st.scrollTopTick++
            }
        }
    }

    fun loadMore() {
        val extractor = activeExtractor ?: return
        val next = nextPage ?: return
        if (!hasNextPage || isLoadingMore || isSearching) return

        isLoadingMore = true
        scope.launch(Dispatchers.IO) {
            try {
                val nextPageResult = OnlineMusicService.fetchNextMusicPage(extractor, next)
                withContext(Dispatchers.Main) {
                    if (nextPageResult.items.isNotEmpty()) {
                        val existingUrls = results.map { it.videoUrl }.toSet()
                        val newItems = nextPageResult.items.filter { it.videoUrl !in existingUrls }
                        results = results + newItems
                    }
                    nextPage = nextPageResult.nextPage
                    hasNextPage = nextPageResult.hasNextPage
                    isLoadingMore = false
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoadingMore = false
                }
            }
        }
    }

    // Debounced search reaction to searchQuery
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(searchQuery) {
        if (firstRun) {
            firstRun = false
            // coming back to the folder: keep what is already loaded (no new load, no loading animation)
            if (st.initialized && searchQuery.isBlank()) {
                if (st.lastQuery.isNotBlank()) executeSearch("") // left during a search: the feed comes back
                return@LaunchedEffect
            }
            // back from a song with the same search still in the box: its list (and place in it) is kept, nothing is searched again
            if (st.initialized && searchQuery.isNotBlank() && searchQuery.trim().equals(st.lastQuery.trim(), ignoreCase = true)) {
                return@LaunchedEffect
            }
        }
        if (searchQuery.isNotBlank()) {
            if (!searchQuery.trim().equals(OnlineSearchBridge.lastPicked.trim(), ignoreCase = true)) delay(350)
            executeSearch(searchQuery)
        } else {
            executeSearch("")
        }
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

    // Pull down from the top (Home screen gesture) = brand-new recommendations / the search again
    var handledRefresh by remember { mutableIntStateOf(st.refreshRequest) }
    LaunchedEffect(st.refreshRequest) {
        if (st.refreshRequest != handledRefresh) {
            handledRefresh = st.refreshRequest
            if (isFeedMode) { home.stale = false; loadHome(true) } else executeSearch(searchQuery)
        }
    }
    // Safety net: the pull-to-refresh indicator can never stay on screen for more than a few seconds.
    LaunchedEffect(st.isRefreshing) {
        if (st.isRefreshing) {
            delay(10000L)
            st.isRefreshing = false
        }
    }

    // Unlimited pagination scroll trigger
    val shouldLoadMore by remember(layoutMode) {
        derivedStateOf {
            if (layoutMode == LayoutMode.GRID) {
                val totalItems = gridState.layoutInfo.totalItemsCount
                val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleIndex >= totalItems - 4
            } else {
                val totalItems = listState.layoutInfo.totalItemsCount
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleIndex >= totalItems - 4
            }
        }
    }

    // re-checked after every load (home.loading / the section count change), so the endless scroll never stalls at the end
    LaunchedEffect(shouldLoadMore, home.sections.size, home.loading, home.tab) {
        if (shouldLoadMore && !isLoadingMore && !isSearching) {
            if (isFeedMode) loadMoreHome() else if (hasNextPage) loadMore()
        }
    }

    fun playOnlineTrack(result: OnlineTrackResult) {
        val stableId = -(kotlin.math.abs(result.videoUrl.hashCode().toLong()))
        val selectedTrack = AudioTrackItem(
            id = stableId,
            uri = Uri.EMPTY,
            title = result.title,
            artist = result.artist,
            album = "Online Music",
            durationMs = result.durationSec * 1000L,
            path = "$ONLINE_TRACK_PATH_PREFIX${result.videoUrl}",
            sizeBytes = 0L,
            format = "STREAM",
            audioType = "Stereo",
            hasLyrics = false,
            lyricsText = "",
            thumbnailUrl = result.thumbnailUrl
        )

        // 1. Immediately open full Audio Player Screen
        AudioPlaybackManager.openFullScreen()

        // 2. Immediately stop previous audio and play new track (buffering animation starts immediately)
        AudioPlaybackManager.playTrack(
            context = context,
            track = selectedTrack,
            newQueue = listOf(selectedTrack)
        )

        scope.launch(Dispatchers.IO) {
            // repeat count for the speed dial + a clear seed for the recommendations (he tapped this song himself)
            OnlineMusicHomeStats.bump(context, result)
            MusicTaste.onUserPicked(context, result)
            // Lyrics are fetched centrally by AudioPlaybackManager.loadLyricsFor().
            // The next songs: same kind of music as the song that is really playing (genre / sub-genre / era / language), other
            // artists, some fresh - built by MusicRecoEngine. The listening itself is tracked by MusicListeningTracker.
            val uniqueCandidates = try {
                MusicRecoEngine.queueAfterPlay(context, result, 30)
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList<OnlineTrackResult>()
            }

            if (uniqueCandidates.isEmpty()) return@launch

            // Pre-resolve direct stream URLs for the first 5 tracks in parallel with timeout
            val preResolveCount = uniqueCandidates.size.coerceAtMost(5)
            val preResolvedMap = uniqueCandidates.take(preResolveCount).map { item ->
                async {
                    val resolved = withTimeoutOrNull(3500L) {
                        OnlineMusicService.resolveStreamUrl(item.videoUrl)
                    }
                    item.videoUrl to resolved
                }
            }.awaitAll().toMap()

            val recommendedTrackItems = uniqueCandidates.map { item ->
                val recStableId = -(kotlin.math.abs(item.videoUrl.hashCode().toLong()))
                val resolvedUrl = preResolvedMap[item.videoUrl]
                val uri = if (resolvedUrl != null) Uri.parse(resolvedUrl) else Uri.EMPTY
                AudioTrackItem(
                    id = recStableId,
                    uri = uri,
                    title = item.title,
                    artist = item.artist,
                    album = "Online Music",
                    durationMs = item.durationSec * 1000L,
                    path = "$ONLINE_TRACK_PATH_PREFIX${item.videoUrl}",
                    sizeBytes = 0L,
                    format = "STREAM",
                    audioType = "Stereo",
                    thumbnailUrl = item.thumbnailUrl
                )
            }

            withContext(Dispatchers.Main) {
                val currentQ = AudioPlaybackManager.queue.value
                val existingIds = currentQ.map { it.id }.toSet()
                val itemsToAdd = recommendedTrackItems.filter { it.id !in existingIds }
                if (itemsToAdd.isNotEmpty()) {
                    AudioPlaybackManager.updateQueue(currentQ + itemsToAdd)
                }
            }
        }
    }

    fun playQueueFrom(list: List<OnlineTrackResult>) {
        if (list.isEmpty()) return
        val items = list.map { it.toAudioTrackItem() }
        AudioPlaybackManager.openFullScreen()
        AudioPlaybackManager.playTrack(context = context, track = items.first(), newQueue = items)
        scope.launch(Dispatchers.IO) {
            OnlineMusicHomeStats.bump(context, list.first())
            MusicTaste.onUserPicked(context, list.first())
        }
    }
    fun openHomePlaylist(url: String) {
        val open = onOpenSharedPlaylist ?: return
        Toast.makeText(context, "Loading playlist...", Toast.LENGTH_SHORT).show()
        scope.launch {
            val pl = withContext(Dispatchers.IO) { com.example.util.OnlineMusicService.fetchPlaylistDetails(url) }
            if (pl != null && pl.tracks.isNotEmpty()) {
                OnlineMusicHomeStats.recordPlaylist(context, pl)
                open(pl)
            } else {
                Toast.makeText(context, "Could not load playlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openArtistByName(name: String) {
        val open = onOpenChannel ?: return
        Toast.makeText(context, "Opening $name...", Toast.LENGTH_SHORT).show()
        scope.launch {
            val ch = withContext(Dispatchers.IO) {
                try { com.example.util.OnlineMusicService.searchArtistChannel(name) } catch (t: kotlinx.coroutines.CancellationException) { throw t } catch (_: Throwable) { null }
            }
            // no channel found: the artist page still opens (songs and playlists of the name come from search)
            open(ch ?: OnlineChannelResult(channelUrl = "artist:$name", name = name, avatarUrl = null, subscriberCountText = null, videoCountText = null, description = null))
        }
    }

    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    // Sort tracks according to global sort parameters
    val sortedTracks = remember(results, sortField, sortDirection, isFeedMode, st.sortTouched) {
        // The recommendation feed keeps its mixed order (and must not reshuffle while new batches append).
        // search results keep their relevance order (original + most popular first) until the user picks a sort order himself
        if (isFeedMode || !st.sortTouched) results else when (sortField) {
            SortField.TITLE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    results.sortedBy { it.title.lowercase(Locale.ROOT) }
                } else {
                    results.sortedByDescending { it.title.lowercase(Locale.ROOT) }
                }
            }
            SortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    results.sortedBy { it.uploadDateEpochMs ?: 0L }
                } else {
                    results.sortedByDescending { it.uploadDateEpochMs ?: 0L }
                }
            }
            SortField.DURATION -> {
                if (sortDirection == SortDirection.DESCENDING) results.sortedByDescending { it.durationSec }
                else results.sortedBy { it.durationSec }
            }
            SortField.SIZE -> results
            SortField.COUNT -> results
        }
    }

    // Watch-later songs are pinned above everything else in the feed; "Not interested" ones never show.
    val shownTracks = remember(sortedTracks, isFeedMode, watchLaterTracks, hiddenUrls) {
        val pinned = if (isFeedMode) watchLaterTracks.filter { it.videoUrl !in hiddenUrls } else emptyList()
        val pinnedUrls = pinned.map { it.videoUrl }.toSet()
        pinned + sortedTracks.filter { it.videoUrl !in pinnedUrls && it.videoUrl !in hiddenUrls }
    }

    LaunchedEffect(sortedTracks) {
        withContext(Dispatchers.IO) {
            sortedTracks.take(15).forEach { trackResult ->
                if (OnlineMusicService.getCachedAudioMeta(trackResult.videoUrl) == null) {
                    try {
                        OnlineMusicService.probeAudioMetadata(trackResult.videoUrl, trackResult.title)
                        withContext(Dispatchers.Main) {
                            metaUpdateTrigger++
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search suggestion or spell correction pill (same width / shape as the search box above it)
        AnimatedVisibility(visible = !searchSuggestion.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isDark) Neutral.D1E293B else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, if (isDark) DarkGlassBorder else LightGlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        searchSuggestion?.let { OnlineSearchBridge.pick(it) }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledIcon(imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isCorrectedSearch) "Showing results for " else "Did you mean: ",
                        fontSize = 13.sp,
                        color = secondaryText
                    )
                    Text(
                        text = searchSuggestion ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Suggestions (5 popular searches for the typed text); the results start below them
        OnlineSearchSuggestions(
            query = searchQuery,
            isDark = isDark,
            glassBlurTransparency = glassBlurTransparency,
            fetch = { OnlineMusicService.getSearchSuggestions(it) }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when {
            isFeedMode -> {
                Column(Modifier.fillMaxSize()) {
                    // thin sliding tab row, like the categories of Online videos
                    ChannelTabsRow(
                        tabs = listOf("home" to "Home", "albums" to "Albums", "artists" to "Artists", "playlists" to "Playlists"),
                        selectedId = home.tab,
                        onSelect = { home.tab = it },
                        isDark = isDark
                    )
                    val otherListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    val tabSections = when (home.tab) { "albums" -> home.albums; "artists" -> home.artists; "playlists" -> home.playlists; else -> home.sections }
                    OmHomeFeed(
                        sections = tabSections,
                        loadingMore = if (home.tab == "home") home.loadingMore else home.tabLoading,
                        listState = if (home.tab == "home") listState else otherListState,
                        isDark = isDark,
                        actionState = actionState,
                        onActionTarget = { actionTarget = it },
                        onPlayTrack = { actionTarget = null; playOnlineTrack(it) },
                        onPlayQueue = { actionTarget = null; playQueueFrom(it) },
                        onOpenPlaylist = { openHomePlaylist(it) },
                        onOpenChannel = { onOpenChannel?.invoke(it) },
                        onOpenArtist = { name -> openArtistByName(name) },
                        hidden = hiddenUrls,
                        emptyText = if (!(if (home.tab == "home") home.loading else home.tabLoading)) "Nothing here yet - connect to the internet or play some songs" else null
                    )
                }
            }
            isSearching && results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ViauraExpressiveLoadingIndicator(size = 54.dp, isDark = isDark)
                }
            }
            hasSearchedOnce && results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotBlank())
                            "No online songs found for \"$searchQuery\". Check your internet connection or try a different search."
                        else
                            "No online songs found. Check your internet connection.",
                        color = secondaryText,
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                if (layoutMode == LayoutMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (channelResults.isNotEmpty()) {
                            items(
                                items = channelResults,
                                key = { "online_audio_channel_${it.channelUrl}" },
                                span = { GridItemSpan(2) }
                            ) { channel ->
                                OnlineChannelResultCard(
                                    channel = channel,
                                    isDark = isDark,
                                    onClick = { onOpenChannel?.invoke(channel) }
                                )
                            }
                        }
                        items(shownTracks, key = { "oa_${st.generation}_${it.videoUrl}" }) { result ->
                            val baseTrackItem = remember(result, metaUpdateTrigger) { result.toAudioTrackItem() }
                            val isCurrent = currentPlayingTrack?.path == "$ONLINE_TRACK_PATH_PREFIX${result.videoUrl}"
                            val progress = if (isCurrent && baseTrackItem.durationMs > 0) {
                                (currentPositionMs.toFloat() / baseTrackItem.durationMs).coerceIn(0f, 1f)
                            } else {
                                val savedPos = AudioPlaybackHistoryManager.getPosition(context, baseTrackItem)
                                if (baseTrackItem.durationMs > 0) (savedPos.toFloat() / baseTrackItem.durationMs).coerceIn(0f, 1f) else 0f
                            }
                            val trackItem = if (progress != baseTrackItem.playbackProgress) baseTrackItem.copy(playbackProgress = progress) else baseTrackItem

                            Box(Modifier.swipeLeftForActions(selected = actionState.contains(result)) { actionTarget = result }) {
                                AudioTrackGridCard(
                                    track = trackItem,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    isDark = isDark,
                                    visibleFields = visibleFields.copy(showExtension = false),
                                    onClick = { actionTarget = null; playOnlineTrack(result) }
                                )
                            }
                        }
                        if (isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ViauraExpressiveLoadingIndicator(size = 36.dp, isDark = isDark)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (channelResults.isNotEmpty()) {
                            items(
                                items = channelResults,
                                key = { "online_audio_channel_${it.channelUrl}" }
                            ) { channel ->
                                OnlineChannelResultCard(
                                    channel = channel,
                                    isDark = isDark,
                                    onClick = { onOpenChannel?.invoke(channel) }
                                )
                            }
                        }
                        items(shownTracks, key = { "oa_${st.generation}_${it.videoUrl}" }) { result ->
                            val baseTrackItem = remember(result, metaUpdateTrigger) { result.toAudioTrackItem() }
                            val isCurrent = currentPlayingTrack?.path == "$ONLINE_TRACK_PATH_PREFIX${result.videoUrl}"
                            val progress = if (isCurrent && baseTrackItem.durationMs > 0) {
                                (currentPositionMs.toFloat() / baseTrackItem.durationMs).coerceIn(0f, 1f)
                            } else {
                                val savedPos = AudioPlaybackHistoryManager.getPosition(context, baseTrackItem)
                                if (baseTrackItem.durationMs > 0) (savedPos.toFloat() / baseTrackItem.durationMs).coerceIn(0f, 1f) else 0f
                            }
                            val trackItem = if (progress != baseTrackItem.playbackProgress) baseTrackItem.copy(playbackProgress = progress) else baseTrackItem

                            Box(Modifier.swipeLeftForActions(selected = actionState.contains(result)) { actionTarget = result }) {
                                AudioTrackListCard(
                                    track = trackItem,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    isDark = isDark,
                                    visibleFields = visibleFields.copy(showExtension = false),
                                    onClick = { actionTarget = null; playOnlineTrack(result) }
                                )
                            }
                        }
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ViauraExpressiveLoadingIndicator(size = 36.dp, isDark = isDark)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Swipe toolbar
        androidx.compose.animation.AnimatedVisibility(
            visible = actionTarget != null,
            enter = androidx.compose.animation.slideInVertically { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it } + androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(vertical = 18.dp)
        ) {
            val t = actionTarget ?: lastActionTarget
            if (t != null) {
                val kind = OnlineMediaLibrary.KIND_AUDIO
                // every action works on ALL selected cards (one card = the same as before)
                val picks = actionState.items.toList().ifEmpty { listOf(t) }
                OnlineQuickActionBar(
                    isDark = isDark,
                    alphaRatio = (glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    isSavedForLater = picks.all { p -> watchLaterTracks.any { it.videoUrl == p.videoUrl } },
                    selectedCount = actionState.size,
                    onClearSelection = { actionTarget = null },
                    onDownload = {
                        if (actionState.size > 1) {
                            batchRequests = actionState.items.map { x -> com.example.ui.components.DownloadRequest(x.videoUrl, x.title, x.artist, x.thumbnailUrl, true) }
                        } else {
                            downloadRequest = com.example.ui.components.DownloadRequest(t.videoUrl, t.title, t.artist, t.thumbnailUrl, true)
                        }
                        actionTarget = null
                    },
                    onPlayNext = {
                        // reversed: the selected songs keep their order right after the current one
                        picks.asReversed().forEach { v -> AudioPlaybackManager.playNext(v.toAudioTrackItem()) }
                        Toast.makeText(context, if (picks.size > 1) "${picks.size} songs will play next" else "Will play next: ${t.title}", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onPlayLast = {
                        picks.forEach { v -> AudioPlaybackManager.playLast(v.toAudioTrackItem()) }
                        Toast.makeText(context, if (picks.size > 1) "${picks.size} songs added to queue" else "Added to queue: ${t.title}", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onSaveToPlaylist = {
                        picks.forEach { v -> OnlineMediaLibrary.rememberMeta(context, kind, v) }
                        playlistTargets = picks
                        actionTarget = null
                    },
                    onWatchLater = {
                        // all already saved -> remove them all, otherwise save the ones that are not saved yet
                        val allSaved = picks.all { p -> watchLaterTracks.any { it.videoUrl == p.videoUrl } }
                        var saved = false
                        picks.forEach { v ->
                            val isSaved = watchLaterTracks.any { it.videoUrl == v.videoUrl }
                            if (allSaved || !isSaved) saved = OnlineMediaLibrary.toggleWatchLater(context, kind, v)
                        }
                        watchLaterTracks = OnlineMediaLibrary.watchLater(context, kind)
                        Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onNotInterested = {
                        picks.forEach { v ->
                            OnlineMediaLibrary.markNotInterested(context, kind, v.videoUrl)
                            if (kind == OnlineMediaLibrary.KIND_AUDIO) {
                                MusicTaste.markRejected(context, v.videoUrl, v.artist)
                                OnlineMusicHomeStats.forget(context, v.videoUrl)
                            }
                        }
                        watchLaterTracks = OnlineMediaLibrary.watchLater(context, kind)
                        hiddenVersion++
                        Toast.makeText(context, "Okay, you will see fewer songs like this", Toast.LENGTH_SHORT).show()
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
                selectedAudioTracks = playlistTargets.map { it.toAudioTrackItem() },
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { playlistTargets = emptyList() },
                onPlaylistUpdated = { playlistTargets = emptyList() }
            )
        }
    }
}
}

// =====================================================================================================================
// ONLINE MUSIC HOME (blank search box): speed dial, song mixes, 1:1 rows, artist rows and playlist cards, endless.
// Everything is built from what the listener plays (repeat count, history, taste) plus internet playlists / albums.
// =====================================================================================================================

/** One tile: a song (track), a ready made mix (tracks), a playlist / album (playlistUrl) or an artist (channel). */
internal data class OmHomeTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val covers: List<String>,
    val track: OnlineTrackResult? = null,
    val tracks: List<OnlineTrackResult> = emptyList(),
    val playlistUrl: String? = null,
    val channel: OnlineChannelResult? = null
)

/** A card with a cover, a play button and the first songs (the "Energise" / artist card). */
internal data class OmHomeSongCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val cover: String?,
    val tracks: List<OnlineTrackResult>,
    val playlistUrl: String?,
    val channel: OnlineChannelResult?
)

internal sealed class OmHomeSection(val key: String) {
    class SpeedDial(val tiles: List<OmHomeTile>) : OmHomeSection("om_speed_dial")
    class SongMixes(val tracks: List<OnlineTrackResult>) : OmHomeSection("om_song_mixes")
    class Squares(key: String, val title: String, val items: List<OmHomeTile>) : OmHomeSection(key)
    class ArtistRow(key: String, val channel: OnlineChannelResult, val items: List<OmHomeTile>) : OmHomeSection(key)
    class Community(key: String, val title: String, val items: List<OmHomeTile>) : OmHomeSection(key)
    class SongCards(key: String, val title: String, val cards: List<OmHomeSongCard>) : OmHomeSection(key)
    class Artists(key: String, val title: String, val artists: List<MusicArtist>) : OmHomeSection(key)
}

/** Survives leaving and coming back to the screen, like OnlineAudioFeedState. */
internal object OnlineMusicHomeState {
    val sections = androidx.compose.runtime.mutableStateListOf<OmHomeSection>()
    var loading by mutableStateOf(false)
    /** True only while the endless scroll appends more (the bottom spinner). */
    var loadingMore by mutableStateOf(false)
    /** Set when a song is played / a playlist is opened: the home is rebuilt around the new taste. */
    var stale by mutableStateOf(false)
    var batch = 0
    var lastBuild = 0L
    val usedKeys = mutableSetOf<String>()
    /** Otube tabs: home | albums | artists | playlists (the home sections are [sections]). */
    var tab by mutableStateOf("home")
    val albums = androidx.compose.runtime.mutableStateListOf<OmHomeSection>()
    val artists = androidx.compose.runtime.mutableStateListOf<OmHomeSection>()
    val playlists = androidx.compose.runtime.mutableStateListOf<OmHomeSection>()
    var tabLoading by mutableStateOf(false)
    val tabLoaded = mutableSetOf<String>()
}

/** Repeat counts of played online songs and the playlists the listener opened (feeds the speed dial). */
internal object OnlineMusicHomeStats {
    private const val PREFS = "viaura_online_music_home"

    private fun prefs(c: android.content.Context) = c.applicationContext.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)

    private fun read(c: android.content.Context, key: String): org.json.JSONObject =
        try { org.json.JSONObject(prefs(c).getString(key, "{}") ?: "{}") } catch (_: Throwable) { org.json.JSONObject() }

    @Synchronized
    fun bump(c: android.content.Context, r: OnlineTrackResult) {
        try {
            val songs = read(c, "songs")
            val o = songs.optJSONObject(r.videoUrl) ?: org.json.JSONObject()
            o.put("title", r.title).put("artist", r.artist).put("thumb", r.thumbnailUrl ?: "")
                .put("dur", r.durationSec).put("count", o.optInt("count", 0) + 1).put("last", System.currentTimeMillis())
            songs.put(r.videoUrl, o)
            if (songs.length() > 300) {
                // keep the 250 most played / recent
                val keys = songs.keys().asSequence().toList()
                val keep = keys.sortedByDescending { k -> songs.getJSONObject(k).optInt("count") * 1_000_000_000_000L + songs.getJSONObject(k).optLong("last") }.take(250).toSet()
                keys.filter { it !in keep }.forEach { songs.remove(it) }
            }
            prefs(c).edit().putString("songs", songs.toString()).apply()
        } catch (_: Throwable) {}
        OnlineMusicHomeState.stale = true
    }

    /** The songs played last (newest first): what the listener is into right now. */
    fun recent(c: android.content.Context, limit: Int): List<OnlineTrackResult> =
        sorted(c, compareByDescending<Pair<String, org.json.JSONObject>> { it.second.optLong("last") }, limit)

    /** The 3 newest songs first (so a song just played is in the speed dial at once), then the most repeated ones. */
    fun topSongs(c: android.content.Context, limit: Int): List<OnlineTrackResult> {
        val newest = recent(c, 3)
        val rest = sorted(
            c,
            compareByDescending<Pair<String, org.json.JSONObject>> { it.second.optInt("count") }.thenByDescending { it.second.optLong("last") },
            limit + 3
        ).filter { r -> newest.none { it.videoUrl == r.videoUrl } }
        return (newest + rest).take(limit)
    }

    private fun sorted(c: android.content.Context, order: Comparator<Pair<String, org.json.JSONObject>>, limit: Int): List<OnlineTrackResult> {
        return try {
            val songs = read(c, "songs")
            songs.keys().asSequence().toList().map { k -> k to songs.getJSONObject(k) }
                .sortedWith(order)
                .take(limit)
                .map { (k, o) ->
                    OnlineTrackResult(
                        videoUrl = k, title = o.optString("title"), artist = o.optString("artist"),
                        durationSec = o.optLong("dur"), thumbnailUrl = o.optString("thumb").ifBlank { null }
                    )
                }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @Synchronized
    fun recordPlaylist(c: android.content.Context, d: com.example.util.OnlinePlaylistDetail) {
        try {
            val pls = read(c, "playlists")
            val covers = org.json.JSONArray()
            (d.previewThumbnails.ifEmpty { d.tracks.mapNotNull { it.thumbnailUrl } }).take(4).forEach { covers.put(it) }
            val o = org.json.JSONObject().put("title", d.title).put("author", d.author).put("count", d.trackCountText ?: "")
                .put("cover", d.coverUrl ?: "").put("covers", covers).put("last", System.currentTimeMillis())
            pls.put(d.playlistUrl, o)
            prefs(c).edit().putString("playlists", pls.toString()).apply()
        } catch (_: Throwable) {}
        OnlineMusicHomeState.stale = true
    }

    /** "Not interested": the song leaves the repeat counter (and so the speed dial) for good. */
    @Synchronized
    fun forget(c: android.content.Context, url: String) {
        try {
            val songs = read(c, "songs")
            if (songs.has(url)) {
                songs.remove(url)
                prefs(c).edit().putString("songs", songs.toString()).apply()
            }
        } catch (_: Throwable) {}
    }

    fun playlists(c: android.content.Context, limit: Int): List<OmHomeTile> {
        return try {
            val pls = read(c, "playlists")
            pls.keys().asSequence().toList().map { k -> k to pls.getJSONObject(k) }
                .sortedByDescending { it.second.optLong("last") }
                .take(limit)
                .map { (k, o) ->
                    val covers = o.optJSONArray("covers")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
                        .ifEmpty { listOfNotNull(o.optString("cover").ifBlank { null }) }
                    OmHomeTile(id = "pl_$k", title = o.optString("title"), subtitle = o.optString("author"), covers = covers, playlistUrl = k)
                }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

private object OmHomeBuilder {
    /** "Not interested" songs of the moment the home is built: they are in no row, no speed dial and no mix. */
    @Volatile private var hiddenNow: Set<String> = emptySet()
    private fun refreshHidden(c: android.content.Context) { hiddenNow = OnlineMediaLibrary.notInterestedSet(c, OnlineMediaLibrary.KIND_AUDIO) }

    private fun baseKey(t: OnlineTrackResult) = "t:" + MusicRecoEngine.songKeyOf(t.artist, t.title)

    private fun playlistTile(p: com.example.util.OnlineHomePlaylist, label: String) = OmHomeTile(
        id = "pl_${p.url}", title = p.name,
        subtitle = if (p.owner.isNotBlank()) "$label \u2022 ${p.owner}" else label,
        covers = listOfNotNull(p.thumbnailUrl), playlistUrl = p.url
    )

    private fun trackTile(t: OnlineTrackResult) = OmHomeTile(
        id = "tr_${t.videoUrl}", title = t.title, subtitle = t.artist, covers = listOfNotNull(t.thumbnailUrl), track = t
    )

    /** One song / playlist is shown once on the whole home: different uploads / versions of the same song count as the same song. */
    private fun uniqueTile(t: OmHomeTile, used: MutableSet<String>): Boolean =
        if (t.track != null) (t.track.videoUrl !in hiddenNow && used.add(baseKey(t.track))) else used.add("i:" + t.id)

    private fun dedupe(sections: List<OmHomeSection>, used: MutableSet<String>): List<OmHomeSection> {
        val out = ArrayList<OmHomeSection>()
        for (sec in sections) {
            when (sec) {
                is OmHomeSection.SpeedDial -> { sec.tiles.forEach { uniqueTile(it, used) }; out.add(OmHomeSection.SpeedDial(sec.tiles.filter { it.track == null || it.track.videoUrl !in hiddenNow })) }
                is OmHomeSection.SongMixes -> {
                    val l = sec.tracks.filter { it.videoUrl !in hiddenNow && used.add(baseKey(it)) }
                    if (l.size >= 4) out.add(OmHomeSection.SongMixes(l))
                }
                is OmHomeSection.Squares -> sec.items.filter { uniqueTile(it, used) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares(sec.key, sec.title, it)) }
                is OmHomeSection.ArtistRow -> sec.items.filter { uniqueTile(it, used) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.ArtistRow(sec.key, sec.channel, it)) }
                is OmHomeSection.Community -> sec.items.filter { uniqueTile(it, used) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Community(sec.key, sec.title, it)) }
                is OmHomeSection.Artists -> out.add(sec)
                is OmHomeSection.SongCards -> {
                    val cards = sec.cards.mapNotNull { c ->
                        val t = c.tracks.filter { it.videoUrl !in hiddenNow && used.add(baseKey(it)) }
                        if (t.isEmpty()) null else c.copy(tracks = t)
                    }
                    if (cards.isNotEmpty()) out.add(OmHomeSection.SongCards(sec.key, sec.title, cards))
                }
            }
        }
        return out
    }

    /** 9 per page, at least 4 pages: most repeated / newest played songs, opened playlists, then charts of the listener's taste. */
    private fun speedDial(c: android.content.Context, fill: List<OnlineTrackResult>, extras: List<OnlineTrackResult>): OmHomeSection.SpeedDial {
        val songs = OnlineMusicHomeStats.topSongs(c, 60).filter { it.videoUrl !in hiddenNow }.take(40).map { trackTile(it) }
        val playlists = OnlineMusicHomeStats.playlists(c, 12)
        val mixed = ArrayList<OmHomeTile>()
        var si = 0
        var pi = 0
        while (si < songs.size || pi < playlists.size) {
            repeat(3) { if (si < songs.size) mixed.add(songs[si++]) }
            if (pi < playlists.size) mixed.add(playlists[pi++])
        }
        val seen = mixed.mapNotNull { it.track }.map { baseKey(it) }.toMutableSet()
        for (t in fill) {
            if (mixed.size >= 36) break
            if (t.videoUrl !in hiddenNow && seen.add(baseKey(t))) mixed.add(trackTile(t))
        }
        val dial = mixed.take(45).toMutableList()
        // exactly two fresh songs (different artists / genres), spread out: never next to each other
        val fresh = extras.filter { it.videoUrl !in hiddenNow && baseKey(it) !in seen }.distinctBy { baseKey(it) }.take(2)
        val slots = listOf(4, 16)
        fresh.forEachIndexed { i, t -> dial.add(slots[i].coerceAtMost(dial.size), trackTile(t)) }
        return OmHomeSection.SpeedDial(dial)
    }

    private fun convert(feed: List<MusicFeedSection>): List<OmHomeSection> = feed.mapNotNull { s ->
        when {
            s.id == "recommended" -> if (s.tracks.size >= 4) OmHomeSection.SongMixes(s.tracks) else null
            s.id == "artists" -> if (s.artists.isEmpty()) null else OmHomeSection.Artists("om_${s.id}", s.title, s.artists)
            s.id.startsWith("genre_") || s.id.startsWith("more_rel_") -> {
                val cards = s.tracks.chunked(3).filter { it.size == 3 }.take(4).map { ch ->
                    OmHomeSongCard("gc_${ch.first().videoUrl}", ch.first().artist.ifBlank { s.title }, "Popular songs", ch.first().thumbnailUrl, ch, null, null)
                }
                if (cards.isEmpty()) null else OmHomeSection.SongCards("om_${s.id}", s.title, cards)
            }
            else -> if (s.tracks.isEmpty()) null else OmHomeSection.Squares("om_${s.id}", s.title, s.tracks.map { trackTile(it) })
        }
    }

    /** Instant home from what is saved on the phone (works offline). */
    fun cachedHome(c: android.content.Context, used: MutableSet<String>): List<OmHomeSection> {
        refreshHidden(c)
        val feed = MusicRecoEngine.cachedFeed(c)
        if (feed.isEmpty() && OnlineMusicHomeStats.topSongs(c, 1).isEmpty()) return emptyList()
        val fill = feed.firstOrNull { it.id == "trending" }?.tracks.orEmpty() + feed.firstOrNull { it.id == "popular" }?.tracks.orEmpty()
        return dedupe(listOf(speedDial(c, fill, emptyList())) + convert(feed), used)
    }

    suspend fun firstBatch(c: android.content.Context, used: MutableSet<String>): List<OmHomeSection> = kotlinx.coroutines.coroutineScope {
        refreshHidden(c)
        val extrasJob = async { try { MusicRecoEngine.speedDialExtras(c, 2) } catch (t: kotlinx.coroutines.CancellationException) { throw t } catch (_: Throwable) { emptyList() } }
        val feed = MusicRecoEngine.feed(c, mutableSetOf())
        val fill = feed.firstOrNull { it.id == "trending" }?.tracks.orEmpty() + feed.firstOrNull { it.id == "popular" }?.tracks.orEmpty() + feed.firstOrNull { it.id == "new" }?.tracks.orEmpty()
        val out = dedupe(listOf(speedDial(c, fill, extrasJob.await())) + convert(feed), used)
        out.forEach { used.add(it.key) }
        out
    }

    /**
     * The endless scroll, in this order: the first 6 pages are playlists of the listener's OWN categories, the next 6 pages are categories
     * next to his (Phonk -> Brazilian phonk, Trap ...), after that new categories with songs and playlists. A page that finds nothing
     * moves on to the next category, so the scroll never stops.
     */
    suspend fun moreBatch(c: android.content.Context, batch: Int, used: MutableSet<String>): List<OmHomeSection> {
        refreshHidden(c)
        val fav = MusicRecoEngine.favoriteCategories(c).ifEmpty { listOf("Phonk", "Hindi", "Pop") }
        val adj = MusicRecoEngine.adjacentCategories(c).ifEmpty { MusicRecoEngine.diverseCategories(c) }
        val div = MusicRecoEngine.diverseCategories(c).ifEmpty { listOf("Indie", "Jazz", "Rock") }
        val variants = listOf("playlist", "mix", "best songs playlist", "hits playlist", "new songs playlist", "top playlist")
        for (attempt in 0 until 8) {
            val n = batch + attempt
            val stage = if (n < 6) 0 else if (n < 12) 1 else 2
            val cats = when (stage) { 0 -> fav; 1 -> adj; else -> div }
            val idx = when (stage) { 0 -> n; 1 -> n - 6; else -> n - 12 }
            val cat = cats[idx % cats.size]
            val variant = variants[(idx / cats.size.coerceAtLeast(1) + stage) % variants.size]
            val sections = ArrayList<OmHomeSection>()
            val pls = try { OnlineMusicService.searchHomePlaylists("$cat $variant", albums = false, limit = 10) } catch (t: kotlinx.coroutines.CancellationException) { throw t } catch (_: Throwable) { emptyList() }
            if (pls.isNotEmpty()) sections.add(OmHomeSection.Squares("om_more_pl_${cat}_$n", if (stage == 0) "$cat playlists" else "$cat for you", pls.map { playlistTile(it, "Playlist") }))
            // songs of the category: always added after the first stage, and also when no playlist was found, so a page is never empty
            if (stage >= 1 || sections.isEmpty()) {
                val songs = try { MusicRecoEngine.categorySongs(c, cat, mutableSetOf(), 12) } catch (t: kotlinx.coroutines.CancellationException) { throw t } catch (_: Throwable) { emptyList() }
                if (songs.size >= 3) sections.add(OmHomeSection.Squares("om_more_songs_${cat}_$n", if (stage == 0) "$cat mix" else "$cat songs", songs.map { trackTile(it) }))
            }
            val out = dedupe(sections, used)
            if (out.isNotEmpty()) {
                out.forEach { used.add(it.key) }
                return out
            }
        }
        // last resort: the engine's own genre sections
        val extra = try { MusicRecoEngine.moreSections(c, batch, mutableSetOf()) } catch (t: kotlinx.coroutines.CancellationException) { throw t } catch (_: Throwable) { emptyList() }
        return dedupe(convert(extra), used).also { l -> l.forEach { used.add(it.key) } }
    }

    // ---- tabs ----

    suspend fun artistsTab(c: android.content.Context): List<OmHomeSection> =
        MusicRecoEngine.artistSections(c).mapIndexed { i, (title, list) -> OmHomeSection.Artists("om_artists_${i}_$title", title, list) }

    suspend fun albumsTab(c: android.content.Context): List<OmHomeSection> = kotlinx.coroutines.coroutineScope {
        val (repeated, genres) = MusicRecoEngine.albumSeeds(c)
        val used = mutableSetOf<String>()
        val artistJobs = repeated.map { a -> async { a to OnlineMusicService.searchHomePlaylists(a, albums = true, limit = 8) } }
        val genreJobs = genres.map { g -> async { g to OnlineMusicService.searchHomePlaylists("$g albums", albums = true, limit = 8) } }
        val popular = async { OnlineMusicService.searchHomePlaylists("top albums 2026", albums = true, limit = 8) }
        val out = ArrayList<OmHomeSection>()
        artistJobs.awaitAll().forEach { (a, l) ->
            l.filter { used.add(it.url) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_alb_a_$a", "$a albums", it.map { p -> playlistTile(p, if (p.songCount in 1..7) "EP" else "Album") })) }
        }
        genreJobs.awaitAll().forEach { (g, l) ->
            l.filter { used.add(it.url) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_alb_g_$g", "$g albums", it.map { p -> playlistTile(p, if (p.songCount in 1..7) "EP" else "Album") })) }
        }
        popular.await().filter { used.add(it.url) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_alb_pop", "Popular albums", it.map { p -> playlistTile(p, "Album") })) }
        out
    }

    suspend fun playlistsTab(c: android.content.Context): List<OmHomeSection> = kotlinx.coroutines.coroutineScope {
        val genres = MusicRecoEngine.topGenres(c).ifEmpty { listOf("Phonk", "Hindi", "Pop") }.take(3)
        val mine = OnlineMusicHomeStats.playlists(c, 12)
        val jobs = genres.map { g -> async { g to OnlineMusicService.searchHomePlaylists("$g playlist best songs", albums = false, limit = 8) } }
        val out = ArrayList<OmHomeSection>()
        if (mine.isNotEmpty()) out.add(OmHomeSection.Squares("om_pl_mine", "Your playlists", mine))
        val used = mutableSetOf<String>()
        jobs.awaitAll().forEach { (g, l) ->
            l.filter { used.add(it.url) }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_pl_$g", "$g playlists", it.map { p -> playlistTile(p, "Playlist") })) }
        }
        out
    }
}

// ----------------------------------------------------------------------------------------------------------- UI

/** Covers stay in memory (48 MB) while the app runs: scrolling up and down never blanks a thumbnail that was loaded once. */
private val omThumbCache = object : android.util.LruCache<String, android.graphics.Bitmap>(48 * 1024 * 1024) {
    override fun sizeOf(key: String, value: android.graphics.Bitmap): Int = value.byteCount
}

@Composable
private fun OmThumb(url: String?, modifier: Modifier, isDark: Boolean) {
    val ctx = LocalContext.current
    var bmp by remember(url) { mutableStateOf<android.graphics.Bitmap?>(if (url.isNullOrBlank()) null else omThumbCache.get(url)) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank() && bmp == null) {
            val loaded = loadRemoteAudioThumbnail(url, targetSizePx = 400, context = ctx)
            if (loaded != null) {
                omThumbCache.put(url, loaded)
                bmp = loaded
            }
        }
    }
    Box(modifier = modifier.background(if (isDark) Color(0xFF1B1B1B) else Color(0xFFE2E8F0))) {
        bmp?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(), contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** A square cover: one picture, or a 2x2 collage when the tile has several covers (playlists / mixes). */
@Composable
private fun OmCover(covers: List<String>, modifier: Modifier, isDark: Boolean) {
    if (covers.size >= 4) {
        Column(modifier = modifier) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                OmThumb(covers[0], Modifier.weight(1f).fillMaxSize(), isDark)
                OmThumb(covers[1], Modifier.weight(1f).fillMaxSize(), isDark)
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                OmThumb(covers[2], Modifier.weight(1f).fillMaxSize(), isDark)
                OmThumb(covers[3], Modifier.weight(1f).fillMaxSize(), isDark)
            }
        }
    } else {
        OmThumb(covers.firstOrNull(), modifier, isDark)
    }
}

@Composable
private fun OmSectionTitle(title: String, primary: Color, trailing: (@Composable () -> Unit)? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (trailing != null) trailing()
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun OmSpeedDialTile(tile: OmHomeTile, isDark: Boolean, modifier: Modifier, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val isPlaylist = tile.playlistUrl != null
    Box(modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        OmCover(tile.covers, Modifier.fillMaxSize(), isDark)
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.78f)))
            )
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tile.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            if (isPlaylist) Text(text = "\u203A", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OmDots(count: Int, selected: Int, isDark: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
        repeat(count) { i ->
            val on = i == selected
            Box(
                Modifier.padding(horizontal = 3.dp).size(if (on) 9.dp else 7.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (on) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color(0xFF52525B) else Color(0xFFCBD5E1)))
            )
        }
    }
}

@Composable
private fun OmSongRow(
    track: OnlineTrackResult, isDark: Boolean, primary: Color, secondary: Color,
    selected: Boolean, onSwipe: () -> Unit, onClick: () -> Unit
) {
    val views = formatViews(track.viewCount)
    // A plain slide moves the row of songs sideways; touch and HOLD, then slide left = the actions bar (download, playlist ...)
    val dragged = remember { floatArrayOf(0f) }
    Row(
        modifier = Modifier.fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragged[0] = 0f },
                    onDrag = { change, amount -> change.consume(); dragged[0] += amount.x },
                    onDragEnd = { if (dragged[0] < -60f) onSwipe() },
                    onDragCancel = { dragged[0] = 0f }
                )
            }
            .clickable { onClick() }.padding(vertical = 6.dp)
            .then(if (selected) Modifier.background(Color(0x1A60A5FA), RoundedCornerShape(10.dp)) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OmThumb(track.thumbnailUrl, Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), isDark)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(track.artist, views).filter { it.isNotBlank() }.joinToString(" \u2022 "),
                color = secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OmHomeFeed(
    sections: List<OmHomeSection>,
    loadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isDark: Boolean,
    actionState: com.example.ui.components.SwipeActionState<OnlineTrackResult>,
    onActionTarget: (OnlineTrackResult) -> Unit,
    onPlayTrack: (OnlineTrackResult) -> Unit,
    onPlayQueue: (List<OnlineTrackResult>) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenChannel: (OnlineChannelResult) -> Unit,
    onOpenArtist: (String) -> Unit,
    hidden: Set<String> = emptySet(),
    emptyText: String? = null
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val screenW = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    // "Not interested" songs disappear at once from every row (speed dial included)
    fun OmHomeTile.visible() = track == null || track.videoUrl !in hidden

    fun tileClick(t: OmHomeTile) {
        when {
            t.track != null -> onPlayTrack(t.track)
            t.tracks.isNotEmpty() -> onPlayQueue(t.tracks)
            t.playlistUrl != null -> onOpenPlaylist(t.playlistUrl)
            t.channel != null -> onOpenChannel(t.channel)
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 130.dp)
    ) {
        sections.forEach { section ->
            item(key = section.key) {
                when (section) {
                    is OmHomeSection.SpeedDial -> {
                        val pages = section.tiles.filter { it.visible() }.chunked(9)
                        val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
                        if (pages.isNotEmpty()) androidx.compose.foundation.pager.HorizontalPager(state = pager, pageSpacing = 10.dp, modifier = Modifier.fillMaxWidth()) { page ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                pages[page].chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        row.forEach { t -> OmSpeedDialTile(t, isDark, Modifier.weight(1f), onLongClick = t.track?.let { tr -> { onActionTarget(tr) } }) { tileClick(t) } }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                        if (pages.size > 1) OmDots(pages.size, pager.currentPage, isDark)
                    }
                    is OmHomeSection.SongMixes -> {
                        Column {
                            OmSectionTitle(
                                "Song mixes", primary,
                                trailing = {
                                    // like the provider's "See all": the text and a thin gradient line under it
                                    Column(
                                        modifier = Modifier
                                            .width(androidx.compose.foundation.layout.IntrinsicSize.Max)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onPlayQueue(section.tracks.filter { it.videoUrl !in hidden }) }
                                            .padding(horizontal = 2.dp, vertical = 4.dp)
                                    ) {
                                        Text("Play all", color = primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(3.dp))
                                        Box(
                                            Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp))
                                                .background(com.example.ui.theme.AccentGradient)
                                        )
                                    }
                                }
                            )
                            val colState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyRow(
                                state = colState,
                                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(colState),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(section.tracks.filter { it.videoUrl !in hidden }.chunked(4), key = { "om_mix_${it.first().videoUrl}" }) { col ->
                                    Column(Modifier.width(screenW - 56.dp)) {
                                        col.forEach { t ->
                                            OmSongRow(t, isDark, primary, secondary, actionState.contains(t), onSwipe = { onActionTarget(t) }, onClick = { onPlayTrack(t) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is OmHomeSection.Squares -> {
                        Column {
                            OmSectionTitle(section.title, primary)
                            OmSquareRow(section.items.filter { it.visible() }, isDark, primary, secondary, { t -> t.track?.let(onActionTarget) }, ::tileClick)
                        }
                    }
                    is OmHomeSection.ArtistRow -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp)
                                    .clickable { onOpenChannel(section.channel) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OmThumb(section.channel.avatarUrl, Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape), isDark)
                                Spacer(Modifier.width(14.dp))
                                Text(section.channel.name, color = primary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("\u203A", color = primary, fontSize = 28.sp)
                            }
                            OmSquareRow(section.items.filter { it.visible() }, isDark, primary, secondary, { t -> t.track?.let(onActionTarget) }, ::tileClick)
                        }
                    }
                    is OmHomeSection.Community -> {
                        Column {
                            OmSectionTitle(section.title, primary)
                            OmSquareRow(section.items.filter { it.visible() }, isDark, primary, secondary, { t -> t.track?.let(onActionTarget) }, ::tileClick)
                        }
                    }
                    is OmHomeSection.Artists -> {
                        Column {
                            OmSectionTitle(section.title, primary)
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(section.artists, key = { "om_artist_${it.name}" }) { a ->
                                    Column(
                                        Modifier.width(96.dp).clickable { onOpenArtist(a.name) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        OmThumb(a.pictureUrl, Modifier.size(88.dp).clip(androidx.compose.foundation.shape.CircleShape), isDark)
                                        Text(a.name, color = primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                    is OmHomeSection.SongCards -> {
                        Column {
                            OmSectionTitle(section.title, primary, trailing = { Text("\u203A", color = primary, fontSize = 28.sp) })
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(section.cards.filter { c -> c.tracks.any { it.videoUrl !in hidden } }, key = { it.id }) { card ->
                                    Column(
                                        Modifier.width(screenW - 48.dp).clip(RoundedCornerShape(26.dp))
                                            .background(if (isDark) Color(0xFF241A18) else Color(0xFFF1F5F9)).padding(18.dp)
                                    ) {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            OmThumb(
                                                card.cover,
                                                Modifier.fillMaxWidth(0.62f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).clickable {
                                                    when {
                                                        card.channel != null -> onOpenChannel(card.channel)
                                                        card.playlistUrl != null -> onOpenPlaylist(card.playlistUrl)
                                                    }
                                                },
                                                isDark
                                            )
                                        }
                                        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(card.title, color = primary, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Text(card.subtitle, color = secondary, fontSize = 14.sp, maxLines = 1)
                                            }
                                            Box(
                                                Modifier.size(54.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x22000000))
                                                    .clickable { onPlayQueue(card.tracks) },
                                                contentAlignment = Alignment.Center
                                            ) { Text("\u25B6", color = primary, fontSize = 22.sp) }
                                        }
                                        card.tracks.filter { it.videoUrl !in hidden }.forEach { t ->
                                            OmSongRow(t, isDark, primary, secondary, actionState.contains(t), onSwipe = { onActionTarget(t) }, onClick = { onPlayTrack(t) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (emptyText != null && sections.isEmpty() && !loadingMore) {
            item(key = "om_empty") {
                Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                    Text(emptyText, color = secondary, fontSize = 14.sp)
                }
            }
        } else if (loadingMore || sections.isEmpty()) {
            item(key = "om_loading") {
                Box(Modifier.fillMaxWidth().padding(vertical = if (sections.isEmpty()) 120.dp else 24.dp), contentAlignment = Alignment.Center) {
                    ViauraExpressiveLoadingIndicator(size = if (sections.isEmpty()) 54.dp else 36.dp, isDark = isDark)
                }
            }
        }
    }
}

/** A row of 1:1 tiles (cover, title, subtitle) that slides left / right. */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun OmSquareRow(items: List<OmHomeTile>, isDark: Boolean, primary: Color, secondary: Color, onLongClick: (OmHomeTile) -> Unit, onClick: (OmHomeTile) -> Unit) {
    val w = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp * 0.42f
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { t ->
            Column(Modifier.width(w).combinedClickable(onClick = { onClick(t) }, onLongClick = if (t.track != null) ({ onLongClick(t) }) else null)) {
                OmCover(t.covers, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), isDark)
                Text(t.title, color = primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                if (t.subtitle.isNotBlank()) Text(t.subtitle, color = secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
