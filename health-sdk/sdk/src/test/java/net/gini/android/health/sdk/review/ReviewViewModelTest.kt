package net.gini.android.health.sdk.review

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.review.openWith.OpenWithPreferences
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by Alpár Szotyori on 13.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ReviewViewModelTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var giniHealth: GiniHealth? = null
    private var userPreferences: UserPreferences? = null
    private var context: Context? = null

    @Before
    fun setup() {
        giniHealth = mockk(relaxed = true)
        every { giniHealth!!.paymentFlow } returns MutableStateFlow<ResultWrapper<PaymentDetails>>(mockk()).asStateFlow()
        userPreferences = mockk(relaxed = true)
        context = getApplicationContext()
    }

    @After
    fun tearDown() {
        giniHealth = null
        userPreferences = null
        context = null
    }

    @Test
    fun `shows info bar on launch`() = runTest {
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk()).apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isTrue()
    }

    @Test
    fun `hides info bar after a delay`() = runTest {
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk()).apply {
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

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

            val viewModel = ReviewViewModel(giniHealth!!, mockk(), mockk())

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

            val viewModel = ReviewViewModel(giniHealth!!, paymentProviderApp, mockk())

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

    @Test
    fun `increments 'Open With' counter`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.id } returns "123"

        val openWithPreferences= OpenWithPreferences(context!!)

        val viewModel = ReviewViewModel(giniHealth!!, paymentProviderApp, mockk()).apply {
            this.openWithPreferences = openWithPreferences
        }

        // When
        viewModel.incrementOpenWithCounter()

        // Then
        coVerify { openWithPreferences.incrementCountForPaymentProviderId(paymentProviderApp.paymentProvider.id) }
    }

    @Test
    fun `returns 'RedirectToBank' when payment provider app supports GPC`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.gpcSupported } returns true

        val viewModel = ReviewViewModel(giniHealth!!, paymentProviderApp, mockk())

        // When
        val onPayment = viewModel.onPaymentButtonTapped()

        // Then
        assertThat(onPayment).isEqualTo(ReviewViewModel.PaymentNextStep.RedirectToBank)
    }

    @Test
    fun `returns 'ShowOpenWith' when payment provider app does not support GPC`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.gpcSupported } returns false

        val viewModel = ReviewViewModel(giniHealth!!, paymentProviderApp, mockk())

        // When
        val onPayment = viewModel.onPaymentButtonTapped()

        // Then
        assertThat(onPayment).isEqualTo(ReviewViewModel.PaymentNextStep.ShowOpenWithSheet)
    }

    @Test
    fun `returns 'OpenSharePdf' when payment provider app does not support GPC and 'Open With' was shown 3 times`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.gpcSupported } returns false
        every { paymentProviderApp.paymentProvider.id } returns "123"

        val openWithPreferences = mockk<OpenWithPreferences>()
        every { openWithPreferences.getLiveCountForPaymentProviderId(any()) } returns flowOf(3)

        val viewModel = ReviewViewModel(giniHealth!!, paymentProviderApp, mockk()).apply {
            this.openWithPreferences = openWithPreferences
        }
        viewModel.startObservingOpenWithCount()

        val onPayment = viewModel.onPaymentButtonTapped()
        assertThat(onPayment).isEqualTo(ReviewViewModel.PaymentNextStep.OpenSharePdf)
    }
}