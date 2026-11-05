package com.example.ui.components

import com.example.ui.theme.Neutral

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.MaterialTheme
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AdvancedAssSource
import com.example.player.AdvancedAssStyleEngine
import com.example.player.AssFields
import com.example.player.AssOverridesStore
import com.example.player.AssStyleInfo
import com.example.player.AssStylesDocument
import com.example.player.AssSubtitleOverrides
import com.example.player.PlayerMediaTrack
import com.example.player.SubtitleFontManager
import com.example.ui.state.PlayerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class LoadedAssDocument(
    val key: String,
    val title: String,
    val document: AssStylesDocument
)

/**
 * Advanced ASS/SSA editor: one expandable card per Style found in the CURRENT ASS/SSA subtitle
 * (nothing is hard-coded). All edits are stored as per-subtitle overrides inside
 * [PlayerSettings.assStyleOverridesJson] (the single source of truth) and are applied to the
 * video by MpvPlayerController.setAdvancedAssState via the existing settings pipeline.
 */
@Composable
internal fun AdvancedAssStyleEditorContent(
    settings: PlayerSettings,
    context: Context,
    activeSubtitleTrack: PlayerMediaTrack?,
    onLoadAdvancedAssSource: (suspend (Int) -> AdvancedAssSource?)?,
    onUpdateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit
) {
    val trackId = activeSubtitleTrack?.id
    val loadSource by rememberUpdatedState(onLoadAdvancedAssSource)
    var loaded by remember { mutableStateOf<LoadedAssDocument?>(null) }
    var loadFinished by remember { mutableStateOf(false) }

    LaunchedEffect(trackId) {
        loadFinished = false
        val loader = loadSource
        val source = if (trackId == null || loader == null) {
            null
        } else {
            try { loader(trackId) } catch (_: Throwable) { null }
        }
        if (source == null) {
            loaded = null
        } else if (loaded?.key != source.key) {
            val parsed = withContext(Dispatchers.Default) { AdvancedAssStyleEngine.parse(source.text) }
            loaded = parsed?.let { LoadedAssDocument(source.key, source.title, it) }
        }
        loadFinished = true
    }

    val current = loaded
    if (current == null) {
        AdvancedAssInfoCard(
            when {
                trackId == null -> "Select an ASS/SSA subtitle track to edit its styles."
                !loadFinished -> "Reading subtitle styles…"
                else -> "The selected subtitle is not an ASS/SSA subtitle (or could not be read), so there are no styles to edit. Existing subtitle settings keep working normally."
            }
        )
        return
    }

    val document = current.document
    val subtitleKey = current.key
    val overridesAll = remember(settings.assStyleOverridesJson) {
        AssOverridesStore.decode(settings.assStyleOverridesJson)
    }
    val overrides = overridesAll[subtitleKey] ?: AssSubtitleOverrides()

    val updateOverrides: ((AssSubtitleOverrides) -> AssSubtitleOverrides) -> Unit = { transform ->
        onUpdateSettings { latest ->
            val existing = AssOverridesStore.get(latest.assStyleOverridesJson, subtitleKey)
            latest.copy(
                assStyleOverridesJson = AssOverridesStore.put(
                    latest.assStyleOverridesJson, subtitleKey, transform(existing)
                )
            )
        }
    }

    val fontFamilies by produceState<List<String>>(emptyList(), settings.subtitleFontsReloadNonce, settings.showVideoEmbeddedSubtitleFonts) {
        value = withContext(Dispatchers.IO) {
            try {
                SubtitleFontManager.getInstalledFonts(
                    context,
                    includeVideoExtracted = settings.showVideoEmbeddedSubtitleFonts
                )
                    .map { SubtitleFontManager.getOriginalFontName(it) }
                    .filter { it.isNotBlank() && ',' !in it }
                    .distinct()
                    .sortedBy { it.lowercase() }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    val expandedSaver = remember {
        listSaver<androidx.compose.runtime.MutableState<List<String>>, String>(
            save = { it.value },
            restore = { mutableStateOf(it) }
        )
    }
    var expandedStyles by rememberSaveable(saver = expandedSaver) { mutableStateOf(emptyList<String>()) }

    val visibleStyles = document.styles.filter { style ->
        !(style.canBeRemoved && overrides.deleted.contains(style.name))
    }
    fun effectiveName(style: AssStyleInfo): String =
        overrides.fieldOf(style.name, AssFields.NAME)?.takeIf { !style.isDefault } ?: style.name

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "${visibleStyles.size} style(s) in \"${current.title}\". Changes apply live to this subtitle only.",
            fontSize = 11.5.sp,
            color = LocalSheetSecondaryTextColor.current
        )
        if (!document.isPlusFormat) {
            Text(
                text = "Classic SSA (V4) script: underline, strike-out, scale, spacing and angle are not part of this format and are disabled.",
                fontSize = 11.sp,
                color = LocalSheetSecondaryTextColor.current
            )
        }

        visibleStyles.forEach { style ->
            key(style.name) {
                val otherNames = visibleStyles.filter { it !== style }.map { effectiveName(it) }
                AssStyleCard(
                    document = document,
                    style = style,
                    overrides = overrides,
                    expanded = expandedStyles.contains(style.name),
                    onToggleExpanded = {
                        expandedStyles = if (expandedStyles.contains(style.name)) {
                            expandedStyles - style.name
                        } else {
                            expandedStyles + style.name
                        }
                    },
                    fontFamilies = fontFamilies,
                    otherStyleNames = otherNames,
                    onOverridesChange = updateOverrides
                )
            }
        }

        val removedCount = document.styles.count { it.canBeRemoved && overrides.deleted.contains(it.name) }
        if (removedCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SheetButtonBgColor)
                    .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                    .clickable { updateOverrides { it.restoreDeleted() } }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Restore $removedCount removed style(s)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AdvancedAssInfoCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SheetButtonBgColor)
            .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = message, fontSize = 12.5.sp, color = SheetButtonTextColor)
    }
}

