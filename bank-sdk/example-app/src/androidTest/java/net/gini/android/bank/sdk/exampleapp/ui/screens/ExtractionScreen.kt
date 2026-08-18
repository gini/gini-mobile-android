package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.R
import org.hamcrest.Matchers.allOf
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources

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
        onView(withId(R.id.transfer_summary))  .check { view, noViewFoundException ->
            if (noViewFoundException == null || view.isClickable()) {
                isTransferSummaryButtonClickable = true
            }
        }
        return isTransferSummaryButtonClickable
    }

    companion object {
        private const val EXTRACTION_TIMEOUT = 30_000L
    }
}
