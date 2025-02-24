package net.gini.android.internal.payment.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * This method:
 * - Detects the decimal and grouping separators based on the input.
 * - Handles both '.' and ',' as decimal separators.
 * - Removes grouping separators (like ',' or '.') and standardizes decimals to '.'.
 * - Ensures the output is formatted with thousands separators and two decimal places.
 *
 * According to IPC-566, the formatting should follow Locale.GERMAN conventions, no matter what locale
 * formatting the input comes in.
 *
 * Examples:
 * - 4,761 -> 4,76
 * - 12,345.6 -> 12.345,60
 * - 1.2 -> 1,20
 * - Invalid input -> 0,00
 */
@Suppress("TooGenericExceptionCaught" ,"SwallowedException")
internal fun formatCurrency(input: String): String {
    if (input.isEmpty()) return ""
    val sanitizedInput = input.trimEnd().trimStart().trimCurrency()
    return try {
        val hasCommaAsDecimal = sanitizedInput.lastIndexOf(',') > sanitizedInput.lastIndexOf('.')
        val decimalSeparator = if (hasCommaAsDecimal) ',' else '.'
        val groupingSeparator = if (decimalSeparator == ',') '.' else ','

        val standardizedInput = sanitizedInput.replace(groupingSeparator.toString(), "")
            .replace(decimalSeparator, '.')

        val number = standardizedInput.toBigDecimal()
        val symbols = DecimalFormatSymbols(Locale.GERMAN)
        val formatter = DecimalFormat("#,##0.00", symbols)
        formatter.format(number)
    } catch (e: Exception) {
        "0,00"
    }
}

internal fun String.trimCurrency(): String {
    val currencySeparator = ":"
    val currencySeparatorIndex = this.lastIndexOf(currencySeparator)
    if (currencySeparatorIndex > -1) return this.substring(0, currencySeparatorIndex)
    return this
}
