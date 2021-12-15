package net.gini.android.health.sdk.review

import android.content.pm.PackageManager
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import net.gini.android.core.api.DocumentManager
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.review.bank.packageInfosFixture
import net.gini.android.health.sdk.review.bank.paymentProvidersFixture
import net.gini.android.health.sdk.review.bank.resolveInfosFixture
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.test.TestCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
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

    private var viewModel: ReviewViewModel? = null
    private var viewModelFactory: ViewModelProvider.Factory? = null

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)

        viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel!! as T
            }
        }
    }

    @After
    fun tearDown() {
        viewModel = null
    }

    @Test
    fun `calls onPayClicked() listener when 'Pay' button is clicked`() {
        // Given
        every { viewModel!!.isPaymentButtonEnabled } returns flowOf(true)

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        launchFragmentInContainer(themeResId = R.style.Root_GiniHealth) {
            ReviewFragment(giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory!!)
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        // Then
        verify {
           listener.onPayClicked()
        }
    }

}