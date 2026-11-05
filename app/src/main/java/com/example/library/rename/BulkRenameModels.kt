package com.example.util

import java.util.Locale

enum class BulkRenameMode { SMART_EPISODE, SEQUENCE, FIND_REPLACE, PREFIX_SUFFIX, CHANGE_CASE }

enum class BulkCaseFormat { LOWER, UPPER, TITLE }

/** One file taking part in a bulk rename. [key] is its full path (unique). */
data class BulkRenameItem(
    val key: String,
    val dir: String,
    val fileName: String
)

data class BulkRenameConfig(
    val mode: BulkRenameMode = BulkRenameMode.SEQUENCE,
    val keepExtension: Boolean = true,
    // Sequence
    val pattern: String = "File_",
    val startNumber: Int = 1,
    val padding: Int = 2,
    // Find & replace
    val find: String = "",
    val replace: String = "",
    val caseSensitive: Boolean = false,
    // Prefix / suffix
    val prefix: String = "",
    val suffix: String = "",
    // Change case
    val caseFormat: BulkCaseFormat = BulkCaseFormat.LOWER,
    // Smart episode
    val seriesName: String = "",
    /** null = take the season from each file name (falls back to 1). */
    val seasonOverride: Int? = null,
    val template: String = DEFAULT_TEMPLATE,
    val episodePadding: Int = 2
) {
    companion object {
        const val DEFAULT_TEMPLATE = "{name} - S{season}E{episode}"
    }
}

enum class BulkRenameStatus { CHANGED, UNCHANGED, CONFLICT, NO_MATCH }

data class BulkRenamePreview(
    val item: BulkRenameItem,
    val newName: String,
    val status: BulkRenameStatus,
    val conflictReason: String? = null
)
