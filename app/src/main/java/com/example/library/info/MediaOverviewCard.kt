package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BottomMoveCopyProgressBar
import com.example.ui.components.DirectoryPickerAction
import com.example.ui.components.FloatingDirectoryPickerSheet
import com.example.ui.components.isRestrictedDirectory
import com.example.ui.components.HeaderBar
import com.example.ui.components.StyledIcon
import com.example.ui.components.bounceClick
import com.example.ui.components.loadVideoThumbnail
import com.example.ui.state.SettingsPreferencesManager
import com.example.ui.state.ThemeMode
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
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
import com.example.util.MkvAttachmentInfo
import com.example.util.MkvChapterInfo
import com.example.util.MkvContainerMetadata
import com.example.util.MkvManagerEngine
import com.example.util.MkvTrackInfo
import com.example.util.MkvTrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
internal fun OverviewCard(
    data: MediaInfoData,
    video: VideoItem,
    bitmap: android.graphics.Bitmap?,
    searchQuery: String,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color,
    onCopy: (String) -> Unit
) {
    val meta = data.metadata
    val all = data.tracks
    val videos = all.filter { it.trackType == MkvTrackType.VIDEO }
    val audios = all.filter { it.trackType == MkvTrackType.AUDIO }
    val subs = all.filter { it.trackType == MkvTrackType.SUBTITLE }
    val duration = meta.durationMs
    val overallBitrate = if (duration > 0) (meta.fileSizeBytes * 8_000L / duration) else 0L
    val videoCodecs = videos.map { it.displayCodec }.distinct().joinToString(", ").ifBlank { "—" }
    val audioCodecs = audios.map { it.displayCodec }.distinct().joinToString(", ").ifBlank { "—" }
    val subFormats = subs.map { subtitleFormatLabel(it) }.distinct().joinToString(", ").ifBlank { "—" }
    val languages = all.map { it.language }.filter { it.isNotBlank() && it != "und" }.distinct().joinToString(", ").ifBlank { "—" }
    val resolution = videos.firstOrNull()?.displayResolution ?: "—"
    val fps = videos.firstOrNull()?.frameRate ?: 0f
    val hdr = videos.firstOrNull()?.hdrFormat?.ifBlank { "SDR" } ?: "SDR"

    val generalRows = listOfNotNull(
        "File Name" to meta.fileName,
        "Format" to (meta.fileName.substringAfterLast('.', "unknown").uppercase(Locale.ROOT).let {
            if (it == "MKV" || it == "WEBM") "Matroska" else it
        }),
        meta.formattedFormatVersion.takeIf { it.isNotBlank() }?.let { "Format Version" to it },
        "File Size" to formatBytes(meta.fileSizeBytes),
        "Duration" to formatMs(meta.durationMs),
        "Overall Bitrate" to formatBitrate(overallBitrate),
        meta.dateUtc.takeIf { it.isNotBlank() }?.let { "Encoded Date" to it },
        meta.muxingApp.takeIf { it.isNotBlank() }?.let { "Muxing App" to it },
        meta.writingApp.takeIf { it.isNotBlank() }?.let { "Writing App" to it }
    )

    val summaryRows = listOf(
        "Video Tracks" to videos.size.toString(),
        "Audio Tracks" to audios.size.toString(),
        "Subtitle Tracks" to subs.size.toString(),
        "Chapters" to data.chapters.size.toString(),
        "Attachments" to data.attachments.size.toString(),
        "Languages" to languages
    )

    val streamRows = listOf(
        "Resolution" to resolution,
        "Frame Rate" to if (fps > 0) "${"%.3f".format(Locale.US, fps)} FPS" else "—",
        "HDR / SDR" to hdr,
        "Video Codec(s)" to videoCodecs,
        "Audio Codec(s)" to audioCodecs,
        "Subtitle Format(s)" to subFormats
    )

    val q = searchQuery.trim().lowercase(Locale.ROOT)
    val filterRows = { list: List<Pair<String, String>> ->
        if (q.isBlank()) list else list.filter {
            it.first.lowercase(Locale.ROOT).contains(q) || it.second.lowercase(Locale.ROOT).contains(q)
        }
    }

    val filteredGeneral = filterRows(generalRows)
    val filteredSummary = filterRows(summaryRows)
    val filteredStream = filterRows(streamRows)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (filteredGeneral.isNotEmpty() || (q.isBlank() && bitmap != null)) {
            InfoCard(
                title = "General Information",
                icon = Icons.Outlined.Description,
                trailingContent = {
                    ActionRow(
                        onCopy = { onCopy(buildInformationText(data, MediaInfoSection.OVERVIEW, video)) },
                        onShare = null,
                        isDark = isDark
                    )
                },
                isDark = isDark, border = border, surface = surface
            ) {
                if (bitmap != null && q.isBlank()) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Video thumbnail",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(10.dp))
                }
                if (filteredGeneral.isNotEmpty()) {
                    InfoGrid(filteredGeneral, primary, secondary)
                }
            }
        }

        if (filteredSummary.isNotEmpty()) {
            InfoCard(
                title = "Track Summary",
                icon = Icons.Outlined.DashboardCustomize,
                badgeText = "${all.size} Tracks",
                badgeColor = AccentSkyBlue,
                isDark = isDark, border = border, surface = surface
            ) {
                InfoGrid(filteredSummary, primary, secondary)
            }
        }

        if (filteredStream.isNotEmpty()) {
            InfoCard(
                title = "Display & Codec Details",
                icon = Icons.Outlined.VideoFile,
                badgeText = resolution.takeIf { it != "—" },
                badgeColor = AccentPink,
                isDark = isDark, border = border, surface = surface
            ) {
                InfoGrid(filteredStream, primary, secondary)
            }
        }

        if (filteredGeneral.isEmpty() && filteredSummary.isEmpty() && filteredStream.isEmpty()) {
            EmptyInfo("No overview properties matching \"$searchQuery\".", primary, secondary, surface, border)
        }
    }
}

