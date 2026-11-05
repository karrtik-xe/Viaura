/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Progressive, piece-prioritised torrent streaming. The approach follows mpvRx's TorrentStreamingEngine / TorrentProxyServer
 * (AGPL-3.0): one libtorrent session per stream, only the chosen file is selected, the head and tail pieces are requested
 * first, a loopback HTTP server with Range support hands the file to the player and every read moves a window of
 * time-critical pieces (deadlines) to the position the player is reading. On top of that:
 *  - the window follows EVERY open connection, so a seek (the player closes the old connection and opens a new one) rebuilds
 *    the deadlines around the new position instead of leaving the old window in front of it;
 *  - the end of an MP4 / MOV is located from the box headers (moov after mdat), other containers get a tail window;
 *  - a background ticker keeps extending the window while the player is paused, watches the free storage and tears an idle
 *    stream down;
 *  - the loopback URL carries a random capability token, so other apps on the phone cannot read the stream;
 *  - subtitle files of the torrent are served from the same loopback server.
 */

package com.example.torrent

import android.content.Context
import android.util.Log
import com.example.util.media.MediaSubtitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionHandle
import org.libtorrent4j.TorrentFlags
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

/** What the picker / player UI shows while a torrent is being streamed. */
sealed interface TorrentStreamState {
    data object Idle : TorrentStreamState

    /** Metadata / first pieces: [bufferPercent] is the progress of the initial buffer (0..100). */
    data class Preparing(val phase: String, val peers: Int = 0, val speedBps: Long = 0L, val bufferPercent: Int = 0) : TorrentStreamState

    data class Streaming(
        val fileName: String,
        val fileSize: Long,
        val bufferedAheadBytes: Long,
        val downloadedBytes: Long,
        val speedBps: Long,
        val peers: Int,
        val seeds: Int,
        /** The player is waiting for a piece that has not arrived yet. */
        val waitingForData: Boolean
    ) : TorrentStreamState

    data class Error(val message: String) : TorrentStreamState
}

/** The result of starting a stream: [url] is the loopback address to hand to the player. */
data class TorrentStreamStart(
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val torrentName: String,
    /** Subtitle files of the torrent that belong to this video (served by the same loopback server). */
    val subtitles: List<MediaSubtitle>
)

/**
 * Plays a torrent (magnet / .torrent) while it downloads. Used by the torrent picker (Play button) and by provider links
 * (providers/playback/PluginStreamResolver.kt, providers/ProviderApiRepository.kt).
 */
object TorrentStreamer {
    private const val TAG = "TorrentStreamer"
    private const val START_TIMEOUT_MS = 120_000L
    private const val START_BUFFER_BYTES = 4L * 1024L * 1024L
    private const val PIECE_WAIT_TIMEOUT_MS = 150_000L
    private const val POLL_MS = 40L
    private const val TICK_MS = 1_000L
    private const val IDLE_TEARDOWN_MS = 480_000L
    private const val DEADLINE_STEP_MS = 100
    private const val MAX_DEADLINE_MS = 30_000
    private const val MIN_AHEAD_BYTES = 16L * 1024L * 1024L
    private const val MAX_AHEAD_BYTES = 160L * 1024L * 1024L
    private const val PROBE_WINDOW_PIECES = 6
    private const val MAX_SUBTITLE_BYTES = 8L * 1024L * 1024L
    private const val MAX_SUBTITLES = 16
    private const val FREE_SPACE_FULL_MODE_HEADROOM = 512L * 1024L * 1024L
    private const val FREE_SPACE_MIN_AT_START = 256L * 1024L * 1024L
    private const val FREE_SPACE_STOP_BELOW = 192L * 1024L * 1024L

    private val VIDEO_EXT = setOf("mkv", "mp4", "avi", "webm", "mov", "m4v", "ts", "m2ts", "mts", "flv", "wmv", "mpg", "mpeg", "3gp", "ogv", "ogm", "vob", "divx", "asf")
    private val AUDIO_EXT = setOf("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "mka", "ac3", "eac3", "dts", "wma", "ape", "alac")
    private val MP4_EXT = setOf("mp4", "m4v", "mov", "3gp", "f4v")
    private val SUBTITLE_EXT = setOf("srt", "ass", "ssa", "vtt", "sub", "smi")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startMutex = Mutex()
    private val pool = Executors.newCachedThreadPool { r -> Thread(r, "torrent-stream").apply { isDaemon = true } }
    private val random = SecureRandom()

    @Volatile private var active: Stream? = null

    private val _state = MutableStateFlow<TorrentStreamState>(TorrentStreamState.Idle)
    val state: StateFlow<TorrentStreamState> = _state.asStateFlow()

    fun isTorrentLink(link: String, type: String = ""): Boolean {
        val t = type.trim().lowercase()
        if (t == "torrent" || t == "magnet") return true
        if (link.startsWith("magnet:", ignoreCase = true)) return true
        return link.substringBefore('?').substringBefore('#').endsWith(".torrent", ignoreCase = true)
    }

    /** True for the files the picker offers a Play button for. */
    fun isPlayableName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXT || ext in AUDIO_EXT
    }

