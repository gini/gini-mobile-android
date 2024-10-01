package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf

class CaptureScreen {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun checkScanTextDisplayed(): Boolean {
        val scanText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Scan")
                .index(1)
        )
        return scanText.waitForExists(5000)
    }

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

    fun clickFilesButton(): CaptureScreen {
        onView(withId(net.gini.android.capture.R.id.gc_button_import_document)).perform(click())
        return this
    }

    fun clickPhotos(): CaptureScreen {
        onView(
            allOf(withId(net.gini.android.capture.R.id.gc_app_label), withText("Photos")))
             .perform(click())
        return this
    }

    fun clickFiles(): CaptureScreen {
        onView(
            allOf(withId(net.gini.android.capture.R.id.gc_app_label), withText("Files")))
            .perform(click())
        return this
    }
}