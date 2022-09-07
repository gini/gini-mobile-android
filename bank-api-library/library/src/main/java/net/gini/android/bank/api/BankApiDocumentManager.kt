package net.gini.android.bank.api;

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.Payment
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONException

/**
 * Created by Alpár Szotyori on 25.01.22.
 * <p>
 * Copyright (c) 2022 Gini GmbH.
 */
class BankApiDocumentManager(private val documentRepository: BankApiDocumentRepository) : DocumentManager<BankApiDocumentRepository, ExtractionsContainer>(
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
     * @return The same document instance when storing the updated
     * extractions was successful.
     * @throws JSONException When a value of an extraction is not JSON serializable.
     */
    suspend fun sendFeedback(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
    ): Resource<Document> =
        documentRepository.
//    withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.sendFeedbackForExtractions(document, specificExtractions, compoundExtractions)
//            continuation.resumeTask(task)
//        }
//    }

    /**
     * Mark a [PaymentRequest] as paid.
     *
     * @param requestId id of request
     * @param resolvePaymentInput information of the actual payment
     */
    suspend fun resolvePaymentRequest(
        requestId: String,
        resolvePaymentInput: ResolvePaymentInput,
    ): ResolvedPayment = withContext(taskDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val task = documentTaskManager.resolvePaymentRequest(requestId, resolvePaymentInput)
            continuation.resumeTask(task)
        }
    }

    /**
     * Get information about the payment of the [PaymentRequest]
     *
     * @param id of the paid [PaymentRequest]
     */
    suspend fun getPayment(
        id: String,
    ): Payment = withContext(taskDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val task = documentTaskManager.getPayment(id)
            continuation.resumeTask(task)
        }
    }

}
