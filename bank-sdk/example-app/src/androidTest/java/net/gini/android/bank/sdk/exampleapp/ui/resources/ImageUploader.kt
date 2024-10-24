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
//        Thread.sleep(2000)
        val photoList = UiScrollable(UiSelector().scrollable(true))
        val firstPhoto: UiObject = photoList.getChildByInstance(UiSelector().className("android.widget.ImageView"), 3)
        firstPhoto.click()

//        val buttons = device.findObjects(By.clazz("android.widget.ImageView"))
//        buttons[3].click()

//        device.waitForIdle()
    }

    fun clickAddButton() {
        val rootObject = device.findObject(UiSelector().index(0))

        val buttonSelector = UiSelector().className("android.widget.Button")
        val buttons = device.findObjects(By.clazz("android.widget.Button"))
        buttons[1].click()
    }
}