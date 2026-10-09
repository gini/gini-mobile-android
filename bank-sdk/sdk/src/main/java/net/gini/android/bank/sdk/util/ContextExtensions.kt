package net.gini.android.bank.sdk.util

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import net.gini.android.capture.R
import net.gini.android.capture.internal.ui.runtimecolors.runtimeColorsThemedContextOrNull

internal fun Context.wrappedWithGiniCaptureTheme(): Context =
    runtimeColorsThemedContextOrNull(this, R.style.GiniCaptureTheme)
        ?: ContextThemeWrapper(this, R.style.GiniCaptureTheme)
