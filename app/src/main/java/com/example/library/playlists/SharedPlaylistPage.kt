package com.example.ui.screens

import com.example.ui.components.liquidSearchSurface
import com.example.ui.components.playedProgressFill
import com.example.ui.theme.Neutral

import android.content.Context
import com.example.ui.components.LocalPanelTransparency
import com.example.ui.components.glassPanelColor
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.components.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.player.AudioPlaybackManager
import com.example.ui.components.*
import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.state.AppViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.*
import com.example.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPlaylistScreen(
    playlist: OnlinePlaylistDetail,
    uiState: UiState,
    viewModel: AppViewModel? = null,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showSortAndViewDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    // Swipe (right -> left) quick-action toolbar target; nothing is selected or highlighted.
    var swipeActionTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    var downloadRequest by remember { mutableStateOf<com.example.ui.components.DownloadRequest?>(null) }
    com.example.ui.components.DownloadRequestHost(downloadRequest, isDark, uiState.glassBlurTransparency) { downloadRequest = null }
    var lastSwipeActionTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    if (swipeActionTrack != null) lastSwipeActionTrack = swipeActionTrack
    var playlistSaveTrack by remember { mutableStateOf<AudioTrackItem?>(null) }
    var swipeHiddenVersion by remember { mutableStateOf(0) }
    val swipeHiddenUrls = remember(swipeHiddenVersion) {
        com.example.util.OnlineMediaLibrary.notInterestedSet(context, com.example.util.OnlineMediaLibrary.KIND_AUDIO)
    }
    var swipeLaterVersion by remember { mutableStateOf(0) }
    LaunchedEffect(listState.isScrollInProgress, gridState.isScrollInProgress) {
        if (listState.isScrollInProgress || gridState.isScrollInProgress) swipeActionTrack = null
    }

    // Search and Sort State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            // The search field is attached a moment after the flag flips (and the screen may already be leaving on a fast
            // Back): requesting focus before the Modifier.focusRequester node exists crashed the app. Wait, retry, never throw.
            var tries = 0
            while (tries < 5) {
                kotlinx.coroutines.delay(if (tries == 0) 60L else 80L)
                val ok = try { searchFocusRequester.requestFocus(); true } catch (_: IllegalStateException) { false } catch (_: Throwable) { true }
                if (ok) break
                tries++
            }
        }
    }

    var currentSortField by remember { mutableStateOf(SortField.TITLE) }
    var currentSortDirection by remember { mutableStateOf(SortDirection.ASCENDING) }
    var currentLayoutMode by remember { mutableStateOf(LayoutMode.LIST) }
    var currentVisibleFields by remember { mutableStateOf(com.example.ui.state.SettingsPreferencesManager.sanitizeOnlineAudioFields(VisibleFields(showThumbnails = true, showDuration = true, showDate = true, showProgressBar = true))) }

    val currentPlayingTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isPlaying by AudioPlaybackManager.isPlaying.collectAsState()
    val currentPositionMs by AudioPlaybackManager.currentPositionMs.collectAsState()

    // 4 random high-quality track thumbnails selected for the collage
    val randomFourThumbnails = remember(playlist.playlistUrl, playlist.tracks) {
        if (playlist.previewThumbnails.size >= 4) {
            playlist.previewThumbnails.take(4)
        } else {
            val fromTracks = playlist.tracks
                .filter { !it.thumbnailUrl.isNullOrBlank() }
                .shuffled()
                .map { OnlineMusicService.getHighQualityThumbnailUrl(it.thumbnailUrl, it.videoUrl) }
                .distinct()
            if (fromTracks.size >= 4) {
                fromTracks.take(4)
            } else if (fromTracks.isNotEmpty()) {
                val padded = fromTracks.toMutableList()
                while (padded.size < 4) {
                    padded.add(fromTracks[padded.size % fromTracks.size])
                }
                padded
            } else if (!playlist.coverUrl.isNullOrBlank()) {
                List(4) { playlist.coverUrl }
            } else {
                emptyList()
            }
        }
    }

    // Convert all online tracks to AudioTrackItems for playback and UI display
    val allTrackItems = remember(playlist.tracks) {
        playlist.tracks.map { it.toAudioTrackItem() }
    }

    // Filtered and Sorted tracks according to search and sort/view options
    val filteredAndSortedTracks = remember(allTrackItems, searchQuery, currentSortField, currentSortDirection, swipeHiddenUrls) {
        val visibleTracks = allTrackItems.filter { t ->
            t.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) !in swipeHiddenUrls
        }
        val filtered = if (searchQuery.isBlank()) {
            visibleTracks
        } else {
            visibleTracks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
        when (currentSortField) {
            SortField.TITLE -> if (currentSortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.title.lowercase() }
            } else {
                filtered.sortedByDescending { it.title.lowercase() }
            }
            SortField.DURATION -> if (currentSortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.durationMs }
            } else {
                filtered.sortedByDescending { it.durationMs }
            }
            SortField.SIZE -> if (currentSortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.sizeBytes }
            } else {
                filtered.sortedByDescending { it.sizeBytes }
            }
            SortField.DATE -> if (currentSortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.dateModified }
            } else {
                filtered.sortedByDescending { it.dateModified }
            }
            else -> filtered
        }
    }

    // Play all starting from track 0 or from a random track
    fun playPlaylist(startIndex: Int = 0, isShuffle: Boolean = false) {
        if (filteredAndSortedTracks.isEmpty()) return
        val queue = if (isShuffle) {
            val startTrack = filteredAndSortedTracks.getOrElse(startIndex) { filteredAndSortedTracks.random() }
            val rest = filteredAndSortedTracks.filter { it.id != startTrack.id }.shuffled()
            listOf(startTrack) + rest
        } else {
            val head = filteredAndSortedTracks.getOrElse(startIndex) { filteredAndSortedTracks.first() }
            val reordered = filteredAndSortedTracks.drop(startIndex) + filteredAndSortedTracks.take(startIndex)
            reordered
        }
        AudioPlaybackManager.playTrack(context, queue.first(), queue)
        AudioPlaybackManager.openFullScreen()
    }

    val isFullScreenAudioOpen by AudioPlaybackManager.isFullScreenOpen.collectAsState()

    BackHandler {
        if (swipeActionTrack != null) {
            swipeActionTrack = null
        } else if (isFullScreenAudioOpen) {
            AudioPlaybackManager.collapseToMiniPlayer()
        } else if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            onBack()
        }
    }

    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    // Same page colour as the main screen: ice (Light), white (System), dark / AMOLED
    val bgColor = if (isDark) {
        if (uiState.amoledBlackMode) MaterialTheme.colorScheme.background else Neutral.D1B2630
    } else if (uiState.themeMode == com.example.ui.state.ThemeMode.SYSTEM) {
        Neutral.FFFFFF
    } else {
        Neutral.DAE3E9
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Settings > Appearance > Background: same animation as Home plays behind this page too
        com.example.background.PageBackdrop(uiState.themeMode)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top App Bar: Exact same UI as offline player (Back Button, "Music" title, Search, Sort & View, Settings)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                HeaderBar(
                    title = "Music",
                    subtitle = "${allTrackItems.size} Tracks",
                    onBackClick = onBack,
                    themeMode = uiState.themeMode,
                    onSearchClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    },
                    onSortClick = { showSortAndViewDialog = true },
                    onSettingsClick = { showSettingsSheet = true }
                )
            }

            // Expandable Search Bar (when user clicks Search icon in top bar)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(
                    animationSpec = tween(180, easing = FastOutLinearInEasing),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .liquidSearchSurface(RoundedCornerShape(18.dp), isDark, surfaceColor, borderColor)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search tracks or artists...",
                                    fontSize = 13.sp,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = com.example.ui.theme.LocalAppFontFamily.current,
                                    color = primaryText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(AccentSkyBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(primaryText.copy(alpha = 0.08f))
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 11.sp,
                                    color = secondaryText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Content: Dynamically switches between List and Grid layout when Sort & View is changed!
            val contentBottomPadding = if (currentPlayingTrack != null) 100.dp else 80.dp

            if (currentLayoutMode == LayoutMode.GRID) {
                // GRID LAYOUT
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = contentBottomPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Item in Grid
                    item(span = { GridItemSpan(maxLineSpan) }, key = "playlist_header_grid") {
                        PlaylistHeaderSection(
                            playlist = playlist,
                            randomFourThumbnails = randomFourThumbnails,
                            allTrackItems = allTrackItems,
                            surfaceColor = surfaceColor,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            isDark = isDark,
                            onPlay = { playPlaylist(startIndex = 0, isShuffle = false) },
                            onShuffle = {
                                val randomIdx = if (allTrackItems.isNotEmpty()) allTrackItems.indices.random() else 0
                                playPlaylist(startIndex = randomIdx, isShuffle = true)
                            },
                            onSaveClick = { showSaveDialog = true }
                        )
                    }

                    // Section title: ♫ AUDIOS (count) exactly like offline player
                    item(span = { GridItemSpan(maxLineSpan) }, key = "audios_section_header_grid") {
                        AudiosSectionHeader(
                            count = filteredAndSortedTracks.size,
                            secondaryText = secondaryText
                        )
                    }

                    // Track Grid Cards (NO M4A, 0.0 MB, Stereo tags - only Title, Artist, 3-dot menu!)
                    itemsIndexed(
                        items = filteredAndSortedTracks,
                        key = { index, track -> "grid_${track.id}_${track.path}_$index" }
                    ) { index, trackItem ->
                        val isCurrent = currentPlayingTrack?.path == trackItem.path
                        val progress = if (isCurrent && trackItem.durationMs > 0) {
                            (currentPositionMs.toFloat() / trackItem.durationMs).coerceIn(0f, 1f)
                        } else 0f
                        val displayItem = if (progress != trackItem.playbackProgress) {
                            trackItem.copy(playbackProgress = progress)
                        } else trackItem

                        SharedTrackGridCard(
                            track = displayItem,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            isDark = isDark,
                            onClick = { swipeActionTrack = null; playPlaylist(startIndex = index, isShuffle = false) },
                            modifier = Modifier.swipeLeftForActions { swipeActionTrack = trackItem }
                        )
                    }
                }
            } else {
                // LIST LAYOUT
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 6.dp, bottom = contentBottomPadding)
                ) {
                    // Header: 2x2 Thumbnail Grid below top bar, Playlist Name, Artist Name, and Play/Shuffle buttons
                    item(key = "playlist_header_list") {
                        PlaylistHeaderSection(
                            playlist = playlist,
                            randomFourThumbnails = randomFourThumbnails,
                            allTrackItems = allTrackItems,
                            surfaceColor = surfaceColor,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            isDark = isDark,
                            onPlay = { playPlaylist(startIndex = 0, isShuffle = false) },
                            onShuffle = {
                                val randomIdx = if (allTrackItems.isNotEmpty()) allTrackItems.indices.random() else 0
                                playPlaylist(startIndex = randomIdx, isShuffle = true)
                            },
                            onSaveClick = { showSaveDialog = true }
                        )
                    }

                    // Section title: ♫ AUDIOS (count) exactly like offline player
                    item(key = "audios_section_header_list") {
                        AudiosSectionHeader(
                            count = filteredAndSortedTracks.size,
                            secondaryText = secondaryText
                        )
                    }

                    // Track List Cards (NO M4A, 0.0 MB, Stereo tags - only Title, Artist, 3-dot menu!)
                    itemsIndexed(
                        items = filteredAndSortedTracks,
                        key = { index, track -> "list_${track.id}_${track.path}_$index" }
                    ) { index, trackItem ->
                        val isCurrent = currentPlayingTrack?.path == trackItem.path
                        val progress = if (isCurrent && trackItem.durationMs > 0) {
                            (currentPositionMs.toFloat() / trackItem.durationMs).coerceIn(0f, 1f)
                        } else 0f
                        val displayItem = if (progress != trackItem.playbackProgress) {
                            trackItem.copy(playbackProgress = progress)
                        } else trackItem

                        SharedTrackListCard(
                            track = displayItem,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            isDark = isDark,
                            onClick = { swipeActionTrack = null; playPlaylist(startIndex = index, isShuffle = false) },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .swipeLeftForActions { swipeActionTrack = trackItem }
                        )
                    }
                }
            }
        }

        // 4. Floating Action Button (FAB): Bottom right corner, purple play button matching the offline player
        val fabShape = RoundedCornerShape(20.dp)
        val fabBottomPadding = if (currentPlayingTrack != null) 92.dp else 24.dp
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = fabBottomPadding)
                .size(60.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = fabShape,
                    ambientColor = AccentSkyBlue.copy(alpha = 0.50f),
                    spotColor = AccentPink.copy(alpha = 0.60f)
                )
                .clip(fabShape)
                .background(
                    if (isDark) {
                        Brush.linearGradient(listOf(Neutral.D1E293B.copy(alpha = 0.95f), Neutral.D0F172A.copy(alpha = 0.95f)))
                    } else {
                        Brush.linearGradient(listOf(com.example.ui.theme.LightGlassSurface, com.example.ui.theme.LightGlassSurface))
                    }
                )
                .then(if (isDark) Modifier.border(1.5.dp, com.example.ui.theme.AccentGradient, fabShape) else Modifier.border(1.5.dp, com.example.ui.theme.LightGlassBorder, fabShape))
                .clickable {
                    // Automatic playback of either the top song or a random song from the playlist
                    playPlaylist(startIndex = 0, isShuffle = false)
                }
                .testTag("shared_playlist_fab"),
            contentAlignment = Alignment.Center
        ) {
            PlayGradientLogoIcon(size = 32.dp)
        }

        // Swipe (right -> left) quick-action toolbar
        AnimatedVisibility(
            visible = swipeActionTrack != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = if (currentPlayingTrack != null) 92.dp else 18.dp)
        ) {
            val t = swipeActionTrack ?: lastSwipeActionTrack
            if (t != null) {
                val kind = com.example.util.OnlineMediaLibrary.KIND_AUDIO
                val trackUrl = t.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                val asResult = com.example.util.OnlineTrackResult(
                    videoUrl = trackUrl,
                    title = t.title,
                    artist = t.artist,
                    durationSec = t.durationMs / 1000L,
                    thumbnailUrl = t.thumbnailUrl,
                    uploadDateEpochMs = t.dateModified.takeIf { it > 0L }
                )
                val savedLater = remember(swipeLaterVersion, trackUrl) {
                    com.example.util.OnlineMediaLibrary.isWatchLater(context, kind, trackUrl)
                }
                OnlineQuickActionBar(
                    isDark = isDark,
                    alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    isSavedForLater = savedLater,
                    onDownload = { downloadRequest = com.example.ui.components.DownloadRequest(trackUrl, t.title, t.artist, t.thumbnailUrl, true); swipeActionTrack = null },
                    onPlayNext = {
                        AudioPlaybackManager.playNext(t)
                        Toast.makeText(context, "Will play next: ${t.title}", Toast.LENGTH_SHORT).show()
                        swipeActionTrack = null
                    },
                    onPlayLast = {
                        AudioPlaybackManager.playLast(t)
                        Toast.makeText(context, "Added to queue: ${t.title}", Toast.LENGTH_SHORT).show()
                        swipeActionTrack = null
                    },
                    onSaveToPlaylist = {
                        com.example.util.OnlineMediaLibrary.rememberMeta(context, kind, asResult)
                        playlistSaveTrack = t
                        swipeActionTrack = null
                    },
                    onWatchLater = {
                        val saved = com.example.util.OnlineMediaLibrary.toggleWatchLater(context, kind, asResult)
                        swipeLaterVersion++
                        Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                        swipeActionTrack = null
                    },
                    onNotInterested = {
                        com.example.util.OnlineMediaLibrary.markNotInterested(context, kind, trackUrl)
                        swipeHiddenVersion++
                        Toast.makeText(context, "Okay, you will see fewer songs like this", Toast.LENGTH_SHORT).show()
                        swipeActionTrack = null
                    },
                    onShare = {
                        shareOnlineLink(context, t.title, trackUrl)
                        swipeActionTrack = null
                    },
                )
            }
        }

        // Minimized Audio Player Pill (if audio is currently playing)
        MinimizedAudioPlayer(
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onExpand = { AudioPlaybackManager.openFullScreen() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        com.example.ui.components.MinimizedVideoPlayer(
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onExpand = {
                com.example.player.VideoMiniPlayerManager.session.value?.let { s ->
                    viewModel?.startPlaying(s.video, s.playlist)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        )

        // Full Screen Audio Player Sheet with dynamic slide-up & collapse animations
        AnimatedVisibility(
            visible = isFullScreenAudioOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 380,
                    easing = CubicBezierEasing(0.12f, 0.95f, 0.22f, 1.0f)
                )
            ) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(
                    durationMillis = 340,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 260,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 360,
                    easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
                )
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(
                    durationMillis = 320,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = 60,
                    easing = LinearEasing
                )
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            AudioPlayerScreen(
                isDark = isDark,
                glassBlurTransparency = uiState.glassBlurTransparency,
                onCollapse = { AudioPlaybackManager.collapseToMiniPlayer() }
            )
        }
    }

    // Sort & View Options Popup Dialog (fully functional List/Grid layout toggle and Sort fields)
    if (showSortAndViewDialog) {
        SortAndViewOptionsPopup(
            isDark = isDark,
            sortField = currentSortField,
            sortDirection = currentSortDirection,
            viewMode = ViewMode.LIBRARY,
            layoutMode = currentLayoutMode,
            visibleFields = currentVisibleFields,
            isInsideFolder = true,
            isMusicMode = true,
            isOnlineAudioFolder = true,
            appScale = uiState.appScale,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onSortFieldChange = { currentSortField = it },
            onSortDirectionChange = { currentSortDirection = it },
            onViewModeChange = { },
            onLayoutModeChange = { currentLayoutMode = it },
            onVisibleFieldsChange = { currentVisibleFields = com.example.ui.state.SettingsPreferencesManager.sanitizeOnlineAudioFields(it) },
            onDismiss = { showSortAndViewDialog = false }
        )
    }

    // Settings Sheet Modal
    if (showSettingsSheet) {
        SettingsSheet(
            uiState = uiState,
            onDismiss = { showSettingsSheet = false },
            onThemeChange = { viewModel?.setThemeMode(it) },
            onScaleChange = { viewModel?.setAppScale(it) },
            onGlassBlurTransparencyChange = { viewModel?.setGlassBlurTransparency(it) },
            onLanguageChange = { viewModel?.setLanguage(it) }
        )
    }

    // Save a single swiped track into one of the user's audio playlists
    playlistSaveTrack?.let { t ->
        AddToPlaylistDialog(
            selectedAudioTracks = listOf(t),
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onDismiss = { playlistSaveTrack = null },
            onPlaylistUpdated = { playlistSaveTrack = null }
        )
    }

    // Save the whole shared playlist: same "Add to Playlist" sheet (with Create New Playlist) used everywhere else
    if (showSaveDialog) {
        AddToPlaylistDialog(
            selectedAudioTracks = allTrackItems,
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            suggestedNewName = playlist.title,
            onDismiss = { showSaveDialog = false },
            onPlaylistUpdated = { showSaveDialog = false }
        )
    }
}

/**
 * Section Header: ♫ AUDIOS (count) matching the offline player UI
 */
@Composable
private fun AudiosSectionHeader(
    count: Int,
    secondaryText: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.viaura_audio_track),
            contentDescription = null,
            tint = AccentSkyBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "AUDIOS ($count)",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = secondaryText
        )
    }
}
