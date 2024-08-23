package net.gini.android.core.api.models


data class DocumentPage(
    val pageNumber: Int,
    val images: Images
) {
    data class Images(
        val medium: String?,
        val large: String?,
    )
}