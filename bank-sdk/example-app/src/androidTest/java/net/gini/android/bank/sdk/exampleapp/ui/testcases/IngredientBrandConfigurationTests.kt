package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.pressBack
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Gini ingredient brand switch itself (epic PP-2568, UI tests PP-3478): how the backend
 * `ingredientBrandScreens` value is applied across SDK starts and when `/configurations` fails,
 * and that the brand stays off every screen other than the analysis step.
 *
 * These cover acceptance criteria the manual Test Set PP-3740 had no case for (see the PP-3476
 * review). Every test arms [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend];
 * the app data is wiped before each test (`clearPackageData`), so two SDK starts inside one test
 * share the saved configuration, and two tests never do.
 */
class IngredientBrandConfigurationTests : WarningBottomSheetTestBase() {

    private val analysisScreen = AnalysisScreen()

    /** R27: a known value next to an unknown one still switches the brand on. */
    @Test
    fun test1_mixedList_showsBrand() {
        arm(setOf("Analysis", "Foo"))

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Powered by Gini badge is not shown", analysisScreen.waitForPoweredByGini())
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
    }

    /** R28: switching the brand on takes effect at the next SDK start — no SDK release needed. */
    @Test
    fun test2_flagTurnedOnBetweenSdkStarts_brandAppearsOnNextStart() {
        arm(BRAND_OFF)
        runPdfAnalysis()
        assertFalse("No badge expected on the first start", analysisScreen.isPoweredByGiniDisplayed())
        finishAndReturnToMainScreen()

        arm(BRAND_ON)
        runPdfAnalysis()
        assertTrue(
            "Powered by Gini badge must appear once the backend lists the screen",
            analysisScreen.waitForPoweredByGini()
        )
    }

    /** R28, reverse: switching the brand off also takes effect at the next SDK start. */
    @Test
    fun test3_flagTurnedOffBetweenSdkStarts_brandGoneOnNextStart() {
        arm(BRAND_ON)
        runPdfAnalysis()
        assertTrue("Powered by Gini badge is not shown on the first start", analysisScreen.waitForPoweredByGini())
        finishAndReturnToMainScreen()

        arm(BRAND_OFF)
        runPdfAnalysis()
        assertTrue("Default loading indicator is not shown", analysisScreen.waitForDefaultLoadingIndicator())
        assertFalse(
            "Powered by Gini badge must be gone once the backend stops listing the screen",
            analysisScreen.isPoweredByGiniDisplayed()
        )
    }

    /** R29: a failed `/configurations` request keeps the value saved by the last good one. */
    @Test
    fun test4_configurationFails_savedValueIsKept() {
        arm(BRAND_ON)
        runPdfAnalysis()
        assertTrue("Powered by Gini badge is not shown on the first start", analysisScreen.waitForPoweredByGini())
        finishAndReturnToMainScreen()

        arm(BRAND_OFF, configurationFails = true)
        runPdfAnalysis()
        assertTrue(
            "The saved value must keep the badge when /configurations fails",
            analysisScreen.waitForPoweredByGini()
        )
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
    }

    /** R30: with nothing saved yet, a failed `/configurations` request means no brand. */
    @Test
    fun test5_configurationFailsOnFreshInstall_noBrand() {
        arm(BRAND_ON, configurationFails = true)

        runPdfAnalysis()

        assertTrue("Default loading indicator is not shown", analysisScreen.waitForDefaultLoadingIndicator())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse("No Gini loading indicator expected", analysisScreen.isGiniLoadingIndicatorDisplayed())
    }

    /** R31: the live camera and the Review screen never carry the brand. */
    @Test
    fun test6_brandOn_noBadgeOnCameraOrReviewScreen() {
        arm(BRAND_ON)

        imageUploader.copyImageToDownloads(getApplicationContext(), ANY_IMAGE)
        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        assertTrue("Camera screen did not appear", captureScreen.checkScanTextDisplayed())
        assertFalse("No badge expected over the live camera", analysisScreen.isPoweredByGiniDisplayed())

        captureScreen.clickFilesButton()
        captureScreen.clickPhotos()
        imageUploader.uploadImageFromPhotos()
        imageUploader.clickAddButton()
        idlingResource.waitForIdle()
        reviewScreen.assertReviewTitleIsDisplayed()
        assertFalse("No badge expected on the Review screen", analysisScreen.isPoweredByGiniDisplayed())
    }

    /** R31: the No Results screen does not carry the brand. */
    @Test
    fun test7_brandOn_noBadgeOnNoResultsScreen() {
        arm(BRAND_ON, scenario = UiTestMockScenario.NO_RESULTS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue(
            "No Results screen did not appear",
            analysisScreen.waitForText(net.gini.android.capture.R.string.gc_noresults_enter_manually)
        )
        assertFalse("No badge expected on the No Results screen", analysisScreen.isPoweredByGiniDisplayed())
    }

    /** R31: the Error screen does not carry the brand. */
    @Test
    fun test8_brandOn_noBadgeOnErrorScreen() {
        arm(BRAND_ON, scenario = UiTestMockScenario.ANALYSIS_ERROR)

        uploadFixturePdfAndProcess(ANY_PDF)

        // The header is always there; the "back to camera" button is hidden for some documents.
        assertTrue("Error screen did not appear", analysisScreen.waitForView("gc_error_header"))
        assertFalse("No badge expected on the Error screen", analysisScreen.isPoweredByGiniDisplayed())
    }

    private fun arm(
        ingredientBrandScreens: Set<String>,
        configurationFails: Boolean = false,
        scenario: UiTestMockScenario = UiTestMockScenario.INVOICE
    ) {
        armMockBackend(
            scenario,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = ingredientBrandScreens,
            analysisDelayMillis = ANALYSIS_DELAY_MS,
            configurationFails = configurationFails
        )
    }

    private fun runPdfAnalysis() {
        uploadFixturePdfAndProcess(ANY_PDF)
        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
    }

    /** Lets the analysis end on the extraction screen, then goes back to the main screen. */
    private fun finishAndReturnToMainScreen() {
        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        pressBack()
    }

    private companion object {
        val BRAND_ON = setOf("Analysis")
        val BRAND_OFF = emptySet<String>()

        const val ANY_PDF = "sample.pdf"
        const val ANY_IMAGE = CreditNoteFixtures.PLAIN_INVOICE_ASSET

        /** Keeps the Analysis screen up long enough to be checked. */
        const val ANALYSIS_DELAY_MS = 5_000L
    }
}
