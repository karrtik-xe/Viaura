package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.example.ui.state.SystemTone
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.MutedPalette
import com.example.ui.theme.SystemTonePalette

/** Tone (hue / saturation / tint depth) that the System surfaces take from one palette swatch. */
private fun toneOfSwatch(argb: Long): SystemTone {
    val out = FloatArray(3)
    ColorUtils.colorToHSL(Color(argb).toArgb(), out)
    return SystemTone(out[0], out[1].coerceIn(SystemTonePalette.MIN_SAT, SystemTonePalette.MAX_SAT), (1f - out[2]).coerceIn(0.35f, 0.65f))
}

/**
 * Inline panel (opened under the Light / Dark / System switch by a double tap on "System", no pop-up).
 * ONE muted colour can be picked; it tints the page background, cards and sheets. Applied live.
 */
@Composable
fun SystemTonePanel(
    initial: SystemTone?,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onApply: (SystemTone) -> Unit,
    onUseWallpaper: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var picked by remember {
        mutableStateOf(
            initial?.let { t ->
                (0xFF000000L or (ColorUtils.HSLToColor(floatArrayOf(t.hue.mod(360f), t.sat.coerceIn(0f, 1f), (1f - t.depth).coerceIn(0f, 1f))).toLong() and 0xFFFFFFL))
            }
        )
    }
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(cardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("System colour", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText)
        // Same 3-arc round picker as the theme colour editor, limited to muted & desaturated colours.
        // The colour is applied when the finger is lifted (the centre circle follows the finger live).
        MutedCircularColorPickerCard(
            title = if (picked == null) "Wallpaper colours" else "System Color",
            selectedArgb = picked,
            centerColor = picked?.let { Color(it.toInt()) } ?: secondaryText.copy(alpha = 0.35f),
            onPick = { argb -> picked = argb },
            onPickFinished = { picked?.let { onApply(toneOfSwatch(it)) } }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { picked = null; onUseWallpaper(); onClose() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Wallpaper colours", fontSize = 12.5.sp, color = secondaryText, fontWeight = FontWeight.SemiBold) }
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentGradient)
                    .clickable { onClose() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Done", fontSize = 12.5.sp, color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}
