package com.example.data.lyrics

import android.util.Log
import android.util.LruCache
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.SyncedLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * API-key-free lyrics translation. It uses the public Google Translate web endpoints and falls
 * back to MyMemory when Google blocks a request. Translation needs an internet connection, but no
 * user-supplied key, billing account, or app backend.
 */
object LyricsTranslationService {
    private const val TAG = "Lyrics"
    private const val CHUNK_SIZE = 12
    private val markerRegex = Regex("""\[(\d+)]\s*(.*?)(?=\[\d+]\s*|$)""", setOf(RegexOption.DOT_MATCHES_ALL))
    private val cache = LruCache<String, Lyrics>(96)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val languages: List<Pair<String, String>> = listOf(
        "hi" to "हिन्दी", "en" to "English", "hinglish_casual" to "Hinglish (Romanized)",
        "romaji" to "Romanized / Pronunciation", "bn" to "বাংলা (Bengali)", "ta" to "தமிழ் (Tamil)",
        "te" to "తెలుగు (Telugu)", "mr" to "मराठी (Marathi)", "pa" to "ਪੰਜਾਬੀ (Punjabi)",
        "ur" to "اردو (Urdu)", "es" to "Español", "fr" to "Français", "de" to "Deutsch",
        "it" to "Italiano", "pt" to "Português", "ru" to "Русский", "ja" to "日本語",
        "ko" to "한국어", "zh-CN" to "简体中文", "ar" to "العربية"
    )

    suspend fun translate(lyrics: Lyrics, targetLanguage: String, cacheKey: String = ""): Lyrics? =
        withContext(Dispatchers.IO) {
            if (!lyrics.isValid() || targetLanguage.isBlank()) return@withContext null
            val key = "$cacheKey|${lyrics.hashCode()}|$targetLanguage"
            cache.get(key)?.let { return@withContext it }
            try {
                val translated = if (!lyrics.synced.isNullOrEmpty()) {
                    val mainTexts = lyrics.synced.map { it.line }
                    val backgroundTexts = lyrics.synced.map { it.background?.line.orEmpty() }
                    val mainTranslations = translateLines(mainTexts, targetLanguage)
                    val backgroundTranslations = translateLines(backgroundTexts, targetLanguage)
                    lyrics.copy(synced = lyrics.synced.mapIndexed { index, line ->
                        val main = mainTranslations.getOrNull(index).orEmpty()
                        val backing = backgroundTranslations.getOrNull(index).orEmpty()
                        line.copy(
                            translation = main.takeIf { it.isNotBlank() && !it.equals(line.line, true) } ?: line.translation,
                            background = line.background?.let { voice ->
                                voice.copy(translation = backing.takeIf { it.isNotBlank() && !it.equals(voice.line, true) } ?: voice.translation)
                            }
                        )
                    })
                } else {
                    val originals = lyrics.plain.orEmpty()
                    val results = translateLines(originals, targetLanguage)
                    lyrics.copy(plain = originals.mapIndexed { i, original ->
                        val translation = results.getOrNull(i).orEmpty()
                        if (translation.isNotBlank() && !translation.equals(original, true)) "$original\n$translation" else original
                    })
                }
                if (translated.isValid() && translated != lyrics) cache.put(key, translated)
                translated.takeIf { it != lyrics }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Lyrics translation failed quietly: ${e.message}")
                null
            }
        }

