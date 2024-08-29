package net.gini.android.internal.payment.util.extensions

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.util.CustomLocaleContextWrapper
import java.util.Locale

internal fun Context.wrappedWithGiniPaymentTheme(): Context = ContextThemeWrapper(this, R.style.GiniPaymentTheme)

internal fun Context.wrappedWithGiniPaymentThemeAndLocale(locale: Locale? = null): Context =
    if (locale == null || locale.language.isEmpty()) {
        this.wrappedWithGiniPaymentTheme()
    } else {
        this.wrappedWithCustomLocale(locale).wrappedWithGiniPaymentTheme()
    }

internal fun Context.wrappedWithCustomLocale(locale: Locale): Context = CustomLocaleContextWrapper.wrap(this, locale)
