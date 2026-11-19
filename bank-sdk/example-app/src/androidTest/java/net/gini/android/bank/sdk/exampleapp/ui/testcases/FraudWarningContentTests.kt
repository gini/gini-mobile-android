package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the fraud prevention warning shows (PP-3970), driven by
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * Xray test cases covered:
 *
 * | Xray          | What                                               | Test |
 * |---------------|----------------------------------------------------|------|
 * | TC-007/008    | QR-vs-printed warning content                      | [test1_qrVsPrintedWarningContent] |
 * | TC-009/010    | country warning content                            | [test2_countryWarningContent] |
 * | TC-011/012    | combined warning content                           | [test3_combinedWarningContent] |
 * | TC-032        | several printed IBANs                              | [test4_severalPrintedIbansAreListed] |
 * | TC-033        | QR signal without another printed IBAN: no warning | [test5_noWarningForQrSignalWithoutOtherPrintedIban] |
 * | TC-018/019    | "How do I protect myself?" opens/closes, QR        | [test6_protectionRowExpandsAndCollapses] |
 * | TC-018/019    | the same, country                                  | [test7_protectionRowExpandsAndCollapsesCountry] |
 * | TC-018/019    | the same, combined                                 | [test8_protectionRowExpandsAndCollapsesCombined] |
 *
 * Every text is resolved from capture-sdk's resources and formatted the way the SDK formats it
 * (IBANs in blocks of four, country names in the language of the SDK's strings), so the tests
 * pass in English and in German. Each content test also checks that the protection text starts
 * collapsed, and the QR-vs-printed one that no sender and no country hint show.
 */
class FraudWarningContentTests : FraudWarningTestBase() {

    /** TC-007 / TC-008: title, description, the QR code IBAN and the printed IBAN. */
    @Test
    fun test1_qrVsPrintedWarningContent() {
        processWithScenario(UiTestMockScenario.FRAUD_QR_MISMATCH)
        assertWarningShown(FraudWarningScreen.Type.QR_VS_PRINTED)

        assertEquals(
            "QR-vs-printed warning is missing texts",
            emptyList<String>(),
            fraudWarning.missingTexts(
                fraudWarning.expectedQrVsPrintedTexts(
                    qrIban = UiTestMockFraudData.QR_IBAN,
                    printedIbans = listOf(UiTestMockFraudData.PRINTED_IBAN)
                )
            )
        )
        assertEquals(
            "QR-vs-printed warning must show no sender and no country hint",
            emptyList<String>(),
            fraudWarning.presentTexts(
                fraudWarning.senderAndHintTexts(
                    ibans = listOf(UiTestMockFraudData.QR_IBAN, UiTestMockFraudData.PRINTED_IBAN),
                    senderCountryCode = UiTestMockFraudData.CREDITOR_COUNTRY
                )
            )
        )
        assertProtectionCollapsed()
    }

    /**
     * TC-009 / TC-010: title, description, "Sender: <name> · <country>" and the document IBAN
     * with "From Slovakia, not the sender's country".
     */
    @Test
    fun test2_countryWarningContent() {
        processWithScenario(UiTestMockScenario.FRAUD_COUNTRY_MISMATCH)
        assertWarningShown(FraudWarningScreen.Type.COUNTRY)

        assertEquals(
            "Country warning is missing texts",
            emptyList<String>(),
            fraudWarning.missingTexts(
                fraudWarning.expectedCountryTexts(
                    senderName = UiTestMockFraudData.SENDER,
                    senderCountryCode = UiTestMockFraudData.CREDITOR_COUNTRY,
                    iban = UiTestMockFraudData.QR_IBAN
                )
            )
        )
        assertProtectionCollapsed()
    }

    /**
     * TC-011 / TC-012: title, description, the sender card, and both IBANs with their country
     * hints (Slovakia for the QR code IBAN, Spain for the printed one).
     */
    @Test
    fun test3_combinedWarningContent() {
        processWithScenario(UiTestMockScenario.FRAUD_COMBINED)
        assertWarningShown(FraudWarningScreen.Type.COMBINED)

        assertEquals(
            "Combined warning is missing texts",
            emptyList<String>(),
            fraudWarning.missingTexts(
                fraudWarning.expectedCombinedTexts(
                    senderName = UiTestMockFraudData.SENDER,
                    senderCountryCode = UiTestMockFraudData.CREDITOR_COUNTRY,
                    qrIban = UiTestMockFraudData.QR_IBAN,
                    printedIbans = listOf(UiTestMockFraudData.PRINTED_IBAN)
                )
            )
        )
        assertProtectionCollapsed()
    }

    /** TC-032: both printed IBANs are listed under the QR code IBAN. */
    @Test
    fun test4_severalPrintedIbansAreListed() {
        processWithScenario(UiTestMockScenario.FRAUD_SEVERAL_PRINTED)
        assertWarningShown(FraudWarningScreen.Type.QR_VS_PRINTED)

        assertEquals(
            "Warning does not list every printed IBAN",
            emptyList<String>(),
            fraudWarning.missingTexts(
                fraudWarning.expectedQrVsPrintedTexts(
                    qrIban = UiTestMockFraudData.QR_IBAN,
                    printedIbans = listOf(
                        UiTestMockFraudData.PRINTED_IBAN,
                        UiTestMockFraudData.SECOND_PRINTED_IBAN
                    )
                )
            )
        )
    }

    /**
     * TC-033: the QR signal arrives, but the document has no IBAN besides the QR code one, so
     * there is nothing to compare and the SDK shows no warning.
     */
    @Test
    fun test5_noWarningForQrSignalWithoutOtherPrintedIban() {
        processWithScenario(UiTestMockScenario.FRAUD_QR_WITHOUT_OTHER_PRINTED)

        assertNoWarningAndExtractionsShown()
    }

    /**
     * TC-018 / TC-019, QR-vs-printed: tapping the row shows the protection text, tapping again
     * hides it.
     */
    @Test
    fun test6_protectionRowExpandsAndCollapses() {
        assertProtectionRowExpandsAndCollapses(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )
    }

    /** TC-018 / TC-019, country: the same drop-down on the country warning. */
    @Test
    fun test7_protectionRowExpandsAndCollapsesCountry() {
        assertProtectionRowExpandsAndCollapses(
            UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
            FraudWarningScreen.Type.COUNTRY
        )
    }

    /** TC-018 / TC-019, combined: the same drop-down on the combined warning. */
    @Test
    fun test8_protectionRowExpandsAndCollapsesCombined() {
        assertProtectionRowExpandsAndCollapses(
            UiTestMockScenario.FRAUD_COMBINED,
            FraudWarningScreen.Type.COMBINED
        )
    }

    private fun assertProtectionRowExpandsAndCollapses(
        scenario: UiTestMockScenario,
        type: FraudWarningScreen.Type
    ) {
        processWithScenario(scenario)
        assertWarningShown(type)

        assertTrue("Protection row not found on $type", fraudWarning.clickProtectionRow())
        assertTrue(
            "Protection text did not appear after expanding, $type",
            fraudWarning.waitForProtectionBody(shown = true)
        )

        assertTrue("Protection row not found on $type", fraudWarning.clickProtectionRow())
        assertTrue(
            "Protection text did not disappear after collapsing, $type",
            fraudWarning.waitForProtectionBody(shown = false)
        )
    }

    private fun assertProtectionCollapsed() {
        assertFalse(
            "Protection text must be collapsed when the warning opens",
            fraudWarning.isTextShown(fraudWarning.protectionBody)
        )
    }
}
