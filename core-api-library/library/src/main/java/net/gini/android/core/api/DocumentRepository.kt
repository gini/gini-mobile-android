package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.*
import net.gini.android.core.api.models.Document
import net.gini.android.core.api.requests.ApiException
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

class DocumentRepository(
    override val coroutineContext: CoroutineContext,
    private val documentRemoteSource: DocumentRemoteSource,
    private val giniApiType: GiniApiType
) : CoroutineScope {

    suspend fun deletePartialDocumentAndParents(documentId: String): Resource<String> =
        wrapResponseIntoResource {
            val document = documentRemoteSource.getDocument(documentId)
            deleteDocuments(document.compositeDocuments)
            documentRemoteSource.deleteDocument(document.id)
        }

    suspend fun deleteDocuments(documents: List<Uri>) {
        val tasks = mutableListOf<Job>()
        for (uri in documents) {
            tasks.add(launch { documentRemoteSource.deleteDocument(uri) })
        }

        tasks.joinAll()
    }

    suspend fun deleteDocument(documentId: String): Resource<String> =
        wrapResponseIntoResource {
            documentRemoteSource.deleteDocument(documentId)
        }

    suspend fun createDocumentInternal(uri: Uri): Resource<Document> =
        wrapResponseIntoResource {
            documentRemoteSource.getDocumentFromUri(uri)
        }

    suspend fun createPartialDocument(documentData: ByteArray, contentType: String?,
                                      filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?,
                                      documentMetadata: DocumentMetadata?): Resource<Document> {
        return createPartialDocumentInternal(documentData, contentType, filename, documentType, documentMetadata)
    }

    suspend fun createPartialDocument(documentData: ByteArray, contentType: String?,
                                      filename: String?,
                                      documentType: DocumentRemoteSource.Companion.DocumentType?): Resource<Document> {
        return createPartialDocumentInternal(documentData, contentType, filename, documentType)
    }

    suspend fun createPartialDocumentInternal(documentData: ByteArray, contentType: String?,
                                              filename: String?, documentType: DocumentRemoteSource.Companion.DocumentType?,
                                              documentMetadata: DocumentMetadata? = null): Resource<Document> {
        var apiDoctypeHint: String? = null
        if (documentType != null) {
            apiDoctypeHint = documentType.apiDoctypeHint
        }

        val contentType = contentType?.let { MediaTypes.forPartialDocument(giniApiType.giniPartialMediaType, it) }
        val documentUri = documentRemoteSource.uploadDocument(documentData, contentType!!, filename, apiDoctypeHint, documentMetadata?.metadata)

        return createDocumentInternal(documentUri)
    }

    companion object {
        const val POLLING_INTERVAL = 1000
        const val DEFAULT_COMPRESSION = 50

        private suspend fun <T> wrapResponseIntoResource(request: suspend () -> T) =
            try {
                Resource.Success(request())
            } catch (e: ApiException) {
                Resource.Error(e.message ?: "")
            } catch (e: CancellationException) {
                Resource.Cancelled()
            }
    }
}
