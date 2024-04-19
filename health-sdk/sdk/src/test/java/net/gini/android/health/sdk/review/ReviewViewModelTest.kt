package net.gini.android.health.sdk.review

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by Alpár Szotyori on 13.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
class ReviewViewModelTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var giniHealth: GiniHealth? = null
    private var userPreferences: UserPreferences? = null

    @Before
    fun setup() {
        giniHealth = mockk(relaxed = true)
        every { giniHealth!!.paymentFlow } returns MutableStateFlow<ResultWrapper<PaymentDetails>>(mockk()).asStateFlow()
        userPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        giniHealth = null
        userPreferences = null
    }

    @Test
    fun `shows info bar on launch`() = runTest {
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
            listOf()
        ))
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent).apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isTrue()
    }

    @Test
    fun `hides info bar after a delay`() = runTest {
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
            listOf()
        ))

        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent).apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        advanceTimeBy(ReviewViewModel.SHOW_INFO_BAR_MS + 100)

        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isFalse()
    }

    @Test
    fun `validates payment details when the extractions have loaded`() = runTest {
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

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
            listOf()
        ))

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

        val validationsAsync = async { viewModel.paymentValidation.take(1).toList() }

        // Then
        val validations = validationsAsync.await()

        assertThat(validations).hasSize(1)
        assertThat(validations[0]).isNotEmpty()
    }

    @Test
    fun `validates payment details on every change`() = runTest {
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

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
            listOf()
        ))

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

        viewModel.paymentValidation.test {
            // Precondition
            assertThat(awaitItem()).isEmpty()

            // When - Then
            viewModel.setRecipient("")
            assertThat(awaitItem()).contains(ValidationMessage.Empty(PaymentField.Recipient))

            viewModel.setIban("")
            assertThat(awaitItem()).contains(ValidationMessage.Empty(PaymentField.Iban))

            viewModel.setAmount("")
            assertThat(awaitItem()).contains(ValidationMessage.Empty(PaymentField.Amount))

            viewModel.setRecipient("foo")
            assertThat(awaitItem()).doesNotContain(ValidationMessage.Empty(PaymentField.Recipient))

            viewModel.setAmount("1.00")
            assertThat(awaitItem()).doesNotContain(ValidationMessage.Empty(PaymentField.Amount))

            viewModel.setIban("DE1234")
            assertThat(awaitItem()).doesNotContain(ValidationMessage.Empty(PaymentField.Iban))

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits only 'input field empty' errors when validating payment details after the extractions have loaded`() =
        runTest {
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

            val paymentComponent = mockk<PaymentComponent>(relaxed = true)
            every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
                listOf()
            ))

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

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
        runTest {
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

            val paymentComponent = mockk<PaymentComponent>(relaxed = true)
            every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
                listOf()
            ))

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

            viewModel.paymentValidation.test {
                // Precondition
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Recipient),
                )

                // When
                viewModel.setRecipient("recipient")

                // Then
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `clears 'input field empty' error if the iban field is not empty after input`() =
        runTest {
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

            val paymentComponent = mockk<PaymentComponent>(relaxed = true)
            every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
                listOf()
            ))

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

            viewModel.paymentValidation.test {
                // Precondition
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.Empty(PaymentField.Iban),
                )

                // When
                viewModel.setIban("iban")

                // Then
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `clears 'input field empty' error if the amount field is not empty after input`() =
        runTest {
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

            val paymentComponent = mockk<PaymentComponent>(relaxed = true)
            every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
                listOf()
            ))

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

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
        runTest {
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

            val paymentComponent = mockk<PaymentComponent>(relaxed = true)
            every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
                listOf()
            ))

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk(), paymentComponent)

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
    fun `clears 'invalid IBAN' error after modifying the IBAN`() =
        runTest {
            // Given
            every { giniHealth!!.paymentFlow } returns MutableStateFlow(
                ResultWrapper.Success(
                    PaymentDetails(
                        recipient = "recipient",
                        iban = "iban",
                        amount = "1",
                        purpose = "purpose",
                        extractions = null
                    )
                )
            )

            val paymentProviderApp = mockk<PaymentProviderApp>()
            every { paymentProviderApp.installedPaymentProviderApp } returns mockk()

            val paymentComponent = mockk<PaymentComponent>(relaxed = true)
            every { paymentComponent.paymentProviderAppsFlow } returns MutableStateFlow(PaymentProviderAppsState.Success(
                listOf()
            ))

            val viewModel = ReviewViewModel(giniHealth!!, paymentProviderApp, mockk(), paymentComponent)

            // Validate all fields to also get an invalid iban validation message
            viewModel.onPayment()

            viewModel.paymentValidation.test {
                // When
                viewModel.setIban("iban2")

                // Then
                assertThat(awaitItem()).containsExactly(
                    ValidationMessage.InvalidIban,
                )
                assertThat(awaitItem()).isEmpty()

                cancelAndConsumeRemainingEvents()
            }
        }

}