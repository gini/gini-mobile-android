package net.gini.android.bank.sdk.exampleapp.core

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the QA-only "Open with: use Uri-based API" flag.
 *
 * This is the only configuration value which is persisted: "open with" cold-starts the app
 * (SplashActivity/MainActivity/CaptureFlowHostActivity receive the ACTION_VIEW/SEND intent in a
 * fresh process), so the flag must survive process death to influence the open-with flow. The
 * rest of [net.gini.android.bank.sdk.exampleapp.ui.data.ExampleAppBankConfiguration] is
 * deliberately kept in-memory only.
 */
@Singleton
class OpenWithUriApiPreference @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = sharedPreferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ENABLED, isEnabled).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "open_with_uri_api_preferences"
        private const val KEY_ENABLED = "open_with_uri_based_api_enabled"
    }
}
