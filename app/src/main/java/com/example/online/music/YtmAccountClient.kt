package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** One card of a YouTube Music shelf. [kind] is song | playlist | album | artist. */
data class YtmItem(
    val kind: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val track: OnlineTrackResult? = null,
    val playlistId: String? = null,
    val channelId: String? = null,
    /** A music video: its cover is 16:9 (shown wide). */
    val wide: Boolean = false
)

/** [style] is "columns" for a shelf of song rows ("Quick picks", "Listen again") and "cards" for covers. */
data class YtmShelf(
    val title: String,
    val items: List<YtmItem>,
    val strapline: String = "",
    val thumbnailUrl: String? = null,
    val style: String = "cards",
    val headerBrowseId: String? = null
)

data class YtmChip(val label: String, val params: String)

data class YtmHome(val chips: List<YtmChip>, val shelves: List<YtmShelf>)

/**
 * Signed-in YouTube Music (InnerTube "WEB_REMIX") requests: the personal home, the library (playlists, albums,
 * artists) and the tracks of a playlist, including private ones. Every call is a no-op without a connected account.
 */
object YtmAccountClient {
    private const val TAG = "YtmAccountClient"
    private const val ORIGIN = "https://music.youtube.com"
    private const val ENDPOINT = "$ORIGIN/youtubei/v1/browse?prettyPrint=false"
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    private const val CLIENT_VERSION = "1.20241111.01.00"

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()

    private fun cookieValue(cookie: String, name: String): String? =
        cookie.split(';').map { it.trim() }.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun authHeader(cookie: String): String? {
        val sapisid = cookieValue(cookie, "SAPISID") ?: cookieValue(cookie, "__Secure-3PAPISID") ?: return null
        val ts = System.currentTimeMillis() / 1000
        return "SAPISIDHASH ${ts}_${sha1("$ts $sapisid $ORIGIN")}"
    }

    private fun context(): JSONObject = JSONObject().put(
        "client", JSONObject()
            .put("clientName", "WEB_REMIX").put("clientVersion", CLIENT_VERSION)
            .put("hl", "en").put("platform", "DESKTOP")
    ).put("user", JSONObject().put("lockedSafetyMode", false))

