package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.download.AudioChoice
import com.example.download.DownloadPlan
import com.example.download.LinkInfo
import com.example.download.MediaProbe
import com.example.download.PlanSubtitle
import com.example.download.VideoChoice
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.DownloadFormat
import com.example.util.DownloadManager
import com.example.util.DownloadSourceType
import com.example.util.NetworkSettings
import kotlinx.coroutines.launch

/**
 * Translucent bottom sheet used by every download panel (same glass colour as the other app sheets).
 *
 * Built exactly like the Settings dashboard: a Dialog + our own sliding panel instead of a Material ModalBottomSheet, so
 * ONLY the small handle bar (drag down / tap) closes it. Touching, swiping or scrolling anywhere else - header, tabs,
 * the middle of the content, the sides - never closes it and never plays a dismiss animation, so the person can scroll
 * down to the lower options and back up freely. A tap on the dark area above the sheet and the Back button still close it.
 */
@Composable
fun DownloadGlassSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    // normal height of the sheet (65 % of the screen) and how far it can be pulled up by holding the handle (to 93 %)
    val baseSheetHeightPx = with(density) { (configuration.screenHeightDp * 0.65f).dp.toPx() }
    val maxExtraPx = with(density) { (configuration.screenHeightDp * 0.93f).dp.toPx() } - baseSheetHeightPx
    val extraHeight = remember { Animatable(0f) }
    // room that always stays free under the last option: the height of the gesture / navigation bar (never less than 32dp,
    // some phones report 0 inside a dialog) so nothing is cut off at the bottom
    val navBarRoom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }.coerceAtLeast(32.dp)
    val screenHeightPx = with(density) { (configuration.screenHeightDp + 100).dp.toPx() }
    val scope = rememberCoroutineScope()
    val sheetOffsetY = remember { Animatable(screenHeightPx) }
    val scrimAlpha = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { scrimAlpha.animateTo(1f, tween(durationMillis = 240, easing = FastOutSlowInEasing)) }
        sheetOffsetY.animateTo(0f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
    }

    val dismissWithAnimation: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            scope.launch {
                try {
                    launch { scrimAlpha.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing)) }
                    sheetOffsetY.animateTo(screenHeightPx, tween(durationMillis = 260, easing = FastOutSlowInEasing))
                } catch (_: Throwable) {
                } finally {
                    onDismiss()
                }
            }
        }
    }

    // swallows every vertical scroll / fling that is left over from the content so it can never travel up and close the sheet
    val consumeLeftovers = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view, isDark) {
            configureEdgeToEdgeDialogWindow(view = view, isLightStatusBars = !isDark, isLightNavBars = !isDark)
            onDispose { }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind { drawRect(Color.Black.copy(alpha = 0.35f * scrimAlpha.value)) }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismissWithAnimation() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, sheetOffsetY.value.roundToInt()) }
                    .glassPanelSurface(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark, glassBlurTransparency)
                    // taps inside the panel are swallowed, they must never reach the dark area behind it
                    .pointerInput(Unit) { detectTapGestures { } }
            ) {
                // the ONLY part that closes the sheet: tap or drag down on the handle
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(34.dp)
                            .pointerInput(Unit) {
                                coroutineScope {
                                    launch { detectTapGestures { dismissWithAnimation() } }
                                    launch {
                                        detectVerticalDragGestures(
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                scope.launch {
                                                    if (dragAmount < 0f) {
                                                        // finger moves up: first undo a downward pull, then make the sheet taller
                                                        if (sheetOffsetY.value > 0f) {
                                                            sheetOffsetY.snapTo((sheetOffsetY.value + dragAmount).coerceAtLeast(0f))
                                                        } else {
                                                            extraHeight.snapTo((extraHeight.value - dragAmount).coerceIn(0f, maxExtraPx))
                                                        }
                                                    } else {
                                                        // finger moves down: first make the sheet shorter again, then slide it away
                                                        if (extraHeight.value > 0f) {
                                                            extraHeight.snapTo((extraHeight.value - dragAmount).coerceIn(0f, maxExtraPx))
                                                        } else {
                                                            sheetOffsetY.snapTo(sheetOffsetY.value + dragAmount)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (sheetOffsetY.value > 50f) {
                                                    dismissWithAnimation()
                                                } else {
                                                    scope.launch { sheetOffsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                                                    // settle: pulled far enough -> stays tall, otherwise returns to the normal height
                                                    scope.launch {
                                                        val target = if (extraHeight.value > maxExtraPx * 0.3f) maxExtraPx else 0f
                                                        extraHeight.animateTo(target, tween(220, easing = FastOutSlowInEasing))
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                scope.launch { sheetOffsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                                                scope.launch { extraHeight.animateTo(0f, tween(180, easing = FastOutSlowInEasing)) }
                                            }
                                        )
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(secondaryText.copy(alpha = 0.4f))
                        )
                    }
                }
                var contentHeightPx by remember { mutableIntStateOf(0) }
                PanelScaled {
                    // everything below is measured in real pixels, so the sheet is exactly the same size on every app-scale setting
                    val d = LocalDensity.current
                    val bottomRoomPx = with(d) { (22.dp + navBarRoom).toPx() }
                    val maxHeightDp = with(d) { (baseSheetHeightPx + extraHeight.value).toDp() }
                    // when the content is taller than the sheet, extra empty room is added at the very end so the last options
                    // (quality box, link ...) can be scrolled well above the bottom edge instead of staying cut off
                    val overflows = contentHeightPx + bottomRoomPx > baseSheetHeightPx * 0.85f
                    // short content: the pulled-up height is filled with free room, so the sheet follows the finger
                    val growBy = if (contentHeightPx + bottomRoomPx < baseSheetHeightPx) with(d) { extraHeight.value.toDp() } else 0.dp
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxHeightDp)
                            .nestedScroll(consumeLeftovers)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, bottom = 22.dp + navBarRoom)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().onSizeChanged { contentHeightPx = it.height },
                            content = content
                        )
                        if (overflows) Spacer(modifier = Modifier.height(80.dp))
                        if (growBy > 0.dp) Spacer(modifier = Modifier.height(growBy))
                    }
                }
            }
        }
    }
}
