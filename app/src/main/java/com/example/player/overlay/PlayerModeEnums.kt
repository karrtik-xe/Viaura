package com.example.ui.screens

import com.example.ui.theme.AccentSkyBlueStrong
import com.example.R
import com.example.ai.TrackTranslationStatus
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ripple
import com.example.ui.components.SeekIcon
import com.example.ui.components.StyledIcon
import com.example.ui.components.LocalLiquidGlass
import com.example.ui.components.LocalLiquidGlassControls
import com.example.ui.components.LiquidGlassVerticalBar
import com.example.ui.components.drawLiquidGlassSeekbarTrack
import com.example.ui.components.drawLiquidGlassSeekbarThumb
import com.example.ui.components.liquidSeekTrackHeightDp
import com.example.ui.components.glassOuterShadow
import com.example.ui.components.liquidGlassBorder
import com.example.ui.components.videoGlassBackdrop
import com.example.ui.components.ViauraSurfaceStyle
import com.example.ui.components.liquidGlassBorderBrush
import com.example.ui.components.liquidGlassPlayerFill
import com.example.ui.components.liquidGlassSheenBrush
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.BatteryManager
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.ui.components.AutoScrollingText
import com.example.ui.components.bounceClick
import com.example.ui.components.cardBounceClick
import com.example.ui.components.cardBounceCombinedClick
import com.example.ui.components.loadVideoThumbnail
import com.example.ui.state.ControlsAnimationStyle
import com.example.ui.components.SubtitleSelectionPanel
import com.example.ui.components.AudioSelectionPanel
import com.example.ui.components.AudioDelayPanel
import com.example.ui.components.AudioEqualizerPanel
import com.example.ui.components.SubtitleDelayPanel
import com.example.ui.components.SubtitleStylePanel
import com.example.ui.components.ChapterSelectionPanel
import com.example.ui.components.MoreMenuSelectionPanel
import com.example.ui.components.SleepTimerPanel
import com.example.ui.components.SpeedSelectionPanel
import com.example.player.TrackAudioConfig
import com.example.player.AudioChannelMode
import com.example.player.VideoFilterPreset
import com.example.player.ManualVideoAdjustments
import com.example.player.ChapterSkipMarker
import com.example.player.ChapterSkipDetector
import com.example.ui.components.PlayerChapterSkipButton
import com.example.ui.components.VideoZoomSelectionPanel
import com.example.ui.components.VideoEqSelectionPanel
import com.example.ui.components.VideoSettingSelectionPanel
import com.example.ui.components.ManualVideoAdjustmentPanel
import com.example.ui.components.AbLoopSelectionPanel
import com.example.ui.components.PlaylistSelectionPanel
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.PlayerControlId
import com.example.ui.state.PlayerLayoutConfig
import com.example.ui.state.PlayerSettings
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.theme.LocalAppFontFamily
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.drawWithContent
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import com.example.ui.components.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.util.AppHaptics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ==========================================
// ENUMS & DATA MODELS
// ==========================================

enum class PlayerPanelType {
    SUBTITLE_TRACK,
    SUBTITLE_DELAY,
    SUBTITLE_STYLE,
    AUDIO_TRACK,
    AUDIO_DELAY,
    SLEEP_TIMER,
    EQUALIZER,
    CHAPTERS,
    MORE_MENU,
    SPEED,
    VIDEO_ZOOM,
    VIDEO_SETTING_FILTER,
    MANUAL_VIDEO_ADJUSTMENT,
    AB_LOOP,
    PLAYLIST,
    PLAYER_LAYOUT,
    VIDEO_QUALITY
}

enum class DecoderMode(val label: String) {
    /** Smart Auto Playback: picks the decoder per video (see SmartPlaybackAdvisor). The others are the manual modes. */
    AUTO("AUTO"),
    HW_PLUS("HW+"),
    HW("HW"),
    SW("SW")
}

enum class PlayerRepeatMode {
    OFF, ONE, ALL
}

enum class AspectRatioMode(
    val modeKey: String,
    val label: String,
    val accessibilityLabel: String,
    val iconRes: Int
) {
    FIT("fit", "Fit", "Video display mode: Fit", R.drawable.viaura_minimize_square_2),
    FILL("fill", "Fill", "Video display mode: Fill", R.drawable.viaura_maximize_square_2),
    CROP("crop", "Zoom In", "Video display mode: Zoom In", R.drawable.viaura_maximize_square),
    ORIGINAL("original", "Full Screen", "Video display mode: Full Screen", R.drawable.viaura_full_screen_square);

    fun next(): AspectRatioMode = when (this) {
        FIT -> FILL
        FILL -> CROP
        CROP -> ORIGINAL
        ORIGINAL -> FIT
    }
}
