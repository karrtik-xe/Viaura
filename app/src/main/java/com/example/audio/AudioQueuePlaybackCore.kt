package com.example.player

import android.content.ContentUris
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import com.example.ui.screens.AudioTrackItem
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsProvider
import com.example.domain.lyrics.LyricsSourceType
import com.example.utils.media.LyricsUtils
import com.example.util.LyricsTextCodec
import com.example.util.OnlineLyricsService
import com.example.util.OnlineTrackVideoIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import com.example.util.AudioPlaybackHistoryManager

enum class AudioRepeatMode(val label: String) {
    OFF("Off"),
    ALL("Repeat All"),
    ONE("Repeat One")
}

/**
 * Global Singleton Audio Playback Engine for the entire application.
 * Manages background playback, queue management, seeking, shuffle, repeat,
 * playback speed, and synchronized state between Full-Screen and Minimized Player.
 */
object AudioPlaybackManager {
    private const val TAG = "AudioPlaybackManager"
    /** How many upcoming online tracks are cached in the background while the current one plays. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    // ONE listener object for the whole life of the player. A new listener per request makes the system treat every
    // request as a different client: the previous one then receives AUDIOFOCUS_LOSS and pauses the track that has just
    // started (a random cut right after tapping next / a new song).
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change -> handleAudioFocusChange(change) }
    @Volatile private var audioFocusHeld: Boolean = false
    // Short sounds (notification, navigation prompt) only lower the music instead of pausing it.
    @Volatile private var duckedForFocus: Boolean = false
    private var volumeAppliedTo: MediaPlayer? = null
    private var volumeApplied: Float = -1f
    private var applicationContext: Context? = null

    // Real-Time Hardware-Accelerated Audio Effects (Zero-glitch live equalizer)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private var isEqEnabled: Boolean = true
    private var eqGain60: Float = 0f
    private var eqGain230: Float = 0f
    private var eqGain910: Float = 0f
    private var eqGain3600: Float = 0f
    private var eqGain14000: Float = 0f
    private var eqVolumeBoostDb: Float = 0f
    private var isHeadphoneSurroundEnabled: Boolean = false

    // State Flows
    private val _currentTrack = MutableStateFlow<AudioTrackItem?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

    // True while lyrics for the current track are being searched (embedded / .lrc / online).
    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics = _isFetchingLyrics.asStateFlow()

    private val _lyricsModel = MutableStateFlow<Lyrics?>(null)
    val lyricsModel = _lyricsModel.asStateFlow()

    private val _activeLyricsProvider = MutableStateFlow<LyricsProvider?>(null)
    val activeLyricsProvider = _activeLyricsProvider.asStateFlow()

    private val _lyricsSourceType = MutableStateFlow(LyricsSourceType.LOCAL)
    val lyricsSourceType = _lyricsSourceType.asStateFlow()

    // Keep embedded/imported and online models separate; AudioTrackItem.lyricsText remains the
    // backwards-compatible LRC/TXT representation used by the audio-library cache.
    private val embeddedLyricsByPath = ConcurrentHashMap<String, Lyrics>()
    private val onlineLyricsByPath = ConcurrentHashMap<String, Lyrics>()
    private val onlineProviderByPath = ConcurrentHashMap<String, LyricsProvider>()
    @Volatile private var preferredLyricsProvider: LyricsProvider? = null
    private var lyricsJob: Job? = null

    private var streamResolutionJob: Job? = null
    private var streamPreloadJob: Job? = null

    // Standby player for the NEXT online track, prepared for stream playback.
    private var standbyPlayer: MediaPlayer? = null
    private var standbyTrackId: Long? = null
    private var standbyPrepared: Boolean = false

    // Incremented for every (non-seamless) player start; lets a seamless hand-off detect that it
    // has been superseded by a newer start.
    @Volatile
    private var startToken: Long = 0L
    private val preloadedOnlineStreams = ConcurrentHashMap<String, String>()

    // Guards MediaPlayer callbacks from an older, rapidly-replaced player instance.
    // This prevents late onCompletion/onError events from restarting the wrong track.
    private var playbackGeneration: Long = 0L
    @Volatile
    private var mediaPlayerPrepared: Boolean = false

    // Exponential-backoff network retry and stall tracking
    @Volatile
    private var trackRetryCount: Int = 0
    @Volatile
    private var lastRetriedTrackId: Long? = null

    // Audio focus & user pause tracking (prevents microphone / keyboard voice typing from auto-starting music)
    @Volatile
    private var userExplicitlyPaused: Boolean = false

    @Volatile
    private var resumeOnFocusGain: Boolean = false

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<AudioTrackItem>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private var originalQueue: List<AudioTrackItem> = emptyList()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.ALL)
    val repeatMode = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private val _isFullScreenOpen = MutableStateFlow(false)
    val isFullScreenOpen = _isFullScreenOpen.asStateFlow()

    private val _isMiniPlayerVisible = MutableStateFlow(false)
    val isMiniPlayerVisible = _isMiniPlayerVisible.asStateFlow()

    // A-B Loop State Flows
    private val _loopPointA = MutableStateFlow<Long?>(null)
    val loopPointA = _loopPointA.asStateFlow()

    private val _loopPointB = MutableStateFlow<Long?>(null)
    val loopPointB = _loopPointB.asStateFlow()

    private val _isABLoopActive = MutableStateFlow(false)
    val isABLoopActive = _isABLoopActive.asStateFlow()

    fun setLoopPointA(timeMs: Long) {
        val dur = _durationMs.value.coerceAtLeast(1L)
        val clampedA = timeMs.coerceIn(0L, dur)
        _loopPointA.value = clampedA
        val currentB = _loopPointB.value
        if (currentB != null && currentB <= clampedA) {
            _loopPointB.value = (clampedA + 2000L).coerceAtMost(dur)
        }
        _isABLoopActive.value = (_loopPointA.value != null && _loopPointB.value != null)
    }

    fun setLoopPointB(timeMs: Long) {
        val dur = _durationMs.value.coerceAtLeast(1L)
        val clampedB = timeMs.coerceIn(0L, dur)
        val currentA = _loopPointA.value
        if (currentA == null) {
            _loopPointA.value = (clampedB - 2000L).coerceAtLeast(0L)
        } else if (clampedB <= currentA) {
            _loopPointB.value = (currentA + 2000L).coerceAtMost(dur)
        } else {
            _loopPointB.value = clampedB
        }
        _isABLoopActive.value = true
        val finalA = _loopPointA.value ?: 0L
        val finalB = _loopPointB.value ?: dur
        if (_currentPositionMs.value < finalA || _currentPositionMs.value >= finalB) {
            seekTo(finalA)
        }
    }

    fun toggleABLoop(currentPos: Long? = null) {
        val pos = currentPos ?: _currentPositionMs.value
        if (_loopPointA.value == null) {
            setLoopPointA(pos)
        } else if (_loopPointB.value == null) {
            setLoopPointB(pos)
        } else {
            clearABLoop()
        }
    }

    fun clearABLoop() {
        _loopPointA.value = null
        _loopPointB.value = null
        _isABLoopActive.value = false
    }

    fun adjustLoopPointA(deltaMs: Long) {
        val curA = _loopPointA.value ?: _currentPositionMs.value
        val maxLimit = (_loopPointB.value ?: _durationMs.value) - 500L
        val newA = (curA + deltaMs).coerceIn(0L, maxLimit.coerceAtLeast(0L))
        _loopPointA.value = newA
        _isABLoopActive.value = (_loopPointA.value != null && _loopPointB.value != null)
        seekTo(newA)
    }

    fun adjustLoopPointB(deltaMs: Long) {
        val curB = _loopPointB.value ?: _durationMs.value
        val minLimit = (_loopPointA.value ?: 0L) + 500L
        val newB = (curB + deltaMs).coerceIn(minLimit, _durationMs.value.coerceAtLeast(minLimit))
        _loopPointB.value = newB
        _isABLoopActive.value = (_loopPointA.value != null && _loopPointB.value != null)
    }

    fun jumpToPointA() {
        _loopPointA.value?.let { seekTo(it) }
    }

    fun openFullScreen() {
        _isFullScreenOpen.value = true
    }

    fun playRecentTrack(context: Context, resume: com.example.util.AudioPlaybackResume, startFromBeginning: Boolean = false): Boolean {
        val uri = try { if (resume.uriString.isNotBlank()) Uri.parse(resume.uriString) else Uri.EMPTY } catch (_: Throwable) { Uri.EMPTY }
        val track = AudioTrackItem(
            id = resume.id,
            uri = uri,
            title = resume.title.ifBlank { resume.path.substringAfterLast('/').ifBlank { "Recent Audio" } },
            artist = resume.artist,
            album = resume.album,
            durationMs = resume.durationMs,
            path = resume.path,
            sizeBytes = 0L,
            format = if (resume.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)) "STREAM" else resume.path.substringAfterLast('.', ""),
            dateModified = resume.lastWatchedTimestamp / 1000L,
            thumbnailUrl = resume.thumbnailUrl
        )
        if (track.path.isBlank() && track.uri == Uri.EMPTY) return false

        val targetPosition = if (startFromBeginning) 0L else resume.positionMs

        // The same track is already loaded (mini / full audio player): keep it playing from where it is. Reloading it from
        // the saved resume point (which lags behind) is what made the sound cut and jump back a little.
        val loadedNow = _currentTrack.value
        if (!startFromBeginning && loadedNow != null && loadedNow.path.isNotBlank() &&
            loadedNow.path == track.path && mediaPlayer != null
        ) {
            _isMiniPlayerVisible.value = true
            if (!_isPlaying.value) resume(context.applicationContext)
            openFullScreen()
            return true
        }

        // If online track, re-resolve stream URL because CDN tokens expire
        if (track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) || !resume.videoUrl.isNullOrBlank()) {
            val videoUrl = resume.videoUrl ?: track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
            // Legacy disk-cache lookup is disabled; online tracks stay stream-based.
            if (track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) &&
                OnlineAudioCache.cachedFile(
                    context.applicationContext,
                    track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                ) != null
            ) {
                playTrack(context, track, listOf(track), targetPosition, continueIfCurrent = false)
                openFullScreen()
                return true
            }
            scope.launch(Dispatchers.IO) {
                val freshStreamUrl = com.example.util.OnlineMusicService.resolveStreamUrl(videoUrl, track.title, track.artist)
                val activeTrack = if (freshStreamUrl != null) {
                    track.copy(uri = Uri.parse(freshStreamUrl))
                } else {
                    track
                }
                withContext(Dispatchers.Main) {
                    playTrack(context, activeTrack, listOf(activeTrack), targetPosition, continueIfCurrent = false)
                    openFullScreen()
                }
            }
            return true
        }

        playTrack(context, track, listOf(track), targetPosition, continueIfCurrent = false)
        openFullScreen()
        return true
    }

    fun collapseToMiniPlayer() {
        _isFullScreenOpen.value = false
        if (_currentTrack.value != null) {
            _isMiniPlayerVisible.value = true
        }
    }

    fun closeAndStop(context: Context? = null) {
        applicationContext?.let { ctx ->
            AudioPlaybackHistoryManager.savePosition(ctx, _currentTrack.value, _currentPositionMs.value, _durationMs.value, force = true)
        }
        stop(context)
        _isFullScreenOpen.value = false
        _isMiniPlayerVisible.value = false
        _currentTrack.value = null
        clearABLoop()
    }

    /**
     * Removes an upcoming queue item without interrupting the currently playing track.
     * The active track is intentionally protected so a swipe cannot unexpectedly stop playback.
     */
    fun removeFromQueue(index: Int) {
        val currentQ = _queue.value.toMutableList()
        if (index !in currentQ.indices) return
        if (index == _currentIndex.value) return

        currentQ.removeAt(index)
        _queue.value = currentQ

        val currentIndex = _currentIndex.value
        _currentIndex.value = when {
            currentQ.isEmpty() -> 0
            index < currentIndex -> (currentIndex - 1).coerceAtLeast(0)
            else -> currentIndex.coerceIn(0, currentQ.lastIndex)
        }
        onQueueOrderChanged()
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentQ = _queue.value.toMutableList()
        if (fromIndex !in currentQ.indices || toIndex !in currentQ.indices || fromIndex == toIndex) return
        val currentTrackId = _currentTrack.value?.id

        val moved = currentQ.removeAt(fromIndex)
        currentQ.add(toIndex, moved)
        _queue.value = currentQ

        if (currentTrackId != null) {
            val newIdx = currentQ.indexOfFirst { it.id == currentTrackId }
            if (newIdx >= 0) {
                _currentIndex.value = newIdx
            }
        }
        onQueueOrderChanged()
    }

