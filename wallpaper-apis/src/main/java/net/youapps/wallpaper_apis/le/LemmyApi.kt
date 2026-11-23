package net.youapps.wallpaper_apis.le

import net.youapps.wallpaper_apis.LoginCredentials
import net.youapps.wallpaper_apis.LoginException
import net.youapps.wallpaper_apis.LoginSession
import net.youapps.wallpaper_apis.RetrofitHelper
import net.youapps.wallpaper_apis.TextUtils
import net.youapps.wallpaper_apis.Wallpaper
import net.youapps.wallpaper_apis.WallpaperApi
import net.youapps.wallpaper_apis.le.obj.LemmyErrorResponse
import net.youapps.wallpaper_apis.le.obj.LemmyLoginRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

class LemmyApi : WallpaperApi() {
    override val name = "Lemmy"
    override val baseUrl = "https://lemmy.ml"
    override val requiresCommunityName = true

    override val supportsPasswordLogin: Boolean = true
    override val requiresInstanceForLogin: Boolean = true

    override val availableFilters: Map<String, List<String>>
        get() = mapOf(
            "sort" to listOf(
                "Active",
                "Hot",
                "New",
                "Old",
                "TopDay",
                "TopWeek",
                "TopMonth",
                "TopYear",
                "TopAll",
                "MostComments",
                "NewComments",
                "TopHour",
                "TopSixHour",
                "TopTwelveHour",
                "TopThreeMonths",
                "TopSixMonths",
                "TopNineMonths"
            ),
            // the feed of subscribed communities is only available when logged in
            "type" to if (loginSession == null) TYPE_OPTIONS else TYPE_OPTIONS + SUBSCRIBED
        )

    override var communityName: String? = "pics@lemmy.world"

    private val api = RetrofitHelper.create<Lemmy>(baseUrl)

    /**
     * API clients for the instances of logged in users, mapped by their instance URL.
     */
    private val instanceApis = mutableMapOf<String, Lemmy>()

    private fun getInstanceApi(instanceUrl: String?): Lemmy {
        if (instanceUrl == null) return api

        return synchronized(instanceApis) {
            instanceApis.getOrPut(instanceUrl) { RetrofitHelper.create<Lemmy>(instanceUrl) }
        }
    }

    override suspend fun getWallpapers(page: Int): List<Wallpaper> {
        val session = loginSession
        val type = selectedFilters["type"]!!
        // the subscribed feed contains the posts of all subscribed communities instead of a single one
        val isSubscribedFeed = type == SUBSCRIBED && session != null

        val response = try {
            // logged in users have to load the posts from their own instance, as the token is only valid there
            getInstanceApi(session?.instanceUrl).getWallpapers(
                page = page,
                communityName = if (isSubscribedFeed) {
                    null
                } else {
                    communityName ?: throw IllegalArgumentException("no community name specified")
                },
                sort = selectedFilters["sort"]!!,
                type = type,
                authorization = session?.let { bearer(it.token) }
            )
        } catch (e: HttpException) {
            if (session != null && e.code() == 401) {
                throw Exception("Your Lemmy session has expired, please log in again from the settings")
            }
            throw e
        }

        return response.posts.filter {
            !it.post.thumbnailUrl.isNullOrEmpty()
        }.map {
            val imgUrl = extractImageUrl(it.post.thumbnailUrl!!)

            Wallpaper(
                imgSrc = imgUrl,
                thumb = "$imgUrl?format=jpg&thumbnail=1080",
                title = it.post.name,
                description = it.post.body?.let { text -> TextUtils.removeMarkdownSymbols(text) },
                url = it.post.postUrl,
                author = it.creator.name,
                creationDate = it.post.published
            )
        }
    }

