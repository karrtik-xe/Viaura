package com.example.ui.screens

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentSkyBlueStrong
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.components.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.player.AudioMetadataExtractor
import com.example.player.AudioPlaybackManager
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.BottomMoveCopyProgressBar
import com.example.ui.components.playedProgressFill
import com.example.ui.components.DirectoryPickerAction
import com.example.ui.components.FloatingDirectoryPickerSheet
import com.example.ui.components.FolderGlyphType
import com.example.ui.components.InlineRenameField
import com.example.ui.components.LayoutMode
import com.example.ui.components.LiquidPullToRefresh
import com.example.ui.components.ModernFolderIcon
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.StyledIcon
import com.example.util.FolderBlockListManager
import com.example.util.PrivateVaultManager
import com.example.ui.components.ThumbnailCache
import com.example.ui.components.ViewMode
import com.example.ui.components.VisibleFields
import com.example.ui.components.hasAllFilesAccess
import com.example.ui.components.loadAudioThumbnail
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.AudioFileManager
import com.example.util.AudioLibraryCache
import com.example.util.accessibleStorageRoots
import androidx.compose.material.icons.filled.Cloud
import com.example.ui.components.OnlineMusicPanel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Music & Audio Tab Content.
 * Features:
 * - 1:1 Unified design matching Video Home Screen
 * - ModernFolderIcon with dedicated Audio Glyph
 * - Exact VideoFolderCard styling, sizes, chip pills, and typography
 * - Exact VideoItemCard audio track cards with thumbnail, duration badge, format chips, and equalizer
 * - Full Selection Mode (select folders and select audio tracks)
 * - Rename folder and audio track with InlineRenameField & touchable fix
 * - Instant 0ms optimistic UI delete
 * - Full Liquid Pull-to-Refresh
 */
