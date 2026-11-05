package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * YouTube-like personalised feed for Online Video.
 *
 * What counts (all stored locally): the videos the user PLAYS - their topic / game, their creator, and how much of the
 * video was watched (half or more = "liked": the creator's newest videos and the same topic come back on the next
 * refresh). What the user only SEARCHES is never used. A brand-new user (no history) gets a broad, mixed feed.
 * Rules: a refresh mixes the creator's latest videos, trending videos of the same topic from other creators, other
 * games / related topics, tech / AI / podcast / music and popular channels "here and there" (see
 * [OnlineRecommendationMixer.scatterMix]); one creator never fills the screen; topics / creators that are no longer
 * played fade away over a few app starts; new uploads of subscribed channels are always on top with a NEW tag.
 */
object OnlineVideoRecommender {
    private const val PREFS = "viaura_online_video_history"
    private const val KEY_WATCHED = "watched_channels"      // JSON object: channel -> count
    private const val KEY_SHOWN = "shown_counts"            // JSON object: url -> times shown
    private const val KEY_SHOWN_CH = "shown_channel_counts" // JSON object: channel -> times shown
    private const val KEY_OPENED = "opened_urls"            // JSON array
    private const val KEY_COMPLETED = "completed_urls"      // JSON array: videos watched to the end
    private const val KEY_META = "opened_meta"              // JSON object: url -> {t: title, c: channel}
    private const val KEY_CREDITED = "credited_urls"        // JSON array: videos watched half or more (already counted)
    private const val KEY_LIKED = "liked_channels"          // JSON object: channel -> score (watched half or more)
    private const val KEY_LIKED_ORDER = "liked_order"       // JSON array, most recent first
    private const val KEY_CHANNEL_URLS = "channel_urls"     // JSON object: channel name -> channel url
    private const val MAX_COMPLETED = 3000
    private const val MAX_META = 40
    private const val MAX_CREDITED = 300
    private const val SUBSCRIBED_QUOTA = 12                 // max NEW uploads of subscribed channels at the top of a batch
    private const val SUBSCRIBED_CHANNELS_PER_BATCH = 24
    private const val SUBSCRIBED_PER_CHANNEL = 3
    private const val NEW_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    private const val MAX_SHOWN = 800
    private const val IGNORE_AFTER = 3
    private const val IGNORE_CHANNEL_AFTER = 10             // shown this often and never opened -> stop showing the channel
    private const val CHANNEL_CAP = 2                       // videos of one creator in a batch / on screen
    private const val LIKED_CHANNEL_VIDEOS = 3              // latest videos of the creator whose video was watched half or more
    private const val TAP_WEIGHT = 1.0                      // a tap alone is a weak signal
    private const val WATCH_WEIGHT = 3.0                    // watching half or more is the strong one
    private const val LIKE_SCORE = 3.0
    private const val CHANNEL_FADE = 0.8
    private const val MIN_LIKE_SCORE = 0.9

    private val DISCOVERY_TOPICS = listOf(
        "trending today", "music video", "podcast episode", "tech review", "documentary", "cooking recipe",
        "travel vlog", "movie trailer", "science explained", "sports highlights", "comedy sketch", "news today",
        "gaming gameplay", "DIY project", "fitness workout", "history explained", "short film", "street food",
        "car review", "stand up comedy", "nature 4k", "cricket highlights", "bollywood", "anime"
    )

    // ---------- storage ----------
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readObj(c: Context, key: String): JSONObject =
        try { JSONObject(prefs(c).getString(key, "{}") ?: "{}") } catch (_: Throwable) { JSONObject() }

    private fun readArr(c: Context, key: String): JSONArray =
        try { JSONArray(prefs(c).getString(key, "[]") ?: "[]") } catch (_: Throwable) { JSONArray() }

