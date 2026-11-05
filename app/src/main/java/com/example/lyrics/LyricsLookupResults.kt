/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.example.repository.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.example.data.lyrics.LrcLibApiService
import com.example.data.lyrics.providers.BiniLyricsProvider
import com.example.data.lyrics.providers.LyricsFetchQuery
import com.example.data.lyrics.providers.LyricsProviderRegistry
import com.example.data.lyrics.providers.PaxSenixApi
import com.example.data.lyrics.providers.ProviderOutcome
import com.example.data.lyrics.providers.TrackRef
import com.example.data.lyrics.providers.withBackgroundVocals
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsProvider
import com.example.domain.lyrics.LyricsSourceType
import com.example.domain.lyrics.SyncedLine
import com.example.preferences.AudioPreferences
import com.example.utils.media.EmbeddedLyricsExtractor
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Lyrics for one track, plus which of its sources they came from. */
data class LyricsResult(
  val embeddedLyrics: Lyrics? = null,
  val onlineLyrics: Lyrics? = null,
  val activeLyrics: Lyrics? = null,
  val selectedSource: LyricsSourceType = LyricsSourceType.EMBEDDED,
  val availableSources: List<LyricsSourceType> = emptyList(),
  /** Which provider stands behind [onlineLyrics]. */
  val onlineProvider: LyricsProvider? = null,
  /**
   * What the picker should show: the chosen provider when it came through, or
   * null when the answer came from the automatic race and the choice is moot.
   */
  val preferredOnlineProvider: LyricsProvider? = null,
  /** What the automatic race produced, kept so switching back to it is free. */
  val autoOnlineLyrics: Lyrics? = null,
  val autoOnlineProvider: LyricsProvider? = null,
  /** Every online answer seen for this track, so changing provider costs no network. */
  val onlineByProvider: Map<LyricsProvider, Lyrics> = emptyMap(),
  /** Providers that answered with nothing for this track. */
  val missingProviders: Set<LyricsProvider> = emptySet(),
  /** Providers that errored out or timed out for this track. */
  val failedProviders: Set<LyricsProvider> = emptySet(),
) {
  /** Folds what one lookup learned about each provider into this track's statuses. */
  fun withOutcomes(result: OnlineLyricsResult): LyricsResult {
    val answered = result.byProvider.keys
    return copy(
      missingProviders = (missingProviders - answered - result.failed) + result.missing,
      failedProviders = (failedProviders - answered - result.missing) + result.failed,
    )
  }
}

/** What one online lookup produced, and what else it saw on the way. */
data class OnlineLyricsResult(
  val provider: LyricsProvider? = null,
  val lyrics: Lyrics? = null,
  /** Whether this came from the automatic race rather than one chosen source. */
  val raced: Boolean = false,
  val byProvider: Map<LyricsProvider, Lyrics> = emptyMap(),
  val missing: Set<LyricsProvider> = emptySet(),
  val failed: Set<LyricsProvider> = emptySet(),
  /** A local answer discovered by loadLyricsForTrack while resolving source priority. */
  val embeddedLyrics: Lyrics? = null,
  val selectedSource: LyricsSourceType = LyricsSourceType.ONLINE,
)
