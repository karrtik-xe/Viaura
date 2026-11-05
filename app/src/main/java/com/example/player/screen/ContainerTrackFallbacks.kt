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

internal fun resolveContainerAudioFallback(tracks: List<PlayerMediaTrack>): PlayerMediaTrack? {
    val embedded = tracks.filter { !it.isExternal }
    if (embedded.isEmpty()) return null

    fun score(track: PlayerMediaTrack): Int {
        val title = track.title.lowercase()
        val isDescription = title.contains("audio description") || title.contains("descriptive")
        val isCommentary = track.isAudioCommentary || title.contains("commentary") || title.contains("commentator")
        val isImpaired = track.isHearingImpaired || track.isVisualImpaired
        var value = 0
        if (track.isDefault) value += 100
        if (track.isForced) value += 35
        if (title.contains("original")) value += 25
        if (isDescription) value -= 80
        if (isCommentary) value -= 100
        if (isImpaired) value -= 30
        return value
    }

    val usable = embedded.filter { track ->
        val title = track.title.lowercase()
        !track.isAudioCommentary &&
            !track.isHearingImpaired &&
            !track.isVisualImpaired &&
            !title.contains("commentary") &&
            !title.contains("commentator") &&
            !title.contains("audio description") &&
            !title.contains("descriptive")
    }
    return (usable.ifEmpty { embedded }).maxByOrNull(::score)
}

internal fun resolveContainerSubtitleFallback(tracks: List<PlayerMediaTrack>): PlayerMediaTrack? {
    val embedded = tracks.filter { !it.isExternal }
    if (embedded.isEmpty()) return null

    fun score(track: PlayerMediaTrack): Int {
        val title = track.title.lowercase()
        val isSignsOrForcedTitle = Regex("(?i)signs|songs|forced|narrative").containsMatchIn(track.title)
        var value = 0
        if (track.isForced) value += 120
        if (track.isDefault) value += 80
        if (isSignsOrForcedTitle) value += 15
        if (track.isHearingImpaired) value -= 25
        if (track.isVisualImpaired) value -= 25
        if (title.contains("commentary")) value -= 80
        return value
    }

    val usable = embedded.filter { track ->
        val title = track.title.lowercase()
        !track.isHearingImpaired &&
            !track.isVisualImpaired &&
            !title.contains("commentary")
    }.ifEmpty { embedded }
    val flagged = usable.filter { it.isForced || it.isDefault }
    return if (flagged.isNotEmpty()) flagged.maxByOrNull(::score) else usable.firstOrNull()
}
