package com.example.util

import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile

/**
 * Minimal random-access byte reader used by the MKV / MP4 container parsers. The member names are the same as the ones of
 * [RandomAccessFile] on purpose, so the parsers read a normal file and a video INSIDE a ZIP / TAR archive with the same code.
 */
abstract class SeekableReader : Closeable {
    abstract val filePointer: Long
    abstract fun length(): Long
    abstract fun seek(pos: Long)

    /** One byte (0..255) or -1 at the end. */
    abstract fun read(): Int

    /** Up to [len] bytes into [b]; -1 at the end. */
    abstract fun read(b: ByteArray, off: Int, len: Int): Int

    fun read(b: ByteArray): Int = read(b, 0, b.size)

    fun readFully(b: ByteArray) {
        var done = 0
        while (done < b.size) {
            val n = read(b, done, b.size - done)
            if (n < 0) throw EOFException()
            done += n
        }
    }

    fun skipBytes(n: Int): Int {
        if (n <= 0) return 0
        val from = filePointer
        val target = minOf(from + n.toLong(), length())
        if (target <= from) return 0
        seek(target)
        return (target - from).toInt()
    }

    fun readInt(): Int {
        val b = ByteArray(4)
        readFully(b)
        return ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or
            ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
    }

    fun readLong(): Long {
        val b = ByteArray(8)
        readFully(b)
        var v = 0L
        for (i in 0 until 8) v = (v shl 8) or (b[i].toLong() and 0xFFL)
        return v
    }

    fun readFloat(): Float = java.lang.Float.intBitsToFloat(readInt())

    fun readDouble(): Double = java.lang.Double.longBitsToDouble(readLong())
}

/** A file on the phone. */
class FileSeekableReader(file: File) : SeekableReader() {
    private val raf = RandomAccessFile(file, "r")

    override val filePointer: Long
        get() = raf.filePointer

    override fun length(): Long = raf.length()

    override fun seek(pos: Long) = raf.seek(pos)

    override fun read(): Int = raf.read()

    override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)

    override fun close() = raf.close()
}
