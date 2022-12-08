package net.gini.android.capture

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class Amount(
    private val mBigDecimal: BigDecimal,
    private val mCurrency: AmountCurrency
) {

    fun amountToPay(): String {
        val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        decimalFormat.isParseBigDecimal = true
        return decimalFormat.format(mBigDecimal) + ":" + mCurrency.name
    }

    companion object {
        @JvmStatic
        val EMPTY = Amount(BigDecimal.valueOf(0), AmountCurrency.EUR)
    }
}