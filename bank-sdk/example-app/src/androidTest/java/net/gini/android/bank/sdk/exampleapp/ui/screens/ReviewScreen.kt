package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
}