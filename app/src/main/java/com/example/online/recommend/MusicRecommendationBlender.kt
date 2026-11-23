package com.example.util

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

/**
 * Generic list-mixing helpers (used by the Online Video feed). The Online Music recommendation engine is further down this file.
 *
 * Old description - Builds Online Music recommendations as a deliberate mix:
 *  - at least [SIMILAR_SHARE] (65%) "similar" tracks, i.e. what the listener is playing / usually plays, and
 *  - the remaining ~35% "discovery" tracks from unrelated genres/moods, so the listener keeps meeting
 *    music outside their usual category.
 *
 * Used by the auto-queue that follows a played online track and by the endless "Recommended for you" feed.
 * All network access goes through [OnlineMusicService.search]; [mix] itself is pure and unit-testable.
 */
object OnlineRecommendationMixer {

    const val SIMILAR_SHARE = 0.65

    /** Suffixes so repeated batches for the same base query return different results. */
    private val QUERY_MODIFIERS = listOf(
        "songs", "best songs", "hits", "new songs", "top tracks", "unplugged", "live", "audio"
    )

    /**
     * Combines two pools into one list of [total] items where ~[similarShare] come from [similar] and the
     * rest from [discovery]. Discovery items are spread evenly through the list (not bunched at the end).
     * If one pool is too small the other one fills the gap so the list stays as full as possible.
     */
    fun <T> mix(
        similar: List<T>,
        discovery: List<T>,
        total: Int,
        similarShare: Double = SIMILAR_SHARE
    ): List<T> {
        if (total <= 0) return emptyList()
        val wantSimilar = kotlin.math.ceil(total * similarShare - 1e-9).toInt().coerceIn(0, total)
        val wantDiscovery = total - wantSimilar

        var takeSimilar = minOf(wantSimilar, similar.size)
        var takeDiscovery = minOf(wantDiscovery, discovery.size)

        var missing = total - takeSimilar - takeDiscovery
        if (missing > 0) {
            val extra = minOf(missing, similar.size - takeSimilar)
            takeSimilar += extra
            missing -= extra
        }
        if (missing > 0) {
            takeDiscovery += minOf(missing, discovery.size - takeDiscovery)
        }

        val s = similar.take(takeSimilar)
        val d = discovery.take(takeDiscovery)
        val n = s.size + d.size
        val out = ArrayList<T>(n)
        var si = 0
        var di = 0
        for (pos in 0 until n) {
            val useDiscovery = when {
                di >= d.size -> false
                si >= s.size -> true
                // Bresenham-style spread: a discovery item is "due" once its proportional share is reached.
                else -> ((pos + 1) * d.size) / n > di
            }
            if (useDiscovery) out.add(d[di++]) else out.add(s[si++])
        }
        return out
    }

    /**
     * Spreads several pools "here and there" through one list instead of in blocks: pool i gets [quotas] [i] places,
     * and those places are spread evenly over the list with a little random jitter, so a batch reads as a mixed feed
     * (same creator, trending of the same topic, other games, tech / AI / podcast, popular channels ...).
     * A pool that runs dry is topped up from the others, an item that is in several pools is used once.
     */
    fun <T> scatterMix(
        pools: List<List<T>>,
        quotas: List<Int>,
        total: Int,
        random: Random = Random.Default
    ): List<T> {
        if (total <= 0) return emptyList()
        val iters = pools.map { it.iterator() }
        val used = HashSet<T>()
        fun next(i: Int): T? {
            val iter = iters[i]
            while (iter.hasNext()) {
                val v = iter.next()
                if (used.add(v)) return v
            }
            return null
        }
        val slots = ArrayList<Pair<Double, Int>>()
        for ((i, q) in quotas.withIndex()) {
            if (i >= pools.size || q <= 0) continue
            for (k in 0 until q) {
                val jitter = (random.nextDouble() - 0.5) * 0.6 / q
                slots.add(((k + 0.5) / q + jitter) to i)
            }
        }
        slots.sortWith(compareBy({ it.first }, { it.second }))
        val out = ArrayList<T>(total)
        for ((_, i) in slots) {
            if (out.size >= total) break
            val v = next(i)
            if (v != null) out.add(v)
        }
        // short pools / more places wanted than the quotas add up to: fill from whichever pool still has something
        var i = 0
        while (out.size < total && i < pools.size) {
            val v = next(i)
            if (v == null) i++ else out.add(v)
        }
        return out
    }

    /** Interleaves several lists (one item from each in turn) so no single query/artist dominates. */
    fun <T> roundRobin(lists: List<List<T>>): List<T> {
        val out = ArrayList<T>()
        var index = 0
        while (true) {
            var added = false
            for (list in lists) {
                if (index < list.size) {
                    out.add(list[index])
                    added = true
                }
            }
            if (!added) break
            index++
        }
        return out
    }

    /**
     * Picks a base+modifier query that was not used yet in [used] (and records it). When every
     * combination has been used, [used] is cleared so the feed can keep going forever.
     */
    fun freshQuery(
        bases: List<String>,
        used: MutableSet<String>,
        random: Random = Random.Default,
        modifiers: List<String> = QUERY_MODIFIERS
    ): String? {
        if (bases.isEmpty()) return null
        repeat(2) {
            for (base in bases.shuffled(random)) {
                for (modifier in modifiers.shuffled(random)) {
                    val q = "$base $modifier".trim()
                    if (used.add(q)) return q
                }
            }
            used.clear()
        }
        return null
    }
}

// =====================================================================================================================
// ONLINE MUSIC RECOMMENDATION ENGINE
//
//  * Only songs that were REALLY PLAYED are seeds. Searching (typing a query, looking at results) never changes anything.
//  * Every played song is described by metadata (genre, sub-genre, language, era, mood when a tag says so) taken from free
//    sources (iTunes Search, MusicBrainz tags). The title is only a low-confidence fallback when no source knows the artist.
//  * Listening behaviour (time listened, finished / skipped, replays, favourites, playlist adds) is stored on the phone and
//    fades with time (half-life 30 days). One skip never rejects a genre.
//  * Candidates come from free sources (Deezer: related artists, top tracks, charts; iTunes charts; YouTube Music search),
//    are ranked on genre / sub-genre / artist / era / popularity / freshness / trend, filtered (duplicates, skipped, recently
//    heard) and made artist-diverse. Everything is cached on the phone; offline the last feed is shown.
//  * All work runs on background dispatchers and never touches the audio player.
// =====================================================================================================================

data class MusicMeta(
    val genre: String,
    val subgenre: String,
    val language: String,
    val year: Int,
    val mood: String,
    val confidence: Float,
    val source: String
) {
    val era: String get() = if (year in 1950..2100) "${year / 10 * 10}s" else ""
    companion object { val UNKNOWN = MusicMeta("", "", "", 0, "", 0f, "none") }
}

data class MusicArtist(val name: String, val pictureUrl: String?, val fans: Long)

data class MusicFeedSection(
    val id: String,
    val title: String,
    val tracks: List<OnlineTrackResult>,
    val artists: List<MusicArtist> = emptyList()
)

private data class MusicCandidate(
    val title: String,
    val artist: String,
    val coverUrl: String?,
    val popularity: Float,        // 0..1
    val releaseMs: Long,          // 0 = unknown
    val genre: String,            // genre hint from the source (chart category / seed)
    val subgenre: String,
    val trend: Boolean,
    val relatedTo: String?,       // seed artist this candidate is "similar to"
    val source: String,
    val ready: OnlineTrackResult? = null   // already playable (YouTube Music result)
)

private object MusicText {
    fun norm(s: String): String = s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** "Song (Slowed) - Artist" / "[Official Video]" ... -> the plain song name, so versions of one song count once. */
    fun baseTitle(t: String): String = norm(
        t.replace(Regex("\\(.*?\\)|\\[.*?\\]|\\{.*?\\}"), " ")
            .substringBefore(" - ").substringBefore(" | ").substringBefore(" ft.").substringBefore(" feat")
    ).replace(Regex("\\b(official|video|audio|lyrics?|lyrical|slowed|reverb|sped up|speed up|remix|extended|ultra|version|hd|4k|mv)\\b"), " ")
        .replace(Regex("\\s+"), " ").trim()

