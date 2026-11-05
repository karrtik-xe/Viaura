package com.example.background

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/* The newer backdrops. Every one of them is built to use the WHOLE screen (top to bottom, edge to edge),
 * never just one corner or band. Same rules as the older ones: pure functions of time, theme colours. */

/** Seven layers of swell stacked from the top of the screen to the bottom, each with a soft fill and a bright crest. */
internal fun DrawScope.drawWaves(t: Float, tilt: Offset, p: BackdropPalette, path: Path) {
  val w = size.width
  val h = size.height
  val layers = 7
  for (layer in 0 until layers) {
    val f = layer / (layers - 1f)
    val baseY = h * (0.08f + 0.86f * f) + tilt.y * 12.dp.toPx() * (0.4f + f)
    val amp = h * (0.016f + 0.022f * f)
    val speed = (0.30f + 0.10f * layer) * (if (layer % 2 == 0) 1f else -1f)
    val shift = tilt.x * 16.dp.toPx() * (0.4f + f)
    path.rewind()
    val steps = 30
    for (s in 0..steps) {
      val u = s.toFloat() / steps
      val x = w * (-0.1f + 1.2f * u) + shift
      val y = baseY + sin(u * TAU * (1.0f + 0.3f * layer) + t * speed + layer * 1.9f) * amp +
        sin(u * TAU * 2.4f - t * speed * 0.6f + layer) * amp * 0.38f
      if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val color = p.accent(layer)
    drawPath(path, color.copy(alpha = 0.40f), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
    val bandBottom = baseY + h * 0.17f
    path.lineTo(w * 1.2f, bandBottom)
    path.lineTo(-w * 0.2f, bandBottom)
    path.close()
    drawPath(
      path,
      Brush.verticalGradient(
        listOf(color.copy(alpha = 0.20f), Color.Transparent),
        startY = baseY - amp,
        endY = bandBottom,
      ),
    )
  }
}

/** A starry sky over the whole screen with shooting stars crossing it from every side, each with a glowing head and a long tail. */
internal fun DrawScope.drawMeteors(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val starColor = lerp(p.ink, p.primary, 0.25f)
  val g1 = Offset(w * 0.2f, h * 0.25f)
  val g2 = Offset(w * 0.85f, h * 0.75f)
  drawCircle(Brush.radialGradient(listOf(p.primary.copy(alpha = 0.14f), Color.Transparent), center = g1, radius = w * 0.9f), radius = w * 0.9f, center = g1)
  drawCircle(Brush.radialGradient(listOf(p.secondary.copy(alpha = 0.12f), Color.Transparent), center = g2, radius = w * 0.9f), radius = w * 0.9f, center = g2)
  for (i in 0 until backdropCount(80, minimum = 44, maximum = 110)) {
    val depth = (i % 3 + 1) / 3f
    val tw = 0.35f + 0.65f * wave01(t * (0.5f + 0.9f * hash01(i, 3)) + i * 2.3f)
    val c = Offset(
      w * hash01(i, 1) + tilt.x * 14.dp.toPx() * depth,
      h * hash01(i, 2) + tilt.y * 14.dp.toPx() * depth,
    )
    drawCircle(starColor.copy(alpha = (0.16f + 0.44f * depth) * tw), radius = (0.5f + 0.9f * depth).dp.toPx(), center = c)
  }
  val dx = -0.55f
  val dy = 0.835f
  val reach = hypot(w, h) * 0.55f
  for (i in 0 until 9) {
    val period = 3.5f + 4f * hash01(i, 11)
    val cyc = t / period + hash01(i, 12)
    val slot = floor(cyc).toInt()
    val u = fract(cyc) / 0.34f
    if (u < 0f || u > 1f) continue
    val seed = i + slot * 7
    val sx = w * (0.15f + 1.00f * hash01(seed, 13))
    val sy = h * (-0.06f + 0.70f * hash01(seed, 14))
    val head = Offset(sx + dx * reach * u, sy + dy * reach * u)
    val tailLen = (110f + 110f * hash01(seed, 15)).dp.toPx()
    val tail = Offset(head.x - dx * tailLen, head.y - dy * tailLen)
    val a = smoothstep(0f, 0.15f, u) * smoothstep(1f, 0.7f, u)
    val color = p.accent(i)
    drawLine(
      brush = Brush.linearGradient(listOf(Color.Transparent, color.copy(alpha = 0.60f * a)), start = tail, end = head),
      start = tail,
      end = head,
      strokeWidth = 2.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawCircle(p.ink.copy(alpha = 0.80f * a), radius = 1.8.dp.toPx(), center = head)
    drawCircle(color.copy(alpha = 0.20f * a), radius = 7.dp.toPx(), center = head)
  }
}

/** Warm sparks rising through the whole screen over a soft ember glow that washes up from the bottom edge. */
internal fun DrawScope.drawEmbers(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val hot = lerp(Color(0xFFFFB04A), p.secondary, 0.15f)
  val cool = lerp(Color(0xFFFF6A3D), p.secondary, 0.25f)
  drawRect(Brush.verticalGradient(listOf(Color.Transparent, cool.copy(alpha = 0.10f), cool.copy(alpha = 0.22f)), startY = 0f, endY = h))
  val floorCenter = Offset(w * 0.5f + tilt.x * 20.dp.toPx(), h * 1.02f)
  drawCircle(
    Brush.radialGradient(listOf(hot.copy(alpha = 0.20f), Color.Transparent), center = floorCenter, radius = w * 1.0f),
    radius = w * 1.0f,
    center = floorCenter,
  )
  for (i in 0 until backdropCount(70, minimum = 38, maximum = 96)) {
    val depth = 0.4f + 0.6f * hash01(i, 1)
    val u = fract(hash01(i, 2) + t * (0.030f + 0.05f * depth))
    val x = w * hash01(i, 3) + sin(u * TAU * 2f + i) * 20.dp.toPx() * depth + tilt.x * 18.dp.toPx() * depth
    val y = h * (1.05f - 1.13f * u) + tilt.y * 10.dp.toPx() * depth
    val flicker = 0.7f + 0.3f * sin(t * (3f + 4f * hash01(i, 4)) + i)
    val a = smoothstep(0f, 0.06f, u) * smoothstep(1f, 0.55f, u) * flicker
    val r = (1.0f + 1.9f * depth * (1f - 0.6f * u)).dp.toPx()
    val c = Offset(x, y)
    drawCircle(lerp(hot, cool, u).copy(alpha = 0.90f * a), radius = r, center = c)
    drawCircle(cool.copy(alpha = 0.17f * a), radius = r * 4.4f, center = c)
  }
  for (i in 0 until 26) {
    val c = Offset(w * hash01(i, 41) + tilt.x * 8.dp.toPx(), h * hash01(i, 42) + tilt.y * 8.dp.toPx())
    val tw = wave01(t * (0.6f + hash01(i, 43)) + i * 1.7f)
    drawCircle(hot.copy(alpha = 0.08f + 0.22f * tw), radius = 0.9.dp.toPx(), center = c)
  }
}

private fun Path.hex(cx: Float, cy: Float, r: Float) {
  rewind()
  for (k in 0 until 6) {
    val a = k * TAU / 6f
    val x = cx + r * cos(a)
    val y = cy + r * sin(a)
    if (k == 0) moveTo(x, y) else lineTo(x, y)
  }
  close()
}

/** A honeycomb covering the screen; three ripples travelling from different spots light up the cells as they pass. */
internal fun DrawScope.drawHexagons(t: Float, tilt: Offset, p: BackdropPalette, path: Path) {
  val w = size.width
  val h = size.height
  val r = 26.dp.toPx()
  val dx = 1.5f * r
  val dy = 1.7320508f * r
  val cols = (w / dx).toInt() + 3
  val rows = (h / dy).toInt() + 3
  val norm = min(w, h)
  val sx = floatArrayOf(w * 0.15f, w * 0.85f, w * 0.30f)
  val sy = floatArrayOf(h * 0.15f, h * 0.50f, h * 0.88f)
  for (c in -1 until cols) {
    for (row in -1 until rows) {
      val cx = c * dx + tilt.x * 10.dp.toPx()
      val cy = row * dy + (if ((c and 1) == 1) dy / 2f else 0f) + tilt.y * 10.dp.toPx()
      var glow = 0f
      for (s in 0 until 3) {
        val d = hypot(cx - sx[s], cy - sy[s]) / norm
        glow = max(glow, smoothstep(0.62f, 1f, wave01(d * 6f - t * 1.0f + s * 2.1f)))
      }
      val color = lerp(p.primary, p.secondary, (cy / h).coerceIn(0f, 1f))
      path.hex(cx, cy, r * 0.92f)
      if (glow > 0.02f) drawPath(path, color.copy(alpha = 0.10f * glow))
      drawPath(path, color.copy(alpha = 0.06f + 0.26f * glow), style = Stroke(width = 1.dp.toPx()))
    }
  }
}

/**
 * A daytime sky: a tinted wash over the whole screen, a warm sun glow and three bands of solid, flat clouds drifting at
 * different speeds from the top of the screen to the bottom. Clouds are white over the wash on light themes (so they stand
 * out on a white page) and soft light shapes on dark themes.
 */
internal fun DrawScope.drawClouds(t: Float, tilt: Offset, p: BackdropPalette, path: Path) {
  val w = size.width
  val h = size.height
  val dark = p.isDark
  drawRect(
    Brush.verticalGradient(
      listOf(
        p.primary.copy(alpha = if (dark) 0.18f else 0.34f),
        p.secondary.copy(alpha = if (dark) 0.06f else 0.14f),
      ),
    ),
  )
  val sun = Offset(w * 0.82f + tilt.x * 10.dp.toPx(), h * 0.10f + tilt.y * 8.dp.toPx())
  drawCircle(
    Brush.radialGradient(listOf(Color(0xFFFFE08A).copy(alpha = if (dark) 0.12f else 0.50f), Color.Transparent), center = sun, radius = w * 0.75f),
    radius = w * 0.75f,
    center = sun,
  )
  val count = 10
  for (i in 0 until count) {
    val band = i % 3
    val depth = (band + 1) / 3f
    val cy = h * ((i + 0.5f + (hash01(i, 3) - 0.5f) * 0.5f) / count) + tilt.y * 14.dp.toPx() * depth
    val f = fract(hash01(i, 2) + t * (0.004f + 0.009f * depth))
    val cx = w * (f * 1.8f - 0.4f) + tilt.x * 24.dp.toPx() * depth
    val s = (20f + 26f * depth).dp.toPx()
    val edge = smoothstep(0f, 0.10f, f) * smoothstep(1f, 0.90f, f)
    path.rewind()
    path.addRoundRect(RoundRect(Rect(cx - 2.2f * s, cy, cx + 2.2f * s, cy + 0.9f * s), CornerRadius(0.45f * s, 0.45f * s)))
    path.addOval(Rect(Offset(cx - 1.35f * s, cy - 0.75f * s), Size(1.5f * s, 1.5f * s)))
    path.addOval(Rect(Offset(cx - 0.25f * s, cy - 1.20f * s), Size(1.9f * s, 1.9f * s)))
    path.addOval(Rect(Offset(cx + 0.75f * s, cy - 0.55f * s), Size(1.35f * s, 1.35f * s)))
    val color = if (dark) p.ink.copy(alpha = (0.07f + 0.08f * depth) * edge) else Color.White.copy(alpha = (0.42f + 0.32f * depth) * edge)
    drawPath(path, color)
  }
}

private val LeafShape: Path by lazy {
  Path().apply {
    moveTo(0f, -1f)
    cubicTo(0.95f, -0.45f, 0.70f, 0.60f, 0f, 1f)
    cubicTo(-0.70f, 0.60f, -0.95f, -0.45f, 0f, -1f)
    close()
  }
}

private val LeafColors = arrayOf(Color(0xFFE07B39), Color(0xFFC8553D), Color(0xFFE9B44C), Color(0xFFA9692B))

/** Autumn leaves swaying down the whole screen over a faint warm haze. */
internal fun DrawScope.drawLeaves(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  drawRect(Brush.verticalGradient(listOf(Color(0xFFE9B44C).copy(alpha = 0.07f), Color(0xFFC8553D).copy(alpha = 0.10f))))
  for (i in 0 until backdropCount(30, minimum = 18, maximum = 42)) {
    val depth = 0.45f + 0.55f * hash01(i, 6)
    val u = fract(hash01(i, 1) + t * (0.018f + 0.024f * hash01(i, 2)))
    val x = w * (hash01(i, 3) * 1.1f - 0.05f) + sin(u * TAU * 2.2f + i) * 34.dp.toPx() * depth + tilt.x * 24.dp.toPx() * depth
    val y = h * (u * 1.14f - 0.07f) + tilt.y * 14.dp.toPx() * depth
    val spin = sin(t * 0.7f + i) * 50f + t * 18f * (if (i % 2 == 0) 1f else -1f) + hash01(i, 5) * 360f
    val flip = cos(t * (0.7f + 0.5f * hash01(i, 7)) + i)
    val s = (7f + 7f * depth).dp.toPx()
    val a = 0.78f * smoothstep(0f, 0.06f, u) * smoothstep(1f, 0.94f, u)
    val color = lerp(LeafColors[i % LeafColors.size], p.secondary, 0.08f)
    translate(left = x, top = y) {
      rotate(spin, pivot = Offset.Zero) {
        scale(scaleX = (0.35f + 0.65f * abs(flip)) * s, scaleY = s, pivot = Offset.Zero) {
          drawPath(LeafShape, color.copy(alpha = a))
          drawLine(p.ink.copy(alpha = a * 0.22f), Offset(0f, -0.9f), Offset(0f, 1f), strokeWidth = 0.7.dp.toPx() / s)
        }
      }
    }
  }
}

/** A face-on spiral galaxy whose three arms sweep across the whole screen, turning faster near the bright core. */
internal fun DrawScope.drawGalaxy(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val center = Offset(w * 0.5f + tilt.x * 18.dp.toPx(), h * 0.5f + tilt.y * 12.dp.toPx())
  val rx = w * 0.66f
  val ry = h * 0.62f
  val glowR = hypot(w, h) * 0.5f
  drawCircle(
    Brush.radialGradient(listOf(p.tertiary.copy(alpha = 0.30f), p.primary.copy(alpha = 0.10f), Color.Transparent), center = center, radius = glowR),
    radius = glowR,
    center = center,
  )
  for (i in 0 until backdropCount(210, minimum = 110, maximum = 280)) {
    val arm = i % 3
    val rn = sqrt(hash01(i, 1)) * 0.97f + 0.03f
    val spin = t * 0.10f / (0.35f + rn)
    val ang = arm * TAU / 3f + rn * 5.0f + spin + (hash01(i, 2) - 0.5f) * 0.6f * (0.4f + rn)
    val x = center.x + cos(ang) * rn * rx
    val y = center.y + sin(ang) * rn * ry
    val tw = 0.6f + 0.4f * sin(t * (0.5f + 0.9f * hash01(i, 3)) + i)
    val color = lerp(p.tertiary, p.primary, rn)
    val a = (0.22f + 0.50f * (1f - rn)) * tw
    drawCircle(lerp(color, p.ink, 0.35f * (1f - rn)).copy(alpha = a), radius = (0.6f + 1.1f * (1f - rn) + 0.6f * hash01(i, 4)).dp.toPx(), center = Offset(x, y))
  }
  for (i in 0 until 40) {
    val c = Offset(w * hash01(i, 31), h * hash01(i, 32))
    drawCircle(p.ink.copy(alpha = 0.10f + 0.28f * wave01(t * 0.6f + i * 1.9f)), radius = 0.8.dp.toPx(), center = c)
  }
  drawCircle(p.ink.copy(alpha = 0.60f), radius = 2.4.dp.toPx(), center = center)
}

/** A radar scope that fills the screen: six rings, crosshair, a sweeping beam and blips that light up as it passes. */
internal fun DrawScope.drawRadar(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val center = Offset(w * 0.5f + tilt.x * 14.dp.toPx(), h * 0.5f + tilt.y * 14.dp.toPx())
  val rMax = hypot(w, h) * 0.5f
  val line = lerp(p.ink, p.primary, 0.5f)
  for (k in 1..6) {
    drawCircle(line.copy(alpha = 0.13f), radius = rMax * k / 6f, center = center, style = Stroke(width = 1.dp.toPx()))
  }
  drawLine(line.copy(alpha = 0.11f), Offset(0f, center.y), Offset(w, center.y), strokeWidth = 1.dp.toPx())
  drawLine(line.copy(alpha = 0.11f), Offset(center.x, 0f), Offset(center.x, h), strokeWidth = 1.dp.toPx())
  drawLine(line.copy(alpha = 0.07f), Offset(center.x - rMax * 0.7f, center.y - rMax * 0.7f), Offset(center.x + rMax * 0.7f, center.y + rMax * 0.7f), strokeWidth = 1.dp.toPx())
  drawLine(line.copy(alpha = 0.07f), Offset(center.x - rMax * 0.7f, center.y + rMax * 0.7f), Offset(center.x + rMax * 0.7f, center.y - rMax * 0.7f), strokeWidth = 1.dp.toPx())
  val sweep = t * 0.8f
  val topLeft = Offset(center.x - rMax, center.y - rMax)
  val arcSize = Size(rMax * 2f, rMax * 2f)
  for (k in 0 until 40) {
    val f = 1f - k / 40f
    drawArc(
      color = p.primary.copy(alpha = 0.24f * f * f),
      startAngle = sweep * 57.29578f - k * 2f,
      sweepAngle = 2.4f,
      useCenter = true,
      topLeft = topLeft,
      size = arcSize,
    )
  }
  drawLine(p.primary.copy(alpha = 0.50f), center, Offset(center.x + cos(sweep) * rMax, center.y + sin(sweep) * rMax), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
  for (i in 0 until 12) {
    val ang = hash01(i, 21) * TAU
    val r = rMax * (0.18f + 0.78f * hash01(i, 22))
    val ph = fract((sweep - ang) / TAU)
    val a = (1f - ph) * (1f - ph)
    val c = Offset(center.x + cos(ang) * r, center.y + sin(ang) * r)
    val color = p.accent(i + 1)
    drawCircle(color.copy(alpha = 0.80f * a), radius = 2.8.dp.toPx(), center = c)
    drawCircle(color.copy(alpha = 0.42f * a), radius = (4f + 16f * ph).dp.toPx(), center = c, style = Stroke(width = 1.dp.toPx()))
  }
}

/** Music bars growing from the bottom edge AND hanging from the top edge, bouncing out of step across the full width. */
internal fun DrawScope.drawEqualizer(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val n = backdropCount(26, minimum = 16, maximum = 34)
  val gap = 4.dp.toPx()
  val barW = (w - gap * (n + 1)) / n
  for (i in 0 until n) {
    val u = i.toFloat() / (n - 1).coerceAtLeast(1)
    val v = 0.30f +
      0.32f * wave01(t * (1.5f + 0.9f * hash01(i, 1)) + i * 0.8f) +
      0.28f * wave01(t * (0.8f + 0.6f * hash01(i, 2)) - i * 1.3f + hash01(i, 3) * TAU) +
      0.10f * wave01(t * 2.7f + i * 2.1f)
    val vTop = 0.30f +
      0.32f * wave01(t * (1.2f + 0.8f * hash01(i, 4)) - i * 0.9f) +
      0.28f * wave01(t * (0.7f + 0.5f * hash01(i, 5)) + i * 1.1f + hash01(i, 6) * TAU)
    val x = gap + i * (barW + gap) + tilt.x * 8.dp.toPx()
    val color = lerp(p.primary, p.secondary, u)
    val bh = (h * 0.50f * v).coerceAtLeast(0.06f * h)
    val y = h - bh + tilt.y * 6.dp.toPx()
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.04f)), startY = y, endY = h),
      topLeft = Offset(x, y),
      size = Size(barW, bh + 6.dp.toPx()),
      cornerRadius = CornerRadius(barW / 2f, barW / 2f),
    )
    drawRoundRect(
      color = color.copy(alpha = 0.45f),
      topLeft = Offset(x, y - 7.dp.toPx() - h * 0.015f * wave01(t * 1.2f + i)),
      size = Size(barW, 2.5.dp.toPx()),
      cornerRadius = CornerRadius(1.25.dp.toPx(), 1.25.dp.toPx()),
    )
    val th = (h * 0.34f * vTop).coerceAtLeast(0.04f * h)
    drawRoundRect(
      brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.04f), color.copy(alpha = 0.26f)), startY = 0f, endY = th),
      topLeft = Offset(x, -6.dp.toPx()),
      size = Size(barW, th + 6.dp.toPx()),
      cornerRadius = CornerRadius(barW / 2f, barW / 2f),
    )
  }
}

