package com.example.ui.screens

import com.example.ui.components.cardSurface
import com.example.ui.theme.Neutral

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.player.AudioPlaybackManager
import com.example.ui.components.LinkThumbnail
import com.example.ui.components.PlaylistFilterTabs
import com.example.ui.components.StyledIcon
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.torrent.TorrentPickerState
import com.example.util.NetworkStreamResolver
import com.example.util.RecentStreamEntry
import com.example.util.RecentStreamStore
import com.example.util.RecentThumbnails
import com.example.util.media.MediaPluginStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun RecentStreamRow(
    entry: RecentStreamEntry,
    primaryText: Color,
    secondaryText: Color,
    surfaceColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDownload: () -> Unit
) {
    val subtitle = entry.subtitle
        ?: entry.url.substringAfter("://").substringBefore('/')
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardSurface(shape = RoundedCornerShape(20.dp)) {
                this
                    .clip(RoundedCornerShape(20.dp))
                    .background(com.example.ui.components.cardTone(surfaceColor))
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinkThumbnail(
            thumbnail = entry.thumbnail,
            modifier = Modifier.size(width = 64.dp, height = 40.dp).clip(RoundedCornerShape(10.dp)),
            fallback = {
                StyledIcon(
                    drawableRes = R.drawable.viaura_link,
                    contentDescription = null,
                    tint = secondaryText.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        RecentActionButton(R.drawable.viaura_trash_bin_2, "Remove from recent", secondaryText.copy(alpha = 0.9f), onRemove)
        RecentActionButton(R.drawable.viaura_play_solid, "Play", AccentSkyBlue, onClick)
        RecentActionButton(R.drawable.viaura_download_minimalistic, "Download", AccentSkyBlue, onDownload)
    }
}

@Composable
private fun RecentActionButton(
    drawableRes: Int,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
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
