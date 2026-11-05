package com.example.ui.components

import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecList
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * About > device information. Every value is READ from the phone (display, codecs, audio outputs, Build / system properties).
 * Nothing is guessed: when the phone does not have a feature (or does not report it) the row says "None" instead of a made-up value.
 */

/** Display info. */
internal data class DetectedDisplayInfo(
    val screenSize: String?,
    val resolution: String?,
    val density: String?,
    val aspectRatio: String?,
    val refreshRate: String?,
    val hdrFormats: String?,
    val wideColorGamut: String?,
    val peakBrightness: String?
)

/** Audio info. */
internal data class DetectedAudioInfo(
    val speakerConfig: String?,
    val dolbyAtmos: String?,
    val dolbyAudio: String?,
    val dts: String?,
    val hiResAudio: String?,
    val bluetoothCodecs: String?,
    val spatialAudio: String?
)

/** System & hardware info. */
internal data class DetectedHardwareInfo(
    val deviceName: String,
    val androidVersion: String,
    val securityOrBuild: String,
    val processor: String,
    val cpuArch: String,
    val cpuCores: String,
    val ramInfo: String?,
    val securityPatch: String? = null
)

private const val NONE = "None"

// ---------------------------------------------------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------------------------------------------------

/** Reads an Android system property (ro.product.marketname, ro.soc.model ...). Null when missing / blocked / "unknown". */
private fun readSystemProp(key: String): String? {
    return try {
        val cls = Class.forName("android.os.SystemProperties")
        val value = cls.getMethod("get", String::class.java).invoke(null, key) as? String
        value?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", ignoreCase = true) }
    } catch (_: Throwable) {
        null
    }
}

private fun cleanValue(raw: String?): String? {
    val v = raw?.trim()
    return if (v.isNullOrEmpty() || v.equals("unknown", ignoreCase = true)) null else v
}

private fun readTextFile(path: String): String? {
    return try {
        java.io.File(path).readText().trim().takeIf { it.isNotEmpty() }
    } catch (_: Throwable) {
        null
    }
}

private fun findDisplay(context: Context): Display? {
    var display: Display? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            display = context.display
        } catch (_: Throwable) {
        }
    }
    if (display == null) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            display = wm?.defaultDisplay
        } catch (_: Throwable) {
        }
    }
    return display
}

// ---------------------------------------------------------------------------------------------------------------------
// DISPLAY
// ---------------------------------------------------------------------------------------------------------------------

