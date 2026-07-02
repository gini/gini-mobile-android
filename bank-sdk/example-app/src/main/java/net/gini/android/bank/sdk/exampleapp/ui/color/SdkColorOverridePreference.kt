package net.gini.android.bank.sdk.exampleapp.ui.color

import android.content.Context
import androidx.core.content.edit

/**
 * Persistence for the "Override SDK colors" configuration flag.
 *
 * Backed by [android.content.SharedPreferences] (not [ExampleAppBankConfiguration]) because the
 * flag must be read synchronously and very early — in the host activity's `attachBaseContext`,
 * which runs before the configuration intent extra is available.
 */
object SdkColorOverridePreference {

    private const val PREFS_NAME = "sdk_color_override_prefs"
    private const val KEY_ENABLED = "override_sdk_colors_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ENABLED, enabled) }
    }
}
