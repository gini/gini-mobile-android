package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Rule
import org.junit.Test

/**
 * Test class for flash on/off on CaptureScreen.
 *
 * Jira link for test case: [https://ginis.atlassian.net/browse/PM-22](https://ginis.atlassian.net/browse/PM-22)
 */
class CaptureScreenTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val configurationScreen = ConfigurationScreen()
    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()

    @Test
    fun test1_flashIsOnByDefault() {
        mainScreen.clickSettingButton()
        configurationScreen.clickFlashToggleToEnable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOn()
    }

    @Test
    fun test2_flashEnabledWhenImageIsCaptured() {
        mainScreen.clickSettingButton()
        configurationScreen.clickFlashToggleToEnable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOn()
        captureScreen.clickCameraButton()
    }

    @Test
    fun test3_flashIsOffByDefault() {
        mainScreen.clickSettingButton()
        configurationScreen.assertFlashToggleIsDisable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOff()
    }

    @Test
    fun test4_flashDisabledWhenImageIsCaptured() {
        mainScreen.clickSettingButton()
        configurationScreen.assertFlashToggleIsDisable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOff()
        captureScreen.clickCameraButton()
    }

}