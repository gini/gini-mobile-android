package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ExtractionScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Properties


/**
 * Test class for Extraction screen.
 */
class ExtractionScreenTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val imageUploader = ImageUploader()
    private val reviewScreen = ReviewScreen()
    private val extractionScreen = ExtractionScreen()
    private lateinit var idlingResource: SimpleIdlingResource

    private fun grantStoragePermission() {
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("pm grant net.gini.android.bank.sdk.exampleapp android.permission.READ_EXTERNAL_STORAGE")
        device.executeShellCommand("pm grant net.gini.android.bank.sdk.exampleapp android.permission.WRITE_EXTERNAL_STORAGE")
    }

    val testProperties = Properties().apply {
        getApplicationContext<Context>().resources.assets
            .open("test.properties").use { load(it) }
    }

    @Before
    fun setup() {
        cancelTestIfRunOnCi()
        grantStoragePermission()
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_clickTransferSummaryButton() {
        chooseAndUploadImageFromPhotos()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
        extractionScreen.clickTransferSummaryButton()
    }

    @Test
    fun test2_editIbanFieldAndCheckTransferSummaryButtonClickable() {
        chooseAndUploadImageFromPhotos()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
        extractionScreen.editTransferSummaryFields("iban", "DE48120400000180115890")
        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test3_editAmountFieldAndCheckTransferSummaryButtonClickable() {
        chooseAndUploadImageFromPhotos()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
        extractionScreen.editTransferSummaryFields("amountToPay", "200:EUR")
        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test4_editPurposeFieldAndCheckTransferSummaryButtonClickable() {
        chooseAndUploadImageFromPhotos()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
        extractionScreen.editTransferSummaryFields("paymentPurpose", "Rent")
        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test5_editRecipientFieldAndCheckTransferSummaryButtonClickable() {
        chooseAndUploadImageFromPhotos()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
        extractionScreen.editTransferSummaryFields("paymentRecipient", "Zalando Gmbh & Co. KG")
        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test6_pressBackOnTransferSummaryAndShowsMainScreenOnSubsequentLaunches() {
        chooseAndUploadImageFromPhotos()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        reviewScreen.clickProcessButton()
        pressBack()
        mainScreen.assertDescriptionTitle()
    }

    private fun chooseAndUploadImageFromPhotos() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
    }

    private fun cancelTestIfRunOnCi() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}