/** Four atom orbits big enough to cross the whole screen, turning slowly, with glowing electrons and fading trails. */
internal fun DrawScope.drawOrbits(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val center = Offset(w * 0.5f + tilt.x * 16.dp.toPx(), h * 0.5f + tilt.y * 12.dp.toPx())
  val rx = hypot(w, h) * 0.52f
  val ry = rx * 0.40f
  for (i in 0 until 36) {
    val c = Offset(w * hash01(i, 51), h * hash01(i, 52))
    drawCircle(p.ink.copy(alpha = 0.08f + 0.24f * wave01(t * 0.7f + i * 2.3f)), radius = 0.8.dp.toPx(), center = c)
  }
  drawCircle(
    Brush.radialGradient(listOf(p.tertiary.copy(alpha = 0.32f), Color.Transparent), center = center, radius = min(w, h) * 0.30f),
    radius = min(w, h) * 0.30f,
    center = center,
  )
  drawCircle(p.ink.copy(alpha = 0.60f), radius = 3.2.dp.toPx(), center = center)
  for (o in 0 until 4) {
    val thetaDeg = o * 45f + t * 3f
    val theta = thetaDeg * 0.017453292f
    val color = p.accent(o)
    rotate(thetaDeg, pivot = center) {
      drawOval(
        color = color.copy(alpha = 0.24f),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
        style = Stroke(width = 1.2.dp.toPx()),
      )
    }
    for (e in 0 until 2) {
      val speed = (0.45f + 0.15f * o) * (if (e == 0) 1f else -0.8f)
      for (k in 0 until 7) {
        val ang = t * speed + e * 3.1416f + o * 1.3f - k * 0.08f * (if (speed > 0f) 1f else -1f)
        val lx = rx * cos(ang)
        val ly = ry * sin(ang)
        val x = center.x + lx * cos(theta) - ly * sin(theta)
        val y = center.y + lx * sin(theta) + ly * cos(theta)
        val f = 1f - k / 7f
        if (k == 0) {
          drawCircle(color.copy(alpha = 0.18f), radius = 10.dp.toPx(), center = Offset(x, y))
          drawCircle(color.copy(alpha = 0.90f), radius = 3.dp.toPx(), center = Offset(x, y))
        } else {
          drawCircle(color.copy(alpha = 0.30f * f), radius = (2.6f * f).dp.toPx(), center = Offset(x, y))
        }
      }
    }
  }
}

