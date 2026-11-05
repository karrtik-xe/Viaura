package com.example.util

/**
 * One playable entry of an M3U / M3U8 playlist (IPTV channel or plain media URL).
 * Everything the file carries is kept: name, real stream URL, logo, group/category and the
 * per-stream HTTP options (User-Agent, referrer, extra headers) that IPTV lists often need.
 */
data class M3uEntry(
    val name: String,
    val url: String,
    val logo: String? = null,
    val group: String? = null,
    val tvgId: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    val extraHeaders: Map<String, String> = emptyMap()
)

data class M3uParseResult(
    val entries: List<M3uEntry>,
    val title: String? = null,
    /** Non-comment lines that were not a usable URL (kept only for diagnostics). */
    val skippedLines: Int = 0
)
