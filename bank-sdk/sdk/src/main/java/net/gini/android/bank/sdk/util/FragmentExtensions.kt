package net.gini.android.bank.sdk.util

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.runtimecolors.runtimeColorsThemedInflaterOrNull

internal fun Fragment.getLayoutInflaterWithGiniCaptureTheme(inflater: LayoutInflater): LayoutInflater {
    runtimeColorsThemedInflaterOrNull(inflater, requireContext(), R.style.GiniCaptureTheme)?.let { return it }
    return inflater.cloneInContext(requireContext().wrappedWithGiniCaptureTheme())
}
