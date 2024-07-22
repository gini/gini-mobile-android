package net.gini.android.health.sdk.util

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.annotation.StyleRes
import java.util.Locale

class CustomLocaleContextThemeWrapper(base: Context, @StyleRes themeResId: Int) : ContextThemeWrapper(base, themeResId) {
    companion object {
        fun wrap(context: Context, locale: Locale, @StyleRes themeResId: Int): ContextThemeWrapper {
            var contextToCopy = context
            val config = context.resources.configuration

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
            } else {
                config.locale = locale
            }
            config.setLayoutDirection(locale)
            contextToCopy = context.createConfigurationContext(config)

            return CustomLocaleContextThemeWrapper(contextToCopy, themeResId)
        }
    }
}
