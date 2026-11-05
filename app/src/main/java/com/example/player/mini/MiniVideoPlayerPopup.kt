package com.example.ui.components

import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.example.player.VideoMiniPlayerManager

/**
 * Picture-in-Picture window of the video mini player: the whole window is just the video (the app UI is hidden behind it).
 * The native video view is moved here while the system PiP window is open and handed back to the mini player afterwards.
 */
@Composable
fun MiniPlayerPipSurface(enabled: Boolean) {
    val inPip by com.example.util.PipModeState.isInPictureInPicture
    val session by VideoMiniPlayerManager.session.collectAsState()
    val s = session
    if (!enabled || !inPip || s == null) return
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        AndroidView(
            factory = { ctx ->
                android.widget.FrameLayout(ctx).also { host ->
                    val v = s.view
                    // The surface is re-created for the PiP window: hold playback for it so the sound never cuts or jumps back.
                    s.controller.markSeamlessHandOver(noDuck = true)
                    (v.parent as? ViewGroup)?.removeView(v)
                    v.outlineProvider = ViewOutlineProvider.BACKGROUND
                    v.clipToOutline = false
                    host.addView(v, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            },
            onRelease = { host -> host.removeAllViews() },
            modifier = Modifier.fillMaxSize()
        )
    }
}
