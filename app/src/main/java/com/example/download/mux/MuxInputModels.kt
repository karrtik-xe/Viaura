package com.example.download

import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

class UnsupportedMuxException(message: String) : Exception(message)

data class MuxSource(val file: File, val role: String, val name: String, val language: String, val isDefault: Boolean)

data class MuxSubtitle(val file: File, val language: String, val name: String, val ext: String, val isAuto: Boolean)
