package net.gini.android.internal.payment.utils.extensions

import net.gini.android.internal.payment.utils.formatCurrency
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val PDF_NAME_MAX_LENGTH = 25
private val PDF_NAME_REGEX = "^[a-zA-Z0-9-_]*\$".toRegex()

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

fun String.isValidPdfName(): Boolean =
    !(this.isEmpty() || this.length > PDF_NAME_MAX_LENGTH || !(this matches PDF_NAME_REGEX))