package net.youapps.wallpaper_apis.wi

import com.fleeksoft.ksoup.Ksoup
import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi
import net.youapps.wallpaper_apis.wi.obj.WikiImageInfo
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WikipediaPotdApi : WallpaperApi() {
    override val name = "Wikipedia POTD"
    override val baseUrl = "https://en.wikipedia.org"
    override val suggestedTags: List<String> = listOf(
        "bird",
        "butterfly",
        "flower",
        "landscape",
        "mountain",
        "lake",
        "church",
        "castle",
        "bridge",
        "painting",
        "insect",
        "night"
    )

    private val api = RetrofitHelper.create<WikiPOTD>(baseUrl)
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        val now = LocalDate.now()
        val dates = mutableListOf<String>()
        for (i in 0 until PAGE_SIZE) {
            val offset = (page - 1) * PAGE_SIZE + i
            val date = now.minusDays(offset.toLong())
            dates.add(date.format(dateFormat))
        }

        val titles = dates.map { POTD_ARTICLE_PREFIX + it }
        return api.getImages(titles = titles.joinToString("|")).query.pages.values
            .filter { it.imageInfo.isNotEmpty() }
            .map { image ->
                val imageInfo = image.imageInfo.first()
                val categories = imageInfo.getMetadata("Categories")
                    ?.split("|")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()

                Wallpaper(
                    imgSrc = imageInfo.url,
                    url = imageInfo.descriptionUrl,
                    title = imageInfo.getPlainMetadata("ObjectName")
                        ?: image.title.substringAfter(":").substringBeforeLast("."),
                    description = imageInfo.getPlainMetadata("ImageDescription"),
                    author = imageInfo.getPlainMetadata("Artist"),
                    category = categories.firstOrNull(),
                    tags = categories,
                    resolution = "${imageInfo.width}x${imageInfo.height}",
                    thumb = imageInfo.thumbUrl
                )
            }
    }

    /** The metadata without HTML formatting, e.g. links. */
    private fun WikiImageInfo.getPlainMetadata(key: String): String? =
        getMetadata(key)?.let { Ksoup.parse(it).text() }?.takeIf { it.isNotBlank() }

    override suspend fun getRandomWallpaperUrl(): String? {
        val today = LocalDate.now().format(dateFormat)
        return api.getImages(titles = POTD_ARTICLE_PREFIX + today).query.pages.values.firstOrNull()
            ?.imageInfo?.firstOrNull()?.url
    }

    companion object {
        private const val POTD_ARTICLE_PREFIX = "Template:POTD/"
        private const val PAGE_SIZE = 10
    }
}
