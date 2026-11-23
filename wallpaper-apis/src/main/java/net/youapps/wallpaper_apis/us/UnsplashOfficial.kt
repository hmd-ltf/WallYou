package net.youapps.wallpaper_apis.us

import net.youapps.wallpaper_apis.us.obj.UsImage
import net.youapps.wallpaper_apis.us.obj.UsSearch
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * The official Unsplash API, which requires the Access Key of an Unsplash developer application.
 *
 * See https://unsplash.com/documentation
 */
interface UnsplashOfficial {
    @Headers(ACCEPT_VERSION_HEADER)
    @GET("photos")
    suspend fun getWallpapers(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Query("page") page: Int,
        @Query("order_by") order: String,
        @Query("per_page") perPage: Int = PER_PAGE
    ): List<UsImage>

    @Headers(ACCEPT_VERSION_HEADER)
    @GET("search/photos")
    suspend fun searchWallpapers(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Query("page") page: Int,
        @Query("query") query: String,
        @Query("orientation") orientation: String?,
        @Query("order_by") order: String,
        @Query("per_page") perPage: Int = PER_PAGE
    ): UsSearch

    @Headers(ACCEPT_VERSION_HEADER)
    @GET("photos/random")
    suspend fun getRandom(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Query("query") query: String?,
        @Query("orientation") orientation: String?
    ): UsImage

    /**
     * Load a single photo to check whether the given access key is valid. Fails with 401 if it's invalid.
     */
    @Headers(ACCEPT_VERSION_HEADER)
    @GET("photos")
    suspend fun checkAccessKey(
        @Header(AUTHORIZATION_HEADER) authorization: String,
        @Query("per_page") perPage: Int = 1
    ): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://api.unsplash.com/"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val ACCEPT_VERSION_HEADER = "Accept-Version: v1"
        private const val PER_PAGE = 20

        fun authorization(accessKey: String) = "Client-ID ${accessKey.trim()}"
    }
}
