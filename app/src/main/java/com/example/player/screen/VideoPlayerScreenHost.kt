package com.example.ui.screens

import com.example.ai.AiFeaturesSettingsStore
import com.example.ai.TrackTranslationStatus
import com.example.ai.TranslateLanguages
import com.example.ui.components.StyledIcon
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.Neutral
import com.example.ui.state.ThemeMode
import com.example.ui.components.HeaderBar
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VisibleFields
import com.example.ui.components.DownloadGlassSheet
import com.example.ui.components.glassPanelSurface
import com.example.ui.components.bounceClick
import com.example.util.OnlineWatchDetails
import com.example.util.OnlineCommentPage
import com.example.util.OnlineTrackResult
import com.example.ui.components.toOnlineVideoItem
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ui.components.AudioFileTreeDialog
import com.example.ui.components.SubtitleFileTreeDialog
import androidx.core.view.WindowInsetsControllerCompat
import com.example.player.AudioLanguageMatcher
import com.example.player.OnlineAudioSource
import com.example.player.PlayerVideoChapter
import com.example.util.OnlineVideoPlayback
import com.example.util.OnlineVideoService
import com.example.util.OnlineVideoQuality
import com.example.player.OnlineSkipMarkerService
import com.example.player.EqualizerPreset
import com.example.player.TrackAudioConfig
import com.example.player.AudioChannelMode
import com.example.player.VideoFilterPreset
import com.example.player.ManualVideoAdjustments
import com.example.player.MpvPlayerController
import com.example.player.SubtitleFontManager
import com.example.player.VideoPlayerMpvView
import com.example.player.ChapterSkipDetector
import com.example.player.ChapterSkipMarker
import com.example.ui.components.ThumbnailCache
import com.example.ui.components.loadVideoThumbnail
import com.example.ui.state.ControlsAnimationStyle
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.HwAccelMode
import com.example.ui.state.PlayerSettings
import com.example.ui.state.UiState
import com.example.MainActivity
import com.example.util.network.NetworkMonitor
import com.example.util.media.ServerReputationStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.ui.components.ViauraExpressiveLoadingIndicator
import com.example.ui.components.loadRemoteAudioThumbnail
import kotlinx.coroutines.withContext
import com.example.util.PipModeState
import com.example.util.PlaybackHistoryManager
import com.example.util.CustomSubtitlePersistenceManager
import com.example.util.AttachedSubtitleRecord
import com.example.util.SavedSubtitleRef
import com.example.util.VideoSubtitleSelection
import com.example.player.PlayerMediaTrack
import com.example.util.SubtitleSessionMemory
import com.example.util.VideoAudioSelectionMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

@Composable
fun VideoPlayerScreen(
    video: VideoItem?,
    playlist: List<VideoItem> = emptyList(),
    uiState: UiState,
    onGestureSensitivityModeChange: (GestureSensitivityMode) -> Unit = {},
    onSeekbarStyleChange: (com.example.ui.state.SeekbarStyle) -> Unit = {},
    onPlayerLayoutConfigChange: (com.example.ui.state.PlayerLayoutConfig) -> Unit = {},
    onResetPlayerLayoutConfig: () -> Unit = {},
    onLandscapeChanged: (Boolean) -> Unit = {},
    onPlayerSettingsChange: (PlayerSettings) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Thin entry point that only resolves the default values. The real player below has NO default parameters: R8 produced
    // invalid bytecode for the default-value prologue of this very large function (VerifyError when a video was opened).
    VideoPlayerScreenImpl(
        video, playlist, uiState, onGestureSensitivityModeChange, onSeekbarStyleChange, onPlayerLayoutConfigChange,
        onResetPlayerLayoutConfig, onLandscapeChanged, onPlayerSettingsChange, onBack, modifier
    )
}

/**
 * Ultra-Fast, Zero-Lag Video Player Screen with Native MPV & ASS Subtitle Engine
 */
