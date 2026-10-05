package net.gini.android.capture.internal.camera.view.education.qrcode

import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.qrcode.PaymentQRCodeData
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Pins the contract `CameraFragmentImpl.restorePoweredByGiniForEducationStep` depends on: a popup
 * that has never been shown must report so, even though its ComposeView is visible.
 *
 * `gc_qr_code_education_compose_view` carries no `android:visibility` in any camera layout
 * variant, so it is `VISIBLE` from inflation and only empty. Deriving [QRCodeEducationPopup
 * .isShowing] from that view made every camera start look like a running education step, which put
 * the Gini ingredient brand element over the live preview with a usable shutter — the state R13
 * forbids.
 */
@RunWith(RobolectricTestRunner::class)
class QRCodeEducationPopupTest {

    private fun newPopup(): Pair<QRCodeEducationPopup<PaymentQRCodeData>, ComposeView> {
        val view = ComposeView(ApplicationProvider.getApplicationContext())
        return QRCodeEducationPopup<PaymentQRCodeData>(view) to view
    }

    @Test
    fun `a popup that was never shown does not report itself as showing`() {
        val (popup, view) = newPopup()

        // The layouts inflate this view visible and empty, which is the trap being guarded here.
        assertThat(view.visibility).isEqualTo(android.view.View.VISIBLE)
        assertThat(popup.isShowing()).isFalse()
    }

    @Test
    fun `hiding leaves the popup reporting not showing`() {
        val (popup, _) = newPopup()

        popup.hide()

        assertThat(popup.isShowing()).isFalse()
    }
}
