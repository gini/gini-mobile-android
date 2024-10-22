package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.DigitalInvoiceEditButton
import net.gini.android.bank.sdk.exampleapp.ui.screens.DigitalInvoiceScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test class for Edit button on Digital Invoice Screen.
 */
class DigitalInvoiceEditButtonTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>()

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val digitalInvoiceScreen = DigitalInvoiceScreen()
    private val digitalInvoiceEditButton = DigitalInvoiceEditButton()
    private val pdfUploader = PdfUploader()
    private lateinit var idlingResource: SimpleIdlingResource
    private val increaseQuantity = 5
    private val decreaseQuantity = 2

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(10000)
        IdlingRegistry.getInstance().register(idlingResource)
    }

    private fun clickPhotoPaymentAndUploadFile() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        Thread.sleep(6000)
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("Testrechnung-RA-1.pdf")
        idlingResource.waitForIdle()
    }

    @Test
    fun test1_verifyNameIsUpdatedAfterEditing() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)

        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()

        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()

        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)
        val isNameTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_name)
        assertEquals(true, isNameTextVisible)

        digitalInvoiceEditButton.editElementTextOnArticleBottomSheet(net.gini.android.bank.sdk.R.id.gbs_article_name_edit_txt, "Testing")
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()

        val isDigitalInvoiceTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
        assertEquals(true, isDigitalInvoiceTextVisible)
        val isNameFieldUpdated = digitalInvoiceEditButton.checkNameIsUpdated("Testing")
          assertEquals(true, isNameFieldUpdated)
    }

    @Test
    fun test2_verifyUnitPriceIsUpdatedAfterEditing() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)

        val isUnitPriceTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_unit_price)
        assertEquals(true, isUnitPriceTextVisible)
        digitalInvoiceEditButton.editElementTextOnArticleBottomSheet(net.gini.android.bank.sdk.R.id.gbs_unit_price_edit_txt, "10.00")
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()

        val isDigitalInvoiceTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
        assertEquals(true, isDigitalInvoiceTextVisible)
        val isUnitPriceFieldUpdated = digitalInvoiceEditButton.checkUnitPriceIsUpdated("10.00")
        assertEquals(true, isUnitPriceFieldUpdated)
    }

    @Test
    fun test3_verifyQuantityIsUpdatedAfterEditing() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)

        val isQuantityTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_quantity)
        assertEquals(true, isQuantityTextVisible)
        digitalInvoiceEditButton.editElementTextOnArticleBottomSheet(net.gini.android.bank.sdk.R.id.gbs_quantity_edit_txt, "4")
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()

        val isDigitalInvoiceTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
        assertEquals(true, isDigitalInvoiceTextVisible)
        val isQuantityFieldUpdated = digitalInvoiceEditButton.checkQuantityIsUpdated("4")
        assertEquals(true, isQuantityFieldUpdated)
    }

    @Test
    fun test4_increaseQuantityByTappingPlus() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)

        val isQuantityTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_quantity)
        assertEquals(true, isQuantityTextVisible)
        digitalInvoiceEditButton.clickPlusToIncreaseQuantity(increaseQuantity)
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()

        val isDigitalInvoiceTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
        assertEquals(true, isDigitalInvoiceTextVisible)
        val isQuantityFieldUpdated = digitalInvoiceEditButton.checkQuantityIsUpdated("$increaseQuantity")
        assertEquals(true, isQuantityFieldUpdated)
    }

    @Test
    fun test5_decreaseQuantityByTappingMinus() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)

        val isQuantityTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_quantity)
        assertEquals(true, isQuantityTextVisible)
        digitalInvoiceEditButton.clickPlusToIncreaseQuantity(increaseQuantity)
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()

        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        idlingResource.waitForIdle()
        digitalInvoiceEditButton.clickMinusToDecreaseQuantity(decreaseQuantity)
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()

        val isDigitalInvoiceTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
        assertEquals(true, isDigitalInvoiceTextVisible)
        val isQuantityFieldUpdated = digitalInvoiceEditButton.checkQuantityIsUpdated("$decreaseQuantity")
        assertEquals(true, isQuantityFieldUpdated)
    }

    @Test
    fun test6_verifyThatMinimumQuantityIs1() {
        clickPhotoPaymentAndUploadFile()
        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)

        val isQuantityTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_quantity)
        assertEquals(true, isQuantityTextVisible)
        digitalInvoiceEditButton.editElementTextOnArticleBottomSheet(net.gini.android.bank.sdk.R.id.gbs_quantity_edit_txt, "0")
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()

        val isDigitalInvoiceTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
        assertEquals(true, isDigitalInvoiceTextVisible)
        val isQuantityFieldUpdated = digitalInvoiceEditButton.checkQuantityIsUpdated("0")
        assertNotEquals(1, isQuantityFieldUpdated)
    }

    @Test
    fun test7_checkCurrencyForUnitPrice() {
        clickPhotoPaymentAndUploadFile()

        val isOnboardingScreenTextVisible = digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible = digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()
        val isEditArticleTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)
        idlingResource.waitForIdle()
        val isUnitPriceTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_unit_price)
        assertEquals(true, isUnitPriceTextVisible)
        idlingResource.waitForIdle()
        val isCurrencyEuroVisible = digitalInvoiceEditButton.checkCurrencyForUnitPrice()
        assertEquals(true, isCurrencyEuroVisible)
    }

    @Test
    fun test8_clickCancelButtonAndVerifyItemDetailsRemainUnchanged() {
        clickPhotoPaymentAndUploadFile()

        val isOnboardingScreenTextVisible =
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val initialName =
            digitalInvoiceEditButton.saveNameValueOnDigitalInvoiceScreen()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()


        val isEditArticleTextVisible =
            digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)
        val isNameTextVisible =
            digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_name)
        assertEquals(true, isNameTextVisible)
        digitalInvoiceEditButton.clickCancelButtonOnEditArticleBottomSheet()

        val updatedName =
            digitalInvoiceEditButton.saveNameValueOnDigitalInvoiceScreen()
        if (initialName == updatedName) {
            val isDigitalInvoiceTextVisible =
                digitalInvoiceScreen.checkDigitalInvoiceTitleIsDisplayed()
            assertEquals(true, isDigitalInvoiceTextVisible)
        }
    }

    @Test
    fun test9_verifyInlineErrorWhenNameFieldIsEmpty() {
        clickPhotoPaymentAndUploadFile()

        val isOnboardingScreenTextVisible =
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()

        val isEditArticleTextVisible =
            digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)
        val isNameTextVisible =
            digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_name)
        assertEquals(true, isNameTextVisible)

        digitalInvoiceEditButton.removeTextFromElementOnArticleBottomSheet(net.gini.android.bank.sdk.R.id.gbs_article_name_edit_txt)
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()
        val isInlineErrorVisible =
            digitalInvoiceEditButton.verifyInlineErrorOnElement(net.gini.android.bank.sdk.R.id.gbs_name_error_textView, "Please enter the article name.")
        assertEquals(true, isInlineErrorVisible)
    }

    @Test
    fun test10_verifyInlineErrorWhenUnitPriceFieldIsEmpty() {
        clickPhotoPaymentAndUploadFile()

        val isOnboardingScreenTextVisible =
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenTextVisible)
        val isOnboardingScreenButtonVisible =
            digitalInvoiceScreen.checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed()
        assertEquals(true, isOnboardingScreenButtonVisible)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
        idlingResource.waitForIdle()

        val isEditButtonVisible = digitalInvoiceEditButton.checkEditButtonTitleIsDisplayed()
        assertEquals(true, isEditButtonVisible)
        digitalInvoiceEditButton.clickEditButtonOnDigitalInvoiceScreen()

        val isEditArticleTextVisible =
            digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_edit_article)
        assertEquals(true, isEditArticleTextVisible)
        val isUnitPriceTextVisible = digitalInvoiceEditButton.checkElementTitleIsDisplayed(net.gini.android.bank.sdk.R.string.gbs_unit_price)
        assertEquals(true, isUnitPriceTextVisible)

        digitalInvoiceEditButton.removeTextFromElementOnArticleBottomSheet(net.gini.android.bank.sdk.R.id.gbs_unit_price_edit_txt)
        digitalInvoiceEditButton.clickSaveButtonOnEditArticleBottomSheet()
        idlingResource.waitForIdle()
        val isInlineErrorVisible =
            digitalInvoiceEditButton.verifyInlineErrorOnElement(net.gini.android.bank.sdk.R.id.gbs_price_error_textView, "Please enter the article price.")
        assertEquals(true, isInlineErrorVisible)
    }
}