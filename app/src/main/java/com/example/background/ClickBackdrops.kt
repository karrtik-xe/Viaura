package com.example.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Five more click-effect backdrops ("NEW" section of the picker). Same rules as the older click effects
 * (FreshBackdrops.kt): pure functions of time, theme colours and the recent touches in [BackdropDrawingState]. */

private const val SHOCK_LIFE = 1.9f
private const val PETAL_LIFE = 2.2f
private const val PIXEL_LIFE = 1.4f
private const val CRYSTAL_LIFE = 1.8f
private const val VORTEX_LIFE = 2.4f

private fun easeOut(k: Float): Float = 1f - (1f - k) * (1f - k) * (1f - k)

/** Soft breathing glow orbs shared by the calm bases below. */
private fun DrawScope.glowOrbs(t: Float, tilt: Offset, p: BackdropPalette, count: Int, alpha: Float) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  for (i in 0 until count) {
    val c = Offset(
      w * (0.5f + 0.36f * sin(t * (0.06f + 0.03f * hash01(i, 11)) + i * 2.3f)) + tilt.x * 12.dp.toPx(),
      h * (0.5f + 0.36f * sin(t * (0.05f + 0.03f * hash01(i, 12)) + i * 1.7f)) + tilt.y * 12.dp.toPx(),
    )
    val r = base * (0.5f + 0.25f * hash01(i, 13))
    drawCircle(Brush.radialGradient(listOf(p.accent(i).copy(alpha = alpha), Color.Transparent), center = c, radius = r), radius = r, center = c)
  }
}

/** Slow concentric rings in the dark. A touch sends a double shockwave that bends a bright ring outward. */
internal fun DrawScope.drawShockwave(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  glowOrbs(t, tilt, p, 3, 0.16f)
  val centre = Offset(w * 0.5f + tilt.x * 10.dp.toPx(), h * 0.5f + tilt.y * 10.dp.toPx())
  for (i in 0 until 5) {
    val u = fract(t * 0.05f + i / 5f)
    drawCircle(p.accent(i).copy(alpha = 0.16f * (1f - u)), radius = base * (0.1f + 0.8f * u), center = centre, style = Stroke(width = 1.2.dp.toPx()))
  }
  forEachBurst(drawing, t, SHOCK_LIFE) { x, y, age, seed ->
    val c = Offset(x, y)
    val k = age / SHOCK_LIFE
    val fade = (1f - k) * (1f - k)
    for (ring in 0 until 2) {
      val kk = (k - ring * 0.14f).coerceIn(0f, 1f)
      if (kk <= 0f) continue
      val r = base * 0.95f * easeOut(kk)
      val col = p.accent(seed + ring)
      drawCircle(col.copy(alpha = 0.50f * fade), radius = r, center = c, style = Stroke(width = (3.2f - 2f * kk).dp.toPx()))
      drawCircle(Brush.radialGradient(listOf(Color.Transparent, col.copy(alpha = 0.14f * fade), Color.Transparent), center = c, radius = r + 22.dp.toPx()), radius = r + 22.dp.toPx(), center = c)
    }
    val flash = 1f - smoothstep(0f, 0.2f, k)
    if (flash > 0f) drawCircle(p.ink.copy(alpha = 0.55f * flash), radius = 14.dp.toPx() * (0.5f + k * 4f), center = c)
  }
}

private fun DrawScope.drawPetal(path: Path, c: Offset, len: Float, angle: Float, color: Color) {
  val ca = cos(angle)
  val sa = sin(angle)
  val tip = Offset(c.x + ca * len, c.y + sa * len)
  val mid = len * 0.5f
  val side = len * 0.32f
  val c1 = Offset(c.x + ca * mid - sa * side, c.y + sa * mid + ca * side)
  val c2 = Offset(c.x + ca * mid + sa * side, c.y + sa * mid - ca * side)
  path.rewind()
  path.moveTo(c.x, c.y)
  path.quadraticBezierTo(c1.x, c1.y, tip.x, tip.y)
  path.quadraticBezierTo(c2.x, c2.y, c.x, c.y)
  path.close()
  drawPath(path, color)
}