internal fun detectDisplayInfo(context: Context): DetectedDisplayInfo {
    return try {
        val appMetrics: DisplayMetrics = context.resources.displayMetrics
        val display = findDisplay(context)

        // REAL panel metrics (not the app window: that one excludes bars / split-screen areas and gave wrong sizes)
        val realMetrics = DisplayMetrics()
        var haveReal = false
        try {
            @Suppress("DEPRECATION")
            display?.getRealMetrics(realMetrics)
            haveReal = realMetrics.widthPixels > 0 && realMetrics.heightPixels > 0
        } catch (_: Throwable) {
        }

        var widthPx = 0
        var heightPx = 0
        try {
            val mode = display?.mode
            if (mode != null && mode.physicalWidth > 0 && mode.physicalHeight > 0) {
                widthPx = mode.physicalWidth
                heightPx = mode.physicalHeight
            }
        } catch (_: Throwable) {
        }
        if (widthPx <= 0 || heightPx <= 0) {
            if (haveReal) {
                widthPx = realMetrics.widthPixels
                heightPx = realMetrics.heightPixels
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    val b = wm?.maximumWindowMetrics?.bounds
                    if (b != null) {
                        widthPx = b.width()
                        heightPx = b.height()
                    }
                } catch (_: Throwable) {
                }
            }
        }
        if (widthPx <= 0 || heightPx <= 0) {
            widthPx = appMetrics.widthPixels
            heightPx = appMetrics.heightPixels
        }
        val maxDim = maxOf(widthPx, heightPx)
        val minDim = minOf(widthPx, heightPx).coerceAtLeast(1)

        // Screen size: physical pixels / the panel's reported dots per inch
        val metricsForDpi = if (haveReal) realMetrics else appMetrics
        val xdpi = if (metricsForDpi.xdpi > 50f) metricsForDpi.xdpi else metricsForDpi.densityDpi.toFloat()
        val ydpi = if (metricsForDpi.ydpi > 50f) metricsForDpi.ydpi else metricsForDpi.densityDpi.toFloat()
        val dimA = widthPx / xdpi
        val dimB = heightPx / ydpi
        val diagonal = sqrt((dimA * dimA + dimB * dimB).toDouble())
        val screenSizeStr = if (diagonal in 2.0..30.0) String.format(java.util.Locale.US, "%.1f\"", diagonal) else null

        val resStr = "$minDim × $maxDim px"

        val dpi = appMetrics.densityDpi
        val densityLabel = when {
            dpi <= 120 -> "ldpi"
            dpi <= 160 -> "mdpi"
            dpi <= 240 -> "hdpi"
            dpi <= 320 -> "xhdpi"
            dpi <= 480 -> "xxhdpi"
            else -> "xxxhdpi"
        }
        val densityStr = "$dpi DPI ($densityLabel)"

        val ratio = maxDim.toFloat() / minDim.toFloat()
        val aspectStr = when {
            ratio in 2.20f..2.26f -> "20:9"
            ratio in 2.14f..2.199f -> "19.5:9"
            ratio in 2.08f..2.139f -> "19:9"
            ratio in 1.98f..2.079f -> "18:9 (2:1)"
            ratio in 1.76f..1.80f -> "16:9"
            ratio in 1.55f..1.62f -> "16:10"
            ratio in 1.30f..1.36f -> "4:3"
            else -> String.format(java.util.Locale.US, "%.2f:1", ratio)
        }

        // Refresh rate: current mode + highest mode the panel offers
        var currentHz = 0f
        var maxHz = 0f
        try {
            currentHz = display?.mode?.refreshRate ?: 0f
            val modes = display?.supportedModes
            if (modes != null) maxHz = modes.maxOfOrNull { it.refreshRate } ?: 0f
        } catch (_: Throwable) {
        }
        if (currentHz <= 0f) {
            try {
                @Suppress("DEPRECATION")
                currentHz = display?.refreshRate ?: 0f
            } catch (_: Throwable) {
            }
        }
        val refreshStr = when {
            currentHz <= 0f -> null
            maxHz > currentHz + 1f -> "${currentHz.roundToInt()} Hz (up to ${maxHz.roundToInt()} Hz)"
            else -> "${currentHz.roundToInt()} Hz"
        }

        // HDR
        var hdrTypes: IntArray? = null
        try {
            hdrTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                display?.mode?.supportedHdrTypes
            } else {
                @Suppress("DEPRECATION")
                display?.hdrCapabilities?.supportedHdrTypes
            }
        } catch (_: Throwable) {
        }
        val hdrList = mutableListOf<String>()
        if (hdrTypes != null) {
            if (hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10)) hdrList.add("HDR10")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)) hdrList.add("HDR10+")
            if (hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)) hdrList.add("Dolby Vision")
            if (hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HLG)) hdrList.add("HLG")
        }
        val hdrStr = if (hdrList.isNotEmpty()) hdrList.joinToString(", ") else "None (SDR only)"

        // Wide colour gamut
        var wide: Boolean? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wide = display?.isWideColorGamut ?: context.resources.configuration.isScreenWideColorGamut
            } catch (_: Throwable) {
            }
        }
        val wcgStr = when (wide) {
            true -> "Wide colour (Display P3)"
            false -> "None (sRGB only)"
            null -> null
        }

        // Peak luminance (only when the panel reports it)
        var peakStr: String = "None (not reported)"
        try {
            @Suppress("DEPRECATION")
            val maxLum = display?.hdrCapabilities?.desiredMaxLuminance ?: 0f
            if (maxLum > 50f) peakStr = "${maxLum.roundToInt()} nits"
        } catch (_: Throwable) {
        }

        DetectedDisplayInfo(
            screenSize = screenSizeStr,
            resolution = resStr,
            density = densityStr,
            aspectRatio = aspectStr,
            refreshRate = refreshStr,
            hdrFormats = hdrStr,
            wideColorGamut = wcgStr,
            peakBrightness = peakStr
        )
    } catch (_: Throwable) {
        val m = context.resources.displayMetrics
        DetectedDisplayInfo(
            screenSize = null,
            resolution = "${minOf(m.widthPixels, m.heightPixels)} × ${maxOf(m.widthPixels, m.heightPixels)} px",
            density = "${m.densityDpi} DPI",
            aspectRatio = null,
            refreshRate = null,
            hdrFormats = "None (SDR only)",
            wideColorGamut = null,
            peakBrightness = "None (not reported)"
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// AUDIO
// ---------------------------------------------------------------------------------------------------------------------

internal fun detectAudioInfo(context: Context): DetectedAudioInfo {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    // Output devices the phone really has
    val outputs: Array<AudioDeviceInfo> = try {
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
    } catch (_: Throwable) {
        emptyArray()
    }

    // Speaker
    val speakerConfig: String = try {
        val speaker = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        when {
            speaker == null -> NONE
            (speaker.channelCounts.maxOrNull() ?: 1) >= 2 -> "Built-in speaker (stereo output)"
            else -> "Built-in speaker"
        }
    } catch (_: Throwable) {
        NONE
    }

    // Audio decoders the device itself offers (the codec list is the only reliable source; plain "contains" checks on
    // video/dolby-vision used to count as Dolby audio, so only audio/* decoder types are looked at now)
    val audioDecoders = mutableSetOf<String>()
    try {
        for (info in MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos) {
            if (info.isEncoder) continue
            for (type in info.supportedTypes) {
                val t = type.lowercase()
                if (t.startsWith("audio/")) audioDecoders.add(t)
            }
        }
    } catch (_: Throwable) {
    }

    val atmos = if (audioDecoders.contains("audio/eac3-joc")) "Device decoder (E-AC-3 JOC)" else NONE

    val dolbyParts = mutableListOf<String>()
    if (audioDecoders.contains("audio/ac3")) dolbyParts.add("AC-3")
    if (audioDecoders.contains("audio/eac3")) dolbyParts.add("E-AC-3")
    val dolby = if (dolbyParts.isNotEmpty()) "Device decoder (${dolbyParts.joinToString(", ")})" else NONE

    val dtsParts = mutableListOf<String>()
    if (audioDecoders.any { it == "audio/vnd.dts" }) dtsParts.add("DTS")
    if (audioDecoders.any { it.startsWith("audio/vnd.dts.hd") }) dtsParts.add("DTS-HD")
    if (audioDecoders.any { it.startsWith("audio/vnd.dts") && it != "audio/vnd.dts" && !it.startsWith("audio/vnd.dts.hd") }) dtsParts.add("DTS:X")
    val dts = if (dtsParts.isNotEmpty()) "Device decoder (${dtsParts.joinToString(", ")})" else NONE

    // Hi-Res output: native mixer rate, and what any output (e.g. USB DAC) reports
    val hiRes: String = try {
        val nativeRate = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 0
        var maxRate = nativeRate
        var deepPcm = false
        for (d in outputs) {
            val rates = d.sampleRates
            if (rates.isNotEmpty()) maxRate = maxOf(maxRate, rates.maxOrNull() ?: 0)
            for (enc in d.encodings) {
                if (enc == AudioFormat.ENCODING_PCM_FLOAT || enc == 21 /* PCM_24BIT_PACKED */ || enc == 22 /* PCM_32BIT */) deepPcm = true
            }
        }
        when {
            maxRate >= 96000 && deepPcm -> "Up to 24-bit / ${formatKhz(maxRate)}"
            maxRate >= 96000 -> "Up to ${formatKhz(maxRate)}"
            nativeRate > 0 -> "None (output ${formatKhz(nativeRate)})"
            else -> NONE
        }
    } catch (_: Throwable) {
        NONE
    }

    // Spatial audio (system Spatializer, Android 12L+)
    val spatial: String = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 && audioManager != null && audioManager.spatializer.isAvailable) {
            "Supported"
        } else {
            NONE
        }
    } catch (_: Throwable) {
        NONE
    }

    // Bluetooth: SBC / AAC are the standard A2DP codecs; LC3 only when the phone supports LE Audio. LDAC / aptX cannot be read through
    // any public Android API (they depend on the connected headset), so they are not listed.
    val bluetooth: String = try {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            NONE
        } else {
            val parts = mutableListOf("SBC", "AAC")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val adapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                    if (adapter != null && adapter.isLeAudioSupported() == BluetoothStatusCodes.FEATURE_SUPPORTED) parts.add("LC3 (LE Audio)")
                } catch (_: Throwable) {
                }
            }
            parts.joinToString(", ")
        }
    } catch (_: Throwable) {
        NONE
    }

    return DetectedAudioInfo(
        speakerConfig = speakerConfig,
        dolbyAtmos = atmos,
        dolbyAudio = dolby,
        dts = dts,
        hiResAudio = hiRes,
        bluetoothCodecs = bluetooth,
        spatialAudio = spatial
    )
}

