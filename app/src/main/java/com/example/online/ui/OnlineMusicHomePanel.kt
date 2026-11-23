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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.aspectRatio
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
                val batch = try {
                    // tastes that were not played for a few app starts fade out (once per app start)
                    OnlineInterestProfile.fadeForNewSession(context)
                    OnlineRecommendationMixer.fetchFeedBatch(
                        topArtists = AudioPlaybackHistoryManager.getTopArtists(context, limit = 5),
                        excludeUrls = emptySet(),
                        usedQueries = feedUsedQueries,
                        focus = OnlineInterestProfile.focus(context, OnlineInterestProfile.KIND_AUDIO, 3).map { it.name }
                    )
                } catch (t: kotlinx.coroutines.CancellationException) {
                    throw t
                } catch (_: Throwable) {
                    emptyList<OnlineTrackResult>()
                }
                withContext(Dispatchers.Main) {
                    if (token == feedToken[0]) {
                        // a failed refresh keeps the feed that is already there
                        if (batch.isNotEmpty() || results.isEmpty()) {
                            results = batch
                            st.generation++
                            st.savedFeed = null
                            st.listIndex = 0; st.listOffset = 0; st.gridIndex = 0; st.gridOffset = 0
                            st.scrollTopTick++
                        }
                        st.initialized = batch.isNotEmpty() || st.initialized
                        isSearching = false
                    }
                }
            } finally {
                // the pull-to-refresh animation must always end, whatever happened above
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    st.isRefreshing = false
                    if (token == feedToken[0]) isSearching = false
                }
            }
        }
    }

    fun loadMoreFeed() {
        if (isLoadingMore || isSearching) return
        val token = feedToken[0]
        val shownUrls = results.map { it.videoUrl }.toSet()
        isLoadingMore = true
        scope.launch(Dispatchers.IO) {
            val batch = try {
                OnlineRecommendationMixer.fetchFeedBatch(
                    topArtists = AudioPlaybackHistoryManager.getTopArtists(context, limit = 5),
                    excludeUrls = shownUrls,
                    usedQueries = feedUsedQueries,
                    focus = OnlineInterestProfile.focus(context, OnlineInterestProfile.KIND_AUDIO, 3).map { it.name }
                )
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList<OnlineTrackResult>()
            }
            withContext(Dispatchers.Main) {
                if (token == feedToken[0] && batch.isNotEmpty()) {
                    val already = results.map { it.videoUrl }.toSet()
                    results = results + batch.filter { it.videoUrl !in already }
                }
                isLoadingMore = false
            }
        }
    }

    // ---- Online music home (blank search box): speed dial, mixes, rows, cards - endless ----
    val home = OnlineMusicHomeState
    fun loadHome(reset: Boolean) {
        if (home.loading && !reset) return
        home.loading = true
        scope.launch {
            try {
                if (reset) { home.sections.clear(); home.usedKeys.clear(); home.batch = 0 }
                // the taste based feed is loaded first (loadFeed): its songs fill the speed dial and the song mixes
                val startGen = st.generation
                var waited = 0L
                // a refresh waits (max 9 s) for the NEW taste feed instead of reusing the old one
                while (waited < 9000L && (results.isEmpty() || (reset && st.generation == startGen))) { delay(300L); waited += 300L }
                val pool = results
                val first = withContext(Dispatchers.IO) { OmHomeBuilder.firstBatch(context, pool, home.usedKeys) }
                home.sections.addAll(first)
                home.batch = 1
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
            } finally {
                home.loading = false
            }
        }
    }
    fun loadMoreHome() {
        if (home.loading || home.sections.isEmpty()) return
        home.loading = true
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
            }
        }
    }
    LaunchedEffect(isFeedMode) {
        if (isFeedMode && home.sections.isEmpty()) loadHome(false)
    }
    // A song was played / a playlist opened: the whole home (speed dial, mixes, rows) is rebuilt around the new taste
    LaunchedEffect(isFeedMode, home.stale) {
        if (isFeedMode && home.stale && home.sections.isNotEmpty()) {
            delay(1500L)
            home.stale = false
            loadFeed()
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
            if (isFeedMode) { loadFeed(); loadHome(true) } else executeSearch(searchQuery)
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

    LaunchedEffect(shouldLoadMore) {
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
            // repeat count for the speed dial
            OnlineMusicHomeStats.bump(context, result)
            // the played song (and its artist) become the newest interest of this listener
            OnlineInterestProfile.record(context, OnlineInterestProfile.KIND_AUDIO, result.title, 3.0, result.artist)
            // Lyrics are fetched centrally by AudioPlaybackManager.loadLyricsFor()

            // Background queue construction: gather recommendations and pre-resolve streams
            val info = OnlineMusicService.resolveStreamWithRelated(result.videoUrl)
            val candidateResults = mutableListOf<OnlineTrackResult>()
            candidateResults.addAll(info.relatedTracks)

            // If related items from StreamInfo are fewer than 6, supplement from artist/title search or user favorites
            val topFavs = AudioPlaybackHistoryManager.getTopArtists(context, limit = 2)
            if (candidateResults.size < 6) {
                val fallbackArtist = result.artist.ifBlank { topFavs.firstOrNull() ?: result.title }
                val fallbackQuery = "$fallbackArtist song audio"
                val extra = OnlineMusicService.search(fallbackQuery)
                candidateResults.addAll(extra)
            }

            // Strict filter: ensure ONLY genuine audio/music items are recommended (NO gameplay, vlogs, movies, etc.)
            val filteredAudioCandidates = candidateResults.filter {
                OnlineMusicService.isAudioOrMusicContent(it.title, it.artist, it.durationSec)
            }

            // Rank candidate recommendations by user affinity
            val userTopArtists = AudioPlaybackHistoryManager.getTopArtists(context, limit = 5)
            val rankedCandidates = filteredAudioCandidates.sortedWith(Comparator { candA: OnlineTrackResult, candB: OnlineTrackResult ->
                val scoreA: Int = run {
                    val favIdx = userTopArtists.indexOfFirst { fav: String ->
                        candA.artist.contains(fav, ignoreCase = true) || candA.title.contains(fav, ignoreCase = true)
                    }
                    if (favIdx >= 0) 50 - favIdx * 10 else 0
                }
                val scoreB: Int = run {
                    val favIdx = userTopArtists.indexOfFirst { fav: String ->
                        candB.artist.contains(fav, ignoreCase = true) || candB.title.contains(fav, ignoreCase = true)
                    }
                    if (favIdx >= 0) 50 - favIdx * 10 else 0
                }
                scoreB.compareTo(scoreA)
            })

            // Deduplicate and filter out current track: this is the "similar" pool (same artist / related tracks)
            val similarPool = rankedCandidates
                .filter { it.videoUrl != result.videoUrl && it.title.isNotBlank() }
                .distinctBy { it.videoUrl }

            // Discovery pool: tracks from genres unlike the current song / the listener's usual artists, so
            // the queue is never one single category from start to finish.
            val discoveryPool = try {
                OnlineRecommendationMixer.fetchDiscoveryTracks(
                    avoidText = listOf(result.title, result.artist) + userTopArtists,
                    exclude = similarPool.map { it.videoUrl }.toSet() + result.videoUrl,
                    usedQueries = mutableSetOf()
                )
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList<OnlineTrackResult>()
            }

            // Things the listener may like next (other artists / related topics) and popular channels
            val taken = similarPool.map { it.videoUrl }.toSet() + discoveryPool.map { it.videoUrl }.toSet() + result.videoUrl
            val relatedPool = try {
                // popular hits of the language / genre he keeps playing (Punjabi hits ...), popular rap, popular Hindi songs
                val tasteNames = OnlineInterestProfile.focus(context, OnlineInterestProfile.KIND_AUDIO, 3).map { it.name }
                OnlineRecommendationMixer.fetchRelatedTracks(
                    bases = (OnlineInterestProfile.audioPopularQueries(tasteNames) +
                        userTopArtists.filter { !it.equals(result.artist, true) }
                            .ifEmpty { OnlineInterestProfile.relatedTopics(result.artist.ifBlank { result.title }) }).distinct(),
                    exclude = taken,
                    usedQueries = mutableSetOf(),
                    queryCount = 3
                )
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList<OnlineTrackResult>()
            }
            val channelPool = try {
                OnlineRecommendationMixer.fetchPopularChannelTracks(
                    exclude = taken + relatedPool.map { it.videoUrl },
                    usedQueries = mutableSetOf()
                )
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList<OnlineTrackResult>()
            }

            // per block of 10: 6 like the song / taste being played, 2 popular hits of his language + rap + Hindi,
            // 1 he may like next, 1 from a popular channel
            val mixedCandidates = OnlineRecommendationMixer.blockMix(
                sameTopic = similarPool,
                related = relatedPool,
                diverse = discoveryPool,
                channels = channelPool,
                total = 30,
                pattern = OnlineRecommendationMixer.BLOCK_PATTERN_AUDIO
            )

            // The next tracks are by OTHER artists of the same kind of music (never the same artist twice in a row);
            // after every 3 tracks the playing artist comes back with another song / a remix / a slowed version.
            val seedKey = OnlineTrackSequencer.creatorKey(result.artist)
            val seedExtras = if (seedKey.isBlank()) emptyList() else try {
                kotlinx.coroutines.coroutineScope {
                    listOf("remix", "slowed reverb", "songs").map { suffix ->
                        async { OnlineMusicService.search("${result.artist} $suffix") }
                    }.awaitAll().flatten()
                }.filter {
                    OnlineMusicService.isAudioOrMusicContent(it.title, it.artist, it.durationSec) &&
                        OnlineTrackSequencer.isSameCreator(seedKey, it)
                }.take(6)
            } catch (t: kotlinx.coroutines.CancellationException) {
                throw t
            } catch (_: Throwable) {
                emptyList<OnlineTrackResult>()
            }
            val uniqueCandidates = OnlineTrackSequencer.arrange(
                items = mixedCandidates,
                seedKey = seedKey,
                currentUrl = result.videoUrl,
                seedExtras = seedExtras
            ).take(30)

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
        scope.launch(Dispatchers.IO) { OnlineMusicHomeStats.bump(context, list.first()) }
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
                OmHomeFeed(
                    sections = home.sections,
                    loadingMore = home.loading,
                    listState = listState,
                    isDark = isDark,
                    actionState = actionState,
                    onActionTarget = { actionTarget = it },
                    onPlayTrack = { actionTarget = null; playOnlineTrack(it) },
                    onPlayQueue = { actionTarget = null; playQueueFrom(it) },
                    onOpenPlaylist = { openHomePlaylist(it) },
                    onOpenChannel = { onOpenChannel?.invoke(it) }
                )
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
                        picks.forEach { v -> OnlineMediaLibrary.markNotInterested(context, kind, v.videoUrl) }
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
}

/** Survives leaving and coming back to the screen, like OnlineAudioFeedState. */
internal object OnlineMusicHomeState {
    val sections = androidx.compose.runtime.mutableStateListOf<OmHomeSection>()
    var loading by mutableStateOf(false)
    /** Set when a song is played / a playlist is opened: the home is rebuilt around the new taste. */
    var stale by mutableStateOf(false)
    var batch = 0
    val usedKeys = mutableSetOf<String>()
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

private val OM_CATEGORIES = listOf(
    "Punjabi", "Hindi", "Phonk", "Lo-fi", "Hip hop", "Bollywood", "EDM", "Pop", "K-pop", "Latin", "Rock", "Workout",
    "Romantic", "Sad", "Party", "Devotional", "Haryanvi", "Tamil", "Telugu", "Retro", "Rap", "Jazz", "Chill", "Gaming"
)
private val OM_LIKED = listOf("Phonk", "English songs", "Hindi songs", "Old Hindi songs")
private val OM_MOODS = listOf("Energise", "Chill", "Workout", "Romance", "Party", "Focus", "Feel good", "Late night")

private object OmHomeBuilder {
    private suspend fun tracks(q: String, n: Int): List<OnlineTrackResult> = try {
        OnlineMusicService.search(q).filter { OnlineMusicService.isAudioOrMusicContent(it.title, it.artist, it.durationSec) }.take(n)
    } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (_: Throwable) { emptyList() }

    private fun playlistTile(p: com.example.util.OnlineHomePlaylist, label: String) = OmHomeTile(
        id = "pl_${p.url}", title = p.name,
        subtitle = if (p.owner.isNotBlank()) "$label \u2022 ${p.owner}" else label,
        covers = listOfNotNull(p.thumbnailUrl), playlistUrl = p.url
    )

    private fun trackTile(t: OnlineTrackResult) = OmHomeTile(
        id = "tr_${t.videoUrl}", title = t.title, subtitle = t.artist, covers = listOfNotNull(t.thumbnailUrl), track = t
    )

    private fun <T> List<T>.interleave(other: List<T>): List<T> {
        val out = ArrayList<T>()
        var i = 0
        while (i < size || i < other.size) {
            if (i < size) out.add(this[i])
            if (i < other.size) out.add(other[i])
            i++
        }
        return out
    }

    /** The 9-per-page speed dial: most repeated songs and opened playlists, filled up with songs of the listener's taste. */
    private fun speedDial(c: android.content.Context, pool: List<OnlineTrackResult>): OmHomeSection.SpeedDial {
        val songs = OnlineMusicHomeStats.topSongs(c, 40).map { trackTile(it) }
        val playlists = OnlineMusicHomeStats.playlists(c, 12)
        val mixed = ArrayList<OmHomeTile>()
        var si = 0
        var pi = 0
        while (si < songs.size || pi < playlists.size) {
            repeat(3) { if (si < songs.size) mixed.add(songs[si++]) }
            if (pi < playlists.size) mixed.add(playlists[pi++])
        }
        val have = mixed.mapNotNull { it.track?.videoUrl }.toMutableSet()
        for (t in pool) {
            if (mixed.size >= 36) break
            if (have.add(t.videoUrl)) mixed.add(trackTile(t))
        }
        return OmHomeSection.SpeedDial(mixed.take(45))
    }

    /** What the listener is into: artists of the songs played last, the taste profile, and the kinds of music he said he likes. */
    private fun tasteCategories(c: android.content.Context): List<String> {
        val recentArtists = OnlineMusicHomeStats.recent(c, 4).map { it.artist }.filter { it.isNotBlank() }
        val taste = try { OnlineInterestProfile.focus(c, OnlineInterestProfile.KIND_AUDIO, 4).map { it.name } } catch (_: Throwable) { emptyList() }
        return (recentArtists.take(2) + taste + OM_LIKED).distinct().take(7)
    }

    suspend fun firstBatch(c: android.content.Context, pool: List<OnlineTrackResult>, used: MutableSet<String>): List<OmHomeSection> =
        kotlinx.coroutines.coroutineScope {
            val taste = tasteCategories(c)
            val liked = OnlineMusicHomeStats.topSongs(c, 5)
            val artists = try { AudioPlaybackHistoryManager.getTopArtists(c, limit = 4) } catch (_: Throwable) { emptyList() }
            val cat1 = taste.first()
            val cat2 = taste.getOrNull(1) ?: OM_CATEGORIES.first { !it.equals(cat1, true) }

            val mixesJob = async {
                val per = liked.map { s -> async { tracks("${s.title} remix", 3) } } +
                    taste.map { k -> async { tracks("$k remix songs", 5) } } +
                    listOf(async { tracks("slowed reverb ${cat1}", 4) })
                per.awaitAll().filter { it.isNotEmpty() }.reduceOrNull { a, b -> a.interleave(b) }.orEmpty()
            }
            val remixJob = async { (tracks("popular $cat1 remix", 8) + tracks("top $cat2 songs", 8)) }
            val likeJob = async {
                val last = liked.firstOrNull()
                if (last == null) emptyList() else tracks("${last.artist} ${last.title} similar songs", 12) + tracks("most popular ${cat1} songs", 8)
            }
            val newJob = async { taste.take(3).map { k -> async { OnlineMusicService.searchHomePlaylists("$k new songs 2026", albums = true, limit = 5) } }.awaitAll().flatten() }
            val albumsJob = async { taste.take(3).map { k -> async { OnlineMusicService.searchHomePlaylists("$k hits", albums = false, limit = 6) } }.awaitAll().flatten() }
            val artistJobs = artists.take(2).map { a -> async { artistRow(a, used) } }
            val cardsJob = async { songCards(taste.firstOrNull() ?: cat1, OM_MOODS[0], artists) }
            val communityJob = async { community(taste + listOf(cat2), "From the community") }

            val out = ArrayList<OmHomeSection>()
            out.add(speedDial(c, pool))
            val mixes = (mixesJob.await() + pool.take(12)).distinctBy { it.videoUrl }.take(32)
            if (mixes.isNotEmpty()) out.add(OmHomeSection.SongMixes(mixes))
            val like = likeJob.await().distinctBy { it.videoUrl }
            if (like.isNotEmpty()) out.add(OmHomeSection.Squares("om_more_like", liked.firstOrNull()?.let { "More like ${it.title.take(24)}" } ?: "Popular for you", like.map { trackTile(it) }))
            val remix = remixJob.await().distinctBy { it.videoUrl }
            if (remix.isNotEmpty()) out.add(OmHomeSection.Squares("om_remix_popular", "Remix & popular", remix.map { trackTile(it) }))
            val news = newJob.await().distinctBy { it.url }
            if (news.isNotEmpty()) out.add(OmHomeSection.Squares("om_new_releases", "New releases", news.map { playlistTile(it, if (it.songCount in 1..7) "EP" else "Album") }))
            val albums = albumsJob.await().distinctBy { it.url }
            if (albums.isNotEmpty()) out.add(OmHomeSection.Squares("om_albums_for_you", "Albums for you", albums.map { playlistTile(it, "Playlist") }))
            artistJobs.awaitAll().forEach { r -> if (r != null) out.add(r) }
            communityJob.await()?.let { out.add(it) }
            cardsJob.await()?.let { out.add(it) }
            out.forEach { used.add(it.key) }
            out
        }

    /** One artist: avatar + name (opens the channel), then his albums / EPs and popular songs as 1:1 tiles. */
    private suspend fun artistRow(name: String, used: Set<String>): OmHomeSection.ArtistRow? = kotlinx.coroutines.coroutineScope {
        val key = "om_artist_${name.lowercase()}"
        if (key in used) return@coroutineScope null
        val channel = try { OnlineMusicService.searchMusicPage(name).second.channelResults.firstOrNull() } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (_: Throwable) { null }
            ?: return@coroutineScope null
        val albums = async { OnlineMusicService.searchHomePlaylists(name, albums = true, limit = 6) }
        val songs = async { tracks("$name songs", 6) }
        val items = albums.await().map { playlistTile(it, if (it.songCount in 1..7) "EP" else "Album") } + songs.await().map { trackTile(it) }
        if (items.isEmpty()) null else OmHomeSection.ArtistRow(key, channel, items)
    }

    /** "From the community": 2x2 cover collages that play a ready made mix of one category. */
    private suspend fun community(categories: List<String>, title: String): OmHomeSection.Community? = kotlinx.coroutines.coroutineScope {
        val tiles = categories.distinct().take(4).map { k ->
            async {
                val list = tracks("$k songs mix", 12).distinctBy { it.videoUrl }
                if (list.size < 4) null else OmHomeTile(
                    id = "mix_${k}_${list.first().videoUrl}", title = "$k mix",
                    subtitle = list.take(2).joinToString(" \u2022 ") { it.artist }.take(40),
                    covers = list.take(4).mapNotNull { it.thumbnailUrl }, tracks = list
                )
            }
        }.awaitAll().filterNotNull()
        if (tiles.isEmpty()) null else OmHomeSection.Community("om_community_${categories.firstOrNull()}", title, tiles)
    }

    /** Cards like "Energise": a playlist (or one artist) on top, then its first songs. */
    private suspend fun songCards(category: String, mood: String, artists: List<String>): OmHomeSection.SongCards? = kotlinx.coroutines.coroutineScope {
        val pls = OnlineMusicService.searchHomePlaylists("$mood $category songs", albums = false, limit = 3)
        val plCards = pls.map { p ->
            async {
                val t = try { OnlineMusicService.fetchPlaylistTracks(p.url).take(3) } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (_: Throwable) { emptyList() }
                if (t.isEmpty()) null else OmHomeSongCard(
                    id = "pc_${p.url}", title = p.name,
                    subtitle = "YouTube Music" + if (p.songCount > 0) " \u2022 ${p.songCount} songs" else "",
                    cover = p.thumbnailUrl ?: t.first().thumbnailUrl, tracks = t, playlistUrl = p.url, channel = null
                )
            }
        }
        val artistCards = artists.take(2).map { a ->
            async {
                val ch = try { OnlineMusicService.searchMusicPage(a).second.channelResults.firstOrNull() } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (_: Throwable) { null }
                val t = tracks("$a songs", 3)
                if (ch == null || t.isEmpty()) null else OmHomeSongCard(
                    id = "ac_${ch.channelUrl}", title = ch.name, subtitle = "Popular songs",
                    cover = ch.avatarUrl ?: t.first().thumbnailUrl, tracks = t, playlistUrl = null, channel = ch
                )
            }
        }
        val cards = (plCards + artistCards).awaitAll().filterNotNull()
        if (cards.isEmpty()) null else OmHomeSection.SongCards("om_cards_${mood}_$category", mood, cards)
    }

    /** More of the same kinds of sections, always with categories that were not shown yet (endless scroll). */
    suspend fun moreBatch(c: android.content.Context, batch: Int, used: MutableSet<String>): List<OmHomeSection> =
        kotlinx.coroutines.coroutineScope {
            val unused = OM_CATEGORIES.filter { cat -> used.none { it.contains("_${cat}_", true) || it.endsWith("_$cat", true) } }
            // the first pages stay inside the listener's own kinds of music (the newest taste first); later pages add new categories
            val tasteList = tasteCategories(c)
            val pickPool = if (batch <= 6 && tasteList.isNotEmpty()) {
                List(4) { i -> tasteList[(batch * 3 + i) % tasteList.size] }
            } else (unused.ifEmpty { OM_CATEGORIES }).shuffled()
            val flavour = listOf("most popular songs", "top hits", "best songs", "popular playlist")[batch % 4]
            val a = pickPool[0]
            val b = pickPool.getOrElse(1) { pickPool[0] }
            val d = pickPool.getOrElse(2) { pickPool[0] }
            val e = pickPool.getOrElse(3) { pickPool[0] }
            val mood = OM_MOODS[batch % OM_MOODS.size]
            val squaresJob = async { OnlineMusicService.searchHomePlaylists("$a $flavour", albums = false, limit = 8) }
            val albumsJob = async { OnlineMusicService.searchHomePlaylists("$b ${if (batch % 2 == 0) "new album" else "best album"}", albums = true, limit = 8) }
            val cardsJob = async { songCards(d, mood, emptyList()) }
            val communityJob = async { community(listOf(a, b, d, e), "$mood picks") }
            val artistJob = async {
                val found = try { OnlineMusicService.searchMusicPage("top $e artists").second.channelResults } catch (c2: kotlinx.coroutines.CancellationException) { throw c2 } catch (_: Throwable) { emptyList() }
                val ch = found.firstOrNull { "om_artist_${it.name.lowercase()}" !in used }
                if (ch == null) null else artistRow(ch.name, used)
            }
            val remixJob = async { tracks("$e remix popular", 10) }
            val out = ArrayList<OmHomeSection>()
            squaresJob.await().distinctBy { it.url }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_hits_${a}_$batch", "$a hits", it.map { p -> playlistTile(p, "Playlist") })) }
            cardsJob.await()?.let { out.add(it) }
            remixJob.await().takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_remix_${e}_$batch", "$e remix & popular", it.map { t -> trackTile(t) })) }
            artistJob.await()?.let { out.add(it) }
            albumsJob.await().distinctBy { it.url }.takeIf { it.isNotEmpty() }?.let { out.add(OmHomeSection.Squares("om_albums_${b}_$batch", "$b albums", it.map { p -> playlistTile(p, if (p.songCount in 1..7) "EP" else "Album") })) }
            communityJob.await()?.let { out.add(it) }
            out.forEach { used.add(it.key) }
            out
        }
}

// ----------------------------------------------------------------------------------------------------------- UI

@Composable
private fun OmThumb(url: String?, modifier: Modifier, isDark: Boolean) {
    var bmp by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) bmp = loadRemoteAudioThumbnail(url, targetSizePx = 400)
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

@Composable
private fun OmSpeedDialTile(tile: OmHomeTile, isDark: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val isPlaylist = tile.playlistUrl != null
    Box(modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).clickable { onClick() }) {
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
    onOpenChannel: (OnlineChannelResult) -> Unit
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val screenW = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp

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
                        val pages = section.tiles.chunked(9)
                        val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })
                        androidx.compose.foundation.pager.HorizontalPager(state = pager, pageSpacing = 10.dp, modifier = Modifier.fillMaxWidth()) { page ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                pages[page].chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        row.forEach { t -> OmSpeedDialTile(t, isDark, Modifier.weight(1f)) { tileClick(t) } }
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
                                    Text(
                                        "Play all", color = primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, secondary, RoundedCornerShape(50))
                                            .clickable { onPlayQueue(section.tracks) }.padding(horizontal = 18.dp, vertical = 9.dp)
                                    )
                                }
                            )
                            val colState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyRow(
                                state = colState,
                                flingBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(colState),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(section.tracks.chunked(4), key = { "om_mix_${it.first().videoUrl}" }) { col ->
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
                            OmSquareRow(section.items, isDark, primary, secondary, ::tileClick)
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
                            OmSquareRow(section.items, isDark, primary, secondary, ::tileClick)
                        }
                    }
                    is OmHomeSection.Community -> {
                        Column {
                            OmSectionTitle(section.title, primary)
                            OmSquareRow(section.items, isDark, primary, secondary, ::tileClick)
                        }
                    }
                    is OmHomeSection.SongCards -> {
                        Column {
                            OmSectionTitle(section.title, primary, trailing = { Text("\u203A", color = primary, fontSize = 28.sp) })
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(section.cards, key = { it.id }) { card ->
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
                                        card.tracks.forEach { t ->
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
        if (loadingMore || sections.isEmpty()) {
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
private fun OmSquareRow(items: List<OmHomeTile>, isDark: Boolean, primary: Color, secondary: Color, onClick: (OmHomeTile) -> Unit) {
    val w = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp * 0.42f
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { t ->
            Column(Modifier.width(w).clickable { onClick(t) }) {
                OmCover(t.covers, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)), isDark)
                Text(t.title, color = primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                if (t.subtitle.isNotBlank()) Text(t.subtitle, color = secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
