package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.R

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerMediaTrack
import com.example.player.SubtitleFontManager
import com.example.ui.state.PlayerSettings
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class SubtitleStyleAccordionSection {
    TYPOGRAPHY,
    COLORS,
    MISCELLANEOUS,
    ADVANCED,
    ADVANCED_ASS,
    RAW_TEXT
}

/**
 * Enhanced, Highly-Polished Subtitle Style Editor Panel
 *
 * Implements:
 * 1. Image 2 Style Typography Row (Single horizontal row with gradient selected style, direct Font Size slider,
 *    Border/Background/Shadow/Font tabs, and Letter/Word spacing slider).
 * 2. Image 1 3-Arc Circular Color Picker with 3 interactive curved arc sliders (full 0°–360° Hue rainbow spectrum, Saturation, Brightness/Value)
 *    and Center Preview circle.
 * 3. Functional state updates guaranteeing all subtitle settings remain independent and persistent without resetting each other.
 * 4. Full-featured, detailed Miscellaneous section.
 */
@Composable
fun SubtitleStylePanel(
    settings: PlayerSettings,
    context: android.content.Context,
    isLandscape: Boolean,
    glassBlurTransparency: Float,
    activeSubtitleTrack: PlayerMediaTrack? = null,
    onSettingsChange: (PlayerSettings) -> Unit,
    onDismissRequest: () -> Unit,
    onBackToSubtitleTracks: () -> Unit = {},
    onLoadAdvancedAssSource: (suspend (Int) -> com.example.player.AdvancedAssSource?)? = null,
    onLoadRawSubtitleSource: (suspend (Int) -> com.example.player.RawSubtitleSource?)? = null,
    onRawSubtitlePreview: (String?, String?) -> Unit = { _, _ -> }
) {
    val rawEditorDraft = remember { RawEditorDraft() }
    var activeSection by remember { mutableStateOf<SubtitleStyleAccordionSection?>(SubtitleStyleAccordionSection.TYPOGRAPHY) }

    val currentSettings by rememberUpdatedState(settings)
    val onSettingsChangeUpdated by rememberUpdatedState(onSettingsChange)

    val updateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit = remember {
        { transform ->
            val updated = transform(currentSettings)
            onSettingsChangeUpdated(updated)
        }
    }

    PlayerGlassModalSheet(
        panelKey = "subtitle_delay",
        isLandscape = isLandscape,
        openOnRightSide = true,
        heightFraction = if (isLandscape) 0.88f else 0.78f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismissRequest,
        headerStartContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StyledIcon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Subtitle Style",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalSheetNormalTextColor.current
                    )
                    if (activeSubtitleTrack != null) {
                        val trackLabel = activeSubtitleTrack.title.ifBlank { "Track ${activeSubtitleTrack.id}" }
                        Text(
                            text = "Target: $trackLabel",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        headerEndContent = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBackToSubtitleTracks() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Tracks",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // =========================================================================
            // 1. SECTION: TYPOGRAPHY
            // =========================================================================
            AccordionSectionCard(
                title = "Typography",
                icon = Icons.Outlined.TextFields,
                expanded = activeSection == SubtitleStyleAccordionSection.TYPOGRAPHY,
                onToggle = {
                    activeSection = if (activeSection == SubtitleStyleAccordionSection.TYPOGRAPHY) null else SubtitleStyleAccordionSection.TYPOGRAPHY
                }
            ) {
                EnhancedTypographyContent(
                    settings = currentSettings,
                    context = context,
                    onUpdateSettings = updateSettings
                )
            }

            // =========================================================================
            // 2. SECTION: COLORS
            // =========================================================================
            AccordionSectionCard(
                title = "Colors",
                icon = Icons.Outlined.ColorLens,
                expanded = activeSection == SubtitleStyleAccordionSection.COLORS,
                onToggle = {
                    activeSection = if (activeSection == SubtitleStyleAccordionSection.COLORS) null else SubtitleStyleAccordionSection.COLORS
                }
            ) {
                EnhancedColorsContent(
                    settings = currentSettings,
                    onUpdateSettings = updateSettings
                )
            }

            // =========================================================================
            // 3. SECTION: MISCELLANEOUS
            // =========================================================================
            AccordionSectionCard(
                title = "Miscellaneous",
                icon = Icons.Outlined.Tune,
                expanded = activeSection == SubtitleStyleAccordionSection.MISCELLANEOUS,
                onToggle = {
                    activeSection = if (activeSection == SubtitleStyleAccordionSection.MISCELLANEOUS) null else SubtitleStyleAccordionSection.MISCELLANEOUS
                }
            ) {
                EnhancedMiscContent(
                    settings = currentSettings,
                    isAssSsaTrack = isNativeAssSsaTrack(activeSubtitleTrack),
                    onUpdateSettings = updateSettings
                )
            }

            // =========================================================================
            // 4. SECTION: ADVANCED (holds the "Advanced ASS/SSA" and the "View Raw [Script Info]
            // & Subtitle text" switches; each switch adds its own section below when ON)
            // =========================================================================
            AccordionSectionCard(
                title = "Advanced",
                icon = Icons.Outlined.Code,
                expanded = activeSection == SubtitleStyleAccordionSection.ADVANCED,
                onToggle = {
                    activeSection = if (activeSection == SubtitleStyleAccordionSection.ADVANCED) null else SubtitleStyleAccordionSection.ADVANCED
                }
            ) {
                EnhancedAdvancedContent(
                    settings = currentSettings,
                    isAssSsaTrack = isNativeAssSsaTrack(activeSubtitleTrack),
                    onUpdateSettings = updateSettings,
                    onOpenSection = { activeSection = it }
                )
            }

            // =========================================================================
            // 5. SECTION: ADVANCED ASS/SSA (only when its switch in "Advanced" is ON, and only
            // for a genuinely native ASS/SSA track — per-style editing has nothing to act on
            // for any other subtitle format)
            // =========================================================================
            if (currentSettings.advancedAssEnabled && isNativeAssSsaTrack(activeSubtitleTrack)) {
                AccordionSectionCard(
                    title = "Advanced ASS/SSA",
                    icon = Icons.Outlined.Palette,
                    expanded = activeSection == SubtitleStyleAccordionSection.ADVANCED_ASS,
                    onToggle = {
                        activeSection = if (activeSection == SubtitleStyleAccordionSection.ADVANCED_ASS) null else SubtitleStyleAccordionSection.ADVANCED_ASS
                    }
                ) {
                    AdvancedAssStyleEditorContent(
                        settings = currentSettings,
                        context = context,
                        activeSubtitleTrack = activeSubtitleTrack,
                        onLoadAdvancedAssSource = onLoadAdvancedAssSource,
                        onUpdateSettings = updateSettings
                    )
                }
            }

            // =========================================================================
            // 6. SECTION: RAW [Script Info] & SUBTITLE TEXT (only when its switch is ON; works
            // for every subtitle format and source)
            // =========================================================================
            if (currentSettings.rawSubtitleEditorEnabled) {
                AccordionSectionCard(
                    title = "Raw [Script Info] & Subtitle Text",
                    icon = Icons.Outlined.Code,
                    expanded = activeSection == SubtitleStyleAccordionSection.RAW_TEXT,
                    onToggle = {
                        activeSection = if (activeSection == SubtitleStyleAccordionSection.RAW_TEXT) null else SubtitleStyleAccordionSection.RAW_TEXT
                    }
                ) {
                    RawSubtitleEditorContent(
                        settings = currentSettings,
                        context = context,
                        activeSubtitleTrack = activeSubtitleTrack,
                        draft = rawEditorDraft,
                        onLoadRawSubtitleSource = onLoadRawSubtitleSource,
                        onRawSubtitlePreview = onRawSubtitlePreview,
                        onUpdateSettings = updateSettings
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

// =============================================================================
// ACCORDION SECTION CONTAINER
// =============================================================================
@Composable
private fun AccordionSectionCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SheetButtonBgColor)
            .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIcon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = SheetButtonTextColor
            )
            StyledIcon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = Neutral.S475569,
                modifier = Modifier.size(19.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content()
            }
        }
    }
}
