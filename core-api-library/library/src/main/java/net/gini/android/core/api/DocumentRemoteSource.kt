package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.core.api.mapper.toDocumentLayout
import net.gini.android.core.api.mapper.toDocumentPage
import net.gini.android.core.api.models.DocumentLayout
import net.gini.android.core.api.models.DocumentPage
import net.gini.android.core.api.models.Payment
import net.gini.android.core.api.models.toPayment
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import net.gini.android.core.api.response.PaymentRequestResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.CoroutineContext

/**
 * Internal use only.
 * 
 * Remote data source for document-related operations.
 * 
 * **Note**: Authentication is handled automatically by [GiniAuthenticationInterceptor].
 * Access tokens are no longer passed manually - the interceptor adds Bearer tokens automatically.
 */
abstract class DocumentRemoteSource(
    open val coroutineContext: CoroutineContext,
    private val documentService: DocumentService,
    private val giniApiType: GiniApiType,
    baseUriString: String
) {

    var baseUri: Uri

    init {
        baseUri = getBaseUri(baseUriString, giniApiType)
    }

    suspend fun uploadDocument(
        data: ByteArray,
        contentType: String,
        filename: String?,
        docType: String?,
        metadata: Map<String, String>?
    ): Uri = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val body: RequestBody = data.toRequestBody(
                "application/octet-stream".toMediaTypeOrNull(),
                0,
                data.size
            )
            // Pass metadata and Content-Type as headers if needed
            val headers = buildHeaderMap(contentType, metadata = metadata)
            documentService.uploadDocument(body, filename, docType, headers)
        }

        Uri.parse(response.headers()[HEADER_LOCATION_KEY] ?: "")
    }

    suspend fun deleteDocument(documentUri: Uri): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocumentFromUri(documentUri)
        }
    }

    suspend fun deleteDocument(documentId: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocument(documentId)
        }
    }

    suspend fun getDocument(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocument(documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getDocumentFromUri(uri: Uri): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocumentFromUri(uriRelativeToBaseUri(uri).toString())
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getExtractions(documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getExtractions(documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getDocumentLayout(documentId: String): DocumentLayout =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getDocumentLayout(documentId)
            }
            response.body()?.toDocumentLayout()
                ?: throw ApiException.forResponse("Empty response body", response)
        }

    suspend fun getDocumentPages(documentId: String): List<DocumentPage> =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getDocumentPages(documentId)
            }
            response.body()?.map { it.toDocumentPage() }
                ?: throw ApiException.forResponse("Empty response body", response)
        }

    suspend fun sendFeedback(documentId: String, requestBody: RequestBody): Unit =
        withContext(coroutineContext) {
            SafeApiRequest.apiRequest {
                documentService.sendFeedback(documentId, requestBody)
            }
        }

    suspend fun getFile(location: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getFile(location)
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPaymentRequest(id: String): PaymentRequestResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequest(id)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPaymentRequests(): List<PaymentRequestResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequests()
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPayment(id: String): Payment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPayment(id)
        }
        response.body()?.toPayment() ?: throw ApiException.forResponse("Empty response body", response)
    }

    /**
     * Builds header map for non-auth headers (Content-Type, Accept, metadata).
     * Authorization header is added automatically by [GiniAuthenticationInterceptor].
     */
    protected fun buildHeaderMap(
        contentType: String? = null,
        accept: String? = giniApiType.giniJsonMediaType,
        metadata: Map<String, String>? = null
    ): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            // NO Authorization header - interceptor handles it!
            accept?.let { put("Accept", it) }
            contentType?.let { put("Content-Type", it) }
            metadata?.let { putAll(it) }
        }
    }

    private fun uriRelativeToBaseUri(uri: Uri): Uri? {
        return baseUri?.buildUpon()?.path(uri.path)?.query(uri.query)?.build()
    }

    private fun getBaseUri(baseUriString: String?, giniApiType: GiniApiType): Uri {
        return if (baseUriString != null) {
            Uri.parse(Utils.checkNotNull(baseUriString))
        } else {
            Uri.parse(giniApiType.baseUrl)
        }
    }

    companion object {
        const val HEADER_LOCATION_KEY = "location"
    }

}
