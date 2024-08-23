package net.gini.android.bank.sdk.capture.digitalinvoice

import net.gini.android.bank.sdk.capture.skonto.model.SkontoData

data class DigitalInvoiceSkontoListItem(
    val data: SkontoData,
    val enabled: Boolean,
)