    private fun post(body: JSONObject, endpoint: String = ENDPOINT): JSONObject? {
        val cookie = YtmSession.cookie() ?: return null
        val auth = authHeader(cookie) ?: return null
        body.put("context", context())
        val req = Request.Builder().url(endpoint)
            .header("User-Agent", UA).header("Cookie", cookie).header("Authorization", auth)
            .header("Origin", ORIGIN).header("X-Origin", ORIGIN).header("Referer", "$ORIGIN/")
            .header("X-Goog-AuthUser", "0").header("X-Youtube-Client-Name", "67").header("X-Youtube-Client-Version", CLIENT_VERSION)
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull())).build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) {
                Log.w(TAG, "HTTP ${r.code} for ${body.optString("browseId")}")
                return null
            }
            return JSONObject(r.body?.string().orEmpty())
        }
    }

    private suspend fun browse(browseId: String, continuation: String? = null, params: String? = null): JSONObject? = withContext(Dispatchers.IO) {
        try {
            if (continuation != null) {
                post(JSONObject(), "$ENDPOINT&ctoken=$continuation&continuation=$continuation&type=next")
            } else post(JSONObject().put("browseId", browseId).also { if (params != null) it.put("params", params) })
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "browse($browseId) failed: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------------------------------------------ public API

    /** The personal YouTube Music home: its mood chips and every shelf the account gets ("Quick picks", "Listen again", mixes ...). */
    suspend fun home(): YtmHome {
        val root = browse("FEmusic_home") ?: return YtmHome(emptyList(), emptyList())
        return YtmHome(chipsOf(root), shelvesOf(root, 14))
    }

    /** The home of one mood chip (Relax, Workout, Energize ...). */
    suspend fun homeForChip(params: String): List<YtmShelf> {
        val root = browse("FEmusic_home", params = params) ?: return emptyList()
        return shelvesOf(root, 14)
    }

    private suspend fun shelvesOf(root: JSONObject, max: Int): List<YtmShelf> {
        val out = ArrayList<YtmShelf>()
        collectShelves(sectionContents(root), out)
        val list0 = root.optJSONObject("contents")?.optJSONObject("singleColumnBrowseResultsRenderer")?.optJSONArray("tabs")
            ?.optJSONObject(0)?.optJSONObject("tabRenderer")?.optJSONObject("content")?.optJSONObject("sectionListRenderer")
        var token = list0?.let { continuationOf(it) }
        var guard = 0
        while (token != null && guard < 3 && out.size < max) {
            val more = browse("", token) ?: break
            val list = more.optJSONObject("continuationContents")?.optJSONObject("sectionListContinuation")
            collectShelves(list?.optJSONArray("contents"), out)
            token = list?.let { continuationOf(it) }
            guard++
        }
        return out.filter { it.items.isNotEmpty() }
    }

    private fun chipsOf(root: JSONObject): List<YtmChip> {
        val chips = root.optJSONObject("contents")?.optJSONObject("singleColumnBrowseResultsRenderer")?.optJSONArray("tabs")
            ?.optJSONObject(0)?.optJSONObject("tabRenderer")?.optJSONObject("content")?.optJSONObject("sectionListRenderer")
            ?.optJSONObject("header")?.optJSONObject("chipCloudRenderer")?.optJSONArray("chips") ?: return emptyList()
        return (0 until chips.length()).mapNotNull { i ->
            val c = chips.optJSONObject(i)?.optJSONObject("chipCloudChipRenderer") ?: return@mapNotNull null
            val label = c.optJSONObject("text")?.let { runsText(it) }.orEmpty().trim()
            val params = c.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("params").orEmpty()
            if (label.isBlank() || params.isBlank()) null else YtmChip(label, params)
        }
    }

    suspend fun libraryPlaylists(): List<YtmItem> = library("FEmusic_liked_playlists", "playlist")
    suspend fun libraryAlbums(): List<YtmItem> = library("FEmusic_liked_albums", "album")
    suspend fun libraryArtists(): List<YtmItem> = library("FEmusic_library_corpus_artists", "artist")

    private suspend fun library(browseId: String, @Suppress("UNUSED_PARAMETER") kind: String): List<YtmItem> {
        val root = browse(browseId) ?: return emptyList()
        val out = ArrayList<YtmItem>()
        val sections = sectionContents(root) ?: return emptyList()
        for (i in 0 until sections.length()) {
            val sec = sections.optJSONObject(i) ?: continue
            val grid = sec.optJSONObject("gridRenderer")?.optJSONArray("items")
            val shelf = sec.optJSONObject("musicShelfRenderer")?.optJSONArray("contents")
            val arr = grid ?: shelf ?: continue
            for (j in 0 until arr.length()) parseItem(arr.optJSONObject(j))?.let { out.add(it) }
        }
        // "New playlist" tile and the like have no id
        return out.filter { it.playlistId != null || it.channelId != null }
    }

    /** All tracks of a playlist / album / "Liked songs" (id "LM"), private ones included. */
    suspend fun playlistTracks(playlistId: String): Pair<String, List<OnlineTrackResult>>? {
        val id = playlistId.removePrefix("VL")
        val root = browse("VL$id") ?: return null
        val title = root.optJSONObject("header")?.let { h ->
            (h.optJSONObject("musicDetailHeaderRenderer") ?: h.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer"))?.optJSONObject("title")?.let { runsText(it) }
        } ?: ""
        val tracks = ArrayList<OnlineTrackResult>()
        var shelfNode: JSONObject? = null
        val contents = root.optJSONObject("contents")
        val twoCol = contents?.optJSONObject("twoColumnBrowseResultsRenderer")
        val secList = twoCol?.optJSONObject("secondaryContents")?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")
            ?: sectionContents(root)
        if (secList != null) for (i in 0 until secList.length()) {
            shelfNode = secList.optJSONObject(i)?.optJSONObject("musicPlaylistShelfRenderer")
                ?: secList.optJSONObject(i)?.optJSONObject("musicShelfRenderer")
            if (shelfNode != null) break
        }
        var token: String? = null
        shelfNode?.optJSONArray("contents")?.let { arr ->
            for (i in 0 until arr.length()) parseSong(arr.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer"))?.let { tracks.add(it) }
            token = continuationOf(shelfNode)
        }
        var guard = 0
        while (token != null && guard < 20 && tracks.size < 1500) {
            val more = browse("", token) ?: break
            val shelf = more.optJSONObject("continuationContents")?.optJSONObject("musicPlaylistShelfContinuation")
                ?: more.optJSONObject("continuationContents")?.optJSONObject("musicShelfContinuation")
            val arr = shelf?.optJSONArray("contents") ?: break
            for (i in 0 until arr.length()) parseSong(arr.optJSONObject(i)?.optJSONObject("musicResponsiveListItemRenderer"))?.let { tracks.add(it) }
            token = shelf?.let { continuationOf(it) }
            guard++
        }
        if (tracks.isEmpty()) return null
        return (title.ifBlank { if (id == "LM") "Liked songs" else "Playlist" }) to tracks
    }

    // ------------------------------------------------------------------------------------------------ parsing

    private fun sectionContents(root: JSONObject): JSONArray? =
        root.optJSONObject("contents")?.optJSONObject("singleColumnBrowseResultsRenderer")?.optJSONArray("tabs")
            ?.optJSONObject(0)?.optJSONObject("tabRenderer")?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")

    private fun continuationOf(node: JSONObject): String? =
        node.optJSONArray("continuations")?.optJSONObject(0)?.optJSONObject("nextContinuationData")?.optString("continuation")
            ?.takeIf { it.isNotBlank() }

    private fun collectShelves(sections: JSONArray?, out: MutableList<YtmShelf>) {
        if (sections == null) return
        for (i in 0 until sections.length()) {
            val sec = sections.optJSONObject(i) ?: continue
            val carousel = sec.optJSONObject("musicCarouselShelfRenderer") ?: sec.optJSONObject("musicImmersiveCarouselShelfRenderer")
            if (carousel != null) {
                val header = carousel.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?: carousel.optJSONObject("header")?.optJSONObject("musicImmersiveCarouselShelfBasicHeaderRenderer")
                val title = header?.optJSONObject("title")?.let { runsText(it) }.orEmpty()
                val strapline = header?.optJSONObject("strapline")?.let { runsText(it) }.orEmpty()
                val headerThumb = bestThumb(header?.optJSONObject("thumbnail"), square = true)
                val headerBrowse = header?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)
                    ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("browseId")
                    ?.takeIf { it.startsWith("UC") || it.startsWith("MPLA") }
                val arr = carousel.optJSONArray("contents") ?: continue
                val items = ArrayList<YtmItem>()
                var rows = 0
                for (j in 0 until arr.length()) {
                    val w = arr.optJSONObject(j)
                    if (w?.has("musicResponsiveListItemRenderer") == true) rows++
                    parseItem(w)?.let { items.add(it) }
                }
                if (title.isNotBlank() && items.isNotEmpty()) {
                    val columns = rows > 0 && rows >= arr.length() - 1 && items.all { it.kind == "song" }
                    out.add(YtmShelf(title, items, strapline, headerThumb, if (columns) "columns" else "cards", headerBrowse))
                }
                continue
            }
            val shelf = sec.optJSONObject("musicShelfRenderer")
            if (shelf != null) {
                val title = shelf.optJSONObject("title")?.let { runsText(it) }.orEmpty()
                val arr = shelf.optJSONArray("contents") ?: continue
                val items = ArrayList<YtmItem>()
                for (j in 0 until arr.length()) parseItem(arr.optJSONObject(j))?.let { items.add(it) }
                if (title.isNotBlank() && items.isNotEmpty()) out.add(YtmShelf(title, items, style = if (items.all { it.kind == "song" }) "columns" else "cards"))
            }
        }
    }

    private fun runsText(node: JSONObject): String {
        val runs = node.optJSONArray("runs") ?: return node.optString("simpleText")
        val sb = StringBuilder()
        for (i in 0 until runs.length()) sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        return sb.toString()
    }

    private fun bestThumb(node: JSONObject?, square: Boolean = true): String? {
        val arr = node?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: node?.optJSONObject("croppedSquareThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: return null
        val url = arr.optJSONObject(arr.length() - 1)?.optString("url")?.takeIf { it.isNotBlank() } ?: return null
        if (!square) return url
        // higher resolution square
        return url.replace(Regex("=w\\d+-h\\d+.*$"), "=w544-h544-l90-rj").replace(Regex("=s\\d+.*$"), "=s544")
    }

    private fun parseItem(wrapper: JSONObject?): YtmItem? {
        if (wrapper == null) return null
        wrapper.optJSONObject("musicTwoRowItemRenderer")?.let { return parseTwoRow(it) }
        wrapper.optJSONObject("musicResponsiveListItemRenderer")?.let { r ->
            val song = parseSong(r)
            if (song != null) return YtmItem("song", song.title, song.artist, song.thumbnailUrl, track = song)
            // library lists use the same renderer for playlists / albums / artists
            val nav = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val id = nav?.optString("browseId").orEmpty()
            val cols = r.optJSONArray("flexColumns")
            val title = cols?.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.let { runsText(it) }.orEmpty()
            val sub = cols?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.let { runsText(it) }.orEmpty()
            val thumb = bestThumb(r.optJSONObject("thumbnail"))
            return kindFromBrowseId(id, title, sub, thumb, null)
        }
        return null
    }

    private fun kindFromBrowseId(id: String, title: String, sub: String, thumb: String?, playlistIdHint: String?): YtmItem? {
        if (title.isBlank()) return null
        return when {
            id.startsWith("UC") || id.startsWith("MPLA") -> YtmItem("artist", title, sub, thumb, channelId = id.removePrefix("MPLA"))
            id.startsWith("MPRE") -> YtmItem("album", title, sub, thumb, playlistId = playlistIdHint)
                .takeIf { playlistIdHint != null }
            id.startsWith("VL") -> YtmItem("playlist", title, sub, thumb, playlistId = id.removePrefix("VL"))
            else -> playlistIdHint?.let { YtmItem("playlist", title, sub, thumb, playlistId = it) }
        }
    }

    private fun parseTwoRow(r: JSONObject): YtmItem? {
        val title = r.optJSONObject("title")?.let { runsText(it) }.orEmpty()
        val sub = r.optJSONObject("subtitle")?.let { runsText(it) }.orEmpty()
        val nav = r.optJSONObject("navigationEndpoint")
        val watch = nav?.optJSONObject("watchEndpoint")
        val videoType = watch?.optJSONObject("watchEndpointMusicSupportedConfigs")?.optJSONObject("watchEndpointMusicConfig")
            ?.optString("musicVideoType").orEmpty()
        val isVideo = videoType == "MUSIC_VIDEO_TYPE_OMV" || videoType == "MUSIC_VIDEO_TYPE_UGC"
        val thumb = bestThumb(r.optJSONObject("thumbnailRenderer"), square = !isVideo)
        val overlayPlay = r.optJSONObject("thumbnailOverlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")?.optJSONObject("playNavigationEndpoint")
        val playlistFromOverlay = overlayPlay?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")?.takeIf { it.isNotBlank() }
            ?: overlayPlay?.optJSONObject("watchEndpoint")?.optString("playlistId")?.takeIf { it.isNotBlank() }
        val videoId = watch?.optString("videoId")?.takeIf { it.isNotBlank() }
        if (videoId != null) {
            val parts = sub.split(" \u2022 ").map { it.trim() }.filter { it.isNotBlank() }
            val artist = parts.firstOrNull { !it.equals("Song", true) && !it.equals("Video", true) && !it.equals("Single", true) } ?: ""
            val t = OnlineTrackResult(
                videoUrl = "https://www.youtube.com/watch?v=$videoId", title = title, artist = artist,
                durationSec = 0L, thumbnailUrl = thumb
            )
            return YtmItem("song", title, artist, thumb, track = t, wide = isVideo)
        }
        val browseId = nav?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
        val watchList = nav?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")?.takeIf { it.isNotBlank() }
        return kindFromBrowseId(browseId, title, sub, thumb, watchList ?: playlistFromOverlay)
            ?: if (watchList != null && title.isNotBlank()) YtmItem("playlist", title, sub, thumb, playlistId = watchList) else null
    }

    private fun parseSong(r: JSONObject?): OnlineTrackResult? {
        if (r == null) return null
        val videoId = r.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: r.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: return null
        val cols = r.optJSONArray("flexColumns") ?: return null
        fun col(i: Int): JSONObject? = cols.optJSONObject(i)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
        val title = col(0)?.let { runsText(it) }.orEmpty().ifBlank { return null }
        val artistRuns = col(1)?.optJSONArray("runs")
        var artist = ""
        var durationSec = 0L
        if (artistRuns != null) {
            val texts = (0 until artistRuns.length()).map { artistRuns.optJSONObject(it)?.optString("text").orEmpty() }
            artist = texts.firstOrNull { it.isNotBlank() && it.trim() != "\u2022" && !it.equals("Song", true) && !it.equals("Video", true) }.orEmpty()
            texts.lastOrNull { Regex("^\\d{1,2}(:\\d{2}){1,2}$").matches(it.trim()) }?.let { durationSec = parseDuration(it.trim()) }
        }
        if (durationSec == 0L) {
            val fixed = r.optJSONArray("fixedColumns")?.optJSONObject(0)?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                ?.optJSONObject("text")?.let { runsText(it) }.orEmpty().trim()
            if (fixed.isNotBlank()) durationSec = parseDuration(fixed)
        }
        return OnlineTrackResult(
            videoUrl = "https://www.youtube.com/watch?v=$videoId", title = title, artist = artist,
            durationSec = durationSec, thumbnailUrl = bestThumb(r.optJSONObject("thumbnail"))
        )
    }

    private fun parseDuration(s: String): Long {
        val p = s.split(':').mapNotNull { it.toLongOrNull() }
        return when (p.size) { 2 -> p[0] * 60 + p[1]; 3 -> p[0] * 3600 + p[1] * 60 + p[2]; else -> 0L }
    }
}
