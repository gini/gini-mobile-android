package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test


/**
 * Test class for Review screen.
 *
 * Jira link for test case: [https://ginis.atlassian.net/browse/PM-24](https://ginis.atlassian.net/browse/PM-24)
 */
class ReviewScreenTests {
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
    private lateinit var idlingResource: SimpleIdlingResource

    private fun grantStoragePermission() {
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
    fun test1_reviewUploadedInvoice() {
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
    fun test2_pinchToZoomInvoice() {
        test1_reviewUploadedInvoice()
        reviewScreen.pinchToZoomInvoice()
    }

    @Test
    fun test3_clickCloseButtonForZoomedInvoice() {
        test2_pinchToZoomInvoice()
        reviewScreen.clickCancelButton()
        reviewScreen.assertReviewTitleIsDisplayed()
    }

    @Test
    fun test4_cancelUploadedInvoice() {
        test1_reviewUploadedInvoice()
        reviewScreen.clickCancelButton()
        mainScreen.assertDescriptionTitle()
    }

    @Test
    fun test5_deleteUploadedInvoice() {
        test1_reviewUploadedInvoice()
        reviewScreen.clickDeleteButton()
        captureScreen.assertCameraTitle()
    }

    @Test
    fun test6_addMorePagesToUploadedInvoice() {
        test1_reviewUploadedInvoice()
        reviewScreen.clickAddMorePagesButton()
        captureScreen.assertCameraTitle()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}