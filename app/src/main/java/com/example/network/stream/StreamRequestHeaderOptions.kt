package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-stream HTTP options handed to mpv right before a stream is opened. The player calls [forUrl];
 * null means "no special options" (mpv then keeps the app's normal User-Agent).
 */
object StreamRequestOptions {
    /**
     * One item of mpv's comma separated list option (http-header-fields). Values that contain commas (Cookie, Accept,
     * Accept-Language ...) used to be rewritten to "%2C", which changed the header the site received; mpv's own
     * length-prefixed form "%<bytes>%<text>" keeps them exactly as the provider gave them.
     */
    fun mpvListItem(text: String): String =
        if (text.indexOf(',') < 0 && !text.startsWith("%")) text else "%" + text.toByteArray(Charsets.UTF_8).size + "%" + text

    data class Options(val userAgent: String?, val referrer: String?, val headerFields: String?)

    fun forUrl(url: String): Options? {
        val m = StreamEntryStore.get(url) ?: return null
        val ua = m.userAgent?.takeIf { it.isNotBlank() }
        val ref = m.referrer?.takeIf { it.isNotBlank() }
        val fields = m.extraHeaders.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .joinToString(",") { mpvListItem("${it.key}: ${it.value}") }
            .takeIf { it.isNotBlank() }
        if (ua == null && ref == null && fields == null) return null
        return Options(ua, ref, fields)
    }
}
