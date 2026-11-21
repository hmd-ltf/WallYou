package com.bnyro.wallpaper.ext

private val hexColorRegex = Regex("^[0-9a-fA-F]{6}$")
private val dimensionRegex = Regex("^(\\d+)x(\\d+)$")

/**
 * Format the key of a search filter for displaying it, e.g. `min_resolution` -> `Min resolution`.
 */
fun String.formatFilterName() = replace("_", " ").capitalize()

/**
 * Format a search filter option for displaying it, e.g.
 * - `date_added` -> `Date added`
 * - `16x9` -> `16:9`
 * - `1920x1080` -> `1920 × 1080`
 */
fun String.formatFilterOption(): String {
    dimensionRegex.matchEntire(this)?.let { match ->
        val (first, second) = match.destructured
        // small numbers are aspect ratios, larger ones are resolutions
        return if (first.length <= 2 && second.length <= 2) "$first:$second" else "$first × $second"
    }

    return replace("_", " ").capitalize()
}

/**
 * Parse the filter option as color if it's a hex color code like `cc3333`.
 *
 * @return the color as ARGB integer or null if the filter option is not a color
 */
fun String.parseFilterColorOrNull(): Int? {
    if (!hexColorRegex.matches(this)) return null

    return (0xFF000000 or toLong(16)).toInt()
}
