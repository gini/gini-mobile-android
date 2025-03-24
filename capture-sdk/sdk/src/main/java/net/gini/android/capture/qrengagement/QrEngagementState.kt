package net.gini.android.capture.qrengagement

data class QrEngagementState(
    val page: Page = Page.NotJustQrCodes,
) {
    sealed interface Page {
        data object NotJustQrCodes : Page
        data object PhotosPdfsMore : Page
        data object EvenScreensGifs : Page
    }
}
