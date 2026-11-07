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
import com.example.player.Anime4KShaders
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
import androidx.compose.runtime.derivedStateOf
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
 * 4. 3-DOT OVERFLOW MENU PANEL
 */
@Composable
fun MoreMenuSelectionPanel(
    playbackSpeed: Double,
    onOpenSpeedDialog: () -> Unit,
    onOpenVideoZoomDialog: () -> Unit = {},
    onOpenVideoEqDialog: () -> Unit,
    onOpenAbLoopDialog: () -> Unit,
    aspectRatioMode: AspectRatioMode,
    onAspectRatioChange: (AspectRatioMode) -> Unit,
    decoderMode: DecoderMode,
    onDecoderModeChange: (DecoderMode) -> Unit,
    isAudioOnly: Boolean,
    onAudioOnlyChange: (Boolean) -> Unit,
    repeatMode: PlayerRepeatMode,
    onRepeatModeChange: (PlayerRepeatMode) -> Unit,
    isShuffle: Boolean,
    onShuffleChange: (Boolean) -> Unit,
    gestureSensitivityMode: GestureSensitivityMode = GestureSensitivityMode.EXPONENTIAL,
    onGestureSensitivityModeChange: (GestureSensitivityMode) -> Unit = {},
    onTakeScreenshot: () -> Unit,
    onTogglePiP: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenPlayerLayoutDialog: (() -> Unit)? = null,
    onOpenVideoQualityDialog: (() -> Unit)? = null,
    selectedOnlineQuality: String? = null,
    onOpenAudioDelay: (() -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    val sleepTimerActive by com.example.util.SleepTimerManager.isActive.collectAsState()
    val sleepTimerRemainingMs by com.example.util.SleepTimerManager.remainingMs.collectAsState()
    PlayerGlassModalSheet(
        panelKey = "more_menu",
        isLandscape = isLandscape,
        heightFraction = 0.65f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Playback Menu",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // 0. Player Layout (Seekbar style & layout)
            if (onOpenPlayerLayoutDialog != null) {
                item {
                    MenuActionItem(
                        icon = Icons.Outlined.DashboardCustomize,
                        title = "Player Layout",
                        subtitle = "Seekbar style & custom layout",
                        badgeValue = null,
                        onClick = {
                            onOpenPlayerLayoutDialog()
                        }
                    )
                }
            }

            // Stream Video Quality
            if (onOpenVideoQualityDialog != null) {
                item {
                    MenuActionItem(
                        icon = Icons.Outlined.HighQuality,
                        title = "Video Quality",
                        subtitle = "Stream resolution: ${selectedOnlineQuality ?: "Auto"}",
                        badgeValue = (selectedOnlineQuality ?: "Auto").substringBefore(" ("),
                        onClick = {
                            onOpenVideoQualityDialog()
                        }
                    )
                }
            }

            // Audio Delay & Sync
            if (onOpenAudioDelay != null) {
                item {
                    MenuActionItem(
                        icon = Icons.Outlined.Timelapse,
                        title = "Audio delay",
                        subtitle = "Audio synchronization & delay offset",
                        badgeValue = null,
                        onClick = {
                            onOpenAudioDelay()
                        }
                    )
                }
            }

            // Equalizer
            if (onOpenEqualizer != null) {
                item {
                    MenuActionItem(
                        iconRes = com.example.R.drawable.viaura_equalizer,
                        title = "Equalizer",
                        subtitle = "5-band audio equalizer & volume boost",
                        badgeValue = null,
                        onClick = {
                            onOpenEqualizer()
                        }
                    )
                }
            }

            // 1. Playback Speed
            item {
                MenuActionItem(
                    icon = Icons.Outlined.Speed,
                    title = "Playback Speed",
                    subtitle = "Current: ${playbackSpeed}x",
                    badgeValue = "${playbackSpeed}x",
                    onClick = {
                        onOpenSpeedDialog()
                    }
                )
            }

            // 2. Video Zoom
            item {
                MenuActionItem(
                    icon = Icons.Outlined.ZoomIn,
                    title = "Video Zoom",
                    subtitle = "Scale and pan video frame",
                    badgeValue = null,
                    onClick = {
                        onOpenVideoZoomDialog()
                    }
                )
            }

            // 3. Video Adjustments / Video Setting
            item {
                MenuActionItem(
                    iconRes = com.example.R.drawable.viaura_video_setting,
                    title = "Video Setting",
                    subtitle = "Filters, Color Presets & Video Adjustments",
                    badgeValue = null,
                    onClick = {
                        onOpenVideoEqDialog()
                    }
                )
            }

            // 3. A-B Repeat Loop
            item {
                MenuActionItem(
                    icon = Icons.Outlined.Repeat,
                    title = "A-B Repeat Loop",
                    subtitle = "Repeat selected section",
                    badgeValue = null,
                    onClick = {
                        onOpenAbLoopDialog()
                    }
                )
            }

            // 4. Aspect Ratio Cycle
            item {
                val nextRatio = aspectRatioMode.next()
                MenuActionItem(
                    iconRes = nextRatio.iconRes,
                    title = "Display Mode",
                    subtitle = "Mode: ${aspectRatioMode.label}",
                    badgeValue = aspectRatioMode.label,
                    onClick = {
                        onAspectRatioChange(nextRatio)
                    }
                )
            }

            // 5. Decoder Mode Cycle
            item {
                val nextDec = when (decoderMode) {
                    DecoderMode.AUTO -> DecoderMode.HW_PLUS
                    DecoderMode.HW_PLUS -> DecoderMode.HW
                    DecoderMode.HW -> DecoderMode.SW
                    DecoderMode.SW -> DecoderMode.AUTO
                }
                MenuActionItem(
                    icon = Icons.Outlined.Memory,
                    title = "Hardware Decoder",
                    subtitle = if (decoderMode == DecoderMode.AUTO) "Smart Auto Playback: best decoder per video" else "Engine: ${decoderMode.label}",
                    badgeValue = decoderMode.label,
                    onClick = {
                        onDecoderModeChange(nextDec)
                    }
                )
            }

            // 6. Audio-Only Mode Toggle
            item {
                MenuActionItem(
                    icon = Icons.Outlined.Headphones,
                    title = "Audio-Only Mode",
                    subtitle = if (isAudioOnly) "Active (Screen savings)" else "Play audio with screen off",
                    badgeValue = if (isAudioOnly) "ON" else "OFF",
                    isActive = isAudioOnly,
                    onClick = {
                        onAudioOnlyChange(!isAudioOnly)
                    }
                )
            }

            // 7. Repeat Mode Cycle
            item {
                val (nextRepeat, repeatLabel) = when (repeatMode) {
                    PlayerRepeatMode.OFF -> PlayerRepeatMode.ALL to "All"
                    PlayerRepeatMode.ALL -> PlayerRepeatMode.ONE to "One"
                    PlayerRepeatMode.ONE -> PlayerRepeatMode.OFF to "Off"
                }
                val repeatIcon = if (repeatMode == PlayerRepeatMode.ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat
                MenuActionItem(
                    icon = repeatIcon,
                    title = "Repeat Mode",
                    subtitle = "Repeat: $repeatLabel",
                    badgeValue = repeatLabel,
                    isActive = repeatMode != PlayerRepeatMode.OFF,
                    onClick = {
                        onRepeatModeChange(nextRepeat)
                    }
                )
            }

            // 8. Shuffle Toggle
            item {
                MenuActionItem(
                    icon = Icons.Outlined.Shuffle,
                    title = "Shuffle Playback",
                    subtitle = if (isShuffle) "Random order enabled" else "Sequential order",
                    badgeValue = if (isShuffle) "ON" else "OFF",
                    isActive = isShuffle,
                    onClick = {
                        onShuffleChange(!isShuffle)
                    }
                )
            }

            // 8.5 Gesture Scaling (Exponential vs Linear)
            item {
                val nextMode = if (gestureSensitivityMode == GestureSensitivityMode.EXPONENTIAL) GestureSensitivityMode.LINEAR else GestureSensitivityMode.EXPONENTIAL
                val gestureIcon = if (gestureSensitivityMode == GestureSensitivityMode.EXPONENTIAL) Icons.Outlined.Speed else Icons.Outlined.Tune
                MenuActionItem(
                    icon = gestureIcon,
                    title = "Gesture Scaling",
                    subtitle = if (gestureSensitivityMode == GestureSensitivityMode.EXPONENTIAL) "Exponential (Perceptual curve)" else "Linear (1:1 Uniform steps)",
                    badgeValue = gestureSensitivityMode.shortLabel,
                    isActive = gestureSensitivityMode == GestureSensitivityMode.EXPONENTIAL,
                    onClick = {
                        onGestureSensitivityModeChange(nextMode)
                    }
                )
            }

            // 9. Take Screenshot
            item {
                MenuActionItem(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "Take Screenshot",
                    subtitle = "Save current video frame to gallery",
                    badgeValue = null,
                    onClick = {
                        onDismissRequest()
                        onTakeScreenshot()
                    }
                )
            }

            // 10. Picture-in-Picture
            item {
                MenuActionItem(
                    icon = Icons.Outlined.PictureInPictureAlt,
                    title = "Picture-in-Picture",
                    subtitle = "Continue playing in floating mini window",
                    badgeValue = null,
                    onClick = {
                        onDismissRequest()
                        onTogglePiP()
                    }
                )
            }

            // 11. Rotate Screen
            item {
                MenuActionItem(
                    icon = Icons.Outlined.ScreenRotation,
                    title = "Rotate Screen",
                    subtitle = if (isLandscape) "Switch to Portrait" else "Switch to Landscape",
                    badgeValue = null,
                    onClick = {
                        onDismissRequest()
                        onToggleOrientation()
                    }
                )
            }

            // 12. Sleep Timer (always the last item)
            if (onOpenSleepTimer != null) {
                item {
                    MenuActionItem(
                        iconRes = com.example.R.drawable.viaura_sleep,
                        title = "Sleep timer",
                        subtitle = if (sleepTimerActive) "Closes in ${com.example.util.SleepTimerManager.formatCountdown(sleepTimerRemainingMs)}" else "Stop playback & close the app after a set time",
                        badgeValue = if (sleepTimerActive) com.example.util.SleepTimerManager.formatCountdown(sleepTimerRemainingMs) else null,
                        isActive = sleepTimerActive,
                        onClick = {
                            onOpenSleepTimer()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 5. SPEED SELECTION PANEL (Liquid Glass System UI)
 */
@Composable
fun SpeedSelectionPanel(
    currentSpeed: Double,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    isPitchCorrectionEnabled: Boolean = true,
    onTogglePitchCorrection: (Boolean) -> Unit = {},
    onSpeedChange: (Double) -> Unit,
    onDismissRequest: () -> Unit
) {
    val speeds = listOf(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0)
    val context = LocalContext.current
    val formattedSpeed = String.format(Locale.US, "%.2f", currentSpeed).trimEnd('0').trimEnd('.')

    PlayerGlassModalSheet(
        panelKey = "speed",
        isLandscape = isLandscape,
        glassBlurTransparency = glassBlurTransparency,
        heightFraction = 0.62f,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Speed • ${formattedSpeed}x",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        },
        headerEndContent = {
            Text(
                text = "Reset",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ActiveHighlightColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSpeedChange(1.0) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Large Centered Current Speed
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${formattedSpeed}x",
                    color = ActiveHighlightColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // 2. Stepper & Progress Slider Row: (-) [======|=======] (+)
            GlassStepperSliderRow(
                value = currentSpeed.toFloat(),
                onValueChange = { spd -> onSpeedChange(spd.toDouble()) },
                valueRange = 0.25f..4.0f,
                step = 0.05f
            )

            // 3. Preset Speed Chips (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                speeds.forEach { spd ->
                    val isSelected = abs(currentSpeed - spd) < 0.01
                    val label = when (spd) {
                        1.0 -> "1.0x"
                        2.0 -> "2.0x"
                        3.0 -> "3.0x"
                        4.0 -> "4.0x"
                        else -> "${spd}x"
                    }
                    GlassPresetChip(
                        label = label,
                        isSelected = isSelected,
                        onClick = {
                            onSpeedChange(spd)
                        }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                color = Color.White.copy(alpha = 0.12f),
                thickness = 1.dp
            )

            // 4. Pitch Correction Option Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onTogglePitchCorrection(!isPitchCorrectionEnabled)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Enable audio pitch correction",
                        color = NormalItemColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Prevents the audio from becoming high-pitched at faster speeds and low-pitched at slower speeds",
                        color = SecondaryTextColor,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
                ViauraSwitch(
                    checked = isPitchCorrectionEnabled,
                    onCheckedChange = onTogglePitchCorrection,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ActiveHighlightColor,
                        uncheckedThumbColor = Neutral.S94A3B8,
                        uncheckedTrackColor = Neutral.E2E8F0
                    )
                )
            }

            // 5. Action Buttons (Make default speed & Reset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Make Default Speed Button
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentGradient)
                        .cardBounceClick(scaleDown = 0.95f) {
                            Toast.makeText(context, "Default speed set to ${formattedSpeed}x", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Make default speed",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SheetButtonBgColor)
                        .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(14.dp))
                        .cardBounceClick(scaleDown = 0.95f) {
                            onSpeedChange(1.0)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset",
                        color = SheetButtonTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 5.5 VIDEO ZOOM PANEL (Interactive scaling & panning controls)
 */
@Composable
fun VideoZoomSelectionPanel(
    zoomScale: Float,
    onZoomChange: (Float) -> Unit,
    isPanAndZoomEnabled: Boolean,
    onTogglePanAndZoom: (Boolean) -> Unit,
    onResetZoomAndPan: () -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    val zoomPresets = listOf(1.0f, 1.25f, 1.5f, 2.0f, 2.5f, 3.0f, 4.0f)
    val context = LocalContext.current
    val displayZoom = String.format(Locale.US, "%.2fx", zoomScale)

    PlayerGlassModalSheet(
        panelKey = "video_zoom",
        isLandscape = isLandscape,
        glassBlurTransparency = glassBlurTransparency,
        heightFraction = 0.62f,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Video Zoom",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        },
        headerEndContent = {
            Text(
                text = displayZoom,
                color = ActiveHighlightColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Stepper & Real-time Live Zoom Slider: (-) [======|=======] (+)
            GlassStepperSliderRow(
                value = zoomScale,
                onValueChange = onZoomChange,
                valueRange = 1.0f..4.0f,
                step = 0.05f
            )

            // 2. Preset Zoom Chips (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                zoomPresets.forEach { preset ->
                    val isSelected = abs(zoomScale - preset) < 0.02f
                    val label = when (preset) {
                        1.0f -> "1.0x"
                        2.0f -> "2.0f"
                        3.0f -> "3.0f"
                        4.0f -> "4.0f"
                        else -> "${String.format(Locale.US, "%.2f", preset).trimEnd('0').trimEnd('.')}x"
                    }
                    GlassPresetChip(
                        label = label,
                        isSelected = isSelected,
                        onClick = {
                            onZoomChange(preset)
                        }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                color = Neutral.E2E8F0,
                thickness = 1.dp
            )

            // 3. Pan & Zoom Option Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onTogglePanAndZoom(!isPanAndZoomEnabled)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Pan & Zoom",
                        color = NormalItemColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Drag with touch to pan across zoomed video frames",
                        color = SecondaryTextColor,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                }
                ViauraSwitch(
                    checked = isPanAndZoomEnabled,
                    onCheckedChange = onTogglePanAndZoom,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ActiveHighlightColor,
                        uncheckedThumbColor = Neutral.S94A3B8,
                        uncheckedTrackColor = Neutral.E2E8F0
                    )
                )
            }

            // 4. Action Buttons (Set as default & Reset)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Set as Default
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentGradient)
                        .cardBounceClick(scaleDown = 0.95f) {
                            Toast.makeText(context, "Default zoom set to ${String.format(Locale.US, "%.2fx", zoomScale)}", Toast.LENGTH_SHORT).show()
                            onDismissRequest()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Set as default",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SheetButtonBgColor)
                        .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(14.dp))
                        .cardBounceClick(scaleDown = 0.95f) {
                            onResetZoomAndPan()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset",
                        color = SheetButtonTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 6. VIDEO SETTING (FILTER PRESETS & MANUAL VIDEO ADJUSTMENT) PANEL
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoSettingSelectionPanel(
    selectedPreset: VideoFilterPreset,
    onSelectPreset: (VideoFilterPreset) -> Unit,
    manualAdjustments: ManualVideoAdjustments,
    onManualAdjustmentsChange: (ManualVideoAdjustments) -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onOpenManualAdjustments: () -> Unit,
    onDismissRequest: () -> Unit
) {
    PlayerGlassModalSheet(
        panelKey = "video_setting_filter",
        isLandscape = isLandscape,
        heightFraction = 0.56f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Video Setting",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NormalItemColor
            )
        },
        headerEndContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Reset to None & 0 adjustments
                if (selectedPreset != VideoFilterPreset.NONE || !manualAdjustments.isNeutral()) {
                    Text(
                        text = "Reset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActiveHighlightColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .cardBounceClick(scaleDown = 0.90f) {
                                onSelectPreset(VideoFilterPreset.NONE)
                                onManualAdjustmentsChange(ManualVideoAdjustments())
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Palette Icon Button -> Opens Manual Video Adjustments (closes Filter UI)
                IconButton(
                    onClick = {
                        onOpenManualAdjustments()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Manual Video Adjustments",
                        tint = if (!manualAdjustments.isNeutral()) ActiveHighlightColor else NormalItemColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            val presets = remember { VideoFilterPreset.entries }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = preset == selectedPreset
                    val isLowGlass = LocalSheetIsLowGlassOpacity.current
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) {
                                    AccentGradient
                                } else if (isLowGlass) {
                                    SolidColor(Color(0x22FFFFFF))
                                } else {
                                    SolidColor(sheetWhite(Neutral.F1F5F9))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) {
                                    Color(0x40FFFFFF)
                                } else if (isLowGlass) {
                                    Color(0x30FFFFFF)
                                } else {
                                    Neutral.CBD5E1
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .cardBounceClick(scaleDown = 0.93f) {
                                onSelectPreset(preset)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else (if (isLowGlass) NormalItemColor else SheetChipTextColor),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6B. MANUAL VIDEO ADJUSTMENT RIGHT-SIDE SLIDING PANEL
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualVideoAdjustmentPanel(
    manualAdjustments: ManualVideoAdjustments,
    onManualAdjustmentsChange: (ManualVideoAdjustments) -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    PlayerGlassModalSheet(
        panelKey = "manual_video_adjustment",
        isLandscape = isLandscape,
        openOnRightSide = true,
        heightFraction = 0.72f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Manual Video Adjustment",
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = NormalItemColor
            )
        },
        headerEndContent = {
            Text(
                text = "Reset",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = ActiveHighlightColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .cardBounceClick(scaleDown = 0.90f) {
                        onManualAdjustmentsChange(ManualVideoAdjustments())
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Brightness (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Brightness",
                value = manualAdjustments.brightness,
                valueRange = -100f..100f,
                valueFormat = { if (it > 0) "+$it" else "$it" },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(brightness = it.roundToInt())) }
            )

            // 2. Contrast (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Contrast",
                value = manualAdjustments.contrast,
                valueRange = -100f..100f,
                valueFormat = { if (it > 0) "+$it" else "$it" },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(contrast = it.roundToInt())) }
            )

            // 3. Saturation (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Saturation",
                value = manualAdjustments.saturation,
                valueRange = -100f..100f,
                valueFormat = { if (it > 0) "+$it" else "$it" },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(saturation = it.roundToInt())) }
            )

            // 4. Gamma (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Gamma",
                value = manualAdjustments.gamma,
                valueRange = -100f..100f,
                valueFormat = { if (it > 0) "+$it" else "$it" },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(gamma = it.roundToInt())) }
            )

            // 5. Sharpness (0 to 100)
            ManualAdjustmentSliderItem(
                title = "Sharpness",
                value = manualAdjustments.sharpness,
                valueRange = 0f..100f,
                valueFormat = { "$it" },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(sharpness = it.roundToInt())) }
            )

            // 6. Hue (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Hue",
                value = manualAdjustments.hue,
                valueRange = -100f..100f,
                valueFormat = { if (it > 0) "+$it°" else "$it°" },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(hue = it.roundToInt())) }
            )

            // 7. Temperature (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Temperature",
                value = manualAdjustments.temperature,
                valueRange = -100f..100f,
                valueFormat = {
                    if (it > 0) "+$it (Warm)" else if (it < 0) "$it (Cool)" else "0"
                },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(temperature = it.roundToInt())) }
            )

            // 8. Tint (-100 to 100)
            ManualAdjustmentSliderItem(
                title = "Tint",
                value = manualAdjustments.tint,
                valueRange = -100f..100f,
                valueFormat = {
                    if (it > 0) "+$it (Magenta)" else if (it < 0) "$it (Green)" else "0"
                },
                onValueChange = { onManualAdjustmentsChange(manualAdjustments.copy(tint = it.roundToInt())) }
            )

            // 9. Deband (Banding Reduction)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SheetButtonBgColor)
                    .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        onManualAdjustmentsChange(manualAdjustments.copy(deband = !manualAdjustments.deband))
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deband Filter",
                        color = SheetButtonTextColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Reduces color banding artifacts",
                        color = Neutral.S475569,
                        fontSize = 12.sp
                    )
                }
                ViauraSwitch(
                    checked = manualAdjustments.deband,
                    onCheckedChange = {
                        onManualAdjustmentsChange(manualAdjustments.copy(deband = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ActiveHighlightColor
                    )
                )
            }

            // 10. Anime4K shaders (real-time anime upscaling / restoring, runs on the GPU)
            val anime4kContext = LocalContext.current
            val anime4kOn = Anime4KShaders.isOn(manualAdjustments.anime4k)
            val anime4kFilesReady = remember(anime4kContext) { Anime4KShaders.isAvailable(anime4kContext, Anime4KShaders.modeId("a", false)) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SheetButtonBgColor)
                    .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = anime4kFilesReady) {
                            onManualAdjustmentsChange(
                                manualAdjustments.copy(anime4k = if (anime4kOn) Anime4KShaders.OFF else Anime4KShaders.modeId("a", false))
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Anime4K Shaders",
                            color = SheetButtonTextColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (anime4kFilesReady) "Sharper lines for anime (uses more GPU / battery)" else "Shader files are not included in this build",
                            color = Neutral.S475569,
                            fontSize = 12.sp
                        )
                    }
                    ViauraSwitch(
                        checked = anime4kOn,
                        onCheckedChange = {
                            if (anime4kFilesReady) {
                                onManualAdjustmentsChange(
                                    manualAdjustments.copy(anime4k = if (it) Anime4KShaders.modeId("a", false) else Anime4KShaders.OFF)
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ActiveHighlightColor
                        )
                    )
                }
                if (anime4kOn) {
                    val currentLetter = Anime4KShaders.letterOf(manualAdjustments.anime4k)
                    val currentHq = Anime4KShaders.isHq(manualAdjustments.anime4k)
                    @Composable
                    fun Anime4KChip(label: String, selected: Boolean, onClick: () -> Unit) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) ActiveHighlightColor else Color.Transparent)
                                .border(1.dp, if (selected) ActiveHighlightColor else SheetButtonBorderColor, RoundedCornerShape(10.dp))
                                .clickable(onClick = onClick)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else SheetButtonTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Anime4KChip("Mode A", currentLetter == "a") {
                            onManualAdjustmentsChange(manualAdjustments.copy(anime4k = Anime4KShaders.modeId("a", currentHq)))
                        }
                        Anime4KChip("Mode B", currentLetter == "b") {
                            onManualAdjustmentsChange(manualAdjustments.copy(anime4k = Anime4KShaders.modeId("b", currentHq)))
                        }
                        Anime4KChip("Mode C", currentLetter == "c") {
                            onManualAdjustmentsChange(manualAdjustments.copy(anime4k = Anime4KShaders.modeId("c", currentHq)))
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Anime4KChip("Fast", !currentHq) {
                            onManualAdjustmentsChange(manualAdjustments.copy(anime4k = Anime4KShaders.modeId(currentLetter, false)))
                        }
                        Anime4KChip("High quality", currentHq) {
                            onManualAdjustmentsChange(manualAdjustments.copy(anime4k = Anime4KShaders.modeId(currentLetter, true)))
                        }
                    }
                    Text(
                        text = "A: clean 1080p anime · B: soft / slightly blurry · C: noisy or low-res (480p–720p). High quality needs a strong phone.",
                        color = Neutral.S475569,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualAdjustmentSliderItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormat: (Int) -> String,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = NormalItemColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGradient)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueFormat(value),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        ViauraSlider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentPink,
                activeTrackColor = AccentSkyBlue,
                inactiveTrackColor = if (LocalSheetIsLowGlassOpacity.current) Color(0x33FFFFFF) else AccentSkyBlue.copy(alpha = 0.20f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Backward compatibility wrapper for VideoEqSelectionPanel
 */
@Composable
fun VideoEqSelectionPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    contrast: Float,
    onContrastChange: (Float) -> Unit,
    saturation: Float,
    onSaturationChange: (Float) -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    VideoSettingSelectionPanel(
        selectedPreset = VideoFilterPreset.NONE,
        onSelectPreset = {},
        manualAdjustments = ManualVideoAdjustments(
            brightness = ((brightness - 0.5f) * 200f).roundToInt(),
            contrast = ((contrast - 1.0f) * 100f).roundToInt(),
            saturation = ((saturation - 1.0f) * 100f).roundToInt()
        ),
        onManualAdjustmentsChange = { manual ->
            onBrightnessChange((manual.brightness / 200f) + 0.5f)
            onContrastChange((manual.contrast / 100f) + 1.0f)
            onSaturationChange((manual.saturation / 100f) + 1.0f)
        },
        isLandscape = isLandscape,
        glassBlurTransparency = glassBlurTransparency,
        onOpenManualAdjustments = {},
        onDismissRequest = onDismissRequest
    )
}

