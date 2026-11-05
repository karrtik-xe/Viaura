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
 * Animated 4-bar equalizer for active playing track
 */
@Composable
fun AnimatedAudioEqualizer(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )
    val bar4 by transition.animateFloat(
        initialValue = 0.70f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b4"
    )

    Row(
        modifier = modifier
            .height(22.dp)
            .width(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar1)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentSkyBlue)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar2)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentPink)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar3)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentSkyBlue)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar4)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentPink)
        )
    }
}
