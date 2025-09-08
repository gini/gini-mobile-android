package net.gini.android.internal.payment.review.openWith

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.internal.payment.GiniInternalPaymentModule
import net.gini.android.internal.payment.R
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.utils.GiniLocalization
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenWithBottomSheetTest {

    private var paymentComponentWithLocale: PaymentComponent? = null
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private lateinit var giniPaymentModule: GiniInternalPaymentModule

    @Test
    fun `listener method called when 'Forward' button tapped`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        giniPaymentModule = GiniInternalPaymentModule(context)
        val listener: OpenWithForwardListener = mockk()
        every { listener.onForwardSelected() } returns mockk()
        paymentComponentWithLocale = PaymentComponent(context, giniPaymentModule)
        giniPaymentModule.setSDKLanguage(GiniLocalization.ENGLISH, context)

        launchFragmentInContainer(themeResId = R.style.GiniPaymentTheme) {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                paymentComponent = paymentComponentWithLocale,
                listener = listener,
                paymentDetails = mockk(relaxed = true),
                paymentRequestId = null
            )
        }

        // When
        onView(withId(R.id.gps_forward_button)).perform(ViewActions.click())

        // Then
        verify { listener.onForwardSelected() }
    }

    @Test
    fun `shows text values in english if that is set to module`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        every { giniHealthAPI.documentManager } returns documentManager

        giniPaymentModule = GiniInternalPaymentModule(context)
        giniPaymentModule.setSDKLanguage(GiniLocalization.ENGLISH, context)
        paymentComponentWithLocale = PaymentComponent(context, giniPaymentModule)

        // When
        launchFragmentInContainer {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                mockk(),
                paymentComponentWithLocale,
                mockk(),
                mockk(relaxed = true),
                null
            )
        }

        // Then
        onView(withId(R.id.gps_open_with_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Share")))
        onView(withId(R.id.gps_open_with_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("screenshot")))
        onView(withId(R.id.gps_forward_button)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Share")))
    }

    @Test
    fun `shows text values in german if that is set to module`() = runTest {
        // Given
        val context: Context = ApplicationProvider.getApplicationContext()
        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        every { giniHealthAPI.documentManager } returns documentManager

        giniPaymentModule = GiniInternalPaymentModule(context)
        giniPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context)
        paymentComponentWithLocale = PaymentComponent(context, giniPaymentModule)

        // When
        launchFragmentInContainer {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                mockk(),
                paymentComponentWithLocale,
                mockk(),
                mockk(relaxed = true),
                paymentRequestId = null
            )
        }

        // Then
        onView(withId(R.id.gps_open_with_title)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Zahlungscode")))
        onView(withId(R.id.gps_open_with_details)).check(ViewAssertions.matches(ViewMatchers.withSubstring("Fotoüberweisung")))
        onView(withId(R.id.gps_forward_button)).check(ViewAssertions.matches(ViewMatchers.withSubstring("teilen")))
    }

    @Test
    fun `calls getPaymentRequestImage on DocumentManager if paymentRequestId is not null`() = runTest {
        // Given
        val byteArray = byteArrayOf()

        val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
        coEvery { documentManager.getPaymentRequestImage(any()) } coAnswers { Resource.Success(byteArray) }
        every { giniHealthAPI.documentManager } returns documentManager

        val giniInternalPaymentModule: GiniInternalPaymentModule = mockk(relaxed = true)
        every { giniInternalPaymentModule.giniHealthAPI } returns giniHealthAPI

        val paymentComponent: PaymentComponent = mockk(relaxed = true)
        every { paymentComponent.paymentModule } returns giniInternalPaymentModule

        // When
        launchFragmentInContainer {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                mockk(),
                paymentComponent,
                mockk(),
                mockk(relaxed = true),
                paymentRequestId = "123"
            )
        }

        coVerify { documentManager.getPaymentRequestImage("123") }

    }
}