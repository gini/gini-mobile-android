package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction

/**
 * Created by Alpar Szotyori on 05.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Interface used by the [DigitalInvoiceFragment] to dispatch events to the hosting Activity.
 */
interface DigitalInvoiceFragmentListener {

    /**
     * Called when the user presses the buy button.
     *
     * The extractions were updated to contain the user's modifications:
     *  - "amountToPay" was updated to contain the sum of the selected line items' prices,
     *  - the line items were updated according to the user's modifications.
     *
     * @param specificExtractions - extractions like the "amountToPay", "iban", etc.
     * @param compoundExtractions - extractions like the "lineItems"
     */
    fun onPayInvoice(
        specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
        compoundExtractions: Map<String, GiniCaptureCompoundExtraction>
    )
}