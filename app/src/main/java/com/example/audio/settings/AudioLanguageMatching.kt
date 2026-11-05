package com.example.player

import android.content.Context
import com.example.ui.state.AppLanguage
import java.io.File
import java.util.Locale

/**
 * Intelligent Language normalization and matching engine.
 * Handles ISO 639-1 (2-letter), ISO 639-2/3 (3-letter), regional locales, full names,
 * and track title heuristics safely with case-insensitivity and zero crashes.
 */
object AudioLanguageMatcher {

    val POPULAR_LANGUAGES = listOf(
        "hin" to "Hindi (हिन्दी)",
        "eng" to "English",
        "jpn" to "Japanese (日本語)",
        "kor" to "Korean (한국어)",
        "spa" to "Spanish (Español)",
        "fra" to "French (Français)",
        "deu" to "German (Deutsch)",
        "tam" to "Tamil (தமிழ்)",
        "tel" to "Telugu (తెలుగు)",
        "ben" to "Bengali (বাংলা)",
        "pan" to "Punjabi (ਪੰਜਾਬੀ)",
        "mar" to "Marathi (मराठी)",
        "guj" to "Gujarati (ગુજરાતી)",
        "kan" to "Kannada (ಕನ್ನಡ)",
        "mal" to "Malayalam (മലയാളം)",
        "rus" to "Russian (Русский)",
        "ara" to "Arabic (العربية)",
        "zho" to "Chinese (中文)",
        "por" to "Portuguese (Português)",
        "ita" to "Italian (Italiano)",
        "urd" to "Urdu (اردو)",
        "tur" to "Turkish (Türkçe)",
        "vie" to "Vietnamese (Tiếng Việt)",
        "ind" to "Indonesian (Bahasa)"
    )

