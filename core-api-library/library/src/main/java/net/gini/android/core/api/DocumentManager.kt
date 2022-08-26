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
open class DocumentManager<DR: DocumentRepository, E: ExtractionsContainer>(protected val documentRepository: DR) {

    /**
     * Uploads raw data and creates a new Gini partial document.
     *
     * @param document     A byte array representing an image, a pdf or UTF-8 encoded text
     * @param contentType  The media type of the uploaded data
     * @param filename     Optional the filename of the given document
     * @param documentType Optional a document type hint. See the documentation for the document type hints for
     *                     possible values
     * @return the Document instance of the freshly created document.
     */
    suspend fun createPartialDocument(
        document: ByteArray,
        contentType: String,
        filename: String? = null,
        documentType: DocumentRemoteSource.Companion.DocumentType? = null,
        documentMetadata: DocumentMetadata? = null,
    ): Document? =
        if (documentMetadata != null) {
                documentRepository.createPartialDocument(document, contentType, filename, documentType, documentMetadata).data
            } else {
                documentRepository.createPartialDocument(document, contentType, filename, documentType).data
            }

    /**
     * Deletes a Gini partial document and all its parent composite documents.
     *
     * Partial documents can be deleted only, if they don't belong to any composite documents and
     * this method deletes the parents before deleting the partial document.
     *
     * @param documentId The id of an existing partial document
     */
    suspend fun deletePartialDocumentAndParents(documentId: String) =
        documentRepository.deletePartialDocumentAndParents(documentId)
//
//    /**
//     * Deletes a Gini document.
//     *
//     * For deleting partial documents use [deletePartialDocumentAndParents] instead.
//     *
//     * @param documentId The id of an existing document
//     */
//    suspend fun deleteDocument(
//        documentId: String,
//    ) = withContext(taskDispatcher) {
//        suspendCancellableCoroutine<Unit> { continuation ->
//            val task = documentTaskManager.deleteDocument(documentId)
//            continuation.resumeUnitTask(task)
//        }
//    }
//
//    /**
//     * Creates a new Gini composite document.
//     *
//     * @param documents    A list of partial documents which should be part of a multi-page document
//     * @param documentType Optional a document type hint. See the documentation for the document type hints for
//     *                     possible values
//     * @return the Document instance of the freshly created document.
//     */
//    suspend fun createCompositeDocument(
//        documents: List<Document>,
//        documentType: DocumentTaskManager.DocumentType? = null,
//    ): Document = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.createCompositeDocument(documents, documentType)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * Creates a new Gini composite document. The input Map must contain the partial documents as keys. These will be
//     * part of the multi-page document. The value for each partial document key is the amount in degrees the document
//     * has been rotated by the user.
//     *
//     * @param documentRotationMap A map of partial documents and their rotation in degrees
//     * @param documentType        Optional a document type hint. See the documentation for the document type hints for
//     *                            possible values
//     * @return the Document instance of the freshly created document.
//     */
//    suspend fun createCompositeDocument(
//        documentRotationMap: LinkedHashMap<Document, Int>,
//        documentType: DocumentTaskManager.DocumentType,
//    ): Document = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.createCompositeDocument(documentRotationMap, documentType)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * Get the document with the given unique identifier.
//     *
//     * @param id The unique identifier of the document.
//     * @return A [Document] instance representing all the document's metadata.
//     */
//    suspend fun getDocument(
//        id: String,
//    ): Document = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.getDocument(id)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * Get the document with the given unique identifier.
//     *
//     * Please note that this method may use a slightly corrected URI from which it gets the document (e.g. if the
//     * URI's host does not conform to the base URL of the Gini API). Therefore it is not possibly to use this method to
//     * get a document from an arbitrary URI.
//     *
//     * @param uri The URI of the document.
//     * @return A [Document] instance representing all the document's metadata.
//     */
//    suspend fun getDocument(
//        uri: Uri,
//    ): Document = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.getDocument(uri)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * Continually checks the document status (via the Gini API) until the document is fully processed. To avoid
//     * flooding the network, there is a pause of at least the number of seconds that is set in the POLLING_INTERVAL
//     * constant of [DocumentTaskManager].
//     *
//     * This method returns a Task which will resolve to a new document instance. It does not update the given
//     * document instance.
//     *
//     * @param document The document which will be polled.
//     */
//    suspend fun pollDocument(
//        document: Document,
//    ): Document = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.pollDocument(document)
//            continuation.resumeTask(task)
//
//            continuation.invokeOnCancellation {
//                documentTaskManager.cancelDocumentPolling(document)
//            }
//        }
//    }
//
//    /**
//     * Sends an error report for the given document to Gini. If the processing result for a document was not
//     * satisfactory (e.g. extractions where empty or incorrect), you can create an error report for a document. This
//     * allows Gini to analyze and correct the problem that was found.
//     *
//     * The owner of this document must agree that Gini can use this document for debugging and error analysis.
//     *
//     * @param document    The erroneous document.
//     * @param summary     Optional a short summary of the occurred error.
//     * @param description Optional a more detailed description of the occurred error.
//     * @return Error ID. This is a unique identifier for your error report
//     * and can be used to refer to the reported error towards the Gini support.
//     */
//    suspend fun reportDocument(
//        document: Document,
//        summary: String? = null,
//        description: String? = null,
//    ): String = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.reportDocument(document, summary, description)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * Gets the layout of a document. The layout of the document describes the textual content of a document with
//     * positional information, based on the processed document.
//     *
//     * @param document The document for which the layouts is requested.
//     * @return A JSONObject containing the layout.
//     */
//    suspend fun getLayout(
//        document: Document,
//    ): JSONObject = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.getLayout(document)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * Get the extractions for the given document.
//     *
//     * @param document The Document instance for whose document the extractions are returned.
//     * @return [ExtractionsContainer] object.
//     */
//    suspend fun getExtractions(
//        document: Document,
//    ) = withContext(taskDispatcher) {
//        suspendCancellableCoroutine<ExtractionsContainer> { continuation ->
//            val pollDocumentTask = documentTaskManager.pollDocument(document)
//            pollDocumentTask.waitForCompletion()
//
//            if (!continuation.isActive) return@suspendCancellableCoroutine
//
//            if (!pollDocumentTask.isFaulted) {
//                val extractionTask = documentTaskManager.getAllExtractions(pollDocumentTask.result)
//                continuation.resumeTask(extractionTask)
//            } else {
//                continuation.resumeWithException(pollDocumentTask.error)
//            }
//
//            continuation.invokeOnCancellation {
//                if (!pollDocumentTask.isCompleted) {
//                    documentTaskManager.cancelDocumentPolling(document)
//                }
//            }
//        }
//    }
//
//    /**
//     * @return [PaymentRequest] for the given id
//     */
//    suspend fun getPaymentRequest(
//        id: String,
//    ): PaymentRequest = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.getPaymentRequest(id)
//            continuation.resumeTask(task)
//        }
//    }
//
//    /**
//     * @return List of payment [PaymentRequest]
//     */
//    suspend fun getPaymentRequests(): List<PaymentRequest> = withContext(taskDispatcher) {
//        suspendCancellableCoroutine { continuation ->
//            val task = documentTaskManager.paymentRequests
//            continuation.resumeTask(task)
//        }
//    }
//
//    protected fun <T> Continuation<T>.resumeTask(task: Task<T>) {
//        task.waitForCompletion()
//        if (!task.isFaulted) {
//            this.resume(task.result)
//        } else {
//            this.resumeWithException(task.error)
//        }
//    }
//
//    private fun <T> Continuation<Unit>.resumeUnitTask(task: Task<T>) {
//        task.waitForCompletion()
//        if (!task.isFaulted) {
//            this.resume(Unit)
//        } else {
//            this.resumeWithException(task.error)
//        }
//    }

}
