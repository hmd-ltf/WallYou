package com.bnyro.wallpaper.util

import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeNeutral
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant
import com.materialkolor.score.Score

/**
 * Generates Material You color schemes from wallpapers, using the same algorithm as Android:
 * the wallpaper's colors are quantized, the best suited seed colors are picked and a dynamic color
 * scheme is derived from the selected seed color.
 */
object MaterialThemeGenerator {
    enum class Style {
        TONAL_SPOT,
        VIBRANT,
        EXPRESSIVE,
        NEUTRAL
    }

    /**
     * Extract the seed colors that Android would offer for a wallpaper with the given [pixels] (ARGB),
     * ordered from the most to the least suitable one.
     */
    fun extractSeedColors(pixels: IntArray, maxColors: Int = 4): List<Int> {
        val quantized = QuantizerCelebi.quantize(pixels, 128)
        return Score.score(quantized, desired = maxColors)
    }

    /**
     * Create the color scheme for the given [seedColor] (ARGB).
     */
    fun createScheme(seedColor: Int, isDark: Boolean, style: Style = Style.TONAL_SPOT): DynamicScheme {
        val hct = Hct.fromInt(seedColor)

        return when (style) {
            Style.TONAL_SPOT -> SchemeTonalSpot(hct, isDark, 0.0)
            Style.VIBRANT -> SchemeVibrant(hct, isDark, 0.0)
            Style.EXPRESSIVE -> SchemeExpressive(hct, isDark, 0.0)
            Style.NEUTRAL -> SchemeNeutral(hct, isDark, 0.0)
        }
    }
}