    private val LANGUAGE_ALIASES: Map<String, Set<String>> = mapOf(
        // Hindi
        "hin" to setOf("hin", "hi", "hi-in", "hin-in", "hindi", "hindustani", "हिन्दी"),
        "hi" to setOf("hin", "hi", "hi-in", "hin-in", "hindi", "hindustani", "हिन्दी"),
        "hindi" to setOf("hin", "hi", "hi-in", "hin-in", "hindi", "hindustani", "हिन्दी"),

        // English
        "eng" to setOf("eng", "en", "en-us", "en-gb", "en-ca", "en-au", "en-in", "english"),
        "en" to setOf("eng", "en", "en-us", "en-gb", "en-ca", "en-au", "en-in", "english"),
        "english" to setOf("eng", "en", "en-us", "en-gb", "en-ca", "en-au", "en-in", "english"),

        // Japanese
        "jpn" to setOf("jpn", "ja", "ja-jp", "japanese", "jap", "nihongo", "日本語"),
        "ja" to setOf("jpn", "ja", "ja-jp", "japanese", "jap", "nihongo", "日本語"),
        "japanese" to setOf("jpn", "ja", "ja-jp", "japanese", "jap", "nihongo", "日本語"),

        // Korean
        "kor" to setOf("kor", "ko", "ko-kr", "korean", "hangul", "한국어"),
        "ko" to setOf("kor", "ko", "ko-kr", "korean", "hangul", "한국어"),
        "korean" to setOf("kor", "ko", "ko-kr", "korean", "hangul", "한국어"),

        // Spanish
        "spa" to setOf("spa", "es", "es-es", "es-mx", "es-419", "es-la", "spanish", "español", "castellano"),
        "es" to setOf("spa", "es", "es-es", "es-mx", "es-419", "es-la", "spanish", "español", "castellano"),
        "spanish" to setOf("spa", "es", "es-es", "es-mx", "es-419", "es-la", "spanish", "español", "castellano"),

        // French
        "fra" to setOf("fra", "fre", "fr", "fr-fr", "fr-ca", "french", "français"),
        "fre" to setOf("fra", "fre", "fr", "fr-fr", "fr-ca", "french", "français"),
        "fr" to setOf("fra", "fre", "fr", "fr-fr", "fr-ca", "french", "français"),
        "french" to setOf("fra", "fre", "fr", "fr-fr", "fr-ca", "french", "français"),

        // German
        "deu" to setOf("deu", "ger", "de", "de-de", "german", "deutsch"),
        "ger" to setOf("deu", "ger", "de", "de-de", "german", "deutsch"),
        "de" to setOf("deu", "ger", "de", "de-de", "german", "deutsch"),
        "german" to setOf("deu", "ger", "de", "de-de", "german", "deutsch"),

        // Tamil
        "tam" to setOf("tam", "ta", "ta-in", "tamil", "தமிழ்"),
        "ta" to setOf("tam", "ta", "ta-in", "tamil", "தமிழ்"),
        "tamil" to setOf("tam", "ta", "ta-in", "tamil", "தமிழ்"),

        // Telugu
        "tel" to setOf("tel", "te", "te-in", "telugu", "తెలుగు"),
        "te" to setOf("tel", "te", "te-in", "telugu", "తెలుగు"),
        "telugu" to setOf("tel", "te", "te-in", "telugu", "తెలుగు"),

        // Bengali
        "ben" to setOf("ben", "bn", "bn-in", "bn-bd", "bengali", "bangla", "বাংলা"),
        "bn" to setOf("ben", "bn", "bn-in", "bn-bd", "bengali", "bangla", "বাংলা"),
        "bengali" to setOf("ben", "bn", "bn-in", "bn-bd", "bengali", "bangla", "বাংলা"),

        // Punjabi
        "pan" to setOf("pan", "pa", "pa-in", "punjabi", "ਪੰਜਾਬੀ"),
        "pa" to setOf("pan", "pa", "pa-in", "punjabi", "ਪੰਜਾਬੀ"),
        "punjabi" to setOf("pan", "pa", "pa-in", "punjabi", "ਪੰਜਾਬੀ"),

        // Marathi
        "mar" to setOf("mar", "mr", "mr-in", "marathi", "मराठी"),
        "mr" to setOf("mar", "mr", "mr-in", "marathi", "मराठी"),
        "marathi" to setOf("mar", "mr", "mr-in", "marathi", "मराठी"),

        // Gujarati
        "guj" to setOf("guj", "gu", "gu-in", "gujarati", "ગુજરાતી"),
        "gu" to setOf("guj", "gu", "gu-in", "gujarati", "ગુજરાતી"),
        "gujarati" to setOf("guj", "gu", "gu-in", "gujarati", "ગુજરાતી"),

        // Kannada
        "kan" to setOf("kan", "kn", "kn-in", "kannada", "ಕನ್ನಡ"),
        "kn" to setOf("kan", "kn", "kn-in", "kannada", "ಕನ್ನಡ"),
        "kannada" to setOf("kan", "kn", "kn-in", "kannada", "ಕನ್ನಡ"),

        // Malayalam
        "mal" to setOf("mal", "ml", "ml-in", "malayalam", "മലയാളം"),
        "ml" to setOf("mal", "ml", "ml-in", "malayalam", "മലയാളം"),
        "malayalam" to setOf("mal", "ml", "ml-in", "malayalam", "മലയാളം"),

        // Russian
        "rus" to setOf("rus", "ru", "ru-ru", "russian", "русский"),
        "ru" to setOf("rus", "ru", "ru-ru", "russian", "русский"),
        "russian" to setOf("rus", "ru", "ru-ru", "russian", "русский"),

        // Arabic
        "ara" to setOf("ara", "ar", "ar-sa", "ar-ae", "arabic", "العربية"),
        "ar" to setOf("ara", "ar", "ar-sa", "ar-ae", "arabic", "العربية"),
        "arabic" to setOf("ara", "ar", "ar-sa", "ar-ae", "arabic", "العربية"),

        // Chinese
        "zho" to setOf("zho", "chi", "zh", "zh-cn", "zh-tw", "zh-hk", "chinese", "mandarin", "cantonese", "中文"),
        "chi" to setOf("zho", "chi", "zh", "zh-cn", "zh-tw", "zh-hk", "chinese", "mandarin", "cantonese", "中文"),
        "zh" to setOf("zho", "chi", "zh", "zh-cn", "zh-tw", "zh-hk", "chinese", "mandarin", "cantonese", "中文"),
        "chinese" to setOf("zho", "chi", "zh", "zh-cn", "zh-tw", "zh-hk", "chinese", "mandarin", "cantonese", "中文"),

        // Portuguese
        "por" to setOf("por", "pt", "pt-br", "pt-pt", "portuguese", "português"),
        "pt" to setOf("por", "pt", "pt-br", "pt-pt", "portuguese", "português"),
        "portuguese" to setOf("por", "pt", "pt-br", "pt-pt", "portuguese", "português"),

        // Italian
        "ita" to setOf("ita", "it", "it-it", "italian", "italiano"),
        "it" to setOf("ita", "it", "it-it", "italian", "italiano"),
        "italian" to setOf("ita", "it", "it-it", "italian", "italiano"),

        // Urdu
        "urd" to setOf("urd", "ur", "ur-pk", "ur-in", "urdu", "اردو"),
        "ur" to setOf("urd", "ur", "ur-pk", "ur-in", "urdu", "اردو"),
        "urdu" to setOf("urd", "ur", "ur-pk", "ur-in", "urdu", "اردو"),

        // Turkish
        "tur" to setOf("tur", "tr", "turkish", "türkçe"),
        "tr" to setOf("tur", "tr", "turkish", "türkçe"),
        "turkish" to setOf("tur", "tr", "turkish", "türkçe")
    )

