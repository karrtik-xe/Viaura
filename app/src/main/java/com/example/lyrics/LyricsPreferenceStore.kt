package com.example.preferences

import android.content.Context
import android.content.SharedPreferences

/** Small lyrics-only preference surface matching the reference repository's contract. */
class AudioPreferences(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("viaura_audio_preferences", Context.MODE_PRIVATE)

    val lyricsAutoFetchOnline = BooleanPreference(preferences, "lyrics_auto_fetch_online", true)
    val paxsenixApiKey = StringPreference(preferences, "paxsenix_api_key", "")

    class BooleanPreference internal constructor(
        private val preferences: SharedPreferences,
        private val key: String,
        private val defaultValue: Boolean
    ) {
        fun get(): Boolean = preferences.getBoolean(key, defaultValue)
        fun set(value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    }

    class StringPreference internal constructor(
        private val preferences: SharedPreferences,
        private val key: String,
        private val defaultValue: String
    ) {
        fun get(): String = preferences.getString(key, defaultValue) ?: defaultValue
        fun set(value: String) { preferences.edit().putString(key, value).apply() }
    }
}
