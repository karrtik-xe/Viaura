package com.example.ui.components

import android.content.Context
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.ui.i18n.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.state.AdvancedEngineSettings
import com.example.ui.state.AppLanguage
import com.example.ui.state.UiState
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun tr(lang: AppLanguage, en: String, hinglish: String, hindi: String): String = when (lang) {
    AppLanguage.ENGLISH -> en
    AppLanguage.HINGLISH -> hinglish
    AppLanguage.HINDI -> hindi
}

private fun megabytes(bytes: Long): String = if (bytes < 1024 * 1024) "${bytes / 1024} KB" else "%.1f MB".format(bytes / 1048576.0)

/**
 * Settings Dashboard > Advanced: auto backup, mpv configuration (mpv.conf, input.conf), scripts, data & cache, logging.
 * Every option here is wired to the real engine: see [AdvancedEngineSettings].
 */
@Composable
fun AdvancedEngineSections(
    uiState: UiState,
    isDark: Boolean,
    cardBg: Color,
    borderColor: Color,
    lang: AppLanguage
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary

    var autoBackup by remember { mutableStateOf(AdvancedEngineSettings.autoBackupEnabled(context)) }
    var backupDir by remember { mutableStateOf(AdvancedEngineSettings.autoBackupDir(context)) }
    var lastBackup by remember { mutableStateOf(AdvancedEngineSettings.lastAutoBackupTime(context)) }
    var showBackupPicker by remember { mutableStateOf(false) }

    var confPriority by remember { mutableStateOf(AdvancedEngineSettings.confPriority(context)) }
    var editor by remember { mutableStateOf<String?>(null) } // "mpv", "input" or "script:<name>"
    var luaEnabled by remember { mutableStateOf(AdvancedEngineSettings.luaEnabled(context)) }
    var showScripts by remember { mutableStateOf(false) }

    var recent by remember { mutableStateOf(AdvancedEngineSettings.recentlyPlayedEnabled(context)) }
    var confirmClearHistory by remember { mutableStateOf(false) }

    var verbose by remember { mutableStateOf(AdvancedEngineSettings.verboseLogging(context)) }
    var logMb by remember { mutableStateOf(AdvancedEngineSettings.logSizeMb(context).toFloat()) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    val nextVideo = tr(lang, "Takes effect from the next video you open.", "Agli video khologe tab se lagu hoga.", "अगला वीडियो खोलने पर लागू होगा।")

    Column(modifier = Modifier.fillMaxWidth()) {
        // ---------------- AUTO BACKUP ----------------
        Spacer(Modifier.height(20.dp))
        EngineSectionHeader(tr(lang, "AUTOMATIC BACKUP", "AUTOMATIC BACKUP", "स्वचालित बैकअप"), secondaryText)
        EngineCard(cardBg, borderColor) {
            EngineSwitchRow(
                icon = Icons.Outlined.Backup,
                title = tr(lang, "Auto backup", "Auto backup", "ऑटो बैकअप"),
                subtitle = when {
                    backupDir.isBlank() -> tr(lang, "Choose a backup folder first", "Pehle backup folder chuno", "पहले बैकअप फ़ोल्डर चुनें")
                    lastBackup > 0L -> tr(lang, "Last backup: ", "Last backup: ", "आख़िरी बैकअप: ") +
                        android.text.format.DateUtils.getRelativeTimeSpanString(lastBackup)
                    else -> tr(lang, "Settings are saved to the folder after every change", "Har change ke baad settings folder mein save hoti hain", "हर बदलाव के बाद सेटिंग्स फ़ोल्डर में सुरक्षित होती हैं")
                },
                checked = autoBackup,
                enabled = backupDir.isNotBlank(),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onChange = { on ->
                    autoBackup = on
                    AdvancedEngineSettings.setAutoBackupEnabled(context, on)
                    if (on) scope.launch {
                        val r = withContext(Dispatchers.IO) { AdvancedEngineSettings.runAutoBackup(context, uiState) }
                        lastBackup = AdvancedEngineSettings.lastAutoBackupTime(context)
                        r.onFailure { toast(it.localizedMessage ?: "Backup failed") }
                    }
                }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.FolderOpen,
                title = tr(lang, "Backup folder", "Backup folder", "बैकअप फ़ोल्डर"),
                subtitle = backupDir.ifBlank { tr(lang, "Not chosen", "Chuna nahi hai", "चुना नहीं गया") },
                actionLabel = tr(lang, "Choose", "Chuno", "चुनें"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = { showBackupPicker = true }
            )
            if (backupDir.isNotBlank()) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                EngineActionRow(
                    icon = Icons.Outlined.Delete,
                    title = tr(lang, "Clear backup folder", "Backup folder hatao", "बैकअप फ़ोल्डर हटाएँ"),
                    subtitle = tr(lang, "Auto backup stops until a folder is chosen again", "Folder dobara chunne tak auto backup band", "फ़ोल्डर दोबारा चुनने तक ऑटो बैकअप बंद"),
                    actionLabel = tr(lang, "Clear", "Hatao", "हटाएँ"),
                    isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                    onClick = {
                        AdvancedEngineSettings.setAutoBackupDir(context, "")
                        AdvancedEngineSettings.setAutoBackupEnabled(context, false)
                        backupDir = ""; autoBackup = false
                    }
                )
            }
        }

        // ---------------- MPV CONFIGURATION ----------------
        Spacer(Modifier.height(20.dp))
        EngineSectionHeader(tr(lang, "MPV CONFIGURATION", "MPV CONFIGURATION", "MPV कॉन्फ़िगरेशन"), secondaryText)
        EngineCard(cardBg, borderColor) {
            EngineActionRow(
                icon = Icons.Outlined.Description,
                title = "mpv.conf",
                subtitle = tr(lang, "Edit the mpv configuration file", "mpv configuration file edit karo", "mpv कॉन्फ़िगरेशन फ़ाइल संपादित करें"),
                actionLabel = tr(lang, "Edit", "Edit", "संपादित"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = { editor = "mpv" }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineSwitchRow(
                icon = Icons.Outlined.Tune,
                title = tr(lang, "mpv.conf has priority", "mpv.conf ko priority", "mpv.conf को प्राथमिकता"),
                subtitle = tr(lang, "Its options win over the app's own start-up options", "Iske options app ke apne start-up options se upar chalte hain", "इसके विकल्प ऐप के अपने शुरुआती विकल्पों पर भारी पड़ते हैं"),
                checked = confPriority,
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onChange = { confPriority = it; AdvancedEngineSettings.setConfPriority(context, it); toast(nextVideo) }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.Keyboard,
                title = "input.conf",
                subtitle = tr(lang, "Key bindings (keyboard / remote)", "Key bindings (keyboard / remote)", "की बाइंडिंग (कीबोर्ड / रिमोट)"),
                actionLabel = tr(lang, "Edit", "Edit", "संपादित"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = { editor = "input" }
            )
        }

        // ---------------- SCRIPTS ----------------
        Spacer(Modifier.height(20.dp))
        EngineSectionHeader(tr(lang, "SCRIPTS", "SCRIPTS", "स्क्रिप्ट्स"), secondaryText)
        EngineCard(cardBg, borderColor) {
            EngineSwitchRow(
                icon = Icons.Outlined.Code,
                title = tr(lang, "Enable Lua / JS scripts", "Lua / JS scripts chalu karo", "Lua / JS स्क्रिप्ट्स चालू करें"),
                subtitle = tr(lang, "Ticked scripts are loaded by mpv", "Tick kiye hue scripts mpv load karta hai", "टिक की गई स्क्रिप्ट्स mpv लोड करता है"),
                checked = luaEnabled,
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onChange = {
                    luaEnabled = it
                    AdvancedEngineSettings.setLuaEnabled(context, it)
                    AdvancedEngineSettings.syncScripts(context)
                    toast(nextVideo)
                }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.Extension,
                title = tr(lang, "Manage scripts", "Scripts manage karo", "स्क्रिप्ट्स प्रबंधित करें"),
                subtitle = tr(lang, "Add, select, edit or delete scripts", "Scripts add, select, edit ya delete karo", "स्क्रिप्ट्स जोड़ें, चुनें, संपादित करें या हटाएँ"),
                actionLabel = tr(lang, "Open", "Kholo", "खोलें"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = { showScripts = true }
            )
        }

        // ---------------- DATA & CACHE ----------------
        Spacer(Modifier.height(20.dp))
        EngineSectionHeader(tr(lang, "DATA & CACHE", "DATA & CACHE", "डेटा और कैश"), secondaryText)
        EngineCard(cardBg, borderColor) {
            EngineSwitchRow(
                icon = Icons.Outlined.History,
                title = tr(lang, "Recently played", "Recently played", "हाल ही में चलाए गए"),
                subtitle = tr(lang, "When OFF no history and no resume position is saved", "OFF ho to history aur resume position save nahi hoti", "बंद होने पर हिस्ट्री और रिज़्यूम पोज़िशन सहेजी नहीं जाती"),
                checked = recent,
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onChange = { recent = it; AdvancedEngineSettings.setRecentlyPlayedEnabled(context, it) }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.DeleteSweep,
                title = tr(lang, "Clear playback history", "Playback history clear karo", "प्लेबैक हिस्ट्री साफ़ करें"),
                subtitle = tr(lang, "Removes recents and all resume positions", "Recents aur saari resume positions hat jaati hain", "रीसेंट्स और सभी रिज़्यूम पोज़िशन हट जाती हैं"),
                actionLabel = tr(lang, "Clear", "Clear", "साफ़ करें"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = { confirmClearHistory = true }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.Image,
                title = tr(lang, "Clear thumbnail cache", "Thumbnail cache clear karo", "थंबनेल कैश साफ़ करें"),
                subtitle = tr(lang, "Thumbnails are made again when needed", "Thumbnails zarurat par dobara ban jaate hain", "थंबनेल ज़रूरत पड़ने पर फिर बन जाते हैं"),
                actionLabel = tr(lang, "Clear", "Clear", "साफ़ करें"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = {
                    scope.launch {
                        val freed = withContext(Dispatchers.IO) { AdvancedEngineSettings.clearThumbnailCache(context) }
                        toast(tr(lang, "Thumbnail cache cleared (${megabytes(freed)})", "Thumbnail cache clear ho gaya (${megabytes(freed)})", "थंबनेल कैश साफ़ हुआ (${megabytes(freed)})"))
                    }
                }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.TextFields,
                title = tr(lang, "Clear fonts cache", "Fonts cache clear karo", "फ़ॉन्ट कैश साफ़ करें"),
                subtitle = tr(lang, "Your own fonts are kept; the font index is rebuilt", "Aapke apne fonts bache rehte hain; font index dobara banta hai", "आपके अपने फ़ॉन्ट्स बने रहते हैं; फ़ॉन्ट इंडेक्स दोबारा बनता है"),
                actionLabel = tr(lang, "Clear", "Clear", "साफ़ करें"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = {
                    scope.launch {
                        val freed = withContext(Dispatchers.IO) { AdvancedEngineSettings.clearFontsCache(context) }
                        toast(tr(lang, "Fonts cache cleared (${megabytes(freed)})", "Fonts cache clear ho gaya (${megabytes(freed)})", "फ़ॉन्ट कैश साफ़ हुआ (${megabytes(freed)})"))
                    }
                }
            )
        }

        // ---------------- LOGGING ----------------
        Spacer(Modifier.height(20.dp))
        EngineSectionHeader(tr(lang, "LOGGING", "LOGGING", "लॉगिंग"), secondaryText)
        EngineCard(cardBg, borderColor) {
            EngineSwitchRow(
                icon = Icons.Outlined.BugReport,
                title = tr(lang, "Verbose logging", "Verbose logging", "विस्तृत लॉगिंग"),
                subtitle = tr(lang, "mpv writes much more detail into the log", "mpv log mein bahut zyada detail likhta hai", "mpv लॉग में बहुत अधिक विवरण लिखता है"),
                checked = verbose,
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onChange = { verbose = it; AdvancedEngineSettings.setVerboseLogging(context, it); toast(nextVideo) }
            )
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    text = tr(lang, "Debug log size: ", "Debug log size: ", "डिबग लॉग आकार: ") + "${logMb.toInt()} MB",
                    fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = primaryText
                )
                ViauraSlider(
                    value = logMb,
                    onValueChange = { logMb = it },
                    valueRange = 1f..20f,
                    steps = 18,
                    onValueChangeFinished = { AdvancedEngineSettings.setLogSizeMb(context, logMb.toInt()) }
                )
            }
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            EngineActionRow(
                icon = Icons.Outlined.FileDownload,
                title = tr(lang, "Dump logs", "Logs save karo", "लॉग सहेजें"),
                subtitle = tr(lang, "Saves the latest log to Downloads/Viaura", "Latest log Downloads/Viaura mein save hota hai", "ताज़ा लॉग Downloads/Viaura में सहेजा जाता है"),
                actionLabel = tr(lang, "Save", "Save", "सहेजें"),
                isDark = isDark, primaryText = primaryText, secondaryText = secondaryText,
                onClick = {
                    scope.launch {
                        val name = withContext(Dispatchers.IO) { AdvancedEngineSettings.dumpLogs(context) }
                        toast(
                            if (name != null) tr(lang, "Saved: Downloads/Viaura/$name", "Save hua: Downloads/Viaura/$name", "सहेजा गया: Downloads/Viaura/$name")
                            else tr(lang, "Could not save the log", "Log save nahi ho paya", "लॉग सहेजा नहीं जा सका")
                        )
                    }
                }
            )
        }
    }

    // ---------------- dialogs ----------------
    if (showBackupPicker) {
        FloatingDirectoryPickerSheet(
            action = DirectoryPickerAction.EXPORT_SETTINGS,
            itemCount = 1,
            itemDescription = "Settings XML",
            initialPath = backupDir.ifBlank { null },
            isDark = isDark,
            glassBlurTransparency = uiState.glassBlurTransparency,
            customTitle = tr(lang, "Select backup folder", "Backup folder chuno", "बैकअप फ़ोल्डर चुनें"),
            customActionLabel = tr(lang, "Use this folder", "Yeh folder use karo", "यही फ़ोल्डर इस्तेमाल करें"),
            customActionIcon = Icons.Outlined.FolderOpen,
            onConfirmTargetDir = { dir ->
                showBackupPicker = false
                AdvancedEngineSettings.setAutoBackupDir(context, dir.absolutePath)
                backupDir = dir.absolutePath
                if (autoBackup) scope.launch {
                    withContext(Dispatchers.IO) { AdvancedEngineSettings.runAutoBackup(context, uiState) }
                    lastBackup = AdvancedEngineSettings.lastAutoBackupTime(context)
                }
            },
            onDismiss = { showBackupPicker = false }
        )
    }

    editor?.let { which ->
        val file: File
        val title: String
        val hint: String
        if (which == "mpv") {
            file = AdvancedEngineSettings.mpvConfFile(context); title = "mpv.conf"
            hint = tr(lang, "One option per line, e.g. hwdec=mediacodec. Lines starting with # are comments.", "Har line mein ek option, jaise hwdec=mediacodec. # wali line comment hai.", "हर पंक्ति में एक विकल्प, जैसे hwdec=mediacodec। # वाली पंक्ति टिप्पणी है।")
        } else if (which == "input") {
            file = AdvancedEngineSettings.inputConfFile(context); title = "input.conf"
            hint = tr(lang, "One binding per line, e.g. f cycle fullscreen", "Har line mein ek binding, jaise f cycle fullscreen", "हर पंक्ति में एक बाइंडिंग, जैसे f cycle fullscreen")
        } else {
            val name = which.removePrefix("script:")
            file = File(AdvancedEngineSettings.scriptsLibraryDir(context), name); title = name
            hint = tr(lang, "Lua / JavaScript source of the script", "Script ka Lua / JavaScript code", "स्क्रिप्ट का Lua / JavaScript कोड")
        }
        ConfigEditorDialog(
            title = title, hint = hint, initial = AdvancedEngineSettings.readText(file), isDark = isDark, lang = lang,
            onSave = { text ->
                val ok = if (which.startsWith("script:")) try { file.writeText(text); true } catch (_: Throwable) { false }
                else AdvancedEngineSettings.writeText(file, text)
                toast(if (ok) nextVideo else tr(lang, "Could not save", "Save nahi ho paya", "सहेजा नहीं जा सका"))
                editor = null
                if (which.startsWith("script:")) { showScripts = true } else if (which == "mpv" || which == "input") { /* mpv reads the files at start */ }
            },
            onDismiss = { editor = null; if (which.startsWith("script:")) showScripts = true }
        )
    }

    if (showScripts && editor == null) {
        ScriptsManagerDialog(
            isDark = isDark, lang = lang, context = context,
            onEdit = { name -> showScripts = false; editor = "script:$name" },
            onChanged = { AdvancedEngineSettings.syncScripts(context) },
            onDismiss = { showScripts = false }
        )
    }

    if (confirmClearHistory) {
        ConfirmGlassDialog(
            title = tr(lang, "Clear playback history?", "Playback history clear karein?", "प्लेबैक हिस्ट्री साफ़ करें?"),
            message = tr(lang, "Recently played and all resume positions will be removed.", "Recently played aur saari resume positions hat jayengi.", "रीसेंटली प्लेड और सभी रिज़्यूम पोज़िशन हट जाएँगी।"),
            confirmLabel = tr(lang, "Clear", "Clear", "साफ़ करें"),
            cancelLabel = tr(lang, "Cancel", "Cancel", "रद्द करें"),
            isDark = isDark,
            onConfirm = {
                confirmClearHistory = false
                AdvancedEngineSettings.clearPlaybackHistory(context)
                toast(tr(lang, "Playback history cleared", "Playback history clear ho gayi", "प्लेबैक हिस्ट्री साफ़ हुई"))
            },
            onDismiss = { confirmClearHistory = false }
        )
    }
}

// ============================================================================================================
// dialogs
// ============================================================================================================

/**
 * The surface of every popup of this page: exactly the background of the Settings dashboard behind it.
 * Liquid Glass ON  = the same frosted liquid-glass panel; OFF = the normal panel colour of the dashboard.
 */
@Composable
private fun EngineGlassSurface(isDark: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    val transparency = LocalPanelTransparency.current
    val border = if (isDark) DarkGlassBorder else LightGlassBorder
    Box(
        modifier = modifier.liquidGlassOr(shape, isDark, transparency, elevation = 24.dp) {
            this
                .shadow(
                    elevation = 18.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.30f)
                )
                .clip(shape)
                .background(glassPanelColor(isDark, transparency))
                .border(1.2.dp, border, shape)
        },
        content = content
    )
}

@Composable
private fun EngineDialogButton(label: String, filled: Boolean, secondary: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(if (filled) Modifier.background(AccentGradient, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = if (filled) Color.White else secondary)
    }
}

@Composable
private fun ConfigEditorDialog(
    title: String, hint: String, initial: String, isDark: Boolean, lang: AppLanguage,
    onSave: (String) -> Unit, onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val border = if (isDark) DarkGlassBorder else LightGlassBorder
    LiquidGlassDialog(isDark = isDark, onDismissRequest = onDismiss) {
        EngineGlassSurface(isDark, Modifier.fillMaxWidth().widthIn(max = 460.dp).fillMaxHeight(0.84f)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primary)
                Spacer(Modifier.height(4.dp))
                Text(hint, fontSize = 11.5.sp, color = secondary, lineHeight = 16.sp)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = primary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentSkyBlue,
                        unfocusedBorderColor = border,
                        focusedContainerColor = Color.Black.copy(alpha = if (isDark) 0.20f else 0.04f),
                        unfocusedContainerColor = Color.Black.copy(alpha = if (isDark) 0.14f else 0.03f),
                        cursorColor = AccentSkyBlue
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EngineDialogButton(tr(lang, "Clear", "Clear", "साफ़ करें"), false, secondary) { text = "" }
                    Spacer(Modifier.weight(1f))
                    EngineDialogButton(tr(lang, "Cancel", "Cancel", "रद्द करें"), false, secondary, onDismiss)
                    Spacer(Modifier.width(6.dp))
                    EngineDialogButton(tr(lang, "Save", "Save", "सहेजें"), true, secondary) { onSave(text) }
                }
            }
        }
    }
}

@Composable
private fun ConfirmGlassDialog(
    title: String, message: String, confirmLabel: String, cancelLabel: String, isDark: Boolean,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    LiquidGlassDialog(isDark = isDark, onDismissRequest = onDismiss) {
        EngineGlassSurface(isDark, Modifier.fillMaxWidth().widthIn(max = 380.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primary)
                Spacer(Modifier.height(8.dp))
                Text(message, fontSize = 13.sp, color = secondary, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    EngineDialogButton(cancelLabel, false, secondary, onDismiss)
                    Spacer(Modifier.width(6.dp))
                    EngineDialogButton(confirmLabel, true, secondary, onConfirm)
                }
            }
        }
    }
}

@Composable
private fun ScriptsManagerDialog(
    isDark: Boolean, lang: AppLanguage, context: Context,
    onEdit: (String) -> Unit, onChanged: () -> Unit, onDismiss: () -> Unit
) {
    val primary = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondary = if (isDark) DarkTextSecondary else LightTextSecondary
    var scripts by remember { mutableStateOf(AdvancedEngineSettings.listScripts(context).map { it.name }) }
    var selected by remember { mutableStateOf(AdvancedEngineSettings.luaSelected(context)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            var name = "script.lua"
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) name = c.getString(0) ?: name
            }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
            if (AdvancedEngineSettings.importScript(context, name, bytes)) {
                scripts = AdvancedEngineSettings.listScripts(context).map { it.name }
                onChanged()
            }
        } catch (_: Throwable) {
            Toast.makeText(context, tr(lang, "Could not add the script", "Script add nahi ho payi", "स्क्रिप्ट जोड़ी नहीं जा सकी"), Toast.LENGTH_LONG).show()
        }
    }
    LiquidGlassDialog(isDark = isDark, onDismissRequest = onDismiss) {
        EngineGlassSurface(isDark, Modifier.fillMaxWidth().widthIn(max = 440.dp).fillMaxHeight(0.78f)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(tr(lang, "Scripts", "Scripts", "स्क्रिप्ट्स"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    tr(lang, "Tick the scripts mpv should load (needs \"Enable Lua / JS scripts\").", "Jo scripts mpv ko load karni hain unhe tick karo (\"Lua / JS scripts chalu karo\" zaroori hai).", "जो स्क्रिप्ट्स mpv लोड करे उन्हें टिक करें (\"Lua / JS स्क्रिप्ट्स चालू करें\" ज़रूरी है)।"),
                    fontSize = 11.5.sp, color = secondary, lineHeight = 16.sp
                )
                Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 10.dp)) {
                    if (scripts.isEmpty()) {
                        Text(tr(lang, "No scripts yet. Add a .lua or .js file.", "Abhi koi script nahi. .lua ya .js file add karo.", "अभी कोई स्क्रिप्ट नहीं। .lua या .js फ़ाइल जोड़ें।"), fontSize = 13.sp, color = secondary)
                    }
                    scripts.forEach { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.Black.copy(alpha = if (isDark) 0.16f else 0.04f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = name in selected,
                                onCheckedChange = { on ->
                                    selected = if (on) selected + name else selected - name
                                    AdvancedEngineSettings.setLuaSelected(context, selected)
                                    onChanged()
                                }
                            )
                            Text(name, fontSize = 14.sp, color = primary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onEdit(name) }) { Icon(Icons.Outlined.Edit, contentDescription = null, tint = secondary) }
                            IconButton(onClick = {
                                AdvancedEngineSettings.deleteScript(context, name)
                                scripts = AdvancedEngineSettings.listScripts(context).map { it.name }
                                selected = AdvancedEngineSettings.luaSelected(context)
                                onChanged()
                            }) { Icon(Icons.Outlined.Delete, contentDescription = null, tint = secondary) }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EngineDialogButton(tr(lang, "+ Add script", "+ Script add karo", "+ स्क्रिप्ट जोड़ें"), false, secondary) { picker.launch(arrayOf("*/*")) }
                    Spacer(Modifier.weight(1f))
                    EngineDialogButton(tr(lang, "Done", "Done", "हो गया"), true, secondary, onDismiss)
                }
            }
        }
    }
}

