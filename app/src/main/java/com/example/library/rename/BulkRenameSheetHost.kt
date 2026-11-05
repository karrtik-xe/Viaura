package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.screens.LocalGlassBlurTransparency
import com.example.ui.screens.VideoItem
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalAppFontFamily
import com.example.ui.theme.Neutral
import com.example.util.BulkCaseFormat
import com.example.util.BulkRenameConfig
import com.example.util.BulkRenameEngine
import com.example.util.BulkRenameItem
import com.example.util.BulkRenameMode
import com.example.util.BulkRenameStatus
import com.example.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

internal const val ROW_H_DP = 50

internal const val ROW_GAP_DP = 4

internal const val LIST_PAD_DP = 8

internal val ConflictRed = Color(0xFFEF4444)

internal val WarnAmber = Color(0xFFF59E0B)

/**
 * Bulk Rename bottom sheet (opens from the Rename button of the selection bar when 2+ videos are selected).
 * Same sheet look as the folder / playlist sheets (normal UI in normal mode, liquid glass in Liquid Glass mode).
 */
@Composable
fun BulkRenameSheet(
    isDark: Boolean,
    glassBlurTransparency: Float,
    videos: List<VideoItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<VideoItem, String>>) -> Unit
) {
    // Same "seat" as the Settings dashboard: a Dialog sheet that stops under the status bar (never reaches the top of the
    // screen), is closed ONLY by the handle line (tap it or drag it down) / Back, and ignores every touch outside of it.
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val currentDensity = LocalDensity.current
    val appScale = LocalAppScale.current
    val screenHeightPx = with(currentDensity) { (configuration.screenHeightDp + 100).dp.toPx() }
    val coroutineScope = rememberCoroutineScope()
    val sheetDragOffsetY = remember { androidx.compose.animation.core.Animatable(screenHeightPx) }
    val scrimAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }
    var windowBlurReady by remember { mutableStateOf(false) }
    val sliderFactor = (glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        launch {
            scrimAlpha.animateTo(1f, androidx.compose.animation.core.tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
        sheetDragOffsetY.animateTo(0f, androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        windowBlurReady = true
    }

    val dismissWithAnimation: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                try {
                    launch {
                        scrimAlpha.animateTo(0f, androidx.compose.animation.core.tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                    }
                    sheetDragOffsetY.animateTo(screenHeightPx, androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                } catch (_: Throwable) {
                } finally {
                    onDismiss()
                }
            }
        }
    }

    androidx.activity.compose.BackHandler { dismissWithAnimation() }

    // Same size rule as the Settings dashboard: follows the app-size slider live and renders a little smaller than the app behind it
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, appScale) {
        fun factorFor(scale: Float) = (0.85f + 0.15f * ((scale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
        val ratio = factorFor(appScale - 29f) / factorFor(appScale)
        androidx.compose.ui.unit.Density(
            density = currentDensity.density * ratio,
            fontScale = currentDensity.fontScale * ratio
        )
    }

    val handleDragModifier = Modifier.pointerInput(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch {
                detectTapGestures { dismissWithAnimation() }
            }
            launch {
                detectVerticalDragGestures(
                    onDragStart = { },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0f || sheetDragOffsetY.value > 0f) {
                            change.consume()
                            coroutineScope.launch {
                                sheetDragOffsetY.snapTo((sheetDragOffsetY.value + dragAmount).coerceAtLeast(0f))
                            }
                        }
                    },
                    onDragEnd = {
                        if (sheetDragOffsetY.value > 50f) {
                            dismissWithAnimation()
                        } else {
                            coroutineScope.launch {
                                sheetDragOffsetY.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = { coroutineScope.launch { sheetDragOffsetY.animateTo(0f) } }
                )
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.DisposableEffect(view, isDark) {
            configureEdgeToEdgeDialogWindow(view = view, isLightStatusBars = !isDark, isLightNavBars = !isDark)
            onDispose { }
        }
        if (windowBlurReady) LiquidDialogBlur()

        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalGlassBlurTransparency provides glassBlurTransparency
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color.Black.copy(alpha = (0.35f * sliderFactor * scrimAlpha.value).coerceIn(0f, 0.40f)))
                    }
                    // touches on the backdrop are swallowed: they never close the sheet
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                        .imePadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = sheetDragOffsetY.value }
                            .glassPanelSurface(sheetShape, isDark, glassBlurTransparency)
                            .pointerInput(Unit) { detectTapGestures { } }
                    ) {
                        // HANDLE: tap it or drag it down = close (the ONLY drag-to-close area)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .then(handleDragModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(secondaryText.copy(alpha = 0.5f))
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            BulkRenameBody(isDark, videos, dismissWithAnimation, onConfirm)
                        }
                    }
                }
            }
        }
    }
}
