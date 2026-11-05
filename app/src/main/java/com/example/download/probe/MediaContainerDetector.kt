package com.example.download

import java.io.File

/**
 * Finds the REAL container of a video from its first bytes (never from a guess): "mkv", "webm", "mp4", "mov", "m4v", "3gp",
 * "avi", "flv", "ts", "mpg" - or null when the bytes are not recognised.
 */
object ContainerSniffer {
    /** [head] = the first bytes of the file (at least 16, ideally 1 KB), [byteAt188] = the byte at offset 188 when known. */
    fun fromHead(head: ByteArray, byteAt188: Int? = null): String? {
        if (head.size < 12) return null
        fun at(i: Int) = head[i].toInt() and 0xFF
        fun text(from: Int, len: Int): String =
            if (from + len <= head.size) String(head, from, len, Charsets.ISO_8859_1) else ""
        // Matroska / WebM: EBML header 1A 45 DF A3, the DocType says which one
        if (at(0) == 0x1A && at(1) == 0x45 && at(2) == 0xDF && at(3) == 0xA3) {
            val probe = String(head, 0, minOf(head.size, 64), Charsets.ISO_8859_1)
            return if (probe.contains("webm")) "webm" else "mkv"
        }
        if (text(4, 4) == "ftyp") {
            val brand = text(8, 4)
            return when {
                brand == "qt  " -> "mov"
                brand == "M4V " || brand == "M4VH" || brand == "M4VP" -> "m4v"
                brand.startsWith("3g") -> "3gp"
                else -> "mp4"
            }
        }
        if (text(0, 4) == "RIFF" && text(8, 4) == "AVI ") return "avi"
        if (text(0, 3) == "FLV") return "flv"
        if (at(0) == 0x00 && at(1) == 0x00 && at(2) == 0x01 && at(3) == 0xBA) return "mpg"
        if (at(0) == 0x47 && (byteAt188 == null || byteAt188 == 0x47)) return "ts"
        return null
    }

    fun fromFile(file: File): String? = try {
        file.inputStream().use { input ->
            val head = ByteArray(1024)
            var n = 0
            while (n < head.size) {
                val r = input.read(head, n, head.size - n)
                if (r <= 0) break
                n += r
            }
            val b188 = if (file.length() > 188L && n > 0 && (head[0].toInt() and 0xFF) == 0x47) {
                java.io.RandomAccessFile(file, "r").use { it.seek(188); it.read() }
            } else null
            fromHead(head.copyOf(n), b188)
        }
    } catch (_: Throwable) {
        null
    }
}
