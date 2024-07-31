package net.gini.android.health.sdk.moreinformation

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
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.health.sdk.util.GiniLocalization
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MoreInformationTest {
    private var paymentComponent: PaymentComponent? = null
    private var context: Context? = null
    private lateinit var paymentComponentWithLocale: PaymentComponent
    private lateinit var giniHealth: GiniHealth
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI)
        paymentComponent = mockk(relaxed = true)
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
        every { paymentComponent!!.giniHealth.localizedContext } returns context
        every { paymentComponent!!.giniHealthLanguage } returns null
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
        onView(withId(R.id.ghs_payment_providers_icons_list)).check { view, _ -> assertThat ((view as RecyclerView).adapter!!.itemCount).isEqualTo(3) }
    }

    @Test
    fun `displays all FAQ questions`() {
        val fragment = MoreInformationFragment.newInstance(paymentComponent)
        // When
        launchFragmentInContainer {
            fragment
        }

        // Then
        onView(withId(R.id.ghs_faq_list)).check { view, _ -> assertThat ((view as ExpandableListView).adapter!!.count).isEqualTo(fragment.faqList.size) }
    }

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        giniHealth.setSDKLanguage(GiniLocalization.ENGLISH, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniHealth)

        // When
        launchFragmentInContainer {
            MoreInformationFragment.newInstance(paymentComponentWithLocale!!)
        }

        // Then
        onView(withId(R.id.ghs_more_information_title)).check(ViewAssertions.matches(ViewMatchers.withText("Pay bills easily with the banking app.")))
        onView(withId(R.id.ghs_faq_title)).check(ViewAssertions.matches(ViewMatchers.withText("Frequently asked questions")))
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        giniHealth.setSDKLanguage(GiniLocalization.GERMAN, context!!)
        paymentComponentWithLocale = PaymentComponent(context!!, giniHealth)

        // When
        launchFragmentInContainer {
            MoreInformationFragment.newInstance(paymentComponentWithLocale)
        }

        // Then
        onView(withId(R.id.ghs_more_information_title)).check(ViewAssertions.matches(ViewMatchers.withText("Rechnungen ganz einfach mit der Banking-App bezahlen.")))
        onView(withId(R.id.ghs_faq_title)).check(ViewAssertions.matches(ViewMatchers.withText("Häufig gestellte Fragen")))
    }
}