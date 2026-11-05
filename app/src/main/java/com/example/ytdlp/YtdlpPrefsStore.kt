/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Ported from mpvRx (https://github.com/gyrolet/mpvRx), AGPL-3.0-or-later.
 * Adapted for Viaura: plain SharedPreferences instead of mpvRx's preference store.
 */

package com.example.ytdlp

import android.content.Context
import android.content.SharedPreferences

/** One stored value; `get()`/`set()` keep the ported yt-dlp code identical to mpvRx. */
class YtdlPref<T>(
  private val prefs: SharedPreferences,
  private val key: String,
  private val default: T,
  private val read: (SharedPreferences, String, T) -> T,
  private val write: (SharedPreferences.Editor, String, T) -> Unit,
) {
  fun get(): T = runCatching { read(prefs, key, default) }.getOrDefault(default)

  fun set(value: T) {
    prefs.edit().also { write(it, key, value) }.apply()
  }
}

/** yt-dlp settings (format/quality/network). Same keys and defaults as mpvRx's YtdlPreferences. */
class YtdlPreferences(
  context: Context,
) {
  private val prefs = context.applicationContext.getSharedPreferences("viaura_ytdlp_settings", Context.MODE_PRIVATE)

  private fun string(key: String, default: String = "") =
    YtdlPref(prefs, key, default, { p, k, d -> p.getString(k, d) ?: d }, { e, k, v -> e.putString(k, v) })

  private fun int(key: String, default: Int) =
    YtdlPref(prefs, key, default, { p, k, d -> p.getInt(k, d) }, { e, k, v -> e.putInt(k, v) })

  private fun bool(key: String, default: Boolean) =
    YtdlPref(prefs, key, default, { p, k, d -> p.getBoolean(k, d) }, { e, k, v -> e.putBoolean(k, v) })

  private inline fun <reified E : Enum<E>> enum(key: String, default: E) =
    YtdlPref(
      prefs,
      key,
      default,
      { p, k, d -> p.getString(k, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: d },
      { e, k, v -> e.putString(k, v.name) },
    )

  val customUserAgent = string("ytdl_custom_user_agent")
  val referer = string("ytdl_referer")
  val cookiesFile = string("ytdl_cookies_file")
  val proxy = string("ytdl_proxy")
  val extractorArgs = string("ytdl_extractor_args")
  val geoBypass = bool("ytdl_geo_bypass", false)

  /** "stable" or "nightly": which release the Update button in Settings > Network installs. */
  val updateChannel = string("ytdl_update_channel", "stable")

  companion object {
    @Volatile private var instance: YtdlPreferences? = null

    fun get(context: Context): YtdlPreferences =
      instance ?: synchronized(this) {
        instance ?: YtdlPreferences(context.applicationContext).also { instance = it }
      }
  }
}
