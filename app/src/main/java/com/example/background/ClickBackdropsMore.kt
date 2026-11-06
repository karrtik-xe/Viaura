package com.example.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Three more click-effect backdrops ("NEW" section of the picker): Comet trail, Firefly swarm and Spirograph.
 * Same rules as the other click effects: pure functions of time, theme colours and the recent touches in
 * [BackdropDrawingState]. */

private const val COMET_LIFE = 1.9f
private const val SWARM_LIFE = 3.4f
private const val SPIRO_LIFE = 2.8f

/** Twinkling stars with a slow comet crossing now and then. A touch launches two comets that curve away from the finger. */
internal fun DrawScope.drawCometTrail(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  for (i in 0 until backdropCount(34, minimum = 18, maximum = 44)) {
    val tw = 0.25f + 0.75f * wave01(t * (0.5f + hash01(i, 71)) + i * 1.9f)
    drawCircle(
      p.ink.copy(alpha = 0.30f * tw),
      radius = (0.7f + 1.2f * hash01(i, 72)).dp.toPx(),
      center = Offset(w * hash01(i, 73) + tilt.x * 8.dp.toPx(), h * hash01(i, 74) + tilt.y * 8.dp.toPx()),
    )
  }
  // ambient comets, long and slow
  for (i in 0 until 2) {
    val u = fract(t * 0.045f + i * 0.5f)
    val head = Offset(w * (-0.15f + 1.3f * u), h * (0.12f + 0.3f * i) + h * 0.35f * u)
    val fade = smoothstep(0f, 0.12f, u) * smoothstep(1f, 0.85f, u)
    for (s in 0 until 14) {
      val f = s / 13f
      drawCircle(
        p.accent(i).copy(alpha = 0.30f * fade * (1f - f)),
        radius = (2.4f - 1.6f * f).dp.toPx(),
        center = Offset(head.x - w * 0.045f * s, head.y - h * 0.016f * s),
      )
    }
  }
  forEachBurst(drawing, t, COMET_LIFE) { x, y, age, seed ->
    val k = age / COMET_LIFE
    val fade = 1f - smoothstep(0.65f, 1f, k)
    for (j in 0 until 2) {
      val dirA = hash01(j, seed) * TAU + j * 3.1416f
      val dx = cos(dirA)
      val dy = sin(dirA)
      val bend = if (j == 0) 1f else -1f
      val reach = base * 0.62f
      val col = p.accent(seed + j)
      for (s in 0 until 22) {
        val e = (k / 0.85f - s * 0.022f).coerceIn(0f, 1f)
        val ee = 1f - (1f - e) * (1f - e)
        val arc = sin(ee * 3.1416f) * 0.30f * bend
        val px = x + dx * reach * ee - dy * reach * arc
        val py = y + dy * reach * ee + dx * reach * arc
        val f = s / 21f
        val a = fade * (1f - f) * (1f - f)
        if (s == 0) {
          drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.85f * a), Color.Transparent), center = Offset(px, py), radius = 16.dp.toPx()), radius = 16.dp.toPx(), center = Offset(px, py))
          drawCircle(p.ink.copy(alpha = 0.9f * a), radius = 3.dp.toPx(), center = Offset(px, py))
        } else {
          drawCircle(col.copy(alpha = 0.55f * a), radius = (3f - 2.2f * f).dp.toPx(), center = Offset(px, py))
        }
      }
    }
    drawCircle(Brush.radialGradient(listOf(p.ink.copy(alpha = 0.35f * (1f - k) * (1f - k)), Color.Transparent), center = Offset(x, y), radius = 40.dp.toPx()), radius = 40.dp.toPx(), center = Offset(x, y))
  }
}