    @Synchronized
    fun recordOpened(c: Context, videoUrl: String, channel: String, title: String = "") {
        // a tap alone is a weak signal; the topic gets its real weight once half of the video is watched
        if (title.isNotBlank()) OnlineInterestProfile.record(c, OnlineInterestProfile.KIND_VIDEO, title, TAP_WEIGHT)
        val opened = readArr(c, KEY_OPENED)
        val list = (0 until opened.length()).map { opened.optString(it) }.toMutableList()
        if (!list.contains(videoUrl)) list.add(videoUrl)
        val trimmed = if (list.size > 1500) list.takeLast(1500) else list
        val watched = readObj(c, KEY_WATCHED)
        if (channel.isNotBlank()) watched.put(channel, watched.optInt(channel, 0) + 1)
        // remembers what this video is, so watching half of it later can credit its topic and its creator
        val meta = readObj(c, KEY_META)
        meta.remove(videoUrl)
        meta.put(videoUrl, JSONObject().put("t", title).put("c", channel))
        if (meta.length() > MAX_META) {
            meta.keys().asSequence().toList().take(meta.length() - MAX_META).forEach { meta.remove(it) }
        }
        prefs(c).edit()
            .putString(KEY_OPENED, JSONArray(trimmed).toString())
            .putString(KEY_WATCHED, watched.toString())
            .putString(KEY_META, meta.toString())
            .apply()
    }

    // In-memory copy of the completed list so playback ticks can call recordCompleted() cheaply.
    @Volatile private var completedCache: MutableSet<String>? = null

    private fun completedSet(c: Context): MutableSet<String> {
        completedCache?.let { return it }
        return synchronized(this) {
            completedCache ?: run {
                val arr = readArr(c, KEY_COMPLETED)
                val set = LinkedHashSet<String>()
                for (i in 0 until arr.length()) set.add(arr.optString(i))
                completedCache = set
                set
            }
        }
    }

    /** Marks an online video as watched to the end; it is not recommended again afterwards. */
    fun recordCompleted(c: Context, videoUrl: String) {
        if (videoUrl.isBlank()) return
        synchronized(this) {
            val set = completedSet(c)
            if (!set.add(videoUrl)) return
            while (set.size > MAX_COMPLETED) set.remove(set.first())
            prefs(c).edit().putString(KEY_COMPLETED, JSONArray(set.toList()).toString()).apply()
        }
    }

    fun isCompleted(c: Context, videoUrl: String): Boolean = videoUrl in completedSet(c)

    // In-memory copy of "already counted as watched half or more", so the playback ticks stay cheap.
    @Volatile private var creditedCache: MutableSet<String>? = null

    private fun creditedSet(c: Context): MutableSet<String> {
        creditedCache?.let { return it }
        return synchronized(this) {
            creditedCache ?: run {
                val arr = readArr(c, KEY_CREDITED)
                val set = LinkedHashSet<String>()
                for (i in 0 until arr.length()) set.add(arr.optString(i))
                creditedCache = set
                set
            }
        }
    }

    /** Marks [videoUrl] as counted; returns what the video was ({t, c}) the first time, null afterwards. */
    @Synchronized
    private fun takeCredit(c: Context, videoUrl: String): JSONObject? {
        val set = creditedSet(c)
        if (!set.add(videoUrl)) return null
        while (set.size > MAX_CREDITED) set.remove(set.first())
        prefs(c).edit().putString(KEY_CREDITED, JSONArray(set.toList()).toString()).apply()
        return readObj(c, KEY_META).optJSONObject(videoUrl)
    }

    /**
     * Called from the playback ticks once an online video is watched half or more: the user liked it, so its topic /
     * game gets the strong weight and its creator becomes the one whose latest videos come back on the next refresh.
     */
    fun recordWatchedHalf(c: Context, videoUrl: String) {
        if (videoUrl.isBlank() || videoUrl in creditedSet(c)) return
        val meta = takeCredit(c, videoUrl) ?: return
        val title = meta.optString("t")
        val channel = meta.optString("c")
        if (title.isNotBlank()) OnlineInterestProfile.record(c, OnlineInterestProfile.KIND_VIDEO, title, WATCH_WEIGHT)
        if (channel.isNotBlank()) likeChannel(c, channel)
    }

