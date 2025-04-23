package net.gini.android.capture.help

import android.content.Context
import androidx.core.content.edit


internal object FileImportHelpFragmentPreferences {

    private const val PREFS_NAME = "popup_prefs"
    private const val KEY_POPUP_SHOWN = "popup_shown"

    fun isIllustrationSnackBarAlreadyShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_POPUP_SHOWN, false)
    }

    fun saveIllustrationSnackBarShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit() { putBoolean(KEY_POPUP_SHOWN, true) }
    }
}


