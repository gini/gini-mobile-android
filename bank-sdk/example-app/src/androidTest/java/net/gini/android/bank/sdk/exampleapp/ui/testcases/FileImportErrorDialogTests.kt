package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
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
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_importPasswordProtectedFileAndVerifyErrorDialogIsDisplayed() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        idlingResource.waitForIdle()
        pdfUploader.uploadPdfFromFiles("password-protected.pdf")
        idlingResource.waitForIdle()
        val isContentPanelVisible =
            fileImportErrorDialog.checkContentIsDisplayed(net.gini.android.capture.R.string.gc_error_file_import_password_title,"Password protected documents cannot be analysed.")
        assertEquals(true, isContentPanelVisible)
    }

    @Test
    fun test2_importTooManyPagesFileAndVerifyErrorDialogIsDisplayed() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        idlingResource.waitForIdle()
        pdfUploader.uploadPdfFromFiles("too-many-pages.pdf")
        idlingResource.waitForIdle()
        val isContentPanelVisible =
            fileImportErrorDialog.checkContentIsDisplayed(net.gini.android.capture.R.string.gc_error_file_import_page_count_title,"The document can only have a maximum of 10 pages.")
        assertEquals(true, isContentPanelVisible)
    }

    @Test
    fun test3_openFilesAndImportLargeSizeFileAndVerifyErrorDialogIsDisplayed() {
        fileImportErrorDialog.clickFilesApp()
        idlingResource.waitForIdle()
        fileImportErrorDialog.clickTooManyPages("file-size-too-large.png")
        idlingResource.waitForIdle()
        fileImportErrorDialog.openWith()
        idlingResource.waitForIdle()
        fileImportErrorDialog.checkToastMessage()
    }
}