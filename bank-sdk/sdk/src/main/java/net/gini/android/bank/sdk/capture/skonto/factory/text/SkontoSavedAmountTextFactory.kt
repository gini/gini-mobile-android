package net.gini.android.bank.sdk.capture.skonto.factory.text

import android.content.res.Resources
import net.gini.android.bank.sdk.R
import net.gini.android.bank.sdk.capture.skonto.formatter.AmountFormatter
import net.gini.android.capture.Amount

internal class SkontoSavedAmountTextFactory(
    private val resources: Resources,
    private val amountFormatter: AmountFormatter,
) {

    fun create(savedAmount: Amount): String = resources.getString(
        R.string.gbs_skonto_section_footer_label_save,
        amountFormatter.format(savedAmount)
    )
}