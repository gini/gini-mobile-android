package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.AppLocale
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppLocaleRule
import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.FontScale
import net.gini.android.bank.sdk.exampleapp.ui.resources.FontScaleRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The Credit Note warning in German and at 200 % font size (PP-3877), against the real backend
 * like [CreditNoteWarningTests].
 *
 * The app's language and the device font size are set by [AppLocaleRule] and [FontScaleRule]
 * before `MainActivity` starts, and put back afterwards. The device language is not changed.
 *
 * What the 200 % tests prove is structural: no button text is cut off, the buttons are on screen
 * and nothing overlaps (`WarningBottomSheetScreen.assertSheetIsNotCutOff`). Whether the sheet
 * *looks* right at 200 % stays a manual check, and so does the extraction screen, which is
 * example-app UI.
 *
 * Preconditions, the same two as [CreditNoteWarningTests]: the server `/configurations` flag
 * `creditNoteHintEnabled` is on for the `gini-mobile-test` client, and the backend classifies
 * `credit_note.png` as `businessDocType = CreditNote`. If these tests are red, look at
 * `CreditNoteWarningTests.test3` first — if it is red too, the cause is the backend.
 *
 * Skips, not failures: every test here below API 33 (no per-app language — the English 200 %
 * test pins its language too), and the 200 % tests where the device does not let a test change
 * the font size.
 */
class CreditNoteLocaleFontScaleTests : WarningBottomSheetTestBase() {

    /** Wraps the activity rule, so the app is launched already in the test's language. */
    @get:Rule(order = Int.MIN_VALUE + 1)
    val appLocaleRule = AppLocaleRule()

    /** Wraps the activity rule, so the app is launched already at the test's font size. */
    @get:Rule(order = Int.MIN_VALUE + 1)
    val fontScaleRule = FontScaleRule()

    /**
     * Xray: PP-2711.
     *
     * The warning in German — "Überweisung abbrechen" and "Trotzdem fortfahren" — and "Trotzdem
     * fortfahren" continues to the extraction screen without an amount. The German counterpart of
     * `CreditNoteWarningTests.test6`, which cannot prove the German copy because it asserts
     * whatever language the app runs in.
     */
    @Test
    @AppLocale(GERMAN)
    fun test1_german_proceedAnywayShowsExtractionsWithoutAmount() {
        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet.assertCreditNoteStateIn(GERMAN)

        warningBottomSheet.clickSecondaryButton()

        assertProceedResult()
    }

    /** Xray: PP-2729. The English warning at 200 % font size; see the class KDoc for the scope. */
    @Test
    @FontScale(LARGEST_FONT_SCALE)
    @AppLocale(ENGLISH)
    fun test2_fontScale200_english_sheetIsNotCutOffAndProceedWorks() {
        assertSheetIsNotCutOffAndProceedWorks(ENGLISH)
    }

    /**
     * Xray: PP-2730. The German warning at 200 % font size; see the class KDoc for the scope. The
     * Xray case names the English labels; the German ones are what the SDK shows in German.
     */
    @Test
    @FontScale(LARGEST_FONT_SCALE)
    @AppLocale(GERMAN)
    fun test3_fontScale200_german_sheetIsNotCutOffAndProceedWorks() {
        assertSheetIsNotCutOffAndProceedWorks(GERMAN)
    }

    private fun assertSheetIsNotCutOffAndProceedWorks(localeTag: String) {
        uploadFixtureInvoiceAndProcess(CreditNoteFixtures.CREDIT_NOTE_ASSET)
        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        warningBottomSheet
            .assertCreditNoteStateIn(localeTag)
            .assertSheetIsNotCutOff(expectedFontScale = LARGEST_FONT_SCALE)

        warningBottomSheet.clickSecondaryButton()

        assertProceedResult()
    }

    private companion object {
        /** The two languages of the example app, `res/xml/locale_configs.xml`. */
        const val GERMAN = "de-DE"
        const val ENGLISH = "en-GB"

        /** 200 %, the largest font size of the accessibility settings. */
        const val LARGEST_FONT_SCALE = 2.0f
    }
}
