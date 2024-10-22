package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ConfigurationScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.DigitalInvoiceScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ExtractionScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test class for Digital Invoice Screen.
 */
class DigitalInvoiceScreenTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val configurationScreen = ConfigurationScreen()
    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val digitalInvoiceScreen = DigitalInvoiceScreen()
    private val extractionScreen = ExtractionScreen()
    private val pdfUploader = PdfUploader()
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(10000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test3_returnReasonDisplaysWhenToggleSwitchIsDisabled() {
        mainScreen.clickSettingButton()
        configurationScreen.scrollToUICustomizationText()
        configurationScreen.clickReturnReasonsDialogToEnable()
        pressBack()
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        idlingResource.waitForIdle()
        val isDisplayed = digitalInvoiceScreen.checkForReturnReasonsList()
        idlingResource.waitForIdle()
        assertEquals(true, isDisplayed)
    }
}