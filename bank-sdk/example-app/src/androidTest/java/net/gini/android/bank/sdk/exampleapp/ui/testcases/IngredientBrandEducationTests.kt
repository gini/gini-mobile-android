package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.espresso.Espresso.pressBack
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Gini ingredient brand while the invoice education is shown on the Analysis screen (epic
 * PP-2568, UI tests PP-3478).
 *
 * The Gini loading indicator only *replaces* the old loading indicator and follows its rules:
 * while the education message is shown there is no loading indicator at all, and after the
 * message ends the screen looks the same as with the brand off. Only the badge and the kind of
 * indicator differ between brand on and brand off.
 *
 * ## How the education is reached
 *
 * - The backend flag `isQrCodeEducationEnabled` switches it; the mock serves it.
 * - It is shown on the first two analyses only. The orchestrator's `clearPackageData` resets the
 *   counter before every test, so each test starts at analysis 1.
 * - It needs the photo flow type, which only a picture taken with the camera sets — an upload does
 *   not trigger it. On BrowserStack the camera sees the device rack; that is fine, because the mock
 *   ignores the bytes.
 *
 * The education is a Compose animation (1.5 s intro + 3 s message), so these tests need system
 * animations ON — they run in their own BrowserStack group, `bs_run_group_ingredientbrand_education.sh`.
 */
class IngredientBrandEducationTests : WarningBottomSheetTestBase() {

    private val analysisScreen = AnalysisScreen()

    /**
     * Lets a running education finish before the activity rule closes the app.
     *
     * Closing puts an empty test activity on top and resumes the Analysis screen again. If that
     * happens while the education is still showing, the SDK's second start waits forever on the
     * education lock (`AnalysisScreenPresenterExtension.showLoadingIndicator`; the release in
     * `AnalysisScreenPresenter.releaseMutexForEducation` is never called) and the main thread
     * freezes. A failing assertion early in a test would then hang the run instead of reporting.
     * This runs before the rule's teardown, so the failure is reported normally.
     */
    @After
    fun waitForEducationToFinish() {
        analysisScreen.waitForInvoiceEducationGone(EDUCATION_DURATION_MS + EDUCATION_MARGIN_MS)
    }

    /** R6a, R22: during the education the badge is shown, but no indicator and no tip. */
    @Test
    fun test1_photo_brandOn_educationShowsBadgeWithoutIndicator() {
        armEducation(BRAND_ON)

        takePhotoAndProcess()

        assertTrue("Invoice education did not appear", analysisScreen.waitForInvoiceEducation())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
        assertNoIndicatorAndNoTip()
    }

    /** R23: without the brand the education screen has no badge and no indicator either. */
    @Test
    fun test2_photo_brandOff_educationShowsNoBadgeNoIndicator() {
        armEducation(BRAND_OFF)

        takePhotoAndProcess()

        assertTrue("Invoice education did not appear", analysisScreen.waitForInvoiceEducation())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
        assertNoIndicatorAndNoTip()
    }

    /**
     * R24: once the education has ended and the analysis is still running, the Gini mark does not
     * come back — exactly like the old indicator, which does not come back either. The badge stays.
     */
    @Test
    fun test3_photo_brandOn_afterEducationMatchesBrandOff() {
        armEducation(BRAND_ON)

        takePhotoAndProcess()

        assertTrue("Invoice education did not appear", analysisScreen.waitForInvoiceEducation())
        assertTrue("Invoice education did not end", analysisScreen.waitForInvoiceEducationGone())
        assertFalse(
            "The analysis must still be pending for this check",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
        assertTrue("Powered by Gini badge is not shown", analysisScreen.isPoweredByGiniDisplayed())
        assertNoIndicatorAndNoTip()
    }

    /**
     * R25: the education is shown on analyses 1 and 2 only. Analysis 3 shows the Gini mark, then
     * a tip, and the tip takes the badge's place.
     */
    @Test
    fun test4_photo_brandOn_educationLimitThenTipsHideBadge() {
        armEducation(BRAND_ON)

        repeat(EDUCATION_LIMIT) { index ->
            takePhotoAndProcess()
            assertTrue(
                "Invoice education did not appear on analysis ${index + 1}",
                analysisScreen.waitForInvoiceEducation()
            )
            assertTrue(
                "Powered by Gini badge is not shown on analysis ${index + 1}",
                analysisScreen.isPoweredByGiniDisplayed()
            )
            assertTrue(
                "Extraction screen did not appear after analysis ${index + 1}",
                extractionScreen.assertExtractionScreenIsDisplayed()
            )
            pressBack()
        }

        takePhotoAndProcess()
        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue(
            "Gini loading indicator is expected once the education limit is reached",
            analysisScreen.waitForGiniLoadingIndicator()
        )
        assertFalse(
            "Invoice education must not appear after $EDUCATION_LIMIT analyses",
            analysisScreen.isInvoiceEducationDisplayed()
        )
        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertTrue(
            "Powered by Gini badge must hide when the first tip appears",
            analysisScreen.waitForPoweredByGiniGone()
        )
    }

    private fun armEducation(ingredientBrandScreens: Set<String>) {
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = ingredientBrandScreens,
            qrCodeEducationEnabled = true,
            analysisDelayMillis = ANALYSIS_DELAY_MS
        )
    }

    private fun assertNoIndicatorAndNoTip() {
        assertFalse("No Gini loading indicator expected", analysisScreen.isGiniLoadingIndicatorDisplayed())
        assertFalse("No default loading indicator expected", analysisScreen.isDefaultLoadingIndicatorDisplayed())
        assertFalse("No capture suggestion expected", analysisScreen.isTipDisplayed())
    }

    private companion object {
        val BRAND_ON = setOf("Analysis")
        val BRAND_OFF = emptySet<String>()

        /** Longer than the 4.5 s education, so the analysis is still pending when it ends. */
        const val ANALYSIS_DELAY_MS = 12_000L

        /** `AnimatedEducationMessageWithIntro`: 1.5 s intro + 3 s message. */
        const val EDUCATION_DURATION_MS = 4_500L
        const val EDUCATION_MARGIN_MS = 3_000L

        /** `GetInvoiceEducationTypeUseCase` shows the education while its counter is 0 or 1. */
        const val EDUCATION_LIMIT = 2
    }
}
