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
