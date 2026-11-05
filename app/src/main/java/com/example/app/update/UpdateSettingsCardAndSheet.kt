package com.example.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.update.AppUpdateInfo
import com.example.update.AppUpdater
import com.example.update.UpdateChannel

// ====================================================================================================
// Settings > About > "Updates" card (auto check switch, channel, "Check for Updates Now")
// ====================================================================================================

@Composable
fun UpdatesSettingsSection(
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val context = LocalContext.current
    remember(context) { AppUpdater.init(context) }
    val accent = if (isDark) AccentSkyBlue else AccentSkyBlueStrong
    val autoCheck = AppUpdater.autoCheck
    val channel = AppUpdater.channel
    val checking = AppUpdater.checking
    val status = AppUpdater.statusMessage
    val available = AppUpdater.available

    Column(modifier = Modifier.fillMaxWidth().testTag("about_updates_section")) {
        // header in the same plain style as the other About section headers (no accent bar)
        Text(
            text = "UPDATES",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryTextColor,
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Check for Updates",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Check on startup", fontSize = 12.5.sp, color = secondaryTextColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ViauraSwitch(
                        checked = autoCheck,
                        onCheckedChange = { AppUpdater.setAutoCheck(context, it) },
                        modifier = Modifier.testTag("update_auto_check_switch")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Update Channel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Preview builds are signed test versions published from the latest GitHub Actions builds and may be unstable.",
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = secondaryTextColor
                )
                Spacer(modifier = Modifier.height(12.dp))

                val segShape = RoundedCornerShape(22.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(segShape)
                        .border(1.dp, borderColor, segShape)
                ) {
                    UpdateChannelSegment(
                        label = "Stable",
                        selected = channel == UpdateChannel.STABLE,
                        accent = accent,
                        textColor = primaryTextColor,
                        modifier = Modifier.weight(1f).testTag("update_channel_stable")
                    ) { AppUpdater.setChannel(context, UpdateChannel.STABLE) }
                    UpdateChannelSegment(
                        label = "Preview (Nightly)",
                        selected = channel == UpdateChannel.PREVIEW,
                        accent = accent,
                        textColor = primaryTextColor,
                        modifier = Modifier.weight(1f).testTag("update_channel_preview")
                    ) { AppUpdater.setChannel(context, UpdateChannel.PREVIEW) }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // "Check for Updates Now"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_check_now_button")
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentGradient)
                        .clickable(enabled = !checking) { AppUpdater.checkNow(context) }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            StyledIcon(
                                drawableRes = R.drawable.viaura_updates,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = if (checking) "Checking for updates..." else "Check for Updates Now",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (available != null && !AppUpdater.sheetVisible) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .clickable { AppUpdater.openSheet() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Update ${available.versionName} is available - tap to view",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                } else if (!status.isNullOrBlank() && !checking) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = status, fontSize = 12.5.sp, color = secondaryTextColor)
                }
            }
        }
    }
}

@Composable
private fun UpdateChannelSegment(
    label: String,
    selected: Boolean,
    accent: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (selected) accent.copy(alpha = 0.20f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                StyledIcon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = label,
                fontSize = 13.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// ====================================================================================================
// "Update Available" bottom sheet (shown by the start-up check and by "Check for Updates Now")
// ====================================================================================================

/** Put once at the root of the app (MainActivity): shows the update sheet whenever [AppUpdater.sheetVisible]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateHost(isDark: Boolean, glassBlurTransparency: Float) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // back from the "Install unknown apps" screen: continue with the install
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) AppUpdater.onAppResumed(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val info = AppUpdater.available
    if (!AppUpdater.sheetVisible || info == null) return

    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val accent = if (isDark) AccentSkyBlue else AccentSkyBlueStrong

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sliderFactor = (glassBlurTransparency / 100f).coerceIn(0.10f, 1.0f)

    // Built exactly like the playlist sheets: ONE glass surface (the sheet container) and everything, including the drag
    // handle, drawn inside it. Liquid Glass ON -> only the glass look; OFF -> only the normal panel.
    ModalBottomSheet(
        onDismissRequest = { AppUpdater.dismissSheet(context) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = sheetShape,
        containerColor = glassPanelColor(isDark, glassBlurTransparency),
        contentColor = primary,
        scrimColor = Color.Black.copy(alpha = 0.35f * sliderFactor),
        dragHandle = null,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) }
    ) {
        EdgeToEdgeSheetWindowEffect()
        LiquidDialogBlur()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (LocalLiquidGlass.current) Modifier.background(liquidGlassSheenBrush(isDark)) else Modifier)
                .liquidGlassEdge(sheetShape, isDark)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(secondary.copy(alpha = 0.4f))
                )
            }
            PanelScaled {
                UpdateSheetContent(
                    info = info,
                    currentVersion = AppUpdater.installedVersionName(context),
                    primary = primary,
                    secondary = secondary,
                    accent = accent,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun UpdateSheetContent(
    info: AppUpdateInfo,
    currentVersion: String,
    primary: Color,
    secondary: Color,
    accent: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val downloading = AppUpdater.downloading
    val progress = AppUpdater.downloadProgress
    val error = AppUpdater.errorMessage
    val downloaded = AppUpdater.downloadedFile != null

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("update_sheet")
    ) {
        // title row: badge + "Update Available" + version
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                StyledIcon(
                    drawableRes = R.drawable.viaura_updates,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Update Available", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version ${info.versionName}" + if (info.isPreview) "  -  Preview" else "",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // current -> new
        Row(verticalAlignment = Alignment.CenterVertically) {
            VersionPill(text = currentVersion, bg = primary.copy(alpha = 0.10f), color = secondary)
            Text(text = "  →  ", fontSize = 16.sp, color = secondary)
            VersionPill(text = info.versionName, bg = accent.copy(alpha = 0.22f), color = primary)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = "Release Date", fontSize = 12.sp, color = secondary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = info.releaseDate, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Size", fontSize = 12.sp, color = secondary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = formatSize(info.sizeBytes), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = primary.copy(alpha = 0.14f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "What's New", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = secondary)
        Spacer(modifier = Modifier.height(6.dp))

        // release notes written on GitHub (Markdown) - scrolls inside the sheet
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            ReleaseNotesMarkdown(
                text = info.notes.ifBlank { "No release notes were provided for this version." },
                primary = primary,
                secondary = secondary,
                accent = accent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (downloading) {
            if (progress >= 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = accent,
                    trackColor = primary.copy(alpha = 0.12f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = accent,
                    trackColor = primary.copy(alpha = 0.12f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (progress >= 0f) "Downloading... ${(progress * 100).toInt()}%" else "Downloading...",
                fontSize = 12.5.sp,
                color = secondary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val laterShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("update_later_button")
                    .clip(laterShape)
                    .border(1.dp, primary.copy(alpha = 0.25f), laterShape)
                    .clickable { AppUpdater.dismissSheet(context) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (downloading) "Hide" else "Later",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.6f)
                    .testTag("update_install_button")
                    .clip(RoundedCornerShape(16.dp))
                    .background(AccentGradient)
                    .clickable(enabled = !downloading) { AppUpdater.downloadAndInstall(context) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        downloading -> "Downloading..."
                        downloaded -> "Install"
                        else -> "Download & Install"
                    },
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun VersionPill(text: String, bg: Color, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "-"
    bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
