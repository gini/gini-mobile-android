package net.gini.android.internal.payment.utils.extensions

import net.gini.android.internal.payment.utils.formatCurrency
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun String.isNumber(): Boolean {
    val separator = DecimalFormatSymbols.getInstance(Locale.GERMAN).decimalSeparator
    return try {
        this.filter { it.isDigit() || it == separator }
            .map { if (it == separator) "." else it.toString() }
            .joinToString(separator = "") { it }
            .toDouble()
        true
    } catch (_: Throwable) {
        false
    }
}

internal fun String.nonEmpty() = if (isEmpty()) " " else this
fun String.toBackendFormat(): String {
    val separator = DecimalFormatSymbols.getInstance(Locale.GERMAN).decimalSeparator
    return this.filter { it.isDigit() || it == separator }
        .map { if (it == separator) "." else it.toString() }
        .joinToString(separator = "") { it }
        .toDouble()
        .toString()
}

fun String.sanitizeAmount(): String = formatCurrency(this)