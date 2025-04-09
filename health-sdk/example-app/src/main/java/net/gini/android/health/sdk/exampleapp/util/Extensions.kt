package net.gini.android.health.sdk.exampleapp.util
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.gini.android.health.sdk.exampleapp.R
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Date
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

@RequiresApi(Build.VERSION_CODES.N)
fun String.prettifyDate(): String {
    if (this.isEmpty()) return ""

    val format = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS")
    val localDateFormat = SimpleDateFormat("dd MMM yyyy")

    try {
        val date = format.parse(this)
        return localDateFormat.format(date)
    } catch (exception: ParseException) {
        return ""
    }
}

@RequiresApi(Build.VERSION_CODES.N)
fun String?.isInTheFuture(): Boolean {
    if (this.isNullOrEmpty()) { return false }
    val format = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS")

    try {
        val date = format.parse(this)
        return date.after(Date())
    } catch (exception: ParseException) {
        return false
    }
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

fun FragmentManager.add(fragment: Fragment) {
    this.beginTransaction()
        .add(R.id.fragment_container, fragment, fragment::class.java.simpleName)
        .addToBackStack(fragment::class.java.simpleName)
        .commit()
}

fun Context.showAlertDialog(title: String, message: String) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok, null)
        .show()
}