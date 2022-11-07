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

/**
 * The [HealthApiDocumentManager] is a high level API on top of the Gini Health API. It
 * provides high level methods to handle document and payment request related tasks easily.
 */
class HealthApiDocumentManager(private val documentRepository: HealthApiDocumentRepository) : DocumentManager<HealthApiDocumentRepository, ExtractionsContainer>(
    documentRepository
) {

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in the Gini Health API documentation.
     *
     * @param document            The document for which the extractions should be updated.
     * @param specificExtractions A Map where the key is the name of the specific extraction and the value is the
     *                            SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @param compoundExtractions A Map where the key is the name of the compound extraction and the value is the
     *                            CompoundExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @return Empty [Resource] or information about the error
     */
    suspend fun sendFeedback(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions, compoundExtractions)

    /**
     * Get the rendered image of a page as byte[]
     *
     * @param documentId id of document
     * @param page page of document
     * @return [Resource] with the image's bytes or information about the error
     */
    suspend fun getPageImage(
        documentId: String,
        page: Int
    ): Resource<ByteArray> = documentRepository.getPageImage(documentId, page)

    /**
     * A payment provider is a Gini partner which integrated the GiniPay for Banks SDK into their mobile apps.
     *
     * @return [Resource] with a list of [PaymentProvider] instances or information about the error
     */
    suspend fun getPaymentProviders(): Resource<List<PaymentProvider>> =
        documentRepository.getPaymentProviders()

    /**
     * @return [Resource] with the [PaymentProvider] instance for the given id or information about the error
     */
    suspend fun getPaymentProvider(
            id: String,
            ): Resource<PaymentProvider> = documentRepository.getPaymentProvider(id)

    /**
     *  A [PaymentRequest] is used to signal to the API the intent of executing a payment
     *  using a specific payment provider for a document with its (possibly modified) extractions.
     *
     *  @return [Resource] with the id string of the [PaymentRequest] or information about the error
     */
    suspend fun createPaymentRequest(
        paymentRequestInput: PaymentRequestInput,
            ): Resource<String> = documentRepository.createPaymentRequest(paymentRequestInput)

}
