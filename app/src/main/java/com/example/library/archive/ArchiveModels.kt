package com.example.util.archive

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Random-access reading of ZIP / TAR archives WITHOUT copying or extracting them. The archive stays where it is (a file on the
 * phone, or a link on a website) and only the byte ranges that the player asks for are read.
 */

/** One file inside an archive. For ZIP [dataOffset] is -1 until the local header has been read. */
data class ArchiveEntry(
    val name: String,
    val size: Long,
    val compressedSize: Long,
    /** 0 = stored (raw bytes, instantly seekable), 8 = deflate (decompressed while streaming). */
    val method: Int,
    val headerOffset: Long,
    val dataOffset: Long,
    val durationMs: Long = 0L,
    /** When the file inside the archive was last changed (milliseconds), 0 when the archive does not tell. */
    val modifiedMs: Long = 0L,
    /** Filled by the background probe (see [ArchiveProbe]); empty / 0 until then. */
    val resolution: String = "",
    val framerate: Double = 0.0,
    val subtitleFormats: List<String> = emptyList(),
    /** True once the probe has looked at this video (even when it could not read everything), so it is not probed again and again. */
    val metaProbed: Boolean = false
)

enum class ArchiveKind { ZIP, TAR }

class ArchiveScan(
    val kind: ArchiveKind,
    val entries: List<ArchiveEntry>,
    /** Files that cannot be played: password protected or packed with a method that is not supported. */
    val lockedCount: Int,
    val unsupportedCount: Int
)

class ArchiveException(message: String) : IOException(message)

interface ArchiveSource {
    /** Total size of the archive in bytes. */
    fun length(): Long

    /** The [length] bytes that start at [offset]. The caller closes the stream. */
    fun open(offset: Long, length: Long): InputStream
}
