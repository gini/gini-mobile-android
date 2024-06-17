package net.gini.android.merchant.sdk.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.merchant.sdk.GiniMerchant
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderAppColors
import net.gini.android.merchant.sdk.review.ValidationMessage
import net.gini.android.merchant.sdk.review.model.PaymentDetails
import net.gini.android.merchant.sdk.review.model.PaymentRequest
import net.gini.android.merchant.sdk.test.ViewModelTestCoroutineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GiniPaymentTest {

    @get:Rule
    val testCoroutineRule = ViewModelTestCoroutineRule()

    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }
    private var giniMerchant: GiniMerchant? = null

    private var paymentProviderApp =  PaymentProviderApp(
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
        )

    private var paymentDetails = PaymentDetails(
        recipient = "recipient",
        iban = "iban",
        amount = "20",
        purpose = "purpose",
        extractions = null
    )

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        giniMerchant = GiniMerchant(giniHealthAPI)
    }

    @Test(expected = Exception::class)
    fun `throws exception if payment provider is null when creating payment request`() = runTest {
        // Given
        val giniPayment = GiniPaymentManager(giniMerchant)

        // When - Then should throw error
        giniPayment.getPaymentRequest(null, paymentDetails)
    }

    @Test(expected = Exception::class)
    fun `throws exception if payment request was canceled`() = runTest {
        // Given
        coEvery { documentManager.createPaymentRequest(any()) } coAnswers  { Resource.Cancelled() }
        val giniPayment = GiniPaymentManager(giniMerchant)

        // When - Then should throw error
        giniPayment.getPaymentRequest(paymentProviderApp, paymentDetails)
    }

    @Test(expected = Exception::class)
    fun `throws exception if payment request was not successful`() = runTest {
        // Given
        coEvery { documentManager.createPaymentRequest(any()) } coAnswers  { Resource.Error() }
        val giniPayment = GiniPaymentManager(giniMerchant)

        // When - Then should throw error
        giniPayment.getPaymentRequest(paymentProviderApp, paymentDetails)
    }

    @Test
    fun `returns successful if payment request was successful`() = runTest {
        //Given
        coEvery { documentManager.createPaymentRequest(any()) } coAnswers  { Resource.Success("123") }
        val giniPayment = GiniPaymentManager(giniMerchant)

        // Then
        assertThat(giniPayment.getPaymentRequest(paymentProviderApp, paymentDetails)).isInstanceOf(PaymentRequest::class.java)
    }

    @Test
    fun `emits validation on flow`() = runTest {
        // Given
        val giniPayment = GiniPaymentManager(giniMerchant)

        // When
        giniPayment.emitPaymentValidation(emptyList())

        // Then
        giniPayment.paymentValidation.test {
            assertThat(awaitItem()).isEmpty()

            giniPayment.emitPaymentValidation(listOf(ValidationMessage.InvalidIban))
            val newItem = awaitItem()

            assertThat(newItem).isNotEmpty()
            assertThat(newItem[0]).isInstanceOf(ValidationMessage.InvalidIban::class.java)
        }
    }
}