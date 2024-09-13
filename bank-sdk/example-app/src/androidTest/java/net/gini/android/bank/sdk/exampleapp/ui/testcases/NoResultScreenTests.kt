package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.NoResultScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test


/**
 * Test class for No Result screen.
 */
class NoResultScreenTests {

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
    private val noResultScreen = NoResultScreen()
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(5000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_uploadInvalidImageAndClickEnterManuallyButton_NavigatesToMainScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        noResultScreen.checkNoResultTitleIsDisplay()
        noResultScreen.checkNoResultHeaderIsDisplay()
        noResultScreen.checkUsefulTipsIsDisplay()
        noResultScreen.checkGoodLightingTitleIsDisplay()
        noResultScreen.checkFlattenTitleIsDisplay()
        noResultScreen.checkParallelTitleIsDisplay()
        noResultScreen.checkPositionInTheFrameTitleIsDisplay()
        noResultScreen.checkMultiPagesTitleIsDisplay()
        idlingResource.waitForIdle()
        noResultScreen.checkEnterManuallyButtonIsDisplay()
        noResultScreen.clickEnterManuallyButton()
        mainScreen.assertDescriptionTitle()
        idlingResource.waitForIdle()
    }

    @Test
    fun test2_uploadInvalidPdfAndClickEnterManuallyButton_NavigatesToMainScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("test-pdf-no-results.pdf")
        idlingResource.waitForIdle()
        noResultScreen.checkNoResultTitleIsDisplay()
        noResultScreen.checkNoResultHeaderIsDisplay()
        noResultScreen.checkSupportedFormatIsDisplay()
        noResultScreen.checkComputerGeneratedInvoiceIsDisplayed()
        noResultScreen.checkNotSupportedFormatIsDisplay()
        noResultScreen.checkHandwritingIsDisplay()
        idlingResource.waitForIdle()
        noResultScreen.checkEnterManuallyButtonIsDisplay()
        noResultScreen.clickEnterManuallyButton()
        mainScreen.assertDescriptionTitle()
    }
}