package net.gini.android.internal.payment.review.reviewComponent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.utils.GiniPaymentManager
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
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val testCoroutineScope =
        TestScope(testCoroutineDispatcher + Job())

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniPaymentTheme)
        paymentComponent = mockk(relaxed = true)
        giniPaymentManager = mockk(relaxed = true)
        giniPaymentModule = mockk(relaxed = true)
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

}