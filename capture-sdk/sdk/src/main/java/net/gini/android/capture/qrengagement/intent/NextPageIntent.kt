package net.gini.android.capture.qrengagement.intent

import net.gini.android.capture.qrengagement.QrEngagementSideEffect
import net.gini.android.capture.qrengagement.QrEngagementState
import net.gini.android.capture.qrengagement.host.QrEngagementContainerHost

internal class NextPageIntent {

    fun QrEngagementContainerHost.run() = intent {

        when (state.page) {
            QrEngagementState.Page.EvenScreensGifs -> postSideEffect(QrEngagementSideEffect.Skip)
            QrEngagementState.Page.NotJustQrCodes ->
                reduce { state.copy(page = QrEngagementState.Page.EvenScreensGifs) }

            QrEngagementState.Page.PhotosPdfsMore ->
                reduce { state.copy(page = QrEngagementState.Page.EvenScreensGifs) }
        }
    }
}
