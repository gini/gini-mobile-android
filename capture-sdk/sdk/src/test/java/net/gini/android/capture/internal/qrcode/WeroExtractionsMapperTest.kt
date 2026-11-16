package net.gini.android.capture.internal.qrcode

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.qrcode.WeroExtractionsMapper.PAYMENT_QR_CODE_TYPE_KEY
import net.gini.android.capture.internal.qrcode.WeroExtractionsMapper.WERO_PAYMENT_QR_CODE_TYPE_VALUE
import net.gini.android.capture.internal.qrcode.WeroExtractionsMapper.WERO_PAYMENT_QR_CODE_URL_KEY
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

class WeroExtractionsMapperTest {

    private val weroUrl = "https://sparkasse.wero.example/pay/abc123"

    @Test
    fun `mapWeroExtractions passes through when flag off`() {
        val input = mapOf(
            PAYMENT_QR_CODE_TYPE_KEY to extraction(
                name = PAYMENT_QR_CODE_TYPE_KEY,
                value = WERO_PAYMENT_QR_CODE_TYPE_VALUE,
                entity = PAYMENT_QR_CODE_TYPE_KEY,
            )
        )

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = weroUrl,
            flagEnabled = false,
        )

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `mapWeroExtractions passes through when payload is null`() {
        val input = weroMarker()

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = null,
            flagEnabled = true,
        )

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `mapWeroExtractions passes through when payload is empty`() {
        val input = weroMarker()

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = "",
            flagEnabled = true,
        )

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `mapWeroExtractions passes through when no paymentQRCodeType present`() {
        val input = mapOf(
            "iban" to extraction(name = "iban", value = "DE00…", entity = "iban")
        )

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = weroUrl,
            flagEnabled = true,
        )

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `mapWeroExtractions matches case-insensitively on name`() {
        val input = mapOf(
            "x" to extraction(name = "PAYMENTQRCODETYPE", value = "WERO", entity = "someEntity")
        )

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = weroUrl,
            flagEnabled = true,
        )

