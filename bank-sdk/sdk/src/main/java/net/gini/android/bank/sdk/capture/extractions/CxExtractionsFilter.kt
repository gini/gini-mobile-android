package net.gini.android.bank.sdk.capture.extractions

import net.gini.android.capture.CaptureSDKResult
import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Filters extraction results when [net.gini.android.capture.ProductTag.CxExtractions] is active.
 *
 * For cross-border payments, the Gini API returns payment data in the [CROSS_BORDER_PAYMENT_KEY]
 * compound extraction. SEPA-specific flat extractions, payment hint extractions, and compound
 * extractions tied to Skonto and Return Assistant are not applicable and are removed.
 *
 * **Partial results**: A [CROSS_BORDER_PAYMENT_KEY] entry whose inner `specificExtractionMaps`
 * contains only a subset of the expected CX fields is treated as a valid partial result and is
 * passed through unchanged. Only the complete absence of any rows in `specificExtractionMaps`
 * (or the absence of the key itself) is treated as "no extractions" and triggers the no-results
 * screen — see [net.gini.android.capture.analysis.AnalysisScreenPresenter].
 */
internal object CxExtractionsFilter {

    internal const val CROSS_BORDER_PAYMENT_KEY = "crossBorderPayment"

    private val sepaSpecificExtractionKeys = setOf(
        "iban",
        "bic",
        "amountToPay",
        "paymentRecipient",
        "paymentReference",
        "paymentPurpose",
        "instantPayment",
    )

    private val paymentHintsExtractionKeys = setOf(
        "paymentDueDate",
        "creditNote",
    )

    private val sepaCompoundExtractionKeys = setOf(
        "skontoDiscounts",
        "lineItems",
    )

    /**
     * Returns `true` if [result] contains a non-empty [CROSS_BORDER_PAYMENT_KEY] compound
     * extraction (i.e. at least one row of CX fields was returned by the backend).
     *
     * A `false` result means no CX data is available and the no-results screen should be shown.
     */
    fun hasCxExtractions(result: CaptureSDKResult.Success): Boolean {
        val cbp = result.compoundExtractions[CROSS_BORDER_PAYMENT_KEY]
        return cbp != null && cbp.specificExtractionMaps.isNotEmpty()
    }

    /**
     * Returns a new [CaptureSDKResult.Success] with all SEPA-specific, payment-hints, and
     * Skonto/Return-Assistant extractions removed, leaving [CROSS_BORDER_PAYMENT_KEY] and any
     * other non-SEPA compound extractions intact.
     */
    fun filterForCxExtractions(result: CaptureSDKResult.Success): CaptureSDKResult.Success {
        val filteredSpecific: Map<String, GiniCaptureSpecificExtraction> =
            result.specificExtractions.filterKeys { key ->
                key !in sepaSpecificExtractionKeys && key !in paymentHintsExtractionKeys
            }

        val filteredCompound: Map<String, GiniCaptureCompoundExtraction> =
            result.compoundExtractions.filterKeys { key ->
                key !in sepaCompoundExtractionKeys
            }

        return CaptureSDKResult.Success(
            specificExtractions = filteredSpecific,
            compoundExtractions = filteredCompound,
            returnReasons = emptyList(),
        )
    }
}
