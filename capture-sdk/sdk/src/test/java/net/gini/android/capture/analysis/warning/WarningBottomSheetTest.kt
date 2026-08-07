package net.gini.android.capture.analysis.warning

import android.app.Dialog
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import net.gini.android.capture.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class WarningBottomSheetTest {

    @Test
    fun `builds title with format argument for PAYMENT_DUE_DATE`() {
        launchSheet(WarningType.PAYMENT_DUE_DATE, "13.08.2026").onFragment { sheet ->
            val title = sheet.dialog?.findViewById<TextView>(R.id.warningTitle)
            assertThat(title?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_due_date_hint_title, "13.08.2026"))
        }
    }

    @Test
    fun `shows description of PAYMENT_DUE_DATE type`() {
        launchSheet(WarningType.PAYMENT_DUE_DATE, "13.08.2026").onFragment { sheet ->
            val description = sheet.dialog?.findViewById<TextView>(R.id.warningDescription)
            assertThat(description?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_due_date_hint_desc))
        }
    }

    @Test
    fun `builds plain title for type without format argument`() {
        launchSheet(WarningType.DOCUMENT_MARKED_AS_PAID).onFragment { sheet ->
            val title = sheet.dialog?.findViewById<TextView>(R.id.warningTitle)
            assertThat(title?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_document_marked_paid_title))
        }
    }

    @Test
    fun `proceed anyway is the primary CTA for PAYMENT_DUE_DATE`() {
        launchSheet(WarningType.PAYMENT_DUE_DATE, "13.08.2026").onFragment { sheet ->
            val primary = sheet.dialog?.findViewById<Button>(R.id.primary_button)
            val secondary = sheet.dialog?.findViewById<Button>(R.id.secondary_button)
            assertThat(primary?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_proceed_anyway))
            assertThat(secondary?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_cancel_transfer))
        }
    }

    @Test
    fun `cancel transfer stays the primary CTA for DOCUMENT_MARKED_AS_PAID`() {
        launchSheet(WarningType.DOCUMENT_MARKED_AS_PAID).onFragment { sheet ->
            val primary = sheet.dialog?.findViewById<Button>(R.id.primary_button)
            val secondary = sheet.dialog?.findViewById<Button>(R.id.secondary_button)
            assertThat(primary?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_cancel_transfer))
            assertThat(secondary?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_proceed_anyway))
        }
    }

    @Test
    fun `builds title with format argument for SCHEDULE_PAYMENT`() {
        launchSheet(WarningType.SCHEDULE_PAYMENT, "13.08.2026").onFragment { sheet ->
            val title = sheet.dialog?.findViewById<TextView>(R.id.warningTitle)
            assertThat(title?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_due_date_hint_title, "13.08.2026"))
        }
    }

    /**
     * Both states of the due date bottom sheet share the same title string (confirmed against
     * Figma), so the schedule state must not introduce a second title resource.
     */
    @Test
    fun `SCHEDULE_PAYMENT shares its title with PAYMENT_DUE_DATE`() {
        assertThat(WarningType.SCHEDULE_PAYMENT.titleRes)
            .isEqualTo(WarningType.PAYMENT_DUE_DATE.titleRes)
    }

    @Test
    fun `shows description of SCHEDULE_PAYMENT type`() {
        launchSheet(WarningType.SCHEDULE_PAYMENT, "13.08.2026").onFragment { sheet ->
            val description = sheet.dialog?.findViewById<TextView>(R.id.warningDescription)
            assertThat(description?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_schedule_payment_hint_desc))
        }
    }

    @Test
    fun `schedule payment is the primary CTA for SCHEDULE_PAYMENT`() {
        launchSheet(WarningType.SCHEDULE_PAYMENT, "13.08.2026").onFragment { sheet ->
            val primary = sheet.dialog?.findViewById<Button>(R.id.primary_button)
            val secondary = sheet.dialog?.findViewById<Button>(R.id.secondary_button)
            assertThat(primary?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_schedule_payment))
            assertThat(secondary?.text?.toString())
                .isEqualTo(sheet.getString(R.string.gc_proceed_anyway))
        }
    }

    @Test
    fun `schedule payment sheet is not cancelable`() {
        launchSheet(WarningType.SCHEDULE_PAYMENT, "13.08.2026").onFragment { sheet ->
            assertThat(sheet.isCancelable).isFalse()
        }
    }

    @Test
    fun `sheet is not cancelable`() {
        launchSheet(WarningType.PAYMENT_DUE_DATE, "13.08.2026").onFragment { sheet ->
            assertThat(sheet.isCancelable).isFalse()
        }
    }

    @Test
    fun `primary CTA notifies listener and dismisses the sheet`() {
        val listener = mock<WarningBottomSheet.Listener>()
        var dialog: Dialog? = null

        launchSheet(WarningType.PAYMENT_DUE_DATE, "13.08.2026").onFragment { sheet ->
            sheet.listener = listener
            dialog = sheet.dialog
            sheet.dialog?.findViewById<View>(R.id.primary_button)?.performClick()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(listener).onPrimaryAction()
        assertThat(dialog?.isShowing).isFalse()
    }

    @Test
    fun `secondary CTA notifies listener and dismisses the sheet`() {
        val listener = mock<WarningBottomSheet.Listener>()
        var dialog: Dialog? = null

        launchSheet(WarningType.PAYMENT_DUE_DATE, "13.08.2026").onFragment { sheet ->
            sheet.listener = listener
            dialog = sheet.dialog
            sheet.dialog?.findViewById<View>(R.id.secondary_button)?.performClick()
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(listener).onSecondaryAction()
        assertThat(dialog?.isShowing).isFalse()
    }

    private fun launchSheet(
        type: WarningType,
        titleFormatArg: String? = null
    ): FragmentScenario<WarningBottomSheet> {
        // FragmentScenario replaces the fragment's arguments with fragmentArgs, so the
        // arguments built by newInstance are passed through explicitly.
        val args = WarningBottomSheet.newInstance(type, titleFormatArg).arguments
        val scenario = FragmentScenario.launch(
            WarningBottomSheet::class.java,
            args,
            R.style.GiniCaptureTheme,
            null as FragmentFactory?
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return scenario
    }
}
