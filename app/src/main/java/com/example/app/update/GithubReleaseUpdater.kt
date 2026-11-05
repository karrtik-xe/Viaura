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

/**
 * In-app updater (GitHub Releases, no Play Store).
 *
 * - STABLE channel: latest normal release. PREVIEW channel: newest release of any kind (the "v<version>beta<N>" pre-releases that
 *   .github/workflows/build-apk.yml publishes after every successful build).
 * - A release is newer when its build number (read from the release's `viaura-build-<N>.txt` marker asset, or from the APK name `Viaura-v1.1beta<N>_<abi>.apk` (N = CI run number =
 *   versionCode); older names `...-beta.N...` / `_b<build>_` still work) is higher than the installed one; releases without a build number are compared by version name.
 * - The APK of the phone's ABI (universal as fallback) is downloaded to the app cache and handed to Android's installer.
 *
 * All state is Compose state: [AppUpdateHost] (sheet) and the Updates card in Settings > About simply read it.
 */
object AppUpdater {
    private const val PREFS = "viaura_update_prefs"
    private const val KEY_AUTO = "auto_check"
    private const val KEY_CHANNEL = "channel"
    private const val KEY_DISMISSED_BUILD = "dismissed_build"
    private const val KEY_DISMISSED_VERSION = "dismissed_version"
    private const val KEY_LAST_AUTO_CHECK = "last_auto_check"
    private const val AUTO_CHECK_MIN_GAP_MS = 15L * 60L * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var loaded = false

    /** "Auto Check for Updates": look for a new version every time the app starts. */
    var autoCheck by mutableStateOf(true)
        private set
    var channel by mutableStateOf(UpdateChannel.STABLE)
        private set

    /** The update the sheet shows. */
    var available by mutableStateOf<AppUpdateInfo?>(null)
        private set
    var sheetVisible by mutableStateOf(false)
    var checking by mutableStateOf(false)
        private set

    /** One line under the "Check for Updates Now" button ("You're on the latest version", errors ...). */
    var statusMessage by mutableStateOf<String?>(null)
    var downloading by mutableStateOf(false)
        private set

    /** 0..1 while downloading, -1 when the size is unknown. */
    var downloadProgress by mutableStateOf(-1f)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** The APK is downloaded and waiting for the "Install unknown apps" permission / the installer. */
    var downloadedFile by mutableStateOf<File?>(null)
        private set
    private var waitingForInstallPermission = false

    // ------------------------------------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------------------------------------

