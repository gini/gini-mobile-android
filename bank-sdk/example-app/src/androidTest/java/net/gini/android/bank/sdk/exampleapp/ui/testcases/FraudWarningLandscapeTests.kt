package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import net.gini.android.bank.sdk.exampleapp.ui.resources.LandscapeDarkModeRule
import net.gini.android.bank.sdk.exampleapp.ui.screens.FraudWarningScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The fraud prevention warning (PP-3970) and its IBAN selection sheet with the device turned to
 * landscape on the SDK's camera screen, in dark mode, driven by
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend]. The portrait tests run
 * in light mode; these cover the other combination.
 *
 * Xray test cases covered:
 *
 * | Xray       | What                                     | Test |
 * |------------|------------------------------------------|------|
 * | TC-007/008 | QR-vs-printed warning content            | [test1_landscapeDark_qrVsPrintedWarningContent] |
 * | TC-009/010 | country warning content                  | [test2_landscapeDark_countryWarningContent] |
 * | TC-011/012 | combined warning content                 | [test3_landscapeDark_combinedWarningContent] |
 * | TC-018/019 | the protection drop-down opens, closes   | [test4_landscapeDark_protectionRowExpandsAndCollapses] |
 * | TC-020     | "Cancel transfer" ends the flow          | [test5_landscapeDark_cancelTransfer] |
 * | TC-024     | "Proceed anyway", country: extractions   | [test6_landscapeDark_proceedAnywayCountry] |
 * | TC-026     | selection content, QR-vs-printed         | [test7_landscapeDark_selectionContentQrVsPrinted] |
 * | TC-027     | selection content, combined              | [test8_landscapeDark_selectionContentCombined] |
 * | TC-031     | printed IBAN chosen: shown, bic empty    | [test9_landscapeDark_continueWithPrintedIban] |
 *
 * ## Setup, as in [IngredientBrandLandscapeTests]
 *
 * [LandscapeDarkModeRule] switches dark mode on and starts the app in portrait, because the
 * example app's main screen hides its "Photo payment" button in landscape. The test opens the SDK
 * in portrait and turns the device on the camera screen, so the Analysis screen and the warning
 * open already in landscape and are never rotated. Phones are not forced to portrait (the example
 * app never calls `ActivityHelper.forcePortraitOrientationOnPhones`).
 *
 * The document is imported as a PDF through the SDK's file picker, the import path
 * [IngredientBrandLandscapeTests.test8_landscapeDark_pdf_showsBadgeAndGiniMark] already runs in
 * landscape. The portrait fraud tests use the photo picker instead; the mock ignores the bytes
 * either way, and the warning does not depend on the document type
 * (`AnalysisScreenPresenter`, `SUCCESS_WITH_EXTRACTIONS`).
 *
 * ## The sheets in landscape
 *
 * - The warning (`GiniBottomSheetScaffold`) is full width, starts below the status bar, and keeps
 *   out of the three-button navigation bar, which is at the side in landscape. Its content
 *   scrolls; the window is short, so most texts and both buttons are reached by scrolling, which
 *   [FraudWarningScreen] does for every lookup.
 * - The selection is a bottom sheet, not the tablet dialog: `IBANSelectionSheet` asks
 *   `isTablet()`, which reads `gc_is_tablet`, true only in `values-sw600dp`. The smallest width
 *   does not change on rotation, so a phone in landscape is still not a tablet.
 * - The protection text can open below the bottom edge of the sheet in landscape, so the drop-down
 *   test looks for it with [FraudWarningScreen.waitForProtectionBodyScrolling].
 */
class FraudWarningLandscapeTests : FraudWarningTestBase() {

    /** Wraps the activity rule, so the app is launched already in dark mode and in portrait. */
    @get:Rule(order = Int.MIN_VALUE + 1)
    val landscapeDarkMode = LandscapeDarkModeRule()

    @Before
    fun checkDarkMode() {
        // Skipped, not failed, where the device does not let a test switch dark mode, as in
        // IngredientBrandLandscapeTests.
        Assume.assumeTrue(
            "Dark mode could not be switched on for this device",
            landscapeDarkMode.isDarkModeOn()
        )
    }

    /** TC-007 / TC-008: title, description, the QR code IBAN and the printed IBAN. */
    @Test
    fun test1_landscapeDark_qrVsPrintedWarningContent() {
        openWarningInLandscape(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )

        assertEquals(
            "QR-vs-printed warning is missing texts in landscape",
            emptyList<String>(),
            fraudWarning.missingTexts(
                fraudWarning.expectedQrVsPrintedTexts(
                    qrIban = UiTestMockFraudData.QR_IBAN,
                    printedIbans = listOf(UiTestMockFraudData.PRINTED_IBAN)
                )
            )
        )
    }

    /** TC-009 / TC-010: title, description, the sender line and the document IBAN with its hint. */
    @Test
    fun test2_landscapeDark_countryWarningContent() {
        openWarningInLandscape(
            UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
            FraudWarningScreen.Type.COUNTRY
        )

        assertEquals(
            "Country warning is missing texts in landscape",
            emptyList<String>(),
            fraudWarning.missingTexts(
                fraudWarning.expectedCountryTexts(
                    senderName = UiTestMockFraudData.SENDER,
                    senderCountryCode = UiTestMockFraudData.CREDITOR_COUNTRY,
                    iban = UiTestMockFraudData.QR_IBAN
                )
            )
        )
    }

