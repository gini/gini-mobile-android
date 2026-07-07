package net.gini.android.capture.internal.qrcode

import net.gini.android.capture.internal.iban.IBANValidator
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeAmount
import net.gini.android.capture.internal.qrcode.AmountAndCurrencyNormalizer.normalizeCurrency
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * QR Code parser for the Pay by Square format used in Slovakia and Czech Republic.
 *
 * The QR code carries an alphanumeric string encoded with the bysquare custom base32hex alphabet
 * (characters `0–9` and `A–V`, each representing 5 bits). After decoding the string to bytes the
 * binary layout is:
 *
 * ```
 * Bytes 0-1: bysquare header  (nibbles: BySquareType | Version | DocType | Reserved)
 * Bytes 2-3: payload length   (little-endian uint16 = uncompressed size of CRC32 + tab-data)
 * Bytes 4+:  raw LZMA body    (standard 13-byte LZMA "alone" header is stripped by the encoder)
 * ```
 *
 * To decompress, the 13-byte LZMA "alone" header is reconstructed with the fixed properties
 * that bysquare encoders always use:
 *   - Properties byte 0x5D  (lc=3, lp=0, pb=2)
 *   - Dictionary size       131 072 bytes (= 2^17, little-endian: 00 00 02 00)
 *   - Uncompressed size     taken from the payload-length field (bytes 2-3)
 *
 * After decompression the first 4 bytes are the CRC32 checksum (little-endian). The remainder
 * is a TAB-separated payload whose fields (0-indexed) for a single-payment QR code are:
 *
 * ```
 *  0  – invoiceId            (usually empty)
 *  1  – paymentsCount        (usually "1")
 *  2  – PaymentType
 *  3  – Amount
 *  4  – CurrencyCode
 *  5  – PaymentDueDate
 *  6  – VariableSymbol
 *  7  – ConstantSymbol
 *  8  – SpecificSymbol
 *  9  – OriginatorReferenceInfo
 *  10 – PaymentNote
 *  11 – BankAccountsCount
 *  12 – IBAN (first bank)
 *  13 – BIC  (first bank)
 *  …  additional banks (2 fields each)
 *  12 + N*2     – StandingOrderExtension ("0" or "1")
 *  12 + N*2 + 1 – DirectDebitExtension  ("0" or "1")
 *  12 + N*2 + 2 – BeneficiaryName
 * ```
 * where N = BankAccountsCount.
 */
@Suppress("MagicNumber", "NestedBlockDepth")
internal class PayBySquareParser : QRCodeParser<PaymentQRCodeData> {

    private val ibanValidator = IBANValidator()

    override fun parse(qrCodeContent: String): PaymentQRCodeData {
        val upper = qrCodeContent.uppercase()

        require(upper.all { it in ALPHABET_SET }) {
            "QR code content contains characters outside the Pay by Square alphabet."
        }

        val rawBytes = decodeBase32(upper)

        require(rawBytes.size > HEADER_SIZE) {
            "QR code content is too short to be a PayBySquare code."
        }

        // Upper nibble of byte 0 = bysquare type; PAY = 0
        val bysquareType = (rawBytes[0].toInt() and 0xFF) shr 4
        require(bysquareType == BYSQUARE_TYPE_PAY) {
            "QR code content is not a PayBySquare PAY document."
        }

        // Bytes 2-3: payload length (little-endian uint16) used as the LZMA uncompressed size
        val payloadLength = (rawBytes[2].toInt() and 0xFF) or
                ((rawBytes[3].toInt() and 0xFF) shl 8)

        // Bytes 4+: raw LZMA body (no standard LZMA "alone" header)
        val lzmaBody = rawBytes.copyOfRange(HEADER_SIZE, rawBytes.size)
        val tabPayload = decompress(lzmaBody, payloadLength)
        return parseFields(qrCodeContent, tabPayload)
    }

