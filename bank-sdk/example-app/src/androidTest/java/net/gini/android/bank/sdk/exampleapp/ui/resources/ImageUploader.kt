package net.gini.android.bank.sdk.exampleapp.ui.resources
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector


class ImageUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun uploadImageFromPhotos() {
       device.waitForIdle()
        val photoList = UiScrollable(UiSelector().scrollable(true))
        val firstPhoto: UiObject = photoList.getChild(UiSelector().className("android.widget.ImageView").instance(0))
        firstPhoto.click()
        device.waitForIdle()
    }

    fun clickAddButton() {
        val rootObject = device.findObject(UiSelector().index(0))

        val buttonSelector = UiSelector().className("android.widget.Button")
        val buttons = device.findObjects(By.clazz("android.widget.Button"))
        buttons[1].click()
        val sd = ""
//        logUiElementsRecursive(rootObject)
//        val okButton = device.findObject(UiSelector().textContains("Add(1)").className("android.widget.Button"))
//
//        okButton.click()
//        val addButton = device.findObject(
//            UiSelector()
//                .className("android.widget.Button")
//                .resourceId("com.google.android.providers.media.module:id/button_add")
//        )
//        if (addButton.exists()) {
//            addButton.click()
//        } else {
//            throw java.lang.Exception("Add button not found")
//        }
    }

    private fun logUiElementsRecursive(uiObject: UiObject, depth: Int = 0) {
        try {
            // Get child count (if any)
            val childCount = uiObject.childCount

            // Log the current element's details
            Log.d("UiElementLog", "${" ".repeat(depth * 2)}Element: ${uiObject.className}, Text: ${uiObject.text}, $uiObject., ContentDesc: ${uiObject.contentDescription}")

            // If there are child elements, recursively log them
            for (i in 0 until childCount) {
                val child = uiObject.getChild(UiSelector().index(i))
                logUiElementsRecursive(child, depth + 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}