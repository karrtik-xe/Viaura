package com.example.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* The newest backdrops ("NEW" section of the picker). Same rules as the others: pure functions of time and the
 * theme colours, nothing reacts to touch, the whole screen is used. */

/** Six wide translucent silk ribbons that twist and drift from the top of the screen to the bottom. */
internal fun DrawScope.drawSilk(t: Float, tilt: Offset, p: BackdropPalette, path: Path) {
  val w = size.width
  val h = size.height
  val ribbons = 6
  val steps = 36
  for (r in 0 until ribbons) {
    val f = r / (ribbons - 1f)
    val baseY = h * (0.10f + 0.80f * f) + tilt.y * 14.dp.toPx() * (0.5f + f)
    val dir = if (r % 2 == 0) 1f else -1f
    val color = p.accent(r)
    path.rewind()
    for (pass in 0..1) {
      for (s in 0..steps) {
        val u = (if (pass == 0) s else steps - s).toFloat() / steps
        val x = w * (-0.1f + 1.2f * u) + tilt.x * 16.dp.toPx() * (0.4f + f)
        val phase = u * TAU * 1.1f + t * 0.35f * dir + r * 1.7f
        val centre = baseY + sin(phase) * h * 0.055f + sin(u * TAU * 2.3f - t * 0.22f + r) * h * 0.02f
        // the ribbon is thick where its two edges are apart, thin where it twists over
        val half = h * (0.012f + 0.050f * (0.5f + 0.5f * cos(phase * 0.8f + r * 2.1f)))
        val y = if (pass == 0) centre - half else centre + half
        if (pass == 0 && s == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
    }
    path.close()
    drawPath(
      path,
      Brush.horizontalGradient(
        listOf(color.copy(alpha = 0.04f), color.copy(alpha = 0.26f), p.accent(r + 1).copy(alpha = 0.10f)),
        startX = 0f,
        endX = w,
      ),
    )
    drawPath(path, color.copy(alpha = 0.22f), style = Stroke(width = 1.dp.toPx()))
  }
}

/** Big soft nebula clouds in the theme colours drifting slowly through a field of twinkling stars. */
internal fun DrawScope.drawNebula(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val clouds = 6
  for (i in 0 until clouds) {
    val a = t * (0.05f + 0.03f * hash01(i, 1)) + hash01(i, 2) * TAU
    val b = t * (0.04f + 0.03f * hash01(i, 3)) + hash01(i, 4) * TAU
    val c = Offset(
      w * (0.5f + 0.42f * sin(a)) + tilt.x * 18.dp.toPx(),
      h * (0.5f + 0.44f * sin(b)) + tilt.y * 18.dp.toPx(),
    )
    val radius = w * (0.55f + 0.30f * hash01(i, 5)) * (0.9f + 0.1f * sin(t * 0.3f + i))
    val color = p.accent(i)
    drawCircle(
      Brush.radialGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.08f), Color.Transparent), center = c, radius = radius),
      radius = radius,
      center = c,
    )
  }
  val star = lerp(p.ink, p.primary, 0.2f)
  for (i in 0 until backdropCount(90, minimum = 50, maximum = 120)) {
    val depth = (i % 3 + 1) / 3f
    val tw = 0.25f + 0.75f * wave01(t * (0.6f + hash01(i, 7)) + i * 1.9f)
    val pos = Offset(
      w * hash01(i, 8) + tilt.x * 10.dp.toPx() * depth,
      h * hash01(i, 9) + tilt.y * 10.dp.toPx() * depth,
    )
    drawCircle(star.copy(alpha = 0.55f * tw), radius = (0.6f + 1.3f * depth).dp.toPx(), center = pos)
    if (i % 9 == 0) drawCircle(star.copy(alpha = 0.12f * tw), radius = 7.dp.toPx(), center = pos)
  }
}

/** Glowing rings spreading from drop points all over the screen, like rain falling on still, lit water. */
internal fun DrawScope.drawRipples(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val drops = 9
  val maxR = min(w, h) * 0.55f
  for (i in 0 until drops) {
    val c = Offset(
      w * (0.08f + 0.84f * hash01(i, 11)) + tilt.x * 12.dp.toPx(),
      h * (0.05f + 0.90f * hash01(i, 12)) + tilt.y * 12.dp.toPx(),
    )
    val period = 0.07f + 0.05f * hash01(i, 13)
    val color = p.accent(i)
    for (k in 0 until 3) {
      val ph = fract(t * period + hash01(i, 14) + k * 0.16f)
      val a = (1f - ph) * (1f - ph)
      val r = maxR * (0.08f + 0.92f * ph)
      drawCircle(color.copy(alpha = 0.50f * a), radius = r, center = c, style = Stroke(width = (2.2f - 1.4f * ph).dp.toPx()))
      if (k == 0) drawCircle(color.copy(alpha = 0.10f * a), radius = r * 0.9f, center = c)
    }
    val spark = fract(t * period + hash01(i, 14))
    drawCircle(color.copy(alpha = 0.6f * (1f - smoothstep(0f, 0.25f, spark))), radius = 3.dp.toPx(), center = c)
  }
}

