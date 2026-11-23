package net.youapps.wallpaper_apis.im

import com.fleeksoft.ksoup.Ksoup
import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi

class ImgthApi: WallpaperApi() {
    override val name: String = "Imgth"
    override val baseUrl: String = "https://imgth.com"

    override var communityName: String? = "1022/high-definition-wallpapers"
    override val requiresCommunityName: Boolean = true

    private val api: Imgth by lazy {
        RetrofitHelper.create(baseUrl)
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        val response = if (communityName.isNullOrBlank()) {
            api.getLive()
        } else {
            api.getCategory(communityName!!, page - 1)
        }

        val doc = Ksoup.parse(response.string())
        return doc.select("a.thumbnail").map {
            val image = it.select("img")
            val thumbnail = image.attr("src")
                .replace("http://", "https://")
            val href = it.attr("href")

            Wallpaper(
                imgSrc = thumbnail.replace("/thumbs/", "/images/"),
                thumb = thumbnail,
                url = baseUrl + href,
                // the image names are only available as alternative texts or in the links
                title = listOf(image.attr("alt"), image.attr("title"), it.attr("title"))
                    .firstOrNull { text -> text.isNotBlank() }
                    ?: href.substringAfterLast("/").replace("-", " ").takeIf { name ->
                        name.any { char -> char.isLetter() }
                    }
            )
        }
    }
}
