package net.gini.android.bank.sdk.exampleapp.ui.resources
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector


class ImageUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun uploadImageFromPhotos() {
       device.waitForIdle()
        val selectPhoto = device.findObject(
            UiSelector()
                .className("android.widget.ImageView")
                .resourceId("com.google.android.providers.media.module:id/icon_thumbnail")
        )
        if (selectPhoto != null) {
            selectPhoto.click()
        } else {
            throw Exception("First photo not found")
        }
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