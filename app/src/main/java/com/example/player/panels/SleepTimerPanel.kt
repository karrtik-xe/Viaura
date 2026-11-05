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

/**
 * 2B-2. SLEEP TIMER PANEL
 * Same glass sheet / layout as the Audio delay panel (opens on the right side in landscape).
 * Stepper (- / +) with hold-to-repeat, quick step chips (1m, 3m, 5m, 7m, 10m, 15m, 30m, 1H, 2H),
 * Start / Cancel button. While a timer runs the centre field shows the live countdown and the
 * +/- buttons add / remove time directly on the running timer.
 * Used by the video player (local + online) and the audio player (local + online).
 */
@Composable
fun SleepTimerPanel(
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onShowNotification: (String) -> Unit = {},
    /** Audio player: full-width bottom sheet in every orientation (landscape opens like portrait). */
    fullScreenPortrait: Boolean = false,
    /** Audio player: same side panel as the video player (portrait + landscape), hosted in its own dialog window. */
    audioPlayerSide: Boolean = false,
    onDismissRequest: () -> Unit
) {
    val actualLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val appGlass = fullScreenPortrait || audioPlayerSide
    val stepVariants = remember {
        listOf(
            60_000L to "1m",
            180_000L to "3m",
            300_000L to "5m",
            420_000L to "7m",
            600_000L to "10m",
            900_000L to "15m",
            1_800_000L to "30m",
            3_600_000L to "1H",
            7_200_000L to "2H"
        )
    }
    var selectedStepMs by remember { mutableStateOf(60_000L) }

    val timerActive by com.example.util.SleepTimerManager.isActive.collectAsState()
    val timerRemainingMs by com.example.util.SleepTimerManager.remainingMs.collectAsState()
    var pendingMs by remember { mutableStateOf(0L) }
    // Running value so a held +/- button never works from a stale value between recompositions.
    val livePending = remember { longArrayOf(0L) }
    livePending[0] = pendingMs

    val isLowGlassOpacity = (glassBlurTransparency < 25f && !appGlass) || isAppInDarkTheme()
    val maxMs = com.example.util.SleepTimerManager.MAX_DURATION_MS

    fun stepBy(deltaMs: Long) {
        if (com.example.util.SleepTimerManager.isActive.value) {
            com.example.util.SleepTimerManager.adjust(deltaMs)
        } else {
            val newMs = (livePending[0] + deltaMs).coerceIn(0L, maxMs)
            livePending[0] = newMs
            pendingMs = newMs
        }
    }

    PlayerGlassModalSheet(
        panelKey = if (fullScreenPortrait && actualLandscape) "sleep_timer_full_land" else "sleep_timer",
        isLandscape = if (fullScreenPortrait) false else isLandscape,
        openOnRightSide = !fullScreenPortrait,
        fullWidth = fullScreenPortrait,
        heightFraction = if (fullScreenPortrait && actualLandscape) 0.95f else 0.52f,
        glassBlurTransparency = glassBlurTransparency,
        appPanelGlass = appGlass,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Sleep timer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LocalSheetNormalTextColor.current
            )
        },
        headerEndContent = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .cardBounceClick(scaleDown = 0.90f) {
                        com.example.util.SleepTimerManager.cancel()
                        pendingMs = 0L
                        livePending[0] = 0L
                        onShowNotification("Sleep timer reset")
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Reset",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (timerActive || pendingMs != 0L) ActiveHighlightColor else LocalSheetSecondaryTextColor.current
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Stepper: - [ value ] +
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HoldRepeatStepButton(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isLowGlassOpacity) Color(0x28FFFFFF) else sheetWhite(Neutral.F1F5F9))
                        .border(1.dp, if (isLowGlassOpacity) Color(0x40FFFFFF) else Neutral.CBD5E1, CircleShape),
                    onStep = { stepBy(-selectedStepMs) }
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = "Decrease time",
                        tint = LocalSheetNormalTextColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isLowGlassOpacity) Color(0x18FFFFFF) else sheetWhite(Neutral.F8FAFC))
                        .border(
                            1.2.dp,
                            if (timerActive || pendingMs != 0L) ActiveHighlightColor else (if (isLowGlassOpacity) Color(0x40FFFFFF) else Neutral.CBD5E1),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (timerActive) com.example.util.SleepTimerManager.formatCountdown(timerRemainingMs)
                        else com.example.util.SleepTimerManager.formatDuration(pendingMs),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (timerActive || pendingMs != 0L) ActiveHighlightColor else LocalSheetNormalTextColor.current
                    )
                }

                HoldRepeatStepButton(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isLowGlassOpacity) Color(0x28FFFFFF) else sheetWhite(Neutral.F1F5F9))
                        .border(1.dp, if (isLowGlassOpacity) Color(0x40FFFFFF) else Neutral.CBD5E1, CircleShape),
                    onStep = { stepBy(selectedStepMs) }
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Increase time",
                        tint = LocalSheetNormalTextColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Step chips: how much one tap (or hold) on + / - changes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stepVariants.forEach { (stepMs, label) ->
                    GlassPresetChip(
                        label = label,
                        isSelected = selectedStepMs == stepMs,
                        onClick = { selectedStepMs = stepMs }
                    )
                }
            }

            Text(
                text = "When the timer ends, the video/audio stops, the player closes, the app steps back out of the folder and closes.",
                fontSize = 12.sp,
                color = LocalSheetSecondaryTextColor.current
            )

            // 3. Start / Cancel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(AccentGradient)
                    .cardBounceClick(scaleDown = 0.95f) {
                        if (timerActive) {
                            com.example.util.SleepTimerManager.cancel()
                            onShowNotification("Sleep timer cancelled")
                        } else if (pendingMs > 0L) {
                            com.example.util.SleepTimerManager.start(pendingMs)
                            onShowNotification("Sleep timer set: ${com.example.util.SleepTimerManager.formatDuration(pendingMs)}")
                            onDismissRequest()
                        } else {
                            onShowNotification("Choose a time first")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (timerActive) "Cancel timer" else "Start timer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
