package com.example.ui.components

import com.example.ui.theme.Neutral

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.example.util.OnlineMediaLibrary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AudioPlaybackManager
import com.example.ui.screens.AudioTrackGridCard
import com.example.ui.screens.AudioTrackItem
import com.example.ui.screens.AudioTrackListCard
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.AudioPlaybackHistoryManager
import com.example.util.OnlineChannelResult
import com.example.util.OnlineMusicService
import com.example.util.OnlineInterestProfile
import com.example.util.OnlineRecommendationMixer
import com.example.util.OnlineTrackResult
import com.example.util.OnlineTrackSequencer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.search.SearchExtractor
import java.util.Locale

/** Path prefix used to tag AudioTrackItem instances that came from Online Music, not disk. */
const val ONLINE_TRACK_PATH_PREFIX = "online://"

fun OnlineTrackResult.toAudioTrackItem(): AudioTrackItem {
    val stableId = -(kotlin.math.abs(videoUrl.hashCode().toLong()))
    val cachedMeta = OnlineMusicService.getCachedAudioMeta(videoUrl)
    val titleFormat = OnlineMusicService.detectFormatFromTitle(title)
    val titleAudioType = OnlineMusicService.detectAudioTypeFromTitle(title)

    val finalFormat = detectedFormat ?: cachedMeta?.first ?: titleFormat ?: "M4A"
    val finalAudioType = detectedAudioType ?: cachedMeta?.second ?: titleAudioType ?: "Stereo"

    return AudioTrackItem(
        id = stableId,
        uri = Uri.parse(videoUrl),
        title = title,
        artist = artist.ifBlank { "YouTube Music" },
        album = "Online Music",
        durationMs = durationSec.coerceAtLeast(0L) * 1000L,
        path = "$ONLINE_TRACK_PATH_PREFIX$videoUrl",
        sizeBytes = 0L,
        format = finalFormat,
        audioType = finalAudioType,
        dateModified = uploadDateEpochMs ?: 0L,
        thumbnailUrl = thumbnailUrl
    )
}

/**
 * Link between the floating search suggestions (Online music / Online video / media plugins) and the app's top search box.
 * A tapped suggestion is published in [picked]; HomeScreen writes it into the search box and clears it again.
 * [lastPicked] makes that search start at once (no typing pause) and keeps the suggestion list closed for it.
 */
object OnlineSearchBridge {
    var picked by mutableStateOf<String?>(null)
    var lastPicked by mutableStateOf("")

    fun pick(text: String) {
        lastPicked = text
        picked = text
    }
}

/**
 * Process-wide state of the Online Audio folder (same idea as [OnlineVideoFeedState]): leaving the folder and coming
 * back shows the SAME recommendations at the SAME scroll position. The feed only changes when the user pulls down from
 * the top to refresh, searches, or restarts the app; scrolling down keeps adding more.
 */
object OnlineAudioFeedState {
    var results by mutableStateOf<List<OnlineTrackResult>>(emptyList())
    var channelResults by mutableStateOf<List<OnlineChannelResult>>(emptyList())
    var hasSearchedOnce by mutableStateOf(false)
    var extractor by mutableStateOf<SearchExtractor?>(null)
    var nextPage by mutableStateOf<Page?>(null)
    var hasNextPage by mutableStateOf(false)
    var searchSuggestion by mutableStateOf<String?>(null)
    var isCorrectedSearch by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var refreshRequest by mutableIntStateOf(0)
    /** True once the user changed Sort & View sorting; until then search results keep their relevance order. */
    var sortTouched by mutableStateOf(false)
    /** Changes whenever the list is REPLACED (new search / new feed), so an old scroll position can't be carried over. */
    var generation by mutableIntStateOf(0)
    var scrollTopTick by mutableIntStateOf(0)
    var initialized = false
    var lastQuery: String = ""
    /** The feed that was on screen when a search started; clearing the search brings the SAME feed back. */
    var savedFeed: List<OnlineTrackResult>? = null
    val usedQueries = mutableSetOf<String>()
    var listIndex = 0
    var listOffset = 0
    var gridIndex = 0
    var gridOffset = 0
}
