package net.gini.android.internal.payment.utils.extensions

import androidx.annotation.StringRes
import com.google.android.material.textfield.TextInputLayout
import net.gini.android.internal.payment.R


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
    if (isErrorEnabled) editText?.setBackgroundResource(R.drawable.gps_payment_input_edit_text_error_background) else editText?.setBackgroundResource(R.drawable.gps_payment_input_edit_text_background)
}