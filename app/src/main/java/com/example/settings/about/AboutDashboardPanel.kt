package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.state.AppLanguage
import com.example.ui.state.UiState
import com.example.ui.theme.AccentGradient
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Dedicated About Settings Screen Composable.
 * 100% crash-proof, accurate hardware inspection, functional Telegram, GitHub & UPI support.
 */
@Composable
fun AboutSettingsPanel(
    uiState: UiState,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    alphaFactor: Float,
    lang: AppLanguage
) {
    val context = LocalContext.current

    // Dynamically detected information (memoized safely)
    val displayInfo = remember(context) { detectDisplayInfo(context) }
    val audioInfo = remember(context) { detectAudioInfo(context) }
    val hwInfo = remember(context) { detectHardwareInfo(context) }

    // App Info
    val appName = stringResource(id = R.string.app_name)
    val appVersion = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Throwable) {
            "1.0.0"
        }
    }

    val primaryTextColor = if (isDark) Neutral.F1F5F9 else Neutral.D0F172A
    val secondaryTextColor = if (isDark) Neutral.S94A3B8 else Neutral.S64748B
    val activeGradient = AccentGradient

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==========================================
        // 1. APP HEADER: SIDE-BY-SIDE ICON + NAME + VERSION + CREATOR
        // ==========================================
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon on the left
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Color(0x40000000))
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xFF0B1120) else Neutral.D1E293B)
                    .border(1.2.dp, borderColor, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_abouticon),
                    contentDescription = "$appName App Icon",
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // App Name, Version & Creator Pill Badge on the right
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = appName,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "v$appVersion release",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(7.dp))

                // "By GOD KARTiK" Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0x28FFFFFF) else Color(0x18000000))
                        .border(
                            1.dp,
                            if (isDark) Color(0x38FFFFFF) else Color(0x18000000),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "By GOD KARTiK",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==========================================
        // 2. SOCIAL BUTTONS: TELEGRAM & GITHUB
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Telegram Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("about_telegram_button")
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) AccentSkyBlue.copy(alpha = 0.13f) else AccentSkyBlueStrong.copy(alpha = 0.08f))
                    .border(1.dp, if (isDark) AccentSkyBlue.copy(alpha = 0.31f) else AccentSkyBlueStrong.copy(alpha = 0.19f), RoundedCornerShape(18.dp))
                    .clickable { openTelegramApp(context) }
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.viaura_telegram),
                        contentDescription = "Telegram",
                        tint = if (isDark) AccentSkyBlue else AccentSkyBlueStrong,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Telegram",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) AccentSkyBlue else AccentSkyBlueStrong
                    )
                }
            }

            // GitHub Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("about_github_button")
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                    .clickable {
                        try {
                            val gitIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/karrtik-xe"))
                            gitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(gitIntent)
                        } catch (_: Throwable) {
                            Toast.makeText(context, "Opening https://github.com/karrtik-xe", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.viaura_github),
                        contentDescription = "GitHub",
                        tint = primaryTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GitHub",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Report a bug: opens the GitHub Issues page of the project
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about_report_bug_button")
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .clickable { openWebLink(context, BUG_REPORT_URL) }
                .padding(vertical = 12.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.viaura_bug),
                    contentDescription = "Report a Bug",
                    tint = primaryTextColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Report a Bug",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ==========================================
        // 3. SUPPORT / DONATION SECTION (UPI)
        // ==========================================
        AlignLeftHeader(title = "SUPPORT & CONTRIBUTION", color = secondaryTextColor)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) AccentSkyBlue.copy(alpha = 0.19f) else AccentSkyBlueStrong.copy(alpha = 0.09f)),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            drawableRes = com.example.R.drawable.viaura_upi,
                            contentDescription = null,
                            tint = if (isDark) AccentSkyBlue else AccentSkyBlueStrong,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPI ID",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryTextColor
                        )
                        Text(
                            text = "kartiik.gpay@okicici",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                    }

                    // Copy UPI Button
                    Box(
                        modifier = Modifier
                            .testTag("copy_upi_button")
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UPI ID", "kartiik.gpay@okicici")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "UPI ID copied: kartiik.gpay@okicici", Toast.LENGTH_SHORT).show()
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "Copied kartiik.gpay@okicici", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StyledIcon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy UPI ID",
                                tint = primaryTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Copy",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pay / Support Button (Universal UPI Intent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pay_upi_button")
                        .clip(RoundedCornerShape(16.dp))
                        .background(activeGradient)
                        .clickable {
                            try {
                                val upiUri = Uri.parse("upi://pay?pa=kartiik.gpay@okicici&pn=GOD%20KARTiK&cu=INR&tn=Support%20Player")
                                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                upiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                val chooser = Intent.createChooser(upiIntent, "Pay / Support via UPI (Google Pay, PhonePe, Paytm, BHIM)")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            } catch (_: Throwable) {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("UPI ID", "kartiik.gpay@okicici")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "UPI ID copied: kartiik.gpay@okicici. Open your UPI app to pay.", Toast.LENGTH_LONG).show()
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "UPI: kartiik.gpay@okicici", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StyledIcon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pay / Support via UPI",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ==========================================
        // 3b. UPDATES (below the Support card) (GitHub releases: auto check, Stable / Preview channel, check now)
        // ==========================================
        if (com.example.BuildConfig.ENABLE_UPDATER) {
            UpdatesSettingsSection(
                isDark = isDark,
                cardBg = cardBg,
                borderColor = borderColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )

            Spacer(modifier = Modifier.height(22.dp))
        }

        // ==========================================
        // 4. DEVICE & HARDWARE SPECIFICATIONS
        // ==========================================
        AlignLeftHeader(title = "DEVICE & SYSTEM SPECIFICATIONS", color = secondaryTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutInfoRow("Device Model", hwInfo.deviceName, primaryTextColor, secondaryTextColor)
                HorizontalDivider(color = borderColor)
                AboutInfoRow("Android Version", hwInfo.androidVersion, primaryTextColor, secondaryTextColor)
                hwInfo.securityPatch?.let {
                    HorizontalDivider(color = borderColor)
                    AboutInfoRow("Security Patch", it, primaryTextColor, secondaryTextColor)
                }
                HorizontalDivider(color = borderColor)
                AboutInfoRow("SoC / Processor", hwInfo.processor, primaryTextColor, secondaryTextColor)
                HorizontalDivider(color = borderColor)
                AboutInfoRow("CPU Architecture", hwInfo.cpuArch, primaryTextColor, secondaryTextColor)
                HorizontalDivider(color = borderColor)
                AboutInfoRow("CPU Cores", hwInfo.cpuCores, primaryTextColor, secondaryTextColor)
                if (hwInfo.ramInfo != null) {
                    HorizontalDivider(color = borderColor)
                    AboutInfoRow("RAM Memory", hwInfo.ramInfo, primaryTextColor, secondaryTextColor)
                }
                HorizontalDivider(color = borderColor)
                AboutInfoRow("Build ID", hwInfo.securityOrBuild, primaryTextColor, secondaryTextColor)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ==========================================
        // 5. DISPLAY SPECIFICATIONS
        // ==========================================
        AlignLeftHeader(title = "DISPLAY SPECIFICATIONS", color = secondaryTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                displayInfo.screenSize?.let {
                    AboutInfoRow("Screen Size", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                displayInfo.resolution?.let {
                    AboutInfoRow("Physical Resolution", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                displayInfo.density?.let {
                    AboutInfoRow("Pixel Density", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                displayInfo.aspectRatio?.let {
                    AboutInfoRow("Aspect Ratio", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                displayInfo.refreshRate?.let {
                    AboutInfoRow("Refresh Rate", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                displayInfo.hdrFormats?.let {
                    AboutInfoRow("HDR Capabilities", it, primaryTextColor, secondaryTextColor)
                }
                displayInfo.wideColorGamut?.let {
                    HorizontalDivider(color = borderColor)
                    AboutInfoRow("Color Gamut", it, primaryTextColor, secondaryTextColor)
                }
                displayInfo.peakBrightness?.let {
                    HorizontalDivider(color = borderColor)
                    AboutInfoRow("Peak Luminance", it, primaryTextColor, secondaryTextColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ==========================================
        // 6. AUDIO HARDWARE & CODECS
        // ==========================================
        AlignLeftHeader(title = "AUDIO HARDWARE & CODECS", color = secondaryTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                audioInfo.speakerConfig?.let {
                    AboutInfoRow("Speaker Output", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                audioInfo.dolbyAtmos?.let {
                    AboutInfoRow("Dolby Atmos", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                audioInfo.dolbyAudio?.let {
                    AboutInfoRow("Dolby Digital (AC-3/E-AC-3)", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                audioInfo.dts?.let {
                    AboutInfoRow("DTS-HD / DTS Audio", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                audioInfo.hiResAudio?.let {
                    AboutInfoRow("Hi-Res Audio Decoding", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                audioInfo.spatialAudio?.let {
                    AboutInfoRow("Spatial Audio Engine", it, primaryTextColor, secondaryTextColor)
                    HorizontalDivider(color = borderColor)
                }
                audioInfo.bluetoothCodecs?.let {
                    AboutInfoRow("Bluetooth Codecs", it, primaryTextColor, secondaryTextColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // ==========================================
        // 7. MEDIA ENGINE & CORE LIBRARIES
        // ==========================================
        AlignLeftHeader(title = "MEDIA PLAYER CORE ENGINES", color = secondaryTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutInfoRow("MPV Engine", "0.41.0-dev (Custom Build)", primaryTextColor, secondaryTextColor)
                HorizontalDivider(color = borderColor)
                AboutInfoRow("FFmpeg Library", "v9.0 (Hardware Codecs)", primaryTextColor, secondaryTextColor)
                HorizontalDivider(color = borderColor)
                AboutInfoRow("libplacebo", "v7.371.0 (Vulkan / OpenGL)", primaryTextColor, secondaryTextColor)
                HorizontalDivider(color = borderColor)
                AboutInfoRow("libass", "v0.17.3 (Styled SSA/ASS Subtitles)", primaryTextColor, secondaryTextColor)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

private const val TELEGRAM_USERNAME = "god_kartik_viaura"
private const val TELEGRAM_LINK = "https://t.me/god_kartik_viaura"
private const val BUG_REPORT_URL = "https://github.com/karrtik-xe/Viaura/issues"

private fun startViewIntent(context: Context, intent: Intent): Boolean {
    return try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
    }
}

/** Opens a web link in the default browser / the matching app (GitHub app when installed). */
private fun openWebLink(context: Context, url: String) {
    if (!startViewIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
        Toast.makeText(context, url, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens the Telegram community directly inside the Telegram app (tg:// deep link, then the link pinned to each known Telegram
 * client). The browser is only the last fallback, when no Telegram app is installed at all.
 */
private fun openTelegramApp(context: Context) {
    if (startViewIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$TELEGRAM_USERNAME")))) return
    val telegramClients = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "org.telegram.plus"
    )
    for (pkg in telegramClients) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_LINK)).setPackage(pkg)
        if (startViewIntent(context, intent)) return
    }
    openWebLink(context, TELEGRAM_LINK)
}

@Composable
private fun AlignLeftHeader(title: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String,
    primaryColor: Color,
    secondaryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryColor,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
