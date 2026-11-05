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
    var actionTarget by remember { mutableStateOf<OnlineTrackResult?>(null) }
    var lastActionTarget by remember { mutableStateOf<OnlineTrackResult?>(null) }
    var downloadRequest by remember { mutableStateOf<com.example.ui.components.DownloadRequest?>(null) }
    com.example.ui.components.DownloadRequestHost(downloadRequest, isDark, glassBlurTransparency) { downloadRequest = null }
    if (actionTarget != null) lastActionTarget = actionTarget
    var playlistTarget by remember { mutableStateOf<OnlineTrackResult?>(null) }
    var watchLaterTracks by remember { mutableStateOf<List<OnlineTrackResult>>(emptyList()) }
    var hiddenVersion by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        OnlineMediaLibrary.attach(context)
        watchLaterTracks = withContext(Dispatchers.IO) { OnlineMediaLibrary.watchLater(context, OnlineMediaLibrary.KIND_AUDIO) }
    }
    val hiddenUrls = remember(hiddenVersion) { OnlineMediaLibrary.notInterestedSet(context, OnlineMediaLibrary.KIND_AUDIO) }
    BackHandler(enabled = actionTarget != null) { actionTarget = null }
    LaunchedEffect(listState.isScrollInProgress, gridState.isScrollInProgress) {
        if (listState.isScrollInProgress || gridState.isScrollInProgress) actionTarget = null
    }

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
            if (isFeedMode) loadFeed() else executeSearch(searchQuery)
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
            if (isFeedMode) loadMoreFeed() else if (hasNextPage) loadMore()
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

                            Box(Modifier.swipeLeftForActions { actionTarget = result }) {
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

                            Box(Modifier.swipeLeftForActions { actionTarget = result }) {
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
                OnlineQuickActionBar(
                    isDark = isDark,
                    alphaRatio = (glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    isSavedForLater = watchLaterTracks.any { it.videoUrl == t.videoUrl },
                    onDownload = { downloadRequest = com.example.ui.components.DownloadRequest(t.videoUrl, t.title, t.artist, t.thumbnailUrl, true); actionTarget = null },
                    onPlayNext = {
                        AudioPlaybackManager.playNext(t.toAudioTrackItem())
                        Toast.makeText(context, "Will play next: ${t.title}", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onPlayLast = {
                        AudioPlaybackManager.playLast(t.toAudioTrackItem())
                        Toast.makeText(context, "Added to queue: ${t.title}", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onSaveToPlaylist = {
                        OnlineMediaLibrary.rememberMeta(context, kind, t)
                        playlistTarget = t
                        actionTarget = null
                    },
                    onWatchLater = {
                        val saved = OnlineMediaLibrary.toggleWatchLater(context, kind, t)
                        watchLaterTracks = OnlineMediaLibrary.watchLater(context, kind)
                        Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onNotInterested = {
                        OnlineMediaLibrary.markNotInterested(context, kind, t.videoUrl)
                        watchLaterTracks = OnlineMediaLibrary.watchLater(context, kind)
                        hiddenVersion++
                        Toast.makeText(context, "Okay, you will see fewer songs like this", Toast.LENGTH_SHORT).show()
                        actionTarget = null
                    },
                    onShare = {
                        shareOnlineLink(context, t.title, t.videoUrl)
                        actionTarget = null
                    },
                )
            }
        }

        playlistTarget?.let { t ->
            AddToPlaylistDialog(
                selectedAudioTracks = listOf(t.toAudioTrackItem()),
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { playlistTarget = null },
                onPlaylistUpdated = { playlistTarget = null }
            )
        }
    }
}
}
