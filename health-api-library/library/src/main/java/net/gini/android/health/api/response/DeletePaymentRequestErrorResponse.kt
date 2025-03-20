package net.gini.android.health.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeletePaymentRequestErrorResponse (
    @Json(name = "unauthorizedPaymentRequests") val unauthorizedPaymentRequests: List<String>? = null,
    @Json(name = "notFoundPaymentRequests") val notFoundPaymentRequests: List<String>? = null,
    @Json(name = "message") val message: String? = null
)