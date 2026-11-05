package com.example.util

/**
 * Detects season / episode numbers (and the show title) from a video file name written in almost any
 * common style: S01E12, S1E012, 1x12, Season 1 Episode 12, "Title - 12 [1080p]", E12, Ep.12, #12,
 * OVA 02, 第12話, "[Group] Title - 05v2 [ABCD1234]" ...
 *
 * The episode number is always the one found in the name – it is never re-counted – so renaming keeps
 * the original episode numbers.
 */
data class ParsedEpisode(
    val title: String,
    val season: Int?,
    val episode: Int?,
    val episodeEnd: Int? = null
)

object EpisodeNameParser {

    private val I = RegexOption.IGNORE_CASE

    // Tokens that look like numbers but are quality / codec / hash / year info. They are blanked out
    // (same length) before the episode patterns run.
    private val noise = listOf(
        Regex("""(?<![A-Za-z0-9])\d{3,4}\s?[pi](?![A-Za-z0-9])""", I),
        Regex("""(?<![A-Za-z0-9])\d{3,4}x\d{3,4}(?![A-Za-z0-9])""", I),
        Regex(
            """(?<![A-Za-z0-9])(?:[xh]\.?26[45]|hevc|avc|av1|vp9|aac|ac3|eac3|dts|flac|opus|mp3|truehd|atmos|bluray|blu-ray|bdrip|brrip|webrip|web-dl|webdl|hdtv|dvdrip|dvd|hdr|sdr|uhd|dual|multi|sub|subs|dub|raw)(?:\d(?:\.\d)?)?(?![A-Za-z0-9])""",
            I
        ),
        Regex("""(?<![A-Za-z0-9])\d{1,2}\s?(?:bit|kbps|fps|mbps)(?![A-Za-z0-9])""", I),
        Regex("""(?<![A-Za-z0-9])\d\.\d(?:ch)?(?![A-Za-z0-9])""", I),
        Regex("""(?<![A-Za-z0-9])[0-9A-Fa-f]{8}(?![A-Za-z0-9])"""),
        Regex("""(?<![A-Za-z0-9])(?:19|20)\d{2}(?![A-Za-z0-9])""")
    )

    private val pSxe = Regex(
        """(?<![A-Za-z0-9])S(\d{1,2})[\s_.-]*E(?:P)?(\d{1,4})(?:-E?P?(\d{1,4})|E(?:P)?(\d{1,4}))?(?!\d)""", I
    )
    private val pSeasonEp = Regex(
        """(?<![A-Za-z0-9])Season\s*(\d{1,2})[\s,._-]*(?:Episode|Ep|E)\s*\.?\s*(\d{1,4})(?!\d)""", I
    )
    private val pNxM = Regex("""(?<![A-Za-z0-9])(\d{1,2})x(\d{2,3})(?![A-Za-z0-9])""", I)
    private val pJpEp = Regex("""第\s*(\d{1,4})\s*[話话集回]""")
    private val pSpecial = Regex("""(?<![A-Za-z])(?:OVA|OAD|ONA|SP|Specials?)[\s._-]*(\d{1,3})(?![A-Za-z0-9])""", I)
    private val pEpWord = Regex("""(?<![A-Za-z])(?:Episodes?|Eps?\.?|E)\s*#?\s*(\d{1,4})(?![A-Za-z0-9])""", I)
    private val pHash = Regex("""#\s*(\d{1,4})(?![A-Za-z0-9])""")
    private val pDash = Regex("""(?:^|\s)-\s*(\d{1,4})(?:v\d+)?(?=\s|$)""", I)
    private val pNum = Regex("""(?<![A-Za-z0-9.])(\d{1,4})(?![A-Za-z0-9]|\.\d)""")

    private val pSeasonS = Regex("""(?<![A-Za-z0-9])S(\d{1,2})(?![A-Za-z0-9])""", I)
    private val pSeasonWord = Regex("""(?<![A-Za-z0-9])Season\s*(\d{1,2})(?![A-Za-z0-9])""", I)
    private val pSeasonOrd = Regex("""(?<![A-Za-z0-9])(\d{1,2})(?:st|nd|rd|th)\s+Season""", I)
    private val pSeasonJp = Regex("""第\s*(\d{1,2})\s*期""")

