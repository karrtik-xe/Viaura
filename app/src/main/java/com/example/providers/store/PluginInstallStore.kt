package com.example.util.media

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.download.HttpFetch
import com.lagradost.viaura.CloudStreamEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Providers of the Network > Media section. A repository link (Vega manifest.json, or CloudStream repo.json / plugins.json /
 * GitHub project / cloudstreamrepo:// / short code) is loaded into a plugin list, and every plugin of that list can be
 * installed (updated, removed) locally; installed providers can be opened and played.
 */
object MediaPluginStore {
    private const val PREFS = "media_plugin_prefs"
    private const val KEY_REPO = "repo_url"
    private const val KEY_BASE = "repo_base"
    private const val KEY_MANIFEST = "manifest_json"
    private const val CS_FILE = "plugin.cs3"

    /** Bumped whenever the retained locally installed plugin list changes (collect it to refresh the UI). */
    val changes = MutableStateFlow(0)

    /** Plugins that are being downloaded / installed right now (by [MediaPluginInfo.value]). */
    val busy = MutableStateFlow<Set<String>>(emptySet())

    private val installScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val installGate = Semaphore(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun root(context: Context): File = File(context.applicationContext.filesDir, "media_plugins").apply { mkdirs() }

    private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private fun dirFor(context: Context, value: String) = File(root(context), safeName(value))

    fun getRepoUrl(context: Context): String = prefs(context).getString(KEY_REPO, "").orEmpty()

    /** Base URL (raw GitHub / jsDelivr) of the last repository that loaded successfully, or null. */
    fun repoBase(context: Context): String? = prefs(context).getString(KEY_BASE, null)

    fun getManifest(context: Context): List<MediaPluginInfo> {
        val text = prefs(context).getString(KEY_MANIFEST, null) ?: return emptyList()
        return try { parseManifest(text) } catch (_: Throwable) { emptyList() }
    }

    fun getInstalled(context: Context): List<MediaPluginInfo> {
        val dirs = root(context).listFiles { f -> f.isDirectory && !f.name.startsWith(".") } ?: return emptyList()
        val out = ArrayList<MediaPluginInfo>()
        for (dir in dirs) {
            val meta = File(dir, "meta.json")
            val isCs = File(dir, CS_FILE).isFile
            if (!meta.isFile) continue
            if (!isCs && (!File(dir, "catalog.js").isFile || !File(dir, "posts.js").isFile)) continue
            try {
                val o = JSONObject(meta.readText())
                out.add(
                    MediaPluginInfo(
                        value = o.getString("value"),
                        name = o.optString("name", dir.name).let { n -> if (isCs && n == o.getString("value")) cleanName(null, n) else n },
                        version = o.optString("version", ""),
                        icon = cleanIconUrl(o.optString("icon", "")),
                        type = o.optString("type", ""),
                        installedAt = o.optLong("installedAt", 0L),
                        engine = if (isCs) MediaPluginInfo.ENGINE_CLOUDSTREAM else MediaPluginInfo.ENGINE_VEGA,
                        url = o.optString("url", ""),
                        fileHash = o.optString("hash", ""),
                        language = o.optString("language", ""),
                        description = o.optString("description", ""),
                        size = o.optLong("size", 0L)
                    )
                )
            } catch (_: Throwable) {
            }
        }
        return out.sortedBy { it.installedAt }
    }

    fun readModule(context: Context, value: String, module: String): String? {
        val f = File(dirFor(context, value), "$module.js")
        return if (f.isFile) f.readText() else null
    }

    /** True when the installed plugin [value] is a CloudStream extension (.cs3), false for a Vega JavaScript plugin. */
    fun isCloudStream(context: Context, value: String): Boolean = File(dirFor(context, value), CS_FILE).isFile

    /** Loads the installed CloudStream extension [value] into the process (once per app start). */
    suspend fun ensureCloudStreamLoaded(context: Context, value: String) {
        if (!com.example.BuildConfig.ENABLE_CLOUDSTREAM) throw IllegalStateException("CloudStream extensions are turned off in this build")
        if (CloudStreamEngine.isLoaded(value)) return
        val file = File(dirFor(context, value), CS_FILE)
        if (!file.isFile) throw IllegalStateException("This provider is not installed")
        CloudStreamEngine.load(context, value, file, force = false)
    }

    fun remove(context: Context, value: String) {
        if (com.example.BuildConfig.ENABLE_CLOUDSTREAM) CloudStreamEngine.unload(value)
        dirFor(context, value).deleteRecursively()
        PluginJsEngine.invalidate(value)
        MediaProviderRepository.clear(value)
        changes.value++
    }

    // ------------------------------------------------------------------------------------------------------------
    // Repository loading
    // ------------------------------------------------------------------------------------------------------------

    private class Fetched(val body: String, val finalUrl: String)

    private class Loaded(val plugins: List<MediaPluginInfo>, val base: String?)

    private fun fetchText(context: Context, url: String): Fetched? = try {
        HttpFetch.call(context, HttpFetch.request(url, emptyMap()), null) { r ->
            if (r.code !in 200..299) null else Fetched(r.body?.string().orEmpty(), r.request.url.toString())
        }
    } catch (_: Throwable) {
        null
    }

    private fun fetchBytes(context: Context, url: String): ByteArray {
        val bytes = HttpFetch.call(context, HttpFetch.request(url, emptyMap()), null) { r ->
            if (r.code !in 200..299) throw IOException("HTTP ${r.code}")
            r.body?.bytes()
        }
        if (bytes == null || bytes.isEmpty()) throw IOException("Empty file")
        return bytes
    }

    /** Every address the text typed into the repository field can stand for, best guess first. */
    internal fun candidateUrls(raw: String): List<String> {
        var u = raw.trim()
        if (u.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()
        val csPrefix = Regex("^(cloudstreamrepo://|https://cs\\.repo/\\??)", RegexOption.IGNORE_CASE)
        if (csPrefix.containsMatchIn(u)) {
            u = u.replace(csPrefix, "")
            if (!u.startsWith("http://", true) && !u.startsWith("https://", true)) u = "https://$u"
        }
        if (u.startsWith("http://", true) || u.startsWith("https://", true)) {
            val gh = Regex("^https?://github\\.com/([^/\\s]+)/([^/\\s#?]+?)(?:\\.git)?/?$", RegexOption.IGNORE_CASE).find(u)
            if (gh != null) {
                val user = gh.groupValues[1]
                val repo = gh.groupValues[2]
                for (branch in listOf("master", "main", "builds")) out.add("https://raw.githubusercontent.com/$user/$repo/$branch/repo.json")
                for (branch in listOf("main", "master")) {
                    out.add("https://cdn.jsdelivr.net/gh/$user/$repo@$branch/manifest.json")
                    out.add("https://raw.githubusercontent.com/$user/$repo/$branch/manifest.json")
                }
            } else {
                out.add(u)
                if (!u.substringBefore('?').lowercase().endsWith(".json")) {
                    val trimmed = u.trimEnd('/')
                    out.add("$trimmed/repo.json")
                    out.add("$trimmed/manifest.json")
                }
            }
        } else {
            // a short code such as "874": the shortener services redirect it to the repository file
            val code = u.trim('/').replace(" ", "")
            out.add("https://l.cloudstream.cf/$code")
            out.add("https://cutt.ly/$code")
            out.add("https://py.md/$code")
        }
        return out.toList()
    }

    private fun resolveAgainst(base: String, value: String): String {
        val v = value.trim()
        return when {
            v.isEmpty() -> ""
            v.startsWith("http://", true) || v.startsWith("https://", true) || v.startsWith("//") -> v
            v.startsWith("/") -> base.trimEnd('/') + v
            else -> base.trimEnd('/') + "/" + v
        }
    }

    /** A Vega manifest.json entry or a CloudStream plugins.json entry -> [MediaPluginInfo]; null for entries that cannot be used. */
    private fun parsePluginEntry(o: JSONObject, base: String): MediaPluginInfo? {
        if (o.optBoolean("disabled", false)) return null
        val vegaValue = o.optString("value", "")
        if (vegaValue.isNotBlank()) {
            return MediaPluginInfo(
                value = vegaValue,
                name = o.optString("display_name", "").ifBlank { vegaValue },
                version = o.versionText(),
                icon = cleanIconUrl(resolveAgainst(base, o.optString("icon", ""))),
                type = o.optString("type", ""),
                engine = MediaPluginInfo.ENGINE_VEGA
            )
        }
        val fileUrl = o.optString("url", "")
        val internal = o.optString("internalName", "").ifBlank { o.optString("name", "") }
        if (fileUrl.isBlank() || internal.isBlank()) return null
        if (o.optInt("status", 1) == 0) return null // the repository marks this extension as down
        val types = o.optJSONArray("tvTypes")
        val typeText = if (types != null) (0 until types.length()).joinToString(", ") { types.optString(it) } else ""
        return MediaPluginInfo(
            value = internal,
            name = cleanName(o.optString("name", ""), internal),
            version = o.versionText(),
            icon = cleanIconUrl(o.optString("iconUrl", o.optString("icon", ""))),
            type = typeText,
            engine = MediaPluginInfo.ENGINE_CLOUDSTREAM,
            url = fileUrl,
            language = o.optString("language", "").takeIf { !it.equals("null", true) }.orEmpty(),
            description = o.optString("description", "").takeIf { !it.equals("null", true) }.orEmpty(),
            size = o.optLong("fileSize", 0L)
        )
    }

    private fun JSONObject.versionText(): String = opt("version")?.toString()?.takeIf { it != "null" }.orEmpty()

    private fun parsePluginArray(arr: JSONArray, base: String): List<MediaPluginInfo> {
        val out = ArrayList<MediaPluginInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            parsePluginEntry(o, base)?.let { out.add(it) }
        }
        return out
    }

    private fun parseRepository(context: Context, text: String, finalUrl: String): Loaded? {
        val t = text.trim().trimStart('﻿')
        val base = finalUrl.substringBefore('?').substringBeforeLast('/')
        if (t.startsWith("[")) return Loaded(parsePluginArray(JSONArray(t), base), base)
        if (!t.startsWith("{")) return null
        val o = JSONObject(t)
        val lists = o.optJSONArray("pluginLists")
        if (lists != null) {
            // CloudStream repo.json: it only names the plugins.json file(s)
            val all = ArrayList<MediaPluginInfo>()
            for (i in 0 until lists.length()) {
                val listUrl = resolveAgainst(base, lists.optString(i, ""))
                if (listUrl.isBlank()) continue
                val got = fetchText(context, listUrl) ?: continue
                val body = got.body.trim().trimStart('﻿')
                if (!body.startsWith("[")) continue
                all.addAll(parsePluginArray(JSONArray(body), got.finalUrl.substringBefore('?').substringBeforeLast('/')))
            }
            return Loaded(all, null)
        }
        val plugins = o.optJSONArray("plugins")
        if (plugins != null) return Loaded(parsePluginArray(plugins, base), base)
        return null
    }

    private fun manifestJson(list: List<MediaPluginInfo>): String {
        val arr = JSONArray()
        for (p in list) {
            arr.put(
                JSONObject()
                    .put("value", p.value).put("display_name", p.name).put("version", p.version).put("icon", p.icon)
                    .put("type", p.type).put("engine", p.engine).put("url", p.url).put("hash", p.fileHash)
                    .put("language", p.language).put("description", p.description).put("size", p.size)
            )
        }
        return arr.toString()
    }

    // ------------------------------------------------------------------------------------------------------------
    // "viaurax": typing this word (any capitalisation) instead of a link lists only the hand-picked providers below
    // ------------------------------------------------------------------------------------------------------------

    private const val VIAURAX_KEYWORD = "viaurax"
    private const val VIAURAX_VEGA_REPO = "https://github.com/Zenda-Cross/vega-providers.git"
    private const val VIAURAX_CLOUDSTREAM_CODE = "864"
    private val viauraxVegaNames = listOf("HdHub4u", "Kartoons", "MX Player", "VMovies", "MultiStream", "4khdHub", "UHDMovies", "MovieBox Web")
    private val viauraxCloudStreamNames = listOf("Toonstream", "MovieBox", "Animedubhindi")

    private fun normalizeName(v: String): String = v.lowercase().filter { it.isLetterOrDigit() }

    /** Fetches the plugins of one repository [input] (link or code); null when it cannot be reached or has none. */
    private fun fetchRepositoryPlugins(app: Context, input: String): Loaded? {
        for (candidate in candidateUrls(input)) {
            val got = fetchText(app, candidate) ?: continue
            val loaded = try {
                parseRepository(app, got.body, got.finalUrl)
            } catch (_: Throwable) {
                null
            }
            if (loaded != null && loaded.plugins.isNotEmpty()) return loaded
        }
        return null
    }

    private fun pickByName(plugins: List<MediaPluginInfo>, wanted: List<String>): List<MediaPluginInfo> {
        val targets = wanted.map { normalizeName(it) }
        val out = ArrayList<MediaPluginInfo>()
        // keep the order of [wanted]; a provider matches by display name or id
        for (t in targets) {
            val hit = plugins.firstOrNull { normalizeName(it.name) == t || normalizeName(it.value) == t }
                ?: plugins.firstOrNull { normalizeName(it.name).contains(t) && normalizeName(it.name).length <= t.length + 4 }
            if (hit != null && out.none { it.value == hit.value }) out.add(hit)
        }
        return out
    }

    private fun loadViauraxSet(app: Context, input: String): Int {
        val vega = fetchRepositoryPlugins(app, VIAURAX_VEGA_REPO)
        val cloud = fetchRepositoryPlugins(app, VIAURAX_CLOUDSTREAM_CODE)
        if (vega == null && cloud == null) throw IOException("Could not reach the providers. Check your connection")
        val picked = ArrayList<MediaPluginInfo>()
        if (vega != null) picked.addAll(pickByName(vega.plugins, viauraxVegaNames))
        if (cloud != null) {
            for (p in pickByName(cloud.plugins, viauraxCloudStreamNames)) if (picked.none { it.value == p.value }) picked.add(p)
        }
        if (picked.isEmpty()) throw IOException("No providers found")
        val ed = prefs(app).edit()
            .putString(KEY_REPO, input.trim())
            .putString(KEY_MANIFEST, manifestJson(picked))
        // Vega plugins download from the Vega repository's own address
        if (vega?.base != null) ed.putString(KEY_BASE, vega.base) else ed.remove(KEY_BASE)
        ed.apply()
        changes.value++
        return picked.size
    }

    /**
     * Loads the repository [input] (link or short code) and keeps its plugin list. Returns how many plugins it offers.
     * Throws an [IOException] with a readable message when nothing usable is found.
     */
    suspend fun loadRepository(context: Context, input: String): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        if (input.trim().equals(VIAURAX_KEYWORD, ignoreCase = true)) return@withContext loadViauraxSet(app, input)
        val candidates = candidateUrls(input)
        if (candidates.isEmpty()) throw IOException("Enter a repository link or code")
        var reached = false
        for (candidate in candidates) {
            val got = fetchText(app, candidate) ?: continue
            reached = true
            val loaded = try {
                parseRepository(app, got.body, got.finalUrl)
            } catch (_: Throwable) {
                null
            }
            if (loaded == null || loaded.plugins.isEmpty()) continue
            val unique = LinkedHashMap<String, MediaPluginInfo>()
            for (p in loaded.plugins) unique[p.value] = p
            val ed = prefs(app).edit()
                .putString(KEY_REPO, input.trim())
                .putString(KEY_MANIFEST, manifestJson(unique.values.toList()))
            if (loaded.base != null) ed.putString(KEY_BASE, loaded.base) else ed.remove(KEY_BASE)
            ed.apply()
            changes.value++
            return@withContext unique.size
        }
        throw IOException(if (reached) "No plugins found at this link" else "Could not reach this link. Check the link or your connection")
    }

    // ------------------------------------------------------------------------------------------------------------
    // Install / update
    // ------------------------------------------------------------------------------------------------------------

    private val vegaModules = listOf("catalog", "posts", "meta", "stream", "episodes")
    private val vegaRequired = setOf("catalog", "posts")

    private fun installBlocking(context: Context, plugin: MediaPluginInfo) {
        val files = LinkedHashMap<String, ByteArray>()
        if (plugin.isCloudStream) {
            if (plugin.url.isBlank()) throw IOException("This extension has no download link")
            val bytes = fetchBytes(context, plugin.url)
            if (bytes.size < 4 || bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) throw IOException("The downloaded file is not a CloudStream extension")
            files[CS_FILE] = bytes
        } else {
            val base = repoBase(context) ?: throw IOException("Load the repository again")
            for (m in vegaModules) {
                val file = "$m.js"
                try {
                    val bytes = fetchBytes(context, "${base.trimEnd('/')}/dist/${plugin.value}/$file")
                    if (bytes[0] == '<'.code.toByte()) throw IOException("Not a script")
                    files[file] = bytes
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    if (m in vegaRequired) throw IOException("Could not download ${plugin.name}: $file")
                }
            }
        }
        val meta = JSONObject()
            .put("value", plugin.value).put("name", plugin.name).put("version", plugin.version).put("icon", plugin.icon)
            .put("type", plugin.type).put("installedAt", System.currentTimeMillis()).put("url", plugin.url).put("hash", plugin.fileHash)
            .put("language", plugin.language).put("description", plugin.description).put("size", plugin.size)

        // everything is downloaded: only now the old copy is replaced (a failed update leaves the working one in place)
        val tmp = File(root(context), ".tmp_" + safeName(plugin.value))
        tmp.deleteRecursively()
        tmp.mkdirs()
        for ((name, bytes) in files) File(tmp, name).writeBytes(bytes)
        File(tmp, "meta.json").writeText(meta.toString())

        if (com.example.BuildConfig.ENABLE_CLOUDSTREAM && plugin.isCloudStream) CloudStreamEngine.unload(plugin.value)
        PluginJsEngine.invalidate(plugin.value)
        val dest = dirFor(context, plugin.value)
        dest.deleteRecursively()
        if (!tmp.renameTo(dest)) {
            tmp.copyRecursively(dest, true)
            tmp.deleteRecursively()
        }
        MediaProviderRepository.clear(plugin.value)
        changes.value++
    }

    /** Downloads and installs (or updates) [plugin] in the background; [onDone] runs on the main thread (null = success). */
    fun installAsync(context: Context, plugin: MediaPluginInfo, onDone: (Throwable?) -> Unit = {}) {
        val app = context.applicationContext
        if (plugin.value in busy.value) return
        busy.update { it + plugin.value }
        installScope.launch {
            val err: Throwable? = try {
                installGate.withPermit { installBlocking(app, plugin) }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                t
            } finally {
                busy.update { it - plugin.value }
            }
            mainHandler.post { onDone(err) }
        }
    }

    /** Installs / updates every plugin of [plugins] (three at a time). [onDone] gets the number of successes and failures. */
    fun installAllAsync(context: Context, plugins: List<MediaPluginInfo>, onDone: (ok: Int, failed: Int) -> Unit = { _, _ -> }) {
        val app = context.applicationContext
        val todo = plugins.filter { it.value !in busy.value }
        if (todo.isEmpty()) {
            onDone(0, 0)
            return
        }
        busy.update { it + todo.map { p -> p.value } }
        installScope.launch {
            val results = todo.map { p ->
                async {
                    try {
                        installGate.withPermit { installBlocking(app, p) }
                        true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        false
                    } finally {
                        busy.update { it - p.value }
                    }
                }
            }.awaitAll()
            mainHandler.post { onDone(results.count { it }, results.count { !it }) }
        }
    }

    /**
     * A repository's icon link made loadable: CloudStream's "%size%" placeholder is filled in, "//host/..." gets a scheme and
     * http links are upgraded to https (the image loader does not follow an http -> https redirect, so those logos stayed empty).
     */
    internal fun cleanIconUrl(raw: String?): String {
        var u = raw?.trim().orEmpty()
        if (u.isEmpty() || u.equals("null", true)) return ""
        u = u.replace("%size%", "128").replace("{size}", "128").replace("\$size", "128")
        if (u.startsWith("//")) u = "https:$u"
        if (u.startsWith("http://", true)) u = "https://" + u.substring(7)
        return u
    }

    /** The name a repository lists, unchanged. Only when it lists none, the internal class name is shown without its "Provider" suffix. */
    internal fun cleanName(name: String?, internal: String): String {
        val n = name?.trim().orEmpty()
        if (n.isNotEmpty() && !n.equals("null", true)) return n
        val stripped = internal.removeSuffix("Provider").removeSuffix("provider").trim()
        return stripped.ifBlank { internal }
    }

    private fun parseManifest(text: String): List<MediaPluginInfo> {
        val arr = JSONArray(text)
        val out = ArrayList<MediaPluginInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optBoolean("disabled", false)) continue
            val value = o.optString("value", "")
            if (value.isBlank()) continue
            out.add(
                MediaPluginInfo(
                    value = value,
                    name = o.optString("display_name", value).let { dn ->
                        if (o.optString("engine", "") == MediaPluginInfo.ENGINE_CLOUDSTREAM && dn == value) cleanName(null, value) else dn
                    },
                    version = o.optString("version", ""),
                    icon = cleanIconUrl(o.optString("icon", "")),
                    type = o.optString("type", ""),
                    engine = if (o.optString("engine", "") == MediaPluginInfo.ENGINE_CLOUDSTREAM) MediaPluginInfo.ENGINE_CLOUDSTREAM else MediaPluginInfo.ENGINE_VEGA,
                    url = o.optString("url", ""),
                    fileHash = o.optString("hash", ""),
                    language = o.optString("language", ""),
                    description = o.optString("description", ""),
                    size = o.optLong("size", 0L)
                )
            )
        }
        return out
    }

}
