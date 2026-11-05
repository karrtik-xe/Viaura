package com.example.util.archive

import android.content.Context
import android.net.Uri
import com.example.util.ExtractedMediaMetadata
import com.example.util.MediaMetadataExtractor
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds out duration, resolution, frame rate and subtitle tracks of the videos inside an archive, and gives the thumbnail
 * extractor a seekable view of a video, WITHOUT extracting or copying anything.
 *
 * Local archive: the bytes are read straight from the file. Archive link: only a few hundred KB of each video (its header)
 * are requested with HTTP Range. Neither of them ever touches the other videos of the archive.
 */
object ArchiveProbe {

    /** How often one video is tried per app session (a video that cannot be read must not be retried for ever). */
    private const val MAX_ATTEMPTS = 2
    private val attempts = ConcurrentHashMap<String, Int>()

    /** True the first [MAX_ATTEMPTS] times it is asked for the same [key]. */
    fun shouldTry(key: String): Boolean {
        val n = attempts.merge(key, 1) { a, b -> a + b } ?: 1
        return n <= MAX_ATTEMPTS
    }

    fun sourceFor(context: Context, record: ArchiveRecord): ArchiveSource =
        if (record.sourceType == "uri") UriArchiveSource(context.applicationContext, Uri.parse(record.source))
        else HttpArchiveSource(record.source, record.userAgent)

    /** True when the archive is a link on a website (reading it uses the internet). */
    fun isRemote(context: Context, archivePath: String): Boolean {
        val (id, _) = ArchiveStore.parsePath(archivePath) ?: return false
        return ArchiveStore.get(context, id)?.sourceType == "url"
    }

    /** True when the video is stored without compression, so any part of it can be read directly. */
    fun isRandomAccess(context: Context, archivePath: String): Boolean =
        ArchiveStore.entryFor(context, archivePath)?.method == 0

    /** A seekable view of the video behind an "archive://" path, or null when the archive is not known any more. */
    fun dataSourceFor(context: Context, archivePath: String): ArchiveEntryDataSource? {
        return try {
            val (id, index) = ArchiveStore.parsePath(archivePath) ?: return null
            val record = ArchiveStore.get(context, id) ?: return null
            val entry = record.entries.getOrNull(index) ?: return null
            val source = sourceFor(context, record)
            val dataOffset = ArchiveStreamServer.dataOffsetFor(source, record, index, entry)
            ArchiveEntryDataSource(source, record.kind, entry, dataOffset)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Reads the header of one video of an archive and remembers the result (duration, resolution, fps, subtitle formats) in the
     * archive's table of contents. Returns null when nothing could be read.
     */
    fun probe(context: Context, archivePath: String, displayName: String): ExtractedMediaMetadata? {
        val (id, index) = ArchiveStore.parsePath(archivePath) ?: return null
        val data = dataSourceFor(context, archivePath) ?: return null
        val remote = isRemote(context, archivePath)
        val randomAccess = isRandomAccess(context, archivePath)
        val reader = ArchiveSeekableReader(data)
        val meta = try {
            // a link only needs the first part of the file (Tracks + Info are there); a local file may be searched further (fonts)
            val scanBytes = if (remote || !randomAccess) 2L * 1024L * 1024L else 16L * 1024L * 1024L
            MediaMetadataExtractor.extractFromArchiveEntry(data, reader, displayName, scanBytes, remote || !randomAccess)
        } catch (_: Throwable) {
            null
        } finally {
            try { data.close() } catch (_: Throwable) {}
        }
        // Remembered only when something real was read. A failed read (no internet, link refused) is tried again later,
        // but never more than [MAX_ATTEMPTS] times in one session.
        if (meta != null && (meta.durationMs > 0L || meta.width > 0)) {
            ArchiveStore.updateMeta(context, id, index, meta)
        }
        return meta
    }
}
