package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import net.gini.android.capture.R as CaptureR

/**
 * Page object for the capture SDK's warning bottom sheet (Due Date Hint, Schedule Payment
 * and Credit Note states) shown on the Analysis screen.
 *
 * The two due date states share the same title (gc_due_date_hint_title), and the Credit Note
 * state shares its CTA labels with them in the opposite order, so state assertions always
 * check the description and both CTA labels as well — never the title alone.
 * All texts are resolved from the SDK's string resources at runtime, which keeps the
 * assertions locale-independent.
 */
class WarningBottomSheetScreen {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Waits for the sheet to appear. The sheet only shows after the upload + analysis
     * round trip and the education animation complete, so the wait is generous.
     */
    fun waitForSheet(): Boolean =
        device.findObject(UiSelector().resourceId(AppResources.resId("warningTitle")))
            .waitForExists(SHEET_TIMEOUT)

    fun isSheetDisplayed(): Boolean =
        device.findObject(UiSelector().resourceId(AppResources.resId("warningTitle"))).exists()

    fun assertDueDateHintState(formattedDueDate: String): WarningBottomSheetScreen {
        assertSheetTexts(
            title = targetContext.getString(
                CaptureR.string.gc_due_date_hint_title,
                formattedDueDate
            ),
            description = targetContext.getString(CaptureR.string.gc_due_date_hint_desc),
            primaryButton = targetContext.getString(CaptureR.string.gc_proceed_anyway),
            secondaryButton = targetContext.getString(CaptureR.string.gc_cancel_transfer)
        )
        return this
    }

    fun assertSchedulePaymentState(formattedDueDate: String): WarningBottomSheetScreen {
        assertSheetTexts(
            title = targetContext.getString(
                CaptureR.string.gc_due_date_hint_title,
                formattedDueDate
            ),
            description = targetContext.getString(CaptureR.string.gc_schedule_payment_hint_desc),
            primaryButton = targetContext.getString(CaptureR.string.gc_schedule_payment),
            secondaryButton = targetContext.getString(CaptureR.string.gc_proceed_anyway)
        )
        return this
    }

    /**
     * Asserts the Credit Note warning state: the title, the description and both CTAs.
     *
     * The CTA order is the reverse of the due date states — DOCUMENT_MARKED_AS_CREDIT_NOTE
     * declares "Cancel transfer" as the primary (filled) button and "Proceed anyway" as the
     * secondary, which is what the Xray steps describe as blue and white respectively. Getting
     * this backwards would still match a title-only assertion, so both labels are checked.
     */
    fun assertCreditNoteState(): WarningBottomSheetScreen {
        assertSheetTexts(
            title = targetContext.getString(
                CaptureR.string.gc_document_marked_credit_note_title
            ),
            description = targetContext.getString(
                CaptureR.string.gc_document_marked_credit_note_desc
            ),
            primaryButton = targetContext.getString(CaptureR.string.gc_cancel_transfer),
            secondaryButton = targetContext.getString(CaptureR.string.gc_proceed_anyway)
        )
        return this
    }

    fun clickPrimaryButton() {
        onView(withId(CaptureR.id.primary_button)).perform(click())
    }

    fun clickSecondaryButton() {
        onView(withId(CaptureR.id.secondary_button)).perform(click())
    }

    /**
     * Taps the dimmed scrim above the sheet. The tap lands halfway between the top of
     * the screen and the sheet's top edge, floored so it can never hit the status bar
     * (a status-bar tap could pull down the notification shade and break the test).
     */
    fun tapOutsideSheet(): WarningBottomSheetScreen {
        val sheetTop = device.findObject(
            UiSelector().resourceId(AppResources.resId("warningTitle"))
        ).bounds.top
        device.click(device.displayWidth / 2, maxOf(MIN_OUTSIDE_TAP_Y, sheetTop / 2))
        return this
    }

    /**
     * Watches the rotating capture suggestions behind the sheet for [timeoutMs] and
     * reports whether they ever appeared. A GONE view is absent from the accessibility
     * tree, so waitForExists() returning false means they stayed suppressed for the
     * whole window. The window must exceed AnalysisHintsAnimator.HINT_START_DELAY
     * (5000 ms) to be meaningful.
     */
    fun didCaptureSuggestionsAppearWithin(timeoutMs: Long): Boolean =
        device.findObject(
            UiSelector().resourceId(AppResources.resId("gc_analysis_hint_container"))
        ).waitForExists(timeoutMs)

    private fun assertSheetTexts(
        title: String,
        description: String,
        primaryButton: String,
        secondaryButton: String
    ) {
        onView(withId(CaptureR.id.warningTitle)).check(matches(withText(title)))
        onView(withId(CaptureR.id.warningDescription)).check(matches(withText(description)))
        onView(withId(CaptureR.id.primary_button)).check(matches(withText(primaryButton)))
        onView(withId(CaptureR.id.secondary_button)).check(matches(withText(secondaryButton)))
    }

    companion object {
        private const val SHEET_TIMEOUT = 30_000L
        private const val MIN_OUTSIDE_TAP_Y = 200

    }
}