@Composable
fun AudioMusicTabContent(
    isDark: Boolean,
    searchQuery: String = "",
    sortField: SortField = SortField.TITLE,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    viewMode: ViewMode = ViewMode.FOLDER,
    layoutMode: LayoutMode = LayoutMode.LIST,
    visibleFields: VisibleFields = VisibleFields(),
    folderSortField: SortField = sortField,
    folderSortDirection: SortDirection = sortDirection,
    folderLayoutMode: LayoutMode = layoutMode,
    folderVisibleFields: VisibleFields = visibleFields,
    contentSortField: SortField = sortField,
    contentSortDirection: SortDirection = sortDirection,
    contentLayoutMode: LayoutMode = layoutMode,
    contentVisibleFields: VisibleFields = visibleFields,
    selectedFolder: AudioFolderItem? = null,
    onSelectFolder: (AudioFolderItem?) -> Unit = {},
    onFolderStateChange: (isInsideFolder: Boolean) -> Unit = {},
    onSelectionModeChange: (isSelecting: Boolean, count: Int, isFolder: Boolean, onClear: () -> Unit, onSelectAll: () -> Unit, onShare: () -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onInfoAction: ((() -> Unit) -> Unit) = {},
    onInfo: (AudioFolderItem?, AudioTrackItem?) -> Unit = { _, _ -> },
    onOpenChannel: ((com.example.util.OnlineChannelResult) -> Unit)? = null,
    onOpenSharedPlaylist: ((com.example.util.OnlinePlaylistDetail) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val allTracks = remember {
        mutableStateListOf<AudioTrackItem>().apply {
            val cached = AudioLibraryCache.loadCachedTracks(context)
            if (!cached.isNullOrEmpty()) {
                addAll(cached)
            }
        }
    }
    var isScanning by remember { mutableStateOf(false) }

    val currentPlayingTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isPlaying by AudioPlaybackManager.isPlaying.collectAsState()

    // Selection States
    val selectedFolderPaths = remember { mutableStateListOf<String>() }
    val selectedTrackPaths = remember { mutableStateListOf<String>() }
    var renamingFolderPath by remember { mutableStateOf<String?>(null) }
    var renamingTrackPath by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDirectoryPickerSheet by remember { mutableStateOf(false) }
    var directoryPickerAction by remember { mutableStateOf(DirectoryPickerAction.MOVE) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    var isMoveCopyInProgress by remember { mutableStateOf(false) }
    var moveCopyProgressFraction by remember { mutableStateOf(0f) }
    var moveCopyIsMove by remember { mutableStateOf(true) }
    var moveCopyIsDelete by remember { mutableStateOf(false) }
    var moveCopyItemName by remember { mutableStateOf("") }
    var moveCopyCompletedCount by remember { mutableStateOf(0) }
    var moveCopyTotalCount by remember { mutableStateOf(0) }

    val isFolderSelection = selectedFolderPaths.isNotEmpty()
    val isTrackSelection = selectedTrackPaths.isNotEmpty()
    val isSelectionMode = isFolderSelection || isTrackSelection || isMoveCopyInProgress
    val totalSelectedCount = selectedFolderPaths.size + selectedTrackPaths.size

    fun clearSelection() {
        selectedFolderPaths.clear()
        selectedTrackPaths.clear()
        renamingFolderPath = null
        renamingTrackPath = null
        showDeleteConfirmDialog = false
    }

    // Handle system back navigation
    BackHandler(enabled = renamingFolderPath != null || renamingTrackPath != null || showDeleteConfirmDialog || isSelectionMode || selectedFolder != null) {
        if (renamingFolderPath != null || renamingTrackPath != null) {
            renamingFolderPath = null
            renamingTrackPath = null
        } else if (showDeleteConfirmDialog) {
            showDeleteConfirmDialog = false
        } else if (isSelectionMode) {
            clearSelection()
        } else {
            onSelectFolder(null)
        }
    }

    LaunchedEffect(selectedFolder) {
        onFolderStateChange(selectedFolder != null)
        clearSelection()
    }

    fun scanAudioFiles(force: Boolean = false, showIndicator: Boolean = false) {
        if (showIndicator) isScanning = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val list = AudioLibraryCache.getOrScanAudio(context, force = force)
                withContext(Dispatchers.Main) {
                    if (allTracks.toList() != list) {
                        val newPaths = list.map { it.path }.toSet()
                        allTracks.removeAll { it.path !in newPaths }
                        for (i in list.indices) {
                            val item = list[i]
                            val existingIndex = allTracks.indexOfFirst { it.path == item.path }
                            if (existingIndex == -1) {
                                allTracks.add(i.coerceAtMost(allTracks.size), item)
                            } else if (allTracks[existingIndex] != item) {
                                allTracks[existingIndex] = item
                            }
                        }
                        for (i in list.indices) {
                            if (i < allTracks.size && allTracks[i].path != list[i].path) {
                                val targetIdx = allTracks.indexOfFirst { it.path == list[i].path }
                                if (targetIdx != -1) {
                                    val el = allTracks.removeAt(targetIdx)
                                    allTracks.add(i, el)
                                }
                            }
                        }
                    }
                }
            } finally {
                if (showIndicator) {
                    withContext(Dispatchers.Main) { isScanning = false }
                }
            }
        }
    }

    // Initial/lifecycle/background MediaStore scans are deliberately silent. Only an explicit
    // pull-to-refresh gesture sets showIndicator=true, so file-manager changes appear without
    // the refresh animation flashing on screen.
    LaunchedEffect(Unit) {
        scanAudioFiles()
    }

    DisposableEffect(context, lifecycleOwner) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var debounceJob: kotlinx.coroutines.Job? = null
        val observer = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                super.onChange(selfChange, uri)
                debounceJob?.cancel()
                debounceJob = coroutineScope.launch {
                    kotlinx.coroutines.delay(600)
                    scanAudioFiles(force = true, showIndicator = false)
                }
            }
        }
        var registered = false
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
            )
            registered = true
        } catch (_: Throwable) {}

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scanAudioFiles(force = true, showIndicator = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            debounceJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (registered) {
                try { context.contentResolver.unregisterContentObserver(observer) } catch (_: Throwable) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        FolderBlockListManager.init(context)
        PrivateVaultManager.init(context)
    }

    val blockedAudioFolderPaths by FolderBlockListManager.blockedAudioFolderPaths
    val privateFoldersList by PrivateVaultManager.privateFolders
    val seenVersion by com.example.util.MediaSeenManager.seenVersion.collectAsState()

    // Filter tracks by search query & blocked/private state
    val searchedTracks = remember(allTracks.toList(), searchQuery, blockedAudioFolderPaths, privateFoldersList) {
        // Online tracks (online:// / http) never form local folders (previously a ghost "www.youtube.com" folder appeared)
        val unblocked = allTracks.filter { track ->
            val p = track.path
            if (com.example.util.isOnlineMediaPath(p)) return@filter false
            val folderPath = if (p.contains('/')) p.substringBeforeLast('/') else "Music"
            !blockedAudioFolderPaths.contains(folderPath) &&
            !PrivateVaultManager.isFolderPrivate(folderPath)
        }
        if (searchQuery.isBlank()) unblocked else {
            unblocked.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true) ||
                        it.format.contains(searchQuery, ignoreCase = true) ||
                        it.path.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Group into Folders
    val audioFolders = remember(searchedTracks, blockedAudioFolderPaths, privateFoldersList, seenVersion) {
        val groups = searchedTracks.groupBy { track ->
            val p = track.path
            if (p.contains('/')) p.substringBeforeLast('/') else "Music"
        }
        groups.filter { (folderPath, _) ->
            !blockedAudioFolderPaths.contains(folderPath) &&
            !PrivateVaultManager.isFolderPrivate(folderPath)
        }.map { (folderPath, tracks) ->
            val folderName = if (folderPath.contains('/')) folderPath.substringAfterLast('/') else folderPath
            val totalDur = tracks.sumOf { it.durationMs }
            val totalSize = tracks.sumOf { it.sizeBytes }
            val maxDate = tracks.maxOfOrNull { it.dateModified } ?: 0L
            val unplayedCount = tracks.count { track ->
                com.example.util.MediaSeenManager.isAudioTrackNew(context, track)
            }

            AudioFolderItem(
                name = folderName,
                path = folderPath,
                trackCount = tracks.size,
                totalDurationMs = totalDur,
                totalSizeBytes = totalSize,
                dateModified = maxDate,
                isNew = unplayedCount > 0,
                unplayedCount = unplayedCount
            )
        }
    }

    // Sort folders
    val sortedFolders = remember(audioFolders, folderSortField, folderSortDirection) {
        val sorted = when (folderSortField) {
            SortField.TITLE -> audioFolders.sortedBy { it.name.lowercase() }
            SortField.DATE -> audioFolders.sortedBy { it.dateModified }
            SortField.SIZE -> audioFolders.sortedBy { it.totalSizeBytes }
            SortField.COUNT -> audioFolders.sortedBy { it.trackCount }
            SortField.DURATION -> audioFolders.sortedBy { it.totalDurationMs }
        }
        if (folderSortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    // Sort tracks
    val activeTrackList = remember(searchedTracks, selectedFolder, contentSortField, contentSortDirection, viewMode, seenVersion) {
        val base = if (viewMode == ViewMode.FOLDER && selectedFolder != null) {
            searchedTracks.filter { it.path.startsWith(selectedFolder.path) }
        } else {
            searchedTracks
        }
        val raw = base.map { track ->
            val isNew = com.example.util.MediaSeenManager.isAudioTrackNew(context, track)
            if (track.isNew != isNew) track.copy(isNew = isNew) else track
        }

        val sorted = when (contentSortField) {
            SortField.TITLE -> raw.sortedBy { it.title.lowercase() }
            SortField.DURATION -> raw.sortedBy { it.durationMs }
            SortField.DATE -> raw.sortedBy { it.dateModified }
            SortField.SIZE -> raw.sortedBy { it.sizeBytes }
            SortField.COUNT -> raw
        }
        if (contentSortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
    }

    // Connect top bar selection events
    fun selectAll() {
        if (viewMode == ViewMode.FOLDER && selectedFolder == null) {
            selectedTrackPaths.clear()
            selectedFolderPaths.clear()
            selectedFolderPaths.addAll(sortedFolders.map { it.path })
        } else {
            selectedFolderPaths.clear()
            selectedTrackPaths.clear()
            selectedTrackPaths.addAll(activeTrackList.map { it.path })
        }
    }

    fun shareSelectedItems() {
        coroutineScope.launch(Dispatchers.IO) {
            val tracksToShare = if (isFolderSelection) {
                allTracks.filter { track -> selectedFolderPaths.any { fPath -> track.path.startsWith(fPath) } }
            } else {
                allTracks.filter { selectedTrackPaths.contains(it.path) }
            }
            withContext(Dispatchers.Main) {
                if (tracksToShare.isNotEmpty()) {
                    AudioFileManager.shareAudio(context, tracksToShare)
                } else {
                    Toast.makeText(context, "No audio tracks to share", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(isSelectionMode, totalSelectedCount, isFolderSelection, selectedFolderPaths.toList(), selectedTrackPaths.toList(), activeTrackList) {
        onSelectionModeChange(
            isSelectionMode,
            totalSelectedCount,
            isFolderSelection,
            { clearSelection() },
            { selectAll() },
            { shareSelectedItems() }
        )
        onInfoAction {
            val folder = if (isFolderSelection) {
                audioFolders.firstOrNull { selectedFolderPaths.contains(it.path) }
            } else null
            val track = if (isTrackSelection) {
                activeTrackList.firstOrNull { selectedTrackPaths.contains(it.path) }
            } else null
            onInfo(folder, track)
        }
    }

    // Actions
    fun handleFolderRename(folder: AudioFolderItem, newName: String) {
        renamingFolderPath = null
        if (newName.isBlank() || newName == folder.name) return
        coroutineScope.launch(Dispatchers.IO) {
            val success = AudioFileManager.renameFolder(context, folder, newName)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Folder renamed to $newName", Toast.LENGTH_SHORT).show()
                    scanAudioFiles(force = true)
                    clearSelection()
                } else {
                    Toast.makeText(context, "Failed to rename folder", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun handleTrackRename(track: AudioTrackItem, newTitle: String) {
        renamingTrackPath = null
        if (newTitle.isBlank()) return
        val srcFile = File(track.path)
        val ext = srcFile.extension
        val finalFileName = if (ext.isNotBlank()) "$newTitle.$ext" else newTitle
        if (finalFileName == srcFile.name) return

        coroutineScope.launch(Dispatchers.IO) {
            val success = AudioFileManager.renameAudio(context, track, finalFileName)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Audio renamed to $newTitle", Toast.LENGTH_SHORT).show()
                    scanAudioFiles(force = true)
                    clearSelection()
                } else {
                    Toast.makeText(context, "Failed to rename audio file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteSelectedItems() {
        showDeleteConfirmDialog = false
        val deletingFolderPaths = selectedFolderPaths.toSet()
        val deletingTrackPaths = selectedTrackPaths.toSet()
        val isFolderDel = isFolderSelection

        // Capture targets before changing the in-memory list. No storage mutation occurs
        // until the final Delete confirmation has been pressed.
        val foldersToDelete = audioFolders.filter { deletingFolderPaths.contains(it.path) }
        val tracksToDelete = searchedTracks.filter { deletingTrackPaths.contains(it.path) }

        isMoveCopyInProgress = true
        moveCopyIsDelete = true
        moveCopyProgressFraction = 0f
        moveCopyTotalCount = if (isFolderDel) foldersToDelete.size else tracksToDelete.size
        moveCopyCompletedCount = 0
        moveCopyItemName = ""

        coroutineScope.launch(Dispatchers.IO) {
            var deletedCount = 0
            if (isFolderDel) {
                for ((index, folder) in foldersToDelete.withIndex()) {
                    moveCopyItemName = folder.name
                    val c = AudioFileManager.deleteFolderAudios(context, folder) { fraction, completed, total, itemName ->
                        val overallFraction = (index + fraction) / foldersToDelete.size.coerceAtLeast(1)
                        moveCopyProgressFraction = overallFraction
                        moveCopyItemName = itemName
                    }
                    deletedCount += c
                    moveCopyCompletedCount = index + 1
                    moveCopyProgressFraction = (index + 1).toFloat() / foldersToDelete.size.coerceAtLeast(1)
                }
            } else {
                deletedCount = AudioFileManager.deleteAudioTracks(context, tracksToDelete) { fraction, completed, total, itemName ->
                    moveCopyProgressFraction = fraction
                    moveCopyCompletedCount = completed
                    moveCopyItemName = itemName
                }
            }
            withContext(Dispatchers.Main) {
                // Storage deletion has already finished. Remove the deleted paths from the
                // in-memory list first, then complete the progress UI. A silent MediaStore
                // rescan follows only as reconciliation, never as the visual completion step.
                if (isFolderDel) {
                    val folderSet = deletingFolderPaths.toSet()
                    allTracks.removeAll { track -> folderSet.any { folder -> track.path == folder || track.path.startsWith(folder.trimEnd('/') + "/") } }
                } else {
                    val deletedSet = deletingTrackPaths.toSet()
                    allTracks.removeAll { deletedSet.contains(it.path) }
                }
                moveCopyProgressFraction = 1f
                kotlinx.coroutines.delay(120L)
                isMoveCopyInProgress = false
                moveCopyIsDelete = false
                clearSelection()
                Toast.makeText(
                    context,
                    if (deletedCount > 0) "Deleted $deletedCount audio file(s)" else "Nothing was deleted",
                    Toast.LENGTH_SHORT
                ).show()
                scanAudioFiles(force = true, showIndicator = false)
            }
        }
    }

    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary

    Box(modifier = modifier.fillMaxSize()) {
        LiquidPullToRefresh(
            isRefreshing = isScanning,
            onRefresh = { scanAudioFiles(force = true, showIndicator = true) },
            isDark = isDark,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (viewMode == ViewMode.FOLDER && selectedFolder == null) {
                    // ================= FOLDER VIEW (ROOT) ================= //
                    if (folderLayoutMode == LayoutMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }, contentType = "online_folder") {
                                OnlineFolderCard(
                                    isDark = isDark,
                                    onClick = { onSelectFolder(ONLINE_FOLDER_ITEM) }
                                )
                            }
                            if (sortedFolders.isEmpty() && !isScanning) {
                                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty_folders") {
                                    EmptyAudioFoldersInlineView(
                                        isDark = isDark,
                                        message = if (searchQuery.isNotBlank()) "No audio folders matching '$searchQuery'" else "No audio folders found"
                                    )
                                }
                            } else {
                                items(sortedFolders, key = { it.path }) { folder ->
                                    val isSelected = selectedFolderPaths.contains(folder.path)
                                    val isRenaming = renamingFolderPath == folder.path
                                    AudioFolderGridCard(
                                        folder = folder,
                                        isDark = isDark,
                                        visibleFields = folderVisibleFields,
                                        isSelected = isSelected,
                                        isRenaming = isRenaming,
                                        onConfirmRename = { newName -> handleFolderRename(folder, newName) },
                                        onCancelRename = { renamingFolderPath = null },
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) selectedFolderPaths.remove(folder.path) else selectedFolderPaths.add(folder.path)
                                            } else {
                                                onSelectFolder(folder)
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelected) selectedFolderPaths.remove(folder.path) else selectedFolderPaths.add(folder.path)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(contentType = "online_folder") {
                                OnlineFolderCard(
                                    isDark = isDark,
                                    onClick = { onSelectFolder(ONLINE_FOLDER_ITEM) }
                                )
                            }
                            if (sortedFolders.isEmpty() && !isScanning) {
                                item(contentType = "empty_folders") {
                                    EmptyAudioFoldersInlineView(
                                        isDark = isDark,
                                        message = if (searchQuery.isNotBlank()) "No audio folders matching '$searchQuery'" else "No audio folders found"
                                    )
                                }
                            } else {
                                items(sortedFolders, key = { it.path }) { folder ->
                                    val isSelected = selectedFolderPaths.contains(folder.path)
                                    val isRenaming = renamingFolderPath == folder.path
                                    AudioFolderListCard(
                                        folder = folder,
                                        isDark = isDark,
                                        visibleFields = folderVisibleFields,
                                        isSelected = isSelected,
                                        isRenaming = isRenaming,
                                        onConfirmRename = { newName -> handleFolderRename(folder, newName) },
                                        onCancelRename = { renamingFolderPath = null },
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) selectedFolderPaths.remove(folder.path) else selectedFolderPaths.add(folder.path)
                                            } else {
                                                onSelectFolder(folder)
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelected) selectedFolderPaths.remove(folder.path) else selectedFolderPaths.add(folder.path)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedFolder?.path == ONLINE_FOLDER_SENTINEL_PATH) {
                    // ================= ONLINE MUSIC VIEW ================= //
                    OnlineMusicPanel(
                        isDark = isDark,
                        searchQuery = searchQuery,
                        layoutMode = contentLayoutMode,
                        visibleFields = contentVisibleFields,
                        sortField = contentSortField,
                        sortDirection = contentSortDirection,
                        onOpenChannel = onOpenChannel,
                        onOpenSharedPlaylist = onOpenSharedPlaylist,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // ================= INSIDE FOLDER OR LIBRARY VIEW ================= //
                    if (activeTrackList.isEmpty() && !isScanning) {
                        EmptyAudioView(
                            isDark = isDark,
                            message = if (searchQuery.isNotBlank()) "No audio tracks matching '$searchQuery'" else "No audio tracks found"
                        )
                    } else {
                        if (contentLayoutMode == LayoutMode.GRID) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (activeTrackList.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "section_header") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                        ) {
                                            StyledIcon(
                                                imageVector = Icons.Outlined.Audiotrack,
                                                contentDescription = null,
                                                tint = AccentSkyBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "AUDIOS (${activeTrackList.size})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                }
                                items(activeTrackList, key = { it.id }) { track ->
                                    val isCurrent = currentPlayingTrack?.uri?.toString() == track.uri.toString()
                                    val isSelected = selectedTrackPaths.contains(track.path)
                                    val isRenaming = renamingTrackPath == track.path
                                    AudioTrackGridCard(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isPlaying = isCurrent && isPlaying,
                                        isDark = isDark,
                                        visibleFields = contentVisibleFields,
                                        isSelected = isSelected,
                                        isRenaming = isRenaming,
                                        onConfirmRename = { newTitle -> handleTrackRename(track, newTitle) },
                                        onCancelRename = { renamingTrackPath = null },
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) selectedTrackPaths.remove(track.path) else selectedTrackPaths.add(track.path)
                                            } else {
                                                com.example.util.MediaSeenManager.markSeen(context, track.path, track.id)
                                                AudioPlaybackManager.playTrack(
                                                    context = context,
                                                    track = track,
                                                    newQueue = activeTrackList
                                                )
                                                AudioPlaybackManager.openFullScreen()
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelected) selectedTrackPaths.remove(track.path) else selectedTrackPaths.add(track.path)
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (activeTrackList.isNotEmpty()) {
                                    item(contentType = "section_header") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                                        ) {
                                            StyledIcon(
                                                imageVector = Icons.Outlined.Audiotrack,
                                                contentDescription = null,
                                                tint = AccentSkyBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "AUDIOS (${activeTrackList.size})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                letterSpacing = 0.8.sp
                                            )
                                        }
                                    }
                                }
                                items(activeTrackList, key = { it.id }) { track ->
                                    val isCurrent = currentPlayingTrack?.uri?.toString() == track.uri.toString()
                                    val isSelected = selectedTrackPaths.contains(track.path)
                                    val isRenaming = renamingTrackPath == track.path
                                    AudioTrackListCard(
                                        track = track,
                                        isCurrent = isCurrent,
                                        isPlaying = isCurrent && isPlaying,
                                        isDark = isDark,
                                        visibleFields = contentVisibleFields,
                                        isSelected = isSelected,
                                        isRenaming = isRenaming,
                                        onConfirmRename = { newTitle -> handleTrackRename(track, newTitle) },
                                        onCancelRename = { renamingTrackPath = null },
                                        onClick = {
                                            if (isSelectionMode) {
                                                if (isSelected) selectedTrackPaths.remove(track.path) else selectedTrackPaths.add(track.path)
                                            } else {
                                                com.example.util.MediaSeenManager.markSeen(context, track.path, track.id)
                                                AudioPlaybackManager.playTrack(
                                                    context = context,
                                                    track = track,
                                                    newQueue = activeTrackList
                                                )
                                                AudioPlaybackManager.openFullScreen()
                                            }
                                        },
                                        onLongClick = {
                                            if (isSelected) selectedTrackPaths.remove(track.path) else selectedTrackPaths.add(track.path)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Bottom Selection Bar or Delete Confirmation Bar
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            val canRename = (selectedFolderPaths.size == 1 && selectedTrackPaths.isEmpty()) ||
                    (selectedTrackPaths.size == 1 && selectedFolderPaths.isEmpty())

            if (isMoveCopyInProgress) {
                BottomMoveCopyProgressBar(
                    isDark = isDark,
                    alphaRatio = 0.95f,
                    isMove = moveCopyIsMove,
                    isDelete = moveCopyIsDelete,
                    progressFraction = moveCopyProgressFraction,
                    currentItemName = moveCopyItemName,
                    completedCount = moveCopyCompletedCount,
                    totalCount = moveCopyTotalCount
                )
            } else if (showDeleteConfirmDialog) {
                val itemTypeStr = if (isFolderSelection) {
                    if (totalSelectedCount > 1) "$totalSelectedCount folders" else "folder"
                } else {
                    if (totalSelectedCount > 1) "$totalSelectedCount audio tracks" else "audio track"
                }
                BottomDeleteConfirmationBar(
                    title = "Delete $itemTypeStr?",
                    message = "Permanently delete? Action cannot be undone.",
                    count = totalSelectedCount,
                    isDark = isDark,
                    alphaRatio = 0.95f,
                    itemNames = (if (isFolderSelection) selectedFolderPaths else selectedTrackPaths)
                        .map { it.trimEnd('/').substringAfterLast('/') },
                    onCancel = { showDeleteConfirmDialog = false },
                    onConfirm = { deleteSelectedItems() }
                )
            } else {
                BottomSelectionBar(
                    isDark = isDark,
                    alphaRatio = 0.95f,
                    canRename = canRename,
                    canDelete = totalSelectedCount > 0,
                    canMove = totalSelectedCount > 0,
                    canCopy = totalSelectedCount > 0,
                    canPlaylist = totalSelectedCount > 0,
                    playlistIconRes = R.drawable.viaura_playlist_audio,
                    onRename = {
                        if (selectedFolderPaths.size == 1 && selectedTrackPaths.isEmpty()) {
                            renamingFolderPath = selectedFolderPaths.first()
                            renamingTrackPath = null
                        } else if (selectedTrackPaths.size == 1 && selectedFolderPaths.isEmpty()) {
                            renamingTrackPath = selectedTrackPaths.first()
                            renamingFolderPath = null
                        }
                    },
                    onDelete = {
                        if (totalSelectedCount > 0) {
                            showDeleteConfirmDialog = true
                        }
                    },
                    onMove = {
                        directoryPickerAction = DirectoryPickerAction.MOVE
                        showDirectoryPickerSheet = true
                    },
                    onCopy = {
                        directoryPickerAction = DirectoryPickerAction.COPY
                        showDirectoryPickerSheet = true
                    },
                    onPlaylist = { showAddToPlaylistDialog = true }
                )
            }
        }

        if (showDirectoryPickerSheet) {
            FloatingDirectoryPickerSheet(
                action = directoryPickerAction,
                itemCount = totalSelectedCount,
                itemDescription = if (isFolderSelection) "audio folders" else "audio tracks",
                isDark = isDark,
                onConfirmTargetDir = { targetDir ->
                    showDirectoryPickerSheet = false
                    val folderPaths = selectedFolderPaths.toList()
                    val trackPaths = selectedTrackPaths.toList()
                    val isMove = directoryPickerAction == DirectoryPickerAction.MOVE

                    isMoveCopyInProgress = true
                    moveCopyProgressFraction = 0f
                    moveCopyIsMove = isMove
                    moveCopyTotalCount = totalSelectedCount
                    moveCopyCompletedCount = 0
                    moveCopyItemName = ""

                    coroutineScope.launch(Dispatchers.IO) {
                        var count = 0
                        if (folderPaths.isNotEmpty()) {
                            val folders = audioFolders.filter { folderPaths.contains(it.path) }
                            for (folder in folders) {
                                moveCopyItemName = folder.name
                                val ok = if (isMove) {
                                    AudioFileManager.moveAudioFolder(context, folder, targetDir) { fraction, _, _, itemName ->
                                        moveCopyProgressFraction = fraction
                                        moveCopyItemName = itemName
                                    }
                                } else {
                                    AudioFileManager.copyAudioFolder(context, folder, targetDir) { fraction, _, _, itemName ->
                                        moveCopyProgressFraction = fraction
                                        moveCopyItemName = itemName
                                    }
                                }
                                if (ok) {
                                    count++
                                    moveCopyCompletedCount = count
                                }
                            }
                        } else {
                            val tracks = allTracks.filter { trackPaths.contains(it.path) }
                            count = if (isMove) {
                                AudioFileManager.moveAudioTracks(context, tracks, targetDir) { fraction, _, _, itemName ->
                                    moveCopyProgressFraction = fraction
                                    moveCopyItemName = itemName
                                }
                            } else {
                                AudioFileManager.copyAudioTracks(context, tracks, targetDir) { fraction, _, _, itemName ->
                                    moveCopyProgressFraction = fraction
                                    moveCopyItemName = itemName
                                }
                            }
                            moveCopyCompletedCount = count
                        }
                        withContext(Dispatchers.Main) {
                            moveCopyProgressFraction = 1f
                            kotlinx.coroutines.delay(200L)
                            isMoveCopyInProgress = false
                            clearSelection()
                            scanAudioFiles(force = true, showIndicator = false)
                            val actionText = if (isMove) "Moved" else "Copied"
                            Toast.makeText(context, "$actionText $count item(s)", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { showDirectoryPickerSheet = false }
            )
        }

        if (showAddToPlaylistDialog) {
            val selectedTracks = if (isFolderSelection) {
                allTracks.filter { track -> selectedFolderPaths.any { it == track.path.substringBeforeLast('/') } }
            } else {
                allTracks.filter { selectedTrackPaths.contains(it.path) }
            }
            AddToPlaylistDialog(
                selectedAudioTracks = selectedTracks,
                isDark = isDark,
                onDismiss = { showAddToPlaylistDialog = false },
                onPlaylistUpdated = {
                    showAddToPlaylistDialog = false
                    clearSelection()
                }
            )
        }
    }
}
