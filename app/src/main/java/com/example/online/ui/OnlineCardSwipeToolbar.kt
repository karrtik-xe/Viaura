package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.mutableStateListOf
import kotlin.reflect.KProperty
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.LightTextPrimary
import kotlinx.coroutines.launch

/**
 * Right-to-left swipe on an online card. The card only follows the finger a little (it is NOT selected / highlighted),
 * and [onTriggered] fires once when the swipe is long enough so the screen can show [OnlineQuickActionBar].
 * Vertical scrolling and taps keep working because only horizontal drags are claimed.
 */
fun Modifier.swipeLeftForActions(enabled: Boolean = true, selected: Boolean = false, onTriggered: () -> Unit): Modifier = composed {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 56.dp.toPx() }
    val maxPx = with(density) { 48.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val trigger by rememberUpdatedState(onTriggered)

    this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    val fire = total <= -thresholdPx
                    total = 0f
                    scope.launch { offset.animateTo(0f) }
                    if (fire) trigger()
                },
                onDragCancel = {
                    total = 0f
                    scope.launch { offset.animateTo(0f) }
                },
                onHorizontalDrag = { change, dx ->
                    change.consume()
                    total += dx
                    // only right -> left is used; the card never moves to the right
                    scope.launch { offset.snapTo(total.coerceIn(-maxPx, 0f)) }
                }
            )
        }
        .graphicsLayer { translationX = offset.value }
        .drawWithContent {
            drawContent()
            // A card that is part of the swipe selection: its top-right corner (about 7 % of the card) glows in the
            // accent gradient, with a small tick - the card itself stays as it is
            if (selected) {
                val r = 16.dp.toPx()
                val rad = kotlin.math.sqrt(size.width * size.height * 0.07f * 4f / Math.PI.toFloat())
                    .coerceAtMost(minOf(size.width, size.height) * 0.95f)
                val corner = Offset(size.width, 0f)
                val cardShape = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
                }
                clipPath(cardShape) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(AccentSkyBlue.copy(alpha = 0.95f), AccentPink.copy(alpha = 0.60f), Color.Transparent),
                            center = corner, radius = rad
                        ),
                        radius = rad, center = corner
                    )
                }
                val c = Offset(size.width - rad * 0.30f, rad * 0.30f)
                val t = rad * 0.11f
                val tick = Path().apply {
                    moveTo(c.x - t * 1.0f, c.y + t * 0.05f)
                    lineTo(c.x - t * 0.25f, c.y + t * 0.85f)
                    lineTo(c.x + t * 1.1f, c.y - t * 0.75f)
                }
                drawPath(tick, color = Color.White, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
}

/**
 * Selection behind the swipe toolbar. It behaves like the old single "action target" variable so the screens keep
 * their code: reading gives the LAST swiped item (null when nothing is selected); assigning an item adds it to the
 * selection (assigning a selected one again removes it); assigning null clears everything. Swiping more cards while the
 * toolbar is open therefore builds a multi-selection that the toolbar's Download button downloads in one go.
 */
class SwipeActionState<T : Any>(private val keyOf: (T) -> String) {
    val items = mutableStateListOf<T>()
    val size: Int get() = items.size

    fun contains(item: T): Boolean { val k = keyOf(item); return items.any { keyOf(it) == k } }

    fun clear() { items.clear() }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = items.lastOrNull()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        if (value == null) { items.clear(); return }
        val k = keyOf(value)
        val i = items.indexOfFirst { keyOf(it) == k }
        if (i >= 0) items.removeAt(i) else items.add(value)
    }
}

@Composable
fun <T : Any> rememberSwipeActionState(keyOf: (T) -> String): SwipeActionState<T> = remember { SwipeActionState(keyOf) }

/**
 * Bottom floating glass toolbar (same look as the selection toolbar of the offline lists):
 * Play next · Play last · Playlist · Watch later · Not interested · Share.
 */
