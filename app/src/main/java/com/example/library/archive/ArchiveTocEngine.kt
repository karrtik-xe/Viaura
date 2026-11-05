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

object ArchiveEngine {
    private const val MAX_CENTRAL_DIRECTORY = 96L * 1024L * 1024L
    private const val MAX_TAR_HEADERS = 20000

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m4v", "flv", "wmv", "ts", "m2ts", "mts", "mpg", "mpeg", "3gp", "ogv", "vob", "divx", "asf", "rmvb"
    )

    fun isVideoName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    fun mimeTypeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "flv" -> "video/x-flv"
        "wmv", "asf" -> "video/x-ms-wmv"
        "ts", "m2ts", "mts" -> "video/mp2t"
        "mpg", "mpeg", "vob" -> "video/mpeg"
        "3gp" -> "video/3gpp"
        "ogv" -> "video/ogg"
        else -> "application/octet-stream"
    }

    /** Reads the table of contents of the archive. Only small parts of the archive are read. */
    fun scan(source: ArchiveSource, nameHint: String): ArchiveScan {
        val total = source.length()
        if (total < 22L) throw ArchiveException("This file is too small to be an archive")
        val head = readFully(source, 0L, minOf(total, 512L).toInt())
        return when {
            isZipMagic(head) -> scanZip(source, total)
            isTar(head, nameHint) -> scanTar(source, total)
            head.size >= 6 && head[0] == '7'.code.toByte() && head[1] == 'z'.code.toByte() && head[2] == 0xBC.toByte() ->
                throw ArchiveException("7z archives cannot be played in place (their data is packed as one block). Use a ZIP or TAR archive.")
            head.size >= 4 && head[0] == 'R'.code.toByte() && head[1] == 'a'.code.toByte() && head[2] == 'r'.code.toByte() ->
                throw ArchiveException("RAR archives are not supported. Use a ZIP or TAR archive.")
            head.size >= 2 && head[0] == 0x1F.toByte() && head[1] == 0x8B.toByte() ->
                throw ArchiveException("Compressed .tar.gz archives cannot be played in place. Use a ZIP or a plain TAR archive.")
            else -> throw ArchiveException("This is not a ZIP or TAR archive")
        }
    }

    private fun isZipMagic(h: ByteArray): Boolean =
        h.size >= 4 && h[0] == 0x50.toByte() && h[1] == 0x4B.toByte() &&
            ((h[2] == 3.toByte() && h[3] == 4.toByte()) || (h[2] == 5.toByte() && h[3] == 6.toByte()) || (h[2] == 7.toByte() && h[3] == 8.toByte()))

    private fun isTar(h: ByteArray, nameHint: String): Boolean {
        if (h.size >= 262 && String(h, 257, 5, Charsets.ISO_8859_1) == "ustar") return true
        // old V7 tar has no magic: trust the file name together with a valid header checksum
        val lower = nameHint.lowercase()
        return h.size >= 512 && lower.substringBefore('?').endsWith(".tar") && tarChecksumOk(h)
    }

    // ------------------------------------------------------------------------------------------ ZIP

    private fun scanZip(source: ArchiveSource, total: Long): ArchiveScan {
        val tailSize = minOf(total, 65_535L + 22L + 20L + 64L).toInt()
        val tailStart = total - tailSize
        val tail = readFully(source, tailStart, tailSize)

        var eocd = -1
        var i = tail.size - 22
        while (i >= 0) {
            if (tail[i] == 0x50.toByte() && tail[i + 1] == 0x4B.toByte() && tail[i + 2] == 5.toByte() && tail[i + 3] == 6.toByte()) {
                eocd = i
                break
            }
            i--
        }
        if (eocd < 0) throw ArchiveException("The end of this ZIP file is missing (incomplete or broken file)")

        var entryCount = u16(tail, eocd + 10).toLong()
        var cdSize = u32(tail, eocd + 12)
        var cdOffset = u32(tail, eocd + 16)

        if (entryCount == 0xFFFFL || cdSize == 0xFFFFFFFFL || cdOffset == 0xFFFFFFFFL) {
            // ZIP64: the real numbers live in the ZIP64 end record
            val locator = eocd - 20
            if (locator >= 0 && u32(tail, locator) == 0x07064b50L) {
                val zip64Offset = u64(tail, locator + 8)
                val rec = readFully(source, zip64Offset, 56)
                if (u32(rec, 0) == 0x06064b50L) {
                    entryCount = u64(rec, 32)
                    cdSize = u64(rec, 40)
                    cdOffset = u64(rec, 48)
                }
            }
        }
        if (cdSize <= 0L || cdSize > MAX_CENTRAL_DIRECTORY || cdOffset < 0L || cdOffset + cdSize > total) {
            throw ArchiveException("The file list of this ZIP is damaged or too large")
        }
        val cd = readFully(source, cdOffset, cdSize.toInt())

        val entries = ArrayList<ArchiveEntry>()
        var locked = 0
        var unsupported = 0
        var p = 0
        while (p + 46 <= cd.size && u32(cd, p) == 0x02014b50L) {
            val flags = u16(cd, p + 8)
            val method = u16(cd, p + 10)
            val modifiedMs = dosToMillis(u16(cd, p + 14), u16(cd, p + 12))
            var compSize = u32(cd, p + 20)
            var size = u32(cd, p + 24)
            val nameLen = u16(cd, p + 28)
            val extraLen = u16(cd, p + 30)
            val commentLen = u16(cd, p + 32)
            var localOffset = u32(cd, p + 42)
            if (p + 46 + nameLen + extraLen > cd.size) break

            if (size == 0xFFFFFFFFL || compSize == 0xFFFFFFFFL || localOffset == 0xFFFFFFFFL) {
                var e = p + 46 + nameLen
                val extraEnd = e + extraLen
                while (e + 4 <= extraEnd) {
                    val id = u16(cd, e)
                    val len = u16(cd, e + 2)
                    if (id == 1) {
                        var q = e + 4
                        if (size == 0xFFFFFFFFL && q + 8 <= extraEnd) { size = u64(cd, q); q += 8 }
                        if (compSize == 0xFFFFFFFFL && q + 8 <= extraEnd) { compSize = u64(cd, q); q += 8 }
                        if (localOffset == 0xFFFFFFFFL && q + 8 <= extraEnd) { localOffset = u64(cd, q) }
                        break
                    }
                    e += 4 + len
                }
            }

            val utf8 = (flags and 0x0800) != 0
            val name = decodeName(cd, p + 46, nameLen, utf8)
            p += 46 + nameLen + extraLen + commentLen

            if (name.isEmpty() || name.endsWith("/")) continue
            if ((flags and 0x0001) != 0) { locked++; continue }
            if (method != 0 && method != 8) { unsupported++; continue }
            entries.add(ArchiveEntry(name, size, compSize, method, localOffset, -1L, modifiedMs = modifiedMs))
        }
        return ArchiveScan(ArchiveKind.ZIP, entries, locked, unsupported)
    }

    /** ZIP stores the change time as an MS-DOS date + time (local time, 2 s steps). */
    private fun dosToMillis(dosDate: Int, dosTime: Int): Long {
        if (dosDate == 0) return 0L
        return try {
            val year = ((dosDate shr 9) and 0x7F) + 1980
            val month = ((dosDate shr 5) and 0x0F).coerceIn(1, 12)
            val day = (dosDate and 0x1F).coerceIn(1, 31)
            val hour = ((dosTime shr 11) and 0x1F).coerceAtMost(23)
            val minute = ((dosTime shr 5) and 0x3F).coerceAtMost(59)
            val second = ((dosTime and 0x1F) * 2).coerceAtMost(59)
            val cal = java.util.Calendar.getInstance()
            cal.clear()
            cal.set(year, month - 1, day, hour, minute, second)
            cal.timeInMillis
        } catch (_: Throwable) {
            0L
        }
    }

    /** Where the file data of a ZIP entry starts (after its local header). */
    fun resolveZipDataOffset(source: ArchiveSource, entry: ArchiveEntry): Long {
        if (entry.dataOffset >= 0L) return entry.dataOffset
        val h = readFully(source, entry.headerOffset, 30)
        if (u32(h, 0) != 0x04034b50L) throw ArchiveException("A file inside the ZIP is damaged")
        return entry.headerOffset + 30L + u16(h, 26) + u16(h, 28)
    }

    private fun decodeName(buf: ByteArray, off: Int, len: Int, utf8: Boolean): String {
        if (utf8) return String(buf, off, len, Charsets.UTF_8)
        // names without the UTF-8 flag: modern tools still write UTF-8; only old ones use the DOS code page
        val asUtf8 = String(buf, off, len, Charsets.UTF_8)
        if (!asUtf8.contains('\uFFFD')) return asUtf8
        return try { String(buf, off, len, Charset.forName("IBM437")) } catch (_: Throwable) { String(buf, off, len, Charsets.ISO_8859_1) }
    }

    // ------------------------------------------------------------------------------------------ TAR

    private fun scanTar(source: ArchiveSource, total: Long): ArchiveScan {
        val entries = ArrayList<ArchiveEntry>()
        var pos = 0L
        var longName: String? = null
        var paxPath: String? = null
        var paxSize = -1L
        var headers = 0
        while (pos + 512L <= total && headers < MAX_TAR_HEADERS) {
            val h = readFully(source, pos, 512)
            if (h.all { it == 0.toByte() }) break
            if (!tarChecksumOk(h)) throw ArchiveException("This TAR file is damaged")
            headers++
            var size = tarNumber(h, 124, 12)
            val type = h[156].toInt().toChar()
            var name = tarString(h, 0, 100)
            if (String(h, 257, 5, Charsets.ISO_8859_1) == "ustar") {
                val prefix = tarString(h, 345, 155)
                if (prefix.isNotEmpty()) name = "$prefix/$name"
            }
            val dataStart = pos + 512L
            when (type) {
                'L' -> {
                    longName = String(readFully(source, dataStart, size.coerceIn(0L, 8192L).toInt()), Charsets.UTF_8).trimEnd('\u0000', '\n')
                }
                'x' -> {
                    val pax = String(readFully(source, dataStart, size.coerceIn(0L, 65536L).toInt()), Charsets.UTF_8)
                    for (line in pax.split('\n')) {
                        val kv = line.substringAfter(' ', "")
                        if (kv.startsWith("path=")) paxPath = kv.removePrefix("path=")
                        else if (kv.startsWith("size=")) paxSize = kv.removePrefix("size=").trim().toLongOrNull() ?: -1L
                    }
                }
                'g' -> {}
                '0', '\u0000', '7' -> {
                    if (paxSize >= 0L) size = paxSize
                    val finalName = (longName ?: paxPath ?: name)
                    if (finalName.isNotEmpty() && !finalName.endsWith("/")) {
                        entries.add(ArchiveEntry(finalName, size, size, 0, pos, dataStart, modifiedMs = tarNumber(h, 136, 12) * 1000L))
                    }
                    longName = null
                    paxPath = null
                    paxSize = -1L
                }
                else -> { longName = null; paxPath = null; paxSize = -1L }
            }
            pos = dataStart + ((size + 511L) / 512L) * 512L
        }
        return ArchiveScan(ArchiveKind.TAR, entries, 0, 0)
    }

    private fun tarString(h: ByteArray, off: Int, len: Int): String {
        var end = off
        while (end < off + len && h[end] != 0.toByte()) end++
        return String(h, off, end - off, Charsets.UTF_8)
    }

    private fun tarNumber(h: ByteArray, off: Int, len: Int): Long {
        if ((h[off].toInt() and 0x80) != 0) {
            // base-256 (used for files over 8 GB)
            var v = (h[off].toInt() and 0x7F).toLong()
            for (k in 1 until len) v = (v shl 8) or (h[off + k].toLong() and 0xFF)
            return v
        }
        var v = 0L
        for (k in off until off + len) {
            val c = h[k].toInt()
            if (c == 0 || c == ' '.code) { if (v != 0L) break else continue }
            if (c < '0'.code || c > '7'.code) break
            v = v * 8 + (c - '0'.code)
        }
        return v
    }

    private fun tarChecksumOk(h: ByteArray): Boolean {
        if (h.size < 512) return false
        val stored = tarNumber(h, 148, 8)
        var sum = 0L
        for (k in 0 until 512) sum += if (k in 148..155) 32 else (h[k].toInt() and 0xFF)
        return sum == stored
    }

    // ------------------------------------------------------------------------------------------ reading entries

    /**
     * The bytes of [entry] from [startAt] (position inside the finished file) to its end.
     * Stored files and TAR files are read directly; deflated ZIP files are unpacked on the fly.
     */
    fun openEntry(source: ArchiveSource, kind: ArchiveKind, entry: ArchiveEntry, dataOffset: Long, startAt: Long): InputStream {
        if (startAt >= entry.size) return LimitedInputStream(java.io.ByteArrayInputStream(ByteArray(0)), 0L)
        if (entry.method == 0) {
            return source.open(dataOffset + startAt, entry.size - startAt)
        }
        val raw = source.open(dataOffset, entry.compressedSize)
        val inflater = Inflater(true)
        val unpacked = object : InflaterInputStream(raw, inflater, 64 * 1024) {
            override fun close() {
                try { super.close() } finally { inflater.end() }
            }
        }
        // a deflated stream cannot jump: the part before the wanted position is unpacked and thrown away
        var toSkip = startAt
        val sink = ByteArray(64 * 1024)
        while (toSkip > 0L) {
            val n = unpacked.read(sink, 0, minOf(sink.size.toLong(), toSkip).toInt())
            if (n < 0) {
                unpacked.close()
                throw ArchiveException("The file inside the ZIP ended early")
            }
            toSkip -= n
        }
        return LimitedInputStream(unpacked, entry.size - startAt)
    }

    // ------------------------------------------------------------------------------------------ helpers

    fun readFully(source: ArchiveSource, offset: Long, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val out = ByteArrayOutputStream(length)
        source.open(offset, length.toLong()).use { input ->
            val buf = ByteArray(minOf(length, 64 * 1024))
            var left = length
            while (left > 0) {
                val n = input.read(buf, 0, minOf(buf.size, left))
                if (n < 0) break
                out.write(buf, 0, n)
                left -= n
            }
        }
        return out.toByteArray()
    }

    private fun u16(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun u32(b: ByteArray, o: Int): Long =
        (b[o].toLong() and 0xFF) or ((b[o + 1].toLong() and 0xFF) shl 8) or
            ((b[o + 2].toLong() and 0xFF) shl 16) or ((b[o + 3].toLong() and 0xFF) shl 24)

    private fun u64(b: ByteArray, o: Int): Long = u32(b, o) or (u32(b, o + 4) shl 32)
}
