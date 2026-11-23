package com.bnyro.wallpaper

import com.bnyro.wallpaper.db.Converters
import com.bnyro.wallpaper.db.obj.Wallpaper
import com.bnyro.wallpaper.util.ThemeColorHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorTest {
    private val blue = 0xFF1E88E5.toInt()
    private val red = 0xFFE53935.toInt()

    @Test
    fun similarColorsMatch() {
        assertTrue(ThemeColorHelper.matchesThemeColor(0xFF2962FF.toInt(), blue))
        assertTrue(ThemeColorHelper.matchesThemeColor(0xFF1565C0.toInt(), blue))
        // hues wrap around: pinkish red and orange-ish red are both red
        assertTrue(ThemeColorHelper.matchesThemeColor(0xFFC62828.toInt(), red))
    }

    @Test
    fun differentColorsDontMatch() {
        assertFalse(ThemeColorHelper.matchesThemeColor(red, blue))
        assertFalse(ThemeColorHelper.matchesThemeColor(0xFF43A047.toInt(), blue))
    }

    @Test
    fun wallpapersWithoutColorNeverMatch() {
        assertFalse(ThemeColorHelper.matchesThemeColor(Wallpaper.NO_SEED_COLOR, blue))
    }

    @Test
    fun everyPresetMatchesOnlyItself() {
        val presets = ThemeColorHelper.presetColors.map { it.first }
        for (preset in presets) {
            val matches = presets.filter { ThemeColorHelper.matchesThemeColor(it, preset) }
            assertTrue("$preset should match itself", preset in matches)
            // neighbouring presets may overlap a bit, but not more than one on each side
            assertTrue("${Integer.toHexString(preset)} matches $matches", matches.size <= 3)
        }
    }

    @Test
    fun tagsSurviveTheDatabase() {
        val converters = Converters()
        val tags = listOf("nature", "dark, moody", "ünïcode")
        assertEquals(tags, converters.toStringList(converters.fromStringList(tags)))
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList("not json"))
    }
}
