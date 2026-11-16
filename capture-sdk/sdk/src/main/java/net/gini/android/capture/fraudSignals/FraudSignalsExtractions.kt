package net.gini.android.capture.fraudSignals

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction

/**
 * The `fraudSignals` compound extraction is for the SDK's own fraud warning only: it is never
 * returned to integrators and never sent back as feedback.
 */
internal object FraudSignalsExtractions {

    const val FRAUD_SIGNALS_KEY = "fraudSignals"

    /**
     * Returns a new map without [FRAUD_SIGNALS_KEY]. The input map is not changed.
     *
     * The result is a mutable [HashMap], like the map the network layer delivers: integrators
     * receive it through `AnalysisFragmentListener.onExtractionsAvailable` and may modify it.
     *
     * A `null` input (possible from an integrator's own network service) gives an empty map, the
     * same result the analysis screen produced for it before.
     */
    @JvmStatic
    fun withoutFraudSignals(
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>?,
    ): Map<String, GiniCaptureCompoundExtraction> =
        HashMap(compoundExtractions.orEmpty()).apply { remove(FRAUD_SIGNALS_KEY) }
}
