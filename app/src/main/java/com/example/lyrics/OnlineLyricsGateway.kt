package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.lyrics.LrcLibApiService
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsProvider
import com.example.preferences.AudioPreferences
import com.example.repository.lyrics.LyricsRepository
import com.example.repository.lyrics.OnlineLyricsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Application-facing bridge to the full MpvRx provider repository. */
object OnlineLyricsService {
    @Volatile private var repository: LyricsRepository? = null
    @Volatile private var preferences: AudioPreferences? = null
    @Volatile private var appContext: Context? = null

    private val apiClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(14, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    private fun repository(context: Context): LyricsRepository {
        val app = context.applicationContext
        val current = repository
        if (current != null && appContext === app) return current
        val prefs = AudioPreferences(app)
        appContext = app
        preferences = prefs
        return LyricsRepository(app, LrcLibApiService(apiClient), prefs).also { repository = it }
    }

    fun isAutoFetchEnabled(context: Context): Boolean = AudioPreferences(context.applicationContext).lyricsAutoFetchOnline.get()

    fun providers(): List<LyricsProvider> = LyricsProvider.available

    suspend fun fetch(
        context: Context,
        title: String?,
        artist: String?,
        durationMs: Long,
        album: String? = null,
        videoId: String? = null,
        isrc: String? = null,
        mediaPath: String? = null,
        provider: LyricsProvider? = null,
        forceRefresh: Boolean = false,
        forceOnline: Boolean = false,
    ): OnlineLyricsResult = withContext(Dispatchers.IO) {
        val titleValue = title?.takeIf { it.isNotBlank() }
        if (titleValue == null) {
            Log.d("Lyrics", "No usable title; provider lookup skipped")
            return@withContext OnlineLyricsResult()
        }
        val repo = repository(context)
        val result = repo.loadLyricsForTrack(
            mediaPath = mediaPath.orEmpty(),
            title = titleValue,
            artist = artist,
            durationSeconds = (durationMs / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            forceRefresh = forceRefresh,
            allowOnline = true,
            forceOnline = forceOnline,
            album = album,
            isrc = isrc,
            provider = provider,
            videoId = videoId,
        )
        val winner = result.onlineProvider ?: result.autoOnlineProvider
        val lyrics = result.onlineLyrics
        val onlineResult = OnlineLyricsResult(
            provider = winner,
            lyrics = lyrics,
            raced = result.autoOnlineLyrics != null,
            byProvider = result.onlineByProvider,
            missing = result.missingProviders,
            failed = result.failedProviders,
            embeddedLyrics = result.embeddedLyrics,
            selectedSource = result.selectedSource,
        )
        if (lyrics?.isValid() == true && winner != null) {
            Log.d("Lyrics", "WINNER: ${winner.label} (${quality(lyrics)})")
        } else {
            Log.d("Lyrics", "WINNER: none; ${result.missingProviders.size} missing, ${result.failedProviders.size} failed")
        }
        onlineResult
    }

    private fun quality(lyrics: Lyrics): String = when {
        lyrics.synced.orEmpty().any { !it.words.isNullOrEmpty() } -> "word-synced"
        !lyrics.synced.isNullOrEmpty() -> "line-synced"
        else -> "plain"
    }
}
