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

object SettingsPreferencesManager {
    private const val PREF_NAME = "video_player_settings_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_SYSTEM_TONE = "key_system_tone"
    private const val KEY_APP_SCALE = "key_app_scale"
    private const val KEY_GLASS_BLUR_TRANSPARENCY = "key_glass_blur_transparency"
    private const val KEY_FORCE_SIDE_BY_SIDE = "key_force_side_by_side"
    private const val KEY_LANGUAGE = "key_language"
    private const val KEY_FRAME_STEP_AMOUNT = "key_frame_step_amount"
    private const val KEY_COPY_TIMESTAMP = "key_copy_timestamp"
    private const val KEY_HW_ACCEL_MODE = "key_hw_accel_mode"
    private const val KEY_GESTURE_SENSITIVITY_MODE = "key_gesture_sensitivity_mode"
    private const val KEY_SHOW_PLAYER_NOTIFICATIONS = "key_show_player_notifications"
    private const val KEY_SEEKBAR_STYLE = "key_seekbar_style"
    private const val KEY_TOP_RIGHT_CONTROLS = "key_top_right_controls"
    private const val KEY_BOTTOM_LEFT_CONTROLS = "key_bottom_left_controls"
    private const val KEY_BOTTOM_RIGHT_CONTROLS = "key_bottom_right_controls"
    private const val KEY_LANDSCAPE_TR_ENABLED = "key_landscape_tr_enabled"
    private const val KEY_LANDSCAPE_BL_ENABLED = "key_landscape_bl_enabled"
    private const val KEY_LANDSCAPE_BR_ENABLED = "key_landscape_br_enabled"
    private const val KEY_PORTRAIT_TOP_CONTROLS = "key_portrait_top_controls"
    private const val KEY_PORTRAIT_BOTTOM_CONTROLS = "key_portrait_bottom_controls"
    private const val KEY_PORTRAIT_TOP_ENABLED = "key_portrait_top_enabled"
    private const val KEY_PORTRAIT_BOTTOM_ENABLED = "key_portrait_bottom_enabled"
    private const val KEY_EDITOR_TEXT_SIZE = "key_editor_text_size"
    private const val KEY_TIME_TEXT_SIZE = "key_time_text_size"
    private const val KEY_PASTE_CONVERTER = "key_paste_converter"
    private const val KEY_PASTE_CLEANER = "key_paste_cleaner"
    private const val KEY_PASTE_SHIFTER = "key_paste_shifter"
    private const val KEY_PASTE_EDITOR = "key_paste_editor"
    private const val KEY_PASTE_TEXT_EDITOR = "key_paste_text_editor"
    private const val KEY_DIALOGUE_EDIT_OFFSET_Y = "key_dialogue_edit_offset_y"
    private const val KEY_COLOR_PICKER_OFFSET_Y = "key_color_picker_offset_y"
    private const val KEY_SHOW_UPLOAD_SECTION_TEXT_EDITOR = "key_show_upload_section_text_editor"
    private const val KEY_TEXT_EDITOR_FONT_SIZE = "key_text_editor_font_size"
    private const val KEY_SHOW_LINE_NUMBERS_TEXT_EDITOR = "key_show_line_numbers_text_editor"
    private const val KEY_TEXT_EDITOR_WORD_WRAP = "key_text_editor_word_wrap"
    private const val KEY_SHOW_BOTTOM_TOOLBAR_TEXT_EDITOR = "key_show_bottom_toolbar_text_editor"
    private const val KEY_SHOW_SEARCH_PANEL_TEXT_EDITOR = "key_show_search_panel_text_editor"
    private const val KEY_TEXT_EDITOR_NAV_STEP = "key_text_editor_nav_step"

    // Appearance Keys
    private const val KEY_APP_THEME = "key_app_theme"
    private const val KEY_CUSTOM_THEME_GRADIENTS = "key_custom_theme_gradients"
    private const val KEY_AMOLED_BLACK_MODE = "key_amoled_black_mode"
    private const val KEY_LIQUID_GLASS_MODE = "key_liquid_glass_mode"
    private const val KEY_LIQUID_GLASS_PARAMS = "key_liquid_glass_params"
    private const val KEY_LIQUID_GLASS_CONTROLS = "key_liquid_glass_controls"
    private const val KEY_USE_SYSTEM_FONT = "key_use_system_font"
    private const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"

    // File Browser Keys
    private const val KEY_SHOW_FULL_NAME = "key_show_full_name"
    private const val KEY_SHOW_NEW_VIDEO_LABEL = "key_show_new_video_label"
    private const val KEY_NEW_VIDEO_DAYS_THRESHOLD = "key_new_video_days_threshold"
    private const val KEY_SHOW_FOLDER_UNPLAYED_BADGE = "key_show_folder_unplayed_badge"
    private const val KEY_AUTO_SCROLL_TO_LAST_PLAYED = "key_auto_scroll_to_last_played"
    private const val KEY_TREE_PATH_COMPRESSION = "key_tree_path_compression"
    private const val KEY_DUAL_PANE_VIEW = "key_dual_pane_view"
    private const val KEY_WATCHED_THRESHOLD_PERCENT = "key_watched_threshold_percent"

    // Thumbnails Keys
    private const val KEY_SHOW_VIDEO_THUMBNAILS = "key_show_video_thumbnails"
    private const val KEY_THUMBNAIL_STRATEGY = "key_thumbnail_strategy"
    private const val KEY_THUMBNAIL_QUALITY = "key_thumbnail_quality"
    private const val KEY_THUMBNAIL_FALLBACK_SECOND = "key_thumbnail_fallback_second"
    private const val KEY_TAP_THUMBNAIL_TO_SELECT = "key_tap_thumbnail_to_select"
    private const val KEY_SHOW_NETWORK_THUMBNAILS = "key_show_network_thumbnails"

    // Navigation Keys
    private const val KEY_SHOW_HOME_TAB = "key_show_home_tab"
    private const val KEY_SHOW_MUSIC_TAB = "key_show_music_tab"
    private const val KEY_SHOW_RECENTS_TAB = "key_show_recents_tab"
    private const val KEY_SHOW_PLAYLISTS_TAB = "key_show_playlists_tab"
    private const val KEY_SHOW_NETWORK_TAB = "key_show_network_tab"
    private const val KEY_SHOW_DOWNLOADS_TAB = "key_show_downloads_tab"

