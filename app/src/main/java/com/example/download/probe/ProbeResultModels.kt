package com.example.download

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.util.NetworkSettings
import com.example.util.OnlineVideoService
import com.example.ytdlp.YtdlPreferences
import com.example.ytdlp.YtdlpManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/** A stream as yt-dlp lists it (nothing is guessed: every field comes from the website's own answer). */
data class RawFormat(
    val id: String,
    val url: String,
    val ext: String,
    val protocol: String,
    val vcodec: String,
    val acodec: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val sizeBytes: Long,
    val sizeApprox: Boolean,
    val tbr: Double,
    val abr: Double,
    val language: String,
    val note: String,
    val headers: Map<String, String>,
    val chunkSize: Long,
    val hdr: Boolean,
    val fragments: List<String>,
    val languagePreference: Int
) {
    // yt-dlp's own rule: only the word "none" says a stream is absent. A missing / unknown codec (very common on websites that
    // do not list the codecs) means the file may hold it, so a plain file with unknown codecs is a video WITH audio.
    // Treating "unknown" as "no audio" made those videos "video only": no audio choice was shown and the MKV step dropped the sound.
    val hasVideo: Boolean get() = !vcodec.equals("none", true)
    val hasAudio: Boolean get() = !acodec.equals("none", true)
}

data class VideoChoice(
    val key: String,
    val label: String,
    val height: Int,
    val fps: Int,
    val hdr: Boolean,
    val codecName: String,
    val ext: String,
    val sizeBytes: Long,
    val sizeApprox: Boolean,
    val muxed: Boolean,
    val format: RawFormat
)

data class AudioChoice(
    val key: String,
    val label: String,
    val language: String,
    val isOriginal: Boolean,
    val bitrateKbps: Int,
    val codecName: String,
    val ext: String,
    val sizeBytes: Long,
    val sizeApprox: Boolean,
    val format: RawFormat
)

data class LinkInfo(
    val pageUrl: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String,
    val durationSec: Long,
    val videos: List<VideoChoice>,
    /** One entry per audio track (language), best bitrate of each. The first one is the original track. */
    val audioTracks: List<AudioChoice>,
    /** Every audio-only stream of the ORIGINAL track, best first (the quality list of an audio download). */
    val audioQualities: List<AudioChoice>,
    val subtitles: List<PlanSubtitle>,
    val chapters: List<PlanChapter>,
    val isDirect: Boolean = false,
    val isAudioOnlySource: Boolean = false
)