    private val brackets = Regex("""[\[\]\(\)\{\}]""")
    private val leadingGroup = Regex("""^\s*[\[\(\{][^\]\)\}]*[\]\)\}]\s*""")
    private val yearInBrackets = Regex("""[\[\(]\s*(?:19|20)\d{2}\s*[\]\)]""")
    private val openTail = Regex("""[\[\(\{][^\]\)\}]*$""")
    private val trailingSep = Regex("""[\s._\-–:]+$""")
    private val leadingSep = Regex("""^[\s._\-–]+""")
    private val manySpaces = Regex("""\s+""")

    private fun blank(text: String, m: MatchResult): String {
        val sb = StringBuilder(text)
        for (i in m.range) sb.setCharAt(i, ' ')
        return sb.toString()
    }

    private fun Regex.intGroup(m: MatchResult, i: Int): Int? =
        m.groupValues.getOrNull(i)?.takeIf { it.isNotEmpty() }?.toIntOrNull()

    /** [baseName] = file name WITHOUT extension. */
    fun parse(baseName: String): ParsedEpisode {
        var s = baseName
        val spaces = s.count { it == ' ' }
        val seps = s.count { it == '.' || it == '_' }
        s = if (seps > spaces) s.replace('.', ' ').replace('_', ' ') else s.replace('_', ' ')

        var n = s
        for (p in noise) {
            var guard = 0
            while (guard++ < 64) {
                val m = p.find(n) ?: break
                n = blank(n, m)
            }
        }
        n = brackets.replace(n, " ")

        var season: Int? = null
        var ep: Int? = null
        var end: Int? = null
        var start = n.length

        pSxe.find(n)?.let { m ->
            season = pSxe.intGroup(m, 1)
            ep = pSxe.intGroup(m, 2)
            val e2 = pSxe.intGroup(m, 3) ?: pSxe.intGroup(m, 4)
            val e1 = ep
            if (e2 != null && e1 != null && e2 > e1) end = e2
            start = m.range.first
        }
        if (ep == null) pSeasonEp.find(n)?.let { m ->
            season = pSeasonEp.intGroup(m, 1); ep = pSeasonEp.intGroup(m, 2); start = m.range.first
        }
        if (ep == null) pNxM.find(n)?.let { m ->
            season = pNxM.intGroup(m, 1); ep = pNxM.intGroup(m, 2); start = m.range.first
        }
        if (ep == null) pJpEp.find(n)?.let { m ->
            ep = pJpEp.intGroup(m, 1); start = m.range.first
        }
        if (ep == null) pSpecial.find(n)?.let { m ->
            season = 0; ep = pSpecial.intGroup(m, 1); start = m.range.first
        }
        if (ep == null) pEpWord.find(n)?.let { m ->
            ep = pEpWord.intGroup(m, 1); start = m.range.first
        }
        if (ep == null) pHash.find(n)?.let { m ->
            ep = pHash.intGroup(m, 1); start = m.range.first
        }
        if (ep == null) pDash.findAll(n).lastOrNull()?.let { m ->
            ep = pDash.intGroup(m, 1)
            start = (m.groups[1]?.range?.first ?: 1) - 1
        }
        if (ep == null) pNum.findAll(n).lastOrNull()?.let { m ->
            ep = pNum.intGroup(m, 1); start = m.range.first
        }

        if (season == null) {
            for (p in listOf(pSeasonS, pSeasonWord, pSeasonOrd, pSeasonJp)) {
                val m = p.find(n) ?: continue
                season = p.intGroup(m, 1)
                start = minOf(start, m.range.first)
                break
            }
        }

        var t = if (ep == null && season == null) s else s.substring(0, start.coerceIn(0, s.length))
        var guard = 0
        while (guard++ < 8) {
            val t2 = leadingGroup.replace(t, "")
            if (t2 == t) break
            t = t2
        }
        t = yearInBrackets.replace(t, " ")
        t = openTail.replace(t, "")
        t = trailingSep.replace(t, "")
        t = leadingSep.replace(t, "")
        t = manySpaces.replace(t, " ").trim()
        return ParsedEpisode(t, season, ep, end)
    }
}
