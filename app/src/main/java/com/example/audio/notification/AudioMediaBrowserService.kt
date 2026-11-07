package com.example.player

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.example.ui.components.ONLINE_TRACK_PATH_PREFIX
import com.example.ui.screens.AudioTrackItem
import com.example.util.AudioLibraryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Music library for Android Auto, Bluetooth car kits / watches (AVRCP browsing) and the Google Assistant.
 *
 * The service only exposes the library; playback itself stays in [AudioPlaybackManager] and the existing
 * MediaSession ([AudioNotificationManager.getOrCreateMediaSession]), whose token is handed to the browsing client here.
 * Tree: Songs / Recently added / Albums > album / Artists > artist. A playable item id is "<parentId>|<trackId>", so the
 * queue that starts playing is the very list the person was looking at.
 */
class AudioMediaBrowserService : MediaBrowserServiceCompat() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        sessionToken = AudioNotificationManager.getOrCreateMediaSession(this).sessionToken
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    // Only song titles / artists are exposed, so every media client (Android Auto, car Bluetooth, Assistant) may browse.
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
        BrowserRoot(AutoLibrary.ROOT, null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        scope.launch {
            val items = try {
                AutoLibrary.children(applicationContext, parentId)
            } catch (t: Throwable) {
                Log.e("AudioMediaBrowser", "onLoadChildren($parentId) failed", t)
                mutableListOf()
            }
            result.sendResult(items)
        }
    }
}

/** Library tree + "play this id / play this search" used by the browser service and the MediaSession callback. */
object AutoLibrary {
    const val ROOT = "root"
    private const val SONGS = "songs"
    private const val RECENT = "recent"
    private const val ALBUMS = "albums"
    private const val ARTISTS = "artists"
    private const val ALBUM_PREFIX = "album:"
    private const val ARTIST_PREFIX = "artist:"
    private const val MAX_ITEMS = 300

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private suspend fun allTracks(context: Context): List<AudioTrackItem> {
        val app = context.applicationContext
        val tracks = AudioLibraryCache.getCachedTracks()
            ?: AudioLibraryCache.loadCachedTracks(app)
            ?: AudioLibraryCache.getOrScanAudio(app)
        return tracks.filter { it.path.isNotBlank() && !it.path.startsWith(ONLINE_TRACK_PATH_PREFIX) }
    }

    private fun tracksFor(parentId: String, all: List<AudioTrackItem>): List<AudioTrackItem> = when {
        parentId == SONGS -> all.sortedBy { it.title.lowercase() }
        parentId == RECENT -> all.sortedByDescending { it.dateModified }
        parentId.startsWith(ALBUM_PREFIX) -> all.filter { it.album == parentId.removePrefix(ALBUM_PREFIX) }.sortedBy { it.title.lowercase() }
        parentId.startsWith(ARTIST_PREFIX) -> all.filter { it.artist == parentId.removePrefix(ARTIST_PREFIX) }.sortedBy { it.title.lowercase() }
        else -> emptyList()
    }

    private fun folder(id: String, title: String, subtitle: String? = null) = MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder().setMediaId(id).setTitle(title).setSubtitle(subtitle).build(),
        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )

    private fun playable(parentId: String, t: AudioTrackItem) = MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder()
            .setMediaId("$parentId|${t.id}")
            .setTitle(t.title)
            .setSubtitle(t.artist)
            .setDescription(t.album)
            .setMediaUri(t.uri)
            .build(),
        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
    )

    suspend fun children(context: Context, parentId: String): MutableList<MediaBrowserCompat.MediaItem> {
        if (parentId == ROOT) {
            return mutableListOf(
                folder(SONGS, "Songs"),
                folder(RECENT, "Recently added"),
                folder(ALBUMS, "Albums"),
                folder(ARTISTS, "Artists")
            )
        }
        val all = allTracks(context)
        return when (parentId) {
            ALBUMS -> all.groupBy { it.album }.entries.sortedBy { it.key.lowercase() }.take(MAX_ITEMS)
                .map { (album, list) -> folder(ALBUM_PREFIX + album, album, list.firstOrNull()?.artist) }.toMutableList()
            ARTISTS -> all.groupBy { it.artist }.entries.sortedBy { it.key.lowercase() }.take(MAX_ITEMS)
                .map { (artist, list) -> folder(ARTIST_PREFIX + artist, artist, "${list.size} songs") }.toMutableList()
            else -> tracksFor(parentId, all).take(MAX_ITEMS).map { playable(parentId, it) }.toMutableList()
        }
    }

    /** Car / watch / Assistant picked an item: play it with the list it came from as the queue. */
    fun playFromMediaId(context: Context, mediaId: String) {
        val app = context.applicationContext
        scope.launch {
            try {
                val parent = mediaId.substringBeforeLast('|')
                val trackId = mediaId.substringAfterLast('|').toLongOrNull() ?: return@launch
                val list = tracksFor(parent, allTracks(app)).take(MAX_ITEMS)
                val track = list.firstOrNull { it.id == trackId } ?: return@launch
                withContext(Dispatchers.Main) { AudioPlaybackManager.playTrack(app, track, list) }
            } catch (t: Throwable) {
                Log.e("AutoLibrary", "playFromMediaId($mediaId) failed", t)
            }
        }
    }

    /** "Hey Google, play <title / artist / album>" (empty query = play the whole library). */
    fun playFromSearch(context: Context, query: String?) {
        val app = context.applicationContext
        scope.launch {
            try {
                val all = allTracks(app)
                val q = query?.trim()?.lowercase().orEmpty()
                val hits = if (q.isEmpty()) all.sortedBy { it.title.lowercase() } else {
                    all.filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q) }
                        .sortedBy { if (it.title.lowercase().contains(q)) 0 else 1 }
                }.take(MAX_ITEMS)
                val first = hits.firstOrNull() ?: return@launch
                withContext(Dispatchers.Main) { AudioPlaybackManager.playTrack(app, first, hits) }
            } catch (t: Throwable) {
                Log.e("AutoLibrary", "playFromSearch($query) failed", t)
            }
        }
    }
}
