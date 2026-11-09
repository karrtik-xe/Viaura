package com.example.ui.state

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.util.PlaybackHistoryManager
import com.example.util.ViauraSettingsXmlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Settings Dashboard > Advanced: the engine-level options (mpv.conf, input.conf, Lua / JS scripts, logging, data and cache
 * cleaning, automatic settings backup). Everything is stored in its own small preference file, so these options do not
 * touch the big UiState / backup XML.
 */
object AdvancedEngineSettings {
    private const val TAG = "AdvancedEngine"
    private const val PREFS = "viaura_advanced_engine_prefs"
    private const val K_AUTO_BACKUP = "auto_backup_enabled"
    private const val K_AUTO_BACKUP_DIR = "auto_backup_dir"
    private const val K_AUTO_BACKUP_TIME = "auto_backup_last_time"
    private const val K_CONF_PRIORITY = "mpv_conf_priority"
    private const val K_LUA_ENABLED = "lua_scripts_enabled"
    private const val K_LUA_SELECTED = "lua_scripts_selected"
    private const val K_LUA_INSTALLED = "lua_scripts_installed_in_mpv"
    private const val K_RECENT = "recently_played_enabled"
    private const val K_VERBOSE = "verbose_logging"
    private const val K_LOG_MB = "debug_log_size_mb"

