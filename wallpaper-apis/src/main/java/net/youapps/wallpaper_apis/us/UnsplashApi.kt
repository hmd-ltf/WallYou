package net.youapps.wallpaper_apis.us

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi
import net.youapps.wallpaper_apis.us.obj.UsImage
import retrofit2.HttpException

class UnsplashApi : WallpaperApi() {
    override val name: String = "Unsplash"
    override val baseUrl: String = "https://unsplash.com"

    override val availableFilters: Map<String, List<String>> = mapOf(
        "orientation" to listOf("any", "landscape", "portrait", "squarish"),
       "order_by" to listOf("latest", "oldest", "popular")
    )
    override val supportsTags: Boolean = true

    // logging in with the Access Key of an Unsplash developer app switches to the official API
    override val supportsApiKey: Boolean = true
    override val apiKeyUrl: String = "https://unsplash.com/oauth/applications"

    private val api = RetrofitHelper.create<Unsplash>(baseUrl)
    private val officialApi = RetrofitHelper.create<UnsplashOfficial>(UnsplashOfficial.BASE_URL)

    override suspend fun validateApiKey(apiKey: String): Boolean {
        val response = officialApi.checkAccessKey(UnsplashOfficial.authorization(apiKey))

        return when {
            response.isSuccessful -> true
            response.code() == 401 -> false
            else -> throw Exception("HTTP ${response.code()}: ${response.message()}")
        }
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        val sortOrder = selectedFilters["order_by"]!!
        val orientation = selectedFilters["orientation"].takeIf { it != "any" }
        val accessKey = apiKey?.takeIf { it.isNotBlank() }

        val wallpapers = withReadableErrors {
            when {
                accessKey != null && selectedTags.isEmpty() -> officialApi.getWallpapers(
                    UnsplashOfficial.authorization(accessKey), page, sortOrder
                )

                accessKey != null -> officialApi.searchWallpapers(
                    UnsplashOfficial.authorization(accessKey),
                    page,
                    selectedTags.joinToString(" "),
                    orientation,
                    // the official search only supports sorting by relevance or date
                    if (sortOrder == "latest") "latest" else "relevant"
                ).results

                selectedTags.isEmpty() -> api.getWallpapers(page, orientation, sortOrder)

                else -> {
                    val tagString = selectedTags.joinToString(" ")
                    api.searchWallpapers(page, tagString, orientation, sortOrder).results
                }
            }
        }

        return wallpapers.filter { it.premium != true }.map {
            Wallpaper(
                imgSrc = it.links.download ?: it.urls?.raw ?: "",
                description = it.alt_description,
                author = it.user?.username,
                url = it.links.html,
                resolution = "${it.width}x${it.height}",
                thumb = it.urls?.small ?: it.urls?.regular ?: it.urls?.full ?: it.urls?.raw,
                creationDate = it.created_at
            )
        }
    }

    override suspend fun getRandomWallpaperUrl(): String? {
        val tagString = selectedTags.joinToString(" ")
        val accessKey = apiKey?.takeIf { it.isNotBlank() }

        val image: UsImage = withReadableErrors {
            if (accessKey != null) {
                officialApi.getRandom(
                    UnsplashOfficial.authorization(accessKey),
                    tagString.ifBlank { null },
                    selectedFilters["orientation"].takeIf { it != "any" }
                )
            } else {
                api.getRandom(tagString)
            }
        }

        return image.links.download ?: image.urls?.raw ?: image.urls?.full
    }

    /**
     * Replace HTTP errors of the official API caused by the access key with an understandable message.
     */
    private inline fun <T> withReadableErrors(block: () -> T): T {
        if (apiKey.isNullOrBlank()) return block()

        return try {
            block()
        } catch (e: HttpException) {
            throw when (e.code()) {
                401 -> Exception("Your Unsplash Access Key is invalid, please log in again from the settings", e)
                403 -> Exception("The rate limit of your Unsplash Access Key has been exceeded, please try again later", e)
                else -> e
            }
        }
    }
}