@Composable
internal fun TrackCard(
    track: MkvTrackInfo,
    type: MkvTrackType,
    thumbnail: android.graphics.Bitmap? = null,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color,
    fileSizeBytes: Long = 0L,
    onExtract: () -> Unit
) {
    val isVideo = type == MkvTrackType.VIDEO
    val icon = if (isVideo) Icons.Outlined.VideoFile else Icons.Outlined.Audiotrack
    val badgeLabel = if (isVideo) track.displayCodec.ifBlank { "VIDEO" } else track.language.ifBlank { track.displayCodec }
    val badgeColor = if (isVideo) AccentSkyBlue else AccentPink

    InfoCard(
        title = "${type.displayName} Track #${track.trackNumber}",
        icon = icon,
        badgeText = badgeLabel,
        badgeColor = badgeColor,
        isDark = isDark, border = border, surface = surface
    ) {
        if (isVideo && thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "Video preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(10.dp))
        }

        InfoGrid(
            listOfNotNull(
                "Track Name" to track.name.ifBlank { "Track #${track.trackNumber}" },
                "Language" to track.language.ifBlank { "und" },
                "Codec" to track.displayCodec,
                "Format / ID" to track.codecId.ifBlank { "—" },
                track.formatInfo.takeIf { it.isNotBlank() }?.let { "Format/Info" to it },
                if (isVideo) "Resolution" to track.displayResolution else null,
                if (isVideo) "Frame Rate" to if (track.frameRate > 0) "%.3f FPS".format(Locale.US, track.frameRate) else "—" else null,
                if (isVideo) track.frameRateMode.takeIf { it.isNotBlank() }?.let { "Frame Rate Mode" to it } else null,
                if (isVideo) "Bitrate" to formatBitrate(track.bitrateBps) else null,
                if (isVideo) track.formatProfile.takeIf { it.isNotBlank() }?.let { "Format Profile" to it } else null,
                if (isVideo) track.formatTier.takeIf { it.isNotBlank() }?.let { "Format Tier" to it } else null,
                if (isVideo) track.formatLevel.takeIf { it.isNotBlank() }?.let { "Format Level" to it } else null,
                if (isVideo) "Bit Depth" to if (track.bitDepth > 0) "${track.bitDepth}-bit" else "—" else null,
                if (isVideo) track.chromaSubsampling.takeIf { it.isNotBlank() }?.let { "Chroma Subsampling" to it } else null,
                if (isVideo) "Color Space" to track.colorSpace.ifBlank { "—" } else null,
                if (isVideo) "HDR Format" to track.hdrFormat.ifBlank { "SDR" } else null,
                if (isVideo) "Aspect Ratio" to track.aspectRatio.ifBlank { "—" } else null,
                if (isVideo) track.bitsPerPixelFrame.takeIf { it.isNotBlank() }?.let { "Bits/(Pixel*Frame)" to it } else null,
                if (isVideo) "Duration" to formatMs(if (track.durationMs > 0) track.durationMs else 0) else null,
                if (!isVideo) "Channels" to channelLabel(track.channels) else null,
                if (!isVideo) "Sample Rate" to if (track.sampleRate > 0) "${track.sampleRate} Hz" else "—" else null,
                if (!isVideo) "Bit Depth" to if (track.bitDepth > 0) "${track.bitDepth}-bit" else "—" else null,
                if (!isVideo) "Bitrate" to formatBitrate(track.bitrateBps) else null,
                if (!isVideo) "Delay vs Video" to if (track.delayRelativeToVideoMs != 0L) "${track.delayRelativeToVideoMs} ms" else "—" else null,
                if (!isVideo) "Duration" to formatMs(if (track.durationMs > 0) track.durationMs else 0) else null,
                track.compressionMode.takeIf { it.isNotBlank() }?.let { "Compression Mode" to it },
                "Stream Size" to track.formattedStreamSize(fileSizeBytes),
                "Default / Forced" to "${if (track.isDefault) "Default" else "No"} / ${if (track.isForced) "Forced" else "No"}",
                "Hearing Impaired" to if (track.isHearingImpaired) "Yes" else "No",
                "Commentary" to if (track.isCommentary) "Yes" else "No"
            ),
            primary, secondary
        )
        Spacer(Modifier.height(10.dp))
        ExtractButton(
            text = if (isVideo) "Extract Video Stream" else "Extract Audio Stream",
            onClick = onExtract
        )
    }
}

