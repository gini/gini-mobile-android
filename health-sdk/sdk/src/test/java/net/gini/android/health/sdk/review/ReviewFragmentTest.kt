package net.gini.android.health.sdk.review

import android.content.Context
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDialog

/**
 * Created by Alpár Szotyori on 14.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ReviewFragmentTest {

    private lateinit var viewModel: ReviewViewModel
    private lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var context: Context

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniHealthTheme)
        configureMockViewModel(viewModel)
    }

    private fun configureMockViewModel(viewModel: ReviewViewModel) {
        val giniHealth = mockk<GiniHealth>(relaxed = true)
        every { giniHealth.documentFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniHealth.paymentFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniHealth.openBankState } returns MutableStateFlow(mockk(relaxed = true))

        every { viewModel.giniHealth } returns giniHealth
        every { viewModel.paymentDetails } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.paymentValidation } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.isPaymentButtonEnabled } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.isInfoBarVisible } returns MutableStateFlow(mockk(relaxed = true))

        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
    }

    @Test
    fun `calls onNextClicked() listener when 'Next' ('Pay') button is clicked`() {
        // Given
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.onPaymentButtonTapped() } returns ReviewViewModel.PaymentNextStep.RedirectToBank

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns mockk(relaxed = true)

        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.isInstalled() } returns true
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        verify {
            listener.onToTheBankButtonClicked(any())
        }
    }

    @Test
    fun `passes selected payment provider name to onNextClicked() listener`() {
        // Given
        val paymentProviderName = "Test Bank App"
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName
        every { paymentProviderApp.isInstalled() } returns true


        val paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.onPaymentButtonTapped() } returns ReviewViewModel.PaymentNextStep.RedirectToBank

        val listener = mockk<ReviewFragmentListener>(relaxed = true)

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        verify {
            listener.onToTheBankButtonClicked(cmpEq(paymentProviderName))
        }
    }

    @Test
    fun `shows install app dialog if payment provider app is not installed`() = runTest {
        // Given
        val paymentProviderName = "Test Bank App"
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName
        every { paymentProviderApp.isInstalled() } returns false


        val paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)
        )

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.onPaymentButtonTapped() } returns ReviewViewModel.PaymentNextStep.ShowInstallApp

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        val fragment = ReviewFragment.newInstance(
            giniHealth = mockk(relaxed = true),
            listener = listener,
            viewModelFactory = viewModelFactory,
            paymentComponent = paymentComponent
        )

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            fragment
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        val dialog = ShadowDialog.getLatestDialog()
        Truth.assertThat(dialog.isShowing)
    }

    @Test
    fun `displays 'Open With' dialog when 'Pay' button is clicked and payment provider does not support 'GPC'`() = runTest {
        // Given
        val paymentProviderName = "Test Bank App"
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName
        every { paymentProviderApp.isInstalled() } returns false

        val paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)
        )

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.onPaymentButtonTapped() } returns ReviewViewModel.PaymentNextStep.ShowOpenWithSheet

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = mockk(relaxed = true),
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        val dialog = ShadowDialog.getLatestDialog()
        Truth.assertThat(dialog.isShowing)
    }
}