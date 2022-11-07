package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Holds information required for requesting information about a user.
 */
@JsonClass(generateAdapter = true)
data class UserRequestModel (
    val email: String? = null,
    val password: String? = null,
    val oldEmail: String? = null
)
