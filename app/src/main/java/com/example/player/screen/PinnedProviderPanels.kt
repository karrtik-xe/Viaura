package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.HighResImage
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.media.MediaLibraryStore
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaSavedVideo
import com.example.util.media.PluginPlaybackSessions

/**
 * Pinned video layout for media that comes from a provider (plugin). Such a video is "online" and a direct stream, but it is NOT a
 * YouTube video: it has no likes, no subscribe button and no comments. Kept in its own file on purpose: the player screen is huge
 * and extra code there upsets R8 (VerifyError).
 */
internal fun isProviderVideo(v: VideoItem?): Boolean = v != null && v.isOnline && v.isDirectStream

/** The provider's name (shown in the header instead of "Online"). */
internal fun providerNameOf(v: VideoItem?): String =
    PluginPlaybackSessions.get(v?.path.orEmpty())?.plugin?.name?.takeIf { it.isNotBlank() }
        ?: v?.channelName?.takeIf { it.isNotBlank() }
        ?: "Provider"

/** Save / Download / Playlist / Share / Link copy of a provider episode or movie (no Like, Dislike or Subscribe). */
@Composable
internal fun ProviderActionChips(video: VideoItem, isDark: Boolean, glass: Float) {
    val context = LocalContext.current
    val session = remember(video.path) { PluginPlaybackSessions.get(video.path) }
    val plugin = session?.plugin
    val link = session?.episodeLink.orEmpty()
    val type = session?.episodeType.orEmpty().ifBlank { "movie" }
    val hasProviderLink = plugin != null && link.isNotBlank()
    var saved by remember(video.path) {
        mutableStateOf(hasProviderLink && MediaLibraryStore.isSaved(context, plugin!!.value, link))
    }
    var showDownload by remember(video.path) { mutableStateOf(false) }
    val streamUrl = video.path.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) }

    if (hasProviderLink && plugin != null) {
        ChannelActionChip(
            iconRes = if (saved) com.example.R.drawable.viaura_saved_bookmark else com.example.R.drawable.viaura_bookmark,
            label = "Save", isDark = isDark, active = saved
        ) {
            saved = MediaLibraryStore.toggleWatchLater(
                context,
                MediaSavedVideo(
                    pluginId = plugin.value,
                    title = video.displayName,
                    pageTitle = video.displayName,
                    poster = video.thumbnailUrl.orEmpty(),
                    link = link,
                    type = type
                )
            )
            Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
        }
        ChannelActionChip(com.example.R.drawable.viaura_download_minimalistic, "Download", isDark) { showDownload = true }
        ChannelActionChip(com.example.R.drawable.viaura_playlist_video, "Playlist", isDark) {
            MediaPlayback.addToPluginPlaylist(context, plugin, video)
            Toast.makeText(context, "Added to the \"${plugin.name}\" playlist", Toast.LENGTH_SHORT).show()
        }
    }
    if (streamUrl != null) {
        ChannelActionChip(com.example.R.drawable.viaura_share, "Share", isDark) {
            com.example.ui.components.shareOnlineLink(context, video.displayName, streamUrl)
        }
        ChannelActionChip(com.example.R.drawable.viaura_copy, "Link copy", isDark) {
            try {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("link", streamUrl))
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
        }
    }

    if (showDownload && hasProviderLink && plugin != null) {
        com.example.ui.components.MediaDownloadSheet(
            isDark = isDark,
            glassBlurTransparency = glass,
            plugin = plugin,
            title = video.displayName,
            poster = video.thumbnailUrl.orEmpty(),
            link = link,
            type = type,
            onDismiss = { showDownload = false }
        )
    }
}

/** One episode of the series that is playing: poster, name, "Now playing" / episode number. Tap = play it. */
@Composable
internal fun ProviderEpisodeRow(item: VideoItem, number: Int, isDark: Boolean, onClick: () -> Unit) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(116.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (!item.thumbnailUrl.isNullOrBlank()) {
                HighResImage(url = item.thumbnailUrl, modifier = Modifier.fillMaxSize(), maxSidePx = 360)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = item.displayName,
                color = primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(text = "Episode $number", color = secondary, fontSize = 12.sp, maxLines = 1)
        }
    }
}