    /** Versions / uploads of one song share a key. A one-word title also needs the artist (many songs are called "Love"). */
    fun songKey(artist: String, title: String): String {
        val bt = baseTitle(title)
        return if (bt.split(' ').count { it.isNotBlank() } >= 2) bt else norm(artist).take(24) + "|" + bt
    }
    fun artistKey(a: String): String = norm(a).replace(Regex("\\b(official|music|vevo|topic|records?|tv|channel)\\b"), "").replace(Regex("\\s+"), " ").trim()
    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

/** Free metadata -> genre / sub-genre / language / era / mood. Results are cached on the phone per artist + song. */
object MusicMetadataResolver {
    private class Rule(val re: Regex, val genre: String, val sub: String)
    private fun r(alts: String, genre: String, sub: String) = Rule(Regex("\\b(?:$alts)\\b"), genre, sub)

    private val RULES = listOf(
        r("brazilian phonk|phonk brasileiro|funk phonk|brazilian funk|funk carioca|baile funk|funk brasileiro|funk br", "Phonk", "Brazilian Phonk"),
        r("drift phonk", "Phonk", "Drift Phonk"), r("memphis phonk|memphis rap|memphis", "Phonk", "Memphis Phonk"),
        r("house phonk", "Phonk", "House Phonk"), r("aggressive phonk|hard phonk", "Phonk", "Aggressive Phonk"), r("phonk", "Phonk", "Phonk"),
        r("drill", "Hip-Hop", "Drill"), r("trap", "Hip-Hop", "Trap"), r("old school hip hop|old school hip-hop|boom bap|golden age hip hop", "Hip-Hop", "Old School Hip-Hop"),
        r("underground hip hop|underground rap|cloud rap", "Hip-Hop", "Underground Hip-Hop"), r("hip hop|hip-hop|hiphop|rap", "Hip-Hop", "Rap"),
        r("dubstep|brostep", "Electronic", "Dubstep"), r("synthwave|retrowave|outrun", "Electronic", "Synthwave"), r("trance", "Electronic", "Trance"),
        r("techno", "Electronic", "Techno"), r("house", "Electronic", "House"), r("edm|electro|electronic|dance|drum and bass|dnb", "Electronic", "EDM"),
        r("lo-fi|lofi|chillhop", "Lo-Fi", "Lo-Fi"), r("ambient", "Ambient", "Ambient"), r("instrumental|soundtrack|score", "Instrumental", "Instrumental"),
        r("k-pop|kpop", "Pop", "K-Pop"), r("bollywood|filmi|hindi film|hindi pop|indian pop|indian|desi", "Bollywood", "Bollywood"),
        r("bhojpuri", "Bhojpuri", "Bhojpuri"), r("haryanvi", "Haryanvi", "Haryanvi"), r("punjabi|bhangra", "Punjabi", "Punjabi"),
        r("tamil|kollywood", "Tamil", "Tamil"), r("telugu|tollywood", "Telugu", "Telugu"),
        r("ghazal|qawwali|sufi", "Devotional", "Sufi / Ghazal"), r("devotional|bhajan|aarti|spiritual", "Devotional", "Devotional"),
        r("heavy metal|thrash metal|death metal|metalcore|metal", "Metal", "Metal"), r("punk|hard rock|alternative rock|indie rock|rock", "Rock", "Rock"),
        r("jazz", "Jazz", "Jazz"), r("blues", "Blues", "Blues"), r("r&b|rnb|r and b|rhythm and blues", "R&B", "R&B"), r("soul|funk", "Soul", "Soul"),
        r("classical|orchestra|opera", "Classical", "Classical"), r("reggaeton|latin|salsa|bachata|reggae", "Latin", "Latin"),
        r("country|folk", "Folk", "Folk"), r("pop", "Pop", "Pop")
    )
    private val MOODS = listOf(
        "sad|melancholy|melancholic|heartbreak" to "Sad", "romantic|romance|love" to "Romantic", "happy|feel good|feel-good|uplifting" to "Happy",
        "energetic|upbeat|energy" to "Energetic", "aggressive|angry|hard|intense" to "Aggressive", "calm|chill|relax|mellow|soothing" to "Calm",
        "emotional" to "Emotional", "motivational|inspirational|motivation" to "Motivational", "dark|gloomy|eerie" to "Dark",
        "workout|gym" to "Workout", "driving|drive" to "Driving", "gaming|gamer" to "Gaming", "study|focus|concentration" to "Studying",
        "sleep|sleeping" to "Sleeping", "party|club" to "Party"
    ).map { Regex("\\b(?:${it.first})\\b") to it.second }

    private fun classify(tags: List<String>): Pair<String, String>? {
        for (t in tags) {
            val low = t.lowercase(Locale.ROOT)
            for (rule in RULES) if (rule.re.containsMatchIn(low)) return rule.genre to rule.sub
        }
        return null
    }

    private fun languageOf(tags: List<String>, country: String, text: String): String {
        val joined = tags.joinToString(" ").lowercase(Locale.ROOT)
        val byTag = listOf("hindi" to "Hindi", "punjabi" to "Punjabi", "tamil" to "Tamil", "telugu" to "Telugu", "bhojpuri" to "Bhojpuri", "haryanvi" to "Haryanvi",
            "portuguese" to "Portuguese", "brazil" to "Portuguese", "spanish" to "Spanish", "korean" to "Korean", "japanese" to "Japanese", "english" to "English")
        byTag.firstOrNull { joined.contains(it.first) }?.let { return it.second }
        for (ch in text) {
            when (Character.UnicodeScript.of(ch.code)) {
                Character.UnicodeScript.DEVANAGARI -> return "Hindi"
                Character.UnicodeScript.GURMUKHI -> return "Punjabi"
                Character.UnicodeScript.TAMIL -> return "Tamil"
                Character.UnicodeScript.TELUGU -> return "Telugu"
                Character.UnicodeScript.HANGUL -> return "Korean"
                Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> return "Japanese"
                Character.UnicodeScript.ARABIC -> return "Arabic"
                else -> {}
            }
        }
        return when (country.uppercase(Locale.ROOT)) {
            "IN" -> "Hindi"; "BR", "PT" -> "Portuguese"; "US", "GB", "CA", "AU", "IE" -> "English"; "KR" -> "Korean"; "JP" -> "Japanese"
            "ES", "MX", "CO", "AR", "CL" -> "Spanish"; else -> ""
        }
    }

    private val mbMutex = Mutex()
    private var lastMbAt = 0L

    /** Genre, sub-genre, language, era (release year) and mood of a song. Cached; never blocks the caller for long. */
    suspend fun resolve(c: Context, artist: String, title: String): MusicMeta = withContext(Dispatchers.IO) {
        val akey = MusicText.artistKey(artist)
        val skey = MusicText.songKey(artist, title)
        MusicCache.getMeta(c, skey)?.let { return@withContext it }
        var year = 0
        var country = ""
        val tags = ArrayList<String>()
        // 1) iTunes Search: primary genre, release date, store country of the song
        try {
            val q = MusicText.enc("$artist ${MusicText.baseTitle(title)}")
            MusicHttp.get(c, "https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=5", 7 * 24 * 3600_000L)?.let { body ->
                val arr = JSONObject(body).optJSONArray("results")
                if (arr != null) for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val a = MusicText.artistKey(o.optString("artistName"))
                    if (a.isNotBlank() && akey.isNotBlank() && (a.contains(akey) || akey.contains(a))) {
                        o.optString("primaryGenreName").takeIf { it.isNotBlank() }?.let { tags.add(it) }
                        year = o.optString("releaseDate").take(4).toIntOrNull() ?: 0
                        country = o.optString("country")
                        break
                    }
                }
            }
        } catch (_: Throwable) {}
        // 2) MusicBrainz artist tags (community genre tags, most voted first); max 1 request per second as the service asks
        var mbCountry = ""
        try {
            val cached = MusicCache.getArtistTags(c, akey)
            if (cached != null) {
                tags.addAll(cached.first); mbCountry = cached.second
            } else if (akey.isNotBlank()) {
                val body = mbMutex.withLock {
                    val wait = 1100L - (System.currentTimeMillis() - lastMbAt)
                    if (wait > 0) delay(wait)
                    lastMbAt = System.currentTimeMillis()
                    MusicHttp.getRaw("https://musicbrainz.org/ws/2/artist/?query=artist:${MusicText.enc("\"$artist\"")}&limit=1&fmt=json")
                }
                val a0 = body?.let { JSONObject(it).optJSONArray("artists")?.optJSONObject(0) }
                val list = ArrayList<String>()
                a0?.optJSONArray("tags")?.let { ta ->
                    val rows = (0 until ta.length()).mapNotNull { ta.optJSONObject(it) }.sortedByDescending { it.optInt("count") }
                    rows.take(8).forEach { list.add(it.optString("name")) }
                }
                mbCountry = a0?.optString("country").orEmpty()
                if (body != null) MusicCache.putArtistTags(c, akey, list, mbCountry)
                tags.addAll(list)
            }
        } catch (_: Throwable) {}

