package com.example.player

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Locale

/**
 * Persistence helper: all per-subtitle overrides are stored as ONE json string inside
 * PlayerSettings (`assStyleOverridesJson`) keyed by subtitle identity, so one subtitle's
 * customisation can never leak into another.
 */
object AssOverridesStore {
    private const val MAX_SUBTITLES = 40

    fun decode(json: String): Map<String, AssSubtitleOverrides> {
        if (json.isBlank()) return emptyMap()
        return try {
            val root = JSONObject(json)
            val out = LinkedHashMap<String, AssSubtitleOverrides>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = root.optJSONObject(key) ?: continue
                val styles = LinkedHashMap<String, Map<String, String>>()
                val stylesObj = entry.optJSONObject("s")
                if (stylesObj != null) {
                    val names = stylesObj.keys()
                    while (names.hasNext()) {
                        val styleName = names.next()
                        val fieldsObj = stylesObj.optJSONObject(styleName) ?: continue
                        val fields = LinkedHashMap<String, String>()
                        val fieldKeys = fieldsObj.keys()
                        while (fieldKeys.hasNext()) {
                            val f = fieldKeys.next()
                            val v = fieldsObj.optString(f, "")
                            if (v.isNotEmpty()) fields[f] = v
                        }
                        if (fields.isNotEmpty()) styles[styleName] = fields
                    }
                }
                val deleted = LinkedHashSet<String>()
                val deletedArr = entry.optJSONArray("d")
                if (deletedArr != null) {
                    for (i in 0 until deletedArr.length()) {
                        val n = deletedArr.optString(i, "")
                        if (n.isNotEmpty()) deleted.add(n)
                    }
                }
                out[key] = AssSubtitleOverrides(styles, deleted, entry.optLong("t", 0L), entry.optLong("r", 0L))
            }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    fun encode(all: Map<String, AssSubtitleOverrides>): String {
        val root = JSONObject()
        val kept = all.filterValues { !it.isCompletelyEmpty }
            .entries
            .sortedByDescending { it.value.touchedAt }
            .take(MAX_SUBTITLES)
        for ((key, value) in kept) {
            val entry = JSONObject()
            entry.put("t", value.touchedAt)
            if (value.rawRevision > 0L) entry.put("r", value.rawRevision)
            val stylesObj = JSONObject()
            for ((styleName, fields) in value.styles) {
                if (fields.isEmpty()) continue
                val fieldsObj = JSONObject()
                for ((f, v) in fields) fieldsObj.put(f, v)
                stylesObj.put(styleName, fieldsObj)
            }
            entry.put("s", stylesObj)
            val deletedArr = JSONArray()
            value.deleted.forEach { deletedArr.put(it) }
            entry.put("d", deletedArr)
            root.put(key, entry)
        }
        return if (root.length() == 0) "" else root.toString()
    }

    fun get(json: String, key: String): AssSubtitleOverrides = decode(json)[key] ?: AssSubtitleOverrides()

    /** True when at least one subtitle has stored overrides. */
    fun hasAny(json: String): Boolean = decode(json).values.any { !it.isCompletelyEmpty }

    fun put(json: String, key: String, value: AssSubtitleOverrides): String {
        val all = LinkedHashMap(decode(json))
        all[key] = value.copy(touchedAt = System.currentTimeMillis())
        return encode(all)
    }
}

/**
 * Disk store for raw (full text) subtitle edits applied by the user, one UTF-8 file per subtitle
 * identity ([AdvancedAssStyleEngine.contentKey] / embedded key). Files live in the app's private
 * storage; they are only written when the user presses Apply in the raw editor.
 */
object AssRawTextStore {
    private const val DIR = "subtitle_raw_edits"

    private fun fileFor(context: android.content.Context, key: String): java.io.File {
        val safe = key.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return java.io.File(java.io.File(context.applicationContext.filesDir, DIR).apply { mkdirs() }, "$safe.txt")
    }

    fun read(context: android.content.Context, key: String): String? {
        return try {
            val f = fileFor(context, key)
            if (f.isFile) f.readText(Charsets.UTF_8) else null
        } catch (_: Throwable) {
            null
        }
    }

    fun write(context: android.content.Context, key: String, text: String): Boolean {
        return try {
            val f = fileFor(context, key)
            val tmp = java.io.File(f.parentFile, f.name + ".tmp")
            tmp.writeText(text, Charsets.UTF_8)
            if (f.exists()) f.delete()
            tmp.renameTo(f) || run { f.writeText(text, Charsets.UTF_8); tmp.delete(); true }
        } catch (_: Throwable) {
            false
        }
    }

    fun delete(context: android.content.Context, key: String) {
        try { fileFor(context, key).delete() } catch (_: Throwable) {}
    }
}
