package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import net.gini.android.bank.sdk.exampleapp.ui.resources.LandscapeDarkModeRule
import net.gini.android.bank.sdk.exampleapp.ui.resources.ScreenLoadingIndicatorConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The Gini ingredient brand with the device turned to landscape on the SDK's camera screen, in dark mode (epic PP-2568, UI
 * tests PP-3478). The portrait tests run in light mode; these cover the other combination for
 * the most important flows, matching the Figma landscape frames (4.1.x, 2.2.x, 1.2.x).
 *
 * The SDK is opened in portrait and the device is turned to landscape on the camera screen
 * ([LandscapeDarkModeRule]); the Analysis screen then opens in landscape and is never rotated.
 *
 * - Photo without education, with a tip (the analysis runs past the first tip at 5 s).
 * - Photo without education, without a tip (the analysis ends before the first tip).
 * - Photo with the brand off and a tip: the tip must not cover the default loading indicator.
 * - The bank's own loading indicator with a tip, a credit note warning that opens after the tips
 *   have started, and a PDF import (a longer analysis message, no tips).
 * - QR code overlay without and with the QR education — these need a real QR code in front of
 *   the camera, and are skipped otherwise (always on BrowserStack). Keep the camera pointed away
 *   until the screen turns to landscape, then point it at `sepa_payment_qr_poster.png`: a scan
 *   before the turn skips the test.
 *
 * Photo *with* the invoice education needs animations ON, so it lives in
 * [IngredientBrandLandscapeEducationTests].
 */
class IngredientBrandLandscapeTests : WarningBottomSheetTestBase() {

    /** Wraps the activity rule, so the app is launched already in dark mode. */
    @get:Rule(order = Int.MIN_VALUE + 1)
    val landscapeDarkMode = LandscapeDarkModeRule()

    private val analysisScreen = AnalysisScreen()

    @Before
    fun checkDarkMode() {
        // Skipped, not failed, where the device does not let a test switch dark mode (like the
        // network toggle in ErrorScreenTests on BrowserStack).
        Assume.assumeTrue(
            "Dark mode could not be switched on for this device",
            landscapeDarkMode.isDarkModeOn()
        )
    }

