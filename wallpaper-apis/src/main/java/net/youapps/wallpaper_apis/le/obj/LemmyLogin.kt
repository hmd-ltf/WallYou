package net.youapps.wallpaper_apis.le.obj

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LemmyLoginRequest(
    @SerialName("username_or_email") val usernameOrEmail: String,
    val password: String,
    @SerialName("totp_2fa_token") val totp2faToken: String? = null
)

@Serializable
data class LemmyLoginResponse(
    // null if the email isn't verified yet or the registration application is still pending
    val jwt: String? = null,
    @SerialName("registration_created") val registrationCreated: Boolean = false,
    @SerialName("verify_email_sent") val verifyEmailSent: Boolean = false
)

@Serializable
data class LemmyErrorResponse(
    val error: String? = null,
    val message: String? = null
)

@Serializable
data class LemmySiteResponse(
    // only present if the request was authenticated with a valid token
    @SerialName("my_user") val myUser: LemmyMyUser? = null
)

@Serializable
data class LemmyMyUser(
    @SerialName("local_user_view") val localUserView: LemmyLocalUserView? = null
)

@Serializable
data class LemmyLocalUserView(
    val person: LemmyPerson? = null
)

@Serializable
data class LemmyPerson(
    val name: String? = null
)
