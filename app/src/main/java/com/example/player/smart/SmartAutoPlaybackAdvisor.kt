package com.example.player

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import com.example.ui.state.HwAccelMode

/**
 * Smart Auto Playback: detects what a video needs (codec, resolution, FPS, bit depth, HDR, bitrate) and what the
 * phone can really do (hardware decoders, their size / frame-rate / profile limits, CPU cores, memory), then chooses
 * one of the EXISTING decoder modes ([HwAccelMode]) plus a few render / buffer tunings.
 *
 * Everything here is an estimate from the device's own codec capability report. It never promises smooth playback:
 * when a stream is beyond what the phone reports it can do, the plan says so ([SmartTier.BEYOND_DEVICE]).
 */
enum class SmartTier(val label: String) {
    /** The chosen decoder is expected to handle this stream comfortably. */
    COMFORTABLE("Comfortable"),
    /** Close to the device limit: cheaper rendering / decoding shortcuts are switched on. */
    DEMANDING("Demanding"),
    /** More than the device reports it can decode in real time: drops are likely, a lower resolution is suggested. */
    BEYOND_DEVICE("Beyond device limits")
}

data class SmartDeviceProfile(
    val cpuCores: Int,
    val memoryClassMb: Int,
    val sdk: Int
)

data class SmartStreamInfo(
    val codec: String,
    val width: Int,
    val height: Int,
    val fps: Double,
    val tenBit: Boolean,
    val hdr: Boolean,
    val bitrateBps: Long
) {
    val shortLabel: String
        get() {
            val res = when {
                height >= 4320 -> "8K"
                height >= 2160 -> "4K"
                height >= 1440 -> "1440p"
                height > 0 -> "${height}p"
                else -> "?"
            }
            val rate = if (fps > 1.0) Math.round(fps).toString() else "?"
            return "$res$rate ${codec.uppercase(java.util.Locale.US)}${if (hdr) " HDR" else if (tenBit) " 10-bit" else ""}"
        }
}

/** Render / decode shortcuts. The defaults equal what the player already used before Smart Auto Playback. */
data class SmartTuning(
    val scale: String = "spline36",
    val dscale: String = "spline36",
    val framedrop: String = "vo",
    val autosync: Int = 0,
    val lavcFast: Boolean = false,
    val skipLoopFilter: String = "default",
    val lavcThreads: Int = 4,
    /** null = leave the demuxer buffer as it is. */
    val readaheadSecs: Int? = null,
    val demuxerMaxMiB: Int? = null
)

data class SmartPlan(
    val stream: SmartStreamInfo,
    val hw: HwAccelMode,
    val tier: SmartTier,
    val tuning: SmartTuning,
    /** A lower resolution (in lines) that this device should manage better, or null when none is needed / known. */
    val suggestedHeight: Int?,
    val hwDecoderFound: Boolean,
    val reason: String
)

object SmartPlaybackAdvisor {

    private data class HwCaps(
        val found: Boolean,
        val supportsSizeAndRate: Boolean,
        val supportsProfile: Boolean
    )

    private val capsCache = HashMap<String, HwCaps>()

    // Profile constants as literals so no newer API level is needed to compile.
    private const val HEVC_MAIN10 = 0x2
    private const val HEVC_MAIN10_HDR10 = 0x1000
    private const val VP9_PROFILE2 = 0x4
    private const val AV1_MAIN10 = 0x2
    private const val AVC_HIGH10 = 0x10

    private val STANDARD_HEIGHTS = intArrayOf(2160, 1440, 1080, 720, 480)

    fun deviceProfile(context: Context): SmartDeviceProfile {
        val am = try { context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager } catch (_: Throwable) { null }
        return SmartDeviceProfile(
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            memoryClassMb = try { am?.memoryClass ?: 128 } catch (_: Throwable) { 128 },
            sdk = Build.VERSION.SDK_INT
        )
    }

    /** mpv `video-format` -> Android MIME type, or null when unknown. */
    fun mimeFor(codec: String): String? = when (codec.lowercase(java.util.Locale.US)) {
        "h264", "avc", "avc1" -> "video/avc"
        "hevc", "h265" -> "video/hevc"
        "av1" -> "video/av01"
        "vp9" -> "video/x-vnd.on2.vp9"
        "vp8" -> "video/x-vnd.on2.vp8"
        "mpeg4", "mp4v" -> "video/mp4v-es"
        "mpeg2video", "mpeg2" -> "video/mpeg2"
        "h263" -> "video/3gpp"
        else -> null
    }

