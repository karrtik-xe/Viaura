package com.example.util

import androidx.compose.runtime.mutableStateOf

/** The three Home header buttons that stay available in the portrait pinned player header. */
enum class PlayerHeaderAction { SEARCH, SORT, SETTINGS }

/**
 * The pinned player's header (Search / Sort & View / Settings) hands the request to the Home screen,
 * which opens the matching UI as soon as it is on screen and then clears it.
 */
object PlayerHeaderRequest {
    val pending = mutableStateOf<PlayerHeaderAction?>(null)
}
