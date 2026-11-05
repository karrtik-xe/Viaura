package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Ten ready-made 3-colour gradients (primary, middle, secondary) shown as small previews under the App Theme preview.
 * Tapping one applies it to the theme (same storage as the colour editor).
 */
val ThemeGradientPresets: List<List<Long>> = listOf(
    listOf(0xFF38BDF8, 0xFF8B5CF6, 0xFFEC4899), // Sky - Violet - Pink
    listOf(0xFFFF9A3C, 0xFFFF5E62, 0xFFC026D3), // Sunset
    listOf(0xFF34D399, 0xFF06B6D4, 0xFF6366F1), // Aurora
    listOf(0xFFF472B6, 0xFFA78BFA, 0xFF60A5FA), // Candy
    listOf(0xFFFACC15, 0xFFFB923C, 0xFFEF4444), // Fire
    listOf(0xFF22D3EE, 0xFF3B82F6, 0xFF8B5CF6), // Ocean
    listOf(0xFF84CC16, 0xFF22C55E, 0xFF14B8A6), // Forest
    listOf(0xFFFB7185, 0xFFE879F9, 0xFF818CF8), // Orchid
    listOf(0xFFFDE047, 0xFFF97316, 0xFFDC2626), // Lava
    listOf(0xFF2DD4BF, 0xFF38BDF8, 0xFFA78BFA)  // Lagoon
)

/**
 * Calls [onDoubleTap] after two quick taps anywhere inside this element. It only WATCHES the touches (initial pass,
 * nothing is consumed), so the tabs inside still react to a single touch at once.
 */
private fun Modifier.observeDoubleTap(onDoubleTap: () -> Unit): Modifier = composed {
    val callback by rememberUpdatedState(onDoubleTap)
    pointerInput(Unit) {
        var lastUpTime = 0L
        var lastUpPos = Offset.Zero
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val up = waitForUpOrCancellation(PointerEventPass.Initial)
            if (up == null || (up.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                lastUpTime = 0L
                return@awaitEachGesture
            }
            val isSecondTap = lastUpTime != 0L &&
                up.uptimeMillis - lastUpTime <= viewConfiguration.doubleTapTimeoutMillis &&
                (up.position - lastUpPos).getDistance() <= 48.dp.toPx()
            if (isSecondTap) {
                lastUpTime = 0L
                callback()
            } else {
                lastUpTime = up.uptimeMillis
                lastUpPos = up.position
            }
        }
    }
}

private fun Float.sd(s: Float): Dp = (this * s).dp

/**
 * Preview of the App Theme (Settings > Appearance > App Theme): a miniature of the real home screen (glass header,
 * Online Video card, folder cards, floating play button and the icon bottom bar), without any text, painted with the
 * theme gradient (logo, cloud tile, selected folder outline, quality chip, active tab pill). The bottom-bar tabs can be
 * tapped to check the colours; nothing changes a real setting.
 * A double tap anywhere on it calls [onDoubleTap] (opens the colour editor).
 * [compact] draws the same screen smaller (for the 5 gradient presets); [interactive] = false makes it display-only.
 */
