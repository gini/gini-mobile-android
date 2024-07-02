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
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf

class CaptureScreen {
    fun assertCameraTitle(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_camera_title)).check(matches(withText("Scan an invoice")))
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

    fun clickFlashButton(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_button_camera_flash)).perform(click())
        return this
    }
}