package net.gini.android.internal.payment.util

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import java.util.Locale

class CustomLocaleContextWrapper(base: Context) : ContextWrapper(base) {
    companion object {
        fun wrap(context: Context, locale: Locale): ContextWrapper {
            val config = context.resources.configuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
            } else {
                config.locale = locale
            }
            config.setLayoutDirection(locale)

            return CustomLocaleContextWrapper(context.createConfigurationContext(config))
        }
    }
}