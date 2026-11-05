package com.example.util

import java.util.Locale

/**
 * Order of the "next tracks" of Online Audio / Online Video.
 *
 * The thing that is playing now has a creator (artist / channel). The next items are:
 *  - from OTHER creators of the same kind of content (a cricket video is followed by another cricketer, a song by
 *    another singer's song), never two of the same creator in a row, and
 *  - every few items the same creator comes back (another song / remix / slowed version / video of his),
 *    so the queue is never "one artist from start to finish".
 *
 * Pure (no Android / network), so it is unit-tested.
 */
object OnlineTrackSequencer {

    private val NOISE = Regex("\\b(vevo|official|topic|music|records|channel|tv|audio|video|vlogs?)\\b")

    /** "Arijit Singh - Topic" / "ArijitSinghVEVO" / "arijit singh official" all become comparable keys. */
    fun creatorKey(name: String): String =
        name.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("(?<=\\p{L})vevo\\b"), "") // "ArijitSinghVEVO"
            .replace(NOISE, " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun normalize(text: String): String =
        text.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").replace(Regex("\\s+"), " ").trim()

    /** True when [t] is by the creator [seedKey] (channel, or the creator is named in the title: "Arijit Singh - ..."). */
    fun isSameCreator(seedKey: String, t: OnlineTrackResult): Boolean {
        if (seedKey.isBlank()) return false
        val key = creatorKey(t.artist)
        if (key.isNotEmpty() && (key == seedKey || key.replace(" ", "") == seedKey.replace(" ", ""))) return true
        return seedKey.length >= 4 && " ${normalize(t.title)} ".contains(" $seedKey ")
    }

    /**
     * [items] re-ordered for the track that is playing now (creator [seedKey]).
     * [seedExtras] are more items of that same creator (remixes, slowed versions, other songs) to bring back in between.
     * [preferred] marks items of the same kind as the current one; those are tried first.
     * [otherGap]: a creator must not appear again within this many items; [seedEvery]: the playing creator is heard
     * again after this many items of other creators.
     */
    fun arrange(
        items: List<OnlineTrackResult>,
        seedKey: String,
        currentUrl: String = "",
        seedExtras: List<OnlineTrackResult> = emptyList(),
        preferred: (OnlineTrackResult) -> Boolean = { true },
        otherGap: Int = 2,
        seedEvery: Int = 3
    ): List<OnlineTrackResult> {
        val seen = HashSet<String>()
        if (currentUrl.isNotBlank()) seen.add(currentUrl)
        val all = (items + seedExtras).filter { it.videoUrl.isNotBlank() && seen.add(it.videoUrl) }

        val seeds = ArrayList<OnlineTrackResult>()
        val others = ArrayList<OnlineTrackResult>()
        for (t in all) if (isSameCreator(seedKey, t)) seeds.add(t) else others.add(t)

        // same kind of content first (stable), the rest after it
        val pool = ArrayList<OnlineTrackResult>(others.size)
        pool.addAll(others.filter(preferred))
        pool.addAll(others.filterNot(preferred))

        // greedy: next item whose creator was not heard within the last [otherGap] picks
        val recent = ArrayDeque<String>()
        if (seedKey.isNotEmpty()) recent.addLast(seedKey)
        val spread = ArrayList<OnlineTrackResult>(pool.size)
        val remaining = pool.toMutableList()
        while (remaining.isNotEmpty()) {
            var idx = remaining.indexOfFirst { creatorKey(it.artist) !in recent }
            if (idx < 0) idx = remaining.indexOfFirst { creatorKey(it.artist) != recent.lastOrNull() }
            if (idx < 0) idx = 0
            val pick = remaining.removeAt(idx)
            spread.add(pick)
            recent.addLast(creatorKey(pick.artist))
            while (recent.size > otherGap) recent.removeFirst()
        }

        // the playing creator comes back every [seedEvery] items
        val out = ArrayList<OnlineTrackResult>(spread.size + seeds.size)
        var seedIdx = 0
        for ((i, t) in spread.withIndex()) {
            out.add(t)
            if ((i + 1) % seedEvery == 0 && seedIdx < seeds.size) out.add(seeds[seedIdx++])
        }
        while (seedIdx < seeds.size) out.add(seeds[seedIdx++])
        return out
    }
}
