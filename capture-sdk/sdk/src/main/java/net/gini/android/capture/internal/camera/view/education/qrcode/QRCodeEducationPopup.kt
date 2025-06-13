package net.gini.android.capture.internal.camera.view.education.qrcode

import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible
import net.gini.android.capture.internal.qreducation.model.QrEducationType

/**
 * Internal use only.
 *
 * @suppress
 */
internal class QRCodeEducationPopup<T> @JvmOverloads constructor(
    private val popupView: ComposeView,
) {

    var qrCodeContent: T? = null
        private set

    private var isShown = false
        private set

    fun show(type: QrEducationType, onComplete: () -> Unit) {
        popupView.invalidate()
        popupView.setContent {
            QrCodeEducationPopupContent(
                qrEducationType = type,
                onComplete = onComplete
            )
        }

        if (isShown) {
            return
        }
        show()
        this.qrCodeContent = qrCodeContent
    }

    private fun show() {
        showViews()
    }

    fun hide() {
        qrCodeContent = null
        hideViews()
    }

    private fun showViews() {
        popupView.isVisible = true
    }

    private fun hideViews() {
        popupView.isVisible = false
        isShown = false
    }
}
