package com.example.util

import android.content.Context

/**
 * The "Favourites" folder of the playlist section: one for the video side, one for the audio side. It is never stored;
 * it is built from the hearts (see [PlaylistMarkStore]) of all playlists of that side, so it always lists exactly the
 * items that were marked as Favourite, and it only exists while at least one item is marked.
 */
object FavouritesFolder {
    const val ID_VIDEO = "viaura_favourites_video"
    const val ID_AUDIO = "viaura_favourites_audio"
    const val NAME = "Favourites"

    fun isFavouritesId(id: String?): Boolean = id == ID_VIDEO || id == ID_AUDIO

    /** Items hearted straight from a player (they do not have to be in any playlist) are marked under this hidden id. */
    private fun directId(audio: Boolean) = if (audio) "viaura_direct_fav_audio" else "viaura_direct_fav_video"

    private fun directPaths(context: Context, audio: Boolean): Set<String> =
        PlaylistMarkStore.paths(context, PlaylistMarkStore.KIND_FAVOURITE, directId(audio))

    /** True when [path] is in the Favourites folder of that side (hearted in a player or in any playlist). */
    fun isFavourite(context: Context, playlists: List<CustomPlaylist>, audio: Boolean, path: String): Boolean {
        if (path in directPaths(context, audio)) return true
        return sourcesOf(playlists, audio).any { pl ->
            path in pl.videoPaths && path in PlaylistMarkStore.paths(context, PlaylistMarkStore.KIND_FAVOURITE, pl.id)
        }
    }

    /** Heart in a player: adds [path] to Favourites, or takes it out again. Returns true when it is a favourite now. */
    fun toggleFromPlayer(context: Context, playlists: List<CustomPlaylist>, audio: Boolean, path: String): Boolean {
        if (isFavourite(context, playlists, audio, path)) {
            removeEverywhere(context, playlists, audio, path)
            return false
        }
        PlaylistMarkStore.toggle(context, PlaylistMarkStore.KIND_FAVOURITE, directId(audio), path)
        return true
    }

    private fun sourcesOf(playlists: List<CustomPlaylist>, audio: Boolean): List<CustomPlaylist> =
        playlists.filter { !isFavouritesId(it.id) && it.isAudio == audio }

    /** The Favourites folder of one side, or null when nothing is marked as Favourite there. */
    fun build(context: Context, playlists: List<CustomPlaylist>, audio: Boolean): CustomPlaylist? {
        val sources = sourcesOf(playlists, audio)
        val paths = LinkedHashSet<String>()
        for (pl in sources) {
            val marked = PlaylistMarkStore.paths(context, PlaylistMarkStore.KIND_FAVOURITE, pl.id)
            if (marked.isEmpty()) continue
            // only items that are still in the playlist (a removed item's old mark is ignored)
            for (p in pl.videoPaths) if (p in marked) paths.add(p)
        }
        paths.addAll(directPaths(context, audio))
        if (paths.isEmpty()) return null
        return CustomPlaylist(
            id = if (audio) ID_AUDIO else ID_VIDEO,
            name = NAME,
            createdAt = sources.maxOfOrNull { it.createdAt } ?: System.currentTimeMillis(),
            videoPaths = paths.toList(),
            mediaType = if (audio) "AUDIO" else "VIDEO"
        )
    }

    /** Takes [path] out of Favourites: its heart is removed in every playlist of that side that has it marked. */
    fun removeEverywhere(context: Context, playlists: List<CustomPlaylist>, audio: Boolean, path: String) {
        if (path in directPaths(context, audio)) {
            PlaylistMarkStore.toggle(context, PlaylistMarkStore.KIND_FAVOURITE, directId(audio), path)
        }
        for (pl in sourcesOf(playlists, audio)) {
            if (path in PlaylistMarkStore.paths(context, PlaylistMarkStore.KIND_FAVOURITE, pl.id)) {
                PlaylistMarkStore.toggle(context, PlaylistMarkStore.KIND_FAVOURITE, pl.id, path)
            }
        }
    }
}
