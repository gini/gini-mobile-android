package net.gini.android.internal.payment.utils.extensions

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

/**
 * Returns how much of [systemBottom] - the navigation bar or keyboard inset - actually overlaps
 * this view.
 *
 * The inset value itself must not be used as padding directly. [ViewCompat.getRootWindowInsets]
 * reports the window's raw insets, before anything consumed them, so on a host that is not drawing
 * edge-to-edge - where the system already keeps the content above the navigation bar - it still
 * reports the full bar height and padding by that value would leave an empty strip. Measuring the
 * overlap is host independent: it is the full inset edge-to-edge, zero when the space is already
 * reserved, and the correct remainder when a host container reserved part of it.
 *
 * Whether this view's own [View.getPaddingBottom] has to be discounted depends on how its height
 * is set. A view that wraps its content grows by the padding applied on the previous pass, so
 * without discounting it the measurement feeds back into itself. A view with a fixed or
 * constraint-resolved height does not grow, and discounting would under-measure it - it would
 * report no overlap on the pass after the padding was applied and drop the padding again.
 */
internal fun View.bottomSystemBarOverlap(systemBottom: Int): Int {
    val location = IntArray(2)
    getLocationInWindow(location)
    val heightGrowsWithPadding = layoutParams?.height == ViewGroup.LayoutParams.WRAP_CONTENT
    val bottom = location[1] + height - if (heightGrowsWithPadding) paddingBottom else 0
    return (bottom - (rootView.height - systemBottom)).coerceIn(0, systemBottom)
}

/**
 * Returns how much of [systemTop] - the status bar inset - actually overlaps this view.
 *
 * Unlike [bottomSystemBarOverlap] no padding has to be discounted: top padding moves this view's
 * content down but not its top edge, so the measurement cannot feed back into itself.
 */
internal fun View.topSystemBarOverlap(systemTop: Int): Int {
    val location = IntArray(2)
    getLocationInWindow(location)
    return (systemTop - location[1]).coerceIn(0, systemTop)
}

/**
 * The larger of the navigation bar and the keyboard bottom inset.
 */
internal fun WindowInsetsCompat.systemBottomInset(): Int = maxOf(
    getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
    getInsets(WindowInsetsCompat.Type.ime()).bottom
)

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
        var typeMask = 0
        if (displayCutout) typeMask = typeMask or WindowInsetsCompat.Type.displayCutout()
        if (navigationBars) typeMask = typeMask or WindowInsetsCompat.Type.navigationBars()
        if (statusBars) typeMask = typeMask or WindowInsetsCompat.Type.statusBars()
        val finalTypeMask = typeMask
        // Capture the margins defined in XML/code once, so applying insets adds to them
        // instead of overwriting them (which would collapse any intended spacing).
        val initialMargins = (layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            Rect(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
        } ?: Rect()
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val i = insets.getInsets(finalTypeMask)
            (v.layoutParams as? ViewGroup.MarginLayoutParams)?.let { mlp ->
                mlp.setMargins(
                    initialMargins.left + i.left,
                    initialMargins.top + i.top,
                    initialMargins.right + i.right,
                    initialMargins.bottom + i.bottom
                )
                v.layoutParams = mlp
            }
            insets
        }
        ViewCompat.requestApplyInsets(this)
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
