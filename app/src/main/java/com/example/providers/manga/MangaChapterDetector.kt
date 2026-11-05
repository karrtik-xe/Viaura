package com.example.util.media

/**
 * Manga / comic providers: their "streams" are page images instead of a video. When a chapter's links are images, the app
 * opens the reader (portrait, page after page) instead of the video player.
 */
object MangaDetect {
    private val IMAGE_EXT = Regex("""\.(jpe?g|png|webp|gif|avif|bmp)(\?|#|$)""", RegexOption.IGNORE_CASE)
    private val IMAGE_TYPES = setOf("image", "images", "manga", "comic", "page", "pages", "jpg", "jpeg", "png", "webp", "gif", "avif")
    private val TRAILING_NUMBER = Regex("""(\d+)\s*$""")

    fun isImage(s: MediaStream): Boolean {
        val t = s.type.trim().lowercase().removePrefix("image/")
        return t in IMAGE_TYPES || IMAGE_EXT.containsMatchIn(s.link)
    }

    /** The pages in reading order, or null when these streams are not manga pages. */
    fun pagesOf(streams: List<MediaStream>): List<MediaStream>? {
        if (streams.isEmpty()) return null
        val pages = streams.filter { isImage(it) }
        if (pages.isEmpty()) return null
        val others = streams.filter { !isImage(it) }
        // a video chapter with a poster link must not be mistaken for a manga: any real video / playlist link means video
        if (others.any { MediaPlayback.rankOf(it) < 5 }) return null
        // a few leftover links of an unknown kind (cover, "next chapter" ...) are fine, as long as the images clearly dominate
        if (others.isNotEmpty() && (pages.size < 2 || pages.size <= others.size)) return null
        return inReadingOrder(pages.distinctBy { it.link })
    }

    /** Providers that fetch pages in parallel can report them out of order: "Page 12" style names put them back. */
    private fun inReadingOrder(pages: List<MediaStream>): List<MediaStream> {
        if (pages.size < 2) return pages
        val numbers = pages.map { TRAILING_NUMBER.find(it.server.trim())?.groupValues?.get(1)?.toIntOrNull() }
        if (numbers.any { it == null } || numbers.toSet().size != numbers.size) return pages
        return pages.zip(numbers).sortedBy { it.second ?: 0 }.map { it.first }
    }
}
