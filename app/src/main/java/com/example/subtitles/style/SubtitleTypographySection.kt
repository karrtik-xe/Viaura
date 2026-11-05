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

enum class TypographySubTab {
    BORDER,
    BACKGROUND,
    SHADOW,
    FONT
}

// =============================================================================
// 1. ENHANCED TYPOGRAPHY SECTION (Image 2 style)
// =============================================================================
@Composable
internal fun EnhancedTypographyContent(
    settings: PlayerSettings,
    context: android.content.Context,
    onUpdateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf<TypographySubTab>(TypographySubTab.BORDER) }

    val installedFonts = remember(settings.subtitleFontsReloadNonce, settings.showVideoEmbeddedSubtitleFonts) {
        SubtitleFontManager.getInstalledFonts(
            context,
            includeVideoExtracted = settings.showVideoEmbeddedSubtitleFonts
        )
    }

    // -------------------------------------------------------------------------
    // Top Row: [ B ]  [ I ]  [ ≡ ]  [ ≣ ]  [ ▤ ]  [ \T Reset ]
    // Single horizontal scrollable row with gradient active style
    // -------------------------------------------------------------------------
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // [ B ] Bold Toggle
        GradientIconPill(
            label = "B",
            icon = Icons.Filled.FormatBold,
            isSelected = settings.subtitleBold,
            contentDescription = "Bold",
            onClick = {
                onUpdateSettings { it.copy(subtitleBold = !it.subtitleBold) }
            }
        )

        // [ I ] Italic Toggle
        GradientIconPill(
            label = "I",
            icon = Icons.Filled.FormatItalic,
            isSelected = settings.subtitleItalic,
            contentDescription = "Italic",
            onClick = {
                onUpdateSettings { it.copy(subtitleItalic = !it.subtitleItalic) }
            }
        )

        // [ U ] Underline Toggle
        GradientIconPill(
            label = "U",
            icon = null,
            drawableRes = R.drawable.viaura_text_underline,
            isSelected = settings.subtitleUnderline,
            contentDescription = "Underline",
            onClick = {
                onUpdateSettings { it.copy(subtitleUnderline = !it.subtitleUnderline) }
            }
        )

        // [ ≡ ] Left Align
        GradientIconPill(
            label = "≡",
            icon = null,
            drawableRes = R.drawable.viaura_align_left,
            isSelected = settings.subtitleAlignment.equals("left", ignoreCase = true),
            contentDescription = "Align Left",
            onClick = {
                onUpdateSettings { it.copy(subtitleAlignment = "left") }
            }
        )

        // [ ≣ ] Center Align
        GradientIconPill(
            label = "≣",
            icon = Icons.Filled.FormatAlignCenter,
            isSelected = settings.subtitleAlignment.equals("center", ignoreCase = true) || settings.subtitleAlignment.isBlank(),
            contentDescription = "Align Center",
            onClick = {
                onUpdateSettings { it.copy(subtitleAlignment = "center") }
            }
        )

        // [ ▤ ] Right Align
        GradientIconPill(
            label = "▤",
            icon = null,
            drawableRes = R.drawable.viaura_align_right,
            isSelected = settings.subtitleAlignment.equals("right", ignoreCase = true),
            contentDescription = "Align Right",
            onClick = {
                onUpdateSettings { it.copy(subtitleAlignment = "right") }
            }
        )

        // [ \T Reset ] Reset Typography Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SheetButtonBgColor)
                .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                .clickable {
                    onUpdateSettings {
                        it.copy(
                            subtitleBold = true,
                            subtitleItalic = false,
                            subtitleUnderline = false,
                            subtitleAlignment = "center",
                            subtitleFontSize = 52f,
                            subtitleBorderStyle = "outline_shadow",
                            subtitleBorderSize = 3f,
                            subtitleShadowOffset = 1.5f,
                            subtitleShadowOffsetX = 2f,
                            subtitleShadowOffsetY = 2f,
                            subtitleShadowBlur = 4f,
                            subtitleBackgroundPadding = 8f,
                            subtitleBackgroundCornerRadius = 8f,
                            subtitleLetterSpacing = 0f,
                            selectedSubtitleFont = ""
                        )
                    }
                }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                StyledIcon(
                    imageVector = Icons.Filled.FormatColorReset,
                    contentDescription = "Reset Typography",
                    tint = SheetButtonTextColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "Reset",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetButtonTextColor
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(2.dp))

    // -------------------------------------------------------------------------
    // Font Size Direct Slider with Title Above
    // -------------------------------------------------------------------------
    FullSliderCard(
        title = "Font Size",
        value = settings.subtitleFontSize,
        unit = "sp",
        min = 12f,
        max = 110f,
        step = 1f,
        format = "%.0f",
        onChange = { newSize ->
            onUpdateSettings { it.copy(subtitleFontSize = newSize) }
        }
    )

    // -------------------------------------------------------------------------
    // 4 Category Tabs: [ Border ]  [ Background ]  [ Shadow ]  [ Font ]
    // -------------------------------------------------------------------------
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TypographySubTab.entries.forEach { tab ->
            val isSelected = selectedSubTab == tab
            val tabTitle = when (tab) {
                TypographySubTab.BORDER -> "Border"
                TypographySubTab.BACKGROUND -> "Background"
                TypographySubTab.SHADOW -> "Shadow"
                TypographySubTab.FONT -> "Font"
            }

            val gradientBrush = if (isSelected) {
                Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            } else null

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (gradientBrush != null) Modifier.background(gradientBrush)
                        else Modifier.background(SheetButtonBgColor)
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else SheetButtonBorderColor,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { selectedSubTab = tab }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabTitle,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else SheetButtonTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Selected Category Sliders & Controls
    // -------------------------------------------------------------------------
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SheetButtonBgColor)
            .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        when (selectedSubTab) {
            // 1. BORDER SUB-TAB
            TypographySubTab.BORDER -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Border Style Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "none" to "None",
                            "outline" to "Outline",
                            "shadow" to "Shadow",
                            "outline_shadow" to "Outline+Shadow"
                        ).forEach { (styleKey, styleName) ->
                            val isStyleSelected = settings.subtitleBorderStyle == styleKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isStyleSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isStyleSelected) MaterialTheme.colorScheme.primary else SheetButtonBorderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onUpdateSettings { it.copy(subtitleBorderStyle = styleKey) }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = styleName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isStyleSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isStyleSelected) MaterialTheme.colorScheme.primary else SheetButtonTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Border Size Slider
                    FullSliderCard(
                        title = "Border Size",
                        value = settings.subtitleBorderSize,
                        unit = "pt",
                        min = 0f,
                        max = 20f,
                        step = 0.5f,
                        format = "%.1f",
                        onChange = { newSize ->
                            onUpdateSettings { it.copy(subtitleBorderSize = newSize) }
                        }
                    )
                }
            }

            // 2. BACKGROUND SUB-TAB (2 Sliders: Size/Padding + Corner Radius)
            TypographySubTab.BACKGROUND -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FullSliderCard(
                        title = "Background Padding / Size",
                        value = settings.subtitleBackgroundPadding,
                        unit = "dp",
                        min = 0f,
                        max = 30f,
                        step = 1f,
                        format = "%.0f",
                        onChange = { newPadding ->
                            onUpdateSettings { it.copy(subtitleBackgroundPadding = newPadding) }
                        }
                    )

                    FullSliderCard(
                        title = "Background Corner Radius",
                        value = settings.subtitleBackgroundCornerRadius,
                        unit = "dp",
                        min = 0f,
                        max = 30f,
                        step = 1f,
                        format = "%.0f",
                        onChange = { newRadius ->
                            onUpdateSettings { it.copy(subtitleBackgroundCornerRadius = newRadius) }
                        }
                    )
                }
            }

            // 3. SHADOW SUB-TAB (3 Sliders: Shadow Blur/Size + X-Offset + Y-Offset)
            TypographySubTab.SHADOW -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FullSliderCard(
                        title = "Shadow Blur",
                        value = settings.subtitleShadowBlur,
                        unit = "px",
                        min = 0f,
                        max = 20f,
                        step = 0.5f,
                        format = "%.1f",
                        onChange = { newBlur ->
                            onUpdateSettings { it.copy(subtitleShadowBlur = newBlur) }
                        }
                    )

                    FullSliderCard(
                        title = "Shadow Offset / Size",
                        value = settings.subtitleShadowOffset,
                        unit = "pt",
                        min = 0f,
                        max = 20f,
                        step = 0.5f,
                        format = "%.1f",
                        onChange = { newOffset ->
                            onUpdateSettings { it.copy(subtitleShadowOffset = newOffset) }
                        }
                    )

                    FullSliderCard(
                        title = "Shadow Horizontal (Left ↔ Right)",
                        value = settings.subtitleShadowOffsetX,
                        unit = "pt",
                        min = -15f,
                        max = 15f,
                        step = 0.5f,
                        format = "%+.1f",
                        onChange = { newX ->
                            onUpdateSettings { it.copy(subtitleShadowOffsetX = newX) }
                        }
                    )

                    FullSliderCard(
                        title = "Shadow Vertical (Up ↕ Down)",
                        value = settings.subtitleShadowOffsetY,
                        unit = "pt",
                        min = -15f,
                        max = 15f,
                        step = 0.5f,
                        format = "%+.1f",
                        onChange = { newY ->
                            onUpdateSettings { it.copy(subtitleShadowOffsetY = newY) }
                        }
                    )
                }
            }

            // 4. FONT SUB-TAB
            TypographySubTab.FONT -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // System default font
                    FontItemRow(
                        name = "Default (System Font)",
                        isSelected = settings.selectedSubtitleFont.isBlank(),
                        onSelect = {
                            onUpdateSettings { it.copy(selectedSubtitleFont = "") }
                        }
                    )

                    installedFonts.forEach { fontFile ->
                        val originalName = SubtitleFontManager.getOriginalFontName(fontFile)
                        val isSelected = settings.selectedSubtitleFont.equals(fontFile.name, ignoreCase = true) ||
                                settings.selectedSubtitleFont.equals(fontFile.nameWithoutExtension, ignoreCase = true) ||
                                settings.selectedSubtitleFont.equals(originalName, ignoreCase = true)
                        FontItemRow(
                            name = originalName,
                            isSelected = isSelected,
                            onSelect = {
                                onUpdateSettings { it.copy(selectedSubtitleFont = fontFile.name) }
                            }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Word / Character Spacing Slider
    // -------------------------------------------------------------------------
    FullSliderCard(
        title = "Character & Word Spacing",
        value = settings.subtitleLetterSpacing,
        unit = "pt",
        min = -5f,
        max = 20f,
        step = 0.5f,
        format = "%+.1f",
        onChange = { newSpacing ->
            onUpdateSettings { it.copy(subtitleLetterSpacing = newSpacing) }
        }
    )
}

// =============================================================================
// GRADIENT ICON PILL (Image 2 style)
// =============================================================================
@Composable
private fun GradientIconPill(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    contentDescription: String?,
    onClick: () -> Unit,
    drawableRes: Int? = null
) {
    val gradientBrush = if (isSelected) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
        )
    } else null

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (gradientBrush != null) Modifier.background(gradientBrush)
                else Modifier.background(SheetButtonBgColor)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else SheetButtonBorderColor,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (drawableRes != null) {
            StyledIcon(
                drawableRes = drawableRes,
                contentDescription = contentDescription,
                tint = if (isSelected) Color.White else SheetButtonTextColor,
                modifier = Modifier.size(20.dp)
            )
        } else if (icon != null) {
            StyledIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) Color.White else SheetButtonTextColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =============================================================================
