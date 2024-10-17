package net.gini.android.core.api.response

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DocumentPageResponse(
    val pageNumber: Int,
    val images: Images
) {

    @JsonClass(generateAdapter = true)
    data class Images(
        val medium: String?,
        val large: String?,
    )
}