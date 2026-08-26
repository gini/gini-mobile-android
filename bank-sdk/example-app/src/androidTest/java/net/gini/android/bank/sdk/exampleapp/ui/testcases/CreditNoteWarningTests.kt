package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.espresso.Espresso.pressBack
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI tests for the Credit Note warning state of the warning bottom sheet on the Analysis
 * screen, automated from Xray Test Set PP-3483 under PP-3427. Feature story: PP-2180.
 *
 * Built on the same approach as [DueDateHintBottomSheetTests] (see specs/PP-3301-feature.md):
 * a real document driven end to end through the Gini API, the server flag assumed enabled, and
 * only the client-side flag toggled from the test. No extraction injection anywhere — an
 * earlier attempt to rewrite the API response from an OkHttp interceptor was abandoned because
 * `configureGiniBank()` runs in SplashActivity/MainActivity `onCreate`, so GiniCapture already
 * holds the original network service before a test can swap it.
 *
 * The warning needs both gates open (`AnalysisScreenPresenter.shouldShowCreditNoteWarning`):
 *
 * | Xray case | Xray step (Charles)    | What the test does | Expected  |
 * |-----------|------------------------|--------------------|-----------|
 * | PP-3446   | Frontend ON + SDK ON   | switch ON          | shown     |
 * | PP-3447   | Frontend ON + SDK OFF  | switch OFF         | not shown |
 * | PP-3451   | Frontend OFF + SDK ON  | not reproducible   | manual    |
 * | PP-3452   | Frontend OFF + SDK OFF | not reproducible   | manual    |
 *
 * "Frontend Feature Flag" is the server `/configurations` flag `creditNoteHintEnabled`, which
 * cannot be changed from a device. The gate is `!clientFlag || !sdkFlag`, so the three negative
 * combinations all collapse onto the single branch PP-3447 already covers — no client-side
 * lever distinguishes them. That is why PP-3451 and PP-3452 stay manual, unlike PP-3301, where
 * two server flags mapped onto two independent client switches.
 *
 * Stability assumptions, as in [WarningBottomSheetTestBase]: the server flag
 * `creditNoteHintEnabled` must be enabled for the `gini-mobile-test` client. An all-red run
 * here almost always means that flag regressed, not that the sheet logic broke. Each test also
 * re-races the `/configurations` fetch against the analysis round trip, because
 * `clearPackageData` wipes the persisted flags.
 */
class CreditNoteWarningTests : WarningBottomSheetTestBase() {

    /**
     * Xray: PP-2690
     *
     * On a fresh install the SDK Feature Flag is on: the example app's Credit Note Hint switch
     * is checked without anybody touching it. Covers AC 9 of PP-2180 — the client default of
     * `creditNoteHintEnabled` is `true`. `clearPackageData` is enabled for this module, so
     * every test really does start from a fresh configuration.
     */
    @Test
    fun test1_creditNoteHintFlagIsEnabledByDefault() {
        mainScreen.clickSettingButton()

        configurationScreen.assertCreditNoteHintSwitchIsChecked()
    }

