package com.example.ui.screens

import com.example.ui.theme.AccentSkyBlueStrong
import com.example.R
import com.example.ui.components.PlayerGlassModalSheet
import com.example.ui.components.NormalItemColor
import com.example.ui.components.ActiveHighlightColor
import com.example.ui.components.SheetButtonBgColor
import com.example.ui.components.SheetButtonBorderColor
import com.example.ui.components.SheetButtonTextColor
import com.example.ui.theme.Neutral
import com.example.ai.TrackTranslationStatus
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ripple
import com.example.ui.components.SeekIcon
import com.example.ui.components.StyledIcon
import com.example.ui.components.LocalLiquidGlass
import com.example.ui.components.LocalLiquidGlassControls
import com.example.ui.components.LiquidGlassVerticalBar
import com.example.ui.components.drawLiquidGlassSeekbarTrack
import com.example.ui.components.drawLiquidGlassSeekbarThumb
import com.example.ui.components.liquidSeekTrackHeightDp
import com.example.ui.components.glassOuterShadow
import com.example.ui.components.liquidGlassBorder
import com.example.ui.components.videoGlassBackdrop
import com.example.ui.components.ViauraSurfaceStyle
import com.example.ui.components.liquidGlassBorderBrush
import com.example.ui.components.liquidGlassPlayerFill
import com.example.ui.components.liquidGlassSheenBrush
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.BatteryManager
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import com.example.ui.components.AutoScrollingText
import com.example.ui.components.bounceClick
import com.example.ui.components.cardBounceClick
import com.example.ui.components.cardBounceCombinedClick
import com.example.ui.components.loadVideoThumbnail
import com.example.ui.state.ControlsAnimationStyle
import com.example.ui.components.SubtitleSelectionPanel
import com.example.ui.components.AudioSelectionPanel
import com.example.ui.components.AudioDelayPanel
import com.example.ui.components.AudioEqualizerPanel
import com.example.ui.components.SubtitleDelayPanel
import com.example.ui.components.SubtitleStylePanel
import com.example.ui.components.ChapterSelectionPanel
import com.example.ui.components.MoreMenuSelectionPanel
import com.example.ui.components.SleepTimerPanel
import com.example.ui.components.SpeedSelectionPanel
import com.example.player.TrackAudioConfig
import com.example.player.AudioChannelMode
import com.example.player.VideoFilterPreset
import com.example.player.ManualVideoAdjustments
import com.example.player.ChapterSkipMarker
import com.example.player.ChapterSkipDetector
import com.example.ui.components.PlayerChapterSkipButton
import com.example.ui.components.VideoZoomSelectionPanel
import com.example.ui.components.VideoEqSelectionPanel
import com.example.ui.components.VideoSettingSelectionPanel
import com.example.ui.components.ManualVideoAdjustmentPanel
import com.example.ui.components.AbLoopSelectionPanel
import com.example.ui.components.PlaylistSelectionPanel
import com.example.ui.state.GestureSensitivityMode
import com.example.ui.state.PlayerControlId
import com.example.ui.state.PlayerLayoutConfig
import com.example.ui.state.PlayerSettings
import com.example.ui.theme.isAppInDarkTheme
import com.example.ui.theme.LocalAppFontFamily
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.drawWithContent
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatColorFill
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import com.example.ui.components.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.util.AppHaptics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Opens the system-level Cast / Smart View / Wireless Display screen, which actively searches for nearby
 * Chromecast / Miracast / Smart TV devices and mirrors the phone screen (including this player) to the chosen one.
 *
 * Why the old version never worked: it guarded every launch with `intent.resolveActivity(...) != null`. Since Android 11
 * (package visibility) that returns null for the system Settings screens unless the app declares <queries>, so the
 * picker was reported as "unavailable" even on phones that have it. Now the launch is simply attempted (and the
 * resulting ActivityNotFoundException / SecurityException is handled), the manifest declares the matching <queries>,
 * and several system + OEM entry points are tried in order.
 */
