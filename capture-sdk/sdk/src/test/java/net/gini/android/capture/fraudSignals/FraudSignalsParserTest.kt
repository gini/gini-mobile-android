package net.gini.android.capture.fraudSignals

import com.google.common.truth.Truth.assertThat
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import org.junit.Test

class FraudSignalsParserTest {

    @Test
    fun `reads a QR code IBAN mismatch signal`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(element(QR_CODE, "STOP", iban = "LT601010012345678901"))
        )

        assertThat(signals).containsExactly(
            FraudSignal(
                code = FraudSignalCode.QR_PRINTED_IBAN_MISMATCH,
                severity = FraudSignalSeverity.STOP,
                iban = "LT601010012345678901",
                creditorCountry = null,
            )
        )
    }

    @Test
    fun `reads an IBAN country mismatch signal`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(COUNTRY_CODE, "CHECK", iban = "PL61109010140000071219812874", country = "AT")
            )
        )

        assertThat(signals).containsExactly(
            FraudSignal(
                code = FraudSignalCode.IBAN_COUNTRY_MISMATCH,
                severity = FraudSignalSeverity.CHECK,
                iban = "PL61109010140000071219812874",
                creditorCountry = "AT",
            )
        )
    }

    @Test
    fun `reads both signals in response order`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(COUNTRY_CODE, "CHECK", iban = "FR7630006000011234567890189", country = "DE"),
                element(QR_CODE, "ESCALATOR", iban = "NL91ABNA0417164300"),
            )
        )

        assertThat(signals).containsExactly(
            FraudSignal(
                FraudSignalCode.IBAN_COUNTRY_MISMATCH,
                FraudSignalSeverity.CHECK,
                "FR7630006000011234567890189",
                "DE",
            ),
            FraudSignal(
                FraudSignalCode.QR_PRINTED_IBAN_MISMATCH,
                FraudSignalSeverity.ESCALATOR,
                "NL91ABNA0417164300",
                null,
            ),
        ).inOrder()
    }

    @Test
    fun `ignores an unknown signal code and keeps the known ones`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element("SOME_FUTURE_SIGNAL", "STOP", iban = "DE89370400440532013000"),
                element(QR_CODE, "SILENT", iban = "BE68539007547034"),
            )
        )

        assertThat(signals.map { it.iban }).containsExactly("BE68539007547034")
        assertThat(signals.single().severity).isEqualTo(FraudSignalSeverity.SILENT)
    }

    @Test
    fun `returns no signals when the fraudSignals compound extraction is missing`() {
        val signals = parser(enabled = true).parse(
            mapOf("lineItems" to GiniCaptureCompoundExtraction("lineItems", emptyList()))
        )

        assertThat(signals).isEmpty()
    }

    @Test
    fun `returns no signals when the fraudSignals compound extraction is empty`() {
        val signals = parser(enabled = true).parse(fraudSignals())

        assertThat(signals).isEmpty()
    }

    @Test
    fun `returns no signals when the client flag is off`() {
        val signals = parser(enabled = false).parse(
            fraudSignals(element(QR_CODE, "STOP", iban = "DE02120300000000202051"))
        )

        assertThat(signals).isEmpty()
    }

    @Test
    fun `drops a signal without iban and keeps the others`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(QR_CODE, "STOP", iban = null),
                element(COUNTRY_CODE, "CHECK", iban = "ES9121000418450200051332", country = "IT"),
            )
        )

        assertThat(signals.map { it.code }).containsExactly(FraudSignalCode.IBAN_COUNTRY_MISMATCH)
    }

    @Test
    fun `drops a signal with a blank iban and keeps the others`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(COUNTRY_CODE, "CHECK", iban = "  ", country = "FR"),
                element(QR_CODE, "STOP", iban = "LU280019400644750000"),
            )
        )

        assertThat(signals.map { it.iban }).containsExactly("LU280019400644750000")
    }

    @Test
    fun `drops signals with a missing or unknown severity and keeps the others`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(QR_CODE, severity = null, iban = "DE75512108001245126199"),
                element(COUNTRY_CODE, "PANIC", iban = "CH9300762011623852957", country = "DE"),
                element(QR_CODE, "CHECK", iban = "PT50000201231234567890154"),
            )
        )

        assertThat(signals).containsExactly(
            FraudSignal(
                FraudSignalCode.QR_PRINTED_IBAN_MISMATCH,
                FraudSignalSeverity.CHECK,
                "PT50000201231234567890154",
                null,
            )
        )
    }

    @Test
    fun `drops a country mismatch signal without creditorCountry`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(COUNTRY_CODE, "CHECK", iban = "IT60X0542811101000000123456", country = " "),
                element(QR_CODE, "STOP", iban = "AT611904300234573201"),
            )
        )

        assertThat(signals.map { it.iban }).containsExactly("AT611904300234573201")
    }

    @Test
    fun `keeps the first signal per code and an optional creditorCountry on the QR signal`() {
        val signals = parser(enabled = true).parse(
            fraudSignals(
                element(QR_CODE, "STOP", iban = "DK5000400440116243", country = "DK"),
                element(QR_CODE, "CHECK", iban = "SE4550000000058398257466"),
            )
        )

        assertThat(signals).containsExactly(
            FraudSignal(
                FraudSignalCode.QR_PRINTED_IBAN_MISMATCH,
                FraudSignalSeverity.STOP,
                "DK5000400440116243",
                "DK",
            )
        )
    }

    private fun parser(enabled: Boolean) = FraudSignalsParser(
        GetFraudSignalsEnabledUseCase(
            GiniBankConfigurationProvider().apply {
                update { it.copy(isFraudSignalsEnabled = enabled) }
            }
        )
    )

    private fun fraudSignals(vararg elements: Map<String, GiniCaptureSpecificExtraction>) =
        mapOf(
            FraudSignalsExtractions.FRAUD_SIGNALS_KEY to GiniCaptureCompoundExtraction(
                FraudSignalsExtractions.FRAUD_SIGNALS_KEY,
                elements.toList(),
            )
        )

    private fun element(
        signalCode: String,
        severity: String?,
        iban: String?,
        country: String? = null,
    ): Map<String, GiniCaptureSpecificExtraction> = buildMap {
        put("signalCode", extraction("signalCode", signalCode, "text"))
        severity?.let { put("severity", extraction("severity", it, "text")) }
        iban?.let { put("iban", extraction("iban", it, "iban")) }
        country?.let { put("creditorCountry", extraction("creditorCountry", it, "text")) }
    }

    private fun extraction(name: String, value: String, entity: String) =
        GiniCaptureSpecificExtraction(name, value, entity, null, emptyList())

    private companion object {
        const val QR_CODE = "QR_PRINTED_IBAN_MISMATCH"
        const val COUNTRY_CODE = "IBAN_COUNTRY_MISMATCH"
    }
}
