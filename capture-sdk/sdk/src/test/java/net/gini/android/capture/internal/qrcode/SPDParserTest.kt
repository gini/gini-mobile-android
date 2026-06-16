package net.gini.android.capture.internal.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SPDParserTest {

    private lateinit var parser: SPDParser

    @Before
    fun setup() {
        parser = SPDParser()
    }

    private fun spdPayload(
        iban: String = "SK6807200002891987426353",
        amount: String = "100.00",
        currency: String = "EUR",
        recipientName: String = "Jan Novak",
        variableSymbol: String = "1234567890",
        message: String = "Invoice payment",
    ) = "SPD*1.0*ACC:$iban*AM:$amount*CC:$currency*RN:$recipientName*VS:$variableSymbol*MSG:$message*"

    @Test
    fun `parses valid SPD with all fields`() {
        val content = spdPayload()
        val result = parser.parse(content)

        assertThat(result).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.SPD, content,
                "Jan Novak",
                "1234567890 Invoice payment",
                "SK6807200002891987426353",
                null,
                "100.00:EUR",
            )
        )
    }

    @Test
    fun `uses only variable symbol as reference when message is absent`() {
        val content = "SPD*1.0*ACC:SK6807200002891987426353*AM:50.00*CC:EUR*VS:9876*"
        val result = parser.parse(content)

        assertThat(result.getPaymentReference()).isEqualTo("9876")
    }

    @Test
    fun `uses only message as reference when variable symbol is absent`() {
        val content = "SPD*1.0*ACC:SK6807200002891987426353*AM:50.00*CC:EUR*MSG:Transfer*"
        val result = parser.parse(content)

        assertThat(result.getPaymentReference()).isEqualTo("Transfer")
    }

    @Test
    fun `defaults currency to EUR when field is absent`() {
        val content = "SPD*1.0*ACC:SK6807200002891987426353*AM:75.00*"
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEqualTo("75.00:EUR")
    }

    @Test
    fun `returns empty amount when AM field is absent`() {
        val content = "SPD*1.0*ACC:SK6807200002891987426353*CC:EUR*"
        val result = parser.parse(content)

        assertThat(result.getAmount()).isEmpty()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for content without SPD header`() {
        parser.parse("BCD\n001\nSCT\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws for invalid IBAN`() {
        parser.parse("SPD*1.0*ACC:NOT_AN_IBAN*AM:10.00*CC:EUR*")
    }
}
