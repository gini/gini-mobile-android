package net.gini.pay.ginipaybusiness

import android.net.Uri
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import net.gini.android.DocumentManager
import net.gini.android.Gini
import net.gini.android.models.Document
import net.gini.android.models.ExtractionsContainer
import net.gini.android.models.SpecificExtraction
import net.gini.pay.ginipaybusiness.review.model.PaymentDetails
import net.gini.pay.ginipaybusiness.review.model.ResultWrapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

val document =
    Document("1234", Document.ProcessingState.COMPLETED, "", 1, Date(124), Document.SourceClassification.COMPOSITE, Uri.EMPTY, emptyList(), emptyList())

val extractions = ExtractionsContainer(
    mapOf(
        "paymentRecipient" to SpecificExtraction("paymentRecipient", "recipient", "", null, listOf()),
        "iban" to SpecificExtraction("iban", "iban", "", null, listOf()),
        "amountToPay" to SpecificExtraction("amountToPay", "123.56", "", null, listOf()),
        "paymentPurpose" to SpecificExtraction("paymentPurpose", "purpose", "", null, listOf()),
    ),
    mapOf(), emptyList()
)

fun copyExtractions(extractions: ExtractionsContainer) = ExtractionsContainer(
    extractions.specificExtractions.toMap(),
    extractions.compoundExtractions.toMap(),
    extractions.returnReasons.toList()
)

@ExperimentalCoroutinesApi
class GiniBusinessTest {

    private lateinit var giniBusiness: GiniBusiness
    private val giniApi: Gini = mockk(relaxed = true) { Gini::class.java }
    private val documentManager: DocumentManager = mockk { DocumentManager::class.java }

    @Before
    fun setUp() {
        every { giniApi.documentManager } returns documentManager
        giniBusiness = GiniBusiness(giniApi)
    }

    @Test
    fun `When setting document for review then document and payment flow emit success`() = runBlockingTest {
        coEvery { documentManager.getExtractions(document) } returns extractions
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)

        assert(giniBusiness.documentFlow.value is ResultWrapper.Loading<Document>) { "Expected Loading but was ${giniBusiness.documentFlow.value}" }
        assert(giniBusiness.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading but was ${giniBusiness.paymentFlow.value}" }
        giniBusiness.setDocumentForReview(document)
        assert(giniBusiness.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success but was ${giniBusiness.documentFlow.value}" }
        assert(giniBusiness.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success but was ${giniBusiness.documentFlow.value}" }
        assert(giniBusiness.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success but was ${giniBusiness.paymentFlow.value}" }
        assertEquals(document, (giniBusiness.documentFlow.value as ResultWrapper.Success<Document>).value)
        assertEquals(paymentDetails, (giniBusiness.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document id for review with payment details then document flow emits document`() = runBlockingTest {
        coEvery { documentManager.getDocument(any<String>()) } returns document
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose")

        assert(giniBusiness.documentFlow.value is ResultWrapper.Loading<Document>) { "Expected Loading" }
        giniBusiness.setDocumentForReview("1234", paymentDetails)
        assert(giniBusiness.documentFlow.value is ResultWrapper.Success<Document>) { "Expected Success" }
        assertEquals(document, (giniBusiness.documentFlow.value as ResultWrapper.Success<Document>).value)
    }

    @Test
    fun `When setting document id for review with payment details then payment flow emits those details`() = runBlockingTest {
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose")

        assert(giniBusiness.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniBusiness.setDocumentForReview("", paymentDetails)
        assert(giniBusiness.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        assertEquals(paymentDetails, (giniBusiness.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `When setting document id for review without payment details then payment flow emits details`() = runBlockingTest {
        coEvery { documentManager.getExtractions(any()) } returns extractions
        coEvery { documentManager.getDocument(any<String>()) } returns document
        val paymentDetails = PaymentDetails("recipient", "iban", "123.56", "purpose", extractions)

        assert(giniBusiness.paymentFlow.value is ResultWrapper.Loading<PaymentDetails>) { "Expected Loading" }
        giniBusiness.setDocumentForReview("")
        assert(giniBusiness.paymentFlow.value is ResultWrapper.Success<PaymentDetails>) { "Expected Success" }
        assertEquals(paymentDetails, (giniBusiness.paymentFlow.value as ResultWrapper.Success<PaymentDetails>).value)
    }

    @Test
    fun `Document is payable if it has an IBAN extraction`() = runBlockingTest {
        coEvery { documentManager.getExtractions(any()) } returns extractions
        coEvery { documentManager.getDocument(any<String>()) } returns document

        assertTrue(giniBusiness.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has no IBAN extraction`() = runBlockingTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            specificExtractions.remove("iban")
        }
        coEvery { documentManager.getExtractions(any()) } returns extractionsWithoutIBAN
        coEvery { documentManager.getDocument(any<String>()) } returns document

        assertFalse(giniBusiness.checkIfDocumentIsPayable(document.id))
    }

    @Test
    fun `Document is not payable if it has an empty IBAN extraction`() = runBlockingTest {
        val extractionsWithoutIBAN = copyExtractions(extractions).apply {
            specificExtractions.set("iban", SpecificExtraction("iban", "", "", null, listOf()))
        }
        coEvery { documentManager.getExtractions(any()) } returns extractionsWithoutIBAN
        coEvery { documentManager.getDocument(any<String>()) } returns document

        assertFalse(giniBusiness.checkIfDocumentIsPayable(document.id))
    }
}