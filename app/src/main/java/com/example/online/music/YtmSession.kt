package com.example.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The YouTube Music account the listener connected through the in-app browser. Only the web session cookies are kept
 * (private app storage); with them the library, the personal home and the recommendations of that account are read.
 */
object YtmSession {
    private const val PREFS = "viaura_ytm_session"
    private const val K_COOKIE = "cookie"
    private const val K_IGNORED = "ignored"

    @Volatile private var app: Context? = null

    /** Compose reads this: any change of the connection state (connect / disconnect / ignore) recomposes the screens. */
    var version by mutableStateOf(0)
        private set

    fun attach(context: Context) { if (app == null) app = context.applicationContext }

    private fun prefs() = app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cookie(): String? = prefs()?.getString(K_COOKIE, null)?.takeIf { it.isNotBlank() }

    fun isConnected(): Boolean = cookie() != null

    /** "Ignore" was tapped: the connect card never comes back (the More tab still offers Connect). */
    fun isIgnored(): Boolean = prefs()?.getBoolean(K_IGNORED, false) == true

    fun ignoreForever() {
        prefs()?.edit()?.putBoolean(K_IGNORED, true)?.apply()
        version++
    }

    fun connect(cookie: String) {
        prefs()?.edit()?.putString(K_COOKIE, cookie)?.apply()
        version++
    }

    fun disconnect() {
        prefs()?.edit()?.remove(K_COOKIE)?.apply()
        try {
            val cm = android.webkit.CookieManager.getInstance()
            listOf("https://music.youtube.com", "https://www.youtube.com", "https://accounts.google.com", "https://google.com").forEach { u ->
                cm.getCookie(u)?.split(';')?.forEach { c -> cm.setCookie(u, c.substringBefore('=').trim() + "=; Max-Age=0") }
            }
            cm.flush()
        } catch (_: Throwable) {}
        version++
    }
}
