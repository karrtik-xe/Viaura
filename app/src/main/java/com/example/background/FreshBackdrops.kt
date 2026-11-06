package com.example.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sqrt
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

/* ============================================================================================
 * Click-effect backdrops: each one answers a touch anywhere on the screen (see BackdropTouch).
 * ============================================================================================ */

private const val INK_LIFE = 2.6f
private const val NEON_LIFE = 2.2f
private const val STAR_LIFE = 1.7f
private const val POP_LIFE = 0.75f
private const val STORM_LIFE = 1.5f

private fun DrawScope.drawSparkle(c: Offset, s: Float, color: Color) {
  val sw = 1.4.dp.toPx()
  drawLine(color, Offset(c.x - s, c.y), Offset(c.x + s, c.y), strokeWidth = sw, cap = StrokeCap.Round)
  drawLine(color, Offset(c.x, c.y - s), Offset(c.x, c.y + s), strokeWidth = sw, cap = StrokeCap.Round)
  drawCircle(color, radius = s * 0.32f, center = c)
}

/** Slow drifting ink clouds. A touch drops ink: coloured blooms spread out from the finger with a thin ring. */
internal fun DrawScope.drawInkBloom(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  for (i in 0 until 4) {
    val c = Offset(
      w * (0.5f + 0.38f * sin(t * (0.07f + 0.03f * hash01(i, 1)) + i * 2.1f)) + tilt.x * 14.dp.toPx(),
      h * (0.5f + 0.38f * sin(t * (0.05f + 0.03f * hash01(i, 2)) + i * 1.3f)) + tilt.y * 14.dp.toPx(),
    )
    val r = base * (0.55f + 0.25f * hash01(i, 3))
    drawCircle(Brush.radialGradient(listOf(p.accent(i).copy(alpha = 0.20f), Color.Transparent), center = c, radius = r), radius = r, center = c)
  }
  for (i in 0 until backdropCount(26, minimum = 14, maximum = 34)) {
    val u = fract(hash01(i, 4) + t * (0.01f + 0.02f * hash01(i, 5)))
    val a = smoothstep(0f, 0.2f, u) * smoothstep(1f, 0.8f, u)
    drawCircle(
      p.accent(i).copy(alpha = 0.32f * a),
      radius = (0.8f + 1.4f * hash01(i, 6)).dp.toPx(),
      center = Offset(w * hash01(i, 7) + sin(u * TAU + i) * 12.dp.toPx() + tilt.x * 10.dp.toPx(), h * (1.02f - u * 1.04f)),
    )
  }
  forEachBurst(drawing, t, INK_LIFE) { x, y, age, seed ->
    val c = Offset(x, y)
    val k = age / INK_LIFE
    val e = 1f - (1f - k) * (1f - k) * (1f - k)
    val fade = 1f - k
    for (j in 0 until 5) {
      val a = hash01(j, seed) * TAU
      val off = base * 0.10f * e * (0.4f + 0.6f * hash01(j, seed + 1))
      val bc = Offset(c.x + cos(a) * off, c.y + sin(a) * off)
      val r = base * (0.10f + 0.22f * e) * (0.7f + 0.5f * hash01(j, seed + 2))
      val col = p.accent(j + seed)
      drawCircle(
        Brush.radialGradient(listOf(col.copy(alpha = 0.40f * fade), col.copy(alpha = 0.10f * fade), Color.Transparent), center = bc, radius = r),
        radius = r,
        center = bc,
      )
    }
    drawCircle(p.ink.copy(alpha = 0.35f * fade * fade), radius = base * (0.04f + 0.38f * e), center = c, style = Stroke(width = 1.5.dp.toPx()))
  }
}

