package com.example.ui.screens

import com.example.ui.components.liquidSearchSurface
import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlueStrong
import androidx.compose.foundation.Image
import com.example.ui.components.liquidGlassOr
import com.example.ui.components.cardSurface
import com.example.background.AnimatedBackground
import com.example.background.BackgroundType
import com.example.background.rememberActiveBackground
import com.example.ui.components.liquidGlassToolbarOr
import com.example.ui.components.LocalPanelTransparency
import com.example.ui.components.glassPanelColor
import com.example.ui.components.glassCardBorder
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.StyledIcon
import com.example.ui.components.FolderGlyphType
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Lock
import com.example.util.NaturalOrderComparator
import com.example.util.VideoLibraryCache
import com.example.util.MediaMetadataExtractor
import com.example.util.FolderBlockListManager
import com.example.util.PrivateVaultManager
import com.example.util.PrivateFolder
import com.example.util.PrivateMediaType

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Cloud
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.BottomMoveCopyProgressBar
import com.example.ui.components.DirectoryPickerAction
import com.example.ui.components.DynamicBottomNavigation
import com.example.ui.components.swipeBetweenTabs
import com.example.ui.components.FloatingDirectoryPickerSheet
import com.example.ui.components.glideToItem
import com.example.ui.components.NavigationTab
import com.example.util.CustomPlaylist
import com.example.util.PlaylistManager
import com.example.util.VideoFileManager
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.components.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.components.HeaderBar
import com.example.ui.components.LayoutMode
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassDialog
import com.example.ui.components.LiquidPullToRefresh
import com.example.ui.components.ModernFolderIcon
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SortAndViewOptionsPopup
import com.example.ui.components.SortDirection
import com.example.ui.components.bounceClick
import com.example.ui.components.InlineRenameField
import com.example.ui.components.SortField
import com.example.ui.components.StoragePermissionDialog
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VideoItemGridCard
import com.example.ui.components.ViewMode
import com.example.ui.components.VisibleFields
import com.example.ui.components.loadVideoThumbnail
import com.example.ui.components.prefetchVideoThumbnails
import com.example.ui.components.hasVideoPermission
import com.example.ui.components.MinimizedAudioPlayer
import com.example.player.AudioPlaybackManager
import com.example.util.AudioPlaybackHistoryManager
import com.example.util.AudioLibraryCache
import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.state.AppLanguage
import com.example.ui.state.AppStrings
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.HwAccelMode
import com.example.ui.state.SettingsPreferencesManager
import com.example.ui.state.TextEditorNavStep
import com.example.ui.state.ThemeMode
import com.example.ui.state.UiState
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import java.io.File
import java.util.Locale
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkBackgroundEnd
import com.example.ui.theme.DarkBackgroundStart
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightBackgroundEnd
import com.example.ui.theme.LightBackgroundStart
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    uiState: UiState,
    viewModel: com.example.ui.state.AppViewModel? = null,
    onThemeChange: (ThemeMode) -> Unit,
    onScaleChange: (Float) -> Unit,
    onGlassBlurTransparencyChange: (Float) -> Unit = {},
    onSideBySideChange: (Boolean) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onFrameStepChange: (Int) -> Unit = {},
    onCopyTimestampChange: (Boolean) -> Unit = {},
    onHwAccelModeChange: (HwAccelMode) -> Unit = {},
    onGestureSensitivityModeChange: (GestureSensitivityMode) -> Unit = {},
    onRequestPermissions: (() -> Unit)? = null,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showLastWatchedDialog by remember { mutableStateOf(false) }
    var showSortAndViewDialog by remember { mutableStateOf(false) }
    // Network tab: the download button of its header opens the Download page (same header, same tab slot)
    var showDownloads by rememberSaveable { mutableStateOf(viewModel?.isDownloadsPageOpen ?: false) }
    LaunchedEffect(showDownloads) { viewModel?.isDownloadsPageOpen = showDownloads }
    // Series folder opened inside the Downloads page (its name + video count are shown in the top bar)
    var downloadsOpenFolder by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(showDownloads) { if (!showDownloads) downloadsOpenFolder = null }
    // Sort & View of the Download page: the same options / defaults as the Online Video folder, saved under "downloads"
    var downloadsSortField by remember {
        val loaded = SettingsPreferencesManager.loadSortField(context, "downloads")
        mutableStateOf(if (loaded == SortField.DATE || loaded == SortField.TITLE) loaded else SortField.DATE)
    }
    var downloadsSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "downloads")) }
    var downloadsLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "downloads")) }
    var downloadsVisibleFields by remember {
        val loaded = SettingsPreferencesManager.loadVisibleFields(context, "downloads")
        mutableStateOf(loaded.copy(showPath = false, showFramerate = false, showVideoCount = false, showNewBadge = false, showFullName = false))
    }
    var isDownloadsSelectionMode by remember { mutableStateOf(false) }
    var downloadsSelectionCount by remember { mutableStateOf(0) }
    var onDownloadsClearSelectionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onDownloadsSelectAllCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onDownloadsShareCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onDownloadsInfoCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    // Both start from what the app remembered: coming back from the player / a title page shows the SAME search list again
    var isSearchActive by remember { mutableStateOf(viewModel?.homeSearchActive ?: false) }
    var searchQuery by remember { mutableStateOf(viewModel?.homeSearchQuery ?: "") }
    // Search / Sort & View / Settings buttons of the pinned player header: the request arrives here and opens the same UI as the Home header.
    val playerHeaderRequest by com.example.util.PlayerHeaderRequest.pending
    LaunchedEffect(playerHeaderRequest) {
        val req = playerHeaderRequest ?: return@LaunchedEffect
        com.example.util.PlayerHeaderRequest.pending.value = null
        when (req) {
            com.example.util.PlayerHeaderAction.SEARCH -> isSearchActive = true
            com.example.util.PlayerHeaderAction.SORT -> showSortAndViewDialog = true
            com.example.util.PlayerHeaderAction.SETTINGS -> showSettingsSheet = true
        }
    }
    SideEffect {
        viewModel?.homeSearchActive = isSearchActive
        viewModel?.homeSearchQuery = searchQuery
    }
    // A provider's title page opens with a clean search bar; the text typed before comes back when the page is closed (and is kept
    // while the player is open). When the screen is only re-created (e.g. Back from the player) nothing is cleared.
    LaunchedEffect(MediaDetailState.plugin, MediaDetailState.provider) {
        val key = (MediaDetailState.provider?.value ?: "") + "|" + (MediaDetailState.plugin?.value ?: "")
        val vm = viewModel
        if (vm != null && vm.lastTitlePageKey == key) return@LaunchedEffect
        val before = vm?.lastTitlePageKey.orEmpty()
        val sameProvider = (MediaDetailState.provider?.value ?: "") == before.substringBefore('|')
        val titleOpened = MediaDetailState.plugin != null
        if (vm != null) vm.lastTitlePageKey = key
        if (titleOpened) {
            if (vm != null && sameProvider && vm.searchBeforeTitle == null && searchQuery.isNotBlank()) vm.searchBeforeTitle = searchQuery
            searchQuery = ""
            isSearchActive = false
        } else {
            val stash = vm?.searchBeforeTitle
            vm?.searchBeforeTitle = null
            if (stash != null && sameProvider && MediaDetailState.provider != null) {
                searchQuery = stash
                isSearchActive = true
            } else {
                searchQuery = ""
                isSearchActive = false
            }
        }
    }
    // a tapped search suggestion (online music / video / media plugins) goes into the search box and is searched at once
    val pickedSuggestion = com.example.ui.components.OnlineSearchBridge.picked
    LaunchedEffect(pickedSuggestion) {
        if (pickedSuggestion != null) {
            searchQuery = pickedSuggestion
            com.example.ui.components.OnlineSearchBridge.picked = null
        }
    }
    // an emptied search box forgets the last tapped suggestion, so typing it again shows the suggestions again
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) com.example.ui.components.OnlineSearchBridge.lastPicked = ""
    }
    val searchFocusRequester = remember { FocusRequester() }

    // a search bar that was only restored (Back from the player / a title page) does not pop the keyboard up again
    var skipFocusOnce by remember { mutableStateOf(isSearchActive) }
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            if (skipFocusOnce) {
                skipFocusOnce = false
            } else {
                // The search field may not be attached yet (Home just came back from the player, or the bar is still animating in):
                // wait a frame and retry a few times instead of crashing with "FocusRequester is not initialized".
                var attempts = 0
                while (attempts < 8) {
                    androidx.compose.runtime.withFrameNanos { }
                    val ok = try { searchFocusRequester.requestFocus(); true } catch (_: IllegalStateException) { false }
                    if (ok) break
                    attempts++
                    kotlinx.coroutines.delay(60L)
                }
            }
        }
    }

    // Navigation Tabs State (Reactive to AppConfig settings)
    // Returning from the video player must land on the SAME tab (and folder) the video was started from.
    var currentBottomTab by rememberSaveable {
        mutableStateOf(
            viewModel?.lastBottomTabName?.let { n -> NavigationTab.values().firstOrNull { it.name == n } }
                ?: NavigationTab.HOME
        )
    }
    LaunchedEffect(currentBottomTab) { viewModel?.lastBottomTabName = currentBottomTab.name }

    val activeTabs = remember(
        uiState.showHomeTab,
        uiState.showMusicTab,
        uiState.showRecentsTab,
        uiState.showPlaylistsTab,
        uiState.showNetworkTab,
        uiState.showDownloadsTab
    ) {
        val list = mutableListOf<NavigationTab>()
        if (uiState.showHomeTab) list.add(NavigationTab.HOME)
        if (uiState.showMusicTab) list.add(NavigationTab.MUSIC)
        if (uiState.showRecentsTab) list.add(NavigationTab.RECENTS)
        if (uiState.showPlaylistsTab) list.add(NavigationTab.PLAYLISTS)
        if (uiState.showNetworkTab) list.add(NavigationTab.NETWORK)
        if (uiState.showDownloadsTab) list.add(NavigationTab.DOWNLOADS)
        list
    }

    val barSelectedTab = if (activeTabs.contains(currentBottomTab)) {
        currentBottomTab
    } else {
        activeTabs.firstOrNull() ?: NavigationTab.HOME
    }
    // The bottom-bar "Downloads" tab shows the Downloads page that lives inside the Network tab's screen.
    val isDownloadsBarTab = barSelectedTab == NavigationTab.DOWNLOADS
    val effectiveSelectedTab = if (isDownloadsBarTab) NavigationTab.NETWORK else barSelectedTab
    LaunchedEffect(isDownloadsBarTab) { if (isDownloadsBarTab) showDownloads = true }

    BackHandler(enabled = effectiveSelectedTab != NavigationTab.HOME && activeTabs.contains(NavigationTab.HOME)) {
        currentBottomTab = NavigationTab.HOME
    }

    // Per-Tab Sort & View Options State (persisted across app restarts, decoupled for folder vs content)
    var homeFolderSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "home_folders")) }
    var homeFolderSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "home_folders")) }
    var homeFolderLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "home_folders")) }
    var homeFolderVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.loadVisibleFields(context, "home_folders")) }

    var homeContentSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "home_content")) }
    var homeContentSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "home_content")) }
    var homeContentLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "home_content")) }
    var homeContentVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.loadVisibleFields(context, "home_content")) }

    var homeViewMode by remember { mutableStateOf(SettingsPreferencesManager.loadViewMode(context, "home")) }

    // Dedicated Sort & View Options State for the "Online Audio" folder ONLY.
    // Kept strictly isolated from the Music screen and other folders.
    val initialOnlineAudioSortField = remember {
        val loaded = SettingsPreferencesManager.loadSortField(context, "online_audio")
        if (loaded == SortField.DATE || loaded == SortField.TITLE) loaded else SortField.TITLE
    }
    var onlineAudioSortField by remember { mutableStateOf(initialOnlineAudioSortField) }
    var onlineAudioSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "online_audio")) }
    var onlineAudioLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "online_audio")) }
    var onlineAudioVisibleFields by remember {
        val loaded = SettingsPreferencesManager.loadVisibleFields(context, "online_audio")
        mutableStateOf(SettingsPreferencesManager.sanitizeOnlineAudioFields(loaded))
    }

    // Dedicated Sort & View Options State for the "Online Video" folder ONLY (isolated from every other folder).
    var onlineVideoSortField by remember {
        val loaded = SettingsPreferencesManager.loadSortField(context, "online_video")
        mutableStateOf(if (loaded == SortField.DATE || loaded == SortField.TITLE) loaded else SortField.DATE)
    }
    var onlineVideoSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "online_video")) }
    var onlineVideoLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "online_video")) }
    var onlineVideoVisibleFields by remember {
        val loaded = SettingsPreferencesManager.loadVisibleFields(context, "online_video")
        mutableStateOf(SettingsPreferencesManager.sanitizeOnlineVideoFields(loaded))
    }

    // Dedicated Sort & View state for the content of ONLINE playlists (same fields/sort as the online folders, kept separate from them).
    var onlinePlVideoSortField by remember {
        val loaded = SettingsPreferencesManager.loadSortField(context, "online_playlist_video")
        mutableStateOf(if (loaded == SortField.DATE || loaded == SortField.TITLE) loaded else SortField.DATE)
    }
    var onlinePlVideoSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "online_playlist_video")) }
    var onlinePlVideoLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "online_playlist_video")) }
    var onlinePlVideoVisibleFields by remember {
        mutableStateOf(SettingsPreferencesManager.sanitizeOnlineVideoFields(SettingsPreferencesManager.loadVisibleFields(context, "online_playlist_video")))
    }
    var onlinePlAudioSortField by remember {
        val loaded = SettingsPreferencesManager.loadSortField(context, "online_playlist_audio")
        mutableStateOf(if (loaded == SortField.DATE || loaded == SortField.TITLE) loaded else SortField.TITLE)
    }
    var onlinePlAudioSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "online_playlist_audio")) }
    var onlinePlAudioLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "online_playlist_audio")) }
    var onlinePlAudioVisibleFields by remember {
        mutableStateOf(SettingsPreferencesManager.sanitizeOnlineAudioFields(SettingsPreferencesManager.loadVisibleFields(context, "online_playlist_audio")))
    }
    val isOnlineVideoOpen = com.example.ui.components.OnlineVideoFeedState.isOpen

    val viewMode = homeViewMode

    var musicFolderSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "music_folders")) }
    var musicFolderSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "music_folders")) }
    var musicFolderLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "music_folders")) }
    var musicFolderVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.loadVisibleFields(context, "music_folders")) }

    var musicContentSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "music_content")) }
    var musicContentSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "music_content")) }
    var musicContentLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "music_content")) }
    var musicContentVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.loadVisibleFields(context, "music_content")) }

    var musicViewMode by remember { mutableStateOf(SettingsPreferencesManager.loadViewMode(context, "music")) }

    var recentsFolderSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "recents_folders")) }
    var recentsFolderSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "recents_folders")) }
    var recentsFolderLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "recents_folders")) }
    var recentsFolderVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.sanitizeRecentsFields(SettingsPreferencesManager.loadVisibleFields(context, "recents_folders"))) }

    var recentsContentSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "recents_content")) }
    var recentsContentSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "recents_content")) }
    var recentsContentLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "recents_content")) }
    var recentsContentVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.sanitizeRecentsFields(SettingsPreferencesManager.loadVisibleFields(context, "recents_content"))) }

    // Recent history is always shown in Library style (View Mode option removed).
    val recentsViewMode = ViewMode.LIBRARY

    var playlistsFolderSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "playlists_folders")) }
    var playlistsFolderSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "playlists_folders")) }
    var playlistsFolderLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "playlists_folders")) }
    var playlistsFolderVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.loadVisibleFields(context, "playlists_folders")) }

    var playlistsContentSortField by remember { mutableStateOf(SettingsPreferencesManager.loadSortField(context, "playlists_content")) }
    var playlistsContentSortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "playlists_content")) }
    var playlistsContentLayoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "playlists_content")) }
    var playlistsContentVisibleFields by remember { mutableStateOf(SettingsPreferencesManager.loadVisibleFields(context, "playlists_content")) }

    var playlistsViewMode by remember { mutableStateOf(SettingsPreferencesManager.loadViewMode(context, "playlists")) }

    // Restored from the ViewModel: this screen is rebuilt when the video player closes, and Back must not move the list.
    val restoreFolderScrollId = viewModel?.folderNavigationStack?.lastOrNull()?.id
    val restoreFolderScroll = restoreFolderScrollId != null && restoreFolderScrollId == viewModel?.homeFolderScrollFolderId
    val libraryListState = rememberLazyListState(
        viewModel?.homeLibraryScrollIndex ?: 0,
        viewModel?.homeLibraryScrollOffset ?: 0
    )
    val folderContentListState = rememberLazyListState(
        if (restoreFolderScroll) viewModel?.homeFolderListScrollIndex ?: 0 else 0,
        if (restoreFolderScroll) viewModel?.homeFolderListScrollOffset ?: 0 else 0
    )
    val folderContentGridState = rememberLazyGridState(
        if (restoreFolderScroll) viewModel?.homeFolderGridScrollIndex ?: 0 else 0,
        if (restoreFolderScroll) viewModel?.homeFolderGridScrollOffset ?: 0 else 0
    )

    var isInsideMusicFolder by remember { mutableStateOf(false) }
    var selectedMusicFolder by remember { mutableStateOf<AudioFolderItem?>(null) }
    var selectedRecentsFolder by remember { mutableStateOf<RecentsFolder?>(null) }
    var selectedPlaylist by remember {
        mutableStateOf<com.example.util.CustomPlaylist?>(
            viewModel?.lastPlaylistId?.let { id ->
                com.example.util.PlaylistManager.getPlaylists(context).firstOrNull { it.id == id }
            }
        )
    }
    LaunchedEffect(selectedPlaylist?.id) { viewModel?.lastPlaylistId = selectedPlaylist?.id }
    // Incremented by the floating "+ Create Playlist" button; CustomPlaylistsTabContent opens its options sheet on each change.
    var playlistCreateRequest by remember { mutableStateOf(0) }
    var isInsideRecentsFolder by remember { mutableStateOf(false) }
    var isInsidePlaylistDetail by remember { mutableStateOf(selectedPlaylist != null) }

    LaunchedEffect(effectiveSelectedTab) {
        if (effectiveSelectedTab != NavigationTab.NETWORK) {
            showDownloads = false
            viewModel?.isDownloadsPageOpen = false
        }
    }
    BackHandler(enabled = effectiveSelectedTab == NavigationTab.NETWORK && showDownloads && (isDownloadsSelectionMode || !isDownloadsBarTab)) {
        if (isDownloadsSelectionMode) {
            onDownloadsClearSelectionCallback?.invoke()
        } else {
            showDownloads = false
            searchQuery = ""
            isSearchActive = false
        }
    }
    BackHandler(enabled = effectiveSelectedTab == NavigationTab.MUSIC && selectedMusicFolder != null) {
        searchQuery = ""
        isSearchActive = false
        selectedMusicFolder = null
    }
    BackHandler(enabled = effectiveSelectedTab == NavigationTab.RECENTS && selectedRecentsFolder != null) {
        selectedRecentsFolder = null
    }
    BackHandler(enabled = effectiveSelectedTab == NavigationTab.PLAYLISTS && selectedPlaylist != null) {
        searchQuery = ""
        isSearchActive = false
        selectedPlaylist = null
    }

    var tabEffectFirstRun by remember { mutableStateOf(true) }
    LaunchedEffect(effectiveSelectedTab) {
        // The first run is the screen being (re)created, e.g. coming back from the player: keep the open folder.
        if (tabEffectFirstRun) { tabEffectFirstRun = false; return@LaunchedEffect }
        searchQuery = ""
        isSearchActive = false
        selectedMusicFolder = null
        selectedRecentsFolder = null
        selectedPlaylist = null
    }

    val isDark = uiState.themeMode == ThemeMode.DARK ||
            (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    val lang = uiState.language

    // Visual Palette
    // Main screen uses the same colour as the "Sort & View Options" panel (opaque), in light / dark / system mode.
    // AMOLED black keeps its pure black background.
    val mainScreenColor = if (isDark && uiState.amoledBlackMode) {
        DarkBackgroundStart
    } else if (isDark) {
        Neutral.D1B2630
    } else if (uiState.themeMode == ThemeMode.SYSTEM) {
        Neutral.FFFFFF
    } else {
        Neutral.DAE3E9
    }
    val backgroundBrush = Brush.verticalGradient(colors = listOf(mainScreenColor, mainScreenColor))
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    // Custom folders persistent list
    val customFolders = remember {
        mutableStateListOf<String>().apply {
            addAll(SettingsPreferencesManager.loadCustomFolders(context))
        }
    }

    // Scanned Video Folders State (Instantly populated from persistent cache for 0ms load)
    val videoFolders = remember {
        mutableStateListOf<VideoFolder>().apply {
            val cached = VideoLibraryCache.loadCachedFolders(context)
            if (!cached.isNullOrEmpty()) {
                addAll(cached)
            }
        }
    }
    var isManualRefreshing by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(hasVideoPermission(context)) }

    // Folder Navigation Stack for Nested Folders & Videos (sync with ViewModel if available)
    val folderStack = remember {
        mutableStateListOf<VideoFolder>().apply {
            if (viewModel != null && viewModel.folderNavigationStack.isNotEmpty()) {
                addAll(viewModel.folderNavigationStack)
            }
        }
    }
    val currentFolder = folderStack.lastOrNull()

    // The video that was watched last (library = global last, inside a folder = that folder's last). Its thumbnail gets the
    // selection-style gradient border when the user comes back to the list, so the last position is easy to find.
    val lastWatchedHistoryVersion by com.example.util.PlaybackHistoryManager.version.collectAsState()
    val lastWatchedRecord by androidx.compose.runtime.produceState<com.example.util.VideoPlaybackRecord?>(
        initialValue = null,
        lastWatchedHistoryVersion, homeViewMode, currentFolder?.path
    ) {
        value = withContext(Dispatchers.Default) {
            if (homeViewMode == ViewMode.FOLDER && currentFolder != null) {
                com.example.util.PlaybackHistoryManager.getLastWatchedRecordInFolder(context, currentFolder.path)
            } else {
                com.example.util.PlaybackHistoryManager.getLastWatchedRecord(context)
            }
        }
    }
    val isLastWatched: (VideoItem) -> Boolean = { v ->
        val last = lastWatchedRecord
        last != null && (
            (last.path.isNotBlank() && v.path == last.path) ||
                (last.uriString.isNotBlank() && v.uri.toString() == last.uriString) ||
                (last.path.isBlank() && last.uriString.isBlank() && last.videoId > 0 && v.id == last.videoId)
            )
    }

    // Navigation & Folder level detection for Sort and View
    val isCurrentInsideFolder = when (effectiveSelectedTab) {
        NavigationTab.HOME -> currentFolder != null || homeViewMode == ViewMode.LIBRARY
        NavigationTab.MUSIC -> isInsideMusicFolder || musicViewMode == ViewMode.LIBRARY
        NavigationTab.RECENTS -> isInsideRecentsFolder || recentsViewMode == ViewMode.LIBRARY
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> false
        NavigationTab.PLAYLISTS -> isInsidePlaylistDetail
    }

    val activeFolderSortField = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeFolderSortField
        NavigationTab.MUSIC -> musicFolderSortField
        NavigationTab.RECENTS -> recentsFolderSortField
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsFolderSortField
        NavigationTab.PLAYLISTS -> playlistsFolderSortField
    }
    val activeFolderSortDirection = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeFolderSortDirection
        NavigationTab.MUSIC -> musicFolderSortDirection
        NavigationTab.RECENTS -> recentsFolderSortDirection
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsFolderSortDirection
        NavigationTab.PLAYLISTS -> playlistsFolderSortDirection
    }
    val activeFolderLayoutMode = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeFolderLayoutMode
        NavigationTab.MUSIC -> musicFolderLayoutMode
        NavigationTab.RECENTS -> recentsFolderLayoutMode
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsFolderLayoutMode
        NavigationTab.PLAYLISTS -> playlistsFolderLayoutMode
    }
    val activeFolderVisibleFields = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeFolderVisibleFields
        NavigationTab.MUSIC -> musicFolderVisibleFields
        NavigationTab.RECENTS -> recentsFolderVisibleFields
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsFolderVisibleFields
        NavigationTab.PLAYLISTS -> playlistsFolderVisibleFields
    }

    val activeContentSortField = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeContentSortField
        NavigationTab.MUSIC -> musicContentSortField
        NavigationTab.RECENTS -> recentsContentSortField
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsContentSortField
        NavigationTab.PLAYLISTS -> playlistsContentSortField
    }
    val activeContentSortDirection = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeContentSortDirection
        NavigationTab.MUSIC -> musicContentSortDirection
        NavigationTab.RECENTS -> recentsContentSortDirection
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsContentSortDirection
        NavigationTab.PLAYLISTS -> playlistsContentSortDirection
    }
    val activeContentLayoutMode = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeContentLayoutMode
        NavigationTab.MUSIC -> musicContentLayoutMode
        NavigationTab.RECENTS -> recentsContentLayoutMode
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsContentLayoutMode
        NavigationTab.PLAYLISTS -> playlistsContentLayoutMode
    }
    val activeContentVisibleFields = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeContentVisibleFields
        NavigationTab.MUSIC -> musicContentVisibleFields
        NavigationTab.RECENTS -> recentsContentVisibleFields
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsContentVisibleFields
        NavigationTab.PLAYLISTS -> playlistsContentVisibleFields
    }

    val activeSortField = if (isCurrentInsideFolder) activeContentSortField else activeFolderSortField
    val activeSortDirection = if (isCurrentInsideFolder) activeContentSortDirection else activeFolderSortDirection
    val activeLayoutMode = if (isCurrentInsideFolder) activeContentLayoutMode else activeFolderLayoutMode

    val activeViewMode = when (effectiveSelectedTab) {
        NavigationTab.HOME -> homeViewMode
        NavigationTab.MUSIC -> musicViewMode
        NavigationTab.RECENTS -> recentsViewMode
        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> playlistsViewMode
        NavigationTab.PLAYLISTS -> playlistsViewMode
    }

    val effectiveHomeFolderVisibleFields = homeFolderVisibleFields.copy(
        showThumbnails = uiState.showVideoThumbnails,
        showFullName = uiState.showFullName,
        showNewBadge = uiState.showNewVideoLabel
    )
    val effectiveHomeContentVisibleFields = homeContentVisibleFields.copy(
        showThumbnails = uiState.showVideoThumbnails,
        showFullName = uiState.showFullName,
        showNewBadge = uiState.showNewVideoLabel
    )
    val effectiveFolderVisibleFields = activeFolderVisibleFields.copy(
        showThumbnails = uiState.showVideoThumbnails,
        showFullName = uiState.showFullName,
        showNewBadge = uiState.showNewVideoLabel
    )
    val effectiveContentVisibleFields = activeContentVisibleFields.copy(
        showThumbnails = uiState.showVideoThumbnails,
        showFullName = uiState.showFullName,
        showNewBadge = uiState.showNewVideoLabel
    )
    val effectiveVisibleFields = if (isCurrentInsideFolder) effectiveContentVisibleFields else effectiveFolderVisibleFields

    fun syncFolderStackToViewModel() {
        if (viewModel != null) {
            viewModel.folderNavigationStack.clear()
            viewModel.folderNavigationStack.addAll(folderStack)
        }
    }

    // Helper to push folder
    fun pushFolder(folder: VideoFolder) {
        var current = folder
        val visited = mutableSetOf<String>()
        folderStack.add(current)
        visited.add(current.path)

        // When enabled, skip an unlimited chain of folders that each contain
        // exactly one child folder and no direct videos. This makes navigation
        // behave like the requested tree-path compression instead of merely
        // storing a preference.
        if (uiState.treePathCompression) {
            while (current.videos.isEmpty() && current.subFolders.size == 1) {
                val next = current.subFolders.first()
                if (!visited.add(next.path)) break
                current = next
                folderStack.add(current)
            }
        }
        syncFolderStackToViewModel()
    }

    // Helper to pop folder
    fun popFolder(): Boolean {
        if (folderStack.isNotEmpty()) {
            folderStack.removeLastOrNull()
            syncFolderStackToViewModel()
            return true
        }
        return false
    }

    BackHandler(enabled = effectiveSelectedTab == NavigationTab.HOME && currentFolder != null) {
        searchQuery = ""
        isSearchActive = false
        popFolder()
    }

    BackHandler(enabled = effectiveSelectedTab == NavigationTab.HOME && isOnlineVideoOpen) {
        searchQuery = ""
        isSearchActive = false
        com.example.ui.components.OnlineVideoFeedState.isOpen = false
    }

    // Selection Mode State (Folders & Videos)
    val selectedFolderIds = remember { mutableStateListOf<String>() }
    val selectedVideoPaths = remember { mutableStateListOf<String>() }
    var isPlaylistTabSelectionMode by remember { mutableStateOf(false) }
    var playlistSelectionCount by remember { mutableStateOf(0) }
    var playlistSelectionIsFolder by remember { mutableStateOf(true) }
    var onPlaylistClearSelectionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onPlaylistSelectAllCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onPlaylistShareCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    var isAudioTabSelectionMode by remember { mutableStateOf(false) }
    var audioSelectionCount by remember { mutableStateOf(0) }
    var audioSelectionIsFolder by remember { mutableStateOf(true) }
    var onAudioClearSelectionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onAudioSelectAllCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onAudioShareCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onAudioInfoCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Information surfaces intentionally stay inside HomeScreen so the existing
    // folder/video selection remains intact when the user returns from details.
    var selectedVideoForInfo by remember { mutableStateOf<VideoItem?>(null) }
    var selectedAudioForInfo by remember { mutableStateOf<AudioTrackItem?>(null) }
    var selectedFolderForInfo by remember { mutableStateOf<VideoFolder?>(null) }
    var selectedAudioFolderForInfo by remember { mutableStateOf<AudioFolderItem?>(null) }
    var showFolderInfoSheet by remember { mutableStateOf(false) }
    var showAudioFolderInfoSheet by remember { mutableStateOf(false) }

    var isMoveCopyInProgress by remember { mutableStateOf(false) }
    var moveCopyProgressFraction by remember { mutableStateOf(0f) }
    var moveCopyIsMove by remember { mutableStateOf(true) }
    var moveCopyIsDelete by remember { mutableStateOf(false) }
    var moveCopyItemName by remember { mutableStateOf("") }
    var moveCopyCompletedCount by remember { mutableStateOf(0) }
    var moveCopyTotalCount by remember { mutableStateOf(0) }

    val isHomeSelectionMode = selectedFolderIds.isNotEmpty() || selectedVideoPaths.isNotEmpty() || isMoveCopyInProgress
    val isSelectionMode = isHomeSelectionMode ||
            (effectiveSelectedTab == NavigationTab.PLAYLISTS && isPlaylistTabSelectionMode) ||
            (effectiveSelectedTab == NavigationTab.MUSIC && isAudioTabSelectionMode) ||
            (effectiveSelectedTab == NavigationTab.NETWORK && showDownloads && isDownloadsSelectionMode)
    val isFolderSelection = selectedFolderIds.isNotEmpty()
    val isVideoSelection = selectedVideoPaths.isNotEmpty()
    val totalSelectedCount = selectedFolderIds.size + selectedVideoPaths.size

    // Inline Rename States
    var renamingFolderId by remember { mutableStateOf<String?>(null) }
    var renamingVideoPath by remember { mutableStateOf<String?>(null) }

    // Selection Dialog States
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var renameFolderNameText by remember { mutableStateOf("") }
    var folderToRename by remember { mutableStateOf<VideoFolder?>(null) }

    var showRenameVideoDialog by remember { mutableStateOf(false) }
    var renameVideoNameText by remember { mutableStateOf("") }
    var videoToRename by remember { mutableStateOf<VideoItem?>(null) }

    // Bulk rename (2+ videos selected): the videos are captured when the sheet opens so a background
    // library refresh cannot reset what the user is typing.
    var bulkRenameVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDirectoryPickerSheet by remember { mutableStateOf(false) }
    var dirPickerAction by remember { mutableStateOf(DirectoryPickerAction.MOVE) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    // Playlist Long-Press Actions & Dialogs
    var playlistTriggerRefresh by remember { mutableStateOf(0) }
    var selectedPlaylistForAction by remember { mutableStateOf<com.example.util.CustomPlaylist?>(null) }
    var showRenamePlaylistDialog by remember { mutableStateOf(false) }
    var renamePlaylistText by remember { mutableStateOf("") }
    var showDeletePlaylistConfirmDialog by remember { mutableStateOf(false) }
    var showClearPlaylistConfirmDialog by remember { mutableStateOf(false) }

    fun clearSelection() {
        renamingFolderId = null
        renamingVideoPath = null
        showDeleteConfirmDialog = false
        selectedFolderIds.clear()
        selectedVideoPaths.clear()
        if (effectiveSelectedTab == NavigationTab.PLAYLISTS) {
            onPlaylistClearSelectionCallback?.invoke()
        }
    }

    fun toggleFolderSelection(folder: VideoFolder) {
        renamingFolderId = null
        renamingVideoPath = null
        showDeleteConfirmDialog = false
        selectedVideoPaths.clear()
        if (selectedFolderIds.contains(folder.id)) {
            selectedFolderIds.remove(folder.id)
        } else {
            selectedFolderIds.add(folder.id)
        }
    }

    fun toggleVideoSelection(video: VideoItem) {
        renamingFolderId = null
        renamingVideoPath = null
        showDeleteConfirmDialog = false
        selectedFolderIds.clear()
        if (selectedVideoPaths.contains(video.path)) {
            selectedVideoPaths.remove(video.path)
        } else {
            selectedVideoPaths.add(video.path)
        }
    }

    // Handle Hardware / Gesture Back Navigation (Exits selection mode first if active)
    BackHandler(enabled = renamingFolderId != null || renamingVideoPath != null || showDeleteConfirmDialog || isHomeSelectionMode || folderStack.isNotEmpty()) {
        if (renamingFolderId != null || renamingVideoPath != null) {
            renamingFolderId = null
            renamingVideoPath = null
        } else if (showDeleteConfirmDialog) {
            showDeleteConfirmDialog = false
        } else if (isHomeSelectionMode) {
            clearSelection()
        } else {
            popFolder()
        }
    }

    // Haptic tick when going back out of a folder / list (Home folder stack, Music, Recents, Playlist, online feed).
    val folderBackHapticView = androidx.compose.ui.platform.LocalView.current
    val folderDepth = folderStack.size +
        (if (selectedMusicFolder != null) 1 else 0) +
        (if (selectedRecentsFolder != null) 1 else 0) +
        (if (selectedPlaylist != null) 1 else 0) +
        (if (isOnlineVideoOpen) 1 else 0)
    var previousFolderDepth by remember { mutableStateOf(folderDepth) }
    LaunchedEffect(folderDepth) {
        if (folderDepth < previousFolderDepth) com.example.util.AppHaptics.performClick(folderBackHapticView)
        previousFolderDepth = folderDepth
    }

    // Rescan function - Silent background scanning without UI flashing
    fun reloadFolders(showIndicator: Boolean = false) {
        if (showIndicator) {
            isManualRefreshing = true
        }
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val scanned = VideoFolderScanner.scanVideoFolders(context, customFolders.toList())
                if (showIndicator) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val minDisplayTime = 700L
                    if (elapsed < minDisplayTime) {
                        kotlinx.coroutines.delay(minDisplayTime - elapsed)
                    }
                }
                withContext(Dispatchers.Main) {
                    // In-place diff update to prevent UI flashing, disappearing folders, and recomposition flicker
                    if (videoFolders.toList() != scanned) {
                        val newIds = scanned.map { it.id }.toSet()
                        videoFolders.removeAll { it.id !in newIds }
                        for (i in scanned.indices) {
                            val item = scanned[i]
                            val existingIndex = videoFolders.indexOfFirst { it.id == item.id }
                            if (existingIndex == -1) {
                                videoFolders.add(i.coerceAtMost(videoFolders.size), item)
                            } else if (videoFolders[existingIndex] != item) {
                                videoFolders[existingIndex] = item
                            }
                        }
                        for (i in scanned.indices) {
                            if (i < videoFolders.size && videoFolders[i].id != scanned[i].id) {
                                val targetIdx = videoFolders.indexOfFirst { it.id == scanned[i].id }
                                if (targetIdx != -1) {
                                    val el = videoFolders.removeAt(targetIdx)
                                    videoFolders.add(i, el)
                                }
                            }
                        }

                        // Re-resolve active folders in folderStack so references are updated with fresh files
                        if (folderStack.isNotEmpty()) {
                            val newStack = mutableListOf<VideoFolder>()
                            var valid = true

                            for (stacked in folderStack) {
                                val matched = findFolderInTree(scanned, stacked.id, stacked.path)
                                if (matched != null) {
                                    newStack.add(matched)
                                } else {
                                    valid = false
                                    break
                                }
                            }

                            if (valid && newStack.isNotEmpty()) {
                                folderStack.clear()
                                folderStack.addAll(newStack)
                                syncFolderStackToViewModel()
                            } else if (!valid) {
                                folderStack.clear()
                                syncFolderStackToViewModel()
                            }
                        }
                    }
                }
            } finally {
                // NonCancellable: a cancelled scan (screen left / refreshed again) must still end the refresh animation
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    if (showIndicator) {
                        isManualRefreshing = false
                    }
                }
            }
        }
    }

    suspend fun reloadFoldersAndWaitAfterMutation() {
        val scanned = withContext(Dispatchers.IO) {
            VideoFolderScanner.scanVideoFolders(context, customFolders.toList())
        }
        withContext(Dispatchers.Main) {
            val newIds = scanned.map { it.id }.toSet()
            videoFolders.removeAll { it.id !in newIds }
            for (i in scanned.indices) {
                val item = scanned[i]
                val existingIndex = videoFolders.indexOfFirst { it.id == item.id }
                if (existingIndex == -1) videoFolders.add(i.coerceAtMost(videoFolders.size), item)
                else if (videoFolders[existingIndex] != item) videoFolders[existingIndex] = item
            }
            for (i in scanned.indices) {
                if (i < videoFolders.size && videoFolders[i].id != scanned[i].id) {
                    val targetIdx = videoFolders.indexOfFirst { it.id == scanned[i].id }
                    if (targetIdx != -1) {
                        val item = videoFolders.removeAt(targetIdx)
                        videoFolders.add(i, item)
                    }
                }
            }
            if (folderStack.isNotEmpty()) {
                val newStack = folderStack.mapNotNull { stacked ->
                    findFolderInTree(scanned, stacked.id, stacked.path)
                }
                if (newStack.size == folderStack.size) {
                    folderStack.clear()
                    folderStack.addAll(newStack)
                    syncFolderStackToViewModel()
                } else {
                    folderStack.clear()
                    syncFolderStackToViewModel()
                }
            }
        }
    }

    fun handleFolderRename(folder: VideoFolder, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == folder.name) {
            renamingFolderId = null
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            val success = VideoFileManager.renameFolder(context, folder, trimmed)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Folder renamed to \"$trimmed\"", Toast.LENGTH_SHORT).show()
                    if (customFolders.contains(folder.name)) {
                        customFolders.remove(folder.name)
                        customFolders.add(trimmed)
                        SettingsPreferencesManager.saveCustomFolders(context, customFolders.toSet())
                    }
                    reloadFolders()
                    clearSelection()
                } else {
                    Toast.makeText(context, "Failed to rename folder", Toast.LENGTH_SHORT).show()
                }
                renamingFolderId = null
            }
        }
    }

    fun handleVideoRename(video: VideoItem, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            renamingVideoPath = null
            return
        }
        val srcFile = java.io.File(video.path)
        val ext = srcFile.extension
        val finalName = if (ext.isNotBlank() && !trimmed.endsWith(".$ext", ignoreCase = true)) {
            "$trimmed.$ext"
        } else {
            trimmed
        }
        coroutineScope.launch(Dispatchers.IO) {
            val success = VideoFileManager.renameVideo(context, video, finalName)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Video renamed to \"$finalName\"", Toast.LENGTH_SHORT).show()
                    reloadFolders()
                    clearSelection()
                } else {
                    Toast.makeText(context, "Failed to rename video", Toast.LENGTH_SHORT).show()
                }
                renamingVideoPath = null
            }
        }
    }

    // Real-time ContentObserver: Automatically detects when any video is downloaded, moved, or deleted in file manager/browser (Silent)
    DisposableEffect(context) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var debounceJob: kotlinx.coroutines.Job? = null
        var isObserverRegistered = false
        val contentObserver = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                super.onChange(selfChange, uri)
                debounceJob?.cancel()
                debounceJob = coroutineScope.launch {
                    kotlinx.coroutines.delay(600) // Debounce rapid writes
                    reloadFolders(false) // Silent background refresh
                }
            }
        }

        try {
            val videoExtUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            context.contentResolver.registerContentObserver(videoExtUri, true, contentObserver)
            isObserverRegistered = true
        } catch (_: Throwable) {}

        onDispose {
            debounceJob?.cancel()
            if (isObserverRegistered) {
                try {
                    context.contentResolver.unregisterContentObserver(contentObserver)
                } catch (_: Throwable) {}
            }
        }
    }

    // Lifecycle check for permissions & auto-sync when returning to app (Silent)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentPerm = hasVideoPermission(context)
                hasPermission = currentPerm
                if (currentPerm) {
                    reloadFolders(false) // Silent background refresh
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial check on launch & reactive refresh trigger from ViewModel / ActivityResultContracts
    val refreshTrigger by viewModel?.mediaRefreshTrigger?.collectAsState() ?: remember { mutableStateOf(0L) }
    LaunchedEffect(refreshTrigger) {
        val currentPerm = hasVideoPermission(context)
        hasPermission = currentPerm
        if (currentPerm) {
            reloadFolders(false)
        }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            showPermissionDialog = true
        } else {
            showPermissionDialog = false
        }
        reloadFolders(false)
    }

    // Dynamic Density Override for Zoom / Scaling
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, uiState.appScale) {
        // 1% maps to 0.85f (85% min size), 100% maps to 1.00f (100% full scale)
        val factor = (0.85f + 0.15f * ((uiState.appScale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
        Density(
            density = baseDensity.density * factor,
            fontScale = baseDensity.fontScale * factor
        )
    }

    LaunchedEffect(Unit) {
        FolderBlockListManager.init(context)
        PrivateVaultManager.init(context)
    }

    val blockedVideoFolderIds by FolderBlockListManager.blockedVideoFolderIds
    val privateFolderList by PrivateVaultManager.privateFolders
    val isVaultSessionAuthenticated by PrivateVaultManager.isSessionAuthenticated
    val seenVersion by com.example.util.MediaSeenManager.seenVersion.collectAsState()

    // Filtered and sorted root folders (with safeguard deduplication, excluding blocked and private folders)
    val displayedFolders = remember(
        videoFolders.toList(), searchQuery, homeFolderSortField, homeFolderSortDirection,
        blockedVideoFolderIds, privateFolderList,
        uiState.showFolderUnplayedBadge, uiState.showNewVideoLabel, uiState.newVideoDaysThreshold,
        seenVersion
    ) {
        val deduplicated = VideoLibraryCache.deduplicateFoldersList(videoFolders.toList())
        val unblocked = deduplicated.filter { folder ->
            !blockedVideoFolderIds.contains(folder.id) &&
            !blockedVideoFolderIds.contains(folder.path) &&
            !PrivateVaultManager.isFolderPrivate(folder.id) &&
            !PrivateVaultManager.isFolderPrivate(folder.path)
        }
        val filtered = if (searchQuery.isBlank()) {
            unblocked
        } else {
            unblocked.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }
        val sorted = when (homeFolderSortField) {
            SortField.TITLE -> {
                if (homeFolderSortDirection == SortDirection.ASCENDING) {
                    filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
                } else {
                    filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.name, a.name) }
                }
            }
            SortField.DATE -> {
                if (homeFolderSortDirection == SortDirection.DESCENDING) {
                    filtered.sortedByDescending { it.lastModifiedDate }
                } else {
                    filtered.sortedBy { it.lastModifiedDate }
                }
            }
            SortField.SIZE -> {
                if (homeFolderSortDirection == SortDirection.ASCENDING) filtered.sortedBy { it.totalSizeBytes }
                else filtered.sortedByDescending { it.totalSizeBytes }
            }
            SortField.COUNT -> {
                if (homeFolderSortDirection == SortDirection.ASCENDING) filtered.sortedBy { it.allVideosCount }
                else filtered.sortedByDescending { it.allVideosCount }
            }
            SortField.DURATION -> filtered
        }

        sorted.map { folder ->
            val unplayedCount = folder.getAllVideos().count { video ->
                com.example.util.MediaSeenManager.isVideoNew(
                    context,
                    video,
                    uiState.newVideoDaysThreshold,
                    uiState.showNewVideoLabel
                )
            }
            folder.copy(newVideosCount = if (uiState.showFolderUnplayedBadge) unplayedCount else 0)
        }
    }

    // Filtered and sorted subfolders of currently opened folder
    val displayedSubFolders = remember(
        currentFolder, searchQuery, homeFolderSortField, homeFolderSortDirection,
        blockedVideoFolderIds, privateFolderList,
        uiState.showFolderUnplayedBadge, uiState.showNewVideoLabel, uiState.newVideoDaysThreshold,
        seenVersion
    ) {
        if (currentFolder == null) return@remember emptyList<VideoFolder>()
        val unblockedSubs = currentFolder.subFolders.filter { folder ->
            !blockedVideoFolderIds.contains(folder.id) &&
            !blockedVideoFolderIds.contains(folder.path) &&
            !PrivateVaultManager.isFolderPrivate(folder.id) &&
            !PrivateVaultManager.isFolderPrivate(folder.path)
        }
        val filtered = if (searchQuery.isBlank()) {
            unblockedSubs
        } else {
            unblockedSubs.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }
        val sorted = when (homeFolderSortField) {
            SortField.TITLE -> if (homeFolderSortDirection == SortDirection.ASCENDING) filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) } else filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.name, a.name) }
            SortField.DATE -> if (homeFolderSortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.lastModifiedDate } else filtered.sortedBy { it.lastModifiedDate }
            SortField.SIZE -> if (homeFolderSortDirection == SortDirection.ASCENDING) filtered.sortedBy { it.totalSizeBytes } else filtered.sortedByDescending { it.totalSizeBytes }
            // Count is intentionally a HOME root-folder sort only; inside a folder it falls back to title order.
            SortField.COUNT -> if (homeFolderSortDirection == SortDirection.ASCENDING) filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) } else filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.name, a.name) }
            SortField.DURATION -> filtered
        }

        sorted.map { folder ->
            val unplayedCount = folder.getAllVideos().count { video ->
                com.example.util.MediaSeenManager.isVideoNew(
                    context,
                    video,
                    uiState.newVideoDaysThreshold,
                    uiState.showNewVideoLabel
                )
            }
            folder.copy(newVideosCount = if (uiState.showFolderUnplayedBadge) unplayedCount else 0)
        }
    }

    // Filtered, sorted and appearance-decorated video items of the current folder.
    val displayedVideos = remember(
        currentFolder, searchQuery, homeContentSortField, homeContentSortDirection,
        uiState.showNewVideoLabel, uiState.newVideoDaysThreshold,
        seenVersion
    ) {
        if (currentFolder == null) return@remember emptyList<VideoItem>()
        val filtered = if (searchQuery.isBlank()) {
            currentFolder.videos
        } else {
            currentFolder.videos.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }
        val sorted = when (homeContentSortField) {
            SortField.TITLE -> if (homeContentSortDirection == SortDirection.ASCENDING) filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.displayName, b.displayName) } else filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.displayName, a.displayName) }
            SortField.DATE -> if (homeContentSortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.dateModified } else filtered.sortedBy { it.dateModified }
            SortField.SIZE -> if (homeContentSortDirection == SortDirection.ASCENDING) filtered.sortedBy { it.sizeBytes } else filtered.sortedByDescending { it.sizeBytes }
            SortField.DURATION -> if (homeContentSortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.durationMs } else filtered.sortedBy { it.durationMs }
            SortField.COUNT -> filtered
        }
        sorted.map { video ->
            val isNew = com.example.util.MediaSeenManager.isVideoNew(
                context,
                video,
                uiState.newVideoDaysThreshold,
                uiState.showNewVideoLabel
            )
            video.copy(isNew = isNew)
        }
    }

    // Warm thumbnails for every non-blocked, non-private video in every folder. This is
    // deliberately independent of LazyColumn/LazyGrid visibility, so opening one video does
    // not leave the rest of the library waiting for the user to scroll through it.
    val thumbnailPrefetchVideos = remember(
        videoFolders.toList(),
        currentFolder,
        blockedVideoFolderIds,
        privateFolderList
    ) {
        val eligibleFolders = videoFolders.filter { folder ->
            !blockedVideoFolderIds.contains(folder.id) &&
                !blockedVideoFolderIds.contains(folder.path) &&
                !PrivateVaultManager.isFolderPrivate(folder.id) &&
                !PrivateVaultManager.isFolderPrivate(folder.path)
        }

        // Prioritize the folder that is actually open. This makes its thumbnails warm first,
        // so rapid up/down scrolling inside a large folder keeps finding decoded frames in the
        // memory cache instead of waiting for the background library prefetch to reach them.
        val currentFolderVideos = currentFolder?.videos.orEmpty()
        val otherVideos = eligibleFolders
            .flatMap { it.getAllVideos() }

        (currentFolderVideos + otherVideos)
            .distinctBy { "${it.id}|${it.path}|${it.uri}" }
    }

    LaunchedEffect(
        thumbnailPrefetchVideos,
        uiState.showVideoThumbnails,
        uiState.thumbnailStrategy,
        uiState.thumbnailQuality,
        uiState.thumbnailFallbackSecond,
        uiState.showNetworkThumbnails
    ) {
        if (uiState.showVideoThumbnails) {
            // Let the currently visible UI render first; extraction then continues off the main
            // thread and progressively fills the disk/memory cache for all folders.
            kotlinx.coroutines.yield()
            // Jobs live in a process-wide scope (not in this screen), so they keep warming every
            // folder while the user browses folders or plays a video.
            com.example.ui.components.ThumbnailPrefetcher.startPriority(
                context = context,
                videos = currentFolder?.videos.orEmpty(),
                strategy = uiState.thumbnailStrategy,
                quality = uiState.thumbnailQuality,
                allowNetwork = uiState.showNetworkThumbnails,
                fallbackSecond = uiState.thumbnailFallbackSecond
            )
            com.example.ui.components.ThumbnailPrefetcher.startLibrary(
                context = context,
                videos = thumbnailPrefetchVideos,
                strategy = uiState.thumbnailStrategy,
                quality = uiState.thumbnailQuality,
                allowNetwork = uiState.showNetworkThumbnails,
                fallbackSecond = uiState.thumbnailFallbackSecond
            )
        }
    }

    // Filtered and sorted tree folders
    val displayedTreeFolders = remember(displayedFolders) {
        displayedFolders
    }

    // Flattened list of all videos across library for Library View Mode (excluding blocked & private folders)
    val allLibraryVideos = remember(
        videoFolders.toList(), searchQuery, homeContentSortField, homeContentSortDirection,
        uiState.showNewVideoLabel, uiState.newVideoDaysThreshold,
        blockedVideoFolderIds, privateFolderList,
        seenVersion
    ) {
        val unblockedFolders = videoFolders.filter { folder ->
            !blockedVideoFolderIds.contains(folder.id) &&
            !blockedVideoFolderIds.contains(folder.path) &&
            !PrivateVaultManager.isFolderPrivate(folder.id) &&
            !PrivateVaultManager.isFolderPrivate(folder.path)
        }
        val allVids = mutableListOf<VideoItem>()
        for (f in unblockedFolders) {
            allVids.addAll(f.getAllVideos())
        }
        val distinctList = allVids.distinctBy { it.id }
        val filtered = if (searchQuery.isBlank()) {
            distinctList
        } else {
            distinctList.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }
        val sorted = when (homeContentSortField) {
            SortField.TITLE -> if (homeContentSortDirection == SortDirection.ASCENDING) filtered.sortedWith { a, b -> NaturalOrderComparator.compare(a.displayName, b.displayName) } else filtered.sortedWith { a, b -> NaturalOrderComparator.compare(b.displayName, a.displayName) }
            SortField.DATE -> if (homeContentSortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.dateModified } else filtered.sortedBy { it.dateModified }
            SortField.SIZE -> if (homeContentSortDirection == SortDirection.ASCENDING) filtered.sortedBy { it.sizeBytes } else filtered.sortedByDescending { it.sizeBytes }
            SortField.DURATION -> if (homeContentSortDirection == SortDirection.DESCENDING) filtered.sortedByDescending { it.durationMs } else filtered.sortedBy { it.durationMs }
            SortField.COUNT -> filtered
        }
        sorted.map { video ->
            val isNew = com.example.util.MediaSeenManager.isVideoNew(
                context,
                video,
                uiState.newVideoDaysThreshold,
                uiState.showNewVideoLabel
            )
            video.copy(isNew = isNew)
        }
    }

    // Background metadata enrichment for videos in currently opened folder (authoritative framerate & subtitles)
    LaunchedEffect(currentFolder?.id, displayedVideos.size) {
        val folder = currentFolder ?: return@LaunchedEffect
        val needsEnrichment = folder.videos.any { it.framerate <= 0.0 }
        if (needsEnrichment) {
            withContext(Dispatchers.IO) {
                var modified = false
                val enrichedVideos = folder.videos.map { video ->
                    if (video.framerate <= 0.0) {
                        val meta = try {
                            MediaMetadataExtractor.extractMetadata(
                                filePath = video.path,
                                context = context,
                                uri = video.uri,
                                displayName = video.displayName,
                                sizeBytes = video.sizeBytes,
                                dateModified = video.dateModified
                            )
                        } catch (_: Throwable) { null }
                        if (meta != null && (meta.framerate > 0.0 || meta.effectiveSubtitleFormats.isNotEmpty())) {
                            modified = true
                            video.copy(
                                framerate = if (meta.framerate > 0.0) meta.framerate else video.framerate,
                                resolution = if (meta.resolution.isNotBlank()) meta.resolution else video.resolution,
                                durationMs = if (meta.durationMs > 0L) meta.durationMs else video.durationMs,
                                embeddedSubtitleFormats = meta.embeddedSubtitleFormats,
                                externalSubtitleFormats = meta.externalSubtitleFormats,
                                subtitleFormats = meta.effectiveSubtitleFormats,
                                hasSubtitles = meta.effectiveSubtitleFormats.isNotEmpty()
                            )
                        } else video
                    } else video
                }
                if (modified) {
                    withContext(Dispatchers.Main) {
                        val cur = currentFolder ?: return@withContext
                        val updated = cur.copy(videos = enrichedVideos)
                        val sIdx = folderStack.indexOfFirst { it.id == cur.id }
                        if (sIdx >= 0) folderStack[sIdx] = updated
                        val fIdx = videoFolders.indexOfFirst { it.id == cur.id }
                        if (fIdx >= 0) videoFolders[fIdx] = updated
                    }
                }
            }
        }
    }

    // Auto-scroll to the most recently played video when opening a list.
    // - Uses the last watched video OF THIS FOLDER (the global "last watched" is usually in another folder).
    // - Runs once per folder visit: the list is re-enriched (subtitle info etc.) several times after it
    //   opens, which used to restart the animation and fight the user's own scrolling.
    // - Jumps instantly to a few rows before the target and only glides the last stretch. Animating
    //   through dozens of rows composes every card on the way, which is what made it janky.
    // The key is kept in the ViewModel: coming back from the video player rebuilds this screen, and that must NOT scroll again.
    var autoScrolledKey by remember { mutableStateOf<String?>(viewModel?.homeAutoScrolledKey) }
    var previousScrollFolderId by remember { mutableStateOf(currentFolder?.id) }
    LaunchedEffect(currentFolder?.id, viewMode) {
        // Only an actual "left the folder" step re-arms the auto-scroll, never the first composition after the player.
        if (currentFolder == null && previousScrollFolderId != null) {
            autoScrolledKey = null
            viewModel?.homeAutoScrolledKey = null
        }
        previousScrollFolderId = currentFolder?.id
    }
    // Remember where the lists were left when this screen goes away (video player opens), see HomeViewModel.homeLibraryScrollIndex.
    DisposableEffect(Unit) {
        onDispose {
            if (viewModel != null) {
                viewModel.homeLibraryScrollIndex = libraryListState.firstVisibleItemIndex
                viewModel.homeLibraryScrollOffset = libraryListState.firstVisibleItemScrollOffset
                viewModel.homeFolderScrollFolderId = folderStack.lastOrNull()?.id
                viewModel.homeFolderListScrollIndex = folderContentListState.firstVisibleItemIndex
                viewModel.homeFolderListScrollOffset = folderContentListState.firstVisibleItemScrollOffset
                viewModel.homeFolderGridScrollIndex = folderContentGridState.firstVisibleItemIndex
                viewModel.homeFolderGridScrollOffset = folderContentGridState.firstVisibleItemScrollOffset
            }
        }
    }
    LaunchedEffect(
        uiState.autoScrollToLastPlayed,
        viewMode,
        currentFolder?.id,
        homeContentLayoutMode,
        displayedVideos.size,
        displayedSubFolders.size
    ) {
        if (!uiState.autoScrollToLastPlayed) return@LaunchedEffect
        val scrollKey = if (viewMode == ViewMode.FOLDER && currentFolder != null) "folder_${currentFolder.id}" else "library"
        if (autoScrolledKey == scrollKey) return@LaunchedEffect

        val last = withContext(Dispatchers.Default) {
            if (viewMode == ViewMode.FOLDER && currentFolder != null) {
                com.example.util.PlaybackHistoryManager.getLastWatchedRecordInFolder(context, currentFolder.path)
            } else {
                com.example.util.PlaybackHistoryManager.getLastWatchedRecord(context)
            }
        } ?: return@LaunchedEffect

        val matches = { it: VideoItem ->
            (last.path.isNotBlank() && it.path == last.path) ||
                (last.uriString.isNotBlank() && it.uri.toString() == last.uriString) ||
                (last.path.isBlank() && last.uriString.isBlank() && last.videoId > 0 && it.id == last.videoId)
        }

        if (viewMode == ViewMode.LIBRARY) {
            val index = allLibraryVideos.indexOfFirst(matches)
            if (index < 0) return@LaunchedEffect
            autoScrolledKey = scrollKey
            viewModel?.homeAutoScrolledKey = scrollKey
            withFrameNanos { }
            libraryListState.glideToItem(index)
        } else if (viewMode == ViewMode.FOLDER && currentFolder != null) {
            val videoIndex = displayedVideos.indexOfFirst(matches)
            if (videoIndex < 0) return@LaunchedEffect
            // Item layout of the folder list/grid: [folders header + folders] then [videos header + videos].
            val foldersBlock = if (displayedSubFolders.isNotEmpty()) 1 + displayedSubFolders.size else 0
            val itemIndex = foldersBlock + 1 + videoIndex
            autoScrolledKey = scrollKey
            viewModel?.homeAutoScrolledKey = scrollKey
            withFrameNanos { }
            if (homeContentLayoutMode == LayoutMode.GRID) {
                folderContentGridState.glideToItem(itemIndex)
            } else {
                folderContentListState.glideToItem(itemIndex)
            }
        }
    }

    // Slash Search Mode (prefix '/' to search playlists and albums)
    val isSlashSearchMode = searchQuery.startsWith("/")
    val rawSlashTerm = if (isSlashSearchMode) searchQuery.removePrefix("/").trim() else ""

    // Slash Search Private Vault Authentication (/password answer)
    val isPrivateSearchMatch = remember(rawSlashTerm, isSlashSearchMode) {
        if (!isSlashSearchMode || rawSlashTerm.isBlank()) false
        else {
            if (PrivateVaultManager.isSetup(context) && rawSlashTerm.contains(' ')) {
                val parts = rawSlashTerm.split(" ", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    PrivateVaultManager.authenticateSession(context, parts[0], parts[1])
                } else false
            } else false
        }
    }

    val matchedPrivateFolders = remember(isPrivateSearchMatch, isVaultSessionAuthenticated, privateFolderList) {
        if (isPrivateSearchMatch || isVaultSessionAuthenticated) {
            privateFolderList
        } else emptyList()
    }

    val savedPlaylists = remember(searchQuery, isSearchActive, playlistTriggerRefresh) {
        PlaylistManager.getPlaylists(context)
    }

    val matchedPlaylists = remember(savedPlaylists, rawSlashTerm, isSlashSearchMode) {
        if (!isSlashSearchMode) emptyList()
        else if (rawSlashTerm.isBlank()) savedPlaylists
        else savedPlaylists.filter { it.name.contains(rawSlashTerm, ignoreCase = true) }
    }

    val matchedSlashFolders = remember(videoFolders.toList(), rawSlashTerm, isSlashSearchMode, blockedVideoFolderIds, privateFolderList) {
        if (!isSlashSearchMode) emptyList()
        else {
            val allFolders = mutableListOf<VideoFolder>()
            fun collect(folders: List<VideoFolder>) {
                for (f in folders) {
                    if (!blockedVideoFolderIds.contains(f.id) &&
                        !blockedVideoFolderIds.contains(f.path) &&
                        !PrivateVaultManager.isFolderPrivate(f.id) &&
                        !PrivateVaultManager.isFolderPrivate(f.path)) {
                        allFolders.add(f)
                        collect(f.subFolders)
                    }
                }
            }
            collect(videoFolders.toList())
            val deduplicated = allFolders.distinctBy { it.path }
            if (rawSlashTerm.isBlank()) deduplicated
            else deduplicated.filter {
                it.name.contains(rawSlashTerm, ignoreCase = true) ||
                        it.path.contains(rawSlashTerm, ignoreCase = true)
            }
        }
    }

    // Stable Top Inset computation: protects against notch/camera cutout jumps when transitioning from immersive player
    val currentStatusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val currentCutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val currentTopInset = maxOf(currentStatusBarTop, currentCutoutTop)

    var stableTopInsetFloat by rememberSaveable { mutableStateOf(0f) }
    if (currentTopInset > 0.dp && currentTopInset.value != stableTopInsetFloat) {
        stableTopInsetFloat = currentTopInset.value
    }
    val safeTopPadding = when {
        currentTopInset > 0.dp -> currentTopInset
        stableTopInsetFloat > 0f -> stableTopInsetFloat.dp
        else -> 36.dp
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            // Settings > Appearance > Background: the animation plays behind every folder / video list of Home.
            // With Appearance = System it only plays while "Show over wallpaper" is ON.
            val activeBackdrop = rememberActiveBackground()
            if (activeBackdrop.type != BackgroundType.NONE &&
                (uiState.themeMode != ThemeMode.SYSTEM || activeBackdrop.overWallpaper)
            ) {
                AnimatedBackground(type = activeBackdrop.type, parallax = activeBackdrop.parallax)
            }
            val tabSwipeIndex = activeTabs.indexOf(barSelectedTab)
            val tabSwipeBlocked = currentFolder != null || isOnlineVideoOpen || selectedMusicFolder != null ||
                selectedRecentsFolder != null || selectedPlaylist != null ||
                MediaDetailState.plugin != null || MediaDetailState.provider != null ||
                (effectiveSelectedTab == NavigationTab.NETWORK && showDownloads && !isDownloadsBarTab) ||
                isSelectionMode || isSlashSearchMode
            val switchBottomTab: (NavigationTab) -> Unit = { newTab ->
                if (newTab == NavigationTab.DOWNLOADS) showDownloads = true
                else if (newTab == NavigationTab.NETWORK) showDownloads = false
                currentBottomTab = newTab
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Swipe left / right anywhere on the page to move to the next / previous tab.
                    .swipeBetweenTabs(
                        enabled = activeTabs.size > 1 && !tabSwipeBlocked,
                        canGoNext = tabSwipeIndex in 0 until activeTabs.size - 1,
                        canGoPrevious = tabSwipeIndex > 0,
                        onNext = { activeTabs.getOrNull(tabSwipeIndex + 1)?.let(switchBottomTab) },
                        onPrevious = { activeTabs.getOrNull(tabSwipeIndex - 1)?.let(switchBottomTab) }
                    )
                    .padding(top = safeTopPadding + 8.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Top App Bar (HeaderBar or SelectionTopBar with Liquid Glass)
                if (isHomeSelectionMode) {
                    SelectionTopBar(
                        selectedCount = totalSelectedCount,
                        isFolderSelection = isFolderSelection,
                        isDark = isDark,
                        alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                        onClose = { clearSelection() },
                        onSelectAll = {
                            if (isFolderSelection) {
                                val visibleList = if (currentFolder != null) displayedSubFolders else displayedFolders
                                if (selectedFolderIds.size == visibleList.size) {
                                    selectedFolderIds.clear()
                                } else {
                                    selectedFolderIds.clear()
                                    selectedFolderIds.addAll(visibleList.map { it.id })
                                }
                            } else {
                                val visibleList = if (currentFolder != null) displayedVideos else allLibraryVideos
                                if (selectedVideoPaths.size == visibleList.size) {
                                    selectedVideoPaths.clear()
                                } else {
                                    selectedVideoPaths.clear()
                                    selectedVideoPaths.addAll(visibleList.map { it.path })
                                }
                            }
                        },
                        onShare = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val videosToShare = if (isFolderSelection) {
                                    val foldersToShare = (if (currentFolder != null) displayedSubFolders else displayedFolders)
                                        .filter { selectedFolderIds.contains(it.id) }
                                    foldersToShare.flatMap { it.getAllVideos() }
                                } else {
                                    val all = if (currentFolder != null) displayedVideos else allLibraryVideos
                                    all.filter { selectedVideoPaths.contains(it.path) }
                                }
                                withContext(Dispatchers.Main) {
                                    if (videosToShare.isNotEmpty()) {
                                        VideoFileManager.shareVideos(context, videosToShare)
                                    } else {
                                        Toast.makeText(context, "No videos to share", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onInfo = {
                            if (isFolderSelection) {
                                val selected = (if (currentFolder != null) displayedSubFolders else displayedFolders)
                                    .firstOrNull { selectedFolderIds.contains(it.id) }
                                if (selected != null) {
                                    selectedFolderForInfo = selected
                                    showFolderInfoSheet = true
                                }
                            } else {
                                val selected = (if (currentFolder != null) displayedVideos else allLibraryVideos)
                                    .firstOrNull { selectedVideoPaths.contains(it.path) }
                                if (selected != null) selectedVideoForInfo = selected
                            }
                        }
                    )
                } else if (effectiveSelectedTab == NavigationTab.PLAYLISTS && isPlaylistTabSelectionMode) {
                    SelectionTopBar(
                        selectedCount = playlistSelectionCount,
                        isFolderSelection = playlistSelectionIsFolder,
                        isDark = isDark,
                        alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                        onClose = { onPlaylistClearSelectionCallback?.invoke() },
                        onSelectAll = { onPlaylistSelectAllCallback?.invoke() },
                        onShare = { onPlaylistShareCallback?.invoke() }
                    )
                } else if (effectiveSelectedTab == NavigationTab.MUSIC && isAudioTabSelectionMode) {
                    SelectionTopBar(
                        selectedCount = audioSelectionCount,
                        isFolderSelection = audioSelectionIsFolder,
                        isDark = isDark,
                        alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                        onClose = { onAudioClearSelectionCallback?.invoke() },
                        onSelectAll = { onAudioSelectAllCallback?.invoke() },
                        onShare = { onAudioShareCallback?.invoke() },
                        onInfo = { onAudioInfoCallback?.invoke() }
                    )
                } else if (effectiveSelectedTab == NavigationTab.NETWORK && showDownloads && isDownloadsSelectionMode) {
                    SelectionTopBar(
                        selectedCount = downloadsSelectionCount,
                        isFolderSelection = false,
                        isDark = isDark,
                        alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                        onClose = { onDownloadsClearSelectionCallback?.invoke() },
                        onSelectAll = { onDownloadsSelectAllCallback?.invoke() },
                        onShare = { onDownloadsShareCallback?.invoke() },
                        onInfo = { onDownloadsInfoCallback?.invoke() }
                    )
                } else {
                    val headerTitle = when (effectiveSelectedTab) {
                        NavigationTab.HOME -> if (isOnlineVideoOpen) "Online" else (currentFolder?.name ?: AppStrings.getTitle(lang))
                        NavigationTab.MUSIC -> selectedMusicFolder?.name ?: "Music & Audio"
                        NavigationTab.RECENTS -> selectedRecentsFolder?.name ?: "Recently Played"
                        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> if (showDownloads) "Downloads" else (MediaDetailState.provider?.name ?: "Network")
                        NavigationTab.PLAYLISTS -> selectedPlaylist?.name ?: "Playlists"
                    }
                    val headerSubtitle = when (effectiveSelectedTab) {
                        NavigationTab.HOME -> if (isOnlineVideoOpen) null else currentFolder?.let { folder ->
                            val vCount = folder.videos.size
                            val fCount = folder.subFolders.size
                            listOfNotNull(
                                if (vCount > 0) "$vCount Video${if (vCount > 1) "s" else ""}" else null,
                                if (fCount > 0) "$fCount Folder${if (fCount > 1) "s" else ""}" else null
                            ).joinToString(" • ").ifEmpty { "Empty Folder" }
                        }
                        NavigationTab.MUSIC -> selectedMusicFolder?.let {
                            // The virtual "Online" folder shows no subtitle line at all.
                            if (it.path == com.example.ui.screens.ONLINE_FOLDER_SENTINEL_PATH) {
                                null
                            } else {
                                "${it.trackCount} Tracks • ${it.formattedSize}"
                            }
                        }
                        NavigationTab.RECENTS -> selectedRecentsFolder?.let {
                            "${it.records.size} video${if (it.records.size != 1) "s" else ""} in folder"
                        } ?: "Resume your watch history"
                        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> if (showDownloads) {
                            val openGroup = downloadsOpenFolder
                            val count = com.example.util.DownloadStore.items.value.size
                            if (openGroup != null) {
                                // inside a series folder: "(10 Videos) Folder name", like a folder on the Home screen
                                val inGroup = com.example.util.DownloadStore.items.value.filter { it.group == openGroup }
                                val unit = if (inGroup.isNotEmpty() && inGroup.all { !it.isAudio }) "Video" else "Item"
                                "(${inGroup.size} $unit${if (inGroup.size != 1) "s" else ""}) $openGroup"
                            } else if (count > 0) "$count ${if (count == 1) "Item" else "Items"} • Downloads" else "No downloads"
                        } else if (MediaDetailState.provider != null) null else "Play links from the internet"
                        NavigationTab.PLAYLISTS -> selectedPlaylist?.let {
                            val count = it.videoPaths.size
                            val type = if (it.isAudio) "Audio" else "Video"
                            "$count $type${if (count != 1) "s" else ""} • Created ${it.formattedDate}"
                        } ?: "Your custom media collections"
                    }

                    HeaderBar(
                        title = headerTitle,
                        subtitle = headerSubtitle,
                        // "(Beta)" beside the app name, only on the Home screen itself and only for beta builds (version name contains "beta")
                        titleSuffix = if (effectiveSelectedTab == NavigationTab.HOME && !isOnlineVideoOpen && currentFolder == null &&
                            com.example.BuildConfig.VERSION_NAME.contains("beta", ignoreCase = true)) "(Beta)" else null,
                        onBackClick = when (effectiveSelectedTab) {
                            NavigationTab.HOME -> if (isOnlineVideoOpen) {
                                {
                                    searchQuery = ""
                                    isSearchActive = false
                                    com.example.ui.components.OnlineVideoFeedState.isOpen = false
                                }
                            } else if (currentFolder != null) {
                                {
                                    searchQuery = ""
                                    isSearchActive = false
                                    popFolder()
                                }
                            } else null
                            NavigationTab.MUSIC -> if (selectedMusicFolder != null) {
                                {
                                    searchQuery = ""
                                    isSearchActive = false
                                    selectedMusicFolder = null
                                }
                            } else null
                            NavigationTab.RECENTS -> if (selectedRecentsFolder != null) { { selectedRecentsFolder = null } } else null
                            NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> if (showDownloads && downloadsOpenFolder != null) {
                                { downloadsOpenFolder = null }
                            } else if (showDownloads && !isDownloadsBarTab) {
                                {
                                    searchQuery = ""
                                    isSearchActive = false
                                    showDownloads = false
                                }
                            } else null
                            NavigationTab.PLAYLISTS -> if (selectedPlaylist != null) {
                                {
                                    searchQuery = ""
                                    isSearchActive = false
                                    selectedPlaylist = null
                                }
                            } else null
                        },
                        themeMode = uiState.themeMode,
                        onSearchClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        onSortClick = if (effectiveSelectedTab == NavigationTab.NETWORK && !showDownloads) null else { { showSortAndViewDialog = true } },
                        onDownloadClick = if (effectiveSelectedTab == NavigationTab.NETWORK && !showDownloads) {
                            {
                                searchQuery = ""
                                isSearchActive = false
                                showDownloads = true
                            }
                        } else null,
                        // a media provider page shows the provider's own icon in place of the app icon
                        leadingIconUrl = if (effectiveSelectedTab == NavigationTab.NETWORK && !showDownloads) MediaDetailState.provider?.icon else null,
                        onSettingsClick = { showSettingsSheet = true }
                    )
                }

                // Expandable Sleek Search Bar for all tabs (Smooth downward spring animation & slim low-profile layout)
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        expandFrom = Alignment.Top
                    ) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { -it } + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(180, easing = FastOutLinearInEasing),
                        shrinkTowards = Alignment.Top
                    ) + slideOutVertically(
                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                    ) { -it } + fadeOut(
                        animationSpec = tween(150)
                    )
                ) {
                    val searchPlaceholder = when (effectiveSelectedTab) {
                        NavigationTab.HOME -> if (isOnlineVideoOpen) "Search videos, channels online..." else if (currentFolder != null) "Search in ${currentFolder.name}..." else "Search videos, or /playlist (e.g. /Op Anime)..."
                        NavigationTab.MUSIC -> if (selectedMusicFolder?.path == com.example.ui.screens.ONLINE_FOLDER_SENTINEL_PATH) "Search songs, albums, artists online..." else "Search songs, albums, artists..."
                        NavigationTab.RECENTS -> "Search watch history..."
                        NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> if (showDownloads) "Search downloads..." else if (MediaDetailState.plugin != null) "Search episodes or movies in this title..." else if (MediaDetailState.provider != null) "Search movies & series in ${MediaDetailState.provider?.name}..." else "Enter stream URL..."
                        NavigationTab.PLAYLISTS -> selectedPlaylist?.let { "Search in ${it.name}..." } ?: "Search playlists..."
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                            .liquidSearchSurface(RoundedCornerShape(18.dp), isDark, surfaceColor, borderColor)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StyledIcon(
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
                                        text = searchPlaceholder,
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
                                    StyledIcon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear",
                                        tint = primaryText.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                when (effectiveSelectedTab) {
                    NavigationTab.MUSIC -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        val isInsideOnlineAudio = (selectedMusicFolder?.path == com.example.ui.screens.ONLINE_FOLDER_SENTINEL_PATH)
                        AudioMusicTabContent(
                            isDark = isDark,
                            searchQuery = searchQuery,
                            sortField = if (isInsideOnlineAudio) onlineAudioSortField else activeSortField,
                            sortDirection = if (isInsideOnlineAudio) onlineAudioSortDirection else activeSortDirection,
                            viewMode = activeViewMode,
                            layoutMode = if (isInsideOnlineAudio) onlineAudioLayoutMode else activeLayoutMode,
                            visibleFields = if (isInsideOnlineAudio) onlineAudioVisibleFields else effectiveVisibleFields,
                            folderSortField = musicFolderSortField,
                            folderSortDirection = musicFolderSortDirection,
                            folderLayoutMode = musicFolderLayoutMode,
                            folderVisibleFields = musicFolderVisibleFields.copy(
                                showThumbnails = uiState.showVideoThumbnails,
                                showFullName = uiState.showFullName,
                                showNewBadge = uiState.showNewVideoLabel
                            ),
                            contentSortField = if (isInsideOnlineAudio) onlineAudioSortField else musicContentSortField,
                            contentSortDirection = if (isInsideOnlineAudio) onlineAudioSortDirection else musicContentSortDirection,
                            contentLayoutMode = if (isInsideOnlineAudio) onlineAudioLayoutMode else musicContentLayoutMode,
                            contentVisibleFields = if (isInsideOnlineAudio) onlineAudioVisibleFields else musicContentVisibleFields.copy(
                                showThumbnails = uiState.showVideoThumbnails,
                                showFullName = uiState.showFullName,
                                showNewBadge = uiState.showNewVideoLabel
                            ),
                            selectedFolder = selectedMusicFolder,
                            onSelectFolder = { folder ->
                                selectedMusicFolder = folder
                                isInsideMusicFolder = (folder != null)
                            },
                            onFolderStateChange = { isInside ->
                                isInsideMusicFolder = isInside
                            },
                            onSelectionModeChange = { inSelection, count, isFolder, onClear, onSelectAll, onShare ->
                                isAudioTabSelectionMode = inSelection
                                audioSelectionCount = count
                                audioSelectionIsFolder = isFolder
                                onAudioClearSelectionCallback = onClear
                                onAudioSelectAllCallback = onSelectAll
                                onAudioShareCallback = onShare
                            },
                            onInfoAction = { action -> onAudioInfoCallback = action },
                            onInfo = { folder, track ->
                                selectedAudioFolderForInfo = folder
                                selectedAudioForInfo = track
                                showAudioFolderInfoSheet = folder != null
                            },
                            onOpenChannel = { channel ->
                                viewModel?.openChannel(channel, isAudioMode = true)
                            },
                            onOpenSharedPlaylist = { playlist ->
                                viewModel?.openSharedPlaylist(playlist)
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                    NavigationTab.RECENTS -> {
                        RecentlyPlayedTabContent(
                            uiState = uiState,
                            isDark = isDark,
                            allLibraryVideos = allLibraryVideos,
                            searchQuery = searchQuery,
                            sortField = activeSortField,
                            sortDirection = activeSortDirection,
                            viewMode = activeViewMode,
                            layoutMode = activeLayoutMode,
                            visibleFields = effectiveVisibleFields,
                            folderSortField = recentsFolderSortField,
                            folderSortDirection = recentsFolderSortDirection,
                            folderLayoutMode = recentsFolderLayoutMode,
                            folderVisibleFields = recentsFolderVisibleFields.copy(
                                showThumbnails = uiState.showVideoThumbnails,
                                showFullName = uiState.showFullName,
                                showNewBadge = uiState.showNewVideoLabel
                            ),
                            contentSortField = recentsContentSortField,
                            contentSortDirection = recentsContentSortDirection,
                            contentLayoutMode = recentsContentLayoutMode,
                            contentVisibleFields = recentsContentVisibleFields.copy(
                                showThumbnails = uiState.showVideoThumbnails,
                                showFullName = uiState.showFullName,
                                showNewBadge = uiState.showNewVideoLabel
                            ),
                            selectedFolder = selectedRecentsFolder,
                            onSelectFolder = { folder ->
                                selectedRecentsFolder = folder
                                isInsideRecentsFolder = (folder != null)
                            },
                            onFolderStateChange = { isInside ->
                                isInsideRecentsFolder = isInside
                            },
                            onPlayVideo = onPlayVideo,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                    NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> {
                        if (showDownloads) {
                            DownloadsTabContent(
                                isDark = isDark,
                                searchQuery = searchQuery,
                                sortField = downloadsSortField,
                                sortDirection = downloadsSortDirection,
                                layoutMode = downloadsLayoutMode,
                                visibleFields = downloadsVisibleFields,
                                thumbnailStrategy = uiState.thumbnailStrategy,
                                thumbnailQuality = uiState.thumbnailQuality,
                                thumbnailFallbackSecond = uiState.thumbnailFallbackSecond,
                                showNetworkThumbnails = uiState.showNetworkThumbnails,
                                onPlayVideo = onPlayVideo,
                                uiState = uiState,
                                onShowMediaInfo = { selectedVideoForInfo = it },
                                openFolder = downloadsOpenFolder,
                                onOpenFolderChange = { downloadsOpenFolder = it },
                                onSelectionModeChange = { inSelection, count, onClear, onSelectAll, onShare, onInfo ->
                                    isDownloadsSelectionMode = inSelection
                                    downloadsSelectionCount = count
                                    onDownloadsClearSelectionCallback = onClear
                                    onDownloadsSelectAllCallback = onSelectAll
                                    onDownloadsShareCallback = onShare
                                    onDownloadsInfoCallback = onInfo
                                },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        } else {
                            NetworkTabContent(
                                isDark = isDark,
                                onPlayVideo = onPlayVideo,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                glassBlurTransparency = uiState.glassBlurTransparency,
                                searchQuery = if (MediaDetailState.plugin != null || MediaDetailState.provider != null) searchQuery else "",
                                onOpenDownloads = {
                                    searchQuery = ""
                                    isSearchActive = false
                                    showDownloads = true
                                }
                            )
                        }
                    }
                    NavigationTab.PLAYLISTS -> {
                        val plOnlineInside = isInsidePlaylistDetail && selectedPlaylist?.isOnlineFolder == true
                        val plOnlineAudio = plOnlineInside && selectedPlaylist?.isAudio == true
                        val plOnlineVideo = plOnlineInside && selectedPlaylist?.isAudio != true
                        CustomPlaylistsTabContent(
                            uiState = uiState,
                            isDark = isDark,
                            allLibraryVideos = allLibraryVideos,
                            searchQuery = searchQuery,
                            sortField = activeSortField,
                            sortDirection = activeSortDirection,
                            viewMode = activeViewMode,
                            layoutMode = activeLayoutMode,
                            visibleFields = effectiveVisibleFields,
                            folderSortField = playlistsFolderSortField,
                            folderSortDirection = playlistsFolderSortDirection,
                            folderLayoutMode = playlistsFolderLayoutMode,
                            folderVisibleFields = playlistsFolderVisibleFields.copy(
                                showThumbnails = uiState.showVideoThumbnails,
                                showFullName = uiState.showFullName,
                                showNewBadge = uiState.showNewVideoLabel
                            ),
                            contentSortField = if (plOnlineVideo) onlinePlVideoSortField else if (plOnlineAudio) onlinePlAudioSortField else playlistsContentSortField,
                            contentSortDirection = if (plOnlineVideo) onlinePlVideoSortDirection else if (plOnlineAudio) onlinePlAudioSortDirection else playlistsContentSortDirection,
                            contentLayoutMode = if (plOnlineVideo) onlinePlVideoLayoutMode else if (plOnlineAudio) onlinePlAudioLayoutMode else playlistsContentLayoutMode,
                            contentVisibleFields = if (plOnlineVideo) onlinePlVideoVisibleFields.copy(showThumbnails = uiState.showVideoThumbnails)
                            else if (plOnlineAudio) onlinePlAudioVisibleFields.copy(showThumbnails = uiState.showVideoThumbnails)
                            else playlistsContentVisibleFields.copy(
                                showThumbnails = uiState.showVideoThumbnails,
                                showFullName = uiState.showFullName,
                                showNewBadge = uiState.showNewVideoLabel
                            ),
                            selectedPlaylist = selectedPlaylist,
                            onSelectPlaylist = { pl ->
                                selectedPlaylist = pl
                                isInsidePlaylistDetail = (pl != null)
                            },
                            onPlaylistSelected = { isSelected ->
                                isInsidePlaylistDetail = isSelected
                            },
                            onSelectionModeChange = { inSelection ->
                                isPlaylistTabSelectionMode = inSelection
                            },
                            onSelectionDetailsChange = { count, isFolder, onClear, onSelectAll, onShare ->
                                playlistSelectionCount = count
                                playlistSelectionIsFolder = isFolder
                                onPlaylistClearSelectionCallback = onClear
                                onPlaylistSelectAllCallback = onSelectAll
                                onPlaylistShareCallback = onShare
                            },
                            onPlayVideo = onPlayVideo,
                            createPlaylistRequest = playlistCreateRequest,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                    NavigationTab.HOME -> {
                        Spacer(modifier = Modifier.height(4.dp))

                // Permission Warning Banner if permission is not granted (Liquid Glass Frosted style)
                if (!hasPermission) {
                    val bannerAlphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)
                    val bannerBg = if (isDark) {
                        if (bannerAlphaRatio >= 0.99f) Neutral.D1E293B
                        else Neutral.D1E293B.copy(alpha = (0.35f + 0.65f * bannerAlphaRatio).coerceIn(0.20f, 1.0f))
                    } else {
                        if (bannerAlphaRatio >= 0.99f) Color(0xFFEFF6FF)
                        else Color(0xFFEFF6FF).copy(alpha = (0.45f + 0.55f * bannerAlphaRatio).coerceIn(0.25f, 1.0f))
                    }
                    val bannerBorder = if (isDark) {
                        AccentSkyBlue.copy(alpha = (0.25f + 0.45f * bannerAlphaRatio).coerceIn(0.20f, 0.70f))
                    } else {
                        AccentSkyBlueStrong.copy(alpha = (0.20f + 0.40f * bannerAlphaRatio).coerceIn(0.15f, 0.60f))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.20f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(bannerBg)
                            .border(1.dp, bannerBorder, RoundedCornerShape(16.dp))
                            .clickable { showPermissionDialog = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentSkyBlueStrong.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                drawableRes = R.drawable.viaura_winrar,
                                    contentDescription = null,
                                    tint = AccentSkyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Storage access is restricted",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                                Text(
                                    text = "Tap to grant permission & scan all device video albums",
                                    fontSize = 11.5.sp,
                                    color = secondaryText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentGradient)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Grant",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 5. Main Content Area with Browser-like Liquid Pull-to-Refresh
                // an open provider page (Network > Media) refreshes its own categories on pull-down, not the folder list
                val providerPageOpen = effectiveSelectedTab == NavigationTab.NETWORK && !showDownloads &&
                    MediaDetailState.provider != null && MediaDetailState.plugin == null
                // an open title (movie / series page inside a plugin) refreshes its own details + episodes on pull-down
                val titlePageOpen = effectiveSelectedTab == NavigationTab.NETWORK && !showDownloads &&
                    MediaDetailState.plugin != null
                // the Online Audio folder is open: pulling down from the top gives a brand-new feed
                val isOnlineAudioOpen = effectiveSelectedTab == NavigationTab.MUSIC &&
                    selectedMusicFolder?.path == com.example.ui.screens.ONLINE_FOLDER_SENTINEL_PATH
                LiquidPullToRefresh(
                    isRefreshing = if (isOnlineVideoOpen) com.example.ui.components.OnlineVideoFeedState.isRefreshing
                    else if (isOnlineAudioOpen) com.example.ui.components.OnlineAudioFeedState.isRefreshing
                    else if (providerPageOpen || titlePageOpen) MediaDetailState.providerRefreshing
                    else isManualRefreshing,
                    onRefresh = {
                        if (isOnlineVideoOpen) {
                            com.example.ui.components.OnlineVideoFeedState.isRefreshing = true
                            com.example.ui.components.OnlineVideoFeedState.refreshRequest++
                        } else if (isOnlineAudioOpen) {
                            com.example.ui.components.OnlineAudioFeedState.isRefreshing = true
                            com.example.ui.components.OnlineAudioFeedState.refreshRequest++
                        } else if (providerPageOpen || titlePageOpen) {
                            MediaDetailState.providerRefreshing = true
                            MediaDetailState.refreshRequest++
                        } else reloadFolders(true)
                    },
                    // a half-finished pull of one page must never stay on the next page (online videos <-> folders <-> plugin pages)
                    resetKey = listOf(isOnlineVideoOpen, isOnlineAudioOpen, providerPageOpen, titlePageOpen, effectiveSelectedTab),
                    isDark = isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isOnlineVideoOpen && effectiveSelectedTab == NavigationTab.HOME) {
                        com.example.ui.components.OnlineVideoPanel(
                            isDark = isDark,
                            searchQuery = searchQuery,
                            layoutMode = onlineVideoLayoutMode,
                            visibleFields = onlineVideoVisibleFields,
                            sortField = onlineVideoSortField,
                            sortDirection = onlineVideoSortDirection,
                            onPlayVideo = { video, list -> onPlayVideo(video, list) },
                            onOpenChannel = { ch -> viewModel?.openChannel(ch, isAudioMode = false) },
                            glassBlurTransparency = uiState.glassBlurTransparency
                        )
                    } else if (isSlashSearchMode) {
                        SlashPlaylistFinderContent(
                            searchQuery = searchQuery,
                            rawSlashTerm = rawSlashTerm,
                            matchedPlaylists = matchedPlaylists,
                            matchedFolders = matchedSlashFolders,
                            matchedPrivateFolders = matchedPrivateFolders,
                            allVideos = allLibraryVideos,
                            isDark = isDark,
                            onOpenPlaylist = { playlist ->
                                val plVideos = PlaylistManager.getVideosForPlaylist(playlist, allLibraryVideos)
                                val plFolder = VideoFolder(
                                    id = "playlist_${playlist.id}",
                                    name = playlist.name,
                                    path = "/Playlists/${playlist.name}",
                                    videoCount = plVideos.size,
                                    totalSizeBytes = plVideos.sumOf { it.sizeBytes },
                                    newVideosCount = 0,
                                    isCustom = true,
                                    videos = plVideos
                                )
                                pushFolder(plFolder)
                                searchQuery = ""
                                isSearchActive = false
                            },
                            onPlaylistLongClick = { playlist ->
                                currentBottomTab = NavigationTab.PLAYLISTS
                                selectedPlaylist = playlist
                                isInsidePlaylistDetail = true
                                searchQuery = ""
                                isSearchActive = false
                            },
                            onOpenFolder = { folder ->
                                pushFolder(folder)
                                searchQuery = ""
                                isSearchActive = false
                            },
                            onOpenPrivateFolder = { privFolder ->
                                if (privFolder.mediaType == PrivateMediaType.VIDEO) {
                                    val privVideos = PrivateVaultManager.getPrivateFolderVideos(context, privFolder)
                                    val privVFolder = VideoFolder(
                                        id = "private_${privFolder.id}",
                                        name = privFolder.name,
                                        path = "/Private/${privFolder.name}",
                                        videoCount = privVideos.size,
                                        totalSizeBytes = privFolder.totalSizeBytes,
                                        newVideosCount = 0,
                                        isCustom = true,
                                        videos = privVideos
                                    )
                                    pushFolder(privVFolder)
                                    searchQuery = ""
                                    isSearchActive = false
                                } else {
                                    Toast.makeText(context, "Audio vault folder: ${privFolder.name} (${privFolder.itemCount} tracks)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCreatePlaylist = { plName ->
                                val newPl = PlaylistManager.createPlaylist(context, plName)
                                val plFolder = VideoFolder(
                                    id = "playlist_${newPl.id}",
                                    name = newPl.name,
                                    path = "/Playlists/${newPl.name}",
                                    videoCount = 0,
                                    totalSizeBytes = 0L,
                                    newVideosCount = 0,
                                    isCustom = true,
                                    videos = emptyList()
                                )
                                pushFolder(plFolder)
                                searchQuery = ""
                                isSearchActive = false
                            }
                        )
                    } else if (currentFolder == null) {
                        when (viewMode) {
                            ViewMode.TREE -> {
                                // TREE VIEW: Filtered to show main folders containing multiple subfolders (e.g. Movies)
                                if (displayedTreeFolders.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StyledIcon(
                                imageVector = Icons.Outlined.AccountTree,
                                                    contentDescription = null,
                                                    tint = AccentSkyBlue,
                                                    modifier = Modifier.size(38.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "No matching tree folders found" else "No multi-folder structures found",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Tree view displays main folders with subfolders (like Movies).",
                                                fontSize = 12.sp,
                                                color = secondaryText,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = displayedTreeFolders,
                                            key = { "tree_${it.id}" },
                                            contentType = { "tree_card" }
                                        ) { treeFolder ->
                                            com.example.ui.components.DeleteAnimatedBox(
                                                key = treeFolder.id,
                                                modifier = Modifier.animateItem()
                                            ) {
                                                TreeFolderCard(
                                                    folder = treeFolder,
                                                    isDark = isDark,
                                                    visibleFields = effectiveHomeFolderVisibleFields,
                                                    onOpenFolder = { pushFolder(it) },
                                                    onOpenSubFolder = { pushFolder(it) },
                                                    onPlayVideo = { video ->
                                                        onPlayVideo(video, treeFolder.getAllVideos())
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            ViewMode.LIBRARY -> {
                                // LIBRARY VIEW: Unified all videos view
                                if (allLibraryVideos.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                                                    contentDescription = null,
                                                    tint = AccentSkyBlue,
                                                    modifier = Modifier.size(38.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "No matching videos found" else "No videos found in library",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else if (homeContentLayoutMode == LayoutMode.GRID) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = allLibraryVideos,
                                            key = { "lib_grid_${it.id}_${it.path}" },
                                            contentType = { "video_grid_card" }
                                        ) { video ->
                                            com.example.ui.components.DeleteAnimatedBox(
                                                key = video.path,
                                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                            ) {
                                                VideoItemGridCard(
                                                    video = video,
                                                    isDark = isDark,
                                                    visibleFields = effectiveHomeContentVisibleFields,
                                                    isSelected = selectedVideoPaths.contains(video.path),
                                                    isLastPlayed = isLastWatched(video),
                                                    isRenaming = renamingVideoPath == video.path,
                                                    onConfirmRename = { newName -> handleVideoRename(video, newName) },
                                                    onCancelRename = { renamingVideoPath = null },
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            toggleVideoSelection(video)
                                                        } else {
                                                            onPlayVideo(video, allLibraryVideos)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleVideoSelection(video)
                                                    },
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = libraryListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = allLibraryVideos,
                                            key = { "lib_list_${it.id}_${it.path}" },
                                            contentType = { "video_card" }
                                        ) { video ->
                                            com.example.ui.components.DeleteAnimatedBox(
                                                key = video.path,
                                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                            ) {
                                                VideoItemCard(
                                                    video = video,
                                                    isDark = isDark,
                                                    visibleFields = effectiveHomeContentVisibleFields,
                                                    isHighlighted = false,
                                                    isSelected = selectedVideoPaths.contains(video.path),
                                                    isLastPlayed = isLastWatched(video),
                                                    isRenaming = renamingVideoPath == video.path,
                                                    onConfirmRename = { newName -> handleVideoRename(video, newName) },
                                                    onCancelRename = { renamingVideoPath = null },
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            toggleVideoSelection(video)
                                                        } else {
                                                            onPlayVideo(video, allLibraryVideos)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleVideoSelection(video)
                                                    },
                                                    thumbnailStrategy = uiState.thumbnailStrategy,
                                                    thumbnailQuality = uiState.thumbnailQuality,
                                                    thumbnailFallbackSecond = uiState.thumbnailFallbackSecond,
                                                    showNetworkThumbnails = uiState.showNetworkThumbnails,
                                                    onThumbnailClick = if (uiState.tapThumbnailToSelect) { { toggleVideoSelection(video) } } else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            ViewMode.FOLDER -> {
                                // FOLDER VIEW: Standard folder list/grid
                                if (displayedFolders.isEmpty()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (searchQuery.isBlank()) {
                                            item(contentType = "online_video_folder") {
                                                OnlineFolderCard(
                                                    isDark = isDark,
                                                    title = "Online Video",
                                                    subtitle = "Search & stream any video online",
                                                    onClick = { com.example.ui.components.OnlineVideoFeedState.isOpen = true }
                                                )
                                            }
                                        }
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.padding(24.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(72.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        StyledIcon(
                                                imageVector = Icons.Outlined.Folder,
                                                            contentDescription = null,
                                                            tint = AccentSkyBlue,
                                                            modifier = Modifier.size(38.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(14.dp))
                                                    Text(
                                                        text = if (searchQuery.isNotBlank()) "No matching folders found" else if (!hasPermission) "Media permission required" else "No video folders found",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = primaryText,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = if (searchQuery.isNotBlank()) "Try searching for a different folder name" else if (!hasPermission) "Grant media permission to automatically detect and browse your device's video library" else "Swipe down to refresh your video library",
                                                        fontSize = 12.sp,
                                                        color = secondaryText,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    if (!hasPermission) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(14.dp))
                                                                .background(AccentGradient)
                                                                .clickable { showPermissionDialog = true }
                                                                .padding(horizontal = 20.dp, vertical = 10.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "Grant Media Access",
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (homeFolderLayoutMode == LayoutMode.GRID) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (searchQuery.isBlank()) {
                                            item(span = { GridItemSpan(2) }, contentType = "online_video_folder") {
                                                OnlineFolderCard(
                                                    isDark = isDark,
                                                    title = "Online Video",
                                                    subtitle = "Search & stream any video online",
                                                    onClick = { com.example.ui.components.OnlineVideoFeedState.isOpen = true }
                                                )
                                            }
                                        }
                                        items(
                                            items = displayedFolders,
                                            key = { "f_grid_${it.id}" },
                                            contentType = { "folder_grid_card" }
                                        ) { folder ->
                                            com.example.ui.components.DeleteAnimatedBox(
                                                key = folder.id,
                                                modifier = Modifier.animateItem()
                                            ) {
                                                VideoFolderGridCard(
                                                    folder = folder,
                                                    isDark = isDark,
                                                    visibleFields = effectiveHomeFolderVisibleFields,
                                                    isSelected = selectedFolderIds.contains(folder.id),
                                                    isRenaming = renamingFolderId == folder.id,
                                                    onConfirmRename = { newName -> handleFolderRename(folder, newName) },
                                                    onCancelRename = { renamingFolderId = null },
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            toggleFolderSelection(folder)
                                                        } else {
                                                            pushFolder(folder)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleFolderSelection(folder)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (searchQuery.isBlank()) {
                                            item(contentType = "online_video_folder") {
                                                OnlineFolderCard(
                                                    isDark = isDark,
                                                    title = "Online Video",
                                                    subtitle = "Search & stream any video online",
                                                    onClick = { com.example.ui.components.OnlineVideoFeedState.isOpen = true }
                                                )
                                            }
                                        }
                                        items(
                                            items = displayedFolders,
                                            key = { "f_list_${it.id}" },
                                            contentType = { "folder_card" }
                                        ) { folder ->
                                            com.example.ui.components.DeleteAnimatedBox(
                                                key = folder.id,
                                                modifier = Modifier.animateItem()
                                            ) {
                                                VideoFolderCard(
                                                    folder = folder,
                                                    isDark = isDark,
                                                    visibleFields = effectiveHomeFolderVisibleFields,
                                                    isSelected = selectedFolderIds.contains(folder.id),
                                                    isRenaming = renamingFolderId == folder.id,
                                                    onConfirmRename = { newName -> handleFolderRename(folder, newName) },
                                                    onCancelRename = { renamingFolderId = null },
                                                    onClick = {
                                                        if (isSelectionMode) {
                                                            toggleFolderSelection(folder)
                                                        } else {
                                                            pushFolder(folder)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleFolderSelection(folder)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // INSIDE FOLDER VIEW: Subfolders + Videos
                        val isFolderEmpty = displayedSubFolders.isEmpty() && displayedVideos.isEmpty()

                        if (isFolderEmpty) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StyledIcon(
                                imageVector = Icons.Outlined.Folder,
                                            contentDescription = null,
                                            tint = AccentSkyBlue,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No matching items in this folder" else "This folder is empty",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Try changing your search query" else "No video files or subfolders were found in ${currentFolder.name}",
                                        fontSize = 12.sp,
                                        color = secondaryText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (homeContentLayoutMode == LayoutMode.GRID) {
                            LazyVerticalGrid(
                                state = folderContentGridState,
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Subfolders Section
                                if (displayedSubFolders.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, contentType = "section_header") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                        ) {
                                            StyledIcon(
                                imageVector = Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = AccentMid,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "FOLDERS (${displayedSubFolders.size})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                    items(
                                        items = displayedSubFolders,
                                        key = { "f_grid_sub_${it.id}" },
                                        contentType = { "folder_grid_card" }
                                    ) { subFolder ->
                                        com.example.ui.components.DeleteAnimatedBox(
                                            key = subFolder.id,
                                            modifier = Modifier.animateItem()
                                        ) {
                                            VideoFolderGridCard(
                                                folder = subFolder,
                                                isDark = isDark,
                                                visibleFields = effectiveHomeFolderVisibleFields,
                                                isSelected = selectedFolderIds.contains(subFolder.id),
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        toggleFolderSelection(subFolder)
                                                    } else {
                                                        pushFolder(subFolder)
                                                    }
                                                },
                                                onLongClick = {
                                                    toggleFolderSelection(subFolder)
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. Videos Section
                                if (displayedVideos.isNotEmpty()) {
                                    item(span = { GridItemSpan(2) }, contentType = "section_header") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        ) {
                                            StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                                                contentDescription = null,
                                                tint = AccentSkyBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "VIDEOS (${displayedVideos.size})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                    items(
                                        items = displayedVideos,
                                        key = { "f_grid_vid_${it.id}_${it.path}" },
                                        contentType = { "video_grid_card" }
                                    ) { video ->
                                        com.example.ui.components.DeleteAnimatedBox(
                                            key = video.path,
                                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                        ) {
                                            VideoItemGridCard(
                                                video = video,
                                                isDark = isDark,
                                                visibleFields = effectiveHomeContentVisibleFields,
                                                isSelected = selectedVideoPaths.contains(video.path),
                                                    isLastPlayed = isLastWatched(video),
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        toggleVideoSelection(video)
                                                    } else {
                                                        onPlayVideo(video, displayedVideos)
                                                    }
                                                },
                                                onLongClick = {
                                                    toggleVideoSelection(video)
                                                },
                                                thumbnailStrategy = uiState.thumbnailStrategy,
                                                thumbnailQuality = uiState.thumbnailQuality,
                                                showNetworkThumbnails = uiState.showNetworkThumbnails,
                                                onThumbnailClick = if (uiState.tapThumbnailToSelect) { { toggleVideoSelection(video) } } else null
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = folderContentListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Subfolders Section
                                if (displayedSubFolders.isNotEmpty()) {
                                    item(contentType = "section_header") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                        ) {
                                            StyledIcon(
                                imageVector = Icons.Outlined.Folder,
                                                contentDescription = null,
                                                tint = AccentMid,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "FOLDERS (${displayedSubFolders.size})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                    items(
                                        items = displayedSubFolders,
                                        key = { "f_sub_${it.id}" },
                                        contentType = { "folder_card" }
                                    ) { subFolder ->
                                        com.example.ui.components.DeleteAnimatedBox(
                                            key = subFolder.id,
                                            modifier = Modifier.animateItem()
                                        ) {
                                            VideoFolderCard(
                                                folder = subFolder,
                                                isDark = isDark,
                                                visibleFields = effectiveHomeFolderVisibleFields,
                                                isSelected = selectedFolderIds.contains(subFolder.id),
                                                isRenaming = renamingFolderId == subFolder.id,
                                                onConfirmRename = { newName -> handleFolderRename(subFolder, newName) },
                                                onCancelRename = { renamingFolderId = null },
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        toggleFolderSelection(subFolder)
                                                    } else {
                                                        pushFolder(subFolder)
                                                    }
                                                },
                                                onLongClick = {
                                                    toggleFolderSelection(subFolder)
                                                }
                                            )
                                        }
                                    }
                                }

                                // 2. Videos Section (matching reference design: Red NEW Badge, Duration, Title, Size Pill)
                                if (displayedVideos.isNotEmpty()) {
                                    item(contentType = "section_header") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                        ) {
                                            StyledIcon(
                                 imageVector = Icons.Outlined.Movie,
                                                contentDescription = null,
                                                tint = AccentSkyBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "VIDEOS (${displayedVideos.size})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                    items(
                                        items = displayedVideos,
                                        key = { "f_vid_${it.id}_${it.path}" },
                                        contentType = { "video_card" }
                                    ) { video ->
                                        com.example.ui.components.DeleteAnimatedBox(
                                            key = video.path,
                                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                                        ) {
                                            VideoItemCard(
                                                video = video,
                                                isDark = isDark,
                                                visibleFields = effectiveHomeContentVisibleFields,
                                                isHighlighted = false,
                                                isSelected = selectedVideoPaths.contains(video.path),
                                                    isLastPlayed = isLastWatched(video),
                                                isRenaming = renamingVideoPath == video.path,
                                                onConfirmRename = { newName -> handleVideoRename(video, newName) },
                                                onCancelRename = { renamingVideoPath = null },
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        toggleVideoSelection(video)
                                                    } else {
                                                        onPlayVideo(video, displayedVideos)
                                                    }
                                                },
                                                onLongClick = {
                                                    toggleVideoSelection(video)
                                                },
                                                thumbnailStrategy = uiState.thumbnailStrategy,
                                                thumbnailQuality = uiState.thumbnailQuality,
                                                thumbnailFallbackSecond = uiState.thumbnailFallbackSecond,
                                                showNetworkThumbnails = uiState.showNetworkThumbnails,
                                                onThumbnailClick = if (uiState.tapThumbnailToSelect) { { toggleVideoSelection(video) } } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

            // 5. Recent/resume floating action buttons.
            // HOME: resume the last video globally, or the last video belonging to the
            // folder currently open. MUSIC: use the same rule for audio tracks.
            val currentAudioPlaying by AudioPlaybackManager.currentTrack.collectAsState()
            val miniVideoSession by com.example.player.VideoMiniPlayerManager.session.collectAsState()
            val miniActive = currentAudioPlaying != null || miniVideoSession != null
            val fabBottomPadding = when {
                isSelectionMode -> 112.dp
                activeTabs.isNotEmpty() -> if (miniActive) 144.dp else 84.dp
                else -> if (miniActive) 86.dp else 22.dp
            }

            AnimatedVisibility(
                visible = !isSelectionMode && effectiveSelectedTab == NavigationTab.HOME,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = fabBottomPadding)
            ) {
                val fabShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        // Liquid Glass: translucent frosted button; OFF = the original solid button
                        .liquidGlassOr(fabShape, isDark, elevation = 8.dp, backdropBlur = false) {
                            this
                            .shadow(
                            elevation = 18.dp,
                            shape = fabShape,
                            ambientColor = com.example.ui.theme.AccentSkyBlue.copy(alpha = 0.50f),
                            spotColor = com.example.ui.theme.AccentPink.copy(alpha = 0.60f)
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
                        }
                        .bounceClick(scaleDown = 0.88f) {
                            // Resume = the video that was really watched LAST in this folder, from the exact
                            // second it was left at (the player restores the saved position by itself).
                            val source = if (currentFolder != null) currentFolder.getAllVideos() else allLibraryVideos
                            var targetVideo: VideoItem? = null
                            // Most recent history record that still maps to an existing video of this folder
                            // (id / path / uri only - matching by title could pick a same-named video of
                            // another episode or sub-folder).
                            val inFolderRecords = com.example.util.PlaybackHistoryManager.getAllPlaybackRecords(context)
                            for (rec in inFolderRecords) {
                                // Strongest identifier first (path, then uri); the numeric id is only a last
                                // resort for records that have neither, because ids can collide and would
                                // otherwise resolve to a different (earlier) episode.
                                val found = (if (rec.path.isNotBlank()) source.firstOrNull { it.path == rec.path } else null)
                                    ?: (if (rec.uriString.isNotBlank()) source.firstOrNull { it.uri.toString() == rec.uriString } else null)
                                    ?: (if (rec.path.isBlank() && rec.uriString.isBlank() && rec.videoId > 0) source.firstOrNull { it.id == rec.videoId } else null)
                                if (found != null) { targetVideo = found; break }
                            }
                            if (targetVideo == null && currentFolder == null) {
                                // Library level: keep the old behaviour (video that may live outside the scanned list).
                                targetVideo = com.example.util.PlaybackHistoryManager.getLastWatchedRecord(context)?.let { record ->
                                    if (record.path.isNotBlank()) VideoItem(
                                        id = if (record.videoId > 0) record.videoId else 1L,
                                        uri = if (record.uriString.isNotBlank()) runCatching { Uri.parse(record.uriString) }.getOrDefault(Uri.EMPTY) else Uri.EMPTY,
                                        displayName = record.title.ifBlank { record.path.substringAfterLast('/').ifBlank { "Recent Video" } },
                                        path = record.path,
                                        sizeBytes = 0L,
                                        durationMs = record.durationMs,
                                        dateModified = record.lastWatchedTimestamp / 1000L,
                                        isNew = false
                                    ) else null
                                }
                            }
                            if (targetVideo != null) {
                                val chosen: VideoItem = targetVideo!!
                                val shown = if (currentFolder != null && displayedVideos.isNotEmpty()) displayedVideos else source
                                val queue = if (shown.any { it.path == chosen.path }) shown else source
                                onPlayVideo(chosen, if (queue.isNotEmpty()) queue else listOf(chosen))
                            } else if (source.isNotEmpty()) {
                                // Nothing played in this folder yet: start from its FIRST episode (natural
                                // name order, so "Ep 2" comes before "Ep 10") and queue the rest in that order.
                                val ordered = source.sortedWith { a, b -> NaturalOrderComparator.compare(a.displayName, b.displayName) }
                                onPlayVideo(ordered.first(), ordered)
                            }
                        }
                        .testTag("resume_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.components.PlayGradientLogoIcon(size = 32.dp)
                }
            }

            AnimatedVisibility(
                visible = !isSelectionMode && effectiveSelectedTab == NavigationTab.MUSIC,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = fabBottomPadding)
            ) {
                val fabShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        // Liquid Glass: translucent frosted button; OFF = the original solid button
                        .liquidGlassOr(fabShape, isDark, elevation = 8.dp, backdropBlur = false) {
                            this
                            .shadow(18.dp, fabShape, ambientColor = com.example.ui.theme.AccentSkyBlue.copy(alpha = 0.50f), spotColor = com.example.ui.theme.AccentPink.copy(alpha = 0.60f))
                        .clip(fabShape)
                        .background(if (isDark) Brush.linearGradient(listOf(Neutral.D1E293B.copy(alpha = 0.95f), Neutral.D0F172A.copy(alpha = 0.95f))) else Brush.linearGradient(listOf(com.example.ui.theme.LightGlassSurface, com.example.ui.theme.LightGlassSurface)))
                        .then(if (isDark) Modifier.border(1.5.dp, com.example.ui.theme.AccentGradient, fabShape) else Modifier.border(1.5.dp, com.example.ui.theme.LightGlassBorder, fabShape))
                        }
                        .bounceClick(scaleDown = 0.88f) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val folderPath = selectedMusicFolder?.path
                                val resume = AudioPlaybackHistoryManager.getLastTrack(context, folderPath)
                                // Nothing played here yet: start from the FIRST track of this folder (natural
                                // name order) and queue the rest in that order.
                                val firstRun: List<AudioTrackItem> = if (resume == null) {
                                    val all = AudioLibraryCache.getCachedTracks().orEmpty()
                                    val inFolder = if (folderPath.isNullOrBlank()) all else all.filter { it.path.startsWith(folderPath) }
                                    inFolder.sortedWith { a, b -> NaturalOrderComparator.compare(a.title, b.title) }
                                } else emptyList()
                                withContext(Dispatchers.Main) {
                                    if (resume != null) {
                                        AudioPlaybackManager.playRecentTrack(context, resume)
                                    } else if (firstRun.isNotEmpty()) {
                                        AudioPlaybackManager.playTrack(context, firstRun.first(), firstRun)
                                        AudioPlaybackManager.openFullScreen()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.components.PlayGradientLogoIcon(size = 32.dp)
                }
            }

            // PLAYLISTS (inside a video playlist / M3U folder): Recent button that jumps straight to the video or
            // channel that was watched last in THIS folder, from the exact second it was left at.
            AnimatedVisibility(
                visible = !isSelectionMode && effectiveSelectedTab == NavigationTab.PLAYLISTS &&
                    selectedPlaylist != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = fabBottomPadding)
            ) {
                val fabShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        // Liquid Glass: translucent frosted button; OFF = the original solid button
                        .liquidGlassOr(fabShape, isDark, elevation = 8.dp, backdropBlur = false) {
                            this
                            .shadow(18.dp, fabShape, ambientColor = com.example.ui.theme.AccentSkyBlue.copy(alpha = 0.50f), spotColor = com.example.ui.theme.AccentPink.copy(alpha = 0.60f))
                        .clip(fabShape)
                        .background(if (isDark) Brush.linearGradient(listOf(Neutral.D1E293B.copy(alpha = 0.95f), Neutral.D0F172A.copy(alpha = 0.95f))) else Brush.linearGradient(listOf(com.example.ui.theme.LightGlassSurface, com.example.ui.theme.LightGlassSurface)))
                        .then(if (isDark) Modifier.border(1.5.dp, com.example.ui.theme.AccentGradient, fabShape) else Modifier.border(1.5.dp, com.example.ui.theme.LightGlassBorder, fabShape))
                        }
                        .bounceClick(scaleDown = 0.88f) {
                            val pl = selectedPlaylist ?: return@bounceClick
                            if (pl.isAudio) {
                                // AUDIO playlist: continue the track played last in THIS playlist (from the second it was
                                // left at); if nothing from it was played yet, start with the first track.
                                coroutineScope.launch(Dispatchers.IO) {
                                    val tracks = com.example.util.PlaylistManager.getAudioTracksForPlaylist(context, pl)
                                    var target: AudioTrackItem? = null
                                    var resumeRec: com.example.util.AudioPlaybackResume? = null
                                    for (rec in AudioPlaybackHistoryManager.getAllHistory(context)) {
                                        val found = (if (rec.path.isNotBlank()) tracks.firstOrNull { it.path == rec.path } else null)
                                        if (found != null) { target = found; resumeRec = rec; break }
                                    }
                                    val chosen = target ?: tracks.firstOrNull()
                                    val startAt = resumeRec?.takeIf { !it.isFinished }?.positionMs ?: 0L
                                    withContext(Dispatchers.Main) {
                                        if (chosen != null) {
                                            AudioPlaybackManager.playTrack(context, chosen, tracks, startAt)
                                            AudioPlaybackManager.openFullScreen()
                                        }
                                    }
                                }
                                return@bounceClick
                            }
                            coroutineScope.launch(Dispatchers.IO) {
                                val source = com.example.util.PlaylistManager.getVideosForPlaylist(pl, allLibraryVideos)
                                var target: VideoItem? = null
                                for (rec in com.example.util.PlaybackHistoryManager.getAllPlaybackRecords(context)) {
                                    val found = (if (rec.path.isNotBlank()) source.firstOrNull { it.path == rec.path } else null)
                                        ?: (if (rec.uriString.isNotBlank()) source.firstOrNull { it.uri.toString() == rec.uriString } else null)
                                    if (found != null) { target = found; break }
                                }
                                val chosen = target ?: source.firstOrNull()
                                withContext(Dispatchers.Main) {
                                    if (chosen != null) onPlayVideo(chosen, source)
                                }
                            }
                        }
                        .testTag("playlist_resume_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.ui.components.PlayGradientLogoIcon(size = 32.dp)
                }
            }

            // PLAYLISTS: floating "+ Create Playlist" button (replaces the old floating Play button).
            // Same glass look as the other floating buttons; opens the translucent options sheet.
            AnimatedVisibility(
                visible = !isSelectionMode && effectiveSelectedTab == NavigationTab.PLAYLISTS && selectedPlaylist == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = fabBottomPadding)
            ) {
                val fabShape = RoundedCornerShape(20.dp)
                Row(
                    modifier = Modifier
                        .height(60.dp)
                        // Liquid Glass: translucent frosted button; OFF = the original solid button
                        .liquidGlassOr(fabShape, isDark, elevation = 8.dp, backdropBlur = false) {
                            this
                            .shadow(18.dp, fabShape, ambientColor = com.example.ui.theme.AccentSkyBlue.copy(alpha = 0.50f), spotColor = com.example.ui.theme.AccentPink.copy(alpha = 0.60f))
                        .clip(fabShape)
                        .background(if (isDark) Brush.linearGradient(listOf(Neutral.D1E293B.copy(alpha = 0.95f), Neutral.D0F172A.copy(alpha = 0.95f))) else Brush.linearGradient(listOf(com.example.ui.theme.LightGlassSurface, com.example.ui.theme.LightGlassSurface)))
                        .then(if (isDark) Modifier.border(1.5.dp, com.example.ui.theme.AccentGradient, fabShape) else Modifier.border(1.5.dp, com.example.ui.theme.LightGlassBorder, fabShape))
                        }
                        .bounceClick(scaleDown = 0.92f) { playlistCreateRequest++ }
                        .padding(horizontal = 20.dp)
                        .testTag("playlist_create_fab"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Create Playlist",
                        tint = com.example.ui.theme.AccentSkyBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Create Playlist",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Neutral.D0F172A
                    )
                }
            }

            val isInsideAnyFolder = currentFolder != null || isOnlineVideoOpen || selectedMusicFolder != null || selectedRecentsFolder != null || selectedPlaylist != null || MediaDetailState.plugin != null

            // 6. Dynamic Floating Bottom Navigation Bar (Hidden inside folders & playlists or during selection)
            if (!isSelectionMode && !isInsideAnyFolder && activeTabs.isNotEmpty()) {
                DynamicBottomNavigation(
                    selectedTab = barSelectedTab,
                    onTabSelected = switchBottomTab,
                    showHomeTab = uiState.showHomeTab,
                    showMusicTab = uiState.showMusicTab,
                    showRecentsTab = uiState.showRecentsTab,
                    showPlaylistsTab = uiState.showPlaylistsTab,
                    showNetworkTab = uiState.showNetworkTab,
                    showDownloadsTab = uiState.showDownloadsTab,
                    isDark = isDark,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    tabNavigationStyle = uiState.tabNavigationStyle,
                    lang = uiState.language,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }

            // 6. Selection Mode Bottom Action Bar
            AnimatedVisibility(
                visible = isHomeSelectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Match the physical bottom position used by Audio and Playlist selection bars.
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                AnimatedContent(
                    targetState = showDeleteConfirmDialog,
                    transitionSpec = {
                        if (targetState) {
                            (slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) { it } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutVertically(animationSpec = tween(160)) { it / 2 } + fadeOut(animationSpec = tween(140)))
                        } else {
                            (slideInVertically(animationSpec = tween(200)) { it / 2 } + fadeIn(animationSpec = tween(200)))
                                .togetherWith(slideOutVertically(animationSpec = tween(160)) { it } + fadeOut(animationSpec = tween(140)))
                        }
                    },
                    label = "SelectionBottomBarTransition"
                ) { isDeleting ->
                    if (isDeleting) {
                        val count = totalSelectedCount
                        val itemTypeStr = if (isFolderSelection) {
                            if (count > 1) "$count folders" else "folder"
                        } else {
                            if (count > 1) "$count videos" else "video"
                        }
                        BottomDeleteConfirmationBar(
                            title = "Delete $itemTypeStr?",
                            message = "Permanently delete? Action cannot be undone.",
                            count = count,
                            isDark = isDark,
                            alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                            itemNames = if (isFolderSelection) {
                                (if (currentFolder != null) displayedSubFolders else displayedFolders)
                                    .filter { selectedFolderIds.contains(it.id) }.map { it.name }
                            } else {
                                (if (currentFolder != null) displayedVideos else allLibraryVideos)
                                    .filter { selectedVideoPaths.contains(it.path) }.map { it.displayName }
                            },
                            onCancel = { showDeleteConfirmDialog = false },
                            onConfirm = {
                                showDeleteConfirmDialog = false
                                val deletingFolderIds = selectedFolderIds.toSet()
                                val deletingVideoPaths = selectedVideoPaths.toSet()
                                val isFolderDel = isFolderSelection

                                // Capture the exact targets BEFORE any UI/cache mutation.
                                // Nothing on disk is touched until the user has pressed the
                                // final red Delete button in BottomDeleteConfirmationBar.
                                val allFoldersSnapshot = if (currentFolder != null) displayedSubFolders.toList() else displayedFolders.toList()
                                val allVideosSnapshot = if (currentFolder != null) displayedVideos.toList() else allLibraryVideos.toList()
                                val foldersToDelete = allFoldersSnapshot.filter { deletingFolderIds.contains(it.id) }
                                val videosToDelete = allVideosSnapshot.filter { deletingVideoPaths.contains(it.path) }

                                isMoveCopyInProgress = true
                                moveCopyIsDelete = true
                                com.example.ui.components.DeleteAnimState.start(
                                    (if (isFolderDel) foldersToDelete.map { it.id } else videosToDelete.map { it.path }).toSet()
                                )
                                moveCopyProgressFraction = 0f
                                moveCopyTotalCount = if (isFolderDel) foldersToDelete.size else videosToDelete.size
                                moveCopyCompletedCount = 0
                                moveCopyItemName = ""

                                coroutineScope.launch(Dispatchers.IO) {
                                    var deletedCount = 0
                                    if (isFolderDel) {
                                        for ((index, folder) in foldersToDelete.withIndex()) {
                                            moveCopyItemName = folder.name
                                            val c = VideoFileManager.deleteFolderVideos(context, folder) { fraction, completed, total, itemName ->
                                                val overallFraction = (index + fraction) / foldersToDelete.size.coerceAtLeast(1)
                                                moveCopyProgressFraction = overallFraction
                                                moveCopyItemName = itemName
                                            }
                                            deletedCount += c
                                            moveCopyCompletedCount = index + 1
                                            moveCopyProgressFraction = (index + 1).toFloat() / foldersToDelete.size.coerceAtLeast(1)
                                        }
                                    } else {
                                        deletedCount = VideoFileManager.deleteVideos(context, videosToDelete) { fraction, completed, total, itemName ->
                                            moveCopyProgressFraction = fraction
                                            moveCopyCompletedCount = completed
                                            moveCopyItemName = itemName
                                        }
                                    }
                                    // The physical delete has already completed here. Prune the
                                    // known-deleted items out of the in-memory tree immediately
                                    // (see pruneDeletedFromFolder) instead of relying solely on the
                                    // rescan below, which can otherwise race a not-yet-settled
                                    // MediaStore/filesystem index and briefly keep showing the
                                    // deleted item.
                                    // Exit animation: the deleted cards shrink + fade away first, then the list drops them.
                                    if (deletedCount > 0) {
                                        withContext(Dispatchers.Main) {
                                            com.example.ui.components.DeleteAnimState.exiting = true
                                        }
                                        kotlinx.coroutines.delay(420L)
                                    }
                                    withContext(Dispatchers.Main) {
                                        // In-place update (not clear+addAll) to avoid a visible
                                        // flash/flicker of the whole grid for unrelated folders.
                                        for (existing in videoFolders.toList()) {
                                            val pruned = pruneDeletedFromFolder(existing, deletingVideoPaths, deletingFolderIds)
                                            val idx = videoFolders.indexOfFirst { it.id == existing.id }
                                            if (idx == -1) continue
                                            if (pruned == null) {
                                                videoFolders.removeAt(idx)
                                            } else if (videoFolders[idx] != pruned) {
                                                videoFolders[idx] = pruned
                                            }
                                        }

                                        if (folderStack.isNotEmpty()) {
                                            val prunedStack = mutableListOf<VideoFolder>()
                                            for (stacked in folderStack) {
                                                val pruned = pruneDeletedFromFolder(stacked, deletingVideoPaths, deletingFolderIds)
                                                if (pruned == null) break
                                                prunedStack.add(pruned)
                                            }
                                            folderStack.clear()
                                            // If a folder in the path was fully emptied/removed by this
                                            // delete, fall back to the deepest surviving ancestor
                                            // instead of leaving the user on a folder that no longer
                                            // exists.
                                            folderStack.addAll(prunedStack)
                                            syncFolderStackToViewModel()
                                        }
                                    }
                                    // Refresh the visible library BEFORE declaring the delete
                                    // operation complete, so the user never sees "100% complete"
                                    // while the file is still present in the list; this also
                                    // reconciles anything the optimistic prune above didn't touch
                                    // (thumbnails, sizes, unrelated tree nodes).
                                    reloadFoldersAndWaitAfterMutation()
                                    withContext(Dispatchers.Main) {
                                        moveCopyProgressFraction = 1f
                                        kotlinx.coroutines.delay(120L)
                                        isMoveCopyInProgress = false
                                        moveCopyIsDelete = false
                                        com.example.ui.components.DeleteAnimState.clear()
                                        clearSelection()
                                        Toast.makeText(
                                            context,
                                            if (deletedCount > 0) "Deleted $deletedCount item(s)" else "Nothing was deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        reloadFolders()
                                    }
                                }
                            }
                        )
                    } else if (isMoveCopyInProgress) {
                        BottomMoveCopyProgressBar(
                            isDark = isDark,
                            alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                            isMove = moveCopyIsMove,
                            isDelete = moveCopyIsDelete,
                            progressFraction = moveCopyProgressFraction,
                            currentItemName = moveCopyItemName,
                            completedCount = moveCopyCompletedCount,
                            totalCount = moveCopyTotalCount
                        )
                    } else {
                        BottomSelectionBar(
                            isDark = isDark,
                            alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                            canRename = (selectedFolderIds.size == 1 && selectedVideoPaths.isEmpty()) || (selectedVideoPaths.isNotEmpty() && selectedFolderIds.isEmpty()),
                            canDelete = totalSelectedCount > 0,
                            canMove = totalSelectedCount > 0,
                            canCopy = totalSelectedCount > 0,
                            canPlaylist = totalSelectedCount > 0,
                            playlistIconRes = R.drawable.viaura_playlist_video,
                            onRename = {
                                if (selectedFolderIds.size == 1 && selectedVideoPaths.isEmpty()) {
                                    renamingFolderId = selectedFolderIds.first()
                                    renamingVideoPath = null
                                } else if (selectedVideoPaths.size == 1 && selectedFolderIds.isEmpty()) {
                                    renamingVideoPath = selectedVideoPaths.first()
                                    renamingFolderId = null
                                } else if (selectedVideoPaths.size > 1 && selectedFolderIds.isEmpty()) {
                                    renamingVideoPath = null
                                    renamingFolderId = null
                                    bulkRenameVideos = (if (currentFolder != null) displayedVideos else allLibraryVideos)
                                        .filter { selectedVideoPaths.contains(it.path) }
                                }
                            },
                            onDelete = {
                                if (totalSelectedCount > 0) {
                                    showDeleteConfirmDialog = true
                                }
                            },
                            onMove = {
                                if (totalSelectedCount > 0) {
                                    dirPickerAction = DirectoryPickerAction.MOVE
                                    showDirectoryPickerSheet = true
                                }
                            },
                            onCopy = {
                                if (totalSelectedCount > 0) {
                                    dirPickerAction = DirectoryPickerAction.COPY
                                    showDirectoryPickerSheet = true
                                }
                            },
                            onPlaylist = {
                                if (totalSelectedCount > 0) {
                                    showAddToPlaylistDialog = true
                                }
                            }
                        )
                    }
                }
            }

            // --- Information surfaces ---
            // Media details are an in-app screen overlay, not a popup. The original
            // selection state remains untouched so Back returns to the same selected item.
            if (selectedVideoForInfo != null) {
                MediaInformationScreen(
                    video = selectedVideoForInfo!!,
                    uiState = uiState,
                    onBack = { selectedVideoForInfo = null },
                    onSettingsClick = { showSettingsSheet = true }
                )
            }

            if (showFolderInfoSheet && selectedFolderForInfo != null) {
                FolderInformationSheet(
                    folder = selectedFolderForInfo!!,
                    isDark = isDark,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onDismiss = {
                        showFolderInfoSheet = false
                        selectedFolderForInfo = null
                    }
                )
            }

            if (selectedAudioForInfo != null) {
                val audio = selectedAudioForInfo!!
                val videoItem = VideoItem(
                    id = audio.id,
                    uri = audio.uri,
                    displayName = audio.title,
                    path = audio.path,
                    sizeBytes = audio.sizeBytes,
                    durationMs = audio.durationMs,
                    dateModified = audio.dateModified
                )
                MediaInformationScreen(
                    video = videoItem,
                    uiState = uiState,
                    onBack = { selectedAudioForInfo = null }
                )
            }

            if (showAudioFolderInfoSheet && selectedAudioFolderForInfo != null) {
                AudioFolderInformationSheet(
                    folder = selectedAudioFolderForInfo!!,
                    isDark = isDark,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onDismiss = {
                        showAudioFolderInfoSheet = false
                        selectedAudioFolderForInfo = null
                    }
                )
            }

            // --- Liquid Glass Dialogs & Sheets ---

            // A. Storage Permission Dialog
            if (showPermissionDialog) {
                StoragePermissionDialog(
                    isDark = isDark,
                    appScale = uiState.appScale,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onRequestPermissions = onRequestPermissions,
                    onDismiss = { showPermissionDialog = false },
                    onPermissionGranted = {
                        showPermissionDialog = false
                        hasPermission = true
                        viewModel?.setHasMediaPermission(true)
                        viewModel?.triggerLibraryRefresh()
                        reloadFolders(true)
                    }
                )
            }

            // B. Create Custom Folder Dialog (Liquid Glass)
            if (showCreateFolderDialog) {
                CreateFolderDialog(
                    isDark = isDark,
                    transparencyPercent = uiState.glassBlurTransparency,
                    onDismiss = { showCreateFolderDialog = false },
                    onCreate = { newFolderName ->
                        if (newFolderName.isNotBlank()) {
                            if (!customFolders.contains(newFolderName)) {
                                customFolders.add(0, newFolderName)
                                SettingsPreferencesManager.saveCustomFolders(context, customFolders.toSet())
                                reloadFolders()
                                Toast.makeText(context, "Folder created: $newFolderName", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showCreateFolderDialog = false
                    }
                )
            }

            // C. Last Watched Quick Resume Dialog (Liquid Glass)
            if (showLastWatchedDialog) {
                val lastRecord = remember(showLastWatchedDialog) {
                    com.example.util.PlaybackHistoryManager.getLastWatchedRecord(context)
                }
                LastWatchedResumeDialog(
                    isDark = isDark,
                    lastRecord = lastRecord,
                    onDismiss = { showLastWatchedDialog = false },
                    onResumePlay = {
                        showLastWatchedDialog = false
                        val targetVideo = if (lastRecord != null) {
                            allLibraryVideos.find {
                                (lastRecord.videoId > 0 && it.id == lastRecord.videoId) ||
                                (lastRecord.path.isNotBlank() && it.path == lastRecord.path) ||
                                (lastRecord.title.isNotBlank() && it.displayName == lastRecord.title)
                            } ?: VideoItem(
                                id = if (lastRecord.videoId > 0) lastRecord.videoId else 1L,
                                uri = if (lastRecord.uriString.isNotBlank()) android.net.Uri.parse(lastRecord.uriString) else android.net.Uri.EMPTY,
                                displayName = lastRecord.title.ifBlank { "Sample Video" },
                                path = lastRecord.path,
                                sizeBytes = 1420000000L,
                                durationMs = if (lastRecord.durationMs > 0) lastRecord.durationMs else 1440000L,
                                dateModified = lastRecord.lastWatchedTimestamp / 1000L,
                                isNew = false
                            )
                        } else {
                            allLibraryVideos.firstOrNull() ?: displayedVideos.firstOrNull()
                        }

                        if (targetVideo != null) {
                            onPlayVideo(targetVideo, if (allLibraryVideos.isNotEmpty()) allLibraryVideos else displayedVideos)
                        }
                    }
                )
            }

            // D. Sort & View Options Liquid Glass Popup
            if (showSortAndViewDialog) {
                val isInsideFolder = when (effectiveSelectedTab) {
                    NavigationTab.HOME -> currentFolder != null || activeViewMode == ViewMode.LIBRARY
                    NavigationTab.MUSIC -> isInsideMusicFolder || activeViewMode == ViewMode.LIBRARY
                    NavigationTab.RECENTS -> isInsideRecentsFolder || activeViewMode == ViewMode.LIBRARY
                    NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> false
                    NavigationTab.PLAYLISTS -> isInsidePlaylistDetail
                }
                val isOnlineAudioFolder = (effectiveSelectedTab == NavigationTab.MUSIC && selectedMusicFolder?.path == com.example.ui.screens.ONLINE_FOLDER_SENTINEL_PATH)
                val isOnlineFolder = (effectiveSelectedTab == NavigationTab.MUSIC && selectedMusicFolder?.path == com.example.ui.screens.ONLINE_FOLDER_SENTINEL_PATH)
                val isDownloadsPage = (effectiveSelectedTab == NavigationTab.NETWORK && showDownloads)
                // The Download page shows the exact Sort & View of the Online Video folder.
                val isOnlineVideoFolder = (effectiveSelectedTab == NavigationTab.HOME && isOnlineVideoOpen) || isDownloadsPage
                val isOnlinePlaylist = (effectiveSelectedTab == NavigationTab.PLAYLISTS &&
                    selectedPlaylist?.isOnlineFolder == true)
                val isMusic = when (effectiveSelectedTab) {
                    NavigationTab.MUSIC -> true
                    NavigationTab.PLAYLISTS -> selectedPlaylist?.isAudio == true
                    else -> false
                }
                // Inside an ONLINE playlist the popup is identical to the matching online folder (video: Online Videos, audio: Online Audio).
                val isOnlinePlVideo = isOnlinePlaylist && isInsidePlaylistDetail && !isMusic
                val isOnlinePlAudio = isOnlinePlaylist && isInsidePlaylistDetail && isMusic
                SortAndViewOptionsPopup(
                    isDark = isDark,
                    sortField = if (isDownloadsPage) downloadsSortField else if (isOnlineVideoFolder) onlineVideoSortField else if (isOnlineAudioFolder) onlineAudioSortField else if (isOnlinePlVideo) onlinePlVideoSortField else if (isOnlinePlAudio) onlinePlAudioSortField else activeSortField,
                    sortDirection = if (isDownloadsPage) downloadsSortDirection else if (isOnlineVideoFolder) onlineVideoSortDirection else if (isOnlineAudioFolder) onlineAudioSortDirection else if (isOnlinePlVideo) onlinePlVideoSortDirection else if (isOnlinePlAudio) onlinePlAudioSortDirection else activeSortDirection,
                    viewMode = activeViewMode,
                    layoutMode = if (isDownloadsPage) downloadsLayoutMode else if (isOnlineVideoFolder) onlineVideoLayoutMode else if (isOnlineAudioFolder) onlineAudioLayoutMode else if (isOnlinePlVideo) onlinePlVideoLayoutMode else if (isOnlinePlAudio) onlinePlAudioLayoutMode else activeLayoutMode,
                    visibleFields = if (isDownloadsPage) downloadsVisibleFields else if (isOnlineVideoFolder) onlineVideoVisibleFields else if (isOnlineAudioFolder) onlineAudioVisibleFields else if (isOnlinePlVideo) onlinePlVideoVisibleFields else if (isOnlinePlAudio) onlinePlAudioVisibleFields else effectiveVisibleFields,
                    isInsideFolder = if (isOnlineAudioFolder || isOnlineVideoFolder || isOnlinePlVideo || isOnlinePlAudio) true else isInsideFolder,
                    isMusicMode = isMusic,
                    isRecentsMode = (effectiveSelectedTab == NavigationTab.RECENTS),
                    isPlaylistMode = (effectiveSelectedTab == NavigationTab.PLAYLISTS),
                    isOnlineMode = isOnlineFolder || isOnlineVideoFolder,
                    isOnlinePlaylist = isOnlinePlaylist,
                    isOnlineAudioFolder = isOnlineAudioFolder || isOnlinePlAudio,
                    isOnlineVideoFolder = isOnlineVideoFolder || isOnlinePlVideo,
                    isDownloadsMode = isDownloadsPage,
                    appScale = uiState.appScale,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onSortFieldChange = { newField ->
                        if (isDownloadsPage) {
                            downloadsSortField = newField
                            SettingsPreferencesManager.saveSortField(context, newField, "downloads")
                        } else if (isOnlineVideoFolder) {
                            onlineVideoSortField = newField
                            com.example.ui.components.OnlineVideoFeedState.sortTouched = true
                            SettingsPreferencesManager.saveSortField(context, newField, "online_video")
                        } else if (isOnlineAudioFolder) {
                            onlineAudioSortField = newField
                            com.example.ui.components.OnlineAudioFeedState.sortTouched = true
                            SettingsPreferencesManager.saveSortField(context, newField, "online_audio")
                        } else if (isOnlinePlVideo) {
                            onlinePlVideoSortField = newField
                            SettingsPreferencesManager.saveSortField(context, newField, "online_playlist_video")
                        } else if (isOnlinePlAudio) {
                            onlinePlAudioSortField = newField
                            SettingsPreferencesManager.saveSortField(context, newField, "online_playlist_audio")
                        } else {
                            when (effectiveSelectedTab) {
                                NavigationTab.HOME -> {
                                    if (isCurrentInsideFolder) homeContentSortField = newField else homeFolderSortField = newField
                                }
                                NavigationTab.MUSIC -> {
                                    if (isCurrentInsideFolder) musicContentSortField = newField else musicFolderSortField = newField
                                }
                                NavigationTab.RECENTS -> {
                                    if (isCurrentInsideFolder) recentsContentSortField = newField else recentsFolderSortField = newField
                                }
                                NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> {}
                                NavigationTab.PLAYLISTS -> {
                                    if (isCurrentInsideFolder) playlistsContentSortField = newField else playlistsFolderSortField = newField
                                }
                            }
                            val suffix = if (isCurrentInsideFolder) "_content" else "_folders"
                            SettingsPreferencesManager.saveSortField(context, newField, "${effectiveSelectedTab.id}$suffix")
                            SettingsPreferencesManager.saveSortField(context, newField, effectiveSelectedTab.id)
                        }
                    },
                    onSortDirectionChange = { newDir ->
                        if (isDownloadsPage) {
                            downloadsSortDirection = newDir
                            SettingsPreferencesManager.saveSortDirection(context, newDir, "downloads")
                        } else if (isOnlineVideoFolder) {
                            onlineVideoSortDirection = newDir
                            com.example.ui.components.OnlineVideoFeedState.sortTouched = true
                            SettingsPreferencesManager.saveSortDirection(context, newDir, "online_video")
                        } else if (isOnlineAudioFolder) {
                            onlineAudioSortDirection = newDir
                            com.example.ui.components.OnlineAudioFeedState.sortTouched = true
                            SettingsPreferencesManager.saveSortDirection(context, newDir, "online_audio")
                        } else if (isOnlinePlVideo) {
                            onlinePlVideoSortDirection = newDir
                            SettingsPreferencesManager.saveSortDirection(context, newDir, "online_playlist_video")
                        } else if (isOnlinePlAudio) {
                            onlinePlAudioSortDirection = newDir
                            SettingsPreferencesManager.saveSortDirection(context, newDir, "online_playlist_audio")
                        } else {
                            when (effectiveSelectedTab) {
                                NavigationTab.HOME -> {
                                    if (isCurrentInsideFolder) homeContentSortDirection = newDir else homeFolderSortDirection = newDir
                                }
                                NavigationTab.MUSIC -> {
                                    if (isCurrentInsideFolder) musicContentSortDirection = newDir else musicFolderSortDirection = newDir
                                }
                                NavigationTab.RECENTS -> {
                                    if (isCurrentInsideFolder) recentsContentSortDirection = newDir else recentsFolderSortDirection = newDir
                                }
                                NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> {}
                                NavigationTab.PLAYLISTS -> {
                                    if (isCurrentInsideFolder) playlistsContentSortDirection = newDir else playlistsFolderSortDirection = newDir
                                }
                            }
                            val suffix = if (isCurrentInsideFolder) "_content" else "_folders"
                            SettingsPreferencesManager.saveSortDirection(context, newDir, "${effectiveSelectedTab.id}$suffix")
                            SettingsPreferencesManager.saveSortDirection(context, newDir, effectiveSelectedTab.id)
                        }
                    },
                    onViewModeChange = { newMode ->
                        if (!isOnlineAudioFolder && !isOnlineVideoFolder && !isOnlinePlVideo && !isOnlinePlAudio) {
                            when (effectiveSelectedTab) {
                                NavigationTab.HOME -> homeViewMode = newMode
                                NavigationTab.MUSIC -> musicViewMode = newMode
                                NavigationTab.RECENTS -> {}
                                NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> {}
                                NavigationTab.PLAYLISTS -> playlistsViewMode = newMode
                            }
                            SettingsPreferencesManager.saveViewMode(context, newMode, effectiveSelectedTab.id)
                        }
                    },
                    onLayoutModeChange = { newLayout ->
                        if (isDownloadsPage) {
                            downloadsLayoutMode = newLayout
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, "downloads")
                        } else if (isOnlineVideoFolder) {
                            onlineVideoLayoutMode = newLayout
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, "online_video")
                        } else if (isOnlineAudioFolder) {
                            onlineAudioLayoutMode = newLayout
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, "online_audio")
                        } else if (isOnlinePlVideo) {
                            onlinePlVideoLayoutMode = newLayout
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, "online_playlist_video")
                        } else if (isOnlinePlAudio) {
                            onlinePlAudioLayoutMode = newLayout
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, "online_playlist_audio")
                        } else {
                            when (effectiveSelectedTab) {
                                NavigationTab.HOME -> {
                                    if (isCurrentInsideFolder) homeContentLayoutMode = newLayout else homeFolderLayoutMode = newLayout
                                }
                                NavigationTab.MUSIC -> {
                                    if (isCurrentInsideFolder) musicContentLayoutMode = newLayout else musicFolderLayoutMode = newLayout
                                }
                                NavigationTab.RECENTS -> {
                                    if (isCurrentInsideFolder) recentsContentLayoutMode = newLayout else recentsFolderLayoutMode = newLayout
                                }
                                NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> {}
                                NavigationTab.PLAYLISTS -> {
                                    if (isCurrentInsideFolder) playlistsContentLayoutMode = newLayout else playlistsFolderLayoutMode = newLayout
                                }
                            }
                            val suffix = if (isCurrentInsideFolder) "_content" else "_folders"
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, "${effectiveSelectedTab.id}$suffix")
                            SettingsPreferencesManager.saveLayoutMode(context, newLayout, effectiveSelectedTab.id)
                        }
                    },
                    onVisibleFieldsChange = { newFields ->
                        if (isDownloadsPage) {
                            val sanitizedDl = newFields.copy(showPath = false, showFramerate = false, showVideoCount = false, showNewBadge = false, showFullName = false)
                            downloadsVisibleFields = sanitizedDl
                            SettingsPreferencesManager.saveVisibleFields(context, sanitizedDl, "downloads")
                        } else if (isOnlineVideoFolder) {
                            val sanitizedVideo = SettingsPreferencesManager.sanitizeOnlineVideoFields(newFields)
                            onlineVideoVisibleFields = sanitizedVideo
                            SettingsPreferencesManager.saveVisibleFields(context, sanitizedVideo, "online_video")
                        } else if (isOnlineAudioFolder) {
                            val sanitized = SettingsPreferencesManager.sanitizeOnlineAudioFields(newFields)
                            onlineAudioVisibleFields = sanitized
                            SettingsPreferencesManager.saveVisibleFields(context, sanitized, "online_audio")
                        } else if (isOnlinePlVideo) {
                            val sanitized = SettingsPreferencesManager.sanitizeOnlineVideoFields(newFields)
                            onlinePlVideoVisibleFields = sanitized
                            SettingsPreferencesManager.saveVisibleFields(context, sanitized, "online_playlist_video")
                        } else if (isOnlinePlAudio) {
                            val sanitized = SettingsPreferencesManager.sanitizeOnlineAudioFields(newFields)
                            onlinePlAudioVisibleFields = sanitized
                            SettingsPreferencesManager.saveVisibleFields(context, sanitized, "online_playlist_audio")
                        } else if (effectiveSelectedTab == NavigationTab.RECENTS) {
                            val sanitized = SettingsPreferencesManager.sanitizeRecentsFields(newFields)
                            recentsContentVisibleFields = sanitized
                            recentsFolderVisibleFields = sanitized
                            SettingsPreferencesManager.saveVisibleFields(context, sanitized, "recents_content")
                            SettingsPreferencesManager.saveVisibleFields(context, sanitized, "recents_folders")
                            SettingsPreferencesManager.saveVisibleFields(context, sanitized, "recents")
                        } else {
                            when (effectiveSelectedTab) {
                                NavigationTab.HOME -> {
                                    if (isCurrentInsideFolder) homeContentVisibleFields = newFields else homeFolderVisibleFields = newFields
                                }
                                NavigationTab.MUSIC -> {
                                    if (isCurrentInsideFolder) musicContentVisibleFields = newFields else musicFolderVisibleFields = newFields
                                }
                                NavigationTab.RECENTS -> {
                                    if (isCurrentInsideFolder) recentsContentVisibleFields = newFields else recentsFolderVisibleFields = newFields
                                }
                                NavigationTab.NETWORK, NavigationTab.DOWNLOADS -> {}
                                NavigationTab.PLAYLISTS -> {
                                    if (isCurrentInsideFolder) playlistsContentVisibleFields = newFields else playlistsFolderVisibleFields = newFields
                                }
                            }
                            val suffix = if (isCurrentInsideFolder) "_content" else "_folders"
                            SettingsPreferencesManager.saveVisibleFields(context, newFields, "${effectiveSelectedTab.id}$suffix")
                            SettingsPreferencesManager.saveVisibleFields(context, newFields, effectiveSelectedTab.id)
                        }
                    },
                    onDismiss = {
                        if (isOnlineAudioFolder) {
                            SettingsPreferencesManager.saveSortField(context, onlineAudioSortField, "online_audio")
                            SettingsPreferencesManager.saveSortDirection(context, onlineAudioSortDirection, "online_audio")
                            SettingsPreferencesManager.saveLayoutMode(context, onlineAudioLayoutMode, "online_audio")
                            SettingsPreferencesManager.saveVisibleFields(context, onlineAudioVisibleFields, "online_audio")
                        }
                        showSortAndViewDialog = false
                    }
                )
            }

            // E. Settings Sheet Modal (Liquid Glass)
            if (showSettingsSheet) {
                // Haptic tick when the settings dashboard opens
                val settingsHapticView = androidx.compose.ui.platform.LocalView.current
                LaunchedEffect(Unit) { com.example.util.AppHaptics.performClick(settingsHapticView) }
                SettingsSheet(
                    uiState = uiState,
                    onDismiss = { showSettingsSheet = false },
                    onThemeChange = onThemeChange,
                    onScaleChange = onScaleChange,
                    onGlassBlurTransparencyChange = onGlassBlurTransparencyChange,
                    onSideBySideChange = onSideBySideChange,
                    onLanguageChange = onLanguageChange,
                    onFrameStepChange = onFrameStepChange,
                    onCopyTimestampChange = onCopyTimestampChange,
                    onHwAccelModeChange = onHwAccelModeChange,
                    onGestureSensitivityModeChange = { mode ->
                        if (viewModel != null) {
                            viewModel.setGestureSensitivityMode(mode)
                        } else {
                            onGestureSensitivityModeChange(mode)
                        }
                    },
                    onShowPlayerNotificationsChange = { show ->
                        viewModel?.setShowPlayerNotifications(show)
                    },
                    onSeekbarStyleChange = { style ->
                        viewModel?.setSeekbarStyle(style)
                    },
                    onPlayerLayoutConfigChange = { config ->
                        viewModel?.setPlayerLayoutConfig(config)
                    },
                    onResetPlayerLayoutConfig = {
                        viewModel?.resetPlayerLayoutConfig()
                    },
                    onPreferredAudioLanguagesChange = { langs ->
                        viewModel?.setPreferredAudioLanguages(langs)
                    },
                    onApplyAudioLanguageToSelectedContentOnlyChange = { apply ->
                        viewModel?.setApplyAudioLanguageToSelectedContentOnly(apply)
                    },
                    onSelectedAudioFoldersChange = { folders ->
                        viewModel?.setSelectedAudioFolders(folders)
                    },
                    onSelectedAudioVideosChange = { videos ->
                        viewModel?.setSelectedAudioVideos(videos)
                    },
                    onEnableAudioPitchCorrectionChange = { enabled ->
                        viewModel?.setEnableAudioPitchCorrection(enabled)
                    },
                    onVolumeNormalizationChange = { norm ->
                        viewModel?.setVolumeNormalization(norm)
                    },
                    onBackgroundPlaybackChange = { bg ->
                        viewModel?.setBackgroundPlayback(bg)
                    },
                    onAudioChannelModeChange = { mode ->
                        viewModel?.setAudioChannelMode(mode)
                    },
                    onVolumeBoostCapChange = { cap ->
                        viewModel?.setVolumeBoostCap(cap)
                    },
                    onRememberSelectedAudioTrackChange = { rem ->
                        viewModel?.setRememberSelectedAudioTrack(rem)
                    },
                    onPlayerSettingsChange = { settings ->
                        viewModel?.setPlayerSettings(settings)
                    },
                    // Appearance / File Browser / Thumbnails / Navigation / Animations
                    // are all persisted through the same AppViewModel state.
                    onAppThemeChange = { viewModel?.setAppTheme(it) },
                    onCustomThemeGradientChange = { id, cols -> viewModel?.setCustomThemeGradient(id, cols) },
                    onAmoledBlackModeChange = { viewModel?.setAmoledBlackMode(it) },
                    onLiquidGlassModeChange = { viewModel?.setLiquidGlassMode(it) },
                    onLiquidGlassParamsChange = { viewModel?.setLiquidGlassParams(it) },
                    onLiquidGlassControlsChange = { viewModel?.setLiquidGlassControls(it) },
                    onUseSystemFontChange = { viewModel?.setUseSystemFont(it) },
                    onHapticFeedbackChange = { viewModel?.setHapticFeedback(it) },
                    onShowFullNameChange = { viewModel?.setShowFullName(it) },
                    onShowNewVideoLabelChange = { viewModel?.setShowNewVideoLabel(it) },
                    onNewVideoDaysThresholdChange = { viewModel?.setNewVideoDaysThreshold(it) },
                    onShowFolderUnplayedBadgeChange = { viewModel?.setShowFolderUnplayedBadge(it) },
                    onAutoScrollToLastPlayedChange = { viewModel?.setAutoScrollToLastPlayed(it) },
                    onTreePathCompressionChange = { viewModel?.setTreePathCompression(it) },
                    onDualPaneViewChange = { viewModel?.setDualPaneView(it) },
                    onWatchedThresholdPercentChange = { viewModel?.setWatchedThresholdPercent(it) },
                    onShowVideoThumbnailsChange = { viewModel?.setShowVideoThumbnails(it) },
                    onThumbnailStrategyChange = { viewModel?.setThumbnailStrategy(it) },
                    onThumbnailQualityChange = { viewModel?.setThumbnailQuality(it) },
                    onTapThumbnailToSelectChange = { viewModel?.setTapThumbnailToSelect(it) },
                    onShowNetworkThumbnailsChange = { viewModel?.setShowNetworkThumbnails(it) },
                    onShowHomeTabChange = { viewModel?.setShowHomeTab(it) },
                    onShowMusicTabChange = { viewModel?.setShowMusicTab(it) },
                    onShowRecentsTabChange = { viewModel?.setShowRecentsTab(it) },
                    onShowPlaylistsTabChange = { viewModel?.setShowPlaylistsTab(it) },
                    onShowNetworkTabChange = { viewModel?.setShowNetworkTab(it) },
                    onShowDownloadsTabChange = { viewModel?.setShowDownloadsTab(it) },
                    onControlsAnimationStyleChange = { viewModel?.setControlsAnimationStyle(it) },
                    onVideoOpeningAnimationChange = { viewModel?.setVideoOpeningAnimation(it) },
                    onScreenNavigationStyleChange = { viewModel?.setScreenNavigationStyle(it) },
                    onTabNavigationStyleChange = { viewModel?.setTabNavigationStyle(it) },
                    onAnimationSpeedChange = { viewModel?.setAnimationSpeed(it) },
                    viewModel = viewModel
                )
            }

            // F. Rename Folder Dialog
            if (showRenameFolderDialog && folderToRename != null) {
                RenameDialog(
                    title = "Rename Folder",
                    initialText = renameFolderNameText,
                    isDark = isDark,
                    alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    onDismiss = {
                        showRenameFolderDialog = false
                        folderToRename = null
                    },
                    onConfirm = { newName ->
                        showRenameFolderDialog = false
                        folderToRename?.let { f ->
                            coroutineScope.launch(Dispatchers.IO) {
                                val success = VideoFileManager.renameFolder(context, f, newName)
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, "Folder renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                                        reloadFolders()
                                        clearSelection()
                                    } else {
                                        Toast.makeText(context, "Failed to rename folder", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        folderToRename = null
                    }
                )
            }

            // G. Rename Video Dialog
            if (showRenameVideoDialog && videoToRename != null) {
                RenameDialog(
                    title = "Rename Video",
                    initialText = renameVideoNameText,
                    isDark = isDark,
                    alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    onDismiss = {
                        showRenameVideoDialog = false
                        videoToRename = null
                    },
                    onConfirm = { newName ->
                        showRenameVideoDialog = false
                        videoToRename?.let { v ->
                            coroutineScope.launch(Dispatchers.IO) {
                                val success = VideoFileManager.renameVideo(context, v, newName)
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(context, "Video renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                                        reloadFolders()
                                        clearSelection()
                                    } else {
                                        Toast.makeText(context, "Failed to rename video", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        videoToRename = null
                    }
                )
            }

            // H. Bulk Rename sheet (opened from the Rename button when 2+ videos are selected)
            if (bulkRenameVideos.size > 1) {
                com.example.ui.components.BulkRenameSheet(
                    isDark = isDark,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    videos = bulkRenameVideos,
                    onDismiss = { bulkRenameVideos = emptyList() },
                    onConfirm = { renames ->
                        bulkRenameVideos = emptyList()
                        if (renames.isNotEmpty()) {
                            coroutineScope.launch {
                                val result = VideoFileManager.bulkRenameVideos(context, renames)
                                val msg = if (result.failed.isEmpty()) {
                                    "Renamed ${result.renamed} video(s)"
                                } else {
                                    "Renamed ${result.renamed}, failed ${result.failed.size}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                reloadFolders()
                                clearSelection()
                            }
                        }
                    }
                )
            }

            // I. Directory Picker Sheet for Move / Copy
            if (showDirectoryPickerSheet) {
                val count = totalSelectedCount
                val itemDesc = if (isFolderSelection) {
                    if (count > 1) "$count folders" else "1 folder"
                } else {
                    if (count > 1) "$count videos" else "1 video"
                }
                val initialPath = if (isFolderSelection) {
                    val allFolders = if (currentFolder != null) displayedSubFolders else displayedFolders
                    allFolders.find { selectedFolderIds.contains(it.id) }?.path
                } else {
                    val allVideos = if (currentFolder != null) displayedVideos else allLibraryVideos
                    allVideos.find { selectedVideoPaths.contains(it.path) }?.path
                }

                FloatingDirectoryPickerSheet(
                    action = dirPickerAction,
                    itemCount = count,
                    itemDescription = itemDesc,
                    initialPath = initialPath,
                    isDark = isDark,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onConfirmTargetDir = { targetDir ->
                        showDirectoryPickerSheet = false
                        val isMove = dirPickerAction == DirectoryPickerAction.MOVE
                        isMoveCopyInProgress = true
                        moveCopyProgressFraction = 0f
                        moveCopyIsMove = isMove
                        moveCopyTotalCount = totalSelectedCount
                        moveCopyCompletedCount = 0
                        moveCopyItemName = ""

                        coroutineScope.launch(Dispatchers.IO) {
                            var opCount = 0
                            if (isFolderSelection) {
                                val allFolders = if (currentFolder != null) displayedSubFolders else displayedFolders
                                val foldersToOperate = allFolders.filter { selectedFolderIds.contains(it.id) }
                                for (f in foldersToOperate) {
                                    moveCopyItemName = f.name
                                    val res = if (isMove) {
                                        VideoFileManager.moveFolder(context, f, targetDir) { fraction, _, _, itemName ->
                                            moveCopyProgressFraction = fraction
                                            moveCopyItemName = itemName
                                        }
                                    } else {
                                        VideoFileManager.copyFolder(context, f, targetDir) { fraction, _, _, itemName ->
                                            moveCopyProgressFraction = fraction
                                            moveCopyItemName = itemName
                                        }
                                    }
                                    if (res) {
                                        opCount++
                                        moveCopyCompletedCount = opCount
                                    }
                                }
                            } else {
                                val allVideos = if (currentFolder != null) displayedVideos else allLibraryVideos
                                val videosToOperate = allVideos.filter { selectedVideoPaths.contains(it.path) }
                                opCount = if (isMove) {
                                    VideoFileManager.moveVideos(context, videosToOperate, targetDir) { fraction, _, _, itemName ->
                                        moveCopyProgressFraction = fraction
                                        moveCopyItemName = itemName
                                    }
                                } else {
                                    VideoFileManager.copyVideos(context, videosToOperate, targetDir) { fraction, _, _, itemName ->
                                        moveCopyProgressFraction = fraction
                                        moveCopyItemName = itemName
                                    }
                                }
                                moveCopyCompletedCount = opCount
                            }
                            withContext(Dispatchers.Main) {
                                moveCopyProgressFraction = 1f
                                kotlinx.coroutines.delay(200L)
                                isMoveCopyInProgress = false
                                val actionName = if (isMove) "Moved" else "Copied"
                                Toast.makeText(context, "$actionName $opCount item(s) to ${targetDir.name}", Toast.LENGTH_SHORT).show()
                                reloadFolders()
                                clearSelection()
                            }
                        }
                    },
                    onDismiss = { showDirectoryPickerSheet = false }
                )
            }

            // J. Add to Playlist Sheet
            if (showAddToPlaylistDialog) {
                val videosToAdd = if (isFolderSelection) {
                    val allFolders = if (currentFolder != null) displayedSubFolders else displayedFolders
                    allFolders.filter { selectedFolderIds.contains(it.id) }.flatMap { it.getAllVideos() }
                } else {
                    val allVideos = if (currentFolder != null) displayedVideos else allLibraryVideos
                    allVideos.filter { selectedVideoPaths.contains(it.path) }
                }
                AddToPlaylistDialog(
                    selectedVideos = videosToAdd,
                    isDark = isDark,
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onDismiss = { showAddToPlaylistDialog = false },
                    onPlaylistUpdated = {
                        showAddToPlaylistDialog = false
                        clearSelection()
                        playlistTriggerRefresh++
                        Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // L. Rename Playlist Dialog
            if (showRenamePlaylistDialog && selectedPlaylistForAction != null) {
                val currentTargetPl = selectedPlaylistForAction!!
                RenameDialog(
                    title = "Rename Playlist",
                    initialText = renamePlaylistText,
                    isDark = isDark,
                    alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    onDismiss = {
                        showRenamePlaylistDialog = false
                    },
                    onConfirm = { newName ->
                        showRenamePlaylistDialog = false
                        if (newName.isNotBlank()) {
                            PlaylistManager.renamePlaylist(context, currentTargetPl.id, newName)
                            playlistTriggerRefresh++
                            Toast.makeText(context, "Renamed to \"$newName\"", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // M. Clear Playlist Confirmation Dialog (Leaves files intact on disk!)
            if (showClearPlaylistConfirmDialog && selectedPlaylistForAction != null) {
                val currentTargetPl = selectedPlaylistForAction!!
                DeleteConfirmDialog(
                    title = "Clear Playlist Videos?",
                    message = "Remove all videos from \"${currentTargetPl.name}\"?\n\nOnly the playlist list will be emptied; your actual video files on disk will NOT be deleted or moved.",
                    isDark = isDark,
                    alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    onDismiss = {
                        showClearPlaylistConfirmDialog = false
                    },
                    onConfirm = {
                        showClearPlaylistConfirmDialog = false
                        PlaylistManager.clearPlaylistVideos(context, currentTargetPl.id)
                        playlistTriggerRefresh++
                        Toast.makeText(context, "Playlist emptied (files kept safely on disk)", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // N. Delete Playlist Confirmation Dialog (Leaves files intact on disk!)
            if (showDeletePlaylistConfirmDialog && selectedPlaylistForAction != null) {
                val currentTargetPl = selectedPlaylistForAction!!
                DeleteConfirmDialog(
                    title = "Delete Playlist?",
                    message = "Delete playlist \"${currentTargetPl.name}\"?\n\nOnly this playlist entry will be removed; none of your video files in storage folders will be deleted.",
                    isDark = isDark,
                    alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                    onDismiss = {
                        showDeletePlaylistConfirmDialog = false
                    },
                    onConfirm = {
                        showDeletePlaylistConfirmDialog = false
                        PlaylistManager.deletePlaylist(context, currentTargetPl.id)
                        playlistTriggerRefresh++
                        Toast.makeText(context, "Playlist \"${currentTargetPl.name}\" removed", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Minimized Floating Audio Player (Smoothly elevates above Selection Bar or Bottom Nav)
            val miniPlayerBottomPadding by androidx.compose.animation.core.animateDpAsState(
                targetValue = when {
                    isSelectionMode -> 112.dp
                    activeTabs.isNotEmpty() -> 70.dp
                    else -> 12.dp
                },
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "mini_player_bottom_padding"
            )

            MinimizedAudioPlayer(
                isDark = isDark,
                glassBlurTransparency = uiState.glassBlurTransparency,
                onExpand = { AudioPlaybackManager.openFullScreen() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = miniPlayerBottomPadding)
            )

            // Video mini player: same pill/position/animation as the audio one (only one of them is ever active).
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
                    .padding(bottom = miniPlayerBottomPadding)
            )

            // Full Screen Audio Player Sheet with dynamic slide-up & collapse animations
            val isFullScreenAudioOpen by AudioPlaybackManager.isFullScreenOpen.collectAsState()
            AnimatedVisibility(
                visible = isFullScreenAudioOpen,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 380,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.12f, 0.95f, 0.22f, 1.0f)
                    )
                ) + androidx.compose.animation.scaleIn(
                    initialScale = 0.96f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 340,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 260,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 360,
                        easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
                    )
                ) + androidx.compose.animation.scaleOut(
                    targetScale = 0.96f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 320,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 300,
                        delayMillis = 60,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // Home runs under a portrait-forced LocalConfiguration; the full-screen audio player must see the real
                // rotation so its landscape layout (artwork/lyrics | controls) can switch on.
                val realConfig = com.example.ui.theme.LocalRealConfiguration.current
                    ?: androidx.compose.ui.platform.LocalConfiguration.current
                CompositionLocalProvider(androidx.compose.ui.platform.LocalConfiguration provides realConfig) {
                    AudioPlayerScreen(
                        isDark = isDark,
                        glassBlurTransparency = uiState.glassBlurTransparency,
                        onCollapse = { AudioPlaybackManager.collapseToMiniPlayer() }
                    )
                }
            }
        }
    }
}

/**
 * Folder Card Styled according to reference:
 * - Rounded folder icon (light blue background, purple/blue folder glyph) on the left
 * - Folder name in bold
 * - Full folder path in smaller gray text below the name
 * - Two small pill/chip badges showing "X Videos" and total size (e.g. "1.4 GB")
 * - A small red circular badge on the top-right corner of the folder icon showing count of new/unwatched videos (if any)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoFolderCard(
    folder: VideoFolder,
    isDark: Boolean,
    visibleFields: VisibleFields = VisibleFields(),
    isSelected: Boolean = false,
    isRenaming: Boolean = false,
    onConfirmRename: ((String) -> Unit)? = null,
    onCancelRename: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val surfaceColor = if (isDark) {
        if (isSelected) Color(0xFF1E1B4B).copy(alpha = 0.90f) else DarkGlassSurface
    } else {
        if (isSelected) Color(0xFFEEF2FF).copy(alpha = 0.95f) else LightGlassSurface
    }
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Remove Card UI (Settings > Appearance > Display & Feel): no card, a selected folder keeps its highlight
            .cardSurface(keepWhenSelected = isSelected) {
                this
                    .shadow(
                        elevation = if (isSelected) 8.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = AccentSkyBlue.copy(alpha = 0.35f),
                        spotColor = com.example.ui.theme.AccentPink.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, AccentGradient, RoundedCornerShape(16.dp))
                        } else {
                            Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        }
                    )
            }
            .then(
                if (!isRenaming) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon using custom ModernFolderIcon with glowing gradient & new count badge
            ModernFolderIcon(
                isCustom = folder.isCustom,
                newCount = folder.newVideosCount,
                showNewBadge = folder.newVideosCount > 0 && visibleFields.showNewBadge,
                isDark = isDark,
                isSelected = isSelected,
                size = com.example.ui.components.folderRowIconSize()
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Folder details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Folder Name (Bold)
                if (isRenaming) {
                    InlineRenameField(
                        initialText = folder.name,
                        onConfirm = { newName -> onConfirmRename?.invoke(newName) },
                        onCancel = { onCancelRename?.invoke() },
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                } else {
                    Text(
                        text = folder.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = if (visibleFields.showFullName) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (visibleFields.showPath && folder.path.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    // Full Path in smaller gray text
                    Text(
                        text = folder.path,
                        fontSize = 11.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (visibleFields.showVideoCount || visibleFields.showSize || visibleFields.showResolution || visibleFields.showDuration || folder.isCustom) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pill/chip badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Video count chip
                        if (visibleFields.showVideoCount) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.viaura_video),
                                        contentDescription = null,
                                        tint = AccentSkyBlue,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${folder.videoCount} Videos",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                }
                            }
                        }

                        // Subfolders count chip if folder has nested folders
                        if (folder.subFolders.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StyledIcon(
                                imageVector = Icons.Outlined.Folder,
                                        contentDescription = null,
                                        tint = AccentMid,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${folder.subFolders.size} Folders",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                }
                            }
                        }

                        // Total Size Chip
                        if (visibleFields.showSize) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = folder.formattedSize,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText
                                )
                            }
                        }

                        // Resolution Chip (e.g. 1080p, 4K)
                        if (visibleFields.showResolution) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Color(0xFF312E81).copy(alpha = 0.7f) else Color(0xFFEEF2FF)))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Color(0xFF4338CA).copy(alpha = 0.5f) else Color(0xFFC7D2FE)),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = folder.maxResolution,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA)
                                )
                            }
                        }

                        // Duration Chip
                        if (visibleFields.showDuration && folder.totalDurationFormatted != "00:00") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = folder.totalDurationFormatted,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText
                                )
                            }
                        }

                        // Date Chip
                        if (visibleFields.showDate && folder.formattedDate.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = folder.formattedDate,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText
                                )
                            }
                        }

                        // Custom folder tag if created by user
                        if (folder.isCustom) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentMid.copy(alpha = 0.20f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Custom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentMid
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tree View Folder Card (System-like hierarchical directory tree)
 * Displays main parent folders with expandable branches for nested subfolders and video files.
 */
@Composable
fun TreeFolderCard(
    folder: VideoFolder,
    isDark: Boolean,
    visibleFields: VisibleFields = VisibleFields(),
    onOpenFolder: (VideoFolder) -> Unit,
    onOpenSubFolder: (VideoFolder) -> Unit,
    onPlayVideo: (VideoItem) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface {
                this
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Root Parent Folder Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand / Collapse Chevron Toggle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.6f) else Neutral.E2E8F0.copy(alpha = 0.8f))
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Tree" else "Expand Tree",
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Modern Folder Icon
                ModernFolderIcon(
                    isCustom = folder.isCustom,
                    newCount = folder.newVideosCount,
                    showNewBadge = folder.newVideosCount > 0,
                    isDark = isDark,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Folder Name & Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = folder.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val stats = buildList {
                        if (folder.subFolders.isNotEmpty()) add("${folder.subFolders.size} Subfolders")
                        add("${folder.allVideosCount} Videos")
                        if (visibleFields.showSize) add(folder.formattedSize)
                    }
                    Text(
                        text = stats.joinToString(" • "),
                        fontSize = 11.5.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Direct Open Action Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) AccentSkyBlueStrong.copy(alpha = 0.22f) else AccentContainerLight)
                        .border(1.dp, AccentSkyBlue.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                        .clickable { onOpenFolder(folder) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Open",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentSkyBlue
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        StyledIcon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // 2. Expandable Tree Branches (Subfolders & Leaf Videos)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // Tree Subfolders
                    folder.subFolders.forEach { subFolder ->
                        TreeSubFolderRow(
                            subFolder = subFolder,
                            isDark = isDark,
                            visibleFields = visibleFields,
                            onOpenSubFolder = { onOpenSubFolder(subFolder) },
                            onPlayVideo = onPlayVideo
                        )
                    }

                    // Direct Videos inside this root folder (if any)
                    folder.videos.forEach { video ->
                        TreeVideoRow(
                            video = video,
                            isDark = isDark,
                            onPlay = { onPlayVideo(video) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Subfolder node row in the Directory Tree
 */
@Composable
fun TreeSubFolderRow(
    subFolder: VideoFolder,
    isDark: Boolean,
    visibleFields: VisibleFields = VisibleFields(),
    onOpenSubFolder: () -> Unit,
    onPlayVideo: (VideoItem) -> Unit
) {
    var isSubExpanded by remember { mutableStateOf(false) }
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.50f) else Neutral.F8FAFC.copy(alpha = 0.70f))
                .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clickable { onOpenSubFolder() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connector branch icon
            StyledIcon(
                                imageVector = Icons.Outlined.SubdirectoryArrowRight,
                contentDescription = null,
                tint = AccentSkyBlue,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Subfolder icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentMid.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                                imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    tint = AccentMid,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Subfolder details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subFolder.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${subFolder.videoCount} Videos • ${subFolder.formattedSize}",
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }

            // Expand Videos toggle in tree
            if (subFolder.videos.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Neutral.D334155.copy(alpha = 0.6f) else Neutral.E2E8F0)
                        .clickable { isSubExpanded = !isSubExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                                imageVector = if (isSubExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isSubExpanded) "Collapse Subfolder" else "Expand Subfolder",
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Arrow to navigate directly inside
            StyledIcon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = "Open Subfolder",
                tint = secondaryText.copy(alpha = 0.5f),
                modifier = Modifier.size(11.dp)
            )
        }

        // Subfolder Videos (if expanded)
        AnimatedVisibility(
            visible = isSubExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, top = 4.dp)
            ) {
                subFolder.videos.forEach { video ->
                    TreeVideoRow(
                        video = video,
                        isDark = isDark,
                        onPlay = { onPlayVideo(video) }
                    )
                }
            }
        }
    }
}

/**
 * Leaf Video file row in the Directory Tree
 */
@Composable
fun TreeVideoRow(
    video: VideoItem,
    isDark: Boolean,
    onPlay: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Neutral.D0F172A.copy(alpha = 0.40f) else Neutral.F1F5F9.copy(alpha = 0.50f))
            .border(0.8.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable { onPlay() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Video icon with dark play badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AccentSkyBlueStrong.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = AccentSkyBlue,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = video.durationFormatted,
                    fontSize = 10.5.sp,
                    color = AccentSkyBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = " • ${video.formattedSize}",
                    fontSize = 10.5.sp,
                    color = secondaryText
                )
                if (video.effectiveSubtitleFormats.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    video.effectiveSubtitleFormats.forEach { formatName ->
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1))
                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = formatName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Play icon button
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(AccentGradient),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play Video",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * Grid View Card for Video Folders
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoFolderGridCard(
    folder: VideoFolder,
    isDark: Boolean,
    visibleFields: VisibleFields = VisibleFields(),
    isSelected: Boolean = false,
    isRenaming: Boolean = false,
    onConfirmRename: ((String) -> Unit)? = null,
    onCancelRename: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val surfaceColor = if (isDark) {
        if (isSelected) Color(0xFF1E1B4B).copy(alpha = 0.90f) else DarkGlassSurface
    } else {
        if (isSelected) Color(0xFFEEF2FF).copy(alpha = 0.95f) else LightGlassSurface
    }
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Remove Card UI (Settings > Appearance > Display & Feel): no card, a selected folder keeps its highlight
            .cardSurface(keepWhenSelected = isSelected) {
                this
                    .shadow(
                        elevation = if (isSelected) 8.dp else 0.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = AccentSkyBlue.copy(alpha = 0.35f),
                        spotColor = com.example.ui.theme.AccentPink.copy(alpha = 0.45f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, AccentGradient, RoundedCornerShape(16.dp))
                        } else {
                            Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        }
                    )
            }
            .then(
                if (!isRenaming) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Folder Icon using ModernFolderIcon with new badge
            ModernFolderIcon(
                isCustom = folder.isCustom,
                newCount = folder.newVideosCount,
                showNewBadge = folder.newVideosCount > 0 && visibleFields.showNewBadge,
                isDark = isDark,
                isSelected = isSelected,
                size = 56.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isRenaming) {
                InlineRenameField(
                    initialText = folder.name,
                    onConfirm = { newName -> onConfirmRename?.invoke(newName) },
                    onCancel = { onCancelRename?.invoke() },
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            } else {
                Text(
                    text = folder.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = if (visibleFields.showFullName) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            if (visibleFields.showVideoCount || visibleFields.showSize || visibleFields.showResolution || visibleFields.showDuration || folder.subFolders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val subtitleParts = mutableListOf<String>()
                if (visibleFields.showVideoCount) subtitleParts.add("${folder.videoCount} vids")
                if (folder.subFolders.isNotEmpty()) subtitleParts.add("${folder.subFolders.size} folders")
                if (visibleFields.showResolution) subtitleParts.add(folder.maxResolution)
                if (visibleFields.showDuration && folder.totalDurationFormatted != "00:00") subtitleParts.add(folder.totalDurationFormatted)
                if (visibleFields.showSize) subtitleParts.add(folder.formattedSize)

                Text(
                    text = subtitleParts.joinToString(" • "),
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

/**
 * Liquid Glass Folder Creation Dialog
 */
@Composable
fun CreateFolderDialog(
    isDark: Boolean,
    transparencyPercent: Float = 65f,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val categories = listOf("Movies", "Anime", "Series", "Screen Recordings", "Downloads", "Family", "Clips")
    var selectedCategory by remember { mutableStateOf("") }

    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val alphaRatio = (transparencyPercent / 100f).coerceIn(0.15f, 1.0f)
    val inputContainerColor = if (isDark) {
        Neutral.D0F172A.copy(alpha = (0.25f + 0.45f * alphaRatio).coerceIn(0.15f, 0.85f))
    } else {
        Neutral.FFFFFF.copy(alpha = (0.30f + 0.50f * alphaRatio).coerceIn(0.20f, 0.85f))
    }

    LiquidGlassDialog(
        isDark = isDark,
        onDismissRequest = onDismiss
    ) {
        LiquidGlassCard(
            isDark = isDark,
            transparencyPercent = transparencyPercent,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "New Custom Folder",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = "Organize videos in your library",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Folder Name input
                Text(
                    text = "Folder Name",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentSkyBlue
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = { Text("e.g. My Favorites", color = secondaryText, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText,
                        focusedContainerColor = inputContainerColor,
                        unfocusedContainerColor = inputContainerColor,
                        cursorColor = Color(0xFF00E5FF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category presets
                Text(
                    text = "Quick Suggestion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentSkyBlue
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        val isSelected = selectedCategory == cat || folderName.equals(cat, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentSkyBlue.copy(alpha = 0.25f) else (if (isDark) Neutral.D1E293B else Neutral.E2E8F0))
                                .border(1.dp, if (isSelected) AccentSkyBlue else borderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategory = cat
                                    folderName = cat
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentSkyBlue else primaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.60f) else Neutral.E2E8F0)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (folderName.isNotBlank()) AccentGradient else Brush.linearGradient(listOf(Neutral.S64748B, Neutral.S475569)))
                            .clickable(enabled = folderName.isNotBlank()) {
                                onCreate(folderName.trim())
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass Quick Resume Dialog
 */
@Composable
fun LastWatchedResumeDialog(
    isDark: Boolean,
    transparencyPercent: Float = 65f,
    lastRecord: com.example.util.VideoPlaybackRecord? = null,
    onDismiss: () -> Unit,
    onResumePlay: () -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    val displayTitle = lastRecord?.title?.ifBlank { "Sample Video" } ?: "Sample Video"
    val posFormatted = lastRecord?.formattedPosition ?: "00:00"
    val durFormatted = lastRecord?.formattedDuration ?: "00:00"
    val percent = ((lastRecord?.progressFraction ?: 0f) * 100).toInt()

    LiquidGlassDialog(
        isDark = isDark,
        onDismissRequest = onDismiss
    ) {
        LiquidGlassCard(
            isDark = isDark,
            transparencyPercent = transparencyPercent,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                                imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Resume Playback",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = "Continue where you left off",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.50f) else Neutral.F1F5F9.copy(alpha = 0.60f))
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = null,
                                tint = AccentSkyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lastRecord != null && lastRecord.positionMs > 0L) {
                                "Stopped at $posFormatted / $durFormatted ($percent% completed)"
                            } else {
                                "Ready to play from start • Tap Resume to watch"
                            },
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )

                        if (lastRecord != null && lastRecord.progressFraction > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isDark) Neutral.D334155 else Neutral.E2E8F0)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(lastRecord.progressFraction)
                                        .background(AccentSkyBlue)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.60f) else Neutral.E2E8F0)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGradient)
                            .clickable { onResumePlay() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StyledIcon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lastRecord != null && lastRecord.positionMs > 0L) "Resume Play" else "Play Video",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Selection Mode Top & Bottom Floating Bars & Action Sheet
// ---------------------------------------------------------------------------

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    isFolderSelection: Boolean,
    isDark: Boolean,
    alphaRatio: Float,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onInfo: (() -> Unit)? = null,
    showSelectAll: Boolean = true
) {
    val barBg = if (isDark) {
        if (alphaRatio >= 0.99f) Neutral.D0F172A
        else Neutral.D0F172A.copy(alpha = (0.70f + 0.30f * alphaRatio).coerceIn(0.40f, 1.0f))
    } else {
        if (alphaRatio >= 0.99f) Neutral.FFFFFF
        else Neutral.FFFFFF.copy(alpha = (0.75f + 0.25f * alphaRatio).coerceIn(0.45f, 1.0f))
    }
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassToolbarOr(RoundedCornerShape(20.dp), isDark) {
                clip(RoundedCornerShape(20.dp))
                    .background(barBg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(38.dp)
            ) {
                StyledIcon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close Selection",
                    modifier = Modifier.size(22.dp),
                    tint = primaryText
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "$selectedCount selected",
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                modifier = Modifier.weight(1f)
            )

            if (onInfo != null && selectedCount == 1) {
                IconButton(
                    onClick = onInfo,
                    modifier = Modifier.size(38.dp)
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Media Information",
                        modifier = Modifier.size(22.dp),
                        tint = AccentSkyBlue
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
            }

            if (showSelectAll) {
                IconButton(
                    onClick = onSelectAll,
                    modifier = Modifier.size(38.dp)
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.SelectAll,
                        contentDescription = "Select All",
                        modifier = Modifier.size(22.dp),
                        tint = AccentSkyBlue
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(38.dp)
            ) {
                StyledIcon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(22.dp),
                    tint = AccentPink
                )
            }
        }
    }
}

@Composable
fun BottomSelectionBar(
    isDark: Boolean,
    alphaRatio: Float,
    canRename: Boolean,
    canDelete: Boolean,
    canMove: Boolean,
    canCopy: Boolean,
    canPlaylist: Boolean,
    playlistIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    playlistIconRes: Int? = R.drawable.viaura_playlist_video,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onPlaylist: () -> Unit,
    showPlaylist: Boolean = true
) {
    val barBg = if (isDark) {
        if (alphaRatio >= 0.99f) Neutral.D0F172A
        else Neutral.D0F172A.copy(alpha = (0.70f + 0.30f * alphaRatio).coerceIn(0.40f, 1.0f))
    } else {
        if (alphaRatio >= 0.99f) Neutral.FFFFFF
        else Neutral.FFFFFF.copy(alpha = (0.75f + 0.25f * alphaRatio).coerceIn(0.45f, 1.0f))
    }
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    // Same size + corner radius as SelectionTopBar (38dp icon + 8dp*2 padding = 54dp, 20dp corners)
    val barShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .liquidGlassToolbarOr(barShape, isDark) {
                shadow(
                    elevation = 18.dp,
                    shape = barShape,
                    ambientColor = AccentSkyBlue.copy(alpha = 0.35f),
                    spotColor = AccentPink.copy(alpha = 0.40f)
                )
                    .clip(barShape)
                    .background(barBg)
                    .border(
                        width = 1.5.dp,
                        brush = AccentGradient,
                        shape = barShape
                    )
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomSelectionBarItem(
                icon = Icons.Outlined.DriveFileRenameOutline,
                label = "Rename",
                tint = if (canRename) primaryText else primaryText.copy(alpha = 0.35f),
                enabled = canRename,
                onClick = onRename
            )
            BottomSelectionBarItem(
                icon = Icons.Outlined.DeleteOutline,
                label = "Delete",
                tint = if (canDelete) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.35f),
                enabled = canDelete,
                onClick = onDelete
            )
            BottomSelectionBarItem(
                icon = Icons.Outlined.DriveFileMove,
                label = "Move",
                tint = if (canMove) AccentSkyBlue else AccentSkyBlue.copy(alpha = 0.35f),
                enabled = canMove,
                onClick = onMove
            )
            BottomSelectionBarItem(
                icon = Icons.Outlined.ContentCopy,
                label = "Copy",
                tint = if (canCopy) AccentMid else AccentMid.copy(alpha = 0.35f),
                enabled = canCopy,
                onClick = onCopy
            )
            if (showPlaylist) {
                BottomSelectionBarItem(
                    icon = playlistIcon,
                    drawableRes = if (playlistIcon == null) (playlistIconRes ?: R.drawable.viaura_playlist_video) else null,
                    label = "Playlist",
                    tint = if (canPlaylist) AccentPink else AccentPink.copy(alpha = 0.35f),
                    enabled = canPlaylist,
                    onClick = onPlaylist
                )
            }
        }
    }
}

@Composable
fun BottomSelectionBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    drawableRes: Int? = null,
    label: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        if (drawableRes != null && drawableRes != 0) {
            StyledIcon(
                drawableRes = drawableRes,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        } else if (icon != null) {
            StyledIcon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
            color = tint
        )
    }
}

@Composable
fun BottomDeleteConfirmationBar(
    title: String,
    message: String,
    count: Int,
    isDark: Boolean,
    alphaRatio: Float,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    itemNames: List<String> = emptyList()
) {
    // Bottom sheet (system style) instead of an inline bar: confirm before anything is deleted.
    val text = if (message.startsWith("Permanently delete")) {
        "This action cannot be undone. The selected ${if (count > 1) "items" else "item"} will be permanently deleted."
    } else message
    com.example.ui.components.DeleteConfirmSheet(
        title = title,
        message = text,
        itemNames = itemNames,
        isDark = isDark,
        onDismiss = onCancel,
        onConfirm = onConfirm
    )
}

@Composable
fun RenameDialog(
    title: String,
    initialText: String,
    isDark: Boolean,
    alphaRatio: Float,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialText) }
    val dialogBg = glassPanelColor(isDark, LocalPanelTransparency.current)
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(26.dp))
                .background(dialogBg)
                .border(1.5.dp, AccentGradient, RoundedCornerShape(26.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AccentSkyBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_pen_2,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.8f) else Neutral.F1F5F9)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = com.example.ui.theme.LocalAppFontFamily.current,
                            color = primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(AccentSkyBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AccentGradient)
                            .clickable(enabled = textValue.isNotBlank()) {
                                onConfirm(textValue.trim())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rename",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    isDark: Boolean,
    alphaRatio: Float,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    itemNames: List<String> = emptyList(),
    confirmLabel: String = if (title.startsWith("Clear")) "Clear" else "Delete"
) {
    // Always a bottom sheet now (never a centered dialog).
    com.example.ui.components.DeleteConfirmSheet(
        title = title,
        message = message,
        itemNames = itemNames,
        isDark = isDark,
        confirmLabel = confirmLabel,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

/**
 * Slash Search Finder Content for /<playlist_or_folder_name>
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlashPlaylistFinderContent(
    searchQuery: String,
    rawSlashTerm: String,
    matchedPlaylists: List<CustomPlaylist>,
    matchedFolders: List<VideoFolder>,
    matchedPrivateFolders: List<PrivateFolder> = emptyList(),
    allVideos: List<VideoItem>,
    isDark: Boolean,
    onOpenPlaylist: (CustomPlaylist) -> Unit,
    onPlaylistLongClick: (CustomPlaylist) -> Unit = {},
    onOpenFolder: (VideoFolder) -> Unit,
    onOpenPrivateFolder: (PrivateFolder) -> Unit = {},
    onCreatePlaylist: (String) -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Header Tag
        item(contentType = "search_banner") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentMid.copy(alpha = if (isDark) 0.15f else 0.10f))
                    .border(1.dp, AccentMid.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AccentGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "/",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAYLIST & FOLDER FINDER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentMid
                    )
                    Text(
                        text = if (rawSlashTerm.isBlank()) "Showing all custom playlists and storage folders"
                        else "Filtering playlists and folders matching \"$rawSlashTerm\"",
                        fontSize = 11.sp,
                        color = secondaryText
                    )
                }
            }
        }

        // 0. Private Vault Folders (if authenticated or unlocked via slash password)
        if (matchedPrivateFolders.isNotEmpty()) {
            item(contentType = "section_header_private") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRIVATE VAULT FOLDERS (${matchedPrivateFolders.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            items(
                items = matchedPrivateFolders,
                key = { "slash_priv_${it.id}" },
                contentType = { "private_folder_card" }
            ) { privFolder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.7f) else Color(0xFFEEF2FF))
                        .border(1.dp, AccentMid.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable { onOpenPrivateFolder(privFolder) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernFolderIcon(
                        isDark = isDark,
                        size = 44.dp,
                        isCustom = true,
                        glyphType = if (privFolder.mediaType == PrivateMediaType.AUDIO) FolderGlyphType.AUDIO else FolderGlyphType.VIDEO,
                        showBadge = false
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = privFolder.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${privFolder.itemCount} ${if (privFolder.mediaType == PrivateMediaType.VIDEO) "Videos" else "Tracks"} • ${privFolder.formattedSize} • Created: ${privFolder.formattedDate}",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }

                    StyledIcon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Open Private Folder",
                        tint = secondaryText.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 1. Playlists Section
        if (matchedPlaylists.isNotEmpty()) {
            item(contentType = "section_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.PlaylistPlay,
                        contentDescription = null,
                        tint = AccentMid,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PLAYLISTS (${matchedPlaylists.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            items(
                items = matchedPlaylists,
                key = { "slash_pl_${it.id}" },
                contentType = { "playlist_card" }
            ) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.6f) else Color(0xFFEEF2FF))
                        .border(1.dp, AccentMid.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = { onOpenPlaylist(playlist) },
                            onLongClick = { onPlaylistLongClick(playlist) }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernFolderIcon(
                        isDark = isDark,
                        size = 44.dp,
                        isCustom = true,
                        showBadge = false,
                        showVideoGlyph = false,
                        iconKind = if (playlist.isAudio) com.example.ui.components.FolderIconKind.PLAYLIST_AUDIO else com.example.ui.components.FolderIconKind.PLAYLIST_VIDEO
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${playlist.videoPaths.size} Videos • ${playlist.formattedDate}",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Open Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Storage Folders Section
        if (matchedFolders.isNotEmpty()) {
            item(contentType = "section_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STORAGE FOLDERS (${matchedFolders.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            items(
                items = matchedFolders,
                key = { "slash_folder_${it.id}_${it.path}" },
                contentType = { "folder_card" }
            ) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.6f) else Neutral.F8FAFC)
                        .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { onOpenFolder(folder) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernFolderIcon(
                        isDark = isDark,
                        size = 44.dp,
                        isCustom = folder.isCustom,
                        showBadge = false,
                        showVideoGlyph = false
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${folder.videoCount} Videos • ${folder.formattedSize}",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }

                    StyledIcon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Open Folder",
                        tint = secondaryText.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Empty State if neither found
        if (matchedPlaylists.isEmpty() && matchedFolders.isEmpty() && matchedPrivateFolders.isEmpty()) {
            item(contentType = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0),
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.PlaylistAdd,
                                contentDescription = null,
                                tint = AccentSkyBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (rawSlashTerm.isNotBlank()) "No playlist or folder named \"/$rawSlashTerm\"" else "No playlists created yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Type any folder name after / (e.g. /Op Anime) to jump directly into it.",
                            fontSize = 12.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )

                        if (rawSlashTerm.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { onCreatePlaylist(rawSlashTerm) },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentGradient)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    StyledIcon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Create \"$rawSlashTerm\" Playlist",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderInformationSheet(
    folder: VideoFolder,
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val surface = if (isDark) DarkGlassSurface else LightGlassSurface
    val border = if (isDark) DarkGlassBorder else LightGlassBorder
    var actualSize by remember(folder.path) { mutableStateOf(folder.totalSizeBytes) }
    var createdText by remember(folder.path) { mutableStateOf(folder.formattedDate) }
    var formatSummary by remember(folder.path) { mutableStateOf("") }
    var expanded by remember(folder.path) { mutableStateOf(true) }
    val allVideos = remember(folder.path) { folder.getAllVideos() }

    LaunchedEffect(folder.path) {
        val result = withContext(Dispatchers.IO) {
            var size = folder.totalSizeBytes
            var created = folder.formattedDate
            try {
                val root = File(folder.path)
                if (root.exists()) {
                    size = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    val attrs = java.nio.file.Files.readAttributes(
                        root.toPath(),
                        java.nio.file.attribute.BasicFileAttributes::class.java
                    )
                    val createdMs = attrs.creationTime().toMillis()
                    if (createdMs > 0) {
                        created = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(java.util.Date(createdMs))
                    }
                }
            } catch (_: Throwable) {}
            val grouped = allVideos.groupingBy {
                it.displayName.substringAfterLast('.', "unknown").uppercase(Locale.ROOT)
            }.eachCount()
            Triple(size, created, if (grouped.isEmpty()) "No videos" else grouped.entries.sortedBy { it.key }.joinToString(" • ") { "${it.value} ${it.key}" })
        }
        actualSize = result.first
        createdText = result.second
        formatSummary = result.third
    }

    val alpha = (glassBlurTransparency / 100f).coerceIn(.15f, 1f)
    BackHandler(onBack = onDismiss)
    // Own dialog window (like the Delete / Download / Playlist sheets): Liquid Glass can blur the screen behind it and the
    // sheet reaches the real bottom edge of the display.
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
    com.example.ui.components.EdgeToEdgeSheetWindowEffect()
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = slideInVertically(animationSpec = tween(340)) { it } + fadeIn(tween(220)),
        exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        modifier = Modifier.fillMaxSize()
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetBg = glassPanelColor(isDark, LocalPanelTransparency.current)
            val sheetInteractionSource = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = sheetInteractionSource,
                        indication = null
                    ) { /* Consume touch events so backdrop doesn't trigger */ }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(sheetBg)
                    .glassCardBorder(1.dp, border, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(AccentGradient), contentAlignment = Alignment.Center) {
                        StyledIcon(imageVector = Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(folder.name, color = primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(folder.path, color = secondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(14.dp))
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        InfoStatGrid(
                            listOf(
                                "Folder Size" to formatFolderSize(actualSize),
                                "Video Count" to allVideos.size.toString(),
                                "Video Formats" to formatSummary,
                                "Total Duration" to formatFolderDuration(allVideos.sumOf { it.durationMs }),
                                "Created" to createdText
                            ),
                            primary, secondary, surface, border
                        )
                    }
                    if (allVideos.isNotEmpty()) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { expanded = !expanded }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Media inside", color = primary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape)
                                        .background(if (isDark) Color.White.copy(alpha = .08f) else Color.Black.copy(alpha = .06f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    StyledIcon(
                                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        modifier = Modifier.size(20.dp),
                                        tint = secondary
                                    )
                                }
                            }
                        }
                        if (expanded) {
                            items(allVideos.take(60), key = { it.path }) { item ->
                                FolderMediaPreviewRow(item, isDark)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun FolderMediaPreviewRow(video: VideoItem, isDark: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bitmap by remember(video.path) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(video.path) {
        bitmap = loadVideoThumbnail(
            context, video.id, video.uri, video.path,
            ThumbnailStrategy.SMART, ThumbnailQuality.MEDIUM, false
        )
    }
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color.White.copy(alpha = .045f) else Color.Black.copy(alpha = .035f))
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(62.dp, 38.dp).clip(RoundedCornerShape(9.dp)).background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0), contentAlignment = Alignment.Center) {
            if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else StyledIcon(imageVector = Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(19.dp), tint = secondary)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(video.displayName, color = primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${video.extensionTag} • ${video.durationFormatted} • ${video.formattedSize}", color = secondary, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AudioFolderInformationSheet(
    folder: AudioFolderItem,
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val surface = if (isDark) DarkGlassSurface else LightGlassSurface
    val border = if (isDark) DarkGlassBorder else LightGlassBorder
    val alpha = (glassBlurTransparency / 100f).coerceIn(.15f, 1f)
    BackHandler(onBack = onDismiss)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
    com.example.ui.components.EdgeToEdgeSheetWindowEffect()
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(tween(340)) { it } + fadeIn(tween(220)),
        exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        modifier = Modifier.fillMaxSize()
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetInteractionSource = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = sheetInteractionSource,
                        indication = null
                    ) { /* Consume touch events */ }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(glassPanelColor(isDark, LocalPanelTransparency.current))
                    .glassCardBorder(1.dp, border, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(AccentGradient), contentAlignment = Alignment.Center) {
                        StyledIcon(imageVector = Icons.Outlined.MusicNote, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(folder.name, color = primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(folder.path, color = secondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(14.dp))
                InfoStatGrid(
                    listOf(
                        "Audio Files" to folder.trackCount.toString(),
                        "Folder Size" to folder.formattedSize,
                        "Total Duration" to folder.formattedDuration,
                        "Modified" to folder.formattedDate.ifBlank { "—" }
                    ),
                    primary, secondary, surface, border
                )
            }
        }
    }
    }
}

@Composable
private fun InfoStatGrid(
    items: List<Pair<String, String>>,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).border(1.dp, border, RoundedCornerShape(18.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEach { (key, value) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(key, color = secondary, fontSize = 11.5.sp, modifier = Modifier.width(115.dp))
                Text(value.ifBlank { "—" }, color = primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun formatFolderSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) { value /= 1024.0; index++ }
    return "%.1f %s".format(Locale.US, value, units[index])
}

private fun formatFolderDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val total = ms / 1000
    return if (total >= 3600) "%02d:%02d:%02d".format(Locale.US, total / 3600, (total % 3600) / 60, total % 60)
    else "%02d:%02d".format(Locale.US, total / 60, total % 60)
}
