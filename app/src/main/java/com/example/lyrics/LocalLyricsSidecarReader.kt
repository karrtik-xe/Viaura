/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.example.utils.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.domain.lyrics.Lyrics
import com.example.domain.lyrics.LyricsSourceType
import com.example.player.AudioMetadataExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Local lyric extraction for the repository; the player also refreshes metadata through its existing extractor. */
object EmbeddedLyricsExtractor {
    private const val TAG = "Lyrics"

    suspend fun extractEmbeddedLyrics(context: Context, mediaPath: String?): Lyrics? = withContext(Dispatchers.IO) {
        if (mediaPath.isNullOrBlank()) return@withContext null
        val uri = runCatching { Uri.parse(mediaPath) }.getOrNull() ?: return@withContext null
        val scheme = uri.scheme
        val local = scheme.isNullOrBlank() || scheme.equals("file", true) || scheme.equals("content", true)
        val filePath = when {
            scheme.equals("file", true) -> uri.path ?: mediaPath.removePrefix("file://")
            scheme.isNullOrBlank() -> mediaPath
            else -> null
        }
        val file = filePath?.let(::File)

        fun parse(raw: String?, source: LyricsSourceType): Lyrics? =
            raw?.takeIf { it.isNotBlank() }?.let { LyricsUtils.parseLyrics(it, sourceType = source) }
                ?.takeIf { it.isValid() }

        if (local && file != null && file.exists()) {
            val lrc = File(file.parentFile ?: file, file.nameWithoutExtension + ".lrc")
            runCatching { if (lrc.isFile && lrc.length() > 0) lrc.readText(Charsets.UTF_8) else null }
                .getOrNull()?.let { parse(it, LyricsSourceType.LOCAL) }?.let {
                    Log.d(TAG, "Found sidecar .lrc")
                    return@withContext it
                }

            val embedded = runCatching {
                when (file.extension.lowercase()) {
                    "mp3" -> AudioMetadataExtractor.extractId3Lyrics(file)
                    "m4a", "mp4", "aac" -> AudioMetadataExtractor.extractMp4Lyrics(file)
                    "flac", "ogg", "opus" -> AudioMetadataExtractor.extractVorbisLyrics(file)
                    else -> null
                }
            }.getOrNull()
            parse(embedded, LyricsSourceType.EMBEDDED)?.let {
                Log.d(TAG, "Found embedded lyrics")
                return@withContext it
            }
        }

        // Android/vendor retrievers sometimes expose METADATA_KEY_LYRICS under key 1000.
        val retrieverLyrics = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (scheme.isNullOrBlank()) retriever.setDataSource(mediaPath)
                else if (scheme.equals("file", true)) retriever.setDataSource(filePath ?: mediaPath.removePrefix("file://"))
                else retriever.setDataSource(context, uri)
                retriever.extractMetadata(1000)
            } finally {
                retriever.release()
            }
        }.getOrNull()
        parse(retrieverLyrics, LyricsSourceType.EMBEDDED)?.let { return@withContext it }

        if (local && file != null) {
            val txt = File(file.parentFile ?: file, file.nameWithoutExtension + ".txt")
            val text = runCatching {
                if (txt.isFile && txt.length() in 10..100_000) {
                    txt.readText(Charsets.UTF_8).takeIf { it.lines().size >= 4 }
                } else null
            }.getOrNull()
            parse(text, LyricsSourceType.LOCAL)?.let {
                Log.d(TAG, "Found sidecar .txt")
                return@withContext it
            }
        }
        null
    }
}