        var conf = 1f
        var source = "iTunes+MusicBrainz"
        var g = classify(tags)
        if (g == null) {
            // last resort, LOW confidence: artist / title words (a song title alone is never trusted as much as real metadata)
            g = classify(listOf(artist, title))
            conf = if (g != null) 0.35f else 0f
            source = if (g != null) "title-hint" else "none"
        } else if (tags.isEmpty()) {
            conf = 0.5f
        }
        val moodTag = MOODS.firstOrNull { (re, _) -> tags.any { re.containsMatchIn(it.lowercase(Locale.ROOT)) } }?.second.orEmpty()
        val meta = MusicMeta(
            genre = g?.first.orEmpty(), subgenre = g?.second.orEmpty(),
            language = languageOf(tags, mbCountry.ifBlank { country }, title + artist),
            year = year, mood = moodTag, confidence = conf, source = source
        )
        MusicCache.putMeta(c, skey, meta)
        meta
    }
}

// ------------------------------------------------------------------------------------------------ storage + network

private class JsonStore(private val name: String, private val cap: Int) {
    private var map: LinkedHashMap<String, JSONObject>? = null
    private var saveQueued = false
    private val saver = java.util.concurrent.Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "viaura-music-store").apply { isDaemon = true } }

    @Synchronized
    private fun load(c: Context): LinkedHashMap<String, JSONObject> {
        map?.let { return it }
        val m = LinkedHashMap<String, JSONObject>()
        try {
            val f = java.io.File(c.applicationContext.filesDir, "music_$name.json")
            if (f.exists()) {
                val o = JSONObject(f.readText())
                val keys = o.keys()
                while (keys.hasNext()) { val k = keys.next(); o.optJSONObject(k)?.let { m[k] = it } }
            }
        } catch (_: Throwable) {}
        map = m
        return m
    }

    @Synchronized fun get(c: Context, k: String): JSONObject? = load(c)[k]

    @Synchronized fun put(c: Context, k: String, v: JSONObject) {
        val m = load(c)
        m.remove(k)
        m[k] = v
        while (m.size > cap) m.remove(m.keys.first())
        queueSave(c)
    }

    @Synchronized fun remove(c: Context, k: String) { load(c).remove(k); queueSave(c) }

    /** A copy of all entries (safe to iterate while other threads write). */
    @Synchronized fun entries(c: Context): List<Pair<String, JSONObject>> = load(c).entries.map { it.key to it.value }

    @Synchronized private fun queueSave(c: Context) {
        if (saveQueued) return
        saveQueued = true
        val app = c.applicationContext
        saver.schedule(Runnable {
            try {
                val snapshot: String = synchronized(this) {
                    saveQueued = false
                    val o = JSONObject()
                    map?.forEach { (k, v) -> o.put(k, v) }
                    o.toString()
                }
                java.io.File(app.filesDir, "music_$name.json").writeText(snapshot)
            } catch (_: Throwable) {}
        }, 4, TimeUnit.SECONDS)
    }
}

private object MusicHttp {
    private val client = OkHttpClient.Builder().connectTimeout(6, TimeUnit.SECONDS).readTimeout(9, TimeUnit.SECONDS).build()

    fun getRaw(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", "Viaura/1.0 (Android music player; free metadata lookups)").header("Accept", "application/json").build()
        client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
    } catch (_: Throwable) {
        null
    }

    /** Cached GET: a fresh copy is used for [ttlMs]; when the network fails (offline, rate limit) the old copy is used. */
    fun get(c: Context, url: String, ttlMs: Long): String? {
        val dir = java.io.File(c.applicationContext.cacheDir, "music_http").apply { mkdirs() }
        val f = java.io.File(dir, Integer.toHexString(url.hashCode()) + "_" + url.length + ".json")
        val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < ttlMs
        if (fresh) try { return f.readText() } catch (_: Throwable) {}
        val body = getRaw(url)
        if (body != null) {
            try {
                f.writeText(body)
                val files = dir.listFiles()
                if (files != null && files.size > 260) files.sortedBy { it.lastModified() }.take(files.size - 200).forEach { it.delete() }
            } catch (_: Throwable) {}
            return body
        }
        return try { if (f.exists()) f.readText() else null } catch (_: Throwable) { null }
    }
}

private object MusicCache {
    private val metaStore = JsonStore("meta", 2500)
    private val artistStore = JsonStore("artist_tags", 1500)
    private val resolvedStore = JsonStore("resolved", 3500)
    private val feedStore = JsonStore("feed", 8)

    fun getMeta(c: Context, key: String): MusicMeta? = metaStore.get(c, key)?.let {
        MusicMeta(it.optString("g"), it.optString("s"), it.optString("l"), it.optInt("y"), it.optString("m"), it.optDouble("c", 0.0).toFloat(), it.optString("src"))
    }
    fun putMeta(c: Context, key: String, m: MusicMeta) = metaStore.put(c, key, JSONObject().put("g", m.genre).put("s", m.subgenre).put("l", m.language).put("y", m.year).put("m", m.mood).put("c", m.confidence.toDouble()).put("src", m.source))

    fun getArtistTags(c: Context, key: String): Pair<List<String>, String>? = artistStore.get(c, key)?.let { o ->
        val a = o.optJSONArray("tags")
        (if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }) to o.optString("country")
    }
    fun putArtistTags(c: Context, key: String, tags: List<String>, country: String) =
        artistStore.put(c, key, JSONObject().put("tags", JSONArray(tags)).put("country", country))

    fun trackToJson(t: OnlineTrackResult): JSONObject = JSONObject().put("u", t.videoUrl).put("t", t.title).put("a", t.artist)
        .put("d", t.durationSec).put("th", t.thumbnailUrl ?: "").put("v", t.viewCount)
    fun trackFromJson(o: JSONObject): OnlineTrackResult = OnlineTrackResult(
        videoUrl = o.optString("u"), title = o.optString("t"), artist = o.optString("a"), durationSec = o.optLong("d"),
        thumbnailUrl = o.optString("th").ifBlank { null }, viewCount = o.optLong("v")
    )

    fun getResolved(c: Context, key: String): OnlineTrackResult? = resolvedStore.get(c, key)?.let { trackFromJson(it) }?.takeIf { it.videoUrl.isNotBlank() }
    fun putResolved(c: Context, key: String, t: OnlineTrackResult) = resolvedStore.put(c, key, trackToJson(t))

    fun saveFeed(c: Context, sections: List<MusicFeedSection>) {
        val arr = JSONArray()
        sections.forEach { s ->
            arr.put(
                JSONObject().put("id", s.id).put("title", s.title)
                    .put("tracks", JSONArray().also { ja -> s.tracks.forEach { ja.put(trackToJson(it)) } })
                    .put("artists", JSONArray().also { ja -> s.artists.forEach { ja.put(JSONObject().put("n", it.name).put("p", it.pictureUrl ?: "").put("f", it.fans)) } })
            )
        }
        feedStore.put(c, "feed", JSONObject().put("ts", System.currentTimeMillis()).put("sections", arr))
    }
    fun loadFeed(c: Context): Pair<Long, List<MusicFeedSection>>? {
        val o = feedStore.get(c, "feed") ?: return null
        val arr = o.optJSONArray("sections") ?: return null
        val list = (0 until arr.length()).mapNotNull { i ->
            val s = arr.optJSONObject(i) ?: return@mapNotNull null
            val ts = s.optJSONArray("tracks")
            val tracks = if (ts == null) emptyList() else (0 until ts.length()).mapNotNull { j -> ts.optJSONObject(j)?.let { trackFromJson(it) } }
            val ars = s.optJSONArray("artists")
            val artists = if (ars == null) emptyList() else (0 until ars.length()).mapNotNull { j ->
                ars.optJSONObject(j)?.let { MusicArtist(it.optString("n"), it.optString("p").ifBlank { null }, it.optLong("f")) }
            }
            MusicFeedSection(s.optString("id"), s.optString("title"), tracks, artists)
        }
        return o.optLong("ts") to list
    }
}

// ------------------------------------------------------------------------------------------------ listening behaviour

