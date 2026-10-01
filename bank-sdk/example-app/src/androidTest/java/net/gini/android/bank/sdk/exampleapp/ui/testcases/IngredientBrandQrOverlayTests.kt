package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * The Gini ingredient brand on the camera screen's QR code overlay (epic PP-2568, UI tests
 * PP-3478). A QR code scan does not open the Analysis screen: the "QR code detected" popup, the
 * QR education and the invoice retrieval all run as an overlay on the camera screen, and the same
 * `ingredientBrandScreens` entry "Analysis" switches the badge and the Gini mark there.
 *
 * ## These tests need a real QR code in front of the camera
 *
 * Camera image injection is not available for Espresso (BrowserStack rejects it), so the camera
 * must really see a SEPA payment QR code. Run them on a local device and hold the back camera in
 * front of `src/androidTest/assets/sepa_payment_qr_poster.png` (for example opened full screen on
 * a laptop). When no QR code is detected within [QR_DETECTION_TIMEOUT_MS], each test is SKIPPED —
 * never passed. On BrowserStack the camera only sees the device rack, so the tests always skip.
 *
 * The mock ignores what the QR code says; any readable SEPA payment QR code works.
 */
class IngredientBrandQrOverlayTests : WarningBottomSheetTestBase() {

    private val analysisScreen = AnalysisScreen()

    /** Lets a running QR education finish before the activity rule closes the app. */
    @After
    fun waitForQrEducationToFinish() {
        analysisScreen.waitForInvoiceEducationGone(QR_EDUCATION_DURATION_MS)
    }

    /** R16: while the invoice is retrieved, the overlay shows the badge and the Gini mark. */
    @Test
    fun test1_qr_brandOn_overlayShowsBadgeAndGiniMark() {
        armQr(BRAND_ON, qrCodeEducationEnabled = false)

        openCameraAndScanQrCode()

        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
    }

    /** R17: without the brand, the overlay shows neither the badge nor the Gini mark. */
    @Test
    fun test2_qr_brandOff_overlayShowsNeither() {
        armQr(BRAND_OFF, qrCodeEducationEnabled = false)

        openCameraAndScanQrCode()

        assertTrue("Default loading indicator is not shown", analysisScreen.waitForDefaultLoadingIndicator())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse("No Gini loading indicator expected", analysisScreen.isGiniLoadingIndicatorDisplayed())
    }

    /** R17a: during the QR education the badge is shown, but the Gini mark is not. */
    @Test
    fun test3_qr_brandOn_educationShowsBadgeWithoutGiniMark() {
        armQr(BRAND_ON, qrCodeEducationEnabled = true)

        openCameraAndScanQrCode()

        assertTrue("QR code education did not appear", analysisScreen.waitForQrCodeEducation())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse(
            "No Gini loading indicator expected during the QR education",
            analysisScreen.isGiniLoadingIndicatorDisplayed()
        )
    }

    /**
     * Without the brand, the QR education has no indicator behind it either — the Gini mark
     * follows the old indicator's rule, and that rule is: none during the education (as on iOS).
     */
    @Test
    fun test4_qr_brandOff_educationShowsNoIndicator() {
        armQr(BRAND_OFF, qrCodeEducationEnabled = true)

        openCameraAndScanQrCode()

        assertTrue("QR code education did not appear", analysisScreen.waitForQrCodeEducation())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse(
            "No default loading indicator expected during the QR education",
            analysisScreen.isDefaultLoadingIndicatorDisplayed()
        )
    }

    private fun armQr(ingredientBrandScreens: Set<String>, qrCodeEducationEnabled: Boolean) {
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = ingredientBrandScreens,
            qrCodeEducationEnabled = qrCodeEducationEnabled,
            analysisDelayMillis = ANALYSIS_DELAY_MS
        )
    }

    /** R18: skipped, not failed, when the camera cannot see a QR code. */
    private fun openCameraAndScanQrCode() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        Assume.assumeTrue(
            "No SEPA payment QR code in front of the camera — hold the device in front of " +
                "sepa_payment_qr_poster.png. Always skipped on BrowserStack.",
            analysisScreen.waitForQrCodeDetected(QR_DETECTION_TIMEOUT_MS)
        )
    }

    private companion object {
        val BRAND_ON = setOf("Analysis")
        val BRAND_OFF = emptySet<String>()

        const val QR_DETECTION_TIMEOUT_MS = 20_000L

        /** Keeps the invoice retrieval state up long enough to be checked. */
        const val ANALYSIS_DELAY_MS = 8_000L

        /** The QR education uses the same intro + message animation, 4.5 s in total. */
        const val QR_EDUCATION_DURATION_MS = 7_500L
    }
}