@Composable
internal fun SubtitleTrackCard(
    track: MkvTrackInfo,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color,
    fileSizeBytes: Long = 0L,
    onCopy: () -> Unit,
    onExtract: () -> Unit
) {
    InfoCard(
        title = "Subtitle Track #${track.trackNumber}",
        icon = Icons.Outlined.Subtitles,
        badgeText = track.language.ifBlank { "SUB" },
        badgeColor = AccentSkyBlue,
        trailingContent = {
            SmallAction("Copy", Icons.Outlined.ContentCopy, AccentSkyBlue, onCopy)
        },
        isDark = isDark, border = border, surface = surface
    ) {
        InfoGrid(
            listOfNotNull(
                "Track Name" to track.name.ifBlank { "Track #${track.trackNumber}" },
                "Language" to track.language.ifBlank { "und" },
                "Format / Codec" to subtitleFormatLabel(track),
                "Format / ID" to track.codecId.ifBlank { "—" },
                "Character Encoding" to if (track.codecId.contains("UTF8", true) || track.codecId.contains("ASS", true) || track.codecId.contains("SSA", true)) "UTF-8" else "Container native",
                "Duration" to formatMs(track.durationMs),
                "Bit Rate" to formatBitrate(track.bitrateBps),
                track.compressionMode.takeIf { it.isNotBlank() }?.let { "Compression Mode" to it },
                if (track.countOfElements > 0) "Count of Elements" to track.countOfElements.toString() else null,
                "Stream Size" to track.formattedStreamSize(fileSizeBytes),
                "Default" to if (track.isDefault) "Yes" else "No",
                "Forced" to if (track.isForced) "Yes" else "No",
                "Hearing Impaired" to if (track.isHearingImpaired) "Yes" else "No"
            ),
            primary, secondary
        )
        Spacer(Modifier.height(10.dp))
        ExtractButton(
            text = "Extract Subtitle File",
            onClick = onExtract
        )
    }
}

