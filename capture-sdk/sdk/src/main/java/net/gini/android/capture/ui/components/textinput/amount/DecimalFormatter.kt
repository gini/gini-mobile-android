package net.gini.android.capture.ui.components.textinput.amount

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat

class DecimalFormatter(
    private val numberFormat: NumberFormat = NumberFormat.getCurrencyInstance().apply {
        (this as? DecimalFormat)?.apply {
            decimalFormatSymbols = decimalFormatSymbols.apply {
                currencySymbol = ""
            }
        }
    }
) {

    fun parseAmount(amount: BigDecimal) = numberFormat.format(amount).trim()
        .filter { NUMBER_CHARS.contains(it) }
        .trimStart('0')

    fun textToDigits(text: String): String = text.trim()
        .filter { NUMBER_CHARS.contains(it) }
        .trimStart('0')

    fun parseDigits(digits: String): BigDecimal =
        kotlin.runCatching { BigDecimal(digits).divide(BigDecimal(100)) }
            .getOrElse { BigDecimal.ZERO }

    fun formatDigits(digits: String): String {
        // Parse to a decimal with two decimal places
        val decimal = parseDigits(digits)
        // Format to a currency string
        return numberFormat.format(decimal).trim()
    }

    companion object {
        private val NUMBER_CHARS = "0123456789".toCharArray()
    }
}