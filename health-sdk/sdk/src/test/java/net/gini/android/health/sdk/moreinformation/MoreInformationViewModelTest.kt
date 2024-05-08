package net.gini.android.health.sdk.moreinformation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MoreInformationViewModelTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var paymentComponent: PaymentComponent? = null

    @Before
    fun setup() {
        paymentComponent = mockk(relaxed = true)
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
    }

    @After
    fun tearDown() {
        paymentComponent = null
    }

    @Test
    fun `gets payment provider apps successfully`() = runTest {
        // Given
        val viewModel = MoreInformationViewModel(paymentComponent)

        // When
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(
            PaymentProviderAppsState.Success(
                listOf(
                    PaymentProviderApp(
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
                            gpcSupported = true
                        )
                    )
                )
            )
        )
        viewModel.start()

        // Then
        viewModel.paymentProviderAppsListFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(MoreInformationViewModel.PaymentProviderAppsListState.Success::class.java)

            val validationResult = validation as MoreInformationViewModel.PaymentProviderAppsListState.Success
            assertThat(validationResult.paymentProviderAppsList).isNotEmpty()
            assertThat(validationResult.paymentProviderAppsList).hasSize(1)
            assertThat(validationResult.paymentProviderAppsList.first().name).isEqualTo("payment provider")

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `processes error from payment component`() = runTest {
        // Given
        val viewModel = MoreInformationViewModel(paymentComponent)

        // When
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow(
            PaymentProviderAppsState.Error(Throwable())
        )
        viewModel.start()

        // Then
        viewModel.paymentProviderAppsListFlow.test {
            val validation = awaitItem()
            assertThat(validation).isInstanceOf(MoreInformationViewModel.PaymentProviderAppsListState.Error::class.java)

            cancelAndConsumeRemainingEvents()
        }
    }
}