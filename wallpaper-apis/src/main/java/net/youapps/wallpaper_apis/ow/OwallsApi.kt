package net.youapps.wallpaper_apis.ow

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

class OwallsApi : WallpaperApi() {
    override val name = "OWalls"
    override val baseUrl = "https://gist.github.com/"
    override val availableFilters: Map<String, List<String>> = mapOf(
        "style" to listOf("all", "light", "dark")
    )

    private val api = RetrofitHelper.create<OWalls>(baseUrl)
    private val resultsPerPage = 20

    private var wallpapers: MutableList<Wallpaper> = mutableListOf()

    // the wallpapers are only named, so suggest the words that appear most often in their names
    override val suggestedTags: List<String>
        get() = wallpapers.asSequence()
            .mapNotNull { it.title }
            .flatMap { title -> title.lowercase().split(Regex("[^\\p{L}]+")) }
            .filter { it.length > 3 }
            .groupingBy { it }
            .eachCount()
            .entries
            .filter { it.value > 1 }
            .sortedByDescending { it.value }
            .take(MAX_SUGGESTED_TAGS)
            .map { it.key }

    private val filteredWallpapers: List<Wallpaper>
        get() = when (val query = selectedFilters["style"]) {
            "light" -> wallpapers.filter { it.category == query }
            "dark" -> wallpapers.filter { it.category == query }
            else -> wallpapers
        }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        if (wallpapers.isEmpty()) loadAll()

        val start = resultsPerPage * (page - 1)
        if (start >= filteredWallpapers.size) return emptyList()

        return filteredWallpapers.subList(start, minOf(page * resultsPerPage, filteredWallpapers.size))
    }

    // all wallpapers are loaded at once, so they can be searched completely
    override suspend fun search(page: Int): List<Wallpaper> {
        if (wallpapers.isEmpty()) loadAll()
        return searchInList(filteredWallpapers, page, resultsPerPage)
    }

    private suspend fun loadAll() {
        val loaded = mutableListOf<Wallpaper>()
        api.getAll().forEach { (name, images) ->
            images.jsonObject.forEach { (category, source) ->
                val imgSrc = source.jsonPrimitive.content
                if (imgSrc.isNotBlank()) {
                    loaded.add(
                        0,
                        // the key is the name of the wallpaper, e.g. "Mountains"
                        Wallpaper(imgSrc = imgSrc, category = category, title = name)
                    )
                }
            }
        }
        wallpapers = loaded
    }

    override suspend fun getRandomWallpaperUrl(): String? {
        if (wallpapers.isEmpty()) loadAll()
        return filteredWallpapers.randomOrNull()?.imgSrc
    }

    override suspend fun randomWallpaperUrl(): String? {
        if (wallpapers.isEmpty()) loadAll()
        return filteredWallpapers.filter { matchesSelectedTags(it) }.randomOrNull()?.imgSrc
    }

    companion object {
        private const val MAX_SUGGESTED_TAGS = 12
    }
}