    fun setLyricsForCurrentTrack(lyrics: String) {
        val cur = _currentTrack.value ?: return
        val hasL = lyrics.isNotBlank()
        val updated = cur.copy(hasLyrics = hasL, lyricsText = lyrics)
        _currentTrack.value = updated
        val parsed = LyricsUtils.parseLyrics(lyrics, LyricsSourceType.LOCAL)
        if (parsed.isValid()) embeddedLyricsByPath[cur.path] = parsed else embeddedLyricsByPath.remove(cur.path)
        _lyricsModel.value = parsed.takeIf { it.isValid() }
        _lyricsSourceType.value = LyricsSourceType.LOCAL
        _activeLyricsProvider.value = null

        // Update in queue
        val currentQ = _queue.value.map { if (it.id == cur.id) updated else it }
        _queue.value = currentQ
        if (originalQueue.isNotEmpty()) {
            originalQueue = originalQueue.map { if (it.id == cur.id) updated else it }
        }
    }

    /**
     * Applies lyrics/metadata to the track [id] wherever it lives (current track and queue) without
     * touching anything else, so a late result can never overwrite newer state with a stale copy.
     */
    private fun applyTrackUpdate(id: Long, transform: (AudioTrackItem) -> AudioTrackItem) {
        val cur = _currentTrack.value
        if (cur != null && cur.id == id) _currentTrack.value = transform(cur)
        _queue.value = _queue.value.map { if (it.id == id) transform(it) else it }
        if (originalQueue.isNotEmpty()) {
            originalQueue = originalQueue.map { if (it.id == id) transform(it) else it }
        }
    }