    @Synchronized
    private fun likeChannel(c: Context, channel: String) {
        val liked = readObj(c, KEY_LIKED)
        liked.put(channel, liked.optDouble(channel, 0.0) + LIKE_SCORE)
        val order = readArr(c, KEY_LIKED_ORDER).let { a -> (0 until a.length()).map { a.optString(it) } }
            .filter { !it.equals(channel, true) }.toMutableList()
        order.add(0, channel)
        prefs(c).edit()
            .putString(KEY_LIKED, liked.toString())
            .putString(KEY_LIKED_ORDER, JSONArray(order.take(10)).toString())
            .apply()
    }

    /** The creator of the video the user watched half or more most recently (and not forgotten since), if any. */
    private fun likedChannelName(c: Context): String? {
        val liked = readObj(c, KEY_LIKED)
        val order = readArr(c, KEY_LIKED_ORDER).let { a -> (0 until a.length()).map { a.optString(it) } }
        return order.firstOrNull { liked.optDouble(it, 0.0) >= MIN_LIKE_SCORE }
    }

    @Volatile private var channelsFaded = false

    /**
     * Call when the Online Video feed is opened. Once per app start the interests that were not played this time fade
     * (topics in [OnlineInterestProfile], liked creators here), so a creator / topic the user stopped watching is
     * pushed less and less and is gone after a few starts; related categories are still shown by the normal mix.
     */
    fun onFeedOpened(c: Context) {
        OnlineInterestProfile.fadeForNewSession(c)
        synchronized(this) {
            if (channelsFaded) return
            channelsFaded = true
            val liked = readObj(c, KEY_LIKED)
            liked.keys().asSequence().toList().forEach { k ->
                val v = liked.optDouble(k, 0.0) * CHANNEL_FADE
                if (v < MIN_LIKE_SCORE) liked.remove(k) else liked.put(k, v)
            }
            val order = readArr(c, KEY_LIKED_ORDER).let { a -> (0 until a.length()).map { a.optString(it) } }
                .filter { liked.has(it) }
            prefs(c).edit()
                .putString(KEY_LIKED, liked.toString())
                .putString(KEY_LIKED_ORDER, JSONArray(order).toString())
                .apply()
        }
    }

    /** Call with the urls that were actually put on screen in a feed batch. */
    @Synchronized
    fun recordShown(c: Context, urls: Collection<String>) {
        if (urls.isEmpty()) return
        val shown = readObj(c, KEY_SHOWN)
        urls.forEach { shown.put(it, shown.optInt(it, 0) + 1) }
        if (shown.length() > MAX_SHOWN) {
            val keys = shown.keys().asSequence().toList()
            keys.take(shown.length() - MAX_SHOWN).forEach { shown.remove(it) }
        }
        prefs(c).edit().putString(KEY_SHOWN, shown.toString()).apply()
    }

    /** Like [recordShown], and also counts how often each creator was put on screen (a creator that is shown again and again but never opened is dropped). */
    @Synchronized
    fun recordShownResults(c: Context, items: Collection<OnlineTrackResult>) {
        if (items.isEmpty()) return
        recordShown(c, items.map { it.videoUrl })
        val byChannel = readObj(c, KEY_SHOWN_CH)
        items.forEach { r ->
            val ch = r.artist.trim()
            if (ch.isNotBlank()) byChannel.put(ch, byChannel.optInt(ch, 0) + 1)
        }
        if (byChannel.length() > 300) {
            byChannel.keys().asSequence().toList().take(byChannel.length() - 300).forEach { byChannel.remove(it) }
        }
        prefs(c).edit().putString(KEY_SHOWN_CH, byChannel.toString()).apply()
    }

    private fun topChannels(c: Context, limit: Int): List<String> {
        val watched = readObj(c, KEY_WATCHED)
        return watched.keys().asSequence().map { it to watched.optInt(it) }
            .sortedByDescending { it.second }.map { it.first }.take(limit).toList()
    }

