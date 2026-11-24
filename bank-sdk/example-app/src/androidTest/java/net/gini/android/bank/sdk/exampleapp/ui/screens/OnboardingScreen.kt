package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not

class OnboardingScreen {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** Uptime of the last Next tap made by [clickNextAndWaitFor], or 0 if there was none yet. */
    private var lastNextTapAt = 0L
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

    fun clickSkipButtonIfPresent(): OnboardingScreen {
        try {
            onView(withId(net.gini.android.capture.R.id.gc_skip)).perform(click())
        } catch (e: NoMatchingViewException) {
            // Onboarding already dismissed; camera screen is shown directly
        }
        return this
    }

    fun checkGetStartedButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_get_started)).check(matches(withText("Get Started")))
        return this
    }

    /**
     * Waits until the onboarding page with [titleRes] is the current page, then checks that its
     * title is displayed. A page change is a ViewPager scroll, which Espresso does not reliably
     * wait for, so the wait is done with UiAutomator. The neighbour pages are attached
     * (offscreenPageLimit = 1) but off screen, so UiAutomator does not match them. UiAutomator also
     * waits for accessibility events to go quiet before each lookup, which lets the scroll settle.
     */
    fun waitForPage(titleRes: Int): OnboardingScreen {
        val title = InstrumentationRegistry.getInstrumentation().targetContext.getString(titleRes)
        val titleView = device.findObject(
            UiSelector().resourceId(AppResources.resId("gc_title")).text(title)
        )
        if (!titleView.waitForExists(ONBOARDING_PAGE_TIMEOUT_MS)) {
            throw AssertionError("Onboarding page '$title' not shown within $ONBOARDING_PAGE_TIMEOUT_MS ms")
        }
        return checkOnboardingScreenTitle(titleRes)
    }

    fun swipeToNextPage(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_onboarding_viewpager)).perform(swipeLeft())
        return this
    }

    fun swipeToPreviousPage(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_onboarding_viewpager)).perform(swipeRight())
        return this
    }

    /**
     * Taps Next and waits for the page with [titleRes].
     *
     * The SDK's Next button ignores a second tap within `IntervalClickListenerHelper.interval`
     * (500 ms), and that state is not observable. A tap that comes too soon is lost without an
     * error, so this waits until [NEXT_TAP_INTERVAL_MS] have passed since the previous Next tap.
     */
    fun clickNextAndWaitFor(titleRes: Int): OnboardingScreen {
        val sinceLastTap = SystemClock.uptimeMillis() - lastNextTapAt
        if (sinceLastTap < NEXT_TAP_INTERVAL_MS) {
            SystemClock.sleep(NEXT_TAP_INTERVAL_MS - sinceLastTap)
        }
        clickNextButton()
        lastNextTapAt = SystemClock.uptimeMillis()
        return waitForPage(titleRes)
    }

    /** Taps Skip. Unlike [clickSkipButtonIfPresent], a missing Skip button fails the test. */
    fun clickSkipButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_skip)).perform(click())
        return this
    }

    fun clickGetStartedButton(): OnboardingScreen {
        onView(withId(net.gini.android.capture.R.id.gc_get_started)).perform(click())
        return this
    }

    /** On the last page the Next/Skip group is INVISIBLE, so [isDisplayed] is false for both. */
    fun checkNextAndSkipDisplayed(displayed: Boolean): OnboardingScreen {
        val visibility = if (displayed) isDisplayed() else not(isDisplayed())
        onView(withId(net.gini.android.capture.R.id.gc_next)).check(matches(visibility))
        onView(withId(net.gini.android.capture.R.id.gc_skip)).check(matches(visibility))
        return this
    }

    fun checkGetStartedDisplayed(displayed: Boolean): OnboardingScreen {
        val visibility = if (displayed) isDisplayed() else not(isDisplayed())
        onView(withId(net.gini.android.capture.R.id.gc_get_started)).check(matches(visibility))
        return this
    }

    private companion object {
        const val ONBOARDING_PAGE_TIMEOUT_MS = 5_000L

        /** The SDK's 500 ms click debounce plus a 100 ms margin. */
        const val NEXT_TAP_INTERVAL_MS = 600L
    }
}