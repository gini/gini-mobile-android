package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector

class PdfUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun uploadPdfFromFiles(file: String) {
        device.waitForIdle()

        // Assuming you’re on the main screen of the Files app, search for the PDF file by name
        val fileList = UiScrollable(UiSelector().scrollable(true))

//         Find the PDF file by name and click on it
        val tabTitle = device.findObject(UiSelector().className("android.widget.TextView").text("Recent"))
        if (tabTitle.exists()) {
//            Interact with hamburger menu
            val hamburgerMenu = device.findObject(UiSelector().className("android.widget.ImageButton"))
            hamburgerMenu.exists()
            hamburgerMenu.click()

//             Click the 'Downloads' option
            val downloadsOption = device.findObject(UiSelector()
                .className("android.widget.TextView")
                .text("Downloads"))
            downloadsOption.exists()
            downloadsOption.click()
            val selectPdfFile = fileList.getChildByText(UiSelector().text(file), file)
            selectPdfFile.click()
        }
        else {
//            Select desired pdf file
            val selectPdfFile = fileList.getChildByText(UiSelector().text(file), file)
            selectPdfFile.click()
        }
    }
}