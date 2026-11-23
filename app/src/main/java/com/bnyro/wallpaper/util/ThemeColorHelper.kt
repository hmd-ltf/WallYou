package com.bnyro.wallpaper.util

import android.graphics.Bitmap
import android.os.Build
import com.bnyro.wallpaper.R
import com.bnyro.wallpaper.db.obj.Wallpaper
import com.materialkolor.hct.Hct
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Helpers to find out which Material You theme a wallpaper would generate, e.g. to only apply
 * wallpapers that result in a theme of a certain color.
 */
object ThemeColorHelper {
    /**
     * Android extracts wallpaper colors from a downscaled image of about this many pixels.
     */
    private const val COLOR_EXTRACTION_AREA = 112 * 112

    /**
     * The side length images should at least be loaded with to compute their seed color.
     */
    const val SEED_COLOR_IMAGE_SIZE = 256

    /**
     * How far the hue of a wallpaper's theme may differ from the selected theme color (in degrees).
     */
    private const val MAX_HUE_DISTANCE = 30.0

    /**
     * Below this chroma, Android doesn't consider a color for generating a theme.
     */
    private const val MIN_SEED_CHROMA = 5.0

    /**
     * Theme colors to choose from when filtering wallpapers by their theme, with their names.
     */
    val presetColors = listOf(
        0xFFE53935.toInt() to R.string.color_red,
        0xFFFB8C00.toInt() to R.string.color_orange,
        0xFFFDD835.toInt() to R.string.color_yellow,
        0xFF43A047.toInt() to R.string.color_green,
        0xFF00897B.toInt() to R.string.color_teal,
        0xFF1E88E5.toInt() to R.string.color_blue,
        0xFF8E24AA.toInt() to R.string.color_purple,
        0xFFD81B60.toInt() to R.string.color_pink,
    )

    /**
     * The seed color Android would generate the Material You theme of the [bitmap] from,
     * or [Wallpaper.NO_SEED_COLOR] if it has no color that is suitable for that (e.g. greyscale images).
     */
    fun getSeedColor(bitmap: Bitmap): Int {
        val quantized = QuantizerCelebi.quantize(bitmap.toSamplePixels(), 128)
        // without a fallback color, greyscale images would count as blue (Android's fallback color)
        return Score.score(quantized, desired = 1, fallbackColorArgb = Wallpaper.NO_SEED_COLOR)
            .firstOrNull()
            ?.takeIf { it != Wallpaper.NO_SEED_COLOR && Hct.fromInt(it).chroma >= MIN_SEED_CHROMA }
            ?: Wallpaper.NO_SEED_COLOR
    }

    /**
     * Whether a wallpaper with the given [seedColor] generates a theme similar to the one of [themeColor].
     */
    fun matchesThemeColor(seedColor: Int, themeColor: Int): Boolean {
        if (seedColor == Wallpaper.NO_SEED_COLOR) return false

        val seedHue = Hct.fromInt(seedColor).hue
        val themeHue = Hct.fromInt(themeColor).hue
        val distance = abs(seedHue - themeHue).let { minOf(it, 360 - it) }
        return distance <= MAX_HUE_DISTANCE
    }

    /**
     * Downscale the bitmap to the size Android extracts colors from and return its pixels.
     */
    fun Bitmap.toSamplePixels(): IntArray {
        val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
            copy(Bitmap.Config.ARGB_8888, false)
        } else {
            this
        }

        val scale = sqrt(COLOR_EXTRACTION_AREA.toFloat() / (source.width * source.height)).coerceAtMost(1f)
        val width = max(1, (source.width * scale).roundToInt())
        val height = max(1, (source.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels
    }
}
