package net.gini.android.health.sdk.review

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.PaymentNextStep
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.After
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
    private lateinit var paymentNextStepFlow: MutableSharedFlow<PaymentNextStep>

    @Before
    fun setup() {
        viewModel = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniHealthTheme)
        paymentNextStepFlow = MutableStateFlow(mockk())
        configureMockViewModel(viewModel)
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown(){
        Dispatchers.resetMain()
    }

    private fun configureMockViewModel(viewModel: ReviewViewModel) {
        val giniHealth = mockk<GiniHealth>(relaxed = true)
        every { giniHealth.documentFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniHealth.paymentFlow } returns MutableStateFlow(mockk(relaxed = true))
        every { giniHealth.openBankState } returns MutableStateFlow(mockk(relaxed = true))

        every { viewModel.giniHealth } returns giniHealth
        every { viewModel.paymentDetails } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.isInfoBarVisible } returns MutableStateFlow(mockk(relaxed = true))
        every { viewModel.paymentNextStepFlow } returns paymentNextStepFlow

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
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns mockk(relaxed = true)

        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        val paymentProvider: PaymentProvider = mockk(relaxed = true)
        every { paymentProvider.gpcSupported() } returns true
        every { paymentProviderApp.paymentProvider } returns paymentProvider
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)

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
        onView(withId(net.gini.android.internal.payment.R.id.payment)).perform(click())

        paymentNextStepFlow.tryEmit(PaymentNextStep.RedirectToBank)

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
        every { paymentComponent.giniPaymentLanguage } returns null
        every { paymentComponent.paymentModule } returns mockk(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)
        )

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)

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
        onView(withId(net.gini.android.internal.payment.R.id.payment)).perform(click())

        paymentNextStepFlow.tryEmit(PaymentNextStep.RedirectToBank)

        // Then
        verify {
            listener.onToTheBankButtonClicked(cmpEq(paymentProviderName))
        }
    }

    @Test
    fun `shows 'install app' dialog if payment provider app is not installed`() {
        // Given
        val paymentProviderName = "Test Bank App"
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName
        every { paymentProviderApp.isInstalled() } returns false

        val paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
        every { paymentComponent.giniPaymentLanguage } returns null
        every { paymentComponent.paymentModule } returns mockk(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)
        )

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)

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
        onView(withId(net.gini.android.internal.payment.R.id.payment)).perform(click())

        paymentNextStepFlow.tryEmit(PaymentNextStep.ShowInstallApp)

        ShadowLooper.runUiThreadTasks()

        // Then
        val dialog = ShadowDialog.getLatestDialog()
        Truth.assertThat(dialog.isShowing).isTrue()
    }

    @Test
    fun `shows 'Open With' app dialog if payment provider app does not support gpc`() {
        // Given
        val paymentProviderName = "Test Bank App"
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName
        every { paymentProviderApp.isInstalled() } returns false

        val paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
        every { paymentComponent.giniPaymentLanguage } returns null
        every { paymentComponent.paymentModule } returns mockk(relaxed = true)
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp)
        )

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)

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
        onView(withId(net.gini.android.internal.payment.R.id.payment)).perform(click())

        paymentNextStepFlow.tryEmit(PaymentNextStep.ShowOpenWithSheet)

        ShadowLooper.runUiThreadTasks()

        // Then
        val dialog = ShadowDialog.getLatestDialog()
        Truth.assertThat(dialog.isShowing).isTrue()
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
        every { paymentComponent.giniPaymentLanguage } returns null

        viewModel = mockk(relaxed = true)
        configureMockViewModel(viewModel)
        every { viewModel.paymentComponent } returns paymentComponent
        every { viewModel.paymentProviderApp } returns MutableStateFlow(paymentProviderApp)
        every { viewModel.viewModelScope } returns CoroutineScope(Dispatchers.Unconfined)

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

        paymentNextStepFlow.tryEmit(PaymentNextStep.OpenSharePdf(mockk()))

        ShadowLooper.runUiThreadTasks()

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
