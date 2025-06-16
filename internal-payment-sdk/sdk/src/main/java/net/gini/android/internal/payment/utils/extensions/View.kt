package net.gini.android.internal.payment.utils.extensions

import android.app.Activity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import net.gini.android.internal.payment.utils.IntervalClickListener
import java.util.Locale

fun View.getLayoutInflaterWithGiniPaymentThemeAndLocale(locale: Locale? = null): LayoutInflater =
    LayoutInflater.from(context.wrappedWithGiniPaymentThemeAndLocale(locale))


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
internal fun View.hideKeyboardFully() {
    val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
    this.windowToken?.let { token ->
        imm?.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS)
    }
    // Also tell the window not to keep the keyboard open
    if (context is Activity) {
        (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }
}
fun View.setIntervalClickListener(click: View.OnClickListener?) {
    setOnClickListener(IntervalClickListener(click))
}

/**
 * Executes [onKeyboardActivate] when the view is activated using a physical keyboard (e.g. Enter or D-Pad center).
 */
fun View.onKeyboardAction(onKeyboardActivate: () -> Unit) {
    setOnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_UP &&
            (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
        ) {
            onKeyboardActivate()
            true
        } else {
            false
        }
    }
}