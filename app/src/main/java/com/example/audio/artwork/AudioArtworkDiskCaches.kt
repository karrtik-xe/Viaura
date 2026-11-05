package com.example.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AudioTrackItem
import com.example.ui.theme.AccentGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

internal object RemoteAudioThumbnailDiskCache {
    private const val VERSION = "v1"
    private const val MAX_BYTES = 48L * 1024L * 1024L

    private fun dir(context: Context): File =
        File(context.cacheDir, "remote_audio_thumbnail_cache").apply { mkdirs() }

    private fun key(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$VERSION|$url".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return digest
    }

    fun read(context: Context, url: String): Bitmap? {
        val file = File(dir(context), "${key(url)}.png")
        if (!file.isFile || file.length() <= 0L) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath)?.takeUnless { it.isRecycled } }.getOrNull()
    }

    fun write(context: Context, url: String, bitmap: Bitmap) {
        val target = File(dir(context), "${key(url)}.png")
        if (target.isFile && target.length() > 0L) return
        val temp = File(dir(context), "${key(url)}.tmp")
        runCatching {
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (!temp.renameTo(target)) { target.delete(); temp.renameTo(target) }
            val files = dir(context).listFiles { f -> f.isFile && f.extension.equals("png", true) }.orEmpty()
            var total = files.sumOf { it.length() }
            for (file in files.sortedBy { it.lastModified() }) {
                if (total <= MAX_BYTES) break
                total -= file.length()
                file.delete()
            }
        }.onFailure { runCatching { temp.delete() } }
    }
}

/**
 * Asynchronous, cached loader for audio album artwork & track thumbnails.
 * Tries embedded ID3 picture first, then MediaStore ContentResolver thumbnail (API 29+),
 * and finally scans local directory for folder/cover artwork files.
 */
internal object LocalAudioThumbnailDiskCache {
    private const val VERSION = "v1"
    private const val MAX_BYTES = 48L * 1024L * 1024L

    private fun dir(context: Context): File =
        File(context.filesDir, "thumbnails/audio").apply { mkdirs() }

    private fun legacyDir(context: Context): File =
        File(context.cacheDir, "local_audio_thumbnail_cache").apply { mkdirs() }

    private fun key(audioId: Long, uri: Uri, path: String, targetSizePx: Int): String {
        val source = if (path.isNotBlank()) {
            val file = File(path)
            if (file.exists()) "file:${file.absolutePath}|${file.length()}|${file.lastModified()}"
            else "path:$path"
        } else {
            "uri:$uri"
        }
        val raw = "$VERSION|$audioId|$source|$targetSizePx"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun read(context: Context, audioId: Long, uri: Uri, path: String, targetSizePx: Int): Bitmap? {
        val name = "${key(audioId, uri, path, targetSizePx)}.png"
        val file = File(dir(context), name)
        if (file.isFile && file.length() > 0L) {
            return runCatching { BitmapFactory.decodeFile(file.absolutePath)?.takeUnless { it.isRecycled } }.getOrNull()
        }
        // Migrate the old cache entry into persistent app storage before returning it.
        val legacy = File(legacyDir(context), name)
        if (!legacy.isFile || legacy.length() <= 0L) return null
        val bitmap = runCatching { BitmapFactory.decodeFile(legacy.absolutePath)?.takeUnless { it.isRecycled } }.getOrNull()
        if (bitmap != null) write(context, audioId, uri, path, targetSizePx, bitmap)
        return bitmap
    }

    fun write(context: Context, audioId: Long, uri: Uri, path: String, targetSizePx: Int, bitmap: Bitmap) {
        val folder = dir(context)
        val key = key(audioId, uri, path, targetSizePx)
        val target = File(folder, "$key.png")
        if (target.isFile && target.length() > 0L) return
        val temp = File(folder, "$key.tmp")
        runCatching {
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
        }.onFailure { runCatching { temp.delete() } }
    }
}
