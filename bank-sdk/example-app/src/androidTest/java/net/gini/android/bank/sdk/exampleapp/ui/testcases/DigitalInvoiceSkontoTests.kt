package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.SkontoFixtures
import net.gini.android.bank.sdk.exampleapp.ui.screens.DigitalInvoiceScreen
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The digital invoice screen when the invoice carries **both** line items and a skonto
 * discount — Xray smoke case TC-013 (PP-3428).
 *
 * ## NOT RUN — excluded from every BrowserStack script
 *
 * This class compiles and its logic is finished, but it cannot pass: **no document yields
 * `lineItems` and `skontoDiscounts` in one analysis result.** Both candidates in the repository
 * were measured on a real device and each returned exactly one of the two — the table in
 * [SkontoFixtures] records which. The document that produced line items produced no skonto,
 * and vice versa, even though both print skonto terms.
 *
 * It is therefore left out of `bs_run_group_smoke.sh` and `bs_run_all_groups.sh` rather than
 * deleted: a guaranteed failure in the release gate teaches the team to ignore the gate,
 * while deleting the work would mean rewriting it when the blocker clears.
 *
 * To re-enable, one of these has to happen first:
 *
 * 1. A document is found that demonstrably extracts both — validate it against the API
 *    *before* committing it, which is what would have shortened this by three runs.
 * 2. The case moves to the mock backend. It worked there; the cost is reintroducing
 *    extraction-faking into a mock that is otherwise limited to configuration flags.
 * 3. Backend confirms whether skonto extraction is suppressed once line items are
 *    detected — that single answer would explain all three measurements.
 *
 * TC-013 stays a manual case in Xray until then.
 *
 * ## Fixture
 *
 * [SkontoFixtures.RA_PAST_ASSET] — the repository's OTTO invoice, which prints both a
 * position table and skonto terms. Its KDoc records the measurement: the line items
 * extract, the skonto discount does not.
 *
 * ## Why the assertions do not assume the discount is claimable
 *
 * This fixture's invoice date is 15.08.2024, so its deadline passed long ago and
 * `GetSkontoEdgeCaseUseCase` reports `SkontoExpired` — the row arrives with the toggle
 * **off**. TC-013 as written says "valid Sconto", and no committed real invoice can satisfy
 * that: a skonto window is only 7-14 days wide, so a real document stops being claimable a
 * fortnight after it was issued. Generating one with a future date is the only way, and the
 * generated attempt's position table does not extract as line items.
 *
 * So these tests read the toggle's *initial* state and assert the direction of change that
 * state implies, rather than hard-coding "on". That covers the substance of the case — the
 * skonto row exists on the digital invoice screen, and both toggles move the total — and
 * leaves the claimable-discount variant to manual QA.
 *
 * Everything here is View-based: the skonto row is an ordinary RecyclerView item
 * (`gbs_item_digital_invoice_skonto.xml`) addressed by view id, so unlike the standalone
 * Skonto screen this needs no Compose test tags and no SDK change.
 */
class DigitalInvoiceSkontoTests : SmokeJourneyTestBase() {

    /** Real upload and analysis, then a full navigation. */
    override val idlingTimeoutMs: Long = 15_000

    private val digitalInvoiceScreen = DigitalInvoiceScreen()

    /**
     * Imports the fixture and steps through the Return Assistant's onboarding screen.
     *
     * This fixture is known to yield `lineItems` without `skontoDiscounts`, so
     * [test1_digitalInvoiceCarriesSkontoRow] fails on the skonto row by design until the
     * blocker above clears — which is why the class is excluded. The assertions below
     * dump the visible text on failure, so a run separates "no line items" (the standalone
     * Skonto screen appears) from "no skonto" (the digital invoice screen appears without a
     * skonto row).
     */
    private fun openDigitalInvoiceWithSkonto() {
        configureReturnAssistantAndSkonto(
            returnAssistantEnabled = true,
            skontoEnabled = true
        )
        importPdfAndAwaitAnalysis(SkontoFixtures.RA_PAST_ASSET)
        // The Return Assistant opens on its own onboarding screen first.
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()
    }

    /**
     * TC-013, first half — the digital invoice screen carries a skonto discount row.
     *
     * The toggle's state is reported rather than asserted: it follows the fixture's deadline,
     * and [test2_lineItemAndSkontoTogglesMoveTotalOppositeWays] is what proves the toggle
     * does something.
     */
    @Test
    fun test1_digitalInvoiceCarriesSkontoRow() {
        openDigitalInvoiceWithSkonto()

        assertTrue(
            "Expected the digital invoice screen. If this fails, the analysis result " +
                "carried no `lineItems`, so the Return Assistant never claimed the flow and " +
                "the standalone Skonto screen is showing instead. " +
                skontoScreen.describeVisibleText(),
            digitalInvoiceScreen.isScreenDisplayed()
        )
        assertTrue(
            "Reached the digital invoice screen but found no Skonto discount row, so the " +
                "analysis result carried `lineItems` without `skontoDiscounts`. " +
                skontoScreen.describeVisibleText(),
            digitalInvoiceScreen.isSkontoRowDisplayed()
        )
    }

    /**
     * TC-013, second half — the two toggles move the total in opposite directions, which is
     * what the manual case walks through.
     *
     * The skonto direction is derived from the toggle's starting state rather than assumed:
     * switching a discount *on* lowers the total, switching it *off* raises it. With this
     * fixture the discount starts off, so the expected move is downwards.
     */
    @Test
    fun test2_lineItemAndSkontoTogglesMoveTotalOppositeWays() {
        openDigitalInvoiceWithSkonto()
        val startTotal = digitalInvoiceScreen.readTotalIntegralPart()

        digitalInvoiceScreen.clickLineItemSwitch()
        val afterItemOff = digitalInvoiceScreen.waitForTotalToChangeFrom(startTotal)
        assertTrue(
            "Expected the total to fall when a line item is switched off, but it went from " +
                "$startTotal to $afterItemOff.",
            afterItemOff < startTotal
        )

        val discountWasOn = digitalInvoiceScreen.isSkontoRowToggleChecked()
        digitalInvoiceScreen.clickSkontoRowSwitch()
        val afterSkontoFlip = digitalInvoiceScreen.waitForTotalToChangeFrom(afterItemOff)

        if (discountWasOn) {
            assertTrue(
                "The discount started on, so switching it off should raise the total, but " +
                    "it went from $afterItemOff to $afterSkontoFlip.",
                afterSkontoFlip > afterItemOff
            )
        } else {
            assertTrue(
                "The discount started off, so switching it on should lower the total, but " +
                    "it went from $afterItemOff to $afterSkontoFlip.",
                afterSkontoFlip < afterItemOff
            )
        }
    }
}
