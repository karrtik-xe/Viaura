package com.example.ui.screens

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.util.NaturalOrderComparator
import com.example.util.MediaMetadataExtractor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.components.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.R
import com.example.ui.components.AddArchivePlaylistSheet
import com.example.ui.components.AddPlaylistFromUrlSheet
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.liquidGlassToolbarOr
import com.example.ui.components.toOnlineVideoItem
import com.example.ui.components.BottomMoveCopyProgressBar
import com.example.ui.components.CreateEmptyPlaylistSheet
import com.example.ui.components.PlaylistOptionsSheet
import com.example.ui.components.FolderGlyphType
import com.example.ui.components.InlineRenameField
import com.example.ui.components.LayoutMode
import com.example.ui.components.ModernFolderIcon
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.PlaylistFilterTabs
import com.example.ui.components.glideToItem
import com.example.ui.components.PlaylistRevealSwipe
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.StyledIcon
import com.example.ui.components.VideoItemCard
import com.example.ui.components.ViewMode
import com.example.ui.components.VisibleFields
import com.example.ui.state.UiState
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
import com.example.util.CustomPlaylist
import com.example.util.PlaylistImportResult
import com.example.util.PlaylistImporter
import com.example.util.PlaylistManager
import com.example.util.VideoFileManager
import com.example.player.AudioPlaybackManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
/** An online item of an online folder that the swipe toolbar acts on (audio track or video). */
internal data class PlaylistQuickTarget(
    val path: String,
    val isAudio: Boolean,
    val result: com.example.util.OnlineTrackResult,
    val audio: AudioTrackItem? = null,
    val video: VideoItem? = null
)

internal fun quickTargetForAudio(t: AudioTrackItem) = PlaylistQuickTarget(
    path = t.path,
    isAudio = true,
    result = com.example.util.OnlineTrackResult(
        videoUrl = t.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX),
        title = t.title,
        artist = t.artist,
        durationSec = t.durationMs / 1000L,
        thumbnailUrl = t.thumbnailUrl,
        uploadDateEpochMs = t.dateModified.takeIf { it > 0L }
    ),
    audio = t
)

internal fun quickTargetForVideo(v: VideoItem) = PlaylistQuickTarget(
    path = v.path,
    isAudio = false,
    result = com.example.util.OnlineTrackResult(
        videoUrl = v.path.removePrefix(com.example.util.OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX),
        title = v.displayName,
        artist = v.channelName ?: "",
        durationSec = v.durationMs / 1000L,
        thumbnailUrl = v.thumbnailUrl,
        uploadDateEpochMs = v.dateModified.takeIf { it > 0L }
    ),
    video = v
)
