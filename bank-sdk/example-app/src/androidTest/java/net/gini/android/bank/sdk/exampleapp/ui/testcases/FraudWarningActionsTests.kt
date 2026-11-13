package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fraud prevention warning's actions (PP-3970), driven by
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * Xray test cases covered:
 *
 * | Xray   | What                                                     | Test |
 * |--------|----------------------------------------------------------|------|
 * | TC-017 | back press keeps the warning, QR-vs-printed              | [test1_backPressKeepsWarning] |
 * | TC-020 | "Cancel transfer", QR-vs-printed                         | [test2_cancelTransferQrVsPrinted] |
 * | TC-021 | "Cancel transfer", country                               | [test3_cancelTransferCountry] |
 * | TC-022 | "Cancel transfer", combined                              | [test4_cancelTransferCombined] |
 * | TC-023 | "Proceed anyway", QR-vs-printed: opens the selection     | [test5_proceedAnywayQrVsPrintedOpensSelection] |
 * | TC-024 | "Proceed anyway", country: straight to the extractions   | [test6_proceedAnywayCountryGoesToExtractions] |
 * | TC-025 | "Proceed anyway", combined: opens the selection          | [test7_proceedAnywayCombinedOpensSelection] |
 * | TC-017 | back press keeps the warning, country                    | [test8_backPressKeepsCountryWarning] |
 * | TC-017 | back press keeps the warning, combined                   | [test9_backPressKeepsCombinedWarning] |
 *
 * The feedback after the warning (TC-005) is in [FraudFeedbackTests].
 *
 * ## What the buttons do, from the code
 *
 * - "Cancel transfer" runs the same steps as the other warning sheets' cancel CTA
 *   (`AnalysisFragmentImpl.showIbanMismatch` → `mCancelListener.onCancelFlow()`, like
 *   `AnalysisFragment.cancelTransaction`): the capture flow is cancelled and the example app is
 *   back on its landing page, as in `CreditNoteWarningTests.test5`.
 * - "Proceed anyway" opens the IBAN selection sheet when there are IBANs to choose from
 *   (`IBANMismatchArgs.toSelectionArgs`: QR-vs-printed and combined), and otherwise (country only)
 *   continues at once with no choice, so the extractions keep the backend's IBAN and BIC.
 * - The warning sheet refuses back, scrim taps and swipes (`GiniBottomSheetScaffold`).
 *
 * The outside tap of TC-017 stays manual: the warning fills its window below the status bar, so
 * on a phone there is no scrim to tap, only the system status bar.
 */
class FraudWarningActionsTests : FraudWarningTestBase() {

    /** TC-017: back does not close the QR-vs-printed warning, and the flow does not move on. */
    @Test
    fun test1_backPressKeepsWarning() {
        assertBackPressKeepsWarning(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )
    }

    /** TC-020: "Cancel transfer" on the QR-vs-printed warning ends the flow. */
    @Test
    fun test2_cancelTransferQrVsPrinted() {
        assertCancelTransferEndsFlow(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )
    }

    /** TC-021: "Cancel transfer" on the country warning ends the flow. */
    @Test
    fun test3_cancelTransferCountry() {
        assertCancelTransferEndsFlow(
            UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
            FraudWarningScreen.Type.COUNTRY
        )
    }

    /** TC-022: "Cancel transfer" on the combined warning ends the flow. */
    @Test
    fun test4_cancelTransferCombined() {
        assertCancelTransferEndsFlow(
            UiTestMockScenario.FRAUD_COMBINED,
            FraudWarningScreen.Type.COMBINED
        )
    }

    /** TC-023: "Proceed anyway" on the QR-vs-printed warning opens the IBAN selection sheet. */
    @Test
    fun test5_proceedAnywayQrVsPrintedOpensSelection() {
        assertProceedAnywayOpensSelection(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )
    }

    /**
     * TC-024: "Proceed anyway" on the country warning has nothing to choose, so it goes straight
     * to the Extractions screen with the backend's IBAN and BIC.
     */
    @Test
    fun test6_proceedAnywayCountryGoesToExtractions() {
        processWithScenario(UiTestMockScenario.FRAUD_COUNTRY_MISMATCH)
        assertWarningShown(FraudWarningScreen.Type.COUNTRY)

        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse("IBAN selection must not open", ibanSelection.isSheetDisplayed())
        assertEquals(
            "iban must be the backend's IBAN",
            UiTestMockFraudData.QR_IBAN,
            extractionScreen.extractionFieldValue("iban")
        )
        assertEquals(
            "bic must be kept",
            UiTestMockFraudData.QR_BIC,
            extractionScreen.extractionFieldValue("bic")
        )
    }

    /** TC-025: "Proceed anyway" on the combined warning opens the IBAN selection sheet. */
    @Test
    fun test7_proceedAnywayCombinedOpensSelection() {
        assertProceedAnywayOpensSelection(
            UiTestMockScenario.FRAUD_COMBINED,
            FraudWarningScreen.Type.COMBINED
        )
    }

    /** TC-017: back does not close the country warning, and the flow does not move on. */
    @Test
    fun test8_backPressKeepsCountryWarning() {
        assertBackPressKeepsWarning(
            UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
            FraudWarningScreen.Type.COUNTRY
        )
    }

    /** TC-017: back does not close the combined warning, and the flow does not move on. */
    @Test
    fun test9_backPressKeepsCombinedWarning() {
        assertBackPressKeepsWarning(
            UiTestMockScenario.FRAUD_COMBINED,
            FraudWarningScreen.Type.COMBINED
        )
    }

    private fun assertBackPressKeepsWarning(
        scenario: UiTestMockScenario,
        type: FraudWarningScreen.Type
    ) {
        processWithScenario(scenario)
        assertWarningShown(type)

        fraudWarning.pressBack()

        assertTrue(
            "Fraud warning $type was closed by a back press",
            fraudWarning.staysOpenFor(STAYS_OPEN_MS)
        )
        assertTrue(
            "Fraud warning $type must still show its title after a back press",
            fraudWarning.waitForWarning(type, TITLE_TIMEOUT_MS)
        )
        assertFalse(
            "Extraction screen must not open after a back press",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    private fun assertCancelTransferEndsFlow(
        scenario: UiTestMockScenario,
        type: FraudWarningScreen.Type
    ) {
        processWithScenario(scenario)
        assertWarningShown(type)

        assertTrue("\"Cancel transfer\" not found", fraudWarning.clickCancelTransfer())

        assertTrue("Fraud warning did not close", fraudWarning.waitForWarningGone())
        idlingResource.waitForIdle()
        assertTrue("Landing page did not appear", mainScreen.assertDescriptionTitle())
        assertFalse(
            "Extraction screen must not open after \"Cancel transfer\"",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    private fun assertProceedAnywayOpensSelection(
        scenario: UiTestMockScenario,
        type: FraudWarningScreen.Type
    ) {
        processWithScenario(scenario)
        assertWarningShown(type)

        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())

        assertTrue("IBAN selection did not open", ibanSelection.waitForSheet())
        assertFalse(
            "Extraction screen must not open before an IBAN is chosen",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    private companion object {
        /** Long enough for a dismissal animation to finish, were the warning dismissed. */
        const val STAYS_OPEN_MS = 3_000L

        /** The warning is already open, so its title only needs a short wait. */
        const val TITLE_TIMEOUT_MS = 5_000L
    }
}
