package net.gini.android.capture

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class AmountTest {

    @Test
    fun `parses value and currency code`() {
        assertThat(Amount.parse("12.50:EUR")).isEqualTo(Amount(BigDecimal("12.50"), AmountCurrency.EUR))
    }

    @Test
    fun `formats the amount to pay as value and currency code`() {
        assertThat(Amount(BigDecimal("12.50"), AmountCurrency.EUR).amountToPay()).isEqualTo("12.50:EUR")
    }

    @Test
    fun `rejects a string without a currency code`() {
        val exception = assertThrows(IllegalArgumentException::class.java) { Amount.parse("12.50") }

        assertThat(exception).hasMessageThat().contains("Should be `value:currency_code`")
    }

    @Test
    fun `rejects a string with more than two parts`() {
        assertThrows(IllegalArgumentException::class.java) { Amount.parse("12.50:EUR:extra") }
    }

    @Test
    fun `rejects a value that is not a number`() {
        val exception = assertThrows(IllegalArgumentException::class.java) { Amount.parse("abc:EUR") }

        assertThat(exception).hasMessageThat().contains("to BigDecimal")
    }

    @Test
    fun `rejects an unknown currency code`() {
        val exception = assertThrows(IllegalArgumentException::class.java) { Amount.parse("12.50:XXX") }

        assertThat(exception).hasMessageThat().contains("to AmountCurrency")
    }
}
