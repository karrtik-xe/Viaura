package com.example.ui.screens

import com.example.ui.theme.Neutral

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DownloadGlassSheet
import com.example.ui.components.HighResImage
import com.example.ui.components.MangaReaderScreen
import com.example.ui.components.ViauraExpressiveLoadingIndicator
import com.example.ui.components.OnlineQuickActionBar
import com.example.ui.components.StyledIcon
import com.example.ui.components.VideoItemCard
import com.example.ui.components.VisibleFields
import com.example.ui.components.shareOnlineLink
import com.example.ui.components.swipeLeftForActions
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
import com.example.util.media.MediaDirectLink
import com.example.util.media.MediaInfo
import com.example.util.media.MediaLibraryStore
import com.example.util.media.MediaLink
import com.example.util.media.MediaPlayback
import com.example.util.media.MediaPluginInfo
import com.example.util.media.PluginException
import com.example.util.media.PluginPlaybackSession
import com.example.util.media.PluginQueuedEpisode
import com.example.util.media.PluginQueuedEpisodes
import com.example.util.media.PluginQualityChoice
import com.example.util.media.MediaPost
import com.example.util.media.MediaProviderRepository
import com.example.util.media.MediaSavedVideo
import com.example.util.media.MediaHistoryStore
import com.example.util.media.MediaWatchedTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class QualityOption(val label: String, val link: MediaLink)

internal data class AudioGroup(val label: String, val options: List<QualityOption>)

internal data class SeasonGroup(val label: String, val audios: List<AudioGroup>)

/** One movie / episode card of the detail page. */
internal data class DetailVideo(
    val title: String,
    val link: String,
    val type: String,
    val poster: String,
    val qualityLabel: String,
    /** Runtime / air date / score of the episode, as the provider gives them (CloudStream providers). */
    val meta: String = ""
)

private val SEASON_RE = Regex("(?i)\\bseason\\s*:?\\s*0*(\\d{1,2})\\b")

private val SHORT_SEASON_RE = Regex("(?i)\\bs0*(\\d{1,2})(?=\\s*e\\d|\\b)")

private val QUALITY_RE = Regex("(?i)\\b(2160p|1440p|1080p|720p|576p|480p|360p|4k)\\b")

private val SIZE_RE = Regex("(?i)\\b\\d+(?:\\.\\d+)?\\s?(?:GB|MB)\\b")

private val AUDIO_NOISE_RE = Regex(
    "(?i)(season\\s*:?\\s*0*\\d{1,2}|\\bs0*\\d{1,2}\\b|\\b(?:2160p|1440p|1080p|720p|576p|480p|360p|4k)\\b|" +
        "\\b\\d+(?:\\.\\d+)?\\s?(?:GB|MB)\\b|\\b(?:web-?dl|web-?rip|blu-?ray|hd-?rip|hevc|x26[45]|10-?bit)\\b)"
)

private val EMPTY_BRACKETS_RE = Regex("\\[\\s*]|\\(\\s*\\)")

private val SEPARATORS_RE = Regex("[\\s|:/,+\u2013\u00B7-]+")

internal fun seasonNumber(title: String): Int? =
    (SEASON_RE.find(title) ?: SHORT_SEASON_RE.find(title))?.groupValues?.get(1)?.toIntOrNull()

/** The audio (language) of a quality option: its title without the season, quality and size words. */
private fun audioLabel(link: MediaLink): String {
    val cleaned = link.title
        .replace(AUDIO_NOISE_RE, " ")
        .replace(EMPTY_BRACKETS_RE, " ")
        .replace(SEPARATORS_RE, " ")
        .trim()
    // a label with no letter or digit at all (only an emoji / symbol) is not an audio name
    return if (cleaned.none { it.isLetterOrDigit() }) "Default" else cleaned
}

/** The quality (and size) of a quality option, exactly as the plugin offers it. */
internal fun qualityLabel(link: MediaLink): String {
    val q = link.quality?.takeIf { it.isNotBlank() } ?: QUALITY_RE.find(link.title)?.value.orEmpty()
    val size = SIZE_RE.find(link.title)?.value?.trim()
    val label = listOfNotNull(q.takeIf { it.isNotBlank() }, size?.let { "[$it]" }).joinToString(" ")
    return label.ifBlank { "Default" }
}

/** Settings > Network > Media settings: the audio (language) group that matches the preferred language; 0 when none does. */
internal fun preferredAudioIndex(context: android.content.Context, season: SeasonGroup?): Int {
    val lang = com.example.util.NetworkSettings.mediaLanguage(context)
    if (lang.isBlank() || season == null) return 0
    return season.audios.indexOfFirst { com.example.util.media.MediaPreferences.mentionsLanguage(it.label, lang) || labelHasShortCode(it.label, lang) }
        .takeIf { it >= 0 } ?: 0
}

/** Short codes ("hi", "en") are only accepted as a whole word of an audio label, e.g. "[Hi-En]" or "Hin + Eng". */
private fun labelHasShortCode(label: String, token: String): Boolean =
    com.example.player.AudioLanguageMatcher.aliasesOf(token).any { alias ->
        alias.length == 2 && Regex("(?i)(^|[^\\p{L}\\p{N}])${Regex.escape(alias)}([^\\p{L}\\p{N}]|$)").containsMatchIn(label)
    }

/** Settings > Network > Media settings: the quality option closest to the preferred quality; 0 (the first) when no quality is set. */
internal fun preferredQualityIndex(context: android.content.Context, audio: AudioGroup?): Int {
    val want = com.example.util.NetworkSettings.mediaPlayQuality(context)
    if (want <= 0 || audio == null) return 0
    var best = -1
    var bestDistance = Int.MAX_VALUE
    audio.options.forEachIndexed { i, opt ->
        val h = com.example.util.media.MediaPreferences.heightOf(opt.label)
        if (h > 0) {
            val d = kotlin.math.abs(h - want)
            if (d < bestDistance) { bestDistance = d; best = i }
        }
    }
    return if (best >= 0) best else 0
}

/** Groups the plugin's list as season -> audio -> quality, so the page can show "audio + quality" pickers. */
internal fun buildGroups(info: MediaInfo): List<SeasonGroup> {
    val bySeason = LinkedHashMap<Int?, LinkedHashMap<String, MutableList<QualityOption>>>()
    for (link in info.linkList) {
        val audios = bySeason.getOrPut(seasonNumber(link.title)) { LinkedHashMap() }
        audios.getOrPut(audioLabel(link)) { ArrayList() }.add(QualityOption(qualityLabel(link), link))
    }
    val hasSeasons = bySeason.keys.any { it != null }
    val keys = bySeason.keys.sortedWith(compareBy<Int?> { it == null }.thenBy { it ?: 0 })
    return keys.map { key ->
        val audios = bySeason.getValue(key).map { (label, raw) ->
            val seen = HashMap<String, Int>()
            AudioGroup(
                label = label,
                options = raw.map { opt ->
                    val n = (seen[opt.label] ?: 0) + 1
                    seen[opt.label] = n
                    if (n > 1) opt.copy(label = "${opt.label} #$n") else opt
                }
            )
        }
        SeasonGroup(
            label = when {
                key != null -> "Season $key"
                hasSeasons -> "Other"
                else -> ""
            },
            audios = audios
        )
    }
}
