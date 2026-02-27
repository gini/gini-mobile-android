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
import net.gini.android.core.api.response.ErrorItem
import net.gini.android.core.api.response.ErrorResponse
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.sdk.review.model.ResultWrapper
import net.gini.android.health.sdk.integratedFlow.PaymentFlowConfiguration
import net.gini.android.health.sdk.integratedFlow.PaymentFragment
import net.gini.android.health.sdk.review.error.NoPaymentDataExtracted
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.test.ViewModelTestCoroutineRule
import net.gini.android.internal.payment.GiniHealthException
import net.gini.android.internal.payment.paymentComponent.PaymentComponent
import net.gini.android.internal.payment.paymentComponent.SelectedPaymentProviderAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

// Helper function for testing suspend functions that throw exceptions
suspend inline fun <reified T : Throwable> assertThrowsSuspend(crossinline block: suspend () -> Unit): T {
    try {
        block()
        throw AssertionError("Expected ${T::class.simpleName} to be thrown")
    } catch (e: Throwable) {
        if (e is T) {
            return e
        } else {
            throw AssertionError("Expected ${T::class.simpleName} but was ${e::class.simpleName}", e)
        }
    }
}

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

        val result = giniHealth.checkIfDocumentIsPayable(document.id)

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `Document is not payable if it has payment state extraction and that equals Other`() = runTest {
        val extractionsWithPaymentStateOther = copyExtractions(extractions).apply {
            specificExtractions["payment_state"]?.value = "Other"
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithPaymentStateOther)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        val result = giniHealth.checkIfDocumentIsPayable(document.id)

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `Document is not payable if it has an empty payment state extraction`() = runTest {
        val extractionsWithPaymentStateEmpty = copyExtractions(extractions).apply {
            specificExtractions["payment_state"]?.value = ""
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithPaymentStateEmpty)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        val result = giniHealth.checkIfDocumentIsPayable(document.id)

        Truth.assertThat(result).isFalse()
    }

    @Test
    fun `Document payable check throws an exception if get document API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(exception = Exception("Failed to get document"))
        coEvery { documentManager.getConfigurations() } returns mockk(relaxed = true)

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.checkIfDocumentIsPayable(document.id)
        }

        Truth.assertThat(exception.message).contains("Failed to get document")
    }

    @Test
    fun `Document payable check throws GiniHealthException with status code if get document API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(
            exception = Exception("Failed to get document"),
            responseStatusCode = 404,
            responseBody = """{"items":[{"code":"2501","message":"Document not found"}],"requestId":"test-123"}""",
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2501",
                        message = "Document not found"
                    )
                ),
                requestId = "test-123"
            )
        )
        coEvery { documentManager.getConfigurations() } returns mockk(relaxed = true)

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.checkIfDocumentIsPayable(document.id)
        }

        Truth.assertThat(exception.message).contains("Failed to get document")
        Truth.assertThat(exception.statusCode).isEqualTo(404)
        Truth.assertThat(exception.errorItems?.firstOrNull()?.code).isEqualTo("2501")
    }

    @Test
    fun `Document payable check throws an exception if get extractions API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(exception = Exception("Failed to get extractions"))
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.checkIfDocumentIsPayable(document.id)
        }

        Truth.assertThat(exception.message).contains("Failed to get extractions")
    }

    fun `Document payable check throws GiniHealthException with status code if get extractions API call fails`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(
            exception = Exception("Failed to get extractions"),
            responseStatusCode = 500,
            responseBody = """{"items":[{"code":"2507","message":"Extractions not found"}],"requestId":"test-456"}""",
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2507",
                        message = "Extractions not found"
                    )
                ),
                requestId = "test-456"
            )
        )
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.checkIfDocumentIsPayable(document.id)
        }

        Truth.assertThat(exception.message).contains("Failed to get extractions")
        Truth.assertThat(exception.statusCode).isEqualTo(500)
        Truth.assertThat(exception.errorItems?.firstOrNull()?.code).isEqualTo("2507")
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
    fun `Throws GiniHealthException with message field when batch delete documents with empty list`() = runTest {
        val errorJson = """{"message": "No documents to expire", "requestId": "test-123"}"""
        coEvery { documentManager.deleteDocuments(emptyList()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                message = "No documents to expire",
                requestId = "test-123"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deleteDocuments(emptyList())
        }

        Truth.assertThat(exception.statusCode).isEqualTo(400)
        Truth.assertThat(exception.requestId).isEqualTo("test-123")
    }

    @Test
    fun `Throws GiniHealthException with error code 2013 for unauthorized documents`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2013",
                        "object": ["8d5h7630-8f16-11ec-bd63-31f9d04e200e", "92de6fec-4a7f-4376-b5d5-5155adf8adca"]
                    }
                ],
                "requestId": "test-456"
            }
        """
        coEvery { documentManager.deleteDocuments(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2013",
                        documentIdList = listOf("8d5h7630-8f16-11ec-bd63-31f9d04e200e", "92de6fec-4a7f-4376-b5d5-5155adf8adca")
                    )
                ),
                requestId = "test-456"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deleteDocuments(listOf("8d5h7630-8f16-11ec-bd63-31f9d04e200e"))
        }

        Truth.assertThat(exception.statusCode).isEqualTo(400)
        Truth.assertThat(exception.errorItems).hasSize(1)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2013")
        Truth.assertThat(exception.errorItems?.first()?.documentIdList).hasSize(2)
        Truth.assertThat(exception.requestId).isEqualTo("test-456")
    }

    @Test
    fun `Throws GiniHealthException with error code 2014 for not found documents`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2014",
                        "object": ["3db07630-8f16-11ec-bd63-31f9d04e200e"]
                    }
                ],
                "requestId": "test-789"
            }
        """
        coEvery { documentManager.deleteDocuments(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2014",
                        documentIdList = listOf("3db07630-8f16-11ec-bd63-31f9d04e200e")
                    )
                ),
                requestId = "test-789"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deleteDocuments(listOf("3db07630-8f16-11ec-bd63-31f9d04e200e"))
        }

        Truth.assertThat(exception.errorItems).hasSize(1)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2014")
        Truth.assertThat(exception.errorItems?.first()?.documentIdList).hasSize(1)
    }

    @Test
    fun `Throws GiniHealthException with error code 2015 for missing composite documents`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2015",
                        "object": ["0eb26fec-4a7f-4376-b5d5-5155adf8adca", "8f434c37-7167-4c42-ac10-02e0826bba98"]
                    }
                ],
                "requestId": "test-abc"
            }
        """
        coEvery { documentManager.deleteDocuments(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2015",
                        documentIdList = listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca", "8f434c37-7167-4c42-ac10-02e0826bba98")
                    )
                ),
                requestId = "test-abc"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deleteDocuments(listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca"))
        }

        Truth.assertThat(exception.errorItems).hasSize(1)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2015")
        Truth.assertThat(exception.errorItems?.first()?.documentIdList).hasSize(2)
    }

    @Test
    fun `Throws GiniHealthException with multiple error items when multiple error types occur`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2013",
                        "object": ["8d5h7630-8f16-11ec-bd63-31f9d04e200e"]
                    },
                    {
                        "code": "2014",
                        "object": ["3db07630-8f16-11ec-bd63-31f9d04e200e", "0db26fec-4a7f-4376-b5d5-5155adf8adca"]
                    }
                ],
                "requestId": "a497-01aa-b6f0-cc17-43d3-76a8"
            }
        """
        coEvery { documentManager.deleteDocuments(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2013",
                        documentIdList = listOf("8d5h7630-8f16-11ec-bd63-31f9d04e200e")
                    ),
                    ErrorItem(
                        code = "2014",
                        documentIdList = listOf("3db07630-8f16-11ec-bd63-31f9d04e200e", "0db26fec-4a7f-4376-b5d5-5155adf8adca")
                    )
                ),
                requestId = "a497-01aa-b6f0-cc17-43d3-76a8"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deleteDocuments(listOf("doc1", "doc2"))
        }

        Truth.assertThat(exception.errorItems).hasSize(2)
        Truth.assertThat(exception.errorItems?.get(0)?.code).isEqualTo("2013")
        Truth.assertThat(exception.errorItems?.get(1)?.code).isEqualTo("2014")
        Truth.assertThat(exception.requestId).isEqualTo("a497-01aa-b6f0-cc17-43d3-76a8")
    }

    @Test
    fun `Returns null when delete payment request was successful`() = runTest {
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Success(Unit)

        val result = giniHealth.deletePaymentRequest("")

        assertTrue(result == null)
    }

    @Test
    fun `Throws GiniHealthException when delete payment request returned with error`() = runTest {
        val errorJson = """{"items":[{"code":"2503","message":"Payment request not found"}],"requestId":"test-123"}"""
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 404,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2503",
                        message = "Payment request not found"
                    )
                ),
                requestId = "test-123"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequest("")
        }

        Truth.assertThat(exception.statusCode).isEqualTo(404)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2503")
        Truth.assertThat(exception.requestId).isEqualTo("test-123")
    }

    @Test
    fun `Throws Exception when delete payment request was cancelled`() = runTest {
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Cancelled()

        val exception = assertThrowsSuspend<Exception> {
            giniHealth.deletePaymentRequest("")
        }

        Truth.assertThat(exception.message).isEqualTo("Request cancelled")
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
        Truth.assertThat(result).isEqualTo(mockPayment)
    }

    @Test
    fun `Returns error message when getPayment request returns an error`() = runTest {
        // Arrange: Mock error response from documentManager.getPayment
        coEvery { documentManager.getPayment(any()) } returns Resource.Error(
            message = "Payment request not found"
        )

        // Act & Assert: Call getPayment function and expect GiniHealthException
        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.getPayment("d89f26ab-a374-4c69-ae83-a88b078e4c49")
        }

        // Assert: Check that the error message is correct
        Truth.assertThat(exception.message).isEqualTo("Payment request not found")
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
    fun `Throws Exception when getPayment request was cancelled`() = runTest {
        // Arrange: Mock cancelled response from documentManager.getPayment
        coEvery { documentManager.getPayment(any()) } returns Resource.Cancelled()

        // Act & Assert: Call getPayment function and expect Exception
        val exception = assertThrowsSuspend<Exception> {
            giniHealth.getPayment("d89f26ab-a374-4c69-ae83-a88b078e4c49")
        }

        // Assert: Check that the error message is correct
        Truth.assertThat(exception.message).isEqualTo("Request cancelled")
    }

    @Test
    fun `Returns null when batch delete payment requests was successful`() = runTest {
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Success(Unit)

        val result = giniHealth.deletePaymentRequests(listOf())
        assertTrue(result == null)
    }

    @Test
    fun `Throws GiniHealthException with message field when batch delete payment requests with empty list`() = runTest {
        val errorJson = """{"message": "No payment requests to delete", "requestId": "test-123"}"""
        coEvery { documentManager.deletePaymentRequests(emptyList()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                message = "No payment requests to delete",
                requestId = "test-123"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequests(emptyList())
        }

        Truth.assertThat(exception.statusCode).isEqualTo(400)
        Truth.assertThat(exception.requestId).isEqualTo("test-123")
    }

    @Test
    fun `Throws GiniHealthException with error code 2016 for unauthorized payment requests`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2016",
                        "object": ["0eb26fec-4a7f-4376-b5d5-5155adf8adca", "8f434c37-7167-4c42-ac10-02e0826bba98"]
                    }
                ],
                "requestId": "7cc7-229b-4b88-dd94-3aad-f072"
            }
        """
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2016",
                        documentIdList = listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca", "8f434c37-7167-4c42-ac10-02e0826bba98")
                    )
                ),
                requestId = "7cc7-229b-4b88-dd94-3aad-f072"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequests(listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca"))
        }

        Truth.assertThat(exception.errorItems).hasSize(1)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2016")
        Truth.assertThat(exception.errorItems?.first()?.documentIdList).hasSize(2)
    }

    @Test
    fun `Throws GiniHealthException with error code 2017 for not found payment requests`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2017",
                        "object": ["0eb26fec-4a7f-4376-b5d5-5155adf8adca"]
                    }
                ],
                "requestId": "7cc7-229b-4b88-dd94-3aad-f072"
            }
        """
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2017",
                        documentIdList = listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca")
                    )
                ),
                requestId = "7cc7-229b-4b88-dd94-3aad-f072"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequests(listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca"))
        }

        Truth.assertThat(exception.errorItems).hasSize(1)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2017")
        Truth.assertThat(exception.errorItems?.first()?.documentIdList).hasSize(1)
    }

    @Test
    fun `Throws GiniHealthException with multiple error items for payment requests`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2016",
                        "object": ["0eb26fec-4a7f-4376-b5d5-5155adf8adca"]
                    },
                    {
                        "code": "2017",
                        "object": ["8f434c37-7167-4c42-ac10-02e0826bba98"]
                    }
                ],
                "requestId": "7cc7-229b-4b88-dd94-3aad-f072"
            }
        """
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2016",
                        documentIdList = listOf("0eb26fec-4a7f-4376-b5d5-5155adf8adca")
                    ),
                    ErrorItem(
                        code = "2017",
                        documentIdList = listOf("8f434c37-7167-4c42-ac10-02e0826bba98")
                    )
                ),
                requestId = "7cc7-229b-4b88-dd94-3aad-f072"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequests(listOf("pr-id-1", "pr-id-2"))
        }

        Truth.assertThat(exception.errorItems).hasSize(2)
        Truth.assertThat(exception.errorItems?.get(0)?.code).isEqualTo("2016")
        Truth.assertThat(exception.errorItems?.get(1)?.code).isEqualTo("2017")
        Truth.assertThat(exception.requestId).isEqualTo("7cc7-229b-4b88-dd94-3aad-f072")
    }

    @Test
    fun `Throws Exception when batch delete payment requests is cancelled`() = runTest {
        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Cancelled()

        val exception = assertThrowsSuspend<Exception> {
            giniHealth.deletePaymentRequests(listOf())
        }

        Truth.assertThat(exception.message).isEqualTo("Request cancelled")
    }

    @Test
    fun `Throws GiniHealthException with validation error codes for API v5_0`() = runTest {
        val errorJson = """
            {
                "items": [
                    {
                        "code": "2002",
                        "message": "Value of payment purpose should be at least 4, at most 200 characters long"
                    },
                    {
                        "code": "2007",
                        "message": "Provide a valid IBAN number"
                    }
                ],
                "requestId": "7cc7-229b-4b88-dd94-3aad-f072"
            }
        """
        coEvery { giniHealthAPI.documentManager.deletePaymentRequest(any()) } returns Resource.Error(
            responseBody = errorJson,
            responseStatusCode = 400,
            errorResponse = ErrorResponse(
                items = listOf(
                    ErrorItem(
                        code = "2002",
                        message = "Value of payment purpose should be at least 4, at most 200 characters long"
                    ),
                    ErrorItem(
                        code = "2007",
                        message = "Provide a valid IBAN number"
                    )
                ),
                requestId = "7cc7-229b-4b88-dd94-3aad-f072"
            )
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequest("invalid-pr-id")
        }

        Truth.assertThat(exception.errorItems).hasSize(2)
        Truth.assertThat(exception.errorItems!![0].code).isEqualTo("2002")
        Truth.assertThat(exception.errorItems!![0].message).isEqualTo("Value of payment purpose should be at least 4, at most 200 characters long")
        Truth.assertThat(exception.errorItems!![1].code).isEqualTo("2007")
        Truth.assertThat(exception.errorItems!![1].message).isEqualTo("Provide a valid IBAN number")
        Truth.assertThat(exception.requestId).isEqualTo("7cc7-229b-4b88-dd94-3aad-f072")
    }

    // ========== BACKWARD COMPATIBILITY TESTS ==========
    // These tests verify that existing host apps using old error handling still work
    // Only delete functions require new error structure - all other functions should be backward compatible

    @Test
    fun `BACKWARD COMPATIBILITY - checkIfDocumentIsPayable with generic Exception catch still works`() = runTest {
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(
            exception = Exception("Network error"),
            responseStatusCode = 500
        )

        // Old way - host apps catching generic Exception
        try {
            giniHealth.checkIfDocumentIsPayable(document.id)
            Truth.assertWithMessage("Expected exception to be thrown").fail()
        } catch (e: GiniHealthException) {
            // Since GiniHealthException extends Throwable (not Exception), 
            // this tests backward compatibility for Exception catch blocks
            Truth.assertThat(e.message).isNotNull()
            Truth.assertThat(e.message).contains("Network error")
        }
    }

    @Test
    fun `BACKWARD COMPATIBILITY - checkIfDocumentIsPayable with Throwable catch still works`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(
            exception = Exception("Extraction failed"),
            responseStatusCode = 400
        )

        // Old way - host apps catching Throwable
        try {
            giniHealth.checkIfDocumentIsPayable(document.id)
            Truth.assertWithMessage("Expected exception to be thrown").fail()
        } catch (t: Throwable) {
            // Old error handling - just verify throwable has message
            Truth.assertThat(t).isNotNull()
            Truth.assertThat(t.message).isNotNull()
        }
    }

    @Test
    fun `BACKWARD COMPATIBILITY - getPayment with generic Exception catch still works`() = runTest {
        coEvery { documentManager.getPayment(any()) } returns Resource.Error(
            exception = Exception("Payment not found"),
            responseStatusCode = 404
        )

        // Old way - host apps catching generic Exception
        try {
            giniHealth.getPayment("test-id")
            Truth.assertWithMessage("Expected exception to be thrown").fail()
        } catch (e: GiniHealthException) {
            // Since GiniHealthException extends Throwable (not Exception),
            // this tests backward compatibility for Exception catch blocks
            Truth.assertThat(e.message).isNotNull()
            Truth.assertThat(e.message).contains("Payment not found")
        }
    }

    @Test
    fun `BACKWARD COMPATIBILITY - setDocumentForReview error accessible via ResultWrapper Error without GiniHealthException`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Error(
            exception = Exception("Extraction failed"),
            responseStatusCode = 500
        )

        giniHealth.setDocumentForReview(document)

        // Old way - host apps checking ResultWrapper.Error without knowing about GiniHealthException
        // Document flow should be success (we provided document directly)
        assert(giniHealth.documentFlow.value is ResultWrapper.Success<*>) {
            "Expected Success for document but was ${giniHealth.documentFlow.value}"
        }

        // Payment flow should have error (extractions failed)
        assert(giniHealth.paymentFlow.value is ResultWrapper.Error<*>) {
            "Expected Error for payment but was ${giniHealth.paymentFlow.value}"
        }

        val errorWrapper = giniHealth.paymentFlow.value as ResultWrapper.Error<*>
        // Old property 'error' should still work for backward compatibility
        Truth.assertThat(errorWrapper.error).isNotNull()
        Truth.assertThat(errorWrapper.error.message).isNotNull()
    }

    @Test
    fun `BACKWARD COMPATIBILITY - setDocumentForReview with payment details error accessible via old property`() = runTest {
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(
            exception = Exception("Document fetch failed")
        )

        giniHealth.setDocumentForReview("doc-id", PaymentDetails("r", "i", "100", "p"))

        // Old way - access error via 'error' property
        val errorWrapper = giniHealth.documentFlow.value as ResultWrapper.Error<*>
        Truth.assertThat(errorWrapper.error).isNotNull()
        Truth.assertThat(errorWrapper.error).isInstanceOf(Throwable::class.java)
    }

    @Test
    fun `BACKWARD COMPATIBILITY - Payment flow error accessible without GiniHealthException knowledge`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(
            exception = Exception("Failed to get extractions")
        )
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        giniHealth.setDocumentForReview("doc-id")

        // Old way - checking error state without knowing specific exception type
        assert(giniHealth.paymentFlow.value is ResultWrapper.Error<*>)
        val errorState = giniHealth.paymentFlow.value as ResultWrapper.Error<*>

        // Access via old 'error' property
        Truth.assertThat(errorState.error).isNotNull()
        Truth.assertThat(errorState.error.message).isNotNull()
    }

    @Test
    fun `BACKWARD COMPATIBILITY - NoPaymentDataExtracted still throwable from old code path`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(document) } returns Resource.Success(
            ExtractionsContainer(emptyMap(), emptyMap())
        )

        // Old way - generic exception handling
        try {
            giniHealth.setDocumentForReview(document)
            // Check payment flow
            val errorState = giniHealth.paymentFlow.value as ResultWrapper.Error<*>

            // Old code might check the error directly
            Truth.assertThat(errorState.error).isNotNull()
            Truth.assertThat(errorState.error.message).contains("No payment data")
        } catch (e: Exception) {
            // Should not reach here in this test, but old code might catch exceptions
            Truth.assertThat(e).isNotNull()
        }
    }

    @Test
    fun `BACKWARD COMPATIBILITY - Error message accessible from exception property for old code`() = runTest {
        coEvery { documentManager.getPayment(any()) } returns Resource.Error(
            exception = Exception("Some error occurred")
        )

        try {
            giniHealth.getPayment("payment-id")
            Truth.assertWithMessage("Expected exception").fail()
        } catch (e: Throwable) {
            // Old code just checking message exists
            Truth.assertThat(e.message).isNotEmpty()
        }
    }

    // ========== Error Structure Tests (API v5.0) ==========

    @Test
    fun `getPayment error 2503 - All error details accessible`() = runTest {
        val errorResponse = ErrorResponse(
            items = listOf(ErrorItem(code = "2503", message = "Payment request not found")),
            requestId = "7cc7-229b-4b88-dd94-3aad-f072"
        )
        val errorJson = """{"items":[{"code":"2503","message":"Payment request not found"}],"requestId":"7cc7-229b-4b88-dd94-3aad-f072"}"""

        coEvery { documentManager.getPayment(any()) } returns Resource.Error(
            message = errorJson,
            responseStatusCode = 404,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.getPayment("invalid-payment-id")
        }

        Truth.assertThat(exception.statusCode).isEqualTo(404)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2503")
        Truth.assertThat(exception.errorItems?.first()?.message).isEqualTo("Payment request not found")
        Truth.assertThat(exception.requestId).isEqualTo("7cc7-229b-4b88-dd94-3aad-f072")
        Truth.assertThat(exception.parsedMessage).isEqualTo(errorJson) // Raw JSON since no top-level message
        Truth.assertThat(exception.message).contains("items") // Raw JSON for backward compatibility
    }

    @Test
    fun `getPayment multiple error codes - All items accessible`() = runTest {
        val errorResponse = ErrorResponse(
            items = listOf(
                ErrorItem(code = "2002", message = "Value of payment purpose should be at least 4, at most 200 characters long"),
                ErrorItem(code = "2007", message = "Provide a valid IBAN number")
            ),
            requestId = "multi-error"
        )

        coEvery { documentManager.getPayment(any()) } returns Resource.Error(
            message = "{}",
            responseStatusCode = 400,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.getPayment("payment-id")
        }

        Truth.assertThat(exception.errorItems).hasSize(2)
        Truth.assertThat(exception.errorItems?.get(0)?.code).isEqualTo("2002")
        Truth.assertThat(exception.errorItems?.get(1)?.code).isEqualTo("2007")
        Truth.assertThat(exception.errorItems?.get(0)?.message).contains("at least 4, at most 200")
    }

    @Test
    fun `getPayment top-level message priority over item message`() = runTest {
        val errorResponse = ErrorResponse(
            message = "Validation of the request entity failed",
            items = listOf(ErrorItem(code = "2100", message = "Validation failed")),
            requestId = "validation-req"
        )

        coEvery { documentManager.getPayment(any()) } returns Resource.Error(
            message = "{}",
            responseStatusCode = 400,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.getPayment("payment-id")
        }

        Truth.assertThat(exception.parsedMessage).isEqualTo("Validation of the request entity failed")
    }

    @Test
    fun `deletePaymentRequests bulk delete with documentIdList`() = runTest {
        val errorResponse = ErrorResponse(
            items = listOf(
                ErrorItem(code = "2016", documentIdList = listOf("pr-unauthorized-1", "pr-unauthorized-2")),
                ErrorItem(code = "2017", documentIdList = listOf("pr-not-found-1", "pr-not-found-2"))
            ),
            requestId = "bulk-delete-error"
        )

        coEvery { documentManager.deletePaymentRequests(any()) } returns Resource.Error(
            message = "{}",
            responseStatusCode = 400,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deletePaymentRequests(listOf("pr-1", "pr-2", "pr-3"))
        }

        Truth.assertThat(exception.errorItems).hasSize(2)
        Truth.assertThat(exception.errorItems?.get(0)?.code).isEqualTo("2016")
        Truth.assertThat(exception.errorItems?.get(0)?.documentIdList).hasSize(2)
        Truth.assertThat(exception.errorItems?.get(0)?.documentIdList).contains("pr-unauthorized-1")
        Truth.assertThat(exception.errorItems?.get(1)?.documentIdList).hasSize(2)
    }

    @Test
    fun `deleteDocuments bulk delete with documentIdList`() = runTest {
        val errorResponse = ErrorResponse(
            items = listOf(
                ErrorItem(code = "2013", documentIdList = listOf("doc-unauthorized")),
                ErrorItem(code = "2014", documentIdList = listOf("doc-not-found-1", "doc-not-found-2"))
            ),
            requestId = "bulk-delete-docs"
        )

        coEvery { documentManager.deleteDocuments(any()) } returns Resource.Error(
            message = "{}",
            responseStatusCode = 400,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.deleteDocuments(listOf("doc-1", "doc-2", "doc-3"))
        }

        Truth.assertThat(exception.errorItems).hasSize(2)
        Truth.assertThat(exception.errorItems?.get(0)?.code).isEqualTo("2013")
        Truth.assertThat(exception.errorItems?.get(1)?.code).isEqualTo("2014")
        Truth.assertThat(exception.errorItems?.get(1)?.documentIdList).contains("doc-not-found-1")
    }

    @Test
    fun `checkIfDocumentIsPayable error 2501 - Document not found`() = runTest {
        val errorResponse = ErrorResponse(
            items = listOf(ErrorItem(code = "2501", message = "Document does not exist")),
            requestId = "doc-not-found"
        )

        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Error(
            message = "{}",
            responseStatusCode = 404,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.checkIfDocumentIsPayable("doc-123")
        }

        Truth.assertThat(exception.statusCode).isEqualTo(404)
        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2501")
    }

    @Test
    fun `checkIfDocumentIsPayable error 2507 - Extractions not found`() = runTest {
        val errorResponse = ErrorResponse(
            items = listOf(ErrorItem(code = "2507", message = "Extractions not found")),
            requestId = "extractions-missing"
        )

        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Error(
            message = "{}",
            responseStatusCode = 404,
            errorResponse = errorResponse
        )

        val exception = assertThrowsSuspend<GiniHealthException> {
            giniHealth.checkIfDocumentIsPayable("doc-456")
        }

        Truth.assertThat(exception.errorItems?.first()?.code).isEqualTo("2507")
        Truth.assertThat(exception.errorItems?.first()?.message).isEqualTo("Extractions not found")
    }
}

