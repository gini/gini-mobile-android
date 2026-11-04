package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
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

        // Check for BrowserStack Custom_Files folder.
        val customFilesFolder = device.findObject(
            UiSelector().className("android.widget.TextView").text("Custom_Files")
        )
        if (customFilesFolder.waitForExists(2000)) {
            customFilesFolder.click()
            device.waitForIdle()
            clickFile(file)
            return
        }

        // BrowserStack places Android non-media files in Downloads.
        // Navigate there from the Recents view.
        navigateToDownloads()

        clickFile(file)
    }

    /**
     * Taps [file] in the picker's file list.
     *
     * The file is looked up by its name first, and the list is scrolled only when the file is
     * not on screen and the picker actually has a scrollable list. The Samsung tablet picker
     * (Galaxy Tab, Android 16) shows Downloads as a grid with no scrollable container, so the
     * earlier UiScrollable-only lookup failed there with
     * `UiObjectNotFoundException: UiSelector[SCROLLABLE=true]` before looking for the file.
     */
    private fun clickFile(file: String) {
        val fileItem = device.findObject(UiSelector().text(file))
        if (fileItem.waitForExists(FILE_TIMEOUT_MS)) {
            fileItem.click()
            return
        }
        val fileList = UiScrollable(UiSelector().scrollable(true))
        if (fileList.exists()) {
            fileList.getChildByText(UiSelector().text(file), file).click()
            return
        }
        throw UiObjectNotFoundException("\"$file\" is not shown in the file picker, and it has no list to scroll")
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

    /** Removes every file named [filename] from Downloads that this app is allowed to delete. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteFromDownloads(context: Context, filename: String) {
        context.contentResolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(filename)
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun copyPdfToDownloads(context: Context, filename: String) {
        deleteFromDownloads(context, filename)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return
        context.contentResolver.openOutputStream(uri)?.use { output ->
            context.assets.open(filename).use { input -> input.copyTo(output) }
        }
    }

    /**
     * Writes a PDF of exactly [sizeBytes] into Downloads, for the file-size limit test. It is
     * generated rather than shipped as an asset, so the test APK uploaded to BrowserStack stays
     * small.
     *
     * Only the header and the end marker are real PDF; the rest is one long comment. That is
     * enough because the SDK checks the size before it parses the PDF
     * (`FileImportValidator.matchesCriteria`).
     *
     * Returns the new file's Uri, so the caller can delete exactly this file. Deleting by name is
     * not enough: a copy left by an earlier install of the app cannot be deleted by this one, and
     * MediaStore then saves the new file as "name (1).pdf". Pass a [filename] that is new on every
     * run for the same reason.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun writeLargePdfToDownloads(context: Context, filename: String, sizeBytes: Long): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = requireNotNull(
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        ) { "Could not create $filename in Downloads" }
        val header = "%PDF-1.4\n%".toByteArray()
        val footer = "\n%%EOF\n".toByteArray()
        requireNotNull(context.contentResolver.openOutputStream(uri)) {
            "Could not open $filename for writing"
        }.use { output ->
            output.write(header)
            val padding = ByteArray(PADDING_CHUNK_BYTES) { 'x'.code.toByte() }
            var remaining = sizeBytes - header.size - footer.size
            while (remaining > 0) {
                val chunk = minOf(remaining, padding.size.toLong()).toInt()
                output.write(padding, 0, chunk)
                remaining -= chunk
            }
            output.write(footer)
        }
        return uri
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

    private companion object {
        /** Waits for the picker to list the folder's files before falling back to scrolling. */
        const val FILE_TIMEOUT_MS = 5_000L

        const val PADDING_CHUNK_BYTES = 64 * 1024
    }
}
