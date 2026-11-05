/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.example.utils.media

import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsSourceType
import com.example.domain.lyrics.SyncedLine
import com.example.domain.lyrics.SyncedWord
import java.util.regex.Pattern

/** LRC/enhanced-LRC parser and playback-position helpers shared by lyrics UI and providers. */
object LyricsUtils {
    private val LINE_STAMP_REGEX = Pattern.compile("^\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val WORD_STAMP_REGEX = Pattern.compile("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>([^<]*)")
    private val WORD_TAG_REGEX = Regex("<\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?>")
    private val METADATA_REGEX = Regex("^\\[([a-zA-Z]+):(.*)]$")
    private val LYRIC_WORD_REGEX = Regex("\\S+")

    fun parseLyrics(
        lyricsText: String?,
        sourceType: LyricsSourceType = LyricsSourceType.ONLINE,
    ): Lyrics {
        if (lyricsText.isNullOrBlank()) {
            return Lyrics(plain = emptyList(), synced = emptyList(), sourceType = sourceType)
        }

        val syncedLines = mutableListOf<SyncedLine>()
        val plainLines = mutableListOf<String>()
        var offsetMs = 0L
        var sawTimestamp = false

        lyricsText.replace("\uFEFF", "").lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val metadata = METADATA_REGEX.matchEntire(line)
            if (metadata != null) {
                if (metadata.groupValues[1].equals("offset", ignoreCase = true)) {
                    offsetMs = metadata.groupValues[2].trim().toLongOrNull() ?: 0L
                }
                return@forEach
            }

            // LRC allows multiple leading timestamps on one physical line.
            var remainder = line
            val stamps = mutableListOf<Long>()
            while (true) {
                val matcher = LINE_STAMP_REGEX.matcher(remainder)
                if (!matcher.find()) break
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val fraction = fractionToMillis(matcher.group(3))
                stamps += ((minutes * 60_000L + seconds * 1_000L + fraction) - offsetMs).coerceAtLeast(0L)
                remainder = remainder.substring(matcher.end())
            }

            if (stamps.isEmpty()) {
                if (sawTimestamp && syncedLines.isNotEmpty()) {
                    val last = syncedLines.last()
                    if (last.translation.isNullOrBlank()) {
                        syncedLines[syncedLines.lastIndex] = last.copy(translation = line)
                    } else {
                        syncedLines[syncedLines.lastIndex] = last.copy(line = "${last.line}\n$line")
                    }
                } else {
                    plainLines += line
                }
                return@forEach
            }

            sawTimestamp = true
            val rawText = remainder.trim()
            val words = parseWords(rawText, offsetMs)
            val displayText = WORD_TAG_REGEX.replace(rawText, "").trim()
            val (visibleLine, inlineTranslation) = splitTranslation(displayText)
            stamps.forEach { time ->
                val previous = syncedLines.lastOrNull()
                if (previous != null && previous.time == time.toInt() && previous.translation == null) {
                    syncedLines[syncedLines.lastIndex] = previous.copy(translation = visibleLine)
                } else {
                    syncedLines += SyncedLine(
                        time = time.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                        line = visibleLine,
                        translation = inlineTranslation,
                        words = words.takeIf { it.isNotEmpty() },
                    )
                }
            }
        }

        if (!sawTimestamp || syncedLines.isEmpty() || !hasUsableTiming(syncedLines)) {
            val fallback = plainLines.ifEmpty {
                syncedLines.flatMap { it.line.lines() + listOfNotNull(it.translation) }.filter { it.isNotBlank() }
            }
            return Lyrics(plain = fallback, synced = null, areFromRemote = sourceType == LyricsSourceType.ONLINE, sourceType = sourceType)
        }

        val sorted = syncedLines.sortedBy { it.time }
        val withWords = sorted.mapIndexed { index, line ->
            if (!line.words.isNullOrEmpty() || line.line.isBlank()) line
            else line.copy(words = estimateWordTimings(line, sorted.getOrNull(index + 1)?.time))
        }
        return Lyrics(
            synced = withWords,
            plain = withWords.map { it.line },
            areFromRemote = sourceType == LyricsSourceType.ONLINE,
            sourceType = sourceType,
        )
    }

    private fun parseWords(rawText: String, offsetMs: Long): List<SyncedWord> {
        if (!rawText.contains(WORD_TAG_REGEX)) return emptyList()
        val words = mutableListOf<SyncedWord>()
        val matcher = WORD_STAMP_REGEX.matcher(rawText)
        while (matcher.find()) {
            val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
            val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
            val fraction = fractionToMillis(matcher.group(3))
            val original = matcher.group(4).orEmpty()
            val word = original.trim()
            if (word.isNotEmpty()) {
                val time = (minutes * 60_000L + seconds * 1_000L + fraction - offsetMs).coerceAtLeast(0L)
                words += SyncedWord(
                    time = time.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                    word = word,
                    startsNewWord = words.isEmpty() || original.firstOrNull()?.isWhitespace() == true,
                )
            }
        }
        return words
    }

    private fun splitTranslation(text: String): Pair<String, String?> = when {
        text.contains("\n") -> text.split("\n", limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim() }
        text.contains(" / ") -> text.split(" / ", limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim() }
        text.contains(" | ") -> text.split(" | ", limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim() }
        else -> text.trim() to null
    }

    fun hasUsableTiming(lines: List<SyncedLine>): Boolean {
        val sung = lines.filter { it.line.isNotBlank() }
        return sung.size < 2 || sung.any { it.time != sung.first().time }
    }

