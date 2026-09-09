package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.os.SystemClock
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import net.gini.android.bank.sdk.exampleapp.ui.resources.AmountText
import net.gini.android.bank.sdk.exampleapp.ui.resources.AppResources
import java.math.BigDecimal

/**
 * Page object for the standalone Skonto screen (`SkontoFragment`).
 *
 * The screen is Jetpack Compose, so its controls are addressed by Compose test tag rather
 * than by view id. `SkontoFragment` opts its Compose root into
 * `testTagsAsResourceId`, which surfaces each tag to UiAutomator as the node's
 * resource-id — so a tag is matched with `By.res("<tag>")`, taking the bare tag string
 * and *not* a `package:id/name` form the way [AppResources.resId] builds for XML views.
 *
 * The tag literals below are a deliberate copy of `SkontoTestTags` in
 * `bank-sdk/sdk/src/main/java/net/gini/android/bank/sdk/capture/skonto/SkontoTestTags.kt`.
 * That object is `internal` to `bank-sdk:sdk`, so this module cannot import it; the two
 * must be edited together and are greppable by the literal string.
 *
 * Everything that is *not* tagged is matched by a string resolved from the SDK's own
 * `R.string` at runtime, never by a hard-coded German or English literal — the screen's
 * copy is translated, and the date-picker dialog is a separate Compose root that does not
 * inherit the `testTagsAsResourceId` opt-in.
 */
class SkontoScreen {

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun sdkString(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    // region element lookup

    private fun requireTagged(tag: String, timeout: Long = DEFAULT_TIMEOUT): UiObject2 =
        device.wait(Until.findObject(By.res(tag)), timeout)
            ?: error(
                "No node with Compose test tag '$tag'. ${describeCurrentScreen()}"
            )

    /**
     * Names the screen actually on display, for the failure message.
     *
     * Worth the effort because the interesting failure is almost never "the tag is missing".
     * `CaptureFlowFragment.tryShowingSkontoScreen` falls back to the extraction screen
     * *silently* whenever `extractSkontoData` throws — which it does for any analysis result
     * without a `skontoDiscounts` compound extraction. A message that only blamed the test
     * tag sent a whole BrowserStack run's diagnosis in the wrong direction once already.
     */
    private fun describeCurrentScreen(): String {
        val onExtractionScreen =
            device.hasObject(By.res(AppResources.resId("transfer_summary")))
        val onDigitalInvoice = device.hasObject(By.res(AppResources.resId("line_items")))
        val onSkontoScreen = device.hasObject(By.res(DISCOUNT_TITLE))

        return when {
            onExtractionScreen -> "The EXTRACTION screen is showing instead of the Skonto " +
                "screen, so the SDK never routed to Skonto: the analysis result carried no " +
                "`skontoDiscounts` compound extraction, or the skonto flags were off. Check " +
                "that the fixture still extracts skonto (SkontoFixtures — a regenerated " +
                "fixture must be re-validated against the API) and that BOTH the SDK-side " +
                "flag and the backend's client-configuration flag are enabled."

            onDigitalInvoice -> "The DIGITAL INVOICE screen is showing: the result carried " +
                "`lineItems`, so the Return Assistant claimed the flow before Skonto."

            onSkontoScreen -> "The Skonto screen IS showing, so this particular tag is " +
                "genuinely missing — check SkontoTestTags and its use in SkontoScreenContent."

            else -> "No recognised screen is showing (not Skonto, extraction or digital " +
                "invoice). The flow may still be analysing, or it hit an error or " +
                "no-results screen."
        }
    }

    /**
     * Waits for the Skonto screen to settle. Anchored on the discount section title, which
     * the screen cannot render without, rather than on the footer — the footer animates in
     * and is the last thing to appear.
     */
    fun waitForSkontoScreen(): SkontoScreen {
        requireTagged(DISCOUNT_TITLE, SCREEN_TIMEOUT)
        // Wait for the two elements the tests actually interact with, not just the title.
        // The title appears as soon as the section composes, while the amount field and the
        // animated footer settle a moment later — acting before then makes an edit or a read
        // race the initial composition.
        requireTagged(FINAL_AMOUNT_FIELD, SCREEN_TIMEOUT)
        requireTagged(FOOTER_TOTAL, SCREEN_TIMEOUT)
        return this
    }

    /** Whether the Skonto screen is on screen right now. Safe for a negative assertion. */
    fun isScreenDisplayed(): Boolean =
        device.wait(Until.hasObject(By.res(DISCOUNT_TITLE)), DEFAULT_TIMEOUT) == true

    // endregion

    // region discount toggle

    fun isDiscountToggleChecked(): Boolean = requireTagged(DISCOUNT_SWITCH).isChecked

    fun clickDiscountToggle(): SkontoScreen {
        val toggle = requireTagged(DISCOUNT_SWITCH)
        val before = toggle.isChecked
        toggle.click()
        // The switch state drives a recomposition of the whole section, so wait for the
        // flip rather than reading straight back and racing it.
        device.wait(Until.findObject(By.res(DISCOUNT_SWITCH).checked(!before)), DEFAULT_TIMEOUT)
            ?: error("Discount toggle did not change from checked=$before after the click.")
        return this
    }

    // endregion

    // region amounts

    /**
     * Reads the footer total, once it has stopped moving, as a number.
     *
     * `FooterSection` animates the total through `animateFloatAsState` and formats the text
     * from the animating value, so a single read lands on an in-flight frame and compares the
     * wrong number. This polls until two consecutive reads agree, which is what makes the
     * before/after comparisons in the tests meaningful.
     *
     * Returned as [BigDecimal] rather than [String] because those comparisons need an
     * ordering, and "1.234,56 €" against "1.334,56 €" is not one.
     */
    fun readFooterTotal(): BigDecimal {
        var previous = AmountText.parse(requireTagged(FOOTER_TOTAL).text)
        val deadline = SystemClock.uptimeMillis() + ANIMATION_SETTLE_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(ANIMATION_POLL_INTERVAL)
            val current = AmountText.parse(requireTagged(FOOTER_TOTAL).text)
            if (current.compareTo(previous) == 0) return current
            previous = current
        }
        return previous
    }