    // Animations Keys
    private const val KEY_CONTROLS_ANIMATION_STYLE = "key_controls_animation_style"
    private const val KEY_VIDEO_OPENING_ANIMATION = "key_video_opening_animation"
    private const val KEY_SCREEN_NAVIGATION_STYLE = "key_screen_navigation_style"
    private const val KEY_TAB_NAVIGATION_STYLE = "key_tab_navigation_style"
    private const val KEY_ANIMATION_SPEED = "key_animation_speed"

    // Audio & Sound Keys
    private const val KEY_PREFERRED_AUDIO_LANGUAGES = "key_preferred_audio_languages"
    private const val KEY_APPLY_AUDIO_LANG_SELECTED_ONLY = "key_apply_audio_lang_selected_only"
    private const val KEY_SELECTED_AUDIO_FOLDERS = "key_selected_audio_folders"
    private const val KEY_SELECTED_AUDIO_VIDEOS = "key_selected_audio_videos"
    private const val KEY_ENABLE_AUDIO_PITCH_CORRECTION = "key_enable_audio_pitch_correction"
    private const val KEY_VOLUME_NORMALIZATION = "key_volume_normalization"
    private const val KEY_BACKGROUND_PLAYBACK = "key_background_playback"
    private const val KEY_AUDIO_CHANNEL_MODE = "key_audio_channel_mode"
    private const val KEY_VOLUME_BOOST_CAP = "key_volume_boost_cap"
    private const val KEY_REMEMBER_SELECTED_AUDIO_TRACK = "key_remember_selected_audio_track"

    // Sort & View Options Keys
    private const val KEY_SORT_FIELD = "key_sort_field"
    private const val KEY_SORT_DIRECTION = "key_sort_direction"
    private const val KEY_VIEW_MODE = "key_view_mode"
    private const val KEY_LAYOUT_MODE = "key_layout_mode"

    // Visible Fields Keys
    private const val KEY_FIELD_SHOW_THUMBNAILS = "key_field_show_thumbnails"
    private const val KEY_FIELD_SHOW_EXTENSION = "key_field_show_extension"
    private const val KEY_FIELD_SHOW_DURATION = "key_field_show_duration"
    private const val KEY_FIELD_SHOW_SUBTITLE_INDICATOR = "key_field_show_subtitle_indicator"
    private const val KEY_FIELD_SHOW_FULL_NAME = "key_field_show_full_name"
    private const val KEY_FIELD_SHOW_SIZE = "key_field_show_size"
    private const val KEY_FIELD_SHOW_RESOLUTION = "key_field_show_resolution"
    private const val KEY_FIELD_SHOW_FRAMERATE = "key_field_show_framerate"
    private const val KEY_FIELD_SHOW_DATE = "key_field_show_date"
    private const val KEY_FIELD_SHOW_PROGRESS_BAR = "key_field_show_progress_bar"
    private const val KEY_FIELD_SHOW_PATH = "key_field_show_path"
    private const val KEY_FIELD_SHOW_VIDEO_COUNT = "key_field_show_video_count"
    private const val KEY_FIELD_SHOW_NEW_BADGE = "key_field_show_new_badge"
    private const val KEY_FIELD_SHOW_AUDIO_TYPE = "key_field_show_audio_type"
    private const val KEY_FIELD_SHOW_LYRICS = "key_field_show_lyrics"
    private const val KEY_FIELD_SHOW_AUDIO_EXTENSION = "key_field_show_audio_extension"

    // Custom Folders Key
    private const val KEY_CUSTOM_FOLDERS = "key_custom_folders"

    // Last Used Extract Directory Key
    private const val KEY_LAST_EXTRACT_DIRECTORY = "key_last_extract_directory"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getLastExtractDirectory(context: Context): String? {
        val path = getPrefs(context).getString(KEY_LAST_EXTRACT_DIRECTORY, null)
        if (!path.isNullOrBlank()) {
            val f = java.io.File(path)
            if (f.exists() && f.isDirectory && f.canRead()) {
                return f.absolutePath
            }
        }
        return null
    }

    fun setLastExtractDirectory(context: Context, path: String) {
        if (path.isNotBlank()) {
            getPrefs(context).edit().putString(KEY_LAST_EXTRACT_DIRECTORY, path).apply()
        }
    }

    fun loadSettings(context: Context): UiState {
        val prefs = getPrefs(context)
        val themeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }

        val appScale = prefs.getFloat(KEY_APP_SCALE, 75f).coerceIn(1f, 100f)
        val glassBlurTransparency = prefs.getFloat(KEY_GLASS_BLUR_TRANSPARENCY, 85f).coerceIn(10f, 100f)
        val forceSideBySide = prefs.getBoolean(KEY_FORCE_SIDE_BY_SIDE, false)

