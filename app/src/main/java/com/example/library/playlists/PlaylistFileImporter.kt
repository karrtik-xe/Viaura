package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import com.example.util.archive.ArchiveEngine
import com.example.util.archive.ArchiveException
import com.example.util.archive.ArchiveRecord
import com.example.util.archive.ArchiveSource
import com.example.util.archive.ArchiveStore
import com.example.util.archive.HttpArchiveSource
import com.example.util.archive.UriArchiveSource

/**
 * Real playlist import: downloads / reads the actual content, parses it and stores the result in the
 * existing playlist system ([PlaylistManager]). Nothing is created until at least one entry was parsed.
 *
 *  - M3U / M3U8 URL            -> [importFromUrl]  (OkHttp on Dispatchers.IO, optional custom User-Agent)
 *  - local M3U / M3U8 file     -> [importFromLocalFile] (Storage Access Framework URI)
 *  - YouTube playlist URL      -> [importFromUrl] detects it and uses NewPipeExtractor (same engine as Online Video;
 *                                  no API key, no login)
 */
object PlaylistImporter {
    private const val TAG = "PlaylistImporter"
    private const val MAX_PLAYLIST_BYTES = 40L * 1024L * 1024L
    private const val MAX_YOUTUBE_ITEMS = 5000
    private const val MAX_YOUTUBE_PAGES = 80
    private const val DEFAULT_FETCH_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // ------------------------------------------------------------------ URL classification

