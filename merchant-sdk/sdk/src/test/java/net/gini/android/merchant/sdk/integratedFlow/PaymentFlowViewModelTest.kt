package net.gini.android.merchant.sdk.integratedFlow

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.R
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.merchant.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.merchant.sdk.util.DisplayedScreen
import net.gini.android.merchant.sdk.util.GiniPaymentManager
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

    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
    private var giniMerchant: GiniMerchant? = null
    private var paymentComponent: PaymentComponent? = null
    private var giniPayment: GiniPaymentManager? = null
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
        every { giniHealthAPI.documentManager } returns documentManager
        giniMerchant = mockk(relaxed = true)
        giniPayment = mockk(relaxed = true)
        paymentComponent = mockk()
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(mockk())
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(SelectedPaymentProviderAppState.AppSelected(initialPaymentProviderApp))
        every { giniMerchant!!.giniHealthAPI } returns giniHealthAPI
        every { giniMerchant!!.paymentFlow } returns MutableStateFlow(mockk())
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
            paymentComponent = paymentComponent,
            documentId = "1234",
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPaymentManager = GiniPaymentManager(giniMerchant)
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
            paymentComponent = paymentComponent,
            documentId = "1234",
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPaymentManager = GiniPaymentManager(giniMerchant)
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
            paymentComponent = paymentComponent,
            documentId = "1234",
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPaymentManager = GiniPaymentManager(giniMerchant)
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
            paymentComponent = paymentComponent,
            documentId = "1234",
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPaymentManager = GiniPaymentManager(giniMerchant)
        )

        // Then
        assertThat(viewModel.paymentProviderAppChanged(changedPaymentProviderApp)).isTrue()
    }

    @Test
    fun `forwards on payment action to giniPayment`() = runTest {
        // Given
        coEvery { giniPayment!!.onPayment(any(), any()) } coAnswers {  }
        val viewModel = PaymentFlowViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPaymentManager = giniPayment!!
        )

        // Then
        viewModel.onPayment()
        coVerify(exactly = 1) { giniPayment!!.onPayment(any(), any()) }
    }

    @Test
    fun `forwards load document to giniMerchant`() = runTest {
        // Given
//        coEvery { giniPayment!!.onPayment(any(), any()) } coAnswers {  }
        val viewModel = PaymentFlowViewModel(
            paymentComponent = paymentComponent,
            documentId = "1234",
            paymentFlowConfiguration = null,
            giniMerchant = giniMerchant,
            giniPaymentManager = giniPayment!!
        )

        // Then
        viewModel.loadPaymentDetails()
        coVerify { giniMerchant!!.setDocumentForReview("1234") }
    }
}