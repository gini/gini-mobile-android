package net.gini.android.capture

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * This class serves for sending the extracted amount and currency in cleanup process.
 *
 * @param value the number value of the amount.
 * @param currency the amount currency.
 * @constructor Creates an instance of Amount class.
 */
class Amount(
    private val value: BigDecimal,
    private val currency: AmountCurrency
) {

    /**
     * For internal use only.
     *
     * @suppress
     */
    fun amountToPay(): String {
        val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        decimalFormat.isParseBigDecimal = true
        return decimalFormat.format(value) + ":" + currency.name
    }

    companion object {
        /**
         * Creates thread safe static instance of Amount class.
         */
        @JvmField
        val EMPTY = Amount(BigDecimal.valueOf(0), AmountCurrency.EUR)
    }
}