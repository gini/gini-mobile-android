package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ErrorScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.Properties

/**
 * Test class for Error Screens.
 */
class ErrorScreenTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val errorScreen = ErrorScreen()
    private lateinit var idlingResource: SimpleIdlingResource

    val testProperties = Properties().apply {
        getApplicationContext<Context>().resources.assets
            .open("test.properties").use { load(it) }
    }

    private fun cancelTestIfRunOnCi() {
        val ignoreTests = testProperties["ignoreLocalTests"] as String
        Assume.assumeTrue(ignoreTests != "true")
    }

    @Before
    fun setup() {
        cancelTestIfRunOnCi()
        idlingResource = SimpleIdlingResource(5000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    private fun clickPhotoPaymentButtonAndSkipOnboarding(){
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickCameraButton()
        idlingResource.waitForIdle()
    }

    @Test
    fun test1_verifyUploadErrorScreen() {
        clickPhotoPaymentButtonAndSkipOnboarding()

        val errorTextVisible = errorScreen.checkErrorTextDisplayed()
        assertEquals(true, errorTextVisible)
        val errorHeaderVisible = errorScreen.checkErrorHeaderTextDisplayed( "There was a problem with the upload")
        assertEquals(true, errorHeaderVisible)
        val errorTextViewVisible = errorScreen.checkErrorTextViewDisplayed("The document couldn’t be accepted. Please check if the image is sharp, the document contains payment information and has the right file type.")
        assertEquals(true, errorTextViewVisible)
    }

    @Test
    fun test2_verifyNetworkErrorScreen() {
        errorScreen.disconnectTheInternetConnection()
        clickPhotoPaymentButtonAndSkipOnboarding()
        idlingResource.waitForIdle()
        val errorTextVisible = errorScreen.checkErrorTextDisplayed()
        assertEquals(true, errorTextVisible)
        val errorHeaderVisible = errorScreen.checkErrorHeaderTextDisplayed( "There was a problem connecting to the internet")
        assertEquals(true, errorHeaderVisible)
        val errorTextViewVisible = errorScreen.checkErrorTextViewDisplayed("Please check your internet connection and try again later on.")
        assertEquals(true, errorTextViewVisible)
    }

    @Test
    fun test3_navigateToMainScreenByClickingEnterManuallyButton() {
        clickPhotoPaymentButtonAndSkipOnboarding()

        val enterManuallyButtonVisible = errorScreen.checkEnterManuallyButtonIsDisplayed()
        assertEquals(true, enterManuallyButtonVisible)
        errorScreen.clickEnterManuallyButton()
        val isDescriptionTitleVisible = mainScreen.assertDescriptionTitle()
        assertEquals(true, isDescriptionTitleVisible)
    }

    @Test
    fun test4_navigateToCameraScreenByClickingBackToCameraButton() {
        clickPhotoPaymentButtonAndSkipOnboarding()

        val backToCameraButtonVisible = errorScreen.checkBackToCameraButtonIsDisplayed()
        assertEquals(true, backToCameraButtonVisible)
        errorScreen.clickBackToCameraButton()
        val isScanTextVisible = captureScreen.checkScanTextDisplayed()
        assertEquals(true, isScanTextVisible)
    }

}