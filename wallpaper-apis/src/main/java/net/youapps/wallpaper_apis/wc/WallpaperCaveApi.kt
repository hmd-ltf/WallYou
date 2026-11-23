package net.youapps.wallpaper_apis.wc

import com.fleeksoft.ksoup.Ksoup
import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi
import retrofit2.HttpException

class WallpaperCaveApi: WallpaperApi() {
    override val name: String = "WallpaperCave"
    override val baseUrl: String = "https://wallpapercave.com"

    override val availableFilters: Map<String, List<String>> = mapOf("page" to listOf("featured", "latest"))

    // searching opens the album (collection) of wallpapers for the tags, e.g. "forest wallpapers"
    override val tagSearch = TagSearch.NATIVE
    override val randomRespectsTags = true
    override val suggestedTags: List<String> = listOf(
        "4k",
        "anime",
        "nature",
        "dark",
        "space",
        "cars",
        "aesthetic",
        "minimalist",
        "gaming",
        "forest",
        "ocean",
        "city"
    )

    private val api: WallpaperCave by lazy {
        RetrofitHelper.create(baseUrl)
    }

    private fun parseHtml(html: String): List<Wallpaper> {
        val doc = Ksoup.parse(html)

        return doc.select("a.wpimg").map {
            val thumbSrc = baseUrl + it.select("img").attr("src")
            println(thumbSrc)

            Wallpaper(
                // "/wpr" seems to be for thumbs, "/wp" for full-res images
                imgSrc = thumbSrc.replace("/wpr/", "/wp/"),
                url = baseUrl + it.attr("href"),
                title = it.attr("title"),
                thumb = thumbSrc
            )
        }
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        if (includedTags.isNotEmpty()) {
            // albums are shown on a single page
            return if (page == 1) searchAlbum(includedTags) else emptyList()
        }

        // page is 0-based
        val pageType = selectedFilters["page"]!!
        val responseHtml = when (pageType) {
            "featured" -> {
                api.getFeaturedWallpapers(page - 1).first().imgs
            }
            "latest" -> {
                api.getLatestWallpapers(page - 1).first().imgs
            }
            else -> {
                throw IllegalArgumentException("unsupported page type")
            }
        }

        return parseHtml(responseHtml)
    }

    override suspend fun getRandomWallpaperUrl(): String? {
        if (includedTags.isNotEmpty()) return searchAlbum(includedTags).randomOrNull()?.imgSrc

        val response = api.getLatestWallpapers(0).first()
        return parseHtml(response.imgs).randomOrNull()?.imgSrc
    }

    /**
     * Load the wallpapers of the album for the given [tags], e.g. "forest-wallpapers" for "forest".
     * If there's no album with exactly that name, the best search result is used instead.
     */
    private suspend fun searchAlbum(tags: List<String>): List<Wallpaper> {
        val slug = tags.joinToString("-") { it.trim().lowercase() }
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        if (slug.isEmpty()) return emptyList()

        val albumName = if (slug.endsWith("wallpapers")) slug else "$slug-wallpapers"
        val wallpapers = loadAlbum(albumName)
        if (wallpapers.isNotEmpty()) return wallpapers

        val searchHtml = try {
            api.search(tags.joinToString(" ")).string()
        } catch (e: HttpException) {
            if (e.code() == 404) return emptyList() else throw e
        }
        val firstAlbum = Ksoup.parse(searchHtml).select("a[href]")
            .map { it.attr("href").substringAfter(baseUrl).trim('/') }
            .firstOrNull { it.endsWith("-wallpapers") && !it.contains("/") && it != albumName }
            ?: return emptyList()
        return loadAlbum(firstAlbum)
    }

    private suspend fun loadAlbum(albumName: String): List<Wallpaper> {
        val html = try {
            api.getAlbum(albumName).string()
        } catch (e: HttpException) {
            if (e.code() == 404) return emptyList() else throw e
        }
        val doc = Ksoup.parse(html)
        val albumTitle = doc.selectFirst("h1")?.text()

        return doc.select("img").mapNotNull { img ->
            val src = listOf("data-src", "src")
                .map { img.attr(it) }
                .firstOrNull { ALBUM_IMAGE_REGEX.containsMatchIn(it) }
                ?: return@mapNotNull null
            val path = ALBUM_IMAGE_REGEX.find(src)!!.value
            val link = img.parents().firstOrNull { it.tagName() == "a" }?.attr("href")

            Wallpaper(
                // "/wpt/" and "/wpr/" are for thumbnails, "/wp/" for full-res images
                imgSrc = baseUrl + path.replace(Regex("^/wp[tr]/"), "/wp/"),
                thumb = baseUrl + path,
                title = img.attr("alt").takeIf { it.isNotBlank() } ?: albumTitle,
                category = albumTitle,
                url = link?.let { if (it.startsWith("http")) it else baseUrl + it }
            )
        }.distinctBy { it.imgSrc }
    }

    companion object {
        private val ALBUM_IMAGE_REGEX = Regex("/wp[tr]?/[A-Za-z0-9_-]+\\.(jpg|jpeg|png|webp)")
    }
}