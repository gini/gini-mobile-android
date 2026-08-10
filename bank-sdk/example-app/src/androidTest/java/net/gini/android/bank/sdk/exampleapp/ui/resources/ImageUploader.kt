package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
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

    fun copyImageToDownloads(context: Context, filename: String) {
        context.contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
            arrayOf(filename)
        )
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return
        context.contentResolver.openOutputStream(uri)?.use { output ->
            context.assets.open(filename).use { input -> input.copyTo(output) }
        }
    }

    fun uploadImageFromFiles(filename: String) {
        device.waitForIdle()
        val fileList = UiScrollable(UiSelector().scrollable(true))
        navigateToDownloads()
        fileList.getChildByText(UiSelector().text(filename), filename).click()
    }

    private fun navigateToDownloads() {
        val downloadsVisible = device.findObject(
            UiSelector().className("android.widget.TextView").text("Downloads")
        )
        if (downloadsVisible.exists()) {
            downloadsVisible.click()
            device.waitForIdle()
            return
        }
        val hamburger = device.findObject(
            UiSelector().resourceId("com.google.android.documentsui:id/drawer_hamburger")
        )
        if (hamburger.waitForExists(2000)) {
            hamburger.click()
            val downloads = device.findObject(
                UiSelector().className("android.widget.TextView").text("Downloads")
            )
            if (downloads.waitForExists(3000)) {
                downloads.click()
                device.waitForIdle()
            }
        }
    }
}