package net.gini.android.health.api;

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.api.models.PaymentRequestInput
import org.json.JSONException

/**
 * Created by Alpár Szotyori on 25.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
class HealthApiDocumentManager(private val documentRepository: HealthApiDocumentRepository) : DocumentManager<HealthApiDocumentRepository, ExtractionsContainer>(
    documentRepository
) {

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in the Gini API documentation.
     *
     * @param document            The document for which the extractions should be updated.
     * @param specificExtractions A Map where the key is the name of the specific extraction and the value is the
     *                            SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @param compoundExtractions A Map where the key is the name of the compound extraction and the value is the
     *                            CompoundExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @return Resource with the success API response
     * @throws JSONException When a value of an extraction is not JSON serializable.
     */
    suspend fun sendFeedback(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions, compoundExtractions)

    suspend fun sendFeedback(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions)

    /**
     * Get the rendered image of a page as byte[]
     *
     * @param documentId id of document
     * @param page page of document
     */
    suspend fun getPageImage(
        documentId: String,
        page: Int
    ): Resource<ByteArray> = documentRepository.getPageImage(documentId, page)

    /**
     * A payment provider is a Gini partner which integrated the GiniPay for Banks SDK into their mobile apps.
     *
     * @return A list of [PaymentProvider]
     */
    suspend fun getPaymentProviders(): Resource<List<PaymentProvider>> =
        documentRepository.getPaymentProviders()

    /**
     * @return [PaymentProvider] for the given id.
     */
    suspend fun getPaymentProvider(
            id: String,
            ): Resource<PaymentProvider> = documentRepository.getPaymentProvider(id)

    /**
     *  A [PaymentRequest] is used to have on the backend the intent of making a payment
     *  for a document with its (modified) extractions and specific payment provider.
     *
     *  @return Id of the [PaymentRequest]
     */
    suspend fun createPaymentRequest(
        paymentRequestInput: PaymentRequestInput,
            ): Resource<String> = documentRepository.createPaymentRequest(paymentRequestInput)

}
