package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the fraud prevention warning must NOT appear (PP-3970), driven by
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * Xray test cases covered:
 *
 * | Xray       | What                                              | Test |
 * |------------|---------------------------------------------------|------|
 * | TC-002/003 | `fraudSignalsEnabled` off + fraud payload         | [test1_noWarningWhenFlagIsOff] |
 * | TC-004     | an unknown `signalCode` is ignored                | [test2_noWarningForUnknownSignalCode] |
 * | TC-038     | no `fraudSignals` in the result                   | [test3_noWarningWhenFraudSignalsAreMissing] |
 * | TC-006     | an ordinary invoice with the flag on              | [test4_noWarningForOrdinaryInvoiceWithFlagOn] |
 * | TC-013     | the analysis fails                                | [test5_errorScreenAndNoWarningWhenAnalysisFails] |
 *
 * The gate under test is `FraudSignalsParser.parse`, which returns no signals when the client
 * flag is off and skips elements with an unknown code, and `IBANMismatchArgsMapper.map`, which
 * returns no warning without signals.
 */
class FraudSignalsFlagTests : FraudWarningTestBase() {

    /**
     * TC-002 / TC-003: the flag is off on the backend, so the SDK ignores a payload that would
     * otherwise show the combined warning, and hands the extractions on unchanged.
     */
    @Test
    fun test1_noWarningWhenFlagIsOff() {
        processWithScenario(UiTestMockScenario.FRAUD_COMBINED, fraudSignalsEnabled = false)

        assertNoWarningAndExtractionsShown()
        assertTrue(
            "iban must be handed over when no warning was shown",
            extractionScreen.isExtractionFieldFilled("iban")
        )
    }

    /** TC-004: the only element has a code this SDK does not know, so it is skipped. */
    @Test
    fun test2_noWarningForUnknownSignalCode() {
        processWithScenario(UiTestMockScenario.FRAUD_UNKNOWN_CODE)

        assertNoWarningAndExtractionsShown()
    }

    /** TC-038: the result has the fraud invoice's IBANs but no `fraudSignals` at all. */
    @Test
    fun test3_noWarningWhenFraudSignalsAreMissing() {
        processWithScenario(UiTestMockScenario.FRAUD_SIGNALS_MISSING)

        assertNoWarningAndExtractionsShown()
    }

    /** TC-006: an ordinary invoice with the flag on goes straight to the extractions. */
    @Test
    fun test4_noWarningForOrdinaryInvoiceWithFlagOn() {
        processWithScenario(UiTestMockScenario.INVOICE)

        assertNoWarningAndExtractionsShown()
    }

    /** TC-013: when the analysis fails the SDK opens its Error screen, and no warning. */
    @Test
    fun test5_errorScreenAndNoWarningWhenAnalysisFails() {
        processWithScenario(UiTestMockScenario.ANALYSIS_ERROR)

        assertTrue("Error screen did not appear", analysisScreen.waitForView("gc_error_header"))
        assertFalse("Fraud warning must not appear", fraudWarning.isAnyWarningDisplayed())
    }
}
