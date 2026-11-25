package net.gini.android.bank.sdk.exampleapp.ui.testcases

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.gini.android.bank.sdk.exampleapp.ui.resources.AmountText
import net.gini.android.bank.sdk.exampleapp.ui.resources.PaymentHintConfigurator
import net.gini.android.bank.sdk.exampleapp.ui.resources.SkontoFixtures
import net.gini.android.bank.sdk.exampleapp.ui.resources.SkontoTexts
import net.gini.android.bank.sdk.exampleapp.ui.screens.DigitalInvoiceScreen
import net.gini.android.bank.sdk.exampleapp.ui.screens.ReturnAssistantSkontoScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * The Return Assistant (digital invoice) screen for an invoice that also carries a Skonto
 * discount, against the **real** Gini API — the Xray folder "QAlity imported test cases /
 * Skonto + RA" (PP-3916).
 *
 * | Xray    | Case                              | Test |
 * |---------|-----------------------------------|------|
 * | PP-2807 | RA screen, active Skonto          | [test1_activeSkontoRowOnReturnAssistantScreen] |
 * | PP-2808 | RA screen, Skonto switched off    | [test2_disabledSkontoRowAfterSwitchingOff] |
 * | PP-2809 | Skonto screen opened from RA      | [test3_skontoScreenOpenedFromReturnAssistant] |
 * | PP-2814 | Config: Skonto off                | [test4_skontoOffHidesSkontoOnReturnAssistantScreen] |
 * | PP-2815 | Config: Skonto and RA off         | [test5_skontoAndReturnAssistantOffOpensExtractionScreen] |
 *
 * The date edge cases (PP-2810 – PP-2813) need a due date relative to today, which no committed
 * document can hold, so they run against the mock in [SkontoReturnAssistantMockBackendTests].
 *
 * ## Fixture
 *
 * [SkontoFixtures.RA_VALID_2028_ASSET]: two line items and 3 % Skonto within 14 days, dated
 * 2028 so the discount stays claimable. Its amounts are in [SkontoFixtures]. The remaining days
 * come from the backend and shrink every day, so the info text is matched with the percentage
 * fixed and any number of days.
 */
class SkontoReturnAssistantTests : SmokeJourneyTestBase() {

    /** Real upload and analysis, then a full navigation. */
    override val idlingTimeoutMs: Long = 15_000

    private val digitalInvoiceScreen = DigitalInvoiceScreen()
    private val returnAssistantSkontoScreen = ReturnAssistantSkontoScreen()
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** PP-2807: the Skonto row and the footer while the discount is active. */
    @Test
    fun test1_activeSkontoRowOnReturnAssistantScreen() {
        openReturnAssistant()

        assertTrue("The Skonto switch must be on", digitalInvoiceScreen.isSkontoRowToggleChecked())

        val rowAmount = digitalInvoiceScreen.skontoRowAmountText()
            ?: error("The Skonto row shows no saved amount while the discount is on.")
        assertTrue("The row's saved amount must be negative: $rowAmount", rowAmount.startsWith("-"))
        assertEquals(0, AmountText.parse(rowAmount).compareTo(savedAmount()))
        assertTrue(
            "The Edit button must be enabled while the discount is on",
            digitalInvoiceScreen.isSkontoRowEditButtonEnabled()
        )

        val footerSaved = digitalInvoiceScreen.footerSkontoSavedAmountText()
            ?: error("The footer shows no saved amount while the discount is on.")
        assertTrue(
            "Unexpected footer saved-amount text: $footerSaved",
            footerSaved.contains(
                SkontoTexts.templateWithoutArgument(
                    net.gini.android.bank.sdk.R.string.gbs_skonto_section_footer_label_save
                )
            )
        )
        assertEquals(0, AmountText.parse(footerSaved).compareTo(savedAmount()))
        assertEquals(
            SkontoTexts.sdkString(
                net.gini.android.bank.sdk.R.string.gbs_skonto_section_footer_label_discount,
                SkontoTexts.percent(SkontoFixtures.RA_VALID_2028_SKONTO_PERCENTAGE)
            ),
            digitalInvoiceScreen.footerSkontoDiscountLabelText()
        )
    }

    /** PP-2808: switching the discount off on the RA screen. */
    @Test
    fun test2_disabledSkontoRowAfterSwitchingOff() {
        openReturnAssistant()

        digitalInvoiceScreen.clickSkontoRowSwitch()

        assertFalse("The Skonto switch must be off", digitalInvoiceScreen.isSkontoRowToggleChecked())
        assertNull(
            "The saved amount must be hidden while the discount is off",
            digitalInvoiceScreen.skontoRowAmountText()
        )
        assertNormalMessage(digitalInvoiceScreen.skontoRowMessageText())
        assertTrue("The Skonto row has no Edit button", digitalInvoiceScreen.isSkontoRowEditButtonDisplayed())
        assertFalse(
            "The Edit button must be disabled while the discount is off",
            digitalInvoiceScreen.isSkontoRowEditButtonEnabled()
        )
        assertNull(
            "The footer's saved amount must be hidden while the discount is off",
            digitalInvoiceScreen.footerSkontoSavedAmountText()
        )
        assertNull(
            "The footer's discount label must be hidden while the discount is off",
            digitalInvoiceScreen.footerSkontoDiscountLabelText()
        )
    }

