package net.gini.android.capture.internal.qrcode

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EPC06912ParserTest {

    private lateinit var parser: EPC069_12Parser

    @Before
    fun setup() {
        parser = EPC069_12Parser()
    }

    @Test
    fun `accepts LF as line delimiter`() {
        // Given
        val qrCodeContent = "BCD\n" +
                "001\n" +
                "2\n" +
                "SCT\n" +
                "BAWAATWW\n" +
                "Magistrat der Stadt Wien\n" +
                "AT736000000002386492\n" +
                "EUR58.99\n" +
                "\n" +
                "\n" +
                "Fuer Franz Mustermann"

        // When
        val paymentData = parser.parse(qrCodeContent)

        // Then
        Truth.assertThat(paymentData).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.EPC069_12,
                qrCodeContent,
                "Magistrat der Stadt Wien",
                "Fuer Franz Mustermann",
                "AT736000000002386492",
                "BAWAATWW",
                "58.99:EUR"
            )
        )
    }

    @Test
    fun `accepts CRLF as line delimiter`() {
        // Given
        val qrCodeContent = "BCD\r\n" +
                "001\r\n" +
                "2\r\n" +
                "SCT\r\n" +
                "BAWAATWW\r\n" +
                "Magistrat der Stadt Wien\r\n" +
                "AT736000000002386492\r\n" +
                "EUR58.99\r\n" +
                "\r\n" +
                "\r\n" +
                "Fuer Franz Mustermann"

        // When
        val paymentData = parser.parse(qrCodeContent)

        // Then
        Truth.assertThat(paymentData).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.EPC069_12,
                qrCodeContent,
                "Magistrat der Stadt Wien",
                "Fuer Franz Mustermann",
                "AT736000000002386492",
                "BAWAATWW",
                "58.99:EUR"
            )
        )
    }

    @Test
    fun `accepts CR as line delimiter`() {
        // Given
        val qrCodeContent = "BCD\r" +
                "001\r" +
                "2\r" +
                "SCT\r" +
                "BAWAATWW\r" +
                "Magistrat der Stadt Wien\r" +
                "AT736000000002386492\r" +
                "EUR58.99\r" +
                "\r" +
                "\r" +
                "Fuer Franz Mustermann"

        // When
        val paymentData = parser.parse(qrCodeContent)

        // Then
        Truth.assertThat(paymentData).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.EPC069_12,
                qrCodeContent,
                "Magistrat der Stadt Wien",
                "Fuer Franz Mustermann",
                "AT736000000002386492",
                "BAWAATWW",
                "58.99:EUR"
            )
        )
    }

    @Test
    fun `accepts CRCRLF as line delimiter`() {
        // Given
        val parser = EPC069_12Parser()
        val qrCodeContent = "BCD\r\n" +
                "001\r\n" +
                "2\r\n" +
                "SCT\r\n" +
                "BAWAATWW\r\r\n" +
                "Magistrat der Stadt Wien\r\r\n" +
                "AT736000000002386492\r\r\n" +
                "EUR58.99\r\n" +
                "\r\n" +
                "3372/12 RgNr.: 2201207\r\n"

        // When
        val paymentData = parser.parse(qrCodeContent)

        // Then
        Truth.assertThat(paymentData).isEqualTo(
            PaymentQRCodeData(
                PaymentQRCodeData.Format.EPC069_12,
                qrCodeContent,
                "Magistrat der Stadt Wien",
                "3372/12 RgNr.: 2201207 ",
                "AT736000000002386492",
                "BAWAATWW",
                "58.99:EUR"
            )
        )
    }
}