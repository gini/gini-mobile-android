package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
class SessionTokenInfo (
    @field:Json(name = "user_name")
    val userName: String
)
