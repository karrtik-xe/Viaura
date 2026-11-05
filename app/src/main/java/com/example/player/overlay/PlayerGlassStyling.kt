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

// Visual color palette matching the screenshot:
// Dynamic glass transparency: 100% = Pure Solid White Image, Lower = Progressive Translucency
val LocalGlassBlurTransparency = compositionLocalOf { 85f }

val LocalPlayerSettings = compositionLocalOf { com.example.ui.state.PlayerSettings() }

@Composable
fun getEffectiveGlassBg(glassBg: Color? = null): Color {
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) return Color.Transparent
    if (glassBg != null) return glassBg
    val trans = LocalGlassBlurTransparency.current
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    // Liquid Glass: one central branch, every player control follows it (slider still controls the opacity)
    if (LocalLiquidGlass.current) {
        val amoled = ViauraSurfaceStyle.amoled
        val tint = com.example.ui.components.LocalLiquidGlassParams.current.tint
        return remember(trans, isDark, amoled, tint) { liquidGlassPlayerFill(isDark, trans, tint) }
    }
    return remember(trans, isDark, settings.alwaysDarkPlayerButtonBackground) {
        val factor = (trans / 100f).coerceIn(0.05f, 1.0f)
        val baseColor = if (isDark) Color(0xFF090D16) else Color(0xFFFFFFFF)
        if (trans < 25f) {
            baseColor.copy(alpha = (factor * 0.45f).coerceIn(0.06f, 0.20f))
        } else {
            if (factor >= 0.99f) baseColor else baseColor.copy(alpha = factor)
        }
    }
}

@Composable
fun getEffectiveGlassBorder(glassBorder: Color? = null): Color {
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) return Color.Transparent
    if (glassBorder != null) return glassBorder
    val trans = LocalGlassBlurTransparency.current
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    // Liquid Glass: soft white specular tone (the real gradient border is drawn by playerGlassSurface)
    if (LocalLiquidGlass.current) return if (isDark) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.80f)
    return remember(trans, isDark, settings.alwaysDarkPlayerButtonBackground) {
        val factor = (trans / 100f).coerceIn(0.05f, 1.0f)
        if (isDark) {
            if (trans < 25f) {
                Color.White.copy(alpha = 0.22f)
            } else {
                if (factor >= 0.98f) Color(0xFF334155) else Color.White.copy(alpha = (factor * 0.35f).coerceIn(0.08f, 0.35f))
            }
        } else {
            if (trans < 25f) {
                Color(0xFFFFFFFF).copy(alpha = 0.38f)
            } else {
                if (factor >= 0.98f) Color(0xFFCBD5E1) else Color(0xFFFFFFFF).copy(alpha = (factor * 0.70f).coerceIn(0.08f, 0.90f))
            }
        }
    }
}

@Composable
fun getEffectiveGlassIconTint(customTint: Color? = null): Color {
    if (customTint != null) return customTint
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) return Color.White
    val trans = LocalGlassBlurTransparency.current
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    // Liquid Glass: the fill is translucent white/dark, so the icon is simply light on dark glass and dark on light glass
    if (LocalLiquidGlass.current) return if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    return remember(trans, isDark, settings.alwaysDarkPlayerButtonBackground, settings.hidePlayerButtonsBackground) {
        if (isDark) {
            Color(0xFFF8FAFC)
        } else {
            if (trans < 25f) Color(0xFFFFFFFF) else Color(0xFF0F172A)
        }
    }
}

val GlassIconTint: Color
    @Composable get() = getEffectiveGlassIconTint()

/**
 * Surface of every video-player control (replaces `.clip(shape).background(bg).border(1.dp, border, shape)`).
 * Liquid Glass OFF (or "hide player button background" ON) = exactly the old look.
 * Liquid Glass ON = translucent fill + glossy sheen + thin specular gradient border + soft floating shadow.
 * The video behind is a native SurfaceView, so it shines through the translucent fill (it cannot be blurred).
 */
@Composable
fun Modifier.playerGlassSurface(
    bg: Color,
    border: Color,
    shape: androidx.compose.ui.graphics.Shape,
    highlightBorder: Boolean = false
): Modifier {
    val settings = LocalPlayerSettings.current
    if (!LocalLiquidGlass.current || settings.hidePlayerButtonsBackground) {
        return this.clip(shape).background(bg).border(1.dp, border, shape)
    }
    val isDark = if (settings.alwaysDarkPlayerButtonBackground) true else isAppInDarkTheme()
    val highlight = com.example.ui.components.liquidHighlightScale()
    return this
        // shadow only OUTSIDE the glass (an elevation shadow would darken the inside of the translucent fill)
        .glassOuterShadow(shape, 10.dp, Color.Black.copy(alpha = 0.30f), 2.dp)
        .clip(shape)
        // blurred + saturated + refracted live video behind the control (no-op without a video sample)
        .videoGlassBackdrop(shape)
        .background(bg)
        .background(liquidGlassSheenBrush(isDark), shape)
        .then(
            if (highlightBorder) Modifier.border(1.dp, border, shape)
            else Modifier.liquidGlassBorder(shape, isDark, 1.dp)
        )
}

/**
 * Surface of the transient player notifications (seek banner, action messages, translation banner):
 * exactly the same glass as the player buttons. With "hide player button background" ON the old dark pill
 * is kept so the text stays readable on the bare video.
 */
@Composable
fun Modifier.playerNotificationGlass(shape: androidx.compose.ui.graphics.Shape, fallbackBg: Color): Modifier {
    val settings = LocalPlayerSettings.current
    if (settings.hidePlayerButtonsBackground) {
        return this.clip(shape).background(fallbackBg).border(1.dp, Color.White.copy(alpha = 0.25f), shape)
    }
    return this.playerGlassSurface(getEffectiveGlassBg(), getEffectiveGlassBorder(), shape)
}
