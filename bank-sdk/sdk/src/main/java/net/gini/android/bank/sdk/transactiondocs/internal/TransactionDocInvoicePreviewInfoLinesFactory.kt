package net.gini.android.bank.sdk.transactiondocs.internal

import android.content.res.Resources
import net.gini.android.bank.sdk.R

internal class TransactionDocInvoicePreviewInfoLinesFactory(
    private val resources: Resources,
) {

    fun create(
        iban: String,
        dueDate: String,
        amount: String,
    ) = listOf(
        resources.getString(R.string.gbs_td_invoice_preview_info_text_iban, iban),
        resources.getString(R.string.gbs_td_invoice_preview_info_text_due_date, dueDate),
        resources.getString(R.string.gbs_td_invoice_preview_info_text_amount, amount),
    )
}
