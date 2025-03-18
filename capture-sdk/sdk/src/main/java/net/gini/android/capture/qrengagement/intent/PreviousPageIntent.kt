package net.gini.android.capture.qrengagement.intent

import net.gini.android.capture.qrengagement.QrEngagementState
import net.gini.android.capture.qrengagement.host.QrEngagementContainerHost

internal class PreviousPageIntent {

    fun QrEngagementContainerHost.run() = intent {
        when (state.page) {
            QrEngagementState.Page.EvenScreensGifs ->
                reduce { state.copy(page = QrEngagementState.Page.PhotosPdfsMore) }

            QrEngagementState.Page.NotJustQrCodes -> { /* no-op */
            }

            QrEngagementState.Page.PhotosPdfsMore ->
                reduce { state.copy(page = QrEngagementState.Page.NotJustQrCodes) }
        }
    }
}
