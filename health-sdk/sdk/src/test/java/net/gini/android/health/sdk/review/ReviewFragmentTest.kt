package net.gini.android.health.sdk.review

import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.review.ReviewConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class ReviewFragmentTest {

    private lateinit var giniHealth: GiniHealth
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var listener: ReviewFragmentListener
    private lateinit var giniInternalPaymentModule: GiniInternalPaymentModule

    @Before
    fun setup() {
        giniHealth = mockk(relaxed = true)
        giniInternalPaymentModule = mockk(relaxed = true)

        every { giniHealth.paymentFlow } returns MutableStateFlow<ResultWrapper<PaymentDetails>>(ResultWrapper.Loading())
        every { giniHealth.documentFlow } returns MutableStateFlow(ResultWrapper.Loading())
        every { giniHealth.giniInternalPaymentModule } returns giniInternalPaymentModule
        every { giniInternalPaymentModule.paymentFlow } returns MutableStateFlow(mockk())
        every { giniInternalPaymentModule.eventsFlow } returns MutableStateFlow(mockk())
        every { giniInternalPaymentModule.giniHealthAPI } returns mockk(relaxed = true)
        every { giniInternalPaymentModule.giniHealthAPI.documentManager } returns mockk(relaxed = true)

        paymentComponent = mockk(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.NothingSelected)
        every { paymentComponent.getGiniPaymentLanguage(any()) } returns null

        listener = mockk(relaxed = true)
    }

    @Test
    fun `fragment is created successfully`() {
        val scenario = launchReviewFragment()

        scenario.onFragment { fragment ->
            assertThat(fragment).isNotNull()
        }
    }

    @Test
    fun `close button is visible when showCloseButtonOnReviewFragment is true`() {
        val paymentFlowConfig = PaymentFlowConfiguration(
            showCloseButtonOnReviewFragment = true,
            popupDurationPaymentReview = 3
        )

        val scenario = launchReviewFragment(paymentFlowConfiguration = paymentFlowConfig)
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            val closeButton = fragment.view?.findViewById<android.view.View>(
                net.gini.android.health.sdk.R.id.close
            )
            assertThat(closeButton?.visibility).isEqualTo(android.view.View.VISIBLE)
        }
    }

    @Test
    fun `close button is hidden when showCloseButtonOnReviewFragment is false`() {
        val paymentFlowConfig = PaymentFlowConfiguration(
            showCloseButtonOnReviewFragment = false,
            popupDurationPaymentReview = 3
        )

        val scenario = launchReviewFragment(paymentFlowConfiguration = paymentFlowConfig)
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            val closeButton = fragment.view?.findViewById<android.view.View>(
                net.gini.android.health.sdk.R.id.close
            )
            assertThat(closeButton?.visibility).isEqualTo(android.view.View.GONE)
        }
    }

    @Test
    fun `fragment handles document pages successfully`() {
        every { giniHealth.documentFlow } returns MutableStateFlow(
            ResultWrapper.Success(mockk(relaxed = true))
        )

        val scenario = launchReviewFragment()
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            assertThat(fragment.view).isNotNull()
        }
    }

    private fun launchReviewFragment(
        configuration: ReviewConfiguration = ReviewConfiguration(),
        paymentFlowConfiguration: PaymentFlowConfiguration = PaymentFlowConfiguration(
            showCloseButtonOnReviewFragment = true,
            popupDurationPaymentReview = 3
        ),
        documentId: String = "test-document-id"
    ): FragmentScenario<ReviewFragment> {
        return launchFragmentInContainer(
            fragmentArgs = Bundle(),
            themeResId = net.gini.android.internal.payment.R.style.GiniPaymentTheme
        ) {
            ReviewFragment.newInstance(
                giniHealth = giniHealth,
                configuration = configuration,
                listener = listener,
                paymentComponent = paymentComponent,
                documentId = documentId,
                paymentFlowConfiguration = paymentFlowConfiguration
            )
        }
    }
}