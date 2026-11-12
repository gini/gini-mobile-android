package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The IBAN selection sheet that "Proceed anyway" opens on the fraud prevention warning
 * (PP-3970), driven by [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * Xray test cases covered:
 *
 * | Xray       | What                                                   | Test |
 * |------------|--------------------------------------------------------|------|
 * | TC-026/027 | sheet content, QR-vs-printed and combined              | [test1_selectionContentQrVsPrinted], [test2_selectionContentCombined] |
 * | TC-028     | back closes only the sheet, the warning stays          | [test3_backClosesOnlyTheSelection] |
 * | TC-029     | "Continue" without a choice shows the error            | [test4_continueWithoutSelectionShowsError] |
 * | TC-030     | QR code IBAN chosen: iban = QR code IBAN, bic kept     | [test5_selectQrIbanKeepsBic] |
 * | TC-031     | printed IBAN chosen: iban = printed IBAN, bic empty    | [test6_selectPrintedIbanClearsBic] |
 *
 * What the choice does comes from `ChosenIbanExtractions.apply`: the QR code IBAN equals the
 * backend's `iban`, so the extractions are handed on unchanged; any other IBAN replaces `iban`
 * and removes `bic`, which belongs to the backend's IBAN. The example app re-adds a missing
 * `bic` as an empty row, so "bic empty" is checked as an empty value.
 */
class IbanSelectionSheetTests : FraudWarningTestBase() {

    /** TC-026: the QR code IBAN and the printed IBAN with their labels, no country hints. */
    @Test
    fun test1_selectionContentQrVsPrinted() {
        openSelection(UiTestMockScenario.FRAUD_QR_MISMATCH, FraudWarningScreen.Type.QR_VS_PRINTED)

        assertEquals(
            "IBAN selection is missing texts",
            emptyList<String>(),
            ibanSelection.missingTexts(
                ibanSelection.expectedTexts(
                    qrIban = UiTestMockFraudData.QR_IBAN,
                    printedIbans = listOf(UiTestMockFraudData.PRINTED_IBAN)
                )
            )
        )
    }

    /** TC-027: in the combined case each IBAN also carries its country hint. */
    @Test
    fun test2_selectionContentCombined() {
        openSelection(UiTestMockScenario.FRAUD_COMBINED, FraudWarningScreen.Type.COMBINED)

        assertEquals(
            "IBAN selection is missing texts",
            emptyList<String>(),
            ibanSelection.missingTexts(
                ibanSelection.expectedTexts(
                    qrIban = UiTestMockFraudData.QR_IBAN,
                    printedIbans = listOf(UiTestMockFraudData.PRINTED_IBAN),
                    hintCountryCodes = listOf(
                        UiTestMockFraudData.QR_IBAN.take(2),
                        UiTestMockFraudData.PRINTED_IBAN.take(2)
                    )
                )
            )
        )
    }

    /** TC-028: back closes the selection and returns to the warning, which stays open. */
    @Test
    fun test3_backClosesOnlyTheSelection() {
        openSelection(UiTestMockScenario.FRAUD_QR_MISMATCH, FraudWarningScreen.Type.QR_VS_PRINTED)

        ibanSelection.pressBack()

        assertTrue("IBAN selection did not close on back", ibanSelection.waitForSheetGone())
        assertTrue(
            "Fraud warning must stay open after the selection closed",
            fraudWarning.waitForWarning(FraudWarningScreen.Type.QR_VS_PRINTED, ELEMENT_TIMEOUT)
        )
        assertFalse(
            "Extraction screen must not open",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    /** TC-029: "Continue" with no IBAN chosen shows the error and keeps the sheet open. */
    @Test
    fun test4_continueWithoutSelectionShowsError() {
        openSelection(UiTestMockScenario.FRAUD_QR_MISMATCH, FraudWarningScreen.Type.QR_VS_PRINTED)

        assertTrue("\"Continue\" not found", ibanSelection.clickContinue())

        assertTrue("Selection error did not appear", ibanSelection.waitForError())
        assertTrue("IBAN selection must stay open", ibanSelection.isSheetDisplayed())
        assertFalse(
            "Extraction screen must not open",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    /** TC-030: the QR code IBAN is the backend's IBAN, so iban and bic are handed on as they are. */
    @Test
    fun test5_selectQrIbanKeepsBic() {
        openSelection(UiTestMockScenario.FRAUD_QR_MISMATCH, FraudWarningScreen.Type.QR_VS_PRINTED)

        chooseAndContinue(UiTestMockFraudData.QR_IBAN)

        assertEquals(
            "iban must be the QR code IBAN",
            UiTestMockFraudData.QR_IBAN,
            extractionScreen.extractionFieldValue("iban")
        )
        assertEquals(
            "bic must be kept for the QR code IBAN",
            UiTestMockFraudData.QR_BIC,
            extractionScreen.extractionFieldValue("bic")
        )
    }

    /** TC-031: a printed IBAN replaces iban, and the QR code IBAN's bic is removed. */
    @Test
    fun test6_selectPrintedIbanClearsBic() {
        openSelection(UiTestMockScenario.FRAUD_QR_MISMATCH, FraudWarningScreen.Type.QR_VS_PRINTED)

        chooseAndContinue(UiTestMockFraudData.PRINTED_IBAN)

        assertEquals(
            "iban must be the chosen printed IBAN",
            UiTestMockFraudData.PRINTED_IBAN,
            extractionScreen.extractionFieldValue("iban")
        )
        assertTrue(
            "bic must be empty after choosing a printed IBAN",
            extractionScreen.extractionFieldValue("bic").isNullOrEmpty()
        )
    }

    private fun openSelection(scenario: UiTestMockScenario, type: FraudWarningScreen.Type) {
        processWithScenario(scenario)
        assertWarningShown(type)
        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())
        assertTrue("IBAN selection did not open", ibanSelection.waitForSheet())
    }

    /** Chooses [iban], confirms, and waits until both sheets are gone and the extractions show. */
    private fun chooseAndContinue(iban: String) {
        assertTrue("IBAN row $iban not found", ibanSelection.selectIban(iban))
        assertTrue("\"Continue\" not found", ibanSelection.clickContinue())
        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse("IBAN selection must be closed", ibanSelection.isSheetDisplayed())
        assertFalse("Fraud warning must be closed", fraudWarning.isAnyWarningDisplayed())
    }

    private companion object {
        const val ELEMENT_TIMEOUT = 5_000L
    }
}
