package net.gini.android.internal.payment.utils

import android.R
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.graphics.ColorUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

internal fun Button.setBackgroundTint(@ColorInt color: Int, @IntRange(from = 0x0, to = 0xFF) nonEnabledAlpha: Int = 100) {
    backgroundTintList = ColorStateList(
        arrayOf(
            intArrayOf(R.attr.state_enabled),
            intArrayOf()
        ),
        intArrayOf(
            color,
            ColorUtils.setAlphaComponent(color, nonEnabledAlpha)
        )
    )
}

internal fun TextInputEditText.setTextIfDifferent(text: String) {
    if (this.text.toString() != text) {
        this.setText(text)
    }
}

internal suspend fun <T> Flow<T>.withPrev() = flow {
    var prev: T? = null
    collect {
        emit(prev to it)
        prev = it
    }
}
