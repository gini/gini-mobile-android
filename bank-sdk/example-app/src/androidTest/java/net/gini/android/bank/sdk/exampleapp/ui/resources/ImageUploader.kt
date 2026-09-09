package net.gini.android.bank.sdk.exampleapp.ui.resources

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import java.util.regex.Pattern


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

    /**
     * Confirms a multi-picture selection, returning whether the picker actually closed.
     *
     * Separate from [clickAddButton], which is left alone because the single-picture tests
     * depend on it. Two differences matter here:
     *
     * - the label is matched by **prefix**, because a multi-select picker puts the count in
     *   it ("Add (11)"), and `clickAddButton`'s exact match then finds nothing;
     * - it *reports* failure. `clickAddButton` returns silently when it matches nothing,
     *   which left the picker in the foreground and the caller's next Espresso call throwing
     *   `NoActivityResumedException` — an error about a missing activity rather than about a
     *   missing button.
     */
    fun confirmMultiSelection(): Boolean {
        val labels = listOf("Add", "Hinzufügen", "Done", "Fertig")
        val selectors = labels.flatMap { label ->
            listOf(
                UiSelector().textStartsWith(label),
                UiSelector().descriptionStartsWith(label)
            )
        } + UiSelector().resourceId(
            "com.google.android.providers.media.module:id/button_add"
        )
        val deadline = SystemClock.uptimeMillis() + CONFIRM_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            selectors.forEach { selector ->
                val button = device.findObject(selector)
                if (button.exists()) {
                    button.click()
                    device.waitForIdle()
                    return true
                }
            }
            SystemClock.sleep(POLL_INTERVAL)
        }
        return false
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
        // LIKE match: inserts use unique "<timestamp>_<filename>" display names, so an
        // exact match would never find the copies previous runs left behind.
        runCatching {
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("%$filename")
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

    /**
     * Puts [count] copies of the [filename] asset into the device's photo gallery.
     *
     * Separate from [copyImageToDownloads] because that one **deletes** every existing copy
     * of the file before inserting, so calling it in a loop would leave exactly one photo
     * behind. Here the delete happens once, up front.
     *
     * Each copy gets a unique display name — timestamp *plus index*, because eleven inserts
     * can land inside the same millisecond and MediaStore then refuses the duplicate with
     * "Failed to build unique file".
     */
    fun copyImagesToGallery(context: Context, filename: String, count: Int) {
        runCatching {
            context.contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("%$filename")
            )
        }
        val mimeType = when (filename.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            else -> "image/png"
        }
        val stamp = System.currentTimeMillis()
        repeat(count) { index ->
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "${stamp}_${index}_$filename")
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: return@repeat
            context.contentResolver.openOutputStream(uri)?.use { output ->
                context.assets.open(filename).use { input -> input.copyTo(output) }
            }
        }
    }

    /**
     * What the picker did when asked for more photos than it allows.
     *
     * @param accepted how many it ended up with, read from its own confirm label.
     * @param limitMessageShown whether it told the user about the limit.
     */
    data class PickerSelection(val accepted: Int, val limitMessageShown: Boolean)

    /**
     * Taps up to [count] photos in the open system picker and reports what happened.
     *
     * The SDK opens the picker through `PickMultipleVisualMedia(maxItems = 10)`
     * (`FileChooserFragment.kt:164`), so an eleventh selection is refused and Android shows a
     * snackbar — "Select up to 10 items" — instead. Both facts were confirmed by hand on a
     * real phone.
     *
     * The snackbar is checked **inside** the loop, right after each tap. It is transient, so
     * a check made after the loop finished would usually miss it and report the case as
     * failing for the wrong reason.
     */
    fun selectPhotosFromPicker(count: Int): PickerSelection {
        device.waitForIdle()
        val tapped = mutableSetOf<Pair<Int, Int>>()
        var limitMessageShown = false
        repeat(count) {
            // Re-found before every tap: selecting a photo reflows the grid, so coordinates
            // captured up front go stale and a later tap can land on one already chosen —
            // which would deselect it.
            val next = photoTileBounds().firstOrNull { it !in tapped } ?: return@repeat
            device.click(next.first, next.second)
            tapped += next
            device.waitForIdle()
            if (!limitMessageShown && isSelectionLimitMessageShown()) {
                limitMessageShown = true
            }
        }
        return PickerSelection(
            accepted = selectedCountFromConfirmLabel() ?: tapped.size,
            limitMessageShown = limitMessageShown
        )
    }

    /**
     * Whether the picker is showing its "you have hit the limit" snackbar.
     *
     * The text belongs to Android's photo picker, not to the Gini SDK, so it cannot be
     * resolved from a string resource the way the SDK's own copy is. Known wordings are
     * listed, with a loose pattern behind them for locales and picker versions not covered —
     * the same layered approach [uploadImageFromPhotos] uses for tile descriptions.
     */
    private fun isSelectionLimitMessageShown(): Boolean {
        val known = listOf(
            "Select up to 10 items", // English, confirmed on a real device
            "Wähle bis zu 10 Elemente aus" // German equivalent
        )
        if (known.any { device.findObject(UiSelector().textContains(it)).exists() }) return true
        // Any short message that mentions the limit — covers rewordings without matching the
        // confirm button, whose label is just "Add (10)".
        return device.findObject(
            UiSelector().textMatches("(?i).*(up to|only|maximum|max\\.?|bis zu).*10.*")
        ).exists()
    }

    /**
     * Closes the system picker if it is still open.
     *
     * The picker is a separate activity, so a test that leaves it in front hands the next
     * test a foreground that is not the app — which is how this suite produced a
     * `NoActivityResumedException` from an Espresso call that looked unrelated. Every test
     * that opens the picker without confirming a selection has to close it again.
     *
     * A no-op when no picker is showing, and capped at two back presses so it can never walk
     * out of the app itself.
     */
    fun dismissPicker() {
        repeat(MAX_DISMISS_PRESSES) {
            if (photoTileBounds().isEmpty()) return
            device.pressBack()
            device.waitForIdle()
        }
    }

    /** The number in the confirm button's label ("Add (10)" -> 10), or null if unreadable. */
    fun selectedCountFromConfirmLabel(): Int? =
        pickerConfirmLabel()?.let { label ->
            Regex("\\d+").find(label)?.value?.toIntOrNull()
        }

    /**
     * The confirm button's label, or null when it is not on screen.
     *
     * A multi-select picker puts the count in it ("Add (11)"), so this is the only reliable
     * read of how many pictures the picker thinks are selected — worth having in a failure
     * message when the app then does not complain about the count.
     */
    fun pickerConfirmLabel(): String? {
        val labels = listOf("Add", "Hinzufügen", "Done", "Fertig")
        labels.forEach { label ->
            val button = device.findObject(UiSelector().textStartsWith(label))
            if (button.exists()) return runCatching { button.text }.getOrNull() ?: label
        }
        return null
    }

    /**
     * Centre points of the photo tiles on screen, in grid order.
     *
     * Tries the same selectors [uploadImageFromPhotos] uses, in the same order: the legacy
     * media-module picker exposes a resource id, the newer Mainline picker exposes only a
     * localized content description, and the year fallback covers any language.
     */
    private fun photoTileBounds(): List<Pair<Int, Int>> {
        val selectors = listOf(
            By.res("com.google.android.providers.media.module:id/icon_thumbnail"),
            By.desc(Pattern.compile("Photo taken on.*")),
            By.desc(Pattern.compile("Foto wurde am.*")),
            By.desc(Pattern.compile(".*\\b20\\d\\d\\b.*"))
        )
        val deadline = SystemClock.uptimeMillis() + TILE_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            selectors.forEach { selector ->
                val found = device.findObjects(selector)
                if (found.isNotEmpty()) {
                    return found.map { it.visibleBounds.centerX() to it.visibleBounds.centerY() }
                }
            }
            SystemClock.sleep(POLL_INTERVAL)
        }
        return emptyList()
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

        /** Back presses allowed when closing the picker — never enough to leave the app. */
        private const val MAX_DISMISS_PRESSES = 2
    }
}