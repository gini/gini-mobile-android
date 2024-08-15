package net.gini.android.merchant.sdk.api.authorization.model

import java.util.Date

/**
 * The session is the value object for the session of a user.
 */
class Session(
    /** The session's access token.  */
    val accessToken: String,
    /** The expiration date of the access token.  */
    expirationDate: Date
) {
    /** The expiration date of the access token.  */
    val expirationDate: Date = Date(expirationDate.time)
}