    /**
     * Decodes a bysquare base32hex string into raw bytes.
     * Each character encodes 5 bits (MSB first); bits are packed into bytes MSB first.
     */
    private fun decodeBase32(encoded: String): ByteArray {
        val totalBits = encoded.length * 5
        val bytes = ByteArray((totalBits + 7) / 8)
        var bitIndex = 0
        for (ch in encoded) {
            val value = ALPHABET.indexOf(ch)
            require(value >= 0) { "Invalid character in Pay by Square code: '$ch'" }
            for (bit in 4 downTo 0) {
                val byteIdx = bitIndex / 8
                val bitInByte = 7 - (bitIndex % 8)
                if ((value shr bit) and 1 == 1) {
                    bytes[byteIdx] = (bytes[byteIdx].toInt() or (1 shl bitInByte)).toByte()
                }
                bitIndex++
            }
        }
        return bytes
    }

    /**
     * Prepends the fixed 13-byte LZMA "alone" header to [lzmaBody] and decompresses.
     * Skips the 4-byte CRC32 checksum at the start of the decompressed output.
     */
    private fun decompress(lzmaBody: ByteArray, uncompressedSize: Int): String {
        // Reconstruct the 13-byte LZMA "alone" header:
        //   1 byte  properties   (0x5D = lc=3, lp=0, pb=2)
        //   4 bytes dictionary   (little-endian 131072 = 0x00020000)
        //   8 bytes uncompressed size (little-endian)
        val lzmaHeader = byteArrayOf(LZMA_PROPERTIES) +
                LZMA_DICT_SIZE +
                byteArrayOf(
                    (uncompressedSize and 0xFF).toByte(),
                    ((uncompressedSize shr 8) and 0xFF).toByte(),
                    0, 0, 0, 0, 0, 0,
                )

        try {
            ByteArrayInputStream(lzmaHeader + lzmaBody).use { bis ->
                LZMACompressorInputStream(bis).use { lzmaIn ->
                    ByteArrayOutputStream().use { bos ->
                        val buffer = ByteArray(512)
                        var read: Int
                        while (lzmaIn.read(buffer).also { read = it } != -1) {
                            bos.write(buffer, 0, read)
                        }
                        val decompressed = bos.toByteArray()
                        require(decompressed.size >= CRC_SIZE) {
                            "Decompressed PayBySquare payload is too short to contain a CRC32."
                        }
                        // Skip 4-byte CRC32; return the tab-separated payload
                        return String(decompressed, CRC_SIZE, decompressed.size - CRC_SIZE, Charsets.UTF_8)
                    }
                }
            }
        } catch (e: IOException) {
            throw IllegalArgumentException(
                "QR code content could not be decompressed; not a PayBySquare code.", e
            )
        }
    }

    private fun parseFields(rawContent: String, payload: String): PaymentQRCodeData {
        val fields = payload.split("\t")

        require(fields.size > IDX_FIRST_IBAN) {
            "Decompressed PayBySquare payload has too few fields."
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

        // After all bank account pairs: 2 extension-flag fields (standing order, direct debit)
        // then beneficiary name
        val beneficiaryIdx = IDX_FIRST_IBAN + banksCount * FIELDS_PER_BANK + EXTENSION_FIELDS
        require(beneficiaryIdx < fields.size) {
            "Malformed PayBySquare payload: declared bank account count ($banksCount) exceeds actual fields."
        }
        val beneficiaryName = fields.getOrEmpty(beneficiaryIdx)

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
        const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
        val ALPHABET_SET = ALPHABET.toSet()

        const val BYSQUARE_TYPE_PAY = 0
        // 2-byte bysquare header + 2-byte payload length; raw LZMA body starts at byte 4
        const val HEADER_SIZE = 4
        // CRC32 prepended to decompressed data by the bysquare encoder
        const val CRC_SIZE = 4
        const val FIELDS_PER_BANK = 2
        // Two extension flags between bank accounts and beneficiary block
        const val EXTENSION_FIELDS = 2

        // Fixed LZMA compression parameters used by all bysquare encoders
        const val LZMA_PROPERTIES = 0x5D.toByte() // lc=3, lp=0, pb=2
        val LZMA_DICT_SIZE = byteArrayOf(0x00, 0x00, 0x02, 0x00) // 131072 bytes little-endian

        // Field indices in the TAB-separated payload (after skipping the 4-byte CRC32)
        const val IDX_AMOUNT = 3
        const val IDX_CURRENCY = 4
        const val IDX_VARIABLE_SYMBOL = 6
        const val IDX_PAYMENT_NOTE = 10
        const val IDX_BANKS_COUNT = 11
        const val IDX_FIRST_IBAN = 12
    }
}
