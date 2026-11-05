package com.example.ui.screens

import com.example.ui.components.cardSurface
import com.example.ui.components.ThumbnailPlayedProgress
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
 * Audio Track Card - List Layout
 * Matches VideoItemCard 1:1.
 */
/**
 * How much of [track] has been played (0..1): live for the track that is playing now, the saved position for the others
 * (same idea as the video card, where the playback history is the single source of truth). Used by the list / grid cards of
 * the audio tab, folders and playlists.
 */
@Composable
fun rememberAudioPlayedFraction(track: AudioTrackItem, isCurrent: Boolean): Float {
    val context = LocalContext.current
    val saved = remember(track.id, track.path, track.durationMs, isCurrent) {
        val dur = track.durationMs
        if (dur > 0L) {
            (com.example.util.AudioPlaybackHistoryManager.getPosition(context, track).toFloat() / dur).coerceIn(0f, 1f)
        } else track.playbackProgress
    }
    if (!isCurrent) return if (saved > 0f) saved else track.playbackProgress
    val pos by AudioPlaybackManager.currentPositionMs.collectAsState()
    val liveDur by AudioPlaybackManager.durationMs.collectAsState()
    val dur = if (liveDur > 0L) liveDur else track.durationMs
    return if (dur > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else saved
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioTrackListCard(
    track: AudioTrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDark: Boolean,
    visibleFields: VisibleFields,
    isSelected: Boolean = false,
    isRenaming: Boolean = false,
    onConfirmRename: ((String) -> Unit)? = null,
    onCancelRename: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
,
    reorderMode: Boolean = false
) {
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    val context = LocalContext.current
    var thumbnailBitmap by remember(track.id, track.path, track.thumbnailUrl) {
        val cacheKey = if (!track.thumbnailUrl.isNullOrBlank()) {
            "remote_audio_${track.thumbnailUrl}_300"
        } else {
            "audio_${track.id}_${track.path}_300"
        }
        val cached = ThumbnailCache.memoryCache.get(cacheKey)
        mutableStateOf<Bitmap?>(cached)
    }

    LaunchedEffect(track.id, track.path, track.thumbnailUrl) {
        if (thumbnailBitmap == null) {
            val loaded = if (!track.thumbnailUrl.isNullOrBlank()) {
                com.example.ui.components.loadRemoteAudioThumbnail(
                    url = track.thumbnailUrl,
                    targetSizePx = 300
                )
            } else {
                loadAudioThumbnail(
                    context = context,
                    audioId = track.id,
                    uri = track.uri,
                    path = track.path,
                    targetSizePx = 300
                )
            }
            thumbnailBitmap = loaded
        }
    }

    val playedFraction = rememberAudioPlayedFraction(track, isCurrent)
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Remove Card UI: no card (fill / border / played-progress fill), only thumbnail + text
            .cardSurface {
                this
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    // played part of the track: the card itself fills from the left (no thin line on the thumbnail)
                    .playedProgressFill(if (visibleFields.showProgressBar) playedFraction else 0f, isDark, RoundedCornerShape(16.dp))
            }
            .then(if (isSelected) Modifier.border(3.dp, AccentGradient, RoundedCornerShape(16.dp)) else Modifier)
            .then(
                if (!isRenaming && !reorderMode) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Card (Left side, exactly matching VideoItemCard 136dp x 80dp)
            if (visibleFields.showThumbnails) {
                Box(
                    modifier = Modifier
                        .width(136.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isCurrent) {
                                AccentGradient
                            } else {
                                Brush.linearGradient(
                                    colors = if (isDark) {
                                        listOf(Neutral.D1E293B, Neutral.D0F172A)
                                    } else {
                                        listOf(Neutral.E2E8F0, Neutral.CBD5E1)
                                    }
                                )
                            }
                        )
                        .then(if (isCurrent) Modifier.border(2.dp, AccentGradient, RoundedCornerShape(12.dp)) else Modifier.border(1.dp, if (isDark) DarkGlassBorder else LightGlassBorder, RoundedCornerShape(12.dp)))
                ) {
                    val bmp = thumbnailBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f))
                            )
                        }
                    }

                    // Center Art Glyph / Equalizer
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrent && isPlaying) {
                            AnimatedAudioEqualizer(isDark = isDark)
                        } else if (bmp == null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = "Audio",
                                    tint = if (isCurrent) Color.White else AccentSkyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    ThumbnailPlayedProgress(playedFraction, visibleFields.showProgressBar)

                    // Duration Badge (Bottom-Right)
                    if (visibleFields.showDuration && track.formattedDuration != "00:00") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.78f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = track.formattedDuration,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // New Badge (Top-Left, matching VideoThumbnailLoader)
                    if (visibleFields.showNewBadge && track.isNew) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 6.dp, y = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE11D48))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                }
            } else {
                // Compact non-thumbnail mode (52dp)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isCurrent) {
                                Modifier.background(AccentGradient)
                            } else {
                                Modifier.background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                            }
                        )
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent && isPlaying) {
                        AnimatedAudioEqualizer(isDark = isDark)
                    } else {
                        StyledIcon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Track",
                            tint = if (isCurrent) Color.White else AccentSkyBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (visibleFields.showNewBadge && track.isNew) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 3.dp, y = 3.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE11D48))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side metadata & details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Track Title or Inline Rename Field
                if (isRenaming) {
                    InlineRenameField(
                        initialText = track.title,
                        isDark = isDark,
                        onConfirm = { newTitle -> onConfirmRename?.invoke(newTitle) },
                        onCancel = { onCancelRename?.invoke() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                } else {
                    // Extension is shown right after the file name (never as a separate chip), local files only
                    val inlineExt = track.format.trim().trimStart('.').lowercase()
                    val titleWithExt = if (visibleFields.showAudioExtension && inlineExt.isNotBlank() &&
                        !com.example.util.isOnlineMediaPath(track.path) &&
                        !track.title.endsWith(".$inlineExt", ignoreCase = true)
                    ) "${track.title}.$inlineExt" else track.title
                    Text(
                        text = titleWithExt,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) (if (isDark) AccentSkyBlue else AccentSkyBlueStrong) else primaryText,
                        maxLines = if (visibleFields.showFullName) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 19.sp
                    )
                }

                // Artist & Album subtitle
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    fontSize = 11.5.sp,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Path (if enabled)
                if (visibleFields.showPath && track.path.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.path,
                        fontSize = 11.sp,
                        color = secondaryText.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata Pill Chips Row
                if (!isRenaming) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lyrics Chip (replaces the old separate Extension chip): shows when the track has lyrics
                        if (visibleFields.showLyrics && track.hasLyrics) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Lyrics",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        // Size Chip
                        if (visibleFields.showSize && !com.example.util.isOnlineMediaPath(track.path)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = track.formattedSize,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText
                                )
                            }
                        }

                        // Audio Type (Stereo)
                        if (visibleFields.showAudioType && track.audioType.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Color(0xFF312E81).copy(alpha = 0.5f) else Color(0xFFEEF2FF)))
                                    .border(1.dp, if (isDark) Color(0xFF4338CA).copy(alpha = 0.4f) else Color(0xFFC7D2FE), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = track.audioType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA)
                                )
                            }
                        }

                        // Date Modified Chip
                        if (visibleFields.showDate && track.formattedDate.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                                    .border(
                                        1.dp,
                                        com.example.ui.components.chipTone(if (isDark) Neutral.D334155 else Neutral.E2E8F0),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = track.formattedDate,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText
                                )
                            }
                        }
                    }
                }
            }

            if (reorderMode) {
                AudioReorderTwoLineHandle(isDark = isDark)
            }
        }
    }
}

