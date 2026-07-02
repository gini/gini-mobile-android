package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.internal.iban.IBANValidator
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeAmount
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeCurrency

/**
 * Barcode parser for the Croatian HUB3 payment standard, which is encoded as a PDF417 barcode.
 *
 * The payload is newline-delimited with fixed field positions. The amount field encodes the value
 * in currency-cents as a zero-padded string (e.g. "000000010000" = 100.00).
 */
@Suppress("MagicNumber", "UseRequire")
internal class HUB3Parser : QRCodeParser<PaymentQRCodeData> {

    private val ibanValidator = IBANValidator()

    override fun parse(qrCodeContent: String): PaymentQRCodeData {
        val lines = qrCodeContent.replace(Regex("\r\r?\n"), "\n").split(Regex("\n|\r"), 0)

        if (lines.size < MINIMUM_LINE_COUNT || lines[0] != HEADER) {
            throw IllegalArgumentException(
                "Barcode content does not conform to the HUB3 format."
            )
        }

        val iban = lines.getOrEmpty(IDX_RECIPIENT_IBAN)
        runCatching { ibanValidator.validate(iban) }.getOrElse {
            throw IllegalArgumentException("Invalid IBAN in HUB3 barcode. ${it.message}", it)
        }

        val currency = normalizeCurrency(lines.getOrEmpty(IDX_CURRENCY)).ifEmpty { "EUR" }
        val amount = parseAmount(lines.getOrEmpty(IDX_AMOUNT), currency)
        val recipientName = lines.getOrEmpty(IDX_RECIPIENT_NAME)
        val reference = lines.getOrEmpty(IDX_CALL_NUMBER)

        return PaymentQRCodeData(
            PaymentQRCodeData.Format.HUB3, qrCodeContent,
            recipientName, reference, iban, null, amount,
        )
    }

    private fun parseAmount(rawAmount: String, currency: String): String {
        if (rawAmount.isBlank()) return ""
        return runCatching {
            val cents = rawAmount.trim().toLong()
            val decimal = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
            normalizeAmount(decimal, currency)
        }.getOrDefault("")
    }

    private fun List<String>.getOrEmpty(index: Int) = getOrElse(index) { "" }

    private companion object {
        const val HEADER = "HRVHUB30"
        const val MINIMUM_LINE_COUNT = 12

        const val IDX_CURRENCY = 1
        const val IDX_AMOUNT = 2
        const val IDX_RECIPIENT_NAME = 6
        const val IDX_RECIPIENT_IBAN = 9
        const val IDX_CALL_NUMBER = 11
    }
}
