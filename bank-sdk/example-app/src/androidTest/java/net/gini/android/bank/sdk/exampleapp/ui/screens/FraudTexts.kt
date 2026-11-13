package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.platform.app.InstrumentationRegistry
import java.util.IllformedLocaleException
import java.util.Locale
import net.gini.android.capture.R as CaptureR

/**
 * The texts of the capture SDK's fraud warning and IBAN selection sheet, built the way the SDK
 * builds them, so the assertions hold in every language the SDK ships.
 *
 * Every string comes from capture-sdk's resources at runtime. The helpers below copy the three
 * places where the SDK formats a value itself, because those are `internal` to capture-sdk and
 * cannot be called from here. Edit them together with the SDK code:
 * - [formatIban]: `formatIbanForDisplay` in `internal/iban/IBANDisplayFormat.kt`
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
    private const val COUNTRY_CODE_LENGTH = 2
}