    private fun isVideoName(name: String) = name.substringAfterLast('.', "").lowercase() in VIDEO_EXT

    // ------------------------------------------------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------------------------------------------------

    /** Provider links: resolves the torrent, streams its biggest video file and returns the local URL. */
    suspend fun prepare(context: Context, source: String, isCancelled: () -> Boolean = { false }): String {
        val engine = TorrentCatalogs.engine(context)
        _state.value = TorrentStreamState.Preparing("Starting Torrent Engine...")
        val catalog = try {
            engine.prepareTorrent(source)
        } catch (t: Throwable) {
            _state.value = if (t is CancellationException) TorrentStreamState.Idle else TorrentStreamState.Error(t.message.orEmpty())
            throw t
        }
        val best = catalog.files.filter { isVideoName(it.name) }.maxByOrNull { it.size }
        if (best == null) {
            engine.discardPreparation(catalog.preparationId)
            _state.value = TorrentStreamState.Error("This torrent has no video file.")
            throw TorrentException("This torrent has no video file.")
        }
        return startFromCatalog(context, catalog.preparationId, best.index, isCancelled).url
    }

    /**
     * Streams [fileIndex] of the torrent the picker has prepared as [preparationId]. Returns once the first part of the file is
     * on disk (the player can start); the rest keeps downloading around the playback position.
     */
    suspend fun startFromCatalog(
        context: Context,
        preparationId: String,
        fileIndex: Int,
        isCancelled: () -> Boolean = { false }
    ): TorrentStreamStart = startMutex.withLock {
        stopNow()
        val detached = TorrentCatalogs.engine(context).detachForStreaming(preparationId)
            ?: run {
                _state.value = TorrentStreamState.Error("The torrent session expired. Open the torrent again.")
                throw TorrentException("The torrent session expired. Open the torrent again.")
            }
        var stream: Stream? = null
        try {
            stream = Stream(detached, fileIndex)
            stream.begin(context)
            waitForStart(stream, isCancelled)
            stream.startTicker()
            active = stream
            val start = TorrentStreamStart(
                url = stream.mainUrl,
                fileName = stream.fileName,
                fileSize = stream.main.size,
                torrentName = detached.torrentName,
                subtitles = stream.subtitleParts.map { MediaSubtitle(it.title, it.language, stream.urlFor(it)) }
            )
            _state.value = TorrentStreamState.Streaming(stream.fileName, stream.main.size, 0L, 0L, 0L, 0, 0, true)
            start
        } catch (c: CancellationException) {
            closeFailed(stream, detached)
            _state.value = TorrentStreamState.Idle
            throw c
        } catch (t: Throwable) {
            closeFailed(stream, detached)
            val safe = (t as? TorrentException) ?: TorrentException("Couldn't start torrent streaming.", t)
            if (t is LinkageError) Log.e(TAG, "Torrent native runtime is unavailable", t) else Log.e(TAG, "Torrent stream failed (${t::class.java.simpleName})")
            _state.value = TorrentStreamState.Error(safe.message.orEmpty())
            throw safe
        }
    }

    /** The subtitle files (loopback URLs) that belong to the stream playing at [url]. */
    fun subtitlesFor(url: String): List<MediaSubtitle> {
        val s = active ?: return emptyList()
        if (!url.startsWith(s.baseUrl)) return emptyList()
        return s.subtitleParts.map { MediaSubtitle(it.title, it.language, s.urlFor(it)) }
    }

    /** True when [url] is the loopback address of the stream that is active now. */
    fun owns(url: String): Boolean = active?.let { url.startsWith(it.baseUrl) } == true

    /** Stops the active stream (connections, torrent, downloaded data). Safe to call from any thread, never blocks. */
    fun stop() {
        scope.launch { startMutex.withLock { stopNow() } }
    }

    /** Kept for the callers of the old streamer: removes the torrent of an earlier stream and its files. */
    fun releaseAll() = stop()

