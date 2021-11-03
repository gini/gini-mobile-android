package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.capture.network.model.GiniCaptureCompoundExtraction
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction
import net.gini.android.bank.sdk.capture.digitalinvoice.details.LineItemDetailsFragment
import net.gini.android.bank.sdk.capture.digitalinvoice.onboarding.DigitalInvoiceOnboardingFragment

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
     * Called when the user tapped on a line item to edit it.
     *
     * You should show the [LineItemDetailsFragment] with the selectable line item.
     *
     * @param selectableLineItem - the [SelectableLineItem] to be edited
     */
    fun onEditLineItem(selectableLineItem: SelectableLineItem)

    /**
     * Called when the user tapped on add button to add new line item.
     *
     * You should show the [LineItemDetailsFragment] with the selectable line item.
     *
     * @param selectableLineItem - the [SelectableLineItem] to be added
     */
    fun onAddLineItem(selectableLineItem: SelectableLineItem)

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
    fun onPayInvoice(specificExtractions: Map<String, GiniCaptureSpecificExtraction>,
                     compoundExtractions: Map<String, GiniCaptureCompoundExtraction>)


    /**
     * Called before displaying [DigitalInvoiceFragment] so the user can see the onboarding view.
     *
     * You should display [DigitalInvoiceOnboardingFragment]
     */
    fun showOnboarding()
}