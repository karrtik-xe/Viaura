package com.example.player

import android.content.Context
import com.example.ui.state.AppLanguage
import java.io.File
import java.util.Locale

/**
 * Audio channel configuration modes for MPV player and audio output.
 */
enum class AudioChannelMode(
    val id: String,
    val mpvValue: String,
    val defaultTitle: String,
    val defaultDescription: String
) {
    AUTO(
        id = "auto",
        mpvValue = "auto",
        defaultTitle = "Auto",
        defaultDescription = "Automatic audio channels configuration"
    ),
    AUTO_SAFE(
        id = "auto-safe",
        mpvValue = "auto-safe",
        defaultTitle = "Auto Safe",
        defaultDescription = "Default multi-channel passthrough with safe downmix fallback"
    ),
    MONO(
        id = "mono",
        mpvValue = "mono",
        defaultTitle = "Mono",
        defaultDescription = "Combines left and right channels centered across all speakers"
    ),
    STEREO(
        id = "stereo",
        mpvValue = "stereo",
        defaultTitle = "Stereo",
        defaultDescription = "Standard Left and Right 2-channel stereo soundstage"
    ),
    REVERSED_STEREO(
        id = "reversed-stereo",
        mpvValue = "reversed-stereo",
        defaultTitle = "Reversed Stereo",
        defaultDescription = "Swaps Left and Right audio channels"
    ),
    LEFT(
        id = "left",
        mpvValue = "left",
        defaultTitle = "Left Channel Only",
        defaultDescription = "Plays only the left channel sound through both speakers"
    ),
    RIGHT(
        id = "right",
        mpvValue = "right",
        defaultTitle = "Right Channel Only",
        defaultDescription = "Plays only the right channel sound through both speakers"
    );

    fun getDisplayName(lang: AppLanguage = AppLanguage.ENGLISH): String = when (this) {
        AUTO -> "Auto"
        AUTO_SAFE -> "Auto Safe"
        MONO -> "Mono"
        STEREO -> "Stereo"
        REVERSED_STEREO -> "Reversed Stereo"
        LEFT -> "Left Channel Only"
        RIGHT -> "Right Channel Only"
    }

    fun getDescription(lang: AppLanguage = AppLanguage.ENGLISH): String = defaultDescription
}

enum class AudioSurroundMode(
    val id: String,
    val displayName: String,
    val filterString: String
) {
    OFF("off", "Off", ""),
    HEADPHONE_SURROUND("headphone_3d", "Headphone 3D", "lavfi=[bs2b=profile=default]"),
    VIRTUAL_SURROUND("virtual_room", "Virtual Room", "lavfi=[surround=chl_out=stereo:level_in=1.0:level_out=1.0]"),
    CROSSFEED("crossfeed", "Crossfeed", "lavfi=[bs2b=fcut=700:feed=45]");
}

data class TrackAudioConfig(
    val channelMode: AudioChannelMode = AudioChannelMode.AUTO_SAFE,
    val volumeNormalization: Boolean = false,
    val dynamicRangeCompression: Boolean = false
)

/**
 * 5-Band Equalizer Presets for real-time sound sculpting:
 * 1. 60 Hz
 * 2. 230 Hz
 * 3. 910 Hz
 * 4. 3.6 kHz
 * 5. 14 kHz
 */
enum class EqualizerPreset(
    val id: String,
    val displayName: String,
    val gains: List<Float>
) {
    FLAT("Flat", "Flat", listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)),
    ROCK("Rock", "Rock", listOf(4.5f, 2.5f, -0.5f, 2.0f, 4.0f)),
    POP("Pop", "Pop", listOf(-1.0f, 2.0f, 4.0f, 2.0f, -1.0f)),
    JAZZ("Jazz", "Jazz", listOf(3.5f, 2.0f, -1.5f, 1.5f, 3.0f)),
    CLASSICAL("Classical", "Classical", listOf(4.0f, 2.5f, -1.0f, 2.0f, 3.5f)),
    ELECTRONIC("Electronic", "Electronic", listOf(4.0f, 3.0f, 0.0f, 2.0f, 4.5f)),
    BASS_BOOST("Bass Boost", "Bass Boost", listOf(6.0f, 4.0f, 1.0f, 0.0f, 0.0f)),
    TREBLE_BOOST("Treble Boost", "Treble Boost", listOf(0.0f, 0.0f, 1.0f, 4.0f, 6.0f)),
    VOICE_BOOST("Voice Boost", "Voice Boost", listOf(-2.0f, 1.0f, 5.0f, 3.5f, 0.0f)),
    LOUDNESS("Loudness", "Loudness", listOf(5.0f, 2.0f, -1.0f, 2.0f, 4.0f)),
    CINEMA_3D("Cinema 3D", "Cinema 3D", listOf(5.5f, 1.5f, 2.0f, 3.0f, 5.0f)),
    DEEP_CLUB_BASS("Deep Bass", "Deep Bass", listOf(7.5f, 5.0f, 0.0f, 1.5f, 3.0f)),
    VOCAL_CLARITY("Vocal Clarity", "Vocal Clarity", listOf(-3.0f, 0.5f, 6.0f, 4.0f, 1.5f)),
    ACOUSTIC("Acoustic", "Acoustic", listOf(3.0f, 4.0f, 2.5f, 1.0f, 2.5f)),
    HIFI_MASTER("Hi-Fi Master", "Hi-Fi Master", listOf(2.5f, -1.0f, 1.0f, 2.0f, 4.0f)),
    NIGHT_DIALOG("Night Dialog", "Night Dialog", listOf(-4.0f, -1.0f, 4.5f, 2.0f, -2.0f));

    companion object {
    }
}
