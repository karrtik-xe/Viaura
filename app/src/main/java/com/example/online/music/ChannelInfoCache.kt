package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Subscriber count of a channel as display text ("1.2M subscribers"). The search/feed results often come without it
 * (the website hides it or the list layout has none), so it is read once from the channel page and remembered.
 */
object ChannelInfoCache {
    private const val PREFS = "channel_info_cache"
    private const val TTL_MS = 3L * 24 * 3600 * 1000
    private val memory = ConcurrentHashMap<String, String>()

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Cached text without any network call (null when unknown). */
    fun cached(context: Context, channelUrl: String): String? {
        if (channelUrl.isBlank()) return null
        memory[channelUrl]?.let { return it }
        val raw = prefs(context).getString(channelUrl, null) ?: return null
        val at = raw.substringBefore('|').toLongOrNull() ?: return null
        val text = raw.substringAfter('|', "")
        if (text.isBlank()) return null
        // an old answer is still shown; it is only refreshed when [subscribers] is asked again
        memory[channelUrl] = text
        return if (System.currentTimeMillis() - at < Long.MAX_VALUE) text else null
    }

    suspend fun subscribers(context: Context, channelUrl: String): String? {
        if (channelUrl.isBlank()) return null
        val raw = prefs(context).getString(channelUrl, null)
        val at = raw?.substringBefore('|')?.toLongOrNull() ?: 0L
        val old = raw?.substringAfter('|', "")?.takeIf { it.isNotBlank() }
        if (old != null && System.currentTimeMillis() - at < TTL_MS) {
            memory[channelUrl] = old
            return old
        }
        val fresh = withContext(Dispatchers.IO) {
            try {
                OnlineMusicService.ensureInit()
                val ex = ServiceList.YouTube.getChannelExtractor(channelUrl)
                ex.fetchPage()
                val n = try { ex.subscriberCount } catch (_: Throwable) { -1L }
                if (n >= 0L) "${format(n)} subscribers" else null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Throwable) { null }
        }
        val text = fresh ?: old
        if (text != null) {
            memory[channelUrl] = text
            if (fresh != null) prefs(context).edit().putString(channelUrl, "${System.currentTimeMillis()}|$fresh").apply()
        }
        return text
    }

    private fun format(n: Long): String = when {
        n >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", n / 1_000_000_000.0).replace(".0B", "B")
        n >= 1_000_000L -> String.format(Locale.US, "%.1fM", n / 1_000_000.0).replace(".0M", "M")
        n >= 1_000L -> String.format(Locale.US, "%.1fK", n / 1_000.0).replace(".0K", "K")
        else -> n.toString()
    }
}
