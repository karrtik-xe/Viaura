package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isAppInDarkTheme

private const val CELLS_X = 16
private const val CELLS_Y = 8
private const val CELL_PX = 14

private fun buildCheckerBitmap(isDark: Boolean): Bitmap {
    val colors = if (isDark) {
        intArrayOf(0xFF1D4ED8.toInt(), 0xFFDB2777.toInt(), 0xFF0F766E.toInt(), 0xFFD97706.toInt(), 0xFF7C3AED.toInt())
    } else {
        intArrayOf(0xFF93C5FD.toInt(), 0xFFF9A8D4.toInt(), 0xFF86EFAC.toInt(), 0xFFFDE68A.toInt(), 0xFFC4B5FD.toInt())
    }
    val bmp = Bitmap.createBitmap(CELLS_X * CELL_PX, CELLS_Y * CELL_PX, Bitmap.Config.ARGB_8888)
    val c = AndroidCanvas(bmp)
    val p = AndroidPaint()
    for (y in 0 until CELLS_Y) for (x in 0 until CELLS_X) {
        p.color = colors[(x * 3 + y * 2 + (x * y) % 3) % colors.size]
        c.drawRect((x * CELL_PX).toFloat(), (y * CELL_PX).toFloat(), ((x + 1) * CELL_PX).toFloat(), ((y + 1) * CELL_PX).toFloat(), p)
    }
    return bmp
}

/**
 * Live preview for the Liquid Glass sliders (Settings > Interface Appearance). A coloured checker board stands in for
 * the video; the glass card on top goes through exactly the same pipeline as the real player controls
 * (blur -> saturation -> lens refraction -> tint -> sheen -> highlight border) and reads the same slider values,
 * so what you see here is what the controls look like.
 */
@Composable
fun LiquidGlassPreview(modifier: Modifier = Modifier) {
    val isDark = isAppInDarkTheme()
    val params = LocalLiquidGlassParams.current
    // The checker board is drawn off the main thread: building it inside composition froze the page while it opened
    val imageState = androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, isDark) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { buildCheckerBitmap(isDark).asImageBitmap() }
    }
    val image = imageState.value
    if (image == null) {
        Box(modifier = modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(24.dp)))
        return
    }
    val source = remember { VideoGlassSource() }
    source.frame = image

    val cardShape = RoundedCornerShape(30.dp)
    val fill = liquidGlassPlayerFill(isDark, 85f, params.tint)
    val textColor = if (isDark) Color.White else Color(0xFF111827)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .onGloballyPositioned { source.bounds = it.boundsInWindow() }
    ) {
        // the "video": crisp checker board
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }

        CompositionLocalProvider(LocalVideoGlassSource provides source) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .clip(cardShape)
                    .videoGlassBackdrop(cardShape)
                    .background(fill)
                    .background(liquidGlassSheenBrush(isDark), cardShape)
                    .liquidGlassBorder(cardShape, isDark, 1.2.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                    Text("Viaura", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val circle = CircleShape
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(circle)
                                .videoGlassBackdrop(circle)
                                .background(fill)
                                .background(liquidGlassSheenBrush(isDark), circle)
                                .liquidGlassBorder(circle, isDark, 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = textColor, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            val pill = CircleShape
                            val ringColors = glassGradientColors()
                            Box(
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(30.dp)
                                    .clip(pill)
                                    .videoGlassBackdrop(pill)
                                    .background(fill)
                                    .background(liquidGlassSheenBrush(isDark), pill)
                                    .liquidGlassBorder(pill, isDark, 1.dp)
                                    .border(1.5.dp, Brush.horizontalGradient(ringColors), pill),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                // glass knob (ON): theme gradient tint + white rim, like the other glass buttons
                                Box(
                                    modifier = Modifier
                                        .padding(end = 3.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(ringColors))
                                        .background(liquidGlassSheenBrush(isDark), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.75f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