    /** Creators that were put on screen many times but never opened: not recommended any more (subscribed ones excepted). */
    private fun ignoredChannels(c: Context): Set<String> {
        val shownCh = readObj(c, KEY_SHOWN_CH)
        val watched = readObj(c, KEY_WATCHED)
        return shownCh.keys().asSequence()
            .filter { shownCh.optInt(it, 0) >= IGNORE_CHANNEL_AFTER && watched.optInt(it, 0) == 0 }
            .map { it.lowercase(Locale.ROOT) }.toSet()
    }

    /** Channel page url of a creator name: subscriptions first, then a remembered one, else found once by searching the name. */
    private suspend fun resolveChannelUrl(c: Context, name: String): String? {
        SubscribedChannelsManager.getAll(c).firstOrNull { it.name.equals(name, true) }?.let { return it.channelUrl }
        readObj(c, KEY_CHANNEL_URLS).optString(name).takeIf { it.isNotBlank() }?.let { return it }
        val channels = try {
            OnlineMusicService.searchMusicPage(name).second.channelResults
        } catch (t: kotlinx.coroutines.CancellationException) {
            throw t
        } catch (_: Throwable) {
            emptyList()
        }
        val hit = channels.firstOrNull { it.name.equals(name, true) }
            ?: channels.firstOrNull { it.name.contains(name, true) || name.contains(it.name, true) }
            ?: return null
        synchronized(this) {
            val map = readObj(c, KEY_CHANNEL_URLS)
            map.put(name, hit.channelUrl)
            prefs(c).edit().putString(KEY_CHANNEL_URLS, map.toString()).apply()
        }
        return hit.channelUrl
    }

    // ---------- feed ----------
    /**
     * One batch of feed videos. [excludeUrls] = already on screen, [usedQueries] is shared between calls so
     * repeated batches (infinite scroll) keep returning different videos. [onScreenChannels] = how many videos of each
     * creator (lower case name) are on screen already: a creator with [CHANNEL_CAP] or more there gets no more.
     */
    suspend fun fetchFeedBatch(
        context: Context,
        excludeUrls: Set<String>,
        usedQueries: MutableSet<String>,
        total: Int = 20,
        onScreenChannels: Map<String, Int> = emptyMap()
    ): List<OnlineTrackResult> = coroutineScope {
        val favs = topChannels(context, 4)
        val shown = readObj(context, KEY_SHOWN)
        val opened = readArr(context, KEY_OPENED).let { a -> (0 until a.length()).map { a.optString(it) }.toSet() }

        val focus = OnlineInterestProfile.focus(context, OnlineInterestProfile.KIND_VIDEO, 2)
        if (focus.isNotEmpty()) {
            return@coroutineScope fetchInterestBatch(context, focus, excludeUrls, usedQueries, total, shown, opened, onScreenChannels)
        }

        val similarQueries = if (favs.isEmpty()) emptyList() else
            List(minOf(3, favs.size + 1)) { OnlineRecommendationMixer.freshQuery(favs, usedQueries) }.filterNotNull()
        val discoveryQueries = List(if (favs.isEmpty()) 5 else 3) {
            OnlineRecommendationMixer.freshQuery(DISCOVERY_TOPICS, usedQueries)
        }.filterNotNull()

        suspend fun run(queries: List<String>): List<List<OnlineTrackResult>> =
            queries.map { q -> async(Dispatchers.IO) { OnlineMusicService.search(q) } }.awaitAll()

        val similarRaw = run(similarQueries)
        val discoveryRaw = run(discoveryQueries)

        fun ok(r: OnlineTrackResult): Boolean =
            r.videoUrl.isNotBlank() && r.title.isNotBlank() &&
                r.videoUrl !in excludeUrls &&
                r.durationSec >= 60L && r.durationSec <= 4 * 3600L &&
                !isCompleted(context, r.videoUrl) &&
                !(shown.optInt(r.videoUrl, 0) >= IGNORE_AFTER && r.videoUrl !in opened) &&
                onScreenChannels.getOrDefault(r.artist.lowercase(Locale.ROOT), 0) < CHANNEL_CAP

        // per query: drop bad ones, cap each channel to 2 so one channel cannot flood a query's results
        fun clean(lists: List<List<OnlineTrackResult>>): List<List<OnlineTrackResult>> = lists.map { l ->
            val perChannel = HashMap<String, Int>()
            l.filter { ok(it) }.filter {
                val key = it.artist.lowercase(Locale.ROOT)
                val n = perChannel.getOrDefault(key, 0)
                perChannel[key] = n + 1
                n < 2
            }
        }

        val subscribed = fetchSubscribedUploads(context, excludeUrls, opened)
        val subscribedUrls = subscribed.map { it.videoUrl }.toSet()

        val similar = OnlineRecommendationMixer.roundRobin(clean(similarRaw)).distinctBy { it.videoUrl }
            .filter { it.videoUrl !in subscribedUrls }
        val discovery = OnlineRecommendationMixer.roundRobin(clean(discoveryRaw)).distinctBy { it.videoUrl }
            .filter { it.videoUrl !in subscribedUrls }
        val rest = (total - subscribed.size).coerceAtLeast(0)
        val mixed = if (favs.isEmpty()) discovery.take(rest)
        else OnlineRecommendationMixer.mix(similar = similar, discovery = discovery, total = rest)
        // NEW uploads of subscribed channels stay on top, the personalised / discovery mix follows.
        subscribed + spreadChannels(mixed)
    }