    private fun stopNow() {
        val s = active ?: return
        active = null
        s.close()
        _state.value = TorrentStreamState.Idle
    }

    private fun closeFailed(stream: Stream?, detached: TorrentCatalogEngine.DetachedTorrent) {
        if (stream != null) stream.close() else cleanupNative(detached)
    }

    private fun cleanupNative(d: TorrentCatalogEngine.DetachedTorrent) {
        if (runCatching { d.handle.isValid }.getOrDefault(false)) {
            runCatching { d.session.remove(d.handle, SessionHandle.DELETE_FILES) }
        }
        runCatching { d.session.stop() }
        runCatching { d.cacheDir.deleteRecursively() }
        runCatching { d.cacheDir.parentFile?.takeIf { it.isDirectory && it.list().isNullOrEmpty() }?.delete() }
    }

    private suspend fun waitForStart(stream: Stream, isCancelled: () -> Boolean) {
        val deadline = System.currentTimeMillis() + START_TIMEOUT_MS
        val need = min(START_BUFFER_BYTES, stream.main.size)
        var probed = false
        while (System.currentTimeMillis() < deadline) {
            if (isCancelled()) throw CancellationException("Torrent stopped")
            if (!stream.alive()) throw TorrentException("The torrent session stopped unexpectedly.")
            val have = stream.contiguousBytesFrom(0L, need)
            val st = stream.statusSnapshot()
            if (!probed && stream.havePieceSafe(stream.firstPiece)) {
                probed = true
                stream.refineContainerHints()
            }
            val percent = if (need > 0) ((have * 100L) / need).toInt().coerceIn(0, 100) else 100
            _state.value = TorrentStreamState.Preparing(
                phase = when {
                    st.peers == 0 -> "Searching for torrent peers..."
                    have == 0L -> "Connecting to peers..."
                    else -> "Buffering..."
                },
                peers = st.peers,
                speedBps = st.rate,
                bufferPercent = percent
            )
            if (have >= need) return
            delay(150L)
        }
        val peers = stream.statusSnapshot().peers
        throw TorrentException(if (peers == 0) "No peers were found for this torrent." else "The torrent is too slow to start playing.")
    }

    // ------------------------------------------------------------------------------------------------------------
    // One stream: torrent session + loopback server + piece scheduler
    // ------------------------------------------------------------------------------------------------------------

    /** A file of the torrent that the loopback server can serve. */
    private class Part(
        val index: Int,
        val offset: Long,
        val size: Long,
        val path: String,
        val mime: String,
        val primary: Boolean,
        val title: String,
        val language: String
    )

    private class Conn(val primary: Boolean, var piece: Int) {
        val openedAt = System.nanoTime()
        @Volatile var scheduledEnd = piece - 1
        @Volatile var socket: Socket? = null
    }

    private class Snapshot(val peers: Int, val seeds: Int, val rate: Long)

    private class Stream(private val d: TorrentCatalogEngine.DetachedTorrent, fileIndex: Int) {
        private val handle = d.handle
        private val storage = d.info.files()
        private val pieceLen: Long = d.info.pieceLength().toLong()
        private val numPieces: Int = d.info.numPieces()
        private val closed = AtomicBoolean(false)
        private val token: String
        private val server: ServerSocket
        private val conns = ConcurrentHashMap.newKeySet<Conn>()
        private val schedLock = Any()
        private val openThreads = AtomicInteger(0)
        private val lastActivity = AtomicLong(System.currentTimeMillis())
        private val servedBytes = AtomicLong(0L)
        private val waiting = AtomicInteger(0)
        private var tickJob: Job? = null
        @Volatile private var lastRate = 0L
        @Volatile private var lastPeers = 0
        @Volatile private var lastSeeds = 0
        @Volatile private var tailFromPiece = -1
        private var fullMode = true

        val main: Part
        val firstPiece: Int
        val lastPiece: Int
        val fileName: String
        val subtitleParts: List<Part>
        val baseUrl: String
        val mainUrl: String

