package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf

class CaptureScreen {
    fun assertCameraTitle(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_camera_title)).check(matches(withText(net.gini.android.capture.R.string.gc_camera_info_label_only_invoice)))
        return this
    }

    fun clickCameraButton(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_button_camera_trigger)).perform(click())
        return this
    }

    fun clickHelpButton(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_action_show_onboarding)).perform(click())
        return this
    }

    fun clickCancelButton(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_navigation_bar)).perform(click())
        return this
    }

    fun assertFlashIconIsDisplayed(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_camera_flash_button_subtitle)).check(
            matches(
                isDisplayed()
            )
        )
        return this
    }

    fun assertFlashIconIsOn(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_camera_flash_button_subtitle)).check(
            matches(
                withText(net.gini.android.capture.R.string.gc_camera_flash_on_subtitle)
            )
        )
        return this
    }

    fun assertFlashIconIsOff(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_camera_flash_button_subtitle)).check(
            matches(
                withText(net.gini.android.capture.R.string.gc_camera_flash_off_subtitle)
            )
        )
        return this
    }
}