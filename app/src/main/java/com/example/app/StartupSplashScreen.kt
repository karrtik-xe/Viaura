package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradientColors
import com.example.ui.theme.LocalAppFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_APP_NAME = "Viaura"

/**
 * Launch animation shown right after the app is opened from the home screen.
 * A big logo springs in with a glow + moving shine, the app name appears letter by letter under it, and the
 * whole thing sits on a see-through "glass" veil. [onBlurBackground] tells the caller when the real UI behind the
 * splash should be blurred (true while the splash is shown, false as soon as it starts to fade out).
 */
@Composable
fun AppLaunchSplash(
    isDark: Boolean,
    onBlurBackground: (Boolean) -> Unit = {},
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    val logoScale = remember { Animatable(0.62f) }
    val logoAlpha = remember { Animatable(1f) }
    val logoSpin = remember { Animatable(-40f) }

    LaunchedEffect(Unit) {
        onBlurBackground(true)
        // logo: spring in (scale + spin + fade)
        launch { logoSpin.animateTo(0f, tween(700, easing = FastOutSlowInEasing)) }
        logoScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        delay(1350)
        visible = false
        onBlurBackground(false)
        delay(450)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "launchSplash")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashPhase"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "splashSweep"
    )
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashGlow"
    )

    val colors = AccentGradientColors
    val loopColors = colors + colors.first()
    val veilBase = if (isDark) Color(0xFF0B1020) else Color(0xFFF8FAFF)
    val appFont = LocalAppFontFamily.current

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(420)) + scaleOut(targetScale = 1.06f, animationSpec = tween(420))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // swallow touches while the splash is visible
                .pointerInput(Unit) { detectTapGestures { } }
                // glass veil over the (blurred) UI: still see-through so the folders behind stay visible
                .background(
                    Brush.verticalGradient(
                        listOf(veilBase.copy(alpha = 0.36f), veilBase.copy(alpha = 0.50f))
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.first().copy(alpha = 0.16f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                    // pulsing glow behind the logo
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .graphicsLayer {
                                scaleX = logoScale.value * (0.9f + 0.15f * glow)
                                scaleY = logoScale.value * (0.9f + 0.15f * glow)
                            }
                            .alpha(logoAlpha.value * 0.55f * glow)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        colors.last().copy(alpha = 0.55f),
                                        colors.first().copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_abouticon),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .size(132.dp)
                            .graphicsLayer {
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                                rotationZ = logoSpin.value
                                alpha = logoAlpha.value
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                val w = size.width
                                val h = size.height
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colors = loopColors,
                                        start = Offset(-w * phase, 0f),
                                        end = Offset(w * (2f - phase), h)
                                    ),
                                    blendMode = BlendMode.SrcIn
                                )
                                val bandX = -w + sweep * w * 3f
                                drawRect(
                                    brush = Brush.linearGradient(
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent),
                                        start = Offset(bandX, 0f),
                                        end = Offset(bandX + w * 0.9f, h)
                                    ),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // app name: letters rise + fade in one after another, gradient text with a moving shine
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    SPLASH_APP_NAME.forEachIndexed { index, ch ->
                        val reveal = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(520L + index * 85L)
                            reveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                        }
                        Text(
                            text = ch.toString(),
                            style = TextStyle(
                                fontFamily = appFont,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp,
                                brush = Brush.linearGradient(
                                    colors = loopColors,
                                    start = Offset(-200f * phase + index * 40f, 0f),
                                    end = Offset(400f * (1f + (1f - phase)) + index * 40f, 80f)
                                )
                            ),
                            modifier = Modifier.graphicsLayer {
                                alpha = reveal.value
                                translationY = (1f - reveal.value) * 36f
                                val s = 0.7f + 0.3f * reveal.value
                                scaleX = s
                                scaleY = s
                            }
                        )
                    }
                }
            }
        }
    }
}
