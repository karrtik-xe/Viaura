package com.example.util.archive

import android.content.Context
import android.content.SharedPreferences
import com.example.util.ExtractedMediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Everything a playlist needs to play the videos of one archive again later (where it is + its table of contents). */
class ArchiveRecord(
    val id: String,
    val kind: ArchiveKind,
    /** "uri" = a file on the phone (content:// or file://), "url" = a link on a website. */
    val sourceType: String,
    val source: String,
    val userAgent: String?,
    val displayName: String,
    /** Only the playable videos, in playlist order. The playlist stores the index into this list. */
    val entries: List<ArchiveEntry>
)

object ArchiveStore {
    private const val PREFS = "archive_playlists_prefs"
    private const val KEY = "archives_json"
    const val PATH_PREFIX = "archive://"

    @Volatile private var cache: MutableMap<String, ArchiveRecord>? = null

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun idFor(source: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(source.toByteArray(Charsets.UTF_8))
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    /** "archive://<id>/<index>/<file name>" - the file name is last so the path keeps its video extension. */
    fun pathFor(record: ArchiveRecord, index: Int): String {
        val name = record.entries[index].name.substringAfterLast('/').replace('\\', '_')
        return "$PATH_PREFIX${record.id}/$index/$name"
    }

    fun isArchivePath(path: String): Boolean = path.startsWith(PATH_PREFIX)

    /** Returns (archive id, entry index) of an "archive://" path. */
    fun parsePath(path: String): Pair<String, Int>? {
        if (!isArchivePath(path)) return null
        val parts = path.removePrefix(PATH_PREFIX).split('/', limit = 3)
        if (parts.size < 2) return null
        val index = parts[1].toIntOrNull() ?: return null
        return parts[0] to index
    }

    @Synchronized
    private fun load(context: Context): MutableMap<String, ArchiveRecord> {
        cache?.let { return it }
        val map = LinkedHashMap<String, ArchiveRecord>()
        try {
            val raw = prefs(context).getString(KEY, null)
            if (raw != null) {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val list = ArrayList<ArchiveEntry>()
                    val entries = o.getJSONArray("entries")
                    for (j in 0 until entries.length()) {
                        val e = entries.getJSONArray(j)
                        val dur = if (e.length() > 6) e.optLong(6, 0L) else 0L
                        val modified = if (e.length() > 7) e.optLong(7, 0L) else 0L
                        val resolution = if (e.length() > 8) e.optString(8, "") else ""
                        val fps = if (e.length() > 9) e.optDouble(9, 0.0).let { if (it.isNaN()) 0.0 else it } else 0.0
                        val subs = if (e.length() > 10) e.optString(10, "").split('|').filter { it.isNotBlank() } else emptyList()
                        val probed = if (e.length() > 11) e.optInt(11, 0) == 1 else false
                        list.add(
                            ArchiveEntry(
                                e.getString(0), e.getLong(1), e.getLong(2), e.getInt(3), e.getLong(4), e.getLong(5), dur,
                                modifiedMs = modified, resolution = resolution, framerate = fps, subtitleFormats = subs, metaProbed = probed
                            )
                        )
                    }
                    val record = ArchiveRecord(
                        id = o.getString("id"),
                        kind = if (o.optString("kind") == "TAR") ArchiveKind.TAR else ArchiveKind.ZIP,
                        sourceType = o.getString("srcType"),
                        source = o.getString("src"),
                        userAgent = o.optString("ua", "").takeIf { it.isNotBlank() },
                        displayName = o.optString("name", "Archive"),
                        entries = list
                    )
                    map[record.id] = record
                }
            }
        } catch (_: Throwable) {}
        cache = map
        return map
    }

