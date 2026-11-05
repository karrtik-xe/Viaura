package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DeleteConfirmSheet
import com.example.ui.components.DirectoryPickerAction
import com.example.ui.components.FloatingDirectoryPickerSheet
import com.example.ui.components.LayoutMode
import com.example.ui.components.LinkThumbnail
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.StyledIcon
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VideoItemGridCard
import com.example.player.AudioPlaybackManager
import com.example.ui.components.VisibleFields
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import com.example.ui.state.UiState
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadFormat
import com.example.util.DownloadItem
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.DownloadStatus
import com.example.util.DownloadStore
import com.example.util.MediaMetadataExtractor
import com.example.util.VideoFileManager
import com.example.player.AudioMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

internal sealed interface DownloadsContentEntry {
    val stableKey: String

    data class Section(val title: String, override val stableKey: String) : DownloadsContentEntry
    data class Media(val item: DownloadItem) : DownloadsContentEntry {
        override val stableKey: String = "download_${item.id}"
    }
    /** A series folder (all its episodes, also while they are still downloading). */
    data class Folder(val name: String, val total: Int, val active: Int, val thumbnailUrl: String?) : DownloadsContentEntry {
        override val stableKey: String = "folder_$name"
    }
    /** Back row at the top of an opened series folder. */
    data class FolderHeader(val name: String) : DownloadsContentEntry {
        override val stableKey: String = "folder_header"
    }
}

@Composable
internal fun DownloadsFolderCard(entry: DownloadsContentEntry.Folder, isDark: Boolean, onClick: () -> Unit) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isDark) DarkGlassSurface else LightGlassSurface)
            .border(1.dp, if (isDark) DarkGlassBorder else LightGlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(AccentSkyBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(imageVector = Icons.Outlined.Folder, contentDescription = null, tint = AccentSkyBlue, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = (if (entry.total == 1) "1 episode" else "${entry.total} episodes") +
                    (if (entry.active > 0) " · ${entry.active} downloading" else ""),
                fontSize = 12.sp,
                color = secondary,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun DownloadsFolderHeaderRow(name: String, isDark: Boolean, onBack: () -> Unit) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onBack)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyledIcon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal fun downloadAudioId(id: String): Long =
    -(kotlin.math.abs(("download-audio:" + id).hashCode().toLong())) - 1L

internal fun downloadToAudioTrack(item: DownloadItem, durationMs: Long): AudioTrackItem {
    val file = item.filePath?.let { File(it) }
    val uri = item.fileUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: file?.takeIf { it.exists() }?.let { Uri.fromFile(it) }
        ?: Uri.EMPTY
    val displayPath = item.filePath?.takeIf { it.isNotBlank() } ?: uri.toString()
    val ext = file?.extension?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        ?: item.detail.orEmpty().substringAfterLast('·', "").trim().substringBefore(' ').uppercase(Locale.ROOT)
    return AudioTrackItem(
        id = downloadAudioId(item.id),
        uri = uri,
        title = item.title.ifBlank { file?.nameWithoutExtension ?: "Downloaded audio" },
        artist = item.subtitle.orEmpty(),
        album = "Downloads",
        durationMs = durationMs,
        path = displayPath,
        sizeBytes = file?.takeIf { it.exists() }?.length() ?: item.totalBytes,
        format = ext.ifBlank { "AUDIO" },
        dateModified = (file?.takeIf { it.exists() }?.lastModified() ?: item.createdAt) / 1000L,
        thumbnailUrl = item.thumbnailUrl
    )
}

/** True when a downloaded song has lyrics: a sibling .lrc file, or lyrics embedded in its tags. */
internal fun downloadHasLyrics(file: File): Boolean = try {
    val lrc = File(file.parentFile ?: file, file.nameWithoutExtension + ".lrc")
    if (lrc.isFile && lrc.length() > 0L) {
        true
    } else {
        val embedded = when (file.extension.lowercase(Locale.ROOT)) {
            "mp3" -> AudioMetadataExtractor.extractId3Lyrics(file)
            "m4a", "mp4", "aac" -> AudioMetadataExtractor.extractMp4Lyrics(file)
            "flac", "ogg", "opus" -> AudioMetadataExtractor.extractVorbisLyrics(file)
            else -> null
        }
        !embedded.isNullOrBlank()
    }
} catch (_: Throwable) {
    false
}

@Composable
internal fun DownloadsSectionHeader(title: String, isDark: Boolean) {
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isDark) DarkTextPrimary else LightTextPrimary,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 1.dp)
    )
}
