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

// --------------------------------------------------------------------------------------------------------------------
// One downloaded plugin (provider) page: sliding trending banner, "Continue watching", then the provider's categories.
// --------------------------------------------------------------------------------------------------------------------

/** The category that feeds the sliding banner: a trending / popular / latest one when the plugin has it, else the first. */
private fun heroEntry(cat: MediaProviderRepository.Catalog): MediaCatalogEntry? {
    val all = cat.categories
    val keywords = listOf("trending", "popular", "latest", "new", "top", "recent")
    for (k in keywords) {
        all.firstOrNull { it.title.contains(k, ignoreCase = true) }?.let { return it }
    }
    return all.firstOrNull() ?: cat.genres.firstOrNull()
}

@Composable
fun MediaPluginPage(
    plugin: MediaPluginInfo,
    isDark: Boolean,
    onOpenPost: (MediaPost) -> Unit = {},
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val context = LocalContext.current
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    val scope = rememberCoroutineScope()
    val savedTick by MediaLibraryStore.changes.collectAsState()
    val savedVideos = remember(savedTick, plugin.value) { MediaLibraryStore.watchLater(context, plugin.value) }
    val historyTick by MediaHistoryStore.changes.collectAsState()
    val watched = remember(historyTick, plugin.value) { MediaHistoryStore.recent(context, plugin.value) }
    var playingSaved by remember { mutableStateOf(false) }
    var playSavedJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    // Back while the servers are still being checked: the whole check is cancelled (its network requests stop too)
    androidx.activity.compose.BackHandler(enabled = playingSaved) {
        playSavedJob?.cancel()
        playSavedJob = null
        playingSaved = false
    }

    fun playSaved(v: MediaSavedVideo) {
        if (playingSaved) return
        playingSaved = true
        Toast.makeText(context, "Connecting to servers…", Toast.LENGTH_SHORT).show()
        playSavedJob = scope.launch {
            try {
                // All servers are raced at the same time first; the player opens with the one that won.
                val placeholder = com.example.util.media.PluginQueuedEpisodes.placeholder(
                    com.example.util.media.PluginQueuedEpisode(
                        plugin = plugin,
                        playerTitle = v.title,
                        poster = v.poster,
                        link = v.link,
                        type = v.type,
                        episodeIndex = 0,
                        episodeTitle = v.title,
                        isSeries = false,
                        choices = emptyList(),
                        selectedQuality = ""
                    )
                )
                val result = com.example.util.media.PluginQueuedEpisodes.resolve(context, placeholder.path)
                val item = result.getOrNull()
                if (item == null) {
                    Toast.makeText(context, "Could not get this video: " + (result.exceptionOrNull()?.message ?: "unknown error"), Toast.LENGTH_LONG).show()
                } else {
                    onPlayVideo(item, listOf(item))
                }
            } finally {
                playingSaved = false
            }
        }
    }

    var catalog by remember(plugin.value, plugin.version) { mutableStateOf<MediaProviderRepository.Catalog?>(null) }
    var catalogError by remember(plugin.value, plugin.version) { mutableStateOf<String?>(null) }
    var reloadKey by remember(plugin.value) { mutableStateOf(0) }
    var seeAll by remember(plugin.value) { mutableStateOf<MediaCatalogEntry?>(null) }
    // bumped by a pull-down while a result grid (search / "See all") is open: the grid loads again from page 1
    var gridReloadKey by remember(plugin.value) { mutableStateOf(0) }
    val typed = searchQuery.trim()

    // pull-down on the provider page (Home screen bumps the request): every cached row of this provider is dropped and loaded again
    val refreshTick = MediaDetailState.refreshRequest
    var handledRefresh by remember(plugin.value) { mutableStateOf(refreshTick) }
    LaunchedEffect(refreshTick) {
        if (refreshTick != handledRefresh) {
            handledRefresh = refreshTick
            MediaProviderRepository.clear(plugin.value)
            MediaGridMemory.clear(plugin.value)
            // search results / "See all" grids reload themselves, otherwise the categories are loaded again
            if (typed.isNotEmpty() || seeAll != null) gridReloadKey++ else reloadKey++
        }
    }
    // the pull-to-refresh animation can never stay on screen for long, whatever happened
    LaunchedEffect(MediaDetailState.providerRefreshing) {
        if (MediaDetailState.providerRefreshing) {
            delay(20_000L)
            MediaDetailState.providerRefreshing = false
        }
    }

    LaunchedEffect(plugin.value, plugin.version, reloadKey) {
        catalog = null
        catalogError = null
        try {
            catalog = MediaProviderRepository.loadCatalog(context, plugin.value, force = reloadKey > 0)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            catalogError = t.message ?: "Could not load this plugin"
        } finally {
            MediaDetailState.providerRefreshing = false
        }
    }

    // top search bar of the app: results of this provider replace the page while something is typed.
    // A short pause lets fast typing / pasting send one request instead of one per letter; a search that was
    // answered before is shown at once.
    var activeQuery by remember(plugin.value) { mutableStateOf("") }
    LaunchedEffect(typed) {
        if (typed.isEmpty()) {
            activeQuery = ""
            return@LaunchedEffect
        }
        if (MediaProviderRepository.cachedSearch(plugin.value, typed, 1) == null &&
            !typed.equals(com.example.ui.components.OnlineSearchBridge.lastPicked.trim(), ignoreCase = true)) delay(250L)
        activeQuery = typed
    }
    if (typed.isNotEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            // Suggestions: the first five titles this plugin (Vega or CloudStream) finds for the typed text; results start below them
            com.example.ui.components.OnlineSearchSuggestions(
                query = typed,
                isDark = isDark,
                glassBlurTransparency = 85f,
                fetch = { q ->
                    MediaProviderRepository.searchPosts(context, plugin.value, q, 1).map { it.title }
                }
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (activeQuery.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 40.dp).size(28.dp),
                        color = AccentSkyBlue,
                        strokeWidth = 2.6.dp
                    )
                }
            } else {
                val searchIdentity = plugin.value + "|search|" + activeQuery + "|" + gridReloadKey
                key(searchIdentity) {
                    MediaPostGrid(
                        identity = searchIdentity,
                        title = "Results for \"$activeQuery\"",
                        isDark = isDark,
                        onBack = null,
                        onOpenPost = onOpenPost,
                        loader = { page -> MediaProviderRepository.searchPosts(context, plugin.value, activeQuery, page) },
                        onFirstLoad = { MediaDetailState.providerRefreshing = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            }
        }
        return
    }

    BackHandler(enabled = seeAll != null) { seeAll = null }

    val current = seeAll
    if (current != null) {
        val gridIdentity = plugin.value + "|" + current.filter + "|" + gridReloadKey
        key(gridIdentity) {
            MediaPostGrid(
                identity = gridIdentity,
                title = current.title,
                isDark = isDark,
                onBack = { seeAll = null },
                onOpenPost = onOpenPost,
                loader = { page -> MediaProviderRepository.loadPosts(context, plugin.value, current.filter, page) },
                onFirstLoad = { MediaDetailState.providerRefreshing = false },
                modifier = modifier
            )
        }
        return
    }

    val cat = catalog
    when {
        catalogError != null -> {
            Column(
                modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = catalogError.orEmpty(), fontSize = 13.sp, color = secondaryText)
                Spacer(modifier = Modifier.height(14.dp))
                RetryPill(primaryText, surfaceColor, borderColor) { reloadKey++ }
            }
        }
        cat == null -> {
            Box(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 40.dp).size(28.dp),
                    color = AccentSkyBlue,
                    strokeWidth = 2.6.dp
                )
            }
        }
        cat.categories.isEmpty() && cat.genres.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
                Text(text = "This plugin has no categories", fontSize = 13.sp, color = secondaryText, modifier = Modifier.padding(top = 32.dp))
            }
        }
        else -> {
            val hero = remember(cat) { heroEntry(cat) }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
            ) {
                if (hero != null) {
                    item(key = "hero") {
                        MediaHeroSlider(plugin, hero, isDark, borderColor, onOpenPost)
                    }
                }
                if (watched.isNotEmpty()) {
                    item(key = "continue-watching") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            MediaSectionHeader(
                                title = "Continue watching",
                                iconRes = R.drawable.viaura_recent,
                                primaryText = primaryText,
                                surfaceColor = surfaceColor,
                                borderColor = borderColor,
                                onSeeAll = null
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            MediaFocusRow(
                                posts = watched.map { MediaPost(it.title, it.link, it.image, null) },
                                isDark = isDark,
                                primaryText = primaryText,
                                borderColor = borderColor,
                                onOpenPost = onOpenPost,
                                onRemove = { MediaHistoryStore.remove(context, plugin.value, it.link) }
                            )
                        }
                    }
                }
                if (savedVideos.isNotEmpty()) {
                    item(key = "watch-later") {
                        MediaWatchLaterRow(
                            videos = savedVideos,
                            isDark = isDark,
                            primaryText = primaryText,
                            surfaceColor = surfaceColor,
                            borderColor = borderColor,
                            onPlay = { playSaved(it) },
                            onRemove = { MediaLibraryStore.remove(context, it.pluginId, it.link) }
                        )
                    }
                }
                items(cat.categories, key = { "c|" + it.title + "|" + it.filter }) { entry ->
                    MediaCategoryRow(plugin, entry, isDark, primaryText, secondaryText, surfaceColor, borderColor, R.drawable.viaura_video_library, onOpenPost) { seeAll = entry }
                }
                if (cat.genres.isNotEmpty()) {
                    item(key = "genres-label") {
                        Text(
                            text = "Genres",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryText,
                            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 2.dp)
                        )
                    }
                    items(cat.genres, key = { "g|" + it.title + "|" + it.filter }) { entry ->
                        MediaCategoryRow(plugin, entry, isDark, primaryText, secondaryText, surfaceColor, borderColor, R.drawable.viaura_list, onOpenPost) { seeAll = entry }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RetryPill(primaryText: Color, surfaceColor: Color, borderColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text = "Retry", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = primaryText)
    }
}

