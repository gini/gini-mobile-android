package net.gini.android.capture.network.model

import net.gini.android.core.api.models.DocumentPage


fun DocumentPage.toCaptureDocumentPages() =
    net.gini.android.capture.internal.network.model.DocumentPage(
        pageNumber = pageNumber,
        images = images.toCaptureImage(),
    )

fun DocumentPage.Images.toCaptureImage() =
    net.gini.android.capture.internal.network.model.DocumentPage.Images(
        medium = medium,
        large = large,
    )
