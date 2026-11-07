package com.example.player

import android.content.Context
import java.io.File

/**
 * Anime4K real-time upscaling / restoring shaders for mpv (`glsl-shaders`).
 *
 * The `.glsl` files are NOT committed: CI downloads them into `app/src/main/assets/anime4k/` (see the
 * "Fetch Anime4K shaders" step in `.github/workflows/build-apk.yml`). They are copied once to
 * `filesDir/anime4k/` because mpv needs real file paths.
 *
 * Mode id format: `off` or `<a|b|c>_<fast|hq>`. The shader chains follow the official Anime4K mpv templates.
 */
object Anime4KShaders {

    const val OFF = "off"
    private const val ASSET_DIR = "anime4k"

    private const val CLAMP = "Anime4K_Clamp_Highlights.glsl"
    private const val DOWN_X2 = "Anime4K_AutoDownscalePre_x2.glsl"
    private const val DOWN_X4 = "Anime4K_AutoDownscalePre_x4.glsl"

    /** Letter of the preset: A = restore + upscale (most 1080p anime), B = softer restore, C = upscale + denoise. */
    val letters = listOf("a", "b", "c")

    fun isOn(mode: String): Boolean = chain(mode).isNotEmpty()

    fun modeId(letter: String, hq: Boolean): String = "${letter}_${if (hq) "hq" else "fast"}"

    fun letterOf(mode: String): String = mode.substringBefore('_').takeIf { it in letters } ?: "a"

    fun isHq(mode: String): Boolean = mode.endsWith("_hq")

    /** Ordered shader file names for [mode]; empty for off / unknown ids. */
    fun chain(mode: String): List<String> {
        val letter = mode.substringBefore('_')
        val hq = mode.endsWith("_hq")
        if (letter !in letters || !(hq || mode.endsWith("_fast"))) return emptyList()
        val size = if (hq) "VL" else "M"
        val first = when (letter) {
            "a" -> "Anime4K_Restore_CNN_$size.glsl"
            "b" -> "Anime4K_Restore_CNN_Soft_$size.glsl"
            else -> null
        }
        val upscale = when (letter) {
            "c" -> "Anime4K_Upscale_Denoise_CNN_x2_$size.glsl"
            else -> "Anime4K_Upscale_CNN_x2_$size.glsl"
        }
        val last = if (hq) "Anime4K_Upscale_CNN_x2_M.glsl" else "Anime4K_Upscale_CNN_x2_S.glsl"
        return listOfNotNull(CLAMP, first, upscale, DOWN_X2, DOWN_X4, last)
    }

    /** True when every shader of [mode] is bundled in this build's assets. */
    fun isAvailable(context: Context, mode: String): Boolean {
        val files = chain(mode)
        if (files.isEmpty()) return false
        return try {
            val present = context.applicationContext.assets.list(ASSET_DIR)?.toSet() ?: emptySet()
            files.all { it in present }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Value for mpv's `glsl-shaders` option ("" = shaders off). Copies the needed files to private storage first.
     * Returns "" when the mode is off or its files are not bundled.
     */
    fun resolveMpvValue(context: Context, mode: String): String {
        val files = chain(mode)
        if (files.isEmpty()) return ""
        return try {
            val app = context.applicationContext
            val dir = File(app.filesDir, ASSET_DIR).apply { mkdirs() }
            val paths = files.map { name ->
                val target = File(dir, name)
                if (!target.exists() || target.length() == 0L) {
                    app.assets.open("$ASSET_DIR/$name").use { input ->
                        val tmp = File(dir, "$name.tmp")
                        tmp.outputStream().use { input.copyTo(it) }
                        if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
                    }
                }
                target.absolutePath
            }
            paths.joinToString(":")
        } catch (_: Throwable) {
            ""
        }
    }
}