@Composable
private fun VideoPlayerScreenImpl(
    video: VideoItem?,
    playlist: List<VideoItem>,
    uiState: UiState,
    onGestureSensitivityModeChange: (GestureSensitivityMode) -> Unit,
    onSeekbarStyleChange: (com.example.ui.state.SeekbarStyle) -> Unit,
    onPlayerLayoutConfigChange: (com.example.ui.state.PlayerLayoutConfig) -> Unit,
    onResetPlayerLayoutConfig: () -> Unit,
    onLandscapeChanged: (Boolean) -> Unit,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    // Every background job of the player (stream resolving, subtitles, HLS lists ...) runs in this scope. An exception that
    // nobody caught there used to kill the whole app the moment a video was opened; now it is only logged.
    val coroutineScope = rememberCoroutineScope {
        kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
            android.util.Log.e("VideoPlayerScreen", "Uncaught error in a player job (ignored)", t)
        }
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var wasInLandscape by remember { mutableStateOf(false) }

    var waitForPortraitBeforeBack by remember { mutableStateOf(false) }
    // Haptic feedback is for the app UI only: keep every player control / gesture silent while this screen is open.
    DisposableEffect(Unit) {
        com.example.util.AppHaptics.inVideoPlayer = true
        onDispose { com.example.util.AppHaptics.inVideoPlayer = false }
    }

    var entryLoadDelayDone by remember { mutableStateOf(false) }

    // ---- Seamless landscape entry -------------------------------------------------------
    // Exact mirror of the landscape exit: when this player is going to be landscape but the
    // window is still portrait, the player slides in (bottom -> up) already drawn landscape
    // via a Compose rotation, and only afterwards is the real window orientation switched
    // (with the system rotation animation disabled), so the user never sees a rotate/flip.
    val entryRequestedMode = uiState.playerSettings.orientation.uppercase()
    val entryVideoIsWide = remember(video?.path) {
        val m = Regex("(\\d+)\\s*[x\u00D7]\\s*(\\d+)").find(video?.resolution.orEmpty())
        val w = m?.groupValues?.getOrNull(1)?.toLongOrNull()
        val h = m?.groupValues?.getOrNull(2)?.toLongOrNull()
        w != null && h != null && w > h
    }
    val entryWantsLandscape = entryRequestedMode == "LANDSCAPE" ||
        entryRequestedMode == "REVERSE_LANDSCAPE" ||
        entryRequestedMode == "SENSOR_LANDSCAPE" ||
        (entryRequestedMode == "VIDEO" && entryVideoIsWide)
    var entryFakeLandscape by remember { mutableStateOf(entryWantsLandscape && !isLandscape && !uiState.playerSettings.portraitChannelPlayer) }
    val entryFakeRotation = if (entryRequestedMode == "REVERSE_LANDSCAPE") -90f else 90f

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            wasInLandscape = true
        }
        onLandscapeChanged(isLandscape)
    }

    // Physical orientation follows the selected Player Settings mode.
    DisposableEffect(uiState.playerSettings.orientation, uiState.playerSettings.portraitChannelPlayer) {
        val requested = when (uiState.playerSettings.orientation) {
            "VIDEO" -> if (entryVideoIsWide || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "REVERSE_PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "SENSOR_PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "REVERSE_LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "SENSOR_LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        // While the fake-landscape entry is running the window stays portrait; the real
        // orientation is applied by the hand-off effect below.
        // Portrait channel player: a landscape-only setting must not force the channel layout sideways, it follows the sensor.
        val requestedFinal = if (uiState.playerSettings.portraitChannelPlayer &&
            (requested == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                requested == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE ||
                requested == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        ) ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR else requested
        if (!entryFakeLandscape) {
            activity?.requestedOrientation = requestedFinal
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Hand-off: after the slide-in finishes, switch the window to real landscape without the
    // system rotation animation, then drop the Compose rotation on the very next frame.
    LaunchedEffect(Unit) {
        if (!entryFakeLandscape) return@LaunchedEffect
        val speed = uiState.animationSpeed.coerceIn(0.25f, 2f)
        kotlinx.coroutines.delay((420 / speed).toLong())
        activity?.window?.let { window ->
            val params = window.attributes
            params.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
            window.attributes = params
        }
        activity?.requestedOrientation = if (entryRequestedMode == "REVERSE_LANDSCAPE") {
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        // Safety net: never stay in the fake-landscape state if the rotation never arrives.
        kotlinx.coroutines.delay(1800)
        entryFakeLandscape = false
    }
    LaunchedEffect(entryFakeLandscape, isLandscape) {
        if (entryFakeLandscape && isLandscape) {
            androidx.compose.runtime.withFrameNanos { }
            entryFakeLandscape = false
            activity?.window?.let { window ->
                val params = window.attributes
                params.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_ROTATE
                window.attributes = params
            }
            if (entryRequestedMode == "SENSOR_LANDSCAPE") {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }
    }

    // System Action Notification Banner State
    var playerNotificationMessage by remember { mutableStateOf<String?>(null) }
    var playerNotificationKey by remember { mutableLongStateOf(0L) }

    val showNotification: (String) -> Unit = { msg ->
        if (uiState.showPlayerNotifications && msg.isNotBlank()) {
            playerNotificationMessage = msg
            playerNotificationKey = System.currentTimeMillis()
        }
    }

    // Active playlist and video index
    var currentPlaylist by remember(playlist, video) {
        mutableStateOf(
            if (playlist.isNotEmpty()) playlist
            else listOfNotNull(video)
        )
    }

    var currentVideoIndex by remember(video, playlist) {
        val initialList = if (playlist.isNotEmpty()) playlist else listOfNotNull(video)
        // Match by path / uri first: ids are not guaranteed unique (files without a MediaStore id,
        // rebuilt history items), and an id-only match could open an earlier episode of the list.
        val target = video
        val byPath = if (target != null && target.path.isNotBlank()) initialList.indexOfFirst { it.path == target.path } else -1
        val byUri = if (byPath < 0 && target != null && target.uri != Uri.EMPTY) initialList.indexOfFirst { it.uri == target.uri } else -1
        val idx = when {
            byPath >= 0 -> byPath
            byUri >= 0 -> byUri
            else -> initialList.indexOfFirst { it.id == video?.id }
        }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    val currentVideo = remember(currentVideoIndex, currentPlaylist, video) {
        currentPlaylist.getOrNull(currentVideoIndex) ?: video
    }

    // Pinned video layout (Settings > Player > Display & Controls).
    // Works for every kind of video: online YouTube, local files, Network tab links, streams and provider (plugin) media.
    val channelEligible = uiState.playerSettings.portraitChannelPlayer && currentVideo != null

    // Always-fresh view of the episode that is playing RIGHT NOW. persistPlaybackPosition() is also
    // called from lifecycle / dispose callbacks that were created when the player first opened; reading
    // `currentVideo` directly there kept saving the FIRST episode (with the last episode's position)
    // after the user had moved on with the next-episode button.
    val persistVideoState by rememberUpdatedState(currentVideo)
    val persistSaveOnQuitState by rememberUpdatedState(uiState.playerSettings.savePositionOnQuit)

    val initialSavedPosition = remember(currentVideo, uiState.playerSettings.savePositionOnQuit) {
        if (uiState.playerSettings.savePositionOnQuit && currentVideo != null) {
            val p = PlaybackHistoryManager.getSavedPosition(
                context = context,
                videoId = currentVideo.id,
                path = currentVideo.path,
                uriString = currentVideo.uri.toString(),
                title = currentVideo.displayName
            )
            if (p > 500L) p else 0L
        } else 0L
    }

    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember(currentVideo) { mutableLongStateOf(initialSavedPosition) }
    var durationMs by remember(currentVideo) {
        mutableLongStateOf(if ((currentVideo?.durationMs ?: 0L) > 0L) currentVideo?.durationMs ?: 0L else 0L)
    }
    var watchedThresholdReached by remember(currentVideo) { mutableStateOf(false) }
    var playbackSpeed by remember { mutableDoubleStateOf(1.0) }
    // Decoder choice is remembered: AUTO = Smart Auto Playback, HW+ / HW / SW = the manual decoder modes.
    val smartPrefs = remember { context.getSharedPreferences("smart_playback", android.content.Context.MODE_PRIVATE) }
    var decoderMode by remember {
        mutableStateOf(
            runCatching { DecoderMode.valueOf(smartPrefs.getString("decoder_mode", DecoderMode.AUTO.name) ?: DecoderMode.AUTO.name) }
                .getOrDefault(DecoderMode.AUTO)
        )
    }
    var repeatMode by remember { mutableStateOf(PlayerRepeatMode.OFF) }
    var isShuffle by remember { mutableStateOf(false) }
    var isAudioOnly by remember { mutableStateOf(false) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }

    // Video Zoom & Pan State (Realtime Interactive Scaling)
    var videoZoomScale by remember { mutableFloatStateOf(1.0f) }
    var videoPanOffset by remember { mutableStateOf(Offset.Zero) }
    var isPanAndZoomEnabled by remember { mutableStateOf(false) }
    var isVideoFlippedVertically by remember(currentVideo) { mutableStateOf(false) }
    var isVideoMirrored by remember(currentVideo) { mutableStateOf(false) }
    var isPitchCorrectionEnabled by remember { mutableStateOf(true) }
    // Ambience Mode (live colour glow in the black side bars) - remembered between videos / app launches
    val ambiencePrefs = remember { context.getSharedPreferences("viaura_player_extras", android.content.Context.MODE_PRIVATE) }
    var isAmbienceEnabled by remember { mutableStateOf(ambiencePrefs.getBoolean("ambience_mode", false)) }

    // A-B Repeat Loop points
    var loopPointA by remember { mutableStateOf<Long?>(null) }
    var loopPointB by remember { mutableStateOf<Long?>(null) }

    // Video Adjustments
    val initialBrightness = remember(currentVideo, uiState.playerSettings.rememberBrightness) {
        val remembered = if (uiState.playerSettings.rememberBrightness) {
            context.getSharedPreferences("viaura_display", Context.MODE_PRIVATE)
                .getFloat("remembered_brightness", Float.NaN)
                .takeIf { it.isFinite() }
        } else null
        remembered ?: run {
            val wBright = activity?.window?.attributes?.screenBrightness ?: -1f
            if (wBright in 0.01f..1.0f) wBright
            else {
            try {
                val sys = android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS
                )
                (sys / 255f).coerceIn(0.05f, 1.0f)
                } catch (_: Throwable) {
                    0.5f
                }
            }
        }
    }
    var brightness by remember { mutableFloatStateOf(initialBrightness) }
    var contrast by remember { mutableFloatStateOf(1.0f) }
    var saturation by remember { mutableFloatStateOf(1.0f) }

    // Audio & Subtitle tracks
    var hasManualAudioTrackSelection by remember(currentVideo) { mutableStateOf(false) }
    var selectedAudioTrackId by remember { mutableIntStateOf(1) }
    var selectedSubtitleTrackId by remember(currentVideo) { mutableIntStateOf(0) }
    var lastTouchedSubtitleTrackId by remember(currentVideo) { mutableIntStateOf(0) }
    var translationStatusByTrackId by remember {
        mutableStateOf<Map<Int, TrackTranslationStatus>>(emptyMap())
    }
    var isSubtitleVisible by remember(currentVideo) { mutableStateOf(false) }
    var subtitleOffsetMs by remember { mutableLongStateOf(0L) }
    var audioDelayMs by remember { mutableLongStateOf(0L) }
    var subtitleScale by remember(uiState.playerSettings.subtitleScale) { mutableFloatStateOf(uiState.playerSettings.subtitleScale) }
    var subtitlePosition by remember(uiState.playerSettings.subtitlePosition) { mutableFloatStateOf(uiState.playerSettings.subtitlePosition) }
    var liveSubtitleSettings by remember { mutableStateOf(uiState.playerSettings) }
    var showSubtitleFileTree by remember { mutableStateOf(false) }
    var showAudioFileTree by remember { mutableStateOf(false) }

    // Audio Equalizer & Volume Boost
    var equalizerEnabled by remember { mutableStateOf(uiState.playerSettings.equalizerEnabled) }
    var eq60Hz by remember { mutableFloatStateOf(uiState.playerSettings.equalizer60Hz) }
    var eq230Hz by remember { mutableFloatStateOf(uiState.playerSettings.equalizer230Hz) }
    var eq910Hz by remember { mutableFloatStateOf(uiState.playerSettings.equalizer910Hz) }
    var eq3600Hz by remember { mutableFloatStateOf(uiState.playerSettings.equalizer3600Hz) }
    var eq14000Hz by remember { mutableFloatStateOf(uiState.playerSettings.equalizer14000Hz) }
    var volumeBoostDb by remember { mutableFloatStateOf(uiState.playerSettings.equalizerVolumeBoostDb) }
    var equalizerPreset by remember { mutableStateOf(uiState.playerSettings.equalizerPreset) }
    var trackAudioConfigs by remember {
        mutableStateOf(PlayerSettings.parseTrackAudioConfigs(uiState.playerSettings.trackAudioConfigsJson))
    }

    // Video Filter Presets & Manual Video Adjustments
    var videoFilterPreset by remember {
        mutableStateOf(VideoFilterPreset.fromId(uiState.playerSettings.videoFilterPreset))
    }
    var manualVideoAdjustments by remember {
        mutableStateOf(
            ManualVideoAdjustments(
                brightness = uiState.playerSettings.videoBrightness,
                contrast = uiState.playerSettings.videoContrast,
                saturation = uiState.playerSettings.videoSaturation,
                gamma = uiState.playerSettings.videoGamma,
                sharpness = uiState.playerSettings.videoSharpness,
                hue = uiState.playerSettings.videoHue,
                temperature = uiState.playerSettings.videoTemperature,
                tint = uiState.playerSettings.videoTint,
                deband = uiState.playerSettings.videoDeband
            )
        )
    }

    val videoTitle = currentVideo?.displayName ?: "Video"
    val totalEpisodes = if (currentPlaylist.isNotEmpty()) currentPlaylist.size else 1
    val currentEpisodeIndex = if (currentPlaylist.isNotEmpty()) currentVideoIndex + 1 else 1

    // Video mini player: when this video was parked in the mini player, its controller + native view are taken back
    // as they are (no reload, playback continues from the very same frame).
    val miniAdopted = remember { com.example.player.VideoMiniPlayerManager.claimFor(video) }
    val miniViewHolder = remember { arrayOfNulls<VideoPlayerMpvView>(1) }
    var miniParkRequested by remember { mutableStateOf(false) }
    var miniParked by remember { mutableStateOf(false) }

    var lastLoadedVideoKey by remember { mutableStateOf<String?>(miniAdopted?.loadedKey) }
    // The parked video is adopted ONCE as it is, even when the list it is opened from puts it at another index
    // (the load key contains the index); otherwise it would reload and restart.
    var miniAdoptPending by remember { mutableStateOf(miniAdopted != null) }

    // ---- Online video (YouTube via NewPipeExtractor): resolved streams of the video that is playing ----
    var onlinePlayback by remember(currentVideo) { mutableStateOf<OnlineVideoPlayback?>(null) }
    var selectedOnlineQuality by remember(currentVideo) { mutableStateOf<String?>(null) }
    // true = "Auto": quality follows the internet speed (default until the user picks a fixed quality)
    var onlineAutoMode by remember(currentVideo) { mutableStateOf(false) }
    val onlinePrefs = remember { context.getSharedPreferences("viaura_online_video", android.content.Context.MODE_PRIVATE) }
    // HTTP headers of a Network-tab link (resolved link); re-applied on every quality switch (setOnlineStream clears them)
    var networkRequestHeaders by remember(currentVideo) { mutableStateOf<Map<String, String>>(emptyMap()) }
    // true = the video is a Network-tab link: mpv uses the mpvRx web-link network settings; re-applied on every quality switch
    var webLinkPlayback by remember(currentVideo) { mutableStateOf(false) }
    // Media-plugin video: the qualities that plugin offers for this movie / episode (player quality menu) + its subtitles
    val pluginSession = remember(currentVideo) {
        currentVideo?.takeIf { it.isDirectStream }?.let { com.example.util.media.PluginPlaybackSessions.get(it.path) }
    }
    var pluginQuality by remember(currentVideo) { mutableStateOf(pluginSession?.selectedQuality) }
    // Provider stream: server that plays, HLS renditions (the Quality list), and which of them plays ("auto" or "hls:<bandwidth>:<height>")
    // (not keyed by the video: they are written from the coroutine that opens the stream, which starts before the
    // screen has recomposed for the new episode; every open of a provider stream sets them again)
    var pluginServerIndex by remember { mutableIntStateOf(pluginSession?.selectedServer ?: 0) }
    var hlsVariants by remember { mutableStateOf<List<com.example.util.media.HlsVariant>>(emptyList()) }
    var hlsSelectedKey by remember { mutableStateOf("auto") }
    var hlsCurrentBandwidth by remember { mutableStateOf<Long?>(null) }
    val pluginVideoPrefs = remember { context.getSharedPreferences("viaura_plugin_video", android.content.Context.MODE_PRIVATE) }
    // queued episodes (series) that are being resolved right now, so a double tap on Next does not open one twice
    val resolvingQueued = remember { HashSet<String>() }
    // true while the player itself is fetching + racing the servers of the episode that is about to open
    // (shown as a small "Connecting to servers…" spinner, nothing blocks the screen); the message is set when it failed
    var queuedResolving by remember { mutableStateOf(false) }
    var queuedResolveError by remember { mutableStateOf<String?>(null) }

    // Video thumbnail / first frame state
    val thumbnailCacheKey = remember(currentVideo, uiState.thumbnailStrategy, uiState.thumbnailQuality, uiState.thumbnailFallbackSecond) {
        currentVideo?.let { "${it.id}|${it.path}|${uiState.thumbnailStrategy.name}|${uiState.thumbnailQuality.name}" }
    }
    var thumbnailBitmap by remember(thumbnailCacheKey) {
        mutableStateOf(
            currentVideo?.let {
                ThumbnailCache.getCachedThumbnail(
                    videoId = it.id,
                    path = it.path,
                    strategy = uiState.thumbnailStrategy,
                    quality = uiState.thumbnailQuality,
                    fallbackSecond = uiState.thumbnailFallbackSecond
                )
            }
        )
    }
    var hasRenderedInitialFrame by remember(currentVideo) { mutableStateOf(false) }

    LaunchedEffect(currentVideo) {
        if (currentVideo != null) {
            com.example.util.MediaSeenManager.markSeen(context, currentVideo.path, currentVideo.id)
            if (thumbnailBitmap == null) {
                val bmp = loadVideoThumbnail(context, currentVideo.id, currentVideo.uri, currentVideo.path, strategy = uiState.thumbnailStrategy, quality = uiState.thumbnailQuality, fallbackSecond = uiState.thumbnailFallbackSecond)
                if (bmp != null) {
                    thumbnailBitmap = bmp
                }
            }
        }
    }

    // MPV Controller integration
    val controller = remember { miniAdopted?.controller ?: MpvPlayerController() }

    fun onlineChaptersOf(pb: OnlineVideoPlayback): List<PlayerVideoChapter> =
        pb.chapters.mapIndexed { i, c ->
            PlayerVideoChapter(
                index = i,
                title = c.title.ifBlank { "Chapter ${i + 1}" },
                timeMs = c.startMs,
                formattedTime = formatPlaybackTime(c.startMs)
            )
        }

    /** Picks the quality (saved choice or Auto) of an already resolved video and starts it; shared by Online Video and pasted links. */
    fun beginResolvedPlayback(pb: OnlineVideoPlayback, startMs: Long, headers: Map<String, String>, webLink: Boolean = false) {
        onlinePlayback = pb
        networkRequestHeaders = headers
        webLinkPlayback = webLink
        val savedQuality = onlinePrefs.getString("quality", null)
        val auto = savedQuality == null || savedQuality == OnlineVideoService.AUTO_QUALITY
        val q = (if (auto) OnlineVideoService.pickAutoQuality(context, pb.qualities) else null)
            ?: OnlineVideoService.pickDefaultQuality(pb.qualities, if (auto) null else savedQuality) ?: return
        onlineAutoMode = auto
        selectedOnlineQuality = q.label
        val audios = if (q.isVideoOnly) pb.audios.map { OnlineAudioSource(it.url, it.title, it.language) } else emptyList()
        controller.setOnlineStream(true, audios, 0, onlineChaptersOf(pb))
        if (webLink) controller.setWebLinkStream(true)
        if (headers.isNotEmpty()) controller.setNetworkRequestHeaders(headers)
        controller.loadVideo(q.url, startMs)
        controller.play()
    }

    /** Media plugin: every subtitle the website offers for this stream is added to the player's subtitle list (none is forced on). */
    fun attachPluginSubtitles(subtitles: List<com.example.util.media.MediaSubtitle>, loadKey: String?) {
        if (subtitles.isEmpty()) return
        coroutineScope.launch {
            // the subtitle files are fetched while the video is still opening (in parallel), and added as local files:
            // mpv then never waits on a subtitle website while the video plays, so choosing a subtitle does not buffer
            val headers = LinkedHashMap<String, String>()
            com.example.util.StreamEntryStore.get(pluginSession?.let { it.servers.getOrNull(it.selectedServer)?.link } ?: currentVideo?.path.orEmpty())?.let { meta ->
                meta.userAgent?.takeIf { it.isNotBlank() }?.let { headers["User-Agent"] = it }
                meta.referrer?.takeIf { it.isNotBlank() }?.let { headers["Referer"] = it }
                headers.putAll(meta.extraHeaders)
            }
            val downloads = subtitles.map { sub ->
                async(Dispatchers.IO) {
                    sub to try { com.example.util.media.SubtitleDownloader.download(context, sub.uri, headers) } catch (_: Throwable) { null }
                }
            }
            val opened = withContext(Dispatchers.IO) { controller.awaitFileLoaded(30000L) }
            if (!opened || lastLoadedVideoKey != loadKey) return@launch
            val fetched = downloads.awaitAll()
            val used = HashSet<String>()
            withContext(Dispatchers.IO) {
                for ((sub, local) in fetched) {
                    val base = sub.title.ifBlank { sub.language }.ifBlank { "Subtitle" }
                    var name = base
                    var n = 2
                    while (!used.add(name.lowercase())) name = "$base ${n++}"
                    try {
                        // the stream's own subtitles are listed under Built-in (not as removable uploads)
                        controller.markBuiltInSubtitle(name)
                        // a file that could not be fetched here is still given to mpv as the link
                        controller.addExternalSubtitle(local?.absolutePath ?: sub.uri, context, originalName = name, select = false)
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    fun hlsKey(v: com.example.util.media.HlsVariant) = "hls:${v.bandwidth}:${v.height}"

    /** The headers (User-Agent / Referer / ...) the provider gave for [link]. */
    fun providerHeaders(link: String): Map<String, String> {
        val meta = com.example.util.StreamEntryStore.get(link) ?: return emptyMap()
        val m = LinkedHashMap<String, String>()
        meta.userAgent?.takeIf { it.isNotBlank() }?.let { m["User-Agent"] = it }
        meta.referrer?.takeIf { it.isNotBlank() }?.let { m["Referer"] = it }
        m.putAll(meta.extraHeaders)
        return m
    }

    /**
     * Auto: like ExoPlayer's AdaptiveTrackSelection in CloudStream, the best rendition that fits into 70% of the REAL download
     * speed ([measuredBytesPerSec], see HlsVariants.measureThroughput). Without a measurement the nominal link speed is used,
     * capped at 4 Mbit/s, because that number only describes the Wi-Fi / mobile link and not the internet behind it.
     * The lowest rendition is used when nothing fits.
     */
    fun pickAutoHlsVariant(variants: List<com.example.util.media.HlsVariant>, measuredBytesPerSec: Long? = null): com.example.util.media.HlsVariant? {
        if (variants.isEmpty()) return null
        if (measuredBytesPerSec != null && measuredBytesPerSec > 0L) {
            val measuredUsableBps = measuredBytesPerSec * 8.0 * 0.7
            return variants.firstOrNull { it.height <= 1080 && it.bandwidth <= measuredUsableBps } ?: variants.last()
        }
        val kbps = try {
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps ?: 0
        } catch (_: Throwable) { 0 }
        if (kbps <= 0) return variants.firstOrNull { it.height in 1..480 } ?: variants.last()
        val usableBps = minOf(kbps * 1000.0 * 0.6, 4_000_000.0)
        return variants.firstOrNull { it.height <= 1080 && it.bandwidth * 1.3 <= usableBps } ?: variants.last()
    }

    /** The real download speed of [stream]'s server (bytes/s), measured on one segment; null when it could not be measured. */
    suspend fun measureHlsSpeed(variants: List<com.example.util.media.HlsVariant>, stream: com.example.util.media.MediaStream): Long? =
        withContext(Dispatchers.IO) {
            val headers = if (stream.headers.isNotEmpty()) stream.headers else providerHeaders(stream.link)
            com.example.util.media.HlsVariants.measureThroughput(variants, headers, null)
                ?: com.example.util.NetworkBypass.okHttpProxy(context)?.let { px ->
                    com.example.util.media.HlsVariants.measureThroughput(variants, headers, px)
                }
        }

    /** Loads [link] with the HLS rendition [v] (null = mpv's own choice) at [posMs]. */
    fun applyHlsVariant(v: com.example.util.media.HlsVariant?, posMs: Long) {
        hlsCurrentBandwidth = v?.bandwidth
        controller.setHlsBitrate(v?.bandwidth)
        controller.reloadCurrentStream(posMs)
        controller.play()
        pluginSession?.let { attachPluginSubtitles(it.subtitles, lastLoadedVideoKey) }
    }

    /**
     * An HLS master playlist can list several audio languages and subtitle tracks as separate renditions. When the player
     * shows fewer of them than the playlist lists, the missing ones are added from the playlist itself, so every audio /
     * subtitle track the provider offers can be chosen (nothing is switched on by itself).
     */
    fun addMissingHlsRenditions(stream: com.example.util.media.MediaStream, loadKey: String?) {
        if (!com.example.util.media.HlsVariants.looksLikeHls(stream.link, stream.type)) return
        if (!com.example.util.NetworkSettings.hlsHandlingEnabled(context)) return
        coroutineScope.launch {
            val headers = if (stream.headers.isNotEmpty()) stream.headers else providerHeaders(stream.link)
            val renditions = withContext(Dispatchers.IO) {
                val text = com.example.util.media.HlsVariants.fetchText(stream.link, headers, null, 4000)
                    ?: com.example.util.NetworkBypass.okHttpProxy(context)?.let { px ->
                        com.example.util.media.HlsVariants.fetchText(stream.link, headers, px, 5000)
                    }
                if (text == null) emptyList() else com.example.util.media.HlsVariants.parseRenditions(text, stream.link)
            }
            val audioR = renditions.filter { it.type == "AUDIO" && it.uri.isNotBlank() }.distinctBy { it.uri }
            val subR = renditions.filter { it.type == "SUBTITLES" && it.uri.isNotBlank() }.distinctBy { it.uri }
            if (audioR.isEmpty() && subR.isEmpty()) return@launch
            val opened = withContext(Dispatchers.IO) { controller.awaitFileLoaded(30000L) }
            if (!opened || lastLoadedVideoKey != loadKey) return@launch
            // mpv needs a few seconds to list every track of the stream: wait until its track list stops growing (2 - 8 s),
            // only what is still missing after that is added
            var waited = 0L
            var stableFor = 0L
            var lastCount = -1
            while (waited < 8000L) {
                delay(250L)
                waited += 250L
                val count = controller.audioTracks.value.size + controller.subtitleTracks.value.size
                if (count == lastCount) stableFor += 250L else { stableFor = 0L; lastCount = count }
                if (waited >= 2000L && stableFor >= 1500L) break
            }
            if (lastLoadedVideoKey != loadKey) return@launch
            // a rendition is already there when a track has its name, or when the language has at least as many tracks as
            // the playlist has renditions of it (two renditions of one language, e.g. 2.0 and 5.1, are different tracks)
            fun known(tracks: List<PlayerMediaTrack>, all: List<com.example.util.media.HlsRendition>, r: com.example.util.media.HlsRendition): Boolean {
                if (tracks.any { t -> t.title.isNotBlank() && t.title.equals(r.name, ignoreCase = true) }) return true
                if (r.language.isBlank()) return false
                val sameLangTracks = tracks.count { it.language.equals(r.language, ignoreCase = true) }
                val sameLangRenditions = all.count { it.language.equals(r.language, ignoreCase = true) }
                return sameLangTracks >= sameLangRenditions
            }
            val haveAudio = controller.audioTracks.value
            val haveSubs = controller.subtitleTracks.value
            val missingAudio = audioR.filter { !known(haveAudio, audioR, it) }
            val missingSubs = subR.filter { !known(haveSubs, subR, it) }
            if (missingAudio.isEmpty() && missingSubs.isEmpty()) return@launch
            withContext(Dispatchers.IO) {
                for (r in missingAudio) {
                    if (lastLoadedVideoKey != loadKey) return@withContext
                    val name = r.name.ifBlank { r.language }.ifBlank { "Audio" }
                    try { controller.addExternalAudio(r.uri, name, r.language.ifBlank { null }, false) } catch (_: Throwable) {}
                }
                for (r in missingSubs) {
                    if (lastLoadedVideoKey != loadKey) return@withContext
                    val name = r.name.ifBlank { r.language }.ifBlank { "Subtitle" }
                    try { controller.addExternalSubtitle(r.uri, context, originalName = name, select = false) } catch (_: Throwable) {}
                }
            }
        }
    }

    /**
     * Opens a provider stream. An HLS stream first has its quality list read, so Auto (or the quality chosen earlier)
     * starts at a rate the connection can hold instead of mpv's default of the highest one.
     */
    suspend fun openPluginStream(stream: com.example.util.media.MediaStream, startMs: Long, key: String?, followPosition: Boolean = false) {
        controller.resetStreamRoute(stream.link)
        var variants: List<com.example.util.media.HlsVariant> = emptyList()
        if (com.example.util.media.HlsVariants.looksLikeHls(stream.link, stream.type)) {
            variants = withContext(Dispatchers.IO) {
                val headers = if (stream.headers.isNotEmpty()) stream.headers else providerHeaders(stream.link)
                // only the quality list: a short wait, direct first, the proxy only when the request itself failed
                com.example.util.media.HlsVariants.fetchResult(stream.link, headers, null, 2500)
                    ?: com.example.util.NetworkBypass.okHttpProxy(context)?.let { px ->
                        com.example.util.media.HlsVariants.fetchResult(stream.link, headers, px, 2500)
                    }
                    ?: emptyList()
            }
        }
        if (key != null && lastLoadedVideoKey != key) return
        hlsVariants = variants
        com.example.util.media.PluginPlaybackSessions.get(stream.link)?.let { pluginServerIndex = it.selectedServer }
        // The quality list could not be read in the short first try (slow site / request failed): it is read again in the
        // background with longer waits and shown as soon as it arrives (the video keeps playing, nothing is reloaded).
        if (variants.isEmpty() && com.example.util.media.HlsVariants.looksLikeHls(stream.link, stream.type)) {
            coroutineScope.launch {
                val headers = if (stream.headers.isNotEmpty()) stream.headers else providerHeaders(stream.link)
                for (attempt in 1..3) {
                    delay(if (attempt == 1) 1200L else 3500L)
                    if (key != null && lastLoadedVideoKey != key) return@launch
                    if (hlsVariants.isNotEmpty()) return@launch
                    val found = withContext(Dispatchers.IO) {
                        com.example.util.media.HlsVariants.fetchResult(stream.link, headers, null, 7000)
                            ?: com.example.util.NetworkBypass.okHttpProxy(context)?.let { px ->
                                com.example.util.media.HlsVariants.fetchResult(stream.link, headers, px, 8000)
                            }
                    }
                    if (key != null && lastLoadedVideoKey != key) return@launch
                    val stillThisServer = com.example.util.media.PluginPlaybackSessions.get(stream.link)
                        ?.let { it.servers.getOrNull(it.selectedServer)?.link } == stream.link
                    if (!stillThisServer) return@launch
                    if (!found.isNullOrEmpty()) {
                        hlsVariants = found
                        return@launch
                    }
                    // answered, but it is not a master playlist: there is no quality list to find
                    if (found != null) return@launch
                }
            }
        }
        // quality chosen in the player earlier, else the one of Settings > Network > Media settings
        val pref = pluginVideoPrefs.getString("hls_pref", null)
            ?: com.example.util.NetworkSettings.mediaPlayQuality(context).let { if (it > 0) it.toString() else "auto" }
        val measuredSpeed: Long? = if (pref == "auto" && variants.size > 1) measureHlsSpeed(variants, stream) else null
        if (key != null && lastLoadedVideoKey != key) return
        val chosen: com.example.util.media.HlsVariant? = when {
            variants.isEmpty() -> null
            pref == "auto" -> pickAutoHlsVariant(variants, measuredSpeed)
            else -> {
                val h = pref.toIntOrNull() ?: 0
                variants.firstOrNull { it.height in 1..h } ?: variants.last()
            }
        }
        hlsSelectedKey = if (variants.isEmpty() || pref == "auto") "auto" else chosen?.let { hlsKey(it) } ?: "auto"
        hlsCurrentBandwidth = chosen?.bandwidth
        controller.setOnlineStream(true)
        controller.setDirectStream(true)
        // CloudStream providers get ExoPlayer's 2.5 s / 5 s, Vega providers react-native-video's 1.5 s / 3 s
        val vegaStream = (com.example.util.media.PluginPlaybackSessions.get(stream.link) ?: pluginSession)?.plugin?.isCloudStream == false
        if (vegaStream) controller.setProviderVod(true, 1.5, 3.0) else controller.setProviderVod(true)
        // quality list not readable (request failed / not a master playlist): mpv would pick the HIGHEST rendition, which a
        // server that only just connects cannot deliver fast enough -> start at a rate the connection can hold
        val blindCap: Long? = if (variants.isEmpty() && com.example.util.media.HlsVariants.looksLikeHls(stream.link, stream.type)) {
            val kbps = try {
                val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                cm.getNetworkCapabilities(cm.activeNetwork)?.linkDownstreamBandwidthKbps ?: 0
            } catch (_: Throwable) { 0 }
            if (kbps > 0) (kbps * 600L).coerceIn(800_000L, 4_000_000L) else 2_500_000L
        } else null
        controller.setHlsBitrate(chosen?.bandwidth ?: blindCap)
        // the old video kept playing while the quality list was read: continue from where it is NOW, not from where it was
        val resumeMs = if (followPosition) maxOf(startMs, controller.currentPositionMs.value) else startMs
        controller.loadVideo(stream.link, resumeMs)
        controller.play()
        addMissingHlsRenditions(stream, key ?: lastLoadedVideoKey)
    }

    /** Opens a direct stream: a provider (media plugin) stream with its quality list, or an imported IPTV channel as before. */
    suspend fun openDirectStream(path: String, startMs: Long, key: String) {
        val session = com.example.util.media.PluginPlaybackSessions.get(path)
        val stream = session?.servers?.firstOrNull { it.link == path }
        if (session == null || stream == null) {
            controller.setOnlineStream(true)
            controller.setDirectStream(true)
            controller.loadVideo(path, startPositionMs = startMs)
            controller.play()
            // a torrent that is streamed while it downloads: its own subtitle files (embedded tracks come with the container)
            com.example.torrent.TorrentStreamer.subtitlesFor(path).takeIf { it.isNotEmpty() }?.let { attachPluginSubtitles(it, key) }
        } else {
            openPluginStream(stream, startMs, key)
            attachPluginSubtitles(session.subtitles, key)
        }
    }

    /**
     * Online Video: the same chapters and subtitle tracks the download panel lists (yt-dlp) are added once they are known,
     * next to what the player already got from the video page. Nothing is switched on by itself.
     */
    suspend fun addOnlineExtras(watchUrl: String, key: String) {
        if (!com.example.BuildConfig.ENABLE_YTDLP || !com.example.ytdlp.YtdlpManager.isInstalled(context)) return
        val info = com.example.download.MediaProbe.probe(context, watchUrl.replace("music.youtube.com", "www.youtube.com"), forPlayback = true).getOrNull() ?: return
        if (lastLoadedVideoKey != key) return
        val pb = onlinePlayback ?: return
        if (pb.pageUrl != watchUrl) return
        if (info.chapters.size > pb.chapters.size) {
            val updated = pb.copy(chapters = info.chapters.map { com.example.util.OnlineChapterInfo(it.title, it.startMs) })
            onlinePlayback = updated
            controller.updateExternalChapters(onlineChaptersOf(updated))
        }
        val have = pb.subtitles.map { it.language.lowercase().removeSuffix("-orig") }.toSet()
        val extra = info.subtitles
            .filter { it.language.lowercase().removeSuffix("-orig") !in have }
            .map { com.example.util.media.MediaSubtitle(it.name, it.language, it.url) }
        if (extra.isNotEmpty()) attachPluginSubtitles(extra, key)
    }

    /** Resolves the stream URLs and starts playback. The existing buffering spinner runs until the first frame. */
    suspend fun startOnlinePlayback(watchUrl: String, startMs: Long, key: String) {
        controller.setOnlineStream(true)
        val pb = OnlineVideoService.resolvePlayback(watchUrl)
        if (lastLoadedVideoKey != key) return
        if (pb == null) {
            Toast.makeText(context, "Could not load this video. Check your internet connection.", Toast.LENGTH_LONG).show()
            return
        }
        beginResolvedPlayback(pb, startMs, emptyMap())
        attachPluginSubtitles(pb.subtitles, key)
        coroutineScope.launch { try { addOnlineExtras(watchUrl, key) } catch (_: kotlinx.coroutines.CancellationException) {} catch (_: Throwable) {} }
    }

    /**
     * A link pasted in the Network tab: resolved (the Network tab already resolved it, so this is normally an
     * instant cache hit) and then played by the SAME mpv player and controls as every other video.
     */
    suspend fun startNetworkPlayback(pageUrl: String, startMs: Long, key: String) {
        controller.setOnlineStream(true)
        val stream = com.example.util.NetworkStreamResolver.resolve(context, pageUrl)
        if (lastLoadedVideoKey != key) return
        if (stream == null || stream.isAudio) {
            Toast.makeText(
                context,
                if (stream == null) (com.example.util.NetworkStreamResolver.lastError ?: "This video can't play") else "Open this audio link from the audio player.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val web = stream.web
        if (web != null) {
            // website link: the streams were checked already, quality / audio tracks / chapters / subtitles come with them
            controller.setDirectStream(false)
            beginResolvedPlayback(web.playback, startMs, web.headers, webLink = true)
            attachPluginSubtitles(web.subtitles, key)
            return
        }
        controller.setOnlineStream(true)
        controller.setDirectStream(true)
        controller.setWebLinkStream(true) // direct file links are also opened through the app's own connection (WebStreamProxy)
        controller.setYtdlpStream(false)
        controller.setNetworkRequestHeaders(stream.headers)
        controller.loadVideo(stream.videoUrl, startMs)
        controller.play()
    }

    val onStartTranslate: (Int) -> Unit = { sourceTrackId ->
        if (translationStatusByTrackId[sourceTrackId] is TrackTranslationStatus.Translating) {
            // Already running translation for this track
        } else {
            val settings = AiFeaturesSettingsStore.load(context)
            if (!settings.hasUsableModel) {
                showNotification("Configure a usable API model or installed offline model first")
            } else {
                val targetLang = TranslateLanguages.ALL.firstOrNull { it.code == settings.translateLanguageCode }?.label ?: settings.translateLanguageCode
                coroutineScope.launch {
                    val startTime = System.currentTimeMillis()
                    translationStatusByTrackId = translationStatusByTrackId +
                        (sourceTrackId to TrackTranslationStatus.Translating(0, null))

                    val result = controller.translateSubtitleTrack(
                        context = context,
                        trackId = sourceTrackId,
                        settings = settings,
                        targetLanguageLabel = targetLang,
                        onProgress = { p ->
                            val percent = if (p.total == 0) 0 else ((p.completed * 100) / p.total).coerceIn(0, 100)
                            val elapsedMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                            val etaSeconds = if (percent in 1..99) {
                                val totalEstimateMs = elapsedMs / (percent / 100.0)
                                val remainingMs = (totalEstimateMs - elapsedMs).coerceAtLeast(0.0)
                                (remainingMs / 1000.0).toInt().coerceAtLeast(1)
                            } else null
                            translationStatusByTrackId = translationStatusByTrackId +
                                (sourceTrackId to TrackTranslationStatus.Translating(percent, etaSeconds))
                        }
                    )
                    result.fold(
                        onSuccess = { translatedTrackId ->
                            val status = TrackTranslationStatus.Completed(targetLang, translatedTrackId)
                            translationStatusByTrackId = translationStatusByTrackId + (translatedTrackId to status)
                            if (translatedTrackId != sourceTrackId) {
                                translationStatusByTrackId = translationStatusByTrackId - sourceTrackId
                            }
                            showNotification("Translated to $targetLang")
                        },
                        onFailure = { error ->
                            val msg = error.localizedMessage ?: "Translation failed"
                            translationStatusByTrackId = translationStatusByTrackId +
                                (sourceTrackId to TrackTranslationStatus.Failed(msg))
                            showNotification("Translation failed: $msg")
                        }
                    )
                }
            }
        }
    }
    var lastKnownOrientation by remember { mutableIntStateOf(configuration.orientation) }

    LaunchedEffect(configuration.orientation) {
        if (lastKnownOrientation != configuration.orientation) {
            lastKnownOrientation = configuration.orientation
        }
    }
    val resolvedPlayablePath = remember(currentVideo) {
        if (currentVideo != null) {
            getPlayableFilePath(context, currentVideo.uri, currentVideo.path)
        } else ""
    }

    LaunchedEffect(selectedAudioTrackId, trackAudioConfigs, resolvedPlayablePath) {
        val config = trackAudioConfigs[selectedAudioTrackId] ?: TrackAudioConfig()
        controller.setAudioProcessing(config.channelMode, config.volumeNormalization, config.dynamicRangeCompression)
    }

    // Audio Equalizer & Dynamic Filter Sync
    LaunchedEffect(resolvedPlayablePath, equalizerEnabled, eq60Hz, eq230Hz, eq910Hz, eq3600Hz, eq14000Hz, volumeBoostDb) {
        controller.setEqualizer(
            enabled = equalizerEnabled,
            eq60Hz = eq60Hz,
            eq230Hz = eq230Hz,
            eq910Hz = eq910Hz,
            eq3600Hz = eq3600Hz,
            eq14000Hz = eq14000Hz,
            volumeBoostDb = volumeBoostDb
        )
    }

    // Video Filter Presets & Manual Video Adjustments Live Sync
    LaunchedEffect(resolvedPlayablePath, videoFilterPreset, manualVideoAdjustments) {
        controller.setVideoFilterAndAdjustments(videoFilterPreset, manualVideoAdjustments)
    }

    // Flip Vertical / Mirror -> actual mpv video frames. Keyed on resolvedPlayablePath so the state
    // (which is reset per video via remember(currentVideo)) is pushed again whenever the video changes.
    LaunchedEffect(resolvedPlayablePath, isVideoFlippedVertically, isVideoMirrored) {
        controller.setVideoTransform(
            flipVertical = isVideoFlippedVertically,
            mirrorHorizontal = isVideoMirrored
        )
    }

    // ---- Per-video subtitle SELECTION memory (embedded / uploaded / both) ----
    fun toSavedSubtitleRef(track: PlayerMediaTrack?, allTracks: List<PlayerMediaTrack>): SavedSubtitleRef? {
        if (track == null) return null
        return if (track.isExternal) {
            SavedSubtitleRef(
                kind = CustomSubtitlePersistenceManager.KIND_UPLOADED,
                name = track.originalFilename.ifBlank { track.title },
                language = track.language,
                index = -1
            )
        } else {
            SavedSubtitleRef(
                kind = CustomSubtitlePersistenceManager.KIND_EMBEDDED,
                name = track.title,
                language = track.language,
                index = allTracks.filter { !it.isExternal }.indexOfFirst { it.id == track.id }
            )
        }
    }

    /** Remembers the manual audio choice for this video and its track number for the next video. */
    fun saveAudioSelection(trackId: Int) {
        val vid = currentVideo ?: return
        val tracks = controller.audioTracks.value
        val track = tracks.firstOrNull { it.id == trackId } ?: return
        VideoAudioSelectionMemory.save(
            context = context,
            path = vid.path,
            videoId = vid.id,
            uriString = vid.uri.toString(),
            title = vid.displayName,
            track = track,
            allTracks = tracks
        )
        // Same language for the other episodes of THIS folder only.
        VideoAudioSelectionMemory.saveForFolder(context, vid.path, track)
    }

    /** Remembers the main + 2nd subtitle of the CURRENT video only (0 = nothing selected). */
    fun saveSubtitleSelection(
        primaryId: Int,
        secondaryId: Int = controller.secondarySubtitleTrackId.value
    ) {
        val vid = currentVideo ?: return
        val tracks = controller.subtitleTracks.value
        val selection = VideoSubtitleSelection(
            primary = toSavedSubtitleRef(tracks.firstOrNull { primaryId > 0 && it.id == primaryId }, tracks),
            secondary = toSavedSubtitleRef(tracks.firstOrNull { secondaryId > 0 && it.id == secondaryId }, tracks)
        )
        CustomSubtitlePersistenceManager.saveSubtitleSelection(
            context = context,
            path = vid.path,
            videoId = vid.id,
            uriString = vid.uri.toString(),
            title = vid.displayName,
            selection = selection
        )
    }

    fun resolveSavedSubtitle(ref: SavedSubtitleRef?, tracks: List<PlayerMediaTrack>): PlayerMediaTrack? {
        if (ref == null) return null
        return if (ref.kind == CustomSubtitlePersistenceManager.KIND_UPLOADED) {
            val wanted = ref.name.trim()
            val wantedBase = wanted.substringBeforeLast('.', wanted).trim()
            tracks.firstOrNull { t ->
                t.isExternal && (
                    t.originalFilename.equals(wanted, ignoreCase = true) ||
                    t.title.equals(wanted, ignoreCase = true) ||
                    File(t.originalFilename).name.equals(wanted, ignoreCase = true) ||
                    File(t.title).name.equals(wanted, ignoreCase = true)
                )
            } ?: tracks.firstOrNull { t ->
                val n = t.originalFilename.ifBlank { t.title }.trim()
                val nBase = n.substringBeforeLast('.', n).trim()
                t.isExternal && (
                    n.equals(wantedBase, ignoreCase = true) ||
                    nBase.equals(wantedBase, ignoreCase = true) ||
                    n.contains(wantedBase, ignoreCase = true) ||
                    wanted.contains(nBase, ignoreCase = true)
                )
            } ?: tracks.firstOrNull { t ->
                // If only one external subtitle track is present and this ref is for an uploaded subtitle, resolve it!
                t.isExternal
            }
        } else {
            val embedded = tracks.filter { !it.isExternal }
            embedded.firstOrNull { it.title == ref.name && it.language.equals(ref.language, ignoreCase = true) }
                ?: embedded.firstOrNull { it.title == ref.name }
                ?: embedded.getOrNull(ref.index)
        }
    }

    fun selectSubtitleTracks(
        primaryId: Int,
        secondaryId: Int,
        notify: Boolean = true,
        persist: Boolean = true,
        rememberTrackChoice: Boolean = notify
    ) {
        // Enforce the atomic state machine:
        // If primary is unselected (<= 0) but secondary exists (> 0),
        // secondary is automatically and immediately promoted to Primary!
        val (finalPrimaryId, finalSecondaryId) = if (primaryId <= 0 && secondaryId > 0) {
            secondaryId to 0
        } else {
            primaryId to if (secondaryId > 0 && secondaryId != primaryId) secondaryId else 0
        }

        selectedSubtitleTrackId = finalPrimaryId
        if (finalPrimaryId > 0) {
            lastTouchedSubtitleTrackId = finalPrimaryId
        } else if (finalSecondaryId > 0) {
            lastTouchedSubtitleTrackId = finalSecondaryId
        }

        val hasAny = finalPrimaryId > 0 || finalSecondaryId > 0
        isSubtitleVisible = hasAny
        if (rememberTrackChoice) SubtitleSessionMemory.setSubtitlesEnabled(hasAny)

        controller.selectSubtitleTracksAtomic(finalPrimaryId, finalSecondaryId)

        if (finalPrimaryId > 0) {
            val selectedTrack = controller.subtitleTracks.value.firstOrNull { it.id == finalPrimaryId }
            if (selectedTrack != null) {
                // An uploaded (external) track is stored together with the others in the background step below.
                if (!selectedTrack.isExternal) {
                    val embedded = controller.subtitleTracks.value.filter { !it.isExternal }
                    val idx = embedded.indexOfFirst { it.id == finalPrimaryId }
                    val trackNum = if (idx >= 0) idx + 1 else finalPrimaryId
                    if (rememberTrackChoice) SubtitleSessionMemory.setPreferredTrack(trackNum, finalPrimaryId)
                    controller.preferredSubtitleTrackNumber = trackNum
                }
            }
            if (notify) {
                val subName = selectedTrack?.title ?: "Track $finalPrimaryId"
                showNotification("Primary Subtitle: $subName")
            }
        } else {
            controller.preferredSubtitleTrackNumber = 0
            if (rememberTrackChoice) SubtitleSessionMemory.setPreferredTrack(0, 0)
            if (notify && finalSecondaryId <= 0) {
                showNotification("Subtitles Off")
            }
        }

        // Restoring a remembered choice must never rewrite that memory (persist = false).
        if (!persist) return

        // Stored in the background (never on the UI thread), one choice after the other.
        persistSubtitleChoiceInBackground(coroutineScope, context, currentVideo, controller.subtitleTracks.value, finalPrimaryId, finalSecondaryId) {
            saveSubtitleSelection(primaryId = finalPrimaryId, secondaryId = finalSecondaryId)
        }
    }

    /** Re-applies the saved main / 2nd subtitle of this video (each one independently). */
    fun applySavedSubtitleSelection(saved: VideoSubtitleSelection) {
        val tracks = controller.subtitleTracks.value
        val primary = resolveSavedSubtitle(saved.primary, tracks)
        val secondary = resolveSavedSubtitle(saved.secondary, tracks)?.takeIf { it.id != primary?.id }
        val primId = primary?.id ?: 0
        val secId = secondary?.id ?: 0
        // A remembered track that can't be found right now (not loaded yet) must NOT be turned
        // into "Subtitles Off" and saved over the user's real choice: keep the memory intact.
        val unresolved = (saved.primary != null && primary == null) || (saved.secondary != null && secondary == null)
        if (unresolved && primId <= 0 && secId <= 0) return
        selectSubtitleTracks(primId, secId, notify = false, persist = !unresolved)
    }

    /**
     * Restores the subtitles of a freshly loaded video. Runs as soon as mpv has really opened the
     * file (not after guessed delays): every subtitle the user uploaded for this video is added
     * back to the upload section (never dropped), then the remembered selection of THIS video is
     * re-applied. [saved] == null means the user never chose anything for this video, so the
     * normal default selection applies.
     */
    fun restoreSubtitlesForVideo(targetVideo: VideoItem, loadKey: String, saved: VideoSubtitleSelection?) {
        coroutineScope.launch {
            try {
                val uploaded = withContext(Dispatchers.IO) {
                    try {
                        CustomSubtitlePersistenceManager.getAttachedSubtitles(
                            context = context,
                            path = targetVideo.path,
                            videoId = targetVideo.id,
                            uriString = targetVideo.uri.toString(),
                            title = targetVideo.displayName
                        ).also { list ->
                            // Pick up edits the user made to the ORIGINAL file since last time
                            // (runs while the video is still opening, so it costs no extra wait).
                            for (sub in list) CustomSubtitlePersistenceManager.syncAttachedFromSource(context, sub)
                        }
                    } catch (_: Throwable) {
                        emptyList()
                    }
                }
                withContext(Dispatchers.IO) { controller.awaitFileLoaded(15000L) }
                if (lastLoadedVideoKey != loadKey) return@launch

                if (uiState.playerSettings.detectSubtitlesByFilename) {
                    val videoFile = File(targetVideo.path)
                    val parent = videoFile.parentFile
                    val base = videoFile.nameWithoutExtension.lowercase()
                    val candidates = parent?.listFiles()?.filter { file ->
                        file.isFile && file.extension.lowercase() in setOf("srt", "ass", "ssa", "vtt", "ttml", "smi", "sub", "sup") &&
                            file.nameWithoutExtension.lowercase().startsWith(base)
                    }.orEmpty()
                    withContext(Dispatchers.IO) {
                        for (file in candidates) {
                            if (uploaded.none { it.filePath.equals(file.absolutePath, ignoreCase = true) }) {
                                controller.addExternalSubtitle(file.absolutePath, context, originalName = file.name, select = false)
                            }
                        }
                    }
                }
                if (uploaded.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        for (sub in uploaded) {
                            if (File(sub.filePath).exists()) {
                                controller.addExternalSubtitle(
                                    filePath = sub.filePath,
                                    context = context,
                                    originalName = sub.originalName,
                                    // With a saved selection nothing is selected while adding;
                                    // the saved choice is applied below.
                                    select = saved == null && sub.isSelected
                                )
                            }
                        }
                    }
                }
                // Make sure the track list is up to date before resolving the saved choice.
                withContext(Dispatchers.IO) { controller.refreshTracksAndChapters() }
                if (lastLoadedVideoKey != loadKey) return@launch
                if (saved != null) {
                    applySavedSubtitleSelection(saved)
                } else {
                    // A manual track-number choice carries over before any container default/forced flag.
                    val sessionTrack = SubtitleSessionMemory.findMatchingTrackForNextEpisode(controller.subtitleTracks.value)
                    if (!SubtitleSessionMemory.isSubtitlesEnabled || SubtitleSessionMemory.preferredTrackNumber == 0) {
                        selectSubtitleTracks(0, 0, notify = false, persist = false)
                        return@launch
                    }
                    if (sessionTrack != null) {
                        selectSubtitleTracks(sessionTrack.id, 0, notify = false, persist = false)
                        return@launch
                    }
                    // If no explicit selection was saved, check if an uploaded subtitle is active or was selected
                    val activeUploadedTrack = controller.subtitleTracks.value.firstOrNull {
                        it.isExternal && uploaded.any { u -> u.isSelected && (u.originalName.equals(it.originalFilename, ignoreCase = true) || u.originalName.equals(it.title, ignoreCase = true)) }
                    }
                    if (activeUploadedTrack != null && activeUploadedTrack.id > 0) {
                        selectSubtitleTracks(activeUploadedTrack.id, 0, notify = false, persist = false)
                    } else {
                        // Check if preferred subtitle language matches
                        // provider video: the language of Settings > Network > Media settings comes first
                        val mediaSubLang = if (pluginSession != null && com.example.util.NetworkSettings.mediaAutoSubtitle(context)) {
                            com.example.util.NetworkSettings.mediaLanguage(context)
                        } else ""
                        val preferred = (
                            (if (mediaSubLang.isNotBlank()) AudioLanguageMatcher.aliasesOf(mediaSubLang).toList() else emptyList()) +
                                uiState.playerSettings.preferredSubtitleLanguages
                                    .split(',', ';')
                                    .map { it.trim().lowercase() }
                                    .filter { it.isNotBlank() }
                            )
                        val currentPath = targetVideo.path
                        val selectedOnly = uiState.playerSettings.applySubtitleLanguageToSelectedContentOnly
                        val appliesToCurrent = !selectedOnly ||
                            uiState.playerSettings.selectedSubtitleVideos.any { it.equals(currentPath, ignoreCase = true) } ||
                            uiState.playerSettings.selectedSubtitleFolders.any { folder ->
                                currentPath.equals(folder, ignoreCase = true) || currentPath.startsWith(folder.trimEnd(File.separatorChar) + File.separator)
                            }
                        val matchingLangTrack = if (preferred.isNotEmpty() && (appliesToCurrent || mediaSubLang.isNotBlank())) {
                            controller.subtitleTracks.value
                                .filter { track ->
                                    val lang = track.language.lowercase()
                                    preferred.any { pref -> lang == pref || lang.startsWith("$pref-") || lang.startsWith(pref) }
                                }
                                .maxByOrNull { track ->
                                    var score = 0
                                    if (track.isDefault) score += 20
                                    if (track.isForced) score += if (uiState.playerSettings.subtitleSignsAndSongs) 35 else 10
                                    if (uiState.playerSettings.subtitleSignsAndSongs && Regex("(?i)signs|songs|forced|narrative").containsMatchIn(track.title)) score += 40
                                    if (preferred.any { pref -> track.language.lowercase() == pref }) score += 30
                                    score
                                }
                                ?: if (uiState.playerSettings.subtitleSignsAndSongs) {
                                    controller.subtitleTracks.value.maxByOrNull {
                                        (if (it.isForced) 40 else 0) +
                                            if (Regex("(?i)signs|songs|forced|narrative").containsMatchIn(it.title)) 35 else 0 +
                                            if (it.isDefault) 10 else 0
                                    }?.takeIf { it.isForced || Regex("(?i)signs|songs|forced|narrative").containsMatchIn(it.title) }
                                } else null
                        } else {
                            // No app-level language preference: respect the MKV/container flags.
                            resolveContainerSubtitleFallback(controller.subtitleTracks.value)
                        }

                        if (matchingLangTrack != null && matchingLangTrack.id > 0) {
                            selectSubtitleTracks(matchingLangTrack.id, 0, notify = false, persist = false)
                        } else {
                            // User ne koi subtitle select nahi kiya:
                            // koi subtitle automatically select mat karo!
                            selectSubtitleTracks(0, 0, notify = false, persist = false)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Throwable) {
            } finally {
                // Only release our own hold (a newer video load manages its own).
                if (lastLoadedVideoKey == loadKey) controller.releaseSubtitleAutoSelectHold()
            }
        }
    }

    // A newly uploaded / pasted subtitle becomes Primary and the subtitle that was Primary until
    // now (embedded or uploaded) becomes Secondary. Any older Secondary is released, so the
    // result is always exactly two tidy slots and a later tap on Secondary simply unselects it.
    fun promoteNewSubtitleToPrimary(newTrackId: Int, previousPrimaryId: Int) {
        if (newTrackId <= 0) return
        val oldPrimary = if (previousPrimaryId > 0 && previousPrimaryId != newTrackId &&
            controller.subtitleTracks.value.any { it.id == previousPrimaryId }
        ) previousPrimaryId else 0
        selectSubtitleTracks(newTrackId, oldPrimary, notify = false)
    }

    // Persist an uploaded subtitle once per source and return the canonical private path.
    // This keeps repeated select/unselect/select operations idempotent.
    suspend fun attachUploadedSubtitle(
        filePath: String,
        originalName: String,
        sourcePath: String = "",
        sourceUri: String = ""
    ): String? {
        return CustomSubtitlePersistenceManager.addAttachedSubtitle(
            context = context,
            path = currentVideo?.path,
            videoId = currentVideo?.id ?: 0L,
            uriString = currentVideo?.uri?.toString(),
            title = currentVideo?.displayName,
            filePath = filePath,
            originalName = originalName,
            isSelected = true,
            sourcePath = sourcePath,
            sourceUri = sourceUri
        )
    }

    // Imports a subtitle file chosen in the in-app explorer. All file copying, font syncing and
    // subtitle preparation run OFF the main thread (they used to run on it, which froze the UI and
    // crashed the app for big / font-heavy subtitles). Any Throwable is reported instead of crashing.
    // Returns true when the subtitle was attached.
    suspend fun importSubtitleFileSafely(selectedFile: File): Boolean {
        val previousPrimaryId = controller.subtitleTracks.value.firstOrNull { it.isSelected }?.id ?: selectedSubtitleTrackId
        val targetId = withContext(Dispatchers.IO) {
            try {
                if (!selectedFile.exists() || !selectedFile.isFile || selectedFile.length() <= 0L) {
                    return@withContext null
                }
                val persistentFile = CustomSubtitlePersistenceManager.persistSubtitleFromFile(context, selectedFile)
                val attachedPath = attachUploadedSubtitle(
                    filePath = persistentFile.absolutePath,
                    originalName = selectedFile.name,
                    sourcePath = selectedFile.absolutePath
                ) ?: persistentFile.absolutePath
                val addedSubId = controller.addExternalSubtitle(
                    attachedPath, context, originalName = selectedFile.name, select = true
                )
                addedSubId?.takeIf { it > 0 } ?: controller.subtitleTracks.value.lastOrNull()?.id ?: 0
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                android.util.Log.w("VideoPlayerScreen", "Subtitle import failed", t)
                null
            }
        } ?: return false
        if (targetId > 0) {
            promoteNewSubtitleToPrimary(targetId, previousPrimaryId)
        }
        return true
    }

    // Subtitle File Picker Launcher
    val subtitleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val subFileName = try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIdx != -1 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
                        }
                    } catch (_: Throwable) { null } ?: "imported_sub_${System.currentTimeMillis() % 10000}.ass"

                    val previousPrimaryId = controller.subtitleTracks.value.firstOrNull { it.isSelected }?.id ?: selectedSubtitleTrackId
                    val persistentSubFile = withContext(Dispatchers.IO) {
                        CustomSubtitlePersistenceManager.persistSubtitleFromUri(context, uri, subFileName)
                    }

                    if (persistentSubFile != null && persistentSubFile.exists() && persistentSubFile.length() > 0) {
                        val attachedPath = attachUploadedSubtitle(
                            filePath = persistentSubFile.absolutePath,
                            originalName = subFileName,
                            sourceUri = uri.toString()
                        ) ?: persistentSubFile.absolutePath
                        val addedSubId = withContext(Dispatchers.IO) {
                            controller.addExternalSubtitle(attachedPath, context, originalName = subFileName, select = true)
                        }
                        val targetId = addedSubId?.takeIf { it > 0 } ?: controller.subtitleTracks.value.lastOrNull()?.id ?: 0
                        if (targetId > 0) {
                            promoteNewSubtitleToPrimary(targetId, previousPrimaryId)
                        }
                        showNotification("Loaded Subtitle: $subFileName")
                    } else {
                        showNotification("Failed to load subtitle file")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showNotification("Error loading subtitle: ${e.message}")
                }
            }
        }
    }

    // Audio File Picker Launcher
    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val audioFileName = try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIdx != -1 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
                        }
                    } catch (_: Throwable) { null } ?: "imported_audio_${System.currentTimeMillis() % 10000}.mp3"

                    val tempAssetsDir = File(context.cacheDir, "temp_player_assets").apply { mkdirs() }
                    val tempAudioFile = File(tempAssetsDir, audioFileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempAudioFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempAudioFile.exists() && tempAudioFile.length() > 0) {
                        hasManualAudioTrackSelection = true
                        val addedAudioId = controller.addExternalAudio(tempAudioFile.absolutePath, originalName = audioFileName)
                        if (addedAudioId != null && addedAudioId > 0) {
                            selectedAudioTrackId = addedAudioId
                            controller.setAudioTrack(addedAudioId)
                        } else {
                            val lastTrack = controller.audioTracks.value.lastOrNull()?.id
                            if (lastTrack != null && lastTrack > 0) {
                                selectedAudioTrackId = lastTrack
                                controller.setAudioTrack(lastTrack)
                            }
                        }
                        showNotification("Loaded Audio Track: $audioFileName")
                    } else {
                        showNotification("Failed to load audio file")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    showNotification("Error loading audio: ${e.message}")
                }
            }
        }
    }

    val realAudioTracks by controller.audioTracks.collectAsState()
    val realSubtitleTracks by controller.subtitleTracks.collectAsState()
    val realSecondarySubtitleTrackId by controller.secondarySubtitleTrackId.collectAsState()
    val realChapters by controller.chapters.collectAsState()
    val realVideoWidth by controller.videoWidth.collectAsState()
    val realVideoHeight by controller.videoHeight.collectAsState()
    val overrideAssSsaAutoResetEvent by controller.overrideAssSsaAutoResetEvent.collectAsState()

    val currentChapterTitle by remember(realChapters, currentPositionMs, currentEpisodeIndex) {
        derivedStateOf {
            if (realChapters.isNotEmpty()) {
                val chap = realChapters.filter { it.timeMs <= currentPositionMs }.maxByOrNull { it.timeMs }
                    ?: realChapters.firstOrNull()
                chap?.title ?: "Chapter $currentEpisodeIndex"
            } else {
                "Chapter $currentEpisodeIndex"
            }
        }
    }

    val controllerIsPlaying by controller.isPlaying.collectAsState()
    // The raw position changes every 100 ms; reading it directly recomposed the whole player screen + overlay 10x per second
    // (the main cause of lag while a video plays). The UI now gets it at ~5 updates per second; a backward jump (seek) is immediate.
    val controllerPos by androidx.compose.runtime.produceState(controller.currentPositionMs.value, controller) {
        controller.currentPositionMs.collect { v ->
            if (v < value || v - value >= 200L) value = v
        }
    }
    val controllerDur by controller.durationMs.collectAsState()
    val controllerEof by controller.eofReached.collectAsState()
    val controllerIsLive by controller.isLiveStream.collectAsState()
    val controllerLiveRange by controller.liveSeekRange.collectAsState()

    LaunchedEffect(controllerIsPlaying) {
        isPlaying = controllerIsPlaying
    }

    LaunchedEffect(controllerPos) {
        currentPositionMs = controllerPos
    }

    LaunchedEffect(controllerDur) {
        if (controllerDur > 0L) {
            durationMs = controllerDur
        }
    }

    // Real watched-threshold behavior: once playback crosses the configured
    // percentage, persist the video at its full duration so the library sees it
    // as completed/watched. The flag resets automatically for the next video.
    LaunchedEffect(currentPositionMs, durationMs, currentVideo, uiState.watchedThresholdPercent) {
        val vid = currentVideo ?: return@LaunchedEffect
        val dur = durationMs
        if (watchedThresholdReached || dur <= 0L) return@LaunchedEffect
        val threshold = (dur * uiState.watchedThresholdPercent.coerceIn(50, 100)) / 100L
        if (currentPositionMs >= threshold) {
            watchedThresholdReached = true
            PlaybackHistoryManager.savePosition(
                context = context,
                videoId = vid.id,
                path = vid.path,
                positionMs = dur,
                durationMs = dur,
                uriString = vid.uri.toString(),
                title = vid.displayName,
                forceDiskWrite = true
            )
        }
    }

    // Apply Audio Pitch Correction setting
    LaunchedEffect(uiState.enableAudioPitchCorrection) {
        isPitchCorrectionEnabled = uiState.enableAudioPitchCorrection
        controller.setAudioPitchCorrection(uiState.enableAudioPitchCorrection)
    }

    // Apply Audio Volume Normalization setting
    LaunchedEffect(uiState.volumeNormalization) {
        controller.setVolumeNormalization(uiState.volumeNormalization)
    }

    // Apply Audio Channel Mode setting
    LaunchedEffect(uiState.audioChannelMode) {
        controller.setAudioChannelMode(uiState.audioChannelMode)
    }

    var subtitleDiskSaveJob by remember { mutableStateOf<Job?>(null) }
    val saveSubtitleSettingsDebounced: (PlayerSettings) -> Unit = remember(coroutineScope, context) {
        { settings ->
            subtitleDiskSaveJob?.cancel()
            subtitleDiskSaveJob = coroutineScope.launch(Dispatchers.IO) {
                delay(300L)
                PlayerSettings.save(context, settings)
            }
        }
    }

    var eqDiskSaveJob by remember { mutableStateOf<Job?>(null) }
    val saveEqualizerSettingsDebounced: ((PlayerSettings) -> PlayerSettings) -> Unit = remember(coroutineScope, context) {
        { transform ->
            eqDiskSaveJob?.cancel()
            eqDiskSaveJob = coroutineScope.launch(Dispatchers.IO) {
                delay(300L)
                val current = PlayerSettings.load(context)
                PlayerSettings.save(context, transform(current))
            }
        }
    }

    // Last global appearance pushed to mpv (everything EXCEPT the Advanced ASS per-style edits).
    // Editing one ASS style only changes assStyleOverridesJson; re-pushing ~25 unchanged global
    // mpv sub-* properties on every keystroke / colour-drag tick made libass re-layout the whole
    // subtitle and caused animation/timing hitches on styles that were never touched.
    val lastAppliedAppearanceKey = remember(controller) { arrayOfNulls<PlayerSettings>(1) }
    val applySubtitleAppearanceDirect: (PlayerSettings) -> Unit = remember(controller, context) {
        appearance@{ settings ->
            // Advanced ASS/SSA state first, so the global override is suppressed correctly for
            // ASS/SSA tracks that are governed by per-style edits.
            controller.setAdvancedAssState(
                enabled = settings.advancedAssEnabled,
                overridesJson = settings.assStyleOverridesJson
            )
            controller.setRawSubtitleEditorEnabled(settings.rawSubtitleEditorEnabled)
            val appearanceKey = settings.copy(assStyleOverridesJson = "", rawSubtitleEditorEnabled = false)
            if (lastAppliedAppearanceKey[0] == appearanceKey) return@appearance
            lastAppliedAppearanceKey[0] = appearanceKey
            val selectedFamily = settings.selectedSubtitleFont
                .takeIf { it.isNotBlank() }
                ?.let { name ->
                    val installed = SubtitleFontManager.getInstalledFonts(
                        context,
                        includeVideoExtracted = settings.showVideoEmbeddedSubtitleFonts
                    )
                    val matchedFile = installed.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        ?: installed.firstOrNull { it.nameWithoutExtension.equals(name, ignoreCase = true) }
                        ?: installed.firstOrNull { SubtitleFontManager.getOriginalFontName(it).equals(name, ignoreCase = true) }
                    matchedFile?.let { file -> SubtitleFontManager.getRenderFontName(file) } ?: name
                }
            controller.applySubtitleAppearance(
                bold = settings.subtitleBold,
                italic = settings.subtitleItalic,
                underline = settings.subtitleUnderline,
                alignment = settings.subtitleAlignment,
                fontSize = settings.subtitleFontSize,
                borderStyle = settings.subtitleBorderStyle,
                borderSize = settings.subtitleBorderSize,
                shadowOffset = settings.subtitleShadowOffset,
                shadowBlur = settings.subtitleShadowBlur,
                backgroundPadding = settings.subtitleBackgroundPadding,
                textColorArgb = (settings.subtitleTextColor and 0xFFFFFFFFL).toInt(),
                borderColorArgb = (settings.subtitleBorderColor and 0xFFFFFFFFL).toInt(),
                backgroundColorArgb = (settings.subtitleBackgroundColor and 0xFFFFFFFFL).toInt(),
                shadowColorArgb = (settings.subtitleShadowColor and 0xFFFFFFFFL).toInt(),
                overrideAssSsa = settings.overrideAssSsaSubtitles,
                scaleWithWindow = settings.scaleSubtitlesByWindow,
                scale = settings.subtitleScale,
                position = settings.subtitlePosition,
                blendWithVideo = settings.subtitleBlendWithVideo,
                fontDirectory = SubtitleFontManager.getSubFontsDir(context).absolutePath,
                fontFamily = selectedFamily,
                letterSpacing = settings.subtitleLetterSpacing,
                shadowOffsetX = settings.subtitleShadowOffsetX,
                shadowOffsetY = settings.subtitleShadowOffsetY
            )
        }
    }

    // Apply subtitle settings live and before each subsequent media load.
    // Apply the complete live subtitle style without recreating the player.
    LaunchedEffect(liveSubtitleSettings) {
        applySubtitleAppearanceDirect(liveSubtitleSettings)
    }

    // The controller silently drops "Override ASS/SSA Styles" back to OFF whenever it auto-picks
    // a subtitle the user never explicitly chose for THIS video (a fresh video/next episode with
    // no saved choice of its own - see the comment in MpvPlayerController's "next episode" block).
    // Follow that reset here so the style panel switch and the persisted setting match what is
    // actually being rendered, instead of still showing ON while the video plays with original
    // (non-overridden) styling. 0L is just this flow's initial value, not a real reset - skip it.
    LaunchedEffect(overrideAssSsaAutoResetEvent) {
        if (overrideAssSsaAutoResetEvent == 0L) return@LaunchedEffect
        if (liveSubtitleSettings.overrideAssSsaSubtitles) {
            val updated = liveSubtitleSettings.copy(overrideAssSsaSubtitles = false)
            liveSubtitleSettings = updated
            saveSubtitleSettingsDebounced(updated)
            onPlayerSettingsChange(updated)
        }
    }

    LaunchedEffect(
        uiState.playerSettings.autoLoadExternalSubtitles,
        uiState.playerSettings.overrideAssSsaSubtitles,
        uiState.playerSettings.scaleSubtitlesByWindow,
        uiState.playerSettings.subtitleFontDirectoryUri,
        uiState.playerSettings.selectedSubtitleFont,
        uiState.playerSettings.subtitleFontsReloadNonce,
        uiState.playerSettings.showVideoEmbeddedSubtitleFonts
    ) {
        val selectedFamily = uiState.playerSettings.selectedSubtitleFont
            .takeIf { it.isNotBlank() }
            ?.let { name ->
                val installed = SubtitleFontManager.getInstalledFonts(
                    context,
                    includeVideoExtracted = uiState.playerSettings.showVideoEmbeddedSubtitleFonts
                )
                val matchedFile = installed.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    ?: installed.firstOrNull { it.nameWithoutExtension.equals(name, ignoreCase = true) }
                    ?: installed.firstOrNull { SubtitleFontManager.getOriginalFontName(it).equals(name, ignoreCase = true) }
                matchedFile?.let { file -> SubtitleFontManager.getRenderFontName(file) } ?: name
            }
        controller.applySubtitleSettings(
            autoLoadExternal = uiState.playerSettings.autoLoadExternalSubtitles,
            overrideAssSsa = uiState.playerSettings.overrideAssSsaSubtitles,
            scaleWithWindow = uiState.playerSettings.scaleSubtitlesByWindow,
            fontDirectory = SubtitleFontManager.getSubFontsDir(context).absolutePath,
            fontFamily = selectedFamily
        )
    }

    LaunchedEffect(uiState.playerSettings.preciseSeeking) {
        controller.setPreciseSeekingEnabled(uiState.playerSettings.preciseSeeking)
    }

    // Sync PipModeState for picture-in-picture actions and Auto-PiP background triggers
    DisposableEffect(Unit) {
        PipModeState.isPlayerActive = true
        PipModeState.onPlayPauseToggle = {
            controller.togglePlayPause()
        }
        onDispose {
            PipModeState.isPlayerActive = false
            PipModeState.isCurrentlyPlaying = false
            PipModeState.autoPipEnabled = false
            PipModeState.onPlayPauseToggle = null
            PipModeState.requestPipUpdate?.invoke(false, false)
        }
    }

    LaunchedEffect(isPlaying, uiState.playerSettings.autoPictureInPicture) {
        PipModeState.isCurrentlyPlaying = isPlaying
        PipModeState.autoPipEnabled = uiState.playerSettings.autoPictureInPicture
        PipModeState.requestPipUpdate?.invoke(isPlaying, uiState.playerSettings.autoPictureInPicture)
    }

    LaunchedEffect(realSubtitleTracks) {
        if (realSubtitleTracks.isEmpty()) return@LaunchedEffect
        val active = realSubtitleTracks.firstOrNull { it.isSelected }
        if (active != null && active.id > 0) {
            selectedSubtitleTrackId = active.id
            lastTouchedSubtitleTrackId = active.id
            isSubtitleVisible = SubtitleSessionMemory.isSubtitlesEnabled
        } else if (selectedSubtitleTrackId > 0 && realSubtitleTracks.none { it.id == selectedSubtitleTrackId }) {
            selectedSubtitleTrackId = 0
        }
    }

    // Automatically select Preferred Audio Language if available
    LaunchedEffect(realSubtitleTracks, uiState.playerSettings.preferredSubtitleLanguages, uiState.playerSettings.applySubtitleLanguageToSelectedContentOnly, uiState.playerSettings.selectedSubtitleFolders, uiState.playerSettings.selectedSubtitleVideos, uiState.playerSettings.subtitleSignsAndSongs, currentVideo) {
        if (SubtitleSessionMemory.preferredTrackNumber != null) return@LaunchedEffect
        // This video has a subtitle choice of its own: never override it with a language default.
        val videoForSelection = currentVideo
        if (videoForSelection != null && CustomSubtitlePersistenceManager.getSubtitleSelection(
                context = context,
                path = videoForSelection.path,
                videoId = videoForSelection.id,
                uriString = videoForSelection.uri.toString(),
                title = videoForSelection.displayName
            ) != null
        ) return@LaunchedEffect
        val currentPath = currentVideo?.path.orEmpty()
        val appliesToCurrent = !uiState.playerSettings.applySubtitleLanguageToSelectedContentOnly ||
            uiState.playerSettings.selectedSubtitleVideos.any { it.equals(currentPath, ignoreCase = true) } ||
            uiState.playerSettings.selectedSubtitleFolders.any { folder -> currentPath.equals(folder, ignoreCase = true) || currentPath.startsWith(folder.trimEnd(File.separatorChar) + File.separator) }
        val preferred = uiState.playerSettings.preferredSubtitleLanguages
            .split(',', ';')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
        if (realSubtitleTracks.isEmpty()) return@LaunchedEffect

        // Settings > Network > Media settings: a provider video starts with the subtitle track of the chosen language
        val mediaSubtitleLanguage = if (pluginSession != null && com.example.util.NetworkSettings.mediaAutoSubtitle(context)) {
            com.example.util.NetworkSettings.mediaLanguage(context)
        } else ""
        val mediaSubtitle = if (mediaSubtitleLanguage.isNotBlank()) {
            realSubtitleTracks
                .filter { AudioLanguageMatcher.doesTrackMatchLanguage(it, mediaSubtitleLanguage) }
                .maxByOrNull { (if (it.isDefault) 20 else 0) + (if (it.isForced) -15 else 0) + (if (it.isHearingImpaired) -10 else 0) }
        } else null

        val best = mediaSubtitle ?: if (preferred.isNotEmpty() && appliesToCurrent) {
            realSubtitleTracks.maxByOrNull { track ->
                var score = 0
                if (track.isDefault) score += 20
                if (track.isForced) score += if (uiState.playerSettings.subtitleSignsAndSongs) 35 else 10
                if (uiState.playerSettings.subtitleSignsAndSongs && Regex("(?i)signs|songs|forced|narrative").containsMatchIn(track.title)) score += 40
                val lang = track.language.lowercase()
                if (preferred.any { pref -> lang == pref }) score += 30
                else if (preferred.any { pref -> lang.startsWith("$pref-") || lang.startsWith(pref) }) score += 20
                score
            }?.takeIf { track ->
                preferred.any { pref ->
                    val lang = track.language.lowercase()
                    lang == pref || lang.startsWith("$pref-") || lang.startsWith(pref)
                }
            }
        } else {
            // No dashboard language preference: follow the MKV/container's default/forced flags.
            resolveContainerSubtitleFallback(realSubtitleTracks)
        }

        if (best != null && best.id != selectedSubtitleTrackId) {
            selectSubtitleTracks(
                best.id,
                controller.secondarySubtitleTrackId.value,
                notify = false,
                persist = false
            )
        }
    }

    LaunchedEffect(realAudioTracks, uiState.preferredAudioLanguages, uiState.applyAudioLanguageToSelectedContentOnly, uiState.selectedAudioFolders, uiState.selectedAudioVideos, uiState.rememberSelectedAudioTrack, currentVideo) {
        if (realAudioTracks.isNotEmpty() && !hasManualAudioTrackSelection) {
            // The audio track the user picked for THIS video always wins over language defaults.
            val rememberedVideo = currentVideo
            val rememberedTrack = if (uiState.rememberSelectedAudioTrack && rememberedVideo != null) {
                VideoAudioSelectionMemory.get(
                    context = context,
                    path = rememberedVideo.path,
                    videoId = rememberedVideo.id,
                    uriString = rememberedVideo.uri.toString(),
                    title = rememberedVideo.displayName
                )?.let { VideoAudioSelectionMemory.resolve(it, realAudioTracks) }
            } else null
            if (rememberedTrack != null) {
                if (!rememberedTrack.isSelected || rememberedTrack.id != selectedAudioTrackId) {
                    selectedAudioTrackId = rememberedTrack.id
                    controller.setAudioTrack(rememberedTrack.id)
                }
                return@LaunchedEffect
            }
            // A manual choice wins over default/forced and language preferences for the other videos of the same folder.
            val sessionTrack = VideoAudioSelectionMemory.findForFolder(context, rememberedVideo?.path, realAudioTracks)
            if (sessionTrack != null) {
                if (!sessionTrack.isSelected || sessionTrack.id != selectedAudioTrackId) {
                    selectedAudioTrackId = sessionTrack.id
                    controller.setAudioTrack(sessionTrack.id)
                }
                return@LaunchedEffect
            }
            val audioPath = currentVideo?.path.orEmpty()
            val audioPreferenceApplies = !uiState.applyAudioLanguageToSelectedContentOnly ||
                uiState.selectedAudioVideos.any { it.equals(audioPath, ignoreCase = true) } ||
                uiState.selectedAudioFolders.any { folder ->
                    audioPath.equals(folder, ignoreCase = true) ||
                        audioPath.startsWith(folder.trimEnd(File.separatorChar) + File.separator)
                }
            // Settings > Network > Media settings: a provider video starts with the audio track of the chosen language
            val mediaLanguage = if (pluginSession != null) com.example.util.NetworkSettings.mediaLanguage(context) else ""
            val mediaAudioTrack = if (mediaLanguage.isNotBlank()) {
                realAudioTracks.firstOrNull { AudioLanguageMatcher.doesTrackMatchLanguage(it, mediaLanguage) }
            } else null
            val bestTrack = if (mediaAudioTrack != null) {
                mediaAudioTrack
            } else if (uiState.preferredAudioLanguages.isNotBlank() && audioPreferenceApplies) {
                // Dashboard language preferences always have priority when configured for this video.
                AudioLanguageMatcher.findBestAudioTrack(
                    tracks = realAudioTracks,
                    videoPath = audioPath,
                    preferredLanguages = uiState.preferredAudioLanguages,
                    applyToSelectedContentOnly = false,
                    selectedFolders = emptySet(),
                    selectedVideos = emptySet(),
                    rememberSelectedTrack = uiState.rememberSelectedAudioTrack
                )
            } else {
                // No applicable dashboard preference: use the MKV/container's own
                // automatic/default/forced metadata instead of forcing a hard-coded language.
                resolveContainerAudioFallback(realAudioTracks)
            }
            // Also compare with what mpv is REALLY playing (isSelected): after an episode change the UI id is
            // reset to 1, so "same id" no longer proves the right (e.g. Japanese) track is the active one.
            if (bestTrack != null && (bestTrack.id != selectedAudioTrackId || !bestTrack.isSelected)) {
                selectedAudioTrackId = bestTrack.id
                controller.setAudioTrack(bestTrack.id)
            }
        }
    }

    fun persistPlaybackPosition(posMs: Long = currentPositionMs, forceDisk: Boolean = true) {
        val vid = persistVideoState ?: return
        if (!persistSaveOnQuitState) return
        // live TV has no meaningful resume position
        if (controller.isLiveStream.value) return

        val controllerVal = controller.currentPositionMs.value
        val rawPos = if (controllerVal > 0L) controllerVal else posMs

        val effectivePos = if (rawPos <= 500L && !controller.isResumeSeekVerified) {
            val saved = PlaybackHistoryManager.getSavedPosition(
                context = context,
                videoId = vid.id,
                path = vid.path,
                uriString = vid.uri.toString(),
                title = vid.displayName
            )
            if (saved > 500L) saved else rawPos
        } else {
            rawPos
        }

        val effectiveDur = if (durationMs > 0L) durationMs else controller.durationMs.value

        if (effectivePos >= 0L) {
            PlaybackHistoryManager.savePosition(
                context = context,
                videoId = vid.id,
                path = vid.path,
                positionMs = effectivePos,
                durationMs = effectiveDur,
                uriString = vid.uri.toString(),
                title = vid.displayName,
                forceDiskWrite = forceDisk
            )
        }
    }

    var isExitingPlayer by remember { mutableStateOf(false) }
    // Back from a LANDSCAPE player: remembered at the very first frame of Back (before the window turns portrait),
    // together with the side the screen was rotated to, so the player keeps drawing itself landscape while it slides down.
    var exitFromLandscape by remember { mutableStateOf(false) }
    var exitRotationSign by remember { mutableStateOf(90f) }
    // Back while the phone is held upside-down in portrait (ROTATION_180): same handling as landscape. The window is
    // snapped to the natural portrait (no system flip) and the player is drawn turned 180 degrees inside it, so every
    // exit slides the same way (top -> bottom of the natural-portrait window).
    var exitUpsideDown by remember { mutableStateOf(false) }
    var exitWindowReady by remember { mutableStateOf(false) }
    var exitFreezeFrameBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // --- Dismiss animation state (drag-to-dismiss gesture removed: the player now closes only via Back button / back gesture) ---
    // A drag starting near the phone's top edge (camera side) and moving towards the bottom
    // edge dismisses the player, in both portrait and landscape. The whole player (video +
    // controls overlay) follows the finger via `dismissDragAnim`, then either springs back
    // (drag released short of the threshold) or completes into the normal exit animation
    // (dismissDragAnim's offset is why the ScreenTransition's outgoing content quietly resets
    // to 0 the moment `exitPlayerNow()` swaps the screen away).
    val dismissDragAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    fun readDisplayRotationForExit(): Int = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity?.display?.rotation ?: Surface.ROTATION_0
        } else {
            @Suppress("DEPRECATION")
            activity?.windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    } catch (_: Throwable) { Surface.ROTATION_0 }

    /**
     * Leaves the player screen smoothly and safely.
     * Persists position, captures live surface frame for seamless zero-flicker transition,
     * pauses playback, initiates the portrait orientation transition,
     * and triggers the back transition.
     */
    fun exitPlayerNow(skipPersist: Boolean = false, toMini: Boolean = false) {
        if (isExitingPlayer) return
        val exitDisplayRot = readDisplayRotationForExit()
        exitUpsideDown = !isLandscape && !entryFakeLandscape && exitDisplayRot == Surface.ROTATION_180
        exitFromLandscape = (isLandscape && !entryFakeLandscape) || exitUpsideDown
        exitRotationSign = when {
            exitUpsideDown -> 180f
            exitDisplayRot == Surface.ROTATION_270 -> -90f
            else -> 90f
        }
        isExitingPlayer = true
        // Memory only here: the disk write (and its history version bump) used to land on the very first frame of the slide.
        // The screen's dispose safety net writes it to disk once the slide is over.
        if (!skipPersist) persistPlaybackPosition(forceDisk = false)
        // Settings > Player > General > "Video mini player": a manual Back keeps the video playing in the mini player.
        // The mini player follows ONLY the "Video mini player" setting, independent of the pinned video layout:
        // pinned layout on + mini player off => no mini player; pinned layout off + mini player on => mini player.
        miniParkRequested = toMini &&
            uiState.playerSettings.videoMiniPlayer &&
            controller.isAlive &&
            (!controller.onlineStreamMode || (channelEligible && currentVideo?.isOnline == true)) &&
            miniViewHolder[0] != null &&
            com.example.player.VideoMiniPlayerManager.isEligible(currentVideo, allowOnline = channelEligible)
        val snap = controller.lastCapturedFrame.value
        if (snap != null) {
            exitFreezeFrameBitmap = snap
        }
        controller.captureCurrentFrame { bmp ->
            if (bmp != null) {
                exitFreezeFrameBitmap = bmp
            }
        }
        if (!miniParkRequested) controller.pause()

        // The player owns the visual back animation, so Android must not add its own
        // rotate-the-whole-window animation underneath it. Without this, the already-portrait
        // folder/library surface can visibly rotate for a few frames while the landscape player
        // is sliding down. JUMPCUT removes only that system rotation animation; the existing
        // Compose player rotation/slide animation remains unchanged.
        // Only needed when the window really is landscape. From a portrait player nothing rotates, so the window is
        // left alone (touching window attributes forces an extra layout pass at the very first frame of Back).
        if (exitFromLandscape || isLandscape || exitUpsideDown) {
            activity?.window?.let { window ->
                val params = window.attributes
                params.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
                window.attributes = params
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        waitForPortraitBeforeBack = true
    }

    LaunchedEffect(waitForPortraitBeforeBack, configuration.orientation) {
        if (waitForPortraitBeforeBack && configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Upside-down portrait: no configuration change is reported, so wait (a few frames at most) until the display
            // really is the natural portrait before the player is drawn turned 180 degrees.
            if (exitUpsideDown) {
                var frames = 0
                while (frames < 30 && readDisplayRotationForExit() != Surface.ROTATION_0) {
                    androidx.compose.runtime.withFrameNanos { }
                    frames++
                }
                exitWindowReady = true
            }
            // Landscape exit: let the portrait configuration commit ONE frame before MainActivity swaps to HOME.
            // Portrait exit: nothing rotates, so go back at once (the old 2 frame wait looked like a short freeze).
            if (exitFromLandscape) androidx.compose.runtime.withFrameNanos { }
            waitForPortraitBeforeBack = false
            // Restore Android's normal rotation behavior for future orientation changes.
            if (exitFromLandscape) activity?.window?.let { window ->
                val params = window.attributes
                params.rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_ROTATE
                window.attributes = params
            }
            // The mini player takes the playing video over, but ONLY after the slide-down is finished. Doing it before
            // onBack() (view detach, session publish, foreground service start, notification) froze the first frames of the
            // slide, and the animation then caught up with a super-fast jump. The session is only flagged here so the
            // screen does not release the controller when it leaves; the manager parks it right after the slide.
            val parkVideo = currentVideo
            val parkView = miniViewHolder[0]
            var parkSession: com.example.player.VideoMiniSession? = null
            if (miniParkRequested && parkVideo != null && parkView != null && controller.isAlive) {
                miniParked = true
                parkSession = com.example.player.VideoMiniSession(controller, parkView, parkVideo, currentPlaylist, lastLoadedVideoKey)
            }
            onBack()
            if (parkSession != null) {
                val slideMs = (420f / uiState.animationSpeed.coerceAtLeast(0.25f)).toLong().coerceIn(0L, 1400L)
                com.example.player.VideoMiniPlayerManager.parkAfterExit(context, parkSession, slideMs + 40L)
            }
        }
    }

    // A corrupt / unplayable OFFLINE video: leave the player immediately and silently (no message, no notification),
    // exactly like pressing Back. The broken file is not written to the watch history.
    val localPlaybackFailed by controller.playbackFailed.collectAsState()
    LaunchedEffect(localPlaybackFailed) {
        if (localPlaybackFailed && !controller.onlineStreamMode) {
            try {
                exitPlayerNow(skipPersist = true)
            } catch (_: Throwable) {
                try { onBack() } catch (_: Throwable) { }
            }
        }
    }

    /**
     * Unified playlist navigation & episode playback function.
     * Systematically saves current state, resets video-specific parameters,
     * updates index & UI state, loads the target media source into MPV engine,
     * and manages subtitle fonts & chapters consistently.
     */
    fun playEpisodeAtIndex(targetIndex: Int) {
        if (currentPlaylist.isEmpty()) return
        val validIndex = targetIndex.coerceIn(0, currentPlaylist.size - 1)

        // A series opened from a provider queues all its episodes: this one is only a placeholder until now, so its
        // stream is requested from the website first (like tapping it), then it opens through the normal path below.
        val queuedTarget = currentPlaylist.getOrNull(validIndex)
        if (queuedTarget != null && com.example.util.media.PluginQueuedEpisodes.isQueued(queuedTarget.path)) {
            val queuedPath = queuedTarget.path
            if (!resolvingQueued.add(queuedPath)) return
            queuedResolving = true
            queuedResolveError = null
            coroutineScope.launch {
                val result = try {
                    com.example.util.media.PluginQueuedEpisodes.resolve(context, queuedPath)
                } finally {
                    resolvingQueued.remove(queuedPath)
                    queuedResolving = false
                }
                val real = result.getOrNull()
                if (real == null) {
                    val why = result.exceptionOrNull()?.message ?: "unknown error"
                    if (com.example.util.media.PluginQueuedEpisodes.isQueued(currentVideo?.path)) {
                        // nothing plays yet (first open): show Retry
                        queuedResolveError = why
                    } else {
                        Toast.makeText(context, "Could not get this episode: $why", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val at = currentPlaylist.indexOfFirst { it.path == queuedPath }
                if (at < 0) return@launch
                currentPlaylist = currentPlaylist.toMutableList().also { it[at] = real }
                playEpisodeAtIndex(at)
            }
            return
        }

        // 1. Save position of current episode before navigating away
        persistPlaybackPosition()

        // 2. Reset episode-specific states & adjustments
        loopPointA = null
        loopPointB = null
        subtitleOffsetMs = 0L
        audioDelayMs = 0L
        controller.setAudioDelay(0L)
        controller.setSubtitleDelay(0L)
        videoZoomScale = 1.0f
        videoPanOffset = Offset.Zero
        isPanAndZoomEnabled = false
        selectedAudioTrackId = 1
        selectedSubtitleTrackId = 0
        lastTouchedSubtitleTrackId = 0
        isSubtitleVisible = false

        // 3. Update active episode index
        currentVideoIndex = validIndex

        // 4. Retrieve target VideoItem and resolved file path
        val targetVideo = currentPlaylist.getOrNull(validIndex) ?: return
        com.example.util.MediaSeenManager.markSeen(context, targetVideo.path, targetVideo.id)
        // This episode is now the "last watched" one right away (not only after the first 3 s tick),
        // so the folder's resume button can never fall back to the episode before it.
        com.example.util.PlaybackHistoryManager.markOpened(
            context = context,
            videoId = targetVideo.id,
            path = targetVideo.path,
            uriString = targetVideo.uri.toString(),
            title = targetVideo.displayName,
            durationMs = targetVideo.durationMs
        )
        val path = getPlayableFilePath(context, targetVideo.uri, targetVideo.path)

        // A "stub" MP4 (placeholder clip + unreadable appended data, made for another app's own player)
        // would otherwise just look like a broken video: tell the user what it is.
        coroutineScope.launch(Dispatchers.IO) {
            if (com.example.util.PackagedStubVideoDetector.isPackagedStub(path)) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "यह file किसी दूसरे app के विशेष format में है, इसलिए यहाँ सिर्फ़ placeholder दिख रहा है",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // 5. Restore saved position & duration (respecting Resume Playback setting)
        val savedPos = if (uiState.playerSettings.savePositionOnQuit) {
            PlaybackHistoryManager.getSavedPosition(
                context = context,
                videoId = targetVideo.id,
                path = targetVideo.path,
                uriString = targetVideo.uri.toString(),
                title = targetVideo.displayName
            )
        } else 0L
        val initialPos = if (savedPos > 500L) savedPos else 0L
        currentPositionMs = initialPos
        if (targetVideo.durationMs > 0) {
            durationMs = targetVideo.durationMs
        } else if (targetVideo.isDirectStream) {
            durationMs = 0L
        }

        // 6. Update thumbnail
        val cachedThumb = ThumbnailCache.getCachedThumbnail(
            videoId = targetVideo.id,
            path = targetVideo.path,
            strategy = uiState.thumbnailStrategy,
            quality = uiState.thumbnailQuality,
            fallbackSecond = uiState.thumbnailFallbackSecond
        )
        if (cachedThumb != null) {
            thumbnailBitmap = cachedThumb
        } else if (!targetVideo.isOnline) {
            coroutineScope.launch {
                val bmp = loadVideoThumbnail(
                    context,
                    targetVideo.id,
                    targetVideo.uri,
                    targetVideo.path,
                    strategy = uiState.thumbnailStrategy,
                    quality = uiState.thumbnailQuality,
                    fallbackSecond = uiState.thumbnailFallbackSecond
                )
                if (bmp != null) thumbnailBitmap = bmp
            }
        }

        // 7. Load into MPV engine & initiate playback
        if (path.isNotBlank() && (File(path).exists() || path.startsWith("fd://") || path.startsWith("http://") || path.startsWith("https://"))) {
            val key = "${targetVideo.id}_${path}_$validIndex"
            lastLoadedVideoKey = key
            // A subtitle choice the user made for THIS video must not be overridden (or flashed
            // over) by a default track while the video opens.
            val savedSubtitleSelection = CustomSubtitlePersistenceManager.getSubtitleSelection(
                context = context,
                path = targetVideo.path,
                videoId = targetVideo.id,
                uriString = targetVideo.uri.toString(),
                title = targetVideo.displayName
            )
            controller.holdSubtitleAutoSelect = savedSubtitleSelection != null
            // Opening a file (demuxer probe, hwdec init, GL surface) is heavy. Doing it while the
            // slide-in animation is still running is what made the entry stutter, so on the very
            // first load of this screen the animation is allowed to finish first.
            val firstLoad = !entryLoadDelayDone
            entryLoadDelayDone = true
            val entryDelayMs = (150 / uiState.animationSpeed.coerceIn(0.25f, 2f)).toLong()
            coroutineScope.launch {
                if (firstLoad) kotlinx.coroutines.delay(entryDelayMs)
                if (lastLoadedVideoKey != key) return@launch
                val isArchive = com.example.util.archive.ArchiveStore.isArchivePath(targetVideo.path) ||
                    com.example.util.archive.ArchiveStreamServer.isLoopbackUrl(path)
                if (targetVideo.isDirectStream) {
                    // Imported M3U / IPTV stream: the real stream URL goes straight into the existing mpv player
                    // (with the stream's own User-Agent / headers); no YouTube resolving.
                    onlinePlayback = null
                    openDirectStream(path, initialPos, key)
                } else if (targetVideo.isNetworkStream) {
                    onlinePlayback = null
                    startNetworkPlayback(path, initialPos, key)
                } else if (isArchive) {
                    onlinePlayback = null
                    controller.setArchiveStream(true)
                    controller.loadVideo(path, startPositionMs = initialPos)
                    controller.play()
                } else if (targetVideo.isOnline) {
                    startOnlinePlayback(path, initialPos, key)
                } else {
                    controller.setOnlineStream(false)
                    controller.loadVideo(path, startPositionMs = initialPos)
                    controller.play()
                }
                isPlaying = true

                // Re-attach the user's uploaded subtitles + remembered selection (see restoreSubtitlesForVideo)
                restoreSubtitlesForVideo(targetVideo, key, savedSubtitleSelection)
            }
        }

        // 8. Sync subtitle fonts for the new directory in background
        coroutineScope.launch {
            try {
                SubtitleFontManager.syncFonts(context, targetVideo.path)
            } catch (_: Throwable) {}
        }
    }

    // Set by the Now Playing reorder: the playing video only moved inside the list, it must not be loaded again.
    var reorderKeepKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentVideoIndex, currentPlaylist) {
        val targetVideo = currentPlaylist.getOrNull(currentVideoIndex) ?: video ?: return@LaunchedEffect
        if (reorderKeepKey == "${targetVideo.id}|${targetVideo.path}") {
            reorderKeepKey = null
            lastLoadedVideoKey = "${targetVideo.id}_${getPlayableFilePath(context, targetVideo.uri, targetVideo.path)}_$currentVideoIndex"
            return@LaunchedEffect
        }
        // A provider video opens the player first: its servers are fetched and raced in here (first working one plays).
        if (com.example.util.media.PluginQueuedEpisodes.isQueued(targetVideo.path)) {
            if (queuedResolveError == null) playEpisodeAtIndex(currentVideoIndex)
            return@LaunchedEffect
        }
        val path = getPlayableFilePath(context, targetVideo.uri, targetVideo.path)
        if (path.isNotBlank() && (File(path).exists() || path.startsWith("fd://") || (targetVideo.isOnline && path.startsWith("http")) || com.example.util.archive.ArchiveStreamServer.isLoopbackUrl(path))) {
            val key = "${targetVideo.id}_${path}_$currentVideoIndex"
            if (lastLoadedVideoKey != key) {
                val adopted = miniAdopted
                if (miniAdoptPending && adopted != null && adopted.controller.isAlive &&
                    ((adopted.video.path.isNotBlank() && adopted.video.path == targetVideo.path) ||
                        (adopted.video.id == targetVideo.id && adopted.video.uri == targetVideo.uri))
                ) {
                    miniAdoptPending = false
                    lastLoadedVideoKey = key
                    if (targetVideo.isOnline) {
                        // The stream keeps playing; only the quality / audio lists of the menu are read again (cached).
                        coroutineScope.launch {
                            val pb = OnlineVideoService.resolvePlayback(OnlineVideoService.toWatchUrl(targetVideo.path)) ?: return@launch
                            onlinePlayback = pb
                            val saved = onlinePrefs.getString("quality", null)
                            onlineAutoMode = saved == null || saved == OnlineVideoService.AUTO_QUALITY
                            selectedOnlineQuality = (pb.qualities.firstOrNull { it.label == saved }
                                ?: pb.qualities.firstOrNull { it.height <= 480 } ?: pb.qualities.lastOrNull())?.label
                        }
                    }
                } else {
                    playEpisodeAtIndex(currentVideoIndex)
                }
            }
            miniAdoptPending = false
        }
    }

    // Hardware-synchronized A-B loop boundaries, repeat mode auto-advance, and periodic playback persistence
    // The end-of-video action must run ONCE per ending. It is re-armed as soon as playback is no
    // longer at the end (new episode loaded, or the user dragged the slider back).
    var endActionHandled by remember { mutableStateOf(false) }
    LaunchedEffect(controllerPos, isPlaying, loopPointA, loopPointB, repeatMode, durationMs, controllerEof) {
        val ptA = loopPointA
        val ptB = loopPointB
        val isAtEnd = (durationMs > 1000L && controllerPos >= (durationMs - 450L)) || (controllerEof && durationMs > 0L)
        // Stays "handled" while the old episode's end state is still visible during the switch to the
        // next one (otherwise the next-next episode would be started too); re-armed once playback has
        // left the end (new file loaded / user seeked back).
        if (!isAtEnd) {
            endActionHandled = false
        }
        if (ptA != null && ptB != null && ptB > ptA && isPlaying) {
            if (controllerPos >= ptB) {
                controller.seekTo(ptA)
            }
        // At EOF mpv (keep-open) is PAUSED, so `isPlaying` is false there: the old `isAtEnd && isPlaying`
        // test never became true and the player just sat on the last frame.
        } else if (isAtEnd && (isPlaying || controllerEof) && !endActionHandled) {
            endActionHandled = true
            when (repeatMode) {
                PlayerRepeatMode.ONE -> {
                    controller.seekTo(0L)
                    controller.play()
                }
                PlayerRepeatMode.ALL -> {
                    val nextIdx = if (currentVideoIndex + 1 < currentPlaylist.size) currentVideoIndex + 1 else 0
                    playEpisodeAtIndex(nextIdx)
                }
                PlayerRepeatMode.OFF -> {
                    if (currentVideoIndex + 1 < currentPlaylist.size) {
                        if (uiState.playerSettings.autoplayNext) {
                            playEpisodeAtIndex(currentVideoIndex + 1)
                        } else if (uiState.playerSettings.closeAfterEnd) {
                            exitPlayerNow()
                        } else {
                            persistPlaybackPosition(durationMs)
                            controller.pause()
                        }
                    } else {
                        // Last episode in playlist
                        if (uiState.playerSettings.repeatPlaylistAfterLast && currentPlaylist.size > 1) {
                            showNotification("Restarting playlist")
                            playEpisodeAtIndex(0)
                        } else if (uiState.playerSettings.closeAfterEnd) {
                            exitPlayerNow()
                        } else {
                            persistPlaybackPosition(durationMs)
                            controller.pause()
                        }
                    }
                }
            }
        }

        // Periodic state save every 3 seconds of active playback (debounced memory update)
        if (controllerPos > 0L && currentVideo != null && (controllerPos / 1000) % 3 == 0L) {
            persistPlaybackPosition(controllerPos, forceDisk = false)
        }
    }

    var onlineSkipMarkers by remember(currentVideo) { mutableStateOf<List<ChapterSkipMarker>>(emptyList()) }

    LaunchedEffect(currentVideo, uiState.playerSettings.onlineSkipMarkers, uiState.playerSettings.markerProvider, durationMs) {
        onlineSkipMarkers = emptyList()
        if (!uiState.playerSettings.onlineSkipMarkers || currentVideo == null || durationMs <= 0L) return@LaunchedEffect
        val remote = OnlineSkipMarkerService.fetch(
            provider = uiState.playerSettings.markerProvider,
            title = currentVideo.displayName,
            durationMs = durationMs
        )
        onlineSkipMarkers = remote.mapIndexed { index, marker ->
            ChapterSkipMarker(
                chapterIndex = -10000 - index,
                originalTitle = "Online ${marker.type.defaultPrimaryLabel}",
                displayButtonTitle = marker.type.defaultPrimaryLabel,
                secondaryTitle = "Online",
                type = marker.type,
                startTimeMs = marker.startMs,
                endTimeMs = marker.endMs
            )
        }
    }

    // Accurately detected chapter-based skip markers using real embedded metadata & configured settings
    val skipMarkers = remember(
        realChapters,
        durationMs,
        uiState.playerSettings.detectChapterIntroOutro,
        uiState.playerSettings.customOpeningKeywords,
        uiState.playerSettings.customEndingKeywords
    ) {
        (ChapterSkipDetector.detectSkipMarkers(
            chapters = realChapters,
            durationMs = durationMs,
            settings = uiState.playerSettings
        ) + onlineSkipMarkers).distinctBy {
            Triple(it.type, it.startTimeMs, it.endTimeMs)
        }.sortedBy { it.startTimeMs }
    }

    // Auto-skip opening/ending chapters when playback enters detected ranges
    var lastAutoSkippedMarkerIndex by remember(currentVideo?.path) { mutableStateOf<Int?>(null) }
    LaunchedEffect(
        currentPositionMs,
        skipMarkers,
        uiState.playerSettings.autoSkipIntro,
        uiState.playerSettings.autoSkipOutro
    ) {
        if (skipMarkers.isEmpty()) return@LaunchedEffect
        val activeMarker = ChapterSkipDetector.getActiveMarker(skipMarkers, currentPositionMs) ?: return@LaunchedEffect
        if (lastAutoSkippedMarkerIndex == activeMarker.chapterIndex) return@LaunchedEffect

        // Opening / Intro Auto-Skip
        if (activeMarker.type.isOpening && uiState.playerSettings.autoSkipIntro) {
            if (currentPositionMs < activeMarker.endTimeMs - 800L) {
                lastAutoSkippedMarkerIndex = activeMarker.chapterIndex
                controller.seekTo(activeMarker.endTimeMs)
                currentPositionMs = activeMarker.endTimeMs
                persistPlaybackPosition(activeMarker.endTimeMs)
                showNotification("Auto-skipped ${activeMarker.type.defaultPrimaryLabel.removePrefix("Skip ")}")
            }
        }
        // Ending / Outro / Preview Auto-Skip
        else if (activeMarker.type.isEnding && uiState.playerSettings.autoSkipOutro) {
            if (currentPositionMs < activeMarker.endTimeMs - 800L) {
                lastAutoSkippedMarkerIndex = activeMarker.chapterIndex
                val isAtEnd = durationMs > 0 && activeMarker.endTimeMs >= (durationMs - 1000L)
                if (isAtEnd) {
                    if (currentVideoIndex + 1 < currentPlaylist.size) {
                        showNotification("Auto-skipped to next episode")
                        playEpisodeAtIndex(currentVideoIndex + 1)
                    } else if (uiState.playerSettings.repeatPlaylistAfterLast && currentPlaylist.size > 1) {
                        showNotification("Restarting playlist")
                        playEpisodeAtIndex(0)
                    } else {
                        persistPlaybackPosition(durationMs)
                        exitPlayerNow()
                    }
                } else {
                    controller.seekTo(activeMarker.endTimeMs)
                    currentPositionMs = activeMarker.endTimeMs
                    persistPlaybackPosition(activeMarker.endTimeMs)
                    showNotification("Auto-skipped ${activeMarker.type.defaultPrimaryLabel.removePrefix("Skip ")}")
                }
            }
        }
    }

    // Reset orientation & exit immersive mode when exiting player
    BackHandler {
        exitPlayerNow(toMini = true)
    }

    // App & Screen Lifecycle Event Observer to handle background/foreground transitions cleanly
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestCurrentVideo by rememberUpdatedState(currentVideo)
    val latestIsPlaying by rememberUpdatedState(isPlaying)

    // Sleep timer finished while the video player is open: close it with the normal exit
    // animation (freeze-frame + portrait + back). MainActivity then steps back out of the
    // folder and closes the app. If the screen is off / the app is in the background there is
    // no UI to animate, so just stop playback (MainActivity closes the app directly).
    LaunchedEffect(Unit) {
        com.example.util.SleepTimerManager.expired.collect {
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                showNotification("Sleep timer ended")
                exitPlayerNow()
            } else {
                controller.pause()
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    controller.onStart()
                }
                Lifecycle.Event.ON_RESUME -> {
                    controller.onResume()
                    // The user may have opened a file manager/text editor while the player was
                    // paused and changed the ORIGINAL uploaded subtitle. Sync that source back
                    // into the canonical app-private copy, then reload the active MPV subtitle
                    // without adding another track. This also covers timing/text edits.
                    val resumedVideo = latestCurrentVideo
                    if (resumedVideo != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val attached = CustomSubtitlePersistenceManager.getAttachedSubtitles(
                                    context = context,
                                    path = resumedVideo.path,
                                    videoId = resumedVideo.id,
                                    uriString = resumedVideo.uri.toString(),
                                    title = resumedVideo.displayName
                                )
                                val changedRecords = mutableListOf<AttachedSubtitleRecord>()
                                for (record in attached) {
                                    if (CustomSubtitlePersistenceManager.syncAttachedFromSource(context, record)) {
                                        changedRecords += record
                                    }
                                }
                                if (changedRecords.isNotEmpty() && latestCurrentVideo?.id == resumedVideo.id) {
                                    // Do a seamless external-track hand-off. Do NOT call
                                    // selectSubtitleTracksAtomic() here: that path is for user
                                    // selection and must never reload an already-visible subtitle.
                                    for (record in changedRecords) {
                                        try {
                                            controller.reloadExternalSubtitleSource(
                                                filePath = record.filePath,
                                                originalName = record.originalName,
                                                context = context
                                            )
                                        } catch (_: Throwable) {}
                                    }
                                }
                            } catch (_: Throwable) {}
                        }
                    }
                    if (uiState.playerSettings.autoplayAfterUnlock && !latestIsPlaying) {
                        controller.play()
                        isPlaying = true
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    persistPlaybackPosition()
                    controller.onPause()
                }
                Lifecycle.Event.ON_STOP -> {
                    persistPlaybackPosition()
                    val act = activity
                    if (act != null && uiState.playerSettings.autoPictureInPicture && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !act.isFinishing) {
                        try {
                            act.enterPictureInPictureMode(
                                PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                            )
                        } catch (_: Throwable) {}
                    }
                    controller.onStop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    persistPlaybackPosition()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Background thumbnail warming must not compete with the video that is opening / playing.
    DisposableEffect(Unit) {
        com.example.ui.components.ThumbnailPrefetcher.setPaused(true)
        onDispose { com.example.ui.components.ThumbnailPrefetcher.setPaused(false) }
    }

    // Keep-screen-on: the screen stays awake ONLY while the video is actually playing.
    // When paused (or stopped at the end) the flag is cleared so the screen locks after the
    // normal system screen-timeout, even if the player is still open.
    // When the player is dismissed, onDispose clears the flag so Folder/File browsing locks normally.
    DisposableEffect(activity, isPlaying) {
        val win = activity?.window
        if (isPlaying) {
            win?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            win?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            win?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Screen bars and brightness follow the active Player Settings.
    LaunchedEffect(brightness) {
        val window = activity?.window ?: return@LaunchedEffect
        val lp = window.attributes
        lp.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        window.attributes = lp
    }

    // NOTE: This effect is intentionally keyed on the display settings ONLY for applying
    // insets, and its onDispose here must NOT release the mpv controller — otherwise simply
    // toggling "show status bar" etc. mid-playback would tear down and restart the player.
    val channelBarsOn = channelEligible && !isLandscape

    // Landscape full screen: no white / blank strip beside the video on phones with a notch, punch-hole or curved edge.
    // (1) the window may draw into the display cutout area, (2) whatever is not covered by the video is black,
    // not the app's light launch background. Both are restored when the player closes.
    DisposableEffect(activity) {
        val window = activity?.window
        val oldBackground = window?.decorView?.background
        val oldCutoutMode = if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.attributes.layoutInDisplayCutoutMode else 0
        window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))
        if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                if (oldBackground != null) window.setBackgroundDrawable(oldBackground)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val lp = window.attributes
                    lp.layoutInDisplayCutoutMode = oldCutoutMode
                    window.attributes = lp
                }
            }
        }
    }

    DisposableEffect(activity, uiState.playerSettings.showStatusBar, uiState.playerSettings.showNavigationBar, uiState.playerSettings.safeAreaWindow, channelBarsOn, isLandscape) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            val allTypes = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            var showTypes = 0
            if (uiState.playerSettings.showStatusBar) showTypes = showTypes or WindowInsetsCompat.Type.statusBars()
            if (uiState.playerSettings.showNavigationBar) showTypes = showTypes or WindowInsetsCompat.Type.navigationBars()
            if (channelBarsOn) showTypes = allTypes
            if (showTypes != 0) insetsController.show(showTypes) else insetsController.hide(allTypes)
            // Landscape with both system bars hidden: the video fills the whole screen (including the notch side); the
            // controls overlay keeps its own cutout / bar padding, so buttons never sit under the notch.
            val landscapeBarsHidden = isLandscape && !uiState.playerSettings.showStatusBar && !uiState.playerSettings.showNavigationBar
            WindowCompat.setDecorFitsSystemWindows(window, if (channelBarsOn || landscapeBarsHidden) false else uiState.playerSettings.safeAreaWindow)
        }
        onDispose { /* insets are re-applied above on the next key change; nothing to tear down here */ }
    }

    // True screen-exit safety net: runs exactly once, only when this screen actually leaves
    // composition (not on every settings toggle). exitPlayerNow()/release() are idempotent,
    // so this is harmless even when BackHandler or an autoplay-close path already ran it.
    DisposableEffect(Unit) {
        onDispose {
            val win = activity?.window
            if (win != null) {
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            persistPlaybackPosition()
            // Parked in the video mini player: it keeps playing there and is released by the mini player.
            if (!miniParked) controller.release()
        }
    }

    val isBuffering by controller.isBuffering.collectAsState()
    val bufferedPositionMs by controller.bufferedPositionMs.collectAsState()

    /** Media plugin quality menu: opens the same movie / episode in another quality of that website, from the same second. */
    fun switchPluginQuality(label: String) {
        val session = pluginSession ?: return
        val video = currentVideo ?: return
        if (label == pluginQuality) return
        val choice = session.choices.firstOrNull { it.quality == label } ?: return
        val pos = controller.currentPositionMs.value
        val loadKey = lastLoadedVideoKey
        session.manualChoice = true
        coroutineScope.launch {
            Toast.makeText(context, "Loading $label…", Toast.LENGTH_SHORT).show()
            val result = com.example.util.media.MediaPlayback.resolveChoice(
                context, session.plugin, choice, session.episodeIndex, session.episodeTitle, session.isSeries
            )
            val stream = result.getOrNull()
            if (stream == null) {
                Toast.makeText(context, "Could not load $label: " + (result.exceptionOrNull()?.message ?: "unknown error"), Toast.LENGTH_LONG).show()
                return@launch
            }
            if (lastLoadedVideoKey != loadKey) return@launch
            // registers the new URL's headers (User-Agent / Referer) exactly like the first load
            com.example.util.media.MediaPlayback.toVideoItem(context, session.plugin, video.displayName, video.thumbnailUrl.orEmpty(), stream, session)
            session.selectedQuality = label
            pluginQuality = label
            pluginServerIndex = session.selectedServer
            ServerReputationStore.rememberChoice(context, session.plugin.value, "", stream.server, stream.quality ?: label)
            openPluginStream(stream, pos, loadKey, followPosition = true)
            attachPluginSubtitles(stream.subtitles, loadKey)
        }
    }

    var allServersFailed by remember { mutableStateOf(false) }

    /**
     * Providers list ONE link per quality of the same server (like CloudStream's own link list). When the playing stream
     * has no quality list of its own, those links are the qualities of the server that plays: (server index, link).
     */
    fun sameServerQualities(): List<Pair<Int, com.example.util.media.MediaStream>> {
        val session = pluginSession ?: return emptyList()
        val cur = session.servers.getOrNull(pluginServerIndex) ?: return emptyList()
        val name = cur.server.trim()
        val list = session.servers.withIndex()
            .filter { (_, st) -> st.server.trim().equals(name, ignoreCase = true) && !st.quality.isNullOrBlank() }
            .map { it.index to it.value }
        return if (list.size > 1) list else emptyList()
    }

    /** Server list: plays the same movie / episode from another source of the website, from the same second. */
    fun switchPluginServer(index: Int, automatic: Boolean = false) {
        val session = pluginSession ?: return
        val video = currentVideo ?: return
        val st = session.servers.getOrNull(index) ?: return
        if (index == session.selectedServer && !automatic) return

        if (!automatic) {
            session.manualChoice = true
            // the next episodes start with this server (and its quality) too, so it never has to be chosen again
            ServerReputationStore.rememberChoice(context, session.plugin.value, "", st.server, st.quality)
        } else {
            session.servers.getOrNull(session.selectedServer)?.let { prev ->
                ServerReputationStore.recordFailure(context, session.plugin.value, prev.server)
            }
        }

        allServersFailed = false
        session.selectedServer = index
        val pos = controller.currentPositionMs.value
        val loadKey = lastLoadedVideoKey
        coroutineScope.launch {
            Toast.makeText(context, if (automatic) "Switching server…" else "Switching to ${st.server}…", Toast.LENGTH_SHORT).show()
            com.example.util.media.MediaPlayback.toVideoItem(context, session.plugin, video.displayName, video.thumbnailUrl.orEmpty(), st, session)
            pluginServerIndex = session.selectedServer
            controller.clearError()
            openPluginStream(st, pos, loadKey, followPosition = true)
            attachPluginSubtitles(session.subtitles, loadKey)
        }
    }

    // servers of this episode that already failed while it plays (a failover never goes back to them)
    val failedServerLinks = remember(currentVideo) { HashSet<String>() }
    var failoverRunning by remember { mutableStateOf(false) }

    /**
     * Automatic failover: the server that plays is bad (error / buffering / stall). All the OTHER servers are checked at
     * the same time (parallel race) and the first one that works takes over from the same second, instead of trying
     * them one after another. When none proves to work, the next untried server is opened anyway; Retry shows only when
     * every server has failed.
     */
    fun failoverToWorkingServer() {
        val session = pluginSession ?: return
        if (failoverRunning) return
        failoverRunning = true
        val current = session.servers.getOrNull(session.selectedServer)
        current?.let { failedServerLinks.add(it.link) }
        coroutineScope.launch {
            try {
                val remaining = session.servers.filter { it.link !in failedServerLinks }
                if (remaining.isEmpty()) {
                    allServersFailed = true
                    return@launch
                }
                val outcome = try {
                    withContext(Dispatchers.IO) {
                        com.example.util.media.StreamProbe.verify(context, session.plugin.value, remaining)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    null
                }
                val pick = if (outcome != null && outcome.anyWorks) outcome.ordered.firstOrNull() else remaining.firstOrNull()
                val index = pick?.let { p -> session.servers.indexOfFirst { it.link == p.link } } ?: -1
                if (index < 0) {
                    allServersFailed = true
                } else {
                    switchPluginServer(index, automatic = true)
                }
            } finally {
                failoverRunning = false
            }
        }
    }

    /** Quality list of an HLS provider stream: "auto" (adaptive bitrate) or one rendition; remembered for the next episodes. */
    fun switchHlsQuality(key: String) {
        val variants = hlsVariants
        if (variants.isEmpty() || key == hlsSelectedKey) return
        val pos = controller.currentPositionMs.value
        if (key == "auto") {
            hlsSelectedKey = "auto"
            pluginVideoPrefs.edit().putString("hls_pref", "auto").apply()
            val current = pluginSession?.let { it.servers.getOrNull(it.selectedServer) }
            coroutineScope.launch {
                val speed = if (current != null) measureHlsSpeed(variants, current) else null
                applyHlsVariant(pickAutoHlsVariant(variants, speed), controller.currentPositionMs.value.coerceAtLeast(pos))
            }
            return
        }
        val v = variants.firstOrNull { hlsKey(it) == key } ?: return
        hlsSelectedKey = key
        pluginVideoPrefs.edit().putString("hls_pref", v.height.toString()).apply()
        applyHlsVariant(v, pos)
    }

    val isPlayingState by controller.isPlaying.collectAsState()

    // 1. Automatic Failover on Playback Error
    val streamError by controller.errorMessage.collectAsState()
    // The order of Vega's handleVideoError (and the same on CloudStream providers):
    //   1. a server that had started playing and then failed (dropped connection) is reloaded ONCE at the same second;
    //      it earns that retry back after playing for another minute,
    //   2. when the reload failed too the link has probably expired: the provider is asked for new links ONCE and the same
    //      server continues from where it stopped,
    //   3. only then the next server is used.
    // A stream that never started goes straight to the next server.
    val retriedAt = remember(currentVideo) { HashMap<String, Long>() }
    val refreshedLinks = remember(currentVideo) { HashSet<String>() }
    var errorHandling by remember { mutableStateOf(false) }
    LaunchedEffect(streamError, pluginSession) {
        val session = pluginSession ?: return@LaunchedEffect
        if (streamError == null || errorHandling) return@LaunchedEffect
        errorHandling = true
        // its own coroutine: clearing the error below restarts this effect, which must not cancel the work
        coroutineScope.launch {
            try {
                val video = currentVideo
                val current = session.servers.getOrNull(session.selectedServer)
                val loadKey = lastLoadedVideoKey
                if (current != null && video != null && !failoverRunning) {
                    val now = System.currentTimeMillis()
                    val last = retriedAt[current.link]
                    if (controller.hasStartedPlayback() && (last == null || now - last >= 60_000L)) {
                        retriedAt[current.link] = now
                        Toast.makeText(context, "Connection lost, retrying", Toast.LENGTH_SHORT).show()
                        val pos = controller.currentPositionMs.value
                        controller.clearError()
                        openPluginStream(current, pos, loadKey, followPosition = true)
                        attachPluginSubtitles(session.subtitles, loadKey)
                        return@launch
                    }
                    if (last != null && session.episodeLink.isNotBlank() && refreshedLinks.add(current.link)) {
                        Toast.makeText(context, "Getting a new link", Toast.LENGTH_SHORT).show()
                        val pos = controller.currentPositionMs.value
                        com.example.util.media.MediaProviderRepository.forgetStreams(session.plugin.value, session.episodeLink)
                        val fresh = withContext(Dispatchers.IO) {
                            com.example.util.media.MediaPlayback.resolve(
                                context, session.plugin, session.episodeLink, session.episodeType,
                                session.episodeTitle, session.episodeIndex, verify = false
                            ).getOrNull()
                        }
                        if (lastLoadedVideoKey != loadKey) return@launch
                        if (fresh != null) {
                            val all = listOf(fresh.copy(alternatives = emptyList())) + fresh.alternatives
                            // the same server as before (its name, then its quality), like Vega's findSameServer
                            val same = all.filter { it.server == current.server }
                                .maxByOrNull { (if (it.quality == current.quality) 2 else 0) + (if (it.link == current.link) 0 else 1) }
                            if (same != null) {
                                val withOthers = same.copy(alternatives = all.filter { it.link != same.link })
                                com.example.util.media.MediaPlayback.toVideoItem(
                                    context, session.plugin, video.displayName, video.thumbnailUrl.orEmpty(), withOthers, session
                                )
                                pluginServerIndex = session.selectedServer
                                controller.clearError()
                                openPluginStream(withOthers, pos, loadKey, followPosition = true)
                                attachPluginSubtitles(withOthers.subtitles.ifEmpty { session.subtitles }, loadKey)
                                return@launch
                            }
                        }
                    }
                }
                failoverToWorkingServer()
            } finally {
                errorHandling = false
            }
        }
    }

    // 2. Slow connection: CloudStream NEVER gives up a server because it buffers (ExoPlayer waits for data and lowers the
    //    quality by itself; only a real playback error moves on to the next server). The old rule here abandoned the server
    //    after 6 s of buffering or a 6 s stall, marked it as failed for 10 minutes and started the next one from zero, which
    //    looped through the servers while each of them was fine, just not instant. Now the QUALITY steps down instead
    //    (Auto mode, HLS) when the stream keeps running dry; a server is only left on a real error (effect 1 above).
    LaunchedEffect(pluginSession, lastLoadedVideoKey, hlsSelectedKey) {
        if (pluginSession == null) return@LaunchedEffect
        var lastStepAt = System.currentTimeMillis()
        while (true) {
            delay(2000L)
            if (hlsSelectedKey != "auto" || hlsVariants.size < 2) continue
            val now = System.currentTimeMillis()
            if (now - lastStepAt < 25_000L) continue
            if (controller.rebuffersWithin(60_000L) < 2) continue
            val currentBandwidth = hlsCurrentBandwidth ?: continue
            val lower = hlsVariants.filter { it.bandwidth < currentBandwidth }.maxByOrNull { it.bandwidth } ?: continue
            lastStepAt = now
            controller.clearRebufferHistory()
            Toast.makeText(context, "Slow connection: lowering quality to ${lower.label}", Toast.LENGTH_SHORT).show()
            applyHlsVariant(lower, controller.currentPositionMs.value)
        }
    }

    // 3. Preload the NEXT episode in the background during playback (cached for 15 minutes)
    LaunchedEffect(currentVideoIndex, currentPlaylist, pluginSession) {
        if (pluginSession == null) return@LaunchedEffect
        val next = currentPlaylist.getOrNull(currentVideoIndex + 1) ?: return@LaunchedEffect
        if (!com.example.util.media.PluginQueuedEpisodes.isQueued(next.path)) return@LaunchedEffect
        while (controller.currentPositionMs.value < 1000L) {
            delay(1000L)
        }
        com.example.util.media.PluginQueuedEpisodes.preloadNext(context, next.path)
    }

    // 4. Network Change Handler (Wi-Fi <-> Mobile data)
    DisposableEffect(Unit) {
        NetworkMonitor.register(context)
        val listener = NetworkMonitor.NetworkChangeListener { isAvailable ->
            if (isAvailable && pluginSession != null && currentVideo?.isDirectStream == true) {
                val currentServer = pluginSession?.servers?.getOrNull(pluginSession?.selectedServer ?: 0)
                if (currentServer != null) {
                    coroutineScope.launch {
                        // The player reconnects by itself and keeps playing from its buffer: the stream is only opened again when
                        // it is still waiting for data a few seconds after the switch (reloading a healthy video is what hurt).
                        delay(3000L)
                        if (!controller.isBuffering.value) return@launch
                        val pos = controller.currentPositionMs.value
                        Toast.makeText(context, "Network changed, reconnecting…", Toast.LENGTH_SHORT).show()
                        controller.loadVideo(currentServer.link, pos)
                        controller.play()
                    }
                }
            }
        }
        NetworkMonitor.addListener(listener)
        onDispose {
            NetworkMonitor.removeListener(listener)
            com.example.util.media.PluginQueuedEpisodes.cancelBackgroundWork()
            com.example.util.media.PluginQueuedEpisodes.clearPreloadCache()
        }
    }

    // "Auto" HLS quality is chosen once, when the video opens. The quality never steps up or down by itself while it
    // buffers or plays: only the person changes it (Quality list).

    /** Loads [q] at the current position (same moment, same audio track) without touching the saved preference. */
    fun applyOnlineQuality(q: OnlineVideoQuality) {
        val pb = onlinePlayback ?: return
        val pos = controller.currentPositionMs.value
        val audioIdx = controller.currentOnlineAudioIndex()
        selectedOnlineQuality = q.label
        val audios = if (q.isVideoOnly) pb.audios.map { OnlineAudioSource(it.url, it.title, it.language) } else emptyList()
        controller.setOnlineStream(true, audios, audioIdx, onlineChaptersOf(pb))
        if (webLinkPlayback) controller.setWebLinkStream(true)
        if (networkRequestHeaders.isNotEmpty()) controller.setNetworkRequestHeaders(networkRequestHeaders)
        controller.loadVideo(q.url, pos)
        controller.play()
    }

    /** Quality menu: "Auto" or a fixed quality. Either way the stream is reloaded at the same second. */
    fun switchOnlineQuality(label: String) {
        val pb = onlinePlayback ?: return
        if (label == OnlineVideoService.AUTO_QUALITY) {
            onlineAutoMode = true
            onlinePrefs.edit().putString("quality", OnlineVideoService.AUTO_QUALITY).apply()
            val target = OnlineVideoService.pickAutoQuality(context, pb.qualities) ?: return
            if (target.label != selectedOnlineQuality) applyOnlineQuality(target)
            return
        }
        val q = pb.qualities.firstOrNull { it.label == label } ?: return
        onlineAutoMode = false
        onlinePrefs.edit().putString("quality", label).apply()
        if (q.label == selectedOnlineQuality) return
        applyOnlineQuality(q)
    }

    // Smart Auto Playback: apply the decoder choice (AUTO or a manual mode) to the player.
    LaunchedEffect(decoderMode) {
        val manual = when (decoderMode) {
            DecoderMode.HW -> HwAccelMode.PREFER
            DecoderMode.SW -> HwAccelMode.DISABLE
            else -> HwAccelMode.FORCE
        }
        controller.setDecoderChoice(decoderMode == DecoderMode.AUTO, manual)
    }

    // Smart Auto Playback: tell the person when a video is demanding for this device or a fallback was used.
    val smartReport by controller.smartPlaybackReport.collectAsState()
    LaunchedEffect(smartReport) {
        val r = smartReport ?: return@LaunchedEffect
        val note = r.note ?: return@LaunchedEffect
        showNotification("Smart Auto: ${r.summary}. $note")
    }

    // Smart Auto Playback: a video that is too heavy for the device. Where a lower-resolution version exists (HLS
    // variants, online quality list) it is selected once; otherwise the person is told plainly that none is available.
    val smartAdvice by controller.smartLowerResolutionAdvice.collectAsState()
    LaunchedEffect(smartAdvice?.id) {
        val adv = smartAdvice ?: return@LaunchedEffect
        val target = adv.suggestedHeight
        val pos = controller.currentPositionMs.value
        var switchedTo: String? = null
        if (pluginSession != null) {
            val variants = hlsVariants
            if (variants.size >= 2) {
                val curH = variants.firstOrNull { it.bandwidth == hlsCurrentBandwidth }?.height?.takeIf { it > 0 } ?: adv.currentHeight
                val pick = variants.filter { it.height in 1..target && it.height < curH }
                    .maxWithOrNull(compareBy<com.example.util.media.HlsVariant>({ it.height }, { it.bandwidth }))
                if (pick != null) {
                    hlsSelectedKey = hlsKey(pick)
                    applyHlsVariant(pick, pos)
                    switchedTo = pick.label
                }
            }
        } else {
            val pb = onlinePlayback
            if (pb != null) {
                val curH = pb.qualities.firstOrNull { it.label == selectedOnlineQuality }?.height?.takeIf { it > 0 } ?: adv.currentHeight
                val pick = pb.qualities.filter { it.height in 1..target && it.height < curH }.maxByOrNull { it.height }
                if (pick != null) {
                    applyOnlineQuality(pick)
                    switchedTo = pick.label
                }
            }
        }
        if (switchedTo != null) {
            showNotification("Smart Auto: ${adv.currentHeight}p is too demanding for this device - playing $switchedTo")
        } else {
            showNotification("Smart Auto: ${adv.currentHeight}p is too demanding for this device (${adv.reason.lowercase()}). No lower-resolution version of this video is available, so frames may drop.")
        }
    }

    // Auto quality is chosen once, when the video opens. It never drops or climbs a step by itself while the video
    // buffers or plays: the quality only changes when the person picks another one.

    val playlistNames = remember(playlist) {
        if (playlist.isNotEmpty()) playlist.map { it.displayName }
        else listOf(videoTitle)
    }

    val currentConfig = LocalConfiguration.current
    val isWindowPortrait = currentConfig.orientation == Configuration.ORIENTATION_PORTRAIT
    val shouldRotateForExit = isExitingPlayer && isWindowPortrait && exitFromLandscape && (!exitUpsideDown || exitWindowReady)

    // Distance (in the drag axis) the player must travel before a release commits to closing
    // rather than springing back — kept as a density-independent constant used by both the
    // gesture handler and the live visual progress below.
    val dismissThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 120.dp.toPx() }

    // Context.getDisplay() only exists from API 30; below that we must use the deprecated
    // windowManager.defaultDisplay instead or this throws NoSuchMethodError at runtime.
    fun currentDisplayRotation(): Int {
        val act = activity ?: return Surface.ROTATION_0
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                act.display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                act.windowManager.defaultDisplay.rotation
            }
        } catch (_: Throwable) {
            Surface.ROTATION_0
        }
    }

    // ---- Portrait channel player geometry (same margins / header / gradient card as the channel screen) ----
    val channelMode = channelEligible && !isLandscape && !entryFakeLandscape && !shouldRotateForExit
    val chIsDark = uiState.themeMode == ThemeMode.DARK ||
        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    val chPageBg = when {
        chIsDark && uiState.amoledBlackMode -> com.example.ui.theme.DarkBackgroundStart
        chIsDark -> Neutral.D1B2630
        uiState.themeMode == ThemeMode.SYSTEM -> Neutral.FFFFFF
        else -> Neutral.DAE3E9
    }
    val chScale = (0.85f + 0.15f * ((uiState.appScale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
    val chHPad = (14f * chScale).dp
    val chStatusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chHeaderBottom = chStatusTop + (58f * chScale).dp + (6f * chScale).dp
    val chRatioTarget = if (realVideoWidth > 0 && realVideoHeight > 0)
        (realVideoWidth.toFloat() / realVideoHeight.toFloat()).coerceIn(0.5f, 2.4f) else 16f / 9f
    // No tween: animating the card height re-lays out (and re-sizes) the native video surface every frame for 280 ms,
    // which made the pinned video and its audio stutter whenever the ratio was read / changed. It snaps once instead,
    // and tiny ratio changes are ignored.
    var chRatioStable by remember { mutableStateOf(chRatioTarget) }
    if (kotlin.math.abs(chRatioStable - chRatioTarget) > 0.02f) chRatioStable = chRatioTarget
    val chRatio = chRatioStable
    val chCardW = configuration.screenWidthDp.dp - chHPad * 2
    // A tall video (Reels / Shorts style, ratio below 1) is opened tall, top to bottom; wide videos keep the normal card.
    val chCardMaxH = (configuration.screenHeightDp * (if (chRatio < 1f) 0.72f else 0.5f)).dp
    val chCardH = (chCardW / chRatio).coerceAtMost(chCardMaxH)
    val chCardShape = RoundedCornerShape(16.dp)

    // During Back, the player is dismissed as a rounded bottom-sheet surface. The radius is
    // animated only for the exit so the normal player UI remains edge-to-edge while playing.
    // The native video surface is replaced by the captured Compose frame during exit below,
    // allowing this clip to remain visually consistent while the whole player slides away.
    val exitCornerRadius by animateDpAsState(
        targetValue = if (isExitingPlayer) 30.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "PlayerExitCornerRadius"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(exitCornerRadius))
            .background(if (channelMode) chPageBg else Color.Black)
            .graphicsLayer {
                val progress = (dismissDragAnim.value / dismissThresholdPx).coerceIn(0f, 1f)
                // Stay fully opaque while dragging so portrait/landscape look identical.
                // One standard direction: always top -> bottom of the (natural portrait) window, whatever the phone's rotation.
                translationY = dismissDragAnim.value
            },

        contentAlignment = Alignment.Center
    ) {
        // Pinned video layout: the page behind the video card / details / recommended list plays the same Settings > Appearance >
        // Background animation as Home (the opaque chPageBg above is only the base colour under it).
        if (channelMode) com.example.background.PageBackdrop(uiState.themeMode)
        BoxWithConstraints(
            // Back from landscape: the window turns portrait instantly (so the library behind is portrait) while this
            // whole player stays drawn landscape (rotated layer) and only slides down - no flip, no portrait re-layout.
            modifier = if (entryFakeLandscape || shouldRotateForExit) {
                // Entry slide-in AND Back slide-out: the window is portrait, the player is drawn landscape inside it
                // (turned 90 degrees as ONE layer: video + controls together), so nothing ever flips or re-lays out.
                val screenW = currentConfig.screenWidthDp.dp
                val screenH = currentConfig.screenHeightDp.dp
                val fakeRotation = if (shouldRotateForExit) exitRotationSign else entryFakeRotation
                // Upside-down portrait keeps the full portrait size (only turned 180); landscape swaps width / height.
                (if (shouldRotateForExit && exitUpsideDown) Modifier.fillMaxSize() else Modifier.requiredSize(screenH, screenW))
                    .graphicsLayer {
                        rotationZ = fakeRotation
                    }
            } else if (channelMode) {
                // The whole player (surface + controls + loading) lives inside the channel-card area:
                // same side margins, rounded corners and gradient border, height follows the video's ratio.
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = chHPad, end = chHPad, top = chHeaderBottom + PinnedHeaderUi.searchExtra)
                    .fillMaxWidth()
                    .height(chCardH)
                    .consumeWindowInsets(WindowInsets.statusBars)
                    .consumeWindowInsets(WindowInsets.navigationBars)
                    .clip(chCardShape)
                    .border(1.5.dp, AccentGradient, chCardShape)
            } else {
                Modifier.fillMaxSize()
            }.background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
        val isFirstFrameReady = !isBuffering && (currentPositionMs > 100L)
        val posterAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isFirstFrameReady) 0f else 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 350,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "PosterAlpha"
        )

        // Calculate dynamic fill scale for Cover / Crop rendering modes
        val containerW = maxWidth.value
        val containerH = maxHeight.value
        val vw = if (realVideoWidth > 0) realVideoWidth.toFloat() else 16f
        val vh = if (realVideoHeight > 0) realVideoHeight.toFloat() else 9f

        val fillScale = remember(containerW, containerH, vw, vh) {
            val scaleX = if (vw > 0f) containerW / vw else 1f
            val scaleY = if (vh > 0f) containerH / vh else 1f
            val minScale = minOf(scaleX, scaleY)
            val maxScale = maxOf(scaleX, scaleY)
            if (minScale > 0.0001f) (maxScale / minScale).coerceAtLeast(1.0f) else 1.0f
        }

        val baseDisplayScale = when (aspectRatioMode) {
            AspectRatioMode.FIT -> 1.0f
            AspectRatioMode.FILL -> fillScale
            AspectRatioMode.CROP -> fillScale * 1.25f
            AspectRatioMode.ORIGINAL -> 1.0f
        }

        val animatedDisplayScale by androidx.compose.animation.core.animateFloatAsState(
            targetValue = baseDisplayScale * videoZoomScale,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 220,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "VideoDisplayScale"
        )
        val resolveError = queuedResolveError
        // Liquid Glass: low-res copy of the video + its window bounds, so the player controls can blur the picture behind them
        val videoGlassSource = remember { com.example.ui.components.VideoGlassSource() }

        @androidx.compose.runtime.Composable
        fun RenderVideoPlayerOverlay() {
            if (!entryFakeLandscape) VideoPlayerOverlay(
                glassSource = videoGlassSource,
                glassCapture = @androidx.compose.runtime.DontMemoize { bmp, done ->
                    // no PixelCopy while the Back animation runs: it only costs frames there
                    if (isExitingPlayer) done(false) else controller.captureGlassSample(bmp, done)
                },
                videoTitle = videoTitle,
                currentEpisodeIndex = currentEpisodeIndex,
                totalEpisodes = totalEpisodes,
                chapterTitle = currentChapterTitle,
                isPlaying = isPlaying,
                onPlayPauseToggle = @androidx.compose.runtime.DontMemoize {
                    // while an online video is buffering it keeps loading: a tap must not pause it (that stopped the loading)
                    if (queuedResolving) return@VideoPlayerOverlay
                    if (isPlaying && isBuffering && controller.onlineStreamMode) return@VideoPlayerOverlay
                    isPlaying = !isPlaying
                    if (isPlaying) {
                        controller.play()
                    } else {
                        controller.pause()
                        persistPlaybackPosition()
                    }
                },
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onSeekTo = @androidx.compose.runtime.DontMemoize { targetMs ->
                    currentPositionMs = targetMs
                    controller.seekTo(targetMs)
                    persistPlaybackPosition(targetMs)
                },
                onPreviousVideo = @androidx.compose.runtime.DontMemoize {
                    if (currentVideoIndex > 0) {
                        playEpisodeAtIndex(currentVideoIndex - 1)
                    } else if (currentPlaylist.size > 1 && repeatMode == PlayerRepeatMode.ALL) {
                        playEpisodeAtIndex(currentPlaylist.size - 1)
                    }
                },
                onNextVideo = @androidx.compose.runtime.DontMemoize {
                    if (isShuffle && currentPlaylist.size > 1) {
                        val availableIndices = currentPlaylist.indices.filter { it != currentVideoIndex }
                        val randomIdx = availableIndices.randomOrNull() ?: ((currentVideoIndex + 1) % currentPlaylist.size)
                        playEpisodeAtIndex(randomIdx)
                    } else if (currentVideoIndex + 1 < currentPlaylist.size) {
                        playEpisodeAtIndex(currentVideoIndex + 1)
                    } else if (currentPlaylist.size > 1 && repeatMode == PlayerRepeatMode.ALL) {
                        playEpisodeAtIndex(0)
                    }
                },
                hidePlayPauseButton = queuedResolving || (isBuffering && isPlaying),
                hideTrackButtons = queuedResolving || isBuffering ||
                    (controller.onlineStreamMode && !controllerIsLive && controllerPos <= 100L && durationMs <= 0L),
                bufferingMinimalUi = controller.onlineStreamMode && (queuedResolving || isBuffering ||
                    (!controllerIsLive && controllerPos <= 100L && durationMs <= 0L)),
                hasPrevious = uiState.playerSettings.enableNextPrevious && (currentVideoIndex > 0 || (currentPlaylist.size > 1 && repeatMode == PlayerRepeatMode.ALL)),
                hasNext = uiState.playerSettings.enableNextPrevious && ((currentVideoIndex + 1 < currentPlaylist.size) || (isShuffle && currentPlaylist.size > 1) || (currentPlaylist.size > 1 && repeatMode == PlayerRepeatMode.ALL)),
                onBack = @androidx.compose.runtime.DontMemoize {
                    exitPlayerNow(toMini = true)
                },
                playbackSpeed = playbackSpeed,
                onSpeedChange = @androidx.compose.runtime.DontMemoize { spd ->
                    playbackSpeed = spd
                    controller.setPlaybackSpeed(spd)
                    showNotification("Speed: ${spd}x")
                },
                decoderMode = decoderMode,
                onDecoderModeChange = @androidx.compose.runtime.DontMemoize { dec ->
                    decoderMode = dec
                    smartPrefs.edit().putString("decoder_mode", dec.name).apply()
                    showNotification(if (dec == DecoderMode.AUTO) "Decoder: Smart Auto" else "Decoder: ${dec.label}")
                },
                repeatMode = repeatMode,
                onRepeatModeChange = @androidx.compose.runtime.DontMemoize { rpt ->
                    repeatMode = rpt
                    val msg = when (rpt) {
                        PlayerRepeatMode.OFF -> "Repeat: Off"
                        PlayerRepeatMode.ONE -> "Repeat: Single Episode"
                        PlayerRepeatMode.ALL -> "Repeat: Entire Playlist"
                    }
                    showNotification(msg)
                },
                isShuffle = isShuffle,
                onShuffleChange = @androidx.compose.runtime.DontMemoize { shf ->
                    isShuffle = shf
                    showNotification(if (shf) "Shuffle: ON" else "Shuffle: OFF")
                },
                isAudioOnly = isAudioOnly,
                onAudioOnlyChange = @androidx.compose.runtime.DontMemoize { ao ->
                    isAudioOnly = ao
                    showNotification(if (ao) "Audio-only playback active" else "Video playback active")
                },
                aspectRatioMode = aspectRatioMode,
                onAspectRatioChange = @androidx.compose.runtime.DontMemoize { asp ->
                    aspectRatioMode = asp
                    showNotification("Display: ${asp.label}")
                },
                videoZoomScale = videoZoomScale,
                onVideoZoomChange = @androidx.compose.runtime.DontMemoize { z -> videoZoomScale = z },
                isVideoFlippedVertically = isVideoFlippedVertically,
                isVideoMirrored = isVideoMirrored,
                onToggleVideoFlipVertical = @androidx.compose.runtime.DontMemoize {
                    isVideoFlippedVertically = !isVideoFlippedVertically
                    showNotification(if (isVideoFlippedVertically) "Flip Vertical: ON" else "Flip Vertical: OFF")
                },
                onMirrorVideoRight = @androidx.compose.runtime.DontMemoize {
                    isVideoMirrored = true
                    showNotification("Mirror: RIGHT")
                },
                onMirrorVideoLeft = @androidx.compose.runtime.DontMemoize {
                    isVideoMirrored = false
                    showNotification("Mirror: LEFT")
                },
                isPanAndZoomEnabled = isPanAndZoomEnabled,
                onTogglePanAndZoom = @androidx.compose.runtime.DontMemoize {
                    isPanAndZoomEnabled = it
                    showNotification(if (it) "Pan & Zoom: Enabled" else "Pan & Zoom: Disabled")
                },
                onResetZoomAndPan = @androidx.compose.runtime.DontMemoize {
                    videoZoomScale = 1.0f
                    videoPanOffset = Offset.Zero
                    isPanAndZoomEnabled = false
                    showNotification("Zoom Reset: 100%")
                },
                isPitchCorrectionEnabled = isPitchCorrectionEnabled,
                onTogglePitchCorrection = @androidx.compose.runtime.DontMemoize { enabled ->
                    isPitchCorrectionEnabled = enabled
                    controller.setAudioPitchCorrection(enabled)
                    showNotification(if (enabled) "Pitch Correction: ON (Natural Voice Pitch)" else "Pitch Correction: OFF (Dynamic Pitch)")
                },
                isAmbienceEnabled = isAmbienceEnabled,
                onToggleAmbience = @androidx.compose.runtime.DontMemoize { enabled ->
                    isAmbienceEnabled = enabled
                    ambiencePrefs.edit().putBoolean("ambience_mode", enabled).apply()
                    showNotification(if (enabled) "Ambience Mode: ON" else "Ambience Mode: OFF")
                },
                loopPointA = loopPointA,
                loopPointB = loopPointB,
                onSetLoopPointA = @androidx.compose.runtime.DontMemoize { pt ->
                    loopPointA = pt
                    showNotification("Loop Point A set: ${formatPlaybackTime(pt)}")
                },
                onSetLoopPointB = @androidx.compose.runtime.DontMemoize { pt ->
                    loopPointB = pt
                    showNotification("Loop Point B set: ${formatPlaybackTime(pt)}")
                },
                onClearLoop = @androidx.compose.runtime.DontMemoize {
                    loopPointA = null
                    loopPointB = null
                    showNotification("A-B Loop Cleared")
                },
                brightness = brightness,
                onBrightnessChange = @androidx.compose.runtime.DontMemoize { brightness = it },
                volumeBoostCap = uiState.volumeBoostCap,
                onVolumeBoostChange = @androidx.compose.runtime.DontMemoize { boost -> controller.setVolume(boost) },
                contrast = contrast,
                onContrastChange = @androidx.compose.runtime.DontMemoize { contrast = it },
                saturation = saturation,
                onSaturationChange = @androidx.compose.runtime.DontMemoize { saturation = it },
                videoFilterPreset = videoFilterPreset,
                onSelectVideoFilterPreset = @androidx.compose.runtime.DontMemoize { preset ->
                    videoFilterPreset = preset
                    coroutineScope.launch {
                        val current = PlayerSettings.load(context)
                        PlayerSettings.save(context, current.copy(videoFilterPreset = preset.id))
                    }
                    showNotification("Filter: ${preset.displayName}")
                },
                manualVideoAdjustments = manualVideoAdjustments,
                onManualVideoAdjustmentsChange = @androidx.compose.runtime.DontMemoize { manual ->
                    manualVideoAdjustments = manual
                    coroutineScope.launch {
                        val current = PlayerSettings.load(context)
                        PlayerSettings.save(
                            context,
                            current.copy(
                                videoBrightness = manual.brightness,
                                videoContrast = manual.contrast,
                                videoSaturation = manual.saturation,
                                videoGamma = manual.gamma,
                                videoSharpness = manual.sharpness,
                                videoHue = manual.hue,
                                videoTemperature = manual.temperature,
                                videoTint = manual.tint,
                                videoDeband = manual.deband
                            )
                        )
                    }
                },
                selectedAudioTrackId = selectedAudioTrackId,
                onSelectAudioTrack = @androidx.compose.runtime.DontMemoize { id ->
                    hasManualAudioTrackSelection = true
                    selectedAudioTrackId = id
                    controller.setAudioTrack(id)
                    saveAudioSelection(id)
                    val trackName = realAudioTracks.firstOrNull { it.id == id }?.title ?: "Track $id"
                    showNotification("Audio Track: $trackName")
                },
                trackAudioConfigs = trackAudioConfigs,
                onTrackChannelModeChange = @androidx.compose.runtime.DontMemoize { trackId, mode ->
                    val old = trackAudioConfigs[trackId] ?: TrackAudioConfig()
                    val updated = old.copy(channelMode = mode)
                    val newMap = trackAudioConfigs + (trackId to updated)
                    trackAudioConfigs = newMap
                    if (trackId == selectedAudioTrackId) {
                        controller.setAudioChannelMode(mode)
                    }
                    coroutineScope.launch {
                        val current = PlayerSettings.load(context)
                        PlayerSettings.save(
                            context,
                            current.copy(trackAudioConfigsJson = PlayerSettings.encodeTrackAudioConfigs(newMap))
                        )
                    }
                    showNotification("Audio Channel: ${mode.getDisplayName()}")
                },
                onTrackVolumeNormalizationChange = @androidx.compose.runtime.DontMemoize { trackId, enabled ->
                    val old = trackAudioConfigs[trackId] ?: TrackAudioConfig()
                    val updated = old.copy(volumeNormalization = enabled)
                    val newMap = trackAudioConfigs + (trackId to updated)
                    trackAudioConfigs = newMap
                    if (trackId == selectedAudioTrackId) {
                        controller.setVolumeNormalization(enabled)
                    }
                    coroutineScope.launch {
                        val current = PlayerSettings.load(context)
                        PlayerSettings.save(
                            context,
                            current.copy(trackAudioConfigsJson = PlayerSettings.encodeTrackAudioConfigs(newMap))
                        )
                    }
                    showNotification("Volume Normalization: ${if (enabled) "On" else "Off"}")
                },
                onTrackDynamicRangeCompressionChange = @androidx.compose.runtime.DontMemoize { trackId, enabled ->
                    val old = trackAudioConfigs[trackId] ?: TrackAudioConfig()
                    val updated = old.copy(dynamicRangeCompression = enabled)
                    val newMap = trackAudioConfigs + (trackId to updated)
                    trackAudioConfigs = newMap
                    if (trackId == selectedAudioTrackId) {
                        controller.setDynamicRangeCompression(enabled)
                    }
                    coroutineScope.launch {
                        val current = PlayerSettings.load(context)
                        PlayerSettings.save(
                            context,
                            current.copy(trackAudioConfigsJson = PlayerSettings.encodeTrackAudioConfigs(newMap))
                        )
                    }
                    showNotification("Dynamic Range Compression: ${if (enabled) "On" else "Off"}")
                },
                onRemoveSubtitleTrack = @androidx.compose.runtime.DontMemoize { trackId ->
                    val trackToRemove = realSubtitleTracks.firstOrNull { it.id == trackId }
                    controller.removeSubtitleTrack(trackId)
                    if (trackToRemove != null) {
                        CustomSubtitlePersistenceManager.removeAttachedSubtitle(
                            context = context,
                            path = currentVideo?.path,
                            videoId = currentVideo?.id ?: 0L,
                            uriString = currentVideo?.uri?.toString(),
                            title = currentVideo?.displayName,
                            trackId = trackId,
                            originalName = trackToRemove.originalFilename.ifBlank { trackToRemove.title }
                        )
                    }
                    // Forget the removed track in the remembered selection (the other side stays).
                    // Persist the selection independent of the on/off toggle so a temporarily
                    // hidden primary track isn't silently dropped from the saved selection.
                    saveSubtitleSelection(
                        primaryId = if (selectedSubtitleTrackId != trackId) selectedSubtitleTrackId else 0,
                        secondaryId = if (controller.secondarySubtitleTrackId.value != trackId) controller.secondarySubtitleTrackId.value else 0
                    )
                    showNotification("Removed Subtitle Track")
                },
                onRemoveAudioTrack = @androidx.compose.runtime.DontMemoize { trackId ->
                    controller.removeAudioTrack(trackId)
                    showNotification("Removed Audio Track")
                },
                selectedSubtitleTrackId = selectedSubtitleTrackId,
                secondarySubtitleTrackId = realSecondarySubtitleTrackId,
                lastTouchedSubtitleTrackId = lastTouchedSubtitleTrackId,
                onLastTouchedSubtitleTrackIdChange = @androidx.compose.runtime.DontMemoize { lastTouchedSubtitleTrackId = it },
                onSelectSubtitleTrack = @androidx.compose.runtime.DontMemoize { id ->
                    selectSubtitleTracks(id, controller.secondarySubtitleTrackId.value)
                },
                onSelectSecondarySubtitleTrack = @androidx.compose.runtime.DontMemoize { id ->
                    selectSubtitleTracks(selectedSubtitleTrackId, id)
                },
                onSelectSubtitleTracksCascade = @androidx.compose.runtime.DontMemoize { primId, secId ->
                    selectSubtitleTracks(primId, secId)
                },
                isSubtitleVisible = isSubtitleVisible,
                onToggleSubtitleVisibility = @androidx.compose.runtime.DontMemoize {
                    val nextVis = !isSubtitleVisible
                    isSubtitleVisible = nextVis
                    controller.setSubtitleVisibility(nextVis)
                    SubtitleSessionMemory.setSubtitlesEnabled(nextVis)
                    if (!nextVis) {
                        controller.selectSubtitleTracksAtomic(0, 0)
                        selectedSubtitleTrackId = 0
                        controller.preferredSubtitleTrackNumber = 0
                        saveSubtitleSelection(primaryId = 0, secondaryId = 0)
                        showNotification("Subtitles Off")
                    } else {
                        val activeTrack = (if (lastTouchedSubtitleTrackId > 0) lastTouchedSubtitleTrackId else null)
                            ?: realSubtitleTracks.firstOrNull { it.isSelected }?.id
                            ?: realSubtitleTracks.firstOrNull()?.id
                            ?: 0
                        if (activeTrack > 0) {
                            selectSubtitleTracks(activeTrack, 0)
                        } else {
                            showNotification("No subtitle selected")
                        }
                    }
                },
                subtitleOffsetMs = subtitleOffsetMs,
                onSubtitleOffsetChange = @androidx.compose.runtime.DontMemoize { offset ->
                    subtitleOffsetMs = offset
                    controller.setSubtitleDelay(offset)
                    showNotification("Subtitle Sync: ${if (offset >= 0) "+$offset" else "$offset"} ms")
                },
                audioDelayMs = audioDelayMs,
                onAudioDelayChange = @androidx.compose.runtime.DontMemoize { delay ->
                    audioDelayMs = delay
                    controller.setAudioDelay(delay)
                },
                equalizerEnabled = equalizerEnabled,
                onToggleEqualizer = @androidx.compose.runtime.DontMemoize { enabled ->
                    equalizerEnabled = enabled
                    controller.setEqualizer(
                        enabled = enabled,
                        eq60Hz = eq60Hz,
                        eq230Hz = eq230Hz,
                        eq910Hz = eq910Hz,
                        eq3600Hz = eq3600Hz,
                        eq14000Hz = eq14000Hz,
                        volumeBoostDb = volumeBoostDb,
                        immediate = true
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizerEnabled = enabled) }
                },
                eq60Hz = eq60Hz,
                onEq60HzChange = @androidx.compose.runtime.DontMemoize { v ->
                    eq60Hz = v
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = v,
                        eq230Hz = eq230Hz,
                        eq910Hz = eq910Hz,
                        eq3600Hz = eq3600Hz,
                        eq14000Hz = eq14000Hz,
                        volumeBoostDb = volumeBoostDb,
                        immediate = false
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizer60Hz = v) }
                },
                eq230Hz = eq230Hz,
                onEq230HzChange = @androidx.compose.runtime.DontMemoize { v ->
                    eq230Hz = v
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = eq60Hz,
                        eq230Hz = v,
                        eq910Hz = eq910Hz,
                        eq3600Hz = eq3600Hz,
                        eq14000Hz = eq14000Hz,
                        volumeBoostDb = volumeBoostDb,
                        immediate = false
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizer230Hz = v) }
                },
                eq910Hz = eq910Hz,
                onEq910HzChange = @androidx.compose.runtime.DontMemoize { v ->
                    eq910Hz = v
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = eq60Hz,
                        eq230Hz = eq230Hz,
                        eq910Hz = v,
                        eq3600Hz = eq3600Hz,
                        eq14000Hz = eq14000Hz,
                        volumeBoostDb = volumeBoostDb,
                        immediate = false
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizer910Hz = v) }
                },
                eq3600Hz = eq3600Hz,
                onEq3600HzChange = @androidx.compose.runtime.DontMemoize { v ->
                    eq3600Hz = v
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = eq60Hz,
                        eq230Hz = eq230Hz,
                        eq910Hz = eq910Hz,
                        eq3600Hz = v,
                        eq14000Hz = eq14000Hz,
                        volumeBoostDb = volumeBoostDb,
                        immediate = false
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizer3600Hz = v) }
                },
                eq14000Hz = eq14000Hz,
                onEq14000HzChange = @androidx.compose.runtime.DontMemoize { v ->
                    eq14000Hz = v
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = eq60Hz,
                        eq230Hz = eq230Hz,
                        eq910Hz = eq910Hz,
                        eq3600Hz = eq3600Hz,
                        eq14000Hz = v,
                        volumeBoostDb = volumeBoostDb,
                        immediate = false
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizer14000Hz = v) }
                },
                volumeBoostDb = volumeBoostDb,
                onVolumeBoostDbChange = @androidx.compose.runtime.DontMemoize { v ->
                    volumeBoostDb = v
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = eq60Hz,
                        eq230Hz = eq230Hz,
                        eq910Hz = eq910Hz,
                        eq3600Hz = eq3600Hz,
                        eq14000Hz = eq14000Hz,
                        volumeBoostDb = v,
                        immediate = false
                    )
                    saveEqualizerSettingsDebounced { it.copy(equalizerVolumeBoostDb = v) }
                },
                equalizerPreset = equalizerPreset,
                onSelectEqualizerPreset = @androidx.compose.runtime.DontMemoize { preset ->
                    val pEq60 = preset.gains[0]
                    val pEq230 = preset.gains[1]
                    val pEq910 = preset.gains[2]
                    val pEq3600 = preset.gains[3]
                    val pEq14000 = preset.gains[4]
                    equalizerPreset = preset.id
                    eq60Hz = pEq60
                    eq230Hz = pEq230
                    eq910Hz = pEq910
                    eq3600Hz = pEq3600
                    eq14000Hz = pEq14000
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = pEq60,
                        eq230Hz = pEq230,
                        eq910Hz = pEq910,
                        eq3600Hz = pEq3600,
                        eq14000Hz = pEq14000,
                        volumeBoostDb = volumeBoostDb,
                        immediate = true
                    )
                    saveEqualizerSettingsDebounced {
                        it.copy(
                            equalizerPreset = preset.id,
                            equalizer60Hz = pEq60,
                            equalizer230Hz = pEq230,
                            equalizer910Hz = pEq910,
                            equalizer3600Hz = pEq3600,
                            equalizer14000Hz = pEq14000
                        )
                    }
                },
                onResetEqualizer = @androidx.compose.runtime.DontMemoize {
                    val flat = EqualizerPreset.FLAT
                    val fEq60 = flat.gains[0]
                    val fEq230 = flat.gains[1]
                    val fEq910 = flat.gains[2]
                    val fEq3600 = flat.gains[3]
                    val fEq14000 = flat.gains[4]
                    equalizerPreset = flat.id
                    eq60Hz = fEq60
                    eq230Hz = fEq230
                    eq910Hz = fEq910
                    eq3600Hz = fEq3600
                    eq14000Hz = fEq14000
                    volumeBoostDb = 0f
                    controller.setEqualizer(
                        enabled = equalizerEnabled,
                        eq60Hz = fEq60,
                        eq230Hz = fEq230,
                        eq910Hz = fEq910,
                        eq3600Hz = fEq3600,
                        eq14000Hz = fEq14000,
                        volumeBoostDb = 0f,
                        immediate = true
                    )
                    saveEqualizerSettingsDebounced {
                        it.copy(
                            equalizerPreset = flat.id,
                            equalizer60Hz = fEq60,
                            equalizer230Hz = fEq230,
                            equalizer910Hz = fEq910,
                            equalizer3600Hz = fEq3600,
                            equalizer14000Hz = fEq14000,
                            equalizerVolumeBoostDb = 0f
                        )
                    }
                },
                chapters = realChapters,
                bufferedPositionMs = bufferedPositionMs,
                isLiveStream = controllerIsLive,
                liveSeekStartMs = controllerLiveRange?.startMs ?: 0L,
                liveSeekEndMs = controllerLiveRange?.endMs ?: 0L,
                pluginStreamPanel = if (pluginSession == null) null else com.example.ui.components.PluginStreamPanelData(
                    servers = pluginSession.servers.mapIndexed { i, st ->
                        val kind = when {
                            st.type == "m3u8" || st.type == "hls" || st.link.substringBefore('?').lowercase().endsWith(".m3u8") -> "HLS"
                            st.type == "mpd" || st.type == "dash" -> "DASH"
                            st.type.isNotBlank() -> st.type.uppercase()
                            else -> ""
                        }
                        com.example.ui.components.PlayerServerOption(
                            key = i.toString(),
                            title = st.server.ifBlank { "Server ${i + 1}" },
                            info = listOf(kind, st.quality.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
                        )
                    },
                    selectedServerKey = pluginServerIndex.toString(),
                    qualities = if (hlsVariants.isNotEmpty()) {
                        listOf(com.example.ui.components.PlayerQualityOption("auto", "Auto", "Adaptive bitrate", null)) +
                            hlsVariants.map { com.example.ui.components.PlayerQualityOption(hlsKey(it), it.label, it.info, it.badge) }
                    } else if (sameServerQualities().isNotEmpty()) {
                        sameServerQualities().map { (i, st) ->
                            com.example.ui.components.PlayerQualityOption("srv:$i", st.quality.orEmpty(), st.type.uppercase(), null)
                        }
                    } else if (!pluginSession.plugin.isCloudStream) {
                        pluginSession.choices.map { com.example.ui.components.PlayerQualityOption(it.quality, it.quality, "", null) }
                    } else emptyList(),
                    selectedQualityKey = if (hlsVariants.isNotEmpty()) hlsSelectedKey
                        else if (sameServerQualities().isNotEmpty()) "srv:$pluginServerIndex"
                        else pluginQuality
                ),
                onSelectStreamServer = @androidx.compose.runtime.DontMemoize { key -> key.toIntOrNull()?.let { switchPluginServer(it) } },
                availableVideoQualities = if (pluginSession != null) {
                    if (hlsVariants.isEmpty() && sameServerQualities().isNotEmpty()) sameServerQualities().map { it.second.quality.orEmpty() }
                    else if (pluginSession.plugin.isCloudStream) emptyList()
                    else pluginSession.choices.map { it.quality }
                } else {
                    onlinePlayback?.qualities?.map { it.label }?.let { listOf(OnlineVideoService.AUTO_QUALITY) + it } ?: emptyList()
                },
                selectedVideoQuality = if (pluginSession != null) {
                        if (hlsVariants.isNotEmpty()) {
                            val cur = hlsVariants.firstOrNull { it.bandwidth == hlsCurrentBandwidth }?.label
                            if (hlsSelectedKey == "auto") "Auto (${cur ?: "…"})" else cur ?: hlsVariants.firstOrNull { hlsKey(it) == hlsSelectedKey }?.label
                        } else if (sameServerQualities().isNotEmpty()) pluginSession.servers.getOrNull(pluginServerIndex)?.quality
                        else pluginQuality
                    }
                    else if (onlineAutoMode) "${OnlineVideoService.AUTO_QUALITY} (${selectedOnlineQuality ?: "…"})" else selectedOnlineQuality,
                onSelectVideoQuality = @androidx.compose.runtime.DontMemoize { label ->
                    if (pluginSession != null && label.startsWith("srv:")) label.removePrefix("srv:").toIntOrNull()?.let { switchPluginServer(it) }
                    else if (pluginSession != null && hlsVariants.isEmpty() &&
                        sameServerQualities().any { it.second.quality == label }) {
                        sameServerQualities().firstOrNull { it.second.quality == label }?.let { switchPluginServer(it.first) }
                    }
                    else if (pluginSession != null && (label == "auto" || label.startsWith("hls:"))) switchHlsQuality(label)
                    else if (pluginSession != null) switchPluginQuality(label)
                    else switchOnlineQuality(label)
                },
                showBufferedRange = uiState.playerSettings.showBufferedRange || currentVideo?.isOnline == true,
                thumbFastPreview = uiState.playerSettings.thumbFastPreview,
                previewVideoUri = currentVideo?.uri,
                previewVideoPath = currentVideo?.path,
                onSeekToChapter = @androidx.compose.runtime.DontMemoize { chap ->
                    currentPositionMs = chap.timeMs
                    controller.seekToChapter(chap)
                    persistPlaybackPosition(chap.timeMs)
                    showNotification("Chapter: ${chap.title}")
                },
                skipMarkers = skipMarkers,
                onSkipToPosition = @androidx.compose.runtime.DontMemoize { targetMs ->
                    val isVideoFullyFinished = durationMs > 0 && targetMs >= (durationMs - 600L)

                    if (isVideoFullyFinished) {
                        if (currentVideoIndex + 1 < currentPlaylist.size) {
                            showNotification("Skipped to next episode")
                            playEpisodeAtIndex(currentVideoIndex + 1)
                        } else if (uiState.playerSettings.repeatPlaylistAfterLast && currentPlaylist.size > 1) {
                            showNotification("Restarting playlist")
                            playEpisodeAtIndex(0)
                        } else {
                            persistPlaybackPosition(durationMs)
                            exitPlayerNow()
                        }
                    } else {
                        currentPositionMs = targetMs
                        controller.seekTo(targetMs)
                        persistPlaybackPosition(targetMs)
                    }
                },
                audioTracks = realAudioTracks,
                subtitleTracks = realSubtitleTracks,
                playlistVideos = playlistNames,
                playlistItems = currentPlaylist,
                onSelectPlaylistItem = @androidx.compose.runtime.DontMemoize { idx ->
                    playEpisodeAtIndex(idx - 1)
                },
                onReorderPlaylist = @androidx.compose.runtime.DontMemoize { from, to ->
                    val list = currentPlaylist.toMutableList()
                    if (from in list.indices && to in list.indices) {
                        // the episode that plays now, found by the object itself (ids are not guaranteed unique)
                        val playingItem = currentPlaylist.getOrNull(currentVideoIndex)
                        val moved = list.removeAt(from)
                        list.add(to, moved)
                        currentPlaylist = list
                        val newIdx = if (playingItem != null) {
                            list.indexOfFirst { it === playingItem }.takeIf { it >= 0 }
                                ?: list.indexOfFirst { it.path == playingItem.path && it.id == playingItem.id }
                        } else -1
                        if (newIdx >= 0) {
                            if (playingItem != null) reorderKeepKey = "${playingItem.id}|${playingItem.path}"
                            currentVideoIndex = newIdx
                        }
                    }
                },
                onImportSubtitle = @androidx.compose.runtime.DontMemoize {
                    showSubtitleFileTree = true
                },
                onPasteSubtitle = @androidx.compose.runtime.DontMemoize {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    val clipData = clipboard?.primaryClip
                    val pastedText = if (clipData != null && clipData.itemCount > 0) {
                        clipData.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
                    } else null

                    if (pastedText.isNullOrBlank()) {
                        showNotification("Clipboard is empty or contains no subtitle text")
                    } else {
                        val previousPrimaryId = realSubtitleTracks.firstOrNull { it.isSelected }?.id ?: selectedSubtitleTrackId
                        coroutineScope.launch {
                            try {
                                val detectedFormat = SubtitleEngine.detectFormat(pastedText)
                                val extension = detectedFormat.extension
                                // Every paste gets its own name (PASTED-SUB, PASTED-SUB-2, ...): a fixed name made
                                // the next paste replace the previous one, so two pasted subtitles could never
                                // exist together in the Uploaded list.
                                val usedPasteNames = controller.subtitleTracks.value
                                    .filter { it.isExternal }
                                    .map { (it.originalFilename.ifBlank { it.title }).substringBeforeLast('.').lowercase() }
                                    .toSet()
                                var pasteIndex = 1
                                var trackName = "PASTED-SUB"
                                while (trackName.lowercase() in usedPasteNames) {
                                    pasteIndex++
                                    trackName = "PASTED-SUB-$pasteIndex"
                                }
                                val originalName = "$trackName.$extension"
                                val targetId = withContext(Dispatchers.IO) {
                                    val persistentFile = CustomSubtitlePersistenceManager.persistSubtitleFromText(
                                        context = context,
                                        content = pastedText,
                                        trackName = trackName,
                                        extension = extension
                                    )
                                    val attachedPath = attachUploadedSubtitle(
                                        filePath = persistentFile.absolutePath,
                                        originalName = originalName
                                    ) ?: persistentFile.absolutePath
                                    val addedSubId = controller.addExternalSubtitle(
                                        filePath = attachedPath,
                                        context = context,
                                        originalName = originalName,
                                        select = true
                                    )
                                    addedSubId?.takeIf { it > 0 } ?: controller.subtitleTracks.value.lastOrNull()?.id ?: 0
                                }
                                if (targetId > 0) {
                                    promoteNewSubtitleToPrimary(targetId, previousPrimaryId)
                                }
                                showNotification("Added & Applied: $originalName")
                            } catch (e: Exception) {
                                e.printStackTrace()
                                showNotification("Error pasting subtitle: ${e.message}")
                            }
                        }
                    }
                },
                onImportAudio = @androidx.compose.runtime.DontMemoize {
                    showAudioFileTree = true
                },
                translationStatusByTrackId = translationStatusByTrackId,
                onStartTranslate = onStartTranslate,
                onTranslateSubtitleTrack = @androidx.compose.runtime.DontMemoize { trackId, progress ->
                    val settings = AiFeaturesSettingsStore.load(context)
                    if (!settings.hasUsableModel) {
                        Result.failure(IllegalStateException("Configure a usable API model or installed offline model first"))
                    } else {
                        controller.translateSubtitleTrack(
                            context = context,
                            trackId = trackId,
                            settings = settings,
                            targetLanguageLabel = TranslateLanguages.ALL.firstOrNull { it.code == settings.translateLanguageCode }?.label ?: settings.translateLanguageCode,
                            onProgress = { p -> progress(if (p.total == 0) 0 else (p.completed * 100) / p.total) }
                        )
                    }
                },
                onSelectSubtitleFile = @androidx.compose.runtime.DontMemoize { selectedFile ->
                    coroutineScope.launch {
                        try {
                            if (importSubtitleFileSafely(selectedFile)) {
                                showNotification("Loaded Subtitle: ${selectedFile.name}")
                            } else {
                                showNotification("Failed to load subtitle file")
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            showNotification("Error: ${t.message}")
                        }
                    }
                },
                onTakeScreenshot = @androidx.compose.runtime.DontMemoize {
                    coroutineScope.launch {
                        val vid = currentVideo
                        val pos = currentPositionMs
                        showNotification("Capturing clean video frame...")
                        val result = if (uiState.playerSettings.subtitlesInScreenshots) {
                            kotlinx.coroutines.suspendCancellableCoroutine<Result<String>> { cont ->
                                controller.captureCurrentFrame { bitmap ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val save = if (bitmap != null) {
                                            com.example.util.VideoScreenshotSaver.saveBitmapFrame(
                                                context = context,
                                                frameBitmap = bitmap,
                                                settings = uiState.playerSettings,
                                                title = vid?.displayName ?: "Screenshot",
                                                positionMs = pos
                                            )
                                        } else Result.failure(Exception("Unable to capture the rendered video surface"))
                                        if (cont.isActive) cont.resume(save) {}
                                    }
                                }
                            }
                        } else {
                            com.example.util.VideoScreenshotSaver.captureAndSaveCleanVideoFrame(
                                context = context, uri = vid?.uri, path = vid?.path, positionMs = pos, settings = uiState.playerSettings
                            )
                        }
                        result.onSuccess { msg ->
                            showNotification(msg)
                        }.onFailure { err ->
                            showNotification("Screenshot failed: ${err.localizedMessage ?: "Unknown error"}")
                        }
                    }
                },
                onTogglePiP = @androidx.compose.runtime.DontMemoize {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                        try {
                            val params = if (activity is MainActivity) {
                                activity.buildPipParams(isPlaying, uiState.playerSettings.autoPictureInPicture)
                            } else {
                                PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                            }
                            if (params != null) {
                                activity.enterPictureInPictureMode(params)
                            }
                        } catch (e: Exception) {
                            showNotification("PiP not supported on this device")
                        }
                    } else {
                        showNotification("Picture-in-Picture requires Android 8.0+")
                    }
                },
                onToggleOrientation = @androidx.compose.runtime.DontMemoize {
                    val current = activity?.requestedOrientation
                    if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || isLandscape) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                },
                channelCardMode = channelMode,
                isLandscape = isLandscape || shouldRotateForExit,
                glassBlurTransparency = uiState.glassBlurTransparency,
                thumbnailStrategy = uiState.thumbnailStrategy,
                thumbnailQuality = uiState.thumbnailQuality,
                thumbnailFallbackSecond = uiState.thumbnailFallbackSecond,
                controlsAnimationStyle = if (uiState.playerSettings.reduceAnimation) ControlsAnimationStyle.MINIMAL_FADE else uiState.controlsAnimationStyle,
                animationSpeed = uiState.animationSpeed,
                showPlayerNotifications = uiState.showPlayerNotifications,
                enableDoubleTap = uiState.playerSettings.enableDoubleTap,
                doubleTapSeekSeconds = (if (uiState.playerSettings.customSkipDuration != 90) uiState.playerSettings.customSkipDuration else uiState.playerSettings.doubleTapSeekDuration).coerceIn(1, 600),
                doubleTapSeekAreaWidthPercent = uiState.playerSettings.doubleTapSeekAreaWidth,
                doubleTapLeftAction = uiState.playerSettings.doubleTapLeftAction,
                doubleTapCenterAction = uiState.playerSettings.doubleTapCenterAction,
                doubleTapRightAction = uiState.playerSettings.doubleTapRightAction,
                enableSingleTap = uiState.playerSettings.enableSingleTap,
                singleTapSeekSeconds = uiState.playerSettings.singleTapSeekDuration,
                singleTapSeekAreaWidthPercent = uiState.playerSettings.singleTapSeekAreaWidth,
                singleTapLeftAction = uiState.playerSettings.singleTapLeftAction,
                singleTapCenterAction = uiState.playerSettings.singleTapCenterAction,
                singleTapRightAction = uiState.playerSettings.singleTapRightAction,
                singleTapCenterGesture = uiState.playerSettings.singleTapCenterGesture,
                brightnessGesturesEnabled = uiState.playerSettings.brightnessGestures,
                volumeGesturesEnabled = uiState.playerSettings.volumeGestures,
                swapVolumeBrightnessGestures = uiState.playerSettings.swapVolumeBrightness,
                gestureHudOppositeSide = uiState.playerSettings.gestureHudOppositeSide,
                pinchToZoomEnabled = uiState.playerSettings.pinchToZoom,
                pinchToZoomSubtitlesEnabled = uiState.playerSettings.pinchToZoomSubtitles,
                swipeSubtitlesToSeekDialogEnabled = uiState.playerSettings.swipeSubtitlesToSeekDialog,
                horizontalSwipeToSeekEnabled = uiState.playerSettings.horizontalSwipeToSeek,
                swipeUpCenterForPlaylistEnabled = uiState.playerSettings.swipeUpCenterForPlaylist,
                horizontalSwipeSensitivity = uiState.playerSettings.horizontalSwipeSensitivity,
                holdMultiSpeedEnabled = uiState.playerSettings.holdMultiSpeed,
                holdSpeedMultiplier = uiState.playerSettings.holdSpeedMultiplier,
                dynamicSpeedOverlayEnabled = uiState.playerSettings.dynamicSpeedOverlay,
                holdDragMovesSubtitlesEnabled = uiState.playerSettings.holdDragMovesSubtitles,
                swipeOnSubtitleSeeksCuesEnabled = uiState.playerSettings.swipeOnSubtitleSeeksCues,
                onSubtitleSeek = @androidx.compose.runtime.DontMemoize { forward -> controller.seekToAdjacentSubtitle(forward) },
                mediaPreviousControlEnabled = uiState.playerSettings.mediaPreviousControl,
                mediaPlayPauseControlEnabled = uiState.playerSettings.mediaPlayPauseControl,
                mediaNextControlEnabled = uiState.playerSettings.mediaNextControl,
                onSubtitleScaleChange = @androidx.compose.runtime.DontMemoize { scale ->
                    subtitleScale = scale.coerceIn(0.5f, 3.0f)
                    val updated = liveSubtitleSettings.copy(subtitleScale = subtitleScale)
                    liveSubtitleSettings = updated
                    controller.setSubtitleScale(subtitleScale.toDouble())
                    applySubtitleAppearanceDirect(updated)
                    saveSubtitleSettingsDebounced(updated)
                    onPlayerSettingsChange(updated)
                },
                // While dragging only the cheap mpv property is updated (smooth, instant); the full style re-apply,
                // settings state and disk save happen once in onSubtitlePositionCommit when the finger lifts.
                onSubtitlePositionChange = @androidx.compose.runtime.DontMemoize { position ->
                    subtitlePosition = position.coerceIn(0f, 150f)
                    controller.setSubtitlePosition(subtitlePosition.toDouble())
                },
                onSubtitlePositionCommit = @androidx.compose.runtime.DontMemoize { position ->
                    subtitlePosition = position.coerceIn(0f, 150f)
                    val updated = liveSubtitleSettings.copy(subtitlePosition = subtitlePosition)
                    liveSubtitleSettings = updated
                    controller.setSubtitlePosition(subtitlePosition.toDouble())
                    applySubtitleAppearanceDirect(updated)
                    saveSubtitleSettingsDebounced(updated)
                    onPlayerSettingsChange(updated)
                },
                subtitleScale = subtitleScale,
                subtitlePosition = subtitlePosition,
                subtitleSettings = liveSubtitleSettings,
                onSubtitleSettingsChange = @androidx.compose.runtime.DontMemoize { updated ->
                    liveSubtitleSettings = updated
                    subtitleScale = updated.subtitleScale
                    subtitlePosition = updated.subtitlePosition
                    applySubtitleAppearanceDirect(updated)
                    saveSubtitleSettingsDebounced(updated)
                    onPlayerSettingsChange(updated)
                },
                onLoadAdvancedAssSource = @androidx.compose.runtime.DontMemoize { trackId -> controller.readAdvancedAssSource(context, trackId) },
                onLoadRawSubtitleSource = @androidx.compose.runtime.DontMemoize { trackId -> controller.readRawSubtitleSource(context, trackId) },
                onRawSubtitlePreview = @androidx.compose.runtime.DontMemoize { key, text -> controller.setRawSubtitlePreview(key, text) },
                showDoubleTapSeekFeedback = uiState.playerSettings.rippleOnDoubleTap && uiState.playerSettings.showSeekTime,
                showVolumeSliderOverlay = uiState.playerSettings.volumeSliderOverlay,
                showBrightnessSliderOverlay = uiState.playerSettings.brightnessSliderOverlay,
                showHoldSpeedOverlay = uiState.playerSettings.holdSpeedOverlay,
                showAspectRatioFeedback = uiState.playerSettings.aspectRatioFeedback,
                showZoomLevelFeedback = uiState.playerSettings.zoomLevelFeedback,
                showRepeatShuffleFeedback = uiState.playerSettings.repeatShuffleFeedback,
                showActionFeedbackPills = uiState.playerSettings.actionFeedbackPills,
                externalNotification = playerNotificationMessage,
                externalNotificationKey = playerNotificationKey,
                gestureSensitivityMode = uiState.gestureSensitivityMode,
                onGestureSensitivityModeChange = onGestureSensitivityModeChange,
                seekbarStyle = uiState.seekbarStyle,
                onSeekbarStyleChange = onSeekbarStyleChange,
                playerLayoutConfig = uiState.playerLayoutConfig,
                onPlayerLayoutConfigChange = onPlayerLayoutConfigChange,
                onResetPlayerLayoutConfig = onResetPlayerLayoutConfig,
                playerSettings = uiState.playerSettings,
                videoWidth = realVideoWidth,
                videoHeight = realVideoHeight
            )
        }

        @androidx.compose.runtime.Composable
        fun RenderAllServersFailed() {
            if (allServersFailed && pluginSession != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Playback failed on all available servers",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                allServersFailed = false
                                failedServerLinks.clear()
                                switchPluginServer(0, automatic = true)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2979FF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        @androidx.compose.runtime.Composable
        fun RenderResolveError() {
            if (resolveError != null && !queuedResolving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Could not get this video: $resolveError",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                queuedResolveError = null
                                playEpisodeAtIndex(currentVideoIndex)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2979FF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        @androidx.compose.runtime.Composable
        fun RenderScalableVideoSurface() {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedDisplayScale,
                            scaleY = animatedDisplayScale,
                            // NOTE: Flip / Mirror are intentionally NOT applied here. The mpv video is a native
                            // SurfaceView that ignores Compose rotationX/rotationY; the transform is applied
                            // inside mpv's filter chain via controller.setVideoTransform(...) instead.
                            translationX = videoPanOffset.x,
                            translationY = videoPanOffset.y
                        )
                        .then(
                            if (isPanAndZoomEnabled && (animatedDisplayScale > 1.0f || videoZoomScale > 1.0f)) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        videoPanOffset += dragAmount
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // VIDEO RENDERING SURFACE (NATIVE MPV)
                    // Hidden once `shouldRotateForExit` is true: at that point the Activity's real
                    // window orientation has already flipped to portrait and this whole box is being
                    // held "landscape-looking" purely via a Compose graphicsLayer rotationZ trick.
                    // A live SurfaceView (this AndroidView) is composited by the OS independently of
                    // its parent's Compose transforms, so it does NOT rotate/resize along with that
                    // trick — it keeps rendering at its old (now-mismatched) geometry for a few
                    // frames, which is what was letting the Home screen behind it show through as a
                    // brief flash during the landscape dismiss animation. The freeze-frame Image
                    // below (a plain Compose bitmap, not a native surface) rotates correctly with the
                    // container, so from this point on it alone represents the paused video while the
                    // dismiss animation finishes.
                    // Keep the live surface in place during the exit animation whenever the
                    // window itself is not yet forced through the landscape->portrait handoff.
                    // The captured frame is drawn above it, so switching from SurfaceView to a
                    // bitmap cannot produce the tiny down/up refresh that was visible at Back time.
                    // For a landscape exit, shouldRotateForExit becomes true only after the window
                    // is portrait, at which point the native surface is hidden and the frozen frame
                    // carries the rest of the animation.
                    if (!shouldRotateForExit &&
                        !entryFakeLandscape &&
                        resolvedPlayablePath.isNotBlank() &&
                        (File(resolvedPlayablePath).exists() || resolvedPlayablePath.startsWith("fd://") || resolvedPlayablePath.startsWith("http://") || resolvedPlayablePath.startsWith("https://"))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val parkedView = miniAdopted?.view
                                if (parkedView != null && controller.isAlive) {
                                    // Taken over from the mini player: same native view, same running mpv, no reload.
                                    (parkedView.parent as? android.view.ViewGroup)?.removeView(parkedView)
                                    miniViewHolder[0] = parkedView
                                    controller.resumePolling(coroutineScope)
                                    parkedView
                                } else {
                                    VideoPlayerMpvView(ctx).also { mpv ->
                                        mpv.initialize(ctx.filesDir.path, ctx.cacheDir.path)
                                        // playEpisodeAtIndex() queues the pending path before the native view is attached.
                                        // The surface callback owns the single initial MPV load.
                                        controller.attach(mpv, coroutineScope)
                                        miniViewHolder[0] = mpv
                                    }
                                }
                            },
                            update = { v ->
                                // No reload here. Only the channel-card mode rounds the native surface (it ignores Compose clipping).
                                if (channelMode) {
                                    v.outlineProvider = object : android.view.ViewOutlineProvider() {
                                        override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                                            outline.setRoundRect(0, 0, view.width, view.height, 14f * view.resources.displayMetrics.density)
                                        }
                                    }
                                    v.clipToOutline = true
                                } else if (v.clipToOutline) {
                                    v.clipToOutline = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .trackVideoGlassBounds(videoGlassSource)
                        )
                    }

                    // VIDEO THUMBNAIL / FIRST FRAME AS INITIAL POSTER (Smoothly fades out when video frame is ready to avoid black screen)
                    if ((controllerPos > 200L || currentPositionMs > 200L) && !isBuffering) {
                        hasRenderedInitialFrame = true
                    }
                    val thumbAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (hasRenderedInitialFrame) 0f else 1f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 280,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        ),
                        label = "ThumbnailTransitionAlpha"
                    )

                    if (thumbnailBitmap != null && thumbAlpha > 0.01f) {
                        Image(
                            bitmap = thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = "Video Initial Frame",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = thumbAlpha)
                        )
                    }

                    val activeFreezeFrame = exitFreezeFrameBitmap ?: controller.lastCapturedFrame.value
                    if (isExitingPlayer && activeFreezeFrame != null) {
                        Image(
                            bitmap = activeFreezeFrame.asImageBitmap(),
                            contentDescription = "Video Exit Freeze Frame",
                            contentScale = when (aspectRatioMode) {
                                AspectRatioMode.FILL, AspectRatioMode.CROP -> ContentScale.Crop
                                else -> ContentScale.Fit
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                }
            }
        }

        // -------------------------------------------------------------
        // AMBIENCE MODE: the picture's own edge colours continue into the black bars (live, every frame)
        // -------------------------------------------------------------
        @androidx.compose.runtime.Composable
        fun RenderAmbienceGlow() {
            val showGlow = isAmbienceEnabled &&
                aspectRatioMode == AspectRatioMode.FIT &&
                animatedDisplayScale <= 1.01f &&
                !isExitingPlayer &&
                realVideoWidth > 0 && realVideoHeight > 0
            val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (showGlow) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(250),
                label = "AmbienceGlowAlpha"
            )
            // The Liquid Glass sample of the controls must show the same glow as the bars (not the black bars of the raw video copy).
            AmbienceGlassSync(videoGlassSource, if (showGlow) realVideoWidth.toFloat() / realVideoHeight.toFloat() else 0f)
            // Latest picture-only frame. A new small bitmap per frame (about 40 KB) so the draw never shows a half-written one.
            var frame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
            var glowSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

            // Where the picture sits inside the screen (FIT) -> same maths as the video surface
            val cw = glowSize.width.toFloat()
            val ch = glowSize.height.toFloat()
            val dar = if (realVideoHeight > 0) realVideoWidth.toFloat() / realVideoHeight.toFloat() else 1f
            val fitW: Float
            val fitH: Float
            if (cw <= 0f || ch <= 0f) { fitW = 0f; fitH = 0f }
            else if (cw / ch > dar) { fitH = ch; fitW = ch * dar } else { fitW = cw; fitH = cw / dar }
            val picLeft = (cw - fitW) / 2f
            val picTop = (ch - fitH) / 2f

            // Frame grabber: copies ONLY the picture area (never the black bars), one frame right after the other while playing.
            LaunchedEffect(showGlow, glowSize, realVideoWidth, realVideoHeight) {
                if (!showGlow || fitW < 4f || fitH < 4f) return@LaunchedEffect
                val src = android.graphics.Rect(
                    picLeft.roundToInt() + 1,
                    picTop.roundToInt() + 1,
                    (picLeft + fitW).roundToInt() - 1,
                    (picTop + fitH).roundToInt() - 1
                )
                // 160 px on the long side keeps edge colours accurate for 720p / 1080p / 4K sources (PixelCopy scales on the GPU)
                val longSide = 160
                val bw: Int
                val bh: Int
                if (fitW >= fitH) { bw = longSide; bh = (longSide * fitH / fitW).roundToInt().coerceAtLeast(8) }
                else { bh = longSide; bw = (longSide * fitW / fitH).roundToInt().coerceAtLeast(8) }
                while (true) {
                    val bmp = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888)
                    val ok = kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
                        controller.captureVideoRegion(bmp, src) { r ->
                            if (cont.isActive) cont.resumeWith(Result.success(r))
                        }
                    }
                    if (ok) frame = bmp.asImageBitmap()
                    // playing: next frame immediately; paused: still notice a seek / new picture
                    // Each PixelCopy of the live SurfaceView stalls the compositor; one back-to-back (4 ms) made the picture and audio stutter.
                    // ~12 fps is plenty for a blurred glow.
                    kotlinx.coroutines.delay(if (isPlaying) 80L else 400L)
                }
            }

            val img = frame
            // NOTE: the sizing Box below must exist BEFORE the first frame arrives - it is what measures the screen (glowSize)
            // that the frame grabber needs. (It used to be skipped until a frame existed, so no frame was ever grabbed.)
            if (!showGlow && glowAlpha <= 0.01f) return

            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { glowSize = it }
                    .graphicsLayer(alpha = glowAlpha)
            ) {
                if (img != null && fitW >= 4f && fitH >= 4f) {
                // Bars: left+right when the picture is narrower than the screen, otherwise top+bottom
                val sideBars = fitW < cw - 1.5f
                val barA = if (sideBars) picLeft else picTop                 // size of first bar (px)
                val barB = if (sideBars) (cw - picLeft - fitW) else (ch - picTop - fitH)
                for (bar in 0..1) {
                    val thick = if (bar == 0) barA else barB
                    if (thick < 1.5f) continue
                    val x: Float
                    val y: Float
                    val w: Float
                    val h: Float
                    if (sideBars) {
                        x = if (bar == 0) 0f else picLeft + fitW; y = 0f; w = thick; h = ch
                    } else {
                        x = 0f; y = if (bar == 0) 0f else picTop + fitH; w = cw; h = thick
                    }
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(x.roundToInt(), y.roundToInt()) }
                            .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                            .clipToBounds()
                            .graphicsLayer {
                                // soft blur on Android 12+, edge pixels are clamped so the seam keeps the exact picture colour
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    renderEffect = androidx.compose.ui.graphics.BlurEffect(
                                        28.dp.toPx(), 28.dp.toPx(), androidx.compose.ui.graphics.TileMode.Clamp
                                    )
                                }
                            }
                    ) {
                        val bw = img.width
                        val bh = img.height
                        // how much of the picture (next to this bar) is mirrored out into it: as much as the bar is thick
                        val first = bar == 0
                        if (sideBars) {
                            val sw = (thick / fitW * bw).roundToInt().coerceIn(2, bw / 2)
                            val sx = if (first) 0 else bw - sw
                            withTransform({ scale(-1f, 1f, pivot = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)) }) {
                                drawImage(
                                    image = img,
                                    srcOffset = androidx.compose.ui.unit.IntOffset(sx, 0),
                                    srcSize = androidx.compose.ui.unit.IntSize(sw, bh),
                                    dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                                )
                            }
                            // slightly darker towards the screen edge so the picture stays the star (seam stays untouched)
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                                    startX = if (first) size.width else 0f,
                                    endX = if (first) 0f else size.width
                                )
                            )
                        } else {
                            val sh = (thick / fitH * bh).roundToInt().coerceIn(2, bh / 2)
                            val sy = if (first) 0 else bh - sh
                            withTransform({ scale(1f, -1f, pivot = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)) }) {
                                drawImage(
                                    image = img,
                                    srcOffset = androidx.compose.ui.unit.IntOffset(0, sy),
                                    srcSize = androidx.compose.ui.unit.IntSize(bw, sh),
                                    dstOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High
                                )
                            }
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                                    startY = if (first) size.height else 0f,
                                    endY = if (first) 0f else size.height
                                )
                            )
                        }
                    }
                }
                }
            }
        }

        // -------------------------------------------------------------
        // LIVE SCALABLE & PANNABLE VIDEO RENDERING CONTAINER
        // -------------------------------------------------------------
        RenderScalableVideoSurface()

        RenderAmbienceGlow()

        // -------------------------------------------------------------
        // BUFFERING INDICATOR
        // -------------------------------------------------------------
        // Streams (online / M3U live TV) always show it so a black screen never looks like a hang.
        // ONE loading indicator for every wait (opening servers of the next episode OR buffering): the two used to be
        // drawn on top of each other (a second one appeared as soon as Play was pressed). No text under it.
        if (queuedResolving ||
            ((uiState.playerSettings.showLoadingCircle || controller.onlineStreamMode) && isBuffering && isPlaying)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                ViauraExpressiveLoadingIndicator(
                    size = 56.dp,
                    isDark = true
                )
            }
        }

        RenderResolveError()

        RenderAllServersFailed()

        // -------------------------------------------------------------
        // LIQUID GLASS PLAYER CONTROLS OVERLAY
        // -------------------------------------------------------------
        RenderVideoPlayerOverlay()

        if (showSubtitleFileTree) {
            SubtitleFileTreeDialog(
                initialVideoPath = currentVideo?.path,
                isLandscape = isLandscape,
                glassBlurTransparency = uiState.glassBlurTransparency,
                onSubtitleSelected = { selectedFile ->
                    coroutineScope.launch {
                        try {
                            if (importSubtitleFileSafely(selectedFile)) {
                                Toast.makeText(context, "Loaded Subtitle: ${selectedFile.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to load subtitle file", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenSystemPicker = {
                    subtitleFilePicker.launch(arrayOf("*/*"))
                },
                onDismiss = {
                    showSubtitleFileTree = false
                }
            )
        }

        if (showAudioFileTree) {
            AudioFileTreeDialog(
                initialVideoPath = currentVideo?.path,
                isLandscape = isLandscape,
                glassBlurTransparency = uiState.glassBlurTransparency,
                onAudioSelected = { selectedFile ->
                    coroutineScope.launch {
                        try {
                            if (selectedFile.exists() && selectedFile.length() > 0) {
                                hasManualAudioTrackSelection = true
                                val addedAudioId = controller.addExternalAudio(selectedFile.absolutePath, originalName = selectedFile.name)
                                if (addedAudioId != null && addedAudioId > 0) {
                                    selectedAudioTrackId = addedAudioId
                                    controller.setAudioTrack(addedAudioId)
                                } else {
                                    val lastTrack = controller.audioTracks.value.lastOrNull()?.id
                                    if (lastTrack != null && lastTrack > 0) {
                                        selectedAudioTrackId = lastTrack
                                        controller.setAudioTrack(lastTrack)
                                    }
                                }
                                Toast.makeText(context, "Loaded Audio Track: ${selectedFile.name}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to load audio file", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error loading audio: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onOpenSystemPicker = {
                    audioFilePicker.launch(arrayOf("audio/*", "*/*"))
                },
                onDismiss = {
                    showAudioFileTree = false
                }
            )
        }
    }
    if (channelMode) {
        ChannelModeLayer(
            currentVideo = currentVideo,
            currentPlaylist = currentPlaylist,
            uiState = uiState,
            chCardH = chCardH,
            onHeaderBack = { exitPlayerNow(toMini = true) },
            onPlayItem = { item ->
                val at = currentPlaylist.indexOfFirst { it.path == item.path }
                if (at >= 0) {
                    playEpisodeAtIndex(at)
                } else {
                    currentPlaylist = currentPlaylist + item
                    playEpisodeAtIndex(currentPlaylist.lastIndex)
                }
            }
        )
    }
}
}

/**
 * Portrait channel player layer (header, details / recommended list, bottom handle, channel page). Kept OUT of VideoPlayerScreen on
 * purpose: that function is huge, and adding more locals to it made R8 produce a class the Android verifier rejects (VerifyError on opening a video).
 */
@Composable
private fun BoxScope.ChannelModeLayer(
    currentVideo: VideoItem?,
    currentPlaylist: List<VideoItem>,
    uiState: UiState,
    chCardH: androidx.compose.ui.unit.Dp,
    onHeaderBack: () -> Unit,
    onPlayItem: (VideoItem) -> Unit
) {
    // same page metrics as the player screen computes for the card (kept identical on purpose)
    val chIsDark = uiState.themeMode == ThemeMode.DARK ||
        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    val chPageBg = when {
        chIsDark && uiState.amoledBlackMode -> com.example.ui.theme.DarkBackgroundStart
        chIsDark -> Neutral.D1B2630
        uiState.themeMode == ThemeMode.SYSTEM -> Neutral.FFFFFF
        else -> Neutral.DAE3E9
    }
    val chScale = (0.85f + 0.15f * ((uiState.appScale.coerceIn(1f, 100f) - 1f) / 99f)).coerceIn(0.85f, 1.00f)
    val chHPad = (14f * chScale).dp
    val chStatusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val chHeaderBottom = chStatusTop + (58f * chScale).dp + (6f * chScale).dp
    var channelCardsHidden by remember { mutableStateOf(false) }
    var openedChannel by remember { mutableStateOf<com.example.util.OnlineChannelResult?>(null) }
    val chScaledDensity = LocalDensity.current.let { d -> Density(d.density * chScale, d.fontScale * chScale) }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(top = chStatusTop, start = chHPad, end = chHPad)
    ) {
        CompositionLocalProvider(LocalDensity provides chScaledDensity) {
            HeaderBar(
                title = when {
                    isProviderPlayback(currentVideo) -> providerNameOf(currentVideo)
                    currentVideo?.isOnline == true -> "Online"
                    currentVideo?.isNetworkStream == true -> "Network"
                    currentVideo?.isDirectStream == true -> "Stream"
                    currentVideo != null && com.example.util.media.PluginQueuedEpisodes.isQueued(currentVideo.path) ->
                        currentVideo.channelName?.takeIf { it.isNotBlank() } ?: "Media"
                    else -> currentVideo?.path?.let { File(it).parentFile?.name }?.takeIf { it.isNotBlank() } ?: "Player"
                },
                themeMode = uiState.themeMode,
                onBackClick = onHeaderBack,
                onSearchClick = { PinnedHeaderUi.onHeaderAction(com.example.util.PlayerHeaderAction.SEARCH) },
                onSortClick = { PinnedHeaderUi.onHeaderAction(com.example.util.PlayerHeaderAction.SORT) },
                onSettingsClick = { PinnedHeaderUi.onHeaderAction(com.example.util.PlayerHeaderAction.SETTINGS) }
            )
        }
    }
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxSize()
            .padding(top = chHeaderBottom + PinnedHeaderUi.searchExtra + chCardH + 8.dp, start = chHPad, end = chHPad)
    ) {
        CompositionLocalProvider(LocalDensity provides chScaledDensity) {
            if (isProviderPlayback(currentVideo)) {
                ProviderPlayerSection(
                    video = currentVideo,
                    playlist = currentPlaylist,
                    uiState = uiState,
                    isDark = chIsDark,
                    cardsHidden = channelCardsHidden,
                    onPlayItem = onPlayItem
                )
            } else {
                ChannelPlayerSection(
                    video = currentVideo,
                    playlist = currentPlaylist,
                    uiState = uiState,
                    isDark = chIsDark,
                    cardsHidden = channelCardsHidden,
                    onOpenChannel = { openedChannel = it },
                    onPlayItem = onPlayItem
                )
            }
        }
    }

    // Handle on the bottom gradient border line of the video card: drag up = title / subscribe / comments cards hide
    // (recommended list moves up), drag down (or tap) = they come back.
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = chHPad, end = chHPad, top = chHeaderBottom + PinnedHeaderUi.searchExtra + chCardH - 4.dp)
            .fillMaxWidth()
            .height(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -4f) channelCardsHidden = true
                        else if (dragAmount > 4f) channelCardsHidden = false
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { channelCardsHidden = !channelCardsHidden },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentGradient)
            )
        }
    }

    // Header buttons: search bar + results, Sort & View popup and Settings sheet all open here, the video keeps playing (no mini player).
    PinnedSearchLayer(
        isDark = chIsDark,
        uiState = uiState,
        chScale = chScale,
        chHeaderBottom = chHeaderBottom,
        chHPad = chHPad,
        chCardH = chCardH,
        pageBg = chPageBg,
        onPlayVideo = onPlayItem,
        onOpenChannel = { openedChannel = it }
    )

    // Channel page opened from the video's channel row. It lives INSIDE the player, so Back closes only this page
    // and the same video is still there (the player is never left).
    val chOpened = openedChannel
    if (chOpened != null) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
                .background(chPageBg)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            com.example.ui.screens.ChannelDetailScreen(
                channel = chOpened,
                uiState = uiState,
                viewModel = null,
                isDark = chIsDark,
                isAudioMode = false,
                onBack = { openedChannel = null },
                onPlayVideo = { v, _ ->
                    openedChannel = null
                    onPlayItem(v)
                }
            )
        }
    }
}

