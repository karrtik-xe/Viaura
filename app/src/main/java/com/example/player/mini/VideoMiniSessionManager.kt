package com.example.player

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import com.example.ui.screens.VideoItem
import com.example.ui.screens.getPlayableFilePath
import com.example.util.PlaybackHistoryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * One video that keeps playing after the user leaves the full-screen video player (Settings > Player >
 * General > "Video mini player"). The player's mpv controller AND its native video view are parked here, so
 * the picture/audio never restart; the mini player (MinimizedVideoPlayer) shows the same view in a small
 * area and the full-screen player takes both back when the mini player is tapped.
 */
class VideoMiniSession(
    val controller: MpvPlayerController,
    val view: VideoPlayerMpvView,
    val video: VideoItem,
    val playlist: List<VideoItem>,
    /** The player screen's own "loaded" key of [video]; lets the screen adopt the session without reloading. */
    val loadedKey: String?
)

object VideoMiniPlayerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _session = MutableStateFlow<VideoMiniSession?>(null)
    val session: StateFlow<VideoMiniSession?> = _session.asStateFlow()

    /** Set while the mini player is the one playing (false once it is closed or expanded). */
    val isActive: Boolean get() = _session.value != null

    private var appContext: Context? = null
    private var saveJob: Job? = null
    private var eofJob: Job? = null
    private var audioWatchJob: Job? = null

    /** Only local files are parked: online / network / archive / provider streams need the full player's own session. */
    fun isEligible(video: VideoItem?, allowOnline: Boolean = false): Boolean {
        if (video == null) return false
        if (video.isNetworkStream || video.isDirectStream) return false
        if (video.isOnline) return allowOnline
        if (video.path.startsWith("http://", true) || video.path.startsWith("https://", true)) return false
        if (com.example.util.archive.ArchiveStore.isArchivePath(video.path)) return false
        if (com.example.util.media.PluginQueuedEpisodes.isQueued(video.path)) return false
        return true
    }

    private var pendingParkJob: Job? = null
    private var pendingPark: Pair<Context, VideoMiniSession>? = null

    /**
     * Back from the player: the slide-down starts first and the hand-over to the mini player happens [delayMs] later
     * (once the slide is over), so none of its work (view detach, service start, notification) lands on the animation.
     * Any other call into this manager completes a pending hand-over first, so nothing is ever lost or released twice.
     */
    fun parkAfterExit(context: Context, session: VideoMiniSession, delayMs: Long) {
        flushPendingPark()
        pendingPark = context.applicationContext to session
        pendingParkJob = scope.launch {
            delay(delayMs)
            flushPendingPark()
        }
    }

    private fun flushPendingPark() {
        val p = pendingPark ?: return
        pendingPark = null
        pendingParkJob?.cancel(); pendingParkJob = null
        park(p.first, p.second)
    }

    /** Called by the player screen when the user leaves it: playback goes on in the mini player. */
    fun park(context: Context, session: VideoMiniSession) {
        if (pendingPark != null) flushPendingPark()
        appContext = context.applicationContext
        // Anything that was parked before (another video) is shut down first.
        val previous = _session.value
        if (previous != null && previous.controller !== session.controller) release(previous)

        // The surface is re-created while the view moves to the mini player: the file must never be reloaded for it.
        session.controller.markSeamlessHandOver(noDuck = false)
        // The native view belongs to the player screen's layout until now: detach it so the mini player can host it.
        (session.view.parent as? ViewGroup)?.removeView(session.view)

        // A mini video and an audio track never play together.
        try {
            if (AudioPlaybackManager.currentTrack.value != null) AudioPlaybackManager.closeAndStop(context.applicationContext)
        } catch (_: Throwable) {}

        _session.value = session
        // mpv keeps the audio going when the screen turns off, but only a foreground service keeps the process alive.
        VideoMiniPlaybackService.start(context.applicationContext)
        startWatchers(session)
    }

    /** The full-screen player takes the session back (mini player tapped). Returns null if nothing matches. */
    fun takeForExpand(): VideoMiniSession? {
        flushPendingPark()
        val s = _session.value ?: return null
        s.controller.markSeamlessHandOver(noDuck = true)
        stopWatchers()
        _session.value = null
        VideoMiniPlaybackService.stop(appContext ?: return s)
        (s.view.parent as? ViewGroup)?.removeView(s.view)
        return s
    }

    /**
     * The full-screen player calls this when it opens [video]: the parked session is handed over when it is the same
     * video (no reload, the picture just grows), otherwise the parked video is closed because another one starts.
     */
    fun claimFor(video: VideoItem?): VideoMiniSession? {
        flushPendingPark()
        val s = _session.value ?: return null
        val same = video != null &&
            ((s.video.path.isNotBlank() && s.video.path == video.path) || (s.video.id == video.id && s.video.uri == video.uri))
        if (same && s.controller.isAlive) return takeForExpand()
        close()
        return null
    }

    fun togglePlayPause() {
        val s = _session.value ?: return
        try {
            if (s.controller.isPlaying.value) s.controller.pause() else s.controller.play()
        } catch (_: Throwable) {}
        VideoMiniPlaybackService.refresh(appContext ?: return)
    }

    fun close() {
        flushPendingPark()
        val s = _session.value ?: return
        stopWatchers()
        persist(s)
        _session.value = null
        appContext?.let { VideoMiniPlaybackService.stop(it) }
        release(s)
    }

    /** Next video of the queue, opened inside the mini player itself. */
    fun skipNext(): Boolean {
        val s = _session.value ?: return false
        val ctx = appContext ?: return false
        val idx = s.playlist.indexOfFirst { it.path == s.video.path }
        val next = s.playlist.getOrNull(idx + 1) ?: return false
        if (!isEligible(next)) return false
        val path = getPlayableFilePath(ctx, next.uri, next.path)
        if (path.isBlank() || !(File(path).exists() || path.startsWith("fd://"))) return false
        persist(s)
        val saved = try {
            PlaybackHistoryManager.getSavedPosition(ctx, next.id, next.path, next.uri.toString(), next.displayName)
        } catch (_: Throwable) { 0L }
        try {
            PlaybackHistoryManager.markOpened(ctx, next.id, next.path, next.uri.toString(), next.displayName, next.durationMs)
        } catch (_: Throwable) {}
        com.example.util.MediaSeenManager.markSeen(ctx, next.path, next.id)
        s.controller.setOnlineStream(false)
        s.controller.loadVideo(path, if (saved > 500L) saved else 0L)
        s.controller.play()
        val key = "${next.id}_${path}_${idx + 1}"
        val updated = VideoMiniSession(s.controller, s.view, next, s.playlist, key)
        stopWatchers()
        _session.value = updated
        startWatchers(updated)
        VideoMiniPlaybackService.refresh(ctx)
        return true
    }

    private fun persist(s: VideoMiniSession) {
        val ctx = appContext ?: return
        try {
            val pos = s.controller.currentPositionMs.value
            val dur = s.controller.durationMs.value.takeIf { it > 0L } ?: s.video.durationMs
            if (pos >= 0L) {
                PlaybackHistoryManager.savePosition(
                    context = ctx,
                    videoId = s.video.id,
                    path = s.video.path,
                    positionMs = pos,
                    durationMs = dur,
                    uriString = s.video.uri.toString(),
                    title = s.video.displayName,
                    forceDiskWrite = true
                )
            }
        } catch (_: Throwable) {}
    }

    private fun release(s: VideoMiniSession) {
        try { s.controller.pause() } catch (_: Throwable) {}
        (s.view.parent as? ViewGroup)?.removeView(s.view)
        try { s.controller.release() } catch (_: Throwable) {}
    }

    private fun startWatchers(s: VideoMiniSession) {
        stopWatchers()
        // Resume point is saved every few seconds, so closing the app from the mini player never loses the position.
        saveJob = scope.launch {
            while (isActive) {
                delay(5000L)
                persist(s)
            }
        }
        // End of the video: continue with the next one of the queue, otherwise just stay paused on the last frame.
        eofJob = scope.launch {
            var handled = false
            s.controller.eofReached.collect { eof ->
                if (eof && !handled) {
                    handled = true
                    if (!skipNext()) {
                        try { s.controller.pause() } catch (_: Throwable) {}
                    }
                } else if (!eof) handled = false
            }
        }
        // Audio started somewhere else (music player): the mini video steps aside.
        audioWatchJob = scope.launch {
            AudioPlaybackManager.isPlaying.drop(1).collect { playing ->
                if (playing) close()
            }
        }
    }

    private fun stopWatchers() {
        saveJob?.cancel(); saveJob = null
        eofJob?.cancel(); eofJob = null
        audioWatchJob?.cancel(); audioWatchJob = null
    }

    /** Shown in the notification and mini player. */
    fun currentTitle(): String = _session.value?.video?.displayName ?: "Video"
    fun currentIsPlaying(): Boolean = _session.value?.controller?.isPlaying?.value ?: false
}
