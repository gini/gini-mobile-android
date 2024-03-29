package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Holds information about a session token.
 */
@JsonClass(generateAdapter = true)
data class SessionToken (
    @field:Json(name = "access_token")
    val accessToken: String,
    @field:Json(name = "token_type")
    val tokenType: String,
    @field:Json(name = "expires_in")
    val expiresIn: Long,
    val scope: String? = null
)