        init {
            val count = storage.numFiles()
            if (fileIndex < 0 || fileIndex >= count) throw TorrentException("The selected torrent file does not exist any more.")
            val path = storage.filePath(fileIndex)
            val size = storage.fileSize(fileIndex)
            if (size <= 0L) throw TorrentException("This torrent file is empty.")
            checkSafePath(path)
            val offset = storage.fileOffset(fileIndex)
            fileName = path.substringAfterLast('/').substringAfterLast('\\')
            main = Part(fileIndex, offset, size, path, mimeFor(fileName), true, fileName, "")
            firstPiece = (offset / pieceLen).toInt()
            lastPiece = ((offset + size - 1L) / pieceLen).toInt().coerceAtMost(numPieces - 1)
            subtitleParts = discoverSubtitles(count)

            val raw = ByteArray(16).also { random.nextBytes(it) }
            token = raw.joinToString("") { "%02x".format(it) }
            server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1")) // loopback only
            baseUrl = "http://127.0.0.1:${server.localPort}/"
            mainUrl = baseUrl + "v/$token/" + android.net.Uri.encode(fileName)
        }

        fun urlFor(p: Part): String =
            if (p.primary) mainUrl else baseUrl + "s/$token/${p.index}/" + android.net.Uri.encode(p.path.substringAfterLast('/'))

        private fun checkSafePath(path: String) {
            if (path.isBlank() || File(path).isAbsolute || path.replace('\\', '/').split('/').any { it == ".." || it.isEmpty() }) {
                throw TorrentException("Torrent metadata contains an unsafe file path.")
            }
        }

        private fun discoverSubtitles(count: Int): List<Part> {
            val dir = main.path.replace('\\', '/').substringBeforeLast('/', "")
            val base = fileName.substringBeforeLast('.').lowercase()
            var videos = 0
            for (i in 0 until count) if (isVideoName(storage.filePath(i)) && storage.fileSize(i) > 0L) videos++
            val out = ArrayList<Part>()
            for (i in 0 until count) {
                if (out.size >= MAX_SUBTITLES) break
                val path = storage.filePath(i)
                val name = path.substringAfterLast('/').substringAfterLast('\\')
                val ext = name.substringAfterLast('.', "").lowercase()
                val size = storage.fileSize(i)
                if (ext !in SUBTITLE_EXT || size <= 0L || size > MAX_SUBTITLE_BYTES || storage.padFileAt(i)) continue
                val norm = path.replace('\\', '/')
                val belongs = if (videos <= 1) true else {
                    val lower = norm.lowercase()
                    name.lowercase().startsWith(base) || (lower.startsWith(dir.lowercase()) && lower.contains("/$base/"))
                }
                if (!belongs) continue
                runCatching { checkSafePath(path) }.getOrNull() ?: continue
                val stem = name.substringBeforeLast('.')
                val language = stem.substringAfterLast('.', "").takeIf { it.length in 2..3 && it.all(Char::isLetter) }.orEmpty()
                out.add(Part(i, storage.fileOffset(i), size, path, "application/octet-stream", false, stem.ifBlank { name }, language))
            }
            return out
        }

        fun alive(): Boolean = !closed.get() && runCatching { handle.isValid }.getOrDefault(false)

        fun havePieceSafe(piece: Int): Boolean = runCatching { handle.havePiece(piece) }.getOrDefault(false)

        fun statusSnapshot(): Snapshot = runCatching {
            val st = handle.status()
            lastRate = st.downloadPayloadRate().toLong()
            lastPeers = st.numPeers()
            lastSeeds = st.numSeeds()
            Snapshot(lastPeers, lastSeeds, lastRate)
        }.getOrDefault(Snapshot(lastPeers, lastSeeds, lastRate))

        private fun pieceOf(relPos: Long, part: Part = main): Int =
            ((part.offset + relPos) / pieceLen).toInt().coerceIn(0, numPieces - 1)

        /** Number of bytes (up to [max]) that are available without a gap from [relPos] of the main file. */
        fun contiguousBytesFrom(relPos: Long, max: Long): Long {
            var pos = relPos
            val end = min(main.size, relPos + max)
            while (pos < end) {
                val p = pieceOf(pos)
                if (!havePieceSafe(p)) break
                val pieceEndAbs = (p + 1L) * pieceLen
                pos = min(end, pieceEndAbs - main.offset)
            }
            return (pos - relPos).coerceAtLeast(0L)
        }

        // ---- start-up: file selection, flags, head and tail hints --------------------------------------------

