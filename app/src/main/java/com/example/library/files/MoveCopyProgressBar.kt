package com.example.ui.components

import com.example.ui.theme.Neutral

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BottomMoveCopyProgressBar(
    isDark: Boolean,
    alphaRatio: Float = 0.85f,
    isMove: Boolean = true,
    isDelete: Boolean = false,
    customActionText: String? = null,
    customIcon: ImageVector? = null,
    progressFraction: Float,
    currentItemName: String = "",
    completedCount: Int = 0,
    totalCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDark) {
        if (alphaRatio >= 0.99f) Neutral.D0F172A
        else Neutral.D0F172A.copy(alpha = (0.70f + 0.30f * alphaRatio).coerceIn(0.40f, 1.0f))
    } else {
        if (alphaRatio >= 0.99f) Neutral.FFFFFF
        else Neutral.FFFFFF.copy(alpha = (0.75f + 0.25f * alphaRatio).coerceIn(0.45f, 1.0f))
    }
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val barShape = RoundedCornerShape(20.dp)

    val animatedFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "progressFraction"
    )

    // Looping animations: shimmer sweeping over the fill, and a per-action icon motion
    // (trash can shakes, move icon slides, copy icon pulses).
    val infinite = rememberInfiniteTransition(label = "opBar")
    val shimmer by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "shimmer"
    )
    val iconPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isDelete) 420 else 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPhase"
    )

    val percentage = (animatedFraction * 100f).coerceIn(0f, 100f).toInt()
    val actionText = customActionText ?: when {
        isDelete -> "Deleting..."
        isMove -> "Moving..."
        else -> "Copying..."
    }
    val icon = customIcon ?: when {
        isDelete -> Icons.Outlined.DeleteOutline
        isMove -> Icons.Outlined.DriveFileMove
        else -> Icons.Outlined.ContentCopy
    }
    val tintColor = when {
        isDelete -> Color(0xFFEF4444)
        isMove -> AccentSkyBlue
        else -> Color(0xFF818CF8)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            // Liquid Glass ON: same frosted glass bar as the selection toolbar. OFF: the original look.
            .liquidGlassToolbarOr(barShape, isDark) {
                shadow(
                    elevation = 18.dp,
                    shape = barShape,
                    ambientColor = if (isDelete) Color(0xFFEF4444).copy(alpha = 0.35f) else AccentSkyBlue.copy(alpha = 0.35f),
                    spotColor = if (isDelete) Color(0xFFDC2626).copy(alpha = 0.40f) else AccentPink.copy(alpha = 0.40f)
                )
                    .clip(barShape)
                    .background(barBg)
                    .border(
                        width = 1.5.dp,
                        brush = if (isDelete) androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444).copy(alpha = 0.5f)) else AccentGradient,
                        shape = barShape
                    )
            }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    StyledIcon(imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                when {
                                    isDelete -> rotationZ = (iconPhase - 0.5f) * 24f
                                    isMove -> translationX = (iconPhase - 0.5f) * 6.dp.toPx()
                                    else -> {
                                        val sc = 0.9f + 0.2f * iconPhase
                                        scaleX = sc
                                        scaleY = sc
                                    }
                                }
                            }
                    )
                    Text(
                        text = actionText,
                        color = primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currentItemName.isNotBlank()) {
                        Text(
                            text = currentItemName,
                            color = secondaryText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "$percentage%",
                    color = tintColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.20f) else Neutral.S64748B.copy(alpha = 0.28f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedFraction.coerceIn(0.01f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDelete) androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444)) else AccentGradient)
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            val x = shimmer * w
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                    startX = x - w * 0.3f,
                                    endX = x + w * 0.3f
                                )
                            )
                        }
                )
            }
        }
    }
}