// =============================================================================
// ONE STYLE CARD
// =============================================================================
@Composable
private fun AssStyleCard(
    document: AssStylesDocument,
    style: AssStyleInfo,
    overrides: AssSubtitleOverrides,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    fontFamilies: List<String>,
    otherStyleNames: List<String>,
    onOverridesChange: ((AssSubtitleOverrides) -> AssSubtitleOverrides) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)

    fun eff(field: String): String = overrides.fieldOf(style.name, field) ?: document.read(style, field)
    fun set(field: String, value: String) {
        onOverridesChange { it.withField(style.name, field, value) }
    }

    var fontPickerOpen by remember { mutableStateOf(false) }
    var openColorField by remember { mutableStateOf<String?>(null) }

    val displayName = if (style.isDefault) style.name else eff(AssFields.NAME)
    val primaryArgb = AssFields.parseColourToArgb(eff(AssFields.PRIMARY_COLOUR)) ?: 0xFFFFFFFFL
    val hasChanges = overrides.styles[style.name]?.isNotEmpty() == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SheetButtonBgColor)
            .border(if (expanded) 1.5.dp else 1.dp, if (expanded) primary else SheetButtonBorderColor, shape)
    ) {
        // ---- collapsed header: name, font summary, delete, expand/collapse ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Neutral.S94A3B8)
                    .border(1.dp, SheetButtonBorderColor, CircleShape)
            ) {
                Box(modifier = Modifier.size(22.dp).background(Color(primaryArgb.toInt())))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SheetButtonTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${eff(AssFields.FONT_NAME)} ${eff(AssFields.FONT_SIZE)}pt",
                    fontSize = 10.5.sp,
                    color = SheetButtonTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.weight(0.001f))
            if (style.canBeRemoved) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onOverridesChange { it.withDeleted(style.name) } },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove style",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            StyledIcon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Neutral.S475569,
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 520.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ---------------- 1. Font & General Properties ----------------
                    AssSectionTitle("1. Font & General Properties")
                    AssFieldRow {
                        AssTextField(
                            label = "Name",
                            value = displayName,
                            enabled = !style.isDefault,
                            numeric = false,
                            modifier = Modifier.weight(1f),
                            validate = { input ->
                                AssFields.normalize(AssFields.NAME, input)
                                    ?.takeIf { n -> otherStyleNames.none { it.equals(n, ignoreCase = true) } }
                            },
                            onCommit = { set(AssFields.NAME, it) }
                        )
                        AssTextField(
                            label = "Fontsize",
                            value = eff(AssFields.FONT_SIZE),
                            modifier = Modifier.weight(if (wide) 0.5f else 0.6f),
                            validate = { AssFields.normalize(AssFields.FONT_SIZE, it) },
                            onCommit = { set(AssFields.FONT_SIZE, it) }
                        )
                    }
                    AssFontField(
                        currentFont = eff(AssFields.FONT_NAME),
                        originalFont = document.read(style, AssFields.FONT_NAME),
                        open = fontPickerOpen,
                        families = fontFamilies,
                        onToggle = { fontPickerOpen = !fontPickerOpen },
                        onSelectOriginal = {
                            onOverridesChange { it.withoutField(style.name, AssFields.FONT_NAME) }
                            fontPickerOpen = false
                        },
                        onSelectFamily = { family ->
                            AssFields.normalize(AssFields.FONT_NAME, family)?.let { set(AssFields.FONT_NAME, it) }
                            fontPickerOpen = false
                        }
                    )
                    AssAlignmentRow(
                        selected = eff(AssFields.ALIGNMENT).toIntOrNull() ?: 2,
                        onSelect = { set(AssFields.ALIGNMENT, it.toString()) }
                    )

                    // ---------------- 2. Colors ----------------
                    AssSectionTitle("2. Color Settings (ASS Hex Format)")
                    val colourFields = listOf(
                        AssFields.PRIMARY_COLOUR to "PrimaryColour",
                        AssFields.SECONDARY_COLOUR to "SecondaryColour",
                        AssFields.OUTLINE_COLOUR to "OutlineColour",
                        AssFields.BACK_COLOUR to "BackColour"
                    )
                    colourFields.chunked(if (wide) 2 else 1).forEach { rowFields ->
                        // The colour row and ITS OWN picker are one child of the parent column, so
                        // the picker opens directly below the colour that was touched and a
                        // collapsed picker adds no extra spacing.
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AssFieldRow {
                                rowFields.forEach { (field, label) ->
                                    AssColourField(
                                        label = label,
                                        value = eff(field),
                                        enabled = document.supports(field),
                                        active = openColorField == field,
                                        modifier = Modifier.weight(1f),
                                        onClick = { openColorField = if (openColorField == field) null else field }
                                    )
                                }
                            }
                            AssInlineColorPicker(
                                fields = rowFields.map { it.first },
                                openField = openColorField,
                                titleOf = { f -> colourFields.firstOrNull { it.first == f }?.second ?: "Colour" },
                                colourOf = { f -> AssFields.parseColourToArgb(eff(f)) ?: 0xFFFFFFFFL },
                                onColorChange = { f, argb -> set(f, AssFields.argbToAssColour(argb)) }
                            )
                        }
                    }

                    // ---------------- 3. Text Formatting Flags ----------------
                    AssSectionTitle("3. Text Formatting Flags")
                    val flagFields = listOf(
                        AssFields.BOLD to "Bold",
                        AssFields.ITALIC to "Italic",
                        AssFields.UNDERLINE to "Underline",
                        AssFields.STRIKE_OUT to "StrikeOut"
                    )
                    val anyFlag = flagFields.any { eff(it.first) == "-1" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssChip(
                            text = "None",
                            selected = !anyFlag,
                            enabled = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onOverridesChange { o ->
                                    flagFields.filter { document.supports(it.first) }
                                        .fold(o) { acc, f -> acc.withField(style.name, f.first, "0") }
                                }
                            }
                        )
                        flagFields.forEach { (field, label) ->
                            AssChip(
                                text = label,
                                selected = eff(field) == "-1",
                                enabled = document.supports(field),
                                modifier = Modifier.weight(1f),
                                onClick = { set(field, if (eff(field) == "-1") "0" else "-1") }
                            )
                        }
                    }

                    // ---------------- 4. Transformation & Geometry ----------------
                    AssSectionTitle("4. Transformation & Geometry")
                    val geometry: List<Triple<String, String, Pair<Boolean, Boolean>>> = listOf(
                        Triple(AssFields.SCALE_X, "ScaleX", false to false),
                        Triple(AssFields.SCALE_Y, "ScaleY", false to false),
                        Triple(AssFields.SPACING, "Spacing", true to false),
                        Triple(AssFields.ANGLE, "Angle", true to false)
                    )
                    geometry.chunked(if (wide) 4 else 2).forEach { rowItems ->
                        AssFieldRow {
                            rowItems.forEach { (field, label, flags) ->
                                AssTextField(
                                    label = label,
                                    value = eff(field),
                                    enabled = document.supports(field),
                                    allowNegative = flags.first,
                                    modifier = Modifier.weight(1f),
                                    validate = { AssFields.normalize(field, it) },
                                    onCommit = { set(field, it) }
                                )
                            }
                        }
                    }

                    // ---------------- 5. Border, Outline & Shadow ----------------
                    AssSectionTitle("5. Border, Outline & Shadow")
                    AssFieldRow {
                        AssTextField(
                            label = "BorderStyle",
                            value = eff(AssFields.BORDER_STYLE),
                            enabled = document.supports(AssFields.BORDER_STYLE),
                            modifier = Modifier.weight(1f),
                            validate = { AssFields.normalize(AssFields.BORDER_STYLE, it) },
                            onCommit = { set(AssFields.BORDER_STYLE, it) }
                        )
                        AssTextField(
                            label = "Outline",
                            value = eff(AssFields.OUTLINE),
                            enabled = document.supports(AssFields.OUTLINE),
                            modifier = Modifier.weight(1f),
                            validate = { AssFields.normalize(AssFields.OUTLINE, it) },
                            onCommit = { set(AssFields.OUTLINE, it) }
                        )
                        AssTextField(
                            label = "Shadow",
                            value = eff(AssFields.SHADOW),
                            enabled = document.supports(AssFields.SHADOW),
                            allowNegative = true,
                            modifier = Modifier.weight(1f),
                            validate = { AssFields.normalize(AssFields.SHADOW, it) },
                            onCommit = { set(AssFields.SHADOW, it) }
                        )
                    }

                    // ---------------- 6. Margins & Encoding ----------------
                    AssSectionTitle("6. Margins (L/R/V) & Encoding")
                    val margins = listOf(
                        Triple(AssFields.MARGIN_L, "MarginL", false),
                        Triple(AssFields.MARGIN_R, "MarginR", false),
                        Triple(AssFields.MARGIN_V, "MarginV", false),
                        Triple(AssFields.ENCODING, "Encoding", true)
                    )
                    margins.chunked(if (wide) 4 else 2).forEach { rowItems ->
                        AssFieldRow {
                            rowItems.forEach { (field, label, negative) ->
                                AssTextField(
                                    label = label,
                                    value = eff(field),
                                    enabled = document.supports(field),
                                    allowNegative = negative,
                                    modifier = Modifier.weight(1f),
                                    validate = { AssFields.normalize(field, it) },
                                    onCommit = { set(field, it) }
                                )
                            }
                        }
                    }

                    if (hasChanges) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                                .clickable {
                                    openColorField = null
                                    fontPickerOpen = false
                                    onOverridesChange { it.resetStyle(style.name) }
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Reset this style to original",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SheetButtonTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}