/** Fireflies drifting and blinking. A touch calls the swarm: they fly in, circle the finger, then scatter outward. */
internal fun DrawScope.drawFireflySwarm(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  for (i in 0 until backdropCount(22, minimum = 12, maximum = 30)) {
    val c = Offset(
      w * (0.5f + 0.46f * sin(t * (0.05f + 0.05f * hash01(i, 81)) + i * 2.7f)) + tilt.x * 10.dp.toPx(),
      h * (0.5f + 0.46f * sin(t * (0.04f + 0.05f * hash01(i, 82)) + i * 1.9f)) + tilt.y * 10.dp.toPx(),
    )
    val blink = 0.15f + 0.85f * wave01(t * (0.8f + hash01(i, 83)) + i * 1.3f)
    val col = p.accent(i)
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.38f * blink), Color.Transparent), center = c, radius = 12.dp.toPx()), radius = 12.dp.toPx(), center = c)
    drawCircle(col.copy(alpha = 0.85f * blink), radius = 1.6.dp.toPx(), center = c)
  }
  forEachBurst(drawing, t, SWARM_LIFE) { x, y, age, seed ->
    val c = Offset(x, y)
    val k = age / SWARM_LIFE
    val alpha = smoothstep(0f, 0.1f, k) * (1f - smoothstep(0.85f, 1f, k))
    drawCircle(Brush.radialGradient(listOf(p.accent(seed).copy(alpha = 0.22f * alpha), Color.Transparent), center = c, radius = 60.dp.toPx()), radius = 60.dp.toPx(), center = c)
    for (j in 0 until 14) {
      val start = Offset(w * hash01(j, seed + 3), h * hash01(j, seed + 4))
      val orbitA = j * TAU / 14f + age * 2.4f
      val orbitR = (14f + 18f * hash01(j, seed + 5)).dp.toPx()
      val orbit = Offset(c.x + cos(orbitA) * orbitR, c.y + sin(orbitA) * orbitR)
      val pos = when {
        k < 0.35f -> {
          val e = smoothstep(0f, 1f, k / 0.35f)
          Offset(start.x + (orbit.x - start.x) * e, start.y + (orbit.y - start.y) * e)
        }
        k < 0.75f -> orbit
        else -> {
          val e = (k - 0.75f) / 0.25f
          val out = base * 0.5f * e * e
          Offset(c.x + cos(orbitA) * (orbitR + out), c.y + sin(orbitA) * (orbitR + out))
        }
      }
      val blink = 0.7f + 0.3f * sin(age * 9f + j)
      val col = p.accent(j + seed)
      drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.55f * alpha * blink), Color.Transparent), center = pos, radius = 10.dp.toPx()), radius = 10.dp.toPx(), center = pos)
      drawCircle(p.ink.copy(alpha = 0.9f * alpha * blink), radius = 1.8.dp.toPx(), center = pos)
    }
  }
}

/** A faint slowly turning rosette. A touch draws a spirograph curve that traces itself around the finger, then fades. */
internal fun DrawScope.drawSpirograph(t: Float, tilt: Offset, p: BackdropPalette, drawing: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val base = min(w, h)
  val centre = Offset(w * 0.5f + tilt.x * 12.dp.toPx(), h * 0.5f + tilt.y * 12.dp.toPx())
  val path = drawing.path
  for (layer in 0 until 2) {
    val (rr, r, d) = if (layer == 0) Triple(7f, 2f, 5.2f) else Triple(5f, 3f, 4.2f)
    val scale = base * (0.46f - 0.12f * layer) / (rr - r + d)
    val turns = r.toInt()
    val rot = t * (0.05f + 0.03f * layer) * (if (layer == 0) 1f else -1f)
    path.reset()
    val n = 160
    for (s in 0..n) {
      val th = TAU * turns * s / n
      val q = (rr - r) / r * th
      val px = (rr - r) * cos(th) + d * cos(q)
      val py = (rr - r) * sin(th) - d * sin(q)
      val rx = (px * cos(rot) - py * sin(rot)) * scale + centre.x
      val ry = (px * sin(rot) + py * cos(rot)) * scale + centre.y
      if (s == 0) path.moveTo(rx, ry) else path.lineTo(rx, ry)
    }
    drawPath(path, p.accent(layer).copy(alpha = 0.13f), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
  forEachBurst(drawing, t, SPIRO_LIFE) { x, y, age, seed ->
    val k = age / SPIRO_LIFE
    val grow = 1f - (1f - (k / 0.6f).coerceIn(0f, 1f)).let { it * it * it }
    val fade = 1f - smoothstep(0.6f, 1f, k)
    val pick = ((seed % 4) + 4) % 4
    val rr = if (pick < 2) 5f else 7f
    val r = if (pick % 2 == 0) 3f else 2f
    val d = rr - 0.8f
    val turns = r.toInt()
    val scale = base * 0.26f / (rr - r + d)
    val rot = age * 0.6f + hash01(seed, 91) * TAU
    val steps = (160 * grow).toInt().coerceAtLeast(2)
    for (layer in 0 until 2) {
      path.reset()
      var hx = x
      var hy = y
      for (s in 0..steps) {
        val th = TAU * turns * grow * s / steps
        val q = (rr - r) / r * th
        val px = (rr - r) * cos(th) + d * cos(q)
        val py = (rr - r) * sin(th) - d * sin(q)
        hx = (px * cos(rot) - py * sin(rot)) * scale + x
        hy = (px * sin(rot) + py * cos(rot)) * scale + y
        if (s == 0) path.moveTo(hx, hy) else path.lineTo(hx, hy)
      }
      val col = p.accent(seed + layer)
      if (layer == 0) {
        drawPath(path, col.copy(alpha = 0.22f * fade), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
      } else {
        drawPath(path, col.copy(alpha = 0.85f * fade), style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        if (grow < 1f) drawCircle(p.ink.copy(alpha = 0.9f * fade), radius = 3.dp.toPx(), center = Offset(hx, hy))
      }
    }
  }
}