    /**
     * Xray: PP-2713 (upload path with a plain invoice; the variant carrying Return Assistant or
     * Skonto screens stays manual, as does the field-by-field "filled in correctly" check
     * beyond amountToPay)
     *
     * With the SDK Feature Flag off, an ordinary invoice goes straight to the extraction
     * screen: no warning, and `amountToPay` is still handed over.
     *
     * Note for whoever maintains this: with a *plain* invoice this case cannot fail even if the
     * feature is broken, because a plain invoice never triggers the warning whatever the flag
     * says. [test8_noWarningWhenSdkFlagIsOffForCreditNote] (PP-3447) is the test that actually
     * proves the flag works. Raised with QA under PP-3427.
     */
    @Test
    fun test2_noCreditNoteWarningForPlainInvoiceWhenSdkFlagIsOff() {
        turnCreditNoteHintSwitchOff()
        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.PLAIN_INVOICE_ASSET)

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "Credit Note warning must not show with the SDK flag off",
            warningBottomSheet.isSheetDisplayed()
        )
        assertEquals(
            "amountToPay must still be handed over when no credit note warning was shown",
            true,
            extractionScreen.isExtractionFieldFilled("amountToPay")
        )
    }

    /**
     * Xray: PP-2693
     *
     * The warning appears after analysis with its title, description and both CTAs. The CTA
     * order matters: "Cancel transfer" is the primary (filled) button and "Proceed anyway" the
     * secondary — the reverse of the due date states, so a title-only assertion would not catch
     * them being swapped.
     */
    @Test
    fun test3_creditNoteWarningIsDisplayedWithCorrectContent() {
        uploadCreditNoteAndProcess()

        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteState()
    }

    /**
     * Xray: PP-2694
     *
     * The warning is not cancelable: tapping the scrim outside the two buttons leaves it up.
     * Covers AC 7 of PP-2180.
     */
    @Test
    fun test4_tapOutsideDoesNotDismissCreditNoteWarning() {
        uploadCreditNoteAndProcess()
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.tapOutsideSheet()

        assertTrue(
            "Credit Note warning was dismissed by an outside tap",
            warningBottomSheet.isSheetDisplayed()
        )
        warningBottomSheet.assertCreditNoteState()
    }

    /**
     * Xray: PP-2695
     *
     * "Cancel transfer" (the primary CTA here) cancels the journey and returns to the screen the
     * user started from — the example app's landing page. Covers AC 5 of PP-2180.
     */
    @Test
    fun test5_cancelTransferReturnsToMainScreen() {
        uploadCreditNoteAndProcess()
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickPrimaryButton()

        idlingResource.waitForIdle()
        assertTrue("Landing page did not appear", mainScreen.assertDescriptionTitle())
    }

    /**
     * Xray: PP-2696 (the amountToPay half; the "IBAN, Recipient and Reference filled in
     * correctly" half stays manual — see below)
     *
     * "Proceed anyway" (the secondary CTA here) continues to the extraction screen, and the
     * amount is stripped on the way. Covers AC 6 of PP-2180 —
     * `AnalysisScreenPresenter.removeAmountToPay` drops `amountToPay` before handing the
     * extractions over.
     *
     * The assertion is on the value being blank, NOT on the row being absent. The example app
     * re-adds every missing SEPA field as an empty editable row (ExtractionsActivity, "Ensure
     * all expected SEPA fields exist"), so an `amountToPay` row is always rendered. Asserting
     * absence here fails even when the SDK behaves correctly — it did, on BrowserStack build
     * 07ddb295. A regression still fails this test, because the row would then carry a value.
     *
     * Why the other fields are not asserted: they depend on what this particular document
     * extracts, and its printed IBAN is only 10 characters. Once [CreditNoteFixtures] records a
     * validated extraction set, the fields it actually returns can be asserted here.
     *
     * PP-2711 is the same case in German. Following PP-3301 decision 4, texts are asserted from
     * string resources so the test is locale-independent, which means it cannot prove the German
     * copy specifically — that verification stays with the capture-sdk unit tests and manual QA.
     */
    @Test
    fun test6_proceedAnywayShowsExtractionsWithoutAmount() {
        uploadCreditNoteAndProcess()
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())

        warningBottomSheet.clickSecondaryButton()

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse("Warning must be dismissed", warningBottomSheet.isSheetDisplayed())
        assertEquals(
            "amountToPay must be empty on the credit note proceed path — the SDK strips it, " +
                "and the example app re-adds it as an editable empty row",
            false,
            extractionScreen.isExtractionFieldFilled("amountToPay")
        )
    }

    /**
     * Xray: PP-3446
     *
     * Frontend Feature Flag ON and SDK Feature Flag ON: the warning shows. Covers AC 1 of
     * PP-2180. The switch is asserted to be on rather than assumed, so this differs from PP-2693
     * by pinning the SDK gate explicitly. The Frontend gate is the server flag this class's KDoc
     * records as a precondition.
     */
    @Test
    fun test7_warningIsDisplayedWhenBothFlagsAreOn() {
        mainScreen.clickSettingButton()
        configurationScreen.assertCreditNoteHintSwitchIsChecked()
        pressBack()

        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)

        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteState()
    }

    /**
     * Xray: PP-3447
     *
     * Frontend Feature Flag ON but SDK Feature Flag OFF: no warning, and the credit note is
     * processed like an ordinary invoice — so `amountToPay` is handed over instead of stripped.
     * Covers AC 2 and AC 3 of PP-2180.
     *
     * This is the case PP-2713 cannot prove, because here the document really is a credit note.
     */
    @Test
    fun test8_noWarningWhenSdkFlagIsOffForCreditNote() {
        turnCreditNoteHintSwitchOff()
        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "Credit Note warning must not show with the SDK flag off",
            warningBottomSheet.isSheetDisplayed()
        )
        assertEquals(
            "amountToPay must be handed over when the warning was suppressed",
            true,
            extractionScreen.isExtractionFieldFilled("amountToPay")
        )
    }

    private fun uploadCreditNoteAndProcess() {
        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)
    }

    /**
     * Turns the SDK Feature Flag off through the example app's own switch, which is what the
     * Xray steps describe ("In Settings, turn the Credit Note Feature Flag OFF"). Asserting the
     * state before and after means a moved or renamed switch fails loudly here instead of
     * silently leaving the flag on and turning a negative test green.
     */
    private fun turnCreditNoteHintSwitchOff() {
        mainScreen.clickSettingButton()
        configurationScreen.assertCreditNoteHintSwitchIsChecked()
        configurationScreen.clickCreditNoteHintSwitch()
        configurationScreen.assertCreditNoteHintSwitchIsUnchecked()
        pressBack()
    }
}
