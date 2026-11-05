package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DownloadGlassSheet
import com.example.ui.components.HighResImage
import com.example.ui.components.MangaReaderScreen
import com.example.ui.components.ViauraExpressiveLoadingIndicator
import com.example.ui.components.OnlineQuickActionBar
import com.example.ui.components.StyledIcon
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VisibleFields
import com.example.ui.components.shareOnlineLink
import com.example.ui.components.swipeLeftForActions
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
import com.example.util.media.MediaDirectLink
import com.example.util.media.MediaInfo
import com.example.util.media.MediaLibraryStore
import com.example.util.media.MediaLink
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaPluginInfo
import com.example.util.media.PluginException
import com.example.util.media.PluginPlaybackSession
import com.example.util.media.PluginQueuedEpisode
import com.example.util.media.PluginQueuedEpisodes
import com.example.util.media.PluginQualityChoice
import com.example.util.media.MediaPost
import com.example.util.media.MediaProviderRepository
import com.example.util.media.MediaSavedVideo
import com.example.util.media.MediaHistoryStore
import com.example.util.media.MediaWatchedTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun DetailPill(text: String, primaryText: Color, surfaceColor: Color, borderColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = primaryText)
    }
}

/** Audio / quality / season box: a small caption, the current value and an arrow; tapping opens the bottom sheet. */
@Composable
internal fun PickerPill(
    caption: String,
    value: String,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(primaryText.copy(alpha = 0.05f))
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = caption, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = secondaryText, maxLines = 1)
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StyledIcon(
            drawableRes = R.drawable.viaura_arrow_right_solid,
            contentDescription = null,
            tint = primaryText.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = 90f }
        )
    }
}

/** Bottom sheet list (season / audio / quality) in the same transparent glass as the download sheet. */
@Composable
internal fun OptionSheet(
    title: String,
    options: List<String>,
    selected: Int,
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    DownloadGlassSheet(isDark = isDark, glassBlurTransparency = glassBlurTransparency, onDismiss = onDismiss) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText)
        Spacer(modifier = Modifier.height(8.dp))
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) primaryText.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) AccentSkyBlue else primaryText,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Text(text = "✓", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AccentSkyBlue)
                }
            }
        }
        // room below the last option, so it is never hidden behind the gesture bar
        Spacer(modifier = Modifier.height(72.dp))
    }
}