/**
 * 6B. CHUNKY VERTICAL EQUALIZER SLIDER
 * Chunky pill-track vertical slider matching the high-contrast aesthetic with 0 dB center reference crossbar and thumb dot.
 */
@Composable
fun VerticalEqChunkySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = -12f..12f,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 140.dp,
    trackWidth: androidx.compose.ui.unit.Dp = 14.dp,
    centerNotchWidth: androidx.compose.ui.unit.Dp = 32.dp,
    centerNotchHeight: androidx.compose.ui.unit.Dp = 3.5.dp,
    isLowGlassOpacity: Boolean = false
) {
    val trackWidthPx = with(LocalDensity.current) { trackWidth.toPx() }
    val centerNotchWidthPx = with(LocalDensity.current) { centerNotchWidth.toPx() }
    val centerNotchHeightPx = with(LocalDensity.current) { centerNotchHeight.toPx() }
    val thumbDotRadiusPx = with(LocalDensity.current) { 3.2.dp.toPx() }

    val baseTrackColor = if (isLowGlassOpacity) Neutral.D334155 else Neutral.CBD5E1
    val activeTrackColor = if (isLowGlassOpacity) Neutral.S64748B else Neutral.S94A3B8
    val centerNotchColor = if (isLowGlassOpacity) Neutral.S94A3B8 else Neutral.S64748B
    val thumbDotColor = if (isLowGlassOpacity) Neutral.F1F5F9 else Neutral.S475569

    val gestureModifier = if (enabled) {
        Modifier
            .pointerInput(valueRange) {
                fun updateFromY(y: Float, totalHeight: Float) {
                    val halfTrack = trackWidthPx / 2f
                    val usableHeight = (totalHeight - 2 * halfTrack).coerceAtLeast(1f)
                    val clampedY = y.coerceIn(halfTrack.coerceAtMost(totalHeight / 2f), (totalHeight - halfTrack).coerceAtLeast(totalHeight / 2f))
                    val fraction = 1f - ((clampedY - halfTrack) / usableHeight)
                    val rangeSpan = valueRange.endInclusive - valueRange.start
                    val rawVal = valueRange.start + fraction * rangeSpan
                    val snappedVal = ((rawVal * 2).roundToInt() / 2f).coerceIn(valueRange)
                    onValueChange(snappedVal)
                }

                // Tap-to-set and drag-to-adjust are combined in ONE pointerInput
                // block (both launched as sibling coroutines sharing the same
                // pointer event stream) instead of two separate stacked
                // pointerInput modifiers. Two independent modifiers here used to
                // race for the very first touch down, which is why a single tap
                // or the start of a drag on this slider sometimes needed to be
                // repeated before it registered.
                kotlinx.coroutines.coroutineScope {
                    launch {
                        detectTapGestures { offset ->
                            updateFromY(offset.y, size.height.toFloat())
                        }
                    }
                    launch {
                        detectVerticalDragGestures { change, _ ->
                            change.consume()
                            updateFromY(change.position.y, size.height.toFloat())
                        }
                    }
                }
            }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .width(44.dp)
            .height(height)
            .alpha(if (enabled) 1f else 0.4f)
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        val totalHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val halfTrackPx = trackWidthPx / 2f
        val usableHeightPx = (totalHeightPx - 2 * halfTrackPx).coerceAtLeast(1f)
        val rangeSpan = valueRange.endInclusive - valueRange.start
        val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
        val thumbY = totalHeightPx - halfTrackPx - (fraction * usableHeightPx)
        val zeroFraction = ((0f - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
        val zeroY = totalHeightPx - halfTrackPx - (zeroFraction * usableHeightPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f

            // 1. Thick base capsule track
            drawRoundRect(
                color = baseTrackColor,
                topLeft = Offset(centerX - halfTrackPx, 0f),
                size = androidx.compose.ui.geometry.Size(trackWidthPx, totalHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfTrackPx, halfTrackPx)
            )

            // 2. Active highlight between 0 dB and current thumb
            if (kotlin.math.abs(thumbY - zeroY) > 1.5f) {
                val topActiveY = kotlin.math.min(thumbY, zeroY)
                val activeHeight = kotlin.math.abs(thumbY - zeroY)
                drawRoundRect(
                    color = activeTrackColor,
                    topLeft = Offset(centerX - halfTrackPx, topActiveY),
                    size = androidx.compose.ui.geometry.Size(trackWidthPx, activeHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfTrackPx, halfTrackPx)
                )
            }

            // 3. Center 0 dB prominent crossbar line
            drawRoundRect(
                color = centerNotchColor,
                topLeft = Offset(centerX - centerNotchWidthPx / 2f, zeroY - centerNotchHeightPx / 2f),
                size = androidx.compose.ui.geometry.Size(centerNotchWidthPx, centerNotchHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(centerNotchHeightPx / 2f, centerNotchHeightPx / 2f)
            )

            // 4. Thumb small dot indicator
            drawCircle(
                color = thumbDotColor,
                radius = thumbDotRadiusPx,
                center = Offset(centerX, thumbY.coerceIn(halfTrackPx.coerceAtMost(totalHeightPx / 2f), (totalHeightPx - halfTrackPx).coerceAtLeast(totalHeightPx / 2f)))
            )
        }
    }
}

/**
 * 6C. CHUNKY HORIZONTAL VOLUME BOOST SLIDER
 * Chunky pill slider with start indicator notch bar and thumb dot indicator.
 */
@Composable
fun HorizontalBoostChunkySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..10f,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    isLowGlassOpacity: Boolean = false
) {
    val trackHeightPx = with(LocalDensity.current) { 14.dp.toPx() }
    val startNotchWidthPx = with(LocalDensity.current) { 3.5.dp.toPx() }
    val startNotchHeightPx = with(LocalDensity.current) { 24.dp.toPx() }
    val thumbDotRadiusPx = with(LocalDensity.current) { 3.2.dp.toPx() }
    val spacingPx = with(LocalDensity.current) { 6.dp.toPx() }

    val baseTrackColor = if (isLowGlassOpacity) Neutral.D334155 else Neutral.CBD5E1
    val activeTrackColor = if (isLowGlassOpacity) Neutral.S64748B else Neutral.S94A3B8
    val startNotchColor = if (isLowGlassOpacity) Neutral.S94A3B8 else Neutral.S64748B
    val thumbDotColor = if (isLowGlassOpacity) Neutral.F1F5F9 else Neutral.S475569

    val gestureModifier = if (enabled) {
        Modifier
            .pointerInput(valueRange) {
                fun updateFromX(x: Float, totalWidth: Float) {
                    val trackStartX = startNotchWidthPx + spacingPx
                    val trackEndX = totalWidth
                    val usableWidth = (trackEndX - trackStartX).coerceAtLeast(1f)
                    val clampedX = x.coerceIn(trackStartX, trackEndX.coerceAtLeast(trackStartX))
                    val fraction = (clampedX - trackStartX) / usableWidth
                    val rangeSpan = valueRange.endInclusive - valueRange.start
                    val rawVal = valueRange.start + fraction * rangeSpan
                    val snappedVal = rawVal.roundToInt().toFloat().coerceIn(valueRange)
                    onValueChange(snappedVal)
                }

                // Combined tap + drag detection in one pointerInput block (see
                // VerticalEqChunkySlider above for why this matters for touch
                // reliability).
                kotlinx.coroutines.coroutineScope {
                    launch {
                        detectTapGestures { offset ->
                            updateFromX(offset.x, size.width.toFloat())
                        }
                    }
                    launch {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            updateFromX(change.position.x, size.width.toFloat())
                        }
                    }
                }
            }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .then(gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        val totalWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val centerY = with(LocalDensity.current) { maxHeight.toPx() / 2f }
        val trackStartX = startNotchWidthPx + spacingPx
        val trackEndX = totalWidthPx
        val usableWidthPx = (trackEndX - trackStartX).coerceAtLeast(1f)
        val rangeSpan = valueRange.endInclusive - valueRange.start
        val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
        val thumbX = trackStartX + (fraction * usableWidthPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Start vertical reference notch bar
            drawRoundRect(
                color = startNotchColor,
                topLeft = Offset(0f, centerY - startNotchHeightPx / 2f),
                size = androidx.compose.ui.geometry.Size(startNotchWidthPx, startNotchHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(startNotchWidthPx / 2f, startNotchWidthPx / 2f)
            )

            // 2. Base thick horizontal capsule track
            drawRoundRect(
                color = baseTrackColor,
                topLeft = Offset(trackStartX, centerY - trackHeightPx / 2f),
                size = androidx.compose.ui.geometry.Size(usableWidthPx, trackHeightPx),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
            )

            // 3. Active highlight
            if (fraction > 0.01f) {
                val activeWidth = (thumbX - trackStartX).coerceAtLeast(trackHeightPx)
                drawRoundRect(
                    color = activeTrackColor,
                    topLeft = Offset(trackStartX, centerY - trackHeightPx / 2f),
                    size = androidx.compose.ui.geometry.Size(activeWidth, trackHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                )
            }

            // 4. Thumb dot indicator
            drawCircle(
                color = thumbDotColor,
                radius = thumbDotRadiusPx,
                center = Offset(thumbX.coerceIn((trackStartX + trackHeightPx / 2f).coerceAtMost((trackStartX + trackEndX) / 2f), (trackEndX - trackHeightPx / 2f).coerceAtLeast((trackStartX + trackEndX) / 2f)), centerY)
            )
        }
    }
}

@Composable
private fun ChunkyEqBandColumn(
    freqLabel: String,
    gain: Float,
    onGainChange: (Float) -> Unit,
    enabled: Boolean = true,
    isLowGlassOpacity: Boolean = false,
    sliderHeight: androidx.compose.ui.unit.Dp = 140.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.alpha(if (enabled) 1f else 0.45f)
    ) {
        // Gain value number on top (e.g. 0, +4, -2)
        val gainText = if (gain > 0f) "+${if (gain % 1f == 0f) gain.toInt().toString() else "%.1f".format(java.util.Locale.US, gain)}"
        else if (gain < 0f) "${if (gain % 1f == 0f) gain.toInt().toString() else "%.1f".format(java.util.Locale.US, gain)}"
        else "0"

        Text(
            text = gainText,
            color = if (isLowGlassOpacity) Neutral.F8FAFC else Neutral.D1E293B,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        // Chunky Vertical Slider
        VerticalEqChunkySlider(
            value = gain,
            onValueChange = onGainChange,
            valueRange = -12f..12f,
            enabled = enabled,
            height = sliderHeight,
            isLowGlassOpacity = isLowGlassOpacity
        )

        // Frequency label at bottom
        Text(
            text = freqLabel,
            color = if (isLowGlassOpacity) Neutral.S94A3B8 else Neutral.S64748B,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 6D. AUDIO EQUALIZER PANEL
 * Real-time 5-band audio equalizer (-12 dB to +12 dB) and Volume Boost (0 to +10 dB).
 * Anchors on the right side in landscape and bottom in portrait (matching Audio Delay / Subtitle Delay panel style).
 */
@Composable
fun AudioEqualizerPanel(
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    equalizerEnabled: Boolean,
    onToggleEqualizer: (Boolean) -> Unit,
    eq60Hz: Float,
    onEq60HzChange: (Float) -> Unit,
    eq230Hz: Float,
    onEq230HzChange: (Float) -> Unit,
    eq910Hz: Float,
    onEq910HzChange: (Float) -> Unit,
    eq3600Hz: Float,
    onEq3600HzChange: (Float) -> Unit,
    eq14000Hz: Float,
    onEq14000HzChange: (Float) -> Unit,
    volumeBoostDb: Float,
    onVolumeBoostDbChange: (Float) -> Unit,
    currentPreset: String,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onReset: () -> Unit,
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
    // Dark sheet (dark app theme OR very transparent glass): the sheet text is light, so fields,
    // +/- buttons and separators must use the dark-glass surfaces, not the light ones. This is
    // read from the app theme directly because this code runs OUTSIDE the sheet's own
    // CompositionLocalProvider, where LocalSheetNormalTextColor is still its light-mode default.
    val isLowGlassOpacity = (glassBlurTransparency < 25f && !appGlass) || isAppInDarkTheme()

    PlayerGlassModalSheet(
        panelKey = if (fullScreenPortrait && actualLandscape) "audio_equalizer_full_land" else "audio_equalizer",
        isLandscape = if (fullScreenPortrait) false else isLandscape,
        openOnRightSide = !fullScreenPortrait,
        fullWidth = fullScreenPortrait,
        heightFraction = if (fullScreenPortrait && actualLandscape) 0.95f else if (isLandscape) 0.92f else 0.72f,
        glassBlurTransparency = glassBlurTransparency,
        appPanelGlass = appGlass,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "EQUALIZER",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = if (isLowGlassOpacity) Neutral.F8FAFC else Neutral.D1E293B
            )
        },
        headerEndContent = {
            ViauraSwitch(
                checked = equalizerEnabled,
                onCheckedChange = { isChecked ->
                    onToggleEqualizer(isChecked)
                    onShowNotification(if (isChecked) "Equalizer Enabled" else "Equalizer Disabled")
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = if (isLowGlassOpacity) Color(0xFF818CF8) else Neutral.S475569,
                    uncheckedThumbColor = Neutral.S94A3B8,
                    uncheckedTrackColor = if (isLowGlassOpacity) Neutral.D1E293B else Neutral.E2E8F0
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Presets Chip Row (Horizontally Scrollable Pill Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (equalizerEnabled) 1f else 0.45f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EqualizerPreset.entries.forEach { preset ->
                    val isSelected = currentPreset.equals(preset.id, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) {
                                    AccentGradient
                                } else {
                                    if (isLowGlassOpacity) SolidColor(Color(0x1AFFFFFF)) else SolidColor(sheetWhite(Neutral.F1F5F9))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) {
                                    Color(0x40FFFFFF)
                                } else {
                                    if (isLowGlassOpacity) Color(0x33FFFFFF) else Neutral.CBD5E1
                                },
                                shape = RoundedCornerShape(50)
                            )
                            .clickable(enabled = equalizerEnabled) {
                                onSelectPreset(preset)
                                onShowNotification("Preset: ${preset.displayName}")
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName,
                            color = if (isSelected) Color.White else (if (isLowGlassOpacity) Neutral.S94A3B8 else Neutral.S64748B),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 2. 5-Band Vertical Equalizer Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Band 1: 60 Hz
                ChunkyEqBandColumn(
                    freqLabel = "60Hz",
                    gain = eq60Hz,
                    onGainChange = onEq60HzChange,
                    enabled = equalizerEnabled,
                    isLowGlassOpacity = isLowGlassOpacity,
                    sliderHeight = if (isLandscape) 105.dp else 135.dp
                )

                // Band 2: 230 Hz
                ChunkyEqBandColumn(
                    freqLabel = "230Hz",
                    gain = eq230Hz,
                    onGainChange = onEq230HzChange,
                    enabled = equalizerEnabled,
                    isLowGlassOpacity = isLowGlassOpacity,
                    sliderHeight = if (isLandscape) 105.dp else 135.dp
                )

                // Band 3: 910 Hz
                ChunkyEqBandColumn(
                    freqLabel = "910Hz",
                    gain = eq910Hz,
                    onGainChange = onEq910HzChange,
                    enabled = equalizerEnabled,
                    isLowGlassOpacity = isLowGlassOpacity,
                    sliderHeight = if (isLandscape) 105.dp else 135.dp
                )

                // Band 4: 3.6 kHz
                ChunkyEqBandColumn(
                    freqLabel = "3.6kHz",
                    gain = eq3600Hz,
                    onGainChange = onEq3600HzChange,
                    enabled = equalizerEnabled,
                    isLowGlassOpacity = isLowGlassOpacity,
                    sliderHeight = if (isLandscape) 105.dp else 135.dp
                )

                // Band 5: 14 kHz
                ChunkyEqBandColumn(
                    freqLabel = "14kHz",
                    gain = eq14000Hz,
                    onGainChange = onEq14000HzChange,
                    enabled = equalizerEnabled,
                    isLowGlassOpacity = isLowGlassOpacity,
                    sliderHeight = if (isLandscape) 105.dp else 135.dp
                )
            }

            // Subtle Divider
            HorizontalDivider(
                color = if (isLowGlassOpacity) Color(0x26FFFFFF) else Neutral.E2E8F0,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // 3. Volume Boost Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (equalizerEnabled) 1f else 0.45f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOLUME BOOST",
                        color = if (isLowGlassOpacity) Neutral.S94A3B8 else Neutral.S64748B,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (volumeBoostDb <= 0.05f) "Off" else "+${volumeBoostDb.roundToInt()} dB",
                        color = if (volumeBoostDb > 0.05f) {
                            if (isLowGlassOpacity) Color(0xFF818CF8) else Color(0xFF2D1F3D)
                        } else {
                            if (isLowGlassOpacity) Neutral.S64748B else Neutral.S94A3B8
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Chunky Horizontal Slider
                HorizontalBoostChunkySlider(
                    value = volumeBoostDb,
                    onValueChange = onVolumeBoostDbChange,
                    valueRange = 0f..10f,
                    enabled = equalizerEnabled,
                    isLowGlassOpacity = isLowGlassOpacity
                )

                // Range Labels (0 dB and +10 dB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0 dB",
                        color = if (isLowGlassOpacity) Neutral.S64748B else Neutral.S94A3B8,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "+10 dB",
                        color = if (isLowGlassOpacity) Neutral.S64748B else Neutral.S94A3B8,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 7. A-B REPEAT LOOP PANEL
 */
@Composable
fun AbLoopSelectionPanel(
    currentPositionMs: Long,
    loopPointA: Long?,
    loopPointB: Long?,
    onSetPointA: (Long) -> Unit,
    onSetPointB: (Long) -> Unit,
    onClearLoop: () -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    PlayerGlassModalSheet(
        panelKey = "ab_loop",
        isLandscape = isLandscape,
        heightFraction = 0.52f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "A-B Repeat Loop",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isLowGlass = LocalSheetIsLowGlassOpacity.current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (loopPointA != null) (if (isLowGlass) AccentSkyBlue.copy(alpha = 0.21f) else Color(0xFFF3E8FF)) else SheetButtonBgColor)
                        .border(1.dp, if (loopPointA != null) ActiveHighlightColor else SheetButtonBorderColor, RoundedCornerShape(14.dp))
                        .cardBounceClick(scaleDown = 0.95f) { onSetPointA(currentPositionMs) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (loopPointA != null) "Point A: ${formatPlaybackMs(loopPointA)}" else "Set Point A",
                            color = if (loopPointA != null) ActiveHighlightColor else SheetButtonTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "Start marker",
                            color = if (loopPointA != null) ActiveHighlightColor.copy(alpha = 0.8f) else Neutral.S475569,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (loopPointB != null) (if (isLowGlass) AccentSkyBlue.copy(alpha = 0.21f) else Color(0xFFF3E8FF)) else SheetButtonBgColor)
                        .border(1.dp, if (loopPointB != null) ActiveHighlightColor else SheetButtonBorderColor, RoundedCornerShape(14.dp))
                        .cardBounceClick(scaleDown = 0.95f) { onSetPointB(currentPositionMs) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (loopPointB != null) "Point B: ${formatPlaybackMs(loopPointB)}" else "Set Point B",
                            color = if (loopPointB != null) ActiveHighlightColor else SheetButtonTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "End marker",
                            color = if (loopPointB != null) ActiveHighlightColor.copy(alpha = 0.8f) else Neutral.S475569,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            if (loopPointA != null || loopPointB != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFEE2E2))
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(14.dp))
                        .cardBounceClick(scaleDown = 0.96f) {
                            onClearLoop()
                            onDismissRequest()
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clear A-B Repeat Loop",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

/**
 * 8. PLAYLIST / EPISODE SELECTION PANEL
 * Matches the requested screenshot layout:
 * - Header: "Now Playing • X items" with Grid view toggle icon
 * - Episode Cards with:
 *   - 16:9 Thumbnail preview with dark episode index badge ("1", "2", "3", ...)
 *   - Episode Name in bold
 *   - Metadata row with clean pills for Duration ("23:59") and Quality ("1920x1080")
 *   - Active "Playing" badge for currently playing episode
 *   - Two-line drag handle with touch-and-hold vertical drag to reorder episodes up/down
 */
@Composable
fun PlaylistSelectionPanel(
    playlistVideos: List<String> = emptyList(),
    playlistItems: List<VideoItem> = emptyList(),
    currentIndex: Int,
    totalEpisodes: Int = playlistVideos.size.coerceAtLeast(playlistItems.size),
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    thumbnailStrategy: com.example.ui.state.ThumbnailStrategy = com.example.ui.state.ThumbnailStrategy.SMART,
    thumbnailQuality: com.example.ui.state.ThumbnailQuality = com.example.ui.state.ThumbnailQuality.HIGH,
    thumbnailFallbackSecond: Int = 1,
    onSelectPlaylistItem: (Int) -> Unit,
    onReorderPlaylist: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onDismissRequest: () -> Unit
) {
    val effectiveCount = if (playlistItems.isNotEmpty()) playlistItems.size else playlistVideos.size
    var isGridView by remember { mutableStateOf(false) }

    PlayerGlassModalSheet(
        panelKey = "playlist",
        isLandscape = isLandscape,
        glassBlurTransparency = glassBlurTransparency,
        heightFraction = 0.58f,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Now Playing • $effectiveCount ${if (effectiveCount == 1) "item" else "items"}",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        },
        headerEndContent = {
            // 4-box Grid View / List View Toggle Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isGridView) ActiveHighlightColor.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { isGridView = !isGridView }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                                imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                    tint = if (isGridView) ActiveHighlightColor else NormalItemColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        if (isGridView) {
            // GRID VIEW MODE: scrolls normally; touch and hold a card to lift it, then drag it up / down / left / right
            // to reorder (same idea as the list view). The cards ease in when the grid opens.
            val gridColumns = if (isLandscape) 3 else 2
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
                initialFirstVisibleItemIndex = (currentIndex - 1).coerceIn(0, (effectiveCount - 1).coerceAtLeast(0))
            )
            var gridHeightPx by remember { mutableFloatStateOf(0f) }
            val gridDensity = LocalDensity.current
            val gridHaptic = LocalHapticFeedback.current
            val gridScope = rememberCoroutineScope()
            val gridCardEstimatePx = with(gridDensity) { 150.dp.toPx() }
            val gridRows = (effectiveCount + gridColumns - 1) / gridColumns
            val gridNeedsRoom = gridHeightPx > 0f && gridRows * gridCardEstimatePx > gridHeightPx
            val gridBottomPadding = if (gridNeedsRoom) {
                with(gridDensity) { (gridHeightPx - gridCardEstimatePx).coerceAtLeast(0f).toDp() }
            } else 0.dp

            var gridDragIndex by remember { mutableStateOf<Int?>(null) }
            var gridDragPointer by remember { mutableStateOf(Offset.Zero) }
            var gridDragTopLeft by remember { mutableStateOf(Offset.Zero) }
            var gridGrab by remember { mutableStateOf(Offset.Zero) }
            var gridDragWidthPx by remember { mutableFloatStateOf(0f) }
            var gridDropping by remember { mutableStateOf(false) }
            var gridIntro by remember { mutableStateOf(true) }
            val gridCount = effectiveCount
            val currentGridCount by rememberUpdatedState(gridCount)
            val onReorderLatest by rememberUpdatedState(onReorderPlaylist)

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(800L)
                gridIntro = false
            }

            // Keep the playing item at the top of the grid (see the list view for the reasoning).
            LaunchedEffect(gridNeedsRoom) {
                if (effectiveCount > 0 && gridDragIndex == null) {
                    gridState.scrollToItem((currentIndex - 1).coerceIn(0, effectiveCount - 1))
                }
            }

            // The card under the finger (or the nearest one) is where the lifted card will land.
            fun gridHoverAt(point: Offset, fallback: Int): Int {
                val infos = gridState.layoutInfo.visibleItemsInfo.filter { it.index in 0 until currentGridCount }
                infos.firstOrNull {
                    point.x >= it.offset.x && point.x < it.offset.x + it.size.width &&
                        point.y >= it.offset.y && point.y < it.offset.y + it.size.height
                }?.let { return it.index }
                return infos.minByOrNull {
                    val cx = it.offset.x + it.size.width / 2f
                    val cy = it.offset.y + it.size.height / 2f
                    (point.x - cx) * (point.x - cx) + (point.y - cy) * (point.y - cy)
                }?.index ?: fallback
            }
            val gridHoverIndex by remember {
                derivedStateOf {
                    val from = gridDragIndex
                    if (from == null) -1 else gridHoverAt(gridDragPointer, from)
                }
            }

            // Scroll the grid while a card is held near its top or bottom edge.
            LaunchedEffect(gridDragIndex) {
                if (gridDragIndex == null) return@LaunchedEffect
                val edge = with(gridDensity) { 72.dp.toPx() }
                val maxStep = with(gridDensity) { 9.dp.toPx() }
                while (gridDragIndex != null) {
                    val y = gridDragPointer.y
                    val step = when {
                        gridHeightPx <= 0f -> 0f
                        y < edge -> -maxStep * ((edge - y) / edge).coerceIn(0f, 1f)
                        y > gridHeightPx - edge -> maxStep * ((y - (gridHeightPx - edge)) / edge).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    if (step != 0f) gridState.scrollBy(step)
                    kotlinx.coroutines.delay(16L)
                }
            }

            @Composable
            fun GridCardFor(idx: Int, cardModifier: Modifier, clickable: Boolean) {
                val onCardClick = {
                    if (clickable && gridDragIndex == null) {
                        onSelectPlaylistItem(idx + 1)
                        onDismissRequest()
                    }
                }
                if (playlistItems.isNotEmpty()) {
                    val item = playlistItems[idx]
                    PlaylistEpisodeGridCard(
                        index = idx + 1,
                        title = item.displayName.substringBeforeLast("."),
                        durationText = formatDuration(item.durationMs),
                        qualityText = if (item.resolution.isNotBlank()) item.resolution else "1920x1080",
                        isCurrent = (idx + 1) == currentIndex,
                        videoItem = item,
                        thumbnailStrategy = thumbnailStrategy,
                        thumbnailQuality = thumbnailQuality,
                        thumbnailFallbackSecond = thumbnailFallbackSecond,
                        modifier = cardModifier,
                        onClick = onCardClick
                    )
                } else {
                    PlaylistEpisodeGridCard(
                        index = idx + 1,
                        title = playlistVideos[idx].substringBeforeLast("."),
                        durationText = "23:59",
                        qualityText = "1920x1080",
                        isCurrent = (idx + 1) == currentIndex,
                        videoItem = null,
                        thumbnailStrategy = thumbnailStrategy,
                        thumbnailQuality = thumbnailQuality,
                        thumbnailFallbackSecond = thumbnailFallbackSecond,
                        modifier = cardModifier,
                        onClick = onCardClick
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        // Watches the finger without taking it: taps and scrolling keep working. After a long press the
                        // finger belongs to the drag until it lifts (so a card scrolling out of view cannot cancel it).
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            if (gridDropping || gridDragIndex != null) return@awaitEachGesture
                            val pointerId = down.id
                            val slop = viewConfiguration.touchSlop
                            val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Initial)
                                    val c = ev.changes.firstOrNull { it.id == pointerId } ?: return@withTimeoutOrNull true
                                    if (!c.pressed) return@withTimeoutOrNull true
                                    if ((c.position - down.position).getDistance() > slop) return@withTimeoutOrNull true
                                }
                                @Suppress("UNREACHABLE_CODE")
                                false
                            }
                            // null = the timeout ran out while the finger stayed put = long press
                            if (cancelled != null) return@awaitEachGesture
                            val grabbed = gridState.layoutInfo.visibleItemsInfo.firstOrNull {
                                it.index in 0 until currentGridCount &&
                                    down.position.x >= it.offset.x && down.position.x < it.offset.x + it.size.width &&
                                    down.position.y >= it.offset.y && down.position.y < it.offset.y + it.size.height
                            } ?: return@awaitEachGesture

                            try { com.example.util.AppHaptics.perform(gridHaptic) } catch (_: Throwable) {}
                            gridGrab = Offset(down.position.x - grabbed.offset.x, down.position.y - grabbed.offset.y)
                            gridDragWidthPx = grabbed.size.width.toFloat()
                            gridDragPointer = down.position
                            gridDragTopLeft = down.position - gridGrab
                            gridDragIndex = grabbed.index

                            while (true) {
                                val ev = awaitPointerEvent(PointerEventPass.Initial)
                                val c = ev.changes.firstOrNull { it.id == pointerId } ?: break
                                c.consume()
                                gridDragPointer = c.position
                                gridDragTopLeft = c.position - gridGrab
                                if (!c.pressed) break
                            }

                            // Dropped: the card glides into the slot it was hovering, then the new order is stored.
                            val from = gridDragIndex ?: return@awaitEachGesture
                            val to = gridHoverAt(gridDragPointer, from)
                            val startTopLeft = gridDragTopLeft
                            val slot = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == to }
                            gridDropping = true
                            gridScope.launch {
                                if (slot != null) {
                                    val end = Offset(slot.offset.x.toFloat(), slot.offset.y.toFloat())
                                    val progress = Animatable(0f)
                                    progress.animateTo(1f, tween(durationMillis = 170, easing = FastOutSlowInEasing)) {
                                        gridDragTopLeft = androidx.compose.ui.geometry.lerp(startTopLeft, end, value)
                                    }
                                }
                                if (to != from) onReorderLatest(from, to)
                                gridDragIndex = null
                                gridDropping = false
                            }
                        }
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { gridHeightPx = it.size.height.toFloat() },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp + gridBottomPadding)
                ) {
                    items(
                        count = gridCount,
                        key = { idx ->
                            if (playlistItems.isNotEmpty()) "${playlistItems[idx].id}_${playlistItems[idx].path}_$idx"
                            else "${playlistVideos[idx]}_$idx"
                        }
                    ) { idx ->
                        val from = gridDragIndex ?: -1
                        val hover = gridHoverIndex
                        val isDragged = from == idx
                        // Cards between the lifted one and its landing slot step aside into the neighbouring slot.
                        val shiftTo = when {
                            from == -1 || hover == -1 || isDragged -> idx
                            from < hover && idx in (from + 1)..hover -> idx - 1
                            from > hover && idx in hover until from -> idx + 1
                            else -> idx
                        }
                        var shiftX = 0f
                        var shiftY = 0f
                        if (shiftTo != idx) {
                            val infos = gridState.layoutInfo.visibleItemsInfo
                            val me = infos.firstOrNull { it.index == idx }
                            val other = infos.firstOrNull { it.index == shiftTo }
                            if (me != null && other != null) {
                                shiftX = (other.offset.x - me.offset.x).toFloat()
                                shiftY = (other.offset.y - me.offset.y).toFloat()
                            }
                        }
                        val shiftSpec: androidx.compose.animation.core.AnimationSpec<Float> =
                            if (gridDragIndex == null) androidx.compose.animation.core.snap()
                            else spring(dampingRatio = 0.78f, stiffness = 520f)
                        val animX = animateFloatAsState(shiftX, shiftSpec, label = "GridShiftX")
                        val animY = animateFloatAsState(shiftY, shiftSpec, label = "GridShiftY")

                        // Entrance: only the cards of the first screenful ease in, staggered; later ones appear plainly.
                        val appear = remember { Animatable(if (gridIntro) 0f else 1f) }
                        LaunchedEffect(Unit) {
                            if (appear.value < 1f) {
                                val rank = (idx - gridState.firstVisibleItemIndex).coerceIn(0, 9)
                                kotlinx.coroutines.delay(rank * 40L)
                                appear.animateTo(1f, tween(durationMillis = 340, easing = FastOutSlowInEasing))
                            }
                        }

                        GridCardFor(
                            idx = idx,
                            cardModifier = Modifier.graphicsLayer {
                                val a = appear.value
                                alpha = if (isDragged) 0f else a
                                scaleX = 0.92f + 0.08f * a
                                scaleY = 0.92f + 0.08f * a
                                translationX = animX.value
                                translationY = animY.value + (1f - a) * 26f
                            },
                            clickable = true
                        )
                    }
                }

                // The lifted card floats above the grid, a little bigger and with a shadow, and follows the finger.
                gridDragIndex?.let { draggedIdx ->
                    if (draggedIdx in 0 until gridCount && gridDragWidthPx > 0f) {
                        GridCardFor(
                            idx = draggedIdx,
                            cardModifier = Modifier
                                .offset { IntOffset(gridDragTopLeft.x.roundToInt(), gridDragTopLeft.y.roundToInt()) }
                                .width(with(gridDensity) { gridDragWidthPx.toDp() })
                                .graphicsLayer {
                                    scaleX = 1.06f
                                    scaleY = 1.06f
                                    shadowElevation = 26f
                                    shape = RoundedCornerShape(16.dp)
                                    clip = false
                                    alpha = 0.97f
                                },
                            clickable = false
                        )
                    }
                }
            }
        } else {
            // LIST VIEW MODE with Smooth Multi-Item Continuous Drag-to-Reorder Support matching ControlLayoutEditor
            val playlistListState = rememberLazyListState(
                initialFirstVisibleItemIndex = (currentIndex - 1).coerceIn(
                    0,
                    (if (playlistItems.isNotEmpty()) playlistItems.size else playlistVideos.size).let { (it - 1).coerceAtLeast(0) }
                )
            )
            var playlistCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            var playlistHeightPx by remember { mutableFloatStateOf(0f) }
            var dragPointerYInList by remember { mutableFloatStateOf(-1f) }
            var dragGrabOffsetY by remember { mutableFloatStateOf(0f) }
            var dragPointerId by remember { mutableStateOf<PointerId?>(null) }

            val itemsCount = if (playlistItems.isNotEmpty()) playlistItems.size else playlistVideos.size
            var draggingIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var measuredItemHeightPx by remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current
            val haptic = LocalHapticFeedback.current

            LaunchedEffect(draggingIndex) {
                if (draggingIndex == null) return@LaunchedEffect
                val edgeZone = with(density) { 86.dp.toPx() }
                val maxScrollPerTick = with(density) { 7.dp.toPx() }

                while (draggingIndex != null) {
                    val pointerY = dragPointerYInList
                    val listH = playlistHeightPx
                    if (pointerY >= 0f && listH > 0f) {
                        val scrollDelta = when {
                            pointerY < edgeZone -> {
                                val ratio = ((edgeZone - pointerY) / edgeZone).coerceIn(0f, 1f)
                                -maxScrollPerTick * ratio
                            }
                            pointerY > (listH - edgeZone) -> {
                                val ratio = ((pointerY - (listH - edgeZone)) / edgeZone).coerceIn(0f, 1f)
                                maxScrollPerTick * ratio
                            }
                            else -> 0f
                        }

                        if (scrollDelta != 0f) {
                            val consumed = playlistListState.scrollBy(scrollDelta)
                            if (consumed != 0f) dragOffsetY += consumed
                        }
                    }
                    kotlinx.coroutines.delay(24L)
                }
            }

            val effectiveHeight = if (measuredItemHeightPx > 0f) measuredItemHeightPx else with(density) { 70.dp.toPx() }

            // Open the list with the item that is playing NOW at the top (the items before it stay above, out of view).
            // The list gets extra room under its last item only when it is longer than the panel, so even the very
            // last item can really scroll up to the top; short lists are left as they are.
            val needsTopAlignRoom = playlistHeightPx > 0f && itemsCount * effectiveHeight > playlistHeightPx
            val topAlignBottomPadding = if (needsTopAlignRoom) {
                with(density) { (playlistHeightPx - effectiveHeight).coerceAtLeast(0f).toDp() }
            } else 0.dp
            LaunchedEffect(needsTopAlignRoom, measuredItemHeightPx > 0f) {
                // Not keyed on currentIndex on purpose: a drag-reorder must never make the list jump.
                if (itemsCount > 0 && draggingIndex == null) {
                    playlistListState.scrollToItem((currentIndex - 1).coerceIn(0, itemsCount - 1))
                }
            }
            val fromIndex = draggingIndex ?: -1
            val hoverIndex = if (fromIndex != -1) {
                val pointerY = dragPointerYInList
                playlistListState.layoutInfo.visibleItemsInfo
                    .filter { it.index in 0 until itemsCount }
                    .minByOrNull { item -> kotlin.math.abs(pointerY - (item.offset + item.size / 2f)) }
                    ?.index ?: fromIndex
            } else -1

            val currentDragPointerId by rememberUpdatedState(dragPointerId)
            val currentDraggingIndex by rememberUpdatedState(draggingIndex)
            val currentItemsCount by rememberUpdatedState(itemsCount)

            // The outer Box owns the active pointer lifecycle. This prevents a LazyColumn item
            // disappearing during auto-scroll from cancelling the drag.
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        playlistCoordinates = coordinates
                        playlistHeightPx = coordinates.size.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                val activeId = currentDragPointerId
                                if (activeId != null) {
                                    val change = event.changes.firstOrNull { it.id == activeId }
                                    if (change != null && change.pressed) {
                                        dragPointerYInList = change.position.y
                                    } else {
                                        // Finger lifted, gesture cancelled or pointer lost: the drag always ends here and the card is
                                        // committed where it was dropped (target worked out from the list's CURRENT layout, not from
                                        // a value that may be one frame old), so what is shown is exactly what is stored.
                                        if (change != null) dragPointerYInList = change.position.y
                                        val f = currentDraggingIndex ?: -1
                                        val pointerY = dragPointerYInList
                                        val t = playlistListState.layoutInfo.visibleItemsInfo
                                            .filter { it.index in 0 until currentItemsCount }
                                            .minByOrNull { item -> kotlin.math.abs(pointerY - (item.offset + item.size / 2f)) }
                                            ?.index ?: f
                                        if (f != -1 && t != -1 && f != t) onReorderPlaylist(f, t)
                                        dragPointerId = null
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                        dragPointerYInList = -1f
                                        dragGrabOffsetY = 0f
                                    }
                                }
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = playlistListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp + topAlignBottomPadding)
                ) {
                if (playlistItems.isNotEmpty()) {
                    itemsIndexed(playlistItems, key = { _, item -> item.id }) { idx, item ->
                        val isCurrent = (idx + 1) == currentIndex
                        val title = item.displayName.substringBeforeLast(".")
                        val durationText = formatDuration(item.durationMs)
                        val qualityText = if (item.resolution.isNotBlank()) item.resolution else "1920x1080"
                        val isBeingDragged = (draggingIndex == idx)

                        val targetTranslationY = when {
                            isBeingDragged -> 0f
                            draggingIndex != null && fromIndex != -1 && hoverIndex != -1 -> {
                                when {
                                    fromIndex < hoverIndex && idx in (fromIndex + 1)..hoverIndex -> -effectiveHeight
                                    fromIndex > hoverIndex && idx in hoverIndex until fromIndex -> effectiveHeight
                                    else -> 0f
                                }
                            }
                            else -> 0f
                        }

                        val animatedTranslationY by animateFloatAsState(
                            targetValue = targetTranslationY,
                            // no drag running = the order is already final: jump to 0 (animating from the old shift made rows slide wrongly)
                            animationSpec = if (draggingIndex == null) androidx.compose.animation.core.snap() else spring(
                                dampingRatio = 0.90f,
                                stiffness = 800f
                            ),
                            label = "EpisodeTranslationY_${item.id}"
                        )

                        val visualTranslationY = if (isBeingDragged) dragOffsetY else animatedTranslationY

                        PlaylistEpisodeRow(
                            index = idx + 1,
                            title = title,
                            durationText = durationText,
                            qualityText = qualityText,
                            isCurrent = isCurrent,
                            videoItem = item,
                            thumbnailStrategy = thumbnailStrategy,
                            thumbnailQuality = thumbnailQuality,
                            thumbnailFallbackSecond = thumbnailFallbackSecond,
                            isBeingDragged = isBeingDragged,
                            visualTranslationY = visualTranslationY,
                            modifier = if (isBeingDragged) Modifier.alpha(0f) else Modifier,
                            onMeasureHeight = { h ->
                                if (draggingIndex == null && h > 0f) {
                                    measuredItemHeightPx = h + with(density) { 8.dp.toPx() }
                                }
                            },
                            onDragStart = { pointerId, offset, itemCoords, handleCoords ->
                                try { com.example.util.AppHaptics.perform(haptic) } catch (_: Throwable) {}
                                draggingIndex = idx
                                dragOffsetY = 0f
                                dragPointerId = pointerId
                                handleCoords?.let { handle ->
                                    playlistCoordinates?.let { lst -> dragPointerYInList = lst.localPositionOf(handle, offset).y }
                                    itemCoords?.let { itm -> dragGrabOffsetY = itm.localPositionOf(handle, offset).y }
                                }
                            },
                            onDragDelta = { deltaY, pos, itemCoords ->
                                if (draggingIndex == idx) {
                                    dragOffsetY += deltaY
                                    itemCoords?.let { itm -> playlistCoordinates?.let { lst -> dragPointerYInList = lst.localPositionOf(itm, pos).y } }
                                }
                            },
                            onDragFinish = {},
                            onClick = {
                                if (draggingIndex == null) {
                                    onSelectPlaylistItem(idx + 1)
                                    onDismissRequest()
                                }
                            }
                        )
                    }
                } else {
                    itemsIndexed(playlistVideos, key = { idx, name -> "${name}_$idx" }) { idx, name ->
                        val isCurrent = (idx + 1) == currentIndex
                        val title = name.substringBeforeLast(".")
                        val isBeingDragged = (draggingIndex == idx)

                        val targetTranslationY = when {
                            isBeingDragged -> 0f
                            draggingIndex != null && fromIndex != -1 && hoverIndex != -1 -> {
                                when {
                                    fromIndex < hoverIndex && idx in (fromIndex + 1)..hoverIndex -> -effectiveHeight
                                    fromIndex > hoverIndex && idx in hoverIndex until fromIndex -> effectiveHeight
                                    else -> 0f
                                }
                            }
                            else -> 0f
                        }

                        val animatedTranslationY by animateFloatAsState(
                            targetValue = targetTranslationY,
                            // no drag running = the order is already final: jump to 0 (animating from the old shift made rows slide wrongly)
                            animationSpec = if (draggingIndex == null) androidx.compose.animation.core.snap() else spring(
                                dampingRatio = 0.90f,
                                stiffness = 800f
                            ),
                            label = "EpisodeTranslationY_${name}_$idx"
                        )

                        val visualTranslationY = if (isBeingDragged) dragOffsetY else animatedTranslationY

                        PlaylistEpisodeRow(
                            index = idx + 1,
                            title = title,
                            durationText = "23:59",
                            qualityText = "1920x1080",
                            isCurrent = isCurrent,
                            videoItem = null,
                            thumbnailStrategy = thumbnailStrategy,
                            thumbnailQuality = thumbnailQuality,
                            thumbnailFallbackSecond = thumbnailFallbackSecond,
                            isBeingDragged = isBeingDragged,
                            visualTranslationY = visualTranslationY,
                            modifier = if (isBeingDragged) Modifier.alpha(0f) else Modifier,
                            onMeasureHeight = { h ->
                                if (draggingIndex == null && h > 0f) {
                                    measuredItemHeightPx = h + with(density) { 8.dp.toPx() }
                                }
                            },
                            onDragStart = { pointerId, offset, itemCoords, handleCoords ->
                                try { com.example.util.AppHaptics.perform(haptic) } catch (_: Throwable) {}
                                draggingIndex = idx
                                dragOffsetY = 0f
                                dragPointerId = pointerId
                                handleCoords?.let { handle ->
                                    playlistCoordinates?.let { lst -> dragPointerYInList = lst.localPositionOf(handle, offset).y }
                                    itemCoords?.let { itm -> dragGrabOffsetY = itm.localPositionOf(handle, offset).y }
                                }
                            },
                            onDragDelta = { deltaY, pos, itemCoords ->
                                if (draggingIndex == idx) {
                                    dragOffsetY += deltaY
                                    itemCoords?.let { itm -> playlistCoordinates?.let { lst -> dragPointerYInList = lst.localPositionOf(itm, pos).y } }
                                }
                            },
                            onDragFinish = {},
                            onClick = {
                                if (draggingIndex == null) {
                                    onSelectPlaylistItem(idx + 1)
                                    onDismissRequest()
                                }
                            }
                        )
                    }
                }
            }
                draggingIndex?.let { draggedIndex ->
                    if (draggedIndex in 0 until itemsCount) {
                        val topPx = dragPointerYInList - dragGrabOffsetY
                        if (playlistItems.isNotEmpty()) {
                            val dragged = playlistItems[draggedIndex]
                            PlaylistEpisodeRow(
                                index = draggedIndex + 1,
                                title = dragged.displayName.substringBeforeLast("."),
                                durationText = formatDuration(dragged.durationMs),
                                qualityText = dragged.resolution.ifBlank { "1920x1080" },
                                isCurrent = (draggedIndex + 1) == currentIndex,
                                videoItem = dragged,
                                thumbnailStrategy = thumbnailStrategy,
                                thumbnailQuality = thumbnailQuality,
                                thumbnailFallbackSecond = thumbnailFallbackSecond,
                                isBeingDragged = true,
                                visualTranslationY = 0f,
                                enableDrag = false,
                                modifier = Modifier.offset { IntOffset(0, topPx.roundToInt()) },
                                onMeasureHeight = {},
                                onDragStart = { _, _, _, _ -> },
                                onDragDelta = { _, _, _ -> },
                                onDragFinish = {},
                                onClick = {}
                            )
                        } else {
                            val name = playlistVideos[draggedIndex]
                            PlaylistEpisodeRow(
                                index = draggedIndex + 1,
                                title = name.substringBeforeLast("."),
                                durationText = "23:59",
                                qualityText = "1920x1080",
                                isCurrent = (draggedIndex + 1) == currentIndex,
                                videoItem = null,
                                thumbnailStrategy = thumbnailStrategy,
                                thumbnailQuality = thumbnailQuality,
                                thumbnailFallbackSecond = thumbnailFallbackSecond,
                                isBeingDragged = true,
                                visualTranslationY = 0f,
                                enableDrag = false,
                                modifier = Modifier.offset { IntOffset(0, topPx.roundToInt()) },
                                onMeasureHeight = {},
                                onDragStart = { _, _, _, _ -> },
                                onDragDelta = { _, _, _ -> },
                                onDragFinish = {},
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Grid View Card matching modern 4-boxes grid layout
 */
@Composable
private fun PlaylistEpisodeGridCard(
    index: Int,
    title: String,
    durationText: String,
    qualityText: String,
    isCurrent: Boolean,
    videoItem: VideoItem?,
    thumbnailStrategy: com.example.ui.state.ThumbnailStrategy,
    thumbnailQuality: com.example.ui.state.ThumbnailQuality,
    thumbnailFallbackSecond: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(videoItem) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(
        videoItem?.id,
        videoItem?.uri,
        videoItem?.path,
        videoItem?.thumbnailUrl,
        thumbnailStrategy,
        thumbnailQuality,
        thumbnailFallbackSecond
    ) {
        thumbnailBitmap = null
        if (videoItem != null) {
            repeat(3) { attempt ->
                if (thumbnailBitmap == null) {
                    // Online / media-plugin episodes (also the ones still queued, not resolved yet): their picture is a
                    // web link, so it is downloaded instead of reading a frame from a local file.
                    val remoteThumb = videoItem.thumbnailUrl?.takeIf { videoItem.isOnline && it.isNotBlank() }
                    thumbnailBitmap = if (remoteThumb != null) {
                        loadRemoteAudioThumbnail(remoteThumb, 480, context)
                    } else loadVideoThumbnail(
                        context, videoItem.id, videoItem.uri, videoItem.path,
                        strategy = thumbnailStrategy,
                        quality = thumbnailQuality,
                        fallbackSecond = thumbnailFallbackSecond
                    )
                    if (thumbnailBitmap == null && attempt < 2) {
                        kotlinx.coroutines.delay(90L * (attempt + 1))
                    }
                }
            }
        }
    }

    // Slightly see-through so the video/background still shows behind the playing item.
    val activeCardBg = sheetWhite(AccentContainerLight)
    val activeBorderColor = AccentSkyBlueStrong
    val normalCardBg = Color(0x0A000000)
    val normalBorderColor = Color(0x14000000)

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) activeCardBg else normalCardBg)
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                color = if (isCurrent) activeBorderColor else normalBorderColor,
                shape = shape
            )
            .cardBounceClick(scaleDown = 0.96f, onClick = onClick)
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Thumbnail Box with Episode Badge & Duration Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Neutral.D1E293B)
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Neutral.D334155, Neutral.D1E293B)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Top-Left Episode Number Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom-Right Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = durationText,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Playing badge overlay if current
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentSkyBlueStrong)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Playing",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Title
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) Neutral.D0F172A else NormalItemColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Quality & Episode Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isCurrent) AccentSkyBlueStrong.copy(alpha = 0.13f) else Color(0x18000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = qualityText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCurrent) AccentSkyBlueStrong else NormalItemColor.copy(alpha = 0.85f)
                    )
                }

                Text(
                    text = "Ep $index",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrent) Neutral.D0F172A else SecondaryTextColor
                )
            }
        }
    }
}

