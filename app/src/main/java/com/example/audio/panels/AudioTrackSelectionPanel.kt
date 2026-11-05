package com.example.ui.components

import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentContainerLight
import com.example.ui.theme.AccentSkyBlueStrong
import com.example.ui.theme.isAppInDarkTheme
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.example.player.EqualizerPreset
import com.example.player.TrackAudioConfig
import com.example.player.AudioChannelMode
import com.example.player.VideoFilterPreset
import com.example.player.ManualVideoAdjustments
import com.example.ui.state.PlayerLayoutConfig
import com.example.ui.state.SeekbarStyle
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Environment
import com.example.player.PlayerMediaTrack
import com.example.player.PlayerVideoChapter
import com.example.ai.AiFeaturesSettingsStore
import com.example.ai.TrackTranslationStatus
import com.example.ai.TranslateLanguages
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import com.example.ui.screens.AspectRatioMode
import com.example.ui.screens.DecoderMode
import com.example.ui.screens.PlayerRepeatMode
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.screens.VideoItem
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Context
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 2. AUDIO TRACK SELECTION PANEL
 * Matches user's exact specification:
 * - Top minus drag handle centered horizontally.
 * - Header Left: Clickable "+ Add external audio track" with Add icon.
 * - Header Right: Time Adjust icon (no Search or Palette icon).
 */
