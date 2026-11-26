package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.util.CustomPlaylist
import com.example.util.PlaylistManager
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.OnlineMediaLibrary
import com.example.util.OnlineTrackResult
import com.example.util.OnlineVideoService

/**
 * "Playlists" tab of Online videos (beside Live): the user's own playlists of online videos. A playlist is created
 * here (or from the swipe toolbar of any video: Save to playlist) and plays as a queue when a video is tapped.
 */
@Composable
internal fun OnlineVideoPlaylistsTab(
    isDark: Boolean,
    onPlay: (first: OnlineTrackResult, queue: List<OnlineTrackResult>) -> Unit
) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val surface = if (isDark) DarkGlassSurface else LightGlassSurface
    val shape = RoundedCornerShape(16.dp)

    var version by remember { mutableStateOf(0) }
    var openId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<CustomPlaylist?>(null) }

    val playlists = remember(version) {
        PlaylistManager.getPlaylists(context).filter { pl ->
            !pl.isAudio && (pl.isOnline || pl.videoPaths.any { it.startsWith(OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX) })
        }
    }
    val open = playlists.firstOrNull { it.id == openId }
    if (openId != null && open == null) openId = null
    BackHandler(enabled = open != null) { openId = null }

    fun itemsOf(pl: CustomPlaylist): List<OnlineTrackResult> = pl.videoPaths
        .filter { it.startsWith(OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX) }
        .map { path ->
            val url = OnlineVideoService.toWatchUrl(path)
            OnlineMediaLibrary.getMeta(OnlineMediaLibrary.KIND_VIDEO, url, context)
                ?: OnlineTrackResult(videoUrl = url, title = url, artist = "", durationSec = 0L, thumbnailUrl = null)
        }

    Box(modifier = Modifier.fillMaxSize()) {
        if (open == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "pl_new") {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(shape).background(surface.copy(alpha = surface.alpha * 0.6f))
                            .border(1.5.dp, AccentGradient, shape).clickable { newName = ""; showCreate = true }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("+  New playlist", color = primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (playlists.isEmpty()) {
                    item(key = "pl_empty") {
                        Text(
                            "No playlists yet. Create one here, then add any online video to it from its swipe toolbar (Save to playlist).",
                            color = secondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }
                items(playlists, key = { "pl_${it.id}" }) { pl ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(shape).background(surface.copy(alpha = surface.alpha * 0.6f))
                            .clickable { openId = pl.id }.padding(14.dp)
                    ) {
                        Text(pl.name, color = primary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(pl.itemCountText, color = secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        } else {
            val list = itemsOf(open)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "pl_head") {
                    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Text(open.name, color = primary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { openId = null }) { Text("Back") }
                            TextButton(onClick = { if (list.isNotEmpty()) onPlay(list.first(), list) }, enabled = list.isNotEmpty()) { Text("Play all") }
                            TextButton(onClick = { confirmDelete = open }) { Text("Delete", color = Color(0xFFE02424)) }
                        }
                    }
                }
                if (list.isEmpty()) {
                    item(key = "pl_items_empty") {
                        Text("This playlist is empty.", color = secondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
                    }
                }
                items(list, key = { "pli_${it.videoUrl}" }) { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(shape).background(surface.copy(alpha = surface.alpha * 0.6f))
                            .clickable { onPlay(r, list) }.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, color = primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (r.artist.isNotBlank()) Text(r.artist, color = secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = {
                            PlaylistManager.removeVideoFromPlaylist(context, open.id, "${OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX}${r.videoUrl}")
                            version++
                        }) { Text("Remove", color = secondary) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, placeholder = { Text("Playlist name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    PlaylistManager.createPlaylist(context, newName, "VIDEO", isOnline = true)
                    showCreate = false
                    version++
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
    confirmDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete playlist?") },
            text = { Text("\"${pl.name}\" will be deleted. The videos themselves are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    PlaylistManager.deletePlaylist(context, pl.id)
                    confirmDelete = null
                    openId = null
                    version++
                }) { Text("Delete", color = Color(0xFFE02424)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
}