    /**
     * Lyrics pipeline for local and online tracks:
     *  1. companion .lrc, embedded tags, companion .txt via the existing Viaura extractor
     *  2. when needed (or explicitly requested), MpvRx's full provider race and matcher
     *
     * Results are applied by track id via applyTrackUpdate so a late result cannot replace a newer
     * queue/current-track copy. Embedded/imported text remains in lyricsText when online lyrics
     * are explicitly previewed; the active structured model switches independently.
     */
    fun loadLyricsFor(
        context: Context,
        track: AudioTrackItem,
        forceOnline: Boolean = false,
        provider: LyricsProvider? = preferredLyricsProvider,
    ) {
        val appContext = context.applicationContext
        lyricsJob?.cancel()
        val isOnlineTrack = track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
        _isFetchingLyrics.value = false

        val embeddedSeed = embeddedLyricsByPath[track.path] ?:
            track.lyricsText.takeIf { it.isNotBlank() }?.let {
                LyricsUtils.parseLyrics(it, LyricsSourceType.LOCAL).takeIf(Lyrics::isValid)
            }
        if (embeddedSeed != null) embeddedLyricsByPath.putIfAbsent(track.path, embeddedSeed)
        val onlineSeed = onlineLyricsByPath[track.path]
        val initialModel = when {
            embeddedSeed?.isValid() == true -> embeddedSeed
            onlineSeed?.isValid() == true -> onlineSeed
            else -> null
        }
        val usingOnlineSeed = onlineSeed != null && initialModel === onlineSeed
        if (_currentTrack.value?.id == track.id) {
            _lyricsModel.value = initialModel
            _activeLyricsProvider.value = if (usingOnlineSeed) onlineProviderByPath[track.path] else null
            _lyricsSourceType.value = if (usingOnlineSeed) LyricsSourceType.ONLINE else (initialModel?.sourceType ?: LyricsSourceType.LOCAL)
        }

        // Lyrics for this track were already looked up in the background while the previous tracks played:
        // show them instantly instead of searching again (a normal search still runs when nothing was found).
        if (!forceOnline && lyricsPrefetched.contains(track.path)) {
            val ready = onlineSeed?.takeIf { it.isValid() } ?: embeddedSeed?.takeIf { it.isValid() }
            if (ready != null) {
                if (_currentTrack.value?.id == track.id) {
                    val isOnlineLyrics = ready === onlineSeed
                    _lyricsModel.value = ready
                    _activeLyricsProvider.value = if (isOnlineLyrics) onlineProviderByPath[track.path] else null
                    _lyricsSourceType.value = if (isOnlineLyrics) LyricsSourceType.ONLINE else ready.sourceType
                }
                return
            }
        }

        lyricsJob = scope.launch(Dispatchers.IO) {
            try {
                var title = track.title
                var artist = track.artist
                var durationMs = track.durationMs
                var album = track.album
                var hasLyrics = track.hasLyrics && track.lyricsText.isNotBlank()

                if (!isOnlineTrack && !forceOnline) {
                    try {
                        val extracted = AudioMetadataExtractor.extractFullMetadata(appContext, track)
                        extracted.title?.let { title = it }
                        extracted.artist?.let { artist = it }
                        if (extracted.durationMs > 0L) durationMs = extracted.durationMs
                        extracted.album?.let { album = it }
                        val found = extracted.lyrics
                        if (!found.isNullOrBlank()) {
                            val parsed = LyricsUtils.parseLyrics(found, LyricsSourceType.LOCAL)
                            if (parsed.isValid()) embeddedLyricsByPath[track.path] = parsed
                        }
                        withContext(Dispatchers.Main) {
                            applyTrackUpdate(track.id) { t ->
                                t.copy(
                                    title = extracted.title ?: t.title,
                                    artist = extracted.artist ?: t.artist,
                                    album = extracted.album ?: t.album,
                                    durationMs = if (extracted.durationMs > 0L) extracted.durationMs else t.durationMs,
                                    hasLyrics = if (!found.isNullOrBlank()) true else t.hasLyrics,
                                    lyricsText = if (!found.isNullOrBlank()) found else t.lyricsText
                                )
                            }
                            if (_currentTrack.value?.id == track.id && onlineLyricsByPath[track.path] == null) {
                                val parsed = found?.let { LyricsUtils.parseLyrics(it, LyricsSourceType.LOCAL) }
                                if (parsed?.isValid() == true) {
                                    _lyricsModel.value = parsed
                                    _lyricsSourceType.value = parsed.sourceType
                                    _activeLyricsProvider.value = null
                                }
                            }
                        }
                        if (!found.isNullOrBlank()) hasLyrics = true
                    } catch (e: Exception) {
                        Log.d("Lyrics", "Local metadata extraction failed quietly: ${e.message}")
                    }
                }

                // Properly timestamped sidecar/embedded lyrics win immediately. Plain embedded
                // text has no word clock, though, so try the provider race for a timestamped match
                // while retaining the local copy as a graceful offline fallback.
                val localTimedLyrics = (embeddedLyricsByPath[track.path] ?: embeddedSeed)
                    ?.synced?.isNotEmpty() == true
                if (hasLyrics && !forceOnline && localTimedLyrics) return@launch
                if (hasLyrics && !forceOnline) {
                    Log.d("Lyrics", "Local lyrics have no timestamps; looking for a synced provider match")
                }
                if (!forceOnline && !OnlineLyricsService.isAutoFetchEnabled(appContext)) {
                    Log.d("Lyrics", "Automatic online lyrics fetching is disabled")
                    return@launch
                }
                if (_currentTrack.value?.id != track.id) return@launch
                withContext(Dispatchers.Main) { _isFetchingLyrics.value = true }

                val result = OnlineLyricsService.fetch(
                    context = appContext,
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    album = album.takeIf { it.isNotBlank() },
                    videoId = OnlineTrackVideoIds.from(track.path),
                    mediaPath = track.path,
                    provider = provider,
                    // Prefer the per-provider cache when switching sources; a missing cache entry
                    // still triggers the network lookup inside LyricsRepository.
                    forceRefresh = false,
                    forceOnline = forceOnline,
                )
                val online = result.lyrics?.takeIf { it.isValid() }
                if (online != null) {
                    val remote = online.copy(areFromRemote = true, sourceType = LyricsSourceType.ONLINE)
                    onlineLyricsByPath[track.path] = remote
                    result.provider?.let { onlineProviderByPath[track.path] = it }
                    val text = LyricsTextCodec.toLrc(remote)
                    withContext(Dispatchers.Main) {
                        applyTrackUpdate(track.id) { t ->
                            // Do not erase a good embedded/sidecar/imported copy just because the
                            // user also asked to preview an online source.
                            if (t.lyricsText.isBlank() || !t.hasLyrics) {
                                t.copy(hasLyrics = true, lyricsText = text)
                            } else {
                                t
                            }
                        }
                        if (_currentTrack.value?.id == track.id) {
                            _lyricsModel.value = remote
                            _activeLyricsProvider.value = result.provider
                            _lyricsSourceType.value = LyricsSourceType.ONLINE
                        }
                    }
                } else {
                    // The repository may discover lyrics via MediaMetadataRetriever even when the
                    // library extractor did not expose them. Preserve that local result instead of
                    // mistaking it for a failed online search and leaving the panel empty.
                    val local = result.embeddedLyrics?.takeIf { it.isValid() }
                    if (local != null) {
                        val embedded = local.copy(areFromRemote = false, sourceType = result.selectedSource)
                        embeddedLyricsByPath[track.path] = embedded
                        val text = LyricsTextCodec.toLrc(embedded)
                        withContext(Dispatchers.Main) {
                            applyTrackUpdate(track.id) { t ->
                                if (t.lyricsText.isBlank() || !t.hasLyrics) t.copy(hasLyrics = true, lyricsText = text) else t
                            }
                            if (_currentTrack.value?.id == track.id) {
                                _lyricsModel.value = embedded
                                _activeLyricsProvider.value = null
                                _lyricsSourceType.value = embedded.sourceType
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d("Lyrics", "Lyrics lookup failed quietly: ${e.message}")
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    if (_currentTrack.value?.id == track.id) _isFetchingLyrics.value = false
                }
            }
        }
    }

    /** Re-run online lookup for the current track (the Search Online action). */
    fun searchLyricsOnline(context: Context) {
        val cur = _currentTrack.value ?: return
        loadLyricsFor(context, cur, forceOnline = true, provider = preferredLyricsProvider)
    }

    /** Provider selection is session-scoped, matching MpvRx; null restores automatic best-match mode. */
    fun searchLyricsWithProvider(context: Context, provider: LyricsProvider?) {
        preferredLyricsProvider = provider
        val cur = _currentTrack.value ?: return
        loadLyricsFor(context, cur, forceOnline = true, provider = provider)
    }

    /** Switch the displayed model without overwriting the underlying library lyrics string. */
    fun selectLyricsSource(sourceType: LyricsSourceType) {
        val cur = _currentTrack.value ?: return
        val path = cur.path
        val chosen = when (sourceType) {
            LyricsSourceType.ONLINE -> onlineLyricsByPath[path]
            LyricsSourceType.EMBEDDED, LyricsSourceType.LOCAL -> embeddedLyricsByPath[path]
                ?: cur.lyricsText.takeIf { it.isNotBlank() }?.let {
                    LyricsUtils.parseLyrics(it, sourceType).takeIf(Lyrics::isValid)
                }
        }
        if (chosen != null && chosen.isValid()) {
            if (sourceType == LyricsSourceType.EMBEDDED || sourceType == LyricsSourceType.LOCAL) {
                embeddedLyricsByPath[path] = chosen
                _activeLyricsProvider.value = null
            } else {
                _activeLyricsProvider.value = onlineProviderByPath[path]
            }
            _lyricsModel.value = chosen.copy(sourceType = sourceType)
            _lyricsSourceType.value = sourceType
        }
    }

    /** Current provider list used by the lyrics picker. */
    fun lyricsProviders(): List<LyricsProvider> = OnlineLyricsService.providers()

    fun updateQueue(newQueue: List<AudioTrackItem>) {
        if (newQueue.isEmpty()) return
        if (_isShuffle.value && originalQueue.isNotEmpty()) {
            // Shuffle is on: [newQueue] is the shuffled play order. Keep the unshuffled order intact
            // (so turning shuffle off restores it) and only merge the added/removed tracks into it.
            val newById = newQueue.associateBy { it.id }
            val oldIds = originalQueue.map { it.id }.toSet()
            val kept = originalQueue.mapNotNull { newById[it.id] }
            val added = newQueue.filter { it.id !in oldIds }
            originalQueue = kept + added
        } else {
            originalQueue = newQueue
        }
        val curId = _currentTrack.value?.id
        val idx = if (curId != null) newQueue.indexOfFirst { it.id == curId }.coerceAtLeast(0) else 0
        _queue.value = newQueue
        _currentIndex.value = idx
        // Recommendations / playlist items often arrive AFTER playback started: make sure the
        // upcoming track still gets pre-buffered.
        onQueueOrderChanged()
    }

    /** Ids of tracks the user explicitly queued with "Play next", most recent first. */
    private val pendingNextIds = mutableListOf<Long>()

    private fun consumePending(trackId: Long) {
        pendingNextIds.remove(trackId)
    }

    /** "Play next": [track] plays right after the current track, in shuffle and non-shuffle mode alike. */
    fun playNext(track: AudioTrackItem) {
        val cur = _currentTrack.value
        if (cur != null && cur.path == track.path) return
        val q = _queue.value.toMutableList()
        q.removeAll { it.path == track.path }
        val curIdx = q.indexOfFirst { it.id == cur?.id }
        q.add(if (curIdx >= 0) curIdx + 1 else 0, track)
        if (_isShuffle.value && originalQueue.isNotEmpty()) {
            val o = originalQueue.toMutableList()
            o.removeAll { it.path == track.path }
            val oi = o.indexOfFirst { it.id == cur?.id }
            o.add(if (oi >= 0) oi + 1 else 0, track)
            originalQueue = o
        } else {
            originalQueue = q
        }
        pendingNextIds.remove(track.id)
        pendingNextIds.add(0, track.id)
        _queue.value = q
        _currentIndex.value = if (curIdx >= 0) curIdx else _currentIndex.value.coerceIn(0, q.lastIndex)
        onQueueOrderChanged()
    }

    /** "Add to queue": [track] goes to the end of the queue. */
    fun playLast(track: AudioTrackItem) {
        val cur = _currentTrack.value
        if (cur != null && cur.path == track.path) return
        val q = _queue.value.toMutableList()
        q.removeAll { it.path == track.path }
        q.add(track)
        if (_isShuffle.value && originalQueue.isNotEmpty()) {
            val o = originalQueue.toMutableList()
            o.removeAll { it.path == track.path }
            o.add(track)
            originalQueue = o
        } else {
            originalQueue = q
        }
        pendingNextIds.remove(track.id)
        val curIdx = q.indexOfFirst { it.id == cur?.id }
        _queue.value = q
        _currentIndex.value = if (curIdx >= 0) curIdx else _currentIndex.value.coerceIn(0, q.lastIndex)
        onQueueOrderChanged()
    }

    fun playTrack(
        context: Context,
        track: AudioTrackItem,
        newQueue: List<AudioTrackItem> = emptyList(),
        startPositionMs: Long = 0L,
        /** Tapping the track that is already loaded (mini or full player) keeps playing from where it is instead of restarting. */
        continueIfCurrent: Boolean = true
    ) {
        val appContext = context.applicationContext
        applicationContext = appContext
        if (audioManager == null) {
            audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        }

        val loaded = _currentTrack.value
        if (continueIfCurrent && startPositionMs <= 0L && loaded != null && loaded.path.isNotBlank() &&
            loaded.path == track.path && mediaPlayer != null
        ) {
            // Same track already loaded: no reload, the position stays. Only the list it was opened from is adopted.
            if (newQueue.isNotEmpty() && !_isShuffle.value) {
                originalQueue = newQueue
                _queue.value = newQueue
                _currentIndex.value = newQueue.indexOfFirst { it.path == track.path }.coerceAtLeast(0)
            }
            _isMiniPlayerVisible.value = true
            if (!_isPlaying.value) resume(appContext)
            return
        }

        // Invalidate callbacks from the previous player before replacing it.
        playbackGeneration++
        mediaPlayerPrepared = false
        trackRetryCount = 0
        lastRetriedTrackId = track.id

        // Cancel previous stream resolution job if running
        streamResolutionJob?.cancel()
        streamResolutionJob = null

        // Stop progress tracking and kill previous player immediately to avoid overlapping audio
        stopProgressTracker()
        releaseAudioEffects()
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Throwable) {}
        mediaPlayer = null

        _isPlaying.value = false
        _isBuffering.value = true
        _currentPositionMs.value = startPositionMs.coerceAtLeast(0L)

        // Set or update queue
        val currentQ = if (newQueue.isNotEmpty()) newQueue else if (_queue.value.isNotEmpty()) _queue.value else listOf(track)
        val existingQ = _queue.value
        val sameTrackSet = existingQ.size == currentQ.size &&
            existingQ.map { it.id }.toSet() == currentQ.map { it.id }.toSet()

        if (_isShuffle.value && sameTrackSet && existingQ.any { it.id == track.id }) {
            // Tapped inside the already-shuffled queue (Up Next sheet etc.): keep the shuffled order
            // (and the user's "Play next" items) and just move the playhead.
            if (originalQueue.isEmpty()) originalQueue = existingQ
            _queue.value = existingQ
            _currentIndex.value = existingQ.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else {
            originalQueue = currentQ
            if (_isShuffle.value && currentQ.size > 1) {
                val shuffled = currentQ.toMutableList()
                val playingItem = shuffled.firstOrNull { it.id == track.id } ?: track
                shuffled.remove(playingItem)
                shuffled.shuffle()
                shuffled.add(0, playingItem)
                _queue.value = shuffled
                _currentIndex.value = 0
            } else {
                _queue.value = currentQ
                val idx = currentQ.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                _currentIndex.value = idx
            }
            pendingNextIds.clear()
        }
        consumePending(track.id)

        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _isMiniPlayerVisible.value = true
        com.example.util.MediaSeenManager.markSeen(appContext, track.path, track.id)
        clearABLoop()
        com.example.util.AudioPlaybackHistoryManager.savePosition(appContext, track, startPositionMs, track.durationMs, force = true)

        // Find lyrics: embedded tag / sidecar .lrc first, then online (LRCLIB, KuGou, ...)
        loadLyricsFor(appContext, track)

        userExplicitlyPaused = false
        resumeOnFocusGain = false
        requestAudioFocus()
        startMediaPlayer(appContext, track, startPositionMs.coerceAtLeast(0L))
    }

    private fun startMediaPlayer(
        context: Context,
        track: AudioTrackItem,
        startPositionMs: Long = 0L,
        seamless: Boolean = false
    ) {
        val generation = playbackGeneration
        val uriStr = track.uri.toString()
        val isOnlineTrack = track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
        // Seamless hand-off (used when the whole online track finished caching mid-playback):
        // the current player keeps playing until the replacement is prepared and positioned, so
        // there is no stop / buffering flash / restart from 0.
        val previousPlayer: MediaPlayer? = if (seamless) mediaPlayer else null

        if (!seamless) {
            startToken++
            stopProgressTracker()
            mediaPlayerPrepared = false

            // Cancel previous stream resolution job if running
            streamResolutionJob?.cancel()
            streamResolutionJob = null

            // Stop and release previous player instance immediately
            releaseAudioEffects()
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) mp.stop()
                    mp.release()
                }
            } catch (_: Throwable) {}
            mediaPlayer = null

            // A pre-buffered next track starts instantly, so no buffering state is shown for it.
            val standbyWaiting = isOnlineTrack && standbyMatches(track.id)
            _isPlaying.value = false
            _isBuffering.value = !standbyWaiting
            _currentPositionMs.value = startPositionMs.coerceAtLeast(0L)
            updateNotification(context, track, playing = false, isBuffering = !standbyWaiting)
        }
        val token = startToken

        // Fully cached online track: play the local file (no network, no buffering, cannot end early).
        if (isOnlineTrack && !uriStr.startsWith("file:")) {
            val cachedFile = OnlineAudioCache.cachedFile(
                context, track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
            )
            if (cachedFile != null) {
                startMediaPlayer(context, track.copy(uri = Uri.fromFile(cachedFile)), startPositionMs, seamless)
                return
            }
        }
        val isUnresolvedOnlineUri = isOnlineTrack &&
            (track.uri == Uri.EMPTY ||
             uriStr.isBlank() ||
             uriStr.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) ||
             uriStr.contains("youtube.com") ||
             uriStr.contains("youtu.be") ||
             uriStr.contains("spotify.com") ||
             uriStr.startsWith("spotify:"))
        if (isUnresolvedOnlineUri) {
            _isBuffering.value = true
            streamResolutionJob = scope.launch(Dispatchers.IO) {
                val videoUrl = track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                // For a fresh start or stream resume, prefer fresh resolution if not in cache
                var resolvedUrl: String? = if (startPositionMs > 5000L) null else preloadedOnlineStreams[videoUrl]
                if (resolvedUrl == null) {
                    for (attempt in 1..4) {
                        resolvedUrl = com.example.util.OnlineMusicService.resolveStreamUrl(videoUrl, track.title, track.artist)
                        if (resolvedUrl != null) break
                        delay(400L * attempt)
                    }
                }
                withContext(Dispatchers.Main) {
                    if (generation != playbackGeneration) return@withContext
                    if (resolvedUrl != null) {
                        preloadedOnlineStreams[videoUrl] = resolvedUrl
                        // keep lyrics that may have arrived while the stream was resolving
                        val latest = _queue.value.firstOrNull { it.id == track.id } ?: _currentTrack.value?.takeIf { it.id == track.id }
                        val updated = track.copy(
                            uri = Uri.parse(resolvedUrl),
                            hasLyrics = latest?.hasLyrics ?: track.hasLyrics,
                            lyricsText = latest?.lyricsText ?: track.lyricsText
                        )
                        val updatedQueue = _queue.value.map { if (it.id == track.id) updated else it }
                        _queue.value = updatedQueue
                        if (originalQueue.isNotEmpty()) {
                            originalQueue = originalQueue.map { if (it.id == track.id) updated else it }
                        }
                        if (_currentTrack.value?.id == track.id) {
                            _currentTrack.value = updated
                        }
                        startMediaPlayer(context, updated, startPositionMs)
                    } else {
                        _isBuffering.value = false
                        Log.e(TAG, "Failed to resolve stream for ${track.title} after retries. Auto-skipping to next track in queue...")
                        android.widget.Toast.makeText(context, "Unable to load stream. Skipping...", android.widget.Toast.LENGTH_SHORT).show()
                        skipNext(context)
                    }
                }
            }
            return
        }