/** What the listener really plays, and how: listening time, finished / skipped, replays, favourites, playlist adds. */
object MusicTaste {
    private const val HALF_LIFE_MS = 30L * 24 * 3600 * 1000
    private val store = JsonStore("taste", 3000)
    /** Counts every real change of the taste (a song played, listened, favourited ...): the UI refreshes its feed from it. */
    val changes = kotlinx.coroutines.flow.MutableStateFlow(0)

    private fun decayed(v: Double, t: Long): Double = v * Math.pow(0.5, (System.currentTimeMillis() - t).toDouble() / HALF_LIFE_MS)

    private fun bump(c: Context, key: String, delta: Double) {
        if (key.endsWith(":") || delta == 0.0) return
        val old = store.get(c, key)
        val cur = if (old == null) 0.0 else decayed(old.optDouble("v"), old.optLong("t"))
        store.put(c, key, JSONObject().put("v", (cur + delta).coerceAtLeast(0.0)).put("t", System.currentTimeMillis()))
    }

    fun score(c: Context, key: String): Double = store.get(c, key)?.let { decayed(it.optDouble("v"), it.optLong("t")) } ?: 0.0

    /** Highest scores of one group ("g:" genre, "s:" sub-genre, "a:" artist, "l:" language, "e:" era, "m:" mood). */
    fun top(c: Context, prefix: String, n: Int): List<Pair<String, Double>> =
        store.entries(c).filter { it.first.startsWith(prefix) }
            .map { it.first.removePrefix(prefix) to decayed(it.second.optDouble("v"), it.second.optLong("t")) }
            .filter { it.second > 0.15 }.sortedByDescending { it.second }.take(n)

    fun track(c: Context, url: String): JSONObject? = store.get(c, "t:$url")

    fun hasData(c: Context): Boolean = store.entries(c).any { it.first.startsWith("t:") }

    fun onPlayStart(c: Context, t: OnlineTrackResult) {
        val old = track(c, t.videoUrl)
        val now = System.currentTimeMillis()
        val e = old ?: JSONObject()
        // played again within 6 hours of the last time = a replay
        val replay = old != null && now - old.optLong("last") < 6 * 3600_000L
        e.put("title", t.title).put("artist", t.artist).put("thumb", t.thumbnailUrl ?: "").put("dur", t.durationSec)
            .put("plays", e.optInt("plays") + 1).put("replays", e.optInt("replays") + if (replay) 1 else 0).put("last", now)
        store.put(c, "t:${t.videoUrl}", e)
        bump(c, "a:" + MusicText.artistKey(t.artist), if (replay) 1.6 else 1.0)
        changes.value = changes.value + 1
    }

    fun applyMeta(c: Context, url: String, meta: MusicMeta) {
        val e = track(c, url) ?: return
        if (e.has("g") && e.optString("metaSrc") == meta.source) return
        e.put("g", meta.genre).put("s", meta.subgenre).put("l", meta.language).put("e", meta.era).put("m", meta.mood).put("metaSrc", meta.source)
        store.put(c, "t:$url", e)
        val w = meta.confidence.toDouble()
        bump(c, "g:" + meta.genre, 1.0 * w)
        bump(c, "s:" + meta.subgenre, 1.0 * w)
        bump(c, "l:" + meta.language, 0.6 * w)
        bump(c, "e:" + meta.era, 0.6 * w)
        bump(c, "m:" + meta.mood, 0.4 * w)
        changes.value = changes.value + 1
    }

    /** A song that was heard for [listenedMs] of [durationMs]: finished = strong interest, a long listen = interest, a quick skip = weak negative. */
    fun onListened(c: Context, url: String, listenedMs: Long, durationMs: Long) {
        val e = track(c, url) ?: return
        if (durationMs <= 0L) return
        val frac = (listenedMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
        e.put("ms", e.optLong("ms") + listenedMs)
        val artistKey = "a:" + MusicText.artistKey(e.optString("artist"))
        when {
            frac >= 0.8 -> {
                e.put("comp", e.optInt("comp") + 1)
                listOf("g:" + e.optString("g"), "s:" + e.optString("s")).forEach { bump(c, it, 0.8) }
                bump(c, artistKey, 0.8)
            }
            frac >= 0.3 -> {
                listOf("g:" + e.optString("g"), "s:" + e.optString("s")).forEach { bump(c, it, frac * 0.6) }
                bump(c, artistKey, frac * 0.6)
            }
            listenedMs < 25_000L -> {
                // a skip: noted for THIS song; its artist / sub-genre only lose a little after the same song was skipped twice.
                // The genre itself is never reduced by skips.
                e.put("skips", e.optInt("skips") + 1)
                if (e.optInt("skips") >= 2 && e.optInt("comp") == 0) {
                    bump(c, artistKey, -0.5)
                    bump(c, "s:" + e.optString("s"), -0.3)
                }
            }
        }
        store.put(c, "t:$url", e)
        changes.value = changes.value + 1
    }

    fun onFavorite(c: Context, url: String) = strong(c, url, 2.0)
    fun onPlaylistAdd(c: Context, url: String) = strong(c, url, 1.5)

    private fun strong(c: Context, url: String, w: Double) {
        val e = track(c, url) ?: return
        e.put("fav", e.optInt("fav") + 1)
        store.put(c, "t:$url", e)
        bump(c, "a:" + MusicText.artistKey(e.optString("artist")), w)
        bump(c, "g:" + e.optString("g"), w)
        bump(c, "s:" + e.optString("s"), w)
        changes.value = changes.value + 1
    }

    /** A song that was skipped again and again and never finished: it is never recommended. */
    fun isRejected(c: Context, url: String): Boolean = track(c, url)?.let { it.optInt("skips") >= 2 && it.optInt("comp") == 0 } ?: false

    data class Played(val url: String, val title: String, val artist: String, val thumb: String?, val durSec: Long, val last: Long, val plays: Int, val completes: Int)

    fun played(c: Context): List<Played> = store.entries(c).filter { it.first.startsWith("t:") }.map { (k, o) ->
        Played(k.removePrefix("t:"), o.optString("title"), o.optString("artist"), o.optString("thumb").ifBlank { null }, o.optLong("dur"), o.optLong("last"), o.optInt("plays"), o.optInt("comp"))
    }

    fun recent(c: Context, n: Int): List<Played> = played(c).sortedByDescending { it.last }.take(n)

    /** Artists with at least [minSongs] DIFFERENT songs played (the "I keep replaying this creator" signal), strongest first. */
    fun repeatedArtists(c: Context, minSongs: Int): List<Pair<String, Int>> =
        played(c).filter { it.artist.isNotBlank() }.groupBy { it.artist }.map { it.key to it.value.size }
            .filter { it.second >= minSongs }.sortedByDescending { it.second }
}

/** Watches the real playback (never the search box): a song counts only once the player is playing it. Runs off the main thread. */
object MusicListeningTracker {
    private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var curPath: String? = null
    private var cur: OnlineTrackResult? = null
    private var maxPos = 0L
    private var lastPos = 0L

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        scope.launch {
            com.example.player.AudioPlaybackManager.currentTrack.collect { t -> onTrack(app, t) }
        }
        scope.launch {
            com.example.player.AudioPlaybackManager.currentPositionMs.collect { p -> onPosition(app, p) }
        }
    }

    @Synchronized
    private fun onTrack(app: Context, t: com.example.ui.screens.AudioTrackItem?) {
        val path = t?.path
        if (path == curPath) return
        finishCurrent(app)
        curPath = path
        cur = null
        maxPos = 0L
        lastPos = 0L
        if (t != null && path != null && path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)) {
            val track = OnlineTrackResult(
                videoUrl = path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX), title = t.title, artist = t.artist,
                durationSec = t.durationMs / 1000L, thumbnailUrl = t.thumbnailUrl
            )
            cur = track
            startPlay(app, track)
        }
    }

    @Synchronized
    private fun onPosition(app: Context, p: Long) {
        val track = cur ?: return
        val durMs = track.durationSec * 1000L
        // the song reached its end and started again: one finished listen + a replay
        if (lastPos > 0 && p + 5000 < lastPos && durMs > 0 && maxPos >= durMs * 0.8) {
            finishCurrent(app)
            maxPos = p
            startPlay(app, track)
        }
        lastPos = p
        if (p > maxPos) maxPos = p
    }

    private fun finishCurrent(app: Context) {
        val t = cur ?: return
        val listened = maxPos
        val dur = t.durationSec * 1000L
        scope.launch { try { MusicTaste.onListened(app, t.videoUrl, listened, dur) } catch (_: Throwable) {} }
    }

    private fun startPlay(app: Context, t: OnlineTrackResult) {
        scope.launch {
            try {
                MusicTaste.onPlayStart(app, t)
                // metadata (genre, sub-genre, language, era, mood) of the song that is really playing now
                MusicTaste.applyMeta(app, t.videoUrl, MusicMetadataResolver.resolve(app, t.artist, t.title))
            } catch (_: Throwable) {}
        }
    }
}

