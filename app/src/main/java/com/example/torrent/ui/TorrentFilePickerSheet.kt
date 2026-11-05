/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Torrent file picker: metadata first, the file list is shown, the user picks a file and either adds it to the downloads or
 * plays it while it downloads (TorrentStreamer: piece-prioritised streaming through a loopback server). The screen itself is
 * Viaura's own sheet.
 */

package com.example.ui.components

import com.example.ui.theme.Neutral

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.torrent.TorrentCatalog
import com.example.torrent.TorrentFileItem
import com.example.torrent.TorrentPickerRequest
import com.example.torrent.TorrentPickerState
import com.example.torrent.TorrentCatalogs
import com.example.torrent.TorrentStreamState
import com.example.torrent.TorrentStreamer
import com.example.ui.screens.VideoItem
import com.example.torrent.formatTorrentBytes
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private sealed interface PickerState {
    data object Loading : PickerState
    data class Ready(val catalog: TorrentCatalog) : PickerState
    data class Failed(val message: String) : PickerState
}

/**
 * Global torrent picker, hosted once in MainActivity. Opened through [TorrentPickerState] (Network tab, magnet / .torrent
 * intent). [onPlayStream] opens the video player with the local stream of the chosen file.
 */
@Composable
fun TorrentPickerHost(
    isDark: Boolean,
    glassBlurTransparency: Float,
    onPlayStream: (VideoItem) -> Unit = {}
) {
    val request = TorrentPickerState.request ?: return
    TorrentPickerSheet(request, isDark, glassBlurTransparency, onPlayStream)
}

@Composable
private fun TorrentPickerSheet(
    request: TorrentPickerRequest,
    isDark: Boolean,
    glassBlurTransparency: Float,
    onPlayStream: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val engine = remember { TorrentCatalogs.engine(context) }
    val enginePhase by engine.phase.collectAsState()
    var state by remember(request) { mutableStateOf<PickerState>(PickerState.Loading) }
    var attempt by remember(request) { mutableStateOf(0) }
    var preparationId by remember(request) { mutableStateOf<String?>(null) }
    // the file that is being buffered for playback (null = the list is shown)
    var startingFile by remember(request) { mutableStateOf<TorrentFileItem?>(null) }
    var startError by remember(request) { mutableStateOf<String?>(null) }
    var startJob by remember(request) { mutableStateOf<Job?>(null) }
    val streamState by TorrentStreamer.state.collectAsState()
    val scope = rememberCoroutineScope()

    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val fieldBg = if (isDark) Neutral.D1E293B else Neutral.F1F5F9

    fun launchDownload(catalog: TorrentCatalog, file: TorrentFileItem) {
        engine.discardPreparation(catalog.preparationId)
        val message = try {
            DownloadManager.enqueueTorrent(context, catalog.source, file.index, file.name, file.size, catalog.torrentName)
        } catch (t: Throwable) {
            "Could not start the download"
        }
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        TorrentPickerState.close()
    }

    fun launchStream(catalog: TorrentCatalog, file: TorrentFileItem) {
        if (startingFile != null) return
        startError = null
        startingFile = file
        startJob = scope.launch {
            try {
                val start = TorrentStreamer.startFromCatalog(context, catalog.preparationId, file.index)
                val item = VideoItem(
                    id = -(kotlin.math.abs(("torrent:" + catalog.infoHash + ":" + file.index).hashCode().toLong())) - 1L,
                    uri = android.net.Uri.parse(start.url),
                    displayName = file.name,
                    path = start.url,
                    sizeBytes = start.fileSize,
                    durationMs = 0L,
                    dateModified = System.currentTimeMillis() / 1000L,
                    isOnline = true,
                    channelName = catalog.torrentName,
                    isDirectStream = true
                )
                onPlayStream(item)
                TorrentPickerState.close()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                startError = t.message?.take(240) ?: "Couldn't start this torrent."
                startingFile = null
            }
        }
    }

    fun cancelStart() {
        startJob?.cancel()
        startJob = null
        startingFile = null
        TorrentStreamer.stop()
    }

    LaunchedEffect(request, attempt) {
        state = PickerState.Loading
        try {
            val catalog = engine.prepareTorrent(request.source)
            preparationId = catalog.preparationId
            // always the list: every file can be played or downloaded (a single file is no longer downloaded unasked)
            state = PickerState.Ready(catalog)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            state = PickerState.Failed(t.message?.take(240) ?: "Couldn't open this torrent.")
        }
    }

    DisposableEffect(request) {
        onDispose {
            preparationId?.let(engine::discardPreparation)
        }
    }

    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss = {
        if (startingFile != null) cancelStart()
        TorrentPickerState.close()
    }) {
        Text(
            text = "Torrent",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText,
            modifier = Modifier.padding(top = 4.dp)
        )
        when (val s = state) {
            PickerState.Loading -> {
                val phase = enginePhase
                Row(
                    modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = phase, fontSize = 13.sp, color = secondaryText)
                }
            }
            is PickerState.Failed -> {
                Text(
                    text = s.message,
                    fontSize = 13.sp,
                    color = secondaryText,
                    modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AccentGradient)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { attempt++ }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = "Retry", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(modifier = Modifier.size(10.dp))
            }
            is PickerState.Ready -> if (startingFile != null) {
                val file = startingFile
                val preparing = streamState as? TorrentStreamState.Preparing
                Text(
                    text = file?.name.orEmpty(),
                    fontSize = 13.sp,
                    color = secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    val detail = if (preparing == null) "Starting Torrent Engine..." else buildString {
                        append(preparing.phase)
                        if (preparing.bufferPercent > 0) append(" ").append(preparing.bufferPercent).append("%")
                        if (preparing.peers > 0) append(" · ").append(preparing.peers).append(" peers")
                        if (preparing.speedBps > 0) append(" · ").append(formatTorrentBytes(preparing.speedBps)).append("/s")
                    }
                    Text(text = detail, fontSize = 13.sp, color = secondaryText)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(fieldBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { cancelStart() }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = "Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = primaryText)
                }
                Spacer(modifier = Modifier.size(10.dp))
            } else {
                startError?.let { message ->
                    Text(text = message, fontSize = 13.sp, color = secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    text = s.catalog.torrentName,
                    fontSize = 13.sp,
                    color = secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${s.catalog.files.size} files · play or download",
                    fontSize = 12.sp,
                    color = secondaryText,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                Box(modifier = Modifier.heightIn(max = 420.dp)) {
                    LazyColumn {
                        items(s.catalog.files, key = { it.index }) { file ->
                            Row(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(fieldBg)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { launchDownload(s.catalog, file) }
                                    )
                                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = primaryText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = formatTorrentBytes(file.size), fontSize = 12.sp, color = secondaryText)
                                }
                                if (TorrentStreamer.isPlayableName(file.name)) {
                                    TorrentRowButton(R.drawable.viaura_play_circle, "Play", AccentSkyBlue) {
                                        launchStream(s.catalog, file)
                                    }
                                }
                                TorrentRowButton(R.drawable.viaura_download_minimalistic, "Download", AccentSkyBlue) {
                                    launchDownload(s.catalog, file)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorrentRowButton(drawableRes: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        StyledIcon(
            drawableRes = drawableRes,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
