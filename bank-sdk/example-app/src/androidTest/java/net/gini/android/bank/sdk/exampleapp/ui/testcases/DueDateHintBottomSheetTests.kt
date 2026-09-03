package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.DueDateFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI tests for the Due Date Hint state of the warning bottom sheet on the Analysis screen
 * (automated from the XRay test cases).
 *
 * The Due Date Hint shows when the client flags are paymentDueHint=ON /
 * paymentScheduleHint=OFF, the extractions carry paymentState=ToBePaid and a
 * paymentDueDate at least paymentDueHintThresholdDays in the future. Show/not-show cases
 * are driven by moving the threshold relative to the fixed fixture due date (see
 * [DueDateFixtures]) instead of needing invoices with different dates.
 *
 * Shared scaffolding and stability assumptions: see [WarningBottomSheetTestBase].
 */
class DueDateHintBottomSheetTests : WarningBottomSheetTestBase() {


    /**
     * R1: with due=ON / schedule=OFF and a qualifying invoice the Due Date Hint sheet
     * shows with the formatted due date, description and both CTAs.
     */
    @Test
    fun test1_dueDateHintSheetIsDisplayedWithCorrectContent() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertDueDateHintState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R3: boundary — remaining days exactly equal to the threshold still shows the sheet.
     */
    @Test
    fun test2_sheetIsDisplayedWhenRemainingDaysEqualThreshold() {
        assumeNotCloseToMidnight()
        configureHints(
            paymentDueHintEnabled = true,
            paymentScheduleHintEnabled = false,
            thresholdDays = DueDateFixtures.remainingDays()
        )
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Sheet must show when remainingDays == threshold", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertDueDateHintState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R4: remaining days below the threshold — no sheet, flow continues to extractions.
     */
    @Test
    fun test3_sheetIsNotDisplayedWhenRemainingDaysBelowThreshold() {
        assumeNotCloseToMidnight()
        configureHints(
            paymentDueHintEnabled = true,
            paymentScheduleHintEnabled = false,
            thresholdDays = DueDateFixtures.remainingDays() + 1
        )
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show below threshold", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R5: both client flags off — neither sheet state shows, flow continues to extractions.
     */
    @Test
    fun test4_noSheetIsDisplayedWhenBothFlagsAreOff() {
        configureHints(paymentDueHintEnabled = false, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show with both flags off", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R8: the sheet is not cancelable — tapping the scrim outside it does nothing.
     */
    @Test
    fun test5_tapOutsideDoesNotDismissSheet() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.tapOutsideSheet()

        assertTrue("Sheet was dismissed by an outside tap", warningBottomSheet.isSheetDisplayed())
        warningBottomSheet.assertDueDateHintState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R9: the rotating capture suggestions stay suppressed while the sheet is shown. The
     * watch window exceeds AnalysisHintsAnimator.HINT_START_DELAY (5000 ms).
     */
    @Test
    fun test6_captureSuggestionsAreSuppressedWhileSheetIsShown() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        assertFalse(
            "Capture suggestions appeared while the sheet was shown",
            warningBottomSheet.didCaptureSuggestionsAppearWithin(SUGGESTIONS_WATCH_WINDOW)
        )
        assertTrue("Sheet disappeared during the watch window", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R10: "Proceed Anyway" (primary CTA) dismisses the sheet and continues to the
     * extraction screen.
     */
    @Test
    fun test7_proceedAnywayDismissesSheetAndShowsExtractions() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickPrimaryButton()

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must be dismissed", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R11: "Cancel Transfer" (secondary CTA) dismisses the sheet, cancels the transaction
     * and returns to the example app's landing page.
     */
    @Test
    fun test8_cancelTransferDismissesSheetAndReturnsToMainScreen() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = false)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Due Date Hint sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickSecondaryButton()

        idlingResource.waitForIdle()
        assertTrue("Landing page did not appear", mainScreen.assertDescriptionTitle())
    }

    /**
     * R6: an invoice without a paymentDueDate extraction never shows a sheet, even with
     * both flags on.
     */
    @Test
    fun test9_noSheetWhenDueDateExtractionIsEmpty() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.NO_DUE_DATE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show without a due date", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R7: a Return Assistant invoice suppresses the sheet — the digital invoice flow
     * starts instead. The PDF import flow (and why the copy to Downloads is wrapped) lives in
     * [WarningBottomSheetTestBase.uploadFixturePdfAndProcess]; this test used to inline it.
     */
    @Test
    fun test10_noSheetForReturnAssistantInvoice() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixturePdfAndProcess(CreditNoteFixtures.RETURN_ASSISTANT_ASSET)

        assertTrue(
            "Digital invoice onboarding did not appear",
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        )
        assertFalse("Sheet must not show for RA invoices", warningBottomSheet.isSheetDisplayed())
    }

    companion object {
        private const val SUGGESTIONS_WATCH_WINDOW = 6_500L
    }
}
