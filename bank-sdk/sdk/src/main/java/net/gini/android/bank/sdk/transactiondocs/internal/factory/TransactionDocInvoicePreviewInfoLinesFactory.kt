package net.gini.android.bank.sdk.transactiondocs.internal.factory

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.capture.Amount
import net.gini.android.capture.provider.LastExtractionsProvider

internal class TransactionDocInvoicePreviewInfoLinesFactory(
    private val resources: Resources,
    private val lastExtractionsProvider: LastExtractionsProvider,
    private val amountFormatter: AmountFormatter,
) {

    fun create(
    ) = listOfNotNull(
        lastExtractionsProvider.provide()["iban"]?.value?.let {
            resources.getString(
                R.string.gbs_td_invoice_preview_info_text_iban,
                it
            )
        },
        lastExtractionsProvider.provide()["amountToPay"]?.value?.let { Amount.parse(it) }?.let {
            resources.getString(
                R.string.gbs_td_invoice_preview_info_text_amount, amountFormatter.format(it)
            )
        })
}
