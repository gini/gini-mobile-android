package net.gini.android.merchant.sdk.review.reviewComponent

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
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.util.GiniPaymentManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ReviewComponentTest {
    private lateinit var context: Context
    private lateinit var giniMerchant: GiniMerchant
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var giniPaymentManager: GiniPaymentManager
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val testCoroutineScope =
        TestScope(testCoroutineDispatcher + Job())

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniMerchantTheme)
        giniMerchant = mockk(relaxed = true)
        paymentComponent = mockk(relaxed = true)
        giniPaymentManager = mockk(relaxed = true)
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
        every { giniMerchant.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniMerchant = giniMerchant,
            giniPaymentManager = giniPaymentManager,
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
        every { giniMerchant.paymentFlow } returns MutableStateFlow(ResultWrapper.Loading())

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniMerchant = giniMerchant,
            giniPaymentManager = giniPaymentManager,
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

        every { giniMerchant.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(paymentDetails))

        val reviewComponent = ReviewComponent(
            paymentComponent = paymentComponent,
            reviewConfig = mockk(relaxed = true),
            giniMerchant = giniMerchant,
            giniPaymentManager = giniPaymentManager,
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