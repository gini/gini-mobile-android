package net.gini.android.bank.sdk.exampleapp.ui.testcases

import android.os.SystemClock
import androidx.test.espresso.Espresso.pressBack
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.ScreenLoadingIndicatorConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.screens.AnalysisScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The Gini ingredient brand on the Analysis screen (epic PP-2568, UI tests PP-3478): the
 * "Powered by Gini" badge and the Gini loading indicator, switched by the backend
 * `/configurations` field `ingredientBrandScreens`.
 *
 * A device cannot change a server flag, so every test here arms
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend] with the value it needs,
 * the same way [CreditNoteMockBackendTests] does. The mock also holds the analysis back for a few
 * seconds, so the Analysis screen stays up long enough to be checked. It ignores the uploaded
 * bytes, so the document used never affects a result.
 *
 * - PDF imports never show capture suggestions (tips), so they check the badge and the indicator
 *   for the whole analysis.
 * - Image uploads show tips, which take the badge's place at the bottom of the screen.
 *
 * The rows match the manual Test Set PP-3740. Visual checks (dark mode, 200% font, the animation
 * itself, TalkBack reading order) stay manual — see specs/PP-3478-feature.md.
 */
class IngredientBrandTests : WarningBottomSheetTestBase() {

    private val analysisScreen = AnalysisScreen()

