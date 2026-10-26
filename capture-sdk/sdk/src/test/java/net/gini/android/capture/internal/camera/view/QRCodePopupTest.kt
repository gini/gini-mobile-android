package net.gini.android.capture.internal.camera.view

import android.content.res.Resources
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.FragmentImplCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * Tests for the unsupported QR code dialog shown by [QRCodePopup].
 */
@RunWith(RobolectricTestRunner::class)
class QRCodePopupTest {

    private lateinit var activity: AppCompatActivity
    private lateinit var popupView: ConstraintLayout

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.GiniCaptureTheme)
        activity = controller.setup().get()
        popupView = ConstraintLayout(activity).apply {
            addView(TextView(activity).apply { id = R.id.gc_qr_code_status })
            addView(ImageView(activity).apply { id = R.id.gc_camera_frame })
            addView(ImageView(activity).apply { id = R.id.gc_qr_code_check })
            addView(TextView(activity).apply { id = R.id.gc_retrieving_invoice })
            addView(ConstraintLayout(activity).apply { id = R.id.gc_unknown_qr_wrapper })
        }
        activity.setContentView(popupView)
    }

    @Test
    @Config(qualifiers = "w800dp-h400dp-land")
    fun `unsupported QR dialog is 55 percent of the screen width in landscape instead of the Material 65 percent`() {
        val dialog = showUnsupportedQrDialog()

        val decorView = measureDecorView(dialog)

        val screenWidth = activity.resources.displayMetrics.widthPixels
        val expectedWidth = activity.resources
            .getFraction(R.dimen.gc_unsupported_qr_code_dialog_min_width, screenWidth, screenWidth)
            .toInt()
        assertThat(decorView.measuredWidth).isEqualTo(expectedWidth)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xlarge-land")
    fun `unsupported QR dialog keeps the AppCompat 45 percent minimum on extra-large tablets in landscape`() {
        val dialog = showUnsupportedQrDialog()

        val decorView = measureDecorView(dialog)

        val screenWidth = activity.resources.displayMetrics.widthPixels
        assertThat(decorView.measuredWidth).isEqualTo((screenWidth * 0.45f).toInt())
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-port")
    fun `unsupported QR dialog keeps the Material 95 percent minimum in portrait`() {
        val dialog = showUnsupportedQrDialog()

        val decorView = measureDecorView(dialog)

        val screenWidth = activity.resources.displayMetrics.widthPixels
        assertThat(decorView.measuredWidth).isAtLeast((screenWidth * 0.95f).toInt())
    }

    private fun showUnsupportedQrDialog(): AlertDialog {
        val callback = mockk<FragmentImplCallback>(relaxed = true)
        every { callback.view } returns popupView
        val popup = QRCodePopup<String>(
            fragmentImplCallback = callback,
            popupView = popupView,
            loadingIndicatorContainer = null,
            hideDelayMs = 1000L,
            supported = false,
            isNewWarningEnabled = { true }
        )
        popup.show("not-a-payment-qr-code")
        shadowOf(Looper.getMainLooper()).idle()
        return ShadowDialog.getLatestDialog() as AlertDialog
    }

    /**
     * Measures like ViewRootImpl: AT_MOST the framework's preferred dialog width (320dp on phones).
     * Only the theme's window minimum width can grow the window past that first pass.
     */
    private fun measureDecorView(dialog: AlertDialog): View {
        val metrics = activity.resources.displayMetrics
        val preferredWidthId = Resources.getSystem().getIdentifier("config_prefDialogWidth", "dimen", "android")
        val firstPassWidth = minOf(metrics.widthPixels, Resources.getSystem().getDimensionPixelSize(preferredWidthId))
        return dialog.window!!.decorView.apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(firstPassWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.AT_MOST)
            )
        }
    }
}
