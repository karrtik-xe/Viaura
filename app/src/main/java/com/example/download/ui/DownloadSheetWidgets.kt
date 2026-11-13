package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue

/**
 * Building blocks shared by every download panel (online video / audio, local network link, media provider title), so that all of
 * them look the same: header with the gradient Download button, Audio | Video tabs, thumbnail card, Title / Author boxes,
 * quality rows that open a second sheet, "Save to" card and the source link.
 */

/** What a [SelectorRow] shows on its right side. */
enum class SheetTrailing { None, Tick, Checked, Unchecked, Locked }

/** One small pill under a row title (codec, size ...). */
data class SheetChip(val text: String, val accent: Boolean = false, val warn: Boolean = false)

private val WarnColor = Color(0xFFF59E0B)

/** "Download" + sub line on the left, the gradient Download pill on the right. */
@Composable
fun DownloadSheetHeader(
    title: String = "Download",
    subtitle: String,
    primary: Color,
    secondary: Color,
    enabled: Boolean,
    onDownload: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 14.sp, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (enabled) AccentGradient else SolidColor(Color.Gray.copy(alpha = 0.4f)))
                .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDownload)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIcon(R.drawable.viaura_download_minimalistic, null, Modifier.size(20.dp), Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Download", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

/** Audio | Video (or any tab titles) with a thin gradient line that slides under the selected one. */
@Composable
fun DownloadModeTabs(
    tabs: List<String>,
    selectedIndex: Int,
    primary: Color,
    secondary: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val sel = tabBounds[selectedIndex]
    val targetLeft = sel?.first ?: 0f
    val targetRight = (sel?.first ?: 0f) + (sel?.second ?: 0f)

    // the edge in the direction of travel moves first, so the line stretches and then follows (liquid effect)
    val lastIndex = remember { intArrayOf(selectedIndex) }
    val movingRight = selectedIndex >= lastIndex[0]
    SideEffect { lastIndex[0] = selectedIndex }

    val fastSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
    val slowSpec = tween<Float>(durationMillis = 350, easing = FastOutSlowInEasing)
    val leftEdge by animateFloatAsState(targetValue = targetLeft, animationSpec = if (movingRight) slowSpec else fastSpec, label = "DlTabLeft")
    val rightEdge by animateFloatAsState(targetValue = targetRight, animationSpec = if (movingRight) fastSpec else slowSpec, label = "DlTabRight")

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(secondary.copy(alpha = 0.25f))
        )
        val density = LocalDensity.current
        if (targetRight > targetLeft) {
            val widthDp = with(density) { (rightEdge - leftEdge).coerceAtLeast(0f).toDp() }
            val offsetDp = with(density) { leftEdge.toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = offsetDp)
                    .width(widthDp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(AccentGradient)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            tabs.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val textColor by animateColorAsState(targetValue = if (selected) primary else secondary, animationSpec = tween(200), label = "DlTabText")
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .onGloballyPositioned { c -> tabBounds[i] = c.positionInParent().x to c.size.width.toFloat() }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                        .padding(vertical = 10.dp)
                ) {
                    Text(label, fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = textColor)
                }
            }
        }
    }
}

@Composable
private fun SheetChipView(chip: SheetChip, secondary: Color) {
    val color = when {
        chip.warn -> WarnColor
        chip.accent -> AccentSkyBlue
        else -> secondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(chip.text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color, maxLines = 1)
    }
}

/**
 * One option card: format badge on the left, title and chips in the middle, optional marker on the right.
 * [selected] tints the card; [trailing] is a tick or a checkbox.
 */
@Composable
fun SelectorRow(
    badge: String,
    title: String,
    chips: List<SheetChip>,
    selected: Boolean,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: SheetTrailing = SheetTrailing.None,
    leadingIconRes: Int? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AccentSkyBlue.copy(alpha = 0.16f) else primary.copy(alpha = 0.07f))
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 58.dp, height = 46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AccentSkyBlue.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            if (leadingIconRes != null) {
                StyledIcon(
                    drawableRes = leadingIconRes,
                    contentDescription = null,
                    tint = AccentSkyBlue,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(badge.uppercase().take(6), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentSkyBlue, maxLines = 1)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (chips.isNotEmpty()) {
                Row(modifier = Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (c in chips) SheetChipView(c, secondary)
                }
            }
        }
        if (trailing != SheetTrailing.None) {
            Spacer(Modifier.width(8.dp))
            when (trailing) {
                SheetTrailing.Tick -> SheetCheckMark()
                SheetTrailing.Checked -> SheetCheckbox(true, secondary)
                SheetTrailing.Unchecked -> SheetCheckbox(false, secondary)
                SheetTrailing.Locked -> SheetCheckbox(true, secondary, enabled = false)
                SheetTrailing.None -> {}
            }
        }
    }
}

/** The round tick shown on the chosen row. */
@Composable
fun SheetCheckMark() {
    StyledIcon(R.drawable.viaura_check_circle, "Selected", Modifier.size(24.dp), AccentSkyBlue)
}