    /** TC-011 / TC-012: title, description, the sender card, both IBANs and their hints. */
    @Test
    fun test3_landscapeDark_combinedWarningContent() {
        openWarningInLandscape(
            UiTestMockScenario.FRAUD_COMBINED,
            FraudWarningScreen.Type.COMBINED
        )

        assertEquals(
            "Combined warning is missing texts in landscape",
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
    }

    /** TC-018 / TC-019: the protection text starts collapsed, opens on a tap, closes on the next. */
    @Test
    fun test4_landscapeDark_protectionRowExpandsAndCollapses() {
        openWarningInLandscape(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )
        assertFalse(
            "Protection text must be collapsed when the warning opens",
            fraudWarning.isTextShown(fraudWarning.protectionBody)
        )

        assertTrue("Protection row not found", fraudWarning.clickProtectionRow())
        assertTrue(
            "Protection text did not appear after expanding",
            fraudWarning.waitForProtectionBodyScrolling(shown = true)
        )

        assertTrue("Protection row not found", fraudWarning.clickProtectionRow())
        assertTrue(
            "Protection text did not disappear after collapsing",
            fraudWarning.waitForProtectionBodyScrolling(shown = false)
        )
    }

    /** TC-020: "Cancel transfer" is reachable in landscape and ends the flow. */
    @Test
    fun test5_landscapeDark_cancelTransfer() {
        openWarningInLandscape(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )

        assertTrue("\"Cancel transfer\" not found", fraudWarning.clickCancelTransfer())

        assertTrue("Fraud warning did not close", fraudWarning.waitForWarningGone())
        idlingResource.waitForIdle()
        assertTrue("Landing page did not appear", mainScreen.assertDescriptionTitle())
        assertFalse(
            "Extraction screen must not open after \"Cancel transfer\"",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    /**
     * TC-024: "Proceed anyway" is reachable in landscape; on the country warning it goes straight
     * to the Extractions screen with the backend's IBAN and BIC.
     */
    @Test
    fun test6_landscapeDark_proceedAnywayCountry() {
        openWarningInLandscape(
            UiTestMockScenario.FRAUD_COUNTRY_MISMATCH,
            FraudWarningScreen.Type.COUNTRY
        )

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

    /** TC-026: the QR code IBAN and the printed IBAN with their labels, no country hints. */
    @Test
    fun test7_landscapeDark_selectionContentQrVsPrinted() {
        openSelectionInLandscape(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )

        assertEquals(
            "IBAN selection is missing texts in landscape",
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
    fun test8_landscapeDark_selectionContentCombined() {
        openSelectionInLandscape(
            UiTestMockScenario.FRAUD_COMBINED,
            FraudWarningScreen.Type.COMBINED
        )

        assertEquals(
            "IBAN selection is missing texts in landscape",
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

    /**
     * TC-031: "Continue" with the printed IBAN chosen reaches the Extractions screen with that
     * IBAN; the QR code IBAN's bic is removed, which the example app shows as an empty row.
     */
    @Test
    fun test9_landscapeDark_continueWithPrintedIban() {
        openSelectionInLandscape(
            UiTestMockScenario.FRAUD_QR_MISMATCH,
            FraudWarningScreen.Type.QR_VS_PRINTED
        )

        assertTrue(
            "Printed IBAN row not found",
            ibanSelection.selectIban(UiTestMockFraudData.PRINTED_IBAN)
        )
        assertTrue("\"Continue\" not found", ibanSelection.clickContinue())

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse("IBAN selection must be closed", ibanSelection.isSheetDisplayed())
        assertFalse("Fraud warning must be closed", fraudWarning.isAnyWarningDisplayed())
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

    /**
     * Arms the mock with [scenario], opens the SDK in portrait, turns the device to landscape on
     * the camera screen, imports a PDF, and waits for the warning of [type], still in landscape.
     */
    private fun openWarningInLandscape(scenario: UiTestMockScenario, type: FraudWarningScreen.Type) {
        armMockBackend(scenario, fraudSignalsEnabled = true)
        runCatching { pdfUploader.copyPdfToDownloads(getApplicationContext(), ANY_PDF) }

        mainScreen.clickPhotoPaymentButton()
        onboardingScreen.clickSkipButtonIfPresent()
        assertTrue("Camera screen did not appear", captureScreen.checkScanTextDisplayed())
        landscapeDarkMode.rotateToLandscape()
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())

        captureScreen.clickFilesButton()
        captureScreen.clickFiles()
        pdfUploader.uploadPdfFromFiles(ANY_PDF)
        idlingResource.waitForIdle()

        assertWarningShown(type)
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
    }

    /** [openWarningInLandscape], then "Proceed anyway" and wait for the IBAN selection. */
    private fun openSelectionInLandscape(
        scenario: UiTestMockScenario,
        type: FraudWarningScreen.Type
    ) {
        openWarningInLandscape(scenario, type)
        assertTrue("\"Proceed anyway\" not found", fraudWarning.clickProceedAnyway())
        assertTrue("IBAN selection did not open", ibanSelection.waitForSheet())
        assertFalse(
            "Extraction screen must not open before an IBAN is chosen",
            extractionScreen.isExtractionScreenDisplayedNow()
        )
    }

    private companion object {
        /** The mock never reads it; the same PDF as IngredientBrandLandscapeTests. */
        const val ANY_PDF = "sample.pdf"
    }
}
