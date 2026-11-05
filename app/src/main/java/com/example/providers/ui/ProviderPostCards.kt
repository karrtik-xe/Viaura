package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.components.HighResImage
import com.example.util.media.MediaHistoryStore
import kotlinx.coroutines.delay
import kotlin.math.abs
import com.example.ui.components.LinkThumbnail
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
import com.example.util.media.MediaCatalogEntry
import com.example.util.media.MediaLibraryStore
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaSavedVideo
import com.example.util.media.MediaPluginInfo
import com.example.util.media.MediaPluginStore
import com.example.util.media.MediaPost
import com.example.util.media.MediaProviderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun MediaPosterCard(
    post: MediaPost,
    index: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isDark: Boolean,
    primaryText: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "posterPress"
    )
    Column(
        modifier = Modifier
            .width(126.dp)
            .graphicsLayer {
                val info = listState.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                val raise = if (index % 2 == 1) -10.dp.toPx() else 0f
                if (item != null && info.viewportSize.width > 0) {
                    val half = info.viewportSize.width / 2f
                    val frac = (((item.offset + item.size / 2f) - half) / half).coerceIn(-1f, 1f)
                    val near = 1f - abs(frac)
                    val s = (0.9f + 0.1f * near) * press
                    scaleX = s
                    scaleY = s
                    rotationZ = frac * 5f
                    translationY = raise
                } else {
                    scaleX = 0.9f * press
                    scaleY = 0.9f * press
                    translationY = raise
                }
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) Neutral.D111827 else Neutral.E2E8F0)
                .border(1.dp, borderColor, RoundedCornerShape(22.dp))
        ) {
            LinkThumbnail(
                thumbnail = post.image.ifBlank { null },
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_video,
                        contentDescription = null,
                        tint = primaryText.copy(alpha = 0.35f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            )
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", fontSize = 12.sp, color = Color.White)
                }
            }
            post.tag?.let { tag ->
                Text(
                    text = tag,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = post.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

/** Plain poster card of the grids ("See all" / search results). */
@Composable
private fun MediaPostCard(
    post: MediaPost,
    isDark: Boolean,
    primaryText: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            } else Modifier
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Neutral.D111827 else Neutral.E2E8F0)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
        ) {
            LinkThumbnail(
                thumbnail = post.image.ifBlank { null },
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_video,
                        contentDescription = null,
                        tint = primaryText.copy(alpha = 0.35f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            )
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", fontSize = 12.sp, color = Color.White)
                }
            }
            post.tag?.let { tag ->
                Text(
                    text = tag,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = post.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A grid of thumbnails; the next pages load while scrolling. Used for "See all" of a category ([onBack] set) and for
 * the search results of a provider ([onBack] null: the app's top search bar is the way out).
 */
@Composable
internal fun MediaPostGrid(
    identity: String,
    title: String,
    isDark: Boolean,
    onBack: (() -> Unit)?,
    onOpenPost: (MediaPost) -> Unit,
    loader: suspend (page: Int) -> List<MediaPost>,
    modifier: Modifier = Modifier,
    onFirstLoad: () -> Unit = {}
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    // A result list that was on screen before (a title was opened / the player was used) comes back as it was: same items, same pages, same scroll place
    val saved = remember(identity) { MediaGridMemory.get(identity) }
    val items = remember(identity) { mutableStateListOf<MediaPost>().also { l -> if (saved != null) l.addAll(saved.items) } }
    var page by remember(identity) { mutableStateOf(saved?.page ?: 1) }
    var loading by remember(identity) { mutableStateOf(false) }
    var ended by remember(identity) { mutableStateOf(saved?.ended ?: false) }
    var error by remember(identity) { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState(saved?.index ?: 0, saved?.offset ?: 0)
    DisposableEffect(identity) {
        onDispose {
            if (items.isNotEmpty()) MediaGridMemory.put(identity, items.toList(), page, ended, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }
    }

    suspend fun loadNext() {
        if (loading || ended) return
        loading = true
        error = null
        try {
            val fresh = loader(page)
            val known = items.map { it.link + "|" + it.title }.toHashSet()
            val added = fresh.filter { known.add(it.link + "|" + it.title) }
            if (added.isEmpty()) ended = true else { items.addAll(added); page++ }
            onFirstLoad()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            error = t.message ?: "Could not load"
            ended = items.isNotEmpty()
            onFirstLoad()
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = if (onBack != null) 8.dp else 18.dp, end = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                PluginIconButton(R.drawable.viaura_arrow_left_solid, "Back", primaryText, onBack)
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.link + "|" + it.title }) { post ->
                MediaPostCard(post, isDark, primaryText, borderColor, onClick = { onOpenPost(post) })
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "footer") {
                // Composed when the end of the grid is reached -> loads the next page.
                LaunchedEffect(identity, page, ended, error) {
                    if (!ended && error == null) loadNext()
                }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentSkyBlue, strokeWidth = 2.4.dp)
                        error != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = error.orEmpty(), fontSize = 12.sp, color = secondaryText, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
                            Spacer(modifier = Modifier.width(10.dp))
                            RetryPill(primaryText, surfaceColor, borderColor) { error = null; ended = false }
                        }
                        ended && items.isEmpty() -> Text(text = "Nothing found", fontSize = 12.5.sp, color = secondaryText)
                        else -> Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}

/** Result lists of the provider pages (search / "See all"), kept while a title or the player is open so Back finds the same list at the same place. */
internal object MediaGridMemory {
    class Saved(val items: List<MediaPost>, val page: Int, val ended: Boolean, val index: Int, val offset: Int, val at: Long)

    private const val KEEP_MS = 20 * 60_000L
    private val map = object : LinkedHashMap<String, Saved>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Saved>?): Boolean = size > 10
    }

    @Synchronized fun get(identity: String): Saved? {
        val s = map[identity] ?: return null
        if (System.currentTimeMillis() - s.at > KEEP_MS) { map.remove(identity); return null }
        return s
    }

    @Synchronized fun put(identity: String, items: List<MediaPost>, page: Int, ended: Boolean, index: Int, offset: Int) {
        map[identity] = Saved(items, page, ended, index, offset, System.currentTimeMillis())
    }

    /** A pull-down refresh of a provider: its saved lists are stale. */
    @Synchronized fun clear(pluginId: String) {
        map.keys.filter { it.startsWith("$pluginId|") }.forEach { map.remove(it) }
    }
}

/** "Watch later" category: the movies / episodes saved from a detail page. Tap plays, the cross removes. */
@Composable
internal fun MediaWatchLaterRow(
    videos: List<MediaSavedVideo>,
    isDark: Boolean,
    primaryText: Color,
    surfaceColor: Color,
    borderColor: Color,
    onPlay: (MediaSavedVideo) -> Unit,
    onRemove: (MediaSavedVideo) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        MediaSectionHeader(
            title = "Watch later",
            iconRes = R.drawable.viaura_watch_later,
            primaryText = primaryText,
            surfaceColor = surfaceColor,
            borderColor = borderColor,
            onSeeAll = null
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(videos, key = { "wl|" + it.link }) { v ->
                MediaPostCard(
                    post = MediaPost(v.title, v.link, v.poster, null),
                    isDark = isDark,
                    primaryText = primaryText,
                    borderColor = borderColor,
                    modifier = Modifier.width(118.dp),
                    onClick = { onPlay(v) },
                    onRemove = { onRemove(v) }
                )
            }
        }
    }
}
