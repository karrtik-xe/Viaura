package com.example.download

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Joins separately downloaded streams (video, one or many audio tracks, subtitles) into ONE Matroska file without
 * re-encoding: every sample is copied as it is. Chapters and the cover picture are written into the file too.
 * Cues and a SeekHead are written, so seeking in the result is instant.
 */
object MkvMuxer {
    // ---------------- EBML helpers ----------------
    private fun idBytes(id: Long): ByteArray {
        var n = 1
        var t = id shr 8
        while (t > 0) { n++; t = t shr 8 }
        return ByteArray(n) { i -> ((id shr (8 * (n - 1 - i))) and 0xFF).toByte() }
    }

    private fun sizeBytes(len: Long): ByteArray {
        var n = 1
        while (n < 8 && len >= (1L shl (7 * n)) - 1) n++
        val v = len or (1L shl (7 * n))
        return ByteArray(n) { i -> ((v shr (8 * (n - 1 - i))) and 0xFF).toByte() }
    }

    private fun size8(len: Long): ByteArray {
        val b = ByteArray(8)
        b[0] = 0x01
        for (i in 1..7) b[i] = ((len shr (8 * (7 - i))) and 0xFF).toByte()
        return b
    }

    private fun uintBytes(v: Long): ByteArray {
        var n = 1
        while (n < 8 && (v ushr (8 * n)) != 0L) n++
        return ByteArray(n) { i -> ((v ushr (8 * (n - 1 - i))) and 0xFF).toByte() }
    }

    private fun uint8(v: Long): ByteArray = ByteArray(8) { i -> ((v ushr (8 * (7 - i))) and 0xFF).toByte() }

    private fun elem(id: Long, payload: ByteArray): ByteArray = idBytes(id) + sizeBytes(payload.size.toLong()) + payload
    private fun u(id: Long, v: Long): ByteArray = elem(id, uintBytes(v))
    private fun s(id: Long, v: String): ByteArray = elem(id, v.toByteArray(Charsets.UTF_8))
    private fun f64(id: Long, v: Double): ByteArray = elem(id, ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(v).array())
    private fun cat(vararg parts: ByteArray): ByteArray {
        val o = ByteArrayOutputStream()
        for (p in parts) o.write(p)
        return o.toByteArray()
    }

    private fun iso3(lang: String): String {
        val l = lang.trim().substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
        if (l.length == 3) return l
        if (l.isEmpty() || l == "und") return "und"
        return try { Locale(l).isO3Language.ifBlank { "und" } } catch (_: Throwable) { "und" }
    }

    // ---------------- track model ----------------
    private class Track(
        val number: Int,
        val type: Int,                 // 1 video, 2 audio, 17 subtitle
        val codecId: String,
        val codecPrivate: ByteArray?,
        val name: String,
        val language: String,
        val isDefault: Boolean,
        val width: Int = 0,
        val height: Int = 0,
        val channels: Int = 0,
        val sampleRate: Int = 0,
        val frameDurationNs: Long = 0L,
        val codecDelayNs: Long = 0L,
        val seekPreRollNs: Long = 0L,
        val colour: ByteArray? = null,
        val isAvc: Boolean = false,
        val isHevc: Boolean = false
    )

    private class SourceReader(val extractor: MediaExtractor, val trackMap: Map<Int, Track>, val sizeBytes: Long)

    class Cue(val startMs: Long, val endMs: Long, val text: String)

    private class SubTrack(val track: Track, val cues: List<Cue>, var next: Int = 0)

    // ---------------- codec mapping ----------------
    private fun csd(format: MediaFormat, key: String): ByteArray? {
        if (!format.containsKey(key)) return null
        val bb = format.getByteBuffer(key) ?: return null
        val dup = bb.duplicate()
        dup.rewind()
        val out = ByteArray(dup.remaining())
        dup.get(out)
        return out
    }