private fun formatKhz(hz: Int): String {
    val k = hz / 1000.0
    return if (k == k.toInt().toDouble()) "${k.toInt()} kHz" else String.format(java.util.Locale.US, "%.1f kHz", k)
}

// ---------------------------------------------------------------------------------------------------------------------
// SYSTEM & HARDWARE
// ---------------------------------------------------------------------------------------------------------------------

private val MARKET_NAME_PROPS = listOf(
    "ro.product.marketname",
    "ro.product.vendor.marketname",
    "ro.product.odm.marketname",
    "ro.product.system.marketname",
    "ro.vendor.oplus.market.name",
    "ro.oppo.market.name",
    "ro.config.marketing_name"
)

private fun detectDeviceName(): String {
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val modelCode = Build.MODEL ?: ""
    val marketing = MARKET_NAME_PROPS.firstNotNullOfOrNull { readSystemProp(it) }
    val base = marketing ?: modelCode
    val branded = if (base.startsWith(manufacturer, ignoreCase = true) || base.startsWith(Build.BRAND ?: "", ignoreCase = true)) base else "$manufacturer $base"
    return if (marketing != null && !marketing.equals(modelCode, ignoreCase = true) && modelCode.isNotBlank()) "$branded ($modelCode)" else branded.trim()
}

