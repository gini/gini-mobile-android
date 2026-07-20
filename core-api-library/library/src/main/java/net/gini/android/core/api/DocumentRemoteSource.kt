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
// TooManyFunctions: every API method temporarily exists twice - the deprecated accessToken
// overloads are kept for compatibility until the next major version (PP-2363).
@Suppress("TooManyFunctions")
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
        data: ByteArray, contentType: String, filename: String?, docType: String?,
        metadata: Map<String, String>?
    ): Uri =
        uploadDocument(headerMap(contentType = contentType, metadata = metadata), data, filename, docType)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun uploadDocument(
        accessToken: String, data: ByteArray, contentType: String, filename: String?,
        docType: String?, metadata: Map<String, String>?
    ): Uri =
        uploadDocument(bearerHeaderMap(accessToken, contentType, metadata = metadata), data, filename, docType)

    private suspend fun uploadDocument(
        headers: Map<String, String>, data: ByteArray, filename: String?, docType: String?
    ): Uri = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val body: RequestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, data.size)
            documentService.uploadDocument(headers, body, filename, docType)
        }

        Uri.parse(response.headers()[HEADER_LOCATION_KEY] ?: "")
    }

    suspend fun deleteDocument(documentUri: Uri): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocumentFromUri(headerMap(contentType = giniApiType.giniJsonMediaType), documentUri)
        }
    }

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun deleteDocument(accessToken: String, documentUri: Uri): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocumentFromUri(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
                documentUri
            )
        }
    }

    suspend fun deleteDocument(documentId: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocument(headerMap(contentType = giniApiType.giniJsonMediaType), documentId)
        }
    }

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun deleteDocument(accessToken: String, documentId: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocument(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
                documentId
            )
        }
    }

    suspend fun getDocument(documentId: String): String =
        getDocument(headerMap(), documentId)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getDocument(accessToken: String, documentId: String): String =
        getDocument(bearerHeaderMap(accessToken), documentId)

    private suspend fun getDocument(headers: Map<String, String>, documentId: String): String =
        withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocument(headers, documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getDocumentFromUri(uri: Uri): String =
        getDocumentFromUri(headerMap(contentType = giniApiType.giniJsonMediaType), uri)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getDocumentFromUri(accessToken: String, uri: Uri): String =
        getDocumentFromUri(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), uri)

    private suspend fun getDocumentFromUri(headers: Map<String, String>, uri: Uri): String =
        withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocumentFromUri(headers, uriRelativeToBaseUri(uri).toString())
        }
        response.body()?.string() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getExtractions(documentId: String): String =
        getExtractions(headerMap(contentType = giniApiType.giniJsonMediaType), documentId)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getExtractions(accessToken: String, documentId: String): String =
        getExtractions(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)

    private suspend fun getExtractions(headers: Map<String, String>, documentId: String): String =
        withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getExtractions(headers, documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getDocumentLayout(documentId: String): DocumentLayout =
        getDocumentLayout(headerMap(contentType = giniApiType.giniJsonMediaType), documentId)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getDocumentLayout(accessToken: String, documentId: String): DocumentLayout =
        getDocumentLayout(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)

    private suspend fun getDocumentLayout(headers: Map<String, String>, documentId: String): DocumentLayout =
        withContext(coroutineContext) {
            val response = SafeApiRequest.apiRequest {
                documentService.getDocumentLayout(headers, documentId)
        }
        response.body()?.toDocumentLayout() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getDocumentPages(documentId: String): List<DocumentPage> =
        getDocumentPages(headerMap(contentType = giniApiType.giniJsonMediaType), documentId)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getDocumentPages(accessToken: String, documentId: String): List<DocumentPage> =
        getDocumentPages(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), documentId)

    private suspend fun getDocumentPages(
        headers: Map<String, String>, documentId: String
    ): List<DocumentPage> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocumentPages(headers, documentId)
        }
        response.body()?.map { it.toDocumentPage() } ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun sendFeedback(documentId: String, requestBody: RequestBody): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.sendFeedback(
                headerMap(contentType = giniApiType.giniJsonMediaType),
                documentId,
                requestBody
            )
        }
    }

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun sendFeedback(
        accessToken: String, documentId: String, requestBody: RequestBody
    ): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.sendFeedback(
                bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType),
                documentId,
                requestBody
            )
        }
    }

    suspend fun getFile(location: String): ByteArray =
        getFile(headerMap(accept = null, contentType = giniApiType.giniJsonMediaType), location)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getFile(accessToken: String, location: String): ByteArray =
        getFile(bearerHeaderMap(accessToken, accept = null, contentType = giniApiType.giniJsonMediaType), location)

    private suspend fun getFile(headers: Map<String, String>, location: String): ByteArray =
        withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getFile(headers, location)
        }
        response.body()?.bytes() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getPaymentRequest(id: String): PaymentRequestResponse =
        getPaymentRequest(headerMap(contentType = giniApiType.giniJsonMediaType), id)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getPaymentRequest(accessToken: String, id: String): PaymentRequestResponse =
        getPaymentRequest(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType), id)

    private suspend fun getPaymentRequest(
        headers: Map<String, String>, id: String
    ): PaymentRequestResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequest(headers, id)
        }
        response.body() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getPaymentRequests(): List<PaymentRequestResponse> =
        getPaymentRequests(headerMap(contentType = giniApiType.giniJsonMediaType))

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getPaymentRequests(accessToken: String): List<PaymentRequestResponse> =
        getPaymentRequests(bearerHeaderMap(accessToken, contentType = giniApiType.giniJsonMediaType))

    private suspend fun getPaymentRequests(
        headers: Map<String, String>
    ): List<PaymentRequestResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequests(headers)
        }
        response.body() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    suspend fun getPayment(id: String): Payment =
        getPayment(headerMap(giniApiType.giniJsonMediaType), id)

    @Deprecated(ACCESS_TOKEN_DEPRECATION_MESSAGE)
    @Suppress("DEPRECATION")
    suspend fun getPayment(accessToken: String, id: String): Payment =
        getPayment(bearerHeaderMap(accessToken, giniApiType.giniJsonMediaType), id)

    private suspend fun getPayment(headers: Map<String, String>, id: String): Payment = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPayment(headers, id)
        }
        response.body()?.toPayment() ?: throw ApiException.forResponse(EMPTY_RESPONSE_BODY, response)
    }

    /**
     * Creates the header map for API requests. The `Authorization` header is not part of it:
     * it is added by the SDK's session interceptor in the OkHttp layer (or by the consumer's
     * own interceptor when the consumer manages authentication themselves).
     */
    protected fun headerMap(
        contentType: String? = null,
        accept: String? = giniApiType.giniJsonMediaType,
        metadata: Map<String, String>? = null
    ): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            accept?.let { put("Accept", accept) }
            contentType?.let { put("Content-Type", contentType) }
            metadata?.let { putAll(metadata) }
        }
    }

    @Deprecated(
        "The Authorization header is added by the SDK's session interceptor in the OkHttp layer. " +
                "Use headerMap() instead."
    )
    protected fun bearerHeaderMap(
        accessToken: String,
        contentType: String? = null,
        accept: String? = giniApiType.giniJsonMediaType,
        metadata: Map<String, String>? = null
    ): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            BearerAuthorizatonHeader(accessToken).addToMap(this)
            putAll(headerMap(contentType, accept, metadata))
        }
    }

    private fun uriRelativeToBaseUri(uri: Uri): Uri {
        return baseUri.buildUpon().path(uri.path).query(uri.query).build()
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
        private const val EMPTY_RESPONSE_BODY = "Empty response body"

        internal const val ACCESS_TOKEN_DEPRECATION_MESSAGE =
            "The Authorization header is added by the SDK's session interceptor in the OkHttp layer. " +
                    "Use the overload without an accessToken parameter."
    }

}