/** Hyperspace: stars stream out of the middle and stretch into streaks that sweep past every edge of the screen. */
internal fun DrawScope.drawWarp(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val center = Offset(w * 0.5f + tilt.x * 24.dp.toPx(), h * 0.5f + tilt.y * 24.dp.toPx())
  val maxR = hypot(w, h) * 0.58f
  drawCircle(
    Brush.radialGradient(listOf(p.primary.copy(alpha = 0.22f), p.tertiary.copy(alpha = 0.06f), Color.Transparent), center = center, radius = maxR * 0.7f),
    radius = maxR * 0.7f,
    center = center,
  )
  for (i in 0 until backdropCount(150, minimum = 80, maximum = 200)) {
    val ang = hash01(i, 1) * TAU
    val speed = 0.10f + 0.14f * hash01(i, 3)
    val u = fract(hash01(i, 2) + t * speed)
    val r1 = u * u * maxR
    val u0 = (u - 0.06f).coerceAtLeast(0f)
    val r0 = u0 * u0 * maxR
    val dirX = cos(ang)
    val dirY = sin(ang)
    val a = smoothstep(0f, 0.12f, u) * (0.22f + 0.60f * u)
    val color = lerp(p.ink, p.accent(i), 0.45f)
    drawLine(
      color = color.copy(alpha = a),
      start = Offset(center.x + dirX * r0, center.y + dirY * r0),
      end = Offset(center.x + dirX * r1, center.y + dirY * r1),
      strokeWidth = (0.6f + 1.8f * u).dp.toPx(),
      cap = StrokeCap.Round,
    )
  }
}

