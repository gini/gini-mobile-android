package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

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
        device.openQuickSettings()
        Thread.sleep(3000)
        if (UiObject(UiSelector().text("AndroidWifi")).exists()) {
            device.findObject(By.text("AndroidWifi")).click()
            device.wait(Until.hasObject(By.text("Internet")), 3000)
            //Mobile Networks - LTE, 3G, 4G
            val mobileNetworkSwitch = device.findObject(
                UiSelector().className("android.widget.Switch")
                    .resourceId("com.android.systemui:id/mobile_toggle").index(0)
            )
            mobileNetworkSwitch.click()
            //Wifi
            val wifiText = device.findObject(
                UiSelector().className("android.widget.TextView")
                    .resourceId("com.android.systemui:id/wifi_toggle_title").text("Wi‑Fi")
            )
            wifiText.waitForExists(5000)

            val wifiSwitch = device.findObject(
                UiSelector().className("android.widget.Switch")
                    .resourceId("com.android.systemui:id/wifi_toggle").descriptionContains("Wi‑Fi")
                    .index(0)
            )
            if (wifiSwitch.waitForExists(5000) && wifiSwitch.isChecked) {
                wifiSwitch.click()
            }
        }
        device.pressBack()
        val centerOfDevice = (device.displayWidth) / 2
        val bottomOfDevice = (device.displayHeight * 1.5).toInt()
        val steps = 40
        device.swipe(centerOfDevice, bottomOfDevice, centerOfDevice, 500, steps)
        Thread.sleep(3000)
        device.swipe(centerOfDevice, bottomOfDevice, centerOfDevice, 500, steps)
    }
}