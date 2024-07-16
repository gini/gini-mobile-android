package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for onboarding screen flow.
 *
 * Jira link for test case: [https://ginis.atlassian.net/browse/PM-18](https://ginis.atlassian.net/browse/PM-18)
 */

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setUp() {
        idlingResource = SimpleIdlingResource(2000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_assertFlatPaperTitle() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.checkOnboardingScreenTitle(net.gini.android.capture.R.string.gc_onboarding_align_corners_title)
    }

    @Test
    fun test2_clickNextButtonAndAssertGoodLightningTitle() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickNextButton()
        onboardingScreen.checkOnboardingScreenTitle(net.gini.android.capture.R.string.gc_onboarding_lighting_title)
    }

    @Test
    fun test3_clickNextButtonAndAssertAddMultiPageTitle() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickNextButton()
        idlingResource.waitForIdle()
        onboardingScreen.clickNextButton()
        onboardingScreen.checkOnboardingScreenTitle(net.gini.android.capture.R.string.gc_onboarding_multipage_title)
    }

    @Test
    fun test4_clickNextButtonAndAssertQRCodeTitle() {
        mainScreen.clickPhotoPaymentButton()
        idlingResource.waitForIdle()
        onboardingScreen.clickNextButton()
        idlingResource.waitForIdle()
        onboardingScreen.clickNextButton()
        idlingResource.waitForIdle()
        onboardingScreen.clickNextButton()
        idlingResource.waitForIdle()
        onboardingScreen.checkOnboardingScreenTitle(net.gini.android.capture.R.string.gc_onboarding_qr_code_title)
    }

    @Test
    fun test5_assertNextButton() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.checkNextButtonText()
    }

    @Test
    fun test6_assertSkipButton() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.checkSkipButtonText()
    }

    @Test
    fun test7_assertGetStartedButton() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickNextButton()
        onboardingScreen.clickNextButton()
        onboardingScreen.clickNextButton()
        onboardingScreen.checkGetStartedButton()
    }

    @Test
    fun test8a_assertOnboardingOnFirstLaunch() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.checkOnboardingScreenTitle(net.gini.android.capture.R.string.gc_onboarding_align_corners_title)
        onboardingScreen.checkSkipButtonText()
    }

    @Test
    fun test8b_skipsOnboardingAndShowsCameraScreenOnSubsequentLaunches() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        pressBack()
        mainScreen.assertDescriptionTitle()
        mainScreen.clickPhotoPaymentButton()
        captureScreen.assertCameraTitle()
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}