fun openSystemCastPicker(context: Context, onUnavailable: (String) -> Unit) {
    fun tryStart(intent: android.content.Intent): Boolean {
        return try {
            if (context !is android.app.Activity) intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    // 1) Standard Android entry points (Pixel / AOSP / most OEMs)
    val systemActions = listOf(
        android.provider.Settings.ACTION_CAST_SETTINGS,
        "android.settings.WIFI_DISPLAY_SETTINGS"
    )
    for (action in systemActions) {
        if (tryStart(android.content.Intent(action))) return
    }

    // 2) OEM screen-sharing apps (Samsung Smart View, Xiaomi Cast ...): open their launcher screen when installed
    val oemPackages = listOf(
        "com.samsung.android.smartmirroring",
        "com.milink.service"
    )
    for (pkg in oemPackages) {
        val launch = try { context.packageManager.getLaunchIntentForPackage(pkg) } catch (_: Throwable) { null }
        if (launch != null && tryStart(launch)) return
    }

    // 3) Last resort: the quick-settings tile is the only entry on some ROMs
    onUnavailable("Cast screen not found. Open Cast / Smart View from the quick settings panel and connect your TV")
}

// =====================================================================================================================
// Real Cast (DLNA / UPnP). The player's Cast button first looks for TVs on the Wi-Fi network (SSDP), sends them the video
// (a local file through the app's own local HTTP server, a web link directly) and then lets the normal player controls
// (play / pause / seek) drive the TV. When there is no Wi-Fi or no TV is found, the old system screen mirroring is used.
// =====================================================================================================================

/** A TV / speaker on the network that can play a video (UPnP MediaRenderer with an AVTransport service). */
data class CastDevice(val id: String, val name: String, val controlUrl: String, val serviceType: String)

object DlnaCast {
    /** TVs found by the last search (shown in the cast sheet). */
    val devices = mutableStateListOf<CastDevice>()
    var searching by mutableStateOf(false)
        private set
    /** The TV that is playing right now, or null. */
    var active by mutableStateOf<CastDevice?>(null)
        private set

    /** Called (main thread) when the cast ends - from the app, from the TV, or because the TV was lost. */
    var onEnded: (() -> Unit)? = null
    /** Called (main thread) when the TV was paused / resumed with its own remote: the player follows. */
    var onRemotePlayState: ((Boolean) -> Unit)? = null
    /** True while the player follows the TV: the player's own play / pause calls must not be sent back to the TV. */
    @Volatile var suppressEcho = false

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val io = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "viaura-cast-io").apply { isDaemon = true } }
    private val finder = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "viaura-cast-find").apply { isDaemon = true } }
    @Volatile private var generation = 0
    @Volatile private var pendingSeekMs = 0L
    @Volatile private var lastLocalCommandAt = 0L
    private val seekRunnable = Runnable {
        val d = active ?: return@Runnable
        val target = pendingSeekMs
        io.execute { soap(d, "Seek", "<Unit>REL_TIME</Unit><Target>${formatRelTime(target)}</Target>") }
    }

    // ---------------------------------------------------------------- network checks

    fun hasWifi(context: Context): Boolean = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET))
    } catch (_: Throwable) {
        false
    }

    private fun lanAddress(context: Context): String? = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        cm.getLinkProperties(cm.activeNetwork)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is java.net.Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    } catch (_: Throwable) {
        null
    }

    // ---------------------------------------------------------------- discovery (SSDP)

    fun discover(context: Context) {
        if (searching) return
        val appCtx = context.applicationContext
        searching = true
        val gen = ++generation
        finder.execute {
            val found = LinkedHashMap<String, CastDevice>()
            var lock: android.net.wifi.WifiManager.MulticastLock? = null
            try {
                // Wi-Fi drops multicast packets unless a multicast lock is held
                val wm = appCtx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                lock = wm?.createMulticastLock("viaura-cast")?.apply { setReferenceCounted(false); acquire() }
                val socket = java.net.DatagramSocket(null)
                try {
                    socket.reuseAddress = true
                    socket.bind(java.net.InetSocketAddress(0))
                    socket.soTimeout = 1000
                    try {
                        val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                        cm.activeNetwork?.bindSocket(socket)
                    } catch (_: Throwable) {}
                    val group = java.net.InetAddress.getByName("239.255.255.250")
                    val targets = listOf("urn:schemas-upnp-org:device:MediaRenderer:1", "urn:schemas-upnp-org:service:AVTransport:1")
                    repeat(2) {
                        for (st in targets) {
                            val msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 2\r\nST: $st\r\n\r\n"
                            val bytes = msg.toByteArray(Charsets.ISO_8859_1)
                            try { socket.send(java.net.DatagramPacket(bytes, bytes.size, group, 1900)) } catch (_: Throwable) {}
                        }
                    }
                    val locations = LinkedHashSet<String>()
                    val end = System.currentTimeMillis() + 4500L
                    val buf = ByteArray(2048)
                    while (System.currentTimeMillis() < end) {
                        try {
                            val packet = java.net.DatagramPacket(buf, buf.size)
                            socket.receive(packet)
                            val text = String(packet.data, 0, packet.length, Charsets.ISO_8859_1)
                            val loc = text.lineSequence().firstOrNull { it.startsWith("LOCATION:", true) }?.substringAfter(':')?.trim()
                            if (!loc.isNullOrBlank() && loc.startsWith("http")) locations.add(loc)
                        } catch (_: java.net.SocketTimeoutException) {
                        }
                    }
                    for (loc in locations) {
                        val d = describe(loc) ?: continue
                        found[d.id] = d
                    }
                } finally {
                    try { socket.close() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
            } finally {
                try { lock?.release() } catch (_: Throwable) {}
            }
            main.post {
                if (gen == generation) {
                    devices.clear()
                    devices.addAll(found.values)
                }
                searching = false
            }
        }
    }

    private fun httpGet(url: String): String? = try {
        val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        c.connectTimeout = 2500
        c.readTimeout = 3000
        val text = try {
            if (c.responseCode in 200..299) c.inputStream.use { it.readBytes().take(512 * 1024).toByteArray().toString(Charsets.UTF_8) } else null
        } finally {
            c.disconnect()
        }
        text
    } catch (_: Throwable) {
        null
    }

    /** Reads the device description: name + the AVTransport control address. Null when it cannot play video. */
    private fun describe(location: String): CastDevice? {
        val xml = httpGet(location) ?: return null
        return try {
            val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(java.io.ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
            fun first(tag: String): String? = doc.getElementsByTagName(tag).item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
            val name = first("friendlyName") ?: return null
            val udn = first("UDN") ?: location
            val services = doc.getElementsByTagName("service")
            var control: String? = null
            var type: String? = null
            for (i in 0 until services.length) {
                val node = services.item(i)
                var sType: String? = null
                var sControl: String? = null
                val kids = node.childNodes
                for (k in 0 until kids.length) {
                    val kid = kids.item(k)
                    when (kid.nodeName) {
                        "serviceType" -> sType = kid.textContent?.trim()
                        "controlURL" -> sControl = kid.textContent?.trim()
                    }
                }
                if (sType != null && sControl != null && sType.contains("AVTransport")) {
                    control = sControl
                    type = sType
                    break
                }
            }
            if (control == null || type == null) return null
            val base = first("URLBase")?.let { if (it.startsWith("http")) it else null } ?: location
            CastDevice(udn, name, java.net.URL(java.net.URL(base), control).toString(), type)
        } catch (_: Throwable) {
            null
        }
    }

    // ---------------------------------------------------------------- control (SOAP)

    private fun xmlEscape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

    private fun formatRelTime(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }

    private fun parseRelTime(text: String?): Long? {
        val parts = text?.trim()?.split(':') ?: return null
        if (parts.size != 3) return null
        val h = parts[0].toLongOrNull() ?: return null
        val m = parts[1].toLongOrNull() ?: return null
        val s = parts[2].toDoubleOrNull() ?: return null
        return ((h * 3600 + m * 60) * 1000L) + (s * 1000.0).toLong()
    }

    /** One SOAP action. Returns the response text, or null when the TV refused / did not answer. */
    private fun soap(d: CastDevice, action: String, args: String): String? {
        return try {
            val body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body><u:$action xmlns:u=\"${d.serviceType}\"><InstanceID>0</InstanceID>$args</u:$action></s:Body></s:Envelope>"
            val c = java.net.URL(d.controlUrl).openConnection() as java.net.HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 3000
            c.readTimeout = 6000
            c.doOutput = true
            c.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
            c.setRequestProperty("SOAPACTION", "\"${d.serviceType}#$action\"")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            val text = try {
                (if (code in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) }
            } finally {
                c.disconnect()
            }
            if (code in 200..299) (text ?: "") else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun tag(xml: String?, name: String): String? =
        xml?.let { Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).find(it)?.groupValues?.getOrNull(1)?.trim() }

    // ---------------------------------------------------------------- session

    /**
     * Sends [mediaPath] (a local file path, a content:// uri or a web link) to [device] and starts it at [positionMs].
     * [onDone] is called on the main thread: true = the TV is playing, false = [message] says why not.
     */
    fun start(context: Context, device: CastDevice, mediaPath: String, title: String, positionMs: Long, onDone: (Boolean, String) -> Unit) {
        val appCtx = context.applicationContext
        val gen = ++generation
        io.execute {
            var ok = false
            var message = "The TV did not accept the video"
            try {
                val url: String? = when {
                    mediaPath.startsWith("http://127.0.0.1") || mediaPath.startsWith("http://localhost") -> {
                        message = "This video cannot be cast"
                        null
                    }
                    mediaPath.startsWith("http://") || mediaPath.startsWith("https://") -> mediaPath
                    mediaPath.startsWith("archive://") || mediaPath.startsWith("magnet:") -> {
                        message = "This video cannot be cast"
                        null
                    }
                    else -> {
                        val lan = lanAddress(appCtx)
                        if (lan == null) {
                            message = "Wi-Fi address not found"
                            null
                        } else {
                            com.example.util.archive.ArchiveStreamServer.castUrlFor(appCtx, mediaPath, title.ifBlank { mediaPath.substringAfterLast('/') }, lan)
                        }
                    }
                }
                if (url != null) {
                    val mime = if (url.contains("/cast/")) com.example.util.archive.ArchiveStreamServer.castMimeFor(url)
                    else com.example.util.archive.ArchiveEngine.mimeTypeFor(url.substringBefore('?')).let { if (it == "application/octet-stream") "video/mp4" else it }
                    val didl = "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                        "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\"><item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
                        "<dc:title>${xmlEscape(title.ifBlank { "Viaura" })}</dc:title><upnp:class>object.item.videoItem</upnp:class>" +
                        "<res protocolInfo=\"http-get:*:$mime:*\">${xmlEscape(url)}</res></item></DIDL-Lite>"
                    soap(device, "Stop", "")
                    val set = soap(device, "SetAVTransportURI", "<CurrentURI>${xmlEscape(url)}</CurrentURI><CurrentURIMetaData>${xmlEscape(didl)}</CurrentURIMetaData>")
                    val play = if (set != null) soap(device, "Play", "<Speed>1</Speed>") else null
                    ok = set != null && play != null
                    if (ok) {
                        if (positionMs > 3000L) {
                            // most TVs only accept a seek once they are playing
                            var waited = 0
                            while (waited < 8000) {
                                val state = tag(soap(device, "GetTransportInfo", ""), "CurrentTransportState")
                                if (state == "PLAYING") break
                                Thread.sleep(500)
                                waited += 500
                            }
                            soap(device, "Seek", "<Unit>REL_TIME</Unit><Target>${formatRelTime(positionMs)}</Target>")
                        }
                        lastLocalCommandAt = System.currentTimeMillis()
                        main.post { active = device }
                        pollLoop(device, gen)
                    } else {
                        com.example.util.archive.ArchiveStreamServer.stopCast()
                    }
                }
            } catch (_: Throwable) {
                ok = false
                com.example.util.archive.ArchiveStreamServer.stopCast()
            }
            main.post { onDone(ok, message) }
        }
    }

    /** Watches the TV every two seconds: it ends the session when the TV stops / is lost and mirrors pause / play done on the TV remote. */
    private fun pollLoop(device: CastDevice, gen: Int) {
        Thread({
            var stoppedCount = 0
            var failures = 0
            var wasPlaying = false
            while (gen == generation) {
                try { Thread.sleep(2000L) } catch (_: InterruptedException) { break }
                if (gen != generation) break
                val info = soap(device, "GetTransportInfo", "")
                if (info == null) {
                    failures++
                    if (failures >= 4) break
                    continue
                }
                failures = 0
                val state = tag(info, "CurrentTransportState")
                when (state) {
                    "PLAYING" -> {
                        wasPlaying = true
                        stoppedCount = 0
                        if (System.currentTimeMillis() - lastLocalCommandAt > 3500L) main.post { onRemotePlayState?.invoke(true) }
                    }
                    "PAUSED_PLAYBACK" -> {
                        stoppedCount = 0
                        if (System.currentTimeMillis() - lastLocalCommandAt > 3500L) main.post { onRemotePlayState?.invoke(false) }
                    }
                    "STOPPED", "NO_MEDIA_PRESENT" -> {
                        if (wasPlaying) {
                            stoppedCount++
                            if (stoppedCount >= 2) break
                        }
                    }
                    else -> {}
                }
            }
            if (gen == generation) main.post { endSession(sendStop = false) }
        }, "viaura-cast-poll").apply { isDaemon = true }.start()
    }

    private fun endSession(sendStop: Boolean) {
        val d = active
        generation++
        main.removeCallbacks(seekRunnable)
        if (sendStop && d != null) io.execute { soap(d, "Stop", "") }
        com.example.util.archive.ArchiveStreamServer.stopCast()
        val wasActive = d != null
        active = null
        if (wasActive) onEnded?.invoke()
    }

    /** Stops the TV and gives the phone its sound back. */
    fun stop() = endSession(sendStop = true)

    // ---------------------------------------------------------------- player -> TV (called by the normal player controls)

    fun onLocalPlayPause(play: Boolean) {
        val d = active ?: return
        if (suppressEcho) return
        lastLocalCommandAt = System.currentTimeMillis()
        io.execute { soap(d, if (play) "Play" else "Pause", if (play) "<Speed>1</Speed>" else "") }
    }

    fun onLocalSeek(ms: Long) {
        if (active == null || suppressEcho) return
        lastLocalCommandAt = System.currentTimeMillis()
        pendingSeekMs = ms
        main.removeCallbacks(seekRunnable)
        main.postDelayed(seekRunnable, 400L) // a dragged seek bar sends many seeks: only the last one goes to the TV
    }
}

@Composable
private fun CastSheetRow(title: String, subtitle: String, highlighted: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SheetButtonBgColor)
            .border(1.dp, if (highlighted) ActiveHighlightColor else SheetButtonBorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = title, color = if (highlighted) ActiveHighlightColor else SheetButtonTextColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = subtitle, color = Neutral.S475569, fontSize = 12.sp)
    }
}

/** Cast sheet of the player (same glass bottom sheet as the other player panels): TVs found on the Wi-Fi + the system mirroring entry. */
@Composable
fun CastDeviceSheet(
    mediaPath: String?,
    mediaTitle: String,
    positionProvider: () -> Long,
    isLandscape: Boolean,
    glassBlurTransparency: Float,
    onCastStarted: () -> Unit,
    onSystemMirroring: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val devices = DlnaCast.devices
    val searching = DlnaCast.searching
    val active = DlnaCast.active
    var error by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (DlnaCast.active == null) DlnaCast.discover(context) }

    PlayerGlassModalSheet(
        panelKey = "cast_devices",
        isLandscape = isLandscape,
        heightFraction = 0.56f,
        glassBlurTransparency = glassBlurTransparency,
        onDismissRequest = onDismiss,
        headerStartContent = {
            Text(text = "Cast to TV", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NormalItemColor)
        },
        headerEndContent = {
            if (searching) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ActiveHighlightColor)
            } else if (active == null) {
                Text(
                    text = "Search again",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActiveHighlightColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { error = null; DlnaCast.discover(context) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (active != null) {
                CastSheetRow("Casting to ${active.name}", "Tap to stop casting", true) {
                    DlnaCast.stop()
                    onDismiss()
                }
            } else {
                devices.forEach { d ->
                    CastSheetRow(d.name, if (connecting == d.id) "Connecting…" else "Tap to cast", false) {
                        val path = mediaPath
                        if (connecting != null) return@CastSheetRow
                        if (path.isNullOrBlank()) {
                            error = "This video cannot be cast"
                            return@CastSheetRow
                        }
                        error = null
                        connecting = d.id
                        DlnaCast.start(context, d, path, mediaTitle, positionProvider()) { ok, msg ->
                            connecting = null
                            if (ok) {
                                onCastStarted()
                                onDismiss()
                            } else {
                                error = msg
                            }
                        }
                    }
                }
                if (devices.isEmpty() && !searching) {
                    Text(text = "No TV found on this Wi-Fi network.", color = Neutral.S475569, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            error?.let { Text(text = it, color = Neutral.S475569, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
            CastSheetRow("System screen mirroring", "Cast / Smart View / Wireless Display of the whole screen", false) {
                onSystemMirroring()
                onDismiss()
            }
        }
    }
}