@Composable
fun OnlineQuickActionBar(
    isDark: Boolean,
    alphaRatio: Float,
    isSavedForLater: Boolean,
    onPlayNext: (() -> Unit)? = null,
    onPlayLast: (() -> Unit)? = null,
    onSaveToPlaylist: () -> Unit,
    onWatchLater: () -> Unit,
    onNotInterested: (() -> Unit)? = null,
    onShare: () -> Unit,
    onDownload: (() -> Unit)? = null,
    /** Online folders (playlists): heart that adds / removes the item from Favourites. */
    isFavourite: Boolean = false,
    onFavourite: (() -> Unit)? = null,
    /** More than one card is selected: the bar shows only "N selected", Download all and Clear. */
    selectedCount: Int = 1,
    onClearSelection: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDark) {
        if (alphaRatio >= 0.99f) Neutral.D0F172A
        else Neutral.D0F172A.copy(alpha = (0.70f + 0.30f * alphaRatio).coerceIn(0.40f, 1.0f))
    } else {
        if (alphaRatio >= 0.99f) Neutral.FFFFFF
        else Neutral.FFFFFF.copy(alpha = (0.75f + 0.25f * alphaRatio).coerceIn(0.45f, 1.0f))
    }
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val barShape = RoundedCornerShape(20.dp)
    // while the bar slides out the selection is already empty: keep the layout it had
    val lastSelectedCount = remember { intArrayOf(selectedCount) }
    if (selectedCount > 0) lastSelectedCount[0] = selectedCount
    val shownCount = if (selectedCount > 0) selectedCount else lastSelectedCount[0]

    Box(
        modifier = modifier
            .fillMaxWidth()
            // same thin size + corner radius as the selection toolbar of the offline lists (glass ON and OFF)
            .height(54.dp)
            .liquidGlassToolbarOr(barShape, isDark) {
                shadow(
                    elevation = 18.dp,
                    shape = barShape,
                    ambientColor = AccentSkyBlue.copy(alpha = 0.35f),
                    spotColor = AccentPink.copy(alpha = 0.40f)
                )
                    .clip(barShape)
                    .background(barBg)
                    .border(width = 1.5.dp, brush = AccentGradient, shape = barShape)
            }
            .padding(horizontal = 0.dp),
        contentAlignment = Alignment.Center
    ) {
        // Items keep a comfortable fixed width and the row scrolls sideways, so more actions can be added later.
        if (shownCount > 1 && onDownload != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$shownCount selected",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onClearSelection != null) {
                        Text(
                            text = "Clear",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onClearSelection)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentGradient)
                            .clickable(onClick = onDownload)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StyledIcon(R.drawable.viaura_download_minimalistic, null, Modifier.size(18.dp), Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Download all", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        } else Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPlayNext != null) {
                QuickActionItem(R.drawable.viaura_next_in_queue, "Play next", AccentSkyBlue, Modifier.width(78.dp), onPlayNext)
            }
            if (onPlayLast != null) {
                QuickActionItem(R.drawable.viaura_last_in_queue, "Play last", AccentMid, Modifier.width(78.dp), onPlayLast)
            }
            QuickActionItem(R.drawable.viaura_playlist_video, "Playlist", AccentPink, Modifier.width(78.dp), onSaveToPlaylist)
            if (onFavourite != null) {
                QuickActionItem(
                    R.drawable.viaura_favourite_heart,
                    if (isFavourite) "Favourited" else "Favourite",
                    if (isFavourite) Color(0xFFFF3366) else primaryText,
                    Modifier.width(78.dp),
                    onFavourite
                )
            }
            QuickActionItem(
                R.drawable.viaura_watch_later,
                if (isSavedForLater) "Remove" else "Watch later",
                Color(0xFFA78BFA),
                Modifier.width(78.dp),
                onWatchLater
            )
            if (onDownload != null) {
                QuickActionItem(R.drawable.viaura_download_minimalistic, "Download", AccentSkyBlue, Modifier.width(78.dp), onDownload)
            }
            if (onNotInterested != null) {
                QuickActionItem(R.drawable.viaura_block, "Not interested", Color(0xFFEF4444), Modifier.width(78.dp), onNotInterested)
            }
            QuickActionItem(R.drawable.viaura_share, "Share", primaryText, Modifier.width(78.dp), onShare)
        }
    }
}

@Composable
private fun QuickActionItem(
    drawableRes: Int,
    label: String,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        StyledIcon(
            drawableRes = drawableRes,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

fun shareOnlineLink(context: Context, title: String, url: String) {
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
        }
        context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {}
}

/** What the swipe toolbar's Download button asks for. */
data class DownloadRequest(val url: String, val title: String, val author: String?, val thumbnail: String?, val audio: Boolean)

/** Shows the download panel (real qualities, sizes, audio tracks, subtitles) for [request]. */
@Composable
fun DownloadRequestHost(request: DownloadRequest?, isDark: Boolean, glassBlurTransparency: Float, onDismiss: () -> Unit) {
    val r = request ?: return
    DownloadOptionsSheet(
        isDark = isDark,
        glassBlurTransparency = glassBlurTransparency,
        url = r.url,
        initialTitle = r.title,
        initialAuthor = r.author,
        initialThumbnail = r.thumbnail,
        startWithAudio = r.audio,
        audioOnly = r.audio, // opened from Online Audio: no Video tab / video section at all
        onDismiss = onDismiss
    )
}
