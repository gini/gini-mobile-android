package net.gini.android.internal.payment.util.extensions

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import net.gini.android.internal.payment.R

internal fun Context.wrappedWithGiniMerchantTheme(): Context = ContextThemeWrapper(this, R.style.GiniPaymentTheme)

 