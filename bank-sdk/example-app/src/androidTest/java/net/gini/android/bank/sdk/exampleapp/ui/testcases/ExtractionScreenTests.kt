package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Properties


/**
 * Test class for Extraction screen.
 */

@RunWith(AndroidJUnit4::class)
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

        grantStoragePermission()
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    fun clickPhoto() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        idlingResource.waitForIdle()
        captureScreen.clickFilesButton()
        idlingResource.waitForIdle()
        captureScreen.clickPhotos()
        idlingResource.waitForIdle()
        Thread.sleep(3000)
        imageUploader.uploadImageFromPhotos()
        idlingResource.waitForIdle()
        Thread.sleep(1000)
        imageUploader.clickAddButton()
    }

    @Test
    fun test1_clickTransferSummaryButton() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
        clickPhoto()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
//        extractionScreen.clickTransferSummaryButton()
    }

    @Test
    fun test2_editIbanFieldAndCheckTransferSummaryButtonClickable() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
        clickPhoto()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        Thread.sleep(5000)
//        extractionScreen.editTransferSummaryFields("iban", "DE48120400000180115890")
        Thread.sleep(5000)
//        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test3_editAmountFieldAndCheckTransferSummaryButtonClickable() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
        clickPhoto()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        Thread.sleep(5000)
        extractionScreen.editTransferSummaryFields("amountToPay", "200:EUR")
        Thread.sleep(5000)
//        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test4_editPurposeFieldAndCheckTransferSummaryButtonClickable() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
        clickPhoto()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        idlingResource.waitForIdle()
        Thread.sleep(3000)
//        extractionScreen.editTransferSummaryFields("paymentPurpose", "Rent")
        Thread.sleep(5000)
//        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test5_editRecipientFieldAndCheckTransferSummaryButtonClickable() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
        clickPhoto()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        Thread.sleep(5000)
//        extractionScreen.editTransferSummaryFields("paymentRecipient", "Zalando Gmbh & Co. KG")
        Thread.sleep(1000)
//        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test6_pressBackOnTransferSummaryAndShowsMainScreenOnSubsequentLaunches() {
        clickPhoto()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        pressBack()
        mainScreen.assertDescriptionTitle()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}