/** Tells the Liquid Glass sample (see VideoGlassBackdrop) whether Ambience glow is on: [dar] = picture width / height, 0 = off. */
@Composable
private fun AmbienceGlassSync(source: com.example.ui.components.VideoGlassSource, dar: Float) {
    androidx.compose.runtime.SideEffect { source.ambienceDar = dar }
    androidx.compose.runtime.DisposableEffect(source) { onDispose { source.ambienceDar = 0f } }
}

fun getPlayableFilePath(context: Context, uri: Uri?, fallbackPath: String? = null): String {
    try {
        // A video inside a ZIP / TAR archive: mpv reads it through the app's own loopback server (no extraction, no copy).
        if (!fallbackPath.isNullOrBlank() && com.example.util.archive.ArchiveStore.isArchivePath(fallbackPath)) {
            return com.example.util.archive.ArchiveStreamServer.urlFor(context, fallbackPath) ?: fallbackPath
        }
        if (!fallbackPath.isNullOrBlank() && fallbackPath.startsWith(OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX)) {
            return OnlineVideoService.toWatchUrl(fallbackPath)
        }
        if (!fallbackPath.isNullOrBlank()) {
            if (fallbackPath.startsWith("/") || fallbackPath.startsWith("fd://") || fallbackPath.startsWith("http://") || fallbackPath.startsWith("https://")) {
                return fallbackPath
            }
            if (File(fallbackPath).exists() && File(fallbackPath).length() > 0) {
                return fallbackPath
            }
        }
        if (uri == null) return fallbackPath ?: ""

        val uriStr = uri.toString()
        if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                return path
            }
        }
        if (uri.scheme == "asset" || uriStr.startsWith("asset:///")) {
            val assetName = if (uriStr.startsWith("asset:///")) {
                uriStr.removePrefix("asset:///")
            } else {
                uri.path?.removePrefix("/") ?: ""
            }
            if (assetName.isNotBlank()) {
                val targetFile = File(context.cacheDir, assetName)
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    try {
                        context.assets.open(assetName).use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (_: Throwable) {}
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    return targetFile.absolutePath
                }
            }
        }
        if (uri.scheme == "content") {
            try {
                val projection = arrayOf(android.provider.MediaStore.Video.Media.DATA)
                val directPath = context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val columnIndex = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
                    if (columnIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(columnIndex)
                    } else null
                }
                if (!directPath.isNullOrBlank() && File(directPath).exists() && File(directPath).length() > 0) {
                    return directPath
                }
            } catch (_: Throwable) {}

            // ZERO-COPY DIRECT STREAMING: Directly acquire FileDescriptor from ContentResolver without copying any data to storage
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val fd = pfd.detachFd()
                    if (fd >= 0) {
                        return "fd://$fd"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("VideoPlayerScreen", "Failed to get direct file descriptor for $uri", e)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return fallbackPath ?: ""
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}



