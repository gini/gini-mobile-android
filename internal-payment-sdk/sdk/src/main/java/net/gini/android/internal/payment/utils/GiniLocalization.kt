package net.gini.android.internal.payment.utils

import java.util.Locale

enum class GiniLocalization(val readableName: String) {
    GERMAN("German"),
    ENGLISH("English"),
    GERMAN_INFORMAL("German_informal");
    /**
     * Unfortunately, there is no formal or informal German according to IETF BCP 47 language tags and language packs provided by the system.
     *
     * So the workaround is applied, which is,
     *
     * we have a default support in German, all the formal strings are placed in values -> strings.xml,
     * and informal are in values-de. So, if the customer wants support for informal, "de" locale will be selected, and all the informal
     * strings will be placed under this locale. It will respect all the regions German, which are austria, switzerland etc.
     * and we have a default locale, which is by default dedicated to German, but for system it's not de, it is the default, dedicated for fallback,
     * So we are asking system to switch to a locale which is not real, system will fall back to default.
     * which have our formal german strings.
     *
     * This workaround is applied to avoid putting if (tone == formal) getString (R.id.hello_formal) else getString(R.id.hello_informal) everywhere.
     *
     * */

    fun languageLocale(): Locale {
        return when (this) {
            GERMAN -> Locale("-1") // setting -1 so system can fallback to default values folder
            GERMAN_INFORMAL -> Locale("de")
            else -> Locale("en")
        }
    }
}