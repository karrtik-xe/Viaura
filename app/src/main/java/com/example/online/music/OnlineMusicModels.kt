package com.example.util

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.json.JSONObject
import org.json.JSONArray
import java.util.Locale

/**
 * One search result from Online Music search. Distinct from AudioTrackItem on purpose:
 * this is the raw search-result shape; OnlineMusicPanel converts it into an AudioTrackItem
 * only once the user taps it and a playable stream URL has been resolved.
 */
data class OnlineTrackResult(
    val videoUrl: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnailUrl: String?,
    val uploadDateText: String? = null,
    val uploadDateEpochMs: Long? = null,
    val viewCount: Long = 0L,
    val detectedFormat: String? = null,
    val detectedAudioType: String? = null,
    /** A fresh upload of a subscribed channel that the user has not opened yet (shows the NEW tag). */
    val isNew: Boolean = false
)

/**
 * Direct audio stream URL and related recommendation tracks resolved from StreamInfo.
 */
data class StreamInfoWithRelated(
    val streamUrl: String?,
    val relatedTracks: List<OnlineTrackResult>
)

data class OnlineChannelTrackPage(
    val items: List<OnlineTrackResult>,
    val nextPage: Page?,
    val hasNextPage: Boolean
)

data class OnlineChannelPlaylist(
    val playlistUrl: String,
    val name: String,
    val thumbnailUrl: String?,
    val trackCountText: String?
)

data class OnlineChannelSection(
    val id: String,
    val label: String,
    val linkHandler: ListLinkHandler
)

data class OnlineChannelDetail(
    val channelUrl: String,
    val name: String,
    val handle: String?,
    val avatarUrl: String?,
    val subscriberCountText: String?,
    val description: String?,
    val availableSections: List<OnlineChannelSection>,
    val initialTracks: OnlineChannelTrackPage,
    val playlists: List<OnlineChannelPlaylist>,
    val bannerUrl: String? = null,
    val isVerified: Boolean = false
)
