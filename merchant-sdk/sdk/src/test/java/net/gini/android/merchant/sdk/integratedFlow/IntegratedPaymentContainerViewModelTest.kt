package net.gini.android.merchant.sdk.integratedFlow

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.merchant.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.merchant.sdk.util.GiniPayment
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class IntegratedPaymentContainerViewModelTest {
    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
    private var giniMerchant: GiniMerchant? = null
    private var paymentComponent: PaymentComponent? = null
    private var giniPayment: GiniPayment? = null

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
        every { giniHealthAPI.documentManager } returns documentManager
        giniMerchant = GiniMerchant(giniHealthAPI)
        giniPayment = mockk()
        paymentComponent = mockk()
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.AppSelected(initialPaymentProviderApp)).asStateFlow()
    }

    @After
    fun tearDown() {
        giniMerchant = null
        paymentComponent = null
    }

    @Test
    fun `adds to backstack`() = runTest {
        // Given
        val viewModel = IntegratedPaymentContainerViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            integratedFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPayment = GiniPayment(giniMerchant)
        )

        // When
        viewModel.addToBackStack(IntegratedPaymentContainerViewModel.DisplayedScreen.BankSelectionBottomSheet)

        // Then
        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(IntegratedPaymentContainerViewModel.DisplayedScreen.BankSelectionBottomSheet::class.java)
    }

    @Test
    fun `pops backstack`() = runTest {
        // Given
        val viewModel = IntegratedPaymentContainerViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            integratedFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPayment = GiniPayment(giniMerchant)
        )

        // When
        viewModel.addToBackStack(IntegratedPaymentContainerViewModel.DisplayedScreen.BankSelectionBottomSheet)
        viewModel.addToBackStack(IntegratedPaymentContainerViewModel.DisplayedScreen.MoreInformationFragment)

        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(IntegratedPaymentContainerViewModel.DisplayedScreen.MoreInformationFragment::class.java)

        // Then
        viewModel.popBackStack()
        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(IntegratedPaymentContainerViewModel.DisplayedScreen.BankSelectionBottomSheet::class.java)
    }

    @Test
    fun `peek backstack returns last value in backstack`() = runTest {
        // Given
        val viewModel = IntegratedPaymentContainerViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            integratedFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPayment = GiniPayment(giniMerchant)
        )

        // When
        viewModel.addToBackStack(IntegratedPaymentContainerViewModel.DisplayedScreen.BankSelectionBottomSheet)
        viewModel.addToBackStack(IntegratedPaymentContainerViewModel.DisplayedScreen.MoreInformationFragment)

        // Then
        assertThat(viewModel.getLastBackstackEntry()).isInstanceOf(IntegratedPaymentContainerViewModel.DisplayedScreen.MoreInformationFragment::class.java)
    }

    @Test
    fun `returns true if payment provider app has been changed`() = runTest {
        // Given
        val viewModel = IntegratedPaymentContainerViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            integratedFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPayment = GiniPayment(giniMerchant)
        )

        // Then
        assertThat(viewModel.paymentProviderAppChanged(changedPaymentProviderApp)).isTrue()
    }

    @Test
    fun `forwards on payment action to giniPayment`() = runTest {
        // Given
        coEvery { giniPayment!!.onPayment(any(), any()) } coAnswers {  }
        val viewModel = IntegratedPaymentContainerViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            integratedFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPayment = giniPayment!!
        )

        // Then
        viewModel.onPayment()
        coVerify(exactly = 1) { giniPayment!!.onPayment(any(), any()) }
    }

    @Test
    fun `forwards load document to giniMerchant`() = runTest {
        // Given
        coEvery { giniPayment!!.onPayment(any(), any()) } coAnswers {  }
        val viewModel = IntegratedPaymentContainerViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            integratedFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPayment = giniPayment!!
        )

        // Then
        viewModel.loadPaymentDetails()
        coVerify { giniMerchant!!.setDocumentForReview("1234") }
    }
}