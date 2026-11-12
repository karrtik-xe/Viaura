package com.example.player

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

object SubtitleFontManager {

    private const val TAG = "SubtitleFontManager"
    private const val FONTS_DIR_NAME = "sub_fonts"
    private const val FONT_REGISTRY_PREF = "viaura_subtitle_font_registry"
    private const val USER_UPLOADED_FONTS_KEY = "user_uploaded"
    private const val VIDEO_EXTRACTED_FONTS_KEY = "video_extracted"

    // Multi-script fallback: the phone's OWN system fonts (Roboto + the Noto script fonts listed in
    // /system/etc/fonts.xml). Nothing is bundled in the APK and nothing is downloaded, so subtitles
    // never show "tofu" boxes offline either. Legacy names of the old bundled Go Noto font are kept
    // only to clean up leftovers from earlier builds.
    private const val BUNDLED_FALLBACK_FONT_FILE = "GoNotoCurrent-Regular.ttf"
    private const val LEGACY_FALLBACK_FONT_FAMILY = "Go Noto Current-Regular"

    /** Family name of the device's default sans-serif font (filled in once the system fonts are linked). */
    @Volatile private var systemDefaultFamily: String = "Roboto"
    val BUNDLED_FALLBACK_FONT_FAMILY: String get() = systemDefaultFamily

    // Set of known font names embedded in or attached to the currently playing video (MKV attachments, etc.)
    private val videoEmbeddedFontNames = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    // Fonts that the CURRENT video / its subtitle really ships (MKV attachments, [Fonts] section of
    // the ASS, fonts next to the video). Unlike videoEmbeddedFontNames this does NOT include every
    // file that merely sits in sub_fonts (user uploads, fonts left over from other videos).
    private val suppliedFontNames = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun addFontNameVariants(target: MutableSet<String>, fontName: String) {
        if (fontName.isBlank()) return
        val clean = fontName.trim()
        target.add(clean.lowercase(java.util.Locale.ROOT))
        val withoutExt = clean.substringBeforeLast('.').trim()
        if (withoutExt.isNotBlank()) {
            target.add(withoutExt.lowercase(java.util.Locale.ROOT))
        }
        val normalized = normalizeFontName(clean)
        if (normalized.isNotBlank()) {
            target.add(normalized)
        }
        val normWithoutExt = normalizeFontName(withoutExt)
        if (normWithoutExt.isNotBlank()) {
            target.add(normWithoutExt)
        }
    }

    /** A font shipped by the current video/subtitle (counts as "attached"). */
    fun registerVideoEmbeddedFont(fontName: String) {
        addFontNameVariants(videoEmbeddedFontNames, fontName)
        addFontNameVariants(suppliedFontNames, fontName)
    }

    /** A font that is merely present on disk (not proof that this video ships it). */
    private fun registerLocalFontName(fontName: String) {
        addFontNameVariants(videoEmbeddedFontNames, fontName)
    }

    /**
     * True only when the current video / subtitle itself supplies this font (attachment, [Fonts]
     * block, font file next to the video). Then the subtitle's own font must be kept as-is.
     */
    fun isSuppliedByCurrentVideo(context: Context, fontName: String): Boolean {
        val target = normalizeFontName(fontName.trim().removePrefix("@"))
        if (target.isBlank() || suppliedFontNames.isEmpty()) return false
        if (target in suppliedFontNames) return true
        val files = getSubFontsDir(context).listFiles() ?: return false
        for (f in files) {
            if (!f.isFile) continue
            val base = f.nameWithoutExtension
            val isSupplied = f.name.lowercase(java.util.Locale.ROOT) in suppliedFontNames ||
                base.lowercase(java.util.Locale.ROOT) in suppliedFontNames ||
                normalizeFontName(f.name) in suppliedFontNames ||
                normalizeFontName(base) in suppliedFontNames
            if (!isSupplied) continue
            if (namesOfFontFile(f).any { normalizeFontName(it) == target }) return true
        }
        return false
    }

