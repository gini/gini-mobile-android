package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Credit Note warning cases that the real backend cannot give us, driven by
 * [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend].
 *
 * Ported from the iOS suite (gini-mobile-ios PR #1250), which splits the same cases across
 * `GiniCreditNoteMockBackendFlagOnUITests` and `...FlagOffUITests`. iOS needs two classes because
 * it configures the mock with per-class launch arguments; here the mock is armed from the test
 * body, so one class covers the whole matrix.
 *
 * ## Why these are not in [CreditNoteWarningTests]
 *
 * That suite is deliberately real-backend end to end: a genuine document, the real Gini API, only
 * the client flag touched. It cannot cover two kinds of case:
 *
 * 1. **The frontend flag off.** `creditNoteHintEnabled` is a server `/configurations` flag. No
 *    device-side lever exists, which is why the two "Frontend OFF" rows of the matrix in
 *    [CreditNoteWarningTests] are marked *not reproducible — manual*. Here they are ordinary tests.
 * 2. **Extraction shapes we own no document for.** A credit note that also carries `lineItems` —
 *    the case that proves `AnalysisScreenPresenter.removeAmountToPay` wipes the compound
 *    extractions, not just the amount. `Testrechnung-RA-1.pdf` has line items but is not a credit
 *    note, `credit_note.png` is a credit note without them, and the classification comes from the
 *    backend, so no edit to an image can produce one document that is both.
 *
 * Only Return Assistant is covered, not Skonto. Both go through the same one map being emptied, so
 * proving it once is enough — and Return Assistant is the side with a real document
 * (`CreditNoteWarningTests.test10`) to act as the control.
 *
 * ## The gate under test
 *
 * `AnalysisScreenPresenter.shouldShowCreditNoteWarning` needs BOTH flags:
 *
 * | Frontend (server) | SDK (client) | Expected  | Test    |
 * |-------------------|--------------|-----------|---------|
 * | ON                | ON           | shown     | [test1_warningIsShownWhenBothFlagsAreOn] |
 * | ON                | OFF          | not shown | [test2_noWarningWhenFrontendOnAndSdkOff] |
 * | OFF               | ON           | not shown | [test3_noWarningWhenFrontendOffAndSdkOn] |
 * | OFF               | OFF          | not shown | [test4_noWarningWhenFrontendOffAndSdkOff] |
 *
 * ## The document does not matter here
 *
 * The mock ignores the uploaded bytes and answers from its scenario, so [ANY_DOCUMENT] is just
 * something for the picker to select. A plain invoice is used on purpose: picking `credit_note.png`
 * would suggest the document drives the outcome, and it does not. What makes a run a credit note is
 * [UiTestMockScenario], nothing else.
 *
 * These tests need no network and no server flag, so unlike [CreditNoteWarningTests] they cannot go
 * red because the backend changed.
 */
class CreditNoteMockBackendTests : WarningBottomSheetTestBase() {

    /** Frontend ON + SDK ON — the warning is displayed with its full content. */
    @Test
    fun test1_warningIsShownWhenBothFlagsAreOn() {
        armMockBackend(UiTestMockScenario.CREDIT_NOTE, creditNoteHintEnabled = true)
        configureCreditNoteHint(enabled = true)

        uploadFixtureInvoiceAndProcess(ANY_DOCUMENT)

        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteState()
    }

    /** Frontend ON + SDK OFF — no warning; the document is processed like an ordinary invoice. */
    @Test
    fun test2_noWarningWhenFrontendOnAndSdkOff() {
        armMockBackend(UiTestMockScenario.CREDIT_NOTE, creditNoteHintEnabled = true)
        configureCreditNoteHint(enabled = false)

        uploadFixtureInvoiceAndProcess(ANY_DOCUMENT)

        assertProcessedLikeRegularInvoice()
    }

    /**
     * Frontend OFF + SDK ON — no warning. Previously manual: there is no way to turn the server
     * flag off from a device.
     */
    @Test
    fun test3_noWarningWhenFrontendOffAndSdkOn() {
        armMockBackend(UiTestMockScenario.CREDIT_NOTE, creditNoteHintEnabled = false)
        configureCreditNoteHint(enabled = true)

        uploadFixtureInvoiceAndProcess(ANY_DOCUMENT)

        assertProcessedLikeRegularInvoice()
    }

    /** Frontend OFF + SDK OFF — no warning. Previously manual, for the same reason as test3. */
    @Test
    fun test4_noWarningWhenFrontendOffAndSdkOff() {
        armMockBackend(UiTestMockScenario.CREDIT_NOTE, creditNoteHintEnabled = false)
        configureCreditNoteHint(enabled = false)

        uploadFixtureInvoiceAndProcess(ANY_DOCUMENT)

        assertProcessedLikeRegularInvoice()
    }

    /**
     * A credit note that also carries `lineItems`, with the Return Assistant enabled on both sides.
     *
     * This is the case no fixture can express. Two SDK behaviours have to hold:
     * - the credit note warning wins over the routing — `shouldShowCreditNoteWarning` is checked on
     *   the Analysis screen, before `CaptureFlowFragment.processOnFinishedResultSuccessState`
     *   decides between Return Assistant, Skonto and plain extractions;
     * - proceeding cannot reach the Digital Invoice screen, because `removeAmountToPay` rebuilds the
     *   ResultHolder with an empty compound-extraction map, so `LineItemsValidator.validate` finds
     *   no `lineItems` and `tryShowingReturnAssistant` throws `DigitalInvoiceException`.
     *
     * Unlike `CreditNoteWarningTests.test9`, this one really fails if either regresses: the mock
     * supplies line items that would otherwise open the Digital Invoice screen. Its control is
     * `CreditNoteWarningTests.test10`, where a real invoice with real line items does reach that
     * screen — without it, a green result here could just mean the Return Assistant flow is broken.
     */
    @Test
    fun test5_returnAssistantIsSkippedForCreditNoteWithLineItems() {
        armMockBackend(
            UiTestMockScenario.CREDIT_NOTE_WITH_LINE_ITEMS,
            creditNoteHintEnabled = true,
            returnAssistantEnabled = true,
            skontoEnabled = false
        )
        configureCreditNoteHint(enabled = true)
        configureReturnAssistantAndSkonto(returnAssistantEnabled = true, skontoEnabled = false)

        uploadFixtureInvoiceAndProcess(ANY_DOCUMENT)

        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.clickSecondaryButton()

        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "Digital Invoice screen must not appear for a credit note, even with line items",
            digitalInvoiceScreen.isScreenDisplayed()
        )
    }

    /**
     * The second import path. iOS covers the credit note twice — `...DisplayedViaFiles` and
     * `...DisplayedViaGallery`; every other test here (and all of [CreditNoteWarningTests]) goes
     * through the photo picker, so this is the one that exercises a PDF arriving via the SDK's file
     * picker.
     *
     * It lives with the mock tests rather than the real-backend suite because we have no credit
     * note PDF — `credit_note.png` is an image. The mock does not care what the document is, so the
     * import path can be tested without one.
     */
    @Test
    fun test6_warningIsShownForAPdfImportedViaFiles() {
        armMockBackend(UiTestMockScenario.CREDIT_NOTE, creditNoteHintEnabled = true)
        configureCreditNoteHint(enabled = true)

        uploadFixturePdfAndProcess(ANY_PDF)

        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteState()
    }

    /**
     * iOS's `assertProcessedLikeRegularInvoice`: no warning, and `amountToPay` is handed over
     * instead of stripped. The amount is the half that distinguishes "the warning was never due"
     * from "the warning was shown and the user proceeded", which strips it.
     */
    private fun assertProcessedLikeRegularInvoice() {
        assertTrue(
            "Extraction screen did not appear",
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "Credit Note warning must not appear",
            warningBottomSheet.isSheetDisplayed()
        )
        assertTrue(
            "amountToPay must be handed over when no credit note warning was shown",
            extractionScreen.isExtractionFieldFilled("amountToPay")
        )
    }

    private companion object {
        /**
         * Something for the photo picker to select. The mock answers from its scenario and never
         * looks at the bytes, so which document this is has no effect on any assertion here.
         */
        const val ANY_DOCUMENT = CreditNoteFixtures.PLAIN_INVOICE_ASSET

        /** Likewise for the file-picker path — BrowserStack pre-loads this one as media. */
        const val ANY_PDF = "sample.pdf"
    }
}
