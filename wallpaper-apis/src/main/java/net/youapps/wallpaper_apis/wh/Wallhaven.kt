package net.youapps.wallpaper_apis.wh

import net.youapps.wallpaper_apis.wh.obj.WhSearchResponse
import net.youapps.wallpaper_apis.wh.obj.WhSettingsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface Wallhaven {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("categories") categories: String,
        @Query("order") order: String,
        @Query("purity") purity: String,
        @Query("sorting") sorting: String,
        @Query("topRange") topRange: String? = null,
        @Query("ratios") ratios: String? = null,
        @Query("resolutions") resolutions: String? = null,
        @Query("atleast") atLeast: String? = null,
        @Query("colors") colors: String? = null,
        @Query("seed") seed: String? = null,
        @Header(API_KEY_HEADER) apiKey: String? = null
    ): WhSearchResponse

    /**
     * Get the settings of the user that owns the given API key. Fails with 401 if the API key is invalid.
     */
    @GET("settings")
    suspend fun getUserSettings(
        @Header(API_KEY_HEADER) apiKey: String
    ): Response<WhSettingsResponse>

    companion object {
        const val API_KEY_HEADER = "X-API-Key"
    }
}
