package net.gini.android.health.sdk

import android.net.Uri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.Payment
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.api.response.DeleteDocumentErrorResponse
import net.gini.android.health.api.response.DeletePaymentRequestErrorResponse
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.integratedFlow.PaymentFragment
import net.gini.android.health.sdk.review.error.NoPaymentDataExtracted
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

val document =
    Document("1234", Document.ProcessingState.COMPLETED, "", 1, Date(124), Date(100), Document.SourceClassification.COMPOSITE, Uri.EMPTY, emptyList(), emptyList())

val extractions = ExtractionsContainer(
    mapOf(
        "payment_state" to SpecificExtraction("payment_state", "Payable", "", null, listOf()),
        "medical_service_provider" to SpecificExtraction("medical_service_provider", "Dr. Test", "", null, listOf())
    ),
    mapOf(
        "payment" to CompoundExtraction("payment", listOf(mutableMapOf(
            "payment_recipient" to SpecificExtraction("payment_recipient", "recipient", "", null, listOf()),
            "iban" to SpecificExtraction("iban", "iban", "", null, listOf()),
            "amount_to_pay" to SpecificExtraction("amount_to_pay", "123.56", "", null, listOf()),
            "payment_purpose" to SpecificExtraction("payment_purpose", "purpose", "", null, listOf()),
            "payment_state" to SpecificExtraction("payment_state", "Payable", "", null, listOf())
        )))
    )
)

