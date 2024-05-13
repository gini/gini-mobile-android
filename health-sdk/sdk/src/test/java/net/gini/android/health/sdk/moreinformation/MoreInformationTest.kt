package net.gini.android.health.sdk.moreinformation

import android.widget.ExpandableListView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.PaymentProviderAppsState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.paymentprovider.PaymentProviderAppColors
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MoreInformationTest {
    private var paymentComponent: PaymentComponent? = null

    @Before
    fun setup() {
        paymentComponent = mockk(relaxed = true)
        every { paymentComponent!!.paymentProviderAppsFlow } returns MutableStateFlow<PaymentProviderAppsState>(mockk()).asStateFlow()
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
                            gpcSupportedPlatforms = listOf("android")
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
}