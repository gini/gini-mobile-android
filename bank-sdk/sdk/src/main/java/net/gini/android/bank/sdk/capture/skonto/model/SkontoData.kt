package net.gini.android.bank.sdk.capture.skonto.model

import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

data class SkontoData(
    val skontoPercentageDiscounted: BigDecimal,
    val skontoPaymentMethod: SkontoPaymentMethod?,
    val skontoAmountToPay: Amount,
    val fullAmountToPay: Amount,
    val skontoRemainingDays: Int,
    val skontoDueDate: LocalDate,
) : Serializable {

    enum class SkontoPaymentMethod : Serializable {
        Unspecified, Cash, PayPal
    }

    data class Amount(val amount: BigDecimal, val currencyCode: String) : Serializable {

        companion object {

            /**
             * Creates [Amount] from string in format `value:currency_code` or throws an Exception
             */
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

                val currencyCode = amountParts.last()

                return Amount(amount, currencyCode)
            }
        }
    }
}
