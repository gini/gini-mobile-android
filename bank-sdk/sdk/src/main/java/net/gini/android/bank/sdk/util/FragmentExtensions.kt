package net.gini.android.bank.sdk.util

import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import net.gini.android.capture.R

internal fun Fragment.getLayoutInflaterWithGiniCaptureTheme(inflater: LayoutInflater): LayoutInflater {
    val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.GiniCaptureTheme)
    return inflater.cloneInContext(contextThemeWrapper)
}