package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.components.DownloadOptionsSheet
import com.example.ui.state.SettingsPreferencesManager
import com.example.ui.state.ThemeMode
import com.example.ui.theme.VideoPlayerTheme
import com.example.util.DownloadSourceType
import com.example.util.NetworkStreamResolver
import com.example.util.OnlineMusicService

/**
 * "Quick download" share target: a link shared from another app (Instagram, YouTube ...) opens Viaura's download panel
 * right over that app. Viaura itself is not opened; the panel closes when the download is queued or dismissed.
 */
class QuickDownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            ?: intent.dataString
        val rawUrl = OnlineMusicService.extractUrlFromText(sharedText)
        val url = rawUrl?.let { NetworkStreamResolver.normalizeUrl(it) }
        if (url == null) {
            Toast.makeText(this, "No link found to download", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val lower = url.lowercase(java.util.Locale.ROOT)
        val isYouTube = lower.contains("youtube.com") || lower.contains("youtu.be/")
        val isMusic = lower.contains("music.youtube.com")
        val settings = SettingsPreferencesManager.loadSettings(applicationContext)

        setContent {
            VideoPlayerTheme(
                themeMode = settings.themeMode,
                appScale = settings.appScale,
                glassBlurTransparency = settings.glassBlurTransparency,
                appTheme = settings.appTheme,
                customGradientHex = settings.customThemeGradients[settings.appTheme.id],
                systemTone = settings.systemTone,
                amoledBlackMode = settings.amoledBlackMode,
                liquidGlassMode = settings.liquidGlassMode,
                useSystemFont = settings.useSystemFont,
                liquidGlassParams = settings.liquidGlassParams,
                liquidGlassControls = settings.liquidGlassControls
            ) {
                val isDark = settings.themeMode == ThemeMode.DARK ||
                    (settings.themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                DownloadOptionsSheet(
                    isDark = isDark,
                    glassBlurTransparency = settings.glassBlurTransparency,
                    url = url,
                    initialTitle = url,
                    startWithAudio = isMusic,
                    sourceType = if (isYouTube) DownloadSourceType.ONLINE_VIDEO else DownloadSourceType.NETWORK_LINK,
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        // no window animation: the panel's own slide-out is the only motion
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
