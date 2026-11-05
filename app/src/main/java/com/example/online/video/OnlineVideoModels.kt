package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.Locale

/** One selectable video quality of an online video (e.g. "1080p60"). [url] is a direct stream URL. */
data class OnlineVideoQuality(
    val label: String,
    val height: Int,
    val fps: Int,
    val url: String,
    /** true = adaptive video-only stream (needs a separate audio track), false = muxed (audio inside). */
    val isVideoOnly: Boolean
)

data class OnlineAudioStreamInfo(
    val url: String,
    val title: String,
    val language: String,
    val bitrate: Int,
    val isOriginal: Boolean
)

data class OnlineChapterInfo(val title: String, val startMs: Long)

/** Everything the player needs to play one online video. */
data class OnlineVideoPlayback(
    val pageUrl: String,
    val title: String,
    val uploader: String,
    val durationSec: Long,
    val qualities: List<OnlineVideoQuality>,
    val audios: List<OnlineAudioStreamInfo>,
    val chapters: List<OnlineChapterInfo>,
    /** Every subtitle track the website lists for this video (none is switched on by itself). */
    val subtitles: List<com.example.util.media.MediaSubtitle> = emptyList()
)

data class OnlineWatchDetails(
    val title: String,
    val uploader: String,
    val uploaderUrl: String?,
    val avatarUrl: String?,
    val subscriberCount: Long,
    val isVerified: Boolean,
    val likeCount: Long,
    val dislikeCount: Long,
    val viewCount: Long,
    val uploadDateText: String?,
    val description: String,
    val related: List<OnlineTrackResult>
)

data class OnlineComment(
    val author: String,
    val avatarUrl: String?,
    val text: String,
    val likesText: String,
    val dateText: String,
    val isPinned: Boolean
)

data class OnlineCommentPage(
    val items: List<OnlineComment>,
    val total: Long,
    val nextPage: org.schabi.newpipe.extractor.Page?,
    internal val info: org.schabi.newpipe.extractor.comments.CommentsInfo?
)