/** Petals drifting down on a warm haze. A touch opens a flower: petals fan out, spin and flutter down. */
internal fun DrawScope.drawPetalBurst(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  glowOrbs(t, tilt, p, 3, 0.14f)
  val path = drawing.path
  for (i in 0 until backdropCount(14, minimum = 8, maximum = 18)) {
    val u = fract(hash01(i, 1) + t * (0.02f + 0.02f * hash01(i, 2)))
    val at = Offset(w * hash01(i, 3) + sin(u * TAU * 2f + i) * 18.dp.toPx() + tilt.x * 10.dp.toPx(), h * (-0.05f + 1.1f * u))
    val a = smoothstep(0f, 0.15f, u) * smoothstep(1f, 0.85f, u)
    drawPetal(path, at, (7f + 6f * hash01(i, 4)).dp.toPx(), u * TAU * 2f + i, p.accent(i).copy(alpha = 0.30f * a))
  }
  forEachBurst(drawing, t, PETAL_LIFE) { x, y, age, seed ->
    val k = age / PETAL_LIFE
    val e = easeOut(k)
    val fade = 1f - k * k
    for (j in 0 until 12) {
      val a = j * TAU / 12f + hash01(j, seed) * 0.4f + k * 1.2f
      val d = base * (0.10f + 0.26f * hash01(j, seed + 1)) * e
      val fall = k * k * base * 0.18f
      val at = Offset(x + cos(a) * d, y + sin(a) * d + fall)
      drawPetal(path, at, (11f + 9f * hash01(j, seed + 2)).dp.toPx() * (1f - 0.35f * k), a + k * 3f, p.accent(j + seed).copy(alpha = 0.85f * fade))
    }
    drawCircle(p.ink.copy(alpha = 0.5f * (1f - smoothstep(0f, 0.25f, k))), radius = 8.dp.toPx(), center = Offset(x, y))
  }
}

/** A faint pixel mosaic that twinkles. A touch scatters square pixels that tumble outward and shrink away. */
internal fun DrawScope.drawPixelPop(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  val cell = 22.dp.toPx().coerceAtMost(base / 8f)
  val cols = (w / cell).toInt() + 1
  val rows = (h / cell).toInt() + 1
  val n = cols * rows
  val side = cell * 0.62f
  for (i in 0 until backdropCount(70, minimum = 40, maximum = 100)) {
    val idx = (hash01(i, 21) * n).toInt().coerceIn(0, n - 1)
    val tw = wave01(t * (0.5f + hash01(i, 22)) + i * 1.9f)
    val x = (idx % cols) * cell + tilt.x * 8.dp.toPx()
    val y = (idx / cols) * cell + tilt.y * 8.dp.toPx()
    drawRect(p.accent(i).copy(alpha = 0.05f + 0.20f * tw), topLeft = Offset(x, y), size = androidx.compose.ui.geometry.Size(side, side))
  }
  forEachBurst(drawing, t, PIXEL_LIFE) { x, y, age, seed ->
    val k = age / PIXEL_LIFE
    val e = easeOut(k)
    val fade = 1f - k
    for (j in 0 until 20) {
      val a = hash01(j, seed) * TAU
      val d = base * (0.06f + 0.30f * hash01(j, seed + 1)) * e
      val s = (4f + 8f * hash01(j, seed + 2)).dp.toPx() * (1f - 0.7f * k)
      val at = Offset(x + cos(a) * d - s / 2f, y + sin(a) * d + k * k * base * 0.12f - s / 2f)
      drawRect(p.accent(j + seed).copy(alpha = 0.90f * fade), topLeft = at, size = androidx.compose.ui.geometry.Size(s, s))
    }
  }
}

