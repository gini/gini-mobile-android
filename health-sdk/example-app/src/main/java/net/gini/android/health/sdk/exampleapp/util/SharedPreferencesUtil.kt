package net.gini.android.health.sdk.exampleapp.util

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesUtil {
    companion object {
        private const val PREFS_KEY = "prefs_key"
        const val PAYMENTREQUEST_KEY = "paymentRequest_key"

        fun saveStringToSharedPreferences(key: String, value: String?, context: Context) {
            val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()
            editor.putString(key, value)
            editor.apply()
        }

        fun getStringFromSharedPreferences(key: String, context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
            return prefs.getString(key, "")
        }
    }
}