/**
 * Sliding banner at the top of a provider page: the trending / latest titles of the provider (at most 6) change by
 * themselves every few seconds; the neighbours peek in and the page in the middle is the biggest.
 */
@Composable
private fun MediaHeroSlider(
    plugin: MediaPluginInfo,
    entry: MediaCatalogEntry,
    isDark: Boolean,
    borderColor: Color,
    onOpenPost: (MediaPost) -> Unit
) {
    val context = LocalContext.current
    var posts by remember(plugin.value, entry.filter) { mutableStateOf<List<MediaPost>?>(null) }
    LaunchedEffect(plugin.value, entry.filter) {
        posts = try {
            MediaProviderRepository.loadPosts(context, plugin.value, entry.filter, 1)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            emptyList()
        }
    }
    val loaded = posts
    val slides = remember(loaded) {
        val all = loaded.orEmpty()
        all.filter { it.image.isNotBlank() }.ifEmpty { all }.take(6)
    }
    val placeholderBg = if (isDark) Neutral.D111827 else Neutral.E2E8F0
    if (loaded == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(210.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(placeholderBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentSkyBlue, strokeWidth = 2.4.dp)
        }
        return
    }
    if (slides.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { slides.size })
    LaunchedEffect(slides.size) {
        if (slides.size < 2) return@LaunchedEffect
        while (true) {
            delay(4500L)
            if (!pagerState.isScrollInProgress) {
                val next = (pagerState.currentPage + 1) % slides.size
                pagerState.animateScrollToPage(next, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 26.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val post = slides[page]
            val distance = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .graphicsLayer {
                        val s = 1f - 0.07f * distance
                        scaleX = s
                        scaleY = s
                        alpha = 1f - 0.35f * distance
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(placeholderBg)
                    .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenPost(post) }
            ) {
                HighResImage(
                    url = post.image.ifBlank { null },
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                    placeholder = {
                        StyledIcon(
                            drawableRes = R.drawable.viaura_video,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.35f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.86f)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = entry.title.uppercase(),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGradient)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post.title,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (slides.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.indices.forEach { i ->
                    val active = pagerState.currentPage == i
                    val w by animateFloatAsState(if (active) 22f else 7f, tween(300), label = "heroDot")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(w.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (active) AccentGradient else Brush.linearGradient(listOf(borderColor, borderColor)))
                    )
                }
            }
        }
    }
}

/** Section title like the reference design: a small rounded icon tile + upper-case title, "See all" with a gradient line below. */
@Composable
internal fun MediaSectionHeader(
    title: String,
    iconRes: Int,
    primaryText: Color,
    surfaceColor: Color,
    borderColor: Color,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(surfaceColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                drawableRes = iconRes,
                contentDescription = null,
                tint = primaryText,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onSeeAll != null) {
            // no round background any more: just the text and a gradient line under it
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSeeAll)
                    .padding(horizontal = 2.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "See all", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = primaryText)
                    Spacer(modifier = Modifier.width(4.dp))
                    StyledIcon(
                        drawableRes = R.drawable.viaura_arrow_right_solid,
                        contentDescription = null,
                        tint = primaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(AccentGradient)
                )
            }
        }
    }
}

