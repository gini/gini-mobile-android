package net.gini.android.merchant.sdk

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.merchant.sdk.api.ResultWrapper
import net.gini.android.merchant.sdk.api.payment.model.PaymentDetails
import net.gini.android.merchant.sdk.paymentcomponent.PaymentComponent
import net.gini.android.merchant.sdk.review.error.NoPaymentDataExtracted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

val document =
    Document("1234", Document.ProcessingState.COMPLETED, "", 1, Date(124), Date(124), Document.SourceClassification.COMPOSITE, Uri.EMPTY, emptyList(), emptyList())

val extractions = ExtractionsContainer(
    emptyMap(),
    mapOf(
        "payment" to CompoundExtraction("payment", listOf(mutableMapOf(
            "payment_recipient" to SpecificExtraction("payment_recipient", "recipient", "", null, listOf()),
            "iban" to SpecificExtraction("iban", "iban", "", null, listOf()),
            "amount_to_pay" to SpecificExtraction("amount_tp_pay", "123.56", "", null, listOf()),
            "payment_purpose" to SpecificExtraction("payment_purpose", "purpose", "", null, listOf()),
        )))
    )
)

fun copyExtractions(extractions: ExtractionsContainer) = ExtractionsContainer(
    extractions.specificExtractions.toMap(),
    extractions.compoundExtractions.map { (name, compoundExtraction) ->
        name to CompoundExtraction(name, compoundExtraction.specificExtractionMaps.map { it.toMap() })
    }.toMap()
)

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class GiniMerchantTest {

    private var context: Context? = null
    private lateinit var giniMerchant: GiniMerchant
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk(relaxed = true) { HealthApiDocumentManager::class.java }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context!!.setTheme(R.style.GiniMerchantTheme)
        every { giniHealthAPI.documentManager } returns documentManager
        giniMerchant = GiniMerchant(mockk(relaxed = true)).apply {
            replaceHealthApiInstance(this@GiniMerchantTest.giniHealthAPI)
        }
        val paymentComponent = PaymentComponent(context!!, giniHealthAPI)
        giniMerchant.paymentComponent = paymentComponent
    }

    // TODO EC-62: Add method and tests for setting image/PDF instead of document or document id
//    @Test
//    fun `When setting document for review then document and payment flow emit success`() = runTest {
//        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(extractions)
//        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)
//
//        assert(giniMerchant.documentFlow.value is ResultWrapper.Loading<Document>) { "Expected Loading but was ${giniMerchant.documentFlow.value}" }
//        assert(giniMerchant.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniMerchant.paymentFlow.value}" }
//        giniMerchant.setDocumentForReview(document)
//        assert(giniMerchant.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success but was ${giniMerchant.documentFlow.value}" }
//        assert(giniMerchant.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success but was ${giniMerchant.documentFlow.value}" }
//        assert(giniMerchant.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success but was ${giniMerchant.paymentFlow.value}" }
//        assertEquals(document, (giniMerchant.documentFlow.value as ResultWrapper.Success<Document>).value)
//        assertEquals(paymentDetails, (giniMerchant.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
//    }
//
//    @Test
//    fun `When setting document for review then payment flow emits failure if extractions have no payment details`() = runTest {
//        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(ExtractionsContainer(
//            emptyMap(),
//            emptyMap()
//        ))
//
//        assert(giniMerchant.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniMerchant.paymentFlow.value}" }
//        giniMerchant.setDocumentForReview(document)
//        assert(giniMerchant.paymentFlow.value is ResultWrapper.Error<PaymentDetails>) { "Expected Success but was ${giniMerchant.paymentFlow.value}" }
//        assertTrue((giniMerchant.paymentFlow.value as ResultWrapper.Error<PaymentDetails>).error is NoPaymentDataExtracted)
//    }

    @Test
    fun `When setting document id for review with payment details then document flow emits document`() = runTest {
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose")

        assert(giniMerchant.documentFlow.value is ResultWrapper.Loading<Document>) { "Expected Loading" }
        giniMerchant.setDocumentForReview("1234", paymentDetails)
        assert(giniMerchant.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success" }
        assertEquals(document, (giniMerchant.documentFlow.value as ResultWrapper.Success<Document>).value)
    }

    @Test
    fun `When setting document id for review with payment details then payment flow emits those details`() = runTest {
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose")

        assert(giniMerchant.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniMerchant.setDocumentForReview("", paymentDetails)
        assert(giniMerchant.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        assertEquals(paymentDetails, (giniMerchant.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document id for review without payment details then payment flow emits details`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)

        assert(giniMerchant.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniMerchant.setDocumentForReview("")
        assert(giniMerchant.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        assertEquals(paymentDetails, (giniMerchant.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document id for review without payment details then payment flow emits failure if extractions have no payment details`() = runTest {
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(ExtractionsContainer(
            emptyMap(),
            emptyMap()
        ))

        assert(giniMerchant.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniMerchant.paymentFlow.value}" }
        giniMerchant.setDocumentForReview("")
        assert(giniMerchant.paymentFlow.value is ResultWrapper.Error<PaymentDetails>) { "Expected Success but was ${giniMerchant.paymentFlow.value}" }
        assertTrue((giniMerchant.paymentFlow.value as ResultWrapper.Error<PaymentDetails>).error is NoPaymentDataExtracted)
    }

    @Test
    fun `Document is payable if it has an IBAN extraction`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertTrue(giniMerchant.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has no IBAN extraction`() = runTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.remove("iban")
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithoutIBAN)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniMerchant.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has an empty IBAN extraction`() = runTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            compoundExtractions["payment"]?.specificExtractionMaps?.get(0)
                ?.set("iban", SpecificExtraction("iban", "", "", null, listOf()))
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithoutIBAN)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniMerchant.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document payable check throws an exception if get document API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(exception = Exception("Failed to get document"))

        var exception: Exception? = null

        try {
            giniMerchant.checkIfDocumentIsPayable(document.id)
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        Truth.assertThat(exception!!.message).contains("Failed to get document")
    }

    @Test
    fun `Document payable check throws an exception if get extractions API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(exception = Exception("Failed to get extractions"))
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        var exception: Exception? = null

        try {
            giniMerchant.checkIfDocumentIsPayable(document.id)
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        Truth.assertThat(exception!!.message).contains("Failed to get extractions")
    }
}