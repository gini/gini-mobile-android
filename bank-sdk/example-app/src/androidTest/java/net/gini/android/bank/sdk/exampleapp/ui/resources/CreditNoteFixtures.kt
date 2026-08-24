package net.gini.android.bank.sdk.exampleapp.ui.resources

/**
 * Fixture data for the Credit Note warning bottom sheet tests.
 *
 * Follows the same approach as [DueDateFixtures]: a real document driven end to end through
 * the Gini API, with no extraction injection anywhere in the suite (PP-3301 decision 1). The
 * warning only fires when the backend returns `businessDocType = CreditNote`, so the document
 * itself is what makes these tests meaningful.
 *
 * VALIDATION: **re-confirmation pending after anonymisation.**
 *
 * The document as originally supplied was validated on 2026-08-24 on BrowserStack (build
 * `07ddb295`, Pixel 9 / Pixel 10 Pro, Android 16, `gini-mobile-test` client) — end to end
 * rather than by a one-off API call: the Credit Note warning appeared with the correct title,
 * description and CTA order in `CreditNoteWarningTests` test3/test4/test5/test7, which is only
 * possible when the backend returns `businessDocType = CreditNote`.
 *
 * The image was then anonymised in place for publication — company, address, contact details
 * and bank replaced with fictional ones. `businessDocType`
 * is a CVIE *classification*, not a field read off the page, so an edited document is not
 * guaranteed to classify identically. The next green run of `CreditNoteWarningTests` re-
 * confirms it; until then treat the classification as assumed rather than proven. If those
 * four tests go red, suspect the anonymisation before the test logic.
 *
 * The same run also confirms the server `/configurations` flag `creditNoteHintEnabled` is
 * enabled for this client — the warning is gated on it, so it could not have shown otherwise.
 * An all-red run here almost always means that flag regressed, not that the sheet logic broke.
 *
 * To re-check either fact after a backend change, run the credit note shard —
 * `src/androidTest/scripts/bs_run_group_creditnote.sh`. If test3/test4/test5/test7 are green,
 * the document still classifies as a credit note and the server flag is still on; those tests
 * cannot pass otherwise.
 *
 * Two quirks of this document, both harmless for what these tests assert:
 * - the printed IBAN is `DE12345678` (10 characters; a German IBAN is 22), so `iban` may not
 *   extract. PP-2696 therefore asserts only what the warning path guarantees.
 * - the amounts are negative (`Gesamtbetrag -31,20 €`), which PP-2180's notes say should not
 *   happen for credit notes in production. It does not affect classification.
 *
 * NO REFRESH DEADLINE, unlike [DueDateFixtures]. Credit note detection does not depend on a
 * date, so nothing about this fixture goes stale — the document's own date (22.07.2020) being
 * in the past is harmless, and in fact keeps the due date hint from competing for the sheet.
 */
object CreditNoteFixtures {

    /** A real credit note ("Gutschrift"), expected to extract businessDocType = CreditNote. */
    const val CREDIT_NOTE_ASSET = "credit_note.png"

    /**
     * An ordinary invoice that is not a credit note, for the negative cases. Shared with
     * ExtractionScreenTests, which edits its `amountToPay` row — so the amount really is
     * extracted for this fixture.
     */
    const val PLAIN_INVOICE_ASSET = "test_image.jpeg"
}
