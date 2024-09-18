package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf

class ReviewScreen {
    fun assertReviewTitleIsDisplayed(): ReviewScreen {
        onView(withText("Review")).check(matches(isDisplayed()))
        return this
    }

    fun clickProcessButton(): ReviewScreen {
        onView(
            allOf(withId(net.gini.android.capture.R.id.gc_button_next), withText("Process")))
                .perform(click())
        return this
    }

    fun clickCancelButton(): ReviewScreen {
        onView(
            allOf(withContentDescription("Close")))
                .perform(click())
        return this
    }

    fun clickDeleteButton(): ReviewScreen {
        onView(
            allOf(withId(net.gini.android.capture.R.id.gc_button_delete), withContentDescription("Delete page")))
            .perform(click())
        return this
    }

    fun clickAddMorePagesButton(): ReviewScreen {
        onView(withId(net.gini.android.capture.R.id.gc_add_page)).perform(click())
        return this
    }

    fun pinchToZoomInvoice(): ReviewScreen {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val documentPage = device.findObject(UiSelector().descriptionContains("Document page"))
        documentPage.click()
        documentPage.pinchOut(100, 50)
        return this
    }
}