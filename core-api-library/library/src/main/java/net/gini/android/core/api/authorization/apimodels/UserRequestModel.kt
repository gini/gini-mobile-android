package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json

data class UserRequestModel (
    val username: String? = null,
    val password: String? = null,
    val oldEmail: String? = null,
    @field:Json(name = "email")
    val newEmail: String? = null
)
