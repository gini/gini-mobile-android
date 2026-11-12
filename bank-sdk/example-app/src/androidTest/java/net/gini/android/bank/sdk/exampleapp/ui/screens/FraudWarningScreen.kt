package net.gini.android.bank.sdk.exampleapp.ui.screens

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import net.gini.android.capture.R as CaptureR

/**
 * Page object for the capture SDK's fraud prevention warning (`IBANMismatchWarningScreen`): the
 * non-dismissable, full-height sheet over the Analysis screen that explains an IBAN mismatch.
 *
 * The warning is Compose in its own `ModalBottomSheet` window and has no test tags, so every node
 * is found by its text, resolved from capture-sdk's string resources at runtime (see
 * [FraudTexts]). Lookups are scoped to the warning's window and scroll its content when needed —
 * see [ComposeSheetFinder].
 *
 * The three warning types share the protection row and the two buttons, so a state is always
 * identified by its title, never by the shared texts.
 */
class FraudWarningScreen {

    /** The three warnings, by the title and description the SDK shows for each. */
    enum class Type(val titleRes: Int, val descriptionRes: Int) {
        QR_VS_PRINTED(
            CaptureR.string.gc_fraud_iban_mismatch_title,
            CaptureR.string.gc_fraud_iban_mismatch_description
        ),
        COUNTRY(
            CaptureR.string.gc_fraud_country_mismatch_title,
            CaptureR.string.gc_fraud_country_mismatch_description
        ),
        COMBINED(
            CaptureR.string.gc_fraud_combined_mismatch_title,
            CaptureR.string.gc_fraud_combined_mismatch_description
        ),
    }

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val finder = ComposeSheetFinder(
        anchorTexts = {
            Type.entries.map { FraudTexts.string(it.titleRes) } + listOf(
                FraudTexts.string(CaptureR.string.gc_fraud_protection_title),
                FraudTexts.string(CaptureR.string.gc_cancel_transfer),
                FraudTexts.string(CaptureR.string.gc_proceed_anyway)
            )
        },
        // The warning refuses swipes (sheetGesturesEnabled = false), so scrolling up is safe.
        canScrollUp = true
    )

    val protectionBody: String
        get() = FraudTexts.string(CaptureR.string.gc_fraud_protection_body)

