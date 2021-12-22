package net.gini.android.health.sdk.review

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
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
            ResultWrapper.Success(
                PaymentDetails(
                    recipient = "",
                    iban = "iban",
                    amount = "amount",
                    purpose = "purpose",
                    extractions = null
                )
            )
        )

        val viewModel = ReviewViewModel(giniHealth!!)

        val validationsAsync = async { viewModel.paymentValidation.take(1).toList() }

        // Then
        val validations = validationsAsync.await()

        assertThat(validations).hasSize(1)
        assertThat(validations[0]).isNotEmpty()
    }

    @Test
    fun `does not validate payment details on every change`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        every { giniHealth!!.paymentFlow } returns MutableStateFlow(
            ResultWrapper.Success(
                PaymentDetails(
                    recipient = "recipient",
                    iban = "iban",
                    amount = "amount",
                    purpose = "purpose",
                    extractions = null
                )
            )
        )

        val viewModel = ReviewViewModel(giniHealth!!)

        val validations = mutableListOf<List<ValidationMessage>>()
        val collectJob = launch { viewModel.paymentValidation.collect { validations.add(it) } }

        // When
        viewModel.setRecipient("")
        viewModel.setIban("")
        viewModel.setAmount("")
        viewModel.setRecipient("foo")
        viewModel.setAmount("1.00")
        viewModel.setIban("DE1234")

        collectJob.cancel()

        // Then
        assertThat(validations).hasSize(1)
        assertThat(validations[0]).isEmpty()
    }

    @Test
    fun `emits only 'input field empty' errors when validating payment details after the extractions have loaded`() =
        testCoroutineRule.scope.runBlockingTest {
            // When
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "",
                        iban = "iban",
                        amount = "amount",
                        purpose = "purpose",
                        extractions = null
                    )
                )
            )

            val viewModel = ReviewViewModel(giniHealth!!)

            val validationsAsync = async { viewModel.paymentValidation.take(1).toList() }

            // Then
            val validations = validationsAsync.await()

            assertThat(validations).hasSize(1)
            assertThat(validations[0]).contains(
                ValidationMessage.Empty(PaymentField.Recipient)
            )
        }

    @Test
    fun `clears 'input field empty' error if the recipient field is not empty after input`() =
        testCoroutineRule.scope.runBlockingTest {
            // Given
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "",
                        iban = "iban",
                        amount = "1",
                        purpose = "purpose",
                        extractions = null
                    )
                )
            )

            val viewModel = ReviewViewModel(giniHealth!!)

            viewModel.paymentValidation.test {
                // When
                viewModel.setRecipient("recipient")

                // Then
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Recipient),
                )
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `clears 'input field empty' error if the iban field is not empty after input`() =
        testCoroutineRule.scope.runBlockingTest {
            // Given
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "recipient",
                        iban = "",
                        amount = "1",
                        purpose = "purpose",
                        extractions = null
                    )
                )
            )

            val viewModel = ReviewViewModel(giniHealth!!)

            viewModel.paymentValidation.test {
                // When
                viewModel.setIban("iban")

                // Then
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Iban),
                )
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `clears 'input field empty' error if the amount field is not empty after input`() =
        testCoroutineRule.scope.runBlockingTest {
            // Given
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "recipient",
                        iban = "iban",
                        amount = "",
                        purpose = "purpose",
                        extractions = null
                    )
                )
            )

            val viewModel = ReviewViewModel(giniHealth!!)

            viewModel.paymentValidation.test {
                // When
                viewModel.setAmount("1")

                // Then
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Amount),
                )
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `clears 'input field empty' error if the purpose field is not empty after input`() =
        testCoroutineRule.scope.runBlockingTest {
            // Given
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "recipient",
                        iban = "iban",
                        amount = "1",
                        purpose = "",
                        extractions = null
                    )
                )
            )

            val viewModel = ReviewViewModel(giniHealth!!)

            viewModel.paymentValidation.test {
                // When
                viewModel.setPurpose("purpose")

                // Then
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Purpose),
                )
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `clears only 'input field empty' errors if the field is not empty after input`() =
        testCoroutineRule.scope.runBlockingTest {
            // Given
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "recipient",
                        iban = "",
                        amount = "1",
                        purpose = "purpose",
                        extractions = null
                    )
                )
            )

            val viewModel = ReviewViewModel(giniHealth!!)

            // Validate all fields to also get an invalid iban validation message
            viewModel.onPayment()

            viewModel.paymentValidation.test {
                // When
                viewModel.setIban("iban")

                // Then
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Iban),
                    ValidationMessage.InvalidIban,
                )
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.InvalidIban,
                )

                cancelAndConsumeRemainingEvents()
            }
        }

}