package net.gini.android.bank.sdk.capture.digitalinvoice.details

/**
 * Created by Alpar Szotyori on 17.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * API of the [DigitalInvoiceBottomSheet].
 *
 */
internal interface LineItemDetailsInterface {

    /**
     * Set a listener for events in the Line Item Details Screen.
     */
    var listener: LineItemDetailsListener?
}