        // Pre-buffered (already prepared) player for this exact track, if one is waiting.
        val standby: MediaPlayer? =
            if (isOnlineTrack && uriStr.startsWith("file:") && !seamless) takeStandbyPlayer(track.id) else null
        val player: MediaPlayer = standby ?: MediaPlayer()
        if (!seamless) mediaPlayer = player

        var playbackBegan = false

        fun beginPlayback(mp: MediaPlayer) {
            if (playbackBegan) return
            if (generation != playbackGeneration || token != startToken) {
                runCatching { mp.release() }
                return
            }
            playbackBegan = true
            if (seamless && (userExplicitlyPaused || runCatching { previousPlayer?.isPlaying }.getOrNull() == false)) {
                // The user paused while the local copy was being prepared: keep everything as it
                // is (the stream player stays paused); the next start uses the cached file anyway.
                runCatching { mp.release() }
                return
            }
            if (seamless) mediaPlayer = mp
            mediaPlayerPrepared = true
            _isBuffering.value = false
            val dur = mp.duration.toLong().coerceAtLeast(0L)
            _durationMs.value = if (dur > 0L) dur else track.durationMs
            applyPlaybackSpeed(_playbackSpeed.value)
            setupAudioEffects(mp.audioSessionId)
            mp.start()
            // A new MediaPlayer always starts at 1.0x: the chosen speed is applied once it runs.
            applySpeedTo(mp, _playbackSpeed.value)
            if (seamless) {
                try {
                    previousPlayer?.let { old ->
                        if (old.isPlaying) old.stop()
                        old.release()
                    }
                } catch (_: Throwable) {}
            }
            _currentPositionMs.value = runCatching { mp.currentPosition.toLong() }.getOrDefault(startPositionMs.coerceAtLeast(0L))
            _isPlaying.value = true
            startProgressTracker()
            updateNotification(context, track, true, isBuffering = false)
            preloadUpcomingOnlineStreams()
            prefetchUpcomingLyrics()
            if (isOnlineTrack) {
                // Online tracks remain stream-based; there is no full-track cache or download queue.
                prepareStandbyForUpcoming(context)
            }
        }

        val onPrepared: (MediaPlayer) -> Unit = prepared@{ mp ->
            if (generation != playbackGeneration || token != startToken) {
                runCatching { mp.release() }
                return@prepared
            }
            val dur = mp.duration.toLong().coerceAtLeast(0L)
            val wantedStart = if (seamless) {
                (runCatching { previousPlayer?.currentPosition?.toLong() }.getOrNull() ?: startPositionMs) + 180L
            } else {
                startPositionMs
            }
            val safeStart = if (wantedStart > 500L) {
                wantedStart.coerceIn(0L, (dur.coerceAtLeast(1L) - 250L).coerceAtLeast(0L))
            } else {
                0L
            }
            if (safeStart > 0L) {
                // Seek FIRST, start on seek-complete: no audible blip from 0:00 when resuming.
                mp.setOnSeekCompleteListener { p ->
                    p.setOnSeekCompleteListener(null)
                    beginPlayback(p)
                }
                scope.launch {
                    delay(2500L)
                    if (!playbackBegan) beginPlayback(mp)
                }
                try {
                    mp.seekTo(safeStart, MediaPlayer.SEEK_CLOSEST)
                } catch (_: Throwable) {
                    beginPlayback(mp)
                }
            } else {
                beginPlayback(mp)
            }
        }

        player.apply {
            try {
                runCatching { setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK) }
                if (standby == null) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    if (track.uri != Uri.EMPTY) {
                        setDataSource(context, track.uri)
                    } else if (track.path.isNotBlank()) {
                        setDataSource(track.path)
                    } else {
                        Log.e(TAG, "No valid URI or path for track: ${track.title}")
                        _isBuffering.value = false
                        if (seamless) runCatching { release() } else skipNext(context)
                        return
                    }
                }

