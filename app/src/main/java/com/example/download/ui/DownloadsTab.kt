package com.example.ui.screens

import com.example.ui.components.cardSurface
import com.example.ui.theme.Neutral

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.R
import com.example.ui.components.DeleteConfirmSheet
import com.example.ui.components.DirectoryPickerAction
import com.example.ui.components.FloatingDirectoryPickerSheet
import com.example.ui.components.LayoutMode
import com.example.ui.components.LinkThumbnail
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.StyledIcon
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VideoItemGridCard
import com.example.player.AudioPlaybackManager
import com.example.ui.components.VisibleFields
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import com.example.ui.state.UiState
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadFormat
import com.example.util.DownloadItem
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.DownloadStatus
import com.example.util.DownloadStore
import com.example.util.MediaMetadataExtractor
import com.example.util.VideoFileManager
import com.example.player.AudioMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Downloads Screen: Completely redesigned to match folder UI 1:1.
 * - Supports List and Grid views identical to folder VideoItemCard / VideoItemGridCard.
 * - Multi-select mode uses the common Info action in the top bar and four bottom actions (Rename, Delete, Move, Copy).
 * - Full Search, Sort (Name, Date, Size, Duration), and View options.
 * - In-progress downloads render matching cards with real-time speed, progress, pause/resume/stop.
 * - MediaInformationScreen support for both video and audio items.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DownloadsTabContent(
    isDark: Boolean,
    searchQuery: String,
    sortField: SortField,
    sortDirection: SortDirection,
    layoutMode: LayoutMode,
    visibleFields: VisibleFields,
    thumbnailStrategy: ThumbnailStrategy,
    thumbnailQuality: ThumbnailQuality,
    thumbnailFallbackSecond: Int,
    showNetworkThumbnails: Boolean,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    uiState: UiState = UiState(),
    onSelectionModeChange: ((
        inSelection: Boolean,
        count: Int,
        onClear: () -> Unit,
        onSelectAll: () -> Unit,
        onShare: () -> Unit,
        onInfo: () -> Unit
    ) -> Unit)? = null,
    // When set, the full-page "Media Information & Extractor" screen is shown by the parent (HomeScreen), exactly like a
    // video opened from a folder (full screen, same header, same Settings button) instead of inside this tab's content area.
    onShowMediaInfo: ((VideoItem) -> Unit)? = null,
    // The opened series folder lives in the parent (HomeScreen) so its name + count show in the top bar.
    openFolder: String? = null,
    onOpenFolderChange: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        DownloadStore.init(context)
    }

    val allDownloads by DownloadStore.items.collectAsState()

    // Duration and resolution cache for completed download items to ensure 60fps scrolling
    val durationCache = remember { mutableStateMapOf<String, Long>() }
    val resolutionCache = remember { mutableStateMapOf<String, String>() }
    // Subtitle formats of downloaded videos / lyrics flag of downloaded songs (for the indicator chips)
    val subtitleCache = remember { mutableStateMapOf<String, List<String>>() }
    val lyricsCache = remember { mutableStateMapOf<String, Boolean>() }

    // Multi-selection state
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedIds.isNotEmpty()
    // opened series folder (null = the main list): state is owned by the parent, see openFolder / onOpenFolderChange
    LaunchedEffect(openFolder, allDownloads) {
        val open = openFolder
        if (open != null && allDownloads.none { it.group == open }) onOpenFolderChange(null)
    }

    // Dialog & overlay states
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<DownloadItem?>(null) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var directoryPickerAction by remember { mutableStateOf(DirectoryPickerAction.MOVE) }
    var selectedMediaForInfo by remember { mutableStateOf<VideoItem?>(null) }

    // Move/Copy progress state
    var isMoveCopyInProgress by remember { mutableStateOf(false) }
    var moveCopyProgressFraction by remember { mutableFloatStateOf(0f) }
    var moveCopyItemName by remember { mutableStateOf("") }
    var moveCopyCompletedCount by remember { mutableIntStateOf(0) }
    var moveCopyTotalCount by remember { mutableIntStateOf(0) }

    // Remove deleted items from selection
    LaunchedEffect(allDownloads) {
        val existingIds = allDownloads.map { it.id }.toSet()
        if (selectedIds.any { it !in existingIds }) {
            selectedIds = selectedIds.filter { it in existingIds }.toSet()
        }
    }

    // Probe subtitles (videos) and lyrics (songs) of completed downloads in the background
    LaunchedEffect(allDownloads) {
        withContext(Dispatchers.IO) {
            allDownloads.forEach { item ->
                if (item.status != DownloadStatus.COMPLETED) return@forEach
                val file = item.filePath?.let { File(it) }
                if (file == null || !file.exists()) return@forEach
                if (item.isAudio) {
                    if (!lyricsCache.containsKey(item.id)) lyricsCache[item.id] = downloadHasLyrics(file)
                } else if (!subtitleCache.containsKey(item.id)) {
                    subtitleCache[item.id] = try {
                        MediaMetadataExtractor.extractMetadata(
                            filePath = file.absolutePath,
                            context = context,
                            uri = item.fileUri?.let { Uri.parse(it) },
                            displayName = file.name,
                            sizeBytes = file.length(),
                            dateModified = file.lastModified() / 1000L
                        ).effectiveSubtitleFormats
                    } catch (_: Throwable) {
                        emptyList()
                    }
                }
            }
        }
    }

    // Probe duration and resolution asynchronously in background for completed files
    LaunchedEffect(allDownloads) {
        withContext(Dispatchers.IO) {
            allDownloads.forEach { item ->
                if (item.status == DownloadStatus.COMPLETED && !durationCache.containsKey(item.id)) {
                    val file = item.filePath?.let { File(it) }
                    if (file != null && file.exists()) {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val dur = durStr?.toLongOrNull() ?: 0L
                            durationCache[item.id] = dur

                            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                            val res = if (height >= 2160 || width >= 3840) "4K"
                            else if (height >= 1440 || width >= 2560) "2K"
                            else if (height >= 1080 || width >= 1920) "1080p"
                            else if (height >= 720 || width >= 1280) "720p"
                            else if (height >= 480 || width >= 640) "480p"
                            else if (height > 0) "${height}p"
                            else (Regex("""\b(\d{3,4}p|4K|8K)\b""", RegexOption.IGNORE_CASE).find(item.detail ?: "")?.value ?: "")
                            resolutionCache[item.id] = res

                            retriever.release()
                        } catch (_: Throwable) {
                            durationCache[item.id] = 0L
                        }
                    } else {
                        durationCache[item.id] = 0L
                    }
                }
            }
        }
    }

    // Helper: Map DownloadItem to VideoItem
    fun downloadToVideoItem(item: DownloadItem): VideoItem {
        val file = item.filePath?.let { File(it) }
        val uri = item.fileUri?.let { Uri.parse(it) }
            ?: (if (file != null && file.exists()) Uri.fromFile(file) else Uri.parse(item.source))
        val sizeBytes = if (file != null && file.exists()) file.length() else item.totalBytes
        val dateModified = (if (file != null && file.exists()) file.lastModified() else item.createdAt) / 1000L
        val name = file?.name ?: item.title
        val dur = durationCache[item.id] ?: 0L
        val res = resolutionCache[item.id] ?: (Regex("""\b(\d{3,4}p|4K|8K)\b""", RegexOption.IGNORE_CASE).find(item.detail ?: "")?.value ?: "")
        val indicatorFormats = if (item.isAudio) {
            if (lyricsCache[item.id] == true) listOf("Lyrics") else emptyList()
        } else {
            subtitleCache[item.id] ?: emptyList()
        }

        return VideoItem(
            id = -(kotlin.math.abs(("download:" + item.id).hashCode().toLong())) - 1L,
            uri = uri,
            displayName = name,
            path = item.filePath ?: uri.toString(),
            sizeBytes = sizeBytes,
            durationMs = dur,
            dateModified = dateModified,
            resolution = res,
            isOnline = false,
            thumbnailUrl = item.thumbnailUrl,
            subtitleFormats = indicatorFormats,
            hasSubtitles = indicatorFormats.isNotEmpty()
        )
    }

    // Filter by search query
    val query = searchQuery.trim()
    val filteredItems = remember(allDownloads, query, openFolder) {
        val inScope = when {
            openFolder != null -> allDownloads.filter { it.group == openFolder }
            // main list: a series' episodes live inside its folder (a search still finds them one by one)
            query.isEmpty() -> allDownloads.filter { it.group.isNullOrBlank() }
            else -> allDownloads
        }
        if (query.isEmpty()) {
            inScope
        } else {
            inScope.filter {
                it.group.orEmpty().contains(query, ignoreCase = true) ||
                        it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.orEmpty().contains(query, ignoreCase = true) ||
                        it.filePath.orEmpty().contains(query, ignoreCase = true) ||
                        it.detail.orEmpty().contains(query, ignoreCase = true)
            }
        }
    }

    // Sort by sortField and sortDirection
    val sortedItems = remember(filteredItems, sortField, sortDirection, durationCache.toMap()) {
        val comparator = when (sortField) {
            SortField.TITLE -> compareBy<DownloadItem> { it.title.lowercase(Locale.ROOT) }
            SortField.DATE -> compareBy { it.createdAt }
            SortField.SIZE -> compareBy { it.totalBytes }
            SortField.DURATION -> compareBy { durationCache[it.id] ?: 0L }
            else -> compareBy { it.createdAt }
        }
        if (sortDirection == SortDirection.DESCENDING) {
            filteredItems.sortedWith(comparator.reversed())
        } else {
            filteredItems.sortedWith(comparator)
        }
    }

    // Keep audio and video queues separate so a downloaded song can never be sent to the video player.
    val completedVideoItems = remember(sortedItems, durationCache.toMap(), resolutionCache.toMap(), subtitleCache.toMap(), lyricsCache.toMap()) {
        sortedItems.filter { it.status == DownloadStatus.COMPLETED && !it.isAudio }.map { downloadToVideoItem(it) }
    }
    val completedAudioItems = remember(sortedItems) {
        sortedItems.filter { it.status == DownloadStatus.COMPLETED && it.isAudio }
    }
    val completedAudioTracks = remember(completedAudioItems, durationCache.toMap()) {
        completedAudioItems.map { item -> downloadToAudioTrack(item, durationCache[item.id] ?: 0L) }
    }

    fun playDownloadedItem(item: DownloadItem) {
        if (item.isAudio) {
            val track = completedAudioTracks.firstOrNull { it.id == downloadAudioId(item.id) }
                ?: downloadToAudioTrack(item, durationCache[item.id] ?: 0L)
            val queue = completedAudioTracks.ifEmpty { listOf(track) }
            AudioPlaybackManager.openFullScreen()
            AudioPlaybackManager.playTrack(context, track, queue)
        } else {
            val videoItem = downloadToVideoItem(item)
            onPlayVideo(videoItem, completedVideoItems.ifEmpty { listOf(videoItem) })
        }
    }

    val processingItems = sortedItems.filter { it.isActive }
    val otherDownloadItems = sortedItems.filter { it.status != DownloadStatus.COMPLETED && !it.isActive }
    val downloadedItems = sortedItems.filter { it.status == DownloadStatus.COMPLETED }
    val folderEntries: List<DownloadsContentEntry.Folder> = remember(allDownloads, openFolder, query) {
        if (openFolder != null || query.isNotEmpty()) emptyList()
        else allDownloads.filter { !it.group.isNullOrBlank() }.groupBy { it.group.orEmpty() }.map { (name, list) ->
            DownloadsContentEntry.Folder(
                name = name,
                total = list.size,
                active = list.count { it.isActive },
                thumbnailUrl = list.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
            ) to (list.maxOfOrNull { it.createdAt } ?: 0L)
        }.sortedByDescending { it.second }.map { it.first }
    }
    val sectionedItems: List<DownloadsContentEntry> = remember(processingItems, otherDownloadItems, downloadedItems, folderEntries, openFolder) {
        buildList<DownloadsContentEntry> {
            // (the folder name no longer gets its own row: it is shown in the top bar)
            if (folderEntries.isNotEmpty()) {
                add(DownloadsContentEntry.Section("Folders (${folderEntries.size})", "section_folders"))
                folderEntries.forEach { add(it) }
            }
            if (processingItems.isNotEmpty()) {
                add(DownloadsContentEntry.Section("Processing (${processingItems.size})", "section_processing"))
                processingItems.forEach { add(DownloadsContentEntry.Media(it)) }
            }
            if (otherDownloadItems.isNotEmpty()) {
                add(DownloadsContentEntry.Section("Other downloads (${otherDownloadItems.size})", "section_other"))
                otherDownloadItems.forEach { add(DownloadsContentEntry.Media(it)) }
            }
            if (downloadedItems.isNotEmpty()) {
                add(DownloadsContentEntry.Section("Downloaded (${downloadedItems.size})", "section_downloaded"))
                downloadedItems.forEach { add(DownloadsContentEntry.Media(it)) }
            }
        }
    }

    // Share action
    fun shareSelectedItems() {
        val completedSelected = sortedItems
            .filter { it.id in selectedIds && it.status == DownloadStatus.COMPLETED && !it.isAudio }
            .map { downloadToVideoItem(it) }
        if (completedSelected.isNotEmpty()) {
            VideoFileManager.shareVideos(context, completedSelected)
        } else {
            Toast.makeText(context, "No completed videos to share", Toast.LENGTH_SHORT).show()
        }
    }

    // Toggle single item selection
    fun toggleSelection(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    // Select all / clear
    fun toggleSelectAll() {
        selectedIds = if (selectedIds.size == sortedItems.size) {
            emptySet()
        } else {
            sortedItems.map { it.id }.toSet()
        }
    }

    // Info action for selected item
    fun openInfoForSelected() {
        if (selectedIds.size == 1) {
            val item = sortedItems.firstOrNull { it.id in selectedIds }
            if (item != null) {
                val infoItem = downloadToVideoItem(item)
                if (onShowMediaInfo != null) onShowMediaInfo(infoItem) else selectedMediaForInfo = infoItem
                // Match folder behavior: the full-page information screen replaces the selection toolbar.
                selectedIds = emptySet()
            }
        }
    }

    // Notify parent HomeScreen of selection mode changes (so top bar switches to SelectionTopBar)
    LaunchedEffect(isSelectionMode, selectedIds.size, sortedItems.size) {
        onSelectionModeChange?.invoke(
            isSelectionMode,
            selectedIds.size,
            { selectedIds = emptySet() },
            { toggleSelectAll() },
            { shareSelectedItems() },
            { openInfoForSelected() }
        )
    }

    // Handle back button when in selection mode or when info screen is open
    BackHandler(enabled = isSelectionMode && selectedMediaForInfo == null) {
        selectedIds = emptySet()
    }

    BackHandler(enabled = openFolder != null && !isSelectionMode && selectedMediaForInfo == null) {
        onOpenFolderChange(null)
    }

    val primaryColor = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryColor = if (isDark) DarkTextSecondary else LightTextSecondary
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    Box(modifier = modifier.fillMaxSize()) {
        if (sortedItems.isEmpty() && folderEntries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp, start = 32.dp, end = 32.dp)
                ) {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_download_minimalistic,
                        contentDescription = null,
                        tint = secondaryColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (query.isNotEmpty()) "No matching downloads" else "No downloads yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (query.isNotEmpty()) "Try a different search term" else "Downloaded videos and audio will appear here.",
                        fontSize = 13.sp,
                        color = secondaryColor
                    )
                }
            }
        } else {
            // Match the spacing used by regular folders, with processing items above downloaded media.
            val contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)

            if (layoutMode == LayoutMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = sectionedItems,
                        key = { it.stableKey },
                        span = { entry -> if (entry !is DownloadsContentEntry.Media) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
                    ) { entry ->
                        when (entry) {
                            is DownloadsContentEntry.Section -> DownloadsSectionHeader(entry.title, isDark)
                            is DownloadsContentEntry.Folder -> DownloadsFolderCard(entry, isDark) { onOpenFolderChange(entry.name) }
                            is DownloadsContentEntry.FolderHeader -> DownloadsFolderHeaderRow(entry.name, isDark) { onOpenFolderChange(null) }
                            is DownloadsContentEntry.Media -> {
                                val item = entry.item
                                val isSelected = item.id in selectedIds
                                if (item.status == DownloadStatus.COMPLETED) {
                                    val videoItem = downloadToVideoItem(item)
                                    VideoItemGridCard(
                                        video = videoItem,
                                        isDark = isDark,
                                        visibleFields = if (item.isAudio) visibleFields.copy(showSubtitleIndicator = visibleFields.showLyrics) else visibleFields,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelectionMode) toggleSelection(item.id) else playDownloadedItem(item)
                                        },
                                        onLongClick = { toggleSelection(item.id) },
                                        thumbnailStrategy = thumbnailStrategy,
                                        thumbnailQuality = thumbnailQuality,
                                        thumbnailFallbackSecond = thumbnailFallbackSecond,
                                        showNetworkThumbnails = showNetworkThumbnails,
                                        onThumbnailClick = if (uiState.tapThumbnailToSelect) ({ toggleSelection(item.id) }) else null
                                    )
                                } else {
                                    DownloadingGridCard(
                                        item = item,
                                        isDark = isDark,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onToggleSelect = { toggleSelection(item.id) },
                                        onLongClick = { toggleSelection(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = sectionedItems,
                        key = { it.stableKey }
                    ) { entry ->
                        when (entry) {
                            is DownloadsContentEntry.Section -> DownloadsSectionHeader(entry.title, isDark)
                            is DownloadsContentEntry.Folder -> DownloadsFolderCard(entry, isDark) { onOpenFolderChange(entry.name) }
                            is DownloadsContentEntry.FolderHeader -> DownloadsFolderHeaderRow(entry.name, isDark) { onOpenFolderChange(null) }
                            is DownloadsContentEntry.Media -> {
                                val item = entry.item
                                val isSelected = item.id in selectedIds
                                if (item.status == DownloadStatus.COMPLETED) {
                                    val videoItem = downloadToVideoItem(item)
                                    VideoItemCard(
                                        video = videoItem,
                                        isDark = isDark,
                                        visibleFields = if (item.isAudio) visibleFields.copy(showSubtitleIndicator = visibleFields.showLyrics) else visibleFields,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelectionMode) toggleSelection(item.id) else playDownloadedItem(item)
                                        },
                                        onLongClick = { toggleSelection(item.id) },
                                        thumbnailStrategy = thumbnailStrategy,
                                        thumbnailQuality = thumbnailQuality,
                                        thumbnailFallbackSecond = thumbnailFallbackSecond,
                                        showNetworkThumbnails = showNetworkThumbnails,
                                        onThumbnailClick = if (uiState.tapThumbnailToSelect) ({ toggleSelection(item.id) }) else null
                                    )
                                } else {
                                    DownloadingListCard(
                                        item = item,
                                        isDark = isDark,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onToggleSelect = { toggleSelection(item.id) },
                                        onLongClick = { toggleSelection(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Bottom Action Bar in Selection Mode
        AnimatedVisibility(
            visible = isSelectionMode && selectedMediaForInfo == null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            val canRename = selectedIds.size == 1
            val canDelete = selectedIds.isNotEmpty()
            val hasCompletedSelected = selectedIds.any { id ->
                sortedItems.any { it.id == id && it.status == DownloadStatus.COMPLETED }
            }

            BottomSelectionBar(
                isDark = isDark,
                alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
                canRename = canRename,
                canDelete = canDelete,
                canMove = hasCompletedSelected,
                canCopy = hasCompletedSelected,
                canPlaylist = selectedIds.size == 1,
                playlistIcon = Icons.Outlined.Info,
                onRename = {
                    if (selectedIds.size == 1) {
                        itemToRename = sortedItems.firstOrNull { it.id in selectedIds }
                        showRenameDialog = true
                    }
                },
                onDelete = {
                    if (selectedIds.isNotEmpty()) {
                        showDeleteConfirm = true
                    }
                },
                onMove = {
                    directoryPickerAction = DirectoryPickerAction.MOVE
                    showDirectoryPicker = true
                },
                onCopy = {
                    directoryPickerAction = DirectoryPickerAction.COPY
                    showDirectoryPicker = true
                },
                onPlaylist = {
                    openInfoForSelected()
                },
                showPlaylist = false
            )
        }
    }

    // Rename Dialog
    if (showRenameDialog && itemToRename != null) {
        val currentItem = itemToRename!!
        RenameDialog(
            title = "Rename Download",
            initialText = currentItem.title,
            isDark = isDark,
            alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f),
            onDismiss = {
                showRenameDialog = false
                itemToRename = null
            },
            onConfirm = { newName ->
                showRenameDialog = false
                val trimmed = newName.trim()
                if (trimmed.isNotBlank() && trimmed != currentItem.title) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val file = currentItem.filePath?.let { File(it) }
                        var newPath = currentItem.filePath
                        var newUri = currentItem.fileUri

                        if (file != null && file.exists()) {
                            val ext = file.extension
                            val targetFileName = if (trimmed.endsWith(".$ext", ignoreCase = true)) trimmed else "$trimmed.$ext"
                            val dest = File(file.parentFile, targetFileName)
                            if (!dest.exists() || dest.absolutePath == file.absolutePath) {
                                val success = try {
                                    java.nio.file.Files.move(file.toPath(), dest.toPath())
                                    true
                                } catch (_: Exception) {
                                    file.renameTo(dest)
                                }
                                if (success) {
                                    newPath = dest.absolutePath
                                    newUri = Uri.fromFile(dest).toString()
                                }
                            }
                        }

                        DownloadStore.update(context, currentItem.id, persist = true) {
                            it.copy(
                                title = trimmed,
                                filePath = newPath,
                                fileUri = newUri
                            )
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Renamed to $trimmed", Toast.LENGTH_SHORT).show()
                            selectedIds = emptySet()
                            itemToRename = null
                        }
                    }
                }
            }
        )
    }

    // Delete Confirmation Sheet
    if (showDeleteConfirm) {
        val namesToDelete = sortedItems.filter { it.id in selectedIds }.map { it.title }
        DeleteConfirmSheet(
            title = "Delete ${selectedIds.size} ${if (selectedIds.size == 1) "download" else "downloads"}?",
            message = "This action cannot be undone. Finished files and any downloading pieces will be permanently deleted.",
            itemNames = namesToDelete,
            isDark = isDark,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                val idsToRemove = selectedIds
                selectedIds = emptySet()
                coroutineScope.launch(Dispatchers.IO) {
                    idsToRemove.forEach { id ->
                        DownloadManager.remove(context, id)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "${idsToRemove.size} downloads deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Directory Picker for Move & Copy
    if (showDirectoryPicker) {
        val completedSelected = sortedItems
            .filter { it.id in selectedIds && it.status == DownloadStatus.COMPLETED }
            .map { downloadToVideoItem(it) }

        FloatingDirectoryPickerSheet(
            action = directoryPickerAction,
            itemCount = completedSelected.size,
            itemDescription = "downloads",
            isDark = isDark,
            onConfirmTargetDir = { targetDir ->
                showDirectoryPicker = false
                val isMove = directoryPickerAction == DirectoryPickerAction.MOVE
                isMoveCopyInProgress = true

                coroutineScope.launch(Dispatchers.IO) {
                    if (isMove) {
                        VideoFileManager.moveVideos(
                            context = context,
                            videos = completedSelected,
                            destDir = targetDir
                        ) { frac, done, tot, currentName ->
                            moveCopyProgressFraction = frac
                            moveCopyCompletedCount = (done / (1024 * 1024)).toInt()
                            moveCopyTotalCount = (tot / (1024 * 1024)).toInt()
                            moveCopyItemName = currentName
                        }

                        // Update paths in DownloadStore
                        completedSelected.forEach { vid ->
                            val originalItem = sortedItems.firstOrNull { it.filePath == vid.path }
                            if (originalItem != null) {
                                val destFile = File(targetDir, File(vid.path).name)
                                DownloadStore.update(context, originalItem.id, persist = true) {
                                    it.copy(
                                        filePath = destFile.absolutePath,
                                        fileUri = Uri.fromFile(destFile).toString()
                                    )
                                }
                            }
                        }
                    } else {
                        VideoFileManager.copyVideos(
                            context = context,
                            videos = completedSelected,
                            destDir = targetDir
                        ) { frac, done, tot, currentName ->
                            moveCopyProgressFraction = frac
                            moveCopyCompletedCount = (done / (1024 * 1024)).toInt()
                            moveCopyTotalCount = (tot / (1024 * 1024)).toInt()
                            moveCopyItemName = currentName
                        }
                    }

                    withContext(Dispatchers.Main) {
                        isMoveCopyInProgress = false
                        selectedIds = emptySet()
                        val actStr = if (isMove) "Moved" else "Copied"
                        Toast.makeText(context, "$actStr ${completedSelected.size} files to ${targetDir.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showDirectoryPicker = false }
        )
    }

    // Media Details Full Screen Overlay (for both Video and Audio)
    if (selectedMediaForInfo != null) {
        MediaInformationScreen(
            video = selectedMediaForInfo!!,
            uiState = uiState,
            onBack = { selectedMediaForInfo = null }
        )
    }
}

/**
 * Downloading Card for List View:
 * Matches VideoItemCard layout 1:1 with 136x80 thumbnail box, rounded corners,
 * glass surface, progress indicator, speed, and pause/resume/stop controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadingListCard(
    item: DownloadItem,
    isDark: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val surfaceColor = if (isDark) {
        if (isSelected) Color(0xFF1E1B4B).copy(alpha = 0.90f) else DarkGlassSurface
    } else {
        if (isSelected) Color(0xFFEEF2FF).copy(alpha = 0.95f) else LightGlassSurface
    }
    val borderColor = if (isSelected) AccentSkyBlue else (if (isDark) DarkGlassBorder else LightGlassBorder)
    val primaryColor = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryColor = if (isDark) DarkTextSecondary else LightTextSecondary
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface(shape = RoundedCornerShape(14.dp)) {
                this
                    .clip(shape)
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.5.dp, borderColor, shape)
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isSelectionMode) onToggleSelect()
                    else {
                        if (item.status == DownloadStatus.DOWNLOADING) {
                            DownloadManager.pause(context, item.id)
                        } else if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.STOPPED || item.status == DownloadStatus.FAILED) {
                            DownloadManager.resume(context, item.id)
                        }
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Card (exactly matching VideoItemCard 136dp x 80dp)
        Box(
            modifier = Modifier
                .width(136.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
        ) {
            LinkThumbnail(
                thumbnail = item.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        StyledIcon(
                            drawableRes = if (item.isAudio) R.drawable.viaura_audio_track else R.drawable.viaura_download_minimalistic,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )

            // Percentage badge in bottom right
            val percentText = if (item.totalBytes > 0) "${(item.fraction * 100).toInt()}%" else "..."
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = percentText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Selection indicator badge in top left
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AccentSkyBlue else Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        StyledIcon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Info & Controls Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            // Subtitle & Status
            val statusLabel = when (item.status) {
                DownloadStatus.DOWNLOADING -> if (item.speedBps > 0) "Downloading · ${DownloadFormat.speed(item.speedBps)}" else "Starting…"
                DownloadStatus.QUEUED -> "Queued…"
                DownloadStatus.PROCESSING -> "Finishing up…"
                DownloadStatus.PAUSED -> "Paused"
                DownloadStatus.STOPPED -> "Stopped"
                DownloadStatus.FAILED -> "Failed · Tap to retry"
                else -> "Downloading"
            }
            Text(
                text = statusLabel,
                fontSize = 11.5.sp,
                color = if (item.status == DownloadStatus.FAILED) Color(0xFFEF4444) else AccentSkyBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Progress Indicator
            if (item.status == DownloadStatus.PROCESSING || item.totalBytes <= 0L) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentSkyBlue,
                    trackColor = secondaryColor.copy(alpha = 0.2f)
                )
            } else {
                LinearProgressIndicator(
                    progress = { item.fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentSkyBlue,
                    trackColor = secondaryColor.copy(alpha = 0.2f)
                )
            }

            Spacer(Modifier.height(5.dp))

            // Bottom details and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sizeText = if (item.totalBytes > 0) {
                    "${DownloadFormat.bytes(item.downloadedBytes)} / ${DownloadFormat.bytes(item.totalBytes)}"
                } else {
                    DownloadFormat.bytes(item.downloadedBytes)
                }

                Text(
                    text = sizeText,
                    fontSize = 11.sp,
                    color = secondaryColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                if (!isSelectionMode) {
                    when (item.status) {
                        DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(secondaryColor.copy(alpha = 0.12f))
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { DownloadManager.pause(context, item.id) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.Pause,
                                    contentDescription = "Pause",
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DownloadStatus.PAUSED, DownloadStatus.STOPPED, DownloadStatus.FAILED -> {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentSkyBlue.copy(alpha = 0.15f))
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { DownloadManager.resume(context, item.id) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = AccentSkyBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

/**
 * Downloading Card for Grid View:
 * Matches VideoItemGridCard layout 1:1 with 16:9 thumbnail, rounded corners,
 * glass surface, progress indicator, and controls.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadingGridCard(
    item: DownloadItem,
    isDark: Boolean,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val surfaceColor = if (isDark) {
        if (isSelected) Color(0xFF1E1B4B).copy(alpha = 0.90f) else DarkGlassSurface
    } else {
        if (isSelected) Color(0xFFEEF2FF).copy(alpha = 0.95f) else LightGlassSurface
    }
    val borderColor = if (isSelected) AccentSkyBlue else (if (isDark) DarkGlassBorder else LightGlassBorder)
    val primaryColor = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryColor = if (isDark) DarkTextSecondary else LightTextSecondary
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface(shape = RoundedCornerShape(12.dp)) {
                this
                    .clip(shape)
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.5.dp, borderColor, shape)
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isSelectionMode) onToggleSelect()
                    else {
                        if (item.status == DownloadStatus.DOWNLOADING) {
                            DownloadManager.pause(context, item.id)
                        } else if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.STOPPED || item.status == DownloadStatus.FAILED) {
                            DownloadManager.resume(context, item.id)
                        }
                    }
                },
                onLongClick = onLongClick
            )
            .padding(10.dp)
    ) {
        // Thumbnail matching VideoItemGridCard (16:9 aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
        ) {
            LinkThumbnail(
                thumbnail = item.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        StyledIcon(
                            drawableRes = if (item.isAudio) R.drawable.viaura_audio_track else R.drawable.viaura_download_minimalistic,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )

            // Progress bar overlaid at bottom of thumbnail
            LinearProgressIndicator(
                progress = { item.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = AccentSkyBlue,
                trackColor = Color.Black.copy(alpha = 0.35f)
            )

            // Percentage badge in bottom right
            val percentText = if (item.totalBytes > 0) "${(item.fraction * 100).toInt()}%" else "..."
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = percentText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Selection indicator badge
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AccentSkyBlue else Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        StyledIcon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = item.title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusLabel = when (item.status) {
                DownloadStatus.DOWNLOADING -> if (item.speedBps > 0) DownloadFormat.speed(item.speedBps) else "Starting…"
                DownloadStatus.QUEUED -> "Queued"
                DownloadStatus.PROCESSING -> "Finishing"
                DownloadStatus.PAUSED -> "Paused"
                DownloadStatus.STOPPED -> "Stopped"
                DownloadStatus.FAILED -> "Failed"
                else -> "Downloading"
            }
            Text(
                text = statusLabel,
                fontSize = 11.sp,
                color = if (item.status == DownloadStatus.FAILED) Color(0xFFEF4444) else AccentSkyBlue,
                maxLines = 1
            )

            if (!isSelectionMode) {
                when (item.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(secondaryColor.copy(alpha = 0.12f))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { DownloadManager.pause(context, item.id) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Pause,
                                contentDescription = "Pause",
                                tint = primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    DownloadStatus.PAUSED, DownloadStatus.STOPPED, DownloadStatus.FAILED -> {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(AccentSkyBlue.copy(alpha = 0.15f))
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { DownloadManager.resume(context, item.id) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "Resume",
                                tint = AccentSkyBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
