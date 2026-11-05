package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlueStrong
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.VideoItem
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy

/**
 * Grid View card for Video Items
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoItemGridCard(
    video: VideoItem,
    isDark: Boolean,
    visibleFields: VisibleFields = VisibleFields(),
    isSelected: Boolean = false,
    isLastPlayed: Boolean = false,
    isRenaming: Boolean = false,
    onConfirmRename: ((String) -> Unit)? = null,
    onCancelRename: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    thumbnailStrategy: ThumbnailStrategy = ThumbnailStrategy.SMART,
    thumbnailQuality: ThumbnailQuality = ThumbnailQuality.HIGH,
    thumbnailFallbackSecond: Int = 1,
    showNetworkThumbnails: Boolean = false,
    onThumbnailClick: (() -> Unit)? = null
) {
    val surfaceColor = if (isDark) {
        DarkGlassSurface
    } else {
        LightGlassSurface
    }
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val badgeBg = com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B.copy(alpha = 0.85f) else AccentContainerLight)
    val badgeText = if (isDark) Neutral.S94A3B8 else Color(0xFF0369A1)
    val resBadgeBg = if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF)
    val resBadgeText = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA)
    val subBadgeBg = if (isDark) Color(0xFF134E4A) else Color(0xFFCCFBF1)
    val subBadgeText = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0F766E)

    val durationString = if (video.durationMs > 0) {
        formatDuration(video.durationMs)
    } else {
        "23:59"
    }

    val displayTitle = if (visibleFields.showExtension) {
        video.displayName
    } else {
        video.nameWithoutExtension
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    // Playback history is the single source of truth (video.watchedProgress is a stale snapshot taken
    // at scan time) and it is observed, so the bar updates as soon as the user returns from the player.
    val historyVersion by com.example.util.PlaybackHistoryManager.version.collectAsState()
    val effectiveProgress = remember(video.id, video.path, video.durationMs, historyVersion) {
        com.example.util.PlaybackHistoryManager.getProgressFraction(context, video.id, video.path, video.durationMs)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Remove Card UI (Settings > Appearance > Display & Feel): no card at all, only thumbnail + text on the page
            .cardSurface {
                this
                    // Liquid Glass: frosted rounded card with specular border; OFF = the original card
                    .liquidGlassOr(RoundedCornerShape(16.dp), isDark, LocalPanelTransparency.current, elevation = 4.dp, backdropBlur = false) {
                        this
                            .shadow(
                                elevation = 0.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = AccentSkyBlue.copy(alpha = 0.35f),
                                spotColor = AccentPink.copy(alpha = 0.45f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(com.example.ui.components.cardTone(surfaceColor))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    }
                    // watched progress: the card itself fills from the left up to the played part (no thin line on the thumbnail)
                    .playedProgressFill(if (visibleFields.showProgressBar) effectiveProgress else 0f, isDark, RoundedCornerShape(16.dp))
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
            if (visibleFields.showThumbnails) {
                VideoThumbnailCard(
                    videoId = video.id,
                    videoUri = video.uri,
                    videoPath = video.path,
                    durationText = durationString,
                    isNew = video.isNew && visibleFields.showNewBadge,
                    isDark = isDark,
                    customGradientSeed = video.displayName.hashCode(),
                    showDuration = visibleFields.showDuration,
                    showProgressBar = visibleFields.showProgressBar,
                    watchedProgress = effectiveProgress,
                    isSelected = false,
                    isLastPlayed = isLastPlayed,
                    thumbnailStrategy = thumbnailStrategy,
                    thumbnailQuality = thumbnailQuality,
                    thumbnailFallbackSecond = thumbnailFallbackSecond,
                    showNetworkThumbnails = showNetworkThumbnails,
                    remoteThumbnailUrl = video.thumbnailUrl,
                    onThumbnailClick = onThumbnailClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                        .then(
                            if (isLastPlayed) {
                                Modifier.border(2.dp, AccentGradient, RoundedCornerShape(12.dp))
                            } else {
                                Modifier.border(1.dp, if (isDark) Neutral.D334155 else Neutral.CBD5E1, RoundedCornerShape(12.dp))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                        contentDescription = null,
                        tint = if (isDark) AccentSkyBlue else AccentSkyBlueStrong,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isRenaming) {
                val baseName = if (visibleFields.showExtension) video.displayName else video.nameWithoutExtension
                InlineRenameField(
                    initialText = baseName,
                    onConfirm = { newName -> onConfirmRename?.invoke(newName) },
                    onCancel = { onCancelRename?.invoke() },
                    isDark = isDark,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            } else {
                Text(
                    text = displayTitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = if (visibleFields.showFullName) 3 else 2,
                    minLines = if (visibleFields.showFullName) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                if (video.isOnline) {
                    val metaLine = listOfNotNull(video.channelName?.takeIf { it.isNotBlank() }, video.viewCountText?.takeIf { it.isNotBlank() }).joinToString(" • ")
                    if (metaLine.isNotEmpty()) {
                        Text(
                            text = metaLine,
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (visibleFields.showSize && (!video.isOnline || video.isArchiveEntry)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = video.formattedSize,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }

                if (visibleFields.showResolution && video.resolution.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(resBadgeBg)
                            .padding(horizontal = 5.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = video.resolution,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = resBadgeText
                        )
                    }
                }

                if (visibleFields.showFramerate && video.framerate > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 5.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = video.formattedFramerate,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    }
                }

                if (visibleFields.showSubtitleIndicator && (!video.isOnline || video.isArchiveEntry)) {
                    val subText = resolveSubtitleBadgeText(video.effectiveSubtitleFormats, video.displayName, video.hasSubtitles)
                    if (subText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(subBadgeBg)
                                .border(
                                    1.dp,
                                    if (isDark) Color(0xFF0D9488).copy(alpha = 0.4f) else Color(0xFF99F6E4),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 5.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = subText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = subBadgeText
                            )
                        }
                    }
                }

                if (visibleFields.showDate && video.formattedDate.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(com.example.ui.components.chipTone(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
                        .border(
                            1.dp,
                            com.example.ui.components.chipTone(if (isDark) Neutral.D334155.copy(alpha = 0.7f) else Neutral.E2E8F0),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = video.formattedDate,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
