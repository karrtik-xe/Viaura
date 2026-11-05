package com.example.ui.state

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.example.player.AudioChannelMode
import com.example.player.AudioLanguageMatcher
import com.example.ui.components.LayoutMode
import com.example.ui.components.SortDirection
import com.example.ui.components.SortField
import com.example.ui.components.ViewMode
import com.example.ui.components.VisibleFields
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

/** Custom (user edited) gradients per theme id, stored as "id=hex,hex,hex;id2=hex,hex,hex". */
fun encodeCustomThemeGradients(map: Map<String, List<Long>>): String =
    map.entries.joinToString(";") { (id, cols) -> id + "=" + cols.joinToString(",") { java.lang.Long.toHexString(it and 0xFFFFFFFFL) } }

fun decodeCustomThemeGradients(raw: String?): Map<String, List<Long>> {
    if (raw.isNullOrBlank()) return emptyMap()
    val out = LinkedHashMap<String, List<Long>>()
    raw.split(";").forEach { entry ->
        val idx = entry.indexOf('=')
        if (idx <= 0) return@forEach
        val cols = entry.substring(idx + 1).split(",").mapNotNull { it.trim().toLongOrNull(16)?.let { v -> 0xFF000000L or (v and 0xFFFFFFL) } }
        if (cols.size in 2..3) out[entry.substring(0, idx)] = cols
    }
    return out
}

/**
 * App themes. DYNAMIC (Sky Blue -> Pink) is the only theme; its gradient ([gradientHex] = 2 or 3 colours, first = primary accent,
 * last = secondary accent) can be edited by the user.
 */
enum class AppThemeVariant(
    val id: String,
    val displayName: String,
    val description: String,
    val gradientHex: List<Long>,
    val lightBgHex: Long = 0xFFF1F5F9,
    val darkBgHex: Long = 0xFF090D16,
    val lightCardHex: Long = 0xFFFFFFFF,
    val darkCardHex: Long = 0xFF111827,
    val lightBorderHex: Long = 0xFFE2E8F0,
    val darkBorderHex: Long = 0xFF1F2937
) {
    DYNAMIC("dynamic", "Dynamic", "Material You Sky Blue to Pink gradient", listOf(0xFF38BDF8, 0xFFEC4899), 0xFFF1F5F9, 0xFF090D16, 0xFFFFFFFF, 0xFF111827, 0xFFE2E8F0, 0xFF1F2937);

    val primaryColorHex: Long get() = gradientHex.first()
    val secondaryColorHex: Long get() = gradientHex.last()
    val primaryColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(primaryColorHex)
    val secondaryColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(secondaryColorHex)
    val gradientColors: List<androidx.compose.ui.graphics.Color> get() = gradientHex.map { androidx.compose.ui.graphics.Color(it) }
    val lightBgColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(lightBgHex)
    val darkBgColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(darkBgHex)
    val lightCardColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(lightCardHex)
    val darkCardColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(darkCardHex)
    val lightBorderColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(lightBorderHex)
    val darkBorderColor: androidx.compose.ui.graphics.Color get() = androidx.compose.ui.graphics.Color(darkBorderHex)
    val gradient: androidx.compose.ui.graphics.Brush get() = androidx.compose.ui.graphics.Brush.linearGradient(gradientColors)
}

enum class ThumbnailStrategy(val displayName: String, val description: String) {
    SMART("Smart", "Samples the video itself and chooses a clear representative frame"),
    EMBEDDED_AND_GENERATED("Embedded + Generated", "Uses an embedded MKV image attachment first; generates a frame only when none is attached")
}

enum class ThumbnailQuality(val displayName: String, val description: String, val sizeDp: Int) {
    LOW("Low", "Fast loading low-res preview (120px)", 120),
    MEDIUM("Medium", "Balanced standard quality (240px)", 240),
    HIGH("High", "Crisp detailed rendering (480px)", 480),
    MAXIMUM("Maximum", "Largest supported thumbnail cache size (720px)", 720)
}

enum class ControlsAnimationStyle(val displayName: String, val description: String) {
    DEFAULT("Default", "Smooth standard fade and slide overlay"),
    CINEMATIC_SCALE("Cinematic Scale", "Subtle zoom and cinematic depth fade"),
    FLUID_EXPAND("Fluid Expand", "Elastic outward expansion from screen center"),
    GENTLE_BOUNCE("Gentle Bounce", "Playful spring bounce on appear/dismiss"),
    MINIMAL_FADE("Minimal Fade", "Instant crisp linear opacity dissolve"),
    NONE("None", "Immediate snap display without motion")
}

enum class VideoOpeningAnimation(val displayName: String, val description: String) {
    DEFAULT("Default", "Standard bottom sheet upwards slide"),
    FADE_FROM_BLACK("Fade from Black", "Theatrical cinematic blackout dissolve"),
    ZOOM_BURST("Zoom Burst", "Expands directly from tapped video thumbnail"),
    SLIDE_UP("Slide Up", "Clean vertical glide covering previous screen"),
    CINEMA_BARS("Cinema Bars", "Aspect ratio letterbox wipe transition"),
    NONE("None", "Instant player launch without delay")
}