    fun readFinalAmount(): BigDecimal = AmountText.parse(requireTagged(FINAL_AMOUNT_FIELD).text)

    /**
     * Types [value] into the final-amount field and dismisses the keyboard.
     *
     * `GiniAmountTextInput` recomputes on every keystroke, so nothing here waits for focus
     * loss — the keyboard is closed only so it cannot cover a later assertion. Closed with
     * `Espresso.closeSoftKeyboard()` rather than `device.pressBack()`: if the keyboard
     * happens to be down already (hardware keyboard on a device rack, or the IME dismissed
     * by the accessibility set-text), a back press reaches the activity and pops the Skonto
     * screen, and every later failure then points at the wrong cause. Closing the keyboard
     * is a no-op when it is not showing.
     */
    fun setFinalAmount(value: String): SkontoScreen {
        val field = requireTagged(FINAL_AMOUNT_FIELD)
        field.click()
        field.text = value
        Espresso.closeSoftKeyboard()
        return this
    }

    // endregion

    // region expiry date

    /**
     * The expiry date as the field exposes it to accessibility — a *spoken* date such as
     * "7th of September 2026" (English) or "7. September 2026" (other locales), followed by
     * the field's label.
     *
     * Read from `contentDescription`, not `text`, because `GiniTextInput` applies
     * `Modifier.clearAndSetSemantics { contentDescription = getSpokenDateForTalkBack(text) + labelText }`
     * when `isDate = true` — to stop TalkBack reading the date twice. `clearAndSetSemantics`
     * wipes the descendants' semantics, so the node has **no text property at all** and
     * reading `.text` returns empty. That is what made the first attempt at this fail with
     * "Could not read a day of month from expiry date ''".
     */
    fun readExpiryDate(): String = requireTagged(EXPIRY_DATE_FIELD).contentDescription ?: ""

