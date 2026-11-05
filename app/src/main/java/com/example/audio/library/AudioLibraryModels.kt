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

data class AudioTrackItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,
    val sizeBytes: Long,
    val format: String,
    val audioType: String = "Stereo",
    val hasLyrics: Boolean = false,
    val lyricsText: String = "",
    val dateModified: Long = 0L,
    val playbackProgress: Float = 0f,
    val isNew: Boolean = false,
    val thumbnailUrl: String? = null
) {
    val formattedDuration: String
        get() {
            if (durationMs <= 0L) return "00:00"
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format(Locale.US, "%02d:%02d", min, sec)
        }

    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return String.format(Locale.US, "%.1f MB", mb)
        }

    val formattedDate: String
        get() {
            if (dateModified <= 0L) return ""
            return try {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeMs = if (dateModified > 100_000_000_000L) dateModified else dateModified * 1000L
                sdf.format(Date(timeMs))
            } catch (_: Exception) {
                ""
            }
        }
}

data class AudioFolderItem(
    val name: String,
    val path: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val totalSizeBytes: Long,
    val dateModified: Long,
    val isNew: Boolean = false,
    val unplayedCount: Int = 0
) {
    val formattedDuration: String
        get() {
            if (totalDurationMs <= 0L) return "00:00"
            val totalSec = totalDurationMs / 1000
            val hours = totalSec / 3600
            val min = (totalSec % 3600) / 60
            val sec = totalSec % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, min, sec)
            } else {
                String.format(Locale.US, "%02d:%02d", min, sec)
            }
        }

    val formattedSize: String
        get() {
            val mb = totalSizeBytes.toDouble() / (1024 * 1024)
            val gb = mb / 1024.0
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                String.format(Locale.US, "%.1f MB", mb)
            }
        }

    val formattedDate: String
        get() {
            if (dateModified <= 0L) return ""
            return try {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val timeMs = if (dateModified > 100_000_000_000L) dateModified else dateModified * 1000L
                sdf.format(Date(timeMs))
            } catch (_: Exception) {
                ""
            }
        }
}

/**
 * Sentinel path identifying the virtual "Online" folder (search/stream any song online).
 * Not a real filesystem path — never matches a real AudioTrackItem.path (which always starts
 * with an absolute filesystem path), so existing folder-filtering logic (`path.startsWith(...)`)
 * is unaffected. Kept alongside AudioFolderItem since it's the data shape being reused.
 */
const val ONLINE_FOLDER_SENTINEL_PATH = "\u0000ONLINE_MUSIC\u0000"

val ONLINE_FOLDER_ITEM = AudioFolderItem(
    name = "Online",
    path = ONLINE_FOLDER_SENTINEL_PATH,
    trackCount = 0,
    totalDurationMs = 0L,
    totalSizeBytes = 0L,
    dateModified = 0L
)
