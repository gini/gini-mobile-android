package net.gini.android.internal.payment.utils.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.gini.android.internal.payment.utils.IntervalClickListener

internal fun View.getLayoutInflaterWithGiniPaymentTheme(): LayoutInflater =
    LayoutInflater.from(context.wrappedWithGiniPaymentTheme())

internal fun Fragment.getLayoutInflaterWithGiniPaymentTheme(inflater: LayoutInflater): LayoutInflater {
    return inflater.cloneInContext(requireContext().wrappedWithGiniPaymentTheme())
}

internal fun View.hideKeyboard() {
    ContextCompat.getSystemService(context, InputMethodManager::class.java)?.let { imm ->
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}

fun View.setIntervalClickListener(click: View.OnClickListener?) {
    setOnClickListener(IntervalClickListener(click))
}