package com.example.util.archive

import android.media.MediaDataSource
import com.example.util.SeekableReader
import java.io.IOException
import java.io.InputStream

/**
 * One video INSIDE an archive as a random-access data source for Android's MediaMetadataRetriever / MediaExtractor and for
 * the container parsers. Nothing is extracted: only the blocks that are asked for are read from the archive (a file on the
 * phone, or HTTP Range requests for a link), and the last blocks are kept in memory.
 *
 * Stored (uncompressed) videos can be read at any position. Deflated ZIP videos can only be unpacked from the start, so a
 * far-away read is refused ([MAX_INFLATE_SKIP]) instead of downloading / unpacking the whole file for a thumbnail.
 */
class ArchiveEntryDataSource(
    private val source: ArchiveSource,
    private val kind: ArchiveKind,
    private val entry: ArchiveEntry,
    private val dataOffset: Long
) : MediaDataSource() {

    private val totalSize: Long = entry.size
    private val blocks = LinkedHashMap<Long, ByteArray>(16, 0.75f, true)
    private var lastFetchedIndex = -2L

    private var inflated: InputStream? = null
    private var inflatedPos = 0L

    @Synchronized
    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (size <= 0) return 0
        if (position < 0L || position >= totalSize) return -1
        var done = 0
        var pos = position
        while (done < size && pos < totalSize) {
            val index = pos / BLOCK
            val block = blockAt(index)
            val inBlock = (pos - index * BLOCK).toInt()
            if (inBlock >= block.size) break
            val n = minOf(size - done, block.size - inBlock)
            System.arraycopy(block, inBlock, buffer, offset + done, n)
            done += n
            pos += n
        }
        return if (done == 0) -1 else done
    }

    override fun getSize(): Long = totalSize

    /** Frees the memory and the open connection. The source can still be read afterwards (it simply starts again). */
    @Synchronized
    override fun close() {
        blocks.clear()
        lastFetchedIndex = -2L
        closeInflated()
    }

    private fun closeInflated() {
        try { inflated?.close() } catch (_: Throwable) {}
        inflated = null
        inflatedPos = 0L
    }

    private fun blockAt(index: Long): ByteArray {
        blocks[index]?.let { return it }
        val data = if (entry.method == 0) fetchStored(index) else fetchInflated(index)
        blocks[index] = data
        while (blocks.size > MAX_BLOCKS) {
            val oldest = blocks.keys.iterator()
            oldest.next()
            oldest.remove()
        }
        return data
    }

    private fun fetchStored(index: Long): ByteArray {
        val start = index * BLOCK
        val firstLen = minOf(BLOCK.toLong(), totalSize - start).toInt()
        // reading forward: take a few blocks with ONE request instead of one request per block
        val sequential = index == lastFetchedIndex + 1L
        val blocksLeft = ((totalSize - start + BLOCK - 1L) / BLOCK).toInt()
        val span = if (sequential) minOf(SEQUENTIAL_BLOCKS, blocksLeft) else 1
        val want = minOf(span.toLong() * BLOCK, totalSize - start).toInt()
        val bytes = ArchiveEngine.readFully(source, dataOffset + start, want)
        if (bytes.size < firstLen) throw IOException("The archive ended early")
        var first: ByteArray? = null
        for (i in 0 until span) {
            val from = i * BLOCK
            if (from >= bytes.size) break
            val part = bytes.copyOfRange(from, minOf(bytes.size, from + BLOCK))
            if (i == 0) first = part
            else if (!blocks.containsKey(index + i)) blocks[index + i] = part
        }
        lastFetchedIndex = index + span - 1L
        return first ?: throw IOException("The archive ended early")
    }

    private fun fetchInflated(index: Long): ByteArray {
        val start = index * BLOCK
        val len = minOf(BLOCK.toLong(), totalSize - start).toInt()
        var stream = inflated
        if (stream == null || inflatedPos > start) {
            closeInflated()
            stream = ArchiveEngine.openEntry(source, kind, entry, dataOffset, 0L)
            inflated = stream
            inflatedPos = 0L
        }
        if (start - inflatedPos > MAX_INFLATE_SKIP) {
            throw IOException("This part of a compressed video is too far to read without unpacking everything")
        }
        val sink = ByteArray(64 * 1024)
        while (inflatedPos < start) {
            val n = stream.read(sink, 0, minOf(sink.size.toLong(), start - inflatedPos).toInt())
            if (n < 0) throw IOException("The file inside the archive ended early")
            inflatedPos += n
        }
        val out = ByteArray(len)
        var got = 0
        while (got < len) {
            val n = stream.read(out, got, len - got)
            if (n < 0) break
            got += n
        }
        inflatedPos += got
        if (got < len) throw IOException("The file inside the archive ended early")
        return out
    }

    companion object {
        private const val BLOCK = 256 * 1024
        private const val SEQUENTIAL_BLOCKS = 4
        private const val MAX_BLOCKS = 14
        private const val MAX_INFLATE_SKIP = 64L * 1024L * 1024L
    }
}

/** [SeekableReader] on top of [ArchiveEntryDataSource]: the MKV / MP4 parsers can read a video inside an archive. */
class ArchiveSeekableReader(private val data: ArchiveEntryDataSource) : SeekableReader() {
    private val total: Long = data.getSize()
    private var pos = 0L
    private val buf = ByteArray(64 * 1024)
    private var bufStart = -1L
    private var bufLen = 0

    override val filePointer: Long
        get() = pos

    override fun length(): Long = total

    override fun seek(pos: Long) {
        this.pos = pos.coerceAtLeast(0L)
    }

    private fun inBuffer(at: Long): Boolean = bufStart >= 0L && at >= bufStart && at < bufStart + bufLen

    private fun fill(at: Long) {
        bufStart = at
        bufLen = try { data.readAt(at, buf, 0, buf.size).coerceAtLeast(0) } catch (e: IOException) { 0 }
    }

    override fun read(): Int {
        if (pos >= total) return -1
        if (!inBuffer(pos)) fill(pos)
        if (!inBuffer(pos)) return -1
        val b = buf[(pos - bufStart).toInt()].toInt() and 0xFF
        pos++
        return b
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len <= 0) return 0
        var done = 0
        while (done < len && pos < total) {
            if (!inBuffer(pos)) fill(pos)
            if (!inBuffer(pos)) break
            val avail = (bufStart + bufLen - pos).toInt()
            val n = minOf(len - done, avail)
            System.arraycopy(buf, (pos - bufStart).toInt(), b, off + done, n)
            pos += n
            done += n
        }
        return if (done == 0) -1 else done
    }

    override fun close() {
        data.close()
    }
}
