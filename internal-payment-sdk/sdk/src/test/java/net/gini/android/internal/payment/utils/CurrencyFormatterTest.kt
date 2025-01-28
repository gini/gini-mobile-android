package net.gini.android.internal.payment.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `formatCurrency handles empty input`() {
        assertThat(formatCurrency("")).isEqualTo("")
    }

    @Test
    fun `formatCurrency handles integers without decimal input`() {
        assertThat(formatCurrency("56")).isEqualTo("56.00")
        assertThat(formatCurrency("12345")).isEqualTo("12,345.00")
        assertThat(formatCurrency("0")).isEqualTo("0.00")
    }

    @Test
    fun `formatCurrency handles valid decimal input with dot`() {
        assertThat(formatCurrency("56.1")).isEqualTo("56.10")
        assertThat(formatCurrency("1234567")).isEqualTo("1,234,567.00")
        assertThat(formatCurrency("4,2535")).isEqualTo("4.25")
        assertThat(formatCurrency("56,5")).isEqualTo("56.50")
        assertThat(formatCurrency("56,")).isEqualTo("56.00")
        assertThat(formatCurrency("56,11")).isEqualTo("56.11")
        assertThat(formatCurrency("0.678")).isEqualTo("0.68")
        assertThat(formatCurrency("0.555")).isEqualTo("0.56")
        assertThat(formatCurrency("5.5")).isEqualTo("5.50")
        assertThat(formatCurrency("0.5")).isEqualTo("0.50")
        assertThat(formatCurrency("1234.56")).isEqualTo("1,234.56")
        assertThat(formatCurrency("123456")).isEqualTo("123,456.00")
        assertThat(formatCurrency("1234567")).isEqualTo("1,234,567.00")
    }

    @Test
    fun `formatCurrency handles valid decimal input with comma`() {
        assertThat(formatCurrency("56,1")).isEqualTo("56.10")
        assertThat(formatCurrency("56,5")).isEqualTo("56.50")
        assertThat(formatCurrency("5,5")).isEqualTo("5.50")
        assertThat(formatCurrency("0,5")).isEqualTo("0.50")
        assertThat(formatCurrency("1234,56")).isEqualTo("1,234.56")

    }

    @Test
    fun `formatCurrency handles input with both comma and dot (comma as decimal)`() {
        assertThat(formatCurrency("1.234,56")).isEqualTo("1,234.56")
    }

    @Test
    fun `formatCurrency handles input with both comma and dot (dot as decimal)`() {
        assertThat(formatCurrency("1,234.56")).isEqualTo("1,234.56")
    }

    @Test
    fun `formatCurrency handles input with leading and trailing spaces`() {
        assertThat(formatCurrency(" 56.1 ")).isEqualTo("56.10")
    }

    @Test
    fun `formatCurrency handles input with non-numeric characters and return zero as it is invalid input`() {
        assertThat(formatCurrency("a56.1b")).isEqualTo("0.00")
        assertThat(formatCurrency("abc")).isEqualTo("0.00")
        assertThat(formatCurrency("123-456")).isEqualTo("0.00")
    }

    @Test
    fun `hasMoreThenTwoDecimalPlaces handles valid input`() {
        assertThat(isValidTwoDecimalNumber("1.234")).isFalse()
        assertThat(isValidTwoDecimalNumber("1,234")).isFalse()
        assertThat(isValidTwoDecimalNumber("1.2")).isFalse()
        assertThat(isValidTwoDecimalNumber("1,2")).isFalse()
        assertThat(isValidTwoDecimalNumber("1.23")).isTrue()
        assertThat(isValidTwoDecimalNumber("1,23")).isTrue()
        assertThat(isValidTwoDecimalNumber("1")).isFalse()
        assertThat(isValidTwoDecimalNumber("1 ")).isFalse()
        assertThat(isValidTwoDecimalNumber(" 1")).isFalse()
        assertThat(isValidTwoDecimalNumber(" 1 ")).isFalse()
        assertThat(isValidTwoDecimalNumber("1,")).isFalse()
        assertThat(isValidTwoDecimalNumber("1.")).isFalse()
        assertThat(isValidTwoDecimalNumber("1,123.123")).isFalse()
        assertThat(isValidTwoDecimalNumber("")).isFalse()
        assertThat(isValidTwoDecimalNumber("a")).isFalse()
        assertThat(isValidTwoDecimalNumber(".")).isFalse()
        assertThat(isValidTwoDecimalNumber(".1")).isFalse()
        assertThat(isValidTwoDecimalNumber(".12")).isTrue()
        assertThat(isValidTwoDecimalNumber("1.123,123")).isFalse()
        assertThat(isValidTwoDecimalNumber("1.123")).isFalse()
        assertThat(isValidTwoDecimalNumber("1.12")).isTrue()
    }
}
