package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.OnlineTrackResult

/** What the actions sheet of a song can do; every entry is optional (null = not shown). */
internal class OmSheetActions(
    val onPlay: () -> Unit,
    val onPlayNext: () -> Unit,
    val onPlayLast: () -> Unit,
    val onDownload: () -> Unit,
    val onSaveToPlaylist: () -> Unit,
    val onWatchLater: () -> Unit,
    val onShare: () -> Unit,
    val onOpenArtist: (() -> Unit)?,
    val onNotInterested: () -> Unit
)

/**
 * The actions of a song of the music feed, sliding up from the bottom in the same glass sheet the app's other
 * sheets use: touch the three dots (or hold the song) -> Play next, Add to queue, Download, Add to playlist ...
 */
@Composable
internal fun OmTrackActionSheet(
    track: OnlineTrackResult,
    isDark: Boolean,
    glassBlurTransparency: Float,
    isSavedForLater: Boolean,
    actions: OmSheetActions,
    onDismiss: () -> Unit
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    PlaylistBottomSheet(isDark, glassBlurTransparency, onDismiss) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            SheetCover(track.thumbnailUrl, isDark)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = primary, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (track.artist.isNotBlank()) {
                    Text(track.artist, color = secondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        fun go(block: () -> Unit): () -> Unit = { block(); onDismiss() }
        SheetRow("Play", primary, go(actions.onPlay))
        SheetRow("Play next", primary, go(actions.onPlayNext))
        SheetRow("Add to queue", primary, go(actions.onPlayLast))
        SheetRow("Download", primary, go(actions.onDownload))
        SheetRow("Add to playlist", primary, go(actions.onSaveToPlaylist))
        SheetRow(if (isSavedForLater) "Remove from Watch later" else "Save to Watch later", primary, go(actions.onWatchLater))
        SheetRow("Share", primary, go(actions.onShare))
        actions.onOpenArtist?.let { open -> SheetRow("Go to artist", primary, go(open)) }
        SheetRow("Not interested", Color(0xFFE02424), go(actions.onNotInterested))
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SheetRow(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 6.dp, vertical = 14.dp)
    ) {
        Text(label, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SheetCover(url: String?, isDark: Boolean) {
    val ctx = LocalContext.current
    var bmp by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) bmp = loadRemoteAudioThumbnail(url, targetSizePx = 300, context = ctx)
    }
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(if (isDark) Color(0xFF1B1B1B) else Color(0xFFE2E8F0))) {
        bmp?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(), contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(56.dp)
            )
        }
    }
}
