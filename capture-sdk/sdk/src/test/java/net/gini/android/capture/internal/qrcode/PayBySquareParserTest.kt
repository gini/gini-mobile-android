package net.gini.android.capture.internal.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.LZMAOutputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class PayBySquareParserTest {

    private lateinit var parser: PayBySquareParser

    @Before
    fun setup() {
        parser = PayBySquareParser()
    }

    /**
     * Builds a real bysquare-compatible QR code string for testing.
     *
     * Binary structure produced (per the bysquare spec):
     *   [2-byte bysquare header] [2-byte payload length] [raw LZMA body (header stripped)]
     *
     * Uncompressed content (before LZMA):
     *   [4-byte CRC32 placeholder] [TAB-separated fields]
     *
     * TAB-separated field layout for a single payment with one bank account:
     *   0:invoiceId, 1:paymentsCount, 2:paymentType, 3:amount, 4:currency,
     *   5:dueDate, 6:variableSymbol, 7:constantSymbol, 8:specificSymbol,
     *   9:originatorRef, 10:paymentNote, 11:bankAccountsCount,
     *   12:IBAN, 13:BIC, 14:standingOrderExt, 15:directDebitExt,
     *   16:beneficiaryName, 17:beneficiaryStreet, 18:beneficiaryCity
     */
    private fun buildPayload(
        amount: String = "100.00",
        currency: String = "EUR",
        iban: String = "SK6807200002891987426353",
        bic: String = "TATRSKBX",
        beneficiaryName: String = "Jan Novak",
        variableSymbol: String = "9876",
        paymentNote: String = "Invoice",
    ): String {
        val fields = listOf(
            "",             // 0: invoiceId
            "1",            // 1: paymentsCount
            "1",            // 2: payment type (1 = regular payment)
            amount,         // 3: amount
            currency,       // 4: currencyCode
            "",             // 5: paymentDueDate
            variableSymbol, // 6: variableSymbol
            "",             // 7: constantSymbol
            "",             // 8: specificSymbol
            "",             // 9: originatorsReferenceInformation
            paymentNote,    // 10: paymentNote
            "1",            // 11: bankAccountsCount
            iban,           // 12: IBAN
            bic,            // 13: BIC
            "0",            // 14: standingOrderExt
            "0",            // 15: directDebitExt
            beneficiaryName, // 16: beneficiaryName
            "",             // 17: beneficiaryStreet
            "",             // 18: beneficiaryCity
        )
        val tabSeparated = fields.joinToString("\t")

        // Prepend 4-byte CRC32 placeholder (zeros) then LZMA-compress the whole thing
        val uncompressed = ByteArray(4) + tabSeparated.toByteArray(Charsets.UTF_8)
        val compressed = lzmaCompress(uncompressed)

        // Strip the 13-byte LZMA "alone" header (the bysquare encoder always strips it)
        val lzmaBody = compressed.copyOfRange(13, compressed.size)
        val payloadLength = uncompressed.size

        // Build the bysquare binary: [header] [payload length] [lzma body]
        val raw = byteArrayOf(0x00, 0x00) +
                byteArrayOf(
                    (payloadLength and 0xFF).toByte(),
                    ((payloadLength shr 8) and 0xFF).toByte(),
                ) +
                lzmaBody

        return encodeBase32(raw)
    }

    private fun lzmaCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val options = LZMA2Options().apply { dictSize = 131072 }
        LZMAOutputStream(bos, options, data.size.toLong()).use { it.write(data) }
        return bos.toByteArray()
    }

    /**
     * Encodes raw bytes into a bysquare base32hex string.
     * Each output character encodes 5 bits (MSB first), using the alphabet 0–9A–V.
     */
    private fun encodeBase32(bytes: ByteArray): String {
        val alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
        val totalBits = bytes.size * 8
        val numChars = (totalBits + 4) / 5
        val sb = StringBuilder(numChars)
        for (charIndex in 0 until numChars) {
            val bitStart = charIndex * 5
            var value = 0
            for (i in 0..4) {
                val bitPos = bitStart + i
                if (bitPos < totalBits) {
                    val byteIdx = bitPos / 8
                    val bitInByte = 7 - (bitPos % 8)
                    if ((bytes[byteIdx].toInt() shr bitInByte) and 1 == 1) {
                        value = value or (1 shl (4 - i))
                    }
                }
            }
            sb.append(alphabet[value])
        }
        return sb.toString()
    }

    @Test
    fun `parses valid PayBySquare with basic payment`() {
        val content = buildPayload()
        val result = parser.parse(content)

        assertThat(result.getFormat()).isEqualTo(PaymentQRCodeData.Format.PAY_BY_SQUARE)
        assertThat(result.getIBAN()).isEqualTo("SK6807200002891987426353")
        assertThat(result.getAmount()).isEqualTo("100.00:EUR")
        assertThat(result.getPaymentRecipient()).isEqualTo("Jan Novak")
        assertThat(result.getPaymentReference()).isEqualTo("9876 Invoice")
    }

    @Test
    fun `uses only variable symbol as reference when payment note is absent`() {
        val content = buildPayload(variableSymbol = "12345", paymentNote = "")
        val result = parser.parse(content)

        assertThat(result.getPaymentReference()).isEqualTo("12345")
    }

    @Test
    fun `uses only payment note as reference when variable symbol is absent`() {
        val content = buildPayload(variableSymbol = "", paymentNote = "Transfer")
        val result = parser.parse(content)

        assertThat(result.getPaymentReference()).isEqualTo("Transfer")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for content that is too short`() {
        // 5 chars × 5 bits = 25 bits → 4 bytes = exactly HEADER_SIZE, triggers "too short"
        parser.parse("00000")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for non-PAY document type in header`() {
        // Upper nibble of byte 0 = 1 (non-PAY type); rest is arbitrary valid structure
        val tabPayload = "\t1\t1\t100.00\tEUR\t\t\t\t\t\t\t1\tSK6807200002891987426353\t\t0\t0\tTest\t\t"
        val uncompressed = ByteArray(4) + tabPayload.toByteArray(Charsets.UTF_8)
        val compressed = lzmaCompress(uncompressed)
        val lzmaBody = compressed.copyOfRange(13, compressed.size)
        val payloadLength = uncompressed.size
        val raw = byteArrayOf(0x10.toByte(), 0x00) +  // type = 1 (non-PAY)
                byteArrayOf(
                    (payloadLength and 0xFF).toByte(),
                    ((payloadLength shr 8) and 0xFF).toByte(),
                ) +
                lzmaBody
        parser.parse(encodeBase32(raw))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when LZMA decompression fails`() {
        // Valid bysquare header + payload length, but garbage LZMA body
        val raw = byteArrayOf(0x00, 0x00, 0x10, 0x00) +
                ByteArray(30) { (it + 1).toByte() }
        parser.parse(encodeBase32(raw))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for invalid IBAN in payload`() {
        parser.parse(buildPayload(iban = "INVALID"))
    }
}
