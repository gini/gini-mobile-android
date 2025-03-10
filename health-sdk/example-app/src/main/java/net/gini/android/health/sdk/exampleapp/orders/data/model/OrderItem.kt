package net.gini.android.health.sdk.exampleapp.orders.data.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale

private val PRICE_STRING_REGEX = "^-?[0-9]+([.,])[0-9]+\$".toRegex()

data class OrderItem(
    val order: Order,
    val recipient: String,
    val amount: String,
    val purpose: String
) {

    companion object {
        fun fromOrder(order: Order): OrderItem {
            return OrderItem(
                order = order,
                recipient = order.recipient,
                amount = parseAmount(order.amount),
                purpose = order.purpose
            )
        }

        private fun parseAmount(amount: String): String = amount.split(":").let { substrings ->
            if (substrings.size != 2) {
                throw java.lang.NumberFormatException(
                    "Invalid price format. Expected <Price>:<Currency Code>, but got: $amount"
                )
            }
            val price = parsePrice(substrings[0])
            val currency = Currency.getInstance(substrings[1])

            val numberFormat = NumberFormat.getCurrencyInstance(Locale.GERMAN)
            numberFormat.maximumFractionDigits = 2
            numberFormat.currency = currency

            return numberFormat.format(price)
        }

        private fun parsePrice(price: String): BigDecimal =
            if (price matches PRICE_STRING_REGEX) {
                when {
                    price.contains(".") -> {
                        parsePriceWithLocale(price, Locale.ENGLISH)
                    }

                    price.contains(",") -> {
                        parsePriceWithLocale(price, Locale.GERMAN)
                    }

                    else -> {
                        throw NumberFormatException("Unknown number format locale")
                    }
                }
            } else {
                throw NumberFormatException("Invalid number format")
            }

        private fun parsePriceWithLocale(price: String, locale: Locale) = DecimalFormat(
            "0.00",
            DecimalFormatSymbols.getInstance(locale)
        )
            .apply { isParseBigDecimal = true }
            .run {
                try {
                    parse(price) as BigDecimal
                } catch (e: ParseException) {
                    throw NumberFormatException(e.message)
                }
            }
    }
}