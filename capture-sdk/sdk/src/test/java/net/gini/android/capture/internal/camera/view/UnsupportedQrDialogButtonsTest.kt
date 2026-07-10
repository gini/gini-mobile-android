package net.gini.android.capture.internal.camera.view

import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import net.gini.android.capture.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Internal use only
 *
 * Verifies that the unsupported QR code dialog buttons allow their labels to wrap
 * onto multiple lines. The Material dialog button style (buttonBarButtonStyle) sets
 * android:lines="1" which forces labels to be ellipsized at large font scales
 * (accessibility issue) unless it is overridden after inflation.
 */
@RunWith(RobolectricTestRunner::class)
class UnsupportedQrDialogButtonsTest {

    private fun inflateDialogButtons(): List<Button> {
        val themedContext = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.GiniCaptureTheme
        )
        val dialogView = LayoutInflater.from(themedContext)
            .inflate(R.layout.gc_dialog_unsupported_qr_code, null)
        return listOf(
            dialogView.findViewById<Button>(R.id.gc_btn_scan_another_qr_code),
            dialogView.findViewById<Button>(R.id.gc_btn_capture_document)
        ).map { QRCodePopup.allowMultilineLabel(it) }
    }

    @Test
    fun `dialog button labels can wrap to multiple lines`() {
        inflateDialogButtons().forEach { button ->
            assertTrue(
                "button ${button.id} must allow more than one line, but maxLines is ${button.maxLines}",
                button.maxLines > 1
            )
            assertFalse(
                "button ${button.id} must not force an exact line count",
                button.minLines > 1
            )
        }
    }
}
