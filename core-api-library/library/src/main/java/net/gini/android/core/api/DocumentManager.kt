package net.gini.android.core.api

import android.net.Uri
import net.gini.android.core.api.models.CompoundExtraction
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequest
import net.gini.android.core.api.models.SpecificExtraction
import org.json.JSONObject

/**
 * The [DocumentManager] is a high level API on top of the Gini API, via which the [DocumentRepository] is used. It
 * provides high level methods to handle document related tasks easily.
 */
abstract class DocumentManager<out DR: DocumentRepository<E>, E: ExtractionsContainer>(private val documentRepository: DR) {

    /**
     * Uploads raw data and creates a new Gini partial document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param contentType  The media type of the uploaded data
     * @param filename     Optional the filename of the given document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return [Resource] with the [Document] instance of the freshly created document or information about the error
     */
    suspend fun createPartialDocument(
        document: ByteArray,
        contentType: String,
        filename: String? = null,
        documentType: DocumentType? = null,
        documentMetadata: DocumentMetadata? = null,
    ): Resource<Document> =
        documentRepository.createPartialDocument(document, contentType, filename, documentType, documentMetadata)

    /**
     * Deletes a Gini partial document and all its parent composite documents.
     *
     * Partial documents can be deleted only, if they don't belong to any composite documents and
     * this method deletes the parents before deleting the partial document.
     *
     * @param documentId The id of an existing partial document
     * @return Empty [Resource] or information about the error
     */

    suspend fun deletePartialDocumentAndParents(documentId: String): Resource<Unit> =
        documentRepository.deletePartialDocumentAndParents(documentId)

    /**
     * Deletes a Gini document.
     *
     * For deleting partial documents use [deletePartialDocumentAndParents] instead.
     *
     * @param documentId The id of an existing document
     * @return Empty [Resource] or information about the error
     */
    suspend fun deleteDocument(documentId: String): Resource<Unit> =
        documentRepository.deleteDocument(documentId)

    /**
     * Creates a new Gini composite document.
     *
     * @param documents    A list of partial documents which should be part of a multi-page document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return [Resource] with the [Document] instance or information about the error
     */
    suspend fun createCompositeDocument(
        documents: List<Document>,
        documentType: DocumentType? = null
    ): Resource<Document> =
        documentRepository.createCompositeDocument(documents, documentType)

    /**
     * Creates a new Gini composite document. The input Map must contain the partial documents as keys. These will be
     * part of the multi-page document. The value for each partial document key is the amount in degrees the document
     * has been rotated by the user.
     *
     * @param documentRotationMap A map of partial documents and their rotation in degrees
     * @param documentType        Optional a document type hint. See the documentation for the document type hints for
     *                            possible values
     * @return [Resource] with the [Document] instance or information about the error
     */
    suspend fun createCompositeDocument(
        documentRotationMap: LinkedHashMap<Document, Int>,
        documentType: DocumentType? = null
    ): Resource<Document> =
        documentRepository.createCompositeDocument(documentRotationMap, documentType)

    /**
     * Get the document with the given unique identifier.
     *
     * @param id The unique identifier of the document.
     * @return [Resource] with the [Document] instance representing all the document's metadata or information about the error
     */
    suspend fun getDocument(
        id: String,
    ): Resource<Document> =
        documentRepository.getDocument(id)

    /**
     * Get the document with the given unique identifier.
     *
     * Please note that this method may use a slightly corrected URI from which it gets the document (e.g. if the
     * URI's host does not conform to the base URL of the Gini API). Therefore it is not possibly to use this method to
     * get a document from an arbitrary URI.
     *
     * @param uri The URI of the document.
     * @return [Resource] with the [Document] instance representing all the document's metadata or information about the error
     */
    suspend fun getDocument(
        uri: Uri,
    ): Resource<Document> =
        documentRepository.getDocument(uri)

    /**
     * Continually checks the document status (via the Gini API) until the document is fully processed. To avoid
     * flooding the network, there is a pause of at least [DocumentRepository.POLLING_INTERVAL]
     * and a timeout of [DocumentRepository.POLLING_TIMEOUT].
     *
     * @param document The document which will be polled.
     * @return [Resource] with the [Document] instance representing all the document's metadata or information about the error
     */
    suspend fun pollDocument(
        document: Document,
    ): Resource<Document> =
        documentRepository.pollDocument(document)

    /**
     * Gets the layout of a document. The layout of the document describes the textual content of a document with
     * positional information, based on the processed document.
     *
     * @param document The document for which the layouts is requested.
     * @return [Resource] with a [JSONObject] instance containing the layout or information about the error
     */
    suspend fun getLayout(
        document: Document
    ): Resource<JSONObject> =
        documentRepository.getLayout(document)

    /**
     * Get all extractions (specific and compound) for the given document.
     *
     * @param document The [Document] instance for whose document the extractions are returned.
     * @return [Resource] with an [ExtractionsContainer] instance or information about the error
     */
    suspend fun getAllExtractions(
        document: Document
    ): Resource<E> =
        documentRepository.getAllExtractions(document)

    /**
     * Poll the document and get all extractions (specific and compound) once processing has completed.
     *
     * @param document The [Document] instance for whose document the extractions are returned.
     * @return [Resource] with an [ExtractionsContainer] instance or information about the error
     */
    suspend fun getAllExtractionsWithPolling(
        document: Document
    ): Resource<E> = when (val pollDocument = documentRepository.pollDocument(document)) {
        is Resource.Cancelled -> Resource.Cancelled()
        is Resource.Error -> Resource.Error(pollDocument)
        is Resource.Success -> documentRepository.getAllExtractions(pollDocument.data)
    }

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
     * @return Empty [Resource] or information about the error
     */
    suspend fun sendFeedbackForExtractions(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
        compoundExtractions: Map<String, CompoundExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions, compoundExtractions)

    /**
     * Sends approved and conceivably corrected extractions for the given document. This is called "submitting feedback
     * on extractions" in the Gini API documentation.
     *
     * @param document            The document for which the extractions should be updated.
     * @param specificExtractions A Map where the key is the name of the specific extraction and the value is the
     *                            SpecificExtraction object. This is the same structure as returned by the getExtractions
     *                            method of this manager.
     * @return Empty [Resource] or information about the error
     */
    suspend fun sendFeedbackForExtractions(
        document: Document,
        specificExtractions: Map<String, SpecificExtraction>,
    ): Resource<Unit> =
        documentRepository.sendFeedbackForExtractions(document, specificExtractions)

    /**
     * @return Resource with [PaymentRequest] for the given id or information about the error
     */
    suspend fun getPaymentRequest(
        id: String,
    ): Resource<PaymentRequest> =
        documentRepository.getPaymentRequest(id)


    /**
     * @return Resource with a list of payment [PaymentRequest] or information about the error
     */
    suspend fun getPaymentRequests(): Resource<List<PaymentRequest>> =
        documentRepository.getPaymentRequests()

    enum class DocumentType(val apiDoctypeHint: String) {
        BANK_STATEMENT("BankStatement"),
        CONTRACT("Contract"),
        INVOICE("Invoice"),
        RECEIPT("Receipt"),
        REMINDER("Reminder"),
        REMITTANCE_SLIP("RemittanceSlip"),
        TRAVEL_EXPENSE_REPORT("TravelExpenseReport"),
        OTHER("Other");
    }
}