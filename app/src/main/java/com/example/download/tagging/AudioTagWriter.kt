package com.example.download

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.Locale

/** Writes title / artist / lyrics / cover into M4A, MP3 and native FLAC metadata without re-encoding audio. */
object AudioTagger {
    private fun be32(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
    private fun box(type: ByteArray, payload: ByteArray): ByteArray = be32(payload.size + 8) + type + payload
    private fun box(type: String, payload: ByteArray): ByteArray = box(type.toByteArray(Charsets.ISO_8859_1), payload)
    private val COPY = byteArrayOf(0xA9.toByte())

    private fun dataBox(flags: Int, payload: ByteArray): ByteArray = box("data", be32(flags) + be32(0) + payload)

    fun tagM4a(file: File, title: String, artist: String, lyrics: String, cover: ByteArray?): Boolean {
        return try {
            val raf = RandomAccessFile(file, "r")
            val len = raf.length()
            // top-level boxes
            class Top(val type: String, val start: Long, val size: Long)
            val tops = ArrayList<Top>()
            var pos = 0L
            while (pos + 8 <= len) {
                raf.seek(pos)
                val sz = raf.readInt().toLong() and 0xFFFFFFFFL
                val tb = ByteArray(4)
                raf.readFully(tb)
                var size = sz
                if (sz == 1L) size = raf.readLong()
                if (sz == 0L) size = len - pos
                if (size < 8 || pos + size > len) break
                tops.add(Top(String(tb, Charsets.ISO_8859_1), pos, size))
                pos += size
            }
            val moov = tops.firstOrNull { it.type == "moov" }
            if (moov == null) { raf.close(); return false }
            val fragmented = tops.any { it.type == "moof" }
            val mdat = tops.firstOrNull { it.type == "mdat" }
            val moovBytes = ByteArray(moov.size.toInt())
            raf.seek(moov.start)
            raf.readFully(moovBytes)

            // children of moov without an existing udta
            val kept = ByteArrayOutputStream()
            var p = 8
            while (p + 8 <= moovBytes.size) {
                val sz = ByteBuffer.wrap(moovBytes, p, 4).int
                val type = String(moovBytes, p + 4, 4, Charsets.ISO_8859_1)
                if (sz < 8 || p + sz > moovBytes.size) break
                if (type != "udta") kept.write(moovBytes, p, sz)
                p += sz
            }
            val items = ByteArrayOutputStream()
            fun text(tag: ByteArray, v: String) {
                if (v.isNotBlank()) items.write(box(tag, dataBox(1, v.toByteArray(Charsets.UTF_8))))
            }
            text(COPY + "nam".toByteArray(), title)
            text(COPY + "ART".toByteArray(), artist)
            text(COPY + "lyr".toByteArray(), lyrics)
            if (cover != null && cover.isNotEmpty()) items.write(box("covr", dataBox(13, cover)))
            if (items.size() == 0) { raf.close(); return false }
            val hdlr = box("hdlr", be32(0) + be32(0) + "mdir".toByteArray() + ByteArray(12) + byteArrayOf(0))
            val meta = box("meta", be32(0) + hdlr + box("ilst", items.toByteArray()))
            val udta = box("udta", meta)
            var newMoov = box("moov", kept.toByteArray() + udta)
            val delta = newMoov.size.toLong() - moov.size
            // a moov in front of the media data moves it: the chunk offsets must follow
            if (!fragmented && mdat != null && mdat.start > moov.start && delta != 0L) {
                newMoov = patchChunkOffsets(newMoov, delta)
            }
            val tmp = File(file.path + ".tag")
            tmp.outputStream().buffered(1 shl 20).use { o ->
                raf.seek(0)
                copyRange(raf, o, moov.start)
                o.write(newMoov)
                raf.seek(moov.start + moov.size)
                copyRange(raf, o, len - (moov.start + moov.size))
            }
            raf.close()
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.copyTo(file, true)
                tmp.delete()
            }
            true
        } catch (_: Throwable) {
            File(file.path + ".tag").delete()
            false
        }
    }