    private fun registerVideoFolderFontNames(videoPath: String) {
        try {
            val parentDir = File(videoPath).parentFile ?: return
            val dirs = mutableListOf(parentDir)
            for (sub in listOf("fonts", "Fonts", "font", "Font", "FONTS")) {
                val f = File(parentDir, sub)
                if (f.isDirectory) dirs.add(f)
            }
            for (dir in dirs) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension.lowercase(java.util.Locale.ROOT) in FONT_EXTS) {
                        registerVideoEmbeddedFont(file.name)
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun registerAssScriptFontNames(assContent: String) {
        try {
            var inFonts = false
            for (line in assContent.lineSequence()) {
                val t = line.trim()
                if (t.startsWith("[Fonts]", ignoreCase = true)) { inFonts = true; continue }
                if (inFonts && t.startsWith("[")) break
                if (inFonts && t.startsWith("fontname:", ignoreCase = true)) {
                    registerVideoEmbeddedFont(t.substringAfter(":").trim())
                }
            }
        } catch (_: Throwable) {}
    }

    fun clearVideoEmbeddedFonts() {
        videoEmbeddedFontNames.clear()
        suppliedFontNames.clear()
    }

    private fun normalizeFontName(name: String): String {
        return name.lowercase(java.util.Locale.ROOT).replace(Regex("[\\s_\\-]+"), "")
    }

    fun getSubFontsDir(context: Context): File {
        val dir = File(context.cacheDir, FONTS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun fontRegistryPrefs(context: Context): android.content.SharedPreferences =
        context.getSharedPreferences(FONT_REGISTRY_PREF, Context.MODE_PRIVATE)

    private fun registrySet(context: Context, key: String): MutableSet<String> =
        fontRegistryPrefs(context).getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun updateRegistry(context: Context, key: String, values: Set<String>) {
        fontRegistryPrefs(context).edit().putStringSet(key, values).apply()
    }

    private fun markUserUploadedFont(context: Context, fileName: String) {
        if (fileName.isBlank()) return
        val user = registrySet(context, USER_UPLOADED_FONTS_KEY)
        user.add(fileName)
        updateRegistry(context, USER_UPLOADED_FONTS_KEY, user)
        val video = registrySet(context, VIDEO_EXTRACTED_FONTS_KEY)
        if (video.remove(fileName)) updateRegistry(context, VIDEO_EXTRACTED_FONTS_KEY, video)
    }

    private fun markVideoExtractedFont(context: Context, fileName: String) {
        if (fileName.isBlank()) return
        val user = registrySet(context, USER_UPLOADED_FONTS_KEY)
        if (fileName in user) return
        val video = registrySet(context, VIDEO_EXTRACTED_FONTS_KEY)
        video.add(fileName)
        updateRegistry(context, VIDEO_EXTRACTED_FONTS_KEY, video)
    }

    private fun unmarkFont(context: Context, fileName: String) {
        val user = registrySet(context, USER_UPLOADED_FONTS_KEY)
        val video = registrySet(context, VIDEO_EXTRACTED_FONTS_KEY)
        val userChanged = user.remove(fileName)
        val videoChanged = video.remove(fileName)
        if (userChanged) updateRegistry(context, USER_UPLOADED_FONTS_KEY, user)
        if (videoChanged) updateRegistry(context, VIDEO_EXTRACTED_FONTS_KEY, video)
    }

    fun isUserUploadedFont(context: Context, file: File): Boolean =
        file.name in registrySet(context, USER_UPLOADED_FONTS_KEY)

    fun isVideoExtractedFont(context: Context, file: File): Boolean =
        file.name in registrySet(context, VIDEO_EXTRACTED_FONTS_KEY)

    private var assetsFontsSynced = false
    private var legacyGeneratedFontsCleaned = false
    private val processedVideoFontPaths = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Synchronously prepares base fallback fonts directory.
     * Note: Does NOT auto-load asset or device fonts on app start.
     */
    fun initializeFontEnvironment(context: Context): File {
        val fontsDir = getSubFontsDir(context)
        try {
            // The subtitle fallback font is a build-time asset (GoNotoCurrent-Regular.ttf).
            // Make it available before the first subtitle is selected.
            if (!assetsFontsSynced) {
                copyAssetFonts(context, fontsDir)
                removeStaleBundledFont(context, fontsDir)
                assetsFontsSynced = true
            }
            prepareSystemFallbackFonts(context, fontsDir)
            ensureDefaultFontAliases(fontsDir)
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to initialize subtitle fallback fonts", t)
        }
        return fontsDir
    }

    /**
     * Synchronizes and populates the MPV/libass fonts directory from:
     * 1. System fonts (/system/fonts, etc.) & creates fallback aliases (default.ttf, Arial.ttf, sans-serif.ttf)
     * 2. App Assets (assets/fonts and assets/)
     * 3. Video file folder & subfolders (fonts/, Fonts/)
     * 4. Embedded ASS script [Fonts] attachments
     * 5. Custom font files picked by user
     */
    fun syncFonts(
        context: Context,
        videoPath: String? = null,
        assContent: String? = null,
        customFontFiles: List<File> = emptyList()
    ): File {
        val fontsDir = getSubFontsDir(context)

        // 1. Asset fallback font. It must always be available to libass, even before
        // a video supplies any font attachments.
        try {
            if (!assetsFontsSynced) {
                copyAssetFonts(context, fontsDir)
                removeStaleBundledFont(context, fontsDir)
                assetsFontsSynced = true
            }
            prepareSystemFallbackFonts(context, fontsDir)
            ensureDefaultFontAliases(fontsDir)
        } catch (t: Throwable) {
            Log.e(TAG, "Error in synchronous font setup", t)
        }

        // 2. Synchronously extract & sync fonts for immediate ASS/SSA rendering
        try {
            if (!legacyGeneratedFontsCleaned) {
                cleanupLegacyGeneratedFonts(fontsDir)
                legacyGeneratedFontsCleaned = true
            }

            // Extract Embedded ASS Script Fonts if ASS script contains [Fonts] section
            if (!assContent.isNullOrBlank() && assContent.contains("[Fonts]", ignoreCase = true)) {
                extractEmbeddedAssFonts(assContent, fontsDir, context)
            }

            if (!assContent.isNullOrBlank() && assContent.contains("[Fonts]", ignoreCase = true)) {
                registerAssScriptFontNames(assContent)
            }
            if (!videoPath.isNullOrBlank()) {
                registerVideoFolderFontNames(videoPath)
                val shouldProcessPath = processedVideoFontPaths.add(videoPath)
                if (shouldProcessPath) {
                    // Copy Video / Subtitle Folder Fonts if any
                    copyVideoFolderFonts(videoPath, fontsDir)

                    // Extract Embedded MKV Video Fonts & attachments if playing MKV container
                    extractEmbeddedMkvFonts(videoPath, fontsDir, context)

                    // Embedded ASS/SSA tracks are rendered by libass itself: find the fonts they
                    // use and fetch the missing ones from Google Fonts (background, never blocks).
                    try { GoogleFontsManager.ensureForMkv(context, videoPath, fontsDir) } catch (_: Throwable) {}
                }
            }

            // Copy Custom Picked Font Files
            for (file in customFontFiles) {
                if (file.exists() && file.length() > 0) {
                    try {
                        file.copyTo(File(fontsDir, file.name), overwrite = true)
                        registerLocalFontName(file.name)
                        registerLocalFontName(file.nameWithoutExtension)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to copy custom font: ${file.name}", e)
                    }
                }
            }

            // Google Fonts: fonts this ASS/SSA script uses that nobody provides are installed from
            // the offline cache right now and downloaded in the background when online.
            if (!assContent.isNullOrBlank() &&
                (assContent.contains("[V4+ Styles]", ignoreCase = true) ||
                    assContent.contains("[V4 Styles]", ignoreCase = true))
            ) {
                // Phone fonts first (accurate: the very same font), Google Fonts only for what is missing.
                try { linkRequestedPhoneFonts(context, assContent) } catch (_: Throwable) {}
                try { GoogleFontsManager.ensureForScript(context, assContent, fontsDir) } catch (_: Throwable) {}
            }

            // Register all font files present in fontsDir so style detection knows they are available
            fontsDir.listFiles()?.forEach { fontFile ->
                if (fontFile.isFile && !fontFile.name.startsWith(SYS_LINK_PREFIX)) {
                    registerLocalFontName(fontFile.name)
                    registerLocalFontName(fontFile.nameWithoutExtension)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error in synchronous syncFonts", t)
        }

        return fontsDir
    }

    /**
     * Ensures fallback aliases (default.ttf, sans-serif.ttf, Arial.ttf, Roboto.ttf) exist in sub_fonts.
     * This prevents libass from dropping text glyphs when an unknown font is requested.
     */
    private fun ensureDefaultFontAliases(destDir: File) {
        // No duplicate alias files: the single GoNoto fallback is enough.
    }

    // ---------------------------------------------------------------------------------------
    // User-selected subtitle font + per-glyph fallback to the bundled Go Noto font
    // ---------------------------------------------------------------------------------------

    /** Render name (as libass should see it) of the font the user selected, or null for none. */
    @Volatile
    var selectedFontRenderName: String? = null
        private set

    @Volatile private var selectedFontFileCache: File? = null
    @Volatile private var selectedFontFileCacheName: String? = null

    /** Remembers the user's selected font. Cheap; the file itself is resolved lazily. */
    fun setSelectedFont(renderName: String?) {
        val clean = renderName?.trim()?.takeIf {
            it.isNotBlank() && !it.equals(BUNDLED_FALLBACK_FONT_FAMILY, ignoreCase = true)
        }
        if (clean == selectedFontRenderName) return
        selectedFontRenderName = clean
        selectedFontFileCache = null
        selectedFontFileCacheName = null
    }

    private val fontNamesCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    private fun namesOfFontFile(file: File): List<String> {
        val key = file.absolutePath + "|" + file.length() + "|" + file.lastModified()
        return fontNamesCache.getOrPut(key) {
            val names = mutableListOf(file.name, file.nameWithoutExtension)
            names.addAll(extractFontNamesFromTtfFile(file))
            names
        }
    }

    private val FONT_EXTS = setOf("ttf", "otf", "ttc", "woff", "woff2")

    /** Finds a font file inside sub_fonts by file name, or by a name declared inside the font. */
    fun findFontFile(context: Context, fontName: String): File? {
        val clean = fontName.trim().removePrefix("@")
        if (clean.isBlank()) return null
        val norm = normalizeFontName(clean)
        val files = getSubFontsDir(context).listFiles()
            ?.filter {
                it.isFile && !it.name.startsWith(SYS_LINK_PREFIX) &&
                    it.extension.lowercase(java.util.Locale.ROOT) in FONT_EXTS
            }
            ?: return null
        // 1) The font's own Full Font Name (nameID 4) / file name is unique per file. Family names
        //    (nameID 1) and PostScript names are NOT: custom font packs often give every file the
        //    same family (e.g. "XE Fonts"), so they must never win over a full-name match.
        files.firstOrNull { f -> uniqueNamesOfFontFile(f).any { it.equals(clean, ignoreCase = true) } }?.let { return it }
        files.firstOrNull { f -> uniqueNamesOfFontFile(f).any { normalizeFontName(it) == norm } }?.let { return it }
        // 2) Fallback: any declared name (family / PostScript) - only reached when nothing unique matched.
        files.firstOrNull { f -> namesOfFontFile(f).any { it.equals(clean, ignoreCase = true) } }?.let { return it }
        return files.firstOrNull { f -> namesOfFontFile(f).any { normalizeFontName(it) == norm } }
    }

    private val uniqueNamesCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    /** File name + Full Font Name (nameID 4): the identifiers that are unique per font file. */
    private fun uniqueNamesOfFontFile(file: File): List<String> {
        val key = file.absolutePath + "|" + file.length() + "|" + file.lastModified()
        return uniqueNamesCache.getOrPut(key) {
            val names = mutableListOf(file.name, file.nameWithoutExtension)
            try {
                if (file.exists() && file.isFile && file.length() in 100..(30L * 1024 * 1024)) {
                    getFullFontName(file.readBytes())?.takeIf { it.isNotBlank() }?.let { names.add(it) }
                }
            } catch (_: Throwable) {}
            names
        }
    }

    fun getSelectedFontFile(context: Context): File? {
        val name = selectedFontRenderName ?: return null
        if (selectedFontFileCacheName == name && selectedFontFileCache?.exists() == true) return selectedFontFileCache
        val file = findFontFile(context, name)
        selectedFontFileCache = file
        selectedFontFileCacheName = name
        return file
    }

    /** Sorted, merged code point ranges (start[i]..end[i]) a font has glyphs for. */
    private class GlyphRanges(val starts: IntArray, val ends: IntArray) {
        fun covers(cp: Int): Boolean {
            var lo = 0
            var hi = starts.size - 1
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                when {
                    cp < starts[mid] -> hi = mid - 1
                    cp > ends[mid] -> lo = mid + 1
                    else -> return true
                }
            }
            return false
        }
    }

    private val CONSERVATIVE_LATIN_COVERAGE = GlyphRanges(intArrayOf(0x20, 0x2000), intArrayOf(0x24F, 0x206F))

    private val coverageCache = java.util.concurrent.ConcurrentHashMap<String, GlyphRanges>()
    private val coverageMissing = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun coverageOf(file: File): GlyphRanges? {
        val key = file.absolutePath + "|" + file.length() + "|" + file.lastModified()
        coverageCache[key]?.let { return it }
        if (key in coverageMissing) return null
        val parsed = try {
            if (file.exists() && file.length() in 100..(60L * 1024 * 1024)) parseCmapRanges(file.readBytes()) else null
        } catch (_: Throwable) { null }
        if (parsed == null) { coverageMissing.add(key); return null }
        coverageCache[key] = parsed
        return parsed
    }

    /** Reads the cmap table (formats 4 and 12) of a TTF/OTF (first face of a TTC). */
    private fun parseCmapRanges(bytes: ByteArray): GlyphRanges? {
        if (bytes.size < 16) return null
        return parseCmapRanges(java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN), 0)
    }

    /** Same as above but reads straight from a (memory-mapped) buffer; [ttcIndex] picks the face of a TTC. */
    private fun parseCmapRanges(buf: java.nio.ByteBuffer, ttcIndex: Int): GlyphRanges? {
        try {
            val limit = buf.limit()
            if (limit < 16) return null
            var base = 0
            if (buf.getInt(0) == 0x74746366) { // 'ttcf'
                val faces = buf.getInt(8)
                val idx = if (ttcIndex in 0 until faces) ttcIndex else 0
                base = buf.getInt(12 + idx * 4)
            }
            val numTables = buf.getShort(base + 4).toInt() and 0xFFFF
            var cmapOff = -1
            for (i in 0 until numTables) {
                val rec = base + 12 + i * 16
                if (rec + 16 > limit) break
                if (buf.getInt(rec) == 0x636D6170) { cmapOff = buf.getInt(rec + 8); break } // 'cmap'
            }
            if (cmapOff < 0 || cmapOff + 4 > limit) return null
            val subCount = buf.getShort(cmapOff + 2).toInt() and 0xFFFF
            val pairs = ArrayList<LongArray>()
            val seenOffsets = HashSet<Int>()
            for (i in 0 until subCount) {
                val rec = cmapOff + 4 + i * 8
                if (rec + 8 > limit) break
                val platform = buf.getShort(rec).toInt() and 0xFFFF
                val encoding = buf.getShort(rec + 2).toInt() and 0xFFFF
                val isUnicode = platform == 0 || (platform == 3 && (encoding == 1 || encoding == 10))
                if (!isUnicode) continue
                val sub = cmapOff + buf.getInt(rec + 4)
                if (sub < 0 || sub + 4 > limit || !seenOffsets.add(sub)) continue
                when (buf.getShort(sub).toInt() and 0xFFFF) {
                    12 -> {
                        val groups = buf.getInt(sub + 12)
                        for (g in 0 until groups) {
                            val gp = sub + 16 + g * 12
                            if (gp + 12 > limit) break
                            val start = buf.getInt(gp).toLong() and 0xFFFFFFFFL
                            val end = buf.getInt(gp + 4).toLong() and 0xFFFFFFFFL
                            if (end >= start && end <= 0x10FFFF) pairs.add(longArrayOf(start, end))
                        }
                    }
                    4 -> {
                        val segX2 = buf.getShort(sub + 6).toInt() and 0xFFFF
                        val seg = segX2 / 2
                        val endBase = sub + 14
                        val startBase = endBase + segX2 + 2
                        val deltaBase = startBase + segX2
                        val rangeBase = deltaBase + segX2
                        for (sIdx in 0 until seg) {
                            val end = buf.getShort(endBase + sIdx * 2).toInt() and 0xFFFF
                            val start = buf.getShort(startBase + sIdx * 2).toInt() and 0xFFFF
                            val rangeOff = buf.getShort(rangeBase + sIdx * 2).toInt() and 0xFFFF
                            if (start > end || start == 0xFFFF) continue
                            if (rangeOff == 0) {
                                pairs.add(longArrayOf(start.toLong(), end.toLong()))
                            } else {
                                var runStart = -1
                                for (cp in start..end) {
                                    val addr = rangeBase + sIdx * 2 + rangeOff + (cp - start) * 2
                                    val glyph = if (addr + 2 <= limit) buf.getShort(addr).toInt() and 0xFFFF else 0
                                    if (glyph != 0) {
                                        if (runStart < 0) runStart = cp
                                    } else if (runStart >= 0) {
                                        pairs.add(longArrayOf(runStart.toLong(), (cp - 1).toLong())); runStart = -1
                                    }
                                }
                                if (runStart >= 0) pairs.add(longArrayOf(runStart.toLong(), end.toLong()))
                            }
                        }
                    }
                }
            }
            if (pairs.isEmpty()) return null
            pairs.sortBy { it[0] }
            val starts = ArrayList<Int>()
            val ends = ArrayList<Int>()
            var cs = pairs[0][0]
            var ce = pairs[0][1]
            for (i in 1 until pairs.size) {
                val (a, b) = pairs[i][0] to pairs[i][1]
                if (a <= ce + 1) { if (b > ce) ce = b } else {
                    starts.add(cs.toInt()); ends.add(ce.toInt()); cs = a; ce = b
                }
            }
            starts.add(cs.toInt()); ends.add(ce.toInt())
            return GlyphRanges(starts.toIntArray(), ends.toIntArray())
        } catch (_: Throwable) {
            return null
        }
    }

    // ---------------------------------------------------------------------------------------
    // Device (system) fonts as the multi-script fallback
    // ---------------------------------------------------------------------------------------

    /** Prefix of the symlinks (in sub_fonts) that point at the phone's own font files. */
    private const val SYS_LINK_PREFIX = "sys_"

    /** Prefix of Noto fonts downloaded on demand (real files in sub_fonts; never listed as user fonts). */
    private const val DL_PREFIX = "sys_dl_"

    /** Folder (next to sub_fonts, never scanned by libass) that holds the links to the phone's own fonts. */
    private const val SYS_LINK_DIR_NAME = "sub_fonts_sys"

    private class SystemFallbackFont(
        val link: File,
        val ttcIndex: Int,
        val name: String,
        val fullName: String?,
        val isDefaultSans: Boolean,
        val lang: String? = null
    ) {
        /** True when this font's fonts.xml language list names [code] (e.g. "ja", "ko"). */
        fun hasLang(code: String): Boolean = lang?.split(',', ' ')
            ?.any { it.isNotBlank() && it.trim().split('-', '_')[0].equals(code, ignoreCase = true) } == true

        val coverage: GlyphRanges? by lazy(LazyThreadSafetyMode.PUBLICATION) { coverageOfFace(link, ttcIndex) }
    }

    private class SystemFontEntry(val src: File, val ttcIndex: Int, val lang: String?, val isDefaultSans: Boolean)

    @Volatile private var systemFallbacks: List<SystemFallbackFont> = emptyList()
    @Volatile private var systemFallbackKey: String? = null
    private val systemFallbackLock = Any()

    private val SYSTEM_FONT_DIRS = listOf(
        "/system/fonts", "/product/fonts", "/system_ext/fonts", "/vendor/fonts", "/system/font"
    )

    /** The old build bundled GoNotoCurrent-Regular.ttf (15 MB); drop its cached copy when it is no longer shipped. */
    private fun removeStaleBundledFont(context: Context, fontsDir: File) {
        try {
            val shipped = try {
                context.assets.open("fonts/$BUNDLED_FALLBACK_FONT_FILE").close(); true
            } catch (_: Throwable) { false }
            if (!shipped) File(fontsDir, BUNDLED_FALLBACK_FONT_FILE).delete()
        } catch (_: Throwable) {}
    }

    /**
     * Indexes the phone's own script fonts (Roboto, Noto Sans Devanagari/Arabic/Thai/CJK/... exactly as
     * listed in /system/etc/fonts.xml) by linking them into sub_fonts_sys (not into sub_fonts, see below). Nothing is
     * copied, so it costs no storage, and it works offline. Returned in priority order: the default
     * sans-serif first, then fonts of the phone's current language, then the rest.
     */
    private fun prepareSystemFallbackFonts(context: Context, fontsDir: File): List<SystemFallbackFont> {
        val key = java.util.Locale.getDefault().toLanguageTag()
        val cached = systemFallbacks
        if (systemFallbackKey == key && (cached.isEmpty() || cached.all { it.link.exists() })) return cached
        synchronized(systemFallbackLock) {
            val again = systemFallbacks
            if (systemFallbackKey == key && (again.isEmpty() || again.all { it.link.exists() })) return again
            // Most links live OUTSIDE sub_fonts. libass reads every file of its fonts folder each time a subtitle
            // track is started (select / switch), on mpv's core thread; ~100 linked system fonts (100+ MB) made
            // every subtitle change stall picture and sound. Only the few fonts that must always be found by libass
            // (the phone's default font + the phone-language fonts) are also linked into sub_fonts. The rest is still
            // found by fontconfig by family name (their folders are in fonts.conf).
            val sysDir = File(fontsDir.parentFile ?: fontsDir, SYS_LINK_DIR_NAME).apply { mkdirs() }
            val built = try {
                buildSystemFallbacks(sysDir, fontsDir, java.util.Locale.getDefault())
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to link system fonts for subtitle fallback", t)
                emptyList()
            }
            systemFallbacks = built
            systemFallbackKey = key
            built.firstOrNull { it.isDefaultSans }?.let { systemDefaultFamily = it.name }
            try { ensureEssentialScriptsVisible(context, built) } catch (t: Throwable) { Log.w(TAG, "script probe failed", t) }
            return built
        }
    }

    /**
     * Scripts whose system font is ALWAYS handed to libass directly (Japanese / Korean / Chinese,
     * Devanagari, Arabic, Thai, Hebrew, Bengali, Tamil, symbols). Without this only the fonts of the
     * phone's current language were visible, so every other script came out as boxes unless the
     * phone language was switched. Other scripts are linked on demand (see [ensureVisibleToLibass]).
     */
    private fun isEssentialScript(lang: String?): Boolean {
        if (lang.isNullOrBlank()) return false
        val langs = setOf("ja", "ko", "zh")
        val scripts = setOf("deva", "arab", "thai", "hebr", "zsym", "zmth", "beng", "taml")
        return lang.split(',', ' ').filter { it.isNotBlank() }.any { tag ->
            val parts = tag.trim().split('-', '_')
            parts[0].lowercase(java.util.Locale.ROOT) in langs ||
                parts.drop(1).any { it.lowercase(java.util.Locale.ROOT) in scripts }
        }
    }

    /** sys_ link names that were added to the libass fonts folder because a subtitle really needed them. */
    private val onDemandLinked = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Makes sure libass itself can see this system font (a link inside sub_fonts). libass lists that folder
     * every time a subtitle track starts, so a font linked here before the subtitle file is written is found
     * by its own family name no matter what fontconfig does with the system font folders.
     */
    private fun ensureVisibleToLibass(context: Context, font: SystemFallbackFont) {
        try {
            val name = if (font.link.name.startsWith(SYS_LINK_PREFIX)) font.link.name else SYS_LINK_PREFIX + font.link.name
            val dst = File(getSubFontsDir(context), name)
            if (dst.exists()) { onDemandLinked.add(name); return }
            dst.delete()
            android.system.Os.symlink(font.link.canonicalPath, dst.absolutePath)
            onDemandLinked.add(name)
        } catch (_: Throwable) {}
    }

    // ---------------------------------------------------------------------------------------------
    // Script coverage that does NOT depend on /system/etc/fonts.xml language tags:
    //  1. every face of every font file in the phone's font folders is checked by its real glyph table;
    //  2. if the phone has no font at all for a script, the matching Google Noto font is downloaded once
    //     (needs internet, ~100-400 KB) and kept in sub_fonts for every later subtitle.
    // ---------------------------------------------------------------------------------------------

    @Volatile private var allFacesCache: List<SystemFallbackFont>? = null
    @Volatile private var downloadedFacesCache: List<SystemFallbackFont>? = null
    private val downloadsInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val downloadFailedAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun ttcFaceCount(file: File): Int = try {
        java.io.RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(12)
            raf.readFully(head)
            if (String(head, 0, 4, Charsets.ISO_8859_1) == "ttcf") java.nio.ByteBuffer.wrap(head).order(java.nio.ByteOrder.BIG_ENDIAN).getInt(8) else 1
        }
    } catch (_: Throwable) { 1 }

    private fun faceFont(file: File, index: Int): SystemFallbackFont? {
        val names = readFaceNames(file, index) ?: return null
        val family = (names.first ?: names.second)?.takeIf { it.isNotBlank() } ?: return null
        if (family.any { it == '{' || it == '}' || it == '\\' || it == ',' }) return null
        return SystemFallbackFont(file, index, family, names.second, false, null)
    }

    /** Every regular-style face of every font file in the phone's font folders, found by file (not fonts.xml). */
    private fun scanAllSystemFaces(): List<SystemFallbackFont> {
        allFacesCache?.let { return it }
        synchronized(systemFallbackLock) {
            allFacesCache?.let { return it }
            val out = ArrayList<SystemFallbackFont>()
            val skip = listOf("emoji", "italic", "bold", "thin", "light", "black", "medium", "condensed", "semi", "demi", "extra")
            val exts = setOf("ttf", "otf", "ttc", "otc")
            for (d in SYSTEM_FONT_DIRS) {
                val files = try { File(d).listFiles() } catch (_: Throwable) { null } ?: continue
                for (f in files.sortedBy { it.name }) {
                    if (!f.isFile || f.extension.lowercase(java.util.Locale.ROOT) !in exts) continue
                    val lower = f.name.lowercase(java.util.Locale.ROOT)
                    if (skip.any { lower.contains(it) }) continue
                    val faces = if (lower.endsWith("c")) ttcFaceCount(f).coerceIn(1, 16) else 1
                    for (i in 0 until faces) faceFont(f, i)?.let { out.add(it) }
                }
            }
            // Android's own list of installed fonts (the one text editors use): also finds OEM / user-installed
            // fonts that live outside the folders above or are missing from fonts.xml.
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                try { addAndroidSystemFonts(out) } catch (t: Throwable) { Log.w(TAG, "SystemFonts API unavailable", t) }
            }
            allFacesCache = out
            return out
        }
    }

    @android.annotation.TargetApi(29)
    private fun addAndroidSystemFonts(out: MutableList<SystemFallbackFont>) {
        val known = HashSet<String>()
        for (f in out) known.add(f.link.absolutePath + "#" + f.ttcIndex)
        val skip = listOf("emoji", "italic", "bold", "thin", "light", "black", "medium", "condensed", "semi", "demi", "extra")
        for (font in android.graphics.fonts.SystemFonts.getAvailableFonts()) {
            val file = font.file ?: continue
            val lower = file.name.lowercase(java.util.Locale.ROOT)
            if (skip.any { lower.contains(it) }) continue
            if (!known.add(file.absolutePath + "#" + font.ttcIndex)) continue
            faceFont(file, font.ttcIndex)?.let { out.add(it) }
        }
    }

    /** Noto fonts downloaded earlier (files in sub_fonts). */
    private fun downloadedFaces(context: Context): List<SystemFallbackFont> {
        downloadedFacesCache?.let { return it }
        val out = ArrayList<SystemFallbackFont>()
        try {
            getSubFontsDir(context).listFiles()?.filter { it.isFile && it.name.startsWith(DL_PREFIX) }?.sortedBy { it.name }?.forEach { f ->
                faceFont(f, 0)?.let { out.add(it) }
            }
        } catch (_: Throwable) {}
        downloadedFacesCache = out
        return out
    }

    /** Google Noto family (notofonts.github.io) for the script of [cp]; null when none is known / needed. */
    private fun onlineFontFor(cp: Int): String? = when (cp) {
        in 0x0530..0x058F -> "NotoSansArmenian"
        in 0x0590..0x05FF, in 0xFB1D..0xFB4F -> "NotoSansHebrew"
        in 0x0600..0x06FF, in 0x0750..0x077F, in 0x08A0..0x08FF, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> "NotoSansArabic"
        in 0x0900..0x097F, in 0xA8E0..0xA8FF -> "NotoSansDevanagari"
        in 0x0980..0x09FF -> "NotoSansBengali"
        in 0x0A00..0x0A7F -> "NotoSansGurmukhi"
        in 0x0A80..0x0AFF -> "NotoSansGujarati"
        in 0x0B00..0x0B7F -> "NotoSansOriya"
        in 0x0B80..0x0BFF -> "NotoSansTamil"
        in 0x0C00..0x0C7F -> "NotoSansTelugu"
        in 0x0C80..0x0CFF -> "NotoSansKannada"
        in 0x0D00..0x0D7F -> "NotoSansMalayalam"
        in 0x0D80..0x0DFF -> "NotoSansSinhala"
        in 0x0E00..0x0E7F -> "NotoSansThai"
        in 0x0E80..0x0EFF -> "NotoSansLao"
        in 0x1000..0x109F -> "NotoSansMyanmar"
        in 0x10A0..0x10FF, in 0x2D00..0x2D2F -> "NotoSansGeorgian"
        in 0x1200..0x139F -> "NotoSansEthiopic"
        in 0x1780..0x17FF -> "NotoSansKhmer"
        in 0x2190..0x23FF, in 0x2600..0x27BF -> "NotoSansSymbols"
        in 0x2200..0x22FF -> "NotoSansMath"
        else -> null
    }

    /**
     * No font of the phone covers [cp]: fetch the Google Noto font of its script in the background. The
     * download never blocks the subtitle; it is used from the next subtitle load on (and stays cached).
     */
    private fun requestOnlineFont(context: Context, cp: Int) {
        val family = onlineFontFor(cp) ?: return
        try {
            val dst = File(getSubFontsDir(context), "$DL_PREFIX$family.ttf")
            if (dst.exists()) return
            val failed = downloadFailedAt[family]
            if (failed != null && System.currentTimeMillis() - failed < 10 * 60 * 1000L) return
            if (!downloadsInFlight.add(family)) return
            val cache = context.applicationContext.cacheDir
            Thread({
                try {
                    val url = java.net.URL("https://raw.githubusercontent.com/notofonts/notofonts.github.io/main/fonts/$family/hinted/ttf/$family-Regular.ttf")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 20000
                    conn.instanceFollowRedirects = true
                    val tmp = File(cache, "$family.font.part")
                    conn.inputStream.use { input -> tmp.outputStream().use { out -> input.copyTo(out) } }
                    if (tmp.length() > 2000 && readFaceNames(tmp, 0) != null && coverageOfFace(tmp, 0)?.covers(cp) == true) {
                        dst.delete()
                        if (!tmp.renameTo(dst)) { tmp.copyTo(dst, overwrite = true); tmp.delete() }
                        downloadedFacesCache = null
                        Log.i(TAG, "Downloaded subtitle font $family for U+${Integer.toHexString(cp)}")
                    } else {
                        tmp.delete()
                        downloadFailedAt[family] = System.currentTimeMillis()
                    }
                } catch (t: Throwable) {
                    downloadFailedAt[family] = System.currentTimeMillis()
                    Log.w(TAG, "Subtitle font download failed: $family", t)
                } finally {
                    downloadsInFlight.remove(family)
                }
            }, "subtitle-font-download").apply { isDaemon = true }.start()
        } catch (_: Throwable) {}
    }

    /**
     * Gives libass (not only fontconfig) a font for every major script, using the phone's glyph tables.
     * Needed for embedded subtitle tracks, whose text we never rewrite.
     */
    private fun ensureEssentialScriptsVisible(context: Context, fallbacks: List<SystemFallbackFont>) {
        val probes = listOf(
            0x3042 to "ja", 0x4E00 to "ja", 0xAC00 to "ko", 0x0915 to null, 0x0627 to null, 0x0E01 to null,
            0x05D0 to null, 0x0995 to null, 0x0B95 to null, 0x0C15 to null, 0x0D15 to null, 0x0C95 to null,
            0x0A95 to null, 0x0A15 to null, 0x2605 to null, 0x266A to null
        )
        val memo = HashMap<Long, SystemFallbackFont?>()
        val memo2 = HashMap<Long, SystemFallbackFont?>()
        for ((cp, hint) in probes) {
            val f = pickSystemFallback(fallbacks, cp, hint, memo)
                ?: pickSystemFallback(scanAllSystemFaces(), cp, hint, memo2)
                ?: continue
            ensureVisibleToLibass(context, f)
        }
    }

    private fun buildSystemFallbacks(fontsDir: File, libassDir: File, locale: java.util.Locale): List<SystemFallbackFont> {
        var entries = parseSystemFontsXml()
        if (entries.isEmpty()) entries = scanSystemFontDirs()

        fun langMatches(lang: String?): Boolean {
            if (lang.isNullOrBlank()) return false
            val country = locale.country.uppercase(java.util.Locale.ROOT)
            val hant = locale.script.equals("Hant", true) || country in setOf("TW", "HK", "MO")
            return lang.split(',', ' ').filter { it.isNotBlank() }.any { tag ->
                val parts = tag.trim().split('-', '_')
                if (!parts[0].equals(locale.language, ignoreCase = true)) return@any false
                val script = parts.getOrNull(1)
                script == null || when {
                    script.equals("Hant", true) -> hant
                    script.equals("Hans", true) -> locale.language == "zh" && !hant
                    else -> true
                }
            }
        }

        val sorted = entries.sortedWith(
            compareBy<SystemFontEntry>({ if (it.isDefaultSans) 0 else 1 }, { if (langMatches(it.lang)) 0 else 1 })
        )

        val wanted = HashSet<String>()
        val libassWanted = HashSet<String>()
        val result = ArrayList<SystemFallbackFont>()
        val seen = HashSet<String>()
        for (e in sorted) {
            if (!seen.add(e.src.name + "#" + e.ttcIndex)) continue
            val linkName = SYS_LINK_PREFIX + e.src.name
            val link = File(fontsDir, linkName)
            if (!link.exists()) {
                try {
                    link.delete()
                    android.system.Os.symlink(e.src.absolutePath, link.absolutePath)
                } catch (_: Throwable) { continue }
            }
            wanted.add(linkName)
            if (e.isDefaultSans || langMatches(e.lang) || isEssentialScript(e.lang) || linkName in onDemandLinked) {
                // Always visible to libass itself (small set), independent of fontconfig.
                val direct = File(libassDir, linkName)
                if (!direct.exists()) {
                    try {
                        direct.delete()
                        android.system.Os.symlink(e.src.absolutePath, direct.absolutePath)
                    } catch (_: Throwable) {}
                }
                libassWanted.add(linkName)
            }
            val names = readFaceNames(link, e.ttcIndex) ?: continue
            val family = (names.first ?: names.second)?.takeIf { it.isNotBlank() } ?: continue
            if (family.any { it == '{' || it == '}' || it == '\\' || it == ',' }) continue
            result.add(SystemFallbackFont(link, e.ttcIndex, family, names.second, e.isDefaultSans, e.lang))
        }

        // Remove links from an older font set (e.g. after a system update) so libass never scans stale entries.
        fontsDir.listFiles()?.forEach { f ->
            if (f.name.startsWith(SYS_LINK_PREFIX) && f.name !in wanted) {
                try { f.delete() } catch (_: Throwable) {}
            }
        }
        // Links of earlier builds (all system fonts) and of an older font set must not stay in the libass folder.
        libassDir.listFiles()?.forEach { f ->
            if (f.name.startsWith(SYS_LINK_PREFIX) && !f.name.startsWith(DL_PREFIX) && f.name !in libassWanted) {
                try { f.delete() } catch (_: Throwable) {}
            }
        }
        return result
    }

    /** Fallback font families of /system/etc/fonts.xml: the regular face of each family, deduplicated. */
    private fun parseSystemFontsXml(): List<SystemFontEntry> {
        val xml = listOf("/system/etc/fonts.xml", "/system/etc/font_fallback.xml")
            .map { File(it) }.firstOrNull { it.isFile && it.canRead() } ?: return emptyList()
        fun resolve(name: String): File? {
            for (d in SYSTEM_FONT_DIRS) {
                val f = File(d, name)
                if (f.isFile) return f
            }
            return null
        }
        val out = ArrayList<SystemFontEntry>()
        try {
            java.io.FileInputStream(xml).use { input ->
                val parser = android.util.Xml.newPullParser()
                parser.setInput(input, null)
                class Face(val file: String, val weight: Int, val index: Int)
                var famName: String? = null
                var famLang: String? = null
                var faces = ArrayList<Face>()
                var inFont = false
                var weight = 400
                var normal = true
                var index = 0
                val text = StringBuilder()
                var ev = parser.eventType
                while (ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    when (ev) {
                        org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                            "family" -> {
                                famName = parser.getAttributeValue(null, "name")
                                famLang = parser.getAttributeValue(null, "lang")
                                faces = ArrayList()
                            }
                            "font" -> {
                                inFont = true
                                text.setLength(0)
                                weight = parser.getAttributeValue(null, "weight")?.toIntOrNull() ?: 400
                                normal = !"italic".equals(parser.getAttributeValue(null, "style"), true)
                                index = parser.getAttributeValue(null, "index")?.toIntOrNull() ?: 0
                            }
                        }
                        org.xmlpull.v1.XmlPullParser.TEXT -> if (inFont) text.append(parser.text)
                        org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
                            "font" -> if (inFont) {
                                inFont = false
                                val fn = text.toString().trim()
                                val ext = fn.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                                if (normal && fn.isNotEmpty() && ext in setOf("ttf", "otf", "ttc", "otc") &&
                                    !fn.contains("emoji", ignoreCase = true)
                                ) faces.add(Face(fn, weight, index))
                            }
                            "family" -> {
                                val best = faces.minByOrNull { Math.abs(it.weight - 400) }
                                val file = best?.let { resolve(it.file) }
                                if (best != null && file != null) {
                                    out.add(SystemFontEntry(file, best.index, famLang, "sans-serif".equals(famName, true)))
                                }
                                famName = null; famLang = null; faces = ArrayList()
                            }
                        }
                    }
                    ev = parser.next()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Could not read fonts.xml", t)
        }
        return out
    }

    /** Last resort when fonts.xml is missing/unreadable: Roboto + every Noto *-Regular in the system font folders. */
    private fun scanSystemFontDirs(): List<SystemFontEntry> {
        val out = ArrayList<SystemFontEntry>()
        for (d in SYSTEM_FONT_DIRS) {
            val files = try { File(d).listFiles() } catch (_: Throwable) { null } ?: continue
            for (f in files.sortedBy { it.name }) {
                if (!f.isFile) continue
                val n = f.name
                val ext = f.extension.lowercase(java.util.Locale.ROOT)
                if (ext !in setOf("ttf", "otf", "ttc", "otc") || n.contains("emoji", true) || n.contains("italic", true)) continue
                val isRoboto = n.startsWith("Roboto-Regular", true) || n.equals("Roboto[wdth,wght].ttf", true)
                val isNoto = n.startsWith("Noto", true) && (n.contains("-Regular") || n.contains("[wght]") || ext == "ttc")
                if (isRoboto || isNoto) out.add(SystemFontEntry(f, 0, null, isRoboto))
            }
        }
        return out
    }

    /** (family name, full name) of one face; reads only the small 'name' table. */
    private fun readFaceNames(file: File, ttcIndex: Int): Pair<String?, String?>? {
        return try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val size = raf.length()
                if (size < 16 || size > Int.MAX_VALUE) return@use null
                val buf = raf.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, size)
                    .order(java.nio.ByteOrder.BIG_ENDIAN)
                val limit = buf.limit()
                var base = 0
                if (buf.getInt(0) == 0x74746366) { // 'ttcf'
                    val faces = buf.getInt(8)
                    val idx = if (ttcIndex in 0 until faces) ttcIndex else 0
                    base = buf.getInt(12 + idx * 4)
                }
                val numTables = buf.getShort(base + 4).toInt() and 0xFFFF
                var nameOff = -1
                for (i in 0 until numTables) {
                    val rec = base + 12 + i * 16
                    if (rec + 16 > limit) break
                    if (buf.getInt(rec) == 0x6E616D65) { nameOff = buf.getInt(rec + 8); break } // 'name'
                }
                if (nameOff < 0 || nameOff + 6 > limit) return@use null
                val count = buf.getShort(nameOff + 2).toInt() and 0xFFFF
                val storage = nameOff + (buf.getShort(nameOff + 4).toInt() and 0xFFFF)
                var family: String? = null
                var full: String? = null
                var familyPref = false
                var fullPref = false
                for (i in 0 until count) {
                    val nr = nameOff + 6 + i * 12
                    if (nr + 12 > limit) break
                    val platform = buf.getShort(nr).toInt() and 0xFFFF
                    val nameId = buf.getShort(nr + 6).toInt() and 0xFFFF
                    if (nameId != 1 && nameId != 4) continue
                    val len = buf.getShort(nr + 8).toInt() and 0xFFFF
                    val off = storage + (buf.getShort(nr + 10).toInt() and 0xFFFF)
                    if (len <= 0 || off + len > limit) continue
                    val raw = ByteArray(len)
                    for (k in 0 until len) raw[k] = buf.get(off + k)
                    val preferred = platform == 0 || platform == 3
                    val str = String(
                        raw,
                        if (preferred) java.nio.charset.StandardCharsets.UTF_16BE else java.nio.charset.StandardCharsets.UTF_8
                    ).trim()
                    if (str.isBlank()) continue
                    if (nameId == 1 && (family == null || (preferred && !familyPref))) { family = str; familyPref = preferred }
                    if (nameId == 4 && (full == null || (preferred && !fullPref))) { full = str; fullPref = preferred }
                }
                Pair(family, full)
            }
        } catch (_: Throwable) { null }
    }

    private fun coverageOfFace(file: File, ttcIndex: Int): GlyphRanges? {
        return try {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val size = raf.length()
                if (size < 16 || size > Int.MAX_VALUE) return@use null
                val buf = raf.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, size)
                    .order(java.nio.ByteOrder.BIG_ENDIAN)
                parseCmapRanges(buf, ttcIndex)
            }
        } catch (_: Throwable) { null }
    }

    /**
     * First system fallback font (in priority order) that really has a glyph for [cp]. [hint] ("ja" / "ko")
     * prefers the Japanese / Korean face of a CJK font so kanji / hanja look right next to kana / hangul.
     */
    private fun pickSystemFallback(
        fallbacks: List<SystemFallbackFont>,
        cp: Int,
        hint: String?,
        memo: HashMap<Long, SystemFallbackFont?>
    ): SystemFallbackFont? {
        val key = (cp.toLong() shl 4) or (when (hint) { "ja" -> 1L; "ko" -> 2L; else -> 0L })
        if (memo.containsKey(key)) return memo[key]
        var found: SystemFallbackFont? = null
        if (hint != null) found = fallbacks.firstOrNull { it.coverage?.covers(cp) == true && it.hasLang(hint) }
        if (found == null) found = fallbacks.firstOrNull { it.coverage?.covers(cp) == true }
        memo[key] = found
        return found
    }

    private fun bundledFallbackFile(context: Context): File = File(getSubFontsDir(context), BUNDLED_FALLBACK_FONT_FILE)

    private fun bundledFallbackName(file: File): String {
        return try {
            getFullFontName(file.readBytes())?.takeIf { it.isNotBlank() } ?: BUNDLED_FALLBACK_FONT_FAMILY
        } catch (_: Throwable) { BUNDLED_FALLBACK_FONT_FAMILY }
    }

    /**
     * "Override ASS/SSA Styles" is ON (and Advanced ASS/SSA is not in charge): the user's selected
     * font must be THE font of every style and every inline \\fn of a native ASS/SSA script.
     * libass' own force-style only replaces the style fonts, so the subtitle text is rewritten
     * here instead (see [forceSelectedFontEverywhere]).
     */
    @Volatile var forceSelectedFontOnAss: Boolean = false

    /**
     * Removes the inline font overrides that [applyGlyphFallback] injected earlier
     * (`{\\fnGo Noto Current-Regular}` ... `{\\fn}`). Without this a later font change keeps drawing
     * those characters (e.g. Hindi) with the old fallback font, even if the new font has them.
     */
    fun stripGlyphFallback(context: Context, assContent: String): String {
        if (assContent.isBlank() || !assContent.contains("\\fn")) return assContent
        return try {
            val names = linkedSetOf(BUNDLED_FALLBACK_FONT_FAMILY, LEGACY_FALLBACK_FONT_FAMILY)
            for (f in prepareSystemFallbackFonts(context, getSubFontsDir(context))) {
                names.add(f.name)
                f.fullName?.let { names.add(it) }
            }
            var out = assContent
            for (n in names) out = out.replace("{\\fn$n}", "")
            out.replace("{\\fn}", "")
        } catch (_: Throwable) {
            assContent
        }
    }

    /**
     * Gives every Style line and every inline `\\fn` the same [fontName], so one font drives the
     * whole script. Layout, colours, tags and timing are untouched.
     */
    fun forceSelectedFontEverywhere(assContent: String, fontName: String): String {
        if (assContent.isBlank() || fontName.isBlank()) return assContent
        return try {
            val lines = assContent.split("\n").toMutableList()
            var inStyles = false
            var format = emptyList<String>()
            var inEvents = false
            val fnPattern = Regex("\\\\fn[^\\\\}]*")
            for (i in lines.indices) {
                val original = lines[i]
                val t = original.trim()
                if (t.startsWith("[") && t.endsWith("]")) {
                    val low = t.lowercase(java.util.Locale.ROOT)
                    inStyles = low == "[v4+ styles]" || low == "[v4 styles]"
                    inEvents = low == "[events]"
                    continue
                }
                if (inStyles && t.startsWith("Format:", ignoreCase = true)) {
                    format = t.substringAfter(":").split(",").map { it.trim().lowercase(java.util.Locale.ROOT) }
                    continue
                }
                if (inStyles && t.startsWith("Style:", ignoreCase = true)) {
                    val idx = original.indexOf("Style:", ignoreCase = true)
                    val head = original.substring(0, idx + 6)
                    val parts = original.substring(idx + 6).split(",").toMutableList()
                    val fontIndex = format.indexOf("fontname").takeIf { it >= 0 } ?: 1
                    if (fontIndex in parts.indices) {
                        val lead = if (parts[fontIndex].isNotEmpty() && parts[fontIndex].first() == ' ') " " else ""
                        val vertical = parts[fontIndex].trim().startsWith("@")
                        parts[fontIndex] = lead + (if (vertical) "@" else "") + fontName
                        lines[i] = head + parts.joinToString(",")
                    }
                    continue
                }
                if (inEvents && original.contains("\\fn")) {
                    lines[i] = fnPattern.replace(original) { "" }
                }
            }
            lines.joinToString("\n")
        } catch (_: Throwable) {
            assContent
        }
    }

    /**
     * PER-GLYPH FALLBACK. The user's selected font stays the font of the subtitle. Every character
     * that font has NO glyph for (music notes, symbols, other scripts...) is wrapped in a tiny
     * inline `{\fn<Go Noto>}...{\fn}` override so that it is drawn from the bundled Go Noto font
     * instead of an empty "tofu" box. Text the font can already draw is left byte-for-byte alone.
     */
    fun applyGlyphFallback(context: Context, assContent: String): String {
        if (assContent.isBlank() || !assContent.contains("[Events]", ignoreCase = true)) return assContent
        try {
            val fallbacks = prepareSystemFallbackFonts(context, getSubFontsDir(context))
            if (fallbacks.isEmpty() && scanAllSystemFaces().isEmpty()) return assContent
            val pickMemo = HashMap<Long, SystemFallbackFont?>()
            val scanMemo = HashMap<Long, SystemFallbackFont?>()
            val dlMemo = HashMap<Long, SystemFallbackFont?>()
            val pickFallback: (Int, String?) -> SystemFallbackFont? = { cp, hint ->
                val found = pickSystemFallback(fallbacks, cp, hint, pickMemo)
                    ?: pickSystemFallback(scanAllSystemFaces(), cp, hint, scanMemo)
                    ?: pickSystemFallback(downloadedFaces(context), cp, hint, dlMemo)
                if (found == null) {
                    Log.w(TAG, "No font on this phone covers U+${Integer.toHexString(cp)}; requesting online font")
                    requestOnlineFont(context, cp)
                }
                found?.also { ensureVisibleToLibass(context, it) }
            }

            val coverageByFont = HashMap<String, GlyphRanges?>()
            fun coverageFor(fontName: String): GlyphRanges? {
                val clean = fontName.trim().removePrefix("@")
                if (clean.isBlank()) return null
                return coverageByFont.getOrPut(clean.lowercase(java.util.Locale.ROOT)) {
                    // A system font (e.g. the default sans-serif the subtitle falls back to) is known by name.
                    fallbacks.firstOrNull {
                        it.name.equals(clean, ignoreCase = true) || it.fullName.equals(clean, ignoreCase = true)
                    }?.coverage?.let { return@getOrPut it }
                    val sel = selectedFontRenderName
                    val isSelected = sel != null && sel.equals(clean, ignoreCase = true)
                    val file = if (isSelected) {
                        getSelectedFontFile(context) ?: findFontFile(context, clean)
                    } else findFontFile(context, clean)
                    // A font we can locate but whose glyph table cannot be read (WOFF, odd cmap
                    // formats, huge files) - or the user's selected font whose file cannot be
                    // resolved by name - must NOT silently skip the fallback, otherwise every
                    // character outside basic Latin turns into a tofu box. Treat it as covering
                    // only basic Latin; the bundled font then draws the rest (it is only used
                    // for characters it really has).
                    val parsed = file?.let { coverageOf(it) }
                    // A font nobody can locate (the phone's own custom font, a font libass would drop)
                    // is treated the same way: libass would draw its missing glyphs as boxes too.
                    parsed ?: CONSERVATIVE_LATIN_COVERAGE
                }
            }

            // "Override ASS/SSA Styles" ON: the selected font IS the font of every style, no matter
            // what the script itself names. The coverage check must use that font, otherwise the
            // script's original font (which usually has no Devanagari) makes every Hindi character
            // get wrapped in a {\fnGo Noto} tag and the selected font is bypassed for those glyphs.
            val forcedFont: String? = if (forceSelectedFontOnAss) selectedFontRenderName?.takeIf { it.isNotBlank() } else null

            val lines = assContent.split("\n")
            val out = ArrayList<String>(lines.size)
            var section = ""
            var styleFormat = emptyList<String>()
            var eventFormat = emptyList<String>()
            val styleFonts = HashMap<String, String>()
            var changed = false

            for (raw in lines) {
                val cr = raw.endsWith("\r")
                val line = if (cr) raw.dropLast(1) else raw
                val t = line.trim()
                if (t.startsWith("[") && t.endsWith("]")) { section = t.lowercase(java.util.Locale.ROOT) }
                if (section.startsWith("[v4") && t.startsWith("Format:", ignoreCase = true)) {
                    styleFormat = t.substringAfter(":").split(",").map { it.trim().lowercase(java.util.Locale.ROOT) }
                } else if (section == "[events]" && t.startsWith("Format:", ignoreCase = true)) {
                    eventFormat = t.substringAfter(":").split(",").map { it.trim().lowercase(java.util.Locale.ROOT) }
                } else if (section.startsWith("[v4") && t.startsWith("Style:", ignoreCase = true)) {
                    val parts = t.substringAfter(":").split(",")
                    val ni = styleFormat.indexOf("name").takeIf { it >= 0 } ?: 0
                    val fi = styleFormat.indexOf("fontname").takeIf { it >= 0 } ?: 1
                    if (ni < parts.size && fi < parts.size) styleFonts[parts[ni].trim().lowercase(java.util.Locale.ROOT)] = parts[fi].trim()
                }

                if (section == "[events]" && t.startsWith("Dialogue:", ignoreCase = true) && eventFormat.isNotEmpty()) {
                    val n = eventFormat.size
                    val body = line.substring(line.indexOf(':') + 1)
                    val parts = body.split(",", limit = n)
                    val styleIdx = eventFormat.indexOf("style").takeIf { it >= 0 } ?: 3
                    if (parts.size == n && styleIdx < n) {
                        val text = parts[n - 1]
                        val styleName = parts[styleIdx].trim().lowercase(java.util.Locale.ROOT).removePrefix("*")
                        val baseFont = forcedFont ?: styleFonts[styleName] ?: styleFonts["default"] ?: ""
                        val newText = injectFallbackIntoText(text, baseFont, styleFonts, pickFallback, ::coverageFor, forcedFont)
                        if (newText != text) {
                            changed = true
                            val prefix = line.substring(0, line.indexOf(':') + 1) + parts.dropLast(1).joinToString(",") + ","
                            out.add(prefix + newText + if (cr) "\r" else "")
                            continue
                        }
                    }
                }
                out.add(raw)
            }
            return if (changed) out.joinToString("\n") else assContent
        } catch (t: Throwable) {
            Log.w(TAG, "Glyph fallback failed; keeping subtitle text untouched", t)
            return assContent
        }
    }

    private fun injectFallbackIntoText(
        text: String,
        styleFont: String,
        styleFonts: Map<String, String>,
        pickFallback: (Int, String?) -> SystemFallbackFont?,
        coverageFor: (String) -> GlyphRanges?,
        forcedFont: String? = null
    ): String {
        var baseFont = styleFont
        var inlineFont: String? = null
        var drawing = false
        var inFallback = false
        var fallbackName: String? = null
        val sb = StringBuilder(text.length + 16)
        val tagPattern = Regex("\\\\(fn|r|p)([^\\\\}]*)")
        // Kana / hangul in the line: kanji / hanja of the same line use the Japanese / Korean face.
        var scriptHint: String? = null
        run {
            var k = 0
            while (k < text.length) {
                val c = text.codePointAt(k)
                if (c in 0x3040..0x30FF || c in 0x31F0..0x31FF || c in 0xFF66..0xFF9F) { scriptHint = "ja"; break }
                if (c in 0xAC00..0xD7AF || c in 0x1100..0x11FF || c in 0x3130..0x318F) scriptHint = "ko"
                k += Character.charCount(c)
            }
        }

        fun currentFont() = inlineFont ?: baseFont
        fun closeFallback() {
            if (inFallback) {
                sb.append(if (inlineFont != null) "{\\fn$inlineFont}" else "{\\fn}")
                inFallback = false
                fallbackName = null
            }
        }

        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            if (c == '{') {
                val close = text.indexOf('}', i)
                if (close < 0) { sb.append(text, i, len); i = len; continue }
                val block = text.substring(i + 1, close)
                closeFallback()
                for (m in tagPattern.findAll(block)) {
                    val key = m.groupValues[1]
                    val value = m.groupValues[2].trim()
                    when (key) {
                        "fn" -> inlineFont = if (forcedFont != null) null else value.ifBlank { null }
                        "r" -> {
                            inlineFont = null
                            if (forcedFont != null) baseFont = forcedFont
                            else if (value.isNotBlank()) styleFonts[value.lowercase(java.util.Locale.ROOT)]?.let { baseFont = it }
                            else baseFont = styleFont
                        }
                        "p" -> value.toIntOrNull()?.let { drawing = it > 0 }
                    }
                }
                sb.append(text, i, close + 1)
                i = close + 1
                continue
            }
            if (c == '\\' && i + 1 < len && (text[i + 1] == 'N' || text[i + 1] == 'n' || text[i + 1] == 'h')) {
                sb.append(c).append(text[i + 1]); i += 2; continue
            }
            if (drawing) { sb.append(c); i++; continue }

            val cp = text.codePointAt(i)
            val cpLen = Character.charCount(cp)
            val neutral = cp <= 0x20 || cp == 0x7F || cp in 0xFE00..0xFE0F || cp == 0x200B || cp == 0x200C ||
                cp == 0x200D || cp == 0xA0 || Character.getType(cp) == Character.FORMAT.toInt()
            val target: SystemFallbackFont? = if (neutral) null else {
                val cov = coverageFor(currentFont())
                if (cov != null && !cov.covers(cp)) pickFallback(cp, scriptHint) else null
            }
            if (neutral) {
                // spaces / joiners stay in whatever font the previous character used
            } else if (target != null) {
                if (!inFallback || fallbackName != target.name) {
                    sb.append("{\\fn").append(target.name).append('}')
                    inFallback = true
                    fallbackName = target.name
                }
            } else if (inFallback) closeFallback()
            sb.appendCodePoint(cp)
            i += cpLen
        }
        closeFallback()
        return sb.toString()
    }

    /**
     * Sanitizes ASS subtitle content so that any style with a missing custom font
     * falls back to the default system font ("sans-serif" / "Arial"), while PRESERVING
     * 100% of the style's colors, sizes, borders, shadows, alignments, karaoke tags, and animations!
     */
    fun sanitizeAssForFallback(context: Context, assContent: String): String {
        if (assContent.isBlank()) return assContent
        if (!assContent.contains("[V4+ Styles]", ignoreCase = true) &&
            !assContent.contains("[V4 Styles]", ignoreCase = true)
        ) return assContent

        try {
            val lines = assContent.lines().toMutableList()
            var inStylesSection = false
            var styleFormatFields = emptyList<String>()
            val availableCache = mutableMapOf<String, Boolean>()

            fun available(fontName: String): Boolean {
                val clean = fontName.trim()
                if (clean.isBlank()) return false
                return availableCache.getOrPut(clean.lowercase(java.util.Locale.ROOT)) {
                    isFontAvailable(context, clean)
                }
            }

            // A font the subtitle asks for but nobody ships is replaced by the font the USER
            // selected (so their choice really is what gets displayed); only when the user
            // selected nothing does the bundled multi-script font take over.
            val replacementFont = selectedFontRenderName?.takeIf { it.isNotBlank() } ?: BUNDLED_FALLBACK_FONT_FAMILY
            // Normal subtitle editor font: when the video/subtitle does NOT ship the font a style
            // asks for, the font the user picked is used (system fonts like Arial included).
            // A font that IS attached to the video/subtitle always keeps its own font.
            val userFont = selectedFontRenderName?.takeIf { it.isNotBlank() }
            val suppliedCache = mutableMapOf<String, Boolean>()
            fun supplied(fontName: String): Boolean =
                suppliedCache.getOrPut(fontName.trim().lowercase(java.util.Locale.ROOT)) {
                    isSuppliedByCurrentVideo(context, fontName)
                }
            fun fallbackFont(fontName: String): String = when {
                userFont != null && !supplied(fontName) -> userFont
                available(fontName) -> fontName
                else -> replacementFont
            }

            for (index in lines.indices) {
                val original = lines[index]
                val trimmed = original.trim()
                if (trimmed.startsWith("[V4+ Styles]", ignoreCase = true) ||
                    trimmed.startsWith("[V4 Styles]", ignoreCase = true)
                ) {
                    inStylesSection = true
                    continue
                }
                if (trimmed.startsWith("[") && inStylesSection) inStylesSection = false

                if (inStylesSection && trimmed.startsWith("Format:", ignoreCase = true)) {
                    styleFormatFields = trimmed.substringAfter(":")
                        .split(",").map { it.trim().lowercase(java.util.Locale.ROOT) }
                    continue
                }

                if (inStylesSection && trimmed.startsWith("Style:", ignoreCase = true)) {
                    val prefixLength = original.indexOf("Style:", ignoreCase = true)
                    val prefix = if (prefixLength >= 0) original.substring(0, prefixLength) else ""
                    val parts = trimmed.substringAfter(":").split(",").toMutableList()
                    val fontIndex = styleFormatFields.indexOf("fontname").takeIf { it >= 0 } ?: 1
                    if (fontIndex in parts.indices) {
                        parts[fontIndex] = fallbackFont(parts[fontIndex])
                        lines[index] = prefix + "Style: " + parts.joinToString(",")
                    }
                }
            }

            // Inline \fn overrides can name a missing font even when the base style is valid.
            val fnPattern = Regex("\\\\fn([^\\\\}]+)")
            for (index in lines.indices) {
                val line = lines[index]
                if (!line.contains("\\fn")) continue
                lines[index] = fnPattern.replace(line) { match ->
                    "\\fn" + fallbackFont(match.groupValues[1])
                }
            }

            return lines.joinToString("\n")
        } catch (e: Exception) {
            Log.w(TAG, "Error sanitizing ASS styles for fallback", e)
            return assContent
        }
    }

    /**
     * Advanced ASS/SSA helper. Replaces ONLY the font names (Style lines + inline \\fn tags) that
     * nobody provides - not the video/subtitle, not the sub_fonts cache and not the device - with
     * the fallback font (the font the user selected in the normal subtitle editor, otherwise the
     * bundled Go Noto). Fonts that are available are never touched, and line endings, positions,
     * colours, tags and animations are preserved exactly.
     *
     * Returns null when nothing needed replacing, so callers can keep playing the original track.
     */
    fun replaceMissingFontsKeepingLayout(context: Context, assContent: String): String? {
        if (assContent.isBlank()) return null
        if (!assContent.contains("[V4+ Styles]", ignoreCase = true) &&
            !assContent.contains("[V4 Styles]", ignoreCase = true)
        ) return null

        val replacement = selectedFontRenderName?.takeIf { it.isNotBlank() } ?: BUNDLED_FALLBACK_FONT_FAMILY
        val cache = mutableMapOf<String, Boolean>()
        var changed = false

        fun ok(bare: String): Boolean {
            if (bare.isBlank()) return true
            return cache.getOrPut(bare.lowercase(java.util.Locale.ROOT)) {
                // Cheapest / cached lookups first; isFontAvailable re-reads font name tables.
                findFontFile(context, bare) != null ||
                    isSuppliedByCurrentVideo(context, bare) ||
                    isFontAvailable(context, bare)
            }
        }

        fun fixed(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return raw
            val vertical = trimmed.startsWith("@")
            val bare = trimmed.removePrefix("@")
            if (ok(bare)) return raw
            changed = true
            return raw.replace(trimmed, (if (vertical) "@" else "") + replacement)
        }

        val lines = assContent.split("\n").toMutableList()
        var inStyles = false
        var format = emptyList<String>()
        val fnPattern = Regex("\\\\fn([^\\\\}]+)")
        for (i in lines.indices) {
            val original = lines[i]
            val t = original.trim()
            if (t.startsWith("[")) {
                inStyles = t.startsWith("[V4+ Styles]", ignoreCase = true) ||
                    t.startsWith("[V4 Styles]", ignoreCase = true)
                continue
            }
            if (inStyles && t.startsWith("Format:", ignoreCase = true)) {
                format = t.substringAfter(":").split(",").map { it.trim().lowercase(java.util.Locale.ROOT) }
                continue
            }
            if (inStyles && t.startsWith("Style:", ignoreCase = true)) {
                val idx = original.indexOf("Style:", ignoreCase = true)
                val head = original.substring(0, idx + 6)
                val parts = original.substring(idx + 6).split(",").toMutableList()
                val fontIndex = format.indexOf("fontname").takeIf { it >= 0 } ?: 1
                if (fontIndex in parts.indices) {
                    val before = parts[fontIndex]
                    val after = fixed(before)
                    if (after != before) {
                        parts[fontIndex] = after
                        lines[i] = head + parts.joinToString(",")
                    }
                }
                continue
            }
            if (!inStyles && original.contains("\\fn")) {
                lines[i] = fnPattern.replace(original) { m -> "\\fn" + fixed(m.groupValues[1]) }
            }
        }
        return if (changed) lines.joinToString("\n") else null
    }

    /**
     * Imports a custom user-uploaded font file (e.g. .ttf, .otf, .ttc) into the MPV fonts directory.
     */
    fun importFontFromUri(context: Context, uri: android.net.Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "custom_font_${System.currentTimeMillis()}.ttf"
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                }
            } catch (_: Throwable) {}

            val destDir = getSubFontsDir(context)
            val destFile = File(destDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                markUserUploadedFont(context, fileName)
                Log.d(TAG, "Successfully imported font: $fileName (${destFile.length()} bytes)")
                fileName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import font from URI: $uri", e)
            null
        }
    }

    /**
     * Returns only user-uploaded fonts. The bundled fallback and generated system aliases
     * are intentionally not shown as removable custom fonts.
     */
    fun importFontsFromTree(context: Context, treeUri: android.net.Uri): Int {
        var imported = 0
        try {
            val resolver = context.contentResolver
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            )
            val projection = arrayOf(
                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                val fontsDir = getSubFontsDir(context)
                while (cursor.moveToNext()) {
                    val id = if (idIndex >= 0) cursor.getString(idIndex) else null
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                    if (id.isNullOrBlank() || name.isNullOrBlank()) continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in setOf("ttf", "otf", "ttc", "woff", "woff2")) continue
                    val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    resolver.openInputStream(docUri)?.use { input ->
                        File(fontsDir, name).outputStream().use { output -> input.copyTo(output) }
                    }
                    if (File(fontsDir, name).length() > 0L) {
                        markUserUploadedFont(context, name)
                        imported++
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to import fonts from selected directory", t)
        }
        return imported
    }

    /**
     * Extracts NameID 4 (Full Font Name) from TTF/OTF metadata, which contains
     * the original exact font name (e.g., "Arial Bold", "Bleach 111 Font Regular").
     */
    fun getFullFontName(bytes: ByteArray): String? {
        try {
            if (bytes.size < 12) return null
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
            val numTables = buffer.getShort(4).toInt() and 0xFFFF
            if (numTables <= 0 || numTables > 200) return null

            var nameTableOffset = -1
            var nameTableLength = -1
            for (i in 0 until numTables) {
                val recordPos = 12 + i * 16
                if (recordPos + 16 > bytes.size) break
                val tag = buffer.getInt(recordPos)
                if (tag == 0x6E616D65) { // 'name'
                    nameTableOffset = buffer.getInt(recordPos + 8)
                    nameTableLength = buffer.getInt(recordPos + 12)
                    break
                }
            }
            if (nameTableOffset !in 0 until bytes.size || nameTableLength <= 6 ||
                nameTableOffset + nameTableLength > bytes.size
            ) return null

            val count = buffer.getShort(nameTableOffset + 2).toInt() and 0xFFFF
            val stringOffset = buffer.getShort(nameTableOffset + 4).toInt() and 0xFFFF
            val storageOffset = nameTableOffset + stringOffset

            val byNameId = mutableMapOf<Int, String>()
            for (i in 0 until count) {
                val nrPos = nameTableOffset + 6 + i * 12
                if (nrPos + 12 > bytes.size) break
                val platformId = buffer.getShort(nrPos).toInt() and 0xFFFF
                val encodingId = buffer.getShort(nrPos + 2).toInt() and 0xFFFF
                val nameId = buffer.getShort(nrPos + 6).toInt() and 0xFFFF
                val length = buffer.getShort(nrPos + 8).toInt() and 0xFFFF
                val strOffset = buffer.getShort(nrPos + 10).toInt() and 0xFFFF
                if (nameId != 4 && nameId != 1 && nameId != 6) continue
                val actualPos = storageOffset + strOffset
                if (actualPos + length > bytes.size || length <= 0) continue
                val str = if (platformId == 0 || platformId == 3 || (platformId == 2 && encodingId == 1)) {
                    String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_16BE)
                } else {
                    String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_8)
                }.trim()
                if (str.isBlank()) continue
                val isPreferredPlatform = platformId == 0 || platformId == 3
                val existing = byNameId[nameId]
                if (existing == null || isPreferredPlatform) {
                    byNameId[nameId] = str
                }
            }
            return byNameId[4] ?: byNameId[1] ?: byNameId[6]
        } catch (_: Throwable) {
            return null
        }
    }

    /**
     * Returns the original font name: prefers metadata Full Font Name (NameID 4),
     * then original filename without extension.
     */
    fun getOriginalFontName(file: File): String {
        try {
            if (file.exists() && file.isFile && file.length() in 100..(30 * 1024 * 1024)) {
                val full = getFullFontName(file.readBytes())
                if (!full.isNullOrBlank()) return full
            }
        } catch (_: Throwable) {}
        return file.nameWithoutExtension
    }

    fun getInstalledFonts(context: Context, includeVideoExtracted: Boolean = true): List<File> {
        val destDir = getSubFontsDir(context)
        val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")
        val bundled = BUNDLED_FALLBACK_FONT_FILE.lowercase()
        val generatedAliases = setOf(
            "default.ttf", "sans-serif.ttf", "sans.ttf", "arial.ttf",
            "arial-regular.ttf", "roboto.ttf", "roboto-regular.ttf"
        )
        return destDir.listFiles()?.filter { file ->
            val lower = file.name.lowercase()
            val baseName = file.nameWithoutExtension.lowercase(java.util.Locale.ROOT)
            val isFont = file.isFile && file.extension.lowercase() in fontExtensions &&
                !file.name.startsWith(SYS_LINK_PREFIX) && lower != bundled && lower !in generatedAliases &&
                baseName !in NON_SUBTITLE_ASSET_FONT_BASENAMES
            if (!isFont) return@filter false
            if (includeVideoExtracted) true else isUserUploadedFont(context, file)
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    /**
     * Returns the exact font name that libass should receive for a selected file.
     * libass/fontconfig supports matching ASS font references against the font's
     * Full Font Name, which is important when several uploaded files intentionally
     * share the same internal family name. Passing only the family collapses those
     * files into whichever face fontconfig happens to select.
     */
    fun getRenderFontName(file: File): String = getOriginalFontName(file)

    /**
     * Deletes a custom font from the fonts directory.
     */
    fun deleteCustomFont(context: Context, fileName: String): Boolean {
        val destDir = getSubFontsDir(context)
        val file = File(destDir, fileName)
        if (!file.exists() || !isUserUploadedFont(context, file)) return false
        val deleted = file.delete()
        if (deleted) unmarkFont(context, fileName)
        return deleted
    }

    /**
     * Deletes all custom imported fonts from the fonts directory.
     */
    fun deleteAllCustomFonts(context: Context): Int {
        val fonts = getInstalledFonts(context, includeVideoExtracted = false)
        var deletedCount = 0
        for (font in fonts) {
            try {
                if (font.exists() && font.delete()) {
                    unmarkFont(context, font.name)
                    deletedCount++
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete font: ${font.name}", e)
            }
        }
        return deletedCount
    }

    // The app's OWN UI/branding font (res/font/viaura.otf, used by ViauraTypography.kt
    // for on-screen text) must never end up in the subtitle font picker. It used to be swept
    // in here along with everything else under assets/fonts/, so it silently appeared in
    // "Settings → Subtitles → Fonts" and in the in-player font list as if the user had
    // uploaded it themselves — with no way to tell it apart from a real custom subtitle font.
    private val NON_SUBTITLE_ASSET_FONT_BASENAMES = setOf("viaura")

    private fun copyAssetFonts(context: Context, destDir: File) {
        val assetManager = context.assets
        val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")

        fun scanAssetPath(path: String) {
            try {
                val list = assetManager.list(path) ?: return
                for (item in list) {
                    val subPath = if (path.isEmpty()) item else "$path/$item"
                    val ext = item.substringAfterLast('.', "").lowercase()
                    if (ext in fontExtensions) {
                        val baseName = item.substringBeforeLast('.').lowercase(java.util.Locale.ROOT)
                        if (baseName in NON_SUBTITLE_ASSET_FONT_BASENAMES) continue
                        val destFile = File(destDir, item)
                        if (!destFile.exists() || destFile.length() == 0L) {
                            assetManager.open(subPath).use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } else if (!item.contains('.')) {
                        scanAssetPath(subPath)
                    }
                }
            } catch (_: Exception) {}
        }

        scanAssetPath("fonts")
        scanAssetPath("")
    }

    private fun copyVideoFolderFonts(videoPath: String, destDir: File) {
        try {
            val videoFile = File(videoPath)
            val parentDir = videoFile.parentFile ?: return
            if (!parentDir.exists() || !parentDir.isDirectory) return

            val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")
            val candidateDirs = mutableListOf(parentDir)

            val subFolders = listOf("fonts", "Fonts", "font", "Font", "FONTS")
            for (sub in subFolders) {
                val f = File(parentDir, sub)
                if (f.exists() && f.isDirectory) {
                    candidateDirs.add(f)
                }
            }

            for (dir in candidateDirs) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile) {
                        val ext = file.extension.lowercase()
                        if (ext in fontExtensions) {
                            val dest = File(destDir, file.name)
                            if (!dest.exists() || dest.length() != file.length()) {
                                file.copyTo(dest, overwrite = true)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error copying video folder fonts", e)
        }
    }

    /**
     * Extracts [Fonts] embedded attachments from ASS script and decodes them into destDir.
     */
    fun extractEmbeddedAssFonts(assContent: String, destDir: File, context: Context? = null) {
        if (!assContent.contains("[Fonts]", ignoreCase = true)) return

        val lines = assContent.lines()
        var inFontsSection = false
        var currentFontName: String? = null
        val encodedDataBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[Fonts]", ignoreCase = true)) {
                inFontsSection = true
                continue
            }

            if (inFontsSection && trimmed.startsWith("[")) {
                if (currentFontName != null && encodedDataBuilder.isNotEmpty()) {
                    saveEncodedFontFile(currentFontName, encodedDataBuilder.toString(), destDir, context)
                }
                break
            }

            if (inFontsSection) {
                if (trimmed.startsWith("fontname:", ignoreCase = true)) {
                    if (currentFontName != null && encodedDataBuilder.isNotEmpty()) {
                        saveEncodedFontFile(currentFontName, encodedDataBuilder.toString(), destDir, context)
                        encodedDataBuilder.clear()
                    }
                    currentFontName = trimmed.substringAfter("fontname:").trim()
                } else if (currentFontName != null && trimmed.isNotEmpty()) {
                    encodedDataBuilder.append(trimmed)
                }
            }
        }

        if (inFontsSection && currentFontName != null && encodedDataBuilder.isNotEmpty()) {
            saveEncodedFontFile(currentFontName, encodedDataBuilder.toString(), destDir, context)
        }
    }

    private fun saveEncodedFontFile(fontName: String, encodedData: String, destDir: File, context: Context? = null) {
        try {
            val fontFile = File(destDir, fontName)
            if (fontFile.exists() && fontFile.length() > 0) return

            val decodedBytes = decodeAssFontData(encodedData)
            if (decodedBytes != null && decodedBytes.isNotEmpty()) {
                fontFile.writeBytes(decodedBytes)
                context?.let { markVideoExtractedFont(it, fontName) }
                Log.d(TAG, "Extracted embedded ASS font: $fontName (${decodedBytes.size} bytes)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save embedded font: $fontName", e)
        }
    }

    /**
     * Decodes ASS UUencoded font attachments.
     */
    private fun decodeAssFontData(data: String): ByteArray? {
        val cleanData = data.replace("\r", "").replace("\n", "").replace(" ", "")
        if (cleanData.isEmpty()) return null

        return try {
            val bytes = ArrayList<Byte>(cleanData.length * 3 / 4)
            var i = 0
            val len = cleanData.length
            while (i < len) {
                val chunkLen = minOf(4, len - i)
                val c1 = (cleanData[i].code - 33) and 0x3F
                val c2 = if (chunkLen > 1) (cleanData[i + 1].code - 33) and 0x3F else 0
                val c3 = if (chunkLen > 2) (cleanData[i + 2].code - 33) and 0x3F else 0
                val c4 = if (chunkLen > 3) (cleanData[i + 3].code - 33) and 0x3F else 0

                val val24 = (c1 shl 18) or (c2 shl 12) or (c3 shl 6) or c4

                bytes.add(((val24 shr 16) and 0xFF).toByte())
                if (chunkLen > 2) bytes.add(((val24 shr 8) and 0xFF).toByte())
                if (chunkLen > 3) bytes.add((val24 and 0xFF).toByte())

                i += 4
            }
            bytes.toByteArray()
        } catch (e: Exception) {
            try {
                Base64.decode(cleanData, Base64.DEFAULT)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun cleanupLegacyGeneratedFonts(destDir: File) {
        try {
            val generatedAliases = setOf(
                "default.ttf", "sans-serif.ttf", "sans.ttf", "arial.ttf",
                "arial-regular.ttf", "roboto.ttf", "roboto-regular.ttf"
            )
            val systemFontNames = mutableSetOf<String>()
            val systemDirs = listOf(
                File("/system/fonts"),
                File("/system/font"),
                File("/product/fonts"),
                File("/apex/com.android.runtime/fonts")
            )
            systemDirs.forEach { dir ->
                try {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile) systemFontNames.add(file.name.lowercase(java.util.Locale.ROOT))
                    }
                } catch (_: Throwable) {}
            }

            destDir.listFiles()?.forEach { file ->
                if (!file.isFile) return@forEach
                val lower = file.name.lowercase(java.util.Locale.ROOT)
                if (lower in generatedAliases || lower in systemFontNames) {
                    try { file.delete() } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Checks if a specified font name is available in video-embedded/sub_fonts/system locations.
     * Follows strict matching priority:
     * 1. Video's own embedded/attached fonts (MKV attachments, track-list attachments)
     * 2. sub_fonts cache directory
     * 3. System font directories (/system/fonts, /product/fonts, /apex/...)
     * 4. Returns false if not found in 1-3 (triggering fallback to bundled Go Noto Current-Regular in sanitizeAssForFallback).
     */
    fun isFontAvailable(context: Context, fontName: String): Boolean {
        if (fontName.isBlank() || fontName.equals(BUNDLED_FALLBACK_FONT_FAMILY, ignoreCase = true)) {
            return true
        }

        val cleanName = fontName.trim()
        val lowerName = cleanName.lowercase(java.util.Locale.ROOT)
        val normalizedTarget = normalizeFontName(cleanName)

        // 1. PRIORITY 1: Check Video's own embedded / attached fonts
        for (embedded in videoEmbeddedFontNames) {
            if (embedded.equals(lowerName, ignoreCase = true) ||
                embedded.equals(normalizedTarget, ignoreCase = true) ||
                normalizeFontName(embedded) == normalizedTarget
            ) {
                return true
            }
        }

        // 2. PRIORITY 2: Check sub_fonts cache directory
        val fontsDir = getSubFontsDir(context)
        val files = fontsDir.listFiles() ?: emptyArray()
        for (file in files) {
            if (file.name.startsWith(SYS_LINK_PREFIX)) continue // system links: covered by step 3 below
            val nameWithoutExt = file.nameWithoutExtension
            if (nameWithoutExt.equals(cleanName, ignoreCase = true) ||
                file.name.equals(cleanName, ignoreCase = true) ||
                normalizeFontName(nameWithoutExt) == normalizedTarget
            ) {
                return true
            }
            // Also check font family names extracted from TTF/OTF name table
            val parsedNames = extractFontNamesFromTtfFile(file)
            for (parsed in parsedNames) {
                if (parsed.equals(cleanName, ignoreCase = true) || normalizeFontName(parsed) == normalizedTarget) {
                    return true
                }
            }
        }

        // 3. PRIORITY 3: Check system font directories directly
        val sysDirs = listOf(
            File("/system/fonts"),
            File("/system/font"),
            File("/product/fonts"),
            File("/apex/com.android.runtime/fonts")
        )
        for (sysDir in sysDirs) {
            if (sysDir.exists() && sysDir.isDirectory) {
                val sysFiles = sysDir.listFiles() ?: continue
                for (file in sysFiles) {
                    val nameWithoutExt = file.nameWithoutExtension
                    if (nameWithoutExt.equals(cleanName, ignoreCase = true) ||
                        file.name.equals(cleanName, ignoreCase = true) ||
                        normalizeFontName(nameWithoutExt) == normalizedTarget
                    ) {
                        return true
                    }
                }
            }
        }

        // 3b. The phone's fonts by their real family / full name (the file name often differs, e.g.
        //     "Noto Sans Tamil" lives in NotoSansTamil-VF.ttf), incl. Android's own installed-font list.
        if (findSystemFaceByName(cleanName) != null) return true

        // 4. PRIORITY 4: Not available in video embedded, sub_fonts, or system fonts -> return false
        return false
    }

    /** A font of this phone whose family / full name is [fontName] (case and spacing tolerant), or null. */
    private fun findSystemFaceByName(fontName: String): SystemFallbackFont? {
        val clean = fontName.trim().removePrefix("@")
        if (clean.isBlank()) return null
        val norm = normalizeFontName(clean)
        fun matches(f: SystemFallbackFont): Boolean =
            f.name.equals(clean, ignoreCase = true) || f.fullName.equals(clean, ignoreCase = true) ||
                normalizeFontName(f.name) == norm || (f.fullName != null && normalizeFontName(f.fullName) == norm)
        systemFallbacks.firstOrNull { matches(it) }?.let { return it }
        return try { scanAllSystemFaces().firstOrNull { matches(it) } } catch (_: Throwable) { null }
    }

    /**
     * ASS/SSA: every font the script names that the video / subtitle folder / imported fonts do not supply is
     * looked up on the phone first (same font = same look) and handed to libass directly. Only what the
     * phone does not have is left for Google Fonts, and only what Google does not have uses the default font.
     */
    private fun linkRequestedPhoneFonts(context: Context, assContent: String) {
        val families = try {
            GoogleFontsManager.collectFontRequests(assContent).map { it.family }.distinct().take(40)
        } catch (_: Throwable) { return }
        for (family in families) {
            try {
                if (isSuppliedByCurrentVideo(context, family)) continue
                if (findFontFile(context, family) != null) continue
                val face = findSystemFaceByName(family) ?: continue
                ensureVisibleToLibass(context, face)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Extracts embedded font attachments (.ttf, .otf, .ttc, etc.) from an MKV video container into destDir
     * and registers all extracted font family names into videoEmbeddedFontNames.
     */
    fun extractEmbeddedMkvFonts(videoPath: String, destDir: File, context: Context? = null) {
        try {
            val videoFile = File(videoPath)
            if (!videoFile.exists() || !videoFile.canRead() || videoFile.length() < 100) return

            java.io.RandomAccessFile(videoFile, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                // EBML Header 0x1A 0x45 0xDF 0xA3
                if (magic[0] != 0x1A.toByte() || magic[1] != 0x45.toByte() || magic[2] != 0xDF.toByte() || magic[3] != 0xA3.toByte()) {
                    return
                }

                val fileLen = raf.length()
                val scanLimit = minOf(fileLen, 4L * 1024L * 1024L) // Matroska headers and font attachments reside in initial header segment
                val bufferSize = 64 * 1024
                val buffer = ByteArray(bufferSize)
                var filePos = 0L

                while (filePos < scanLimit) {
                    raf.seek(filePos)
                    val bytesRead = raf.read(buffer)
                    if (bytesRead <= 8) break

                    var i = 0
                    while (i < bytesRead - 4) {
                        // AttachedFile element ID in Matroska: 0x61 0xA7
                        if (buffer[i] == 0x61.toByte() && buffer[i + 1] == 0xA7.toByte()) {
                            val elementAbsPos = filePos + i
                            try {
                                parseAndSaveMkvAttachedFile(raf, elementAbsPos, destDir, context)
                            } catch (_: Throwable) {}
                            i += 2
                        } else {
                            i++
                        }
                    }
                    filePos += (bytesRead - 8)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error extracting MKV embedded fonts from $videoPath", t)
        }
    }

    private fun parseAndSaveMkvAttachedFile(raf: java.io.RandomAccessFile, elementPos: Long, destDir: File, context: Context? = null) {
        raf.seek(elementPos + 2) // Skip 0x61 0xA7
        val size = readEbmlVint(raf)
        if (size <= 0 || size > 50 * 1024 * 1024) return // Guard against corrupt sizes (max 50MB per font)

        val endPos = raf.filePointer + size
        var fileName: String? = null
        var fileMimeType: String? = null
        var fontData: ByteArray? = null

        while (raf.filePointer < endPos) {
            val childId = readEbmlId(raf) ?: break
            val childSize = readEbmlVint(raf)
            if (childSize < 0 || raf.filePointer + childSize > endPos + 16) break

            when (childId) {
                0x466E -> { // FileName (UTF-8 string)
                    val nameBytes = ByteArray(childSize.toInt())
                    raf.readFully(nameBytes)
                    fileName = String(nameBytes, java.nio.charset.StandardCharsets.UTF_8).trim()
                }
                0x4660 -> { // FileMimeType (ASCII string)
                    val mimeBytes = ByteArray(childSize.toInt())
                    raf.readFully(mimeBytes)
                    fileMimeType = String(mimeBytes, java.nio.charset.StandardCharsets.US_ASCII).trim()
                }
                0x465C -> { // FileData (Binary font data)
                    if (childSize in 100..(30 * 1024 * 1024)) {
                        fontData = ByteArray(childSize.toInt())
                        raf.readFully(fontData)
                    } else {
                        raf.skipBytes(childSize.toInt())
                    }
                }
                else -> {
                    raf.skipBytes(childSize.toInt())
                }
            }
        }

        if (fontData != null && (!fileName.isNullOrBlank() || !fileMimeType.isNullOrBlank())) {
            val validFontExts = listOf(".ttf", ".otf", ".ttc", ".woff", ".woff2")
            val isFont = (fileName != null && validFontExts.any { fileName.endsWith(it, ignoreCase = true) }) ||
                    (fileMimeType != null && (fileMimeType.contains("font", ignoreCase = true) ||
                            fileMimeType.contains("truetype", ignoreCase = true) ||
                            fileMimeType.contains("opentype", ignoreCase = true)))

            if (isFont) {
                val targetFileName = fileName?.takeIf { it.isNotBlank() }
                    ?: "embedded_font_${System.currentTimeMillis() % 10000}.ttf"
                val outFile = File(destDir, targetFileName)
                if (!outFile.exists() || outFile.length() != fontData.size.toLong()) {
                    try {
                        FileOutputStream(outFile).use { it.write(fontData) }
                    } catch (_: Throwable) {}
                }

                // Register file name and without extension
                context?.let { markVideoExtractedFont(it, targetFileName) }
                registerVideoEmbeddedFont(targetFileName)
                val withoutExt = targetFileName.substringBeforeLast('.')
                registerVideoEmbeddedFont(withoutExt)

                // Extract TTF/OTF internal font family & full names from binary bytes
                val familyNames = extractFontNamesFromTtfBytes(fontData)
                for (name in familyNames) {
                    registerVideoEmbeddedFont(name)
                }
            }
        }
    }

    private fun readEbmlId(raf: java.io.RandomAccessFile): Int? {
        val b0 = raf.read()
        if (b0 == -1) return null
        var id = b0
        val numBytes = when {
            (b0 and 0x80) != 0 -> 1
            (b0 and 0x40) != 0 -> 2
            (b0 and 0x20) != 0 -> 3
            (b0 and 0x10) != 0 -> 4
            else -> 1
        }
        for (k in 1 until numBytes) {
            val b = raf.read()
            if (b == -1) return null
            id = (id shl 8) or (b and 0xFF)
        }
        return id
    }

    private fun readEbmlVint(raf: java.io.RandomAccessFile): Long {
        val b0 = raf.read()
        if (b0 == -1) return -1L
        var mask = 0x80
        var length = 1
        while (length <= 8 && (b0 and mask) == 0) {
            mask = mask shr 1
            length++
        }
        if (length > 8) return -1L
        var value = (b0 and (mask xor 0xFF)).toLong()
        for (i in 1 until length) {
            val b = raf.read()
            if (b == -1) return -1L
            value = (value shl 8) or (b.toLong() and 0xFFL)
        }
        return value
    }

    /**
     * Parses TrueType / OpenType font table records to extract declared Font Family (Name ID 1)
     * and Full Font Name (Name ID 4).
     */
    fun extractFontNamesFromTtfBytes(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        try {
            if (bytes.size < 12) return names
            val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
            val numTables = buffer.getShort(4).toInt() and 0xFFFF
            if (numTables <= 0 || numTables > 200) return names

            var nameTableOffset = -1
            var nameTableLength = -1
            for (i in 0 until numTables) {
                val recordPos = 12 + i * 16
                if (recordPos + 16 > bytes.size) break
                val tag = buffer.getInt(recordPos)
                // Tag 'name' == 0x6E616D65
                if (tag == 0x6E616D65) {
                    nameTableOffset = buffer.getInt(recordPos + 8)
                    nameTableLength = buffer.getInt(recordPos + 12)
                    break
                }
            }

            if (nameTableOffset in 0 until bytes.size && nameTableLength > 6 && nameTableOffset + nameTableLength <= bytes.size) {
                val count = buffer.getShort(nameTableOffset + 2).toInt() and 0xFFFF
                val stringOffset = buffer.getShort(nameTableOffset + 4).toInt() and 0xFFFF
                val storageOffset = nameTableOffset + stringOffset

                for (i in 0 until count) {
                    val nrPos = nameTableOffset + 6 + i * 12
                    if (nrPos + 12 > bytes.size) break
                    val platformId = buffer.getShort(nrPos).toInt() and 0xFFFF
                    val encodingId = buffer.getShort(nrPos + 2).toInt() and 0xFFFF
                    val nameId = buffer.getShort(nrPos + 6).toInt() and 0xFFFF
                    val length = buffer.getShort(nrPos + 8).toInt() and 0xFFFF
                    val strOffset = buffer.getShort(nrPos + 10).toInt() and 0xFFFF

                    // nameId 1 = Font Family Name, nameId 4 = Full Font Name, nameId 6 = PostScript Name
                    if (nameId == 1 || nameId == 4 || nameId == 6) {
                        val actualPos = storageOffset + strOffset
                        if (actualPos + length <= bytes.size && length > 0) {
                            val str = if (platformId == 0 || platformId == 3 || (platformId == 2 && encodingId == 1)) {
                                String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_16BE)
                            } else {
                                String(bytes, actualPos, length, java.nio.charset.StandardCharsets.UTF_8)
                            }.trim()
                            if (str.isNotBlank() && !names.contains(str)) {
                                names.add(str)
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return names
    }

    /**
     * Reads a TTF/OTF font file and returns its declared font family/full names.
     */
    fun extractFontNamesFromTtfFile(file: File): List<String> {
        return try {
            if (file.exists() && file.isFile && file.length() in 100..(30 * 1024 * 1024)) {
                extractFontNamesFromTtfBytes(file.readBytes())
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