@Composable
fun AudioSelectionPanel(
    audioTracks: List<PlayerMediaTrack>,
    selectedTrackId: Int,
    // false while the video has not loaded yet (buffering): no track is listed or marked as selected, nothing is guessed
    mediaReady: Boolean = true,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    showMediaInfo: Boolean = true,
    trackAudioConfigs: Map<Int, TrackAudioConfig> = emptyMap(),
    onSelectTrack: (Int) -> Unit,
    onRemoveTrack: ((Int) -> Unit)? = null,
    onTrackChannelModeChange: (trackId: Int, mode: AudioChannelMode) -> Unit = { _, _ -> },
    onTrackVolumeNormalizationChange: (trackId: Int, enabled: Boolean) -> Unit = { _, _ -> },
    onTrackDynamicRangeCompressionChange: (trackId: Int, enabled: Boolean) -> Unit = { _, _ -> },
    onImportAudio: () -> Unit = {},
    onShowNotification: (String) -> Unit = {},
    onOpenAudioDelay: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onDismissRequest: () -> Unit
) {
    val actualTracks = if (!mediaReady) {
        emptyList()
    } else if (audioTracks.isNotEmpty()) {
        audioTracks
    } else {
        listOf(
            PlayerMediaTrack(
                id = 1,
                type = "audio",
                title = "Main Audio Track",
                language = "en",
                codec = "",
                isSelected = true
            )
        )
    }

    // Dark sheet (dark app theme OR very transparent glass): the sheet text is light, so fields,
    // +/- buttons and separators must use the dark-glass surfaces, not the light ones. This is
    // read from the app theme directly because this code runs OUTSIDE the sheet's own
    // CompositionLocalProvider, where LocalSheetNormalTextColor is still its light-mode default.
    val isLowGlassOpacity = glassBlurTransparency < 25f || isAppInDarkTheme()

    val effectiveSelectedId = when {
        !mediaReady -> -1
        selectedTrackId > 0 -> selectedTrackId
        else -> actualTracks.firstOrNull { it.isSelected }?.id ?: actualTracks.firstOrNull()?.id ?: 1
    }
    val currentActiveConfig = trackAudioConfigs[effectiveSelectedId] ?: TrackAudioConfig()

    val embeddedTracks = actualTracks.filter { !it.isExternal }
    val uploadedTracks = actualTracks.filter { it.isExternal }

    PlayerGlassModalSheet(
        panelKey = "audio",
        isLandscape = isLandscape,
        heightFraction = if (isLandscape) 0.85f else 0.65f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerCustomRow = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Clickable + Add external audio track
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onImportAudio()
                            onDismissRequest()
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add external audio track",
                        tint = NormalItemColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add external audio track",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = NormalItemColor
                    )
                }

                // Right: Equalizer and Audio Delay buttons (Audio Delay is at the very end)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Equalizer icon button (Tuning 3 SVG) - before Audio Delay
                    IconButton(
                        onClick = {
                            onOpenEqualizer()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.viaura_equalizer),
                            contentDescription = "Equalizer",
                            tint = NormalItemColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Time Adjust icon button (Audio Delay) - at the very end
                    IconButton(
                        onClick = {
                            onOpenAudioDelay()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        StyledIcon(
                            imageVector = Icons.Outlined.Timelapse,
                            contentDescription = "Audio Time Adjust",
                            tint = NormalItemColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) {
        // UNIFIED SCROLLING CONTAINER: Audio Tracks List -> Divider -> Audio Controls
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp)
        ) {
            // If user has uploaded custom audio, show Embedded Audio Tracks header
            if (uploadedTracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Audiotrack,
                                contentDescription = null,
                                tint = ActiveHighlightColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Embedded Audio Tracks (${embeddedTracks.size})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NormalItemColor
                            )
                        }
                        Text(
                            text = "Built-in",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryTextColor
                        )
                    }
                }
            }

            if (embeddedTracks.isEmpty()) {
                if (uploadedTracks.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "No embedded audio tracks in this video",
                                fontSize = 12.5.sp,
                                color = SecondaryTextColor,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(embeddedTracks) { idx, track ->
                    val isSelected = (track.id == effectiveSelectedId)
                    val langName = if (showMediaInfo) formatLanguageName(track.language) else ""
                    val cleanTitle = cleanTrackTitle(track.title, "Audio Track ${idx + 1}")

                    val displayTitle = if (langName.isNotBlank() && !cleanTitle.contains(langName, ignoreCase = true)) {
                        "$cleanTitle • $langName"
                    } else {
                        cleanTitle
                    }

                    SelectableTrackItem(
                        indexNumber = "${idx + 1}:",
                        primaryTitle = displayTitle,
                        badgeTag = null,
                        metadataTags = trackMetadataTags(track = track, showMediaInfo = showMediaInfo),
                        isSelected = isSelected,
                        onClick = {
                            onSelectTrack(track.id)
                        }
                    )
                }
            }

            // Only show DIVIDER LINE and UPLOADED AUDIO if at least one audio track has been uploaded
            if (uploadedTracks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        thickness = 1.dp,
                        color = Neutral.S94A3B8.copy(alpha = 0.35f)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StyledIcon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Uploaded Audio Tracks (${uploadedTracks.size})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NormalItemColor
                            )
                        }
                    }
                }

                itemsIndexed(uploadedTracks, key = { _, t -> "uploaded_audio_${t.id}_${t.originalFilename}" }) { _, track ->
                    val isSelected = (track.id == effectiveSelectedId)
                    val originalName = track.originalFilename.ifBlank { track.title }
                    val cleanName = if (originalName.matches(Regex("(?i).*\\.(m4a|mp3|aac|opus|ogg|flac|wav)$"))) {
                        originalName.substringBeforeLast('.')
                    } else {
                        originalName
                    }

                    SwipeableTrackItem(
                        primaryTitle = cleanName,
                        badgeTag = null,
                        metadataTags = trackMetadataTags(track = track, showMediaInfo = showMediaInfo),
                        isSelected = isSelected,
                        onRemove = if (onRemoveTrack != null) { { onRemoveTrack(track.id) } } else null,
                        onClick = {
                            onSelectTrack(track.id)
                        }
                    )
                }
            }

            // 2. Horizontal Line Separator - rendered strictly AFTER all audio tracks
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(if (isLowGlassOpacity) Color(0x20FFFFFF) else sheetWhite(Neutral.E2E8F0))
                )
            }

            // 3. Audio Channels (Applies to selected audio track)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Audio channels",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalSheetSecondaryTextColor.current
                    )

                    val channelOptions = listOf(
                        AudioChannelMode.AUTO to "Auto",
                        AudioChannelMode.AUTO_SAFE to "Auto Safe",
                        AudioChannelMode.MONO to "Mono",
                        AudioChannelMode.STEREO to "Stereo",
                        AudioChannelMode.REVERSED_STEREO to "Reversed Stereo"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        channelOptions.forEach { (mode, label) ->
                            val isChannelSelected = currentActiveConfig.channelMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (isChannelSelected) Modifier.background(AccentGradient)
                                        else Modifier.background(SheetButtonBgColor)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isChannelSelected) Color(0x60FFFFFF)
                                        else SheetButtonBorderColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .cardBounceClick(scaleDown = 0.94f) {
                                        onTrackChannelModeChange(effectiveSelectedId, mode)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChannelSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChannelSelected) Color.White else SheetButtonTextColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. Audio Processing (Applies to selected audio track)
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Audio processing",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalSheetSecondaryTextColor.current
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume normalization toggle
                        val isVolNormActive = currentActiveConfig.volumeNormalization
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isVolNormActive) Modifier.background(AccentGradient)
                                    else Modifier.background(SheetButtonBgColor)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isVolNormActive) Color(0x60FFFFFF)
                                    else SheetButtonBorderColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .cardBounceClick(scaleDown = 0.94f) {
                                    onTrackVolumeNormalizationChange(effectiveSelectedId, !isVolNormActive)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Volume normalization",
                                fontSize = 13.sp,
                                fontWeight = if (isVolNormActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isVolNormActive) Color.White else SheetButtonTextColor,
                                maxLines = 1
                            )
                        }

                        // Dynamic range compression (DRC) toggle
                        val isDrcActive = currentActiveConfig.dynamicRangeCompression
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (isDrcActive) Modifier.background(AccentGradient)
                                    else Modifier.background(SheetButtonBgColor)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDrcActive) Color(0x60FFFFFF)
                                    else SheetButtonBorderColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .cardBounceClick(scaleDown = 0.94f) {
                                    onTrackDynamicRangeCompressionChange(effectiveSelectedId, !isDrcActive)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dynamic range compression (DRC)",
                                fontSize = 13.sp,
                                fontWeight = if (isDrcActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDrcActive) Color.White else SheetButtonTextColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