/**
 * Horizontal strip of posters. Like the reference design the cards stand a little uneven (every second one is raised),
 * the cards near the middle of the screen are bigger and straight, the ones towards the edges are smaller and tilt
 * outwards, so the strip sways while it is dragged; a touched card bounces a little.
 */
@Composable
private fun MediaFocusRow(
    posts: List<MediaPost>,
    isDark: Boolean,
    primaryText: Color,
    borderColor: Color,
    onOpenPost: (MediaPost) -> Unit,
    onRemove: ((MediaPost) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(posts.size, key = { posts[it].link + "|" + posts[it].title }) { index ->
            val post = posts[index]
            MediaPosterCard(
                post = post,
                index = index,
                listState = listState,
                isDark = isDark,
                primaryText = primaryText,
                borderColor = borderColor,
                onClick = { onOpenPost(post) },
                onRemove = onRemove?.let { remover -> { remover(post) } }
            )
        }
    }
}

/** Title + "See all" + a strip of posters; the posts load when the row scrolls into view. */
@Composable
private fun MediaCategoryRow(
    plugin: MediaPluginInfo,
    entry: MediaCatalogEntry,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    surfaceColor: Color,
    borderColor: Color,
    iconRes: Int,
    onOpenPost: (MediaPost) -> Unit,
    onSeeAll: () -> Unit
) {
    val context = LocalContext.current
    var posts by remember(plugin.value, entry.filter) { mutableStateOf<List<MediaPost>?>(null) }
    var error by remember(plugin.value, entry.filter) { mutableStateOf<String?>(null) }
    var attempt by remember(plugin.value, entry.filter) { mutableStateOf(0) }

    LaunchedEffect(plugin.value, entry.filter, attempt) {
        if (posts != null) return@LaunchedEffect
        error = null
        // A site that answers slowly / refuses one request used to make the whole category vanish: it is asked again
        // (3 tries in all) before the row shows its error with a Retry button.
        var lastError: Throwable? = null
        for (try_ in 0 until 3) {
            try {
                if (try_ > 0) delay(1500L * try_)
                posts = MediaProviderRepository.loadPosts(context, plugin.value, entry.filter, 1, force = attempt > 0 || try_ > 0)
                lastError = null
                break
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                lastError = t
            }
        }
        if (lastError != null) error = lastError.message ?: "Could not load"
    }

    // only a category that loaded fine and really has nothing is left out; a failed one stays with its Retry button
    if (error == null && posts != null && posts?.isEmpty() == true) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        MediaSectionHeader(
            title = entry.title,
            iconRes = iconRes,
            primaryText = primaryText,
            surfaceColor = surfaceColor,
            borderColor = borderColor,
            onSeeAll = onSeeAll
        )
        val list = posts
        when {
            list != null && list.isNotEmpty() -> {
                MediaFocusRow(
                    posts = list,
                    isDark = isDark,
                    primaryText = primaryText,
                    borderColor = borderColor,
                    onOpenPost = onOpenPost
                )
            }
            list != null -> {
                Text(text = "Nothing found", fontSize = 12.5.sp, color = secondaryText, modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 8.dp))
            }
            error != null -> {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error.orEmpty(),
                        fontSize = 12.sp,
                        color = secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    RetryPill(primaryText, surfaceColor, borderColor) { attempt++ }
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AccentSkyBlue, strokeWidth = 2.4.dp)
                }
            }
        }
    }
}
