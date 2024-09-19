package net.gini.android.bank.sdk.capture.skonto.factory.lines

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.bank.sdk.capture.skonto.model.SkontoData
import java.time.format.DateTimeFormatter

internal class SkontoInvoicePreviewTextLinesFactory(
    private val resources: Resources,
    private val amountFormatter: AmountFormatter,
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun create(skontoData: SkontoData) = listOf(
        resources.getString(
            R.string.gbs_skonto_invoice_preview_expire_date,
            dateFormatter
        ),
        resources.getString(
            R.string.gbs_skonto_invoice_preview_final_amount,
            amountFormatter.format(skontoData.skontoAmountToPay)
        ),
        resources.getString(
            R.string.gbs_skonto_invoice_preview_full_amount,
            amountFormatter.format(skontoData.fullAmountToPay)
        )
    )
}