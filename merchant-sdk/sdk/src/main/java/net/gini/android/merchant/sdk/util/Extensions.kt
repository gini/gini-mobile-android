package net.gini.android.merchant.sdk.util

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.gini.android.merchant.sdk.R
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

internal fun TextInputEditText.setTextIfDifferent(text: String) {
    if (this.text.toString() != text) {
        this.setText(text)
    }
}

internal fun String.isNumber(): Boolean {
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
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

internal fun String.toBackendFormat(): String {
    val separator = DecimalFormatSymbols.getInstance().decimalSeparator
    return this.filter { it.isDigit() || it == separator }
        .map { if (it == separator) "." else it.toString() }
        .joinToString(separator = "") { it }
        .toDouble()
        .toString()
}

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

internal fun String.adjustToLocalDecimalSeparation(): String {
    return this.replace('.', DecimalFormatSymbols.getInstance().decimalSeparator)
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
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf()
        ),
        intArrayOf(
            color,
            ColorUtils.setAlphaComponent(color, nonEnabledAlpha)
        )
    )
}

internal fun Button.setTextColorTint(@ColorInt color: Int, @IntRange(from = 0x0, to = 0xFF) nonEnabledAlpha: Int = 200) {
    setTextColor(ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf()
        ),
        intArrayOf(
            color,
            ColorUtils.setAlphaComponent(color, nonEnabledAlpha)
        )
    ))
}

internal fun TextInputLayout.setErrorMessage(@StringRes errorStringId: Int) {
    isErrorEnabled = true
    setTag(R.id.text_input_layout_tag_is_error_enabled, true)
    error = resources.getString(errorStringId).nonEmpty()
    setTag(R.id.text_input_layout_tag_error_string_id, errorStringId)
    setBackground()
}

internal fun TextInputLayout.clearErrorMessage() {
    isErrorEnabled = false
    setTag(R.id.text_input_layout_tag_is_error_enabled, false)
    error = ""
    setTag(R.id.text_input_layout_tag_error_string_id, null)
    setBackground()
}

internal fun TextInputLayout.hideErrorMessage() {
    isErrorEnabled = false
    error = ""
    setBackground()
}

internal fun TextInputLayout.showErrorMessage() {
    isErrorEnabled = getTag(R.id.text_input_layout_tag_is_error_enabled) as? Boolean ?: false
    error =
        (getTag(R.id.text_input_layout_tag_error_string_id) as? Int)?.let { resources.getString(it).nonEmpty() } ?: ""
    setBackground()
}

internal fun TextInputLayout.setBackground() {
    if (isErrorEnabled) editText?.setBackgroundResource(R.drawable.ghs_payment_input_edit_text_error_background) else editText?.setBackgroundResource(R.drawable.ghs_payment_input_edit_text_background)
}

private fun String.nonEmpty() = if (isEmpty()) " " else this

internal fun View.hideKeyboard() {
    getSystemService(context, InputMethodManager::class.java)?.let { imm ->
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}

internal suspend fun <T> Flow<T>.withPrev() = flow {
    var prev: T? = null
    collect {
        emit(prev to it)
        prev = it
    }
}

internal fun View.getLayoutInflaterWithGiniHealthTheme(): LayoutInflater =
    LayoutInflater.from(context.wrappedWithGiniHealthTheme())

internal fun Context.wrappedWithGiniHealthTheme(): Context = ContextThemeWrapper(this, R.style.GiniHealthTheme)

internal fun Fragment.getLayoutInflaterWithGiniHealthTheme(inflater: LayoutInflater): LayoutInflater {
    return inflater.cloneInContext(requireContext().wrappedWithGiniHealthTheme())
}

internal fun View.setIntervalClickListener(click: View.OnClickListener?) {
    setOnClickListener(IntervalClickListener(click))
}