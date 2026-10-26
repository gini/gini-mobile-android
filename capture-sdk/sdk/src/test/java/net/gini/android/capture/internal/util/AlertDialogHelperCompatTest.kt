package net.gini.android.capture.internal.util

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.colors.CustomResourceProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

/**
 * Tests that dialogs shown by [AlertDialogHelperCompat] use the provider colors.
 */
@RunWith(RobolectricTestRunner::class)
class AlertDialogHelperCompatTest {

    private lateinit var activity: AppCompatActivity

    @Before
    fun setUp() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(R.style.GiniCaptureTheme)
        activity = controller.setup().get()
    }

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `dialog buttons use the provider accent color`() {
        configure { name, _ -> if (name == "gc_accent_01") ACCENT else null }

        val dialog = showDialog()

        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).currentTextColor).isEqualTo(ACCENT)
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).currentTextColor).isEqualTo(ACCENT)
    }

    @Test
    fun `without a provider dialog buttons keep the resource accent color`() {
        configure(null)

        val dialog = showDialog()

        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).currentTextColor)
            .isEqualTo(activity.getColor(R.color.gc_accent_01))
    }

    private fun showDialog(): AlertDialog {
        AlertDialogHelperCompat.showAlertDialog(activity, "message", "ok", { _, _ -> }, "cancel", { _, _ -> }, null)
        return ShadowDialog.getLatestDialog() as AlertDialog
    }

    private fun configure(provider: CustomResourceProvider?) {
        val builder = GiniCapture.newInstance(activity).setGiniCaptureNetworkService(mockk(relaxed = true))
        provider?.let { builder.setCustomResourceProvider(it) }
        builder.build()
    }

    private companion object {
        const val ACCENT = 0xFFAA0011.toInt()
    }
}
