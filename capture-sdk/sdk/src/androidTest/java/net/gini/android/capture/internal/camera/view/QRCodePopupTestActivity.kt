package net.gini.android.capture.internal.camera.view

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.FragmentImplCallback

/**
 * Hosts a [QRCodePopup] on a real window for [QRCodePopupInstrumentedTest]. Declared in the
 * androidTest manifest with the GiniCaptureTheme and a fixed landscape orientation.
 */
class QRCodePopupTestActivity : AppCompatActivity() {

    private lateinit var popupView: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        popupView = ConstraintLayout(this).apply {
            addView(TextView(context).apply { id = R.id.gc_qr_code_status })
            addView(ImageView(context).apply { id = R.id.gc_camera_frame })
            addView(ImageView(context).apply { id = R.id.gc_qr_code_check })
            addView(TextView(context).apply { id = R.id.gc_retrieving_invoice })
            addView(ConstraintLayout(context).apply { id = R.id.gc_unknown_qr_wrapper })
        }
        setContentView(popupView)
    }

    fun showUnsupportedQrDialog() {
        val callback = mock<FragmentImplCallback> { on { view } doReturn popupView }
        QRCodePopup<String>(
            fragmentImplCallback = callback,
            popupView = popupView,
            loadingIndicatorContainer = null,
            hideDelayMs = 1000L,
            supported = false,
            isNewWarningEnabled = { true }
        ).show("not-a-payment-qr-code")
    }
}
