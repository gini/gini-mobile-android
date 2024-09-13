package net.gini.android.merchant.sdk.integratedFlow

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.paymentprovider.PaymentProviderAppColors
import net.gini.android.internal.payment.util.PaymentNextStep
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.merchant.sdk.util.DisplayedScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PaymentFlowViewModelTest {
    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var giniMerchant: GiniMerchant? = null
    private var paymentComponent: PaymentComponent? = null
    private var giniInternalPaymentModule: GiniInternalPaymentModule? = null
    private var context: Context? = null

    private var initialPaymentProviderApp =  PaymentProviderApp(
        name = "payment provider",
        icon = null,
        colors = PaymentProviderAppColors(-1, -1),
        paymentProvider = PaymentProvider(
            id = "payment provider id",
            name = "payment provider name",
            packageName = "com.paymentProvider.packageName",
            appVersion = "appVersion",
            colors = PaymentProvider.Colors(backgroundColorRGBHex = "", textColoRGBHex = ""),
            icon = ByteArray(0),
            gpcSupportedPlatforms = listOf("android"),
            openWithSupportedPlatforms = listOf("android")
        )
    )

    private var changedPaymentProviderApp =  PaymentProviderApp(
        name = "payment provider2",
        icon = null,
        colors = PaymentProviderAppColors(-1, -1),
        paymentProvider = PaymentProvider(
            id = "payment provider2 id",
            name = "payment provider name",
            packageName = "com.paymentProvider.packageName",
            appVersion = "appVersion",
            colors = PaymentProvider.Colors(backgroundColorRGBHex = "", textColoRGBHex = ""),
            icon = ByteArray(0),
            gpcSupportedPlatforms = listOf("android"),
            openWithSupportedPlatforms = listOf("android")
        )
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniMerchantTheme)
        giniMerchant = mockk(relaxed = true)
        paymentComponent = mockk(relaxed = true)
        giniInternalPaymentModule = mockk(relaxed = true)

        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(mockk())
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.AppSelected(initialPaymentProviderApp))
        every { giniInternalPaymentModule!!.paymentComponent } returns paymentComponent!!
        every { giniMerchant!!.giniInternalPaymentModule } returns giniInternalPaymentModule!!
    }

    @After
    fun tearDown() {
        giniMerchant = null
        paymentComponent = null
    }

    @Test
    fun `adds to backstack`() = runTest {
        // Given
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        // When
        viewModel.addToBackStack(DisplayedScreen.BankSelectionBottomSheet)

        // Then
        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(DisplayedScreen.BankSelectionBottomSheet::class.java)
    }

    @Test
    fun `pops backstack`() = runTest {
        // Given
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        // When
        viewModel.addToBackStack(DisplayedScreen.BankSelectionBottomSheet)
        viewModel.addToBackStack(DisplayedScreen.MoreInformationFragment)

        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(DisplayedScreen.MoreInformationFragment::class.java)

        // Then
        viewModel.popBackStack()
        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(DisplayedScreen.BankSelectionBottomSheet::class.java)
    }

    @Test
    fun `peek backstack returns last value in backstack`() = runTest {
        // Given
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        // When
        viewModel.addToBackStack(DisplayedScreen.BankSelectionBottomSheet)
        viewModel.addToBackStack(DisplayedScreen.MoreInformationFragment)

        // Then
        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(DisplayedScreen.MoreInformationFragment::class.java)
    }

    @Test
    fun `returns true if payment provider app has been changed`() = runTest {
        // Given
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        // Then
        assertThat(viewModel.paymentProviderAppChanged(changedPaymentProviderApp)).isTrue()
    }

    @Test
    fun `forwards on payment action to giniPayment`() = runTest {
        // Given
        coEvery { giniInternalPaymentModule!!.onPayment(any(), any()) } coAnswers {  }
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        // Then
        viewModel.onPayment()
        coVerify(exactly = 1) { giniInternalPaymentModule!!.onPayment(any(), any()) }
    }

    @Test
    fun `checks bank app installed state`() = runTest {
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        viewModel.checkBankAppInstallState(changedPaymentProviderApp)
        assertThat(viewModel.getPaymentProviderApp()?.paymentProvider?.id).isEqualTo(changedPaymentProviderApp.paymentProvider.id)
    }

    @Test
    fun `emits share with started event`() = runTest {
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        viewModel.emitShareWithStartedEvent()
        viewModel.shareWithFlowStarted.test {
            val validation = awaitItem()
            assertThat(validation).isTrue()
        }
    }

    @Test
    fun `returns payment request from giniPaymentManager`() = runTest {
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        coEvery { giniMerchant!!.giniInternalPaymentModule.getPaymentRequest(any(), any()) } coAnswers { PaymentRequest("1234", null, null, "", "", null, "20", "", PaymentRequest.Status.INVALID) }

        val paymentRequest = viewModel.getPaymentRequest()
        assertThat(paymentRequest.id).isEqualTo("1234")
    }

    @Test
    fun `returns document as byteArray from giniMerchant`() = runTest {
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )
        val byteArray = byteArrayOf()

        coEvery { giniMerchant!!.giniInternalPaymentModule.giniHealthAPI.documentManager.getPaymentRequestDocument("1234") } coAnswers { Resource.Success(byteArray)}

        val document = viewModel.getPaymentRequestDocument(PaymentRequest("1234", "", "", "", "", "", "", "", PaymentRequest.Status.OPEN))
        assertThat(document.data).isEqualTo(byteArray)
    }

    @Test
    fun `increments 'Open With' counter`() = runTest {
        // Given
        val paymentProviderApp = mockk<PaymentProviderApp>()
        every { paymentProviderApp.paymentProvider.id } returns "123"

        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

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

        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )
        viewModel.checkBankAppInstallState(paymentProviderApp)
        viewModel.paymentNextStep.test {
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

        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        viewModel.paymentNextStep.test {
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

        coEvery { giniInternalPaymentModule!!.getLiveCountForPaymentProviderId(any()) } returns flowOf(3)

        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )
        viewModel.startObservingOpenWithCount(viewModel.viewModelScope, paymentProviderApp.paymentProvider.id)

        viewModel.paymentNextStep.test {
            // When
            viewModel.onPaymentButtonTapped(context!!.externalCacheDir)
            val nextStep = awaitItem()

            // Then
            assertThat(nextStep).isEqualTo(PaymentNextStep.SetLoadingVisibility(true))
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updates payment details`() {
        // Given
        val viewModel = PaymentFlowViewModel(
            paymentDetails = PaymentDetails("", "", "", ""),
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant!!,
        )

        val updatedPaymentDetails = PaymentDetails("recipient", "iban", "amount", "purpose")

        // When
        viewModel.updatePaymentDetails(updatedPaymentDetails)

        // Then
        assertThat(viewModel.paymentDetails).isEqualTo(updatedPaymentDetails)
    }
}