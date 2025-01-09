package net.gini.android.internal.payment.review.reviewComponent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.review.PaymentField
import net.gini.android.internal.payment.review.ValidationMessage
import net.gini.android.internal.payment.utils.GiniPaymentManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ReviewComponentTest {

    private lateinit var context: Context
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var giniPaymentManager: GiniPaymentManager
    private lateinit var giniPaymentModule: GiniInternalPaymentModule
    private val testCoroutineDispatcher = UnconfinedTestDispatcher()
    private val testCoroutineScope =
        TestScope(testCoroutineDispatcher + Job())

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniPaymentTheme)
        paymentComponent = mockk(relaxed = true)
        giniPaymentManager = mockk(relaxed = true)
        giniPaymentModule = mockk(relaxed = true)
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @Test
    fun `sets payment button not enabled if payment details are empty`() = runTest {
        // Given
        val paymentDetails = PaymentDetails(
            recipient = "",
            iban = "",
            amount = "",
            purpose = ""
        )

        // When
        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = testCoroutineScope
        )

        // Then
        reviewComponent.isPaymentButtonEnabled.test {
            val validation = awaitItem()
            assertThat(validation).isFalse()
        }
    }

    @Test
    fun `sets payment button not enabled if still loading`() = runTest {
        //Given
        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Loading())

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = testCoroutineScope
        )

        // Then
        reviewComponent.isPaymentButtonEnabled.test {
            val validation = awaitItem()
            assertThat(validation).isFalse()
        }
    }

    @Test
    fun `sets payment button enabled`() = runTest {
        // Given
        val paymentDetails = PaymentDetails(
            recipient = "",
            iban = "",
            amount = "",
            purpose = ""
        )

        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = testCoroutineScope
        )

        reviewComponent.isPaymentButtonEnabled.test {
            val validation = awaitItem()
            assertThat(validation).isFalse()

            // When
            reviewComponent.setRecipient("recipient")
            awaitItem()

            reviewComponent.setAmount("30")
            awaitItem()

            reviewComponent.setIban("GR96 0810 0010 0000 0123 4567 890")
            awaitItem()

            reviewComponent.setPurpose("purpose")

            // Then
            val enabledValidation = awaitItem()
            assertThat(enabledValidation).isTrue()

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits only 'input field empty' errors when validating payment details after the extractions have loaded`() = runTest {
        val paymentDetails = PaymentDetails(
            recipient = "",
            iban = "iban",
            amount = "amount",
            purpose = "purpose",
        )

        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(
            SelectedPaymentProviderAppState.NothingSelected)

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = CoroutineScope(Dispatchers.Main)
        )

        reviewComponent.paymentValidation.test {
            val validation = awaitItem()
            assertThat(validation.size).isEqualTo(1)
            assertThat(validation[0]).isEqualTo(ValidationMessage.Empty(PaymentField.Recipient))
        }
    }

    @Test
    fun `clears 'input field empty' error if the recipient field is not empty after input`() = runTest {
        //Given
        val paymentDetails = PaymentDetails(
            recipient = "",
            iban = "iban",
            amount = "amount",
            purpose = "purpose",
        )

        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = CoroutineScope(Dispatchers.Main)
        )

        reviewComponent.paymentValidation.test {
            val validation = awaitItem()
            assertThat(validation.size).isEqualTo(1)
            assertThat(validation[0]).isEqualTo(ValidationMessage.Empty(PaymentField.Recipient))

            //When
            reviewComponent.setRecipient("recipient")

            //Then
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `clears 'input field empty' error if the iban field is not empty after input`() = runTest {
        //Given
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            iban = "",
            amount = "amount",
            purpose = "purpose",
        )

        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = testCoroutineScope
        )

        reviewComponent.paymentValidation.test {
            val validation = awaitItem()
            assertThat(validation.size).isEqualTo(1)
            assertThat(validation[0]).isEqualTo(ValidationMessage.Empty(PaymentField.Iban))

            //When
            reviewComponent.setIban("iban")

            //Then
            assertThat(awaitItem()).isEmpty()
        }
    }
//
    @Test
    fun `clears 'input field empty' error if the amount field is not empty after input`() = runTest {
        //Given
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            iban = "iban",
            amount = "",
            purpose = "purpose",
        )

        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = testCoroutineScope
        )

        reviewComponent.paymentValidation.test {
            val validation = awaitItem()
            assertThat(validation.size).isEqualTo(1)
            assertThat(validation[0]).isEqualTo(ValidationMessage.Empty(PaymentField.Amount))

            //When
            reviewComponent.setAmount("amount")

            //Then
            assertThat(awaitItem()).isEmpty()
        }
    }


    @Test
    fun `clears 'input field empty' error if the purpose field is not empty after input`() = runTest {
        //Given
        val paymentDetails = PaymentDetails(
            recipient = "recipient",
            iban = "iban",
            amount = "amount",
            purpose = "",
        )

        every { giniPaymentModule.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.NothingSelected)

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniInternalPaymentModule = giniPaymentModule,
            coroutineScope = testCoroutineScope
        )

        reviewComponent.paymentValidation.test {
            val validation = awaitItem()
            assertThat(validation.size).isEqualTo(1)
            assertThat(validation[0]).isEqualTo(ValidationMessage.Empty(PaymentField.Purpose))

            //When
            reviewComponent.setPurpose("purpose")

            //Then
            assertThat(awaitItem()).isEmpty()
        }
    }
}