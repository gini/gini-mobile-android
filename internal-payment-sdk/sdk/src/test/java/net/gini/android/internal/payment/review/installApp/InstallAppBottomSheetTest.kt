package net.gini.android.internal.payment.review.installApp

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallAppBottomSheetTest {

    private lateinit var paymentProviderApp: PaymentProviderApp
    private lateinit var paymentComponent: PaymentComponent

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
}