package com.example.util

import android.content.Context

/**
 * Settings > Network: the HLS stream handling and whether subtitles of pasted links are loaded. Plain
 * SharedPreferences, readable from anywhere (resolver, player) without going through the view model.
 */
object NetworkSettings {
    private const val PREFS = "viaura_network_settings"
    private const val KEY_HLS = "hls_stream_handling"
    private const val KEY_SUBS = "download_media_subtitles"
    private const val KEY_AUTO_SUBS = "include_auto_generated_subtitles"
    private const val KEY_MEDIA_LANGUAGE = "media_language"
    private const val KEY_MEDIA_AUTO_SUBTITLE = "media_auto_select_subtitle"
    private const val KEY_MEDIA_PLAY_QUALITY = "media_play_quality"
    private const val KEY_MEDIA_DOWNLOAD_QUALITY = "media_download_quality"

    /** Quality value meaning "no fixed quality": best available (play: Auto). */
    const val QUALITY_AUTO = 0
    /** Download quality value meaning "use the same quality as the play quality". */
    const val QUALITY_SAME_AS_PLAY = -1

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hlsHandlingEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_HLS, true)
    fun setHlsHandlingEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_HLS, v).apply()

    fun downloadSubtitles(c: Context): Boolean = prefs(c).getBoolean(KEY_SUBS, true)
    fun setDownloadSubtitles(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_SUBS, v).apply()

    fun autoSubtitles(c: Context): Boolean = prefs(c).getBoolean(KEY_AUTO_SUBS, true)
    fun setAutoSubtitles(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_AUTO_SUBS, v).apply()

    // ---- Settings > Network > Media settings ----

    /** Preferred language of the Media providers ("" = off): ISO code like "hin" / "eng". Drives the audio, subtitle and server choice. */
    fun mediaLanguage(c: Context): String = prefs(c).getString(KEY_MEDIA_LANGUAGE, "") ?: ""
    fun setMediaLanguage(c: Context, v: String) = prefs(c).edit().putString(KEY_MEDIA_LANGUAGE, v).apply()

    /** Select the subtitle track of the preferred language automatically (Media providers). */
    fun mediaAutoSubtitle(c: Context): Boolean = prefs(c).getBoolean(KEY_MEDIA_AUTO_SUBTITLE, true)
    fun setMediaAutoSubtitle(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_MEDIA_AUTO_SUBTITLE, v).apply()

    /** Video height the Media providers should play at (0 = Auto / best). */
    fun mediaPlayQuality(c: Context): Int = prefs(c).getInt(KEY_MEDIA_PLAY_QUALITY, QUALITY_AUTO)
    fun setMediaPlayQuality(c: Context, v: Int) = prefs(c).edit().putInt(KEY_MEDIA_PLAY_QUALITY, v).apply()

    /** Video height that is pre-selected in the download sheet: [QUALITY_SAME_AS_PLAY] (default), [QUALITY_AUTO] (best) or a height. */
    fun mediaDownloadQuality(c: Context): Int = prefs(c).getInt(KEY_MEDIA_DOWNLOAD_QUALITY, QUALITY_SAME_AS_PLAY)
    fun setMediaDownloadQuality(c: Context, v: Int) = prefs(c).edit().putInt(KEY_MEDIA_DOWNLOAD_QUALITY, v).apply()

    /** The height the download sheet should pre-select (0 = best available). */
    fun effectiveDownloadQuality(c: Context): Int {
        val d = mediaDownloadQuality(c)
        return if (d == QUALITY_SAME_AS_PLAY) mediaPlayQuality(c) else d
    }
}
