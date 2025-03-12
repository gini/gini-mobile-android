package net.gini.android.health.sdk.exampleapp.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale

private val PRICE_STRING_REGEX = "^-?[0-9]+([.,])[0-9]+\$".toRegex()

fun InputStream.getBytes(): ByteArray {
    val byteBuffer = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var len: Int
    while (this.read(buffer).also { len = it } != -1) {
        byteBuffer.write(buffer, 0, len)
    }
    return byteBuffer.toByteArray()
}

fun String?.parseAmount(shouldThrowErrorForFormat: Boolean): String =
    this?.split(":")?.let { substrings ->
        if (substrings.size != 2 && shouldThrowErrorForFormat) {
            throw java.lang.NumberFormatException(
                "Invalid price format. Expected <Price>:<Currency Code>, but got: $this"
            )
        }
        val price = substrings[0].parsePrice()
        val currency = if (substrings.size == 2) Currency.getInstance(substrings[1]) else Currency.getInstance(Locale.GERMANY)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale.GERMAN)
        numberFormat.maximumFractionDigits = 2
        numberFormat.currency = currency

        return numberFormat.format(price)
    } ?: ""

fun String.parsePrice(): BigDecimal =
    if (this matches PRICE_STRING_REGEX) {
        when {
            this.contains(".") -> {
                parsePriceWithLocale(this, Locale.ENGLISH)
            }

            this.contains(",") -> {
                parsePriceWithLocale(this, Locale.GERMAN)
            }

            else -> {
                throw NumberFormatException("Unknown number format locale")
            }
        }
    } else {
        throw NumberFormatException("Invalid number format")
    }

fun parsePriceWithLocale(price: String, locale: Locale) = DecimalFormat(
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