/** Checkbox used by the multi-select rows (audio tracks, subtitles). */
@Composable
fun SheetCheckbox(checked: Boolean, secondary: Color, enabled: Boolean = true) {
    Checkbox(
        checked = checked,
        onCheckedChange = null,
        enabled = enabled,
        colors = CheckboxDefaults.colors(checkedColor = AccentSkyBlue, uncheckedColor = secondary)
    )
}

/** A card with an optional leading picture, a title / sub line and an optional switch (thumbnail, lyrics, chapters). */
@Composable
fun SheetOptionCard(
    title: String,
    subtitle: String,
    primary: Color,
    secondary: Color,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(primary.copy(alpha = 0.07f))
            .then(
                if (checked != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }
                else Modifier
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 14.sp, color = secondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
        }
        if (checked != null) {
            Spacer(Modifier.width(8.dp))
            ViauraSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentSkyBlue,
                    uncheckedThumbColor = secondary,
                    uncheckedTrackColor = secondary.copy(alpha = 0.18f),
                    uncheckedBorderColor = secondary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

/** Rounded text box with its label sitting on the border (Title / Author). */
@Composable
fun SheetTextField(label: String, value: String, onValueChange: (String) -> Unit, primary: Color, secondary: Color, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        textStyle = TextStyle(fontFamily = com.example.ui.theme.LocalAppFontFamily.current, fontSize = 17.sp, fontWeight = FontWeight.Medium),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = primary,
            unfocusedTextColor = primary,
            focusedBorderColor = AccentSkyBlue,
            unfocusedBorderColor = secondary.copy(alpha = 0.6f),
            focusedLabelColor = AccentSkyBlue,
            unfocusedLabelColor = secondary,
            cursorColor = AccentSkyBlue
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Small heading above a group of rows ("Video quality", "Audio tracks" ...), with an optional action on the right. */
@Composable
fun SheetSectionLabel(text: String, color: Color, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Grey helper line under a row. */
@Composable
fun SheetCaption(text: String, secondary: Color) {
    Text(text, fontSize = 13.sp, color = secondary, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp))
}

/** Blue text action ("Select all" / "Clear"). */
@Composable
fun SheetTextAction(text: String, onClick: () -> Unit) {
    Text(
        text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AccentSkyBlue,
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(6.dp)
    )
}

/** "Save to / Movies/Viaura" card. */
@Composable
fun SheetSaveToCard(folder: String, primary: Color, secondary: Color) {
    Column(
        modifier = Modifier
            .padding(top = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(primary.copy(alpha = 0.07f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("Save to", fontSize = 13.sp, color = secondary)
        Text(folder, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = primary, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The link the download comes from. */
@Composable
fun SheetLinkRow(url: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        StyledIcon(R.drawable.viaura_link, null, Modifier.size(20.dp), AccentSkyBlue)
        Spacer(Modifier.width(10.dp))
        Text(url, fontSize = 14.sp, color = AccentSkyBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The second sheet that comes up from the bottom when a quality / audio track / subtitle row is touched.
 * Multi-select pickers pass [showDone] so they get a Done button.
 */
@Composable
fun DownloadPickerSheet(
    title: String,
    isDark: Boolean,
    glassBlurTransparency: Float,
    primary: Color,
    onDismiss: () -> Unit,
    showDone: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DownloadGlassSheet(isDark, glassBlurTransparency, onDismiss) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = primary, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        content()
        if (showDone) {
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(AccentGradient)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

/**
 * A cover when the website / provider gives none: a soft gradient (its colours come from the title, so every video keeps
 * the same one) with the first letter.
 */
@Composable
fun GeneratedCover(seed: String, modifier: Modifier = Modifier) {
    val h = ((seed.hashCode() and 0x7fffffff) % 360).toFloat()
    Box(
        modifier = modifier.background(
            androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.hsv(h, 0.55f, 0.85f), Color.hsv((h + 55f) % 360f, 0.60f, 0.62f)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            seed.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "▶",
            fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.92f)
        )
    }
}

/**
 * Download sheet header block: a large cover on the left; the Title and Author boxes sit on top of each other on the
 * right. The cover is exactly as tall as both boxes together (same top and bottom edge). The cover is always attached to
 * the downloaded file (the picture of the video itself), so there is no switch for it.
 */
@Composable
fun SheetCoverTitleAuthor(
    thumbnail: String?,
    title: String,
    onTitleChange: (String) -> Unit,
    author: String,
    onAuthorChange: (String) -> Unit,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    coverWidth: androidx.compose.ui.unit.Dp = 128.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(coverWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(24.dp))
        ) {
            LinkThumbnail(
                thumbnail = thumbnail?.takeIf { it.isNotBlank() },
                modifier = Modifier.fillMaxSize(),
                fallback = { GeneratedCover(title, Modifier.fillMaxSize()) }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            SheetTextField("Title", title, onTitleChange, primary, secondary)
            SheetTextField("Author", author, onAuthorChange, primary, secondary, Modifier.padding(top = 12.dp))
        }
    }
}

