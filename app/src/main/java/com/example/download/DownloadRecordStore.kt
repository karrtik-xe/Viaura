package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** Download list. It is kept on disk; unfinished downloads are started again by [DownloadManager.resumeInterrupted]. */
object DownloadStore {
    private const val PREFS = "viaura_downloads_v2"
    private const val KEY = "items"
    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items

    @Volatile private var loaded = false
    @Volatile private var lastPersist = 0L
    private val lock = Any()

    @Synchronized
    fun init(context: Context) {
        if (loaded) return
        loaded = true
        val app = context.applicationContext
        val text = try { app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) } catch (_: Throwable) { null }
        if (text.isNullOrBlank()) return
        val list = ArrayList<DownloadItem>()
        try {
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    DownloadItem(
                        id = o.getString("id"), source = o.optString("source"), title = o.optString("title"),
                        subtitle = o.optString("subtitle").takeIf { it.isNotBlank() }, isAudio = o.optBoolean("isAudio"),
                        status = try { DownloadStatus.valueOf(o.optString("status")) } catch (_: Throwable) { DownloadStatus.PAUSED },
                        downloadedBytes = o.optLong("downloaded"), totalBytes = o.optLong("total"), note = o.optString("note").takeIf { it.isNotBlank() },
                        filePath = o.optString("filePath").takeIf { it.isNotBlank() }, fileUri = o.optString("fileUri").takeIf { it.isNotBlank() },
                        createdAt = o.optLong("createdAt"), thumbnailUrl = o.optString("thumb").takeIf { it.isNotBlank() },
                        sourceType = try { DownloadSourceType.valueOf(o.optString("type")) } catch (_: Throwable) { DownloadSourceType.NETWORK_LINK },
                        torrentFileIndex = o.optInt("torrentIndex", -1), planJson = o.optString("plan").takeIf { it.isNotBlank() },
                        detail = o.optString("detail").takeIf { it.isNotBlank() },
                        group = o.optString("group").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Throwable) {
        }
        _items.value = list
    }

    private fun persist(context: Context) {
        lastPersist = System.currentTimeMillis()
        try {
            val arr = JSONArray()
            for (it in _items.value) {
                arr.put(
                    JSONObject().put("id", it.id).put("source", it.source).put("title", it.title).put("subtitle", it.subtitle ?: "")
                        .put("isAudio", it.isAudio).put("status", it.status.name).put("downloaded", it.downloadedBytes).put("total", it.totalBytes)
                        .put("note", it.note ?: "").put("filePath", it.filePath ?: "").put("fileUri", it.fileUri ?: "")
                        .put("createdAt", it.createdAt).put("thumb", it.thumbnailUrl ?: "").put("type", it.sourceType.name)
                        .put("torrentIndex", it.torrentFileIndex).put("plan", it.planJson ?: "").put("detail", it.detail ?: "").put("group", it.group ?: "")
                )
            }
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
        } catch (_: Throwable) {
        }
    }

    /** Ids of downloads that were running (or waiting) when the app was closed. */
    fun takeInterrupted(): List<String> =
        _items.value.filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PROCESSING }.map { it.id }

    fun get(id: String): DownloadItem? = _items.value.firstOrNull { it.id == id }

    fun add(context: Context, item: DownloadItem) {
        synchronized(lock) {
            _items.value = listOf(item) + _items.value
            persist(context)
        }
    }

    fun update(context: Context, id: String, persist: Boolean = false, block: (DownloadItem) -> DownloadItem) {
        synchronized(lock) {
            var statusChanged = false
            _items.value = _items.value.map { item ->
                if (item.id == id) {
                    val n = block(item)
                    if (n.status != item.status) statusChanged = true
                    n
                } else item
            }
            if (persist || statusChanged || System.currentTimeMillis() - lastPersist > 4000L) persist(context)
        }
    }

    fun remove(context: Context, id: String) {
        synchronized(lock) {
            _items.value = _items.value.filterNot { it.id == id }
            persist(context)
        }
    }
}
