package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.util.network.ViauraHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Which GitHub releases the in-app updater looks at. */
enum class UpdateChannel(val id: String) {
    /** Normal (non-pre-release) GitHub releases. */
    STABLE("stable"),

    /** The newest release of any kind, including the "Nightly" pre-releases built by GitHub Actions. */
    PREVIEW("preview");

    companion object {
        fun fromId(id: String?): UpdateChannel = values().firstOrNull { it.id == id } ?: STABLE
    }
}

/** One installable release found on GitHub. */
data class AppUpdateInfo(
    val versionName: String,
    /** CI run number of the build (= versionCode of the APK). 0 when the release does not carry one. */
    val buildNumber: Int,
    val title: String,
    /** Release description exactly as written on GitHub (Markdown). */
    val notes: String,
    /** "Oct 25, 2026" */
    val releaseDate: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val assetName: String,
    val isPreview: Boolean,
    val pageUrl: String
)
