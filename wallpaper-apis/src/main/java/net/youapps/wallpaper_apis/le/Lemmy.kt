package net.youapps.wallpaper_apis.le

import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.le.obj.LemmyLoginRequest
import net.youapps.wallpaper_apis.le.obj.LemmyLoginResponse
import net.youapps.wallpaper_apis.le.obj.LemmyResponse
import net.youapps.wallpaper_apis.le.obj.LemmySiteResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface Lemmy {
    @GET("api/v3/post/list")
    suspend fun getWallpapers(
        @Query("page") page: Int,
        @Query("community_name") communityName: String?,
        @Query("limit") limit: Int = 10,
        @Query("type_") type: String = "All",
        @Query("sort") sort: String = "Active",
        @Header(AUTHORIZATION_HEADER) authorization: String? = null
    ): LemmyResponse

    /**
     * Search posts, see https://join-lemmy.org/api/main#operation/search.
     */
    @GET("api/v3/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("community_name") communityName: String?,
        @Query("limit") limit: Int = 10,
        @Query("type_") type: String = "Posts",
        @Query("listing_type") listingType: String = "All",
        @Query("sort") sort: String = "Active",
        @Header(AUTHORIZATION_HEADER) authorization: String? = null
    ): LemmyResponse

    @Headers("${RetrofitHelper.NO_LOGGING_HEADER}: 1")
    @POST("api/v3/user/login")
    suspend fun login(
        @Body body: LemmyLoginRequest
    ): Response<LemmyLoginResponse>

    /**
     * Get information about the instance and the logged in user, see [LemmySiteResponse.myUser].
     */
    @Headers("${RetrofitHelper.NO_LOGGING_HEADER}: 1")
    @GET("api/v3/site")
    suspend fun getSite(
        @Header(AUTHORIZATION_HEADER) authorization: String
    ): Response<LemmySiteResponse>

    @POST("api/v3/user/logout")
    suspend fun logout(
        @Header(AUTHORIZATION_HEADER) authorization: String
    ): Response<ResponseBody>

    companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
