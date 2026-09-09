package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.os.Build
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.FileImportErrorDialog
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test


/**
 * Test class for Error dialogs of different File Import.
 */
class FileImportErrorDialogTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)


    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val fileImportErrorDialog = FileImportErrorDialog()
    private val pdfUploader = PdfUploader()
    private val imageUploader = ImageUploader()
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_importPasswordProtectedFileAndVerifyErrorDialogIsDisplayed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            pdfUploader.copyPdfToDownloads(context, "password-protected.pdf")
        }
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        idlingResource.waitForIdle()
        pdfUploader.uploadPdfFromFiles("password-protected.pdf")
        idlingResource.waitForIdle()
        val isContentPanelVisible =
            fileImportErrorDialog.checkContentIsDisplayed(net.gini.android.capture.R.string.gc_error_file_import_password_title,"Password protected documents cannot be analysed.")
        assertEquals(true, isContentPanelVisible)
    }

    /**
     * TC-025 / PP-3440 — trying to choose more than ten pictures is refused, and the user is
     * told the limit is ten.
     *
     * Both halves of the case's expected result — *"the pop-up with alert is displayed,
     * informing that max 10 pages / files can be selected"*:
     *
     * - the selection **stops at ten**, because the SDK opens the picker through
     *   `PickMultipleVisualMedia(maxItems = 10)` (`FileChooserFragment.kt:164`);
     * - Android shows the message **"Select up to 10 items"**.
     *
     * Both were confirmed by hand on a real device. The alert the case names is that
     * snackbar, shown by the picker — not a dialog raised by the SDK. Reading it as an SDK
     * dialog led to two wrong conclusions earlier: first that the eleventh tap had been
     * mis-aimed, then that the case was obsolete. It is neither.
     *
     * Note this never reaches `FileImportValidator.matchesCriteria(Uri[])`, which rejects
     * more than ten *files*: the picker refuses before the app is handed anything. That
     * validator is still reachable through the Files picker, which uses
     * `ACTION_OPEN_DOCUMENT` with no cap.
     */
    @Test
    fun test3_selectingMoreThanTenPicturesIsRefusedWithTheLimitMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        imageUploader.copyImagesToGallery(context, PICTURE_ASSET, ATTEMPTED_PICTURES)

        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()

        val selection = imageUploader.selectPhotosFromPicker(ATTEMPTED_PICTURES)
        // Closed before asserting, not after: the picker is a separate activity, and leaving
        // it in front would hand the next test a foreground that is not the app. Both facts
        // this test needs are already captured in `selection`, so dismissing first costs
        // nothing and also covers the path where an assertion below fails.
        imageUploader.dismissPicker()

        assertEquals(
            "Tapped $ATTEMPTED_PICTURES photos and expected the picker to stop at " +
                "$PICKER_MAX_PICTURES, matching PickMultipleVisualMedia(maxItems = " +
                "$PICKER_MAX_PICTURES). More than that means the cap is gone; fewer means a " +
                "tap deselected a photo.",
            PICKER_MAX_PICTURES,
            selection.accepted
        )
        assertEquals(
            "Expected the picker to say the limit is $PICKER_MAX_PICTURES — \"Select up to " +
                "$PICKER_MAX_PICTURES items\" — when the ${PICKER_MAX_PICTURES + 1}th photo " +
                "was tapped. The snackbar is transient, so a miss here can also mean it came " +
                "and went between the tap and the check.",
            true,
            selection.limitMessageShown
        )
    }

    @Test
    fun test2_importTooManyPagesFileAndVerifyErrorDialogIsDisplayed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            pdfUploader.copyPdfToDownloads(context, "too-many-pages.pdf")
        }
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        idlingResource.waitForIdle()
        pdfUploader.uploadPdfFromFiles("too-many-pages.pdf")
        idlingResource.waitForIdle()
        val isContentPanelVisible =
            fileImportErrorDialog.checkContentIsDisplayed(net.gini.android.capture.R.string.gc_error_file_import_page_count_title,"The document can only have a maximum of 10 pages.")
        assertEquals(true, isContentPanelVisible)
    }

    private companion object {
        /** One more than the picker's cap, so the cap itself is what is measured. */
        const val ATTEMPTED_PICTURES = 11

        /** `PickMultipleVisualMedia(maxItems = 10)` in `FileChooserFragment`. */
        const val PICKER_MAX_PICTURES = 10

        const val PICTURE_ASSET = "test_image.jpeg"
    }
}