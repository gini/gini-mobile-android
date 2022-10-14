package net.gini.android.core.api

import android.net.Uri
import kotlinx.coroutines.withContext
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.requests.ApiException
import net.gini.android.core.api.requests.SafeApiRequest
import net.gini.android.core.api.response.PaymentRequestResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.CoroutineContext

abstract class DocumentRemoteSource(
    open val coroutineContext: CoroutineContext,
    private val documentService: DocumentService,
    private val giniApiType: GiniApiType,
    baseUriString: String
) {

    private var baseUri: Uri? = null

    init {
        baseUri = getBaseUri(baseUriString, giniApiType)
    }

    suspend fun uploadDocument(sessionToken: SessionToken, data: ByteArray, contentType: String ,filename: String?, docType: String?, metadata: Map<String, String>?): Uri = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            val body: RequestBody = data.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, data.size)
            documentService.uploadDocument(bearerHeaderMap(sessionToken, contentType, metadata = metadata), body, filename, docType)
        }

        Uri.parse(response.headers()[HEADER_LOCATION_KEY] ?: "")
    }

    suspend fun deleteDocument(sessionToken: SessionToken, documentUri: Uri): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocumentFromUri(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentUri)
        }
    }

    suspend fun deleteDocument(sessionToken: SessionToken, documentId: String): Unit = withContext(coroutineContext) {
        SafeApiRequest.apiRequest {
            documentService.deleteDocument(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
    }

    suspend fun getDocument(sessionToken: SessionToken, documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocument(bearerHeaderMap(sessionToken), documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getDocumentFromUri(sessionToken: SessionToken, uri: Uri): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getDocumentFromUri(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), uriRelativeToBaseUri(uri).toString())
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getExtractions(sessionToken: SessionToken, documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getExtractions(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun errorReportForDocument(sessionToken: SessionToken, documentId: String, summary: String?, description: String?): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.errorReportForDocument(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentId, summary, description)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getLayout(sessionToken: SessionToken, documentId: String): String = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getLayoutForDocument(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), documentId)
        }
        response.body()?.string() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getFile(sessionToken: SessionToken, location: String): ByteArray = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getFile(bearerHeaderMap(sessionToken, accept = null, contentType = giniApiType.giniJsonMediaType), location)
        }
        response.body()?.bytes() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPaymentRequest(sessionToken: SessionToken, id: String): PaymentRequestResponse = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequest(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType), id)
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    suspend fun getPaymentRequests(sessionToken: SessionToken): List<PaymentRequestResponse> = withContext(coroutineContext) {
        val response = SafeApiRequest.apiRequest {
            documentService.getPaymentRequests(bearerHeaderMap(sessionToken, contentType = giniApiType.giniJsonMediaType))
        }
        response.body() ?: throw ApiException.forResponse("Empty response body", response)
    }

    protected fun bearerHeaderMap(
        sessionToken: SessionToken,
        contentType: String? = null,
        accept: String? = giniApiType.giniJsonMediaType,
        metadata: Map<String, String>? = null
    ): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            put("Authorization", "BEARER ${sessionToken.accessToken}")
            accept?.let { put("Accept", accept) }
            contentType?.let { put("Content-Type", contentType) }
            metadata?.let { putAll(metadata) }
        }
    }

    private fun uriRelativeToBaseUri(uri: Uri): Uri? {
        return baseUri?.buildUpon()?.path(uri.path)?.query(uri.query)?.build()
    }

    private fun getBaseUri(baseUriString: String?, giniApiType: GiniApiType): Uri? {
        return if (baseUriString != null) {
            Uri.parse(Utils.checkNotNull(baseUriString))
        } else {
            Uri.parse(giniApiType.baseUrl)
        }
    }

    companion object {
        const val HEADER_LOCATION_KEY = "location"
    }

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