    /** R1, R3, R7: the badge in its designed place and the Gini mark instead of the spinner. */
    @Test
    fun test1_pdf_brandOn_showsBadgeInPositionAndGiniMark() {
        armBrand(BRAND_ON, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue("Powered by Gini badge is not shown", analysisScreen.waitForPoweredByGini())
        analysisScreen.badgePlacementProblem()?.let { problem -> fail(problem) }
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        assertFalse(
            "Default loading indicator must not be shown while the brand is on",
            analysisScreen.isDefaultLoadingIndicatorDisplayed()
        )
    }

    /** R8: TalkBack announces the Gini mark with the SDK's localized description. */
    @Test
    fun test2_pdf_brandOn_giniMarkHasTalkBackDescription() {
        armBrand(BRAND_ON, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        val mark = analysisScreen.giniLoadingIndicator()
        assertNotNull("Gini loading indicator vanished", mark)
        assertEquals(analysisScreen.giniLoadingIndicatorDescription, mark?.contentDescription)
    }

    /** R9: with an empty list nothing changes — no badge, the SDK's own spinner. */
    @Test
    fun test3_pdf_brandOff_showsDefaultIndicatorAndNoBadge() {
        armBrand(BRAND_OFF, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertBrandOffDuringAnalysis()
    }

    /** R10: a value the SDK does not know is ignored, and the flow still ends normally. */
    @Test
    fun test4_pdf_unknownValue_behavesLikeBrandOff() {
        armBrand(setOf("Foo"), PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertBrandOffDuringAnalysis()
        assertTrue(
            "Analysis must end on the extraction screen",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
    }

    /** R11: the screen name is matched without regard to case. */
    @Test
    fun test5_pdf_lowercaseAnalysis_showsBrand() {
        armBrand(setOf("analysis"), PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Powered by Gini badge is not shown", analysisScreen.waitForPoweredByGini())
        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
    }

    /**
     * R12: the first tip takes the badge's place, and the badge stays away while the tips cycle.
     *
     * The first tip slides in 5 s after the Analysis screen starts, so the "badge before the tip"
     * check has to run inside that window.
     */
    @Test
    fun test6_image_brandOn_badgeHidesWhenFirstTipAppears() {
        armBrand(BRAND_ON, IMAGE_DELAY_MS)

        uploadFixtureInvoiceAndProcess(ANY_IMAGE)

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue(
            "Powered by Gini badge must be shown before the first tip",
            analysisScreen.waitForPoweredByGini(BEFORE_FIRST_TIP_TIMEOUT_MS)
        )
        assertFalse("No tip is expected yet", analysisScreen.isTipDisplayed())

        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertTrue(
            "Powered by Gini badge must hide when the first tip appears",
            analysisScreen.waitForPoweredByGiniGone()
        )

        val firstTip = analysisScreen.currentTipHeadline()
        assertTrue("The next tip did not appear", analysisScreen.waitForNextTip(firstTip))
        assertFalse(
            "Powered by Gini badge must stay hidden while the tips cycle",
            analysisScreen.isPoweredByGiniDisplayed()
        )
    }

    /** R13: without the brand the tips appear as before, and there is never a badge. */
    @Test
    fun test7_image_brandOff_tipsWithoutBadge() {
        armBrand(BRAND_OFF, IMAGE_DELAY_MS)

        uploadFixtureInvoiceAndProcess(ANY_IMAGE)

        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
        assertTrue("No capture suggestion appeared", analysisScreen.waitForTip())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
    }

    /** R14: the bank's own indicator cannot replace the Gini mark. */
    @Test
    fun test8_customIndicator_brandOn_giniMarkWins() {
        ScreenLoadingIndicatorConfigurator.applyScreenCustomLoadingIndicator(
            activityRule.scenario,
            enabled = true
        )
        armBrand(BRAND_ON, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Gini loading indicator is not shown", analysisScreen.waitForGiniLoadingIndicator())
        assertFalse(
            "The custom indicator must not be shown while the brand is on",
            analysisScreen.isCustomLoadingIndicatorDisplayed()
        )
    }

    /** R15: without the brand, the bank's own indicator keeps working as today. */
    @Test
    fun test9_customIndicator_brandOff_customIndicatorShown() {
        ScreenLoadingIndicatorConfigurator.applyScreenCustomLoadingIndicator(
            activityRule.scenario,
            enabled = true
        )
        armBrand(BRAND_OFF, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Custom loading indicator is not shown", analysisScreen.waitForCustomLoadingIndicator())
        assertFalse("No Gini loading indicator expected", analysisScreen.isGiniLoadingIndicatorDisplayed())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
    }

    /**
     * R4: closing the Analysis screen cancels the pending analysis. If the mock still delivered
     * its result, the flow would jump to the extraction screen after the user left.
     */
    @Test
    fun test10_closeDuringDelayedAnalysis_callbackNeverArrives() {
        armBrand(BRAND_ON, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)
        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())

        pressBack()
        SystemClock.sleep(PDF_DELAY_MS + AFTER_DELAY_MARGIN_MS)

        assertTrue("Main screen is expected after closing the analysis", mainScreen.assertDescriptionTitle())
        assertFalse(
            "The cancelled analysis must never open the extraction screen",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    /**
     * R32: the badge is read as the brand name, in English on every device language — it is a
     * name, not a translated label.
     */
    @Test
    fun test11_pdf_brandOn_badgeIsReadAsPoweredByGini() {
        armBrand(BRAND_ON, PDF_DELAY_MS)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Powered by Gini badge is not shown", analysisScreen.waitForPoweredByGini())
        assertEquals("Powered by Gini", analysisScreen.poweredByGiniContentDescription())
    }

    private fun armBrand(ingredientBrandScreens: Set<String>, analysisDelayMillis: Long) {
        armMockBackend(
            UiTestMockScenario.INVOICE,
            creditNoteHintEnabled = false,
            ingredientBrandScreens = ingredientBrandScreens,
            analysisDelayMillis = analysisDelayMillis
        )
    }

    private fun assertBrandOffDuringAnalysis() {
        assertTrue("Analysis screen did not appear", analysisScreen.waitForAnalysisScreen())
        assertTrue("Default loading indicator is not shown", analysisScreen.waitForDefaultLoadingIndicator())
        assertFalse("No badge expected", analysisScreen.isPoweredByGiniDisplayed())
        assertFalse("No Gini loading indicator expected", analysisScreen.isGiniLoadingIndicatorDisplayed())
    }

    private companion object {
        val BRAND_ON = setOf("Analysis")
        val BRAND_OFF = emptySet<String>()

        /** BrowserStack pre-loads this PDF as media; the mock never reads it. */
        const val ANY_PDF = "sample.pdf"
        const val ANY_IMAGE = CreditNoteFixtures.PLAIN_INVOICE_ASSET

        /** Long enough to check the screen; PDFs show no tips, so no tip timing is involved. */
        const val PDF_DELAY_MS = 8_000L

        /** Two tips need about 11 s (first at 5.5 s, the next one cycle later). */
        const val IMAGE_DELAY_MS = 20_000L

        /** The first tip starts sliding in 5 s after the Analysis screen starts. */
        const val BEFORE_FIRST_TIP_TIMEOUT_MS = 4_000L
        const val AFTER_DELAY_MARGIN_MS = 2_000L
    }
}
