package net.youapps.wallpaper_apis

import kotlinx.serialization.Serializable

/**
 * The credentials entered by the user for logging in with a username and password,
 * see [WallpaperApi.supportsPasswordLogin].
 *
 * @param instanceUrl the server the account belongs to, only used if [WallpaperApi.requiresInstanceForLogin]
 * @param twoFactorToken the current two factor authentication code, if the account requires one
 */
data class LoginCredentials(
    val username: String,
    val password: String,
    val instanceUrl: String? = null,
    val twoFactorToken: String? = null
)

/**
 * A session of a user that logged in with their username and password.
 *
 * @param token the access token used to authenticate requests
 * @param username the name of the logged in user, used for displaying only
 * @param instanceUrl the server the account belongs to, if [WallpaperApi.requiresInstanceForLogin]
 */
@Serializable
data class LoginSession(
    val token: String,
    val username: String,
    val instanceUrl: String? = null
) {
    /**
     * The name of the account including its instance, e.g. `user@lemmy.world`.
     */
    val accountName: String
        get() {
            val host = instanceUrl
                ?.substringAfter("://")
                ?.substringBefore("/")
                ?.takeIf { it.isNotBlank() }

            return if (host != null) "$username@$host" else username
        }
}

/**
 * Thrown if logging in failed for a known [reason], e.g. because of a wrong password.
 */
class LoginException(
    val reason: Reason,
    message: String? = null
) : Exception(message ?: reason.name) {
    enum class Reason {
        INVALID_CREDENTIALS,
        TWO_FACTOR_REQUIRED,
        INVALID_TWO_FACTOR_TOKEN,
        EMAIL_NOT_VERIFIED,
        ACCOUNT_NOT_APPROVED,
        INVALID_INSTANCE,
        OTHER
    }
}