// ------------------------------------------------------------------------------------------------ free sources

private data class DzArtist(val id: Long, val name: String, val pic: String?, val fans: Long)

private object MusicSources {
    private const val DAY = 24L * 3600 * 1000
    private val DEEZER_GENRE = mapOf(
        "Pop" to 132, "Hip-Hop" to 116, "Phonk" to 116, "Rock" to 152, "Electronic" to 106, "R&B" to 165, "Metal" to 464,
        "Jazz" to 129, "Classical" to 98, "Latin" to 197, "Soul" to 169, "Folk" to 466, "Lo-Fi" to 106
    )
    fun deezerGenreId(genre: String): Int? = DEEZER_GENRE[genre]

    /** iTunes store used for the country charts (language of the listener decides which one). */
    fun chartCountry(language: String): String = when (language) {
        "Hindi", "Punjabi", "Bhojpuri", "Haryanvi", "Tamil", "Telugu" -> "in"; "Portuguese" -> "br"; "Spanish" -> "mx"; "Korean" -> "kr"; "Japanese" -> "jp"; else -> "us"
    }

    private fun pop(rank: Long): Float = (ln(rank + 1.0) / ln(1_000_001.0)).toFloat().coerceIn(0f, 1f)
    private fun parseDay(s: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s.take(10))?.time ?: 0L
    } catch (_: Throwable) { 0L }
    private fun big(url: String?): String? = url?.replace(Regex("\\d+x\\d+bb"), "400x400bb")?.takeIf { it.isNotBlank() }

    suspend fun deezerArtist(c: Context, name: String): DzArtist? = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://api.deezer.com/search/artist?q=${MusicText.enc(name)}&limit=3", 14 * DAY) ?: return@withContext null
            val arr = JSONObject(body).optJSONArray("data") ?: return@withContext null
            val key = MusicText.artistKey(name)
            val o = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.firstOrNull {
                val k = MusicText.artistKey(it.optString("name")); k.contains(key) || key.contains(k)
            } ?: return@withContext null
            DzArtist(o.optLong("id"), o.optString("name"), o.optString("picture_medium").ifBlank { null }, o.optLong("nb_fan"))
        } catch (_: Throwable) { null }
    }

    suspend fun deezerRelated(c: Context, id: Long, limit: Int): List<DzArtist> = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://api.deezer.com/artist/$id/related?limit=$limit", 7 * DAY) ?: return@withContext emptyList()
            val arr = JSONObject(body).optJSONArray("data") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { DzArtist(it.optLong("id"), it.optString("name"), it.optString("picture_medium").ifBlank { null }, it.optLong("nb_fan")) }
        } catch (_: Throwable) { emptyList() }
    }

    private fun tracksOf(body: String, genre: String, sub: String, trend: Boolean, relatedTo: String?, source: String): List<MusicCandidate> {
        val arr = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.mapNotNull { o ->
            val title = o.optString("title").ifBlank { return@mapNotNull null }
            val artist = o.optJSONObject("artist")?.optString("name").orEmpty().ifBlank { return@mapNotNull null }
            MusicCandidate(
                title = title, artist = artist, coverUrl = o.optJSONObject("album")?.optString("cover_medium")?.ifBlank { null },
                popularity = pop(o.optLong("rank")), releaseMs = parseDay(o.optString("release_date")),
                genre = genre, subgenre = sub, trend = trend, relatedTo = relatedTo, source = source
            )
        }
    }

    suspend fun deezerTop(c: Context, id: Long, limit: Int, genre: String, sub: String, relatedTo: String?): List<MusicCandidate> = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://api.deezer.com/artist/$id/top?limit=$limit", 3 * DAY) ?: return@withContext emptyList()
            tracksOf(body, genre, sub, false, relatedTo, "deezer-top")
        } catch (_: Throwable) { emptyList() }
    }

    suspend fun deezerChart(c: Context, genreId: Int, limit: Int, genre: String): List<MusicCandidate> = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://api.deezer.com/chart/$genreId/tracks?limit=$limit", 6 * 3600_000L) ?: return@withContext emptyList()
            tracksOf(body, genre, "", true, null, "deezer-chart")
        } catch (_: Throwable) { emptyList() }
    }

    suspend fun deezerChartArtists(c: Context, genreId: Int, limit: Int): List<MusicArtist> = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://api.deezer.com/chart/$genreId/artists?limit=$limit", 12 * 3600_000L) ?: return@withContext emptyList()
            val arr = JSONObject(body).optJSONArray("data") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { MusicArtist(it.optString("name"), it.optString("picture_medium").ifBlank { null }, it.optLong("nb_fan")) }
        } catch (_: Throwable) { emptyList() }
    }

    /** Country top songs of the iTunes store: what is trending now, with release date and genre. */
    suspend fun itunesChart(c: Context, country: String): List<MusicCandidate> = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://itunes.apple.com/$country/rss/topsongs/limit=50/json", 6 * 3600_000L) ?: return@withContext emptyList()
            val entries = JSONObject(body).optJSONObject("feed")?.optJSONArray("entry") ?: return@withContext emptyList()
            (0 until entries.length()).mapNotNull { i ->
                val o = entries.optJSONObject(i) ?: return@mapNotNull null
                val title = o.optJSONObject("im:name")?.optString("label").orEmpty().ifBlank { return@mapNotNull null }
                val artist = o.optJSONObject("im:artist")?.optString("label").orEmpty().ifBlank { return@mapNotNull null }
                val imgs = o.optJSONArray("im:image")
                val cover = if (imgs != null && imgs.length() > 0) big(imgs.optJSONObject(imgs.length() - 1)?.optString("label")) else null
                val rel = parseDay(o.optJSONObject("im:releaseDate")?.optString("label").orEmpty())
                val cat = o.optJSONObject("category")?.optJSONObject("attributes")?.optString("term").orEmpty()
                MusicCandidate(title, artist, cover, 1f - i / 50f, rel, genreOfTag(cat), cat, true, null, "itunes-chart-$country")
            }
        } catch (_: Throwable) { emptyList() }
    }

    /** Newest songs of an artist the listener likes (iTunes search, newest first). */
    suspend fun itunesNewest(c: Context, artist: String, genre: String, sub: String): List<MusicCandidate> = withContext(Dispatchers.IO) {
        try {
            val body = MusicHttp.get(c, "https://itunes.apple.com/search?term=${MusicText.enc(artist)}&media=music&entity=song&limit=25", 12 * 3600_000L) ?: return@withContext emptyList()
            val arr = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            val key = MusicText.artistKey(artist)
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                .filter { val a = MusicText.artistKey(it.optString("artistName")); a.contains(key) || key.contains(a) }
                .map { o ->
                    MusicCandidate(
                        o.optString("trackName"), o.optString("artistName"), big(o.optString("artworkUrl100")), 0.5f,
                        parseDay(o.optString("releaseDate")), genre, sub, false, artist, "itunes-new"
                    )
                }.filter { it.title.isNotBlank() }.sortedByDescending { it.releaseMs }.take(4)
        } catch (_: Throwable) { emptyList() }
    }

    private fun genreOfTag(tag: String): String {
        val low = tag.lowercase(Locale.ROOT)
        return listOf("hip" to "Hip-Hop", "rap" to "Hip-Hop", "dance" to "Electronic", "electronic" to "Electronic", "rock" to "Rock", "alternative" to "Rock",
            "r&b" to "R&B", "soul" to "Soul", "latin" to "Latin", "bollywood" to "Bollywood", "indian" to "Bollywood", "pop" to "Pop", "metal" to "Metal",
            "jazz" to "Jazz", "classical" to "Classical", "country" to "Folk", "folk" to "Folk").firstOrNull { low.contains(it.first) }?.second ?: "Pop"
    }

    /** YouTube Music search: already playable, with view counts as popularity. */
    suspend fun youtube(c: Context, query: String, genre: String, sub: String, relatedTo: String?, n: Int): List<MusicCandidate> = withContext(Dispatchers.IO) {
        try {
            OnlineMusicService.search(query)
                .filter { OnlineMusicService.isAudioOrMusicContent(it.title, it.artist, it.durationSec) && it.durationSec in 60L..720L }
                .take(n).map { t ->
                    MusicCandidate(t.title, t.artist, t.thumbnailUrl, (ln(t.viewCount + 1.0) / ln(1e9)).toFloat().coerceIn(0f, 1f),
                        t.uploadDateEpochMs ?: 0L, genre, sub, false, relatedTo, "youtube", ready = t)
                }
        } catch (t: CancellationException) { throw t } catch (_: Throwable) { emptyList() }
    }

    private val sem = Semaphore(6)
    private val BAD = Regex("\\b(live|cover|karaoke|reaction|8d|nightcore|sped up|speed up|slowed|reverb|instrumental|tutorial|lesson)\\b")

    /** A candidate from a chart / related artist -> the playable YouTube Music track (cached forever once found). */
    suspend fun playable(c: Context, cand: MusicCandidate): OnlineTrackResult? {
        cand.ready?.let { return it }
        val key = MusicText.songKey(cand.artist, cand.title)
        MusicCache.getResolved(c, key)?.let { return it.copy(thumbnailUrl = cand.coverUrl ?: it.thumbnailUrl) }
        return sem.withPermit {
            try {
                val bt = MusicText.baseTitle(cand.title)
                if (bt.isBlank()) return@withPermit null
                val candBad = BAD.findAll(MusicText.norm(cand.title)).map { it.value }.toSet()
                val best = OnlineMusicService.search("${cand.artist} ${cand.title}")
                    .filter { OnlineMusicService.isAudioOrMusicContent(it.title, it.artist, it.durationSec) && it.durationSec in 60L..720L }
                    .firstOrNull { r ->
                        val t = MusicText.baseTitle(r.title)
                        val bad = BAD.findAll(MusicText.norm(r.title)).map { it.value }.toSet()
                        (t.contains(bt) || (t.length > 3 && bt.contains(t))) && bad.all { it in candBad }
                    } ?: return@withPermit null
                MusicCache.putResolved(c, key, best)
                best.copy(thumbnailUrl = cand.coverUrl ?: best.thumbnailUrl)
            } catch (t: CancellationException) { throw t } catch (_: Throwable) { null }
        }
    }
}

