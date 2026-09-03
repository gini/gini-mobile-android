package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import net.gini.android.bank.sdk.GiniBank
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.RetryRule
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ExtractionScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test class for Import PDF and Photos.
 *
 * No automation for step 4 and 5, since it is not part of our SDK
 */
class ImportPdfImageTests {
    @get:Rule(order = Int.MIN_VALUE)
    val retryRule = RetryRule()

    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val imageUploader = ImageUploader()
    private val pdfUploader = PdfUploader()
    private val reviewScreen = ReviewScreen()
    private val extractionScreen = ExtractionScreen()
    private lateinit var idlingResource: SimpleIdlingResource


    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    /**
     * Makes the SDK attach the document without asking.
     *
     * With always-attach off, `CaptureFlowFragment.tryShowAttachDocToTransactionDialog` puts an
     * "Add an attachment to this transaction?" dialog over the extraction screen, so the
     * transfer-summary button never becomes visible.
     *
     * Two constraints decide where this can be called from:
     *  - `GiniBank.giniTransactionDocs` only exists after `setCaptureConfiguration()`, which the
     *    example app calls when the Photo payment flow starts — so this cannot run in `@Before`.
     *  - the value is persisted in a DataStore, so setting it once here holds for the rest of the
     *    flow even though `setCaptureConfiguration()` recreates the transaction-docs instance.
     *
     * Setting the value beats toggling the switch in the settings screen: a toggle flips whatever
     * the current state happens to be, which differs between a local device (the setting persists
     * between runs) and BrowserStack (`clearPackageData` resets it), and it avoids the
     * settings-screen `pressBack()` that makes ProductTagConfigurationTests throw
     * NoActivityResumedException.
     */
    private fun attachTransactionDocsWithoutAsking() {
        runBlocking {
            GiniBank.giniTransactionDocs.transactionDocsSettings.setAlwaysAttachSetting(true)
        }
    }

    @Test
    fun test1_uploadPhoto() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
    }

    @Test
    fun test2_cancelUploadedPhoto() {
        test1_uploadPhoto()
        reviewScreen.clickCancelButton()
        mainScreen.assertDescriptionTitle()
    }

    /**
     * Imports a PDF that BrowserStack injected into the device's Downloads folder
     * (uploaded as `SamplePDF` by `bs_build_and_upload.sh`), and asserts the SDK
     * actually analysed it.
     *
     * Unlike an image, a PDF skips the review step and goes straight to analysis, so the
     * extraction screen — not the review screen — is what proves the import worked.
     */
    @Test
    fun test3_uploadPdf() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        attachTransactionDocsWithoutAsking()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("sample.pdf")
        idlingResource.waitForIdle()
        assertEquals(true, extractionScreen.checkTransferSummaryButtonIsClickable())
    }
}