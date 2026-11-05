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
                            name = video.displayName.substringBeforeLast('.', video.displayName),
                            codecName = "Container video stream",
                            pixelWidth = meta.width, pixelHeight = meta.height,
                            frameRate = meta.framerate.toFloat(), durationMs = meta.durationMs,
                            bitrateBps = if (meta.durationMs > 0) video.sizeBytes * 8_000L / meta.durationMs else 0L
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
                data = result
                // The page is already visible. Exact bitrate / stream sizes need a pass over every block of the file, so they
                // are filled in afterwards (same exact numbers, they just appear a moment later on big files).
                if (result.tracks.isNotEmpty() && result.metadata.fileSizeBytes > 0L && result.tracks.any { it.streamSizeBytes <= 0L }) {
                    val ext = video.displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (ext == "mkv" || ext == "webm" || ext == "mka") {
                        MkvManagerEngine.scanTrackSizes(context, video.uri, result.metadata, result.tracks)?.let { scanned ->
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
