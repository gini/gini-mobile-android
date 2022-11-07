package net.gini.android.health.sdk

import android.net.Uri
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.GiniHealthAPI
import net.gini.android.health.api.HealthApiDocumentManager
import net.gini.android.health.sdk.review.error.NoPaymentDataExtracted
import net.gini.android.health.sdk.review.model.PaymentDetails
import net.gini.android.health.sdk.review.model.ResultWrapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

val document =
    Document("1234", Document.ProcessingState.COMPLETED, "", 1, Date(124), Document.SourceClassification.COMPOSITE, Uri.EMPTY, emptyList(), emptyList())

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

@ExperimentalCoroutinesApi
class GiniHealthTest {

    private lateinit var giniHealth: GiniHealth
    private val giniHealthAPI: GiniHealthAPI = mockk(relaxed = true) { GiniHealthAPI::class.java }
    private val documentManager: HealthApiDocumentManager = mockk { HealthApiDocumentManager::class.java }

    @Before
    fun setUp() {
        every { giniHealthAPI.documentManager } returns documentManager
        giniHealth = GiniHealth(giniHealthAPI)
    }

    @Test
    fun `When setting document for review then document and payment flow emit success`() = runTest {
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
    fun `Document is payable if it has an IBAN extraction`() = runTest {
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractions)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertTrue(giniHealth.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has no IBAN extraction`() = runTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            compoundExtractions["payment"]?.specificExtractionMaps?.get(0)?.remove("iban")
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithoutIBAN)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniHealth.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has an empty IBAN extraction`() = runTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            compoundExtractions["payment"]?.specificExtractionMaps?.get(0)
                ?.set("iban", SpecificExtraction("iban", "", "", null, listOf()))
        }
        coEvery { documentManager.getAllExtractionsWithPolling(any()) } returns Resource.Success(extractionsWithoutIBAN)
        coEvery { documentManager.getDocument(any<String>()) } returns Resource.Success(document)

        assertFalse(giniHealth.checkIfDocumentIsPayable(document.id))
    }
}