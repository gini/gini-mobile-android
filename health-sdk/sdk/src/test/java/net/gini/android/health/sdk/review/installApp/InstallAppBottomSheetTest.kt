package net.gini.android.health.sdk.review.installApp

import android.content.Context
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.R
import net.gini.android.health.sdk.paymentcomponent.PaymentComponent
import net.gini.android.health.sdk.paymentcomponent.SelectedPaymentProviderAppState
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.health.sdk.review.openWith.OpenWithBottomSheet
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallAppBottomSheetTest {

    private lateinit var paymentProviderApp: PaymentProviderApp
    private lateinit var paymentComponent: PaymentComponent
    private var paymentComponentWithLocale: PaymentComponent? = null
    private lateinit var giniHealth: GiniHealth
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }

    @Before
    fun setup() {
        val paymentProviderName = "Test Bank App"
        paymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName

        paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
        every { paymentComponent.giniHealth.localizedContext } returns ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `get it on play store button visible if bank app not installed`() = runTest {
        every { paymentProviderApp.isInstalled() } returns false
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        // Given
        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponent,
            mockk(),
            0
        )

        // When
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.ghs_play_store_logo)).check { view, _ -> Truth.assertThat(view.visibility).isEqualTo(View.VISIBLE) }
    }

    @Test
    fun `forward button visible if bank app not installed`() = runTest {
        // Given
        every { paymentProviderApp.isInstalled() } returns true
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponent,
            mockk(),
            0
        )

        // When
        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.ghs_forward_button)).check { view, _ -> Truth.assertThat(view.visibility).isEqualTo(View.VISIBLE) }
    }

    @Test
    fun `redirect to bank called when tapping on forward button`() = runTest {
        // Given
        every { paymentProviderApp.isInstalled() } returns true
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        val listener: InstallAppForwardListener = mockk()
        every { listener.onForwardToBankSelected() } returns mockk()

        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponent,
            listener,
            0
        )

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            bottomSheet
        }

        onView(withId(R.id.ghs_forward_button)).check { view, _ -> Truth.assertThat(view.visibility).isEqualTo(View.VISIBLE) }

        // When
        onView(withId(R.id.ghs_forward_button)).perform(ViewActions.click())

        // Then
        verify(exactly = 1) { listener.onForwardToBankSelected() }
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
        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponentWithLocale!!,
            mockk(),
            0
        )

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.ghs_install_app_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Invoice")))
        onView(withId(R.id.ghs_install_app_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Note:")))
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
        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponentWithLocale!!,
            mockk(),
            0
        )

        launchFragmentInContainer(themeResId = R.style.GiniHealthTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.ghs_install_app_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Rechnungsdaten")))
        onView(withId(R.id.ghs_install_app_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Hinweis:")))
        onView(withId(R.id.ghs_forward_button)).check(ViewAssertions.matches(ViewMatchers.withText("Weiter")))
    }
}