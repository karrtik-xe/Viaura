package com.example.util

import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.SyncedLine
import java.util.Locale

/** Converts provider models to enhanced LRC so the existing audio library/cache remains compatible. */
object LyricsTextCodec {
    fun toLrc(lyrics: Lyrics?): String {
        if (lyrics == null || !lyrics.isValid()) return ""
        val synced = lyrics.synced
        if (synced.isNullOrEmpty()) return lyrics.plain.orEmpty().joinToString("\n").trim()
        return synced.joinToString("\n") { line ->
            val prefix = "[${timestamp(line.time)}]"
            val wordText = line.words.orEmpty()
            if (wordText.isEmpty()) {
                prefix + line.line
            } else {
                prefix + wordText.mapIndexed { index, word ->
                    val spacer = if (index > 0 && word.startsNewWord) " " else ""
                    "$spacer<${timestamp(word.time)}>${word.word}"
                }.joinToString("")
            }
        }.trim()
    }

    private fun timestamp(milliseconds: Int): String {
        val safe = milliseconds.coerceAtLeast(0)
        val minutes = safe / 60_000
        val seconds = (safe / 1_000) % 60
        val hundredths = (safe % 1_000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}
