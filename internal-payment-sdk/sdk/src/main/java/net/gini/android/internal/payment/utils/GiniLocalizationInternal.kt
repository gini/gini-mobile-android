package net.gini.android.internal.payment.utils

import net.gini.android.health.api.response.CommunicationTone
import java.util.Locale

/**
 * [GiniLocalizationInternal] class to track record of language Internally (wrapper around [GiniLocalization])
 * because we don't want clients to see three languages as of now which are English, German and German Informal.
 * This class is implemented because we need to implement a workaround because of introduction of Informal
 * german language [IPC-492] and unfortunately, there is no formal or informal German according to IETF BCP 47
 * language tags and language packs provided by the system.
 *
 * That's why workaround is applied, which is,
 *
 * We have a default support in German, all the formal strings are placed in values -> strings.xml,
 * and informal are in values-de. So, if the customer wants support for informal, "de" locale will
 * be selected which will be decided by backend response[CommunicationTone], and all the informal
 * strings will be placed under this locale. It will respect all the German regions, which are austria,
 * switzerland etc.
 *
 * And we have a default locale, which is by default dedicated to so called "Formal German", but for
 * system it's not de, it is the default values folder, dedicated for fallback, So we ask system to
 * switch to a locale ("-1") in [languageLocale] which is not real, system will fall back to default,
 * which have our formal german strings.
 *
 * This workaround is applied to avoid putting if (tone == formal) getString (R.id.hello_formal)
 * else getString(R.id.hello_informal) everywhere.
 *
 * Important Note!!
 * As of now if user wants to override the strings, they have to put the same keys in default values
 * folder.
 * It will work as it is, but the customers who wants the support for Informal, and they want to
 * override the informal strings, they need to make this folder in their project values-de and
 * keep a string file in this folder and add the keys in this file (to override the informal strings),
 * for the formal no change is made.
 * */

internal enum class GiniLocalizationInternal(val readableName: String) {
    GERMAN("German"),
    ENGLISH("English"),
    GERMAN_INFORMAL("German_informal");

    fun languageLocale(): Locale {
        return when (this) {
            /**
             * setting -1 so system can fallback to default values folder. Detailed reason
             * is provided in above comments.
             * */
            GERMAN -> Locale("-1")
            GERMAN_INFORMAL -> Locale("de")
            else -> Locale("en")
        }
    }
}
