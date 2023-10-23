package net.gini.android.capture.internal.iban

import java.math.BigInteger
import java.util.regex.Pattern

/**
 * Checks that an IBAN string conforms to the IBAN standard.
 *
 * Internal use only.
 */
class IBANValidator {

    private val mPattern: Pattern = Pattern.compile("^[A-Z0-9]+$")

    /**
     * Verifies, that the IBAN string conforms to the IBAN standard.
     *
     * @param iban an IBAN string
     * @throws IllegalIBANException if the IBAN was not valid
     */
    @Throws(IllegalIBANException::class)
    fun validate(iban: String?) {
        if (iban.isNullOrEmpty()) {
            throw IllegalIBANException(IBANError.EMPTY)
        }
        val sanitizedIban = sanitizeIBAN(iban)
        val matcher = mPattern.matcher(sanitizedIban)
        if (!matcher.matches()) {
            throw IllegalIBANException(IBANError.INVALID_CHARACTERS)
        }
        validateCountryAndChecksum(sanitizedIban)
        validateLength(sanitizedIban)
        validateChecksum(sanitizedIban)
    }

    private fun getBban(iban: String): String {
        return iban.substring(4, iban.length)
    }

    private fun getCheckDigit(iban: String): String {
        return iban.substring(2, 4)
    }

    private fun getChecksum(iban: String): String {
        val countryCodeNumbers = lettersToNumbers(getCountryCode(iban))
        val checkDigit = getCheckDigit(iban)
        val bban = lettersToNumbers(getBban(iban))
        return bban + countryCodeNumbers + checkDigit
    }

    private fun getCountryCode(iban: String): String {
        return iban.substring(0, 2)
    }

    private fun lettersToNumbers(letters: String): String {
        val numbers = StringBuilder()
        val chars = letters.toCharArray()
        for (character in chars) {
            val number = character.code - 55
            if (number >= 10 && number <= 35) {
                numbers.append(number.toString())
            } else {
                numbers.append(character.toString())
            }
        }
        return numbers.toString()
    }

    private fun sanitizeIBAN(iban: String): String {
        var sanitizedIban = iban.trim { it <= ' ' }
        sanitizedIban = sanitizedIban.replace(" ", "")
        sanitizedIban = sanitizedIban.uppercase()
        return sanitizedIban
    }

    @Throws(IllegalIBANException::class)
    private fun validateChecksum(iban: String) {
        val bigInt = BigInteger(getChecksum(iban))
        val divisionResult = bigInt.divideAndRemainder(BigInteger("97"))
        if (divisionResult[1].compareTo(BigInteger.ONE) != 0) {
            throw IllegalIBANException(IBANError.INVALID_CHECKSUM)
        }
    }

    @Throws(IllegalIBANException::class)
    private fun validateCountryAndChecksum(iban: String) {
        if (!iban.matches("^[A-Z]{2}[0-9]{2}.*".toRegex())) {
            throw IllegalIBANException(IBANError.INVALID_FORMAT)
        }
    }

    @Throws(IllegalIBANException::class)
    private fun validateLength(iban: String) {
        val requiredLength = IBANKnowledge.countryIbanDictionary[getCountryCode(iban)]
            ?: throw IllegalIBANException(IBANError.INVALID_COUNTRY)
        if (iban.length > requiredLength) {
            throw IllegalIBANException(IBANError.TOO_LONG)
        }
        if (iban.length < requiredLength) {
            throw IllegalIBANException(IBANError.TOO_SHORT)
        }
    }

    internal enum class IBANError {
        EMPTY, INVALID_CHARACTERS, UNKNOWN_STRING_ERROR, INVALID_FORMAT, INVALID_COUNTRY, TOO_LONG, TOO_SHORT, INVALID_CHECKSUM
    }

    /**
     * Exception containing an [IBANError] for information about the reason why an
     * IBAN was not valid.
     */
    internal class IllegalIBANException(
        val iBANError: IBANError
    ) : RuntimeException() {

        override val message: String
            get() = "IBAN error: $iBANError"
    }

}