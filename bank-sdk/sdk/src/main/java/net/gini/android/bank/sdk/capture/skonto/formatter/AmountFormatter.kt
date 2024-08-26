package net.gini.android.bank.sdk.capture.skonto.formatter

import net.gini.android.capture.Amount
import java.text.NumberFormat

class AmountFormatter(
    private val amountFormatter: NumberFormat,
) {

    fun format(amount: Amount): String {
        return "${amountFormatter.format(amount.value).trim()} ${amount.currency.name}"
    }
}