package com.example

import com.example.R

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.player.SubtitleFontManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.getRequiredMediaPermissions
import com.example.ui.components.hasVideoPermission
import com.example.ui.components.hasAllFilesAccess
import com.example.ui.components.openAllFilesAccessSettings
import com.example.ui.screens.HomeScreen
import com.example.ui.components.toOnlineVideoItem
import com.example.ui.screens.VideoItem
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.state.AppViewModel
import com.example.ui.state.ThemeMode
import com.example.ui.state.VideoOpeningAnimation
import com.example.ui.state.ScreenNavigationStyle
import com.example.ui.theme.VideoPlayerTheme
import com.example.util.PipModeState
import com.example.util.SleepTimerManager
import kotlinx.coroutines.delay

enum class Screen {
    HOME, PLAYER, CHANNEL_DETAIL, SHARED_PLAYLIST
}

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    private var pendingPostNotificationAction: (() -> Unit)? = null

    val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val track = com.example.player.AudioPlaybackManager.currentTrack.value
            val isPlaying = com.example.player.AudioPlaybackManager.isPlaying.value
            if (track != null) {
                com.example.player.AudioNotificationManager.showOrUpdateNotification(applicationContext, track, isPlaying)
            }
        }
        pendingPostNotificationAction?.invoke()
        pendingPostNotificationAction = null
    }

    fun requestNotificationPermissions(after: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !com.example.player.AudioNotificationManager.hasNotificationPermission(this)
        ) {
            pendingPostNotificationAction = after
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            after?.invoke()
        }
    }

    private fun continueFullStorageAccessFlow(after: ((Boolean) -> Unit)? = null) {
        requestNotificationPermissions {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess(this)) {
                openAllFilesAccessSettings(this)
            }
            after?.invoke(hasVideoPermission(this))
        }
    }

    val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _: Map<String, Boolean> ->
        val granted = hasVideoPermission(this)
        viewModel.setHasMediaPermission(granted)
        if (granted) {
            viewModel.triggerLibraryRefresh()
        }
        val callback = pendingPermissionCallback
        pendingPermissionCallback = null
        continueFullStorageAccessFlow { fullAccessGranted ->
            val finalGranted = fullAccessGranted || hasVideoPermission(this)
            viewModel.setHasMediaPermission(finalGranted)
            if (finalGranted) viewModel.triggerLibraryRefresh()
            callback?.invoke(finalGranted)
        }
    }

    fun requestMediaPermissions(callback: ((Boolean) -> Unit)? = null) {
        pendingPermissionCallback = callback

        // Always request the standard runtime media permissions first (READ_MEDIA_VIDEO /
        // READ_MEDIA_AUDIO on Android 13+, READ_EXTERNAL_STORAGE on Android 10-12/below) BEFORE
        // moving on to "All Files Access", on every Android version — including 11+.
        //
        // Previously, on Android 11+ this jumped straight to the All Files Access settings
        // screen and never called mediaPermissionLauncher at all, so READ_MEDIA_VIDEO/AUDIO
        // stayed in their default "not granted" state even after the user granted All Files
        // Access. On stock Android, MANAGE_EXTERNAL_STORAGE alone is enough, but several OEM
        // builds (seen especially on some Xiaomi/MIUI, Oppo/ColorOS and Vivo/FuntouchOS
        // devices) still gate MediaStore.Video/Audio queries behind the standard runtime
        // permission even when All Files Access is granted. That is what made the Video
        // section stay empty and the Home screen show no folders on other people's phones
        // while working fine on the developer's own phone/OEM.
        val permissions = getRequiredMediaPermissions()
        val needsRuntimeMediaPermission = permissions.any {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (needsRuntimeMediaPermission) {
            mediaPermissionLauncher.launch(permissions)
        } else {
            continueFullStorageAccessFlow { granted ->
                viewModel.setHasMediaPermission(granted)
                if (granted) viewModel.triggerLibraryRefresh()
                val cb = pendingPermissionCallback
                pendingPermissionCallback = null
                cb?.invoke(granted)
            }
        }
    }

    /** Paints the window with the app's own background colour (from the saved theme) before anything else is drawn. */
    private fun applyLaunchWindowBackground() {
        try {
            val prefs = getSharedPreferences("video_player_settings_prefs", MODE_PRIVATE)
            val mode = prefs.getString("key_theme_mode", "LIGHT") ?: "LIGHT"
            val systemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val dark = mode == "DARK" || (mode == "SYSTEM" && systemDark)
            val amoled = prefs.getBoolean("key_amoled_black_mode", false)
            val color = if (dark) (if (amoled) 0xFF000000.toInt() else 0xFF090D16.toInt()) else 0xFFF8FAFC.toInt()
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(color))
        } catch (_: Throwable) {
        }
    }

    // Settings > Advanced > input.conf: keys of a hardware keyboard / remote run the commands bound in input.conf while a video plays.
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        try {
            if (viewModel.currentScreenState.value == Screen.PLAYER &&
                com.example.player.VideoPlayerMpvView.forwardKey(this, event)
            ) return true
        } catch (_: Throwable) {}
        return super.dispatchKeyEvent(event)
    }

    // Every new touch is reported to the page backdrop (Diwali sets off a firework wherever the screen is touched).
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        try {
            val action = ev.actionMasked
            if (action == android.view.MotionEvent.ACTION_DOWN || action == android.view.MotionEvent.ACTION_POINTER_DOWN) {
                val i = ev.actionIndex
                com.example.background.BackdropTouch.report(ev.getX(i), ev.getY(i))
            }
        } catch (_: Throwable) {}
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keeps the reason of a crash so it can be shown (and copied) on the next start.
        com.example.util.CrashReporter.install(applicationContext)
        val previousCrashReport = if (savedInstanceState == null) com.example.util.CrashReporter.consume(applicationContext) else null
        applyLaunchWindowBackground()
        com.example.util.DownloadStore.init(applicationContext)
        // Appearance extras: background animation, and Remove Card UI choices
        com.example.background.BackgroundAnimationSettings.init(applicationContext)
        com.example.ui.components.CardStyleSettings.init(applicationContext)
        enableEdgeToEdge()
        val initiallyGranted = hasVideoPermission(this)
        viewModel.setHasMediaPermission(initiallyGranted)
        // HomeScreen performs the initial silent/cache scan. Do not start a second
        // full-library scan here every time the Activity is recreated.
        // The start-up jobs below run right after the first frame, so the launch animation appears without delay.
        window.decorView.post {
            com.example.util.AppStorageManager.performBackgroundCleanup(applicationContext)
            // downloads that were running when the app was closed continue by themselves, from where they stopped
            com.example.util.DownloadManager.resumeInterrupted(applicationContext)
            // yt-dlp installs itself in the background (no button press needed in Settings > Network)
            com.example.ytdlp.YtdlpManager.autoInstallInBackground(applicationContext)
            // new GitHub release? (only when "Auto Check for Updates" is ON in Settings > About)
            if (com.example.BuildConfig.ENABLE_UPDATER) com.example.update.AppUpdater.autoCheckOnStartup(applicationContext)
        }
        PipModeState.requestPipUpdate = { isPlaying, autoPip ->
            updatePipParams(isPlaying, autoPip)
        }
        val filter = IntentFilter("com.example.action.PIP_PLAY_PAUSE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pipBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pipBroadcastReceiver, filter)
        }
        // Back out of the app while the video mini player plays: the video continues in a Picture-in-Picture popup
        // (Compose BackHandlers registered later take priority, so Back inside the app behaves exactly as before).
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tryEnterMiniPip()) return
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
        handleIncomingIntent(intent)
        // Sleep timer: runs for the whole life of the Activity (video, audio, online or offline).
        lifecycleScope.launch {
            SleepTimerManager.expired.collect { handleSleepTimerExpired() }
        }
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreenState.collectAsStateWithLifecycle()
            var showLaunchSplash by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(savedInstanceState == null) }
            // the real UI behind the launch splash is blurred while the splash is shown (Android 12+; older versions skip the blur)
            // The launch animation is drawn in the very first frame; the (heavy) real UI is composed one frame later.
            var contentReady by remember { mutableStateOf(savedInstanceState != null) }
            LaunchedEffect(Unit) {
                androidx.compose.runtime.withFrameNanos { }
                contentReady = true
            }
            var splashBlurOn by remember { mutableStateOf(savedInstanceState == null) }
            val splashBlurRadius by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (showLaunchSplash && splashBlurOn) 18.dp else 0.dp,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "SplashBlur"
            )

            LaunchedEffect(currentScreen) {
                if (currentScreen != Screen.PLAYER) {
                    PipModeState.isPlayerActive = false
                    PipModeState.isCurrentlyPlaying = false
                    PipModeState.autoPipEnabled = false
                    updatePipParams(isPlaying = false, autoPipEnabled = false)
                    kotlinx.coroutines.delay(700)
                    viewModel.isPlayerInLandscape = false
                }
            }

            VideoPlayerTheme(
                themeMode = uiState.themeMode,
                appScale = uiState.appScale,
                glassBlurTransparency = uiState.glassBlurTransparency,
                appTheme = uiState.appTheme,
                customGradientHex = uiState.customThemeGradients[uiState.appTheme.id],
                systemTone = uiState.systemTone,
                amoledBlackMode = uiState.amoledBlackMode,
                liquidGlassMode = uiState.liquidGlassMode,
                useSystemFont = uiState.useSystemFont,
                liquidGlassParams = uiState.liquidGlassParams,
                liquidGlassControls = uiState.liquidGlassControls,
                seekbarStyle = uiState.seekbarStyle,
                language = uiState.language
            ) {
                com.example.ui.components.PluginBrowserHost()
                // "Update Available" sheet (start-up check / Settings > About > Check for Updates Now)
                if (com.example.BuildConfig.ENABLE_UPDATER) com.example.ui.components.AppUpdateHost(
                    isDark = uiState.themeMode == ThemeMode.DARK ||
                        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                    glassBlurTransparency = uiState.glassBlurTransparency
                )
                if (contentReady) {
                AnimatedContent(
                    modifier = if (splashBlurRadius > 0.dp) Modifier.blur(splashBlurRadius) else Modifier,
                    targetState = currentScreen,
                    transitionSpec = {
                        val speed = uiState.animationSpeed.coerceIn(0.25f, 2f)
                        val duration = { base: Int -> (base / speed).roundToInt().coerceIn(0, 1400) }
                        if (targetState == Screen.PLAYER) {
                            val opening = uiState.videoOpeningAnimation
                            when (opening) {
                                VideoOpeningAnimation.NONE -> ContentTransform(
                                    targetContentEnter = fadeIn(animationSpec = tween(0)),
                                    initialContentExit = fadeOut(animationSpec = tween(0)),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.FADE_FROM_BLACK -> ContentTransform(
                                    targetContentEnter = fadeIn(animationSpec = tween(duration(380), easing = FastOutSlowInEasing)),
                                    initialContentExit = fadeOut(animationSpec = tween(duration(180))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.ZOOM_BURST -> ContentTransform(
                                    targetContentEnter = scaleIn(initialScale = 0.85f, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 0.96f, animationSpec = tween(duration(240))) + fadeOut(tween(duration(160))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.CINEMA_BARS -> ContentTransform(
                                    targetContentEnter = slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(duration(360), easing = CubicBezierEasing(0.12f, 0.95f, 0.22f, 1.0f))) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 0.96f, animationSpec = tween(duration(260))) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.DEFAULT -> ContentTransform(
                                    // Always enters bottom -> up in window coordinates: the exact
                                    // opposite of Back. Landscape videos are drawn landscape inside
                                    // the player during the slide (no rotate/flip), see
                                    // VideoPlayerScreen "Seamless landscape entry".
                                    targetContentEnter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(duration(360), easing = CubicBezierEasing(0.12f, 0.95f, 0.22f, 1.0f))
                                    ),
                                    initialContentExit = scaleOut(
                                        targetScale = 0.97f,
                                        animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                    ) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = 1f
                                )
                                VideoOpeningAnimation.SLIDE_UP -> ContentTransform(
                                    // Keep this option genuinely different from Default so every
                                    // Appearance choice produces a distinct, smooth result.
                                    targetContentEnter = scaleIn(
                                        initialScale = 0.965f,
                                        animationSpec = tween(duration(320), easing = FastOutSlowInEasing)
                                    ) + slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(duration(320), easing = FastOutSlowInEasing)
                                    ) + fadeIn(tween(duration(180))),
                                    initialContentExit = scaleOut(
                                        targetScale = 0.985f,
                                        animationSpec = tween(duration(260), easing = FastOutSlowInEasing)
                                    ) + fadeOut(tween(duration(160))),
                                    targetContentZIndex = 1f
                                )
                            }
                        } else if (initialState == Screen.PLAYER && targetState != Screen.PLAYER) {
                            // Back always dismisses the player straight downward in the current
                            // window coordinates. The destination screen stays underneath, so
                            // the landscape player can slide away while the portrait library is
                            // revealed without rotating that background UI.
                            // Starts moving instantly (no dead first frames) and settles softly; shorter than before so it never feels laggy.
                            val backDuration = duration(420)
                            val backEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
                            ContentTransform(
                                targetContentEnter = EnterTransition.None,
                                // Pure slide, no fade: the player stays fully opaque so portrait
                                // and landscape exits look identical (no see-through background).
                                initialContentExit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(backDuration, easing = backEasing)
                                ),
                                targetContentZIndex = -1f
                            )
                        } else if ((initialState == Screen.SHARED_PLAYLIST || initialState == Screen.CHANNEL_DETAIL) && targetState == Screen.HOME) {
                            // Pure slide, NO fade: both screens stay fully opaque during the move, so the window background never
                            // shows through (a fade-in on one screen + fade-out on the other looked like a blink / flash).
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(
                                    initialOffsetX = { -it / 5 },
                                    animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                ),
                                initialContentExit = slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                ),
                                targetContentZIndex = -1f
                            )
                        } else if (initialState == Screen.HOME && (targetState == Screen.SHARED_PLAYLIST || targetState == Screen.CHANNEL_DETAIL)) {
                            ContentTransform(
                                targetContentEnter = slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                ),
                                initialContentExit = slideOutHorizontally(
                                    targetOffsetX = { -it / 5 },
                                    animationSpec = tween(duration(300), easing = FastOutSlowInEasing)
                                ),
                                targetContentZIndex = 1f
                            )
                        } else {
                            when (uiState.screenNavigationStyle) {
                                ScreenNavigationStyle.NONE -> ContentTransform(fadeIn(tween(0)), fadeOut(tween(0)), targetContentZIndex = -1f)
                                ScreenNavigationStyle.MINIMAL_FADE -> ContentTransform(fadeIn(tween(duration(220))), fadeOut(tween(duration(180))), targetContentZIndex = -1f)
                                ScreenNavigationStyle.DEPTH_ZOOM -> ContentTransform(
                                    targetContentEnter = scaleIn(initialScale = 0.94f, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 0.98f, animationSpec = tween(duration(260))) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                                ScreenNavigationStyle.FLIP_FADE -> ContentTransform(
                                    targetContentEnter = scaleIn(initialScale = 0.90f, animationSpec = tween(duration(300))) + fadeIn(tween(duration(220))),
                                    initialContentExit = scaleOut(targetScale = 1.02f, animationSpec = tween(duration(260))) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                                ScreenNavigationStyle.ELASTIC_SLIDE -> ContentTransform(
                                    targetContentEnter = slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(duration(320), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(duration(280), easing = FastOutSlowInEasing)) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                                ScreenNavigationStyle.DEFAULT -> ContentTransform(
                                    targetContentEnter = slideInHorizontally(initialOffsetX = { it / 8 }, animationSpec = tween(duration(300), easing = FastOutSlowInEasing)) + fadeIn(tween(duration(220))),
                                    initialContentExit = slideOutHorizontally(targetOffsetX = { -it / 10 }, animationSpec = tween(duration(260), easing = FastOutSlowInEasing)) + fadeOut(tween(duration(180))),
                                    targetContentZIndex = -1f
                                )
                            }
                        }
                    },
                    label = "ScreenTransition"
                ) { target ->
                    when (target) {
                        Screen.HOME -> {
                            val currentConfig = LocalConfiguration.current
                            val effectiveConfig = remember(currentConfig) {
                                if (currentConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    currentConfig
                                } else {
                                    Configuration(currentConfig).apply {
                                        orientation = Configuration.ORIENTATION_PORTRAIT
                                        if (screenWidthDp > screenHeightDp) {
                                            val tmp = screenWidthDp
                                            screenWidthDp = screenHeightDp
                                            screenHeightDp = tmp
                                        }
                                    }
                                }
                            }
                            CompositionLocalProvider(
                                LocalConfiguration provides effectiveConfig,
                                com.example.ui.theme.LocalRealConfiguration provides currentConfig
                            ) {
                                HomeScreen(
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onRequestPermissions = { requestMediaPermissions() },
                                    onPlayVideo = { video, playlist ->
                                        com.example.util.MediaSeenManager.markSeen(this@MainActivity, video.path, video.id)
                                        viewModel.startPlaying(video, playlist)
                                    },
                                    onThemeChange = viewModel::setThemeMode,
                                    onScaleChange = viewModel::setAppScale,
                                    onGlassBlurTransparencyChange = viewModel::setGlassBlurTransparency,
                                    onSideBySideChange = viewModel::setForceSideBySide,
                                    onLanguageChange = viewModel::setLanguage,
                                    onFrameStepChange = viewModel::setFrameStepAmount,
                                    onCopyTimestampChange = viewModel::setCopyTimestampOnDoubleTap,
                                    onHwAccelModeChange = viewModel::setHwAccelMode,
                                    onGestureSensitivityModeChange = viewModel::setGestureSensitivityMode
                                )
                            }
                        }
                        Screen.PLAYER -> {
                            // Player notifications / seek badge / toasts are plain Text() outside any Material Surface, so they
                            // fell back to the phone's system font. Give the whole player the app font (system font when that setting is ON).
                            androidx.compose.material3.ProvideTextStyle(
                                androidx.compose.material3.LocalTextStyle.current.copy(
                                    fontFamily = com.example.ui.theme.LocalAppFontFamily.current
                                )
                            ) {
                            VideoPlayerScreen(
                                video = viewModel.activeVideoItem,
                                playlist = viewModel.activeVideoPlaylist,
                                uiState = uiState,
                                onGestureSensitivityModeChange = viewModel::setGestureSensitivityMode,
                                onSeekbarStyleChange = viewModel::setSeekbarStyle,
                                onPlayerLayoutConfigChange = viewModel::setPlayerLayoutConfig,
                                onResetPlayerLayoutConfig = viewModel::resetPlayerLayoutConfig,
                                onLandscapeChanged = { wasLand -> viewModel.isPlayerInLandscape = wasLand },
                                onPlayerSettingsChange = viewModel::syncPlayerSettingsInMemory,
                                onBack = { viewModel.exitPlayer() }
                            )
                            }
                        }
                        Screen.CHANNEL_DETAIL -> {
                            com.example.ui.screens.ChannelDetailScreen(
                                channel = viewModel.activeChannelResult,
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = uiState.themeMode == ThemeMode.DARK ||
                                    (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                                isAudioMode = viewModel.isChannelAudioMode,
                                onBack = { viewModel.exitChannel() },
                                onPlayVideo = { video, playlist ->
                                    com.example.util.MediaSeenManager.markSeen(this@MainActivity, video.path, video.id)
                                    viewModel.startPlaying(video, playlist)
                                }
                            )
                        }
                        Screen.SHARED_PLAYLIST -> {
                            val pl = viewModel.activeSharedPlaylist
                            if (pl != null) {
                                com.example.ui.screens.SharedPlaylistScreen(
                                    playlist = pl,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    isDark = uiState.themeMode == ThemeMode.DARK ||
                                        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                                    onBack = { viewModel.exitSharedPlaylist() }
                                )
                            } else {
                                viewModel.exitSharedPlaylist()
                            }
                        }
                    }
                }
                }
                // Launch animation (cold start only): glass veil over the real UI + logo + app-name text animation
                // Picture-in-Picture of the video mini player: the small window shows only the video, not the whole app.
                com.example.ui.components.MiniPlayerPipSurface(currentScreen != Screen.PLAYER)

                if (showLaunchSplash) {
                    com.example.ui.components.AppLaunchSplash(
                        isDark = uiState.themeMode == ThemeMode.DARK ||
                            (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                        onBlurBackground = { splashBlurOn = it },
                        onFinished = { showLaunchSplash = false }
                    )
                }
                var crashReport by remember { mutableStateOf(previousCrashReport) }
                crashReport?.let {
                    com.example.ui.components.CrashReportDialog(
                        report = it,
                        isDark = uiState.themeMode == ThemeMode.DARK ||
                            (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                        glassBlurTransparency = uiState.glassBlurTransparency
                    ) { crashReport = null }
                }
                // Torrent file picker (play while downloading, or download; magnet / .torrent from the Network tab or from another app)
                com.example.ui.components.TorrentPickerHost(
                    isDark = uiState.themeMode == ThemeMode.DARK ||
                        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
                    glassBlurTransparency = uiState.glassBlurTransparency,
                    onPlayStream = { video -> viewModel.startPlaying(video, listOf(video)) }
                )
            }
        }
    }

    /**
     * Sleep timer reached zero. Closes everything the way the user would by hand:
     * audio stops, the video player leaves with its own exit animation (VideoPlayerScreen),
     * then the app steps back through the folders/screens with the normal Back animation and
     * finally closes. If the screen is off / app not visible there is nothing to animate, so the
     * app is just closed directly (playback is stopped first).
     */
    private fun handleSleepTimerExpired() {
        lifecycleScope.launch {
            try {
                if (com.example.player.AudioPlaybackManager.currentTrack.value != null) {
                    com.example.player.AudioPlaybackManager.closeAndStop(applicationContext)
                }
            } catch (_: Throwable) {}
            try { com.example.player.VideoMiniPlayerManager.close() } catch (_: Throwable) {}

            val visible = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
            if (!visible) {
                finishAndRemoveTask()
                return@launch
            }

            if (viewModel.currentScreenState.value == Screen.PLAYER) {
                // VideoPlayerScreen plays its exit animation and then calls viewModel.exitPlayer().
                val startedAt = System.currentTimeMillis()
                while (viewModel.currentScreenState.value == Screen.PLAYER &&
                    System.currentTimeMillis() - startedAt < 5000L
                ) {
                    delay(100L)
                }
                if (viewModel.currentScreenState.value == Screen.PLAYER) {
                    viewModel.exitPlayer()
                }
                delay(550L)
            } else {
                delay(350L)
            }

            // Press "Back" until nothing is left to go back to (folders -> tab -> exit).
            var presses = 0
            while (!isFinishing && presses < 12) {
                onBackPressedDispatcher.onBackPressed()
                presses++
                delay(450L)
            }
            if (!isFinishing) finishAndRemoveTask()
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPerm = hasVideoPermission(this)
        val permissionJustChanged = hasPerm != viewModel.hasMediaPermission.value
        viewModel.setHasMediaPermission(hasPerm)
        if (hasPerm && permissionJustChanged) {
            // This is the common path: user just came back from the "All Files Access" /
            // permission Settings screen, so do a full rescan immediately rather than
            // waiting for the debounced silent refresh in HomeScreen's own resume observer.
            viewModel.triggerLibraryRefresh()
        }
    }

    private val pipBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.action.PIP_PLAY_PAUSE") {
                if (viewModel.currentScreenState.value != Screen.PLAYER && com.example.player.VideoMiniPlayerManager.isActive) {
                    com.example.player.VideoMiniPlayerManager.togglePlayPause()
                    updateMiniPipActions()
                } else {
                    PipModeState.onPipPlayPauseTriggered()
                }
            }
        }
    }

    fun buildPipParams(isPlaying: Boolean, autoPipEnabled: Boolean): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))

        val actions = ArrayList<RemoteAction>()
        val playPauseIntent = Intent("com.example.action.PIP_PLAY_PAUSE").setPackage(packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            101,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(
            this,
            if (isPlaying) R.drawable.viaura_pause_solid else R.drawable.viaura_play_solid
        )
        val label = if (isPlaying) "Pause" else "Play"
        actions.add(RemoteAction(icon, label, label, pendingIntent))
        builder.setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val isPlayerOnScreen = (viewModel.currentScreenState.value == Screen.PLAYER) && PipModeState.isPlayerActive
            builder.setAutoEnterEnabled(autoPipEnabled && isPlayerOnScreen && isPlaying)
        }
        return builder.build()
    }

    fun updatePipParams(isPlaying: Boolean, autoPipEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams(isPlaying, autoPipEnabled)
                if (params != null) {
                    setPictureInPictureParams(params)
                }
            } catch (_: Throwable) {}
        }
    }

    // Keep the global PiP state flag in sync so the player UI can hide its controls
    // and switch to a minimal tap-to-play/pause surface while in PiP mode.
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipModeState.isInPictureInPicture.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            if (viewModel.currentScreenState.value != Screen.PLAYER && com.example.player.VideoMiniPlayerManager.isActive) {
                updateMiniPipActions()
            } else {
                updatePipParams(PipModeState.isCurrentlyPlaying, PipModeState.autoPipEnabled)
            }
        }
    }

    /**
     * Video mini player running and the app is being left (Home / Back): the video goes on in a Picture-in-Picture popup.
     * Only when "Auto Picture-in-Picture" is ON and the mini video is actually playing.
     */
    private fun tryEnterMiniPip(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (viewModel.currentScreenState.value == Screen.PLAYER) return false
        if (!viewModel.uiState.value.playerSettings.autoPictureInPicture) return false
        val session = com.example.player.VideoMiniPlayerManager.session.value ?: return false
        if (!com.example.player.VideoMiniPlayerManager.currentIsPlaying()) return false
        return try {
            enterPictureInPictureMode(buildMiniPipParams(session, true))
        } catch (_: Throwable) {
            false
        }
    }

    private fun buildMiniPipParams(session: com.example.player.VideoMiniSession, isPlaying: Boolean): PictureInPictureParams {
        val w = try { session.controller.videoWidth.value } catch (_: Throwable) { 0 }
        val h = try { session.controller.videoHeight.value } catch (_: Throwable) { 0 }
        val ratio = if (w > 0 && h > 0) (w.toFloat() / h.toFloat()).coerceIn(0.45f, 2.3f) else 16f / 9f
        val pending = PendingIntent.getBroadcast(
            this, 101,
            Intent("com.example.action.PIP_PLAY_PAUSE").setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = if (isPlaying) "Pause" else "Play"
        val icon = Icon.createWithResource(this, if (isPlaying) R.drawable.viaura_pause_solid else R.drawable.viaura_play_solid)
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
            .setActions(arrayListOf(RemoteAction(icon, label, label, pending)))
            .build()
    }

    private fun updateMiniPipActions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val session = com.example.player.VideoMiniPlayerManager.session.value ?: return
        try {
            setPictureInPictureParams(buildMiniPipParams(session, com.example.player.VideoMiniPlayerManager.currentIsPlaying()))
        } catch (_: Throwable) {}
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (tryEnterMiniPip()) return
        val isPlayerScreen = (viewModel.currentScreenState.value == Screen.PLAYER)
        if (isPlayerScreen && PipModeState.isPlayerActive && PipModeState.autoPipEnabled && PipModeState.isCurrentlyPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = buildPipParams(isPlaying = true, autoPipEnabled = true)
                if (params != null) {
                    enterPictureInPictureMode(params)
                }
            } catch (_: Throwable) {}
        }
    }

    override fun onDestroy() {
        // The app is really closing (not a rotation): the video mini player ends with it.
        if (isFinishing) {
            try { com.example.player.VideoMiniPlayerManager.close() } catch (_: Throwable) {}
        }
        super.onDestroy()
        PipModeState.requestPipUpdate = null
        try {
            unregisterReceiver(pipBroadcastReceiver)
        } catch (_: Throwable) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("EXTRA_OPEN_AUDIO_PLAYER", false)) {
            com.example.player.AudioPlaybackManager.openFullScreen()
        }
        val action = intent.action ?: return
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return
        }

        // magnet link / .torrent file or link: the torrent file picker (play or download; the same Intent object is handed to a re-created
        // Activity after rotation, so it is handled only once)
        com.example.torrent.torrentSourceFromIntent(intent)?.let { torrentSource ->
            if (intent.getBooleanExtra("viaura_torrent_handled", false)) return
            intent.putExtra("viaura_torrent_handled", true)
            com.example.torrent.TorrentPickerState.open(torrentSource)
            return
        }

        // Check if a shared link from YouTube Music / Spotify / YouTube was received
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.getItemAt(0)?.text?.toString()
            ?: intent.dataString

        val extractedUrl = com.example.util.OnlineMusicService.extractUrlFromText(sharedText)
        val lowerSharedUrl = extractedUrl.orEmpty().lowercase(java.util.Locale.ROOT)
        val isYouTubeShare = lowerSharedUrl.contains("youtube.com") || lowerSharedUrl.contains("youtu.be/")
        val isSpotifyCollection = (lowerSharedUrl.contains("open.spotify.com/") || lowerSharedUrl.contains("spotify.link/")) &&
            (lowerSharedUrl.contains("/playlist/") || lowerSharedUrl.contains("/album/"))
        val isSpotifyTrack = lowerSharedUrl.contains("open.spotify.com/track/")
        if (extractedUrl != null && (isYouTubeShare || isSpotifyCollection || isSpotifyTrack) &&
            com.example.util.OnlineMusicService.isMusicPlaylistOrTrackUrl(extractedUrl)) {
            // A single shared video / song: YouTube Music and Spotify tracks go to Audio; normal YouTube goes to Video.
            val sharedVideoId = com.example.util.OnlineMusicService.extractVideoId(extractedUrl)
            if (isSpotifyTrack) {
                if (intent.getBooleanExtra("viaura_share_handled", false)) return
                intent.putExtra("viaura_share_handled", true)
                handleSharedSpotifyTrack(extractedUrl)
                return
            }
            if (sharedVideoId != null) {
                // The same Intent object is handed to a re-created Activity (rotation / theme change): play a share only once.
                if (intent.getBooleanExtra("viaura_share_handled", false)) return
                intent.putExtra("viaura_share_handled", true)
                handleSharedSingleMedia(extractedUrl, sharedVideoId)
                return
            }
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(applicationContext, "Loading shared playlist...", android.widget.Toast.LENGTH_SHORT).show()
                }
                val pl = com.example.util.OnlineMusicService.fetchPlaylistDetails(extractedUrl)
                withContext(Dispatchers.Main) {
                    if (pl != null && pl.tracks.isNotEmpty()) {
                        viewModel.openSharedPlaylist(pl)
                    } else {
                        android.widget.Toast.makeText(applicationContext, "Could not load playlist from link", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }

        // Other shared HTTP(S) URLs (Instagram, Facebook, TikTok and supported providers) use the same
        // yt-dlp / direct-stream resolver as the Network tab. The cache lets the player reuse this probe.
        if (extractedUrl != null && (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE || action == Intent.ACTION_VIEW)) {
            val normalizedSharedUrl = com.example.util.NetworkStreamResolver.normalizeUrl(extractedUrl)
            if (normalizedSharedUrl != null && intent.type != "application/x-bittorrent" &&
                !intent.getBooleanExtra("viaura_generic_link_handled", false)) {
                intent.putExtra("viaura_generic_link_handled", true)
                handleSharedGenericLink(normalizedSharedUrl)
                return
            }
        }

        val targetUri: Uri? = when (action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.firstOrNull() ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
            }
            else -> intent.data ?: intent.clipData?.getItemAt(0)?.uri
        }

        if (targetUri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    targetUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {}

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val videoItem = resolveMediaUri(targetUri, intent.type)
                    withContext(Dispatchers.Main) {
                        viewModel.startPlaying(videoItem, listOf(videoItem))
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("MainActivity", "Failed to resolve incoming media URI: $targetUri", e)
                }
            }
        }
    }

    /** Resolve a Spotify track share to the same Audio Player used by Online Music. */
    private fun handleSharedSpotifyTrack(url: String) {
        lifecycleScope.launch {
            android.widget.Toast.makeText(applicationContext, "Opening Spotify track…", android.widget.Toast.LENGTH_SHORT).show()
            val detail = withContext(Dispatchers.IO) {
                com.example.util.OnlineMusicService.fetchPlaylistDetails(url)
            }
            if (detail == null) {
                android.widget.Toast.makeText(applicationContext, "Could not load this Spotify track", android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            val result = detail.tracks.firstOrNull()
            if (result == null) {
                android.widget.Toast.makeText(applicationContext, "Could not load this Spotify track", android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            val track = com.example.ui.screens.AudioTrackItem(
                id = -(kotlin.math.abs(result.videoUrl.hashCode().toLong())) - 1L,
                uri = Uri.EMPTY,
                title = result.title,
                artist = result.artist.ifBlank { detail.author.ifBlank { "Spotify" } },
                album = detail.title,
                durationMs = result.durationSec * 1000L,
                path = "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${result.videoUrl}",
                sizeBytes = 0L,
                format = "STREAM",
                thumbnailUrl = result.thumbnailUrl
            )
            com.example.util.OnlineTrackMetadataCache.saveTracks(applicationContext, listOf(track))
            com.example.player.AudioPlaybackManager.openFullScreen()
            com.example.player.AudioPlaybackManager.playTrack(applicationContext, track, listOf(track))
        }
    }

    /** Resolve a shared website URL with the same resolver used by the Network tab. */
    private fun handleSharedGenericLink(url: String) {
        lifecycleScope.launch {
            android.widget.Toast.makeText(applicationContext, "Opening shared link…", android.widget.Toast.LENGTH_SHORT).show()
            val stream = withContext(Dispatchers.IO) {
                try {
                    com.example.util.NetworkStreamResolver.resolve(applicationContext, url)
                } catch (error: Throwable) {
                    android.util.Log.e("MainActivity", "Failed to resolve shared URL: $url", error)
                    null
                }
            }
            if (stream == null) {
                android.widget.Toast.makeText(
                    applicationContext,
                    com.example.util.NetworkStreamResolver.lastError ?: "Could not open this shared link",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            com.example.util.RecentStreamStore.record(applicationContext, url, stream.title, stream.uploader, stream.thumbnailUrl)
            if (stream.isAudio) {
                val track = com.example.ui.screens.AudioTrackItem(
                    id = -(kotlin.math.abs(("shared-audio:" + url).hashCode().toLong())) - 1L,
                    uri = Uri.parse(stream.videoUrl),
                    title = stream.title,
                    artist = stream.uploader ?: "Online Audio",
                    album = "Shared Links",
                    durationMs = stream.durationSec * 1000L,
                    path = url,
                    sizeBytes = 0L,
                    format = "STREAM",
                    thumbnailUrl = stream.thumbnailUrl
                )
                com.example.player.AudioPlaybackManager.openFullScreen()
                com.example.player.AudioPlaybackManager.playTrack(applicationContext, track, listOf(track))
            } else {
                val item = VideoItem(
                    id = -(kotlin.math.abs(("shared-video:" + url).hashCode().toLong())) - 1L,
                    uri = Uri.parse(url),
                    displayName = stream.title.ifBlank { url },
                    path = url,
                    sizeBytes = 0L,
                    durationMs = stream.durationSec * 1000L,
                    dateModified = System.currentTimeMillis() / 1000L,
                    isOnline = true,
                    thumbnailUrl = stream.thumbnailUrl,
                    channelName = stream.uploader,
                    isNetworkStream = true
                )
                viewModel.startPlaying(item, listOf(item))
            }
        }
    }

    /**
     * One video / song shared from YouTube or YouTube Music.
     * - YouTube Music link  -> opens in the music (audio) player, with related songs queued after it.
     * - YouTube link        -> plays as an online video in the normal video player.
     */
    private fun handleSharedSingleMedia(url: String, videoId: String) {
        val watchUrl = com.example.util.OnlineMusicService.canonicalWatchUrl(videoId)
        val isMusic = com.example.util.OnlineMusicService.isYouTubeMusicUrl(url)
        lifecycleScope.launch {
            android.widget.Toast.makeText(
                applicationContext,
                if (isMusic) "Loading shared song..." else "Loading shared video...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            val meta = withContext(Dispatchers.IO) {
                com.example.util.OnlineMusicService.fetchSingleVideoInfo(watchUrl)
            }
            val result = meta ?: com.example.util.OnlineTrackResult(
                videoUrl = watchUrl, title = watchUrl, artist = "", durationSec = 0L, thumbnailUrl = null
            )
            if (isMusic) playSharedSong(result) else playSharedVideo(result)
        }
    }

    private fun playSharedVideo(result: com.example.util.OnlineTrackResult) {
        try {
            com.example.util.OnlineVideoRecommender.recordOpened(applicationContext, result.videoUrl, result.artist, result.title)
        } catch (_: Throwable) {}
        val item = result.toOnlineVideoItem()
        viewModel.startPlaying(item, listOf(item))
    }

    private fun playSharedSong(result: com.example.util.OnlineTrackResult) {
        val stableId = -(kotlin.math.abs(result.videoUrl.hashCode().toLong()))
        val track = com.example.ui.screens.AudioTrackItem(
            id = stableId,
            uri = Uri.EMPTY,
            title = result.title,
            artist = result.artist.ifBlank { "YouTube Music" },
            album = "Online Music",
            durationMs = result.durationSec * 1000L,
            path = "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${result.videoUrl}",
            sizeBytes = 0L,
            format = "STREAM",
            audioType = "Stereo",
            hasLyrics = false,
            lyricsText = "",
            thumbnailUrl = result.thumbnailUrl
        )
        // The audio full-screen player lives on Home / Channel screens: leave the video player first.
        if (viewModel.currentScreenState.value == Screen.PLAYER) viewModel.exitPlayer()
        com.example.player.AudioPlaybackManager.openFullScreen()
        com.example.player.AudioPlaybackManager.playTrack(applicationContext, track, listOf(track))

        lifecycleScope.launch(Dispatchers.IO) {
            // Lyrics are fetched centrally by AudioPlaybackManager.loadLyricsFor()
            try {
                val related = com.example.util.OnlineMusicService.resolveStreamWithRelated(result.videoUrl).relatedTracks
                    .filter { it.videoUrl != result.videoUrl && it.title.isNotBlank() }
                    .distinctBy { it.videoUrl }
                    .let {
                        // other artists first, the shared song's artist only comes back every few tracks
                        com.example.util.OnlineTrackSequencer.arrange(
                            it, com.example.util.OnlineTrackSequencer.creatorKey(result.artist), result.videoUrl
                        )
                    }
                    .take(15)
                val relatedItems = related.map { item ->
                    com.example.ui.screens.AudioTrackItem(
                        id = -(kotlin.math.abs(item.videoUrl.hashCode().toLong())),
                        uri = Uri.EMPTY,
                        title = item.title,
                        artist = item.artist,
                        album = "Online Music",
                        durationMs = item.durationSec * 1000L,
                        path = "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${item.videoUrl}",
                        sizeBytes = 0L,
                        format = "STREAM",
                        audioType = "Stereo",
                        thumbnailUrl = item.thumbnailUrl
                    )
                }
                if (relatedItems.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val currentQ = com.example.player.AudioPlaybackManager.queue.value
                        val existingIds = currentQ.map { it.id }.toSet()
                        val toAdd = relatedItems.filter { it.id !in existingIds }
                        if (toAdd.isNotEmpty() &&
                            com.example.player.AudioPlaybackManager.currentTrack.value?.id == stableId
                        ) {
                            com.example.player.AudioPlaybackManager.updateQueue(currentQ + toAdd)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
    }

    private fun resolveMediaUri(uri: Uri, mimeType: String?): VideoItem {
        var displayName = ""
        var sizeBytes = 0L

        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeCol = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (nameCol != -1) displayName = cursor.getString(nameCol) ?: ""
                        if (sizeCol != -1) sizeBytes = cursor.getLong(sizeCol)
                    }
                }
            } catch (_: Throwable) {}
        }

        if (displayName.isBlank()) {
            val lastSegment = uri.lastPathSegment ?: ""
            displayName = if (lastSegment.isNotBlank()) {
                if (lastSegment.contains("/")) lastSegment.substringAfterLast("/") else lastSegment
            } else {
                "Media_${System.currentTimeMillis()}"
            }
        }

        val directPath = if (uri.scheme == "file") uri.path ?: uri.toString() else uri.toString()

        val extracted = com.example.util.MediaMetadataExtractor.extractMetadata(
            filePath = directPath,
            context = applicationContext,
            uri = uri,
            displayName = displayName,
            sizeBytes = sizeBytes,
            dateModified = System.currentTimeMillis()
        )

        return VideoItem(
            id = System.currentTimeMillis(),
            uri = uri,
            displayName = displayName,
            path = directPath,
            sizeBytes = if (sizeBytes > 0) sizeBytes else 0L,
            durationMs = extracted.durationMs,
            dateModified = System.currentTimeMillis(),
            isNew = false,
            resolution = extracted.resolution,
            framerate = extracted.framerate,
            hasSubtitles = extracted.effectiveSubtitleFormats.isNotEmpty() || extracted.embeddedSubtitleFormats.isNotEmpty() || extracted.externalSubtitleFormats.isNotEmpty(),
            subtitleFormats = extracted.effectiveSubtitleFormats,
            watchedProgress = 0.0f,
            embeddedSubtitleFormats = extracted.embeddedSubtitleFormats,
            externalSubtitleFormats = extracted.externalSubtitleFormats,
            embeddedFontCount = extracted.embeddedFontCount
        )
    }
}

