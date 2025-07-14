package net.gini.android.internal.payment.utils.extensions

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import dev.chrisbanes.insetter.applyInsetter
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

/**
 * [applyWindowInsetsWithTopPadding]
 * From Android 15 onwards we have to support the edge to edge enforcement. In health example app
 * and SDK we are going to use this method, so we can protect the view from display cut outs
 * nav bars and status bars.
 * @param paddingTargetView : Because we are using action bars in many places for having the
 * title of screen, in Android 15 onwards we were facing the issue that screen content was drawn
 * under the status bar, so instead of giving top margin in every layout, we can pass the view to
 * below method and it will add top padding to the content of screen equals to action bar's height,
 * and it will only work for Android 15 onwards
 *
 * Important Note: We need to remove this method and start using the custom material toolbar.
 * */

fun View.applyWindowInsetsWithTopPadding(
    paddingTargetView: View? = null,
    displayCutout: Boolean = true,
    navigationBars: Boolean = true,
    statusBars: Boolean = true
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

    val list = booleanArrayOf(displayCutout, navigationBars, statusBars)

    if (list.atLeastOneIsTrue()) {
        applyInsetter {
            type(
                displayCutout = displayCutout,
                navigationBars = navigationBars,
                statusBars = statusBars,
            ) {
                margin()
            }
        }
    }

    paddingTargetView?.let { view ->
        val topPadding = context.getActionBarHeightInPx()
        view.updatePadding(top = topPadding)
    }
}

private fun Context.getActionBarHeightInPx(): Int {
    val tv = TypedValue()
    return if (theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, tv, true)) {
        TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
    } else 0
}
