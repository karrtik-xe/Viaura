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

object AppStrings {
    fun getTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH, AppLanguage.HINGLISH, AppLanguage.HINDI -> "Viaura"
    }

    fun getSettingsDashboard(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Settings Dashboard")

    fun getDone(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Done")

    fun getTabPlayer(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Player")

    fun getTabAudio(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Audio")

    fun getTabDecoder(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Decoder")

    fun getTabSubtitles(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Subtitles")

    fun getTabAbout(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "About")

    fun getTabPlayerLayout(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Layout")

    fun getTabGestures(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Gesture")

    fun getSeekbarStyleTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Seekbar Style")

    fun getSectionAppearance(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "INTERFACE APPEARANCE")

    fun getLightMode(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Light Mode")

    fun getDarkMode(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Dark Mode")

    fun getSectionLanguage(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "INTERFACE LANGUAGE")

    fun getSectionScaling(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "DISPLAY ZOOM & SCALE")

    fun getAppScaleZoom(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Application Scale / Zoom:")

    fun getSectionGlassBlur(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "CONTROLS UI GLASS BLUR & TRANSLUCENCY")

    fun getGlassBlurTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "UI Glass Blur & Opacity:")

    fun getGlassBlurSubtitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Controls player buttons, menus, popups & panels")

    fun getGlassBlurLow(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "10% (High Transparent)")

    fun getGlassBlurHigh(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "100% (Solid Frost)")

    fun getSectionPlayback(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "PLAYBACK SETTINGS")

    fun getCopyTimestamp(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Copy Timestamp on Double Tap")

    fun getCopyTimestampDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Quickly copy current playback time to clipboard")

    fun getForceSideBySide(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Side-by-Side Dual View")

    fun getForceSideBySideDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Force split view layout on landscape screens")

    fun getSectionFastSeek(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "FAST SEEK STEP")

    fun getSectionGestureSensitivity(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "GESTURE SENSITIVITY & SCALING")

    fun getGestureLinearTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Linear (1:1 Uniform)")

    fun getGestureLinearDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Direct 1:1 proportional swipe response for uniform volume & brightness steps")

    fun getGestureExponentialTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Exponential (Perceptual Curve)")

    fun getGestureExponentialDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Fine-grain precision at lower levels with smooth dynamic curve to prevent lag & jumps")

    fun getRecommended(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "RECOMMENDED")

    // Audio & Sound Strings
    fun getSectionPreferredLanguages(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "PREFERRED AUDIO LANGUAGES")

    fun getPreferredLanguagesTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Preferred languages")

    fun getPreferredLanguagesDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Comma-separated codes (e.g. hin,eng,jpn). The first code has the highest priority and falls back sequentially.")

    fun getApplyToSelectedContentTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Apply to selected folder/videos only")

    fun getApplyToSelectedContentDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Restrict auto-selection rule to chosen directories instead of applying globally to all videos")

    fun getSectionSoundProcessing(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "PLAYBACK & SOUND PROCESSING")

    fun getAudioPitchCorrectionTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Enable audio pitch correction")

    fun getAudioPitchCorrectionDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Preserves natural human vocal pitch when playing at high (0.25x - 4.0x) or slow speeds")

    fun getVolumeNormalizationTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Volume normalization")

    fun getVolumeNormalizationDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Maintains consistent perceived loudness between different media and audio tracks dynamically")

    fun getRememberAudioTrackTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Remember selected audio track")

    fun getRememberAudioTrackDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Saves manual audio track changes per video so your selection is restored next time")

    fun getSectionBackgroundPlayback(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "BACKGROUND AUDIO")

    fun getBackgroundPlaybackTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Background playback")

    fun getBackgroundPlaybackDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Continue playing audio when app is minimized or the screen is turned off")

    fun getSectionAudioChannels(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "AUDIO CHANNELS & ACOUSTICS")

    fun getSectionVolumeBoost(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "VOLUME AMPLIFICATION & BOOST")

    fun getVolumeBoostTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Volume boost cap")

    fun getVolumeBoostDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Maximum software amplification limit when swiping beyond standard volume")

    fun getSectionHwAccel(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "HARDWARE ACCELERATION")

    fun getHwForceTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Force MediaCodec")

    fun getHwForceDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Direct MediaCodec HW acceleration for max frame sync & smoothness")

    fun getHwPreferTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Prefer (Auto Copy)")

    fun getHwPreferDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Hardware decoding with auto copyback buffer fallback")

    fun getHwDisableTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Disable (Software CPU)")

    fun getHwDisableDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Software CPU decoding to troubleshoot persistent GPU/rendering glitches")

    fun getSectionRenderEngine(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "RENDER ENGINE")

    fun getRenderLibassTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "libass Subtitle Renderer")

    fun getRenderLibassDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Pixel-perfect Aegisub styles, transforms & karaoke effects")

    fun getRenderEmbeddedFontsTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Embedded Font Extraction")

    fun getRenderEmbeddedFontsDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Loads custom fonts packed directly in MKV / MP4 containers")

    fun getRenderHwEqTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Video Setting")

    fun getRenderHwEqDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Live filters, color presets and video adjustments")

    fun getActiveBadge(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Active")

    fun getSectionPlayerNotification(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "PLAYER ACTION NOTIFICATIONS")

    fun getPlayerNotificationTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Player Action Notifications")

    fun getPlayerNotificationDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Show sleek system glass notifications when changing tracks, looping, or seeking")

    fun getTabAdvanced(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Advanced")

    fun getExportSettingsTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Export Settings")

    fun getExportSettingsDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Export settings to an XML file")

    fun getImportSettingsTitle(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Import Settings")

    fun getImportSettingsDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Import settings from an XML file")

    fun getImportConfirmMessage(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "Do you want to apply these settings?")

    fun getImportConfirmDesc(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "All current Viaura player, appearance, audio, and subtitle configurations will be replaced.")

    fun getCancelButton(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "CANCEL")

    fun getConfirmButton(lang: AppLanguage): String = com.example.ui.i18n.I18n.t(lang, "CONFIRM")
}
