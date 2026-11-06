package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.R

import com.example.ui.theme.*
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.key
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.background.reportBackdropTouches
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SwitchDefaults
import com.example.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AudioChannelMode
import com.example.player.AudioLanguageMatcher
import com.example.player.SubtitleFontManager
import com.example.ui.state.AppLanguage
import com.example.ui.state.AppThemeVariant
import com.example.ui.state.ControlsAnimationStyle
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.HwAccelMode
import com.example.ui.state.PlayerSettings
import com.example.ui.state.ScreenNavigationStyle
import com.example.ui.state.SeekbarStyle
import com.example.ui.state.TabNavigationStyle
import com.example.ui.state.ThemeMode
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import com.example.ui.state.UiState
import com.example.ui.state.VideoOpeningAnimation
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.screens.LocalGlassBlurTransparency
import kotlinx.coroutines.launch

import com.example.ui.state.AppStrings
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.AccentGradientColors

enum class SettingsCategoryTab(
    val icon: ImageVector,
    val viauraIconRes: Int
) {
    APPEARANCE(Icons.Outlined.Palette, R.drawable.viaura_pallete_2),
    LIQUID_GLASS(Icons.Outlined.BlurOn, R.drawable.viaura_liquid_glass),
    PLAYER(Icons.Outlined.PlayCircle, R.drawable.viaura_play_circle),
    AUDIO(Icons.Outlined.VolumeUp, R.drawable.viaura_audio_circle),
    SUBTITLES(Icons.Outlined.Subtitles, R.drawable.viaura_subtitles),
    DECODER(Icons.Outlined.Memory, R.drawable.viaura_cpu),
    GESTURES(Icons.Outlined.Tune, R.drawable.viaura_gesture),
    PLAYER_LAYOUT(Icons.Outlined.DashboardCustomize, R.drawable.viaura_widget_5),
    FOLDERS(Icons.Outlined.Folder, R.drawable.viaura_folder),
    NETWORK(Icons.Outlined.Language, R.drawable.viaura_network),
    ADVANCED(Icons.Outlined.Settings, R.drawable.viaura_settings_minimalistic),
    ABOUT(Icons.Outlined.Info, R.drawable.viaura_user);

    fun getTitle(lang: AppLanguage): String = when (this) {
        APPEARANCE -> when (lang) {
            AppLanguage.ENGLISH -> "Appearance"
            AppLanguage.HINGLISH -> "Appearance"
            AppLanguage.HINDI -> "दिखावट"
        }
        LIQUID_GLASS -> when (lang) {
            AppLanguage.ENGLISH -> "Liquid glass"
            AppLanguage.HINGLISH -> "Liquid glass"
            AppLanguage.HINDI -> "लिक्विड ग्लास"
        }
        FOLDERS -> when (lang) {
            AppLanguage.ENGLISH -> "Folders"
            AppLanguage.HINGLISH -> "Folders"
            AppLanguage.HINDI -> "फ़ोल्डर"
        }
        NETWORK -> when (lang) {
            AppLanguage.ENGLISH -> "Network"
            AppLanguage.HINGLISH -> "Network"
            AppLanguage.HINDI -> "नेटवर्क"
        }
        PLAYER -> AppStrings.getTabPlayer(lang)
        AUDIO -> AppStrings.getTabAudio(lang)
        DECODER -> AppStrings.getTabDecoder(lang)
        SUBTITLES -> AppStrings.getTabSubtitles(lang)
        GESTURES -> AppStrings.getTabGestures(lang)
        PLAYER_LAYOUT -> AppStrings.getTabPlayerLayout(lang)
        ADVANCED -> AppStrings.getTabAdvanced(lang)
        ABOUT -> AppStrings.getTabAbout(lang)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    uiState: UiState,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onScaleChange: (Float) -> Unit = {},
    onGlassBlurTransparencyChange: (Float) -> Unit = {},
    onSideBySideChange: (Boolean) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
    onFrameStepChange: (Int) -> Unit = {},
    onCopyTimestampChange: (Boolean) -> Unit = {},
    onHwAccelModeChange: (HwAccelMode) -> Unit = {},
    onGestureSensitivityModeChange: (GestureSensitivityMode) -> Unit = {},
    onShowPlayerNotificationsChange: (Boolean) -> Unit = {},
    onSeekbarStyleChange: (SeekbarStyle) -> Unit = {},
    onPlayerLayoutConfigChange: (com.example.ui.state.PlayerLayoutConfig) -> Unit = {},
    onResetPlayerLayoutConfig: () -> Unit = {},
    onPreferredAudioLanguagesChange: (String) -> Unit = {},
    onApplyAudioLanguageToSelectedContentOnlyChange: (Boolean) -> Unit = {},
    onSelectedAudioFoldersChange: (Set<String>) -> Unit = {},
    onSelectedAudioVideosChange: (Set<String>) -> Unit = {},
    onEnableAudioPitchCorrectionChange: (Boolean) -> Unit = {},
    onVolumeNormalizationChange: (Boolean) -> Unit = {},
    onBackgroundPlaybackChange: (Boolean) -> Unit = {},
    onAudioChannelModeChange: (AudioChannelMode) -> Unit = {},
    onVolumeBoostCapChange: (Int) -> Unit = {},
    onRememberSelectedAudioTrackChange: (Boolean) -> Unit = {},
    onPlayerSettingsChange: (PlayerSettings) -> Unit = {},
    onAppThemeChange: (AppThemeVariant) -> Unit = {},
    onCustomThemeGradientChange: (String, List<Long>?) -> Unit = { _, _ -> },
    onAmoledBlackModeChange: (Boolean) -> Unit = {},
    onLiquidGlassModeChange: (Boolean) -> Unit = {},
    onLiquidGlassParamsChange: (LiquidGlassParams) -> Unit = {},
    onLiquidGlassControlsChange: (Boolean) -> Unit = {},
    onUseSystemFontChange: (Boolean) -> Unit = {},
    onHapticFeedbackChange: (Boolean) -> Unit = {},
    onShowFullNameChange: (Boolean) -> Unit = {},
    onShowNewVideoLabelChange: (Boolean) -> Unit = {},
    onNewVideoDaysThresholdChange: (Int) -> Unit = {},
    onShowFolderUnplayedBadgeChange: (Boolean) -> Unit = {},
    onAutoScrollToLastPlayedChange: (Boolean) -> Unit = {},
    onTreePathCompressionChange: (Boolean) -> Unit = {},
    onDualPaneViewChange: (Boolean) -> Unit = {},
    onWatchedThresholdPercentChange: (Int) -> Unit = {},
    onShowVideoThumbnailsChange: (Boolean) -> Unit = {},
    onThumbnailStrategyChange: (ThumbnailStrategy) -> Unit = {},
    onThumbnailQualityChange: (ThumbnailQuality) -> Unit = {},
    onTapThumbnailToSelectChange: (Boolean) -> Unit = {},
    onShowNetworkThumbnailsChange: (Boolean) -> Unit = {},
    onShowHomeTabChange: (Boolean) -> Unit = {},
    onShowMusicTabChange: (Boolean) -> Unit = {},
    onShowRecentsTabChange: (Boolean) -> Unit = {},
    onShowPlaylistsTabChange: (Boolean) -> Unit = {},
    onShowNetworkTabChange: (Boolean) -> Unit = {},
    onShowDownloadsTabChange: (Boolean) -> Unit = {},
    onControlsAnimationStyleChange: (ControlsAnimationStyle) -> Unit = {},
    onVideoOpeningAnimationChange: (VideoOpeningAnimation) -> Unit = {},
    onScreenNavigationStyleChange: (ScreenNavigationStyle) -> Unit = {},
    onTabNavigationStyleChange: (TabNavigationStyle) -> Unit = {},
    onAnimationSpeedChange: (Float) -> Unit = {},
    viewModel: com.example.ui.state.AppViewModel? = null
) {
    val configuration = LocalConfiguration.current
    val currentDensity = LocalDensity.current
    val screenHeightPx = with(currentDensity) { (configuration.screenHeightDp + 100).dp.toPx() }

    val coroutineScope = rememberCoroutineScope()
    val sheetDragOffsetY = remember { Animatable(screenHeightPx) }
    val scrimAlpha = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    // The neighbouring settings pages are composed only AFTER the open animation, so the first frames stay smooth.
    var preloadNeighbourPages by remember { mutableStateOf(false) }
    // The heavy dialog blur (Android 12+) is switched on only AFTER the slide-in, so it can't make the opening animation stutter.
    var windowBlurReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Let the first (heavy) composition + layout finish BEFORE the animation clock starts, otherwise the
        // slide-in jumps / drops frames on the very first frames.
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        launch {
            scrimAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            )
        }
        sheetDragOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
        windowBlurReady = true
        delay(120)
        preloadNeighbourPages = true
    }

    val dismissWithAnimation: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            coroutineScope.launch {
                try {
                    launch {
                        scrimAlpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        )
                    }
                    sheetDragOffsetY.animateTo(
                        targetValue = screenHeightPx,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                } catch (_: Throwable) {
                } finally {
                    onDismiss()
                }
            }
        }
    }

    BackHandler {
        dismissWithAnimation()
    }

    val context = LocalContext.current
    val isDark = uiState.themeMode == ThemeMode.DARK || (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    val lang = uiState.language
    val alphaFactor = (uiState.glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)

    // Consume all residual vertical scrolling & fling from content so it NEVER bubbles up to dismiss the sheet
    val consumeContentScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
            override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }

    // The dashboard follows the Application Scale / Zoom exactly like the app behind it.
    // LocalDensity here is ALREADY the app's scaled density (VideoPlayerTheme + the screen apply the appScale
    // factor) and this Dialog inherits it. The factor used to be multiplied in AGAIN on top, so the dashboard
    // did not match the app's size. Now it uses the inherited density as-is: same size, live with the slider.
    // The dashboard looked too big next to the app at the same percentage: it now renders the way the 46% look did at 75%
    // (the slider still zooms it live - same offset at every value).
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, uiState.appScale) {
        fun factorFor(scale: Float) = (0.85f + 0.15f * ((scale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
        val ratio = factorFor(uiState.appScale - 29f) / factorFor(uiState.appScale)
        Density(
            density = currentDensity.density * ratio,
            fontScale = currentDensity.fontScale * ratio
        )
    }

    // Dashboard navigation: null = the sectioned list, otherwise the opened settings page
    // Bottom-sheet settings: a tab row at the top, the selected tab's page below it
    var openCategory by remember { mutableStateOf(SettingsCategoryTab.APPEARANCE) }
    // Appearance tab has two sections (like Network: Local Network / Media): 0 = Appearance, 1 = Liquid Glass
    var appearanceSub by remember { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    val sectionHapticView = LocalView.current
    // slide direction of the tab-content animation (set by tab taps and by the swipe, which wraps around)
    var tabSlideForward by remember { mutableStateOf(true) }
    val openSettingsPage: (SettingsCategoryTab) -> Unit = { tab ->
        if (tab != openCategory) {
            com.example.util.AppHaptics.performClick(sectionHapticView)
            tabSlideForward = tab.ordinal > openCategory.ordinal
        }
        openCategory = tab
    }
    // A search result: Liquid Glass settings live inside the Appearance tab (second section)
    val openSearchResult: (SettingsCategoryTab) -> Unit = { tab ->
        searchOpen = false
        if (tab == SettingsCategoryTab.LIQUID_GLASS) {
            appearanceSub = 1
            openCategory = SettingsCategoryTab.APPEARANCE
        } else {
            if (tab == SettingsCategoryTab.APPEARANCE) appearanceSub = 0
            openCategory = tab
        }
    }

    // Tab pages: a real HorizontalPager (the page follows the finger, snaps, fling) instead of a gesture + AnimatedContent.
    // The page count is "virtual" so the swipe stays endless (last tab -> first tab and back) like before.
    val swipeTabs = remember { SettingsCategoryTab.entries.filter { it != SettingsCategoryTab.LIQUID_GLASS } }
    val swipeTabCount = swipeTabs.size
    val pagerState = rememberPagerState(
        initialPage = swipeTabCount * 500 + swipeTabs.indexOf(SettingsCategoryTab.APPEARANCE).coerceAtLeast(0),
        pageCount = { swipeTabCount * 1000 }
    )
    val pagerAnimToken = remember { intArrayOf(0) }
    var pagerAnimating by remember { mutableStateOf(false) }
    // tab tap / search result -> scroll the pager to that tab (shortest way round)
    LaunchedEffect(openCategory) {
        val index = swipeTabs.indexOf(openCategory)
        val current = pagerState.currentPage
        if (index >= 0 && current % swipeTabCount != index) {
            var delta = index - current % swipeTabCount
            if (delta > swipeTabCount / 2) delta -= swipeTabCount else if (delta < -swipeTabCount / 2) delta += swipeTabCount
            val token = ++pagerAnimToken[0]
            pagerAnimating = true
            try {
                pagerState.animateScrollToPage(
                    page = current + delta,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            } finally {
                if (token == pagerAnimToken[0]) pagerAnimating = false
            }
        }
    }
    // swipe -> the tab row follows the page as soon as it is more than half way over
    LaunchedEffect(pagerState) {
        // also re-checked when a tab-tap animation ends / is interrupted by a finger, so the tab row never stays out of sync
        snapshotFlow { pagerState.currentPage to pagerAnimating }.collect { (page, animating) ->
            if (!animating) {
                val tab = swipeTabs[page % swipeTabCount]
                if (tab != openCategory) {
                    com.example.util.AppHaptics.performClick(sectionHapticView)
                    openCategory = tab
                }
            }
        }
    }

    var showAudioFolderSelectorSheet by remember { mutableStateOf(false) }
    var showSubtitleFolderSelectorSheet by remember { mutableStateOf(false) }
    var isPreferredLanguagesExpanded by remember { mutableStateOf(false) }

    // Subtitle font directory picker & individual font files picker.
    var fontListVersion by remember { mutableIntStateOf(0) }
    val fontDirectoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}
            val count = SubtitleFontManager.importFontsFromTree(context, uri)
            fontListVersion++
            onPlayerSettingsChange(
                uiState.playerSettings.copy(
                    subtitleFontDirectoryUri = uri.toString(),
                    subtitleFontsReloadNonce = System.currentTimeMillis()
                )
            )
            Toast.makeText(
                context,
                if (count > 0) "Loaded $count subtitle font(s)" else "No supported font files found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val fontFilesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var importedCount = 0
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Throwable) {}
                val result = SubtitleFontManager.importFontFromUri(context, uri)
                if (result != null) importedCount++
            }
            fontListVersion++
            onPlayerSettingsChange(
                uiState.playerSettings.copy(
                    subtitleFontsReloadNonce = System.currentTimeMillis()
                )
            )
            Toast.makeText(
                context,
                if (importedCount > 0) "Imported $importedCount font file(s)" else "Could not load font files",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val sheetBg = glassPanelColor(isDark, uiState.glassBlurTransparency)

    val cardBg = if (isDark) {
        if (alphaFactor >= 0.99f) Neutral.D1E293B
        else Neutral.D1E293B.copy(alpha = (0.08f + 0.50f * alphaFactor).coerceIn(0.08f, 0.90f))
    } else {
        if (alphaFactor >= 0.99f) Neutral.FFFFFF
        else Neutral.FFFFFF.copy(alpha = (0.08f + 0.50f * alphaFactor).coerceIn(0.08f, 0.90f))
    }
    val segmentedBg = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
    val borderColor = if (isDark) AccentSkyBlue.copy(alpha = 0.20f) else AccentSkyBlueStrong.copy(alpha = 0.18f)
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    val handleDragModifier = Modifier
        .pointerInput(Unit) {
            // Tap-to-dismiss and drag-to-dismiss are combined inside a SINGLE
            // pointerInput block (instead of two separate stacked pointerInput
            // modifiers) so both gesture detectors observe the exact same
            // pointer event stream. Two independent pointerInput nodes on the
            // same handle used to race for the first touch (the drag detector
            // could "steal" the initial down before the tap detector resolved
            // it), which is what made the handle sometimes need a second tap
            // to register.
            kotlinx.coroutines.coroutineScope {
                launch {
                    detectTapGestures {
                        dismissWithAnimation()
                    }
                }
                launch {
                    detectVerticalDragGestures(
                        onDragStart = { /* Drag started on handle/divider */ },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0f || sheetDragOffsetY.value > 0f) {
                                change.consume()
                                coroutineScope.launch {
                                    sheetDragOffsetY.snapTo(
                                        (sheetDragOffsetY.value + dragAmount).coerceAtLeast(0f)
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            if (sheetDragOffsetY.value > 50f) {
                                dismissWithAnimation()
                            } else {
                                coroutineScope.launch {
                                    sheetDragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                sheetDragOffsetY.animateTo(0f)
                            }
                        }
                    )
                }
            }
        }

    Dialog(
        onDismissRequest = {
            dismissWithAnimation()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            // Outside touches never close the sheet: only Done, the top handle or Back do
            dismissOnClickOutside = false
        )
    ) {
        val view = LocalView.current
        // Keyed: window flags / attributes are IPC calls - applied once, again only when the light/dark look changes
        DisposableEffect(view, isDark) {
            configureEdgeToEdgeDialogWindow(
                view = view,
                isLightStatusBars = !isDark,
                isLightNavBars = !isDark
            )
        
            onDispose { }
        }
        // Liquid Glass: the app behind the dashboard is blurred (Android 12+), no-op when the style is OFF
        if (windowBlurReady) LiquidDialogBlur()

        // Back: close the search first, then the sheet
        BackHandler(enabled = searchOpen) { searchOpen = false }

        CompositionLocalProvider(
            LocalDensity provides scaledDensity,
            LocalGlassBlurTransparency provides uiState.glassBlurTransparency
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // this sheet is its own window: the activity never sees a touch here, so it is reported to the page
                    // backdrop (click-effect backgrounds) and to the live preview from here
                    .reportBackdropTouches()
                    .drawBehind {
                        // read in the DRAW phase: the scrim fade no longer recomposes the whole dashboard every frame
                        drawRect(Color.Black.copy(alpha = (0.35f * alphaFactor * scrimAlpha.value).coerceIn(0.0f, 0.40f)))
                    }
                    .pointerInput(Unit) {
                        // swallow touches on the backdrop: it must NOT dismiss the sheet
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        // draw-phase translation (no re-layout of the whole dashboard on every animation frame)
                        .graphicsLayer { translationY = sheetDragOffsetY.value }
                        .statusBarsPadding()
                        .padding(top = 8.dp)
                        // same fill as before (glassPanelColor); Liquid Glass adds the sheen + specular border
                        .glassPanelSurface(
                            RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            isDark,
                            uiState.glassBlurTransparency
                        )
                        .pointerInput(Unit) {
                            detectTapGestures { /* consume taps inside the sheet */ }
                        }
                ) {
                // HANDLE: tap it or drag it down = close (the ONLY drag-to-close area)
                SettingsSheetHandle(modifier = handleDragModifier, isDark = isDark)
                SettingsSheetHeader(
                    title = AppStrings.getSettingsDashboard(lang),
                    doneLabel = AppStrings.getDone(lang),
                    textColor = primaryText,
                    isDark = isDark,
                    borderColor = borderColor,
                    onSearch = { searchOpen = !searchOpen },
                    onDone = dismissWithAnimation
                )
                // everything below the header (tabs + page) - the search opens OVER all of it
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(modifier = Modifier.fillMaxSize()) {
                SettingsTabRow(
                    selected = openCategory,
                    lang = lang,
                    isDark = isDark,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    borderColor = borderColor,
                    onSelect = openSettingsPage
                )
                // CONTENT ZONE: Isolated from sheet dismissal, scrolls independently
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(consumeContentScrollConnection)
                        .pointerInput(Unit) {
                            detectTapGestures { /* consumes tap events on empty areas so they never dismiss */ }
                        }
                ) {
HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0,
                        key = { it }
                    ) { page ->
                        val currentCategory = swipeTabs[page % swipeTabCount]
                        Column(modifier = Modifier.fillMaxSize()) {
                        val pageScrollState = rememberScrollState()
                        var pageViewportHeight by remember { mutableFloatStateOf(0f) }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                // size only (a position callback would fire on every frame of a swipe)
                                .onSizeChanged { pageViewportHeight = it.height.toFloat() }
                                .verticalScroll(pageScrollState)
                        ) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsPageTitlePanel(tab = currentCategory, lang = lang, isDark = isDark)
                    if (currentCategory == SettingsCategoryTab.APPEARANCE) {
                        LaunchedEffect(appearanceSub) { pageScrollState.scrollTo(0) }
                        PlaylistFilterTabs(
                            tabs = listOf(
                                SettingsCategoryTab.APPEARANCE.getTitle(lang),
                                SettingsCategoryTab.LIQUID_GLASS.getTitle(lang),
                                "Background"
                            ),
                            selectedIndex = appearanceSub,
                            isDark = isDark,
                            onSelect = { appearanceSub = it },
                            animationScale = 0.5f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    if (currentCategory == SettingsCategoryTab.APPEARANCE && appearanceSub >= 2) {
                        // Appearance section 3: Background animation
                        SettingsChunk {
                            BackgroundAnimationSettingsSection(isDark, cardBg, borderColor, primaryText, secondaryText)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else
                    when (if (currentCategory == SettingsCategoryTab.APPEARANCE && appearanceSub == 1) SettingsCategoryTab.LIQUID_GLASS else currentCategory) {
                    SettingsCategoryTab.APPEARANCE -> {
                        SettingsChunk {

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION 1: APP THEME PALETTES
                        Text(
                            text = "APP THEME",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var editingThemeId by remember { mutableStateOf<String?>(null) }
                        // Editor only belongs to the selected theme; close it when another theme gets selected.
                        LaunchedEffect(uiState.appTheme) {
                            if (editingThemeId != null && editingThemeId != uiState.appTheme.id) editingThemeId = null
                        }

                        // One row of small home-screen previews: the first is the user's own Dynamic gradient (double tap = edit its
                        // colours), then 5 ready-made gradients. Tap any of them to apply it.
                        run {
                            val presetTheme = AppThemeVariant.DYNAMIC
                            val themePrefs = LocalContext.current.getSharedPreferences("viaura_theme_preview", android.content.Context.MODE_PRIVATE)
                            fun loadOwn(): List<Long> = themePrefs.getString("own_gradient", null)
                                ?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.takeIf { it.size in 2..3 }
                                ?: presetTheme.gradientHex
                            var ownGradient by remember { mutableStateOf(loadOwn()) }
                            val applied = uiState.customThemeGradients[presetTheme.id] ?: presetTheme.gradientHex
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemePresetPreview(
                                    colors = ownGradient.map { Color(it) },
                                    isDark = isDark,
                                    isSelected = ownGradient == applied,
                                    borderColor = borderColor,
                                    onClick = {
                                        onAppThemeChange(presetTheme)
                                        onCustomThemeGradientChange(presetTheme.id, ownGradient)
                                    },
                                    onDoubleTap = {
                                        onAppThemeChange(presetTheme)
                                        onCustomThemeGradientChange(presetTheme.id, ownGradient)
                                        editingThemeId = presetTheme.id
                                    },
                                    modifier = Modifier.width(104.dp)
                                )
                                ThemeGradientPresets.forEach { preset ->
                                    ThemePresetPreview(
                                        colors = preset.map { Color(it) },
                                        isDark = isDark,
                                        isSelected = preset == applied && preset != ownGradient,
                                        borderColor = borderColor,
                                        onClick = {
                                            onAppThemeChange(presetTheme)
                                            onCustomThemeGradientChange(presetTheme.id, preset)
                                        },
                                        modifier = Modifier.width(104.dp)
                                    )
                                }
                            }

                            // Gradient editor: appears right under the previews (double tap on the first one).
                            AnimatedVisibility(visible = editingThemeId != null) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ThemeGradientEditorPanel(
                                        theme = presetTheme,
                                        currentHex = ownGradient,
                                        cardBg = cardBg,
                                        borderColor = borderColor,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        onCancel = { editingThemeId = null },
                                        onDone = { colors ->
                                            if (colors == null) {
                                                // reset to the default Sky Blue -> Pink
                                                ownGradient = presetTheme.gradientHex
                                                themePrefs.edit().remove("own_gradient").apply()
                                            } else {
                                                ownGradient = colors
                                                themePrefs.edit().putString("own_gradient", colors.joinToString(",")).apply()
                                            }
                                            onAppThemeChange(presetTheme)
                                            onCustomThemeGradientChange(presetTheme.id, colors)
                                            editingThemeId = null
                                        }
                                    )
                                }
                            }
                        }

                        } // end SettingsChunk
                        SettingsChunk {
                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 1.1: THEME MODE & TYPOGRAPHY
                        Text(
                            text = AppStrings.getSectionAppearance(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var showSystemTonePicker by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(segmentedBg)
                                .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                                .padding(4.dp)
                        ) {
                            val modes = listOf(
                                 Triple(ThemeMode.LIGHT, AppStrings.getLightMode(lang), R.drawable.viaura_sun),
                                 Triple(ThemeMode.DARK, AppStrings.getDarkMode(lang), R.drawable.viaura_moon),
                                 Triple(ThemeMode.SYSTEM, "System", R.drawable.viaura_device)
                             )

                            modes.forEach { (mode, label, icon) ->
                                val isSelected = uiState.themeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) AccentGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                        .then(
                                            if (mode == ThemeMode.SYSTEM) {
                                                // single tap = System (wallpaper colours); double tap = muted colour circle picker
                                                Modifier.pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onTap = { onThemeChange(mode) },
                                                        onDoubleTap = {
                                                            onThemeChange(mode)
                                                            showSystemTonePicker = true
                                                        }
                                                    )
                                                }
                                            } else {
                                                Modifier.clickable { onThemeChange(mode) }
                                            }
                                        )
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StyledIcon(
                                            drawableRes = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else secondaryText,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else secondaryText
                                        )
                                    }
                                }
                            }
                        }

                        // Opens inline under the switch (slides open like the theme editor), one muted colour only.
                        AnimatedVisibility(
                            visible = showSystemTonePicker,
                            enter = expandVertically(tween(260, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
                            exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(150))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                SystemTonePanel(
                                    initial = uiState.systemTone,
                                    cardBg = cardBg,
                                    borderColor = borderColor,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onApply = { viewModel?.setSystemTone(it) },
                                    onUseWallpaper = { viewModel?.setSystemTone(null) },
                                    onClose = { showSystemTonePicker = false }
                                )
                            }
                        }
                        } // end SettingsChunk
                        SettingsChunk {
                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION: DISPLAY & FEEL (kept apart from Liquid Glass)
                        Text(
                            text = "DISPLAY & FEEL",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                val isAmoledAvailable = uiState.themeMode != ThemeMode.LIGHT
                                PlayerSwitchRow(
                                    title = "AMOLED Black Mode",
                                    description = if (isAmoledAvailable) "Pure pitch-black (#000000) background for OLED power saving" else "Available in Dark or System mode",
                                    checked = uiState.amoledBlackMode,
                                    enabled = isAmoledAvailable,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onAmoledBlackModeChange
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))
                                PlayerSwitchRow(
                                    title = "Use System Font",
                                    description = "Use device native typography across the interface",
                                    checked = uiState.useSystemFont,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onUseSystemFontChange
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))
                                PlayerSwitchRow(
                                    title = "Haptic Feedback",
                                    description = "Use subtle vibration feedback for key actions and interactions",
                                    checked = uiState.hapticFeedback,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onHapticFeedbackChange
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))
                                PlayerSwitchRow(
                                    title = "Remove Card UI",
                                    description = "Remove the card behind folders and videos: only the thumbnail and text stay, so the background animation looks clearer",
                                    checked = CardStyleSettings.removeCards,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = { CardStyleSettings.setRemoveCards(it) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 1.2: DISPLAY ZOOM & SCALE
                        Text(
                            text = AppStrings.getSectionScaling(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = AppStrings.getAppScaleZoom(lang),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentGradient)
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uiState.appScale.roundToInt()}%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                ViauraSlider(
                                    value = uiState.appScale.coerceIn(1f, 100f),
                                    onValueChange = { newScale ->
                                        onScaleChange(newScale)
                                    },
                                    valueRange = 1f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPink,
                                        activeTrackColor = AccentSkyBlue,
                                        inactiveTrackColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else AccentSkyBlue.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "1%",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                    Text(
                                        text = "100%",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 1.3: UI BLUR & GLASS TRANSPARENCY
                        Text(
                            text = AppStrings.getSectionGlassBlur(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = AppStrings.getGlassBlurTitle(lang),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                        Text(
                                            text = AppStrings.getGlassBlurSubtitle(lang),
                                            fontSize = 11.5.sp,
                                            color = secondaryText
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentGradient)
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uiState.glassBlurTransparency.roundToInt()}%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                ViauraSlider(
                                    value = uiState.glassBlurTransparency.coerceIn(10f, 100f),
                                    onValueChange = { newTrans ->
                                        onGlassBlurTransparencyChange(newTrans)
                                    },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPink,
                                        activeTrackColor = AccentSkyBlue,
                                        inactiveTrackColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else AccentSkyBlue.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = AppStrings.getGlassBlurLow(lang),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                    Text(
                                        text = AppStrings.getGlassBlurHigh(lang),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECTION 2: FILE BROWSER
                        Text(
                            text = "FILE DISPLAY & BADGES",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                PlayerSwitchRow(
                                    title = "Show Full Filename",
                                    description = "Display full video file extension and unaltered filenames in library",
                                    checked = uiState.showFullName,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowFullNameChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Show 'NEW' Video Badge",
                                    description = "Highlight recently discovered videos with a vibrant NEW badge",
                                    checked = uiState.showNewVideoLabel,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowNewVideoLabelChange
                                )

                                AnimatedVisibility(visible = uiState.showNewVideoLabel) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = borderColor)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        PlayerSliderRow(
                                            title = "'NEW' Badge Threshold",
                                            description = "Maximum age (in days) for a video to be marked as NEW",
                                            value = uiState.newVideoDaysThreshold,
                                            min = 1,
                                            max = 30,
                                            unit = " days",
                                            isDark = isDark,
                                            primaryText = primaryText,
                                            secondaryText = secondaryText,
                                            onChange = onNewVideoDaysThresholdChange
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Show Unplayed Badge on Folders",
                                    description = "Display number of unplayed videos on top of video folders",
                                    checked = uiState.showFolderUnplayedBadge,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowFolderUnplayedBadgeChange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "DIRECTORY NAVIGATION & LAYOUT",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                PlayerSwitchRow(
                                    title = "Auto-scroll to Last Played",
                                    description = "Automatically focus and center on the last played video when opening folders",
                                    checked = uiState.autoScrollToLastPlayed,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onAutoScrollToLastPlayedChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Path Compression in Tree",
                                    description = "Collapse single-child nested folders into compact combined nodes",
                                    checked = uiState.treePathCompression,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onTreePathCompressionChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Dual-Pane View on Wide Displays",
                                    description = "Show folder list and media items side-by-side on tablets and foldables",
                                    checked = uiState.dualPaneView,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onDualPaneViewChange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "WATCHED PROGRESS THRESHOLD",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            PlayerSliderRow(
                                title = "Watched Status Completion",
                                description = "Percentage of video length after which it is marked as completed/watched",
                                value = uiState.watchedThresholdPercent,
                                min = 50,
                                max = 100,
                                unit = "%",
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                onChange = onWatchedThresholdPercentChange
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                        SettingsChunk {

                        // SECTION 3: THUMBNAILS
                        Text(
                            text = "THUMBNAIL EXTRACTION",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                PlayerSwitchRow(
                                    title = "Show Video Thumbnails",
                                    description = "Generate visual frame previews for videos in lists and grids",
                                    checked = uiState.showVideoThumbnails,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowVideoThumbnailsChange
                                )

                                AnimatedVisibility(visible = uiState.showVideoThumbnails) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = borderColor)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        PlayerChoiceRow(
                                            title = "Extraction Strategy",
                                            description = "Choose how thumbnail artwork is selected from the video",
                                            value = uiState.thumbnailStrategy.name,
                                            options = ThumbnailStrategy.entries.map { it.name to it.displayName },
                                            isDark = isDark,
                                            primaryText = primaryText,
                                            secondaryText = secondaryText,
                                            borderColor = borderColor,
                                            alphaFactor = alphaFactor,
                                            onChange = { sel ->
                                                onThumbnailStrategyChange(ThumbnailStrategy.entries.firstOrNull { it.name == sel } ?: ThumbnailStrategy.SMART)
                                            }
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = borderColor)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        PlayerChoiceRow(
                                            title = "Thumbnail Quality",
                                            description = "Resolution and cache density for image generation",
                                            value = uiState.thumbnailQuality.name,
                                            options = ThumbnailQuality.entries.map { it.name to it.displayName },
                                            isDark = isDark,
                                            primaryText = primaryText,
                                            secondaryText = secondaryText,
                                            borderColor = borderColor,
                                            alphaFactor = alphaFactor,
                                            onChange = { sel ->
                                                onThumbnailQualityChange(ThumbnailQuality.entries.firstOrNull { it.name == sel } ?: ThumbnailQuality.HIGH)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "THUMBNAIL INTERACTION",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                PlayerSwitchRow(
                                    title = "Tap Thumbnail to Multi-Select",
                                    description = "Tapping the thumbnail preview directly engages selection mode",
                                    checked = uiState.tapThumbnailToSelect,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onTapThumbnailToSelectChange
                                )

                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECTION 4: NAVIGATION
                        Text(
                            text = "BOTTOM BAR TAB VISIBILITY",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                PlayerSwitchRow(
                                    title = "Videos / Home Tab",
                                    description = "Display primary video library and folders screen",
                                    checked = uiState.showHomeTab,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowHomeTabChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Audio / Music Tab",
                                    description = "Display dedicated music player and audio files tab",
                                    checked = uiState.showMusicTab,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowMusicTabChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Recently Played Tab",
                                    description = "Display playback history and quick resume section",
                                    checked = uiState.showRecentsTab,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowRecentsTabChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Custom Playlists Tab",
                                    description = "Display custom playlists and categorized series collections",
                                    checked = uiState.showPlaylistsTab,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowPlaylistsTabChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Network Tab",
                                    description = "Paste a link (YouTube and other supported sites, or a direct video URL) and play it in the player",
                                    checked = uiState.showNetworkTab,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowNetworkTabChange
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerSwitchRow(
                                    title = "Downloads Tab",
                                    description = "Open your downloads directly from the bottom bar",
                                    checked = uiState.showDownloadsTab,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    onChange = onShowDownloadsTabChange
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                        SettingsChunk {

                        // SECTION 5: ANIMATIONS
                        Text(
                            text = "TRANSITION STYLES",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column {
                                PlayerChoiceRow(
                                    title = "Player Controls Animation",
                                    description = "Style of on-screen controls overlay entrance and exit",
                                    value = uiState.controlsAnimationStyle.name,
                                    options = ControlsAnimationStyle.entries.map { it.name to it.displayName },
                                    isDark = isDark,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    borderColor = borderColor,
                                    alphaFactor = alphaFactor,
                                    onChange = { sel ->
                                        onControlsAnimationStyleChange(ControlsAnimationStyle.entries.firstOrNull { it.name == sel } ?: ControlsAnimationStyle.DEFAULT)
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerChoiceRow(
                                    title = "Video Player Enter Transition",
                                    description = "Visual motion when launching video playback from library",
                                    value = uiState.videoOpeningAnimation.name,
                                    options = VideoOpeningAnimation.entries.map { it.name to it.displayName },
                                    isDark = isDark,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    borderColor = borderColor,
                                    alphaFactor = alphaFactor,
                                    onChange = { sel ->
                                        onVideoOpeningAnimationChange(VideoOpeningAnimation.entries.firstOrNull { it.name == sel } ?: VideoOpeningAnimation.DEFAULT)
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerChoiceRow(
                                    title = "Screen Transition Style",
                                    description = "Visual page transition when navigating between application views",
                                    value = uiState.screenNavigationStyle.name,
                                    options = ScreenNavigationStyle.entries.map { it.name to it.displayName },
                                    isDark = isDark,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    borderColor = borderColor,
                                    alphaFactor = alphaFactor,
                                    onChange = { sel ->
                                        onScreenNavigationStyleChange(ScreenNavigationStyle.entries.firstOrNull { it.name == sel } ?: ScreenNavigationStyle.DEFAULT)
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(10.dp))

                                PlayerChoiceRow(
                                    title = "Tab Switch Animation",
                                    description = "Motion when switching between bottom or category tabs",
                                    value = uiState.tabNavigationStyle.name,
                                    options = TabNavigationStyle.entries.map { it.name to it.displayName },
                                    isDark = isDark,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    borderColor = borderColor,
                                    alphaFactor = alphaFactor,
                                    onChange = { sel ->
                                        onTabNavigationStyleChange(TabNavigationStyle.entries.firstOrNull { it.name == sel } ?: TabNavigationStyle.DEFAULT)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "ANIMATION SPEED MULTIPLIER",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = "Global Motion Scale",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                        Text(
                                            text = "Scale duration of all UI animations across the app",
                                            fontSize = 11.5.sp,
                                            color = secondaryText
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentGradient)
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.2fx", uiState.animationSpeed),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                ViauraSlider(
                                    value = uiState.animationSpeed.coerceIn(0.25f, 2.0f),
                                    onValueChange = { newSpeed ->
                                        onAnimationSpeedChange(newSpeed)
                                    },
                                    valueRange = 0.25f..2.0f,
                                    steps = 6,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPink,
                                        activeTrackColor = AccentSkyBlue,
                                        inactiveTrackColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else AccentSkyBlue.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "0.25x (Slower)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                    Text(
                                        text = "1.00x (Normal)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                    Text(
                                        text = "2.00x (Faster)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECTION 6: INTERFACE LANGUAGE
                        Text(
                            text = AppStrings.getSectionLanguage(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(segmentedBg)
                                .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                                .padding(4.dp)
                        ) {
                            AppLanguage.entries.forEach { appLang ->
                                val isLangSelected = uiState.language == appLang
                                val label = when (appLang) {
                                    AppLanguage.ENGLISH -> "English"
                                    AppLanguage.HINGLISH -> "Hinglish"
                                    AppLanguage.HINDI -> "हिन्दी"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isLangSelected) AccentGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                        .clickable { onLanguageChange(appLang) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isLangSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isLangSelected) Color.White else secondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                    }

                    SettingsCategoryTab.LIQUID_GLASS -> {
                        SettingsChunk {
                        Spacer(modifier = Modifier.height(4.dp))
                        // SECTION: LIQUID GLASS (own section; tap the row to slide the settings open)
                        Text(
                            text = "LIQUID GLASS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Column {
                                // The section is always open (no open / close animation). Only the Liquid Glass switch decides
                                // whether the options below it exist: ON -> they are there at once, OFF -> they are gone.
                                val liquidDensity = LocalDensity.current
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(38.dp).clip(CircleShape).background(AccentSkyBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StyledIcon(imageVector = Icons.Outlined.Tune, contentDescription = null, tint = AccentSkyBlue, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Liquid Glass", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = (if (uiState.liquidGlassMode) "On" else "Off"),
                                            fontSize = 11.5.sp,
                                            color = if (uiState.liquidGlassMode) AccentSkyBlue else secondaryText
                                        )
                                    }
                                }

                                // Liquid Glass options: slide out right under the switch (only while it is ON)
                                val gp = uiState.liquidGlassParams
                                @Composable
                                fun GlassSliderSetting(
                                    title: String,
                                    description: String,
                                    value: Float,
                                    range: ClosedFloatingPointRange<Float>,
                                    signed: Boolean = false,
                                    onChange: (Float) -> Unit
                                ) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = borderColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                            Text(text = description, fontSize = 11.5.sp, color = secondaryText)
                                        }
                                        val shown = value.roundToInt()
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AccentGradient)
                                                .padding(horizontal = 14.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (if (signed && shown > 0) "+" else "") + "$shown%",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    ViauraSlider(
                                        value = value.coerceIn(range.start, range.endInclusive),
                                        onValueChange = onChange,
                                        valueRange = range,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AccentPink,
                                            activeTrackColor = AccentSkyBlue,
                                            inactiveTrackColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else AccentSkyBlue.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${range.start.roundToInt()}%", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = secondaryText)
                                        Text(text = "${range.endInclusive.roundToInt()}%", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = secondaryText)
                                    }
                                }
                                @Composable
                                fun GlassSwitchSetting(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = borderColor)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    PlayerSwitchRow(
                                        title = title,
                                        description = description,
                                        checked = checked,
                                        primaryText = primaryText,
                                        secondaryText = secondaryText,
                                        onChange = onChange
                                    )
                                }

                                AnimatedVisibility(
                                    visible = true,
                                    enter = EnterTransition.None,
                                    exit = ExitTransition.None
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                        HorizontalDivider(color = borderColor)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        PlayerSwitchRow(
                                            title = "Liquid Glass",
                                            description = "Frosted glassy effect: translucent blur, chamakdaar safed border, round shapes",
                                            checked = uiState.liquidGlassMode,
                                            primaryText = primaryText,
                                            secondaryText = secondaryText,
                                            onChange = onLiquidGlassModeChange
                                        )
                                        // Liquid Glass options appear below the switch only while it is ON
                                        AnimatedVisibility(
                                            visible = uiState.liquidGlassMode,
                                            enter = EnterTransition.None,
                                            exit = ExitTransition.None
                                        ) {
                                            Column {
                                                // Liquid Glass sliders (seek bars, settings sliders) + ON / OFF switches
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = borderColor)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                PlayerSwitchRow(
                                                    title = "Liquid Glass Sliders & Switches",
                                                    description = "Sliders (video seek bar, audio seek bar, settings) and ON / OFF buttons become liquid glass. The filled part and the ON knob use your selected gradient colour",
                                                    checked = uiState.liquidGlassControls,
                                                    primaryText = primaryText,
                                                    secondaryText = secondaryText,
                                                    onChange = onLiquidGlassControlsChange
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                // Live preview: stays fixed at the top while the options below scroll
                                                Box {
                                                    LiquidGlassPreview()
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                // The preview is pinned at the top of the page; the options list takes ALL the remaining height
                                                // down to the bottom of the sheet (no more small fixed box) and scrolls inside.
                                                val optionsHeight = with(liquidDensity) {
                                                    (pageViewportHeight.toDp() - 170.dp - 24.dp).coerceAtLeast(260.dp)
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(optionsHeight)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                            GlassSliderSetting("Opacity", "Strength of the blurred picture behind the glass", gp.opacity, 0f..100f) { onLiquidGlassParamsChange(gp.copy(opacity = it)) }
                                            GlassSliderSetting("Blur", "How frosted the glass looks", gp.blur, 0f..200f) { onLiquidGlassParamsChange(gp.copy(blur = it)) }
                                            GlassSliderSetting("Tint Opacity", "Colour wash on top of the glass", gp.tint, 0f..100f) { onLiquidGlassParamsChange(gp.copy(tint = it)) }
                                            GlassSliderSetting("Refraction Height", "How far from the edge the glass bends the picture", gp.refractionHeight, 0f..200f) { onLiquidGlassParamsChange(gp.copy(refractionHeight = it)) }
                                            GlassSliderSetting("Refraction Amount", "How strongly the edge bends the picture", gp.refractionAmount, 0f..200f) { onLiquidGlassParamsChange(gp.copy(refractionAmount = it)) }
                                            GlassSwitchSetting("Depth Effect", "Adds depth shading to the bent edge", gp.depthEffect) { onLiquidGlassParamsChange(gp.copy(depthEffect = it)) }
                                            GlassSwitchSetting("Chromatic Aberration", "Slight colour fringe at the glass edge", gp.chromaticAberration) { onLiquidGlassParamsChange(gp.copy(chromaticAberration = it)) }
                                            GlassSwitchSetting("Vibrancy", "Boosts the colours seen through the glass", gp.vibrancy) { onLiquidGlassParamsChange(gp.copy(vibrancy = it)) }
                                            GlassSliderSetting("Saturation", "Colour intensity behind the glass", gp.saturation, 0f..200f) { onLiquidGlassParamsChange(gp.copy(saturation = it)) }
                                            GlassSliderSetting("Brightness", "Lighter or darker glass", gp.brightness, -100f..100f, signed = true) { onLiquidGlassParamsChange(gp.copy(brightness = it)) }
                                            GlassSliderSetting("Contrast", "Difference between light and dark behind the glass", gp.contrast, 0f..200f) { onLiquidGlassParamsChange(gp.copy(contrast = it)) }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = borderColor)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(text = "Highlight Style", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                            Text(text = "Look of the shiny glass border", fontSize = 11.5.sp, color = secondaryText)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf("Component", "Default", "Ambient", "Plain").forEachIndexed { index, label ->
                                                    val selected = gp.highlightStyle == index
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .then(if (selected) Modifier.background(AccentGradient) else Modifier.border(1.dp, borderColor, RoundedCornerShape(14.dp)))
                                                            .clickable { onLiquidGlassParamsChange(gp.copy(highlightStyle = index)) }
                                                            .padding(vertical = 9.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (selected) Color.White else secondaryText,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                            GlassSliderSetting("Highlight Strength", "Brightness of the border light", gp.highlightStrength, 0f..200f) { onLiquidGlassParamsChange(gp.copy(highlightStrength = it)) }
                                            GlassSliderSetting("Highlight Width", "Thickness of the border", gp.highlightWidth, 0f..200f) { onLiquidGlassParamsChange(gp.copy(highlightWidth = it)) }
                                            GlassSliderSetting("Highlight Blur", "Softness / glow of the border", gp.highlightBlur, 0f..200f) { onLiquidGlassParamsChange(gp.copy(highlightBlur = it)) }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = borderColor)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                                                    .clickable { onLiquidGlassParamsChange(LiquidGlassParams()) }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Reset Liquid Glass options", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                            }
                        }

                        } // end SettingsChunk
                    }

                    SettingsCategoryTab.FOLDERS -> {

                        Spacer(modifier = Modifier.height(16.dp))

                        FoldersSettingsTabContent(
                            isDark = isDark,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            glassBlurTransparency = uiState.glassBlurTransparency
                        )
                    }

                    SettingsCategoryTab.PLAYER -> {
                        SettingsChunk {

                        Spacer(modifier = Modifier.height(16.dp))

                        // TITLE NAME PLAYER — extended native player settings.
                        PlayerSettingsPanel(
                            settings = uiState.playerSettings,
                            onSettingsChange = onPlayerSettingsChange,
                            isDark = isDark,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            alphaFactor = alphaFactor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = AppStrings.getSectionPlayback(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(AppStrings.getCopyTimestamp(lang), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText)
                                        Text(AppStrings.getCopyTimestampDesc(lang), fontSize = 12.sp, color = secondaryText)
                                    }
                                    ViauraSwitch(
                                        checked = uiState.copyTimestampOnDoubleTap,
                                        onCheckedChange = onCopyTimestampChange,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AccentSkyBlue
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = borderColor)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(AppStrings.getForceSideBySide(lang), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText)
                                        Text(AppStrings.getForceSideBySideDesc(lang), fontSize = 12.sp, color = secondaryText)
                                    }
                                    ViauraSwitch(
                                        checked = uiState.forceSideBySide,
                                        onCheckedChange = onSideBySideChange,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AccentSkyBlue
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = AppStrings.getSectionFastSeek(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(segmentedBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(4.dp)
                        ) {
                            listOf(1, 5, 10, 15).forEach { sec ->
                                val isSelected = uiState.frameStepAmount == sec
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) AccentGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                                        .clickable { onFrameStepChange(sec) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else secondaryText
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = AppStrings.getSectionGestureSensitivity(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GestureSensitivityMode.entries.forEach { mode ->
                                val isSelected = uiState.gestureSensitivityMode == mode
                                val icon = when (mode) {
                                    GestureSensitivityMode.EXPONENTIAL -> Icons.Outlined.Speed
                                    GestureSensitivityMode.LINEAR -> Icons.Outlined.Tune
                                }
                                val cardShape = RoundedCornerShape(20.dp)
                                val cardBackground = if (isSelected) {
                                    if (isDark) AccentContainerDark.copy(alpha = 0.50f) else AccentContainerLight
                                } else {
                                    if (isDark) Color(0x1AFFFFFF) else Neutral.F1F5F9
                                }
                                val cardBorder = if (isSelected) {
                                    BorderStroke(1.5.dp, AccentSkyBlue)
                                } else {
                                    BorderStroke(1.dp, borderColor)
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .clickable { onGestureSensitivityModeChange(mode) },
                                    shape = cardShape,
                                    color = cardBackground,
                                    border = cardBorder
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AccentSkyBlue.copy(alpha = 0.2f) else Color(0x15FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            StyledIcon(
                                imageVector = icon,
                                                contentDescription = mode.getDisplayName(lang),
                                                tint = if (isSelected) AccentSkyBlue else secondaryText,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = mode.getDisplayName(lang),
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryText
                                                )
                                                if (mode == GestureSensitivityMode.EXPONENTIAL) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = AccentSkyBlue.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = AppStrings.getRecommended(lang),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = AccentSkyBlue,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = mode.getDescription(lang),
                                                fontSize = 12.sp,
                                                color = secondaryText,
                                                lineHeight = 16.sp
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onGestureSensitivityModeChange(mode) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = AccentSkyBlue,
                                                unselectedColor = secondaryText.copy(alpha = 0.6f)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION: PLAYER NOTIFICATIONS
                        Text(
                            text = AppStrings.getSectionPlayerNotification(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppStrings.getPlayerNotificationTitle(lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppStrings.getPlayerNotificationDesc(lang),
                                        fontSize = 12.sp,
                                        color = secondaryText,
                                        lineHeight = 16.sp
                                    )
                                }
                                ViauraSwitch(
                                    checked = uiState.showPlayerNotifications,
                                    onCheckedChange = onShowPlayerNotificationsChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentSkyBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                    }

                    SettingsCategoryTab.AUDIO -> {
                        SettingsChunk {

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION 1: PREFERRED AUDIO LANGUAGES
                        Text(
                            text = AppStrings.getSectionPreferredLanguages(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Header Row (Always visible, tap to expand/collapse)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isPreferredLanguagesExpanded = !isPreferredLanguagesExpanded }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(AccentSkyBlue.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            StyledIcon(
                                imageVector = Icons.Outlined.Translate,
                                                contentDescription = null,
                                                tint = AccentSkyBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = AppStrings.getPreferredLanguagesTitle(lang),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = primaryText
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val currentLangs = uiState.preferredAudioLanguages.trim()
                                            val summaryText = if (currentLangs.isNotBlank()) {
                                                val targetInfo = if (uiState.applyAudioLanguageToSelectedContentOnly) {
                                                    val total = uiState.selectedAudioFolders.size + uiState.selectedAudioVideos.size
                                                    if (total > 0) " • $total items" else " • 0 items"
                                                } else {
                                                    " • All media"
                                                }
                                                "$currentLangs$targetInfo"
                                            } else {
                                                "Tap to configure"
                                            }
                                            Text(
                                                text = summaryText,
                                                fontSize = 11.5.sp,
                                                color = if (currentLangs.isNotBlank()) AccentSkyBlue else secondaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Expand / Collapse Chevron Button
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPreferredLanguagesExpanded) AccentSkyBlue.copy(alpha = 0.18f)
                                                else if (isDark) Color.White.copy(alpha = 0.08f)
                                                else Color.Black.copy(alpha = 0.05f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StyledIcon(
                                imageVector = if (isPreferredLanguagesExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                            contentDescription = if (isPreferredLanguagesExpanded) "Collapse" else "Expand",
                                            tint = if (isPreferredLanguagesExpanded) AccentSkyBlue else secondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Collapsible Content
                                AnimatedVisibility(
                                    visible = isPreferredLanguagesExpanded,
                                    enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                                    exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                    ) {
                                        HorizontalDivider(color = borderColor)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = AppStrings.getPreferredLanguagesDesc(lang),
                                            fontSize = 12.sp,
                                            color = secondaryText,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val audioLangContainerColor = if (isDark) {
                                            Neutral.D0F172A.copy(alpha = (0.25f + 0.45f * alphaFactor).coerceIn(0.15f, 0.85f))
                                        } else {
                                            Neutral.FFFFFF.copy(alpha = (0.30f + 0.50f * alphaFactor).coerceIn(0.20f, 0.85f))
                                        }
                                        OutlinedTextField(
                                            value = uiState.preferredAudioLanguages,
                                            onValueChange = onPreferredAudioLanguagesChange,
                                            placeholder = {
                                                Text(
                                                    text = "e.g. hin, eng, jpn, spa",
                                                    fontSize = 13.sp,
                                                    color = secondaryText.copy(alpha = 0.6f)
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AccentSkyBlue,
                                                unfocusedBorderColor = borderColor,
                                                focusedTextColor = primaryText,
                                                unfocusedTextColor = primaryText,
                                                focusedContainerColor = audioLangContainerColor,
                                                unfocusedContainerColor = audioLangContainerColor,
                                                cursorColor = AccentSkyBlue
                                            ),
                                            trailingIcon = {
                                                if (uiState.preferredAudioLanguages.isNotBlank()) {
                                                    IconButton(onClick = { onPreferredAudioLanguagesChange("") }) {
                                                        StyledIcon(
                                                            imageVector = Icons.Outlined.Close,
                                                            contentDescription = "Clear",
                                                            tint = secondaryText,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Quick-Add Language Chips
                                        Text(
                                            text = "Quick Presets:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = secondaryText
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        val currentTokens = remember(uiState.preferredAudioLanguages) {
                                            AudioLanguageMatcher.normalizePreferredLanguages(uiState.preferredAudioLanguages)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AudioLanguageMatcher.POPULAR_LANGUAGES.take(10).forEach { (code, label) ->
                                                val isPresent = currentTokens.contains(code)
                                                val chipBg = if (isPresent) {
                                                    if (isDark) AccentSkyBlueStrong.copy(alpha = 0.40f) else AccentContainerLight
                                                } else {
                                                    if (isDark) Color(0x1AFFFFFF) else Neutral.F1F5F9
                                                }
                                                val chipBorder = if (isPresent) AccentSkyBlue else borderColor

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(chipBg)
                                                        .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            val tokens = AudioLanguageMatcher.normalizePreferredLanguages(uiState.preferredAudioLanguages).toMutableList()
                                                            if (tokens.contains(code)) {
                                                                tokens.remove(code)
                                                            } else {
                                                                tokens.add(code)
                                                            }
                                                            onPreferredAudioLanguagesChange(tokens.joinToString(", "))
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (isPresent) {
                                                            StyledIcon(
                                imageVector = Icons.Outlined.Check,
                                                                contentDescription = null,
                                                                tint = AccentSkyBlue,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isPresent) AccentSkyBlue else primaryText
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = borderColor)
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Sub-option: Apply to selected folder/videos only
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = AppStrings.getApplyToSelectedContentTitle(lang),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = primaryText
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = AppStrings.getApplyToSelectedContentDesc(lang),
                                                    fontSize = 11.5.sp,
                                                    color = secondaryText,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            ViauraSwitch(
                                                checked = uiState.applyAudioLanguageToSelectedContentOnly,
                                                onCheckedChange = onApplyAudioLanguageToSelectedContentOnlyChange,
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = AccentSkyBlue
                                                )
                                            )
                                        }

                                        // Rounded Rectangular Box for Selected Folders & Videos
                                        AnimatedVisibility(
                                            visible = uiState.applyAudioLanguageToSelectedContentOnly,
                                            enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                                            exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(
                                                            if (isDark) Neutral.D0F172A.copy(alpha = 0.85f)
                                                            else Neutral.F1F5F9.copy(alpha = 0.95f)
                                                        )
                                                        .border(
                                                            1.5.dp,
                                                            if (isDark) AccentSkyBlue.copy(alpha = 0.45f) else AccentSkyBlueStrong.copy(alpha = 0.35f),
                                                            RoundedCornerShape(16.dp)
                                                        )
                                                        .padding(14.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        // Header Row
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(28.dp)
                                                                        .clip(CircleShape)
                                                                        .background(AccentSkyBlue.copy(alpha = 0.18f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    StyledIcon(
                                imageVector = Icons.Outlined.FolderSpecial,
                                                                        contentDescription = null,
                                                                        tint = AccentSkyBlue,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "Target Folders & Videos",
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = primaryText
                                                                )
                                                            }

                                                            val totalCount = uiState.selectedAudioFolders.size + uiState.selectedAudioVideos.size
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (totalCount > 0) AccentSkyBlue.copy(alpha = 0.20f)
                                                                        else if (isDark) Color.White.copy(alpha = 0.10f)
                                                                        else Color.Black.copy(alpha = 0.06f)
                                                                    )
                                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "$totalCount selected",
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (totalCount > 0) AccentSkyBlue else secondaryText
                                                                )
                                                            }
                                                        }

                                                        // List of selected tags/chips
                                                        val hasItems = uiState.selectedAudioFolders.isNotEmpty() || uiState.selectedAudioVideos.isNotEmpty()
                                                        if (hasItems) {
                                                            @OptIn(ExperimentalLayoutApi::class)
                                                            FlowRow(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                uiState.selectedAudioFolders.forEach { folderPath ->
                                                                    val folderName = File(folderPath).name.ifBlank { folderPath }
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(10.dp))
                                                                            .background(if (isDark) Neutral.D1E293B else AccentSkyBlueStrong.copy(alpha = 0.10f))
                                                                            .border(
                                                                                1.dp,
                                                                                AccentSkyBlue.copy(alpha = 0.40f),
                                                                                RoundedCornerShape(10.dp)
                                                                            )
                                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    ) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            StyledIcon(
                                imageVector = Icons.Outlined.Folder,
                                                                                contentDescription = null,
                                                                                tint = AccentSkyBlue,
                                                                                modifier = Modifier.size(13.dp)
                                                                            )
                                                                            Text(
                                                                                text = folderName,
                                                                                fontSize = 11.5.sp,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = primaryText,
                                                                                maxLines = 1
                                                                            )
                                                                            StyledIcon(
                                imageVector = Icons.Outlined.Close,
                                                                                contentDescription = "Remove",
                                                                                tint = secondaryText,
                                                                                modifier = Modifier
                                                                                    .size(14.dp)
                                                                                    .clickable {
                                                                                        val newSet = uiState.selectedAudioFolders.toMutableSet()
                                                                                        newSet.remove(folderPath)
                                                                                        onSelectedAudioFoldersChange(newSet)
                                                                                    }
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                uiState.selectedAudioVideos.forEach { videoPath ->
                                                                    val videoName = File(videoPath).name.ifBlank { videoPath }
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(10.dp))
                                                                            .background(if (isDark) Neutral.D1E293B else Color(0x1A6366F1))
                                                                            .border(
                                                                                1.dp,
                                                                                AccentMid.copy(alpha = 0.40f),
                                                                                RoundedCornerShape(10.dp)
                                                                            )
                                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    ) {
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                                            StyledIcon(
                                imageVector = Icons.Outlined.Movie,
                                                                                contentDescription = null,
                                                                                tint = AccentMid,
                                                                                modifier = Modifier.size(13.dp)
                                                                            )
                                                                            Text(
                                                                                text = videoName,
                                                                                fontSize = 11.5.sp,
                                                                                fontWeight = FontWeight.Medium,
                                                                                color = primaryText,
                                                                                maxLines = 1
                                                                            )
                                                                            StyledIcon(
                                imageVector = Icons.Outlined.Close,
                                                                                contentDescription = "Remove",
                                                                                tint = secondaryText,
                                                                                modifier = Modifier
                                                                                    .size(14.dp)
                                                                                    .clickable {
                                                                                        val newSet = uiState.selectedAudioVideos.toMutableSet()
                                                                                        newSet.remove(videoPath)
                                                                                        onSelectedAudioVideosChange(newSet)
                                                                                    }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Text(
                                                                text = "No folders or videos selected yet. Tap '+' below to select directories or videos where auto-selection rule applies.",
                                                                fontSize = 11.5.sp,
                                                                color = secondaryText,
                                                                lineHeight = 15.sp
                                                            )
                                                        }

                                                        // Plus Add Button
                                                        Button(
                                                            onClick = { showAudioFolderSelectorSheet = true },
                                                            shape = RoundedCornerShape(12.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = Color.Transparent
                                                            ),
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(38.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(AccentGradient)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                StyledIcon(
                                imageVector = Icons.Outlined.Add,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Text(
                                                                    text = "+ Add Folders or Videos",
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        } // end SettingsChunk
                        SettingsChunk {

                        // SECTION 2: PLAYBACK & SOUND PROCESSING
                        Text(
                            text = AppStrings.getSectionSoundProcessing(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Audio Pitch Correction
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = AppStrings.getAudioPitchCorrectionTitle(lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = AppStrings.getAudioPitchCorrectionDesc(lang),
                                            fontSize = 12.sp,
                                            color = secondaryText,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ViauraSwitch(
                                        checked = uiState.enableAudioPitchCorrection,
                                        onCheckedChange = onEnableAudioPitchCorrectionChange,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AccentSkyBlue
                                        )
                                    )
                                }

                                HorizontalDivider(color = borderColor)

                                // 2. Volume Normalization
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = AppStrings.getVolumeNormalizationTitle(lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = AppStrings.getVolumeNormalizationDesc(lang),
                                            fontSize = 12.sp,
                                            color = secondaryText,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ViauraSwitch(
                                        checked = uiState.volumeNormalization,
                                        onCheckedChange = onVolumeNormalizationChange,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AccentSkyBlue
                                        )
                                    )
                                }

                                HorizontalDivider(color = borderColor)

                                // 3. Remember Selected Audio Track
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = AppStrings.getRememberAudioTrackTitle(lang),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = AppStrings.getRememberAudioTrackDesc(lang),
                                            fontSize = 12.sp,
                                            color = secondaryText,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ViauraSwitch(
                                        checked = uiState.rememberSelectedAudioTrack,
                                        onCheckedChange = onRememberSelectedAudioTrackChange,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = AccentSkyBlue
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 3: BACKGROUND PLAYBACK
                        Text(
                            text = AppStrings.getSectionBackgroundPlayback(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = AppStrings.getBackgroundPlaybackTitle(lang),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = AppStrings.getBackgroundPlaybackDesc(lang),
                                        fontSize = 12.sp,
                                        color = secondaryText,
                                        lineHeight = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                ViauraSwitch(
                                    checked = uiState.backgroundPlayback,
                                    onCheckedChange = onBackgroundPlaybackChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AccentSkyBlue
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 4: AUDIO CHANNELS
                        Text(
                            text = AppStrings.getSectionAudioChannels(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AudioChannelMode.entries.forEach { mode ->
                                val isSelected = uiState.audioChannelMode == mode
                                val icon = when (mode) {
                                    AudioChannelMode.AUTO, AudioChannelMode.AUTO_SAFE -> Icons.Outlined.GraphicEq
                                    AudioChannelMode.STEREO, AudioChannelMode.REVERSED_STEREO -> Icons.Outlined.Headphones
                                    AudioChannelMode.MONO -> Icons.Outlined.VolumeUp
                                    AudioChannelMode.LEFT, AudioChannelMode.RIGHT -> Icons.Outlined.SurroundSound
                                }
                                val cardShape = RoundedCornerShape(20.dp)
                                val itemBg = if (isSelected) {
                                    if (isDark) AccentContainerDark.copy(alpha = 0.50f) else AccentContainerLight
                                } else {
                                    if (isDark) Color(0x1AFFFFFF) else Neutral.F1F5F9
                                }
                                val itemBorder = if (isSelected) {
                                    if (isDark) AccentSkyBlueStrong else AccentSkyBlue
                                } else {
                                    if (isDark) Color(0x22FFFFFF) else Neutral.E2E8F0
                                }
                                val iconTint = if (isSelected) {
                                    if (isDark) AccentSkyBlue else AccentSkyBlueStrong
                                } else {
                                    secondaryText
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .background(itemBg)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = itemBorder,
                                            shape = cardShape
                                        )
                                        .clickable { onAudioChannelModeChange(mode) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StyledIcon(
                                imageVector = icon,
                                            contentDescription = mode.getDisplayName(lang),
                                            tint = iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = mode.getDisplayName(lang),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = mode.getDescription(lang),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = secondaryText
                                            )
                                        }

                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) AccentSkyBlueStrong else AccentSkyBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StyledIcon(
                                imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // SECTION 5: VOLUME AMPLIFICATION & BOOST
                        Text(
                            text = AppStrings.getSectionVolumeBoost(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = AppStrings.getVolumeBoostTitle(lang),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentGradient)
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${uiState.volumeBoostCap}%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = AppStrings.getVolumeBoostDesc(lang),
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                ViauraSlider(
                                    value = uiState.volumeBoostCap.toFloat(),
                                    onValueChange = { onVolumeBoostCapChange(it.roundToInt()) },
                                    valueRange = 100f..200f,
                                    steps = 9,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPink,
                                        activeTrackColor = AccentSkyBlue,
                                        inactiveTrackColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else AccentSkyBlue.copy(alpha = 0.15f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("100% (1.0x)", fontSize = 11.sp, color = secondaryText)
                                    Text("150% (1.5x)", fontSize = 11.sp, color = secondaryText)
                                    Text("200% (2.0x)", fontSize = 11.sp, color = secondaryText)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                    }

                    SettingsCategoryTab.SUBTITLES -> {
                        SubtitlesTab(
                            settings = uiState.playerSettings,
                            isDark = isDark,
                            context = context,
                            fontListVersion = fontListVersion,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            alphaFactor = alphaFactor,
                            onSettingsChange = onPlayerSettingsChange,
                            onChooseFontDirectory = { fontDirectoryPicker.launch(null) },
                            onPickFontFiles = {
                                fontFilesPicker.launch(arrayOf("font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf", "*/*"))
                            },
                            onReloadFonts = {
                                val uriString = uiState.playerSettings.subtitleFontDirectoryUri
                                if (uriString.isNotBlank()) {
                                    try {
                                        val count = SubtitleFontManager.importFontsFromTree(context, Uri.parse(uriString))
                                        fontListVersion++
                                        onPlayerSettingsChange(uiState.playerSettings.copy(subtitleFontsReloadNonce = System.currentTimeMillis()))
                                        Toast.makeText(context, "Reloaded $count subtitle font(s)", Toast.LENGTH_SHORT).show()
                                    } catch (_: Throwable) {
                                        Toast.makeText(context, "Unable to reload fonts", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    fontListVersion++
                                    SubtitleFontManager.syncFonts(context)
                                    onPlayerSettingsChange(uiState.playerSettings.copy(subtitleFontsReloadNonce = System.currentTimeMillis()))
                                    Toast.makeText(context, "Fonts refreshed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteFont = { fontName ->
                                val deleted = SubtitleFontManager.deleteCustomFont(context, fontName)
                                if (deleted) {
                                    fontListVersion++
                                    val newSelected = if (uiState.playerSettings.selectedSubtitleFont.equals(fontName, ignoreCase = true) ||
                                        uiState.playerSettings.selectedSubtitleFont.equals(fontName.substringBeforeLast('.'), ignoreCase = true)) {
                                        ""
                                    } else uiState.playerSettings.selectedSubtitleFont
                                    onPlayerSettingsChange(
                                        uiState.playerSettings.copy(
                                            selectedSubtitleFont = newSelected,
                                            subtitleFontsReloadNonce = System.currentTimeMillis()
                                        )
                                    )
                                    Toast.makeText(context, "Removed font: $fontName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteAllFonts = {
                                val count = SubtitleFontManager.deleteAllCustomFonts(context)
                                fontListVersion++
                                onPlayerSettingsChange(
                                    uiState.playerSettings.copy(
                                        selectedSubtitleFont = "",
                                        subtitleFontsReloadNonce = System.currentTimeMillis()
                                    )
                                )
                                Toast.makeText(context, "Removed all $count custom font(s)", Toast.LENGTH_SHORT).show()
                            },
                            lang = lang,
                            onOpenFolderVideoSelector = { showSubtitleFolderSelectorSheet = true }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    SettingsCategoryTab.DECODER -> {
                        SettingsChunk {

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = AppStrings.getSectionHwAccel(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HwAccelMode.entries.forEach { mode ->
                                val isSelected = uiState.hwAccelMode == mode
                                val icon = when (mode) {
                                    HwAccelMode.FORCE -> Icons.Outlined.Speed
                                    HwAccelMode.PREFER -> Icons.Outlined.Memory
                                    HwAccelMode.DISABLE -> Icons.Outlined.Tune
                                }
                                val cardShape = RoundedCornerShape(20.dp)
                                val cardBackground = if (isSelected) {
                                    if (isDark) AccentContainerDark.copy(alpha = 0.50f) else AccentContainerLight
                                } else {
                                    if (isDark) Color(0x1AFFFFFF) else Neutral.F1F5F9
                                }
                                val cardBorder = if (isSelected) {
                                    if (isDark) AccentSkyBlueStrong else AccentSkyBlue
                                } else {
                                    if (isDark) Color(0x22FFFFFF) else Neutral.E2E8F0
                                }
                                val iconTint = if (isSelected) {
                                    if (isDark) AccentSkyBlue else AccentSkyBlueStrong
                                } else {
                                    secondaryText
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(cardShape)
                                        .background(cardBackground)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = cardBorder,
                                            shape = cardShape
                                        )
                                        .clickable { onHwAccelModeChange(mode) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StyledIcon(
                                            imageVector = icon,
                                            contentDescription = mode.getDisplayName(lang),
                                            tint = iconTint,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = mode.getDisplayName(lang),
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = mode.getDescription(lang),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = secondaryText
                                            )
                                        }

                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) AccentSkyBlueStrong else AccentSkyBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StyledIcon(
                                                    imageVector = Icons.Outlined.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = AppStrings.getSectionRenderEngine(lang),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                EngineFeatureRow(
                                    title = AppStrings.getRenderLibassTitle(lang),
                                    subtitle = AppStrings.getRenderLibassDesc(lang),
                                    activeBadgeText = AppStrings.getActiveBadge(lang),
                                    active = true
                                )
                                HorizontalDivider(color = borderColor)
                                EngineFeatureRow(
                                    title = AppStrings.getRenderEmbeddedFontsTitle(lang),
                                    subtitle = AppStrings.getRenderEmbeddedFontsDesc(lang),
                                    activeBadgeText = AppStrings.getActiveBadge(lang),
                                    active = true
                                )
                                HorizontalDivider(color = borderColor)
                                EngineFeatureRow(
                                    title = AppStrings.getRenderHwEqTitle(lang),
                                    subtitle = AppStrings.getRenderHwEqDesc(lang),
                                    activeBadgeText = AppStrings.getActiveBadge(lang),
                                    active = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                    }

                    SettingsCategoryTab.GESTURES -> {

                        Spacer(modifier = Modifier.height(16.dp))

                        GestureSettingsPanel(
                            settings = uiState.playerSettings,
                            isDark = isDark,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            alphaFactor = alphaFactor,
                            onSettingsChange = onPlayerSettingsChange
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    SettingsCategoryTab.PLAYER_LAYOUT -> {
                        SettingsChunk {

                        Spacer(modifier = Modifier.height(16.dp))

                        // SECTION 1: CONTROL LAYOUT CUSTOMIZER
                        Text(
                            text = "CONTROL LAYOUT CUSTOMIZER",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ControlLayoutEditor(
                            config = uiState.playerLayoutConfig,
                            onConfigChange = onPlayerLayoutConfigChange,
                            onResetToDefault = onResetPlayerLayoutConfig,
                            isDark = isDark,
                            transparencyPercent = uiState.glassBlurTransparency
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // SECTION 2: SEEKBAR STYLE
                        Text(
                            text = AppStrings.getSeekbarStyleTitle(lang).uppercase(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        SeekbarStyleSelectionCard(
                            currentStyle = uiState.seekbarStyle,
                            onSelectStyle = { onSeekbarStyleChange(it) },
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PlayerSettingsCategory("Playback Buttons", cardBg, borderColor, secondaryText) {
                            // 1. Portrait playback buttons
                            PlayerChoiceRow(
                                title = "Portrait playback buttons",
                                description = "Position of play/pause and skip buttons in portrait orientation",
                                value = uiState.playerSettings.portraitPlaybackButtonsPosition,
                                options = listOf(
                                    "CENTER" to "Center of screen",
                                    "BOTTOM" to "Between seekbar and controls"
                                ),
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                borderColor = borderColor,
                                alphaFactor = alphaFactor
                            ) { v ->
                                onPlayerSettingsChange(uiState.playerSettings.copy(portraitPlaybackButtonsPosition = v))
                            }

                            HorizontalDivider(color = borderColor)

                            // 2. Landscape playback buttons
                            PlayerChoiceRow(
                                title = "Landscape playback buttons",
                                description = "Position of play/pause and skip buttons in landscape orientation",
                                value = uiState.playerSettings.landscapePlaybackButtonsPosition,
                                options = listOf(
                                    "CENTER" to "Center of screen",
                                    "BOTTOM" to "Between seekbar and controls"
                                ),
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                borderColor = borderColor,
                                alphaFactor = alphaFactor
                            ) { v ->
                                onPlayerSettingsChange(uiState.playerSettings.copy(landscapePlaybackButtonsPosition = v))
                            }

                            HorizontalDivider(color = borderColor)

                            // 3. Hide player buttons background
                            PlayerSwitchRow(
                                title = "Hide player buttons background",
                                description = "Hide the background of all player control buttons",
                                checked = uiState.playerSettings.hidePlayerButtonsBackground,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            ) { checked ->
                                onPlayerSettingsChange(uiState.playerSettings.copy(hidePlayerButtonsBackground = checked))
                            }

                            HorizontalDivider(color = borderColor)

                            // 4. Always use dark player button backgrounds
                            PlayerSwitchRow(
                                title = "Always use dark player button backgrounds",
                                description = "Keep player control button backgrounds dark in light, dark, and automatic themes",
                                checked = uiState.playerSettings.alwaysDarkPlayerButtonBackground,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            ) { checked ->
                                onPlayerSettingsChange(uiState.playerSettings.copy(alwaysDarkPlayerButtonBackground = checked))
                            }

                            HorizontalDivider(color = borderColor)

                            // 5. Hide player control time
                            PlayerSliderRow(
                                title = "Hide player control time",
                                description = "Inactivity timeout before video controls automatically hide",
                                value = uiState.playerSettings.hidePlayerControlsTimeoutMs,
                                min = 500,
                                max = 10000,
                                unit = " ms",
                                presets = listOf(1000, 2000, 3000, 4000, 5000, 7500, 10000),
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            ) { v ->
                                onPlayerSettingsChange(uiState.playerSettings.copy(hidePlayerControlsTimeoutMs = v))
                            }

                            HorizontalDivider(color = borderColor)

                            // 6. Time + Network clock
                            PlayerChoiceRow(
                                title = "Time + Network clock",
                                description = "Format of top header clock and status indicators",
                                value = uiState.playerSettings.timeNetworkClockFormat,
                                options = listOf(
                                    "NONE" to "None",
                                    "SYSTEM" to "System",
                                    "12_HOUR" to "12 hour",
                                    "24_HOUR" to "24 hour"
                                ),
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                borderColor = borderColor,
                                alphaFactor = alphaFactor
                            ) { v ->
                                onPlayerSettingsChange(uiState.playerSettings.copy(timeNetworkClockFormat = v))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        } // end SettingsChunk
                    }

                    SettingsCategoryTab.NETWORK -> {

                        Spacer(modifier = Modifier.height(16.dp))

                        NetworkSettingsPanel(
                            isDark = isDark,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            alphaFactor = alphaFactor
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    SettingsCategoryTab.ADVANCED -> {

                        Spacer(modifier = Modifier.height(16.dp))

                        AdvancedSettingsPanel(
                            uiState = uiState,
                            viewModel = viewModel,
                            isDark = isDark,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            alphaFactor = alphaFactor,
                            lang = lang
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    SettingsCategoryTab.ABOUT -> {

                        Spacer(modifier = Modifier.height(16.dp))

                        AboutSettingsPanel(
                            uiState = uiState,
                            isDark = isDark,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            alphaFactor = alphaFactor,
                            lang = lang
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Seamless bottom navigation bar spacing:
                // Ensures sheet background bleeds completely under the gesture pill without visual cut-off
                Spacer(modifier = Modifier.navigationBarsPadding())
                // Extra bottom scroll buffer so the last settings item can fully scroll
                // into view above the nav bar on smaller-height screens instead of
                // getting clipped at the bottom edge of the sheet.
                Spacer(modifier = Modifier.height(48.dp))
            }
                        }  // end settings page column
            }
                }  // end tab row + content column
                SettingsSearchOverlay(
                    visible = searchOpen,
                    lang = lang,
                    background = glassPanelColor(isDark, 100f).copy(alpha = 1f),
                    cardBg = cardBg,
                    borderColor = borderColor,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClose = { searchOpen = false },
                    onOpenResult = openSearchResult
                )
                }  // end tabs + search box
        }
    }
}

    // Modal popup for folder and video selection
    if (showAudioFolderSelectorSheet) {
        AudioFolderVideoSelectorSheet(
            initialSelectedFolders = uiState.selectedAudioFolders,
            initialSelectedVideos = uiState.selectedAudioVideos,
            onSaveSelection = { folders, videos ->
                onSelectedAudioFoldersChange(folders)
                onSelectedAudioVideosChange(videos)
            },
            onDismiss = { showAudioFolderSelectorSheet = false },
            isDark = isDark,
            appScale = uiState.appScale,
            glassBlurTransparency = uiState.glassBlurTransparency,
            lang = lang
        )
    }

    if (showSubtitleFolderSelectorSheet) {
        AudioFolderVideoSelectorSheet(
            initialSelectedFolders = uiState.playerSettings.selectedSubtitleFolders,
            initialSelectedVideos = uiState.playerSettings.selectedSubtitleVideos,
            onSaveSelection = { folders, videos ->
                onPlayerSettingsChange(
                    uiState.playerSettings.copy(
                        selectedSubtitleFolders = folders,
                        selectedSubtitleVideos = videos
                    )
                )
            },
            onDismiss = { showSubtitleFolderSelectorSheet = false },
            isDark = isDark,
            appScale = uiState.appScale,
            glassBlurTransparency = uiState.glassBlurTransparency,
            lang = lang,
            sheetTitle = "Subtitle Filter Folders & Videos"
        )
    }
}
}
}

/**
 * Splits a very large settings page into separate compiled lambdas so a single
 * generated method never exceeds the JVM 64 KB method-size limit ("Method too large").
 */
@Composable
private fun ColumnScope.SettingsChunk(content: @Composable ColumnScope.() -> Unit) {
    content()
}

@Composable
private fun EngineFeatureRow(
    title: String,
    subtitle: String,
    activeBadgeText: String = "Active",
    active: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = DarkTextPrimary)
            Text(text = subtitle, fontSize = 11.5.sp, color = DarkTextSecondary)
        }
        if (active) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StyledIcon(
                                imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
