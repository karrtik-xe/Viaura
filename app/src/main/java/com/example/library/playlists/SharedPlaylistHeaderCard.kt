package com.example.ui.screens

import com.example.ui.components.ThumbnailPlayedProgress
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

/**
 * Playlist Header Section:
 * - 2x2 Thumbnail Grid below top bar
 * - Playlist Name + Artist Name
 * - Play & Shuffle Buttons using viaura_* local resource icons (NOT default Material icons)
 */
@Composable
internal fun PlaylistHeaderSection(
    playlist: OnlinePlaylistDetail,
    randomFourThumbnails: List<String>,
    allTrackItems: List<AudioTrackItem>,
    surfaceColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSaveClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(borderColor, borderColor.copy(alpha = 0.4f)))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Horizontal row with 2x2 collage on the left, and Title, 3-dots, Platform badge, song count, and Artist on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 2x2 Thumbnail Grid Collage on the left
                PlaylistCollageHeader(
                    thumbnails = randomFourThumbnails,
                    isDark = isDark,
                    modifier = Modifier.size(112.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Playlist Info Column on the right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Playlist Name + 3-Dot Options Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                lineHeight = 22.sp
                            ),
                            color = primaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        IconButton(
                            onClick = onSaveClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.viaura_menu_dots),
                                contentDescription = "Playlist Options",
                                tint = secondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Platform Badge & Song count
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isSpotify = playlist.sourcePlatform.contains("Spotify", ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSpotify) {
                                Color(0xFF1DB954).copy(alpha = 0.18f)
                            } else {
                                Color(0xFFFF0000).copy(alpha = 0.16f)
                            },
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        if (isSpotify) Color(0xFF1DB954).copy(alpha = 0.5f) else Color(0xFFFF0000).copy(alpha = 0.5f),
                                        borderColor
                                    )
                                )
                            )
                        ) {
                            Text(
                                text = playlist.sourcePlatform,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSpotify) Color(0xFF1DB954) else Color(0xFFFF4E4E),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "${allTrackItems.size} ${if (allTrackItems.size == 1) "song" else "songs"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = secondaryText
                        )
                    }

                    // Artist Name below platform and song count
                    if (playlist.author.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist.author,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Play & Shuffle using local resource icons (NOT Material icons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Play Button using local viaura_play_solid resource
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.viaura_play_solid),
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Shuffle Button using local viaura_shuffle resource
                FilledTonalButton(
                    onClick = onShuffle,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isDark) Neutral.D1E293B else Neutral.F1F5F9,
                        contentColor = primaryText
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.viaura_shuffle),
                        contentDescription = "Shuffle",
                        tint = primaryText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shuffle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Track Item Card - List Mode
 * STRICT REQUIREMENTS:
 * - NO "M4A", "0.0 MB", or "Stereo" tags!
 * - ONLY Song Title, Artist Name, and Three-Dot Menu!
 */
@Composable
fun SharedTrackListCard(
    track: AudioTrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (isDark) {
        DarkGlassSurface
    } else {
        LightGlassSurface
    }
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    val context = LocalContext.current
    var thumbnailBitmap by remember(track.id, track.path, track.thumbnailUrl) {
        val cacheKey = if (!track.thumbnailUrl.isNullOrBlank()) {
            "remote_audio_${track.thumbnailUrl}_600"
        } else {
            "audio_${track.id}_${track.path}_600"
        }
        mutableStateOf(ThumbnailCache.memoryCache.get(cacheKey))
    }

    LaunchedEffect(track.id, track.path, track.thumbnailUrl) {
        if (thumbnailBitmap == null) {
            val loaded = if (!track.thumbnailUrl.isNullOrBlank()) {
                loadRemoteAudioThumbnail(url = track.thumbnailUrl, targetSizePx = 600)
            } else {
                loadAudioThumbnail(context = context, audioId = track.id, uri = track.uri, path = track.path, targetSizePx = 600)
            }
            thumbnailBitmap = loaded
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardSurface {
                this
                    .liquidGlassOr(RoundedCornerShape(16.dp), isDark, com.example.ui.components.LocalPanelTransparency.current, elevation = 4.dp, backdropBlur = false) {
                        this
                            .clip(RoundedCornerShape(16.dp))
                            .background(com.example.ui.components.cardTone(surfaceColor))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    }
                    .playedProgressFill(track.playbackProgress, isDark, RoundedCornerShape(16.dp))
            }
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Thumbnail (136.dp x 80dp)
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) AccentGradient else Brush.linearGradient(
                            if (isDark) listOf(Neutral.D1E293B, Neutral.D0F172A) else listOf(Neutral.E2E8F0, Neutral.CBD5E1)
                        )
                    )
                    .then(if (isCurrent) Modifier.border(2.5.dp, AccentGradient, RoundedCornerShape(12.dp)) else Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp)))
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

                ThumbnailPlayedProgress(track.playbackProgress)

                // Equalizer or fallback headphones
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
                            Icon(
                                painter = painterResource(id = R.drawable.viaura_audio_track),
                                contentDescription = "Audio",
                                tint = if (isCurrent) Color.White else AccentSkyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Duration Badge
                if (track.formattedDuration.isNotBlank() && track.formattedDuration != "00:00") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp)
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

            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Column: ONLY Song Title & Artist Name
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) (if (isDark) AccentSkyBlue else AccentSkyBlueStrong) else primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist.ifBlank { "Unknown Artist" },
                    fontSize = 12.sp,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Track Item Card - Grid Mode
 * STRICT REQUIREMENTS:
 * - NO "M4A", "0.0 MB", or "Stereo" tags!
 * - ONLY Song Title, Artist Name, and Three-Dot Menu!
 */
