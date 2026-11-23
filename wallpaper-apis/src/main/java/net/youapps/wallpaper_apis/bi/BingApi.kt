package net.youapps.wallpaper_apis.bi

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

class BingApi : WallpaperApi() {
    override val name: String = "Bing"
    override val baseUrl: String = "https://www.bing.com"
    override val availableFilters: Map<String, List<String>> = mapOf(
        "resolution" to listOf("1080x1920", "768x1366", "1366x768", "1920x1080", "UHD")
    )
    override val suggestedTags: List<String> = listOf(
        "mountain",
        "national park",
        "beach",
        "island",
        "lake",
        "snow",
        "forest",
        "wildlife",
        "castle",
        "aurora"
    )

    private val previewResolution = "768x1366"

    val api = RetrofitHelper.create<Bing>(baseUrl)

    private fun getImgSrc(path: String, resolution: String): String {
        return "$baseUrl${path}_${resolution}.jpg"
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        // only the images of about the last two weeks are available, in two pages
        if (page > 2) return emptyList()
        val resolution = selectedFilters["resolution"]!!

        return api.getImages(index = if (page == 1) 0 else 7).images.map {
            Wallpaper(
                imgSrc = getImgSrc(it.urlBase, resolution),
                thumb = getImgSrc(it.urlBase, previewResolution),
                title = it.title,
                url = "$baseUrl${it.quiz}",
                resolution = resolution,
                // creation date doesn't contain any dividers by default
                creationDate = it.startDate
                    ?.replaceRange(6, 6, "-")
                    ?.replaceRange(4, 4, "-"),
                author = it.copyright
            )
        }
    }

    override suspend fun getRandomWallpaperUrl() = api.getImages(1).images
        .firstOrNull()
        ?.let {
            getImgSrc(it.urlBase, selectedFilters["resolution"]!!)
        }
}
