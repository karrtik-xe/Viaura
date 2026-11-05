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

/**
 * Where the player gets its lyrics.
 *
 * Embedded tags and a sidecar `.lrc` are read locally; everything else is
 * fetched from one of the [LyricsProvider]s. On an automatic lookup every
 * provider is asked at the same time and their answers are taken in priority
 * order, so a miss on a slow host costs only the wait for its budget rather
 * than a round trip before the next is tried — and the first word-timed answer
 * in that order wins outright.
 */
class LyricsRepository(
  private val context: Context,
  lrcLibApiService: LrcLibApiService,
  private val audioPreferences: AudioPreferences,
) {
  companion object {
    private const val TAG = "LyricsRepository"
    private val BRACKETED_REGEX =
      Regex(
        """[\(\[\{\uFF08\uFF3B\uFF5B\u3010\u300E\u300C\u3014\u3008\u300A]([^)\]\}\uFF09\uFF3D\uFF5D\u3011\u300F\u300D\u3015\u3009\u300B\u300C]*)[\)\]\}\uFF09\uFF3D\uFF5D\u3011\u300F\u300D\u3015\u3009\u300B\u300C]""",
      )
    private val TRACK_NO_REGEX = Regex("""^\s*\d{1,3}\s*[\._-]\s+""")
    private val YOUTUBE_ID_REGEX = Regex("""\s*\[[a-zA-Z0-9_-]{6,16}\]\s*$""")
    private val MEDIA_EXT_REGEX =
      Regex(
        """\.(mp3|flac|m4a|aac|wav|ogg|opus|wma|alac|ape|mp4|mkv|webm|avi|mov|flv|wmv|m4v|3gp|ts)$""",
        RegexOption.IGNORE_CASE,
      )
    private val YOUTUBE_NOISE_REGEX =
      Regex(
        """(?i)\b(official\s+(music\s+)?video|official\s+audio|official\s+lyric\s+video|lyric\s+video|lyrics\s+video|lyrical\s+video|full\s+song\s+lyrics|full\s+song|full\s+video|video\s+song|music\s+video|lyrical|lyrics|audio|visualizer|remastered|remaster|4k|hd|8k|mv)\b""",
      )
    private val CHANNEL_TOPIC_REGEX = Regex("""\s*[-–—]\s*topic\s*$""", RegexOption.IGNORE_CASE)
    private val CHANNEL_VEVO_REGEX = Regex("""\s*vevo\s*$""", RegexOption.IGNORE_CASE)
    private val CHANNEL_OFFICIAL_PREFIX_REGEX =
      Regex("""^\s*(?:the\s+)?official\s+(?:artist\s+channel\s+(?:of\s+)?)?""", RegexOption.IGNORE_CASE)
    private val CHANNEL_OFFICIAL_SUFFIX_REGEX =
      Regex("""\s+(?:official(?:\s+(?:channel|artist\s+channel|youtube\s+channel))?|tv|channel)\s*$""", RegexOption.IGNORE_CASE)
    private val VIDEO_TITLE_REGEX =
      Regex("""official\s+(?:music\s+)?video|music\s+video|full\s+video|video\s+song|\(mv\)|\bm/v\b""", RegexOption.IGNORE_CASE)
    private val SEGMENT_DELIMITER_REGEX = Regex("""\s*[|｜¦/／]\s*""")
    private val HYPHEN_DELIMITER_REGEX = Regex("""\s*[-–—－]\s*""")
    private val UNKNOWN_ARTISTS = setOf("", "unknown", "unknown artist", "<unknown>", "various artists", "various")
    private val YOUTUBE_VIDEO_REGEX =
      Regex(
        """(?:youtube\.com/watch\?(?:[^#]*&)?v=|music\.youtube\.com/watch\?(?:[^#]*&)?v=|youtu\.be/|youtube\.com/shorts/)([A-Za-z0-9_-]{11})""",
      )

    /** How many title/artist guesses a lookup hands the providers. */
    private const val MAX_REFS = 8

    /**
     * Longest the lookup will wait to find out which recording this is.
     *
     * Short on purpose. Knowing the recording makes every match better, but
     * not knowing it only leaves the lookup where it was before, and an empty
     * panel because one host is slow is the worse failure.
     */
    private const val IDENTIFY_TIMEOUT_MS = 2_500L

    /** How many recordings to keep in hand. */
    private const val REMEMBERED_RECORDINGS = 100

    /** How far past the end of the file a timed line may sit before the lyrics are another edit. */
    private const val LENGTH_OVERRUN_MS = 12_000

    /** Last sung line this far past the end of the file = lyrics of a longer edit (they are scaled down). */
    private const val RETIME_TOLERANCE_MS = 6_000

    /** Smallest / largest file-vs-recording length gap treated as a video intro. */
    private const val LEAD_SHIFT_MIN_MS = 4_000
    private const val LEAD_SHIFT_MAX_MS = 40_000

    private val PLACEHOLDER_REGEX =
      Regex(
        "纯音乐|純音樂|没有填词|沒有填詞|暂无歌词|暫無歌詞|暂时没有歌词|无歌词|歌詞なし|インストゥルメンタル|instrumental|no lyrics|lyrics not available|lyrics unavailable",
        RegexOption.IGNORE_CASE,
      )
    private val SLOWED_REGEX = Regex("""slowed|slow\s*\+|\bslow\b|reverb|ultra\s*slow""")
    private val SPED_REGEX = Regex("""sped\s*up|speed\s*up|speedup|nightcore""")

    /** The video behind a playing path, when the path is a YouTube URL at all. */
    fun videoIdFrom(path: String?): String? =
      path
        ?.takeIf { it.isNotBlank() }
        ?.let { YOUTUBE_VIDEO_REGEX.find(it) }
        ?.groupValues
        ?.get(1)
  }

  private val registry = LyricsProviderRegistry(lrcLibApiService)

  /**
   * The online provider to try first for this listening session. Null is the
   * automatic provider race, which is the fresh-session default: it takes the
   * best-timed answer that fits the playing track, with LRCLIB as the last
   * resort when nothing else has words. The choice remains session-scoped
   * rather than becoming a persisted setting.
   */
  @Volatile
  var preferredProvider: LyricsProvider? = null
    private set

  private data class CacheKey(
    val mediaPath: String,
    val allowOnline: Boolean,
    val autoFetchOnline: Boolean,
  )

  private fun cacheKey(mediaPath: String, allowOnline: Boolean): CacheKey =
    CacheKey(mediaPath, allowOnline, audioPreferences.lyricsAutoFetchOnline.get())

  private val cache = LruCache<CacheKey, LyricsResult>(64)
  private val providerCache = LyricsProviderCache(File(context.cacheDir, "lyrics/providers-v5"))

  /**
   * The recording behind a track, once something has worked it out.
   *
   * In memory only: this is a shortcut, not a store, and losing it costs one
   * fuzzy match — which is what every lookup did before any of this.
   */
  private val isrcs = LruCache<String, String>(REMEMBERED_RECORDINGS)

  private fun cleanTitle(title: String): String =
    title
      .replace(MEDIA_EXT_REGEX, "")
      .replace(YOUTUBE_ID_REGEX, "")
      .replace(TRACK_NO_REGEX, "")
      .trim()

  private fun superCleanTitle(title: String): String {
    val step1 = cleanTitle(title)
    val firstSeg =
      step1
        .split(SEGMENT_DELIMITER_REGEX)
        .firstOrNull()
        ?.trim()
        .orEmpty()
        .ifBlank { step1 }
    val step2 = BRACKETED_REGEX.replace(firstSeg, "").trim()
    val step3 = step2.split(" feat.", " ft.", " featuring", " Feat.", " Ft.", " feat ", " ft ").first().trim()
    val step4 = YOUTUBE_NOISE_REGEX.replace(step3, "").trim()
    return step4.ifBlank { step3.ifBlank { step1 } }
  }

  private fun cleanArtist(artist: String?): String {
    var raw = artist?.trim().orEmpty()
    if (raw.lowercase() in UNKNOWN_ARTISTS) return ""
    // YouTube channel names: "Official Arctic Monkeys", "Arctic Monkeys - Topic", "ArcticMonkeysVEVO", "Ed Sheeran Official".
    raw = CHANNEL_TOPIC_REGEX.replace(raw, "")
    raw = CHANNEL_VEVO_REGEX.replace(raw, "")
    raw = CHANNEL_OFFICIAL_PREFIX_REGEX.replace(raw, "")
    raw = CHANNEL_OFFICIAL_SUFFIX_REGEX.replace(raw, "")
    raw = raw.trim().trim('-', '–', '—', '|').trim()
    if (raw.lowercase() in UNKNOWN_ARTISTS) return ""
    return raw.split(" feat.", " ft.", " featuring", " Feat.", " Ft.", " feat ", " ft ").first().trim()
  }

  private fun isLikelyChannelOrLabel(artist: String?): Boolean {
    val lower = artist?.trim().orEmpty().lowercase()
    if (lower.isBlank() || lower in UNKNOWN_ARTISTS) return true
    return lower.endsWith(" music") || lower.endsWith(" records") ||
      lower.endsWith(" official") || lower.endsWith(" entertainment") ||
      lower.endsWith(" channel") || lower.endsWith(" series") ||
      lower.endsWith(" films") || lower.endsWith(" company") ||
      lower.endsWith(" vevo") || lower.contains("t-series") ||
      lower.contains("tseries") || lower.contains("saregama") ||
      lower.contains("zee music") || lower.contains("sony music") ||
      lower.contains("tips official") || lower.contains("yrf") ||
      lower.contains("speed records") || lower.contains("geet mp3") ||
      lower.contains("desire music") || lower.contains("lofi girl") ||
      lower.contains("aditya music") || lower.contains("lahari music")
  }

  /**
   * The guesses about what this file actually is, in the order they are worth
   * trying: the metadata as given, then the halves of an "Artist - Title" line
   * read both ways round, then any other segment of a multi-part title as an
   * artist, then the title on its own.
   */
  private fun buildRefs(
    rawTitle: String?,
    rawArtist: String?,
  ): List<TrackRef> {
    if (rawTitle.isNullOrBlank()) return emptyList()

    val baseTitle = cleanTitle(rawTitle)
    val segments = baseTitle.split(SEGMENT_DELIMITER_REGEX).map { it.trim() }.filter { it.isNotBlank() }
    val firstSeg = segments.firstOrNull() ?: baseTitle
    val primaryTitle = superCleanTitle(firstSeg)

    val cleanRawArtist = cleanArtist(rawArtist)
    val metadataArtist = if (isLikelyChannelOrLabel(cleanRawArtist)) "" else cleanRawArtist

    val refs = LinkedHashMap<String, TrackRef>()

    fun add(
      title: String,
      artist: String,
    ) {
      val cleanT = title.trim()
      if (cleanT.isBlank()) return
      val ref = TrackRef(cleanT, artist.trim())
      refs.putIfAbsent(ref.key(), ref)
    }

    if (metadataArtist.isNotBlank()) add(primaryTitle, metadataArtist)

    if (firstSeg.contains(HYPHEN_DELIMITER_REGEX)) {
      val parts = firstSeg.split(HYPHEN_DELIMITER_REGEX, limit = 2)
      if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        val p0 = superCleanTitle(parts[0])
        val p1 = superCleanTitle(parts[1])
        // "A.R. Rahman - Raanjhanaa" and "Vaaroon - Mirzapur" are both common,
        // and nothing here knows which half is the artist.
        add(p1, p0)
        add(p0, p1)
      }
    }

    for (otherSeg in segments.drop(1)) {
      val cleanSeg = YOUTUBE_NOISE_REGEX.replace(BRACKETED_REGEX.replace(otherSeg, "").trim(), "").trim()
      if (cleanSeg.isBlank() || isLikelyChannelOrLabel(cleanSeg)) continue
      val firstArtist = cleanArtist(cleanSeg)
      if (firstArtist.isNotBlank()) add(primaryTitle, firstArtist)
      add(primaryTitle, cleanSeg)
    }

    add(primaryTitle, "")
    val rawFirstClean = cleanTitle(firstSeg)
    if (rawFirstClean != primaryTitle) add(rawFirstClean, "")

    return refs.values.toList().take(MAX_REFS)
  }

  suspend fun loadLyricsForTrack(
    mediaPath: String,
    title: String?,
    artist: String?,
    durationSeconds: Int = 0,
    forceRefresh: Boolean = false,
    allowOnline: Boolean = true,
    forceOnline: Boolean = false,
    album: String? = null,
    isrc: String? = null,
    provider: LyricsProvider? = preferredProvider,
    videoId: String? = null,
  ): LyricsResult =
    withContext(Dispatchers.IO) {
      val cacheKey = cacheKey(mediaPath, allowOnline)
      val requestedProvider = provider
      if (!forceRefresh && !forceOnline) {
        cache.get(cacheKey)?.let { existing ->
          val embeddedHasTimestamps = !existing.embeddedLyrics?.synced.isNullOrEmpty()
          val hasMatchingOnlineCache = existing.onlineLyrics?.isValid() == true &&
            existing.preferredOnlineProvider == requestedProvider
          if (!allowOnline || embeddedHasTimestamps ||
            !audioPreferences.lyricsAutoFetchOnline.get() || hasMatchingOnlineCache
          ) return@withContext existing
          // A cached plain embedded lyric is useful offline but does not have a playback clock;
          // allow another provider race rather than treating it as a complete sync match.
          switchProvider(mediaPath, requestedProvider, allowOnline)?.let { return@withContext it }
        }
      }

      Log.d(TAG, "Loading lyrics for: $title by $artist ($mediaPath)")

      val embedded = EmbeddedLyricsExtractor.extractEmbeddedLyrics(context, mediaPath)

      // Timestamped embedded/local lyrics are immediately usable. Plain embedded text has no
      // playback clock, so also try the providers and prefer a matched synced answer while keeping
      // the original embedded text available if every network source misses.
      val embeddedHasTimestamps = !embedded?.synced.isNullOrEmpty()
      val online =
        if (allowOnline && (forceOnline ||
            (audioPreferences.lyricsAutoFetchOnline.get() && !embeddedHasTimestamps))) {
          fetchOnlineLyrics(
            rawTitle = title,
            rawArtist = artist,
            durationSeconds = durationSeconds,
            provider = requestedProvider,
            album = album,
            isrc = isrc,
            mediaPath = mediaPath,
            forceRefresh = forceRefresh,
            videoId = videoId,
          )
        } else {
          OnlineLyricsResult()
        }

      val sources = mutableListOf<LyricsSourceType>()
      if (embedded != null && embedded.isValid()) {
        sources.add(
          if (embedded.sourceType ==
            LyricsSourceType.LOCAL
          ) {
            LyricsSourceType.LOCAL
          } else {
            LyricsSourceType.EMBEDDED
          },
        )
      }
      if (online.lyrics != null && online.lyrics.isValid()) {
        sources.add(LyricsSourceType.ONLINE)
      }

      val defaultSelected =
        when {
          forceOnline && online.lyrics?.isValid() == true -> LyricsSourceType.ONLINE
          embedded != null && embedded.isValid() -> embedded.sourceType
          online.lyrics != null && online.lyrics.isValid() -> LyricsSourceType.ONLINE
          else -> LyricsSourceType.EMBEDDED
        }

      val active =
        when (defaultSelected) {
          LyricsSourceType.EMBEDDED, LyricsSourceType.LOCAL -> embedded ?: online.lyrics
          LyricsSourceType.ONLINE -> online.lyrics ?: embedded
        }

      val autoResult = online.lyrics.takeIf { online.raced }
      val previous = cache.get(cacheKey)
      val result =
        LyricsResult(
          embeddedLyrics = embedded,
          onlineLyrics = online.lyrics,
          activeLyrics = active,
          selectedSource = defaultSelected,
          availableSources = sources.distinct(),
          onlineProvider = online.provider,
          preferredOnlineProvider =
            if (requestedProvider != null && online.provider == requestedProvider) {
              online.provider
            } else {
              null
            },
          autoOnlineLyrics = autoResult,
          autoOnlineProvider = if (autoResult != null) online.provider else null,
          onlineByProvider = previous?.onlineByProvider.orEmpty() + online.byProvider,
          missingProviders = previous?.missingProviders.orEmpty(),
          failedProviders = previous?.failedProviders.orEmpty(),
        ).withOutcomes(online)

      currentCoroutineContext().ensureActive()
      cache.put(cacheKey, result)
      result
    }

  /**
   * Asks for online lyrics.
   *
   * With [provider] set, that one provider is asked — and if it has nothing,
   * the automatic race runs anyway rather than leaving the panel empty. Without
   * it every provider is raced and the first answer in priority order is taken.
   */
  suspend fun fetchOnlineLyrics(
    rawTitle: String?,
    rawArtist: String?,
    durationSeconds: Int = 0,
    provider: LyricsProvider? = null,
    album: String? = null,
    isrc: String? = null,
    mediaPath: String? = null,
    forceRefresh: Boolean = false,
    videoId: String? = null,
  ): OnlineLyricsResult =
    withContext(Dispatchers.IO) {
      val saved = if (!forceRefresh && !mediaPath.isNullOrBlank()) providerCache.read(mediaPath) else emptyMap()
      cachedOnlineResult(saved, provider, durationSeconds * 1000)?.let { return@withContext it }
      if (rawTitle.isNullOrBlank()) return@withContext OnlineLyricsResult()

      val query =
        LyricsFetchQuery(
          refs = buildRefs(rawTitle, rawArtist),
          durationMs = (durationSeconds * 1000).coerceAtLeast(0),
          album = album,
          videoId = videoId?.takeIf { it.isNotBlank() } ?: videoIdFrom(mediaPath),
          isrc = isrc,
          fileDurationMs = (durationSeconds * 1000).coerceAtLeast(0),
          speedHint = speedHintOf(rawTitle),
          videoHint = VIDEO_TITLE_REGEX.containsMatchIn(rawTitle),
        )
      if (query.refs.isEmpty()) return@withContext OnlineLyricsResult()

      PaxSenixApi.setApiKey(audioPreferences.paxsenixApiKey.get())

      val resolved = withRecording(query)

      if (provider != null) {
        val outcome = fetchProvider(provider, resolved, mediaPath)
        if (outcome is ProviderOutcome.Found) {
          val answers = mediaPath?.let(providerCache::read).orEmpty() + (provider to outcome.lyrics)
          return@withContext OnlineLyricsResult(provider, outcome.lyrics, false, answers)
        }
        Log.d(TAG, "${provider.label} had nothing; falling back to the automatic lookup")
        val raced = raceWithLrcLibFallback(resolved, mediaPath, saved)
        val raceKnows = provider in raced.byProvider || provider in raced.missing || provider in raced.failed
        return@withContext raced.copy(
          byProvider = mediaPath?.let(providerCache::read).orEmpty() + raced.byProvider,
          missing = if (!raceKnows && outcome == ProviderOutcome.Missing) raced.missing + provider else raced.missing,
          failed = if (!raceKnows && outcome == ProviderOutcome.Failed) raced.failed + provider else raced.failed,
        )
      }

      val result = raceWithLrcLibFallback(resolved, mediaPath, saved)
      result.copy(byProvider = mediaPath?.let(providerCache::read).orEmpty() + result.byProvider)
    }

  private fun cachedOnlineResult(
    answers: Map<LyricsProvider, Lyrics>,
    provider: LyricsProvider?,
    durationMs: Int,
  ): OnlineLyricsResult? {
    if (provider != null) {
      val lyrics = answers[provider] ?: return null
      Log.d(TAG, "${provider.label}: found (per-provider disk cache)")
      return OnlineLyricsResult(provider, lyrics, false, answers)
    }
    val candidates = LyricsProvider.available.mapNotNull { source -> answers[source]?.let { source to it } }
    val fitting = candidates.filter { (_, lyrics) -> lyrics.fitsLength(durationMs) }
    val winner = fitting.firstOrNull { (_, lyrics) -> lyrics.synced.orEmpty().any { !it.words.isNullOrEmpty() } }
      ?: fitting.firstOrNull { (_, lyrics) -> !lyrics.synced.isNullOrEmpty() }
      ?: fitting.firstOrNull()
      ?: candidates.firstOrNull()
      ?: return null
    Log.d(TAG, "${winner.first.label}: found (per-provider disk cache)")
    return OnlineLyricsResult(winner.first, winner.second, true, answers)
  }

  private suspend fun fetchProvider(
    provider: LyricsProvider,
    query: LyricsFetchQuery,
    mediaPath: String?,
  ): ProviderOutcome =
    when (val outcome = registry.fetch(provider, query)) {
      is ProviderOutcome.Found ->
        if (outcome.lyrics.isValid() && !outcome.lyrics.isPlaceholder()) {
          // another edit of the song (slowed / sped up / different intro) is fitted to the length of the playing file
          val shaped = outcome.lyrics.withBackgroundVocals().fittedTo(query).leadShiftedTo(query)
          if (!mediaPath.isNullOrBlank()) providerCache.write(mediaPath, provider, shaped)
          ProviderOutcome.Found(shaped)
        } else {
          ProviderOutcome.Missing
        }
      else -> outcome
    }

  /**
   * Names the recording before anybody is asked for words.
   *
   * A title is ambiguous where it matters: a single and its album cut share a
   * name, an artist and very nearly a length, and routinely differ in the
   * words. An ISRC settles it, and BiniLyrics is the one source here that
   * hands them out, so one short search up front gives every ISRC-capable
   * provider a name instead of a description. Capped and remembered, so a
   * slow host costs the match rather than the panel, and a track asked about
   * twice pays for it once.
   */
  private suspend fun withRecording(query: LyricsFetchQuery): LyricsFetchQuery {
    if (!query.isrc.isNullOrBlank()) return query
    if (LyricsProvider.BINI_LYRICS !in LyricsProvider.available) return query
    val key = recordingKey(query) ?: return query
    isrcs.get(key)?.let { return query.copy(isrc = it) }
    val found =
      withTimeoutOrNull(IDENTIFY_TIMEOUT_MS) {
        runCatching { BiniLyricsProvider.identifyIsrc(query) }
          .getOrElse { error -> if (error is CancellationException) throw error else null }
      } ?: return query
    isrcs.put(key, found)
    return query.copy(isrc = found)
  }

  private fun recordingKey(query: LyricsFetchQuery): String? {
    query.videoId?.takeIf { it.isNotBlank() }?.let { return it }
    val ref = query.primary
    if (ref.title.isBlank()) return null
    return "${ref.title}|${ref.artist}|${query.durationMs / 1000}"
  }

  /**
   * The automatic race, with LRCLIB as the safety net.
   *
   * When every provider missed or failed, LRCLIB gets one more go with the
   * loosest question there is: the title alone, closest length wins. That is
   * wrong more often than the exact lookups, so its answer is never written to
   * the per-provider cache and only ever shown when nothing better exists.
   */
  private suspend fun raceWithLrcLibFallback(
    query: LyricsFetchQuery,
    mediaPath: String?,
    cached: Map<LyricsProvider, Lyrics>,
  ): OnlineLyricsResult {
    val raced = raceProviders(query, mediaPath, cached)
    if (raced.lyrics != null) return raced

    // Second pass without the length check. The catalogue usually has only the ORIGINAL song; a slowed / sped-up / shortened
    // file is 10-40 s away from it, so the strict pass rejects every answer. Here the same title + artist are asked again
    // and the answer is fitted to the audio afterwards (see fittedTo).
    if (query.durationMs > 0) {
      val relaxedQuery = query.copy(durationMs = 0)
      val relaxed = raceProviders(relaxedQuery, mediaPath, cached)
      if (relaxed.lyrics != null) {
        Log.d(TAG, "Strict pass found nothing; using the answer of the relaxed pass (${relaxed.provider?.label})")
        return relaxed.copy(
          byProvider = raced.byProvider + relaxed.byProvider,
          missing = relaxed.missing,
          failed = relaxed.failed,
        )
      }
    }

    val loose =
      query.copy(
        refs = query.candidates().map { TrackRef(it.title, "") }.distinctBy { it.key() },
        isrc = null,
      )
    if (loose.refs.isEmpty()) return raced
    val outcome = registry.fetch(LyricsProvider.LRCLIB, loose)
    val lyrics = (outcome as? ProviderOutcome.Found)?.lyrics?.takeIf { it.isValid() && it.fitsLength(query.durationMs) }
      ?: return raced
    Log.d(TAG, "Nothing answered the race; using the loose LRCLIB title match")
    return OnlineLyricsResult(
      provider = LyricsProvider.LRCLIB,
      lyrics = lyrics,
      raced = true,
      byProvider = raced.byProvider + (LyricsProvider.LRCLIB to lyrics),
      missing = raced.missing - LyricsProvider.LRCLIB,
      failed = raced.failed - LyricsProvider.LRCLIB,
    )
  }

  /** Placeholder answers such as "纯音乐，请欣赏" (pure music, please enjoy) are not lyrics: the song has no words. */
  private fun Lyrics.isPlaceholder(): Boolean {
    val lines = synced?.map { it.line } ?: plain.orEmpty()
    val text = lines.joinToString(" ").trim()
    if (text.isEmpty()) return false
    return lines.size <= 4 && text.length <= 80 && PLACEHOLDER_REGEX.containsMatchIn(text)
  }

  /**
   * Fits timed lyrics of another edit to the playing file.
   *
   * Only the relaxed pass (no length check, [LyricsFetchQuery.durationMs] = 0) can return another edit. Its last sung line tells
   * how long the lyrics' recording is: far past the end of the file = the file is faster / shorter than the lyrics; far short of the
   * end AND a "slowed" title = the file is slower. All times (lines, words, background voices) are scaled by the ratio, so the lines
   * land where the singer sings them instead of drifting minutes away. Exact-length answers are never touched.
   */
  private fun Lyrics.fittedTo(query: LyricsFetchQuery): Lyrics {
    val file = query.fileDurationMs
    if (query.durationMs > 0 || file <= 0) return this
    val lines = synced?.takeIf { it.isNotEmpty() } ?: return this
    val last = lines.lastOrNull { it.line.isNotBlank() }?.time ?: return this
    if (last <= 0) return this
    val tooLong = last > file + RETIME_TOLERANCE_MS
    val tooShort = query.speedHint > 0 && last < file * 0.80
    if (!tooLong && !tooShort) return this
    val scale = (file * 0.95 / last).coerceIn(0.5, 2.0)
    if (kotlin.math.abs(scale - 1.0) < 0.03) return this
    Log.d(TAG, "Fitting lyrics to the file: last line ${last}ms, file ${file}ms, scale $scale")
    return copy(synced = lines.map { it.scaledBy(scale) })
  }

  private fun SyncedLine.scaledBy(scale: Double): SyncedLine =
    copy(
      time = (time * scale).toInt(),
      endTime = endTime?.let { (it * scale).toInt() },
      words = words?.map { it.copy(time = (it.time * scale).toInt()) },
      background = background?.scaledBy(scale),
    )

  /**
   * Music videos often open with an intro / skit that the studio recording (which the lyrics were timed against) does not have.
   * When the provider tells how long its recording is and the playing file is a few seconds longer, with a video title, the
   * whole lyric is moved later by the difference. Only done when the shifted last line still fits inside the file.
   */
  private fun Lyrics.leadShiftedTo(query: LyricsFetchQuery): Lyrics {
    val file = query.fileDurationMs
    val recording = recordingDurationMs
    if (!query.videoHint || file <= 0 || recording <= 0) return this
    val delta = file - recording
    if (delta < LEAD_SHIFT_MIN_MS || delta > LEAD_SHIFT_MAX_MS) return this
    val lines = synced?.takeIf { it.isNotEmpty() } ?: return this
    val last = lines.lastOrNull { it.line.isNotBlank() }?.time ?: return this
    if (last + delta > file + 2_000) return this
    Log.d(TAG, "Video intro detected: file ${file}ms vs recording ${recording}ms, shifting lyrics by ${delta}ms")
    return copy(synced = lines.map { it.shiftedBy(delta) })
  }

  private fun SyncedLine.shiftedBy(delta: Int): SyncedLine =
    copy(
      time = time + delta,
      endTime = endTime?.plus(delta),
      words = words?.map { it.copy(time = it.time + delta) },
      background = background?.shiftedBy(delta),
    )

  private fun speedHintOf(rawTitle: String?): Int {
    val t = rawTitle?.lowercase().orEmpty()
    return when {
      SPED_REGEX.containsMatchIn(t) -> -1
      SLOWED_REGEX.containsMatchIn(t) -> 1
      else -> 0
    }
  }

  /**
   * Whether timed lyrics could belong to a file this long.
   *
   * A different edit of the same song drifts or runs on past the end; a line
   * stamped well beyond the end of the file is proof it is not this recording.
   * Untimed lyrics, and files whose length is unknown, always fit.
   */
  private fun Lyrics.fitsLength(durationMs: Int): Boolean {
    if (durationMs <= 0) return true
    val last = synced?.lastOrNull { it.line.isNotBlank() }?.time ?: return true
    return last <= durationMs + LENGTH_OVERRUN_MS
  }

  /** Every provider is asked together; actual timing quality wins, then provider priority. */
  private suspend fun raceProviders(
    query: LyricsFetchQuery,
    mediaPath: String?,
    cached: Map<LyricsProvider, Lyrics>,
  ): OnlineLyricsResult =
    coroutineScope {
      val jobs =
        LyricsProvider.available.map { provider ->
          val start = if (provider in LyricsProvider.LAZY_PROVIDERS) CoroutineStart.LAZY else CoroutineStart.DEFAULT
          provider to async(Dispatchers.IO, start = start) {
            cached[provider]?.let(ProviderOutcome::Found) ?: fetchProvider(provider, query, mediaPath)
          }
        }

      try {
        var wordTimed: Pair<LyricsProvider, Lyrics>? = null
        var lineTimed: Pair<LyricsProvider, Lyrics>? = null
        var plainFallback: Pair<LyricsProvider, Lyrics>? = null
        // Timed answers that run well past the end of the playing file: very
        // likely another edit of the song. Used only when nothing else exists.
        var misfit: Pair<LyricsProvider, Lyrics>? = null
        val seen = mutableMapOf<LyricsProvider, Lyrics>()
        val missing = mutableSetOf<LyricsProvider>()
        val failed = mutableSetOf<LyricsProvider>()

        for ((provider, job) in jobs) {
          if ((wordTimed != null || lineTimed != null) && provider in LyricsProvider.LAZY_PROVIDERS) continue
          val outcome =
            runCatching { job.await() }
              .getOrElse { error -> if (error is CancellationException) throw error else ProviderOutcome.Failed }
          val found =
            when (outcome) {
              is ProviderOutcome.Found -> outcome.lyrics
              ProviderOutcome.Missing -> {
                missing += provider
                continue
              }
              ProviderOutcome.Failed -> {
                failed += provider
                continue
              }
            }
          if (!found.isValid()) {
            missing += provider
            continue
          }
          seen[provider] = found

          if (!found.fitsLength(query.durationMs)) {
            if (misfit == null) misfit = provider to found
            continue
          }
          if (found.synced.orEmpty().any { !it.words.isNullOrEmpty() }) {
            wordTimed = provider to found
            break
          }
          if (lineTimed == null && !found.synced.isNullOrEmpty()) lineTimed = provider to found
          if (plainFallback == null) plainFallback = provider to found
        }

        val winner = wordTimed ?: lineTimed ?: plainFallback ?: misfit
        if (winner != null) {
          seen[winner.first] = winner.second
          OnlineLyricsResult(
            provider = winner.first,
            lyrics = winner.second,
            raced = true,
            byProvider = seen.toMap(),
            missing = missing.toSet(),
            failed = failed.toSet(),
          )
        } else {
          OnlineLyricsResult(byProvider = seen.toMap(), missing = missing.toSet(), failed = failed.toSet())
        }
      } finally {
        jobs.forEach { it.second.cancel() }
      }
    }

  /**
   * Moves the online half of the result onto [provider], when this track has
   * already been fetched from it. Null for "not fetched yet", which is the
   * caller's signal to go and get it.
   */
  fun switchProvider(
    mediaPath: String,
    provider: LyricsProvider?,
    allowOnline: Boolean = true,
  ): LyricsResult? {
    if (!allowOnline) return null
    preferredProvider = provider
    val cacheKey = cacheKey(mediaPath, allowOnline)
    val existing = cache.get(cacheKey) ?: return null

    val picked =
      if (provider == null) {
        existing.autoOnlineLyrics?.let { it to existing.autoOnlineProvider }
      } else {
        existing.onlineByProvider[provider]?.let { it to provider }
      }

    val (lyrics, from) = picked ?: return null
    val updated =
      existing.copy(
        selectedSource = LyricsSourceType.ONLINE,
        onlineLyrics = lyrics,
        onlineProvider = from,
        preferredOnlineProvider = provider,
        activeLyrics = lyrics,
        availableSources = (existing.availableSources + LyricsSourceType.ONLINE).distinct(),
      )
    cache.put(cacheKey, updated)
    return updated
  }

  fun invalidate(
    mediaPath: String,
    allowOnline: Boolean = true,
  ) {
    cache.remove(cacheKey(mediaPath, allowOnline))
  }
}
