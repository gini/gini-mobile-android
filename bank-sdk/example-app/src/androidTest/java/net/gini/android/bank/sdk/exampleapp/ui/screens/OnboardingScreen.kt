package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.CoreMatchers.allOf

class OnboardingScreen {
    fun checkOnboardingScreenTitle(onboardingTitle: Int): OnboardingScreen {
        onView(
            allOf(
                withId(net.gini.android.capture.R.id.gc_title), withText(onboardingTitle)
            )
        ).check(matches(isDisplayed()))
        return this
    }

    fun checkNextButtonText(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_next)).check(matches(withText("Next")))
        return this
    }

    fun clickNextButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_next)).check(matches(isDisplayed()))
            .check(matches(isClickable())).perform(click())
        return this
    }

    fun checkSkipButtonText(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_skip)).check(matches(withText("Skip")))
        return this
    }

    fun clickSkipButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_skip)).perform(click())
        return this
    }

    fun checkGetStartedButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_get_started)).check(matches(withText("Get Started")))
        return this
    }
}