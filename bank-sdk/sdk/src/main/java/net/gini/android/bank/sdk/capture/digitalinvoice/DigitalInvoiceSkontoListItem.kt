package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.capture.Amount

data class DigitalInvoiceSkontoListItem(
    val savedAmount: Amount,
    val message: String,
    val enabled: Boolean,
)