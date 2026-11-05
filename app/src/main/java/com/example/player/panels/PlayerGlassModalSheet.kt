package com.example.ui.components

import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlueStrong
import com.example.ui.theme.isAppInDarkTheme
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.example.player.EqualizerPreset
import com.example.player.TrackAudioConfig
import com.example.player.AudioChannelMode
import com.example.player.VideoFilterPreset
import com.example.player.ManualVideoAdjustments
import com.example.ui.state.PlayerLayoutConfig
import com.example.ui.state.SeekbarStyle
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Environment
import com.example.player.PlayerMediaTrack
import com.example.player.PlayerVideoChapter
import com.example.ai.AiFeaturesSettingsStore
import com.example.ai.TrackTranslationStatus
import com.example.ai.TranslateLanguages
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import com.example.ui.screens.AspectRatioMode
import com.example.ui.screens.DecoderMode
import com.example.ui.screens.PlayerRepeatMode
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.screens.VideoItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Context
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PlayerGlassModalSheet(
    panelKey: String = "default_panel",
    title: String? = null,
    icon: ImageVector? = null,
    badgeText: String? = null,
    isLandscape: Boolean = false,
    openOnRightSide: Boolean = false,
    /** Full screen width, bottom-anchored (used by the audio player's Equalizer / Sleep timer in every orientation). */
    fullWidth: Boolean = false,
    heightFraction: Float? = 0.58f,
    glassBlurTransparency: Float = 85f,
    /**
     * Audio player panels: the sheet gets exactly the same surface as every other app sheet (glassPanelColor + Liquid Glass
     * sheen / border, no extra shadow) and lives in its own dialog window, so Liquid Glass can blur the player behind it.
     * The video player keeps its own look (it has no window of its own and blurs the live video copy instead).
     */
    appPanelGlass: Boolean = fullWidth,
    onDismissRequest: () -> Unit,
    headerStartContent: (@Composable () -> Unit)? = null,
    headerEndContent: (@Composable () -> Unit)? = null,
    headerCustomRow: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (appPanelGlass || LocalForcePanelDialog.current) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false
            )
        ) {
            EdgeToEdgeSheetWindowEffect()
            // Liquid Glass: blurred player behind the sheet (no-op when the style is OFF)
            LiquidDialogBlur()
            PlayerGlassModalSheetContent(
                panelKey, title, icon, badgeText, isLandscape, openOnRightSide, fullWidth, heightFraction,
                glassBlurTransparency, true, onDismissRequest, headerStartContent, headerEndContent, headerCustomRow, content
            )
        }
    } else {
        PlayerGlassModalSheetContent(
            panelKey, title, icon, badgeText, isLandscape, openOnRightSide, fullWidth, heightFraction,
            glassBlurTransparency, false, onDismissRequest, headerStartContent, headerEndContent, headerCustomRow, content
        )
    }
}

