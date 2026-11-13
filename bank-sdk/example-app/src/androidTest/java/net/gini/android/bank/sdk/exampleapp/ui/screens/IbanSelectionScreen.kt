package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.gini.android.capture.R as CaptureR

/**
 * Page object for the capture SDK's IBAN selection sheet (`IBANSelectionSheet`), which "Proceed
 * anyway" opens on top of the QR-vs-printed and combined fraud warnings.
 *
 * On phones it is a `ModalBottomSheet`, on tablets a `Dialog`; either way it has its own window,
 * no test tags, and repeats the warning's labels and IBANs. Every lookup is therefore scoped to
 * the sheet's window (see [ComposeSheetFinder]) and resolved from capture-sdk's string resources
 * at runtime (see [FraudTexts]).
 *
 * The sheet is never scrolled upwards: it is a draggable bottom sheet on phones, and a downward
 * swipe at the top of its content would drag it closed.
 */
class IbanSelectionScreen {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val title: String
        get() = FraudTexts.string(CaptureR.string.gc_fraud_select_iban_title)

    private val continueLabel: String
        get() = FraudTexts.string(CaptureR.string.gc_fraud_select_iban_continue)

    val errorText: String
        get() = FraudTexts.string(CaptureR.string.gc_fraud_select_iban_error)

    private val finder = ComposeSheetFinder(
        anchorTexts = {
            listOf(
                title,
                FraudTexts.string(CaptureR.string.gc_fraud_select_iban_footnote),
                continueLabel
            )
        },
        canScrollUp = false
    )

    fun waitForSheet(timeoutMs: Long = SHEET_TIMEOUT): Boolean = finder.waitUntilShown(timeoutMs)

    fun isSheetDisplayed(): Boolean = finder.isShown()

    fun waitForSheetGone(timeoutMs: Long = SHEET_TIMEOUT): Boolean = finder.waitUntilGone(timeoutMs)

    /**
     * What the sheet shows for [qrIban] and [printedIbans]: the title, the QR code IBAN and every
     * printed IBAN with their labels, the footnote and "Continue". [hintCountryCodes] are the
     * countries whose "From <country>, not the sender's country" hint must show, in the combined
     * case; empty for the QR-vs-printed case.
     *
     * The texts are listed top to bottom, as `IBANSelectionSheet` lays them out: each hint right
     * below the IBAN of its country, footnote and "Continue" last. [missingTexts] only scrolls
     * down, so a text listed after one further down the sheet could be out of view already.
     */
    fun expectedTexts(
        qrIban: String,
        printedIbans: List<String>,
        hintCountryCodes: List<String> = emptyList()
    ): List<String> {
        val hintCodes = hintCountryCodes.map { it.uppercase() }
        val ibanCountries = (listOf(qrIban) + printedIbans).map(FraudTexts::ibanCountry)
        // A code that matches no row is still expected, so a wrong code fails instead of vanishing.
        val unmatchedHints = hintCodes.filterNot { it in ibanCountries }
            .map(FraudTexts::countryHint)
        return listOf(title, FraudTexts.string(CaptureR.string.gc_fraud_qr_iban_label)) +
            rowTexts(qrIban, hintCodes) +
            FraudTexts.string(CaptureR.string.gc_fraud_printed_iban_label) +
            printedIbans.flatMap { rowTexts(it, hintCodes) } +
            unmatchedHints +
            FraudTexts.string(CaptureR.string.gc_fraud_select_iban_footnote) +
            continueLabel
    }

    /** One row's IBAN and, when its country is in [hintCodes], the country hint below it. */
    private fun rowTexts(iban: String, hintCodes: List<String>): List<String> {
        val country = FraudTexts.ibanCountry(iban)
        val hint = if (country in hintCodes) FraudTexts.countryHint(country) else null
        return listOfNotNull(FraudTexts.formatIban(iban), hint)
    }

    /** The texts of [expected] the sheet does not show. Empty when all are there. */
    fun missingTexts(expected: List<String>): List<String> =
        expected.filter { finder.scrollToText(it) == null }

    /** Taps the row of [iban], given unformatted. Returns whether a row was tapped. */
    fun selectIban(iban: String): Boolean = finder.clickText(FraudTexts.formatIban(iban))

    fun clickContinue(): Boolean = finder.clickText(continueLabel)

    /** Waits for "Select an IBAN to continue" under the rows. */
    fun waitForError(timeoutMs: Long = ELEMENT_TIMEOUT): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (finder.scrollToText(errorText) != null) return true
            SystemClock.sleep(POLL_INTERVAL)
        }
        return finder.scrollToText(errorText) != null
    }

    fun pressBack() {
        device.pressBack()
    }

    private companion object {
        const val SHEET_TIMEOUT = 10_000L
        const val ELEMENT_TIMEOUT = 5_000L
        const val POLL_INTERVAL = 200L
    }
}
