package net.gini.android.health.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeleteDocumentErrorResponse(
    @Json(name = "notFoundDocuments") val notFoundDocuments: List<String>? = null,
    @Json(name = "unauthorizedDocuments") val unauthorizedDocuments: List<String>? = null,
    @Json(name = "missingCompositeDocuments") val missingCompositeDocuments: List<String>? = null,
    @Json(name = "message") val message: String? = null
)