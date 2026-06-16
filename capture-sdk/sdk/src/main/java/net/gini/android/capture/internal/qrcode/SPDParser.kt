package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.internal.iban.IBANValidator
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeAmount
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeCurrency

/**
 * QR Code parser for the Slovak Payment Descriptor (SPD) format.
 *
 * The payload uses asterisk-delimited key:value pairs, e.g.:
 * `SPD*1.0*ACC:SK6807200002891987426353*AM:100.00*CC:EUR*RN:Recipient Name*`
 */
internal class SPDParser : QRCodeParser<PaymentQRCodeData> {

    private val ibanValidator = IBANValidator()

    override fun parse(qrCodeContent: String): PaymentQRCodeData {
        val parts = qrCodeContent.split("*")

        if (parts.isEmpty() || parts[0] != HEADER) {
            throw IllegalArgumentException("QR code content does not conform to the SPD format.")
        }

        val iban = parts.extractValue(KEY_ACCOUNT)
        runCatching { ibanValidator.validate(iban) }.getOrElse {
            throw IllegalArgumentException("Invalid IBAN in SPD QR code. ${it.message}", it)
        }

        val rawAmount = parts.extractValue(KEY_AMOUNT)
        val currency = normalizeCurrency(parts.extractValue(KEY_CURRENCY)).ifEmpty { "EUR" }
        val amount = normalizeAmount(rawAmount, currency)
        val recipientName = parts.extractValue(KEY_RECIPIENT_NAME)
        val reference = buildReference(
            variableSymbol = parts.extractValue(KEY_VARIABLE_SYMBOL),
            message = parts.extractValue(KEY_MESSAGE),
        )

        return PaymentQRCodeData(
            PaymentQRCodeData.Format.SPD, qrCodeContent,
            recipientName, reference, iban, null, amount,
        )
    }

    private fun List<String>.extractValue(key: String): String {
        val prefix = "$key:"
        return firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix) ?: ""
    }

    private fun buildReference(variableSymbol: String, message: String): String = when {
        variableSymbol.isNotEmpty() && message.isNotEmpty() -> "$variableSymbol $message"
        variableSymbol.isNotEmpty() -> variableSymbol
        else -> message
    }

    private companion object {
        const val HEADER = "SPD"
        const val KEY_ACCOUNT = "ACC"
        const val KEY_AMOUNT = "AM"
        const val KEY_CURRENCY = "CC"
        const val KEY_RECIPIENT_NAME = "RN"
        const val KEY_MESSAGE = "MSG"
        const val KEY_VARIABLE_SYMBOL = "VS"
    }
}
