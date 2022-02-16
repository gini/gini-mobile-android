package net.gini.android.bank.sdk.util

import com.google.common.truth.Truth.assertThat
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.sdk.error.AmountParsingException
import org.junit.Test

/**
 * Created by Alpár Szotyori on 15.02.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

class ResolvePaymentInputExtensionsTest {

    @Test
    fun `parses amount according to amount entity spec`() {
        // Given
        val amount = "12.46"

        // When
        val formattedAmount = parseAmount(amount)

        // Then
        assertThat(formattedAmount).isEqualTo("12.46:EUR")
    }

    private fun parseAmount(amount: String) =
        ResolvePaymentInput("recipient", "iban", amount, "purpose").parseAmountToBackendFormat()

    @Test(expected = AmountParsingException::class)
    fun `throws AmountParsingException if amount contains currency symbol at the end`() {
        // Given
        val amount = "12.46 €"

        // When
        val formattedAmount = parseAmount(amount)

        // Then
        assertThat(formattedAmount).isEqualTo("12.46:EUR")
    }

    @Test(expected = AmountParsingException::class)
    fun `throws AmountParsingException if amount contains currency symbol at the beginning`() {
        // Given
        val amount = "£ 12.46"

        // When
        val formattedAmount = parseAmount(amount)

        // Then
        assertThat(formattedAmount).isEqualTo("12.46:EUR")
    }

    @Test(expected = AmountParsingException::class)
    fun `throws AmountParsingException if amount is not in English format (decimal separator is not dot)`() {
        // Given
        val amount = "12,46"

        // When
        val formattedAmount = parseAmount(amount)

        // Then
        assertThat(formattedAmount).isEqualTo("12.46:EUR")
    }

}