package net.gini.android.health.sdk.util

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService

fun View.hideKeyboard() {
    getSystemService(context, InputMethodManager::class.java)?.let { imm ->
        if (imm.isAcceptingText) {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
    }
}
