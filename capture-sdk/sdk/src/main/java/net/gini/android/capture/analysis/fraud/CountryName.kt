package net.gini.android.capture.analysis.fraud

import java.util.IllformedLocaleException
import java.util.Locale

/**
 * Returns the name of the country with the ISO 3166-1 alpha-2 [code] in the language of
 * [locale], e.g. "Polen" for `PL` in German. Returns [code] itself when the JDK does not know it.
 */
internal fun countryDisplayName(code: String, locale: Locale): String {
    val name = try {
        Locale.Builder().setRegion(code.uppercase()).build().getDisplayCountry(locale)
    } catch (_: IllformedLocaleException) {
        null
    }
    return name?.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) } ?: code
}
