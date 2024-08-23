package net.gini.android.bank.sdk.capture.digitalinvoice.skonto.args

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import net.gini.android.bank.sdk.capture.skonto.model.SkontoInvoiceHighlightBoxes

@Parcelize
data class DigitalInvoiceSkontoArgs(
    val data: SkontoData,
    val invoiceHighlights: List<SkontoInvoiceHighlightBoxes>,
    val isSkontoSectionActive: Boolean,
) : Parcelable