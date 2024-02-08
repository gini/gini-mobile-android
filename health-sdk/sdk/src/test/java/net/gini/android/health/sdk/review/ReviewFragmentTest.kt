package net.gini.android.health.sdk.review

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.review.bank.BankApp
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

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)

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
        every { viewModel.selectedBank } returns MutableStateFlow(BankApp(
            name = "",
            packageName = "",
            version = "",
            colors = mockk(relaxed = true),
            paymentProvider = mockk(relaxed = true),
            launchIntent = mockk(relaxed = true),
            icon = null
        ))

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment(giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory)
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        verify {
           listener.onNextClicked(any())
        }
    }

    @Test
    fun `passes selected payment provider name to onNextClicked() listener`() {
        // Given
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)

        val paymentProviderName = "Test Bank App"

        every { viewModel.selectedBank } returns MutableStateFlow(BankApp(
            name = paymentProviderName,
            packageName = "",
            version = "",
            colors = mockk(relaxed = true),
            paymentProvider = mockk(relaxed = true),
            launchIntent = mockk(relaxed = true),
            icon = null
        ))

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment(
                giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        verify {
            listener.onNextClicked(cmpEq(paymentProviderName))
        }
    }

}