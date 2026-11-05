package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.screens.LocalGlassBlurTransparency
import com.example.ui.screens.VideoItem
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.LocalAppFontFamily
import com.example.ui.theme.Neutral
import com.example.util.BulkCaseFormat
import com.example.util.BulkRenameConfig
import com.example.util.BulkRenameEngine
import com.example.util.BulkRenameItem
import com.example.util.BulkRenameMode
import com.example.util.BulkRenameStatus
import com.example.util.NaturalOrderComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BulkRenameBody(
    isDark: Boolean,
    videos: List<VideoItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<Pair<VideoItem, String>>) -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val panelBg = if (isDark) Color.White.copy(alpha = 0.07f) else Neutral.D0F172A.copy(alpha = 0.06f)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ---- the files ------------------------------------------------------------------------------
    val items = remember(videos) {
        videos.map { v ->
            val f = File(v.path)
            BulkRenameItem(key = v.path, dir = f.parent ?: "", fileName = f.name)
        }.sortedWith { a, b -> NaturalOrderComparator.compare(a.fileName, b.fileName) }
    }
    val itemByKey = remember(items) { items.associateBy { it.key } }
    val videoByKey = remember(videos) { videos.associateBy { it.path } }
    val order = remember(items) { mutableStateListOf<String>().apply { addAll(items.map { it.key }) } }

    val names = remember(items) { items.map { it.fileName } }
    val detectedCount = remember(names) { BulkRenameEngine.countEpisodesDetected(names) }
    val autoSeries = remember(names) { BulkRenameEngine.detectSeriesName(names) }
    val autoPadding = remember(names) {
        max(2, BulkRenameEngine.maxDetectedEpisode(names).toString().length).coerceAtMost(4)
    }

    // ---- options --------------------------------------------------------------------------------
    var mode by remember(items) {
        mutableStateOf(
            if (items.size > 1 && detectedCount * 10 >= items.size * 8) BulkRenameMode.SMART_EPISODE
            else BulkRenameMode.SEQUENCE
        )
    }
    var keepExt by remember { mutableStateOf(true) }
    var pattern by remember { mutableStateOf("File_") }
    var startText by remember { mutableStateOf("1") }
    var padding by remember { mutableIntStateOf(2) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var caseFormat by remember { mutableStateOf(BulkCaseFormat.LOWER) }
    var seriesName by remember(autoSeries) { mutableStateOf(autoSeries) }
    var seasonText by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(BulkRenameConfig.DEFAULT_TEMPLATE) }
    var episodePadding by remember(autoPadding) { mutableIntStateOf(autoPadding) }

    // names already present in the folders (a rename never overwrites them)
    var taken by remember(items) { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    LaunchedEffect(items) {
        taken = withContext(Dispatchers.IO) {
            val own = items.map { it.dir + "\u0000" + it.fileName.lowercase() }.toSet()
            items.map { it.dir }.distinct().associateWith { dir ->
                (File(dir).list() ?: emptyArray<String>())
                    .map { it.lowercase() }
                    .filter { (dir + "\u0000" + it) !in own }
                    .toSet()
            }
        }
    }

    val cfg = BulkRenameConfig(
        mode = mode,
        keepExtension = keepExt,
        pattern = pattern,
        startNumber = startText.toIntOrNull() ?: 1,
        padding = padding,
        find = findText,
        replace = replaceText,
        caseSensitive = caseSensitive,
        prefix = prefix,
        suffix = suffix,
        caseFormat = caseFormat,
        seriesName = seriesName,
        seasonOverride = seasonText.toIntOrNull(),
        template = template,
        episodePadding = episodePadding
    )

    // ---- drag & drop ordering of the preview ------------------------------------------------------
    var draggingKey by remember(items) { mutableStateOf<String?>(null) }
    var dragOffsetY by remember(items) { mutableFloatStateOf(0f) }
    var settlingKey by remember(items) { mutableStateOf<String?>(null) }
    var settleOffset by remember(items) { mutableFloatStateOf(0f) }
    val pitchPx = with(density) { (ROW_H_DP + ROW_GAP_DP).dp.toPx() }
    val topPadPx = with(density) { LIST_PAD_DP.dp.toPx() }

    val fromIndex by remember(items) {
        derivedStateOf { draggingKey?.let { order.indexOf(it) } ?: -1 }
    }
    val hoverIndex by remember(items) {
        derivedStateOf {
            if (fromIndex >= 0) (fromIndex + (dragOffsetY / pitchPx).roundToInt()).coerceIn(0, order.lastIndex)
            else -1
        }
    }
    // the order the numbers are calculated from – follows the dragged row live
    val effectiveKeys: List<String> =
        if (fromIndex >= 0 && hoverIndex >= 0 && hoverIndex != fromIndex) {
            order.toMutableList().also { it.add(hoverIndex, it.removeAt(fromIndex)) }
        } else order.toList()

    val previews = remember(effectiveKeys, cfg, taken) {
        BulkRenameEngine.compute(effectiveKeys.mapNotNull { itemByKey[it] }, cfg, taken)
    }
    val previewByKey = remember(previews) { previews.associateBy { it.item.key } }
    val positionByKey = remember(effectiveKeys) {
        effectiveKeys.withIndex().associate { it.value to it.index + 1 }
    }
    val changeCount = previews.count { it.status == BulkRenameStatus.CHANGED }
    val conflictCount = previews.count { it.status == BulkRenameStatus.CONFLICT }
    val noMatchCount = previews.count { it.status == BulkRenameStatus.NO_MATCH }
    val canRename = changeCount > 0 && conflictCount == 0
    val dragEnabled = mode == BulkRenameMode.SEQUENCE

    fun finishDrag() {
        val k = draggingKey ?: return
        val f = order.indexOf(k)
        if (f < 0) {
            draggingKey = null
            dragOffsetY = 0f
            return
        }
        val h = (f + (dragOffsetY / pitchPx).roundToInt()).coerceIn(0, max(0, order.lastIndex))
        val residual = dragOffsetY - (h - f) * pitchPx
        if (h != f) order.add(h, order.removeAt(f))
        settleOffset = residual
        settlingKey = k
        draggingKey = null
        dragOffsetY = 0f
        scope.launch {
            animate(
                initialValue = residual,
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
            ) { v, _ -> settleOffset = v }
            settlingKey = null
        }
    }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    var previewHeightPx by remember { mutableIntStateOf(0) }
    var previewWidthPx by remember { mutableIntStateOf(0) }

    // scroll the list while a row is held near its top / bottom edge
    LaunchedEffect(draggingKey) {
        if (draggingKey == null) return@LaunchedEffect
        while (isActive) {
            val k = draggingKey ?: break
            val f = order.indexOf(k)
            if (f >= 0 && previewHeightPx > 0) {
                val center = topPadPx + f * pitchPx + pitchPx / 2f + dragOffsetY - vScroll.value
                val edge = pitchPx * 0.8f
                val delta = when {
                    center < edge -> -16f
                    center > previewHeightPx - edge -> 16f
                    else -> 0f
                }
                if (delta != 0f) {
                    val consumed = vScroll.scrollBy(delta)
                    dragOffsetY += consumed
                }
            }
            delay(16)
        }
    }

    // ---- header ---------------------------------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(AccentSkyBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.DriveFileRenameOutline,
                contentDescription = null,
                tint = AccentSkyBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Bulk Rename", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = primaryText)
            Text("${items.size} files selected", fontSize = 13.sp, color = secondaryText)
        }
    }

    // ---- mode chips -----------------------------------------------------------------------------
    // one row, swiped left / right (no tall wrapped block)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BulkChip("Smart Episode", mode == BulkRenameMode.SMART_EPISODE, panelBg, primaryText) { mode = BulkRenameMode.SMART_EPISODE }
        BulkChip("Sequence #", mode == BulkRenameMode.SEQUENCE, panelBg, primaryText) { mode = BulkRenameMode.SEQUENCE }
        BulkChip("Find & Replace", mode == BulkRenameMode.FIND_REPLACE, panelBg, primaryText) { mode = BulkRenameMode.FIND_REPLACE }
        BulkChip("Prefix / Suffix", mode == BulkRenameMode.PREFIX_SUFFIX, panelBg, primaryText) { mode = BulkRenameMode.PREFIX_SUFFIX }
        BulkChip("Change Case", mode == BulkRenameMode.CHANGE_CASE, panelBg, primaryText) { mode = BulkRenameMode.CHANGE_CASE }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { keepExt = !keepExt },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = keepExt,
            onCheckedChange = { keepExt = it },
            colors = CheckboxDefaults.colors(checkedColor = AccentSkyBlue, uncheckedColor = secondaryText)
        )
        Text("Keep original file extensions", fontSize = 15.sp, color = secondaryText)
    }

    // ---- options of the chosen mode -------------------------------------------------------------
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(panelBg)
            .padding(14.dp)
            .animateContentSize()
    ) {
        Crossfade(targetState = mode, label = "bulkRenameMode") { m ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (m) {
                    BulkRenameMode.SMART_EPISODE -> {
                        BulkField("Series name", seriesName, { seriesName = it }, isDark, primaryText, secondaryText,
                            placeholder = "Auto-detect from each file")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BulkField("Season", seasonText, { seasonText = it.filter { c -> c.isDigit() }.take(2) },
                                isDark, primaryText, secondaryText, Modifier.weight(1f),
                                keyboardType = KeyboardType.Number, placeholder = "Auto")
                            BulkField("Name format", template, { template = it }, isDark, primaryText, secondaryText,
                                Modifier.weight(2f))
                        }
                        Text(
                            "Tip: {name} {season} {episode} – the episode number is read from each file name and is never changed.",
                            fontSize = 12.sp, lineHeight = 18.sp, color = secondaryText
                        )
                        PaddingSelector("Episode digits", episodePadding, { episodePadding = it }, panelBg, primaryText, secondaryText)
                        Text(
                            "Episode number found in $detectedCount of ${items.size} files" +
                                if (detectedCount < items.size) " – the others are left unchanged." else ".",
                            fontSize = 12.sp, lineHeight = 18.sp,
                            color = if (detectedCount < items.size) WarnAmber else secondaryText
                        )
                    }
                    BulkRenameMode.SEQUENCE -> {
                        BulkField("Name Pattern / Prefix", pattern, { pattern = it }, isDark, primaryText, secondaryText)
                        Text(
                            "Tip: Use {num} to position sequence numbers (e.g. Doc_{num}_2026)",
                            fontSize = 12.sp, lineHeight = 18.sp, color = secondaryText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                            BulkField("Start #", startText, { startText = it.filter { c -> c.isDigit() }.take(6) },
                                isDark, primaryText, secondaryText, Modifier.weight(0.8f),
                                keyboardType = KeyboardType.Number)
                            Box(Modifier.weight(1.4f)) {
                                PaddingSelector("Digits Padding", padding, { padding = it }, panelBg, primaryText, secondaryText)
                            }
                        }
                    }
                    BulkRenameMode.FIND_REPLACE -> {
                        BulkField("Find Text", findText, { findText = it }, isDark, primaryText, secondaryText)
                        BulkField("Replace With", replaceText, { replaceText = it }, isDark, primaryText, secondaryText)
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { caseSensitive = !caseSensitive },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = caseSensitive,
                                onCheckedChange = { caseSensitive = it },
                                colors = CheckboxDefaults.colors(checkedColor = AccentSkyBlue, uncheckedColor = secondaryText)
                            )
                            Text("Case sensitive matching", fontSize = 14.sp, color = secondaryText)
                        }
                    }
                    BulkRenameMode.PREFIX_SUFFIX -> {
                        BulkField("Add Prefix (Start)", prefix, { prefix = it }, isDark, primaryText, secondaryText)
                        BulkField("Add Suffix (End)", suffix, { suffix = it }, isDark, primaryText, secondaryText)
                    }
                    BulkRenameMode.CHANGE_CASE -> {
                        Text("Select Letter Case Format", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = secondaryText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallChoice("lowercase", caseFormat == BulkCaseFormat.LOWER, panelBg, primaryText, Modifier.weight(1f)) { caseFormat = BulkCaseFormat.LOWER }
                            SmallChoice("UPPERCASE", caseFormat == BulkCaseFormat.UPPER, panelBg, primaryText, Modifier.weight(1f)) { caseFormat = BulkCaseFormat.UPPER }
                            SmallChoice("Title Case", caseFormat == BulkCaseFormat.TITLE, panelBg, primaryText, Modifier.weight(1f)) { caseFormat = BulkCaseFormat.TITLE }
                        }
                    }
                }
            }
        }
    }

    // ---- live preview ---------------------------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Live Preview ($changeCount change${if (changeCount == 1) "" else "s"})",
            fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AccentSkyBlue
        )
        if (conflictCount > 0) {
            Spacer(Modifier.width(10.dp))
            Text("$conflictCount name clash${if (conflictCount == 1) "" else "es"}", fontSize = 12.5.sp, color = ConflictRed)
        } else if (noMatchCount > 0) {
            Spacer(Modifier.width(10.dp))
            Text("$noMatchCount skipped", fontSize = 12.5.sp, color = WarnAmber)
        }
    }

    val previewShape = RoundedCornerShape(18.dp)
    val listHeight = (ROW_H_DP * 3 + ROW_GAP_DP * 2 + LIST_PAD_DP * 2).dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(listHeight)
            .clip(previewShape)
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.85f))
            .border(1.dp, secondaryText.copy(alpha = 0.25f), previewShape)
            .onSizeChanged {
                previewHeightPx = it.height
                previewWidthPx = it.width
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(vScroll)) {
            Box(modifier = Modifier.horizontalScroll(hScroll)) {
                val minWidthDp = with(density) { (previewWidthPx - (LIST_PAD_DP * 2).dp.roundToPx()).coerceAtLeast(0).toDp() }
                Column(
                    modifier = Modifier
                        .padding(LIST_PAD_DP.dp)
                        .widthIn(min = minWidthDp)
                        .width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.spacedBy(ROW_GAP_DP.dp)
                ) {
                    order.forEachIndexed { index, rowKey ->
                        key(rowKey) {
                            val preview = previewByKey[rowKey]
                            val item = itemByKey[rowKey]
                            if (preview != null && item != null) {
                                val isDragged = draggingKey == rowKey
                                val isSettling = settlingKey == rowKey
                                val shift = when {
                                    isDragged || isSettling -> 0f
                                    fromIndex >= 0 && hoverIndex >= 0 -> when {
                                        fromIndex < hoverIndex && index in (fromIndex + 1)..hoverIndex -> -pitchPx
                                        fromIndex > hoverIndex && index in hoverIndex until fromIndex -> pitchPx
                                        else -> 0f
                                    }
                                    else -> 0f
                                }
                                val spec: AnimationSpec<Float> =
                                    if (draggingKey == null) snap() else spring(dampingRatio = 0.9f, stiffness = 800f)
                                val animShift = animateFloatAsState(targetValue = shift, animationSpec = spec, label = "bulkRowShift")
                                val rowShape = RoundedCornerShape(14.dp)
                                val newColor = when (preview.status) {
                                    BulkRenameStatus.CHANGED -> AccentSkyBlue
                                    BulkRenameStatus.UNCHANGED -> secondaryText
                                    BulkRenameStatus.CONFLICT -> ConflictRed
                                    BulkRenameStatus.NO_MATCH -> WarnAmber
                                }
                                val newText = when (preview.status) {
                                    BulkRenameStatus.NO_MATCH -> "No episode number found – kept as is"
                                    BulkRenameStatus.CONFLICT ->
                                        preview.newName + "   (" + (preview.conflictReason ?: "name clash") + ")"
                                    else -> preview.newName
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ROW_H_DP.dp)
                                        .zIndex(if (isDragged || isSettling) 10f else 0f)
                                        .graphicsLayer {
                                            translationY = when {
                                                isDragged -> dragOffsetY
                                                isSettling -> settleOffset
                                                else -> animShift.value
                                            }
                                            if (isDragged) {
                                                scaleX = 1.02f
                                                scaleY = 1.02f
                                                shadowElevation = 14.dp.toPx()
                                                shape = rowShape
                                                clip = false
                                            }
                                        }
                                        .clip(rowShape)
                                        .background(
                                            if (isDragged) {
                                                if (isDark) Neutral.D1E293B else Neutral.F1F5F9
                                            } else if (isDark) Color.White.copy(alpha = 0.06f) else Neutral.F1F5F9.copy(alpha = 0.75f)
                                        )
                                        .then(
                                            if (isDragged) Modifier.border(2.dp, AccentGradient, rowShape)
                                            else Modifier
                                        )
                                        .pointerInput(rowKey, dragEnabled) {
                                            if (!dragEnabled) return@pointerInput
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    try { com.example.util.AppHaptics.perform(haptic) } catch (_: Throwable) {}
                                                    draggingKey = rowKey
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (draggingKey == rowKey) dragOffsetY += dragAmount.y
                                                },
                                                onDragEnd = { finishDrag() },
                                                onDragCancel = { finishDrag() }
                                            )
                                        }
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(26.dp).clip(CircleShape).background(AccentSkyBlue.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${positionByKey[rowKey] ?: (index + 1)}",
                                            fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                                            color = AccentSkyBlue, maxLines = 1, softWrap = false
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            item.fileName, fontSize = 11.5.sp, color = secondaryText,
                                            maxLines = 1, softWrap = false
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            StyledIcon(
                                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                contentDescription = null,
                                                tint = newColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                newText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                                                color = newColor, maxLines = 1, softWrap = false
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Text(
        text = if (dragEnabled) "Touch & hold a file, then drag it up or down to change its number. Swipe left / right to read full names."
        else "Swipe left / right to read the full names.",
        fontSize = 11.5.sp, lineHeight = 16.sp, color = secondaryText,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
    )

    // ---- buttons --------------------------------------------------------------------------------
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AccentSkyBlue)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(CircleShape)
                .then(
                    if (canRename) Modifier.background(AccentGradient)
                    else Modifier.background(secondaryText.copy(alpha = 0.18f))
                )
                .clickable(enabled = canRename) {
                    val list = previews
                        .filter { it.status == BulkRenameStatus.CHANGED }
                        .mapNotNull { p -> videoByKey[p.item.key]?.let { v -> v to p.newName } }
                    onConfirm(list)
                }
                .padding(horizontal = 34.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Rename $changeCount File(s)",
                fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = if (canRename) Color.White else secondaryText.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun BulkChip(
    label: String,
    selected: Boolean,
    idleBg: Color,
    primaryText: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .then(if (selected) Modifier.background(AccentGradient) else Modifier.background(idleBg))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false,
            color = if (selected) Color.White else primaryText.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun SmallChoice(
    label: String,
    selected: Boolean,
    idleBg: Color,
    primaryText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.background(AccentGradient) else Modifier.background(idleBg))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            color = if (selected) Color.White else primaryText
        )
    }
}

@Composable
private fun PaddingSelector(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit,
    idleBg: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Column {
        Text(label, fontSize = 11.5.sp, color = secondaryText, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (n in 1..4) {
                val chipLabel = "0".repeat(n - 1) + "1"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (selected == n) Modifier.background(AccentGradient) else Modifier.background(idleBg))
                        .clickable { onSelect(n) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        chipLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        color = if (selected == n) Color.White else primaryText
                    )
                }
            }
        }
    }
}

/** Labelled text box in the app's field style. The caret keeps long text in view; the whole box is tappable. */
@Composable
private fun BulkField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = modifier) {
        Text(
            label, fontSize = 11.5.sp,
            color = if (focused) AccentSkyBlue else secondaryText,
            modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.40f) else Neutral.F1F5F9.copy(alpha = 0.70f))
                .border(
                    if (focused) 1.5.dp else 1.dp,
                    if (focused) AccentSkyBlue else secondaryText.copy(alpha = 0.45f),
                    shape
                )
        ) {
            FollowCursorTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                minHeight = 40.dp,
                placeholder = placeholder,
                placeholderColor = secondaryText.copy(alpha = 0.6f),
                textStyle = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontSize = 14.sp,
                    color = primaryText
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                onFocusChanged = { focused = it }
            )
        }
    }
}
