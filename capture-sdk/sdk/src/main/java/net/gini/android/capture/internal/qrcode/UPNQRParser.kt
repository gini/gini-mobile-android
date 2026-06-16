package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.internal.iban.IBANValidator
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeAmount

/**
 * QR Code parser for the Slovenian Universal Payment Note (UPNQR) format.
 *
 * The payload is newline-delimited with fixed field positions. The amount field encodes
 * the value in euro-cents as a zero-padded 10-digit string (e.g. "0000010000" = €100.00).
 */
internal class UPNQRParser : QRCodeParser<PaymentQRCodeData> {

    private val ibanValidator = IBANValidator()

    override fun parse(qrCodeContent: String): PaymentQRCodeData {
        val lines = qrCodeContent.replace(Regex("\r\r?\n"), "\n").split(Regex("\n|\r"), 0)

        if (lines.size < MINIMUM_LINE_COUNT || lines[0] != HEADER) {
            throw IllegalArgumentException(
                "QR code content does not conform to the UPNQR format."
            )
        }

        val iban = lines.getOrEmpty(IDX_RECIPIENT_IBAN)
        runCatching { ibanValidator.validate(iban) }.getOrElse {
            throw IllegalArgumentException("Invalid IBAN in UPNQR QR code. ${it.message}", it)
        }

        val recipientName = lines.getOrEmpty(IDX_RECIPIENT_NAME)
        val amount = parseAmount(lines.getOrEmpty(IDX_AMOUNT))
        val reference = lines.getOrEmpty(IDX_PAYMENT_REFERENCE).ifEmpty {
            lines.getOrEmpty(IDX_PAYER_REFERENCE)
        }

        return PaymentQRCodeData(
            PaymentQRCodeData.Format.UPNQR, qrCodeContent,
            recipientName, reference, iban, null, amount,
        )
    }

    private fun parseAmount(rawAmount: String): String {
        if (rawAmount.isBlank()) return ""
        return runCatching {
            val cents = rawAmount.trim().toLong()
            val euros = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
            normalizeAmount(euros, "EUR")
        }.getOrDefault("")
    }

    private fun List<String>.getOrEmpty(index: Int) = getOrElse(index) { "" }

    private companion object {
        const val HEADER = "UPNQR"
        const val MINIMUM_LINE_COUNT = 19

        const val IDX_PAYER_REFERENCE = 4
        const val IDX_AMOUNT = 8
        const val IDX_PAYMENT_REFERENCE = 12
        const val IDX_RECIPIENT_IBAN = 13
        const val IDX_RECIPIENT_NAME = 15
    }
}