    private fun codecCostFactor(codec: String): Double = when (codec.lowercase(java.util.Locale.US)) {
        "av1" -> 3.0
        "hevc", "h265" -> 1.7
        "vp9" -> 1.4
        "vp8" -> 0.9
        "mpeg2video", "mpeg2", "mpeg4", "mp4v", "h263" -> 0.7
        else -> 1.0
    }

    private fun isHardwareCodec(info: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= 29) return info.isHardwareAccelerated && !info.isSoftwareOnly
        val n = info.name.lowercase(java.util.Locale.US)
        return !(n.startsWith("omx.google.") || n.startsWith("c2.android.") || n.startsWith("c2.google.") || n.contains(".sw."))
    }

    private fun profileSupported(caps: MediaCodecInfo.CodecCapabilities, stream: SmartStreamInfo): Boolean {
        if (!stream.tenBit && !stream.hdr) return true
        val levels = caps.profileLevels ?: return true
        return when (stream.codec.lowercase(java.util.Locale.US)) {
            "hevc", "h265" -> levels.any { it.profile == HEVC_MAIN10 || it.profile == HEVC_MAIN10_HDR10 }
            "vp9" -> levels.any { it.profile == VP9_PROFILE2 || it.profile == 0x8 }
            "av1" -> levels.any { it.profile == AV1_MAIN10 }
            "h264", "avc", "avc1" -> levels.any { it.profile == AVC_HIGH10 }
            else -> true
        }
    }

    private fun hardwareCaps(stream: SmartStreamInfo): HwCaps {
        val mime = mimeFor(stream.codec) ?: return HwCaps(found = false, supportsSizeAndRate = false, supportsProfile = false)
        val key = "$mime|${stream.width}x${stream.height}|${(stream.fps * 100).toInt()}|${stream.tenBit}|${stream.hdr}"
        synchronized(capsCache) { capsCache[key]?.let { return it } }
        var found = false
        var sizeRate = false
        var profile = false
        try {
            val infos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            for (info in infos) {
                if (info.isEncoder || !isHardwareCodec(info)) continue
                if (info.supportedTypes.none { it.equals(mime, ignoreCase = true) }) continue
                found = true
                try {
                    val caps = info.getCapabilitiesForType(mime)
                    val vc = caps.videoCapabilities
                    val fps = if (stream.fps > 1.0) stream.fps else 30.0
                    val okSize = vc != null && vc.areSizeAndRateSupported(stream.width, stream.height, fps)
                    val okProfile = profileSupported(caps, stream)
                    if (okSize) sizeRate = true
                    if (okProfile) profile = true
                    if (okSize && okProfile) { sizeRate = true; profile = true; break }
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        val result = HwCaps(found, sizeRate, profile)
        synchronized(capsCache) { capsCache[key] = result }
        return result
    }

    /** Pixels per second the software decoder is expected to manage on this device (rough, conservative). */
    private fun softwareBudget(device: SmartDeviceProfile): Double =
        device.cpuCores.coerceAtMost(8) * 25_000_000.0

    private fun softwareLoad(stream: SmartStreamInfo): Double {
        val fps = if (stream.fps > 1.0) stream.fps else 30.0
        return stream.width.toDouble() * stream.height * fps * codecCostFactor(stream.codec) * (if (stream.tenBit) 1.25 else 1.0)
    }

    /** True when the phone's hardware decoder reports it can decode this size at this frame rate (and bit depth). */
    private fun hwHandles(stream: SmartStreamInfo): Boolean {
        val c = hardwareCaps(stream)
        return c.found && c.supportsSizeAndRate && c.supportsProfile
    }

    fun plan(stream: SmartStreamInfo, device: SmartDeviceProfile): SmartPlan {
        val hw = hardwareCaps(stream)
        val hwOk = hw.found && hw.supportsSizeAndRate && hw.supportsProfile
        val swRatio = softwareLoad(stream) / softwareBudget(device)
        val pixels = stream.width.toLong() * stream.height

        val mode: HwAccelMode
        val tier: SmartTier
        val reason: String
        when {
            hwOk -> {
                mode = HwAccelMode.FORCE
                tier = if (pixels >= 3840L * 2160L && stream.fps > 50.0) SmartTier.DEMANDING else SmartTier.COMFORTABLE
                reason = "Hardware decoder reports support"
            }
            hw.found && !hw.supportsProfile && swRatio <= 1.0 -> {
                mode = HwAccelMode.DISABLE
                tier = if (swRatio > 0.6) SmartTier.DEMANDING else SmartTier.COMFORTABLE
                reason = "Hardware decoder does not report this bit depth / profile - software decoding"
            }
            hw.found -> {
                // The hardware decoder exists but reports a lower size / frame-rate limit. Try it anyway unless the
                // CPU is clearly the better choice.
                if (swRatio <= 0.6) {
                    mode = HwAccelMode.DISABLE
                    tier = SmartTier.COMFORTABLE
                    reason = "Hardware decoder reports a lower limit than this stream - software decoding"
                } else {
                    mode = HwAccelMode.FORCE
                    tier = SmartTier.BEYOND_DEVICE
                    reason = "Stream is above the hardware decoder's reported limit"
                }
            }
            swRatio <= 1.0 -> {
                mode = HwAccelMode.DISABLE
                tier = if (swRatio > 0.6) SmartTier.DEMANDING else SmartTier.COMFORTABLE
                reason = "No hardware decoder for this codec - software decoding"
            }
            else -> {
                mode = HwAccelMode.DISABLE
                tier = SmartTier.BEYOND_DEVICE
                reason = "No hardware decoder for this codec and the CPU is not expected to keep up"
            }
        }

        val software = mode == HwAccelMode.DISABLE
        val heavy = tier != SmartTier.COMFORTABLE
        val veryHeavy = tier == SmartTier.BEYOND_DEVICE

        // Rendering: spline36 is also used when DOWN-scaling (dscale is unset), which is costly for 1440p / 4K frames
        // drawn on a phone screen. A cheaper down-scaler is used for big frames; quality loss is small on a phone.
        val dscale = when {
            veryHeavy || (heavy && pixels >= 2560L * 1440L) -> "bilinear"
            pixels >= 2560L * 1440L -> "hermite"
            else -> "spline36"
        }
        val scale = if (veryHeavy) "bilinear" else "spline36"

        // Buffering (local files only - the controller leaves online streams to their own buffer rules).
        val bitrate = stream.bitrateBps
        val readahead = when {
            bitrate >= 40_000_000L -> 12
            bitrate >= 15_000_000L -> 8
            else -> null
        }
        val maxMiB = if (readahead != null) {
            val wanted = (bitrate / 8.0 * readahead * 1.3 / (1024.0 * 1024.0)).toInt()
            wanted.coerceIn(32, (device.memoryClassMb / 3).coerceIn(48, 128))
        } else null

        val tuning = SmartTuning(
            scale = scale,
            dscale = dscale,
            framedrop = if (heavy && software) "decoder+vo" else "vo",
            autosync = if (heavy) 30 else 0,
            lavcFast = software && heavy,
            skipLoopFilter = when {
                software && veryHeavy -> "all"
                software && heavy -> "nonref"
                else -> "default"
            },
            lavcThreads = if (software) device.cpuCores.coerceIn(2, 8) else 4,
            readaheadSecs = readahead,
            demuxerMaxMiB = maxMiB
        )

        val suggested = if (veryHeavy) suggestHeight(stream, device) else null
        return SmartPlan(stream, mode, tier, tuning, suggested, hw.found, reason)
    }

    /** The largest standard height below the current one that the device is expected to handle, or null. */
    fun suggestHeight(stream: SmartStreamInfo, device: SmartDeviceProfile): Int? {
        for (h in STANDARD_HEIGHTS) {
            if (h >= stream.height) continue
            val w = (stream.width.toDouble() * h / stream.height).toInt().coerceAtLeast(2)
            val lower = stream.copy(width = w, height = h)
            if (hwHandles(lower) || softwareLoad(lower) / softwareBudget(device) <= 0.6) return h
        }
        return null
    }

    /** One step down for the "still dropping frames" advice: 4K -> 1080p, 1440p -> 1080p, 1080p -> 720p, 720p -> 480p. */
    fun stepDownHeight(currentHeight: Int): Int? = when {
        currentHeight >= 2000 -> 1080
        currentHeight >= 1300 -> 1080
        currentHeight >= 1000 -> 720
        currentHeight >= 700 -> 480
        else -> null
    }

    /** Next decoder mode to try when the current one failed to start. FORCE -> PREFER (copy) -> DISABLE (software). */
    fun nextCompatibleMode(current: HwAccelMode): HwAccelMode? = when (current) {
        HwAccelMode.FORCE -> HwAccelMode.PREFER
        HwAccelMode.PREFER -> HwAccelMode.DISABLE
        HwAccelMode.DISABLE -> null
    }
}

/** What Smart Auto Playback found and decided for the current video (shown to the user; never a promise of smoothness). */
data class SmartPlaybackReport(
    val summary: String,
    val tier: SmartTier,
    val note: String?
)

/** "This is too heavy for the device - a lower resolution is suggested." One per video at most. */
data class SmartLowerResolutionAdvice(
    val id: Int,
    val currentHeight: Int,
    val suggestedHeight: Int,
    val reason: String
)
