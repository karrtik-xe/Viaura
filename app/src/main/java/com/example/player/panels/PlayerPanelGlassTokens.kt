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
 * Global persistent store for panel custom heights.
 * Remembers resized height per specific panel (Chapters, Subtitles, Audio, etc.)
 * so user changes persist and never reset unless changed by the user.
 */
object PlayerPanelHeightStore {
    private const val PREFS_NAME = "player_panel_heights_pref"
    private val memoryHeights = mutableMapOf<String, Float>()

    fun getHeight(context: Context, panelTitle: String, defaultHeight: Float): Float {
        val key = panelTitle.trim().lowercase(Locale.ROOT)
        memoryHeights[key]?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getFloat("height_$key", defaultHeight)
        memoryHeights[key] = saved
        return saved
    }

    fun saveHeight(context: Context, panelTitle: String, height: Float) {
        val key = panelTitle.trim().lowercase(Locale.ROOT)
        memoryHeights[key] = height
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("height_$key", height).apply()
    }
}

/**
 * Clean Frosted Glass Palette
 */
internal val ScrimColor = Color.Transparent

internal val ActiveHighlightColor: Color
    get() = AccentSkyBlue

internal val ActiveItemBg: Color
    get() = AccentContainerLight.copy(alpha = 0.90f)

val LocalSheetNormalTextColor = compositionLocalOf { Neutral.D1E293B }

val LocalSheetSecondaryTextColor = compositionLocalOf { Neutral.S64748B }

val LocalSheetIsLowGlassOpacity = compositionLocalOf { false }

/**
 * Opacity of every white / light surface (cards, chips, the selected item) inside a glass sheet.
 * It follows the Settings "glass transparency" slider, exactly like the sheet background does.
 */
val LocalSheetWhiteAlpha = compositionLocalOf { 1f }

/** Applies the slider-controlled opacity to a light surface colour. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun sheetWhite(base: Color): Color {
    // Liquid Glass: light chips / cards become see-through glass instead of opaque white
    if (LocalLiquidGlass.current) {
        val dark = com.example.ui.theme.LocalIsDarkTheme.current
        return if (dark && base.luminance() > 0.8f) Color.White.copy(alpha = 0.10f)
        else base.copy(alpha = base.alpha * (if (dark) 0.30f else 0.42f))
    }
    return base.copy(alpha = base.alpha * LocalSheetWhiteAlpha.current)
}

val NormalItemColor: Color
    @Composable get() = LocalSheetNormalTextColor.current

val SecondaryTextColor: Color
    @Composable get() = LocalSheetSecondaryTextColor.current

val SheetButtonBgColor: Color
    @Composable get() = if (LocalLiquidGlass.current) Color.White.copy(alpha = if (com.example.ui.theme.LocalIsDarkTheme.current) 0.10f else 0.40f)
        else if (LocalSheetIsLowGlassOpacity.current) Color(0x22FFFFFF) else sheetWhite(Neutral.F1F5F9)

val SheetButtonTextColor: Color
    @Composable get() = if (LocalLiquidGlass.current && com.example.ui.theme.LocalIsDarkTheme.current) Color(0xFFF8FAFC) else Neutral.D0F172A

/** Text on a light chip: dark on the light chip, light only when the chip itself is see-through glass. */
val SheetChipTextColor: Color
    @Composable get() = if (LocalLiquidGlass.current && com.example.ui.theme.LocalIsDarkTheme.current) Color(0xFFF8FAFC) else if (LocalSheetIsLowGlassOpacity.current) LocalSheetNormalTextColor.current else Neutral.D0F172A

val SheetButtonBorderColor: Color
    @Composable get() = if (LocalLiquidGlass.current) Color.White.copy(alpha = 0.35f) else if (LocalSheetIsLowGlassOpacity.current) Color(0x30FFFFFF) else Neutral.CBD5E1

/**
 * Ultra-stable, high-performance modal container for all player panels.
 * Height strictly constrained to at most 60% of the screen with touch isolation.
 * In portrait: reaches full width edge-to-edge with top rounded corners.
 * In landscape: constrained to strictly 60% of screen width.
 * Tapping outside the sheet card cancels and closes the panel.
 * Tapping inside the sheet card is completely consumed and never leaks to video controls underneath.
 */
/** Channel-style portrait player: the controls live in a small card, so every panel opens in its own full-screen dialog window. */
val LocalForcePanelDialog = androidx.compose.runtime.compositionLocalOf { false }
