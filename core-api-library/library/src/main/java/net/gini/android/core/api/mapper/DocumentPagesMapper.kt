package net.gini.android.core.api.mapper

import net.gini.android.core.api.models.DocumentPage
import net.gini.android.core.api.response.DocumentPageResponse


fun DocumentPageResponse.toDocumentPage() = DocumentPage(
    pageNumber = pageNumber,
    images = images.toImages()
)

fun DocumentPageResponse.Images.toImages() = DocumentPage.Images(
    medium = medium,
    large = large
)