private fun detectProcessor(): String {
    // 1) Android 12+: SoC manufacturer + model (e.g. Qualcomm SM8550, Mediatek MT6877)
    var maker: String? = null
    var model: String? = null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        maker = cleanValue(Build.SOC_MANUFACTURER)
        model = cleanValue(Build.SOC_MODEL)
    }
    if (model == null) model = readSystemProp("ro.soc.model")
    if (maker == null) maker = readSystemProp("ro.soc.manufacturer")
    if (model != null) {
        val m = maker?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        return if (m != null && !model.startsWith(m, ignoreCase = true)) "$m $model" else model
    }
    // 2) /proc/cpuinfo "Hardware" line (some phones)
    try {
        val line = java.io.File("/proc/cpuinfo").readLines().firstOrNull { it.startsWith("Hardware", ignoreCase = true) }
        val hw = cleanValue(line?.substringAfter(":"))
        if (hw != null) return hw
    } catch (_: Throwable) {
    }
    // 3) platform / hardware names (skip generic ones such as "qcom")
    val generic = setOf("qcom", "unknown", "")
    val hardware = cleanValue(Build.HARDWARE)
    val board = cleanValue(Build.BOARD)
    val platform = readSystemProp("ro.board.platform")
    return listOf(hardware, platform, board).firstOrNull { it != null && it.lowercase() !in generic }
        ?: hardware ?: board ?: "Unknown"
}

private fun detectCpuCoreCount(): Int {
    // "0-7" in /sys/devices/system/cpu/possible = 8 cores, even when the system has some of them switched off right now
    try {
        val possible = readTextFile("/sys/devices/system/cpu/possible")
        if (possible != null) {
            var total = 0
            for (part in possible.split(",")) {
                val bits = part.trim().split("-")
                total += if (bits.size == 2) bits[1].toInt() - bits[0].toInt() + 1 else 1
            }
            if (total in 1..32) return total
        }
    } catch (_: Throwable) {
    }
    return Runtime.getRuntime().availableProcessors()
}

private fun formatGb(gb: Double): String {
    return if (gb == gb.toInt().toDouble()) "${gb.toInt()} GB" else String.format(java.util.Locale.US, "%.1f GB", gb)
}

private fun detectRam(context: Context): String? {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val gib = 1024.0 * 1024.0 * 1024.0
        val usable = info.totalMem / gib
        val free = info.availMem / gib
        // the kernel reports a little less than the installed memory (an 8 GB phone shows ~7.4): name the retail size
        val retail = listOf(1, 2, 3, 4, 6, 8, 10, 12, 16, 18, 24, 32, 48, 64)
            .firstOrNull { it >= usable && (it - usable) <= maxOf(0.8, it * 0.12) }
        val total = if (retail != null) formatGb(retail.toDouble()) else formatGb(usable)
        val usableText = String.format(java.util.Locale.US, "%.1f GB usable", usable)
        val freeText = String.format(java.util.Locale.US, "%.1f GB free", free)
        if (retail != null) "$total ($usableText, $freeText)" else "$total ($freeText)"
    } catch (_: Throwable) {
        null
    }
}

internal fun detectHardwareInfo(context: Context): DetectedHardwareInfo {
    return try {
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val buildId = Build.DISPLAY.ifBlank { Build.ID }

        val abis = Build.SUPPORTED_ABIS.toList()
        val cpuArch = when {
            abis.isEmpty() -> "Unknown"
            abis.size == 1 -> abis[0]
            else -> "${abis[0]} (also ${abis.drop(1).joinToString(", ")})"
        }
        val cores = detectCpuCoreCount()

        DetectedHardwareInfo(
            deviceName = detectDeviceName(),
            androidVersion = androidVersion,
            securityOrBuild = buildId,
            processor = detectProcessor(),
            cpuArch = cpuArch,
            cpuCores = "$cores cores",
            ramInfo = detectRam(context),
            securityPatch = try {
                cleanValue(Build.VERSION.SECURITY_PATCH)
            } catch (_: Throwable) {
                null
            }
        )
    } catch (_: Throwable) {
        DetectedHardwareInfo(
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            securityOrBuild = Build.ID,
            processor = "Unknown",
            cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
            cpuCores = "${Runtime.getRuntime().availableProcessors()} cores",
            ramInfo = null
        )
    }
}
