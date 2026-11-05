package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Two colours taken from a thumbnail: [top] for the upper part of the screen, [bottom] for the lower part. */
data class ArtworkColors(val top: Color, val bottom: Color)

/**
 * Picks a lively dominant colour from the artwork for the top of the player and a different, deeper
 * colour (the next most prominent hue, or a shifted/darker version of the first) for the bottom.
 * No external palette library: the bitmap is shrunk to 32x32 and its pixels are bucketed by hue.
 */
fun extractArtworkColors(source: Bitmap): ArtworkColors? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    val small = try {
        Bitmap.createScaledBitmap(source, 32, 32, true)
    } catch (_: Throwable) {
        return null
    }
    val bins = 12
    val weight = FloatArray(bins)
    val hueSum = FloatArray(bins)
    val satSum = FloatArray(bins)
    val valSum = FloatArray(bins)
    val count = FloatArray(bins)
    val hsv = FloatArray(3)
    for (y in 0 until small.height) {
        for (x in 0 until small.width) {
            val px = small.getPixel(x, y)
            if ((px ushr 24) < 200) continue
            android.graphics.Color.colorToHSV(px, hsv)
            val s = hsv[1]
            val v = hsv[2]
            if (v < 0.12f) continue                       // near black carries no colour
            if (s < 0.10f && v > 0.92f) continue          // near white
            val bin = ((hsv[0] / 360f) * bins).toInt().coerceIn(0, bins - 1)
            // vivid, reasonably bright pixels count more than dull grey ones
            val w = (0.15f + s) * (0.35f + v)
            weight[bin] += w
            hueSum[bin] += hsv[0] * w
            satSum[bin] += s * w
            valSum[bin] += v * w
            count[bin] += w
        }
    }
    small.recycle()
    val order = (0 until bins).filter { weight[it] > 0f }.sortedByDescending { weight[it] }
    if (order.isEmpty()) return null

    fun binColor(bin: Int): FloatArray {
        val w = count[bin].coerceAtLeast(0.0001f)
        return floatArrayOf(hueSum[bin] / w, satSum[bin] / w, valSum[bin] / w)
    }

    val first = binColor(order[0])
    // second colour: the strongest bin whose hue is clearly different from the first
    val secondBin = order.drop(1).firstOrNull { b ->
        val h = binColor(b)[0]
        val d = abs(h - first[0]).let { if (it > 180f) 360f - it else it }
        d >= 40f && weight[b] >= weight[order[0]] * 0.12f
    }
    val second = secondBin?.let { binColor(it) }
        ?: floatArrayOf((first[0] + 38f) % 360f, first[1], first[2])

    fun tune(c: FloatArray, minS: Float, maxS: Float, minV: Float, maxV: Float): Color =
        Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(c[0], c[1].coerceIn(minS, maxS), c[2].coerceIn(minV, maxV))
            )
        )

    return ArtworkColors(
        top = tune(first, 0.40f, 0.85f, 0.55f, 0.90f),
        bottom = tune(second, 0.35f, 0.80f, 0.30f, 0.62f),
    )
}
