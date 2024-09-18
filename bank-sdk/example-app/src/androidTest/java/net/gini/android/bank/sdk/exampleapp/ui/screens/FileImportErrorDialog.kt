package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.hamcrest.Matchers.allOf

class FileImportErrorDialog {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun checkContentIsDisplayed(resourceId: Int, content: String): Boolean {
        var isContentPanelDisplayed = false
        onView(
            allOf(
                withText(resourceId),
                withText(content)
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isContentPanelDisplayed = true
                }
            }
        return isContentPanelDisplayed
    }


    fun clickFilesApp() {
        device.pressHome()
        val appDrawer = UiScrollable(UiSelector().className("android.widget.FrameLayout"))
        appDrawer.scrollIntoView(UiSelector().text("Files"))
        val filesApp = device.findObject(UiSelector().text("Files"))
        filesApp.click()
    }

    fun clickTooManyPages(text: String): Boolean {
        val tooManyPagesPhoto = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text(text)
                .resourceId("android:id/title")
        )
        if(tooManyPagesPhoto.exists()) {
            tooManyPagesPhoto.click()
        }
        return false
    }

    fun openWith(): Boolean {
        val openWith = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Gini Bank")
                .resourceId("android:id/text1")
        )

        val alwaysButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .index(1)
                .text("Always")
                .resourceId("android:id/button_always")
        )
        if(openWith.exists() && openWith.isEnabled) {
            openWith.click()
            alwaysButton.click()
        }
        return false
    }

    fun checkToastMessage() {
        //To check the toast message
    }
}