/** Twenty silky wind lines running across the full height of the screen, each with a bright pulse gliding along it. */
internal fun DrawScope.drawFlow(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val lines = 20
  val steps = 26
  for (i in 0 until lines) {
    val y0 = h * (i + 0.5f) / lines + tilt.y * 10.dp.toPx()
    val color = p.accent(i)
    val speed = 0.05f + 0.07f * hash01(i, 1)
    val head = fract(t * speed + hash01(i, 2))
    val dir = if (i % 2 == 0) 1f else -1f
    var px = 0f
    var py = 0f
    for (s in 0..steps) {
      val u = s.toFloat() / steps
      val x = w * (-0.05f + 1.1f * u) + tilt.x * 10.dp.toPx()
      val y = y0 + sin(u * TAU * 1.2f + t * 0.5f * dir + i * 0.7f) * h * 0.030f +
        sin(u * TAU * 2.7f - t * 0.7f + i) * h * 0.011f
      if (s > 0) {
        val d = abs(fract(u - head + 0.5f) - 0.5f)
        val g = smoothstep(0.20f, 0f, d)
        drawLine(color.copy(alpha = 0.08f + 0.40f * g), Offset(px, py), Offset(x, y), strokeWidth = (1f + 1.4f * g).dp.toPx(), cap = StrokeCap.Round)
      }
      px = x
      py = y
    }
  }
}

