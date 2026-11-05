package com.example.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.ui.screens.AudioFolderItem
import com.example.ui.screens.AudioTrackItem
import com.example.ui.screens.VideoFolder
import com.example.ui.screens.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PrivateMediaType {
    VIDEO,
    AUDIO
}

data class PrivateFolder(
    val id: String,
    val name: String,
    val mediaType: PrivateMediaType,
    val vaultRelativePath: String,
    val itemCount: Int,
    val totalSizeBytes: Long,
    val createdDate: Long,
    val originalPath: String = ""
) {
    val formattedSize: String
        get() = FolderDateUtils.formatFileSize(totalSizeBytes)

    val formattedDate: String
        get() = FolderDateUtils.formatCreatedDate(createdDate)
}