    /** PP-2809: the Skonto screen opened with the Skonto row's Edit button. */
    @Test
    fun test3_skontoScreenOpenedFromReturnAssistant() {
        openReturnAssistant()

        digitalInvoiceScreen.clickSkontoRowEditButton()

        assertTrue(
            "The Skonto screen of the Return Assistant did not open. " +
                skontoScreen.describeVisibleText(),
            returnAssistantSkontoScreen.isDisplayed()
        )
        assertTrue("No back button", returnAssistantSkontoScreen.hasBackButton())
        assertTrue("No title", returnAssistantSkontoScreen.hasTitle())
        assertTrue("No help button", returnAssistantSkontoScreen.hasHelpButton())
        assertTrue("No invoice preview row", returnAssistantSkontoScreen.hasInvoicePreviewRow())
        assertTrue("No active hint", returnAssistantSkontoScreen.hasActiveHint())
        assertTrue(
            "No info banner. " + skontoScreen.describeVisibleText(),
            returnAssistantSkontoScreen.hasTextMatching(
                SkontoTexts.normalMessagePattern(SkontoFixtures.RA_VALID_2028_SKONTO_PERCENTAGE)
            )
        )
        assertEquals(
            "The final amount field must hold the discounted amount",
            0,
            AmountText.parse(returnAssistantSkontoScreen.finalAmountText())
                .compareTo(BigDecimal(SkontoFixtures.RA_VALID_2028_SKONTO_AMOUNT_TO_PAY))
        )
    }

    /** PP-2814: Skonto off in the configuration hides the Skonto part, in both orientations. */
    @Test
    fun test4_skontoOffHidesSkontoOnReturnAssistantScreen() {
        openReturnAssistant(skontoEnabled = false)

        assertNoSkontoOnReturnAssistantScreen("portrait")
        inLandscape { assertNoSkontoOnReturnAssistantScreen("landscape") }
    }

    /**
     * PP-2815: Skonto and Return Assistant off in the configuration skip the RA screen and open
     * the extraction screen, in both orientations.
     */
    @Test
    fun test5_skontoAndReturnAssistantOffOpensExtractionScreen() {
        configureReturnAssistantAndSkonto(returnAssistantEnabled = false, skontoEnabled = false)
        // Without RA and Skonto the flow ends on the Analysis screen, where the fixture's due date
        // (about 30 days after its invoice date) triggers the payment hint sheets ("Your invoice is
        // due on …"). They block the way to the extraction screen and are not part of this case.
        PaymentHintConfigurator.applyHintConfiguration(
            activityRule.scenario,
            paymentDueHintEnabled = false,
            paymentScheduleHintEnabled = false
        )

        importPdfAndAwaitAnalysis(SkontoFixtures.RA_VALID_2028_ASSET)

        assertOnExtractionScreen("portrait")
        inLandscape { assertOnExtractionScreen("landscape") }
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────────────

    /**
     * Imports the fixture and dismisses the Return Assistant's onboarding. Fails with the
     * screen's visible text when the RA screen does not open: the standalone Skonto screen showing
     * instead means the API returned no `lineItems` for the fixture, and an RA screen without a
     * Skonto row means it returned no `skontoDiscounts`.
     */
    private fun openReturnAssistant(skontoEnabled: Boolean = true) {
        configureReturnAssistantAndSkonto(returnAssistantEnabled = true, skontoEnabled = skontoEnabled)

        importPdfAndAwaitAnalysis(SkontoFixtures.RA_VALID_2028_ASSET)
        digitalInvoiceScreen.clickGetStartedButtonOnOnboardingScreen()

        assertTrue(
            "Expected the Return Assistant screen — the API returned no lineItems? " +
                skontoScreen.describeVisibleText(),
            digitalInvoiceScreen.isScreenDisplayed()
        )
        if (skontoEnabled) {
            assertTrue(
                "The Return Assistant screen has no Skonto row — the API returned no " +
                    "skontoDiscounts? " + skontoScreen.describeVisibleText(),
                digitalInvoiceScreen.isSkontoRowDisplayed()
            )
        }
    }

    private fun assertNormalMessage(message: String?) {
        val pattern = SkontoTexts.normalMessagePattern(SkontoFixtures.RA_VALID_2028_SKONTO_PERCENTAGE)
        assertTrue(
            "Expected the info text '$pattern', got '$message'",
            message != null && pattern.matches(message)
        )
    }

    private fun assertNoSkontoOnReturnAssistantScreen(orientation: String) {
        assertTrue(
            "Expected the Return Assistant screen in $orientation.",
            digitalInvoiceScreen.isScreenDisplayed()
        )
        assertFalse(
            "No Skonto row may show with Skonto off ($orientation)",
            digitalInvoiceScreen.isSkontoRowDisplayed()
        )
        assertNull(digitalInvoiceScreen.footerSkontoSavedAmountText())
        assertNull(digitalInvoiceScreen.footerSkontoDiscountLabelText())
    }

    private fun assertOnExtractionScreen(orientation: String) {
        assertTrue(
            "Expected the extraction screen in $orientation. " + skontoScreen.describeVisibleText(),
            extractionScreen.assertExtractionScreenIsDisplayed()
        )
        assertFalse(
            "The Return Assistant screen must be skipped ($orientation)",
            digitalInvoiceScreen.isScreenDisplayed()
        )
        assertFalse(
            "The Skonto screen must be skipped ($orientation)",
            returnAssistantSkontoScreen.isDisplayed(timeout = 0)
        )
    }

    /** Runs [block] with the device turned to landscape, then turns it back. */
    private fun inLandscape(block: () -> Unit) {
        try {
            device.setOrientationLandscape()
            device.waitForIdle()
            block()
        } finally {
            device.setOrientationNatural()
            device.unfreezeRotation()
        }
    }

    private fun savedAmount(): BigDecimal =
        BigDecimal(SkontoFixtures.RA_VALID_2028_AMOUNT_TO_PAY)
            .subtract(BigDecimal(SkontoFixtures.RA_VALID_2028_SKONTO_AMOUNT_TO_PAY))
}