@Composable
private fun PlayerGlassModalSheetContent(
    panelKey: String,
    title: String?,
    icon: ImageVector?,
    badgeText: String?,
    isLandscape: Boolean,
    openOnRightSide: Boolean,
    fullWidth: Boolean,
    heightFraction: Float?,
    glassBlurTransparency: Float,
    appPanelGlass: Boolean,
    onDismissRequest: () -> Unit,
    headerStartContent: (@Composable () -> Unit)?,
    headerEndContent: (@Composable () -> Unit)?,
    headerCustomRow: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onDismissRequest)

    val context = LocalContext.current
    val effectiveKey = if (panelKey != "default_panel") panelKey else (title ?: "default_panel")
    val defaultFraction = heightFraction ?: 0.58f
    val initialFraction = remember(effectiveKey) {
        PlayerPanelHeightStore.getHeight(context, effectiveKey, defaultFraction)
    }
    var currentHeightFraction by remember(effectiveKey) { mutableFloatStateOf(initialFraction) }
    val haptic = LocalHapticFeedback.current

    val animatedHeightFraction by animateFloatAsState(
        targetValue = currentHeightFraction.coerceIn(0.35f, 0.95f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "SheetHeightFraction"
    )

    val isDark = isAppInDarkTheme()
    val liquidGlass = LocalLiquidGlass.current
    // Liquid Glass keeps the text readable by tint (dark / light glass), so the old "low opacity = white text" switch is not used
    val isLowGlassOpacity = glassBlurTransparency < 25f && !liquidGlass && !appPanelGlass
    val sheetNormalTextColor = if (isDark || isLowGlassOpacity) Neutral.F8FAFC else Neutral.D1E293B
    val sheetSecondaryTextColor = if (isDark || isLowGlassOpacity) Neutral.S94A3B8 else Neutral.S64748B

    val alphaRatio = (glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)
    // lighter than other panels: the blurred live video shows through (same glass as the player controls)
    val sheetGlassFraction = ((glassBlurTransparency - 10f) / 90f).coerceIn(0f, 1f)
    val liquidFill = if (liquidGlass) liquidGlassBase(isDark).copy(
        alpha = if (isDark) 0.30f + 0.34f * sheetGlassFraction else 0.42f + 0.30f * sheetGlassFraction
    ) else null
    val dynamicGlassGradient = remember(alphaRatio, isLowGlassOpacity, isDark, liquidFill) {
        if (liquidFill != null) {
            SolidColor(liquidFill)
        } else if (isDark) {
            if (isLowGlassOpacity) {
                SolidColor(Neutral.D090D16.copy(alpha = (alphaRatio * 0.45f).coerceIn(0.15f, 0.40f)))
            } else if (alphaRatio >= 0.99f) {
                SolidColor(Neutral.D090D16)
            } else {
                SolidColor(Neutral.D090D16.copy(alpha = alphaRatio))
            }
        } else if (isLowGlassOpacity) {
            SolidColor(Neutral.D0F172A.copy(alpha = (0.50f + alphaRatio * 0.40f).coerceIn(0.50f, 0.75f)))
        } else if (alphaRatio >= 0.99f) {
            SolidColor(Neutral.FFFFFF)
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Neutral.FFFFFF.copy(alpha = alphaRatio),
                    Neutral.F8FAFC.copy(alpha = (alphaRatio * 0.95f).coerceIn(0.10f, 1.0f))
                )
            )
        }
    }
    // Audio player panels (Equalizer / Sleep timer): EXACTLY the same background as the "Up Next" queue sheet and every other
    // app sheet = glassPanelColor():
    //   Liquid Glass ON  -> frosted translucent glass fill (+ sheen and highlight border below, blurred player behind the dialog)
    //   Liquid Glass OFF -> the normal solid panel colour (dark / ice / white as per the app theme)
    // It is painted with drawBehind (a plain solid colour), so the fill can never be lost.
    val appPanelFill: Color = glassPanelColor(isDark, glassBlurTransparency)
    val dynamicBorderColor = remember(alphaRatio, isLowGlassOpacity, isDark) {
        if (isDark) {
            Color.White.copy(alpha = (0.12f + 0.18f * alphaRatio).coerceIn(0.15f, 0.35f))
        } else if (isLowGlassOpacity) {
            Color.White.copy(alpha = 0.35f)
        } else if (alphaRatio >= 0.98f) {
            Neutral.CBD5E1
        } else {
            Color.White.copy(alpha = (alphaRatio * 0.80f).coerceIn(0.15f, 0.95f))
        }
    }

    // Window bounds of the backdrop and of the sheet card: a tap that lands on the sheet (e.g. the +/- buttons of the
    // Sleep timer) must never be treated as an "outside" tap, whatever the gesture order of the children is.
    var backdropOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var sheetWindowBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val latestDismiss by rememberUpdatedState(onDismissRequest)

    // Direct in-tree overlay - backdrop consumes touches and dismisses when tapped outside
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { backdropOrigin = it.positionInWindow() }
            .background(if (appPanelGlass) Color.Black.copy(alpha = 0.35f) else ScrimColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val bounds = sheetWindowBounds
                        val inWindow = androidx.compose.ui.geometry.Offset(
                            tapOffset.x + backdropOrigin.x,
                            tapOffset.y + backdropOrigin.y
                        )
                        if (bounds == null || !bounds.contains(inWindow)) latestDismiss()
                    }
                )
            },
        contentAlignment = if (openOnRightSide) Alignment.BottomEnd else Alignment.BottomCenter
    ) {
        val sheetShape = if (appPanelGlass && !(openOnRightSide && !fullWidth)) {
            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        } else if (openOnRightSide && !fullWidth) {
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 16.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        } else {
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            )
        }

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            Box(
                modifier = if (fullWidth) {
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedHeightFraction)
                } else if (openOnRightSide) {
                    Modifier
                        .fillMaxWidth(if (isLandscape) 0.45f else 0.82f)
                        .widthIn(max = if (isLandscape) 380.dp else 330.dp)
                        .fillMaxHeight(animatedHeightFraction)
                } else {
                    Modifier
                        .fillMaxWidth(if (isLandscape) 0.60f else 1.0f)
                        .widthIn(max = if (isLandscape) 520.dp else 600.dp)
                        .fillMaxHeight(animatedHeightFraction)
                }
                    .onGloballyPositioned { sheetWindowBounds = it.boundsInWindow() }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { /* Consume clicks completely inside sheet so they never bleed into backdrop or video controls */ }
                        )
                    }
                    .pointerInput(effectiveKey) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                            var totalDragY = 0f
                            var totalDragX = 0f
                            var isResizing = false
                            val pointerId = down.id

                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                if (!change.pressed) {
                                    // Pointer released or cancelled
                                    if (isResizing) {
                                        PlayerPanelHeightStore.saveHeight(context, effectiveKey, currentHeightFraction)
                                    }
                                    break
                                }

                                if (change.isConsumed) {
                                    // Child view consumed the event (e.g. Slider drag, list scrolling, color picker)
                                    if (!isResizing) {
                                        break
                                    }
                                } else {
                                    val dragY = change.position.y - change.previousPosition.y
                                    val dragX = change.position.x - change.previousPosition.x
                                    totalDragY += dragY
                                    totalDragX += dragX

                                    if (!isResizing) {
                                        if (abs(totalDragY) > touchSlop && abs(totalDragY) > abs(totalDragX) * 1.2f) {
                                            isResizing = true
                                            try {
                                                com.example.util.AppHaptics.perform(haptic)
                                            } catch (_: Exception) {}
                                        }
                                    }

                                    if (isResizing) {
                                        change.consume()
                                        val deltaFraction = -dragY / 500f
                                        val newFraction = (currentHeightFraction + deltaFraction).coerceIn(0.35f, 0.95f)
                                        currentHeightFraction = newFraction
                                    }
                                }
                            }
                            if (isResizing) {
                                PlayerPanelHeightStore.saveHeight(context, effectiveKey, currentHeightFraction)
                            }
                        }
                    }
                    .then(
                        if (appPanelGlass) Modifier else Modifier.shadow(
                            elevation = 24.dp,
                            shape = sheetShape,
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                            spotColor = AccentSkyBlueStrong.copy(alpha = 0.15f)
                        )
                    )
                    .clip(sheetShape)
                    .then(if (liquidGlass && !appPanelGlass) Modifier.videoGlassBackdrop(sheetShape) else Modifier)
                    .then(
                        if (appPanelGlass) Modifier.drawBehind { drawRect(appPanelFill) } else Modifier.background(dynamicGlassGradient)
                    )
                    .then(
                        if (appPanelGlass) {
                            if (liquidGlass) {
                                Modifier
                                    .background(liquidGlassSheenBrush(isDark))
                                    .liquidGlassBorder(sheetShape, isDark, 1.dp)
                            } else Modifier
                        } else if (liquidGlass) {
                            Modifier
                                .background(liquidGlassSheenBrush(isDark))
                                .border(1.2.dp, liquidGlassBorderBrush(isDark), sheetShape)
                        } else {
                            Modifier.border(1.5.dp, dynamicBorderColor, sheetShape)
                        }
                    )
            ) {
                CompositionLocalProvider(
                    LocalSheetNormalTextColor provides sheetNormalTextColor,
                    LocalSheetSecondaryTextColor provides sheetSecondaryTextColor,
                    LocalSheetIsLowGlassOpacity provides isLowGlassOpacity,
                    LocalSheetWhiteAlpha provides (if (isLowGlassOpacity) 1f else alphaRatio.coerceIn(0.40f, 1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                    ) {
                        // Header Row
                        if (headerCustomRow != null) {
                            headerCustomRow()
                        } else if (headerStartContent != null || headerEndContent != null || title != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left: Title or custom start content
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(end = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (headerStartContent != null) {
                                        headerStartContent()
                                    } else if (title != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (icon != null) {
                                                StyledIcon(
                                imageVector = icon,
                                                    contentDescription = null,
                                                    tint = ActiveHighlightColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = title,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NormalItemColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Right: Action buttons / trailing content
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .padding(start = 4.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (headerEndContent != null) {
                                        headerEndContent()
                                    } else if (!badgeText.isNullOrBlank()) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SecondaryTextColor
                                        )
                                    }
                                }
                            }
                        }

                        // Clean dynamic list content
                        content()
                    }
                }
            }
        }
    }
}
