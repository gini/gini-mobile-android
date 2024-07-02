package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches

class OnboardingScreen {

    fun assertSkipButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_skip)).check(matches(withText("Skip")))
        return this
    }

    fun clickSkipButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_skip)).perform(click())
        return this
    }
}