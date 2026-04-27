package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

class ErrorScreen {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun checkErrorTextDisplayed(): Boolean {
        val errorText = device.findObject(
            UiSelector().className("android.widget.TextView").text("Error").index(1)
        )
        return errorText.waitForExists(5000)
    }

    fun checkErrorHeaderTextDisplayed(content: String): Boolean {
        val errorHeaderText = device.findObject(
            UiSelector().className("android.widget.TextView")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gc_error_header").index(1)
        )
        if (errorHeaderText.waitForExists(5000)) {
            if (errorHeaderText.text == content) return true
        }
        return false
    }

    fun checkErrorTextViewDisplayed(content: String): Boolean {
        val errorTextViewText = device.findObject(
            UiSelector().className("android.widget.TextView")
                .resourceId("net.gini.android.bank.sdk.exampleapp:id/gc_error_textview").index(3)
        )
        if (errorTextViewText.waitForExists(5000)) {
            if (errorTextViewText.text == content) return true
        }
        return false
    }

    fun checkEnterManuallyButtonIsDisplayed(): Boolean {
        var enterManuallyButtonDisplayed = false
        onView(withText(net.gini.android.capture.R.string.gc_noresults_enter_manually)).check { view, _ ->
            if (view.isShown()) {
                enterManuallyButtonDisplayed = true
            }
        }
        return enterManuallyButtonDisplayed
    }

    fun clickEnterManuallyButton() {
        onView(withText(net.gini.android.capture.R.string.gc_noresults_enter_manually)).perform(
            click()
        )
    }

    fun checkBackToCameraButtonIsDisplayed(): Boolean {
        var backToCameraButtonDisplayed = false
        onView(withText(net.gini.android.capture.R.string.gc_error_back_to_camera)).check { view, _ ->
            if (view.isShown()) {
                backToCameraButtonDisplayed = true
            }
        }
        return backToCameraButtonDisplayed
    }

    fun clickBackToCameraButton() {
        onView(withText(net.gini.android.capture.R.string.gc_error_back_to_camera)).perform(
            click()
        )
    }

    fun disconnectTheInternetConnection() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("svc wifi disable").close()
        uiAutomation.executeShellCommand("svc data disable").close()
        Thread.sleep(2000)
    }

    fun reconnectTheInternetConnection() {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.executeShellCommand("svc wifi enable").close()
        uiAutomation.executeShellCommand("svc data enable").close()
        Thread.sleep(2000)
    }
}