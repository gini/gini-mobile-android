package net.gini.android.capture.internal.network.model

data class DocumentPage(
    val pageNumber: Int,
    val images: Images
) {
    data class Images(
        val medium: String?,
        val large: String?,
    )

    fun getSmallestImage() = images.medium ?: images.large
}
