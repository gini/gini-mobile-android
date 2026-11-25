package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.os.SystemClock
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiCollection
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import net.gini.android.bank.sdk.exampleapp.ui.resources.AmountText
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import org.hamcrest.Matchers.allOf


class DigitalInvoiceScreen {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var initialValue: String? = null
    private var updatedValue: String? = null

    /**
     * Whether the Digital Invoice (Return Assistant) screen is on screen right now.
     *
     * Absence-safe, unlike [checkDigitalInvoiceTitleIsDisplayed], which throws
     * `UiObjectNotFoundException` when the screen is not there and matches a hard-coded English
     * title — neither works for a negative assertion. Matched on the `line_items` list instead:
     * a view id, so it is locale-independent, and it is the one view the screen cannot render
     * without.
     */
    fun isScreenDisplayed(): Boolean =
        device.findObject(UiSelector().resourceId(AppResources.resId("line_items"))).exists()

    fun checkDigitalInvoiceTitleIsDisplayed(): Boolean {
        val uiCollection =
            UiCollection(UiSelector().className("android.view.ViewGroup"))

        val digitalInvoiceText = uiCollection.getChildByInstance(
            UiSelector().className("android.widget.TextView").text("Digital invoice"), 0)

        return digitalInvoiceText.isEnabled
    }

