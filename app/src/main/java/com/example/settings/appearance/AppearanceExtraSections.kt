package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.background.AnimatedBackground
import com.example.background.BackgroundAnimationSettings
import com.example.background.BackgroundType
import com.example.background.BackdropDrawingState
import com.example.background.RandomBackground
import com.example.background.RandomInterval
import com.example.background.drawBackdrop
import com.example.background.rememberActiveBackground
import com.example.background.rememberBackdropPalette
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentGradientColors
import com.example.ui.theme.AccentPink
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.Neutral
import kotlin.math.roundToInt

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text = text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.5.sp)
}

@Composable
private fun SectionCard(isDark: Boolean, cardBg: Color, borderColor: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) { Column { content() } }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, isDark: Boolean, primaryText: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (selected) Modifier.background(AccentGradient) else Modifier.background(if (isDark) Neutral.D1E293B else Neutral.F1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else primaryText, textAlign = TextAlign.Center)
    }
}

private val Geometric = listOf(BackgroundType.CIRCLES, BackgroundType.RINGS, BackgroundType.SHAPES, BackgroundType.GRID, BackgroundType.LAVA, BackgroundType.BOKEH,
    BackgroundType.HEXAGONS, BackgroundType.RADAR, BackgroundType.ORBITS, BackgroundType.EQUALIZER,
    BackgroundType.PRISM, BackgroundType.CONFETTI)
private val Nature = listOf(BackgroundType.SNOW, BackgroundType.RAIN, BackgroundType.SAKURA, BackgroundType.FIREFLIES, BackgroundType.BUBBLES,
    BackgroundType.WAVES, BackgroundType.CLOUDS, BackgroundType.LEAVES, BackgroundType.EMBERS,
    BackgroundType.FLOW, BackgroundType.LANTERNS)
private val Sky = listOf(BackgroundType.SPACE, BackgroundType.AURORA, BackgroundType.PARTICLES, BackgroundType.CONSTELLATION,
    BackgroundType.METEORS, BackgroundType.GALAXY, BackgroundType.WARP)

