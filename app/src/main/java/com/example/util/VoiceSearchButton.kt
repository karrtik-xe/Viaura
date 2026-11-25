package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.AccentSkyBlue

/**
 * Voice search button for every search bar: the same blue as the search icon, placed at the END of the search box.
 * Tap: the microphone permission is asked first (only once it is allowed the microphone opens). While it listens the icon pulses; what
 * is said appears in the search box ([onText], live). When the speaking stops (a short silence) the microphone closes by itself and the
 * last text stays, so the search runs. Tapping again while it listens ends it at once. Uses the phone's own speech service (no AI API).
 */
@Composable
fun VoiceSearchButton(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Called once when the talking is over, with the final text (for search boxes that need a "search now" action). */
    onFinished: ((String) -> Unit)? = null,
    tint: Color = AccentSkyBlue,
    iconSize: Dp = 18.dp
) {
    val context = LocalContext.current
    val latest by rememberUpdatedState(onText)
    val latestFinished by rememberUpdatedState(onFinished)
    var listening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val silenceStop = remember { Runnable { try { recognizer?.stopListening() } catch (_: Throwable) {} } }

    fun finish() {
        handler.removeCallbacks(silenceStop)
        listening = false
        try { recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
    }

    fun start() {
        if (listening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Voice search is not available on this phone", Toast.LENGTH_SHORT).show()
            return
        }
        val r = try { SpeechRecognizer.createSpeechRecognizer(context) } catch (_: Throwable) { null }
        if (r == null) {
            Toast.makeText(context, "Voice search is not available on this phone", Toast.LENGTH_SHORT).show()
            return
        }
        var lastText = ""
        fun textOf(b: Bundle?): String = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                val t = textOf(partialResults)
                if (t.isNotBlank()) {
                    lastText = t
                    latest(t)
                    // no new words for a moment = he stopped talking: close the microphone, the text stays and is searched
                    handler.removeCallbacks(silenceStop)
                    handler.postDelayed(silenceStop, 1400L)
                }
            }
            override fun onResults(results: Bundle?) {
                val t = textOf(results).ifBlank { lastText }
                if (t.isNotBlank()) { latest(t); latestFinished?.invoke(t) }
                finish()
            }
            override fun onError(error: Int) {
                if (lastText.isNotBlank()) { latest(lastText); latestFinished?.invoke(lastText) }
                finish()
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }
        recognizer = r
        listening = true
        try {
            r.startListening(intent)
            // nothing said at all: close after a few seconds
            handler.postDelayed(silenceStop, 7000L)
        } catch (_: Throwable) {
            finish()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else Toast.makeText(context, "Microphone permission is needed for voice search", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(Unit) {
        onDispose {
            handler.removeCallbacks(silenceStop)
            try { recognizer?.destroy() } catch (_: Throwable) {}
            recognizer = null
        }
    }

    val pulse = rememberInfiniteTransition(label = "voiceSearchPulse")
    val wave by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "wave"
    )
    val beat by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse), label = "beat"
    )

    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (listening) {
                    try { recognizer?.stopListening() } catch (_: Throwable) { finish() }
                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    start()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (listening) {
            // the microphone is open: a soft ring spreads out and the icon beats
            Box(
                Modifier.size(iconSize + 6.dp).scale(wave).clip(CircleShape)
                    .background(tint.copy(alpha = (0.28f * (1.9f - wave) / 0.9f).coerceIn(0f, 0.28f)))
            )
        }
        StyledIcon(
            drawableRes = R.drawable.viaura_microphone,
            contentDescription = "Voice search",
            tint = tint,
            modifier = Modifier.size(iconSize).scale(if (listening) beat else 1f)
        )
    }
}
