package net.youapps.wallpaper_apis.px

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

class GooglePixelApi : WallpaperApi() {
    override val name: String = "Google Pixel"
    override val baseUrl: String = "https://api.github.com"
    private val api = RetrofitHelper.create<Pixel>(baseUrl)
    private val imgSrcPrefix = "https://raw.githubusercontent.com/wacko1805/Pixel-Wallpapers/main/"

    private var wallpapers: List<Wallpaper> = emptyList()
    private val resultsPerPage = 20

    // the wallpapers are sorted into folders by device or collection, e.g. "Pixel 8"
    override val suggestedTags: List<String>
        get() = wallpapers.mapNotNull { it.category }.distinct().sortedDescending()

    private suspend fun updateWallpaperList() {
        wallpapers = api.getWallpapers()
            .tree
            .filter { it.path.endsWith(".jpg") || it.path.endsWith(".png") }
            .map {
                Wallpaper(
                    title = it.path.split("/").last().split(".").first(),
                    imgSrc = imgSrcPrefix + it.path,
                    category = it.path.split("/").first(),
                    // the folders in between often describe the wallpaper too, e.g. its collection
                    tags = it.path.split("/").drop(1).dropLast(1),
                    fileSize = it.size
                )
            }
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        if (wallpapers.isEmpty()) {
            updateWallpaperList()
        }

        val start = (page - 1) * resultsPerPage
        if (start >= wallpapers.size) return emptyList()
        return wallpapers.subList(start, minOf(start + resultsPerPage, wallpapers.size))
    }

    // all wallpapers are loaded at once, so they can be searched completely
    override suspend fun search(page: Int): List<Wallpaper> {
        if (wallpapers.isEmpty()) updateWallpaperList()
        return searchInList(wallpapers, page, resultsPerPage)
    }

    override suspend fun randomWallpaperUrl(): String? {
        if (wallpapers.isEmpty()) updateWallpaperList()
        return wallpapers.filter { matchesSelectedTags(it) }.randomOrNull()?.imgSrc
    }

    override suspend fun getRandomWallpaperUrl(): String? {
        if (wallpapers.isEmpty()) updateWallpaperList()
        return wallpapers.randomOrNull()?.imgSrc
    }
}
