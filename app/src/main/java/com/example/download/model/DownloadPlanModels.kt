package com.example.download

import org.json.JSONArray
import org.json.JSONObject

/** One file that has to be fetched (a video stream, an audio stream or a complete muxed file). */
data class PlanPart(
    val role: String,                    // "video" | "audio" | "muxed"
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val sizeBytes: Long = -1L,           // -1 = the server did not tell
    val ext: String = "",
    val protocol: String = "http",       // "http" | "hls" | "dash" | "ytdlp"
    val formatId: String = "",
    val label: String = "",              // audio: "Original", "Hindi" ...
    val language: String = "",
    val isDefault: Boolean = false,
    val chunkSize: Long = 0L,            // > 0 = the host wants ranged requests of at most this size
    val fragments: List<String> = emptyList(),   // "dash": init + media segment URLs
    val codec: String = ""
)

data class PlanSubtitle(val language: String, val name: String, val url: String, val ext: String, val isAuto: Boolean = false)

data class PlanChapter(val title: String, val startMs: Long, val endMs: Long)

/** Everything the download engine needs. It is stored as JSON with the download so it can continue after an app restart. */
data class DownloadPlan(
    val pageUrl: String,
    val title: String,
    val uploader: String = "",
    val thumbnailUrl: String = "",
    val durationSec: Long = 0L,
    val isAudio: Boolean = false,
    val parts: List<PlanPart> = emptyList(),
    val subtitles: List<PlanSubtitle> = emptyList(),
    val chapters: List<PlanChapter> = emptyList(),
    val attachThumbnail: Boolean = true,
    val fetchLyrics: Boolean = false,
    val lyrics: String = "",
    val qualityLabel: String = "",
    /** Backup cover (the thumbnail the app showed) tried when [thumbnailUrl] is empty or can not be loaded. */
    val fallbackThumbnailUrl: String = "",
    /** Series name: the finished file is saved in its own folder (Movies/Viaura/<group>) and listed in that folder of Downloads. */
    val group: String = ""
) {
    fun toJson(): String {
        val o = JSONObject()
        o.put("pageUrl", pageUrl).put("title", title).put("uploader", uploader).put("thumbnailUrl", thumbnailUrl)
            .put("durationSec", durationSec).put("isAudio", isAudio).put("attachThumbnail", attachThumbnail)
            .put("fetchLyrics", fetchLyrics).put("lyrics", lyrics).put("qualityLabel", qualityLabel).put("fallbackThumb", fallbackThumbnailUrl).put("group", group)
        val ps = JSONArray()
        for (p in parts) {
            val po = JSONObject()
            po.put("role", p.role).put("url", p.url).put("size", p.sizeBytes).put("ext", p.ext).put("protocol", p.protocol)
                .put("formatId", p.formatId).put("label", p.label).put("language", p.language).put("isDefault", p.isDefault)
                .put("chunk", p.chunkSize).put("codec", p.codec)
            val h = JSONObject()
            for ((k, v) in p.headers) h.put(k, v)
            po.put("headers", h)
            val fr = JSONArray()
            p.fragments.forEach { fr.put(it) }
            po.put("fragments", fr)
            ps.put(po)
        }
        o.put("parts", ps)
        val ss = JSONArray()
        for (s in subtitles) {
            ss.put(JSONObject().put("lang", s.language).put("name", s.name).put("url", s.url).put("ext", s.ext).put("auto", s.isAuto))
        }
        o.put("subs", ss)
        val cs = JSONArray()
        for (c in chapters) cs.put(JSONObject().put("t", c.title).put("s", c.startMs).put("e", c.endMs))
        o.put("chapters", cs)
        return o.toString()
    }

    companion object {
        fun fromJson(text: String): DownloadPlan? = try {
            val o = JSONObject(text)
            val parts = ArrayList<PlanPart>()
            val ps = o.optJSONArray("parts") ?: JSONArray()
            for (i in 0 until ps.length()) {
                val po = ps.getJSONObject(i)
                val headers = LinkedHashMap<String, String>()
                po.optJSONObject("headers")?.let { h -> h.keys().forEach { k -> headers[k] = h.optString(k) } }
                val frags = ArrayList<String>()
                po.optJSONArray("fragments")?.let { a -> for (j in 0 until a.length()) frags.add(a.optString(j)) }
                parts.add(
                    PlanPart(
                        role = po.optString("role"), url = po.optString("url"), headers = headers,
                        sizeBytes = po.optLong("size", -1L), ext = po.optString("ext"), protocol = po.optString("protocol", "http"),
                        formatId = po.optString("formatId"), label = po.optString("label"), language = po.optString("language"),
                        isDefault = po.optBoolean("isDefault"), chunkSize = po.optLong("chunk", 0L), fragments = frags,
                        codec = po.optString("codec")
                    )
                )
            }
            val subs = ArrayList<PlanSubtitle>()
            o.optJSONArray("subs")?.let { a ->
                for (i in 0 until a.length()) {
                    val s = a.getJSONObject(i)
                    subs.add(PlanSubtitle(s.optString("lang"), s.optString("name"), s.optString("url"), s.optString("ext"), s.optBoolean("auto")))
                }
            }
            val chapters = ArrayList<PlanChapter>()
            o.optJSONArray("chapters")?.let { a ->
                for (i in 0 until a.length()) {
                    val c = a.getJSONObject(i)
                    chapters.add(PlanChapter(c.optString("t"), c.optLong("s"), c.optLong("e")))
                }
            }
            DownloadPlan(
                pageUrl = o.optString("pageUrl"), title = o.optString("title"), uploader = o.optString("uploader"),
                thumbnailUrl = o.optString("thumbnailUrl"), durationSec = o.optLong("durationSec"), isAudio = o.optBoolean("isAudio"),
                parts = parts, subtitles = subs, chapters = chapters, attachThumbnail = o.optBoolean("attachThumbnail", true),
                fetchLyrics = o.optBoolean("fetchLyrics"), lyrics = o.optString("lyrics"), qualityLabel = o.optString("qualityLabel"),
                fallbackThumbnailUrl = o.optString("fallbackThumb"), group = o.optString("group")
            )
        } catch (_: Throwable) {
            null
        }
    }
}
