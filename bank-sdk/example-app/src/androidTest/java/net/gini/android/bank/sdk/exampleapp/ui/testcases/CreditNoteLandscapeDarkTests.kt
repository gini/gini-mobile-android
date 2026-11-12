package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.LandscapeDarkModeRule
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The Credit Note warning in dark mode and landscape (PP-3877), against the real backend like
 * [CreditNoteWarningTests], whose tests run in light mode and portrait.
 *
 * One test for the other combination, not a full light/dark × portrait/landscape matrix: the
 * sheet has its own landscape layout, and "Proceed anyway" is the path that hands a result on.
 *
 * The document is picked in portrait and the device is turned on the review screen
 * ([LandscapeDarkModeRule]): the system photo picker stays in portrait, and the Analysis screen
 * opens already in landscape and is never rotated.
 *
 * Preconditions, the same two as [CreditNoteWarningTests]: the server `/configurations` flag
 * `creditNoteHintEnabled` is on for the `gini-mobile-test` client, and the backend classifies
 * `credit_note.png` as `businessDocType = CreditNote`.
 */
class CreditNoteLandscapeDarkTests : WarningBottomSheetTestBase() {

    /** Wraps the activity rule, so the app is launched already in dark mode. */
    @get:Rule(order = Int.MIN_VALUE + 1)
    val landscapeDarkMode = LandscapeDarkModeRule()

    @Before
    fun checkDarkMode() {
        // Skipped, not failed, where the device does not let a test switch dark mode — the same
        // check as IngredientBrandLandscapeTests.
        Assume.assumeTrue(
            "Dark mode could not be switched on for this device",
            landscapeDarkMode.isDarkModeOn()
        )
    }

    /**
     * Xray: PP-2696 (dark, landscape; light + portrait is `CreditNoteWarningTests.test6`).
     *
     * The warning shows its credit note state in landscape, and "Proceed anyway" continues to
     * the extraction screen without an amount.
     */
    @Test
    fun test1_landscapeDark_proceedAnywayShowsExtractionsWithoutAmount() {
        uploadFixtureInvoiceToReview(CreditNoteFixtures.CREDIT_NOTE_ASSET)
        landscapeDarkMode.rotateToLandscape()
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
        reviewScreen.clickProcessButton()

        assertTrue("Credit Note warning did not appear", warningBottomSheet.waitForSheet())
        assertTrue("Device is not in landscape", landscapeDarkMode.isLandscape())
        warningBottomSheet.assertCreditNoteState()

        warningBottomSheet.clickSecondaryButton()

        assertProceedResult()
    }
}
