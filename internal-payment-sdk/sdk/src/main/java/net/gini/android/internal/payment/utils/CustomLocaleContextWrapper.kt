package net.gini.android.internal.payment.utils

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
                // API 23 (Android 6.0) requires direct field access as Configuration.setLocale()
                // was introduced in API 24. This is the only way to set locale for API 23.
                // Suppression is necessary since there's no alternative API for this version.
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            config.setLayoutDirection(locale)

            return CustomLocaleContextWrapper(context.createConfigurationContext(config))
        }
    }
}