package net.youapps.wallpaper_apis.ps

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

class PicsumApi : WallpaperApi() {
    override val name: String = "Picsum"
    override val baseUrl: String = "https://picsum.photos"

    private val maxPage = 34

    private val api = RetrofitHelper.create<Picsum>(baseUrl)

    /** How often each photographer appeared in the loaded pages, to suggest them as tags. */
    private val authorCounts = mutableMapOf<String, Int>()

    // the photos only have the name of their photographer, so these are suggested for searching
    override val suggestedTags: List<String>
        get() = synchronized(authorCounts) {
            authorCounts.entries
                .sortedByDescending { it.value }
                .take(MAX_SUGGESTED_AUTHORS)
                .map { it.key }
        }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        if (page > maxPage) return emptyList()

        return api.getWallpapers(maxPage + 1 - page).map {
            Wallpaper(
                imgSrc = it.downloadUrl!!,
                author = it.author,
                resolution = "${it.width}x${it.height}",
                url = it.url
            )
        }.also { wallpapers ->
            synchronized(authorCounts) {
                wallpapers.mapNotNull { it.author }.forEach { author ->
                    authorCounts[author] = (authorCounts[author] ?: 0) + 1
                }
            }
        }
    }

    override suspend fun getRandomWallpaperUrl(): String {
        return "$baseUrl/1080/1920"
    }

    companion object {
        private const val MAX_SUGGESTED_AUTHORS = 10
    }
}
