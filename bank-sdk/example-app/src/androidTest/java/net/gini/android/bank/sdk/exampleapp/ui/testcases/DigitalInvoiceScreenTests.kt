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
    fun test1_digitalInvoiceOnboardingScreenIsDisplayed() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
    }

    @Test
    fun test2_disableToggleSwitchToRemoveItemFromList() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        val isItemEnabled =  digitalInvoiceScreen.checkItemIsEnabledFromDigitalScreen()
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

    @Test
    fun test4_verifyCountOnReturnReasonsList() {
        test3_returnReasonDisplaysWhenToggleSwitchIsDisabled()
        val totalItemsOnReturnReason = 7
        val itemSize =  digitalInvoiceScreen.returnItemCountOnReturnReasonsList()
        assertEquals(totalItemsOnReturnReason, itemSize)
    }

    @Test
    fun test5_checkItemOnListIsDisabledAfterClickItemOnReturnReasonsList() {
        test3_returnReasonDisplaysWhenToggleSwitchIsDisabled()
        digitalInvoiceScreen.clickItemOnReturnReasonsList()
        idlingResource.waitForIdle()
        val isItemDisabled = digitalInvoiceScreen.checkItemIsDisabledFromDigitalScreen()
        assertEquals(true, isItemDisabled)
    }

    @Test
    fun test6_enableToggleSwitchAndVerifyAnItemIsAddedBackToList() {
        test5_checkItemOnListIsDisabledAfterClickItemOnReturnReasonsList()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickArticleSwitch()
        val isItemEnabled = digitalInvoiceScreen.checkItemIsEnabledFromDigitalScreen()
        assertEquals(true, isItemEnabled)
    }

    @Test
    fun test7_differenceInTotalAmountWithSwitchEnabledOrDisabled() {
        test5_checkItemOnListIsDisabledAfterClickItemOnReturnReasonsList()
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
    fun test8_verifyAdditionalChargesOnDigitalInvoice() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        val isDisplayed = digitalInvoiceScreen.assertOtherChargesDisplayed()
        idlingResource.waitForIdle()
        assertEquals(true, isDisplayed)
    }

    @Test
    fun test9_checkTransferSummaryButtonIsClickableAfterClickOnProceedButton() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickProceedButton()
        val isTransferSummaryButtonVisible = extractionScreen.checkTransferSummaryButtonIsClickable()
        assertEquals(true, isTransferSummaryButtonVisible)
    }

    @Test
    fun test10_clickHelpButtonAndVerifyContentOnHelpScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
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
    fun test11_checkMainScreenTitleIsDisplayedAfterClickOnCancelButton() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()
        digitalInvoiceScreen.clickCancelButton()
        val isDescriptionTitleVisible = mainScreen.assertDescriptionTitle()
        assertEquals(true, isDescriptionTitleVisible)
    }
}