        fun begin(context: Context) {
            val free = d.cacheDir.usableSpace
            if (free < FREE_SPACE_MIN_AT_START) throw TorrentException("Not enough free storage: the device storage is almost full.")
            // libtorrent writes through memory-mapped files: a full disk ends the process (SIGBUS). With less space than the
            // whole file needs only the windows around the playback position are requested and the ticker watches the space.
            fullMode = free >= main.size + FREE_SPACE_FULL_MODE_HEADROOM

            val priorities = Array(storage.numFiles()) { Priority.IGNORE }
            if (fullMode) priorities[main.index] = Priority.TOP_PRIORITY
            for (s in subtitleParts) priorities[s.index] = Priority.TOP_PRIORITY
            handle.prioritizeFiles(priorities)
            if (fullMode) {
                handle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
                // the range keeps playback-ordered download inside the chosen file (same as mpvRx)
                runCatching { handle.setSequentialRange(firstPiece, lastPiece) }
            }
            // head: the container headers (tracks: every audio / subtitle stream is listed here)
            val headPieces = ((8L * 1024L * 1024L) / pieceLen).toInt().coerceIn(2, 16)
            val headEnd = min(lastPiece, firstPiece + headPieces - 1)
            for (p in firstPiece..headEnd) {
                handle.piecePriority(p, Priority.TOP_PRIORITY)
                handle.setPieceDeadline(p, (p - firstPiece) * DEADLINE_STEP_MS)
            }
            // subtitle files are tiny: ask for them right after the head
            for ((i, s) in subtitleParts.withIndex()) {
                val a = (s.offset / pieceLen).toInt()
                val b = ((s.offset + s.size - 1L) / pieceLen).toInt()
                for (p in a..b) {
                    handle.piecePriority(p, Priority.TOP_PRIORITY)
                    handle.setPieceDeadline(p, 1_500 + i * 50)
                }
            }
            // tail: MKV cues / AVI index / MP4 moov. Exact for MP4 once the head is known (refineContainerHints).
            val tailBytes = (main.size / 200L).coerceIn(4L * 1024L * 1024L, 24L * 1024L * 1024L)
            tailFromPiece = pieceOf(max(0L, main.size - tailBytes)).coerceAtLeast(firstPiece)
            applyTail()
            handle.unsetFlags(TorrentFlags.UPLOAD_MODE)
            handle.resume()
        }

        private fun applyTail() {
            if (tailFromPiece < 0) return
            var i = 0
            for (p in tailFromPiece..lastPiece) {
                if (!havePieceSafe(p)) {
                    runCatching {
                        handle.piecePriority(p, Priority.TOP_PRIORITY)
                        handle.setPieceDeadline(p, min(MAX_DEADLINE_MS, 3_000 + i * DEADLINE_STEP_MS))
                    }
                }
                i++
            }
        }

        /** MP4 / MOV: when the movie box sits behind the media data its position follows from the box headers at the start. */
        fun refineContainerHints() {
            if (fileName.substringAfterLast('.', "").lowercase() !in MP4_EXT) return
            val file = File(d.cacheDir, main.path)
            if (!file.isFile) return
            val tailStart = try {
                RandomAccessFile(file, "r").use { mp4MovieBoxStart(it) }
            } catch (_: Throwable) {
                null
            }
            synchronized(schedLock) {
                if (tailStart == null) {
                    // moov first (fast-start) or unknown: nothing is needed from the end
                    tailFromPiece = -1
                    return
                }
                val from = pieceOf(tailStart)
                val cap = ((48L * 1024L * 1024L) / pieceLen).toInt().coerceAtLeast(4)
                tailFromPiece = if (lastPiece - from + 1 <= cap) from else max(from, lastPiece - cap + 1)
                applyTail()
            }
        }

        /** Offset where the box after the media data starts, or null when the movie box is in front / cannot be told. */
        private fun mp4MovieBoxStart(raf: RandomAccessFile): Long? {
            var off = 0L
            var guard = 0
            val header = ByteArray(16)
            while (off < main.size && guard++ < 64) {
                if (!havePieceSafe(pieceOf(off)) || !havePieceSafe(pieceOf(min(main.size - 1, off + 15)))) return null
                raf.seek(off)
                if (raf.read(header, 0, 16) < 8) return null
                var size = readU32(header, 0)
                val type = String(header, 4, 4, Charsets.ISO_8859_1)
                var headerSize = 8L
                if (size == 1L) { size = readU64(header, 8); headerSize = 16L } else if (size == 0L) size = main.size - off
                if (type == "moov") return null
                if (size < headerSize) return null
                if (type == "mdat") {
                    val end = off + size
                    return if (end < main.size) end else null
                }
                off += size
            }
            return null
        }

        private fun readU32(b: ByteArray, o: Int): Long =
            ((b[o].toLong() and 0xff) shl 24) or ((b[o + 1].toLong() and 0xff) shl 16) or ((b[o + 2].toLong() and 0xff) shl 8) or (b[o + 3].toLong() and 0xff)