    /**
     * The day-of-month currently shown in the expiry-date field.
     *
     * Taken as the first run of digits in the spoken date, which both locale formats lead
     * with ("7th of September…", "7. September…").
     */
    fun readExpiryDayOfMonth(): Int {
        val spoken = readExpiryDate()
        return DAY_PREFIX.find(spoken)?.value?.toIntOrNull()
            ?: error(
                "Could not read a day of month from the expiry-date field's " +
                    "contentDescription '$spoken'."
            )
    }

    /**
     * Opens the expiry-date picker, confirms it appeared, and dismisses it again.
     *
     * The dialog is `GiniDatePickerDialog` — a Material 3 `DatePicker` in its own Compose
     * root — so its nodes carry no test tags and are matched by the SDK's own
     * `gc_date_picker_select` and `gc_date_picker_cancel` strings.
     *
     * Returns whether it opened, so a test can assert on it.
     *
     * There is deliberately no "pick a day" counterpart. `getSkontoSelectableDates` limits
     * the picker to now through six months out, while a fixture whose discount is still
     * claimable has to be dated years ahead — so no day in the month the picker opens on is
     * selectable, and a tap on one silently does nothing. Changing the date is a manual
     * step; see COVERAGE.md.
     */
    fun openAndDismissDatePicker(): Boolean {
        val selectLabel = sdkString(net.gini.android.capture.R.string.gc_date_picker_select)
        openExpiryDatePicker(selectLabel)

        val cancelLabel = sdkString(net.gini.android.capture.R.string.gc_date_picker_cancel)
        val cancel = device.wait(Until.findObject(By.text(cancelLabel)), DEFAULT_TIMEOUT)
            ?: error("Date picker opened but has no '$cancelLabel' button to dismiss it.")
        cancel.click()
        device.wait(Until.gone(By.text(selectLabel)), DEFAULT_TIMEOUT)
        return true
    }

    /**
     * Opens the expiry-date picker, waiting for its confirm button to prove it opened.
     *
     * Needs more than a plain click. The field is a `readOnly` Compose text field whose
     * `.clickable` never fires — `BasicTextField` consumes the tap first — which is exactly
     * why the SDK opens the picker from an interaction source instead:
     *
     * ```
     * val pressed by dueDateOnClickSource.collectIsPressedAsState()
     * LaunchedEffect(key1 = pressed) { if (pressed) onDatePickerVisibilityChanged(true) }
     * ```
     *
     * `collectIsPressedAsState()` is only true *while the press is held*, and UiAutomator's
     * `click()` is a down-up fast enough that the recomposition may never observe it. So a
     * click is tried first and a long click — a real press-and-hold — is the fallback. The
     * field is `readOnly` and `focusable(false)`, so a hold raises no text-selection UI.
     */
    private fun openExpiryDatePicker(selectLabel: String) {
        val field = requireTagged(EXPIRY_DATE_FIELD)
        field.click()
        if (device.wait(Until.hasObject(By.text(selectLabel)), PICKER_OPEN_TIMEOUT) == true) return

        requireTagged(EXPIRY_DATE_FIELD).longClick()
        if (device.wait(Until.hasObject(By.text(selectLabel)), PICKER_OPEN_TIMEOUT) == true) return

        error(
            "Date picker did not open after a click and a long click on the expiry-date " +
                "field — no '$selectLabel' button appeared. ${describeVisibleText()}"
        )
    }

