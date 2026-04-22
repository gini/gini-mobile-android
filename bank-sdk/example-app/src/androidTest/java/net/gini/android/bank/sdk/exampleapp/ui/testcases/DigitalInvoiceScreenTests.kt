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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * Test class for Digital Invoice Screen.
 */
@Ignore("Excluded from CI - covered by bank-sdk.check.ui-tests.yml")
class DigitalInvoiceScreenTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get:Rule
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

    private fun clickPhotoPaymentAndUploadFile() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
    }

    @Test
    fun test1_digitalInvoiceOnboardingScreenIsDisplayed() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible =
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
    }

    @Test
    fun test2_disableToggleSwitchToRemoveItemFromList() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        val isItemEnabled = digitalInvoiceScreen.checkItemIsEnabledFromDigitalScreen()
        assertEquals(false, isItemEnabled)
    }


    @Test
    fun test3_enableToggleSwitchAndVerifyAnItemIsAddedBackToList() {
        mainScreen.clickSettingButton()
        configurationScreen.scrollToUICustomizationText()
        pressBack()
        clickPhotoPaymentAndUploadFile()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        idlingResource.waitForIdle()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        val isItemEnabled = digitalInvoiceScreen.checkItemIsEnabledFromDigitalScreen()
        assertEquals(true, isItemEnabled)
    }

    @Test
    fun test4_differenceInTotalAmountWithSwitchEnabledOrDisabled() {
        mainScreen.clickSettingButton()
        configurationScreen.scrollToUICustomizationText()
        pressBack()
        clickPhotoPaymentAndUploadFile()
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        idlingResource.waitForIdle()
        idlingResource.waitForIdle()

        val isTotalTitleVisible = digitalInvoiceScreen.checkTotalTitleIsDisplayed()
        assertEquals(true, isTotalTitleVisible)

        val isTotalPriceVisible = digitalInvoiceScreen.checkTotalPriceIsDisplayed()
        assertEquals(true, isTotalPriceVisible)

        digitalInvoiceScreen.storeInitialPrice()
        val hasTotalSumDistinct = digitalInvoiceScreen.verifyTotalSumValue()
        assertEquals(true, hasTotalSumDistinct)

        digitalInvoiceScreen.clickArticleSwitch()
        idlingResource.waitForIdle()

        val isTotalPriceReAppear = digitalInvoiceScreen.checkTotalPriceIsDisplayed()
        assertEquals(true, isTotalPriceReAppear)

        digitalInvoiceScreen.storeUpdatedPrice()
        val hasTotalSumChanged = digitalInvoiceScreen.verifyTotalSumValue()
        assertEquals(true, hasTotalSumChanged)
        idlingResource.waitForIdle()
    }

    @Test
    fun test5_verifyAdditionalChargesOnDigitalInvoice() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        val isDisplayed = digitalInvoiceScreen.assertOtherChargesDisplayed()
        idlingResource.waitForIdle()
        assertEquals(true, isDisplayed)
    }

    @Test
    fun test6_checkTransferSummaryButtonIsClickableAfterClickOnProceedButton() {
        mainScreen.clickSettingButton()
        configurationScreen.clickTransactionDocsSwitch()
        pressBack()
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickProceedButton()
        val isTransferSummaryButtonVisible =
            extractionScreen.checkTransferSummaryButtonIsClickable()
        assertEquals(true, isTransferSummaryButtonVisible)
    }

    @Test
    fun test7_clickHelpButtonAndVerifyContentOnHelpScreen() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickHelpButtonOnDigitalInvoiceScreen()
        val isHelpTextVisible = digitalInvoiceScreen.verifyHelpTextOnNextScreen()
        assertEquals(true, isHelpTextVisible)

        val isFirstTitleTextVisible = digitalInvoiceScreen.verifyFirstTitleOnHelpScreen()
        assertEquals(true, isFirstTitleTextVisible)
    }

    @Test
    fun test8_checkMainScreenTitleIsDisplayedAfterClickOnCancelButton() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickCancelButton()
        val isDescriptionTitleVisible = mainScreen.assertDescriptionTitle()
        assertEquals(true, isDescriptionTitleVisible)
    }

}