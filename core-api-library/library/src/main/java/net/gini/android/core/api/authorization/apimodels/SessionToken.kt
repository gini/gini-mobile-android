package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
class SessionToken (
    @field:Json(name = "access_token")
    val accessToken: String,
    @field:Json(name = "token_type")
    val tokenType: String,
    @field:Json(name = "expires_in")
    val expiresIn: Long,
    val scope: String? = null,
    @field:Json(ignore = true)
    var expirationDate: Date? = null
) {

    init {
        expirationDate = Date(Date().time + (expiresIn * 1000))
    }

    fun hasExpired(): Boolean {
        val now = Date()
        return now.after(expirationDate)
    }
}
