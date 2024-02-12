package net.gini.android.bank.sdk.util

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import net.gini.android.capture.R

internal fun Context.wrappedWithGiniCaptureTheme(): Context = ContextThemeWrapper(this, R.style.GiniCaptureTheme)