package net.gini.android.internal.payment

import android.content.Context
import android.content.SharedPreferences
import net.gini.android.internal.payment.util.GiniLocalization

class GiniInternalPaymentModule {

    /**
     * Sets the app language to the desired one from the languages the SDK is supporting. If not set then defaults to the system's language locale.
     *
     * @param language enum value for the desired language or null for default system language
     * @param context Context object to save the configuration.
     */
    fun setSDKLanguage(language: GiniLocalization?, context: Context) {
        localizedContext = null
        GiniPaymentPreferences(context).saveSDKLanguage(language)
    }

    internal class GiniPaymentPreferences(context: Context) {
        private val sharedPreferences = context.getSharedPreferences("GiniPaymentPreferences", Context.MODE_PRIVATE)

        fun saveSDKLanguage(value: GiniLocalization?) {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(SDK_LANGUAGE_PREFS_KEY, value?.readableName?.uppercase())
            editor.apply()
        }

        fun getSDKLanguage(): GiniLocalization? {
            val enumValue = sharedPreferences.getString(SDK_LANGUAGE_PREFS_KEY, null)
            return if (enumValue.isNullOrEmpty()) null else GiniLocalization.valueOf(enumValue)
        }
    }

    internal var localizedContext: Context? = null

    companion object {
        private const val SDK_LANGUAGE_PREFS_KEY = "SDK_LANGUAGE_PREFS_KEY"

        fun getSDKLanguage(context: Context): GiniLocalization? {
            return GiniPaymentPreferences(context).getSDKLanguage()
        }
    }
}