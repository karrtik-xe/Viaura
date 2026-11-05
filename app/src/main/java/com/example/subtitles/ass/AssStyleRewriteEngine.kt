package com.example.player

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Locale

object AdvancedAssStyleEngine {

    private const val SECTION_OTHER = 0
    private const val SECTION_STYLES = 1
    private const val SECTION_EVENTS = 2

    /** Stable identity of a subtitle script: first 16 hex chars of the SHA-1 of its text. */
    fun contentKey(text: String): String = sha1Hex(text).take(16)

    fun sha1Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) sb.append(String.format(Locale.US, "%02x", b.toInt() and 0xFF))
        return sb.toString()
    }

    private fun isStylesSectionName(name: String): Boolean =
        name.startsWith("v4") && name.endsWith("styles")

    private fun parseFormatColumns(line: String): List<String> {
        val body = line.substring(line.indexOf(':') + 1)
        return body.split(',').map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
    }

    private fun normStyleName(name: String): String =
        name.trim().removePrefix("*").trim().lowercase(Locale.ROOT)

    /**
     * Returns the Style definitions of an ASS/SSA script, or null when [text] has no
     * `[V4+ Styles]` / `[V4 Styles]` section (i.e. it is not an ASS/SSA script). Never throws
     * for malformed input.
     */
    fun parse(text: String): AssStylesDocument? {
        return try {
            parseInternal(text)
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseInternal(text: String): AssStylesDocument? {
        if (text.isBlank()) return null
        val lines = text.split("\n")
        var section = SECTION_OTHER
        var stylesSectionSeen = false
        var isPlus = true
        var styleFormat: List<String>? = null
        val styleLineIndices = ArrayList<Int>()
        var eventFormat: List<String>? = null
        val eventLineIndices = ArrayList<Int>()

        for (i in lines.indices) {
            var line = lines[i]
            if (i == 0 && line.startsWith("\uFEFF")) line = line.substring(1)
            val t = line.trim()
            if (t.isEmpty()) continue
            if (t.startsWith("[")) {
                val close = t.indexOf(']')
                if (close > 0) {
                    val name = t.substring(1, close).trim().lowercase(Locale.ROOT)
                    section = when {
                        isStylesSectionName(name) && !stylesSectionSeen -> {
                            stylesSectionSeen = true
                            isPlus = name.contains('+')
                            SECTION_STYLES
                        }
                        name == "events" -> SECTION_EVENTS
                        else -> SECTION_OTHER
                    }
                    continue
                }
            }
            when (section) {
                SECTION_STYLES -> {
                    if (t.startsWith("format:", ignoreCase = true)) {
                        if (styleFormat == null) styleFormat = parseFormatColumns(t)
                    } else if (t.startsWith("style:", ignoreCase = true)) {
                        styleLineIndices.add(i)
                    }
                }
                SECTION_EVENTS -> {
                    if (t.startsWith("format:", ignoreCase = true)) {
                        if (eventFormat == null) eventFormat = parseFormatColumns(t)
                    } else if (t.startsWith("dialogue:", ignoreCase = true) ||
                        t.startsWith("comment:", ignoreCase = true)
                    ) {
                        eventLineIndices.add(i)
                    }
                }
            }
        }
        if (!stylesSectionSeen) return null

        val format = styleFormat?.takeIf { it.contains("name") }
            ?: if (isPlus) AssFields.V4PLUS_DEFAULT_FORMAT else AssFields.V4_DEFAULT_FORMAT
        val nameIdx = format.indexOf("name")

        val evFormat = eventFormat ?: listOf(
            "layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text"
        )
        val eventStyleCol = evFormat.indexOf("style").let { if (it >= 0) it else 3 }
        val eventColCount = evFormat.size

        val usage = HashMap<String, Int>()
        for (idx in eventLineIndices) {
            val line = lines[idx]
            val colon = line.indexOf(':')
            if (colon < 0) continue
            val bounds = fieldBounds(line, colon + 1, eventStyleCol, eventColCount) ?: continue
            val key = normStyleName(line.substring(bounds[0], bounds[1]))
            usage[key] = (usage[key] ?: 0) + 1
        }

        val styles = ArrayList<AssStyleInfo>()
        val seen = HashSet<String>()
        for (idx in styleLineIndices) {
            val raw = lines[idx].trimEnd('\r')
            val colon = raw.indexOf(':')
            if (colon < 0) continue
            val body = raw.substring(colon + 1).trimStart(' ', '\t')
            val parts = body.split(',', limit = format.size).map { it.trim() }.toMutableList()
            while (parts.size < format.size) parts.add("")
            val name = parts[nameIdx]
            if (name.isEmpty()) continue
            val key = normStyleName(name)
            if (!seen.add(key)) continue // duplicate definition: only the first one is exposed
            styles.add(AssStyleInfo(name, parts, idx, usage[key] ?: 0))
        }
        return AssStylesDocument(isPlus, format, styles, eventLineIndices, eventStyleCol, eventColCount)
    }

    /**
     * Start (inclusive) / end (exclusive) offsets of comma separated column [index] inside [s],
     * starting to count at [from]. The last column of the format swallows any further commas
     * (that is where Dialogue text lives), so it is never split.
     */
    private fun fieldBounds(s: String, from: Int, index: Int, columnCount: Int): IntArray? {
        var start = from
        var field = 0
        var i = from
        while (i < s.length) {
            if (s[i] == ',') {
                if (field == index) return intArrayOf(start, i)
                field++
                start = i + 1
                if (field >= columnCount - 1) {
                    return if (field == index) intArrayOf(start, s.length) else null
                }
            }
            i++
        }
        return if (field == index) intArrayOf(start, s.length) else null
    }

    /**
     * Returns [text] with [overrides] applied to the Style lines. When [overrides] is empty (or
     * the text is not an ASS/SSA script) [text] is returned unchanged.
     */
    fun apply(text: String, overrides: AssSubtitleOverrides): String {
        if (overrides.isEmpty) return text
        return try {
            applyInternal(text, overrides)
        } catch (_: Throwable) {
            text
        }
    }

    private fun applyInternal(text: String, overrides: AssSubtitleOverrides): String {
        val doc = parseInternal(text) ?: return text
        val lines = text.split("\n")
        val nameIdx = doc.formatColumns.indexOf("name")

        val removedLines = HashSet<Int>()
        val deletedNorm = overrides.deleted.map { normStyleName(it) }.toSet()
        for (s in doc.styles) {
            if (normStyleName(s.name) in deletedNorm && s.canBeRemoved) removedLines.add(s.lineIndex)
        }

        // Names that exist after removals; used to reject renames that would collide.
        val taken = HashSet<String>()
        for (s in doc.styles) if (s.lineIndex !in removedLines) taken.add(normStyleName(s.name))

        val renames = HashMap<String, String>() // normalised old name -> new name
        val rewrittenStyleNames = HashMap<Int, String>() // lineIndex -> new name
        for (s in doc.styles) {
            if (s.lineIndex in removedLines || s.isDefault) continue
            val requested = overrides.fieldOf(s.name, AssFields.NAME) ?: continue
            val newName = AssFields.normalize(AssFields.NAME, requested) ?: continue
            val oldKey = normStyleName(s.name)
            val newKey = normStyleName(newName)
            if (newName == s.name || newKey.isEmpty()) continue
            if (newKey != oldKey && newKey in taken) continue
            taken.remove(oldKey)
            taken.add(newKey)
            renames[oldKey] = newName
            rewrittenStyleNames[s.lineIndex] = newName
        }

        val rewritten = HashMap<Int, String>()
        for (s in doc.styles) {
            if (s.lineIndex in removedLines) continue
            val edits = overrides.styles[s.name]
            val newName = rewrittenStyleNames[s.lineIndex]
            if ((edits == null || edits.isEmpty()) && newName == null) continue
            rewritten[s.lineIndex] = rewriteStyleLine(lines[s.lineIndex], doc, edits ?: emptyMap(), newName, nameIdx)
        }

        if (renames.isNotEmpty()) {
            for (idx in doc.eventLineIndices) {
                val line = lines[idx]
                val colon = line.indexOf(':')
                if (colon < 0) continue
                val bounds = fieldBounds(line, colon + 1, doc.eventStyleColumn, doc.eventColumnCount) ?: continue
                val current = line.substring(bounds[0], bounds[1])
                val newName = renames[normStyleName(current)] ?: continue
                val star = if (current.trim().startsWith("*")) "*" else ""
                rewritten[idx] = line.substring(0, bounds[0]) + star + newName + line.substring(bounds[1])
            }
        }

        val sb = StringBuilder(text.length + 256)
        for (i in lines.indices) {
            if (i !in removedLines) {
                sb.append(rewritten[i] ?: lines[i])
                if (i < lines.lastIndex) sb.append('\n')
            }
        }
        return sb.toString()
    }

    private fun rewriteStyleLine(
        raw: String,
        doc: AssStylesDocument,
        edits: Map<String, String>,
        newName: String?,
        nameIdx: Int
    ): String {
        val hasCr = raw.endsWith("\r")
        val line = if (hasCr) raw.dropLast(1) else raw
        val colon = line.indexOf(':')
        if (colon < 0) return raw
        val head = line.substring(0, colon + 1)
        val rest = line.substring(colon + 1)
        val lead = rest.takeWhile { it == ' ' || it == '\t' }
        val body = rest.substring(lead.length)
        val columns = doc.formatColumns.size
        val parts = body.split(',', limit = columns).toMutableList()
        while (parts.size < columns) parts.add("")

        for ((field, value) in edits) {
            if (field == AssFields.NAME) continue
            val idx = AssFields.columnIndex(doc.formatColumns, field)
            if (idx < 0) continue // this format cannot store the field: never write it elsewhere
            val normalized = AssFields.normalize(field, value) ?: continue
            parts[idx] = if (field == AssFields.ALIGNMENT && !doc.isPlusFormat) {
                AssFields.assAlignmentToSsa(normalized.toInt()).toString()
            } else {
                normalized
            }
        }
        if (newName != null && nameIdx in parts.indices) parts[nameIdx] = newName
        return head + lead + parts.joinToString(",") + (if (hasCr) "\r" else "")
    }
}
