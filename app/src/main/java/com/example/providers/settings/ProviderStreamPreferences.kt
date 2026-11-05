package com.example.util.media

import android.content.Context
import com.example.player.AudioLanguageMatcher
import com.example.util.NetworkSettings
import java.util.Locale

/**
 * Settings > Network > Media settings, applied to the provider streams: the server / stream of the chosen language and
 * the stream of the chosen quality are tried first. Everything here is a pure ordering helper: with no language and no
 * quality set every score is 0, so the provider's own order is kept exactly as before.
 */
object MediaPreferences {
    private val QUALITY_RE = Regex("(?i)\\b(2160|1440|1080|720|576|480|360|240|144)\\s*p?\\b|\\b4k\\b")

    /** "1080p" / "1080" / "4K" / "HD 720p" -> 1080 / 1080 / 2160 / 720; 0 when the text names no quality. */
    fun heightOf(text: String?): Int {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return 0
        val m = QUALITY_RE.find(t) ?: return 0
        return if (m.value.equals("4k", ignoreCase = true)) 2160 else m.groupValues[1].toIntOrNull() ?: 0
    }

    private val aliasRegexCache = java.util.concurrent.ConcurrentHashMap<String, Regex>()
    private fun wholeWord(alias: String): Regex =
        aliasRegexCache.getOrPut(alias) { Regex("(?i)(^|[^\\p{L}\\p{N}])${Regex.escape(alias)}([^\\p{L}\\p{N}]|$)") }

    /** "Dual Audio" / "Multi Audio": one server that carries several languages, the player then selects the audio track itself. */
    private val MULTI_AUDIO_RE = Regex("(?i)\\b(dual|multi)[ _.-]?(audio|lang(uage)?s?)\\b")

    /** True when [text] names the language [token] (a code such as "hin", or its English / native name) as a whole word. */
    fun mentionsLanguage(text: String?, token: String): Boolean {
        val hay = text?.lowercase(Locale.ROOT).orEmpty()
        if (hay.isBlank() || token.isBlank()) return false
        // 2-letter codes are too ambiguous inside a server name ("it", "es", "de" ...): only names and 3-letter codes count here
        return AudioLanguageMatcher.aliasesOf(token).any { alias -> alias.length >= 3 && wholeWord(alias).containsMatchIn(hay) }
    }

    /** True when a Media preference is set at all (otherwise nothing is reordered). */
    fun active(context: Context): Boolean =
        NetworkSettings.mediaLanguage(context).isNotBlank() || NetworkSettings.mediaPlayQuality(context) > 0

    /** The server the user chose last (by hand in the player, or for a download) for this provider / series, if any. */
    fun stickyFor(context: Context, pluginId: String, scope: String = ""): ServerReputationStore.Choice? =
        if (pluginId.isBlank()) null else ServerReputationStore.lastChoice(context, pluginId, scope)

    /** How well the language of [s] fits the chosen language: own language 1000, a dual / multi audio server 400, another named language -300. */
    private fun languageScore(lang: String, s: MediaStream): Int {
        if (lang.isBlank()) return 0
        val path = s.link.substringBefore('?')
        val text = s.server + " " + s.quality.orEmpty()
        if (mentionsLanguage(text, lang) || mentionsLanguage(path, lang)) return 1000
        if (MULTI_AUDIO_RE.containsMatchIn(text) || MULTI_AUDIO_RE.containsMatchIn(path)) return 400
        // a server that names ANOTHER language (and not the chosen one) goes behind servers that name none
        val other = AudioLanguageMatcher.POPULAR_LANGUAGES.any { (code, _) ->
            !code.equals(lang, ignoreCase = true) && mentionsLanguage(text, code)
        }
        return if (other) -300 else 0
    }

    /**
     * Higher = better fit. Order of importance: the server the user chose last (1500), the language (1000), the quality
     * (closest to [wantOverride] / the play quality, 0..100). [qualityText] lets a caller pass a quality it read itself.
     */
    fun score(
        context: Context,
        s: MediaStream,
        sticky: ServerReputationStore.Choice? = null,
        wantOverride: Int = -1,
        qualityText: String? = s.quality
    ): Int {
        var score = languageScore(NetworkSettings.mediaLanguage(context), s)
        val want = if (wantOverride >= 0) wantOverride else NetworkSettings.mediaPlayQuality(context)
        val h = heightOf(qualityText)
        if (want > 0) {
            score += when {
                h <= 0 -> 40                                   // quality not stated: after an exact match, before a clearly different one
                else -> (100 - (kotlin.math.abs(h - want) / 20)).coerceIn(0, 100)
            }
        }
        if (sticky != null && s.server.trim().equals(sticky.server.trim(), ignoreCase = true)) {
            score += 1500
            val sh = heightOf(sticky.quality)
            if (sh > 0 && sh == h) score += 200
        }
        return score
    }

    /** [streams] reordered by [score]; equal scores keep their order (stable), so no setting = no change. */
    fun order(context: Context, streams: List<MediaStream>): List<MediaStream> = order(context, "", streams)

    /** Same, and the server the user chose last for this provider (episode after episode) comes first. */
    fun order(context: Context, pluginId: String, streams: List<MediaStream>, scope: String = ""): List<MediaStream> {
        val sticky = stickyFor(context, pluginId, scope)
        if (!active(context) && sticky == null) return streams
        return streams.sortedByDescending { score(context, it, sticky) }
    }

    /** The preferred language as the list of tokens the player's track matcher understands ("" = none). */
    fun playerLanguageTokens(context: Context): List<String> {
        val lang = NetworkSettings.mediaLanguage(context)
        return if (lang.isBlank()) emptyList() else listOf(lang)
    }
}