    /** Every spelling (codes, names) known for [token]; just the token itself for an unknown language. */
    fun aliasesOf(token: String): Set<String> {
        val t = token.trim().lowercase(Locale.ROOT)
        if (t.isBlank()) return emptySet()
        return LANGUAGE_ALIASES[t] ?: setOf(t)
    }

    /** Name shown for a language code in the settings ("hin" -> "Hindi (हिन्दी)"), the code itself when unknown. */
    fun displayName(token: String): String =
        POPULAR_LANGUAGES.firstOrNull { it.first.equals(token, ignoreCase = true) }?.second ?: token

    /**
     * Splits comma-separated input, trims whitespace, drops empties, and converts to lowercase.
     * Example: " hin , eng, JPN , , spa " -> ["hin", "eng", "jpn", "spa"]
     */
    fun normalizePreferredLanguages(rawInput: String): List<String> {
        if (rawInput.isBlank()) return emptyList()
        return rawInput.split(',', ';', '|', '/', ' ')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * Checks if a PlayerMediaTrack matches a preferred language token.
     */
    fun doesTrackMatchLanguage(track: PlayerMediaTrack, preferredToken: String): Boolean {
        val token = preferredToken.trim().lowercase(Locale.ROOT)
        if (token.isBlank()) return false

        val aliases = LANGUAGE_ALIASES[token] ?: setOf(token)
        val trackLang = track.language.trim().lowercase(Locale.ROOT)
        val trackTitle = track.title.trim().lowercase(Locale.ROOT)
        val trackFilename = track.originalFilename.trim().lowercase(Locale.ROOT)

        // 1. Direct language code or alias match
        if (trackLang.isNotBlank()) {
            if (aliases.contains(trackLang)) return true
            if (trackLang.startsWith("$token-") || trackLang.startsWith("${token}_")) return true
            // Inverse check if track language has aliases
            val trackLangAliases = LANGUAGE_ALIASES[trackLang]
            if (trackLangAliases != null && (trackLangAliases.contains(token) || trackLangAliases.any { aliases.contains(it) })) {
                return true
            }
        }

        // 2. Title token matching (e.g. "Hindi [Audio 1]", "English Stereo", "[JPN]", "Hindi")
        for (alias in aliases) {
            if (alias.length >= 2) {
                // Word boundary or bracket check
                val regex = Regex("(?i)(^|[^a-zA-Z0-9])${Regex.escape(alias)}([^a-zA-Z0-9]|\$)")
                if (regex.containsMatchIn(trackTitle) || regex.containsMatchIn(trackFilename)) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Finds the best audio track based on strict user-defined priority:
     * 1. Explicit current-session manual selection (if user explicitly switched track during current playback).
     * 2. Selected folder/video restriction (if enabled and video belongs to selected paths).
     * 3. Remembered audio track (if "Remember selected audio track" is enabled and a saved selection exists).
     * 4. Global preferred languages (evaluating tokens in priority order 0..N).
     * 5. Default track / first audio track.
     */
    fun findBestAudioTrack(
        tracks: List<PlayerMediaTrack>,
        videoPath: String = "",
        preferredLanguages: String = "",
        applyToSelectedContentOnly: Boolean = false,
        selectedFolders: Set<String> = emptySet(),
        selectedVideos: Set<String> = emptySet(),
        rememberSelectedTrack: Boolean = false,
        rememberedTrackId: Int? = null,
        rememberedTrackTitle: String? = null,
        isCurrentSessionExplicit: Boolean = false,
        currentSessionTrackId: Int = -1
    ): PlayerMediaTrack? {
        if (tracks.isEmpty()) return null

        // 1. Explicit manual track selection in current session takes absolute top precedence
        if (isCurrentSessionExplicit && currentSessionTrackId > 0) {
            tracks.firstOrNull { it.id == currentSessionTrackId }?.let { return it }
        }

        // Determine if preferred languages should be evaluated for this video
        val isVideoInSelectedScope = if (applyToSelectedContentOnly && videoPath.isNotBlank()) {
            val file = File(videoPath)
            val parentPath = file.parentFile?.absolutePath ?: ""
            val matchesFolder = selectedFolders.any { sf ->
                sf.isNotBlank() && (videoPath.startsWith(sf) || parentPath.equals(sf, ignoreCase = true))
            }
            val matchesVideo = selectedVideos.any { sv ->
                sv.isNotBlank() && (sv.equals(videoPath, ignoreCase = true) || sv.equals(file.name, ignoreCase = true))
            }
            matchesFolder || matchesVideo
        } else {
            true
        }

        val tokens = normalizePreferredLanguages(preferredLanguages)

        // 2. If video is within preferred language scope, evaluate preferred language list in priority order
        if (isVideoInSelectedScope && tokens.isNotEmpty()) {
            for (token in tokens) {
                val matched = tracks.firstOrNull { doesTrackMatchLanguage(it, token) }
                if (matched != null) {
                    return matched
                }
            }
        }

        // 3. Remembered track preference (if enabled and present)
        if (rememberSelectedTrack) {
            if (rememberedTrackId != null && rememberedTrackId > 0) {
                tracks.firstOrNull { it.id == rememberedTrackId }?.let { return it }
            }
            if (!rememberedTrackTitle.isNullOrBlank()) {
                tracks.firstOrNull { it.title.equals(rememberedTrackTitle, ignoreCase = true) }?.let { return it }
            }
        }

        // 4. Default: track marked selected, or first track
        return tracks.firstOrNull { it.isSelected } ?: tracks.firstOrNull()
    }

    /**
     * Simplified helper finding the best audio track by preferred language tokens.
     */
    fun findBestAudioTrack(
        tracks: List<PlayerMediaTrack>,
        preferredLanguages: String
    ): PlayerMediaTrack? {
        return findBestAudioTrack(
            tracks = tracks,
            videoPath = "",
            preferredLanguages = preferredLanguages,
            applyToSelectedContentOnly = false,
            selectedFolders = emptySet(),
            selectedVideos = emptySet(),
            rememberSelectedTrack = false,
            rememberedTrackId = null,
            rememberedTrackTitle = null,
            isCurrentSessionExplicit = false,
            currentSessionTrackId = -1
        )
    }
}
