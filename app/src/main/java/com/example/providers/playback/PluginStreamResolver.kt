package com.example.util.media

import android.content.Context
import android.net.Uri
import com.example.ui.screens.VideoItem
import com.example.util.PlaylistManager
import com.example.util.StreamEntryMeta
import com.example.util.StreamEntryStore
import com.example.util.network.ViauraHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Turns a plugin's stream into something the app's player, playlists, and downloader understand. */
object MediaPlayback {
    /** The browser User-Agent used for the player when the plugin names none. */
    const val STREAM_UA = ViauraHttpClient.DEFAULT_USER_AGENT

    private val PLAYER_OWN_HEADERS = setOf(
        "host", "connection", "content-length", "content-type", "accept-encoding", "range", "if-range",
        "transfer-encoding", "te", "upgrade-insecure-requests"
    )

    internal fun rankOf(s: MediaStream): Int = rank(s)

    private fun rank(s: MediaStream): Int {
        val path = s.link.substringBefore('?').substringBefore('#').lowercase()
        val type = s.type.lowercase()
        return when {
            type == "m3u8" || type == "hls" || path.endsWith(".m3u8") -> 0
            type == "mp4" || path.endsWith(".mp4") -> 1
            type == "webm" || path.endsWith(".webm") -> 2
            type == "mkv" || path.endsWith(".mkv") -> 3
            type == "mpd" || type == "dash" || path.endsWith(".mpd") -> 4
            else -> 5
        }
    }

    // CloudStream's Qualities enum (value to defaultPriority), used to order the servers exactly like its player does
    private val CS_QUALITY_PRIORITY = listOf(144 to 0, 240 to 2, 360 to 3, 480 to 4, 720 to 5, 1080 to 6, 1440 to 7, 2160 to 8, 400 to 4)

    /**
     * CloudStream's link priority (QualityDataHelper.getLinkPriority with the default profile): the priority of the quality
     * closest to the link's quality, plus 1 for the source. The player lists the links highest priority first, stable
     * (the provider's own order decides between equal ones), and plays the first one; the next one is only used after an error.
     */
    internal fun csPriority(s: MediaStream): Int {
        val q = s.quality?.trim()?.lowercase().orEmpty()
        val value = when {
            q.isEmpty() -> 400
            q == "4k" -> 2160
            else -> q.filter { it.isDigit() }.toIntOrNull() ?: 400
        }
        val closest = CS_QUALITY_PRIORITY.minByOrNull { kotlin.math.abs(it.first - value) } ?: (400 to 4)
        return closest.second + 1
    }

    /** Prefer adaptive streaming when a provider offers it; fall back to direct media files. */
    fun pickBest(streams: List<MediaStream>): MediaStream? = streams.minByOrNull { rank(it) }

