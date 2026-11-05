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

// =============================================================================
// SMALL BUILDING BLOCKS
// =============================================================================
@Composable
internal fun AssSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
internal fun AssFieldRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun AssFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = LocalSheetSecondaryTextColor.current,
        modifier = Modifier.padding(bottom = 3.dp)
    )
}

/**
 * Text input that keeps its own text while typing. A value is committed (and therefore written to
 * the subtitle) only when [validate] accepts it; invalid text is shown with a red border and is
 * never saved, so an unsupported value can never corrupt a style line.
 */
@Composable
internal fun AssTextField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    numeric: Boolean = true,
    allowNegative: Boolean = false,
    validate: (String) -> String?,
    onCommit: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }
    var invalid by remember { mutableStateOf(false) }
    val validateUpdated by rememberUpdatedState(validate)

    LaunchedEffect(value) {
        if (validateUpdated(text) != value && text != value) {
            text = value
            invalid = false
        }
    }

    Column(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        AssFieldLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Neutral.FFFFFF.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    if (invalid) Color(0xFFEF4444) else SheetButtonBorderColor,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    val normalized = validate(newText)
                    invalid = normalized == null
                    if (normalized != null && normalized != value) onCommit(normalized)
                },
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(fontFamily = com.example.ui.theme.LocalAppFontFamily.current, fontSize = 13.sp, color = SheetButtonTextColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = when {
                        !numeric || allowNegative -> KeyboardType.Text
                        else -> KeyboardType.Decimal
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun AssFontField(
    currentFont: String,
    originalFont: String,
    open: Boolean,
    families: List<String>,
    onToggle: () -> Unit,
    onSelectOriginal: () -> Unit,
    onSelectFamily: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AssFieldLabel("Fontname")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Neutral.FFFFFF.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    if (open) MaterialTheme.colorScheme.primary else SheetButtonBorderColor,
                    RoundedCornerShape(10.dp)
                )
                .clickable { onToggle() }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentFont,
                fontSize = 13.sp,
                color = SheetButtonTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            StyledIcon(
                imageVector = if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = Neutral.S475569,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, SheetButtonBorderColor, RoundedCornerShape(10.dp))
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AssFontRow(
                    name = "Original ($originalFont)",
                    selected = false,
                    onSelect = onSelectOriginal
                )
                families.forEach { family ->
                    AssFontRow(
                        name = family,
                        selected = family.equals(currentFont, ignoreCase = true),
                        onSelect = { onSelectFamily(family) }
                    )
                }
                if (families.isEmpty()) {
                    Text(
                        text = "No imported fonts available. Import fonts from Settings → Subtitles → Fonts.",
                        fontSize = 11.sp,
                        color = LocalSheetSecondaryTextColor.current,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssFontRow(name: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else SheetButtonTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            StyledIcon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
internal fun AssAlignmentRow(selected: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AssFieldLabel("Alignment (1-9)")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (n in 1..9) {
                AssChip(
                    text = n.toString(),
                    selected = selected == n,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(n) }
                )
            }
        }
    }
}

@Composable
internal fun AssChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .height(36.dp)
            .clip(shape)
            .background(if (selected) primary else Neutral.FFFFFF.copy(alpha = 0.55f))
            .border(1.dp, if (selected) primary else SheetButtonBorderColor, shape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else SheetButtonTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Colour picker that opens right under the colour row that was touched (not at the bottom of the
 * whole colour section). It grows out of that row with a short, subtle expand + fade, and shrinks
 * back the same way. The last shown colour is kept while the exit animation runs.
 */
@Composable
internal fun AssInlineColorPicker(
    fields: List<String>,
    openField: String?,
    titleOf: (String) -> String,
    colourOf: (String) -> Long,
    onColorChange: (String, Long) -> Unit
) {
    val visible = openField != null && openField in fields
    val lastShown = remember { arrayOf(fields.first()) }
    if (openField != null && openField in fields) lastShown[0] = openField
    val shown = lastShown[0]
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(durationMillis = 220)),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            key(shown) {
                ThreeArcCircularColorPickerCard(
                    title = titleOf(shown),
                    colorLong = colourOf(shown),
                    onColorChange = { argb -> onColorChange(shown, argb) }
                )
            }
        }
    }
}

@Composable
internal fun AssColourField(
    label: String,
    value: String,
    enabled: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val argb = AssFields.parseColourToArgb(value) ?: 0xFFFFFFFFL
    Column(modifier = modifier.alpha(if (enabled) 1f else 0.45f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AssFieldLabel(label)
            StyledIcon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Neutral.FFFFFF.copy(alpha = 0.55f))
                .border(
                    1.dp,
                    if (active) MaterialTheme.colorScheme.primary else SheetButtonBorderColor,
                    RoundedCornerShape(10.dp)
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Neutral.S94A3B8)
                    .border(1.dp, SheetButtonBorderColor, CircleShape)
            ) {
                Box(modifier = Modifier.size(24.dp).background(Color(argb.toInt())))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                fontSize = 12.5.sp,
                color = SheetButtonTextColor,
                maxLines = 1
            )
        }
    }
}
