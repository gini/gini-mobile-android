package net.gini.android.core.api.authorization.apimodels

import com.squareup.moshi.Json
import java.util.*

class SessionToken (
    @field:Json(name = "user_name")
    val userName: String? = null,
    @field:Json(name = "access_token")
    val accessToken: String? = null,
    @field:Json(name = "token_type")
    val tokenType: String? = null,
    @field:Json(name = "expires_in")
    val expiresIn: Long? = null,
    val scope: String? = null,
    var expirationDate: Date? = null
) {

    // TODO: check if this calculation is needed when new structure is connected to backend
//    init {
//        expirationDate = Date(Date().time + (expiresIn?.times(1000) ?: 0))
//    }

    fun hasExpired(): Boolean {
        val now = Date()
        return now.after(expirationDate)
    }
}
