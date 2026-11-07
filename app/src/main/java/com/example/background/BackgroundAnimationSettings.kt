package com.example.background

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDate

/**
 * Settings > Appearance > Background: the animation drawn behind every Home / folder / video list screen.
 * Snapshot-state backed (like the accent gradient), so a change in the settings sheet shows live behind it.
 * Values live in their own SharedPreferences file; [init] must run once at app start.
 */
object BackgroundAnimationSettings {
    private const val PREFS = "viaura_background_animation"
    private var appContext: Context? = null

    private val typeState = mutableStateOf(BackgroundType.NONE)
    private val intervalState = mutableStateOf(RandomInterval.EACH_LAUNCH)
    private val parallaxState = mutableStateOf(true)
    private val overWallpaperState = mutableStateOf(true)
    private val seasonalState = mutableStateOf(true)
    // True from the first launch after the Diwali update until the user picks a background by hand:
    // while Diwali runs, its backdrop plays whatever the pick is. Never touched again after the festival ends.
    private val festivalAutoState = mutableStateOf(false)
    // Names of the "NEW" backdrops the user has already tapped (their tag is gone for good).
    private val seenNewState = mutableStateOf<Set<String>>(emptySet())

    val type: BackgroundType get() = typeState.value
    val randomInterval: RandomInterval get() = intervalState.value
    val parallax: Boolean get() = parallaxState.value
    val overWallpaper: Boolean get() = overWallpaperState.value
    val seasonalThemes: Boolean get() = seasonalState.value
    val festivalAuto: Boolean get() = festivalAutoState.value

    private fun prefs(): android.content.SharedPreferences? =
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        val p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        typeState.value = BackgroundType.PICKABLE.firstOrNull { it.name == p.getString("type", null) } ?: BackgroundType.NONE
        intervalState.value = RandomInterval.entries.firstOrNull { it.name == p.getString("interval", null) } ?: RandomInterval.EACH_LAUNCH
        parallaxState.value = p.getBoolean("parallax", true)
        overWallpaperState.value = p.getBoolean("over_wallpaper", true)
        // One-time switch-on for the Diwali update: the key is missing only on the first launch of this version
        if (!p.contains("festival_auto")) {
            p.edit().putBoolean("festival_auto", true).putBoolean("seasonal", true).apply()
        }
        festivalAutoState.value = p.getBoolean("festival_auto", false)
        seasonalState.value = p.getBoolean("seasonal", true)
        seenNewState.value = p.getStringSet("seen_new", emptySet())?.toSet() ?: emptySet()
    }

    /** True while [type] is a newer backdrop that the user has not tapped yet (shows the "NEW" tag). */
    fun isNewBadge(type: BackgroundType): Boolean =
        type in BackgroundType.NEW_TYPES && type.name !in seenNewState.value

    /** The user touched [type]: its "NEW" tag goes away and stays away. */
    fun markSeen(type: BackgroundType) {
        if (type !in BackgroundType.NEW_TYPES || type.name in seenNewState.value) return
        val next = seenNewState.value + type.name
        seenNewState.value = next
        prefs()?.edit()?.putStringSet("seen_new", next)?.apply()
    }

    fun setType(value: BackgroundType) {
        typeState.value = value
        // The user chose a background by hand: from now on their pick wins over the automatic festival backdrop
        festivalAutoState.value = false
        prefs()?.edit()?.putString("type", value.name)?.putBoolean("festival_auto", false)?.apply()
    }

    fun setRandomInterval(value: RandomInterval) {
        intervalState.value = value
        prefs()?.edit()?.putString("interval", value.name)?.apply()
    }

    fun setParallax(value: Boolean) {
        parallaxState.value = value
        prefs()?.edit()?.putBoolean("parallax", value)?.apply()
    }

    fun setOverWallpaper(value: Boolean) {
        overWallpaperState.value = value
        prefs()?.edit()?.putBoolean("over_wallpaper", value)?.apply()
    }

    fun setSeasonalThemes(value: Boolean) {
        seasonalState.value = value
        if (!value) festivalAutoState.value = false
        val e = prefs()?.edit()
        e?.putBoolean("seasonal", value)
        if (!value) e?.putBoolean("festival_auto", false)
        e?.apply()
    }

    internal fun lastRandom(): String = prefs()?.getString("last_random", "") ?: ""
    internal fun lastRandomDay(): Long = prefs()?.getLong("last_random_day", 0L) ?: 0L
    internal fun saveRandom(name: String, day: Long) {
        prefs()?.edit()?.putString("last_random", name)?.putLong("last_random_day", day)?.apply()
    }
    internal fun clearRandomDay() {
        prefs()?.edit()?.putLong("last_random_day", 0L)?.apply()
    }
}

