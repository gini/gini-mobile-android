package net.gini.android.internal.payment.moreInformation

import android.content.Context
import android.widget.ExpandableListView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.internal.GiniCoreAPIBuilder
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.PaymentProviderAppsState
import net.gini.android.internal.payment.paymentprovider.PaymentProviderApp
import net.gini.android.internal.payment.paymentprovider.PaymentProviderAppColors
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.moreinformation.MoreInformationFragment
import net.gini.android.internal.payment.util.GiniLocalization
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MoreInformationTest {
    private var paymentComponent: PaymentComponent? = null
    private lateinit var paymentComponentWithLocale: PaymentComponent
    private var context: Context? = null
    private lateinit var giniPaymentModule: GiniInternalPaymentModule

    @Before
    fun setup() {
        paymentComponent = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
        every { paymentComponent!!.paymentModule.localizedContext } returns context
        every { paymentComponent!!.giniPaymentLanguage } returns null
    }

    @Test
    fun `loads payment provider icons when returned from view model`() {
        // Given
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
                            gpcSupportedPlatforms = listOf("android"),
                            openWithSupportedPlatforms = listOf("android")
                        )
                    ),
                    PaymentProviderApp(
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
                    ),
                    PaymentProviderApp(
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
                )
            )
        )

        // When
        launchFragmentInContainer {
            MoreInformationFragment.newInstance(paymentComponent)
        }

        // Then
        onView(withId(R.id.gps_payment_providers_icons_list)).check { view, _ -> assertThat ((view as RecyclerView).adapter!!.itemCount).isEqualTo(3) }
    }

    @Test
    fun `displays all FAQ questions`() {
        val fragment = MoreInformationFragment.newInstance(paymentComponent)
        // When
        launchFragmentInContainer {
            fragment
        }

        // Then
        onView(withId(R.id.gps_faq_list)).check { view, _ -> assertThat ((view as ExpandableListView).adapter!!.count).isEqualTo(fragment.faqList.size) }
    }

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        giniPaymentModule = GiniInternalPaymentModule(context!!)
        giniPaymentModule.setSDKLanguage(GiniLocalization.ENGLISH, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniPaymentModule)

        // When
        launchFragmentInContainer {
            MoreInformationFragment.newInstance(paymentComponentWithLocale!!)
        }

        // Then
        onView(withId(R.id.gps_more_information_title)).check(ViewAssertions.matches(ViewMatchers.withText("Pay bills easily with the banking app.")))
        onView(withId(R.id.gps_faq_title)).check(ViewAssertions.matches(ViewMatchers.withText("Frequently asked questions")))
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        giniPaymentModule = GiniInternalPaymentModule(context!!)
        giniPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniPaymentModule)

        // When
        launchFragmentInContainer {
            MoreInformationFragment.newInstance(paymentComponentWithLocale)
        }

        // Then
        onView(withId(R.id.gps_more_information_title)).check(ViewAssertions.matches(ViewMatchers.withText("Rechnungen ganz einfach mit der Banking-App bezahlen.")))
        onView(withId(R.id.gps_faq_title)).check(ViewAssertions.matches(ViewMatchers.withText("Häufig gestellte Fragen")))
    }
}
