package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Lightweight animated lyric-line placeholders shown while providers are racing. */
@Composable
fun LyricsPlaceholderLines(
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "lyrics-placeholder")
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "lyrics-placeholder-alpha",
    )
    val widths = listOf(0.62f, 0.88f, 0.74f, 0.94f, 0.53f)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = if (compact) 10.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp),
    ) {
        widths.forEachIndexed { index, fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(if (compact) 7.dp else 9.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.Gray.copy(alpha = alpha - (index % 2) * 0.04f)),
            )
        }
    }
}
