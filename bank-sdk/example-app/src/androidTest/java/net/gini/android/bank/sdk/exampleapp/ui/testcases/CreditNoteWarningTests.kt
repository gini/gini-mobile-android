package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.espresso.Espresso.pressBack
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI tests for the Credit Note warning state of the warning bottom sheet on the Analysis
 * screen, automated from the Credit Note warning Xray Test Set. Feature story: PP-2180.
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
 * | Xray step (Charles)    | What the test does      | Expected  |
 * |------------------------|-------------------------|-----------|
 * | Frontend ON + SDK ON   | client default (on)     | shown     |
 * | Frontend ON + SDK OFF  | configureCreditNoteHint | not shown |
 * | Frontend OFF + SDK ON  | not reproducible        | manual    |
 * | Frontend OFF + SDK OFF | not reproducible        | manual    |
 *
 * The SDK flag is set through `CreditNoteHintConfigurator`, not the Settings switch — see
 * that class for why the switch must not be tapped. The switch itself stays covered by
 * [test1_creditNoteHintFlagIsEnabledByDefault], which only reads it.
 *
 * "Frontend Feature Flag" is the server `/configurations` flag `creditNoteHintEnabled`, which
 * cannot be changed from a device. The gate is `!clientFlag || !sdkFlag`, so the three negative
 * combinations all collapse onto the single branch [test8_noWarningWhenSdkFlagIsOffForCreditNote]
 * already covers — no client-side lever distinguishes them. That is why the two Frontend OFF
 * rows stay manual, unlike PP-3301, where two server flags mapped onto two independent client
 * switches.
 *
 * Stability assumptions, as in [WarningBottomSheetTestBase]: the server flag
 * `creditNoteHintEnabled` must be enabled for the `gini-mobile-test` client. An all-red run
 * here almost always means that flag regressed, not that the sheet logic broke. Each test also
 * re-races the `/configurations` fetch against the analysis round trip, because
 * `clearPackageData` wipes the persisted flags.
 */
class CreditNoteWarningTests : WarningBottomSheetTestBase() {

