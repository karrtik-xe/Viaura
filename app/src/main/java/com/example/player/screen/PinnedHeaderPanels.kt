package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.components.LayoutMode
import com.example.ui.components.OnlineVideoPanel
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SortAndViewOptionsPopup
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.StyledIcon
import com.example.ui.components.ViewMode
import com.example.ui.components.liquidSearchSurface
import androidx.compose.ui.graphics.Color
import com.example.ui.state.AppViewModel
import com.example.ui.state.SettingsPreferencesManager
import com.example.ui.state.UiState
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.OnlineChannelResult

/**
 * Pinned video layout: Search / Sort & View / Settings of the header open INSIDE the player (the video keeps playing where it is,
 * no mini player). Kept in its own file on purpose: the player screen is huge and extra locals there upset R8 (VerifyError).
 */
object PinnedHeaderUi {
    /** Height the search bar takes under the header: the pinned video card is pushed down by it. */
    var searchExtra by mutableStateOf(0.dp)
    var searchOpen by mutableStateOf(false)
    var showSort by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var query by mutableStateOf("")

    fun reset() {
        searchExtra = 0.dp
        searchOpen = false
        showSort = false
        showSettings = false
        query = ""
    }

    fun onHeaderAction(action: com.example.util.PlayerHeaderAction) {
        when (action) {
            com.example.util.PlayerHeaderAction.SEARCH -> {
                searchOpen = !searchOpen
                if (!searchOpen) query = ""
            }
            com.example.util.PlayerHeaderAction.SORT -> showSort = true
            com.example.util.PlayerHeaderAction.SETTINGS -> showSettings = true
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Search bar under the header + (when something is typed) the online search results in place of the details / recommended list. */
@Composable
internal fun BoxScope.PinnedSearchLayer(
    isDark: Boolean,
    uiState: UiState,
    chScale: Float,
    chHeaderBottom: Dp,
    chHPad: Dp,
    chCardH: Dp,
    pageBg: androidx.compose.ui.graphics.Color,
    onPlayVideo: (VideoItem) -> Unit,
    onOpenChannel: (OnlineChannelResult) -> Unit
) {
    val context = LocalContext.current
    val ui = PinnedHeaderUi
    val barH = 46.dp * chScale
    val extra = if (ui.searchOpen) barH + 8.dp else 0.dp
    SideEffect { ui.searchExtra = extra }
    DisposableEffect(Unit) { onDispose { ui.reset() } }
    BackHandler(enabled = ui.searchOpen) {
        ui.searchOpen = false
        ui.query = ""
    }

    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val shape = RoundedCornerShape(18.dp)
    val focus = remember { FocusRequester() }
    LaunchedEffect(ui.searchOpen) {
        if (ui.searchOpen) {
            try { focus.requestFocus() } catch (_: Throwable) {}
        }
    }

    if (ui.searchOpen) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = chHeaderBottom, start = chHPad, end = chHPad)
                .fillMaxWidth()
                .height(barH)
                .liquidSearchSurface(shape, isDark, if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f), primary.copy(alpha = 0.12f), uiState.glassBlurTransparency)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StyledIcon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = AccentSkyBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (ui.query.isEmpty()) {
                        Text("Search videos, channels online...", fontSize = 13.sp, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BasicTextField(
                        value = ui.query,
                        onValueChange = { ui.query = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = com.example.ui.theme.LocalAppFontFamily.current,
                            color = primary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(AccentSkyBlue),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus)
                    )
                }
                if (ui.query.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.08f))
                            .clickable { ui.query = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(imageVector = Icons.Outlined.Close, contentDescription = "Clear", tint = primary.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    // Sort & View popup (centre of the screen, same popup as everywhere else) - settings of the Online Videos folder
    if (ui.showSort) {
        var sortField by remember {
            val loaded = SettingsPreferencesManager.loadSortField(context, "online_video")
            mutableStateOf(if (loaded == SortField.DATE || loaded == SortField.TITLE) loaded else SortField.DATE)
        }
        var sortDirection by remember { mutableStateOf(SettingsPreferencesManager.loadSortDirection(context, "online_video")) }
        var layoutMode by remember { mutableStateOf(SettingsPreferencesManager.loadLayoutMode(context, "online_video")) }
        var visibleFields by remember {
            mutableStateOf(SettingsPreferencesManager.sanitizeOnlineVideoFields(SettingsPreferencesManager.loadVisibleFields(context, "online_video")))
        }
        SortAndViewOptionsPopup(
            isDark = isDark,
            sortField = sortField,
            sortDirection = sortDirection,
            viewMode = ViewMode.FOLDER,
            layoutMode = layoutMode,
            visibleFields = visibleFields,
            isInsideFolder = true,
            isMusicMode = false,
            isOnlineMode = true,
            isOnlineVideoFolder = true,
            appScale = uiState.appScale,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onSortFieldChange = {
                sortField = it
                com.example.ui.components.OnlineVideoFeedState.sortTouched = true
                SettingsPreferencesManager.saveSortField(context, it, "online_video")
            },
            onSortDirectionChange = {
                sortDirection = it
                com.example.ui.components.OnlineVideoFeedState.sortTouched = true
                SettingsPreferencesManager.saveSortDirection(context, it, "online_video")
            },
            onViewModeChange = {},
            onLayoutModeChange = {
                layoutMode = it
                SettingsPreferencesManager.saveLayoutMode(context, it, "online_video")
            },
            onVisibleFieldsChange = {
                val s = SettingsPreferencesManager.sanitizeOnlineVideoFields(it)
                visibleFields = s
                SettingsPreferencesManager.saveVisibleFields(context, s, "online_video")
            },
            onDismiss = { ui.showSort = false }
        )
    }

    // Settings dashboard (slides up from the bottom, same sheet as the Home header)
    if (ui.showSettings) {
        val viewModel: AppViewModel? = remember {
            (context.findActivity() as? ComponentActivity)?.let { ViewModelProvider(it)[AppViewModel::class.java] }
        }
        SettingsSheet(
            uiState = uiState,
            onDismiss = { ui.showSettings = false },
                    onThemeChange = { viewModel?.setThemeMode(it) },
                    onScaleChange = { viewModel?.setAppScale(it) },
                    onGlassBlurTransparencyChange = { viewModel?.setGlassBlurTransparency(it) },
                    onSideBySideChange = { viewModel?.setForceSideBySide(it) },
                    onLanguageChange = { viewModel?.setLanguage(it) },
                    onFrameStepChange = { viewModel?.setFrameStepAmount(it) },
                    onCopyTimestampChange = { viewModel?.setCopyTimestampOnDoubleTap(it) },
                    onHwAccelModeChange = { viewModel?.setHwAccelMode(it) },
                    onGestureSensitivityModeChange = { mode -> viewModel?.setGestureSensitivityMode(mode) },
                    onShowPlayerNotificationsChange = { show ->
                        viewModel?.setShowPlayerNotifications(show)
                    },
                    onSeekbarStyleChange = { style ->
                        viewModel?.setSeekbarStyle(style)
                    },
                    onPlayerLayoutConfigChange = { config ->
                        viewModel?.setPlayerLayoutConfig(config)
                    },
                    onResetPlayerLayoutConfig = {
                        viewModel?.resetPlayerLayoutConfig()
                    },
                    onPreferredAudioLanguagesChange = { langs ->
                        viewModel?.setPreferredAudioLanguages(langs)
                    },
                    onApplyAudioLanguageToSelectedContentOnlyChange = { apply ->
                        viewModel?.setApplyAudioLanguageToSelectedContentOnly(apply)
                    },
                    onSelectedAudioFoldersChange = { folders ->
                        viewModel?.setSelectedAudioFolders(folders)
                    },
                    onSelectedAudioVideosChange = { videos ->
                        viewModel?.setSelectedAudioVideos(videos)
                    },
                    onEnableAudioPitchCorrectionChange = { enabled ->
                        viewModel?.setEnableAudioPitchCorrection(enabled)
                    },
                    onVolumeNormalizationChange = { norm ->
                        viewModel?.setVolumeNormalization(norm)
                    },
                    onBackgroundPlaybackChange = { bg ->
                        viewModel?.setBackgroundPlayback(bg)
                    },
                    onAudioChannelModeChange = { mode ->
                        viewModel?.setAudioChannelMode(mode)
                    },
                    onVolumeBoostCapChange = { cap ->
                        viewModel?.setVolumeBoostCap(cap)
                    },
                    onRememberSelectedAudioTrackChange = { rem ->
                        viewModel?.setRememberSelectedAudioTrack(rem)
                    },
                    onPlayerSettingsChange = { settings ->
                        viewModel?.setPlayerSettings(settings)
                    },
                    // Appearance / File Browser / Thumbnails / Navigation / Animations
                    // are all persisted through the same AppViewModel state.
                    onAppThemeChange = { viewModel?.setAppTheme(it) },
                    onCustomThemeGradientChange = { id, cols -> viewModel?.setCustomThemeGradient(id, cols) },
                    onAmoledBlackModeChange = { viewModel?.setAmoledBlackMode(it) },
                    onLiquidGlassModeChange = { viewModel?.setLiquidGlassMode(it) },
                    onLiquidGlassParamsChange = { viewModel?.setLiquidGlassParams(it) },
                    onLiquidGlassControlsChange = { viewModel?.setLiquidGlassControls(it) },
                    onUseSystemFontChange = { viewModel?.setUseSystemFont(it) },
                    onHapticFeedbackChange = { viewModel?.setHapticFeedback(it) },
                    onShowFullNameChange = { viewModel?.setShowFullName(it) },
                    onShowNewVideoLabelChange = { viewModel?.setShowNewVideoLabel(it) },
                    onNewVideoDaysThresholdChange = { viewModel?.setNewVideoDaysThreshold(it) },
                    onShowFolderUnplayedBadgeChange = { viewModel?.setShowFolderUnplayedBadge(it) },
                    onAutoScrollToLastPlayedChange = { viewModel?.setAutoScrollToLastPlayed(it) },
                    onTreePathCompressionChange = { viewModel?.setTreePathCompression(it) },
                    onDualPaneViewChange = { viewModel?.setDualPaneView(it) },
                    onWatchedThresholdPercentChange = { viewModel?.setWatchedThresholdPercent(it) },
                    onShowVideoThumbnailsChange = { viewModel?.setShowVideoThumbnails(it) },
                    onThumbnailStrategyChange = { viewModel?.setThumbnailStrategy(it) },
                    onThumbnailQualityChange = { viewModel?.setThumbnailQuality(it) },
                    onTapThumbnailToSelectChange = { viewModel?.setTapThumbnailToSelect(it) },
                    onShowNetworkThumbnailsChange = { viewModel?.setShowNetworkThumbnails(it) },
                    onShowHomeTabChange = { viewModel?.setShowHomeTab(it) },
                    onShowMusicTabChange = { viewModel?.setShowMusicTab(it) },
                    onShowRecentsTabChange = { viewModel?.setShowRecentsTab(it) },
                    onShowPlaylistsTabChange = { viewModel?.setShowPlaylistsTab(it) },
                    onShowNetworkTabChange = { viewModel?.setShowNetworkTab(it) },
                    onShowDownloadsTabChange = { viewModel?.setShowDownloadsTab(it) },
                    onControlsAnimationStyleChange = { viewModel?.setControlsAnimationStyle(it) },
                    onVideoOpeningAnimationChange = { viewModel?.setVideoOpeningAnimation(it) },
                    onScreenNavigationStyleChange = { viewModel?.setScreenNavigationStyle(it) },
                    onTabNavigationStyleChange = { viewModel?.setTabNavigationStyle(it) },
                    onAnimationSpeedChange = { viewModel?.setAnimationSpeed(it) },
            viewModel = viewModel
        )
    }

    // Search results: where the details / recommended list normally is. Only while something is typed.
    if (ui.searchOpen && ui.query.isNotBlank()) {
        val layoutMode = remember { SettingsPreferencesManager.loadLayoutMode(context, "online_video") }
        val fields = remember { SettingsPreferencesManager.sanitizeOnlineVideoFields(SettingsPreferencesManager.loadVisibleFields(context, "online_video")) }
        val sf = remember {
            val l = SettingsPreferencesManager.loadSortField(context, "online_video")
            if (l == SortField.DATE || l == SortField.TITLE) l else SortField.DATE
        }
        val sd = remember { SettingsPreferencesManager.loadSortDirection(context, "online_video") }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .padding(top = chHeaderBottom + extra + chCardH + 8.dp)
                .background(pageBg)
                .padding(start = chHPad, end = chHPad)
        ) {
            OnlineVideoPanel(
                isDark = isDark,
                searchQuery = ui.query,
                layoutMode = layoutMode,
                visibleFields = fields,
                sortField = sf,
                sortDirection = sd,
                onPlayVideo = { v, _ ->
                    ui.searchOpen = false
                    ui.query = ""
                    onPlayVideo(v)
                },
                onOpenChannel = onOpenChannel,
                glassBlurTransparency = uiState.glassBlurTransparency
            )
        }
    }
}
