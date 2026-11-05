package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.R

import com.example.ui.theme.*
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.key
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SwitchDefaults
import com.example.ui.i18n.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.AudioChannelMode
import com.example.player.AudioLanguageMatcher
import com.example.player.SubtitleFontManager
import com.example.ui.state.AppLanguage
import com.example.ui.state.AppThemeVariant
import com.example.ui.state.ControlsAnimationStyle
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.HwAccelMode
import com.example.ui.state.PlayerSettings
import com.example.ui.state.ScreenNavigationStyle
import com.example.ui.state.SeekbarStyle
import com.example.ui.state.TabNavigationStyle
import com.example.ui.state.ThemeMode
import com.example.ui.state.ThumbnailQuality
import com.example.ui.state.ThumbnailStrategy
import com.example.ui.state.UiState
import com.example.ui.state.VideoOpeningAnimation
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.screens.LocalGlassBlurTransparency
import kotlinx.coroutines.launch

import com.example.ui.state.AppStrings
import java.io.File
import kotlin.math.roundToInt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.AccentGradientColors

/**
 * The editor always works with 3 colours: first, a muted & desaturated second (the middle of the gradient) and third.
 * A 2-colour gradient gets a muted middle colour mixed from its two ends.
 */
private fun themeEditorThreeColors(hex: List<Long>): List<Long> {
    if (hex.size >= 3) return hex.take(3)
    val a = hex.firstOrNull() ?: 0xFF38BDF8L
    val c = hex.lastOrNull() ?: 0xFFEC4899L
    val mid = androidx.core.graphics.ColorUtils.blendARGB(a.toInt(), c.toInt(), 0.5f)
    val hm = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(mid, hm)
    hm[1] = hm[1].coerceAtMost(0.40f)
    hm[2] = hm[2].coerceIn(0.45f, 0.65f)
    val muted = androidx.core.graphics.ColorUtils.HSLToColor(hm) and 0xFFFFFF
    return listOf(a, 0xFF000000L or muted.toLong(), c)
}

/**
 * Inline gradient editor for an App Theme. Same colour UI as the subtitle "Colors" panel: a chip per colour
 * (first / second / third), then the 3-arc circular picker. A live gradient panel shows the result; Done applies it.
 */
@Composable
internal fun ThemeGradientEditorPanel(
    theme: AppThemeVariant,
    currentHex: List<Long>,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onCancel: () -> Unit,
    onDone: (List<Long>?) -> Unit
) {
    val working = remember(theme.id) { mutableStateListOf(*themeEditorThreeColors(currentHex).toTypedArray()) }
    var selected by remember(theme.id) { mutableIntStateOf(0) }
    var resetNonce by remember(theme.id) { mutableIntStateOf(0) }
    val colors = working.map { Color(it.toInt()) }
    val shape = RoundedCornerShape(20.dp)
    val chipLabels = listOf("First", "Second", "Third")
    val titles = listOf("First Color", "Second Color", "Third Color")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(cardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header (tap = close without applying)
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onCancel() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledIcon(imageVector = Icons.Outlined.Palette, contentDescription = null, tint = AccentSkyBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Colors \u2022 ${theme.displayName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                modifier = Modifier.weight(1f)
            )
            StyledIcon(imageVector = Icons.Outlined.KeyboardArrowUp, contentDescription = "Close", tint = secondaryText, modifier = Modifier.size(22.dp))
        }

        // Live result panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(colors))
        )

        // Colour chips + reset (same look as the subtitle Colors panel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            colors.forEachIndexed { i, c ->
                val on = i == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) AccentSkyBlue.copy(alpha = 0.20f) else Color.Transparent)
                        .border(1.dp, if (on) AccentSkyBlue else borderColor, RoundedCornerShape(10.dp))
                        .clickable { selected = i }
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        )
                        Text(
                            text = chipLabels[i],
                            fontSize = 11.5.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            color = if (on) AccentSkyBlue else secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable {
                        themeEditorThreeColors(theme.gradientHex).forEachIndexed { i, v -> working[i] = v }
                        resetNonce++
                    }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(imageVector = Icons.Outlined.RestartAlt, contentDescription = "Reset Colors", tint = secondaryText, modifier = Modifier.size(16.dp))
            }
        }

        // First / Third colour: any colour (3-arc round picker). Second colour: the same round picker, limited to
        // muted & desaturated colours, so a too-glowing middle colour cannot be chosen.
        key(selected, resetNonce) {
            if (selected == 1) {
                MutedCircularColorPickerCard(
                    title = titles[selected],
                    selectedArgb = working[selected],
                    centerColor = colors[selected],
                    onPick = { argb -> working[selected] = 0xFF000000L or (argb and 0xFFFFFFL) }
                )
            } else {
                ThreeArcCircularColorPickerCard(
                    title = titles[selected],
                    colorLong = working[selected],
                    showAlpha = false,
                    onColorChange = { argb -> working[selected] = 0xFF000000L or (argb and 0xFFFFFFL) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { onCancel() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = secondaryText) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(colors))
                    .clickable { onDone(working.toList().takeIf { it != themeEditorThreeColors(theme.gradientHex) }) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        }
    }
}
