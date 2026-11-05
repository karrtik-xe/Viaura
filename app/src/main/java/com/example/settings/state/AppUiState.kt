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

data class UiState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val systemTone: SystemTone? = null,
    val appScale: Float = 75f, // 1f (maps to 85% min size) to 100f (maps to 100% full size)
    val glassBlurTransparency: Float = 85f, // 10f to 100f (controls frosted glass blur opacity & translucency)
    val forceSideBySide: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val frameStepAmount: Int = 1, // 1 to 30 frames
    val copyTimestampOnDoubleTap: Boolean = true,
    val hwAccelMode: HwAccelMode = HwAccelMode.FORCE,
    val gestureSensitivityMode: GestureSensitivityMode = GestureSensitivityMode.EXPONENTIAL,
    val editorTextSizeSp: Float = 14f, // 10f to 28f
    val timeTextSizeSp: Float = 13.5f, // 10f to 24f
    val showPasteBoxConverter: Boolean = true,
    val showPasteBoxCleaner: Boolean = true,
    val showPasteBoxShifter: Boolean = true,
    val showPasteBoxEditor: Boolean = true,
    val showPasteBoxTextEditor: Boolean = true,
    val dialogueEditOffsetY: Float = 0f, // -300f to 300f dp
    val colorPickerOffsetY: Float = 0f,  // -300f to 300f dp
    val showUploadSectionTextEditor: Boolean = true,
    val textEditorFontSizeSp: Float = 14f, // 10f to 28f
    val showLineNumbersTextEditor: Boolean = true,
    val textEditorWordWrap: Boolean = true,
    val showBottomToolbarTextEditor: Boolean = true,
    val showSearchPanelTextEditor: Boolean = true,
    val textEditorNavStep: TextEditorNavStep = TextEditorNavStep.CHARACTER,
    val showPlayerNotifications: Boolean = true,
    val seekbarStyle: SeekbarStyle = SeekbarStyle.THICK,
    val playerLayoutConfig: PlayerLayoutConfig = PlayerLayoutConfig.default,
    val playerSettings: PlayerSettings = PlayerSettings(),

    // Appearance
    val appTheme: AppThemeVariant = AppThemeVariant.DYNAMIC,
    // User edited gradients (theme id -> 3 colours). DYNAMIC is never editable.
    val customThemeGradients: Map<String, List<Long>> = emptyMap(),
    val amoledBlackMode: Boolean = false,
    val liquidGlassMode: Boolean = true,
    // Liquid Glass options (see LiquidGlassParams)
    val liquidGlassParams: com.example.ui.components.LiquidGlassParams = com.example.ui.components.LiquidGlassParams(),
    // Liquid Glass sliders (seek bars, settings sliders) and ON / OFF switches (only used while liquidGlassMode is ON)
    val liquidGlassControls: Boolean = true,
    val useSystemFont: Boolean = false,
    val hapticFeedback: Boolean = true,

    // File Browser
    val showFullName: Boolean = false,
    val showNewVideoLabel: Boolean = true,
    val newVideoDaysThreshold: Int = 7,
    val showFolderUnplayedBadge: Boolean = true,
    val autoScrollToLastPlayed: Boolean = true,
    val treePathCompression: Boolean = true,
    val dualPaneView: Boolean = false,
    val watchedThresholdPercent: Int = 95,

    // Thumbnails
    val showVideoThumbnails: Boolean = true,
    val thumbnailStrategy: ThumbnailStrategy = ThumbnailStrategy.SMART,
    val thumbnailQuality: ThumbnailQuality = ThumbnailQuality.HIGH,
    val thumbnailFallbackSecond: Int = 1,
    val tapThumbnailToSelect: Boolean = false,
    val showNetworkThumbnails: Boolean = false,

    // Navigation
    val showHomeTab: Boolean = true,
    val showMusicTab: Boolean = true,
    val showRecentsTab: Boolean = true,
    val showPlaylistsTab: Boolean = true,
    /** Optional "Network" bottom tab (play a pasted link). Off by default. */
    val showNetworkTab: Boolean = true,
    val showDownloadsTab: Boolean = true,

    // Animations
    val controlsAnimationStyle: ControlsAnimationStyle = ControlsAnimationStyle.DEFAULT,
    val videoOpeningAnimation: VideoOpeningAnimation = VideoOpeningAnimation.DEFAULT,
    val screenNavigationStyle: ScreenNavigationStyle = ScreenNavigationStyle.DEFAULT,
    val tabNavigationStyle: TabNavigationStyle = TabNavigationStyle.DEFAULT,
    val animationSpeed: Float = 1.0f,

    // Audio & Sound System Preferences
    val preferredAudioLanguages: String = "",
    val applyAudioLanguageToSelectedContentOnly: Boolean = false,
    val selectedAudioFolders: Set<String> = emptySet(),
    val selectedAudioVideos: Set<String> = emptySet(),
    val enableAudioPitchCorrection: Boolean = true,
    val volumeNormalization: Boolean = false,
    val backgroundPlayback: Boolean = false,
    val audioChannelMode: AudioChannelMode = AudioChannelMode.AUTO,
    val volumeBoostCap: Int = 150,
    val rememberSelectedAudioTrack: Boolean = true
) {
    val isSystemFontEnabled: Boolean get() = useSystemFont
}

typealias AppConfig = SettingsPreferencesManager
