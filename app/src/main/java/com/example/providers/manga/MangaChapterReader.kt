package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val mangaClient: OkHttpClient by lazy {
    OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

// Pages that were read stay in memory (a few of them), so scrolling back up never downloads them again.
private class PageCache(maxBytes: Int) : LruCache<String, List<Bitmap>>(maxBytes) {
    override fun sizeOf(key: String, value: List<Bitmap>): Int = value.sumOf { it.byteCount }.coerceAtLeast(1)
}

private val pageCache = PageCache((Runtime.getRuntime().maxMemory() / 6L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

// A tall webtoon strip can be larger than the GPU texture limit: it is cut into slices that are shown one under the other.
private const val SLICE_HEIGHT_PX = 2400

private sealed interface PageState {
    object Loading : PageState
    object Failed : PageState
    class Ready(val slices: List<Bitmap>) : PageState
}

private fun fetchPage(page: MediaStream, targetWidth: Int): List<Bitmap>? {
    return try {
        val b = Request.Builder().url(page.link)
        if (page.headers.keys.none { it.equals("User-Agent", true) }) b.header("User-Agent", MediaPlayback.STREAM_UA)
        for ((k, v) in page.headers) try { b.header(k, v) } catch (_: Throwable) {}
        mangaClient.newCall(b.build()).execute().use { r ->
            val bytes = if (r.isSuccessful) r.body?.bytes() else null
            if (bytes == null) {
                null
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                if (bmp == null) {
                    null
                } else if (bmp.height <= SLICE_HEIGHT_PX * 3 / 2) {
                    listOf(bmp)
                } else {
                    val out = ArrayList<Bitmap>()
                    var y = 0
                    while (y < bmp.height) {
                        val h = minOf(SLICE_HEIGHT_PX, bmp.height - y)
                        out.add(Bitmap.createBitmap(bmp, 0, y, bmp.width, h))
                        y += h
                    }
                    bmp.recycle()
                    out
                }
            }
        }
    } catch (_: Throwable) {
        null
    }
}

private suspend fun loadPage(page: MediaStream, targetWidth: Int): List<Bitmap>? {
    pageCache.get(page.link)?.let { return it }
    repeat(3) { attempt ->
        val slices = withContext(Dispatchers.IO) { fetchPage(page, targetWidth) }
        if (slices != null) {
            pageCache.put(page.link, slices)
            return slices
        }
        delay(700L * (attempt + 1))
    }
    return null
}

/** Reader for manga / comic chapters: portrait, pages one under the other, pinch to zoom. */
@Composable
fun MangaReaderScreen(title: String, pages: List<MediaStream>, onClose: () -> Unit) {
    val context = LocalContext.current
    // portrait while reading
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { if (activity != null && previous != null) activity.requestedOrientation = previous }
    }
    var scale by remember { mutableStateOf(1f) }
    var offX by remember { mutableStateOf(0f) }
    var barVisible by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val current by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
    val widthPx = context.resources.displayMetrics.widthPixels
    // the next pages are fetched while the current one is read, so scrolling down never waits
    LaunchedEffect(current, pages) {
        for (i in current until minOf(current + 3, pages.size)) {
            val p = pages[i]
            if (pageCache.get(p.link) == null) launch { loadPage(p, widthPx) }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val ev = awaitPointerEvent()
                                if (ev.changes.size >= 2) {
                                    scale = (scale * ev.calculateZoom()).coerceIn(1f, 4f)
                                    val limit = size.width * (scale - 1f) / 2f
                                    offX = (offX + ev.calculatePan().x).coerceIn(-limit, limit)
                                    ev.changes.forEach { it.consume() }
                                }
                            } while (ev.changes.any { it.pressed })
                        }
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offX }
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {
                        barVisible = !barVisible
                    }
            ) {
                itemsIndexed(pages, key = { i, p -> "$i|${p.link}" }) { _, page ->
                    var attempt by remember(page.link) { mutableIntStateOf(0) }
                    val state by produceState<PageState>(PageState.Loading, page.link, attempt) {
                        value = PageState.Loading
                        val slices = loadPage(page, widthPx)
                        value = if (slices != null) PageState.Ready(slices) else PageState.Failed
                    }
                    when (val st = state) {
                        is PageState.Ready -> Column(Modifier.fillMaxWidth()) {
                            st.slices.forEach { slice ->
                                Image(
                                    bitmap = slice.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                        PageState.Failed -> Box(
                            Modifier.fillMaxWidth().height(260.dp).clickable { attempt++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Could not load this page. Tap to retry.", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                        PageState.Loading -> Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            if (barVisible) {
                Row(
                    Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    Text("$current / ${pages.size}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    Text("✕", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable { onClose() }.padding(6.dp))
                }
            }
        }
    }
}
