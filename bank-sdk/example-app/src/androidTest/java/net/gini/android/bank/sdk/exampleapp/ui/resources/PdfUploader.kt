package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector

class PdfUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun uploadPdfFromFiles() {
        //Wait for 'tab'
        device.waitForIdle()
        val tabTitle = device.findObject(UiSelector().className("android.widget.TextView").text("Recent"))
        if (tabTitle.exists() && tabTitle.text == "Recents") {
            //Interact with hamburger menu
            val hamburgerMenu = device.findObject(UiSelector().className("android.widget.ImageButton"))
            hamburgerMenu.click()

            // Click the 'Downloads' option
            val downloadsOption = device.findObject(UiSelector()
                .className("android.widget.TextView")  // Changed to TextView to match the item type
                .text("Downloads"))
            downloadsOption.click()

            //Select desired pdf file
            val selectPdfFile = device.findObject(UiSelector().text("sample.pdf"))
            selectPdfFile.click()
        }
        else {

            //Select desired pdf file
            val selectPdfFile = device.findObject(UiSelector().text("sample.pdf"))
            selectPdfFile.click()
        }
    }
}