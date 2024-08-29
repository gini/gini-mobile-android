package net.gini.android.internal.payment.utils.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

internal fun View.getLayoutInflaterWithGiniPaymentTheme(): LayoutInflater =
    LayoutInflater.from(context.wrappedWithGiniPaymentTheme())

internal fun View.hideKeyboard() {
    ContextCompat.getSystemService(context, InputMethodManager::class.java)?.let { imm ->
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}