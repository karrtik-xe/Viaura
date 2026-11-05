package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import com.example.util.archive.ArchiveEngine
import com.example.util.archive.ArchiveException
import com.example.util.archive.ArchiveRecord
import com.example.util.archive.ArchiveSource
import com.example.util.archive.ArchiveStore
import com.example.util.archive.HttpArchiveSource
import com.example.util.archive.UriArchiveSource

/** Outcome of an import. The UI shows [Failure.message] as-is, so it is written for the user. */
sealed class PlaylistImportResult {
    data class Success(
        val playlist: CustomPlaylist,
        val itemCount: Int,
        /** Non-fatal remark, e.g. "stopped after 5000 videos". */
        val note: String? = null
    ) : PlaylistImportResult()

    data class Failure(val message: String) : PlaylistImportResult()
}
