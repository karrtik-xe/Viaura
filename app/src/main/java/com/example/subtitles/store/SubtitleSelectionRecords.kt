package com.example.util

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Data class representing a permanently attached user-uploaded subtitle.
 */
data class AttachedSubtitleRecord(
    val id: String,
    val filePath: String,
    val originalName: String,
    val isSelected: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis(),
    /** Where the user picked the file from (file path). Used to pick up later edits of the original. */
    val sourcePath: String = "",
    /** Same, when the file was picked through the system picker (content:// uri). */
    val sourceUri: String = ""
)

/**
 * A remembered subtitle choice for ONE video. [kind] is [CustomSubtitlePersistenceManager.KIND_EMBEDDED]
 * (built into the video file) or [CustomSubtitlePersistenceManager.KIND_UPLOADED] (external file).
 * The track is identified by name/language/index, never by mpv's numeric id (ids change per load).
 */
data class SavedSubtitleRef(
    val kind: String,
    val name: String,
    val language: String = "",
    val index: Int = -1
)

/**
 * The remembered subtitle selection of ONE video: the main subtitle and the 2nd (top) subtitle.
 * A null side means "nothing selected there" (the user really chose that).
 */
data class VideoSubtitleSelection(
    val primary: SavedSubtitleRef?,
    val secondary: SavedSubtitleRef?
)
