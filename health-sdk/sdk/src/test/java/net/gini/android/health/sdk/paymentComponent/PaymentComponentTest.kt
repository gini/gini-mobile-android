package net.gini.android.health.sdk.paymentComponent

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.review.ReviewFragment
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.review.ReviewConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PaymentComponentTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private var context: Context? = null
    private var giniHealth: GiniHealth? = null
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI)
        context = getApplicationContext()
    }

    @After
    fun tearDown() {
        giniHealth = null
        context = null
        unmockkAll()
    }

    @Test(expected = IllegalStateException::class)
    fun `throws exception when trying to create ReviewFragment if no payment provider app is set`() {
        // Given
        val reviewConfiguration: ReviewConfiguration = mockk(relaxed = true)
        val paymentComponent = PaymentComponent(context!!, giniHealth!!)

        // When trying to instantiate fragment, then exception should be thrown
        paymentComponent.getPaymentReviewFragment("", reviewConfiguration)
    }

    @Test
    fun `instantiates review fragment if payment provider app is set`() {
        // Given
        val reviewConfiguration: ReviewConfiguration = mockk(relaxed = true)
        val paymentComponent: PaymentComponent = mockk(relaxed = true)
        val paymentProviderApp: PaymentProviderApp = mockk(relaxed = true)

        // When
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(SelectedPaymentProviderAppState.AppSelected(paymentProviderApp))

        // Then
        assertThat(paymentComponent.getPaymentReviewFragment("", reviewConfiguration)).isInstanceOf(ReviewFragment::class.java)
    }
}