    /**
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
     * The upload path with a plain invoice. The variant carrying Return Assistant or Skonto
     * screens stays manual, as does the field-by-field "filled in correctly" check beyond
     * amountToPay.
     *
     * With the SDK Feature Flag off, an ordinary invoice goes straight to the extraction
     * screen: no warning, and `amountToPay` is still handed over.
     *
     * Note for whoever maintains this: with a *plain* invoice this case cannot fail even if the
     * feature is broken, because a plain invoice never triggers the warning whatever the flag
     * says. [test8_noWarningWhenSdkFlagIsOffForCreditNote] is the test that actually proves the
     * flag works. Raised with QA.
     */
    @Test
    fun test2_noCreditNoteWarningForPlainInvoiceWhenSdkFlagIsOff() {
        configureCreditNoteHint(enabled = false)
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
     * Covers the amountToPay half only; the "IBAN, Recipient and Reference filled in
     * correctly" half stays manual — see below.
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
     * The same case in German is tracked separately. Following PP-3301 decision 4, texts are
     * asserted from string resources so the test is locale-independent, which means it cannot
     * prove the German copy specifically — that verification stays with the capture-sdk unit
     * tests and manual QA.
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
     * Frontend Feature Flag ON and SDK Feature Flag ON: the warning shows. Covers AC 1 of
     * PP-2180. The switch is asserted to be on rather than assumed, so this differs from
     * [test3_creditNoteWarningIsDisplayedWithCorrectContent] by pinning the SDK gate explicitly.
     * The Frontend gate is the server flag this class's KDoc records as a precondition.
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
     * Frontend Feature Flag ON but SDK Feature Flag OFF: no warning, and the credit note is
     * processed like an ordinary invoice — so `amountToPay` is handed over instead of stripped.
     * Covers AC 2 and AC 3 of PP-2180.
     *
     * This is the case [test2_noCreditNoteWarningForPlainInvoiceWhenSdkFlagIsOff] cannot prove,
     * because here the document really is a credit note.
     */
    @Test
    fun test8_noWarningWhenSdkFlagIsOffForCreditNote() {
        configureCreditNoteHint(enabled = false)
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

    /**
     * Return Assistant on, credit note in: the warning still wins, and "Proceed anyway" lands on
     * the extraction screen rather than the Digital Invoice screen.
     *
     * Two SDK behaviours make this so, both worth locking down:
     * - The credit note branch sits above the routing. `shouldShowCreditNoteWarning` is checked on
     *   the Analysis screen (`AnalysisScreenPresenter`, in the SUCCESS_WITH_EXTRACTIONS chain),
     *   which runs before `CaptureFlowFragment.processOnFinishedResultSuccessState` gets to decide
     *   between Return Assistant, Skonto and plain extractions.
     * - The proceed path cannot reach the Digital Invoice screen even if it tried.
     *   `AnalysisScreenPresenter.removeAmountToPay` rebuilds the ResultHolder with an empty
     *   compound-extraction map, so `LineItemsValidator.validate` finds no `lineItems` and
     *   `tryShowingReturnAssistant` throws `DigitalInvoiceException`.
     *
     * SCOPE, and why the name is narrow: with a real document this can only prove the first half.
     * [CreditNoteFixtures.CREDIT_NOTE_ASSET] is not known to extract `lineItems`, so the absence of
     * the Digital Invoice screen would hold even if the stripping regressed. The stripping itself is
     * covered by `CreditNoteMockBackendTests.test5_returnAssistantIsSkippedForCreditNoteWithLineItems`,
     * where the mock supplies line items that really would open the screen. What this test adds over
     * that one is the real backend and a real document.
     */
    @Test
    fun test9_warningIsNotSkippedWhenReturnAssistantIsEnabled() {
        configureReturnAssistantAndSkonto(returnAssistantEnabled = true, skontoEnabled = false)
        uploadCreditNoteAndProcess()
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteState()

        warningBottomSheet.clickSecondaryButton()

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "Digital Invoice screen must not appear on the credit note proceed path",
            digitalInvoiceScreen.isScreenDisplayed()
        )
    }

    /**
     * The control case with a REAL document: a Return Assistant invoice, with the credit note
     * feature fully on, must start the digital invoice flow and show no warning.
     *
     * This is the counterpart of `CreditNoteMockBackendTests.test5`, from the other direction.
     * test5 proves a credit note carrying line items does NOT reach the Digital Invoice screen;
     * this proves an ordinary invoice carrying line items still DOES, so test5's result is the
     * credit note gate working rather than the Return Assistant flow being broken outright. Neither
     * test means much without the other.
     *
     * Built on `DueDateHintBottomSheetTests.test10_noSheetForReturnAssistantInvoice`, which
     * establishes that [CreditNoteFixtures.RETURN_ASSISTANT_ASSET] really extracts line items — it
     * could not reach the digital invoice onboarding otherwise.
     *
     * iOS's equivalent (`testInvoiceFlowUnaffectedWhenHintFlagDisabled`) uses a skonto invoice. We
     * have no skonto document anywhere in `assets/`, so Return Assistant is the control here. Skonto
     * is deliberately not covered: it goes through the same compound-extraction stripping, so one
     * side is enough, and this is the side with a real document.
     */
    @Test
    fun test10_returnAssistantInvoiceIsUnaffectedByTheCreditNoteFeature() {
        configureCreditNoteHint(enabled = true)
        configureReturnAssistantAndSkonto(returnAssistantEnabled = true, skontoEnabled = true)

        uploadFixturePdfAndProcess(CreditNoteFixtures.RETURN_ASSISTANT_ASSET)

        assertTrue(
            "Digital invoice onboarding did not appear for a Return Assistant invoice",
            digitalInvoiceScreen.checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed()
        )
        assertFalse(
            "Credit Note warning must not appear for an invoice that is not a credit note",
            warningBottomSheet.isSheetDisplayed()
        )
    }

    private fun uploadCreditNoteAndProcess() {
        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)
    }
}