/** Retro-future horizon: a striped glowing sun, a perspective grid rolling towards you and stars above. */
internal fun DrawScope.drawSynthwave(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val horizon = h * 0.52f + tilt.y * 8.dp.toPx()
  val cx = w * 0.5f + tilt.x * 14.dp.toPx()
  // stars
  for (i in 0 until backdropCount(40, minimum = 24, maximum = 60)) {
    val tw = 0.3f + 0.7f * wave01(t * (0.5f + hash01(i, 21)) + i)
    drawCircle(p.ink.copy(alpha = 0.40f * tw), radius = (0.7f + hash01(i, 22)).dp.toPx(), center = Offset(w * hash01(i, 23), horizon * 0.9f * hash01(i, 24)))
  }
  // sun with stripes cut out of its lower half
  val sunR = w * 0.30f
  val sunC = Offset(cx, horizon - sunR * 0.25f)
  drawCircle(Brush.radialGradient(listOf(p.secondary.copy(alpha = 0.30f), Color.Transparent), center = sunC, radius = sunR * 2f), radius = sunR * 2f, center = sunC)
  val bands = 9
  for (b in 0 until bands) {
    val y0 = sunC.y - sunR + 2f * sunR * b / bands
    val y1 = y0 + 2f * sunR / bands
    val bandH = y1 - y0
    // lower bands get thinner (the gap grows) and slide down slowly
    val gap = if (b < 3) 0f else bandH * 0.16f * (b - 2)
    val top = y0 + gap * 0.5f
    val dy = (top - sunC.y) / sunR
    if (dy < -1f || dy > 1f) continue
    val half = sunR * kotlin.math.sqrt((1f - dy * dy).coerceAtLeast(0f))
    val ratio = b / (bands - 1f)
    drawRect(
      color = lerp(p.tertiary, p.secondary, ratio).copy(alpha = 0.42f),
      topLeft = Offset(cx - half, top),
      size = androidx.compose.ui.geometry.Size(half * 2f, (bandH - gap).coerceAtLeast(1f)),
    )
  }
  // ground
  val line = lerp(p.ink, p.primary, 0.55f)
  drawLine(p.primary.copy(alpha = 0.55f), Offset(0f, horizon), Offset(w, horizon), strokeWidth = 1.6.dp.toPx())
  val rows = 12
  for (k in 0 until rows) {
    val u = fract(t * 0.16f + k / rows.toFloat())
    val y = horizon + (h - horizon) * u * u
    drawLine(line.copy(alpha = 0.10f + 0.34f * u), Offset(0f, y), Offset(w, y), strokeWidth = (0.6f + 1.4f * u).dp.toPx())
  }
  val cols = 12
  for (k in -cols..cols) {
    // every line starts close together at the horizon and fans out towards the bottom edge
    val topX = cx + k * w * 0.02f
    val bottomX = cx + k * w * 0.26f
    drawLine(line.copy(alpha = 0.22f), Offset(topX, horizon), Offset(bottomX, h), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
  }
}

/** A slowly turning mandala of overlapping circles that breathes, filling the middle of the screen with light. */
internal fun DrawScope.drawMandala(t: Float, tilt: Offset, p: BackdropPalette) {
  val w = size.width
  val h = size.height
  val c = Offset(w * 0.5f + tilt.x * 16.dp.toPx(), h * 0.5f + tilt.y * 16.dp.toPx())
  val reach = kotlin.math.hypot(w, h) * 0.5f
  drawCircle(Brush.radialGradient(listOf(p.primary.copy(alpha = 0.16f), Color.Transparent), center = c, radius = reach * 0.8f), radius = reach * 0.8f, center = c)
  val rings = 4
  for (ring in 0 until rings) {
    val petals = 6 + ring * 3
    val dir = if (ring % 2 == 0) 1f else -1f
    val orbit = reach * (0.16f + 0.22f * ring) * (1f + 0.04f * sin(t * 0.6f + ring))
    val petalR = orbit * (0.55f + 0.12f * sin(t * 0.5f + ring * 1.3f))
    val color = p.accent(ring)
    for (i in 0 until petals) {
      val a = TAU * i / petals + t * 0.10f * dir * (1f - 0.15f * ring)
      val pc = Offset(c.x + cos(a) * orbit, c.y + sin(a) * orbit)
      drawCircle(color.copy(alpha = 0.26f), radius = petalR, center = pc, style = Stroke(width = 1.2.dp.toPx()))
      if (i % 3 == 0) drawCircle(color.copy(alpha = 0.55f), radius = 2.4.dp.toPx(), center = pc)
    }
  }
  drawCircle(p.secondary.copy(alpha = 0.30f), radius = reach * 0.05f * (1f + 0.2f * sin(t)), center = c)
  drawCircle(p.ink.copy(alpha = 0.35f), radius = 3.dp.toPx(), center = c)
}
