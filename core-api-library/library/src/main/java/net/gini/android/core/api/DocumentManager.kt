package net.gini.android.core.api

import android.net.Uri
import bolts.Task
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.models.ExtractionsContainer
import net.gini.android.core.api.models.PaymentRequest
import org.json.JSONObject

/**
 * The [DocumentManager] is a high level API on top of the Gini API, which is used via the DocumentRepository. It
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
     * @return Resource with the Document instance of the freshly created document or null data with informations about the error
     */
    suspend fun createPartialDocument(
        document: ByteArray,
        contentType: String,
        filename: String? = null,
        documentType: DocumentRemoteSource.Companion.DocumentType? = null,
        documentMetadata: DocumentMetadata? = null,
    ): Resource<Document> =
        if (documentMetadata != null) {
            documentRepository.createPartialDocument(document, contentType, filename, documentType, documentMetadata)
        } else {
            documentRepository.createPartialDocument(document, contentType, filename, documentType)
        }

    /**
     * Deletes a Gini partial document and all its parent composite documents.
     *
     * Partial documents can be deleted only, if they don't belong to any composite documents and
     * this method deletes the parents before deleting the partial document.
     *
     * @param documentId The id of an existing partial document
     * @return Empty Resource or informations about the error
     */

    suspend fun deletePartialDocumentAndParents(documentId: String): Resource<String> =
        documentRepository.deletePartialDocumentAndParents(documentId)

    /**
     * Deletes a Gini document.
     *
     * For deleting partial documents use [deletePartialDocumentAndParents] instead.
     *
     * @param documentId The id of an existing document
     * @return Empty Resource or informations about the error
     */
    suspend fun deleteDocument(documentId: String): Resource<String> =
        documentRepository.deleteDocument(documentId)

    /**
     * Creates a new Gini composite document.
     *
     * @param documents    A list of partial documents which should be part of a multi-page document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return Resource with Document or informations about the error
     */
    suspend fun createCompositeDocument(
        documents: List<Document>,
        documentType: DocumentRemoteSource.Companion.DocumentType? = null
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
     * @return Resource with Document or informations about the error
     */
    suspend fun createCompositeDocument(
        documentRotationMap: LinkedHashMap<Document, Int>,
        documentType: DocumentRemoteSource.Companion.DocumentType?,
    ): Resource<Document> =
        documentRepository.createCompositeDocument(documentRotationMap, documentType)

    /**
     * Get the document with the given unique identifier.
     *
     * @param id The unique identifier of the document.
     * @return Resource with [Document] instance representing all the document's metadata or informations about the error
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
     * @return Resource with [Document] instance representing all the document's metadata or informations about the error
     */
    suspend fun getDocument(
        uri: Uri,
    ): Resource<Document> =
        documentRepository.getDocument(uri)

    /**
     * Continually checks the document status (via the Gini API) until the document is fully processed. To avoid
     * flooding the network, there is a pause of at least the number of seconds that is set in the POLLING_INTERVAL
     * constant of [DocumentTaskManager].
     *
     * This method returns a Task which will resolve to a new document instance. It does not update the given
     * document instance.
     *
     * @param document The document which will be polled.
     */
    suspend fun pollDocument(
        document: Document,
    ): Resource<Document> =
        documentRepository.pollDocument(document)

    /**
     * Sends an error report for the given document to Gini. If the processing result for a document was not
     * satisfactory (e.g. extractions where empty or incorrect), you can create an error report for a document. This
     * allows Gini to analyze and correct the problem that was found.
     *
     * The owner of this document must agree that Gini can use this document for debugging and error analysis.
     *
     * @param document    The erroneous document.
     * @param summary     Optional a short summary of the occurred error.
     * @param description Optional a more detailed description of the occurred error.
     * @return Resource with Error ID (This is a unique identifier for your error report
     * and can be used to refer to the reported error towards the Gini support.) or infos about API error
     */
    suspend fun reportDocument(
        document: Document,
        summary: String? = null,
        description: String? = null,
    ): Resource<String> =
        documentRepository.reportDocument(document, summary, description)

    /**
     * Gets the layout of a document. The layout of the document describes the textual content of a document with
     * positional information, based on the processed document.
     *
     * @param document The document for which the layouts is requested.
     * @return A JSONObject containing the layout.
     */
    suspend fun getLayout(
        document: Document
    ): Resource<String?> =
        documentRepository.getLayout(document)

    suspend fun getAllExtractions(
        document: Document
    ): Resource<E> =
        documentRepository.getAllExtractions(document)

    /**
     * Get the extractions for the given document.
     *
     * @param document The Document instance for whose document the extractions are returned.
     * @return [ExtractionsContainer] object.
     */
    suspend fun getExtractions(
        document: Document
    ) : Resource<E> {
        val pollDocument = documentRepository.pollDocument(document)

        if (pollDocument.data != null) {
            return documentRepository.getAllExtractions(pollDocument.data)
        }

        return Resource.Error("Empty data from poll")
    }

    /**
     * @return Resource with [PaymentRequest] for the given id or info about API error
     */
    suspend fun getPaymentRequest(
        id: String,
    ): Resource<PaymentRequest?> =
        documentRepository.getPaymentRequest(id)


    /**
     * @return Resource with a list of payment [PaymentRequest] or info about API error
     */
    suspend fun getPaymentRequests(): Resource<List<PaymentRequest>> =
        documentRepository.getPaymentRequests()
}