/** Loaded episode pictures, so the floating drag copy (and re-shown rows) never flash an empty placeholder. */
private val episodeRowThumbCache = android.util.LruCache<String, Bitmap>(48)

/**
 * Single Playlist Episode Row with smooth continuous Drag-to-Reorder matching ControlLayoutEditor
 */
@Composable
private fun PlaylistEpisodeRow(
    index: Int,
    title: String,
    durationText: String,
    qualityText: String,
    isCurrent: Boolean,
    videoItem: VideoItem?,
    thumbnailStrategy: com.example.ui.state.ThumbnailStrategy,
    thumbnailQuality: com.example.ui.state.ThumbnailQuality,
    thumbnailFallbackSecond: Int,
    isBeingDragged: Boolean,
    visualTranslationY: Float,
    enableDrag: Boolean = true,
    modifier: Modifier = Modifier,
    onMeasureHeight: (Float) -> Unit,
    onDragStart: (PointerId, Offset, LayoutCoordinates?, LayoutCoordinates?) -> Unit,
    onDragDelta: (Float, Offset, LayoutCoordinates?) -> Unit,
    onDragFinish: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // Already-loaded picture is reused (also by the floating copy that follows the finger while dragging), so the drag
    // never shows an empty placeholder frame first (that was the blink).
    val thumbCacheKey = if (videoItem != null) {
        "${videoItem.id}|${videoItem.uri}|${videoItem.path}|${videoItem.thumbnailUrl}|$thumbnailStrategy|$thumbnailQuality|$thumbnailFallbackSecond"
    } else null
    var thumbnailBitmap by remember(videoItem, thumbCacheKey) {
        mutableStateOf<Bitmap?>(thumbCacheKey?.let { episodeRowThumbCache.get(it) })
    }
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // The drag gestures below live as long as the row; after a reorder the row sits at a NEW index, so they must always
    // call the newest callbacks (not the ones from the first composition, which still hold the old index).
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)

    LaunchedEffect(
        videoItem?.id,
        videoItem?.uri,
        videoItem?.path,
        videoItem?.thumbnailUrl,
        thumbnailStrategy,
        thumbnailQuality,
        thumbnailFallbackSecond
    ) {
        if (thumbnailBitmap != null) return@LaunchedEffect
        if (videoItem != null) {
            repeat(3) { attempt ->
                if (thumbnailBitmap == null) {
                    // Online / media-plugin episodes (also the ones still queued, not resolved yet): their picture is a
                    // web link, so it is downloaded instead of reading a frame from a local file.
                    val remoteThumb = videoItem.thumbnailUrl?.takeIf { videoItem.isOnline && it.isNotBlank() }
                    thumbnailBitmap = if (remoteThumb != null) {
                        loadRemoteAudioThumbnail(remoteThumb, 480, context)
                    } else loadVideoThumbnail(
                        context, videoItem.id, videoItem.uri, videoItem.path,
                        strategy = thumbnailStrategy,
                        quality = thumbnailQuality,
                        fallbackSecond = thumbnailFallbackSecond
                    )
                    if (thumbnailBitmap == null && attempt < 2) {
                        kotlinx.coroutines.delay(90L * (attempt + 1))
                    }
                }
            }
            val loaded = thumbnailBitmap
            if (loaded != null && thumbCacheKey != null) episodeRowThumbCache.put(thumbCacheKey, loaded)
        }
    }

    // Slightly see-through so the video/background still shows behind the playing item.
    val activeCardBg = sheetWhite(AccentContainerLight)
    val activeBorderColor = AccentSkyBlueStrong
    val normalCardBg = Color(0x0A000000)
    val normalBorderColor = Color(0x14000000)
    val draggedBorderGradient = Brush.linearGradient(listOf(AccentSkyBlue, com.example.ui.theme.AccentMid))

    val shape = RoundedCornerShape(16.dp)
    val itemKey = videoItem?.id ?: "$title$index"

    // true only for the floating copy that follows the finger; the invisible in-list row of the same item gets no lifted styling
    val isGhost = isBeingDragged && !enableDrag

    val dragBorderModifier = if (isGhost) {
        Modifier.border(2.dp, draggedBorderGradient, shape)
    } else {
        Modifier.border(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) activeBorderColor else normalBorderColor,
            shape = shape
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isGhost) 99f else 1f)
            .graphicsLayer {
                translationY = if (isBeingDragged && !isGhost) 0f else visualTranslationY
            }
            // Floating copy: same look as the audio queue (soft shadow under a nearly solid card). The old layer shadow under
            // a see-through card drew a long faint white bar through it.
            .then(if (isGhost) Modifier.shadow(18.dp, shape) else Modifier)
            .clip(shape)
            .background(
                if (isGhost) AccentContainerLight.copy(alpha = 0.92f)
                else if (isCurrent) activeCardBg else normalCardBg
            )
            .then(dragBorderModifier)
            .onGloballyPositioned { coordinates ->
                itemCoordinates = coordinates
                if (coordinates.size.height > 0) {
                    onMeasureHeight(coordinates.size.height.toFloat())
                }
            }
            .pointerInput(itemKey) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) return@awaitEachGesture

                    currentOnDragStart(down.id, longPress.position, itemCoordinates, handleCoordinates)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val delta = change.positionChange().y
                        if (delta != 0f) {
                            change.consume()
                            currentOnDragDelta(delta, change.position, itemCoordinates)
                        }
                    }
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Thumbnail Box with Episode Number Badge
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Neutral.D1E293B)
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Neutral.D334155, Neutral.D1E293B)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            imageVector = Icons.Outlined.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Top-Left Episode Number Badge (Black rounded square with bold number)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.5.dp, vertical = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val isHighlighted = isBeingDragged || isCurrent
            // 2. Middle Content: Title and Metadata Pills (Time and Quality)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) Neutral.D0F172A else NormalItemColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1200,
                            initialDelayMillis = 1000
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isHighlighted) AccentSkyBlueStrong.copy(alpha = 0.13f) else Color(0x18000000))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = durationText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHighlighted) AccentSkyBlueStrong else NormalItemColor.copy(alpha = 0.85f)
                        )
                    }

                    // Quality Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isHighlighted) AccentSkyBlueStrong.copy(alpha = 0.13f) else Color(0x18000000))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = qualityText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHighlighted) AccentSkyBlueStrong else NormalItemColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3. Playing Badge (if active)
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(sheetWhite(AccentContainerLight))
                        .border(1.dp, AccentSkyBlueStrong, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Playing",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentSkyBlueStrong
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // 4. Two-line Drag Handle (=) that also immediately accepts vertical drag
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isBeingDragged) ActiveHighlightColor.copy(alpha = 0.15f) else Color.Transparent)
                    .onGloballyPositioned { handleCoordinates = it }
                    .then(
                        if (enableDrag) {
                            Modifier.pointerInput(itemKey) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val longPress = awaitLongPressOrCancellation(down.id)
                                    if (longPress != null) {
                                        currentOnDragStart(down.id, longPress.position, itemCoordinates, handleCoordinates)
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) break
                                            val delta = change.positionChange().y
                                            if (delta != 0f) {
                                                change.consume()
                                                currentOnDragDelta(delta, change.position, itemCoordinates)
                                            }
                                        }
                                    }
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 2.5.dp)
                            .clip(RoundedCornerShape(1.2.dp))
                            .background(if (isBeingDragged) ActiveHighlightColor else Neutral.S64748B)
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 2.5.dp)
                            .clip(RoundedCornerShape(1.2.dp))
                            .background(if (isBeingDragged) ActiveHighlightColor else Neutral.S64748B)
                    )
                }
            }
        }
    }
}

