package net.gini.android.bank.sdk.capture.digitalinvoice.details

import net.gini.android.bank.sdk.capture.digitalinvoice.SelectableLineItem

/**
 * Created by Alpar Szotyori on 17.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Interface used by the [DigitalInvoiceActivity] to dispatch events to it's fragment.
 */
interface LineItemDetailsListener {

    /**
     * Called when the user presses the save button.
     *
     * The selectable line item is updated to contain the user's modifications.
     *
     * @param selectableLineItem - the modified selectable line item
     */
    fun onSave(selectableLineItem: SelectableLineItem)
}