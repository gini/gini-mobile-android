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
            var context = context
            val config = context.resources.configuration
            Locale.setDefault(locale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setSystemLocale(config, locale)
            } else {
                setSystemLocaleLegacy(config, locale)
            }
            config.setLayoutDirection(locale)
            context = context.createConfigurationContext(config)

            return CustomLocaleContextThemeWrapper(context, themeResId)
        }

        @SuppressWarnings("deprecation")
        fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
            config.locale = locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        fun setSystemLocale(config: Configuration, locale: Locale) {
            config.setLocale(locale)
        }
    }
}
