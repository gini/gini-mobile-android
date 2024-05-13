package net.gini.android.health.sdk.bankselectionbottomsheet

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.PaymentProviderAppColors
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BankSelectionBottomSheetTest {

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
            gpcSupportedPlatforms = listOf("android")
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
            gpcSupportedPlatforms = listOf("android")
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
            gpcSupportedPlatforms = listOf()
        )
    )

    @Before
    fun setup() {
        paymentComponent = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        paymentComponent = null
    }

    @Test
    fun `displays correct number of payment providers when returned from view model`() {
        every {paymentComponent!!.paymentProviderAppsFlow} returns MutableStateFlow(PaymentProviderAppsState.Success(
            listOf(
                paymentProvider1,
                paymentProvider2,
                paymentProvider3
            )
        ))
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.NothingSelected)

        // When
        launchFragmentInContainer {
            BankSelectionBottomSheet.newInstance(paymentComponent!!)
        }

        // Then
        onView(withId(R.id.ghs_payment_provider_apps_list)).check { view, _ -> Truth.assertThat((view as RecyclerView).adapter!!.itemCount).isEqualTo(3) }
    }
}