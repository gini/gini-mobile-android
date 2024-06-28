package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.models.Payment
import net.gini.android.core.api.models.toPayment
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.BearerAuthorizatonHeader
import net.gini.android.core.api.requests.SafeApiRequest
import net.gini.android.core.api.response.PaymentRequestResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.CoroutineContext

/**
 * Internal use only.
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

    suspend fun uploadDocument(accessToken: String, data: ByteArray, contentType: String ,filename: String?, docType: String?, metadata: Map<String, String>?): Uri = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val body: RequestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, data.size)
            documentService.uploadDocument(bearerHeaderMap(accessToken, contentType, metadata = metadata), body, filename, docType)
        }

        Uri.parse(response.headers()[HEADER_LOCATION_KEY] ?: "")
    }

    suspend fun deleteDocument(accessToken: String, documentUri: Uri): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocumentFromUri(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentUri)
        }
    }

    suspend fun deleteDocument(accessToken: String, documentId: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocument(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
    }

    suspend fun getDocument(accessToken: String, documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocument(bearerHeaderMap(accessToken), documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getDocumentFromUri(accessToken: String, uri: Uri): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocumentFromUri(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), uriRelativeToBaseUri(uri).toString())
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getExtractions(accessToken: String, documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getExtractions(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getLayout(accessToken: String, documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getLayoutForDocument(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun sendFeedback(accessToken: String, documentId: String, requestBody: RequestBody): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.sendFeedback(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId, requestBody)
        }
    }

    suspend fun getFile(accessToken: String, location: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getFile(bearerHeaderMap(accessToken, accept = null, contentType = giniApiType.giniJsonMediaType), location)
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPaymentRequest(accessToken: String, id: String): PaymentRequestResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequest(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), id)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPaymentRequests(accessToken: String): List<PaymentRequestResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequests(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType))
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPayment(accessToken: String, id: String): Payment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPayment(bearerHeaderMap(accessToken, giniApiType.giniJsonMediaType), id)
        }
        response.body()?.toPayment() ?: throw ApiException.forResponse("Empty response body", response)
    }

    protected fun bearerHeaderMap(
        accessToken: String,
        contentType: String? = null,
        accept: String? = giniApiType.giniJsonMediaType,
        metadata: Map<String, String>? = null
    ): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            BearerAuthorizatonHeader(accessToken).addToMap(this)
            accept?.let { put("Accept", accept) }
            contentType?.let { put("Content-Type", contentType) }
            metadata?.let { putAll(metadata) }
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