/** Settings > Appearance > Background: live preview, None / Random, interval, animation tiles, behaviour switches. */
@Composable
fun BackgroundAnimationSettingsSection(
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
) {
    val picked = BackgroundAnimationSettings.type
    val active = rememberActiveBackground()
    val choose: (BackgroundType) -> Unit = { t ->
        if (t == BackgroundType.RANDOM) RandomBackground.reroll()
        BackgroundAnimationSettings.markSeen(t)
        BackgroundAnimationSettings.setType(t)
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("BACKGROUND ANIMATION", secondaryText)
    Spacer(Modifier.height(8.dp))
    SectionCard(isDark, cardBg, borderColor) {
        Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp)).background(if (isDark) Neutral.D0F172A else Neutral.F1F5F9)) {
            AnimatedBackground(type = active.type, parallax = active.parallax)
            if (active.type == BackgroundType.NONE) {
                Text("No animation", color = secondaryText, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Preview · ${active.type.label}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText)
        Text(
            if (active.type in BackgroundType.TOUCH) "Touch anywhere on the screen to see the click effect"
            else "The animation that plays behind the app's screens",
            fontSize = 12.5.sp, color = secondaryText
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceChip("None", picked == BackgroundType.NONE, isDark, primaryText, { choose(BackgroundType.NONE) })
            ChoiceChip("Random", picked == BackgroundType.RANDOM, isDark, primaryText, { choose(BackgroundType.RANDOM) })
        }
        if (picked == BackgroundType.RANDOM) {
            Spacer(Modifier.height(14.dp))
            Text("Change background", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = primaryText)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RandomInterval.entries.forEach { r ->
                    ChoiceChip(r.label, BackgroundAnimationSettings.randomInterval == r, isDark, primaryText, {
                        if (BackgroundAnimationSettings.randomInterval != r) {
                            RandomBackground.reroll()
                            BackgroundAnimationSettings.setRandomInterval(r)
                        }
                    })
                }
            }
        }
        if (active.event != null && active.type.isSeasonal && picked != BackgroundType.NONE) {
            Spacer(Modifier.height(12.dp))
            Text(
                "${active.type.label} is playing for the current festival. Turn off Seasonal themes to see your own pick.",
                fontSize = 12.5.sp, color = AccentSkyBlue
            )
        }
    }

    BackgroundGroup("CLICK EFFECT · TOUCH ANYWHERE", BackgroundType.TOUCH, picked, choose, isDark, cardBg, borderColor, primaryText, secondaryText)
    BackgroundGroup("NEW", BackgroundType.FRESH, picked, choose, isDark, cardBg, borderColor, primaryText, secondaryText)
    BackgroundGroup("GEOMETRY & GLOW", Geometric, picked, choose, isDark, cardBg, borderColor, primaryText, secondaryText)
    BackgroundGroup("NATURE", Nature, picked, choose, isDark, cardBg, borderColor, primaryText, secondaryText)
    BackgroundGroup("SKY & STARS", Sky, picked, choose, isDark, cardBg, borderColor, primaryText, secondaryText)

    Spacer(Modifier.height(20.dp))
    SectionLabel("BEHAVIOR", secondaryText)
    Spacer(Modifier.height(8.dp))
    SectionCard(isDark, cardBg, borderColor) {
        PlayerSwitchRow(
            title = "Parallax effect", description = "Smooth background shifting when tilting the device",
            checked = BackgroundAnimationSettings.parallax, enabled = picked != BackgroundType.NONE,
            primaryText = primaryText, secondaryText = secondaryText, onChange = { BackgroundAnimationSettings.setParallax(it) }
        )
        Spacer(Modifier.height(10.dp)); HorizontalDivider(color = borderColor); Spacer(Modifier.height(10.dp))
        PlayerSwitchRow(
            title = "Show over wallpaper", description = "Also play the animation while Appearance is System (wallpaper colours)",
            checked = BackgroundAnimationSettings.overWallpaper, enabled = picked != BackgroundType.NONE,
            primaryText = primaryText, secondaryText = secondaryText, onChange = { BackgroundAnimationSettings.setOverWallpaper(it) }
        )
        Spacer(Modifier.height(10.dp)); HorizontalDivider(color = borderColor); Spacer(Modifier.height(10.dp))
        PlayerSwitchRow(
            title = "Seasonal themes", description = "Festive backgrounds during Diwali, Holi, Halloween, New Year and winter holidays",
            checked = BackgroundAnimationSettings.seasonalThemes,
            primaryText = primaryText, secondaryText = secondaryText, onChange = { BackgroundAnimationSettings.setSeasonalThemes(it) }
        )
    }
}

@Composable
private fun BackgroundGroup(
    title: String, types: List<BackgroundType>, picked: BackgroundType, choose: (BackgroundType) -> Unit,
    isDark: Boolean, cardBg: Color, borderColor: Color, primaryText: Color, secondaryText: Color,
) {
    Spacer(Modifier.height(20.dp))
    SectionLabel(title, secondaryText)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        types.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { t ->
                    BackgroundTile(t, picked == t, isDark, cardBg, borderColor, primaryText, { choose(t) }, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A still from the real renderer (no clocks or sensors) with the name below. */
@Composable
private fun BackgroundTile(
    type: BackgroundType, selected: Boolean, isDark: Boolean, cardBg: Color, borderColor: Color,
    primaryText: Color, onClick: () -> Unit, modifier: Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val palette = rememberBackdropPalette()
    // click-effect tiles show a frozen mid-effect touch, so the tile tells what a click does
    val drawing = remember(type) { BackdropDrawingState().also { it.demo = type in BackgroundType.TOUCH } }
    Column(
        modifier = modifier.clip(shape).background(cardBg)
            .border(if (selected) 2.dp else 1.dp, if (selected) AccentSkyBlue else borderColor, shape)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            Canvas(Modifier.fillMaxWidth().height(96.dp).clipToBounds()) {
                if (size.minDimension > 0f) drawBackdrop(type, 12f, Offset.Zero, palette, drawing)
            }
            // "NEW" tag: stays until this tile is tapped once
            if (BackgroundAnimationSettings.isNewBadge(type)) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)).background(AccentGradient)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text("NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
        Text(
            type.label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = primaryText,
            maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )
    }
}
