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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MerchantFragmentTest {
    private var context: Context? = null
    private var giniMerchant : GiniMerchant? = null
    private var paymentComponent: PaymentComponent? = null
    private var merchantViewModel: MerchantViewModel? = null
    private lateinit var viewModelFactory: ViewModelProvider.Factory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniMerchantTheme)

        giniMerchant = mockk<GiniMerchant>(relaxed = true)
        every { giniMerchant!!.openBankState } returns MutableStateFlow(mockk(relaxed = true))
        every { giniMerchant!!.paymentFlow } returns MutableStateFlow(mockk(relaxed = true))

        paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { paymentComponent!!.getPaymentReviewFragment("1234", any()) } returns mockk(relaxed = true)

        merchantViewModel = mockk<MerchantViewModel>(relaxed = true)
        every { merchantViewModel!!.giniMerchant } returns giniMerchant
        every { merchantViewModel!!.paymentComponent } returns paymentComponent
        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return merchantViewModel as T
            }
        }
    }

    @Test
    fun `shows payment component bottom sheet on startup`() = runTest {
        // Given
        every { merchantViewModel!!.getLastBackstackEntry() } returns MerchantViewModel.DisplayedScreen.Nothing
        val fragment = MerchantFragment.newInstance(
            giniMerchant = giniMerchant!!,
            paymentComponent = paymentComponent!!,
            documentId = "1234",
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
        every { merchantViewModel!!.addToBackStack(MerchantViewModel.DisplayedScreen.ReviewFragment) } returns Unit
        every { merchantViewModel!!.merchantFlowConfiguration!!.shouldHandleErrorsInternally } returns false
        every { merchantViewModel!!.merchantFlowConfiguration!!.shouldShowReviewFragment } returns true
        every { merchantViewModel!!.merchantFlowConfiguration!!.isAmountFieldEditable } returns false
        every { merchantViewModel!!.paymentComponent!!.getContainerFragment(any(), any()) } returns mockk(relaxed = true)
        every { merchantViewModel!!.paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            mockk(relaxed = true)
        )
        val documentId = "1234"
        val fragment = MerchantFragment.newInstance(
            giniMerchant = giniMerchant!!,
            paymentComponent = paymentComponent!!,
            documentId = documentId,
            merchantFlowConfiguration = MerchantFlowConfiguration(shouldShowReviewFragment = true),
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
            fragment
        }

        // When
        fragment.handlePayFlow(documentId)

        // Then
        verify { fragment.showReviewFragment(documentId) }
    }

    @Test
    fun `forwards payment request to viewModel when config doesn't allow showing ReviewFragment`() = runTest {
        // Given
        every { merchantViewModel!!.getLastBackstackEntry() } returns MerchantViewModel.DisplayedScreen.Nothing
        val documentId = "1234"
        val fragment = MerchantFragment.newInstance(
            giniMerchant = giniMerchant!!,
            paymentComponent = paymentComponent!!,
            documentId = documentId,
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
            fragment
        }

        // When
        fragment.handlePayFlow(documentId)

        // Then
        verify { merchantViewModel!!.onPayment() }
    }
}