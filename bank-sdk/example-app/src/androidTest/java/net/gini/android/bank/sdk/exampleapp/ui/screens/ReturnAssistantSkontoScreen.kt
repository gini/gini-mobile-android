package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/**
 * Page object for the Skonto screen of the Return Assistant (`DigitalInvoiceSkontoFragment`),
 * opened with the Edit button of the Skonto row on the digital invoice screen.
 *
 * It is a different screen from the standalone one in [SkontoScreen]. That one opts its Compose
 * root into `testTagsAsResourceId` and tags its controls; this one has no test tags at all. So
 * every node here is matched by a string resolved from the SDK's own `R.string` at runtime —
 * a text, or a content description for the two icon buttons — never by a hard-coded German or
 * English literal, because the screen's copy is translated.
 */
class ReturnAssistantSkontoScreen {

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun sdkString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun has(selector: BySelector, timeout: Long = DEFAULT_TIMEOUT): Boolean =
        device.wait(Until.hasObject(selector), timeout) == true

    /**
     * Whether the screen is up: its discount section title is on screen. The title "Skonto" alone
     * is not enough — the Skonto row of the digital invoice screen carries the same word.
     */
    fun isDisplayed(timeout: Long = SCREEN_TIMEOUT): Boolean =
        has(By.text(sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_title)), timeout)

    fun hasBackButton(): Boolean =
        has(By.desc(sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_screen_content_description_back)))

    fun hasHelpButton(): Boolean =
        has(By.desc(sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_screen_content_description_help)))

    fun hasTitle(): Boolean =
        has(By.text(sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_screen_title)))

    /** The invoice row: its title and the "tap to view" subtitle. */
    fun hasInvoicePreviewRow(): Boolean =
        has(By.text(sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_section_invoice_preview_title))) &&
            has(By.text(sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_invoice_section_preview_subtitle)))

    /**
     * The green "• Active" hint next to the discount title. Matched with `textContains` on the
     * trimmed string: the resource starts with spaces and a bullet.
     */
    fun hasActiveHint(): Boolean {
        val hint = sdkString(net.gini.android.bank.sdk.R.string.gbs_skonto_section_discount_hint_label_enabled)
            .trim()
        return has(By.textContains(hint))
    }

    /** Whether [text] — the info banner, for example — is on screen. */
    fun hasText(text: String): Boolean = has(By.text(text))

    /** Whether a text matching [pattern] is on screen — for texts with a variable part. */
    fun hasTextMatching(pattern: Regex): Boolean = has(By.text(pattern.toPattern()))

    /**
     * The text of the final amount field, the first text field on the screen (the due date field
     * follows it), or `null` when there is none.
     */
    fun finalAmountText(): String? = firstTextField()?.text

    private fun firstTextField(): UiObject2? {
        device.wait(Until.hasObject(By.clazz(EDIT_TEXT)), DEFAULT_TIMEOUT)
        return device.findObjects(By.clazz(EDIT_TEXT))
            .minByOrNull { it.visibleBounds.top }
    }

    private companion object {
        const val DEFAULT_TIMEOUT = 5_000L

        /** The screen is a navigation away from the digital invoice screen, nothing slower. */
        const val SCREEN_TIMEOUT = 10_000L

        const val EDIT_TEXT = "android.widget.EditText"
    }
}