    fun checkDigitalInvoiceTextOnOnboardingScreenIsDisplayed(): Boolean {
        val onboardingScreenText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Digital invoice")
                .resourceId(AppResources.resId("onboarding_text_1"))
        )
        return onboardingScreenText.waitForExists(ONBOARDING_TIMEOUT)
    }

    fun checkDigitalInvoiceButtonOnOnboardingScreenIsDisplayed(): Boolean {
        val onboardingScreenButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .text("Get Started")
                .resourceId(AppResources.resId("done_button"))
        )
        return onboardingScreenButton.waitForExists(ONBOARDING_TIMEOUT)
    }

    fun clickGetStartedButtonOnOnboardingScreen() {
        val onboardingScreenButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .text("Get Started")
                .resourceId(AppResources.resId("done_button"))
        )
        if (onboardingScreenButton.waitForExists(ONBOARDING_TIMEOUT) && onboardingScreenButton.isClickable) {
            onboardingScreenButton.click()
        }
    }

    fun clickCancelButton() {
        val cancelButton = device.findObject(
            UiSelector()
                .className("android.widget.ImageButton")
                .descriptionContains("Close")
        )
        if(cancelButton.exists() && cancelButton.isClickable()){
            cancelButton.click()
        }
    }

    fun assertOtherChargesDisplayed() : Boolean {
        // The digital-invoice content comes from the network extraction result; wait for the
        // "other charges" label to render before asserting so a slow render doesn't fail it.
        val text = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_addon_other_charges)
        device.findObject(UiSelector().textContains(text)).waitForExists(15_000L)

        var isOtherChargesDisplayed = false
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_digital_invoice_addon_other_charges))
            .check { view,_ ->
                if (view != null && view.isShown()) {
                    isOtherChargesDisplayed = true
                }
            }
        return isOtherChargesDisplayed
    }

    fun clickProceedButton() {
        onView(withText(net.gini.android.bank.sdk.R.string.gbs_proceed)).perform(click())
    }

    /**
     * The Skonto discount row, or `null` when it is not on screen.
     *
     * Scoped by *content*, not by index, because the Skonto row and a line-item row share
     * every structural id — both roots are `gsb_line_item` and both switches are
     * `gbs_enable_switch` (see `gbs_item_digital_invoice_skonto.xml` and
     * `gbs_item_digital_invoice_line_item.xml`). The only reliable difference is what each
     * row contains: the Skonto row's `gbs_per_unit` always reads the SDK's
     * `gbs_skonto_screen_title` ("Skonto"), where a line item's shows its unit price.
     *
     * Not matched on `gbs_skonto_amount`, although it is unique to the Skonto row: the adapter
     * sets it GONE whenever the discount is off, and a GONE view is not in the accessibility
     * tree — so a row switched off, expired or cash-only would not be found. Nor on a missing
     * `gbs_description`: a line-item row whose top is scrolled out of view has none either.
     *
     * The existing [clickArticleSwitch] disambiguates with `.index(1)` instead, which is why
     * it stopped working once a Skonto row joined the list — a positional selector indexes
     * into a live list. It is left alone because other suites depend on it.
     */
    private fun skontoRow(): UiObject2? {
        device.wait(Until.hasObject(By.res(AppResources.resId(ROW_ROOT))), SKONTO_ROW_TIMEOUT)
        return device.findObjects(By.res(AppResources.resId(ROW_ROOT)))
            .firstOrNull { row ->
                row.findObject(By.res(AppResources.resId(SKONTO_ROW_TITLE)).text(skontoTitle())) != null
            }
    }

    private fun skontoTitle(): String =
        InstrumentationRegistry.getInstrumentation().targetContext
            .getString(net.gini.android.bank.sdk.R.string.gbs_skonto_screen_title)

    private fun requireSkontoRow(): UiObject2 =
        skontoRow() ?: error("No Skonto row on the digital invoice screen.")

    private fun UiObject2.childText(id: String): String? =
        findObject(By.res(AppResources.resId(id)))?.text

    /** The saved amount on the Skonto row, e.g. "-1,26 EUR", or `null` while the discount is off. */
    fun skontoRowAmountText(): String? = requireSkontoRow().childText(SKONTO_ROW_AMOUNT)

    /** The grey info text on the Skonto row, or `null` when the row shows none. */
    fun skontoRowMessageText(): String? = requireSkontoRow().childText(SKONTO_ROW_MESSAGE)

    /**
     * The Skonto row's Edit button, or `null` when the row has none.
     *
     * The Skonto row is the last item of the list, and Edit is its bottom view. With both the
     * saved amount and the info text shown (an active discount with an edge case, e.g. "pay
     * today") the row is tall enough that Edit sits below the visible list, where UiAutomator does
     * not see it — so the list is scrolled to its end once before giving up.
     */
    private fun skontoRowEditButton(): UiObject2? {
        requireSkontoRow().findObject(By.res(AppResources.resId(EDIT_BUTTON)))?.let { return it }
        device.findObject(By.res(AppResources.resId(LINE_ITEMS)))?.scroll(Direction.DOWN, 1f)
        return requireSkontoRow().findObject(By.res(AppResources.resId(EDIT_BUTTON)))
    }

    private fun requireSkontoRowEditButton(): UiObject2 =
        skontoRowEditButton() ?: error("The Skonto row has no $EDIT_BUTTON.")

    fun isSkontoRowEditButtonDisplayed(): Boolean = skontoRowEditButton() != null

    /** `false` while the discount is off: the adapter disables the Edit button then. */
    fun isSkontoRowEditButtonEnabled(): Boolean = requireSkontoRowEditButton().isEnabled

    /** Opens the Skonto screen of the Return Assistant. */
    fun clickSkontoRowEditButton() {
        requireSkontoRowEditButton().click()
    }

    /** The footer's "… sparen" / "Save …" text, or `null` while the discount is off. */
    fun footerSkontoSavedAmountText(): String? =
        device.findObject(By.res(AppResources.resId(FOOTER_SAVED_AMOUNT)))?.text

    /** The footer's "…% Skonto" label, or `null` while the discount is off. */
    fun footerSkontoDiscountLabelText(): String? =
        device.findObject(By.res(AppResources.resId(FOOTER_DISCOUNT_LABEL)))?.text

    /** A line-item row (the first one), identified by the description only it carries. */
    private fun lineItemRow(): UiObject2? {
        device.wait(Until.hasObject(By.res(AppResources.resId(ROW_ROOT))), SKONTO_ROW_TIMEOUT)
        return device.findObjects(By.res(AppResources.resId(ROW_ROOT)))
            .firstOrNull { row ->
                row.findObject(By.res(AppResources.resId(LINE_ITEM_MARKER))) != null
            }
    }

    private fun UiObject2.enableSwitch(): UiObject2 =
        findObject(By.res(AppResources.resId(ENABLE_SWITCH)))
            ?: error("Row has no $ENABLE_SWITCH — the row layout changed.")

    fun isSkontoRowDisplayed(): Boolean = skontoRow() != null

    fun isSkontoRowToggleChecked(): Boolean = requireSkontoRow().enableSwitch().isChecked

    /**
     * Flips the Skonto row's toggle and waits for the flip to land.
     *
     * Safe to verify by re-finding the row: there is only ever one Skonto row and switching
     * it off does not remove it from the list.
     */
    fun clickSkontoRowSwitch(): DigitalInvoiceScreen {
        val target = skontoRow() ?: error("No Skonto row on the digital invoice screen.")
        val before = target.enableSwitch().isChecked
        target.enableSwitch().click()
        val deadline = System.currentTimeMillis() + SKONTO_ROW_TIMEOUT
        while (System.currentTimeMillis() < deadline) {
            // Re-found each poll: toggling rebinds the RecyclerView and stales the old node.
            if (skontoRow()?.enableSwitch()?.isChecked == !before) return this
            SystemClock.sleep(SWITCH_POLL_INTERVAL)
        }
        error(
            "The Skonto toggle did not change from checked=$before after the click."
        )
    }

    /**
     * Flips the first line item's toggle.
     *
     * Deliberately does **not** verify the switch state afterwards. Switching an item off
     * rebinds the list — the neighbouring test is literally called
     * `test2_disableToggleSwitchToRemoveItemFromList` — so "the first row carrying a
     * description" is a *different* row after the click, and polling it waits forever on a
     * still-enabled sibling. That is what made an earlier version of this time out.
     *
     * The caller asserts the consequence instead, with [waitForTotalToChangeFrom]: the total
     * moving is the observable behaviour the test is about, and it cannot pass without the
     * click having landed.
     */
    fun clickLineItemSwitch(): DigitalInvoiceScreen {
        val target = lineItemRow() ?: error("No line-item row on the digital invoice screen.")
        target.enableSwitch().click()
        return this
    }

    /**
     * Waits for the footer total to move away from [previous] and returns the new value.
     *
     * The total is what proves a toggle took effect, so it is polled rather than read once —
     * the list rebinds and the footer recomputes a moment after the tap.
     */
    fun waitForTotalToChangeFrom(previous: java.math.BigDecimal): java.math.BigDecimal {
        val deadline = System.currentTimeMillis() + SKONTO_ROW_TIMEOUT
        while (System.currentTimeMillis() < deadline) {
            val current = readTotalIntegralPart()
            if (current.compareTo(previous) != 0) return current
            SystemClock.sleep(SWITCH_POLL_INTERVAL)
        }
        error("The total stayed at $previous — the toggle had no effect.")
    }

    fun clickArticleSwitch(): DigitalInvoiceScreen {
        val articleSwitch = device.findObject(
            UiSelector()
                .className("android.widget.Switch")
                .resourceId(AppResources.resId("gbs_enable_switch"))
                .index(1)
        )
        if(articleSwitch.exists() && articleSwitch.isClickable){
            articleSwitch.click()
        }
        return this
    }

    fun  checkItemIsEnabledFromDigitalScreen(): Boolean {
        val lineItem = device.findObject(UiSelector()
            .className("android.view.ViewGroup")
            .resourceId(AppResources.resId("gsb_line_item"))
            .index(0))
        return lineItem.isEnabled
    }

    fun clickHelpButtonOnDigitalInvoiceScreen() {
        val helpButton = device.findObject(
            UiSelector()
                .className("android.widget.Button")
                .descriptionContains("Help")
        )
        if(helpButton.exists()){
            helpButton.click()
        }
    }

    fun verifyHelpTextOnNextScreen(): Boolean {
        val helpText = device.findObject(
            UiSelector()
                .className("android.widget.TextView")
                .text("Help")
        )
        return helpText.exists()
    }

    fun verifyFirstTitleOnHelpScreen(): Boolean {
        var isFirstTitleDisplayed = false
        onView(
            allOf(withId(net.gini.android.bank.sdk.R.id.gbs_help_title),
                withText("1. How does a digital invoice work?")
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isFirstTitleDisplayed = true
                }
            }
        return isFirstTitleDisplayed
    }

    fun checkTotalTitleIsDisplayed(): Boolean {
        var isTotalTitleDisplayed = false
        onView(
            allOf(withId(net.gini.android.bank.sdk.R.id.total_label),
                withText("Total")
            )
        )
            .check { view, _ ->
                if (view.isShown()) {
                    isTotalTitleDisplayed = true
                }
            }
        return isTotalTitleDisplayed
    }

    fun checkTotalPriceIsDisplayed() : Boolean{
        var isTotalPriceDisplayed = false
        onView((withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part)))
            .check { view, _ ->
                if (view.isShown()) {
                    isTotalPriceDisplayed = true
                }
            }
        return isTotalPriceDisplayed
    }

    /**
     * The footer total as a number, so a test can assert which *direction* it moved.
     *
     * [storeInitialPrice] / [storeUpdatedPrice] / [verifyTotalSumValue] only prove the total
     * changed. TC-013 asks for more than that — a line item off must *reduce* the total and
     * the discount off must *raise* it — so this reads the value instead of comparing strings.
     *
     * Reads only the integral part, which is the view the existing helpers use too. That is
     * enough for a direction comparison and avoids depending on how the fractional part and
     * currency symbol are split across views.
     */
    fun readTotalIntegralPart(): java.math.BigDecimal {
        var raw: String? = null
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ -> raw = (view as? TextView)?.text?.toString() }
        // Was a bare cast, which threw "null cannot be cast to non-null type TextView" when
        // the flow had not reached the digital invoice screen at all — an NPE that named the
        // cast rather than the cause.
        return AmountText.parse(
            raw ?: error(
                "No digital-invoice total on screen: the flow is not on the digital invoice " +
                    "screen. The analysis result may have carried no lineItems, so the " +
                    "Return Assistant never claimed the flow."
            )
        )
    }

    fun storeInitialPrice() {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ ->
                val totalTextView = view as TextView
                initialValue = totalTextView.text.toString()
            }
    }

    fun verifyTotalSumValue(): Boolean{
        var isSumDifferent = false
            if(initialValue!=updatedValue) {
            isSumDifferent = true
        }
        return isSumDifferent
    }

    fun storeUpdatedPrice() {
        onView(withId(net.gini.android.bank.sdk.R.id.gross_price_total_integral_part))
            .check { view, _ ->
                val totalTextView = view as TextView
                updatedValue = totalTextView.text.toString()
            }
    }

    companion object {
        // The digital-invoice onboarding screen only appears after the Gini API returns the
        // extraction, which can be slow on remote/BrowserStack devices. Wait generously.
        private const val ONBOARDING_TIMEOUT = 30_000L

        /** Shared by both row layouts, hence the content-scoped lookups above. */
        private const val ROW_ROOT = "gsb_line_item"
        private const val ENABLE_SWITCH = "gbs_enable_switch"

        /** Always on a line-item row, never on the Skonto row. */
        private const val LINE_ITEM_MARKER = "gbs_description"

        /** On the Skonto row it holds the Skonto title; on a line item, the unit price. */
        private const val SKONTO_ROW_TITLE = "gbs_per_unit"

        // Views of the Skonto row (gbs_item_digital_invoice_skonto.xml) and the footer
        // (gbs_fragment_digital_invoice.xml).
        private const val SKONTO_ROW_AMOUNT = "gbs_skonto_amount"
        private const val SKONTO_ROW_MESSAGE = "gbs_message"
        private const val EDIT_BUTTON = "gbs_edit_button"
        private const val LINE_ITEMS = "line_items"
        private const val FOOTER_SAVED_AMOUNT = "skonto_saved_amount"
        private const val FOOTER_DISCOUNT_LABEL = "skonto_discount_label"

        private const val SKONTO_ROW_TIMEOUT = 5_000L
        private const val SWITCH_POLL_INTERVAL = 150L
    }
}