package com.example.background

/**
 * Every animated backdrop the app can draw behind its screens.
 *
 * The first block is what the user can pick by hand. The seasonal block is only ever worn while
 * its [SeasonalEvent] runs, so it never shows in the picker and never comes out of the shuffle.
 */
enum class BackgroundType(
  val label: String,
) {
  CIRCLES("Circles"),
  RINGS("Rings"),
  SPACE("Space"),
  SHAPES("Shapes"),
  SNOW("Snow"),
  GRID("Grid"),
  PARTICLES("Particles"),
  LAVA("Lava lamp"),
  AURORA("Aurora"),
  FIREFLIES("Fireflies"),
  RAIN("Rain"),
  BOKEH("Bokeh"),
  SAKURA("Sakura"),
  CONSTELLATION("Constellation"),
  BUBBLES("Bubbles"),
  WAVES("Waves"),
  METEORS("Meteors"),
  EMBERS("Embers"),
  HEXAGONS("Hexagons"),
  CLOUDS("Clouds"),
  LEAVES("Leaves"),
  GALAXY("Galaxy"),
  RADAR("Radar"),
  EQUALIZER("Equalizer"),
  ORBITS("Orbits"),
  WARP("Warp"),
  FLOW("Flow"),
  PRISM("Prism"),
  LANTERNS("Lanterns"),
  CONFETTI("Confetti"),
  SILK("Silk"),
  NEBULA("Nebula"),
  RIPPLES("Ripples"),
  SYNTHWAVE("Synthwave"),
  MANDALA("Mandala"),
  NONE("None"),
  RANDOM("Random"),

  // Seasonal only
  HALLOWEEN("Halloween"),
  FIREWORKS("Fireworks"),
  WINTER("Winter lights"),
  DIWALI("Diwali"),
  HOLI("Holi"),
  ;

  val isSeasonal: Boolean get() = this in SEASONAL

  companion object {
    val SEASONAL: Set<BackgroundType> = setOf(HALLOWEEN, FIREWORKS, WINTER, DIWALI, HOLI)

    /** Backdrops added after v1.0: they wear a "NEW" tag in the picker until the user taps them once. */
    val NEW_TYPES: Set<BackgroundType> = setOf(
      WAVES, METEORS, EMBERS, HEXAGONS, CLOUDS, LEAVES, GALAXY, RADAR, EQUALIZER, ORBITS,
      WARP, FLOW, PRISM, LANTERNS, CONFETTI,
      SILK, NEBULA, RIPPLES, SYNTHWAVE, MANDALA,
    )

    /** The newest five: shown together in their own "NEW" section of the picker. */
    val FRESH: List<BackgroundType> = listOf(SILK, NEBULA, RIPPLES, SYNTHWAVE, MANDALA)

    /** Tiles of the picker, in display order. */
    val PICKABLE: List<BackgroundType> = entries.filter { it !in SEASONAL }

    /** What RANDOM may land on: anything that actually draws something. */
    val SHUFFLEABLE: List<BackgroundType> = PICKABLE.filter { it != NONE && it != RANDOM }
  }
}

/** How often RANDOM re-rolls. */
enum class RandomInterval(
  val label: String,
  val days: Int,
) {
  EACH_LAUNCH("On launch", 0),
  EVERY_DAY("Daily", 1),
  EVERY_3_DAYS("Every 3 days", 3),
}