    private fun fractionToMillis(fraction: String?): Long = when (fraction?.length) {
        null, 0 -> 0L
        1 -> fraction.toLong() * 100L
        2 -> fraction.toLong() * 10L
        else -> fraction.toLong()
    }

    private fun estimateWordTimings(line: SyncedLine, nextLineTimeMs: Int?): List<SyncedWord>? {
        val words = LYRIC_WORD_REGEX.findAll(line.line).map { it.value }.toList()
        if (words.isEmpty()) return null
        val weights = words.map { it.count(Char::isLetterOrDigit).coerceAtLeast(1) }
        val totalWeight = weights.sum().coerceAtLeast(1)
        val naturalDurationMs = (totalWeight * 90L).coerceIn(1_200L, 6_000L)
        val availableDurationMs = nextLineTimeMs?.takeIf { it > line.time }
            ?.let { (it - line.time).toLong().coerceAtMost(8_000L) } ?: naturalDurationMs
        var elapsedWeight = 0
        return words.mapIndexed { index, word ->
            val wordTimeMs = line.time.toLong() + availableDurationMs * elapsedWeight / totalWeight
            elapsedWeight += weights[index]
            SyncedWord(
                time = wordTimeMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                word = word,
                startsNewWord = true,
            )
        }
    }

    /** Add best-effort word positions to provider models that have line timestamps but no word tags. */
    fun ensureWordTimings(lyrics: Lyrics?): Lyrics? {
        if (lyrics == null || lyrics.synced.isNullOrEmpty()) return lyrics
        val sorted = lyrics.synced.sortedBy { it.time }
        val timed = sorted.mapIndexed { index, line ->
            if (!line.words.isNullOrEmpty() || line.line.isBlank()) line
            else line.copy(words = estimateWordTimings(line, sorted.getOrNull(index + 1)?.time))
        }
        return if (timed == lyrics.synced) lyrics else lyrics.copy(synced = timed)
    }

    /**
     * Builds a best-effort timeline for plain embedded/imported lyrics that contain no timestamps.
     * Real LRC/SYLT/provider timestamps always take precedence. The generated times are approximate
     * (text alone cannot reveal the singer's exact timing), but they keep offline lyrics animated,
     * scrollable and seekable instead of leaving the lyrics view completely static.
     */
    fun estimateTimingForPlainLyrics(lyrics: Lyrics?, durationMs: Long): Lyrics? {
        if (lyrics == null || !lyrics.synced.isNullOrEmpty() || durationMs < 1_000L) return lyrics
        val sourceLines = lyrics.plain.orEmpty()
            .flatMap { it.replace("\r", "").lines() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (sourceLines.isEmpty()) return lyrics

        val startMs = (durationMs * 0.025).toLong().coerceAtLeast(0L)
        val endMs = (durationMs * 0.975).toLong().coerceAtLeast(startMs + 1L)
        val spanMs = (endMs - startMs).coerceAtLeast(sourceLines.size.toLong())
        val wordsByLine = sourceLines.map { text ->
            LYRIC_WORD_REGEX.findAll(text).map { it.value }.toList().ifEmpty { listOf(text) }
        }
        val weights = wordsByLine.map { words ->
            words.sumOf { word -> word.count(Char::isLetterOrDigit).coerceAtLeast(1) }.coerceAtLeast(1)
        }
        val totalWeight = weights.sum().coerceAtLeast(1)
        var elapsedWeight = 0L
        val timedLines = sourceLines.mapIndexed { index, text ->
            val lineStart = startMs + spanMs * elapsedWeight / totalWeight
            elapsedWeight += weights[index]
            val lineEnd = if (index == sourceLines.lastIndex) endMs
                else startMs + spanMs * elapsedWeight / totalWeight
            val lineDuration = (lineEnd - lineStart).coerceAtLeast(wordsByLine[index].size.toLong())
            val lineWords = wordsByLine[index]
            val wordWeights = lineWords.map { it.count(Char::isLetterOrDigit).coerceAtLeast(1) }
            val wordTotal = wordWeights.sum().coerceAtLeast(1)
            var wordElapsed = 0L
            val words = lineWords.mapIndexed { wordIndex, word ->
                val wordTime = lineStart + lineDuration * wordElapsed / wordTotal
                wordElapsed += wordWeights[wordIndex]
                SyncedWord(
                    time = wordTime.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                    word = word,
                    startsNewWord = true,
                )
            }
            SyncedLine(
                time = lineStart.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                line = text,
                words = words,
                endTime = lineEnd.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
            )
        }
        return lyrics.copy(synced = timedLines, plain = sourceLines)
    }

    fun getActiveLineIndex(syncedLines: List<SyncedLine>?, positionMs: Long, offsetMs: Int = 0): Int {
        if (syncedLines.isNullOrEmpty()) return -1
        val targetTime = positionMs + offsetMs
        var activeIndex = -1
        for (i in syncedLines.indices) {
            if (syncedLines[i].time <= targetTime) activeIndex = i else break
        }
        return activeIndex
    }

    fun getActiveWordIndex(line: SyncedLine?, positionMs: Long, offsetMs: Int = 0): Int {
        val words = line?.words ?: return -1
        if (words.isEmpty()) return -1
        val target = positionMs + offsetMs
        var active = -1
        for (index in words.indices) {
            if (words[index].time <= target) active = index else break
        }
        return active
    }
}