        val langStr = prefs.getString(KEY_LANGUAGE, AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name
        val language = try {
            AppLanguage.valueOf(langStr)
        } catch (_: Exception) {
            AppLanguage.ENGLISH
        }

        val frameStepAmount = prefs.getInt(KEY_FRAME_STEP_AMOUNT, 1).coerceIn(1, 30)
        val copyTimestamp = prefs.getBoolean(KEY_COPY_TIMESTAMP, true)

        val hwStr = prefs.getString(KEY_HW_ACCEL_MODE, HwAccelMode.FORCE.name) ?: HwAccelMode.FORCE.name
        val hwAccelMode = try {
            HwAccelMode.valueOf(hwStr)
        } catch (_: Exception) {
            HwAccelMode.FORCE
        }

        val gestureStr = prefs.getString(KEY_GESTURE_SENSITIVITY_MODE, GestureSensitivityMode.EXPONENTIAL.name) ?: GestureSensitivityMode.EXPONENTIAL.name
        val gestureSensitivityMode = try {
            GestureSensitivityMode.valueOf(gestureStr)
        } catch (_: Exception) {
            GestureSensitivityMode.EXPONENTIAL
        }

        val editorTextSizeSp = prefs.getFloat(KEY_EDITOR_TEXT_SIZE, 14f).coerceIn(10f, 28f)
        val timeTextSizeSp = prefs.getFloat(KEY_TIME_TEXT_SIZE, 13.5f).coerceIn(10f, 24f)
        val showPasteBoxConverter = prefs.getBoolean(KEY_PASTE_CONVERTER, true)
        val showPasteBoxCleaner = prefs.getBoolean(KEY_PASTE_CLEANER, true)
        val showPasteBoxShifter = prefs.getBoolean(KEY_PASTE_SHIFTER, true)
        val showPasteBoxEditor = prefs.getBoolean(KEY_PASTE_EDITOR, true)
        val showPasteBoxTextEditor = prefs.getBoolean(KEY_PASTE_TEXT_EDITOR, true)
        val dialogueEditOffsetY = prefs.getFloat(KEY_DIALOGUE_EDIT_OFFSET_Y, 0f).coerceIn(-300f, 300f)
        val colorPickerOffsetY = prefs.getFloat(KEY_COLOR_PICKER_OFFSET_Y, 0f).coerceIn(-300f, 300f)
        val showUploadSectionTextEditor = prefs.getBoolean(KEY_SHOW_UPLOAD_SECTION_TEXT_EDITOR, true)
        val textEditorFontSizeSp = prefs.getFloat(KEY_TEXT_EDITOR_FONT_SIZE, 14f).coerceIn(10f, 28f)
        val showLineNumbersTextEditor = prefs.getBoolean(KEY_SHOW_LINE_NUMBERS_TEXT_EDITOR, true)
        val textEditorWordWrap = prefs.getBoolean(KEY_TEXT_EDITOR_WORD_WRAP, true)
        val showBottomToolbarTextEditor = prefs.getBoolean(KEY_SHOW_BOTTOM_TOOLBAR_TEXT_EDITOR, true)
        val showSearchPanelTextEditor = prefs.getBoolean(KEY_SHOW_SEARCH_PANEL_TEXT_EDITOR, true)
        val navStepStr = prefs.getString(KEY_TEXT_EDITOR_NAV_STEP, TextEditorNavStep.CHARACTER.name) ?: TextEditorNavStep.CHARACTER.name
        val textEditorNavStep = try {
            TextEditorNavStep.valueOf(navStepStr)
        } catch (_: Exception) {
            TextEditorNavStep.CHARACTER
        }

        val showPlayerNotifications = prefs.getBoolean(KEY_SHOW_PLAYER_NOTIFICATIONS, true)
        val seekbarStyleStr = prefs.getString(KEY_SEEKBAR_STYLE, SeekbarStyle.THICK.name) ?: SeekbarStyle.THICK.name
        val seekbarStyle = try {
            SeekbarStyle.valueOf(seekbarStyleStr)
        } catch (_: Exception) {
            SeekbarStyle.STANDARD
        }

        val playerLayoutConfig = loadPlayerLayoutConfig(context)

        // Appearance
        val themeVariantStr = prefs.getString(KEY_APP_THEME, AppThemeVariant.DYNAMIC.name) ?: AppThemeVariant.DYNAMIC.name
        val appTheme = try {
            AppThemeVariant.valueOf(themeVariantStr)
        } catch (_: Exception) {
            AppThemeVariant.DYNAMIC
        }
        val customThemeGradients = decodeCustomThemeGradients(prefs.getString(KEY_CUSTOM_THEME_GRADIENTS, null))
        val amoledBlackMode = prefs.getBoolean(KEY_AMOLED_BLACK_MODE, false)
        val liquidGlassMode = prefs.getBoolean(KEY_LIQUID_GLASS_MODE, true)
        val liquidGlassParams = com.example.ui.components.LiquidGlassParams.decode(prefs.getString(KEY_LIQUID_GLASS_PARAMS, null))
        val liquidGlassControls = prefs.getBoolean(KEY_LIQUID_GLASS_CONTROLS, true)
        val useSystemFont = prefs.getBoolean(KEY_USE_SYSTEM_FONT, false)
        val hapticFeedback = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        com.example.util.AppHaptics.isEnabled = hapticFeedback

        // File Browser
        val showFullName = prefs.getBoolean(KEY_SHOW_FULL_NAME, false)
        val showNewVideoLabel = prefs.getBoolean(KEY_SHOW_NEW_VIDEO_LABEL, true)
        val newVideoDaysThreshold = prefs.getInt(KEY_NEW_VIDEO_DAYS_THRESHOLD, 7).coerceIn(1, 30)
        val showFolderUnplayedBadge = prefs.getBoolean(KEY_SHOW_FOLDER_UNPLAYED_BADGE, true)
        val autoScrollToLastPlayed = prefs.getBoolean(KEY_AUTO_SCROLL_TO_LAST_PLAYED, true)
        val treePathCompression = prefs.getBoolean(KEY_TREE_PATH_COMPRESSION, true)
        val dualPaneView = prefs.getBoolean(KEY_DUAL_PANE_VIEW, false)
        val watchedThresholdPercent = prefs.getInt(KEY_WATCHED_THRESHOLD_PERCENT, 95).coerceIn(50, 100)

        // Thumbnails
        val showVideoThumbnails = prefs.getBoolean(KEY_SHOW_VIDEO_THUMBNAILS, true)
        val thumbStratStr = prefs.getString(KEY_THUMBNAIL_STRATEGY, ThumbnailStrategy.SMART.name) ?: ThumbnailStrategy.SMART.name
        val thumbnailStrategy = try {
            ThumbnailStrategy.valueOf(thumbStratStr)
        } catch (_: Exception) {
            ThumbnailStrategy.SMART
        }
        val thumbQualStr = prefs.getString(KEY_THUMBNAIL_QUALITY, ThumbnailQuality.HIGH.name) ?: ThumbnailQuality.HIGH.name
        val thumbnailQuality = try {
            ThumbnailQuality.valueOf(thumbQualStr)
        } catch (_: Exception) {
            ThumbnailQuality.HIGH
        }
        val thumbnailFallbackSecond = prefs.getInt(KEY_THUMBNAIL_FALLBACK_SECOND, 1).coerceIn(1, 10)
        val tapThumbnailToSelect = prefs.getBoolean(KEY_TAP_THUMBNAIL_TO_SELECT, false)
        val showNetworkThumbnails = prefs.getBoolean(KEY_SHOW_NETWORK_THUMBNAILS, false)

        // Navigation
        val showHomeTab = prefs.getBoolean(KEY_SHOW_HOME_TAB, true)
        val showMusicTab = prefs.getBoolean(KEY_SHOW_MUSIC_TAB, true)
        val showRecentsTab = prefs.getBoolean(KEY_SHOW_RECENTS_TAB, true)
        val showPlaylistsTab = prefs.getBoolean(KEY_SHOW_PLAYLISTS_TAB, true)
        val showNetworkTab = prefs.getBoolean(KEY_SHOW_NETWORK_TAB, true)
        val showDownloadsTab = prefs.getBoolean(KEY_SHOW_DOWNLOADS_TAB, true)

        // Animations
        val ctrlAnimStr = prefs.getString(KEY_CONTROLS_ANIMATION_STYLE, ControlsAnimationStyle.DEFAULT.name) ?: ControlsAnimationStyle.DEFAULT.name
        val controlsAnimationStyle = try {
            ControlsAnimationStyle.valueOf(ctrlAnimStr)
        } catch (_: Exception) {
            ControlsAnimationStyle.DEFAULT
        }
        val vidOpenAnimStr = prefs.getString(KEY_VIDEO_OPENING_ANIMATION, VideoOpeningAnimation.DEFAULT.name) ?: VideoOpeningAnimation.DEFAULT.name
        val videoOpeningAnimation = try {
            VideoOpeningAnimation.valueOf(vidOpenAnimStr)
        } catch (_: Exception) {
            VideoOpeningAnimation.DEFAULT
        }
        val scrNavStr = prefs.getString(KEY_SCREEN_NAVIGATION_STYLE, ScreenNavigationStyle.DEFAULT.name) ?: ScreenNavigationStyle.DEFAULT.name
        val screenNavigationStyle = try {
            ScreenNavigationStyle.valueOf(scrNavStr)
        } catch (_: Exception) {
            ScreenNavigationStyle.DEFAULT
        }
        val tabNavStr = prefs.getString(KEY_TAB_NAVIGATION_STYLE, TabNavigationStyle.DEFAULT.name) ?: TabNavigationStyle.DEFAULT.name
        val tabNavigationStyle = try {
            TabNavigationStyle.valueOf(tabNavStr)
        } catch (_: Exception) {
            TabNavigationStyle.DEFAULT
        }
        val animationSpeed = prefs.getFloat(KEY_ANIMATION_SPEED, 1.0f).coerceIn(0.25f, 2.0f)

        // Audio Settings Load
        val preferredAudioLanguages = prefs.getString(KEY_PREFERRED_AUDIO_LANGUAGES, "") ?: ""
        val applyAudioLanguageToSelectedContentOnly = prefs.getBoolean(KEY_APPLY_AUDIO_LANG_SELECTED_ONLY, false)
        val selectedAudioFolders = prefs.getStringSet(KEY_SELECTED_AUDIO_FOLDERS, emptySet()) ?: emptySet()
        val selectedAudioVideos = prefs.getStringSet(KEY_SELECTED_AUDIO_VIDEOS, emptySet()) ?: emptySet()
        val enableAudioPitchCorrection = prefs.getBoolean(KEY_ENABLE_AUDIO_PITCH_CORRECTION, true)
        val volumeNormalization = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false)
        val backgroundPlayback = prefs.getBoolean(KEY_BACKGROUND_PLAYBACK, false)
        val audioChannelStr = prefs.getString(KEY_AUDIO_CHANNEL_MODE, AudioChannelMode.AUTO.name) ?: AudioChannelMode.AUTO.name
        val audioChannelMode = try {
            AudioChannelMode.valueOf(audioChannelStr)
        } catch (_: Exception) {
            AudioChannelMode.AUTO
        }
        val volumeBoostCap = prefs.getInt(KEY_VOLUME_BOOST_CAP, 150).coerceIn(100, 200)
        val rememberSelectedAudioTrack = prefs.getBoolean(KEY_REMEMBER_SELECTED_AUDIO_TRACK, true)

