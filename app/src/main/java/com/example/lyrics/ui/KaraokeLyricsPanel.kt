package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsSourceType
import com.example.domain.lyrics.SyncedLine
import com.example.domain.lyrics.SyncedWord
import com.example.ui.theme.AccentSkyBlue
import com.example.utils.media.LyricsUtils
import kotlin.math.abs

/** Shared synced/plain renderer for compact player lyrics and the full-screen, karaoke-style view. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioLyricsContent(
    lyrics: Lyrics?,
    fallbackText: String,
    positionMs: Long,
    primaryText: Color,
    secondaryText: Color,
    compact: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    durationMs: Long = 0L,
    isPlaying: Boolean = false,
    playbackSpeed: Float = 1f,
    replaceWithTranslation: Boolean = false,
    /** Lyrics are shown this many ms later (+) or earlier (-) than their stamps: the per-track timing nudge. */
    timeOffsetMs: Long = 0L,
    /** Called when the empty space around the lines (not a line itself) is tapped. */
    onBackgroundTap: (() -> Unit)? = null,
) {
    val parsedModel = remember(lyrics, fallbackText) {
        lyrics?.takeIf { it.isValid() }
            ?: LyricsUtils.parseLyrics(fallbackText, sourceType = LyricsSourceType.LOCAL).takeIf { it.isValid() }
    }
    // Plain embedded tags do not contain enough data for exact karaoke sync. Prefer any real
    // timestamp model; when none exists, create a clearly best-effort duration-based timeline so
    // downloaded/imported lyrics are not permanently static.
    val model = remember(parsedModel, durationMs) {
        if (parsedModel?.synced.isNullOrEmpty()) {
            LyricsUtils.estimateTimingForPlainLyrics(parsedModel, durationMs) ?: parsedModel
        } else {
            LyricsUtils.ensureWordTimings(parsedModel) ?: parsedModel
        }
    }
    val synced = model?.synced.orEmpty()
    val plain = model?.plain.orEmpty()
    val listState = rememberLazyListState()
    val smoothPositionState = rememberSmoothedPositionMs(positionMs - timeOffsetMs, isPlaying, playbackSpeed)
    val smoothPosition by smoothPositionState
    val activeIndex = remember(synced, smoothPosition) {
        LyricsUtils.getActiveLineIndex(synced, smoothPosition)
    }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val centerPadding = with(density) { (viewportHeightPx / 2f).toDp() }
    val activeColor = AccentSkyBlue

    // Centre newly-active lines. Fullscreen uses half-viewport content padding so the first and
    // last lyric can also pass through the same central focus point as lines in the middle.
    LaunchedEffect(activeIndex, synced.size, isFullscreen, viewportHeightPx, model) {
        if (activeIndex !in synced.indices || viewportHeightPx <= 0) return@LaunchedEffect
        try {
            var item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeIndex }
            if (item == null) {
                listState.animateScrollToItem(activeIndex)
                withFrameNanos { }
                item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeIndex }
            }
            item?.let {
                val info = listState.layoutInfo
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val itemCenter = it.offset + it.size / 2f
                val delta = itemCenter - viewportCenter
                if (abs(delta) > 1f) {
                    listState.animateScrollBy(
                        delta,
                        animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessLow),
                    )
                }
            }
        } catch (_: Exception) {
            // The list can be replaced while a provider's asynchronous lyrics result arrives.
        }
    }

    val tapModifier = if (onBackgroundTap != null) {
        Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onBackgroundTap,
        )
    } else Modifier

    val fadeModifier = if (isFullscreen) {
        Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.12f to Color.Black,
                        0.86f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
    } else Modifier

    if (synced.isNotEmpty()) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().onSizeChanged { viewportHeightPx = it.height }.then(fadeModifier).then(tapModifier),
            contentPadding = PaddingValues(
                horizontal = if (isFullscreen) 18.dp else if (compact) 4.dp else 12.dp,
                vertical = if (isFullscreen) centerPadding else if (compact) 10.dp else 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                if (isFullscreen) 20.dp else if (compact) 6.dp else 10.dp,
            ),
        ) {
            itemsIndexed(synced, key = { index, line -> "${line.time}:$index:${line.line.hashCode()}" }) { index, line ->
                val active = index == activeIndex
                val scale by animateFloatAsState(
                    targetValue = when {
                        active && isFullscreen -> 1.075f
                        active -> 1.035f
                        isFullscreen -> 0.94f
                        else -> 1f
                    },
                    animationSpec = spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessLow),
                    label = "lyrics_line_zoom",
                )
                val distanceFromActive = if (activeIndex >= 0) abs(index - activeIndex) else index + 1
                val lineAlpha by animateFloatAsState(
                    targetValue = when {
                        active -> 1f
                        distanceFromActive == 1 -> 0.74f
                        distanceFromActive == 2 -> 0.52f
                        else -> 0.34f
                    },
                    animationSpec = tween(durationMillis = if (active) 320 else 460),
                    label = "lyrics_line_alpha",
                )
                val lineColor by animateColorAsState(
                    targetValue = when {
                        active && isFullscreen -> primaryText
                        active -> activeColor
                        isFullscreen -> secondaryText.copy(alpha = 0.48f)
                        else -> secondaryText.copy(alpha = 0.72f)
                    },
                    animationSpec = tween(durationMillis = 260),
                    label = "lyrics_line_color",
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .graphicsLayer { alpha = lineAlpha; scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(if (isFullscreen) 18.dp else 8.dp))
                        .background(if (active && !isFullscreen) AccentSkyBlue.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable { onSeekTo((line.time.toLong() + timeOffsetMs).coerceAtLeast(0L)) }
                        .padding(
                            horizontal = if (compact) 5.dp else if (isFullscreen) 8.dp else 12.dp,
                            vertical = if (isFullscreen) 4.dp else if (compact) 4.dp else 7.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val words = line.words.orEmpty()
                    if (active && words.isNotEmpty() && !(replaceWithTranslation && !line.translation.isNullOrBlank())) {
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            words.forEachIndexed { wordIndex, word ->
                                val wordStart = word.time.toLong()
                                val nextWord = words.getOrNull(wordIndex + 1)?.time?.toLong()?.takeIf { it > wordStart }
                                val nextLine = synced.getOrNull(index + 1)?.time?.toLong()
                                    ?.coerceAtMost(line.time.toLong() + 8_000L)?.takeIf { it > wordStart }
                                val endTime = nextWord
                                    ?: line.endTime?.toLong()?.takeIf { it > wordStart }
                                    ?: nextLine
                                    ?: (wordStart + 600L)
                                AnimatedLyricWord(
                                    word = word,
                                    endTimeMs = endTime,
                                    positionMs = smoothPositionState,
                                    activeColor = primaryText,
                                    inactiveColor = primaryText.copy(alpha = 0.38f),
                                    fontSize = when {
                                        isFullscreen -> 29.sp
                                        compact -> 15.sp
                                        else -> 21.sp
                                    },
                                    isRtl = line.line.hasRtlDirection(),
                                    trailingSpace = word.startsNewWord,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (replaceWithTranslation && !line.translation.isNullOrBlank()) {
                                androidx.compose.ui.text.buildAnnotatedString { append(line.translation.orEmpty()) }
                            } else lyricAnnotatedText(line, active, primaryText, secondaryText),
                            fontSize = when {
                                active && isFullscreen -> 28.sp
                                isFullscreen -> 23.sp
                                active && !compact -> 18.sp
                                active -> 16.sp
                                compact -> 13.5.sp
                                else -> 15.sp
                            },
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = when {
                                isFullscreen -> 34.sp
                                compact -> 19.sp
                                else -> 23.sp
                            },
                            color = lineColor,
                        )
                    }
                    line.translation?.takeIf { it.isNotBlank() && !replaceWithTranslation }?.let { translation ->
                        Text(
                            text = translation,
                            fontSize = if (isFullscreen) 17.sp else if (compact) 10.5.sp else 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (active) activeColor.copy(alpha = 0.86f) else secondaryText.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            lineHeight = if (isFullscreen) 22.sp else 16.sp,
                            modifier = Modifier.padding(top = if (isFullscreen) 5.dp else 2.dp),
                        )
                    }
                    line.background?.let { background ->
                        val backingStart = background.time.toLong() - 120L
                        val backingEnd = (background.endTime ?: line.endTime)?.toLong()?.plus(350L)
                            ?: (background.time.toLong() + 3_000L)
                        val backingIsSinging = smoothPosition in backingStart until backingEnd
                        AnimatedVisibility(
                            visible = backingIsSinging,
                            enter = fadeIn(tween(320)) + slideInVertically { it / 3 },
                            exit = fadeOut(tween(280)) + slideOutVertically(tween(300)) { it / 4 },
                        ) {
                            val backingWords = background.words.orEmpty()
                            if (backingWords.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.padding(top = 3.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    backingWords.forEachIndexed { backingIndex, backingWord ->
                                        val wordStart = backingWord.time.toLong()
                                        val wordEnd = backingWords.getOrNull(backingIndex + 1)?.time?.toLong()
                                            ?.takeIf { it > wordStart }
                                            ?: background.endTime?.toLong()?.takeIf { it > wordStart }
                                            ?: (wordStart + 600L)
                                        AnimatedLyricWord(
                                            word = backingWord,
                                            endTimeMs = wordEnd,
                                            positionMs = smoothPositionState,
                                            activeColor = activeColor.copy(alpha = 0.82f),
                                            inactiveColor = secondaryText.copy(alpha = 0.42f),
                                            fontSize = if (isFullscreen) 15.sp else if (compact) 10.sp else 12.sp,
                                            isRtl = background.line.hasRtlDirection(),
                                            trailingSpace = backingWord.startsNewWord,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = background.translation?.takeIf { it.isNotBlank() } ?: background.line,
                                    fontSize = if (isFullscreen) 15.sp else if (compact) 10.sp else 12.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = activeColor.copy(alpha = 0.72f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    } else if (plain.isNotEmpty()) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().then(fadeModifier).then(tapModifier),
            contentPadding = PaddingValues(vertical = if (isFullscreen) centerPadding else 12.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
        ) {
            itemsIndexed(plain, key = { index, line -> "$index:${line.hashCode()}" }) { _, text ->
                Text(
                    text = text,
                    fontSize = if (isFullscreen) 25.sp else if (compact) 13.5.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    textAlign = TextAlign.Center,
                    lineHeight = if (isFullscreen) 33.sp else 22.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = if (isFullscreen) 6.dp else 5.dp),
                )
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize().then(tapModifier), contentAlignment = Alignment.Center) {}
    }
}

/** Frame-smoothed playhead: extrapolates between MediaPlayer's periodic position updates. */
@Composable
private fun rememberSmoothedPositionMs(positionMs: Long, isPlaying: Boolean, playbackSpeed: Float): State<Long> {
    val state = remember { mutableLongStateOf(positionMs.coerceAtLeast(0L)) }
    LaunchedEffect(positionMs, isPlaying, playbackSpeed) {
        val anchorPosition = positionMs.coerceAtLeast(0L)
        state.longValue = anchorPosition
        if (!isPlaying) return@LaunchedEffect
        val anchorFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            // Position updates stop while the stream buffers; never run more than ~0.7 s ahead of the last
            // real position, or the lyrics race away from the singer on slow connections.
            val elapsedMs = ((frame - anchorFrame).coerceAtLeast(0L) / 1_000_000f).coerceAtMost(700f)
            state.longValue = (anchorPosition + elapsedMs * playbackSpeed.coerceIn(0.25f, 4f)).toLong()
        }
    }
    return state
}

/** Smooth per-word karaoke fill, following MpvRx's active/inactive two-layer word rendering. */
@Composable
private fun AnimatedLyricWord(
    word: SyncedWord,
    endTimeMs: Long,
    positionMs: State<Long>,
    activeColor: Color,
    inactiveColor: Color,
    fontSize: TextUnit,
    isRtl: Boolean,
    trailingSpace: Boolean,
) {
    val text = word.word + if (trailingSpace) " " else ""
    val style: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
    )
    if (text.isEmpty()) return
    val startTimeMs = word.time.toLong()
    val wordDurationMs = (endTimeMs - startTimeMs).coerceAtLeast(1L)
    Box(contentAlignment = Alignment.CenterStart) {
        Text(text = text, color = inactiveColor, style = style)
        Text(
            text = text,
            color = activeColor,
            style = style.copy(shadow = androidx.compose.ui.graphics.Shadow(
                color = activeColor.copy(alpha = 0.42f), offset = Offset.Zero, blurRadius = 10f,
            )),
            modifier = Modifier.drawWithContent {
                val progress = ((positionMs.value - startTimeMs).toFloat() / wordDurationMs).coerceIn(0f, 1f)
                val left = if (isRtl) size.width * (1f - progress) else 0f
                val right = if (isRtl) size.width else size.width * progress
                clipRect(left = left, right = right) { this@drawWithContent.drawContent() }
            },
        )
    }
}

private fun lyricAnnotatedText(
    line: SyncedLine,
    isActive: Boolean,
    primaryText: Color,
    secondaryText: Color,
) = buildAnnotatedString {
    val words = line.words.orEmpty()
    if (words.isEmpty()) {
        append(line.line)
        return@buildAnnotatedString
    }
    words.forEachIndexed { index, word ->
        if (index > 0 && word.startsNewWord) append(" ")
        withStyle(
            SpanStyle(
                color = if (isActive) primaryText else secondaryText,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            ),
        ) { append(word.word) }
    }
}


private fun String.hasRtlDirection(): Boolean =
    firstNotNullOfOrNull { character ->
        when (Character.getDirectionality(character)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> false
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> true
            else -> null
        }
    } ?: false
