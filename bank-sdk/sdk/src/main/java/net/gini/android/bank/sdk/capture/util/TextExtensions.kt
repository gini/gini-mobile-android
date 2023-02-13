package net.gini.android.bank.sdk.capture.util

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat

internal val amountWatcher = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {
        s?.let { input ->
            // Take only the first 7 digits (without leading zeros)
            val onlyDigits = input.toString().trim()
                .filter { it != '.' && it != ',' }
                .take(7)
                .trimStart('0')

            val newString = try {
                // Parse to a decimal with two decimal places
                val decimal = BigDecimal(onlyDigits).divide(BigDecimal(100))
                // Format to a currency string
                currencyFormatterWithoutSymbol().format(decimal).trim()
            } catch (e: NumberFormatException) {
                ""
            }

            if (newString != input.toString()) {
                input.replace(0, input.length, newString)
            }
        }
    }

}

internal fun currencyFormatterWithoutSymbol(): NumberFormat =
    NumberFormat.getCurrencyInstance().apply {
        (this as? DecimalFormat)?.apply {
            decimalFormatSymbols = decimalFormatSymbols.apply {
                currencySymbol = ""
            }
        }
    }

internal fun View.hideKeyboard() {
    ContextCompat.getSystemService(context, InputMethodManager::class.java)?.let { imm ->
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}