// =========================================================================
// REUSABLE SUB-COMPONENTS & ROWS (MATCHING SCREENSHOT AESTHETICS)
// =========================================================================

internal fun trackMetadataTags(
    track: PlayerMediaTrack,
    showMediaInfo: Boolean = true,
    selectionLabel: String? = null
): List<String> = buildList {
    if (!selectionLabel.isNullOrBlank()) add(selectionLabel)
    if (showMediaInfo) {
        formatLanguageName(track.language).takeIf { it.isNotBlank() }?.let(::add)
        track.language.trim().takeIf { it.isNotBlank() }?.let { add(it.uppercase(Locale.ROOT)) }
        resolveTrackFormatBadge(track)?.takeIf { it.isNotBlank() }?.let(::add)
    }
    if (track.isDefault) add("Default")
    if (track.isForced) add("Forced")
}.distinct()

@Composable
internal fun SwipeableTrackItem(
    primaryTitle: String,
    badgeTag: String?,
    metadataTags: List<String> = emptyList(),
    isSelected: Boolean,
    isSecondary: Boolean = false,
    isLastTouched: Boolean = false,
    selectedColor: Color = ActiveHighlightColor,
    selectedBgColor: Color = ActiveItemBg,
    selectionTag: String? = null,
    translationStatus: TrackTranslationStatus = TrackTranslationStatus.Idle,
    onRemove: (() -> Unit)? = null,
    // false (default): swipe RIGHT -> LEFT removes the row (audio tracks).
    // true: swipe LEFT -> RIGHT removes the row (uploaded subtitles).
    swipeLeftToRightToRemove: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (onRemove == null) {
        SelectableTrackItem(
            indexNumber = null,
            primaryTitle = primaryTitle,
            badgeTag = badgeTag,
            metadataTags = metadataTags,
            isSelected = isSelected,
            isSecondary = isSecondary,
            isLastTouched = isLastTouched,
            selectedColor = selectedColor,
            selectedBgColor = selectedBgColor,
            selectionTag = selectionTag,
            translationStatus = translationStatus,
            onClick = onClick
        )
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var isDismissed by remember { mutableStateOf(false) }
    var rowWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 85.dp.toPx() }
    val cornerPx = with(density) { 12.dp.toPx() }
    // +1 = the row travels to the right, -1 = to the left while it is being removed.
    val dir = if (swipeLeftToRightToRemove) 1f else -1f
    val removeRed = Color(0xFFEF4444)

    AnimatedVisibility(
        visible = !isDismissed,
        exit = shrinkVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(160)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowWidthPx = it.width.toFloat() }
                .clip(RoundedCornerShape(12.dp))
                // Red "remove" surface: drawn behind the row, only in the strip the row has
                // already slid away from, so the whole card visibly slides (not just its text).
                .drawBehind {
                    val revealed = kotlin.math.abs(offsetX.value)
                    if (revealed > 1f) {
                        val progress = (revealed / dismissThresholdPx).coerceIn(0f, 1f)
                        val w = revealed.coerceAtMost(size.width)
                        val left = if (dir > 0f) 0f else size.width - w
                        drawRoundRect(
                            color = removeRed.copy(alpha = 0.18f + 0.42f * progress),
                            topLeft = Offset(left, 0f),
                            size = androidx.compose.ui.geometry.Size(w, size.height),
                            cornerRadius = CornerRadius(cornerPx, cornerPx)
                        )
                    }
                }
        ) {
            // Icon + label of the remove action, sitting on the red surface.
            Row(
                modifier = Modifier
                    .align(if (dir > 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 14.dp)
                    .graphicsLayer {
                        val revealed = kotlin.math.abs(offsetX.value)
                        val p = ((revealed - dismissThresholdPx * 0.35f) /
                            (dismissThresholdPx * 0.65f)).coerceIn(0f, 1f)
                        alpha = p
                        scaleX = 0.8f + 0.2f * p
                        scaleY = 0.8f + 0.2f * p
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StyledIcon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove",
                    tint = removeRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Remove",
                    color = removeRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Draggable row (the complete card slides as one piece)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .graphicsLayer {
                        val progress = (kotlin.math.abs(offsetX.value) / (rowWidthPx.takeIf { it > 0f } ?: 900f))
                            .coerceIn(0f, 1f)
                        alpha = 1f - 0.5f * progress
                    }
                    .pointerInput(swipeLeftToRightToRemove) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDrag = 0f
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (kotlin.math.abs(offsetX.value) > dismissThresholdPx) {
                                        val exitTarget = dir * ((rowWidthPx.takeIf { it > 0f } ?: 900f) + 40f)
                                        offsetX.animateTo(
                                            targetValue = exitTarget,
                                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                                        )
                                        isDismissed = true
                                        // Let the row finish collapsing before it leaves the list.
                                        delay(230)
                                        onRemove()
                                    } else {
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, animationSpec = tween(150))
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                totalDrag += dragAmount
                                // Only the removal direction moves the row; the opposite direction
                                // is ignored so the row can never be pushed the wrong way.
                                val inRemoveDirection = totalDrag * dir > 0f
                                if (inRemoveDirection) {
                                    change.consume()
                                    coroutineScope.launch {
                                        val next = offsetX.value + dragAmount
                                        offsetX.snapTo(if (dir > 0f) next.coerceAtLeast(0f) else next.coerceAtMost(0f))
                                    }
                                }
                            }
                        )
                    }
            ) {
                SelectableTrackItem(
                    indexNumber = null,
                    primaryTitle = primaryTitle,
                    badgeTag = badgeTag,
                    isSelected = isSelected,
                    isSecondary = isSecondary,
                    isLastTouched = isLastTouched,
                    selectedColor = selectedColor,
                    selectedBgColor = selectedBgColor,
                    selectionTag = selectionTag,
                    translationStatus = translationStatus,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
internal fun SelectableTrackItem(
    indexNumber: String?,
    primaryTitle: String,
    badgeTag: String?,
    metadataTags: List<String> = emptyList(),
    isSelected: Boolean,
    isSecondary: Boolean = false,
    isLastTouched: Boolean = false,
    selectedColor: Color = ActiveHighlightColor,
    selectedBgColor: Color = ActiveItemBg,
    selectionTag: String? = null,
    translationStatus: TrackTranslationStatus = TrackTranslationStatus.Idle,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) selectedBgColor else Color.Transparent)
            .cardBounceClick(scaleDown = 0.98f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Radio circle indicator — gradient ring while selected (per AI
                    // Translate UI spec), plain ring otherwise.
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    if (isSecondary) {
                                        Modifier.border(width = 1.8.dp, color = AccentSkyBlue, shape = CircleShape)
                                    } else {
                                        Modifier.border(width = 1.8.dp, brush = AccentGradient, shape = CircleShape)
                                    }
                                } else {
                                    Modifier.border(
                                        width = 1.8.dp,
                                        color = LocalSheetSecondaryTextColor.current.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSecondary) {
                                            Modifier.background(AccentSkyBlue)
                                        } else {
                                            Modifier.background(AccentGradient)
                                        }
                                    )
                            )
                        }
                    }

                    val fullText = if (!indexNumber.isNullOrBlank()) "$indexNumber $primaryTitle" else primaryTitle
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = fullText,
                            color = if (isSelected) Neutral.D0F172A else NormalItemColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (metadataTags.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(top = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                metadataTags.forEach { tag ->
                                    val isSelection = tag == "Primary" || tag == "Secondary"
                                    Text(
                                        text = tag,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isSelection) selectedColor.copy(alpha = 0.16f)
                                                else LocalSheetSecondaryTextColor.current.copy(alpha = 0.09f)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp),
                                        color = if (isSelection) selectedColor
                                        else if (isSelected) Neutral.D0F172A else SecondaryTextColor,
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = if (isSelection || tag == "Default" || tag == "Forced") FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        // Completed AI translation: translate icon + [Language] tag under the title.
                        if (translationStatus is TrackTranslationStatus.Completed) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                StyledIcon(
                                    drawableRes = R.drawable.viaura_aitranslate,
                                    contentDescription = null,
                                    tint = AccentSkyBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "[${translationStatus.languageLabel}]",
                                    color = AccentSkyBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!selectionTag.isNullOrBlank()) {
                        val isSecTag = isSecondary || selectionTag == "S"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSecTag) AccentSkyBlue.copy(alpha = 0.15f) else AccentSkyBlueStrong.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = selectionTag,
                                color = if (isSecTag) AccentSkyBlue else Color(0xFF0369A1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!badgeTag.isNullOrBlank()) {
                        Text(
                            text = badgeTag,
                            color = if (isSelected) Color(0xFF0369A1) else SecondaryTextColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Thin progress line while AI translation is running on this track. It is drawn as an overlay hugging the very
        // bottom edge of the row (the row keeps exactly the same height as a normal / selected row), with the percentage
        // sitting right next to the line's end.
        if (translationStatus is TrackTranslationStatus.Translating) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 30.dp)
                    .offset(y = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { translationStatus.progressPercent / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = AccentSkyBlue,
                    trackColor = AccentSkyBlue.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (translationStatus.etaSeconds != null) {
                        "${translationStatus.progressPercent}% • ~${translationStatus.etaSeconds}s"
                    } else {
                        "${translationStatus.progressPercent}%"
                    },
                    fontSize = 9.5.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentSkyBlue
                )
            }
        }
    }
}

