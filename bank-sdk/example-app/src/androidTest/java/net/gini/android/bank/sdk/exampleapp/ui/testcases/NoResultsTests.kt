package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.Manifest
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.rule.GrantPermissionRule
import net.gini.android.bank.sdk.exampleapp.ui.MainActivity
import net.gini.android.bank.sdk.exampleapp.ui.resources.ImageUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.PdfUploader
import net.gini.android.bank.sdk.exampleapp.ui.resources.SimpleIdlingResource
import net.gini.android.bank.sdk.exampleapp.ui.screens.CaptureScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.MainScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.NoResultScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.OnboardingScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReviewScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
/**
 * Test class for No Result screen.
 */
class NoResultsTests {
    @get:Rule
    val activityRule = activityScenarioRule<MainActivity>() 

    @get: Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val mainScreen = MainScreen()
    private val onboardingScreen = OnboardingScreen()
    private val captureScreen = CaptureScreen()
    private val imageUploader = ImageUploader()
    private val pdfUploader = PdfUploader()
    private val reviewScreen = ReviewScreen()
    private val noResultScreen = NoResultScreen()
    private lateinit var idlingResource: SimpleIdlingResource

    @Before
    fun setup() {
        idlingResource = SimpleIdlingResource(5000)
        IdlingRegistry.getInstance().register(idlingResource)
    }
    @Test
    fun test1_uploadInvalidImageAndClickEnterManuallyButton_NavigatesToMainScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
        idlingResource.waitForIdle()
        reviewScreen.clickProcessButton()
        val isNoResultTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_title_no_results)
        assertEquals(true, isNoResultTitleVisible)

        val isNoResultHeaderVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_noresults_header)
        assertEquals(true, isNoResultHeaderVisible)

        val isUsefulTipsVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_useful_tips)
        assertEquals(true, isUsefulTipsVisible)

        val isGoodLightingTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_photo_tip_good_lighting_title)
        assertEquals(true, isGoodLightingTitleVisible)

        val isFlattenTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_photo_tip_flatten_the_page_title)
        assertEquals(true, isFlattenTitleVisible)

        val isParallelTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_photo_tip_parallel_title)
        assertEquals(true, isParallelTitleVisible)

        val isPositionInTheFrameTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_photo_tip_align_title)
        assertEquals(true, isPositionInTheFrameTitleVisible)

        val isMultiPagesTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_photo_tip_multiple_pages_title)
        assertEquals(true, isMultiPagesTitleVisible)

        idlingResource.waitForIdle()
        val isEnterManuallyButtonVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_noresults_enter_manually)
        assertEquals(true, isEnterManuallyButtonVisible)

        noResultScreen.clickEnterManuallyButton()

        val isDescriptionTitleVisible = mainScreen.assertDescriptionTitle()
        assertEquals(true, isDescriptionTitleVisible)
    }


    @Test
    fun test2_uploadInvalidPdfAndClickEnterManuallyButton_NavigatesToMainScreen() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButton()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles("test-pdf-no-results.pdf")
        idlingResource.waitForIdle()
        val isNoResultTitleVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_title_no_results)
        assertEquals(true, isNoResultTitleVisible)

        val isNoResultHeaderVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_noresults_header)
        assertEquals(true, isNoResultHeaderVisible)

        val isSupportedFormatVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_supported_format_section_header)
        assertEquals(true, isSupportedFormatVisible)

        val isComputerGeneratedInvoiceTextVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_supported_format_printed_invoices)
        assertEquals(true, isComputerGeneratedInvoiceTextVisible)

        val isOneSidedPhotoTextVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_supported_format_single_page_as_jpeg_png_gif)
        assertEquals(true, isOneSidedPhotoTextVisible)

        val isPdfDocumentTextVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_supported_format_pdf)
        assertEquals(true, isPdfDocumentTextVisible)

        val isQRCodeTextVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_supported_format_qr_code)
        assertEquals(true, isQRCodeTextVisible)

        val isMonitorScreenPhotosTextVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_unsupported_format_photos_of_screens)
        assertEquals(true, isMonitorScreenPhotosTextVisible)

        val isNotSupportedFormatVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_unsupported_format_section_header)
        assertEquals(true, isNotSupportedFormatVisible)

        val isHandwritingTextVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_unsupported_format_handwriting)
        assertEquals(true, isHandwritingTextVisible)

        idlingResource.waitForIdle()
        val isEnterManuallyButtonVisible = noResultScreen.checkElementWithTextIsDisplayed(net.gini.android.capture.R.string.gc_noresults_enter_manually)
        assertEquals(true, isEnterManuallyButtonVisible)

        noResultScreen.clickEnterManuallyButton()

        val isDescriptionTitleVisible = mainScreen.assertDescriptionTitle()
        assertEquals(true, isDescriptionTitleVisible)
    }
}