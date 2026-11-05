package com.example.ui.screens

import com.example.ui.components.cardSurface
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
 * Audio Folder Card - List Layout
 * Matches VideoFolderCard 1:1 in size, padding, spacing, and typography.
 */
@OptIn(ExperimentalFoundationApi::class)
/** Virtual folder card for Online Music — sits above real device folders, never selectable/renameable. */
@Composable
fun OnlineFolderCard(
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Online",
    subtitle: String = "Search & stream any song online"
) {
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardSurface {
                this
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            }
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp * com.example.ui.components.cardlessIconScale())
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.example.ui.theme.AccentGradient),
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = "Online",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp * com.example.ui.components.cardlessIconScale())
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioFolderListCard(
    folder: AudioFolderItem,
    isDark: Boolean,
    visibleFields: VisibleFields,
    isSelected: Boolean = false,
    isRenaming: Boolean = false,
    onConfirmRename: ((String) -> Unit)? = null,
    onCancelRename: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxWidth()
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
            // Folder Icon using custom ModernFolderIcon with dedicated Audio glyph
            ModernFolderIcon(
                isCustom = false,
                newCount = folder.unplayedCount,
                showNewBadge = visibleFields.showNewBadge && folder.unplayedCount > 0,
                isDark = isDark,
                isSelected = isSelected,
                size = com.example.ui.components.folderRowIconSize(),
                glyphType = FolderGlyphType.AUDIO
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Folder details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Folder Name (Bold or Inline Rename Field)
                if (isRenaming) {
                    InlineRenameField(
                        initialText = folder.name,
                        isDark = isDark,
                        onConfirm = { newName -> onConfirmRename?.invoke(newName) },
                        onCancel = { onCancelRename?.invoke() },
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
                    Text(
                        text = folder.path,
                        fontSize = 11.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!isRenaming && (visibleFields.showVideoCount || visibleFields.showSize || visibleFields.showDuration || visibleFields.showDate)) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pill/chip badges matching VideoFolderCard
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track count chip
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
                                    StyledIcon(
                                        imageVector = Icons.Filled.Headphones,
                                        contentDescription = null,
                                        tint = AccentSkyBlue,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${folder.trackCount} Tracks",
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

                        // Duration Chip
                        if (visibleFields.showDuration && folder.formattedDuration != "00:00") {
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
                                    text = folder.formattedDuration,
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
                    }
                }
            }
        }
    }
}

/**
 * Audio Folder Card - Grid Layout
 * Matches VideoFolderGridCard 1:1.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioFolderGridCard(
    folder: AudioFolderItem,
    isDark: Boolean,
    visibleFields: VisibleFields,
    isSelected: Boolean = false,
    isRenaming: Boolean = false,
    onConfirmRename: ((String) -> Unit)? = null,
    onCancelRename: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxWidth()
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
            ModernFolderIcon(
                isCustom = false,
                newCount = folder.unplayedCount,
                showNewBadge = visibleFields.showNewBadge && folder.unplayedCount > 0,
                isDark = isDark,
                isSelected = isSelected,
                size = 56.dp,
                glyphType = FolderGlyphType.AUDIO
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isRenaming) {
                InlineRenameField(
                    initialText = folder.name,
                    isDark = isDark,
                    onConfirm = { newName -> onConfirmRename?.invoke(newName) },
                    onCancel = { onCancelRename?.invoke() },
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

            if (!isRenaming && (visibleFields.showVideoCount || visibleFields.showSize || visibleFields.showDuration)) {
                Spacer(modifier = Modifier.height(4.dp))
                val subtitleParts = mutableListOf<String>()
                if (visibleFields.showVideoCount) subtitleParts.add("${folder.trackCount} tracks")
                if (visibleFields.showDuration && folder.formattedDuration != "00:00") subtitleParts.add(folder.formattedDuration)
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