enum class ScreenNavigationStyle(val displayName: String, val description: String) {
    DEFAULT("Default", "Standard horizontal slide transition"),
    ELASTIC_SLIDE("Elastic Slide", "Bouncy damped spring horizontal slide"),
    DEPTH_ZOOM("Depth Zoom", "Parallax background depth push effect"),
    FLIP_FADE("Flip Fade", "Subtle 3D card tilt and fade cross dissolve"),
    MINIMAL_FADE("Minimal Fade", "Linear alpha crossfade between views"),
    NONE("None", "Instant view change")
}

enum class TabNavigationStyle(val displayName: String, val description: String) {
    DEFAULT("Default", "Smooth sliding underline and subtle slide"),
    ELASTIC_SLIDE("Elastic Slide", "Dynamic spring pill indicator transition"),
    DEPTH_ZOOM("Depth Zoom", "Scale pulse on tab switch with blur dissolve"),
    FLIP_FADE("Flip Fade", "Quick cross-fade with directional glide"),
    MINIMAL_FADE("Minimal Fade", "Clean instantaneous opacity crossfade"),
    NONE("None", "Direct tab switch without animation")
}

enum class AppLanguage {
    ENGLISH, HINGLISH, HINDI
}

enum class HwAccelMode(val displayName: String, val mpvValue: String, val description: String) {
    FORCE("Force MediaCodec", "mediacodec", "Direct MediaCodec HW acceleration for max frame sync & smoothness"),
    PREFER("Prefer (Auto Copy)", "mediacodec-copy", "Hardware decoding with auto copyback buffer fallback"),
    DISABLE("Disable (Software CPU)", "no", "Software CPU decoding to troubleshoot persistent GPU/rendering glitches");

    fun getDisplayName(lang: AppLanguage): String = when (this) {
        FORCE -> AppStrings.getHwForceTitle(lang)
        PREFER -> AppStrings.getHwPreferTitle(lang)
        DISABLE -> AppStrings.getHwDisableTitle(lang)
    }

    fun getDescription(lang: AppLanguage): String = when (this) {
        FORCE -> AppStrings.getHwForceDesc(lang)
        PREFER -> AppStrings.getHwPreferDesc(lang)
        DISABLE -> AppStrings.getHwDisableDesc(lang)
    }
}

enum class TextEditorNavStep {
    CHARACTER, WORD
}

enum class SeekbarStyle(val displayName: String, val description: String) {
    NORMAL(
        displayName = "Normal",
        description = "Classic sleek line with circular thumb knob"
    ),
    STANDARD(
        displayName = "Standard",
        description = "Medium rounded bar with vertical dividing pill"
    ),
    WAVY(
        displayName = "Wavy",
        description = "Dynamic wavy sine curve with dividing pin"
    ),
    THICK(
        displayName = "Thick",
        description = "Bold thick rounded pill bar with vertical dividing pin"
    ),
    SLIM(
        displayName = "Slim",
        description = "Clean continuous flat pill track without thumb"
    )
}

enum class GestureSensitivityMode(val displayName: String, val shortLabel: String, val description: String) {
    LINEAR(
        displayName = "Linear (1:1 Uniform)",
        shortLabel = "Linear",
        description = "Direct 1:1 proportional swipe response for uniform volume & brightness steps"
    ),
    EXPONENTIAL(
        displayName = "Exponential (Perceptual Curve)",
        shortLabel = "Exponential",
        description = "Fine-grain precision at lower levels with smooth dynamic curve to prevent lag & jumps"
    );

    fun getDisplayName(lang: AppLanguage): String = when (this) {
        LINEAR -> AppStrings.getGestureLinearTitle(lang)
        EXPONENTIAL -> AppStrings.getGestureExponentialTitle(lang)
    }

    fun getDescription(lang: AppLanguage): String = when (this) {
        LINEAR -> AppStrings.getGestureLinearDesc(lang)
        EXPONENTIAL -> AppStrings.getGestureExponentialDesc(lang)
    }
}

/**
 * Muted colour picked with the circle picker (double tap on "System"). [hue] 0..360, [sat] muted range 0.18..0.60,
 * [depth] 0..1 (0.5 = default tint strength). null in UiState = use the wallpaper (Material You) colour.
 */
data class SystemTone(val hue: Float, val sat: Float, val depth: Float = 0.5f) {
    fun encode(): String = "$hue,$sat,$depth"

    companion object {
        /** The mustard the picker opens with. */
        val Mustard = SystemTone(hue = 45f, sat = 0.45f, depth = 0.5f)

        fun decode(raw: String?): SystemTone? {
            val v = raw?.split(",")?.mapNotNull { it.trim().toFloatOrNull() } ?: return null
            if (v.size != 3 || v.any { !it.isFinite() }) return null
            return SystemTone(v[0].mod(360f), v[1].coerceIn(0f, 0.60f), v[2].coerceIn(0f, 1f))
        }
    }
}
