package net.gini.android.bank.sdk.util

import android.view.LayoutInflater
import androidx.fragment.app.Fragment

internal fun Fragment.getLayoutInflaterWithGiniCaptureTheme(inflater: LayoutInflater): LayoutInflater {
    return inflater.cloneInContext(requireContext().wrappedWithGiniCaptureTheme())
}