/**
 * Draws [type] full-screen and cross-fades when it changes. NONE and RANDOM draw nothing here:
 * RANDOM must be resolved to a concrete type first (see [rememberActiveBackground]).
 *
 * @param paused Stop advancing the clock, for when something opaque covers the page.
 */
@Composable
fun AnimatedBackground(
    type: BackgroundType,
    parallax: Boolean,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
) {
    Crossfade(
        targetState = type,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        modifier = modifier.fillMaxSize().clipToBounds(),
        label = "background_type",
    ) { shown ->
        if (shown == BackgroundType.NONE || shown == BackgroundType.RANDOM) {
            Box(Modifier.fillMaxSize())
        } else {
            BackdropCanvas(type = shown, parallax = parallax, running = !paused)
        }
    }
}

/** What the app should be drawing behind its screens right now. */
data class ActiveBackground(
    /** Concrete backdrop to draw (never RANDOM). NONE means nothing. */
    val type: BackgroundType,
    val parallax: Boolean,
    val overWallpaper: Boolean,
    val event: SeasonalEvent?,
)

/**
 * The user's choices folded into one answer: RANDOM turned into a concrete type, and a running
 * seasonal event laid over the pick when seasonal themes are on.
 */
@Composable
fun rememberActiveBackground(): ActiveBackground {
    val picked = BackgroundAnimationSettings.type
    val interval = BackgroundAnimationSettings.randomInterval
    val seasonal = BackgroundAnimationSettings.seasonalThemes
    val festivalAuto = BackgroundAnimationSettings.festivalAuto
    val parallax = BackgroundAnimationSettings.parallax
    val overWallpaper = BackgroundAnimationSettings.overWallpaper
    val today = rememberBackdropDate()
    val event = remember(today) { SeasonalEvent.on(today) }

    val revision = RandomBackground.revision
    val resolved = remember(picked, interval, today, revision) {
        if (picked == BackgroundType.RANDOM) RandomBackground.current(interval) else picked
    }
    val shown = event.backdropFor(picked, seasonal, festivalAuto).let { if (it == BackgroundType.RANDOM) resolved else it }
    return ActiveBackground(shown, parallax, overWallpaper, event?.takeIf { seasonal })
}

/**
 * The page animation for full-screen pages that are not part of Home (Shared playlist, Channel page): same pick and
 * same rule as Home. With Appearance = System it only plays while "Show over wallpaper" is ON.
 * Place it as the FIRST child of the page's root Box (which must not paint an opaque colour over it).
 */
@Composable
fun PageBackdrop(themeMode: com.example.ui.state.ThemeMode?) {
    val active = rememberActiveBackground()
    if (active.type != BackgroundType.NONE &&
        (themeMode != com.example.ui.state.ThemeMode.SYSTEM || active.overWallpaper)
    ) {
        AnimatedBackground(type = active.type, parallax = active.parallax)
    }
}

/** Re-evaluate seasonal and daily picks when returning to an app left open overnight. */
@Composable
private fun rememberBackdropDate(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = LocalDate.now() }
    return today
}

/** Resolves RANDOM once per process (or per interval) so every caller agrees on the same pick. */
object RandomBackground {
    private var processPick: BackgroundType? = null
    var revision by mutableIntStateOf(0)
        private set

    /** Forget the current pick so the next lookup rolls again. */
    fun reroll() {
        processPick = null
        BackgroundAnimationSettings.clearRandomDay()
        revision++
    }

    fun current(interval: RandomInterval): BackgroundType {
        processPick?.let { if (interval == RandomInterval.EACH_LAUNCH) return it }
        val today = LocalDate.now().toEpochDay()
        val last = BackgroundType.SHUFFLEABLE.firstOrNull { it.name == BackgroundAnimationSettings.lastRandom() }
        val lastDay = BackgroundAnimationSettings.lastRandomDay()
        if (interval != RandomInterval.EACH_LAUNCH && last != null && lastDay > 0L && today - lastDay in 0L until interval.days.toLong()) {
            processPick = last
            return last
        }
        // Never hand out the one the previous launch showed
        val next = BackgroundType.SHUFFLEABLE.filter { it != last }.random()
        BackgroundAnimationSettings.saveRandom(next.name, today)
        processPick = next
        return next
    }
}
