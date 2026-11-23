package net.youapps.wallpaper_apis.re

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface Reddit {
    /**
     * Search the posts of a subreddit, see https://www.reddit.com/dev/api/#GET_search.
     */
    @GET("r/{subreddit}/search.rss")
    suspend fun search(
        @Path("subreddit") subreddit: String,
        @Query("q") query: String,
        @Query("sort") sort: String,
        @Query("t") time: String? = null,
        @Query("after") after: String? = null,
        @Query("restrict_sr") restrictToSubreddit: String = "on"
    ): ResponseBody

    @GET("r/{subreddit}/{sort}.rss")
    suspend fun getRedditData(
        @Path("subreddit") subreddit: String,
        @Path("sort") sort: String,
        @Query("t") time: String? = null,
        @Query("after") after: String? = null
    ): ResponseBody
}
