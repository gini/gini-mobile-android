package net.gini.android.internal.payment

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
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
import net.gini.android.internal.payment.api.model.PaymentDetails
import net.gini.android.internal.payment.api.model.PaymentRequest
import net.gini.android.internal.payment.api.model.ResultWrapper
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentProvider.PaymentProviderApp
import net.gini.android.internal.payment.paymentProvider.PaymentProviderAppColors
import net.gini.android.internal.payment.review.openWith.OpenWithPreferences
import net.gini.android.internal.payment.utils.DisplayedScreen
import net.gini.android.internal.payment.utils.GiniLocalization
import net.gini.android.internal.payment.utils.GiniPaymentManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GiniInternalPaymentModuleTest {
    private lateinit var giniHealthAPI: GiniHealthAPI
    private lateinit var context: Context
    private lateinit var giniPaymentManager: GiniPaymentManager
    private lateinit var paymentComponent: PaymentComponent
    private lateinit var documentManager: HealthApiDocumentManager
    private lateinit var openWithPreferences: OpenWithPreferences
    private val invalidPaymentRequest = PaymentRequest("1234", null, null, "", "", null, "20", "", PaymentRequest.Status.INVALID)
    private val paymentProviderApp = PaymentProviderApp(
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
    private val paymentDetails = PaymentDetails(
        recipient = "recipient", iban = "iban", amount = "30", purpose = "purpose"
    )
    @Before
    fun setup() {
        giniHealthAPI = mockk(relaxed = true)
        context = getApplicationContext()
        giniPaymentManager = mockk(relaxed = true)
        paymentComponent = mockk(relaxed = true)
        documentManager = mockk(relaxed = true)
        openWithPreferences = OpenWithPreferences(context)
        every { giniHealthAPI.documentManager } returns documentManager
        every { giniPaymentManager.giniHealthAPI } returns giniHealthAPI
    }

    @Test
    fun `gets payment request`() = runTest {
        // Given
        coEvery { giniPaymentManager.getPaymentRequest(paymentProviderApp, paymentDetails) } coAnswers { invalidPaymentRequest }
        coEvery { giniHealthAPI.documentManager.getPaymentRequest("1234") } coAnswers { Resource.Success(net.gini.android.core.api.models.PaymentRequest(
            paymentProviderId = paymentProviderApp.paymentProvider.id,
            requesterUri = null,
            recipient = "",
            iban = "",
            bic = null,
            amount = "",
            purpose = "",
            status = net.gini.android.core.api.models.PaymentRequest.Status.OPEN
        )) }
        coEvery { giniHealthAPI.documentManager.createPaymentRequest(any()) } coAnswers { Resource.Success(invalidPaymentRequest.id) }

        val giniInternalPaymentModule = GiniInternalPaymentModule(context, giniHealthAPI)

        // When
        val paymentRequest = giniInternalPaymentModule.getPaymentRequest(paymentProviderApp, paymentDetails)

        // Then
        assertThat(paymentRequest.id).isEqualTo(invalidPaymentRequest.id)
    }

   @Test
   fun `emits payment details`() = runTest {
       //Given
       val giniInternalPaymentModule = GiniInternalPaymentModule(context, giniHealthAPI)
       val paymentDetails = PaymentDetails(
           recipient = "recipient", iban = "iban", amount = "40", purpose = "purpose"
       )

       giniInternalPaymentModule.paymentFlow.test {
           val initialState = awaitItem()
           assertThat(initialState).isInstanceOf(ResultWrapper.Loading::class.java)

           // When
           giniInternalPaymentModule.setPaymentDetails(paymentDetails)
           val updatedDetails = awaitItem()
           assertThat(updatedDetails).isInstanceOf(ResultWrapper.Success::class.java)

           //Then
           assertThat((updatedDetails as ResultWrapper.Success).value.amount).isEqualTo(paymentDetails.amount)
       }
   }

    @Test
    fun `increments count for payment provider id`() = runTest {
        //Given
        val giniInternalPaymentModule = GiniInternalPaymentModule(context, giniHealthAPI)

        giniInternalPaymentModule.getLiveCountForPaymentProviderId("123").test {
            assertThat(awaitItem()).isNull()

            //When
            giniInternalPaymentModule.incrementCountForPaymentProviderId("123")
            //Then
            assertThat(awaitItem()).isEqualTo(1)
        }
    }

    @Test()
    fun `emits SDK events`() = runTest {
        //Given
        val giniInternalPaymentModule = GiniInternalPaymentModule(context, giniHealthAPI)

        giniInternalPaymentModule.eventsFlow.test {
            //When
            giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnLoading)
            //Then
            assertThat(awaitItem()).isInstanceOf(GiniInternalPaymentModule.InternalPaymentEvents.OnLoading::class.java)

            //When
            giniInternalPaymentModule.emitSdkEvent(GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed(DisplayedScreen.ReviewScreen))
            //Then
            val newItem = awaitItem()
            assertThat(newItem).isInstanceOf(GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed::class.java)
            assertThat((newItem as GiniInternalPaymentModule.InternalPaymentEvents.OnScreenDisplayed).displayedScreen).isEqualTo(DisplayedScreen.ReviewScreen)
        }
    }

    @Test
    fun `sets SDK language`() = runTest {
        //Given
        val giniInternalPaymentModule = GiniInternalPaymentModule(context, giniHealthAPI)

        //When
        giniInternalPaymentModule.setSDKLanguage(GiniLocalization.GERMAN, context)

        //Then
        assertThat(GiniInternalPaymentModule.getSDKLanguage(context)).isEqualTo(GiniLocalization.GERMAN)
    }

    @Test
    fun `saves returning user`() = runTest {
        //Given
        val giniInternalPaymentModule = GiniInternalPaymentModule(context, giniHealthAPI)
        assertThat(giniInternalPaymentModule.getReturningUser()).isFalse()

        //When
        giniInternalPaymentModule.saveReturningUser()

        //Then
        assertThat(giniInternalPaymentModule.getReturningUser()).isTrue()
    }
}