// ------------------------------------------------------------------------------------------------ ranking + feed

private class MusicProfile(
    val genres: Map<String, Double>, val subs: Map<String, Double>, val artists: Map<String, Double>,
    val eras: Map<String, Double>, val langs: Map<String, Double>,
    val artistNames: List<String>, val recent: List<MusicTaste.Played>, val heardKeys: Set<String>, val rejected: Set<String>,
    val coldStart: Boolean
) {
    val topGenres: List<String> get() = genres.entries.sortedByDescending { it.value }.map { it.key }
    val topSubs: List<String> get() = subs.entries.sortedByDescending { it.value }.map { it.key }
    val topLanguage: String get() = langs.entries.maxByOrNull { it.value }?.key.orEmpty()
}

private class RankWeights(val g: Double, val s: Double, val a: Double, val era: Double, val pop: Double, val fresh: Double, val trend: Double, val rel: Double, val emerging: Double = 0.0) {
    companion object {
        val BALANCED = RankWeights(0.28, 0.14, 0.12, 0.06, 0.12, 0.08, 0.08, 0.12)
        val TRENDING = RankWeights(0.20, 0.05, 0.0, 0.0, 0.25, 0.20, 0.50, 0.0)
        val NEW = RankWeights(0.20, 0.10, 0.10, 0.0, 0.10, 0.60, 0.10, 0.10)
        val POPULAR = RankWeights(0.35, 0.10, 0.05, 0.05, 0.50, 0.05, 0.15, 0.05)
        val DISCOVER = RankWeights(0.35, 0.20, 0.0, 0.05, 0.0, 0.10, 0.0, 0.10, emerging = 0.25)
        val SIMILAR = RankWeights(0.30, 0.25, 0.10, 0.10, 0.10, 0.05, 0.0, 0.10)
    }
}

object MusicRecoEngine {
    private const val FEED_TTL_MS = 3L * 3600_000L
    private val COLD_GENRES = mapOf("Phonk" to 0.6, "Bollywood" to 0.5, "Hip-Hop" to 0.4, "Pop" to 0.4)
    private val DISCOVERY_GENRES = listOf("Pop", "Hip-Hop", "Electronic", "Rock", "Latin", "R&B", "Jazz", "Bollywood", "Folk", "Soul", "Metal", "Classical")

    private fun norm(m: List<Pair<String, Double>>): Map<String, Double> {
        val mx = m.maxOfOrNull { it.second } ?: return emptyMap()
        return m.associate { it.first to (it.second / mx) }
    }

    private fun profile(c: Context): MusicProfile {
        val g = MusicTaste.top(c, "g:", 8)
        val artists = MusicTaste.top(c, "a:", 10)
        val played = MusicTaste.played(c)
        val now = System.currentTimeMillis()
        val heard = played.filter { now - it.last < 14L * 24 * 3600_000L }.map { MusicText.songKey(it.artist, it.title) }.toSet()
        val rejected = played.filter { MusicTaste.isRejected(c, it.url) }.map { MusicText.songKey(it.artist, it.title) }.toSet()
        val cold = g.isEmpty()
        return MusicProfile(
            genres = if (cold) COLD_GENRES else norm(g), subs = norm(MusicTaste.top(c, "s:", 8)), artists = norm(artists),
            eras = norm(MusicTaste.top(c, "e:", 6)), langs = norm(MusicTaste.top(c, "l:", 4)),
            artistNames = MusicTaste.played(c).groupBy { MusicText.artistKey(it.artist) }.mapNotNull { (k, v) ->
                val s = MusicTaste.score(c, "a:$k"); if (s > 0.15) Triple(v.first().artist, s, k) else null
            }.sortedByDescending { it.second }.map { it.first }.take(8),
            recent = MusicTaste.recent(c, 8), heardKeys = heard, rejected = rejected, coldStart = cold
        )
    }

    private fun eraOf(ms: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
        return "${cal.get(java.util.Calendar.YEAR) / 10 * 10}s"
    }

    private fun score(p: MusicProfile, c: MusicCandidate, w: RankWeights, now: Long): Double {
        val g = p.genres[c.genre] ?: 0.0
        val s = p.subs[c.subgenre] ?: 0.0
        val a = p.artists[MusicText.artistKey(c.artist)] ?: 0.0
        val era = if (c.releaseMs > 0) p.eras[eraOf(c.releaseMs)] ?: 0.0 else 0.0
        val ageDays = if (c.releaseMs > 0) (now - c.releaseMs) / 86_400_000.0 else 400.0
        val fresh = if (c.releaseMs > 0) exp(-ageDays.coerceAtLeast(0.0) / 90.0) else 0.2
        var v = w.g * g + w.s * s + w.a * a + w.era * era + w.pop * c.popularity + w.fresh * fresh +
            w.trend * (if (c.trend) 1.0 else 0.0) + w.rel * (if (c.relatedTo != null) 1.0 else 0.0) + w.emerging * (1.0 - c.popularity) * (if (a == 0.0) 1.0 else 0.0)
        if (MusicText.songKey(c.artist, c.title) in p.heardKeys) v *= 0.35
        return v
    }

    /**
     * Sorted + filtered + artist-diverse picks: no duplicate song (any version), no rejected / skipped song, [perArtist] songs of one artist
     * at most, and no more than 70% of one sub-genre. [taken] is shared between sections so one song never shows twice.
     */
    private fun pick(p: MusicProfile, cands: List<MusicCandidate>, w: RankWeights, limit: Int, perArtist: Int, taken: MutableSet<String>, now: Long, allowHeard: Boolean = false): List<MusicCandidate> {
        val scored = cands.map { it to score(p, it, w, now) }.sortedByDescending { it.second }
        val out = ArrayList<MusicCandidate>()
        val artistCount = HashMap<String, Int>()
        val subCount = HashMap<String, Int>()
        for ((cand, _) in scored) {
            if (out.size >= limit) break
            val key = MusicText.songKey(cand.artist, cand.title)
            if (key in taken || key in p.rejected) continue
            if (!allowHeard && key in p.heardKeys) continue
            val ak = MusicText.artistKey(cand.artist)
            if ((artistCount[ak] ?: 0) >= perArtist) continue
            if (cand.subgenre.isNotBlank() && out.size >= 6 && (subCount[cand.subgenre] ?: 0) >= (limit * 0.7).toInt().coerceAtLeast(4)) continue
            taken.add(key)
            artistCount[ak] = (artistCount[ak] ?: 0) + 1
            subCount[cand.subgenre] = (subCount[cand.subgenre] ?: 0) + 1
            out.add(cand)
        }
        return out
    }

