package net.gini.android.bank.sdk

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.bank.api.GiniBankAPI
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.sdk.error.AmountParsingException
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by Alpár Szotyori on 15.02.22.
 *
 * Copyright (c) 2022 Gini GmbH.
 */

@ExperimentalCoroutinesApi
class GiniBankTest {

    lateinit var bankApi: GiniBankAPI

    @Before
    fun setup() {
        bankApi = mockk()
        GiniBank.setGiniApi(bankApi)
    }

    @After
    fun teardown() {
        GiniBank.releaseGiniApi()
    }

    @Test
    fun `amount is parsed and formatted according to amount entity spec when resolving payment requests`() = runTest {
        // Given
        val resolvePaymentInputSlot = slot<ResolvePaymentInput>()
        coEvery {
            bankApi.documentManager.resolvePaymentRequest(
                any(),
                capture(resolvePaymentInputSlot)
            )
        } returns ResolvedPayment("", "", "", "", "", "", ResolvedPayment.Status.OPEN)

        val resolvePaymentInfo = ResolvePaymentInput("recipient", "iban", "12.46", "purpose")

        // When
        GiniBank.resolvePaymentRequest("1234", resolvePaymentInfo)

        // Then
        assertThat(resolvePaymentInputSlot.captured.amount).isEqualTo("12.46:EUR")
    }

    @Test(expected = AmountParsingException::class)
    fun `throws AmountParsingException if amount cannot be formatted according to amount entity spec when resolving payment requests`() = runTest {
        // Given
        val resolvePaymentInputSlot = slot<ResolvePaymentInput>()
        coEvery {
            bankApi.documentManager.resolvePaymentRequest(
                any(),
                capture(resolvePaymentInputSlot)
            )
        } returns ResolvedPayment("", "", "", "", "", "", ResolvedPayment.Status.OPEN)

        val resolvePaymentInfo = ResolvePaymentInput("recipient", "iban", "12.46€", "purpose")

        // When
        GiniBank.resolvePaymentRequest("1234", resolvePaymentInfo)

        // Then
        assertThat(resolvePaymentInputSlot.captured.amount).isEqualTo("12.46:EUR")
    }

    @Test
    fun `return reasons dialog is enabled by default`() {
        assertThat(GiniBank.enableReturnReasons).isTrue()
    }
}