    /**
     * Feed batch built around what the user PLAYED last. The places are scattered through the batch (not in blocks):
     *  - 3 latest videos of the creator whose video was watched half or more,
     *  - 3 trending / popular videos of the same topic or game from other creators,
     *  - 3 videos of related topics (watched Minecraft -> other games),
     *  - 4 from other categories (tech, AI, podcast, popular music, a trending topic),
     *  - 3 of the user's other interests, 2 from popular channels.
     * NEW uploads of subscribed channels always go first. All searches run at the same time.
     */
    private suspend fun fetchInterestBatch(
        context: Context,
        focus: List<OnlineInterestProfile.Topic>,
        excludeUrls: Set<String>,
        usedQueries: MutableSet<String>,
        total: Int,
        shown: JSONObject,
        opened: Set<String>,
        onScreenChannels: Map<String, Int>
    ): List<OnlineTrackResult> = coroutineScope {
        val plain = listOf("", "new", "latest", "best")
        val trendy = listOf("trending", "popular", "new", "best", "top")
        val focusNames = focus.map { it.name }
        val sameBase = OnlineInterestProfile.videoQueryFor(focusNames.first())
        val tasteBases = focusNames.drop(1).map { OnlineInterestProfile.videoQueryFor(it) }
        val relatedBases = focusNames.flatMap { OnlineInterestProfile.relatedTopics(it) }
            .filter { r -> focusNames.none { it.equals(r, true) } }.distinct()
            .map { OnlineInterestProfile.videoQueryFor(it) }
        val avoid = focus.map { it.category }.toSet()

        val sameQueries = List(3) { OnlineRecommendationMixer.freshQuery(listOf(sameBase), usedQueries, modifiers = trendy) }.filterNotNull()
        val tasteQueries = List(2) { OnlineRecommendationMixer.freshQuery(tasteBases, usedQueries, modifiers = plain) }.filterNotNull()
        val relatedQueries = List(3) { OnlineRecommendationMixer.freshQuery(relatedBases, usedQueries, modifiers = plain) }.filterNotNull()
        val diverseQueries = OnlineInterestProfile.diverseQueries(OnlineInterestProfile.KIND_VIDEO, avoid, 4) { opts ->
            OnlineRecommendationMixer.freshQuery(opts, usedQueries, modifiers = listOf("")) ?: opts.random()
        }
        val channelPicked = List(2) {
            OnlineRecommendationMixer.freshQuery(OnlineInterestProfile.POPULAR_VIDEO_CHANNELS, usedQueries, modifiers = listOf("latest video", "new video", "best video"))
        }.filterNotNull()

        suspend fun run(queries: List<String>): List<List<OnlineTrackResult>> = coroutineScope {
            queries.map { q -> async(Dispatchers.IO) { OnlineMusicService.search(q) } }.awaitAll()
        }
        val sameJob = async { run(sameQueries) }
        val tasteJob = async { run(tasteQueries) }
        val relatedJob = async { run(relatedQueries) }
        val diverseJob = async { run(diverseQueries) }
        val channelJob = async { run(channelPicked) }
        val subscribedJob = async { fetchSubscribedUploads(context, excludeUrls, opened) }
        val likedName = likedChannelName(context)
        val likedJob = async { fetchLikedChannelLatest(context, likedName, excludeUrls, opened) }
        val ignored = ignoredChannels(context)
        val subscribedNames = SubscribedChannelsManager.getAll(context).map { it.name.lowercase(Locale.ROOT) }.toSet()

        fun ok(r: OnlineTrackResult): Boolean {
            val ch = r.artist.lowercase(Locale.ROOT)
            return r.videoUrl.isNotBlank() && r.title.isNotBlank() &&
                r.videoUrl !in excludeUrls &&
                r.durationSec >= 60L && r.durationSec <= 4 * 3600L &&
                !isCompleted(context, r.videoUrl) &&
                !(shown.optInt(r.videoUrl, 0) >= IGNORE_AFTER && r.videoUrl !in opened) &&
                onScreenChannels.getOrDefault(ch, 0) < CHANNEL_CAP &&
                (ch !in ignored || ch in subscribedNames)
        }

        fun pool(lists: List<List<OnlineTrackResult>>): List<OnlineTrackResult> =
            // one result of each query in turn, so no single query / creator dominates a pool
            OnlineRecommendationMixer.roundRobin(lists.map { l -> l.filter { ok(it) } }).distinctBy { it.videoUrl }

        val subscribed = subscribedJob.await()
        val taken = HashSet<String>(subscribed.map { it.videoUrl })
        val liked = likedJob.await().filter { taken.add(it.videoUrl) }
        val same = pool(sameJob.await()).filter { r -> likedName == null || !r.artist.equals(likedName, true) }
            .filter { taken.add(it.videoUrl) }
        val related = pool(relatedJob.await()).filter { taken.add(it.videoUrl) }
        val diverse = pool(diverseJob.await()).filter { taken.add(it.videoUrl) }
        val taste = pool(tasteJob.await()).filter { taken.add(it.videoUrl) }
        val channelRaw = channelJob.await()
        val popular = pool(channelRaw.mapIndexed { i, list ->
            val name = channelPicked[i].substringBefore(" latest").substringBefore(" new").substringBefore(" best")
                .lowercase(Locale.ROOT)
            list.filter { it.artist.lowercase(Locale.ROOT).contains(name) }.ifEmpty { list.take(2) }
        }).filter { taken.add(it.videoUrl) }

        val rest = (total - subscribed.size).coerceAtLeast(0)
        // a few more than needed: the per-creator cap below removes some
        val mixed = OnlineRecommendationMixer.scatterMix(
            pools = listOf(liked, same, related, diverse, taste, popular),
            quotas = listOf(LIKED_CHANNEL_VIDEOS, 3, 3, 4, 3, 2),
            total = rest + 8
        )
        val counts = HashMap<String, Int>()
        subscribed.forEach { val k = it.artist.lowercase(Locale.ROOT); counts[k] = counts.getOrDefault(k, 0) + 1 }
        val capped = mixed.filter { r ->
            val key = r.artist.lowercase(Locale.ROOT)
            val n = counts.getOrDefault(key, 0)
            // the creator whose video was watched half or more may bring his 3 latest videos; nobody else more than 2
            val cap = if (likedName != null && key == likedName.lowercase(Locale.ROOT)) LIKED_CHANNEL_VIDEOS else CHANNEL_CAP
            counts[key] = n + 1
            n < cap
        }.take(rest)
        // NEW uploads stay exactly on top; the mixed part is only separated so one creator is never twice in a row
        (subscribed + spreadChannels(capped)).distinctBy { it.videoUrl }
    }

