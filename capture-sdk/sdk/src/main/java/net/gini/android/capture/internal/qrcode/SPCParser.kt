package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.internal.iban.IBANValidator
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeAmount
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeCurrency

/**
 * QR Code parser for the Swiss Payment Code (SPC) format.
 *
 * See the [Swiss Payment Standards specification](https://www.paymentstandards.ch/dam/downloads/ig-qr-bill-en.pdf)
 * for details.
 */
internal class SPCParser : QRCodeParser<PaymentQRCodeData> {

    private val ibanValidator = IBANValidator()

    override fun parse(qrCodeContent: String): PaymentQRCodeData {
        val lines = qrCodeContent.replace(Regex("\r\r?\n"), "\n").split(Regex("\n|\r"), 0)

        require(lines.isNotEmpty() && lines[0] == HEADER) {
            "QR code content does not conform to the SPC format."
        }

        val iban = lines.getOrEmpty(IDX_IBAN)
        runCatching { ibanValidator.validate(iban) }.getOrElse {
            throw IllegalArgumentException("Invalid IBAN in SPC QR code. ${it.message}", it)
        }

        val creditorName = lines.getOrEmpty(IDX_CREDITOR_NAME)
        val rawAmount = lines.getOrEmpty(IDX_AMOUNT)
        val currency = normalizeCurrency(lines.getOrEmpty(IDX_CURRENCY)).ifEmpty { "CHF" }
        val amount = normalizeAmount(rawAmount, currency)
        val reference = buildReference(
            referenceType = lines.getOrEmpty(IDX_REFERENCE_TYPE),
            reference = lines.getOrEmpty(IDX_REFERENCE),
            message = lines.getOrEmpty(IDX_MESSAGE),
        )

        return PaymentQRCodeData(
            PaymentQRCodeData.Format.SPC, qrCodeContent,
            creditorName, reference, iban, null, amount,
        )
    }

    private fun buildReference(referenceType: String, reference: String, message: String): String {
        // NON means no structured reference; fall back to unstructured message
        if (referenceType == "NON" || reference.isEmpty()) return message
        return if (message.isNotEmpty()) "$reference $message" else reference
    }

    private fun List<String>.getOrEmpty(index: Int) = getOrElse(index) { "" }

    private companion object {
        const val HEADER = "SPC"

        const val IDX_IBAN = 3
        const val IDX_CREDITOR_NAME = 5
        // Lines 4-10: Creditor (7 fields), lines 11-17: Ultimate Creditor (7 fields, usually empty)
        const val IDX_AMOUNT = 18
        const val IDX_CURRENCY = 19
        // Lines 20-26: Ultimate Debtor (7 fields, usually empty)
        const val IDX_REFERENCE_TYPE = 27
        const val IDX_REFERENCE = 28
        const val IDX_MESSAGE = 29
    }
}