    /** Opens the SDK in portrait, then turns the device to landscape on the camera screen. */
    private fun openSdkAndRotateToLandscape() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        assertTrue("Camera screen did not appear", captureScreen.checkScanTextDisplayed())
        landscapeDarkMode.rotateToLandscape()
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
    }

    /** Lets a running QR education finish before the activity rule closes the app. */
    @After
    fun waitForEducationToFinish() {
        analysisScreen.waitForInvoiceEducationGone(EDUCATION_DURATION_MS)
    }

    /**
     * Photo, no education, with a tip: badge in place and the Gini mark, then the tip replaces the
     * badge without covering the Gini mark or the analysis message (Figma 35002:11528).
     */
    @Test
    fun test1_landscapeDark_photo_noEducation_tipReplacesBadge() {
        arm(qrCodeEducationEnabled = false, analysisDelayMillis = WITH_TIP_DELAY_MS)

        openSdkAndRotateToLandscape()
        takePhotoOnCameraScreenAndProcess()

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue(
            "Powered by Gini badge must be shown before the first tip",
            analysisScreen.waitForPoweredByGini(BEFORE_FIRST_TIP_TIMEOUT_MS)
        )
        analysisScreen.badgePlacementProblem()?.let { problem -> fail(problem) }
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())

        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertTrue(
            "Powered by Gini badge must hide when the first tip appears",
            analysisScreen.waitForPoweredByGiniGone()
        )
        assertTrue("Tip did not finish sliding in", analysisScreen.waitForTipSettled())
        analysisScreen.tipOverlapProblem()?.let { problem -> fail(problem) }
    }

    /** Photo, no education, no tip: the analysis ends before the first tip; the badge stays until then. */
    @Test
    fun test2_landscapeDark_photo_noEducation_noTip() {
        arm(qrCodeEducationEnabled = false, analysisDelayMillis = NO_TIP_DELAY_MS)

        openSdkAndRotateToLandscape()
        takePhotoOnCameraScreenAndProcess()

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.waitForPoweredByGini(BEFORE_FIRST_TIP_TIMEOUT_MS))
        analysisScreen.badgePlacementProblem()?.let { problem -> fail(problem) }
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        assertFalse("No capture suggestion expected", analysisScreen.isTipDisplayed())
        assertTrue(
            "Analysis must end on the extraction screen",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
    }

    /** Photo, brand off, with a tip: the tip must leave the default loading indicator uncovered. */
    @Test
    fun test5_landscapeDark_photo_brandOff_tipLeavesIndicatorUncovered() {
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = emptySet(),
            qrCodeEducationEnabled = false,
            analysisDelayMillis = WITH_TIP_DELAY_MS
        )

        openSdkAndRotateToLandscape()
        takePhotoOnCameraScreenAndProcess()

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue("Default loading indicator is not shown", analysisScreen.waitForDefaultLoadingIndicator())
        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertTrue("Tip did not finish sliding in", analysisScreen.waitForTipSettled())
        analysisScreen.tipOverlapProblem()?.let { problem -> fail(problem) }
    }

    /** Photo, brand off, the bank's own indicator, with a tip: the tip must leave it uncovered. */
    @Test
    fun test6_landscapeDark_photo_customIndicator_tipLeavesIndicatorUncovered() {
        ScreenLoadingIndicatorConfigurator.applyScreenCustomLoadingIndicator(
            activityRule.scenario,
            enabled = true
        )
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = emptySet(),
            qrCodeEducationEnabled = false,
            analysisDelayMillis = WITH_TIP_DELAY_MS
        )

        openSdkAndRotateToLandscape()
        takePhotoOnCameraScreenAndProcess()

        assertTrue("Custom loading indicator is not shown", analysisScreen.waitForCustomLoadingIndicator())
        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertTrue("Tip did not finish sliding in", analysisScreen.waitForTipSettled())
        analysisScreen.tipOverlapProblem()?.let { problem -> fail(problem) }
    }

    /**
     * Photo, brand on, a credit note: the tips start, then the credit note warning opens in
     * landscape once the analysis ends. The warning hides the tips behind it.
     */
    @Test
    fun test7_landscapeDark_photo_warningOpensAfterTips() {
        armMockBackend(
            UiTestMockScenario.CREDIT_NOTE,
            creditNoteHintEnabled = true,
            ingredientBrandScreens = setOf("Analysis"),
            qrCodeEducationEnabled = false,
            analysisDelayMillis = WITH_TIP_DELAY_MS
        )
        configureCreditNoteHint(enabled = true)

        openSdkAndRotateToLandscape()
        takePhotoOnCameraScreenAndProcess()

        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertTrue("Tip did not finish sliding in", analysisScreen.waitForTipSettled())
        analysisScreen.tipOverlapProblem()?.let { problem -> fail(problem) }
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteState()
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
    }

    /** PDF import, brand on: the badge in place, clear of the longer PDF analysis message. */
    @Test
    fun test8_landscapeDark_pdf_showsBadgeAndGiniMark() {
        arm(qrCodeEducationEnabled = false, analysisDelayMillis = NO_TIP_DELAY_MS)
        runCatching { pdfUploader.copyPdfToDownloads(getApplicationContext(), ANY_PDF) }

        openSdkAndRotateToLandscape()
        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles(ANY_PDF)
        idlingResource.waitForIdle()

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.waitForPoweredByGini())
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        analysisScreen.badgePlacementProblem()?.let { problem -> fail(problem) }
    }

    /** QR code overlay, no education: the badge and the Gini mark while the invoice is retrieved. */
    @Test
    fun test3_landscapeDark_qr_noEducation_showsBadgeAndGiniMark() {
        arm(qrCodeEducationEnabled = false, analysisDelayMillis = QR_DELAY_MS)

        openCameraAndScanQrCode()

        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
    }

    /** QR code overlay with the QR education: the badge, and no indicator behind the message. */
    @Test
    fun test4_landscapeDark_qr_education_showsBadgeWithoutIndicator() {
        arm(qrCodeEducationEnabled = true, analysisDelayMillis = QR_DELAY_MS)

        openCameraAndScanQrCode()

        assertTrue("QR code education did not appear", analysisScreen.waitForQrCodeEducation())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse(
            "No Gini loading indicator expected during the QR education",
            analysisScreen.isGiniLoadingIndicatorDisplayed()
        )
    }

    private fun arm(qrCodeEducationEnabled: Boolean, analysisDelayMillis: Long) {
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = setOf("Analysis"),
            qrCodeEducationEnabled = qrCodeEducationEnabled,
            analysisDelayMillis = analysisDelayMillis
        )
    }

    /** Skipped, not failed, when the camera cannot see a QR code — always the case on BrowserStack. */
    private fun openCameraAndScanQrCode() {
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        assertTrue("Camera screen did not appear", captureScreen.checkScanTextDisplayed())
        // A scan in portrait would start the QR step before the turn, and the turn rebuilds the
        // camera screen under it — that is a different case (rotation during the QR step).
        Assume.assumeFalse(
            "The QR code was scanned before the device turned to landscape — keep the camera " +
                "pointed away until the screen turns, then point it at the QR code.",
            analysisScreen.waitForQrCodeDetected(0)
        )
        landscapeDarkMode.rotateToLandscape()
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
        Assume.assumeTrue(
            "No SEPA payment QR code in front of the camera — hold the device, in landscape, in " +
                "front of sepa_payment_qr_poster.png. Always skipped on BrowserStack.",
            analysisScreen.waitForQrCodeDetected(QR_DETECTION_TIMEOUT_MS)
        )
    }

    private companion object {
        /** Past the first tip (5 s after the Analysis screen starts). */
        const val WITH_TIP_DELAY_MS = 12_000L

        /** Ends before the first tip can slide in. */
        const val NO_TIP_DELAY_MS = 4_000L

        const val QR_DELAY_MS = 8_000L

        /** The mock never reads it; see IngredientBrandTests. */
        const val ANY_PDF = "sample.pdf"
        const val QR_DETECTION_TIMEOUT_MS = 20_000L

        /** The first tip starts sliding in 5 s after the Analysis screen starts. */
        const val BEFORE_FIRST_TIP_TIMEOUT_MS = 3_000L

        /** Intro + message, 4.5 s, plus a margin. */
        const val EDUCATION_DURATION_MS = 7_500L
    }
}
