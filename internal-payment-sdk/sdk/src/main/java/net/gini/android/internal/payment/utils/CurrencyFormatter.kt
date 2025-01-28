package net.gini.android.internal.payment.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Context: IBM reported a issue, that if the value is passed like this 471.8 then it
 * is converted to 47.18 which is a major bug, as clients can give this value often
 * to ReviewView, and we already have proper formating of the field in specific fragments
 * which takes care that if user is entering 12 then it will be automatically converted
 * to 12.00, so we will just sanitize the input first time and then it will be taken
 * care by the views it self by [amountWatcher].
 *
 * This method:
 * - Detects the decimal and grouping separators based on the input.
 * - Handles both '.' and ',' as decimal separators.
 * - Removes grouping separators (like ',' or '.') and standardizes decimals to '.'.
 * - Ensures the output is formatted with thousands separators and two decimal places.
 *
 * Examples:
 * - 4,761 -> 4.76
 * - 12,345.6 -> 12,345.60
 * - 1.2 -> 1.20
 * - Invalid input -> 0.00
 *
 */
@Suppress("TooGenericExceptionCaught" ,"SwallowedException")
internal fun formatCurrency(input: String): String {
    if (input.isEmpty()) return ""
    val sanitizedInput = input.trimEnd().trimStart()
    return try {
        val hasCommaAsDecimal = sanitizedInput.lastIndexOf(',') > sanitizedInput.lastIndexOf('.')
        val decimalSeparator = if (hasCommaAsDecimal) ',' else '.'
        val groupingSeparator = if (decimalSeparator == ',') '.' else ','

        val standardizedInput = sanitizedInput.replace(groupingSeparator.toString(), "")
            .replace(decimalSeparator, '.')

        val number = standardizedInput.toBigDecimal()
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            this.decimalSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0.00", symbols)
        formatter.format(number)
    } catch (e: Exception) {
        "0.00"
    }
}

/**
 * This method:
 * - Detects if the number has exactly two decimal places or not because
 * value with the two decimals are already perfect and can be taken
 * care by [amountWatcher].
 *
 *  Examples:
 * - 1.2 -> false
 * - 1.22 -> true
 * - 1 -> false
 * - 1.333 -> false
 * - "" -> false
 * */
@Suppress("TooGenericExceptionCaught" ,"SwallowedException")
internal fun isValidTwoDecimalNumber(input: String): Boolean {
    if (input.isEmpty()) return false
    val isCommaAsDecimal = input.trimStart().trimEnd().contains(',') && input.lastIndexOf(',') > input.lastIndexOf('.')
    val normalizedInput = if (isCommaAsDecimal) {
        input.replace(".", "").replace(",", ".")
    } else {
        input.replace(",", "")
    }

    return try {
        val parts = normalizedInput.split('.')
        parts.size == 2 && parts[1].length == 2
    } catch (e: Exception) {
        false
    }
}
