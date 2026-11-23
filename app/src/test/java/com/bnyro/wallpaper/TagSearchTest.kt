package com.bnyro.wallpaper

import kotlinx.coroutines.runBlocking
import net.youapps.wallpaper_apis.TagMatcher
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun wallpaper(title: String) =
    Wallpaper(imgSrc = "https://example.com/${title.hashCode()}.jpg", title = title)

class TagSearchTest {
    /**
     * A source without its own search that returns [pages] of wallpapers.
     */
    private class LocalSource(private val pages: List<List<Wallpaper>>) : WallpaperApi() {
        override val name = "Local"
        override val baseUrl = "https://example.com"
        val requestedPages = mutableListOf<Int>()

        override suspend fun getWallpapers(page: Int): List<Wallpaper> {
            requestedPages += page
            return pages.getOrElse(page - 1) { emptyList() }
        }

        override suspend fun getRandomWallpaperUrl() = "https://example.com/unfiltered.jpg"
    }

    /**
     * A source with its own search, which records the queries it receives.
     */
    private class NativeSource(override val excludesTagsNatively: Boolean) : WallpaperApi() {
        override val name = "Native"
        override val baseUrl = "https://example.com"
        override val tagSearch = TagSearch.NATIVE
        val queries = mutableListOf<String>()

        override suspend fun getWallpapers(page: Int): List<Wallpaper> {
            queries += searchQuery
            if (page > 1) return emptyList()
            return listOf(wallpaper("red car"), wallpaper("blue car"))
        }
    }


    @Test
    fun tagsMatchWordStarts() {
        val text = TagMatcher.normalize("Snowy Mountains at Night_2024")
        assertTrue(TagMatcher.containsTag(text, "mountain"))
        assertTrue(TagMatcher.containsTag(text, "MOUNTAINS"))
        assertTrue(TagMatcher.containsTag(text, "snowy mountains"))
        assertTrue(TagMatcher.containsTag(text, "night"))
        assertFalse(TagMatcher.containsTag(text, "tains"))
        assertFalse(TagMatcher.containsTag(text, "mountains snowy"))
    }

    @Test
    fun allIncludedAndNoExcludedTagsMustMatch() {
        val wallpaper = Wallpaper(
            imgSrc = "https://example.com/forest-path.jpg",
            title = "Morning",
            description = "Fog over a lake",
            tags = listOf("Nature")
        )
        assertTrue(TagMatcher.matches(wallpaper, listOf("fog", "nature"), emptyList()))
        // the file name is searched too
        assertTrue(TagMatcher.matches(wallpaper, listOf("forest"), emptyList()))
        assertFalse(TagMatcher.matches(wallpaper, listOf("fog", "desert"), emptyList()))
        assertFalse(TagMatcher.matches(wallpaper, listOf("fog"), listOf("lake")))
    }

    @Test
    fun localSearchLoadsMorePagesUntilEnoughMatches() = runBlocking {
        val source = LocalSource(
            listOf(
                listOf(wallpaper("city"), wallpaper("beach")),
                listOf(wallpaper("forest"), wallpaper("dark forest")),
                List(12) { wallpaper("forest $it") },
                listOf(wallpaper("forest late"))
            )
        )
        source.selectedTags = listOf("forest")

        val first = source.search(1)
        assertEquals(14, first.size)
        assertEquals(listOf(1, 2, 3), source.requestedPages)
        assertTrue(source.hasMoreResults)

        // continues where the last search stopped
        val second = source.search(2)
        assertEquals(listOf("forest late"), second.map { it.title })
        assertEquals(listOf(1, 2, 3, 4, 5), source.requestedPages)
        assertFalse(source.hasMoreResults)
    }

    @Test
    fun localSearchGivesUpAfterSomePagesWithoutMatches() = runBlocking {
        val source = LocalSource(List(20) { listOf(wallpaper("city $it")) })
        source.selectedTags = listOf("forest")

        assertTrue(source.search(1).isEmpty())
        assertEquals(5, source.requestedPages.size)
        // there are more pages to search through
        assertTrue(source.hasMoreResults)
    }

    @Test
    fun excludedTagsAreFilteredLocally() = runBlocking {
        val source = LocalSource(listOf(listOf(wallpaper("red car"), wallpaper("blue car"), wallpaper("tree"))))
        source.selectedTags = listOf("car", "-blue")

        assertEquals(listOf("red car"), source.search(1).map { it.title })
    }

    @Test
    fun withoutTagsPagesAreLoadedAsIs() = runBlocking {
        val source = LocalSource(listOf(listOf(wallpaper("a")), listOf(wallpaper("b"))))

        assertEquals(listOf("b"), source.search(2).map { it.title })
        assertEquals(listOf(2), source.requestedPages)
        assertTrue(source.search(3).isEmpty())
        assertFalse(source.hasMoreResults)
    }

    @Test
    fun nativeSearchOnlyReceivesIncludedTagsIfItCantExclude() = runBlocking {
        val source = NativeSource(excludesTagsNatively = false)
        source.selectedTags = listOf("car", "-blue")

        // the source searched for "car", the excluded tag was filtered in the app
        assertEquals(listOf("red car"), source.search(1).map { it.title })
        assertTrue(source.queries.all { it == "car" })
    }

    @Test
    fun nativeExclusionReceivesAllTags() = runBlocking {
        val source = NativeSource(excludesTagsNatively = true)
        source.selectedTags = listOf("car", "-blue")

        // filtering is left to the source
        assertEquals(2, source.search(1).size)
        assertEquals(listOf("car -blue"), source.queries)
    }

    @Test
    fun randomWallpaperRespectsTags() = runBlocking {
        val source = LocalSource(listOf(listOf(wallpaper("city"), wallpaper("forest"))))
        assertEquals("https://example.com/unfiltered.jpg", source.randomWallpaperUrl())

        source.selectedTags = listOf("forest")
        assertEquals(wallpaper("forest").imgSrc, source.randomWallpaperUrl())

        source.selectedTags = listOf("desert")
        assertNull(source.randomWallpaperUrl())
    }
}
