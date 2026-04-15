package net.gini.android.bank.sdk.exampleapp.ui.resources

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector

class PdfUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    // Selects a PDF from the system file picker.
    //
    // BrowserStack places non-media files in the device's Downloads folder.
    // The picker opens to Recents, so we navigate to Downloads via the drawer.
    // On iOS, BrowserStack uses a Custom_Files folder — checked first as a
    // quick fallback in case Android ever adopts the same behaviour.
    fun uploadPdfFromFiles(file: String) {
        device.waitForIdle()
        val fileList = UiScrollable(UiSelector().scrollable(true))

        // Check for BrowserStack Custom_Files folder.
        val customFilesFolder = device.findObject(
            UiSelector().className("android.widget.TextView").text("Custom_Files")
        )
        if (customFilesFolder.waitForExists(2000)) {
            customFilesFolder.click()
            device.waitForIdle()
            fileList.getChildByText(UiSelector().text(file), file).click()
            return
        }

        // BrowserStack places Android non-media files in Downloads.
        // Navigate there from the Recents view.
        navigateToDownloads()

        fileList.getChildByText(UiSelector().text(file), file).click()
    }

    private fun navigateToDownloads() {
        // Downloads may already be visible in the sidebar (e.g. on tablets or
        // when the drawer is already open).
        val downloadsVisible = device.findObject(
            UiSelector().className("android.widget.TextView").text("Downloads")
        )
        if (downloadsVisible.exists()) {
            downloadsVisible.click()
            device.waitForIdle()
            return
        }

        // Open the navigation drawer, then tap Downloads.
        if (openNavigationDrawer()) {
            val downloadsOption = device.findObject(
                UiSelector().className("android.widget.TextView").text("Downloads")
            )
            if (downloadsOption.waitForExists(3000)) {
                downloadsOption.click()
                device.waitForIdle()
            }
        }
    }

    private fun openNavigationDrawer(): Boolean {
        // Prefer the stable DocumentsUI resource ID for the hamburger button.
        val byResourceId = device.findObject(
            UiSelector().resourceId("com.google.android.documentsui:id/drawer_hamburger")
        )
        if (byResourceId.waitForExists(2000)) {
            byResourceId.click()
            return true
        }

        // Fallback: any ImageButton in the picker (covers older Android versions).
        val byClass = device.findObject(UiSelector().className("android.widget.ImageButton"))
        if (byClass.waitForExists(2000)) {
            byClass.click()
            return true
        }

        return false
    }
}