@Composable
internal fun SelectableChapterItem(
    indexNumber: String,
    title: String,
    timestamp: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) ActiveItemBg else Color.Transparent)
            .cardBounceClick(scaleDown = 0.98f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$indexNumber $title",
                color = if (isCurrent) Neutral.D0F172A else NormalItemColor,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = timestamp,
                color = if (isCurrent) AccentSkyBlueStrong else NormalItemColor.copy(alpha = 0.9f),
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.5.sp
            )
        }
    }
}

@Composable
private fun MenuActionItem(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    title: String,
    subtitle: String,
    badgeValue: String?,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) ActiveItemBg else Color.Transparent)
            .cardBounceClick(scaleDown = 0.98f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(iconRes),
                        contentDescription = null,
                        tint = if (isActive) ActiveHighlightColor else Neutral.S475569,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (icon != null) {
                    StyledIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) ActiveHighlightColor else Neutral.S475569,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = title,
                    color = if (isActive) ActiveHighlightColor else NormalItemColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!badgeValue.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = badgeValue,
                    color = if (isActive) ActiveHighlightColor else SecondaryTextColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun GlassPresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowGlass = LocalSheetIsLowGlassOpacity.current
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 56.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    AccentGradient
                } else if (isLowGlass) {
                    SolidColor(Color(0x22FFFFFF))
                } else {
                    SolidColor(Color(0x18FFFFFF))
                }
            )
            .border(
                1.dp,
                if (isSelected) Color(0x40FFFFFF) else if (isLowGlass) Color(0x30FFFFFF) else Color(0x25FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .cardBounceClick(scaleDown = 0.90f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else NormalItemColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun GlassStepperSliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 0.05f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Decrement Button (-)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SheetButtonBgColor)
                .border(1.dp, SheetButtonBorderColor, CircleShape)
                .cardBounceClick(scaleDown = 0.88f) {
                    val next = ((value - step) * 100).roundToInt() / 100f
                    onValueChange(next.coerceIn(valueRange.start, valueRange.endInclusive))
                },
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = "Decrease",
                tint = SheetButtonTextColor,
                modifier = Modifier.size(20.dp)
            )
        }

        // Slider Track with Thumb indicator
        ViauraSlider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { v ->
                val rounded = ((v * 20).roundToInt() / 20f).coerceIn(valueRange.start, valueRange.endInclusive)
                onValueChange(rounded)
            },
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AccentPink,
                activeTrackColor = AccentSkyBlue,
                inactiveTrackColor = if (LocalSheetIsLowGlassOpacity.current) Color(0x33FFFFFF) else AccentSkyBlue.copy(alpha = 0.20f)
            )
        )

        // Increment Button (+)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SheetButtonBgColor)
                .border(1.dp, SheetButtonBorderColor, CircleShape)
                .cardBounceClick(scaleDown = 0.88f) {
                    val next = ((value + step) * 100).roundToInt() / 100f
                    onValueChange(next.coerceIn(valueRange.start, valueRange.endInclusive))
                },
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Increase",
                tint = SheetButtonTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =========================================================================