/** A glowing dot grid that breathes. A touch sends a shock ring through it: dots are pushed aside and flare up. */
internal fun DrawScope.drawNeonPulse(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  val gap = 30.dp.toPx().coerceAtMost(base / 5f)
  val cols = (w / gap).toInt() + 2
  val rows = (h / gap).toInt() + 2
  val ox = (w - (cols - 1) * gap) / 2f + tilt.x * 6.dp.toPx()
  val oy = (h - (rows - 1) * gap) / 2f + tilt.y * 6.dp.toPx()
  for (r in 0 until rows) {
    for (c in 0 until cols) {
      var px = ox + c * gap
      var py = oy + r * gap
      var glow = 0.10f + 0.16f * wave01(t * 1.1f + (c + r) * 0.55f)
      for (b in 0 until BURST_SLOTS) {
        val age = t - drawing.burstT[b]
        if (age < 0f || age > NEON_LIFE) continue
        val dx = px - drawing.burstX[b]
        val dy = py - drawing.burstY[b]
        val d = sqrt(dx * dx + dy * dy)
        val front = age * base * 0.9f
        val band = (1f - abs(d - front) / (gap * 2.2f)).coerceAtLeast(0f)
        if (band > 0f) {
          val left = 1f - age / NEON_LIFE
          if (d > 0.01f) {
            val push = band * gap * 0.45f * left
            px += dx / d * push
            py += dy / d * push
          }
          glow += 0.75f * band * left
        }
      }
      glow = glow.coerceAtMost(1f)
      val col = p.accent(r + c)
      val at = Offset(px, py)
      drawCircle(col.copy(alpha = glow), radius = (1.4f + 2.6f * (glow - 0.1f).coerceIn(0f, 1f)).dp.toPx(), center = at)
      if (glow > 0.5f) drawCircle(col.copy(alpha = 0.18f * (glow - 0.5f) * 2f), radius = 9.dp.toPx(), center = at)
    }
  }
  forEachBurst(drawing, t, NEON_LIFE) { x, y, age, seed ->
    val k = age / NEON_LIFE
    val col = p.accent(seed)
    drawCircle(col.copy(alpha = 0.38f * (1f - k)), radius = age * base * 0.9f, center = Offset(x, y), style = Stroke(width = 1.4.dp.toPx()))
    val flash = 1f - smoothstep(0f, 0.25f, k)
    if (flash > 0f) drawCircle(col.copy(alpha = 0.45f * flash), radius = 26.dp.toPx() * (0.6f + k * 2f), center = Offset(x, y))
  }
}

/** A twinkling star field in three depths. A touch throws a ring of sparkles outward from the finger. */
internal fun DrawScope.drawStarburst(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  drawCircle(Brush.radialGradient(listOf(p.secondary.copy(alpha = 0.12f), Color.Transparent), center = Offset(0f, 0f), radius = base * 0.9f), radius = base * 0.9f, center = Offset(0f, 0f))
  drawCircle(Brush.radialGradient(listOf(p.primary.copy(alpha = 0.12f), Color.Transparent), center = Offset(w, h), radius = base * 0.9f), radius = base * 0.9f, center = Offset(w, h))
  for (i in 0 until backdropCount(64, minimum = 34, maximum = 84)) {
    val layer = i % 3
    val x = w * fract(hash01(i, 1) + t * 0.004f * (layer + 1)) + tilt.x * (5 + 6 * layer).dp.toPx()
    val y = h * hash01(i, 2) + tilt.y * (5 + 6 * layer).dp.toPx()
    val tw = 0.35f + 0.65f * wave01(t * (0.8f + hash01(i, 3)) + i * 1.7f)
    val r = (0.6f + 0.5f * layer + 0.5f * hash01(i, 4)).dp.toPx()
    val col = if (layer == 2) p.accent(i) else p.ink
    drawCircle(col.copy(alpha = (0.25f + 0.2f * layer) * tw), radius = r, center = Offset(x, y))
    if (layer == 2 && tw > 0.75f) drawSparkle(Offset(x, y), r * 3.6f, col.copy(alpha = 0.45f * tw))
  }
  forEachBurst(drawing, t, STAR_LIFE) { x, y, age, seed ->
    val k = age / STAR_LIFE
    val e = 1f - (1f - k) * (1f - k)
    val fade = (1f - k) * (1f - k)
    val c = Offset(x, y)
    val flash = 1f - smoothstep(0f, 0.3f, k)
    if (flash > 0f) drawCircle(Brush.radialGradient(listOf(p.ink.copy(alpha = 0.40f * flash), Color.Transparent), center = c, radius = 60.dp.toPx()), radius = 60.dp.toPx(), center = c)
    for (j in 0 until 16) {
      val a = j * TAU / 16f + hash01(j, seed) * 0.3f
      val d = base * (0.12f + 0.34f * hash01(j, seed + 1)) * e
      val s = (3f + 4f * hash01(j, seed + 2)).dp.toPx() * (1f - 0.6f * k)
      drawSparkle(Offset(c.x + cos(a) * d, c.y + sin(a) * d), s, p.accent(j + seed).copy(alpha = 0.95f * fade))
    }
  }
}

