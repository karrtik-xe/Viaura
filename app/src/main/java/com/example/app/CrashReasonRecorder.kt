package com.example.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the reason of the last crash so it can be read on the phone itself (no computer / logcat needed).
 *
 * - A Kotlin / Java crash is written to `last_crash.txt` right before the process dies, then the normal crash handling goes on.
 * - A native crash (libmpv, JNI) cannot be caught from Kotlin; on Android 11+ the system's own exit record of the
 *   previous run (signal / reason) is read instead.
 * [consume] returns the text once (and forgets it), MainActivity shows it in a dialog with a Copy button.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"
    private const val CRUMB_FILE = "breadcrumbs.txt"
    private const val CRUMB_MAX_BYTES = 24 * 1024
    private const val PREFS = "crash_reporter"
    private const val KEY_LAST_EXIT_TS = "last_exit_ts"
    @Volatile private var installed = false
    @Volatile private var crumbFile: File? = null
    private val crumbLock = Any()
    private val crumbTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    /**
     * Writes one short line ("what the app was doing") straight to a small file. It is written with a plain unbuffered
     * write, so it is already on disk when a native crash kills the process a moment later; the next start shows the last
     * lines together with the crash, which tells what happened right before it.
     */
    fun breadcrumb(message: String) {
        val f = crumbFile ?: return
        try {
            synchronized(crumbLock) {
                if (f.length() > CRUMB_MAX_BYTES) {
                    val keep = f.readBytes().let { it.copyOfRange(it.size / 2, it.size) }
                    f.writeBytes(keep)
                }
                java.io.FileOutputStream(f, true).use { out ->
                    out.write((crumbTime.format(Date()) + "  " + message + "\n").toByteArray())
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun readBreadcrumbs(app: Context): String {
        return try {
            val f = File(app.filesDir, CRUMB_FILE)
            if (!f.isFile) return ""
            val lines = f.readLines()
            lines.takeLast(45).joinToString("\n")
        } catch (_: Throwable) {
            ""
        }
    }

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
        crumbFile = File(app.filesDir, CRUMB_FILE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val text = buildString {
                    append("Time: ").append(stamp).append('\n')
                    append("Thread: ").append(thread.name).append('\n')
                    append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                    append(" / Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n\n")
                    append(sw.toString())
                    val crumbs = readBreadcrumbs(app)
                    if (crumbs.isNotBlank()) append("\n\nLast actions:\n").append(crumbs)
                }
                File(app.filesDir, FILE_NAME).writeText(text.take(60_000))
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The reason of the crash of the previous run, or null when there was none (or it was already shown). */
    fun consume(context: Context): String? {
        val app = context.applicationContext
        // The last actions of the previous run; the file is emptied right away so it only ever describes ONE run.
        val crumbs = readBreadcrumbs(app)
        clearBreadcrumbs(app)
        try {
            val f = File(app.filesDir, FILE_NAME)
            if (f.isFile) {
                val text = f.readText()
                f.delete()
                markExitSeen(app)
                if (text.isNotBlank()) return text
            }
        } catch (_: Throwable) {
        }
        return nativeCrashOfPreviousRun(app, crumbs)
    }

    private fun clearBreadcrumbs(app: Context) {
        try { File(app.filesDir, CRUMB_FILE).delete() } catch (_: Throwable) {}
    }

    private fun markExitSeen(app: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        try {
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val newest = am.getHistoricalProcessExitReasons(app.packageName, 0, 1).firstOrNull() ?: return
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST_EXIT_TS, newest.timestamp).apply()
        } catch (_: Throwable) {
        }
    }

    /** Readable pieces of the system's native crash record (it is a binary tombstone): signal, library names, function names. */
    private fun readTraceStrings(info: ApplicationExitInfo): String {
        return try {
            val bytes = info.traceInputStream?.use { it.readBytes() } ?: return ""
            val out = StringBuilder()
            val cur = StringBuilder()
            val seen = HashSet<String>()
            fun flush() {
                if (cur.length >= 6) {
                    val t = cur.toString()
                    val interesting = t.contains("lib") || t.contains("Java_") || t.contains("SIG") || t.contains("mpv") ||
                        t.contains("pc ") || t.contains("Abort") || t.contains("Fatal") || t.contains("signal") ||
                        t.contains("kotlin") || t.contains("com.example") || t.contains("::") || t.contains("art")
                    if (interesting && seen.add(t) && out.length < 6000) out.append(t.take(220)).append('\n')
                }
                cur.setLength(0)
            }
            for (b in bytes) {
                val c = b.toInt() and 0xff
                if (c in 32..126) cur.append(c.toChar()) else flush()
            }
            flush()
            out.toString().trim()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun nativeCrashOfPreviousRun(app: Context, crumbs: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastSeen = prefs.getLong(KEY_LAST_EXIT_TS, 0L)
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exits = am.getHistoricalProcessExitReasons(app.packageName, 0, 6)
            val newestTs = exits.firstOrNull()?.timestamp ?: return null
            if (newestTs <= lastSeen) return null
            prefs.edit().putLong(KEY_LAST_EXIT_TS, newestTs).apply()
            // Several records can have piled up since the last look (the phone may also restart the app by itself): the
            // newest one that is a real failure is the one to show. A normal exit (swipe away, back) is not a crash.
            val failure = exits.firstOrNull { e ->
                e.timestamp > lastSeen && when (e.reason) {
                    ApplicationExitInfo.REASON_CRASH_NATIVE,
                    ApplicationExitInfo.REASON_CRASH,
                    ApplicationExitInfo.REASON_ANR,
                    ApplicationExitInfo.REASON_SIGNALED,
                    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
                    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
                    ApplicationExitInfo.REASON_LOW_MEMORY,
                    ApplicationExitInfo.REASON_DEPENDENCY_DIED -> true
                    else -> false
                }
            } ?: return null
            val label = when (failure.reason) {
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash (libmpv / JNI)"
                ApplicationExitInfo.REASON_ANR -> "App not responding (ANR)"
                ApplicationExitInfo.REASON_CRASH -> "Crash"
                ApplicationExitInfo.REASON_SIGNALED -> "App was killed by a signal"
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "App failed to start"
                ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "App was stopped for using too many resources"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "App was stopped because the phone ran out of memory"
                else -> "App stopped by the system"
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(failure.timestamp))
            val trace = readTraceStrings(failure)
            buildString {
                append(label).append('\n')
                append("Time: ").append(stamp).append('\n')
                append("Details: ").append(failure.description ?: "-").append(" (status ").append(failure.status).append(")\n")
                append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                append(" / Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")")
                if (trace.isNotBlank()) append("\n\nSystem crash record:\n").append(trace)
                if (crumbs.isNotBlank()) append("\n\nLast actions before it:\n").append(crumbs)
            }.take(60_000)
        } catch (_: Throwable) {
            null
        }
    }
}
