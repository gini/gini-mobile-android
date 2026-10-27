package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

class NoResultScreen {

    // The no-result screen only appears after the Gini API returns, which can take
    // noticeably longer on remote/BrowserStack devices than the fixed IdlingResource
    // sleep. Wait for the given text to actually exist before asserting on the screen.
    fun waitForNoResultScreen(resourceId: Int) {
        val text = InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.findObject(UiSelector().text(text)).waitForExists(NO_RESULT_TIMEOUT)
    }

    fun checkElementWithTextIsDisplayed(resourceId: Int) : Boolean {
        var isElementDisplayed = false
        onView(withText(resourceId)).
            check { view, _ ->
                // When the view is not found, Espresso passes a null view (with the
                // NoMatchingViewException in the second param). Guard against null so a
                // missing element returns false instead of throwing a NullPointerException.
                if (view != null && view.isShown()) {
                    isElementDisplayed = true
            }
        }
        return isElementDisplayed
    }

    fun clickEnterManuallyButton() {
        onView(withText(net.gini.android.capture.R.string.gc_noresults_enter_manually)).perform(
            click())
    }

    /**
     * Only present for camera-sourced documents (`NoResultsFragmentImpl.shouldAllowRetakeImages`);
     * an imported image or PDF never shows it. Returns the flow to the camera screen.
     */
    fun clickRetakeImagesButton() {
        onView(withText(net.gini.android.capture.R.string.gc_noresults_retake_images)).perform(
            click())
    }

    /**
     * The back arrow of the SDK's top navigation bar (`NavigationBarTopAdapter` with
     * `NavButtonType.BACK`). On the No Results screen it leads back to the camera, not to the
     * previous screen. Matched by content description like `HelpScreen.clickBackButton`.
     */
    fun clickNavigationBackButton() {
        onView(withContentDescription(net.gini.android.capture.R.string.gc_back_button_description))
            .perform(click())
    }

    /** The system back button; `NoResultsFragmentImpl.handleOnBackPressed` routes it to the camera. */
    fun pressSystemBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    }

    companion object {
        private const val NO_RESULT_TIMEOUT = 30_000L
    }
}
