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
import androidx.compose.ui.unit.DpOffset
import com.example.ui.components.LocalLiquidGlass
import com.example.ui.components.liquidGlassEdge
import com.example.ui.components.liquidGlassSheenBrush

@Composable
fun SubtitlesTab(
    settings: PlayerSettings,
    isDark: Boolean,
    context: android.content.Context,
    fontListVersion: Int,
    cardBg: Color,
    borderColor: Color,
    alphaFactor: Float = 1.0f,
    lang: AppLanguage = AppLanguage.ENGLISH,
    onSettingsChange: (PlayerSettings) -> Unit,
    onChooseFontDirectory: () -> Unit,
    onPickFontFiles: () -> Unit,
    onReloadFonts: () -> Unit,
    onDeleteFont: (String) -> Unit,
    onDeleteAllFonts: () -> Unit,
    onOpenFolderVideoSelector: () -> Unit = {}
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val installedFonts = remember(fontListVersion, settings.subtitleFontsReloadNonce, settings.showVideoEmbeddedSubtitleFonts) {
        SubtitleFontManager.getInstalledFonts(context, includeVideoExtracted = settings.showVideoEmbeddedSubtitleFonts)
    }
    var isSubtitlesExpanded by remember { mutableStateOf(false) }
    var isFontsExpanded by remember { mutableStateOf(false) }
    // font deletion is confirmed in a bottom sheet first ("\u0000ALL" = every custom font)
    var pendingDeleteFont by remember { mutableStateOf<String?>(null) }
    pendingDeleteFont?.let { target ->
        val all = target == "\u0000ALL"
        DeleteConfirmSheet(
            title = if (all) "Delete all custom fonts?" else "Delete 1 font?",
            message = "This action cannot be undone. The selected ${if (all) "fonts" else "item"} will be permanently deleted.",
            itemNames = if (all) installedFonts.filter { SubtitleFontManager.isUserUploadedFont(context, it) }.map { it.name } else listOf(target),
            isDark = isDark,
            onDismiss = { pendingDeleteFont = null },
            onConfirm = {
                pendingDeleteFont = null
                if (all) onDeleteAllFonts() else onDeleteFont(target)
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // 1. Header: "Subtitles & Custom Fonts"

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: SUBTITLE PREFERENCES
        Text(
            text = "SUBTITLE PREFERENCES",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Main Expandable Card named "Subtitles" (or "Subtitle Preferences")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row (Always visible, tap to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSubtitlesExpanded = !isSubtitlesExpanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentSkyBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Subtitles,
                                contentDescription = null,
                                tint = AccentSkyBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preferred Subtitle Languages",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val currentSubtitleLangs = settings.preferredSubtitleLanguages.trim()
                            val subtitleSummaryText = if (currentSubtitleLangs.isNotBlank()) {
                                val targetInfo = if (settings.applySubtitleLanguageToSelectedContentOnly) {
                                    val total = settings.selectedSubtitleFolders.size + settings.selectedSubtitleVideos.size
                                    if (total > 0) " • $total items" else " • 0 items"
                                } else {
                                    " • All media"
                                }
                                "$currentSubtitleLangs$targetInfo"
                            } else {
                                "Tap to configure"
                            }
                            Text(
                                text = subtitleSummaryText,
                                fontSize = 11.5.sp,
                                color = if (currentSubtitleLangs.isNotBlank()) AccentSkyBlue else secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Expand / Collapse Chevron Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSubtitlesExpanded) AccentSkyBlue.copy(alpha = 0.18f)
                                else if (isDark) Color.White.copy(alpha = 0.08f)
                                else Color.Black.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            imageVector = if (isSubtitlesExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isSubtitlesExpanded) "Collapse" else "Expand",
                            tint = if (isSubtitlesExpanded) AccentSkyBlue else secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Collapsible Content
                AnimatedVisibility(
                    visible = isSubtitlesExpanded,
                    enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Subtitle languages ordered by priority (comma-separated)",
                            fontSize = 12.sp,
                            color = secondaryText,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val subLangContainerColor = if (isDark) {
                            Neutral.D0F172A.copy(alpha = (0.25f + 0.45f * alphaFactor).coerceIn(0.15f, 0.85f))
                        } else {
                            Neutral.FFFFFF.copy(alpha = (0.30f + 0.50f * alphaFactor).coerceIn(0.20f, 0.85f))
                        }
                        OutlinedTextField(
                            value = settings.preferredSubtitleLanguages,
                            onValueChange = { onSettingsChange(settings.copy(preferredSubtitleLanguages = it)) },
                            placeholder = {
                                Text(
                                    text = "e.g. eng, hin, jpn, spa",
                                    fontSize = 13.sp,
                                    color = secondaryText.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentSkyBlue,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = primaryText,
                                unfocusedTextColor = primaryText,
                                focusedContainerColor = subLangContainerColor,
                                unfocusedContainerColor = subLangContainerColor,
                                cursorColor = AccentSkyBlue
                            ),
                            trailingIcon = {
                                if (settings.preferredSubtitleLanguages.isNotBlank()) {
                                    IconButton(onClick = { onSettingsChange(settings.copy(preferredSubtitleLanguages = "")) }) {
                                        StyledIcon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Clear",
                                            tint = secondaryText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Presets
                        Text(
                            text = "Quick Presets:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val currentSubtitleTokens = remember(settings.preferredSubtitleLanguages) {
                            AudioLanguageMatcher.normalizePreferredLanguages(settings.preferredSubtitleLanguages)
                        }

                        val subtitlePresets = listOf(
                            "hin" to "Hindi (हिन्दी)",
                            "eng" to "English",
                            "jpn" to "Japanese (日本語)",
                            "kor" to "Korean (한국어)",
                            "spa" to "Spanish (Español)",
                            "fra" to "French (Français)"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subtitlePresets.forEach { (code, label) ->
                                val isPresent = currentSubtitleTokens.contains(code)
                                val chipBg = if (isPresent) {
                                    if (isDark) AccentSkyBlueStrong.copy(alpha = 0.40f) else AccentContainerLight
                                } else {
                                    if (isDark) Color(0x1AFFFFFF) else Neutral.F1F5F9
                                }
                                val chipBorder = if (isPresent) AccentSkyBlue else borderColor

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            val tokens = AudioLanguageMatcher.normalizePreferredLanguages(settings.preferredSubtitleLanguages).toMutableList()
                                            if (tokens.contains(code)) {
                                                tokens.remove(code)
                                            } else {
                                                tokens.add(code)
                                            }
                                            onSettingsChange(settings.copy(preferredSubtitleLanguages = tokens.joinToString(", ")))
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isPresent) {
                                            StyledIcon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = AccentSkyBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isPresent) AccentSkyBlue else primaryText
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. Apply to selected folder/videos only
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Apply to selected folder/videos only",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Restrict auto-selection rule to chosen directories instead of applying globally to all videos",
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            ViauraSwitch(
                                checked = settings.applySubtitleLanguageToSelectedContentOnly,
                                onCheckedChange = { onSettingsChange(settings.copy(applySubtitleLanguageToSelectedContentOnly = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentSkyBlue
                                )
                            )
                        }

                        AnimatedVisibility(
                            visible = settings.applySubtitleLanguageToSelectedContentOnly,
                            enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                            exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isDark) Neutral.D0F172A.copy(alpha = 0.85f)
                                            else Neutral.F1F5F9.copy(alpha = 0.95f)
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isDark) AccentSkyBlue.copy(alpha = 0.45f) else AccentSkyBlueStrong.copy(alpha = 0.35f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Header Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(AccentSkyBlue.copy(alpha = 0.18f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    StyledIcon(
                                                        imageVector = Icons.Outlined.FolderSpecial,
                                                        contentDescription = null,
                                                        tint = AccentSkyBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "Target Folders & Videos",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryText
                                                )
                                            }

                                            val totalCount = settings.selectedSubtitleFolders.size + settings.selectedSubtitleVideos.size
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (totalCount > 0) AccentSkyBlue.copy(alpha = 0.20f)
                                                        else if (isDark) Color.White.copy(alpha = 0.10f)
                                                        else Color.Black.copy(alpha = 0.06f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "$totalCount selected",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (totalCount > 0) AccentSkyBlue else secondaryText
                                                )
                                            }
                                        }

                                        // List of selected tags/chips
                                        val hasItems = settings.selectedSubtitleFolders.isNotEmpty() || settings.selectedSubtitleVideos.isNotEmpty()
                                        if (hasItems) {
                                            @OptIn(ExperimentalLayoutApi::class)
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                settings.selectedSubtitleFolders.forEach { folderPath ->
                                                    val folderName = File(folderPath).name.ifBlank { folderPath }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isDark) Neutral.D1E293B else AccentSkyBlueStrong.copy(alpha = 0.10f))
                                                            .border(
                                                                1.dp,
                                                                AccentSkyBlue.copy(alpha = 0.40f),
                                                                RoundedCornerShape(10.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            StyledIcon(
                                                                imageVector = Icons.Outlined.Folder,
                                                                contentDescription = null,
                                                                tint = AccentSkyBlue,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = folderName,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = primaryText,
                                                                maxLines = 1
                                                            )
                                                            StyledIcon(
                                                                imageVector = Icons.Outlined.Close,
                                                                contentDescription = "Remove",
                                                                tint = secondaryText,
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .clickable {
                                                                        val newSet = settings.selectedSubtitleFolders.toMutableSet()
                                                                        newSet.remove(folderPath)
                                                                        onSettingsChange(settings.copy(selectedSubtitleFolders = newSet))
                                                                    }
                                                            )
                                                        }
                                                    }
                                                }

                                                settings.selectedSubtitleVideos.forEach { videoPath ->
                                                    val videoName = File(videoPath).name.ifBlank { videoPath }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(if (isDark) Neutral.D1E293B else Color(0x1A6366F1))
                                                            .border(
                                                                1.dp,
                                                                AccentMid.copy(alpha = 0.40f),
                                                                RoundedCornerShape(10.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            StyledIcon(
                                                                imageVector = Icons.Outlined.Movie,
                                                                contentDescription = null,
                                                                tint = AccentMid,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = videoName,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = primaryText,
                                                                maxLines = 1
                                                            )
                                                            StyledIcon(
                                                                imageVector = Icons.Outlined.Close,
                                                                contentDescription = "Remove",
                                                                tint = secondaryText,
                                                                modifier = Modifier
                                                                    .size(14.dp)
                                                                    .clickable {
                                                                        val newSet = settings.selectedSubtitleVideos.toMutableSet()
                                                                        newSet.remove(videoPath)
                                                                        onSettingsChange(settings.copy(selectedSubtitleVideos = newSet))
                                                                    }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "No folders or videos selected yet. Tap '+' below to select directories or videos where auto-selection rule applies.",
                                                fontSize = 11.5.sp,
                                                color = secondaryText,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        // Plus Add Button
                                        Button(
                                            onClick = { onOpenFolderVideoSelector() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent
                                            ),
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(AccentGradient)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                StyledIcon(
                                                    imageVector = Icons.Outlined.Add,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "+ Add Folders or Videos",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Dual subtitles
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dual subtitles",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Enable displaying primary and secondary subtitles simultaneously",
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            ViauraSwitch(
                                checked = settings.dualSubtitlesEnabled,
                                onCheckedChange = { onSettingsChange(settings.copy(dualSubtitlesEnabled = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentSkyBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Signs & Songs subtitles
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Signs & Songs subtitles",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Prioritize signs, songs, and forced narrative subtitle tracks",
                                    fontSize = 12.sp,
                                    color = secondaryText,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            ViauraSwitch(
                                checked = settings.subtitleSignsAndSongs,
                                onCheckedChange = { onSettingsChange(settings.copy(subtitleSignsAndSongs = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AccentSkyBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Standalone Subtitle Settings outside the expandable card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PlayerSwitchRow(
                    title = "Detect subtitles by filename",
                    description = "Auto-detect external subtitle tracks matching video name",
                    checked = settings.detectSubtitlesByFilename,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onChange = { onSettingsChange(settings.copy(detectSubtitlesByFilename = it)) }
                )
                HorizontalDivider(color = borderColor)
                PlayerSwitchRow(
                    title = "Automatically load subtitles",
                    description = "Auto-detect and load external subtitle tracks matching video name",
                    checked = settings.autoLoadExternalSubtitles,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onChange = { onSettingsChange(settings.copy(autoLoadExternalSubtitles = it)) }
                )
                HorizontalDivider(color = borderColor)
                PlayerSwitchRow(
                    title = "Override ASS/SSA subtitles",
                    description = "Apply custom font styling and sizing to ASS/SSA subtitles",
                    checked = settings.overrideAssSsaSubtitles,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onChange = { onSettingsChange(settings.copy(overrideAssSsaSubtitles = it)) }
                )
                HorizontalDivider(color = borderColor)
                PlayerSwitchRow(
                    title = "Advanced ASS/SSA",
                    description = "Edit each style of the current ASS/SSA subtitle individually (Player → Subtitle Style)",
                    checked = settings.advancedAssEnabled,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onChange = { onSettingsChange(settings.copy(advancedAssEnabled = it)) }
                )
                HorizontalDivider(color = borderColor)
                PlayerSwitchRow(
                    title = "Scale by window",
                    description = "Scale subtitle font size proportionally with player window",
                    checked = settings.scaleSubtitlesByWindow,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onChange = { onSettingsChange(settings.copy(scaleSubtitlesByWindow = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        PlayerSettingsCategory("Fonts", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Show video-embedded fonts",
                description = if (settings.showVideoEmbeddedSubtitleFonts) {
                    "Show fonts extracted from the currently played video in subtitle font pickers"
                } else {
                    "Hide video-extracted fonts; only manually uploaded fonts remain selectable"
                },
                checked = settings.showVideoEmbeddedSubtitleFonts,
                primaryText = primaryText,
                secondaryText = secondaryText,
                onChange = { enabled ->
                    val selectedIsVideoFont = if (!enabled && settings.selectedSubtitleFont.isNotBlank()) {
                        val selected = SubtitleFontManager.getInstalledFonts(context, includeVideoExtracted = true)
                            .firstOrNull { file ->
                                file.name.equals(settings.selectedSubtitleFont, ignoreCase = true) ||
                                    file.nameWithoutExtension.equals(settings.selectedSubtitleFont, ignoreCase = true)
                            }
                        selected?.let { SubtitleFontManager.isVideoExtractedFont(context, it) } == true
                    } else false
                    onSettingsChange(
                        settings.copy(
                            showVideoEmbeddedSubtitleFonts = enabled,
                            selectedSubtitleFont = if (selectedIsVideoFont) "" else settings.selectedSubtitleFont
                        )
                    )
                }
            )

            HorizontalDivider(color = borderColor)

            PlayerActionRow(
                title = "Fonts directory",
                subtitle = if (settings.subtitleFontDirectoryUri.isBlank()) "Choose folder containing .ttf or .otf subtitle fonts" else "Custom font folder configured",
                actionLabel = if (settings.subtitleFontDirectoryUri.isBlank()) "Choose" else "Change",
                icon = R.drawable.viaura_folder_open,
                enabled = true,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                onClick = onChooseFontDirectory
            )

            HorizontalDivider(color = borderColor)

            PlayerActionRow(
                title = "Import font files",
                subtitle = "Select one or more .ttf / .otf files directly",
                actionLabel = "Import",
                icon = Icons.Outlined.Add,
                enabled = true,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                onClick = onPickFontFiles
            )

            HorizontalDivider(color = borderColor)

            // Expandable Loaded Fonts Box UI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { isFontsExpanded = !isFontsExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Loaded fonts library",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentSkyBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${installedFonts.size} loaded",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentSkyBlue,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFontsExpanded) "Click to collapse font container" else "Click extend to inspect, apply, or remove loaded font files",
                        fontSize = 12.sp,
                        color = secondaryText,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AccentSkyBlue.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, AccentSkyBlue.copy(alpha = 0.35f)),
                    onClick = { isFontsExpanded = !isFontsExpanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        StyledIcon(
                            imageVector = if (isFontsExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isFontsExpanded) "Collapse" else "Extend",
                            tint = AccentSkyBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFontsExpanded) "Collapse" else "Extend",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentSkyBlue
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isFontsExpanded,
                enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing))
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) {
                        Neutral.D0F172A.copy(alpha = (0.35f + 0.45f * alphaFactor).coerceIn(0.20f, 0.85f))
                    } else {
                        Neutral.F8FAFC.copy(alpha = (0.45f + 0.45f * alphaFactor).coerceIn(0.25f, 0.90f))
                    },
                    border = BorderStroke(1.dp, borderColor.copy(alpha = (0.5f + 0.5f * alphaFactor).coerceIn(0.3f, 0.95f))),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Box Header: Title + Badge + Remove All Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StyledIcon(
                                    imageVector = Icons.Outlined.FontDownload,
                                    contentDescription = null,
                                    tint = AccentSkyBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Loaded Font Repository",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                            }

                            if (installedFonts.any { SubtitleFontManager.isUserUploadedFont(context, it) }) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.40f)),
                                    onClick = { pendingDeleteFont = "\u0000ALL" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        StyledIcon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Remove All",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Remove All",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = borderColor.copy(alpha = 0.4f))

                        if (installedFonts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "No custom fonts loaded yet",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText
                                    )
                                    Text(
                                        text = "Use 'Import font files' or 'Fonts directory' above to add .ttf or .otf fonts.",
                                        fontSize = 11.5.sp,
                                        color = secondaryText,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                installedFonts.forEach { fontFile ->
                                    val isVideoFont = SubtitleFontManager.isVideoExtractedFont(context, fontFile)
                                    val isSelected = settings.selectedSubtitleFont.equals(fontFile.name, ignoreCase = true) ||
                                        settings.selectedSubtitleFont.equals(fontFile.nameWithoutExtension, ignoreCase = true)
                                    val fileSizeKb = (fontFile.length() / 1024).coerceAtLeast(1)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) AccentSkyBlue.copy(alpha = 0.12f)
                                        else if (isDark) Neutral.D1E293B.copy(alpha = 0.45f)
                                        else Color.White.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, if (isSelected) AccentSkyBlue.copy(alpha = 0.45f) else borderColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) AccentSkyBlue.copy(alpha = 0.20f)
                                                    else if (isDark) Color(0x33FFFFFF)
                                                    else Color(0x1A000000),
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        StyledIcon(
                                                            imageVector = Icons.Outlined.FontDownload,
                                                            contentDescription = null,
                                                            tint = if (isSelected) AccentSkyBlue else primaryText,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    val originalFontName = SubtitleFontManager.getOriginalFontName(fontFile)
                                                    Text(
                                                        text = originalFontName,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) AccentSkyBlue else primaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (!originalFontName.equals(fontFile.name, ignoreCase = true)) {
                                                            Text(
                                                                text = fontFile.name,
                                                                fontSize = 11.sp,
                                                                color = secondaryText.copy(alpha = 0.85f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "•",
                                                                fontSize = 11.sp,
                                                                color = secondaryText.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                        Text(
                                                            text = "${fileSizeKb} KB",
                                                            fontSize = 11.sp,
                                                            color = secondaryText
                                                        )
                                                        if (isSelected) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = AccentSkyBlue.copy(alpha = 0.20f)
                                                            ) {
                                                                Text(
                                                                    text = "Active",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = AccentSkyBlue,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        } else {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isVideoFont) "Video font" else "Tap to apply",
                                                                    fontSize = 10.5.sp,
                                                                    color = if (isVideoFont) secondaryText else AccentSkyBlue,
                                                                    modifier = if (isVideoFont) Modifier else Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .clickable {
                                                                            onSettingsChange(settings.copy(selectedSubtitleFont = fontFile.name))
                                                                        }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Only manually uploaded fonts are removable. Video-extracted
                                            // fonts remain available for the current renderer/session.
                                            if (!isVideoFont) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                                    onClick = { pendingDeleteFont = fontFile.name }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        StyledIcon(
                                                            imageVector = Icons.Outlined.Delete,
                                                            contentDescription = "Remove font",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "Remove",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFFEF4444)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = borderColor)

            PlayerActionRow(
                title = "Reload fonts",
                subtitle = "Rescan font directory and refresh loaded font list",
                actionLabel = "Reload",
                icon = Icons.Outlined.Refresh,
                enabled = settings.subtitleFontDirectoryUri.isNotBlank() || installedFonts.isNotEmpty(),
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                onClick = onReloadFonts
            )
        }
    }
}

@Composable
internal fun GestureSettingsPanel(
    settings: PlayerSettings,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    alphaFactor: Float = 1.0f,
    onSettingsChange: (PlayerSettings) -> Unit
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PlayerSettingsCategory("Swipe & Speed", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Brightness gestures",
                description = "Swipe vertically on left side to adjust screen brightness",
                checked = settings.brightnessGestures,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(brightnessGestures = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Volume gestures",
                description = "Swipe vertically on right side to adjust media volume",
                checked = settings.volumeGestures,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(volumeGestures = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Swap volume and brightness slider",
                description = "Swipe left for volume and right for brightness",
                checked = settings.swapVolumeBrightness,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(swapVolumeBrightness = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Pinch to zoom",
                description = "Pinch with two fingers to zoom and scale video frame",
                checked = settings.pinchToZoom,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(pinchToZoom = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Pinch to zoom subtitles",
                description = "Pinch subtitle text with two fingers to resize",
                checked = settings.pinchToZoomSubtitles,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(pinchToZoomSubtitles = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Swipe subtitles to seek dialog",
                description = "Swipe subtitle area to open seek jump dialog",
                checked = settings.swipeSubtitlesToSeekDialog,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(swipeSubtitlesToSeekDialog = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Horizontal swipe to seek",
                description = "Swipe horizontally anywhere to seek forward or backward",
                checked = settings.horizontalSwipeToSeek,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(horizontalSwipeToSeek = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Swipe up in center for playlist",
                description = "Swipe upward from center of player to display playlist",
                checked = settings.swipeUpCenterForPlaylist,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(swipeUpCenterForPlaylist = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSliderRow(
                title = "Horizontal swipe sensitivity",
                description = "Maximum seek jump percentage when swiping across the entire screen",
                value = settings.horizontalSwipeSensitivity,
                min = 1,
                max = 100,
                unit = "%",
                presets = listOf(5, 10, 15, 20, 25, 50, 100),
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(horizontalSwipeSensitivity = it)) }
        }

        PlayerSettingsCategory("Hold & Speed", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Hold for multi-x speed",
                description = "Long-press and hold screen to accelerate playback speed",
                checked = settings.holdMultiSpeed,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(holdMultiSpeed = it)) }

            HorizontalDivider(color = borderColor)

            PlayerChoiceRow(
                title = "Hold speed multiplier",
                description = "Playback rate applied during long-press hold",
                value = String.format(java.util.Locale.US, "%.2f", settings.holdSpeedMultiplier),
                options = listOf(
                    "1.25" to "1.25x",
                    "1.50" to "1.50x",
                    "2.00" to "2.00x",
                    "2.50" to "2.50x",
                    "3.00" to "3.00x",
                    "4.00" to "4.00x"
                ),
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { onSettingsChange(settings.copy(holdSpeedMultiplier = it.toDoubleOrNull() ?: 2.0)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Dynamic Speed Overlay",
                description = "Display animated speed indicator badge during long-press",
                checked = settings.dynamicSpeedOverlay,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(dynamicSpeedOverlay = it)) }
        }

        // Mutual exclusion callbacks between Single Tap and Double Tap
        val onDoubleTapLeftChange: (String) -> Unit = { newVal ->
            var updated = settings.copy(doubleTapLeftAction = newVal)
            if (newVal != "NONE") {
                if (updated.singleTapLeftAction == newVal || updated.singleTapLeftAction != "NONE") {
                    updated = updated.copy(singleTapLeftAction = "NONE")
                }
                if (updated.singleTapCenterAction == newVal) {
                    updated = updated.copy(singleTapCenterAction = "NONE")
                }
                if (updated.singleTapRightAction == newVal) {
                    updated = updated.copy(singleTapRightAction = "NONE")
                }
            }
            onSettingsChange(updated)
        }

        val onDoubleTapCenterChange: (String) -> Unit = { newVal ->
            var updated = settings.copy(doubleTapCenterAction = newVal)
            if (newVal != "NONE") {
                if (updated.singleTapCenterAction == newVal || updated.singleTapCenterAction != "NONE") {
                    updated = updated.copy(singleTapCenterAction = "NONE")
                }
                if (updated.singleTapLeftAction == newVal) {
                    updated = updated.copy(singleTapLeftAction = "NONE")
                }
                if (updated.singleTapRightAction == newVal) {
                    updated = updated.copy(singleTapRightAction = "NONE")
                }
            }
            onSettingsChange(updated)
        }

        val onDoubleTapRightChange: (String) -> Unit = { newVal ->
            var updated = settings.copy(doubleTapRightAction = newVal)
            if (newVal != "NONE") {
                if (updated.singleTapRightAction == newVal || updated.singleTapRightAction != "NONE") {
                    updated = updated.copy(singleTapRightAction = "NONE")
                }
                if (updated.singleTapLeftAction == newVal) {
                    updated = updated.copy(singleTapLeftAction = "NONE")
                }
                if (updated.singleTapCenterAction == newVal) {
                    updated = updated.copy(singleTapCenterAction = "NONE")
                }
            }
            onSettingsChange(updated)
        }

        val onSingleTapLeftChange: (String) -> Unit = { newVal ->
            var updated = settings.copy(singleTapLeftAction = newVal)
            if (newVal != "NONE") {
                if (updated.doubleTapLeftAction == newVal || updated.doubleTapLeftAction != "NONE") {
                    updated = updated.copy(doubleTapLeftAction = "NONE")
                }
                if (updated.doubleTapCenterAction == newVal) {
                    updated = updated.copy(doubleTapCenterAction = "NONE")
                }
                if (updated.doubleTapRightAction == newVal) {
                    updated = updated.copy(doubleTapRightAction = "NONE")
                }
            }
            onSettingsChange(updated)
        }

        val onSingleTapCenterChange: (String) -> Unit = { newVal ->
            var updated = settings.copy(singleTapCenterAction = newVal)
            if (newVal != "NONE") {
                if (updated.doubleTapCenterAction == newVal || updated.doubleTapCenterAction != "NONE") {
                    updated = updated.copy(doubleTapCenterAction = "NONE")
                }
                if (updated.doubleTapLeftAction == newVal) {
                    updated = updated.copy(doubleTapLeftAction = "NONE")
                }
                if (updated.doubleTapRightAction == newVal) {
                    updated = updated.copy(doubleTapRightAction = "NONE")
                }
            }
            onSettingsChange(updated)
        }

        val onSingleTapRightChange: (String) -> Unit = { newVal ->
            var updated = settings.copy(singleTapRightAction = newVal)
            if (newVal != "NONE") {
                if (updated.doubleTapRightAction == newVal || updated.doubleTapRightAction != "NONE") {
                    updated = updated.copy(doubleTapRightAction = "NONE")
                }
                if (updated.doubleTapLeftAction == newVal) {
                    updated = updated.copy(doubleTapLeftAction = "NONE")
                }
                if (updated.doubleTapCenterAction == newVal) {
                    updated = updated.copy(doubleTapCenterAction = "NONE")
                }
            }
            onSettingsChange(updated)
        }

        PlayerSettingsCategory("Double Tap", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Enable Double Tap",
                description = "Master toggle for all double tap gestures and shortcut actions",
                checked = settings.enableDoubleTap,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(enableDoubleTap = it)) }

            AnimatedVisibility(
                visible = settings.enableDoubleTap,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(modifier = Modifier.height(0.dp))
                    HorizontalDivider(color = borderColor)

                    PlayerSliderRow(
                        title = "Double tap seek duration",
                        description = "Seconds to seek per double-tap gesture (1s, 5s, 10s, etc.)",
                        value = settings.doubleTapSeekDuration,
                        min = 1,
                        max = 120,
                        unit = "s",
                        presets = listOf(1, 3, 5, 10, 15, 20, 30, 60),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    ) { onSettingsChange(settings.copy(doubleTapSeekDuration = it)) }

                    HorizontalDivider(color = borderColor)

                    PlayerSliderRow(
                        title = "Double Tap Seek Area Width",
                        description = "Horizontal width percentage of double-tap trigger zones",
                        value = settings.doubleTapSeekAreaWidth,
                        min = 10,
                        max = 50,
                        unit = "%",
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    ) { onSettingsChange(settings.copy(doubleTapSeekAreaWidth = it)) }

                    HorizontalDivider(color = borderColor)

                    PlayerChoiceRow(
                        title = "Double tap (left)",
                        description = "Action triggered by double tapping left side of screen",
                        value = settings.doubleTapLeftAction,
                        options = listOf("SEEK_BACK" to "Seek / Back", "PLAY_PAUSE" to "Play / Pause", "NONE" to "None"),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        alphaFactor = alphaFactor,
                        onChange = onDoubleTapLeftChange
                    )

                    HorizontalDivider(color = borderColor)

                    PlayerChoiceRow(
                        title = "Double tap (center)",
                        description = "Action triggered by double tapping center of screen",
                        value = settings.doubleTapCenterAction,
                        options = listOf("PLAY_PAUSE" to "Play / Pause", "SEEK_BACK" to "Seek / Back", "SEEK_FORWARD" to "Seek / Forward", "NONE" to "None"),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        alphaFactor = alphaFactor,
                        onChange = onDoubleTapCenterChange
                    )

                    HorizontalDivider(color = borderColor)

                    PlayerChoiceRow(
                        title = "Double tap (right)",
                        description = "Action triggered by double tapping right side of screen",
                        value = settings.doubleTapRightAction,
                        options = listOf("SEEK_FORWARD" to "Seek / Forward", "PLAY_PAUSE" to "Play / Pause", "NONE" to "None"),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        alphaFactor = alphaFactor,
                        onChange = onDoubleTapRightChange
                    )
                }
            }
        }

        PlayerSettingsCategory("Single Tap", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Enable Single Tap",
                description = "Master toggle for single tap zone gestures and shortcut actions",
                checked = settings.enableSingleTap,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { enabled ->
                onSettingsChange(
                    settings.copy(
                        enableSingleTap = enabled,
                        singleTapLeftAction = "NONE",
                        singleTapCenterAction = "NONE",
                        singleTapRightAction = "NONE"
                    )
                )
            }

            AnimatedVisibility(
                visible = settings.enableSingleTap,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Spacer(modifier = Modifier.height(0.dp))
                    HorizontalDivider(color = borderColor)

                    PlayerSliderRow(
                        title = "Single tap seek duration",
                        description = "Seconds to seek per single-tap gesture (1s, 5s, 10s, etc.)",
                        value = settings.singleTapSeekDuration,
                        min = 1,
                        max = 120,
                        unit = "s",
                        presets = listOf(1, 3, 5, 10, 15, 20, 30, 60),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    ) { onSettingsChange(settings.copy(singleTapSeekDuration = it)) }

                    HorizontalDivider(color = borderColor)

                    PlayerSliderRow(
                        title = "Single Tap Seek Area Width",
                        description = "Horizontal width percentage of single-tap trigger zones",
                        value = settings.singleTapSeekAreaWidth,
                        min = 10,
                        max = 50,
                        unit = "%",
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText
                    ) { onSettingsChange(settings.copy(singleTapSeekAreaWidth = it)) }

                    HorizontalDivider(color = borderColor)

                    PlayerChoiceRow(
                        title = "Single tap (left)",
                        description = "Action triggered by single tapping left side of screen",
                        value = settings.singleTapLeftAction,
                        options = listOf("SEEK_BACK" to "Seek / Back", "PLAY_PAUSE" to "Play / Pause", "NONE" to "None"),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        alphaFactor = alphaFactor,
                        onChange = onSingleTapLeftChange
                    )

                    HorizontalDivider(color = borderColor)

                    PlayerChoiceRow(
                        title = "Single tap (center)",
                        description = "Action triggered by single tapping center of screen",
                        value = settings.singleTapCenterAction,
                        options = listOf("PLAY_PAUSE" to "Play / Pause", "SEEK_BACK" to "Seek / Back", "SEEK_FORWARD" to "Seek / Forward", "NONE" to "None"),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        alphaFactor = alphaFactor,
                        onChange = onSingleTapCenterChange
                    )

                    HorizontalDivider(color = borderColor)

                    PlayerChoiceRow(
                        title = "Single tap (right)",
                        description = "Action triggered by single tapping right side of screen",
                        value = settings.singleTapRightAction,
                        options = listOf("SEEK_FORWARD" to "Seek / Forward", "PLAY_PAUSE" to "Play / Pause", "NONE" to "None"),
                        isDark = isDark,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        borderColor = borderColor,
                        alphaFactor = alphaFactor,
                        onChange = onSingleTapRightChange
                    )
                }
            }
        }

        PlayerSettingsCategory("Subtitle Gestures", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Hold and drag moves subtitles",
                description = "Long press on subtitle text and drag to reposition on screen",
                checked = settings.holdDragMovesSubtitles,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(holdDragMovesSubtitles = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Swipe on subtitle jumps between subtitles",
                description = "Touch the subtitle area and swipe left for the previous subtitle or right for the next one; swiping elsewhere still seeks the video",
                checked = settings.swipeOnSubtitleSeeksCues,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(swipeOnSubtitleSeeksCues = it)) }
        }

        PlayerSettingsCategory("Media controls", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Previous",
                description = "Enable gesture or media button for previous item",
                checked = settings.mediaPreviousControl,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(mediaPreviousControl = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Play/Pause",
                description = "Enable gesture or media button for toggle play/pause",
                checked = settings.mediaPlayPauseControl,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(mediaPlayPauseControl = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Next",
                description = "Enable gesture or media button for next item",
                checked = settings.mediaNextControl,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(mediaNextControl = it)) }
        }
    }
}

@Composable
internal fun PlayerSettingsPanel(
    settings: PlayerSettings,
    onSettingsChange: (PlayerSettings) -> Unit,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    alphaFactor: Float = 1.0f
) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PlayerSettingsCategory("General", cardBg, borderColor, secondaryText) {
            PlayerChoiceRow(
                title = "Orientation",
                description = "Video playback orientation and screen lock behavior",
                value = settings.orientation,
                options = listOf(
                    "FREE" to "Free",
                    "VIDEO" to "Video",
                    "PORTRAIT" to "Portrait",
                    "REVERSE_PORTRAIT" to "Reverse portrait",
                    "SENSOR_PORTRAIT" to "Sensor portrait",
                    "LANDSCAPE" to "Landscape",
                    "REVERSE_LANDSCAPE" to "Reverse landscape",
                    "SENSOR_LANDSCAPE" to "Sensor landscape"
                ),
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { v -> onSettingsChange(settings.copy(orientation = v)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Save position on quit",
                description = "Resume video playback from last known position",
                checked = settings.savePositionOnQuit,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(savePositionOnQuit = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Close after the end of playback",
                description = "Automatically exit player when playback finishes",
                checked = settings.closeAfterEnd,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(closeAfterEnd = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Autoplay next video",
                description = "Play next video in folder or queue automatically",
                checked = settings.autoplayNext,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(autoplayNext = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Repeat playlist after last episode",
                description = "Restart the playlist from the first episode after the last episode finishes",
                checked = settings.repeatPlaylistAfterLast,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(repeatPlaylistAfterLast = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Enable next/previous navigation",
                description = "Show previous and next track controls on overlay",
                checked = settings.enableNextPrevious,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(enableNextPrevious = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Remember display brightness",
                description = "Restore individual screen brightness level for each session",
                checked = settings.rememberBrightness,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(rememberBrightness = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Auto Picture-in-Picture",
                description = "Switch to PiP mode automatically when minimizing player",
                checked = settings.autoPictureInPicture,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(autoPictureInPicture = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Video mini player",
                description = "Keep the video playing in a floating mini player (like audio) when you leave the player",
                checked = settings.videoMiniPlayer,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(videoMiniPlayer = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Autoplay after screen unlock",
                description = "Resume playback automatically when device is unlocked",
                checked = settings.autoplayAfterUnlock,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(autoplayAfterUnlock = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show Media Info in chooser",
                description = "Display audio and video technical codecs in track chooser",
                checked = settings.showMediaInfoChooser,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(showMediaInfoChooser = it)) }
        }

        PlayerSettingsCategory("Display & Controls", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Pinned video layout",
                description = "In portrait the video stays pinned at the top while details, related videos and comments scroll below it",
                checked = settings.portraitChannelPlayer,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(if (it) settings.copy(portraitChannelPlayer = true, videoMiniPlayer = true) else settings.copy(portraitChannelPlayer = false)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show system status bar with controls",
                description = "Display battery and time status bar when controls are visible",
                checked = settings.showStatusBar,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(showStatusBar = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show navigation bar with controls",
                description = "Display Android navigation buttons when controls are visible",
                checked = settings.showNavigationBar,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(showNavigationBar = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Safe Area Window",
                description = "Pad player margins around notches and curved screen edges",
                checked = settings.safeAreaWindow,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(safeAreaWindow = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Reduce player animation",
                description = "Minimize motion effects for faster UI performance",
                checked = settings.reduceAnimation,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(reduceAnimation = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show loading circle",
                description = "Display circular progress spinner during video loading",
                checked = settings.showLoadingCircle,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(showLoadingCircle = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Swap volume and brightness slider",
                description = "Swipe left for volume and right for brightness",
                checked = settings.swapVolumeBrightness,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(swapVolumeBrightness = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show slider on opposite side",
                description = "Swipe on one side and the volume or brightness slider appears on the other side",
                checked = settings.gestureHudOppositeSide,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(gestureHudOppositeSide = it)) }
        }

        PlayerSettingsCategory("Seeking", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Show ripple when double tap seeking",
                description = "Display animated ripple indicator when double tapping",
                checked = settings.rippleOnDoubleTap,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(rippleOnDoubleTap = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show seek time",
                description = "Display relative seek target timestamp during swipe gestures",
                checked = settings.showSeekTime,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(showSeekTime = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Show buffered range on seek bar",
                description = "Display cached playback buffer range on progress bar",
                checked = settings.showBufferedRange,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(showBufferedRange = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Use precise seeking",
                description = "Seek to exact requested frame rather than nearest keyframe",
                checked = settings.preciseSeeking,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(preciseSeeking = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Use ThumbFast seek preview",
                description = "Display rapid visual thumbnail preview while seek scrubbing",
                checked = settings.thumbFastPreview,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(thumbFastPreview = it)) }
        }

        PlayerSettingsCategory("Skip", cardBg, borderColor, secondaryText) {
            PlayerSliderRow(
                title = "Custom skip duration",
                description = "Duration in seconds to jump when pressing skip buttons",
                value = settings.customSkipDuration,
                min = 1,
                max = 600,
                unit = "s",
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(customSkipDuration = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Use online skip markers",
                description = "Query online chapter and intro databases automatically",
                checked = settings.onlineSkipMarkers,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(onlineSkipMarkers = it)) }

            HorizontalDivider(color = borderColor)

            PlayerChoiceRow(
                title = "Online marker provider",
                description = "Preferred service for fetching intro and outro timestamps",
                value = settings.markerProvider,
                options = listOf("INTRODB" to "IntroDB", "TIDB" to "TIDB", "ANISKIP" to "AniSkip"),
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { v -> onSettingsChange(settings.copy(markerProvider = v)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Detect intro/outro from chapter titles",
                description = "Auto-identify openings and endings from embedded chapter titles",
                checked = settings.detectChapterIntroOutro,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(detectChapterIntroOutro = it)) }

            HorizontalDivider(color = borderColor)

            PlayerTextRow(
                title = "Custom keywords for skipping openings",
                description = "Comma-separated keywords used to detect intro markers",
                value = settings.customOpeningKeywords,
                placeholder = "e.g. OP, Opening, Intro",
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { onSettingsChange(settings.copy(customOpeningKeywords = it)) }

            HorizontalDivider(color = borderColor)

            PlayerTextRow(
                title = "Custom keywords for skipping endings",
                description = "Comma-separated keywords used to detect ending markers",
                value = settings.customEndingKeywords,
                placeholder = "e.g. ED, Ending, Outro",
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { onSettingsChange(settings.copy(customEndingKeywords = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Auto-skip intro",
                description = "Automatically jump past detected opening chapter segment",
                checked = settings.autoSkipIntro,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(autoSkipIntro = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Auto-skip outro",
                description = "Automatically jump to next episode when ending segment starts",
                checked = settings.autoSkipOutro,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(autoSkipOutro = it)) }
        }

        PlayerSettingsCategory("Screenshots", cardBg, borderColor, secondaryText) {
            PlayerChoiceRow(
                title = "Image format",
                description = "Image file encoding format for captured frames",
                value = settings.screenshotFormat,
                options = listOf("PNG" to "PNG", "JPG" to "JPG", "WEBP" to "WebP"),
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { v -> onSettingsChange(settings.copy(screenshotFormat = v)) }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Include subtitles in screenshots",
                description = "Burn visible subtitle text directly into captured images",
                checked = settings.subtitlesInScreenshots,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(subtitlesInScreenshots = it)) }

            HorizontalDivider(color = borderColor)

            PlayerTextRow(
                title = "Filename template",
                description = "Naming template pattern for saved screenshots",
                value = settings.screenshotFilenameTemplate,
                placeholder = "e.g. %F_%wH.%wM.%wS",
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) { onSettingsChange(settings.copy(screenshotFilenameTemplate = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSliderRow(
                title = "JPEG/WebP quality",
                description = "Compression quality level for lossy captures",
                value = settings.jpegWebpQuality,
                min = 1,
                max = 100,
                unit = "%",
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(jpegWebpQuality = it)) }

            HorizontalDivider(color = borderColor)

            PlayerSliderRow(
                title = "PNG compression",
                description = "Compression level from 0 (fastest) to 9 (smallest file)",
                value = settings.pngCompression,
                min = 0,
                max = 9,
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) { onSettingsChange(settings.copy(pngCompression = it)) }
        }
    }
}

@Composable
internal fun PlayerSettingsCategory(
    title: String,
    cardBg: Color,
    borderColor: Color,
    secondaryText: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        }
    }
}

@Composable
internal fun PlayerSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!enabled) Modifier.alpha(0.45f) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ViauraSwitch(
            checked = checked && enabled,
            onCheckedChange = if (enabled) onChange else { _ -> },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentSkyBlue
            )
        )
    }
}

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
    alphaFactor: Float,
    searchable: Boolean = false,
    onChange: (String) -> Unit
) {
    var expanded by remember(value) { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == value }?.second ?: value

    // Searchable menus (Language): the menu opens only in the space BELOW the touched pill, with a search box on top
    var anchorBottomPx by remember { mutableFloatStateOf(0f) }
    var searchQuery by remember(expanded) { mutableStateOf("") }
    val menuDensity = LocalDensity.current
    val windowHeightPx = LocalView.current.rootView.height
    val navBottomPx = WindowInsets.navigationBars.getBottom(menuDensity)
    val menuMaxHeight = with(menuDensity) {
        // 48dp = the vertical margin Compose keeps around a dropdown; 8dp = small breathing room
        val below = windowHeightPx - anchorBottomPx - navBottomPx - 56.dp.toPx()
        below.toDp().coerceAtLeast(200.dp)
    }

    // Obtain dynamic transparency from slider (10% to 100%)
    val currentGlassTrans = LocalGlassBlurTransparency.current
    val effectiveFactor = if (alphaFactor != 1.0f) {
        alphaFactor.coerceIn(0.10f, 1.0f)
    } else {
        (currentGlassTrans / 100f).coerceIn(0.10f, 1.0f)
    }

    // Dynamic glass opacity smoothly interpolates across slider (10% to 100%)
    // Uses high baseline opacity (86% to 99%) so background controls/text do not show through unevenly,
    // while providing genuine frosted glass translucency and seamlessly reacting to the UI Glass Blur & Opacity slider.
    val dropdownBg = glassPanelColor(isDark, LocalPanelTransparency.current)

    val dropdownBorder = if (isDark) {
        AccentSkyBlue.copy(alpha = (0.35f + 0.35f * effectiveFactor).coerceIn(0.35f, 0.70f))
    } else {
        AccentSkyBlueStrong.copy(alpha = (0.30f + 0.35f * effectiveFactor).coerceIn(0.30f, 0.65f))
    }

    val unselectedTextColor = if (isDark) Neutral.F8FAFC else Neutral.D0F172A
    val accentColor = if (isDark) AccentSkyBlue else AccentSkyBlueStrong
    val selectedItemBg = if (isDark) {
        AccentSkyBlue.copy(alpha = 0.20f).copy(alpha = (0.22f + 0.20f * effectiveFactor).coerceIn(0.22f, 0.45f))
    } else {
        AccentSkyBlueStrong.copy(alpha = (0.12f + 0.14f * effectiveFactor).coerceIn(0.12f, 0.26f))
    }

    val menuShape = RoundedCornerShape(16.dp)

    // The menu opens inside the settings card: its right edge sits on the right edge of the pill and it grows to the left only,
    // never wider than the card itself; thin, and glass like the other panels while Liquid Glass is on.
    val glassOn = LocalLiquidGlass.current
    val rowDensity = LocalDensity.current
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    val menuWidth = with(rowDensity) { minOf(if (searchable) 210.dp else 172.dp, rowWidthPx.toDp().coerceAtLeast(120.dp)) }
    val menuOffset = DpOffset(with(rowDensity) { pillWidthPx.toDp() } - menuWidth, 0.dp)

    Row(
        modifier = Modifier.fillMaxWidth().onSizeChanged { rowWidthPx = it.width.toFloat() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.onGloballyPositioned { anchorBottomPx = it.boundsInWindow().bottom }.onSizeChanged { pillWidthPx = it.width.toFloat() }) {
            val pillAccent = accentColor
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (expanded) {
                    if (isDark) AccentSkyBlue.copy(alpha = 0.20f) else AccentSkyBlueStrong.copy(alpha = 0.13f)
                } else if (isDark) {
                    Neutral.D1E293B.copy(alpha = (0.20f + 0.60f * effectiveFactor).coerceIn(0.15f, 0.85f))
                } else {
                    AccentSkyBlueStrong.copy(alpha = (0.07f + 0.15f * effectiveFactor).coerceIn(0.07f, 0.22f))
                },
                border = if (expanded) {
                    BorderStroke(1.2.dp, pillAccent)
                } else {
                    BorderStroke(1.dp, borderColor.copy(alpha = (0.35f + 0.55f * effectiveFactor).coerceIn(0.35f, 0.90f)))
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
                        color = pillAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                    StyledIcon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        tint = pillAccent,
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
                shadowElevation = 0.dp,
                border = if (glassOn) null else BorderStroke(1.2.dp, dropdownBorder),
                modifier = Modifier
                    .width(menuWidth)
                    .clip(menuShape)
                    .then(if (glassOn) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
                    .liquidGlassEdge(menuShape, isDark)
            ) {
                val renderOption: @Composable (String, String) -> Unit = { key, label ->
                    val isSelected = key == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) selectedItemBg else Color.Transparent
                            )
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

                if (!searchable) {
                    options.forEach { (key, label) -> renderOption(key, label) }
                } else {
                    val q = searchQuery.trim()
                    val filtered = if (q.isEmpty()) options else options.filter {
                        it.second.contains(q, ignoreCase = true) || it.first.equals(q, ignoreCase = true)
                    }
                    val listScroll = rememberScrollState()
                    val listMaxHeight = (menuMaxHeight - 58.dp).coerceAtLeast(120.dp)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Search box pinned above the list; the list fades out underneath it while it scrolls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = if (isDark) 0.14f else 0.09f))
                                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(text = "Search", fontSize = 13.5.sp, color = unselectedTextColor.copy(alpha = 0.55f))
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.5.sp,
                                        color = unselectedTextColor,
                                        fontFamily = LocalAppFontFamily.current
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = listMaxHeight)
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    // the more the list is scrolled, the softer the part that slides under the search box
                                    val fadePx = 30.dp.toPx() * (listScroll.value / 36f).coerceIn(0f, 1f)
                                    if (fadePx > 0.5f) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startY = 0f,
                                                endY = fadePx
                                            ),
                                            size = androidx.compose.ui.geometry.Size(size.width, fadePx),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                }
                                .verticalScroll(listScroll)
                        ) {
                            if (filtered.isEmpty()) {
                                Text(
                                    text = "No results",
                                    fontSize = 13.sp,
                                    color = unselectedTextColor.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                )
                            } else {
                                filtered.forEach { (key, label) -> renderOption(key, label) }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerSliderRow(
    title: String,
    description: String,
    value: Int,
    min: Int,
    max: Int,
    unit: String = "",
    presets: List<Int>? = null,
    isDark: Boolean,
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    onChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = secondaryText,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGradient)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$value$unit",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        if (!presets.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { preset ->
                    val isSelected = preset == value
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AccentSkyBlue else if (isDark) Neutral.D1E293B else AccentSkyBlueStrong.copy(alpha = 0.10f),
                        modifier = Modifier.clickable { onChange(preset) }
                    ) {
                        Text(
                            text = "$preset$unit",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else primaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        ViauraSlider(
            value = value.toFloat().coerceIn(min.toFloat(), max.toFloat()),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AccentPink,
                activeTrackColor = AccentSkyBlue,
                inactiveTrackColor = if (isDark) AccentSkyBlue.copy(alpha = 0.22f) else AccentSkyBlue.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayerTextRow(
    title: String,
    description: String,
    value: String,
    placeholder: String = "",
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    borderColor: Color = DarkGlassBorder,
    alphaFactor: Float = 1.0f,
    onChange: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    val isDark = primaryText == DarkTextPrimary
    val textFieldContainerColor = if (isDark) {
        Neutral.D0F172A.copy(alpha = (0.25f + 0.45f * alphaFactor).coerceIn(0.15f, 0.85f))
    } else {
        Neutral.FFFFFF.copy(alpha = (0.30f + 0.50f * alphaFactor).coerceIn(0.20f, 0.85f))
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryText
        )
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onChange(it)
            },
            placeholder = if (placeholder.isNotBlank()) {
                { Text(placeholder, fontSize = 13.sp, color = secondaryText.copy(alpha = 0.6f)) }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentSkyBlue,
                unfocusedBorderColor = borderColor,
                focusedTextColor = primaryText,
                unfocusedTextColor = primaryText,
                focusedContainerColor = textFieldContainerColor,
                unfocusedContainerColor = textFieldContainerColor,
                cursorColor = AccentSkyBlue
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayerActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    icon: ImageVector,
    enabled: Boolean = true,
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    borderColor: Color = DarkGlassBorder,
    onClick: () -> Unit
) {
    PlayerActionRow(
        title = title,
        subtitle = subtitle,
        actionLabel = actionLabel,
        iconContent = {
            StyledIcon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AccentSkyBlue else secondaryText,
                modifier = Modifier.size(16.dp)
            )
        },
        enabled = enabled,
        primaryText = primaryText,
        secondaryText = secondaryText,
        borderColor = borderColor,
        onClick = onClick
    )
}

@Composable
private fun PlayerActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    icon: Int,
    enabled: Boolean = true,
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    borderColor: Color = DarkGlassBorder,
    onClick: () -> Unit
) {
    PlayerActionRow(
        title = title,
        subtitle = subtitle,
        actionLabel = actionLabel,
        iconContent = {
            StyledIcon(
                drawableRes = icon,
                contentDescription = null,
                tint = if (enabled) AccentSkyBlue else secondaryText,
                modifier = Modifier.size(16.dp)
            )
        },
        enabled = enabled,
        primaryText = primaryText,
        secondaryText = secondaryText,
        borderColor = borderColor,
        onClick = onClick
    )
}

@Composable
private fun PlayerActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    iconContent: @Composable () -> Unit,
    enabled: Boolean = true,
    primaryText: Color = DarkTextPrimary,
    secondaryText: Color = DarkTextSecondary,
    borderColor: Color = DarkGlassBorder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) primaryText else secondaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (enabled) AccentSkyBlue.copy(alpha = 0.15f) else Color.Transparent,
            border = BorderStroke(1.dp, if (enabled) AccentSkyBlue.copy(alpha = 0.35f) else borderColor),
            onClick = onClick,
            enabled = enabled
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                iconContent()
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) AccentSkyBlue else secondaryText
                )
            }
        }
    }
}

/**
 * Settings > Network: HLS stream handling and the subtitle options for pasted links. Built from the same rows as the other tabs.
 */
@Composable
internal fun NetworkSettingsPanel(
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    alphaFactor: Float
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hls by remember { mutableStateOf(com.example.util.NetworkSettings.hlsHandlingEnabled(context)) }
    var subs by remember { mutableStateOf(com.example.util.NetworkSettings.downloadSubtitles(context)) }
    var autoSubs by remember { mutableStateOf(com.example.util.NetworkSettings.autoSubtitles(context)) }

    // Media settings: language, play quality and download quality of the Media providers
    var mediaLanguage by remember { mutableStateOf(com.example.util.NetworkSettings.mediaLanguage(context)) }
    var mediaAutoSubtitle by remember { mutableStateOf(com.example.util.NetworkSettings.mediaAutoSubtitle(context)) }
    var mediaPlayQuality by remember { mutableStateOf(com.example.util.NetworkSettings.mediaPlayQuality(context)) }
    var mediaDownloadQuality by remember { mutableStateOf(com.example.util.NetworkSettings.mediaDownloadQuality(context)) }
    val languageOptions = remember {
        listOf("" to "Off (provider default)") +
            com.example.player.AudioLanguageMatcher.POPULAR_LANGUAGES.map { it.first to it.second }
    }
    val heightOptions = remember {
        listOf(2160 to "4K (2160p)", 1440 to "2K (1440p)", 1080 to "Full HD (1080p)", 720 to "HD (720p)", 480 to "SD (480p)", 360 to "360p", 240 to "240p")
            .map { it.first.toString() to it.second }
    }

    // yt-dlp (ported from mpvRx YtdlpManagerSections): installation status, release channel, update + live log
    val ytdlPrefs = remember { com.example.ytdlp.YtdlPreferences.get(context) }
    val ytdlInfo by com.example.ytdlp.YtdlpManager.installationInfo.collectAsState()
    var ytdlChannel by remember { mutableStateOf(ytdlPrefs.updateChannel.get()) }
    var ytdlBusy by remember { mutableStateOf(false) }
    var ytdlLog by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        if (com.example.BuildConfig.ENABLE_YTDLP) com.example.ytdlp.YtdlpManager.refreshInstallationInfo(context)
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (com.example.BuildConfig.ENABLE_YTDLP) {
            PlayerSettingsCategory("yt-dlp", cardBg, borderColor, secondaryText) {
                val info = ytdlInfo
                val status = when {
                    info == null -> "Checking…"
                    !info.isInstalled -> "Not installed"
                    else -> listOfNotNull(
                        info.version?.let { "Version $it" },
                        info.channel.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                        info.shortCommitHash
                    ).joinToString(" · ")
                }
                PlayerActionRow(
                    title = "Installation",
                    subtitle = status,
                    actionLabel = if (ytdlBusy) "Working…" else if (info?.isInstalled == true) "Update" else "Install",
                    icon = Icons.Outlined.Refresh,
                    enabled = !ytdlBusy,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    borderColor = borderColor
                ) {
                    ytdlBusy = true
                    ytdlLog = ""
                    scope.launch {
                        val onLog: (String) -> Unit = { line ->
                            ytdlLog = (ytdlLog + line).takeLast(1500)
                        }
                        try {
                            val installed = info?.isInstalled == true
                            val ok = when {
                                !installed -> com.example.ytdlp.YtdlpManager.runInstall(context, onLog)
                                ytdlChannel == "nightly" -> com.example.ytdlp.YtdlpManager.runUpdateToNightly(context, onLog)
                                else -> com.example.ytdlp.YtdlpManager.runUpdate(context, onLog)
                            }
                            if (!ok) onLog("Failed. Check your connection and try again.\n")
                            com.example.ytdlp.YtdlpManager.refreshInstallationInfo(context)
                        } finally {
                            ytdlBusy = false
                        }
                    }
                }

                HorizontalDivider(color = borderColor)

                PlayerChoiceRow(
                    title = "Release channel",
                    description = "Stable is the tested release; Nightly gets site fixes first. Applies to the next update.",
                    value = ytdlChannel,
                    options = listOf("stable" to "Stable", "nightly" to "Nightly"),
                    isDark = isDark,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    borderColor = borderColor,
                    alphaFactor = alphaFactor
                ) {
                    ytdlChannel = it
                    ytdlPrefs.updateChannel.set(it)
                }

                if (ytdlBusy || ytdlLog.isNotBlank()) {
                    HorizontalDivider(color = borderColor)
                    if (ytdlBusy) {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentSkyBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = ytdlLog.trim(),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = secondaryText
                    )
                }
            }
        }

        PlayerSettingsCategory("Media settings", cardBg, borderColor, secondaryText) {
            PlayerChoiceRow(
                title = "Language",
                description = "Plays the server, audio and subtitle of this language.",
                value = mediaLanguage,
                options = languageOptions,
                isDark = isDark,
                searchable = true,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) {
                mediaLanguage = it
                com.example.util.NetworkSettings.setMediaLanguage(context, it)
                // a new language must apply again: the server remembered from earlier episodes is forgotten
                com.example.util.media.ServerReputationStore.clearChoices(context)
            }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Auto-select subtitle",
                description = "Select the subtitle of this language too.",
                checked = mediaAutoSubtitle,
                enabled = mediaLanguage.isNotBlank(),
                primaryText = primaryText,
                secondaryText = secondaryText
            ) {
                mediaAutoSubtitle = it
                com.example.util.NetworkSettings.setMediaAutoSubtitle(context, it)
            }

            HorizontalDivider(color = borderColor)

            PlayerChoiceRow(
                title = "Video quality",
                description = "Plays this quality; the closest one if it is missing.",
                value = mediaPlayQuality.toString(),
                options = listOf("0" to "Auto (best for my connection)") + heightOptions,
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) {
                val q = it.toIntOrNull() ?: 0
                mediaPlayQuality = q
                com.example.util.NetworkSettings.setMediaPlayQuality(context, q)
                com.example.util.media.ServerReputationStore.clearChoices(context)
                // the player's own quality memory follows, so HLS streams start at this quality too
                context.getSharedPreferences("viaura_plugin_video", android.content.Context.MODE_PRIVATE)
                    .edit().putString("hls_pref", if (q > 0) q.toString() else "auto").apply()
            }
        }

        PlayerSettingsCategory("Download", cardBg, borderColor, secondaryText) {
            PlayerChoiceRow(
                title = "Download quality",
                description = "Pre-selected in the download sheet.",
                value = mediaDownloadQuality.toString(),
                options = listOf(
                    com.example.util.NetworkSettings.QUALITY_SAME_AS_PLAY.toString() to "Same as video quality",
                    com.example.util.NetworkSettings.QUALITY_AUTO.toString() to "Best available"
                ) + heightOptions,
                isDark = isDark,
                primaryText = primaryText,
                secondaryText = secondaryText,
                borderColor = borderColor,
                alphaFactor = alphaFactor
            ) {
                val q = it.toIntOrNull() ?: com.example.util.NetworkSettings.QUALITY_SAME_AS_PLAY
                mediaDownloadQuality = q
                com.example.util.NetworkSettings.setMediaDownloadQuality(context, q)
                com.example.util.media.ServerReputationStore.clearChoices(context)
            }
        }

        PlayerSettingsCategory("Streaming", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Enable HLS streaming proxy",
                description = "Normalize adaptive HLS manifests and handle split audio/video streams",
                checked = hls,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) {
                hls = it
                com.example.util.NetworkSettings.setHlsHandlingEnabled(context, it)
            }
        }

        PlayerSettingsCategory("Subtitles & Language", cardBg, borderColor, secondaryText) {
            PlayerSwitchRow(
                title = "Download Media Subtitles",
                description = "Automatically extract and load physical subtitle tracks from supported URLs.",
                checked = subs,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) {
                subs = it
                com.example.util.NetworkSettings.setDownloadSubtitles(context, it)
            }

            HorizontalDivider(color = borderColor)

            PlayerSwitchRow(
                title = "Include Auto-Generated Subtitles",
                description = "Fetch auto-caption tracks (e.g. YouTube Speech-to-Text) when regular subs are absent.",
                checked = autoSubs,
                enabled = subs,
                primaryText = primaryText,
                secondaryText = secondaryText
            ) {
                autoSubs = it
                com.example.util.NetworkSettings.setAutoSubtitles(context, it)
            }
        }
    }
}

/** One-line description shown under the page name in the dashboard list. */
internal fun SettingsCategoryTab.subtitle(lang: AppLanguage): String {
    val hi = lang == AppLanguage.HINDI
    return when (this) {
        SettingsCategoryTab.APPEARANCE ->
            if (hi) "थीम, रंग, डिस्प्ले और भाषा" else "Theme, colors, display, language"
        SettingsCategoryTab.LIQUID_GLASS ->
            if (hi) "ऑप्टिक्स, रंग, हाइलाइट" else "Optics, Color, Highlights"
        SettingsCategoryTab.PLAYER ->
            if (hi) "सामान्य, सीकिंग, डिस्प्ले और कंट्रोल, स्क्रीनशॉट" else "General, Seeking, Display & Controls, Screenshots"
        SettingsCategoryTab.AUDIO ->
            if (hi) "ऑडियो भाषा, साउंड, चैनल, वॉल्यूम बूस्ट" else "Languages, sound, audio channels, volume boost"
        SettingsCategoryTab.SUBTITLES ->
            if (hi) "पसंदीदा भाषा, फ़ॉन्ट और स्टाइल" else "Preferred languages, fonts and style"
        SettingsCategoryTab.DECODER ->
            if (hi) "हार्डवेयर डिकोडिंग, रेंडर इंजन" else "Hardware decoding, render engine"
        SettingsCategoryTab.GESTURES ->
            if (hi) "स्वाइप, टैप और संवेदनशीलता" else "Swipe, tap and sensitivity"
        SettingsCategoryTab.PLAYER_LAYOUT ->
            if (hi) "प्लेयर बटन और सीकबार लेआउट" else "Player buttons and seekbar layout"
        SettingsCategoryTab.FOLDERS ->
            if (hi) "फ़ोल्डर, ब्लॉक और प्राइवेट फ़ोल्डर" else "Folders, blocked and private folders"
        SettingsCategoryTab.NETWORK ->
            if (hi) "स्ट्रीमिंग, क्वालिटी, डाउनलोड" else "Streaming, quality, downloads"
        SettingsCategoryTab.ADVANCED ->
            if (hi) "बैकअप, रीस्टोर और एडवांस्ड विकल्प" else "Backup, restore and advanced options"
        SettingsCategoryTab.ABOUT ->
            if (hi) "ऐप, डिवाइस की जानकारी और सपोर्ट" else "App, device info and support"
    }
}

/**
 * The settings icon: does one full 360° turn every time it appears (and when tapped) and carries the same sliding
 * accent-gradient + moving shine as the bottom bar's selected tab.
 */
@Composable
internal fun SettingsShineSpinIcon(
    spinKey: Int,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinKey) {
        rotation.snapTo(0f)
        rotation.animateTo(360f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    val transition = rememberInfiniteTransition(label = "settingsIconShine")
    // Slowly drifting gradient (same motion as the bottom navigation indicator)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settingsIconGradientPhase"
    )
    // A bright band sweeping over the icon
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "settingsIconSweep"
    )
    val gradientColors = AccentGradientColors.let { it + it.first() }

    Icon(
        painter = painterResource(R.drawable.viaura_settings),
        contentDescription = "Settings",
        tint = Color.Black,
        modifier = modifier
            .size(iconSize)
            .graphicsLayer {
                rotationZ = rotation.value
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                // gradient only where the icon is drawn
                drawRect(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(-w * phase, 0f),
                        end = Offset(w * (2f - phase), h)
                    ),
                    blendMode = BlendMode.SrcIn
                )
                // moving shine on top of the icon strokes
                val bandX = -w + sweep * w * 3f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.Transparent),
                        start = Offset(bandX, 0f),
                        end = Offset(bandX + w * 0.9f, h)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
    )
}

// ---------------------------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------------------------

/** Settings whose NAME or DESCRIPTION (or section name) contains every typed word; best matches first. */
private fun searchSettings(query: String, lang: AppLanguage): List<SettingsSearchEntry> {
    val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return emptyList()
    // the pages themselves are searchable too ("gesture" finds the Gestures page)
    val pageEntries = SettingsCategoryTab.entries.map {
        SettingsSearchEntry(it, it.getTitle(lang), it.subtitle(lang))
    }
    return (pageEntries + SettingsSearchIndex)
        .mapNotNull { entry ->
            val title = entry.title.lowercase()
            val desc = entry.description.lowercase()
            val section = entry.category.getTitle(lang).lowercase()
            val haystack = "$title $desc $section"
            if (!tokens.all { haystack.contains(it) }) return@mapNotNull null
            var score = 0
            tokens.forEach { t ->
                score += when {
                    title.startsWith(t) -> 0
                    title.contains(t) -> 1
                    section.contains(t) -> 2
                    else -> 3
                }
            }
            score to entry
        }
        .sortedBy { it.first }
        .map { it.second }
        .distinctBy { it.category to it.title.lowercase() }
        .take(80)
}

private fun highlighted(text: String, tokens: List<String>, color: Color) = buildAnnotatedString {
    if (tokens.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val lower = text.lowercase()
    val marked = BooleanArray(text.length)
    tokens.forEach { t ->
        var from = 0
        while (true) {
            val at = lower.indexOf(t, from)
            if (at < 0) break
            for (i in at until (at + t.length).coerceAtMost(text.length)) marked[i] = true
            from = at + t.length
        }
    }
    var i = 0
    while (i < text.length) {
        val on = marked[i]
        var j = i
        while (j < text.length && marked[j] == on) j++
        if (on) withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(text.substring(i, j)) }
        else append(text.substring(i, j))
        i = j
    }
}

/**
 * Full screen search: type a setting NAME or a word of its DESCRIPTION; a result opens the page the setting lives on.
 */
@Composable
internal fun SettingsSearchOverlay(
    visible: Boolean,
    lang: AppLanguage,
    background: Color,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onClose: () -> Unit,
    onOpenResult: (SettingsCategoryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(160, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(110, easing = FastOutSlowInEasing))
    ) {
        var query by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            delay(160)
            try {
                focusRequester.requestFocus()
                keyboard?.show()
            } catch (_: Throwable) {
            }
        }
        val tokens = remember(query) {
            query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        }
        val results = remember(query, lang) { searchSettings(query, lang) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // not a separate page: the sheet header stays, the search bar + results cover the tabs and the page
                .background(background)
                .pointerInput(Unit) { detectTapGestures { } }
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // slim bar, same look as the Online Videos search bar
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StyledIcon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = AccentSkyBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = if (lang == AppLanguage.HINDI) "नाम या विवरण से खोजें" else "Search by name or description",
                                fontSize = 13.sp,
                                color = secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = com.example.ui.theme.LocalAppFontFamily.current, fontSize = 13.5.sp, color = primaryText),
                            cursorBrush = SolidColor(AccentSkyBlue),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("settings_search_field")
                        )
                    }
                    run {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(primaryText.copy(alpha = 0.08f))
                                .testTag("settings_search_back")
                                .clickable { if (query.isNotEmpty()) query = "" else { keyboard?.hide(); onClose() } },
                            contentAlignment = Alignment.Center
                        ) {
                            StyledIcon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear",
                                tint = primaryText.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            when {
                tokens.isEmpty() -> SettingsSearchMessage(
                    text = if (lang == AppLanguage.HINDI) "किसी भी सेटिंग का नाम या विवरण लिखें" else "Type a setting name or a word from its description",
                    color = secondaryText
                )
                results.isEmpty() -> SettingsSearchMessage(
                    text = if (lang == AppLanguage.HINDI) "कोई सेटिंग नहीं मिली" else "No settings found",
                    color = secondaryText
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(results, key = { _, e -> e.category.name + "|" + e.title }) { index, entry ->
                        SettingsSearchResultRow(
                            entry = entry,
                            lang = lang,
                            tokens = tokens,
                            index = index,
                            cardBg = cardBg,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = {
                                keyboard?.hide()
                                onOpenResult(entry.category)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSearchMessage(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(text = text, fontSize = 15.sp, color = color)
    }
}

@Composable
private fun SettingsSearchResultRow(
    entry: SettingsSearchEntry,
    lang: AppLanguage,
    tokens: List<String>,
    index: Int,
    cardBg: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index * 25L).coerceAtMost(250L))
        progress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 18.dp.toPx()
            }
            .clip(shape)
            .background(cardBg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(AccentSkyBlue.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                drawableRes = entry.category.viauraIconRes,
                contentDescription = null,
                tint = primaryText,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlighted(entry.title, tokens, AccentSkyBlue),
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = highlighted(entry.description, tokens, AccentSkyBlue),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = entry.category.getTitle(lang),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentSkyBlue
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StyledIcon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
* Searchable list of the settings (name + description) per settings section. Generated from the settings screens;
* a search result opens the section the setting lives in.
*/
internal data class SettingsSearchEntry(val category: SettingsCategoryTab, val title: String, val description: String)

internal val SettingsSearchIndex: List<SettingsSearchEntry> = listOf(
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "App Theme", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Interface Appearance", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Display & Feel", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "AMOLED Black Mode", "Pure pitch-black (#000000) background for OLED power saving"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Use System Font", "Use device native typography across the interface"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Haptic Feedback", "Use subtle vibration feedback for key actions and interactions"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Display Zoom & Scale", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Application Scale / Zoom", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Controls Ui Glass Blur & Translucency", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "UI Glass Blur & Opacity", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Controls player buttons, menus, popups & panels", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "File Display & Badges", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Show Full Filename", "Display full video file extension and unaltered filenames in library"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Show 'NEW' Video Badge", "Highlight recently discovered videos with a vibrant NEW badge"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "'NEW' Badge Threshold", "Maximum age (in days) for a video to be marked as NEW"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Show Unplayed Badge on Folders", "Display number of unplayed videos on top of video folders"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Directory Navigation & Layout", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Auto-scroll to Last Played", "Automatically focus and center on the last played video when opening folders"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Path Compression in Tree", "Collapse single-child nested folders into compact combined nodes"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Dual-Pane View on Wide Displays", "Show folder list and media items side-by-side on tablets and foldables"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Watched Progress Threshold", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Watched Status Completion", "Percentage of video length after which it is marked as completed/watched"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Thumbnail Extraction", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Show Video Thumbnails", "Generate visual frame previews for videos in lists and grids"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Extraction Strategy", "Choose how thumbnail artwork is selected from the video"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Thumbnail Quality", "Resolution and cache density for image generation"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Thumbnail Interaction", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Tap Thumbnail to Multi-Select", "Tapping the thumbnail preview directly engages selection mode"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Bottom Bar Tab Visibility", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Videos / Home Tab", "Display primary video library and folders screen"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Audio / Music Tab", "Display dedicated music player and audio files tab"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Recently Played Tab", "Display playback history and quick resume section"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Custom Playlists Tab", "Display custom playlists and categorized series collections"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Network Tab", "Paste a link (YouTube and other supported sites, or a direct video URL) and play it in the player"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Downloads Tab", "Open your downloads directly from the bottom bar"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Transition Styles", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Player Controls Animation", "Style of on-screen controls overlay entrance and exit"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Video Player Enter Transition", "Visual motion when launching video playback from library"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Screen Transition Style", "Visual page transition when navigating between application views"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Tab Switch Animation", "Motion when switching between bottom or category tabs"),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Animation Speed Multiplier", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Global Motion Scale", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Scale duration of all UI animations across the app", ""),
    SettingsSearchEntry(SettingsCategoryTab.APPEARANCE, "Interface Language", ""),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Liquid Glass", ""),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Liquid Glass Sliders & Switches", "Sliders (video seek bar, audio seek bar, settings) and ON / OFF buttons become liquid glass. The filled part and the ON knob use your selected gradient colour"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Highlight Style", ""),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Look of the shiny glass border", ""),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Opacity", "Strength of the blurred picture behind the glass"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Blur", "How frosted the glass looks"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Tint Opacity", "Colour wash on top of the glass"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Refraction Height", "How far from the edge the glass bends the picture"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Refraction Amount", "How strongly the edge bends the picture"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Depth Effect", "Adds depth shading to the bent edge"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Chromatic Aberration", "Slight colour fringe at the glass edge"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Vibrancy", "Boosts the colours seen through the glass"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Saturation", "Colour intensity behind the glass"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Brightness", "Lighter or darker glass"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Contrast", "Difference between light and dark behind the glass"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Highlight Strength", "Brightness of the border light"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Highlight Width", "Thickness of the border"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Highlight Blur", "Softness / glow of the border"),
    SettingsSearchEntry(SettingsCategoryTab.LIQUID_GLASS, "Reset Liquid Glass options", "Restore the default glass look"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Playback Settings", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Fast Seek Step", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Gesture Sensitivity & Scaling", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Player Action Notifications", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Orientation", "Video playback orientation and screen lock behavior"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Save position on quit", "Resume video playback from last known position"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Close after the end of playback", "Automatically exit player when playback finishes"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Autoplay next video", "Play next video in folder or queue automatically"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Repeat playlist after last episode", "Restart the playlist from the first episode after the last episode finishes"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Enable next/previous navigation", "Show previous and next track controls on overlay"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Remember display brightness", "Restore individual screen brightness level for each session"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Auto Picture-in-Picture", "Switch to PiP mode automatically when minimizing player"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Video mini player", "Keep the video playing in a floating mini player (like audio) when you leave the player"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Keep screen on when paused", "Prevent display timeout while playback is paused"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Autoplay after screen unlock", "Resume playback automatically when device is unlocked"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show Media Info in chooser", "Display audio and video technical codecs in track chooser"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show system status bar with controls", "Display battery and time status bar when controls are visible"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show navigation bar with controls", "Display Android navigation buttons when controls are visible"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Safe Area Window", "Pad player margins around notches and curved screen edges"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Reduce player animation", "Minimize motion effects for faster UI performance"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show loading circle", "Display circular progress spinner during video loading"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Swap volume and brightness slider", "Swipe left for volume and right for brightness"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show slider on opposite side", "Swipe on one side and the volume or brightness slider appears on the other side"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show ripple when double tap seeking", "Display animated ripple indicator when double tapping"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show seek time", "Display relative seek target timestamp during swipe gestures"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Show buffered range on seek bar", "Display cached playback buffer range on progress bar"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Use precise seeking", "Seek to exact requested frame rather than nearest keyframe"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Use ThumbFast seek preview", "Display rapid visual thumbnail preview while seek scrubbing"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Custom skip duration", "Duration in seconds to jump when pressing skip buttons"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Use online skip markers", "Query online chapter and intro databases automatically"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Online marker provider", "Preferred service for fetching intro and outro timestamps"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Detect intro/outro from chapter titles", "Auto-identify openings and endings from embedded chapter titles"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Custom keywords for skipping openings", "Comma-separated keywords used to detect intro markers"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Custom keywords for skipping endings", "Comma-separated keywords used to detect ending markers"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Auto-skip intro", "Automatically jump past detected opening chapter segment"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Auto-skip outro", "Automatically jump to next episode when ending segment starts"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Image format", "Image file encoding format for captured frames"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Include subtitles in screenshots", "Burn visible subtitle text directly into captured images"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "Filename template", "Naming template pattern for saved screenshots"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "JPEG/WebP quality", "Compression quality level for lossy captures"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER, "PNG compression", "Compression level from 0 (fastest) to 9 (smallest file)"),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Audio & Sound", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Preferred Audio Languages", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Preferred languages", "Comma-separated codes (e.g. hin,eng,jpn). The first code has the highest priority and falls back sequentially."),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Apply to selected folder/videos only", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Target Folders & Videos", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Playback & Sound Processing", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Enable audio pitch correction", "Preserves natural human vocal pitch when playing at high (0.25x - 4.0x) or slow speeds"),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Volume normalization", "Maintains consistent perceived loudness between different media and audio tracks dynamically"),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Remember selected audio track", "Saves manual audio track changes per video so your selection is restored next time"),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Background Audio", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Background playback", "Continue playing audio when app is minimized or the screen is turned off"),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Audio Channels & Acoustics", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Volume Amplification & Boost", ""),
    SettingsSearchEntry(SettingsCategoryTab.AUDIO, "Volume boost cap", "Maximum software amplification limit when swiping beyond standard volume"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Delete all custom fonts?", ""),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Subtitles & Custom Fonts", ""),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Subtitle Preferences", ""),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Preferred Subtitle Languages", "Subtitle languages ordered by priority (comma-separated)"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Apply to selected folder/videos only", "Restrict auto-selection rule to chosen directories instead of applying globally to all videos"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Target Folders & Videos", ""),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Dual subtitles", "Enable displaying primary and secondary subtitles simultaneously"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Signs & Songs subtitles", "Prioritize signs, songs, and forced narrative subtitle tracks"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Detect subtitles by filename", "Auto-detect external subtitle tracks matching video name"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Automatically load subtitles", "Auto-detect and load external subtitle tracks matching video name"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Override ASS/SSA subtitles", "Apply custom font styling and sizing to ASS/SSA subtitles"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Advanced ASS/SSA", "Edit each style of the current ASS/SSA subtitle individually (Player → Subtitle Style)"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Scale by window", "Scale subtitle font size proportionally with player window"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Show video-embedded fonts", "Show fonts extracted from the currently played video in subtitle font pickers"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Fonts directory", "Choose folder containing .ttf or .otf subtitle fonts"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Import font files", "Select one or more .ttf / .otf files directly"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Loaded fonts library", "Click to collapse font container"),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Loaded Font Repository", ""),
    SettingsSearchEntry(SettingsCategoryTab.SUBTITLES, "Reload fonts", "Rescan font directory and refresh loaded font list"),
    SettingsSearchEntry(SettingsCategoryTab.DECODER, "Decoder & Hardware", ""),
    SettingsSearchEntry(SettingsCategoryTab.DECODER, "Hardware Acceleration", ""),
    SettingsSearchEntry(SettingsCategoryTab.DECODER, "Render Engine", ""),
    SettingsSearchEntry(SettingsCategoryTab.DECODER, "libass Subtitle Renderer", "Pixel-perfect Aegisub styles, transforms & karaoke effects"),
    SettingsSearchEntry(SettingsCategoryTab.DECODER, "Embedded Font Extraction", "Loads custom fonts packed directly in MKV / MP4 containers"),
    SettingsSearchEntry(SettingsCategoryTab.DECODER, "Video Setting", "Live filters, color presets and video adjustments"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Gesture", ""),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Brightness gestures", "Swipe vertically on left side to adjust screen brightness"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Volume gestures", "Swipe vertically on right side to adjust media volume"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Swap volume and brightness slider", "Swipe left for volume and right for brightness"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Pinch to zoom", "Pinch with two fingers to zoom and scale video frame"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Pinch to zoom subtitles", "Pinch subtitle text with two fingers to resize"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Swipe subtitles to seek dialog", "Swipe subtitle area to open seek jump dialog"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Horizontal swipe to seek", "Swipe horizontally anywhere to seek forward or backward"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Swipe up in center for playlist", "Swipe upward from center of player to display playlist"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Horizontal swipe sensitivity", "Maximum seek jump percentage when swiping across the entire screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Hold for multi-x speed", "Long-press and hold screen to accelerate playback speed"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Hold speed multiplier", "Playback rate applied during long-press hold"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Dynamic Speed Overlay", "Display animated speed indicator badge during long-press"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Enable Double Tap", "Master toggle for all double tap gestures and shortcut actions"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Double tap seek duration", "Seconds to seek per double-tap gesture (1s, 5s, 10s, etc.)"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Double Tap Seek Area Width", "Horizontal width percentage of double-tap trigger zones"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Double tap (left)", "Action triggered by double tapping left side of screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Double tap (center)", "Action triggered by double tapping center of screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Double tap (right)", "Action triggered by double tapping right side of screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Enable Single Tap", "Master toggle for single tap zone gestures and shortcut actions"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Single tap seek duration", "Seconds to seek per single-tap gesture (1s, 5s, 10s, etc.)"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Single Tap Seek Area Width", "Horizontal width percentage of single-tap trigger zones"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Single tap (left)", "Action triggered by single tapping left side of screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Single tap (center)", "Action triggered by single tapping center of screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Single tap (right)", "Action triggered by single tapping right side of screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Hold and drag moves subtitles", "Long press on subtitle text and drag to reposition on screen"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Swipe on subtitle jumps between subtitles", "Touch the subtitle area and swipe left for the previous subtitle or right for the next one; swiping elsewhere still seeks the video"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Previous", "Enable gesture or media button for previous item"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Play/Pause", "Enable gesture or media button for toggle play/pause"),
    SettingsSearchEntry(SettingsCategoryTab.GESTURES, "Next", "Enable gesture or media button for next item"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Player Layout", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Control Layout Customizer", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Seekbar Style", ""),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Portrait playback buttons", "Position of play/pause and skip buttons in portrait orientation"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Landscape playback buttons", "Position of play/pause and skip buttons in landscape orientation"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Hide player buttons background", "Hide the background of all player control buttons"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Always use dark player button backgrounds", "Keep player control button backgrounds dark in light, dark, and automatic themes"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Hide player control time", "Inactivity timeout before video controls automatically hide"),
    SettingsSearchEntry(SettingsCategoryTab.PLAYER_LAYOUT, "Time + Network clock", "Format of top header clock and status indicators"),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Language", "Media providers first look for a server of this language. If the video already plays, its audio track and subtitle of this language are selected automatically."),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Auto-select subtitle", "Also select the subtitle track of the chosen language when the video has one."),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Video quality", "Providers play the stream of this quality directly. If it is not offered, the closest quality is used."),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Download quality", "This quality is already selected when the download sheet opens. You can still change it for a single download."),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Installation", ""),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Release channel", "Stable is the tested release; Nightly gets site fixes first. Applies to the next update."),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Enable HLS streaming proxy", "Normalize adaptive HLS manifests and handle split audio/video streams"),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Download Media Subtitles", "Automatically extract and load physical subtitle tracks from supported URLs."),
    SettingsSearchEntry(SettingsCategoryTab.NETWORK, "Include Auto-Generated Subtitles", "Fetch auto-caption tracks (e.g. YouTube Speech-to-Text) when regular subs are absent."),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Block List Selection", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Toggle All", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "No video folders detected", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "No audio folders detected", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Block List", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Select Folders to Block", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Currently Blocked", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Private Vault", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Tap to set up password", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Set Up Private Vault", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Unlock Vault with Password", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Select Folders for Vault", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Hashed with PBKDF2. Required to unlock private files.", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Security Question (for recovery)", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Create Vault", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Unlock Private Vault", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Enter password or answer security question.", ""),
    SettingsSearchEntry(SettingsCategoryTab.FOLDERS, "Unlock Vault", ""),
    SettingsSearchEntry(SettingsCategoryTab.ADVANCED, "Backup & Restore", ""),
    SettingsSearchEntry(SettingsCategoryTab.ADVANCED, "Export Settings", "Export settings to an XML file"),
    SettingsSearchEntry(SettingsCategoryTab.ADVANCED, "Import Settings", "Import settings from an XML file"),
    SettingsSearchEntry(SettingsCategoryTab.ABOUT, "Support & Contribution", "UPI ID"),
    SettingsSearchEntry(SettingsCategoryTab.ABOUT, "Device & System Specifications", ""),
    SettingsSearchEntry(SettingsCategoryTab.ABOUT, "Display Specifications", ""),
    SettingsSearchEntry(SettingsCategoryTab.ABOUT, "Audio Hardware & Codecs", ""),
    SettingsSearchEntry(SettingsCategoryTab.ABOUT, "Media Player Core Engines", ""),
)


/** Drag handle of the settings bottom sheet. Tap or drag down on it to close the sheet. */
@Composable
internal fun SettingsSheetHandle(modifier: Modifier, isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isDark) Color(0x80FFFFFF) else Color(0x99475569))
        )
    }
}

/** Sheet header: settings icon + title on the left, gradient "Done" button on the right. */
@Composable
internal fun SettingsSheetHeader(
    title: String,
    doneLabel: String,
    textColor: Color,
    isDark: Boolean,
    borderColor: Color,
    onSearch: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // same sliding gradient + shine as the app icon; turns once when the dashboard opens and on every tap
        var iconSpinKey by remember { mutableIntStateOf(0) }
        SettingsShineSpinIcon(
            spinKey = iconSpinKey,
            iconSize = 30.dp,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { iconSpinKey++ }
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        // search button (in front of Done): opens the settings search over all tabs
        val searchShape = CircleShape
        Box(
            modifier = Modifier
                .size(30.dp)
                .testTag("settings_search_button")
                .liquidGlassOr(searchShape, isDark, elevation = 0.dp, backdropBlur = false) {
                    clip(searchShape)
                        .background(if (isDark) Color(0x26FFFFFF) else Color(0x14000000))
                        .border(1.dp, borderColor, searchShape)
                }
                .bounceClick(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search settings",
                tint = textColor,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .testTag("settings_done_button")
                .clip(RoundedCornerShape(50))
                .background(AccentGradient)
                .then(if (LocalLiquidGlass.current) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
                .bounceClick(onClick = onDone)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = doneLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

/** Horizontally scrollable tab pills (one per settings section) with a divider underneath. */
@Composable
internal fun SettingsTabRow(
    selected: SettingsCategoryTab,
    lang: AppLanguage,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    onSelect: (SettingsCategoryTab) -> Unit
) {
    val tabs = SettingsCategoryTab.entries.filter { it != SettingsCategoryTab.LIQUID_GLASS }
    val listState = rememberLazyListState()
    // keep the selected pill fully visible
    LaunchedEffect(selected) {
        val index = tabs.indexOf(selected)
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
        val fullyVisible = item != null &&
            item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    val idleBg = if (isDark) Color(0x26FFFFFF) else Color(0x14000000)
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs, key = { it.name }) { tab ->
            val isSelected = tab == selected
            val shape = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .testTag("settings_tab_${tab.name}")
                    .then(
                        if (isSelected) {
                            Modifier
                                .clip(shape)
                                .background(AccentGradient)
                                .then(if (LocalLiquidGlass.current) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
                                .liquidGlassEdge(shape, isDark)
                        } else {
                            // frosted glass pill (Liquid Glass ON); plain tinted pill when OFF
                            Modifier.liquidGlassOr(shape, isDark, elevation = 0.dp, backdropBlur = false) {
                                clip(shape).background(idleBg).border(1.dp, borderColor, shape)
                            }
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(tab) }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledIcon(
                    drawableRes = tab.viauraIconRes,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else secondaryText,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tab.getTitle(lang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else primaryText,
                    maxLines = 1
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.5.dp)
            .background(borderColor)
    )
}


/** Gradient title panel at the top of every settings tab (rounded corners, tab icon in a glass circle). */
@Composable
internal fun SettingsPageTitlePanel(tab: SettingsCategoryTab, lang: AppLanguage, isDark: Boolean) {
    // same corner roundness as the settings search text field: used at the top of every settings tab
    val shape = RoundedCornerShape(18.dp)
    val title = if (tab == SettingsCategoryTab.APPEARANCE) {
        if (lang == AppLanguage.HINDI) "दिखावट और थीम" else "Appearance & Themes"
    } else {
        tab.getTitle(lang)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AccentGradient)
            .then(if (LocalLiquidGlass.current) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
            .liquidGlassEdge(shape, isDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            StyledIcon(
                drawableRes = tab.viauraIconRes,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