@Composable
internal fun ChapterInfoCard(
    chapters: List<MkvChapterInfo>,
    totalChapterCount: Int,
    searchQuery: String,
    format: String,
    onFormatChange: (String) -> Unit,
    onCopy: () -> Unit,
    onExtract: () -> Unit,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color
) {
    val countLabel = if (searchQuery.isBlank()) "$totalChapterCount Chapters" else "${chapters.size} / $totalChapterCount Chapters"
    InfoCard(
        title = "Chapters",
        icon = Icons.Outlined.Bookmarks,
        badgeText = countLabel,
        badgeColor = AccentPink,
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallAction("XML", Icons.Outlined.Bookmarks, if (format == "xml") AccentPink else secondary) { onFormatChange("xml") }
                SmallAction("OGM", Icons.Outlined.Bookmarks, if (format == "ogm") AccentPink else secondary) { onFormatChange("ogm") }
                SmallAction("Copy", Icons.Outlined.ContentCopy, AccentSkyBlue, onCopy)
            }
        },
        isDark = isDark, border = border, surface = surface
    ) {
        if (chapters.isEmpty()) {
            Text(
                if (searchQuery.isBlank()) "No chapters detected in container." else "No chapters matching \"$searchQuery\".",
                color = secondary, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chapters.forEachIndexed { index, ch ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                            .border(1.dp, border.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentPink.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d", index + 1),
                                    color = AccentPink,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = ch.title.ifBlank { "Chapter ${index + 1}" },
                                    color = primary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ch.formattedStartTime}${if (ch.endTimeMs > 0) " → ${ch.formattedEndTime}" else ""} • ${ch.language.ifBlank { "eng" }}",
                                    color = secondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ExtractButton(
            text = "Extract Chapters (.${if (format == "xml") "xml" else "txt"})",
            onClick = onExtract
        )
    }
}

@Composable
internal fun AttachmentCard(
    attachment: MkvAttachmentInfo,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color,
    onExtract: () -> Unit
) {
    InfoCard(
        title = attachment.fileName.ifBlank { "Attachment #${attachment.id}" },
        icon = Icons.Outlined.AttachFile,
        badgeText = if (attachment.isFont) "Font (.${attachment.fileName.substringAfterLast('.', "ttf")})" else "File",
        badgeColor = if (attachment.isFont) AccentSkyBlue else secondary,
        isDark = isDark, border = border, surface = surface
    ) {
        InfoGrid(
            listOf(
                "File Name" to attachment.fileName,
                "MIME Type" to attachment.mimeType,
                "Size" to attachment.formattedSize,
                "Description" to attachment.description.ifBlank { "—" },
                "Type" to if (attachment.isFont) "Embedded Font" else "Attachment"
            ), primary, secondary
        )
        Spacer(Modifier.height(10.dp))
        ExtractButton(
            text = "Extract ${if (attachment.isFont) "Font File" else "Attachment"}",
            onClick = onExtract
        )
    }
}

@Composable
private fun ExtractButton(
    text: String = "Extract",
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AccentGradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyledIcon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
    }
}

@Composable
private fun ActionRow(onCopy: () -> Unit, onShare: (() -> Unit)?, isDark: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SmallAction("Copy", Icons.Outlined.ContentCopy, AccentSkyBlue, onCopy)
        if (onShare != null) SmallAction("Share", Icons.Outlined.Share, AccentPink, onShare)
    }
}