    @Synchronized
    private fun persist(context: Context, map: Map<String, ArchiveRecord>) {
        val array = JSONArray()
        for (r in map.values) {
            val o = JSONObject()
            o.put("id", r.id)
            o.put("kind", r.kind.name)
            o.put("srcType", r.sourceType)
            o.put("src", r.source)
            o.put("ua", r.userAgent ?: "")
            o.put("name", r.displayName)
            val entries = JSONArray()
            for (e in r.entries) {
                entries.put(
                    JSONArray().put(e.name).put(e.size).put(e.compressedSize).put(e.method).put(e.headerOffset).put(e.dataOffset)
                        .put(e.durationMs).put(e.modifiedMs).put(e.resolution).put(e.framerate)
                        .put(e.subtitleFormats.joinToString("|")).put(if (e.metaProbed) 1 else 0)
                )
            }
            o.put("entries", entries)
            array.put(o)
        }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    @Synchronized
    fun updateDuration(context: Context, id: String, index: Int, durationMs: Long) {
        if (durationMs <= 0L) return
        val map = load(context)
        val record = map[id] ?: return
        val entry = record.entries.getOrNull(index) ?: return
        if (entry.durationMs == durationMs) return
        val updatedEntries = record.entries.toMutableList()
        updatedEntries[index] = entry.copy(durationMs = durationMs)
        val updatedRecord = ArchiveRecord(
            id = record.id,
            kind = record.kind,
            sourceType = record.sourceType,
            source = record.source,
            userAgent = record.userAgent,
            displayName = record.displayName,
            entries = updatedEntries
        )
        map[id] = updatedRecord
        persist(context, map)
    }

    @Synchronized
    fun put(context: Context, record: ArchiveRecord) {
        val map = load(context)
        // the same archive added again keeps what was already found out about its videos (duration, resolution, subtitles ...)
        val previous = map[record.id]?.entries?.associateBy { it.name }
        val merged = if (previous == null) record else ArchiveRecord(
            id = record.id,
            kind = record.kind,
            sourceType = record.sourceType,
            source = record.source,
            userAgent = record.userAgent,
            displayName = record.displayName,
            entries = record.entries.map { e ->
                val old = previous[e.name]
                if (old == null || old.size != e.size) e
                else e.copy(
                    durationMs = old.durationMs,
                    resolution = old.resolution,
                    framerate = old.framerate,
                    subtitleFormats = old.subtitleFormats,
                    metaProbed = old.metaProbed
                )
            }
        )
        map[record.id] = merged
        persist(context, map)
    }

    /** The table-of-contents line of the video behind an "archive://" path. */
    @Synchronized
    fun entryFor(context: Context, path: String): ArchiveEntry? {
        val (id, index) = parsePath(path) ?: return null
        return load(context)[id]?.entries?.getOrNull(index)
    }

    /** The name of the video INSIDE the archive (with its folders), e.g. "Season 1/Episode 01.mkv". */
    fun innerPathFor(context: Context, path: String): String? = entryFor(context, path)?.name

    /** Remembers what the background probe found out about one video, so the folder shows it at once next time. */
    @Synchronized
    fun updateMeta(context: Context, id: String, index: Int, meta: ExtractedMediaMetadata) {
        val map = load(context)
        val record = map[id] ?: return
        val entry = record.entries.getOrNull(index) ?: return
        val updated = entry.copy(
            durationMs = if (meta.durationMs > 0L) meta.durationMs else entry.durationMs,
            resolution = if (meta.resolution.isNotBlank()) meta.resolution else entry.resolution,
            framerate = if (meta.framerate > 0.0) meta.framerate else entry.framerate,
            subtitleFormats = meta.effectiveSubtitleFormats.ifEmpty { entry.subtitleFormats },
            metaProbed = true
        )
        val entries = record.entries.toMutableList()
        entries[index] = updated
        map[id] = ArchiveRecord(
            id = record.id,
            kind = record.kind,
            sourceType = record.sourceType,
            source = record.source,
            userAgent = record.userAgent,
            displayName = record.displayName,
            entries = entries
        )
        persist(context, map)
    }

    @Synchronized
    fun get(context: Context, id: String): ArchiveRecord? = load(context)[id]

    /** Forgets archives that no playlist uses any more. */
    @Synchronized
    fun prune(context: Context, usedIds: Set<String>) {
        val map = load(context)
        if (map.keys.removeAll { it !in usedIds }) persist(context, map)
    }

    /** Name shown for each video: the file name, or the folder path too when two folders hold the same file name. */
    fun displayNames(record: ArchiveRecord): List<String> {
        val short = record.entries.map { it.name.substringAfterLast('/') }
        val hasDuplicates = short.toSet().size != short.size
        return if (hasDuplicates) record.entries.map { it.name } else short
    }
}
