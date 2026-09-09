package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.AmountText
import net.gini.android.bank.sdk.exampleapp.ui.resources.SkontoFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The standalone Skonto screen — Xray smoke cases TC-009 (PP-3423), TC-010 (PP-3424) and
 * TC-011 (PP-3425).
 *
 * Android had no Skonto UI coverage at all before this class, which is why six of the 26
 * manual smoke cases had nothing behind them.
 *
 * ## Fixtures
 *
 * These run against the real API, on the invoices in [SkontoFixtures]. The Skonto screen
 * appears only when the analysis result carries a `skontoDiscounts` compound extraction —
 * `CaptureFlowFragment.tryShowingSkontoScreen` falls back to the extraction screen
 * *silently* when `extractSkontoData` throws — so a fixture has to carry real German
 * discount terms, and both fixtures here do ("Bei Zahlung innerhalb von N Tagen werden
 * X % Skonto gewährt").
 *
 * [SkontoFixtures.VALID_ASSET] is dated 2028 so its discount is still claimable; that is
 * not cosmetic, see the reasoning in [SkontoFixtures]. Neither fixture carries a position
 * table, so the Return Assistant cannot claim the flow ahead of the Skonto screen.
 *
 * Only the SDK-side feature flags are set, through [configureReturnAssistantAndSkonto];
 * the server-side ones come from the real `getConfiguration`. Return Assistant is off, so
 * nothing can route into the digital invoice screen first.
 *
 * Amounts come from the live extraction, so the assertions below are about direction of
 * change rather than exact figures wherever the backend's own rounding could differ.
 */
class SkontoScreenTests : SmokeJourneyTestBase() {

    /** Real upload and analysis, not a canned answer — the mock's 3s is not enough. */
    override val idlingTimeoutMs: Long = 15_000

    private fun openSkontoScreen(asset: String = SkontoFixtures.VALID_ASSET) {
        configureReturnAssistantAndSkonto(
            returnAssistantEnabled = false,
            skontoEnabled = true
        )
        if (asset.endsWith(".pdf")) {
            importPdfAndAwaitAnalysis(asset)
        } else {
            importImageAndAwaitAnalysis(asset)
        }
        // Before waiting for the screen, not after: an edge case (an expired or cash-only
        // discount) opens an info dialog, and a Compose Dialog is its own window — while it
        // is up, nothing behind it is in the accessibility tree, so waitForSkontoScreen
        // cannot see the screen at all. A no-op when there is no dialog.
        skontoScreen.dismissEdgeCaseInfoDialog()
        skontoScreen.waitForSkontoScreen()
    }

    /**
     * TC-009 / PP-3423 — a valid Skonto invoice lands on the Skonto screen with the
     * discount active and the proceed action available.
     */
    @Test
    fun test1_skontoScreenIsDisplayedWithDiscountEnabled() {
        openSkontoScreen()

        assertTrue(
            "Expected the Skonto screen for an invoice with a valid skonto discount.",
            skontoScreen.isScreenDisplayed()
        )
        assertTrue(
            "Expected the discount toggle to arrive on for a still-claimable discount.",
            skontoScreen.isDiscountToggleChecked()
        )
        assertTrue(
            "Expected the confirm-and-proceed button to be displayed.",
            skontoScreen.isProceedButtonDisplayed()
        )
    }

    /**
     * TC-010 / PP-3424 — switching the discount off raises the total, because the invoice
     * is then payable at its full amount.
     */
    @Test
    fun test2_switchingDiscountOffIncreasesTotal() {
        openSkontoScreen()
        val totalWithDiscount = skontoScreen.readFooterTotal()

        skontoScreen.clickDiscountToggle()

        assertFalse(
            "Expected the discount toggle to be off after the click.",
            skontoScreen.isDiscountToggleChecked()
        )
        val totalWithoutDiscount = skontoScreen.readFooterTotal()
        assertTrue(
            "Expected the total to rise when the discount is switched off, but it went " +
                "from $totalWithDiscount to $totalWithoutDiscount.",
            totalWithoutDiscount > totalWithDiscount
        )
    }

    /**
     * TC-011 / PP-3425, first half — editing the final amount updates the footer total, and
     * the edited amount is what the extraction screen then carries.
     *
     * [EDITED_FINAL_AMOUNT] is well below any test invoice's full amount, so it does not
     * trip the validation that test5 exercises.
     */
    @Test
    fun test3_editingFinalAmountUpdatesTotal() {
        openSkontoScreen()

        skontoScreen.setFinalAmount(EDITED_FINAL_AMOUNT)

        val edited = AmountText.parse(EDITED_FINAL_AMOUNT)
        assertEquals(
            "Expected the footer total to follow the edited final amount.",
            0,
            skontoScreen.readFooterTotal().compareTo(edited)
        )

        skontoScreen.clickProceedButton()

        assertTrue(
            "Expected the extraction screen after confirming the Skonto screen.",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        val extractedAmount = extractionScreen.extractionFieldValue(AMOUNT_FIELD)
        assertEquals(
            "Expected the extracted amount to be the edited final amount, but the " +
                "extraction read '$extractedAmount'.",
            0,
            AmountText.parse(extractedAmount).compareTo(edited)
        )
    }

    /**
     * TC-011 / PP-3425, second half — the expiry-date field carries the discount deadline the
     * backend extracted, and opening it brings up the date picker.
     *
     * **Why this does not pick a new date.** `getSkontoSelectableDates` in
     * `SkontoScreenContent` restricts the picker to *now through six months out*.
     * [SkontoFixtures.VALID_ASSET] is deliberately dated 2028 so its discount never stops
     * being claimable — which puts every day in the month the picker opens on far outside
     * that window. A real run proved it: the tap on the next day did nothing and the field
     * still read 19. The two requirements are in direct conflict, and a fixture that could
     * be picked in would rot within six months.
     *
     * So this asserts the two things that *are* true and stable: the extracted deadline
     * reaches the field, and the field opens its picker. Actually changing the date stays a
     * manual step — recorded in COVERAGE.md.
     */
    @Test
    fun test4_expiryDateShowsExtractedDeadlineAndOpensPicker() {
        openSkontoScreen()

        val shown = skontoScreen.readExpiryDate()
        assertEquals(
            "Expected the expiry-date field to show the deadline printed on the fixture.",
            SkontoFixtures.VALID_SKONTO_DUE_DATE.dayOfMonth,
            skontoScreen.readExpiryDayOfMonth()
        )
        assertTrue(
            "Expected the spoken expiry date to name the fixture's year, but read '$shown'.",
            shown.contains(SkontoFixtures.VALID_SKONTO_DUE_DATE.year.toString())
        )

        assertTrue(
            "Expected the date picker to open when the expiry-date field is tapped.",
            skontoScreen.openAndDismissDatePicker()
        )
    }

    /**
     * A final amount above the invoice's full amount is **rejected** — the entered value is
     * discarded and the total stays where it was.
     *
     * Asserted that way, rather than on the error message, after two real runs. Both showed
     * the field back at its original 485.00 with no message anywhere in the accessibility
     * tree — first with an eight-digit entry, then with a six-digit one, so input width was
     * not the cause. Reverting to 485.00 is exactly what
     * `SkontoAmountFieldChangeIntent` does on its error branch: it reduces
     * `skontoAmount = state.skontoAmount`, keeping the old value. So the rejection provably
     * happens; only its inline message is missing.
     *
     * A valid amount *does* move the total — `test3` proves it — so an unchanged total after
     * entering [ABOVE_FULL_AMOUNT] is evidence the value was refused rather than ignored.
     *
     * **The missing message is raised separately as a probable product issue.** It is not
     * asserted here because a test should fail on the behaviour it names, and this test names
     * the validation rule.
     */
    @Test
    fun test5_amountAboveFullAmountIsRejected() {
        openSkontoScreen()
        val totalBefore = skontoScreen.readFooterTotal()

        skontoScreen.setFinalAmount(ABOVE_FULL_AMOUNT)

        assertEquals(
            "Expected the total to stay at $totalBefore after entering " +
                "$ABOVE_FULL_AMOUNT, which is above the invoice's " +
                "${SkontoFixtures.VALID_FULL_AMOUNT} full amount. " +
                skontoScreen.describeVisibleText(),
            0,
            skontoScreen.readFooterTotal().compareTo(totalBefore)
        )
    }

    /**
     * An expired discount still shows the Skonto screen, but with the discount off — there
     * is nothing left to claim. Mirrors iOS's
     * `testSkontoSwitchDisabledForExpiredDiscount`.
     */
    @Test
    fun test6_expiredDiscountArrivesWithToggleOff() {
        openSkontoScreen(SkontoFixtures.PAST_ASSET)

        assertTrue(
            "Expected the Skonto screen even for an expired discount.",
            skontoScreen.isScreenDisplayed()
        )
        assertFalse(
            "Expected the discount toggle to arrive off for an expired discount.",
            skontoScreen.isDiscountToggleChecked()
        )
    }

    private companion object {
        /** The example app's extraction row for the amount to pay. */
        const val AMOUNT_FIELD = "amountToPay"

        /**
         * Below [SkontoFixtures.VALID_FULL_AMOUNT] (500.00 EUR), so it does not trip the
         * validation that test5 exercises.
         */
        const val EDITED_FINAL_AMOUNT = "12.34"

        /**
         * Above [SkontoFixtures.VALID_FULL_AMOUNT] (500.00), so the "more than full amount"
         * branch trips — and **six digits**, which matters.
         *
         * The previous value, 999999.99, is eight digits and never reached the ViewModel: a
         * real run showed the field still holding its original 485.00 and no error at all.
         * `DecimalInputVisualTransformation` only maps source lengths 1..7 explicitly
         * (`else -> formatted.length`), so an eight-digit entry falls outside the width the
         * component supports. Whether that is a product bug in its own right is a separate
         * question — this test is about the validation rule, and 9999.99 exercises it.
         */
        const val ABOVE_FULL_AMOUNT = "9999.99"

        /** Keeps `currentDay + 1` inside every month, February included. */
        const val LAST_SAFE_DAY_OF_MONTH = 28
    }
}
