package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.screens.VideoItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.ui.components.toOnlineVideoItem

/**
 * True for every path that points to an online item: online audio ("online://"), online video ("online-video://")
 * and plain http(s) stream URLs.
 */
fun isOnlineMediaPath(path: String): Boolean =
    path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) ||
        path.startsWith(com.example.util.OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX) ||
        path.startsWith("http://", ignoreCase = true) ||
        path.startsWith("https://", ignoreCase = true)

data class CustomPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val videoPaths: List<String> = emptyList(),
    val mediaType: String = "VIDEO",
    val isOnline: Boolean = false
) {
    val isAudio: Boolean
        get() = mediaType.equals("AUDIO", ignoreCase = true) ||
                (videoPaths.isNotEmpty() && videoPaths.all { path ->
                    // imported stream URLs (IPTV / radio lists) are never classified by their file extension
                    val ext = path.substringAfterLast('.', "").lowercase()
                    !path.startsWith("http://", ignoreCase = true) &&
                        !path.startsWith("https://", ignoreCase = true) &&
                        ext in listOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma")
                })

    val isOnlineFolder: Boolean
        get() = isOnline || videoPaths.any { isOnlineMediaPath(it) }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(createdAt))
        }

    val itemCountText: String
        get() = if (isAudio) {
            val suffix = if (isOnlineFolder) " (Online)" else ""
            "${videoPaths.size} audio track${if (videoPaths.size != 1) "s" else ""}$suffix"
        } else {
            "${videoPaths.size} video${if (videoPaths.size != 1) "s" else ""}"
        }

    val videoCountText: String
        get() = itemCountText
}

