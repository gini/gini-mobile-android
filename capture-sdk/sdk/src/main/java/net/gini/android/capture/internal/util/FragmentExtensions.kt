package net.gini.android.capture.internal.util

import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.runtimecolors.runtimeColorsThemedInflaterOrNull

internal fun Fragment.getLayoutInflaterWithGiniCaptureTheme(inflater: LayoutInflater): LayoutInflater {
    runtimeColorsThemedInflaterOrNull(inflater, requireContext(), R.style.GiniCaptureTheme)?.let { return it }
    val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.GiniCaptureTheme)
    return inflater.cloneInContext(contextThemeWrapper)
}