/** Translucent glass triangles and diamonds floating up from the bottom of the screen to the top, turning as they rise. */
internal fun DrawScope.drawPrism(t: Float, tilt: Offset, p: BackdropPalette, path: Path) {
  val w = size.width
  val h = size.height
  for (i in 0 until backdropCount(18, minimum = 11, maximum = 24)) {
    val depth = 0.4f + 0.6f * hash01(i, 6)
    val u = fract(hash01(i, 2) + t * (0.010f + 0.016f * hash01(i, 5)))
    val cy = h * (1.12f - 1.24f * u) + tilt.y * 14.dp.toPx() * depth
    val cx = w * hash01(i, 1) + sin(t * 0.2f + i) * 22.dp.toPx() + tilt.x * 20.dp.toPx() * depth
    val s = (26f + 52f * hash01(i, 3)).dp.toPx() * (0.7f + 0.3f * depth)
    val rot = (t * (6f + 12f * hash01(i, 4)) * (if (i % 2 == 0) 1f else -1f) + hash01(i, 7) * 360f)
    val edge = smoothstep(0f, 0.08f, u) * smoothstep(1f, 0.92f, u)
    val sides = if (i % 3 == 0) 4 else 3
    val color = p.accent(i)
    val color2 = p.accent(i + 1)
    path.rewind()
    for (k in 0 until sides) {
      val a = (k * TAU / sides) - TAU / 4f
      val x = cx + s * cos(a)
      val y = cy + s * sin(a)
      if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    rotate(rot, pivot = Offset(cx, cy)) {
      drawPath(
        path,
        Brush.linearGradient(
          listOf(color.copy(alpha = 0.20f * edge), color2.copy(alpha = 0.04f * edge)),
          start = Offset(cx - s, cy - s),
          end = Offset(cx + s, cy + s),
        ),
      )
      drawPath(path, color.copy(alpha = 0.34f * edge), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
    }
  }
}

/** Glowing sky lanterns drifting up through the whole screen, swaying and flickering as they climb. */
internal fun DrawScope.drawLanterns(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val warm = Color(0xFFFF9A3C)
  val flame = Color(0xFFFFE08A)
  drawRect(Brush.verticalGradient(listOf(Color.Transparent, warm.copy(alpha = 0.07f))))
  for (i in 0 until backdropCount(16, minimum = 10, maximum = 22)) {
    val depth = 0.5f + 0.5f * hash01(i, 6)
    val u = fract(hash01(i, 2) + t * (0.016f + 0.020f * depth))
    val y = h * (1.12f - 1.30f * u) + tilt.y * 10.dp.toPx() * depth
    val x = w * (0.05f + 0.90f * hash01(i, 1)) + sin(u * TAU * 1.5f + i) * 18.dp.toPx() * depth + tilt.x * 20.dp.toPx() * depth
    val s = (9f + 10f * depth).dp.toPx()
    val a = smoothstep(0f, 0.08f, u) * smoothstep(1f, 0.90f, u)
    val flick = 0.85f + 0.15f * sin(t * (4f + 3f * hash01(i, 4)) + i)
    val c = Offset(x, y)
    drawCircle(
      Brush.radialGradient(listOf(warm.copy(alpha = 0.32f * a * flick), Color.Transparent), center = c, radius = s * 3.2f),
      radius = s * 3.2f,
      center = c,
    )
    drawRoundRect(
      color = warm.copy(alpha = 0.85f * a),
      topLeft = Offset(x - s * 0.55f, y - s * 0.70f),
      size = Size(s * 1.10f, s * 1.40f),
      cornerRadius = CornerRadius(s * 0.35f, s * 0.35f),
    )
    drawRoundRect(
      color = p.ink.copy(alpha = 0.20f * a),
      topLeft = Offset(x - s * 0.40f, y - s * 0.78f),
      size = Size(s * 0.80f, s * 0.14f),
      cornerRadius = CornerRadius(s * 0.07f, s * 0.07f),
    )
    drawCircle(flame.copy(alpha = 0.90f * a * flick), radius = s * 0.26f, center = Offset(x, y + s * 0.15f))
  }
}

private val ConfettiExtra = arrayOf(Color(0xFFFFC857), Color(0xFF4ADE80), Color(0xFFF472B6))

/** Colourful confetti tumbling down over the entire screen, each piece spinning and flipping as it falls. */
internal fun DrawScope.drawConfetti(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  for (i in 0 until backdropCount(70, minimum = 40, maximum = 100)) {
    val depth = 0.5f + 0.5f * hash01(i, 6)
    val u = fract(hash01(i, 2) + t * (0.05f + 0.07f * hash01(i, 3)))
    val x = w * hash01(i, 1) + sin(u * TAU * 2f + i) * 20.dp.toPx() * depth + tilt.x * 18.dp.toPx() * depth
    val y = h * (u * 1.14f - 0.07f) + tilt.y * 10.dp.toPx() * depth
    val rot = (t * (90f + 120f * hash01(i, 4)) + hash01(i, 5) * 360f) * (if (i % 2 == 0) 1f else -1f)
    val flip = abs(cos(t * (1.5f + hash01(i, 7)) + i))
    val s = (4f + 4f * depth).dp.toPx()
    val color = when (i % 6) {
      0 -> p.primary
      1 -> p.secondary
      2 -> p.tertiary
      else -> ConfettiExtra[i % 3]
    }
    val a = 0.80f * smoothstep(0f, 0.05f, u) * smoothstep(1f, 0.95f, u)
    rotate(rot, pivot = Offset(x, y)) {
      drawRect(
        color = color.copy(alpha = a),
        topLeft = Offset(x - s, y - s * 0.5f * max(flip, 0.15f)),
        size = Size(s * 2f, s * max(flip, 0.15f)),
      )
    }
  }
}
