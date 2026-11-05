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

/**
 * What the Media section currently has open. It lives outside composition on purpose: after the player is closed the
 * same title (with the same season / quality) is shown again, and the Media tab that was open is selected again.
 */
object MediaDetailState {
    var plugin by androidx.compose.runtime.mutableStateOf<MediaPluginInfo?>(null)
    var post by androidx.compose.runtime.mutableStateOf<MediaPost?>(null)
    var lastSection by mutableIntStateOf(0)
    /** The provider page that is open inside Network > Media (null = the plugin list). Kept so Back from a title / the player returns to it. */
    var provider by androidx.compose.runtime.mutableStateOf<MediaPluginInfo?>(null)
    /** Pull-down on an open provider page: bumped by the Home screen, handled by the page (reloads categories + rows). */
    var refreshRequest by mutableIntStateOf(0)
    /** True from the pull-down until the provider page has loaded its categories again (drives the refresh indicator). */
    var providerRefreshing by androidx.compose.runtime.mutableStateOf(false)
    /** "plugin|link" -> [season index, audio index, quality index] */
    val selections = HashMap<String, IntArray>()

    fun open(plugin: MediaPluginInfo, post: MediaPost) {
        this.plugin = plugin
        this.post = post
    }

    fun close() {
        plugin = null
        post = null
    }
}
