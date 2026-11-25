package net.gini.android.bank.sdk.exampleapp.ui.testcases

import net.gini.android.bank.sdk.exampleapp.ui.resources.CreditNoteFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.SkontoTexts
import net.gini.android.bank.sdk.exampleapp.ui.screens.SkontoScreen
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockFraudData
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockScenario
import net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockSkontoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The date edge cases of the Xray folder "QAlity imported test cases / Skonto + RA" (PP-3916),
 * driven by [net.gini.android.bank.sdk.exampleapp.uitestsupport.UiTestMockBackend]. The other
 * five cases of the folder run against the real API in [SkontoReturnAssistantTests].
 *
 * | Xray    | Case                                | Test |
 * |---------|-------------------------------------|------|
 * | PP-2810 | Edge case: cash only                | [test1_cashOnlySkonto] |
 * | PP-2811 | Edge case: expires today            | [test2_skontoExpiresToday] |
 * | PP-2812 | Edge case: expired                  | [test3_expiredSkonto] |
 * | PP-2813 | Edge case: cash only, expires today | [test4_cashOnlySkontoExpiringToday] |
 *
 * ## Why the mock
 *
 * "Expires today" and "cash today" need the Skonto due date to be today on every run, which no
 * committed document can print. The `SKONTO_RETURN_ASSISTANT_*` scenarios compute the due date
 * when the analysis result is built, and serve the two line items the Return Assistant needs.
 * The uploaded image only gives the photo picker something to select.
 *
 * ## Android differs from Xray in three cases
 *
 * These tests assert what Android does today (decided in specs/PP-3916-feature.md):
 * - Cash only (PP-2810) and cash today (PP-2813): the Skonto switch starts **off**
 *   (`GetSkontoDefaultSelectionStateUseCase`), so the Edit button is disabled. Xray expects both
 *   enabled.
 * - Expired (PP-2812): the switch is **off but still tappable**. Xray calls it "disabled".
 * - Cash today (PP-2813): the row shows the normal text, because `SkontoInfoBannerTextFactory`
 *   has no `PayByCashToday` branch. Xray expects the "pay in cash today" text.
 *
 * Every expected text is resolved from the SDK's `R.string` with the values the mock serves.
 */
class SkontoReturnAssistantMockBackendTests : WarningBottomSheetTestBase() {

    /**
     * PP-2810: cash-only Skonto. Android starts the switch **off** and disables Edit, where Xray
     * expects both enabled — see the class KDoc. Change these assertions if the SDK changes.
     */
    @Test
    fun test1_cashOnlySkonto() {
        assumeNotCloseToMidnight()
        openReturnAssistant(UiTestMockScenario.SKONTO_RETURN_ASSISTANT_CASH_ONLY)

        assertFalse(
            "Android starts a cash-only discount switched off",
            digitalInvoiceScreen.isSkontoRowToggleChecked()
        )
        assertEquals(
            SkontoTexts.sdkString(
                net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_info_banner_pay_cash_message,
                SkontoTexts.percent(UiTestMockFraudData.SKONTO_PERCENTAGE),
                SkontoTexts.remainingDays(UiTestMockSkontoData.REMAINING_DAYS)
            ),
            digitalInvoiceScreen.skontoRowMessageText()
        )
        assertEditButtonDisabled()
    }

    /** PP-2811: the discount expires today. Matches Xray. */
    @Test
    fun test2_skontoExpiresToday() {
        assumeNotCloseToMidnight()
        openReturnAssistant(UiTestMockScenario.SKONTO_RETURN_ASSISTANT_EXPIRES_TODAY)

        assertTrue("The Skonto switch must be on", digitalInvoiceScreen.isSkontoRowToggleChecked())
        assertEquals(
            SkontoTexts.sdkString(
                net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_info_banner_pay_today_message,
                SkontoTexts.percent(UiTestMockFraudData.SKONTO_PERCENTAGE)
            ),
            digitalInvoiceScreen.skontoRowMessageText()
        )
        assertTrue(
            "The Edit button must be enabled while the discount is on",
            digitalInvoiceScreen.isSkontoRowEditButtonEnabled()
        )
    }

    /**
     * PP-2812: the discount expired yesterday. Android shows the switch off but still tappable,
     * where Xray says "disabled" — see the class KDoc.
     */
    @Test
    fun test3_expiredSkonto() {
        assumeNotCloseToMidnight()
        openReturnAssistant(UiTestMockScenario.SKONTO_RETURN_ASSISTANT_EXPIRED)

        assertFalse(
            "An expired discount must start switched off",
            digitalInvoiceScreen.isSkontoRowToggleChecked()
        )
        assertEquals(
            SkontoTexts.sdkString(
                net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_info_banner_date_expired_message,
                SkontoTexts.percent(UiTestMockFraudData.SKONTO_PERCENTAGE)
            ),
            digitalInvoiceScreen.skontoRowMessageText()
        )
        assertEditButtonDisabled()
    }

    /**
     * PP-2813: cash only, due today. Android starts the switch off, disables Edit, and shows the
     * **normal** text, because `SkontoInfoBannerTextFactory` has no `PayByCashToday` branch. Xray
     * expects `gbs_skonto_section_discount_info_banner_pay_cash_today_message`; switch the
     * expected text to it when the SDK is fixed.
     */
    @Test
    fun test4_cashOnlySkontoExpiringToday() {
        assumeNotCloseToMidnight()
        openReturnAssistant(UiTestMockScenario.SKONTO_RETURN_ASSISTANT_CASH_TODAY)

        assertFalse(
            "Android starts a cash discount switched off",
            digitalInvoiceScreen.isSkontoRowToggleChecked()
        )
        assertEquals(
            SkontoTexts.normalMessage(0, UiTestMockFraudData.SKONTO_PERCENTAGE),
            digitalInvoiceScreen.skontoRowMessageText()
        )
        assertEditButtonDisabled()
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────

    /**
     * Arms [scenario], turns Return Assistant and Skonto on, imports an image and dismisses the
     * Return Assistant's onboarding. Fails with the screen's visible text when the RA screen does
     * not open — the Skonto screen showing instead means the mock's `lineItems` failed validation.
     */
    private fun openReturnAssistant(scenario: UiTestMockScenario) {
        armMockBackend(scenario)
        configureReturnAssistantAndSkonto(returnAssistantEnabled = true, skontoEnabled = true)

        uploadFixtureInvoiceAndProcess(ANY_IMAGE)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()

        assertTrue(
            "Expected the Return Assistant screen. " + SkontoScreen().describeVisibleText(),
            digitalInvoiceScreen.isScreenDisplayed()
        )
    }

    private fun assertEditButtonDisabled() {
        assertTrue("The Skonto row has no Edit button", digitalInvoiceScreen.isSkontoRowEditButtonDisplayed())
        assertFalse(
            "The Edit button must be disabled while the discount is off",
            digitalInvoiceScreen.isSkontoRowEditButtonEnabled()
        )
    }

    private companion object {
        /** Something for the photo picker to select; the mock never looks at the bytes. */
        const val ANY_IMAGE = CreditNoteFixtures.PLAIN_INVOICE_ASSET
    }
}
