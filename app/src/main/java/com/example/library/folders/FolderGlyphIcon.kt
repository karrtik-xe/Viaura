package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink

/** Which ready-made folder artwork [ModernFolderIcon] shows. AUTO picks it from the glyph type. */
enum class FolderIconKind {
    AUTO,
    /** Video folders (Home / video section). */
    PLAY,
    /** Songs folders of the audio player. */
    SONG,
    /** Playlist section, audio side. */
    PLAYLIST_AUDIO,
    /** Playlist section, video side. */
    PLAYLIST_VIDEO,
    /** The always-first Favourites folder of the playlist section. */
    HEART
}

enum class FolderGlyphType {
    NONE,
    VIDEO,
    AUDIO,
    PLAYLIST
}

/**
 * Size of the folder icon in the folder LIST cards (normal 52dp, +5 % while Remove Card UI is ON); it grows with the
 * system font scale like the text does.
 */
@Composable
fun folderRowIconSize(base: Dp = 52.dp): Dp {
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    // Remove Card UI ON: the folder icon is 5 % bigger, OFF: normal size
    return base * maxOf(1f, fontScale) * cardlessIconScale()
}

/**
 * Folder icon:
 * - the folder artwork is a ready-made vector (res/drawable/viaura_folder_*.xml), chosen by [iconKind]
 * - the squircle container behind it and its outline keep the folder-item colours
 * - Unwatched / new count notification badge
 */
@Composable
fun ModernFolderIcon(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    folderColor: Color? = null,
    containerColor: Color? = null,
    newVideosCount: Int = 0,
    newCount: Int = newVideosCount,
    showBadge: Boolean = true,
    showNewBadge: Boolean = showBadge,
    isCustom: Boolean = false,
    isSelected: Boolean = false,
    showVideoGlyph: Boolean = true,
    glyphType: FolderGlyphType = if (showVideoGlyph) FolderGlyphType.VIDEO else FolderGlyphType.NONE,
    iconKind: FolderIconKind = FolderIconKind.AUTO
) {
    val iconRes = when (iconKind) {
        FolderIconKind.PLAY -> R.drawable.viaura_folder_play
        FolderIconKind.SONG -> R.drawable.viaura_folder_song
        FolderIconKind.PLAYLIST_AUDIO -> R.drawable.viaura_folder_playlist_audio
        FolderIconKind.PLAYLIST_VIDEO -> R.drawable.viaura_folder_playlist_video
        FolderIconKind.HEART -> R.drawable.viaura_folder_heart
        FolderIconKind.AUTO -> when (glyphType) {
            FolderGlyphType.AUDIO -> R.drawable.viaura_folder_song
            FolderGlyphType.PLAYLIST -> R.drawable.viaura_folder_playlist_video
            else -> R.drawable.viaura_folder_play
        }
    }
    val actualNewCount = if (newCount != 0) newCount else newVideosCount
    val actualShowBadge = (showBadge || showNewBadge) && actualNewCount > 0

    // Every folder kind has its own colour (blue video, orange songs, teal audio playlist, indigo video
    // playlist, pink favourites); the squircle behind the artwork, its outline and its glow follow it.
    val accent = when (iconRes) {
        R.drawable.viaura_folder_song -> Color(0xFFF2A03D)
        R.drawable.viaura_folder_playlist_audio -> Color(0xFF22B5A4)
        R.drawable.viaura_folder_playlist_video -> Color(0xFF6674EE)
        R.drawable.viaura_folder_heart -> Color(0xFFE85F83)
        else -> Color(0xFF1E9CF2)
    }
    val base = if (isDark) Color(0xFF0B192E) else Color.White
    val containerBg = containerColor ?: if (isDark) {
        accent.copy(alpha = if (isSelected) 0.30f else 0.20f).compositeOver(base.copy(alpha = 0.90f))
    } else {
        accent.copy(alpha = if (isSelected) 0.22f else 0.14f).compositeOver(base.copy(alpha = 0.96f))
    }

    val containerBorder = accent.copy(alpha = if (isDark) 0.55f else 0.60f)

    val cornerRadius = size * 0.28f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Frosted Glass / Warm Squircle Container
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isSelected) 8.dp else if (isDark) 4.dp else 2.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = accent.copy(alpha = if (isSelected) 0.70f else 0.50f),
                    spotColor = accent.copy(alpha = if (isSelected) 0.80f else 0.60f)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerBg)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = if (isDark) 0.38f else 0.30f), Color.Transparent),
                    )
                )
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, AccentGradient, RoundedCornerShape(cornerRadius))
                    } else {
                        Modifier.border(1.2.dp, containerBorder, RoundedCornerShape(cornerRadius))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // 2. Folder artwork: one ready-made vector per kind (the squircle behind it and its outline stay as before)
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(size * 0.74f)
            )
        }

        // 3. New / Unwatched videos notification badge
        if (actualShowBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF3366), Color(0xFFEF4444))
                        )
                    )
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (actualNewCount > 99) "99+" else actualNewCount.toString(),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
