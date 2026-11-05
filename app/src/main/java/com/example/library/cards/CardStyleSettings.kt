package com.example.ui.components

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Settings > Appearance > Display & Feel > Remove Card UI.
 * ON = folder / video rows lose their card (fill, border, shadow, watched-progress fill): only the thumbnail and the
 * text stay, drawn straight on the page, so the background animation shows through cleanly. The watched / played
 * progress then shows as a thin bar on the thumbnail (see ThumbnailPlayedProgress.kt).
 * Snapshot-state backed, so every card recomposes the moment the switch is flipped.
 */
object CardStyleSettings {
    private const val PREFS = "viaura_card_style"
    private const val KEY_REMOVE = "remove_card_ui"
    private var appContext: Context? = null
    private val removeState = mutableStateOf(false)

    val removeCards: Boolean get() = removeState.value

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        removeState.value = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_REMOVE, false)
    }

    fun setRemoveCards(value: Boolean) {
        removeState.value = value
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_REMOVE, value)?.apply()
    }
}

/**
 * Applies [block] (the card fill / border / shadow) unless Remove Card UI is ON.
 * A selected card keeps its look so the selection stays visible.
 * With the card removed the row is still clipped to [shape] (the shape the card had), so the touch ripple that the
 * click modifier after it draws has the same rounded corners instead of a sharp rectangle.
 * Inline on purpose: the block may call composable modifiers such as liquidGlassOr.
 */
inline fun Modifier.cardSurface(
    keepWhenSelected: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
    block: Modifier.() -> Modifier
): Modifier =
    if (CardStyleSettings.removeCards && !keepWhenSelected) this.clip(shape) else this.block()

/** Size factor of the folder icon / Online logos: +5 % while Remove Card UI is ON, normal (1f) when OFF. */
fun cardlessIconScale(): Float = if (CardStyleSettings.removeCards) 1.05f else 1f

/**
 * Info chips (video count, storage, quality, duration, date ...) are drawn slightly see-through so the animated
 * background shows behind them. Works for the fill and the outline colour of a chip.
 */
fun chipTone(color: Color): Color = color.copy(alpha = color.alpha * 0.55f)

/**
 * Fill of a card while Remove Card UI is OFF: about 40 % see-through, so the animated background still shows
 * clearly behind the card.
 */
fun cardTone(color: Color): Color = color.copy(alpha = color.alpha * 0.6f)
