package net.gini.android.capture.analysis.fraud

import androidx.annotation.RestrictTo
import net.gini.android.capture.fraudSignals.FraudSignalsExtractions
import net.gini.android.capture.fraudSignals.FraudSignalsParser
import net.gini.android.capture.fraudSignals.GetFraudSignalsEnabledUseCase
import net.gini.android.capture.internal.provider.GiniBankConfigurationProvider
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Development switch for the IBAN mismatch warning, in the spirit of
 * [net.gini.android.capture.GiniCaptureDebug]: while [enabled] every successful analysis gets a
 * sample `fraudSignals` compound extraction (both signals) and sample printed IBANs, and the real
 * [FraudSignalsParser] and [IBANMismatchArgsMapper] build the warning from them, whatever the
 * `fraudSignalsEnabled` flag says. This shows the logic on a device before the backend delivers
 * fraud signals. The sample data never reaches the integrator: only the mapper sees it.
 *
 * Branch scaffolding: removed before merge, see "Before merge" in `specs/PP-3961-feature.md`.
 * Off by default and not reset by `GiniCapture.cleanup()`. Public because the example app is a
 * separate Gradle module and Kotlin `internal` does not cross module boundaries; integrators
 * have no reason to call it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object IBANMismatchDebug {

    /** When `true` the Analysis screen shows the warning built from the sample signals. */
    @JvmStatic
    var enabled: Boolean = false

    private const val QR_IBAN = "PL12100001110100100012345678"
    private val PRINTED_IBANS = listOf("DE89370400440532013000", QR_IBAN, "AT611904300234573201")

    /** Parses with the flag forced on, so the switch works before the client flag is rolled out. */
    internal val parser: FraudSignalsParser by lazy {
        val configurationProvider = GiniBankConfigurationProvider().apply {
            update { it.copy(isFraudSignalsEnabled = true) }
        }
        FraudSignalsParser(GetFraudSignalsEnabledUseCase(configurationProvider))
    }

    /** [compoundExtractions] with the sample `fraudSignals` added. */
    internal fun withSampleSignals(
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>,
    ): Map<String, GiniCaptureCompoundExtraction> = compoundExtractions + (
        FraudSignalsExtractions.FRAUD_SIGNALS_KEY to GiniCaptureCompoundExtraction(
            FraudSignalsExtractions.FRAUD_SIGNALS_KEY,
            listOf(
                mapOf(
                    text("signalCode", "QR_PRINTED_IBAN_MISMATCH"),
                    text("severity", "STOP"),
                    iban(QR_IBAN),
                ),
                mapOf(
                    text("signalCode", "IBAN_COUNTRY_MISMATCH"),
                    text("severity", "CHECK"),
                    iban(QR_IBAN),
                    text("creditorCountry", "DE"),
                ),
            ),
        )
        )

    /**
     * [extractions] with an `iban` extraction whose candidates are the sample printed IBANs.
     * The payment recipient of the scanned document is kept.
     */
    internal fun withSamplePrintedIbans(
        extractions: Map<String, GiniCaptureSpecificExtraction>,
    ): Map<String, GiniCaptureSpecificExtraction> = extractions + (
        "iban" to GiniCaptureSpecificExtraction(
            "iban",
            QR_IBAN,
            "iban",
            null,
            PRINTED_IBANS.map { GiniCaptureExtraction(it, "iban", null) },
        )
        )

    private fun text(name: String, value: String) =
        name to GiniCaptureSpecificExtraction(name, value, "text", null, emptyList())

    private fun iban(value: String) =
        "iban" to GiniCaptureSpecificExtraction("iban", value, "iban", null, emptyList())
}
