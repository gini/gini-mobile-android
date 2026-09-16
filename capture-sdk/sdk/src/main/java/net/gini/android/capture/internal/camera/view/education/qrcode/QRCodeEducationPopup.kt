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
     * Whether this popup has been shown and not hidden again.
     *
     * Deliberately *not* read from `popupView.isVisible`: the ComposeView carries no
     * `android:visibility` in any `gc_fragment_camera.xml` variant, so it is `VISIBLE` from
     * inflation and merely empty until [show] gives it content. Asking the view would therefore
     * answer "yes" on a camera screen where no education overlay has ever run.
     *
     * [isShown] has the lifetime this needs. `CameraFragmentImpl.createPopups` builds a new popup
     * for every new view, so a screen rebuilt after navigating away starts `false`, while a screen
     * merely stopped and restarted keeps the same instance and stays `true`.
     * `CameraFragmentImpl.onStart` uses this to decide whether the ingredient brand element
     * belongs back on screen.
     */
    fun isShowing(): Boolean = isShown

    private fun showViews() {
        popupView.isVisible = true
        isShown = true
    }

    private fun hideViews() {
        popupView.isVisible = false
        isShown = false
    }
}