/** Liquid Glass: reports where the native video view sits in the window (zoom / pan included) to the glass source. */
private fun Modifier.trackVideoGlassBounds(source: com.example.ui.components.VideoGlassSource): Modifier =
    this.onGloballyPositioned {
        source.coordinates = it
        source.refreshBounds()
    }

// =====================================================================================================
// PINNED VIDEO LAYOUT (Settings > Player > Display & Controls > "Pinned video layout", portrait only)
// Everything under the video card: details card (title, views, channel + Subscribe, action row), comments card,
// recommended videos. Lives in this file on purpose: it is part of the video player screen.
// =====================================================================================================

private fun chFormatCount(n: Long): String = when {
    n >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.1fB", n / 1_000_000_000.0)
    n >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", n / 1_000_000.0)
    n >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", n / 1_000.0)
    else -> n.toString()
}

@Composable
private fun ChannelActionChip(
    iconRes: Int,
    label: String,
    isDark: Boolean,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val bg = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (iconRes != 0) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = label,
                tint = if (active) com.example.ui.theme.AccentSkyBlue else primary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (label.isNotBlank()) {
            Text(text = label, color = primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun ChannelPlayerSection(
    video: VideoItem?,
    playlist: List<VideoItem>,
    uiState: UiState,
    isDark: Boolean,
    cardsHidden: Boolean = false,
    onOpenChannel: (com.example.util.OnlineChannelResult) -> Unit = {},
    onPlayItem: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val glass = uiState.glassBlurTransparency
    val cardShape = RoundedCornerShape(16.dp)
    val watchUrl: String? = video?.takeIf { it.isOnline }?.let { OnlineVideoService.toWatchUrl(it.path) }
    val prefs = remember { context.getSharedPreferences("viaura_channel_player", Context.MODE_PRIVATE) }
    // viaura_like / viaura_dislike come from viaura-icons.zip (unpacked into res/drawable before the build). Direct references,
    // so the resources are always packed into the APK and the Like / Dislike chips can never end up without their icon.
    val likeIconRes = com.example.R.drawable.viaura_like
    val dislikeIconRes = com.example.R.drawable.viaura_dislike

    var details by remember(watchUrl) { mutableStateOf<OnlineWatchDetails?>(null) }
    var commentPage by remember(watchUrl) { mutableStateOf<OnlineCommentPage?>(null) }
    var commentsLoading by remember(watchUrl) { mutableStateOf(false) }
    var showComments by remember(watchUrl) { mutableStateOf(false) }
    var descExpanded by remember(watchUrl) { mutableStateOf(false) }
    var liked by remember(watchUrl) { mutableStateOf(watchUrl != null && prefs.getBoolean("like_$watchUrl", false)) }
    var disliked by remember(watchUrl) { mutableStateOf(watchUrl != null && prefs.getBoolean("dislike_$watchUrl", false)) }
    var saved by remember(watchUrl) {
        mutableStateOf(watchUrl != null && com.example.util.OnlineMediaLibrary.isWatchLater(context, com.example.util.OnlineMediaLibrary.KIND_VIDEO, watchUrl))
    }
    var downloadRequest by remember { mutableStateOf<com.example.ui.components.DownloadRequest?>(null) }
    var playlistTarget by remember { mutableStateOf<OnlineTrackResult?>(null) }
    var localPlaylistTarget by remember { mutableStateOf<VideoItem?>(null) }
    com.example.ui.components.DownloadRequestHost(downloadRequest, isDark, glass) { downloadRequest = null }

    LaunchedEffect(watchUrl) {
        if (watchUrl != null) {
            try {
                details = OnlineVideoService.fetchWatchDetails(watchUrl)
                commentsLoading = true
                commentPage = OnlineVideoService.fetchComments(watchUrl)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (t: Throwable) {
                android.util.Log.w("VideoPlayerScreen", "Pinned layout: details / comments failed", t)
            } finally {
                commentsLoading = false
            }
        }
    }

    // YouTube no longer exposes the dislike count: the public Return YouTube Dislike estimate fills it in (silently skipped when offline).
    var dislikeEstimate by remember(watchUrl) { mutableStateOf(-1L) }
    LaunchedEffect(watchUrl) {
        val u = watchUrl
        if (u != null) {
            dislikeEstimate = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val id = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})").find(u)?.groupValues?.get(1) ?: return@withContext -1L
                    val conn = java.net.URL("https://returnyoutubedislikeapi.com/votes?videoId=$id").openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    try {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        org.json.JSONObject(body).optLong("dislikes", -1L)
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Throwable) { -1L }
            }
        }
    }

    var subscribed by remember(details?.uploaderUrl) {
        mutableStateOf(details?.uploaderUrl?.let { com.example.util.SubscribedChannelsManager.isSubscribed(context, it) } ?: false)
    }

    var avatarBitmap by remember(details?.avatarUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(details?.avatarUrl) {
        val u = details?.avatarUrl
        if (!u.isNullOrBlank()) {
            try {
                avatarBitmap = loadRemoteAudioThumbnail(u, targetSizePx = 160)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Throwable) {}
        }
    }

    val currentTrack: OnlineTrackResult? = remember(video, details) {
        if (video != null && watchUrl != null) OnlineTrackResult(
            videoUrl = watchUrl,
            title = video.displayName,
            artist = details?.uploader?.takeIf { it.isNotBlank() } ?: video.channelName.orEmpty(),
            durationSec = (video.durationMs / 1000L).coerceAtLeast(0L),
            thumbnailUrl = video.thumbnailUrl
        ) else null
    }

    // Recommended videos: YouTube's own related list of THIS video (same topic), re-ordered so one channel never repeats back to back.
    val hidden = remember(watchUrl) {
        com.example.util.OnlineMediaLibrary.notInterestedSet(context, com.example.util.OnlineMediaLibrary.KIND_VIDEO)
    }
    val recommended: List<VideoItem> = remember(details, playlist, video) {
        val list: List<VideoItem> = if (watchUrl != null) {
            val rel = details?.related.orEmpty().filter { it.videoUrl !in hidden }
            com.example.util.OnlineVideoRecommender.spreadChannels(rel).map { it.toOnlineVideoItem() }
        } else {
            // Not YouTube (local file, Network tab link, stream, provider media):
            //  - a single movie gets NO recommendations,
            //  - an episode of a series gets the other episodes of that series (the ones after it first, then the earlier ones).
            val isProviderLike = video != null && (video.isNetworkStream || video.isDirectStream ||
                com.example.util.media.PluginQueuedEpisodes.isQueued(video.path))
            val series: List<VideoItem> = if (isProviderLike) {
                playlist
            } else {
                val curFolder = video?.path?.let { File(it).parent }
                val sameFolder = if (curFolder != null) playlist.filter { File(it.path).parent == curFolder } else emptyList()
                if (sameFolder.size > 1) sameFolder else emptyList()
            }
            val at = series.indexOfFirst { it.path == video?.path }
            when {
                series.size <= 1 -> emptyList()
                at >= 0 -> series.drop(at + 1) + series.take(at)
                else -> series
            }
        }
        // LazyColumn keys are the paths: one repeated path would crash the list
        list.distinctBy { it.path }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ------------------------------------------------------------------ details card
        item(key = "channel_info") {
            androidx.compose.animation.AnimatedVisibility(
                visible = !cardsHidden,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanelSurface(cardShape, isDark, glass)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = video?.displayName ?: details?.title.orEmpty(),
                    color = primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = if (watchUrl != null) {
                    listOfNotNull(
                        details?.viewCount?.takeIf { it >= 0 }?.let { "${chFormatCount(it)} views" } ?: video?.viewCountText,
                        details?.uploadDateText?.takeIf { it.isNotBlank() }
                    ).joinToString(" \u2022 ")
                } else {
                    listOf(video?.formattedSize.orEmpty(), video?.durationFormatted.orEmpty(), video?.resolution.orEmpty())
                        .filter { it.isNotBlank() && it != "0 B" }.joinToString(" \u2022 ")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = meta, color = secondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (watchUrl != null && !details?.description.isNullOrBlank()) {
                        Text(
                            text = if (descExpanded) "  less" else "  ...more",
                            color = primary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { descExpanded = !descExpanded }
                        )
                    }
                }
                if (descExpanded && !details?.description.isNullOrBlank()) {
                    Text(text = details?.description.orEmpty(), color = secondary, fontSize = 12.5.sp, maxLines = 14, overflow = TextOverflow.Ellipsis)
                }

                if (watchUrl != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Avatar + channel name + subscribers: tap = open this channel (Back returns to this very video).
                        val channelUrlForOpen = details?.uploaderUrl
                        val channelNameForOpen = details?.uploader?.takeIf { it.isNotBlank() } ?: video?.channelName.orEmpty()
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    enabled = !channelUrlForOpen.isNullOrBlank(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val u = channelUrlForOpen
                                    if (!u.isNullOrBlank()) {
                                        val subsN = details?.subscriberCount ?: -1L
                                        onOpenChannel(
                                            com.example.util.OnlineChannelResult(
                                                channelUrl = u,
                                                name = channelNameForOpen,
                                                avatarUrl = details?.avatarUrl,
                                                subscriberCountText = if (subsN >= 0) "${chFormatCount(subsN)} subscribers" else null,
                                                videoCountText = null
                                            )
                                        )
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(AccentGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = avatarBitmap
                            if (bmp != null) {
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape))
                            } else {
                                Text(
                                    text = (details?.uploader ?: video?.channelName.orEmpty()).take(1).uppercase(),
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = details?.uploader?.takeIf { it.isNotBlank() } ?: video?.channelName.orEmpty(),
                                color = primary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            val subs = details?.subscriberCount ?: -1L
                            if (subs >= 0) Text(text = "${chFormatCount(subs)} subscribers", color = secondary, fontSize = 12.sp, maxLines = 1)
                        }
                        }
                        val subUrl = details?.uploaderUrl
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (subscribed) SolidColorBrushFor(isDark) else AccentGradient)
                                .bounceClick(scaleDown = 0.94f) {
                                    if (!subUrl.isNullOrBlank()) {
                                        subscribed = !subscribed
                                        com.example.util.SubscribedChannelsManager.setSubscribed(
                                            context, subUrl, details?.uploader.orEmpty(), details?.avatarUrl, subscribed
                                        )
                                    }
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (subscribed) "Subscribed" else "Subscribe",
                                color = if (subscribed) primary else Color.White,
                                fontSize = 13.5.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Like / Dislike / Save / Download / Playlist / Share / Link copy - slides left and right
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val t = currentTrack
                    if (t != null) {
                        val likeCount = details?.likeCount ?: -1L
                        ChannelActionChip(
                            iconRes = likeIconRes,
                            label = if (likeCount >= 0) chFormatCount(likeCount + if (liked) 1 else 0) else "Like",
                            isDark = isDark, active = liked
                        ) {
                            liked = !liked
                            if (liked) disliked = false
                            prefs.edit().putBoolean("like_${t.videoUrl}", liked).putBoolean("dislike_${t.videoUrl}", disliked).apply()
                            if (liked) com.example.util.OnlineVideoRecommender.recordWatchedHalf(context, t.videoUrl)
                        }
                        ChannelActionChip(
                            iconRes = dislikeIconRes,
                            label = (details?.dislikeCount?.takeIf { it > 0 } ?: dislikeEstimate).let { if (it > 0) chFormatCount(it + if (disliked) 1 else 0) else "Dislike" },
                            isDark = isDark, active = disliked
                        ) {
                            disliked = !disliked
                            if (disliked) liked = false
                            prefs.edit().putBoolean("like_${t.videoUrl}", liked).putBoolean("dislike_${t.videoUrl}", disliked).apply()
                        }
                        ChannelActionChip(
                            iconRes = if (saved) com.example.R.drawable.viaura_saved_bookmark else com.example.R.drawable.viaura_bookmark,
                            label = "Save", isDark = isDark, active = saved
                        ) {
                            com.example.util.OnlineMediaLibrary.rememberMeta(context, com.example.util.OnlineMediaLibrary.KIND_VIDEO, t)
                            saved = com.example.util.OnlineMediaLibrary.toggleWatchLater(context, com.example.util.OnlineMediaLibrary.KIND_VIDEO, t)
                            Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                        }
                        ChannelActionChip(com.example.R.drawable.viaura_download_minimalistic, "Download", isDark) {
                            downloadRequest = com.example.ui.components.DownloadRequest(t.videoUrl, t.title, t.artist, t.thumbnailUrl, false)
                        }
                        ChannelActionChip(com.example.R.drawable.viaura_playlist_video, "Playlist", isDark) {
                            com.example.util.OnlineMediaLibrary.rememberMeta(context, com.example.util.OnlineMediaLibrary.KIND_VIDEO, t)
                            playlistTarget = t
                        }
                        ChannelActionChip(com.example.R.drawable.viaura_share, "Share", isDark) {
                            com.example.ui.components.shareOnlineLink(context, t.title, t.videoUrl)
                        }
                        ChannelActionChip(com.example.R.drawable.viaura_copy, "Link copy", isDark) {
                            try {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("link", t.videoUrl))
                                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                            } catch (_: Throwable) {}
                        }
                    } else if (video != null && !video.isNetworkStream && !video.isDirectStream &&
                        !com.example.util.media.PluginQueuedEpisodes.isQueued(video.path)) {
                        ChannelActionChip(com.example.R.drawable.viaura_playlist_video, "Playlist", isDark) { localPlaylistTarget = video }
                    }
                }
            }
            }
        }

        // ------------------------------------------------------------------ comments card (read only)
        if (watchUrl != null) {
            item(key = "channel_comments") {
                val page = commentPage
                androidx.compose.animation.AnimatedVisibility(
                    visible = !cardsHidden,
                    enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                    exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanelSurface(cardShape, isDark, glass)
                        .clickable(enabled = page != null && page.items.isNotEmpty()) { showComments = true }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalText = page?.total?.takeIf { it > 0 }?.let { " \u00B7 ${chFormatCount(it)}" }.orEmpty()
                    Text(text = "Comments$totalText", color = primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    when {
                        commentsLoading -> ViauraExpressiveLoadingIndicator(size = 22.dp, isDark = isDark)
                        page == null || page.items.isEmpty() -> Text(text = "Comments are off or not available", color = secondary, fontSize = 12.5.sp)
                        else -> Text(text = page.items.first().text, color = secondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                }
            }
        }

        // ------------------------------------------------------------------ recommended videos
        if (recommended.isNotEmpty()) {
            item(key = "channel_rec_title") {
                Text(
                    text = if (watchUrl != null) "Recommended" else "Up next",
                    color = primary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
            items(recommended, key = { "channel_rec_${it.path}" }) { item ->
                VideoItemCard(
                    video = item,
                    isDark = isDark,
                    visibleFields = VisibleFields(showExtension = false, showSize = false, showSubtitleIndicator = false),
                    onClick = {
                        if (item.isOnline) {
                            com.example.util.OnlineVideoRecommender.recordOpened(
                                context, OnlineVideoService.toWatchUrl(item.path), item.channelName.orEmpty(), item.displayName
                            )
                        }
                        onPlayItem(item)
                    }
                )
            }
        } else if (watchUrl != null && details == null) {
            item(key = "channel_rec_loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    ViauraExpressiveLoadingIndicator(size = 32.dp, isDark = isDark)
                }
            }
        }
    }

    // ---------------------------------------------------------------------- comments sheet (opens ~65 %, handle pulls it taller)
    if (showComments) {
        DownloadGlassSheet(isDark = isDark, glassBlurTransparency = glass, onDismiss = { showComments = false }) {
            val page = commentPage
            Text(
                text = "Comments",
                color = primary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            page?.items?.forEach { c ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(AccentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = c.author.removePrefix("@").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = listOf(c.author, c.dateText).filter { it.isNotBlank() }.joinToString(" \u2022 ") + if (c.isPinned) " \u2022 Pinned" else "",
                            color = secondary, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(text = c.text, color = primary, fontSize = 13.5.sp)
                        if (c.likesText.isNotBlank() && c.likesText != "0") {
                            Text(text = "\uD83D\uDC4D ${c.likesText}", color = secondary, fontSize = 11.5.sp)
                        }
                    }
                }
            }
            if (page?.nextPage != null) {
                var loadingMore by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentGradient)
                        .clickable(enabled = !loadingMore) {
                            loadingMore = true
                            scope.launch {
                                try {
                                    val more = OnlineVideoService.fetchMoreComments(page)
                                    if (more != null) commentPage = OnlineCommentPage(page.items + more.items, more.total, more.nextPage, more.info)
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (_: Throwable) {
                                } finally {
                                    loadingMore = false
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (loadingMore) "Loading..." else "Load more comments", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    playlistTarget?.let { t ->
        com.example.ui.components.AddToPlaylistDialog(
            selectedVideos = listOf(t.toOnlineVideoItem()),
            isDark = isDark,
            glassBlurTransparency = glass,
            onDismiss = { playlistTarget = null },
            onPlaylistUpdated = { playlistTarget = null }
        )
    }
    localPlaylistTarget?.let { v ->
        com.example.ui.components.AddToPlaylistDialog(
            selectedVideos = listOf(v),
            isDark = isDark,
            glassBlurTransparency = glass,
            onDismiss = { localPlaylistTarget = null },
            onPlaylistUpdated = { localPlaylistTarget = null }
        )
    }
}

// ---- Media provider (plugin) videos in the pinned layout: no YouTube-style cards, just title, actions and the episodes ----

private fun providerSessionOf(v: VideoItem?): com.example.util.media.PluginPlaybackSession? =
    v?.path?.let { com.example.util.media.PluginPlaybackSessions.get(it) }

/** True for a video that comes from a media provider (a resolved stream with a provider session, or a queued episode). */
private fun isProviderPlayback(v: VideoItem?): Boolean =
    v != null && (providerSessionOf(v) != null || com.example.util.media.PluginQueuedEpisodes.isQueued(v.path))

private fun providerNameOf(v: VideoItem?): String =
    providerSessionOf(v)?.plugin?.name?.takeIf { it.isNotBlank() }
        ?: v?.channelName?.takeIf { it.isNotBlank() }
        ?: "Provider"

/** Under the video: the episode's title with Save / Download / Playlist / Share / Link, then (for a series) its episodes. */
@Composable
private fun ProviderPlayerSection(
    video: VideoItem?,
    playlist: List<VideoItem>,
    uiState: UiState,
    isDark: Boolean,
    cardsHidden: Boolean = false,
    onPlayItem: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val glass = uiState.glassBlurTransparency
    val cardShape = RoundedCornerShape(16.dp)
    val session = providerSessionOf(video)
    val plugin = session?.plugin
    val episodeLink = session?.episodeLink.orEmpty()
    val episodeType = session?.episodeType.orEmpty()
    val episodeTitle = session?.episodeTitle?.takeIf { it.isNotBlank() } ?: video?.displayName.orEmpty()
    val shareLink = episodeLink.takeIf { it.startsWith("http", true) } ?: video?.path?.takeIf { it.startsWith("http", true) }.orEmpty()

    var saved by remember(episodeLink) {
        mutableStateOf(plugin != null && episodeLink.isNotBlank() && com.example.util.media.MediaLibraryStore.isSaved(context, plugin.value, episodeLink))
    }
    var showDownload by remember(episodeLink) { mutableStateOf(false) }

    // A single movie / single episode shows nothing below; a series shows all its episodes with the playing one marked.
    val episodes = remember(playlist) {
        playlist.filter { com.example.util.media.PluginQueuedEpisodes.isQueued(it.path) }.distinctBy { it.path }
    }
    val showEpisodes = (session == null || session.isSeries) && episodes.size > 1
    val currentIdx = remember(video, episodes, session) {
        val wanted = session?.episodeIndex
        episodes.indexOfFirst { it.path == video?.path || (wanted != null && it.path.substringAfterLast('/').toIntOrNull() == wanted) }
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(currentIdx, showEpisodes) {
        if (showEpisodes && currentIdx > 0) listState.scrollToItem(2 + currentIdx - 1)
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "provider_info") {
            androidx.compose.animation.AnimatedVisibility(
                visible = !cardsHidden,
                enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
                exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassPanelSurface(cardShape, isDark, glass)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = video?.displayName.orEmpty(),
                        color = primary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOf(video?.formattedSize.orEmpty(), video?.durationFormatted.orEmpty(), video?.resolution.orEmpty())
                        .filter { it.isNotBlank() && it != "0 B" && it != "00:00" }.joinToString(" \u2022 ")
                    if (meta.isNotBlank()) {
                        Text(text = meta, color = secondary, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (plugin != null && episodeLink.isNotBlank()) {
                            ChannelActionChip(
                                iconRes = if (saved) com.example.R.drawable.viaura_saved_bookmark else com.example.R.drawable.viaura_bookmark,
                                label = "Save", isDark = isDark, active = saved
                            ) {
                                saved = com.example.util.media.MediaLibraryStore.toggleWatchLater(
                                    context,
                                    com.example.util.media.MediaSavedVideo(
                                        pluginId = plugin.value,
                                        title = episodeTitle,
                                        pageTitle = video?.displayName.orEmpty(),
                                        poster = video?.thumbnailUrl.orEmpty(),
                                        link = episodeLink,
                                        type = episodeType
                                    )
                                )
                                Toast.makeText(context, if (saved) "Added to Watch later" else "Removed from Watch later", Toast.LENGTH_SHORT).show()
                            }
                            ChannelActionChip(com.example.R.drawable.viaura_download_minimalistic, "Download", isDark) {
                                showDownload = true
                            }
                        }
                        if (plugin != null && video != null) {
                            ChannelActionChip(com.example.R.drawable.viaura_playlist_video, "Playlist", isDark) {
                                com.example.util.media.MediaPlayback.addToPluginPlaylist(context, plugin, video)
                                Toast.makeText(context, "Added to the \"${plugin.name}\" playlist", Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (shareLink.isNotBlank()) {
                            ChannelActionChip(com.example.R.drawable.viaura_share, "Share", isDark) {
                                com.example.ui.components.shareOnlineLink(context, video?.displayName.orEmpty(), shareLink)
                            }
                            ChannelActionChip(com.example.R.drawable.viaura_copy, "Link copy", isDark) {
                                try {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("link", shareLink))
                                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                                } catch (_: Throwable) {}
                            }
                        }
                    }
                }
            }
        }

        if (showEpisodes) {
            item(key = "provider_ep_title") {
                Text(
                    text = "Episodes \u2022 ${episodes.size}",
                    color = primary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }
            items(episodes, key = { "provider_ep_${it.path}" }) { ep ->
                val isCurrent = ep.path == episodes.getOrNull(currentIdx)?.path
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isCurrent) Modifier.border(1.5.dp, AccentGradient, RoundedCornerShape(16.dp)) else Modifier)
                ) {
                    VideoItemCard(
                        video = ep,
                        isDark = isDark,
                        visibleFields = VisibleFields(showExtension = false, showSize = false, showSubtitleIndicator = false),
                        onClick = { if (!isCurrent) onPlayItem(ep) }
                    )
                }
            }
        }
    }

    if (showDownload && plugin != null) {
        com.example.ui.components.MediaDownloadSheet(
            isDark = isDark,
            glassBlurTransparency = glass,
            plugin = plugin,
            title = episodeTitle,
            poster = video?.thumbnailUrl.orEmpty(),
            link = episodeLink,
            type = episodeType,
            onDismiss = { showDownload = false },
            group = if (session?.isSeries == true) video?.displayName.orEmpty().substringBefore(" \u00B7 ").trim() else ""
        )
    }
}

@Composable
private fun SolidColorBrushFor(isDark: Boolean): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.SolidColor(if (isDark) Neutral.D334155 else Neutral.E2E8F0)

private val subtitlePersistMutex = kotlinx.coroutines.sync.Mutex()

/**
 * Marks the chosen uploaded subtitle(s) as selected (and the rest as not selected) for [video], then runs [saveSelection].
 * Kept out of the big player composable (its compiled method size is at the JVM limit) and run off the UI thread.
 */
private fun persistSubtitleChoiceInBackground(
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context,
    video: VideoItem?,
    tracks: List<PlayerMediaTrack>,
    primaryId: Int,
    secondaryId: Int,
    saveSelection: () -> Unit
) {
    scope.launch(Dispatchers.IO) {
        subtitlePersistMutex.lock()
        try {
            tracks.filter { it.isExternal }.forEach { extTrack ->
                CustomSubtitlePersistenceManager.setSubtitleSelected(
                    context = context,
                    path = video?.path,
                    videoId = video?.id ?: 0L,
                    uriString = video?.uri?.toString(),
                    title = video?.displayName,
                    originalName = extTrack.originalFilename.ifBlank { extTrack.title },
                    isSelected = (extTrack.id == primaryId) || (extTrack.id == secondaryId)
                )
            }
            saveSelection()
        } catch (_: Throwable) {
        } finally {
            subtitlePersistMutex.unlock()
        }
    }
}