    /**
     * Waits for the warning of [type]. The warning shows once the analysis has finished, so the
     * wait is generous, like [WarningBottomSheetScreen.waitForSheet].
     */
    fun waitForWarning(type: Type, timeoutMs: Long = SHEET_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { finder.scrollToText(FraudTexts.string(type.titleRes)) != null }

    /**
     * Whether any of the three warnings is open now. Safe for a negative assertion. Checks the
     * titles and the protection row, which only the fraud warning shows; the button labels are
     * left out because the credit note warning uses the same ones.
     */
    fun isAnyWarningDisplayed(): Boolean =
        (Type.entries.map { it.titleRes } + CaptureR.string.gc_fraud_protection_title)
            .any { device.hasObject(By.text(FraudTexts.string(it))) }

    /** Waits until no warning is open any more. */
    fun waitForWarningGone(timeoutMs: Long = GONE_TIMEOUT): Boolean =
        finder.waitUntilGone(timeoutMs)

    /**
     * Whether the warning stays open for the whole of [timeoutMs]. Used after an action that must
     * not close it, such as a back press.
     */
    fun staysOpenFor(timeoutMs: Long): Boolean = !finder.waitUntilGone(timeoutMs)

    /** Whether the warning shows [text] anywhere, scrolling to it if needed. */
    fun isTextShown(text: String): Boolean = finder.scrollToText(text) != null

    /** The texts of [expected] the warning does not show. Empty when all are there. */
    fun missingTexts(expected: List<String>): List<String> = expected.filterNot(::isTextShown)

    /** The texts of [unexpected] the warning does show. Empty when none is there. */
    fun presentTexts(unexpected: List<String>): List<String> = unexpected.filter(::isTextShown)

    /**
     * What the QR-vs-printed warning shows: the shared texts, the list title, the QR code IBAN
     * and every printed IBAN with their labels. It shows no sender and no country hints.
     */
    fun expectedQrVsPrintedTexts(qrIban: String, printedIbans: List<String>): List<String> =
        sharedTexts(Type.QR_VS_PRINTED) +
            FraudTexts.string(CaptureR.string.gc_fraud_detected_ibans_title) +
            FraudTexts.string(CaptureR.string.gc_fraud_qr_iban_label) +
            FraudTexts.formatIban(qrIban) +
            FraudTexts.string(CaptureR.string.gc_fraud_printed_iban_label) +
            printedIbans.map(FraudTexts::formatIban)

    /**
     * What the country warning shows: the shared texts, the sender with its country, and the one
     * IBAN under the "On the document" label, with the country hint when its country differs
     * from the sender's.
     */
    fun expectedCountryTexts(
        senderName: String,
        senderCountryCode: String,
        iban: String
    ): List<String> =
        sharedTexts(Type.COUNTRY) +
            FraudTexts.string(CaptureR.string.gc_fraud_sender_label) +
            FraudTexts.senderLine(senderName, senderCountryCode) +
            FraudTexts.string(CaptureR.string.gc_fraud_document_iban_label) +
            FraudTexts.formatIban(iban) +
            hintsFor(listOf(iban), senderCountryCode)

    /**
     * What the combined warning shows: the shared texts, the sender card, the list title, and the
     * QR code IBAN and the printed IBANs with their labels and country hints.
     */
    fun expectedCombinedTexts(
        senderName: String,
        senderCountryCode: String,
        qrIban: String,
        printedIbans: List<String>
    ): List<String> =
        sharedTexts(Type.COMBINED) +
            FraudTexts.string(CaptureR.string.gc_fraud_sender_label) +
            FraudTexts.senderLine(senderName, senderCountryCode) +
            FraudTexts.string(CaptureR.string.gc_fraud_detected_ibans_title) +
            FraudTexts.string(CaptureR.string.gc_fraud_qr_iban_label) +
            FraudTexts.formatIban(qrIban) +
            FraudTexts.string(CaptureR.string.gc_fraud_printed_iban_label) +
            printedIbans.map(FraudTexts::formatIban) +
            hintsFor(listOf(qrIban) + printedIbans, senderCountryCode)

    /** The sender label and the country hints of [ibans], which the QR-vs-printed case lacks. */
    fun senderAndHintTexts(ibans: List<String>, senderCountryCode: String): List<String> =
        listOf(FraudTexts.string(CaptureR.string.gc_fraud_sender_label)) +
            hintsFor(ibans, senderCountryCode)

    fun clickCancelTransfer(): Boolean =
        finder.clickText(FraudTexts.string(CaptureR.string.gc_cancel_transfer))

    fun clickProceedAnyway(): Boolean =
        finder.clickText(FraudTexts.string(CaptureR.string.gc_proceed_anyway))

    /** Taps "How do I protect myself?", which expands or collapses the row. */
    fun clickProtectionRow(): Boolean =
        finder.clickText(FraudTexts.string(CaptureR.string.gc_fraud_protection_title))

    /**
     * Waits until the protection text is [shown] or hidden. It sits right below the row's title,
     * which was just tapped and is on screen, so no scrolling is needed.
     */
    fun waitForProtectionBody(shown: Boolean, timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { (finder.findTextNow(protectionBody) != null) == shown }

    /**
     * Like [waitForProtectionBody], but scrolls the warning to look for the protection text. For a
     * short window (landscape on a phone), where the row's title can sit at the bottom edge of
     * the sheet when it is tapped, so the text that opens below it is out of view. `shown = false`
     * then means the text is nowhere in the warning, which takes a scroll through the whole sheet.
     */
    fun waitForProtectionBodyScrolling(shown: Boolean, timeoutMs: Long = ELEMENT_TIMEOUT): Boolean =
        waitUntil(timeoutMs) { isTextShown(protectionBody) == shown }

    fun pressBack() {
        device.pressBack()
    }

    private fun sharedTexts(type: Type): List<String> = listOf(
        FraudTexts.string(type.titleRes),
        FraudTexts.string(type.descriptionRes),
        FraudTexts.string(CaptureR.string.gc_fraud_protection_title),
        FraudTexts.string(CaptureR.string.gc_cancel_transfer),
        FraudTexts.string(CaptureR.string.gc_proceed_anyway)
    )

    /** The country hints of [ibans] whose country differs from the sender's, in order. */
    private fun hintsFor(ibans: List<String>, senderCountryCode: String): List<String> =
        ibans.map(FraudTexts::ibanCountry)
            .filter { it != senderCountryCode.uppercase() }
            .map(FraudTexts::countryHint)

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(POLL_INTERVAL)
        }
        return condition()
    }

    private companion object {
        const val SHEET_TIMEOUT = 30_000L
        const val GONE_TIMEOUT = 5_000L
        const val ELEMENT_TIMEOUT = 5_000L
        const val POLL_INTERVAL = 200L
    }
}
