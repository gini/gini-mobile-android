package net.gini.android.health.sdk.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Created by Alpár Szotyori on 14.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ReviewFragmentTest.ShadowFileProvider::class, ShadowDialog::class])
class ReviewFragmentTest {

    private lateinit var viewModel: ReviewViewModel
    private lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var context: Context
    private lateinit var paymentNextStepSharedFlow: MutableSharedFlow<ReviewViewModel.PaymentNextStep>

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniHealthTheme)
        paymentNextStepSharedFlow = MutableSharedFlow<ReviewViewModel.PaymentNextStep>(extraBufferCapacity = 1)
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
                documentId = documentId
            )
        }

        // Then
        verify {
            viewModel.loadPaymentDetails()
        }
    }

    @Test
    fun `calls onNextClicked() listener when 'Next' ('Pay') button is clicked`() {
        // Given
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)

        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns mockk(relaxed = true)

        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        val paymentProvider: PaymentProvider = mockk(relaxed = true)
        every { paymentProvider.gpcSupported() } returns true
        every { paymentProviderApp.paymentProvider } returns paymentProvider
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.paymentNextStep } returns paymentNextStepSharedFlow

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent,
                documentId = ""
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        paymentNextStepSharedFlow.tryEmit(ReviewViewModel.PaymentNextStep.RedirectToBank)

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
        every { paymentComponent.giniHealth.localizedContext } returns ApplicationProvider.getApplicationContext()
        every { paymentComponent.giniHealthLanguage } returns null

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.paymentNextStep } returns paymentNextStepSharedFlow

        val listener = mockk<ReviewFragmentListener>(relaxed = true)

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent,
                documentId = ""
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        paymentNextStepSharedFlow.tryEmit(ReviewViewModel.PaymentNextStep.RedirectToBank)

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
        every { paymentComponent.giniHealth.localizedContext } returns ApplicationProvider.getApplicationContext()
        every { paymentComponent.giniHealthLanguage } returns null

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.paymentNextStep } returns paymentNextStepSharedFlow
        every { viewModel.validatePaymentDetails() } returns true

        val listener = mockk<ReviewFragmentListener>(relaxed = true)

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = listener,
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent,
                documentId = ""
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())
        paymentNextStepSharedFlow.tryEmit(ReviewViewModel.PaymentNextStep.ShowInstallApp)

        ShadowLooper.runUiThreadTasks()

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
        every { paymentComponent.giniHealth.localizedContext } returns ApplicationProvider.getApplicationContext()
        every { paymentComponent.giniHealthLanguage } returns null

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.paymentNextStep } returns paymentNextStepSharedFlow
        every { viewModel.validatePaymentDetails() } returns true

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            ReviewFragment.newInstance(
                giniHealth = mockk(relaxed = true),
                listener = mockk(relaxed = true),
                viewModelFactory = viewModelFactory,
                paymentComponent = paymentComponent,
                documentId = ""
            )
        }

        // When
        onView(withId(R.id.payment)).perform(click())

        paymentNextStepSharedFlow.tryEmit(ReviewViewModel.PaymentNextStep.ShowOpenWithSheet)

        ShadowLooper.runUiThreadTasks()

        // Then
        val dialog = ShadowDialog.getLatestDialog()
        Truth.assertThat(dialog.isShowing)
    }

    @Test
    fun `opens 'Share With' chooser after downloading PDF file`() = runTest {
        Intents.init();
        // Given
        val paymentProviderName = "Test Bank App"
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName
        every { paymentProviderApp.isInstalled() } returns true

        val paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
        every { paymentComponent.giniHealth.localizedContext } returns ApplicationProvider.getApplicationContext()
        every { paymentComponent.giniHealthLanguage } returns null

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.isPaymentButtonEnabled } returns flowOf(true)
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.paymentNextStep } returns paymentNextStepSharedFlow

        val listener = mockk<ReviewFragmentListener>(relaxed = true)
        val fragment = ReviewFragment.newInstance(
            giniHealth = mockk(relaxed = true),
            listener = listener,
            viewModelFactory = viewModelFactory,
            paymentComponent = paymentComponent,
            documentId = ""
        )
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            fragment
        }

        paymentNextStepSharedFlow.tryEmit(ReviewViewModel.PaymentNextStep.OpenSharePdf(mockk()))

        val expectedIntent = Matchers.allOf(
            hasAction(Intent.ACTION_SEND),
            hasExtra(Intent.EXTRA_STREAM, Uri.EMPTY),
            hasType("application/pdf")
        )
        intended(chooser(expectedIntent))
        Intents.release()
    }

    private fun chooser(matcher: Matcher<Intent>): Matcher<Intent> {
        return allOf(
            hasAction(Intent.ACTION_CHOOSER),
            hasExtra(`is`(Intent.EXTRA_INTENT), matcher))
    }

    @Suppress("UtilityClassWithPublicConstructor")
    @Implements(FileProvider::class)
    internal class ShadowFileProvider {

        companion object {
            @Suppress("UnusedParameter")
            @JvmStatic
            @Implementation
            fun getUriForFile(context: Context, authority: String, file: File): Uri {
                return Uri.EMPTY
            }
        }
    }
}
