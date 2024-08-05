package net.gini.android.bank.sdk.capture.skonto

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Interface used by the [SkontoFragment] to dispatch events to the hosting Activity.
 */
interface SkontoFragmentListener {

    /**
     * Called when the user presses the proceed button.
     *
     * The extractions were updated to contain the user's modifications:
     *  - "amountToPay" was updated to contain the new amount to pay (with or without skonto)
     *
     * @param specificExtractions - extractions like the "amountToPay", "iban", etc.
     * @param compoundExtractions - extractions like the "skontoAmountToPay", "skontoDueDate", etc.
     */
    fun onPayInvoiceWithSkonto(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    )
}