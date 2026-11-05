package com.example.ui.screens

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.io.File
import java.util.Locale

// ==========================================
// SUBTITLE DATA MODELS
// ==========================================

data class SubtitleCue(
    val id: String = java.util.UUID.randomUUID().toString(),
    val index: Int = 1,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 1000L,
    val text: String = "",
    val style: String = "Default",
    val actor: String = "",
    val layer: Int = 0,
    val marginL: Int = 0,
    val marginR: Int = 0,
    val marginV: Int = 0,
    val effect: String = "",
    val overrideTags: String = "",
    val rawText: String = "",
    val fontSize: Int = 18,
    val alignment: Int = 2,
    val fontName: String = "Arial",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikeout: Boolean = false,
    val textColorHex: String? = null,
    val outlineColorHex: String? = null,
    val backColorHex: String? = null
)

enum class SubtitleFormat(
    val displayName: String,
    val extension: String
) {
    SRT("SubRip (.srt)", "srt"),
    ASS("Advanced SubStation Alpha (.ass)", "ass"),
    SSA("SubStation Alpha (.ssa)", "ssa"),
    VTT("WebVTT (.vtt)", "vtt"),
    STL("Spruce STL (.stl)", "stl"),
    TTML("TTML (.ttml)", "ttml"),
    SUB("MicroDVD (.sub)", "sub"),
    SMI("SAMI (.smi)", "smi"),
    LRC("Lyrics (.lrc)", "lrc"),
    TXT("Plain Text (.txt)", "txt"),
    SBV("YouTube SBV (.sbv)", "sbv"),
    DFXP("DFXP / TTML (.dfxp)", "dfxp"),
    CLEAN_TEXT("Clean Text Only", "txt");

    companion object {
        fun fromFileName(fileName: String): SubtitleFormat {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == ext } ?: SRT
        }

        fun fromExtension(ext: String): SubtitleFormat? {
            val cleanExt = ext.removePrefix(".").lowercase()
            return entries.firstOrNull { it.extension == cleanExt }
        }
    }
}
