package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.util.OnlineMediaLibrary
import com.example.util.OnlineVideoQueue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.screens.VideoItem
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextSecondary
import com.example.util.OnlineChannelResult
import com.example.util.OnlineMusicService
import com.example.util.OnlineTrackResult
import com.example.util.OnlineVideoRecommender
import com.example.util.OnlineVideoService
import com.example.util.cachedBestLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import java.util.Locale

/**
 * Process-wide state of the Online Video folder. It lives outside composition on purpose: when the user opens a video
 * and comes back, the SAME recommendations (and scroll position) are shown — the feed only changes when the user
 * pulls down to refresh or searches.
 */
object OnlineVideoFeedState {
    var isOpen by mutableStateOf(false)
    var results by mutableStateOf<List<OnlineTrackResult>>(emptyList())
    var channelResults by mutableStateOf<List<OnlineChannelResult>>(emptyList())
    var isSearching by mutableStateOf(false)
    var isLoadingMore by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var refreshRequest by mutableIntStateOf(0)
    /** True once the user changed Sort & View sorting; until then the feed keeps its recommendation order. */
    var sortTouched by mutableStateOf(false)
    /** "Watch later" videos: always pinned at the very top of the feed. */
    var watchLater by mutableStateOf<List<OnlineTrackResult>>(emptyList())
    /** Bumped whenever the "Not interested" list changes so the visible list is recomputed. */
    var hiddenVersion by mutableIntStateOf(0)
    var initialized = false
    var lastQuery: String = ""
    var extractor: SearchExtractor? = null
    var nextPage: Page? = null
    var hasNextPage: Boolean = false
    val usedQueries = mutableSetOf<String>()
    var token = 0
    /** Changes whenever the list is REPLACED (new search / new feed): the old scroll position can then never be carried over. */
    var generation by mutableIntStateOf(0)
    /** Bumped when the list must jump to its first item once the new items are laid out. */
    var scrollTopTick by mutableIntStateOf(0)
    /** The recommendation feed that was on screen when a search started; clearing the search brings the SAME feed back. */
    var savedFeed: List<OnlineTrackResult>? = null
    var listIndex = 0
    var listOffset = 0
    var gridIndex = 0
    var gridOffset = 0
}

/** Outlives the panel: the "after watching" recommendations keep loading while the player is on screen. */
internal val feedPrefetchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

internal var afterWatchJob: Job? = null

private fun formatViews(count: Long): String {
    if (count <= 0L) return ""
    val t = when {
        count >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
    return "$t views"
}

/** Converts a search/feed result into the app's regular VideoItem so the existing cards and player are reused. */
fun OnlineTrackResult.toOnlineVideoItem(): VideoItem = VideoItem(
    id = -(kotlin.math.abs(videoUrl.hashCode().toLong())) - 1L,
    uri = Uri.parse(videoUrl),
    displayName = title,
    path = "${OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX}$videoUrl",
    sizeBytes = 0L,
    durationMs = durationSec.coerceAtLeast(0L) * 1000L,
    dateModified = uploadDateEpochMs ?: 0L,
    resolution = OnlineVideoService.cachedBestLabel(videoUrl) ?: "",
    isOnline = true,
    thumbnailUrl = thumbnailUrl,
    channelName = artist,
    viewCountText = formatViews(viewCount).ifEmpty { null },
    isNew = isNew
)
