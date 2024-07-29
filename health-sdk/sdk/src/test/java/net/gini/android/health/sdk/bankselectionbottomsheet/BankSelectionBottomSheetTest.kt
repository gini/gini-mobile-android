package net.gini.android.health.sdk.bankselectionbottomsheet

import android.content.Context
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.button.MaterialButton
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentComponentView
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.health.sdk.util.GiniLocalization
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class BankSelectionBottomSheetTest {
    private var context: Context? = null
    private lateinit var paymentComponentWithLocale: PaymentComponent
    private lateinit var giniHealth: GiniHealth
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
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
        context = ApplicationProvider.getApplicationContext()
        every { giniHealthAPI.documentManager } returns documentManager
        paymentComponent = mockk(relaxed = true)
        giniHealth = GiniHealth(giniHealthAPI)
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

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        giniHealth.setSDKLanguage(GiniLocalization.ENGLISH, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniHealth)

        // When
        launchFragmentInContainer {
            BankSelectionBottomSheet.newInstance(paymentComponentWithLocale!!)
        }

        // Then
        onView(withId(R.id.ghs_title_label)).check(ViewAssertions.matches(ViewMatchers.withText("Your Bank")))
        onView(withId(R.id.ghs_subtitle_label)).check(ViewAssertions.matches(ViewMatchers.withText
            ("You can only pay the bill if you have an account with one of the banks listed below.")
        ))
        onView(withId(R.id.ghs_more_information_label)).check(ViewAssertions.matches(ViewMatchers.withText("More information.")))
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        giniHealth.setSDKLanguage(GiniLocalization.GERMAN, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniHealth)

        // When
        launchFragmentInContainer {
            BankSelectionBottomSheet.newInstance(paymentComponentWithLocale)
        }

        // Then
        onView(withId(R.id.ghs_title_label)).check(ViewAssertions.matches(ViewMatchers.withText("Ihre Bank")))
        onView(withId(R.id.ghs_subtitle_label)).check(ViewAssertions.matches(ViewMatchers.withText
            ("Sie können die Rechnung nur bezahlen, wenn Sie ein Konto bei einer der unten aufgeführten Banken haben.")
        ))
        onView(withId(R.id.ghs_more_information_label)).check(ViewAssertions.matches(ViewMatchers.withText("Mehr Informationen.")))
    }
}