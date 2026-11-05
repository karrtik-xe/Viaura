package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.components.HighResImage
import com.example.util.media.MediaHistoryStore
import kotlinx.coroutines.delay
import kotlin.math.abs
import com.example.ui.components.LinkThumbnail
import com.example.ui.components.StyledIcon
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.DarkGlassBorder
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightGlassBorder
import com.example.ui.theme.LightGlassSurface
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.util.media.MediaCatalogEntry
import com.example.util.media.MediaLibraryStore
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaSavedVideo
import com.example.util.media.MediaPluginInfo
import com.example.util.media.MediaPluginStore
import com.example.util.media.MediaPost
import com.example.util.media.MediaProviderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Last known plugin lists, so the Media page can draw immediately while the lists are re-read in the background. */
private object MediaPluginListCache {
    @Volatile var manifest: List<MediaPluginInfo> = emptyList()
    @Volatile var installed: List<MediaPluginInfo> = emptyList()
    @Volatile var loaded: Boolean = false
}

/**
 * Network > "Media": paste a Vega / CloudStream repository link (or short code), tap the download button, and every plugin of that
 * repository is listed below with its own download / update button (and "Install all"). Installed providers open as a page.
 */
@Composable
fun MediaPluginManager(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onOpenPlugin: (MediaPluginInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val surfaceColor = if (isDark) DarkGlassSurface else LightGlassSurface
    val borderColor = if (isDark) DarkGlassBorder else LightGlassBorder
    val outerBg = Color.Transparent

    val tick by MediaPluginStore.changes.collectAsState()
    // The lists are read (files + JSON) on a background thread; the last known lists are shown at once, so opening
    // the Media page / sliding to it does not freeze the UI.
    var manifest by remember { mutableStateOf(MediaPluginListCache.manifest) }
    var installed by remember { mutableStateOf(MediaPluginListCache.installed) }
    LaunchedEffect(tick) {
        val (m, i) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            MediaPluginStore.getManifest(context) to MediaPluginStore.getInstalled(context)
        }
        MediaPluginListCache.manifest = m
        MediaPluginListCache.installed = i
        MediaPluginListCache.loaded = true
        manifest = m
        installed = i
    }
    val listsLoaded = MediaPluginListCache.loaded || manifest.isNotEmpty() || installed.isNotEmpty()

    var repoUrl by rememberSaveable { mutableStateOf(MediaPluginStore.getRepoUrl(context)) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var loadingRepo by remember { mutableStateOf(false) }
    val busy by MediaPluginStore.busy.collectAsState()

    fun loadRepo() {
        if (loadingRepo) return
        val text = repoUrl.trim()
        if (text.isEmpty()) {
            Toast.makeText(context, "Enter a repository link or code", Toast.LENGTH_SHORT).show()
            return
        }
        focusManager.clearFocus()
        loadingRepo = true
        scope.launch {
            try {
                val n = MediaPluginStore.loadRepository(context, text)
                Toast.makeText(context, "$n plugins found", Toast.LENGTH_SHORT).show()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Toast.makeText(context, t.message?.take(160) ?: "Could not load this repository", Toast.LENGTH_LONG).show()
            } finally {
                loadingRepo = false
            }
        }
    }

    fun install(p: MediaPluginInfo) {
        MediaPluginStore.installAsync(context, p) { err ->
            if (err != null) Toast.makeText(context, err.message?.take(160) ?: "Install failed", Toast.LENGTH_LONG).show()
        }
    }

    // Installed plugins are listed first; repository entries follow.
    val ordered = remember(manifest, installed) {
        val manifestById = manifest.associateBy { it.value }
        val done = installed.sortedByDescending { it.installedAt }.map { inst ->
            val listed = manifestById[inst.value]
            // fall back to the locally saved version / description
            listed ?: inst
        }
        val installedIds = installed.map { it.value }.toSet()
        done + manifest.filter { it.value !in installedIds }
    }
    val listState = rememberLazyListState()
    var pasteFlash by remember { mutableStateOf(false) }
    LaunchedEffect(pasteFlash) {
        if (pasteFlash) {
            kotlinx.coroutines.delay(1400L)
            pasteFlash = false
        }
    }



    Column(modifier = modifier.fillMaxSize()) {
        // repository URL card (same look as the Local Network link field)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(outerBg)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(surfaceColor)
                    .border(1.dp, borderColor, RoundedCornerShape(22.dp))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StyledIcon(
                    drawableRes = R.drawable.viaura_cloud,
                    contentDescription = null,
                    tint = secondaryText.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (repoUrl.isEmpty()) {
                        Text(
                            text = "Vega or CloudStream repository link...",
                            fontSize = 15.sp,
                            color = secondaryText.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = repoUrl,
                        onValueChange = { repoUrl = it },
                        singleLine = true,
                        textStyle = TextStyle(color = primaryText, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                        cursorBrush = SolidColor(AccentSkyBlue),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { loadRepo() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (repoUrl.isNotEmpty()) {
                    // cancel: removes the text from the retained repository input
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                repoUrl = ""
                                pasteFlash = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        StyledIcon(
                            drawableRes = R.drawable.viaura_close_circle,
                            contentDescription = "Clear link",
                            tint = secondaryText.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            val text = clipboard.getText()?.text?.trim().orEmpty()
                            if (text.isNotEmpty()) {
                                repoUrl = text
                                pasteFlash = true
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    StyledIcon(
                        drawableRes = if (pasteFlash) R.drawable.viaura_pasted else R.drawable.viaura_paste,
                        contentDescription = "Paste URL",
                        tint = if (pasteFlash) AccentSkyBlue else primaryText.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            val canLoad = repoUrl.isNotBlank() && !loadingRepo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (canLoad) AccentGradient else SolidColor(if (isDark) Neutral.D1E293B else Color(0xFFD5D9DD)))
                    .then(
                        if (canLoad) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { loadRepo() } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (loadingRepo) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                } else {
                    StyledIcon(
                        drawableRes = R.drawable.viaura_download_minimalistic,
                        contentDescription = "Load repository",
                        tint = if (canLoad) Color.White else secondaryText.copy(alpha = 0.45f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        com.example.ui.components.NetworkBypassCard(isDark = isDark)

        if (!listsLoaded) {
            Box(modifier = Modifier.fillMaxSize())
        } else if (manifest.isEmpty() && installed.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text(
                    text = "Paste a Vega or CloudStream repository link or code above and tap the download button. All its plugins will be listed here to install.",
                    fontSize = 13.sp,
                    color = secondaryText,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 12.dp, top = 22.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plugins (${ordered.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    modifier = Modifier.weight(1f)
                )
                val pendingPlugins = manifest.filter { p -> installed.firstOrNull { it.value == p.value }?.version != p.version }
                if (pendingPlugins.isNotEmpty()) {
                    val working = pendingPlugins.any { it.value in busy }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(surfaceColor)
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!working) {
                                    MediaPluginStore.installAllAsync(context, pendingPlugins) { ok, failed ->
                                        Toast.makeText(
                                            context,
                                            if (failed == 0) "$ok plugins installed" else "$ok installed, $failed failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (working) "Installing…" else "Install all (${pendingPlugins.size})",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = primaryText
                        )
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(ordered, key = { it.value }) { plugin ->
                    val installedVersion = installed.firstOrNull { it.value == plugin.value }?.version
                    PluginRow(
                        modifier = Modifier.animateItem(),
                        onOpen = if (installedVersion != null) { { onOpenPlugin(installed.first { it.value == plugin.value }) } } else null,
                        plugin = plugin,
                        installedVersion = installedVersion,
                        busy = plugin.value in busy,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        surfaceColor = surfaceColor,
                        borderColor = borderColor,
                        onInstall = { install(plugin) },
                        onRemove = { MediaPluginStore.remove(context, plugin.value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginRow(
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    plugin: MediaPluginInfo,
    installedVersion: String?,
    busy: Boolean,
    primaryText: Color,
    secondaryText: Color,
    surfaceColor: Color,
    borderColor: Color,
    onInstall: () -> Unit,
    onRemove: () -> Unit
) {
    val isInstalled = installedVersion != null
    val hasUpdate = isInstalled && installedVersion != plugin.version
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .then(
                if (onOpen != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen
                ) else Modifier
            )
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinkThumbnail(
            thumbnail = plugin.icon.ifBlank { null },
            // the whole logo is shown (never cropped)
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)),
            fallback = {
                StyledIcon(
                    drawableRes = R.drawable.viaura_cloud,
                    contentDescription = null,
                    tint = secondaryText.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plugin.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // language, version and size on one line (like the CloudStream repository page)
            val meta = buildList {
                languageLabel(plugin.language)?.let { add(it) }
                add("v${plugin.version}")
                sizeLabel(plugin.size)?.let { add(it) }
                if (plugin.type.isNotBlank() && !plugin.isCloudStream) add(plugin.type)
            }.joinToString("  ")
            Text(
                text = meta,
                fontSize = 12.5.sp,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (plugin.description.isNotBlank()) {
                Text(
                    text = plugin.description.trim(),
                    fontSize = 12.5.sp,
                    color = secondaryText.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isInstalled) {
                Text(
                    text = if (hasUpdate) "Update available · installed v$installedVersion" else "Installed",
                    fontSize = 12.sp,
                    color = AccentSkyBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!isInstalled || hasUpdate) {
            if (busy) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentSkyBlue)
                }
            } else {
                PluginIconButton(R.drawable.viaura_download_minimalistic, if (hasUpdate) "Update" else "Download", AccentSkyBlue, onInstall)
            }
        }
        if (isInstalled) {
            PluginIconButton(R.drawable.viaura_trash_bin_2, "Remove", secondaryText.copy(alpha = 0.9f), onRemove)
        }
    }
}

/** "🇬🇧 English" for a repository language code; "🌐 Multi" for several languages; null when unknown. */
private fun languageLabel(code: String): String? {
    val c = code.trim().lowercase()
    if (c.isEmpty()) return null
    if (c == "multi" || c == "all" || c == "multiple") return "\uD83C\uDF10 Multi"
    val flagCountry = when (c) {
        "en" -> "GB"; "hi" -> "IN"; "ta" -> "IN"; "te" -> "IN"; "ml" -> "IN"; "kn" -> "IN"; "mr" -> "IN"; "pa" -> "IN"
        "bn" -> "BD"; "ur" -> "PK"; "ja" -> "JP"; "ko" -> "KR"; "zh" -> "CN"; "ar" -> "SA"; "he" -> "IL"; "fa" -> "IR"
        "el" -> "GR"; "uk" -> "UA"; "sv" -> "SE"; "da" -> "DK"; "cs" -> "CZ"; "vi" -> "VN"; "id" -> "ID"; "tl" -> "PH"
        else -> if (c.length == 2) c.uppercase() else null
    }
    val flag = flagCountry?.let { cc ->
        String(Character.toChars(0x1F1E6 + (cc[0] - 'A'))) + String(Character.toChars(0x1F1E6 + (cc[1] - 'A')))
    }
    val name = try {
        java.util.Locale(c).getDisplayLanguage(java.util.Locale.ENGLISH).takeIf { it.isNotBlank() } ?: c.uppercase()
    } catch (_: Throwable) {
        c.uppercase()
    }
    return if (flag != null) "$flag $name" else name
}

private fun sizeLabel(bytes: Long): String? = when {
    bytes <= 0L -> null
    bytes < 1000L -> "$bytes B"
    bytes < 1_000_000L -> "${bytes / 1000} kB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_000_000.0)
}

@Composable
internal fun PluginIconButton(drawableRes: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        StyledIcon(drawableRes = drawableRes, contentDescription = description, tint = tint, modifier = Modifier.size(21.dp))
    }
}