// METADATA FORMATTERS & SANITIZERS
// =========================================================================

fun formatLanguageName(langCode: String): String {
    if (langCode.isBlank()) return ""
    val clean = langCode.trim().lowercase()
    return when (clean) {
        "en", "eng", "english" -> "English"
        "ja", "jpn", "jp", "japanese" -> "Japanese"
        "hi", "hin", "hindi" -> "Hindi"
        "es", "spa", "spanish" -> "Spanish"
        "fr", "fra", "fre", "french" -> "French"
        "de", "deu", "ger", "german" -> "German"
        "it", "ita", "italian" -> "Italian"
        "pt", "por", "portuguese" -> "Portuguese"
        "ru", "rus", "russian" -> "Russian"
        "zh", "zho", "chi", "chinese" -> "Chinese"
        "ko", "kor", "korean" -> "Korean"
        "ar", "ara", "arabic" -> "Arabic"
        "id", "ind", "indonesian" -> "Indonesian"
        "vi", "vie", "vietnamese" -> "Vietnamese"
        "th", "tha", "thai" -> "Thai"
        "tr", "tur", "turkish" -> "Turkish"
        "bn", "ben", "bengali" -> "Bengali"
        "ta", "tam", "tamil" -> "Tamil"
        "te", "tel", "telugu" -> "Telugu"
        "mr", "mar", "marathi" -> "Marathi"
        "ur", "urd", "urdu" -> "Urdu"
        else -> {
            try {
                val loc = Locale.forLanguageTag(clean)
                val display = loc.displayLanguage
                if (!display.isNullOrBlank() && !display.equals(clean, ignoreCase = true)) {
                    display
                } else {
                    Locale(clean).displayLanguage.ifBlank { clean.uppercase() }
                }
            } catch (_: Throwable) {
                clean.uppercase()
            }
        }
    }
}

