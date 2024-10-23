package net.gini.android.bank.sdk.exampleapp.ui.resources
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector


class ImageUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun uploadImageFromPhotos() {
       device.waitForIdle()
//        val selectPhoto = device.findObject(
//            UiSelector()
//                .className("android.widget.ImageView")
//                .resourceId("com.google.android.providers.media.module:id/icon_thumbnail")
//        )
        // Step 3: Use UiScrollable to scroll through the list of photos (adjust based on actual layout)
        val photoList = UiScrollable(UiSelector().scrollable(true))

        // Step 4: Find the first photo in the list (adjust index or criteria as necessary)
        val firstPhoto: UiObject = photoList.getChild(UiSelector().className("android.widget.ImageView").instance(0))
        firstPhoto.click()
//        if (selectPhoto != null) {
//            selectPhoto.click()
//        } else {
//            throw Exception("First photo not found")
//        }
    }

    fun clickAddButton() {
        val addButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .resourceId("com.google.android.providers.media.module:id/button_add")
        )
    if (addButton.exists()) {
        addButton.click()
    } else {
        throw java.lang.Exception("Add button not found")
    }
    }
}