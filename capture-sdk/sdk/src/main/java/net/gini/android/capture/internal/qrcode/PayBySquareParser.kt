package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.internal.iban.IBANValidator
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeAmount
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeCurrency
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * QR Code parser for the Pay by Square format used in Slovakia and Czech Republic.
 *
 * The QR code carries binary data: a 2-byte bysquare header followed by an LZMA-compressed,
 * TAB-separated payload. ML Kit returns the raw bytes decoded as a Latin-1 string, so we
 * recover the bytes via ISO-8859-1 before decompressing.
 *
 * Payload field layout (0-indexed, TAB-separated after decompression):
 * ```
 *  0  – PaymentOptions
 *  1  – Amount
 *  2  – CurrencyCode
 *  3  – PaymentDueDate
 *  4  – VariableSymbol
 *  5  – ConstantSymbol
 *  6  – SpecificSymbol
 *  7  – OriginatorReferenceInfo
 *  8  – PaymentNote
 *  9  – BankAccountsCount
 *  10 – IBAN (first bank)
 *  11 – BIC  (first bank)
 *  12 + N*2 – BeneficiaryName  (N = BankAccountsCount)
 * ```
 */
internal class PayBySquareParser : QRCodeParser<PaymentQRCodeData> {

    private val ibanValidator = IBANValidator()

    override fun parse(qrCodeContent: String): PaymentQRCodeData {
        // ML Kit returns binary QR data as a Latin-1 string; convert back to raw bytes
        val rawBytes = qrCodeContent.toByteArray(StandardCharsets.ISO_8859_1)

        if (rawBytes.size <= HEADER_SIZE) {
            throw IllegalArgumentException(
                "QR code content is too short to be a PayBySquare code."
            )
        }

        // Upper nibble of byte 0 is the document type; PAY = 0
        val documentType = (rawBytes[0].toInt() and 0xFF) shr 4
        if (documentType != DOCUMENT_TYPE_PAY) {
            throw IllegalArgumentException(
                "QR code content is not a PayBySquare PAY document."
            )
        }

        val lzmaData = rawBytes.copyOfRange(HEADER_SIZE, rawBytes.size)
        val decompressed = decompress(lzmaData)
        return parseFields(qrCodeContent, decompressed)
    }

    private fun decompress(lzmaData: ByteArray): String {
        try {
            ByteArrayInputStream(lzmaData).use { bis ->
                LZMACompressorInputStream(bis).use { lzmaStream ->
                    ByteArrayOutputStream().use { bos ->
                        val buffer = ByteArray(512)
                        var read: Int
                        while (lzmaStream.read(buffer).also { read = it } != -1) {
                            bos.write(buffer, 0, read)
                        }
                        return bos.toString("UTF-8")
                    }
                }
            }
        } catch (e: IOException) {
            throw IllegalArgumentException(
                "QR code content could not be decompressed; not a PayBySquare code.", e
            )
        }
    }

    private fun parseFields(rawContent: String, decompressed: String): PaymentQRCodeData {
        val fields = decompressed.split("\t")

        if (fields.size <= IDX_BANKS_COUNT) {
            throw IllegalArgumentException(
                "Decompressed PayBySquare payload has too few fields."
            )
        }

        val rawAmount = fields.getOrEmpty(IDX_AMOUNT)
        val currency = normalizeCurrency(fields.getOrEmpty(IDX_CURRENCY)).ifEmpty { "EUR" }
        val amount = normalizeAmount(rawAmount, currency)

        val banksCount = fields.getOrEmpty(IDX_BANKS_COUNT).trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
        val iban = fields.getOrEmpty(IDX_FIRST_IBAN)
        runCatching { ibanValidator.validate(iban) }.getOrElse {
            throw IllegalArgumentException(
                "Invalid IBAN in PayBySquare QR code. ${it.message}", it
            )
        }

        // Beneficiary name follows all bank account pairs (IBAN + BIC per bank)
        val beneficiaryName = fields.getOrEmpty(IDX_FIRST_IBAN + banksCount * FIELDS_PER_BANK)

        val reference = buildReference(
            variableSymbol = fields.getOrEmpty(IDX_VARIABLE_SYMBOL),
            paymentNote = fields.getOrEmpty(IDX_PAYMENT_NOTE),
        )

        return PaymentQRCodeData(
            PaymentQRCodeData.Format.PAY_BY_SQUARE, rawContent,
            beneficiaryName, reference, iban, null, amount,
        )
    }

    private fun buildReference(variableSymbol: String, paymentNote: String): String = when {
        variableSymbol.isNotEmpty() && paymentNote.isNotEmpty() -> "$variableSymbol $paymentNote"
        variableSymbol.isNotEmpty() -> variableSymbol
        else -> paymentNote
    }

    private fun List<String>.getOrEmpty(index: Int) = getOrElse(index) { "" }

    private companion object {
        const val DOCUMENT_TYPE_PAY = 0
        const val HEADER_SIZE = 2
        const val FIELDS_PER_BANK = 2

        const val IDX_AMOUNT = 1
        const val IDX_CURRENCY = 2
        const val IDX_VARIABLE_SYMBOL = 4
        const val IDX_PAYMENT_NOTE = 8
        const val IDX_BANKS_COUNT = 9
        const val IDX_FIRST_IBAN = 10
    }
}
