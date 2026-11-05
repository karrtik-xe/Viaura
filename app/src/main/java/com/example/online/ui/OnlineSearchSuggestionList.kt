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

/**
 * Type-ahead list: up to five popular searches for what is typed, refreshed on every letter that is typed or deleted.
 * It sits between the search box and the results (the results start below it). Tapping a row puts it into the top
 * search box, the list glides up with an animation and the results of that search are shown right away.
 * [fetch] returns the suggestions for one query (YouTube suggestions, or the titles a media plugin finds).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun OnlineSearchSuggestions(
    query: String,
    isDark: Boolean,
    glassBlurTransparency: Float,
    modifier: Modifier = Modifier,
    fetch: suspend (String) -> List<String>
) {
    val typed = query.trim()
    var items by remember { mutableStateOf<List<String>>(emptyList()) }
    val alreadyPicked = typed.equals(OnlineSearchBridge.lastPicked.trim(), ignoreCase = true)
    // The list belongs to the keyboard: when the keyboard goes away (Done / Back / tap outside) the suggestions go away too.
    // Android 11+ reports the keyboard reliably; older versions keep the previous behaviour (list stays).
    val imeVisible = WindowInsets.isImeVisible
    val keyboardAllows = imeVisible || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R

    LaunchedEffect(typed) {
        if (typed.isBlank() || typed.startsWith("http", ignoreCase = true) || typed.startsWith("/") ||
            typed.equals(OnlineSearchBridge.lastPicked.trim(), ignoreCase = true)
        ) {
            items = emptyList()
            return@LaunchedEffect
        }
        delay(140)
        val fresh = try {
            withContext(Dispatchers.IO) { fetch(typed) }
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (_: Throwable) {
            emptyList()
        }
        items = fresh.map { it.trim() }
            .filter { it.isNotBlank() && !it.equals(typed, ignoreCase = true) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(5)
    }

    val surface = if (isDark) DarkGlassSurface else LightGlassSurface
    val border = if (isDark) DarkGlassBorder else LightGlassBorder
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val rowAlpha = (0.35f + 0.30f * (glassBlurTransparency / 100f).coerceIn(0f, 1f)).coerceIn(0.35f, 0.7f)
    val shape = RoundedCornerShape(18.dp)

    AnimatedVisibility(
        visible = items.isNotEmpty() && typed.isNotBlank() && !alreadyPicked && keyboardAllows,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
            androidx.compose.animation.expandVertically(
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ) +
            androidx.compose.animation.slideInVertically(
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) { -it / 4 },
        // after a suggestion is tapped the list glides up and away, the results move up into its place
        exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) +
            androidx.compose.animation.shrinkVertically(
                animationSpec = androidx.compose.animation.core.tween(340, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                shrinkTowards = Alignment.Top
            ) +
            androidx.compose.animation.slideOutVertically(
                animationSpec = androidx.compose.animation.core.tween(340, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) { -it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { suggestion ->
                val label = buildAnnotatedString {
                    if (suggestion.startsWith(typed, ignoreCase = true)) {
                        withStyle(SpanStyle(color = secondary)) { append(suggestion.substring(0, typed.length)) }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primary)) { append(suggestion.substring(typed.length)) }
                    } else {
                        withStyle(SpanStyle(color = primary)) { append(suggestion) }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(surface.copy(alpha = rowAlpha))
                        .border(1.dp, border.copy(alpha = 0.9f), shape)
                        .clickable { OnlineSearchBridge.pick(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = label,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