    private fun prefs(context: Context): SharedPreferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------------- simple options ----------------
    fun autoBackupEnabled(c: Context) = prefs(c).getBoolean(K_AUTO_BACKUP, false)
    fun setAutoBackupEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_AUTO_BACKUP, v).apply()
    fun autoBackupDir(c: Context): String = prefs(c).getString(K_AUTO_BACKUP_DIR, "").orEmpty()
    fun setAutoBackupDir(c: Context, v: String) = prefs(c).edit().putString(K_AUTO_BACKUP_DIR, v).apply()
    fun lastAutoBackupTime(c: Context) = prefs(c).getLong(K_AUTO_BACKUP_TIME, 0L)

    fun confPriority(c: Context) = prefs(c).getBoolean(K_CONF_PRIORITY, false)
    fun setConfPriority(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_CONF_PRIORITY, v).apply()

    fun luaEnabled(c: Context) = prefs(c).getBoolean(K_LUA_ENABLED, false)
    fun setLuaEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_LUA_ENABLED, v).apply()
    fun luaSelected(c: Context): Set<String> = prefs(c).getStringSet(K_LUA_SELECTED, emptySet()).orEmpty().toSet()
    fun setLuaSelected(c: Context, v: Set<String>) = prefs(c).edit().putStringSet(K_LUA_SELECTED, v).apply()

    /** Read on every video open / progress save: SharedPreferences answers from memory. */
    fun recentlyPlayedEnabled(c: Context) = prefs(c).getBoolean(K_RECENT, true)
    fun setRecentlyPlayedEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_RECENT, v).apply()

    fun verboseLogging(c: Context) = prefs(c).getBoolean(K_VERBOSE, false)
    fun setVerboseLogging(c: Context, v: Boolean) = prefs(c).edit().putBoolean(K_VERBOSE, v).apply()
    fun logSizeMb(c: Context) = prefs(c).getInt(K_LOG_MB, 3).coerceIn(1, 20)
    fun setLogSizeMb(c: Context, v: Int) = prefs(c).edit().putInt(K_LOG_MB, v.coerceIn(1, 20)).apply()

    // ---------------- mpv.conf / input.conf (mpv itself reads them from its config folder = filesDir) ----------------
    fun mpvConfFile(c: Context) = File(c.filesDir, "mpv.conf")
    fun inputConfFile(c: Context) = File(c.filesDir, "input.conf")

    fun readText(f: File): String = try { if (f.isFile) f.readText() else "" } catch (_: Throwable) { "" }

    /** Saves [text]; an empty text removes the file so mpv goes back to its defaults. */
    fun writeText(f: File, text: String): Boolean = try {
        if (text.isBlank()) f.delete() else f.writeText(text.replace("\r\n", "\n"))
        true
    } catch (t: Throwable) {
        Log.w(TAG, "could not write ${f.name}", t)
        false
    }

    /** `key=value` lines of an mpv.conf (comments, blank lines and [profile] sections are not options of the main config). */
    fun parseConf(text: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("[")) break
            val eq = line.indexOf('=')
            var key = (if (eq >= 0) line.substring(0, eq) else line).trim()
            var value = if (eq >= 0) line.substring(eq + 1).trim() else "yes"
            if (key.isEmpty()) continue
            if (eq < 0 && key.startsWith("no-")) { key = key.removePrefix("no-"); value = "no" }
            value = value.removeSurrounding("\"").removeSurrounding("'")
            if (key.startsWith("script") || key == "include" || key == "profile" || key == "config" || key == "config-dir") continue
            out.add(key to value)
        }
        return out
    }

    // ---------------- scripts ----------------
    fun scriptsLibraryDir(c: Context) = File(c.filesDir, "user_scripts_library").apply { mkdirs() }
    private fun mpvScriptsDir(c: Context) = File(c.filesDir, "scripts").apply { mkdirs() }

    fun listScripts(c: Context): List<File> =
        scriptsLibraryDir(c).listFiles { f -> f.isFile && f.extension.lowercase() in setOf("lua", "js") }.orEmpty().sortedBy { it.name.lowercase() }

    fun importScript(c: Context, name: String, bytes: ByteArray): Boolean = try {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "script.lua" }
        val finalName = if (safe.substringAfterLast('.', "").lowercase() in setOf("lua", "js")) safe else "$safe.lua"
        File(scriptsLibraryDir(c), finalName).writeBytes(bytes)
        true
    } catch (_: Throwable) {
        false
    }

    fun deleteScript(c: Context, name: String) {
        try { File(scriptsLibraryDir(c), name).delete() } catch (_: Throwable) {}
        setLuaSelected(c, luaSelected(c) - name)
    }

    /**
     * mpv loads every script of <config folder>/scripts at start. The scripts of the user's library are copied there only while
     * "Enable scripts" is ON and the script is ticked; our own copies are removed again otherwise. Scripts that the app itself
     * (or the user by hand) put into that folder are never touched.
     */
    fun syncScripts(c: Context) {
        try {
            val dir = mpvScriptsDir(c)
            val installed = prefs(c).getStringSet(K_LUA_INSTALLED, emptySet()).orEmpty().toSet()
            installed.forEach { File(dir, it).delete() }
            val now = HashSet<String>()
            if (luaEnabled(c)) {
                val library = scriptsLibraryDir(c)
                for (name in luaSelected(c)) {
                    val src = File(library, name)
                    if (!src.isFile) continue
                    src.copyTo(File(dir, name), overwrite = true)
                    now.add(name)
                }
            }
            prefs(c).edit().putStringSet(K_LUA_INSTALLED, now).apply()
        } catch (t: Throwable) {
            Log.w(TAG, "script sync failed", t)
        }
    }

    // ---------------- mpv hooks ----------------
    /** Called by the mpv view before mpv starts. */
    fun beforeMpvInit(c: Context, setOption: (String, String) -> Unit) {
        syncScripts(c)
        if (verboseLogging(c)) {
            setOption("msg-level", "all=v")
        }
    }

    /** Called right after mpv started: with "mpv.conf has priority" its options win over the app's own start-up options. */
    fun afterMpvInit(c: Context, setOption: (String, String) -> Unit) {
        if (!confPriority(c)) return
        for ((k, v) in parseConf(readText(mpvConfFile(c)))) {
            try { setOption(k, v) } catch (_: Throwable) {}
        }
    }

    // ---------------- data & cache ----------------
    fun clearPlaybackHistory(c: Context) {
        PlaybackHistoryManager.clearAllHistory(c)
    }

    fun clearThumbnailCache(c: Context): Long {
        var freed = 0L
        try { com.example.ui.components.ThumbnailCache.memoryCache.evictAll() } catch (_: Throwable) {}
        for (dir in listOf(File(c.filesDir, "thumbnails"), File(c.cacheDir, "video_thumbnail_cache"))) {
            freed += deleteContents(dir)
        }
        return freed
    }

    fun clearFontsCache(c: Context): Long {
        com.example.player.SubtitleFontManager.clearVideoEmbeddedFonts()
        // the font index that libass / fontconfig built; it is rebuilt by itself the next time a subtitle is shown
        return deleteContents(File(c.cacheDir, "fontconfig-cache"))
    }

    private fun deleteContents(dir: File): Long {
        var size = 0L
        try {
            dir.walkBottomUp().forEach { f ->
                if (f == dir) return@forEach
                if (f.isFile) size += f.length()
                f.delete()
            }
        } catch (_: Throwable) {}
        return size
    }

    // ---------------- logs ----------------
    /** Saves the last [logSizeMb] MB of this app's own log (app + mpv) to Downloads and returns the file name, or null. */
    fun dumpLogs(c: Context): String? {
        return try {
            val limit = logSizeMb(c) * 1024L * 1024L
            val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "--pid=" + android.os.Process.myPid()))
            val lines = ArrayDeque<String>()
            var bytes = 0L
            proc.inputStream.bufferedReader().useLines { seq ->
                for (l in seq) {
                    lines.addLast(l)
                    bytes += l.length + 1
                    while (bytes > limit && lines.isNotEmpty()) bytes -= lines.removeFirst().length + 1
                }
            }
            try { proc.destroy() } catch (_: Throwable) {}
            val header = buildString {
                append("Viaura debug log\n")
                append("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                append("Verbose logging: ${verboseLogging(c)}\n")
                append("mpv.conf:\n").append(readText(mpvConfFile(c)).ifBlank { "(none)" }).append("\n\n")
            }
            val name = "Viaura_log_" + java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date()) + ".txt"
            val data = (header + lines.joinToString("\n")).toByteArray(Charsets.UTF_8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Viaura")
                }
                val uri = c.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                c.contentResolver.openOutputStream(uri)?.use { it.write(data) } ?: return null
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Viaura").apply { mkdirs() }
                File(dir, name).writeBytes(data)
            }
            name
        } catch (t: Throwable) {
            Log.w(TAG, "log dump failed", t)
            null
        }
    }

    // ---------------- automatic backup ----------------
    private val backupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var backupJob: Job? = null

    /** Called whenever settings are saved: a few seconds after the last change the settings file is written to the chosen folder. */
    fun scheduleAutoBackup(context: Context, state: UiState) {
        val app = context.applicationContext
        if (!autoBackupEnabled(app)) return
        val dir = autoBackupDir(app)
        if (dir.isBlank()) return
        backupJob?.cancel()
        backupJob = backupScope.launch {
            delay(4_000L)
            runAutoBackup(app, state)
        }
    }

    fun runAutoBackup(context: Context, state: UiState): Result<File> {
        val app = context.applicationContext
        val dir = autoBackupDir(app)
        if (dir.isBlank()) return Result.failure(IllegalStateException("No backup folder is chosen"))
        val r = ViauraSettingsXmlManager.exportSettingsToFile(app, state, File(dir))
        if (r.isSuccess) prefs(app).edit().putLong(K_AUTO_BACKUP_TIME, System.currentTimeMillis()).apply()
        return r
    }
}
