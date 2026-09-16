package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.AmountText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Xray smoke journeys that end on the extraction screen and had no equivalent in the
 * existing suites: PDF import (TC-004 / PP-3418), picture import (TC-003 / PP-3417) and
 * e-invoice PDF import (TC-016 / PP-3431).
 *
 * The third journey — capture-to-extraction (TC-002 / PP-3416) — is deliberately absent.
 * It needs a real invoice in front of the lens, and BrowserStack does not support camera
 * image injection for Espresso (a build requesting it is rejected outright), so on a device
 * the camera photographs the rack and analysis can only return no-results. The case stays
 * manual; `ReviewScreenTests#test7` still captures pages because it never inspects them.
 *
 * The existing `ImportPdfImageTests` covers the same entry points but stops at the review
 * screen, so it never proves the invoice was analysed. These go through to extraction.
 *
 * Return Assistant and Skonto are switched off in every test: with them on, a SEPA invoice
 * carrying line items or a skonto discount routes into the digital-invoice or Skonto screen
 * instead of the plain extraction screen, and the assertion would fail for the wrong
 * reason. `GiniSmokeUITests.disableReturnAssistantAndSkonto` on iOS does the same thing.
 *
 * These map onto the manual smoke set in Xray (TC-001…TC-026, Jira PP-3415…PP-3442); each
 * test's KDoc names the case it covers.
 */
class SmokeJourneyTests : SmokeJourneyTestBase() {

    /**
     * TC-004 / PP-3418 — a valid SEPA invoice PDF reaches the extraction screen with the
     * payment fields filled.
     */
    @Test
    fun test1_uploadPdfShowsExtractions() {
        configureReturnAssistantAndSkonto(
            returnAssistantEnabled = false,
            skontoEnabled = false
        )

        importPdfAndAwaitAnalysis(SEPA_INVOICE_PDF)

        assertTrue(
            "Expected the extraction screen after analysing the SEPA invoice.",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertTrue(
            "Expected the IBAN extraction to be filled for a valid SEPA invoice.",
            extractionScreen.isExtractionFieldFilled(IBAN_FIELD)
        )
    }

    /**
     * TC-003 / PP-3417 — the same invoice as a picture: imported, processed from the review
     * screen, and analysed through to the extraction screen.
     */
    @Test
    fun test2_uploadPictureShowsExtractions() {
        configureReturnAssistantAndSkonto(
            returnAssistantEnabled = false,
            skontoEnabled = false
        )

        importImageAndAwaitAnalysis(SEPA_INVOICE_IMAGE)

        assertTrue(
            "Expected the extraction screen after analysing the SEPA invoice.",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertTrue(
            "Expected the IBAN extraction to be filled for a valid SEPA invoice.",
            extractionScreen.isExtractionFieldFilled(IBAN_FIELD)
        )
    }

    /**
     * TC-016 / PP-3431 — a valid e-invoice PDF reaches the extraction screen with the payment
     * fields filled.
     *
     * **Real backend on purpose.** This is the one case where mocking the analysis result
     * would destroy the test: TC-016 asserts exactly what TC-004 asserts — the extraction
     * screen with filled fields — so the *only* thing that makes it an e-invoice test is the
     * document. Fake the answer and it becomes TC-004 with a different filename, green even
     * if e-invoice support were completely broken.
     *
     * The fixture is a ZUGFeRD 2 PDF/A-3b carrying an embedded `zugferd-invoice.xml`, which
     * is what the backend reads. Neither this repo nor `gini-mobile-ios` had such a file
     * before; iOS has no e-invoice test at all.
     */
    @Test
    fun test3_uploadEInvoicePdfShowsExtractions() {
        configureReturnAssistantAndSkonto(
            returnAssistantEnabled = false,
            skontoEnabled = false
        )

        importPdfAndAwaitAnalysis(E_INVOICE_PDF)

        assertTrue(
            "Expected the extraction screen after analysing the ZUGFeRD e-invoice.",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )

        // The amount, not the IBAN. This fixture's embedded XML has no IBAN at all — no
        // `IBANID`, no `PayeePartyCreditorFinancialAccount`, not even the string "IBAN" —
        // so a first run failed on an assertion the document could never satisfy. TC-016's
        // own expected result allows the payment fields to "be empty", and its amount is
        // stated twice in the XML (`GrandTotalAmount` and `DuePayableAmount`), which makes
        // it the one value that ties the result to *this* document.
        val extractedAmount = extractionScreen.extractionFieldValue(AMOUNT_FIELD)
        assertEquals(
            "Expected the e-invoice's own total of $E_INVOICE_TOTAL, but the extraction " +
                "read '$extractedAmount'.",
            0,
            AmountText.parse(extractedAmount).compareTo(AmountText.parse(E_INVOICE_TOTAL))
        )
    }

    private companion object {
        const val SEPA_INVOICE_PDF = "sepa_invoice.pdf"
        const val SEPA_INVOICE_IMAGE = "sepa_invoice.png"

        /** ZUGFeRD 2 PDF/A-3b with an embedded `zugferd-invoice.xml`. */
        const val E_INVOICE_PDF = "e_invoice.pdf"

        /** `GrandTotalAmount` / `DuePayableAmount` in that file's embedded XML. */
        const val E_INVOICE_TOTAL = "1005.55"

        /** The example app's extraction row for the amount to pay. */
        const val AMOUNT_FIELD = "amountToPay"

        /**
         * The extraction the SDK exposes as the payee's IBAN. Asserted as non-empty rather
         * than against a literal: the value comes from the live backend, and a test that
         * hardcoded it would also pass against a canned response.
         */
        const val IBAN_FIELD = "iban"
    }
}
