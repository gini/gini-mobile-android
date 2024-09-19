package net.gini.android.internal.payment.util.extensions

import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import net.gini.android.internal.payment.GiniInternalPaymentModule
import java.util.Locale

internal fun Fragment.getLayoutInflaterWithGiniPaymentThemeAndLocale(inflater: LayoutInflater, locale: Locale? = null): LayoutInflater {
    return inflater.cloneInContext(requireContext().wrappedWithGiniPaymentThemeAndLocale(locale))
}

fun Fragment.getLocaleStringResource(resourceId: Int, giniPaymentModule: GiniInternalPaymentModule?): String {
    if (giniPaymentModule?.localizedContext == null) {
        giniPaymentModule?.localizedContext = context?.createConfigurationContext(resources.configuration)
    }

    return giniPaymentModule?.localizedContext?.getText(resourceId).toString()
}