// ============================================================================================================
// small building blocks
// ============================================================================================================

@Composable
private fun EngineSectionHeader(text: String, secondaryText: Color) {
    Text(text = text, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, letterSpacing = 0.5.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun EngineCard(cardBg: Color, borderColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

@Composable
private fun EngineIcon(icon: ImageVector, isDark: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isDark) AccentSkyBlue.copy(alpha = 0.19f) else AccentSkyBlueStrong.copy(alpha = 0.09f)),
        contentAlignment = Alignment.Center
    ) {
        StyledIcon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) AccentSkyBlue else AccentSkyBlueStrong,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EngineTexts(title: String, subtitle: String, primaryText: Color, secondaryText: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(text = title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = primaryText)
        Spacer(Modifier.height(2.dp))
        Text(text = subtitle, fontSize = 12.sp, color = secondaryText, lineHeight = 16.sp)
    }
}

@Composable
private fun EngineSwitchRow(
    icon: ImageVector, title: String, subtitle: String, checked: Boolean, isDark: Boolean,
    primaryText: Color, secondaryText: Color, enabled: Boolean = true, onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EngineIcon(icon, isDark)
        Spacer(Modifier.width(14.dp))
        EngineTexts(title, subtitle, primaryText, secondaryText, Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        ViauraSwitch(checked = checked, onCheckedChange = if (enabled) onChange else null, enabled = enabled)
    }
}

@Composable
private fun EngineActionRow(
    icon: ImageVector, title: String, subtitle: String, actionLabel: String, isDark: Boolean,
    primaryText: Color, secondaryText: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EngineIcon(icon, isDark)
        Spacer(Modifier.width(14.dp))
        EngineTexts(title, subtitle, primaryText, secondaryText, Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AccentSkyBlue.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentSkyBlue.copy(alpha = 0.35f)),
            onClick = onClick
        ) {
            Text(
                text = actionLabel,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = AccentSkyBlue,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