    fun init(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            autoCheck = prefs.getBoolean(KEY_AUTO, true)
            channel = UpdateChannel.fromId(prefs.getString(KEY_CHANNEL, null))
            loaded = true
        }
    }

    fun setAutoCheck(context: Context, enabled: Boolean) {
        init(context)
        autoCheck = enabled
        prefs(context).edit().putBoolean(KEY_AUTO, enabled).apply()
    }

    fun setChannel(context: Context, newChannel: UpdateChannel) {
        init(context)
        if (newChannel == channel) return
        channel = newChannel
        prefs(context).edit().putString(KEY_CHANNEL, newChannel.id).apply()
        // what was found on the other channel does not apply any more
        available = null
        sheetVisible = false
        downloadedFile = null
        errorMessage = null
        statusMessage = null
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun installedVersionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: BuildConfig.VERSION_NAME
    } catch (_: Throwable) {
        BuildConfig.VERSION_NAME
    }

    // ------------------------------------------------------------------------------------------------
    // Checking
    // ------------------------------------------------------------------------------------------------

    /** Called once per app start (MainActivity): silent, only opens the sheet when something new is out. */
    fun autoCheckOnStartup(context: Context) {
        val app = context.applicationContext
        init(app)
        cleanupDownloads(app)
        if (!autoCheck || checking) return
        val now = System.currentTimeMillis()
        if (now - prefs(app).getLong(KEY_LAST_AUTO_CHECK, 0L) < AUTO_CHECK_MIN_GAP_MS) return
        prefs(app).edit().putLong(KEY_LAST_AUTO_CHECK, now).apply()
        runCheck(app, manual = false)
    }

    /** "Check for Updates Now": always answers (sheet, "latest version" or the error). */
    fun checkNow(context: Context) {
        val app = context.applicationContext
        init(app)
        if (checking) return
        runCheck(app, manual = true)
    }

    private fun runCheck(app: Context, manual: Boolean) {
        checking = true
        if (manual) statusMessage = null
        val usedChannel = channel
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { fetchLatest(app, usedChannel) } }
            checking = false
            if (usedChannel != channel) return@launch // channel switched meanwhile
            result.onSuccess { info ->
                if (info != null && isNewerThanInstalled(app, info)) {
                    available = info
                    errorMessage = null
                    if (downloadedFile?.name != info.assetName) downloadedFile = null
                    statusMessage = "Update ${info.versionName} is available"
                    if (manual || !wasDismissed(app, info)) sheetVisible = true
                } else {
                    available = null
                    if (manual) statusMessage = "You're on the latest version (v${installedVersionName(app)})"
                }
            }.onFailure { e ->
                if (manual) statusMessage = "Couldn't check for updates: ${friendly(e)}"
            }
        }
    }

    /** "Later" / swipe down: the sheet is not opened again by the start-up check for this same release. */
    fun dismissSheet(context: Context) {
        sheetVisible = false
        val info = available ?: return
        if (downloading) return
        prefs(context).edit()
            .putInt(KEY_DISMISSED_BUILD, info.buildNumber)
            .putString(KEY_DISMISSED_VERSION, info.versionName)
            .apply()
    }

    fun openSheet() {
        if (available != null) sheetVisible = true
    }

    private fun wasDismissed(context: Context, info: AppUpdateInfo): Boolean {
        val p = prefs(context)
        return p.getInt(KEY_DISMISSED_BUILD, -1) == info.buildNumber &&
            p.getString(KEY_DISMISSED_VERSION, null) == info.versionName
    }

    private fun fetchLatest(context: Context, channel: UpdateChannel): AppUpdateInfo? {
        val body = httpGet(context, "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases?per_page=30")
        val releases = JSONArray(body)
        var best: AppUpdateInfo? = null
        for (i in 0 until releases.length()) {
            val r = releases.optJSONObject(i) ?: continue
            if (r.optBoolean("draft")) continue
            val pre = r.optBoolean("prerelease")
            if (channel == UpdateChannel.STABLE && pre) continue
            val info = parseRelease(r, pre) ?: continue
            if (best == null || compareReleases(info, best) > 0) best = info
        }
        return best
    }

    // new: Viaura-v1.1beta3_arm64-v8a.apk / Viaura-v1.1_universal.apk   old: Viaura-v1.1-beta.3_arm64-v8a.apk, Viaura_1.0-beta.2_b2_arm64-v8a.apk
    private val assetNameRegex = Regex("""^Viaura[-_]v?(.+?)(?:_b(\d+))?_([A-Za-z0-9-]+)\.apk$""")
    private val betaBuildRegex = Regex("""(?i)beta[.-]?(\d+)""")
    private val tagBuildRegex = Regex("""(?i)^nightly[-._]?(\d+)""")
    private val buildMarkerRegex = Regex("""(?i)^viaura-build-(\d+)\.txt$""")

    private fun parseRelease(r: JSONObject, pre: Boolean): AppUpdateInfo? {
        val assets = r.optJSONArray("assets") ?: return null
        val apks = ArrayList<JSONObject>()
        var markerBuild: Int? = null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            buildMarkerRegex.matchEntire(a.optString("name"))?.let { markerBuild = it.groupValues[1].toIntOrNull() }
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) apks.add(a)
        }
        if (apks.isEmpty()) return null

        // the APK that fits this phone: its own ABI first (arm64-v8a ...), then the universal one
        fun abiOf(name: String): String? = assetNameRegex.matchEntire(name)?.groupValues?.get(3)
        val chosen: JSONObject = Build.SUPPORTED_ABIS.firstNotNullOfOrNull { abi ->
            apks.firstOrNull { abiOf(it.optString("name")).equals(abi, ignoreCase = true) }
        }
            ?: apks.firstOrNull { abiOf(it.optString("name")).equals("universal", ignoreCase = true) }
            ?: apks.firstOrNull { it.optString("name").contains("universal", ignoreCase = true) }
            ?: apks.first()
        val assetName = chosen.optString("name")
        val match = assetNameRegex.matchEntire(assetName)
        val tag = r.optString("tag_name")
        // a stable release (Viaura-v1.1_<abi>.apk) has no number in its name: it comes from the marker asset of the workflow
        val build = markerBuild
            ?: match?.groupValues?.get(2)?.toIntOrNull()
            ?: match?.groupValues?.get(1)?.let { betaBuildRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            ?: tagBuildRegex.find(tag)?.groupValues?.get(1)?.toIntOrNull()
            ?: 0
        val version = match?.groupValues?.get(1)
            ?: tag.removePrefix("v").removePrefix("V").ifBlank { r.optString("name") }
        val title = r.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: tag
        val notes = r.optString("body").takeIf { it != "null" }.orEmpty()
        return AppUpdateInfo(
            versionName = version,
            buildNumber = build,
            title = title,
            notes = notes,
            releaseDate = formatDate(r.optString("published_at")),
            sizeBytes = chosen.optLong("size", 0L),
            downloadUrl = chosen.optString("browser_download_url"),
            assetName = assetName,
            isPreview = pre,
            pageUrl = r.optString("html_url")
        )
    }

    private fun compareReleases(a: AppUpdateInfo, b: AppUpdateInfo): Int =
        if (a.buildNumber > 0 && b.buildNumber > 0) a.buildNumber.compareTo(b.buildNumber)
        else compareVersions(a.versionName, b.versionName)

    private fun isNewerThanInstalled(context: Context, info: AppUpdateInfo): Boolean =
        if (info.buildNumber > 0) info.buildNumber > BuildConfig.BUILD_NUMBER
        else compareVersions(info.versionName, installedVersionName(context)) > 0

    /** "1.10.2" vs "1.9" (digits only; a missing part counts as 0). */
    internal fun compareVersions(a: String, b: String): Int {
        fun nums(s: String) = Regex("""\d+""").findAll(s.split(Regex("""(?i)[-._]?beta"""))[0]).map { it.value.toLongOrNull() ?: 0L }.toList()
        val pa = nums(a)
        val pb = nums(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = (pa.getOrElse(i) { 0L }).compareTo(pb.getOrElse(i) { 0L })
            if (c != 0) return c
        }
        // same base version: the stable release is newer than any of its betas; betas compare by their number
        val betaA = betaBuildRegex.find(a)?.groupValues?.get(1)?.toLongOrNull()
        val betaB = betaBuildRegex.find(b)?.groupValues?.get(1)?.toLongOrNull()
        return when {
            betaA == null && betaB == null -> 0
            betaA == null -> 1
            betaB == null -> -1
            else -> betaA.compareTo(betaB)
        }
    }

    private fun formatDate(iso: String): String = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(parser.parse(iso)!!)
    } catch (_: Throwable) {
        iso.take(10)
    }

    private fun httpGet(context: Context, url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Viaura-Updater")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        ViauraHttpClient.forContext(context).newCall(request).execute().use { resp ->
            when {
                resp.code == 404 -> throw IOException("release repository not found (${BuildConfig.UPDATE_REPO} must be a public repository)")
                resp.code == 403 || resp.code == 429 -> throw IOException("GitHub rate limit reached, try again in a while")
                !resp.isSuccessful -> throw IOException("GitHub answered with HTTP ${resp.code}")
            }
            return resp.body?.string() ?: throw IOException("empty answer from GitHub")
        }
    }

    private fun friendly(e: Throwable): String = when (e) {
        is java.net.UnknownHostException, is java.net.SocketTimeoutException, is java.net.ConnectException -> "no internet connection"
        else -> e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
    }

    // ------------------------------------------------------------------------------------------------
    // Download + install
    // ------------------------------------------------------------------------------------------------

    /** "Download & Install": downloads the APK (progress is shown in the sheet), then opens Android's installer. */
    fun downloadAndInstall(context: Context) {
        val app = context.applicationContext
        val info = available ?: return
        if (downloading) return
        val ready = downloadedFile
        if (ready != null && ready.exists() && ready.name == info.assetName) {
            installFile(app, ready)
            return
        }
        errorMessage = null
        downloading = true
        downloadProgress = 0f
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { download(app, info) } }
            downloading = false
            downloadProgress = -1f
            result.onSuccess { file ->
                downloadedFile = file
                installFile(app, file)
            }.onFailure { e ->
                errorMessage = "Download failed: ${friendly(e)}"
            }
        }
    }

    private fun updatesDir(context: Context): File = File(context.cacheDir, "updates").apply { mkdirs() }

    private fun cleanupDownloads(context: Context) {
        try {
            File(context.cacheDir, "updates").listFiles()?.forEach { it.delete() }
        } catch (_: Throwable) {
        }
    }

    private fun download(context: Context, info: AppUpdateInfo): File {
        val dir = updatesDir(context)
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, info.assetName)
        val part = File(dir, info.assetName + ".part")
        val client = ViauraHttpClient.forContext(context).newBuilder()
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        val request = Request.Builder().url(info.downloadUrl).header("User-Agent", "Viaura-Updater").build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("server answered HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("empty download")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
            var done = 0L
            var lastReport = 0L
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        val now = System.nanoTime()
                        if (now - lastReport > 80_000_000L) {
                            lastReport = now
                            downloadProgress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else -1f
                        }
                    }
                }
            }
            if (total > 0 && done != total) {
                part.delete()
                throw IOException("the file is incomplete ($done of $total bytes)")
            }
        }
        if (!part.renameTo(target)) throw IOException("could not save the downloaded file")
        try {
            verifyApk(context, target)
        } catch (e: Throwable) {
            target.delete()
            throw e
        }
        return target
    }

    /** Catches a broken / foreign / unsigned-differently APK before the installer shows a vague error. */
    private fun verifyApk(context: Context, file: File) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("the downloaded file is not a valid APK")
        if (archive.packageName != context.packageName) throw IOException("the downloaded APK belongs to a different app")
        val installed = pm.getPackageInfo(context.packageName, flags)
        if (PackageInfoCompat.getLongVersionCode(archive) < PackageInfoCompat.getLongVersionCode(installed)) {
            throw IOException("the downloaded APK is older than the installed app")
        }
        val installedCerts = signerHashes(installed, history = true)
        val archiveCerts = signerHashes(archive, history = false)
        if (installedCerts.isNotEmpty() && archiveCerts.isNotEmpty() && installedCerts.intersect(archiveCerts).isEmpty()) {
            throw IOException(
                "this build is signed with a different key than the installed app, so Android cannot update over it. " +
                    "Uninstall Viaura once and install this APK (keep debug.keystore in the repository so later updates work)"
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun signerHashes(info: android.content.pm.PackageInfo, history: Boolean): Set<String> {
        val certs = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val si = info.signingInfo
                when {
                    si == null -> emptyArray<android.content.pm.Signature>()
                    history && !si.hasMultipleSigners() -> si.signingCertificateHistory ?: emptyArray<android.content.pm.Signature>()
                    else -> si.apkContentsSigners ?: emptyArray<android.content.pm.Signature>()
                }
            } else {
                info.signatures ?: emptyArray<android.content.pm.Signature>()
            }
        } catch (_: Throwable) {
            emptyArray<android.content.pm.Signature>()
        }
        return certs.map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    private fun installFile(context: Context, file: File) {
        if (!canInstallPackages(context)) {
            // the "Install unknown apps" switch of this app; the install continues from onAppResumed()
            waitingForInstallPermission = true
            errorMessage = "Allow \"Install unknown apps\" for Viaura, then come back - the update continues."
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Throwable) {
                errorMessage = "Open Settings > Apps > Viaura > Install unknown apps and allow it, then tap Install again."
            }
            return
        }
        waitingForInstallPermission = false
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            errorMessage = null
        } catch (e: Throwable) {
            errorMessage = "Couldn't open the installer: ${friendly(e)}"
        }
    }

    /** MainActivity / the sheet calls this when the app comes back to the front (after the permission screen). */
    fun onAppResumed(context: Context) {
        val file = downloadedFile ?: return
        if (waitingForInstallPermission && canInstallPackages(context) && file.exists()) {
            installFile(context.applicationContext, file)
        }
    }
}
