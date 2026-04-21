package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test class for flash on/off on CaptureScreen.
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
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(10000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    private fun assumeFlashAvailable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val hasFlash = try {
            cameraManager.cameraIdList.any { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
                        characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            false
        }
        assumeTrue("Skipping: device does not have camera flash hardware", hasFlash)
    }


    @Test
    fun test1_flashIsOnByDefault() {
        assumeFlashAvailable()
        mainScreen.clickSettingButton()
        configurationScreen.scrollToAnalysisText()
        configurationScreen.clickFlashToggleToEnable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOn()
    }

    @Test
    fun test2_flashEnabledWhenImageIsCaptured() {
        assumeFlashAvailable()
        mainScreen.clickSettingButton()
        configurationScreen.scrollToAnalysisText()
        configurationScreen.clickFlashToggleToEnable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOn()
    }

    @Test
    fun test3_flashIsOffByDefault() {
        assumeFlashAvailable()
        mainScreen.clickSettingButton()
        configurationScreen.scrollToAnalysisText()
        configurationScreen.assertFlashToggleIsDisable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOff()
    }

    @Test
    fun test4_flashDisabledWhenImageIsCaptured() {
        assumeFlashAvailable()
        mainScreen.clickSettingButton()
        configurationScreen.scrollToAnalysisText()
        configurationScreen.assertFlashToggleIsDisable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.assertFlashIconIsDisplayed()
        captureScreen.assertFlashIconIsOff()
        captureScreen.clickCameraButton()
    }

}