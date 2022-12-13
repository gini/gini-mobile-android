package net.gini.android.capture

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * This class serves for sending the extracted amount and currency in cleanup process
 **/

/**
 * Creates an instance of Amount class
 *
 * @param mBigDecimal
 * @param mCurrency
 *
 */
class Amount(
    private val mBigDecimal: BigDecimal,
    private val mCurrency: AmountCurrency
) {

    /**
     * For internal use only
     *
     * @suppress
     */
    fun amountToPay(): String {
        val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        decimalFormat.isParseBigDecimal = true
        return decimalFormat.format(mBigDecimal) + ":" + mCurrency.name
    }

    /**
     * Creates thread safe static instance of Amount class
     */
    companion object {
        @JvmStatic
        val EMPTY = Amount(BigDecimal.valueOf(0), AmountCurrency.EUR)
    }
}