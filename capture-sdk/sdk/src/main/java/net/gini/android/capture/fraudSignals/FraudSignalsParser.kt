package net.gini.android.capture.fraudSignals

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Reads the `fraudSignals` compound extraction into [FraudSignal]s.
 *
 * Returns no signals when the client's `fraudSignalsEnabled` flag is off. Elements with an
 * unknown signal code or a missing member are skipped, so a new backend signal never breaks the
 * flow. Never throws.
 */
internal class FraudSignalsParser(
    private val getFraudSignalsEnabled: GetFraudSignalsEnabledUseCase,
) {

    fun parse(compoundExtractions: Map<String, GiniCaptureCompoundExtraction>): List<FraudSignal> {
        if (!getFraudSignalsEnabled()) return emptyList()

        val elements = compoundExtractions[FraudSignalsExtractions.FRAUD_SIGNALS_KEY]
            ?.specificExtractionMaps
            .orEmpty()

        return elements
            .mapNotNull(::toFraudSignal)
            .distinctBy { it.code }
    }

    private fun toFraudSignal(element: Map<String, GiniCaptureSpecificExtraction>): FraudSignal? {
        val code = FraudSignalCode.fromApiValue(element.valueOf(LABEL_SIGNAL_CODE))
        val severity = FraudSignalSeverity.fromApiValue(element.valueOf(LABEL_SEVERITY))
        val iban = element.valueOf(LABEL_IBAN)
        val creditorCountry = element.valueOf(LABEL_CREDITOR_COUNTRY)

        if (code == null || severity == null || iban == null) return null

        return FraudSignal(code, severity, iban, creditorCountry).takeIf {
            it.code != FraudSignalCode.IBAN_COUNTRY_MISMATCH || it.creditorCountry != null
        }
    }

    private fun Map<String, GiniCaptureSpecificExtraction>.valueOf(label: String): String? =
        this[label]?.value?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val LABEL_SIGNAL_CODE = "signalCode"
        const val LABEL_SEVERITY = "severity"
        const val LABEL_IBAN = "iban"
        const val LABEL_CREDITOR_COUNTRY = "creditorCountry"
    }
}
