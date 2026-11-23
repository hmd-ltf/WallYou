package net.youapps.wallpaper_apis.bi

import net.youapps.wallpaper_apis.bi.obj.BingImageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface Bing {
    @GET("HPImageArchive.aspx")
    suspend fun getImages(
        // limit must be <= 7
        @Query("n") limit: Int = 8,
        // how many days ago the newest image was shown, at most 7
        @Query("idx") index: Int = 0,
        @Query("format") format: String = "js"
    ): BingImageResponse
}
