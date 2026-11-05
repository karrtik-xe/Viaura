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
    private const val PREFS = "crash_reporter"
    private const val KEY_LAST_EXIT_TS = "last_exit_ts"
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext
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
        return nativeCrashOfPreviousRun(app)
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

    private fun nativeCrashOfPreviousRun(app: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return try {
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastSeen = prefs.getLong(KEY_LAST_EXIT_TS, 0L)
            val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val newest = am.getHistoricalProcessExitReasons(app.packageName, 0, 1).firstOrNull() ?: return null
            if (newest.timestamp <= lastSeen) return null
            prefs.edit().putLong(KEY_LAST_EXIT_TS, newest.timestamp).apply()
            val label = when (newest.reason) {
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash (libmpv / JNI)"
                ApplicationExitInfo.REASON_ANR -> "App not responding (ANR)"
                ApplicationExitInfo.REASON_CRASH -> "Crash"
                else -> return null
            }
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(newest.timestamp))
            "$label\nTime: $stamp\nDetails: ${newest.description ?: "-"}\n" +
                "Device: ${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        } catch (_: Throwable) {
            null
        }
    }
}
