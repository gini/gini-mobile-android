package net.gini.android.core.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents the error response from the Gini API v5.0.
 *
 * @property items Array of error items containing details about individual errors
 * @property requestId Unique ID identifying the request (provide when contacting support)
 * @property message Optional human consumable error description (not intended for end-users)
 */
@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @Json(name = "items") val items: List<ErrorItem>? = null,
    @Json(name = "requestId") val requestId: String? = null,
    @Json(name = "message") val message: String? = null
)

/**
 * Represents an individual error item within the error response.
 *
 * @property code A short error code identifying the error type
 * @property message Optional human consumable error description (not intended for end-users)
 * @property affectedIds Optional list of entity IDs related to the error (e.g., document IDs or payment request IDs for bulk operations)
 */
@JsonClass(generateAdapter = true)
data class ErrorItem(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String? = null,
    @Json(name = "object") val affectedIds: List<String>? = null
)

