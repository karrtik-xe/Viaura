package com.example.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/* ------------------------------------------------------------------------------------------------------------
 * "Click effect" backdrops: a calm animated scene that ALSO reacts to a touch anywhere on the screen.
 * The activity (and the settings sheet) report every new finger-down to BackdropTouch; BackdropCanvas turns it into a
 * burst (position + canvas time) in BackdropDrawingState. These functions only read those bursts, so they stay pure
 * functions of time like every other backdrop. The picker tiles set `demo` to show a frozen mid-effect touch.
 * ------------------------------------------------------------------------------------------------------------ */

private const val WATER_LIFE = 2.4f
private const val STARDUST_LIFE = 2.2f
private const val PULSE_LIFE = 2.0f
private const val BLOOM_LIFE = 2.8f
private const val POP_LIFE = 1.9f

/** Calls [block] for every touch that is still playing: its position, progress 0..1 and a per-touch seed. */
private inline fun DrawScope.forEachTouch(
  d: BackdropDrawingState,
  t: Float,
  life: Float,
  block: (c: Offset, x: Float, seed: Int) -> Unit,
) {
  for (i in 0 until BURST_SLOTS) {
    val age = t - d.burstT[i]
    if (age < 0f || age > life) continue
    block(Offset(d.burstX[i], d.burstY[i]), age / life, d.burstSeed[i])
  }
  if (d.demo) block(Offset(size.width * 0.6f, size.height * 0.45f), 0.32f, 41)
}

