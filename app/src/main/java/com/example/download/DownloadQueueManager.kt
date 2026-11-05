package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log
import com.example.download.AudioTagger
import com.example.download.CancelToken
import com.example.download.DownloadPlan
import com.example.download.FetchCancelled
import com.example.download.HttpFetch
import com.example.download.LinkExpiredException
import com.example.download.MediaProbe
import com.example.download.ContainerSniffer
import com.example.download.MkvMuxer
import com.example.download.MuxSource
import com.example.download.MuxSubtitle
import com.example.download.PlanPart
import com.example.download.SegmentFetch
import com.example.torrent.TorrentDownloadEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * One coordinator for every download of the app: Online Videos, Online Audio, links pasted in Local Network, Media
 * (provider) videos and torrents. Streams are fetched with several connections at once, every finished piece is kept on
 * disk, so Pause / Stop / app restart / a lost connection continue from the same point. While the finished file is put
 * together (PROCESSING) nothing can interrupt it.
 */
object DownloadManager {
    private const val TAG = "DownloadManager"
    private const val MAX_CONCURRENT = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val permits = Semaphore(MAX_CONCURRENT)
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val tokens = ConcurrentHashMap<String, CancelToken>()
    private val cancellationFlags = ConcurrentHashMap<String, Boolean>()

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    private fun baseWorkDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads").apply { mkdirs() }

    private fun taskDir(context: Context, id: String): File = File(baseWorkDir(context), id).apply { mkdirs() }

    private fun isAudioFile(name: String): Boolean =
        name.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase() in
            setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac", "oga", "weba", "mka", "wma", "ac3")