@Composable
fun SharedTrackGridCard(
    track: AudioTrackItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (isDark) {
        DarkGlassSurface
    } else {
        LightGlassSurface
    }
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    val context = LocalContext.current
    var thumbnailBitmap by remember(track.id, track.path, track.thumbnailUrl) {
        val cacheKey = if (!track.thumbnailUrl.isNullOrBlank()) {
            "remote_audio_${track.thumbnailUrl}_600"
        } else {
            "audio_${track.id}_${track.path}_600"
        }
        mutableStateOf(ThumbnailCache.memoryCache.get(cacheKey))
    }

    LaunchedEffect(track.id, track.path, track.thumbnailUrl) {
        if (thumbnailBitmap == null) {
            val loaded = if (!track.thumbnailUrl.isNullOrBlank()) {
                loadRemoteAudioThumbnail(url = track.thumbnailUrl, targetSizePx = 600)
            } else {
                loadAudioThumbnail(context = context, audioId = track.id, uri = track.uri, path = track.path, targetSizePx = 600)
            }
            thumbnailBitmap = loaded
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .cardSurface {
                this
                    .liquidGlassOr(RoundedCornerShape(16.dp), isDark, com.example.ui.components.LocalPanelTransparency.current, elevation = 4.dp, backdropBlur = false) {
                        this
                            .clip(RoundedCornerShape(16.dp))
                            .background(com.example.ui.components.cardTone(surfaceColor))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    }
            }
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) AccentGradient else Brush.linearGradient(
                            if (isDark) listOf(Neutral.D1E293B, Neutral.D0F172A) else listOf(Neutral.E2E8F0, Neutral.CBD5E1)
                        )
                    )
                    .then(if (isCurrent) Modifier.border(2.5.dp, AccentGradient, RoundedCornerShape(12.dp)) else Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp)))
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

                // Equalizer or fallback
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent && isPlaying) {
                        AnimatedAudioEqualizer(isDark = isDark)
                    } else if (bmp == null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.viaura_audio_track),
                                contentDescription = "Audio",
                                tint = if (isCurrent) Color.White else AccentSkyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Duration Badge
                if (track.formattedDuration.isNotBlank() && track.formattedDuration != "00:00") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row: Title + 3-Dot Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) (if (isDark) AccentSkyBlue else AccentSkyBlueStrong) else primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Artist Name ONLY (NO M4A, 0.0 MB, Stereo tags!)
            Text(
                text = track.artist.ifBlank { "Unknown Artist" },
                fontSize = 11.5.sp,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2x2 Artwork Collage displaying 4 randomly selected tracks' high-resolution thumbnails.
 */
@Composable
private fun PlaylistCollageHeader(
    thumbnails: List<String>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val surfaceColor = if (isDark) Neutral.D0F172A else Neutral.E2E8F0

    val displayThumbs = remember(thumbnails) {
        if (thumbnails.isEmpty()) {
            emptyList()
        } else if (thumbnails.size >= 4) {
            thumbnails.take(4)
        } else {
            val list = thumbnails.toMutableList()
            while (list.size < 4) {
                list.add(thumbnails[list.size % thumbnails.size])
            }
            list
        }
    }

    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    ) {
        if (displayThumbs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageImageItem(
                        url = displayThumbs[0],
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                    CollageImageItem(
                        url = displayThumbs[1],
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(borderColor))
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CollageImageItem(
                        url = displayThumbs[2],
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(borderColor))
                    CollageImageItem(
                        url = displayThumbs[3],
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageImageItem(
    url: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) {
        mutableStateOf(ThumbnailCache.memoryCache.get("remote_audio_${url}_800"))
    }

    LaunchedEffect(url) {
        if (bitmap == null && url.isNotBlank()) {
            bitmap = loadRemoteAudioThumbnail(url, targetSizePx = 800)
        }
    }

    Box(
        modifier = modifier.background(Neutral.D1E293B),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
