package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.platform.app.InstrumentationRegistry
import java.util.IllformedLocaleException
import java.util.Locale
import net.gini.android.capture.R as CaptureR

/**
 * The texts of the capture SDK's fraud warning and IBAN selection sheet, built the way the SDK
 * builds them, so the assertions hold in every language the SDK ships.
 *
 * Every string comes from capture-sdk's resources at runtime. The helpers below copy the places
 * where the SDK formats a value itself, because those are `internal` to capture-sdk and cannot be
 * called from here. Edit them together with the SDK code:
 * - [formatIban]: `formatIbanForDisplay` in `internal/iban/IBANDisplayFormat.kt`
 * - [spokenIban]: `formatIbanForSpeech` in the same file
 * - [countryName]: `countryDisplayName` in `analysis/fraud/CountryName.kt`, in the locale of
 *   `gc_fraud_country_name_locale` (`countryNameLocale` in `IBANMismatchWarningScreen.kt`)
 * - [senderLine] and [countryHint]: `ibanMismatchContent` and `ibanItem` in
 *   `IBANMismatchWarningScreen.kt`
 */
internal object FraudTexts {

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    fun string(resId: Int): String = targetContext.getString(resId)

    /** The IBAN in blocks of four, e.g. `SK28 1100 0000 0026 2015 4106`. */
    fun formatIban(iban: String): String =
        iban.filterNot { it.isWhitespace() }
            .chunked(IBAN_BLOCK_SIZE)
            .joinToString(separator = " ")

    /**
     * The IBAN as TalkBack reads it: every character on its own, a comma between the blocks of
     * four, e.g. `S K 2 8, 1 1 0 0, 0 0 0 0, 0 0 2 6, 2 0 1 5, 4 1 0 6`.
     *
     * An IBAN row puts this, not the written IBAN, into its content description and has no text
     * node, so this is the only form of the IBAN a UI test can find in the row.
     */
    fun spokenIban(iban: String): String =
        iban.filterNot { it.isWhitespace() }
            .chunked(IBAN_BLOCK_SIZE)
            .joinToString(separator = ", ") { block -> block.toList().joinToString(separator = " ") }

    /**
     * [spokenIban] of [text] when it is an IBAN as [formatIban] writes it, otherwise `null`.
     * This is the `spokenForm` of the fraud sheets' [ComposeSheetFinder]: the page objects list
     * the IBANs they expect as written, and the finder looks them up as spoken.
     */
    fun spokenFormOf(text: String): String? =
        if (FORMATTED_IBAN.matches(text)) spokenIban(text) else null

    /** The country name of the ISO 3166-1 alpha-2 [code] in the language of the SDK's strings. */
    fun countryName(code: String): String {
        val locale = Locale.forLanguageTag(string(CaptureR.string.gc_fraud_country_name_locale))
        val name = try {
            Locale.Builder().setRegion(code.uppercase()).build().getDisplayCountry(locale)
        } catch (_: IllformedLocaleException) {
            null
        }
        return name?.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) } ?: code
    }

    /** The sender value, e.g. "Martin Braun Backmittel und Essenzen KG · Germany". */
    fun senderLine(senderName: String, senderCountryCode: String): String =
        targetContext.getString(
            CaptureR.string.gc_fraud_sender_with_country,
            senderName,
            countryName(senderCountryCode)
        )

    /** The red hint under an IBAN, e.g. "From Slovakia, not the sender’s country". */
    fun countryHint(countryCode: String): String =
        targetContext.getString(CaptureR.string.gc_fraud_iban_country_hint, countryName(countryCode))

    /** The country code of [iban]: its first two characters. */
    fun ibanCountry(iban: String): String =
        iban.filterNot { it.isWhitespace() }.take(COUNTRY_CODE_LENGTH).uppercase()

    private const val IBAN_BLOCK_SIZE = 4

    /** Country code, two check digits, then blocks of up to four characters, as [formatIban]. */
    private val FORMATTED_IBAN = Regex("[A-Z]{2}[0-9]{2}( [A-Z0-9]{1,4})+")
    private const val COUNTRY_CODE_LENGTH = 2
}
