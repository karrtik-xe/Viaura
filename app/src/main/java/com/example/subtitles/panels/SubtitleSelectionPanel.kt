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
 * Helper to clean video title into searchable keyword query
 */
fun cleanVideoTitleForSearch(title: String): String {
    if (title.isBlank()) return ""
    var clean = title.substringBeforeLast('.')
    clean = clean.replace(Regex("(?i)\\b(1080p|720p|480p|4k|2160p|bluray|bdrip|webrip|web-dl|x264|x265|hevc|aac|dts)\\b"), "")
    clean = clean.replace(Regex("[\\[\\]()_.-]"), " ")
    return clean.trim().replace(Regex("\\s+"), " ")
}

/**
 * Searches local device storage folders for matching subtitle files
 */
suspend fun searchLocalDeviceSubtitles(
    query: String,
    videoTitle: String,
    season: Int? = null,
    episode: Int? = null,
    languages: List<OnlineSubLanguage> = emptyList()
): List<File> = withContext(Dispatchers.IO) {
    data class ScoredFile(val file: File, val score: Int)

    val visited = mutableSetOf<String>()
    val scored = mutableListOf<ScoredFile>()
    // The typed name is not a file name: do not cut it at a "." ("Dr. Stone" must stay "dr stone").
    val normalizedQuery = query.replace(Regex("[\\[\\]()_.-]"), " ").trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
    val normalizedVideo = cleanVideoTitleForSearch(videoTitle).lowercase(Locale.ROOT)

    // Search by meaningful tokens instead of requiring the complete filename to match.
    // This handles common release-name differences such as:
    //   Video: "Bleach - 110 [FHD 1080p]..."
    //   Subtitle: "Bleach_110.ass" / "110.srt" / "Bleach - 110.eng.srt"
    // Episode numbers are intentionally weighted more heavily than generic words.
    val stopWords = setOf(
        "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "with",
        "episode", "ep", "sub", "subs", "subtitle", "subtitles", "eng", "english",
        "jpn", "jap", "japanese", "multi", "dual", "audio", "video"
    )

    fun tokens(text: String): List<String> = text
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.length >= 2 && it !in stopWords }
        .distinct()

    val queryTokens = tokens(if (normalizedQuery.isNotBlank()) normalizedQuery else normalizedVideo)
    val videoTokens = tokens(normalizedVideo)
    val effectiveTokens = (queryTokens + videoTokens).distinct()

    if (effectiveTokens.isEmpty()) return@withContext emptyList()

    fun baseScore(file: File): Int {
        val fileName = file.nameWithoutExtension.lowercase(Locale.ROOT)
        val folderName = file.parentFile?.name?.lowercase(Locale.ROOT).orEmpty()
        val searchable = "$fileName $folderName"
        val fileTokens = tokens(searchable)
        var score = 0
        var matched = 0

        for (token in effectiveTokens) {
            val numeric = token.all { it.isDigit() }
            val exact = fileTokens.any { it == token }
            val contains = searchable.contains(token)
            if (exact || contains) {
                matched++
                score += if (numeric) 6 else 3
                if (exact) score += 2
            }
        }

        // A complete normalized base-name match is a strong signal.
        if (fileName == normalizedQuery || fileName == normalizedVideo) score += 20
        if (normalizedVideo.isNotBlank() && fileName.contains(normalizedVideo)) score += 8

        // If an episode number exists in the query/video title, require it when possible.
        val queryNumbers = effectiveTokens.filter { it.all(Char::isDigit) && it.length >= 1 }
        if (queryNumbers.isNotEmpty()) {
            val hasEpisodeNumber = queryNumbers.any { searchable.contains(it) }
            if (!hasEpisodeNumber) return 0
        }

        // At least one meaningful token must match. For multi-token searches, reward stronger
        // overlap but don't require the subtitle filename to reproduce the entire video name.
        if (matched == 0) return 0
        val minimum = if (queryTokens.size >= 2) 5 else 3
        return if (score >= minimum) score else 0
    }

    // With an episode given (the usual case for series), only files for exactly that season / episode are kept:
    // "Show.S14E03.srt" is dropped when S14E02 is wanted. Without an episode the scoring above is used as before.
    val queryNumbers = queryTokens.mapNotNull { it.toIntOrNull() }.toSet()
    val markerLike = Regex("(?i)s\\d+e\\d+|\\d+x\\d+|\\d{1,4}")
    val nameTokens = queryTokens.filter { !it.matches(markerLike) }

    fun episodeScore(file: File): Int {
        val fileName = file.nameWithoutExtension
        val folder = file.parentFile?.name.orEmpty()
        var verdict = SubtitleEpisodeMatcher.verdict(fileName, season, episode, queryNumbers)
        if (verdict == 0) verdict = SubtitleEpisodeMatcher.verdict(folder, season, episode, queryNumbers)
        if (verdict != 1) return 0
        if (season != null) {
            val folderSeason = SubtitleEpisodeMatcher.folderSeason(folder)
            if (folderSeason != null && folderSeason != season) return 0
        }
        val searchable = "$fileName $folder".lowercase(Locale.ROOT)
        val fileTokens = tokens(searchable)
        var matched = 0
        for (token in nameTokens) if (fileTokens.contains(token) || searchable.contains(token)) matched++
        if (nameTokens.isNotEmpty()) {
            val need = maxOf(1, (nameTokens.size + 1) / 2)
            if (matched < need) return 0
        }
        var score = 22 + matched * 3
        if (fileName.lowercase(Locale.ROOT) == normalizedVideo) score += 10
        return score
    }

    // Language tag in the file name ("Show.S14E02.eng.srt"): a small boost for the chosen languages, a small
    // penalty for another language. Files without a tag stay as they are.
    fun firstWord(name: String) = name.lowercase(Locale.ROOT).substringBefore(' ')
    val selectedWords = languages.flatMap { listOf(firstWord(it.name), it.code3) }.toSet()
    val selectedShort = languages.map { it.code }.toSet()
    val allWords = OnlineSubtitleLanguages.all.flatMap { listOf(firstWord(it.name), it.code3) }.toSet()
    val allShort = OnlineSubtitleLanguages.all.map { it.code }.toSet()

    fun languageAdjust(file: File): Int {
        if (languages.isEmpty()) return 0
        val parts = file.nameWithoutExtension.lowercase(Locale.ROOT).split(Regex("[^\\p{L}]+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return 0
        val last = parts.last()
        val hitSelected = parts.any { it in selectedWords } || last in selectedShort
        val hitOther = parts.any { it in allWords && it !in selectedWords } || (last.length == 2 && last in allShort && last !in selectedShort)
        return when {
            hitSelected -> 4
            hitOther -> -4
            else -> 0
        }
    }

    fun scoreFile(file: File): Int {
        val base = if (episode != null) episodeScore(file) else baseScore(file)
        if (base <= 0) return 0
        return (base + languageAdjust(file)).coerceAtLeast(1)
    }

    val candidateRoots = listOfNotNull(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        File(Environment.getExternalStorageDirectory(), "Videos"),
        File(Environment.getExternalStorageDirectory(), "Subtitles"),
        File(Environment.getExternalStorageDirectory(), "Download"),
        Environment.getExternalStorageDirectory()
    )

    fun scanDir(dir: File, depth: Int) {
        if (depth > 4 || scored.size >= 120 || !dir.exists() || !dir.canRead()) return
        val canonical = try { dir.canonicalPath } catch (_: Throwable) { dir.absolutePath }
        if (!visited.add(canonical)) return

        val files = try { dir.listFiles() } catch (_: Throwable) { null } ?: return
        for (f in files) {
            if (f.isDirectory) {
                if (!f.name.startsWith(".") && !f.name.equals("Android", ignoreCase = true)) {
                    scanDir(f, depth + 1)
                }
            } else if (f.isFile && STRICT_SUBTITLE_EXTENSIONS.contains(f.extension.lowercase(Locale.ROOT))) {
                val score = scoreFile(f)
                if (score > 0) scored.add(ScoredFile(f, score))
            }
            if (scored.size >= 120) return
        }
    }

    for (root in candidateRoots) {
        if (root.exists() && root.canRead()) scanDir(root, 0)
        if (scored.size >= 120) break
    }

    scored
        .distinctBy { it.file.absolutePath }
        .sortedWith(compareByDescending<ScoredFile> { it.score }.thenBy { it.file.name.lowercase(Locale.ROOT) })
        .take(120)
        .map { it.file }
}

/**
 * 1. SUBTITLE TRACK SELECTION PANEL
 * Matches user's exact reference screenshot:
 * - Top minus drag handle centered horizontally.
 * - Header Left: Clickable "+ Add external subtitles" with Add icon.
 * - Header Right: Search icon (auto-fills search), Palette icon, Time Adjust icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubtitleSelectionPanel(
    videoTitle: String = "",
    subtitleTracks: List<PlayerMediaTrack>,
    selectedTrackId: Int,
    secondaryTrackId: Int = 0,
    lastTouchedTrackId: Int = 0,
    isSubtitleVisible: Boolean,
    isLandscape: Boolean,
    glassBlurTransparency: Float = 85f,
    showMediaInfo: Boolean = true,
    onSelectTrack: (Int) -> Unit,
    onSelectSecondaryTrack: (Int) -> Unit = {},
    onSelectTrackWithCascade: ((Int) -> Unit)? = null,
    onSelectSubtitleTracks: ((primaryId: Int, secondaryId: Int) -> Unit)? = null,
    onLastTouchedTrackIdChange: ((Int) -> Unit)? = null,
    onRemoveTrack: ((Int) -> Unit)? = null,
    onToggleVisibility: () -> Unit,
    onImportSubtitle: () -> Unit,
    onPasteSubtitle: () -> Unit = {},
    onSelectSubtitleFile: ((File) -> Unit)? = null,
    onShowNotification: (String) -> Unit = {},
    onOpenSubtitleDelay: () -> Unit = {},
    onOpenSubtitleStyle: () -> Unit = {},
    translationStatusByTrackId: Map<Int, TrackTranslationStatus> = emptyMap(),
    onStartTranslate: (Int) -> Unit = {},
    onTranslateSubtitleTrack: suspend (Int, (Int) -> Unit) -> Result<Int> = { _, _ ->
        Result.failure(IllegalStateException("Subtitle translation is not connected"))
    },
    onDismissRequest: () -> Unit
) {
    var isSearchMode by remember { mutableStateOf(false) }
    // One search screen: show name + season + episode + language(s). The search button / keyboard search key bumps
    // [searchNonce], which re-runs the on-device search and the online search together.
    val searchPanelContext = LocalContext.current
    val searchState = remember(videoTitle) { SubtitleSearchState(searchPanelContext.applicationContext, videoTitle) }
    val searchScope = rememberCoroutineScope()
    var searchNonce by remember { mutableStateOf(0) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var languagesBeforeSheet by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var foundSubtitleFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Attached (built-in) subtitles are visible the moment the panel opens, like the uploaded ones.
    var isEmbeddedExpanded by remember { mutableStateOf(true) }
    var isUploadedExpanded by remember { mutableStateOf(true) }
    var isImportantExpanded by remember { mutableStateOf(true) }
    var isFilesExpanded by remember { mutableStateOf(true) }
    var isOnlineExpanded by remember { mutableStateOf(false) }

    // ---- AI Subtitle Translate (real pipeline) ----
    val aiTranslateContext = androidx.compose.ui.platform.LocalContext.current
    var aiSettingsSnapshot by remember { mutableStateOf(AiFeaturesSettingsStore.load(aiTranslateContext)) }
    LaunchedEffect(Unit) { aiSettingsSnapshot = AiFeaturesSettingsStore.load(aiTranslateContext) }
    val aiTranslateReady = aiSettingsSnapshot.aiFeaturesEnabled &&
        aiSettingsSnapshot.subtitleTranslateEnabled &&
        aiSettingsSnapshot.hasUsableModel

    // On-device search (runs when the search screen opens and whenever the search button is pressed)
    LaunchedEffect(isSearchMode, searchNonce) {
        if (isSearchMode) {
            isSearching = true
            try {
                foundSubtitleFiles = searchLocalDeviceSubtitles(
                    query = searchState.query,
                    videoTitle = videoTitle,
                    season = searchState.seasonNumber,
                    episode = searchState.episodeNumber,
                    languages = searchState.languages
                )
            } finally {
                isSearching = false
            }
        }
    }
    // Online search (all providers, every selected language)
    LaunchedEffect(isSearchMode, searchNonce) {
        if (isSearchMode) {
            try {
                searchState.searchOnline()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (t: Throwable) {
                onShowNotification("Search failed: ${t.message}")
            }
        }
    }
    val runSearch: () -> Unit = {
        if (searchState.query.isBlank()) {
            onShowNotification("Type a show or movie name first")
        } else {
            searchNonce += 1
        }
    }

    PlayerGlassModalSheet(
        panelKey = "subtitles",
        isLandscape = isLandscape,
        heightFraction = if (isSearchMode) 0.84f else 0.58f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerCustomRow = {
            if (isSearchMode) {
                // Search header: back, language (opens a sheet), show name, search button + Season / Episode
                SubtitleSearchHeader(
                    state = searchState,
                    onBack = { isSearchMode = false },
                    onOpenLanguages = {
                        languagesBeforeSheet = searchState.langCodes
                        showLanguageSheet = true
                    },
                    onSearch = runSearch
                )
            } else {
                // Standard Header matching User's Exact Screenshot:
                // Left: "+ Add external subtitles"
                // Right: [Search Icon] [Palette Icon] [Time Adjust Icon]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Clickable + Add external subtitles (Tap opens file tree, Long-press pastes from clipboard)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    onImportSubtitle()
                                    onDismissRequest()
                                },
                                onLongClick = {
                                    onPasteSubtitle()
                                }
                            )
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StyledIcon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add external subtitles",
                            tint = NormalItemColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add external subtitles",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = NormalItemColor
                        )
                    }

                    // Right: Action Icons (Search, Palette, Time Adjust)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 1. Search button (Auto-fills file name and scans all folders)
                        IconButton(
                            onClick = { isSearchMode = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search Matching Subtitles",
                                tint = NormalItemColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 1.5 AI Subtitle Translate button: only visible when AI Features are enabled
                        if (aiSettingsSnapshot.aiFeaturesEnabled) {
                            IconButton(
                                onClick = {
                                    val targetTrackId = lastTouchedTrackId.takeIf { it > 0 } ?: selectedTrackId
                                    when {
                                        !aiTranslateReady -> onShowNotification("Configure AI Subtitle Translate in Settings > Advanced first")
                                        targetTrackId <= 0 -> onShowNotification("Select a subtitle track to translate")
                                        else -> onStartTranslate(targetTrackId)
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                StyledIcon(
                                    drawableRes = R.drawable.viaura_aitranslate,
                                    contentDescription = "AI Subtitle Translate",
                                    tint = if (aiTranslateReady) NormalItemColor else NormalItemColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 2. Palette / Subtitle Style button. Reuse the existing
                        // right-side subtitle panel host; do not create another sheet here.
                        // Hidden for an image/bitmap-based track (PGS, VobSub, DVB, CEA-608/708,
                        // ARIB B24, ...) since there is no text style to edit on those at all.
                        val styleButtonTargetTrack = subtitleTracks.firstOrNull {
                            it.id == (lastTouchedTrackId.takeIf { id -> id > 0 } ?: selectedTrackId)
                        }
                        if (!isImageBasedSubtitleTrack(styleButtonTargetTrack)) {
                            IconButton(
                                onClick = { onOpenSubtitleStyle() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Subtitle Style & Colors",
                                    tint = NormalItemColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 3. Time Adjust / Sync button (Stopwatch Icon)
                        IconButton(
                            onClick = {
                                onOpenSubtitleDelay()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Timelapse,
                                contentDescription = "Subtitle Time Adjust",
                                tint = NormalItemColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        if (isSearchMode) {
            // SEARCH RESULTS VIEW: "Matching Subtitle Files" (device) and "Online Subtitles" - both expand / collapse the same way
            val deduplicatedSubtitleFiles = remember(foundSubtitleFiles) {
                val seen = HashSet<String>()
                foundSubtitleFiles.filter { file -> seen.add(file.absolutePath) }
            }
            val sectionEnter = fadeIn(animationSpec = tween(240)) +
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight / 3 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) +
                expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            val sectionExit = fadeOut(animationSpec = tween(180)) +
                slideOutVertically(
                    targetOffsetY = { fullHeight -> -fullHeight / 4 },
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ) +
                shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                )

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                // ---- Device section
                item(key = "device_header") {
                    SubtitleSearchSectionHeader(
                        title = if (isSearching) "Matching Subtitle Files" else "Matching Subtitle Files (${deduplicatedSubtitleFiles.size})",
                        trailingLabel = "Device",
                        expanded = isFilesExpanded,
                        loading = isSearching,
                        onToggle = { isFilesExpanded = !isFilesExpanded }
                    )
                }
                item(key = "device_content") {
                    AnimatedVisibility(visible = isFilesExpanded, enter = sectionEnter, exit = sectionExit) {
                        if (isSearching) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.5.dp,
                                    color = ActiveHighlightColor
                                )
                            }
                        } else if (deduplicatedSubtitleFiles.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = null,
                                    tint = Neutral.S94A3B8,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchState.query.isBlank()) "No subtitle files found on device" else "No subtitle files found for \"${searchState.query}\"",
                                    color = SecondaryTextColor,
                                    fontSize = 13.5.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(sheetWhite(AccentContainerLight))
                                        .clickable {
                                            onImportSubtitle()
                                            onDismissRequest()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "Browse in Full File Explorer",
                                        color = AccentSkyBlueStrong,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                deduplicatedSubtitleFiles.forEach { file ->
                                    val ext = file.extension.uppercase(Locale.ROOT)
                                    val folderPath = file.parentFile?.absolutePath ?: file.parent ?: ""
                                    val displayFolderPath = if (folderPath.startsWith("/storage/emulated/0")) {
                                        folderPath.removePrefix("/storage/emulated/0")
                                    } else folderPath

                                    val isDark = com.example.ui.theme.isAppInDarkTheme()
                                    val fileItemBg = if (isDark) Neutral.D1E293B.copy(alpha = 0.50f) else sheetWhite(Neutral.F8FAFC)
                                    val fileItemBorder = if (isDark) Color.White.copy(alpha = 0.18f) else Neutral.E2E8F0
                                    val iconBadgeBg = if (isDark) AccentSkyBlueStrong.copy(alpha = 0.25f) else sheetWhite(AccentContainerLight)
                                    val iconBadgeTint = if (isDark) AccentSkyBlue else AccentSkyBlueStrong

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(fileItemBg)
                                            .border(1.dp, fileItemBorder, RoundedCornerShape(12.dp))
                                            .cardBounceClick {
                                                onSelectSubtitleFile?.invoke(file)
                                                onShowNotification("Loaded: ${file.name}")
                                                onDismissRequest()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(iconBadgeBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                StyledIcon(
                                                    imageVector = Icons.Outlined.Subtitles,
                                                    contentDescription = null,
                                                    tint = iconBadgeTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = file.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = NormalItemColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(iconBadgeBg)
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = ext,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = iconBadgeTint
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Text(
                                                    text = "📁 $displayFolderPath",
                                                    fontSize = 11.sp,
                                                    color = SecondaryTextColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- Online section (same look; collapsed until expanded)
                item(key = "online_header") {
                    SubtitleSearchSectionHeader(
                        title = if (searchState.running) "Online Subtitles" else "Online Subtitles (${searchState.results.size})",
                        trailingLabel = "Online",
                        expanded = isOnlineExpanded,
                        loading = searchState.running,
                        onToggle = { isOnlineExpanded = !isOnlineExpanded }
                    )
                }
                item(key = "online_content") {
                    AnimatedVisibility(visible = isOnlineExpanded, enter = sectionEnter, exit = sectionExit) {
                        OnlineSubtitleSectionContent(
                            state = searchState,
                            scope = searchScope,
                            onSubtitleFile = { file -> onSelectSubtitleFile?.invoke(file) },
                            onNotify = onShowNotification,
                            onDone = onDismissRequest
                        )
                    }
                }
            }
        } else {
            // AI-translated tracks move into "Important Subtitle" instead of staying under
            // Embedded/Uploaded — matches spec: translated track is promoted out of "Attach Subtitle".
            val importantTrackIds = translationStatusByTrackId
                .filterValues { it is TrackTranslationStatus.Completed }
                .keys
            val rawImportantTracks = subtitleTracks.filter { it.id in importantTrackIds }
            val rawEmbeddedTracks = subtitleTracks.filter { !it.isExternal && it.id !in importantTrackIds }
            val rawUploadedTracks = subtitleTracks.filter { it.isExternal && it.id !in importantTrackIds }

            // NOTE: selection highlighting reflects the active player state and ensures
            // external/uploaded tracks are reliably highlighted when active.
            val isPrimaryTrack = { trackId: Int ->
                selectedTrackId > 0 && selectedTrackId == trackId
            }
            val isSecondaryTrack = { trackId: Int ->
                secondaryTrackId > 0 && secondaryTrackId == trackId
            }
            val isTrackActive = { trackId: Int -> isPrimaryTrack(trackId) || isSecondaryTrack(trackId) }

            // Retain ALL tracks exposing genuine distinct subtitle streams (never deduplicate by title/name)
            val importantTracks = remember(rawImportantTracks) {
                rawImportantTracks.distinctBy { it.id }
            }
            val embeddedTracks = remember(rawEmbeddedTracks) {
                rawEmbeddedTracks.distinctBy { it.id }
            }
            val uploadedTracks = remember(rawUploadedTracks) {
                rawUploadedTracks.distinctBy { it.id }
            }

            val handleTrackToggle = { track: PlayerMediaTrack, title: String ->
                val trackId = track.id
                val isPrimary = isPrimaryTrack(trackId)
                val isSecondary = isSecondaryTrack(trackId)

                onLastTouchedTrackIdChange?.invoke(trackId)

                val (newPrimary, newSecondary, notificationMsg) = when {
                    isPrimary -> {
                        // CASE: Tapping Primary subtitle unselects it.
                        // If a Secondary subtitle is active, it is immediately promoted to Primary!
                        // Otherwise, subtitles are turned off.
                        if (secondaryTrackId > 0) {
                            val promotedTrack = (rawImportantTracks + rawEmbeddedTracks + rawUploadedTracks).firstOrNull { it.id == secondaryTrackId }
                            val promotedTitle = promotedTrack?.title ?: "Track $secondaryTrackId"
                            Triple(secondaryTrackId, 0, "Primary Subtitle: $promotedTitle")
                        } else {
                            Triple(0, 0, "Subtitles Off")
                        }
                    }
                    isSecondary -> {
                        // CASE: Tapping Secondary subtitle unselects it.
                        // Primary remains completely unchanged.
                        Triple(selectedTrackId, 0, "2nd Subtitle unselected: $title")
                    }
                    else -> {
                        // CASE: Tapping an unselected track:
                        // Tapped track becomes Primary.
                        // Previous Primary (if any) becomes Secondary.
                        val oldPrimary = if (selectedTrackId > 0 && selectedTrackId != trackId) selectedTrackId else 0
                        Triple(trackId, oldPrimary, "Primary Subtitle: $title")
                    }
                }

                if (newPrimary > 0 || newSecondary > 0) {
                    if (!isSubtitleVisible) onToggleVisibility()
                }

                if (onSelectSubtitleTracks != null) {
                    onSelectSubtitleTracks(newPrimary, newSecondary)
                } else {
                    onSelectSecondaryTrack(newSecondary)
                    onSelectTrack(newPrimary)
                }
                if (notificationMsg.isNotBlank()) {
                    onShowNotification(notificationMsg)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (embeddedTracks.isEmpty() && uploadedTracks.isEmpty() && importantTracks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Subtitles,
                                contentDescription = null,
                                tint = Neutral.S94A3B8,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No subtitle tracks available",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NormalItemColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap \"+ Add external subtitles\" above to load .srt, .ass, or .vtt subtitles",
                                fontSize = 12.sp,
                                color = SecondaryTextColor,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // 1. IMPORTANT SUBTITLE (AI-translated tracks)
                if (importantTracks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isImportantExpanded = !isImportantExpanded }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StyledIcon(
                                    drawableRes = R.drawable.viaura_aitranslate,
                                    contentDescription = null,
                                    tint = AccentSkyBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Important Subtitle (${importantTracks.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NormalItemColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Translate",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryTextColor
                                )
                                val arrowRotation by animateFloatAsState(
                                    targetValue = if (isImportantExpanded) 180f else 0f,
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    label = "importantArrowRotation"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { isImportantExpanded = !isImportantExpanded },
                                    contentAlignment = Alignment.Center
                                ) {
                                    StyledIcon(
                                        drawableRes = R.drawable.viaura_list_arrow_down,
                                        contentDescription = if (isImportantExpanded) "Collapse translate subtitles" else "Expand translate subtitles",
                                        tint = SecondaryTextColor,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .graphicsLayer { rotationZ = arrowRotation }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isImportantExpanded,
                            enter = fadeIn(animationSpec = tween(240)) +
                                    slideInVertically(
                                        initialOffsetY = { fullHeight -> fullHeight / 3 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) +
                                    expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ),
                            exit = fadeOut(animationSpec = tween(180)) +
                                   slideOutVertically(
                                       targetOffsetY = { fullHeight -> -fullHeight / 4 },
                                       animationSpec = tween(240, easing = FastOutSlowInEasing)
                                   ) +
                                   shrinkVertically(
                                       shrinkTowards = Alignment.Top,
                                       animationSpec = tween(240, easing = FastOutSlowInEasing)
                                   )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                importantTracks.forEachIndexed { idx, track ->
                                    val isPrimary = isPrimaryTrack(track.id)
                                    val isSecondary = isSecondaryTrack(track.id)
                                    val isSelected = isPrimary || isSecondary
                                    val isLastTouched = isSelected && (lastTouchedTrackId == track.id)
                                    val selectedColor = if (isSecondary) AccentSkyBlue else ActiveHighlightColor
                                    val selectedBg = if (isSecondary) AccentSkyBlue.copy(alpha = 0.12f) else ActiveItemBg
                                    val langName = if (showMediaInfo) formatLanguageName(track.language) else ""
                                    val cleanTitle = cleanTrackTitle(track.title, "Track ${idx + 1}")
                                    val displayTitle = if (langName.isNotBlank() && !cleanTitle.contains(langName, ignoreCase = true)) {
                                        "$cleanTitle • $langName"
                                    } else cleanTitle

                                    SelectableTrackItem(
                                        indexNumber = null,
                                        primaryTitle = displayTitle,
                                        badgeTag = null,
                                        metadataTags = trackMetadataTags(
                                            track = track,
                                            showMediaInfo = showMediaInfo,
                                            selectionLabel = if (isSecondary) "Secondary" else if (isPrimary) "Primary" else null
                                        ),
                                        isSelected = isSelected,
                                        isSecondary = isSecondary,
                                        isLastTouched = isLastTouched,
                                        selectedColor = selectedColor,
                                        selectedBgColor = selectedBg,
                                        selectionTag = if (isSecondary) "S" else if (isPrimary) "P" else null,
                                        translationStatus = translationStatusByTrackId[track.id] ?: TrackTranslationStatus.Idle,
                                        onClick = { handleTrackToggle(track, displayTitle) }
                                    )
                                }
                            }
                        }
                    }

                    if (embeddedTracks.isNotEmpty() || uploadedTracks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                thickness = 1.dp,
                                color = Neutral.S94A3B8.copy(alpha = 0.35f)
                            )
                        }
                    }
                }

                // 2. EMBEDDED SUBTITLES
                if (embeddedTracks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isEmbeddedExpanded = !isEmbeddedExpanded }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = null,
                                    tint = ActiveHighlightColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Embedded Subtitles (${embeddedTracks.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NormalItemColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Built-in",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryTextColor
                                )
                                val arrowRotation by animateFloatAsState(
                                    targetValue = if (isEmbeddedExpanded) 180f else 0f,
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    label = "embeddedArrowRotation"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { isEmbeddedExpanded = !isEmbeddedExpanded },
                                    contentAlignment = Alignment.Center
                                ) {
                                    StyledIcon(
                                        drawableRes = R.drawable.viaura_list_arrow_down,
                                        contentDescription = if (isEmbeddedExpanded) "Collapse embedded subtitles" else "Expand embedded subtitles",
                                        tint = SecondaryTextColor,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .graphicsLayer { rotationZ = arrowRotation }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isEmbeddedExpanded,
                            enter = fadeIn(animationSpec = tween(240)) +
                                    slideInVertically(
                                        initialOffsetY = { fullHeight -> fullHeight / 3 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) +
                                    expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ),
                            exit = fadeOut(animationSpec = tween(180)) +
                                   slideOutVertically(
                                       targetOffsetY = { fullHeight -> -fullHeight / 4 },
                                       animationSpec = tween(240, easing = FastOutSlowInEasing)
                                   ) +
                                   shrinkVertically(
                                       shrinkTowards = Alignment.Top,
                                       animationSpec = tween(240, easing = FastOutSlowInEasing)
                                   )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                embeddedTracks.forEachIndexed { idx, track ->
                                    val isPrimary = isPrimaryTrack(track.id)
                                    val isSecondary = isSecondaryTrack(track.id)
                                    val isSelected = isPrimary || isSecondary
                                    val isLastTouched = isSelected && (lastTouchedTrackId == track.id)
                                    val selectedColor = if (isSecondary) AccentSkyBlue else ActiveHighlightColor
                                    val selectedBg = if (isSecondary) AccentSkyBlue.copy(alpha = 0.12f) else ActiveItemBg

                                    val langName = if (showMediaInfo) formatLanguageName(track.language) else ""
                                    val cleanTitle = cleanTrackTitle(track.title, "Track ${idx + 1}")
                                    val displayTitle = if (langName.isNotBlank() && !cleanTitle.contains(langName, ignoreCase = true)) {
                                        "$cleanTitle • $langName"
                                    } else {
                                        cleanTitle
                                    }

                                    SelectableTrackItem(
                                        indexNumber = "${idx + 1}:",
                                        primaryTitle = displayTitle,
                                        badgeTag = null,
                                        metadataTags = trackMetadataTags(
                                            track = track,
                                            showMediaInfo = showMediaInfo,
                                            selectionLabel = if (isSecondary) "Secondary" else if (isPrimary) "Primary" else null
                                        ),
                                        isSelected = isSelected,
                                        isSecondary = isSecondary,
                                        isLastTouched = isLastTouched,
                                        selectedColor = selectedColor,
                                        selectedBgColor = selectedBg,
                                        selectionTag = if (isSecondary) "S" else if (isPrimary) "P" else null,
                                        translationStatus = translationStatusByTrackId[track.id] ?: TrackTranslationStatus.Idle,
                                        onClick = {
                                            handleTrackToggle(track, displayTitle)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. UPLOADED SUBTITLES
                if (uploadedTracks.isNotEmpty()) {
                    if (embeddedTracks.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                thickness = 1.dp,
                                color = Neutral.S94A3B8.copy(alpha = 0.35f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isUploadedExpanded = !isUploadedExpanded }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
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
                                    text = "Uploaded Subtitles (${uploadedTracks.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NormalItemColor
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "External",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SecondaryTextColor
                                )
                                val arrowRotation by animateFloatAsState(
                                    targetValue = if (isUploadedExpanded) 180f else 0f,
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    label = "uploadedArrowRotation"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable { isUploadedExpanded = !isUploadedExpanded },
                                    contentAlignment = Alignment.Center
                                ) {
                                    StyledIcon(
                                        drawableRes = R.drawable.viaura_list_arrow_down,
                                        contentDescription = if (isUploadedExpanded) "Collapse uploaded subtitles" else "Expand uploaded subtitles",
                                        tint = SecondaryTextColor,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .graphicsLayer { rotationZ = arrowRotation }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = isUploadedExpanded,
                            enter = fadeIn(animationSpec = tween(240)) +
                                    slideInVertically(
                                        initialOffsetY = { fullHeight -> fullHeight / 3 },
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ) +
                                    expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                                    ),
                            exit = fadeOut(animationSpec = tween(180)) +
                                   slideOutVertically(
                                       targetOffsetY = { fullHeight -> -fullHeight / 4 },
                                       animationSpec = tween(240, easing = FastOutSlowInEasing)
                                   ) +
                                   shrinkVertically(
                                       shrinkTowards = Alignment.Top,
                                       animationSpec = tween(240, easing = FastOutSlowInEasing)
                                   )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                uploadedTracks.forEach { track ->
                                    val isPrimary = isPrimaryTrack(track.id)
                                    val isSecondary = isSecondaryTrack(track.id)
                                    val isSelected = isPrimary || isSecondary
                                    val isLastTouched = isSelected && (lastTouchedTrackId == track.id)
                                    val selectedColor = if (isSecondary) AccentSkyBlue else ActiveHighlightColor
                                    val selectedBg = if (isSecondary) AccentSkyBlue.copy(alpha = 0.12f) else ActiveItemBg

                                    val originalName = track.originalFilename.ifBlank { track.title }
                                    val cleanName = if (originalName.contains('.')) originalName.substringBeforeLast('.') else originalName
                                    SwipeableTrackItem(
                                        primaryTitle = cleanName,
                                        badgeTag = null,
                                        metadataTags = trackMetadataTags(
                                            track = track,
                                            showMediaInfo = showMediaInfo,
                                            selectionLabel = if (isSecondary) "Secondary" else if (isPrimary) "Primary" else null
                                        ),
                                        isSelected = isSelected,
                                        isSecondary = isSecondary,
                                        isLastTouched = isLastTouched,
                                        selectedColor = selectedColor,
                                        selectedBgColor = selectedBg,
                                        selectionTag = if (isSecondary) "S" else if (isPrimary) "P" else null,
                                        translationStatus = translationStatusByTrackId[track.id] ?: TrackTranslationStatus.Idle,
                                        onRemove = if (onRemoveTrack != null) { { onRemoveTrack(track.id) } } else null,
                                        swipeLeftToRightToRemove = true,
                                        onClick = {
                                            handleTrackToggle(track, cleanName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSearchMode && showLanguageSheet) {
        SubtitleLanguageSheet(
            state = searchState,
            isLandscape = isLandscape,
            glassBlurTransparency = glassBlurTransparency,
            onDismiss = {
                showLanguageSheet = false
                if (searchState.langCodes != languagesBeforeSheet && searchState.query.isNotBlank()) searchNonce += 1
            }
        )
    }
}

/** Collapsible section header of the search screen: [icon] Title (count) ........ Device / Online [arrow]. */
@Composable
private fun SubtitleSearchSectionHeader(
    title: String,
    trailingLabel: String,
    expanded: Boolean,
    loading: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StyledIcon(
                imageVector = Icons.Outlined.Subtitles,
                contentDescription = null,
                tint = ActiveHighlightColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = NormalItemColor
            )
            if (loading) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = ActiveHighlightColor
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = trailingLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SecondaryTextColor
            )
            val arrowRotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = tween(260, easing = FastOutSlowInEasing),
                label = "searchSectionArrowRotation"
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                    drawableRes = R.drawable.viaura_list_arrow_down,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = SecondaryTextColor,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
    }
}