object PlaylistManager {
    private const val PREFS_NAME = "custom_playlists_prefs"
    private const val KEY_PLAYLISTS = "saved_playlists_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getPlaylists(context: Context): List<CustomPlaylist> {
        appContext = context.applicationContext
        OnlineMediaLibrary.attach(context)
        StreamEntryStore.attach(context)
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        val list = mutableListOf<CustomPlaylist>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Untitled")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val mediaType = obj.optString("mediaType", "VIDEO")
                val isOnline = obj.optBoolean("isOnline", false)
                val pathsArray = obj.optJSONArray("videoPaths")
                val paths = mutableListOf<String>()
                if (pathsArray != null) {
                    for (j in 0 until pathsArray.length()) {
                        val p = pathsArray.optString(j)
                        if (p.isNotBlank()) paths.add(p)
                    }
                }
                list.add(CustomPlaylist(id, name, createdAt, paths, mediaType, isOnline))
            }
        } catch (_: Throwable) {}
        return list
    }

    @Synchronized
    fun savePlaylists(context: Context, playlists: List<CustomPlaylist>) {
        val prefs = getPrefs(context)
        val array = JSONArray()
        for (pl in playlists) {
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("createdAt", pl.createdAt)
            obj.put("mediaType", pl.mediaType)
            obj.put("isOnline", pl.isOnline || pl.isOnlineFolder)
            val pathsArray = JSONArray()
            for (path in pl.videoPaths) {
                pathsArray.put(path)
            }
            obj.put("videoPaths", pathsArray)
            array.put(obj)
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    @Synchronized
    fun createPlaylist(context: Context, name: String, mediaType: String = "VIDEO", isOnline: Boolean = false): CustomPlaylist {
        val trimmed = name.trim().ifBlank { "Playlist ${System.currentTimeMillis() % 1000}" }
        val current = getPlaylists(context).toMutableList()
        val newPl = CustomPlaylist(name = trimmed, mediaType = mediaType, isOnline = isOnline)
        current.add(0, newPl)
        savePlaylists(context, current)
        return newPl
    }

    /** Creates a playlist that already contains [paths] with ONE save (used by the M3U / YouTube importers). */
    @Synchronized
    fun createPlaylistWithPaths(
        context: Context,
        name: String,
        mediaType: String = "VIDEO",
        isOnline: Boolean = false,
        paths: List<String>
    ): CustomPlaylist {
        val trimmed = name.trim().ifBlank { "Playlist ${System.currentTimeMillis() % 1000}" }
        val current = getPlaylists(context).toMutableList()
        val newPl = CustomPlaylist(
            name = trimmed,
            videoPaths = paths.filter { it.isNotBlank() }.distinct(),
            mediaType = mediaType,
            isOnline = isOnline
        )
        current.add(0, newPl)
        savePlaylists(context, current)
        return newPl
    }

    @Synchronized
    fun addVideosToPlaylist(context: Context, playlistId: String, newVideoPaths: List<String>): Int {
        val current = getPlaylists(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index == -1) return 0

        val pl = current[index]
        val updatedPaths = pl.videoPaths.toMutableList()
        var addedCount = 0
        for (path in newVideoPaths) {
            if (path.isNotBlank() && !updatedPaths.contains(path)) {
                updatedPaths.add(path)
                addedCount++
            }
        }
        val hasOnlineTracks = updatedPaths.any { isOnlineMediaPath(it) }
        current[index] = pl.copy(
            videoPaths = updatedPaths,
            isOnline = pl.isOnline || hasOnlineTracks
        )
        savePlaylists(context, current)
        return addedCount
    }

    /**
     * Media files were deleted from storage: their paths leave EVERY playlist (and the Favourites marks).
     * Only the playlist entries go; nothing else is touched. Other playlists / files stay as they are.
     */
    @Synchronized
    fun removePathsEverywhere(context: Context, paths: Collection<String>, audio: Boolean) {
        if (paths.isEmpty()) return
        val gone = paths.toHashSet()
        val current = getPlaylists(context)
        try {
            for (p in gone) FavouritesFolder.removeEverywhere(context, current, audio, p)
        } catch (_: Throwable) {}
        var changed = false
        val updated = current.map { pl ->
            if (pl.videoPaths.any { it in gone }) {
                changed = true
                pl.copy(videoPaths = pl.videoPaths.filter { it !in gone })
            } else pl
        }
        if (changed) savePlaylists(context, updated)
    }

    @Synchronized
    fun removeVideoFromPlaylist(context: Context, playlistId: String, videoPath: String) {
        val current = getPlaylists(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = current[index]
            val updated = pl.videoPaths.filter { it != videoPath }
            current[index] = pl.copy(videoPaths = updated)
            savePlaylists(context, current)
        }
    }

    @Synchronized
    fun deletePlaylist(context: Context, playlistId: String) {
        val current = getPlaylists(context).toMutableList()
        current.removeAll { it.id == playlistId }
        savePlaylists(context, current)
        // forget logos / headers of imported streams that no remaining playlist uses
        StreamEntryStore.prune(
            current.flatMap { it.videoPaths }
                .filter { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
                .toSet()
        )
        // forget the table of contents of archives that no remaining playlist uses
        com.example.util.archive.ArchiveStore.prune(
            context,
            current.flatMap { it.videoPaths }
                .mapNotNull { com.example.util.archive.ArchiveStore.parsePath(it)?.first }
                .toSet()
        )
    }

    @Synchronized
    fun renamePlaylist(context: Context, playlistId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        val current = getPlaylists(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            current[index] = current[index].copy(name = trimmed)
            savePlaylists(context, current)
        }
    }

    @Synchronized
    fun duplicatePlaylist(context: Context, playlistId: String, customName: String? = null): CustomPlaylist? {
        val current = getPlaylists(context).toMutableList()
        val original = current.find { it.id == playlistId } ?: return null
        val newName = customName?.trim()?.ifBlank { null } ?: "${original.name} (Copy)"
        val newPl = CustomPlaylist(
            name = newName,
            videoPaths = original.videoPaths.toList(),
            mediaType = original.mediaType,
            isOnline = original.isOnlineFolder
        )
        current.add(0, newPl)
        savePlaylists(context, current)
        return newPl
    }

    @Synchronized
    fun updatePlaylistVideosOrder(context: Context, playlistId: String, newVideoPaths: List<String>) {
        val current = getPlaylists(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            current[index] = current[index].copy(videoPaths = newVideoPaths)
            savePlaylists(context, current)
        }
    }

    @Synchronized
    fun clearPlaylistVideos(context: Context, playlistId: String) {
        val current = getPlaylists(context).toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            current[index] = current[index].copy(videoPaths = emptyList())
            savePlaylists(context, current)
        }
    }

    // set by getPlaylists() (always called before a playlist's videos are listed); used to read the archive table of contents
    @Volatile private var appContext: Context? = null

    /**
     * Audio tracks of an audio playlist in the playlist's own order (local files from the library cache,
     * online tracks from the metadata cache, plain files as a last resort). Used by the floating
     * "Recent" button inside a playlist.
     */
    fun getAudioTracksForPlaylist(
        context: Context,
        playlist: CustomPlaylist
    ): List<com.example.ui.screens.AudioTrackItem> {
        val cachedMap = com.example.util.AudioLibraryCache.getCachedTracks()?.associateBy { it.path } ?: emptyMap()
        return playlist.videoPaths.mapNotNull { path ->
            cachedMap[path]
                ?: com.example.util.OnlineTrackMetadataCache.getTrack(context, path)
                ?: if (path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)) {
                    val videoUrl = path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                    val meta = com.example.util.OnlineMusicService.getCachedAudioMeta(videoUrl)
                    com.example.ui.screens.AudioTrackItem(
                        id = -(kotlin.math.abs(videoUrl.hashCode().toLong())),
                        uri = android.net.Uri.EMPTY,
                        title = meta?.first ?: "Online Track",
                        artist = meta?.second ?: "Online Music",
                        album = "Online Playlist",
                        durationMs = 0L,
                        path = path,
                        sizeBytes = 0L,
                        format = "STREAM",
                        audioType = "Stereo",
                        dateModified = System.currentTimeMillis()
                    )
                } else {
                    val f = File(path)
                    if (!f.exists()) null else com.example.ui.screens.AudioTrackItem(
                        id = f.absolutePath.hashCode().toLong(),
                        uri = android.net.Uri.fromFile(f),
                        title = f.nameWithoutExtension,
                        artist = "Unknown Artist",
                        album = "Unknown Album",
                        durationMs = 0L,
                        path = f.absolutePath,
                        sizeBytes = f.length(),
                        format = f.extension.uppercase(),
                        dateModified = f.lastModified() / 1000L
                    )
                }
        }
    }

    fun getVideosForPlaylist(
        playlist: CustomPlaylist,
        allVideos: List<VideoItem>
    ): List<VideoItem> {
        val videoMap = allVideos.associateBy { it.path }
        val result = mutableListOf<VideoItem>()
        for (path in playlist.videoPaths) {
            val matched = videoMap[path]
            if (matched != null) {
                result.add(matched)
            } else if (path.startsWith(com.example.util.OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX)) {
                // Online video saved through the swipe toolbar: rebuild the card from the stored metadata.
                val url = com.example.util.OnlineVideoService.toWatchUrl(path)
                val meta = OnlineMediaLibrary.getMeta(OnlineMediaLibrary.KIND_VIDEO, url)
                    ?: com.example.util.OnlineTrackResult(videoUrl = url, title = url, artist = "", durationSec = 0L, thumbnailUrl = null)
                result.add(meta.toOnlineVideoItem())
            } else if (com.example.util.archive.ArchiveStore.isArchivePath(path)) {
                // A video inside a ZIP / TAR archive: played in place (never extracted or copied).
                val parsed = com.example.util.archive.ArchiveStore.parsePath(path)
                val ctx = appContext
                val record = if (parsed != null && ctx != null) com.example.util.archive.ArchiveStore.get(ctx, parsed.first) else null
                val entry = if (parsed != null && record != null) record.entries.getOrNull(parsed.second) else null
                if (parsed != null && record != null && entry != null) {
                    val label = com.example.util.archive.ArchiveStore.displayNames(record).getOrNull(parsed.second)
                        ?: entry.name.substringAfterLast('/')
                    val isWeb = record.sourceType == "url"
                    result.add(
                        VideoItem(
                            id = -(kotlin.math.abs(("archive:" + path).hashCode().toLong())) - 1L,
                            uri = android.net.Uri.parse(path),
                            displayName = label,
                            path = path,
                            sizeBytes = entry.size,
                            durationMs = entry.durationMs,
                            // what the archive and the background probe know about this video (see ArchiveProbe)
                            dateModified = entry.modifiedMs,
                            resolution = entry.resolution,
                            framerate = entry.framerate,
                            hasSubtitles = entry.subtitleFormats.isNotEmpty(),
                            subtitleFormats = entry.subtitleFormats,
                            embeddedSubtitleFormats = entry.subtitleFormats,
                            channelName = record.displayName,
                            isOnline = isWeb
                        )
                    )
                }
            } else if (path.startsWith("http://", ignoreCase = true) || path.startsWith("https://", ignoreCase = true)) {
                // Imported M3U / IPTV stream: rebuild the normal video card (logo as thumbnail, group as sub-line).
                val meta = StreamEntryStore.get(path)
                result.add(
                    VideoItem(
                        id = -(kotlin.math.abs(("stream:" + path).hashCode().toLong())) - 1L,
                        uri = android.net.Uri.parse(path),
                        displayName = meta?.name?.takeIf { it.isNotBlank() }
                            ?: path.substringBefore('?').substringAfterLast('/').ifBlank { path },
                        path = path,
                        sizeBytes = 0L,
                        durationMs = 0L,
                        dateModified = 0L,
                        isOnline = true,
                        thumbnailUrl = meta?.logo,
                        channelName = meta?.group,
                        isDirectStream = true
                    )
                )
            } else {
                val f = File(path)
                if (f.exists()) {
                    val cachedMeta = VideoLibraryCache.getCachedVideoMeta(f.absolutePath, f.length(), f.lastModified())
                    result.add(
                        VideoItem(
                            id = f.name.hashCode().toLong(),
                            uri = android.net.Uri.fromFile(f),
                            displayName = f.name,
                            path = f.absolutePath,
                            sizeBytes = f.length(),
                            durationMs = cachedMeta?.durationMs ?: 0L,
                            dateModified = f.lastModified() / 1000L,
                            isNew = false,
                            resolution = cachedMeta?.resolution ?: "",
                            framerate = cachedMeta?.framerate ?: 0.0,
                            hasSubtitles = (cachedMeta?.subtitleFormats?.isNotEmpty() == true) || (cachedMeta?.embeddedSubtitleFormats?.isNotEmpty() == true),
                            subtitleFormats = cachedMeta?.subtitleFormats ?: emptyList(),
                            embeddedSubtitleFormats = cachedMeta?.embeddedSubtitleFormats ?: emptyList(),
                            externalSubtitleFormats = cachedMeta?.externalSubtitleFormats ?: emptyList(),
                            embeddedFontCount = cachedMeta?.embeddedFontCount ?: 0
                        )
                    )
                }
            }
        }
        return result
    }

}
