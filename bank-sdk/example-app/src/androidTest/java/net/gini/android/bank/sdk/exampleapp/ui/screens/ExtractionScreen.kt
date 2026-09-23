package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.widget.EditText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.R
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import org.hamcrest.Matchers.allOf

class ExtractionScreen {

    // The extraction screen only appears after the Gini API returns results, which can
    // take noticeably longer on remote/BrowserStack devices than the fixed IdlingResource
    // sleep. Wait for the transfer-summary button to actually exist before interacting,
    // so slow network responses don't cause a NoMatchingViewException.
    private fun waitForExtractionScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        // Resolve the package at runtime — the app-under-test's applicationId varies by
        // flavor (e.g. paymentProvider flavors), so don't hard-code it into the resourceId.
        device.findObject(
            UiSelector().resourceId(AppResources.resId("transfer_summary"))
        ).waitForExists(EXTRACTION_TIMEOUT)
    }

    fun clickTransferSummaryButton(): ExtractionScreen {
        waitForExtractionScreen()
        onView(withId(R.id.transfer_summary)).perform(click())
        return this
    }

    fun editTransferSummaryFields(hint: String, value: String) {
        waitForExtractionScreen()
        onView(allOf(withId(R.id.text_value), withHint(hint)))
            .perform(click())
            .perform(replaceText(value))
    }

    fun assertExtractionScreenIsDisplayed(): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return device.findObject(
            UiSelector().resourceId(AppResources.resId("transfer_summary"))
        ).waitForExists(EXTRACTION_TIMEOUT)
    }

    // The scheduled-payment indicator is GONE unless ExtractionsActivity was launched for
    // a CaptureResult.SchedulePayment; a GONE view is absent from the accessibility tree,
    // so exists() distinguishes the schedule path from the pay-now Success path.
    fun isScheduledPaymentIndicatorDisplayed(): Boolean {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        return device.findObject(
            UiSelector().resourceId(AppResources.resId("text_scheduled_payment_indicator"))
        ).exists()
    }

    fun checkTransferSummaryButtonIsClickable(): Boolean {
        waitForExtractionScreen()
        var isTransferSummaryButtonClickable = false
        // When the view isn't found Espresso passes a null view plus the exception, so the
        // view must be null-guarded — otherwise a missing extraction screen surfaces as an
        // opaque NullPointerException instead of a plain assertion failure.
        onView(withId(R.id.transfer_summary))  .check { view, noViewFoundException ->
            if (noViewFoundException == null || (view != null && view.isClickable())) {
                isTransferSummaryButtonClickable = true
            }
        }
        return isTransferSummaryButtonClickable
    }

    /**
     * Value of the extraction row whose label is [extractionName] (the raw extraction key, e.g.
     * "amountToPay"), or null when the extraction is not in the result at all.
     *
     * The rows live in a RecyclerView, so a row that is merely off-screen is absent from the
     * view hierarchy and indistinguishable from a missing extraction. This scrolls the list from
     * the top and only reports null once the list can no longer scroll, so a null here means
     * "no such row anywhere in the list" rather than "not currently visible".
     *
     * Null is NOT how you detect a stripped extraction. The example app re-adds every missing
     * SEPA field as an empty editable row, so keys like `amountToPay` come back as "" and never
     * as null. Use [isExtractionFieldFilled] for that assertion.
     *
     * Matching is on the TextInputLayout hint via Espresso's withHint (EditText.getHint()) rather
     * than on the accessibility node's hintText, so it does not depend on how Material forwards
     * the hint to the accessibility tree.
     */
    fun extractionFieldValue(extractionName: String): String? {
        waitForExtractionScreen()
        val list = UiScrollable(
            UiSelector().resourceId(AppResources.resId("recyclerview_extractions"))
        ).setAsVerticalList()

        runCatching { list.scrollToBeginning(MAX_LIST_SWIPES) }

        do {
            readVisibleFieldValue(extractionName)?.let { return it }
        } while (runCatching { list.scrollForward() }.getOrDefault(false))

        return readVisibleFieldValue(extractionName)
    }

    /**
     * True when the extraction is present and carries a non-blank value.
     *
     * Always ask this rather than "is the row present". The example app re-adds every missing
     * SEPA field as an empty editable row, so an `amountToPay` row exists even after the SDK
     * has stripped the extraction — presence proves nothing, the value does.
     */
    fun isExtractionFieldFilled(extractionName: String): Boolean =
        extractionFieldValue(extractionName)?.isNotBlank() == true

    /**
     * Reads the row currently in the hierarchy, or null when it is not bound. A
     * NoMatchingViewException here means "not on screen", which the caller resolves by
     * scrolling — it is not a test failure.
     */
    private fun readVisibleFieldValue(extractionName: String): String? {
        var value: String? = null
        try {
            onView(allOf(withId(R.id.text_value), withHint(extractionName)))
                .check { view, noViewFoundException ->
                    if (noViewFoundException == null && view is EditText) {
                        value = view.text.toString()
                    }
                }
        } catch (_: NoMatchingViewException) {
            return null
        }
        return value
    }

    companion object {
        private const val EXTRACTION_TIMEOUT = 30_000L
        private const val MAX_LIST_SWIPES = 20
    }
}

