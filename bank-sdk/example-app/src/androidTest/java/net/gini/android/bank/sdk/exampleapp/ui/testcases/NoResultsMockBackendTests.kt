package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.NoResultScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import net.gini.android.capture.R
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * The No Results screen reached from the camera (Xray folder "No Result screen", PP-3920),
 * driven by [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * ## Why the mock backend and not a real document
 *
 * The **Retake images** button exists only for camera-sourced documents
 * (`NoResultsFragmentImpl.shouldAllowRetakeImages`), so every "retake" and "back to camera" case
 * needs a camera photo — the import variants live in [NoResultsTests]. On BrowserStack the camera
 * photographs the device rack, and a rack photo against the real Gini API is not guaranteed to
 * end in "no results" (a blank image produces an upload error instead). The `NO_RESULTS`
 * scenario ignores the uploaded bytes and answers with empty extractions, which is exactly what
 * PP-3195 ("Testing via API") asks for. All text is asserted by capture-sdk string id, so the
 * tests pass in every device locale.
 *
 * ## The QR tests need a real QR code in front of the camera
 *
 * Camera image injection is not available for Espresso (BrowserStack rejects it), so for
 * [test6_qr_noResults_showsQrHeaderFormatsRetakeAndEnterManually],
 * [test7_qr_enterManually_returnsToMainScreen] and [test8_qr_retakeImages_returnsToCamera] the
 * camera must really see a SEPA payment QR code. Run them on a local device and hold the back
 * camera in front of `src/androidTest/assets/sepa_payment_qr_poster.png` (for example opened
 * full screen on a laptop). When no QR code is detected within [QR_DETECTION_TIMEOUT_MS], each
 * of them is SKIPPED — never passed, never failed — exactly like [IngredientBrandQrOverlayTests].
 * On BrowserStack the camera only sees the device rack, so those three always skip.
 *
 * The mock ignores what the QR code says; any readable SEPA payment QR code works.
 */
class NoResultsMockBackendTests : WarningBottomSheetTestBase() {

    private val noResultScreen = NoResultScreen()
    private val analysisScreen = AnalysisScreen()

    /** PP-3189 / PP-3195: a camera photo with empty extractions shows the full No Results screen. */
    @Test
    fun test1_cameraPhoto_noResults_showsTipsRetakeAndEnterManually() {
        armMockBackend(UiTestMockScenario.NO_RESULTS)
        takePhotoAndProcess()
        waitForNoResultScreen()

        assertDisplayed(R.string.gc_title_no_results)
        assertDisplayed(R.string.gc_noresults_header)
        assertDisplayed(R.string.gc_useful_tips)
        assertDisplayed(R.string.gc_photo_tip_good_lighting_title)
        assertDisplayed(R.string.gc_photo_tip_flatten_the_page_title)
        assertDisplayed(R.string.gc_photo_tip_parallel_title)
        assertDisplayed(R.string.gc_photo_tip_align_title)
        assertDisplayed(R.string.gc_photo_tip_multiple_pages_title)
        assertDisplayed(R.string.gc_noresults_retake_images)
        assertDisplayed(R.string.gc_noresults_enter_manually)
    }

    /** PP-3191: Retake images on a camera No Results screen returns to the camera. */
    @Test
    fun test2_cameraPhoto_retakeImages_returnsToCamera() {
        armMockBackend(UiTestMockScenario.NO_RESULTS)
        takePhotoAndProcess()
        waitForNoResultScreen()

        noResultScreen.clickRetakeImagesButton()

        assertTrue("Camera screen is not shown after Retake images", captureScreen.checkScanTextDisplayed())
    }

    /** PP-3190 (camera variant): Enter manually on a camera No Results screen returns to the main screen. */
    @Test
    fun test3_cameraPhoto_enterManually_returnsToMainScreen() {
        armMockBackend(UiTestMockScenario.NO_RESULTS)
        takePhotoAndProcess()
        waitForNoResultScreen()

        noResultScreen.clickEnterManuallyButton()

        assertTrue("Main screen is not shown after Enter manually", mainScreen.assertDescriptionTitle())
    }

    /** PP-3200: the top bar's back button returns to the camera, not to the Review screen. */
    @Test
    fun test4_cameraPhoto_navigationBack_returnsToCamera() {
        armMockBackend(UiTestMockScenario.NO_RESULTS)
        takePhotoAndProcess()
        waitForNoResultScreen()

        noResultScreen.clickNavigationBackButton()

        assertTrue("Camera screen is not shown after the navigation back button", captureScreen.checkScanTextDisplayed())
    }

    /** PP-3200: the system back button is routed to the camera as well (`handleOnBackPressed`). */
    @Test
    fun test5_cameraPhoto_systemBack_returnsToCamera() {
        armMockBackend(UiTestMockScenario.NO_RESULTS)
        takePhotoAndProcess()
        waitForNoResultScreen()

        noResultScreen.pressSystemBack()

        assertTrue("Camera screen is not shown after the system back button", captureScreen.checkScanTextDisplayed())
    }

    /** PP-3194: a payment QR code with empty extractions shows the QR variant of the No Results screen. */
    @Test
    fun test6_qr_noResults_showsQrHeaderFormatsRetakeAndEnterManually() {
        armMockBackend(UiTestMockScenario.NO_RESULTS, qrCodeEducationEnabled = false)
        openCameraAndScanQrCode()
        waitForNoResultScreen()

        assertDisplayed(R.string.gc_noresults_header_qr)
        assertDisplayed(R.string.gc_supported_format_section_header)
        assertDisplayed(R.string.gc_supported_format_qr_type_gini_payment)
        assertDisplayed(R.string.gc_noresults_retake_images)
        assertDisplayed(R.string.gc_noresults_enter_manually)
    }

    /** PP-3196: Enter manually on the QR No Results screen returns to the main screen. */
    @Test
    fun test7_qr_enterManually_returnsToMainScreen() {
        armMockBackend(UiTestMockScenario.NO_RESULTS, qrCodeEducationEnabled = false)
        openCameraAndScanQrCode()
        waitForNoResultScreen()

        noResultScreen.clickEnterManuallyButton()

        assertTrue("Main screen is not shown after Enter manually", mainScreen.assertDescriptionTitle())
    }

    /** PP-3197: Retake images on the QR No Results screen returns to the camera. */
    @Test
    fun test8_qr_retakeImages_returnsToCamera() {
        armMockBackend(UiTestMockScenario.NO_RESULTS, qrCodeEducationEnabled = false)
        openCameraAndScanQrCode()
        waitForNoResultScreen()

        noResultScreen.clickRetakeImagesButton()

        assertTrue("Camera screen is not shown after Retake images", captureScreen.checkScanTextDisplayed())
    }

    /**
     * The analysis is asynchronous: wait for the screen title (30 s, UiAutomator) instead of
     * relying on the fixed IdlingResource sleep, then confirm it really is there. The QR variant
     * swaps the header, the title is the same.
     */
    private fun waitForNoResultScreen() {
        noResultScreen.waitForNoResultScreen(R.string.gc_title_no_results)
        assertDisplayed(R.string.gc_title_no_results)
    }

    /** Skipped, not failed, when the camera cannot see a QR code — see the class KDoc. */
    private fun openCameraAndScanQrCode() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        Assume.assumeTrue(
            "No SEPA payment QR code in front of the camera — hold the device in front of " +
                "sepa_payment_qr_poster.png. Always skipped on BrowserStack.",
            analysisScreen.waitForQrCodeDetected(QR_DETECTION_TIMEOUT_MS)
        )
    }

    private fun assertDisplayed(resId: Int) {
        val name = resources.getResourceEntryName(resId)
        assertTrue("'$name' is not displayed", noResultScreen.checkElementWithTextIsDisplayed(resId))
    }

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private companion object {
        const val QR_DETECTION_TIMEOUT_MS = 20_000L
    }
}
