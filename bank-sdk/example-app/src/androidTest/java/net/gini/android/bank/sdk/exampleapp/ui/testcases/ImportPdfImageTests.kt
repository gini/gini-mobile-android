package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ExtractionScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test class for flash on/off on CaptureScreen.
 *
 * Jira link for test case: [https://ginis.atlassian.net/browse/PM-21](https://ginis.atlassian.net/browse/PM-21)
 *
 * No automation for step 4 and 5, since it is not part of our SDK
 */
class ImportPdfImageTests {
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

    @Test
    fun grantStoragePermission(): Unit {
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant net.gini.android.bank.sdk.exampleapp android.permission.READ_EXTERNAL_STORAGE")
        device.executeShellCommand("pm grant net.gini.android.bank.sdk.exampleapp android.permission.WRITE_EXTERNAL_STORAGE")
    }

    @Before
    fun setup() {
        grantStoragePermission()
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_uploadPhoto() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
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

    @Test
    fun test3_uploadPdf() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles()
        idlingResource.waitForIdle()
        extractionScreen.checkTransferSummaryButtonIsClickable()
    }
}