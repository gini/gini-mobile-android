package net.gini.android.bank.sdk.capture.skonto.formatter

import java.math.BigDecimal
import java.math.RoundingMode

class SkontoDiscountPercentageFormatter {

    fun format(discount: Float): String {
        val value = BigDecimal(discount.toString()).setScale(2, RoundingMode.HALF_UP)
        return "${value.toString().trimEnd('0').trimEnd('.')}%"
    }
}