@Composable
private fun SmallAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, tint.copy(alpha = .35f), RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        StyledIcon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoBadge(
    text: String,
    color: Color = AccentSkyBlue,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
internal fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badgeText: String? = null,
    badgeColor: Color = AccentSkyBlue,
    trailingContent: (@Composable () -> Unit)? = null,
    isDark: Boolean,
    border: Color,
    surface: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(surface)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                color = if (isDark) DarkTextPrimary else LightTextPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                Spacer(Modifier.width(8.dp))
                InfoBadge(text = badgeText, color = badgeColor)
            }
            if (trailingContent != null) {
                Spacer(Modifier.weight(1f))
                trailingContent()
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun InfoGrid(items: List<Pair<String, String>>, primary: Color, secondary: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (k, v) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(k, color = secondary, fontSize = 12.sp, modifier = Modifier.width(140.dp))
                Text(v.ifBlank { "—" }, color = primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun EmptyInfo(message: String, primary: Color, secondary: Color, surface: Color, border: Color) {
    InfoCard("No Data", isDark = true, border = border, surface = surface) {
        Text(message, color = secondary, fontSize = 13.sp)
    }
}

internal fun subtitleFormatLabel(track: MkvTrackInfo): String {
    val c = track.codecId.uppercase(Locale.ROOT)
    return when {
        c.contains("ASS") || c.contains("SSA") -> "Advanced SubStation Alpha (.ass)"
        c.contains("UTF8") || c.contains("SRT") -> "SubRip Text (.srt)"
        c.contains("WEBVTT") || c.contains("VTT") -> "WebVTT (.vtt)"
        c.contains("PGS") || c.contains("HDMV") -> "HDMV PGS Bitmap (.sup)"
        c.contains("VOBSUB") -> "VobSub Bitmap (.sub)"
        else -> track.displayCodec
    }
}

internal fun videoExtension(track: MkvTrackInfo): String {
    val c = track.codecId.uppercase(Locale.ROOT)
    val name = track.codecName.uppercase(Locale.ROOT)
    return when {
        c.contains("AVC") || c.contains("H264") || name.contains("AVC") || name.contains("H.264") -> "h264"
        c.contains("HEVC") || c.contains("H265") || name.contains("HEVC") || name.contains("H.265") -> "hevc"
        c.contains("AV1") -> "av1"
        c.contains("VP9") -> "vp9"
        c.contains("VP8") -> "vp8"
        else -> "mkv"
    }
}

internal fun audioExtension(track: MkvTrackInfo): String {
    val c = track.codecId.uppercase(Locale.ROOT)
    val name = track.codecName.uppercase(Locale.ROOT)
    val display = track.displayCodec.uppercase(Locale.ROOT)
    val combined = "$c $name $display"
    return when {
        combined.contains("OPUS") -> "opus"
        combined.contains("AAC") || combined.contains("MP4A") || combined.contains("M4A") -> "m4a"
        combined.contains("EAC3") || combined.contains("EC-3") || combined.contains("DOLBY DIGITAL PLUS") -> "ac3"
        combined.contains("AC3") || combined.contains("AC-3") || combined.contains("DOLBY DIGITAL") -> "ac3"
        combined.contains("TRUEHD") -> "thd"
        combined.contains("MPEG/L3") || combined.contains("MP3") || combined.contains("MPEGLAYER3") -> "mp3"
        combined.contains("FLAC") -> "flac"
        combined.contains("VORBIS") || combined.contains("OGG") -> "ogg"
        combined.contains("DTS-HD") || combined.contains("DTSHD") -> "dtshd"
        combined.contains("DTS") -> "dts"
        combined.contains("ALAC") -> "m4a"
        combined.contains("PCM") || combined.contains("WAV") || combined.contains("RIFF") -> "wav"
        combined.contains("WMA") -> "wma"
        else -> "m4a"
    }
}

internal fun subtitleExtension(track: MkvTrackInfo): String {
    val c = track.codecId.uppercase(Locale.ROOT)
    val name = track.codecName.uppercase(Locale.ROOT)
    val display = track.displayCodec.uppercase(Locale.ROOT)
    val combined = "$c $name $display"
    return when {
        combined.contains("ASS") || combined.contains("SSA") || combined.contains("SUBSTATION") -> "ass"
        combined.contains("WEBVTT") || combined.contains("VTT") -> "vtt"
        combined.contains("PGS") || combined.contains("HDMV") -> "sup"
        combined.contains("VOBSUB") || combined.contains("DVD_SUB") -> "sub"
        combined.contains("TTML") || combined.contains("DFXP") -> "ttml"
        combined.contains("UTF8") || combined.contains("SRT") || combined.contains("SUBRIP") -> "srt"
        else -> "srt"
    }
}

internal fun channelLabel(channels: Int): String {
    return when (channels) {
        1 -> "1.0 (Mono)"
        2 -> "2.0 (Stereo)"
        6 -> "5.1 Surround (6 ch)"
        8 -> "7.1 Surround (8 ch)"
        else -> if (channels > 0) "$channels channels" else "—"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSecs = ms / 1000
    val s = totalSecs % 60
    val m = (totalSecs / 60) % 60
    val h = totalSecs / 3600
    return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

private fun formatBitrate(bps: Long): String {
    if (bps <= 0) return "—"
    val kbps = bps / 1000.0
    val mbps = kbps / 1000.0
    return if (mbps >= 1.0) String.format(Locale.US, "%.2f Mbps", mbps)
    else String.format(Locale.US, "%.0f kbps", kbps)
}

internal fun sanitizeFileComponent(name: String): String {
    val cleaned = name.replace(Regex("""[/\\:*?"<>|\u0000]"""), "_").trim()
    return cleaned.ifBlank { "media" }
}

private fun mapLanguageToSimpleName(langRaw: String): String? {
    val clean = langRaw.trim().lowercase(Locale.ROOT)
    if (clean.isBlank() || clean == "und" || clean == "unk" || clean == "unknown" || clean == "mis" || clean == "qaa") {
        return null
    }
    return when (clean) {
        "hi", "hin", "hindi" -> "hindi"
        "en", "eng", "english" -> "english"
        "ta", "tam", "tamil" -> "tamil"
        "te", "tel", "telugu" -> "telugu"
        "ml", "mal", "malayalam" -> "malayalam"
        "kn", "kan", "kannada" -> "kannada"
        "mr", "mar", "marathi" -> "marathi"
        "bn", "ben", "bengali", "bangla" -> "bengali"
        "gu", "guj", "gujarati" -> "gujarati"
        "pa", "pan", "punjabi" -> "punjabi"
        "ur", "urd", "urdu" -> "urdu"
        "bho", "bhojpuri" -> "bhojpuri"
        "or", "ori", "odia", "oriya" -> "odia"
        "as", "asm", "assamese" -> "assamese"
        "ne", "nep", "nepali" -> "nepali"
        "sa", "san", "sanskrit" -> "sanskrit"
        "si", "sin", "sinhala" -> "sinhala"
        "es", "spa", "spanish" -> "spanish"
        "fr", "fra", "fre", "french" -> "french"
        "de", "deu", "ger", "german" -> "german"
        "it", "ita", "italian" -> "italian"
        "pt", "por", "portuguese" -> "portuguese"
        "ru", "rus", "russian" -> "russian"
        "ja", "jpn", "japanese" -> "japanese"
        "ko", "kor", "korean" -> "korean"
        "zh", "zho", "chi", "chinese", "mandarin", "cantonese" -> "chinese"
        "ar", "ara", "arabic" -> "arabic"
        "tr", "tur", "turkish" -> "turkish"
        "th", "tha", "thai" -> "thai"
        "vi", "vie", "vietnamese" -> "vietnamese"
        "id", "ind", "indonesian" -> "indonesian"
        "ms", "msa", "may", "malay" -> "malay"
        "fa", "fas", "per", "persian", "farsi" -> "persian"
        "pl", "pol", "polish" -> "polish"
        "nl", "nld", "dut", "dutch" -> "dutch"
        "sv", "swe", "swedish" -> "swedish"
        "no", "nor", "norwegian" -> "norwegian"
        "da", "dan", "danish" -> "danish"
        "fi", "fin", "finnish" -> "finnish"
        "el", "ell", "gre", "greek" -> "greek"
        "he", "heb", "hebrew" -> "hebrew"
        "hu", "hun", "hungarian" -> "hungarian"
        "cs", "ces", "cze", "czech" -> "czech"
        "ro", "ron", "rum", "romanian" -> "romanian"
        "uk", "ukr", "ukrainian" -> "ukrainian"
        "tl", "tgl", "fil", "tagalog", "filipino" -> "filipino"
        else -> {
            try {
                val loc = Locale.forLanguageTag(clean)
                val display = loc.getDisplayLanguage(Locale.ENGLISH)
                if (!display.isNullOrBlank() && !display.equals(clean, ignoreCase = true)) {
                    display.lowercase(Locale.ROOT)
                } else {
                    val l2 = Locale(clean).getDisplayLanguage(Locale.ENGLISH)
                    if (!l2.isNullOrBlank() && !l2.equals(clean, ignoreCase = true)) {
                        l2.lowercase(Locale.ROOT)
                    } else if (clean.all { it.isLetter() }) {
                        clean
                    } else null
                }
            } catch (_: Throwable) {
                if (clean.all { it.isLetter() }) clean else null
            }
        }
    }
}

private fun detectLanguageFromTitle(title: String): String? {
    val lower = title.lowercase(Locale.ROOT)
    val keywords = listOf(
        "hindi", "english", "tamil", "telugu", "malayalam", "kannada",
        "marathi", "bengali", "gujarati", "punjabi", "urdu", "bhojpuri",
        "spanish", "french", "german", "italian", "portuguese", "russian",
        "japanese", "korean", "chinese", "arabic", "turkish", "thai",
        "vietnamese", "indonesian"
    )
    for (kw in keywords) {
        if (Regex("""\b$kw\b""").containsMatchIn(lower)) {
            return kw
        }
    }
    return null
}

internal fun resolveTrackLanguageName(track: MkvTrackInfo, defaultFallback: String): String {
    mapLanguageToSimpleName(track.language)?.let { return it }
    if (track.name.isNotBlank()) {
        mapLanguageToSimpleName(track.name)?.let { return it }
        detectLanguageFromTitle(track.name)?.let { return it }
    }
    return defaultFallback
}

internal fun buildTrackOutputFileName(
    videoDisplayName: String,
    track: MkvTrackInfo,
    allTracksOfType: List<MkvTrackInfo>,
    extension: String,
    defaultFallbackLang: String
): String {
    val baseVideoName = sanitizeFileComponent(videoDisplayName.substringBeforeLast('.', videoDisplayName))
    val lang = resolveTrackLanguageName(track, defaultFallbackLang)

    val trackIndex = allTracksOfType.indexOfFirst { it.id == track.id && it.trackNumber == track.trackNumber }
        .takeIf { it >= 0 } ?: allTracksOfType.indexOf(track)

    val precedingSameLangCount = if (trackIndex >= 0) {
        allTracksOfType.take(trackIndex).count {
            resolveTrackLanguageName(it, defaultFallbackLang) == lang
        }
    } else 0

    val langWithNumber = if (precedingSameLangCount == 0) lang else "$lang${precedingSameLangCount + 1}"
    return "$baseVideoName-$langWithNumber.$extension"
}

fun getUniqueDestinationFile(targetDir: File, proposedFileName: String): File {
    val initialFile = File(targetDir, proposedFileName)
    if (!initialFile.exists()) {
        return initialFile
    }

    val ext = proposedFileName.substringAfterLast('.', "")
    val dotExt = if (ext.isNotEmpty()) ".$ext" else ""
    val nameWithoutExt = if (ext.isNotEmpty()) proposedFileName.substringBeforeLast('.') else proposedFileName

    val match = Regex("""^(.*)-([a-zA-Z]+)(\d*)$""").matchEntire(nameWithoutExt)
    if (match != null) {
        val prefix = match.groupValues[1]
        val lang = match.groupValues[2]
        val numStr = match.groupValues[3]
        val startNum = numStr.toIntOrNull() ?: 1
        var counter = if (startNum <= 1) 2 else startNum + 1
        while (true) {
            val candidateFile = File(targetDir, "$prefix-$lang$counter$dotExt")
            if (!candidateFile.exists()) {
                return candidateFile
            }
            counter++
        }
    } else {
        var counter = 2
        while (true) {
            val candidateFile = File(targetDir, "${nameWithoutExt}_$counter$dotExt")
            if (!candidateFile.exists()) {
                return candidateFile
            }
            counter++
        }
    }
}

internal fun buildInformationText(data: MediaInfoData?, active: MediaInfoSection, video: VideoItem): String {
    if (data == null) return "Media: ${video.displayName}"
    val meta = data.metadata
    val builder = StringBuilder()
    when (active) {
        MediaInfoSection.OVERVIEW -> {
            builder.appendLine("=== MEDIA INFORMATION (OVERVIEW) ===")
            builder.appendLine("File Name: ${meta.fileName}")
            builder.appendLine("File Size: ${formatBytes(meta.fileSizeBytes)}")
            builder.appendLine("Duration: ${formatMs(meta.durationMs)}")
            builder.appendLine("Video Tracks: ${data.tracks.count { it.trackType == MkvTrackType.VIDEO }}")
            builder.appendLine("Audio Tracks: ${data.tracks.count { it.trackType == MkvTrackType.AUDIO }}")
            builder.appendLine("Subtitle Tracks: ${data.tracks.count { it.trackType == MkvTrackType.SUBTITLE }}")
            builder.appendLine("Chapters: ${data.chapters.size}")
            builder.appendLine("Attachments: ${data.attachments.size}")
        }
        MediaInfoSection.VIDEO -> {
            builder.appendLine("=== VIDEO TRACKS ===")
            data.tracks.filter { it.trackType == MkvTrackType.VIDEO }.forEach {
                builder.appendLine("Track #${it.trackNumber}: ${it.name} | Codec: ${it.displayCodec} | Resolution: ${it.displayResolution} | FPS: ${it.frameRate} | Lang: ${it.language}")
            }
        }
        MediaInfoSection.AUDIO -> {
            builder.appendLine("=== AUDIO TRACKS ===")
            data.tracks.filter { it.trackType == MkvTrackType.AUDIO }.forEach {
                builder.appendLine("Track #${it.trackNumber}: ${it.name} | Codec: ${it.displayCodec} | Channels: ${channelLabel(it.channels)} | Rate: ${it.sampleRate} Hz | Lang: ${it.language}")
            }
        }
        MediaInfoSection.SUBTITLE -> {
            builder.appendLine("=== SUBTITLE TRACKS ===")
            data.tracks.filter { it.trackType == MkvTrackType.SUBTITLE }.forEach {
                builder.appendLine("Track #${it.trackNumber}: ${it.name} | Format: ${subtitleFormatLabel(it)} | Lang: ${it.language} | Default: ${it.isDefault}")
            }
        }
        MediaInfoSection.CHAPTERS -> {
            builder.appendLine(buildChapterText(data.chapters, "ogm"))
        }
        MediaInfoSection.ATTACHMENTS -> {
            builder.appendLine("=== ATTACHMENTS ===")
            data.attachments.forEach {
                builder.appendLine("${it.fileName} (${it.formattedSize}) - ${it.mimeType}")
            }
        }
    }
    return builder.toString()
}

internal fun buildChapterText(chapters: List<MkvChapterInfo>, format: String): String {
    val sb = StringBuilder()
    if (format == "xml") {
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<Chapters>")
        sb.appendLine("  <EditionEntry>")
        chapters.forEachIndexed { index, ch ->
            sb.appendLine("    <ChapterAtom>")
            sb.appendLine("      <ChapterTimeStart>${ch.formattedStartTime}.000</ChapterTimeStart>")
            if (ch.endTimeMs > 0) sb.appendLine("      <ChapterTimeEnd>${ch.formattedEndTime}.000</ChapterTimeEnd>")
            sb.appendLine("      <ChapterDisplay>")
            sb.appendLine("        <ChapterString>${ch.title}</ChapterString>")
            sb.appendLine("        <ChapterLanguage>${ch.language.ifBlank { "eng" }}</ChapterLanguage>")
            sb.appendLine("      </ChapterDisplay>")
            sb.appendLine("    </ChapterAtom>")
        }
        sb.appendLine("  </EditionEntry>")
        sb.appendLine("</Chapters>")
    } else {
        chapters.forEachIndexed { index, ch ->
            val num = String.format(Locale.US, "%02d", index + 1)
            sb.appendLine("CHAPTER${num}=${ch.formattedStartTime}.000")
            sb.appendLine("CHAPTER${num}NAME=${ch.title}")
        }
    }
    return sb.toString()
}
