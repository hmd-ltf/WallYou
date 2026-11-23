package net.youapps.wallpaper_apis.wc

import net.youapps.wallpaper_apis.wc.obj.WallpaperCaveMoreResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WallpaperCave {
    /**
     * An album (collection) of wallpapers, e.g. `forest-wallpapers`.
     */
    @GET("/{album}")
    suspend fun getAlbum(@Path("album") album: String): ResponseBody

    /**
     * Search for albums, the results are listed as HTML.
     */
    @GET("/search")
    suspend fun search(@Query("q") query: String): ResponseBody

    @POST("/morefeatured")
    @Multipart
    suspend fun getFeaturedWallpapers(
        @Part("page") page: Int
    ): List<WallpaperCaveMoreResponse>

    @POST("/morelatest")
    @Multipart
    suspend fun getLatestWallpapers(
        @Part("page") page: Int
    ): List<WallpaperCaveMoreResponse>
}