    private suspend fun resolveAll(c: Context, picked: List<MusicCandidate>, limit: Int): List<OnlineTrackResult> = coroutineScope {
        picked.map { cand -> async { cand to MusicSources.playable(c, cand) } }.awaitAll()
            .mapNotNull { (cand, t) -> t }
            .distinctBy { it.videoUrl }.take(limit)
    }

    // ----------------------------------------------------------------------------------- candidate pools

    private class Pool(val all: List<MusicCandidate>, val relatedArtists: List<MusicArtist>)

    private suspend fun gather(c: Context, p: MusicProfile): Pool = coroutineScope {
        val genres = p.topGenres.take(3)
        val subs = p.topSubs.take(3)
        val artists = p.artistNames.take(4)
        fun genreOfArtist(a: String): Pair<String, String> {
            val e = MusicTaste.played(c).firstOrNull { MusicText.artistKey(it.artist) == MusicText.artistKey(a) }?.let { MusicTaste.track(c, it.url) }
            return (e?.optString("g").orEmpty().ifBlank { genres.firstOrNull().orEmpty() }) to e?.optString("s").orEmpty()
        }
        val relatedArtists = java.util.Collections.synchronizedList(ArrayList<MusicArtist>())

        val tops = artists.map { a -> async {
            val dz = MusicSources.deezerArtist(c, a) ?: return@async emptyList<MusicCandidate>()
            val (g, s) = genreOfArtist(a)
            MusicSources.deezerTop(c, dz.id, 8, g, s, a)
        } }
        val related = artists.take(3).map { a -> async {
            val dz = MusicSources.deezerArtist(c, a) ?: return@async emptyList<MusicCandidate>()
            val (g, s) = genreOfArtist(a)
            val rel = MusicSources.deezerRelated(c, dz.id, 6).filter { r -> artists.none { MusicText.artistKey(it) == MusicText.artistKey(r.name) } }
            rel.forEach { relatedArtists.add(MusicArtist(it.name, it.pic, it.fans)) }
            rel.take(4).map { r -> async { MusicSources.deezerTop(c, r.id, 3, g, s, a) } }.awaitAll().flatten()
        } }
        val charts = async {
            val list = ArrayList<MusicCandidate>()
            list += MusicSources.deezerChart(c, 0, 40, "")
            genres.forEach { g -> MusicSources.deezerGenreId(g)?.let { list += MusicSources.deezerChart(c, it, 25, g) } }
            list += MusicSources.itunesChart(c, MusicSources.chartCountry(p.topLanguage))
            list
        }
        val yt = (subs.ifEmpty { genres }).take(3).map { sg -> async {
            val g = genres.firstOrNull { true }.orEmpty()
            val gOfSub = MusicTaste.top(c, "s:", 8).firstOrNull { it.first == sg }?.let { g } ?: g
            MusicSources.youtube(c, "$sg new songs 2026", gOfSub, sg, null, 8) +
                MusicSources.youtube(c, "best $sg songs popular", gOfSub, sg, null, 8)
        } }
        val newest = artists.take(3).map { a -> async { val (g, s) = genreOfArtist(a); MusicSources.itunesNewest(c, a, g, s) } }

        val all = (tops + related + listOf(charts) + yt + newest).awaitAll().flatten()
        Pool(all, relatedArtists.distinctBy { it.name })
    }

    private fun shortTitle(t: String) = MusicText.baseTitle(t).split(' ').filter { it.isNotBlank() }.take(4).joinToString(" ").replaceFirstChar { it.uppercase() }

    // ----------------------------------------------------------------------------------- public API

    fun cachedFeed(c: Context): List<MusicFeedSection> = MusicCache.loadFeed(c)?.second.orEmpty()

    /** The key that makes versions of one song the same song (used to show every song once). */
    fun songKeyOf(artist: String, title: String): String = MusicText.songKey(artist, title)

    /** True when the saved feed is older than 3 hours, missing, or the listener played something new since. */
    fun feedNeedsRefresh(c: Context): Boolean {
        val saved = MusicCache.loadFeed(c) ?: return true
        return System.currentTimeMillis() - saved.first > FEED_TTL_MS
    }

    /**
     * The Music Feed: Recommended for you, Based on recently played, Similar songs, Trending music, Popular in your genres, New releases,
     * Similar artists, Discover new music and Genre based picks. Each finished section is passed to [onSection] at once, so the screen
     * fills while the rest is still loading. Without network the last saved feed is returned.
     */
    suspend fun feed(c: Context, taken: MutableSet<String>, onSection: (suspend (MusicFeedSection) -> Unit)? = null): List<MusicFeedSection> = withContext(Dispatchers.Default) {
        val p = profile(c)
        val pool = try { gather(c, p) } catch (t: CancellationException) { throw t } catch (_: Throwable) { Pool(emptyList(), emptyList()) }
        if (pool.all.isEmpty()) return@withContext cachedFeed(c)
        val now = System.currentTimeMillis()
        val out = ArrayList<MusicFeedSection>()
        suspend fun add(id: String, title: String, picked: List<MusicCandidate>, limit: Int) {
            val tracks = resolveAll(c, picked, limit)
            if (tracks.size >= 3) {
                val s = MusicFeedSection(id, title, tracks)
                out.add(s)
                onSection?.invoke(s)
            }
        }
        val last = p.recent.firstOrNull()
        val lastMeta = last?.let { MusicTaste.track(c, it.url) }
        val lastSub = lastMeta?.optString("s").orEmpty()
        val lastGenre = lastMeta?.optString("g").orEmpty()
        val lastArtistKey = last?.let { MusicText.artistKey(it.artist) }.orEmpty()

        add("recommended", "Recommended for you", pick(p, pool.all, RankWeights.BALANCED, 40, 2, taken, now), 32)
        if (last != null) {
            val basedOn = pool.all.filter { it.relatedTo != null && MusicText.artistKey(it.relatedTo) == lastArtistKey || MusicText.artistKey(it.artist) == lastArtistKey }
            add("recent", "Based on ${shortTitle(last.title).ifBlank { "your last song" }}", pick(p, basedOn, RankWeights.SIMILAR, 14, 3, taken, now), 12)
            val similar = pool.all.filter { (lastSub.isNotBlank() && it.subgenre == lastSub) || (lastGenre.isNotBlank() && it.genre == lastGenre) }
            add("similar", "Similar songs", pick(p, similar, RankWeights.SIMILAR, 14, 2, taken, now), 12)
        }
        add("trending", "Trending music", pick(p, pool.all.filter { it.trend }, RankWeights.TRENDING, 14, 2, taken, now), 12)
        val myGenres = p.topGenres.take(3).toSet()
        add("popular", "Popular in your genres", pick(p, pool.all.filter { it.genre in myGenres }, RankWeights.POPULAR, 14, 2, taken, now), 12)
        add("new", "New releases", pick(p, pool.all.filter { it.releaseMs > 0 && now - it.releaseMs < 90L * 86_400_000L }, RankWeights.NEW, 14, 2, taken, now), 12)
        if (pool.relatedArtists.isNotEmpty()) {
            val s = MusicFeedSection("artists", "Similar artists", emptyList(), pool.relatedArtists.sortedByDescending { it.fans }.take(14))
            out.add(s); onSection?.invoke(s)
        }
        add("discover", "Discover new music", pick(p, pool.all.filter { (p.artists[MusicText.artistKey(it.artist)] ?: 0.0) == 0.0 }, RankWeights.DISCOVER, 14, 1, taken, now), 12)
        p.topGenres.take(2).forEach { g ->
            add("genre_$g", "$g picks", pick(p, pool.all.filter { it.genre == g }, RankWeights.BALANCED, 12, 2, taken, now), 10)
        }
        if (out.isNotEmpty()) MusicCache.saveFeed(c, out)
        out
    }

