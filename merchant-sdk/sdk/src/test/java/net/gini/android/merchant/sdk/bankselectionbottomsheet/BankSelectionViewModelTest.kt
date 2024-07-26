package net.gini.android.merchant.sdk.bankselectionbottomsheet

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.merchant.sdk.bankselection.BankSelectionViewModel
import net.gini.android.merchant.sdk.bankselection.PaymentProviderAppsListState
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.merchant.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.merchant.sdk.test.ViewModelTestCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class BankSelectionViewModelTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var paymentComponent: PaymentComponent? = null
    private var paymentProvider1 = PaymentProviderApp(
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

    private var paymentProvider2 = PaymentProviderApp(
        name = "payment provider 2",
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

    private var paymentProvider3 = PaymentProviderApp(
        name = "payment provider 3",
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

    @Before
    fun setup() {
        paymentComponent = mockk(relaxed = true)
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow<SelectedPaymentProviderAppState>(mockk()).asStateFlow()
    }

    @After
    fun tearDown() {
        paymentComponent = null
    }
    @Test
    fun `processes payment provider apps when emitted from payment component with none selected`() = runTest {
        // Given
        val viewModel = BankSelectionViewModel(paymentComponent, mockk())

        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(
            PaymentProviderAppsState.Success(
                listOf(
                    paymentProvider1,
                    paymentProvider2,
                    paymentProvider3
                )
            )
        )

        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.NothingSelected)

        viewModel.start()

        // When
        viewModel.paymentProviderAppsListFlow.test {

            // Then
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsListState.Success::class.java)

            val validationResult = validation as PaymentProviderAppsListState.Success
            assertThat(validationResult.paymentProviderAppsList).isNotEmpty()
            assertThat(validationResult.paymentProviderAppsList).hasSize(3)
            assertThat(validationResult.paymentProviderAppsList.filter { it.isSelected }.size).isEqualTo(0)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `processes payment provider apps when emitted from payment component with default selected`() = runTest {
        // Given
        val viewModel = BankSelectionViewModel(paymentComponent, mockk())

        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(
            PaymentProviderAppsState.Success(
                listOf(
                    paymentProvider1,
                    paymentProvider2,
                    paymentProvider3
                )
            )
        )

        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(paymentProviderApp = paymentProvider1))

        viewModel.start()

        // When
        viewModel.paymentProviderAppsListFlow.test {

            // Then
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(PaymentProviderAppsListState.Success::class.java)

            val validationResult = validation as PaymentProviderAppsListState.Success
            assertThat(validationResult.paymentProviderAppsList).isNotEmpty()
            assertThat(validationResult.paymentProviderAppsList).hasSize(3)

            val selectedPaymentProviderAppsList = validationResult.paymentProviderAppsList.filter { it.isSelected }
            assertThat(selectedPaymentProviderAppsList.size).isEqualTo(1)
            assertThat(selectedPaymentProviderAppsList.first().paymentProviderApp.name).isEqualTo("payment provider")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `sets selected payment provider app`() = runTest {
        // Given
        val viewModel = BankSelectionViewModel(paymentComponent, mockk())

        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(
            PaymentProviderAppsState.Success(
                listOf(
                    paymentProvider1,
                    paymentProvider2,
                    paymentProvider3
                )
            )
        )

        val paymentAppProviderFlow = MutableStateFlow<SelectedPaymentProviderAppState> (SelectedPaymentProviderAppState.NothingSelected)
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns paymentAppProviderFlow

        coEvery { paymentComponent!!.setSelectedPaymentProviderApp(paymentProvider1) } coAnswers  {
            paymentAppProviderFlow.emit(SelectedPaymentProviderAppState.AppSelected(paymentProvider1))
        }

        viewModel.start()

        viewModel.paymentProviderAppsListFlow.test {
            val nothingSelectedValidation = awaitItem()
            assertThat(nothingSelectedValidation).isInstanceOf(PaymentProviderAppsListState.Success::class.java)
            assertThat((nothingSelectedValidation as PaymentProviderAppsListState.Success).paymentProviderAppsList.filter { it.isSelected }.size).isEqualTo(0)

            // When
            viewModel.setSelectedPaymentProviderApp(paymentProvider1)

            // Then
            val paymentAppProviderSelectedValidation = awaitItem()
            assertThat(paymentAppProviderSelectedValidation).isInstanceOf(PaymentProviderAppsListState.Success::class.java)

            val selectedPaymentProvidersList = (paymentAppProviderSelectedValidation as PaymentProviderAppsListState.Success).paymentProviderAppsList.filter { it.isSelected }
            assertThat(selectedPaymentProvidersList.size).isEqualTo(1)
            assertThat(selectedPaymentProvidersList.first().paymentProviderApp.name).isEqualTo(paymentProvider1.name)
        }
    }

    // Text recheck

    @Test
    fun `rechecks which payment provider apps are installed`() = runTest {
        // Given
        val viewModel = BankSelectionViewModel(paymentComponent, mockk())

        val paymentProvider1: PaymentProviderApp = mockk()
        every { paymentProvider1.isInstalled() } returns false

        val paymentProviderAppsList = listOf(
            paymentProvider1,
            paymentProvider2,
            paymentProvider3
        )

        val paymentProvidersListFlow = MutableStateFlow(
            PaymentProviderAppsState.Success(
                paymentProviderAppsList
            )
        )
        every { paymentComponent!!.paymentProviderAppsFlow } returns paymentProvidersListFlow

        coEvery { paymentComponent!!.recheckWhichPaymentProviderAppsAreInstalled() } coAnswers {
            paymentProvidersListFlow.emit(PaymentProviderAppsState.Success(paymentProviderAppsList))
        }

        viewModel.start()

        viewModel.paymentProviderAppsListFlow.test {
            val validateNoPaymentProviderAppInstalled = awaitItem()
            assertThat(validateNoPaymentProviderAppInstalled).isInstanceOf(PaymentProviderAppsListState.Success::class.java)
            assertThat((validateNoPaymentProviderAppInstalled as PaymentProviderAppsListState.Success).paymentProviderAppsList).isNotEmpty()
            assertThat(validateNoPaymentProviderAppInstalled.paymentProviderAppsList.filter { it.paymentProviderApp.isInstalled() }).isEmpty()

            // When
            every { paymentProvider1.isInstalled() } returns true

            viewModel.recheckWhichPaymentProviderAppsAreInstalled()

            // Then
            val validateOnePaymentProviderAppInstalled = awaitItem()
            assertThat(validateOnePaymentProviderAppInstalled).isInstanceOf(PaymentProviderAppsListState.Success::class.java)
            assertThat((validateOnePaymentProviderAppInstalled as PaymentProviderAppsListState.Success).paymentProviderAppsList).isNotEmpty()
            assertThat(validateOnePaymentProviderAppInstalled.paymentProviderAppsList.filter { it.paymentProviderApp.isInstalled() }).isNotEmpty()
        }
    }
}