fun copyExtractions(extractions: ExtractionsContainer) = ExtractionsContainer(
    extractions.specificExtractions.toMap().mapValues { specificExtraction ->
        SpecificExtraction(
            specificExtraction.value.name,
            specificExtraction.value.value,
            specificExtraction.value.entity,
            specificExtraction.value.box,
            specificExtraction.value.candidate
        )
    },
    extractions.compoundExtractions.map { (name, compoundExtraction) ->
        name to CompoundExtraction(
            name,
            compoundExtraction.specificExtractionMaps.map { specificExtractionMap ->
                specificExtractionMap.toMap().mapValues { specificExtraction ->
                    SpecificExtraction(
                        specificExtraction.value.name,
                        specificExtraction.value.value,
                        specificExtraction.value.entity,
                        specificExtraction.value.box,
                        specificExtraction.value.candidate
                    )
                }
            })
    }.toMap()
)

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GiniHealthTest {

    @get:Rule
    val rule = ViewModelTestCoroutineRule()

    private lateinit var giniHealth: GiniHealth
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk(relaxed = true) { HealthApiDocumentManager::class.java }

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI, getApplicationContext())
    }

    @Test
    fun `When setting document for review then document and payment flow emit success`() = runTest {
        coEvery { documentManager.getConfigurations() } returns Resource.Cancelled()
        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(extractions)
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)

        assert(giniHealth.documentFlow.value is ResultWrapper.Loading<Document>) { "Expected Loading but was ${giniHealth.documentFlow.value}" }
        assert(giniHealth.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniHealth.paymentFlow.value}" }
        giniHealth.setDocumentForReview(document)
        assert(giniHealth.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success but was ${giniHealth.documentFlow.value}" }
        assert(giniHealth.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success but was ${giniHealth.documentFlow.value}" }
        assert(giniHealth.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success but was ${giniHealth.paymentFlow.value}" }
        assertEquals(document, (giniHealth.documentFlow.value as ResultWrapper.Success<Document>).value)
        assertEquals(paymentDetails, (giniHealth.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document for review then payment flow emits failure if extractions have no payment details`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(ExtractionsContainer(
            emptyMap(),
            emptyMap()
        ))

        assert(giniHealth.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniHealth.paymentFlow.value}" }
        giniHealth.setDocumentForReview(document)
        assert(giniHealth.paymentFlow.value is ResultWrapper.Error<PaymentDetails>) { "Expected Success but was ${giniHealth.paymentFlow.value}" }
        assertTrue((giniHealth.paymentFlow.value as ResultWrapper.Error<PaymentDetails>).error is NoPaymentDataExtracted)
    }

    @Test
    fun `When setting document id for review with payment details then document flow emits document`() = runTest {
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose")

        assert(giniHealth.documentFlow.value is ResultWrapper.Loading<Document>) { "Expected Loading" }
        giniHealth.setDocumentForReview("1234", paymentDetails)
        assert(giniHealth.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success" }
        assertEquals(document, (giniHealth.documentFlow.value as ResultWrapper.Success<Document>).value)
    }

    @Test
    fun `When setting document id for review with payment details then payment flow emits those details`() = runTest {
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose")

        assert(giniHealth.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniHealth.setDocumentForReview("", paymentDetails)
        assert(giniHealth.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        assertEquals(paymentDetails, (giniHealth.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document id for review without payment details then payment flow emits details`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)

        assert(giniHealth.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniHealth.setDocumentForReview("")
        assert(giniHealth.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        assertEquals(paymentDetails, (giniHealth.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document id for review without payment details then payment flow emits failure if extractions have no payment details`() = runTest {
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(ExtractionsContainer(
            emptyMap(),
            emptyMap()
        ))

        assert(giniHealth.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniHealth.paymentFlow.value}" }
        giniHealth.setDocumentForReview("")
        assert(giniHealth.paymentFlow.value is ResultWrapper.Error<PaymentDetails>) { "Expected Success but was ${giniHealth.paymentFlow.value}" }
        assertTrue((giniHealth.paymentFlow.value as ResultWrapper.Error<PaymentDetails>).error is NoPaymentDataExtracted)
    }

    @Test
    fun `Document is payable if it has payment state extraction and that equals Payable`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertTrue(giniHealth.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has payment state extraction and that equals Other`() = runTest {
        val extractionsWithPaymentStateOther = copyExtractions(extractions).apply {
            specificExtractions["payment_state"]?.value = "Other"
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithPaymentStateOther)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniHealth.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has an empty payment state extraction`() = runTest {
        val extractionsWithPaymentStateEmpty = copyExtractions(extractions).apply {
            specificExtractions["payment_state"]?.value = ""
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithPaymentStateEmpty)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniHealth.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document payable check throws an exception if get document API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(exception = Exception("Failed to get document"))
        coEvery { documentManager.getConfigurations() } returns mockk(relaxed = true)

        var exception: Exception? = null

        try {
            giniHealth.checkIfDocumentIsPayable(document.id)
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
            giniHealth.checkIfDocumentIsPayable(document.id)
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        Truth.assertThat(exception!!.message).contains("Failed to get extractions")
    }

    @Test
    fun `instantiates payment fragment with documentId`() {
        // Given
        val paymentFlowConfiguration: PaymentFlowConfiguration = mockk(relaxed = true)
        val giniHealth: GiniHealth = mockk(relaxed = true)

        // Then
        Truth.assertThat(giniHealth.getPaymentFragmentWithDocument("123", paymentFlowConfiguration)).isInstanceOf(PaymentFragment::class.java)
    }

    @Test
    fun `instantiates payment fragment if payment details are valid`() {
        // Given
        val paymentFlowConfiguration: PaymentFlowConfiguration = mockk(relaxed = true)
        val giniHealth: GiniHealth = mockk(relaxed = true)

        // Then
        Truth.assertThat(giniHealth.getPaymentFragmentWithoutDocument(PaymentDetails("recipient", "iban", "40", "purpose"), paymentFlowConfiguration)).isInstanceOf(PaymentFragment::class.java)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws exception when trying to create Payment fragment if payment details are incomplete`() {
        // Given
        val paymentFlowConfiguration: PaymentFlowConfiguration = mockk(relaxed = true)
        val paymentComponent = mockk<PaymentComponent>(relaxed = true)

        // When
        every { paymentComponent.selectedPaymentProviderAppFlow } returns MutableStateFlow(
            SelectedPaymentProviderAppState.NothingSelected)

        // When trying to instantiate fragment, then exception should be thrown
        giniHealth.getPaymentFragmentWithoutDocument(PaymentDetails("", "", "", ""), paymentFlowConfiguration)
    }

    @Test
    fun `When setting document id for review with medical provider details then that value can be reached from payment details`() = runTest {
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)

        assert(giniHealth.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniHealth.setDocumentForReview("", paymentDetails)
        assert(giniHealth.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        val result = (giniHealth.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value.extractions?.specificExtractions?.get("medical_service_provider")
        assertEquals(extractions.specificExtractions["medical_service_provider"], result)
    }

    @Test
    fun `Returns null when batch delete was successful`() = runTest {
        coEvery { documentManager.deleteDocuments(any()) } returns Resource.Success(Unit)

        val result = giniHealth.deleteDocuments(listOf())
        assertTrue(result == null)
    }

    @Test
    fun `Returns DocumentResponseError with message field when batch delete with empty list`() = runTest {
        coEvery { documentManager.deleteDocuments(emptyList()) } returns Resource.Error("{ \"message\": \"No documents to expire\" }")

        val result = giniHealth.deleteDocuments(emptyList())
        Truth.assertThat(result).isInstanceOf(DeleteDocumentErrorResponse::class.java)
        Truth.assertThat(result?.message).isNotNull()
    }

    @Test
    fun `Returns DocumentResponseError with unauthorizedDocuments field when batch delete documents that do not belong to the user`() = runTest {
        coEvery { documentManager.deleteDocuments(emptyList()) } returns Resource.Error(
                "  { \"unauthorizedDocuments\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }" )

        val result = giniHealth.deleteDocuments(emptyList())
        Truth.assertThat(result).isInstanceOf(DeleteDocumentErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.notFoundDocuments).isNull()
        Truth.assertThat(result?.missingCompositeDocuments).isNull()
        Truth.assertThat(result?.unauthorizedDocuments).isNotNull()
        Truth.assertThat(result?.unauthorizedDocuments).isNotEmpty()
        Truth.assertThat(result?.unauthorizedDocuments).hasSize(2)
    }

    @Test
    fun `Returns DocumentResponseError with notFoundDocuments field when batch delete documents that do not exist anymore`() = runTest {
        coEvery { documentManager.deleteDocuments(emptyList()) } returns Resource.Error(
            "  { \"notFoundDocuments\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }" )

        val result = giniHealth.deleteDocuments(emptyList())
        Truth.assertThat(result).isInstanceOf(DeleteDocumentErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.unauthorizedDocuments).isNull()
        Truth.assertThat(result?.missingCompositeDocuments).isNull()
        Truth.assertThat(result?.notFoundDocuments).isNotNull()
        Truth.assertThat(result?.notFoundDocuments).isNotEmpty()
        Truth.assertThat(result?.notFoundDocuments).hasSize(2)
    }

    @Test
    fun `Returns DocumentResponseError with missingCompositeDocuments field when batch delete documents that do not have the composite documents`() = runTest {
        coEvery { documentManager.deleteDocuments(emptyList()) } returns Resource.Error(
            "  { \"missingCompositeDocuments\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }" )

        val result = giniHealth.deleteDocuments(emptyList())
        Truth.assertThat(result).isInstanceOf(DeleteDocumentErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.unauthorizedDocuments).isNull()
        Truth.assertThat(result?.notFoundDocuments).isNull()
        Truth.assertThat(result?.missingCompositeDocuments).isNotNull()
        Truth.assertThat(result?.missingCompositeDocuments).isNotEmpty()
        Truth.assertThat(result?.missingCompositeDocuments).hasSize(2)
    }

    @Test
    fun `Returns DocumentResponseError with notFoundDocuments and unauthorizedDocuments fields when batch delete documents that do not exist anymore and documents that do not belong to the user`() = runTest {
        coEvery { documentManager.deleteDocuments(emptyList()) } returns Resource.Error(
            "  { \"notFoundDocuments\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"], \"unauthorizedDocuments\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }" )

        val result = giniHealth.deleteDocuments(emptyList())
        Truth.assertThat(result).isInstanceOf(DeleteDocumentErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.missingCompositeDocuments).isNull()
        Truth.assertThat(result?.notFoundDocuments).isNotNull()
        Truth.assertThat(result?.notFoundDocuments).isNotEmpty()
        Truth.assertThat(result?.notFoundDocuments).hasSize(2)
        Truth.assertThat(result?.unauthorizedDocuments).isNotNull()
        Truth.assertThat(result?.unauthorizedDocuments).isNotEmpty()
        Truth.assertThat(result?.unauthorizedDocuments).hasSize(2)
    }

    @Test
    fun `Returns null when delete payment request was successful`() = runTest {
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Success(Unit)

        val result = giniHealth.deletePaymentRequest("")

        assertTrue(result == null)
    }

    @Test
    fun `Returns error message when delete request returned with error`() = runTest {
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Error("{ \"message\": \"Payment request not found\" }")

        val result = giniHealth.deletePaymentRequest("")

        assertEquals("{ \"message\": \"Payment request not found\" }", result)
    }

    @Test
    fun `Returns Request cancelled when delete request was cancelled`() = runTest {
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Cancelled()

        val result = giniHealth.deletePaymentRequest("")

        assertEquals("Request cancelled", result)
    }

    @Test
    fun `Returns Payment model when getPayment request was successful`() = runTest {
        // Arrange: Create a mock Payment model
        val mockPayment = Payment(
            paidAt = "2021-03-18T16:29:37.572409",
            recipient = "Dr. med. Hackler",
            iban = "DE02300209000106531065",
            amount = "335.50:EUR",
            purpose = "ReNr AZ356789Z",
            bic = "CMCIDEDDXXX"
        )
        // Mock the response from documentManager.getPayment
        coEvery { documentManager.getPayment(any()) } returns Resource.Success(mockPayment)

        // Act: Call getPayment function
        val result = giniHealth.getPayment("d89f26ab-a374-4c69-ae83-a88b078e4c49")

        // Assert: Check that the result is the Payment model
        assertTrue(result == mockPayment)
    }

    @Test
    fun `Returns error message when getPayment request returns an error`() = runTest {
        // Arrange: Mock error response from documentManager.getPayment
        coEvery { documentManager.getPayment(any()) } returns Resource.Error("{ \"message\": \"Payment request not found\" }")

        // Act: Call getPayment function
        val result = try {
            giniHealth.getPayment("d89f26ab-a374-4c69-ae83-a88b078e4c49")
            null // This should not be reached
        } catch (e: Exception) {
            e.message
        }

        // Assert: Check that the error message is returned
        assertEquals("{ \"message\": \"Payment request not found\" }", result)
    }

    @Test
    fun `Returns Request cancelled when getPayment request was cancelled`() = runTest {
        // Arrange: Mock cancelled response from documentManager.getPayment
        coEvery { documentManager.getPayment(any()) } returns Resource.Cancelled()

        // Act: Call getPayment function
        val result = try {
            giniHealth.getPayment("d89f26ab-a374-4c69-ae83-a88b078e4c49")
            null // This should not be reached
        } catch (e: Exception) {
            e.message
        }

        // Assert: Check that the request was cancelled
        assertEquals("Request cancelled", result)
    }


    @Test
    fun `Returns null when batch delete payment requests was successful`() = runTest {
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Success(Unit)

        val result = giniHealth.deletePaymentRequests(listOf())
        assertTrue(result == null)
    }

    @Test
    fun `Returns DeletePaymentRequestErrorResponse with message field when batch delete with empty list`() = runTest {
        coEvery { documentManager.deletePaymentRequests(emptyList()) } returns Resource.Error(
            "{ \"message\": \"No payment requests to delete\" }"
        )

        val result = giniHealth.deletePaymentRequests(emptyList())
        Truth.assertThat(result).isInstanceOf(DeletePaymentRequestErrorResponse::class.java)
        Truth.assertThat(result?.message).isEqualTo("No payment requests to delete")
    }

    @Test
    fun `Returns DeletePaymentRequestErrorResponse with unauthorizedPaymentRequests field when batch delete payment requests that do not belong to the user`() = runTest {
        coEvery { documentManager.deletePaymentRequests(emptyList()) } returns Resource.Error(
            "{ \"unauthorizedPaymentRequests\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }"
        )

        val result = giniHealth.deletePaymentRequests(emptyList())
        Truth.assertThat(result).isInstanceOf(DeletePaymentRequestErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.unauthorizedPaymentRequests).isNotNull()
        Truth.assertThat(result?.unauthorizedPaymentRequests).hasSize(2)
    }

    @Test
    fun `Returns DeletePaymentRequestErrorResponse with notFoundPaymentRequests field when batch delete payment requests that do not exist anymore`() = runTest {
        coEvery { documentManager.deletePaymentRequests(emptyList()) } returns Resource.Error(
            "{ \"notFoundPaymentRequests\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }"
        )

        val result = giniHealth.deletePaymentRequests(emptyList())
        Truth.assertThat(result).isInstanceOf(DeletePaymentRequestErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.notFoundPaymentRequests).isNotNull()
        Truth.assertThat(result?.notFoundPaymentRequests).hasSize(2)
    }

    @Test
    fun `Returns DeletePaymentRequestErrorResponse with notFoundPaymentRequests and unauthorizedPaymentRequests fields when batch delete contains both cases`() = runTest {
        coEvery { documentManager.deletePaymentRequests(emptyList()) } returns Resource.Error(
            "{ \"notFoundPaymentRequests\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"], \"unauthorizedPaymentRequests\": [\"0eb26fec-4a7f-4376-b5d5-5155adf8adca\", \"8f434c37-7167-4c42-ac10-02e0826bba98\"] }"
        )

        val result = giniHealth.deletePaymentRequests(emptyList())
        Truth.assertThat(result).isInstanceOf(DeletePaymentRequestErrorResponse::class.java)
        Truth.assertThat(result?.message).isNull()
        Truth.assertThat(result?.notFoundPaymentRequests).isNotNull()
        Truth.assertThat(result?.notFoundPaymentRequests).hasSize(2)
        Truth.assertThat(result?.unauthorizedPaymentRequests).isNotNull()
        Truth.assertThat(result?.unauthorizedPaymentRequests).hasSize(2)
    }


    @Test
    fun `Returns DeletePaymentRequestErrorResponse with cancellation message when batch delete is cancelled`() = runTest {
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Cancelled()

        val result = giniHealth.deletePaymentRequests(listOf())
        Truth.assertThat(result).isInstanceOf(DeletePaymentRequestErrorResponse::class.java)
        Truth.assertThat(result?.message).isEqualTo("Delete payment requests request was cancelled")
    }
}
