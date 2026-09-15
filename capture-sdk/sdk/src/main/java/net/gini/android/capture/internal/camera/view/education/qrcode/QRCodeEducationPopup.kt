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

    /**
     * Whether the education overlay is currently on screen.
     *
     * Derived from the view rather than from [isShown], because the overlay's visibility is the
     * only thing that survives a stop/start cycle correctly: the view is not recreated when the
     * app is merely backgrounded, but it *is* recreated — back to its `GONE` default — when the
     * user navigates away and returns. `CameraFragmentImpl.onStart` uses this to decide whether
     * the ingredient brand element belongs back on screen.
     */
    fun isShowing(): Boolean = popupView.isVisible

    private fun showViews() {
        popupView.isVisible = true
    }

    private fun hideViews() {
        popupView.isVisible = false
        isShown = false
    }
}