private fun easeOut(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

/** Soft underwater light and gentle waves; a touch drops a stone: rings spread out and a few droplets jump. */
internal fun DrawScope.drawWaterTouch(t: Float, tilt: Offset, p: BackdropPalette, d: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  for (i in 0 until 5) {
    val a = t * (0.06f + 0.04f * hash01(i, 1)) + hash01(i, 2) * TAU
    val b = t * (0.05f + 0.04f * hash01(i, 3)) + hash01(i, 4) * TAU
    val c = Offset(
      w * (0.5f + 0.40f * sin(a)) + tilt.x * 14.dp.toPx(),
      h * (0.5f + 0.42f * sin(b)) + tilt.y * 14.dp.toPx(),
    )
    val r = w * (0.50f + 0.25f * hash01(i, 5))
    val col = p.accent(i)
    drawCircle(
      Brush.radialGradient(listOf(col.copy(alpha = 0.20f), col.copy(alpha = 0.06f), Color.Transparent), center = c, radius = r),
      radius = r,
      center = c,
    )
  }
  val path = d.path
  val lines = 8
  val steps = 28
  for (l in 0 until lines) {
    val baseY = h * (0.08f + 0.86f * l / (lines - 1f)) + tilt.y * 8.dp.toPx() * (0.5f + l / lines.toFloat())
    path.rewind()
    for (s in 0..steps) {
      val u = s / steps.toFloat()
      val y = baseY + sin(u * TAU * 1.4f + t * 0.5f + l * 1.3f) * h * 0.012f + sin(u * TAU * 0.7f - t * 0.3f + l) * h * 0.008f
      if (s == 0) path.moveTo(w * u, y) else path.lineTo(w * u, y)
    }
    drawPath(path, p.accent(l).copy(alpha = 0.11f), style = Stroke(width = 1.2.dp.toPx()))
  }
  for (i in 0 until backdropCount(22, minimum = 10, maximum = 34)) {
    val tw = wave01(t * (0.8f + hash01(i, 7)) + i * 2.1f)
    drawCircle(
      p.ink.copy(alpha = 0.05f + 0.18f * tw * tw),
      radius = (0.8f + 1.2f * hash01(i, 8)).dp.toPx(),
      center = Offset(w * hash01(i, 9), h * hash01(i, 10)),
    )
  }

  val maxR = min(w, h) * 0.5f
  forEachTouch(d, t, WATER_LIFE) { c, x, seed ->
    val fade = (1f - x) * (1f - x)
    drawCircle(
      Brush.radialGradient(listOf(p.accent(seed).copy(alpha = 0.28f * fade), Color.Transparent), center = c, radius = maxR * 0.5f),
      radius = maxR * 0.5f,
      center = c,
    )
    for (k in 0 until 3) {
      val ph = ((x - k * 0.10f) / (1f - k * 0.10f)).coerceIn(0f, 1f)
      if (ph <= 0f) continue
      val a = (1f - ph) * (1f - ph)
      drawCircle(
        p.accent(seed + k).copy(alpha = 0.65f * a),
        radius = maxR * (0.05f + 0.95f * (1f - (1f - ph) * (1f - ph))),
        center = c,
        style = Stroke(width = (2.6f - 1.6f * ph).dp.toPx()),
      )
    }
    if (x < 0.6f) {
      val dx = x / 0.6f
      for (i in 0 until 7) {
        val ang = -TAU / 4f + (i - 3) * 0.34f + (hash01(i, seed) - 0.5f) * 0.2f
        val reach = (26f + 34f * hash01(i, seed + 3)).dp.toPx()
        val pos = Offset(c.x + cos(ang) * reach * dx, c.y + sin(ang) * reach * dx + 40.dp.toPx() * dx * dx)
        drawCircle(
          p.accent(seed + i).copy(alpha = 0.8f * (1f - dx)),
          radius = (1.4f + 1.4f * hash01(i, seed + 5)).dp.toPx() * (1f - 0.4f * dx),
          center = pos,
        )
      }
    }
    drawCircle(p.ink.copy(alpha = 0.8f * (1f - smoothstep(0f, 0.2f, x))), radius = 3.dp.toPx(), center = c)
  }
}

/** A twinkling night sky; a touch throws a burst of glittering stardust that spreads out and slowly drifts down. */
internal fun DrawScope.drawStardust(t: Float, tilt: Offset, p: BackdropPalette, d: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  drawRect(
    brush = Brush.verticalGradient(listOf(p.secondary.copy(alpha = 0.10f), Color.Transparent, p.primary.copy(alpha = 0.08f))),
    size = size,
  )
  for (i in 0 until backdropCount(70, minimum = 40, maximum = 100)) {
    val depth = (i % 3 + 1) / 3f
    val tw = 0.25f + 0.75f * wave01(t * (0.6f + hash01(i, 21)) + i * 1.7f)
    val pos = Offset(
      w * hash01(i, 22) + tilt.x * 10.dp.toPx() * depth,
      h * hash01(i, 23) + tilt.y * 10.dp.toPx() * depth,
    )
    drawCircle(p.ink.copy(alpha = 0.50f * tw), radius = (0.6f + 1.2f * depth).dp.toPx(), center = pos)
    if (i % 11 == 0) {
      val len = (4f + 3f * depth).dp.toPx() * tw
      drawLine(p.ink.copy(alpha = 0.35f * tw), Offset(pos.x - len, pos.y), Offset(pos.x + len, pos.y), strokeWidth = 1.dp.toPx())
      drawLine(p.ink.copy(alpha = 0.35f * tw), Offset(pos.x, pos.y - len), Offset(pos.x, pos.y + len), strokeWidth = 1.dp.toPx())
    }
  }
  for (i in 0 until 3) {
    val a = t * (0.05f + 0.03f * hash01(i, 24)) + hash01(i, 25) * TAU
    val c = Offset(w * (0.5f + 0.38f * sin(a)) + tilt.x * 16.dp.toPx(), h * (0.25f + 0.5f * hash01(i, 26)) + tilt.y * 16.dp.toPx())
    val r = w * 0.5f
    drawCircle(Brush.radialGradient(listOf(p.accent(i).copy(alpha = 0.14f), Color.Transparent), center = c, radius = r), radius = r, center = c)
  }

  val base = min(w, h)
  forEachTouch(d, t, STARDUST_LIFE) { c, x, seed ->
    val ease = easeOut(x)
    val flash = 1f - smoothstep(0f, 0.22f, x)
    if (flash > 0f) {
      drawCircle(
        Brush.radialGradient(listOf(p.ink.copy(alpha = 0.50f * flash), p.accent(seed).copy(alpha = 0.18f * flash), Color.Transparent), center = c, radius = base * 0.28f),
        radius = base * 0.28f,
        center = c,
      )
    }
    for (i in 0 until 30) {
      val ang = i * TAU / 30f + (hash01(i, seed) - 0.5f) * 0.5f
      val reach = base * (0.18f + 0.30f * hash01(i, seed + 7))
      val pos = Offset(c.x + cos(ang) * reach * ease, c.y + sin(ang) * reach * ease + h * 0.10f * x * x)
      val tw = 0.5f + 0.5f * sin(x * TAU * 3f + i)
      val a = (1f - x) * (0.45f + 0.55f * tw)
      val col = if (i % 4 == 0) p.ink else p.accent(i + seed)
      drawCircle(col.copy(alpha = 0.9f * a), radius = (0.9f + 1.8f * hash01(i, seed + 3)).dp.toPx() * (1f - 0.5f * x), center = pos)
      if (i % 5 == 0) {
        val len = (4f + 4f * hash01(i, seed + 9)).dp.toPx() * (1f - 0.4f * x)
        drawLine(col.copy(alpha = 0.7f * a), Offset(pos.x - len, pos.y), Offset(pos.x + len, pos.y), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
        drawLine(col.copy(alpha = 0.7f * a), Offset(pos.x, pos.y - len), Offset(pos.x, pos.y + len), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
      }
    }
  }
}

/** A field of breathing neon dots with a slow scan line; a touch sends a shockwave that pushes the dots and lights them up. */
internal fun DrawScope.drawNeonPulse(t: Float, tilt: Offset, p: BackdropPalette, d: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val cols = 12
  val spacing = w / cols
  val rows = (h / spacing).toInt() + 1
  val maxR = min(w, h) * 0.85f
  val band = 46.dp.toPx()
  val push = 18.dp.toPx()

  // scan line
  val scanY = h * fract(t * 0.07f)
  drawRect(
    brush = Brush.verticalGradient(
      listOf(Color.Transparent, p.primary.copy(alpha = 0.10f), Color.Transparent),
      startY = scanY - 40.dp.toPx(),
      endY = scanY + 40.dp.toPx(),
    ),
    topLeft = Offset(0f, scanY - 40.dp.toPx()),
    size = Size(w, 80.dp.toPx()),
  )

  for (r in 0 until rows) {
    for (c in 0 until cols) {
      val ox = spacing * (c + 0.5f) + tilt.x * 6.dp.toPx()
      val oy = spacing * (r + 0.5f) + tilt.y * 6.dp.toPx()
      var dx = 0f
      var dy = 0f
      var glow = 0f
      var tone = 0
      for (slot in 0..BURST_SLOTS) {
        val bpx: Float
        val bpy: Float
        val age: Float
        val seed: Int
        if (slot < BURST_SLOTS) {
          bpx = d.burstX[slot]; bpy = d.burstY[slot]; age = t - d.burstT[slot]; seed = d.burstSeed[slot]
        } else {
          if (!d.demo) break
          bpx = w * 0.6f; bpy = h * 0.45f; age = PULSE_LIFE * 0.32f; seed = 41
        }
        if (age < 0f || age > PULSE_LIFE) continue
        val x = age / PULSE_LIFE
        val ring = maxR * easeOut(x)
        val ex = ox - bpx
        val ey = oy - bpy
        val dist = sqrt(ex * ex + ey * ey).coerceAtLeast(1f)
        val delta = (dist - ring) / band
        val infl = exp(-delta * delta) * (1f - x)
        if (infl > 0.01f) {
          dx += ex / dist * infl * push
          dy += ey / dist * infl * push
          if (infl > glow) { glow = infl; tone = seed }
        }
      }
      val breathe = wave01(t * 0.8f + (r + c) * 0.5f)
      val pos = Offset(ox + dx, oy + dy)
      val baseAlpha = 0.14f + 0.12f * breathe
      if (glow > 0f) {
        val col = p.accent(tone + c)
        drawCircle(col.copy(alpha = 0.20f * glow), radius = (7f + 6f * glow).dp.toPx(), center = pos)
        drawCircle(col.copy(alpha = (baseAlpha + 0.8f * glow).coerceAtMost(1f)), radius = (1.5f + 2.2f * glow).dp.toPx(), center = pos)
      } else {
        drawCircle(p.accent(c + r).copy(alpha = baseAlpha), radius = 1.5.dp.toPx(), center = pos)
      }
    }
  }
  forEachTouch(d, t, PULSE_LIFE) { c, x, seed ->
    drawCircle(
      p.accent(seed).copy(alpha = 0.40f * (1f - x)),
      radius = maxR * easeOut(x),
      center = c,
      style = Stroke(width = (2.2f * (1f - x) + 0.6f).dp.toPx()),
    )
  }
}

/** Pastel petals floating up; a touch makes a flower bloom open, turn slowly and drift away. */
internal fun DrawScope.drawBloom(t: Float, tilt: Offset, p: BackdropPalette, d: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  for (i in 0 until 2) {
    val c = Offset(w * (0.25f + 0.5f * i) + tilt.x * 10.dp.toPx(), h * (0.3f + 0.4f * i) + tilt.y * 10.dp.toPx())
    val r = w * 0.7f
    drawCircle(Brush.radialGradient(listOf(p.accent(i + 1).copy(alpha = 0.10f), Color.Transparent), center = c, radius = r), radius = r, center = c)
  }
  for (i in 0 until backdropCount(16, minimum = 9, maximum = 24)) {
    val depth = (i % 3 + 1) / 3f
    val u = fract(hash01(i, 31) + t * (0.012f + 0.012f * hash01(i, 32)))
    val px = w * hash01(i, 33) + sin(u * TAU * 1.6f + i) * 22.dp.toPx() + tilt.x * (8f + 8f * depth).dp.toPx()
    val py = h * (1.08f - 1.16f * u)
    val a = smoothstep(0f, 0.12f, u) * smoothstep(1f, 0.85f, u)
    val s = (7f + 9f * hash01(i, 34)).dp.toPx()
    val deg = t * 20f * (hash01(i, 35) - 0.5f) * 2f + i * 37f
    rotate(deg, pivot = Offset(px, py)) {
      drawOval(p.accent(i).copy(alpha = 0.22f * a), topLeft = Offset(px - s * 0.45f, py - s), size = Size(s * 0.9f, s * 2f))
    }
  }

  val base = min(w, h)
  forEachTouch(d, t, BLOOM_LIFE) { c, x, seed ->
    val grow = smoothstep(0f, 0.38f, x)
    val fade = 1f - smoothstep(0.55f, 1f, x)
    val spin = x * 50f
    val centre = Offset(c.x, c.y - h * 0.05f * smoothstep(0.5f, 1f, x))
    drawCircle(
      Brush.radialGradient(listOf(p.accent(seed).copy(alpha = 0.22f * fade), Color.Transparent), center = centre, radius = base * 0.30f),
      radius = base * 0.30f,
      center = centre,
    )
    for (layer in 0 until 2) {
      val n = if (layer == 0) 8 else 6
      val len = base * (if (layer == 0) 0.15f else 0.09f) * grow
      val wid = len * 0.5f
      if (len <= 0.5f) continue
      for (i in 0 until n) {
        val deg = i * 360f / n + (if (layer == 0) spin else -spin * 1.3f) + (seed % 30).toFloat()
        val col = p.accent(i + layer + seed)
        rotate(deg, pivot = centre) {
          drawOval(col.copy(alpha = (if (layer == 0) 0.40f else 0.52f) * fade), topLeft = Offset(centre.x - wid / 2f, centre.y - len), size = Size(wid, len))
          drawOval(col.copy(alpha = 0.70f * fade), topLeft = Offset(centre.x - wid / 2f, centre.y - len), size = Size(wid, len), style = Stroke(width = 1.dp.toPx()))
        }
      }
    }
    drawCircle(p.ink.copy(alpha = 0.70f * fade), radius = 3.5.dp.toPx() * grow, center = centre)
  }
}

/** Soap bubbles rising; a touch blows a bubble that pops into a ring and droplets, and the bubbles nearby are pushed away. */
internal fun DrawScope.drawBubblePop(t: Float, tilt: Offset, p: BackdropPalette, d: BackdropDrawingState) {
  val w = size.width
  val h = size.height
  val reach = min(w, h) * 0.45f
  for (i in 0 until backdropCount(14, minimum = 8, maximum = 20)) {
    val u = fract(hash01(i, 41) + t * (0.018f + 0.020f * hash01(i, 42)))
    var px = w * (0.06f + 0.88f * hash01(i, 43)) + sin(u * TAU * 2f + i) * 12.dp.toPx() + tilt.x * 10.dp.toPx()
    var py = h * (1.10f - 1.20f * u) + tilt.y * 10.dp.toPx()
    val r = (9f + 26f * hash01(i, 44)).dp.toPx()
    for (slot in 0 until BURST_SLOTS) {
      val age = t - d.burstT[slot]
      if (age < 0f || age > 1.4f) continue
      val k = age / 1.4f
      val ex = px - d.burstX[slot]
      val ey = py - d.burstY[slot]
      val dist = sqrt(ex * ex + ey * ey).coerceAtLeast(1f)
      if (dist < reach) {
        val f = 1f - dist / reach
        val pushPx = f * f * 60.dp.toPx() * (1f - k) * smoothstep(0f, 0.15f, k)
        px += ex / dist * pushPx
        py += ey / dist * pushPx
      }
    }
    val a = smoothstep(0f, 0.08f, u) * smoothstep(1f, 0.9f, u)
    val col = p.accent(i)
    val centre = Offset(px, py)
    drawCircle(col.copy(alpha = 0.06f * a), radius = r, center = centre)
    drawCircle(col.copy(alpha = 0.45f * a), radius = r, center = centre, style = Stroke(width = 1.3.dp.toPx()))
    drawCircle(p.ink.copy(alpha = 0.35f * a), radius = r * 0.16f, center = Offset(px - r * 0.38f, py - r * 0.38f))
  }

  val grow = 0.26f
  forEachTouch(d, t, POP_LIFE) { c, x, seed ->
    val col = p.accent(seed)
    if (x < grow) {
      val k = x / grow
      val r = (8f + 30f * (1f - (1f - k) * (1f - k))).dp.toPx()
      drawCircle(col.copy(alpha = 0.14f), radius = r, center = c)
      drawCircle(col.copy(alpha = 0.70f), radius = r, center = c, style = Stroke(width = 1.6.dp.toPx()))
      drawCircle(p.ink.copy(alpha = 0.50f), radius = r * 0.16f, center = Offset(c.x - r * 0.38f, c.y - r * 0.38f))
    } else {
      val k = (x - grow) / (1f - grow)
      val ease = easeOut(k)
      val r0 = 38.dp.toPx()
      drawCircle(
        col.copy(alpha = 0.55f * (1f - k)),
        radius = r0 * (1f + 0.9f * ease),
        center = c,
        style = Stroke(width = (2f * (1f - k) + 0.4f).dp.toPx()),
      )
      for (i in 0 until 12) {
        val ang = i * TAU / 12f + hash01(i, seed) * 0.4f
        val inner = r0 * (0.9f + 0.5f * ease)
        val len = (6f + 8f * hash01(i, seed + 2)).dp.toPx() * (1f - k)
        val dirX = cos(ang)
        val dirY = sin(ang)
        drawLine(
          col.copy(alpha = 0.80f * (1f - k)),
          Offset(c.x + dirX * inner, c.y + dirY * inner),
          Offset(c.x + dirX * (inner + len + 20.dp.toPx() * ease), c.y + dirY * (inner + len + 20.dp.toPx() * ease)),
          strokeWidth = 1.6.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
      for (i in 0 until 8) {
        val ang = i * TAU / 8f + 0.3f + hash01(i, seed + 4) * 0.5f
        val dist = r0 * (1.1f + 0.8f * ease)
        val pos = Offset(c.x + cos(ang) * dist, c.y + sin(ang) * dist + 30.dp.toPx() * k * k)
        drawCircle(p.accent(seed + i).copy(alpha = 0.8f * (1f - k)), radius = (1.4f + 1.4f * hash01(i, seed + 6)).dp.toPx(), center = pos)
      }
    }
  }
}
