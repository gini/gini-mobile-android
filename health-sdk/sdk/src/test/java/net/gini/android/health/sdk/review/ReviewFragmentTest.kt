package net.gini.android.health.sdk.review

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
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.internal.payment.utils.PaymentNextStep
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
    private lateinit var paymentNextStepFlow: MutableSharedFlow<PaymentNextStep>

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniHealthTheme)
        paymentNextStepFlow = MutableStateFlow(mockk())
        configureMockViewModel(viewModel)
    }

    private fun configureMockViewModel(viewModel: ReviewViewModel) {
        val giniHealth = mockk<GiniHealth>(relaxed = true)
        every { giniHealth.documentFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniHealth.paymentFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniHealth.openBankState } returns MutableStateFlow(mockk(relaxed = true))

        every { viewModel.giniHealth } returns giniHealth
        every { viewModel.paymentDetails } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.isInfoBarVisible } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.paymentProviderApp } returns MutableStateFlow(mockk(relaxed = true))

        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
    }

    @Test
    fun `loads payment details for documentId`() {
        // Given
        val documentId = "1234"

        // When
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = mockk(relaxed = true),
                viewModelFactory = viewModelFactory,
                paymentComponent = mockk(relaxed = true),
                documentId = documentId,
                shouldShowCloseButton = true
            )
        }

        // Then
        verify {
            viewModel.loadPaymentDetails()
        }
    }
}
