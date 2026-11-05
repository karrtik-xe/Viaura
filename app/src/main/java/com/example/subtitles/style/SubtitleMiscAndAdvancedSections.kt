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

// =============================================================================
// 3. ENHANCED MISCELLANEOUS SECTION (Full, Expandable, Detailed Layout)
// =============================================================================
@Composable
internal fun EnhancedMiscContent(
    settings: PlayerSettings,
    isAssSsaTrack: Boolean,
    onUpdateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // This only applies to a genuinely native ASS/SSA subtitle (it forces its own
        // embedded styling) — hidden for every other subtitle format, where the typography/
        // colour settings above always apply directly with no toggle needed.
        if (isAssSsaTrack) {
            // Detailed Switch: Override ASS/SSA
            DetailedSwitchRow(
                title = "Override ASS/SSA Styles",
                description = "Force custom styling over embedded ASS subtitles",
                checked = settings.overrideAssSsaSubtitles,
                onCheckedChange = { isChecked ->
                    onUpdateSettings { it.copy(overrideAssSsaSubtitles = isChecked) }
                }
            )
        }

        // Detailed Switch: Scale with Window
        DetailedSwitchRow(
            title = "Scale with Window",
            description = "Resize subtitles proportionally with player dimensions",
            checked = settings.scaleSubtitlesByWindow,
            onCheckedChange = { isChecked ->
                onUpdateSettings { it.copy(scaleSubtitlesByWindow = isChecked) }
            }
        )

        // Detailed Switch: Blend with Video
        DetailedSwitchRow(
            title = "Blend Subtitles with Video",
            description = "Apply video color grading and filters to subtitles",
            checked = settings.subtitleBlendWithVideo,
            onCheckedChange = { isChecked ->
                onUpdateSettings { it.copy(subtitleBlendWithVideo = isChecked) }
            }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Full Subtitle Scale Factor Slider
        FullSliderCard(
            title = "Subtitle Scale Factor",
            value = settings.subtitleScale,
            unit = "x",
            min = 0.5f,
            max = 3.0f,
            step = 0.05f,
            format = "%.2f",
            onChange = { newScale ->
                onUpdateSettings { it.copy(subtitleScale = newScale) }
            }
        )

        // Vertical Position: a single slider that always starts at the bottom (0%) and goes up (100% = top).
        // 0% sits slightly above the very bottom edge ([SUBTITLE_BOTTOM_POSITION]) so the subtitle never disappears.
        val uiValue = subtitlePositionToSlider(settings.subtitlePosition)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SheetButtonBgColor)
                .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vertical Position",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetButtonTextColor
                )
                Text(
                    text = "${uiValue.roundToInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Origin: Bottom → Top",
                        fontSize = 11.sp,
                        color = Neutral.S475569
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGradient)
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${uiValue.roundToInt()}%",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                ViauraSlider(
                    value = uiValue,
                    onValueChange = { newUiVal ->
                        onUpdateSettings {
                            it.copy(
                                subtitlePosition = sliderToSubtitlePosition(newUiVal),
                                subtitlePositionDirection = "BOTTOM_TO_TOP"
                            )
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPink,
                        activeTrackColor = AccentSkyBlue,
                        inactiveTrackColor = SheetButtonBorderColor
                    )
                )
            }
        }

        // Reset Miscellaneous Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SheetButtonBgColor)
                .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                .clickable {
                    onUpdateSettings {
                        it.copy(
                            overrideAssSsaSubtitles = false,
                            scaleSubtitlesByWindow = true,
                            subtitleBlendWithVideo = false,
                            subtitleScale = 1.0f,
                            subtitlePosition = SUBTITLE_BOTTOM_POSITION,
                            subtitlePositionDirection = "BOTTOM_TO_TOP"
                        )
                    }
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StyledIcon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = "Reset Miscellaneous",
                    tint = SheetButtonTextColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Reset Miscellaneous Settings",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SheetButtonTextColor
                )
            }
        }
    }
}

/**
 * "Advanced" section: the Advanced ASS/SSA per-style editor switch and the raw script/text switch.
 * Turning a switch ON adds its own section below and opens it.
 */
@Composable
internal fun EnhancedAdvancedContent(
    settings: PlayerSettings,
    isAssSsaTrack: Boolean,
    onUpdateSettings: ((PlayerSettings) -> PlayerSettings) -> Unit,
    onOpenSection: (SubtitleStyleAccordionSection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // The per-style editor only exists for a genuinely native ASS/SSA subtitle.
        if (isAssSsaTrack) {
            DetailedSwitchRow(
                title = "Advanced ASS/SSA",
                description = "Edit every style of the current ASS/SSA subtitle individually",
                checked = settings.advancedAssEnabled,
                onCheckedChange = { isChecked ->
                    onUpdateSettings { it.copy(advancedAssEnabled = isChecked) }
                    if (isChecked) onOpenSection(SubtitleStyleAccordionSection.ADVANCED_ASS)
                }
            )
        }

        DetailedSwitchRow(
            title = "View Raw [Script Info] & Subtitle Text",
            description = "See and edit the full text of the current subtitle (any format)",
            checked = settings.rawSubtitleEditorEnabled,
            onCheckedChange = { isChecked ->
                onUpdateSettings { it.copy(rawSubtitleEditorEnabled = isChecked) }
                if (isChecked) onOpenSection(SubtitleStyleAccordionSection.RAW_TEXT)
            }
        )
    }
}

@Composable
private fun DetailedSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SheetButtonBgColor)
            .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SheetButtonTextColor
            )
            Text(
                text = description,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Neutral.S475569,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        ViauraSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(24.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = LocalSheetSecondaryTextColor.current,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = SheetButtonBorderColor
            )
        )
    }
}

/** Lowest allowed subtitle position (mpv sub-pos, 100 = very bottom edge). Slightly above 100 so the subtitle never vanishes. */
const val SUBTITLE_BOTTOM_POSITION = 94f

/** mpv sub-pos -> slider percent (0% = bottom limit, 100% = top). */
fun subtitlePositionToSlider(position: Float): Float =
    ((SUBTITLE_BOTTOM_POSITION - position) / SUBTITLE_BOTTOM_POSITION * 100f).coerceIn(0f, 100f)

/** Slider percent -> mpv sub-pos. */
fun sliderToSubtitlePosition(percent: Float): Float =
    (SUBTITLE_BOTTOM_POSITION * (1f - percent.coerceIn(0f, 100f) / 100f)).coerceIn(0f, SUBTITLE_BOTTOM_POSITION)
