package net.gini.android.internal.payment.bankselectionbottomsheet

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.bankselection.BankSelectionBottomSheet
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.paymentProvider.PaymentProviderAppColors
import net.gini.android.internal.payment.utils.GiniLocalization
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BankSelectionBottomSheetTest {

    private var paymentComponent: PaymentComponent? = null
    private lateinit var paymentComponentWithLocale: PaymentComponent
    private var context: Context? = null
    private lateinit var giniPaymentModule: GiniInternalPaymentModule
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
        context = ApplicationProvider.getApplicationContext()
        every { paymentComponent!!.paymentModule.localizedContext } returns context
        every { paymentComponent!!.getGiniPaymentLanguage() } returns null
        every { paymentComponent!!.paymentModule.localizedContext } returns context
        every { paymentComponent!!.getGiniPaymentLanguage(context) } returns null
    }

    @After
    fun tearDown() {
        paymentComponent = null
    }

    @Test
    fun `displays correct number of payment providers when returned from view model`() {
        every {paymentComponent!!.paymentProviderAppsFlow} returns MutableStateFlow(
            PaymentProviderAppsState.Success(
            listOf(
                paymentProvider1,
                paymentProvider2,
                paymentProvider3
            )
        ))
        every { paymentComponent!!.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.NothingSelected)

        // When
        launchFragmentInContainer {
            BankSelectionBottomSheet.newInstance(paymentComponent!!)
        }

        // Then
        onView(withId(R.id.gps_payment_provider_apps_list)).check { view, _ -> Truth.assertThat((view as RecyclerView).adapter!!.itemCount).isEqualTo(3) }
    }

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        giniPaymentModule = GiniInternalPaymentModule(context!!)
        giniPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniPaymentModule)

        // When
        launchFragmentInContainer {
            BankSelectionBottomSheet.newInstance(paymentComponentWithLocale)
        }

        // Then
        onView(withId(R.id.gps_title_label)).check(ViewAssertions.matches(ViewMatchers.withText("Your Bank")))
        onView(withId(R.id.gps_subtitle_label)).check(
            ViewAssertions.matches(
                ViewMatchers.withText
            ("You can only pay the bill if you have an account with one of the banks listed below.")
        ))
        onView(withId(R.id.gps_more_information_label)).check(ViewAssertions.matches(ViewMatchers.withText("More information.")))
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        giniPaymentModule = GiniInternalPaymentModule(context!!)
        giniPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniPaymentModule)

        // When
        launchFragmentInContainer {
            BankSelectionBottomSheet.newInstance(paymentComponentWithLocale)
        }

        // Then
        onView(withId(R.id.gps_title_label)).check(ViewAssertions.matches(ViewMatchers.withText("Ihre Bank")))
        onView(withId(R.id.gps_subtitle_label)).check(
            ViewAssertions.matches(
                ViewMatchers.withText
            ("Sie können die Rechnung nur bezahlen, wenn Sie ein Konto bei einer der unten aufgeführten Banken haben.")
        ))
        onView(withId(R.id.gps_more_information_label)).check(ViewAssertions.matches(ViewMatchers.withText("Mehr Informationen.")))
    }
}