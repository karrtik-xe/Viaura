package com.example.ui.screens

import com.example.ui.components.liquidSearchSurface
import com.example.ui.theme.Neutral

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import com.example.ui.components.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BottomMoveCopyProgressBar
import com.example.ui.components.DirectoryPickerAction
import com.example.ui.components.FloatingDirectoryPickerSheet
import com.example.ui.components.isRestrictedDirectory
import com.example.ui.components.HeaderBar
import com.example.ui.components.StyledIcon
import com.example.ui.components.bounceClick
import com.example.ui.components.loadVideoThumbnail
import com.example.ui.state.SettingsPreferencesManager
import com.example.ui.state.ThemeMode
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkBackgroundEnd
import com.example.ui.theme.DarkBackgroundStart
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightBackgroundEnd
import com.example.ui.theme.LightBackgroundStart
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.MkvAttachmentInfo
import com.example.util.MkvChapterInfo
import com.example.util.MkvContainerMetadata
import com.example.util.MkvManagerEngine
import com.example.util.MkvTrackInfo
import com.example.util.MkvTrackType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun MediaInformationScreen(
    video: VideoItem,
    uiState: com.example.ui.state.UiState,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = uiState.themeMode == ThemeMode.DARK ||
        (uiState.themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
    // Same screen colour as the Home screen (ice in Light, dark panel colour in Dark, pure black in AMOLED, white only for Theme = System)
    val mainScreenColor = if (isDark && uiState.amoledBlackMode) {
        DarkBackgroundStart
    } else if (isDark) {
        Neutral.D1B2630
    } else if (uiState.themeMode == ThemeMode.SYSTEM) {
        Neutral.FFFFFF
    } else {
        Neutral.DAE3E9
    }
    val background = Brush.verticalGradient(listOf(mainScreenColor, mainScreenColor))
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val surface = if (isDark) DarkGlassSurface else LightGlassSurface
    val border = if (isDark) DarkGlassBorder else LightGlassBorder

    var data by remember(video.path) { mutableStateOf<MediaInfoData?>(null) }
    var error by remember(video.path) { mutableStateOf<String?>(null) }
    var active by rememberSaveable { mutableStateOf(MediaInfoSection.OVERVIEW) }
    var showPicker by remember { mutableStateOf(false) }
    var pendingOutputName by remember { mutableStateOf("") }
    var pendingExtract: (suspend (Uri) -> Boolean)? by remember { mutableStateOf(null) }
    var processing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressName by remember { mutableStateOf("") }
    var progressMessage by remember { mutableStateOf("") }
    var chapterFormat by rememberSaveable { mutableStateOf("xml") }
    var expandedAttachments by rememberSaveable { mutableStateOf(false) }
    var showFontDirectoryPicker by remember { mutableStateOf(false) }
    var mediaThumbnail by remember(video.path) { mutableStateOf<android.graphics.Bitmap?>(null) }

    var lastExtractDir by rememberSaveable {
        mutableStateOf<String?>(SettingsPreferencesManager.getLastExtractDirectory(context))
    }

    val preferredExtractDir = remember(lastExtractDir, video.path) {
        lastExtractDir ?: try {
            val f = java.io.File(video.path)
            val p = if (f.isDirectory) f else f.parentFile
            if (p != null && p.exists() && p.canRead() && !isRestrictedDirectory(p)) {
                p.absolutePath
            } else null
        } catch (_: Throwable) { null }
    }

    // Search state
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            // The search field is attached a moment after the flag flips (and the screen may already be leaving on a fast
            // Back): requesting focus before the Modifier.focusRequester node exists crashed the app. Wait, retry, never throw.
            var tries = 0
            while (tries < 5) {
                kotlinx.coroutines.delay(if (tries == 0) 60L else 80L)
                val ok = try { searchFocusRequester.requestFocus(); true } catch (_: IllegalStateException) { false } catch (_: Throwable) { true }
                if (ok) break
                tries++
            }
        }
    }

    LaunchedEffect(video.path, video.uri) {
        error = null
        data = null
        try {
            val result = withContext(Dispatchers.IO) {
                val ext = video.displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                if (ext == "mkv" || ext == "webm" || ext == "mka") {
                        val parsed = MkvManagerEngine.parseMkvFile(context, video.uri, scanSizes = false)
                        val enrichedTracks = parsed.second.map { track ->
                            if (track.trackType == MkvTrackType.VIDEO && track.frameRate <= 0f && video.framerate > 0.0) {
                                track.copy(frameRate = video.framerate.toFloat(), durationMs = parsed.first.durationMs)
                            } else if (track.durationMs <= 0L) {
                                track.copy(durationMs = parsed.first.durationMs)
                            } else track
                        }
                        MediaInfoData(parsed.first, enrichedTracks, parsed.third.first, parsed.third.second)
                    } else {
                        buildMediaInfoFromExtractor(context, video) ?: run {
                        val meta = com.example.util.MediaMetadataExtractor.extractMetadata(
                            filePath = video.path,
                            context = context,
                            uri = video.uri,
                            displayName = video.displayName,
                            sizeBytes = video.sizeBytes,
                            dateModified = video.dateModified
                        )
                        val track = MkvTrackInfo(
                            id = 1, trackNumber = 1, trackType = MkvTrackType.VIDEO,
                            pixelWidth = meta.width, pixelHeight = meta.height,
                            frameRate = meta.framerate.toFloat(), durationMs = meta.durationMs
                        )
                        MediaInfoData(
                            MkvContainerMetadata(
                                fileName = video.displayName,
                                filePath = video.path,
                                fileSizeBytes = video.sizeBytes,
                                durationMs = meta.durationMs
                            ),
                            listOf(track), emptyList(), emptyList()
                        )
                        }
                    }
                }
                data = result
                // The page is already visible. Exact bitrate / stream sizes need a pass over every block of the file, so they
                // are filled in afterwards (same exact numbers, they just appear a moment later on big files).
                if (result.tracks.isNotEmpty() && result.metadata.fileSizeBytes > 0L && result.tracks.any { it.streamSizeBytes <= 0L }) {
                    val ext = video.displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (ext == "mkv" || ext == "webm" || ext == "mka") {
                        MkvManagerEngine.scanTrackSizes(context, video.uri, result.metadata, result.tracks)?.let { scanned ->
                            data = result.copy(tracks = scanned)
                        }
                    } else {
                        scanExtractorTrackSizes(context, video, result)?.let { scanned ->
                            data = result.copy(tracks = scanned)
                        }
                    }
                }
            } catch (t: Throwable) {
                error = t.localizedMessage ?: "Unable to read media information."
            }
    }

    LaunchedEffect(video.path, video.uri) {
        mediaThumbnail = loadVideoThumbnail(
            context, video.id, video.uri, video.path,
            ThumbnailStrategy.SMART, ThumbnailQuality.MEDIUM, false
        )
    }

    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            onBack()
        }
    }

    val infoText = remember(data, active) { buildInformationText(data, active, video) }

    fun copyText(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Media Information", text))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${video.displayName} — ${active.title}")
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share"))
    }

    val body = data

    if (showPicker) {
        FloatingDirectoryPickerSheet(
            action = DirectoryPickerAction.EXTRACT,
            itemCount = 1,
            itemDescription = pendingOutputName,
            initialPath = preferredExtractDir,
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onConfirmTargetDir = { targetDir ->
                showPicker = false
                val dirPath = targetDir.absolutePath
                lastExtractDir = dirPath
                SettingsPreferencesManager.setLastExtractDirectory(context, dirPath)
                val work = pendingExtract
                pendingExtract = null
                val targetFile = getUniqueDestinationFile(targetDir, pendingOutputName)
                val targetUri = Uri.fromFile(targetFile)
                if (work != null) {
                    scope.launch {
                        processing = true
                        progress = 0.05f
                        progressName = targetFile.name
                        progressMessage = "Extracting..."
                        val ok = try {
                            work(targetUri)
                        } catch (_: Throwable) { false }
                        processing = false
                        progress = if (ok) 1f else 0f
                        if (ok) {
                            try {
                                android.media.MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(targetFile.absolutePath),
                                    null,
                                    null
                                )
                            } catch (_: Throwable) {}
                        }
                        Toast.makeText(
                            context,
                            if (ok) "Extracted successfully: ${targetFile.name}" else "Extraction failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onDismiss = {
                showPicker = false
                pendingExtract = null
            }
        )
    }

    if (showFontDirectoryPicker && body != null) {
        FloatingDirectoryPickerSheet(
            action = DirectoryPickerAction.EXTRACT,
            itemCount = body.attachments.count { it.isFont },
            itemDescription = "font attachments",
            initialPath = preferredExtractDir,
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            onConfirmTargetDir = { target ->
                showFontDirectoryPicker = false
                val dirPath = target.absolutePath
                lastExtractDir = dirPath
                SettingsPreferencesManager.setLastExtractDirectory(context, dirPath)
                val fonts = body.attachments.filter { it.isFont }
                scope.launch {
                    processing = true
                    progress = 0f
                    progressName = "Fonts"
                    var completed = 0
                    for (font in fonts) {
                        try {
                            val out = getUniqueDestinationFile(target, sanitizeFileComponent(font.fileName))
                            out.parentFile?.mkdirs()
                            out.outputStream().use { it.write(font.data) }
                            try {
                                android.media.MediaScannerConnection.scanFile(
                                    context,
                                    arrayOf(out.absolutePath),
                                    null,
                                    null
                                )
                            } catch (_: Throwable) {}
                        } catch (_: Throwable) {}
                        completed++
                        progress = completed.toFloat() / fonts.size.coerceAtLeast(1)
                        progressName = font.fileName
                        progressMessage = "Saving font ${completed}/${fonts.size}"
                    }
                    processing = false
                    Toast.makeText(context, "${fonts.size} font(s) extracted to ${target.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showFontDirectoryPicker = false }
        )
    }

    // Exact top safe area calculation (identical to HomeScreen to prevent camera notch jump)
    val currentStatusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val currentCutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val currentTopInset = maxOf(currentStatusBarTop, currentCutoutTop)

    var stableTopInsetFloat by rememberSaveable { mutableStateOf(0f) }
    if (currentTopInset > 0.dp && currentTopInset.value != stableTopInsetFloat) {
        stableTopInsetFloat = currentTopInset.value
    }
    val safeTopPadding = when {
        currentTopInset > 0.dp -> currentTopInset
        stableTopInsetFloat > 0f -> stableTopInsetFloat.dp
        else -> 36.dp
    }

    Box(Modifier.fillMaxSize().background(background)) {
        // Settings > Appearance > Background: same page animation as Home plays behind this page too
        com.example.background.PageBackdrop(uiState.themeMode)
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = safeTopPadding + 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            HeaderBar(
                title = "Media Information & Extractor",
                subtitle = video.displayName,
                themeMode = uiState.themeMode,
                onSettingsClick = null, // Settings button removed as requested
                onBackClick = onBack,
                onSearchClick = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) searchQuery = ""
                },
                onSortClick = null,
                onCopyClick = { copyText(infoText) },
                onShareClick = { shareText(infoText) }
            )

            // Sleek Search Bar for Media Information
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                        .liquidSearchSurface(RoundedCornerShape(18.dp), isDark, surface, border)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StyledIcon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search by language (hi, ja, en), codec, quality (1080p), chapter, font...",
                                    fontSize = 13.sp,
                                    color = secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = com.example.ui.theme.LocalAppFontFamily.current,
                                    color = primary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(AccentSkyBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(primary.copy(alpha = 0.08f))
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear",
                                    tint = secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Navigation Tabs with Custom Icons & Gradient Active Indicators
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(MediaInfoSection.entries) { section ->
                    val selected = active == section
                    val icon = when (section) {
                        MediaInfoSection.OVERVIEW -> Icons.Outlined.Info
                        MediaInfoSection.VIDEO -> Icons.Outlined.VideoFile
                        MediaInfoSection.AUDIO -> Icons.Outlined.Audiotrack
                        MediaInfoSection.SUBTITLE -> Icons.Outlined.Subtitles
                        MediaInfoSection.CHAPTERS -> Icons.Outlined.Bookmarks
                        MediaInfoSection.ATTACHMENTS -> Icons.Outlined.AttachFile
                    }
                    val tabModifier = if (selected) {
                        Modifier.background(AccentGradient)
                    } else {
                        Modifier.background(if (isDark) Neutral.D1E293B.copy(alpha = .72f) else Neutral.F1F5F9)
                    }
                    Box(
                        Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .then(tabModifier)
                            .border(1.dp, if (selected) Color.Transparent else border, RoundedCornerShape(19.dp))
                            .bounceClick { active = section }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StyledIcon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) Color.White else primary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = section.title,
                                color = if (selected) Color.White else primary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (body == null && error == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentSkyBlue)
                }
            } else if (error != null) {
                InfoCard(title = "Unable to scan media", isDark = isDark, border = border, surface = surface) {
                    Text(error!!, color = secondary, fontSize = 14.sp)
                }
            } else {
                MediaInfoContent(
                    section = active,
                    data = body!!,
                    video = video,
                    searchQuery = searchQuery,
                    mediaThumbnail = mediaThumbnail,
                    isDark = isDark,
                    primary = primary,
                    secondary = secondary,
                    surface = surface,
                    border = border,
                    chapterFormat = chapterFormat,
                    onChapterFormatChange = { chapterFormat = it },
                    expandedAttachments = expandedAttachments,
                    onExpandedAttachmentsChange = { expandedAttachments = it },
                    onExtractAllFonts = { showFontDirectoryPicker = true },
                    onExtract = { outName, work ->
                        // opens the folder picker; the extraction runs when the folder is confirmed
                        pendingOutputName = outName
                        pendingExtract = work
                        showPicker = true
                    },
                    onCopy = ::copyText,
                    onShare = ::shareText,
                    onProgress = { f, msg, name ->
                        progress = f
                        progressMessage = msg
                        progressName = name
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = processing,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).navigationBarsPadding()
        ) {
            BottomMoveCopyProgressBar(
                isDark = isDark,
                alphaRatio = (uiState.glassBlurTransparency / 100f).coerceIn(.15f, 1f),
                isMove = false,
                customActionText = "Extracting...",
                customIcon = Icons.Outlined.Download,
                progressFraction = progress,
                currentItemName = progressName,
                completedCount = if (progress >= 1f) 1 else 0,
                totalCount = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Non-Matroska files (MP4, MOV, 3GP, TS, AVI ...): every track is read from the file itself, nothing is guessed.
// ---------------------------------------------------------------------------------------------------------------

private fun extractorCodec(mime: String): Pair<String, String> {
    val m = mime.lowercase(Locale.ROOT)
    return when {
        m == "video/avc" -> "V_MPEG4/ISO/AVC" to ""
        m == "video/hevc" -> "V_MPEGH/ISO/HEVC" to ""
        m == "video/x-vnd.on2.vp9" -> "V_VP9" to ""
        m == "video/x-vnd.on2.vp8" -> "V_VP8" to ""
        m == "video/av01" -> "V_AV1" to ""
        m == "video/mp4v-es" -> "V_MPEG4/ISO/ASP" to "MPEG-4 Visual"
        m == "video/mpeg2" -> "V_MPEG2" to "MPEG-2 Video"
        m == "video/3gpp" -> "V_H263" to "H.263"
        m.startsWith("video/dolby-vision") -> "V_DOVI" to "Dolby Vision"
        m == "audio/mp4a-latm" -> "A_AAC" to ""
        m == "audio/opus" -> "A_OPUS" to ""
        m == "audio/vorbis" -> "A_VORBIS" to ""
        m == "audio/flac" -> "A_FLAC" to ""
        m == "audio/ac3" -> "A_AC3" to ""
        m == "audio/eac3-joc" -> "A_EAC3" to "Dolby Digital Plus (E-AC-3 JOC / Atmos)"
        m == "audio/eac3" -> "A_EAC3" to ""
        m == "audio/mpeg" -> "A_MPEG/L3" to "MP3"
        m == "audio/mpeg-l2" -> "A_MPEG/L2" to "MP2"
        m == "audio/raw" -> "A_PCM/INT/LIT" to "PCM"
        m == "audio/alac" -> "A_ALAC" to "ALAC"
        m == "audio/amr-wb" || m == "audio/amr_wb" -> "A_AMR" to "AMR-WB"
        m == "audio/3gpp" -> "A_AMR" to "AMR-NB"
        m == "audio/vnd.dts.hd" -> "A_DTS" to "DTS-HD"
        m == "audio/vnd.dts" -> "A_DTS" to ""
        m == "audio/true-hd" -> "A_TRUEHD" to ""
        m == "application/x-subrip" -> "S_TEXT/UTF8" to ""
        m == "text/vtt" || m == "application/x-mp4-vtt" -> "S_TEXT/WEBVTT" to ""
        m == "text/x-ssa" -> "S_TEXT/ASS" to ""
        m == "application/pgs" -> "S_HDMV/PGS" to ""
        m == "application/vobsub" -> "S_VOBSUB" to ""
        m == "application/x-quicktime-tx3g" || m == "text/3gpp-tt" -> "S_TX3G" to "3GPP Timed Text (tx3g)"
        m == "application/dvbsubs" -> "S_DVBSUB" to "DVB Subtitles"
        m == "application/ttml+xml" -> "S_TEXT/TTML" to "TTML"
        m.contains("cea-608") -> "S_CEA608" to "CEA-608 Closed Captions"
        m.contains("cea-708") -> "S_CEA708" to "CEA-708 Closed Captions"
        else -> mime to mime.substringAfter('/').uppercase(Locale.ROOT)
    }
}

private fun isExtractorSubtitleMime(mime: String): Boolean {
    val m = mime.lowercase(Locale.ROOT)
    return m.startsWith("text/") || m in setOf(
        "application/x-subrip", "application/x-quicktime-tx3g", "application/vobsub", "application/pgs",
        "application/dvbsubs", "application/ttml+xml", "application/x-mp4-vtt", "application/x-media3-cues"
    ) || m.contains("cea-608") || m.contains("cea-708")
}

private fun avcProfileName(p: Int): String = when (p) {
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline -> "Baseline"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline -> "Constrained Baseline"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileMain -> "Main"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileExtended -> "Extended"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh -> "High"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedHigh -> "Constrained High"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10 -> "High 10"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422 -> "High 4:2:2"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444 -> "High 4:4:4 Predictive"
    else -> ""
}

private fun avcLevelName(l: Int): String = when (l) {
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel1 -> "1"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel1b -> "1b"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel11 -> "1.1"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel12 -> "1.2"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel13 -> "1.3"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel2 -> "2"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel21 -> "2.1"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel22 -> "2.2"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel3 -> "3"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel31 -> "3.1"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel32 -> "3.2"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel4 -> "4"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel41 -> "4.1"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel42 -> "4.2"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel5 -> "5"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel51 -> "5.1"
    android.media.MediaCodecInfo.CodecProfileLevel.AVCLevel52 -> "5.2"
    else -> ""
}

private fun hevcProfileName(p: Int): String = when (p) {
    android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain -> "Main"
    android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 -> "Main 10"
    android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill -> "Main Still Picture"
    android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 -> "Main 10"
    android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus -> "Main 10"
    else -> ""
}

/** HEVC level flags come in pairs: main tier, then high tier (shifted one bit). Returns "level" and "tier". */
private fun hevcLevelAndTier(l: Int): Pair<String, String> {
    val names = listOf("1", "2", "2.1", "3", "3.1", "4", "4.1", "5", "5.1", "5.2", "6", "6.1", "6.2")
    var bit = 1
    for (name in names) {
        if (l == bit) return name to "Main"
        if (l == (bit shl 1)) return name to "High"
        bit = bit shl 2
    }
    return "" to ""
}

private fun gcdInt(a: Int, b: Int): Int {
    var x = kotlin.math.abs(a); var y = kotlin.math.abs(b)
    while (y != 0) { val t = x % y; x = y; y = t }
    return x.coerceAtLeast(1)
}

/**
 * Reads the real track list of a non-Matroska file with MediaExtractor / MediaMetadataRetriever. Only values the file
 * (or Android's parser) really reports are filled in; everything else stays empty and is simply not shown.
 */
internal fun buildMediaInfoFromExtractor(context: Context, video: VideoItem): MediaInfoData? {
    var durationMs = 0L
    var title = ""
    var date = ""
    var mimeType = ""
    var frameCount = 0L
    val onDisk = video.path.isNotBlank() && !video.path.startsWith("content:") && File(video.path).exists()
    val retriever = android.media.MediaMetadataRetriever()
    try {
        if (onDisk) retriever.setDataSource(video.path) else retriever.setDataSource(context, video.uri)
        durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        title = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty().trim()
        date = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DATE).orEmpty().trim()
        mimeType = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE).orEmpty()
        frameCount = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toLongOrNull() ?: 0L
    } catch (_: Throwable) {
    } finally {
        try { retriever.release() } catch (_: Throwable) {}
    }

    val tracks = mutableListOf<MkvTrackInfo>()
    val extractor = android.media.MediaExtractor()
    try {
        if (onDisk) extractor.setDataSource(video.path) else extractor.setDataSource(context, video.uri, null)
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: continue
            val type = when {
                mime.startsWith("video/") -> MkvTrackType.VIDEO
                mime.startsWith("audio/") -> MkvTrackType.AUDIO
                isExtractorSubtitleMime(mime) -> MkvTrackType.SUBTITLE
                else -> continue
            }
            fun int(key: String): Int = try { if (f.containsKey(key)) f.getInteger(key) else 0 } catch (_: Throwable) { 0 }
            fun long(key: String): Long = try { if (f.containsKey(key)) f.getLong(key) else 0L } catch (_: Throwable) { 0L }
            val (codecId, codecName) = extractorCodec(mime)
            val lang = try { f.getString(android.media.MediaFormat.KEY_LANGUAGE) } catch (_: Throwable) { null }
                ?.trim()?.takeIf { it.isNotBlank() } ?: "und"
            val trackDurMs = long(android.media.MediaFormat.KEY_DURATION) / 1000L
            val width = int(android.media.MediaFormat.KEY_WIDTH)
            val height = int(android.media.MediaFormat.KEY_HEIGHT)
            val rotation = int(android.media.MediaFormat.KEY_ROTATION)
            val rotated = rotation == 90 || rotation == 270
            val dispW = if (rotated) height else width
            val dispH = if (rotated) width else height
            val fps = try {
                if (!f.containsKey(android.media.MediaFormat.KEY_FRAME_RATE)) 0f else try {
                    f.getFloat(android.media.MediaFormat.KEY_FRAME_RATE)
                } catch (_: Throwable) { f.getInteger(android.media.MediaFormat.KEY_FRAME_RATE).toFloat() }
            } catch (_: Throwable) { 0f }

            var profileName = ""
            var levelName = ""
            var tierName = ""
            var bitDepth = 0
            if (type == MkvTrackType.VIDEO) {
                val profile = int(android.media.MediaFormat.KEY_PROFILE)
                val level = int(android.media.MediaFormat.KEY_LEVEL)
                when (codecId) {
                    "V_MPEG4/ISO/AVC" -> {
                        profileName = avcProfileName(profile)
                        levelName = avcLevelName(level)
                        bitDepth = when (profile) {
                            android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10 -> 10
                            android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh422,
                            android.media.MediaCodecInfo.CodecProfileLevel.AVCProfileHigh444 -> 0
                            0 -> 0
                            else -> 8
                        }
                    }
                    "V_MPEGH/ISO/HEVC" -> {
                        profileName = hevcProfileName(profile)
                        hevcLevelAndTier(level).let { levelName = it.first; tierName = it.second }
                        bitDepth = when (profile) {
                            android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain,
                            android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMainStill -> 8
                            android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10,
                            android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10,
                            android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus -> 10
                            else -> 0
                        }
                    }
                }
            } else if (type == MkvTrackType.AUDIO) {
                if (codecId == "A_AAC") {
                    profileName = when (int("aac-profile")) {
                        2 -> "LC"; 5 -> "HE-AAC"; 29 -> "HE-AACv2"; 23 -> "LD"; 39 -> "ELD"; 1 -> "Main"; else -> ""
                    }
                }
                bitDepth = when (int(android.media.MediaFormat.KEY_PCM_ENCODING)) {
                    3 -> 8; 2 -> 16; 21 -> 24; 22 -> 32; 4 -> 32; else -> 0
                }
            }

            val transfer = int(android.media.MediaFormat.KEY_COLOR_TRANSFER)
            val standard = int(android.media.MediaFormat.KEY_COLOR_STANDARD)
            val range = int(android.media.MediaFormat.KEY_COLOR_RANGE)
            val hdr = when {
                type != MkvTrackType.VIDEO -> ""
                mime.startsWith("video/dolby-vision", true) -> "Dolby Vision"
                f.containsKey("hdr10-plus-info") -> "HDR10+"
                transfer == android.media.MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10 / PQ"
                transfer == android.media.MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
                transfer == android.media.MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> "SDR"
                else -> ""
            }
            val colorSpace = if (type != MkvTrackType.VIDEO) "" else listOfNotNull(
                when (standard) {
                    android.media.MediaFormat.COLOR_STANDARD_BT709 -> "BT.709"
                    android.media.MediaFormat.COLOR_STANDARD_BT601_PAL, android.media.MediaFormat.COLOR_STANDARD_BT601_NTSC -> "BT.601"
                    android.media.MediaFormat.COLOR_STANDARD_BT2020 -> "BT.2020"
                    else -> null
                },
                when (range) {
                    android.media.MediaFormat.COLOR_RANGE_FULL -> "Full range"
                    android.media.MediaFormat.COLOR_RANGE_LIMITED -> "Limited range"
                    else -> null
                }
            ).joinToString(" • ")

            val aspect = if (type == MkvTrackType.VIDEO && dispW > 0 && dispH > 0) {
                val g = gcdInt(dispW, dispH)
                "${dispW / g}:${dispH / g}"
            } else ""

            tracks.add(
                MkvTrackInfo(
                    id = i + 1,
                    trackNumber = (i + 1).toLong(),
                    trackUid = (i + 1).toLong(),
                    trackType = type,
                    name = "",
                    language = lang,
                    codecId = codecId,
                    codecName = codecName,
                    isDefault = int("is-default") == 1,
                    isForced = int("is-forced-subtitle") == 1,
                    pixelWidth = width,
                    pixelHeight = height,
                    displayWidth = if (type == MkvTrackType.VIDEO) dispW else 0,
                    displayHeight = if (type == MkvTrackType.VIDEO) dispH else 0,
                    frameRate = fps,
                    channels = int(android.media.MediaFormat.KEY_CHANNEL_COUNT),
                    sampleRate = int(android.media.MediaFormat.KEY_SAMPLE_RATE),
                    bitDepth = bitDepth,
                    bitrateBps = int(android.media.MediaFormat.KEY_BIT_RATE).toLong(),
                    durationMs = trackDurMs,
                    colorSpace = colorSpace,
                    hdrFormat = hdr,
                    aspectRatio = aspect,
                    formatProfile = profileName,
                    formatLevel = levelName,
                    formatTier = tierName,
                    originalTrackIndex = i,
                    originalTrackNumber = (i + 1).toLong()
                )
            )
        }
    } catch (_: Throwable) {
    } finally {
        try { extractor.release() } catch (_: Throwable) {}
    }
    if (tracks.isEmpty()) return null

    if (durationMs <= 0L) durationMs = tracks.maxOfOrNull { it.durationMs } ?: 0L
    // Frame rate fallbacks: frame count / duration, then the library's own scan value.
    tracks.forEach { t ->
        if (t.trackType == MkvTrackType.VIDEO && t.frameRate <= 0f) {
            if (frameCount > 0 && durationMs > 0) t.frameRate = (frameCount * 1000.0 / durationMs).toFloat()
            else if (video.framerate > 0.0) t.frameRate = video.framerate.toFloat()
        }
        if (t.durationMs <= 0L && durationMs > 0) t.durationMs = durationMs
    }

    val formattedDate = Regex("^(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})").find(date)?.let {
        val g = it.groupValues
        "${g[1]}-${g[2]}-${g[3]} ${g[4]}:${g[5]}:${g[6]} UTC"
    } ?: date
    val container = when (mimeType.lowercase(Locale.ROOT)) {
        "video/mp4" -> "MPEG-4"
        "video/quicktime" -> "QuickTime"
        "video/3gpp", "video/3gpp2" -> "3GPP"
        "video/mp2ts" -> "MPEG-TS"
        "video/avi", "video/x-msvideo" -> "AVI"
        "video/x-flv" -> "Flash Video"
        "video/ogg" -> "Ogg"
        else -> ""
    }
    return MediaInfoData(
        MkvContainerMetadata(
            fileName = video.displayName,
            filePath = video.path,
            fileSizeBytes = video.sizeBytes,
            durationMs = durationMs,
            title = title,
            dateUtc = formattedDate,
            containerFormat = container
        ),
        tracks, emptyList(), emptyList()
    )
}

/**
 * Exact compressed bytes per track (and bitrate / subtitle cue count) for non-Matroska files. Only sample sizes are read
 * (no media data is decoded), so it finishes quickly; the page is already visible while this runs.
 */
internal suspend fun scanExtractorTrackSizes(context: Context, video: VideoItem, data: MediaInfoData): List<MkvTrackInfo>? =
    withContext(Dispatchers.IO) {
        val extractor = android.media.MediaExtractor()
        try {
            val onDisk = video.path.isNotBlank() && !video.path.startsWith("content:") && File(video.path).exists()
            if (onDisk) extractor.setDataSource(video.path) else extractor.setDataSource(context, video.uri, null)
            val count = extractor.trackCount
            for (i in 0 until count) extractor.selectTrack(i)
            val bytes = LongArray(count)
            val samples = IntArray(count)
            var guard = 0L
            while (true) {
                val idx = extractor.sampleTrackIndex
                if (idx < 0 || idx >= count) break
                val size = extractor.sampleSize
                if (size > 0) bytes[idx] += size
                samples[idx]++
                if (!extractor.advance()) break
                if ((++guard and 0x3FFFL) == 0L) ensureActive()
                if (guard > 8_000_000L) break
            }
            data.tracks.map { original ->
                val t = original.copy()
                val b = bytes.getOrElse(t.originalTrackIndex) { 0L }
                if (b > 0L) {
                    t.streamSizeBytes = b
                    val dur = if (t.durationMs > 0L) t.durationMs else data.metadata.durationMs
                    if (dur > 0L) t.bitrateBps = (b.toDouble() * 8_000.0 / dur.toDouble()).toLong()
                }
                if (t.trackType == MkvTrackType.SUBTITLE) t.countOfElements = samples.getOrElse(t.originalTrackIndex) { 0 }
                t
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        } finally {
            try { extractor.release() } catch (_: Throwable) {}
        }
    }