        private fun readU64(b: ByteArray, o: Int): Long = (readU32(b, o) shl 32) or readU32(b, o + 4)

        // ---- scheduling: a window of time-critical pieces per open connection ---------------------------------

        private fun aheadPieces(): Int {
            val bytes = (lastRate * 30L).coerceIn(MIN_AHEAD_BYTES, MAX_AHEAD_BYTES)
            return ((bytes + pieceLen - 1L) / pieceLen).toInt().coerceIn(4, 256)
        }

        /** Requests the pieces from [c.piece] on (window of [n]) with deadlines; the older ones of the window keep theirs. */
        private fun fill(c: Conn, n: Int) {
            val want = min(lastPiece, c.piece + n - 1)
            var p = max(c.scheduledEnd + 1, c.piece)
            while (p <= want) {
                if (!havePieceSafe(p)) {
                    runCatching {
                        handle.piecePriority(p, Priority.TOP_PRIORITY)
                        handle.setPieceDeadline(p, min(MAX_DEADLINE_MS, (p - c.piece) * DEADLINE_STEP_MS))
                    }
                }
                p++
            }
            c.scheduledEnd = max(c.scheduledEnd, want)
        }

        /**
         * Rebuilds all deadlines from the open connections. Called when a connection opens or closes: after a seek the
         * player opens a new connection at the new position and closes the old one, so the old window is dropped and the
         * newest connection gets the big window (the others, e.g. head / tail probes, a small one).
         */
        private fun rebuild() {
            synchronized(schedLock) {
                if (closed.get()) return
                runCatching { handle.clearPieceDeadlines() }
                val ordered = conns.filter { it.primary }.sortedByDescending { it.openedAt }
                for ((i, c) in ordered.withIndex()) {
                    c.scheduledEnd = c.piece - 1
                    fill(c, if (i == 0) aheadPieces() else PROBE_WINDOW_PIECES)
                }
                applyTail()
            }
        }

        private fun onRead(c: Conn, relPos: Long) {
            if (!c.primary) return
            val piece = pieceOf(relPos)
            c.piece = piece
            lastActivity.set(System.currentTimeMillis())
            if (c.scheduledEnd - piece < aheadPieces() / 2) synchronized(schedLock) { if (!closed.get()) fill(c, aheadPieces()) }
        }

        // ---- ticker: window while paused, stats, free space, idle teardown ----------------------------------

        fun startTicker() {
            tickJob = scope.launch {
                while (isActive && !closed.get()) {
                    delay(TICK_MS)
                    try {
                        tick()
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        Log.w(TAG, "tick failed (${t::class.java.simpleName})")
                    }
                }
            }
        }

        private fun tick() {
            if (!alive()) {
                if (!closed.get() && active === this) stopFromTicker("The torrent session stopped.")
                return
            }
            val st = statusSnapshot()
            val newest = conns.filter { it.primary }.maxByOrNull { it.openedAt }
            if (newest != null) synchronized(schedLock) { if (!closed.get()) fill(newest, aheadPieces()) }

            // free storage: below the limit the stream is stopped instead of letting the native layer hit a full disk
            if (d.cacheDir.usableSpace < FREE_SPACE_STOP_BELOW) {
                runCatching { handle.pause() }
                stopFromTicker("Storage is almost full, so the torrent was stopped.")
                return
            }
            val idleFor = System.currentTimeMillis() - lastActivity.get()
            if (conns.isEmpty() && idleFor > IDLE_TEARDOWN_MS) {
                stopFromTicker(null)
                return
            }
            val ahead = if (newest != null) contiguousBytesFrom(((newest.piece * pieceLen) - main.offset).coerceAtLeast(0L), 96L * 1024L * 1024L) else 0L
            val downloaded = runCatching { handle.fileProgress().getOrNull(main.index) ?: 0L }.getOrDefault(0L)
            if (active === this) {
                _state.value = TorrentStreamState.Streaming(fileName, main.size, ahead, downloaded.coerceAtLeast(0L), st.rate, st.peers, st.seeds, waiting.get() > 0)
            }
        }

        private fun stopFromTicker(error: String?) {
            // the same path as stop(): the mutex serialises it with a new stream starting
            scope.launch {
                startMutex.withLock {
                    if (active === this@Stream) {
                        active = null
                        close()
                        _state.value = if (error == null) TorrentStreamState.Idle else TorrentStreamState.Error(error)
                    }
                }
            }
        }