/**
 * Resolves the real original format/codec badge for a track (audio or subtitle).
 * Never invents a value: if the actual codec cannot be determined, returns null so the
 * badge is simply hidden instead of showing a misleading default (e.g. always "AAC"/"SUB").
 * 1. Prefers the codec reported directly by the decoder/demuxer (track.codec).
 * 2. Falls back to the file extension of the original filename for external/uploaded tracks.
 */
fun resolveTrackFormatBadge(track: PlayerMediaTrack): String? {
    // External files can be transcoded for MPV playback (e.g. SRT -> temporary ASS). The
    // decoder codec then describes the playback representation, not the user's original file.
    // Prefer the original filename extension for uploaded/external subtitles.
    val originalName = track.originalFilename.trim()
    if (track.isExternal && originalName.isNotBlank()) {
        val originalExt = originalName.substringAfterLast('.', "").trim()
        if (originalExt.isNotBlank() && originalExt.length in 2..6 && originalExt.all { it.isLetterOrDigit() }) {
            return normalizeCodecLabel(originalExt)
        }
    }

    val rawCodec = track.codec.trim()
    if (rawCodec.isNotBlank()) return normalizeCodecLabel(rawCodec)

    val nameToCheck = track.originalFilename.ifBlank { track.title }
    val ext = nameToCheck.substringAfterLast('.', "").trim()
    if (ext.isNotBlank() && ext.length in 2..6 && ext.all { it.isLetterOrDigit() }) {
        return normalizeCodecLabel(ext)
    }
    return null
}

private fun normalizeCodecLabel(rawCodec: String): String {
    val c = rawCodec.trim().lowercase()
    return when (c) {
        "subrip", "srt" -> "SRT"
        "ass" -> "ASS"
        "ssa" -> "SSA"
        "webvtt", "vtt" -> "VTT"
        "dvd_subtitle", "dvdsub" -> "SUB"
        "dvb_subtitle" -> "DVB"
        "hdmv_pgs_subtitle", "pgs" -> "PGS"
        "mov_text" -> "MOV_TEXT"
        "aac", "aac_latm" -> "AAC"
        "ac3", "ac-3", "a52" -> "AC3"
        "eac3", "e-ac-3", "ec3" -> "EAC3"
        "dts", "dca" -> "DTS"
        "dts-hd", "dtshd" -> "DTS-HD"
        "truehd" -> "TRUEHD"
        "mp3", "mp3float", "mpeg" -> "MP3"
        "vorbis" -> "VORBIS"
        "opus" -> "OPUS"
        "flac" -> "FLAC"
        "wav", "wave" -> "WAV"
        "m4a" -> "M4A"
        "ogg" -> "OGG"
        "pcm_s16le", "pcm_s24le", "pcm_s32le", "pcm_u8", "pcm" -> "PCM"
        else -> c.uppercase()
    }
}

/**
 * Sanitizes track titles: Strips "#", dummy internal codes like "#1: A90Sec"
 */
fun cleanTrackTitle(rawTitle: String, fallback: String): String {
    if (rawTitle.isBlank()) return fallback

    var cleaned = rawTitle.trim()

    // Strip leading hash symbols like "#1:", "#2 -", "#"
    cleaned = cleaned.replace(Regex("^#\\s*\\d*\\s*[:\\-.]?\\s*"), "")

    // If it is just an internal debug string like A90Sec, B10Sec, Track1, etc.
    if (cleaned.matches(Regex("(?i)^[A-Z]?\\d+(sec|s)?$"))) {
        return fallback
    }

    if (cleaned.isBlank()) return fallback
    return cleaned
}

/**
 * Sanitizes chapter titles: Strips "#", dummy internal codes like "A90Sec"
 */
fun cleanChapterTitle(rawTitle: String, chapterNumber: Int): String {
    if (rawTitle.isBlank()) return "Chapter $chapterNumber"

    var cleaned = rawTitle.trim()

    // Strip leading hash symbols like "#1:", "#2 -", "#"
    cleaned = cleaned.replace(Regex("^#\\s*\\d*\\s*[:\\-.]?\\s*"), "")

    // Check if it matches raw internal token like "A90Sec", "A10Sec", "ch01"
    if (cleaned.matches(Regex("(?i)^[A-Z]?\\d+(sec|s)?$")) || cleaned.matches(Regex("(?i)^ch(apter)?_?\\d+$"))) {
        return "Chapter $chapterNumber"
    }

    if (cleaned.isBlank()) return "Chapter $chapterNumber"
    return cleaned
}

