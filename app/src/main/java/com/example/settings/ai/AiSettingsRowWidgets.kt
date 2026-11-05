package com.example.ui.components

import com.example.ui.theme.Neutral

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.ui.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ai.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.DpOffset
import com.example.ui.components.LocalLiquidGlass
import com.example.ui.components.liquidGlassEdge
import com.example.ui.components.liquidGlassSheenBrush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ---------------- Shared small building blocks ----------------

@Composable
internal fun SectionHeader(text: String, color: Color) {
    Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun GlassCard(cardBg: Color, borderColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
internal fun SettingsDivider(borderColor: Color) {
    HorizontalDivider(
        color = borderColor.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 14.dp)
    )
}

@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    drawableRes: Int? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) primaryText else secondaryText
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )
        }
        ViauraSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentSkyBlue,
                checkedThumbColor = Color.White
            )
        )
    }
}

/**
 * Dropdown Popup Choice Row — exactly matches the Orientation popup in player settings
 * (Screenshot 3).
 */
@Composable
internal fun PlayerChoiceRow(
    title: String,
    description: String,
    value: String,
    options: List<Pair<String, String>>,
    isDark: Boolean,
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    borderColor: Color = DarkGlassBorder,
    emptyMessage: String = "No options available",
    onChange: (String) -> Unit
) {
    var expanded by remember(value) { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == value }?.second ?: value.ifBlank { "Choose" }

    val dropdownBg = glassPanelColor(isDark, LocalPanelTransparency.current)

    val dropdownBorder = if (isDark) {
        AccentSkyBlue.copy(alpha = 0.55f)
    } else {
        AccentSkyBlueStrong.copy(alpha = 0.45f)
    }

    val unselectedTextColor = if (isDark) Neutral.F8FAFC else Neutral.D0F172A
    val accentColor = if (isDark) AccentSkyBlue else AccentSkyBlueStrong
    val selectedItemBg = if (isDark) AccentSkyBlue.copy(alpha = 0.20f) else AccentContainerLight

    val menuShape = RoundedCornerShape(16.dp)

    // The menu opens inside the settings card: its right edge sits on the right edge of the pill and it grows to the left only,
    // never wider than the card itself; thin, and glass like the other panels while Liquid Glass is on.
    val glassOn = LocalLiquidGlass.current
    val rowDensity = LocalDensity.current
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    val menuWidth = with(rowDensity) { minOf(172.dp, rowWidthPx.toDp().coerceAtLeast(120.dp)) }
    val menuOffset = DpOffset(with(rowDensity) { pillWidthPx.toDp() } - menuWidth, 0.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(14.dp))
            .clickable { expanded = true }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.onSizeChanged { pillWidthPx = it.width.toFloat() }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (expanded) {
                    if (isDark) AccentSkyBlue.copy(alpha = 0.20f) else AccentSkyBlueStrong.copy(alpha = 0.13f)
                } else if (isDark) {
                    Neutral.D1E293B.copy(alpha = 0.55f)
                } else {
                    AccentSkyBlueStrong.copy(alpha = 0.10f)
                },
                border = if (expanded) {
                    BorderStroke(1.2.dp, accentColor)
                } else {
                    BorderStroke(1.dp, borderColor.copy(alpha = 0.60f))
                },
                onClick = { expanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                    StyledIcon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = menuShape,
                offset = menuOffset,
                containerColor = dropdownBg,
                tonalElevation = 0.dp,
                shadowElevation = if (glassOn) 0.dp else 8.dp,
                border = if (glassOn) null else BorderStroke(1.2.dp, dropdownBorder),
                modifier = Modifier
                    .width(menuWidth)
                    .clip(menuShape)
                    .then(if (glassOn) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
                    .liquidGlassEdge(menuShape, isDark)
            ) {
                if (options.isEmpty()) {
                    Text(
                        text = emptyMessage,
                        fontSize = 13.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    options.forEach { (key, label) ->
                        val isSelected = key == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) selectedItemBg else Color.Transparent)
                                .clickable {
                                    expanded = false
                                    onChange(key)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) accentColor else unselectedTextColor,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                StyledIcon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OutlinedFieldLike(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Neutral.D1E293B.copy(alpha = 0.40f) else Neutral.F1F5F9.copy(alpha = 0.70f))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        FollowCursorTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            minHeight = 50.dp,
            placeholder = label,
            placeholderColor = secondaryText,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.example.ui.theme.LocalAppFontFamily.current, color = primaryText, fontSize = 13.sp)
        )
    }
}

@Composable
internal fun OutlinedActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    borderColor: Color,
    primaryText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StyledIcon(icon, null, tint = primaryText, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = primaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