    /** Returns the YouTube playlist id when [url] is a YouTube / YouTube Music playlist link, else null. */
    fun youTubePlaylistId(url: String): String? {
        val uri = try { URI(url.trim()) } catch (_: Throwable) { return null }
        val host = (uri.host ?: return null).lowercase()
        val isYt = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
        if (!isYt) return null
        val query = uri.rawQuery ?: return null
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq > 0 && part.substring(0, eq) == "list") {
                val id = part.substring(eq + 1)
                if (id.isNotBlank()) return id
            }
        }
        return null
    }

    fun isYouTubeUrl(url: String): Boolean {
        val host = try { URI(url.trim()).host?.lowercase() } catch (_: Throwable) { null } ?: return false
        return host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")
    }

    /** Basic validation of the URL field. Returns an error message or null when the URL looks usable. */
    fun validateUrl(raw: String): String? {
        val url = raw.trim()
        if (url.isEmpty()) return "Enter a playlist URL"
        val lower = url.lowercase()
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) return "URL must start with http:// or https://"
        val uri = try { URI(url) } catch (_: Throwable) { return "This is not a valid URL" }
        if (uri.host.isNullOrBlank()) return "This is not a valid URL"
        return null
    }

    // ------------------------------------------------------------------ public entry points

    suspend fun importFromUrl(
        context: Context,
        rawUrl: String,
        userAgent: String?,
        mediaType: String = "VIDEO"
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
            val url = rawUrl.trim()
            validateUrl(url)?.let { return@withContext PlaylistImportResult.Failure(it) }
            // lets a cancelled import stop right before the playlist would be created
            val checkActive: () -> Unit = { ensureActive() }
            try {
                if (isYouTubeUrl(url)) {
                    val listId = youTubePlaylistId(url)
                        ?: return@withContext PlaylistImportResult.Failure(
                            "This YouTube link has no playlist in it. Paste a playlist link (it contains \"list=\")."
                        )
                    return@withContext importYouTubePlaylist(context, listId, mediaType, checkActive)
                }
                if (isSpotifyPlaylistUrl(url)) {
                    // Spotify is an audio source; create an audio playlist using the same metadata
                    // resolver used by shared Spotify playlists instead of treating its web page as M3U.
                    return@withContext importSpotifyPlaylist(context, url, checkActive)
                }

                val text = download(url, userAgent)
                if (text.isBlank()) return@withContext PlaylistImportResult.Failure("The playlist is empty")
                importFromText(
                    context = context,
                    text = text,
                    baseUrl = url,
                    fallbackName = nameFromUrl(url),
                    userAgent = userAgent,
                    mediaType = mediaType,
                    ensureActive = checkActive
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "importFromUrl failed: ${e.message}", e)
                PlaylistImportResult.Failure(describeError(e))
            }
        }

    suspend fun importFromLocalFile(
        context: Context,
        uri: Uri,
        mediaType: String = "VIDEO"
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
            val checkActive: () -> Unit = { ensureActive() }
            try {
                val bytes = readLimited(context, uri)
                    ?: return@withContext PlaylistImportResult.Failure("Could not open the selected file")
                val text = String(bytes, Charsets.UTF_8)
                if (text.isBlank()) return@withContext PlaylistImportResult.Failure("The selected file is empty")
                importFromText(
                    context = context,
                    text = text,
                    baseUrl = null,
                    fallbackName = displayNameOf(context, uri)?.substringBeforeLast('.')?.ifBlank { null } ?: "Imported playlist",
                    userAgent = null,
                    mediaType = mediaType,
                    ensureActive = checkActive
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                Log.e(TAG, "importFromLocalFile denied: ${e.message}", e)
                PlaylistImportResult.Failure("No permission to read this file. Pick it again with the file picker.")
            } catch (e: Throwable) {
                Log.e(TAG, "importFromLocalFile failed: ${e.message}", e)
                PlaylistImportResult.Failure(describeError(e))
            }
        }

    // ------------------------------------------------------------------ M3U / M3U8

    private fun importFromText(
        context: Context,
        text: String,
        baseUrl: String?,
        fallbackName: String,
        userAgent: String?,
        mediaType: String,
        ensureActive: () -> Unit
    ): PlaylistImportResult {
        if (!M3uPlaylistParser.looksLikePlaylist(text)) {
            return PlaylistImportResult.Failure("This is not a valid M3U/M3U8 playlist (no #EXTM3U / #EXTINF found)")
        }
        val parsed = M3uPlaylistParser.parse(text, baseUrl)
        var entries = parsed.entries
        if (entries.isEmpty()) {
            // A bare HLS stream manifest (one channel, not a channel list): keep it as a one-stream playlist.
            if (baseUrl != null && M3uPlaylistParser.isHlsStreamManifest(text)) {
                entries = listOf(M3uEntry(name = fallbackName, url = baseUrl))
            } else {
                return PlaylistImportResult.Failure(
                    if (parsed.skippedLines > 0) "No playable stream URLs found in this playlist (${parsed.skippedLines} entries were not valid URLs)"
                    else "The playlist has no entries"
                )
            }
        }
        // same stream URL twice in a file -> keep the first one (a playlist stores each path once)
        val unique = LinkedHashMap<String, M3uEntry>()
        for (e in entries) if (!unique.containsKey(e.url)) unique[e.url] = e

        val customUa = userAgent?.trim()?.takeIf { it.isNotEmpty() }
        val metas = unique.values.map { e ->
            StreamEntryMeta(
                url = e.url,
                name = e.name,
                logo = e.logo,
                group = e.group,
                tvgId = e.tvgId,
                // a per-channel option from the file wins over the playlist-wide custom User-Agent
                userAgent = e.userAgent ?: customUa,
                referrer = e.referrer,
                extraHeaders = e.extraHeaders
            )
        }
        ensureActive()
        StreamEntryStore.attach(context)
        StreamEntryStore.putAll(metas)

        val name = parsed.title?.takeIf { it.isNotBlank() } ?: fallbackName
        val playlist = PlaylistManager.createPlaylistWithPaths(
            context = context,
            name = name,
            mediaType = mediaType,
            isOnline = true,
            paths = metas.map { it.url }
        )
        return PlaylistImportResult.Success(playlist, metas.size)
    }

    /** Downloads the playlist text. Throws on network / HTTP errors (mapped by [describeError]). */
    private fun download(url: String, userAgent: String?): String {
        val ua = userAgent?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_FETCH_UA
        val request = Request.Builder().url(url).header("User-Agent", ua).header("Accept", "*/*").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpStatusException(resp.code)
            val body = resp.body ?: throw IOException("Empty response")
            val declared = body.contentLength()
            if (declared > MAX_PLAYLIST_BYTES) throw IOException("Playlist is too large")
            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var total = 0L
            body.byteStream().use { input ->
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_PLAYLIST_BYTES) throw IOException("Playlist is too large")
                    out.write(buf, 0, n)
                }
            }
            return String(out.toByteArray(), Charsets.UTF_8)
        }
    }

    private class HttpStatusException(val code: Int) : IOException("HTTP $code")

    private fun readLimited(context: Context, uri: Uri): ByteArray? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_PLAYLIST_BYTES) throw IOException("File is too large")
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        }
    }

    private fun displayNameOf(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Throwable) { null }
    }

    private fun nameFromUrl(url: String): String {
        val uri = try { URI(url) } catch (_: Throwable) { null }
        val seg = uri?.path?.trimEnd('/')?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
        val host = uri?.host?.removePrefix("www.")
        val generic = setOf("playlist", "playlists", "index", "list", "channels", "get", "tv")
        return when {
            seg != null && seg.lowercase() !in generic -> seg
            host != null && seg != null -> "$host $seg"
            host != null -> host
            else -> "Imported playlist"
        }
    }

    // ------------------------------------------------------------------ YouTube

    private fun importYouTubePlaylist(
        context: Context,
        listId: String,
        mediaType: String,
        ensureActive: () -> Unit
    ): PlaylistImportResult {
        OnlineMusicService.ensureInit()
        val target = "https://www.youtube.com/playlist?list=$listId"
        val extractor = try {
            ServiceList.YouTube.getPlaylistExtractor(target).also { it.fetchPage() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "YouTube playlist fetch failed: ${e.message}", e)
            return PlaylistImportResult.Failure(
                "Could not load this YouTube playlist. It may be private, deleted or an auto-generated list that YouTube does not expose. (${describeError(e)})"
            )
        }

        val title = extractor.name?.takeIf { it.isNotBlank() } ?: "YouTube playlist"
        val items = ArrayList<StreamInfoItem>()
        var note: String? = null
        try {
            var page = extractor.initialPage
            items.addAll(page.items.filterIsInstance<StreamInfoItem>())
            var pageCount = 1
            while (page.hasNextPage() && items.size < MAX_YOUTUBE_ITEMS && pageCount < MAX_YOUTUBE_PAGES) {
                page = extractor.getPage(page.nextPage)
                val more = page.items.filterIsInstance<StreamInfoItem>()
                if (more.isEmpty()) break
                items.addAll(more)
                pageCount++
            }
            if (page.hasNextPage() && items.size >= MAX_YOUTUBE_ITEMS) {
                note = "Loaded the first $MAX_YOUTUBE_ITEMS videos"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "YouTube playlist paging failed: ${e.message}", e)
            if (items.isEmpty()) {
                return PlaylistImportResult.Failure("Could not read the videos of this playlist. (${describeError(e)})")
            }
            note = "Stopped early (${describeError(e)}), loaded ${items.size} videos"
        }

        // unavailable / private entries come back without a duration: they cannot be played, so they are left out
        val results = items
            .filter { it.duration > 0L }
            .map { it.toVideoResult() }
            .distinctBy { it.videoUrl }
        if (results.isEmpty()) {
            return PlaylistImportResult.Failure("This YouTube playlist has no playable videos")
        }

        ensureActive()
        val isAudioPlaylist = mediaType.equals("AUDIO", ignoreCase = true)
        val normalizedMediaType = if (isAudioPlaylist) "AUDIO" else "VIDEO"
        OnlineMediaLibrary.rememberMetaBatch(
            context,
            if (isAudioPlaylist) OnlineMediaLibrary.KIND_AUDIO else OnlineMediaLibrary.KIND_VIDEO,
            results
        )
        val playlistPaths = if (isAudioPlaylist) {
            results.map { "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${it.videoUrl}" }
        } else {
            results.map { "${OnlineVideoService.ONLINE_VIDEO_PATH_PREFIX}${it.videoUrl}" }
        }
        if (isAudioPlaylist) {
            OnlineTrackMetadataCache.saveTracks(
                context,
                results.map { result ->
                    com.example.ui.screens.AudioTrackItem(
                        id = -(kotlin.math.abs(result.videoUrl.hashCode().toLong())) - 1L,
                        uri = Uri.EMPTY,
                        title = result.title,
                        artist = result.artist.ifBlank { "Online Music" },
                        album = title,
                        durationMs = result.durationSec * 1000L,
                        path = "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${result.videoUrl}",
                        sizeBytes = 0L,
                        format = "STREAM",
                        dateModified = (result.uploadDateEpochMs ?: System.currentTimeMillis()) / 1000L,
                        thumbnailUrl = result.thumbnailUrl
                    )
                }
            )
        }
        val playlist = PlaylistManager.createPlaylistWithPaths(
            context = context,
            name = title,
            mediaType = normalizedMediaType,
            isOnline = true,
            paths = playlistPaths
        )
        return PlaylistImportResult.Success(playlist, results.size, note)
    }


    private fun isSpotifyPlaylistUrl(url: String): Boolean {
        val lower = url.lowercase()
        return (lower.contains("open.spotify.com/playlist/") ||
            lower.contains("open.spotify.com/album/"))
    }

    private suspend fun importSpotifyPlaylist(
        context: Context,
        url: String,
        ensureActive: () -> Unit
    ): PlaylistImportResult {
        val detail = OnlineMusicService.fetchPlaylistDetails(url)
            ?: return PlaylistImportResult.Failure("Could not load this Spotify playlist. Check the link and try again.")
        val tracks = detail.tracks.filter { it.durationSec > 0L }.distinctBy { it.videoUrl }
        if (tracks.isEmpty()) return PlaylistImportResult.Failure("This Spotify playlist has no playable tracks")

        ensureActive()
        OnlineMediaLibrary.rememberMetaBatch(context, OnlineMediaLibrary.KIND_AUDIO, tracks)
        val playlistPaths = tracks.map { "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${it.videoUrl}" }
        OnlineTrackMetadataCache.saveTracks(
            context,
            tracks.map { track ->
                com.example.ui.screens.AudioTrackItem(
                    id = -(kotlin.math.abs(track.videoUrl.hashCode().toLong())) - 1L,
                    uri = Uri.EMPTY,
                    title = track.title,
                    artist = track.artist.ifBlank { detail.author.ifBlank { "Spotify" } },
                    album = detail.title,
                    durationMs = track.durationSec * 1000L,
                    path = "${com.example.ui.components.ONLINE_TRACK_PATH_PREFIX}${track.videoUrl}",
                    sizeBytes = 0L,
                    format = "STREAM",
                    dateModified = (track.uploadDateEpochMs ?: System.currentTimeMillis()) / 1000L,
                    thumbnailUrl = track.thumbnailUrl
                )
            }
        )
        val playlist = PlaylistManager.createPlaylistWithPaths(
            context = context,
            name = detail.title.ifBlank { "Spotify Playlist" },
            mediaType = "AUDIO",
            isOnline = true,
            paths = playlistPaths
        )
        return PlaylistImportResult.Success(playlist, tracks.size)
    }

    private fun StreamInfoItem.toVideoResult(): OnlineTrackResult {
        val uploadEpoch = try {
            val d = this.uploadDate
            if (d != null) d.offsetDateTime().toInstant().toEpochMilli() else 0L
        } catch (_: Throwable) { 0L }
        return OnlineTrackResult(
            videoUrl = this.url,
            title = this.name?.takeIf { it.isNotBlank() } ?: this.url,
            artist = this.uploaderName ?: "",
            durationSec = this.duration.coerceAtLeast(0L),
            thumbnailUrl = OnlineMusicService.extractBestThumbnailUrl(this.thumbnails, this.url),
            uploadDateText = this.textualUploadDate,
            uploadDateEpochMs = uploadEpoch.takeIf { it > 0L },
            viewCount = this.viewCount.coerceAtLeast(0L)
        )
    }

    // ------------------------------------------------------------------ ZIP / TAR archives (played in place, nothing is copied)

    /**
     * A ZIP / TAR file chosen in the file manager. The file stays where it is: only its table of contents is read now, and the
     * videos inside are read straight out of the file when they are played. The playlist gets the archive's own name.
     */
    suspend fun importFromArchiveFile(
        context: Context,
        uri: Uri,
        customName: String? = null
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
        val checkActive: () -> Unit = { ensureActive() }
        try {
            // keep read access after the app is closed, so the playlist still plays tomorrow
            try {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {}
            val fileName = displayNameOf(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Archive"
            importArchive(
                context = context,
                source = UriArchiveSource(context, uri),
                sourceType = "uri",
                sourceValue = uri.toString(),
                userAgent = null,
                fileName = fileName,
                customName = customName,
                ensureActive = checkActive
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ArchiveException) {
            PlaylistImportResult.Failure(e.message ?: "Could not open this archive")
        } catch (e: Throwable) {
            Log.e(TAG, "importFromArchiveFile failed: ${e.message}", e)
            PlaylistImportResult.Failure(describeError(e))
        }
    }

    /** A direct link to a ZIP / TAR file on a website. Only the needed parts are requested; the archive is not downloaded. */
    suspend fun importFromArchiveUrl(
        context: Context,
        rawUrl: String,
        customName: String?,
        userAgent: String?
    ): PlaylistImportResult = withContext(Dispatchers.IO) {
        val url = rawUrl.trim()
        validateUrl(url)?.let { return@withContext PlaylistImportResult.Failure(it) }
        val checkActive: () -> Unit = { ensureActive() }
        try {
            val decodedName: String = try {
                java.net.URLDecoder.decode(URI(url).path.orEmpty().trimEnd('/').substringAfterLast('/'), "UTF-8")
            } catch (_: Throwable) {
                ""
            }
            val fileName = decodedName.ifBlank { nameFromUrl(url) }
            importArchive(
                context = context,
                source = HttpArchiveSource(url, userAgent?.trim()?.takeIf { it.isNotEmpty() }),
                sourceType = "url",
                sourceValue = url,
                userAgent = userAgent?.trim()?.takeIf { it.isNotEmpty() },
                fileName = fileName,
                customName = customName,
                ensureActive = checkActive
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ArchiveException) {
            PlaylistImportResult.Failure(e.message ?: "Could not open this archive link")
        } catch (e: Throwable) {
            Log.e(TAG, "importFromArchiveUrl failed: ${e.message}", e)
            PlaylistImportResult.Failure(describeError(e))
        }
    }

    private fun importArchive(
        context: Context,
        source: ArchiveSource,
        sourceType: String,
        sourceValue: String,
        userAgent: String?,
        fileName: String,
        customName: String?,
        ensureActive: () -> Unit
    ): PlaylistImportResult {
        val scan = ArchiveEngine.scan(source, fileName)
        ensureActive()
        val videos = scan.entries
            .filter { ArchiveEngine.isVideoName(it.name) }
            .sortedWith(Comparator { a, b -> NaturalOrderComparator.compare(a.name, b.name) })
        if (videos.isEmpty()) {
            return PlaylistImportResult.Failure(
                when {
                    scan.lockedCount > 0 -> "This archive is password protected, so its videos cannot be played."
                    scan.unsupportedCount > 0 -> "The files in this archive use a compression type that is not supported."
                    else -> "No video files were found inside this archive"
                }
            )
        }
        val baseName = fileName.substringBeforeLast('.', fileName).let {
            // "show.tar" left over from "show.tar.gz"-like names
            if (it.endsWith(".tar", ignoreCase = true)) it.dropLast(4) else it
        }.ifBlank { "Archive" }
        val playlistName = customName?.trim()?.takeIf { it.isNotEmpty() } ?: baseName

        val record = ArchiveRecord(
            id = ArchiveStore.idFor(sourceValue),
            kind = scan.kind,
            sourceType = sourceType,
            source = sourceValue,
            userAgent = userAgent,
            displayName = playlistName,
            entries = videos
        )
        ensureActive()
        ArchiveStore.put(context, record)
        val paths = videos.indices.map { ArchiveStore.pathFor(record, it) }
        val playlist = PlaylistManager.createPlaylistWithPaths(
            context = context,
            name = playlistName,
            mediaType = "VIDEO",
            isOnline = (sourceType == "url"),
            paths = paths
        )
        val skipped = scan.lockedCount + scan.unsupportedCount
        val note = if (skipped > 0) "$skipped file${if (skipped != 1) "s" else ""} skipped (locked or unsupported)" else null
        return PlaylistImportResult.Success(playlist, videos.size, note)
    }

    // ------------------------------------------------------------------ errors

    private fun describeError(e: Throwable): String = when (e) {
        is HttpStatusException -> when (e.code) {
            401, 403 -> "The server refused access (HTTP ${e.code}). Try a custom User-Agent."
            404 -> "Playlist not found (HTTP 404)"
            else -> "The server answered with HTTP ${e.code}"
        }
        is UnknownHostException -> "No internet connection or unknown server"
        is SocketTimeoutException -> "The server took too long to answer"
        is javax.net.ssl.SSLException -> "Secure connection failed (${e.message ?: "SSL error"})"
        is IOException -> e.message?.takeIf { it.isNotBlank() } ?: "Network error"
        else -> e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
    }
}
