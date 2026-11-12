package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The feedback the SDK sends after the fraud prevention warning (PP-3970), for every warning type,
 * driven by [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * Xray test cases covered (TC-005, one test per case):
 *
 * | Xray   | Case                               | `iban`       | `bic`      | Test |
 * |--------|------------------------------------|--------------|------------|------|
 * | TC-005 | QR-vs-printed, printed IBAN chosen | printed IBAN | sent empty | [test1_qrVsPrintedPrintedIbanChosen] |
 * | TC-005 | QR-vs-printed, QR code IBAN chosen | QR code IBAN | QR_BIC     | [test2_qrVsPrintedQrIbanChosen] |
 * | TC-005 | combined, printed IBAN chosen      | printed IBAN | sent empty | [test3_combinedPrintedIbanChosen] |
 * | TC-005 | country, "Proceed anyway"          | backend IBAN | QR_BIC     | [test4_countryProceedAnyway] |
 *
 * `iban` and `bic` are the values of the feedback's specific extractions; the country case has
 * nothing to choose, so it keeps the backend's `iban` (the QR code IBAN) and its BIC. Every test
 * also checks that the compound extractions of the feedback have no `fraudSignals`; see
 * "What the `fraudSignals` check does not prove" below.
 *
 * ## How the feedback is sent
 *
 * The example app sends it from the Extractions screen's "transfer summary" action:
 * `ExtractionsActivity.sendTransferSummaryAndClose` → `GiniBank.sendTransferSummary` →
 * `GiniCapture.sendTransferSummary(paymentRecipient, …, iban, bic, amount, instantPayment)`, which
 * builds the specific extractions from those values, takes the stored compound extractions and
 * calls `sendFeedback` on the mock network service. The mock records the values
 * (`UiTestMockBackend.lastFeedbackExtractionValues`) and the compound map.
 *
 * ## What the `fraudSignals` check does not prove
 *
 * The stored compound map is empty in these flows. `GiniCapture` starts it as an empty map, and
 * only `setUpdatedCompoundExtractions` writes it: with an empty map, or from the Return Assistant
 * (`DigitalInvoiceScreenPresenter`), which these flows never open. So the feedback's compound map
 * is `{}`, and the check passes whether or not `GiniCapture.sendTransferSummary` removes
 * `fraudSignals`. It only guards against a future change that stores the analysis compound map.
 * Proving the removal itself needs a capture-sdk unit test.
 *
 * ## "No BIC" is a `bic` key with an empty value, not a missing key
 *
 * - Choosing a printed IBAN removes `bic` from the SDK's result (`ChosenIbanExtractions.apply`:
 *   `extractions - BIC_KEY`).
 * - The example app re-adds every missing SEPA field as an empty row
 *   (`ExtractionsActivity.setUpRecyclerView`), and reads the fields it sends with
 *   `extraction(key) = mExtractions[key]?.value ?: ""`, so it passes `bic = ""`.
 * - `GiniCapture.sendTransferSummary` always puts a `bic` extraction with the value it was given.
 *
 * So the feedback carries `bic` = `""`. The test asserts exactly that: a missing key would mean
 * one of the three places above changed, and should fail.
 */
class FraudFeedbackTests : FraudWarningTestBase() {

    /** TC-005: QR-vs-printed, the printed IBAN chosen: iban = printed IBAN, bic sent empty. */
    @Test
    fun test1_qrVsPrintedPrintedIbanChosen() {
        processWithScenario(UiTestMockScenario.FRAUD_QR_MISMATCH)
        assertWarningShown(FraudWarningScreen.Type.QR_VS_PRINTED)
        proceedAndChoose(UiTestMockFraudData.PRINTED_IBAN)

        sendTransferSummary()

        assertFeedback(expectedIban = UiTestMockFraudData.PRINTED_IBAN, expectedBic = EMPTY_BIC)
    }

    /** TC-005: QR-vs-printed, the QR code IBAN chosen: iban = QR code IBAN, its bic kept. */
    @Test
    fun test2_qrVsPrintedQrIbanChosen() {
        processWithScenario(UiTestMockScenario.FRAUD_QR_MISMATCH)
        assertWarningShown(FraudWarningScreen.Type.QR_VS_PRINTED)
        proceedAndChoose(UiTestMockFraudData.QR_IBAN)

        sendTransferSummary()

        assertFeedback(
            expectedIban = UiTestMockFraudData.QR_IBAN,
            expectedBic = UiTestMockFraudData.QR_BIC
        )
    }

    /** TC-005: combined, the printed IBAN chosen: iban = printed IBAN, bic sent empty. */
    @Test
    fun test3_combinedPrintedIbanChosen() {
        processWithScenario(UiTestMockScenario.FRAUD_COMBINED)
        assertWarningShown(FraudWarningScreen.Type.COMBINED)
        proceedAndChoose(UiTestMockFraudData.PRINTED_IBAN)

        sendTransferSummary()

        assertFeedback(expectedIban = UiTestMockFraudData.PRINTED_IBAN, expectedBic = EMPTY_BIC)
    }

    /**
     * TC-005: country only. "Proceed anyway" opens no selection and continues with no choice, so
     * the feedback keeps the backend's `iban` extraction and its bic.
     */
    @Test
    fun test4_countryProceedAnyway() {
        processWithScenario(UiTestMockScenario.FRAUD_COUNTRY_MISMATCH)
        assertWarningShown(FraudWarningScreen.Type.COUNTRY)
        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())
        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse("IBAN selection must not open", ibanSelection.isSheetDisplayed())

        sendTransferSummary()

        assertFeedback(
            expectedIban = UiTestMockFraudData.QR_IBAN,
            expectedBic = UiTestMockFraudData.QR_BIC
        )
    }

    /** "Proceed anyway", choose [iban] on the selection sheet, "Continue", Extractions screen. */
    private fun proceedAndChoose(iban: String) {
        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())
        assertTrue("IBAN selection did not open", ibanSelection.waitForSheet())
        assertTrue("IBAN row $iban not found", ibanSelection.selectIban(iban))
        assertTrue("\"Continue\" not found", ibanSelection.clickContinue())
        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
    }

    private fun sendTransferSummary() {
        extractionScreen.clickTransferSummaryButton()
    }

    /**
     * The feedback the mock recorded: [expectedIban] and [expectedBic] as the `iban` and `bic`
     * specific extractions, and no `fraudSignals` among the compound extractions (a guard only:
     * the compound map is empty in these flows, see the class KDoc).
     */
    private fun assertFeedback(expectedIban: String, expectedBic: String) {
        val compound = awaitFeedbackCompoundExtractions()
        assertNotNull("The SDK sent no feedback after the transfer summary", compound)
        assertFalse(
            "The feedback must not contain fraudSignals, but had ${compound?.keys}",
            compound.orEmpty().containsKey(UiTestMockFraudData.FRAUD_SIGNALS_KEY)
        )

        val extractions = awaitFeedbackExtractionValues().orEmpty()
        assertEquals("Feedback iban, in $extractions", expectedIban, extractions[IBAN_KEY])
        assertTrue(
            "Feedback must always carry a bic extraction, but had ${extractions.keys}",
            extractions.containsKey(BIC_KEY)
        )
        assertEquals("Feedback bic, in $extractions", expectedBic, extractions[BIC_KEY])
    }

    private companion object {
        const val IBAN_KEY = "iban"
        const val BIC_KEY = "bic"

        /** What the feedback carries for "no BIC"; see the class KDoc. */
        const val EMPTY_BIC = ""
    }
}
