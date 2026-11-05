package net.gini.android.capture.analysis.fraud

import net.gini.android.capture.fraudSignals.FraudSignal
import net.gini.android.capture.fraudSignals.FraudSignalCode
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Builds the [IBANMismatchArgs] of the warning from the parsed fraud signals and the specific
 * extractions of the same analysis. Returns `null` when no warning should be shown.
 *
 * The printed IBANs are the candidates of the `iban` extraction without the QR code IBAN (the API
 * may list it among them). A QR code signal without any other printed IBAN cannot be explained on
 * the screen, so it is dropped. Never throws.
 */
internal object IBANMismatchArgsMapper {

    private const val IBAN_KEY = "iban"
    private const val PAYMENT_RECIPIENT_KEY = "paymentRecipient"
    private const val COUNTRY_CODE_LENGTH = 2

    fun map(
        signals: List<FraudSignal>,
        extractions: Map<String, GiniCaptureSpecificExtraction>,
    ): IBANMismatchArgs? {
        val qrSignal = signals.firstOrNull { it.code == FraudSignalCode.QR_PRINTED_IBAN_MISMATCH }
        val countrySignal = signals.firstOrNull { it.code == FraudSignalCode.IBAN_COUNTRY_MISMATCH }
        // The parser guarantees creditorCountry for the country signal.
        val senderCountry = countrySignal?.creditorCountry?.uppercase()

        val printedIbans = qrSignal?.let { printedIbans(extractions, it.iban) }.orEmpty()
        val qrIban = qrSignal?.iban?.takeIf { printedIbans.isNotEmpty() }

        return when {
            qrIban != null && senderCountry != null -> IBANMismatchArgs(
                type = IBANMismatchType.COMBINED,
                qrIban = qrIban.toMismatchIban(senderCountry),
                printedIbans = printedIbans.map { it.toMismatchIban(senderCountry) },
                senderName = senderName(extractions),
                senderCountryCode = senderCountry,
            )

            qrIban != null -> IBANMismatchArgs(
                type = IBANMismatchType.QR_VS_PRINTED,
                qrIban = IBANMismatchIban(qrIban),
                printedIbans = printedIbans.map { IBANMismatchIban(it) },
            )

            countrySignal != null && senderCountry != null -> IBANMismatchArgs(
                type = IBANMismatchType.COUNTRY,
                qrIban = null,
                printedIbans = listOf(countrySignal.iban.toMismatchIban(senderCountry)),
                senderName = senderName(extractions),
                senderCountryCode = senderCountry,
            )

            else -> null
        }
    }

    private fun printedIbans(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
        qrIban: String,
    ): List<String> {
        val qrKey = qrIban.normalized()
        return extractions[IBAN_KEY]?.candidates.orEmpty()
            .mapNotNull { it.value?.trim()?.takeIf(String::isNotEmpty) }
            .distinctBy { it.normalized() }
            .filter { it.normalized() != qrKey }
    }

    private fun senderName(extractions: Map<String, GiniCaptureSpecificExtraction>): String? =
        extractions[PAYMENT_RECIPIENT_KEY]?.value?.trim()?.takeIf { it.isNotEmpty() }

    /** Sets the hint when the IBAN's country is known and differs from [senderCountry]. */
    private fun String.toMismatchIban(senderCountry: String): IBANMismatchIban {
        val ibanCountry = normalized().take(COUNTRY_CODE_LENGTH)
            .takeIf { it.length == COUNTRY_CODE_LENGTH && it.all(Char::isLetter) }
        return IBANMismatchIban(
            iban = this,
            hintCountryCode = ibanCountry?.takeIf { it != senderCountry },
        )
    }

    private fun String.normalized(): String = filterNot(Char::isWhitespace).uppercase()
}