/** Glassy bubbles drifting up. A touch pops the bubbles near the finger into droplets and leaves a soft ripple. */
internal fun DrawScope.drawBubblePop(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val reach = 90.dp.toPx()
  for (i in 0 until backdropCount(22, minimum = 12, maximum = 28)) {
    val speed = 0.018f + 0.022f * hash01(i, 1)
    val phase = hash01(i, 2)
    val r = (10f + 26f * hash01(i, 3)).dp.toPx()
    val cyc = phase + t * speed
    val k = floor(cyc).toInt()
    val u = cyc - k
    val at = Offset(
      w * (0.06f + 0.88f * hash01(i + k * 37, 4)) + sin(u * TAU * 2f + i) * 14.dp.toPx() + tilt.x * 10.dp.toPx(),
      h * (1.08f - 1.16f * u),
    )
    val col = p.accent(i)
    // popped by a touch earlier in this same trip up the screen?
    var popped = false
    for (b in 0 until BURST_SLOTS) {
      val bt = drawing.burstT[b]
      val c0 = phase + bt * speed
      if (floor(c0).toInt() != k) continue
      val u0 = c0 - k
      val at0 = Offset(
        w * (0.06f + 0.88f * hash01(i + k * 37, 4)) + sin(u0 * TAU * 2f + i) * 14.dp.toPx() + tilt.x * 10.dp.toPx(),
        h * (1.08f - 1.16f * u0),
      )
      val dx = at0.x - drawing.burstX[b]
      val dy = at0.y - drawing.burstY[b]
      if (sqrt(dx * dx + dy * dy) > reach + r) continue
      popped = true
      val age = t - bt
      if (age in 0f..POP_LIFE) {
        val kk = age / POP_LIFE
        val e = 1f - (1f - kk) * (1f - kk)
        val fade = 1f - kk
        drawCircle(col.copy(alpha = 0.55f * fade), radius = r * (1f + 1.0f * e), center = at0, style = Stroke(width = 1.6.dp.toPx()))
        for (j in 0 until 8) {
          val a = j * TAU / 8f + hash01(i + j, 9) * 0.4f
          val d = r * (0.8f + 1.5f * e)
          drawCircle(col.copy(alpha = 0.8f * fade), radius = (2.4f * (1f - 0.5f * kk)).dp.toPx(), center = Offset(at0.x + cos(a) * d, at0.y + sin(a) * d))
        }
      }
      break
    }
    if (popped) continue
    val a = smoothstep(0f, 0.08f, u) * smoothstep(1f, 0.92f, u)
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.03f * a), col.copy(alpha = 0.20f * a)), center = at, radius = r), radius = r, center = at)
    drawCircle(col.copy(alpha = 0.55f * a), radius = r, center = at, style = Stroke(width = 1.3.dp.toPx()))
    drawCircle(p.ink.copy(alpha = 0.55f * a), radius = r * 0.16f, center = Offset(at.x - r * 0.38f, at.y - r * 0.40f))
  }
  forEachBurst(drawing, t, 1.0f) { x, y, age, seed ->
    val k = age / 1.0f
    drawCircle(p.accent(seed).copy(alpha = 0.30f * (1f - k)), radius = reach * (0.2f + 0.9f * k), center = Offset(x, y), style = Stroke(width = 1.2.dp.toPx()))
  }
}

