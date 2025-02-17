package net.gini.android.health.sdk.util

import android.content.res.ColorStateList
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.ColorUtils
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.gini.android.health.sdk.R
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun String.toBackendFormat(): String {
    val separator = DecimalFormatSymbols.getInstance(Locale.GERMAN).decimalSeparator
    return this.filter { it.isDigit() || it == separator }
        .map { if (it == separator) "." else it.toString() }
        .joinToString(separator = "") { it }
        .toDouble()
        .toString()
}

internal fun String.adjustToLocalDecimalSeparation(): String {
    return this.replace('.', DecimalFormatSymbols.getInstance(Locale.GERMAN).decimalSeparator)
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
    if (isErrorEnabled) editText?.setBackgroundResource(net.gini.android.internal.payment.R.drawable.gps_payment_input_edit_text_error_background) else editText?.setBackgroundResource(net.gini.android.internal.payment.R.drawable.gps_payment_input_edit_text_background)
}

private fun String.nonEmpty() = if (isEmpty()) " " else this

fun View.hideKeyboard() {
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