    /** Asks the plugin for a playable stream for [link] using parallel server race. */
    suspend fun resolve(
        context: Context,
        plugin: MediaPluginInfo,
        link: String,
        type: String,
        episodeTitle: String = "",
        episodeIndex: Int = -1,
        verify: Boolean = true,
        requireWorking: Boolean = false
    ): Result<MediaStream> {
        return try {
            val streams = MediaProviderRepository.loadStreams(context, plugin.value, link, type, forPlayback = true)
            // CloudStream lists the links best quality first (stable); Vega plays them in the order the provider gave them
            var ranked = streams.distinctBy { it.link }
            if (plugin.isCloudStream) ranked = ranked.sortedByDescending { csPriority(it) }

            // Episode thumbnail frame server preference check
            val proven = EpisodeFrames.preferred(context, plugin, link)
            if (proven != null) {
                val i = ranked.indexOfFirst { it.server == proven.server && it.quality == proven.quality }
                    .takeIf { it >= 0 } ?: ranked.indexOfFirst { it.server == proven.server }
                if (i > 0) ranked = listOf(ranked[i]) + ranked.filterIndexed { idx, _ -> idx != i }
            }

            // Settings > Network > Media settings: the server of the preferred language / the stream of the preferred quality first
            ranked = MediaPreferences.order(context, plugin.value, ranked)

            // torrents (TorraStream ...) are not probed like a web link: they are streamed through the torrent engine, and only
            // when no direct link exists (a direct link starts faster)
            val torrents = ranked.filter { it.type == "torrent" }
            ranked = ranked.filter { it.type != "torrent" }
            if (ranked.isEmpty() && torrents.isNotEmpty()) {
                var lastError: Throwable? = null
                for (t in torrents.take(3)) {
                    try {
                        val local = com.example.torrent.TorrentStreamer.prepare(context, t.link)
                        return Result.success(t.copy(server = "Torrent · " + t.server, link = local, type = "video", headers = emptyMap(), alternatives = emptyList()))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        lastError = e
                    }
                }
                return Result.failure(lastError ?: PluginException("No playable link was found"))
            }

            if (verify && ranked.isNotEmpty()) {
                val outcome = StreamProbe.verify(context, plugin.value, ranked)
                if (!outcome.anyWorks) {
                    MediaProviderRepository.forgetStreams(plugin.value, link)
                }
                if (requireWorking && !outcome.anyWorks) {
                    return Result.failure(PluginException("This video does not play on any server right now. Try again in a moment."))
                }
                ranked = outcome.ordered
            }

            val chosen = ranked.firstOrNull()
            if (chosen == null) {
                Result.failure(PluginException("No playable link was found"))
            } else {
                val allSubs = streams.flatMap { it.subtitles }.distinctBy { it.uri }
                val others = ranked.filter { it !== chosen && it.link != chosen.link }.distinctBy { it.link }
                Result.success(
                    chosen.copy(
                        subtitles = if (allSubs.size > chosen.subtitles.size) allSubs else chosen.subtitles,
                        alternatives = others
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Converts a resolved stream to a VideoItem and configures mpv stream headers.
     */
    fun toVideoItem(
        context: Context,
        plugin: MediaPluginInfo,
        title: String,
        poster: String,
        stream: MediaStream,
        session: PluginPlaybackSession? = null
    ): VideoItem {
        if (session != null) {
            val known = session.servers.indexOfFirst { it.link == stream.link }
            if (known >= 0) {
                session.selectedServer = known
            } else {
                session.servers = listOf(stream.copy(alternatives = emptyList())) + stream.alternatives
                session.selectedServer = 0
            }
            if (stream.subtitles.isNotEmpty() || known < 0) session.subtitles = stream.subtitles
            PluginPlaybackSessions.put(stream.link, session)
        }

        // Header extraction
        val ua = stream.headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.value?.takeIf { it.isNotBlank() } ?: STREAM_UA
        val ref = stream.headers.entries.firstOrNull { it.key.equals("Referer", true) || it.key.equals("Referrer", true) }?.value
        val extra = LinkedHashMap<String, String>()

        for ((k, v) in stream.headers) {
            if (k.equals("User-Agent", true) || k.equals("Referer", true) || k.equals("Referrer", true)) continue
            if (k.lowercase() in PLAYER_OWN_HEADERS) continue
            extra[k] = v
        }

        StreamEntryStore.attach(context)
        StreamEntryStore.putAll(
            listOf(
                StreamEntryMeta(
                    url = stream.link,
                    name = title,
                    logo = poster.ifBlank { null },
                    group = plugin.name,
                    userAgent = ua,
                    referrer = ref,
                    extraHeaders = extra
                )
            )
        )

        return VideoItem(
            id = -(kotlin.math.abs(("mediaplugin:" + stream.link).hashCode().toLong())) - 1L,
            uri = Uri.parse(stream.link),
            displayName = title,
            path = stream.link,
            sizeBytes = 0L,
            durationMs = 0L,
            dateModified = System.currentTimeMillis() / 1000L,
            isOnline = true,
            thumbnailUrl = poster.ifBlank { null },
            channelName = plugin.name,
            isDirectStream = true
        )
    }

    /** The same movie / episode in another quality option of the title. */
    suspend fun resolveChoice(
        context: Context,
        plugin: MediaPluginInfo,
        choice: PluginQualityChoice,
        episodeIndex: Int,
        episodeTitle: String,
        isSeries: Boolean
    ): Result<MediaStream> {
        return try {
            val list = choice.episodesLink?.let { MediaProviderRepository.loadEpisodes(context, plugin.value, it) } ?: choice.directLinks
            fun norm(t: String) = t.lowercase().filter { it.isLetterOrDigit() }
            val target = list.firstOrNull { norm(it.title) == norm(episodeTitle) } ?: list.getOrNull(episodeIndex)
                ?: return Result.failure(PluginException("This quality does not have the same episode"))
            resolve(context, plugin, target.link, target.type ?: if (isSeries) "series" else "movie", episodeTitle, if (isSeries) episodeIndex else -1)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun addToPluginPlaylist(context: Context, plugin: MediaPluginInfo, item: VideoItem): Boolean {
        val existing = PlaylistManager.getPlaylists(context)
            .firstOrNull { it.name.equals(plugin.name, ignoreCase = true) && !it.isAudio }
        val playlist = existing ?: PlaylistManager.createPlaylist(context, plugin.name, "VIDEO", true)
        PlaylistManager.updatePlaylistVideosOrder(context, playlist.id, listOf(item.path) + playlist.videoPaths.filter { it != item.path })
        return existing == null
    }
}
