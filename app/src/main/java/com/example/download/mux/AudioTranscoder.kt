package com.example.download

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

/**
 * Most TVs / USB players can't decode Opus or Vorbis sound inside a video file: the picture plays, the sound is silent.
 * AAC plays everywhere. This converts such an audio stream to AAC (stereo / mono) in an .m4a file, which is then joined into the MKV.
 */
object AudioTranscoder {
    private const val TAG = "AudioTranscoder"
    private val tvUnfriendly = setOf("audio/opus", "audio/vorbis")

    /** MIME type of the first audio stream of [file], or null. */
    fun firstAudioMime(file: File): String? {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(file.absolutePath)
            (0 until ex.trackCount).asSequence().mapNotNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME) }.firstOrNull { it.startsWith("audio/") }
        } catch (_: Throwable) {
            null
        } finally {
            try { ex.release() } catch (_: Throwable) {}
        }
    }

    fun needsAac(file: File): Boolean = firstAudioMime(file) in tvUnfriendly

    /** true = [dst] holds the AAC version of the first audio stream of [src]. Any problem returns false (the original sound is then kept). */
    fun toAac(src: File, dst: File, onProgress: (Float) -> Unit = {}): Boolean {
        val ex = MediaExtractor()
        var dec: MediaCodec? = null
        var enc: MediaCodec? = null
        var mux: MediaMuxer? = null
        var muxStarted = false
        try {
            ex.setDataSource(src.absolutePath)
            var track = -1
            var inFmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; inFmt = f; break }
            }
            if (track < 0 || inFmt == null) return false
            val channels = if (inFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) inFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            val rate = if (inFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) inFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 48000
            if (channels !in 1..2) return false // surround sound: left as it is
            val durationUs = if (inFmt.containsKey(MediaFormat.KEY_DURATION)) inFmt.getLong(MediaFormat.KEY_DURATION) else 0L
            ex.selectTrack(track)

            dec = MediaCodec.createDecoderByType(inFmt.getString(MediaFormat.KEY_MIME)!!)
            dec.configure(inFmt, null, null, 0)
            dec.start()

            val encFmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, channels).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, if (channels == 1) 128_000 else 192_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }
            enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            enc.configure(encFmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            enc.start()

            dst.delete()
            mux = MediaMuxer(dst.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxTrack = -1
            val bytesPerFrame = 2 * channels
            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var decoderDone = false
            var encoderDone = false
            var lastProgressAt = System.currentTimeMillis()
            var lastReport = 0L

            fun drainEncoder() {
                while (true) {
                    val oi = enc.dequeueOutputBuffer(encInfo, 0)
                    when {
                        oi == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                        oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            muxTrack = mux.addTrack(enc.outputFormat)
                            mux.start()
                            muxStarted = true
                        }
                        oi >= 0 -> {
                            val buf = enc.getOutputBuffer(oi)
                            val isConfig = encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (buf != null && encInfo.size > 0 && !isConfig && muxStarted) {
                                buf.position(encInfo.offset)
                                buf.limit(encInfo.offset + encInfo.size)
                                mux.writeSampleData(muxTrack, buf, encInfo)
                                lastProgressAt = System.currentTimeMillis()
                            }
                            enc.releaseOutputBuffer(oi, false)
                            if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) { encoderDone = true; return }
                        }
                    }
                }
            }

            fun feedEncoder(data: ByteArray, ptsUs: Long, eos: Boolean) {
                var off = 0
                var eosSent = false
                while (off < data.size || (eos && !eosSent)) {
                    if (System.currentTimeMillis() - lastProgressAt > 30_000L) throw IllegalStateException("the audio encoder stopped answering")
                    val ii = enc.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val b: ByteBuffer = enc.getInputBuffer(ii)!!
                        b.clear()
                        val n = minOf(b.remaining(), data.size - off)
                        if (n > 0) b.put(data, off, n)
                        val pts = maxOf(0L, ptsUs) + (off / bytesPerFrame) * 1_000_000L / rate
                        val last = eos && off + n >= data.size
                        enc.queueInputBuffer(ii, 0, n, pts, if (last) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                        off += n
                        if (last) eosSent = true
                    }
                    drainEncoder()
                }
            }

            while (!encoderDone) {
                if (System.currentTimeMillis() - lastProgressAt > 30_000L) throw IllegalStateException("the audio converter stopped answering")
                if (!inputDone) {
                    val ii = dec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val b = dec.getInputBuffer(ii)!!
                        val n = ex.readSampleData(b, 0)
                        if (n < 0) {
                            dec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            dec.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                if (!decoderDone) {
                    val oi = dec.dequeueOutputBuffer(decInfo, 10_000)
                    if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val of = dec.outputFormat
                        val r = if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) of.getInteger(MediaFormat.KEY_SAMPLE_RATE) else rate
                        val c = if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) of.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else channels
                        if (r != rate || c != channels) throw IllegalStateException("the decoded audio format differs from the stream header")
                    } else if (oi >= 0) {
                        val ob = dec.getOutputBuffer(oi)
                        val data = ByteArray(if (ob != null) decInfo.size else 0)
                        if (ob != null && decInfo.size > 0) {
                            ob.position(decInfo.offset)
                            ob.limit(decInfo.offset + decInfo.size)
                            ob.get(data)
                        }
                        val eos = decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        dec.releaseOutputBuffer(oi, false)
                        feedEncoder(data, decInfo.presentationTimeUs, eos)
                        if (eos) decoderDone = true
                        lastProgressAt = System.currentTimeMillis()
                        val now = System.currentTimeMillis()
                        if (durationUs > 0L && now - lastReport > 700L) {
                            lastReport = now
                            onProgress((decInfo.presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f))
                        }
                    }
                }
                drainEncoder()
            }
            if (!muxStarted) return false
            mux.stop()
            muxStarted = false
            onProgress(1f)
            return dst.length() > 0L
        } catch (t: Throwable) {
            Log.w(TAG, "audio conversion failed", t)
            return false
        } finally {
            try { dec?.stop() } catch (_: Throwable) {}
            try { dec?.release() } catch (_: Throwable) {}
            try { enc?.stop() } catch (_: Throwable) {}
            try { enc?.release() } catch (_: Throwable) {}
            try { if (muxStarted) mux?.stop() } catch (_: Throwable) {}
            try { mux?.release() } catch (_: Throwable) {}
            try { ex.release() } catch (_: Throwable) {}
        }
    }
}
