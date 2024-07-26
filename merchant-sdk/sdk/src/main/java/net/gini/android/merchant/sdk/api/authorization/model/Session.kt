package net.gini.android.merchant.sdk.api.authorization.model

import net.gini.android.core.api.authorization.apimodels.SessionToken
import java.util.Date

class Session(
    /** The session's access token.  */
    val accessToken: String,
    /** The expiration date of the access token.  */
    expirationDate: Date
) {
    /** The expiration date of the access token.  */
    val expirationDate: Date = Date(expirationDate.time)

    /**
     * Uses the current locale's time to check whether or not this session has already expired.
     *
     * @return Whether or not the session has already expired.
     */
    fun hasExpired(): Boolean {
        val now = Date()
        return now.after(expirationDate)
    }

    companion object {
        fun fromAPIResponse(apiResponse: SessionToken): Session {
            val accessToken = apiResponse.accessToken
            val now = Date()
            val expirationTime = now.time + apiResponse.expiresIn * 1000
            return Session(accessToken, Date(expirationTime))
        }
    }
}