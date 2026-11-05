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

/** One quality of the audio (language) a title was opened with. */
data class PluginQualityChoice(
    val quality: String,
    val episodesLink: String?,
    val directLinks: List<MediaDirectLink>
)

/**
 * Session tracking servers, qualities, and failover state for a playing plugin video.
 */
class PluginPlaybackSession(
    val plugin: MediaPluginInfo,
    val episodeIndex: Int,
    val episodeTitle: String,
    val isSeries: Boolean,
    val choices: List<PluginQualityChoice>,
    @Volatile var selectedQuality: String,
    @Volatile var subtitles: List<MediaSubtitle> = emptyList(),
    @Volatile var servers: List<MediaStream> = emptyList(),
    @Volatile var selectedServer: Int = 0,
    @Volatile var manualChoice: Boolean = false,
    @Volatile var hasPlayed: Boolean = false
) {
    /** The episode's own link and type (what the provider's getStream understands): needed to ask the provider for new links. */
    @Volatile var episodeLink: String = ""
    @Volatile var episodeType: String = ""
}

object PluginPlaybackSessions {
    private val map = object : LinkedHashMap<String, PluginPlaybackSession>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PluginPlaybackSession>?): Boolean = size > 24
    }

    @Synchronized fun put(url: String, session: PluginPlaybackSession) { map[url] = session }
    @Synchronized fun get(url: String): PluginPlaybackSession? = map[url]
}
