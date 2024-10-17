package net.gini.android.internal.payment

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.utils.GiniPaymentManager
import net.gini.android.internal.payment.utils.PaymentEventListener
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GiniPaymentManagerTest {
    private lateinit var context: Context
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var giniPaymentModule: GiniInternalPaymentModule
    private lateinit var giniHealthAPI: GiniHealthAPI
    private val paymentDetails = PaymentDetails(recipient = "recipient",
        iban = "iban",
        amount = "10",
        purpose = "purpose")

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.GiniPaymentTheme)
        paymentComponent = mockk(relaxed = true)
        giniPaymentModule = mockk(relaxed = true)
        giniHealthAPI = mockk(relaxed = true)
    }

    @Test(expected = Exception::class)
    fun `throws exception if trying to make payment when GiniHealthAPI is null`() = runTest {
        //Given
        val giniPaymentManager = GiniPaymentManager(null, null)

        //When
        giniPaymentManager.onPayment(null, paymentDetails)
    }

    @Test
    fun `calls onError function on listener if payment provider app is null when creating payment request`() = runTest {
        val paymentEventListener : PaymentEventListener = mockk(relaxed = true)
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, paymentEventListener)

        // When
        giniPaymentManager.onPayment(null, paymentDetails)

        //Then
        verify(exactly = 0) { paymentEventListener.onError(Exception("No selected payment provider app")) }
    }

    @Test
    fun `calls onError function on listener if payment provider is not installed when creating payment request`() = runTest {
        val paymentEventListener : PaymentEventListener = mockk(relaxed = true)
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, paymentEventListener)

        // When
        giniPaymentManager.onPayment(PaymentProviderApp(
            name = "payment provider",
            colors = mockk(relaxed = true),
            icon = null,
            paymentProvider = mockk(relaxed = true),
            installedPaymentProviderApp = null
        ), paymentDetails)

        //Then
        verify(exactly = 0) { paymentEventListener.onError(Exception("No selected payment provider app")) }
    }

    @Test
    fun `calls onPaymentRequestCreated method of listener is payment provider app is installed`() = runTest {
        val paymentEventListener : PaymentEventListener = mockk(relaxed = true)
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, paymentEventListener)

        // When
        giniPaymentManager.onPayment(PaymentProviderApp(
            name = "payment provider",
            colors = mockk(relaxed = true),
            icon = null,
            paymentProvider = mockk(relaxed = true),
            installedPaymentProviderApp = mockk(relaxed = true)
        ), paymentDetails)

        //Then
        verify(exactly = 0) { paymentEventListener.onPaymentRequestCreated(any(), any()) }
    }

    @Test(expected = Exception::class)
    fun `throws exception if trying to get payment request when GiniHealthAPI is null`() = runTest {
        //Given
        val giniPaymentManager = GiniPaymentManager(null, null)

        //When
        giniPaymentManager.getPaymentRequest(null, paymentDetails)
    }

    @Test(expected = Exception::class)
    fun `throws exception if trying to get payment request when paymentProviderApp is null`() = runTest {
        //Given
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, null)

        //When
        giniPaymentManager.getPaymentRequest(null, paymentDetails)
    }

    @Test(expected = Exception::class)
    fun `throws exception if getting payment request was cancelled`() = runTest {
        //Given
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, null)
        coEvery { giniHealthAPI.documentManager.createPaymentRequest(any()) } coAnswers { Resource.Cancelled() }

        val paymentProviderApp = PaymentProviderApp(
            name = "payment provider",
            colors = mockk(relaxed = true),
            icon = null,
            paymentProvider = mockk(relaxed = true),
            installedPaymentProviderApp = mockk(relaxed = true)
        )
        // When - Then
        giniPaymentManager.getPaymentRequest(paymentProviderApp, paymentDetails)
    }

    @Test(expected = Exception::class)
    fun `throws exception if getting payment request returned with an error`() = runTest {
        //Given
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, null)
        coEvery { giniHealthAPI.documentManager.createPaymentRequest(any()) } coAnswers { Resource.Error() }

        val paymentProviderApp = PaymentProviderApp(
            name = "payment provider",
            colors = mockk(relaxed = true),
            icon = null,
            paymentProvider = mockk(relaxed = true),
            installedPaymentProviderApp = mockk(relaxed = true)
        )
        // When - Then
        giniPaymentManager.getPaymentRequest(paymentProviderApp, paymentDetails)
    }

    @Test
    fun `returns PaymentRequest if getting payment request was successful`() = runTest {
        //Given
        val giniPaymentManager = GiniPaymentManager(giniHealthAPI, null)
        coEvery { giniHealthAPI.documentManager.createPaymentRequest(any()) } coAnswers { Resource.Success(data = "payment request id") }
        coEvery { giniHealthAPI.documentManager.getPaymentRequest("payment request id") } coAnswers { Resource.Success(
            mockk(relaxed = true)
        ) }

        val paymentProviderApp = PaymentProviderApp(
            name = "payment provider",
            colors = mockk(relaxed = true),
            icon = null,
            paymentProvider = mockk(relaxed = true),
            installedPaymentProviderApp = mockk(relaxed = true)
        )
        // When
        val paymentRequest = giniPaymentManager.getPaymentRequest(paymentProviderApp, paymentDetails)

        //Then
        assertThat(paymentRequest).isInstanceOf(PaymentRequest::class.java)
        assertThat(paymentRequest.id).isEqualTo("payment request id")
    }
}
