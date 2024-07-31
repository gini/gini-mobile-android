package net.gini.android.health.sdk.review.openWith

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.sdk.GiniHealth
import org.junit.Test
import org.junit.runner.RunWith
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.bankselection.BankSelectionBottomSheet
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.util.GiniLocalization
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class OpenWithBottomSheetTest {

    private lateinit var paymentComponent: PaymentComponent
    private var paymentComponentWithLocale: PaymentComponent? = null
    private lateinit var giniHealth: GiniHealth
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }

    @Test
    fun `listener method called when 'Forward' button tapped`() = runTest {
        // Given
        paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.giniHealth.localizedContext } returns ApplicationProvider.getApplicationContext()
        val listener: OpenWithForwardListener = mockk()
        every { listener.onForwardSelected() } returns mockk()

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                paymentComponent,
                listener
            )
        }

        // When
        onView(withId(R.id.ghs_forward_button)).perform(ViewActions.click())

        // Then
        verify { listener.onForwardSelected() }
    }

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI)
        giniHealth.setSDKLanguage(GiniLocalization.ENGLISH, context)
        paymentComponentWithLocale = PaymentComponent(context, giniHealth)

        // When
        launchFragmentInContainer {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                paymentComponentWithLocale,
                mockk()
            )
        }

        // Then
        onView(withId(R.id.ghs_open_with_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Invoice")))
        onView(withId(R.id.ghs_open_with_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("In the")))
        onView(withId(R.id.ghs_open_with_info)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Tip")))
        onView(withId(R.id.ghs_forward_button)).check(ViewAssertions.matches(ViewMatchers.withText("Forward")))
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI)
        giniHealth.setSDKLanguage(GiniLocalization.GERMAN, context)
        paymentComponentWithLocale = PaymentComponent(context, giniHealth)

        // When
        launchFragmentInContainer {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                paymentComponentWithLocale,
                mockk()
            )
        }

        // Then
        onView(withId(R.id.ghs_open_with_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Rechnungsdaten")))
        onView(withId(R.id.ghs_open_with_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Im nächsten")))
        onView(withId(R.id.ghs_open_with_info)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Tipp")))
        onView(withId(R.id.ghs_forward_button)).check(ViewAssertions.matches(ViewMatchers.withText("Weiter")))
    }
}