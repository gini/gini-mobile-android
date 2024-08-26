package net.gini.android.capture

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
@Parcelize
data class Amount(
    val value: BigDecimal,
    val currency: AmountCurrency
) : Parcelable {

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

        /**
         * Creates [Amount] from string in format `value:currency_code` or throws an Exception
         */
        @Suppress("ThrowsCount")
        @Throws(IllegalArgumentException::class)
        fun parse(amountStr: String): Amount {
            val amountParts = amountStr.split(":").also {
                if (it.size != 2) {
                    throw IllegalArgumentException(
                        "Invalid amount format for value: $amountStr. " +
                                "Should be `value:currency_code`"
                    )
                }
            }

            val amount = runCatching { BigDecimal(amountParts.first()) }.getOrElse {
                throw IllegalArgumentException(
                    "Invalid amount format for value: $amountStr. " +
                            "Can't convert `${amountParts.first()} to BigDecimal`"
                )
            }

            val currencyCode = kotlin.runCatching { AmountCurrency.valueOf(amountParts.last()) }
                .getOrNull() ?: throw IllegalArgumentException(
                "Invalid currency code format for value: $amountStr. " +
                        "Can't convert `${amountParts.last()} to AmountCurrency`"
            )

            return Amount(amount, currencyCode)
        }
    }
}