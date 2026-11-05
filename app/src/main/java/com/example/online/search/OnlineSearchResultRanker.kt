package com.example.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.min

/**
 * Orders Online Video / Online Audio search results so that the thing the user typed comes first:
 * the original upload (official channel, the most played one) goes above covers, remixes, slowed/reverb versions,
 * reaction videos, clips and long mixes. YouTube's own order is only a small tie-breaker, so nothing "slips in" between
 * the real result and the user's query.
 *
 * Pure (no Android / network), so it is unit-tested.
 */
object OnlineSearchRanker {

    private val STOP_WORDS = setOf(
        "official", "video", "videos", "song", "songs", "full", "hd", "4k", "lyrics", "lyrical", "audio", "new",
        "latest", "the", "and", "feat", "ft", "music", "movie", "a", "of", "in", "to"
    )

    /** Versions that are not the original; they are pushed down unless the user typed that word himself. */
    private val NOT_ORIGINAL = listOf(
        "cover", "remix", "slowed", "reverb", "sped up", "speed up", "nightcore", "karaoke", "8d", "bass boosted",
        "reaction", "reacts", "tutorial", "mashup", "tribute", "status", "whatsapp", "ringtone", "parody", "fan made",
        "fanmade", "jukebox", "nonstop", "non stop", "mix", "megamix", "compilation", "edit", "shorts", "dj", "trending reels"
    )

    private val OFFICIAL_CHANNEL_HINTS = listOf(
        "vevo", "- topic", "official", "t-series", "tseries", "sony music", "zee music", "tips", "saregama", "speed records",
        "universal music", "warner", "atlantic records", "def jam", "columbia", "t series", "yrf", "eros now", "times music",
        "white hill", "desi melodies", "lahari", "venus", "gaana", "jiosaavn", "t-series apna punjab"
    )

    private val boundaryCache = HashMap<String, Regex>()

    private fun phraseRegex(phrase: String): Regex = synchronized(boundaryCache) {
        boundaryCache.getOrPut(phrase) {
            Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(phrase) + "(?![\\p{L}\\p{N}])")
        }
    }

    private fun normalize(text: String): String =
        text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private fun tokens(text: String): List<String> =
        normalize(text).split(' ').filter { it.isNotEmpty() }

    private fun significant(tokens: List<String>): List<String> {
        val kept = tokens.filter { it.length >= 2 && it !in STOP_WORDS }
        return kept.ifEmpty { tokens.filter { it.isNotEmpty() } }
    }

    /** Higher = closer to what the user typed and more likely the original upload. */
    internal fun score(query: String, item: OnlineTrackResult, position: Int): Double {
        val q = normalize(query)
        val qTokens = significant(tokens(query))
        if (q.isEmpty() || qTokens.isEmpty()) return 0.0

        val title = normalize(item.title)
        val artist = normalize(item.artist)
        val titleTokens = tokens(item.title).toSet()
        val artistTokens = tokens(item.artist).toSet()

        val matched = qTokens.count { t ->
            t in titleTokens || t in artistTokens ||
                (t.length >= 4 && (title.contains(t) || artist.contains(t)))
        }
        val coverage = matched.toDouble() / qTokens.size
        var s = 60.0 * coverage

        // the whole query as one phrase in the title (or in "title + channel") means this is really the thing searched for
        if (title.contains(q)) s += 25.0
        else if ("$artist $title".contains(q) || "$title $artist".contains(q)) s += 12.0

        // official / label / "- Topic" channel, or the channel is the very artist named in the query
        val channelRaw = item.artist.lowercase(Locale.ROOT)
        if (OFFICIAL_CHANNEL_HINTS.any { channelRaw.contains(it) }) s += 12.0
        if (artistTokens.isNotEmpty() && artistTokens.any { it in qTokens }) s += 8.0

        // the original is the most played one
        if (item.viewCount > 0L) s += min(45.0, ln(item.viewCount.toDouble() + 1.0) / ln(10.0) * 5.0)

        // covers / remixes / slowed / reaction / mixes: not the original unless the user asked for them
        var penalty = 0
        for (word in NOT_ORIGINAL) {
            if (phraseRegex(word).containsMatchIn(title) && !phraseRegex(word).containsMatchIn(q)) penalty++
        }
        s -= 35.0 * min(penalty, 2)

        // clips / shorts
        if (item.durationSec in 1L..44L) s -= 30.0

        // YouTube's own order only breaks ties
        s += 6.0 / (1.0 + position)
        return s
    }

    /** [items] re-ordered best-first; a stable sort, so equal scores keep YouTube's order. */
    fun rank(query: String, items: List<OnlineTrackResult>): List<OnlineTrackResult> {
        if (items.size < 2 || query.isBlank()) return items
        return items.withIndex()
            .map { (i, item) -> item to score(query, item, i) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Channel cards are only kept when the channel really is what was searched (its name shares a word with the query).
     * A song search therefore starts with the song, not with unrelated channel cards.
     */
    fun relevantChannels(query: String, channels: List<OnlineChannelResult>): List<OnlineChannelResult> {
        val qTokens = significant(tokens(query))
        if (qTokens.isEmpty()) return channels
        return channels.filter { ch ->
            val name = significant(tokens(ch.name))
            name.isNotEmpty() && name.any { n -> qTokens.any { q -> q == n || (q.length >= 4 && n.length >= 4 && (n.startsWith(q) || q.startsWith(n))) } }
        }.take(2)
    }
}
