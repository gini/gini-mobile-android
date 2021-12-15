package net.gini.android.health.sdk.review

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.fail
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.test.TestCoroutineRule
import org.junit.*

/**
 * Created by Alpár Szotyori on 13.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
class ReviewViewModelTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private var giniHealth: GiniHealth? = null
    private var userPreferences: UserPreferences? = null

    @Before
    fun setup() {
        giniHealth = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        giniHealth = null
        userPreferences = null
    }

    @Test
    fun `shows info bar on launch`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        val viewModel = ReviewViewModel(giniHealth!!).apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isTrue()
    }

    @Test
    fun `hides info bar after a delay`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        val viewModel = ReviewViewModel(giniHealth!!).apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        testCoroutineRule.scope.advanceTimeBy(ReviewViewModel.SHOW_INFO_BAR_MS)

        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isFalse()
    }

    @Test
    fun `validates payment details when the extractions have loaded`() = testCoroutineRule.scope.runBlockingTest {
        // When
        every { giniHealth!!.paymentFlow } returns MutableStateFlow(
            ResultWrapper.Success(PaymentDetails(
                recipient = "",
                iban = "iban",
                amount = "amount",
                purpose = "purpose",
                extractions = null
            )))

        val viewModel = ReviewViewModel(giniHealth!!)

        val validationsAsync = async { viewModel.paymentValidation.take(1).toList() }

        // Then
        val validations = validationsAsync.await()

        assertThat(validations).hasSize(1)
        assertThat(validations[0]).contains(
            ValidationMessage.Empty(PaymentField.Recipient)
        )
    }

    // validates payment details on every change after the extractions have been loaded and emits only "input field empty" errors
    @Test
    fun `validates payment details on every change after the extractions have been loaded`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        every { giniHealth!!.paymentFlow } returns MutableStateFlow(
            ResultWrapper.Success(PaymentDetails(
            recipient = "recipient",
            iban = "iban",
            amount = "amount",
            purpose = "purpose",
            extractions = null
        )))

        val viewModel = ReviewViewModel(giniHealth!!)

        val validationsAsync = async { viewModel.paymentValidation.take(7).toList() }

        // When
        viewModel.setRecipient("")
        viewModel.setIban("")
        viewModel.setAmount("")
        viewModel.setRecipient("foo")
        viewModel.setAmount("1.00")
        viewModel.setIban("DE1234")

        // Then
        val validations = validationsAsync.await()

        assertThat(validations).hasSize(7)
        assertThat(validations[0]).isEmpty()
        assertThat(validations[1]).contains(
            ValidationMessage.Empty(PaymentField.Recipient)
        )
        assertThat(validations[2]).containsExactly(
            ValidationMessage.Empty(PaymentField.Recipient),
            ValidationMessage.Empty(PaymentField.Iban)
        )
        assertThat(validations[3]).containsExactly(
            ValidationMessage.Empty(PaymentField.Recipient),
            ValidationMessage.Empty(PaymentField.Iban),
            ValidationMessage.Empty(PaymentField.Amount)
        )
        assertThat(validations[4]).containsExactly(
            ValidationMessage.Empty(PaymentField.Iban),
            ValidationMessage.Empty(PaymentField.Amount)
        )
        assertThat(validations[5]).containsExactly(
            ValidationMessage.Empty(PaymentField.Iban)
        )
        assertThat(validations[6]).isEmpty()
    }

    @Test
    fun `does not validate payment details on every change if the extractions have not been loaded`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        every { giniHealth!!.paymentFlow } returns MutableStateFlow(ResultWrapper.Loading())

        val viewModel = ReviewViewModel(giniHealth!!)

        val validationsAsync = async { viewModel.paymentValidation.take(1).toList() }

        // When
        viewModel.setRecipient("")
        viewModel.setIban("")
        viewModel.setAmount("")
        viewModel.setRecipient("foo")

        // Then
        try {
            withTimeout(1) { validationsAsync.await() }
            fail("there was an unexpected emission in the paymentValidation flow")
        } catch (e: Exception) {
            validationsAsync.cancel()
            assertThat(e).isInstanceOf(TimeoutCancellationException::class.java)
        }
    }

    @Test
    fun `emits only 'input field empty' errors when validating payment details on every change`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        every { giniHealth!!.paymentFlow } returns MutableStateFlow(
            ResultWrapper.Success(PaymentDetails(
                recipient = "recipient",
                iban = "iban",
                amount = "amount",
                purpose = "purpose",
                extractions = null
            )))

        val viewModel = ReviewViewModel(giniHealth!!)

        val validationsAsync = async { viewModel.paymentValidation.take(3).toList() }

        // When
        viewModel.setIban("")
        viewModel.setIban("DE1234")

        // Then
        val validations = validationsAsync.await()

        assertThat(validations).hasSize(3)
        assertThat(validations[0]).isEmpty()
        assertThat(validations[1]).contains(
            ValidationMessage.Empty(PaymentField.Iban)
        )
        assertThat(validations[2]).isEmpty()
    }

}