// FULL SLIDER CARD WITH +/- BUTTONS
// =============================================================================
@Composable
internal fun FullSliderCard(
    title: String,
    value: Float,
    unit: String = "",
    min: Float,
    max: Float,
    step: Float,
    format: String,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = SheetButtonTextColor
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGradient)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${String.format(Locale.US, format, value)} $unit".trim(),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(SheetButtonBgColor)
                    .border(1.dp, SheetButtonBorderColor, CircleShape)
                    .clickable(enabled = value > min) {
                        onChange((value - step).coerceAtLeast(min))
                    },
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease",
                    tint = if (value > min) SheetButtonTextColor else Neutral.S94A3B8,
                    modifier = Modifier.size(15.dp)
                )
            }

            ViauraSlider(
                value = value.coerceIn(min, max),
                onValueChange = onChange,
                valueRange = min..max,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AccentPink,
                    activeTrackColor = AccentSkyBlue,
                    inactiveTrackColor = SheetButtonBorderColor
                )
            )

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(SheetButtonBgColor)
                    .border(1.dp, SheetButtonBorderColor, CircleShape)
                    .clickable(enabled = value < max) {
                        onChange((value + step).coerceAtMost(max))
                    },
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase",
                    tint = if (value < max) SheetButtonTextColor else Neutral.S94A3B8,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun FontItemRow(
    name: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else Color.Transparent
            )
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else SheetButtonTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isSelected) {
            StyledIcon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
