package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentSkyBlueStrong
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LightTextSecondary
import com.example.ui.theme.Neutral
import com.example.util.YtmSession
import com.example.util.media.PluginBrowserState

/**
 * Sign-in of the YouTube Music account. The SAME in-app browser the providers use is shown; as soon as the Google
 * login has finished (the YouTube Music page carries the account cookies) the account is connected by itself and the
 * browser closes.
 */
internal object YtmLogin {
    private const val LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"
    private val main = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    fun start(context: Context, onConnected: () -> Unit) {
        YtmSession.attach(context)
        val wv = WebView(context)
        var done = false
        var requestId = 0L
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        runCatching { cm.setAcceptThirdPartyCookies(wv, true) }
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // Google refuses the sign-in inside an embedded WebView ("wv" in the user agent)
            userAgentString = userAgentString.replace("; wv", "").replace(Regex("Version/\\d+\\.\\d+ "), "")
        }

        fun close() {
            main.post {
                PluginBrowserState.hide(requestId)
                runCatching { wv.stopLoading(); wv.loadUrl("about:blank") }
            }
        }

        fun check(url: String?) {
            if (done) return
            val host = runCatching { android.net.Uri.parse(url).host }.getOrNull().orEmpty()
            if (!host.endsWith("music.youtube.com")) return
            val cookie = cm.getCookie("https://music.youtube.com").orEmpty()
            val signedIn = cookie.contains("SAPISID=") || cookie.contains("__Secure-3PAPISID=")
            if (!signedIn) return
            done = true
            YtmSession.connect(cookie)
            runCatching { cm.flush() }
            close()
            main.post { onConnected() }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { check(url) }
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) { check(url) }
        }
        requestId = PluginBrowserState.show(
            wv,
            "Connect YouTube Music",
            "Sign in with your Google account. It connects by itself as soon as the login is done.",
            onDone = { check(wv.url); if (!done) close() },
            onCancel = { close() }
        )
        wv.loadUrl(LOGIN_URL)
    }
}

/** Same look as the "Storage access is restricted" banner: icon, text, Ignore and Connect. */
@Composable
internal fun YtmConnectCard(isDark: Boolean, onIgnore: () -> Unit, onConnect: () -> Unit) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val bg = if (isDark) Neutral.D1E293B.copy(alpha = 0.9f) else Color(0xFFEFF6FF).copy(alpha = 0.95f)
    val border = AccentSkyBlue.copy(alpha = if (isDark) 0.5f else 0.35f)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .shadow(6.dp, shape, ambientColor = Color.Black.copy(alpha = 0.15f), spotColor = Color.Black.copy(alpha = 0.20f))
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Image(painter = painterResource(R.drawable.viaura_ytmusic), contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Connect YouTube Music", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryText)
                    Text(
                        "Your playlists and liked songs, plus YouTube's own recommendations on this home",
                        fontSize = 11.5.sp, color = secondaryText
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onIgnore() }.padding(horizontal = 14.dp, vertical = 6.dp)
                ) { Text("Ignore", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = secondaryText) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AccentGradient).clickable { onConnect() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }
    }
}

/** "More" tab: connect / disconnect the YouTube Music account (also reachable after Ignore was tapped). */
@Composable
internal fun YtmMoreTab(isDark: Boolean, connected: Boolean, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    val primaryText = if (isDark) DarkTextPrimary else LightTextPrimary
    val secondaryText = if (isDark) DarkTextSecondary else LightTextSecondary
    val bg = if (isDark) Neutral.D1E293B.copy(alpha = 0.9f) else Color(0xFFEFF6FF).copy(alpha = 0.95f)
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(shape).background(bg)
                .border(1.dp, AccentSkyBlue.copy(alpha = 0.4f), shape).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painter = painterResource(R.drawable.viaura_ytmusic), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("YouTube Music", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryText)
                Text(
                    if (connected) "Connected. Your library and recommendations are on Home, Albums, Artists and Playlists."
                    else "Not connected. Connect to bring your playlists, liked songs and YouTube's recommendations here.",
                    fontSize = 12.sp, color = secondaryText
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (connected) Color(0x33E02424) else AccentSkyBlueStrong)
                .clickable { if (connected) onDisconnect() else onConnect() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (connected) "Disconnect" else "Connect YouTube Music",
                fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                color = if (connected) Color(0xFFE02424) else Color.White
            )
        }
    }
}
