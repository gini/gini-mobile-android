package net.gini.android.health.sdk.exampleapp.invoices.ui.model

import net.gini.android.health.sdk.exampleapp.invoices.data.model.DocumentWithExtractions
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale

private val PRICE_STRING_REGEX = "^-?[0-9]+([.,])[0-9]+\$".toRegex()

data class InvoiceItem(
    val documentId: String,
    val recipient: String?,
    val amount: String?,
    val dueDate: String?,
    val isPayable: Boolean = false,
    val medicalProvider: String?
) {

    companion object {
        fun fromInvoice(documentWithExtractions: DocumentWithExtractions): InvoiceItem {
            return InvoiceItem(
                documentWithExtractions.documentId,
                documentWithExtractions.recipient,
                parseAmount(documentWithExtractions.amount),
                documentWithExtractions.dueDate,
                documentWithExtractions.isPayable,
                documentWithExtractions.medicalProvider
            )
        }

        private fun parseAmount(amount: String?): String? {
            return amount?.split(":")?.let { substrings ->
                if (substrings.size != 2) {
                    throw java.lang.NumberFormatException(
                        "Invalid price format. Expected <Price>:<Currency Code>, but got: $amount"
                    )
                }
                val price = parsePrice(substrings[0])
                val currency = Currency.getInstance(substrings[1])

                val numberFormat = NumberFormat.getCurrencyInstance()
                numberFormat.maximumFractionDigits = 2
                numberFormat.currency = currency

                return numberFormat.format(price)
            }
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