    /**
     * Dismisses the edge-case info dialog if it is up.
     *
     * The SDK opens it whenever `SkontoScreenState.Ready.edgeCaseInfoDialogVisible` is set —
     * an expired discount, a cash-only discount, or the last claimable day. It is a Compose
     * `Dialog`, i.e. its own semantics owner, so it does not inherit the screen's
     * `testTagsAsResourceId` opt-in and has to be matched by the SDK's own OK-button string.
     * A no-op when no dialog is showing.
     */
    fun dismissEdgeCaseInfoDialog(): SkontoScreen {
        val okLabel = sdkString(
            net.gini.android.bank.sdk.R.string.gbs_skonto_section_info_dialog_ok_button_text
        )
        device.wait(Until.findObject(By.text(okLabel)), DEFAULT_TIMEOUT)?.let { button ->
            button.click()
            device.wait(Until.gone(By.text(okLabel)), DEFAULT_TIMEOUT)
        }
        return this
    }

    /**
     * Whether the inline validation error for [resId] is showing under the amount field.
     * The message carries a `%1$s` placeholder in some cases, so only the literal prefix
     * before the first placeholder is matched.
     */
    fun isAmountValidationErrorDisplayed(resId: Int): Boolean {
        val message = sdkString(resId).substringBefore("%1\$s").trim()
        return device.wait(Until.hasObject(By.textContains(message)), DEFAULT_TIMEOUT) == true
    }

    /**
     * Every piece of text currently in the accessibility tree, for a failure message.
     *
     * Exists because "the expected error was not displayed" is not a diagnosis: it does not
     * say whether the error is absent, worded differently, or rendered somewhere this
     * selector cannot see. It has already earned its keep twice — it is what revealed that
     * Material 3 labels a day cell with a full date, and that an eight-digit amount never
     * reaches the field. Call it only on the failure path; it walks the whole tree.
     */
    fun describeVisibleText(): String {
        val texts = device.findObjects(By.textContains(""))
            .mapNotNull { it.text?.takeIf(String::isNotBlank) }
            .distinct()
        val descriptions = device.findObjects(By.clazz(".*".toPattern()))
            .mapNotNull { it.contentDescription?.takeIf(String::isNotBlank) }
            .distinct()
        return "Visible text: $texts; contentDescriptions: $descriptions"
    }

    fun isProceedButtonDisplayed(): Boolean =
        device.wait(Until.hasObject(By.res(PROCEED_BUTTON)), DEFAULT_TIMEOUT) == true

    fun clickProceedButton(): SkontoScreen {
        requireTagged(PROCEED_BUTTON).click()
        return this
    }

    // endregion

    private companion object {
        const val DEFAULT_TIMEOUT = 5_000L

        /**
         * How long to wait for the Skonto screen's own composition, *not* for the analysis
         * that precedes it — `SkontoScreenTests` runs against the real API and does its
         * waiting through `SmokeJourneyTestBase.idlingTimeoutMs`, so by the time these
         * lookups run the navigation has already happened.
         *
         * Deliberately not the 30s an earlier revision used: six tests each burning 30s
         * before failing is what starved a BrowserStack session and left most of the class
         * unreported.
         */
        const val SCREEN_TIMEOUT = 10_000L

        /** Long enough for the footer's animateFloatAsState to come to rest. */
        const val ANIMATION_SETTLE_TIMEOUT = 5_000L
        const val ANIMATION_POLL_INTERVAL = 150L

        /** Per attempt at opening the date picker; two attempts are made. */
        const val PICKER_OPEN_TIMEOUT = 3_000L

        /** Leading digits of a spoken date ("19th of August 2028", "19. August 2028"). */
        val DAY_PREFIX = "^\\d{1,2}".toRegex()

        // Mirrors SkontoTestTags in bank-sdk:sdk — see the class KDoc.
        const val DISCOUNT_SWITCH = "skontoDiscountSwitch"
        const val DISCOUNT_TITLE = "skontoDiscountTitle"
        const val FINAL_AMOUNT_FIELD = "skontoFinalAmountField"
        const val EXPIRY_DATE_FIELD = "skontoExpiryDateField"
        const val FOOTER_TOTAL = "skontoFooterTotal"
        const val PROCEED_BUTTON = "skontoProceedButton"
    }
}
