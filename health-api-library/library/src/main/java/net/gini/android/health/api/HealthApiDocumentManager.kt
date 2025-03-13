package net.gini.android.health.api;

import net.gini.android.core.api.DocumentManager
import net.gini.android.core.api.Resource
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.health.api.models.PaymentProvider
import net.gini.android.health.api.models.PaymentRequestInput
import net.gini.android.health.api.response.ConfigurationResponse

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
     * Delete multiple documents in one go.
     *
     * @param documentIds the list of document ids to be deleted
     * @return [Resource] with the status of the deletion, success or failure - if at least one document could not
     * be deleted, the call will not return Success
     */
    suspend fun deleteDocuments(
        documentIds: List<String>
    ): Resource<Unit> =
        documentRepository.deleteDocuments(documentIds)

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


    /**
     * Returns a QR code in PDF format which can be shared to payment providers
     *
     * @param paymentRequestId the generated payment request id for which the QR code should be generated
     * @return [Resource] with the byte array corresponding to the [PaymentRequest]
     */
    suspend fun getPaymentRequestDocument(
        paymentRequestId: String
    ): Resource<ByteArray> = documentRepository.getPaymentRequestDocument(paymentRequestId)


    /**
     * This function is used to delete payment requests.
     *
     * @param paymentRequestId The unique identifier of the payment request to be deleted.
     * @return [Resource] with the byte array corresponding to the deleted [PaymentRequest]
     */
    suspend fun deletePaymentRequest(
        paymentRequestId: String
    ): Resource<ByteArray> = documentRepository.deletePaymentRequest(paymentRequestId)

    /**
     * Returns a QR code in PNG format
     *
     * @param paymentRequestId the generated payment request id for which the QR code should be generated
     * @return [Resource] with the byte array corresponding to the [PaymentRequest]
     */
    suspend fun getPaymentRequestImage(
        paymentRequestId: String
    ): Resource<ByteArray> = documentRepository.getPaymentRequestImage(paymentRequestId)

    /**
     * Returns the configurations for the client.
     *
     * @return [Resource] with the [ConfigurationResponse] for the client.
     */
    suspend fun getConfigurations(): Resource<ConfigurationResponse> = documentRepository.getConfigurations()
}
