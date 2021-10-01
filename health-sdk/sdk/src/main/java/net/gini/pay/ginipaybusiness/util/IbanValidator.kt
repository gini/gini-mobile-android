package net.gini.pay.ginipaybusiness.util

import java.util.*

private val countryIbanLength = mapOf(
    "AL" to 28, "AD" to 24, "AT" to 20, "AZ" to 28, "BH" to 22, "BE" to 16,
    "BA" to 20, "BR" to 29, "BG" to 22, "CR" to 21, "HR" to 21, "CY" to 28,
    "CZ" to 24, "DK" to 18, "DO" to 28, "EE" to 20, "FO" to 18, "FI" to 18,
    "FR" to 27, "GE" to 22, "DE" to 22, "GI" to 23, "GB" to 22, "GR" to 27,
    "GL" to 18, "GT" to 28, "HU" to 28, "IS" to 26, "IE" to 22, "IL" to 23,
    "IT" to 27, "KZ" to 20, "KW" to 30, "LV" to 21, "LB" to 28, "LT" to 20,
    "LU" to 20, "MK" to 19, "MT" to 31, "MR" to 27, "MU" to 30, "MD" to 24,
    "MC" to 27, "ME" to 22, "NL" to 18, "NO" to 15, "PK" to 24, "PS" to 29,
    "PL" to 28, "PT" to 25, "RO" to 24, "SM" to 27, "SA" to 24, "RS" to 22,
    "SK" to 24, "SI" to 19, "ES" to 24, "SE" to 24, "TN" to 24, "TR" to 26,
    "AE" to 23, "VG" to 24, "CH" to 21
)

private const val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

internal fun isValidIban(iban: String): Boolean {
    var ibanNumber = iban.filter { !it.isWhitespace() }.uppercase(Locale.US)

    if (countryIbanLength[ibanNumber.take(2)] != ibanNumber.length) return false

    if (ibanNumber.contains("[^0-9A-Z]+".toRegex())) return false

    ibanNumber = ibanNumber.substring(startIndex = 4) + ibanNumber.substring(0, 4)
    val number = ibanNumber.map { it.toString() }.joinToString(separator = "") { char ->
        if (char in alphabet) (alphabet.indexOf(char) + 10).toString() else char
    }.toBigInteger()

    if (number.mod(97.toBigInteger()) != 1.toBigInteger()) return false
    return true
}