package net.gini.android.health.sdk.review

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
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
    private var giniInternalPaymentModule: GiniInternalPaymentModule? = null

    @Before
    fun setup() {
        giniHealth = mockk(relaxed = true)
        every { giniHealth!!.paymentFlow } returns MutableStateFlow<ResultWrapper<PaymentDetails>>(mockk()).asStateFlow()
        giniInternalPaymentModule = mockk(relaxed = true)
        every { giniInternalPaymentModule!!.paymentFlow } returns MutableStateFlow(mockk())
        every { giniInternalPaymentModule!!.eventsFlow } returns MutableStateFlow(mockk())
        every { giniInternalPaymentModule!!.giniHealthAPI } returns mockk(relaxed = true)
        every { giniInternalPaymentModule!!.giniHealthAPI.documentManager } returns mockk(relaxed = true)
        every { giniHealth!!.giniInternalPaymentModule } returns giniInternalPaymentModule!!
        userPreferences = mockk(relaxed = true)
        context = getApplicationContext()
    }

    @After
    fun tearDown() {
        giniHealth = null
        userPreferences = null
        context = null
        giniInternalPaymentModule = null
    }

    @Test
    fun `shows info bar on launch`() = runTest {
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "",PaymentFlowConfiguration(showCloseButtonOnReviewFragment = true, popupDurationPaymentReview = 3),reviewFragmentListener = mockk()).apply {
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
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "", PaymentFlowConfiguration(showCloseButtonOnReviewFragment = true, popupDurationPaymentReview = 3), mockk()).apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        advanceTimeBy(viewModel.showInfoBarDurationMs + 100)

        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isFalse()
    }

    @Test
    fun `popup duration should not be negative`() = runTest {
        // Given
        val invalidDuration = -1
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "", PaymentFlowConfiguration(showCloseButtonOnReviewFragment = true, popupDurationPaymentReview = invalidDuration), mockk())

        // When
        val actualDuration = viewModel.showInfoBarDurationMs

        // Then
        assertThat(actualDuration).isAtLeast(0)
    }

    @Test
    fun `popup duration should not exceed 10 seconds`() = runTest {
        // Given
        val invalidDuration = 11
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "", PaymentFlowConfiguration(showCloseButtonOnReviewFragment = true, popupDurationPaymentReview = invalidDuration), mockk())

        // When
        val actualDuration = viewModel.showInfoBarDurationMs

        // Then
        assertThat(actualDuration).isAtMost(10000)
    }

    @Test
    fun `retries document review`() {
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))

        val documentId = "1234"

        val viewModel = ReviewViewModel(giniHealth!!,
            mockk(), paymentComponent, documentId, PaymentFlowConfiguration(showCloseButtonOnReviewFragment = true, popupDurationPaymentReview = 3), mockk())

        // When
        viewModel.retryDocumentReview()


        coVerify { giniHealth!!.retryDocumentReview() }
    }
}