    // ---------------- keep the phone's network awake while downloading ----------------
    private val runningTransfers = AtomicInteger(0)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    @Synchronized
    private fun acquireLocks(app: Context) {
        if (runningTransfers.getAndIncrement() > 0) return
        try {
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "viaura:download").apply {
                setReferenceCounted(false)
                acquire(6 * 3600_000L)
            }
        } catch (_: Throwable) {}
        try {
            val wm = app.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val lock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "viaura:download")
            lock.setReferenceCounted(false)
            lock.acquire()
            wifiLock = lock
        } catch (_: Throwable) {}
    }

    @Synchronized
    private fun releaseLocks() {
        if (runningTransfers.decrementAndGet() > 0) return
        runningTransfers.set(0)
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Throwable) {}
        try { wifiLock?.let { if (it.isHeld) it.release() } } catch (_: Throwable) {}
        wakeLock = null
        wifiLock = null
    }

    // ---------------- enqueue ----------------
    /** Adds a download of a prepared [plan]. Returns a short message for the user. */
    fun enqueuePlan(
        context: Context,
        plan: DownloadPlan,
        type: DownloadSourceType,
        detail: String,
        totalEstimate: Long
    ): String {
        val app = context.applicationContext
        DownloadStore.init(app)
        if (plan.parts.isEmpty()) return "Nothing to download"
        val id = newId()
        DownloadStore.add(
            app,
            DownloadItem(
                id = id,
                source = plan.pageUrl,
                title = plan.title.ifBlank { "Download" },
                subtitle = plan.uploader.takeIf { it.isNotBlank() },
                isAudio = plan.isAudio,
                totalBytes = totalEstimate.coerceAtLeast(0L),
                thumbnailUrl = plan.thumbnailUrl.takeIf { it.isNotBlank() },
                sourceType = type,
                planJson = plan.toJson(),
                detail = detail,
                group = plan.group.takeIf { it.isNotBlank() }
            )
        )
        start(app, id)
        return "Download started"
    }

    /** Download the selected file from a torrent. */
    fun enqueueTorrent(
        context: Context,
        source: String,
        fileIndex: Int,
        fileName: String,
        sizeBytes: Long,
        torrentName: String?
    ): String {
        val app = context.applicationContext
        val src = source.trim()
        if (src.isBlank() || !com.example.torrent.isTorrentSource(src)) return "Invalid torrent source"
        if (fileIndex < 0) return "Choose a file from the torrent first"

        DownloadStore.init(app)
        val id = newId()
        DownloadStore.add(
            app,
            DownloadItem(
                id = id,
                source = src,
                title = fileName.ifBlank { torrentName ?: src },
                subtitle = torrentName?.takeIf { it.isNotBlank() && it != fileName },
                isAudio = isAudioFile(fileName),
                totalBytes = sizeBytes.coerceAtLeast(0L),
                sourceType = DownloadSourceType.TORRENT,
                torrentFileIndex = fileIndex
            )
        )
        start(app, id)
        return "Torrent download started"
    }

    // ---------------- controls ----------------
    private fun interrupt(context: Context, id: String, status: DownloadStatus, note: String) {
        val app = context.applicationContext
        val item = DownloadStore.get(id) ?: return
        // the finished file is being put together: that is never interrupted
        if (!item.canInterrupt) return
        cancellationFlags[id] = true
        tokens[id]?.cancel()
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        DownloadStore.update(app, id, persist = true) {
            it.copy(status = status, speedBps = 0L, etaSec = -1L, note = note)
        }
    }

    fun pause(context: Context, id: String) = interrupt(context, id, DownloadStatus.PAUSED, "Paused")

    /** Stop keeps everything that was downloaded; Start continues from the same point. */
    fun stop(context: Context, id: String) = interrupt(context, id, DownloadStatus.STOPPED, "Stopped")

    fun resume(context: Context, id: String) {
        val app = context.applicationContext
        val item = DownloadStore.get(id) ?: return
        if (item.status == DownloadStatus.COMPLETED || item.isActive) return
        cancellationFlags.remove(id)
        start(app, id)
    }

    /** Removes an unfinished download (its pieces are deleted). */
    fun cancel(context: Context, id: String) {
        val app = context.applicationContext
        cancellationFlags[id] = true
        tokens[id]?.cancel()
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        DownloadStore.remove(app, id)
        scope.launch {
            delay(600L)
            try { taskDir(app, id).deleteRecursively() } catch (_: Throwable) {}
        }
    }

    /** Delete: a finished download loses its file, an unfinished one its pieces. */
    fun remove(context: Context, id: String) {
        val app = context.applicationContext
        val item = DownloadStore.get(id) ?: return
        if (item.status == DownloadStatus.COMPLETED) {
            DownloadFileSaver.delete(app, item)
            DownloadStore.remove(app, id)
        } else {
            cancel(app, id)
        }
    }

    /** At app start: everything that was running when the app was closed continues from where it stopped. */
    fun resumeInterrupted(context: Context) {
        val app = context.applicationContext
        DownloadStore.init(app)
        for (id in DownloadStore.takeInterrupted()) start(app, id)
    }

    // ---------------- running ----------------
    private fun start(app: Context, id: String) {
        val item = DownloadStore.get(id) ?: return
        if (activeJobs[id]?.isActive == true) return
        cancellationFlags.remove(id)
        DownloadStore.update(app, id) {
            it.copy(status = DownloadStatus.QUEUED, speedBps = 0L, etaSec = -1L, note = "Queued…")
        }
        DownloadForegroundService.start(app)

        val isTorrent = item.sourceType == DownloadSourceType.TORRENT
        activeJobs[id] = scope.launch {
            try {
                permits.withPermit {
                    if (cancellationFlags[id] == true) return@withPermit
                    DownloadStore.update(app, id) { it.copy(status = DownloadStatus.DOWNLOADING, note = null) }
                    acquireLocks(app)
                    try {
                        if (isTorrent) executeTorrent(app, id) else executePlan(app, id)
                    } finally {
                        releaseLocks()
                    }
                }
            } catch (_: CancellationException) {
                // Pause / Stop / Delete: the pieces stay on disk
            } catch (error: Throwable) {
                Log.e(TAG, "Download failed for $id", error)
                fail(app, id, error)
            } finally {
                activeJobs.remove(id)
                tokens.remove(id)
            }
        }
    }

    private suspend fun executeTorrent(app: Context, id: String) {
        val item = DownloadStore.get(id) ?: return
        val workDir = taskDir(app, id)
        val isCancelled = { cancellationFlags[id] == true }
        val file = TorrentDownloadEngine.download(
            context = app,
            source = item.source,
            fileIndex = item.torrentFileIndex,
            saveDir = workDir,
            isCancelled = isCancelled
        ) { progress ->
            DownloadStore.update(app, id) {
                if (it.status == DownloadStatus.DOWNLOADING) {
                    it.copy(
                        downloadedBytes = progress.downloadedBytes,
                        totalBytes = if (progress.totalBytes > 0L) progress.totalBytes else it.totalBytes,
                        speedBps = progress.speedBps,
                        note = progress.phase
                    )
                } else it
            }
        } ?: throw IllegalStateException("Torrent download failed")

        if (isCancelled()) return
        DownloadStore.update(app, id, persist = true) { it.copy(status = DownloadStatus.PROCESSING, note = "Saving file…", speedBps = 0L, etaSec = -1L) }
        val extension = file.extension
        val safeName = DownloadFileSaver.safeName(item.title)
        val publicName = if (extension.isBlank() || safeName.endsWith(".$extension", true)) safeName else "$safeName.$extension"
        val saved = withContext(Dispatchers.IO) {
            DownloadFileSaver.saveToPublic(app, file, publicName, item.isAudio)
        } ?: throw IllegalStateException("Could not save torrent file")
        finish(app, id, saved)
        try { workDir.deleteRecursively() } catch (_: Throwable) {}
    }

    private fun finish(app: Context, id: String, saved: DownloadFileSaver.Saved) {
        DownloadStore.update(app, id, persist = true) {
            it.copy(
                status = DownloadStatus.COMPLETED, downloadedBytes = saved.size, totalBytes = saved.size, speedBps = 0L,
                etaSec = -1L, note = null, filePath = saved.path, fileUri = saved.uri
            )
        }
        DownloadStore.get(id)?.let { DownloadForegroundService.notifyDone(app, it) }
    }

    private fun fail(app: Context, id: String, error: Throwable) {
        val message = error.message?.trim()?.lineSequence()?.firstOrNull { it.isNotBlank() }
            ?: error.javaClass.simpleName
        DownloadStore.update(app, id, persist = true) {
            it.copy(status = DownloadStatus.FAILED, speedBps = 0L, etaSec = -1L, note = message.take(120))
        }
        DownloadStore.get(id)?.let { DownloadForegroundService.notifyFailed(app, it) }
    }

    private fun partFile(dir: File, index: Int, part: PlanPart): File {
        val ext = if (part.protocol == "http") part.ext.ifBlank { "bin" } else SegmentFetch.outputExt(part)
        return File(dir, "part$index.$ext")
    }

    /** Fresh addresses for the streams of [plan]: links of a website expire after a few hours. */
    private suspend fun refreshLinks(app: Context, plan: DownloadPlan): DownloadPlan? {
        val info = MediaProbe.probe(app, plan.pageUrl).getOrNull() ?: return null
        val formats = (info.videos.map { it.format } + info.audioTracks.map { it.format } + info.audioQualities.map { it.format }).distinctBy { it.id }
        val byId = formats.associateBy { it.id }
        var changed = false
        val parts = plan.parts.map { p ->
            val f = byId[p.formatId]
            if (f == null) p else {
                changed = true
                p.copy(url = f.url, headers = f.headers, fragments = f.fragments)
            }
        }
        return if (changed) plan.copy(parts = parts) else null
    }

    private fun httpBytes(app: Context, url: String, headers: Map<String, String>): ByteArray? = try {
        HttpFetch.call(app, HttpFetch.request(url, headers), null) { r ->
            if (r.code !in 200..299) null else r.body?.bytes()
        }
    } catch (_: Throwable) {
        null
    }

    private fun coverJpeg(app: Context, url: String): ByteArray? {
        val raw = httpBytes(app, url, emptyMap()) ?: return null
        return try {
            val bmp = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
            bmp.recycle()
            out.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    private suspend fun executePlan(app: Context, id: String) {
        val item = DownloadStore.get(id) ?: return
        var plan = DownloadPlan.fromJson(item.planJson ?: "") ?: throw IllegalStateException("This download can't be continued. Please add it again.")
        val workDir = taskDir(app, id)
        val token = CancelToken()
        tokens[id] = token
        val counters = plan.parts.map { AtomicLong(0L) }
        val totals = plan.parts.map { AtomicLong(maxOf(it.sizeBytes, 0L)) }

        coroutineScope {
            // progress, speed and time left
            val ticker = launch {
                var last = System.nanoTime()
                var lastBytes = 0L
                var speed = 0.0
                while (isActive) {
                    delay(700L)
                    val now = System.nanoTime()
                    val done = counters.sumOf { it.get() }
                    val dt = (now - last) / 1e9
                    if (dt > 0.0) {
                        val inst = ((done - lastBytes).coerceAtLeast(0L)) / dt
                        speed = if (speed == 0.0) inst else speed * 0.65 + inst * 0.35
                    }
                    last = now
                    lastBytes = done
                    val allKnown = totals.all { it.get() > 0L }
                    val knownTotal = totals.sumOf { it.get() }
                    val currentSpeed = speed
                    DownloadStore.update(app, id) {
                        if (it.status != DownloadStatus.DOWNLOADING) it else {
                            val total = if (allKnown) knownTotal else maxOf(it.totalBytes, done)
                            val eta = if (currentSpeed > 1.0 && total > done) ((total - done) / currentSpeed).toLong() else -1L
                            it.copy(downloadedBytes = done, totalBytes = total, speedBps = currentSpeed.toLong(), etaSec = eta, note = null)
                        }
                    }
                }
            }
            try {
                var refreshes = 0
                while (true) {
                    try {
                        val current = plan
                        coroutineScope {
                            current.parts.mapIndexed { i, part ->
                                async(Dispatchers.IO) {
                                    val dest = partFile(workDir, i, part)
                                    if (part.protocol == "http") {
                                        HttpFetch.download(app, part, dest, token, counters[i], totals[i], 4)
                                    } else {
                                        SegmentFetch.download(app, part, dest, token, counters[i], 6)
                                    }
                                }
                            }.awaitAll()
                        }
                        break
                    } catch (e: LinkExpiredException) {
                        refreshes++
                        if (refreshes > 3) throw IllegalStateException("The website no longer gives this video. Open it again and start a new download.")
                        val fresh = refreshLinks(app, plan) ?: throw IllegalStateException("The link has expired. Open it again and start a new download.")
                        plan = fresh
                        DownloadStore.update(app, id, persist = true) { it.copy(planJson = fresh.toJson()) }
                    }
                }
            } finally {
                ticker.cancel()
            }
        }

        // from here on the transfer is over: PROCESSING can no longer be interrupted
        val doneBytes = counters.sumOf { it.get() }
        DownloadStore.update(app, id, persist = true) {
            it.copy(status = DownloadStatus.PROCESSING, downloadedBytes = maxOf(it.downloadedBytes, doneBytes), speedBps = 0L, etaSec = -1L, note = "Finishing…")
        }
        val finalPlan = plan
        withContext(Dispatchers.IO) { process(app, id, finalPlan, workDir) }
    }

    private fun process(app: Context, id: String, plan: DownloadPlan, workDir: File) {
        val title = DownloadFileSaver.safeName(plan.title).take(100).trim().trim('.').ifBlank { "download" }
        val partFiles = plan.parts.mapIndexed { i, p -> partFile(workDir, i, p) }
        val cover = if (plan.attachThumbnail) {
            listOf(plan.thumbnailUrl, plan.fallbackThumbnailUrl).filter { it.isNotBlank() }.distinct().firstNotNullOfOrNull { coverJpeg(app, it) }
        } else null
        val subtitleFiles = ArrayList<MuxSubtitle>()
        plan.subtitles.forEachIndexed { i, s ->
            val bytes = httpBytes(app, s.url, emptyMap()) ?: return@forEachIndexed
            val f = File(workDir, "sub$i.${s.ext.ifBlank { "vtt" }}")
            f.writeBytes(bytes)
            subtitleFiles.add(MuxSubtitle(f, s.language.removeSuffix("-orig"), s.name, s.ext, s.isAuto))
        }

        var lyrics = plan.lyrics
        if (plan.isAudio && plan.fetchLyrics && lyrics.isBlank()) {
            lyrics = try {
                val r = kotlinx.coroutines.runBlocking { LyricsFetcher.fetch(plan.title, plan.uploader, plan.durationSec) }
                r?.plainLyrics?.takeIf { it.isNotBlank() }
                    ?: r?.syncedLyrics?.lines()?.joinToString("\n") { it.replace(Regex("^\\[[^\\]]*\\]\\s*"), "") }.orEmpty()
            } catch (_: Throwable) { "" }
        }

        var out: File
        var finalName = ""
        var note: String? = null
        if (plan.isAudio) {
            val src = partFiles.first()
            val ext = src.extension.lowercase()
            val artist = plan.uploader
            val needsMka = plan.parts.first().role == "muxed" || ext == "webm" || ext == "weba"
            if (needsMka) {
                out = File(workDir, "out.mka")
                var name = "$title.mka"
                try {
                    MkvMuxer.mux(
                        listOf(MuxSource(src, "muxed", "", plan.parts.first().language, true)), emptyList(), emptyList(), cover, plan.title,
                        true, mapOf("TITLE" to plan.title, "ARTIST" to artist, "LYRICS" to lyrics), out
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "audio mux failed", t)
                    out.delete()
                    out = src
                    name = "$title.$ext"
                    note = "Saved without cover"
                }
                finalName = name
            } else {
                out = File(workDir, "out.$ext")
                src.copyTo(out, true)
                when (ext) {
                    "m4a", "mp4" -> AudioTagger.tagM4a(out, plan.title, artist, lyrics, cover)
                    "mp3" -> AudioTagger.tagMp3(out, plan.title, artist, lyrics, cover)
                    "flac" -> AudioTagger.tagFlac(out, plan.title, artist, lyrics, cover)
                }
                finalName = "$title.$ext"
            }
        } else {
            // Save videos in Matroska consistently so selected audio tracks, subtitles, chapters and cover art
            // can live in the same file. If the muxer rejects a codec, the safe original-stream fallback below applies.
            // A single finished video file from the provider / website (nothing to join, nothing to add) is saved EXACTLY as
            // it was downloaded. Re-muxing it would throw away its audio-track names / languages, subtitle tracks, chapters,
            // fonts and other attachments. A Matroska file is also kept untouched when extras were requested, for the same reason.
            val single = plan.parts.size == 1 && plan.parts.first().role == "muxed" && plan.parts.first().protocol == "http"
            val sniffed = if (single) ContainerSniffer.fromFile(partFiles.first()) else null
            val noExtras = subtitleFiles.isEmpty() && plan.chapters.isEmpty() && cover == null
            val keepOriginal = single && sniffed != null && sniffed != "ts" && (noExtras || sniffed == "mkv")
            if (keepOriginal) {
                out = partFiles.first()
                finalName = "$title.$sniffed"
                if (!noExtras) note = "Saved as the original MKV (all tracks kept)"
            } else {
                out = File(workDir, "out.mkv")
                val sources = plan.parts.mapIndexed { i, p ->
                    MuxSource(partFiles[i], p.role, p.label, p.language, p.isDefault || i == 0)
                }
                try {
                    var lastReport = 0L
                    MkvMuxer.mux(sources, subtitleFiles, plan.chapters, cover, plan.title, false, emptyMap(), out) { p ->
                        val now = System.currentTimeMillis()
                        if (now - lastReport > 800L) {
                            lastReport = now
                            DownloadStore.update(app, id) { it.copy(note = "Finishing… ${(p * 100).toInt()}%") }
                        }
                    }
                    finalName = "$title.mkv"
                } catch (t: Throwable) {
                    // the streams could not be joined (a codec Android can't read): nothing is lost, the files are saved as they are
                    Log.w(TAG, "mux failed", t)
                    out.delete()
                    var firstSaved: DownloadFileSaver.Saved? = null
                    plan.parts.forEachIndexed { i, p ->
                        val f = partFiles[i]
                        val nm = if (p.role == "audio") "$title (${p.label.ifBlank { "audio $i" }}).${f.extension}" else "$title.${f.extension}"
                        val s = DownloadFileSaver.saveToPublic(app, f, nm, p.role == "audio", plan.group) ?: throw IllegalStateException("Could not save the file")
                        if (firstSaved == null) firstSaved = s
                    }
                    val first = firstSaved
                    if (first != null) {
                        finish(app, id, first)
                        val fallbackNote = if (plan.parts.size > 1) "Saved as separate video and audio files" else "MKV mux unavailable; saved in original format"
                        DownloadStore.update(app, id, persist = true) { it.copy(note = fallbackNote) }
                    }
                    try { workDir.deleteRecursively() } catch (_: Throwable) {}
                    return
                }
            }
        }
        val saved = DownloadFileSaver.saveToPublic(app, out, finalName, plan.isAudio, plan.group) ?: throw IllegalStateException("Could not save the file")
        finish(app, id, saved)
        val finalNote = note
        if (finalNote != null) DownloadStore.update(app, id, persist = true) { it.copy(note = finalNote) }
        try { workDir.deleteRecursively() } catch (_: Throwable) {}
    }
}
