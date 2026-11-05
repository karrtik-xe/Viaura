package com.example.util

/**
 * Shared LRC reader used by the lyrics UI and the online lyrics fetcher.
 *
 * Handles what real-world .lrc / embedded USLT / online lyrics contain and the old inline
 * parser did not:
 *  - several stamps on one line (`[00:12.00][01:30.50]Chorus`) -> one entry per stamp
 *  - stamps without a fraction (`[00:54]`), 1/2/3-digit fractions, `mm:ss:xx`
 *  - `[offset:+/-ms]` header and the other `[ar:] [ti:] [al:] [by:] [length:]` headers (hidden)
 *  - enhanced word stamps (`<00:12.30>word`) stripped from the text
 *  - lines sorted by time, so out-of-order files still highlight correctly
 *  - "synced" files whose stamps are all identical are treated as plain text
 */
object LrcParser {
    data class Line(val timeMs: Long?, val text: String)

    private val LEADING_STAMP = Regex("""^\s*\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val WORD_STAMP = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val HEADER_TAG = Regex("""^\[([A-Za-z#]+):(.*)]\s*$""")

    fun parse(raw: String?): List<Line> {
        if (raw.isNullOrBlank()) return emptyList()

        var offsetMs = 0L
        val timed = ArrayList<Line>()
        val leadingPlain = ArrayList<Line>()
        var sawTimed = false

        for (rawLine in raw.replace("﻿", "").lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val header = HEADER_TAG.matchEntire(line)
            if (header != null) {
                if (header.groupValues[1].equals("offset", ignoreCase = true)) {
                    offsetMs = header.groupValues[2].trim().removePrefix("+").toLongOrNull() ?: 0L
                }
                continue
            }

            val stamps = ArrayList<Long>()
            var rest = line
            while (true) {
                val m = LEADING_STAMP.find(rest) ?: break
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = when (m.groupValues[3].length) {
                    0 -> 0L
                    1 -> m.groupValues[3].toLong() * 100L
                    2 -> m.groupValues[3].toLong() * 10L
                    else -> m.groupValues[3].toLong()
                }
                stamps += (min * 60 + sec) * 1000 + frac
                rest = rest.substring(m.range.last + 1)
            }

            if (stamps.isNotEmpty()) {
                sawTimed = true
                val text = WORD_STAMP.replace(rest, "").trim()
                for (t in stamps) {
                    // LRC spec: positive offset = lyrics appear sooner.
                    timed += Line((t - offsetMs).coerceAtLeast(0L), if (text.isEmpty()) "♪" else text)
                }
            } else {
                val text = WORD_STAMP.replace(line, "").trim()
                if (text.isEmpty()) continue
                if (!sawTimed) {
                    leadingPlain += Line(null, text)
                } else if (timed.isNotEmpty()) {
                    // continuation / translation line of the previous stamped line
                    val last = timed.last()
                    timed[timed.lastIndex] = last.copy(text = last.text + "\n" + text)
                }
            }
        }

        if (!sawTimed) return leadingPlain

        val sorted = timed.sortedBy { it.timeMs }
        val sung = sorted.filter { it.text != "♪" }
        val usable = sung.size < 2 || sung.any { it.timeMs != sung.first().timeMs }
        return if (usable) leadingPlain + sorted
        else (leadingPlain + sorted).map { Line(null, it.text) }
    }

    /** True when the text has stamps that can really drive line highlighting. */
    fun isSynced(raw: String?): Boolean = parse(raw).any { it.timeMs != null }

    /** Timestamp of the last sung line, or null for plain lyrics. */
    fun lastStampMs(raw: String?): Long? =
        parse(raw).lastOrNull { it.timeMs != null && it.text != "♪" }?.timeMs
}