    /** More sections for the endless scroll: the listener's genres first (newest taste first), new categories later. */
    suspend fun moreSections(c: Context, batch: Int, taken: MutableSet<String>): List<MusicFeedSection> = withContext(Dispatchers.Default) {
        val p = profile(c)
        val mine = p.topGenres
        val rotation = if (batch <= 6 && mine.isNotEmpty()) mine else (mine + DISCOVERY_GENRES.filter { it !in mine }.shuffled())
        val g = rotation[batch % rotation.size]
        val now = System.currentTimeMillis()
        val out = ArrayList<MusicFeedSection>()
        val cands = coroutineScope {
            val a = async { MusicSources.deezerGenreId(g)?.let { MusicSources.deezerChart(c, it, 30, g) }.orEmpty() }
            val b = async { MusicSources.youtube(c, "$g popular songs", g, "", null, 10) + MusicSources.youtube(c, "$g new songs 2026", g, "", null, 10) }
            val d = async {
                val seed = p.artistNames.getOrNull(batch % (p.artistNames.size.coerceAtLeast(1)))
                if (seed == null) emptyList() else MusicSources.deezerArtist(c, seed)?.let { dz ->
                    MusicSources.deezerRelated(c, dz.id, 5).take(4).map { r -> async { MusicSources.deezerTop(c, r.id, 3, g, "", seed) } }.awaitAll().flatten()
                }.orEmpty()
            }
            a.await() + b.await() + d.await()
        }
        val popular = resolveAll(c, pick(p, cands, RankWeights.POPULAR, 14, 2, taken, now), 12)
        if (popular.size >= 3) out.add(MusicFeedSection("more_pop_${g}_$batch", "$g popular", popular))
        val fresh = resolveAll(c, pick(p, cands.filter { it.releaseMs > 0 }, RankWeights.NEW, 12, 2, taken, now), 10)
        if (fresh.size >= 3) out.add(MusicFeedSection("more_new_${g}_$batch", "New in $g", fresh))
        val rel = resolveAll(c, pick(p, cands.filter { it.relatedTo != null }, RankWeights.SIMILAR, 12, 2, taken, now), 10)
        if (rel.size >= 3) out.add(MusicFeedSection("more_rel_${g}_$batch", "Because you like ${p.artistNames.getOrNull(batch % p.artistNames.size.coerceAtLeast(1)) ?: g}", rel))
        out
    }

    /** The queue that follows a played song: same kind of music (genre / sub-genre / era / language), different artists, some fresh. */
    suspend fun queueAfterPlay(c: Context, seed: OnlineTrackResult, count: Int = 30): List<OnlineTrackResult> = withContext(Dispatchers.Default) {
        val meta = withTimeoutOrNull(4000L) { MusicMetadataResolver.resolve(c, seed.artist, seed.title) } ?: MusicMeta.UNKNOWN
        val base = profile(c)
        val sg = meta.genre.ifBlank { base.topGenres.firstOrNull().orEmpty() }
        val ss = meta.subgenre
        // the song being played counts more than the older taste while its queue is built
        val p = MusicProfile(
            genres = base.genres + (sg to 1.0), subs = if (ss.isNotBlank()) base.subs + (ss to 1.0) else base.subs,
            artists = base.artists + (MusicText.artistKey(seed.artist) to 1.0), eras = if (meta.era.isNotBlank()) base.eras + (meta.era to 1.0) else base.eras,
            langs = base.langs, artistNames = (listOf(seed.artist) + base.artistNames).distinct(), recent = base.recent,
            heardKeys = base.heardKeys, rejected = base.rejected, coldStart = false
        )
        val now = System.currentTimeMillis()
        val cands = coroutineScope {
            val own = async {
                MusicSources.deezerArtist(c, seed.artist)?.let { dz ->
                    val top = MusicSources.deezerTop(c, dz.id, 6, sg, ss, seed.artist)
                    val rel = MusicSources.deezerRelated(c, dz.id, 8).take(6).map { r -> async { MusicSources.deezerTop(c, r.id, 3, sg, ss, seed.artist) } }.awaitAll().flatten()
                    top + rel
                }.orEmpty()
            }
            val yt = async {
                val q = if (ss.isNotBlank()) "$ss songs" else if (sg.isNotBlank()) "$sg songs" else "${seed.artist} songs"
                MusicSources.youtube(c, q, sg, ss, null, 12) + MusicSources.youtube(c, "${seed.artist} songs", sg, ss, seed.artist, 6)
            }
            val chart = async { MusicSources.deezerGenreId(sg)?.let { MusicSources.deezerChart(c, it, 25, sg) }.orEmpty() }
            own.await() + yt.await() + chart.await()
        }
        val taken = mutableSetOf(MusicText.songKey(seed.artist, seed.title))
        val similarN = (count * 0.75).toInt()
        val similar = pick(p, cands, RankWeights.SIMILAR, similarN + 8, 2, taken, now)
        val fresh = pick(p, cands.filter { it.trend || (it.releaseMs > 0 && now - it.releaseMs < 120L * 86_400_000L) }, RankWeights.NEW, count - similarN + 4, 1, taken, now)
        val rs = resolveAll(c, similar, similarN + 4)
        val rf = resolveAll(c, fresh, count - similarN + 2)
        // 3 similar, then 1 fresh; never the same artist twice in a row
        val merged = ArrayList<OnlineTrackResult>()
        var i = 0; var j = 0
        while ((i < rs.size || j < rf.size) && merged.size < count) {
            repeat(3) { if (i < rs.size) merged.add(rs[i++]) }
            if (j < rf.size) merged.add(rf[j++])
        }
        val spaced = ArrayList<OnlineTrackResult>()
        val rest = merged.filter { it.videoUrl != seed.videoUrl }.toMutableList()
        var prev = MusicText.artistKey(seed.artist)
        while (rest.isNotEmpty() && spaced.size < count) {
            val idx = rest.indexOfFirst { MusicText.artistKey(it.artist) != prev }.let { if (it < 0) 0 else it }
            val t = rest.removeAt(idx)
            spaced.add(t)
            prev = MusicText.artistKey(t.artist)
        }
        spaced
    }

    /** Exactly the extra songs for the speed dial: different artists / genres, nothing the listener heard lately. */
    suspend fun speedDialExtras(c: Context, count: Int = 2): List<OnlineTrackResult> = withContext(Dispatchers.Default) {
        val p = profile(c)
        val pool = try { gather(c, p) } catch (t: CancellationException) { throw t } catch (_: Throwable) { return@withContext emptyList() }
        val taken = mutableSetOf<String>()
        val now = System.currentTimeMillis()
        val picks = ArrayList<MusicCandidate>()
        for (g in p.topGenres.take(count)) picks += pick(p, pool.all.filter { it.genre == g }, RankWeights.BALANCED, 1, 1, taken, now)
        if (picks.size < count) picks += pick(p, pool.all, RankWeights.BALANCED, count - picks.size, 1, taken, now)
        resolveAll(c, picks, count)
    }

    /** Artists for the Artists tab: replayed ones (with picture), similar ones and the popular ones of the listener's genres. */
    suspend fun artistSections(c: Context): List<Pair<String, List<MusicArtist>>> = withContext(Dispatchers.Default) {
        val p = profile(c)
        val out = ArrayList<Pair<String, List<MusicArtist>>>()
        val mine = coroutineScope {
            MusicTaste.repeatedArtists(c, 2).map { it.first }.take(12).map { a -> async { MusicSources.deezerArtist(c, a)?.let { MusicArtist(it.name, it.pic, it.fans) } ?: MusicArtist(a, null, 0) } }.awaitAll()
        }
        if (mine.isNotEmpty()) out.add("Artists you replay" to mine)
        val pool = try { gather(c, p) } catch (t: CancellationException) { throw t } catch (_: Throwable) { Pool(emptyList(), emptyList()) }
        if (pool.relatedArtists.isNotEmpty()) out.add("Similar to your artists" to pool.relatedArtists.sortedByDescending { it.fans }.take(15))
        for (g in p.topGenres.take(3)) {
            val id = MusicSources.deezerGenreId(g) ?: continue
            val list = MusicSources.deezerChartArtists(c, id, 15)
            if (list.isNotEmpty()) out.add("Popular in $g" to list)
        }
        out
    }

    /** Artists the listener replays with at least [minSongs] different songs, and his top genres: the seeds for the Albums tab. */
    fun albumSeeds(c: Context): Pair<List<String>, List<String>> {
        val p = profile(c)
        val rep = MusicTaste.repeatedArtists(c, 4).map { it.first }.ifEmpty { p.artistNames.take(2) }.take(3)
        return rep to p.topGenres.take(3)
    }

    fun topGenres(c: Context): List<String> = profile(c).topGenres.take(4)
    fun topSubgenres(c: Context): List<String> = profile(c).topSubs.take(4)
    fun recentArtists(c: Context, n: Int): List<String> = MusicTaste.recent(c, n * 2).map { it.artist }.filter { it.isNotBlank() }.distinct().take(n)
}
