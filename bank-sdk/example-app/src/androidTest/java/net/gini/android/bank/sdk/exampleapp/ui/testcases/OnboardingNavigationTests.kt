package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.capture.R
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The Xray "Onboarding Screens" cases (PP-3411..PP-3414): swiping through the pages and where
 * Skip and Get Started lead. PP-3410 (every page in order) is covered by [OnboardingScreenTests].
 *
 * Every case starts from a fresh install. The Test Orchestrator clears the package data before
 * each test (`clearPackageData`, locally and on BrowserStack), so the onboarding shows on the first
 * tap of the photo-payment button. Each test checks the expected page before it acts, so a run
 * where the app goes straight to the camera fails instead of passing.
 *
 * The camera permission is granted up front; the system pop-up is not part of these cases.
 */
class OnboardingNavigationTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()

    @Before
    fun setUp() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.waitForPage(R.string.gc_onboarding_align_corners_title)
    }

    /** PP-3411: slide through the 4 onboarding screens back and forth. */
    @Test
    fun test1_swipeForwardThroughAllPagesAndBack() {
        onboardingScreen.swipeToNextPage().waitForPage(R.string.gc_onboarding_lighting_title)
        onboardingScreen.swipeToNextPage().waitForPage(R.string.gc_onboarding_multipage_title)
        onboardingScreen.swipeToNextPage().waitForPage(R.string.gc_onboarding_qr_code_title)
        onboardingScreen.checkGetStartedDisplayed(true)
        onboardingScreen.checkNextAndSkipDisplayed(false)

        onboardingScreen.swipeToPreviousPage().waitForPage(R.string.gc_onboarding_multipage_title)
        onboardingScreen.checkNextAndSkipDisplayed(true)
        onboardingScreen.checkGetStartedDisplayed(false)
        onboardingScreen.swipeToPreviousPage().waitForPage(R.string.gc_onboarding_lighting_title)
        onboardingScreen.swipeToPreviousPage().waitForPage(R.string.gc_onboarding_align_corners_title)
    }

    /** PP-3412: Skip on the first screen ("Flat paper within the frame") opens the camera. */
    @Test
    fun test2_skipOnFirstPage_opensCamera() {
        onboardingScreen.clickSkipButton()
        assertTrue(captureScreen.checkScanTextDisplayed())
    }

    /** PP-3413: Skip on the second screen ("Good lighting") opens the camera. */
    @Test
    fun test3_skipOnSecondPage_opensCamera() {
        onboardingScreen.clickNextAndWaitFor(R.string.gc_onboarding_lighting_title)
        onboardingScreen.clickSkipButton()
        assertTrue(captureScreen.checkScanTextDisplayed())
    }

    /** PP-3414: Skip on the third screen ("Add multiple pages") opens the camera. */
    @Test
    fun test4_skipOnThirdPage_opensCamera() {
        onboardingScreen.clickNextAndWaitFor(R.string.gc_onboarding_lighting_title)
        onboardingScreen.clickNextAndWaitFor(R.string.gc_onboarding_multipage_title)
        onboardingScreen.clickSkipButton()
        assertTrue(captureScreen.checkScanTextDisplayed())
    }

    /** PP-3410 (end of the walk-through): Get Started on the last screen opens the camera. */
    @Test
    fun test5_getStartedOnLastPage_opensCamera() {
        onboardingScreen.clickNextAndWaitFor(R.string.gc_onboarding_lighting_title)
        onboardingScreen.clickNextAndWaitFor(R.string.gc_onboarding_multipage_title)
        onboardingScreen.clickNextAndWaitFor(R.string.gc_onboarding_qr_code_title)
        onboardingScreen.clickGetStartedButton()
        assertTrue(captureScreen.checkScanTextDisplayed())
    }
}
