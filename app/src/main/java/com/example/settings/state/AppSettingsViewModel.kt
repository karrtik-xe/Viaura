package com.example.ui.state

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.example.player.AudioChannelMode
import com.example.player.AudioLanguageMatcher
import com.example.ui.components.LayoutMode
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.ViewMode
import com.example.ui.components.VisibleFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    init { com.example.ui.i18n.I18n.init(appContext) }
    private val _uiState = MutableStateFlow(SettingsPreferencesManager.loadSettings(appContext))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _hasMediaPermission = MutableStateFlow(com.example.ui.components.hasVideoPermission(application))
    val hasMediaPermission: StateFlow<Boolean> = _hasMediaPermission.asStateFlow()

    private val _mediaRefreshTrigger = MutableStateFlow(0L)
    val mediaRefreshTrigger: StateFlow<Long> = _mediaRefreshTrigger.asStateFlow()

    fun setHasMediaPermission(hasPermission: Boolean) {
        _hasMediaPermission.value = hasPermission
    }

    fun triggerLibraryRefresh() {
        _mediaRefreshTrigger.value = System.currentTimeMillis()
    }

    fun setThemeMode(mode: ThemeMode) {
        val newState = _uiState.value.copy(themeMode = mode)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    /** Muted colour for Appearance = System (null = follow the wallpaper). Does not touch the accent gradient. */
    fun setSystemTone(tone: SystemTone?) {
        val newState = _uiState.value.copy(systemTone = tone)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setAppScale(scale: Float) {
        val newState = _uiState.value.copy(appScale = scale.coerceIn(1f, 100f))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setGlassBlurTransparency(transparency: Float) {
        val newState = _uiState.value.copy(glassBlurTransparency = transparency.coerceIn(10f, 100f))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setForceSideBySide(enabled: Boolean) {
        val newState = _uiState.value.copy(forceSideBySide = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setLanguage(lang: AppLanguage) {
        val newState = _uiState.value.copy(language = lang)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setFrameStepAmount(amount: Int) {
        val newState = _uiState.value.copy(frameStepAmount = amount.coerceIn(1, 30))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setCopyTimestampOnDoubleTap(enabled: Boolean) {
        val newState = _uiState.value.copy(copyTimestampOnDoubleTap = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setHwAccelMode(mode: HwAccelMode) {
        val newState = _uiState.value.copy(hwAccelMode = mode)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setGestureSensitivityMode(mode: GestureSensitivityMode) {
        val newState = _uiState.value.copy(gestureSensitivityMode = mode)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setSeekbarStyle(style: SeekbarStyle) {
        val newState = _uiState.value.copy(seekbarStyle = style)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowPlayerNotifications(show: Boolean) {
        val newState = _uiState.value.copy(showPlayerNotifications = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setPlayerLayoutConfig(config: PlayerLayoutConfig) {
        val sanitized = config.sanitized()
        val newState = _uiState.value.copy(playerLayoutConfig = sanitized)
        _uiState.value = newState
        SettingsPreferencesManager.savePlayerLayoutConfig(appContext, sanitized)
    }

    fun resetPlayerLayoutConfig() {
        setPlayerLayoutConfig(PlayerLayoutConfig.default)
    }

    fun applyImportedState(newState: UiState) {
        _uiState.value = newState
        triggerLibraryRefresh()
    }

    fun setPlayerSettings(settings: PlayerSettings) {
        val newState = _uiState.value.copy(playerSettings = settings)
        _uiState.value = newState
        PlayerSettings.save(appContext, settings)
    }

    /**
     * Updates the in-memory [UiState.playerSettings] ONLY - no disk write. Callers (e.g. the
     * video player's subtitle style editor) that already persist [PlayerSettings] themselves,
     * on their own debounced/IO-dispatched schedule, must still call this on every change so
     * this ViewModel's cached copy never goes stale. Without it, [uiState] keeps serving the
     * OLD snapshot that was current when the player screen was entered; leaving the player and
     * re-entering (or any other screen that reads/re-saves [UiState.playerSettings] via
     * [setPlayerSettings] in the meantime) would then silently revert the subtitle changes,
     * because [setPlayerSettings] always writes whatever it is currently holding - stale or not
     * - back over the newer values already on disk. Cheap enough (a plain StateFlow assignment,
     * no I/O) to call on every keystroke, slider tick, or gesture frame.
     */
    fun syncPlayerSettingsInMemory(settings: PlayerSettings) {
        _uiState.value = _uiState.value.copy(playerSettings = settings)
    }
    // Audio & Sound Preferences Updaters
    fun setPreferredAudioLanguages(languages: String) {
        val newState = _uiState.value.copy(preferredAudioLanguages = languages)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setApplyAudioLanguageToSelectedContentOnly(enabled: Boolean) {
        val newState = _uiState.value.copy(applyAudioLanguageToSelectedContentOnly = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setSelectedAudioFolders(folders: Set<String>) {
        val newState = _uiState.value.copy(selectedAudioFolders = folders)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setSelectedAudioVideos(videos: Set<String>) {
        val newState = _uiState.value.copy(selectedAudioVideos = videos)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setEnableAudioPitchCorrection(enabled: Boolean) {
        val newState = _uiState.value.copy(enableAudioPitchCorrection = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setVolumeNormalization(enabled: Boolean) {
        val newState = _uiState.value.copy(volumeNormalization = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        val newState = _uiState.value.copy(backgroundPlayback = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setAudioChannelMode(mode: AudioChannelMode) {
        val newState = _uiState.value.copy(audioChannelMode = mode)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setVolumeBoostCap(cap: Int) {
        val newState = _uiState.value.copy(volumeBoostCap = cap.coerceIn(100, 200))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setRememberSelectedAudioTrack(enabled: Boolean) {
        val newState = _uiState.value.copy(rememberSelectedAudioTrack = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    // Appearance Setters
    fun setAppTheme(theme: AppThemeVariant) {
        val newState = _uiState.value.copy(appTheme = theme)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    /** Saves (or, with null, resets) the user edited gradient of one theme. DYNAMIC is ignored. */
    fun setCustomThemeGradient(themeId: String, colors: List<Long>?) {
        val map = LinkedHashMap(_uiState.value.customThemeGradients)
        if (colors == null || colors.size !in 2..3) map.remove(themeId) else map[themeId] = colors
        val newState = _uiState.value.copy(customThemeGradients = map)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setLiquidGlassMode(enabled: Boolean) {
        val newState = _uiState.value.copy(liquidGlassMode = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setLiquidGlassControls(enabled: Boolean) {
        val newState = _uiState.value.copy(liquidGlassControls = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setLiquidGlassParams(params: com.example.ui.components.LiquidGlassParams) {
        val newState = _uiState.value.copy(liquidGlassParams = params)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setAmoledBlackMode(enabled: Boolean) {
        val newState = _uiState.value.copy(amoledBlackMode = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setUseSystemFont(enabled: Boolean) {
        val newState = _uiState.value.copy(useSystemFont = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setHapticFeedback(enabled: Boolean) {
        com.example.util.AppHaptics.isEnabled = enabled
        val newState = _uiState.value.copy(hapticFeedback = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    // File Browser Setters
    fun setShowFullName(show: Boolean) {
        val newState = _uiState.value.copy(showFullName = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowNewVideoLabel(show: Boolean) {
        val newState = _uiState.value.copy(showNewVideoLabel = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setNewVideoDaysThreshold(days: Int) {
        val newState = _uiState.value.copy(newVideoDaysThreshold = days.coerceIn(1, 30))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowFolderUnplayedBadge(show: Boolean) {
        val newState = _uiState.value.copy(showFolderUnplayedBadge = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setAutoScrollToLastPlayed(enabled: Boolean) {
        val newState = _uiState.value.copy(autoScrollToLastPlayed = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setTreePathCompression(enabled: Boolean) {
        val newState = _uiState.value.copy(treePathCompression = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setDualPaneView(enabled: Boolean) {
        val newState = _uiState.value.copy(dualPaneView = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setWatchedThresholdPercent(percent: Int) {
        val newState = _uiState.value.copy(watchedThresholdPercent = percent.coerceIn(50, 100))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    // Thumbnails Setters
    fun setShowVideoThumbnails(show: Boolean) {
        val newState = _uiState.value.copy(showVideoThumbnails = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setThumbnailStrategy(strategy: ThumbnailStrategy) {
        val newState = _uiState.value.copy(thumbnailStrategy = strategy)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setThumbnailQuality(quality: ThumbnailQuality) {
        val newState = _uiState.value.copy(thumbnailQuality = quality)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setTapThumbnailToSelect(enabled: Boolean) {
        val newState = _uiState.value.copy(tapThumbnailToSelect = enabled)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowNetworkThumbnails(show: Boolean) {
        val newState = _uiState.value.copy(showNetworkThumbnails = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    // Navigation Setters
    fun setShowHomeTab(show: Boolean) {
        val newState = _uiState.value.copy(showHomeTab = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowMusicTab(show: Boolean) {
        val newState = _uiState.value.copy(showMusicTab = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowRecentsTab(show: Boolean) {
        val newState = _uiState.value.copy(showRecentsTab = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowNetworkTab(show: Boolean) {
        val newState = _uiState.value.copy(showNetworkTab = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowDownloadsTab(show: Boolean) {
        val newState = _uiState.value.copy(showDownloadsTab = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setShowPlaylistsTab(show: Boolean) {
        val newState = _uiState.value.copy(showPlaylistsTab = show)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    // Animations Setters
    fun setControlsAnimationStyle(style: ControlsAnimationStyle) {
        val newState = _uiState.value.copy(controlsAnimationStyle = style)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setVideoOpeningAnimation(anim: VideoOpeningAnimation) {
        val newState = _uiState.value.copy(videoOpeningAnimation = anim)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setScreenNavigationStyle(style: ScreenNavigationStyle) {
        val newState = _uiState.value.copy(screenNavigationStyle = style)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setTabNavigationStyle(style: TabNavigationStyle) {
        val newState = _uiState.value.copy(tabNavigationStyle = style)
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    fun setAnimationSpeed(speed: Float) {
        val newState = _uiState.value.copy(animationSpeed = speed.coerceIn(0.25f, 2.0f))
        _uiState.value = newState
        SettingsPreferencesManager.saveSettings(appContext, newState)
    }

    // Active player & playback state preserved across rotation
    var activeVideoItem: com.example.ui.screens.VideoItem? = null
    var activeVideoPlaylist: List<com.example.ui.screens.VideoItem> = emptyList()
    var isPlayerInLandscape: Boolean = false
    val currentScreenState = MutableStateFlow(com.example.Screen.HOME)

    // Which screen to return to when the player is closed. Defaults to HOME, but is set
    // to CHANNEL_DETAIL when a video was opened from inside a channel's video list, so
    // Back correctly returns to that channel instead of always jumping to Home.
    private var screenBeforePlayer: com.example.Screen = com.example.Screen.HOME

    // Channel currently shown on the Channel Detail screen (set by openChannel()).
    var activeChannelResult: com.example.util.OnlineChannelResult? = null
    var isChannelAudioMode: Boolean = false

    // Shared playlist currently shown on the Shared Playlist screen (set by openSharedPlaylist()).
    var activeSharedPlaylist: com.example.util.OnlinePlaylistDetail? = null

    // Folder navigation stack preserved across playing videos and screen switches
    val folderNavigationStack = mutableListOf<com.example.ui.screens.VideoFolder>()

    // Last bottom tab and opened playlist of the Home screen, so Back from the player returns to the same place.
    var lastBottomTabName: String? = null
    /** Keeps the Downloads sub-page when returning from the video player. */
    var isDownloadsPageOpen: Boolean = false

    // The top search bar (text + open state) is kept while a video / song is opened and closed again, so the search list that was
    // on screen is still there on Back. [searchBeforeTitle]: the text that was in the bar when a provider's title page opened.
    var homeSearchQuery: String = ""
    var homeSearchActive: Boolean = false
    var searchBeforeTitle: String? = null
    var lastTitlePageKey: String = "|"
    var lastPlaylistId: String? = null

    // Home list scroll memory. HomeScreen leaves composition while the video player is open, so its remembered scroll
    // states and the "already auto-scrolled" key used to be lost: Back rebuilt the list at the top and then scrolled to the
    // last played video again. They live here instead, so Back shows the list exactly where the user left it and the
    // last-played auto-scroll only runs when a list / folder is first opened.
    var homeAutoScrolledKey: String? = null
    var homeLibraryScrollIndex: Int = 0
    var homeLibraryScrollOffset: Int = 0
    var homeFolderScrollFolderId: String? = null
    var homeFolderListScrollIndex: Int = 0
    var homeFolderListScrollOffset: Int = 0
    var homeFolderGridScrollIndex: Int = 0
    var homeFolderGridScrollOffset: Int = 0

    fun startPlaying(video: com.example.ui.screens.VideoItem, playlist: List<com.example.ui.screens.VideoItem>) {
        // Network-tab links go into "Recent Streams", so a stream closed by mistake can be reopened.
        if (video.isNetworkStream) {
            try {
                com.example.util.RecentStreamStore.record(
                    getApplication<android.app.Application>(), video.path, video.displayName,
                    video.channelName,
                    video.thumbnailUrl
                )
            } catch (_: Throwable) {}
        }
        activeVideoItem = video
        activeVideoPlaylist = playlist
        screenBeforePlayer = currentScreenState.value.takeIf { it != com.example.Screen.PLAYER }
            ?: com.example.Screen.HOME
        currentScreenState.value = com.example.Screen.PLAYER
    }

    fun openChannel(channel: com.example.util.OnlineChannelResult, isAudioMode: Boolean = false) {
        activeChannelResult = channel
        isChannelAudioMode = isAudioMode
        currentScreenState.value = com.example.Screen.CHANNEL_DETAIL
    }

    fun exitChannel() {
        currentScreenState.value = com.example.Screen.HOME
    }

    fun openSharedPlaylist(playlist: com.example.util.OnlinePlaylistDetail) {
        activeSharedPlaylist = playlist
        currentScreenState.value = com.example.Screen.SHARED_PLAYLIST
    }

    fun exitSharedPlaylist() {
        currentScreenState.value = com.example.Screen.HOME
    }

    fun exitPlayer() {
        // Return to wherever the player was actually opened from (Home or Channel Detail)
        // instead of always forcing Home, so channel/search state survives Search Results
        // -> Channel Detail -> Video Player -> Back -> Channel Detail.
        currentScreenState.value = screenBeforePlayer
        com.example.util.AppStorageManager.performBackgroundCleanup(appContext)
    }
}
