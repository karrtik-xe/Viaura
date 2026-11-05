package com.example.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.media.PluginBrowserState

/**
 * Shows the in-app browser of a provider (Cloudflare "Verify you are human", sign in, link account ...) over the whole app.
 * Header = title + description given by the provider, body = the live page, footer = Reload / Done.
 */
@Composable
fun PluginBrowserHost() {
    val req by PluginBrowserState.current.collectAsState()
    val r = req ?: return
    BackHandler { r.onCancel() }
    Dialog(
        onDismissRequest = { r.onCancel() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).padding(14.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.82f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF17181A))
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(r.title.ifBlank { "Verify you are human" }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            r.description.ifBlank { "Complete the check below, then tap Done." },
                            color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text("✕", color = Color(0xFFC1C4C9), fontSize = 22.sp, modifier = Modifier.clickable { r.onCancel() }.padding(8.dp))
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            val wv: WebView = r.webView
                            (wv.parent as? ViewGroup)?.removeView(wv)
                            wv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            wv
                        },
                        onRelease = { v -> (v.parent as? ViewGroup)?.removeView(v) }
                    )
                }
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)).clickable { try { r.webView.reload() } catch (_: Throwable) {} }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) { Text("Reload", color = Color.White, fontSize = 14.sp) }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF8FD3E8)).clickable { r.onDone() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Done", color = Color(0xFF0B2A33), fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
