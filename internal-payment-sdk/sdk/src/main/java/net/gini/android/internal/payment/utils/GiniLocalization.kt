package net.gini.android.internal.payment.utils

import java.util.Locale

enum class GiniLocalization(val readableName: String) {
    GERMAN("German"),
    ENGLISH("English");

    fun languageLocale(): Locale {
        return when (this) {
            GERMAN -> Locale("de")
            else -> Locale("en")
        }
    }
}