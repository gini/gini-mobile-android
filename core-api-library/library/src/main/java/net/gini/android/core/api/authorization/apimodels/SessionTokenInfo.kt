package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Holds information about a session token info response.
 */
@JsonClass(generateAdapter = true)
data class SessionTokenInfo (
    @field:Json(name = "user_name")
    val userName: String
)