    private fun translateLines(lines: List<String>, targetLanguage: String): List<String?> {
        if (lines.isEmpty()) return emptyList()
        if (targetLanguage == "romaji" || targetLanguage == "hinglish_casual") {
            return lines.map { line ->
                if (line.isBlank()) return@map line
                val romanized = try {
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        android.icu.text.Transliterator.getInstance("Any-Latin").transliterate(line).trim()
                    } else null
                } catch (_: Exception) { null }
                romanized?.takeIf { it.isNotBlank() && !it.equals(line, true) }?.let {
                    if (targetLanguage == "hinglish_casual") it.replace(Regex("\\p{Mn}+"), "").replace("'", "").replace("’", "") else it
                }
            }
        }
        val output = MutableList<String?>(lines.size) { null }
        lines.indices.chunked(CHUNK_SIZE).forEach { indices ->
            val nonBlank = indices.filter { lines[it].isNotBlank() }
            if (nonBlank.isEmpty()) return@forEach
            val query = nonBlank.mapIndexed { local, source -> "[${local + 1}] ${lines[source].trim()}" }.joinToString("\n")
            val translatedBlock = translateBlock(query, targetLanguage)
            if (!translatedBlock.isNullOrBlank()) {
                val found = linkedMapOf<Int, String>()
                markerRegex.findAll(normalizeNumbers(translatedBlock)).forEach { match ->
                    val index = match.groupValues[1].toIntOrNull()
                    val value = match.groupValues[2].trim().trim(' ', '\n', '\r', '"', '\'')
                    if (index != null && value.isNotBlank()) found[index] = value
                }
                nonBlank.forEachIndexed { local, sourceIndex ->
                    found[local + 1]?.takeIf { it.isNotBlank() }?.let { output[sourceIndex] = it }
                }
            }
            // If the translator rewrites the numbered markers, recover only the missing rows;
            // do not let a failed batch turn every lyric line into an empty translation.
            nonBlank.filter { output[it].isNullOrBlank() }.forEach { index ->
                output[index] = translateSingle(lines[index], targetLanguage)
            }
        }
        return output
    }

    private fun translateBlock(text: String, targetLanguage: String): String? {
        try {
            val url = HttpUrl.Builder().scheme("https").host("translate.google.com").addPathSegment("m")
                .addQueryParameter("sl", "auto").addQueryParameter("tl", targetLanguage)
                .addQueryParameter("hl", "en-US").addQueryParameter("q", text).build()
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string().orEmpty()
                    val result = Jsoup.parse(html).selectFirst(".result-container")?.text()?.trim()
                    if (!result.isNullOrBlank()) return result
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Google web translation unavailable: ${e.message}")
        }
        return translateGoogleJson(text, targetLanguage)
    }

    private fun translateSingle(text: String, targetLanguage: String): String? {
        if (targetLanguage == "romaji" || targetLanguage == "hinglish_casual") {
            val local = try {
                if (android.os.Build.VERSION.SDK_INT >= 29) android.icu.text.Transliterator
                    .getInstance("Any-Latin").transliterate(text).trim() else null
            } catch (_: Exception) { null }
            if (!local.isNullOrBlank() && !local.equals(text, true)) return local
        }
        return translateGoogleJson(text, targetLanguage)?.takeIf { it.isNotBlank() }
            ?: translateMyMemory(text, targetLanguage)
    }

    private fun translateGoogleJson(text: String, targetLanguage: String): String? = try {
        val url = HttpUrl.Builder().scheme("https").host("translate.googleapis.com")
            .addPathSegment("translate_a").addPathSegment("single")
            .addQueryParameter("client", "gtx").addQueryParameter("sl", "auto")
            .addQueryParameter("tl", if (targetLanguage == "romaji" || targetLanguage == "hinglish_casual") "en" else targetLanguage)
            .addQueryParameter("dt", "t").addQueryParameter("q", text).build()
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val root = JSONArray(response.body?.string().orEmpty())
            val sentenceArray = root.optJSONArray(0) ?: return null
            buildString {
                for (i in 0 until sentenceArray.length()) {
                    val item = sentenceArray.optJSONArray(i) ?: continue
                    val piece = item.optString(0)
                    if (piece.isNotBlank() && piece != "null") append(piece)
                }
            }.trim().takeIf { it.isNotBlank() }
        }
    } catch (e: Exception) {
        Log.d(TAG, "Google translation fallback unavailable: ${e.message}")
        null
    }

    private fun translateMyMemory(text: String, targetLanguage: String): String? = try {
        val lang = if (targetLanguage == "romaji" || targetLanguage == "hinglish_casual") "en" else targetLanguage
        val url = HttpUrl.Builder().scheme("https").host("api.mymemory.translated.net").addPathSegment("get")
            .addQueryParameter("q", text).addQueryParameter("langpair", "autodetect|$lang").build()
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            JSONObject(response.body?.string().orEmpty()).optJSONObject("responseData")?.optString("translatedText")
                ?.let { Jsoup.parse(it).text().trim() }?.takeIf { it.isNotBlank() && !it.equals(text, true) }
        }
    } catch (_: Exception) { null }

    private fun normalizeNumbers(text: String): String = buildString(text.length) {
        text.forEach { c ->
            val digit = Character.digit(c, 10)
            append(if (digit in 0..9) ('0'.code + digit).toChar() else c)
        }
    }

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
