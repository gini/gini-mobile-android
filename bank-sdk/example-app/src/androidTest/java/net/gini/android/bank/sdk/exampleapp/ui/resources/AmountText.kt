package net.gini.android.bank.sdk.exampleapp.ui.resources

import java.math.BigDecimal

/**
 * Turns a formatted amount as rendered on screen into a number.
 *
 * Needed because the smoke tests compare totals before and after a toggle, and
 * "1.234,56 €" against "1.334,56 €" is a string comparison, not an ordering. Shared
 * between the Skonto page object (footer total, amount field) and the tests that read the
 * extraction list, so both read a value the same way.
 */
object AmountText {

    /**
     * Parses [raw] into a [BigDecimal], ignoring currency symbols, codes and whitespace.
     *
     * The SDK formats amounts through the device locale, so the separators are not known
     * ahead of time: a German device renders "1.234,56 €" and an English one "1,234.56 €".
     * Whichever of `.` or `,` appears *last* is the decimal separator; every earlier one is
     * grouping and is dropped. A value with no separator at all is read as a whole number.
     *
     * Gini's own amount strings carry a currency suffix after a colon ("123.45:EUR"); the
     * colon and everything after it is cut before parsing.
     *
     * A separator followed by anything other than one or two digits is treated as grouping,
     * so "1.234" reads as 1234 and a trailing "1.234," as 1234.
     *
     * @throws IllegalArgumentException when [raw] holds no digits — a caller that got an
     *   empty or absent field should fail loudly rather than silently compare against zero.
     */
    fun parse(raw: String?): BigDecimal {
        val withoutCurrencyCode = (raw ?: "").substringBefore(':')
        val digitsAndSeparators = withoutCurrencyCode.replace("[^0-9.,]".toRegex(), "")
        require(digitsAndSeparators.isNotEmpty()) {
            "No number found in amount text '$raw'."
        }

        val lastSeparator = digitsAndSeparators.indexOfLast { it == '.' || it == ',' }
        if (lastSeparator == -1) return BigDecimal(digitsAndSeparators)

        val fractionPart = digitsAndSeparators.substring(lastSeparator + 1)
        // A trailing separator, or a group of three digits after the last one, means the
        // separator was grouping rather than decimal ("1.234" is one thousand two hundred
        // thirty-four, not 1.234). Only a 1-2 digit tail is a real fraction.
        if (fractionPart.length !in 1..2) {
            return BigDecimal(digitsAndSeparators.replace(".", "").replace(",", ""))
        }

        val integerPart = digitsAndSeparators.substring(0, lastSeparator)
            .replace(".", "")
            .replace(",", "")
        return BigDecimal("${integerPart.ifEmpty { "0" }}.$fractionPart")
    }
}
