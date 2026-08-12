package net.gini.android.capture.internal.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HUB3ParserTest {

    private lateinit var parser: HUB3Parser

    @Before
    fun setup() {
        parser = HUB3Parser()
    }

    // Field order:
    // 0:HRVHUB30, 1:currency, 2:amount(cents), 3:payerName, 4:payerStreet,
    // 5:payerCity, 6:recipientName, 7:recipientStreet, 8:recipientCity,
    // 9:recipientIBAN, 10:model, 11:callNumber, 12:intentCode, 13:description
    private fun hub3Payload(
        currency: String = "EUR",
        amountCents: String = "000000010000",
        recipientName: String = "Primatelj d.o.o.",
        recipientIban: String = "HR1723600001101234565",
        callNumber: String = "HR99 123-456",
    ) = listOf(
        "HRVHUB30",
        currency,
        amountCents,
        "Platitelj Marko", "Ilica 1", "10000 Zagreb",
        recipientName,
        "Vukovarska 2", "10000 Zagreb",
        recipientIban,
        "HR99",
        callNumber,
        "COST",
        "Uplata po računu",
    ).joinToString("\n")

    @Test
    fun `parses valid HUB3 with full fields`() {
        val content = hub3Payload()
        val result = parser.parse(content)

        assertThat(result).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.HUB3, content,
                "Primatelj d.o.o.",
                "HR99 123-456",
                "HR1723600001101234565",
                null,
                "100.00:EUR",
            )
        )
    }

    @Test
    fun `converts amount from currency-cents correctly`() {
        val content = hub3Payload(amountCents = "000000000599")
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEqualTo("5.99:EUR")
    }

    @Test
    fun `defaults currency to EUR when field is empty`() {
        val content = hub3Payload(currency = "")
        val result = parser.parse(content)

        assertThat(result.getAmount()).endsWith(":EUR")
    }

    @Test
    fun `returns empty amount when amount field is blank`() {
        val content = hub3Payload(amountCents = "")
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEmpty()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for content without HRVHUB30 header`() {
        parser.parse("UPNQR\n\nEUR\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when line count is below minimum`() {
        parser.parse("HRVHUB30\nEUR\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for invalid IBAN`() {
        parser.parse(hub3Payload(recipientIban = "NOT_AN_IBAN"))
    }
}