    /** Adds native FLAC Vorbis-comment tags and a front-cover picture without re-encoding the audio frames. */
    fun tagFlac(file: File, title: String, artist: String, lyrics: String, cover: ByteArray?): Boolean {
        return try {
            data class MetadataBlock(val type: Int, val payload: ByteArray)
            val raf = RandomAccessFile(file, "r")
            if (raf.length() < 8) { raf.close(); return false }
            val signature = ByteArray(4)
            raf.readFully(signature)
            if (!signature.contentEquals(byteArrayOf('f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()))) {
                raf.close(); return false
            }

            val blocks = ArrayList<MetadataBlock>()
            var foundLast = false
            while (!foundLast && raf.filePointer + 4 <= raf.length()) {
                val header = ByteArray(4)
                raf.readFully(header)
                foundLast = (header[0].toInt() and 0x80) != 0
                val type = header[0].toInt() and 0x7F
                val length = ((header[1].toInt() and 0xFF) shl 16) or
                    ((header[2].toInt() and 0xFF) shl 8) or (header[3].toInt() and 0xFF)
                if (length < 0 || length > 16 * 1024 * 1024 || raf.filePointer + length > raf.length()) {
                    raf.close(); return false
                }
                val payload = ByteArray(length)
                raf.readFully(payload)
                blocks.add(MetadataBlock(type, payload))
            }
            if (blocks.isEmpty() || blocks.first().type != 0 || !foundLast) { raf.close(); return false }
            val audioOffset = raf.filePointer

            fun le32(value: Int): ByteArray = byteArrayOf(
                value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte()
            )
            fun readLe32(data: ByteArray, start: Int): Int {
                if (start < 0 || start + 4 > data.size) throw IllegalArgumentException("Invalid FLAC comment block")
                return (data[start].toInt() and 0xFF) or
                    ((data[start + 1].toInt() and 0xFF) shl 8) or
                    ((data[start + 2].toInt() and 0xFF) shl 16) or
                    ((data[start + 3].toInt() and 0xFF) shl 24)
            }
            fun parseComments(payload: ByteArray): MutableList<String> {
                val result = mutableListOf<String>()
                var pos = 0
                if (payload.size < 8) return result
                val vendorLen = readLe32(payload, pos).coerceAtLeast(0)
                pos += 4
                if (vendorLen > payload.size - pos) return result
                pos += vendorLen
                if (pos + 4 > payload.size) return result
                val count = readLe32(payload, pos).coerceIn(0, 100_000)
                pos += 4
                repeat(count) {
                    if (pos + 4 > payload.size) return@repeat
                    val size = readLe32(payload, pos).coerceAtLeast(0)
                    pos += 4
                    if (size > payload.size - pos) return@repeat
                    result.add(String(payload, pos, size, Charsets.UTF_8))
                    pos += size
                }
                return result
            }
            val oldComments = blocks.firstOrNull { it.type == 4 }?.let { parseComments(it.payload) }.orEmpty()
            val comments = oldComments.filterNot {
                val key = it.substringBefore('=').uppercase(Locale.ROOT)
                key in setOf("TITLE", "ARTIST", "LYRICS", "UNSYNCEDLYRICS")
            }.toMutableList()
            if (title.isNotBlank()) comments.add("TITLE=$title")
            if (artist.isNotBlank()) comments.add("ARTIST=$artist")
            if (lyrics.isNotBlank()) {
                comments.add("LYRICS=$lyrics")
                comments.add("UNSYNCEDLYRICS=$lyrics")
            }
            val vendor = "Viaura".toByteArray(Charsets.UTF_8)
            val commentBytes = ByteArrayOutputStream().apply {
                write(le32(vendor.size)); write(vendor); write(le32(comments.size))
                comments.forEach { value ->
                    val bytes = value.toByteArray(Charsets.UTF_8)
                    write(le32(bytes.size)); write(bytes)
                }
            }.toByteArray()

            val updatedBlocks = blocks.filterNot { it.type == 4 || (cover != null && it.type == 6) }.toMutableList()
            updatedBlocks.add(MetadataBlock(4, commentBytes))
            if (cover != null && cover.isNotEmpty()) {
                val mime = "image/jpeg".toByteArray(Charsets.UTF_8)
                val pictureBytes = ByteArrayOutputStream().apply {
                    write(be32(3)) // front cover
                    write(be32(mime.size)); write(mime)
                    write(be32(0)) // empty description
                    write(be32(0)); write(be32(0)); write(be32(0)); write(be32(0)) // unknown dimensions / depth / palette
                    write(be32(cover.size)); write(cover)
                }.toByteArray()
                updatedBlocks.add(MetadataBlock(6, pictureBytes))
            }

            val temp = File(file.path + ".tag")
            temp.outputStream().buffered(1 shl 18).use { out ->
                out.write(signature)
                updatedBlocks.forEachIndexed { index, block ->
                    val isLast = index == updatedBlocks.lastIndex
                    out.write((block.type and 0x7F) or if (isLast) 0x80 else 0)
                    val n = block.payload.size
                    out.write((n ushr 16) and 0xFF)
                    out.write((n ushr 8) and 0xFF)
                    out.write(n and 0xFF)
                    out.write(block.payload)
                }
                raf.seek(audioOffset)
                copyRange(raf, out, raf.length() - audioOffset)
            }
            raf.close()
            if (!temp.renameTo(file)) {
                file.delete()
                temp.copyTo(file, true)
                temp.delete()
            }
            true
        } catch (_: Throwable) {
            File(file.path + ".tag").delete()
            false
        }
    }

    private fun copyRange(raf: RandomAccessFile, o: java.io.OutputStream, count: Long) {
        var left = count
        val buf = ByteArray(1 shl 18)
        while (left > 0) {
            val n = raf.read(buf, 0, minOf(left, buf.size.toLong()).toInt())
            if (n <= 0) break
            o.write(buf, 0, n)
            left -= n
        }
    }

    /** Adds [delta] to every entry of every stco / co64 box inside [moov]. */
    private fun patchChunkOffsets(moov: ByteArray, delta: Long): ByteArray {
        val out = moov.copyOf()
        fun walk(start: Int, end: Int) {
            var p = start
            while (p + 8 <= end) {
                val sz = ByteBuffer.wrap(out, p, 4).int
                if (sz < 8 || p + sz > end) return
                val type = String(out, p + 4, 4, Charsets.ISO_8859_1)
                when (type) {
                    "trak", "mdia", "minf", "stbl", "moov" -> walk(p + 8, p + sz)
                    "stco" -> {
                        val count = ByteBuffer.wrap(out, p + 12, 4).int
                        for (i in 0 until count) {
                            val at = p + 16 + i * 4
                            val v = (ByteBuffer.wrap(out, at, 4).int.toLong() and 0xFFFFFFFFL) + delta
                            ByteBuffer.wrap(out, at, 4).putInt(v.toInt())
                        }
                    }
                    "co64" -> {
                        val count = ByteBuffer.wrap(out, p + 12, 4).int
                        for (i in 0 until count) {
                            val at = p + 16 + i * 8
                            val v = ByteBuffer.wrap(out, at, 8).long + delta
                            ByteBuffer.wrap(out, at, 8).putLong(v)
                        }
                    }
                }
                p += sz
            }
        }
        walk(8, out.size)
        return out
    }

    private fun syncSafe(v: Int) = byteArrayOf(((v shr 21) and 0x7F).toByte(), ((v shr 14) and 0x7F).toByte(), ((v shr 7) and 0x7F).toByte(), (v and 0x7F).toByte())

    private fun utf16(text: String): ByteArray = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)

    private fun frame(id: String, body: ByteArray): ByteArray = id.toByteArray() + be32(body.size) + byteArrayOf(0, 0) + body

    fun tagMp3(file: File, title: String, artist: String, lyrics: String, cover: ByteArray?): Boolean {
        return try {
            val frames = ByteArrayOutputStream()
            if (title.isNotBlank()) frames.write(frame("TIT2", byteArrayOf(1) + utf16(title)))
            if (artist.isNotBlank()) frames.write(frame("TPE1", byteArrayOf(1) + utf16(artist)))
            if (lyrics.isNotBlank()) frames.write(frame("USLT", byteArrayOf(1) + "eng".toByteArray() + utf16("") + byteArrayOf(0, 0) + utf16(lyrics)))
            if (cover != null && cover.isNotEmpty()) {
                frames.write(frame("APIC", byteArrayOf(0) + "image/jpeg".toByteArray() + byteArrayOf(0, 3, 0) + cover))
            }
            if (frames.size() == 0) return false
            val tagBody = frames.toByteArray()
            val header = "ID3".toByteArray() + byteArrayOf(3, 0, 0) + syncSafe(tagBody.size)
            val tmp = File(file.path + ".tag")
            val raf = RandomAccessFile(file, "r")
            var skip = 0L
            val h = ByteArray(10)
            if (raf.length() > 10) {
                raf.readFully(h)
                if (h[0] == 'I'.code.toByte() && h[1] == 'D'.code.toByte() && h[2] == '3'.code.toByte()) {
                    skip = 10L + (((h[6].toInt() and 0x7F) shl 21) or ((h[7].toInt() and 0x7F) shl 14) or ((h[8].toInt() and 0x7F) shl 7) or (h[9].toInt() and 0x7F))
                }
            }
            tmp.outputStream().buffered(1 shl 20).use { o ->
                o.write(header); o.write(tagBody)
                raf.seek(skip)
                copyRange(raf, o, raf.length() - skip)
            }
            raf.close()
            if (!tmp.renameTo(file)) { file.delete(); tmp.copyTo(file, true); tmp.delete() }
            true
        } catch (_: Throwable) {
            File(file.path + ".tag").delete()
            false
        }
    }
}
