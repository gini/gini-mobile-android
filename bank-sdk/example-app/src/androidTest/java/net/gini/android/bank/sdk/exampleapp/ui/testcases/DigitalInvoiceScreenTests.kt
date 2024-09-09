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
        idlingResource = SimpleIdlingResource(5000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun test1_digitalInvoiceOnboardingScreenIsDisplayed() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        digitalInvoiceScreen.displayDigitalInvoiceTextOnOnboardingScreen()
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
    }

    @Test
    fun test2_disableToggleSwitchToRemoveItemFromList() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertDigitalInvoiceText()
        digitalInvoiceScreen.clickArticleSwitch()
        val isItemEnabled =  digitalInvoiceScreen.checkItemIsDisabledFromDigitalScreen()
        assertEquals(false, isItemEnabled)
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
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertDigitalInvoiceText()
        digitalInvoiceScreen.clickArticleSwitch()
        digitalInvoiceScreen.checkForReturnReasonsList()
    }

    @Test
    fun test4_verifyCountOnReturnReasonsList() {
        test3_returnReasonDisplaysWhenToggleSwitchIsDisabled()
        digitalInvoiceScreen.checkItemCountOnReturnReasonsList()
    }

    @Test
    fun test5_clickOnItemFromReturnReasonsList() {
        test3_returnReasonDisplaysWhenToggleSwitchIsDisabled()
        digitalInvoiceScreen.clickItemOnReturnReasonsList()
        idlingResource.waitForIdle()
        val isItemEnabled = digitalInvoiceScreen.checkItemIsDisabledFromDigitalScreen()
        assertEquals(false, isItemEnabled)
    }

    @Test
    fun test6_enableToggleSwitchToAddItemBackToList() {
        test5_clickOnItemFromReturnReasonsList()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertDigitalInvoiceText()
        digitalInvoiceScreen.clickArticleSwitch()
        val isItemDisabled =digitalInvoiceScreen.checkItemIsEnabledFromDigitalScreen()
        assertEquals(true, isItemDisabled)
    }

    @Test
    fun test7_differenceInTotalAmountWithSwitchEnabledOrDisabled() {
        test5_clickOnItemFromReturnReasonsList()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.checkTotalTitleIsDisplayed()
        digitalInvoiceScreen.checkTotalPriceIsDisplayed()
        digitalInvoiceScreen.storeInitialPrice()
        digitalInvoiceScreen.clickArticleSwitch()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.checkTotalPriceIsDisplayed()
        digitalInvoiceScreen.storeUpdatedPrice()
        digitalInvoiceScreen.assertPriceHasChanged()
        idlingResource.waitForIdle()
    }

    @Test
    fun test8_verifyAdditionalChargesOnDigitalInvoice() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertOtherChargesDisplayed()
    }

    @Test
    fun test9_clickProceedButtonOnDigitalInvoiceScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertDigitalInvoiceText()
        digitalInvoiceScreen.clickProceedButton()
        extractionScreen.checkTransferSummaryButtonIsClickable()
    }

    @Test
    fun test10_clickHelpButtonAndVerifyContentOnHelpScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertDigitalInvoiceText()
        digitalInvoiceScreen.clickHelpButtonOnDigitalInvoiceScreen()
        digitalInvoiceScreen.verifyHelpTextOnNextScreen()
        digitalInvoiceScreen.verifyFirstTitleOnHelpScreen()
    }

    @Test
    fun test11_clickCancelButtonOnDigitalInvoiceScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        digitalInvoiceScreen.displayGetStartedButtonOnOnboardingScreen()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.assertDigitalInvoiceText()
        digitalInvoiceScreen.clickCancelButton()
        mainScreen.assertDescriptionTitle()
    }
}