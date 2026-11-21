package com.example.ui.screens

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentContainerDark
import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlueStrong
import android.Manifest
import com.example.ui.components.LocalPanelTransparency
import com.example.ui.components.glassPanelColor
import com.example.ui.components.LiquidDialogBlur
import com.example.ui.components.LocalLiquidGlass
import com.example.ui.components.liquidGlass
import com.example.ui.components.liquidGlassOr
import com.example.ui.components.liquidGlassEdge
import com.example.ui.components.liquidGlassBorderBrush
import com.example.ui.components.liquidGlassSheenBrush
import com.example.ui.components.ViauraExpressiveLoadingIndicator
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import com.example.ui.components.extractArtworkColors
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.ui.components.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import com.example.util.AppHaptics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.player.AudioNotificationManager
import com.example.player.AudioPlaybackManager
import com.example.player.AudioRepeatMode
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsProvider
import com.example.domain.lyrics.LyricsSourceType
import com.example.data.lyrics.LyricsTranslationService
import com.example.player.EqualizerPreset
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AudioLyricsContent
import com.example.ui.components.LyricsProviderPickerDialog
import com.example.ui.components.LyricsPlaceholderLines
import com.example.ui.components.AudioEqualizerPanel
import com.example.ui.components.AudioTrackThumbnail
import com.example.ui.components.SleepTimerPanel
import com.example.ui.components.InteractiveAudioScrubber
import com.example.ui.components.StyledIcon
import com.example.ui.components.SubtitleFileTreeDialog
import com.example.ui.components.hasNotificationPermission
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Dedicated Full-Screen Audio Player UI.
 * Conforms strictly to the application's Liquid Glass Design System,
 * accent themes, dynamic color scheme, and responsive typography.
 * Includes A-B Loop creator and notification permission management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    isDark: Boolean,
    glassBlurTransparency: Float = 85f,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isPlaying by AudioPlaybackManager.isPlaying.collectAsState()
    val isBuffering by AudioPlaybackManager.isBuffering.collectAsState()
    val isFetchingLyrics by AudioPlaybackManager.isFetchingLyrics.collectAsState()
    val lyricsModel by AudioPlaybackManager.lyricsModel.collectAsState()
    val activeLyricsProvider by AudioPlaybackManager.activeLyricsProvider.collectAsState()
    val lyricsSourceType by AudioPlaybackManager.lyricsSourceType.collectAsState()
    val positionMs by AudioPlaybackManager.currentPositionMs.collectAsState()
    val durationMs by AudioPlaybackManager.durationMs.collectAsState()
    val queue by AudioPlaybackManager.queue.collectAsState()
    val currentIndex by AudioPlaybackManager.currentIndex.collectAsState()
    val isShuffle by AudioPlaybackManager.isShuffle.collectAsState()
    val repeatMode by AudioPlaybackManager.repeatMode.collectAsState()
    val playbackSpeed by AudioPlaybackManager.playbackSpeed.collectAsState()
    val sleepTimerActive by com.example.util.SleepTimerManager.isActive.collectAsState()
    val sleepTimerRemainingMs by com.example.util.SleepTimerManager.remainingMs.collectAsState()

    // A-B Loop states
    val loopPointA by AudioPlaybackManager.loopPointA.collectAsState()
    val loopPointB by AudioPlaybackManager.loopPointB.collectAsState()
    val isABLoopActive by AudioPlaybackManager.isABLoopActive.collectAsState()

    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder

    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }

    var showInfoSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showLyricsProviderPicker by remember { mutableStateOf(false) }
    var showLyricsLanguagePicker by remember { mutableStateOf(false) }
    var isLyricsViewExpanded by remember { mutableStateOf(false) }
    var showLyricsFilePicker by remember { mutableStateOf(false) }
    var showABLoopSheet by remember { mutableStateOf(false) }
    var showABControlsBar by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var statusToastMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    val lyricsDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    }
                    if (!content.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            AudioPlaybackManager.setLyricsForCurrentTrack(content)
                            statusToastMessage = "Lyrics loaded successfully"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        statusToastMessage = "Could not load lyrics: ${e.message}"
                    }
                }
            }
        }
    }

    // Equalizer & Headphone Audio States
    var equalizerEnabled by remember { mutableStateOf(true) }
    var eq60Hz by remember { mutableFloatStateOf(0f) }
    var eq230Hz by remember { mutableFloatStateOf(0f) }
    var eq910Hz by remember { mutableFloatStateOf(0f) }
    var eq3600Hz by remember { mutableFloatStateOf(0f) }
    var eq14000Hz by remember { mutableFloatStateOf(0f) }
    var volumeBoostDb by remember { mutableFloatStateOf(0f) }
    var currentEqPreset by remember { mutableStateOf("FLAT") }
    var isHeadphoneSurroundActive by remember { mutableStateOf(false) }

    // Real-Time Audio FX synchronization to hardware engine
    LaunchedEffect(equalizerEnabled, eq60Hz, eq230Hz, eq910Hz, eq3600Hz, eq14000Hz, volumeBoostDb) {
        AudioPlaybackManager.updateEqualizer(
            enabled = equalizerEnabled,
            eq60 = eq60Hz,
            eq230 = eq230Hz,
            eq910 = eq910Hz,
            eq3600 = eq3600Hz,
            eq14000 = eq14000Hz,
            volumeBoostDb = volumeBoostDb
        )
    }

    LaunchedEffect(isHeadphoneSurroundActive) {
        AudioPlaybackManager.setHeadphoneSurround(isHeadphoneSurroundActive)
    }

    // Notification Permission Handling (Android 13+)
    val notificationPrefs = remember(context) {
        context.getSharedPreferences("viaura_audio_permissions", Context.MODE_PRIVATE)
    }
    var hasNotifPerm by remember { mutableStateOf(hasNotificationPermission(context)) }
    var showNotifPromptBanner by remember { mutableStateOf(!hasNotificationPermission(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
        if (granted) {
            showNotifPromptBanner = false
            currentTrack?.let { AudioNotificationManager.showOrUpdateNotification(context, it, isPlaying) }
        }
    }

    // Auto-request notification permission only once. Android keeps a denied permission across
    // restarts, so launching the system dialog on every player open was unnecessarily repetitive.
    LaunchedEffect(Unit) {
        val alreadyPrompted = notificationPrefs.getBoolean("notification_prompt_shown", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission(context) && !alreadyPrompted
        ) {
            notificationPrefs.edit().putBoolean("notification_prompt_shown", true).apply()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Show AB controls if user sets loop points
    LaunchedEffect(loopPointA, loopPointB) {
        if (loopPointA != null || loopPointB != null) {
            showABControlsBar = true
        }
    }

    // Intercept system back button to smoothly close secondary bottom sheets first, or collapse to Mini Player
    BackHandler(enabled = true) {
        when {
            showLyricsLanguagePicker -> showLyricsLanguagePicker = false
            showSleepTimerSheet -> showSleepTimerSheet = false
            showInfoSheet -> showInfoSheet = false
            showQueueSheet -> showQueueSheet = false
            showSpeedSheet -> showSpeedSheet = false
            showLyricsSheet -> showLyricsSheet = false
            showABLoopSheet -> showABLoopSheet = false
            showEqualizerSheet -> showEqualizerSheet = false
            showAddToPlaylistDialog -> showAddToPlaylistDialog = false
            isLyricsViewExpanded -> isLyricsViewExpanded = false
            else -> onCollapse()
        }
    }

    val track = currentTrack ?: return

    // Landscape: the screen is split in two halves (artwork / lyrics on the left, controls on the right).
    val isLandscapeLayout = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // The audio player follows the phone's rotation (respecting the system auto-rotate switch) even if a video player
    // locked the activity to portrait earlier; the previous lock is restored when the player closes.
    DisposableEffect(Unit) {
        val activity = generateSequence(context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        onDispose {
            if (activity != null && previous != null) activity.requestedOrientation = previous
        }
    }

    // Lyrics view: starts compact (small, no card), turns full-screen after 3 s with the title/share/playlist
    // controls hidden. Tapping the empty space beside the lyrics toggles the controls back; tapping a line seeks.
    var lyricsImmersive by remember { mutableStateOf(false) }
    var lyricsManualControl by remember { mutableStateOf(false) }
    LaunchedEffect(isLyricsViewExpanded, track.id) {
        lyricsImmersive = false
        lyricsManualControl = false
        if (isLyricsViewExpanded) {
            delay(3000L)
            if (!lyricsManualControl) lyricsImmersive = true
        }
    }
    // Per-track timing nudge for lyrics (+ = lyrics appear later), remembered between sessions.
    val lyricsTimingPrefs = remember(context) { context.getSharedPreferences("viaura_lyrics_timing", Context.MODE_PRIVATE) }
    var lyricsShiftMs by remember(track.path) { mutableLongStateOf(lyricsTimingPrefs.getLong("shift:" + track.path, 0L)) }
    fun updateLyricsShift(newValue: Long) {
        val clamped = newValue.coerceIn(-30_000L, 30_000L)
        lyricsShiftMs = clamped
        lyricsTimingPrefs.edit().apply {
            if (clamped == 0L) remove("shift:" + track.path) else putLong("shift:" + track.path, clamped)
        }.apply()
    }

    val lyricsTranslatePrefs = remember(context) {
        context.getSharedPreferences("viaura_lyrics_translation", Context.MODE_PRIVATE)
    }
    var targetLyricsLanguage by remember(context) {
        mutableStateOf(lyricsTranslatePrefs.getString("target_language", "hi") ?: "hi")
    }
    var lyricsTranslationReplaceMode by remember(context) {
        mutableStateOf(lyricsTranslatePrefs.getBoolean("replace_translation", false))
    }
    var lyricsLanguageSearch by remember { mutableStateOf("") }
    var isTranslatingLyrics by remember(track.id) { mutableStateOf(false) }
    var translatedLyricsModel by remember(track.id) { mutableStateOf<Lyrics?>(null) }
    var showTranslatedLyrics by remember(track.id) { mutableStateOf(false) }
    val displayedLyricsModel = if (showTranslatedLyrics) translatedLyricsModel ?: lyricsModel else lyricsModel

    LaunchedEffect(track.id, lyricsModel) {
        translatedLyricsModel = null
        showTranslatedLyrics = false
        isTranslatingLyrics = false
    }

    fun translateLyricsTo(language: String) {
        showLyricsLanguagePicker = false
        if (language == "__original") {
            showTranslatedLyrics = false
            return
        }
        targetLyricsLanguage = language
        lyricsTranslatePrefs.edit().putString("target_language", language).apply()
        val source = lyricsModel?.takeIf { it.isValid() }
            ?: com.example.utils.media.LyricsUtils.parseLyrics(track.lyricsText, LyricsSourceType.LOCAL).takeIf { it.isValid() }
        if (source == null) {
            statusToastMessage = "Find lyrics before translating"
            return
        }
        val translationSource = if (source.synced.isNullOrEmpty()) {
            com.example.utils.media.LyricsUtils.estimateTimingForPlainLyrics(source, durationMs) ?: source
        } else source
        scope.launch {
            isTranslatingLyrics = true
            try {
                val result = LyricsTranslationService.translate(translationSource, language, track.path)
                translatedLyricsModel = result
                showTranslatedLyrics = result != null
                statusToastMessage = if (result != null) {
                    "Lyrics translated to ${LyricsTranslationService.languages.firstOrNull { it.first == language }?.second ?: language}"
                } else {
                    "Translation unavailable. Check your internet connection."
                }
            } finally {
                isTranslatingLyrics = false
            }
        }
    }

    var artworkBitmap by remember(track.id, track.path, track.thumbnailUrl) {
        val cacheKey = if (!track.thumbnailUrl.isNullOrBlank()) {
            "remote_audio_${track.thumbnailUrl}_1000"
        } else {
            "audio_${track.id}_${track.path}_1000"
        }
        val cached = com.example.ui.components.ThumbnailCache.memoryCache.get(cacheKey)
        mutableStateOf<android.graphics.Bitmap?>(cached)
    }

    LaunchedEffect(track.id, track.path, track.thumbnailUrl) {
        if (artworkBitmap == null) {
            val loaded = if (!track.thumbnailUrl.isNullOrBlank()) {
                com.example.ui.components.loadRemoteAudioThumbnail(
                    url = track.thumbnailUrl,
                    targetSizePx = 1000
                )
            } else {
                com.example.ui.components.loadAudioThumbnail(
                    context = context,
                    audioId = track.id,
                    uri = track.uri,
                    path = track.path,
                    targetSizePx = 1000
                )
            }
            artworkBitmap = loaded
        }
    }

    // Calculate duration and elapsed string
    val effectivePosition = if (isSeeking) {
        (seekFraction * durationMs.coerceAtLeast(1L)).toLong()
    } else {
        positionMs
    }

    // Thumbnail-driven background: top colour from the artwork, a different deeper colour at the bottom.
    val artColors = remember(artworkBitmap) { artworkBitmap?.let { extractArtworkColors(it) } }
    val artTop by animateColorAsState(artColors?.top ?: AccentSkyBlue, tween(700), label = "art_top")
    val artAlpha by animateFloatAsState(if (artColors != null) 1f else 0f, tween(700), label = "art_alpha")
    // The thumbnail tint covers only the part above the seek bar and melts away towards it; everything
    // below the seek bar keeps the system dark / light background.
    var artRootTopPx by remember { mutableFloatStateOf(0f) }
    var artScrubTopPx by remember { mutableFloatStateOf(0f) }
    val artFadeEnd = if (!isLandscapeLayout && artScrubTopPx > artRootTopPx + 1f) artScrubTopPx - artRootTopPx else 1400f
    val artBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to artTop.copy(alpha = (if (isDark) 0.62f else 0.50f) * artAlpha),
            0.45f to artTop.copy(alpha = (if (isDark) 0.30f else 0.24f) * artAlpha),
            0.80f to artTop.copy(alpha = (if (isDark) 0.09f else 0.07f) * artAlpha),
            1f to artTop.copy(alpha = 0f),
        ),
        startY = 0f,
        endY = artFadeEnd,
    )

    val alphaFactor = (glassBlurTransparency / 100f).coerceIn(0.15f, 1.0f)
    val screenBg = if (isDark) {
        if (alphaFactor >= 0.99f) Neutral.D090D16
        else Neutral.D090D16.copy(alpha = (0.75f + 0.25f * alphaFactor).coerceIn(0.6f, 1.0f))
    } else {
        // Same as the Home screen: ice tone when Light is chosen by hand, white when Theme = System (phone in light mode)
        val lightBase = if (com.example.ui.components.ViauraSurfaceStyle.systemMode) Neutral.F8FAFC else Neutral.DAE3E9
        if (alphaFactor >= 0.99f) lightBase
        else lightBase.copy(alpha = (0.80f + 0.20f * alphaFactor).coerceIn(0.7f, 1.0f))
    }

    // Pulse animation for active A-B loop
    val infiniteTransition = rememberInfiniteTransition(label = "ab_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Shared by all bottom sheets below (they now sit outside the padded player content).
    val floatingSheetBg = glassPanelColor(isDark, LocalPanelTransparency.current)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { artRootTopPx = it.positionInWindow().y }
            .background(screenBg)
            .background(artBrush)
    ) {
      // The player content keeps the system-bar padding; the sheets below are NOT inside it, so they start at the real
      // screen bottom instead of floating above the navigation bar.
      Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // Transparent input shield: the player stays visually transparent, but taps outside
        // the actual player controls must never reach the screen underneath it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        )

        // The player is built from four sections so the same UI works in portrait (one column) and landscape (two halves).
        val topBarSection: @Composable ColumnScope.() -> Unit = {
            // TOP BAR: Down Arrow + "Now Playing" + Notification Icon + Info
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Collapse Button (Down Arrow)
                    Box(
                        modifier = Modifier
                            .testTag("audio_player_collapse_button")
                            .size(42.dp)
                            .liquidGlassOr(CircleShape, isDark, elevation = 6.dp) {
                                this
                                    .clip(CircleShape)
                                    .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.7f) else Neutral.E2E8F0.copy(alpha = 0.85f))
                                    .border(1.dp, borderColor, CircleShape)
                            }
                            .clickable(onClick = onCollapse),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            drawableRes = R.drawable.viaura_leave,
                            contentDescription = "Collapse Player",
                            tint = primaryText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Title Header
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NOW PLAYING",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = AccentSkyBlue
                            )
                            if (isABLoopActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentSkyBlue)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "A-B LOOP",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        if (queue.isNotEmpty()) {
                            Text(
                                text = "Track ${currentIndex + 1} of ${queue.size}",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                        if (sleepTimerActive) {
                            Text(
                                text = "Sleep in ${com.example.util.SleepTimerManager.formatCountdown(sleepTimerRemainingMs)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentSkyBlue
                            )
                        }
                    }

                    // Right Side Actions: Notification Permission toggle + Info Button
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                            Box(
                                modifier = Modifier
                                    .testTag("audio_player_notif_perm_button")
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AccentSkyBlue.copy(alpha = 0.15f))
                                    .border(1.dp, AccentSkyBlue.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = "Enable Notification",
                                    tint = AccentSkyBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Info Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_info_button")
                                .size(42.dp)
                                .liquidGlassOr(CircleShape, isDark, elevation = 6.dp) {
                                    this
                                        .clip(CircleShape)
                                        .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.7f) else Neutral.E2E8F0.copy(alpha = 0.85f))
                                        .border(1.dp, borderColor, CircleShape)
                                }
                                .clickable { showInfoSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Track Info",
                                tint = primaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Notification Permission Request Prompt Banner (if not granted on Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm && showNotifPromptBanner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentSkyBlue.copy(alpha = 0.12f))
                            .border(1.dp, AccentSkyBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = AccentSkyBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Show player in notification panel",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = primaryText
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentSkyBlue)
                                    .clickable {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Enable",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { showNotifPromptBanner = false },
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Dismiss",
                                    tint = secondaryText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val centerSection: @Composable ColumnScope.() -> Unit = {
            Spacer(modifier = Modifier.height(4.dp))

            // CENTER CONTAINER: Switch between Artwork and Expanded Lyrics View
            // Supports swipe-up on artwork to show lyrics, swipe-down on lyrics to show artwork
            AnimatedContent(
                targetState = isLyricsViewExpanded,
                modifier = Modifier
                    .then(if (isLyricsViewExpanded || isLandscapeLayout) Modifier.weight(1f) else Modifier)
                    .fillMaxWidth(),
                transitionSpec = {
                    if (targetState) {
                        (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { it / 4 })
                            .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { -it / 4 })
                    } else {
                        (fadeIn(animationSpec = tween(280)) + slideInVertically(animationSpec = tween(280)) { -it / 4 })
                            .togetherWith(fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200)) { it / 4 })
                    }
                },
                label = "CenterPlayerContent"
            ) { showLyrics ->
                if (!showLyrics) {
                    // ARTWORK VIEW
                    Box(
                        modifier = Modifier
                            .then(
                                if (isLandscapeLayout) Modifier.fillMaxSize().padding(vertical = 8.dp)
                                else Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 300.dp)
                            )
                            .padding(horizontal = 16.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -25) {
                                        isLyricsViewExpanded = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .sizeIn(
                                    maxWidth = if (isLandscapeLayout) 420.dp else 290.dp,
                                    maxHeight = if (isLandscapeLayout) 420.dp else 290.dp
                                )
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    ambientColor = if (isDark) AccentSkyBlue.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.15f),
                                    spotColor = if (isDark) AccentSkyBlue.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.20f)
                                )
                                .clip(RoundedCornerShape(32.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            if (isDark) Neutral.D1E293B else Neutral.E2E8F0,
                                            if (isDark) Neutral.D0F172A else Neutral.CBD5E1
                                        )
                                    )
                                )
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            if (isABLoopActive) AccentSkyBlue else AccentSkyBlue.copy(alpha = 0.6f),
                                            borderColor
                                        )
                                    ),
                                    RoundedCornerShape(32.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = artworkBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.45f)
                                                )
                                            )
                                        )
                                )
                            } else {
                                // Disc Glow Effect Fallback
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    AccentSkyBlue.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(AccentGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StyledIcon(
                                            imageVector = Icons.Filled.Headphones,
                                            contentDescription = "Music Artwork",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = track.format,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentSkyBlue,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Channel badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Neutral.D0F172A.copy(alpha = 0.85f) else Neutral.FFFFFF.copy(alpha = 0.9f))
                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = track.audioType,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText
                                )
                            }

                            // Swipe up hint pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.50f))
                                    .clickable { isLyricsViewExpanded = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StyledIcon(
                                        drawableRes = R.drawable.viaura_lyrice,
                                        contentDescription = null,
                                        tint = if (track.hasLyrics) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (track.hasLyrics) "Lyrics • Swipe Up" else "Lyrics • Swipe Up",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            // Rotating refresh/loading animation right in the center of the thumbnail (over album art)
                            if (isBuffering) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ViauraExpressiveLoadingIndicator(
                                        size = 72.dp,
                                        isDark = true
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // EXPANDED LYRICS VIEW (Above Seek Bar)
                    ExpandedLyricsContainer(
                        track = track,
                        positionMs = positionMs,
                        isPlaying = isPlaying,
                        playbackSpeed = playbackSpeed,
                        replaceWithTranslation = lyricsTranslationReplaceMode,
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        onClose = { isLyricsViewExpanded = false },
                        onAddLyrics = { showLyricsFilePicker = true },
                        onSeekTo = { AudioPlaybackManager.seekTo(it) },
                        isSearching = isFetchingLyrics,
                        onSearchOnline = { AudioPlaybackManager.searchLyricsOnline(context) },
                        lyricsModel = displayedLyricsModel,
                        activeProvider = activeLyricsProvider,
                        sourceType = lyricsSourceType,
                        onPickProvider = { showLyricsProviderPicker = true },
                        onTranslate = { showLyricsLanguagePicker = true },
                        isTranslating = isTranslatingLyrics,
                        isTranslated = showTranslatedLyrics,
                        immersive = lyricsImmersive,
                        shiftMs = lyricsShiftMs,
                        onShiftChange = { updateLyricsShift(it) },
                        onBackgroundTap = {
                            lyricsManualControl = true
                            lyricsImmersive = !lyricsImmersive
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount > 25) {
                                        isLyricsViewExpanded = false
                                    }
                                }
                            }
                    )
                }
            }

        }

        val infoSection: @Composable ColumnScope.() -> Unit = {
            // Title / artist / share / playlist / A-B controls: hidden while the lyrics are full-screen.
            AnimatedVisibility(
                visible = !(isLyricsViewExpanded && lyricsImmersive && !isLandscapeLayout),
                enter = fadeIn(tween(240)) + expandVertically(tween(280)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(260)),
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            // TRACK METADATA (Title, Artist, and Badges + AddToPlaylist row)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.title.uppercase(Locale.getDefault()),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (track.artist.isNotBlank()) track.artist else "Unknown Artist",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryText,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // METADATA ROW: Track X/Y | Speed | A-B Loop Badge ... [Right] Add to Playlist Icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Badges Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (queue.isNotEmpty()) {
                            Text(
                                text = "Track ${currentIndex + 1}/${queue.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText
                            )

                            Text(
                                text = "|",
                                fontSize = 12.sp,
                                color = secondaryText.copy(alpha = 0.4f)
                            )
                        }

                        // Playback Speed pill button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showSpeedSheet = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_speed,
                                contentDescription = "Playback Speed",
                                tint = secondaryText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2f", playbackSpeed)}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText
                            )
                        }

                        Text(
                            text = "|",
                            fontSize = 12.sp,
                            color = secondaryText.copy(alpha = 0.4f)
                        )

                        // A-B Loop Badge button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showABControlsBar = !showABControlsBar }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.viaura_ab_loop),
                                contentDescription = "A-B Loop",
                                tint = if (isABLoopActive) AccentSkyBlue else secondaryText,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isABLoopActive) "A-B (ON)" else if (loopPointA != null) "A-B (SET)" else "(A B)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isABLoopActive) AccentSkyBlue else secondaryText
                            )
                        }
                    }

                    // Right Action Buttons: Share + Add To Playlist
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favourite (heart): puts the track in / takes it out of the Favourites folder of the playlist section
                        var favVersion by remember { mutableStateOf(0) }
                        val isFavourite = remember(track.path, favVersion) {
                            com.example.util.FavouritesFolder.isFavourite(
                                context, com.example.util.PlaylistManager.getPlaylists(context), true, track.path
                            )
                        }
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_favourite_button")
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    // keep the title / artwork of an online track so the Favourites folder lists it properly
                                    if (track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)) {
                                        try { com.example.util.OnlineTrackMetadataCache.saveTracks(context, listOf(track)) } catch (_: Throwable) {}
                                    }
                                    val now = com.example.util.FavouritesFolder.toggleFromPlayer(
                                        context, com.example.util.PlaylistManager.getPlaylists(context), true, track.path
                                    )
                                    favVersion++
                                    statusToastMessage = if (now) "Added to Favourites" else "Removed from Favourites"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_favourite_heart,
                                contentDescription = if (isFavourite) "Remove from Favourites" else "Add to Favourites",
                                tint = if (isFavourite) Color(0xFFFF3366) else secondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Share Action Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_share_button")
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)) {
                                        val onlineUrl = track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                                        val shareText = "Listen to \"${track.title}\" by ${track.artist}: $onlineUrl"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            putExtra(Intent.EXTRA_SUBJECT, track.title)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                                    } else {
                                        com.example.util.AudioFileManager.shareAudio(context, listOf(track))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Add To Playlist Action Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_add_to_playlist_button")
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { showAddToPlaylistDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_playlist_audio,
                                contentDescription = "Add to Playlist",
                                tint = secondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // A-B LOOP QUICK CONTROL BAR (Expandable / Active)
            AnimatedVisibility(
                visible = showABControlsBar || loopPointA != null || isABLoopActive,
                enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ),
                exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.90f) else Neutral.F1F5F9.copy(alpha = 0.95f))
                        .border(
                            1.dp,
                            if (isABLoopActive) AccentSkyBlue.copy(alpha = 0.6f) else borderColor,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Point A Control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (loopPointA != null) AccentSkyBlue.copy(alpha = 0.25f)
                                        else if (isDark) Neutral.D0F172A
                                        else Neutral.E2E8F0
                                    )
                                    .border(
                                        1.dp,
                                        if (loopPointA != null) AccentSkyBlue else borderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        AudioPlaybackManager.setLoopPointA(effectivePosition)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (loopPointA != null) "A: ${formatAudioTime(loopPointA!!)}" else "Set A",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (loopPointA != null) AccentSkyBlue else primaryText
                                )
                            }

                            if (loopPointA != null) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { AudioPlaybackManager.adjustLoopPointA(-1000L) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-1s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { AudioPlaybackManager.adjustLoopPointA(1000L) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+1s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                                }
                            }
                        }

                        // Loop Center Status / Quick Jump
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isABLoopActive) {
                                        AudioPlaybackManager.jumpToPointA()
                                    } else {
                                        showABLoopSheet = true
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            if (isABLoopActive) {
                                val loopDur = ((loopPointB ?: 0L) - (loopPointA ?: 0L)).coerceAtLeast(0L)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StyledIcon(
                                        imageVector = Icons.Default.Loop,
                                        contentDescription = null,
                                        tint = AccentSkyBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${loopDur / 1000}s Loop",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentSkyBlue
                                    )
                                }
                            } else {
                                Text(
                                    text = if (loopPointA != null) "Tap [Set B]" else "Set Loop Points",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = secondaryText
                                )
                            }
                        }

                        // Point B Control
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (loopPointB != null) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { AudioPlaybackManager.adjustLoopPointB(-1000L) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("-1s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { AudioPlaybackManager.adjustLoopPointB(1000L) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+1s", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (loopPointB != null) AccentSkyBlue.copy(alpha = 0.25f)
                                        else if (isDark) Neutral.D0F172A
                                        else Neutral.E2E8F0
                                    )
                                    .border(
                                        1.dp,
                                        if (loopPointB != null) AccentSkyBlue else borderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        AudioPlaybackManager.setLoopPointB(effectivePosition)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (loopPointB != null) "B: ${formatAudioTime(loopPointB!!)}" else "Set B",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (loopPointB != null) AccentSkyBlue else primaryText
                                )
                            }

                            // Clear Loop button
                            if (loopPointA != null || loopPointB != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Neutral.D0F172A else Neutral.CBD5E1)
                                        .clickable {
                                            AudioPlaybackManager.clearABLoop()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    StyledIcon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear AB Loop",
                                        tint = secondaryText,
                                        modifier = Modifier.size(15.dp)
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

        val controlsSection: @Composable ColumnScope.() -> Unit = {
            Spacer(modifier = Modifier.height(6.dp))

            // PROGRESS BAR & TIME LABELS WITH TOUCH-AND-HOLD EXPANSION
            InteractiveAudioScrubber(
                currentPositionMs = positionMs,
                durationMs = durationMs,
                onSeekTo = { targetMs ->
                    AudioPlaybackManager.seekTo(targetMs)
                },
                isDark = isDark,
                activeTrackColor = AccentSkyBlue,
                inactiveTrackColor = if (isDark) Neutral.D334155 else Neutral.E2E8F0,
                textColor = secondaryText,
                loopPointA = loopPointA,
                loopPointB = loopPointB,
                isABLoopActive = isABLoopActive,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { artScrubTopPx = it.positionInWindow().y }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // -------------------------------------------------------------
            // 1. MAIN PLAYBACK CONTROLS: Prev, -10s, Big Play/Pause, +10s, Next
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Track
                Box(
                    modifier = Modifier
                        .testTag("audio_player_prev_button")
                        .size(48.dp)
                        .liquidGlassOr(CircleShape, isDark, elevation = 6.dp) { clip(CircleShape) }
                        .clickable { AudioPlaybackManager.skipPrevious(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.viaura_arrow_left_solid),
                        contentDescription = "Previous Track",
                        tint = primaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Rewind 10s
                Box(
                    modifier = Modifier
                        .testTag("audio_player_replay_10_button")
                        .size(48.dp)
                        .liquidGlassOr(CircleShape, isDark, elevation = 6.dp) { clip(CircleShape) }
                        .clickable { AudioPlaybackManager.seekRelative(-10_000L) },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 Seconds",
                        tint = primaryText,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Primary Play / Pause Button (Large Prominent Accent Circle)
                Box(
                    modifier = Modifier
                        .testTag("audio_player_play_pause_button")
                        .size(68.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = AccentSkyBlue.copy(alpha = 0.55f),
                            spotColor = AccentSkyBlue.copy(alpha = 0.7f)
                        )
                        .clip(CircleShape)
                        .background(AccentGradient)
                        .then(
                            if (LocalLiquidGlass.current) {
                                Modifier
                                    .background(liquidGlassSheenBrush(true))
                                    .border(1.2.dp, liquidGlassBorderBrush(true), CircleShape)
                            } else Modifier
                        )
                        .clickable(enabled = !isBuffering) { AudioPlaybackManager.togglePlayPause(context) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        ViauraExpressiveLoadingIndicator(
                            size = 38.dp,
                            isDark = true,
                            transparentBg = true
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.viaura_pause_solid else R.drawable.viaura_play_solid
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Forward 10s
                Box(
                    modifier = Modifier
                        .testTag("audio_player_forward_10_button")
                        .size(48.dp)
                        .liquidGlassOr(CircleShape, isDark, elevation = 6.dp) { clip(CircleShape) }
                        .clickable { AudioPlaybackManager.seekRelative(10_000L) },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10 Seconds",
                        tint = primaryText,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Next Track
                Box(
                    modifier = Modifier
                        .testTag("audio_player_next_button")
                        .size(48.dp)
                        .liquidGlassOr(CircleShape, isDark, elevation = 6.dp) { clip(CircleShape) }
                        .clickable { AudioPlaybackManager.skipNext(context) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.viaura_arrow_right_solid),
                        contentDescription = "Next Track",
                        tint = primaryText,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -------------------------------------------------------------
            // 2. BOTTOM ISLANDS ROW:
            //    - Left Island: Equalizer circular container (52dp)
            //    - Center Island: Capsule container with 5 buttons
            //    - Right Island: Queue / Playlist circular container (52dp)
            // -------------------------------------------------------------
            val islandBg = if (isDark) {
                Neutral.D1E293B.copy(alpha = (0.70f + 0.25f * alphaFactor).coerceIn(0.60f, 0.95f))
            } else {
                Neutral.E2E8F0.copy(alpha = (0.80f + 0.18f * alphaFactor).coerceIn(0.70f, 0.98f))
            }
            val islandBorder = if (isDark) AccentSkyBlue.copy(alpha = 0.20f) else AccentSkyBlueStrong.copy(alpha = 0.18f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Island 1 (Left): Equalizer Button in a 52dp circular container
                Box(
                    modifier = Modifier
                        .testTag("audio_player_eq_button")
                        .size(52.dp)
                        .liquidGlassOr(CircleShape, isDark, elevation = 8.dp) {
                            this
                                .clip(CircleShape)
                                .background(islandBg)
                                .border(
                                    1.dp,
                                    if (equalizerEnabled) AccentSkyBlue.copy(alpha = 0.65f) else islandBorder,
                                    CircleShape
                                )
                        }
                        .clickable { showEqualizerSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_equalizer,
                        contentDescription = "Equalizer",
                        tint = if (equalizerEnabled) AccentSkyBlue else primaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Island 2 (Center): Capsule Island with Shuffle, Repeat, AB Loop, Lyrics, Headphone
                Box(
                    modifier = Modifier
                        .liquidGlassOr(RoundedCornerShape(26.dp), isDark, elevation = 8.dp) {
                            this
                                .clip(RoundedCornerShape(26.dp))
                                .background(islandBg)
                                .border(1.dp, islandBorder, RoundedCornerShape(26.dp))
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_shuffle_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isShuffle) AccentSkyBlue.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable { AudioPlaybackManager.toggleShuffle() },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) AccentSkyBlue else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Repeat Mode Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_repeat_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (repeatMode != AudioRepeatMode.OFF) AccentSkyBlue.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable { AudioPlaybackManager.toggleRepeat() },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = if (repeatMode == AudioRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = repeatMode.label,
                                tint = if (repeatMode != AudioRepeatMode.OFF) AccentSkyBlue else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // A-B Loop Control Button (Vector ic_ab_loop)
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_ab_loop_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isABLoopActive) AccentSkyBlue.copy(alpha = 0.28f)
                                    else if (loopPointA != null) AccentSkyBlue.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isABLoopActive || loopPointA != null) AccentSkyBlue.copy(alpha = 0.65f) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    AudioPlaybackManager.toggleABLoop(effectivePosition)
                                    showABControlsBar = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.viaura_ab_loop),
                                contentDescription = "A-B Loop",
                                tint = if (isABLoopActive || loopPointA != null) AccentSkyBlue else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Lyrics Viewer Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_lyrics_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLyricsViewExpanded) AccentSkyBlue.copy(alpha = 0.28f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isLyricsViewExpanded) AccentSkyBlue.copy(alpha = 0.65f) else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    isLyricsViewExpanded = !isLyricsViewExpanded
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_lyrice,
                                contentDescription = "Lyrics",
                                tint = if (isLyricsViewExpanded || track.hasLyrics) AccentSkyBlue else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Headphone Spatial Button
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_headphone_button")
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isHeadphoneSurroundActive) AccentSkyBlue.copy(alpha = 0.22f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    isHeadphoneSurroundActive = !isHeadphoneSurroundActive
                                    statusToastMessage = if (isHeadphoneSurroundActive) "Headphone Spatial Mode Enabled" else "Headphone Spatial Mode Disabled"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Headphones,
                                contentDescription = "Headphone Surround",
                                tint = if (isHeadphoneSurroundActive) AccentSkyBlue else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Island 3 (Right): Queue / Playlist Button in a 52dp circular container
                Box(
                    modifier = Modifier
                        .testTag("audio_player_queue_button")
                        .size(52.dp)
                        .liquidGlassOr(CircleShape, isDark, elevation = 8.dp) {
                            this
                                .clip(CircleShape)
                                .background(islandBg)
                                .border(1.dp, islandBorder, CircleShape)
                        }
                        .clickable { showQueueSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_audio_track,
                        contentDescription = "Queue",
                        tint = primaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (isLandscapeLayout) {
            // LANDSCAPE: left half = artwork (swipe up for lyrics), right half = title, seek bar and all controls.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .displayCutoutPadding()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    centerSection()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        topBarSection()
                        Spacer(modifier = Modifier.height(6.dp))
                        infoSection()
                        controlsSection()
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                topBarSection()
                centerSection()
                Spacer(modifier = Modifier.height(10.dp))
                infoSection()
                controlsSection()
            }
        }

        // ================= BOTTOM SHEETS & DIALOGS (TRANSLUCENT LIQUID GLASS) ================= //

        // On-device selection + no-key online translation fallback, matching MpvRx's language picker.
        if (showLyricsLanguagePicker) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsLanguagePicker = false },
                containerColor = floatingSheetBg,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null,
            ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
                // Liquid Glass: blurred app behind the sheet (no-op when the style is OFF)
                LiquidDialogBlur()
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.72f)
                        .liquidGlassEdge(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    Text("Translate lyrics", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("No API key required • Internet connection needed", fontSize = 12.sp, color = secondaryText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(false to "Dual line", true to "Replace original").forEach { (replaceMode, label) ->
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(if (lyricsTranslationReplaceMode == replaceMode) AccentSkyBlue.copy(alpha = 0.18f) else Color.Transparent)
                                    .border(1.dp, if (lyricsTranslationReplaceMode == replaceMode) AccentSkyBlue else borderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        lyricsTranslationReplaceMode = replaceMode
                                        lyricsTranslatePrefs.edit().putBoolean("replace_translation", replaceMode).apply()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, color = if (lyricsTranslationReplaceMode == replaceMode) AccentSkyBlue else primaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    BasicTextField(
                        value = lyricsLanguageSearch,
                        onValueChange = { lyricsLanguageSearch = it },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = com.example.ui.theme.LocalAppFontFamily.current, color = primaryText, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF202B38) else Color(0xFFE8EDF1))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (lyricsLanguageSearch.isBlank()) Text("Search language…", color = secondaryText, fontSize = 14.sp)
                                innerTextField()
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (showTranslatedLyrics) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { translateLyricsTo("__original") }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Show original lyrics", color = primaryText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            StyledIcon(imageVector = Icons.Outlined.Check, contentDescription = null, tint = AccentSkyBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                    val filteredLyricsLanguages = remember(lyricsLanguageSearch) {
                        LyricsTranslationService.languages.filter { (code, label) ->
                            code.contains(lyricsLanguageSearch, ignoreCase = true) || label.contains(lyricsLanguageSearch, ignoreCase = true)
                        }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        itemsIndexed(filteredLyricsLanguages, key = { _, item -> item.first }) { _, language ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (targetLyricsLanguage == language.first && showTranslatedLyrics) AccentSkyBlue.copy(alpha = 0.13f) else Color.Transparent)
                                    .clickable { translateLyricsTo(language.first) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(language.second, color = primaryText, fontWeight = if (targetLyricsLanguage == language.first) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                                if (targetLyricsLanguage == language.first && showTranslatedLyrics) {
                                    StyledIcon(imageVector = Icons.Outlined.Check, contentDescription = "Selected", tint = AccentSkyBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

      }

        // 0. ADD TO PLAYLIST DIALOG
        if (showAddToPlaylistDialog) {
            val audioVideoItem = remember(track) {
                VideoItem(
                    id = track.id,
                    displayName = track.title,
                    path = track.path,
                    uri = track.uri,
                    durationMs = track.durationMs,
                    sizeBytes = track.sizeBytes,
                    dateModified = track.dateModified
                )
            }
            AddToPlaylistDialog(
                selectedVideos = listOf(audioVideoItem),
                isDark = isDark,
                glassBlurTransparency = glassBlurTransparency,
                onDismiss = { showAddToPlaylistDialog = false }
            )
        }

        // 1. SPEED PICKER SHEET
        if (showSpeedSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedSheet = false },
                containerColor = floatingSheetBg,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null
            ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
                // Liquid Glass: blurred app behind the sheet (no-op when the style is OFF)
                LiquidDialogBlur()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassEdge(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Playback Speed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                    speeds.forEach { spd ->
                        val isSelected = (playbackSpeed == spd)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentSkyBlue.copy(alpha = 0.18f) else Color.Transparent)
                                .clickable {
                                    AudioPlaybackManager.setPlaybackSpeed(spd)
                                    showSpeedSheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${spd}x ${if (spd == 1.0f) "(Normal)" else ""}",
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentSkyBlue else primaryText
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentSkyBlue)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 2. QUEUE LIST SHEET
        if (showQueueSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showQueueSheet = false
                },
                containerColor = floatingSheetBg,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null
            ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
                // Liquid Glass: blurred app behind the sheet (no-op when the style is OFF)
                LiquidDialogBlur()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassEdge(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                        .fillMaxHeight(0.75f)
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next • ${queue.size} ${if (queue.size == 1) "track" else "tracks"}",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText,
                            modifier = Modifier.weight(1f)
                        )
                        // Sleep Timer (works for offline + online audio) - sits at the right end of "Up Next"
                        Box(
                            modifier = Modifier
                                .testTag("audio_player_sleep_timer_button")
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable {
                                    // Close the "Up Next" track list first so only the sleep timer panel is visible
                                    showQueueSheet = false
                                    showSleepTimerSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_sleep,
                                contentDescription = "Sleep timer",
                                tint = if (sleepTimerActive) AccentSkyBlue else primaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val queueListState = rememberLazyListState()
                    var queueListCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                    var queueListHeightPx by remember { mutableFloatStateOf(0f) }
                    var dragPointerYInList by remember { mutableFloatStateOf(-1f) }
                    var dragGrabOffsetY by remember { mutableFloatStateOf(0f) }
                    var dragPointerId by remember { mutableStateOf<PointerId?>(null) }

                    var draggingIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                    var measuredItemHeightPx by remember { mutableFloatStateOf(0f) }
                    val density = LocalDensity.current

                    LaunchedEffect(draggingIndex) {
                        if (draggingIndex == null) return@LaunchedEffect
                        val edgeZone = with(density) { 86.dp.toPx() }
                        val maxScrollPerTick = with(density) { 7.dp.toPx() }
                        while (draggingIndex != null) {
                            val pointerY = dragPointerYInList
                            val listH = queueListHeightPx
                            if (pointerY >= 0f && listH > 0f) {
                                val scrollDelta = when {
                                    pointerY < edgeZone -> -maxScrollPerTick * ((edgeZone - pointerY) / edgeZone).coerceIn(0f, 1f)
                                    pointerY > (listH - edgeZone) -> maxScrollPerTick * ((pointerY - (listH - edgeZone)) / edgeZone).coerceIn(0f, 1f)
                                    else -> 0f
                                }
                                if (scrollDelta != 0f) {
                                    val consumed = queueListState.scrollBy(scrollDelta)
                                    if (consumed != 0f) dragOffsetY += consumed
                                }
                            }
                            kotlinx.coroutines.delay(24L)
                        }
                    }

                    val effectiveHeight = if (measuredItemHeightPx > 0f) measuredItemHeightPx else with(density) { 76.dp.toPx() }
                    val fromIndex = draggingIndex ?: -1
                    val hoverIndex = if (fromIndex != -1) {
                        val pointerY = dragPointerYInList
                        queueListState.layoutInfo.visibleItemsInfo
                            .filter { it.index in queue.indices }
                            .minByOrNull { item -> kotlin.math.abs(pointerY - (item.offset + item.size / 2f)) }
                            ?.index ?: fromIndex
                    } else -1

                    val currentDragPointerId by rememberUpdatedState(dragPointerId)
                    val currentDraggingIndex by rememberUpdatedState(draggingIndex)
                    val currentQueueSize by rememberUpdatedState(queue.size)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .onGloballyPositioned { coordinates ->
                                queueListCoordinates = coordinates
                                queueListHeightPx = coordinates.size.height.toFloat()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Final)
                                        val activeId = currentDragPointerId
                                        if (activeId != null) {
                                            val change = event.changes.firstOrNull { it.id == activeId }
                                            if (change != null && change.pressed) {
                                                dragPointerYInList = change.position.y
                                            } else {
                                                // Finger lifted, gesture cancelled or pointer lost: the drag always ends here and the
                                                // track is committed where it was dropped (target from the CURRENT list layout).
                                                if (change != null) dragPointerYInList = change.position.y
                                                val f = currentDraggingIndex ?: -1
                                                val pointerY = dragPointerYInList
                                                val t = queueListState.layoutInfo.visibleItemsInfo
                                                    .filter { it.index in 0 until currentQueueSize }
                                                    .minByOrNull { item -> kotlin.math.abs(pointerY - (item.offset + item.size / 2f)) }
                                                    ?.index ?: f
                                                if (f != -1 && t != -1 && f != t) AudioPlaybackManager.reorderQueue(f, t)
                                                dragPointerId = null
                                                draggingIndex = null
                                                dragOffsetY = 0f
                                                dragPointerYInList = -1f
                                                dragGrabOffsetY = 0f
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        LazyColumn(
                            state = queueListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                        itemsIndexed(queue, key = { _, item -> item.id }) { idx, item ->
                            val isCurrent = (idx == currentIndex)
                            val isBeingDragged = (draggingIndex == idx)
                            val isHighlighted = isBeingDragged || isCurrent
                            var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            // The gestures below live as long as the row; after a reorder the row sits at a NEW index,
                            // so they must read the newest index / state instead of the one from the first composition.
                            val currentIdx by rememberUpdatedState(idx)
                            val currentIsCurrent by rememberUpdatedState(isCurrent)
                            val swipeScope = rememberCoroutineScope()
                            val swipeOffsetX = remember(item.id) { Animatable(0f) }
                            val swipeThresholdPx = with(density) { 85.dp.toPx() }

                            val targetTranslationY = when {
                                isBeingDragged -> 0f
                                draggingIndex != null && fromIndex != -1 && hoverIndex != -1 -> {
                                    when {
                                        fromIndex < hoverIndex && idx in (fromIndex + 1)..hoverIndex -> -effectiveHeight
                                        fromIndex > hoverIndex && idx in hoverIndex until fromIndex -> effectiveHeight
                                        else -> 0f
                                    }
                                }
                                else -> 0f
                            }

                            val animatedTranslationY by animateFloatAsState(
                                targetValue = targetTranslationY,
                                // no drag running = the order is already final: jump to 0 (animating from the old shift made rows slide wrongly)
                                animationSpec = if (draggingIndex == null) androidx.compose.animation.core.snap() else spring(
                                    dampingRatio = 0.90f,
                                    stiffness = 800f
                                ),
                                label = "AudioQueueTranslationY_${item.id}"
                            )

                            val visualTranslationY = if (isBeingDragged) dragOffsetY else animatedTranslationY

                            val activeCardBg = if (isDark) AccentContainerDark.copy(alpha = 0.4f) else AccentContainerLight
                            val activeBorderColor = AccentSkyBlueStrong
                            val normalCardBg = if (isDark) Color(0x10FFFFFF) else Color(0x0A000000)
                            val normalBorderColor = if (isDark) Color(0x1AFFFFFF) else Color(0x14000000)
                            val draggedBorderGradient = Brush.linearGradient(listOf(AccentSkyBlue, com.example.ui.theme.AccentMid))
                            val shape = RoundedCornerShape(16.dp)

                            val dragBorderModifier = if (isBeingDragged) {
                                Modifier.border(2.dp, draggedBorderGradient, shape)
                            } else {
                                Modifier.border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) activeBorderColor else normalBorderColor,
                                    shape = shape
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { androidx.compose.ui.unit.IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                                    .zIndex(if (isBeingDragged) 99f else 1f)
                                    .pointerInput(item.id) {
                                        var totalSwipe = 0f
                                        // Horizontal-only detector: vertical movement is never consumed here, so the
                                        // LazyColumn scrolls smoothly even when the finger starts on a track row.
                                        detectHorizontalDragGestures(
                                            onDragStart = { totalSwipe = 0f },
                                            onHorizontalDrag = { change, dragAmount ->
                                                if (draggingIndex == null) {
                                                    totalSwipe += dragAmount
                                                    if (totalSwipe < 0f) {
                                                        change.consume()
                                                        swipeScope.launch {
                                                            swipeOffsetX.snapTo((swipeOffsetX.value + dragAmount).coerceAtMost(0f))
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                swipeScope.launch {
                                                    if (!currentIsCurrent && swipeOffsetX.value <= -swipeThresholdPx) {
                                                        swipeOffsetX.animateTo(-900f, tween(220, easing = FastOutSlowInEasing))
                                                        AudioPlaybackManager.removeFromQueue(currentIdx)
                                                    } else {
                                                        swipeOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                swipeScope.launch { swipeOffsetX.animateTo(0f, tween(150)) }
                                            }
                                        )
                                    }
                                    .alpha(if (isBeingDragged) 0f else 1f)
                                    .graphicsLayer {
                                        translationY = visualTranslationY
                                        if (isBeingDragged) {
                                            scaleX = 1.028f
                                            scaleY = 1.028f
                                            shadowElevation = 18.dp.toPx()
                                            this.shape = shape
                                            clip = false
                                        }
                                    }
                                    .onGloballyPositioned { coordinates ->
                                        itemCoordinates = coordinates
                                        if (draggingIndex == null && coordinates.size.height > 0) {
                                            measuredItemHeightPx = coordinates.size.height.toFloat() + with(density) { 8.dp.toPx() }
                                        }
                                    }
                                    .clip(shape)
                                    .background(if (isHighlighted) activeCardBg else normalCardBg)
                                    .then(dragBorderModifier)
                                    .clickable {
                                        if (draggingIndex == null) {
                                            AudioPlaybackManager.playTrack(context, item, queue)
                                            showQueueSheet = false
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Compact Thumbnail (60x60dp) with Track Number & Duration overlays
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                                ) {
                                    AudioTrackThumbnail(
                                        track = item,
                                        modifier = Modifier.fillMaxSize(),
                                        cornerRadius = 10.dp,
                                        fallbackIcon = Icons.Filled.MusicNote,
                                        fallbackIconSize = 22.dp
                                    )

                                    // Top-Left Track Number Overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .padding(horizontal = 2.5.dp, vertical = 0.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = Color.White,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Bottom-Right Duration Overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .padding(horizontal = 2.5.dp, vertical = 0.5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formatAudioTime(item.durationMs),
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 2. Middle Content: Song Title (with smooth marquee) & Artist Pill
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHighlighted) (if (isDark) Color.White else Neutral.D0F172A) else primaryText,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .basicMarquee(
                                                iterations = Int.MAX_VALUE,
                                                repeatDelayMillis = 1200,
                                                initialDelayMillis = 1000
                                            )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Artist Pill (replacing video resolution like "1080p")
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isHighlighted) AccentSkyBlueStrong.copy(alpha = 0.13f)
                                                    else if (isDark) Color(0x22FFFFFF)
                                                    else Color(0x14000000)
                                                )
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.artist.ifBlank { "Unknown Artist" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isHighlighted) AccentSkyBlueStrong else secondaryText,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip,
                                                modifier = Modifier.basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    repeatDelayMillis = 1500,
                                                    initialDelayMillis = 1200
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 3. Playing Badge (if active)
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDark) AccentContainerDark.copy(alpha = 0.6f) else AccentContainerLight)
                                            .border(1.dp, AccentSkyBlueStrong, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 9.dp, vertical = 3.5.dp)
                                    ) {
                                        Text(
                                            text = "Playing",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentSkyBlueStrong
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                // 4. Subtle Two-Line Drag Handle (=) matching Video Player style
                                Box(
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isBeingDragged) AccentSkyBlue.copy(alpha = 0.15f) else Color.Transparent)
                                        .onGloballyPositioned { handleCoordinates = it }
                                        .pointerInput(item.id) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val longPress = awaitLongPressOrCancellation(down.id)
                                                if (longPress != null) {
                                                    AppHaptics.perform(haptic)
                                                    draggingIndex = currentIdx
                                                    dragOffsetY = 0f
                                                    dragPointerId = down.id
                                                    handleCoordinates?.let { handle ->
                                                        queueListCoordinates?.let { lst -> dragPointerYInList = lst.localPositionOf(handle, longPress.position).y }
                                                        itemCoordinates?.let { itm -> dragGrabOffsetY = itm.localPositionOf(handle, longPress.position).y }
                                                    }
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                        if (!change.pressed) break
                                                        val delta = change.positionChange().y
                                                        if (delta != 0f) {
                                                            change.consume()
                                                            if (draggingIndex == currentIdx) {
                                                                dragOffsetY += delta
                                                                handleCoordinates?.let { handle -> queueListCoordinates?.let { lst -> dragPointerYInList = lst.localPositionOf(handle, change.position).y } }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 19.dp, height = 2.5.dp)
                                                .clip(RoundedCornerShape(1.2.dp))
                                                .background(if (isBeingDragged) AccentSkyBlue else if (isDark) Neutral.S64748B else Neutral.S94A3B8)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(width = 19.dp, height = 2.5.dp)
                                                .clip(RoundedCornerShape(1.2.dp))
                                                .background(if (isBeingDragged) AccentSkyBlue else if (isDark) Neutral.S64748B else Neutral.S94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                draggingIndex?.let { draggedIndex ->
                            queue.getOrNull(draggedIndex)?.let { dragged ->
                                val topPx = dragPointerYInList - dragGrabOffsetY
                                val ghostShape = RoundedCornerShape(16.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset { androidx.compose.ui.unit.IntOffset(0, topPx.roundToInt()) }
                                        .zIndex(100f)
                                        .shadow(18.dp, ghostShape)
                                        .clip(ghostShape)
                                        .background(if (isDark) AccentContainerDark.copy(alpha = 0.92f) else AccentContainerLight)
                                        .border(2.dp, Brush.linearGradient(listOf(AccentSkyBlue, com.example.ui.theme.AccentMid)), ghostShape)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp))) {
                                        AudioTrackThumbnail(track = dragged, modifier = Modifier.fillMaxSize(), cornerRadius = 10.dp, fallbackIcon = Icons.Filled.MusicNote, fallbackIconSize = 22.dp)
                                        Box(modifier = Modifier.align(Alignment.TopStart).padding(3.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 2.5.dp, vertical = 0.5.dp)) { Text("${draggedIndex + 1}", color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold) }
                                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 2.5.dp, vertical = 0.5.dp)) { Text(formatAudioTime(dragged.durationMs), color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.SemiBold) }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dragged.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Neutral.D0F172A, maxLines = 1, overflow = TextOverflow.Clip)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentSkyBlueStrong.copy(alpha = 0.13f)).padding(horizontal = 7.dp, vertical = 2.dp)) { Text(dragged.artist.ifBlank { "Unknown Artist" }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentSkyBlueStrong, maxLines = 1, overflow = TextOverflow.Clip) }
                                    }
                                    if (draggedIndex == currentIndex) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isDark) AccentContainerDark else AccentContainerLight).border(1.dp, AccentSkyBlueStrong, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 3.5.dp)) { Text("Playing", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentSkyBlueStrong) }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Column(modifier = Modifier.size(width = 38.dp, height = 44.dp), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(width = 19.dp, height = 2.5.dp).clip(RoundedCornerShape(1.2.dp)).background(AccentSkyBlue))
                                        Box(Modifier.size(width = 19.dp, height = 2.5.dp).clip(RoundedCornerShape(1.2.dp)).background(AccentSkyBlue))
                                    }
                                }
                            }

                    }
                }
            }
        }
        }

        // 3. TRACK INFO DETAILS SHEET
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                containerColor = floatingSheetBg,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null
            ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
                // Liquid Glass: blurred app behind the sheet (no-op when the style is OFF)
                LiquidDialogBlur()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassEdge(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Track Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(label = "Title", value = track.title, primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "Artist", value = track.artist, primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "Album", value = track.album, primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "Format", value = track.format, primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "Channels", value = track.audioType, primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "Duration", value = formatAudioTime(track.durationMs), primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "File Size", value = formatAudioFileSize(track.sizeBytes), primaryText = primaryText, secondaryText = secondaryText)
                    InfoRow(label = "File Path", value = track.path, primaryText = primaryText, secondaryText = secondaryText)

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 4. LYRICS SHEET (Fallback Modal Sheet with Import Action)
        if (showLyricsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLyricsSheet = false },
                containerColor = floatingSheetBg,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null
            ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
                // Liquid Glass: blurred app behind the sheet (no-op when the style is OFF)
                LiquidDialogBlur()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassEdge(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                        .fillMaxHeight(0.7f)
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Lyrics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                                    .clickable { showLyricsProviderPicker = true }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = activeLyricsProvider?.label ?: "Auto",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentSkyBlue
                                )
                            }
                        }

                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (lyricsModel?.isValid() == true || (track.hasLyrics && track.lyricsText.isNotBlank())) {
                        AudioLyricsContent(
                            lyrics = displayedLyricsModel,
                            fallbackText = track.lyricsText,
                            positionMs = positionMs,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            compact = false,
                            onSeekTo = { AudioPlaybackManager.seekTo(it) },
                            durationMs = durationMs,
                            isPlaying = isPlaying,
                            playbackSpeed = playbackSpeed,
                            replaceWithTranslation = lyricsTranslationReplaceMode,
                            timeOffsetMs = lyricsShiftMs,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                StyledIcon(
                                    drawableRes = R.drawable.viaura_lyrice,
                                    contentDescription = "No Lyrics",
                                    tint = secondaryText,
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFetchingLyrics) "Searching lyrics online…" else "No lyrics found for this track",
                                    fontSize = 14.sp,
                                    color = secondaryText,
                                )
                                if (isFetchingLyrics) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LyricsPlaceholderLines(compact = false, modifier = Modifier.fillMaxWidth())
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (!isFetchingLyrics) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AccentSkyBlue)
                                            .clickable { AudioPlaybackManager.searchLyricsOnline(context) }
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    ) {
                                        Text("Search online", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                                        .clickable {
                                            showLyricsSheet = false
                                            showLyricsFilePicker = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text("Import Lyrics (.lrc, .txt)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        LyricsProviderPickerDialog(
            visible = showLyricsProviderPicker,
            activeProvider = activeLyricsProvider,
            sourceType = lyricsSourceType,
            hasLocalLyrics = currentTrack?.let { it.hasLyrics && it.lyricsText.isNotBlank() } == true,
            providers = AudioPlaybackManager.lyricsProviders(),
            onDismiss = { showLyricsProviderPicker = false },
            onChooseLocal = {
                AudioPlaybackManager.selectLyricsSource(LyricsSourceType.LOCAL)
                showLyricsProviderPicker = false
            },
            onChooseProvider = { provider ->
                AudioPlaybackManager.searchLyricsWithProvider(context, provider)
                showLyricsProviderPicker = false
            },
        )

        // 4b. LYRICS FILE EXPLORER PICKER
        if (showLyricsFilePicker) {
            SubtitleFileTreeDialog(
                initialVideoPath = track.path,
                isLandscape = false,
                glassBlurTransparency = glassBlurTransparency,
                appPanelGlass = true,
                onSubtitleSelected = { selectedFile ->
                    showLyricsFilePicker = false
                    scope.launch(Dispatchers.IO) {
                        try {
                            val text = selectedFile.readText(Charsets.UTF_8)
                            if (text.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    AudioPlaybackManager.setLyricsForCurrentTrack(text)
                                    statusToastMessage = "Lyrics imported from ${selectedFile.name}"
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                statusToastMessage = "Error reading lyrics: ${e.message}"
                            }
                        }
                    }
                },
                onOpenSystemPicker = {
                    showLyricsFilePicker = false
                    try {
                        lyricsDocLauncher.launch(arrayOf("*/*", "text/*", "application/octet-stream"))
                    } catch (_: Exception) {}
                },
                onDismiss = { showLyricsFilePicker = false }
            )
        }

        // 5. A-B LOOP MODAL SHEET (Full Loop Fine-Tuning & Editor)
        if (showABLoopSheet) {
            ModalBottomSheet(
                onDismissRequest = { showABLoopSheet = false },
                containerColor = floatingSheetBg,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = null
            ) {
        com.example.ui.components.EdgeToEdgeSheetWindowEffect()
                // Liquid Glass: blurred app behind the sheet (no-op when the style is OFF)
                LiquidDialogBlur()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlassEdge(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), isDark)
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "A-B Segment Loop",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        if (isABLoopActive || loopPointA != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .clickable {
                                        AudioPlaybackManager.clearABLoop()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Clear Loop",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // POINT A ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Neutral.D1E293B else Neutral.F1F5F9)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "POINT A (START)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                            Text(
                                text = if (loopPointA != null) formatAudioTime(loopPointA!!) else "Not Set",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (loopPointA != null) AccentSkyBlue else primaryText
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (loopPointA != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Neutral.D0F172A else Neutral.E2E8F0)
                                        .clickable { AudioPlaybackManager.adjustLoopPointA(-1000L) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("-1s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Neutral.D0F172A else Neutral.E2E8F0)
                                        .clickable { AudioPlaybackManager.adjustLoopPointA(1000L) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("+1s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentSkyBlue)
                                    .clickable {
                                        AudioPlaybackManager.setLoopPointA(effectivePosition)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (loopPointA != null) "Update to Now" else "Set Here",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // POINT B ROW
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Neutral.D1E293B else Neutral.F1F5F9)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "POINT B (END)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                            Text(
                                text = if (loopPointB != null) formatAudioTime(loopPointB!!) else "Not Set",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (loopPointB != null) AccentSkyBlue else primaryText
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (loopPointB != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Neutral.D0F172A else Neutral.E2E8F0)
                                        .clickable { AudioPlaybackManager.adjustLoopPointB(-1000L) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("-1s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isDark) Neutral.D0F172A else Neutral.E2E8F0)
                                        .clickable { AudioPlaybackManager.adjustLoopPointB(1000L) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("+1s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryText)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentSkyBlue)
                                    .clickable {
                                        AudioPlaybackManager.setLoopPointB(effectivePosition)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (loopPointB != null) "Update to Now" else "Set Here",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isABLoopActive) {
                        val loopDurationMs = ((loopPointB ?: 0L) - (loopPointA ?: 0L)).coerceAtLeast(0L)
                        Text(
                            text = "Loop duration: ${formatAudioTime(loopDurationMs)} (${loopDurationMs / 1000}s). Audio will repeatedly play between ${formatAudioTime(loopPointA ?: 0L)} and ${formatAudioTime(loopPointB ?: 0L)}.",
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    } else {
                        Text(
                            text = "Set Point A where repeat starts, and Point B where repeat ends. The player will continuously loop between both points.",
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 6. EQUALIZER MODAL SHEET (Full Equalizer Controls)
        if (showEqualizerSheet) {
            AudioEqualizerPanel(
                isLandscape = isLandscapeLayout,
                audioPlayerSide = true,
                glassBlurTransparency = glassBlurTransparency,
                equalizerEnabled = equalizerEnabled,
                onToggleEqualizer = { equalizerEnabled = it },
                eq60Hz = eq60Hz,
                onEq60HzChange = { eq60Hz = it },
                eq230Hz = eq230Hz,
                onEq230HzChange = { eq230Hz = it },
                eq910Hz = eq910Hz,
                onEq910HzChange = { eq910Hz = it },
                eq3600Hz = eq3600Hz,
                onEq3600HzChange = { eq3600Hz = it },
                eq14000Hz = eq14000Hz,
                onEq14000HzChange = { eq14000Hz = it },
                volumeBoostDb = volumeBoostDb,
                onVolumeBoostDbChange = { volumeBoostDb = it },
                currentPreset = currentEqPreset,
                onSelectPreset = { preset ->
                    currentEqPreset = preset.id
                    eq60Hz = preset.gains.getOrElse(0) { 0f }
                    eq230Hz = preset.gains.getOrElse(1) { 0f }
                    eq910Hz = preset.gains.getOrElse(2) { 0f }
                    eq3600Hz = preset.gains.getOrElse(3) { 0f }
                    eq14000Hz = preset.gains.getOrElse(4) { 0f }
                },
                onReset = {
                    currentEqPreset = "FLAT"
                    eq60Hz = 0f
                    eq230Hz = 0f
                    eq910Hz = 0f
                    eq3600Hz = 0f
                    eq14000Hz = 0f
                    volumeBoostDb = 0f
                },
                onShowNotification = { msg ->
                    statusToastMessage = msg
                },
                onDismissRequest = { showEqualizerSheet = false }
            )
        }

        // 7. SLEEP TIMER PANEL (same glass panel as the video player's sleep timer)
        if (showSleepTimerSheet) {
            SleepTimerPanel(
                isLandscape = isLandscapeLayout,
                audioPlayerSide = true,
                glassBlurTransparency = glassBlurTransparency,
                onShowNotification = { msg ->
                    statusToastMessage = msg
                },
                onDismissRequest = { showSleepTimerSheet = false }
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    primaryText: Color,
    secondaryText: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value.ifBlank { "Unknown" },
            fontSize = 14.sp,
            color = primaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatAudioTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatAudioFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}

@Composable
fun ExpandedLyricsContainer(
    track: AudioTrackItem,
    positionMs: Long,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    onClose: () -> Unit,
    onAddLyrics: () -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSearching: Boolean = false,
    onSearchOnline: () -> Unit = {},
    lyricsModel: Lyrics? = null,
    activeProvider: LyricsProvider? = null,
    sourceType: LyricsSourceType = LyricsSourceType.LOCAL,
    onPickProvider: () -> Unit = {},
    isPlaying: Boolean = false,
    playbackSpeed: Float = 1f,
    replaceWithTranslation: Boolean = false,
    onTranslate: () -> Unit = {},
    isTranslating: Boolean = false,
    isTranslated: Boolean = false,
    immersive: Boolean = false,
    shiftMs: Long = 0L,
    onShiftChange: (Long) -> Unit = {},
    onBackgroundTap: (() -> Unit)? = null,
) {
    // No card / white circle behind the lyrics: they sit directly on the thumbnail-tinted background.
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !immersive,
                enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(220)),
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_lyrice,
                        contentDescription = "Lyrics",
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LYRICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentSkyBlue, letterSpacing = 1.sp)
                    if (lyricsModel?.isValid() == true || (track.hasLyrics && track.lyricsText.isNotBlank())) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            val isSynced = lyricsModel?.synced?.isNotEmpty() ?: com.example.util.LrcParser.isSynced(track.lyricsText)
                            Text(if (isSynced) "Synced" else "Plain", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // translation lives on the right side of the lyrics header (nothing on the left)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTranslated) AccentSkyBlue.copy(alpha = 0.23f) else if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                            .clickable(onClick = onTranslate)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(if (isTranslating) "…" else "文A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isTranslated) AccentSkyBlue else Color(0xFF2A9994))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                            .clickable(onClick = onPickProvider)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(activeProvider?.label ?: "Auto", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AccentSkyBlue)
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        StyledIcon(imageVector = Icons.Outlined.Close, contentDescription = "Close Lyrics", tint = secondaryText, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            if (lyricsModel?.synced?.isNotEmpty() == true) {
                // Timing nudge: use if the lyrics run ahead of / behind the singer on this track.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pillBg = if (isDark) Neutral.D1E293B.copy(alpha = 0.7f) else Neutral.E2E8F0.copy(alpha = 0.8f)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(pillBg)
                            .clickable { onShiftChange(shiftMs - 250L) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("Earlier", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = secondaryText) }
                    Text(
                        text = (if (shiftMs > 0) "+" else "") + String.format(Locale.US, "%.2fs", shiftMs / 1000.0),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (shiftMs == 0L) secondaryText else AccentSkyBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = shiftMs != 0L) { onShiftChange(0L) }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(pillBg)
                            .clickable { onShiftChange(shiftMs + 250L) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("Later", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = secondaryText) }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            }
            }
            if (lyricsModel?.isValid() == true || (track.hasLyrics && track.lyricsText.isNotBlank())) {
                AudioLyricsContent(
                    lyrics = lyricsModel,
                    fallbackText = track.lyricsText,
                    positionMs = positionMs,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    compact = !immersive,
                    isFullscreen = immersive,
                    onSeekTo = onSeekTo,
                    durationMs = track.durationMs,
                    isPlaying = isPlaying,
                    playbackSpeed = playbackSpeed,
                    replaceWithTranslation = replaceWithTranslation,
                    timeOffsetMs = shiftMs,
                    onBackgroundTap = onBackgroundTap,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        StyledIcon(
                            drawableRes = R.drawable.viaura_lyrice,
                            contentDescription = "No Lyrics",
                            tint = secondaryText.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (isSearching) "Searching lyrics online…" else "No lyrics available for this track", fontSize = 13.sp, color = secondaryText)
                        if (isSearching) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LyricsPlaceholderLines(compact = true, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!isSearching) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isDark) Neutral.D1E293B else Neutral.E2E8F0)
                                    .clickable(onClick = onSearchOnline)
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text("Search Online", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryText)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentSkyBlue)
                                .clickable(onClick = onAddLyrics)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text("Import Lyrics (.lrc, .txt)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