                setOnInfoListener { _, what, _ ->
                    if (generation != playbackGeneration || token != startToken || !playbackBegan) return@setOnInfoListener true
                    when (what) {
                        MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                            _isBuffering.value = true
                            updateNotification(context, track, playing = true, isBuffering = true)
                        }
                        MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                            _isBuffering.value = false
                            updateNotification(context, track, playing = true, isBuffering = false)
                        }
                    }
                    false
                }

                setOnPreparedListener { mp -> onPrepared(mp) }

                setOnCompletionListener { mp ->
                    if (generation != playbackGeneration || token != startToken) return@setOnCompletionListener
                    mediaPlayerPrepared = false
                    _isBuffering.value = false
                    val isOnline = track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) ||
                                   track.uri.toString().startsWith("http")
                    val dur = expectedDurationMs(track, isOnline)
                    val pos = runCatching { mp.currentPosition.toLong() }.getOrNull() ?: _currentPositionMs.value

                    // Strict requirement: Fix premature termination (e.g. ~70% stopping bug)
                    // If an online stream closed or finished before reaching the end of the song (pos < dur - 3000L),
                    // it is a network drop / CDN chunk timeout. Reconnect with backoff or advance to next track.
                    if (isOnline && dur > 5000L && pos < (dur - 3000L)) {
                        if (lastRetriedTrackId != track.id) {
                            lastRetriedTrackId = track.id
                            trackRetryCount = 0
                        }
                        if (trackRetryCount < 3) {
                            trackRetryCount++
                            val videoUrl = track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                            preloadedOnlineStreams.remove(videoUrl)
                            Log.w(TAG, "Online audio stream ended prematurely at $pos ms / $dur ms. Re-connecting attempt $trackRetryCount of 3...")
                            _isBuffering.value = true
                            updateNotification(context, track, playing = true, isBuffering = true)
                            scope.launch {
                                delay(300L * trackRetryCount)
                                startMediaPlayer(context, track.copy(uri = Uri.EMPTY), startPositionMs = pos)
                            }
                        } else {
                            Log.w(TAG, "Premature completion retries exhausted for ${track.title}. Auto-advancing to next track.")
                            trackRetryCount = 0
                            handleTrackCompletion(context)
                        }
                    } else {
                        trackRetryCount = 0
                        handleTrackCompletion(context)
                    }
                }

                setOnErrorListener { failedPlayer, what, extra ->
                    if (generation != playbackGeneration || token != startToken) return@setOnErrorListener true
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    if (seamless && !playbackBegan) {
                        // The local copy could not be played: drop it, keep playing the stream.
                        runCatching { failedPlayer.release() }
                        if (uriStr.startsWith("file:")) {
                            OnlineAudioCache.delete(context, track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX))
                        }
                        return@setOnErrorListener true
                    }
                    mediaPlayerPrepared = false
                    val isOnline = track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) ||
                                   track.uri.toString().startsWith("http")
                    if (isOnline) {
                        val pos = _currentPositionMs.value
                        val dur = expectedDurationMs(track, true)
                        // A corrupt cached file must never be replayed: drop it so the retry streams again.
                        if (track.uri.toString().startsWith("file:")) {
                            OnlineAudioCache.delete(context, track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX))
                        }
                        // If stream errored before reaching 100% completion, reconnect and resume with backoff
                        if (dur <= 0L || pos < (dur - 3000L)) {
                            if (lastRetriedTrackId != track.id) {
                                lastRetriedTrackId = track.id
                                trackRetryCount = 0
                            }
                            if (trackRetryCount < 8) {
                                trackRetryCount++
                                val videoUrl = track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                                preloadedOnlineStreams.remove(videoUrl)
                                Log.w(TAG, "Online audio stream error at $pos ms / $dur ms. Re-connecting attempt $trackRetryCount/8...")
                                _isBuffering.value = true
                                updateNotification(context, track, playing = true, isBuffering = true)
                                scope.launch {
                                    delay(minOf(400L * trackRetryCount, 4000L))
                                    startMediaPlayer(context, track.copy(uri = Uri.EMPTY), startPositionMs = pos)
                                }
                                return@setOnErrorListener true
                            } else {
                                Log.e(TAG, "Online stream error retries exhausted. Auto-skipping to next track in playlist.")
                                trackRetryCount = 0
                                _isBuffering.value = false
                                scope.launch(Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "Playback error. Skipping to next track...", android.widget.Toast.LENGTH_SHORT).show()
                                    skipNext(context)
                                }
                                return@setOnErrorListener true
                            }
                        }
                    }
                    _isBuffering.value = false
                    _isPlaying.value = false
                    stopProgressTracker()
                    updateNotification(context, track, false)
                    // If error occurs, auto-skip to next track so player never gets permanently stuck
                    scope.launch(Dispatchers.Main) {
                        skipNext(context)
                    }
                    true
                }

                if (standby != null) {
                    onPrepared(this)
                } else {
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up MediaPlayer: ${e.message}", e)
                if (seamless) {
                    // Keep the current (streaming) player untouched; just discard the failed copy.
                    runCatching { release() }
                } else {
                    mediaPlayerPrepared = false
                    _isBuffering.value = false
                    _isPlaying.value = false
                    scope.launch(Dispatchers.Main) {
                        skipNext(context)
                    }
                }
            }
        }
    }

    /**
     * Duration that decides whether a track really finished. For online tracks the catalogue
     * duration is trusted when it is longer than what the stream reports, so a truncated CDN
     * stream can never be mistaken for the end of the song (no auto-advance before the end).
     */
    private fun expectedDurationMs(track: AudioTrackItem, isOnline: Boolean): Long {
        val playerDur = _durationMs.value.takeIf { it > 0L } ?: track.durationMs
        if (!isOnline || track.durationMs <= 5000L) return playerDur
        // A complete local file knows its own length; only a live stream can be truncated.
        if (track.uri.toString().startsWith("file:")) return playerDur
        return maxOf(playerDur, track.durationMs)
    }

    /** The online track [offset] places after the current one, exactly as skipNext() will pick it. */
    private fun upcomingOnlineTrack(offset: Int): AudioTrackItem? {
        if (_repeatMode.value == AudioRepeatMode.ONE) return null
        val q = _queue.value
        if (q.size < 2) return null
        val idx = _currentIndex.value
        val i = idx + offset
        val candidate = q.getOrNull(i)
            ?: if (_repeatMode.value == AudioRepeatMode.ALL) q[i % q.size] else null
        return candidate?.takeIf {
            it.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) &&
                it.id != _currentTrack.value?.id
        }
    }

    // ---------------------------------------------------------------------------------------
    // Standby player: the NEXT online track, pre-prepared for streaming, so the
    // switch at the end of the current song only has to call start().
    // ---------------------------------------------------------------------------------------
    private fun standbyMatches(trackId: Long): Boolean =
        standbyPlayer != null && standbyPrepared && standbyTrackId == trackId

    private fun takeStandbyPlayer(trackId: Long): MediaPlayer? {
        val p = standbyPlayer
        if (p != null && standbyPrepared && standbyTrackId == trackId) {
            standbyPlayer = null
            standbyTrackId = null
            standbyPrepared = false
            return p
        }
        return null
    }

    private fun releaseStandby() {
        val p = standbyPlayer
        standbyPlayer = null
        standbyTrackId = null
        standbyPrepared = false
        if (p != null) {
            try { p.setOnPreparedListener(null) } catch (_: Throwable) {}
            try { p.setOnErrorListener(null) } catch (_: Throwable) {}
            try { p.release() } catch (_: Throwable) {}
        }
    }

    /** Main thread only. Prepares a player for the track skipNext() would play next (if cached). */
    private fun prepareStandbyForUpcoming(context: Context) {
        if (!mediaPlayerPrepared) return
        val next = upcomingOnlineTrack(1)
        if (next == null) {
            releaseStandby()
            return
        }
        if (standbyPlayer != null && standbyTrackId == next.id) return
        val videoUrl = next.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
        val file = OnlineAudioCache.cachedFile(context, videoUrl)
        releaseStandby()
        if (file == null) return
        val mp = MediaPlayer()
        try {
            runCatching { mp.setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK) }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setDataSource(file.absolutePath)
            standbyPlayer = mp
            standbyTrackId = next.id
            standbyPrepared = false
            mp.setOnPreparedListener { p ->
                if (standbyPlayer === p) {
                    standbyPrepared = true
                } else {
                    runCatching { p.release() }
                }
            }
            mp.setOnErrorListener { p, _, _ ->
                if (standbyPlayer === p) {
                    OnlineAudioCache.delete(context, videoUrl)
                    releaseStandby()
                } else {
                    runCatching { p.release() }
                }
                true
            }
            mp.prepareAsync()
        } catch (t: Throwable) {
            Log.w(TAG, "Standby prepare failed: ${t.message}")
            if (standbyPlayer === mp) releaseStandby() else runCatching { mp.release() }
        }
    }

    /** Queue order / repeat mode changed: the pre-buffered "next" track may no longer be right. */
    private fun onQueueOrderChanged() {
        val ctx = applicationContext ?: return
        if (_currentTrack.value == null) return
        prepareStandbyForUpcoming(ctx)
        prefetchUpcomingLyrics()
    }

    // ---------------------------------------------------------------------------------------
    // Lyrics prefetch: the lyrics of the next two tracks are searched in the background while the
    // current track plays, so they are already there (no network wait) when the track starts.
    // ---------------------------------------------------------------------------------------
    private val lyricsPrefetched = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val lyricsPrefetchTried = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var lyricsPrefetchJob: Job? = null

    private fun prefetchUpcomingLyrics(count: Int = 2) {
        val ctx = applicationContext ?: return
        val q = _queue.value
        if (q.size < 2 || _repeatMode.value == AudioRepeatMode.ONE) return
        val cur = _currentIndex.value
        val curId = _currentTrack.value?.id
        val upcoming = (1..count).mapNotNull { off ->
            q.getOrNull(cur + off)
                ?: if (_repeatMode.value == AudioRepeatMode.ALL) q[(cur + off) % q.size] else null
        }.filter { it.id != curId }
            .distinctBy { it.path }
            .filter { it.path !in lyricsPrefetchTried }
        if (upcoming.isEmpty()) return
        if (lyricsPrefetchTried.size > 300) { lyricsPrefetchTried.clear(); lyricsPrefetched.clear() }

        lyricsPrefetchJob?.cancel()
        lyricsPrefetchJob = scope.launch(Dispatchers.IO) {
            for (track in upcoming) {
                try {
                    prefetchLyricsFor(ctx, track)
                    lyricsPrefetchTried.add(track.path)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.d("Lyrics", "Lyrics prefetch failed quietly: ${e.message}")
                }
            }
        }
    }

    /** Same lookup as [loadLyricsFor] but silent: it only fills the caches / queue copy, never the current-track UI state. */
    private suspend fun prefetchLyricsFor(context: Context, track: AudioTrackItem) {
        val isOnlineTrack = track.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
        var title = track.title
        var artist = track.artist
        var durationMs = track.durationMs
        var album = track.album
        var found = false

        if (!isOnlineTrack) {
            val extracted = AudioMetadataExtractor.extractFullMetadata(context, track)
            extracted.title?.let { title = it }
            extracted.artist?.let { artist = it }
            if (extracted.durationMs > 0L) durationMs = extracted.durationMs
            extracted.album?.let { album = it }
            val text = extracted.lyrics
            if (!text.isNullOrBlank()) {
                val parsed = LyricsUtils.parseLyrics(text, LyricsSourceType.LOCAL)
                if (parsed.isValid()) {
                    embeddedLyricsByPath[track.path] = parsed
                    found = true
                    withContext(Dispatchers.Main) {
                        applyTrackUpdate(track.id) { t ->
                            t.copy(hasLyrics = true, lyricsText = text)
                        }
                    }
                }
            }
            if (embeddedLyricsByPath[track.path]?.synced?.isNotEmpty() == true) {
                lyricsPrefetched.add(track.path)
                return
            }
        }

        if (!OnlineLyricsService.isAutoFetchEnabled(context)) {
            if (found) lyricsPrefetched.add(track.path)
            return
        }
        val result = OnlineLyricsService.fetch(
            context = context,
            title = title,
            artist = artist,
            durationMs = durationMs,
            album = album.takeIf { it.isNotBlank() },
            videoId = OnlineTrackVideoIds.from(track.path),
            mediaPath = track.path,
            provider = preferredLyricsProvider,
            forceRefresh = false,
            forceOnline = false,
        )
        val online = result.lyrics?.takeIf { it.isValid() }
        if (online != null) {
            val remote = online.copy(areFromRemote = true, sourceType = LyricsSourceType.ONLINE)
            onlineLyricsByPath[track.path] = remote
            result.provider?.let { onlineProviderByPath[track.path] = it }
            val text = LyricsTextCodec.toLrc(remote)
            withContext(Dispatchers.Main) {
                applyTrackUpdate(track.id) { t ->
                    if (t.lyricsText.isBlank() || !t.hasLyrics) t.copy(hasLyrics = true, lyricsText = text) else t
                }
            }
            lyricsPrefetched.add(track.path)
        } else {
            val local = result.embeddedLyrics?.takeIf { it.isValid() }
            if (local != null) {
                val embedded = local.copy(areFromRemote = false, sourceType = result.selectedSource)
                embeddedLyricsByPath[track.path] = embedded
                val text = LyricsTextCodec.toLrc(embedded)
                withContext(Dispatchers.Main) {
                    applyTrackUpdate(track.id) { t ->
                        if (t.lyricsText.isBlank() || !t.hasLyrics) t.copy(hasLyrics = true, lyricsText = text) else t
                    }
                }
                lyricsPrefetched.add(track.path)
            } else if (found) {
                lyricsPrefetched.add(track.path)
            }
        }
    }

    /**
     * Resolve the next three online queue items and prefetch their thumbnails in the background
     * so that track changes and Dynamic Island updates are instantaneous.
     */
    private fun preloadUpcomingOnlineStreams() {
        streamPreloadJob?.cancel()
        val queueSnapshot = _queue.value
        val currentIndexSnapshot = _currentIndex.value
        if (queueSnapshot.isEmpty()) return

        val upcoming = (1..3).mapNotNull { offset ->
            queueSnapshot.getOrNull(currentIndexSnapshot + offset)
                ?: if (_repeatMode.value == AudioRepeatMode.ALL && queueSnapshot.isNotEmpty()) {
                    queueSnapshot.getOrNull((currentIndexSnapshot + offset) % queueSnapshot.size)
                } else null
        }.filter { it.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) }
            .distinctBy { it.path }

        if (upcoming.isEmpty()) return

        streamPreloadJob = scope.launch(Dispatchers.IO) {
            for (track in upcoming) {
                // Prefetch high-resolution thumbnail so Next button shows image instantly
                if (!track.thumbnailUrl.isNullOrBlank()) {
                    try {
                        com.example.ui.components.loadRemoteAudioThumbnail(track.thumbnailUrl, 384)
                    } catch (_: Throwable) {}
                }
                val videoUrl = track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                if (videoUrl.isBlank() || preloadedOnlineStreams.containsKey(videoUrl)) continue
                val resolved = com.example.util.OnlineMusicService.resolveStreamUrl(videoUrl, track.title, track.artist)
                if (!resolved.isNullOrBlank()) {
                    preloadedOnlineStreams[videoUrl] = resolved
                    withContext(Dispatchers.Main) {
                        val latestInQueue = _queue.value.firstOrNull { it.id == track.id }
                        if (latestInQueue != null) {
                            val updated = latestInQueue.copy(uri = Uri.parse(resolved))
                            _queue.value = _queue.value.map { if (it.id == track.id) updated else it }
                            if (originalQueue.isNotEmpty()) {
                                originalQueue = originalQueue.map { if (it.id == track.id) updated else it }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateNotification(
        context: Context?,
        track: AudioTrackItem? = _currentTrack.value,
        playing: Boolean = _isPlaying.value,
        isBuffering: Boolean = _isBuffering.value
    ) {
        val targetContext = context ?: applicationContext ?: return
        AudioNotificationManager.showOrUpdateNotification(
            context = targetContext,
            track = track,
            isPlaying = playing,
            positionMs = _currentPositionMs.value,
            durationMs = _durationMs.value,
            playbackSpeed = _playbackSpeed.value,
            isBuffering = isBuffering
        )
    }

    private fun handleTrackCompletion(context: Context) {
        val current = _currentTrack.value
        if (current != null) {
            val isOnline = current.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) ||
                           current.uri.toString().startsWith("http")
            val dur = _durationMs.value.takeIf { it > 0L } ?: current.durationMs
            val pos = _currentPositionMs.value
            // Only switch tracks upon 100% completion
            if (isOnline && dur > 5000L && pos < (dur - 3000L)) {
                val videoUrl = current.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
                preloadedOnlineStreams.remove(videoUrl)
                Log.w(TAG, "handleTrackCompletion invoked prematurely at $pos ms of $dur ms (~${((pos.toFloat()/dur)*100).toInt()}%). Resuming stream...")
                _isBuffering.value = true
                scope.launch {
                    delay(200L)
                    startMediaPlayer(context, current.copy(uri = Uri.EMPTY), startPositionMs = pos)
                }
                return
            }
        }
        when (_repeatMode.value) {
            AudioRepeatMode.ONE -> {
                seekTo(0L)
                mediaPlayer?.start()
                _isPlaying.value = true
                updateNotification(context, _currentTrack.value, true)
            }
            AudioRepeatMode.ALL -> {
                skipNext(context)
            }
            AudioRepeatMode.OFF -> {
                val q = _queue.value
                val cur = _currentIndex.value
                if (cur < q.size - 1) {
                    skipNext(context)
                } else {
                    _isPlaying.value = false
                    seekTo(0L)
                    stopProgressTracker()
                    updateNotification(context, _currentTrack.value, false)
                }
            }
        }
    }

    fun togglePlayPause(context: Context? = null) {
        val targetContext = context ?: applicationContext
        val player = mediaPlayer ?: return
        if (!mediaPlayerPrepared) return
        try {
            if (player.isPlaying) {
                userExplicitlyPaused = true
                resumeOnFocusGain = false
                abandonAudioFocus()
                val exactPos = runCatching { player.currentPosition.toLong() }.getOrNull() ?: _currentPositionMs.value
                _currentPositionMs.value = exactPos
                targetContext?.let { ctx -> AudioPlaybackHistoryManager.savePosition(ctx, _currentTrack.value, exactPos, _durationMs.value, force = true) }
                player.pause()
                _isPlaying.value = false
                _isBuffering.value = false
                stopProgressTracker()
                targetContext?.let { updateNotification(it, _currentTrack.value, playing = false, isBuffering = false) }
            } else {
                userExplicitlyPaused = false
                resumeOnFocusGain = false
                requestAudioFocus()
                applyPlaybackSpeed(_playbackSpeed.value)
                player.start()
                applySpeedTo(player, _playbackSpeed.value)
                _isPlaying.value = true
                _isBuffering.value = false
                startProgressTracker()
                targetContext?.let { updateNotification(it, _currentTrack.value, playing = true, isBuffering = false) }
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Ignoring rapid play/pause while MediaPlayer is not ready: ${e.message}")
        }
    }

    fun pause(context: Context? = null) {
        val targetContext = context ?: applicationContext
        userExplicitlyPaused = true
        resumeOnFocusGain = false
        abandonAudioFocus()
        mediaPlayer?.let { player ->
            if (!mediaPlayerPrepared) return
            if (player.isPlaying) {
                val exactPos = runCatching { player.currentPosition.toLong() }.getOrNull() ?: _currentPositionMs.value
                _currentPositionMs.value = exactPos
                targetContext?.let { ctx -> AudioPlaybackHistoryManager.savePosition(ctx, _currentTrack.value, exactPos, _durationMs.value, force = true) }
                player.pause()
                _isPlaying.value = false
                _isBuffering.value = false
                stopProgressTracker()
                targetContext?.let { updateNotification(it, _currentTrack.value, playing = false, isBuffering = false) }
            }
        }
    }

    fun resume(context: Context? = null) {
        val targetContext = context ?: applicationContext
        userExplicitlyPaused = false
        resumeOnFocusGain = false
        mediaPlayer?.let { player ->
            if (!mediaPlayerPrepared) return
            if (!player.isPlaying) {
                requestAudioFocus()
                applyPlaybackSpeed(_playbackSpeed.value)
                player.start()
                applySpeedTo(player, _playbackSpeed.value)
                _isPlaying.value = true
                startProgressTracker()
                targetContext?.let { updateNotification(it, _currentTrack.value, true) }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val dur = _durationMs.value.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, dur)
        _currentPositionMs.value = clamped
        applicationContext?.let { ctx -> AudioPlaybackHistoryManager.savePosition(ctx, _currentTrack.value, clamped, _durationMs.value, force = true) }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mediaPlayer?.seekTo(clamped, MediaPlayer.SEEK_CLOSEST)
            } else {
                mediaPlayer?.seekTo(clamped.toInt())
            }
            applicationContext?.let { ctx ->
                AudioNotificationManager.updateMediaSessionState(
                    context = ctx,
                    track = _currentTrack.value,
                    isPlaying = _isPlaying.value,
                    positionMs = clamped,
                    durationMs = _durationMs.value,
                    playbackSpeed = _playbackSpeed.value
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Seek error: ${e.message}")
        }
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = _currentPositionMs.value + deltaMs
        seekTo(newPos)
    }

    fun skipNext(context: Context? = null) {
        val targetContext = context ?: applicationContext
        playbackGeneration++
        mediaPlayerPrepared = false
        val q = _queue.value
        if (q.isEmpty()) return

        streamResolutionJob?.cancel()
        streamResolutionJob = null
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Throwable) {}
        mediaPlayer = null
        _isPlaying.value = false
        _isBuffering.value = true
        _currentPositionMs.value = 0L

        val nextIndex = (_currentIndex.value + 1) % q.size
        val nextTrack = q.getOrNull(nextIndex) ?: return
        _currentIndex.value = nextIndex
        _currentTrack.value = nextTrack
        consumePending(nextTrack.id)
        _durationMs.value = nextTrack.durationMs
        trackRetryCount = 0
        lastRetriedTrackId = nextTrack.id
        clearABLoop()
        // Next track already cached + prepared: it starts instantly, so don't flash "buffering".
        val nextIsPrebuffered = nextTrack.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) &&
            standbyMatches(nextTrack.id)
        if (nextIsPrebuffered) _isBuffering.value = false
        targetContext?.let { loadLyricsFor(it, nextTrack) }

        if (targetContext != null) {
            updateNotification(targetContext, nextTrack, playing = false, isBuffering = !nextIsPrebuffered)
            startMediaPlayer(targetContext, nextTrack, startPositionMs = 0L)
        }
    }

    fun skipPrevious(context: Context? = null) {
        val targetContext = context ?: applicationContext
        // If current position > 3 seconds, restart current track without replacing the player.
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        playbackGeneration++
        mediaPlayerPrepared = false
        val q = _queue.value
        if (q.isEmpty()) return

        streamResolutionJob?.cancel()
        streamResolutionJob = null
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (_: Throwable) {}
        mediaPlayer = null
        _isPlaying.value = false
        _isBuffering.value = true
        _currentPositionMs.value = 0L

        val prevIndex = if (_currentIndex.value - 1 < 0) q.size - 1 else _currentIndex.value - 1
        val prevTrack = q.getOrNull(prevIndex) ?: return
        _currentIndex.value = prevIndex
        _currentTrack.value = prevTrack
        consumePending(prevTrack.id)
        _durationMs.value = prevTrack.durationMs
        trackRetryCount = 0
        lastRetriedTrackId = prevTrack.id
        clearABLoop()
        targetContext?.let { loadLyricsFor(it, prevTrack) }

        if (targetContext != null) {
            updateNotification(targetContext, prevTrack, playing = false, isBuffering = true)
            startMediaPlayer(targetContext, prevTrack, startPositionMs = 0L)
        }
    }

    fun toggleShuffle() {
        val enablingShuffle = !_isShuffle.value
        _isShuffle.value = enablingShuffle

        val curTrack = _currentTrack.value
        val curTrackId = curTrack?.id

        if (enablingShuffle) {
            if (originalQueue.isEmpty() || originalQueue.size != _queue.value.size) {
                originalQueue = _queue.value
            }
            if (_queue.value.size > 1) {
                val currentQ = _queue.value.toMutableList()
                val currentPlaying = currentQ.firstOrNull { it.id == curTrackId }
                if (currentPlaying != null) {
                    currentQ.remove(currentPlaying)
                    // Tracks queued with "Play next" stay right after the current track.
                    val pending = pendingNextIds.mapNotNull { pid -> currentQ.firstOrNull { it.id == pid } }
                    currentQ.removeAll(pending.toSet())
                    currentQ.shuffle()
                    currentQ.addAll(0, pending)
                    currentQ.add(0, currentPlaying)
                } else {
                    currentQ.shuffle()
                }
                _queue.value = currentQ
                _currentIndex.value = 0
            }
        } else {
            if (originalQueue.isNotEmpty()) {
                val restored = originalQueue
                _queue.value = restored
                if (curTrackId != null) {
                    val idx = restored.indexOfFirst { it.id == curTrackId }.coerceAtLeast(0)
                    _currentIndex.value = idx
                }
            }
        }
        onQueueOrderChanged()
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            AudioRepeatMode.ALL -> AudioRepeatMode.ONE
            AudioRepeatMode.ONE -> AudioRepeatMode.OFF
            AudioRepeatMode.OFF -> AudioRepeatMode.ALL
        }
        onQueueOrderChanged()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying || _isPlaying.value) {
                        applySpeedTo(player, speed)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply playback speed: ${e.message}")
        }
    }

    fun stop(context: Context? = null) {
        val targetContext = context ?: applicationContext
        playbackGeneration++
        mediaPlayerPrepared = false
        streamResolutionJob?.cancel()
        streamResolutionJob = null
        streamPreloadJob?.cancel()
        streamPreloadJob = null
        lyricsPrefetchJob?.cancel()
        lyricsPrefetchJob = null
        releaseStandby()
        preloadedOnlineStreams.clear()
        targetContext?.let { ctx -> AudioPlaybackHistoryManager.savePosition(ctx, _currentTrack.value, _currentPositionMs.value, _durationMs.value, force = true) }
        stopProgressTracker()
        releaseAudioEffects()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        _isPlaying.value = false
        _isBuffering.value = false
        _currentPositionMs.value = 0L
        abandonAudioFocus()
        targetContext?.let { AudioNotificationManager.cancelNotification(it) }
    }

    private fun setupAudioEffects(sessionId: Int) {
        releaseAudioEffects()
        if (sessionId <= 0) return
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = isEqEnabled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer init error: ${e.message}")
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = isEqEnabled && (eqVolumeBoostDb > 0.05f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost init error: ${e.message}")
        }

        try {
            virtualizer = Virtualizer(0, sessionId).apply {
                enabled = isHeadphoneSurroundEnabled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer init error: ${e.message}")
        }

        applyAudioFxImmediately()
    }

    // Last values written to the effect objects. Re-writing an unchanged value still makes the effect engine
    // re-configure (an audible tick / crack), so only real changes are sent.
    private var fxAppliedEqKey: String? = null
    private var fxAppliedBassKey: String? = null
    private var fxAppliedVirtKey: String? = null

    private fun releaseAudioEffects() {
        fxAppliedEqKey = null
        fxAppliedBassKey = null
        fxAppliedVirtKey = null
        try {
            equalizer?.release()
            equalizer = null
        } catch (_: Exception) {}
        try {
            bassBoost?.release()
            bassBoost = null
        } catch (_: Exception) {}
        try {
            virtualizer?.release()
            virtualizer = null
        } catch (_: Exception) {}
    }

    /**
     * Applies real-time hardware Equalizer, BassBoost, and Volume Boost directly
     * to the running audio stream with zero delay, zero audio dropout, and zero glitches.
     */
    fun updateEqualizer(
        enabled: Boolean,
        eq60: Float,
        eq230: Float,
        eq910: Float,
        eq3600: Float,
        eq14000: Float,
        volumeBoostDb: Float
    ) {
        isEqEnabled = enabled
        eqGain60 = eq60.coerceIn(-12f, 12f)
        eqGain230 = eq230.coerceIn(-12f, 12f)
        eqGain910 = eq910.coerceIn(-12f, 12f)
        eqGain3600 = eq3600.coerceIn(-12f, 12f)
        eqGain14000 = eq14000.coerceIn(-12f, 12f)
        eqVolumeBoostDb = volumeBoostDb.coerceIn(0f, 10f)

        applyAudioFxImmediately()
    }

    /**
     * Toggles 3D Spatial Virtualizer for immersive headphone surround sound.
     */
    fun setHeadphoneSurround(enabled: Boolean) {
        isHeadphoneSurroundEnabled = enabled
        // Goes through the changed-value guard: re-writing an unchanged effect setting is an audible tick.
        applyAudioFxImmediately()
    }

    private fun applyAudioFxImmediately() {
        try {
            equalizer?.let { eq ->
                val eqKey = "$isEqEnabled|$eqGain60|$eqGain230|$eqGain910|$eqGain3600|$eqGain14000"
                if (eqKey == fxAppliedEqKey) return@let
                fxAppliedEqKey = eqKey
                eq.enabled = isEqEnabled
                if (isEqEnabled) {
                    val minLevel = eq.bandLevelRange?.getOrNull(0) ?: -1500
                    val maxLevel = eq.bandLevelRange?.getOrNull(1) ?: 1500
                    val numBands = eq.numberOfBands.toInt()
                    val uiGains = floatArrayOf(eqGain60, eqGain230, eqGain910, eqGain3600, eqGain14000)
                    val uiFreqs = intArrayOf(60, 230, 910, 3600, 14000)

                    for (band in 0 until numBands) {
                        val centerFreqHz = try { eq.getCenterFreq(band.toShort()) / 1000 } catch (_: Exception) { 0 }
                        var closestIdx = 0
                        var minDiff = Int.MAX_VALUE
                        for (i in uiFreqs.indices) {
                            val diff = kotlin.math.abs(uiFreqs[i] - centerFreqHz)
                            if (diff < minDiff) {
                                minDiff = diff
                                closestIdx = i
                            }
                        }
                        val gainDb = if (numBands == 5 && band < 5) uiGains[band] else uiGains[closestIdx]
                        val millibels = (gainDb * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                        eq.setBandLevel(band.toShort(), millibels.toShort())
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying equalizer bands: ${e.message}")
        }

        try {
            bassBoost?.let { bb ->
                val active = isEqEnabled && (eqVolumeBoostDb > 0.05f)
                val bbKey = if (active) "on|${(eqVolumeBoostDb * 100f).toInt()}" else "off"
                if (bbKey == fxAppliedBassKey) return@let
                fxAppliedBassKey = bbKey
                bb.enabled = active
                if (active) {
                    val strength = (eqVolumeBoostDb * 100f).toInt().coerceIn(0, 1000)
                    bb.setStrength(strength.toShort())
                }
            }
        } catch (_: Exception) {}

        try {
            virtualizer?.let { v ->
                val vKey = isHeadphoneSurroundEnabled.toString()
                if (vKey == fxAppliedVirtKey) return@let
                fxAppliedVirtKey = vKey
                v.enabled = isHeadphoneSurroundEnabled
                if (isHeadphoneSurroundEnabled) {
                    v.setStrength(1000.toShort())
                }
            }
        } catch (_: Exception) {}

        applyOutputVolume()
    }

    private fun handleOnlineStreamStall(context: Context?, track: AudioTrackItem, pos: Long) {
        val targetContext = context ?: applicationContext ?: return
        if (lastRetriedTrackId != track.id) {
            lastRetriedTrackId = track.id
            trackRetryCount = 0
        }
        if (trackRetryCount < 40) {
            trackRetryCount++
            val videoUrl = track.path.removePrefix(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX)
            preloadedOnlineStreams.remove(videoUrl)
            Log.w(TAG, "Online stream stalled at $pos ms. Watchdog triggering reconnect retry $trackRetryCount of 40...")
            _isBuffering.value = true
            updateNotification(targetContext, track, playing = true, isBuffering = true)
            scope.launch {
                delay(minOf(500L * trackRetryCount, 5000L))
                startMediaPlayer(targetContext, track.copy(uri = Uri.EMPTY), startPositionMs = pos)
            }
        } else {
            // Never abandon a song mid-way because of a slow network: keep waiting and retrying.
            Log.w(TAG, "Online stream still stalled at $pos ms; continuing to wait instead of skipping.")
            trackRetryCount = 20
            _isBuffering.value = true
        }
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            var lastPos = -1L
            var stalledTicks = 0
            while (isActive) {
                try {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            val pos = player.currentPosition.toLong()
                            _currentPositionMs.value = pos
                            val dur = player.duration.toLong()
                            if (dur > 0L) {
                                _durationMs.value = dur
                            }
                            applicationContext?.let { ctx ->
                                AudioPlaybackHistoryManager.savePosition(ctx, _currentTrack.value, pos, _durationMs.value)
                            }

                            // Buffer & network stall watchdog for online tracks
                            val current = _currentTrack.value
                            val isOnline = current != null && (
                                current.path.startsWith(com.example.ui.components.ONLINE_TRACK_PATH_PREFIX) ||
                                current.uri.toString().startsWith("http")
                            )
                            if (isOnline && dur > 5000L && pos < (dur - 2000L)) {
                                if (pos == lastPos) {
                                    stalledTicks++
                                    // Stalled for > 3 seconds (12 ticks of 250ms)
                                    if (stalledTicks == 12) {
                                        _isBuffering.value = true
                                        updateNotification(applicationContext, current, playing = true, isBuffering = true)
                                    }
                                    // Stalled for > 6 seconds (24 ticks of 250ms) -> trigger auto-recovery
                                    if (stalledTicks >= 24) {
                                        stalledTicks = 0
                                        handleOnlineStreamStall(applicationContext, current, pos)
                                    }
                                } else {
                                    if (stalledTicks >= 12) {
                                        _isBuffering.value = false
                                        updateNotification(applicationContext, current, playing = true, isBuffering = false)
                                    }
                                    stalledTicks = 0
                                    lastPos = pos
                                }
                            } else {
                                stalledTicks = 0
                                lastPos = pos
                            }

                            // AB Loop checking
                            val ptA = _loopPointA.value
                            val ptB = _loopPointB.value
                            if (ptA != null && ptB != null && ptB > ptA) {
                                if (pos >= ptB) {
                                    seekTo(ptA)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                audioFocusHeld = false
                duckedForFocus = false
                resumeOnFocusGain = false
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        player.pause()
                        _isPlaying.value = false
                        stopProgressTracker()
                        applicationContext?.let { updateNotification(it, _currentTrack.value, false) }
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // A short sound over the music: keep playing, only lower the volume (no pause / resume gap).
                duckedForFocus = true
                applyOutputVolume()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusHeld = false
                // If music was actively playing when mic or external app requested audio focus,
                // pause immediately so microphone (e.g. keyboard voice typing) does not record music.
                // However, if the user had ALREADY explicitly paused playback, keep resumeOnFocusGain = false
                // so that when mic closes, music does NOT automatically start playing!
                val wasPlaying = _isPlaying.value && (mediaPlayer?.isPlaying == true)
                if (wasPlaying && !userExplicitlyPaused) {
                    resumeOnFocusGain = true
                    mediaPlayer?.let { player ->
                        player.pause()
                        _isPlaying.value = false
                        stopProgressTracker()
                        applicationContext?.let { updateNotification(it, _currentTrack.value, false) }
                    }
                } else {
                    resumeOnFocusGain = false
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusHeld = true
                if (duckedForFocus) {
                    duckedForFocus = false
                    applyOutputVolume()
                }
                // NEVER resume if user explicitly paused the music!
                if (resumeOnFocusGain && !userExplicitlyPaused) {
                    resumeOnFocusGain = false
                    mediaPlayer?.let { player ->
                        if (!player.isPlaying) {
                            applyPlaybackSpeed(_playbackSpeed.value)
                            player.start()
                            applySpeedTo(player, _playbackSpeed.value)
                            _isPlaying.value = true
                            startProgressTracker()
                            applicationContext?.let { updateNotification(it, _currentTrack.value, true) }
                        }
                    }
                } else {
                    resumeOnFocusGain = false
                }
            }
        }
    }

    /** The volume the player really needs: volume boost and the short duck while another app makes a sound. */
    private fun applyOutputVolume() {
        try {
            val player = mediaPlayer ?: return
            val base = if (eqVolumeBoostDb > 0.05f) {
                Math.pow(10.0, (eqVolumeBoostDb / 25.0)).toFloat().coerceIn(1.0f, 2.0f)
            } else 1.0f
            val wanted = if (duckedForFocus) base * 0.2f else base
            // An unchanged value is not written again (every write is one more tick in the output).
            if (player === volumeAppliedTo && wanted == volumeApplied) return
            player.setVolume(wanted, wanted)
            volumeAppliedTo = player
            volumeApplied = wanted
        } catch (_: Exception) {}
    }

    private var speedAppliedTo: MediaPlayer? = null
    private var speedApplied: Float = 1.0f

    private fun applySpeedTo(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        // A fresh MediaPlayer runs at 1.0x; an unchanged speed is not written again (re-configures the output = a tick).
        val current = if (player === speedAppliedTo) speedApplied else 1.0f
        if (speed == current) return
        try {
            val params = player.playbackParams ?: PlaybackParams()
            params.speed = speed.coerceIn(0.25f, 3.0f)
            player.playbackParams = params
            speedAppliedTo = player
            speedApplied = speed
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply playback speed: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        try {
            val am = audioManager ?: return
            // Already ours: asking again would only create one more entry in the system's focus stack.
            if (audioFocusHeld) return
            val result: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                    .also { focusRequest = it }
                result = am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                result = am.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            audioFocusHeld = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (_: Exception) {}
    }

    private fun abandonAudioFocus() {
        try {
            resumeOnFocusGain = false
            audioFocusHeld = false
            if (duckedForFocus) {
                duckedForFocus = false
                applyOutputVolume()
            }
            val am = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusChangeListener)
            }
        } catch (_: Exception) {}
    }
}
