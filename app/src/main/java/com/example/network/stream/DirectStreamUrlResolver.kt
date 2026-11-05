package com.example.util

import android.content.Context
import java.net.URI
import java.net.URLDecoder

/**
 * Local Network: a direct video/audio link plays as it is; the link of any website is processed by yt-dlp (installed from
 * Settings > Network) in the background. In both cases the stream is tried FIRST: the player opens only when the video
 * really delivers data, otherwise [lastError] says so and the player is never opened. Torrent sources use TorrentPickerState.
 */
object NetworkStreamResolver {
    private val audioExtensions = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "oga", "weba", "mka", "wma", "ac3")
    private val videoExtensions = setOf(
        "mp4", "m4v", "mkv", "webm", "mov", "avi", "flv", "wmv", "mpg", "mpeg", "3gp", "ts", "m2ts", "m3u8", "mpd", "ogv", "asf"
    )
    private val webpageHosts = setOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com", "instagram.com", "facebook.com", "fb.watch",
        "twitter.com", "x.com", "tiktok.com", "vimeo.com", "dailymotion.com", "reddit.com", "redd.it",
        "pinterest.com", "tumblr.com", "twitch.tv", "streamable.com", "rumble.com", "vk.com",
        "drive.google.com", "docs.google.com", "dropbox.com", "1drv.ms", "onedrive.live.com", "mega.nz",
        "mediafire.com", "pixeldrain.com", "github.com", "snapchat.com", "threads.net", "linkedin.com",
        "soundcloud.com", "spotify.com", "mixcloud.com", "bilibili.com", "odysee.com", "bitchute.com",
        "kick.com", "dai.ly", "flickr.com", "imgur.com", "telegram.me", "t.me", "wa.me", "whatsapp.com",
        "discord.com", "discord.gg", "patreon.com", "substack.com"
    )

    @Volatile var lastError: String? = null
        private set

    /** Adds https:// where omitted; only ordinary HTTP(S) links are accepted here. Torrent URLs remain recognized separately. */
    fun normalizeUrl(raw: String): String? {
        com.example.torrent.normalizeTorrentSource(raw)?.let { return it }
        val text = raw.trim().replace("\\s".toRegex(), "")
        if (text.isEmpty()) return null
        val candidate = when {
            text.startsWith("http://", true) || text.startsWith("https://", true) -> text
            text.contains("://") -> return null
            text.contains('.') && !text.startsWith('.') && !text.endsWith('.') -> "https://$text"
            else -> return null
        }
        val uri = try { URI(candidate) } catch (_: Throwable) { return null }
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        return candidate
    }

    fun isDirectMediaUrl(url: String): Boolean {
        val uri = try { URI(url) } catch (_: Throwable) { return false }
        if (uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) return false
        val host = uri.host.lowercase()
        val isWebsitePageHost = webpageHosts.any { host == it || host.endsWith(".$it") }
        if (isWebsitePageHost) return false
        val path = uri.path.orEmpty()
        val ext = path.substringAfterLast('.', "").lowercase()
        if (ext in audioExtensions || ext in videoExtensions) return true
        // Some signed/CDN media URLs have no file extension. Pass those raw to the player without resolving or scraping.
        if (ext.isNotBlank()) return false
        return path.isNotBlank() && path != "/"
    }

    private class CacheEntry(val at: Long, val stream: NetworkStream)
    private val webCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()
    private const val CACHE_MS = 20 * 60_000L

    /** The already checked answer for [url] (the player asks for it right after the Network tab resolved the same link). */
    fun cached(url: String): NetworkStream? = webCache[url]?.takeIf { System.currentTimeMillis() - it.at < CACHE_MS }?.stream

    suspend fun resolve(context: Context, url: String, forceRefresh: Boolean = false): NetworkStream? {
        val app = context.applicationContext
        lastError = null
        if (com.example.torrent.isTorrentSource(url)) {
            lastError = "Torrent links must be opened with the torrent picker"
            return null
        }
        if (!forceRefresh) cached(url)?.let { return it }
        if (isDirectMediaUrl(url)) {
            val directProblem = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.example.download.HttpFetch.checkPlay(app, url, emptyMap()) }
            val ok = directProblem == null
            val path = try { URI(url).path.orEmpty() } catch (_: Throwable) { "" }
            val extension = path.substringAfterLast('.', "").lowercase()
            if (ok) {
                val rawName = path.trimEnd('/').substringAfterLast('/').ifBlank { "Network media" }
                val title = try { URLDecoder.decode(rawName, "UTF-8") } catch (_: Throwable) { rawName }
                val stream = NetworkStream(pageUrl = url, title = title, videoUrl = url, isDirect = true, isAudio = extension in audioExtensions)
                webCache[url] = CacheEntry(System.currentTimeMillis(), stream)
                return stream
            }
            // a link that clearly names a media file but does not deliver it is broken; a link without a file name may be a website page
            if (extension in audioExtensions || extension in videoExtensions) {
                lastError = directProblem ?: "This video can't play"
                return null
            }
        }
        // a website link: yt-dlp finds the real streams
        if (!com.example.BuildConfig.ENABLE_YTDLP || !com.example.ytdlp.YtdlpManager.isInstalled(app)) {
            lastError = "Install yt-dlp from Settings > Network to play links of websites"
            return null
        }
        val probed = com.example.download.MediaProbe.probe(app, url, forPlayback = true)
        val info = probed.getOrNull()
        if (info == null) {
            // the website's own answer is shown (login needed, removed, blocked, no video on the page ...) instead of a bare "can't play"
            lastError = com.example.download.MediaProbe.friendlyReason(probed.exceptionOrNull())
            return null
        }
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { buildWebStream(app, url, info) }?.also { webCache[url] = CacheEntry(System.currentTimeMillis(), it) }
    }

    private fun buildWebStream(app: Context, url: String, info: com.example.download.LinkInfo): NetworkStream? {
        // audio-only website (SoundCloud, Bandcamp ...)
        if (info.videos.isEmpty()) {
            var audioProblem: String? = null
            for (a in info.audioQualities.ifEmpty { info.audioTracks }) {
                if (a.format.url.isBlank()) continue
                val problem = com.example.download.HttpFetch.checkPlay(app, a.format.url, a.format.headers)
                if (problem != null) { if (audioProblem == null) audioProblem = problem; continue }
                return NetworkStream(
                    pageUrl = url, title = info.title, uploader = info.uploader.takeIf { it.isNotBlank() }, thumbnailUrl = info.thumbnailUrl.takeIf { it.isNotBlank() },
                    durationSec = info.durationSec, videoUrl = a.format.url, headers = a.format.headers, isDirect = false, isAudio = true
                )
            }
            lastError = audioProblem ?: "This audio can't play"
            return null
        }
        val playable = info.videos.filter { it.format.url.isNotBlank() }
        val qualities = playable.map { v -> OnlineVideoQuality(v.label, v.height, v.fps, v.format.url, !v.muxed) }
        val audios = info.audioTracks.filter { it.format.url.isNotBlank() }.map { a ->
            OnlineAudioStreamInfo(a.format.url, a.label, a.language, a.bitrateKbps, a.isOriginal)
        }
        // the best quality that really answers (with its audio) is checked before the player is allowed to open
        var verified = false
        var firstProblem: String? = null
        for (v in playable) {
            val videoProblem = com.example.download.HttpFetch.checkPlay(app, v.format.url, v.format.headers)
            if (videoProblem != null) { if (firstProblem == null) firstProblem = videoProblem; continue }
            if (!v.muxed && audios.isNotEmpty()) {
                val audioProblem = com.example.download.HttpFetch.checkPlay(app, audios.first().url, info.audioTracks.first().format.headers)
                if (audioProblem != null) { if (firstProblem == null) firstProblem = audioProblem; continue }
            }
            verified = true
            break
        }
        if (!verified) {
            lastError = firstProblem ?: if (playable.isEmpty()) "No playable video stream was found on this page." else "This video can't play"
            return null
        }
        val first = playable.first()
        val chapters = info.chapters.map { OnlineChapterInfo(it.title, it.startMs) }
        val playback = OnlineVideoPlayback(url, info.title, info.uploader, info.durationSec, qualities, audios, chapters)
        val subs = info.subtitles.map { s -> com.example.util.media.MediaSubtitle(s.name, s.language, s.url) }
        return NetworkStream(
            pageUrl = url, title = info.title, uploader = info.uploader.takeIf { it.isNotBlank() }, thumbnailUrl = info.thumbnailUrl.takeIf { it.isNotBlank() },
            durationSec = info.durationSec, videoUrl = first.format.url, headers = first.format.headers, isDirect = false, isAudio = false,
            web = WebPlayback(playback, first.format.headers, subs)
        )
    }
}