        assertThat(result.keys).containsExactly(WERO_PAYMENT_QR_CODE_URL_KEY)
        assertThat(result.getValue(WERO_PAYMENT_QR_CODE_URL_KEY).value).isEqualTo(weroUrl)
    }

    @Test
    fun `mapWeroExtractions matches case-insensitively on entity`() {
        val input = mapOf(
            "x" to extraction(name = "someName", value = "Wero", entity = "PaymentQRCodeType")
        )

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = weroUrl,
            flagEnabled = true,
        )

        assertThat(result.keys).containsExactly(WERO_PAYMENT_QR_CODE_URL_KEY)
        assertThat(result.getValue(WERO_PAYMENT_QR_CODE_URL_KEY).value).isEqualTo(weroUrl)
    }

    @Test
    fun `mapWeroExtractions reduces mixed extractions to a single Wero entry`() {
        val input = mapOf(
            PAYMENT_QR_CODE_TYPE_KEY to extraction(
                name = PAYMENT_QR_CODE_TYPE_KEY,
                value = WERO_PAYMENT_QR_CODE_TYPE_VALUE,
                entity = PAYMENT_QR_CODE_TYPE_KEY,
            ),
            "iban" to extraction(name = "iban", value = "DE00…", entity = "iban"),
            "amountToPay" to extraction(name = "amountToPay", value = "10.00:EUR", entity = "amount"),
        )

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = weroUrl,
            flagEnabled = true,
        )

        assertThat(result.keys).containsExactly(WERO_PAYMENT_QR_CODE_URL_KEY)
        val entry = result.getValue(WERO_PAYMENT_QR_CODE_URL_KEY)
        assertThat(entry.value).isEqualTo(weroUrl)
        // Candidates mirror handleEPSPaymentQRCode: one GiniCaptureExtraction carrying the payload.
        assertThat(entry.candidates).hasSize(1)
        assertThat(entry.candidates[0].value).isEqualTo(weroUrl)
        assertThat(entry.candidates[0].entity).isEqualTo(WERO_PAYMENT_QR_CODE_URL_KEY)
    }

    @Test
    fun `mapWeroExtractions does not match when value is not wero`() {
        val input = mapOf(
            PAYMENT_QR_CODE_TYPE_KEY to extraction(
                name = PAYMENT_QR_CODE_TYPE_KEY,
                value = "other",
                entity = PAYMENT_QR_CODE_TYPE_KEY,
            )
        )

        val result = WeroExtractionsMapper.mapWeroExtractions(
            specificExtractions = input,
            scannedPayload = weroUrl,
            flagEnabled = true,
        )

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `shouldRouteBackToCameraForWeroFallback is false when flag off`() {
        assertThat(
            WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions = emptyMap(),
                scannedPayload = weroUrl,
                flagEnabled = false,
            )
        ).isFalse()
    }

    @Test
    fun `shouldRouteBackToCameraForWeroFallback is false when payload missing`() {
        assertThat(
            WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions = emptyMap(),
                scannedPayload = null,
                flagEnabled = true,
            )
        ).isFalse()
    }

    @Test
    fun `shouldRouteBackToCameraForWeroFallback is false when payload is empty`() {
        assertThat(
            WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions = emptyMap(),
                scannedPayload = "",
                flagEnabled = true,
            )
        ).isFalse()
    }

    @Test
    fun `shouldRouteBackToCameraForWeroFallback is true for Wero candidate with empty extractions`() {
        assertThat(
            WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions = emptyMap(),
                scannedPayload = weroUrl,
                flagEnabled = true,
            )
        ).isTrue()
    }

    @Test
    fun `shouldRouteBackToCameraForWeroFallback is false when backend classified as Wero`() {
        assertThat(
            WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions = weroMarker(),
                scannedPayload = weroUrl,
                flagEnabled = true,
            )
        ).isFalse()
    }

    @Test
    fun `shouldRouteBackToCameraForWeroFallback is false when weroPaymentQRCodeUrl already present`() {
        val input = mapOf(
            WERO_PAYMENT_QR_CODE_URL_KEY to extraction(
                name = WERO_PAYMENT_QR_CODE_URL_KEY,
                value = weroUrl,
                entity = WERO_PAYMENT_QR_CODE_URL_KEY,
            )
        )

        assertThat(
            WeroExtractionsMapper.shouldRouteBackToCameraForWeroFallback(
                originalSpecificExtractions = input,
                scannedPayload = weroUrl,
                flagEnabled = true,
            )
        ).isFalse()
    }

    @Test
    fun `performWeroFallbackIfNeeded fires callback and returns true when gate fires`() {
        var called = 0

        val fired = WeroExtractionsMapper.performWeroFallbackIfNeeded(
            originalSpecificExtractions = emptyMap(),
            scannedPayload = weroUrl,
            flagEnabled = true,
            onFallback = { called += 1 },
        )

        assertThat(fired).isTrue()
        assertThat(called).isEqualTo(1)
    }

    @Test
    fun `performWeroFallbackIfNeeded is a no-op and returns false when gate blocks`() {
        var called = 0

        val fired = WeroExtractionsMapper.performWeroFallbackIfNeeded(
            originalSpecificExtractions = weroMarker(),
            scannedPayload = weroUrl,
            flagEnabled = true,
            onFallback = { called += 1 },
        )

        assertThat(fired).isFalse()
        assertThat(called).isEqualTo(0)
    }

    private fun weroMarker() = mapOf(
        PAYMENT_QR_CODE_TYPE_KEY to extraction(
            name = PAYMENT_QR_CODE_TYPE_KEY,
            value = WERO_PAYMENT_QR_CODE_TYPE_VALUE,
            entity = PAYMENT_QR_CODE_TYPE_KEY,
        )
    )

    private fun extraction(name: String, value: String, entity: String) =
        GiniCaptureSpecificExtraction(name, value, entity, null, emptyList())
}