private fun formatPlaybackMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Renders high-fidelity interactive seekbar visual previews matching all 5 styles.
 */
@Composable
fun SeekbarPreviewTrack(
    style: SeekbarStyle,
    progressFraction: Float = 0.48f,
    activeColor: Color = AccentSkyBlueStrong,
    inactiveColor: Color = Color(0xFFBAE6FD),
    thumbColor: Color = AccentSkyBlueStrong,
    onDarkBackdrop: Boolean = false,
    modifier: Modifier = Modifier
) {
    val liquid = LocalLiquidGlassControls.current
    val gradientColors = com.example.ui.theme.AccentGradientColors
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val activeW = (w * progressFraction).coerceIn(0f, w)

        if (liquid) {
            // Liquid Glass Sliders ON: same glass seekbar the player / audio player draw for this style
            drawLiquidGlassSeekbar(style, progressFraction, onDarkBackdrop)
            return@Canvas
        }
        // Wavy / Thick / Slim: no thumb, theme gradient line that is slightly see-through
        val gradientBrush = Brush.horizontalGradient(colors = gradientColors, startX = 0f, endX = w.coerceAtLeast(1f))

        when (style) {
            SeekbarStyle.NORMAL -> {
                val trackThickness = 3.dp.toPx()
                // Inactive track line
                drawLine(
                    color = inactiveColor,
                    start = Offset(0f, centerY),
                    end = Offset(w, centerY),
                    strokeWidth = trackThickness
                )
                // Active track line
                if (activeW > 0) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, centerY),
                        end = Offset(activeW, centerY),
                        strokeWidth = trackThickness
                    )
                }
                // Thumb circle
                val thumbRadius = 6.5.dp.toPx()
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(activeW.coerceIn(thumbRadius.coerceAtMost(w / 2f), (w - thumbRadius).coerceAtLeast(w / 2f)), centerY)
                )
            }
            SeekbarStyle.STANDARD -> {
                val barHeight = 6.dp.toPx()
                val corner = CornerRadius(barHeight / 2f, barHeight / 2f)
                val topY = centerY - barHeight / 2f

                // Inactive rounded track
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, topY),
                    size = Size(w, barHeight),
                    cornerRadius = corner
                )
                // Active rounded track
                if (activeW > 0) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, topY),
                        size = Size(activeW, barHeight),
                        cornerRadius = corner
                    )
                }
                // Vertical dividing pin
                val pinW = 3.dp.toPx()
                val pinH = 15.dp.toPx()
                val pinX = (activeW - pinW / 2f).coerceIn(0f, (w - pinW).coerceAtLeast(0f))
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(pinX, centerY - pinH / 2f),
                    size = Size(pinW, pinH),
                    cornerRadius = CornerRadius(pinW / 2f, pinW / 2f)
                )
            }
            SeekbarStyle.WAVY -> {
                // Inactive straight line
                val inactiveHeight = 4.dp.toPx()
                val topY = centerY - inactiveHeight / 2f
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(activeW, topY),
                    size = Size((w - activeW).coerceAtLeast(0f), inactiveHeight),
                    cornerRadius = CornerRadius(inactiveHeight / 2f, inactiveHeight / 2f)
                )

                // Active wavy sine line with dental teeth oscillating back and forth with progress
                if (activeW > 0) {
                    val wavePath = Path()
                    val waveLength = 26.dp.toPx()
                    val waveAmplitude = 4.dp.toPx()
                    // Dental wave teeth move back and forth directly in sync with progress
                    val wavePhaseShift = progressFraction * (4f * Math.PI.toFloat())
                    wavePath.moveTo(0f, centerY)
                    var x = 0f
                    val step = 1.5.dp.toPx()
                    while (x <= activeW) {
                        val angle = (x / waveLength) * (2f * Math.PI.toFloat()) - wavePhaseShift
                        val y = centerY + sin(angle) * waveAmplitude
                        wavePath.lineTo(x, y)
                        x += step
                    }
                    wavePath.lineTo(activeW, centerY)
                    drawPath(
                        path = wavePath,
                        brush = gradientBrush,
                        alpha = 0.72f,
                        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            SeekbarStyle.THICK -> {
                val barHeight = 13.dp.toPx()
                val corner = CornerRadius(barHeight / 2f, barHeight / 2f)
                val topY = centerY - barHeight / 2f

                // Inactive thick pill
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, topY),
                    size = Size(w, barHeight),
                    cornerRadius = corner
                )
                // Active thick pill
                if (activeW > 0) {
                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(0f, topY),
                        size = Size(activeW, barHeight),
                        cornerRadius = corner,
                        alpha = 0.72f
                    )
                }
            }
            SeekbarStyle.SLIM -> {
                val barHeight = 7.dp.toPx()
                val corner = CornerRadius(barHeight / 2f, barHeight / 2f)
                val topY = centerY - barHeight / 2f

                // Inactive rounded pill
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, topY),
                    size = Size(w, barHeight),
                    cornerRadius = corner
                )
                // Active rounded pill
                if (activeW > 0) {
                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(0f, topY),
                        size = Size(activeW, barHeight),
                        cornerRadius = corner,
                        alpha = 0.72f
                    )
                }
            }
        }
    }
}

/**
 * Reusable Card containing all 5 Seekbar Style selections with previews and radio buttons.
 */
@Composable
fun SeekbarStyleSelectionCard(
    currentStyle: SeekbarStyle,
    onSelectStyle: (SeekbarStyle) -> Unit,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Neutral.D1E293B.copy(alpha = 0.85f) else sheetWhite(Neutral.F1F5F9)
    val cardBorder = if (isDark) AccentSkyBlue.copy(alpha = 0.20f) else Neutral.E2E8F0
    val dividerColor = if (isDark) Color(0x26FFFFFF) else Neutral.E2E8F0
    val textPrimary = if (isDark) Neutral.F8FAFC else Neutral.D0F172A
    val textSecondary = if (isDark) Neutral.S94A3B8 else Neutral.S64748B

    // Continuous smooth back-and-forth seeking animation simulating active playback
    val infiniteTransition = rememberInfiniteTransition(label = "SeekbarPreviewProgressTransition")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SeekbarPreviewProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SeekbarStyle.entries.forEachIndexed { index, style ->
                val isSelected = currentStyle == style
                val isFirst = index == 0
                val isLast = index == SeekbarStyle.entries.size - 1

                val itemCornerShape = when {
                    isFirst && isLast -> RoundedCornerShape(16.dp)
                    isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemCornerShape)
                        .clickable { onSelectStyle(style) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Style Label & Visual Seekbar Preview
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = style.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) ActiveHighlightColor else textPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Visual Preview Track animating back and forth
                        SeekbarPreviewTrack(
                            style = style,
                            progressFraction = animatedProgress,
                            activeColor = if (isSelected) ActiveHighlightColor else AccentSkyBlueStrong,
                            inactiveColor = if (isDark) Neutral.D334155 else Color(0xFFBAE6FD),
                            thumbColor = if (isSelected) ActiveHighlightColor else AccentSkyBlueStrong,
                            onDarkBackdrop = isDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                        )
                    }

                    // Right: Custom Radio Button Indicator matching user's design
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 2.2.dp else 1.8.dp,
                                color = if (isSelected) ActiveHighlightColor else textSecondary.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(ActiveHighlightColor)
                            )
                        }
                    }
                }

                if (index < SeekbarStyle.entries.size - 1) {
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Player Layout Selection Modal Sheet in the Video Player.
 * Matches user's exact design specifications:
 * - Header: Back arrow + "Player Layout"
 * - Sub-tabs: "Control Layout" and "Seekbar Style"
 * - Reorderable, configurable Control Layout editor
 * - 5 styled seekbar selections with crisp previews and radio buttons
 */
@Composable
fun PlayerLayoutSelectionPanel(
    currentSeekbarStyle: SeekbarStyle,
    playerLayoutConfig: PlayerLayoutConfig = PlayerLayoutConfig.default,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onSelectSeekbarStyle: (SeekbarStyle) -> Unit,
    onConfigChange: (PlayerLayoutConfig) -> Unit = {},
    onResetToDefault: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0: Control Layout, 1: Seekbar Style
    val isDark = glassBlurTransparency < 25f

    PlayerGlassModalSheet(
        panelKey = "player_layout",
        isLandscape = isLandscape,
        heightFraction = if (isLandscape) 0.88f else 0.78f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerCustomRow = {
            // Header Row: Left Back Arrow + Title "Player Layout"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    StyledIcon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to Player",
                        tint = NormalItemColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Player Layout",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NormalItemColor
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Sub-tabs: "Control Layout" & "Seekbar Style"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tab 0: Control Layout
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSubTab = 0 }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Control Layout",
                        fontSize = 14.sp,
                        fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSubTab == 0) ActiveHighlightColor else NormalItemColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (selectedSubTab == 0) ActiveHighlightColor else Color.Transparent)
                    )
                }

                // Tab 1: Seekbar Style
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSubTab = 1 }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seekbar Style",
                        fontSize = 14.sp,
                        fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSubTab == 1) ActiveHighlightColor else NormalItemColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (selectedSubTab == 1) ActiveHighlightColor else Color.Transparent)
                    )
                }
            }

            if (selectedSubTab == 0) {
                // Tab 0: Control Layout Editor
                ControlLayoutEditor(
                    config = playerLayoutConfig,
                    onConfigChange = onConfigChange,
                    onResetToDefault = onResetToDefault,
                    isDark = isDark,
                    transparencyPercent = glassBlurTransparency
                )
            } else {
                // Tab 1: 5 Seekbar Style Cards
                SeekbarStyleSelectionCard(
                    currentStyle = currentSeekbarStyle,
                    onSelectStyle = { style ->
                        onSelectSeekbarStyle(style)
                    },
                    isDark = isDark
                )
            }
        }
    }
}

/** One quality row of a provider stream: "Auto", or an HLS rendition (title "1080p", info "4.1 Mbps · 1920x1080 · avc1.640028"). */
data class PlayerQualityOption(
    val key: String,
    val title: String,
    val info: String,
    /** HD / SD / 2K / 4K tile shown in front of the row; null for "Auto". */
    val badge: String?
)

/** One server (stream source) a provider returned for the movie / episode. */
data class PlayerServerOption(val key: String, val title: String, val info: String)

/** Everything the "Server | Quality" panel of a provider video shows. */
data class PluginStreamPanelData(
    val servers: List<PlayerServerOption>,
    val selectedServerKey: String?,
    val qualities: List<PlayerQualityOption>,
    val selectedQualityKey: String?
)

@Composable
private fun StreamChoiceRow(
    iconRes: Int?,
    icon: ImageVector?,
    badge: String?,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) ActiveItemBg else Color.Transparent)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) ActiveHighlightColor else SecondaryTextColor.copy(alpha = 0.28f), shape)
            .cardBounceClick(scaleDown = 0.98f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Neutral.S64748B),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else if (iconRes != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(iconRes),
                    contentDescription = null,
                    tint = if (selected) ActiveHighlightColor else Neutral.S475569,
                    modifier = Modifier.size(26.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) ActiveHighlightColor else Neutral.S475569,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (selected) ActiveHighlightColor else NormalItemColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = SecondaryTextColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(ActiveHighlightColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Three-dot menu > Video settings of a provider (media plugin) video: "Server" (the sources the website gave, the player
 * switches to the next one by itself when one cannot be played) next to "Quality" (Auto = adaptive bitrate, or one fixed
 * HLS rendition with its bitrate, resolution and codec).
 */
@Composable
fun PluginStreamPanel(
    data: PluginStreamPanelData,
    onSelectServer: (String) -> Unit,
    onSelectQuality: (String) -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    PlayerGlassModalSheet(
        panelKey = "plugin_stream_panel",
        isLandscape = isLandscape,
        heightFraction = if (isLandscape) 0.80f else 0.70f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Video Settings",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        }
    ) {
        val serverList: @Composable () -> Unit = {
            data.servers.forEach { sv ->
                StreamChoiceRow(
                    iconRes = null,
                    icon = Icons.Outlined.Dns,
                    badge = null,
                    title = sv.title,
                    subtitle = sv.info,
                    selected = sv.key == data.selectedServerKey,
                    onClick = { onSelectServer(sv.key); onDismissRequest() }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        val qualityList: @Composable () -> Unit = {
            data.qualities.forEach { q ->
                StreamChoiceRow(
                    iconRes = if (q.badge == null) com.example.R.drawable.viaura_video_setting else null,
                    icon = null,
                    badge = q.badge,
                    title = q.title,
                    subtitle = q.info,
                    selected = q.key == data.selectedQualityKey,
                    onClick = { onSelectQuality(q.key); onDismissRequest() }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        @Composable
        fun ColumnTitle(text: String) {
            Text(
                text = text,
                color = NormalItemColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (data.servers.isNotEmpty()) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        ColumnTitle("Server")
                        serverList()
                    }
                }
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    ColumnTitle("Quality")
                    qualityList()
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (data.servers.isNotEmpty()) {
                    ColumnTitle("Server")
                    serverList()
                }
                ColumnTitle("Quality")
                qualityList()
            }
        }
    }
}

@Composable
fun VideoQualitySelectionPanel(
    qualities: List<String>,
    selectedQuality: String?,
    onSelectQuality: (String) -> Unit,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    onDismissRequest: () -> Unit
) {
    PlayerGlassModalSheet(
        panelKey = "video_quality",
        isLandscape = isLandscape,
        heightFraction = 0.55f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Text(
                text = "Video Quality",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = NormalItemColor
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(qualities) { quality ->
                val isAuto = quality == "Auto"
                val isSelected = quality == selectedQuality || (isAuto && selectedQuality?.startsWith("Auto") == true)
                MenuActionItem(
                    icon = Icons.Outlined.HighQuality,
                    title = quality,
                    subtitle = if (isAuto) {
                        if (isSelected) "Follows your internet speed · now ${selectedQuality?.substringAfter("(", "")?.substringBefore(")").orEmpty()}"
                        else "Follows your internet speed"
                    } else if (isSelected) "Active stream quality" else "",
                    badgeValue = if (isSelected) "✓" else null,
                    onClick = {
                        onSelectQuality(quality)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}
