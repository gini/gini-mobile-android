package net.gini.android.internal.payment.review.installApp

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
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.GiniLocalization
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallAppBottomSheetTest {

    private lateinit var paymentProviderApp: PaymentProviderApp
    private lateinit var paymentComponent: PaymentComponent
    private var paymentComponentWithLocale: PaymentComponent? = null
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private lateinit var giniPaymentModule: GiniInternalPaymentModule

    @Before
    fun setup() {
        val paymentProviderName = "Test Bank App"
        paymentProviderApp = mockk(relaxed = true)
        every { paymentProviderApp.name } returns paymentProviderName

        paymentComponent = mockk<PaymentComponent>()
        every { paymentComponent.recheckWhichPaymentProviderAppsAreInstalled() } returns Unit
    }

    @Test
    fun `get it on play store button visible if bank app not installed`() = runTest {
        every { paymentProviderApp.isInstalled() } returns false
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))
        every { paymentComponent.paymentModule } returns mockk(relaxed = true)

        // Given
        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponent,
            mockk(),
            mockk(),
            0
        )

        // When
        launchFragmentInContainer(themeResId = R.style.GiniPaymentTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.gps_play_store_logo)).check { view, _ -> Truth.assertThat(view.visibility).isEqualTo(View.VISIBLE) }
    }

    @Test
    fun `forward button visible if bank app not installed`() = runTest {
        // Given
        every { paymentProviderApp.isInstalled() } returns true
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))
        every { paymentComponent.paymentModule } returns mockk(relaxed = true)

        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponent,
            mockk(),
            mockk(),
            0
        )

        // When
        launchFragmentInContainer(themeResId = R.style.GiniPaymentTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.gps_forward_button)).check { view, _ -> Truth.assertThat(view.visibility).isEqualTo(View.VISIBLE) }
    }

    @Test
    fun `redirect to bank called when tapping on forward button`() = runTest {
        // Given
        every { paymentProviderApp.isInstalled() } returns true
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))
        every { paymentComponent.paymentModule } returns mockk(relaxed = true)

        val listener: InstallAppForwardListener = mockk()
        every { listener.onForwardToBankSelected() } returns mockk()

        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponent,
            listener,
            mockk(relaxed = true),
            0
        )

        launchFragmentInContainer(themeResId = R.style.GiniPaymentTheme) {
            bottomSheet
        }

        onView(withId(R.id.gps_forward_button)).check { view, _ -> Truth.assertThat(view.visibility).isEqualTo(View.VISIBLE) }

        // When
        onView(withId(R.id.gps_forward_button)).perform(ViewActions.click())

        // Then
        verify(exactly = 1) { listener.onForwardToBankSelected() }
    }

    @Test
    fun `shows text values in english if that is set to GiniHealth`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        giniPaymentModule = GiniInternalPaymentModule(context)
        giniPaymentModule.setSDKLanguage(GiniLocalization.ENGLISH, context)
        paymentComponentWithLocale = PaymentComponent(context, giniPaymentModule)
        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        every { giniHealthAPI.documentManager } returns documentManager


        // When
        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponentWithLocale!!,
            mockk(),
            mockk(),
            0
        )

        launchFragmentInContainer(themeResId = R.style.GiniPaymentTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.gps_install_app_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Invoice")))
        onView(withId(R.id.gps_install_app_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Note:")))
        onView(withId(R.id.gps_forward_button)).check(ViewAssertions.matches(ViewMatchers.withText("Forward")))
    }

    @Test
    fun `shows text values in german if that is set to GiniHealth`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        giniPaymentModule = GiniInternalPaymentModule(context)
        giniPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context)
        paymentComponentWithLocale = PaymentComponent(context, giniPaymentModule)
        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        every { giniHealthAPI.documentManager } returns documentManager


        // When
        val bottomSheet = InstallAppBottomSheet.newInstance(
            paymentComponentWithLocale!!,
            mockk(),
            mockk(),
            0
        )

        launchFragmentInContainer(themeResId = R.style.GiniPaymentTheme) {
            bottomSheet
        }

        // Then
        onView(withId(R.id.gps_install_app_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Rechnungsdaten")))
        onView(withId(R.id.gps_install_app_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Hinweis:")))
        onView(withId(R.id.gps_forward_button)).check(ViewAssertions.matches(ViewMatchers.withText("Weiter")))
    }
}