    private fun splitNals(data: ByteArray): List<ByteArray> {
        val starts = ArrayList<Int>()
        var i = 0
        while (i + 2 < data.size) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 1) { starts.add(i + 3); i += 3 } else i++
        }
        val out = ArrayList<ByteArray>()
        for (k in starts.indices) {
            var end = if (k + 1 < starts.size) starts[k + 1] - 3 else data.size
            if (k + 1 < starts.size && end > starts[k] && data[end - 1].toInt() == 0) end--
            if (end > starts[k]) out.add(data.copyOfRange(starts[k], end))
        }
        return out
    }

    private fun be16(v: Int): ByteArray = byteArrayOf(((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte())

    private fun avcC(c0: ByteArray?, c1: ByteArray?): ByteArray? {
        val nals = splitNals((c0 ?: ByteArray(0)) + (c1 ?: ByteArray(0)))
        val sps = nals.filter { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 7 }
        val pps = nals.filter { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 8 }
        if (sps.isEmpty() || pps.isEmpty() || sps[0].size < 4) return null
        val o = ByteArrayOutputStream()
        o.write(1); o.write(sps[0][1].toInt()); o.write(sps[0][2].toInt()); o.write(sps[0][3].toInt())
        o.write(0xFF)
        o.write(0xE0 or sps.size)
        for (x in sps) { o.write(be16(x.size)); o.write(x) }
        o.write(pps.size)
        for (x in pps) { o.write(be16(x.size)); o.write(x) }
        return o.toByteArray()
    }

    private fun unescape(nal: ByteArray): ByteArray {
        val o = ByteArrayOutputStream()
        var zeros = 0
        for (b in nal) {
            if (zeros >= 2 && b.toInt() == 3) { zeros = 0; continue }
            o.write(b.toInt())
            zeros = if (b.toInt() == 0) zeros + 1 else 0
        }
        return o.toByteArray()
    }

    private fun hvcC(c0: ByteArray?, c1: ByteArray?, c2: ByteArray?): ByteArray? {
        val nals = splitNals((c0 ?: ByteArray(0)) + (c1 ?: ByteArray(0)) + (c2 ?: ByteArray(0)))
        fun type(n: ByteArray) = (n[0].toInt() shr 1) and 0x3F
        val vps = nals.filter { it.size > 2 && type(it) == 32 }
        val sps = nals.filter { it.size > 15 && type(it) == 33 }
        val pps = nals.filter { it.size > 2 && type(it) == 34 }
        if (sps.isEmpty()) return null
        val r = unescape(sps[0]).drop(2).toByteArray()   // after the 2-byte NAL header
        if (r.size < 13) return null
        val o = ByteArrayOutputStream()
        o.write(1)
        o.write(r[1].toInt())                              // profile space / tier / profile idc
        o.write(r, 2, 4)                                   // compatibility flags
        o.write(r, 6, 6)                                   // constraint flags
        o.write(r[12].toInt())                             // level idc
        o.write(0xF0); o.write(0x00)
        o.write(0xFC)
        o.write(0xFD)                                      // 4:2:0
        val profile = r[1].toInt() and 0x1F
        val depth = if (profile == 2) 0xFA else 0xF8      // Main10 = 10 bit
        o.write(depth); o.write(depth)
        o.write(0); o.write(0)
        o.write(0x0F)
        val arrays = listOf(32 to vps, 33 to sps, 34 to pps).filter { it.second.isNotEmpty() }
        o.write(arrays.size)
        for ((t, list) in arrays) {
            o.write(0x80 or t)
            o.write(be16(list.size))
            for (n in list) { o.write(be16(n.size)); o.write(n) }
        }
        return o.toByteArray()
    }

    private fun aacConfig(sampleRate: Int, channels: Int): ByteArray {
        val rates = intArrayOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350)
        val idx = rates.indexOf(sampleRate).let { if (it < 0) 4 else it }
        val v = (2 shl 11) or (idx shl 7) or (channels.coerceIn(1, 15) shl 3)
        return be16(v)
    }

    private fun opusHead(channels: Int, rate: Int): ByteArray {
        val bb = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("OpusHead".toByteArray()).put(1).put(channels.toByte()).putShort(312).putInt(rate).putShort(0).put(0)
        return bb.array()
    }

    private fun leLong(b: ByteArray?): Long {
        if (b == null || b.size < 8) return 0L
        return ByteBuffer.wrap(b, 0, 8).order(ByteOrder.nativeOrder()).long
    }

    private fun colourOf(format: MediaFormat): ByteArray? {
        if (!format.containsKey(MediaFormat.KEY_COLOR_TRANSFER) && !format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) return null
        val transfer = if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) format.getInteger(MediaFormat.KEY_COLOR_TRANSFER) else 0
        val standard = if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) format.getInteger(MediaFormat.KEY_COLOR_STANDARD) else 0
        val range = if (format.containsKey(MediaFormat.KEY_COLOR_RANGE)) format.getInteger(MediaFormat.KEY_COLOR_RANGE) else 0
        // Android: transfer 6 = ST2084 (PQ), 7 = HLG; standard 6 = BT.2020. Only HDR is written, plain SDR needs nothing.
        if (transfer != 6 && transfer != 7) return null
        val matrix = if (standard == 6) 9L else 1L
        val primaries = if (standard == 6) 9L else 1L
        val tc = if (transfer == 6) 16L else 18L
        val r = if (range == 1) 2L else if (range == 2) 1L else 0L
        return elem(0x55B0, cat(u(0x55B1, matrix), u(0x55B9, r), u(0x55BA, tc), u(0x55BB, primaries)))
    }

    private fun buildTrack(number: Int, format: MediaFormat, name: String, lang: String, isDefault: Boolean): Track {
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw UnsupportedMuxException("unknown stream type")
        fun int(key: String) = if (format.containsKey(key)) format.getInteger(key) else 0
        return when {
            mime.startsWith("video/") -> {
                val w = int(MediaFormat.KEY_WIDTH)
                val h = int(MediaFormat.KEY_HEIGHT)
                val fps = int(MediaFormat.KEY_FRAME_RATE)
                val dur = if (fps in 1..240) 1_000_000_000L / fps else 0L
                val c0 = csd(format, "csd-0")
                val c1 = csd(format, "csd-1")
                val c2 = csd(format, "csd-2")
                when (mime) {
                    "video/avc" -> Track(number, 1, "V_MPEG4/ISO/AVC", avcC(c0, c1) ?: throw UnsupportedMuxException("H.264 setup data is missing"), name, lang, isDefault, w, h, frameDurationNs = dur, isAvc = true)
                    "video/hevc" -> Track(number, 1, "V_MPEGH/ISO/HEVC", hvcC(c0, c1, c2) ?: throw UnsupportedMuxException("HEVC setup data is missing"), name, lang, isDefault, w, h, frameDurationNs = dur, colour = colourOf(format), isHevc = true)
                    "video/x-vnd.on2.vp9" -> Track(number, 1, "V_VP9", null, name, lang, isDefault, w, h, frameDurationNs = dur, colour = colourOf(format))
                    "video/x-vnd.on2.vp8" -> Track(number, 1, "V_VP8", null, name, lang, isDefault, w, h, frameDurationNs = dur)
                    "video/av01" -> Track(number, 1, "V_AV1", c0, name, lang, isDefault, w, h, frameDurationNs = dur, colour = colourOf(format))
                    "video/mp4v-es" -> Track(number, 1, "V_MPEG4/ISO/ASP", c0, name, lang, isDefault, w, h, frameDurationNs = dur)
                    else -> throw UnsupportedMuxException("the video codec $mime can't be joined")
                }
            }
            mime.startsWith("audio/") -> {
                val ch = int(MediaFormat.KEY_CHANNEL_COUNT).takeIf { it > 0 } ?: 2
                val rate = int(MediaFormat.KEY_SAMPLE_RATE).takeIf { it > 0 } ?: 48000
                val c0 = csd(format, "csd-0")
                when (mime) {
                    "audio/mp4a-latm" -> Track(number, 2, "A_AAC", c0 ?: aacConfig(rate, ch), name, lang, isDefault, channels = ch, sampleRate = rate)
                    "audio/opus" -> Track(
                        number, 2, "A_OPUS", c0 ?: opusHead(ch, rate), name, lang, isDefault, channels = ch, sampleRate = 48000,
                        codecDelayNs = leLong(csd(format, "csd-1")).takeIf { it in 1..100_000_000L } ?: 6_500_000L,
                        seekPreRollNs = leLong(csd(format, "csd-2")).takeIf { it in 1..1_000_000_000L } ?: 80_000_000L
                    )
                    "audio/mpeg" -> Track(number, 2, "A_MPEG/L3", null, name, lang, isDefault, channels = ch, sampleRate = rate)
                    "audio/ac3" -> Track(number, 2, "A_AC3", null, name, lang, isDefault, channels = ch, sampleRate = rate)
                    "audio/eac3" -> Track(number, 2, "A_EAC3", null, name, lang, isDefault, channels = ch, sampleRate = rate)
                    "audio/flac" -> Track(number, 2, "A_FLAC", c0 ?: throw UnsupportedMuxException("FLAC setup data is missing"), name, lang, isDefault, channels = ch, sampleRate = rate)
                    else -> throw UnsupportedMuxException("the audio codec $mime can't be joined")
                }
            }
            else -> throw UnsupportedMuxException("unsupported stream $mime")
        }
    }

    /** NAL units with start codes (what Android's extractor hands out) -> NAL units with 4-byte lengths (what Matroska stores). */
    private fun toLengthPrefixed(src: ByteArray, len: Int): ByteArray {
        val starts = ArrayList<Int>()
        var i = 0
        while (i + 2 < len) {
            if (src[i].toInt() == 0 && src[i + 1].toInt() == 0 && src[i + 2].toInt() == 1) { starts.add(i + 3); i += 3 } else i++
        }
        if (starts.isEmpty()) return src.copyOf(len)
        val out = ByteArrayOutputStream(len + starts.size)
        for (k in starts.indices) {
            var end = if (k + 1 < starts.size) starts[k + 1] - 3 else len
            if (k + 1 < starts.size && end > starts[k] && src[end - 1].toInt() == 0) end--
            val n = end - starts[k]
            if (n <= 0) continue
            out.write(byteArrayOf((n ushr 24).toByte(), (n ushr 16).toByte(), (n ushr 8).toByte(), n.toByte()))
            out.write(src, starts[k], n)
        }
        return out.toByteArray()
    }

    // ---------------- subtitles ----------------
    private fun parseTime(raw: String): Long {
        val t = raw.trim().replace(',', '.')
        val parts = t.split(':')
        return try {
            when (parts.size) {
                3 -> ((parts[0].toLong() * 3600 + parts[1].toLong() * 60) * 1000 + (parts[2].toDouble() * 1000).toLong())
                2 -> (parts[0].toLong() * 60 * 1000 + (parts[1].toDouble() * 1000).toLong())
                else -> -1L
            }
        } catch (_: Throwable) { -1L }
    }

    private fun cleanText(t: String): String =
        t.replace(Regex("<[^>]*>"), "").replace(Regex("\\{\\\\[^}]*\\}"), "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&quot;", "\"").replace("&#39;", "'")
            .trim()

    fun parseSrtOrVtt(text: String, dedupeRolling: Boolean): List<Cue> {
        val cues = ArrayList<Cue>()
        val blocks = text.replace("\r\n", "\n").replace('\r', '\n').split(Regex("\n{2,}"))
        var prevLines: List<String> = emptyList()
        for (b in blocks) {
            val lines = b.split('\n')
            val ti = lines.indexOfFirst { it.contains("-->") }
            if (ti < 0) continue
            val times = lines[ti].split("-->")
            if (times.size < 2) continue
            val start = parseTime(times[0])
            val end = parseTime(times[1].trim().substringBefore(' '))
            if (start < 0 || end < 0) continue
            var body = lines.drop(ti + 1).map { cleanText(it) }.filter { it.isNotBlank() }
            if (dedupeRolling) {
                val fresh = body.filter { it !in prevLines }
                prevLines = body
                body = fresh
            }
            if (body.isEmpty()) continue
            cues.add(Cue(start, maxOf(end, start + 1), body.joinToString("\n")))
        }
        return cues
    }

    private fun assTime(raw: String): Long {
        val m = Regex("(\\d+):(\\d+):(\\d+)[.:](\\d+)").find(raw.trim()) ?: return -1L
        val (h, mi, sec, cs) = m.destructured
        val frac = cs.padEnd(3, '0').take(3).toLong()
        return (h.toLong() * 3600 + mi.toLong() * 60 + sec.toLong()) * 1000 + frac
    }

    /** ASS/SSA: the header (everything up to the Events format line) and one block text per Dialogue line. */
    private fun parseAss(text: String): Pair<String, List<Cue>> {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val header = StringBuilder()
        val cues = ArrayList<Cue>()
        var inEvents = false
        var order = 0
        for (l in lines) {
            val t = l.trim()
            if (!inEvents) {
                header.append(l).append('\n')
                if (t.startsWith("Format:", true) && header.contains("[Events]")) inEvents = true
                continue
            }
            if (!t.startsWith("Dialogue:", true)) continue
            val f = t.substringAfter(':').trimStart().split(',', limit = 10)
            if (f.size < 10) continue
            val start = assTime(f[1])
            val end = assTime(f[2])
            if (start < 0 || end < 0) continue
            cues.add(Cue(start, maxOf(end, start + 1), "${order++},${f[0]},${f[3]},${f[4]},${f[5]},${f[6]},${f[7]},${f[8]},${f[9]}"))
        }
        return header.toString() to cues
    }

    // ---------------- the muxer ----------------
    /**
     * @param audioOnly the result holds no video (an audio download): only the audio track(s) of the sources are used.
     * @param tags extra file tags (title, artist, lyrics ...).
     */
    fun mux(
        sources: List<MuxSource>,
        subtitles: List<MuxSubtitle>,
        chapters: List<PlanChapter>,
        cover: ByteArray?,
        title: String,
        audioOnly: Boolean,
        tags: Map<String, String>,
        out: File,
        onProgress: (Float) -> Unit = {}
    ) {
        val readers = ArrayList<SourceReader>()
        val tracks = ArrayList<Track>()
        var number = 1
        var hasVideo = false
        try {
            for (src in sources) {
                val ex = MediaExtractor()
                try {
                    ex.setDataSource(src.file.absolutePath)
                } catch (t: Throwable) {
                    try { ex.release() } catch (_: Throwable) {}
                    throw UnsupportedMuxException("this stream can't be read (${t.message})")
                }
                val map = LinkedHashMap<Int, Track>()
                var videoTaken = false
                var audioTaken = 0
                try {
                for (i in 0 until ex.trackCount) {
                    val fmt = ex.getTrackFormat(i)
                    val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                    val isVideo = mime.startsWith("video/")
                    val isAudio = mime.startsWith("audio/")
                    val take = when (src.role) {
                        "video" -> isVideo && !videoTaken
                        "audio" -> isAudio && audioTaken == 0
                        else -> (isVideo && !videoTaken && !audioOnly) || isAudio
                    }
                    if (!take) continue
                    val isDefault = if (isAudio) (src.isDefault && audioTaken == 0) else true
                    val trackName = if (isAudio) src.name else ""
                    val t = buildTrack(number++, fmt, trackName, if (isAudio) src.language else "", isDefault)
                    ex.selectTrack(i)
                    map[i] = t
                    tracks.add(t)
                    if (isVideo) { videoTaken = true; hasVideo = true }
                    if (isAudio) audioTaken++
                }
                } catch (t: Throwable) {
                    try { ex.release() } catch (_: Throwable) {}
                    throw t
                }
                if (map.isEmpty()) {
                    ex.release()
                    continue
                }
                readers.add(SourceReader(ex, map, src.file.length()))
            }
            if (readers.isEmpty()) throw UnsupportedMuxException("no audio or video stream was found")

            // ---- subtitle tracks ----
            val subTracks = ArrayList<SubTrack>()
            for (sub in subtitles) {
                try {
                    val raw = sub.file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
                    val isAss = sub.ext.equals("ass", true) || sub.ext.equals("ssa", true)
                    if (isAss) {
                        val (header, cues) = parseAss(raw)
                        if (cues.isEmpty()) continue
                        val t = Track(number++, 17, "S_TEXT/ASS", header.toByteArray(Charsets.UTF_8), sub.name, sub.language, false)
                        tracks.add(t); subTracks.add(SubTrack(t, cues.sortedBy { it.startMs }))
                    } else {
                        val cues = parseSrtOrVtt(raw, sub.isAuto)
                        if (cues.isEmpty()) continue
                        val t = Track(number++, 17, "S_TEXT/UTF8", null, sub.name, sub.language, false)
                        tracks.add(t); subTracks.add(SubTrack(t, cues.sortedBy { it.startMs }))
                    }
                } catch (_: Throwable) {
                    // one broken subtitle file must not stop the video
                }
            }

            // ---- pass 1: the clusters go to a side file ----
            val clustersFile = File(out.path + ".clusters")
            val cuePoints = ArrayList<Triple<Long, Int, Long>>() // time ms, track, position in the clusters file
            var maxEndMs = 0L
            val totalBytes = readers.sumOf { it.sizeBytes }.coerceAtLeast(1L)
            var readBytes = 0L
            val cuesTrack = tracks.firstOrNull { it.type == 1 }?.number ?: tracks.first { it.type == 2 }.number
            val buffer = ByteBuffer.allocate(16 * 1024 * 1024)
            BufferedOutputStream(FileOutputStream(clustersFile), 1 shl 20).use { cout ->
                var position = 0L
                var cluster = ByteArrayOutputStream()
                var clusterStart = -1L
                var clusterHasKey = false
                fun flush() {
                    if (clusterStart < 0 || cluster.size() == 0) return
                    val body = cat(u(0xE7, clusterStart), cluster.toByteArray())
                    val bytes = elem(0x1F43B675, body)
                    if (clusterHasKey) cuePoints.add(Triple(clusterStart, cuesTrack, position))
                    cout.write(bytes)
                    position += bytes.size
                    cluster = ByteArrayOutputStream()
                    clusterStart = -1L
                    clusterHasKey = false
                }
                fun place(timeMs: Long, trackNumber: Int, flags: Int, data: ByteArray, isKeyOfCueTrack: Boolean, duration: Long, group: Boolean) {
                    val t = maxOf(0L, timeMs)
                    val rel = if (clusterStart < 0) 0L else t - clusterStart
                    val due = clusterStart >= 0 && (
                        (t - clusterStart >= 5000L && (!hasVideo || (trackNumber == cuesTrack && isKeyOfCueTrack))) ||
                            rel > 30000L || rel < -30000L || cluster.size() > 8 * 1024 * 1024
                        )
                    if (due) flush()
                    if (clusterStart < 0) clusterStart = t
                    val r = (t - clusterStart).toInt()
                    val head = byteArrayOf((0x80 or trackNumber).toByte(), ((r shr 8) and 0xFF).toByte(), (r and 0xFF).toByte(), flags.toByte())
                    if (group) {
                        cluster.write(elem(0xA0, cat(elem(0xA1, head + data), u(0x9B, duration))))
                    } else {
                        cluster.write(elem(0xA3, head + data))
                    }
                    if (trackNumber == cuesTrack && isKeyOfCueTrack) clusterHasKey = true
                    if (t + duration > maxEndMs) maxEndMs = t + duration
                }

                var lastProgress = 0f
                while (true) {
                    var bestReader = -1
                    var bestTime = Long.MAX_VALUE
                    for ((ri, rd) in readers.withIndex()) {
                        if (rd.extractor.sampleTrackIndex < 0) continue
                        val st = rd.extractor.sampleTime
                        if (st < bestTime) { bestTime = st; bestReader = ri }
                    }
                    var bestSub = -1
                    var bestSubTime = Long.MAX_VALUE
                    for ((si, st) in subTracks.withIndex()) {
                        val c = st.cues.getOrNull(st.next) ?: continue
                        val micros = c.startMs * 1000L
                        if (micros < bestSubTime) { bestSubTime = micros; bestSub = si }
                    }
                    if (bestReader < 0 && bestSub < 0) break
                    if (bestSub >= 0 && (bestReader < 0 || bestSubTime <= bestTime)) {
                        val st = subTracks[bestSub]
                        val c = st.cues[st.next++]
                        place(c.startMs, st.track.number, 0, c.text.toByteArray(Charsets.UTF_8), false, c.endMs - c.startMs, true)
                        continue
                    }
                    val rd = readers[bestReader]
                    val idx = rd.extractor.sampleTrackIndex
                    val track = rd.trackMap[idx]
                    if (track == null) { rd.extractor.advance(); continue }
                    buffer.clear()
                    val n = rd.extractor.readSampleData(buffer, 0)
                    if (n < 0) { rd.extractor.advance(); continue }
                    val flagsIn = rd.extractor.sampleFlags
                    val raw = ByteArray(n)
                    buffer.position(0)
                    buffer.get(raw, 0, n)
                    val isKey = (flagsIn and MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                    val data = if (track.isAvc || track.isHevc) toLengthPrefixed(raw, n) else raw
                    val flags = if (track.type == 1) (if (isKey) 0x80 else 0) else 0x80
                    place(bestTime / 1000L, track.number, flags, data, track.type == 1 && isKey || (track.number == cuesTrack && track.type != 1), 0L, false)
                    readBytes += n
                    rd.extractor.advance()
                    val p = (readBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    if (p - lastProgress >= 0.01f) { lastProgress = p; onProgress(p * 0.9f) }
                }
                flush()
            }

            // ---- pass 2: header elements + the clusters ----
            val info = elem(
                0x1549A966,
                cat(
                    u(0x2AD7B1, 1_000_000L), s(0x4D80, "Viaura"), s(0x5741, "Viaura"),
                    if (title.isNotBlank()) s(0x7BA9, title) else ByteArray(0),
                    f64(0x4489, maxEndMs.toDouble())
                )
            )
            val trackEntries = tracks.map { t ->
                val parts = ArrayList<ByteArray>()
                parts.add(u(0xD7, t.number.toLong()))
                parts.add(u(0x73C5, t.number.toLong() * 7919L))
                parts.add(u(0x83, t.type.toLong()))
                parts.add(u(0x88, if (t.isDefault) 1 else 0))
                parts.add(u(0x9C, 0))
                parts.add(s(0x86, t.codecId))
                t.codecPrivate?.let { if (it.isNotEmpty()) parts.add(elem(0x63A2, it)) }
                if (t.name.isNotBlank()) parts.add(s(0x536E, t.name))
                parts.add(s(0x22B59C, iso3(t.language)))
                if (t.language.isNotBlank() && t.language != "und") parts.add(s(0x22B59D, t.language))
                if (t.frameDurationNs > 0L) parts.add(u(0x23E383, t.frameDurationNs))
                if (t.codecDelayNs > 0L) parts.add(u(0x56AA, t.codecDelayNs))
                if (t.seekPreRollNs > 0L) parts.add(u(0x56BB, t.seekPreRollNs))
                when (t.type) {
                    1 -> parts.add(elem(0xE0, cat(u(0xB0, t.width.toLong()), u(0xBA, t.height.toLong()), t.colour ?: ByteArray(0))))
                    2 -> parts.add(elem(0xE1, cat(f64(0xB5, t.sampleRate.toDouble()), u(0x9F, t.channels.toLong()))))
                }
                elem(0xAE, cat(*parts.toTypedArray()))
            }
            val tracksEl = elem(0x1654AE6B, cat(*trackEntries.toTypedArray()))
            val chaptersEl = if (chapters.isEmpty()) null else elem(
                0x1043A770,
                elem(
                    0x45B9,
                    cat(*chapters.mapIndexed { i, c ->
                        val atom = ArrayList<ByteArray>()
                        atom.add(u(0x73C4, (i + 1).toLong()))
                        atom.add(u(0x91, c.startMs * 1_000_000L))
                        if (c.endMs > c.startMs) atom.add(u(0x92, c.endMs * 1_000_000L))
                        atom.add(elem(0x80, cat(s(0x85, c.title), s(0x437C, "eng"))))
                        elem(0xB6, cat(*atom.toTypedArray()))
                    }.toTypedArray())
                )
            )
            val attachmentsEl = if (cover == null || cover.isEmpty()) null else elem(
                0x1941A469,
                elem(0x61A7, cat(s(0x467E, "Cover"), s(0x466E, "cover.jpg"), s(0x4660, "image/jpeg"), u(0x46AE, 4242L), elem(0x465C, cover)))
            )
            val cleanTags = tags.filter { it.value.isNotBlank() }
            val tagsEl = if (cleanTags.isEmpty()) null else elem(
                0x1254C367,
                elem(0x7373, cat(elem(0x63C0, ByteArray(0)), *cleanTags.map { (k, v) -> elem(0x67C8, cat(s(0x45A3, k), s(0x4487, v))) }.toTypedArray()))
            )

            fun cuesElement(base: Long): ByteArray = elem(
                0x1C53BB6B,
                cat(*cuePoints.map { (time, track, pos) ->
                    elem(0xBB, cat(u(0xB3, time), elem(0xB7, cat(u(0xF7, track.toLong()), elem(0xF1, uint8(base + pos))))))
                }.toTypedArray())
            )

            val placeholders = ArrayList<Pair<Long, ByteArray?>>()
            placeholders.add(0x1549A966L to info)
            placeholders.add(0x1654AE6BL to tracksEl)
            placeholders.add(0x1043A770L to chaptersEl)
            placeholders.add(0x1941A469L to attachmentsEl)
            placeholders.add(0x1254C367L to tagsEl)
            val hasCues = cuePoints.isNotEmpty()
            val present = placeholders.filter { it.second != null }.map { it.first to it.second!! }.toMutableList()
            if (hasCues) present.add(0x1C53BB6BL to cuesElement(0L))
            fun seekHead(positions: List<Long>): ByteArray = elem(
                0x114D9B74,
                cat(*present.mapIndexed { i, p -> elem(0x4DBB, cat(elem(0x53AB, idBytes(p.first)), elem(0x53AC, uint8(positions[i])))) }.toTypedArray())
            )
            val seekLen = seekHead(List(present.size) { 0L }).size.toLong()
            val positions = ArrayList<Long>()
            var cursor = seekLen
            for (p in present) { positions.add(cursor); cursor += p.second.size }
            val headerLen = cursor
            val finalElements = present.map { p -> if (p.first == 0x1C53BB6BL) cuesElement(headerLen) else p.second }
            val clusterLen = clustersFile.length()
            val segmentPayload = headerLen + clusterLen
            val ebmlHeader = elem(
                0x1A45DFA3,
                cat(u(0x4286, 1), u(0x42F7, 1), u(0x42F2, 4), u(0x42F3, 8), s(0x4282, "matroska"), u(0x4287, 4), u(0x4285, 2))
            )
            FileOutputStream(out).use { fo ->
                val head = ByteArrayOutputStream()
                head.write(ebmlHeader)
                head.write(idBytes(0x18538067L)); head.write(size8(segmentPayload))
                head.write(seekHead(positions))
                for (e in finalElements) head.write(e)
                fo.write(head.toByteArray())
                // the clusters (the whole video) are handed over by the kernel instead of being copied through a Java buffer
                FastFileCopy.copy(clustersFile, fo)
            }
            clustersFile.delete()
            onProgress(1f)
        } finally {
            for (r in readers) try { r.extractor.release() } catch (_: Throwable) {}
            try { File(out.path + ".clusters").delete() } catch (_: Throwable) {}
        }
    }
}
