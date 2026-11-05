package com.example.util

import java.util.Locale

object BulkRenameEngine {

    /** Splits "name.ext" into (name, ".ext"). Names without a plausible extension keep an empty extension. */
    fun splitExtension(fileName: String): Pair<String, String> {
        val dot = fileName.lastIndexOf('.')
        if (dot <= 0 || dot == fileName.length - 1) return fileName to ""
        val ext = fileName.substring(dot)
        if (ext.length > 8 || ext.any { it.isWhitespace() }) return fileName to ""
        return fileName.substring(0, dot) to ext
    }

    private val illegal = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    fun sanitize(name: String): String {
        var r = illegal.replace(name, "_")
        r = r.trim().trimEnd('.')
        return r
    }

    private fun limitLength(base: String, ext: String): String {
        var b = base
        while (b.isNotEmpty() && (b + ext).toByteArray(Charsets.UTF_8).size > 250) b = b.dropLast(1)
        return b + ext
    }

    private fun pad(n: Int, width: Int): String = n.toString().padStart(width.coerceIn(1, 6), '0')

    fun titleCase(text: String): String {
        val sb = StringBuilder(text.length)
        var newWord = true
        for (c in text) {
            if (c.isLetterOrDigit()) {
                sb.append(if (newWord) c.uppercaseChar() else c.lowercaseChar())
                newWord = false
            } else {
                sb.append(c)
                newWord = true
            }
        }
        return sb.toString()
    }

    /** Most common detected show title (case-insensitive) among [fileNames]; "" when nothing was detected. */
    fun detectSeriesName(fileNames: List<String>): String {
        val titles = fileNames.map { EpisodeNameParser.parse(splitExtension(it).first).title }
            .filter { it.isNotBlank() }
        if (titles.isEmpty()) return ""
        return titles.groupBy { it.lowercase(Locale.ROOT) }.maxByOrNull { it.value.size }?.value?.first() ?: ""
    }

    /** How many of [fileNames] have a detectable episode number. */
    fun countEpisodesDetected(fileNames: List<String>): Int =
        fileNames.count { EpisodeNameParser.parse(splitExtension(it).first).episode != null }

    /** Longest episode number among the files – used to pick a sensible default digit padding. */
    fun maxDetectedEpisode(fileNames: List<String>): Int =
        fileNames.maxOfOrNull { EpisodeNameParser.parse(splitExtension(it).first).episode ?: 0 } ?: 0

    /**
     * Computes the new name of every item. [items] must already be in the wanted order (the order is what the
     * Sequence mode numbers by). [takenNames] = lower-cased names that already exist in each folder and are NOT
     * part of this rename (a rename may never overwrite them).
     */
    fun compute(
        items: List<BulkRenameItem>,
        cfg: BulkRenameConfig,
        takenNames: Map<String, Set<String>> = emptyMap()
    ): List<BulkRenamePreview> {
        val raw = ArrayList<Pair<BulkRenameItem, String?>>(items.size) // null = no match -> keep as is
        items.forEachIndexed { index, item ->
            raw.add(item to newNameFor(item, index, cfg))
        }

        val used = HashMap<String, String>() // dir + lower name -> item key
        val out = ArrayList<BulkRenamePreview>(items.size)
        for ((item, candidate) in raw) {
            if (candidate == null) {
                out.add(BulkRenamePreview(item, item.fileName, BulkRenameStatus.NO_MATCH))
                continue
            }
            val changed = candidate != item.fileName
            var status = if (changed) BulkRenameStatus.CHANGED else BulkRenameStatus.UNCHANGED
            var reason: String? = null
            if (candidate.isBlank() || candidate == ".") {
                status = BulkRenameStatus.CONFLICT
                reason = "Empty name"
            }
            val key = item.dir + "\u0000" + candidate.lowercase(Locale.ROOT)
            val clash = used[key]
            if (status != BulkRenameStatus.CONFLICT && clash != null && clash != item.key) {
                status = BulkRenameStatus.CONFLICT
                reason = "Same name as another file"
            }
            if (status != BulkRenameStatus.CONFLICT &&
                changed &&
                takenNames[item.dir]?.contains(candidate.lowercase(Locale.ROOT)) == true
            ) {
                status = BulkRenameStatus.CONFLICT
                reason = "A file with this name already exists"
            }
            if (clash == null) used[key] = item.key
            out.add(BulkRenamePreview(item, candidate, status, reason))
        }
        // The first file of a clashing pair is flagged too.
        val clashingKeys = out.filter { it.status == BulkRenameStatus.CONFLICT && it.conflictReason == "Same name as another file" }
            .map { it.item.dir + "\u0000" + it.newName.lowercase(Locale.ROOT) }.toSet()
        return out.map { p ->
            val k = p.item.dir + "\u0000" + p.newName.lowercase(Locale.ROOT)
            if (p.status == BulkRenameStatus.CHANGED && k in clashingKeys) {
                p.copy(status = BulkRenameStatus.CONFLICT, conflictReason = "Same name as another file")
            } else p
        }
    }

    private fun newNameFor(item: BulkRenameItem, index: Int, cfg: BulkRenameConfig): String? {
        val (baseName, ext) = if (cfg.keepExtension) splitExtension(item.fileName) else item.fileName to ""
        val newBase: String = when (cfg.mode) {
            BulkRenameMode.SEQUENCE -> {
                val num = pad(cfg.startNumber + index, cfg.padding)
                if (cfg.pattern.contains("{num}")) cfg.pattern.replace("{num}", num) else cfg.pattern + num
            }
            BulkRenameMode.FIND_REPLACE -> {
                if (cfg.find.isEmpty()) baseName
                else baseName.replace(cfg.find, cfg.replace, ignoreCase = !cfg.caseSensitive)
            }
            BulkRenameMode.PREFIX_SUFFIX -> cfg.prefix + baseName + cfg.suffix
            BulkRenameMode.CHANGE_CASE -> when (cfg.caseFormat) {
                BulkCaseFormat.LOWER -> baseName.lowercase()
                BulkCaseFormat.UPPER -> baseName.uppercase()
                BulkCaseFormat.TITLE -> titleCase(baseName)
            }
            BulkRenameMode.SMART_EPISODE -> {
                val parsed = EpisodeNameParser.parse(splitExtension(item.fileName).first)
                val episode = parsed.episode ?: return null
                val season = cfg.seasonOverride ?: parsed.season ?: 1
                val name = cfg.seriesName.trim().ifEmpty { parsed.title }
                val epText = pad(episode, cfg.episodePadding) +
                    (parsed.episodeEnd?.let { "-E" + pad(it, cfg.episodePadding) } ?: "")
                cfg.template
                    .replace("{name}", name)
                    .replace("{season}", pad(season, 2))
                    .replace("{episode}", epText)
                    .trim()
                    .trimStart('-', '_', '.', ' ')
            }
        }
        val cleaned = sanitize(newBase)
        if (cleaned.isEmpty()) return ""
        return limitLength(cleaned, ext)
    }
}
