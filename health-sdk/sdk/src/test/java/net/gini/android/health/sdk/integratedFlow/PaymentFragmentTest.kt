package net.gini.android.health.sdk.integratedFlow

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
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.util.DisplayedScreen
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.utils.PaymentNextStep
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PaymentFragmentTest {
    private var context: Context? = null
    private var giniHealth : GiniHealth? = null
    private var paymentComponent: PaymentComponent? = null
    private var paymentFlowViewModel: PaymentFlowViewModel? = null
    private var giniInternalPaymentModule: GiniInternalPaymentModule? = null
    private lateinit var viewModelFactory: ViewModelProvider.Factory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        giniInternalPaymentModule = mockk<GiniInternalPaymentModule>(relaxed = true)

        giniHealth = mockk<GiniHealth>(relaxed = true)
        every { giniHealth!!.paymentFlow } returns MutableStateFlow(ResultWrapper.Success(mockk(relaxed = true)))
        every { giniHealth!!.documentFlow } returns MutableStateFlow(ResultWrapper.Success(mockk(relaxed = true)))

        paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniInternalPaymentModule!!.paymentComponent } returns paymentComponent!!
        every { giniInternalPaymentModule!!.paymentFlow } returns MutableStateFlow(net.gini.android.internal.payment.api.model.ResultWrapper.Success(
            mockk(relaxed = true)
        ))
        every { giniHealth!!.giniInternalPaymentModule } returns giniInternalPaymentModule!!

        paymentFlowViewModel = mockk<PaymentFlowViewModel>(relaxed = true)
        every { paymentFlowViewModel!!.giniHealth } returns giniHealth!!
        every { paymentFlowViewModel!!.paymentComponent } returns paymentComponent!!
        every { paymentFlowViewModel!!.backButtonEvent } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.shareWithFlowStarted } returns MutableStateFlow(false)
        every { paymentFlowViewModel!!.giniInternalPaymentModule } returns giniInternalPaymentModule!!
        every { paymentFlowViewModel!!.giniInternalPaymentModule.paymentComponent } returns paymentComponent!!
        every { paymentFlowViewModel!!.giniInternalPaymentModule.eventsFlow } returns MutableStateFlow(GiniInternalPaymentModule.InternalPaymentEvents.NoAction)
        every { paymentFlowViewModel!!.giniInternalPaymentModule.paymentFlow } returns MutableStateFlow(net.gini.android.internal.payment.api.model.ResultWrapper.Success(
            mockk(relaxed = true)
        ))
        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return paymentFlowViewModel as T
            }
        }
    }

    @Test
    fun `shows payment component bottom sheet on startup in case of new user`() = runTest {
        // Given
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns DisplayedScreen.Nothing
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.paymentFlowConfiguration } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentFlowConfiguration?.shouldShowReviewBottomDialog } returns false
        every { paymentFlowViewModel!!.giniInternalPaymentModule!!.getReturningUser() } returns false
        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = mockk(relaxed = true),
            viewModelFactory = viewModelFactory
        )

        // When
        launchFragmentInContainer() {
            fragment
        }

        // Then
        verify { fragment.showPaymentComponentBottomSheet() }
    }

    @Test
    fun `shows review fragment on startup in case returning user and document id`() = runTest {
        // Given
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns DisplayedScreen.Nothing
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.paymentFlowConfiguration } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentFlowConfiguration?.shouldShowReviewBottomDialog } returns false
        every { paymentFlowViewModel!!.giniInternalPaymentModule.getReturningUser() } returns true
        every { paymentFlowViewModel!!.documentId } returns "1234"
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()

        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = mockk(relaxed = true),
            viewModelFactory = viewModelFactory
        )

        // When
        launchFragmentInContainer {
            fragment
        }

        // Then
        verify { fragment.showReviewFragment() }
    }

    @Test
    fun `shows review bottom sheet on startup in case of returning user review configured as enabled`() = runTest {
        // Given
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns DisplayedScreen.Nothing
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow(extraBufferCapacity = 1)
        every { paymentFlowViewModel!!.paymentFlowConfiguration?.shouldShowReviewBottomDialog } returns true
        every { paymentFlowViewModel!!.giniInternalPaymentModule.getReturningUser() } returns true
        every { paymentFlowViewModel!!.documentId } returns null

        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = mockk(relaxed = true),
            viewModelFactory = viewModelFactory
        )

        // When
        launchFragmentInContainer {
            fragment
        }

        // Then
        verify { fragment.showReviewBottomDialog() }
    }

    @Test
    fun `shows payment component bottom sheet on startup in case of returning user and review screen disabled`() = runTest {
        // Given
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns DisplayedScreen.Nothing
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.paymentFlowConfiguration } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentFlowConfiguration?.shouldShowReviewBottomDialog } returns false
        every { paymentFlowViewModel!!.giniInternalPaymentModule.getReturningUser() } returns true
        every { paymentFlowViewModel!!.paymentNextStep } returns MutableSharedFlow()
        every { paymentFlowViewModel!!.documentId } returns null
        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = mockk(relaxed = true),
            viewModelFactory = viewModelFactory
        )

        // When
        launchFragmentInContainer {
            fragment
        }

        // Then
        verify { fragment.showPaymentComponentBottomSheet() }
    }

    @Test
    fun `forwards payment request to viewModel when config doesn't allow showing ReviewBottomSHeet and documentId is null`() = runTest {
        // Given
        val paymentFlow = MutableSharedFlow<PaymentNextStep>(extraBufferCapacity = 1)
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentNextStep } returns paymentFlow
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldHandleErrorsInternally } returns false
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldShowReviewBottomDialog } returns false
        every { paymentFlowViewModel!!.documentId } returns null
        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = PaymentFlowConfiguration(shouldShowReviewBottomDialog = false),
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer {
            fragment
        }

        // When
        fragment.handlePayFlow()

        // Then
        verify { paymentFlowViewModel!!.onPaymentButtonTapped() }
    }

    @Test
    fun `shows ReviewFragment when PayInvoice btn clicked on PaymentComponent and documentId is not null`() = runTest {
        // Given
        val paymentFlow = MutableSharedFlow<PaymentNextStep>(extraBufferCapacity = 1)
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentNextStep } returns paymentFlow
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldHandleErrorsInternally } returns false
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldShowReviewBottomDialog } returns false
        every { paymentFlowViewModel!!.documentId } returns "123"
        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = PaymentFlowConfiguration(shouldShowReviewBottomDialog = false),
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer {
            fragment
        }

        // When
        fragment.handlePayFlow()

        // Then
        verify { fragment.showReviewFragment() }
    }

    @Test
    fun `shows ReviewBottomSheet when PayInvoice btn clicked on PaymentComponent and documentId is null and review is enabled`() = runTest {
        // Given
        val paymentFlow = MutableSharedFlow<PaymentNextStep>(extraBufferCapacity = 1)
        every { paymentFlowViewModel!!.getLastBackstackEntry() } returns mockk(relaxed = true)
        every { paymentFlowViewModel!!.paymentNextStep } returns paymentFlow
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldHandleErrorsInternally } returns false
        every { paymentFlowViewModel!!.paymentFlowConfiguration!!.shouldShowReviewBottomDialog } returns true
        every { paymentFlowViewModel!!.documentId } returns null
        val fragment = PaymentFragment.newInstance(
            giniHealth = giniHealth!!,
            paymentDetails = mockk(relaxed = true),
            paymentFlowConfiguration = PaymentFlowConfiguration(shouldShowReviewBottomDialog = false),
            viewModelFactory = viewModelFactory
        )

        launchFragmentInContainer {
            fragment
        }

        // When
        fragment.handlePayFlow()

        // Then
        verify { fragment.showReviewBottomDialog() }
    }

}