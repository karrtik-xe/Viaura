package com.example.download

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Copies a whole file into [out] with the kernel doing the work (FileChannel.transferTo = sendfile) instead of pushing every
 * byte through a Java buffer. The big copies at the end of a download (joining segments, writing the MKV, saving into
 * Movies/Viaura) are made with it, which is what makes "Finishing…" much shorter for a 1 GB video.
 */
object FastFileCopy {
    fun copy(src: File, out: FileOutputStream, onProgress: ((Long) -> Unit)? = null) {
        FileInputStream(src).use { fin ->
            val inCh = fin.channel
            val outCh = out.channel
            val size = inCh.size()
            var pos = 0L
            while (pos < size) {
                val n = try { inCh.transferTo(pos, minOf(size - pos, 32L shl 20), outCh) } catch (_: Throwable) { -1L }
                if (n <= 0L) break
                pos += n
                onProgress?.invoke(pos)
            }
            if (pos < size) {
                // transferTo is not possible for this target: plain big-buffer copy of the rest
                inCh.position(pos)
                val bb = ByteBuffer.allocateDirect(1 shl 20)
                while (true) {
                    bb.clear()
                    if (inCh.read(bb) < 0) break
                    bb.flip()
                    while (bb.hasRemaining()) outCh.write(bb)
                    pos += bb.limit()
                    onProgress?.invoke(pos)
                }
            }
        }
    }
}
