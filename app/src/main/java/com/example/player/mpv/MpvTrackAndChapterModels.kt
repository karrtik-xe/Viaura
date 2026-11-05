package com.example.player

import com.example.ui.components.StyledIcon

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.example.ui.state.HwAccelMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.screens.SubtitleCue
import com.example.ui.screens.AssParser
import com.example.ui.screens.getPlayableFilePath
import com.example.util.AppStorageManager
import com.example.util.CustomSubtitlePersistenceManager
import com.example.util.SubtitleSessionMemory
import com.example.util.MkvManagerEngine
import com.example.util.NetworkBypass
import com.example.util.StreamRequestOptions
import com.example.ai.AiFeaturesSettings
import com.example.ai.SubtitleTranslationService
import com.example.ai.TranslateLanguages
import android.os.ParcelFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class PlayerMediaTrack(
    val id: Int,
    val type: String, // "audio" or "sub"
    val title: String,
    val language: String,
    val codec: String,
    val isSelected: Boolean,
    val isExternal: Boolean = false,
    val originalFilename: String = "",
    val isSecondarySelected: Boolean = false,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isHearingImpaired: Boolean = false,
    val isVisualImpaired: Boolean = false,
    val isAudioCommentary: Boolean = false
)

/** One audio stream (original / dubbed / auto-translated) of an online video, attached as an external mpv audio track. */
data class OnlineAudioSource(val url: String, val title: String, val language: String)

/** Seekable part of a live stream (DVR / time-shift window) in the same timeline as the playback position. */
data class LiveSeekRange(val startMs: Long, val endMs: Long)

data class PlayerVideoChapter(
    val index: Int,
    val title: String,
    val timeMs: Long,
    val formattedTime: String
)
