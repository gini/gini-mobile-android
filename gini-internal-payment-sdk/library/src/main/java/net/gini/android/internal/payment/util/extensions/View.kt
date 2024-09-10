package net.gini.android.internal.payment.util.extensions

import android.view.LayoutInflater
import android.view.View
import java.util.Locale

fun View.getLayoutInflaterWithGiniPaymentThemeAndLocale(locale: Locale? = null): LayoutInflater =
    LayoutInflater.from(context.wrappedWithGiniPaymentThemeAndLocale(locale))
