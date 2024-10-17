package net.gini.android.health.sdk.review

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.internal.payment.review.reviewFragment.model.PaymentDetails
import net.gini.android.internal.payment.review.reviewFragment.model.ResultWrapper
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.reviewFragment.ReviewViewModel
import net.gini.android.internal.payment.utils.PaymentNextStep
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
    }

    @Test
    fun `shows info bar on launch`() = runTest {
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "").apply {
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
        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "").apply {
            userPreferences = this@ReviewViewModelTest.userPreferences!!
        }

        // When
        advanceTimeBy(ReviewViewModel.SHOW_INFO_BAR_MS + 100)

        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isFalse()
    }


    @Test
    fun `loads payment details for documentId`() {
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(mockk()))

        val documentId = "1234"

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, documentId)

        // When
        viewModel.loadPaymentDetails()


        coVerify { giniHealth!!.setDocumentForReview(documentId) }
    }

    @Test
    fun `increments 'Open With' counter`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.id } returns "123"

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "")

        // When
        viewModel.incrementOpenWithCounter(viewModel.viewModelScope, paymentProviderApp.paymentProvider.id)

        // Then
        coVerify { giniInternalPaymentModule!!.incrementCountForPaymentProviderId("123") }
    }

    @Test
    fun `returns 'RedirectToBank' when payment provider app supports GPC and is installed`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.gpcSupported() } returns true
        every { paymentProviderApp.isInstalled() } returns true

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "")

        viewModel.paymentNextStepFlow.test {
            // When
            viewModel.onPaymentButtonTapped(context!!.externalCacheDir)
            val nextStep = awaitItem()

            // Then
            assertThat(nextStep).isEqualTo(PaymentNextStep.RedirectToBank)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `returns 'ShowOpenWith' when payment provider app does not support GPC`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.gpcSupported() } returns false

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "")

        viewModel.paymentNextStepFlow.test {
            // When
            viewModel.onPaymentButtonTapped(context!!.externalCacheDir)
            val nextStep = awaitItem()

            // Then
            assertThat(nextStep).isEqualTo(PaymentNextStep.ShowOpenWithSheet)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `returns 'DownloadPaymentRequestFile' when payment provider app does not support GPC and 'Open With' was shown 3 times`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.gpcSupported() } returns false
        every { paymentProviderApp.paymentProvider.id } returns "123"

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        coEvery { giniInternalPaymentModule!!.getLiveCountForPaymentProviderId(any()) } returns flowOf(3)

        val viewModel = ReviewViewModel(giniHealth!!, mockk(), paymentComponent, "")
        viewModel.startObservingOpenWithCount()

        viewModel.paymentNextStepFlow.test {
            // When
            viewModel.onPaymentButtonTapped(context!!.externalCacheDir)
            val nextStep = awaitItem()

            // Then
            assertThat(nextStep).isEqualTo(PaymentNextStep.SetLoadingVisibility(true))
            cancelAndConsumeRemainingEvents()
        }
    }
}