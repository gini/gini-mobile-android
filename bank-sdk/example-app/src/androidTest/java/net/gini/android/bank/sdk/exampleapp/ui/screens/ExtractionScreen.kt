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

class ExtractionScreen {

    // The extraction screen only appears after the Gini API returns results, which can
    // take noticeably longer on remote/BrowserStack devices than the fixed IdlingResource
    // sleep. Wait for the transfer-summary button to actually exist before interacting,
    // so slow network responses don't cause a NoMatchingViewException.
    private fun waitForExtractionScreen() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.findObject(
            UiSelector().resourceId("net.gini.android.bank.sdk.exampleapp:id/transfer_summary")
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
