package net.gini.android.capture.internal.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SPCParserTest {

    private lateinit var parser: SPCParser

    @Before
    fun setup() {
        parser = SPCParser()
    }

    // Valid SPC payload: 7 creditor fields (4-10), 7 empty UC fields (11-17),
    // amount (18), currency (19), 7 empty UD fields (20-26), reference (27-29), trailer (30)
    private fun spcPayload(
        iban: String = "CH5604835012345678009",
        creditorName: String = "Robert Schneider AG",
        amount: String = "100.00",
        currency: String = "CHF",
        referenceType: String = "QRR",
        reference: String = "210000000003139471430009017",
        message: String = "Invoice 1234",
        delimiter: String = "\n",
    ) = listOf(
        "SPC", "0200", "1",
        iban,
        "S", creditorName, "Rue du Lac", "1268", "2501", "Biel", "CH",
        "", "", "", "", "", "", "",   // Ultimate Creditor (7 empty lines)
        amount, currency,
        "", "", "", "", "", "", "",   // Ultimate Debtor (7 empty lines)
        referenceType, reference, message,
        "EPD", "",
    ).joinToString(delimiter)

    @Test
    fun `parses valid SPC with LF delimiter`() {
        val content = spcPayload()
        val result = parser.parse(content)

        assertThat(result).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.SPC, content,
                "Robert Schneider AG",
                "210000000003139471430009017 Invoice 1234",
                "CH5604835012345678009",
                null,
                "100.00:CHF",
            )
        )
    }

    @Test
    fun `parses valid SPC with CRLF delimiter`() {
        val content = spcPayload(delimiter = "\r\n")
        val result = parser.parse(content)

        assertThat(result.getIBAN()).isEqualTo("CH5604835012345678009")
        assertThat(result.getAmount()).isEqualTo("100.00:CHF")
    }

    @Test
    fun `uses message as reference when reference type is NON`() {
        val content = spcPayload(referenceType = "NON", reference = "")
        val result = parser.parse(content)

        assertThat(result.getPaymentReference()).isEqualTo("Invoice 1234")
    }

    @Test
    fun `returns empty amount when amount field is blank`() {
        val content = spcPayload(amount = "")
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEmpty()
    }

    @Test
    fun `defaults currency to CHF when field is missing`() {
        val content = spcPayload(amount = "50.00", currency = "")
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEqualTo("50.00:CHF")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for content without SPC header`() {
        parser.parse("BCD\n001\nSCT\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for invalid IBAN`() {
        parser.parse(spcPayload(iban = "INVALID_IBAN"))
    }
}
