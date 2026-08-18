package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.DueDateFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI tests for the Schedule Payment state of the warning bottom sheet on the Analysis
 * screen (PP-3264, automated per PP-3301 from the PP-3263 XRay test cases).
 *
 * The Schedule Payment state shows whenever the client paymentScheduleHint flag is ON and
 * the invoice qualifies — it takes priority over the Due Date Hint and is independent of
 * the paymentDueHint flag. Its primary CTA hands the extractions back to the host app as
 * CaptureResult.SchedulePayment, which the example app surfaces via the
 * scheduled-payment indicator on the extraction screen (the observable that
 * distinguishes it from a pay-now Success result).
 *
 * Shared scaffolding and stability assumptions: see [WarningBottomSheetTestBase].
 */
class SchedulePaymentBottomSheetTests : WarningBottomSheetTestBase() {

    /**
     * R2 (priority): with BOTH flags on, the Schedule Payment state shows — not the Due
     * Date Hint. The description and CTA labels are the distinguishing assertions; the
     * title is shared between the two states.
     */
    @Test
    fun test1_schedulePaymentSheetIsDisplayedWhenBothFlagsOn() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertSchedulePaymentState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R2 (independence): the schedule state shows even with the due flag OFF.
     */
    @Test
    fun test2_schedulePaymentSheetIsDisplayedWhenOnlyScheduleFlagOn() {
        configureHints(paymentDueHintEnabled = false, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertSchedulePaymentState(DueDateFixtures.FORMATTED_DUE_DATE)
    }

    /**
     * R4 analog for the schedule state: below the threshold no sheet shows.
     */
    @Test
    fun test3_sheetIsNotDisplayedWhenRemainingDaysBelowThreshold() {
        assumeNotCloseToMidnight()
        configureHints(
            paymentDueHintEnabled = true,
            paymentScheduleHintEnabled = true,
            thresholdDays = DueDateFixtures.remainingDays() + 1
        )
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse("Sheet must not show below threshold", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R12: the "Schedule Payment" primary CTA finishes the flow with
     * CaptureResult.SchedulePayment — asserted via the scheduled-payment indicator on the
     * extraction screen. Landing on the extraction screen alone would not distinguish
     * this from "Proceed Anyway".
     */
    @Test
    fun test4_schedulePaymentButtonShowsScheduledPaymentIndicator() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertSchedulePaymentState(DueDateFixtures.FORMATTED_DUE_DATE)

        warningBottomSheet.clickPrimaryButton()

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertTrue(
            "Scheduled-payment indicator missing — the result was not SchedulePayment",
            extractionScreen.isScheduledPaymentIndicatorDisplayed()
        )
        assertFalse("Sheet must be dismissed", warningBottomSheet.isSheetDisplayed())
    }

    /**
     * R13: the "Proceed Anyway" secondary CTA continues as a pay-now Success — the
     * scheduled-payment indicator must NOT be shown.
     */
    @Test
    fun test5_proceedAnywayDismissesSheetAndShowsExtractions() {
        configureHints(paymentDueHintEnabled = true, paymentScheduleHintEnabled = true)
        uploadFixtureInvoiceAndProcess(DueDateFixtures.FUTURE_DUE_ASSET)
        assertTrue("Schedule Payment sheet did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickSecondaryButton()

        assertTrue("Extraction screen did not appear", extractionScreen.assertExtractionScreenIsDisplayed())
        assertFalse(
            "Scheduled-payment indicator shown for a pay-now result",
            extractionScreen.isScheduledPaymentIndicatorDisplayed()
        )
        assertFalse("Sheet must be dismissed", warningBottomSheet.isSheetDisplayed())
    }
}
