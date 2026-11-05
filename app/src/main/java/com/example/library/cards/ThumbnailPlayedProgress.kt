package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentGradient

/**
 * Played / watched progress drawn ON the thumbnail, only while Settings > Appearance > Remove Card UI is ON
 * (with the card gone there is no card fill that could show it). It is a thin bar along the bottom edge, so the
 * thumbnail itself stays clear. Call it inside the thumbnail's Box (clipped to the thumbnail shape).
 */
@Composable
fun BoxScope.ThumbnailPlayedProgress(progress: Float, enabled: Boolean = true) {
    val fraction = progress.coerceIn(0f, 1f)
    if (!CardStyleSettings.removeCards || !enabled || fraction <= 0f) return
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.Black.copy(alpha = 0.40f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(AccentGradient)
        )
    }
}