@Composable
private fun AudioReorderTwoLineHandle(isDark: Boolean) {
    val tint = if (isDark) Neutral.S94A3B8 else Neutral.S64748B
    Column(
        modifier = Modifier.padding(start = 8.dp).size(width = 24.dp, height = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.width(20.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(tint))
        Spacer(Modifier.height(5.dp))
        Box(Modifier.width(20.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(tint))
    }
}

/**
 * Audio Track Card - Grid Layout
 * Matches VideoItemGridCard.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioTrackGridCard(
    track: AudioTrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
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
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    val context = LocalContext.current
    var thumbnailBitmap by remember(track.id, track.path, track.thumbnailUrl) {
        val cacheKey = if (!track.thumbnailUrl.isNullOrBlank()) {
            "remote_audio_${track.thumbnailUrl}_300"
        } else {
            "audio_${track.id}_${track.path}_300"
        }
        val cached = ThumbnailCache.memoryCache.get(cacheKey)
        mutableStateOf<Bitmap?>(cached)
    }

    LaunchedEffect(track.id, track.path, track.thumbnailUrl) {
        if (thumbnailBitmap == null) {
            val loaded = if (!track.thumbnailUrl.isNullOrBlank()) {
                com.example.ui.components.loadRemoteAudioThumbnail(
                    url = track.thumbnailUrl,
                    targetSizePx = 300
                )
            } else {
                loadAudioThumbnail(
                    context = context,
                    audioId = track.id,
                    uri = track.uri,
                    path = track.path,
                    targetSizePx = 300
                )
            }
            thumbnailBitmap = loaded
        }
    }

    val playedFraction = rememberAudioPlayedFraction(track, isCurrent)
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Remove Card UI: no card (fill / border / played-progress fill), only thumbnail + text
            .cardSurface {
                this
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    // played part of the track: the card itself fills from the left (no thin line on the thumbnail)
                    .playedProgressFill(if (visibleFields.showProgressBar) playedFraction else 0f, isDark, RoundedCornerShape(16.dp))
            }
            .then(if (isSelected) Modifier.border(3.dp, AccentGradient, RoundedCornerShape(16.dp)) else Modifier)
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
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) AccentGradient else Brush.linearGradient(
                            colors = if (isDark) listOf(Neutral.D1E293B, Neutral.D0F172A) else listOf(Neutral.E2E8F0, Neutral.CBD5E1)
                        )
                    )
                    .then(if (isCurrent) Modifier.border(2.dp, AccentGradient, RoundedCornerShape(12.dp)) else Modifier.border(1.dp, if (isDark) DarkGlassBorder else LightGlassBorder, RoundedCornerShape(12.dp)))
            ) {
                val bmp = thumbnailBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent && isPlaying) {
                        AnimatedAudioEqualizer(isDark = isDark)
                    } else if (bmp == null) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = "Audio",
                                tint = if (isCurrent) Color.White else AccentSkyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                ThumbnailPlayedProgress(playedFraction, visibleFields.showProgressBar)

                if (visibleFields.showDuration && track.formattedDuration != "00:00") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.Black.copy(alpha = 0.78f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = track.formattedDuration,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }


                // New Badge (Top-Left, matching VideoThumbnailLoader)
                if (visibleFields.showNewBadge && track.isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 6.dp, y = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE11D48))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isRenaming) {
                InlineRenameField(
                    initialText = track.title,
                    isDark = isDark,
                    onConfirm = { newTitle -> onConfirmRename?.invoke(newTitle) },
                    onCancel = { onCancelRename?.invoke() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            } else {
                val gridExt = track.format.trim().trimStart('.').lowercase()
                val gridTitle = if (visibleFields.showAudioExtension && gridExt.isNotBlank() &&
                    !com.example.util.isOnlineMediaPath(track.path) &&
                    !track.title.endsWith(".$gridExt", ignoreCase = true)
                ) "${track.title}.$gridExt" else track.title
                Text(
                    text = gridTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) AccentSkyBlue else primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            if (!isRenaming) {
                Spacer(modifier = Modifier.height(2.dp))

                val subtitleParts = buildList {
                    if (track.artist.isNotBlank()) add(track.artist)
                    if (visibleFields.showLyrics && track.hasLyrics) add("Lyrics")
                    if (visibleFields.showAudioType && track.audioType.isNotBlank()) add(track.audioType)
                    if (visibleFields.showDate && track.formattedDate.isNotBlank()) add(track.formattedDate)
                    if (visibleFields.showSize && !com.example.util.isOnlineMediaPath(track.path) && track.formattedSize.isNotBlank()) add(track.formattedSize)
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" • "),
                        fontSize = 11.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
