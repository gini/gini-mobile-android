package net.gini.android.capture.internal.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UPNQRParserTest {

    private lateinit var parser: UPNQRParser

    @Before
    fun setup() {
        parser = UPNQRParser()
    }

    // Field order:
    // 0:UPNQR, 1:payerIBAN, 2:deposit, 3:withdrawal, 4:payerRef,
    // 5:payerName, 6:payerStreet, 7:payerCity, 8:amount(cents),
    // 9:date, 10:urgent, 11:purposeCode, 12:paymentRef, 13:swift/BIC,
    // 14:recipientIBAN, 15:recipientRef, 16:recipientName,
    // 17:recipientStreet, 18:recipientCity, 19:checksum
    private fun upnqrPayload(
        recipientIban: String = "SI56020170014356205",
        recipientName: String = "Janez Novak",
        amountCents: String = "0000010000",
        paymentRef: String = "SI00RI-123-456",
        payerRef: String = "",
    ) = listOf(
        "UPNQR",
        "", "", "",
        payerRef,
        "Marko Kranjc", "Slovenčeva 22", "1000 Ljubljana",
        amountCents, "01012024", "", "GDSV",
        paymentRef,
        "",             // 13: SWIFT/BIC of recipient bank
        recipientIban,  // 14
        "",             // 15: recipient reference
        recipientName,  // 16
        "Rožna dolina 5", "1000 Ljubljana",
        "474",
    ).joinToString("\n")

    @Test
    fun `parses valid UPNQR with full fields`() {
        val content = upnqrPayload()
        val result = parser.parse(content)

        assertThat(result).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.UPNQR, content,
                "Janez Novak",
                "SI00RI-123-456",
                "SI56020170014356205",
                null,
                "100.00:EUR",
            )
        )
    }

    @Test
    fun `converts amount from euro-cents correctly`() {
        val content = upnqrPayload(amountCents = "0000000599")
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEqualTo("5.99:EUR")
    }

    @Test
    fun `falls back to payer reference when payment reference is empty`() {
        val content = upnqrPayload(paymentRef = "", payerRef = "SI01-123")
        val result = parser.parse(content)

        assertThat(result.getPaymentReference()).isEqualTo("SI01-123")
    }

    @Test
    fun `returns empty amount when amount field is blank`() {
        val content = upnqrPayload(amountCents = "")
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEmpty()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for content without UPNQR header`() {
        parser.parse("SPC\n0200\n1\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when line count is below minimum`() {
        parser.parse("UPNQR\n\n\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for invalid IBAN`() {
        parser.parse(upnqrPayload(recipientIban = "NOT_AN_IBAN"))
    }
}