/** Slowly turning translucent facets. A touch shatters into glinting shards that fly out and spin. */
internal fun DrawScope.drawCrystalShatter(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  val path = drawing.path
  glowOrbs(t, tilt, p, 3, 0.12f)
  for (i in 0 until 6) {
    val c = Offset(w * hash01(i, 31) + tilt.x * (6 + 3 * i).dp.toPx(), h * hash01(i, 32) + tilt.y * (6 + 3 * i).dp.toPx())
    val r = base * (0.10f + 0.10f * hash01(i, 33))
    val rot = t * (0.05f + 0.04f * hash01(i, 34)) * (if (i % 2 == 0) 1f else -1f) + i
    path.rewind()
    for (v in 0 until 5) {
      val a = rot + v * TAU / 5f
      val rr = r * (0.7f + 0.3f * hash01(v + i * 5, 35))
      val px = c.x + cos(a) * rr
      val py = c.y + sin(a) * rr
      if (v == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, p.accent(i).copy(alpha = 0.07f + 0.05f * wave01(t * 0.6f + i)))
    drawPath(path, p.accent(i).copy(alpha = 0.22f), style = Stroke(width = 1.dp.toPx()))
  }
  forEachBurst(drawing, t, CRYSTAL_LIFE) { x, y, age, seed ->
    val k = age / CRYSTAL_LIFE
    val e = easeOut(k)
    val fade = (1f - k) * (1f - k)
    for (j in 0 until 14) {
      val a = j * TAU / 14f + hash01(j, seed) * 0.35f
      val d = base * (0.08f + 0.36f * hash01(j, seed + 1)) * e
      val c = Offset(x + cos(a) * d, y + sin(a) * d)
      val len = (9f + 15f * hash01(j, seed + 2)).dp.toPx() * (1f - 0.5f * k)
      val spin = a + k * 4f * (if (j % 2 == 0) 1f else -1f)
      path.rewind()
      path.moveTo(c.x + cos(spin) * len, c.y + sin(spin) * len)
      path.lineTo(c.x + cos(spin + 2.4f) * len * 0.45f, c.y + sin(spin + 2.4f) * len * 0.45f)
      path.lineTo(c.x + cos(spin + 3.9f) * len * 0.55f, c.y + sin(spin + 3.9f) * len * 0.55f)
      path.close()
      drawPath(path, p.accent(j + seed).copy(alpha = 0.80f * fade))
      drawPath(path, p.ink.copy(alpha = 0.55f * fade), style = Stroke(width = 0.8.dp.toPx()))
    }
    val flash = 1f - smoothstep(0f, 0.2f, k)
    if (flash > 0f) drawCircle(Brush.radialGradient(listOf(p.ink.copy(alpha = 0.5f * flash), Color.Transparent), center = Offset(x, y), radius = 50.dp.toPx()), radius = 50.dp.toPx(), center = Offset(x, y))
  }
}

/** Faint drifting stardust arms. A touch spawns a spinning vortex: a spiral of glowing dots that winds in and fades. */
internal fun DrawScope.drawVortex(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  val centre = Offset(w * 0.5f + tilt.x * 12.dp.toPx(), h * 0.5f + tilt.y * 12.dp.toPx())
  glowOrbs(t, tilt, p, 3, 0.13f)
  for (i in 0 until backdropCount(46, minimum = 28, maximum = 60)) {
    val arm = i % 3
    val u = fract(hash01(i, 41) + t * 0.012f)
    val a = arm * TAU / 3f + u * 5f + t * 0.05f
    val r = base * (0.04f + 0.62f * u)
    drawCircle(p.accent(arm).copy(alpha = 0.34f * smoothstep(0f, 0.2f, u) * (1f - u)), radius = (0.8f + 1.4f * hash01(i, 42)).dp.toPx(), center = Offset(centre.x + cos(a) * r, centre.y + sin(a) * r * 1.4f))
  }
  forEachBurst(drawing, t, VORTEX_LIFE) { x, y, age, seed ->
    val k = age / VORTEX_LIFE
    val fade = (1f - k) * smoothstep(0f, 0.08f, k)
    val spin = age * 3.2f
    for (arm in 0 until 3) {
      for (s in 0 until 16) {
        val f = s / 15f
        val r = base * 0.34f * f * (1f - 0.55f * k)
        val a = arm * TAU / 3f + f * 4.2f + spin
        drawCircle(
          p.accent(arm + seed).copy(alpha = 0.85f * fade * (1f - f * 0.6f)),
          radius = (3.2f - 2f * f).dp.toPx(),
          center = Offset(x + cos(a) * r, y + sin(a) * r),
        )
      }
    }
    drawCircle(Brush.radialGradient(listOf(p.ink.copy(alpha = 0.40f * fade), Color.Transparent), center = Offset(x, y), radius = 36.dp.toPx()), radius = 36.dp.toPx(), center = Offset(x, y))
  }
}