        return UiState(
            themeMode = themeMode,
            appScale = appScale,
            glassBlurTransparency = glassBlurTransparency,
            forceSideBySide = forceSideBySide,
            language = language,
            frameStepAmount = frameStepAmount,
            copyTimestampOnDoubleTap = copyTimestamp,
            hwAccelMode = hwAccelMode,
            gestureSensitivityMode = gestureSensitivityMode,
            editorTextSizeSp = editorTextSizeSp,
            timeTextSizeSp = timeTextSizeSp,
            showPasteBoxConverter = showPasteBoxConverter,
            showPasteBoxCleaner = showPasteBoxCleaner,
            showPasteBoxShifter = showPasteBoxShifter,
            showPasteBoxEditor = showPasteBoxEditor,
            showPasteBoxTextEditor = showPasteBoxTextEditor,
            dialogueEditOffsetY = dialogueEditOffsetY,
            colorPickerOffsetY = colorPickerOffsetY,
            showUploadSectionTextEditor = showUploadSectionTextEditor,
            textEditorFontSizeSp = textEditorFontSizeSp,
            showLineNumbersTextEditor = showLineNumbersTextEditor,
            textEditorWordWrap = textEditorWordWrap,
            showBottomToolbarTextEditor = showBottomToolbarTextEditor,
            showSearchPanelTextEditor = showSearchPanelTextEditor,
            textEditorNavStep = textEditorNavStep,
            showPlayerNotifications = showPlayerNotifications,
            seekbarStyle = seekbarStyle,
            playerLayoutConfig = playerLayoutConfig,
            playerSettings = PlayerSettings.load(context),

            // Appearance
            appTheme = appTheme,
            systemTone = SystemTone.decode(prefs.getString(KEY_SYSTEM_TONE, null)),
            customThemeGradients = customThemeGradients,
            amoledBlackMode = amoledBlackMode,
            liquidGlassMode = liquidGlassMode,
            liquidGlassParams = liquidGlassParams,
            liquidGlassControls = liquidGlassControls,
            useSystemFont = useSystemFont,
            hapticFeedback = hapticFeedback,

            // File Browser
            showFullName = showFullName,
            showNewVideoLabel = showNewVideoLabel,
            newVideoDaysThreshold = newVideoDaysThreshold,
            showFolderUnplayedBadge = showFolderUnplayedBadge,
            autoScrollToLastPlayed = autoScrollToLastPlayed,
            treePathCompression = treePathCompression,
            dualPaneView = dualPaneView,
            watchedThresholdPercent = watchedThresholdPercent,

            // Thumbnails
            showVideoThumbnails = showVideoThumbnails,
            thumbnailStrategy = thumbnailStrategy,
            thumbnailQuality = thumbnailQuality,
            thumbnailFallbackSecond = thumbnailFallbackSecond,
            tapThumbnailToSelect = tapThumbnailToSelect,
            showNetworkThumbnails = showNetworkThumbnails,

            // Navigation
            showHomeTab = showHomeTab,
            showMusicTab = showMusicTab,
            showRecentsTab = showRecentsTab,
            showPlaylistsTab = showPlaylistsTab,
            showNetworkTab = showNetworkTab,
            showDownloadsTab = showDownloadsTab,

            // Animations
            controlsAnimationStyle = controlsAnimationStyle,
            videoOpeningAnimation = videoOpeningAnimation,
            screenNavigationStyle = screenNavigationStyle,
            tabNavigationStyle = tabNavigationStyle,
            animationSpeed = animationSpeed,

            // Audio & Sound
            preferredAudioLanguages = preferredAudioLanguages,
            applyAudioLanguageToSelectedContentOnly = applyAudioLanguageToSelectedContentOnly,
            selectedAudioFolders = selectedAudioFolders,
            selectedAudioVideos = selectedAudioVideos,
            enableAudioPitchCorrection = enableAudioPitchCorrection,
            volumeNormalization = volumeNormalization,
            backgroundPlayback = backgroundPlayback,
            audioChannelMode = audioChannelMode,
            volumeBoostCap = volumeBoostCap,
            rememberSelectedAudioTrack = rememberSelectedAudioTrack
        )
    }

    fun loadPlayerLayoutConfig(context: Context): PlayerLayoutConfig {
        val prefs = getPrefs(context)
        val trStr = prefs.getString(KEY_TOP_RIGHT_CONTROLS, null)
        val blStr = prefs.getString(KEY_BOTTOM_LEFT_CONTROLS, null)
        val brStr = prefs.getString(KEY_BOTTOM_RIGHT_CONTROLS, null)
        val trEnabled = prefs.getBoolean(KEY_LANDSCAPE_TR_ENABLED, true)
        val blEnabled = prefs.getBoolean(KEY_LANDSCAPE_BL_ENABLED, true)
        val brEnabled = prefs.getBoolean(KEY_LANDSCAPE_BR_ENABLED, true)

        val ptStr = prefs.getString(KEY_PORTRAIT_TOP_CONTROLS, null)
        val pbStr = prefs.getString(KEY_PORTRAIT_BOTTOM_CONTROLS, null)
        val ptEnabled = prefs.getBoolean(KEY_PORTRAIT_TOP_ENABLED, true)
        val pbEnabled = prefs.getBoolean(KEY_PORTRAIT_BOTTOM_ENABLED, true)

        if (trStr == null && blStr == null && brStr == null && ptStr == null && pbStr == null) {
            return PlayerLayoutConfig.default
        }

        val trList = trStr?.split(",")?.mapNotNull { PlayerControlId.fromId(it.trim()) } ?: PlayerLayoutConfig.defaultTopRightControls
        val blList = blStr?.split(",")?.mapNotNull { PlayerControlId.fromId(it.trim()) } ?: PlayerLayoutConfig.defaultBottomLeftControls
        val brList = brStr?.split(",")?.mapNotNull { PlayerControlId.fromId(it.trim()) } ?: PlayerLayoutConfig.defaultBottomRightControls

        val ptList = ptStr?.split(",")?.mapNotNull { PlayerControlId.fromId(it.trim()) } ?: PlayerLayoutConfig.defaultPortraitTopControls
        val pbList = pbStr?.split(",")?.mapNotNull { PlayerControlId.fromId(it.trim()) } ?: PlayerLayoutConfig.defaultPortraitBottomControls

        return PlayerLayoutConfig(
            topRightControls = trList,
            bottomLeftControls = blList,
            bottomRightControls = brList,
            isLandscapeTopRightEnabled = trEnabled,
            isLandscapeBottomLeftEnabled = blEnabled,
            isLandscapeBottomRightEnabled = brEnabled,
            portraitTopControls = ptList,
            portraitBottomControls = pbList,
            isPortraitTopEnabled = ptEnabled,
            isPortraitBottomEnabled = pbEnabled
        ).sanitized()
    }

    fun savePlayerLayoutConfig(context: Context, config: PlayerLayoutConfig) {
        val sanitized = config.sanitized()
        getPrefs(context).edit()
            .putString(KEY_TOP_RIGHT_CONTROLS, sanitized.topRightControls.joinToString(",") { it.id })
            .putString(KEY_BOTTOM_LEFT_CONTROLS, sanitized.bottomLeftControls.joinToString(",") { it.id })
            .putString(KEY_BOTTOM_RIGHT_CONTROLS, sanitized.bottomRightControls.joinToString(",") { it.id })
            .putBoolean(KEY_LANDSCAPE_TR_ENABLED, sanitized.isLandscapeTopRightEnabled)
            .putBoolean(KEY_LANDSCAPE_BL_ENABLED, sanitized.isLandscapeBottomLeftEnabled)
            .putBoolean(KEY_LANDSCAPE_BR_ENABLED, sanitized.isLandscapeBottomRightEnabled)
            .putString(KEY_PORTRAIT_TOP_CONTROLS, sanitized.portraitTopControls.joinToString(",") { it.id })
            .putString(KEY_PORTRAIT_BOTTOM_CONTROLS, sanitized.portraitBottomControls.joinToString(",") { it.id })
            .putBoolean(KEY_PORTRAIT_TOP_ENABLED, sanitized.isPortraitTopEnabled)
            .putBoolean(KEY_PORTRAIT_BOTTOM_ENABLED, sanitized.isPortraitBottomEnabled)
            .apply()
    }

    fun saveSettings(context: Context, state: UiState) {
        val sanitized = state.playerLayoutConfig.sanitized()
        getPrefs(context).edit()
            .putString(KEY_THEME_MODE, state.themeMode.name)
            .putFloat(KEY_APP_SCALE, state.appScale)
            .putFloat(KEY_GLASS_BLUR_TRANSPARENCY, state.glassBlurTransparency)
            .putBoolean(KEY_FORCE_SIDE_BY_SIDE, state.forceSideBySide)
            .putString(KEY_LANGUAGE, state.language.name)
            .putInt(KEY_FRAME_STEP_AMOUNT, state.frameStepAmount)
            .putBoolean(KEY_COPY_TIMESTAMP, state.copyTimestampOnDoubleTap)
            .putString(KEY_HW_ACCEL_MODE, state.hwAccelMode.name)
            .putString(KEY_GESTURE_SENSITIVITY_MODE, state.gestureSensitivityMode.name)
            .putBoolean(KEY_SHOW_PLAYER_NOTIFICATIONS, state.showPlayerNotifications)
            .putString(KEY_SEEKBAR_STYLE, state.seekbarStyle.name)
            .putString(KEY_TOP_RIGHT_CONTROLS, sanitized.topRightControls.joinToString(",") { it.id })
            .putString(KEY_BOTTOM_LEFT_CONTROLS, sanitized.bottomLeftControls.joinToString(",") { it.id })
            .putString(KEY_BOTTOM_RIGHT_CONTROLS, sanitized.bottomRightControls.joinToString(",") { it.id })
            .putBoolean(KEY_LANDSCAPE_TR_ENABLED, sanitized.isLandscapeTopRightEnabled)
            .putBoolean(KEY_LANDSCAPE_BL_ENABLED, sanitized.isLandscapeBottomLeftEnabled)
            .putBoolean(KEY_LANDSCAPE_BR_ENABLED, sanitized.isLandscapeBottomRightEnabled)
            .putString(KEY_PORTRAIT_TOP_CONTROLS, sanitized.portraitTopControls.joinToString(",") { it.id })
            .putString(KEY_PORTRAIT_BOTTOM_CONTROLS, sanitized.portraitBottomControls.joinToString(",") { it.id })
            .putBoolean(KEY_PORTRAIT_TOP_ENABLED, sanitized.isPortraitTopEnabled)
            .putBoolean(KEY_PORTRAIT_BOTTOM_ENABLED, sanitized.isPortraitBottomEnabled)
            .putFloat(KEY_EDITOR_TEXT_SIZE, state.editorTextSizeSp)
            .putFloat(KEY_TIME_TEXT_SIZE, state.timeTextSizeSp)
            .putBoolean(KEY_PASTE_CONVERTER, state.showPasteBoxConverter)
            .putBoolean(KEY_PASTE_CLEANER, state.showPasteBoxCleaner)
            .putBoolean(KEY_PASTE_SHIFTER, state.showPasteBoxShifter)
            .putBoolean(KEY_PASTE_EDITOR, state.showPasteBoxEditor)
            .putBoolean(KEY_PASTE_TEXT_EDITOR, state.showPasteBoxTextEditor)
            .putFloat(KEY_DIALOGUE_EDIT_OFFSET_Y, state.dialogueEditOffsetY)
            .putFloat(KEY_COLOR_PICKER_OFFSET_Y, state.colorPickerOffsetY)
            .putBoolean(KEY_SHOW_UPLOAD_SECTION_TEXT_EDITOR, state.showUploadSectionTextEditor)
            .putFloat(KEY_TEXT_EDITOR_FONT_SIZE, state.textEditorFontSizeSp)
            .putBoolean(KEY_SHOW_LINE_NUMBERS_TEXT_EDITOR, state.showLineNumbersTextEditor)
            .putBoolean(KEY_TEXT_EDITOR_WORD_WRAP, state.textEditorWordWrap)
            .putBoolean(KEY_SHOW_BOTTOM_TOOLBAR_TEXT_EDITOR, state.showBottomToolbarTextEditor)
            .putBoolean(KEY_SHOW_SEARCH_PANEL_TEXT_EDITOR, state.showSearchPanelTextEditor)
            .putString(KEY_TEXT_EDITOR_NAV_STEP, state.textEditorNavStep.name)

            // Appearance
            .putString(KEY_APP_THEME, state.appTheme.name)
            .putString(KEY_SYSTEM_TONE, state.systemTone?.encode() ?: "")
            .putString(KEY_CUSTOM_THEME_GRADIENTS, encodeCustomThemeGradients(state.customThemeGradients))
            .putBoolean(KEY_AMOLED_BLACK_MODE, state.amoledBlackMode)
            .putBoolean(KEY_LIQUID_GLASS_MODE, state.liquidGlassMode)
            .putString(KEY_LIQUID_GLASS_PARAMS, state.liquidGlassParams.encode())
            .putBoolean(KEY_LIQUID_GLASS_CONTROLS, state.liquidGlassControls)
            .putBoolean(KEY_USE_SYSTEM_FONT, state.useSystemFont)
            .putBoolean(KEY_HAPTIC_FEEDBACK, state.hapticFeedback)

            // File Browser
            .putBoolean(KEY_SHOW_FULL_NAME, state.showFullName)
            .putBoolean(KEY_SHOW_NEW_VIDEO_LABEL, state.showNewVideoLabel)
            .putInt(KEY_NEW_VIDEO_DAYS_THRESHOLD, state.newVideoDaysThreshold)
            .putBoolean(KEY_SHOW_FOLDER_UNPLAYED_BADGE, state.showFolderUnplayedBadge)
            .putBoolean(KEY_AUTO_SCROLL_TO_LAST_PLAYED, state.autoScrollToLastPlayed)
            .putBoolean(KEY_TREE_PATH_COMPRESSION, state.treePathCompression)
            .putBoolean(KEY_DUAL_PANE_VIEW, state.dualPaneView)
            .putInt(KEY_WATCHED_THRESHOLD_PERCENT, state.watchedThresholdPercent)

            // Thumbnails
            .putBoolean(KEY_SHOW_VIDEO_THUMBNAILS, state.showVideoThumbnails)
            .putString(KEY_THUMBNAIL_STRATEGY, state.thumbnailStrategy.name)
            .putString(KEY_THUMBNAIL_QUALITY, state.thumbnailQuality.name)
            .putInt(KEY_THUMBNAIL_FALLBACK_SECOND, state.thumbnailFallbackSecond.coerceIn(1, 10))
            .putBoolean(KEY_TAP_THUMBNAIL_TO_SELECT, state.tapThumbnailToSelect)
            .putBoolean(KEY_SHOW_NETWORK_THUMBNAILS, state.showNetworkThumbnails)

            // Navigation
            .putBoolean(KEY_SHOW_HOME_TAB, state.showHomeTab)
            .putBoolean(KEY_SHOW_MUSIC_TAB, state.showMusicTab)
            .putBoolean(KEY_SHOW_RECENTS_TAB, state.showRecentsTab)
            .putBoolean(KEY_SHOW_PLAYLISTS_TAB, state.showPlaylistsTab)
            .putBoolean(KEY_SHOW_NETWORK_TAB, state.showNetworkTab)
            .putBoolean(KEY_SHOW_DOWNLOADS_TAB, state.showDownloadsTab)

            // Animations
            .putString(KEY_CONTROLS_ANIMATION_STYLE, state.controlsAnimationStyle.name)
            .putString(KEY_VIDEO_OPENING_ANIMATION, state.videoOpeningAnimation.name)
            .putString(KEY_SCREEN_NAVIGATION_STYLE, state.screenNavigationStyle.name)
            .putString(KEY_TAB_NAVIGATION_STYLE, state.tabNavigationStyle.name)
            .putFloat(KEY_ANIMATION_SPEED, state.animationSpeed)

            // Audio & Sound
            .putString(KEY_PREFERRED_AUDIO_LANGUAGES, state.preferredAudioLanguages)
            .putBoolean(KEY_APPLY_AUDIO_LANG_SELECTED_ONLY, state.applyAudioLanguageToSelectedContentOnly)
            .putStringSet(KEY_SELECTED_AUDIO_FOLDERS, state.selectedAudioFolders)
            .putStringSet(KEY_SELECTED_AUDIO_VIDEOS, state.selectedAudioVideos)
            .putBoolean(KEY_ENABLE_AUDIO_PITCH_CORRECTION, state.enableAudioPitchCorrection)
            .putBoolean(KEY_VOLUME_NORMALIZATION, state.volumeNormalization)
            .putBoolean(KEY_BACKGROUND_PLAYBACK, state.backgroundPlayback)
            .putString(KEY_AUDIO_CHANNEL_MODE, state.audioChannelMode.name)
            .putInt(KEY_VOLUME_BOOST_CAP, state.volumeBoostCap)
            .putBoolean(KEY_REMEMBER_SELECTED_AUDIO_TRACK, state.rememberSelectedAudioTrack)
            .apply()
        PlayerSettings.save(context, state.playerSettings)
        // Settings > Advanced > Auto backup
        AdvancedEngineSettings.scheduleAutoBackup(context, state)
    }

    fun loadSortField(context: Context, tabId: String = "home"): SortField {
        val defaultField = SortField.TITLE.name
        val key = if (tabId == "home") KEY_SORT_FIELD else "${KEY_SORT_FIELD}_$tabId"
        val prefs = getPrefs(context)
        var str = prefs.getString(key, null)
        if (str == null && tabId.contains("_")) {
            val baseTab = tabId.substringBefore("_")
            val baseKey = if (baseTab == "home") KEY_SORT_FIELD else "${KEY_SORT_FIELD}_$baseTab"
            str = prefs.getString(baseKey, null)
        }
        val finalStr = str ?: defaultField
        return try {
            SortField.valueOf(finalStr)
        } catch (_: Exception) {
            SortField.TITLE
        }
    }

    fun saveSortField(context: Context, field: SortField, tabId: String = "home") {
        val key = if (tabId == "home") KEY_SORT_FIELD else "${KEY_SORT_FIELD}_$tabId"
        getPrefs(context).edit().putString(key, field.name).apply()
    }

    fun loadSortDirection(context: Context, tabId: String = "home"): SortDirection {
        val defaultDir = if (tabId.contains("recents")) SortDirection.DESCENDING.name else SortDirection.ASCENDING.name
        val key = if (tabId == "home") KEY_SORT_DIRECTION else "${KEY_SORT_DIRECTION}_$tabId"
        val prefs = getPrefs(context)
        var str = prefs.getString(key, null)
        if (str == null && tabId.contains("_")) {
            val baseTab = tabId.substringBefore("_")
            val baseKey = if (baseTab == "home") KEY_SORT_DIRECTION else "${KEY_SORT_DIRECTION}_$baseTab"
            str = prefs.getString(baseKey, null)
        }
        val finalStr = str ?: defaultDir
        return try {
            SortDirection.valueOf(finalStr)
        } catch (_: Exception) {
            if (tabId.contains("recents")) SortDirection.DESCENDING else SortDirection.ASCENDING
        }
    }

    fun saveSortDirection(context: Context, direction: SortDirection, tabId: String = "home") {
        val key = if (tabId == "home") KEY_SORT_DIRECTION else "${KEY_SORT_DIRECTION}_$tabId"
        getPrefs(context).edit().putString(key, direction.name).apply()
    }

    fun loadViewMode(context: Context, tabId: String = "home"): ViewMode {
        if (tabId.startsWith("recents")) return ViewMode.LIBRARY
        val defaultMode = if (tabId.contains("recents")) ViewMode.LIBRARY.name else ViewMode.FOLDER.name
        val key = if (tabId == "home") KEY_VIEW_MODE else "${KEY_VIEW_MODE}_$tabId"
        val prefs = getPrefs(context)
        var str = prefs.getString(key, null)
        if (str == null && tabId.contains("_")) {
            val baseTab = tabId.substringBefore("_")
            val baseKey = if (baseTab == "home") KEY_VIEW_MODE else "${KEY_VIEW_MODE}_$baseTab"
            str = prefs.getString(baseKey, null)
        }
        val finalStr = str ?: defaultMode
        return try {
            ViewMode.valueOf(finalStr)
        } catch (_: Exception) {
            if (tabId.contains("recents")) ViewMode.LIBRARY else ViewMode.FOLDER
        }
    }

    fun saveViewMode(context: Context, mode: ViewMode, tabId: String = "home") {
        val key = if (tabId == "home") KEY_VIEW_MODE else "${KEY_VIEW_MODE}_$tabId"
        getPrefs(context).edit().putString(key, mode.name).apply()
    }

    fun loadLayoutMode(context: Context, tabId: String = "home"): LayoutMode {
        val defaultLayout = LayoutMode.LIST.name
        val key = if (tabId == "home") KEY_LAYOUT_MODE else "${KEY_LAYOUT_MODE}_$tabId"
        val prefs = getPrefs(context)
        var str = prefs.getString(key, null)
        if (str == null && tabId.contains("_")) {
            val baseTab = tabId.substringBefore("_")
            val baseKey = if (baseTab == "home") KEY_LAYOUT_MODE else "${KEY_LAYOUT_MODE}_$baseTab"
            str = prefs.getString(baseKey, null)
        }
        val finalStr = str ?: defaultLayout
        return try {
            LayoutMode.valueOf(finalStr)
        } catch (_: Exception) {
            LayoutMode.LIST
        }
    }

    fun saveLayoutMode(context: Context, mode: LayoutMode, tabId: String = "home") {
        val key = if (tabId == "home") KEY_LAYOUT_MODE else "${KEY_LAYOUT_MODE}_$tabId"
        getPrefs(context).edit().putString(key, mode.name).apply()
    }

    fun loadVisibleFields(context: Context, tabId: String = "home"): VisibleFields {
        migrateSortViewV2(context, tabId)
        val prefs = getPrefs(context)
        val prefix = if (tabId == "home") "" else "${tabId}_"
        val hasSpecific = prefs.contains("${prefix}$KEY_FIELD_SHOW_THUMBNAILS")
        val effectivePrefix = if (!hasSpecific && tabId.contains("_")) {
            val baseTab = tabId.substringBefore("_")
            if (baseTab == "home") "" else "${baseTab}_"
        } else {
            prefix
        }
        val isFolderScope = tabId.endsWith("_folders") || (tabId in listOf("home", "music") && !tabId.endsWith("_content"))
        val defaultDuration = if (isFolderScope) false else (tabId.contains("recents") || tabId.contains("music") || tabId.endsWith("_content"))
        val defaultVideoCount = isFolderScope || tabId == "home" || tabId == "music"
        return VisibleFields(
            showThumbnails = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_THUMBNAILS", true),
            showExtension = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_EXTENSION", true),
            showDuration = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_DURATION", defaultDuration),
            showSubtitleIndicator = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_SUBTITLE_INDICATOR", true),
            showFullName = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_FULL_NAME", false),
            showSize = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_SIZE", true),
            showResolution = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_RESOLUTION", true),
            showFramerate = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_FRAMERATE", false),
            showDate = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_DATE", tabId.contains("recents")),
            showProgressBar = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_PROGRESS_BAR", true),
            showPath = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_PATH", false),
            showVideoCount = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_VIDEO_COUNT", defaultVideoCount),
            showNewBadge = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_NEW_BADGE", false),
            showAudioType = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_AUDIO_TYPE", true),
            showLyrics = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_LYRICS", true),
            showAudioExtension = prefs.getBoolean("${effectivePrefix}$KEY_FIELD_SHOW_AUDIO_EXTENSION", false)
        )
    }

    /** Online video folder / online video playlist: only Thumbnails, Duration, Date, Progress Bar are offered (never Size). */
    fun sanitizeOnlineVideoFields(f: VisibleFields) = f.copy(
        showSize = false, showPath = false, showSubtitleIndicator = false, showFramerate = false,
        showResolution = false, showVideoCount = false, showNewBadge = false, showFullName = false,
        showExtension = false, showAudioType = false, showLyrics = false
    )

    /** Online audio folder / online audio playlist: only Thumbnails, Duration, Date, Progress Bar are offered (never Size). */
    fun sanitizeOnlineAudioFields(f: VisibleFields) = f.copy(
        showSize = false, showPath = false, showSubtitleIndicator = false, showFramerate = false,
        showResolution = false, showVideoCount = false, showNewBadge = false, showFullName = false,
        showExtension = false, showAudioType = false, showLyrics = false
    )

    /** Recent history (video & audio): only Thumbnails, Duration, Date, Progress Bar, Path are offered. */
    fun sanitizeRecentsFields(f: VisibleFields) = f.copy(
        showSize = false, showSubtitleIndicator = false, showFramerate = false, showResolution = false,
        showVideoCount = false, showNewBadge = false, showFullName = false, showExtension = false,
        showAudioType = false, showLyrics = false
    )

    private val sortViewV2Tabs = setOf(
        "online_video", "online_audio", "online_playlist_video", "online_playlist_audio",
        "recents_content", "recents_folders", "recents"
    )

    /** One-time reset so the new default (Thumbnails, Duration, Date, Progress Bar [+ Path] all selected) applies to existing installs. */
    private fun migrateSortViewV2(context: Context, tabId: String) {
        if (tabId !in sortViewV2Tabs) return
        val prefs = getPrefs(context)
        val flag = "sv_defaults_v2_$tabId"
        if (prefs.getBoolean(flag, false)) return
        val base = VisibleFields(
            showThumbnails = true, showDuration = true, showDate = true, showProgressBar = true,
            showPath = tabId.startsWith("recents")
        )
        val fields = when (tabId) {
            "online_video", "online_playlist_video" -> sanitizeOnlineVideoFields(base)
            "online_audio", "online_playlist_audio" -> sanitizeOnlineAudioFields(base)
            else -> sanitizeRecentsFields(base)
        }
        saveVisibleFields(context, fields, tabId)
        prefs.edit().putBoolean(flag, true).apply()
        if (tabId.startsWith("recents")) {
            // Recent history is always shown in Library style
            saveViewMode(context, ViewMode.LIBRARY, "recents")
        }
    }

    fun saveVisibleFields(context: Context, fields: VisibleFields, tabId: String = "home") {
        val prefix = if (tabId == "home") "" else "${tabId}_"
        getPrefs(context).edit()
            .putBoolean("${prefix}$KEY_FIELD_SHOW_THUMBNAILS", fields.showThumbnails)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_EXTENSION", fields.showExtension)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_DURATION", fields.showDuration)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_SUBTITLE_INDICATOR", fields.showSubtitleIndicator)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_FULL_NAME", fields.showFullName)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_SIZE", fields.showSize)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_RESOLUTION", fields.showResolution)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_FRAMERATE", fields.showFramerate)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_DATE", fields.showDate)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_PROGRESS_BAR", fields.showProgressBar)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_PATH", fields.showPath)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_VIDEO_COUNT", fields.showVideoCount)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_NEW_BADGE", fields.showNewBadge)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_AUDIO_TYPE", fields.showAudioType)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_LYRICS", fields.showLyrics)
            .putBoolean("${prefix}$KEY_FIELD_SHOW_AUDIO_EXTENSION", fields.showAudioExtension)
            .apply()
    }

    fun loadCustomFolders(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_CUSTOM_FOLDERS, emptySet()) ?: emptySet()
    }

    fun saveCustomFolders(context: Context, folders: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_CUSTOM_FOLDERS, folders).apply()
    }
}
