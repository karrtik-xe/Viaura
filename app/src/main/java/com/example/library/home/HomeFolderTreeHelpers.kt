package com.example.ui.screens

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlueStrong
import androidx.compose.foundation.Image
import com.example.ui.components.liquidGlassOr
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

/**
 * Recursively removes just-deleted videos/folders from an already-scanned [VideoFolder] tree,
 * without waiting for a fresh MediaStore/filesystem rescan.
 *
 * Deletion UI-glitch fix: the app previously relied entirely on a background rescan
 * (`reloadFoldersAndWaitAfterMutation`) to make a deleted item disappear from the grid/list.
 * That rescan re-queries MediaStore and/or walks the filesystem, and on some devices the
 * MediaStore index (or, rarely, the filesystem's own directory listing) hasn't fully settled
 * by the time the rescan runs immediately after `File.delete()`/`ContentResolver.delete()`
 * return — so the stale item would still be reported as present for that one rescan pass,
 * leaving it visibly stuck in the UI until the next unrelated refresh. Pruning the known-deleted
 * paths/ids out of the in-memory tree immediately (before the rescan even starts) removes the
 * dependency on that race entirely: the UI updates the instant the delete call returns, and the
 * subsequent rescan just reconciles everything else (thumbnails, counts, sizes) as before.
 *
 * Returns null if this folder itself was deleted (its id is in [deletedFolderIds]) or if it
 * becomes completely empty (no videos and no remaining sub-folders) as a result of the prune,
 * mirroring the "an emptied folder disappears" behavior already used when deleting on disk
 * (see VideoFileManager.deleteFolderVideos).
 */
internal fun pruneDeletedFromFolder(
    folder: VideoFolder,
    deletedVideoPaths: Set<String>,
    deletedFolderIds: Set<String>
): VideoFolder? {
    if (deletedFolderIds.contains(folder.id)) return null

    val prunedVideos = if (deletedVideoPaths.isEmpty()) {
        folder.videos
    } else {
        folder.videos.filter { it.path !in deletedVideoPaths }
    }
    val prunedSubFolders = folder.subFolders.mapNotNull {
        pruneDeletedFromFolder(it, deletedVideoPaths, deletedFolderIds)
    }

    if (prunedVideos.isEmpty() && prunedSubFolders.isEmpty()) return null
    if (prunedVideos === folder.videos && prunedSubFolders == folder.subFolders) return folder

    val prunedSize = prunedVideos.sumOf { it.sizeBytes } + prunedSubFolders.sumOf { it.totalSizeBytes }
    return folder.copy(
        videoCount = prunedVideos.size + prunedSubFolders.sumOf { it.videoCount },
        totalSizeBytes = prunedSize,
        videos = prunedVideos,
        subFolders = prunedSubFolders
    )
}

/**
 * Finds a folder anywhere in a scanned folder tree (root folders and every level of nested
 * `subFolders`), matched by id first and then by path.
 *
 * `videoFolders` only holds root-level folders; each root's descendants live in its
 * `subFolders` tree. Re-resolving `folderStack` (the user's current navigation path, which can
 * point several levels deep) by searching only the flat root list — as opposed to the full
 * tree — fails to find anything below root depth and was causing the app to silently reset
 * navigation back to Home on every background rescan while browsing inside a sub-folder,
 * including right after a delete (part of the same UI-refresh glitch).
 */
internal fun findFolderInTree(folders: List<VideoFolder>, id: String, path: String): VideoFolder? {
    for (folder in folders) {
        if (folder.id == id || folder.path == path) return folder
        val nested = findFolderInTree(folder.subFolders, id, path)
        if (nested != null) return nested
    }
    return null
}