    /** The newest videos of the creator whose video the user watched half or more (not yet opened / finished). */
    private suspend fun fetchLikedChannelLatest(
        context: Context,
        channelName: String?,
        excludeUrls: Set<String>,
        opened: Set<String>
    ): List<OnlineTrackResult> {
        if (channelName.isNullOrBlank()) return emptyList()
        return try {
            val url = resolveChannelUrl(context, channelName) ?: return emptyList()
            OnlineMusicService.fetchChannelLatestVideos(url, 8)
                .map { if (it.artist.isBlank()) it.copy(artist = channelName) else it }
                .filter {
                    it.videoUrl.isNotBlank() && it.title.isNotBlank() &&
                        it.videoUrl !in excludeUrls && it.videoUrl !in opened &&
                        it.durationSec >= 60L && it.durationSec <= 4 * 3600L &&
                        !isCompleted(context, it.videoUrl)
                }
                .take(LIKED_CHANNEL_VIDEOS)
        } catch (t: kotlinx.coroutines.CancellationException) {
            throw t
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * NEW uploads (last 14 days, not opened yet, not finished) of the channels the user subscribed to, newest first,
     * tagged [OnlineTrackResult.isNew]. They are put at the very top of a batch so they are on the first screen.
     */
    private suspend fun fetchSubscribedUploads(
        context: Context,
        excludeUrls: Set<String>,
        opened: Set<String>
    ): List<OnlineTrackResult> = coroutineScope {
        val channels = SubscribedChannelsManager.getAll(context).take(SUBSCRIBED_CHANNELS_PER_BATCH)
        if (channels.isEmpty()) return@coroutineScope emptyList()
        val now = System.currentTimeMillis()
        val perChannel = channels.map { ch ->
            async(Dispatchers.IO) {
                try {
                    OnlineMusicService.fetchChannelLatestVideos(ch.channelUrl, 5)
                        .map { if (it.artist.isBlank()) it.copy(artist = ch.name) else it }
                        .filter {
                            val uploaded = it.uploadDateEpochMs ?: 0L
                            it.videoUrl.isNotBlank() && it.title.isNotBlank() &&
                                it.videoUrl !in excludeUrls && it.videoUrl !in opened &&
                                it.durationSec >= 60L && it.durationSec <= 4 * 3600L &&
                                !isCompleted(context, it.videoUrl) &&
                                uploaded > 0L && now - uploaded <= NEW_WINDOW_MS
                        }
                        .take(SUBSCRIBED_PER_CHANNEL)
                        .map { it.copy(isNew = true) }
                } catch (t: kotlinx.coroutines.CancellationException) {
                    throw t
                } catch (_: Throwable) {
                    emptyList<OnlineTrackResult>()
                }
            }
        }.awaitAll()
        perChannel.flatten()
            .distinctBy { it.videoUrl }
            .sortedByDescending { it.uploadDateEpochMs ?: 0L }
            .take(SUBSCRIBED_QUOTA)
    }

    /** Re-orders so the same channel is never shown twice in a row (and rarely close together). */
    fun spreadChannels(items: List<OnlineTrackResult>): List<OnlineTrackResult> {
        val pending = items.toMutableList()
        val out = ArrayList<OnlineTrackResult>(items.size)
        while (pending.isNotEmpty()) {
            val recent = out.takeLast(3).map { it.artist.lowercase(Locale.ROOT) }
            val idx = pending.indexOfFirst { it.artist.lowercase(Locale.ROOT) !in recent }
                .takeIf { it >= 0 } ?: pending.indexOfFirst { it.artist.lowercase(Locale.ROOT) != recent.lastOrNull() }
                .takeIf { it >= 0 } ?: 0
            out.add(pending.removeAt(idx))
        }
        return out
    }
}