@Composable
fun ThemePreviewControls(
    colors: List<Color>,
    isDark: Boolean,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    interactive: Boolean = true
) {
    val first = colors.firstOrNull() ?: Color(0xFF38BDF8)
    val last = colors.lastOrNull() ?: Color(0xFFEC4899)
    val stops = if (colors.size >= 2) colors else listOf(first, last)
    val diagonal = Brush.linearGradient(stops)
    val horizontal = Brush.horizontalGradient(stops)

    val s = if (compact) 0.46f else 1f
    val stroke = if (compact) 0.7.dp else 1.dp

    val backdrop = if (isDark) Color(0xFF111A28) else Color(0xFFDBE3EB)
    val glass = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.42f)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.90f)
    val ink = if (isDark) Color(0xFFE6EDF7) else Color(0xFF1B2433)
    val chipBg = if (isDark) Color.White.copy(alpha = 0.09f) else Color(0xFFE9EEF4)

    val tabIcons = remember {
        listOf(
            R.drawable.viaura_video_library,
            R.drawable.viaura_music_library,
            R.drawable.viaura_recent,
            R.drawable.viaura_playlist_video,
            R.drawable.viaura_network,
            R.drawable.viaura_downloads
        )
    }
    var activeTab by remember { mutableIntStateOf(0) }
    val noRipple = remember { MutableInteractionSource() }
    val folderCount = 2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (compact) Modifier.aspectRatio(240f / 415f) else Modifier)
            .clip(RoundedCornerShape(if (compact) 10.dp else 15f.sd(s)))
            .background(backdrop)
            .then(if (interactive) Modifier.observeDoubleTap(onDoubleTap) else Modifier)
            .padding(8f.sd(s)),
        verticalArrangement = Arrangement.spacedBy(6f.sd(s))
    ) {
        // 1. Header: only the gradient app logo (no app name, no Settings icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34f.sd(s))
                .clip(RoundedCornerShape(11f.sd(s)))
                .background(glass)
                .border(stroke, glassBorder, RoundedCornerShape(11f.sd(s)))
                .padding(horizontal = 7f.sd(s)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayGradientLogoIcon(size = 22f.sd(s))
        }

        // 2. Two folder cards only (no Online Video card): the first one is the "selected" one

        // 3. Folder cards (first one is the "selected" one: outline in the theme gradient)
        repeat(folderCount) { index ->
            val shape = RoundedCornerShape(16f.sd(s))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(glass)
                    .then(
                        if (index == 0) Modifier.border(if (compact) 1.dp else 1.6.dp, horizontal, shape)
                        else Modifier.border(stroke, glassBorder, shape)
                    )
                    .padding(7f.sd(s)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernFolderIcon(
                    isDark = isDark,
                    size = 38f.sd(s),
                    showBadge = false
                )
                Spacer(modifier = Modifier.width(8f.sd(s)))
                Column(verticalArrangement = Arrangement.spacedBy(5f.sd(s))) {
                    Box(
                        Modifier
                            .width((92f - index * 14f).sd(s))
                            .height(6f.sd(s))
                            .clip(RoundedCornerShape(50))
                            .background(ink.copy(alpha = 0.60f))
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4f.sd(s)), verticalAlignment = Alignment.CenterVertically) {
                        // videos chip: gradient dot + line
                        Row(
                            modifier = Modifier
                                .height(12f.sd(s))
                                .clip(RoundedCornerShape(50))
                                .background(chipBg)
                                .padding(horizontal = 5f.sd(s)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(5f.sd(s)).clip(RoundedCornerShape(50)).background(first))
                            Spacer(Modifier.width(3f.sd(s)))
                            Box(Modifier.width(16f.sd(s)).height(3f.sd(s)).clip(RoundedCornerShape(50)).background(ink.copy(alpha = 0.45f)))
                        }
                        // size chip
                        Box(
                            Modifier
                                .height(12f.sd(s))
                                .width(28f.sd(s))
                                .clip(RoundedCornerShape(50))
                                .background(chipBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.width(16f.sd(s)).height(3f.sd(s)).clip(RoundedCornerShape(50)).background(ink.copy(alpha = 0.35f)))
                        }
                        // quality chip: tinted with the secondary colour of the gradient
                        Box(
                            Modifier
                                .height(12f.sd(s))
                                .width(26f.sd(s))
                                .clip(RoundedCornerShape(50))
                                .background(last.copy(alpha = if (isDark) 0.22f else 0.16f))
                                .border(stroke, last.copy(alpha = 0.35f), RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.width(14f.sd(s)).height(3f.sd(s)).clip(RoundedCornerShape(50)).background(last))
                        }
                    }
                }
            }
        }

        // Free space where a 3rd / 4th folder would be (makes the preview tall like a phone)
        if (compact) Spacer(modifier = Modifier.weight(1f))

        // 4. Floating play button (bottom right, like the home screen)
        if (!compact) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(glass)
                        .border(1.dp, glassBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(diagonal),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            drawableRes = R.drawable.viaura_play_solid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }
        }

        // 5. Bottom bar: icons only, the active tab is a gradient pill (tap a tab to move it)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38f.sd(s))
                .clip(RoundedCornerShape(19f.sd(s)))
                .background(glass)
                .border(stroke, glassBorder, RoundedCornerShape(19f.sd(s)))
                .padding(3f.sd(s)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabIcons.forEachIndexed { i, res ->
                val on = i == activeTab
                Box(
                    modifier = Modifier
                        .weight(if (on) 1.9f else 1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .then(if (on) Modifier.background(horizontal) else Modifier)
                        .then(
                            if (interactive) Modifier.clickable(interactionSource = noRipple, indication = null) { activeTab = i }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StyledIcon(
                            drawableRes = res,
                            contentDescription = null,
                            tint = if (on) Color.White else ink.copy(alpha = 0.60f),
                            modifier = Modifier.size(16f.sd(s))
                        )
                        if (on) {
                            Spacer(Modifier.width(3f.sd(s)))
                            Box(Modifier.width(14f.sd(s)).height(3f.sd(s)).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f)))
                        }
                    }
                }
            }
        }
    }
}

/**
 * One small preset preview (the same home-screen miniature, display-only). The whole thumbnail is the tap target;
 * the selected one gets a 2dp outline in its own gradient.
 */
@Composable
fun ThemePresetPreview(
    colors: List<Color>,
    isDark: Boolean,
    isSelected: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isSelected) Modifier.border(2.5.dp, Brush.horizontalGradient(if (colors.size >= 2) colors else listOf(Color(0xFF38BDF8), Color(0xFFEC4899))), shape)
                else Modifier.border(1.dp, borderColor, shape)
            )
            .padding(2.dp)
            .pointerInput(onDoubleTap != null) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap?.invoke() }
                )
            }
    ) {
        ThemePreviewControls(
            colors = colors,
            isDark = isDark,
            onDoubleTap = {},
            compact = true,
            interactive = false
        )
    }
}
