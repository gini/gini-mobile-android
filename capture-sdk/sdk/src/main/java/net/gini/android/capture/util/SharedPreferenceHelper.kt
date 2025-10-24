package net.gini.android.capture.util

import android.content.Context
import androidx.core.content.edit

/**
 * Generic class for saving simple data in to the shared preferences.
 * In future this could be extended to support other data types as well.
 * */

object SharedPreferenceHelper {

    private const val PREFS_KEY = "generic_data_preferences"
    const val SAF_STORAGE_URI_KEY = "SAF_storage_uri_key"

    fun saveString(key: String, value: String, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        prefs.edit { putString(key, value) }
    }

    fun getString(key: String, context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }
}
