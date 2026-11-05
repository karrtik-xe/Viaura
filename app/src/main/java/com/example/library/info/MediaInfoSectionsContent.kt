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
internal fun MediaInfoContent(
    section: MediaInfoSection,
    data: MediaInfoData,
    video: VideoItem,
    searchQuery: String,
    mediaThumbnail: android.graphics.Bitmap?,
    isDark: Boolean,
    primary: Color,
    secondary: Color,
    surface: Color,
    border: Color,
    chapterFormat: String,
    onChapterFormatChange: (String) -> Unit,
    expandedAttachments: Boolean,
    onExpandedAttachmentsChange: (Boolean) -> Unit,
    onExtractAllFonts: () -> Unit,
    onExtract: (String, suspend (Uri) -> Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onProgress: (Float, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val q = searchQuery.trim().lowercase(Locale.ROOT)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (section) {
            MediaInfoSection.OVERVIEW -> {
                item {
                    var bitmap by remember(video.path) { mutableStateOf<android.graphics.Bitmap?>(null) }
                    LaunchedEffect(video.path) {
                        bitmap = loadVideoThumbnail(
                            context, video.id, video.uri, video.path,
                            ThumbnailStrategy.SMART, ThumbnailQuality.MEDIUM, false
                        )
                    }
                    OverviewCard(data, video, bitmap, q, isDark, primary, secondary, surface, border, onCopy)
                }
            }
            MediaInfoSection.VIDEO -> {
                val allTracks = data.tracks.filter { it.trackType == MkvTrackType.VIDEO }
                val tracks = if (q.isBlank()) allTracks else allTracks.filter { track ->
                    track.name.contains(q, true) ||
                    track.language.contains(q, true) ||
                    track.displayCodec.contains(q, true) ||
                    track.codecId.contains(q, true) ||
                    track.displayResolution.contains(q, true) ||
                    "${track.pixelWidth}".contains(q) ||
                    "${track.pixelHeight}".contains(q) ||
                    track.hdrFormat.contains(q, true) ||
                    (q.contains("1080") && (track.pixelHeight >= 1080 || track.displayResolution.contains("1080"))) ||
                    (q.contains("720") && (track.pixelHeight == 720 || track.displayResolution.contains("720"))) ||
                    (q.contains("4k") && (track.pixelWidth >= 3840 || track.pixelHeight >= 2160)) ||
                    (q.contains("2k") && track.pixelWidth >= 2000) ||
                    (track.isDefault && "default".contains(q)) ||
                    (track.isForced && "forced".contains(q))
                }

                if (tracks.isEmpty()) {
                    item {
                        EmptyInfo(
                            if (q.isBlank()) "No video tracks detected." else "No video tracks matching \"$searchQuery\".",
                            primary, secondary, surface, border
                        )
                    }
                }
                val allVideoTracks = data.tracks.filter { it.trackType == MkvTrackType.VIDEO }
                items(tracks, key = { "v-${it.id}-${it.trackNumber}" }) { track ->
                    TrackCard(
                        track = track, type = MkvTrackType.VIDEO, thumbnail = mediaThumbnail, isDark = isDark, primary = primary,
                        secondary = secondary, surface = surface, border = border,
                        fileSizeBytes = data.metadata.fileSizeBytes,
                        onExtract = {
                            val ext = videoExtension(track)
                            val baseVideoName = sanitizeFileComponent(video.displayName.substringBeforeLast('.', video.displayName))
                            val trackIndex = allVideoTracks.indexOfFirst { it.id == track.id && it.trackNumber == track.trackNumber }
                                .takeIf { it >= 0 } ?: allVideoTracks.indexOf(track)
                            val videoSuffix = if (trackIndex <= 0) "" else "${trackIndex + 1}"
                            val outFileName = "$baseVideoName-video$videoSuffix.$ext"
                            onExtract(outFileName) { uri ->
                                onProgress(.12f, "Scanning video track...", outFileName)
                                MkvManagerEngine.extractSingleTrack(
                                    context, video.uri, data.metadata, track, uri
                                ) { f, msg -> onProgress(f, msg, outFileName) }
                            }
                        }
                    )
                }
            }
            MediaInfoSection.AUDIO -> {
                val allTracks = data.tracks.filter { it.trackType == MkvTrackType.AUDIO }
                val tracks = if (q.isBlank()) allTracks else allTracks.filter { track ->
                    track.name.contains(q, true) ||
                    track.language.contains(q, true) ||
                    track.displayCodec.contains(q, true) ||
                    track.codecId.contains(q, true) ||
                    channelLabel(track.channels).contains(q, true) ||
                    "${track.channels}".contains(q) ||
                    "${track.sampleRate}".contains(q) ||
                    (q.contains("5.1") && track.channels == 6) ||
                    (q.contains("7.1") && track.channels == 8) ||
                    (q.contains("stereo") && track.channels == 2) ||
                    (q.contains("mono") && track.channels == 1) ||
                    (q.contains("hindi") && (track.language.equals("hi", true) || track.language.contains("hin", true))) ||
                    (q.contains("english") && (track.language.equals("en", true) || track.language.contains("eng", true))) ||
                    (q.contains("japanese") && (track.language.equals("ja", true) || track.language.contains("jpn", true))) ||
                    (track.isDefault && "default".contains(q)) ||
                    (track.isForced && "forced".contains(q))
                }

                if (tracks.isEmpty()) {
                    item {
                        EmptyInfo(
                            if (q.isBlank()) "No audio tracks detected." else "No audio tracks matching \"$searchQuery\".",
                            primary, secondary, surface, border
                        )
                    }
                }
                items(tracks, key = { "a-${it.id}-${it.trackNumber}" }) { track ->
                    TrackCard(
                        track = track, type = MkvTrackType.AUDIO, isDark = isDark, primary = primary,
                        secondary = secondary, surface = surface, border = border,
                        fileSizeBytes = data.metadata.fileSizeBytes,
                        onExtract = {
                            val ext = audioExtension(track)
                            val outFileName = buildTrackOutputFileName(
                                videoDisplayName = video.displayName,
                                track = track,
                                allTracksOfType = allTracks,
                                extension = ext,
                                defaultFallbackLang = "audio"
                            )
                            onExtract(outFileName) { uri ->
                                onProgress(.12f, "Scanning audio track...", outFileName)
                                MkvManagerEngine.extractSingleTrack(
                                    context, video.uri, data.metadata, track, uri
                                ) { f, msg -> onProgress(f, msg, outFileName) }
                            }
                        }
                    )
                }
            }
            MediaInfoSection.SUBTITLE -> {
                val allTracks = data.tracks.filter { it.trackType == MkvTrackType.SUBTITLE }
                val tracks = if (q.isBlank()) allTracks else allTracks.filter { track ->
                    track.name.contains(q, true) ||
                    track.language.contains(q, true) ||
                    subtitleFormatLabel(track).contains(q, true) ||
                    track.codecId.contains(q, true) ||
                    (q.contains("hindi") && (track.language.equals("hi", true) || track.language.contains("hin", true))) ||
                    (q.contains("english") && (track.language.equals("en", true) || track.language.contains("eng", true))) ||
                    (q.contains("japanese") && (track.language.equals("ja", true) || track.language.contains("jpn", true))) ||
                    (q.contains("ass") && (track.codecId.contains("ASS", true) || track.codecId.contains("SSA", true))) ||
                    (q.contains("srt") && (track.codecId.contains("UTF8", true) || track.codecId.contains("SRT", true))) ||
                    (q.contains("pgs") && track.codecId.contains("PGS", true)) ||
                    (q.contains("vtt") && track.codecId.contains("VTT", true)) ||
                    (track.isDefault && "default".contains(q)) ||
                    (track.isForced && "forced".contains(q)) ||
                    (track.isHearingImpaired && "hearing impaired".contains(q))
                }

                if (tracks.isEmpty()) {
                    item {
                        EmptyInfo(
                            if (q.isBlank()) "No subtitle tracks detected." else "No subtitle tracks matching \"$searchQuery\".",
                            primary, secondary, surface, border
                        )
                    }
                }
                items(tracks, key = { "s-${it.id}-${it.trackNumber}" }) { track ->
                    SubtitleTrackCard(
                        track = track, isDark = isDark, primary = primary, secondary = secondary,
                        surface = surface, border = border,
                        fileSizeBytes = data.metadata.fileSizeBytes,
                        onCopy = {
                            scope.launch {
                                val content = MkvManagerEngine.extractSubtitleTrackToString(context, video.uri, track)
                                onCopy(content)
                            }
                        },
                        onExtract = {
                            val ext = subtitleExtension(track)
                            val outFileName = buildTrackOutputFileName(
                                videoDisplayName = video.displayName,
                                track = track,
                                allTracksOfType = allTracks,
                                extension = ext,
                                defaultFallbackLang = "subtitle"
                            )
                            onExtract(outFileName) { uri ->
                                onProgress(.10f, "Reading subtitle cues...", outFileName)
                                MkvManagerEngine.extractSingleTrack(
                                    context, video.uri, data.metadata, track, uri
                                ) { f, msg -> onProgress(f, msg, outFileName) }
                            }
                        }
                    )
                }
            }
            MediaInfoSection.CHAPTERS -> {
                val allChapters = data.chapters
                val filteredChapters = if (q.isBlank()) allChapters else allChapters.filterIndexed { index, ch ->
                    ch.title.contains(q, true) ||
                    ch.formattedStartTime.contains(q, true) ||
                    ch.formattedEndTime.contains(q, true) ||
                    ch.language.contains(q, true) ||
                    "chapter ${index + 1}".contains(q, true)
                }

                item {
                    ChapterInfoCard(
                        chapters = filteredChapters,
                        totalChapterCount = allChapters.size,
                        searchQuery = searchQuery,
                        format = chapterFormat,
                        onFormatChange = onChapterFormatChange,
                        onCopy = {
                            onCopy(buildChapterText(filteredChapters, chapterFormat))
                        },
                        onExtract = {
                            val extension = if (chapterFormat == "xml") "xml" else "txt"
                            val engineFormat = if (chapterFormat == "xml") "xml" else "ogm"
                            val baseVideoName = sanitizeFileComponent(video.displayName.substringBeforeLast('.', video.displayName))
                            val safe = "$baseVideoName-chapters.$extension"
                            onExtract(safe) { uri ->
                                onProgress(.25f, "Writing chapter file...", safe)
                                MkvManagerEngine.exportChapters(context, filteredChapters, engineFormat, uri)
                            }
                        },
                        isDark = isDark, primary = primary, secondary = secondary,
                        surface = surface, border = border
                    )
                }
            }
            MediaInfoSection.ATTACHMENTS -> {
                val allAttachments = data.attachments
                val attachments = if (q.isBlank()) allAttachments else allAttachments.filter { att ->
                    att.fileName.contains(q, true) ||
                    att.mimeType.contains(q, true) ||
                    att.description.contains(q, true) ||
                    (att.isFont && (q.contains("font") || q.contains("ttf") || q.contains("otf")))
                }

                if (attachments.isEmpty()) {
                    item {
                        EmptyInfo(
                            if (q.isBlank()) "No attachments detected." else "No attachments matching \"$searchQuery\".",
                            primary, secondary, surface, border
                        )
                    }
                } else {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${attachments.size} attachments • ${attachments.count { it.isFont }} fonts",
                                color = primary, fontWeight = FontWeight.Bold, fontSize = 15.sp
                            )
                            Text(
                                "Extract all fonts",
                                color = AccentSkyBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = attachments.any { it.isFont }) {
                                    onExtractAllFonts()
                                }
                            )
                        }
                    }
                    items(attachments, key = { "att-${it.id}-${it.uid}" }) { att ->
                        AttachmentCard(
                            attachment = att, isDark = isDark, primary = primary, secondary = secondary,
                            surface = surface, border = border,
                            onExtract = {
                                onExtract(att.fileName.ifBlank { "attachment_${att.id}" }) { uri ->
                                    onProgress(.25f, "Saving attachment...", att.fileName)
                                    MkvManagerEngine.extractAttachment(context, att, uri)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