        // ---- the loopback HTTP server --------------------------------------------------------------------------

        init {
            pool.execute {
                while (!closed.get() && !server.isClosed) {
                    val sock = try { server.accept() } catch (_: IOException) { break }
                    openThreads.incrementAndGet()
                    pool.execute {
                        try { serve(sock) } finally { openThreads.decrementAndGet() }
                    }
                }
            }
        }

        private fun readLine(input: BufferedInputStream): String? {
            val sb = StringBuilder()
            while (true) {
                val c = input.read()
                if (c < 0) return if (sb.isEmpty()) null else sb.toString()
                if (c == '\n'.code) return sb.toString().trimEnd('\r')
                sb.append(c.toChar())
                if (sb.length > 8192) return null
            }
        }

        private fun reply(out: OutputStream, status: String, extra: String = "", body: String = "") {
            val bytes = body.toByteArray()
            out.write("HTTP/1.1 $status\r\nContent-Type: text/plain\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n$extra\r\n".toByteArray())
            out.write(bytes)
            out.flush()
        }

        private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
            "mp4", "m4v", "f4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "ts", "m2ts", "mts" -> "video/mp2t"
            "flv" -> "video/x-flv"
            "wmv", "asf" -> "video/x-ms-wmv"
            "mpg", "mpeg", "vob" -> "video/mpeg"
            "3gp" -> "video/3gpp"
            "ogv", "ogm" -> "video/ogg"
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "m4a", "alac" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg", "opus" -> "audio/ogg"
            "wav" -> "audio/wav"
            "mka" -> "audio/x-matroska"
            else -> "application/octet-stream"
        }

