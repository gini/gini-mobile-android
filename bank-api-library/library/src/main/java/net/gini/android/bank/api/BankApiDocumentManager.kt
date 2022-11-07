package net.gini.android.bank.api;

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.gini.android.bank.api.models.ExtractionsContainer
import net.gini.android.bank.api.models.Payment
import net.gini.android.bank.api.models.ResolvePaymentInput
import net.gini.android.bank.api.models.ResolvedPayment
import net.gini.android.bank.api.requests.ErrorEvent
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import okhttp3.ResponseBody
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
     * @return Empty resource or information about the error
     */
    suspend fun sendFeedbackForExtractions(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions, compoundExtractions)

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in the Gini Bank API documentation.
     *
     * @param document            The document for which the extractions should be updated.
     * @param specificExtractions A Map where the key is the name of the specific extraction and the value is the
     *                            SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @return Empty resource or information about the error
     */
    suspend fun sendFeedbackForExtractions(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions)

    /**
     * Mark a [PaymentRequest] as paid.
     *
     * @param requestId id of request
     * @param resolvePaymentInput information of the actual payment
     */
    suspend fun resolvePaymentRequest(
        requestId: String,
        resolvePaymentInput: ResolvePaymentInput,
    ): Resource<ResolvedPayment> =
        documentRepository.resolvePaymentRequest(requestId, resolvePaymentInput)

    /**
     * Get information about the payment of the [PaymentRequest]
     *
     * @param id of the paid [PaymentRequest]
     */
    suspend fun getPayment(
        id: String,
    ): Resource<Payment> =
        documentRepository.getPayment(id)

    suspend fun logErrorEvent(
        errorEvent: ErrorEvent
    ): Resource<Unit> =
        documentRepository.logErrorEvent(errorEvent)
}
