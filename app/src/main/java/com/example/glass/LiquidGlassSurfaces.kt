package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentMid
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Liquid Glass design token constants:
 * Matches SettingsSheet backdrop and opacity level exactly (0.65f dark / 0.70f light frosted glass, soft scrim)
 */
object LiquidGlassDefaults {

    val DarkGlassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            AccentSkyBlue.copy(alpha = 0.40f),
            AccentMid.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    val LightGlassBorderBrush = Brush.linearGradient(
        colors = listOf(
            AccentSkyBlue.copy(alpha = 0.45f),
            AccentMid.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.60f),
            AccentSkyBlueStrong.copy(alpha = 0.30f)
        )
    )

    fun getDynamicGlassFill(isDark: Boolean, transparencyPercent: Float = 65f): Color {
        val alphaRatio = (transparencyPercent / 100f).coerceIn(0.10f, 1.0f)
        return if (isDark) {
            if (alphaRatio >= 0.99f) Color(0xFF0B1120)
            else Color(0xFF0B1120).copy(alpha = (0.12f + 0.85f * alphaRatio).coerceIn(0.10f, 1.0f))
        } else {
            if (alphaRatio >= 0.99f) Neutral.FFFFFF
            else Neutral.FFFFFF.copy(alpha = (0.12f + 0.85f * alphaRatio).coerceIn(0.10f, 1.0f))
        }
    }

}

@Composable
fun LiquidGlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    transparencyPercent: Float = 65f,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val alphaRatio = (transparencyPercent / 100f).coerceIn(0.10f, 1.0f)
    val fill = LiquidGlassDefaults.getDynamicGlassFill(isDark, transparencyPercent)
    val borderBrush = if (isDark) LiquidGlassDefaults.DarkGlassBorderBrush else LiquidGlassDefaults.LightGlassBorderBrush

    Box(
        modifier = modifier
            .liquidGlassOr(shape, isDark, transparencyPercent, elevation = elevation, backdropBlur = false) {
                this
                    .then(
                        if (alphaRatio > 0.35f) {
                            Modifier.shadow(
                                elevation = elevation * alphaRatio,
                                shape = shape,
                                ambientColor = Color.Black.copy(alpha = 0.20f * alphaRatio),
                                spotColor = Color.Black.copy(alpha = 0.30f * alphaRatio)
                            )
                        } else Modifier
                    )
                    .clip(shape)
                    .background(fill)
                    .border(1.2.dp, borderBrush, shape)
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun LiquidGlassDialog(
    isDark: Boolean,
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            configureEdgeToEdgeDialogWindow(
                view = view,
                isLightStatusBars = !isDark,
                isLightNavBars = !isDark
            )
            onDispose {}
        }

        // Clean transparent backdrop behind popup (no dark UI layer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            // Click interceptor so clicking inside dialog does not dismiss
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                content = content
            )
        }
    }
}
