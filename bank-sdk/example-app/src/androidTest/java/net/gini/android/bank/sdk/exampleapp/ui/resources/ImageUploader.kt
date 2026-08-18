package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector


class ImageUploader {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    fun uploadImageFromPhotos() {
        device.waitForIdle()
        // Legacy media-module picker (used by the BrowserStack Pixel devices).
        val legacyThumbnail = device.findObject(
            UiSelector()
                .className("android.widget.ImageView")
                .resourceId("com.google.android.providers.media.module:id/icon_thumbnail")
        )
        if (legacyThumbnail.waitForExists(3000)) {
            legacyThumbnail.click()
            return
        }
        // New Compose-based Mainline photo picker (com.google.android.photopicker, seen on
        // e.g. Samsung / Android 16): it exposes no resource ids at all — the photo tiles
        // only carry a localized content description built from the picker's
        // photopicker_item_content_desc template ("%1$s taken on %2$s" in English,
        // "%1$s wurde am %2$s aufgenommen" in German). The first tile is the newest
        // photo, i.e. the test image copied to the MediaStore right before.
        val tileSelectors = listOf(
            UiSelector().descriptionStartsWith("Photo taken on"), // English
            UiSelector().descriptionStartsWith("Foto wurde am"), // German
            // Locale-agnostic last resort: only the media tiles carry a year in their
            // content description, whatever the device language.
            UiSelector().descriptionMatches(".*\\b20\\d\\d\\b.*")
        )
        val deadline = SystemClock.uptimeMillis() + TILE_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            tileSelectors.forEach { selector ->
                val tile = device.findObject(selector)
                if (tile.exists()) {
                    tile.click()
                    return
                }
            }
            SystemClock.sleep(POLL_INTERVAL)
        }
        throw Exception("First photo not found in photo picker")
    }

    fun clickAddButton() {
        // Legacy media-module picker (BrowserStack Pixels).
        val addButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .resourceId("com.google.android.providers.media.module:id/button_add")
        )
        if (addButton.waitForExists(3000)) {
            addButton.click()
            return
        }
        // New Mainline photo picker: multi-select confirms via a localized button (no
        // resource id; photopicker_done_button_label is "Done" in English, "Fertig" in
        // German — "Add"/"Hinzufügen" cover picker variants using an add label);
        // single-select closes right after the photo tap, in which case there is
        // nothing to confirm and we return once the deadline passes.
        val confirmSelectors = listOf("Done", "Fertig", "Add", "Hinzufügen")
            .flatMap { label -> listOf(UiSelector().text(label), UiSelector().description(label)) }
        val deadline = SystemClock.uptimeMillis() + CONFIRM_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            confirmSelectors.forEach { selector ->
                val confirmButton = device.findObject(selector)
                if (confirmButton.exists()) {
                    confirmButton.click()
                    return
                }
            }
            SystemClock.sleep(POLL_INTERVAL)
        }
    }

    fun copyImageToDownloads(context: Context, filename: String) {
        runCatching {
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media.DISPLAY_NAME} = ?",
                arrayOf(filename)
            )
        }
        // Insert under a unique display name: repeated same-name inserts can fail with
        // "Failed to build unique file" when MediaStore is left with an orphaned file
        // (seen locally under the Orchestrator's clearPackageData). Every caller selects
        // the newest photo in the picker, never by name, so uniqueness is safe.
        val mimeType = when (filename.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            else -> "image/png"
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${System.currentTimeMillis()}_$filename")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
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

    companion object {
        private const val TILE_TIMEOUT = 5_000L
        private const val CONFIRM_TIMEOUT = 3_000L
        private const val POLL_INTERVAL = 250L
    }
}