    override suspend fun logIn(credentials: LoginCredentials): LoginSession {
        val instanceUrl = normalizeInstanceUrl(credentials.instanceUrl.orEmpty())
        val instanceApi = getInstanceApi(instanceUrl)

        val response = instanceApi.login(
            LemmyLoginRequest(
                usernameOrEmail = credentials.username.trim(),
                password = credentials.password,
                totp2faToken = credentials.twoFactorToken?.trim()?.takeIf { it.isNotEmpty() }
            )
        )

        if (!response.isSuccessful) {
            val error = response.errorBody()?.string()?.let { body ->
                runCatching { RetrofitHelper.json.decodeFromString<LemmyErrorResponse>(body) }.getOrNull()
            }
            throw toLoginException(response.code(), response.message(), error?.error ?: error?.message)
        }

        val loginResponse = response.body()
        val token = loginResponse?.jwt
        if (token.isNullOrBlank()) {
            throw when {
                loginResponse?.verifyEmailSent == true -> LoginException(LoginException.Reason.EMAIL_NOT_VERIFIED)
                loginResponse?.registrationCreated == true -> LoginException(LoginException.Reason.ACCOUNT_NOT_APPROVED)
                else -> LoginException(LoginException.Reason.OTHER, "The server didn't return a session token")
            }
        }

        // users may log in with their email, so look up the actual username for displaying it
        val username = runCatching {
            instanceApi.getSite(bearer(token)).body()?.myUser?.localUserView?.person?.name
        }.getOrNull() ?: credentials.username.trim()

        return LoginSession(token = token, username = username, instanceUrl = instanceUrl)
    }

    override suspend fun logOut(session: LoginSession) {
        // older Lemmy versions don't support logging out, the token is discarded locally anyway
        runCatching {
            getInstanceApi(session.instanceUrl).logout(bearer(session.token))
        }
    }

    private fun bearer(token: String) = "Bearer $token"

    /**
     * Convert user input like `lemmy.world` or `https://lemmy.world/c/pics` to the base URL of the instance,
     * e.g. `https://lemmy.world/`.
     */
    private fun normalizeInstanceUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) throw LoginException(LoginException.Reason.INVALID_INSTANCE)

        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val url = withScheme.toHttpUrlOrNull()
            ?.takeIf { it.host.contains(".") || it.host == "localhost" }
            ?: throw LoginException(LoginException.Reason.INVALID_INSTANCE)

        return url.newBuilder()
            .encodedPath("/")
            .query(null)
            .fragment(null)
            .build()
            .toString()
    }

    private fun toLoginException(code: Int, message: String, error: String?): Exception {
        val reason = when (error) {
            "incorrect_login", "password_incorrect", "couldnt_find_that_username_or_email" ->
                LoginException.Reason.INVALID_CREDENTIALS

            "missing_totp_token" -> LoginException.Reason.TWO_FACTOR_REQUIRED
            "incorrect_totp_token" -> LoginException.Reason.INVALID_TWO_FACTOR_TOKEN
            "email_not_verified" -> LoginException.Reason.EMAIL_NOT_VERIFIED
            "registration_application_is_pending", "registration_denied" ->
                LoginException.Reason.ACCOUNT_NOT_APPROVED

            else -> null
        }
        if (reason != null) return LoginException(reason)

        // e.g. the instance isn't a Lemmy instance at all
        if (code == 404) return LoginException(LoginException.Reason.INVALID_INSTANCE)

        return LoginException(
            LoginException.Reason.OTHER,
            listOfNotNull("HTTP $code", error ?: message.takeIf { it.isNotBlank() }).joinToString(": ")
        )
    }

    private fun extractImageUrl(plainUrl: String): String {
        val url = plainUrl.toHttpUrlOrNull() ?: return plainUrl

        // proxied urls: https://lemmy.local/api/v3/image_proxy?url=...
        if (!url.queryParameter("url").isNullOrEmpty()) {
            return url.queryParameter("url")!!
        }

        return plainUrl
    }

    override suspend fun getRandomWallpaperUrl(): String? = getWallpapers(1).randomOrNull()?.imgSrc

    companion object {
        private const val SUBSCRIBED = "Subscribed"
        private val TYPE_OPTIONS = listOf("All", "Local")
    }
}
