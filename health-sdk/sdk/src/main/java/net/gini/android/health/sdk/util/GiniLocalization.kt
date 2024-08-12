package net.gini.android.health.sdk.util

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