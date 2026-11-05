package com.example.util.media

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * The in-app browser of `providerContext.openWebView` (like Vega's WafWebViewDialog): when a site needs a tap / captcha
 * ("Verify you are human") or a login, the SAME WebView that [PluginWebViewSolver] works with is shown in a dialog
 * ([com.example.ui.components.PluginBrowserHost]) so the user can finish the check inside the app, then tap Done.
 */
object PluginBrowserState {
    class Request(
        val id: Long,
        val title: String,
        val description: String,
        val webView: WebView,
        val onDone: () -> Unit,
        val onCancel: () -> Unit
    )

    val current = MutableStateFlow<Request?>(null)
    private val ids = AtomicLong(1L)

    init {
        // CloudStream plugins (.cs3): their WebViewResolver / CloudflareKiller use the same in-app browser
        com.lagradost.cloudstream3.network.WebViewResolver.showBrowser = { wv, host, cancel ->
            show(wv, "Verify you are human", "Complete the check or sign in on $host, then tap Done. It closes by itself when a check is passed.", onDone = cancel, onCancel = cancel)
        }
        com.lagradost.cloudstream3.network.WebViewResolver.hideBrowser = { id -> hide(id) }
    }

    fun show(webView: WebView, title: String, description: String, onDone: () -> Unit, onCancel: () -> Unit): Long {
        val id = ids.getAndIncrement()
        current.value = Request(id, title, description, webView, onDone, onCancel)
        return id
    }

    fun hide(id: Long) {
        val c = current.value
        if (c != null && c.id == id) current.value = null
    }
}
