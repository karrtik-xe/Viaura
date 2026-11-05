package com.example.ui.components

import com.example.ui.theme.Neutral

import com.example.R

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerMediaTrack
import com.example.player.SubtitleFontManager
import com.example.ui.state.PlayerSettings
import com.example.ui.theme.AccentGradient
import com.example.ui.theme.AccentSkyBlue
import com.example.ui.theme.AccentPink
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Whether [track] is a genuinely native ASS/SSA subtitle, as opposed to another text format
 * (SRT/VTT/SAMI/MicroDVD/...) that gets converted to an .ass file purely so libass can render
 * it. "Override ASS/SSA Styles" and "Advanced ASS/SSA" only make sense for a real ASS/SSA
 * track that already carries its own embedded styling to override or edit — showing them for
 * every subtitle format was confusing (and, worse, made the override toggle look like it was
 * required for plain-text subtitles to pick up any styling at all). For external tracks the
 * original file extension (still known even after conversion) is authoritative; for embedded
 * tracks we fall back to the container-reported codec.
 */
internal fun isNativeAssSsaTrack(track: PlayerMediaTrack?): Boolean {
    if (track == null) return false
    val ext = track.originalFilename.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
    if (ext.isNotBlank()) return ext == "ass" || ext == "ssa"
    val codec = track.codec.lowercase(java.util.Locale.ROOT)
    return codec == "ass" || codec == "ssa"
}

/**
 * Whether [track] is an image/bitmap-based subtitle (PGS/SUP, VobSub, DVB, CEA-608/708,
 * ARIB B24, BDN, ...). These carry no editable text at all, so any "edit this subtitle" affordance
 * should be hidden for them rather than opening an editor with nothing it can do.
 */
fun isImageBasedSubtitleTrack(track: PlayerMediaTrack?): Boolean {
    if (track == null) return false
    val codec = track.codec.lowercase(java.util.Locale.ROOT)
    return codec in setOf(
        "hdmv_pgs_subtitle", "pgssub", "dvd_subtitle", "vobsub", "dvb_subtitle", "dvbsub",
        "xsub", "eia_608", "cea608", "eia_708", "cea708", "arib_caption"
    )
}