        private fun serve(sock: Socket) {
            var conn: Conn? = null
            try {
                sock.use {
                    sock.soTimeout = 120_000
                    lastActivity.set(System.currentTimeMillis())
                    val input = BufferedInputStream(sock.getInputStream())
                    val out = sock.getOutputStream()
                    val requestLine = readLine(input) ?: return
                    val parts = requestLine.split(' ')
                    if (parts.size < 2) return
                    val method = parts[0].uppercase()
                    var range: String? = null
                    var headerCount = 0
                    while (true) {
                        val h = readLine(input) ?: break
                        if (h.isEmpty()) break
                        if (++headerCount > 64) { reply(out, "431 Request Header Fields Too Large"); return }
                        if (h.startsWith("range:", ignoreCase = true)) range = h.substringAfter(':').trim()
                    }
                    if (method != "GET" && method != "HEAD") { reply(out, "405 Method Not Allowed", "Allow: GET, HEAD\r\n"); return }
                    val seg = parts[1].substringBefore('?').trim('/').split('/')
                    val part: Part? = when {
                        seg.size >= 2 && seg[0] == "v" && tokenOk(seg[1]) -> main
                        seg.size >= 3 && seg[0] == "s" && tokenOk(seg[1]) -> subtitleParts.firstOrNull { it.index == seg[2].toIntOrNull() }
                        else -> null
                    }
                    if (part == null) { reply(out, "404 Not Found", body = "Not found"); return }
                    if (!alive()) { reply(out, "503 Service Unavailable", body = "Torrent not ready"); return }

                    val size = part.size
                    var start = 0L
                    var end = size - 1
                    var partial = false
                    if (range != null) {
                        val v = range.removePrefix("bytes=").substringBefore(',').trim()
                        val dash = v.indexOf('-')
                        val a = if (dash >= 0) v.substring(0, dash).trim() else ""
                        val b = if (dash >= 0) v.substring(dash + 1).trim() else ""
                        if (a.isEmpty()) {
                            val suffix = b.toLongOrNull()?.takeIf { it > 0 }
                            if (suffix == null) { reply(out, "416 Range Not Satisfiable", "Content-Range: bytes */$size\r\n"); return }
                            start = max(0L, size - suffix)
                        } else {
                            start = a.toLongOrNull()?.takeIf { it in 0 until size }
                                ?: run { reply(out, "416 Range Not Satisfiable", "Content-Range: bytes */$size\r\n"); return }
                            if (b.isNotEmpty()) end = min(b.toLongOrNull() ?: (size - 1), size - 1)
                        }
                        if (end < start) { reply(out, "416 Range Not Satisfiable", "Content-Range: bytes */$size\r\n"); return }
                        partial = true
                    }
                    val length = end - start + 1
                    val head = StringBuilder()
                    head.append(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                    head.append("Content-Type: ").append(part.mime).append("\r\n")
                    head.append("Accept-Ranges: bytes\r\nContent-Length: ").append(length).append("\r\nConnection: close\r\n")
                    if (partial) head.append("Content-Range: bytes $start-$end/$size\r\n")
                    head.append("\r\n")
                    if (method == "HEAD") { out.write(head.toString().toByteArray()); out.flush(); return }

                    val c = Conn(part.primary, pieceOf(start, part))
                    c.socket = sock
                    conn = c
                    if (part.primary) { conns.add(c); rebuild() }

                    val file = File(d.cacheDir, part.path)
                    waitForFile(file)
                    out.write(head.toString().toByteArray())
                    RandomAccessFile(file, "r").use { raf ->
                        val buf = ByteArray(128 * 1024)
                        var pos = start
                        var remaining = length
                        var fp = -1L
                        while (remaining > 0 && !closed.get()) {
                            val readable = awaitReadable(part, pos, min(buf.size.toLong(), remaining))
                            if (readable <= 0L) return
                            if (fp != pos) { raf.seek(pos); fp = pos }
                            val n = raf.read(buf, 0, readable.toInt())
                            if (n <= 0) return
                            out.write(buf, 0, n)
                            pos += n; fp += n; remaining -= n
                            servedBytes.addAndGet(n.toLong())
                            if (part.primary) onRead(c, pos)
                        }
                        out.flush()
                    }
                }
            } catch (_: Throwable) {
                // the player closed the connection (seek / stop) or the stream was closed: nothing to report
            } finally {
                conn?.let { c ->
                    if (conns.remove(c) && !closed.get()) rebuild()
                }
                lastActivity.set(System.currentTimeMillis())
            }
        }

        private fun tokenOk(candidate: String): Boolean =
            MessageDigest.isEqual(candidate.toByteArray(), token.toByteArray())

        private fun waitForFile(file: File) {
            val deadline = System.currentTimeMillis() + 15_000L
            while (!file.isFile && System.currentTimeMillis() < deadline) {
                if (closed.get()) throw IOException("closed")
                Thread.sleep(POLL_MS)
            }
            if (!file.isFile) throw IOException("File not ready")
        }

        /** Blocks until the piece at [relPos] is on disk, then returns how many bytes (<= [max]) can be read without a gap. */
        private fun awaitReadable(part: Part, relPos: Long, max: Long): Long {
            val absStart = part.offset + relPos
            val first = (absStart / pieceLen).toInt()
            waitForPiece(first)
            var endAbs = (first + 1L) * pieceLen
            while (endAbs < absStart + max) {
                val next = (endAbs / pieceLen).toInt()
                if (next >= numPieces || !havePieceSafe(next)) break
                endAbs += pieceLen
            }
            return min(min(max, endAbs - absStart), part.size - relPos)
        }

        private fun waitForPiece(piece: Int) {
            if (havePieceSafe(piece)) return
            waiting.incrementAndGet()
            try {
                val started = System.currentTimeMillis()
                var lastKick = started
                var reannounced = false
                while (!havePieceSafe(piece)) {
                    if (closed.get() || !alive()) throw IOException("Torrent stream stopped")
                    val now = System.currentTimeMillis()
                    if (now - started > PIECE_WAIT_TIMEOUT_MS) throw IOException("Timed out waiting for torrent data")
                    lastActivity.set(now)
                    // slow / vanished peers: ask again for the piece, and after a while look for new peers
                    if (now - lastKick > 4_000L) {
                        lastKick = now
                        runCatching { handle.piecePriority(piece, Priority.TOP_PRIORITY); handle.setPieceDeadline(piece, 0) }
                    }
                    if (!reannounced && now - started > 20_000L) {
                        reannounced = true
                        runCatching { handle.forceReannounce() }
                    }
                    Thread.sleep(POLL_MS)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Torrent stream interrupted", e)
            } finally {
                waiting.decrementAndGet()
            }
        }

        // ---- shutdown --------------------------------------------------------------------------------------------

        fun close() {
            if (!closed.compareAndSet(false, true)) return
            tickJob?.cancel()
            runCatching { server.close() }
            conns.forEach { c -> runCatching { c.socket?.close() } }
            // let the reader threads leave before the native session goes away
            val deadline = System.currentTimeMillis() + 1_500L
            while (openThreads.get() > 0 && System.currentTimeMillis() < deadline) {
                try { Thread.sleep(25L) } catch (_: InterruptedException) { break }
            }
            cleanupNative(d)
        }
    }
}