/** A stormy night: dark clouds, slanting rain, a distant flash now and then. A touch calls down a lightning bolt. */
internal fun DrawScope.drawStorm(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  drawRect(Brush.verticalGradient(listOf(p.primary.copy(alpha = 0.16f), Color.Transparent), startY = 0f, endY = h * 0.6f), size = size)
  val cloud = lerp(p.primary, p.ink, 0.35f)
  for (i in 0 until 5) {
    val cx = w * (fract(hash01(i, 1) + t * 0.006f * (1f + hash01(i, 2))) * 1.4f - 0.2f) + tilt.x * 12.dp.toPx()
    val cy = h * (0.04f + 0.10f * hash01(i, 3))
    val r = w * (0.35f + 0.25f * hash01(i, 4))
    drawCircle(Brush.radialGradient(listOf(cloud.copy(alpha = 0.18f), Color.Transparent), center = Offset(cx, cy), radius = r), radius = r, center = Offset(cx, cy))
  }
  val dx = -5.dp.toPx()
  val dy = 16.dp.toPx()
  for (i in 0 until backdropCount(46, minimum = 24, maximum = 60)) {
    val u = fract(hash01(i, 5) + t * (0.55f + 0.6f * hash01(i, 6)))
    val x = w * hash01(i, 7) - u * h * 0.12f
    val y = h * (u * 1.15f - 0.08f)
    drawLine(p.ink.copy(alpha = 0.20f), Offset(x, y), Offset(x + dx, y + dy), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
  }
  // a faint flash somewhere far away
  val ph = fract(t / 11f + 0.3f)
  if (ph < 0.04f) drawRect(p.ink.copy(alpha = 0.07f * (1f - ph / 0.04f)), size = size)

  forEachBurst(drawing, t, STORM_LIFE) { x, y, age, seed ->
    val flick = when {
      age < 0.08f -> 1f
      age < 0.16f -> 0.25f
      age < 0.26f -> 0.9f
      else -> (1f - (age - 0.26f) / (STORM_LIFE - 0.26f)).coerceAtLeast(0f) * 0.55f
    }
    if (age < 0.3f) drawRect(p.ink.copy(alpha = 0.12f * flick), size = size)
    val path = drawing.path
    path.rewind()
    val startX = x + (hash01(0, seed) - 0.5f) * w * 0.3f
    path.moveTo(startX, -10f)
    val segs = 9
    var branchAt = Offset(x, y)
    for (s in 1..segs) {
      val f = s / segs.toFloat()
      val bx = if (s == segs) x else startX + (x - startX) * f + (hash01(s, seed + 5) - 0.5f) * w * 0.12f * (1f - f)
      val by = if (s == segs) y else y * f
      path.lineTo(bx, by)
      if (s == 4) branchAt = Offset(bx, by)
    }
    drawPath(path, p.primary.copy(alpha = 0.30f * flick), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, Color.White.copy(alpha = 0.95f * flick), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // one side branch
    val dir = if (hash01(1, seed) > 0.5f) 1f else -1f
    var bp = branchAt
    for (s in 1..3) {
      val np = Offset(bp.x + dir * (14f + 10f * hash01(s, seed + 8)).dp.toPx(), bp.y + (18f + 10f * hash01(s, seed + 9)).dp.toPx())
      drawLine(Color.White.copy(alpha = 0.75f * flick), bp, np, strokeWidth = 1.3.dp.toPx(), cap = StrokeCap.Round)
      bp = np
    }
    val c = Offset(x, y)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f * flick), p.primary.copy(alpha = 0.25f * flick), Color.Transparent), center = c, radius = 70.dp.toPx()), radius = 70.dp.toPx(), center = c)
    drawCircle(p.primary.copy(alpha = 0.40f * (1f - age / STORM_LIFE)), radius = 14.dp.toPx() + age * 110.dp.toPx(), center = c, style = Stroke(width = 1.4.dp.toPx()))
  }
}
