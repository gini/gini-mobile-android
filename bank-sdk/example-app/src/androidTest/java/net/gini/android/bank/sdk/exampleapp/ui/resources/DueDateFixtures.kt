package net.gini.android.bank.sdk.exampleapp.ui.resources

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Fixture data for the Due Date Hint / Schedule Payment bottom sheet tests (PP-3301).
 *
 * The two invoice images were generated with the scripts in
 * `src/androidTest/scripts/fixtures/` and validated against the real Gini API on
 * 2026-08-17:
 * - [FUTURE_DUE_ASSET] extracts paymentDueDate = 2028-09-01 and paymentState = ToBePaid.
 * - [NO_DUE_DATE_ASSET] extracts NO paymentDueDate (paymentState = ToBePaid).
 *
 * REFRESH DEADLINE: regenerate [FUTURE_DUE_ASSET] with a new future due date (and update
 * [FIXTURE_DUE_DATE] + [FORMATTED_DUE_DATE]) before mid-2028, when 2028-09-01 stops
 * being comfortably in the future.
 */
object DueDateFixtures {

    /** Must match the due date printed on [FUTURE_DUE_ASSET]. */
    val FIXTURE_DUE_DATE: LocalDate = LocalDate.of(2028, 9, 1)

    /** [FIXTURE_DUE_DATE] as the sheet renders it (DueDateFormatter uses dd.MM.yyyy). */
    const val FORMATTED_DUE_DATE = "01.09.2028"

    const val FUTURE_DUE_ASSET = "invoice_future_due.jpeg"
    const val NO_DUE_DATE_ASSET = "invoice_no_due_date.jpeg"

    /**
     * Days between today and the fixture due date, computed the same way as
     * AnalysisScreenPresenter.calculateRemainingDays (ChronoUnit.DAYS between LocalDates).
     */
    fun remainingDays(): Int =
        ChronoUnit.DAYS.between(LocalDate.now(), FIXTURE_DUE_DATE).toInt()
}
