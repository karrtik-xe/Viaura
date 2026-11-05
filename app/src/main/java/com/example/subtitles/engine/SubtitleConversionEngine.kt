package com.example.ui.screens

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.io.File
import java.util.Locale

// ==========================================
// GENERAL SUBTITLE ENGINE
// ==========================================

object SubtitleEngine {

    fun stripAllSubtitleTags(input: String): String {
        if (input.isBlank()) return ""
        var s = input
        // ASS/SSA override tags {\...}
        s = s.replace(Regex("\\{\\\\.*?\\}"), "")
        // MicroDVD tags {y:...}, {c:...}, {f:...}, {s:...}
        s = s.replace(Regex("\\{[a-zA-Z]:.*?\\}"), "")
        // HTML / XML tags <...>
        s = s.replace(Regex("<[^>]*>"), "")
        // Convert ASS line breaks \N, \n
        s = s.replace("\\N", "\n").replace("\\n", "\n")
        return s.trim()
    }

    fun detectFormat(content: String, fileName: String = ""): SubtitleFormat {
        if (fileName.isNotEmpty()) {
            val fmt = SubtitleFormat.fromFileName(fileName)
            if (fmt != SubtitleFormat.SRT) return fmt
        }
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("WEBVTT", ignoreCase = true) -> SubtitleFormat.VTT
            trimmed.contains("[Script Info]", ignoreCase = true) ||
                trimmed.contains("[V4+ Styles]", ignoreCase = true) -> SubtitleFormat.ASS
            trimmed.contains("[V4 Styles]", ignoreCase = true) -> SubtitleFormat.SSA
            trimmed.contains("<tt", ignoreCase = true) -> SubtitleFormat.TTML
            trimmed.contains("<SAMI>", ignoreCase = true) -> SubtitleFormat.SMI
            Regex("(?m)^\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?\\]").containsMatchIn(trimmed) -> SubtitleFormat.LRC
            Regex("(?m)^\\{\\d+\\}\\{\\d+\\}").containsMatchIn(trimmed) -> SubtitleFormat.SUB
            Regex("(?m)^\\d+:\\d{2}:\\d{2}\\.\\d{3}\\s*,\\s*\\d+:\\d{2}:\\d{2}\\.\\d{3}").containsMatchIn(trimmed) -> SubtitleFormat.SBV
            Regex("(?m)^\\d+\\s*\\r?\\n\\d{2}:\\d{2}:\\d{2},\\d{3}\\s*-->\\s*\\d{2}:\\d{2}:\\d{2},\\d{3}").containsMatchIn(trimmed) -> SubtitleFormat.SRT
            else -> SubtitleFormat.SRT
        }
    }

    fun parse(content: String, format: SubtitleFormat = SubtitleFormat.SRT): List<SubtitleCue> {
        val autoFmt = if (format == SubtitleFormat.SRT) detectFormat(content) else format
        return parseContent(content, autoFmt)
    }

    fun parse(content: String, format: SubtitleFormat, fps: Float): List<SubtitleCue> {
        val autoFmt = if (format == SubtitleFormat.SRT) detectFormat(content) else format
        return parseContent(content, autoFmt, fps.toDouble())
    }

    fun parse(content: String, format: SubtitleFormat, fps: Double): List<SubtitleCue> {
        val autoFmt = if (format == SubtitleFormat.SRT) detectFormat(content) else format
        return parseContent(content, autoFmt, fps)
    }

    fun convert(cues: List<SubtitleCue>, targetFormat: SubtitleFormat): String {
        return exportContent(cues, targetFormat)
    }

    fun convert(cues: List<SubtitleCue>, targetFormat: SubtitleFormat, fps: Float): String {
        return exportContent(cues, targetFormat, fps.toDouble())
    }

    fun convert(cues: List<SubtitleCue>, targetFormat: SubtitleFormat, fps: Double): String {
        return exportContent(cues, targetFormat, fps)
    }

    fun parseTimestamp(text: String, fps: Double = 23.976): Long {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0L

        if (trimmed.endsWith("ms", ignoreCase = true)) {
            return trimmed.dropLast(2).trim().toLongOrNull() ?: 0L
        }
        if (trimmed.endsWith("s", ignoreCase = true)) {
            return ((trimmed.dropLast(1).trim().toDoubleOrNull() ?: 0.0) * 1000.0).toLong()
        }

        return try {
            val parts = trimmed.replace(',', '.').split(":")
            when (parts.size) {
                4 -> {
                    val h = parts[0].toLong()
                    val m = parts[1].toLong()
                    val s = parts[2].toLong()
                    val f = parts[3].toLong()
                    val frameMs = (f * 1000.0 / fps).toLong()
                    (h * 3600_000L) + (m * 60_000L) + (s * 1000L) + frameMs
                }
                3 -> {
                    val h = parts[0].toLong()
                    val m = parts[1].toLong()
                    val secParts = parts[2].split(".")
                    val s = secParts[0].toLong()
                    val ms = if (secParts.size > 1) {
                        val raw = secParts[1]
                        if (raw.length == 2) raw.toLong() * 10L else raw.padEnd(3, '0').take(3).toLong()
                    } else 0L
                    (h * 3600_000L) + (m * 60_000L) + (s * 1000L) + ms
                }
                2 -> {
                    val m = parts[0].toLong()
                    val secParts = parts[1].split(".")
                    val s = secParts[0].toLong()
                    val ms = if (secParts.size > 1) {
                        val raw = secParts[1]
                        if (raw.length == 2) raw.toLong() * 10L else raw.padEnd(3, '0').take(3).toLong()
                    } else 0L
                    (m * 60_000L) + (s * 1000L) + ms
                }
                else -> trimmed.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    fun formatTimestamp(timeMs: Long, format: SubtitleFormat = SubtitleFormat.ASS, fps: Double = 25.0): String {
        val totalMs = maxOf(0L, timeMs)
        val hours = totalMs / 3600_000L
        val minutes = (totalMs % 3600_000L) / 60_000L
        val seconds = (totalMs % 60_000L) / 1000L
        val ms = totalMs % 1000L

        return when (format) {
            SubtitleFormat.ASS, SubtitleFormat.SSA -> {
                val cs = ms / 10L
                String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, cs)
            }
            SubtitleFormat.SRT -> {
                String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
            }
            SubtitleFormat.VTT, SubtitleFormat.TTML, SubtitleFormat.DFXP -> {
                String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, ms)
            }
            SubtitleFormat.SUB -> {
                val frame = (totalMs / 1000.0 * fps).toLong()
                "$frame"
            }
            SubtitleFormat.STL -> {
                val frame = ((totalMs % 1000L) * fps / 1000.0).toLong().coerceIn(0L, (fps - 1).toLong())
                String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frame)
            }
            SubtitleFormat.LRC -> {
                val totalSec = totalMs / 1000L
                val m = totalSec / 60L
                val s = totalSec % 60L
                val cs = ms / 10L
                String.format(Locale.US, "%02d:%02d.%02d", m, s, cs)
            }
            else -> {
                String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, ms)
            }
        }
    }

    fun parseContent(content: String, format: SubtitleFormat, fps: Double = 23.976): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()

        val result = when (format) {
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser.parse(content)
            SubtitleFormat.VTT -> parseVtt(content)
            SubtitleFormat.SUB -> parseMicroDvd(content, fps)
            SubtitleFormat.SBV -> parseSbv(content)
            SubtitleFormat.SMI -> parseSami(content)
            SubtitleFormat.LRC -> parseLrc(content)
            SubtitleFormat.TTML, SubtitleFormat.DFXP -> parseTtmlOrDfxp(content)
            SubtitleFormat.STL -> parseStl(content, fps)
            SubtitleFormat.TXT, SubtitleFormat.CLEAN_TEXT -> {
                val auto = detectFormat(content)
                if (auto != SubtitleFormat.TXT && auto != SubtitleFormat.SRT) {
                    parseContent(content, auto, fps)
                } else {
                    parseSrt(content)
                }
            }
            else -> parseSrt(content)
        }

        if (AssParser.lastParsedStyleMap.isEmpty()) {
            AssParser.lastParsedStyleMap["Default"] = AssParser.AssStyleDef(name = "Default")
            AssParser.rebuildHeaderFromStyleMap()
        }

        return result
    }

    private fun parseMicroDvd(content: String, fps: Double): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var index = 1
        val regex = Regex("^\\{(\\d+)\\}\\{(\\d+)\\}(.*)", RegexOption.MULTILINE)
        content.lines().forEach { line ->
            val trimmed = line.trim()
            val match = regex.find(trimmed)
            if (match != null) {
                val startFrame = match.groupValues[1].toLongOrNull() ?: 0L
                val endFrame = match.groupValues[2].toLongOrNull() ?: 0L
                val rawText = match.groupValues[3]

                val startMs = (startFrame * 1000.0 / fps).toLong()
                val endMs = (endFrame * 1000.0 / fps).toLong()

                val normText = rawText.replace("|", "\n")
                val cleanText = stripAllSubtitleTags(normText)

                if (cleanText.isNotBlank()) {
                    cues.add(
                        SubtitleCue(
                            index = index++,
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            text = cleanText,
                            rawText = rawText
                        )
                    )
                }
            }
        }
        return cues
    }

    private fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.replace("\r\n", "\n").replace("\r", "\n").split("\n\n")

        var autoIndex = 1
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            var timeLineIndex = -1
            for (i in lines.indices) {
                if (lines[i].contains("-->")) {
                    timeLineIndex = i
                    break
                }
            }

            if (timeLineIndex != -1) {
                val timeLine = lines[timeLineIndex]
                val timeParts = timeLine.split("-->").map { it.trim() }
                if (timeParts.size >= 2) {
                    val startMs = parseTimestamp(timeParts[0])
                    val endMs = parseTimestamp(timeParts[1])
                    val textLines = lines.drop(timeLineIndex + 1)
                    val rawText = textLines.joinToString("\n")
                    val cleanText = stripAllSubtitleTags(rawText)

                    if (cleanText.isNotBlank()) {
                        cues.add(
                            SubtitleCue(
                                index = autoIndex++,
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = cleanText,
                                rawText = rawText
                            )
                        )
                    }
                }
            }
        }
        return cues
    }

    private fun parseVtt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()

        var autoIndex = 1
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("WEBVTT", ignoreCase = true) || line.startsWith("NOTE", ignoreCase = true)) {
                i++
                continue
            }

            if (line.contains("-->")) {
                val timeParts = line.split("-->").map { it.trim() }
                if (timeParts.size >= 2) {
                    val startMs = parseTimestamp(timeParts[0])
                    val endPart = timeParts[1].split(" ")[0]
                    val endMs = parseTimestamp(endPart)

                    i++
                    val textLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        textLines.add(lines[i].trim())
                        i++
                    }
                    val rawText = textLines.joinToString("\n")
                    val cleanText = stripAllSubtitleTags(rawText)

                    if (cleanText.isNotBlank()) {
                        cues.add(
                            SubtitleCue(
                                index = autoIndex++,
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = cleanText,
                                rawText = rawText
                            )
                        )
                    }
                }
            } else {
                i++
            }
        }
        return cues
    }

    private fun parseSbv(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.replace("\r\n", "\n").split("\n\n")
        var autoIndex = 1

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            val timeLine = lines[0]
            if (timeLine.contains(",")) {
                val parts = timeLine.split(",")
                if (parts.size >= 2) {
                    val startMs = parseTimestamp(parts[0])
                    val endMs = parseTimestamp(parts[1])
                    val rawText = lines.drop(1).joinToString("\n")
                    val cleanText = stripAllSubtitleTags(rawText)

                    if (cleanText.isNotBlank()) {
                        cues.add(
                            SubtitleCue(
                                index = autoIndex++,
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = cleanText,
                                rawText = rawText
                            )
                        )
                    }
                }
            }
        }
        return cues
    }

    private fun parseSami(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val syncRegex = Regex("<SYNC\\s+Start=(\\d+)>(.*?)(?=<SYNC|\\s*</BODY>|\\s*</SAMI>|$)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = syncRegex.findAll(content).toList()

        var autoIndex = 1
        for (i in matches.indices) {
            val match = matches[i]
            val startMs = match.groupValues[1].toLongOrNull() ?: 0L
            val nextStartMs = if (i + 1 < matches.size) {
                matches[i + 1].groupValues[1].toLongOrNull() ?: (startMs + 3000L)
            } else {
                startMs + 3000L
            }

            val bodyPart = match.groupValues[2]
            val pRegex = Regex("<P[^>]*>(.*)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val pMatch = pRegex.find(bodyPart)
            val rawHtml = pMatch?.groupValues?.get(1) ?: bodyPart

            var text = rawHtml.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace("&nbsp;", " ", ignoreCase = true)
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
            text = stripAllSubtitleTags(text)

            if (text.isNotBlank()) {
                cues.add(
                    SubtitleCue(
                        index = autoIndex++,
                        startTimeMs = startMs,
                        endTimeMs = nextStartMs,
                        text = text,
                        rawText = rawHtml
                    )
                )
            }
        }
        return cues
    }

    private fun parseLrc(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lrcRegex = Regex("\\[(\\d+):(\\d{2})[\\.:](\\d{2,3})\\](.*)")
        val lines = content.lines()

        val parsedList = mutableListOf<Pair<Long, String>>()
        for (line in lines) {
            val trimmed = line.trim()
            val match = lrcRegex.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val subSecRaw = match.groupValues[3]
                val subSecMs = if (subSecRaw.length == 2) subSecRaw.toLong() * 10L else subSecRaw.take(3).padEnd(3, '0').toLong()
                val startMs = (min * 60_000L) + (sec * 1000L) + subSecMs
                val text = stripAllSubtitleTags(match.groupValues[4])
                if (text.isNotBlank()) {
                    parsedList.add(Pair(startMs, text))
                }
            }
        }

        parsedList.sortBy { it.first }
        var autoIndex = 1
        for (i in parsedList.indices) {
            val (startMs, text) = parsedList[i]
            val endMs = if (i + 1 < parsedList.size) parsedList[i + 1].first else (startMs + 3000L)
            cues.add(
                SubtitleCue(
                    index = autoIndex++,
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    text = text
                )
            )
        }
        return cues
    }

    private fun parseTtmlOrDfxp(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val pRegex = Regex("<p[^>]*begin=[\"']([^\"']+)[\"'][^>]*end=[\"']([^\"']+)[\"'][^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val matches = pRegex.findAll(content).toList()

        var autoIndex = 1
        for (match in matches) {
            val startStr = match.groupValues[1]
            val endStr = match.groupValues[2]
            val innerXml = match.groupValues[3]

            val startMs = parseTimestamp(startStr)
            val endMs = parseTimestamp(endStr)

            var text = innerXml.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
            text = stripAllSubtitleTags(text)

            if (text.isNotBlank()) {
                cues.add(
                    SubtitleCue(
                        index = autoIndex++,
                        startTimeMs = startMs,
                        endTimeMs = endMs,
                        text = text,
                        rawText = innerXml
                    )
                )
            }
        }
        return cues
    }

    private fun parseStl(content: String, fps: Double): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lineRegex = Regex("^(\\d{2}:\\d{2}:\\d{2}:\\d{2})\\s*,\\s*(\\d{2}:\\d{2}:\\d{2}:\\d{2})\\s*,\\s*(.*)", RegexOption.MULTILINE)
        var autoIndex = 1

        content.lines().forEach { line ->
            val trimmed = line.trim()
            val match = lineRegex.find(trimmed)
            if (match != null) {
                val startMs = parseTimestamp(match.groupValues[1], fps)
                val endMs = parseTimestamp(match.groupValues[2], fps)
                val text = stripAllSubtitleTags(match.groupValues[3].replace("|", "\n"))

                if (text.isNotBlank()) {
                    cues.add(
                        SubtitleCue(
                            index = autoIndex++,
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            text = text
                        )
                    )
                }
            }
        }
        return cues
    }

    fun exportContent(cues: List<SubtitleCue>, format: SubtitleFormat, fps: Double = 23.976): String {
        return when (format) {
            SubtitleFormat.ASS, SubtitleFormat.SSA -> AssParser.export(cues)
            SubtitleFormat.VTT -> exportVtt(cues)
            SubtitleFormat.SUB -> exportMicroDvd(cues, fps)
            SubtitleFormat.SBV -> exportSbv(cues)
            SubtitleFormat.SMI -> exportSami(cues)
            SubtitleFormat.LRC -> exportLrc(cues)
            SubtitleFormat.TTML, SubtitleFormat.DFXP -> exportTtmlOrDfxp(cues)
            SubtitleFormat.STL -> exportStl(cues, fps)
            SubtitleFormat.TXT, SubtitleFormat.CLEAN_TEXT -> exportTxt(cues)
            else -> exportSrt(cues)
        }
    }

    private fun exportSrt(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        cues.forEachIndexed { i, cue ->
            sb.appendLine("${i + 1}")
            val start = formatTimestamp(cue.startTimeMs, SubtitleFormat.SRT)
            val end = formatTimestamp(cue.endTimeMs, SubtitleFormat.SRT)
            sb.appendLine("$start --> $end")
            val cleanText = stripAllSubtitleTags(cue.text)
            sb.appendLine(cleanText)
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private fun exportVtt(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        sb.appendLine("WEBVTT")
        sb.appendLine()
        cues.forEachIndexed { i, cue ->
            sb.appendLine("${i + 1}")
            val start = formatTimestamp(cue.startTimeMs, SubtitleFormat.VTT)
            val end = formatTimestamp(cue.endTimeMs, SubtitleFormat.VTT)
            sb.appendLine("$start --> $end")
            val cleanText = stripAllSubtitleTags(cue.text)
            sb.appendLine(cleanText)
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private fun exportMicroDvd(cues: List<SubtitleCue>, fps: Double): String {
        val sb = StringBuilder()
        cues.forEach { cue ->
            val startFrame = (cue.startTimeMs * fps / 1000.0).toLong().coerceAtLeast(0L)
            val endFrame = (cue.endTimeMs * fps / 1000.0).toLong().coerceAtLeast(startFrame + 1)
            val cleanText = stripAllSubtitleTags(cue.text).replace("\n", "|")
            sb.appendLine("{$startFrame}{$endFrame}$cleanText")
        }
        return sb.toString().trimEnd()
    }

    private fun exportSbv(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        cues.forEach { cue ->
            val start = formatTimestamp(cue.startTimeMs, SubtitleFormat.VTT).let {
                if (it.startsWith("0")) it.substring(1) else it
            }
            val end = formatTimestamp(cue.endTimeMs, SubtitleFormat.VTT).let {
                if (it.startsWith("0")) it.substring(1) else it
            }
            sb.appendLine("$start,$end")
            val cleanText = stripAllSubtitleTags(cue.text)
            sb.appendLine(cleanText)
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private fun exportSami(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        sb.appendLine("<SAMI>")
        sb.appendLine("<HEAD>")
        sb.appendLine("<TITLE>Subtitle</TITLE>")
        sb.appendLine("<STYLE TYPE=\"text/css\">")
        sb.appendLine("<!--")
        sb.appendLine("P { font-family: Arial; font-size: 18pt; text-align: center; color: white; }")
        sb.appendLine(".ENCC { Name: English; lang: en-US; SAMIType: CC; }")
        sb.appendLine("-->")
        sb.appendLine("</STYLE>")
        sb.appendLine("</HEAD>")
        sb.appendLine("<BODY>")
        cues.forEach { cue ->
            val cleanText = stripAllSubtitleTags(cue.text).replace("\n", "<br>")
            sb.appendLine("<SYNC Start=${cue.startTimeMs}><P Class=ENCC>$cleanText")
            sb.appendLine("<SYNC Start=${cue.endTimeMs}><P Class=ENCC>&nbsp;")
        }
        sb.appendLine("</BODY>")
        sb.appendLine("</SAMI>")
        return sb.toString()
    }

    private fun exportLrc(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        cues.forEach { cue ->
            val totalMs = cue.startTimeMs
            val min = totalMs / 60_000L
            val sec = (totalMs % 60_000L) / 1000L
            val cs = (totalMs % 1000L) / 10L
            val timeStr = String.format(Locale.US, "[%02d:%02d.%02d]", min, sec, cs)
            val cleanText = stripAllSubtitleTags(cue.text).replace("\n", " ")
            sb.appendLine("$timeStr$cleanText")
        }
        return sb.toString().trimEnd()
    }

    private fun exportTtmlOrDfxp(cues: List<SubtitleCue>): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<tt xmlns=\"http://www.w3.org/ns/ttml\" xmlns:tts=\"http://www.w3.org/ns/ttml#styling\" xml:lang=\"en\">")
        sb.appendLine("  <head>")
        sb.appendLine("    <styling>")
        sb.appendLine("      <style xml:id=\"s0\" tts:fontFamily=\"Arial\" tts:fontSize=\"18px\" tts:color=\"white\" tts:textAlign=\"center\"/>")
        sb.appendLine("    </styling>")
        sb.appendLine("  </head>")
        sb.appendLine("  <body>")
        sb.appendLine("    <div>")

        cues.forEach { cue ->
            val start = formatTimestamp(cue.startTimeMs, SubtitleFormat.VTT)
            val end = formatTimestamp(cue.endTimeMs, SubtitleFormat.VTT)
            val cleanText = stripAllSubtitleTags(cue.text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br/>")

            sb.appendLine("      <p begin=\"$start\" end=\"$end\">$cleanText</p>")
        }

        sb.appendLine("    </div>")
        sb.appendLine("  </body>")
        sb.appendLine("</tt>")
        return sb.toString()
    }

    private fun exportStl(cues: List<SubtitleCue>, fps: Double): String {
        val sb = StringBuilder()
        cues.forEach { cue ->
            val start = formatTimecode(cue.startTimeMs, fps)
            val end = formatTimecode(cue.endTimeMs, fps)
            val cleanText = stripAllSubtitleTags(cue.text).replace("\n", " | ")
            sb.appendLine("$start , $end , $cleanText")
        }
        return sb.toString().trimEnd()
    }

    private fun exportTxt(cues: List<SubtitleCue>): String {
        return cues.joinToString("\n") { stripAllSubtitleTags(it.text) }
    }

    private fun formatTimecode(timeMs: Long, fps: Double): String {
        val totalMs = maxOf(0L, timeMs)
        val hours = totalMs / 3600_000L
        val minutes = (totalMs % 3600_000L) / 60_000L
        val seconds = (totalMs % 60_000L) / 1000L
        val remMs = totalMs % 1000L
        val frame = (remMs * fps / 1000.0).toLong().coerceIn(0L, (fps - 1).toLong())
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", hours, minutes, seconds, frame)
    }

}
