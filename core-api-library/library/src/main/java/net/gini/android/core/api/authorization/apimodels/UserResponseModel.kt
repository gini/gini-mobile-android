package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.JsonClass

/**
 * Holds information about a user info response.
 */
@JsonClass(generateAdapter = true)
data class UserResponseModel (
    val id: String? = null,
    val email: String? = null
)
