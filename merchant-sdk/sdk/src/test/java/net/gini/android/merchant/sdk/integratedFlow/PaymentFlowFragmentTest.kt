package net.gini.android.merchant.sdk.integratedFlow

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.utils.PaymentNextStep
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.util.DisplayedScreen
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PaymentFlowFragmentTest {
    private var context: Context? = null
    private var giniMerchant : GiniMerchant? = null
    private var paymentComponent: PaymentComponent? = null
    private var paymentFlowViewModel: PaymentFlowViewModel? = null
    private var giniInternalPaymentModule: GiniInternalPaymentModule? = null
    private lateinit var viewModelFactory: ViewModelProvider.Factory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniMerchantTheme)

        giniInternalPaymentModule = mockk<GiniInternalPaymentModule>(relaxed = true)

        giniMerchant = mockk<GiniMerchant>(relaxed = true)
        every { giniMerchant!!.eventsFlow } returns MutableStateFlow(mockk(relaxed = true))

        paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniInternalPaymentModule!!.paymentComponent } returns paymentComponent!!
        every { giniMerchant!!.giniInternalPaymentModule } returns giniInternalPaymentModule!!

        paymentFlowViewModel = mockk<PaymentFlowViewModel>(relaxed = true)
        every { paymentFlowViewModel!!.giniMerchant } returns giniMerchant!!
        every { paymentFlowViewModel!!.paymentComponent } returns paymentComponent!!
        every { paymentFlowViewModel!!.backButtonEvent } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.shareWithFlowStarted } returns MutableStateFlow(false)
        every { paymentFlowViewModel!!.giniInternalPaymentModule } returns giniInternalPaymentModule!!
        every { paymentFlowViewModel!!.giniInternalPaymentModule.paymentComponent } returns paymentComponent!!
        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return paymentFlowViewModel as T
            }
        }
    }

    @Test
    fun `shows payment component bottom sheet on startup`() = runTest {
        // Given
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns DisplayedScreen.Nothing
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.paymentFlowConfiguration } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentFlowConfiguration?.shouldShowReviewFragment } returns false
        val fragment = PaymentFragment.newInstance(
            giniMerchant = giniMerchant!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = mockk(relaxed = true),
            viewModelFactory = viewModelFactory
        )

        // When
        launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
            fragment
        }

        // Then
        verify { fragment.showPaymentComponentBottomSheet() }
    }

    @Test
    fun `shows review fragment when payment button is tapped and config allows showing of review fragment`() = runTest {
        // Given
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldHandleErrorsInternally } returns false
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldShowReviewFragment } returns true
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.isAmountFieldEditable } returns false
        every { paymentFlowViewModel!!.giniMerchant!!.createFragment("", "", "", "") } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.giniMerchant.eventsFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { paymentFlowViewModel!!.paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            mockk(relaxed = true)
        )
        val fragment = PaymentFragment.newInstance(
            giniMerchant = giniMerchant!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = PaymentFlowConfiguration(shouldShowReviewFragment = true),
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
            fragment
        }

        // When
        fragment.handlePayFlow()

        // Then
        verify { fragment.showReviewBottomDialog() }
    }

    //TODO will be fixed during PR review
//    @Test
//    fun `updates payment details when payment button is tapped on the review fragment`() = runTest {
//        // Given
//        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldHandleErrorsInternally } returns false
//        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldShowReviewFragment } returns true
//        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.isAmountFieldEditable } returns false
//        every { paymentFlowViewModel!!.giniMerchant!!.createFragment("", "", "", "") } returns mockk(relaxed = true)
//        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
//        every { paymentFlowViewModel!!.giniMerchant.eventsFlow } returns MutableStateFlow(mockk(relaxed = true))
//        every { paymentFlowViewModel!!.paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
//            mockk(relaxed = true)
//        )
//
//        val fragment = PaymentFragment.newInstance(
//            giniMerchant = giniMerchant!!,
//            paymentDetails = mockk(relaxed = true),
//            paymentFlowConfiguration = PaymentFlowConfiguration(shouldShowReviewFragment = true),
//            viewModelFactory = viewModelFactory
//        )
//
//        val scenario = launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
//            fragment
//        }
//
//        val updatedPaymentDetails = PaymentDetails("recipient", "iban", "amount", "purpose")
//        fragment.reviewViewListener = object: ReviewViewListener {
//            override fun onPaymentButtonTapped(paymentDetails: PaymentDetails) {
//                paymentFlowViewModel!!.updatePaymentDetails(updatedPaymentDetails)
//            }
//        }
//
//        scenario.onFragment { paymentFragment ->
//            // When
//            paymentFragment.handlePayFlow()
//
//            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
//
//            val reviewFragment = paymentFragment.childFragmentManager.fragments.first {
//                it is ReviewBottomSheet
//            } as ReviewBottomSheet
//
//            reviewFragment.paymentButtonListener?.onPaymentButtonTapped(updatedPaymentDetails)
//        }
//
//        // Then
//        verify { paymentFlowViewModel!!.updatePaymentDetails(eq(updatedPaymentDetails)) }
//    }

    @Test
    fun `forwards payment request to viewModel when config doesn't allow showing ReviewFragment`() = runTest {
        // Given
        val paymentFlow = MutableSharedFlow<PaymentNextStep>(extraBufferCapacity = 1)
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentNextStep } returns paymentFlow.also { it.tryEmit(
            PaymentNextStep.RedirectToBank) }
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldHandleErrorsInternally } returns false
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldShowReviewFragment } returns false
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.isAmountFieldEditable } returns false
        val fragment = PaymentFragment.newInstance(
            giniMerchant = giniMerchant!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = PaymentFlowConfiguration(shouldShowReviewFragment = false),
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
            fragment
        }

        // When
        fragment.handlePayFlow()

        // Then
        verify { paymentFlowViewModel!!.onPaymentButtonTapped(any()) }
    }

}