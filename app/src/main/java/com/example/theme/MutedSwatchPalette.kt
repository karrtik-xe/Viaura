package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/** One swatch of the muted & desaturated colour palette. */
data class MutedSwatch(val name: String, val argb: Long) {
    val color: Color get() = Color(argb)
}

/**
 * Muted & desaturated colours offered by the App Theme (Dynamic) gradient editor and by the Interface Appearance
 * "System" colour panel. The first 48 are the reference palette; the rest extend it in the same soft style.
 * Very light colours make a washed-out gradient, so every swatch lighter than [LIGHT_LIMIT] is listed LAST.
 */
object MutedPalette {
    private const val LIGHT_LIMIT = 0.70f

    private fun s(name: String, hex: Long) = MutedSwatch(name, 0xFF000000L or hex)

    private val base = listOf(
        s("Muted Red", 0x8B2F3B), s("Dusty Rose", 0xA05252), s("Terracotta", 0xC98474), s("Sandstone", 0xD9A38C),
        s("Peach Beige", 0xE6B89C), s("Burnt Orange", 0xC77D33), s("Mustard", 0xD4A017), s("Olive Beige", 0xB3A369),
        s("Olive Green", 0x556B2F), s("Sage Green", 0x6B7F3A), s("Moss Green", 0x7F9D58), s("Muted Mint", 0x9BBF9A),
        s("Sea Green", 0x5B8F72), s("Teal Green", 0x2F6F6A), s("Teal Blue", 0x3B7C82), s("Muted Cyan", 0x4A8CA8),
        s("Pale Sky", 0x9CC3D5), s("Steel Blue", 0x2E5B8A), s("Muted Blue", 0x406A9F), s("Slate Blue", 0x5B7DB1),
        s("Dusty Blue", 0x7C8FBE), s("Soft Blue", 0x9AB0D6), s("Indigo Blue", 0x3F4E7A), s("Blue Gray", 0x485673),
        s("Muted Purple", 0x6A5ACD), s("Deep Purple", 0x542F6F), s("Muted Violet", 0x7E5BA8), s("Soft Lavender", 0xA98ED1),
        s("Pale Purple", 0xD1A7E0), s("Muted Magenta", 0xB24C6E), s("Dusty Pink", 0xD17C9A), s("Blush Pink", 0xE3A5B1),
        s("Mocha", 0x8B6F47), s("Chocolate", 0x654321), s("Taupe", 0xA68D72), s("Sand Brown", 0xC4B198),
        s("Light Taupe", 0xD8C9AF), s("Warm Beige", 0xB8A98F), s("Mushroom", 0x9E8D7B), s("Stone Brown", 0x7A6F64),
        s("Charcoal", 0x1C1C1C), s("Graphite", 0x2D2D2D), s("Gunmetal", 0x3F3F3F), s("Slate Gray", 0x5A5A5A),
        s("Ash Gray", 0x7A7A7A), s("Cool Gray", 0x8A8A8A), s("Silver Gray", 0xB0B0B0), s("Soft White", 0xE8E8E8)
    )

    private val extra = listOf(
        s("Burgundy", 0x6D1F2B), s("Brick", 0x9C4A3C), s("Copper", 0xB87333), s("Ochre", 0xB8892B),
        s("Khaki", 0xA89F68), s("Forest", 0x3E5A3A), s("Fern", 0x4F7942), s("Jade", 0x3E8E7E),
        s("Petrol", 0x1F5F6B), s("Cobalt", 0x2B4C8C), s("Navy", 0x1F2F5A), s("Twilight", 0x5B4B8A),
        s("Plum", 0x6B3A5E), s("Mauve", 0x9C6B8E), s("Rosewood", 0x8E4A5A), s("Clay", 0xA9715A),
        s("Ink", 0x20283A), s("Steel Gray", 0x6B7280), s("Mist Green", 0xC9D8C5), s("Ice Blue", 0xBFD7E3),
        s("Lilac Mist", 0xD9CBE6), s("Shell", 0xEBCFC3), s("Linen", 0xE4DACB), s("Cloud Gray", 0xCBD0D6)
    )

    private fun lightness(c: MutedSwatch): Float {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(c.color.toArgb(), out)
        return out[2]
    }

    /** Every swatch, strong / mid tones first and the very light ones last. */
    val all: List<MutedSwatch> by lazy { (base + extra).sortedBy { if (lightness(it) > LIGHT_LIMIT) 1 else 0 } }
}
